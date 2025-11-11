# Collaborative Whiteboard Backend - Network Implementation

## 🎯 Overview

This is a comprehensive Spring Boot backend implementation for a collaborative whiteboard application, featuring advanced network programming concepts including:

- **WebSockets** for real-time bidirectional communication
- **Java NIO (Non-blocking I/O)** with Selector-based multiplexing
- **Multi-threading** with ExecutorService thread pools
- **Asynchronous processing** with CompletableFuture
- **Client-Server architecture** with multiple protocols
- **Batch processing** for optimized database operations
- **Connection pooling** and session management

## 🔧 Network Architecture

### 1. WebSocket Server (Port 8080)
- **Purpose**: Real-time collaborative drawing synchronization
- **Protocol**: WebSocket over HTTP
- **Features**:
  - Bidirectional full-duplex communication
  - Multi-threaded message broadcasting
  - Rate limiting (100 messages/second per user)
  - Session management with concurrent collections
  - Automatic connection cleanup

### 2. NIO TCP Server (Port 9999)
- **Purpose**: Alternative synchronization channel with lower overhead
- **Protocol**: TCP with JSON messages
- **Features**:
  - Non-blocking I/O with Java NIO Selector
  - Single-threaded event loop for I/O operations
  - Worker thread pool for message processing
  - Multiplexing multiple client connections
  - Efficient buffer management

### 3. UDP Server (Port 9876)
- **Purpose**: Heartbeat and notification system
- **Protocol**: UDP datagrams
- **Features**:
  - Connectionless communication
  - Low-latency heartbeat monitoring
  - User presence tracking
  - Existing implementation integrated

## 📦 Key Components

### Network Layer

#### WebSocket Components
```
websocket/
├── WhiteboardWebSocketHandler.java    - WebSocket message handler with async processing
└── WebSocketSessionManager.java       - Thread-safe session management
```

#### NIO Components
```
nio/
├── WhiteboardNioServer.java           - NIO server with Selector-based multiplexing
└── WhiteboardNioClient.java           - Test client demonstrating NIO concepts
```

### Service Layer (Multi-threading)

#### WhiteboardSessionService
- Async session creation/deletion with CompletableFuture
- Thread pool (10 threads) for concurrent operations
- Synchronized user management
- Scheduled cleanup tasks

#### DrawingActionService
- High-throughput action processing (20 threads)
- Batch processing with BlockingQueue
- Optimized database writes (100 actions per batch)
- Background batch processor thread

#### ActiveUserService
- User presence management (10 threads)
- Scheduled executor for periodic cleanup
- Concurrent user session tracking
- Non-blocking updates

### Data Layer

#### Models
- **WhiteboardSession**: Session metadata with active users
- **DrawingAction**: Drawing events with coordinates
- **CanvasSnapshot**: Canvas state snapshots
- **ActiveUserSession**: Real-time user presence data

#### Repositories (MongoDB)
- Async data access with Spring Data MongoDB
- Indexed queries for performance
- Paginated result sets
- Cascade delete operations

## 🌐 Network Concepts Demonstrated

### 1. Sockets
- **ServerSocket**: NIO ServerSocketChannel for accepting connections
- **Socket**: SocketChannel for client communications
- **DatagramSocket**: UDP socket for heartbeat (existing)

### 2. NIO (Non-blocking I/O)
- **Channels**: ServerSocketChannel, SocketChannel
- **Selectors**: Multiplexing I/O operations
- **ByteBuffers**: Efficient memory management
- **SelectionKeys**: Event notification for channels
- **Operations**: OP_ACCEPT, OP_READ, OP_WRITE

### 3. Multi-threading
- **ExecutorService**: Fixed thread pools for concurrent tasks
- **ScheduledExecutorService**: Periodic background jobs
- **CompletableFuture**: Async programming model
- **ConcurrentHashMap**: Thread-safe collections
- **CopyOnWriteArraySet**: Thread-safe sets
- **BlockingQueue**: Producer-consumer pattern

