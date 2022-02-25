@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

if not defined HADOOP_HOME (
  set EXITCODE=5
  goto :errorexit
)
if not defined HIVE_HOME (
  set EXITCODE=6
  goto :errorexit
)
if not defined TEZ_HOME (
  set EXITCODE=7
  goto :errorexit
)

set EXITCODE=0

if not exist %HIVE_HOME%\conf\hive-tez-configured (
  %HADOOP_HOME%\bin\hadoop.cmd fs -mkdir /apps/tez
  set EXITCODE=%ERRORLEVEL%
  if %EXITCODE% neq 0 goto :errorexit

  %HADOOP_HOME%\bin\hadoop.cmd fs -chmod -R 755 /apps/tez
  set EXITCODE=%ERRORLEVEL%
  if %EXITCODE% neq 0 goto :errorexit

  %HADOOP_HOME%\bin\hadoop.cmd fs -chown -R hadoop:users /apps/tez
  set EXITCODE=%ERRORLEVEL%
  if %EXITCODE% neq 0 goto :errorexit

  %HADOOP_HOME%\bin\hadoop.cmd fs -put %TEZ_HOME%\* /apps/tez
  set EXITCODE=%ERRORLEVEL%
  if %EXITCODE% neq 0 goto :errorexit

  %HADOOP_HOME%\bin\hadoop.cmd fs -rm -r -skipTrash /apps/tez/conf
  set EXITCODE=%ERRORLEVEL%
  if %EXITCODE% neq 0 goto :errorexit

  echo done > %HIVE_HOME%\conf\hive-tez-configured
)
goto :eof

:errorexit
exit /B %EXITCODE%
