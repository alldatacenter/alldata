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

module.exports = {
  configProperties: [
    {
      "name": "smokeuser_principal_name",
      "displayName": "Smoke user principal",
      "category": "Ambari Principals",
      "filename": "cluster-env.xml",
      "index": 1
    },
    {
      "name": "smokeuser_keytab",
      "displayName": "Smoke user keytab",
      "category": "Ambari Principals",
      "filename": "cluster-env.xml",
      "index": 2
    },
    {
      "name": "ambari-server_keytab",
      "displayName": "Ambari Keytab",
      "category": "Ambari Principals",
      "filename": "cluster-env.xml",
      "index": 3
    },
    {
      "name": "hdfs_principal_name",
      "displayName": "HDFS user principal",
      "category": "Ambari Principals",
      "filename": "hadoop-env.xml",
      "index": 4
    },
    {
      "name": "hdfs_user_keytab",
      "displayName": "HDFS user keytab",
      "category": "Ambari Principals",
      "filename": "hadoop-env.xml",
      "index": 5
    },
    {
      "name": "hbase_principal_name",
      "displayName": "HBase user principal",
      "category": "Ambari Principals",
      "filename": "hbase-env.xml",
      "index": 6
    },
    {
      "name": "hbase_user_keytab",
      "displayName": "HBase user keytab",
      "category": "Ambari Principals",
      "filename": "hbase-env.xml",
      "index": 7
    },
    {
      "name": "accumulo_principal_name",
      "displayName": "Accumulo user principal",
      "category": "Ambari Principals",
      "filename": "accumulo-env.xml",
      "index": 8
    },
    {
      "name": "accumulo_user_keytab",
      "displayName": "Accumulo user keytab",
      "category": "Ambari Principals",
      "filename": "accumulo-env.xml",
      "index": 9
    },
    {
      "name": "spark.history.kerberos.principal",
      "displayName": "Spark user principal",
      "category": "Ambari Principals",
      "filename": "spark-defaults.xml",
      "index": 10
    },
    {
      "name": "spark.history.kerberos.keytab",
      "displayName": "Spark user keytab",
      "category": "Ambari Principals",
      "filename": "spark-defaults.xml",
      "index": 11
    },
    {
      "name": "spark.history.kerberos.principal",
      "displayName": "Spark2 user principal",
      "category": "Ambari Principals",
      "filename": "spark2-defaults.xml",
      "index": 12
    },
    {
      "name": "spark.history.kerberos.keytab",
      "displayName": "Spark2 user keytab",
      "category": "Ambari Principals",
      "filename": "spark2-defaults.xml",
      "index": 13
    },
    {
      "name": "storm_principal_name",
      "displayName": "Storm user principal",
      "category": "Ambari Principals",
      "filename": "storm-env.xml",
      "index": 14
    },
    {
      "name": "storm_keytab",
      "displayName": "Storm user keytab",
      "category": "Ambari Principals",
      "filename": "storm-env.xml",
      "index": 15
    },
    {
      "name": "druid.hadoop.security.kerberos.principal",
      "displayName": "Druid user principal",
      "category": "Ambari Principals",
      "filename": "druid-common.xml",
      "index": 16
    },
    {
      "name": "druid.hadoop.security.kerberos.keytab",
      "displayName": "Druid user keytab",
      "category": "Ambari Principals",
      "filename": "druid-common.xml",
      "index": 17
    },
    {
      "name": "KERBEROS_PRINCIPAL",
      "displayName": "Superset user principal",
      "category": "Ambari Principals",
      "filename": "superset.xml",
      "index": 18
    },
    {
      "name": "KERBEROS_KEYTAB",
      "displayName": "Superset user keytab",
      "category": "Ambari Principals",
      "filename": "superset.xml",
      "index": 19
    }
  ]
};
