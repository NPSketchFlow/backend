# Collaborative Whiteboard - Backend API Specification

## 📋 Overview

This document outlines the backend requirements for the collaborative whiteboard feature. The whiteboard allows multiple users to draw, collaborate, and interact in real-time on a shared canvas.

---

## 🎯 Core Features

### 1. **Real-time Drawing & Collaboration**
- Multiple users can draw simultaneously on the same canvas
- Real-time synchronization of drawing actions across all connected users
- Support for various drawing tools (pen, eraser, shapes)

### 2. **Drawing Tools**
- **Pen**: Freehand drawing with customizable colors
- **Eraser**: Remove existing drawings
- **Circle**: Draw circles with adjustable radius
- **Rectangle**: Draw rectangles
- **Line**: Draw straight lines
- **Arrow**: Draw directional arrows

### 3. **Color Palette**
- 7 predefined colors: Blue, Red, Yellow, Green, Purple, Pink, Dark Gray
- User can select any color for drawing

### 4. **Canvas Management**
- Clear entire canvas
- Download canvas as PNG image
- Zoom in/out (50% to 200%)
- Save canvas snapshots

### 5. **User Management**
- Track active users on the whiteboard
- Display user avatars
- Show online user count

---

## 🔧 Technical Requirements

### Data Models

#### 1. **Whiteboard Session**
```json
{
  "sessionId": "string (UUID)",
  "name": "string",
  "createdBy": "string (userId)",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "isActive": "boolean",
  "activeUsers": ["string (userId)"],
  "maxUsers": "integer (default: 50)"
}
```

#### 2. **Drawing Action**
```json
{
  "actionId": "string (UUID)",
  "sessionId": "string",
  "userId": "string",
  "tool": "enum [pen, eraser, circle, rectangle, line, arrow]",
  "color": "string (hex color)",
  "timestamp": "datetime",
  "actionType": "enum [draw, clear, undo, redo]",
  "coordinates": {
    "points": [
      {"x": "number", "y": "number"}
    ],
    "start": {"x": "number", "y": "number"},
    "end": {"x": "number", "y": "number"}
  },
  "properties": {
    "lineWidth": "number",
    "isEraser": "boolean"
  }
}
```

#### 3. **Canvas Snapshot**
```json
{
  "snapshotId": "string (UUID)",
  "sessionId": "string",
  "name": "string",
  "createdBy": "string (userId)",
  "createdAt": "datetime",
  "imageUrl": "string (S3/storage path)",
  "thumbnail": "string (S3/storage path)",
  "canvasData": "text (JSON serialized drawing actions)"
}
```

#### 4. **Active User Session**
```json
{
  "userId": "string",
  "sessionId": "string",
  "username": "string",
  "avatar": "string (URL)",
  "joinedAt": "datetime",
  "lastActivity": "datetime",
  "cursorPosition": {"x": "number", "y": "number"},
  "currentTool": "string",
  "currentColor": "string"
}
```

---

## 🌐 REST API Endpoints

### Session Management

#### 1. Create Whiteboard Session
```http
POST /api/whiteboard/sessions
Authorization: Bearer {token}

Request Body:
{
  "name": "Team Brainstorming",
  "maxUsers": 50
}

Response: 201 Created
{
  "sessionId": "uuid",
  "name": "Team Brainstorming",
  "createdBy": "userId",
  "createdAt": "2025-11-11T10:00:00Z",
  "shareLink": "https://app.com/whiteboard/uuid"
}
```

#### 2. Get Session Details
```http
GET /api/whiteboard/sessions/{sessionId}
Authorization: Bearer {token}

Response: 200 OK
{
  "sessionId": "uuid",
  "name": "Team Brainstorming",
  "createdBy": "userId",
  "createdAt": "2025-11-11T10:00:00Z",
  "activeUsers": 5,
  "isActive": true
}
```

#### 3. Get All User Sessions
```http
GET /api/whiteboard/sessions?userId={userId}
Authorization: Bearer {token}

Response: 200 OK
{
  "sessions": [
    {
      "sessionId": "uuid",
      "name": "Team Brainstorming",
      "createdAt": "2025-11-11T10:00:00Z",
      "activeUsers": 5
    }
  ],
  "total": 10
}
```

#### 4. Delete Session
```http
DELETE /api/whiteboard/sessions/{sessionId}
Authorization: Bearer {token}

Response: 204 No Content
```

---

### Drawing Actions

#### 5. Save Drawing Action
```http
POST /api/whiteboard/sessions/{sessionId}/actions
Authorization: Bearer {token}

Request Body:
{
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
    "lineWidth": 3
  }
}

Response: 201 Created
{
  "actionId": "uuid",
  "timestamp": "2025-11-11T10:05:00Z"
}
```

#### 6. Get Session Drawing History
```http
GET /api/whiteboard/sessions/{sessionId}/actions?limit=100&offset=0
Authorization: Bearer {token}

Response: 200 OK
{
  "actions": [
    {
      "actionId": "uuid",
      "userId": "user123",
      "tool": "pen",
      "color": "#3B82F6",
      "timestamp": "2025-11-11T10:05:00Z",
      "coordinates": {...}
    }
  ],
  "total": 250
}
```

