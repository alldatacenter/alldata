## Licensed to the Apache Software Foundation (ASF) under one
## or more contributor license agreements.  See the NOTICE file
## distributed with this work for additional information
## regarding copyright ownership.  The ASF licenses this file
## to you under the Apache License, Version 2.0 (the
## "License"); you may not use this file except in compliance
## with the License.  You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing,
## software distributed under the License is distributed on an
## "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
## KIND, either express or implied.  See the License for the
## specific language governing permissions and limitations
## under the License.

Building the Management Pack
=========


1.	Install Microsoft Visual Studio 2010 Professional or higher
2.	Install System Center 2012 Visual Studio Authoring Extensions from
http://www.microsoft.com/en-us/download/details.aspx?id=30169
3.	Replace `C:\Program Files (x86)\MSBuild\Microsoft\VSAC Replace Microsoft.EnterpriseManagement.Core.dll` with a version from Operations Management 2012 SP1 or R2 Preview
4.	Install latest Wix toolset from http://wix.codeplex.com/
5.	Install Silverlight (Open solution in Visual Studio and it will suggest download)
6.	If you would like to sign MP with your key then replace `Solution Items\key.snk` file with your own but keep the same name.
7.	Update `\Installer\Assets\EULA.rtf`
8.	Run `build.bat`
9.	Look for built MPs:
    * `\HadoopMp\bin\Debug\Ambari.SCOM.mpb`
    * `\ManagementMp\bin\Debug\Ambari.SCOM.Management.mpb`
    * `\PresentationMp\bin\Debug\Ambari.SCOM.Presentation.mpb`
10. Built msi will be located in `\Installer\bin\Debug\en-us\`
