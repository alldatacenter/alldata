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

import org.apache.calcite.adapter.cassandra.CassandraSchemaFactory;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.cassandra.CassandraStoragePlugin;

public class CassandraRootDrillSchemaFactory extends AbstractSchemaFactory {

  private final CassandraStoragePlugin plugin;
  private final SchemaFactory calciteSchemaFactory;

  public CassandraRootDrillSchemaFactory(String name, CassandraStoragePlugin plugin) {
    super(name);
    this.plugin = plugin;
    this.calciteSchemaFactory = new CassandraSchemaFactory();
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    Schema schema = new CassandraRootDrillSchema(getName(), plugin,
        calciteSchemaFactory, parent, getName(), plugin.getConfig().toConfigMap());
    parent.add(getName(), schema);
  }
}