#### 7. Clear Canvas
```http
POST /api/whiteboard/sessions/{sessionId}/clear
Authorization: Bearer {token}

Response: 200 OK
{
  "message": "Canvas cleared successfully",
  "clearedBy": "userId",
  "timestamp": "2025-11-11T10:10:00Z"
}
```

---

### Snapshot Management

#### 8. Save Canvas Snapshot
```http
POST /api/whiteboard/sessions/{sessionId}/snapshots
Authorization: Bearer {token}
Content-Type: multipart/form-data

Request Body:
- name: "Design v1"
- image: [file]
- canvasData: [JSON string]

Response: 201 Created
{
  "snapshotId": "uuid",
  "name": "Design v1",
  "imageUrl": "https://storage.com/snapshots/uuid.png",
  "thumbnail": "https://storage.com/thumbnails/uuid.png",
  "createdAt": "2025-11-11T10:15:00Z"
}
```

#### 9. Get Session Snapshots
```http
GET /api/whiteboard/sessions/{sessionId}/snapshots
Authorization: Bearer {token}

Response: 200 OK
{
  "snapshots": [
    {
      "snapshotId": "uuid",
      "name": "Design v1",
      "thumbnail": "url",
      "createdBy": "userId",
      "createdAt": "2025-11-11T10:15:00Z"
    }
  ],
  "total": 5
}
```

#### 10. Download Snapshot
```http
GET /api/whiteboard/snapshots/{snapshotId}/download
Authorization: Bearer {token}

Response: 200 OK
Content-Type: image/png
[Binary image data]
```

#### 11. Delete Snapshot
```http
DELETE /api/whiteboard/snapshots/{snapshotId}
Authorization: Bearer {token}

Response: 204 No Content
```

---

### User Presence

#### 12. Get Active Users
```http
GET /api/whiteboard/sessions/{sessionId}/users
Authorization: Bearer {token}

Response: 200 OK
{
  "users": [
    {
      "userId": "user123",
      "username": "John Doe",
      "avatar": "url",
      "joinedAt": "2025-11-11T10:00:00Z",
      "lastActivity": "2025-11-11T10:20:00Z"
    }
  ],
  "total": 8
}
```

---

## 🔌 WebSocket API

### Real-time Communication Protocol

#### Connection
```
WS /api/whiteboard/sessions/{sessionId}/ws
Authorization: Bearer {token}
```

#### Message Types

##### 1. Join Session
```json
{
  "type": "JOIN",
  "userId": "user123",
  "username": "John Doe",
  "avatar": "url"
}
```

##### 2. Drawing Action
```json
{
  "type": "DRAW",
  "actionId": "uuid",
  "userId": "user123",
  "tool": "pen",
  "color": "#3B82F6",
  "coordinates": {
    "points": [{"x": 100, "y": 150}]
  },
  "timestamp": "2025-11-11T10:05:00Z"
}
```

##### 3. Clear Canvas
```json
{
  "type": "CLEAR",
  "userId": "user123",
  "timestamp": "2025-11-11T10:10:00Z"
}
```

##### 4. Cursor Movement
```json
{
  "type": "CURSOR_MOVE",
  "userId": "user123",
  "position": {"x": 200, "y": 300}
}
```

##### 5. User Left
```json
{
  "type": "LEAVE",
  "userId": "user123",
  "timestamp": "2025-11-11T10:30:00Z"
}
```

##### 6. Tool/Color Change
```json
{
  "type": "TOOL_CHANGE",
  "userId": "user123",
  "tool": "circle",
  "color": "#EF4444"
}
```

---

## 🗄️ Database Schema

### Tables

#### 1. `whiteboard_sessions`
```sql
CREATE TABLE whiteboard_sessions (
    session_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    max_users INT DEFAULT 50,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);
```

#### 2. `drawing_actions`
```sql
CREATE TABLE drawing_actions (
    action_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    tool VARCHAR(50) NOT NULL,
    color VARCHAR(7) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    coordinates JSON NOT NULL,
    properties JSON,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES whiteboard_sessions(session_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    INDEX idx_session_timestamp (session_id, timestamp)
);
```

#### 3. `canvas_snapshots`
```sql
CREATE TABLE canvas_snapshots (
    snapshot_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    canvas_data LONGTEXT,
    FOREIGN KEY (session_id) REFERENCES whiteboard_sessions(session_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);
```

#### 4. `active_user_sessions`
```sql
CREATE TABLE active_user_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    cursor_x INT,
    cursor_y INT,
    current_tool VARCHAR(50),
    current_color VARCHAR(7),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (session_id) REFERENCES whiteboard_sessions(session_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_session (user_id, session_id)
);
```

---

## 🔐 Security & Authorization

### Access Control
- Users must be authenticated to access whiteboard features
- Session creator has admin privileges (can delete session, manage snapshots)
- All users in a session can draw and interact
- Implement rate limiting for drawing actions (e.g., 100 actions/second per user)

