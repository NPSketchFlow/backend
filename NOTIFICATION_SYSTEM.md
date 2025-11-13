# Notification System Implementation

## Overview
The notification system is now fully implemented for voice chat using a **hybrid approach** (In-Memory + Database).

## Architecture

### Components

1. **UdpServer** - Handles UDP communication
   - Receives heartbeats from clients
   - Sends notifications via UDP packets
   - Broadcasts to all clients or specific users

2. **OnlineUserTracker** - Tracks online users
   - Maintains list of connected clients with their addresses
   - Publishes presence change events
   - Auto-detects offline users (15-second timeout)

3. **NotificationService** - Core notification logic
   - Stores notifications in MongoDB
   - Sends real-time notifications via UDP
   - Handles missed notifications when users come online
   - Listens to presence change events

4. **VoiceChatService** - Voice message handling
   - Stores voice messages in database
   - Triggers notifications when new voice messages arrive

## Features Implemented

### ✅ Real-Time Notifications
- **Voice Message Notifications**: When a voice message is sent, receiver gets notified immediately
- **User Online Status**: When a user comes online, all online users are notified
- **User Offline Status**: When a user goes offline (no heartbeat for 15 seconds), all users are notified

### ✅ Missed Notifications
- Notifications are stored in MongoDB
- When user comes online, all unread notifications are sent automatically
- Supports notification history and retrieval

### ✅ Notification Types
1. `TEXT_MESSAGE` - Voice chat messages
2. `USER_STATUS` - User online/offline events
3. `NEW_VOICE` - New voice file available

## How It Works

### 1. Voice Message Flow
```
User A sends voice message → VoiceChatService.addVoiceChat()
    ↓
Saves to MongoDB
    ↓
NotificationService.recordTextMessage()
    ↓
Saves notification to DB + Broadcasts via UDP
    ↓
User B (if online) receives notification immediately
User B (if offline) receives when they come online
```

### 2. User Online Flow
```
Client sends HEARTBEAT → UdpServer receives
    ↓
OnlineUserTracker.onHeartbeat()
    ↓
Publishes PresenceChangeEvent (status: ONLINE)
    ↓
NotificationService.onPresenceChange()
    ↓
Creates USER_STATUS notification + Sends missed notifications
    ↓
All online users notified + User receives missed messages
```

### 3. Notification Delivery
```
NotificationService.sendNotification()
    ↓
Persists to MongoDB
    ↓
NotificationService.broadcast()
    ↓
Gets all online users from OnlineUserTracker
    ↓
Sends UDP packet to each online user's IP:Port
```

## API Endpoints

### Send Notification
```http
POST /api/notifications/send
Content-Type: application/json

{
  "type": "TEXT_MESSAGE",
  "senderId": "user1",
  "receiverId": "user2",
  "message": "New voice message",
  "metadata": {
    "chatId": "chat123",
    "filePath": "voice-data/uploads/file.webm"
  }
}
```

### List Unread Notifications
```http
GET /api/notifications/unread?receiverId=user1
```

### Count Unread Notifications
```http
GET /api/notifications/unread/count?receiverId=user1
```

### Mark Notification as Read
```http
PATCH /api/notifications/{notificationId}/read
```

### Mark All as Read
```http
POST /api/notifications/mark-all-read
Content-Type: application/json

{
  "receiverId": "user1"
}
```

### List Online Users
```http
GET /api/online-users
```

## UDP Protocol

### Client → Server (Heartbeat)
```json
{
  "type": "HEARTBEAT",
  "userId": "user123",
  "timestamp": 1704067200000
}
```

### Server → Client (Notification)
```json
{
  "id": "uuid",
  "type": "TEXT_MESSAGE",
  "senderId": "user1",
  "receiverId": "user2",
  "message": "New voice message received",
  "timestamp": 1704067200000,
  "priority": 1,
  "read": false,
  "metadata": {
    "chatId": "chat123",
    "filePath": "voice-data/uploads/file.webm"
  }
}
```

### Server → Client (ACK Response)
```
ACK
```

## Configuration

### application.properties
```properties
# UDP notification server port
sketchflow.udp.port=8888

# MongoDB for notification persistence
spring.data.mongodb.uri=mongodb+srv://...
spring.data.mongodb.database=sketchflow
```

## Database Schema

### Notification Collection
```javascript
{
  "_id": "uuid",
  "type": "TEXT_MESSAGE",
  "fileId": "file123",
  "senderId": "user1",
  "receiverId": "user2",
  "message": "New voice message received",
  "timestamp": 1704067200000,
  "priority": 1,
  "read": false,
  "metadata": {
    "chatId": "chat123",
    "filePath": "voice-data/uploads/file.webm"
  }
}
```

### VoiceChat Collection
```javascript
{
  "_id": "chat123",
  "senderId": "user1",
  "receiverId": "user2",
  "filePath": "voice-data/uploads/file.webm",
  "timestamp": 1704067200000
}
```

## Testing

### 1. Test Heartbeat (using Python UDP client)
```python
import socket
import json
import time

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_address = ('localhost', 8888)

heartbeat = {
    "type": "HEARTBEAT",
    "userId": "testUser123",
    "timestamp": int(time.time() * 1000)
}

sock.sendto(json.dumps(heartbeat).encode(), server_address)
print("Heartbeat sent")

# Receive ACK
data, server = sock.recvfrom(4096)
print(f"Received: {data.decode()}")
```

### 2. Test Voice Message Notification
```bash
curl -X POST http://localhost:8080/api/voice-chats \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": "test123",
    "senderId": "user1",
    "receiverId": "user2",
    "filePath": "voice-data/uploads/test.webm",
    "timestamp": 1704067200000
  }'
```

### 3. Check Online Users
```bash
curl http://localhost:8080/api/online-users
```

## Java Networking Concepts Used

1. **UDP (DatagramSocket, DatagramPacket)** - Fast, connectionless protocol for real-time notifications
2. **InetAddress & InetSocketAddress** - Network address handling
3. **Broadcasting** - Send to multiple clients
4. **Heartbeat Protocol** - Keep-alive mechanism to track online status
5. **CRC32 Checksum** - Data integrity verification
6. **Multithreading (ExecutorService)** - Handle concurrent UDP packet processing
7. **Event-Driven Architecture** - Spring ApplicationEventPublisher for presence changes

## Database vs In-Memory Decision

### ✅ Uses Database For:
- Notification history
- Missed notifications (user was offline)
- Unread count/badge
- Audit trail

### ✅ Uses In-Memory For:
- Real-time delivery (UDP packets)
- Online user tracking (ConcurrentHashMap)
- Active session state

## Benefits

1. **Fast**: UDP provides low-latency delivery
2. **Reliable**: Database ensures no messages are lost
3. **Scalable**: Thread pool handles multiple concurrent users
4. **Persistent**: MongoDB stores history for offline users
5. **Real-time**: Immediate notification for online users

## Future Enhancements

- [ ] Add notification read receipts
- [ ] Implement notification retry mechanism with exponential backoff
- [ ] Add push notification support for mobile
- [ ] Group notifications (batch multiple messages)
- [ ] Add notification preferences per user
- [ ] Implement notification sound/vibration settings

