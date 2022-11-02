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
    $response = InvokeRestAPI (JoinUri "$MonitoringAmbariUri" 'components/JOBTRACKER?fields=metrics/mapred/jobtracker') $Username $Password

    $propertyBag = $ScriptApi.CreatePropertyBag()

    if (($jobtrackerGroup = TryGetChildValue $response @('metrics', 'mapred', 'jobtracker')) -eq $null) { return $propertyBag }

    if ($jobtrackerGroup['SummaryJson'] -ne $null) {
        try {
            $nodesSummary = ParseJsonString $jobtrackerGroup['SummaryJson']

            $totalNodes = $nodesSummary['nodes']
            $aliveCount = $nodesSummary['alive']

            if ($aliveCount -ne $null -and $totalNodes -ne $null) {
                $propertyBag.AddValue('livetasktrackers', $aliveCount)
                $propertyBag.AddValue('deadtasktrackers', $totalNodes - $aliveCount)

                if ($totalNodes -gt 0) {
                    $propertyBag.AddValue('deadtasktrackers_percent', [Math]::Round(100 - $aliveCount / $totalNodes * 100, 2))
                }
            }
        } catch {}
    }

    foreach ($metricName in $jobtrackerGroup.Keys) {
        if (!(ShouldCollectMetric $metricName) -or $jobtrackerGroup[$metricName] -eq $null) { continue }
        $propertyBag.AddValue("$metricName".ToLower(), $jobtrackerGroup[$metricName])
    }

    $jobsSubmitted = $jobtrackerGroup['jobs_submitted']
    if ($jobtrackerGroup['jobs_failed'] -ne $null -and $jobsSubmitted -ne $null -and $jobsSubmitted -gt 0) {
        $propertyBag.AddValue('failedjobs_percent', [Math]::Round($jobtrackerGroup['jobs_failed'] / $jobsSubmitted * 100, 2))
    } elseif ($jobtrackerGroup['jobs_failed'] -eq 0) {
        $propertyBag.AddValue('failedjobs_percent', 0)
	}

    $propertyBag
}

function ShouldCollectMetric($metricName) {
    return 'BlacklistedNodesInfoJson', 'GraylistedNodesInfoJson', 'QueueInfoJson', 'SummaryJson', 'AliveNodesInfoJson' -notcontains $metricName
}