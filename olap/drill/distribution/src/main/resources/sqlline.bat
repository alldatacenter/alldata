@REM
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing, software
@REM distributed under the License is distributed on an "AS IS" BASIS,
@REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM See the License for the specific language governing permissions and
@REM limitations under the License.
@REM

@echo off
setlocal EnableExtensions EnableDelayedExpansion

rem ----
rem Sets Drill home and bin dirs before shift is applied
rem to preserve correct paths
rem ----

set DRILL_BIN_DIR=%~dp0
pushd %DRILL_BIN_DIR%..
set DRILL_HOME=%cd%
popd

rem ----
rem In order to pass in arguments with an equals symbol, use quotation marks.
rem For example
rem sqlline -u "jdbc:drill:zk=local" -n admin -p admin
rem ----

rem ----
rem Deal with command-line arguments
rem ----

:argactionstart
if -%1-==-- goto argactionend

set atleastonearg=0

if x%1 == x-q (
  set QUERY=%2
  set QUERY=!QUERY:"=!
  set atleastonearg=1
  shift
  shift
)

if x%1 == x-e (
  set QUERY=%2
  set QUERY=!QUERY:"=!
  set atleastonearg=1
  shift
  shift
)

if x%1 == x-f (
  set FILE=%2
  set FILE=!FILE:"=!
  set atleastonearg=1
  shift
  shift
)

if x%1 == x--config (
  set confdir=%2
  set DRILL_CONF_DIR=%2
  set DRILL_CONF_DIR=!DRILL_CONF_DIR:"=!
  set atleastonearg=1
  shift
  shift
)

if x%1 == x--jvm (
  set DRILL_SHELL_JAVA_OPTS=!DRILL_SHELL_JAVA_OPTS! %2
  set DRILL_SHELL_JAVA_OPTS=!DRILL_SHELL_JAVA_OPTS:"=!
  set atleastonearg=1
  shift
  shift
)

if "!atleastonearg!"=="0" (
  set DRILL_ARGS=!DRILL_ARGS! %~1
  shift
)

goto argactionstart
:argactionend

rem ----
rem Validate JAVA_HOME
rem ----
if "%JAVA_EXE%" == "" (set JAVA_EXE=java.exe)

if not "%JAVA_HOME%" == "" goto javaHomeSet
echo.
echo WARN: JAVA_HOME not found in your environment.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto initDrillEnv

:javaHomeSet
if exist "%JAVA_HOME%\bin\%JAVA_EXE%" goto initDrillEnv
echo.
echo ERROR: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = %JAVA_HOME%
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation
echo.
goto error

:initDrillEnv

rem ----
rem Deal with Drill variables
rem ----

if "test%DRILL_CONF_DIR%" == "test" (
  set DRILL_CONF_DIR=%DRILL_HOME%\conf
)

if "test%DRILL_LOG_DIR%" == "test" (
  set DRILL_LOG_DIR=%DRILL_HOME%\log
)

rem Drill temporary directory is used as base for temporary storage of Dynamic UDF jars
if "test%DRILL_TMP_DIR%" == "test" (
  set DRILL_TMP_DIR=%TEMP%
)

rem ----
rem Deal with Hadoop JARs, if HADOOP_HOME was specified
rem ----

if "test%HADOOP_HOME%" == "test" (
  rem HADOOP_HOME not detected...
  set USE_HADOOP_CP=0
  set HADOOP_HOME=%DRILL_HOME%\winutils
) else (
  rem Calculating HADOOP_CLASSPATH ...
  for %%i in (%HADOOP_HOME%\lib\*.jar) do (
    set IGNOREJAR=0
    for /F "tokens=*" %%A in (%DRILL_BIN_DIR%\hadoop-excludes.txt) do (
      echo.%%~ni|findstr /C:"%%A" >nul 2>&1
      if not errorlevel 1 set IGNOREJAR=1
    )
    if "!IGNOREJAR!"=="0" set HADOOP_CLASSPATH=%%i;!HADOOP_CLASSPATH!
  )
  set HADOOP_CLASSPATH=%HADOOP_HOME%\conf;!HADOOP_CLASSPATH!
  set USE_HADOOP_CP=1
)
set PATH=!HADOOP_HOME!\bin;!PATH!

