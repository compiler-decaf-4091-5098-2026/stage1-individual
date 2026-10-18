@echo off
chcp 65001 >nul
echo Building Decaf Compiler...
if not exist build mkdir build

g++ -std=c++17 -O2 -Wall src\*.cpp src\utils\*.cpp -o build\decaf.exe

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Build failed!
    exit /b %errorlevel%
)
echo [SUCCESS] Build successful! Executable: build\decaf.exe