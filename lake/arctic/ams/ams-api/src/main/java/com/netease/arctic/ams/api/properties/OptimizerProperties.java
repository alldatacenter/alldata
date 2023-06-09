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

package com.netease.arctic.ams.api.properties;

public class OptimizerProperties {
  //container properties
  public static final String CONTAINER_INFO = "containerInfo";
  public static final String CONTAINER_PROPERTIES = "properties";

  //optimizer group properties
  public static final String OPTIMIZER_GROUP_INFO = "groupInfo";
  public static final String OPTIMIZER_GROUP_PROPERTIES = "properties";
  public static final String OPTIMIZER_GROUP_HEART_BEAT_INTERVAL = "heartBeatInterval";
  public static final Long OPTIMIZER_GROUP_HEART_BEAT_INTERVAL_DEFAULT = 60000L;

  //ams system properties
  public static final String AMS_SYSTEM_INFO = "systemInfo";
  public static final String ARCTIC_HOME = "ARCTIC_HOME";
  public static final String THRIFT_BIND_HOST = "arctic.ams.server-host";
  public static final String THRIFT_BIND_PORT = "arctic.ams.thrift.port";
  public static final String HA_ENABLE = "arctic.ams.ha.enabled";
  public static final String CLUSTER_NAME = "arctic.ams.cluster.name";
  public static final String ZOOKEEPER_SERVER = "arctic.ams.zookeeper.server";

  //optimizer launcher properties
  public static final String OPTIMIZER_LAUNCHER_INFO = "launcherInfo";

  //optimizer job properties
  public static final String OPTIMIZER_JOB_INFO = "jobInfo";
  public static final String OPTIMIZER_JOB_PROPERTIES = "properties";
  public static final String OPTIMIZER_JOB_ID = "jobId";
  public static final String OPTIMIZER_JOB_PARALLELISM = "parallelism";

  // optimizer spill map properties
  public static final String SPILLABLE_MAP_ENABLE = "spillable.map.enabled";
  public static final String SPILLABLE_MAP_DIR = "spillable.map.dir";
  public static final String SPILLABLE_MEMORY_LIMIT = "spillable.memory.limit";
}
