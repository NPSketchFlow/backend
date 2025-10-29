package com.sketchflow.sketchflow_backend.service;

import com.sketchflow.sketchflow_backend.dto.DrawEventDTO;
import com.sketchflow.sketchflow_backend.model.DrawEvent;
import com.sketchflow.sketchflow_backend.repository.DrawEventRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.*;

@Service
public class DrawingService {

    // Injecting the DrawEventRepository to save drawing events to MongoDB
    @Autowired
    private DrawEventRepository drawEventRepository;

    // Method to get the current list of active sessions (for debugging or further logic)
    // A thread-safe set to hold all active WebSocket sessions
    @Getter
    private final Set<WebSocketSession> activeSessions = Collections.synchronizedSet(new HashSet<>());

    // Store the board state as a list of DrawEvent objects
    @Getter
    private final List<DrawEvent> boardState = new ArrayList<>();

    // This method will add a session to the active sessions when a client connects
    public void addSession(WebSocketSession session) {
        activeSessions.add(session);
        sendBoardStateToNewClient(session);  // Send the board state to new client
    }

    // This method will remove a session when a client disconnects
    public void removeSession(WebSocketSession session) {
        activeSessions.remove(session);
    }

    // This method broadcasts drawing events to all connected clients
    public void broadcastDrawing(String drawingData) {
        // Parse drawing data and create a DrawEvent
        DrawEvent drawEvent = parseDrawingData(drawingData);

        // Add the drawing event to the board state
        boardState.add(drawEvent);

        // Broadcast the drawing event to all active sessions
        synchronized (activeSessions) {
            for (WebSocketSession session : activeSessions) {
                if (session.isOpen()) {
                    try {
                        String eventData = convertDrawEventToString(drawEvent);
                        session.sendMessage(new TextMessage(eventData));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Send the entire board state to a new client
    private void sendBoardStateToNewClient(WebSocketSession session) {
        for (DrawEvent drawEvent : boardState) {
            try {
                String eventData = convertDrawEventToString(drawEvent);
                session.sendMessage(new TextMessage(eventData));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Save drawing event to MongoDB
    private void saveDrawingData(DrawEvent drawEvent) {
        try {
            drawEventRepository.save(drawEvent);
            System.out.println("Drawing event saved to MongoDB: " + drawEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to parse the drawing data (String) and create a DrawEvent object
    private DrawEvent parseDrawingData(String drawingData) {
        // Example: Parse drawingData and create a DrawEvent object
        // You can use a JSON parser here for better accuracy (e.g., Jackson or Gson)
        String[] parts = drawingData.split(",");
        String action = parts[0].split(":")[1].trim().replaceAll("\"", "");
        int x = Integer.parseInt(parts[1].split(":")[1].trim());
        int y = Integer.parseInt(parts[2].split(":")[1].trim());
        String color = parts[3].split(":")[1].trim().replaceAll("\"", "");
        String tool = parts[4].split(":")[1].trim().replaceAll("\"", "");

        // Create and return the DrawEvent object
        DrawEvent drawEvent = new DrawEvent();
        drawEvent.setAction(action);
        drawEvent.setX(x);
        drawEvent.setY(y);
        drawEvent.setColor(color);
        drawEvent.setTool(tool);
        drawEvent.setTimestamp(String.valueOf(System.currentTimeMillis())); // Set timestamp

        // Save the drawing event to MongoDB
        saveDrawingData(drawEvent);

        return drawEvent;
    }

    // Helper method to convert a DrawEvent object to a String representation
    private String convertDrawEventToString(DrawEvent drawEvent) {
        return "Action: " + drawEvent.getAction() +
                ", X: " + drawEvent.getX() +
                ", Y: " + drawEvent.getY() +
                ", Color: " + drawEvent.getColor() +
                ", Tool: " + drawEvent.getTool();
    }

    // Get the entire drawing history (board state) as a list of DrawEvent objects
    public List<DrawEvent> getBoardState() {
        return drawEventRepository.findAll();
    }
}
