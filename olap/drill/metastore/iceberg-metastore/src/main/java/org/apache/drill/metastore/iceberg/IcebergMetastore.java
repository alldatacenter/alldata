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
package org.apache.drill.metastore.iceberg;

import com.typesafe.config.Config;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.Metastore;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.components.views.Views;
import org.apache.drill.metastore.iceberg.components.tables.IcebergTables;
import org.apache.drill.metastore.iceberg.config.IcebergConfigConstants;
import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.drill.metastore.iceberg.schema.IcebergTableSchema;
import org.apache.drill.shaded.guava.com.google.common.collect.MapDifference;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.Table;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.hadoop.HadoopTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Iceberg Drill Metastore implementation that inits / loads Iceberg tables
 * which correspond to Metastore components: tables, views, etc.
 */
public class IcebergMetastore implements Metastore {

  private static final Logger logger = LoggerFactory.getLogger(IcebergMetastore.class);

  private final DrillConfig config;
  private final org.apache.iceberg.Tables tables;
  private final String baseLocation;
  private final Map<String, String> commonProperties;

  /**
   * Table properties for each Iceberg table should be updated only once,
   * since during Iceberg Metastore instance existence, Drill config is not changed
   * and there is no need to update table properties each Metastore component call.
   *
   * Concurrent map stores component interface class if corresponding Iceberg table
   * was loaded, which is indication that table properties should be updated.
   */
  private final Map<Class<?>, Boolean> loadStatusMap = new ConcurrentHashMap<>();

  public IcebergMetastore(DrillConfig config) {
    this.config = config;
    Configuration configuration = configuration();
    this.tables = new HadoopTables(new Configuration(configuration));
    this.baseLocation = baseLocation(new Configuration(configuration));
    this.commonProperties = properties(IcebergConfigConstants.COMPONENTS_COMMON_PROPERTIES);
  }

  @Override
  public Tables tables() {
    Table table = loadTable(IcebergConfigConstants.COMPONENTS_TABLES_LOCATION,
      IcebergConfigConstants.COMPONENTS_TABLES_PROPERTIES,
      IcebergTables.SCHEMA, Tables.class);
    return new IcebergTables(table);
  }

  @Override
  public Views views() {
    throw new UnsupportedOperationException("Views metadata support is not implemented");
  }

  /**
   * Initializes {@link Configuration} based on config properties.
   * if config properties are not indicated, returns default instance.
   *
   * @return {@link Configuration} instance
   */
  private Configuration configuration() {
    Configuration configuration = new Configuration();
    if (config.hasPath(IcebergConfigConstants.CONFIG_PROPERTIES)) {
      Config configProperties = config.getConfig(IcebergConfigConstants.CONFIG_PROPERTIES);
      configProperties.entrySet().forEach(
        entry -> configuration.set(entry.getKey(), String.valueOf(entry.getValue().unwrapped()))
      );
    }
    return configuration;
  }

  /**
   * Constructs Iceberg tables base location based on given base and relative paths.
   * If {@link IcebergConfigConstants#BASE_PATH} is not set, user home directory is used.
   * {@link IcebergConfigConstants#RELATIVE_PATH} must be set.
   *
   * @param configuration Hadoop configuration
   * @return Iceberg table base location
   * @throws IcebergMetastoreException if unable to init file system
   *         or Iceberg Metastore relative path is not indicated
   */
  private String baseLocation(Configuration configuration) {
    FileSystem fs;
    try {
      fs = FileSystem.get(configuration);
    } catch (IOException e) {
      throw new IcebergMetastoreException(
        String.format("Error during file system [%s] setup", configuration.get(FileSystem.FS_DEFAULT_NAME_KEY)));
    }

    String root = fs.getHomeDirectory().toUri().getPath();
    if (config.hasPath(IcebergConfigConstants.BASE_PATH)) {
      root = config.getString(IcebergConfigConstants.BASE_PATH);
    }

    String relativeLocation = config.getString(IcebergConfigConstants.RELATIVE_PATH);
    if (relativeLocation == null) {
      throw new IcebergMetastoreException(String.format(
        "Iceberg Metastore relative path [%s] is not provided", IcebergConfigConstants.RELATIVE_PATH));
    }

    String location = new Path(root, relativeLocation).toUri().getPath();
    logger.info("Iceberg Metastore is located in [{}] on file system [{}]", location, fs.getUri());
    return location;
  }

