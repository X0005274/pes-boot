@echo off
rem Console launcher (dev / maintenance). For production use the WinSW service.
setlocal
call "%~dp0env.bat"

set "JAR=%PES_HOME%\app\pes-app.jar"
if not exist "%JAR%" (
    echo [ERROR] jar not found: %JAR%
    exit /b 1
)

echo Starting PES (profile=%SPRING_PROFILES_ACTIVE%) ...
"%JAVA_HOME%\bin\java" %JAVA_OPTS% -jar "%JAR%" ^
    --spring.config.additional-location=optional:file:%PES_HOME%/config/

endlocal
