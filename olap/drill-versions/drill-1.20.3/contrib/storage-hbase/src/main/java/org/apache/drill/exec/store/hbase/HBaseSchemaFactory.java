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
package org.apache.drill.exec.store.hbase;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class HBaseSchemaFactory extends AbstractSchemaFactory {
  private static final Logger logger = LoggerFactory.getLogger(HBaseSchemaFactory.class);

  private final HBaseStoragePlugin plugin;

  public HBaseSchemaFactory(HBaseStoragePlugin plugin, String name) throws IOException {
    super(name);
    this.plugin = plugin;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    HBaseSchema schema = new HBaseSchema(getName());
    SchemaPlus hPlus = parent.add(getName(), schema);
    schema.setHolder(hPlus);
  }

  class HBaseSchema extends AbstractSchema {

    HBaseSchema(String name) {
      super(Collections.emptyList(), name);
    }

    public void setHolder(SchemaPlus plusOfThis) {
    }

    @Override
    public AbstractSchema getSubSchema(String name) {
      return null;
    }

    @Override
    public Set<String> getSubSchemaNames() {
      return Collections.emptySet();
    }

    @Override
    public Table getTable(String name) {
      HBaseScanSpec scanSpec = new HBaseScanSpec(name);
      try {
        return new DrillHBaseTable(getName(), plugin, scanSpec);
      } catch (Exception e) {
        // Calcite firstly looks for a table in the default schema, if the table was not found,
        // it looks in the root schema.
        // If the table does not exist, a query will fail at validation stage,
        // so the error should not be thrown here.
        logger.warn("Failure while loading table '{}' for database '{}'.", name, getName(), e.getCause());
        return null;
      }
    }

    @Override
    public Set<String> getTableNames() {
      try(Admin admin = plugin.getConnection().getAdmin()) {
        HTableDescriptor[] tables = admin.listTables();
        Set<String> tableNames = Sets.newHashSet();
        for (HTableDescriptor table : tables) {
          tableNames.add(new String(table.getTableName().getNameAsString()));
        }
        return tableNames;
      } catch (Exception e) {
        logger.warn("Failure while loading table names for database '{}'.", getName(), e.getCause());
        return Collections.emptySet();
      }
    }

    @Override
    public String getTypeName() {
      return HBaseStoragePluginConfig.NAME;
    }
  }
}
