# Whiteboard Backend - API Testing Guide

## Quick Test Commands (Using curl)

### 1. Health Check
```bash
curl http://localhost:8080/api/whiteboard/monitor/health
```

### 2. System Statistics
```bash
curl http://localhost:8080/api/whiteboard/monitor/stats
```

### 3. Create a Whiteboard Session
```bash
curl -X POST http://localhost:8080/api/whiteboard/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Team Brainstorming",
    "createdBy": "user123",
    "maxUsers": 50
  }'
```

### 4. Get Session Details
```bash
# Replace {sessionId} with actual session ID from step 3
curl http://localhost:8080/api/whiteboard/sessions/{sessionId}
```

### 5. Get All Sessions
```bash
curl http://localhost:8080/api/whiteboard/sessions
```

### 6. Get Active Users in Session
```bash
curl http://localhost:8080/api/whiteboard/sessions/{sessionId}/users
```

### 7. Save Drawing Action
```bash
curl -X POST http://localhost:8080/api/whiteboard/sessions/{sessionId}/actions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "tool": "pen",
    "color": "#3B82F6",
    "actionType": "draw",
    "coordinates": {
      "points": [
        {"x": 100, "y": 150},
        {"x": 105, "y": 155}
      ]
    },
    "properties": {
      "lineWidth": 3,
      "isEraser": false
    }
  }'
```

### 8. Get Drawing Actions (Paginated)
```bash
curl "http://localhost:8080/api/whiteboard/sessions/{sessionId}/actions?page=0&size=10"
```

### 9. Get All Drawing Actions
```bash
curl http://localhost:8080/api/whiteboard/sessions/{sessionId}/actions/all
```

### 10. Get Action Statistics
```bash
curl http://localhost:8080/api/whiteboard/sessions/{sessionId}/actions/stats
```

### 11. Clear Canvas
```bash
curl -X DELETE http://localhost:8080/api/whiteboard/sessions/{sessionId}/actions
```

### 12. Delete Session
```bash
curl -X DELETE http://localhost:8080/api/whiteboard/sessions/{sessionId}
```

## WebSocket Testing (JavaScript Example)

```javascript
// Connect to WebSocket
const sessionId = 'your-session-id';
const ws = new WebSocket(`ws://localhost:8080/api/whiteboard/sessions/${sessionId}/ws`);

ws.onopen = () => {
  console.log('Connected to whiteboard');
  
  // Join session
  ws.send(JSON.stringify({
    type: 'JOIN',
    userId: 'user123',
    username: 'John Doe',
    avatar: 'https://example.com/avatar.jpg'
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};

// Send drawing action
function draw(x, y) {
  ws.send(JSON.stringify({
    type: 'DRAW',
    userId: 'user123',
    tool: 'pen',
    color: '#FF0000',
    coordinates: {
      points: [{ x, y }]
    }
  }));
}

// Send cursor movement
function moveCursor(x, y) {
  ws.send(JSON.stringify({
    type: 'CURSOR_MOVE',
    userId: 'user123',
    position: { x, y }
  }));
}

// Clear canvas
function clearCanvas() {
  ws.send(JSON.stringify({
    type: 'CLEAR',
    userId: 'user123'
  }));
}
```

## NIO TCP Client Testing

### Using the provided NIO client:
```bash
# Compile and run
cd src/main/java
javac -cp ../../../target/classes com/sketchflow/sketchflow_backend/nio/WhiteboardNioClient.java
java -cp ../../../target/classes com.sketchflow.sketchflow_backend.nio.WhiteboardNioClient

# Commands:
> sync session123 user123
> draw action1
> ping
> quit
```

### Using netcat (nc):
```bash
# Connect to NIO server
nc localhost 9999

# Send messages (JSON format):
{"type":"PING","timestamp":1234567890}
{"type":"SYNC_REQUEST","sessionId":"test123","userId":"user456"}
{"type":"DRAWING_ACTION","actionId":"act1","tool":"pen","color":"#FF0000","timestamp":1234567890}
```

## Load Testing

### Using Apache Bench (ab):
```bash
# Test session creation endpoint
ab -n 1000 -c 10 -T application/json -p session.json http://localhost:8080/api/whiteboard/sessions

# session.json content:
# {"name":"Load Test","createdBy":"loadtest","maxUsers":50}
```

### Using hey:
```bash
hey -n 1000 -c 50 http://localhost:8080/api/whiteboard/monitor/health
```

## Expected Response Examples

### Create Session Response:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Team Brainstorming",
  "createdBy": "user123",
  "createdAt": "2025-11-11T10:00:00",
  "shareLink": "https://app.com/whiteboard/550e8400-e29b-41d4-a716-446655440000"
}
```

### System Stats Response:
```json
{
  "websocket": {
    "totalWhiteboardSessions": 5,
    "totalWebSocketConnections": 15,
    "connectionsPerSession": {
      "session1": 3,
      "session2": 5,
      "session3": 7
    }
  },
  "nioServer": {
    "running": true,
    "port": 9999,
    "totalClients": 8
  },
  "jvm": {
    "totalMemory": 536870912,
    "freeMemory": 268435456,
    "usedMemory": 268435456,
    "maxMemory": 1073741824,
    "availableProcessors": 8
  },
  "threads": {
    "activeThreads": 45
  },
  "timestamp": 1699708800000
}
```

## Troubleshooting

### Port Already in Use
```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (Windows)
taskkill /PID <process_id> /F

# Kill process (Linux/Mac)
kill -9 <process_id>
```

### Connection Refused
- Ensure the application is running
- Check firewall settings
- Verify the correct port numbers

### MongoDB Connection Issues
- Check MongoDB Atlas connection string in application.properties
- Verify network access in MongoDB Atlas
- Ensure IP address is whitelisted

## Performance Benchmarks

Target metrics:
- REST API response time: < 50ms
- WebSocket message latency: < 100ms
- Drawing action throughput: 1000 actions/second
- Concurrent users per session: 50
- Maximum concurrent sessions: 100

## Monitoring

Access real-time statistics at:
- http://localhost:8080/api/whiteboard/monitor/stats
- http://localhost:8080/api/whiteboard/monitor/health

