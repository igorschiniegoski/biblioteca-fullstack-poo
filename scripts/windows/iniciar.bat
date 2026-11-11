@echo off
setlocal
chcp 65001 >nul
title Biblioteca - iniciando
rem =====================================================================
rem  Sobe o sistema completo no Windows (para demonstracao):
rem    MySQL proprio do projeto (porta 3307) + dados de exemplo novos
rem    Backend Spring Boot (porta 8080) + Frontend React (porta 5173)
rem  Requisitos: JDK 26, Node.js e MySQL Server instalados (ver README).
rem =====================================================================

set "RAIZ=%~dp0..\.."
for %%i in ("%RAIZ%") do set "RAIZ=%%~fi"
set "DADOS=%USERPROFILE%\biblioteca-mysql"
set "PORTA_DB=3307"

call :acharJava || goto fim

set "MYSQL_BIN="
for /d %%d in ("%ProgramFiles%\MySQL\MySQL Server *") do set "MYSQL_BIN=%%~fd\bin"
if not defined MYSQL_BIN (
  echo [ERRO] MySQL Server nao encontrado. Instale com: winget install Oracle.MySQL
  goto fim
)
where npm >nul 2>&1
if errorlevel 1 (
  echo [ERRO] Node.js nao encontrado. Instale com: winget install OpenJS.NodeJS.LTS
  goto fim
)

echo Java:  %JAVA_HOME%
echo MySQL: %MYSQL_BIN%
echo.
echo === 1/4 Banco de dados - MySQL na porta %PORTA_DB% ===
if not exist "%DADOS%\data" (
  echo Criando o banco local em %DADOS% - so na primeira vez...
  mkdir "%DADOS%" 2>nul
  "%MYSQL_BIN%\mysqld.exe" --initialize-insecure --datadir="%DADOS%\data"
  if errorlevel 1 goto erro
)
"%MYSQL_BIN%\mysqladmin.exe" -u root -h 127.0.0.1 -P %PORTA_DB% ping >nul 2>&1
if errorlevel 1 (
  start "MySQL - nao feche" /min "%MYSQL_BIN%\mysqld.exe" --datadir="%DADOS%\data" --port=%PORTA_DB% --bind-address=127.0.0.1 --mysqlx=OFF --character-set-server=utf8mb4 --console
)
set /a n=0
:esperaBanco
"%MYSQL_BIN%\mysqladmin.exe" -u root -h 127.0.0.1 -P %PORTA_DB% ping >nul 2>&1
if not errorlevel 1 goto bancoOk
set /a n+=1
if %n% geq 60 (
  echo [ERRO] O MySQL nao iniciou. Veja a janela "MySQL - nao feche".
  goto fim
)
ping -n 2 127.0.0.1 >nul
goto esperaBanco
:bancoOk

echo === 2/4 Criando tabelas - schema.sql - e dados de exemplo ===
"%MYSQL_BIN%\mysql.exe" -u root -h 127.0.0.1 -P %PORTA_DB% --default-character-set=utf8mb4 < "%RAIZ%\database\schema.sql"
if errorlevel 1 goto erro
"%MYSQL_BIN%\mysql.exe" -u root -h 127.0.0.1 -P %PORTA_DB% --default-character-set=utf8mb4 biblioteca < "%RAIZ%\database\dados-iniciais.sql"
if errorlevel 1 goto erro
"%MYSQL_BIN%\mysql.exe" -u root -h 127.0.0.1 -P %PORTA_DB% -e "CREATE USER IF NOT EXISTS 'biblioteca'@'localhost' IDENTIFIED BY 'biblioteca'; CREATE USER IF NOT EXISTS 'biblioteca'@'127.0.0.1' IDENTIFIED BY 'biblioteca'; GRANT ALL ON biblioteca.* TO 'biblioteca'@'localhost'; GRANT ALL ON biblioteca.* TO 'biblioteca'@'127.0.0.1';"
if errorlevel 1 goto erro

echo === 3/4 Backend - Spring Boot, porta 8080 ===
set "DB_URL=jdbc:mysql://127.0.0.1:%PORTA_DB%/biblioteca?serverTimezone=America/Sao_Paulo"
set "PATH=%JAVA_HOME%\bin;%PATH%"
start "Backend - nao feche" /d "%RAIZ%\backend" cmd /k mvnw.cmd spring-boot:run

echo === 4/4 Frontend - React, porta 5173 ===
if not exist "%RAIZ%\frontend\node_modules" (
  echo Instalando dependencias do frontend - so na primeira vez...
  pushd "%RAIZ%\frontend"
  call npm install --no-fund --no-audit
  popd
)
start "Frontend - nao feche" /d "%RAIZ%\frontend" cmd /k npm run dev

echo.
echo Aguardando o backend responder. Na primeira vez pode levar alguns minutos - download do Maven e das dependencias...
set /a n=0
:esperaBackend
ping -n 4 127.0.0.1 >nul
curl -s -o nul http://localhost:8080/api/livros
if not errorlevel 1 goto pronto
set /a n+=1
if %n% lss 200 goto esperaBackend
echo [ERRO] O backend nao respondeu. Veja a janela "Backend - nao feche".
goto fim

:pronto
start "" http://localhost:5173
echo.
echo ============================================================
echo  Tudo no ar: http://localhost:5173
echo  Dados de exemplo: Ana regular, Bruno atrasado, Carla com multa.
echo  Para desligar: scripts\windows\parar.bat
echo ============================================================
goto fim

:erro
echo.
echo [ERRO] Falha ao preparar o banco. Veja a mensagem acima.

:fim
echo.
pause
exit /b

rem ---------------------------------------------------------------------
:acharJava
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
  "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /r /c:"version .26" >nul && exit /b 0
)
set "JAVA_HOME="
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\jdk-26*" "%ProgramFiles%\Java\jdk-26*" "%ProgramFiles%\Microsoft\jdk-26*" "%USERPROFILE%\dev\jdk-26*" "%USERPROFILE%\.jdks\*26*") do (
  if exist "%%~fd\bin\java.exe" set "JAVA_HOME=%%~fd"
)
if defined JAVA_HOME exit /b 0
for /f "delims=" %%j in ('where java 2^>nul') do (
  "%%j" -version 2>&1 | findstr /r /c:"version .26" >nul && for %%k in ("%%~dpj..") do set "JAVA_HOME=%%~fk"
)
if defined JAVA_HOME exit /b 0
echo [ERRO] JDK 26 nao encontrado. Instale com: winget install EclipseAdoptium.Temurin.26.JDK
exit /b 1
