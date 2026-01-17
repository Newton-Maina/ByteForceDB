@echo off
setlocal

:: Change to the directory where this script is located
cd /d "%~dp0"

echo [ByteForce-Java] Building JAR artifact...
call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo [Error] Build failed.
    pause
    exit /b %errorlevel%
)
echo [ByteForce-Java] Build complete. Artifact is in target/
pause