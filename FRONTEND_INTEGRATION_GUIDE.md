# 🎨 Frontend Integration Guide - Collaborative Whiteboard Backend

## 📋 Overview

This guide provides everything you need to connect your frontend to the collaborative whiteboard backend. The backend is **running on `http://localhost:8080`** and provides REST APIs, WebSocket connections, and real-time synchronization.

---

## 🚀 Backend Status

### ✅ Backend is Running
- **REST API**: `http://localhost:8080`
- **WebSocket**: `ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws`
- **NIO TCP Server**: `tcp://localhost:9999` (optional - for advanced features)
- **UDP Server**: `udp://localhost:9876` (heartbeat)

### ✅ All Features Implemented
- Real-time multi-user collaboration via WebSocket
- Session management (create, join, leave)
- Drawing synchronization
- User presence tracking
- Cursor position sharing
- Canvas snapshots/export
- Drawing history
- Multi-threading for performance
- Rate limiting (100 msg/sec per user)

---

## 📡 API Endpoints for Frontend

### 1. Session Management

#### Create a New Whiteboard Session
```javascript
// POST /api/whiteboard/sessions
const createSession = async (name, userId) => {
  const response = await fetch('http://localhost:8080/api/whiteboard/sessions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      name: name,
      createdBy: userId,
      maxUsers: 50
    })
  });
  
  const data = await response.json();
  return data; // { sessionId, name, createdBy, createdAt, shareLink }
};

// Example usage:
const session = await createSession("Team Brainstorming", "user123");
console.log("Session created:", session.sessionId);
```

#### Get Session Details
```javascript
// GET /api/whiteboard/sessions/{sessionId}
const getSession = async (sessionId) => {
  const response = await fetch(`http://localhost:8080/api/whiteboard/sessions/${sessionId}`);
  const data = await response.json();
  return data; // { sessionId, name, createdBy, createdAt, activeUsers, isActive }
};
```

#### List All Sessions
```javascript
// GET /api/whiteboard/sessions
const getAllSessions = async () => {
  const response = await fetch('http://localhost:8080/api/whiteboard/sessions');
  const data = await response.json();
  return data.sessions; // Array of sessions
};
```

#### Get Active Users in Session
```javascript
// GET /api/whiteboard/sessions/{sessionId}/users
const getActiveUsers = async (sessionId) => {
  const response = await fetch(`http://localhost:8080/api/whiteboard/sessions/${sessionId}/users`);
  const data = await response.json();
  return data.users; // Array of active users
};
```

#### Delete Session
```javascript
// DELETE /api/whiteboard/sessions/{sessionId}
const deleteSession = async (sessionId) => {
  await fetch(`http://localhost:8080/api/whiteboard/sessions/${sessionId}`, {
    method: 'DELETE'
  });
};
```

---

### 2. Drawing Actions

#### Save Drawing Action
```javascript
// POST /api/whiteboard/sessions/{sessionId}/actions
const saveDrawingAction = async (sessionId, userId, tool, color, coordinates) => {
  const response = await fetch(`http://localhost:8080/api/whiteboard/sessions/${sessionId}/actions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      userId: userId,
      tool: tool, // "pen", "eraser", "circle", "rectangle", "line", "arrow"
      color: color, // "#FF0000"
      actionType: "draw",
      coordinates: {
        points: coordinates // [{ x: 100, y: 150 }, { x: 105, y: 155 }]
      },
      properties: {
        lineWidth: 3,
        isEraser: false
      }
    })
  });
  
  return await response.json();
};
```

#### Get Drawing History (Paginated)
```javascript
// GET /api/whiteboard/sessions/{sessionId}/actions?page=0&size=100
const getDrawingHistory = async (sessionId, page = 0, size = 100) => {
  const response = await fetch(
    `http://localhost:8080/api/whiteboard/sessions/${sessionId}/actions?page=${page}&size=${size}`
  );
  const data = await response.json();
  return data.actions; // Array of drawing actions
};
```

#### Get All Drawing Actions
```javascript
// GET /api/whiteboard/sessions/{sessionId}/actions/all
const getAllDrawingActions = async (sessionId) => {
  const response = await fetch(
    `http://localhost:8080/api/whiteboard/sessions/${sessionId}/actions/all`
  );
  const data = await response.json();
  return data.actions; // Complete drawing history
};
```

#### Clear Canvas
```javascript
// DELETE /api/whiteboard/sessions/{sessionId}/actions
const clearCanvas = async (sessionId) => {
  await fetch(`http://localhost:8080/api/whiteboard/sessions/${sessionId}/actions`, {
    method: 'DELETE'
  });
};
```

---

### 3. Canvas Snapshots

#### Save Canvas Snapshot
```javascript
// POST /api/whiteboard/sessions/{sessionId}/snapshots
const saveSnapshot = async (sessionId, name, userId, imageBlob) => {
  const formData = new FormData();
  formData.append('name', name);
  formData.append('createdBy', userId);
  formData.append('image', imageBlob, 'snapshot.png');
  
  const response = await fetch(
    `http://localhost:8080/api/whiteboard/sessions/${sessionId}/snapshots`,
    {
      method: 'POST',
      body: formData
    }
  );
  
  return await response.json();
};

