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

Function New-Symlink {
  param (
    [Parameter(Position=0, Mandatory=$true)]
    [string] $link,
    [Parameter(Position=1, Mandatory=$true)]
    [string] $target
  )
  Invoke-Mklink -Link $link -Target $target -Symlink
}

Function Remove-Symlink {
  param (
    [Parameter(Position=0, Mandatory=$true)]
    [string] $link
  )

  if(-not(Test-Path $link)) {
    throw "Symbolic link $link does not exist!"
  }

  $cmd = ""
  if (Test-Path -PathType Container $link) {
      $cmd = "cmd /c rmdir"
  } else {
    $cmd = "cmd /c del"
  }

  $output = Invoke-Expression "$cmd `"$link`" 2>&1"

  if ($lastExitCode -ne 0) {
    throw "Failed to delete link $link.`nExit code: $lastExitCode`nOutput: $output"
  } else {
    Write-Output $output
  }
}

Function Remove-Symlink-IfExists {
  param (
    [Parameter(Position=0, Mandatory=$true)]
    [string] $link
  )

  if(Test-Path $link) {
    Remove-Symlink -Link $link
  }
}

Function New-Hardlink {
  param (
    [Parameter(Position=0, Mandatory=$true)]
    [string] $link,
    [Parameter(Position=1, Mandatory=$true)]
    [string] $target
  )
  Invoke-Mklink -Link $link -Target $target -Hardlink
}

Function Invoke-Mklink {
  [CmdletBinding(DefaultParameterSetName = "Symlink")]
  param (
    [Parameter(Position=0, Mandatory=$true)]
    [string] $link,
    [Parameter(Position=1, Mandatory=$true)]
    [string] $target,

    [Parameter(ParameterSetName = "Symlink")]
    [switch] $symlink = $true,
    [Parameter(ParameterSetName = "HardLink")]
    [switch] $hardlink
  )

  if(-not(Test-Path $target)) {
    throw "Target $target does not exist!"
  }
  if(Test-Path $link) {
    throw "Link $link already exists!"
  }

  $isDirectory = Test-Path -PathType Container $target
  $cmd = "cmd /c mklink"
  if($isDirectory) {
    if($hardlink) {
      throw "Target $target is a directory. Hard links cannot be created for directories!"
    }
    $cmd += " /D"
  }
  if($hardlink) {
    $cmd += " /H"
  }

  $output = Invoke-Expression "$cmd `"$link`" `"$target`" 2>&1"
  if ($lastExitCode -ne 0) {
    throw "Failed to create a link using mklink.`nExit code: $lastExitCode`nOutput: $output"
  } else {
    Write-Output $output
  }
}

Export-ModuleMember New-Symlink, Remove-Symlink, Remove-Symlink-IfExists, New-Hardlink, Remove-Hardlink