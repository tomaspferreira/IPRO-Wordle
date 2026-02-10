@echo off
setlocal

cd /d "%~dp0"

REM ---- Validate JAVA_HOME ----
if "%JAVA_HOME%"=="" (
  echo ERROR: JAVA_HOME is not set.
  pause
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo ERROR: "%JAVA_HOME%\bin\javac.exe" not found. Fix JAVA_HOME.
  pause
  exit /b 1
)

REM ---- Clean output ----
rmdir /s /q out 2>nul
mkdir out

if not exist dist mkdir dist

REM ---- Collect all sources (recursively) ----
dir /s /b src\*.java > sources.txt

REM ---- Compile ----
"%JAVA_HOME%\bin\javac.exe" -d out -cp "lib\*" @sources.txt
if errorlevel 1 (
  echo.
  echo COMPILATION FAILED
  del sources.txt
  pause
  exit /b 1
)
del sources.txt

REM ---- Copy resources into OUT ROOT (so they end up at jar root) ----
xcopy /E /I /Y resources out >nul

REM ---- Build jar ----
del /q dist\clusterle.jar 2>nul
"%JAVA_HOME%\bin\jar.exe" --create --file dist\clusterle.jar --main-class Clusterle -C out .

echo Done: dist\clusterle.jar
pause
