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
include_once("../../../src/dataServices/common/response.inc");
include_once("../../../src/dataServices/jmx/hdp_mon_jmx_helpers.inc");

function verify_hdfs_info($info) {
  assert(is_array($info));
  assert($info["service_type"] === "HDFS");
  assert($info["installed"]);

  assert($info["namenode_addr"] == "namenode:50070");
  assert($info["secondary_namenode_addr"] == "snamenode:50071");
  assert($info["total_nodes"] == 10);
  assert($info["memory_heap_used"] == 529321952);
  assert($info["memory_heap_max"] == 1006632960);
  assert($info["dfs_dirfiles_count"] == 554);
  assert($info["dfs_blocks_total"] == 458);
  assert($info["dfs_blocks_underreplicated"] == 0);
  assert($info["dfs_blocks_missing"] == 0);
  assert($info["dfs_blocks_corrupt"] == 0);
  assert($info["dfs_state"] == "Operational");
  assert($info["start_time"] == 1327557522);
  assert($info["live_nodes"] == 10);
  assert($info["dead_nodes"] == 1);
  assert($info["decommissioning_nodes"] == 0);
  assert($info["version"] == "1.0.0");
  assert($info["safemode"] == TRUE);
  assert($info["pending_upgrades"] == "");
  assert($info["dfs_configured_capacity"] == 36336891658240);
  assert($info["dfs_percent_used"] == 0);
  assert($info["dfs_percent_remaining"] == 99.08);
  assert($info["dfs_total_bytes"] == 36336891658240);
  assert($info["dfs_used_bytes"] == 1750237184);
  assert($info["nondfs_used_bytes"] == 331691536384);
  assert($info["dfs_free_bytes"] == 36003449884672);
  assert($info["safemode_reason"] != "");
}

function verify_mr_info($info) {
  assert(is_array($info));
  assert($info["service_type"] === "MAPREDUCE");
  assert($info["installed"]);

  assert($info["jobtracker_addr"] == "jobtracker:50030");
  assert($info["trackers_total"] == 20);
  assert($info["jobhistory_addr"] == "jobhistory_host:52890");
  assert($info["memory_heap_used"] == 158277552);
  assert($info["memory_heap_max"] == 1052770304);
  assert($info["trackers_live"] == 10);
  assert($info["trackers_graylisted"] == 0);
  assert($info["trackers_blacklisted"] == 0);
  assert($info["version"] == "1.0.0, r1224962");

  assert(is_array($info["queue_info"])
         && $info["queue_info"]["type"] == "CapacityTaskScheduler"
         && count($info["queue_info"]["queues"]) == 1);

  assert($info["queue_info"]["queues"]["default"]["state"] == "running"
    && $info["queue_info"]["queues"]["default"]["capacity_percentage"]
        == 100.0
    && $info["queue_info"]["queues"]["default"]["user_limit"] ==  100
    && $info["queue_info"]["queues"]["default"]["priority_supported"] == 1
    && $info["queue_info"]["queues"]["default"]["map_capacity"] == 40
    && $info["queue_info"]["queues"]["default"]["map_running_tasks"] ==  0
    && $info["queue_info"]["queues"]["default"]["reduce_capacity"] == 20
    && $info["queue_info"]["queues"]["default"]["reduce_running_tasks"] == 0
    && $info["queue_info"]["queues"]["default"]["waiting_jobs"] == 3
    && $info["queue_info"]["queues"]["default"]["initializing_jobs"] ==  0
    && $info["queue_info"]["queues"]["default"]["users_with_submitted_jobs"]
        == 0);

  assert($info["trackers_excluded"] == 0);
  assert($info["map_task_capacity"] == 40);
  assert($info["reduce_task_capacity"] == 20);
  assert($info["job_total_submissions"] == 105);
  assert($info["job_total_completions"] == 104);
  assert($info["running_jobs"] == 0);
  assert($info["waiting_jobs"] == 3);
  assert($info["running_map_tasks"] == 0);
  assert($info["running_reduce_tasks"] == 0);
  assert($info["occupied_map_slots"] == 0);
  assert($info["occupied_reduce_slots"] == 0);
  assert($info["reserved_map_slots"] == 0);
  assert($info["reserved_reduce_slots"] == 0);
  assert($info["waiting_maps"] == 1);
  assert($info["waiting_reduces"] == 0);
  assert($info["start_time"] == 1327557546);
  assert($info["average_node_capacity"] == 6);
}

