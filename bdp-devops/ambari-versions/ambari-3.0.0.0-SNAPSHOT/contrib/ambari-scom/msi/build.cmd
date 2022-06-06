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


@echo on
echo Copying resources
pushd ..
copy /y "%cd%\ambari-scom-server\target\ambari-scom-*.*" "%cd%\msi\src\AmbariPackages\ambari-winpkg\resources\" || exit /b 1
copy /y "%cd%\metrics-sink\target\metrics-sink-*.jar" "%cd%\msi\src\AmbariPackages\ambari-winpkg\resources\" || exit /b 1
copy /y "%cd%\metrics-sink\db\Hadoop-Metrics-SQLServer-CREATE.ddl" "%cd%\msi\src\AmbariPackages\ambari-winpkg\resources\" || exit /b 1
popd

set msBuildDir=%WINDIR%\Microsoft.NET\Framework\v4.0.30319
echo Building servicehost
call %msBuildDir%\msbuild.exe "%cd%\src\ServiceHost\ServiceHost.csproj" /target:clean;build /p:Configuration=Release || exit /b 1
copy /y "%cd%\src\ServiceHost\bin\Release\ServiceHost.exe" "%cd%\src\AmbariPackages\ambari-winpkg\resources\" || exit /b 1

echo Compressing resources
powershell.exe -NoProfile -InputFormat none -ExecutionPolicy unrestricted -command "%cd%\build\zip.ps1" "%cd%\src\AmbariPackages\ambari-winpkg" "%cd%\src\AmbariPackages\ambari-winpkg.zip" || exit /b 1

echo Building GUI
call %msBuildDir%\msbuild.exe "%cd%\src\GUI_Ambari\GUI_Ambari.csproj"  || exit /b 1
mkdir "%cd%\src\bin\"
copy /y "%cd%\src\GUI_Ambari\bin\Debug\GUI_Ambari.exe" "%cd%\src\bin\GUI_Ambari.exe" || exit /b 1

echo Building MSI
pushd "%cd%\src" || exit /b 1
candle "%cd%\ambari-scom.wxs"  2>&1 || exit /b 1
light "%cd%\ambari-scom.wixobj" -ext WixUtilExtension.dll 2>&1 || exit /b 1
if not exist "%cd%\ambari-scom.msi" (
echo MSI build failed
exit /b 1
)
popd || exit /b 1
copy /y "%cd%\src\ambari-scom.msi" "%cd%\ambari-scom.msi" || exit /b 1

echo Cleaning 
del /f /q "%cd%\src\ambari-scom.wixobj"
del /f /q "%cd%\src\ambari-scom.wixpdb"
del /f /q "%cd%\src\ambari-scom.msi"
attrib -r -s -h "%cd%\src\GUI_Ambari.v11.suo"
del /f /q "%cd%\src\GUI_Ambari.v11.suo"
del /f /q "%cd%\src\bin\GUI_Ambari.exe"
del /f /q "%cd%\src\bin\Ambari_Result.exe"
del /f /q "%cd%\src\AmbariPackages\ambari-winpkg.zip"
del /f /q "%cd%\src\AmbariPackages\ambari-winpkg\resources\*.jar"
del /f /q "%cd%\src\AmbariPackages\ambari-winpkg\resources\*.zip"
del /f /q "%cd%\src\AmbariPackages\ambari-winpkg\resources\*.exe"
del /f /q "%cd%\src\AmbariPackages\ambari-winpkg\resources\Hadoop-Metrics-SQLServer-CREATE.ddl"
rd /s /q "%cd%\src\GUI_Ambari\bin"
rd /s /q "%cd%\src\GUI_Ambari\obj"
rd /s /q "%cd%\src\ServiceHost\bin"
rd /s /q "%cd%\src\ServiceHost\obj"
echo Done
