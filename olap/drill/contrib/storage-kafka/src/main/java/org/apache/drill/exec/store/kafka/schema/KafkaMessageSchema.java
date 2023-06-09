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
package org.apache.drill.exec.store.kafka.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.kafka.KafkaScanSpec;
import org.apache.drill.exec.store.kafka.KafkaStoragePlugin;
import org.apache.drill.exec.store.kafka.KafkaStoragePluginConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaMessageSchema extends AbstractSchema {

  private static final Logger logger = LoggerFactory.getLogger(KafkaMessageSchema.class);
  private final KafkaStoragePlugin plugin;
  private final Map<String, DrillTable> drillTables = new HashMap<>();
  private Set<String> tableNames;

  public KafkaMessageSchema(final KafkaStoragePlugin plugin, final String name) {
    super(Collections.emptyList(), name);
    this.plugin = plugin;
  }

  @Override
  public String getTypeName() {
    return KafkaStoragePluginConfig.NAME;
  }

  void setHolder(SchemaPlus plusOfThis) {
    for (String s : getSubSchemaNames()) {
      plusOfThis.add(s, getSubSchema(s));
    }
  }

  @Override
  public Table getTable(String tableName) {
    if (!drillTables.containsKey(tableName)) {
      KafkaScanSpec scanSpec = new KafkaScanSpec(tableName);
      DrillTable table = new DynamicDrillTable(plugin, getName(), scanSpec);
      drillTables.put(tableName, table);
    }

    return drillTables.get(tableName);
  }

  @Override
  public Set<String> getTableNames() {
    if (tableNames == null) {
      KafkaConsumer<?, ?> kafkaConsumer = null;
      try {
        kafkaConsumer = new KafkaConsumer<>(plugin.getConfig().getKafkaConsumerProps());
        tableNames = kafkaConsumer.listTopics().keySet();
      } catch (Exception e) {
        logger.warn("Failure while loading table names for database '{}': {}", getName(), e.getMessage(), e.getCause());
        return Collections.emptySet();
      } finally {
        plugin.registerToClose(kafkaConsumer);
      }
    }
    return tableNames;
  }
}