  /**
   * Collects properties name and values into map if they are present in the config,
   * returns empty map otherwise.
   *
   * @param propertiesPath path to properties in the config
   * @return map with properties names and their values
   */
  private Map<String, String> properties(String propertiesPath) {
    return config.hasPath(propertiesPath)
      ? config.getConfig(propertiesPath).entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> String.valueOf(entry.getValue().unwrapped()),
        (o, n) -> n))
      : Collections.emptyMap();
  }

  /**
   * Creates / loads Iceberg table for specific component based on given location
   * and config properties and table schema.
   * Updates table properties for existing Iceberg table only once based on
   * {@link #loadStatusMap} status.
   *
   * @param componentLocationConfig path to component location config
   * @param componentPropertiesConfig path to component properties config
   * @param schema Iceberg table schema
   * @param loadClass Metastore component implementation interface
   * @return Iceberg table instance
   */
  private Table loadTable(String componentLocationConfig,
                          String componentPropertiesConfig,
                          IcebergTableSchema schema,
                          Class<?> loadClass) {
    String location = tableLocation(componentLocationConfig);
    Map<String, String> tableProperties = tableProperties(componentPropertiesConfig);

    Table table;
    try {
      table = tables.load(location);
    } catch (NoSuchTableException e) {
      try {

        // creating new Iceberg table, no need to update table properties
        return tables.create(schema.tableSchema(), schema.partitionSpec(), tableProperties, location);
      } catch (AlreadyExistsException ex) {
        table = tables.load(location);
      }
    }

    // updates table properties only during first component table call
    if (loadStatusMap.putIfAbsent(loadClass, Boolean.TRUE) == null) {
      updateTableProperties(table, tableProperties);
    }
    return table;
  }

  /**
   * Constructs Metastore component Iceberg table location based on
   * Iceberg Metastore base location and component specific location.
   *
   * @param componentLocationConfig path to component location config
   * @return component Iceberg table location
   * @throws IcebergMetastoreException if component location config is absent
   */
  private String tableLocation(String componentLocationConfig) {
    String componentLocation;
    if (config.hasPath(componentLocationConfig)) {
      componentLocation = config.getString(componentLocationConfig);
    } else {
      throw new IcebergMetastoreException(
        String.format("Component location config [%s] is not defined", componentLocationConfig));
    }

    return new Path(baseLocation, componentLocation).toUri().getPath();
  }

  /**
   * Collects common Iceberg metastore properties and component specific properties
   * into one map. Component properties take precedence.
   *
   * @param componentPropertiesConfig path to component properties config
   * @return map with properties names and values
   */
  private Map<String, String> tableProperties(String componentPropertiesConfig) {
    Map<String, String> properties = new HashMap<>(commonProperties);
    properties.putAll(properties(componentPropertiesConfig));
    return properties;
  }

  /**
   * Checks config table properties against current table properties.
   * Adds properties that are absent, updates existing and removes absent.
   * If properties are the same, does nothing.
   *
   * @param table Iceberg table instance
   * @param tableProperties table properties from the config
   */
  private void updateTableProperties(Table table, Map<String, String> tableProperties) {
    Map<String, String> currentProperties = table.properties();
    MapDifference<String, String> difference = Maps.difference(tableProperties, currentProperties);

    if (difference.areEqual()) {
      return;
    }

    UpdateProperties updateProperties = table.updateProperties();

    // collect properties that are different
    Map<String, String> propertiesToUpdate = difference.entriesDiffering().entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> entry.getValue().leftValue(),
        (o, n) -> n));

    // add new properties
    propertiesToUpdate.putAll(difference.entriesOnlyOnLeft());

    logger.debug("Updating Iceberg table [{}] properties: {}", table.location(), updateProperties);
    propertiesToUpdate.forEach(updateProperties::set);

    logger.debug("Removing Iceberg table [{}] properties: {}", table.location(), difference.entriesOnlyOnRight());
    difference.entriesOnlyOnRight().keySet().forEach(updateProperties::remove);

    updateProperties.commit();
  }

  @Override
  public void close() {
  }
}
