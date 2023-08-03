/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.persistence;

import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.persistence.mapper.ApiTokensMapper;
import com.netease.arctic.server.persistence.mapper.CatalogMetaMapper;
import com.netease.arctic.server.persistence.mapper.OptimizerMapper;
import com.netease.arctic.server.persistence.mapper.OptimizingMapper;
import com.netease.arctic.server.persistence.mapper.PlatformFileMapper;
import com.netease.arctic.server.persistence.mapper.ResourceMapper;
import com.netease.arctic.server.persistence.mapper.TableBlockerMapper;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.utils.Configurations;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;

public class SqlSessionFactoryProvider {

  private static final String DERBY_INIT_SQL_SCRIPT = "derby/ams-derby-init.sql";

  private static final SqlSessionFactoryProvider INSTANCE = new SqlSessionFactoryProvider();

  public static SqlSessionFactoryProvider getInstance() {
    return INSTANCE;
  }

  private volatile SqlSessionFactory sqlSessionFactory;

  public void init(Configurations config) {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl(config.getString(ArcticManagementConf.DB_CONNECTION_URL));
    dataSource.setDriverClassName(config.getString(ArcticManagementConf.DB_DRIVER_CLASS_NAME));
    if (ArcticManagementConf.DB_TYPE_MYSQL.equals(config.getString(ArcticManagementConf.DB_TYPE))) {
      dataSource.setUsername(config.getString(ArcticManagementConf.DB_USER_NAME));
      dataSource.setPassword(config.getString(ArcticManagementConf.DB_PASSWORD));
    }
    dataSource.setDefaultAutoCommit(false);
    dataSource.setMaxTotal(20);
    dataSource.setMaxIdle(16);
    dataSource.setMinIdle(0);
    dataSource.setMaxWaitMillis(1000L);
    dataSource.setLogAbandoned(true);
    dataSource.setRemoveAbandonedOnBorrow(true);
    dataSource.setRemoveAbandonedTimeout(60);
    dataSource.setTimeBetweenEvictionRunsMillis(Duration.ofMillis(10 * 60 * 1000L).toMillis());
    dataSource.setTestOnBorrow(BaseObjectPoolConfig.DEFAULT_TEST_ON_BORROW);
    dataSource.setTestWhileIdle(BaseObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE);
    dataSource.setMinEvictableIdleTimeMillis(1000);
    dataSource.setNumTestsPerEvictionRun(BaseObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
    dataSource.setTestOnReturn(BaseObjectPoolConfig.DEFAULT_TEST_ON_RETURN);
    dataSource.setSoftMinEvictableIdleTimeMillis(
        BaseObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_TIME.toMillis());
    dataSource.setLifo(BaseObjectPoolConfig.DEFAULT_LIFO);
    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    Environment environment = new Environment("develop", transactionFactory, dataSource);
    Configuration configuration = new Configuration(environment);
    configuration.addMapper(TableMetaMapper.class);
    configuration.addMapper(OptimizingMapper.class);
    configuration.addMapper(CatalogMetaMapper.class);
    configuration.addMapper(OptimizerMapper.class);
    configuration.addMapper(ApiTokensMapper.class);
    configuration.addMapper(PlatformFileMapper.class);
    configuration.addMapper(ResourceMapper.class);
    configuration.addMapper(TableBlockerMapper.class);
    if (sqlSessionFactory == null) {
      synchronized (this) {
        if (sqlSessionFactory == null) {
          sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        }
      }
    }
    createTablesIfNeed(config);
  }

  // create tables for derby database type
  private void createTablesIfNeed(Configurations config) {
    if (ArcticManagementConf.DB_TYPE_DERBY.equals(config.getString(ArcticManagementConf.DB_TYPE))) {
      try (SqlSession sqlSession = get().openSession(true)) {
        try (Connection connection = sqlSession.getConnection()) {
          try (Statement statement = connection.createStatement()) {
            String query = "SELECT 1 FROM SYS.SYSTABLES WHERE TABLENAME = 'CATALOG_METADATA'";
            try (ResultSet rs = statement.executeQuery(query)) {
              if (!rs.next()) {
                ScriptRunner runner = new ScriptRunner(connection);
                runner.runScript(new InputStreamReader(new FileInputStream(getDerbyInitSqlScriptPath()),
                    StandardCharsets.UTF_8));
              }
            }
          }
        }
      } catch (Exception e) {
        throw new IllegalStateException("Create derby tables failed", e);
      }
    }
  }

  private String getDerbyInitSqlScriptPath() {
    URL scriptUrl = ClassLoader.getSystemResource(DERBY_INIT_SQL_SCRIPT);
    if (scriptUrl == null) {
      throw new IllegalStateException("Cannot find derby init sql script:" + DERBY_INIT_SQL_SCRIPT);
    }
    return scriptUrl.getPath();
  }

  public SqlSessionFactory get() {
    Preconditions.checkState(
        sqlSessionFactory != null,
        "Persistent configuration is not initialized yet.");

    return sqlSessionFactory;
  }
}
