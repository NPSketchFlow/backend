@echo off
REM Whiteboard Backend - Quick Start Guide (Windows)

echo ==========================================
echo Collaborative Whiteboard Backend
echo ==========================================
echo.

echo Building the project...
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo.

    echo Starting the application...
    echo    - REST API: http://localhost:8080
    echo    - WebSocket: ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws
    echo    - NIO Server: tcp://localhost:9999
    echo    - UDP Server: udp://localhost:9876
    echo.

    java -jar target\sketchflow_backend-0.0.1-SNAPSHOT.jar
) else (
    echo Build failed. Please check the errors above.
    exit /b 1
)
#!/bin/bash

# Whiteboard Backend - Quick Start Guide

echo "=========================================="
echo "Collaborative Whiteboard Backend"
echo "=========================================="
echo ""

echo "📦 Building the project..."
mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""

    echo "🚀 Starting the application..."
    echo "   - REST API: http://localhost:8080"
    echo "   - WebSocket: ws://localhost:8080/api/whiteboard/sessions/{sessionId}/ws"
    echo "   - NIO Server: tcp://localhost:9999"
    echo "   - UDP Server: udp://localhost:9876"
    echo ""

    java -jar target/sketchflow_backend-0.0.1-SNAPSHOT.jar
else
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

