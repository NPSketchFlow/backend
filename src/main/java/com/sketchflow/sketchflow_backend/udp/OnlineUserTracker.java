package com.sketchflow.sketchflow_backend.udp;

import com.sketchflow.sketchflow_backend.service.NotificationService;
import com.sketchflow.sketchflow_backend.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Component
public class OnlineUserTracker {

    private static final Logger logger = Logger.getLogger(OnlineUserTracker.class.getName());
    private static final long TTL = 15000; // Time-to-live for online users in milliseconds

    private final ConcurrentHashMap<InetSocketAddress, OnlineUserInfo> onlineUsers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public OnlineUserTracker(@Autowired(required = false) UserService userService,
                             ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        // Schedule a task to mark users offline if lastSeen is older than TTL
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

        if (userService != null && info != null) {
            userService.updatePresence(userId,
                    "ONLINE",
                    info.getIp(),
                    info.getPort(),
                    serverTimestamp);
        }

        if (becameOnline[0] && eventPublisher != null && info != null) {
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
            if (currentTime - userInfo.getLastSeenTimestamp() > TTL && !"OFFLINE".equals(userInfo.getStatus())) {
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
    }

    public List<OnlineUserInfo> listOnlineUsers() {
        List<OnlineUserInfo> onlineList = new ArrayList<>();
        onlineUsers.forEach((addr, userInfo) -> {
            if ("ONLINE".equals(userInfo.getStatus())) {
                onlineList.add(userInfo);
            }
        });
        return onlineList;
    }

    public String getUserStatus(InetSocketAddress addr) {
        OnlineUserInfo userInfo = onlineUsers.get(addr);
        return userInfo != null ? userInfo.getStatus() : "UNKNOWN";
    }

    public static class OnlineUserInfo {
        private final String userId;
        private long lastSeenTimestamp;
        private long rttEstimate;
        private final String ip;
        private final int port;
        private String status;

        public OnlineUserInfo(String userId, long lastSeenTimestamp, long rttEstimate, String ip, int port, String status) {
            this.userId = userId;
            this.lastSeenTimestamp = lastSeenTimestamp;
            this.rttEstimate = rttEstimate;
            this.ip = ip;
            this.port = port;
            this.status = status;
        }

        public String getUserId() {
            return userId;
        }

        public long getLastSeenTimestamp() {
            return lastSeenTimestamp;
        }

        public void setLastSeenTimestamp(long lastSeenTimestamp) {
            this.lastSeenTimestamp = lastSeenTimestamp;
        }

        public long getRttEstimate() {
            return rttEstimate;
        }

        public void setRttEstimate(long rttEstimate) {
            this.rttEstimate = rttEstimate;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
