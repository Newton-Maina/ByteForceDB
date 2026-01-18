@echo off
SETLOCAL EnableDelayedExpansion

echo ========================================
echo   ByteForceDB - Java Web Interface
echo ========================================

cd /d "%~dp0"

echo [1/3] Cleaning and Compiling...
:: Perform a deep clean to remove any old ATN version 3 classes
call mvn clean compile dependency:copy-dependencies -q
if %ERRORLEVEL% NEQ 0 (
    echo [!] Build failed.
    pause
    exit /b
)

echo [2/3] Preparing Classpath...
set "CP=target\classes;target\dependency\*"

echo [3/3] Starting Spark Java Server...
echo.
echo URL: http://localhost:4567
echo Press Ctrl+C to stop.
echo.

java -cp "%CP%" com.byteforce.web.WebApp
pause
