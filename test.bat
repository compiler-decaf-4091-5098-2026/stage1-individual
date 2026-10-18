@echo off
chcp 65001 >nul
if not exist build\decaf.exe (
    echo [ERROR] Compiler not found. Please run build.bat first.
    exit /b 1
)

echo === Testing Valid Programs ===
for %%f in (tests\valid*.decaf) do (
    build\decaf.exe -t parse "%%f" >nul 2>&1
    if errorlevel 1 (
        echo [FAIL] %%f ^(should compile, but failed^)
    ) else (
        echo [OK] %%f
    )
)

echo.
echo === Testing Invalid Programs ===
for %%f in (tests\invalid*.decaf) do (
    build\decaf.exe -t parse "%%f" >nul 2>&1
    if errorlevel 1 (
        echo [OK] %%f
    ) else (
        echo [FAIL] %%f ^(should fail, but compiled^)
    )
)
echo.
pause