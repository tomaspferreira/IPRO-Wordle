@echo off
setlocal
cd /d "%~dp0"

set SRC=src
set OUT=out
set LIB=lib
set RES=resources
set JFXBIN=javafx-bin
set MAIN=Clusterle

REM Hunspell native DLL location (adjust if your dll is elsewhere)
set HUNNATIVE=win32-x86-64

echo Compiling...
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

javac ^
  --module-path "%LIB%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.web,javafx.swing ^
  -cp "%LIB%\*" ^
  -d "%OUT%" ^
  "%SRC%\*.java"

if errorlevel 1 (
  echo Compile failed.
  pause
  exit /b 1
)

echo Copying resources...
if exist "%RES%" xcopy "%RES%\*" "%OUT%\" /E /I /Y >nul

echo Running...
java ^
  -Djava.library.path="%JFXBIN%" ^
  -Djna.library.path="%HUNNATIVE%" ^
  -Dprism.order=sw ^
  --enable-native-access=ALL-UNNAMED ^
  --module-path "%LIB%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.web,javafx.swing ^
  -cp "%LIB%\*;%OUT%" ^
  %MAIN%

pause
endlocal
