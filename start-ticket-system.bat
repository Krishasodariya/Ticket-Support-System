@echo off
title Ticket Support System Start

cd /d "%~dp0"

echo =====================================
echo Ticket Support System
echo =====================================
echo.

REM Check Java
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java wurde nicht gefunden.
    echo Bitte Java JDK 21 installieren und JAVA_HOME/PATH konfigurieren.
    pause
    exit /b 1
)

REM Check Maven
where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven wurde nicht gefunden.
    echo Bitte Maven installieren und zum PATH hinzufuegen.
    pause
    exit /b 1
)

echo Java-Version:
java -version
echo.

echo Maven-Version:
call mvn -version
echo.

echo =====================================
echo Projekt wird gebaut...
echo =====================================
call mvn clean install -DskipTests

if errorlevel 1 (
    echo.
    echo [ERROR] Der Maven-Build ist fehlgeschlagen.
    pause
    exit /b 1
)

echo.
echo =====================================
echo Backend wird gestartet...
echo =====================================
start "Ticket Backend" cmd /k "mvn -f backend/pom.xml spring-boot:run"

echo.
echo Warte auf Backend...
timeout /t 10 /nobreak >nul

echo.
echo =====================================
echo Frontend wird gestartet...
echo =====================================
start "Ticket Frontend" cmd /k "mvn -f frontend/pom.xml javafx:run"

echo.
echo Ticket Support System wurde gestartet.
exit /b 0
