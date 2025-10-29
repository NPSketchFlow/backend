package com.sketchflow.sketchflow_backend.controller;


import com.sketchflow.sketchflow_backend.model.DrawEvent;
import com.sketchflow.sketchflow_backend.service.DrawingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000") // Allow only frontend to make requests
@RestController
@RequestMapping("/api/drawing")
public class WebSocketController extends TextWebSocketHandler {


    @Autowired
    private DrawingService drawingService;



    // 🧭 REST endpoint for Swagger documentation
    @GetMapping("/status")
    @Operation(summary = "Check if the whiteboard server is running")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Server is running"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    public String checkServerStatus() {
        return "Whiteboard WebSocket server is running!";
    }

    // 🕸️ WebSocket logic (Swagger won't display this)
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        drawingService.broadcastDrawing(message.getPayload());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        drawingService.addSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        drawingService.removeSession(session);
    }

    // 🎨 Endpoint to get the drawing history (List of DrawEvent objects)
    @GetMapping("/history")
    @Operation(summary = "Get the drawing history of the whiteboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved drawing history"),
            @ApiResponse(responseCode = "404", description = "No drawing history found")
    })
    public List<DrawEvent> getDrawingHistory() {
        // Retrieve the drawing history as a list of DrawEvent objects
        return drawingService.getBoardState();
    }

    // 🎨 Endpoint to save drawing data from frontend (POST request)
    @PostMapping("/draw")
    @Operation(summary = "Save drawing data sent from frontend")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully saved drawing data"),
            @ApiResponse(responseCode = "400", description = "Invalid drawing data")
    })
    public String saveDrawingData(@RequestBody String drawingData) {
        drawingService.broadcastDrawing(drawingData); // Broadcast the drawing data
        return "Drawing data saved successfully!";
    }
}
