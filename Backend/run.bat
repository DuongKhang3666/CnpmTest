@echo off
setlocal
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven is not installed or not on PATH.
  echo Install Maven 3.9+ and JDK 17+, then run this file again.
  pause
  exit /b 1
)
mvn clean spring-boot:run
pause
