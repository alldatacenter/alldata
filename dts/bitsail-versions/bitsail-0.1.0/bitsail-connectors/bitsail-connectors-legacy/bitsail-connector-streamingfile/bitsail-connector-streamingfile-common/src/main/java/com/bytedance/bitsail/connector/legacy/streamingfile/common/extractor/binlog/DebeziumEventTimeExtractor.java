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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.binlog;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor.AbstractEventTimeExtractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created 2021/1/5
 */
public class DebeziumEventTimeExtractor extends AbstractEventTimeExtractor {

  private final ObjectMapper mapper;

  public DebeziumEventTimeExtractor(BitSailConfiguration jobConf) {
    super(jobConf);
    mapper = new ObjectMapper();
  }

  @Override
  public JsonNode parse(byte[] record) throws Exception {
    return mapper.readTree(record);
  }

  /**
   * Extract the change time of the record in database.
   * Details of Debezium mysql message can be seen
   * <a href="https://debezium.io/documentation/reference/stable/connectors/mysql.html">Debezium Mysql</a>.
   *
   * @param record           A Debezium message.
   * @param defaultTimestamp Default timestamp if no 'ts_ms' found.
   * @return The change timestamp of the record in database.
   */
  @Override
  protected long extract(Object record, long defaultTimestamp) {
    JsonNode node = (JsonNode) record;
    return node.get("payload").get("source").get("ts_ms").asLong(defaultTimestamp);
  }
}