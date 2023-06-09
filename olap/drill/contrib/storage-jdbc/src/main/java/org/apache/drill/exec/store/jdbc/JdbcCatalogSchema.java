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
package org.apache.drill.exec.store.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlDialect;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JdbcCatalogSchema extends AbstractSchema {

  private static final Logger logger = LoggerFactory.getLogger(JdbcCatalogSchema.class);

  /**
   * Maps name in lowercase to its catalog or schema instance
   */
  private final Map<String, CapitalizingJdbcSchema> schemaMap;
  private final CapitalizingJdbcSchema defaultSchema;

  JdbcCatalogSchema(String name, DataSource source, SqlDialect dialect, DrillJdbcConvention convention, boolean caseSensitive) {
    super(Collections.emptyList(), name);
    this.schemaMap = new HashMap<>();
    String connectionSchemaName = null;
    try (Connection con = source.getConnection();
         ResultSet set = con.getMetaData().getCatalogs()) {

      try {
        connectionSchemaName = con.getSchema();
      } catch (AbstractMethodError ex) {
        // DRILL-8227. Some Sybase JDBC drivers still don't implement this method, e.g. JConnect, jTDS.
        logger.warn(
          "{} does not provide an implementation of getSchema(), default schema will be guessed",
          con.getClass()
        );
      }

      while (set.next()) {
        final String catalogName = set.getString(1);
        if (catalogName == null) {
          // DRILL-8219. DB2 is an example of why of this escape is needed.
          continue;
        }

        CapitalizingJdbcSchema schema = new CapitalizingJdbcSchema(
            getSchemaPath(), catalogName, source, dialect, convention, catalogName, null, caseSensitive);
        schemaMap.put(schema.getName(), schema);
      }
    } catch (SQLException e) {
      logger.warn("Failure while attempting to load JDBC schema.", e);
    }

    // unable to read catalog list.
    if (schemaMap.isEmpty()) {

      // try to add a list of schemas to the schema map.
      boolean schemasAdded = addSchemas(source, dialect, convention, caseSensitive);

      if (!schemasAdded) {
        // there were no schemas, just create a default one (the jdbc system doesn't support catalogs/schemas).
        schemaMap.put(SchemaFactory.DEFAULT_WS_NAME, new CapitalizingJdbcSchema(Collections.emptyList(), name, source, dialect,
          convention, null, null, caseSensitive));
      }
    } else {
      // We already have catalogs. Add schemas in this context of their catalogs.
      addSchemas(source, dialect, convention, caseSensitive);
    }

    defaultSchema = determineDefaultSchema(connectionSchemaName);
  }

  private CapitalizingJdbcSchema determineDefaultSchema(String connectionSchemaName) {
    CapitalizingJdbcSchema connSchema;
    if (connectionSchemaName == null ||
        (connSchema = schemaMap.get(connectionSchemaName.toLowerCase())) == null) {
      connSchema = schemaMap.values().iterator().next();
    }
    return connSchema.getDefaultSchema();
  }

  void setHolder(SchemaPlus plusOfThis) {
    for (String s : getSubSchemaNames()) {
      CapitalizingJdbcSchema inner = getSubSchema(s);
      SchemaPlus holder = plusOfThis.add(s, inner);
      inner.setHolder(holder);
    }
  }

  private boolean addSchemas(DataSource source, SqlDialect dialect, DrillJdbcConvention convention, boolean caseSensitive) {
    boolean added = false;
    try (Connection con = source.getConnection();
         ResultSet set = con.getMetaData().getSchemas()) {
      while (set.next()) {
        final String schemaName = set.getString(1);
        final String catalogName = set.getString(2);

        String parentKey = StringUtils.lowerCase(catalogName);
        CapitalizingJdbcSchema parentSchema = schemaMap.get(parentKey);
        if (parentSchema == null) {
          CapitalizingJdbcSchema schema = new CapitalizingJdbcSchema(getSchemaPath(), schemaName, source, dialect,
              convention, catalogName, schemaName, caseSensitive);

          // if a catalog schema doesn't exist, we'll add this at the top level.
          schemaMap.put(schema.getName(), schema);
        } else {
          CapitalizingJdbcSchema schema = new CapitalizingJdbcSchema(parentSchema.getSchemaPath(), schemaName,
              source, dialect,
              convention, catalogName, schemaName, caseSensitive);
          parentSchema.addSubSchema(schema);
        }
        added = true;
      }
    } catch (SQLException e) {
      logger.warn("Failure while attempting to load JDBC schema.", e);
    }

    return added;
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
  public CapitalizingJdbcSchema getSubSchema(String name) {
    return schemaMap.get(name);
  }

  @Override
  public Set<String> getSubSchemaNames() {
    return schemaMap.keySet();
  }

  @Override
  public Table getTable(String name) {
    if (defaultSchema != null) {
      try {
        return defaultSchema.getTable(name);
      } catch (RuntimeException e) {
        logger.warn("Failure while attempting to read table '{}' from JDBC source.", name, e);
      }
    }

    // no table was found.
    return null;
  }

  @Override
  public Set<String> getTableNames() {
    return defaultSchema.getTableNames();
  }

  @Override
  public boolean areTableNamesCaseSensitive() {
    return defaultSchema.areTableNamesCaseSensitive();
  }
}
