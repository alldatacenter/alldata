#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#Usage: Use this script in kerberos enabled hadoop only. Run this script after kinit'ing as hdfs user
#This script creates the folders in HDFS required by Apache Ranger for writing Audit records
#Note 1: Use this script only for kerberos environment. In kerberos environment, Ranger KMS writes the audit logs as user "HTTP"
#Note 2: Please update the below variables according to your environment

HBASE_USER_GROUP=hbase:hbase
HDFS_USER_GROUP=hdfs:hdfs
HIVE_USER_GROUP=hive:hive
KAFKA_USER_GROUP=kafka:kafka
KMS_USER_GROUP=HTTP:HTTP
KNOX_USER_GROUP=knox:knox
SOLR_USER_GROUP=solr:solr
STORM_USER_GROUP=storm:storm
YARN_USER_GROUP=yarn:yarn

set -x

#Create parent folder with rx permission
hdfs dfs -mkdir -p /ranger/audit
hdfs dfs -chown $HDFS_USER_GROUP /ranger/audit
hdfs dfs -chmod 755 /ranger
hdfs dfs -chmod 755 /ranger/audit

hdfs dfs -mkdir -p /ranger/audit/hbaseMaster
hdfs dfs -chown $HBASE_USER_GROUP /ranger/audit/hbaseMaster
hdfs dfs -chmod -R 0700 /ranger/audit/hbaseMaster

hdfs dfs -mkdir -p /ranger/audit/hbaseRegional
hdfs dfs -chown $HBASE_USER_GROUP /ranger/audit/hbaseRegional
hdfs dfs -chmod -R 0700 /ranger/audit/hbaseRegional

hdfs dfs -mkdir -p /ranger/audit/hdfs
hdfs dfs -chown $HDFS_USER_GROUP /ranger/audit/hdfs
hdfs dfs -chmod -R 0700 /ranger/audit/hdfs

hdfs dfs -mkdir -p /ranger/audit/hiveServer2
hdfs dfs -chown $HIVE_USER_GROUP /ranger/audit/hiveServer2
hdfs dfs -chmod -R 0700 /ranger/audit/hiveServer2

hdfs dfs -mkdir -p /ranger/audit/kafka
hdfs dfs -chown $KAFKA_USER_GROUP /ranger/audit/kafka
hdfs dfs -chmod -R 0700 /ranger/audit/kafka

hdfs dfs -mkdir -p /ranger/audit/kms
hdfs dfs -chown $KMS_USER_GROUP /ranger/audit/kms
hdfs dfs -chmod -R 0700 /ranger/audit/kms

hdfs dfs -mkdir -p /ranger/audit/knox
hdfs dfs -chown $KNOX_USER_GROUP /ranger/audit/knox
hdfs dfs -chmod -R 0700 /ranger/audit/knox

hdfs dfs -mkdir -p /ranger/audit/solr
hdfs dfs -chown $SOLR_USER_GROUP /ranger/audit/solr
hdfs dfs -chmod -R 0700 /ranger/audit/solr

hdfs dfs -mkdir -p /ranger/audit/storm
hdfs dfs -chown $STORM_USER_GROUP /ranger/audit/storm
hdfs dfs -chmod -R 0700 /ranger/audit/storm

hdfs dfs -mkdir -p /ranger/audit/yarn
hdfs dfs -chown $YARN_USER_GROUP /ranger/audit/yarn
hdfs dfs -chmod -R 0700 /ranger/audit/yarn

