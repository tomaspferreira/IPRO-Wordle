@echo off
setlocal

cd /d "%~dp0"

set "APPNAME=Clusterle"
set "MAINJAR=clusterle.jar"
set "DISTDIR=dist"
set "OUTDIR=package"
set "BUILDDIR=build"
set "RUNTIMEDIR=%BUILDDIR%\runtime"
set "JFXJMODS=javafx-jmods"

echo Using JAVA_HOME="%JAVA_HOME%"

REM ---- Validate JAVA_HOME
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: JAVA_HOME is invalid
  pause
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\jlink.exe" (
  echo ERROR: jlink missing (need a full JDK, not JRE)
  pause
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\jpackage.exe" (
  echo ERROR: jpackage missing (need a full JDK)
  pause
  exit /b 1
)

REM ---- Validate inputs
if not exist "%DISTDIR%\%MAINJAR%" (
  echo ERROR: dist\%MAINJAR% missing. Run build-jar.bat first.
  pause
  exit /b 1
)

if not exist "%JAVA_HOME%\jmods\java.base.jmod" (
  echo ERROR: JDK jmods not found at "%JAVA_HOME%\jmods"
  pause
  exit /b 1
)

if not exist "%JFXJMODS%\javafx.base.jmod" (
  echo ERROR: JavaFX jmods not found. Put them in "%JFXJMODS%\"
  echo (Download "JMODs -> Windows/x64 zip" from the JavaFX release page.)
  pause
  exit /b 1
)

echo.
echo === Java version ===
"%JAVA_HOME%\bin\java.exe" -version
echo.

REM ---- Clean old output
if exist "%OUTDIR%" rmdir /s /q "%OUTDIR%"
if exist "%BUILDDIR%" rmdir /s /q "%BUILDDIR%"
mkdir "%BUILDDIR%"

echo.
echo === Building custom runtime with jlink (includes JavaFX) ===
"%JAVA_HOME%\bin\jlink.exe" ^
  --module-path "%JAVA_HOME%\jmods;javafx-jmods\javafx-jmods-25.0.2" ^
  --add-modules javafx.controls,javafx.graphics,javafx.base,javafx.fxml,javafx.media,javafx.web,java.desktop,java.sql,jdk.unsupported ^
  --strip-debug ^
  --no-header-files ^
  --no-man-pages ^
  --output build\runtime


if errorlevel 1 (
  echo jlink FAILED
  pause
  exit /b 1
)

echo.
echo === Running jpackage ===
"%JAVA_HOME%\bin\jpackage.exe" ^
  --type app-image ^
  --name Clusterle ^
  --input dist ^
  --main-jar clusterle.jar ^
  --dest package ^
  --runtime-image build\runtime ^
  --win-console ^
  --java-options "-Dprism.order=sw" ^
  --java-options "--enable-native-access=ALL-UNNAMED"

if errorlevel 1 (
  echo jpackage FAILED
  pause
  exit /b 1
)

echo.
echo SUCCESS!
echo Run: %OUTDIR%\%APPNAME%\%APPNAME%.exe
pause
