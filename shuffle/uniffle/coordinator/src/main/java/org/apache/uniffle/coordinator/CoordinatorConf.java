/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator;

import java.util.List;
import java.util.Map;

import org.apache.uniffle.common.config.ConfigOption;
import org.apache.uniffle.common.config.ConfigOptions;
import org.apache.uniffle.common.config.ConfigUtils;
import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.util.RssUtils;
import org.apache.uniffle.coordinator.strategy.assignment.AbstractAssignmentStrategy;
import org.apache.uniffle.coordinator.strategy.assignment.AssignmentStrategyFactory;

import static org.apache.uniffle.coordinator.ApplicationManager.StrategyName.APP_BALANCE;
import static org.apache.uniffle.coordinator.strategy.assignment.AssignmentStrategyFactory.StrategyName.PARTITION_BALANCE;

/**
 * Configuration for Coordinator Service and rss-cluster, including service port,
 * heartbeat interval, etc.
 */
public class CoordinatorConf extends RssBaseConf {

  public static final ConfigOption<String> COORDINATOR_EXCLUDE_NODES_FILE_PATH = ConfigOptions
      .key("rss.coordinator.exclude.nodes.file.path")
      .stringType()
      .noDefaultValue()
      .withDescription("The path of configuration file which have exclude nodes");
  public static final ConfigOption<Long> COORDINATOR_EXCLUDE_NODES_CHECK_INTERVAL = ConfigOptions
      .key("rss.coordinator.exclude.nodes.check.interval.ms")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("Update interval for exclude nodes");
  public static final ConfigOption<Long> COORDINATOR_HEARTBEAT_TIMEOUT = ConfigOptions
      .key("rss.coordinator.server.heartbeat.timeout")
      .longType()
      .defaultValue(30 * 1000L)
      .withDescription("timeout if can't get heartbeat from shuffle server");
  public static final ConfigOption<Long> COORDINATOR_NODES_PERIODIC_OUTPUT_INTERVAL_TIMES = ConfigOptions
      .key("rss.coordinator.server.periodic.output.interval.times")
      .longType()
      .checkValue(ConfigUtils.POSITIVE_LONG_VALIDATOR, "output server list interval times must be positive")
      .defaultValue(30L)
      .withDescription("The periodic interval times of output alive nodes. The interval sec can be calculated by ("
          + COORDINATOR_HEARTBEAT_TIMEOUT.key() + "/3 * rss.coordinator.server.periodic.output.interval.times)");
  public static final ConfigOption<AssignmentStrategyFactory.StrategyName>
      COORDINATOR_ASSIGNMENT_STRATEGY = ConfigOptions
      .key("rss.coordinator.assignment.strategy")
      .enumType(AssignmentStrategyFactory.StrategyName.class)
      .defaultValue(PARTITION_BALANCE)
      .withDescription("Strategy for assigning shuffle server to write partitions");
  public static final ConfigOption<Long> COORDINATOR_APP_EXPIRED = ConfigOptions
      .key("rss.coordinator.app.expired")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("Application expired time (ms), the heartbeat interval must be less than it");
  public static final ConfigOption<Integer> COORDINATOR_SHUFFLE_NODES_MAX = ConfigOptions
      .key("rss.coordinator.shuffle.nodes.max")
      .intType()
      .defaultValue(9)
      .withDescription("The max limitation number of shuffle server when do the assignment");
  public static final ConfigOption<List<String>> COORDINATOR_ACCESS_CHECKERS = ConfigOptions
      .key("rss.coordinator.access.checkers")
      .stringType()
      .asList()
      .defaultValues("org.apache.uniffle.coordinator.access.checker.AccessClusterLoadChecker",
          "org.apache.uniffle.coordinator.access.checker.AccessQuotaChecker")
      .withDescription("Access checkers");
  public static final ConfigOption<Integer> COORDINATOR_ACCESS_CANDIDATES_UPDATE_INTERVAL_SEC = ConfigOptions
      .key("rss.coordinator.access.candidates.updateIntervalSec")
      .intType()
      .checkValue(ConfigUtils.POSITIVE_INTEGER_VALIDATOR_2, "access candidates update interval must be positive")
      .defaultValue(120)
      .withDescription("Accessed candidates update interval in seconds");
  public static final ConfigOption<String> COORDINATOR_ACCESS_CANDIDATES_PATH = ConfigOptions
      .key("rss.coordinator.access.candidates.path")
      .stringType()
      .noDefaultValue()
      .withDescription("Accessed candidates file path");
  public static final ConfigOption<Double> COORDINATOR_ACCESS_LOADCHECKER_MEMORY_PERCENTAGE = ConfigOptions
      .key("rss.coordinator.access.loadChecker.memory.percentage")
      .doubleType()
      .checkValue(ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR,
          "The recovery usage percentage must be between 0.0 and 100.0")
      .defaultValue(15.0)
      .withDescription("The minimal percentage of available memory percentage of a server");
  public static final ConfigOption<Integer> COORDINATOR_ACCESS_LOADCHECKER_SERVER_NUM_THRESHOLD = ConfigOptions
      .key("rss.coordinator.access.loadChecker.serverNum.threshold")
      .intType()
      .checkValue(ConfigUtils.POSITIVE_INTEGER_VALIDATOR_2, "load checker serverNum threshold must be positive")
      .noDefaultValue()
      .withDescription(
          "The minimal required number of healthy shuffle servers when being accessed by client. "
          + "And when not specified, it will use the required shuffle-server number from client as the checking "
          + "condition. If there is no client shuffle-server number specified, the coordinator conf "
          + "of rss.coordinator.shuffle.nodes.max will be adopted");
  public static final ConfigOption<Boolean> COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED = ConfigOptions
      .key("rss.coordinator.dynamicClientConf.enabled")
      .booleanType()
      .defaultValue(false)
      .withDescription("enable dynamic client conf");
  public static final ConfigOption<String> COORDINATOR_DYNAMIC_CLIENT_CONF_PATH = ConfigOptions
      .key("rss.coordinator.dynamicClientConf.path")
      .stringType()
      .noDefaultValue()
      .withDescription("dynamic client conf of this cluster");
  public static final ConfigOption<String> COORDINATOR_REMOTE_STORAGE_PATH = ConfigOptions
          .key("rss.coordinator.remote.storage.path")
          .stringType()
          .noDefaultValue()
          .withDescription("all supported remote paths for RSS cluster, separated by ','");
  public static final ConfigOption<Integer> COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC = ConfigOptions
      .key("rss.coordinator.dynamicClientConf.updateIntervalSec")
      .intType()
      .checkValue(ConfigUtils.POSITIVE_INTEGER_VALIDATOR_2, "dynamic client conf update interval in seconds")
      .defaultValue(120)
      .withDescription("The dynamic client conf update interval in seconds");
  public static final ConfigOption<String> COORDINATOR_REMOTE_STORAGE_CLUSTER_CONF = ConfigOptions
      .key("rss.coordinator.remote.storage.cluster.conf")
      .stringType()
      .noDefaultValue()
      .withDescription("Remote Storage Cluster related conf with format $clusterId,$key=$value, separated by ';'");
  public static final ConfigOption<ApplicationManager.StrategyName> COORDINATOR_REMOTE_STORAGE_SELECT_STRATEGY =
      ConfigOptions.key("rss.coordinator.remote.storage.select.strategy")
      .enumType(ApplicationManager.StrategyName.class)
      .defaultValue(APP_BALANCE)
      .withDescription("Strategy for selecting the remote path");
  public static final ConfigOption<Long> COORDINATOR_REMOTE_STORAGE_SCHEDULE_TIME = ConfigOptions
      .key("rss.coordinator.remote.storage.schedule.time")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("The time of scheduling the read and write time of the paths to obtain different HDFS");
  public static final ConfigOption<Integer> COORDINATOR_REMOTE_STORAGE_SCHEDULE_FILE_SIZE = ConfigOptions
      .key("rss.coordinator.remote.storage.schedule.file.size")
      .intType()
      .defaultValue(204800 * 1000)
      .withDescription("The size of the file that the scheduled thread reads and writes");
  public static final ConfigOption<Integer> COORDINATOR_REMOTE_STORAGE_SCHEDULE_ACCESS_TIMES = ConfigOptions
      .key("rss.coordinator.remote.storage.schedule.access.times")
      .intType()
      .defaultValue(3)
      .withDescription("The number of times to read and write HDFS files");
  public static final ConfigOption<AbstractAssignmentStrategy.HostAssignmentStrategyName>
      COORDINATOR_ASSIGNMENT_HOST_STRATEGY =
      ConfigOptions.key("rss.coordinator.assignment.host.strategy")
          .enumType(AbstractAssignmentStrategy.HostAssignmentStrategyName.class)
          .defaultValue(AbstractAssignmentStrategy.HostAssignmentStrategyName.PREFER_DIFF)
          .withDescription("Strategy for selecting shuffle servers");
  public static final ConfigOption<Boolean> COORDINATOR_START_SILENT_PERIOD_ENABLED = ConfigOptions
      .key("rss.coordinator.startup-silent-period.enabled")
      .booleanType()
      .defaultValue(false)
      .withDescription("Enable the startup-silent-period to reject the assignment requests "
          + "for avoiding partial assignments. To avoid service interruption, this mechanism is disabled by default. "
          + "Especially it's recommended to use in coordinator HA mode when restarting single coordinator.");
  public static final ConfigOption<Long> COORDINATOR_START_SILENT_PERIOD_DURATION = ConfigOptions
      .key("rss.coordinator.startup-silent-period.duration")
      .longType()
      .defaultValue(20 * 1000L)
      .withDescription("The waiting duration(ms) when conf of "
          + COORDINATOR_START_SILENT_PERIOD_ENABLED + " is enabled.");
  public static final ConfigOption<AbstractAssignmentStrategy.SelectPartitionStrategyName>
      COORDINATOR_SELECT_PARTITION_STRATEGY =
      ConfigOptions.key("rss.coordinator.select.partition.strategy")
          .enumType(AbstractAssignmentStrategy.SelectPartitionStrategyName.class)
          .defaultValue(AbstractAssignmentStrategy.SelectPartitionStrategyName.ROUND)
          .withDescription("Strategy for selecting partitions");
  public static final ConfigOption<Integer> COORDINATOR_QUOTA_DEFAULT_APP_NUM = ConfigOptions
      .key("rss.coordinator.quota.default.app.num")
      .intType()
      .defaultValue(5)
      .withDescription("Default number of apps at user level");
  public static final ConfigOption<String> COORDINATOR_QUOTA_DEFAULT_PATH = ConfigOptions
      .key("rss.coordinator.quota.default.path")
      .stringType()
      .noDefaultValue()
      .withDescription("A configuration file for the number of apps for a user-defined user");
  public static final ConfigOption<Long> COORDINATOR_QUOTA_UPDATE_INTERVAL = ConfigOptions
      .key("rss.coordinator.quota.update.interval")
      .longType()
      .defaultValue(60 * 1000L)
      .withDescription("Update interval for the default number of submitted apps per user");

  public CoordinatorConf() {
  }

  public CoordinatorConf(String fileName) {
    super();
    boolean ret = loadConfFromFile(fileName);
    if (!ret) {
      throw new IllegalStateException("Fail to load config file " + fileName);
    }
  }

  public boolean loadConfFromFile(String fileName) {
    Map<String, String> properties = RssUtils.getPropertiesFromFile(fileName);

    if (properties == null) {
      return false;
    }

    loadCommonConf(properties);

    List<ConfigOption<Object>> configOptions = ConfigUtils.getAllConfigOptions(CoordinatorConf.class);
    properties.forEach((k, v) -> {
      configOptions.forEach(config -> {
        if (config.key().equalsIgnoreCase(k)) {
          set(config, ConfigUtils.convertValue(v, config.getClazz()));
        }
      });
    });
    return true;
  }
}