function verify_hbase_info($info) {
  assert(is_array($info));
  assert($info["service_type"] === "HBASE");
  assert($info["installed"]);
  assert($info["total_regionservers"] === 30);
  assert($info["memory_heap_used"] === 32946880);
  assert($info["memory_heap_max"] === 1035468800);
  assert($info["cluster_id"] === "d24914d7-75d3-4dcc-9e6f-0d7770833993");
  assert($info["start_time"] == 1329244267);
  assert($info["active_time"] == 1329244269);
  assert(is_array($info["coprocessors"])
         && count($info["coprocessors"]) == 0);
  assert($info["average_load"] == 2);
  assert($info["regions_in_transition_count"] === 0);
  assert($info["live_regionservers"] === 1);
  assert($info["dead_regionservers"] === 0);
  assert(is_array($info["zookeeper_quorum"])
         && count($info["zookeeper_quorum"]) == 1
         && $info["zookeeper_quorum"][0] === "localhost:2181");
  assert($info["version"] ===
      "0.92.1-SNAPSHOT, ra23f8636efd6dd9d37f3a15d83f2396819509502");
}

function verify_overall_info($info) {
  assert(is_array($info));
  assert(is_array($info["overall"]));
  assert(is_array($info["hbase"]));
  assert(is_array($info["hdfs"]));
  assert(is_array($info["mapreduce"]));

  assert($info["overall"]["ganglia_url"] ==
      "http://gangliaweb_host:80/var/www/ganglia2");
  assert($info["overall"]["nagios_url"] == "http://nagiosserver_host:80/nagios");
  assert($info["overall"]["hdfs_installed"] == 1);
  assert($info["overall"]["mapreduce_installed"] == 1);
  assert($info["overall"]["hbase_installed"] == 1);
  assert($info["overall"]["namenode_addr"] == "namenode:50070");
  assert($info["overall"]["secondary_namenode_addr"] == "snamenode:50071");
  assert($info["overall"]["namenode_starttime"] == 1327557522);
  assert($info["overall"]["total_nodes"] == 10);
  assert($info["overall"]["live_nodes"] == 10);
  assert($info["overall"]["dead_nodes"] == 1);
  assert($info["overall"]["decommissioning_nodes"] == 0);
  assert($info["overall"]["dfs_blocks_underreplicated"] == 0);
  assert($info["overall"]["safemode"] == TRUE);
  assert($info["overall"]["pending_upgrades"] == "");
  assert($info["overall"]["dfs_configured_capacity"] == 36336891658240);
  assert($info["overall"]["dfs_percent_used"] == 0);
  assert($info["overall"]["dfs_percent_remaining"] == 99.08);
  assert($info["overall"]["dfs_total_bytes"] == 36336891658240);
  assert($info["overall"]["dfs_used_bytes"] == 1750237184);
  assert($info["overall"]["nondfs_used_bytes"] == 331691536384);
  assert($info["overall"]["dfs_free_bytes"] == 36003449884672);
  assert($info["overall"]["jobtracker_addr"] == "jobtracker:50030");
  assert($info["overall"]["jobtracker_starttime"] == 1327557546);
  assert($info["overall"]["running_jobs"] == 0);
  assert($info["overall"]["waiting_jobs"] == 3);
  assert($info["overall"]["trackers_total"] == 20);
  assert($info["overall"]["trackers_live"] == 10);
  assert($info["overall"]["trackers_graylisted"] == 0);
  assert($info["overall"]["trackers_blacklisted"] == 0);
  assert($info["overall"]["hbasemaster_addr"] == "hbasemaster:60010");
  assert($info["overall"]["total_regionservers"] == 30);
  assert($info["overall"]["hbasemaster_starttime"] == 1329244267);
  assert($info["overall"]["live_regionservers"] == 1);
  assert($info["overall"]["dead_regionservers"] == 0);
  assert($info["overall"]["regions_in_transition_count"] == 0);

  assert($info["hdfs"]["namenode_addr"] == "namenode:50070");
  assert($info["hdfs"]["secondary_namenode_addr"] == "snamenode:50071");
  assert($info["hdfs"]["namenode_starttime"] == 1327557522);
  assert($info["hdfs"]["total_nodes"] == 10);
  assert($info["hdfs"]["live_nodes"] == 10);
  assert($info["hdfs"]["dead_nodes"] == 1);
  assert($info["hdfs"]["decommissioning_nodes"] == 0);
  assert($info["hdfs"]["dfs_blocks_underreplicated"] == 0);
  assert($info["hdfs"]["safemode"] == TRUE);
  assert($info["hdfs"]["pending_upgrades"] == "");
  assert($info["hdfs"]["dfs_configured_capacity"] == 36336891658240);
  assert($info["hdfs"]["dfs_percent_used"] == 0);
  assert($info["hdfs"]["dfs_percent_remaining"] == 99.08);
  assert($info["hdfs"]["dfs_total_bytes"] == 36336891658240);
  assert($info["hdfs"]["dfs_used_bytes"] == 1750237184);
  assert($info["hdfs"]["nondfs_used_bytes"] == 331691536384);
  assert($info["hdfs"]["dfs_free_bytes"] == 36003449884672);

  assert($info["mapreduce"]["jobtracker_addr"] == "jobtracker:50030");
  assert($info["mapreduce"]["jobtracker_starttime"] == 1327557546);
  assert($info["mapreduce"]["running_jobs"] == 0);
  assert($info["mapreduce"]["waiting_jobs"] == 3);
  assert($info["mapreduce"]["trackers_total"] == 20);
  assert($info["mapreduce"]["trackers_live"] == 10);
  assert($info["mapreduce"]["trackers_graylisted"] == 0);
  assert($info["mapreduce"]["trackers_blacklisted"] == 0);

  assert($info["hbase"]["hbasemaster_addr"] == "hbasemaster:60010");
  assert($info["hbase"]["total_regionservers"] == 30);
  assert($info["hbase"]["hbasemaster_starttime"] == 1329244267);
  assert($info["hbase"]["live_regionservers"] == 1);
  assert($info["hbase"]["dead_regionservers"] == 0);
  assert($info["hbase"]["regions_in_transition_count"] == 0);
}

