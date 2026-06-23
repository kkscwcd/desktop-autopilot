@echo off
:: Desktop Autopilot launcher for Windows
:: Usage: run.bat [options]   -- see --help for all options
::
:: Common examples:
::   run.bat                                              -- default (auto native input, minimal profile)
::   run.bat --input-mode native                          -- force hardware-level input
::   run.bat --keyboard-jiggle                            -- enable random key presses (F15/Up/Down/Shift)
::   run.bat --profile stealth                            -- stealth profile
::   run.bat --schedule --start 09:00 --end 18:00         -- working-hours only
::   run.bat --input-mode native --keyboard-jiggle --smooth --mouse-click
::   run.bat --profile keep-awake --prevent-sleep

setlocal

set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%target\mouse-jiggler-1.0.0.jar"

if exist "%JAR%" goto run

echo JAR not found. Building first...

:: Try Maven wrapper first
set "MVN="
for /f "delims=" %%i in ('dir /s /b "%USERPROFILE%\.m2\wrapper\dists\*\bin\mvn.cmd" 2^>nul ^| sort /r') do (
    if not defined MVN set "MVN=%%i"
)

:: Fall back to mvn on PATH
if not defined MVN (
    where mvn >nul 2>&1 && set "MVN=mvn"
)

if not defined MVN (
    echo Maven not found. Install Maven or run: mvn package
    exit /b 1
)

pushd "%SCRIPT_DIR%"
"%MVN%" -q package
if errorlevel 1 (
    echo Build failed.
    popd
    exit /b 1
)
popd

:run
java -jar "%JAR%" %*
