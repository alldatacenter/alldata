# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

# Stop on all errors
$ErrorActionPreference = 'Stop';

# Package Name
$packageName = $Env:chocolateyPackageName
# Package Version
$packageVersion = $Env:chocolateyPackageVersion
# Package Folder
$packageFolder = $Env:chocolateyPackageFolder
# Package Parameters
$packageParameters = $env:chocolateyPackageParameters

$arguments = @{}
$ambariRoot = "C:\ambari"
$retries = 5
$lockTimeout = 60000

# Parse the packageParameters
#   /AmbariRoot:C:\ambari /Retries:5
if ($packageParameters) {
  $match_pattern = "\/(?<option>([a-zA-Z]+)):(?<value>([`"'])?([a-zA-Z0-9- _\\:\.]+)([`"'])?)|\/(?<option>([a-zA-Z]+))"
  $option_name = 'option'
  $value_name = 'value'

  if ($packageParameters -match $match_pattern ){
    $results = $packageParameters | Select-String $match_pattern -AllMatches
    $results.matches | % {
      $arguments.Add(
        $_.Groups[$option_name].Value.Trim(),
        $_.Groups[$value_name].Value.Trim())
    }
  } else {
    Throw "Package Parameters were found but were invalid (REGEX Failure)"
  }
  if ($arguments.ContainsKey("AmbariRoot")) {
    Write-Debug "AmbariRoot Argument Found"
    $ambariRoot = $arguments["AmbariRoot"]
  }
  if ($arguments.ContainsKey("Retries")) {
    Write-Debug "Retries Argument Found"
    $retries = $arguments["Retries"]
  }
  if ($arguments.ContainsKey("LockTimeout")) {
    Write-Debug "LockTimeout Argument Found"
    $lockTimeout = $arguments["LockTimeout"]
  }
} else {
  Write-Debug "No Package Parameters Passed in"
}

$modulesFolder = "$(Join-Path $packageFolder modules)"
$contentFolder = "$(Join-Path $packageFolder content)"
$zipFile = "$(Join-Path $contentFolder $packageName-$packageVersion-windows-dist.zip)"
$specificFolder = ""
$link = "$ambariRoot\$packageName"
$target = "$ambariRoot\$packageName-$packageVersion"

Import-Module "$modulesFolder\link.psm1"
Import-Module "$modulesFolder\retry.psm1"

$agentMutex = New-Object System.Threading.Mutex $false, "Global\$packageName"
if($agentMutex.WaitOne($lockTimeout)) {
  try {
    Retry-Command -Command "Get-ChocolateyUnzip" -Arguments @{ FileFullPath = $zipFile; Destination = $target; SpecificFolder = $specificFolder; PackageName = $packageName} -Retries $retries
    Retry-Command -Command "Remove-Symlink-IfExists" -Arguments @{Link = $link} -Retries $retries
    Retry-Command -Command "New-Symlink" -Arguments @{ Link = $link; Target = $target } -Retries $retries
  } finally {
    $agentMutex.ReleaseMutex()
  }
} else {
  Write-Host ("Failed to acquire lock [$packageName] within [$lockTimeout] ms. Installation failed!")
  throw
}
