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
package org.apache.drill.exec.store.hive.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.schema.Table;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.hive.client.DrillHiveMetaStoreClient;
import org.apache.drill.exec.store.hive.HiveStoragePluginConfig;
import org.apache.drill.exec.store.hive.schema.HiveSchemaFactory.HiveSchema;
import org.apache.thrift.TException;

public class HiveDatabaseSchema extends AbstractSchema {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HiveDatabaseSchema.class);

  private final HiveSchema hiveSchema;

  private Map<String, TableType> tables;

  private final DrillHiveMetaStoreClient mClient;

  private final SchemaConfig schemaConfig;

  public HiveDatabaseSchema(
      HiveSchema hiveSchema,
      String name,
      DrillHiveMetaStoreClient mClient,
      SchemaConfig schemaConfig) {
    super(hiveSchema.getSchemaPath(), name);
    this.hiveSchema = hiveSchema;
    this.mClient = mClient;
    this.schemaConfig = schemaConfig;
  }

  @Override
  public Table getTable(String tableName) {
    return hiveSchema.getDrillTable(this.name, tableName);
  }

  @Override
  public Collection<Map.Entry<String, TableType>> getTableNamesAndTypes() {
    ensureInitTables();
    return tables.entrySet();
  }

  @Override
  public Set<String> getTableNames() {
    ensureInitTables();
    return tables.keySet();
  }

  private void ensureInitTables() {
    if (tables == null) {
      try {
        tables = mClient.getTableNamesAndTypes(this.name, schemaConfig.getIgnoreAuthErrors());
      } catch (TException e) {
        logger.warn(String.format(
            "Exception was thrown while getting table names and type for db '%s'.", this.name), e);
        tables = Collections.emptyMap();
      }
    }
  }

  @Override
  public String getTypeName() {
    return HiveStoragePluginConfig.NAME;
  }

  @Override
  public boolean areTableNamesCaseSensitive() {
    return false;
  }

}
