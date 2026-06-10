@echo off
title Ticket Support System Update Start

cd /d "%~dp0"

echo =====================================
echo GitHub Update wird geladen
echo =====================================

git pull

echo.
echo Backend wird vorbereitet...
call mvn -f backend/pom.xml clean install -DskipTests

echo.
echo Backend wird gestartet...
start "Ticket Backend" cmd /k "mvn -f backend/pom.xml spring-boot:run"

echo.
echo Warte auf Backend...
timeout /t 10 /nobreak > nul

echo.
echo Frontend wird gestartet...
start "Ticket Frontend" cmd /k "mvn -f frontend/pom.xml javafx:run"

exit