### Data Validation
- Validate coordinate ranges (0 to canvas dimensions)
- Validate color format (hex color codes)
- Sanitize user input for session names
- Limit snapshot file size (max 10MB)

---

## ⚡ Performance Considerations

### Optimization Strategies

1. **WebSocket Connection Pool**
   - Limit concurrent connections per session
   - Implement connection timeout (5 minutes of inactivity)

2. **Database Optimization**
   - Index session_id and timestamp for drawing_actions
   - Implement pagination for action history
   - Archive old sessions (older than 30 days)

3. **Caching**
   - Cache active user lists (Redis with 10-second TTL)
   - Cache session metadata
   - Use CDN for snapshot images

4. **Batch Processing**
   - Batch save drawing actions every 5 seconds
   - Aggregate cursor movements (send every 100ms)

5. **Data Compression**
   - Compress WebSocket messages using gzip
   - Store canvas data as compressed JSON

---

## 📦 Spring Boot Implementation Checklist

### Dependencies (pom.xml)
```xml
<dependencies>
    <!-- WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <!-- JPA & MySQL -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    
    <!-- Redis for caching -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- File storage (AWS S3 or similar) -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

### Package Structure
```
com.yourcompany.whiteboard
├── config
│   ├── WebSocketConfig.java
│   ├── SecurityConfig.java
│   └── RedisConfig.java
├── controller
│   ├── WhiteboardSessionController.java
│   ├── DrawingActionController.java
│   └── SnapshotController.java
├── websocket
│   ├── WhiteboardWebSocketHandler.java
│   └── WebSocketSessionManager.java
├── service
│   ├── WhiteboardSessionService.java
│   ├── DrawingActionService.java
│   ├── SnapshotService.java
│   └── FileStorageService.java
├── repository
│   ├── WhiteboardSessionRepository.java
│   ├── DrawingActionRepository.java
│   ├── CanvasSnapshotRepository.java
│   └── ActiveUserSessionRepository.java
├── model
│   ├── WhiteboardSession.java
│   ├── DrawingAction.java
│   ├── CanvasSnapshot.java
│   └── ActiveUserSession.java
├── dto
│   ├── SessionCreateRequest.java
│   ├── DrawingActionRequest.java
│   ├── SnapshotRequest.java
│   └── WebSocketMessage.java
└── exception
    ├── SessionNotFoundException.java
    ├── UnauthorizedAccessException.java
    └── GlobalExceptionHandler.java
```

---

## 🧪 Testing Requirements

### Unit Tests
- Test drawing action serialization/deserialization
- Test coordinate validation
- Test session creation and deletion
- Test user join/leave logic

### Integration Tests
- Test WebSocket connection lifecycle
- Test drawing action broadcast
- Test snapshot upload and download
- Test concurrent user interactions

### Load Tests
- Test with 50 concurrent users per session
- Test drawing action throughput (1000 actions/second)
- Test WebSocket message latency (<100ms)

---

## 📊 Monitoring & Logging

### Metrics to Track
- Active sessions count
- Total users online
- Drawing actions per second
- WebSocket connection count
- API response times
- Snapshot storage usage

### Log Events
- Session created/deleted
- User joined/left session
- Canvas cleared
- Snapshot saved
- WebSocket connection errors
- Authentication failures

---

## 🚀 Deployment Considerations

### Environment Variables
```properties
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=whiteboard_db
DB_USER=root
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# File Storage
S3_BUCKET_NAME=whiteboard-snapshots
S3_REGION=us-east-1
AWS_ACCESS_KEY=your_key
AWS_SECRET_KEY=your_secret

# Application
MAX_USERS_PER_SESSION=50
SESSION_TIMEOUT_MINUTES=60
SNAPSHOT_MAX_SIZE_MB=10
```

### Scaling Strategy
- Use load balancer for REST API
- Use sticky sessions for WebSocket connections
- Implement session affinity based on session_id
- Use Redis for distributed session management
- Consider using message queue (RabbitMQ/Kafka) for action persistence

---

## 📝 Additional Features (Future Enhancements)

1. **Undo/Redo Functionality**
   - Implement action history stack
   - API endpoints for undo/redo operations

2. **Layers Support**
   - Allow multiple drawing layers
   - Layer visibility control

3. **Templates**
   - Pre-designed canvas templates
   - Template library

4. **Export Options**
   - Export as PDF
   - Export as SVG
   - Export as video (time-lapse)

5. **Permissions & Roles**
   - View-only mode
   - Editor role
   - Admin role

6. **Comments & Annotations**
   - Add text comments to specific areas
   - Pin comments to coordinates

7. **Session Recording**
   - Record entire drawing session
   - Playback functionality

---

## 📞 Support & Questions

For any questions or clarifications regarding this specification, please contact the frontend team or refer to the frontend implementation at `app/whiteboard/page.tsx`.

---

**Last Updated:** November 11, 2025  
**Version:** 1.0  
**Author:** Development Team
