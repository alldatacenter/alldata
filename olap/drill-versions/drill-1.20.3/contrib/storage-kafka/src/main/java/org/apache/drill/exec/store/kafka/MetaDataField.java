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

import org.apache.drill.common.types.TypeProtos;

/**
 * MetaData fields provide additional information about each message.
 * It is expected that one should not modify the fieldName of each constant as it breaks the compatibility.
 */
public enum MetaDataField {

  KAFKA_TOPIC("kafkaTopic", TypeProtos.MinorType.VARCHAR),
  KAFKA_PARTITION_ID("kafkaPartitionId", TypeProtos.MinorType.BIGINT),
  KAFKA_OFFSET("kafkaMsgOffset", TypeProtos.MinorType.BIGINT),
  KAFKA_TIMESTAMP("kafkaMsgTimestamp", TypeProtos.MinorType.BIGINT),
  KAFKA_MSG_KEY("kafkaMsgKey", TypeProtos.MinorType.VARCHAR);

  private final String fieldName;
  private final TypeProtos.MinorType fieldType;

  MetaDataField(final String fieldName, TypeProtos.MinorType type) {
    this.fieldName = fieldName;
    this.fieldType = type;
  }

  public String getFieldName() {
    return fieldName;
  }

  public TypeProtos.MinorType getFieldType() {
    return fieldType;
  }
}
