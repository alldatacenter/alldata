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
package org.apache.drill.exec.store.druid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@JsonTypeName(DruidStoragePluginConfig.NAME)
public class DruidStoragePluginConfig extends StoragePluginConfig {

  private static final Logger logger = LoggerFactory.getLogger(DruidStoragePluginConfig.class);
  public static final String NAME = "druid";
  private static final int DEFAULT_AVERAGE_ROW_SIZE_BYTES = 100;

  private final String brokerAddress;
  private final String coordinatorAddress;
  private final int averageRowSizeBytes;

  @JsonCreator
  public DruidStoragePluginConfig(
    @JsonProperty("brokerAddress") String brokerAddress,
    @JsonProperty("coordinatorAddress") String coordinatorAddress,
    @JsonProperty("averageRowSizeBytes") Integer averageRowSizeBytes) {
    this.brokerAddress = brokerAddress;
    this.coordinatorAddress = coordinatorAddress;
    this.averageRowSizeBytes =
        averageRowSizeBytes == null ? DEFAULT_AVERAGE_ROW_SIZE_BYTES : averageRowSizeBytes;
    logger.debug(
        "Broker Address - {}, Coordinator Address - {}, averageRowSizeBytes - {}",
        brokerAddress,
        coordinatorAddress,
        averageRowSizeBytes
    );
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    } else if (that == null || getClass() != that.getClass()) {
      return false;
    }
    DruidStoragePluginConfig thatConfig = (DruidStoragePluginConfig) that;
    return Objects.equals(brokerAddress, thatConfig.brokerAddress) &&
           Objects.equals(coordinatorAddress, thatConfig.coordinatorAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(brokerAddress, coordinatorAddress);
  }

  public String getBrokerAddress() {
    return brokerAddress;
  }

  public String getCoordinatorAddress() {
    return coordinatorAddress;
  }

  public int getAverageRowSizeBytes() { return averageRowSizeBytes; }
}
