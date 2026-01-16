@echo off
setlocal

:: Check for virtual environment
if not exist "venv" (
    echo [ByteForce] Virtual environment not found. Creating 'venv'...
    python -m venv venv
    
    echo [ByteForce] Installing exact dependencies from requirements.txt...
    venv\Scripts\python.exe -m pip install -r requirements.txt
    
    if %errorlevel% neq 0 (
        echo [Error] Installation failed.
        pause
        exit /b %errorlevel%
    )
    echo [ByteForce] Setup complete.
    echo.
)

:: Run the tests
set PYTHONPATH=.
venv\Scripts\pytest.exe tests/
pause
