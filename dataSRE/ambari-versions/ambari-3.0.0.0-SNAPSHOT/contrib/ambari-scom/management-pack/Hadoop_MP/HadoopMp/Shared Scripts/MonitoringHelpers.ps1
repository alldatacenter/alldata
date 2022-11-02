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

function TryGetChildValue($source, $sourceKey) {
    foreach ($subKey in @($sourceKey)) {
        if ($source -eq $null) { return $null }
        if ($subKey -eq $null) { continue }
        $source = $source[$subKey]
    }
    $source
}

function AddToBagIfExists($propertyBag, $bagKey, $source, $path) {
    if (($sourceValue = TryGetChildValue $source $path) -eq $null) { return $null }
    $propertyBag.AddValue($bagKey, $sourceValue)
    $sourceValue
}

$epochStartTimeStamp = New-Object DateTime @(1970, 1, 1, 0, 0, 0, [DateTimeKind]::Utc)

function ToUnixEpochTimeStamp($timeStamp)
{
    [Math]::Floor($timeStamp.ToUniversalTime().Subtract($epochStartTimeStamp).TotalSeconds)
}
