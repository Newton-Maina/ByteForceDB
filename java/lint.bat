@echo off
setlocal

:: Change to the directory where this script is located
cd /d "%~dp0"

echo [ByteForce-Java] Formatting code...
call mvn com.spotify.fmt:fmt-maven-plugin:format

if %errorlevel% neq 0 (
    echo [Error] Linting/Formatting failed.
    pause
    exit /b %errorlevel%
)
echo [ByteForce-Java] Code formatted successfully.
pause