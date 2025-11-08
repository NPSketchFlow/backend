package com.sketchflow.sketchflow_backend.tcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sketchflow.sketchflow_backend.service.ChatService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class TcpServer {
    private final ChatService chatService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${chat.tcp.port:9090}")
    private int port;

    private ServerSocket serverSocket;
    private final ExecutorService acceptor = Executors.newSingleThreadExecutor();
    private final ExecutorService workerPool = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    public TcpServer(ChatService chatService) { this.chatService = chatService; }

    @PostConstruct
    public void start() {
        acceptor.submit(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                this.serverSocket = ss;
                while (running) {
                    Socket client = ss.accept();
                    workerPool.submit(new ClientHandler(client));
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
            }
        });
    }

    @PreDestroy
    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        acceptor.shutdownNow();
        workerPool.shutdownNow();
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

                ChatService.TcpClientConnection conn = new ChatService.TcpClientConnection(out);
                chatService.registerTcpClient(conn);

                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        JsonNode node = mapper.readTree(line);
                        String sender = node.has("sender") ? node.get("sender").asText() : "tcp-client";
                        String content = node.has("content") ? node.get("content").asText() : "";
                        String type = node.has("type") ? node.get("type").asText() : "message";
                        chatService.handleIncomingMessage(sender, content, type);
                    } catch (Exception ex) {
                        chatService.handleIncomingMessage("tcp-client", line, "message");
                    }
                }
            } catch (IOException ignored) {
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
