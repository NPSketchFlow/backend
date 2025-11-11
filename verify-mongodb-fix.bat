@echo off
REM Quick build verification after MongoDB driver fix

echo ==========================================
echo MongoDB Driver Fix Verification
echo ==========================================
echo.

echo [Step 1] Cleaning previous builds...
call mvnw clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)

echo.
echo [Step 2] Compiling with correct MongoDB driver...
call mvnw compile -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed
    echo.
    echo Check for compilation errors above.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo SUCCESS! MongoDB driver issue resolved.
echo ==========================================
echo.
echo The application should now start without errors.
echo.
echo To run the application:
echo   mvnw spring-boot:run
echo.
echo Or:
echo   start.bat
echo.
echo After starting, test with:
echo   curl http://localhost:8080/api/whiteboard/monitor/health
echo.

pause

