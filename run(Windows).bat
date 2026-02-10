@echo off
setlocal
cd /d %~dp0

REM =========================
REM 1) Check for Java + Javac
REM =========================
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java not found.
  echo Please install a JDK (recommended: Temurin JDK 21) then restart your PC.
  echo After install, make sure "java -version" works in cmd.
  pause
  exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
  echo ERROR: javac not found (JDK required).
  echo You have Java runtime, but not a full JDK in PATH.
  echo Install a JDK (recommended: Temurin JDK 21) then restart.
  echo After install, make sure "javac -version" works in cmd.
  pause
  exit /b 1
)

REM =========================
REM 2) Settings
REM =========================
set SRC=src
set OUT=out
set LIB=lib
set RES=resources
set JFXBIN=javafx-bin
set MAIN=Clusterle

REM =========================
REM 3) Validate folders
REM =========================
if not exist "%LIB%" (
  echo ERROR: Missing folder "%LIB%".
  pause
  exit /b 1
)

if not exist "%JFXBIN%" (
  echo ERROR: Missing folder "%JFXBIN%" (JavaFX native dlls).
  echo Make sure you committed the JavaFX bin dlls into "%JFXBIN%".
  pause
  exit /b 1
)

REM =========================
REM 4) Clean + Compile
REM =========================
echo Compiling...
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

javac ^
  --module-path "%LIB%" ^
  --add-modules javafx.controls,javafx.graphics,javafx.base ^
  -cp "%LIB%\*" ^
  -d "%OUT%" ^
  "%SRC%\*.java"

if errorlevel 1 (
  echo.
  echo Compile failed.
  pause
  exit /b 1
)

REM =========================
REM 5) Copy resources
REM =========================
if exist "%RES%" (
  echo Copying resources...
  xcopy "%RES%\*" "%OUT%\" /E /I /Y >nul
)

REM =========================
REM 6) Run
REM =========================
echo Running...
java ^
  -Djava.library.path="%JFXBIN%" ^
  -Dprism.order=sw ^
  --enable-native-access=javafx.graphics ^
  --enable-native-access=ALL-UNNAMED ^
  --module-path "%LIB%" ^
  --add-modules javafx.controls,javafx.graphics,javafx.base ^
  -cp "%LIB%\*;%OUT%" ^
  %MAIN%

pause
endlocal
