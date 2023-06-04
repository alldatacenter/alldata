@echo off

REM Datart
REM <p>
REM Copyright 2021
REM <p>
REM Licensed under the Apache License, Version 2.0 (the "License");
REM you may not use this file except in compliance with the License.
REM You may obtain a copy of the License at
REM <p>
REM http://www.apache.org/licenses/LICENSE-2.0
REM <p>
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.


if "%1"=="start" goto START


:START

cd /d %~dp0

cd ..

java -server -Xms2G -Xmx2G -Dspring.profiles.active=config -Dfile.encoding=UTF-8 -cp ".\lib\*" datart.DatartServerApplication