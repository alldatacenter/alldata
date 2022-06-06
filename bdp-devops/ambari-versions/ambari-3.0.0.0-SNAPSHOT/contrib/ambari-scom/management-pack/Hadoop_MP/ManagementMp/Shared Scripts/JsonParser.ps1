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

try {
    $JSONUtil = (Add-Type -Language JScript -MemberDefinition "static function parseJSON(json){return eval('('+json+')');}" -Name "JSONUtil" -PassThru)[1]
} catch {
    TraceTaskExecution "Failed to create JSON parser: $_"
}

function ParseJsonString([string] $jsonString) {
    $jsObject = $JSONUtil::parseJSON($jsonString)
    ConvertJsObject([ref]$jsObject)
}

function ConvertJsObject([ref]$objRef) {
    $obj = $objRef.Value
    if ($obj -is [Microsoft.JScript.ArrayObject]) {
        return ,(ConvertJsArrayObject([ref]$obj))
    } elseif ($obj -is [Microsoft.JScript.JSObject]) {
        return ConvertJsScriptObject([ref]$obj)
    } else {
        return $obj
    }
}

function ConvertJsScriptObject([ref]$objRef) {
    $obj = $objRef.Value
    $result = @{}
    foreach ($fieldName in $obj) {
        $result[$fieldName] = ConvertJsObject([ref]$obj[$fieldName])
    }
    $result
}

function ConvertJsArrayObject([ref]$arrayRef) {
    $array = $arrayRef.Value
    $result = @()
    foreach ($index in $array) {
        $conv = ConvertJsObject([ref]$array[$index])
        $result += $conv
    }
    return ,$result
}
