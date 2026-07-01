@echo off
title MediCare - Application Launcher
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "LAUNCHER=%PROJECT_DIR%MediCareLauncher.exe"

:: First, check if the launcher exe exists
if exist "%LAUNCHER%" (
    :: Use the compiled launcher exe
    start "" "%LAUNCHER%"
    exit /b 0
)

:: If launcher doesn't exist, check if we need to build it
echo MediCareLauncher.exe not found. Attempting to build...
echo.

:: Try to compile C# launcher
set "CSC=C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
set "LAUNCHER_CS=%PROJECT_DIR%MediCareLauncher.cs"

if exist "%CSC%" (
    if exist "%LAUNCHER_CS%" (
        echo Compiling MediCareLauncher.exe...
        "%CSC%" /target:winexe /out:"%LAUNCHER%" /r:System.Windows.Forms.dll /win32icon:"%PROJECT_DIR%src\main\resources\rxpro.ico" "%LAUNCHER_CS%"
        if !errorlevel! equ 0 (
            echo Compilation successful!
            timeout /t 1 /nobreak >nul
            start "" "%LAUNCHER%"
            exit /b 0
        )
    )
)

:: Fallback to Maven compilation if exe compilation failed
echo Could not build launcher exe. Using Maven build instead...
call "%PROJECT_DIR%run.bat"


