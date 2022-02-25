@echo off
@rem Licensed to the Apache Software Foundation (ASF) under one or more
@rem contributor license agreements.  See the NOTICE file distributed with
@rem this work for additional information regarding copyright ownership.
@rem The ASF licenses this file to You under the Apache License, Version 2.0
@rem (the "License"); you may not use this file except in compliance with
@rem the License.  You may obtain a copy of the License at
@rem
@rem     http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.set ARTIFACT_DIR=artifacts
@echo on

IF dummy==dummy%1 (
SET SCOM_VERSION="1.0.0"
) ELSE (
SET SCOM_VERSION=%1
)

echo SCOM_VERSION = %SCOM_VERSION%

set ARTIFACT_DIR=artifacts
set TEMP_DIR=%ARTIFACT_DIR%\temp
set FINAL_ZIPS=%ARTIFACT_DIR%\zips
set SCOM_DIR=%TEMP_DIR%\server
set SINK_DIR=%TEMP_DIR%\metrics-sink
set MP_DIR=%TEMP_DIR%\mp

rmdir /s /q %ARTIFACT_DIR%

mkdir %FINAL_ZIPS% || exit /b 1
mkdir %SCOM_DIR% || exit /b 1
mkdir %MP_DIR% || exit /b 1
mkdir %SINK_DIR% || exit /b 1

copy ambari-scom-server\target\ambari-scom*.zip %SCOM_DIR% || exit /b 1
copy ambari-scom-server\target\ambari-scom*.jar %SCOM_DIR% || exit /b 1

copy metrics-sink\target\*.jar %SINK_DIR% || exit /b 1
copy metrics-sink\db\*.ddl %SINK_DIR% || exit /b 1

copy management-pack\Hadoop_MP\ManagementMp\bin\Debug\*.mpb %MP_DIR% || exit /b 1
copy management-pack\Hadoop_MP\HadoopMp\bin\Debug\*.mpb %MP_DIR% || exit /b 1
copy management-pack\Hadoop_MP\PresentationMp\bin\Debug\*.mpb %MP_DIR% || exit /b 1

copy msi\*.msi %FINAL_ZIPS% || exit /b 1
copy README.md %FINAL_ZIPS% || exit /b 1


powershell.exe -NoProfile -InputFormat none -ExecutionPolicy unrestricted -command "%cd%\msi\build\zip.ps1" "%SCOM_DIR%" "%FINAL_ZIPS%\server.zip" || exit /b 1
powershell.exe -NoProfile -InputFormat none -ExecutionPolicy unrestricted -command "%cd%\msi\build\zip.ps1" "%SINK_DIR%" "%FINAL_ZIPS%\metrics-sink.zip" || exit /b 1
powershell.exe -NoProfile -InputFormat none -ExecutionPolicy unrestricted -command "%cd%\msi\build\zip.ps1" "%MP_DIR%" "%FINAL_ZIPS%\mp.zip" || exit /b 1

powershell.exe -NoProfile -InputFormat none -ExecutionPolicy unrestricted -command "%cd%\msi\build\zip.ps1" "%FINAL_ZIPS%" "%ARTIFACT_DIR%\ambari-scom-%SCOM_VERSION%.zip" || exit /b 1
