# 🎨 Collaborative Whiteboard Backend

A comprehensive Spring Boot backend implementation featuring advanced network programming concepts including WebSockets, Java NIO, multi-threading, and client-server communication.

## 📚 Quick Links

- **[Implementation Summary](IMPLEMENTATION_SUMMARY.md)** - Overview of what was built
- **[Network Implementation Details](NETWORK_IMPLEMENTATION.md)** - Technical deep dive
- **[API Testing Guide](API_TESTING_GUIDE.md)** - Testing examples and commands
- **[Whiteboard API Specification](WHITEBOARD_API_SPEC.md)** - Complete API reference

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MongoDB Atlas account (connection configured in `application.properties`)

### Run the Application

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

**Manual:**
```bash
mvn clean package -DskipTests
java -jar target/sketchflow_backend-0.0.1-SNAPSHOT.jar
```

### Access Points
- **REST API**: http://localhost:8080
- **WebSocket**: ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws
- **NIO Server**: tcp://localhost:9999
- **UDP Server**: udp://localhost:9876

## 🎯 Key Features

### Network Technologies
- ✅ **WebSocket** - Real-time bidirectional communication
- ✅ **Java NIO** - Non-blocking I/O with Selector pattern
- ✅ **Multi-threading** - 60+ worker threads with thread pools
- ✅ **UDP** - Heartbeat and presence tracking
- ✅ **REST API** - HTTP endpoints for session management

### Functionality
- ✅ Real-time collaborative drawing
- ✅ Multiple users per session (50 concurrent)
- ✅ Drawing tools (pen, eraser, shapes)
- ✅ Canvas snapshots and export
- ✅ User presence tracking
- ✅ Session management
- ✅ Drawing action history
- ✅ System monitoring and stats

## 📦 Project Structure

```
src/main/java/com/sketchflow/sketchflow_backend/
├── config/
│   └── WebSocketConfig.java
├── controller/
│   ├── WhiteboardSessionController.java
│   ├── DrawingActionController.java
│   ├── SnapshotController.java
│   └── WhiteboardMonitorController.java
├── dto/
│   ├── SessionCreateRequest.java
│   ├── DrawingActionRequest.java
│   └── WebSocketMessage.java
├── model/
│   ├── WhiteboardSession.java
│   ├── DrawingAction.java
│   ├── CanvasSnapshot.java
│   └── ActiveUserSession.java
├── repository/
│   ├── WhiteboardSessionRepository.java
│   ├── DrawingActionRepository.java
│   ├── CanvasSnapshotRepository.java
│   └── ActiveUserSessionRepository.java
├── service/
│   ├── WhiteboardSessionService.java
│   ├── DrawingActionService.java
│   └── ActiveUserService.java
├── websocket/
│   ├── WhiteboardWebSocketHandler.java
│   └── WebSocketSessionManager.java
├── nio/
│   ├── WhiteboardNioServer.java
│   └── WhiteboardNioClient.java
└── udp/ (existing)
    └── UdpServer.java
```

## 🧪 Quick Test

### 1. Health Check
```bash
curl http://localhost:8080/api/whiteboard/monitor/health
```

### 2. Create Session
```bash
curl -X POST http://localhost:8080/api/whiteboard/sessions \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Session","createdBy":"user1","maxUsers":50}'
```

### 3. WebSocket Connection (Browser Console)
```javascript
const ws = new WebSocket('ws://localhost:8080/api/whiteboard/sessions/YOUR_SESSION_ID/ws');
ws.onopen = () => ws.send(JSON.stringify({
  type: 'JOIN',
  userId: 'user1',
  username: 'Test User',
  avatar: 'https://example.com/avatar.jpg'
}));
ws.onmessage = (e) => console.log(JSON.parse(e.data));
```

### 4. NIO Client Test
```bash
cd src/main/java
javac -cp ../../../target/classes com/sketchflow/sketchflow_backend/nio/WhiteboardNioClient.java
java -cp ../../../target/classes com.sketchflow.sketchflow_backend.nio.WhiteboardNioClient
```

