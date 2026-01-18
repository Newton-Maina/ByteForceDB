@echo off
setlocal

:: Change to the directory where this script is located
cd /d "%~dp0"

echo [ByteForce-Java] Cleaning and Building...
call mvn clean package -DskipTests -q

if %errorlevel% neq 0 (
    echo [Error] Build failed.
    pause
    exit /b %errorlevel%
)

echo [ByteForce-Java] Starting CLI...
echo Press Ctrl+C to stop.
echo.

java -jar target/byteforce-db-1.0-SNAPSHOT-jar-with-dependencies.jar

if %errorlevel% neq 0 (
    echo [Error] Execution failed.
    pause
    exit /b %errorlevel%
)
pause