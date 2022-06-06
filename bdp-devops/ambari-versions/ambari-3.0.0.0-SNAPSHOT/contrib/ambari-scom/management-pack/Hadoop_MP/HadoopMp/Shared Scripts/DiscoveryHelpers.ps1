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

function MergeStrings($a, $b) {
    $idxA = $idxb = 0
    $result = New-Object char[] ($a.Length + $b.Length)
    for ($idx = 0; $idx -lt $a.Length + $b.Length;) {
        if ($idxa -lt $a.Length) { $result[$idx++] = $a[$idxa++] }
        if ($idxb -lt $b.Length) { $result[$idx++] = $b[$idxb++] }
    }
    New-Object string ($result,0,$result.Length)
}

function FormatClusterServiceName($serviceName) {
    switch ($serviceName) {
        'hdfs' { 'HDFS' }
        'mapreduce' { 'MapReduce' }
        'hive' { 'Hive' }
        'templeton' { 'Templeton' }
        'webhcat' { 'WebHCat' }
        'oozie' { 'Oozie' }
        'pig' { 'Pig' }
        'sqoop' { 'Sqoop' }
        'mapreduce2' { 'MapReduce 2' }
        'yarn' { 'YARN' }
		'zookeeper' { 'ZooKeeper' }
        default { $serviceName }
    }
}

function FormatHostComponentName($componentName) {
    switch ($componentName) {
        'namenode' { 'NameNode' }
        'secondary_namenode' { 'Secondary NameNode' }
        'jobtracker' { 'JobTracker' }
        'datanode' { 'DataNode' }
        'tasktracker' { 'TaskTracker' }
        'hive_server' { 'Hive Server' }
        'hive_metastore' { 'Hive Metastore' }
        'hive_client' { 'Hive Client' }
        'templeton' { 'Templeton' }
        'webhcat_server' { 'WebHCat Server' }
        'oozie_server' { 'Oozie Server' }
        'pig' { 'Pig' }
        'sqoop' { 'Sqoop' }
        'historyserver' { 'History Server' }
        'mapreduce2_client' { 'MapReduce 2 Client' }
        'nodemanager' { 'Node Manager' }
        'resourcemanager' { 'Resource Manager' }
        'yarn_client' { 'YARN Client' }
        'zookeeper_client' { 'ZooKeeper Client' }
        'zookeeper_server' { 'ZooKeeper Server' }
        'zkfc' { 'ZKFC' }
        'journalnode' { 'JournalNode' }
        default { $componentName }
    }
}

function GetPrivateEntityDisplayName($entityDisplayName) {
    "$entityDisplayName (Private)"
}

function CreateEmptyDiscoveryData() {
    $discoveryData = $ScriptApi.CreateDiscoveryData(0, "$MPElement$", "$Target/Id$")
    $discoveryData.IsSnapshot = $false
    $discoveryData
}
