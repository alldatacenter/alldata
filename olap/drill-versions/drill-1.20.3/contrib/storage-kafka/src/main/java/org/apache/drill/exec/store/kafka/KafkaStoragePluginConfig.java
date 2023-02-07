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
package org.apache.drill.exec.store.kafka;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.StringJoiner;

import org.apache.drill.common.logical.StoragePluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(KafkaStoragePluginConfig.NAME)
public class KafkaStoragePluginConfig extends StoragePluginConfig {

  private static final Logger logger = LoggerFactory.getLogger(KafkaStoragePluginConfig.class);
  public static final String NAME = "kafka";

  private final Properties kafkaConsumerProps;

  @JsonCreator
  public KafkaStoragePluginConfig(@JsonProperty("kafkaConsumerProps") Map<String, String> kafkaConsumerProps) {
    this.kafkaConsumerProps = new Properties();
    this.kafkaConsumerProps.putAll(kafkaConsumerProps);
    logger.debug("Kafka Consumer Props {}", this.kafkaConsumerProps);
  }

  public Properties getKafkaConsumerProps() {
    return kafkaConsumerProps;
  }

  @Override
  public int hashCode() {
    return Objects.hash(kafkaConsumerProps);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KafkaStoragePluginConfig that = (KafkaStoragePluginConfig) o;
    return kafkaConsumerProps.equals(that.kafkaConsumerProps);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", KafkaStoragePluginConfig.class.getSimpleName() + "[", "]")
      .add("kafkaConsumerProps=" + kafkaConsumerProps)
      .toString();
  }
}
