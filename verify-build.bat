@echo off
REM Build verification script for Whiteboard Backend

echo ==========================================
echo Whiteboard Backend - Build Verification
echo ==========================================
echo.

echo [1/3] Cleaning previous builds...
call mvnw clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed
    exit /b 1
)

echo.
echo [2/3] Compiling project...
call mvnw compile -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed
    exit /b 1
)

echo.
echo [3/3] Packaging application...
call mvnw package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Packaging failed
    exit /b 1
)

echo.
echo ==========================================
echo BUILD SUCCESSFUL!
echo ==========================================
echo.
echo The application is ready to run:
echo   java -jar target\sketchflow_backend-0.0.1-SNAPSHOT.jar
echo.
echo Or use start.bat to run directly.
echo.

pause

