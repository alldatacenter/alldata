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

[CmdletBinding()]
param (
    [Parameter(Mandatory=$true)]
    [string]
    $ComputerName,

  [Parameter(Mandatory=$true)]
    [string]
    $Path,

  [Parameter(Mandatory=$true)]
    [string]
    $Destination,

  [int]
    $Overwrite = 0x14
)

function RunRemote {
  $ret = Invoke-Command -ComputerName $ComputerName -ScriptBlock {
    $fileName = $args[0]
    $destination = $args[1]
    try {
      if (!(Test-Path $fileName)) {
        Write-Host "Archive $fileName does not exist"
        return -1
      }
      if (!(Test-Path $destination)) {
        Write-Host "Destination $destination does not exist. Creating.."
        New-Item -Path $destination -type directory -Force
        Write-Host "Destination path $destination created!"
      }
      $shell_app = new-object -com shell.application
      $zip_file = $shell_app.namespace($fileName)
      $shell_app.namespace($destination).Copyhere($zip_file.items(), $args[2])
    }
    catch {
      Write-Host $_.Exception.Message
      Write-Host $_.Exception.ItemName
      return -1
    }
    return 0
  } -ArgumentList $Path, $Destination, $Overwrite
  return $ret
}
$status = Invoke-Expression "RunRemote"
[environment]::exit($status)