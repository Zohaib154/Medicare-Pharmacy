@echo off
title MediCare Pharmacy Management System - Desktop Launcher
cls
echo =======================================================================
echo          MediCare Pharmacy Management System - Desktop Launcher
echo =======================================================================
echo.

:: Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java is not installed or not in system PATH.
    echo Please install JDK 17 or higher to run this application.
    pause
    exit /b 1
)

set "PROJECT_DIR=%~dp0"
set "JAR=%PROJECT_DIR%target\medicare-system-1.0.0.jar"
set "JAVAFX_MODPATH=%PROJECT_DIR%target\javafx-lib"

:: Locate Maven
set "MAVEN_CMD=mvn"
if exist "%PROJECT_DIR%maven-dist\apache-maven-3.9.6\bin\mvn.cmd" (
    set "MAVEN_CMD=%PROJECT_DIR%maven-dist\apache-maven-3.9.6\bin\mvn.cmd"
)

if not exist "%JAR%" (
    echo [INFO] JAR not found. Building MediCare...
    call "%MAVEN_CMD%" -f "%PROJECT_DIR%pom.xml" clean package -DskipTests
    if %errorlevel% neq 0 (
        echo.
        echo [ERROR] Build compilation failed. Please review error messages above.
        pause
        exit /b 1
    )
)

if not exist "%JAVAFX_MODPATH%" (
    echo [INFO] JavaFX modules missing. Rebuilding MediCare package...
    call "%MAVEN_CMD%" -f "%PROJECT_DIR%pom.xml" package -DskipTests
    if %errorlevel% neq 0 (
        echo.
        echo [ERROR] Build compilation failed. Please review error messages above.
        pause
        exit /b 1
    )
)

:run_loop
echo.
echo [INFO] Launching MediCare desktop application...
echo.

java --module-path "%JAVAFX_MODPATH%" ^
  --add-modules javafx.controls,javafx.web,javafx.graphics,javafx.base,javafx.media ^
  --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED ^
  --add-opens javafx.web/com.sun.webkit=ALL-UNNAMED ^
  -jar "%JAR%"

if %errorlevel% equ 3 (
    echo.
    echo [INFO] Restarting database and application services...
    echo.
    goto run_loop
)

if %errorlevel% neq 0 (
    echo.
    echo [WARNING] Application terminated with exit code %errorlevel%.
    pause
)