// Example: Convert canvas to blob and save
const canvas = document.getElementById('whiteboard-canvas');
canvas.toBlob(async (blob) => {
  const result = await saveSnapshot(sessionId, "My Snapshot", userId, blob);
  console.log("Snapshot saved:", result.snapshotId);
});
```

#### Get Snapshots for Session
```javascript
// GET /api/whiteboard/sessions/{sessionId}/snapshots
const getSnapshots = async (sessionId) => {
  const response = await fetch(
    `http://localhost:8080/api/whiteboard/sessions/${sessionId}/snapshots`
  );
  const data = await response.json();
  return data.snapshots; // Array of snapshots
};
```

#### Download Snapshot
```javascript
// GET /api/whiteboard/snapshots/{snapshotId}/download
const downloadSnapshot = async (snapshotId) => {
  const response = await fetch(
    `http://localhost:8080/api/whiteboard/snapshots/${snapshotId}/download`
  );
  return await response.json(); // { snapshotId, name, imageUrl, canvasData }
};
```

---

### 4. Monitoring

#### Health Check
```javascript
// GET /api/whiteboard/monitor/health
const checkHealth = async () => {
  const response = await fetch('http://localhost:8080/api/whiteboard/monitor/health');
  return await response.json(); // { status: "UP", service: "Whiteboard Backend", timestamp }
};
```

#### System Statistics
```javascript
// GET /api/whiteboard/monitor/stats
const getSystemStats = async () => {
  const response = await fetch('http://localhost:8080/api/whiteboard/monitor/stats');
  return await response.json(); // WebSocket, NIO, JVM statistics
};
```

---

## 🔌 WebSocket Integration (Real-time Collaboration)

### WebSocket Connection Setup

```javascript
class WhiteboardWebSocket {
  constructor(sessionId, userId, username, avatar) {
    this.sessionId = sessionId;
    this.userId = userId;
    this.username = username;
    this.avatar = avatar;
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    const wsUrl = `ws://localhost:8080/api/whiteboard/sessions/${this.sessionId}/ws`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      
      // Send JOIN message
      this.sendMessage({
        type: 'JOIN',
        userId: this.userId,
        username: this.username,
        avatar: this.avatar
      });
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    this.ws.onclose = () => {
      console.log('WebSocket closed');
      this.attemptReconnect();
    };
  }

