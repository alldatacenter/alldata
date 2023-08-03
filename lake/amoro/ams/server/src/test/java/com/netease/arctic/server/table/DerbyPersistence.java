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

package com.netease.arctic.server.table;

import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.persistence.SqlSessionFactoryProvider;
import com.netease.arctic.server.utils.Configurations;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DerbyPersistence extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(DerbyPersistence.class);

  private static final TemporaryFolder SINGLETON_FOLDER;

  static {
    try {
      SINGLETON_FOLDER = new TemporaryFolder();
      SINGLETON_FOLDER.create();
      String derbyFilePath = SINGLETON_FOLDER.newFolder("derby").getPath();
      String derbyUrl = String.format("jdbc:derby:%s/derby;create=true", derbyFilePath);
      Configurations configurations = new Configurations();
      configurations.set(ArcticManagementConf.DB_CONNECTION_URL, derbyUrl);
      configurations.set(ArcticManagementConf.DB_TYPE, ArcticManagementConf.DB_TYPE_DERBY);
      configurations.set(ArcticManagementConf.DB_DRIVER_CLASS_NAME, "org.apache.derby.jdbc.EmbeddedDriver");
      SqlSessionFactoryProvider.getInstance().init(configurations);
      LOG.info("Initialized derby persistent with url: {}", derbyUrl);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        SINGLETON_FOLDER.delete();
        LOG.info("Deleted resources in derby persistent.");
      }));
      truncateAllTables();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected void after() {
    truncateAllTables();
  }

  private static void truncateAllTables() {
    try (SqlSession sqlSession = SqlSessionFactoryProvider.getInstance().get().openSession(true)) {
      try (Connection connection = sqlSession.getConnection()) {
        try (Statement statement = connection.createStatement()) {
          String query = "SELECT TABLENAME FROM SYS.SYSTABLES WHERE TABLETYPE='T'";
          List<String> tableList = Lists.newArrayList();
          try (ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
              tableList.add(rs.getString(1));
            }
          }
          for (String table : tableList) {
            statement.execute("TRUNCATE TABLE " + table);
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Clear table failed", e);
    }
  }
}
