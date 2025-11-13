package com.sketchflow.sketchflow_backend.nio;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * NIO-based TCP server for whiteboard synchronization
 * Demonstrates advanced network programming with Java NIO
 * Uses Selector for multiplexing I/O operations across multiple channels
 */
@Service
public class WhiteboardNioServer {

    private static final Logger logger = Logger.getLogger(WhiteboardNioServer.class.getName());
    private static final int PORT = 9999;
    private static final int BUFFER_SIZE = 8192;

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private final ExecutorService workerPool = Executors.newFixedThreadPool(10);
    private volatile boolean running = false;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map to track client channels
    private final Map<SocketChannel, ClientSession> clientSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() {
        workerPool.submit(this::startServer);
    }

    private void startServer() {
        try {
            // Open selector and server socket channel
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(PORT));

            // Register server channel with selector for ACCEPT operations
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            running = true;
            logger.info("Whiteboard NIO Server started on port " + PORT);

            // Main event loop
            while (running) {
                // Block until at least one channel is ready
                int readyChannels = selector.select(1000);

                if (readyChannels == 0) {
                    continue;
                }

                // Get ready keys
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (Exception e) {
                        logger.warning("Error handling key: " + e.getMessage());
                        closeChannel(key);
                    }
                }
            }

        } catch (IOException e) {
            logger.severe("NIO Server error: " + e.getMessage());
        }
    }

    /**
     * Handle new client connection
     */
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);

            // Register client channel for READ operations
            clientChannel.register(selector, SelectionKey.OP_READ);

            // Track client session
            ClientSession session = new ClientSession(clientChannel);
            clientSessions.put(clientChannel, session);

            logger.info("New NIO client connected: " + clientChannel.getRemoteAddress() +
                       ". Total clients: " + clientSessions.size());

            // Send welcome message
            sendWelcomeMessage(clientChannel);
        }
    }

    /**
     * Handle READ operation - data available from client
     */
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientSession session = clientSessions.get(clientChannel);

        if (session == null) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead;

        try {
            bytesRead = clientChannel.read(buffer);
        } catch (IOException e) {
            logger.warning("Error reading from client: " + e.getMessage());
            closeChannel(key);
            return;
        }

        if (bytesRead == -1) {
            // Client closed connection
            logger.info("Client disconnected: " + clientChannel.getRemoteAddress());
            closeChannel(key);
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            String message = new String(data, StandardCharsets.UTF_8).trim();

            logger.info("Received from NIO client: " + message);

            // Process message in worker thread to avoid blocking selector
            workerPool.submit(() -> processClientMessage(clientChannel, session, message));
        }
    }

    /**
     * Handle WRITE operation - ready to send data to client
     */
    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientSession session = clientSessions.get(clientChannel);

        if (session == null || session.writeQueue.isEmpty()) {
            // No more data to write, switch back to READ mode
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        ByteBuffer buffer = session.writeQueue.poll();
        if (buffer != null) {
            clientChannel.write(buffer);

            if (buffer.hasRemaining()) {
                // Still data to write, put back in queue
                session.writeQueue.offer(buffer);
            } else if (session.writeQueue.isEmpty()) {
                // All data written, switch back to READ mode
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    /**
     * Process client message
     */
    @SuppressWarnings("unchecked")
    private void processClientMessage(SocketChannel channel, ClientSession session, String message) {
        try {
            // Parse message as JSON
            Map<String, Object> messageData = objectMapper.readValue(message, Map.class);
            String type = (String) messageData.get("type");

            switch (type) {
                case "SYNC_REQUEST":
                    handleSyncRequest(channel, session, messageData);
                    break;
                case "DRAWING_ACTION":
                    handleDrawingAction(channel, session, messageData);
                    break;
                case "PING":
                    sendResponse(channel, Map.of("type", "PONG", "timestamp", System.currentTimeMillis()));
                    break;
                default:
                    logger.warning("Unknown message type: " + type);
            }

            session.updateLastActivity();

        } catch (Exception e) {
            logger.warning("Error processing client message: " + e.getMessage());
        }
    }

    /**
     * Handle sync request
     */
    private void handleSyncRequest(SocketChannel channel, ClientSession session, Map<String, Object> data) {
        String sessionId = (String) data.get("sessionId");
        String userId = (String) data.get("userId");

        session.sessionId = sessionId;
        session.userId = userId;

        Map<String, Object> response = new HashMap<>();
        response.put("type", "SYNC_RESPONSE");
        response.put("status", "success");
        response.put("sessionId", sessionId);
        response.put("timestamp", System.currentTimeMillis());

        sendResponse(channel, response);
    }

    /**
     * Handle drawing action
     */
    private void handleDrawingAction(SocketChannel channel, ClientSession session, Map<String, Object> data) {
        // Broadcast to other clients in the same session
        broadcastToSession(session.sessionId, data, channel);

        // Send acknowledgment
        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "ACK");
        ack.put("actionId", data.get("actionId"));

        sendResponse(channel, ack);
    }

    /**
     * Send response to client
     */
    private void sendResponse(SocketChannel channel, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            ByteBuffer buffer = ByteBuffer.wrap((json + "\n").getBytes(StandardCharsets.UTF_8));

            ClientSession session = clientSessions.get(channel);
            if (session != null) {
                session.writeQueue.offer(buffer);

                // Register for WRITE operation
                SelectionKey key = channel.keyFor(selector);
                if (key != null && key.isValid()) {
                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    selector.wakeup();
                }
            }

        } catch (Exception e) {
            logger.warning("Error sending response: " + e.getMessage());
        }
    }

    /**
     * Broadcast message to all clients in a session
     */
    private void broadcastToSession(String sessionId, Map<String, Object> data, SocketChannel excludeChannel) {
        if (sessionId == null) return;

        for (Map.Entry<SocketChannel, ClientSession> entry : clientSessions.entrySet()) {
            SocketChannel channel = entry.getKey();
            ClientSession session = entry.getValue();

            if (sessionId.equals(session.sessionId) && !channel.equals(excludeChannel)) {
                sendResponse(channel, data);
            }
        }
    }

    /**
     * Send welcome message to new client
     */
    private void sendWelcomeMessage(SocketChannel channel) {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "WELCOME");
        welcome.put("message", "Connected to Whiteboard NIO Server");
        welcome.put("timestamp", System.currentTimeMillis());

        sendResponse(channel, welcome);
    }

    /**
     * Close client channel
     */
    private void closeChannel(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            clientSessions.remove(channel);
            key.cancel();
            channel.close();
            logger.info("Closed client channel. Remaining clients: " + clientSessions.size());
        } catch (IOException e) {
            logger.warning("Error closing channel: " + e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;

        try {
            // Close all client connections
            for (SocketChannel channel : clientSessions.keySet()) {
                channel.close();
            }
            clientSessions.clear();

            // Close server channel and selector
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();

            // Shutdown worker pool
            workerPool.shutdown();
            if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }

            logger.info("Whiteboard NIO Server shut down successfully");

        } catch (Exception e) {
            logger.severe("Error during shutdown: " + e.getMessage());
        }
    }

    /**
     * Client session data
     */
    private static class ClientSession {
        final SocketChannel channel;
        String sessionId;
        String userId;
        long lastActivity;
        final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();

        ClientSession(SocketChannel channel) {
            this.channel = channel;
            this.lastActivity = System.currentTimeMillis();
        }

        void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }

    /**
     * Get server statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("running", running);
        stats.put("port", PORT);
        stats.put("totalClients", clientSessions.size());
        stats.put("activeWorkers", workerPool.toString());

        return stats;
    }
}

