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
// All of the "name" properties have to coincide with how they will appear in the *-site.xml file
// The "template" properties can come from the config properties in site_properties.js or secure_properties.js .
var props = [
  {
    "name": "hadoop.security.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "core-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "hadoop.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "core-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "hadoop.rpc.protection",
    "templateName": [],
    "foreignKey": null,
    "value": "authentication,privacy",
    "filename": "core-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "hadoop.security.auth_to_local",
    "templateName": ["resourcemanager_primary_name", "kerberos_domain", "yarn_user", "nodemanager_primary_name", "namenode_primary_name", "hdfs_user", "datanode_primary_name", "hbase_master_primary_name", "hbase_user","hbase_regionserver_primary_name","oozie_primary_name","oozie_user","jobhistory_primary_name","mapred_user","journalnode_principal_name","falcon_primary_name","falcon_user"],
    "foreignKey": null,
    "value": "RULE:[2:$1@$0](<templateName[0]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[3]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[4]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[6]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[10]>@.*<templateName[1]>)s/.*/<templateName[11]>/\nRULE:[2:$1@$0](<templateName[12]>@.*<templateName[1]>)s/.*/<templateName[13]>/\nRULE:[2:$1@$0](<templateName[14]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[15]>@.*<templateName[1]>)s/.*/<templateName[16]>/\nDEFAULT",
    "filename": "core-site.xml",
    "serviceName": "HDFS",
    "dependedServiceName": [{name: "HBASE", replace: "\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/"},{name: "OOZIE",replace: "\nRULE:[2:$1@$0](<templateName[10]>@.*<templateName[1]>)s/.*/<templateName[11]>/"},{name: "MAPREDUCE2",replace: "\nRULE:[2:$1@$0](<templateName[12]>@.*<templateName[1]>)s/.*/<templateName[13]>/"}]
  },
  {
    "name": "dfs.namenode.kerberos.principal",
    "templateName": ["namenode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.namenode.keytab.file",
    "templateName": ["namenode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.principal",
    "templateName": ["snamenode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.secondary.namenode.keytab.file",
    "templateName": ["snamenode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.web.authentication.kerberos.principal",
    "templateName": ["hadoop_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.web.authentication.kerberos.keytab",
    "templateName": ["hadoop_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.kerberos.principal",
    "templateName": ["datanode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.keytab.file",
    "templateName": ["datanode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.namenode.kerberos.internal.spnego.principal",
    "templateName": [],
    "foreignKey": null,
    "value": "${dfs.web.authentication.kerberos.principal}",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.secondary.namenode.kerberos.internal.spnego.principal",
    "templateName": [],
    "foreignKey": null,
    "value": "${dfs.web.authentication.kerberos.principal}",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.journalnode.kerberos.principal",
    "templateName": ["journalnode_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.journalnode.kerberos.internal.spnego.principal",
    "templateName": ["hadoop_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.journalnode.keytab.file",
    "templateName": ["journalnode_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.address",
    "templateName": ["dfs_datanode_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "nonSecureValue": "0.0.0.0:50010",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.datanode.http.address",
    "templateName": ["dfs_datanode_http_address"],
    "foreignKey": null,
    "value": "0.0.0.0:<templateName[0]>",
    "nonSecureValue": "0.0.0.0:50075",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "dfs.data.transfer.protection",
    "templateName": [],
    "foreignKey": null,
    "value": "authentication,privacy",
    "filename": "hdfs-site.xml",
    "serviceName": "HDFS"
  },
  {
    "name": "mapreduce.jobhistory.principal",
    "templateName": ["jobhistory_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE2"
  },
  {
    "name": "mapreduce.jobhistory.keytab",
    "templateName": ["jobhistory_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE2"
  },
  {
    "name": "mapreduce.jobhistory.webapp.spnego-principal",
    "templateName": ["jobhistory_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE2"
  },
  {
    "name": "mapreduce.jobhistory.webapp.spnego-keytab-file",
    "templateName": ["jobhistory_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "mapred-site.xml",
    "serviceName": "MAPREDUCE2"
  },
  {
    "name": "yarn.timeline-service.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.acl.enable",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  // YARN Timeline Service
  // These "http-authentication" properties are supported in HDP Champlain
  {
    "name": "yarn.timeline-service.principal",
    "templateName": ["apptimelineserver_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.timeline-service.keytab",
    "templateName": ["apptimelineserver_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.timeline-service.http-authentication.type",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.timeline-service.http-authentication.kerberos.principal",
    "templateName": ["apptimelineserver_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.timeline-service.http-authentication.kerberos.keytab",
    "templateName": ["apptimelineserver_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  // YARN Resource Manager
  {
    "name": "yarn.resourcemanager.principal",
    "templateName": ["resourcemanager_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.resourcemanager.keytab",
    "templateName": ["resourcemanager_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.nodemanager.principal",
    "templateName": ["nodemanager_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.nodemanager.keytab",
    "templateName": ["nodemanager_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.resourcemanager.webapp.spnego-principal",
    "templateName": ["resourcemanager_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.resourcemanager.webapp.spnego-keytab-file",
    "templateName": ["resourcemanager_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.nodemanager.webapp.spnego-principal",
    "templateName": ["nodemanager_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.nodemanager.webapp.spnego-keytab-file",
    "templateName": ["nodemanager_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "yarn.nodemanager.container-executor.class",
    "templateName": ["yarn_nodemanager_container-executor_class"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "nonSecureValue": "org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor",
    "filename": "yarn-site.xml",
    "serviceName": "YARN"
  },
  {
    "name": "hbase.master.kerberos.principal",
    "templateName": ["hbase_master_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.master.keytab.file",
    "templateName": ["hbase_master_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.regionserver.kerberos.principal",
    "templateName": ["hbase_regionserver_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.regionserver.keytab.file",
    "templateName": ["hbase_regionserver_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hive.metastore.sasl.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.security.authorization.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "KERBEROS",
    "nonSecureValue": "NONE",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.metastore.kerberos.principal",
    "templateName": ["hive_metastore_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.metastore.kerberos.keytab.file",
    "templateName": ["hive_metastore_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication.kerberos.principal",
    "templateName": ["hive_metastore_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication.kerberos.keytab",
    "templateName": ["hive_metastore_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication.spnego.principal",
    "templateName": ["hive_metastore_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hive.server2.authentication.spnego.keytab",
    "templateName": ["hive_metastore_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "hive-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "oozie.service.AuthorizationService.authorization.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.HadoopAccessorService.kerberos.enabled",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "local.realm",
    "templateName": ["kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.HadoopAccessorService.keytab.file",
    "templateName": ["oozie_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.service.HadoopAccessorService.kerberos.principal",
    "templateName": ["oozie_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.type",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.kerberos.principal",
    "templateName": ["oozie_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.kerberos.keytab",
    "templateName": ["oozie_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "oozie.authentication.kerberos.name.rules",
    "templateName": ["resourcemanager_primary_name", "kerberos_domain", "yarn_user", "nodemanager_primary_name", "namenode_primary_name", "hdfs_user", "datanode_primary_name", "hbase_master_primary_name", "hbase_user","hbase_regionserver_primary_name"],
    "foreignKey": null,
    "value": "RULE:[2:$1@$0](<templateName[0]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[3]>@.*<templateName[1]>)s/.*/<templateName[2]>/\nRULE:[2:$1@$0](<templateName[4]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[6]>@.*<templateName[1]>)s/.*/<templateName[5]>/\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nDEFAULT",
    "filename": "oozie-site.xml",
    "serviceName": "OOZIE",
    "dependedServiceName": [{name: "HBASE", replace: "\nRULE:[2:$1@$0](<templateName[7]>@.*<templateName[1]>)s/.*/<templateName[8]>/\nRULE:[2:$1@$0](<templateName[9]>@.*<templateName[1]>)s/.*/<templateName[8]>/"}]
  },
  {
    "name": "templeton.kerberos.principal",
    "templateName": ["webHCat_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "templeton.kerberos.keytab",
    "templateName": ["webhcat_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "templeton.kerberos.secret",
    "templateName": [""],
    "foreignKey": null,
    "value": "secret",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "templeton.hive.properties",
    "templateName": ["hive_metastore","hive_metastore_principal_name","kerberos_domain"],
    "foreignKey": null,
    "value": "hive.metastore.local=false,hive.metastore.uris=<templateName[0]>,hive." +
      "metastore.sasl.enabled=true,hive.metastore.execute.setugi=true,hive.metastore.warehouse.dir=/apps/hive/warehouse,hive.exec.mode.local.auto=false,hive.metastore.kerberos.principal=<templateName[1]>@<templateName[2]>",
    "filename": "webhcat-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hbase.security.authentication",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.security.authorization",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "hbase.bulkload.staging.dir",
    "templateName": [],
    "foreignKey": null,
    "value": "/apps/hbase/staging",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },
  {
    "name": "zookeeper.znode.parent",
    "templateName": [],
    "foreignKey": null,
    "value": "/hbase-secure",
    "nonSecureValue": "/hbase-unsecure",
    "filename": "hbase-site.xml",
    "serviceName": "HBASE"
  },

  /***************************************FALCON***********************************************/
  {
    "name": "*.falcon.authentication.type",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },
  {
    "name": "*.falcon.http.authentication.type",
    "templateName": [],
    "foreignKey": null,
    "value": "kerberos",
    "nonSecureValue": "simple",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },
  {
    "name": "*.falcon.service.authentication.kerberos.principal",
    "templateName": ["falcon_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },
  {
    "name": "*.falcon.service.authentication.kerberos.keytab",
    "templateName": ["falcon_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },
  {
    "name": "*.falcon.http.authentication.kerberos.principal",
    "templateName": ["falcon_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },
  {
    "name": "*.falcon.http.authentication.kerberos.keytab",
    "templateName": ["falcon_http_keytab"],
    "foreignKey": null,
    "value": "<templateName[0]>",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },
  {
    "name": "*.dfs.namenode.kerberos.principal",
    "templateName": ["namenode_principal_name_falcon", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "filename": "falcon-startup.properties.xml",
    "serviceName": "FALCON"
  },

  /***************************************KNOX***********************************************/
  {
    "name": "gateway.hadoop.kerberos.secured",
    "templateName": [],
    "foreignKey": null,
    "value": "true",
    "nonSecureValue": "false",
    "filename": "gateway-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "java.security.krb5.conf",
    "templateName": [],
    "foreignKey": null,
    "value": "/etc/krb5.conf",
    "filename": "gateway-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["knox_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["knox_gateway_hosts"],
    "foreignKey": ["knox_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "webhcat.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["knox_primary_name"],
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "webhcat.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["knox_gateway_hosts"],
    "foreignKey": ["knox_primary_name"],
    "value": "<templateName[0]>",
    "filename": "webhcat-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "oozie.service.ProxyUserService.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["knox_primary_name"],
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "KNOX"
  },
  {
    "name": "oozie.service.ProxyUserService.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["knox_gateway_hosts"],
    "foreignKey": ["knox_primary_name"],
    "value": "<templateName[0]>",
    "filename": "oozie-site.xml",
    "serviceName": "KNOX"
  },
/***************************************core-site***************************************************/
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["hive_metastore_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["hive_metastore"],
    "foreignKey": ["hive_metastore_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["oozie_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["oozieserver_host"],
    "foreignKey": ["oozie_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "OOZIE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.groups",
    "templateName": ["proxyuser_group"],
    "foreignKey": ["webHCat_http_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  },
  {
    "name": "hadoop.proxyuser.<foreignKey[0]>.hosts",
    "templateName": ["webhcat_server"],
    "foreignKey": ["webHCat_http_primary_name"],
    "value": "<templateName[0]>",
    "filename": "core-site.xml",
    "serviceName": "HIVE"
  }
];

var yarn22Mapping = [
  {
    "name": 'hadoop.http.authentication.kerberos.principal',
    "templateName": ["hadoop_http_principal_name", "kerberos_domain"],
    "foreignKey": null,
    "value": "<templateName[0]>@<templateName[1]>",
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.kerberos.keytab',
    "foreignKey": null,
    "templateName": ["hadoop_http_keytab"],
    "value": "<templateName[0]>",
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    name: 'hadoop.http.authentication.kerberos.name.rules',
    templateName: [],
    foreignKey: null,
    value: "",
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.signature.secret',
    "templateName": [],
    "foreignKey": null,
    "value": "",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.signature.secret.file',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.signer.secret.provider',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.signer.secret.provider.object',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {

    "name": 'yarn.timeline-service.http-authentication.token.validity',
    "templateName": [],
    "foreignKey": null,
    "value": "",
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.cookie.domain',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.cookie.path',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.simple.anonymous.allowed',
    "value": "true",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.proxyuser.*.hosts',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.proxyuser.*.users',
    "value": "",
    "serviceName": "YARN",
    "templateName": [],
    "foreignKey": null,
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.proxyuser.*.groups',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'hadoop.http.filter.initializers',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.type',
    "value": "simple",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.signature.secret',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.signature.secret.file',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.signer.secret.provider',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.signer.secret.provider.object',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.token.validity',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.cookie.domain',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'hadoop.http.authentication.cookie.path',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "HDFS",
    "filename": "core-site.xml"
  },
  {
    "name": 'yarn.timeline-service.http-authentication.kerberos.name.rules',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.resourcemanager.proxyuser.*.hosts',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.resourcemanager.proxyuser.*.users',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.resourcemanager.proxyuser.*.groups',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.resourcemanager.proxy-user-privileges.enabled',
    "value": "true",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  },
  {
    "name": 'yarn.nodemanager.linux-container-executor.cgroups.mount-path',
    "value": "",
    "templateName": [],
    "foreignKey": null,
    "serviceName": "YARN",
    "filename": "yarn-site.xml"
  }
];

props.pushObjects(yarn22Mapping);

module.exports = props;
