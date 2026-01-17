@echo off
setlocal

:: Check for virtual environment
if not exist "venv" (
    echo [ByteForce] Virtual environment not found. Please run run.bat first.
    pause
    exit /b 1
)

echo [ByteForce] Running Black formatter...
venv\Scripts\black.exe cli.py core tests

echo.
echo [ByteForce] Linting complete.
pause
