@echo off
setlocal

:: Check for virtual environment
if not exist "venv" (
    echo [ByteForce] Virtual environment not found. Creating 'venv'...
    python -m venv venv
    
    echo [ByteForce] Installing exact dependencies from requirements.txt...
    venv\Scripts\python.exe -m pip install -r requirements.txt
    
    if %errorlevel% neq 0 (
        echo [Error] Installation failed. Please ensure Python is installed and requirements.txt is present.
        pause
        exit /b %errorlevel%
    )
    echo [ByteForce] Setup complete.
    echo.
)

:: Run the application
set PYTHONPATH=.
venv\Scripts\python.exe cli.py
pause