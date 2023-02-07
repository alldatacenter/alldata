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
package org.apache.drill.exec.record.metadata.schema;

import org.apache.drill.exec.planner.sql.handlers.SchemaHandler;
import org.apache.drill.exec.planner.sql.parser.SqlSchema;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Factory class responsible for creating different instances of schema provider based on given parameters.
 */
public class SchemaProviderFactory {

  /**
   * Creates schema provider for sql schema commands.
   *
   * @param sqlSchema sql schema call
   * @param schemaHandler schema handler
   * @return schema provider instance
   * @throws IOException if unable to init schema provider
   */
  public static SchemaProvider create(SqlSchema sqlSchema, SchemaHandler schemaHandler) throws IOException {
    if (sqlSchema.hasTable()) {
      String tableName = sqlSchema.getTableName();
      WorkspaceSchemaFactory.WorkspaceSchema wsSchema = schemaHandler.getWorkspaceSchema(sqlSchema.getSchemaPath(), tableName);
      return new FsMetastoreSchemaProvider(wsSchema, tableName);
    } else {
      return new PathSchemaProvider(new Path(sqlSchema.getPath()));
    }
  }

  /**
   * Creates schema provider based table function schema parameter.
   *
   * @param parameterValue schema parameter value
   * @return schema provider instance
   * @throws IOException if unable to init schema provider
   */
  public static SchemaProvider create(String parameterValue) throws IOException {
    String[] split = parameterValue.split("=", 2);
    if (split.length < 2) {
      throw new IOException("Incorrect parameter value format: " + parameterValue);
    }
    ProviderType providerType = ProviderType.valueOf(split[0].trim().toUpperCase());
    String value = split[1].trim();
    switch (providerType) {
      case INLINE:
        return new InlineSchemaProvider(value);
      case PATH:
        char c = value.charAt(0);
        // if path starts with any type of quotes, strip them
        if (c == '\'' || c == '"' || c == '`') {
          value = value.substring(1, value.length() - 1);
        }
        return new PathSchemaProvider(new Path(value));
      default:
        throw new IOException("Unexpected provider type: " + providerType);
    }
  }

  /**
   * Indicates provider type will be used to provide schema.
   */
  private enum ProviderType {
    INLINE,
    PATH
  }

}
