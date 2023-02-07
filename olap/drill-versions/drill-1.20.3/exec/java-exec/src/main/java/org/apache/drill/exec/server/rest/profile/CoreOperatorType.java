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
package org.apache.drill.exec.server.rest.profile;


import java.util.Arrays;

/**
 * This class is used for backward compatibility when reading older query profiles that
 * stored operator id instead of its name.
 * <b>Please do not update this class. It will be removed for Drill 2.0</b>
 */
public enum CoreOperatorType {

  /**
   * <code>SINGLE_SENDER = 0;</code>
   */
  SINGLE_SENDER(0),
  /**
   * <code>BROADCAST_SENDER = 1;</code>
   */
  BROADCAST_SENDER(1),
  /**
   * <code>FILTER = 2;</code>
   */
  FILTER(2),
  /**
   * <code>HASH_AGGREGATE = 3;</code>
   */
  HASH_AGGREGATE(3),
  /**
   * <code>HASH_JOIN = 4;</code>
   */
  HASH_JOIN(4),
  /**
   * <code>MERGE_JOIN = 5;</code>
   */
  MERGE_JOIN(5),
  /**
   * <code>HASH_PARTITION_SENDER = 6;</code>
   */
  HASH_PARTITION_SENDER(6),
  /**
   * <code>LIMIT = 7;</code>
   */
  LIMIT(7),
  /**
   * <code>MERGING_RECEIVER = 8;</code>
   */
  MERGING_RECEIVER(8),
  /**
   * <code>ORDERED_PARTITION_SENDER = 9;</code>
   */
  ORDERED_PARTITION_SENDER(9),
  /**
   * <code>PROJECT = 10;</code>
   */
  PROJECT(10),
  /**
   * <code>UNORDERED_RECEIVER = 11;</code>
   */
  UNORDERED_RECEIVER(11),
  /**
   * <code>RANGE_PARTITION_SENDER = 12;</code>
   */
  RANGE_PARTITION_SENDER(12),
  /**
   * <code>SCREEN = 13;</code>
   */
  SCREEN(13),
  /**
   * <code>SELECTION_VECTOR_REMOVER = 14;</code>
   */
  SELECTION_VECTOR_REMOVER(14),
  /**
   * <code>STREAMING_AGGREGATE = 15;</code>
   */
  STREAMING_AGGREGATE(15),
  /**
   * <code>TOP_N_SORT = 16;</code>
   */
  TOP_N_SORT(16),
  /**
   * <code>EXTERNAL_SORT = 17;</code>
   */
  EXTERNAL_SORT(17),
  /**
   * <code>TRACE = 18;</code>
   */
  TRACE(18),
  /**
   * <code>UNION = 19;</code>
   */
  UNION(19),
  /**
   * <code>OLD_SORT = 20;</code>
   */
  OLD_SORT(20),
  /**
   * <code>PARQUET_ROW_GROUP_SCAN = 21;</code>
   */
  PARQUET_ROW_GROUP_SCAN(21),
  /**
   * <code>HIVE_SUB_SCAN = 22;</code>
   */
  HIVE_SUB_SCAN(22),
  /**
   * <code>SYSTEM_TABLE_SCAN = 23;</code>
   */
  SYSTEM_TABLE_SCAN(23),
  /**
   * <code>MOCK_SUB_SCAN = 24;</code>
   */
  MOCK_SUB_SCAN(24),
  /**
   * <code>PARQUET_WRITER = 25;</code>
   */
  PARQUET_WRITER(25),
  /**
   * <code>DIRECT_SUB_SCAN = 26;</code>
   */
  DIRECT_SUB_SCAN(26),
  /**
   * <code>TEXT_WRITER = 27;</code>
   */
  TEXT_WRITER(27),
  /**
   * <code>TEXT_SUB_SCAN = 28;</code>
   */
  TEXT_SUB_SCAN(28),
  /**
   * <code>JSON_SUB_SCAN = 29;</code>
   */
  JSON_SUB_SCAN(29),
  /**
   * <code>INFO_SCHEMA_SUB_SCAN = 30;</code>
   */
  INFO_SCHEMA_SUB_SCAN(30),
  /**
   * <code>COMPLEX_TO_JSON = 31;</code>
   */
  COMPLEX_TO_JSON(31),
  /**
   * <code>PRODUCER_CONSUMER = 32;</code>
   */
  PRODUCER_CONSUMER(32),
  /**
   * <code>HBASE_SUB_SCAN = 33;</code>
   */
  HBASE_SUB_SCAN(33),
  /**
   * <code>WINDOW = 34;</code>
   */
  WINDOW(34),
  /**
   * <code>NESTED_LOOP_JOIN = 35;</code>
   */
  NESTED_LOOP_JOIN(35),
  /**
   * <code>AVRO_SUB_SCAN = 36;</code>
   */
  AVRO_SUB_SCAN(36),
  /**
   * <code>PCAP_SUB_SCAN = 37;</code>
   */
  PCAP_SUB_SCAN(37),
  /**
   * <code>KAFKA_SUB_SCAN = 38;</code>
   */
  KAFKA_SUB_SCAN(38),
  /**
   * <code>KUDU_SUB_SCAN = 39;</code>
   */
  KUDU_SUB_SCAN(39),
  /**
   * <code>FLATTEN = 40;</code>
   */
  FLATTEN(40),
  /**
   * <code>LATERAL_JOIN = 41;</code>
   */
  LATERAL_JOIN(41),
  /**
   * <code>UNNEST = 42;</code>
   */
  UNNEST(42),
  /**
   * <code>HIVE_DRILL_NATIVE_PARQUET_ROW_GROUP_SCAN = 43;</code>
   */
  HIVE_DRILL_NATIVE_PARQUET_ROW_GROUP_SCAN(43),
  /**
   * <code>JDBC_SCAN = 44;</code>
   */
  JDBC_SCAN(44),
  /**
   * <code>REGEX_SUB_SCAN = 45;</code>
   */
  REGEX_SUB_SCAN(45),
  /**
   * <code>MAPRDB_SUB_SCAN = 46;</code>
   */
  MAPRDB_SUB_SCAN(46),
  /**
   * <code>MONGO_SUB_SCAN = 47;</code>
   */
  MONGO_SUB_SCAN(47),
  /**
   * <code>KUDU_WRITER = 48;</code>
   */
  KUDU_WRITER(48),
  /**
   * <code>OPEN_TSDB_SUB_SCAN = 49;</code>
   */
  OPEN_TSDB_SUB_SCAN(49),
  /**
   * <code>JSON_WRITER = 50;</code>
   */
  JSON_WRITER(50),
  /**
   * <code>HTPPD_LOG_SUB_SCAN = 51;</code>
   */
  HTPPD_LOG_SUB_SCAN(51),
  /**
   * <code>IMAGE_SUB_SCAN = 52;</code>
   */
  IMAGE_SUB_SCAN(52),
  /**
   * <code>SEQUENCE_SUB_SCAN = 53;</code>
   */
  SEQUENCE_SUB_SCAN(53),
  /**
   * <code>PARTITION_LIMIT = 54;</code>
   */
  PARTITION_LIMIT(54),
  /**
   * <code>PCAPNG_SUB_SCAN = 55;</code>
   */
  PCAPNG_SUB_SCAN(55),
  /**
   * <code>RUNTIME_FILTER = 56;</code>
   */
  RUNTIME_FILTER(56),
  /**
   * <code>ROWKEY_JOIN = 57;</code>
   */
  ROWKEY_JOIN(57),
  /**
   * <code>SYSLOG_SUB_SCAN = 58;</code>
   */
  SYSLOG_SUB_SCAN(58),
  /**
   * <code>STATISTICS_AGGREGATE = 59;</code>
   */
  STATISTICS_AGGREGATE(59),
  /**
   * <code>UNPIVOT_MAPS = 60;</code>
   */
  UNPIVOT_MAPS(60),
  /**
   * <code>STATISTICS_MERGE = 61;</code>
   */
  STATISTICS_MERGE(61),
  /**
   * <code>LTSV_SUB_SCAN = 62;</code>
   */
  LTSV_SUB_SCAN(62),
  /**
   * <code>HDF5_SUB_SCAN = 63;</code>
   */
  HDF5_SUB_SCAN(63),
  /**
   * <code>EXCEL_SUB_SCAN = 64;</code>
   */
  EXCEL_SUB_SCAN(64),
  /**
   * <code>SHP_SUB_SCAN = 65;</code>
   */
  SHP_SUB_SCAN(65),
  /**
   * <code>METADATA_HANDLER = 66;</code>
   */
  METADATA_HANDLER(66),
  /**
   * <code>METADATA_CONTROLLER = 67;</code>
   */
  METADATA_CONTROLLER(67),
  /**
   * <code>DRUID_SUB_SCAN = 68;</code>
   */
  DRUID_SUB_SCAN(68),
  /**
   * <code>SPSS_SUB_SCAN = 69;</code>
   */
  SPSS_SUB_SCAN(69),
  /**
   * <code>HTTP_SUB_SCAN = 70;</code>
   */
  HTTP_SUB_SCAN(70),
  /**
   * <code>XML_SUB_SCAN = 71;</code>
   */
  XML_SUB_SCAN(71);

  private final int value;

  CoreOperatorType(int value) {
    this.value = value;
  }

  public int getId() {
    return value;
  }

  public static CoreOperatorType valueOf(int id) {
    if (id >= 0 && id <= XML_SUB_SCAN.getId()) {
      return values()[id];
    }
    return null;
  }

  public static CoreOperatorType forName(String name) {
    return Arrays.stream(values())
        .filter(value -> value.name().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }
}
