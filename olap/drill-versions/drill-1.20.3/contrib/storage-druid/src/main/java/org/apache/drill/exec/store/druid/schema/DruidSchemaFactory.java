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
package org.apache.drill.exec.store.druid.schema;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.druid.DruidScanSpec;
import org.apache.drill.exec.store.druid.DruidStoragePlugin;
import org.apache.drill.exec.store.druid.DruidStoragePluginConfig;
import org.apache.drill.exec.store.druid.druid.SimpleDatasourceInfo;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DruidSchemaFactory extends AbstractSchemaFactory {

  private static final Logger logger = LoggerFactory.getLogger(DruidSchemaFactory.class);
  private final DruidStoragePlugin plugin;

  public DruidSchemaFactory(DruidStoragePlugin plugin, String schemaName) {
    super(schemaName);
    this.plugin = plugin;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    DruidDataSources schema = new DruidDataSources(getName());
    SchemaPlus hPlus = parent.add(getName(), schema);
    schema.setHolder(hPlus);
  }

  public class DruidDataSources extends AbstractSchema {

    private final Set<String> tableNames;
    private final Map<String, DrillTable> drillTables = Maps.newHashMap();
    private Map<String, SimpleDatasourceInfo> druidDatasourceInfos = Maps.newHashMap();

    public DruidDataSources(String name) {
      super(ImmutableList.of(), name);
      this.tableNames = this.getTableNames();
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
    public Table getTable(String tableName) {

      if (!tableNames.contains(tableName)) {
        return null;
      }

      try {

        if (! drillTables.containsKey(tableName)) {
          SimpleDatasourceInfo simpleDatasourceInfo = druidDatasourceInfos.get(tableName);
          DruidScanSpec scanSpec =
            new DruidScanSpec(
              tableName,
              simpleDatasourceInfo.getProperties().getSegments().getSize(),
              simpleDatasourceInfo.getProperties().getSegments().getMinTime(),
              simpleDatasourceInfo.getProperties().getSegments().getMaxTime()
            );
          DynamicDrillTable dynamicDrillTable =
            new DynamicDrillTable(plugin, getName(), null, scanSpec);
          drillTables.put(tableName, dynamicDrillTable);
        }

        return drillTables.get(tableName);
      } catch (Exception e) {
        logger.warn("Failure while retrieving druid table {}", tableName, e);
        return null;
      }
    }

    @Override
    public Set<String> getTableNames() {
      try {
        List<SimpleDatasourceInfo> dataSources = plugin.getAdminClient().getDataSources();
        this.druidDatasourceInfos =
          dataSources.stream()
            .collect(Collectors.toMap(SimpleDatasourceInfo::getName, x -> x));
        Set<String> dataSourceNames = this.druidDatasourceInfos.keySet();
        logger.debug("Found Druid DataSources - {}", StringUtils.join(dataSourceNames, ","));
        return dataSourceNames;
      } catch (Exception e) {
        throw UserException.dataReadError(e)
            .message("Failure while loading druid datasources for database '%s'.", getName())
            .build(logger);
      }
    }

    @Override
    public String getTypeName() {
      return DruidStoragePluginConfig.NAME;
    }
  }
}
