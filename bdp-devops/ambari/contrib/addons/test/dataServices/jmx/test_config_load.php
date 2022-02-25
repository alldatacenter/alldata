<?php
/*
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

$GLOBALS["HDP_MON_DEBUG_MODE"] = FALSE;
$pwd = exec("pwd");
$GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"] = $pwd
    ."/data/cluster_configuration.json";

include_once("../../../src/dataServices/common/common.inc");
include_once("../../../src/dataServices/common/cluster_configuration.inc");

hdp_mon_load_cluster_configuration();

if (!isset($GLOBALS["HDP_MON_CONFIG"])) {
  error_log("global CONFIG is still not set");
  exit(1);
}

assert($GLOBALS["HDP_MON_CONFIG"]["STACK_VERSION"] === "1.0.2");
assert($GLOBALS["HDP_MON_CONFIG"]["CLUSTER_NAME"] === "MyHDPCluster");

assert($GLOBALS["HDP_MON_CONFIG"]["HDP_MON"]["DASHBOARD_HOST"] ===
    "dashboard_host");
assert($GLOBALS["HDP_MON_CONFIG"]["HDP_MON"]["DASHBOARD_PORT"] === 80);

assert($GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_HOST"] === "gangliaweb_host");
assert($GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_PORT"] === 80);
assert($GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["WEB_ROOT"] ===
    "/var/www/ganglia2");
assert($GLOBALS["HDP_MON_CONFIG"]["GANGLIA"]["GRID_NAME"] === "HDP_GRID");

assert($GLOBALS["HDP_MON_CONFIG"]["NAGIOS"]["NAGIOSSERVER_HOST"] ===
    "nagiosserver_host");
assert($GLOBALS["HDP_MON_CONFIG"]["NAGIOS"]["NAGIOSSERVER_PORT"] === 80);

assert($GLOBALS["HDP_MON_CONFIG"]["JMX"]["TIMEOUT"] === 1);

assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["NAMENODE_HOST"] ===
    "namenode");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["NAMENODE_PORT"] ===
    50070);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["NAMENODE_ADDR"] ===
    "namenode:50070");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["SECONDARY_NAMENODE_ADDR"]
    === "snamenode:50071");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]["TOTAL_DATANODES"] === 10);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]
    ["GANGLIA_CLUSTERS"]["NAMENODE"] === "HDPNameNode");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]
    ["GANGLIA_CLUSTERS"]["SLAVES"] === "HDPSlaves");

assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBTRACKER_HOST"]
    === "jobtracker");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBTRACKER_PORT"]
    === 50030);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBTRACKER_ADDR"]
    === "jobtracker:50030");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["TOTAL_TASKTRACKERS"]
    === 20);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBHISTORY_HOST"]
    === "jobhistory_host");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]["JOBHISTORY_PORT"]
    === 52890);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]
    ["GANGLIA_CLUSTERS"]["JOBTRACKER"] === "HDPJobTracker");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]
    ["GANGLIA_CLUSTERS"]["SLAVES"] === "HDPSlaves");

assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["HBASEMASTER_HOST"]
    === "hbasemaster");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["HBASEMASTER_PORT"]
    === 60010);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["HBASEMASTER_ADDR"]
    === "hbasemaster:60010");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]["TOTAL_REGIONSERVERS"]
    === 30);
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]
    ["GANGLIA_CLUSTERS"]["HBASEMASTER"] === "HDPHBaseMaster");
assert($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]
    ["GANGLIA_CLUSTERS"]["SLAVES"] === "HDPSlaves");
assert(!isset($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["ZOOKEEPER"]));
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HIVE-METASTORE"]));
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["TEMPLETON"]));
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["OOZIE"]));

$GLOBALS["HDP_MON_CLUSTER_CONFIG_LOCATION"] = $pwd
    ."/data/cluster_configuration.json.nohbase";

unset($GLOBALS["HDP_MON_CONFIG_INITIALIZED"]);
hdp_mon_load_cluster_configuration();

if (!isset($GLOBALS["HDP_MON_CONFIG"])) {
  error_log("global CONFIG is still not set");
  exit(1);
}
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HDFS"]));
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["MAPREDUCE"]));
assert(!isset($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HBASE"]));
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["ZOOKEEPER"]));
assert(!isset($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["HIVE-METASTORE"]));
assert(!isset($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["TEMPLETON"]));
assert(is_array($GLOBALS["HDP_MON_CONFIG"]["SERVICES"]["OOZIE"]));

?>
