@echo off
SETLOCAL EnableDelayedExpansion

echo ========================================
echo   ByteForceDB - Web Interface Launcher
echo ========================================

cd /d "%~dp0"

:: Check if venv exists
if not exist "venv\Scripts\python.exe" (
    echo [!] Virtual environment not found. Creating it...
    python -m venv venv
    venv\Scripts\pip install -r requirements.txt
)

echo [1/2] Database check...
:: The app handles its own table initialization

echo [2/2] Starting Flask server...
echo.
echo URL: http://127.0.0.1:5000
echo Press Ctrl+C to stop.
echo.

venv\Scripts\python webapp.py
pause
