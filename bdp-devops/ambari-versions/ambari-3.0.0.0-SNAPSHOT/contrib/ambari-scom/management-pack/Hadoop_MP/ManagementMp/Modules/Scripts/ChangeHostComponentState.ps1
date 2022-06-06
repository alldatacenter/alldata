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

function Main() {
    TraceTaskExecution "Changing Host Component state to $TargetState."

    $response = InvokeRestAPI $MonitoringAmbariUri $Username $Password 'PUT' ('{"RequestInfo":{"context":"' + $OperationFriendlyName + '"},"Body":{"HostRoles":{"state":"' + $TargetState +'"}}}')
    if ($response.StatusCode -eq 200) {
        TraceTaskExecution "Host Component already in $TargetState state."
        return
    }

    if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
        throw "Failed to change Host Component state."
    }
    
    $response = InvokeRestAPI $response.Body.href $Username $Password
    WaitForTasksCompletion $response.Body.tasks
}
