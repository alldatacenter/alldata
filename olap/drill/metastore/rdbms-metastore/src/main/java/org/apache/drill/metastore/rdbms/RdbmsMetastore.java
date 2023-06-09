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
package org.apache.drill.metastore.rdbms;

import com.typesafe.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.Metastore;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.components.views.Views;
import org.apache.drill.metastore.rdbms.components.tables.RdbmsTables;
import org.apache.drill.metastore.rdbms.config.RdbmsConfigConstants;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.metastore.rdbms.util.DbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/**
 * RDBMS Drill Metastore implementation that creates necessary tables using Liquibase,
 * initializes data source using provided config.
 */
public class RdbmsMetastore implements Metastore {

  private static final Logger logger = LoggerFactory.getLogger(RdbmsMetastore.class);

  private static final String LIQUIBASE_CHANGELOG_FILE = "db/changelog/changelog.yaml";

  private final QueryExecutorProvider executorProvider;

  public RdbmsMetastore(DrillConfig config) {
    HikariDataSource dataSource = dataSource(config);
    this.executorProvider = new QueryExecutorProvider(dataSource);
    initTables(dataSource);
  }

  @Override
  public Tables tables() {
    return new RdbmsTables(executorProvider);
  }

  @Override
  public Views views() {
    throw new UnsupportedOperationException("Views metadata support is not implemented");
  }

  @Override
  public void close() {
    executorProvider.close();
  }

  /**
   * Prepares database before initializing data source based on its type,
   * initializes {@link HikariDataSource} instance and configures it based on given
   * Metastore configuration.
   * Basic parameters such as driver, url, user name and password are set using setters.
   * Other source parameters are set dynamically through the properties. See the list
   * of available Hikari properties: <a href="https://github.com/brettwooldridge/HikariCP">.
   *
   * @param config Metastore config
   * @return Hikari data source instance
   * @throws RdbmsMetastoreException if unable to configure Hikari data source
   */
  private HikariDataSource dataSource(DrillConfig config) {
    DbHelper.init(config).prepareDatabase();
    try {
      Properties properties = new Properties();
      if (config.hasPath(RdbmsConfigConstants.DATA_SOURCE_PROPERTIES)) {
        Config propertiesConfig = config.getConfig(RdbmsConfigConstants.DATA_SOURCE_PROPERTIES);
        propertiesConfig.entrySet().forEach(e -> properties.put(e.getKey(), e.getValue().unwrapped()));
      }
      HikariConfig hikariConfig = new HikariConfig(properties);
      hikariConfig.setDriverClassName(config.getString(RdbmsConfigConstants.DATA_SOURCE_DRIVER));
      hikariConfig.setJdbcUrl(config.getString(RdbmsConfigConstants.DATA_SOURCE_URL));
      if (config.hasPath(RdbmsConfigConstants.DATA_SOURCE_USER_NAME)) {
        hikariConfig.setUsername(config.getString(RdbmsConfigConstants.DATA_SOURCE_USER_NAME));
      }
      if (config.hasPath(RdbmsConfigConstants.DATA_SOURCE_PASSWORD)) {
        hikariConfig.setPassword(config.getString(RdbmsConfigConstants.DATA_SOURCE_PASSWORD));
      }
      return new HikariDataSource(hikariConfig);
    } catch (RuntimeException e) {
      throw new RdbmsMetastoreException("Unable to init RDBMS Metastore data source: " + e.getMessage(), e);
    }
  }

  /**
   * Initializes RDBMS Metastore tables structure based on {@link #LIQUIBASE_CHANGELOG_FILE} file.
   * See <a href="https://www.liquibase.org/documentation/core-concepts/index.html"> for more details.
   *
   * @param dataSource data source
   */
  private void initTables(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      JdbcConnection jdbcConnection = new JdbcConnection(connection);
      // TODO It is recommended to use the new function if the following issue is resolved.
      // https://github.com/liquibase/liquibase/issues/2349
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
      ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
      try (Liquibase liquibase = new Liquibase(LIQUIBASE_CHANGELOG_FILE, resourceAccessor, database)) {
        liquibase.update("");
      }
    } catch (Exception e) {
      throw new RdbmsMetastoreException("Unable to init Metastore tables using Liquibase: " + e.getMessage(), e);
    }
  }
}
