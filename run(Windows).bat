@echo off
setlocal
cd /d %~dp0

set SRC=src
set OUT=out
set LIB=lib
set RES=resources

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo Compiling...
javac ^
  --module-path "%LIB%" --add-modules javafx.controls,javafx.graphics,javafx.base ^
  -cp "%LIB%\*" ^
  -d "%OUT%" ^
  "%SRC%\*.java

if errorlevel 1 (
  echo.
  echo Compile failed.
  pause
  exit /b 1
)

echo Copying resources...
xcopy "%RES%\*" "%OUT%\" /E /I /Y >nul

echo Running...
java ^
  -Djava.library.path=javafx-bin ^
  -Dprism.order=sw ^
  --enable-native-access=javafx.graphics ^
  --module-path "%LIB%" --add-modules javafx.controls,javafx.graphics,javafx.base ^
  -cp "%LIB%\*;%OUT%" ^
  Clusterle



pause
endlocal
