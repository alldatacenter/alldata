/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hbase;

import java.util.Map;

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

@JsonTypeName(HBaseStoragePluginConfig.NAME)
public class HBaseStoragePluginConfig extends StoragePluginConfig implements DrillHBaseConstants {
  private static final Logger logger = LoggerFactory.getLogger(HBaseStoragePluginConfig.class);

  private Map<String, String> config;

  @JsonIgnore
  private Configuration hbaseConf;

  @JsonIgnore
  private Boolean sizeCalculatorEnabled;

  public static final String NAME = "hbase";

  @JsonCreator
  public HBaseStoragePluginConfig(@JsonProperty("config") Map<String, String> props, @JsonProperty("size.calculator.enabled") Boolean sizeCalculatorEnabled) {
    this.config = props;
    if (config == null) {
      config = Maps.newHashMap();
    }
    // TODO: Config-based information should reside in the
    // storage plugin instance, not here.
    logger.debug("Initializing HBase StoragePlugin configuration with zookeeper quorum '{}', port '{}'.",
        config.get(HConstants.ZOOKEEPER_QUORUM), config.get(HBASE_ZOOKEEPER_PORT));
    if (sizeCalculatorEnabled == null) {
      this.sizeCalculatorEnabled = false;
    } else {
      this.sizeCalculatorEnabled = sizeCalculatorEnabled;
    }
  }

  @JsonProperty
  public Map<String, String> getConfig() {
    return ImmutableMap.copyOf(config);
  }

  @JsonProperty("size.calculator.enabled")
  public boolean isSizeCalculatorEnabled() {
    return sizeCalculatorEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HBaseStoragePluginConfig that = (HBaseStoragePluginConfig) o;
    return config.equals(that.config);
  }

  @Override
  public int hashCode() {
    return this.config != null ? this.config.hashCode() : 0;
  }

  @JsonIgnore
  public Configuration getHBaseConf() {
    if (hbaseConf == null) {
      hbaseConf = HBaseConfiguration.create();
      if (config != null) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
          hbaseConf.set(entry.getKey(), entry.getValue());
        }
      }
    }
    return hbaseConf;
  }

  @JsonIgnore
  public String getZookeeperQuorum() {
    return getHBaseConf().get(HConstants.ZOOKEEPER_QUORUM);
  }

  @JsonIgnore
  public String getZookeeperPort() {
    return getHBaseConf().get(HBASE_ZOOKEEPER_PORT);
  }

  @JsonIgnore
  @VisibleForTesting
  public void setZookeeperPort(int zookeeperPort) {
    this.config.put(HBASE_ZOOKEEPER_PORT, String.valueOf(zookeeperPort));
    getHBaseConf().setInt(HBASE_ZOOKEEPER_PORT, zookeeperPort);
  }

}
