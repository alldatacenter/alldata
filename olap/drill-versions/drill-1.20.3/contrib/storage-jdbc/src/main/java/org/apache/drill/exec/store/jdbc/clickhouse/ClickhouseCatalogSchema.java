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
package org.apache.drill.exec.store.jdbc.clickhouse;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlDialect;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.jdbc.CapitalizingJdbcSchema;
import org.apache.drill.exec.store.jdbc.DrillJdbcConvention;
import org.apache.drill.exec.store.jdbc.JdbcStorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClickhouseCatalogSchema extends AbstractSchema {

  private static final Logger logger = LoggerFactory.getLogger(ClickhouseCatalogSchema.class);
  private final Map<String, CapitalizingJdbcSchema> schemaMap;
  private final CapitalizingJdbcSchema defaultSchema;

  public ClickhouseCatalogSchema(String name, DataSource source, SqlDialect dialect, DrillJdbcConvention convention) {
    super(Collections.emptyList(), name);
    this.schemaMap = new HashMap<>();
    String connectionSchemaName = null;
    try (Connection con = source.getConnection();
         ResultSet set = con.getMetaData().getSchemas()) {
      connectionSchemaName = con.getSchema();
      while (set.next()) {
        final String schemaName = set.getString(1);
        final String catalogName = set.getString(2);
        schemaMap.put(schemaName, new CapitalizingJdbcSchema(getSchemaPath(), schemaName, source, dialect,
          convention, catalogName, schemaName, false));
      }
    } catch (SQLException e) {
      logger.error("Failure while attempting to load clickhouse schema.", e);
    }
    defaultSchema = determineDefaultSchema(connectionSchemaName);
  }

  private CapitalizingJdbcSchema determineDefaultSchema(String connectionSchemaName) {
    CapitalizingJdbcSchema schema = schemaMap.get(connectionSchemaName);
    if (schema == null) {
      return schemaMap.values().iterator().next();
    } else {
      return schema;
    }
  }

  public void setHolder(SchemaPlus plusOfThis) {
    for (Map.Entry<String, CapitalizingJdbcSchema> entry : schemaMap.entrySet()) {
      plusOfThis.add(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public String getTypeName() {
    return JdbcStorageConfig.NAME;
  }

  @Override
  public Schema getDefaultSchema() {
    return defaultSchema;
  }

  @Override
  public Table getTable(String name) {
    if (defaultSchema != null) {
      try {
        return defaultSchema.getTable(name);
      } catch (RuntimeException e) {
        logger.warn("Failure while attempting to read table '{}' from {}.",
          name, this.getClass().getSimpleName(), e);
      }
    }
    return null;
  }

  @Override
  public Set<String> getTableNames() {
    return defaultSchema.getTableNames();
  }
}