  sendMessage(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    }
  }

  handleMessage(message) {
    switch (message.type) {
      case 'USER_JOINED':
        this.onUserJoined(message);
        break;
      case 'USER_LEFT':
        this.onUserLeft(message);
        break;
      case 'DRAW':
        this.onDraw(message);
        break;
      case 'CLEAR':
        this.onClear(message);
        break;
      case 'CURSOR_MOVE':
        this.onCursorMove(message);
        break;
      case 'TOOL_CHANGE':
        this.onToolChange(message);
        break;
      default:
        console.log('Unknown message type:', message.type);
    }
  }

  // Drawing action
  draw(tool, color, coordinates) {
    this.sendMessage({
      type: 'DRAW',
      userId: this.userId,
      tool: tool,
      color: color,
      coordinates: coordinates
    });
  }

  // Clear canvas
  clear() {
    this.sendMessage({
      type: 'CLEAR',
      userId: this.userId
    });
  }

  // Update cursor position
  updateCursor(x, y) {
    this.sendMessage({
      type: 'CURSOR_MOVE',
      userId: this.userId,
      position: { x, y }
    });
  }

  // Change tool
  changeTool(tool, color) {
    this.sendMessage({
      type: 'TOOL_CHANGE',
      userId: this.userId,
      tool: tool,
      color: color
    });
  }

  // Leave session
  leave() {
    this.sendMessage({
      type: 'LEAVE',
      userId: this.userId
    });
    this.ws.close();
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      console.log(`Reconnecting... Attempt ${this.reconnectAttempts}`);
      setTimeout(() => this.connect(), 2000 * this.reconnectAttempts);
    }
  }

  // Event handlers (override these in your implementation)
  onUserJoined(message) {
    console.log('User joined:', message.userId, message.username);
  }

  onUserLeft(message) {
    console.log('User left:', message.userId);
  }

  onDraw(message) {
    console.log('Draw action received:', message);
    // Implement drawing on canvas based on message.coordinates
  }

  onClear(message) {
    console.log('Clear canvas:', message.userId);
    // Clear your canvas
  }

  onCursorMove(message) {
    console.log('Cursor moved:', message.userId, message.position);
    // Show other user's cursor
  }

  onToolChange(message) {
    console.log('Tool changed:', message.userId, message.tool);
    // Update UI to show what tool other user is using
  }
}
```

### Usage Example

```javascript
// Initialize WebSocket connection
const ws = new WhiteboardWebSocket(
  'session-123',      // sessionId
  'user-456',         // userId
  'John Doe',         // username
  'https://...'       // avatar URL
);

// Connect
ws.connect();

// Send drawing action when user draws
canvas.addEventListener('mousemove', (e) => {
  if (isDrawing) {
    ws.draw('pen', '#FF0000', {
      points: [{ x: e.offsetX, y: e.offsetY }]
    });
  }
});

// Send cursor position
canvas.addEventListener('mousemove', (e) => {
  ws.updateCursor(e.offsetX, e.offsetY);
});

// Clear canvas
clearButton.addEventListener('click', () => {
  ws.clear();
});

// Leave session
window.addEventListener('beforeunload', () => {
  ws.leave();
});
```

---

## 🎨 Complete Frontend Integration Example

### Full Whiteboard Component (React Example)

```javascript
import React, { useEffect, useRef, useState } from 'react';

