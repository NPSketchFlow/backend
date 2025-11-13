@echo off
REM Quick MongoDB Community Edition Installer Helper
REM This script will guide you through installing MongoDB locally

echo ============================================
echo MongoDB Local Installation Helper
echo ============================================
echo.

echo Your backend is currently running WITHOUT database.
echo To enable full functionality (user auth, data storage), you need MongoDB.
echo.
echo OPTION 1: Install MongoDB Community Edition (Recommended)
echo -------------------------------------------------------
echo 1. Download from: https://www.mongodb.com/try/download/community
echo 2. Run the installer (Accept all defaults)
echo 3. MongoDB will auto-start as a Windows Service
echo.

echo OPTION 2: Use MongoDB Docker (If you have Docker)
echo -------------------------------------------------------
echo Run: docker run -d -p 27017:27017 --name mongodb mongo:latest
echo.

echo After Installation:
echo -------------------------------------------------------
echo 1. Edit application.properties
echo 2. Uncomment these lines:
echo    spring.data.mongodb.uri=mongodb://localhost:27017/sketchflow
echo    spring.data.mongodb.database=sketchflow
echo    spring.data.mongodb.repositories.enabled=true
echo    app.mongodb.enabled=true
echo.
echo 3. Comment out these lines:
echo    spring.autoconfigure.exclude=...
echo.
echo 4. Restart your backend
echo.

echo ============================================
echo Current Status Check
echo ============================================
echo.

echo Checking if MongoDB is already installed...
sc query MongoDB >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] MongoDB service found!
    echo Run: net start MongoDB
    echo Then update application.properties and restart backend
) else (
    echo [INFO] MongoDB not installed yet
    echo.
    echo Opening download page in browser...
    timeout /t 2 >nul
    start https://www.mongodb.com/try/download/community
)

echo.
echo After installing MongoDB, run this to verify:
echo    mongosh
echo.
echo If it connects, you're ready!
echo.
pause

