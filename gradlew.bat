@echo off
setlocal
set DIR=%~dp0
if exist "%DIR%gradle" goto run
if exist "%DIR%..\gradle" goto run

echo Gradle wrapper is not available in this workspace.
echo Please install Gradle or run the Android project from an environment with the wrapper.
exit /b 1

:run
set GRADLE_EXE=%DIR%gradle
if not exist "%GRADLE_EXE%" set GRADLE_EXE=%DIR%..\gradle
if not exist "%GRADLE_EXE%" (
  echo Gradle executable not found.
  exit /b 1
)

"%GRADLE_EXE%" %*