const Whiteboard = ({ sessionId, userId, username }) => {
  const canvasRef = useRef(null);
  const [ws, setWs] = useState(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [currentTool, setCurrentTool] = useState('pen');
  const [currentColor, setCurrentColor] = useState('#000000');
  const [activeUsers, setActiveUsers] = useState([]);

  useEffect(() => {
    // Initialize WebSocket connection
    const websocket = new WebSocket(
      `ws://localhost:8080/api/whiteboard/sessions/${sessionId}/ws`
    );

    websocket.onopen = () => {
      console.log('Connected to whiteboard');
      
      // Join session
      websocket.send(JSON.stringify({
        type: 'JOIN',
        userId: userId,
        username: username,
        avatar: `https://ui-avatars.com/api/?name=${username}`
      }));
    };

    websocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      handleWebSocketMessage(message);
    };

    setWs(websocket);

    // Load existing drawing history
    loadDrawingHistory();

    return () => {
      websocket.send(JSON.stringify({ type: 'LEAVE', userId }));
      websocket.close();
    };
  }, [sessionId, userId, username]);

  const loadDrawingHistory = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/whiteboard/sessions/${sessionId}/actions/all`
      );
      const data = await response.json();
      
      // Replay all drawing actions on canvas
      data.actions.forEach(action => {
        drawOnCanvas(action.coordinates, action.color, action.tool);
      });
    } catch (error) {
      console.error('Failed to load history:', error);
    }
  };

  const handleWebSocketMessage = (message) => {
    switch (message.type) {
      case 'USER_JOINED':
        setActiveUsers(prev => [...prev, { 
          userId: message.userId, 
          username: message.username 
        }]);
        break;
        
      case 'USER_LEFT':
        setActiveUsers(prev => prev.filter(u => u.userId !== message.userId));
        break;
        
      case 'DRAW':
        if (message.userId !== userId) {
          drawOnCanvas(message.coordinates, message.color, message.tool);
        }
        break;
        
      case 'CLEAR':
        clearCanvas();
        break;
        
      case 'CURSOR_MOVE':
        if (message.userId !== userId) {
          showUserCursor(message.userId, message.position);
        }
        break;
    }
  };

  const startDrawing = (e) => {
    setIsDrawing(true);
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    ctx.beginPath();
    ctx.moveTo(e.nativeEvent.offsetX, e.nativeEvent.offsetY);
  };

  const draw = (e) => {
    if (!isDrawing) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const x = e.nativeEvent.offsetX;
    const y = e.nativeEvent.offsetY;

    ctx.lineTo(x, y);
    ctx.strokeStyle = currentColor;
    ctx.lineWidth = 2;
    ctx.stroke();

    // Send to backend via WebSocket
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'DRAW',
        userId: userId,
        tool: currentTool,
        color: currentColor,
        coordinates: {
          points: [{ x, y }]
        }
      }));
    }
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  };

  const handleClear = () => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'CLEAR',
        userId: userId
      }));
    }
    clearCanvas();
  };

  const drawOnCanvas = (coordinates, color, tool) => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    
    if (coordinates && coordinates.points) {
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      ctx.beginPath();
      
      coordinates.points.forEach((point, index) => {
        if (index === 0) {
          ctx.moveTo(point.x, point.y);
        } else {
          ctx.lineTo(point.x, point.y);
        }
      });
      
      ctx.stroke();
    }
  };

  const showUserCursor = (userId, position) => {
    // Implement showing other users' cursors
    console.log(`User ${userId} cursor at:`, position);
  };

  const saveSnapshot = async () => {
    const canvas = canvasRef.current;
    canvas.toBlob(async (blob) => {
      const formData = new FormData();
      formData.append('name', `Snapshot ${new Date().toISOString()}`);
      formData.append('createdBy', userId);
      formData.append('image', blob, 'snapshot.png');

      try {
        const response = await fetch(
          `http://localhost:8080/api/whiteboard/sessions/${sessionId}/snapshots`,
          { method: 'POST', body: formData }
        );
        const data = await response.json();
        alert('Snapshot saved! ID: ' + data.snapshotId);
      } catch (error) {
        console.error('Failed to save snapshot:', error);
      }
    });
  };

  return (
    <div className="whiteboard-container">
      <div className="toolbar">
        <select value={currentTool} onChange={(e) => setCurrentTool(e.target.value)}>
          <option value="pen">Pen</option>
          <option value="eraser">Eraser</option>
          <option value="circle">Circle</option>
          <option value="rectangle">Rectangle</option>
          <option value="line">Line</option>
        </select>
        
        <input 
          type="color" 
          value={currentColor} 
          onChange={(e) => setCurrentColor(e.target.value)} 
        />
        
        <button onClick={handleClear}>Clear Canvas</button>
        <button onClick={saveSnapshot}>Save Snapshot</button>
      </div>

      <div className="active-users">
        <h3>Active Users ({activeUsers.length})</h3>
        {activeUsers.map(user => (
          <div key={user.userId}>{user.username}</div>
        ))}
      </div>

      <canvas
        ref={canvasRef}
        width={1200}
        height={800}
        onMouseDown={startDrawing}
        onMouseMove={draw}
        onMouseUp={stopDrawing}
        onMouseLeave={stopDrawing}
        style={{ border: '1px solid black' }}
      />
    </div>
  );
};

