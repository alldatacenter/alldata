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

Param ($MonitoringAmbariUri, $Username, $Password)

function Main {
    $response = InvokeRestAPI "$MonitoringAmbariUri" $Username $Password

    $propertyBag = $ScriptApi.CreatePropertyBag()

    if ($response.metrics -eq $null) { return $propertyBag }

    Add-MetricChildrenItems $propertyBag '' $response.metrics

    if ('namenode', 'jobtracker', 'resourcemanager' -contains $response.HostRoles.component_name) {
        if ($response.metrics['jvm'] -ne $null) {
            $memoryUsed = $response.metrics['jvm']['memHeapUsedM']
            $memoryCommitted = $response.metrics['jvm']['memHeapCommittedM']
            if ($memoryUsed -ne $null -and $memoryCommitted -ne $null -and $memoryCommitted -gt 0) {
                $propertyBag.AddValue("calculated.memheapusedpercent", [Math]::Round($memoryUsed / $memoryCommitted * 100, 2))
            }
        }
    }

    $propertyBag
}

function Add-MetricChildrenItems($propertyBag, $parentName, $children) {
    $parentPrefix = $(if (!$parentName) { '' } else { "$parentName." })
    foreach ($childName in $children.Keys) {
        if ($children[$childName] -eq $null) { continue }

        if ($children[$childName] -is [Hashtable]) {
            Add-MetricChildrenItems $propertyBag "$parentPrefix$childName" $children[$childName]
        } else {
            $propertyBag.AddValue("$parentPrefix$childName".ToLower(), $children[$childName])
        }
    }
}