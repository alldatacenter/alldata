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

$UserAgent = $("{0} (PowerShell {1}; .NET CLR {2}; {3})" -f "SCOM MP",
                $(if ($Host.Version) { $Host.Version } else { "1.0" }),
                [Environment]::Version,
                [Environment]::OSVersion.ToString().Replace("Microsoft Windows ", "Win"))

function InvokeRestAPI($uri, [string]$username, [string]$password, [string]$method = 'GET', [string]$requestBody = $null) {
	# TODO: Remove prior to release!
    #TraceTaskExecution "$method $uri $requestBody"
    $request = [System.Net.HttpWebRequest]::Create($uri)
    $request.Method = $method
    $request.UserAgent = $UserAgent
    $request.Timeout = 30000

    $credentials = [Convert]::ToBase64String([Text.Encoding]::Default.GetBytes($username + ':' + $password));
    $request.Headers.Add('Authorization', "Basic $credentials")
    
    if ($requestBody) {
        $requestData = [Text.Encoding]::UTF8.GetBytes($requestBody)
        $request.ContentLength = $requestData.Length
        $requestStream = $request.GetRequestStream()
        $requestStream.Write($requestData, 0, $requestData.Length)
        $requestStream.Close()
    }

    $response = $request.GetResponse()
    if ([int]$response.StatusCode -lt 200 -or [int]$response.StatusCode -ge 300) {
        throw "Ambari API response status is $($response.StatusCode) : $($response.StatusDescription)."
    }
    $reader = [IO.StreamReader] $response.GetResponseStream()
    $jsonString = $reader.ReadToEnd()
    $reader.Close()
    $response.Close()

    if ($jsonString) {
        @{ StatusCode = [int]$response.StatusCode;
           Body = ParseJsonString $jsonString }
    } else {
        @{ StatusCode = $response.StatusCode }
    }
}

function JoinUri([string]$baseUri, [string]$segment) {
    $baseUri.TrimEnd('/') + '/' + $segment.TrimStart('/')
}