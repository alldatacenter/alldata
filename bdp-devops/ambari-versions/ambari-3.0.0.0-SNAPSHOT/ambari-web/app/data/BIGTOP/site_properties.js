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

var App = require('app');

module.exports =
{
  "configProperties": [
  //***************************************** BIGTOP stack **************************************
  /**********************************************HDFS***************************************/
    {
      "name": "dfs.namenode.checkpoint.dir",
      "displayName": "SecondaryNameNode Checkpoint directories",
      "displayType": "directories",
      "isOverridable": false,
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "category": "SECONDARY_NAMENODE",
      "index": 1
    },
    {
      "name": "dfs.namenode.checkpoint.period",
      "displayName": "HDFS Maximum Checkpoint Delay",
      "displayType": "int",
      "unit": "seconds",
      "category": "General",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "index": 3
    },
    {
      "name": "dfs.namenode.name.dir",
      "displayName": "NameNode directories",
      "displayType": "directories",
      "isOverridable": false,
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "category": "NAMENODE",
      "index": 1
    },
    {
      "name": "dfs.webhdfs.enabled",
      "displayName": "WebHDFS enabled",
      "displayType": "checkbox",
      "isOverridable": false,
      "category": "General",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "index": 0
    },
    {
      "name": "dfs.datanode.failed.volumes.tolerated",
      "displayName": "DataNode volumes failure toleration",
      "displayType": "int",
      "category": "DATANODE",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "index": 3
    },
    {
      "name": "dfs.datanode.data.dir",
      "displayName": "DataNode directories",
      "displayType": "directories",
      "category": "DATANODE",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "index": 1
    },
    {
      "name": "dfs.datanode.data.dir.perm",
      "displayName": "DataNode directories permission",
      "displayType": "int",
      "category": "DATANODE",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml"
    },
    {
      "name": "dfs.replication",
      "displayName": "Block replication",
      "displayType": "int",
      "category": "General",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml"
    },
    {
      "name": "dfs.datanode.du.reserved",
      "displayName": "Reserved space for HDFS",
      "displayType": "int",
      "unit": "bytes",
      "category": "General",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml",
      "index": 2
    },
    {
      "name": "dfs.client.read.shortcircuit",
      "displayName": "HDFS Short-circuit read",
      "displayType": "checkbox",
      "category": "Advanced hdfs-site",
      "serviceName": "HDFS",
      "filename": "hdfs-site.xml"
    },
    {
      "name": "apache_artifacts_download_url",
      "displayName": "apache_artifacts_download_url",
      "description": "",
      "isRequired": false,
      "isRequiredByAgent": false,
      "isVisible": false,
      "category": "Advanced hdfs-site",
      "serviceName": "HDFS"
    },

  /**********************************************YARN***************************************/
    {
      "name": "yarn.acl.enable",
      "displayName": "yarn.acl.enable",
      "displayType": "checkbox",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "RESOURCEMANAGER"
    },
    {
      "name": "yarn.admin.acl",
      "displayName": "yarn.admin.acl",
      "isRequired": false,
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "RESOURCEMANAGER"
    },
    {
      "name": "yarn.log-aggregation-enable",
      "displayName": "yarn.log-aggregation-enable",
      "displayType": "checkbox",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "RESOURCEMANAGER"
    },
    {
      "name": "yarn.resourcemanager.scheduler.class",
      "displayName": "yarn.resourcemanager.scheduler.class",
      "serviceName": "YARN",
      "category": "CapacityScheduler",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.scheduler.minimum-allocation-mb",
      "displayName": "yarn.scheduler.minimum-allocation-mb",
      "displayType": "int",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "CapacityScheduler"
    },
    {
      "name": "yarn.scheduler.maximum-allocation-mb",
      "displayName": "yarn.scheduler.maximum-allocation-mb",
      "displayType": "int",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "CapacityScheduler"
    },
    {
      "name": "yarn.nodemanager.resource.memory-mb",
      "displayName": "yarn.nodemanager.resource.memory-mb",
      "displayType": "int",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.vmem-pmem-ratio",
      "displayName": "yarn.nodemanager.vmem-pmem-ratio",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.linux-container-executor.group",
      "displayName": "yarn.nodemanager.linux-container-executor.group",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.log-dirs",
      "displayName": "yarn.nodemanager.log-dirs",
      "displayType": "directories",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.local-dirs",
      "displayName": "yarn.nodemanager.local-dirs",
      "displayType": "directories",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.remote-app-log-dir",
      "displayName": "yarn.nodemanager.remote-app-log-dir",
      "displayType": "directory",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.remote-app-log-dir-suffix",
      "displayName": "yarn.nodemanager.remote-app-log-dir-suffix",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.aux-services",
      "displayName": "yarn.nodemanager.aux-services",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.nodemanager.log.retain-second",
      "displayName": "yarn.nodemanager.log.retain-second",
      "serviceName": "YARN",
      "filename": "yarn-site.xml",
      "category": "NODEMANAGER"
    },
    {
      "name": "yarn.log.server.url",
      "displayName": "yarn.log.server.url",
      "category": "Advanced yarn-site",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.enabled",
      "displayName": "yarn.timeline-service.enabled",
      "category": "APP_TIMELINE_SERVER",
      "displayType": "checkbox",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.leveldb-timeline-store.path",
      "displayName": "yarn.timeline-service.leveldb-timeline-store.path",
      "category": "APP_TIMELINE_SERVER",
      "displayType": "directory",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.leveldb-timeline-store.ttl-interval-ms",
      "displayName": "yarn.timeline-service.leveldb-timeline-store.ttl-interval-ms",
      "displayType": "int",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.store-class",
      "displayName": "yarn.timeline-service.store-class",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.ttl-enable",
      "displayName": "yarn.timeline-service.ttl-enable",
      "displayType": "checkbox",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.ttl-ms",
      "displayName": "yarn.timeline-service.ttl-ms",
      "displayType": "int",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.generic-application-history.store-class",
      "displayName": "yarn.timeline-service.generic-application-history.store-class",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.webapp.address",
      "displayName": "yarn.timeline-service.webapp.address",
      "displayType": "string",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.webapp.https.address",
      "displayName": "yarn.timeline-service.webapp.https.address",
      "displayType": "string",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
    {
      "name": "yarn.timeline-service.address",
      "displayName": "yarn.timeline-service.address",
      "displayType": "string",
      "category": "APP_TIMELINE_SERVER",
      "serviceName": "YARN",
      "filename": "yarn-site.xml"
    },
  /**********************************************MAPREDUCE2***************************************/
    {
      "name": "mapreduce.map.memory.mb",
      "displayName": "Default virtual memory for a job's map-task",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-site.xml"
    },
    {
      "name": "mapreduce.reduce.memory.mb",
      "displayName": "Default virtual memory for a job's reduce-task",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-site.xml"
    },
    {
      "name": "mapreduce.task.io.sort.mb",
      "displayName": "Map-side sort buffer memory",
      "displayType": "int",
      "unit": "MB",
      "category": "General",
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-site.xml"
    },
    {
      "name": "hadoop.security.auth_to_local",
      "displayName": "hadoop.security.auth_to_local",
      "displayType": "multiLine",
      "serviceName": "HDFS",
      "filename": "core-site.xml",
      "category": "Advanced core-site"
    },
    {
      "name": "yarn.app.mapreduce.am.resource.mb",
      "displayName": "yarn.app.mapreduce.am.resource.mb",
      "displayType": "int",
      "category": "Advanced mapred-site",
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-site.xml"
    },

  /**********************************************oozie-site***************************************/
    {
      "name": "oozie.db.schema.name",
      "displayName": "Database Name",
      "isOverridable": false,
      "displayType": "host",
      "isObserved": true,
      "category": "OOZIE_SERVER",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 4
    },
    {
      "name": "oozie.service.JPAService.jdbc.username",
      "displayName": "Database Username",
      "isOverridable": false,
      "displayType": "host",
      "category": "OOZIE_SERVER",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 5
    },
    {
      "name": "oozie.service.JPAService.jdbc.password",
      "displayName": "Database Password",
      "isOverridable": false,
      "displayType": "password",
      "category": "OOZIE_SERVER",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 6
    },
    {
      "name": "oozie.service.JPAService.jdbc.driver", // the default value of this property is overriden in code
      "displayName": "JDBC Driver Class",
      "isOverridable": false,
      "category": "OOZIE_SERVER",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 7
    },
    {
      "name": "oozie.service.JPAService.jdbc.url",
      "displayName": "Database URL",
      "isOverridable": false,
      "displayType": "advanced",
      "category": "OOZIE_SERVER",
      "serviceName": "OOZIE",
      "filename": "oozie-site.xml",
      "index": 8
    },

  /**********************************************hive-site***************************************/
    {
      "name": "javax.jdo.option.ConnectionDriverName",  // the default value is overwritten in code
      "displayName": "JDBC Driver Class",
      "isOverridable": false,
      "category": "HIVE_METASTORE",
      "serviceName": "HIVE",
      "filename": "hive-site.xml",
      "index": 7
    },
    {
      "name": "hive.heapsize",
      "displayName": "Hive heap size",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "serviceName": "HIVE",
      "filename": "hive-site.xml",
      "category": "General",
      "index": 9
    },
    {
      "name": "javax.jdo.option.ConnectionUserName",
      "displayName": "Database Username",
      "displayType": "host",
      "isOverridable": false,
      "category": "HIVE_METASTORE",
      "serviceName": "HIVE",
      "filename": "hive-site.xml",
      "index": 5
    },
    {
      "name": "javax.jdo.option.ConnectionPassword",
      "displayName": "Database Password",
      "displayType": "password",
      "isOverridable": false,
      "category": "HIVE_METASTORE",
      "serviceName": "HIVE",
      "filename": "hive-site.xml",
      "index": 6
    },
    {
      "name": "javax.jdo.option.ConnectionURL",
      "displayName": "Database URL",
      "displayType": "advanced",
      "isOverridable": false,
      "category": "HIVE_METASTORE",
      "serviceName": "HIVE",
      "filename": "hive-site.xml",
      "index": 8
    },
    {
      "name": "ambari.hive.db.schema.name",
      "displayName": "Database Name",
      "displayType": "host",
      "isOverridable": false,
      "isObserved": true,
      "serviceName": "HIVE",
      "filename": "hive-site.xml",
      "category": "HIVE_METASTORE",
      "index": 4
    },
    {
      "name": "hive.server2.tez.default.queues",
      "displayName": "hive.server2.tez.default.queues",
      "isRequired": false,
      "serviceName": "HIVE",
      "category": "Advanced hive-site"
    },
    {
      "name": "hive.server2.thrift.port",
      "displayName": "Hive Server Port",
      "description": "TCP port number to listen on, default 10000.",
      "recommendedValue": "10000",
      "displayType": "int",
      "isReconfigurable": true,
      "isOverridable": false,
      "isVisible": true,
      "category": "Advanced hive-site",
      "serviceName": "HIVE",
      "filename": "hive-site.xml"
    },
    {
      "name": "hive.server2.support.dynamic.service.discovery",
      "displayName": "hive.server2.support.dynamic.service.discovery",
      "recommendedValue": true,
      "displayType": "checkbox",
      "category": "Advanced hive-site",
      "serviceName": "HIVE"
    },
    {
      "name": "hive.zookeeper.quorum",
      "displayName": "hive.zookeeper.quorum",
      "recommendedValue": "localhost:2181",
      "displayType": "multiLine",
      "isVisible": true,
      "serviceName": "HIVE",
      "category": "Advanced hive-site"
    },
  /**********************************************tez-site*****************************************/
    {
      "name": "tez.am.resource.memory.mb",
      "displayName": "tez.am.resource.memory.mb",
      "displayType": "int",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.am.java.opts",
      "displayName": "tez.am.java.opts",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.am.grouping.split-waves",
      "displayName": "tez.am.grouping.split-waves",
      "displayType": "float",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.am.grouping.min-size",
      "displayName": "tez.am.grouping.min-size",
      "displayType": "int",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.am.grouping.max-size",
      "displayName": "tez.am.grouping.max-size",
      "displayType": "int",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.am.log.level",
      "displayName": "tez.am.log.level",
      "displayType": "string",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.runtime.intermediate-input.compress.codec",
      "displayName": "tez.runtime.intermediate-input.compress.codec",
      "displayType": "string",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.runtime.intermediate-input.is-compressed",
      "displayName": "tez.runtime.intermediate-input.is-compressed",
      "displayType": "checkbox",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.runtime.intermediate-output.compress.codec",
      "displayName": "tez.runtime.intermediate-output.compress.codec",
      "displayType": "string",
      "category": "General",
      "serviceName": "TEZ"
    },
    {
      "name": "tez.runtime.intermediate-output.should-compress",
      "displayName": "tez.runtime.intermediate-output.should-compress",
      "displayType": "checkbox",
      "category": "General",
      "serviceName": "TEZ"
    },

  /**********************************************hbase-site***************************************/
    {
      "name": "hbase.tmp.dir",
      "displayName": "HBase tmp directory",
      "displayType": "directory",
      "category": "Advanced hbase-site",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.master.port",
      "displayName": "HBase Master Port",
      "isReconfigurable": true,
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "category": "Advanced hbase-site"
    },
    {
      "name": "hbase.regionserver.global.memstore.upperLimit",
      "displayName": "hbase.regionserver.global.memstore.upperLimit",
      "displayType": "float",
      "category": "Advanced hbase-site",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.regionserver.global.memstore.lowerLimit",
      "displayName": "hbase.regionserver.global.memstore.lowerLimit",
      "displayType": "float",
      "category": "Advanced hbase-site",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.hstore.blockingStoreFiles",
      "displayName": "hstore blocking storefiles",
      "displayType": "int",
      "category": "Advanced hbase-site",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.hstore.compactionThreshold",
      "displayName": "HBase HStore compaction threshold",
      "displayType": "int",
      "category": "General",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 0
    },
    {
      "name": "hfile.block.cache.size",
      "displayName": "HFile block cache size ",
      "displayType": "float",
      "category": "General",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 1
    },
    {
      "name": "hbase.hregion.max.filesize",
      "displayName": "Maximum HStoreFile Size",
      "displayType": "int",
      "unit": "bytes",
      "category": "General",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 2
    },
    {
      "name": "hbase.regionserver.handler.count",
      "displayName": "RegionServer Handler",
      "displayType": "int",
      "category": "HBASE_REGIONSERVER",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 2
    },
    {
      "name": "hbase.hregion.majorcompaction",
      "displayName": "HBase Region Major Compaction",
      "displayType": "int",
      "unit": "ms",
      "category": "HBASE_REGIONSERVER",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 3
    },
    {
      "name": "hbase.hregion.memstore.block.multiplier",
      "displayName": "HBase Region Block Multiplier",
      "displayType": "int",
      "category": "HBASE_REGIONSERVER",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 4
    },
    {
      "name": "hbase.hregion.memstore.mslab.enabled",
      "displayName": "hbase.hregion.memstore.mslab.enabled",
      "displayType": "checkbox",
      "category": "Advanced hbase-site",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.hregion.memstore.flush.size",
      "displayName": "HBase Region Memstore Flush Size",
      "displayType": "int",
      "unit": "bytes",
      "category": "HBASE_REGIONSERVER",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 5
    },
    {
      "name": "hbase.client.scanner.caching",
      "displayName": "HBase Client Scanner Caching",
      "displayType": "int",
      "unit": "rows",
      "category": "General",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 3
    },
    {
      "name": "zookeeper.session.timeout",
      "displayName": "Zookeeper timeout for HBase Session",
      "displayType": "int",
      "unit": "ms",
      "category": "General",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 4
    },
    {
      "name": "hbase.client.keyvalue.maxsize",
      "displayName": "HBase Client Maximum key-value Size",
      "displayType": "int",
      "unit": "bytes",
      "category": "General",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "index": 5
    },
    {
      "name": "hbase.coprocessor.region.classes",
      "displayName": "hbase.coprocessor.region.classes",
      "category": "Advanced hbase-site",
      "isRequired": false,
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.coprocessor.master.classes",
      "displayName": "hbase.coprocessor.master.classes",
      "category": "Advanced hbase-site",
      "isRequired": false,
      "serviceName": "HBASE",
      "filename": "hbase-site.xml"
    },
    {
      "name": "hbase.zookeeper.quorum",
      "displayName": "hbase.zookeeper.quorum",
      "displayType": "multiLine",
      "serviceName": "HBASE",
      "filename": "hbase-site.xml",
      "category": "Advanced hbase-site"
    },

  /**********************************************storm-site***************************************/
    {
      "name": "storm.zookeeper.root",
      "displayName": "storm.zookeeper.root",
      "displayType": "directory",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.local.dir",
      "displayName": "storm.local.dir",
      "displayType": "directory",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.zookeeper.servers",
      "displayName": "storm.zookeeper.servers",
      "displayType": "componentHosts",
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.zookeeper.port",
      "displayName": "storm.zookeeper.port",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.zookeeper.session.timeout",
      "displayName": "storm.zookeeper.session.timeout",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.zookeeper.connection.timeout",
      "displayName": "storm.zookeeper.connection.timeout",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.zookeeper.retry.times",
      "displayName": "storm.zookeeper.retry.times",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.zookeeper.retry.interval",
      "displayName": "storm.zookeeper.retry.interval",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General",
      "unit": "ms"
    },
    {
      "name": "storm.zookeeper.retry.intervalceiling.millis",
      "displayName": "storm.zookeeper.retry.intervalceiling.millis",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "General",
      "unit": "ms"
    },
    {
      "name": "storm.cluster.mode",
      "displayName": "storm.cluster.mode",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.local.mode.zmq",
      "displayName": "storm.local.mode.zmq",
      "displayType": "checkbox",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.thrift.transport",
      "displayName": "storm.thrift.transport",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "name": "storm.messaging.transport",
      "displayName": "storm.messaging.transport",
      "serviceName": "STORM",
      "category": "General"
    },
    {
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.buffer_size",
      "name": "storm.messaging.netty.buffer_size",
      "displayType": "int",
      "unit": "bytes"
    },
    {
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.max_retries",
      "name": "storm.messaging.netty.max_retries",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.max_wait_ms",
      "name": "storm.messaging.netty.max_wait_ms",
      "displayType": "int",
      "unit": "ms"
    },
    {
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.min_wait_ms",
      "name": "storm.messaging.netty.min_wait_ms",
      "displayType": "int",
      "unit": "ms"
    },
    {
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.server_worker_threads",
      "name": "storm.messaging.netty.server_worker_threads",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "General",
      "displayName": "storm.messaging.netty.client_worker_threads",
      "name": "storm.messaging.netty.client_worker_threads",
      "displayType": "int"
    },
    {
      "name": "nimbus.host",
      "displayName": "nimbus.host",
      "displayType": "masterHost",
      "isOverridable": false,
      "isReconfigurable": false,
      "serviceName": "STORM",
      "category": "NIMBUS"
    },
    {
      "name": "nimbus.thrift.port",
      "displayName": "nimbus.thrift.port",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS"
    },
    {
      "name": "nimbus.thrift.max_buffer_size",
      "displayName": "nimbus.thrift.max_buffer_size",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "bytes"
    },
    {
      "name": "nimbus.childopts",
      "displayName": "nimbus.childopts",
      "displayType": "multiLine",
      "isOverridable": false,
      "serviceName": "STORM",
      "category": "NIMBUS",
      "filename": "storm-site.xml"
    },
    {
      "name": "nimbus.task.timeout.secs",
      "displayName": "nimbus.task.timeout.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.supervisor.timeout.secs",
      "displayName": "nimbus.supervisor.timeout.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.monitor.freq.secs",
      "displayName": "nimbus.monitor.freq.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.cleanup.inbox.freq.secs",
      "displayName": "nimbus.cleanup.inbox.freq.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.inbox.jar.expiration.secs",
      "displayName": "nimbus.inbox.jar.expiration.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.task.launch.secs",
      "displayName": "nimbus.task.launch.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.reassign",
      "displayName": "nimbus.reassign",
      "displayType": "checkbox",
      "isReconfigurable": true,
      "serviceName": "STORM",
      "category": "NIMBUS"
    },
    {
      "name": "nimbus.file.copy.expiration.secs",
      "displayName": "nimbus.file.copy.expiration.secs",
      "displayType": "int",
      "serviceName": "STORM",
      "category": "NIMBUS",
      "unit": "seconds"
    },
    {
      "name": "nimbus.topology.validator",
      "displayName": "nimbus.topology.validator",
      "serviceName": "STORM",
      "category": "NIMBUS"
    },
    {
      "name": "supervisor.slots.ports",
      "displayName": "supervisor.slots.ports",
      "displayType": "string",
      "serviceName": "STORM",
      "category": "SUPERVISOR"
    },
    {
      "isOverrideable": false,
      "serviceName": "STORM",
      "category": "SUPERVISOR",
      "displayName": "supervisor.childopts",
      "name": "supervisor.childopts",
      "displayType": "multiLine",
      "filename": "storm-site.xml"
    },
    {
      "serviceName": "STORM",
      "category": "SUPERVISOR",
      "displayName": "supervisor.worker.start.timeout.secs",
      "name": "supervisor.worker.start.timeout.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "serviceName": "STORM",
      "category": "SUPERVISOR",
      "displayName": "supervisor.worker.timeout.secs",
      "name": "supervisor.worker.timeout.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "serviceName": "STORM",
      "category": "SUPERVISOR",
      "displayName": "supervisor.monitor.frequency.secs",
      "name": "supervisor.monitor.frequency.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "serviceName": "STORM",
      "category": "SUPERVISOR",
      "displayName": "supervisor.heartbeat.frequency.secs",
      "name": "supervisor.heartbeat.frequency.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "serviceName": "STORM",
      "category": "DRPC_SERVER",
      "displayName": "drpc.port",
      "name": "drpc.port",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "DRPC_SERVER",
      "displayName": "drpc.worker.threads",
      "name": "drpc.worker.threads",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "DRPC_SERVER",
      "displayName": "drpc.queue.size",
      "name": "drpc.queue.size",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "DRPC_SERVER",
      "displayName": "drpc.invocations.port",
      "name": "drpc.invocations.port",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "DRPC_SERVER",
      "displayName": "drpc.request.timeout.secs",
      "name": "drpc.request.timeout.secs",
      "displayType": "int",
      "unit": "seconds"
    },
    {
      "serviceName": "STORM",
      "category": "DRPC_SERVER",
      "displayName": "drpc.childopts",
      "name": "drpc.childopts",
      "displayType": "string"
    },
    {
      "serviceName": "STORM",
      "category": "STORM_UI_SERVER",
      "displayName": "ui.port",
      "name": "ui.port",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "STORM_UI_SERVER",
      "displayName": "ui.childopts",
      "name": "ui.childopts",
      "displayType": "string"
    },
    //@Todo: uncomment following properties when logviewer is treated as different section on storm service page
    /*
    {
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.port",
      "name": "logviewer.port",
      "displayType": "int"
    },
    {
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.childopts",
      "name": "logviewer.childopts",
      "displayType": "string"
    },
    {
      "serviceName": "STORM",
      "category": "LogviewerServer",
      "displayName": "logviewer.appender.name",
      "name": "logviewer.appender.name",
      "displayType": "string"
    },
    */
    {
      "serviceName": "STORM",
      "category": "Advanced storm-site",
      "displayName": "worker.childopts",
      "name": "worker.childopts",
      "displayType": "multiLine",
      "filename": "storm-site.xml"
    },
  /*********************************************oozie-site for Falcon*****************************/
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-job-submit-instances",
      "name": "oozie.service.ELService.ext.functions.coord-job-submit-instances",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-action-create-inst",
      "name": "oozie.service.ELService.ext.functions.coord-action-create-inst",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-action-create",
      "name": "oozie.service.ELService.ext.functions.coord-action-create",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-job-submit-data",
      "name": "oozie.service.ELService.ext.functions.coord-job-submit-data",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-action-start",
      "name": "oozie.service.ELService.ext.functions.coord-action-start",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-sla-submit",
      "name": "oozie.service.ELService.ext.functions.coord-sla-submit",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },
    {
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "category": "Falcon - Oozie integration",
      "displayName": "oozie.service.ELService.ext.functions.coord-sla-create",
      "name": "oozie.service.ELService.ext.functions.coord-sla-create",
      "displayType": "custom",
      "filename": "oozie-site.xml"
    },

    // Runtime properties
    {
      "name": "*.domain",
      "displayName": "*.domain",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"

    },
    {
      "name": "*.log.cleanup.frequency.minutes.retention",
      "displayName": "*.log.cleanup.frequency.minutes.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },
    {
      "name": "*.log.cleanup.frequency.hours.retention",
      "displayName": "*.log.cleanup.frequency.hours.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },
    {
      "name": "*.log.cleanup.frequency.days.retention",
      "displayName": "*.log.cleanup.frequency.days.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },
    {
      "name": "*.log.cleanup.frequency.months.retention",
      "displayName": "*.log.cleanup.frequency.months.retention",
      "category": "FalconRuntimeSite",
      "serviceName": "FALCON",
      "filename": "falcon-runtime.properties.xml"
    },

    //  Startup properties

    {
      "name": "*.domain",
      "displayName": "*.domain",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.workflow.engine.impl",
      "displayName": "*.workflow.engine.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.oozie.process.workflow.builder",
      "displayName": "*.oozie.process.workflow.builder",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.oozie.feed.workflow.builder",
      "displayName": "*.oozie.feed.workflow.builder",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.SchedulableEntityManager.impl",
      "displayName": "*.SchedulableEntityManager.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.ConfigSyncService.impl",
      "displayName": "*.ConfigSyncService.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.ProcessInstanceManager.impl",
      "displayName": "*.ProcessInstanceManager.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.catalog.service.impl",
      "displayName": "*.catalog.service.impl",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.application.services",
      "displayName": "*.application.services",
      "displayType": "multiLine",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.configstore.listeners",
      "displayName": "*.configstore.listeners",
      "displayType": "multiLine",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.broker.impl.class",
      "displayName": "*.broker.impl.class",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.shared.libs",
      "displayName": "*.shared.libs",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.config.store.uri",
      "displayName": "*.config.store.uri",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.system.lib.location",
      "displayName": "*.system.lib.location",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.retry.recorder.path",
      "displayName": "*.retry.recorder.path",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.cleanup.service.frequency",
      "displayName": "*.falcon.cleanup.service.frequency",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.broker.url",
      "displayName": "*.broker.url",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.broker.ttlInMins",
      "displayName": "*.broker.ttlInMins",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.entity.topic",
      "displayName": "*.entity.topic",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.max.retry.failure.count",
      "displayName": "*.max.retry.failure.count",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.internal.queue.size",
      "displayName": "*.internal.queue.size",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.authentication.type",
      "displayName": "*.falcon.authentication.type",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.http.authentication.type",
      "displayName": "*.falcon.http.authentication.type",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.http.authentication.token.validity",
      "displayName": "*.falcon.http.authentication.token.validity",
      "displayType": "int",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.http.authentication.signature.secret",
      "displayName": "*.falcon.http.authentication.signature.secret",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.http.authentication.simple.anonymous.allowed",
      "displayName": "*.falcon.http.authentication.simple.anonymous.allowed",
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.http.authentication.kerberos.name.rules",
      "displayName": "*.falcon.http.authentication.kerberos.name.rules",
      "category": "FalconStartupSite",
      "displayType": "multiLine",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },
    {
      "name": "*.falcon.http.authentication.blacklisted.users",
      "displayName": "*.falcon.http.authentication.blacklisted.users",
      "isRequired": false,
      "category": "FalconStartupSite",
      "serviceName": "FALCON",
      "filename": "falcon-startup.properties.xml"
    },

  /**********************************************webhcat-site***************************************/
    {
      "name": "templeton.hive.archive",
      "displayName": "templeton.hive.archive",
      "isRequired": false,
      "serviceName": "HIVE",
      "filename": "webhcat-site.xml",
      "category": "Advanced webhcat-site"
    },
    {
      "name": "templeton.pig.archive",
      "displayName": "templeton.pig.archive",
      "isRequired": false,
      "serviceName": "HIVE",
      "filename": "webhcat-site.xml",
      "category": "Advanced webhcat-site"
    },
    {
      "name": "templeton.zookeeper.hosts",
      "displayName": "templeton.zookeeper.hosts",
      "displayType": "multiLine",
      "serviceName": "HIVE",
      "filename": "webhcat-site.xml",
      "category": "Advanced webhcat-site"
    },
  /**********************************************pig.properties*****************************************/
    {
      "name": "content",
      "displayName": "content",
      "value": "",
      "recommendedValue": "",
      "description": "pig properties",
      "displayType": "content",
      "isRequired": false,
      "showLabel": false,
      "serviceName": "PIG",
      "filename": "pig-properties.xml",
      "category": "Advanced pig-properties"
    },
  /********************************************* flume-agent *****************************/
    {
      "name": "content",
      "displayName": "content",
      "showLabel": false,
      "isRequired": false,
      "displayType": "content",
      "serviceName": "FLUME",
      "category": "FLUME_HANDLER",
      "filename": "flume-conf.xml"
    },
    {
      "name": "flume_conf_dir",
      "displayName": "Flume Conf Dir",
      "description": "Location to save configuration files",
      "recommendedValue": "/etc/flume/conf",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "FLUME",
      "filename": "flume-env.xml",
      "category": "Advanced flume-env",
      "index": 0
    },
    {
      "name": "flume_log_dir",
      "displayName": "Flume Log Dir",
      "description": "Location to save log files",
      "recommendedValue": "/var/log/flume",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "FLUME",
      "filename": "flume-env.xml",
      "category": "Advanced flume-env",
      "index": 1
    },
  /**********************************************HDFS***************************************/
    {
      "name": "namenode_heapsize",
      "displayName": "NameNode Java heap size",
      "description": "Initial and maximum Java heap size for NameNode (Java options -Xms and -Xmx).  This also applies to the Secondary NameNode.",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "NAMENODE",
      "index": 2
    },
    {
      "name": "namenode_opt_newsize",
      "displayName": "NameNode new generation size",
      "description": "Default size of Java new generation for NameNode (Java option -XX:NewSize).  This also applies to the Secondary NameNode.",
      "recommendedValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "NAMENODE",
      "index": 3
    },
    {
      "name": "namenode_opt_maxnewsize",
      "displayName": "NameNode maximum new generation size",
      "description": "Maximum size of Java new generation for NameNode (Java option -XX:MaxnewSize).",
      "recommendedValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "NAMENODE",
      "index": 4
    },
    {
      "name": "namenode_opt_permsize",
      "displayName": "NameNode permanent generation size",
      "description": "Default size of Java new generation for NameNode (Java option -XX:PermSize).  This also applies to the Secondary NameNode.",
      "recommendedValue": "128",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "NAMENODE",
      "index": 5
    },
    {
      "name": "namenode_opt_maxpermsize",
      "displayName": "NameNode maximum permanent generation size",
      "description": "Maximum size of Java permanent generation for NameNode (Java option -XX:MaxPermSize).",
      "recommendedValue": "256",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "NAMENODE",
      "index": 6
    },
    {
      "name": "dtnode_heapsize",
      "displayName": "DataNode maximum Java heap size",
      "description": "Maximum Java heap size for DataNode (Java option -Xmx)",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "DATANODE",
      "index": 2
    },
    {
      "name": "hadoop_heapsize",
      "displayName": "Hadoop maximum Java heap size",
      "description": "Maximum Java heap size for daemons such as Balancer (Java option -Xmx)",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "General",
      "index": 1
    },
    {
      "name": "hdfs_log_dir_prefix",
      "displayName": "Hadoop Log Dir Prefix",
      "description": "The parent directory for Hadoop log files.  The HDFS log directory will be ${hadoop_log_dir_prefix} / ${hdfs_user} and the MapReduce log directory will be ${hadoop_log_dir_prefix} / ${mapred_user}.",
      "recommendedValue": "/var/log/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "Advanced hadoop-env"
    },
    {
      "name": "hadoop_pid_dir_prefix",
      "displayName": "Hadoop PID Dir Prefix",
      "description": "The parent directory in which the PID files for Hadoop processes will be created.  The HDFS PID directory will be ${hadoop_pid_dir_prefix} / ${hdfs_user} and the MapReduce PID directory will be ${hadoop_pid_dir_prefix} / ${mapred_user}.",
      "recommendedValue": "/var/run/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "Advanced hadoop-env"
    },
    {
      "name": "hadoop_root_logger",
      "displayName": "Hadoop Root Logger",
      "description": "Hadoop logging options",
      "recommendedValue": "INFO,RFA",
      "displayType": "string",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HDFS",
      "filename": "hadoop-env.xml",
      "category": "Advanced hadoop-env"
    },

  /**********************************************MAPREDUCE2***************************************/
    {
      "name": "jobhistory_heapsize",
      "displayName": "History Server heap size",
      "description": "History Server heap size",
      "recommendedValue": "900",
      "unit": "MB",
      "isOverridable": true,
      "displayType": "int",
      "isVisible": true,
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-env.xml",
      "category": "HISTORYSERVER",
      "index": 1
    },
    {
      "name": "mapred_log_dir_prefix",
      "displayName": "Mapreduce Log Dir Prefix",
      "description": "",
      "recommendedValue": "/var/log/hadoop-mapreduce",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isReconfigurable": false,
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-env.xml",
      "category": "Advanced mapred-env"
    },
    {
      "name": "mapred_pid_dir_prefix",
      "displayName": "Mapreduce PID Dir Prefix",
      "description": "",
      "recommendedValue": "/var/run/hadoop-mapreduce",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isReconfigurable": false,
      "serviceName": "MAPREDUCE2",
      "filename": "mapred-env.xml",
      "category": "Advanced mapred-env"
    },
  /**********************************************YARN***************************************/
    {
      "name": "yarn_heapsize",
      "displayName": "YARN Java heap size",
      "description": "Max heapsize for all YARN components",
      "recommendedValue": "1024",
      "isOverridable": true,
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "General",
      "index": 0
    },
    {
      "name": "resourcemanager_heapsize",
      "displayName": "ResourceManager Java heap size",
      "description": "Max heapsize for ResourceManager",
      "recommendedValue": "1024",
      "isOverridable": false,
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "RESOURCEMANAGER",
      "index": 1
    },
    {
      "name": "nodemanager_heapsize",
      "displayName": "NodeManager Java heap size",
      "description": "Max heapsize for NodeManager",
      "recommendedValue": "1024",
      "isOverridable": true,
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "NODEMANAGER",
      "index": 0
    },
    {
      "name": "yarn_log_dir_prefix",
      "displayName": "YARN Log Dir Prefix",
      "description": "",
      "recommendedValue": "/var/log/hadoop-yarn",
      "displayType": "directory",
      "isOverridable": false,
      "isReconfigurable": false,
      "isVisible": true,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "Advanced yarn-env"
    },
    {
      "name": "yarn_pid_dir_prefix",
      "displayName": "YARN PID Dir Prefix",
      "description": "",
      "recommendedValue": "/var/run/hadoop-yarn",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isReconfigurable": false,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "Advanced yarn-env"
    },
    {
      "name": "min_user_id",
      "displayName": "Minimum user ID for submitting job",
      "isOverridable": true,
      "displayType": "int",
      "isVisible": true,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "Advanced yarn-env"
    },
    {
      "name": "apptimelineserver_heapsize",
      "displayName": "AppTimelineServer Java heap size",
      "description": "AppTimelineServer Java heap size",
      "recommendedValue": "1024",
      "isOverridable": false,
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "YARN",
      "filename": "yarn-env.xml",
      "category": "APP_TIMELINE_SERVER",
      "index": 1
    },
  /**********************************************HBASE***************************************/
    {
      "name": "hbase_master_heapsize",
      "displayName": "HBase Master Maximum Java heap size",
      "description": "Maximum Java heap size for HBase master (Java option -Xmx)",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": true,
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-env.xml",
      "category": "HBASE_MASTER",
      "index": 1
    },
    {
      "name": "hbase_regionserver_heapsize",
      "displayName": "RegionServers maximum Java heap size",
      "description": "Maximum Java heap size for RegionServers (Java option -Xmx)",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-env.xml",
      "category": "HBASE_REGIONSERVER",
      "index": 1
    },
    {
      "name": "hbase_regionserver_xmn_max",
      "displayName": "RegionServers maximum value for -Xmn",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-env.xml",
      "category": "HBASE_REGIONSERVER",
      "index": 6
    },
    {
      "name": "hbase_regionserver_xmn_ratio",
      "displayName": "RegionServers -Xmn in -Xmx ratio",
      "displayType": "float",
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-env.xml",
      "category": "HBASE_REGIONSERVER",
      "index": 7
    },
    {
      "name": "hbase_log_dir",
      "displayName": "HBase Log Dir",
      "description": "Directory for HBase logs",
      "recommendedValue": "/var/log/hbase",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-env.xml",
      "category": "Advanced hbase-env"
    },
    {
      "name": "hbase_pid_dir",
      "displayName": "HBase PID Dir",
      "description": "Directory in which the pid files for HBase processes will be created",
      "recommendedValue": "/var/run/hbase",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HBASE",
      "filename": "hbase-env.xml",
      "category": "Advanced hbase-env"
    },
     //***************************************** GLUSTERFS stack********************************************
    {
      "name": "fs.glusterfs.impl",
      "displayName": "GlusterFS fs impl",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
    {
      "name": "fs.AbstractFileSystem.glusterfs.impl",
      "displayName": "GlusterFS Abstract File System Implementation",
      "displayType": "string",
      "filename": "core-site.xml",
      "serviceName": "GLUSTERFS",
      "category": "General"
    },
  /**********************************************GLUSTERFS***************************************/
    {
      "name": "fs_glusterfs_default_name",
      "displayName": "GlusterFS default fs name 1.x Hadoop",
      "description": "GlusterFS default filesystem name (glusterfs://{MasterFQDN}:9000)",
      "recommendedValue": "glusterfs:///localhost:8020",
      "displayType": "string",
      "isVisible": true,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "glusterfs_defaultFS_name",
      "displayName": "GlusterFS default fs name 2.x Hadoop",
      "description": "GlusterFS default filesystem name (glusterfs:///)",
      "recommendedValue": "glusterfs:///localhost:8020",
      "displayType": "string",
      "isVisible": true,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "hadoop_heapsize",
      "displayName": "Hadoop maximum Java heap size",
      "description": "Maximum Java heap size for daemons such as Balancer (Java option -Xmx)",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": true,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop",
      "index": 1
    },
    {
      "name": "hdfs_log_dir_prefix",
      "displayName": "Hadoop Log Dir Prefix",
      "description": "The parent directory for Hadoop log files.  The HDFS log directory will be ${hadoop_log_dir_prefix} / ${hdfs_user} and the MapReduce log directory will be ${hadoop_log_dir_prefix} / ${mapred_user}.",
      "recommendedValue": "/var/log/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "hadoop_pid_dir_prefix",
      "displayName": "Hadoop PID Dir Prefix",
      "description": "The parent directory in which the PID files for Hadoop processes will be created.  The HDFS PID directory will be ${hadoop_pid_dir_prefix} / ${hdfs_user} and the MapReduce PID directory will be ${hadoop_pid_dir_prefix} / ${mapred_user}.",
      "recommendedValue": "/var/run/hadoop",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "namenode_heapsize",
      "displayName": "Name Node Heap Size",
      "description": "Name Node Heap Size, default jvm memory setting",
      "recommendedValue": "1024",
      "isReconfigurable": false,
      "displayType": "int",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "namenode_opt_newsize",
      "displayName": "NameNode new generation size",
      "description": "Default size of Java new generation for NameNode (Java option -XX:NewSize).  This also applies to the Secondary NameNode.",
      "recommendedValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "namenode_opt_maxnewsize",
      "displayName": "NameNode maximum new generation size",
      "description": "Maximum size of Java new generation for NameNode (Java option -XX:MaxnewSize).",
      "recommendedValue": "200",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "namenode_opt_permsize",
      "displayName": "NameNode permanent generation size",
      "description": "Default size of Java permanent generation for NameNode (Java option -XX:PermSize).  This also applies to the Secondary NameNode.",
      "recommendedValue": "128",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "namenode_opt_maxpermsize",
      "displayName": "NameNode maximum permanent generation size",
      "description": "Maximum size of Java permanent generation for NameNode (Java option -XX:MaxPermSize).",
      "recommendedValue": "256",
      "displayType": "int",
      "unit": "MB",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "dtnode_heapsize",
      "displayName": "DataNode maximum Java heap size",
      "description": "Maximum Java heap size for DataNode (Java option -Xmx)",
      "recommendedValue": "1024",
      "displayType": "int",
      "unit": "MB",
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
    {
      "name": "glusterfs_user",
      "displayName": "glusterfs user",
      "description": "glusterfs user",
      "recommendedValue": "root",
      "displayType": "string",
      "isVisible": false,
      "serviceName": "GLUSTERFS",
      "filename": "hadoop-env.xml",
      "category": "General Hadoop"
    },
  /**********************************************HIVE***************************************/
    {
      "name": "hive_database",
      "displayName": "Hive Database",
      "value": "",
      "recommendedValue": "New PostgreSQL Database",
      "options": [
        {
          displayName: 'New PostgreSQL Database'
        },
        {
          displayName: 'Existing MySQL Database'
        },
        {
          displayName: 'Existing PostgreSQL Database'
        },
        {
          displayName: 'Existing Oracle Database'
        }
      ],
      "description": "PostgreSQL will be installed by Ambari",
      "displayType": "radio button",
      "isReconfigurable": true,
      "radioName": "hive-database",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "HIVE_METASTORE",
      "index": 2
    },
    {
      "name": "hive_hostname",
      "displayName": "Database Host",
      "description": "Specify the host on which the database is hosted",
      "savedValue": "",
      "recommendedValue": "",
      "isReconfigurable": true,
      "displayType": "host",
      "isOverridable": false,
      "isVisible": false,
      "isObserved": true,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "HIVE_METASTORE",
      "index": 3
    },
    {
      "name": "hive_metastore_port",
      "displayName": "Hive metastore port",
      "description": "",
      "recommendedValue": "9083",
      "isReconfigurable": false,
      "displayType": "int",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced hive-env"
    },
    {
      "name": "hive_lib",
      "displayName": "Hive library",
      "description": "",
      "recommendedValue": "/usr/lib/hive/lib/",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "isRequiredByAgent": false, // Make this to true when we expose the property on ui by making "isVisible": true
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced hive-env"
    },
    {
      "name": "hive_dbroot",
      "displayName": "Hive db directory",
      "description": "",
      "recommendedValue": "/usr/lib/hive/lib",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "isRequiredByAgent": false, // Make this to true when we expose the property on ui by making "isVisible": true
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced hive-env"
    },
    {
      "name": "hive_log_dir",
      "displayName": "Hive Log Dir",
      "description": "Directory for Hive log files",
      "recommendedValue": "/var/log/hive",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced hive-env"
    },
    {
      "name": "hive_pid_dir",
      "displayName": "Hive PID Dir",
      "description": "Directory in which the PID files for Hive processes will be created",
      "recommendedValue": "/var/run/hive",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced hive-env"
    },
  /**********************************************HIVE***************************************/
    {
      "name": "hcat_log_dir",
      "displayName": "WebHCat Log Dir",
      "description": "Directory for WebHCat log files",
      "recommendedValue": "/var/log/webhcat",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced webhcat-env"
    },
    {
      "name": "hcat_pid_dir",
      "displayName": "WebHCat PID Dir",
      "description": "Directory in which the PID files for WebHCat processes will be created",
      "recommendedValue": "/var/run/webhcat",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "HIVE",
      "filename": "hive-env.xml",
      "category": "Advanced webhcat-env"
    },
  /**********************************************OOZIE***************************************/
    {
      "name": "oozie_database",
      "displayName": "Oozie Database",
      "value": "",
      "recommendedValue": "New Derby Database",
      "options": [
        {
          displayName: 'New Derby Database'
        },
        {
          displayName: 'Existing MySQL Database'
        },
        {
          displayName: 'Existing PostgreSQL Database'
        },
        {
          displayName: 'Existing Oracle Database'
        }
      ],
      "description": "Current Derby Database will be installed by Ambari",
      "displayType": "radio button",
      "isReconfigurable": true,
      "isOverridable": false,
      "radioName": "oozie-database",
      "isVisible": true,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "OOZIE_SERVER",
      "index": 2
    },
    {
      "name": "oozie_data_dir",
      "displayName": "Oozie Data Dir",
      "description": "Data directory in which the Oozie DB exists",
      "isReconfigurable": true,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isRequired": false,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "OOZIE_SERVER",
      "index": 9
    },
    {
      "name": "oozie_hostname",
      "savedValue": "",
      "recommendedValue": "",
      "displayName": "Database Host",
      "description": "The host where the Oozie database is located",
      "isReconfigurable": true,
      "isOverridable": false,
      "displayType": "host",
      "isVisible": false,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "OOZIE_SERVER",
      "index": 3
    },
    {
      "name": "oozie_log_dir",
      "displayName": "Oozie Log Dir",
      "description": "Directory for oozie logs",
      "recommendedValue": "/var/log/oozie",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "Advanced oozie-env"
    },
    {
      "name": "oozie_pid_dir",
      "displayName": "Oozie PID Dir",
      "description": "Directory in which the pid files for oozie processes will be created",
      "recommendedValue": "/var/run/oozie",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "Advanced oozie-env"
    },
    {
      "name": "oozie_admin_port",
      "displayName": "Oozie Server Admin Port",
      "isReconfigurable": true,
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "OOZIE",
      "filename": "oozie-env.xml",
      "category": "Advanced oozie-env"
    },
  /**********************************************ZOOKEEPER***************************************/
    {
      "name": "zk_data_dir",
      "displayName": "ZooKeeper directory",
      "description": "Data directory for ZooKeeper",
      "isReconfigurable": true,
      "displayType": "directory",
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZOOKEEPER_SERVER",
      "index": 1
    },
    {
      "name": "tickTime",
      "displayName": "Length of single Tick",
      "description": "The length of a single tick in milliseconds, which is the basic time unit used by ZooKeeper",
      "recommendedValue": "2000",
      "displayType": "int",
      "unit": "ms",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZOOKEEPER_SERVER",
      "index": 2
    },
    {
      "name": "initLimit",
      "displayName": "Ticks to allow for sync at Init",
      "description": "Amount of time, in ticks to allow followers to connect and sync to a leader",
      "recommendedValue": "10",
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZOOKEEPER_SERVER",
      "index": 3
    },
    {
      "name": "syncLimit",
      "displayName": "Ticks to allow for sync at Runtime",
      "description": "Amount of time, in ticks to allow followers to connect",
      "recommendedValue": "5",
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZOOKEEPER_SERVER",
      "index": 4
    },
    {
      "name": "clientPort",
      "displayName": "Port for running ZK Server",
      "description": "Port for running ZooKeeper server",
      "recommendedValue": "2181",
      "displayType": "int",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "ZOOKEEPER_SERVER",
      "index": 5
    },
    {
      "name": "zk_log_dir",
      "displayName": "ZooKeeper Log Dir",
      "description": "Directory for ZooKeeper log files",
      "recommendedValue": "/var/log/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "Advanced zookeeper-env",
      "index": 0
    },
    {
      "name": "zk_pid_dir",
      "displayName": "ZooKeeper PID Dir",
      "description": "Directory in which the pid files for zookeeper processes will be created",
      "recommendedValue": "/var/run/zookeeper",
      "isReconfigurable": false,
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "ZOOKEEPER",
      "filename": "zookeeper-env.xml",
      "category": "Advanced zookeeper-env",
      "index": 1
    },
  /**********************************************GANGLIA***************************************/
    {
      "name": "ganglia_conf_dir",
      "displayName": "Ganglia conf directory",
      "description": "",
      "recommendedValue": "/etc/ganglia/hdp",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "isRequiredByAgent": false,
      "serviceName": "GANGLIA",
      "filename": "ganglia-env.xml",
      "category": "Advanced ganglia-env"
    },
  /**********************************************FALCON***************************************/
    {
      "name": "falcon_port",
      "displayName": "Falcon server port",
      "description": "Port the Falcon Server listens on",
      "recommendedValue": "15000",
      "isReconfigurable": true,
      "displayType": "int",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "FALCON_SERVER"
    },
    {
      "name": "falcon_local_dir",
      "displayName": "Falcon data directory",
      "description": "Directory where Falcon data, such as activemq data, is stored",
      "recommendedValue": "/hadoop/falcon",
      "isReconfigurable": true,
      "displayType": "directory",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "FALCON_SERVER"
    },
    {
      "name": "falcon_store_uri",
      "displayName": "Falcon store URI",
      "description": "Directory where entity definitions are stored",
      "recommendedValue": "file:///hadoop/falcon/store",
      "isReconfigurable": true,
      "displayType": "string",
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "FALCON_SERVER"
    },
    {
      "name": "falcon_log_dir",
      "displayName": "Falcon Log Dir",
      "description": "Directory for Falcon logs",
      "recommendedValue": "/var/log/falcon",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "Advanced falcon-env"
    },
    {
      "name": "falcon_pid_dir",
      "displayName": "Falcon PID Dir",
      "description": "Directory in which the pid files for Falcon processes will be created",
      "recommendedValue": "/var/run/falcon",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "isReconfigurable": false,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "Advanced falcon-env"
    },
    {
      "name": "falcon.embeddedmq",
      "displayName": "falcon.embeddedmq",
      "description": "Whether embeddedmq is enabled or not.",
      "recommendedValue": "true",
      "displayType": "string",
      "isOverridable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "Advanced falcon-env"
    },
    {
      "name": "falcon.embeddedmq.data",
      "displayName": "falcon.embeddedmq.data",
      "description": "Directory in which embeddedmq data is stored.",
      "recommendedValue": "/hadoop/falcon/embeddedmq/data",
      "displayType": "directory",
      "isOverridable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "Advanced falcon-env"
    },
    {
      "name": "falcon.emeddedmq.port",
      "displayName": "falcon.emeddedmq.port",
      "description": "Port that embeddedmq will listen on.",
      "recommendedValue": "61616",
      "displayType": "string",
      "isOverridable": false,
      "isVisible": true,
      "isRequiredByAgent": true,
      "isReconfigurable": true,
      "serviceName": "FALCON",
      "filename": "falcon-env.xml",
      "category": "Advanced falcon-env"
    },
  /**********************************************STORM***************************************/
    {
      "name": "storm_log_dir",
      "displayName": "storm_log_dir",
      "description": "Storm log directory",
      "recommendedValue": "/var/log/storm",
      "displayType": "directory",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "filename": "storm-env.xml",
      "category": "Advanced storm-env"
    },
    {
      "name": "storm_pid_dir",
      "displayName": "storm_pid_dir",
      "description": "Storm PID directory",
      "recommendedValue": "/var/run/storm",
      "displayType": "directory",
      "isReconfigurable": true,
      "isVisible": true,
      "isRequiredByAgent": true,
      "serviceName": "STORM",
      "filename": "storm-env.xml",
      "category": "Advanced storm-env"
    },
  /**********************************************MISC***************************************/
    {
      "name": "hbase_conf_dir",
      "displayName": "HBase conf dir",
      "description": "",
      "recommendedValue": "/etc/hbase",
      "isRequired": false,
      "displayType": "directory",
      "isVisible": false,
      "isRequiredByAgent": false,
      "serviceName": "MISC",
      "filename": "hbase-env.xml",
      "category": "General",
      "belongsToService": []
    },
    {
      "name": "proxyuser_group",
      "displayName": "Proxy group for Hive, WebHCat, Oozie and Falcon",
      "description": "",
      "recommendedValue": "users",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "filename": "hadoop-env.xml",
      "serviceName": "MISC",
      "category": "Users and Groups",
      "belongsToService": ["HIVE", "OOZIE", "FALCON"],
      "index": 18
    },
    {
      "name": "ganglia_runtime_dir",
      "displayName": "Ganglia runtime directory",
      "description": "",
      "recommendedValue": "/var/run/ganglia/hdp",
      "isReconfigurable": false,
      "displayType": "directory",
      "isVisible": false,
      "serviceName": "MISC",
      "filename": "ganglia-env.xml",
      "category": "General",
      "belongsToService": []
    },
    {
      "name": "hdfs_user",
      "displayName": "HDFS User",
      "description": "User to run HDFS as",
      "recommendedValue": "hdfs",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "hadoop-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HDFS"],
      "index": 1
    },
    {
      "name": "mapred_user",
      "displayName": "MapReduce User",
      "description": "User to run MapReduce as",
      "recommendedValue": "mapred",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "mapred-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["MAPREDUCE2"],
      "index": 2
    },
    {
      "name": "yarn_user",
      "displayName": "YARN User",
      "description": "User to run YARN as",
      "recommendedValue": "yarn",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "yarn-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["YARN"],
      "index": 3
    },
    {
      "name": "hbase_user",
      "displayName": "HBase User",
      "description": "User to run HBase as",
      "recommendedValue": "hbase",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "hbase-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HBASE"],
      "index": 4
    },
    {
      "name": "hive_user",
      "displayName": "Hive User",
      "description": "User to run Hive as",
      "recommendedValue": "hive",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "hive-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HIVE"],
      "index": 5
    },
    {
      "name": "hcat_user",
      "displayName": "HCat User",
      "description": "User to run HCatalog as",
      "recommendedValue": "hcat",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "hive-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HIVE"],
      "index": 6
    },
    {
      "name": "webhcat_user",
      "displayName": "WebHCat User",
      "description": "User to run WebHCat as",
      "recommendedValue": "hcat",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "hive-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["HIVE"],
      "index": 7
    },
    {
      "name": "oozie_user",
      "displayName": "Oozie User",
      "description": "User to run Oozie as",
      "recommendedValue": "oozie",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "oozie-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["OOZIE"],
      "index": 8
    },
    {
      "name": "falcon_user",
      "displayName": "Falcon User",
      "description": "User to run Falcon as",
      "recommendedValue": "falcon",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "falcon-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["FALCON"],
      "index": 10
    },
    {
      "name": "storm_user",
      "displayName": "Storm User",
      "description": "User to run Storm as",
      "recommendedValue": "storm",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "storm-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["STORM"],
      "index": 9
    },
    {
      "name": "zk_user",
      "displayName": "ZooKeeper User",
      "description": "User to run ZooKeeper as",
      "recommendedValue": "zookeeper",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "zookeeper-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["ZOOKEEPER"],
      "index": 11
    },
    {
      "name": "flume_user",
      "displayName": "Flume User",
      "description": "User to run Flume as",
      "recommendedValue": "flume",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "flume-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["FLUME"],
      "index": 12
    },
    {
      "name": "gmetad_user",
      "displayName": "Ganglia User",
      "description": "The user used to run Ganglia",
      "recommendedValue": "nobody",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "ganglia-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["GANGLIA"],
      "index": 13
    },
    {
      "name": "gmond_user",
      "displayName": "Gmond User",
      "description": "The user used to run gmond for Ganglia",
      "recommendedValue": "nobody",
      "isReconfigurable": false,
      "displayType": "advanced",
      "isOverridable": false,
      "isVisible": false,
      "serviceName": "MISC",
      "filename": "ganglia-env.xml",
      "category": "Users and Groups",
      "belongsToService": []
    },
    {
      "name": "tez_user",
      "displayName": "Tez User",
      "description": "User to run Tez as",
      "recommendedValue": "tez",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "tez-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["TEZ"],
      "index": 15
    },
    {
      "name": "sqoop_user",
      "displayName": "Sqoop User",
      "isReconfigurable": false,
      "displayType": "user",
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "MISC",
      "filename": "sqoop-env.xml",
      "category": "Users and Groups",
      "belongsToService": ["SQOOP"],
      "index": 17
    },
    {
      "name": "rrdcached_base_dir",
      "displayName": "Ganglia rrdcached base directory",
      "description": "Default directory for saving the rrd files on ganglia server",
      "recommendedValue": "/var/lib/ganglia/rrds",
      "displayType": "directory",
      "isReconfigurable": true,
      "isOverridable": false,
      "isVisible": true,
      "serviceName": "GANGLIA",
      "filename": "ganglia-env.xml",
      "category": "General",
      "belongsToService": ["GANGLIA"]
    },
    {
      "name": "ignore_groupsusers_create",
      "displayName": "Skip group modifications during install",
      "displayType": "checkbox",
      "isReconfigurable": true,
      "isOverridable": false,
      "isVisible": true,
      "filename": "cluster-env.xml",
      "category": "Users and Groups"
    }
  ]
};
