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
    $TransferChunkSize = 0x10000
)

function Initialize-TempScript ($Path) {
    "<# DATA" | Set-Content -Path $Path
}

function Complete-Chunk () {
@"
DATA #>
`$TransferPath = `$Env:TEMP | Join-Path -ChildPath '$TransferId'
`$InData = `$false
`$WriteStream = [IO.File]::OpenWrite(`$TransferPath)
try {
    `$WriteStream.Seek(0, 'End') | Out-Null
    `$MyInvocation.MyCommand.Definition -split "``n" | ForEach-Object {
        if (`$InData) {
            `$InData = -not `$_.StartsWith('DATA #>')
            if (`$InData) {
                `$WriteBuffer = [Convert]::FromBase64String(`$_)
                `$WriteStream.Write(`$WriteBuffer, 0, `$WriteBuffer.Length)
            }
        } else {
            `$InData = `$_.StartsWith('<# DATA')
        }
    }
} finally {
    `$WriteStream.Close()
}
"@
}

function Complete-FinalChunk ($Destination) {
@"
`$TransferPath | Move-Item -Destination '$Destination' -Force
"@
}

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$EncodingChunkSize = 57 * 100
if ($EncodingChunkSize % 57 -ne 0) {
    throw "EncodingChunkSize must be a multiple of 57"
}

$TransferId = [Guid]::NewGuid().ToString()


$Path = ($Path | Resolve-Path).ProviderPath
$ReadBuffer = New-Object -TypeName byte[] -ArgumentList $EncodingChunkSize

$TempPath = ([IO.Path]::GetTempFileName() | % { $_ | Move-Item -Destination "$_.ps1" -PassThru}).FullName
$Session = New-PSSession -ComputerName $ComputerName
$ReadStream = [IO.File]::OpenRead($Path)

$ChunkCount = 0
Initialize-TempScript -Path $TempPath

try {
    do {
        $ReadCount = $ReadStream.Read($ReadBuffer, 0, $EncodingChunkSize)
        if ($ReadCount -gt 0) {
            [Convert]::ToBase64String($ReadBuffer, 0, $ReadCount, 'InsertLineBreaks') |
                Add-Content -Path $TempPath
        }
        $ChunkCount += $ReadCount
        if ($ChunkCount -ge $TransferChunkSize -or $ReadCount -eq 0) {
            # send
            Complete-Chunk | Add-Content -Path $TempPath
            if ($ReadCount -eq 0) {
                Complete-FinalChunk -Destination $Destination | Add-Content -Path $TempPath
                Write-Verbose "Sending final chunk"
            }
            Invoke-Command -Session $Session -FilePath $TempPath

            # reset
            $ChunkCount = 0
            Initialize-TempScript -Path $TempPath
        }
    } while ($ReadCount -gt 0)
} finally {
    if ($ReadStream) { $ReadStream.Close() }
    $Session | Remove-PSSession
    $TempPath | Remove-Item
}

