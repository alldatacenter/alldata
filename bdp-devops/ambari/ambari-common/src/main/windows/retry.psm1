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

function Retry-Command
{
  param (
    [Parameter(Mandatory=$true)]
    [string]$command,
    [Parameter(Mandatory=$true)]
    [hashtable]$arguments,

    [Parameter(Mandatory=$false)]
    [int]$retries = 5,
    [Parameter(Mandatory=$false)]
    [int]$secondsDelay = 2
  )

  $retrycount = 0
  $done = $false
  while (-not $done) {
    try {
      & $command @arguments
      Write-Host ("Command [$command] succeeded.")
      $done = $true
    } catch {
      $errorMessage = $_.Exception.Message
      $failedItem = $_.Exception.ItemName
      if ($retrycount -ge $retries) {
        Write-Host ("Command [$command] failed the maximum number of $retrycount times.")
        throw
      } else {
        Write-Host ("Command [$command] failed.`nError Message: $errorMessage`nRetrying in $secondsDelay seconds.")
        Start-Sleep $secondsDelay
        $retrycount++
      }
    }
  }
}