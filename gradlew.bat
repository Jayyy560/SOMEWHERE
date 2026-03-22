@REM ---------------------------------------------------------------------------
@REM Gradle startup script for Windows
@REM ---------------------------------------------------------------------------

@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL

set APP_HOME=%~dp0

set DEFAULT_JVM_OPTS=

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper-main.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar;%APP_HOME%gradle\wrapper\gradle-cli.jar;%APP_HOME%gradle\wrapper\gradle-files.jar

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
goto fail

:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto execute
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
goto fail

:execute
set CMD_LINE_ARGS=
set _SKIP=2
:setupArgs
if "%1"=="" goto doneArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setupArgs
:doneArgs

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% -Dorg.gradle.appname=Gradle -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
if %ERRORLEVEL% neq 0 goto fail

:end
@ENDLOCAL
goto eof

:fail
@ENDLOCAL
exit /b 1

:eof
