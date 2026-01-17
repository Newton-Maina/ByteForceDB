@echo off
setlocal

:: Change to the directory where this script is located
cd /d "%~dp0"

echo [ByteForce-Java] Running tests...
call mvn test

if %errorlevel% neq 0 (
    echo [Error] Tests failed.
    pause
    exit /b %errorlevel%
)
pause