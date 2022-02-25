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

function DiscoverHosts($discoveryData, $healthServices) {
    $parent = $discoveryData.CreateClassInstance("$MPElement[Name="Ambari.SCOM.ClusterHardwareProjection"]$")
    $parent.AddProperty("$MPElement[Name="Ambari.SCOM.Cluster.Private"]/TemplateName$", $TemplateName)
    $parent.AddProperty("$MPElement[Name="Ambari.SCOM.Cluster.Private"]/ClusterName$", $ClusterName)

    $nodesResult = InvokeRestAPI (JoinUri "$ClusterAmbariUri" "hosts") $Username $Password
    foreach ($node in $nodesResult.items) {
        # TODO: Get IP addresses with single request?
        $nodeResult = InvokeRestAPI $node.href $Username $Password

        $nodePrivateEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Ambari.SCOM.Host.Private"]$")
        $nodePrivateEntity.AddProperty("$MPElement[Name='Ambari.SCOM.AmbariManagedEntity']/AmbariUri$", $node.href)
        $nodePrivateEntity.AddProperty("$MPElement[Name="Ambari.SCOM.Host.Private"]/TemplateName$", $TemplateName)
        $nodePrivateEntity.AddProperty("$MPElement[Name="Ambari.SCOM.Host.Private"]/ClusterName$", $ClusterName)
        $nodePrivateEntity.AddProperty("$MPElement[Name='Ambari.SCOM.Host.Private']/HostName$", $node.Hosts.host_name)
        $nodePrivateEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", (GetPrivateEntityDisplayName $node.Hosts.host_name))
        $discoveryData.AddInstance($nodePrivateEntity)

        AddManagementRelationship $discoveryData $healthServices (MergeStrings $ClusterName $node.Hosts.host_name) $nodePrivateEntity

        $nodeEntity = $discoveryData.CreateClassInstance("$MPElement[Name="Ambari.SCOM.Host"]$")
        $nodeEntity.AddProperty("$MPElement[Name="Ambari.SCOM.Host.Private"]/TemplateName$", $TemplateName)
        $nodeEntity.AddProperty("$MPElement[Name="Ambari.SCOM.Host.Private"]/ClusterName$", $ClusterName)
        $nodeEntity.AddProperty("$MPElement[Name='Ambari.SCOM.Host.Private']/HostName$", $node.Hosts.host_name)
        $nodeEntity.AddProperty("$MPElement[Name='Ambari.SCOM.Host']/IpAddress$", $nodeResult.Hosts.ip)
        $nodeEntity.AddProperty("$MPElement[Name='System!System.Entity']/DisplayName$", $node.Hosts.host_name)
        $discoveryData.AddInstance($nodeEntity)

        $parentRelationship = $discoveryData.CreateRelationshipInstance("$MPElement[Name="Ambari.SCOM.ClusterHardwareProjectionContainsHost"]$")
        $parentRelationship.Source = $parent
        $parentRelationship.Target = $nodeEntity
        $discoveryData.AddInstance($parentRelationship)
    }
}

function Main() {
    $discoveryData = $ScriptApi.CreateDiscoveryData(0, "$MPElement$", "$Target/Id$")

    $healthServices = CreateHealthServicesFromWatcherNodesList $discoveryData $WatcherNodesList
    if ($healthServices.Count -eq 0) { return $discoveryData }

    DiscoverHosts $discoveryData $healthServices

    $discoveryData
}
