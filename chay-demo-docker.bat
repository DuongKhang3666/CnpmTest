@echo off
rem Chay demo bang Docker (MySQL + Backend trong Docker, web bang npm). Xem chi tiet trong chay-demo-docker.ps1
rem   chay-demo-docker.bat              chay demo
rem   chay-demo-docker.bat -Translate   chay kem dich may LibreTranslate
rem   chay-demo-docker.bat -Stop        tat MySQL + Backend
title Chay demo - He thong thuyet minh da ngon ngu
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0chay-demo-docker.ps1" %*
