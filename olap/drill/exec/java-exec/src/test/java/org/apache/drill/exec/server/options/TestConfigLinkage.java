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
package org.apache.drill.exec.server.options;

import org.apache.drill.categories.OptionsTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.sys.SystemTable;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
/*
 * Tests to test if the linkage between the two config option systems
 * i.e., the linkage between boot-config system and system/session options.
 * Tests to assert if the config options are read in the order of session , system, boot-config.
 * Max width per node is slightly different from other options since it is set to zero by default
 * in the config and the option value is computed dynamically everytime if the value is zero
 * i.e., if the value is not set in system/session.
 * */

@Category({OptionsTest.class, SlowTest.class})
public class TestConfigLinkage extends BaseTest {
  public static final String MOCK_PROPERTY = "mock.prop";

  @Rule
  public BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  public static OptionDefinition createMockPropOptionDefinition() {
    return new OptionDefinition(new TypeValidators.StringValidator(MOCK_PROPERTY, null), new OptionMetaData(OptionValue.AccessibleScopes.ALL, false, true));
  }

  @Test
  public void testDefaultInternalValue() throws Exception {
    OptionDefinition optionDefinition = createMockPropOptionDefinition();

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .configProperty(ExecConstants.bootDefaultFor(MOCK_PROPERTY), "a")
      .putDefinition(optionDefinition);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String mockProp = client.queryBuilder().
        sql("SELECT string_val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS_OLD.getTableName(), MOCK_PROPERTY).singletonString();
      String mockProp2 = client.queryBuilder().
        sql("SELECT val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS.getTableName(), MOCK_PROPERTY).singletonString();

      assertEquals("a", mockProp);
      assertEquals("a", mockProp2);
    }
  }

  @Test
  public void testDefaultValidatorInternalValue() throws Exception {
    OptionDefinition optionDefinition = createMockPropOptionDefinition();

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher).
      putDefinition(optionDefinition).
      configProperty(ExecConstants.bootDefaultFor(MOCK_PROPERTY), "a");

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String mockProp = client.queryBuilder().
        sql("SELECT string_val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS_OLD.getTableName(), MOCK_PROPERTY).singletonString();
      String mockProp2 = client.queryBuilder().
        sql("SELECT val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS.getTableName(), MOCK_PROPERTY).singletonString();

      assertEquals("a", mockProp);
      assertEquals("a", mockProp2);
    }
  }

  /* Test if session option takes precedence */
  @Test
  public void testSessionOption() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .sessionOption(ExecConstants.SLICE_TARGET, 10);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String slice_target = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.slice_target' and optionScope = 'SESSION'", SystemTable.OPTIONS
        .getTableName())
        .singletonString();
      assertEquals(slice_target, "10");
    }
  }

  /* Test if system option takes precedence over the boot option */
  @Test
  public void testSystemOption() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .systemOption(ExecConstants.SLICE_TARGET, 10000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String slice_target = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.slice_target' and optionScope = 'SYSTEM'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals(slice_target, "10000");
    }
  }

  @Test
  public void testInternalSystemOption() throws Exception {
    OptionDefinition optionDefinition = createMockPropOptionDefinition();

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher).
      putDefinition(optionDefinition).
      configProperty(ExecConstants.bootDefaultFor(MOCK_PROPERTY), "a").
      systemOption(MOCK_PROPERTY, "blah");

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String mockProp = client.queryBuilder().
        sql("SELECT string_val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS_OLD.getTableName(), MOCK_PROPERTY)
        .singletonString();
      String mockProp2 = client.queryBuilder().
        sql("SELECT val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS.getTableName(), MOCK_PROPERTY)
        .singletonString();

      assertEquals("blah", mockProp);
      assertEquals("blah", mockProp2);
    }
  }

  /* Test if config option takes precedence if config option is not set */
  @Test
  public void testConfigOption() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
            .setOptionDefault(ExecConstants.SLICE_TARGET, 1000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String slice_target = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.slice_target' and optionScope = 'BOOT'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals(slice_target, "1000");
    }
  }

  /* Test if altering system option takes precedence over config option */
  @Test
  public void testAlterSystem() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
         ClientFixture client = cluster.clientFixture()) {
      client.queryBuilder().sql("ALTER SYSTEM SET `planner.slice_target` = 10000").run();
      String slice_target = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.slice_target' and optionScope = 'SYSTEM'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals(slice_target, "10000");
    }
  }

  /* Test if altering session option takes precedence over system option */
  @Test
  public void testSessionPrecedence() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
      .systemOption(ExecConstants.SLICE_TARGET, 100000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.queryBuilder().sql("ALTER SESSION SET `planner.slice_target` = 10000").run();
      String slice_target = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.slice_target' and optionScope = 'SESSION'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals(slice_target, "10000");
    }
  }

  /* Test if setting maxwidth option through config takes effect */
  @Test
  public void testMaxWidthPerNodeConfig() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .setOptionDefault(ExecConstants.MAX_WIDTH_PER_NODE_KEY, 2);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String maxWidth = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.width.max_per_node' and optionScope = 'BOOT'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals("2", maxWidth);
    }
  }

  /* Test if setting maxwidth at system level takes precedence */
  @Test
  public void testMaxWidthPerNodeSystem() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .systemOption(ExecConstants.MAX_WIDTH_PER_NODE_KEY, 3);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String maxWidth = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.width.max_per_node' and optionScope = 'SYSTEM'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals("3", maxWidth);
    }
  }

  /* Test if setting maxwidth at session level takes precedence */
  @Test
  public void testMaxWidthPerNodeSession() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .sessionOption(ExecConstants.MAX_WIDTH_PER_NODE_KEY, 2);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String maxWidth = client.queryBuilder().sql("SELECT val FROM sys.%s where name='planner.width.max_per_node' and optionScope = 'SESSION'", SystemTable.OPTIONS.getTableName())
        .singletonString();
      assertEquals("2", maxWidth);
    }
  }

  /* Test if max width is computed correctly using the cpu load average
     when the option is not set at either system or session level
  */
  @Test
  public void testMaxWidthPerNodeDefault() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .setOptionDefault(ExecConstants.CPU_LOAD_AVERAGE_KEY, 0.70);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      long maxWidth = ExecConstants.MAX_WIDTH_PER_NODE.computeMaxWidth(0.70, 0);
      int availProc = Runtime.getRuntime().availableProcessors();
      long maxWidthPerNode = Math.max(1, Math.min(availProc, Math.round(availProc * 0.70)));
      assertEquals(maxWidthPerNode, maxWidth);
    }
  }

  /* Test if the scope is set during BOOT time and scope is actually BOOT */
  @Test
  public void testScope() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .setOptionDefault(ExecConstants.SLICE_TARGET, 100000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String scope = client.queryBuilder()
                          .sql("SELECT optionScope from sys.%s where name='planner.slice_target'", SystemTable.OPTIONS.getTableName())
                          .singletonString();
      Assert.assertEquals("BOOT",scope);
    }
  }

  /* Test if the option is set at SYSTEM scope and the scope is actually SYSTEM */
  @Test
  public void testScopeSystem() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .systemOption(ExecConstants.SLICE_TARGET, 10000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String scope = client.queryBuilder()
              .sql("SELECT optionScope from sys.%s where name='planner.slice_target'", SystemTable.OPTIONS.getTableName())
              .singletonString();
      Assert.assertEquals("SYSTEM",scope);
    }
  }

  /* Test if the option is set at SESSION scope and the scope is actually SESSION */
  @Test
  public void testScopeSession() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher)
      .sessionOption(ExecConstants.SLICE_TARGET, 100000);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String scope = client.queryBuilder()
              .sql("SELECT optionScope from sys.%s where name='planner.slice_target'", SystemTable.OPTIONS.getTableName())
              .singletonString();
      Assert.assertEquals("SESSION",scope);
    }
  }

  /* Test if the option is altered at SYSTEM scope and the scope is actually SYSTEM */
  @Test
  public void testScopeAlterSystem() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.queryBuilder().sql("ALTER SYSTEM set `planner.slice_target`= 10000").run();
      String scope = client.queryBuilder()
              .sql("SELECT optionScope from sys.%s where name='planner.slice_target'", SystemTable.OPTIONS.getTableName())
              .singletonString();
      Assert.assertEquals("SYSTEM",scope);
    }
  }

  /* Test if the option is altered at SESSION scope and the scope is actually SESSION */
  @Test
  public void testScopeAlterSession() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.bareBuilder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.queryBuilder().sql("ALTER SESSION set `planner.slice_target`= 10000").run();
      String scope = client.queryBuilder()
              .sql("SELECT optionScope from sys.%s where name='planner.slice_target'", SystemTable.OPTIONS.getTableName())
              .singletonString();
      Assert.assertEquals("SESSION",scope);
    }
  }

  @Test
  public void testAlterInternalSystemOption() throws Exception {
    OptionDefinition optionDefinition = createMockPropOptionDefinition();

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher).
      configProperty(ExecConstants.bootDefaultFor(MOCK_PROPERTY), "a").
      putDefinition(optionDefinition);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      client.queryBuilder().sql("ALTER SYSTEM SET `%s` = 'bleh'", MOCK_PROPERTY).run();

      client.queryBuilder().sql("SELECT * FROM sys.%s", SystemTable.INTERNAL_OPTIONS_OLD.getTableName()).logCsv();
      client.queryBuilder().sql("SELECT * FROM sys.%s", SystemTable.INTERNAL_OPTIONS.getTableName()).logCsv();

      String mockProp = client.queryBuilder().
        sql("SELECT string_val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS_OLD, MOCK_PROPERTY).singletonString();
      String mockProp2 = client.queryBuilder().
        sql("SELECT val FROM sys.%s where name='%s'", SystemTable.INTERNAL_OPTIONS, MOCK_PROPERTY).singletonString();

      assertEquals("bleh", mockProp);
      assertEquals("bleh", mockProp2);
    }
  }
}
