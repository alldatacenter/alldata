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

package com.netease.arctic.ams.server.config;

public class ConfigFileProperties {
  //system config
  public static final String SYSTEM_CONFIG = "ams";
  public static final String SYSTEM_EXTENSION_CONFIG = "extension_properties";
  public static final String SYSTEM_ARCTIC_HOME = "ARCTIC_HOME";

  //catalog config
  public static final String CATALOG_LIST = "catalogs";
  public static final String CATALOG_NAME = "name";
  //  public static final String CATALOG_DISPLAY_NAME = "display_name";
  public static final String CATALOG_TYPE = "type";

  public static final String CATALOG_STORAGE_CONFIG = "storage_config";
  public static final String CATALOG_STORAGE_TYPE = "storage.type";
  public static final String CATALOG_CORE_SITE = "core-site";
  public static final String CATALOG_HDFS_SITE = "hdfs-site";
  public static final String CATALOG_HIVE_SITE = "hive-site";

  public static final String CATALOG_AUTH_CONFIG = "auth_config";
  public static final String CATALOG_AUTH_TYPE = "type";
  public static final String CATALOG_PRINCIPAL = "principal";
  public static final String CATALOG_KEYTAB = "keytab";
  public static final String CATALOG_KRB5 = "krb5";
  public static final String CATALOG_SIMPLE_HADOOP_USERNAME = "hadoop_username";

  public static final String CATALOG_PROPERTIES = "properties";
  public static final String CATALOG_WAREHOUSE = "warehouse";

  //container config
  public static final String CONTAINER_LIST = "containers";
  public static final String CONTAINER_NAME = "name";
  public static final String CONTAINER_TYPE = "type";
  public static final String EXTERNAL_CONTAINER_TYPE = "external";
  public static final String CONTAINER_PROPERTIES = "properties";

  //optimize config
  public static final String OPTIMIZE_GROUP_LIST = "optimize_group";
  public static final String OPTIMIZE_GROUP_NAME = "name";
  public static final String OPTIMIZE_GROUP_CONTAINER = "container";
  public static final String OPTIMIZE_GROUP_PROPERTIES = "properties";
  public static final String OPTIMIZE_SCHEDULING_POLICY = "scheduling_policy";
  public static final String OPTIMIZE_SCHEDULING_POLICY_QUOTA = "quota";
  public static final String OPTIMIZE_SCHEDULING_POLICY_BALANCED = "balanced";

  public static final String OPTIMIZE_GROUP_PARALLELISM = "parallelism";
  public static final String OPTIMIZE_GROUP_MEMORY = "memory";
  public static final String OPTIMIZE_GROUP_TASKMANAGER_MEMORY = "taskmanager.memory";
  public static final String OPTIMIZE_GROUP_JOBMANAGER_MEMORY = "jobmanager.memory";
  public static final String OPTIMIZE_GROUP_FLINK_HOME = "flink_home";
  public static final String OPTIMIZE_GROUP_HADOOP_HOME = "hadoop_home";
  public static final String OPTIMIZE_GROUP_HADOOP_CONF_DIR = "hadoop_conf_dir";
}
