@echo off
chcp 65001 >nul
if not exist build\decaf.exe (
    echo [ERROR] Compiler not found. Please run build.bat first.
    exit /b 1
)
build\decaf.exe %*