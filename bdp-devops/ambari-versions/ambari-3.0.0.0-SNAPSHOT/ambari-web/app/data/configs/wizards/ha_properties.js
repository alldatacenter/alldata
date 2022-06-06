/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports =
{
  "haConfig": {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'}),
      App.ServiceConfigCategory.create({ name: 'HBASE', displayName: 'HBase'}),
      App.ServiceConfigCategory.create({ name: 'ACCUMULO', displayName: 'Accumulo'}),
      App.ServiceConfigCategory.create({ name: 'AMBARI_METRICS', displayName: 'Ambari Metrics'}),
      App.ServiceConfigCategory.create({ name: 'HAWQ', displayName: 'HAWQ'}), 
      App.ServiceConfigCategory.create({ name: 'RANGER', displayName: 'Ranger'})
    ],
    sites: ['core-site', 'hdfs-site', 'hbase-site', 'accumulo-site', 'ams-hbase-site', 'hawq-site', 'hdfs-client', 'ranger-env', 'ranger-knox-plugin-properties', 'ranger-kms-audit', 'ranger-storm-plugin-properties', 'ranger-hbase-plugin-properties', 'ranger-hdfs-plugin-properties', 'ranger-hive-plugin-properties', 'ranger-kafka-audit', 'ranger-knox-audit', 'ranger-hdfs-audit', 'ranger-hive-audit', 'ranger-atlas-audit', 'ranger-storm-audit', 'ranger-hbase-audit', 'ranger-yarn-audit'],
    configs: [
    /**********************************************HDFS***************************************/
      {
        "name": "dfs.journalnode.edits.dir",
        "displayName": "dfs.journalnode.edits.dir",
        "description": "The Directory where the JournalNode will store its local state.",
        "isReconfigurable": true,
        "recommendedValue": "/hadoop/hdfs/journal",
        "value": "/hadoop/hdfs/journal",
        "displayType": "directory",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "fs.defaultFS",
        "displayName": "fs.defaultFS",
        "description": "The default path prefix used by the Hadoop FS client when none is given.",
        "recommendedValue": "hdfs://haCluster",
        "isReconfigurable": false,
        "value": "hdfs://haCluster",
        "category": "HDFS",
        "filename": "core-site",
        serviceName: 'MISC'
      },
      {
        "name": "ha.zookeeper.quorum",
        "displayName": "ha.zookeeper.quorum",
        "isReconfigurable": false,
        "description": "This lists the host-port pairs running the ZooKeeper service.",
        "recommendedValue": "zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181",
        "value": "zk1.example.com:2181,zk2.example.com:2181,zk3.example.com:2181",
        "category": "HDFS",
        "filename": "core-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.nameservices",
        "displayName": "dfs.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "haCluster",
        "value": "haCluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.internal.nameservices",
        "displayName": "dfs.internal.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "haCluster",
        "value": "haCluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.ha.namenodes.${dfs.nameservices}",
        "displayName": "dfs.ha.namenodes.${dfs.nameservices}",
        "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
        "isReconfigurable": false,
        "recommendedValue": "nn1,nn2",
        "value": "nn1,nn2",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.rpc-address.${dfs.nameservices}.nn1",
        "description": "RPC address that handles all clients requests for nn1.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:8020",
        "value": "0.0.0.0:8020",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.rpc-address.${dfs.nameservices}.nn2",
        "description": "RPC address that handles all clients requests for nn2.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:8020",
        "value": "0.0.0.0:8020",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.http-address.${dfs.nameservices}.nn1",
        "description": "The fully-qualified HTTP address for nn1 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:50070",
        "value": "0.0.0.0:50070",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.http-address.${dfs.nameservices}.nn2",
        "description": "The fully-qualified HTTP address for nn2 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:50070",
        "value": "0.0.0.0:50070",
        "category": "HDFS",
        "filename": "hdfs-site",
        serviceName: 'MISC'
      },
      {
        "name": "dfs.namenode.https-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.https-address.${dfs.nameservices}.nn1",
        "description": "The fully-qualified HTTP address for nn1 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:50470",
        "value": "0.0.0.0:50470",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.https-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.https-address.${dfs.nameservices}.nn2",
        "description": "The fully-qualified HTTP address for nn2 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:50470",
        "value": "0.0.0.0:50470",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.client.failover.proxy.provider.${dfs.nameservices}",
        "displayName": "dfs.client.failover.proxy.provider.${dfs.nameservices}",
        "description": "The Java class that HDFS clients use to contact the Active NameNode.",
        "recommendedValue": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
        "isReconfigurable": false,
        "value": "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.shared.edits.dir",
        "displayName": "dfs.namenode.shared.edits.dir",
        "description": " The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "recommendedValue": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "value": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.ha.fencing.methods",
        "displayName": "dfs.ha.fencing.methods",
        "description": "A list of scripts or Java classes which will be used to fence the Active NameNode during a failover.",
        "isReconfigurable": false,
        "recommendedValue": "shell(/bin/true)",
        "displayType": "multiLine",
        "value": "shell(/bin/true)",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.ha.automatic-failover.enabled",
        "displayName": "dfs.ha.automatic-failover.enabled",
        "description": "Enable Automatic failover.",
        "isReconfigurable": false,
        "recommendedValue": true,
        "value": true,
        "displayType": "checkbox",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.safemode.threshold-pct",
        "displayName": "dfs.namenode.safemode.threshold-pct",
        "description": "Specifies the percentage of blocks that should satisfy\n        the minimal replication requirement defined by dfs.namenode.replication.min.\n        Values less than or equal to 0 mean not to start in safe mode.\n        Values greater than 1 will make safe mode permanent.\n ",
        "isReconfigurable": false,
        "recommendedValue": "0.99f",
        "value": "0.99f",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      },
      {
        "name": "hbase.rootdir",
        "displayName": "hbase.rootdir",
        "description": "The directory shared by region servers and into which HBase persists.",
        "isReconfigurable": false,
        "recommendedValue": "/hadoop/hdfs/journal",
        "value": "/hadoop/hdfs/journal",
        "category": "HBASE",
        "filename": "hbase-site",
        "serviceName": 'MISC'
      },
      {
        "name": "instance.volumes",
        "displayName": "instance.volumes",
        "isReconfigurable": false,
        "recommendedValue": "/hadoop/hdfs/journal",
        "value": "/hadoop/hdfs/journal",
        "category": "ACCUMULO",
        "filename": "accumulo-site",
        "serviceName": 'MISC'
      },
      {
        "name": "instance.volumes.replacements",
        "displayName": "instance.volumes.replacements",
        "isReconfigurable": false,
        "recommendedValue": "/hadoop/hdfs/journal",
        "value": "/hadoop/hdfs/journal",
        "category": "ACCUMULO",
        "filename": "accumulo-site",
        "serviceName": 'MISC'
      },
      {
        "name": "hbase.rootdir",
        "displayName": "hbase.rootdir",
        "description": "Ambari Metrics service uses HBase as default storage backend. Set the rootdir for HBase to either local filesystem path if using Ambari Metrics in embedded mode or to a HDFS dir, example: hdfs://namenode.example.org:8020/amshbase.",
        "isReconfigurable": false,
        "recommendedValue": "file:///var/lib/ambari-metrics-collector/hbase",
        "value": "file:///var/lib/ambari-metrics-collector/hbase",
        "category": "AMBARI_METRICS",
        "isVisible": false,
        "filename": "ams-hbase-site",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "RANGER",
        "filename": "ranger-env",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "HDFS",
        "filename": "ranger-hdfs-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "HDFS",
        "isVisible": false,
        "filename": "ranger-hdfs-plugin-properties",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "KAFKA",
        "isVisible": false,
        "filename": "ranger-kafka-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "KNOX",
        "isVisible": false,
        "filename": "ranger-knox-plugin-properties",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "RANGER_KMS",
        "isVisible": false,
        "filename": "ranger-kms-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "STORM",
        "isVisible": false,
        "filename": "ranger-storm-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "STORM",
        "isVisible": false,
        "filename": "ranger-storm-plugin-properties",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "HBASE",
        "isVisible": false,
        "filename": "ranger-hbase-plugin-properties",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "HIVE",
        "isVisible": false,
        "filename": "ranger-hive-plugin-properties",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "HBASE",
        "isVisible": false,
        "filename": "ranger-hbase-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "KNOX",
        "isVisible": false,
        "filename": "ranger-knox-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "HIVE",
        "isVisible": false,
        "filename": "ranger-hive-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "YARN",
        "isVisible": false,
        "filename": "ranger-yarn-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "ATLAS",
        "isVisible": false,
        "filename": "ranger-atlas-audit",
        "serviceName": 'MISC'
      },
      {
        "name": "xasecure.audit.destination.hdfs.dir",
        "displayName": "Destination HDFS Directory",
        "description": "HDFS folder to write audit to, make sure all service user has required permissions. This property is overridable at service level.",
        "isReconfigurable": false,
        "recommendedValue": "hdfs://haCluster",
        "value": "hdfs://haCluster",
        "category": "RANGER_KMS",
        "isVisible": false,
        "filename": "ranger-kms-audit",
        "serviceName": 'MISC'
      },
    /**********************************************HAWQ***************************************/
      {
        "name": "hawq_dfs_url",
        "displayName": "hawq_dfs_url",
        "description": "URL for Accessing HDFS",
        "isReconfigurable": false,
        "recommendedValue": "haCluster/hawq_data",
        "value": "haCluster/hawq_data",
        "category": "HAWQ",
        "filename": "hawq-site",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.nameservices",
        "displayName": "dfs.nameservices",
        "description": "Comma-separated list of nameservices.",
        "isReconfigurable": false,
        "recommendedValue": "haCluster",
        "value": "haCluster",
        "category": "HAWQ",
        "filename": "hdfs-client",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.ha.namenodes.${dfs.nameservices}",
        "displayName": "dfs.ha.namenodes.${dfs.nameservices}",
        "description": "The prefix for a given nameservice, contains a comma-separated list of namenodes for a given nameservice.",
        "isReconfigurable": false,
        "recommendedValue": "nn1,nn2",
        "value": "nn1,nn2",
        "category": "HAWQ",
        "filename": "hdfs-client",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.rpc-address.${dfs.nameservices}.nn1",
        "description": "RPC address that handles all clients requests for nn1.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:8020",
        "value": "0.0.0.0:8020",
        "category": "HAWQ",
        "filename": "hdfs-client",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.rpc-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.rpc-address.${dfs.nameservices}.nn2",
        "description": "RPC address that handles all clients requests for nn2.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:8020",
        "value": "0.0.0.0:8020",
        "category": "HAWQ",
        "filename": "hdfs-client",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.${dfs.nameservices}.nn1",
        "displayName": "dfs.namenode.http-address.${dfs.nameservices}.nn1",
        "description": "The fully-qualified HTTP address for nn1 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:50070",
        "value": "0.0.0.0:50070",
        "category": "HAWQ",
        "filename": "hdfs-client",
        "serviceName": 'MISC'
      },
      {
        "name": "dfs.namenode.http-address.${dfs.nameservices}.nn2",
        "displayName": "dfs.namenode.http-address.${dfs.nameservices}.nn2",
        "description": "The fully-qualified HTTP address for nn2 NameNode.",
        "isReconfigurable": false,
        "recommendedValue": "0.0.0.0:50070",
        "value": "0.0.0.0:50070",
        "category": "HAWQ",
        "filename": "hdfs-client",
        "serviceName": 'MISC'
      }
    ]
  }
};
