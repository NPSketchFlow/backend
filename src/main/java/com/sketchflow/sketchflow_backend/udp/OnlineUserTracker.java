package com.sketchflow.sketchflow_backend.udp;

import com.sketchflow.sketchflow_backend.service.NotificationService;
import com.sketchflow.sketchflow_backend.service.UserService;
import com.sketchflow.sketchflow_backend.model.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Component
public class OnlineUserTracker {

    private static final Logger logger = Logger.getLogger(OnlineUserTracker.class.getName());
    // configurable TTL (default 3 minutes = 180000 ms)
    private final long ttlMs;

    private final ConcurrentHashMap<InetSocketAddress, OnlineUserInfo> onlineUsers = new ConcurrentHashMap<>();
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public OnlineUserTracker(@Autowired(required = false) UserService userService,
                             ApplicationEventPublisher eventPublisher,
                             @Value("${sketchflow.online.ttl-ms:180000}") long ttlMs) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.ttlMs = ttlMs;
        // Schedule a task to mark users offline if lastSeen is older than ttlMs
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkUserStatus, 5, 5, TimeUnit.SECONDS);
    }

    public void onHeartbeat(InetSocketAddress addr, String userId, long clientTimestamp) {
        long serverTimestamp = System.currentTimeMillis();
        long rtt = serverTimestamp - clientTimestamp;

        final boolean[] becameOnline = {false};

        OnlineUserInfo info = onlineUsers.compute(addr, (key, userInfo) -> {
            if (userInfo == null) {
                logger.info("User " + userId + " became online from " + addr);
                becameOnline[0] = true;
                return new OnlineUserInfo(userId, serverTimestamp, rtt, addr.getAddress().getHostAddress(), addr.getPort(), "ONLINE");
            } else {
                if (!"ONLINE".equals(userInfo.getStatus())) {
                    becameOnline[0] = true;
                }
                userInfo.setLastSeenTimestamp(serverTimestamp);
                userInfo.setRttEstimate(rtt);
                userInfo.setStatus("ONLINE");
                return userInfo;
            }
        });

        if (userService != null) {
            userService.updatePresence(userId,
                    "ONLINE",
                    info.getIp(),
                    info.getPort(),
                    serverTimestamp);
        }

        if (becameOnline[0] && eventPublisher != null) {
            eventPublisher.publishEvent(new NotificationService.PresenceChangeEvent(
                    userId,
                    "ONLINE",
                    info.getIp(),
                    info.getPort(),
                    serverTimestamp
            ));
        }
    }

    private void checkUserStatus() {
        long currentTime = System.currentTimeMillis();
        onlineUsers.forEach((addr, userInfo) -> {
            if (currentTime - userInfo.getLastSeenTimestamp() > ttlMs && !"OFFLINE".equals(userInfo.getStatus())) {
                userInfo.setStatus("OFFLINE");
                logger.info("User " + userInfo.getUserId() + " went offline from " + addr);
                if (userService != null) {
                    userService.updatePresence(userInfo.getUserId(),
                            "OFFLINE",
                            userInfo.getIp(),
                            userInfo.getPort(),
                            currentTime);
                }
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new NotificationService.PresenceChangeEvent(
                            userInfo.getUserId(),
                            "OFFLINE",
                            userInfo.getIp(),
                            userInfo.getPort(),
                            currentTime
                    ));
                }
            }
        });

        // Also sync presence from DB: any user who has status ONLINE in DB or a recent lastSeen should be returned as online
        if (userService != null) {
            try {
                List<User> users = userService.getAllUsers();
                long cutoff = System.currentTimeMillis() - ttlMs;
                for (User u : users) {
                    if (u == null) continue;
                    long lastActiveMillis = 0L;
                    if (u.getLastSeen() != null) {
                        lastActiveMillis = u.getLastSeen();
                    } else if (u.getLoginTime() != null) {
                        try {
                            lastActiveMillis = u.getLoginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                        } catch (Exception ex) {
                            lastActiveMillis = 0L;
                        }
                    }

                    boolean recentSeen = lastActiveMillis > cutoff;
                    boolean dbOnline = "ONLINE".equalsIgnoreCase(u.getStatus()) || recentSeen;

                    if (dbOnline) {
                        String ip = u.getIp() != null ? u.getIp() : "127.0.0.1";
                        int port = u.getPort() != null ? u.getPort() : 0;
                        InetSocketAddress addr = new InetSocketAddress(ip, port);
                        long useLast = lastActiveMillis > 0 ? lastActiveMillis : System.currentTimeMillis();
                        onlineUsers.computeIfAbsent(addr, k -> {
                            logger.info("Syncing DB presence: marking " + u.getUsername() + " online (from DB)");
                            if (eventPublisher != null) {
                                eventPublisher.publishEvent(new NotificationService.PresenceChangeEvent(u.getUsername(), "ONLINE", ip, port, System.currentTimeMillis()));
                            }
                            return new OnlineUserInfo(u.getUsername(), useLast, 0L, ip, port, "ONLINE");
                        });
                    }
                }
            } catch (Exception ex) {
                logger.warning("Failed to sync presence from DB: " + ex.getMessage());
            }
        }
    }

    public List<OnlineUserInfo> listOnlineUsers() {
        List<OnlineUserInfo> onlineList = new ArrayList<>();
        onlineUsers.forEach((addr, userInfo) -> {
            if ("ONLINE".equals(userInfo.getStatus())) {
                onlineList.add(userInfo);
            }
        });

        // Merge with DB users marked online if any, to ensure login-based presence is visible
        if (userService != null) {
            try {
                List<User> dbOnline = userService.getUsersByStatus("ONLINE");
                for (User u : dbOnline) {
                    // Avoid duplicates based on username
                    boolean exists = onlineList.stream().anyMatch(info -> u.getUsername().equals(info.getUserId()));
                    if (!exists) {
                        String ip = u.getIp() != null ? u.getIp() : "127.0.0.1";
                        int port = u.getPort() != null ? u.getPort() : 0;
                        onlineList.add(new OnlineUserInfo(u.getUsername(), u.getLastSeen() != null ? u.getLastSeen() : System.currentTimeMillis(), 0L, ip, port, "ONLINE"));
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return onlineList;
    }

    public static class OnlineUserInfo {
        @Getter
        private final String userId;
        @Getter
        @Setter
        private long lastSeenTimestamp;
        @Setter
        private long rttEstimate;
        @Getter
        private final String ip;
        @Getter
        private final int port;
        @Getter
        @Setter
        private String status;

        public OnlineUserInfo(String userId, long lastSeenTimestamp, long rttEstimate, String ip, int port, String status) {
            this.userId = userId;
            this.lastSeenTimestamp = lastSeenTimestamp;
            this.rttEstimate = rttEstimate;
            this.ip = ip;
            this.port = port;
            this.status = status;
        }

    }
}
