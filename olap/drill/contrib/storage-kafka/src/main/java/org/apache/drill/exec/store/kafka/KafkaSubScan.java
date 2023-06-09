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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.store.StoragePluginRegistry;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

@JsonTypeName("kafka-partition-scan")
public class KafkaSubScan extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "KAFKA_SUB_SCAN";

  private final KafkaStoragePlugin kafkaStoragePlugin;
  private final List<SchemaPath> columns;
  private final List<KafkaPartitionScanSpec> partitionSubScanSpecList;

  @JsonCreator
  public KafkaSubScan(@JacksonInject StoragePluginRegistry registry,
                      @JsonProperty("userName") String userName,
                      @JsonProperty("kafkaStoragePluginConfig") KafkaStoragePluginConfig kafkaStoragePluginConfig,
                      @JsonProperty("columns") List<SchemaPath> columns,
                      @JsonProperty("partitionSubScanSpecList") LinkedList<KafkaPartitionScanSpec> partitionSubScanSpecList)
      throws ExecutionSetupException {
    this(userName,
        registry.resolve(kafkaStoragePluginConfig, KafkaStoragePlugin.class),
        columns,
        partitionSubScanSpecList);
  }

  public KafkaSubScan(String userName,
                      KafkaStoragePlugin kafkaStoragePlugin,
                      List<SchemaPath> columns,
                      List<KafkaPartitionScanSpec> partitionSubScanSpecList) {
    super(userName);
    this.kafkaStoragePlugin = kafkaStoragePlugin;
    this.columns = columns;
    this.partitionSubScanSpecList = partitionSubScanSpecList;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new KafkaSubScan(getUserName(), kafkaStoragePlugin, columns, partitionSubScanSpecList);
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @JsonProperty
  public KafkaStoragePluginConfig getKafkaStoragePluginConfig() {
    return kafkaStoragePlugin.getConfig();
  }

  @JsonProperty
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty
  public List<KafkaPartitionScanSpec> getPartitionSubScanSpecList() {
    return partitionSubScanSpecList;
  }

  @JsonIgnore
  public KafkaStoragePlugin getKafkaStoragePlugin() {
    return kafkaStoragePlugin;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
