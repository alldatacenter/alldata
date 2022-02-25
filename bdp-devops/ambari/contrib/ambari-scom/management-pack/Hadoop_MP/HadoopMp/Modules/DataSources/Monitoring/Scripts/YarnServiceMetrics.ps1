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
    $response = InvokeRestAPI (JoinUri "$MonitoringAmbariUri" 'components/RESOURCEMANAGER?fields=metrics/yarn') $Username $Password

    $propertyBag = $ScriptApi.CreatePropertyBag()

    if ($response.metrics -eq $null) { return $propertyBag }

    Add-MetricChildrenItems $propertyBag '' $response.metrics

	if ($response.metrics['yarn'] -ne $null -and $response.metrics['yarn']['Queue'] -ne $null -and $response.metrics['yarn']['Queue']['root'] -ne $null) 
	{
		$appsSubmitted = $response.metrics['yarn']['Queue']['root']['AppsSubmitted']
		$appsFailed = $response.metrics['yarn']['Queue']['root']['AppsFailed']
		if ($appsSubmitted -ne $null -and $appsSubmitted -gt 0 -and $appsFailed -ne $null)
		{
	        $propertyBag.AddValue('appsfailed_percent', [Math]::Round($appsFailed / $appsSubmitted * 100, 2))
		}
		elseif ($appsFailed -eq 0) 
		{
	        $propertyBag.AddValue('appsfailed_percent', 0)
		}
	}

	if ($response.metrics['yarn'] -ne $null -and $response.metrics['yarn']['ClusterMetrics'] -ne $null) 
	{
		$NumActiveNMs = 0
		$NumDecommissionedNMs = 0
		$NumLostNMs = 0
		$NumRebootedNMs = 0
		$NumUnhealthyNMs = 0
		if ($response.metrics['yarn']['ClusterMetrics']['NumActiveNMs'] -ne $null) 
		{
			$NumActiveNMs = $response.metrics['yarn']['ClusterMetrics']['NumActiveNMs']
		}
		if ($response.metrics['yarn']['ClusterMetrics']['NumDecommissionedNMs'] -ne $null) 
		{
			$NumDecommissionedNMs = $response.metrics['yarn']['ClusterMetrics']['NumDecommissionedNMs']
		}
		if ($response.metrics['yarn']['ClusterMetrics']['NumLostNMs'] -ne $null) 
		{
			$NumLostNMs = $response.metrics['yarn']['ClusterMetrics']['NumLostNMs']
		}
		if ($response.metrics['yarn']['ClusterMetrics']['NumRebootedNMs'] -ne $null) 
		{
			$NumRebootedNMs = $response.metrics['yarn']['ClusterMetrics']['NumRebootedNMs']
		}
		if ($response.metrics['yarn']['ClusterMetrics']['NumUnhealthyNMs'] -ne $null) 
		{
			$NumUnhealthyNMs = $response.metrics['yarn']['ClusterMetrics']['NumUnhealthyNMs']
		}
		$NumTotalNMs = $NumActiveNMs + $NumDecommissionedNMs + $NumLostNMs

		if ($NumTotalNMs -gt 0)
		{
	        $propertyBag.AddValue('lostnodemanagers_percent', [Math]::Round($NumLostNMs / $NumTotalNMs * 100, 2))
	        $propertyBag.AddValue('unhealthynodemanagers_percent', [Math]::Round($NumUnhealthyNMs / $NumTotalNMs * 100, 2))
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

