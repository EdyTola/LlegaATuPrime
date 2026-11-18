@echo off
setlocal
cd /d "%~dp0"

rem --- JDK 17+ (usa JAVA_HOME si ya esta definido; si no, el JBR de PyCharm) ---
if not defined JAVA_HOME (
  if exist "C:\Program Files\JetBrains\PyCharm 2026.2.2\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\JetBrains\PyCharm 2026.2.2\jbr"
  )
)

rem --- Maven (PATH si esta instalado; si no, el portable ya descargado) ---
set "MVN_CMD=mvn.cmd"
where mvn.cmd >nul 2>nul
if errorlevel 1 set "MVN_CMD=%TEMP%\symmetry-buildtools\apache-maven-3.9.9\bin\mvn.cmd"

if not exist "%MVN_CMD%" if /i not "%MVN_CMD%"=="mvn.cmd" (
  echo [ERROR] No se encontro Maven.
  echo Instala Maven ^(https://maven.apache.org/download.cgi^) o descarga el portable en:
  echo   %TEMP%\symmetry-buildtools\apache-maven-3.9.9
  pause
  exit /b 1
)

echo Compilando y ejecutando Llega a tu Prime...
if /i "%MVN_CMD%"=="mvn.cmd" (
  call mvn.cmd javafx:run
) else (
  call "%MVN_CMD%" javafx:run
)

if errorlevel 1 (
  echo.
  echo [ERROR] La ejecucion fallo. Revisa los mensajes anteriores.
  pause
)
endlocal
