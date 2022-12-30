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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.legacy.kafka.constants;

public class KafkaConstants {
  public static final String CONNECTOR_TYPE_VALUE_KAFKA = "kafka";
  public static final String CONNECTOR_TYPE_VALUE_KAFKA010X = "kafka010x";
  public static final String CONNECTOR_TYPE_VALUE_KAFKA220 = "kafka220";
  public static final String CONNECTOR_NAME_VALUE_KAFKA_SOURCE = "kafka_source";
  public static final String CONNECTOR_NAME_VALUE_KAFKA_SINK = "kafka-sink";
  public static final String CONNECTOR_VERSION_VALUE_08 = "0.8";
  public static final String CONNECTOR_VERSION_VALUE_09 = "0.9";
  public static final String CONNECTOR_VERSION_VALUE_010 = "0.10";
  public static final String CONNECTOR_VERSION_VALUE_011 = "0.11";
  public static final String CONNECTOR_VERSION_VALUE_UNIVERSAL = "universal";
  public static final String CONNECTOR_TOPIC = "connector.topic";
  public static final String CONNECTOR_CLUSTER = "connector.cluster";
  public static final String CONNECTOR_SERVERS = "connector.bootstrap.servers";
  public static final String CONNECTOR_TEAM = "connector.team";
  public static final String CONNECTOR_PSM = "connector.psm";
  public static final String CONNECTOR_OWNER = "connector.owner";
  public static final String CONNECTOR_GROUP_ID = "connector.group.id";
  public static final String CONNECTOR_SOURCE_INDEX = "connector.source.index";
  public static final String CONNECTOR_LOCAL_MODE = "connector.local-mode";
  public static final String CONNECTOR_STARTUP_MODE = "connector.startup-mode";
  public static final String CONNECTOR_STARTUP_MODE_VALUE_EARLIEST = "earliest-offset";
  public static final String CONNECTOR_STARTUP_MODE_VALUE_LATEST = "latest-offset";
  public static final String CONNECTOR_STARTUP_MODE_VALUE_GROUP_OFFSETS = "group-offsets";
  public static final String CONNECTOR_STARTUP_MODE_VALUE_SPECIFIC_OFFSETS = "specific-offsets";
  public static final String CONNECTOR_STARTUP_MODE_VALUE_SPECIFIC_TIMESTAMP = "specific-timestamp";
  public static final String CONNECTOR_SPECIFIC_OFFSETS = "connector.specific-offsets";
  public static final String CONNECTOR_SPECIFIC_OFFSETS_PARTITION = "partition";
  public static final String CONNECTOR_SPECIFIC_OFFSETS_OFFSET = "offset";
  public static final String CONNECTOR_SPECIFIC_TIMESTAMP = "connector.specific-timestamp";
  public static final String CONNECTOR_RELATIVE_OFFSET = "connector.relative-offset";
  public static final String CONNECTOR_RESET_TO_EARLIEST_FOR_NEW_PARTITION =
      "connector.reset-to-earliest-for-new-partition";
  public static final String CONNECTOR_KAFKA_PROPERTIES = "connector.kafka.properties";
  public static final String CONNECTOR_KAFKA_PROPERTIES_PARTITIONER_CLASS =
      "connector.kafka.properties.partitioner.class";
  public static final String CONNECTOR_PROPERTIES = "connector.properties";
  public static final String CONNECTOR_PROPERTIES_KEY = "key";
  public static final String CONNECTOR_PROPERTIES_VALUE = "value";
  public static final String CONNECTOR_SECURITY_PROTOCOL = "connector.security-protocol";
  public static final String CONNECTOR_SASL_MECHANISM = "connector.sasl_mechanism";
  public static final String CONNECTOR_SASL_PLAIN_USERNAME = "connector.sasl-plain-username";
  public static final String CONNECTOR_SASL_PLAIN_PASSWORD = "connector.sasl-plain-password";
  public static final String CONNECTOR_SSL_TRUSTSTORE_LOCATION = "connector.ssl_truststore_location";
  public static final String CONNECTOR_SSL_TRUSTSTORE_PASSWORD = "connector.ssl_truststore_password";
  public static final String CONNECTOR_SSL_IDENTIFICATION_ALGORITHM = "connector.ssl_endpoint_identification_algorithm";
  public static final String CONNECTOR_SINK_SEMANTIC = "connector.sink-semantic";
  public static final String CONNECTOR_SINK_PARTITIONER = "connector.sink-partitioner";
  public static final String CONNECTOR_SINK_PARTITIONER_VALUE_FIXED = "fixed";
  public static final String CONNECTOR_SINK_PARTITIONER_VALUE_ROUND_ROBIN = "round-robin";
  public static final String CONNECTOR_SINK_PARTITIONER_VALUE_ROW_FIELDS_HASH = "row-fields-hash";
  public static final String CONNECTOR_SINK_PARTITIONER_VALUE_CUSTOM = "custom";
  public static final String CONNECTOR_SINK_PARTITIONER_CLASS = "connector.sink-partitioner-class";
  public static final String CONNECTOR_SINK_IGNORE_TRANSACTION_TIMEOUT =
      "connector.sink-ignore-transaction-timeout-error";

  // Rate limiting configurations
  public static final String CONNECTOR_RATE_LIMITING_NUM = "connector.rate-limiting-num";
  public static final String CONNECTOR_RATE_LIMITING_UNIT = "connector.rate-limiting-unit";

  // Partition range to consume
  public static final String CONNECTOR_SOURCE_PARTITION_RANGE = "connector.source-partition-range";

  // Source sampling
  public static final String CONNECTOR_SOURCE_SAMPLE_INTERVAL = "connector.source-sample-interval";
  public static final String CONNECTOR_SOURCE_SAMPLE_NUM = "connector.source-sample-num";

  // Disable currentOffsetsRate metrics
  public static final String DISABLE_CURRENT_OFFSET_RATE_METRICS = "disableCurrentOffsetsRateMetrics";
  public static final int MAX_PARALLELISM = 5;
  public static final int REQUEST_TIMEOUT_MS_CONFIG = 1200 * 1000;
}
