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

import com.zaxxer.hikari.HikariDataSource;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class TestDataSource extends BaseTest {

  @Rule
  public BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String DRIVER = "org.h2.Driver";

  private String url;

  @Before
  public void init() throws Exception {
    url = "jdbc:h2:" + dirTestWatcher.getTmpDir().getCanonicalPath();
  }

  @Test
  public void testInitWithoutUserAndPassword() {
    JdbcStorageConfig config = new JdbcStorageConfig(
      DRIVER, url, null, null, false, false, null, null, 1000);
    try (HikariDataSource dataSource = JdbcStoragePlugin.initDataSource(config)) {
      assertEquals(DRIVER, dataSource.getDriverClassName());
      assertEquals(url, dataSource.getJdbcUrl());
      assertNull(dataSource.getUsername());
      assertNull(dataSource.getPassword());
    }
  }

  @Test
  public void testInitWithUserAndPassword() {
    JdbcStorageConfig config = new JdbcStorageConfig(
      DRIVER, url, "user", "password", false, false, null, null, 1000);
    try (HikariDataSource dataSource = JdbcStoragePlugin.initDataSource(config)) {
      assertEquals("user", dataSource.getUsername());
      assertEquals("password", dataSource.getPassword());
    }
  }

  @Test
  public void testInitWithSourceParameters() {
    Map<String, Object> sourceParameters = new HashMap<>();
    sourceParameters.put("minimumIdle", 5);
    sourceParameters.put("autoCommit", false);
    sourceParameters.put("connectionTestQuery", "select * from information_schema.collations");
    sourceParameters.put("dataSource.cachePrepStmts", true);
    sourceParameters.put("dataSource.prepStmtCacheSize", 250);
    JdbcStorageConfig config = new JdbcStorageConfig(
      DRIVER, url, "user", "password", false, false, sourceParameters, null, 1000);
    try (HikariDataSource dataSource = JdbcStoragePlugin.initDataSource(config)) {
      assertEquals(5, dataSource.getMinimumIdle());
      assertFalse(dataSource.isAutoCommit());
      assertEquals("select * from information_schema.collations", dataSource.getConnectionTestQuery());
      assertEquals(true, dataSource.getDataSourceProperties().get("cachePrepStmts"));
      assertEquals(250, dataSource.getDataSourceProperties().get("prepStmtCacheSize"));
    }
  }

  @Test
  public void testInitWithIncorrectSourceParameterName() {
    Map<String, Object> sourceParameters = new HashMap<>();
    sourceParameters.put("abc", "abc");
    JdbcStorageConfig config = new JdbcStorageConfig(
      DRIVER, url, "user", "password", false, false, sourceParameters, null, 1000);

    thrown.expect(UserException.class);
    thrown.expectMessage(UserBitShared.DrillPBError.ErrorType.CONNECTION.name());

    JdbcStoragePlugin.initDataSource(config);
  }

  @Test
  public void testInitWithIncorrectSourceParameterValue() {
    Map<String, Object> sourceParameters = new HashMap<>();
    sourceParameters.put("minimumIdle", "abc");
    JdbcStorageConfig config = new JdbcStorageConfig(
      DRIVER, url, "user", "password", false, false, sourceParameters, null, 1000);

    thrown.expect(UserException.class);
    thrown.expectMessage(UserBitShared.DrillPBError.ErrorType.CONNECTION.name());

    JdbcStoragePlugin.initDataSource(config);
  }
}
