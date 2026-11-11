@echo off
setlocal
rem Desliga o frontend, o backend e o MySQL do projeto (porta 3307).
set "MYSQL_BIN="
for /d %%d in ("%ProgramFiles%\MySQL\MySQL Server *") do set "MYSQL_BIN=%%~fd\bin"

taskkill /FI "WINDOWTITLE eq Frontend - nao feche*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Backend - nao feche*" /T /F >nul 2>&1
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":8080 .*LISTENING"') do taskkill /PID %%p /T /F >nul 2>&1
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":5173 .*LISTENING"') do taskkill /PID %%p /T /F >nul 2>&1
if defined MYSQL_BIN "%MYSQL_BIN%\mysqladmin.exe" -u root -h 127.0.0.1 -P 3307 shutdown >nul 2>&1

echo Tudo desligado.
ping -n 3 127.0.0.1 >nul