rem ----
rem Deal with HBase JARs, if HBASE_HOME was specified
rem ----

if "test%HBASE_HOME%" == "test" (
  rem HBASE_HOME not detected...
  set USE_HBASE_CP=0
) else (
  rem Calculating HBASE_CLASSPATH ...
  for %%i in (%HBASE_HOME%\lib\*.jar) do (
    set IGNOREJAR=0
    for /F "tokens=*" %%A in (%DRILL_BIN_DIR%\hadoop-excludes.txt) do (
      echo.%%~ni|findstr /C:"%%A" >nul 2>&1
      if not errorlevel 1 set IGNOREJAR=1
    )
    if "!IGNOREJAR!"=="0" set HBASE_CLASSPATH=%%i;!HBASE_CLASSPATH!
  )
  set HBASE_CLASSPATH=%HADOOP_HOME%\conf;!HBASE_CLASSPATH!
  set USE_HBASE_CP=1
)

rem Calculating Drill classpath...

set DRILL_CP=%DRILL_CONF_DIR%
if NOT "test%DRILL_CLASSPATH_PREFIX%"=="test" set DRILL_CP=!DRILL_CP!;%DRILL_CLASSPATH_PREFIX%
set DRILL_CP=%DRILL_CP%;%DRILL_HOME%\jars\*
set DRILL_CP=%DRILL_CP%;%DRILL_HOME%\jars\ext\*
if "test%USE_HADOOP_CP%"=="test1" set DRILL_CP=!DRILL_CP!;%HADOOP_CLASSPATH%
if "test%USE_HBASE_CP%"=="test1" set DRILL_CP=!DRILL_CP!;%HBASE_CLASSPATH%
set DRILL_CP=%DRILL_CP%;%DRILL_HOME%\jars\3rdparty\*
set DRILL_CP=%DRILL_CP%;%DRILL_HOME%\jars\classb\*
if NOT "test%DRILL_CLASSPATH%"=="test" set DRILL_CP=!DRILL_CP!;%DRILL_CLASSPATH%

set DRILL_SHELL_JAVA_OPTS=%DRILL_SHELL_JAVA_OPTS% -Dlog.path="%DRILL_LOG_DIR%\sqlline.log" -Dlog.query.path="%DRILL_LOG_DIR%\sqlline_queries.log"

SET JAVA_CMD=%JAVA_HOME%\bin\%JAVA_EXE%
if "%JAVA_HOME%" == "" (set JAVA_CMD=%JAVA_EXE%)
set ERROR_CODE=0

rem Check that java is newer than 1.8
"%JAVA_CMD%" -version 2>&1 | findstr "1.8" > nul 2>&1
if errorlevel 1 (
  rem allow reflective access on Java 9+
  set DRILL_SHELL_JAVA_OPTS=!DRILL_SHELL_JAVA_OPTS! ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/java.util=ALL-UNNAMED ^
    --add-opens java.base/sun.nio.ch=ALL-UNNAMED ^
    --add-opens java.base/java.net=ALL-UNNAMED ^
    --add-opens java.base/java.nio=ALL-UNNAMED ^
    --add-opens java.security.jgss/sun.security.krb5=ALL-UNNAMED
)

set SQLLINE_CALL=sqlline.SqlLine -ac org.apache.drill.exec.client.DrillSqlLineApplication
if NOT "test%QUERY%"=="test" (
  "%JAVA_CMD%" %DRILL_SHELL_JAVA_OPTS% %DRILL_JAVA_OPTS% -cp "%DRILL_CP%" %SQLLINE_CALL% %DRILL_ARGS% -e "%QUERY%"
) else (
  if NOT "test%FILE%"=="test" (
    "%JAVA_CMD%" %DRILL_SHELL_JAVA_OPTS% %DRILL_JAVA_OPTS% -cp "%DRILL_CP%" %SQLLINE_CALL% %DRILL_ARGS% --run="%FILE%"
  ) else (
    "%JAVA_CMD%" %DRILL_SHELL_JAVA_OPTS% %DRILL_JAVA_OPTS% -cp "%DRILL_CP%" %SQLLINE_CALL% %DRILL_ARGS%
  )
)
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
endlocal
exit /B %ERROR_CODE%
