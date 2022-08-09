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

Param ($TemplateName, $ClusterName, $ClusterAmbariUri, $WatcherNodesList = "", $Username, $Password)

function DiscoverServices($discoveryData, $healthServices) {
    $parent = $discoveryData.CreateClassInstance("$MPElement[Name="Ambari.SCOM.ClusterSoftwareProjection"]$")
    $parent.AddProperty("$MPElement[Name="Ambari.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $parent.AddProperty("$MPElement[Name="Ambari.SCOM.Cluster.Private"]/ClusterName$", $ClusterName)

    $clusterServices = InvokeRestAPI (JoinUri "$ClusterAmbariUri" "services") $Username $Password

    foreach ($clusterService in $clusterServices.items) {
        $serviceClassId = GetServiceClassId $clusterService.ServiceInfo.service_name
        if ($serviceClassId -eq $null) { continue }

        $serviceName = FormatClusterServiceName $clusterService.ServiceInfo.service_name

        $servicePrivateEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Ambari.SCOM.ClusterService.Private"]$")
        $servicePrivateEntity.AddProperty("$MPElement[Name='Ambari.SCOM.AmbariManagedEntity']/AmbariUri$", $clusterService.href)
        $servicePrivateEntity.AddProperty("$MPElement[Name="Ambari.SCOM.ClusterService.Private"]/TemplateName$", $TemplateName)
        $servicePrivateEntity.AddProperty("$MPElement[Name="Ambari.SCOM.ClusterService.Private"]/ClusterName$", $ClusterName)
        $servicePrivateEntity.AddProperty("$MPElement[Name='Ambari.SCOM.ClusterService.Private']/ServiceName$", $clusterService.ServiceInfo.service_name)
        $servicePrivateEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", (GetPrivateEntityDisplayName $serviceName))
        $discoveryData.AddInstance($servicePrivateEntity)

        AddManagementRelationship $discoveryData $healthServices (MergeStrings $ClusterName $clusterService.name) $servicePrivateEntity

        $serviceEntity = $discoveryData.CreateClassInstance($serviceClassId)
        $serviceEntity.AddProperty("$MPElement[Name="Ambari.SCOM.ClusterService.Private"]/TemplateName$", $TemplateName)
        $serviceEntity.AddProperty("$MPElement[Name="Ambari.SCOM.ClusterService.Private"]/ClusterName$", $ClusterName)
        $serviceEntity.AddProperty("$MPElement[Name='Ambari.SCOM.ClusterService.Private']/ServiceName$", $clusterService.ServiceInfo.service_name)
        $serviceEntity.AddProperty("$MPElement[Name="Ambari.SCOM.ClusterServiceBase"]/ClusterName$", $ClusterName)
        $serviceEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", $serviceName)
        $discoveryData.AddInstance($serviceEntity)

        $parentRelationship = $discoveryData.CreateRelationshipInstance("$MPElement[Name="Ambari.SCOM.ClusterSoftwareProjectionContainsClusterService"]$")
        $parentRelationship.Source = $parent
        $parentRelationship.Target = $serviceEntity
        $discoveryData.AddInstance($parentRelationship)
    }
}

function GetServiceClassId($monitoringUri) {
    switch ($monitoringUri -replace ".*/", "") {
        'hdfs' { '$MPElement[Name="Ambari.SCOM.ClusterService.Hdfs"]$' }
        'mapreduce' { '$MPElement[Name="Ambari.SCOM.ClusterService.MapReduce"]$' }
        'hive' { '$MPElement[Name="Ambari.SCOM.ClusterService.Hive"]$' }
        { 'templeton', 'webhcat' -contains $_ } { '$MPElement[Name="Ambari.SCOM.ClusterService.Templeton"]$' }
        'oozie' { '$MPElement[Name="Ambari.SCOM.ClusterService.Oozie"]$' }
        'pig' { '$MPElement[Name="Ambari.SCOM.ClusterService.Pig"]$' }
        'sqoop' { '$MPElement[Name="Ambari.SCOM.ClusterService.Sqoop"]$' }
        'mapreduce2' { '$MPElement[Name="Ambari.SCOM.ClusterService.MapReduce2"]$' }
        'yarn' { '$MPElement[Name="Ambari.SCOM.ClusterService.Yarn"]$' }
		'zookeeper' { '$MPElement[Name="Ambari.SCOM.ClusterService.ZooKeeper"]$' }
        default: { $null }
    }
}

function Main() {
    $discoveryData = $ScriptApi.CreateDiscoveryData(0, "$MPElement$", "$Target/Id$")

    $healthServices = CreateHealthServicesFromWatcherNodesList $discoveryData $WatcherNodesList
    if ($healthServices.Count -eq 0) { return $discoveryData }

    DiscoverServices $discoveryData $healthServices

    $discoveryData
}