### 4. Client-Server Communication
- **Request-Response**: REST API endpoints
- **Publish-Subscribe**: WebSocket broadcasting
- **Message Queue**: Batch processing queue
- **Multiplexing**: NIO selector handling multiple clients
- **Connection Pooling**: WebSocket session management

### 5. Synchronization
- **synchronized**: Method-level locking
- **ConcurrentHashMap**: Lock-free concurrent access
- **Atomic Operations**: Thread-safe updates
- **Rate Limiting**: Throttling with timestamps

## 🚀 API Endpoints

### REST API (HTTP/1.1)

#### Session Management
```
POST   /api/whiteboard/sessions              - Create session
GET    /api/whiteboard/sessions/{id}         - Get session details
GET    /api/whiteboard/sessions              - List sessions
DELETE /api/whiteboard/sessions/{id}         - Delete session
GET    /api/whiteboard/sessions/{id}/users   - Get active users
```

#### Drawing Actions
```
POST   /api/whiteboard/sessions/{id}/actions     - Save drawing action
GET    /api/whiteboard/sessions/{id}/actions     - Get actions (paginated)
GET    /api/whiteboard/sessions/{id}/actions/all - Get all actions
DELETE /api/whiteboard/sessions/{id}/actions     - Clear canvas
```

#### Snapshots
```
POST   /api/whiteboard/sessions/{id}/snapshots       - Save snapshot
GET    /api/whiteboard/sessions/{id}/snapshots       - List snapshots
GET    /api/whiteboard/snapshots/{id}                - Get snapshot
GET    /api/whiteboard/snapshots/{id}/download       - Download snapshot
DELETE /api/whiteboard/snapshots/{id}                - Delete snapshot
```

#### Monitoring
```
GET    /api/whiteboard/monitor/stats   - System statistics
GET    /api/whiteboard/monitor/health  - Health check
```

### WebSocket API

**Connection**: `ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws`

#### Message Types
```json
// JOIN - User joins session
{
  "type": "JOIN",
  "userId": "user123",
  "username": "John Doe",
  "avatar": "url"
}

// DRAW - Drawing action
{
  "type": "DRAW",
  "userId": "user123",
  "tool": "pen",
  "color": "#FF0000",
  "coordinates": {
    "points": [{"x": 100, "y": 150}]
  }
}

// CURSOR_MOVE - Cursor position update
{
  "type": "CURSOR_MOVE",
  "userId": "user123",
  "position": {"x": 200, "y": 300}
}

// TOOL_CHANGE - Tool/color change
{
  "type": "TOOL_CHANGE",
  "userId": "user123",
  "tool": "circle",
  "color": "#00FF00"
}

// CLEAR - Clear canvas
{
  "type": "CLEAR",
  "userId": "user123"
}

// LEAVE - User leaves
{
  "type": "LEAVE",
  "userId": "user123"
}
```

### NIO TCP Protocol

**Connection**: `tcp://localhost:9999`

#### Message Format (JSON over TCP)
```json
// Sync Request
{
  "type": "SYNC_REQUEST",
  "sessionId": "session123",
  "userId": "user123"
}

// Drawing Action
{
  "type": "DRAWING_ACTION",
  "actionId": "action123",
  "tool": "pen",
  "color": "#FF0000",
  "timestamp": 1234567890
}

// Ping
{
  "type": "PING",
  "timestamp": 1234567890
}
```

## 🔥 Performance Features

### Multi-threading Optimizations
1. **Thread Pools**: Sized based on operation type
   - WebSocket broadcast: 20 threads
   - Session management: 10 threads
   - Drawing actions: 20 threads
   - User management: 10 threads

2. **Batch Processing**: Drawing actions batched (100 per batch)

3. **Non-blocking Operations**: NIO for efficient I/O multiplexing

4. **Async Processing**: CompletableFuture for non-blocking APIs

5. **Connection Pooling**: Reusable database connections

