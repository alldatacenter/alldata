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
package org.apache.drill.exec.planner.sql.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.exec.store.SchemaFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDescribeSchema;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.exec.work.foreman.ForemanSetupException;

import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public class DescribeSchemaHandler extends DefaultSqlHandler {
  private static final Logger logger = LoggerFactory.getLogger(DescribeSchemaHandler.class);

  @SuppressWarnings("serial")
  private static final ObjectMapper mapper = new ObjectMapper(
      new ObjectMapper().getFactory().setCharacterEscapes(new CharacterEscapes() {
    @Override
    public int[] getEscapeCodesForAscii() {
      // add standard set of escaping characters
      int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
      // don't escape backslash (not to corrupt windows path)
      esc['\\'] = CharacterEscapes.ESCAPE_NONE;
      return esc;
    }

    @Override
    public SerializableString getEscapeSequence(int i) {
      // no further escaping (beyond ASCII chars) needed
      return null;
    }
  })).enable(INDENT_OUTPUT);


  public DescribeSchemaHandler(SqlHandlerConfig config) {
    super(config);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException {
    SqlIdentifier schema = unwrap(sqlNode, SqlDescribeSchema.class).getSchema();
    SchemaPlus schemaPlus = SchemaUtilites.findSchema(config.getConverter().getDefaultSchema(), schema.names);

    if (schemaPlus == null) {
      throw UserException.validationError()
        .message("Invalid schema name [%s]", Joiner.on(".").join(schema.names))
        .build(logger);
    }

    AbstractSchema drillSchema = SchemaUtilites.unwrapAsDrillSchemaInstance(schemaPlus);
    StoragePlugin storagePlugin;
    try {
      storagePlugin = context.getStorage().getPlugin(drillSchema.getSchemaPath().get(0));
      if (storagePlugin == null) {
        throw new DrillRuntimeException(String.format("Unable to find storage plugin with the following name [%s].",
          drillSchema.getSchemaPath().get(0)));
      }
    } catch (PluginException e) {
      throw new DrillRuntimeException("Failure while retrieving storage plugin", e);
    }

    try {
      Map configMap = mapper.convertValue(storagePlugin.getConfig(), Map.class);
      if (storagePlugin instanceof FileSystemPlugin) {
        transformWorkspaces(drillSchema.getSchemaPath(), configMap);
      }
      String properties = mapper.writeValueAsString(configMap);
      return DirectPlan.createDirectPlan(context, new DescribeSchemaResult(drillSchema.getFullSchemaName(), properties));
    } catch (JsonProcessingException e) {
      throw new DrillRuntimeException("Error while trying to convert storage config to json string", e);
    }
  }

  /**
   * If storage plugin has several workspaces, picks appropriate one and removes the others.
   */
  private void transformWorkspaces(List<String> names, Map configMap) {
    Object workspaces = configMap.remove("workspaces");
    if (workspaces != null) {
      Map map = (Map) workspaces;
      String key = names.size() > 1 ? names.get(1) : SchemaFactory.DEFAULT_WS_NAME;
      Object workspace = map.get(key);
      if (workspace != null) {
        Map workspaceMap = (Map) map.get(key);
        configMap.putAll(workspaceMap);
      } else if (SchemaFactory.DEFAULT_WS_NAME.equals(key)) {
        configMap.putAll(mapper.convertValue(WorkspaceConfig.DEFAULT, Map.class));
      }
    }
  }

  public static class DescribeSchemaResult {
    public String schema;
    public String properties;

    public DescribeSchemaResult(String schema, String properties) {
      this.schema = schema;
      this.properties = properties;
    }
  }
}