export default Whiteboard;
```

---

## 📋 WebSocket Message Types

### Messages FROM Frontend TO Backend

#### 1. JOIN - Join Session
```json
{
  "type": "JOIN",
  "userId": "user123",
  "username": "John Doe",
  "avatar": "https://example.com/avatar.jpg"
}
```

#### 2. DRAW - Drawing Action
```json
{
  "type": "DRAW",
  "userId": "user123",
  "tool": "pen",
  "color": "#FF0000",
  "coordinates": {
    "points": [
      { "x": 100, "y": 150 },
      { "x": 105, "y": 155 }
    ]
  }
}
```

#### 3. CLEAR - Clear Canvas
```json
{
  "type": "CLEAR",
  "userId": "user123"
}
```

#### 4. CURSOR_MOVE - Update Cursor Position
```json
{
  "type": "CURSOR_MOVE",
  "userId": "user123",
  "position": { "x": 200, "y": 300 }
}
```

#### 5. TOOL_CHANGE - Change Tool/Color
```json
{
  "type": "TOOL_CHANGE",
  "userId": "user123",
  "tool": "circle",
  "color": "#00FF00"
}
```

#### 6. LEAVE - Leave Session
```json
{
  "type": "LEAVE",
  "userId": "user123"
}
```

### Messages FROM Backend TO Frontend

#### 1. USER_JOINED - New User Joined
```json
{
  "type": "USER_JOINED",
  "userId": "user456",
  "username": "Jane Smith",
  "avatar": "https://example.com/avatar2.jpg",
  "timestamp": "2025-11-11T12:30:00"
}
```

#### 2. USER_LEFT - User Left
```json
{
  "type": "USER_LEFT",
  "userId": "user456",
  "timestamp": "2025-11-11T12:35:00"
}
```

#### 3. DRAW - Drawing Action from Other User
```json
{
  "type": "DRAW",
  "userId": "user456",
  "tool": "pen",
  "color": "#0000FF",
  "coordinates": {
    "points": [...]
  },
  "timestamp": "2025-11-11T12:30:05"
}
```

#### 4. CLEAR - Canvas Cleared
```json
{
  "type": "CLEAR",
  "userId": "user456",
  "timestamp": "2025-11-11T12:30:10"
}
```

#### 5. CURSOR_MOVE - Other User's Cursor
```json
{
  "type": "CURSOR_MOVE",
  "userId": "user456",
  "position": { "x": 250, "y": 350 }
}
```

---

## 🎨 Drawing Tools Reference

### Supported Tools
- `pen` - Free drawing
- `eraser` - Erase drawings
- `circle` - Draw circles
- `rectangle` - Draw rectangles
- `line` - Draw straight lines
- `arrow` - Draw arrows

### Color Format
Use hex color codes: `#RRGGBB`
- Example: `#FF0000` (red), `#00FF00` (green), `#0000FF` (blue)

### Coordinates Format
```javascript
{
  points: [
    { x: 100, y: 150 },
    { x: 105, y: 155 },
    { x: 110, y: 160 }
  ]
}
```

---

## 🔒 Best Practices

### 1. Error Handling
```javascript
// Always wrap API calls in try-catch
try {
  const response = await fetch('http://localhost:8080/api/...');
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }
  const data = await response.json();
  // Process data
} catch (error) {
  console.error('API call failed:', error);
  // Show user-friendly error message
}
```

### 2. WebSocket Reconnection
```javascript
// Implement automatic reconnection
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;

ws.onclose = () => {
  if (reconnectAttempts < maxReconnectAttempts) {
    reconnectAttempts++;
    setTimeout(() => connectWebSocket(), 2000 * reconnectAttempts);
  }
};
```

### 3. Rate Limiting
```javascript
// Throttle cursor position updates
let lastCursorUpdate = 0;
const CURSOR_UPDATE_INTERVAL = 50; // ms

canvas.addEventListener('mousemove', (e) => {
  const now = Date.now();
  if (now - lastCursorUpdate > CURSOR_UPDATE_INTERVAL) {
    ws.updateCursor(e.offsetX, e.offsetY);
    lastCursorUpdate = now;
  }
});
```

### 4. Canvas Optimization
```javascript
// Use requestAnimationFrame for smooth drawing
let drawQueue = [];

function processDrawQueue() {
  if (drawQueue.length > 0) {
    const action = drawQueue.shift();
    drawOnCanvas(action);
  }
  requestAnimationFrame(processDrawQueue);
}

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  if (message.type === 'DRAW') {
    drawQueue.push(message);
  }
};

requestAnimationFrame(processDrawQueue);
```

---

## 🧪 Testing Your Integration

### 1. Test REST API
```javascript
// Test session creation
const testAPI = async () => {
  try {
    // Create session
    const createResponse = await fetch('http://localhost:8080/api/whiteboard/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: 'Test Session',
        createdBy: 'testUser',
        maxUsers: 50
      })
    });
    const session = await createResponse.json();
    console.log('✅ Session created:', session.sessionId);

    // Get session details
    const getResponse = await fetch(`http://localhost:8080/api/whiteboard/sessions/${session.sessionId}`);
    const sessionDetails = await getResponse.json();
    console.log('✅ Session details:', sessionDetails);

    return session.sessionId;
  } catch (error) {
    console.error('❌ API test failed:', error);
  }
};