hdp_mon_load_cluster_configuration();
if (!isset($GLOBALS["HDP_MON_CONFIG"])) {
  error_log("global CONFIG is still not set");
  exit(1);
}

$hdfs_jmx_json = file_get_contents("./data/sample_namenode_jmx.json");
if (!$hdfs_jmx_json || $hdfs_jmx_json == "") {
  error_log("Invalid json data for namenode jmx");
  exit(1);
}

$hdfsinfo = hdp_mon_jmx_parse_hdfs_info(json_decode($hdfs_jmx_json, true));
verify_hdfs_info($hdfsinfo);

$mr_jmx_json = file_get_contents("./data/sample_jobtracker_jmx.json");
if (!$mr_jmx_json || $mr_jmx_json == "") {
  error_log("Invalid json data for jobtracker jmx");
  exit(1);
}

$mrinfo = hdp_mon_jmx_parse_mapreduce_info(json_decode($mr_jmx_json, true));
verify_mr_info($mrinfo);

$hbase_jmx_json = file_get_contents("./data/sample_hbasemaster_jmx.json");
if (!$hbase_jmx_json || $hbase_jmx_json == "") {
  error_log("Invalid json data for hbase master jmx");
  exit(1);
}

$hbaseinfo = hdp_mon_jmx_parse_hbase_info(json_decode($hbase_jmx_json, true));
verify_hbase_info($hbaseinfo);

$overallinfo = hdp_mon_helper_get_cluster_info($hdfsinfo,
    $mrinfo, $hbaseinfo);
verify_overall_info($overallinfo);

?>
