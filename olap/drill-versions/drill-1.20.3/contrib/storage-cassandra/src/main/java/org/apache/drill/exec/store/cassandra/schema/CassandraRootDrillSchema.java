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
package org.apache.drill.exec.store.cassandra.schema;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.cassandra.CassandraSchema;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.cassandra.CassandraStorageConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CassandraRootDrillSchema extends AbstractSchema {
  private final Map<String, Schema> schemas = new ConcurrentHashMap<>();

  private final StoragePlugin plugin;
  private final SchemaFactory schemaFactory;
  private final SchemaPlus parent;
  private final String parentName;
  private final Map<String, Object> configMap;

  public CassandraRootDrillSchema(String name, StoragePlugin plugin, SchemaFactory schemaFactory,
      SchemaPlus parent, String parentName, Map<String, Object> configMap) {
    super(Collections.emptyList(), name);
    this.plugin = plugin;
    this.schemaFactory = schemaFactory;
    this.parent = parent;
    this.parentName = parentName;
    this.configMap = configMap;
  }

  @Override
  public String getTypeName() {
    return CassandraStorageConfig.NAME;
  }

  @Override
  public Schema getSubSchema(String name) {
    return schemas.computeIfAbsent(name, this::createSubSchema);
  }

  private Schema createSubSchema(String schemaName) {
    Map<String, Object> configs = new HashMap<>(configMap);
    configs.put("keyspace", schemaName);

    SchemaPlus parentSchema = parent.getSubSchema(parentName);
    Schema schema = new CassandraDrillSchema(schemaName, plugin,
        (CassandraSchema) schemaFactory.create(parentSchema, schemaName, configs));
    parentSchema.add(schemaName, schema);
    return schema;
  }

  @Override
  public Expression getExpression(SchemaPlus parentSchema, String name) {
    return Expressions.call(
        DataContext.ROOT,
        BuiltInMethod.DATA_CONTEXT_GET_ROOT_SCHEMA.method);
  }
}