## 📊 API Endpoints

### Session Management
- `POST /api/whiteboard/sessions` - Create session
- `GET /api/whiteboard/sessions/{id}` - Get session details
- `GET /api/whiteboard/sessions` - List all sessions
- `DELETE /api/whiteboard/sessions/{id}` - Delete session
- `GET /api/whiteboard/sessions/{id}/users` - Get active users

### Drawing Actions
- `POST /api/whiteboard/sessions/{id}/actions` - Save drawing action
- `GET /api/whiteboard/sessions/{id}/actions` - Get actions (paginated)
- `GET /api/whiteboard/sessions/{id}/actions/all` - Get all actions
- `DELETE /api/whiteboard/sessions/{id}/actions` - Clear canvas

### Snapshots
- `POST /api/whiteboard/sessions/{id}/snapshots` - Save snapshot
- `GET /api/whiteboard/sessions/{id}/snapshots` - List snapshots
- `GET /api/whiteboard/snapshots/{id}/download` - Download snapshot

### Monitoring
- `GET /api/whiteboard/monitor/stats` - System statistics
- `GET /api/whiteboard/monitor/health` - Health check

## 🔧 Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server Ports
server.port=8080
whiteboard.nio.server.port=9999
sketchflow.udp.port=9876

# Session Settings
whiteboard.max.users.per.session=50
whiteboard.session.timeout.minutes=60

# MongoDB
spring.data.mongodb.uri=mongodb+srv://...
spring.data.mongodb.database=sketchflow
```

## 🌐 Network Concepts Demonstrated

### Sockets
- ServerSocketChannel (NIO)
- SocketChannel (client connections)
- WebSocket (bidirectional)
- DatagramSocket (UDP)

### Java NIO
- Selector-based multiplexing
- Non-blocking channels
- ByteBuffer management
- Event-driven I/O

### Multi-threading
- ExecutorService thread pools
- CompletableFuture async operations
- ScheduledExecutorService
- BlockingQueue for batch processing
- Concurrent collections

### Communication Patterns
- Request-Response (REST)
- Publish-Subscribe (WebSocket)
- Message Queue (batch processing)
- Event-driven (NIO Selector)

## 📈 Performance

- **Thread Pools**: 60+ worker threads
- **Batch Processing**: 100 actions per database write
- **Rate Limiting**: 100 messages/sec per user
- **Concurrent Users**: 50 per session
- **NIO**: Thousands of connections on single thread

## 🎓 Educational Value

This implementation demonstrates:
1. Socket programming (NIO & WebSocket)
2. Multi-threading & concurrency
3. Non-blocking I/O patterns
4. Client-server architectures
5. Real-time communication
6. Performance optimization
7. Clean code architecture
8. Spring Boot framework

## 📖 Documentation

For detailed information, see:
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md)
- [Network Implementation](NETWORK_IMPLEMENTATION.md)
- [API Testing Guide](API_TESTING_GUIDE.md)
- [API Specification](WHITEBOARD_API_SPEC.md)

## 🛠️ Troubleshooting

### Port Already in Use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### MongoDB Connection Issues
- Check connection string in `application.properties`
- Verify IP whitelist in MongoDB Atlas
- Ensure network access is allowed

## 📞 Support

For questions or issues:
1. Check documentation files
2. Review API Testing Guide for examples
3. Examine code comments and logs
4. Use monitoring endpoints for diagnostics

## ✨ Status

**✅ IMPLEMENTATION COMPLETE**

All features have been implemented with:
- 30+ Java class files
- 4 network protocols
- Multi-threading throughout
- Non-blocking I/O
- Real-time synchronization
- Comprehensive documentation

**Ready for deployment and testing!** 🚀

---

**Last Updated**: November 11, 2025  
**Version**: 1.0  
**Framework**: Spring Boot 3.3.4  
**Java Version**: 17

