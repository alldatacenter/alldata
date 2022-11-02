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
    $response = InvokeRestAPI (JoinUri "$MonitoringAmbariUri" 'components/NAMENODE?fields=metrics/dfs') $Username $Password

    $propertyBag = $ScriptApi.CreatePropertyBag()

    if ($response.metrics -ne $null) {
        if (($namenodeGroup = TryGetChildValue $response.metrics @('dfs', 'namenode')) -ne $null) {
            if ($namenodeGroup['LiveNodes']) {
                try {
                    $liveNodes = (ParseJsonString $namenodeGroup['LiveNodes']).Count
                    $propertyBag.AddValue('livenodes', $liveNodes)
                } catch {}

                try {
                    $deadNodes = (ParseJsonString $namenodeGroup['DeadNodes']).Count
                    $propertyBag.AddValue('deadnodes', $deadNodes)
                } catch {}
            
                if ($liveNodes -ne $null -and $deadNodes -ne $null -and (($totalNodes = $liveNodes + $deadNodes) -gt 0)) {
                    $propertyBag.AddValue('deadnodes_percent', [Math]::Round($deadNodes / $totalNodes * 100, 2))
                }

                try {
                    $decomNodes = (ParseJsonString $namenodeGroup['DecomNodes']).Count
                    $propertyBag.AddValue('decomnodes', $decomNodes)
                } catch {}
            }

            $null = AddToBagIfExists $propertyBag 'filescreated' $namenodeGroup 'FilesCreated'
            $null = AddToBagIfExists $propertyBag 'filesappended' $namenodeGroup 'FilesAppended'
            $null = AddToBagIfExists $propertyBag 'filesdeleted' $namenodeGroup 'FilesDeleted'
        }

        if (($fsNamesystemGroup = TryGetChildValue $response.metrics @('dfs', 'FSNamesystem')) -ne $null) {
            $null = AddToBagIfExists $propertyBag 'filestotal' $fsNamesystemGroup 'FilesTotal'
            $totalBlocks = AddToBagIfExists $propertyBag 'blockstotal' $fsNamesystemGroup 'BlocksTotal'
            $capacityTotal = AddToBagIfExists $propertyBag 'capacitytotalgb' $fsNamesystemGroup 'CapacityTotalGB'
            $capacityUsed = AddToBagIfExists $propertyBag 'capacityusedgb' $fsNamesystemGroup 'CapacityUsedGB'
            $capacityRemaining = AddToBagIfExists $propertyBag 'capacityremaininggb' $fsNamesystemGroup 'CapacityRemainingGB'
            if ($capacityTotal -ne $null -and $capacityUsed -ne $null -and $capacityRemaining -ne $null -and $capacityTotal -gt 0) {
                $propertyBag.AddValue('capacitynondfsusedgb', $capacityTotal - $capacityUsed - $capacityRemaining)
                $propertyBag.AddValue('capacityremaining_percent', [Math]::Round($capacityRemaining / $capacityTotal * 100, 2))
            }
            $null = AddToBagIfExists $propertyBag 'corruptblocks' $fsNamesystemGroup 'CorruptBlocks'
            $null = AddToBagIfExists $propertyBag 'pendingdeletionblocks' $fsNamesystemGroup 'PendingDeletionBlocks'
            $null = AddToBagIfExists $propertyBag 'pendingreplicationblocks' $fsNamesystemGroup 'PendingReplicationBlocks'
            $underReplicatedBlocks = AddToBagIfExists $propertyBag 'underreplicatedblocks' $fsNamesystemGroup 'UnderReplicatedBlocks'
            if ($underReplicatedBlocks -ne $null -and $totalBlocks -ne $null -and $totalBlocks -gt 0) {
                $propertyBag.AddValue('underreplicatedblocks_percent', [Math]::Round($underReplicatedBlocks / $totalBlocks * 100, 2))
            }
            $null = AddToBagIfExists $propertyBag 'missingblocks' $fsNamesystemGroup 'MissingBlocks'
        }
    }

    $startTimeStamp = ToUnixEpochTimeStamp([DateTime]::UtcNow.AddMinutes(-10))
    $response = InvokeRestAPI (JoinUri "$MonitoringAmbariUri" "components/DATANODE?fields=metrics/dfs/datanode[$startTimeStamp]") $Username $Password

    if (($datanodeGroup = TryGetChildValue $response @('metrics', 'dfs', 'datanode')) -ne $null) {
        foreach ($metric in $datanodeGroup.Keys) {
            $null = AddToBagIfExists $propertyBag ("aggregated.datanode.$metric".ToLower()) (GetLatestValue $datanodeGroup[$metric])
        }
    }

    $propertyBag
}

function GetLatestValue($measures) {
    $maxDate = 0
    $maxValue = $null
    foreach ($measure in $measures) {
        if ($measure[1] -le $maxDate) { continue }
        $maxDate = $measure[1]
        $maxValue = $measure[0]
    }
    $maxValue
}