### Memory Optimization
- ByteBuffer pooling in NIO server
- CopyOnWriteArraySet for concurrent read-heavy operations
- LinkedBlockingQueue with bounded capacity
- Efficient JSON serialization

### Scalability
- Horizontal scaling with load balancer
- Session affinity based on sessionId
- Stateless REST APIs
- Distributed caching ready (Redis)

## 🛠️ Running the Application

### Prerequisites
- Java 17+
- Maven 3.6+
- MongoDB Atlas account

### Build & Run
```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Or run JAR
java -jar target/sketchflow_backend-0.0.1-SNAPSHOT.jar
```

### Testing NIO Client
```bash
# Compile and run NIO test client
cd src/main/java
javac -cp ../../../target/classes com/sketchflow/sketchflow_backend/nio/WhiteboardNioClient.java
java -cp ../../../target/classes com.sketchflow.sketchflow_backend.nio.WhiteboardNioClient

# Commands in client:
> sync session123 user123
> draw action1
> ping
> quit
```

### Testing WebSocket
Use tools like:
- **Browser**: JavaScript WebSocket API
- **Postman**: WebSocket request
- **wscat**: CLI WebSocket client
```bash
npm install -g wscat
wscat -c ws://localhost:8080/api/whiteboard/sessions/test123/ws
```

## 📊 Monitoring

### System Statistics
```bash
curl http://localhost:8080/api/whiteboard/monitor/stats
```

Returns:
- WebSocket connection count
- NIO server statistics
- JVM memory usage
- Thread counts
- Active sessions

### Health Check
```bash
curl http://localhost:8080/api/whiteboard/monitor/health
```

## 🔒 Network Security Considerations

### Implemented
- Rate limiting (100 msg/sec per user)
- Connection timeout (5 minutes inactivity)
- Input validation
- Session-based access control

### Recommended for Production
- TLS/SSL for WebSocket (WSS)
- Authentication tokens (JWT)
- IP whitelisting
- DDoS protection
- Message size limits
- CORS configuration

## 📚 Network Concepts Reference

### Socket Programming
- Created custom NIO server with ServerSocketChannel
- Implemented client SocketChannel connections
- Buffer management with ByteBuffer
- Channel registration and event handling

### Multi-threading Patterns
- **Thread Pool**: ExecutorService for concurrent tasks
- **Producer-Consumer**: BlockingQueue for batch processing
- **Future Pattern**: CompletableFuture for async results
- **Scheduled Tasks**: ScheduledExecutorService for cleanup

### I/O Models
- **Blocking I/O**: Traditional socket operations
- **Non-blocking I/O**: Java NIO with Selector
- **Asynchronous I/O**: CompletableFuture pattern
- **Event-driven**: Selector-based multiplexing

### Communication Patterns
- **Request-Response**: REST API
- **Publish-Subscribe**: WebSocket broadcast
- **Push**: Server-initiated WebSocket messages
- **Polling**: Client periodic sync (optional)

## 🎓 Learning Outcomes

This implementation demonstrates:
1. **Sockets**: Low-level network programming
2. **NIO**: Non-blocking I/O with Selector pattern
3. **WebSockets**: Real-time bidirectional communication
4. **Thread Pools**: Concurrent request handling
5. **Async Programming**: CompletableFuture and callbacks
6. **Connection Management**: Session tracking and cleanup
7. **Protocol Design**: Custom message formats
8. **Scalability**: Load distribution and optimization
9. **Monitoring**: System health and metrics
10. **Clean Architecture**: Separation of concerns

## 📖 Additional Resources

- [Java NIO Tutorial](https://docs.oracle.com/javase/8/docs/api/java/nio/package-summary.html)
- [WebSocket Protocol RFC 6455](https://tools.ietf.org/html/rfc6455)
- [Spring WebSocket Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket)
- [Concurrency Utilities](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/package-summary.html)

## 🤝 Contributing

This is an educational project demonstrating network programming concepts for a collaborative whiteboard application.

## 📄 License

Educational use only.