testAPI();
```

### 2. Test WebSocket
```javascript
// Test WebSocket connection
const testWebSocket = (sessionId) => {
  const ws = new WebSocket(`ws://localhost:8080/api/whiteboard/sessions/${sessionId}/ws`);

  ws.onopen = () => {
    console.log('✅ WebSocket connected');
    
    ws.send(JSON.stringify({
      type: 'JOIN',
      userId: 'testUser',
      username: 'Test User',
      avatar: 'https://ui-avatars.com/api/?name=Test'
    }));
  };

  ws.onmessage = (event) => {
    console.log('✅ Message received:', JSON.parse(event.data));
  };

  ws.onerror = (error) => {
    console.error('❌ WebSocket error:', error);
  };

  // Send test draw message after 2 seconds
  setTimeout(() => {
    ws.send(JSON.stringify({
      type: 'DRAW',
      userId: 'testUser',
      tool: 'pen',
      color: '#FF0000',
      coordinates: {
        points: [{ x: 100, y: 100 }, { x: 200, y: 200 }]
      }
    }));
    console.log('✅ Draw message sent');
  }, 2000);
};
```

---

## 📊 Performance Considerations

### Backend Performance Features
- **Rate Limiting**: 100 messages per second per user
- **Batch Processing**: Drawing actions batched (100 per database write)
- **Multi-threading**: 60+ worker threads for concurrent operations
- **Non-blocking I/O**: Java NIO for efficient network operations
- **Connection Pooling**: Optimized database connections

### Frontend Optimization Tips
1. **Throttle events** (mouse moves, cursor updates)
2. **Use requestAnimationFrame** for smooth rendering
3. **Batch drawing operations** when replaying history
4. **Implement local caching** for drawing history
5. **Use Web Workers** for heavy computations
6. **Debounce save operations**

---

## 🐛 Troubleshooting

### Connection Issues

**Problem**: Cannot connect to REST API
```javascript
// Solution: Check if backend is running
fetch('http://localhost:8080/api/whiteboard/monitor/health')
  .then(res => res.json())
  .then(data => console.log('Backend status:', data))
  .catch(err => console.error('Backend not responding:', err));
```

**Problem**: WebSocket connection fails
```javascript
// Solution: Check WebSocket URL format
// Correct: ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws
// Wrong: http://localhost:8080/... (use ws:// not http://)
```

**Problem**: CORS errors
```javascript
// Backend already has CORS enabled with @CrossOrigin(origins = "*")
// If still having issues, check browser console for specific error
```

### Drawing Issues

**Problem**: Drawings not syncing
- Check WebSocket connection is open: `ws.readyState === WebSocket.OPEN`
- Verify message format matches backend expectations
- Check browser console for errors

**Problem**: Canvas clears unexpectedly
- Check for unintended CLEAR messages
- Verify user permissions
- Check if session was deleted

---

## 📚 Additional Resources

### Backend Documentation
- `NETWORK_IMPLEMENTATION.md` - Technical architecture
- `API_TESTING_GUIDE.md` - API examples with curl
- `README.md` - Project overview

### Example Projects
- React whiteboard components
- Vue.js integration examples
- Vanilla JavaScript implementation

---

## 🎉 Quick Start Checklist

- [ ] Backend is running on `http://localhost:8080`
- [ ] Test health endpoint: `GET /api/whiteboard/monitor/health`
- [ ] Create a session: `POST /api/whiteboard/sessions`
- [ ] Connect WebSocket: `ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws`
- [ ] Send JOIN message
- [ ] Implement drawing on canvas
- [ ] Send DRAW messages via WebSocket
- [ ] Handle incoming DRAW messages
- [ ] Implement cursor tracking
- [ ] Add clear canvas functionality
- [ ] Test multi-user collaboration
- [ ] Implement snapshot saving

---

## 💡 Support

For questions or issues:
1. Check browser console for errors
2. Verify backend is running: `http://localhost:8080/api/whiteboard/monitor/health`
3. Check WebSocket connection state
4. Review backend logs for errors
5. Test with curl commands from `API_TESTING_GUIDE.md`

---

**Your collaborative whiteboard frontend is ready to be built!** 🎨✨

**Backend is running and waiting for connections!** 🚀

