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

public interface TestQueryConstants {

  // Kafka Server Prop Constants
  String BROKER_DELIM = ",";
  String LOCAL_HOST = "127.0.0.1";

  // ZK
  String ZK_TMP = "zk_tmp";
  int TICK_TIME = 500;
  int MAX_CLIENT_CONNECTIONS = 100;

  String JSON_TOPIC = "drill-json-topic";
  String JSON_PUSHDOWN_TOPIC = "drill-pushdown-topic";
  String AVRO_TOPIC = "drill-avro-topic";
  String INVALID_TOPIC = "invalid-topic";

  String KAFKA_MSG_TIMESTAMP_FIELD = "kafkaMsgTimestamp";
  String KAFKA_PARTITION_ID_FIELD = "kafkaPartitionId";
  String KAFKA_MSG_OFFSET_FIELD = "kafkaMsgOffset";

  // Queries
  String MSG_COUNT_QUERY = "select count(*) from kafka.`%s`";
  String MSG_SELECT_QUERY = "select * from kafka.`%s`";
  String MIN_OFFSET_QUERY = "select MIN(kafkaMsgOffset) as minOffset from kafka.`%s`";
  String MAX_OFFSET_QUERY = "select MAX(kafkaMsgOffset) as maxOffset from kafka.`%s`";

  String QUERY_TEMPLATE_BASIC = "select * from kafka.`%s` where %s";
  String QUERY_TEMPLATE_AND = "select * from kafka.`%s` where %s AND %s";
  String QUERY_TEMPLATE_OR = "select * from kafka.`%s` where %s OR %s";
  String QUERY_TEMPLATE_AND_OR_PATTERN_1 = "select * from kafka.`%s` where %s AND (%s OR %s)";
  String QUERY_TEMPLATE_AND_OR_PATTERN_2 = "select * from kafka.`%s` where %s OR (%s AND %s)";
  String QUERY_TEMPLATE_AND_OR_PATTERN_3 = "select * from kafka.`%s` where (%s OR %s) AND (%s OR %s)";
}
