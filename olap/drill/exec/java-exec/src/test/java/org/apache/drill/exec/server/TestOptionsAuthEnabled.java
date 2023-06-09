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
package org.apache.drill.exec.server;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_GROUP;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_USER;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_USER_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.PROCESS_USER;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

/**
 * Test setting system scoped options with user authentication enabled. (DRILL-3622)
 */
public class TestOptionsAuthEnabled extends BaseTestQuery {
  private static final String setSysOptionQuery =
      String.format("ALTER SYSTEM SET `%s` = %d;", ExecConstants.SLICE_TARGET, 200);

  @BeforeClass
  public static void setupCluster() throws Exception {
    // Create a new DrillConfig which has user authentication enabled and test authenticator set
    final DrillConfig config = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED, ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, PROCESS_USER);
    connectionProps.setProperty(DrillProperties.PASSWORD, PROCESS_USER_PASSWORD);

    updateTestCluster(1, config, connectionProps);

    // Add user "admin" to admin username list
    test(String.format("ALTER SYSTEM SET `%s`='%s,%s'", ExecConstants.ADMIN_USERS_KEY, ADMIN_USER, PROCESS_USER));

    // Set "admingrp" to admin username list
    test(String.format("ALTER SYSTEM SET `%s`='%s'", ExecConstants.ADMIN_USER_GROUPS_KEY, ADMIN_GROUP));
  }

  @Test
  public void updateSysOptAsAdminUser() throws Exception {
    updateClient(ADMIN_USER, ADMIN_USER_PASSWORD);
    setOptHelper();
  }

  @Test
  public void updateSysOptAsNonAdminUser() throws Exception {
    updateClient(TEST_USER_2, TEST_USER_2_PASSWORD);
    errorMsgTestHelper(setSysOptionQuery, "Not authorized to change SYSTEM options.");
  }

  @Test
  public void updateSysOptAsUserInAdminGroup() throws Exception {
    updateClient(TEST_USER_1, TEST_USER_1_PASSWORD);
    setOptHelper();
  }

  @Test
  public void trySettingAdminOptsAtSessionScopeAsAdmin() throws Exception {
    updateClient(ADMIN_USER, ADMIN_USER_PASSWORD);
    final String setOptionQuery =
        String.format("ALTER SESSION SET `%s`='%s,%s'", ExecConstants.ADMIN_USERS_KEY, ADMIN_USER, PROCESS_USER);
    errorMsgTestHelper(setOptionQuery, "PERMISSION ERROR: Cannot change option security.admin.users in scope SESSION");
  }

  @Test
  public void trySettingAdminOptsAtSessionScopeAsNonAdmin() throws Exception {
    updateClient(TEST_USER_2, TEST_USER_2_PASSWORD);
    final String setOptionQuery =
        String.format("ALTER SESSION SET `%s`='%s,%s'", ExecConstants.ADMIN_USERS_KEY, ADMIN_USER, PROCESS_USER);
    errorMsgTestHelper(setOptionQuery, "PERMISSION ERROR: Cannot change option security.admin.users in scope SESSION");
  }

  private void setOptHelper() throws Exception {
    try {
      test(setSysOptionQuery);
      testBuilder()
          .sqlQuery(String.format("SELECT val FROM sys.options WHERE name = '%s' AND optionScope = 'SYSTEM'",
              ExecConstants.SLICE_TARGET))
          .unOrdered()
          .baselineColumns("val")
          .baselineValues(String.valueOf(200L))
          .go();
    } finally {
      test(String.format("ALTER SYSTEM SET `%s` = %d;", ExecConstants.SLICE_TARGET, ExecConstants.SLICE_TARGET_DEFAULT));
    }
  }

  @Test
  public void testAdminUserOptions() throws Exception {

    try (ClusterFixture cluster = ClusterFixture.standardCluster(dirTestWatcher);
         ClientFixture client = cluster.clientFixture()) {
      OptionManager optionManager = cluster.drillbit().getContext().getOptionManager();

      // Admin Users Tests
      // config file should have the 'fake' default admin user and it should be returned
      // by the option manager if the option has not been set by the user
      String configAdminUser =  optionManager.getOption(ExecConstants.ADMIN_USERS_VALIDATOR);
      assertEquals(configAdminUser, ExecConstants.ADMIN_USERS_VALIDATOR.DEFAULT_ADMIN_USERS);

      // Option accessor should never return the 'fake' default from the config
      String adminUser1 = ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(optionManager);
      assertNotEquals(adminUser1, ExecConstants.ADMIN_USERS_VALIDATOR.DEFAULT_ADMIN_USERS);

      // Change testAdminUser if necessary
      String testAdminUser = "ronswanson";
      if (adminUser1.equals(testAdminUser)) {
        testAdminUser += "thefirst";
      }
      // Check if the admin option accessor honors a user-supplied value
      client.alterSystem(ExecConstants.ADMIN_USERS_KEY, testAdminUser);
      String adminUser2 = ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(optionManager);
      assertEquals(adminUser2, testAdminUser);

      // Ensure that the default admin users have admin privileges
      client.resetSystem(ExecConstants.ADMIN_USERS_KEY);
      client.resetSystem(ExecConstants.ADMIN_USER_GROUPS_KEY);
      String systemAdminUsersList0 = ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(optionManager);
      String systemAdminUserGroupsList0 = ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(optionManager);
      for (String user : systemAdminUsersList0.split(",")) {
        assertTrue(ImpersonationUtil.hasAdminPrivileges(user, systemAdminUsersList0, systemAdminUserGroupsList0));
      }

      // test if admin users, set by the user, have admin privileges
      // test if we can handle a user-supplied list that is not well formatted
      String crummyTestAdminUsersList = " alice, bob bob, charlie  ,, dave ";
      client.alterSystem(ExecConstants.ADMIN_USERS_KEY, crummyTestAdminUsersList);
      String[] sanitizedAdminUsers = {"alice", "bob bob", "charlie", "dave"};
      // also test the CSV sanitizer
      assertEquals(Joiner.on(",").join(sanitizedAdminUsers), DrillStringUtils.sanitizeCSV(crummyTestAdminUsersList));
      String systemAdminUsersList1 = ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(optionManager);
      String systemAdminUserGroupsList1 = ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(optionManager);
      for (String user : sanitizedAdminUsers) {
        assertTrue(ImpersonationUtil.hasAdminPrivileges(user, systemAdminUsersList1, systemAdminUserGroupsList1));
      }

      // Admin User Groups Tests
      // config file should have the 'fake' default admin user and it should be returned
      // by the option manager if the option has not been set by the user
      String configAdminUserGroups =  optionManager.getOption(ExecConstants.ADMIN_USER_GROUPS_VALIDATOR);
      assertEquals(configAdminUserGroups, ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.DEFAULT_ADMIN_USER_GROUPS);

      // Option accessor should never return the 'fake' default from the config
      String adminUserGroups1 = ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(optionManager);
      assertNotEquals(adminUserGroups1, ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.DEFAULT_ADMIN_USER_GROUPS);

      // Change testAdminUserGroups if necessary
      String testAdminUserGroups = "yakshavers";
      if (adminUserGroups1.equals(testAdminUserGroups)) {
        testAdminUserGroups += ",wormracers";
      }
      // Check if the admin option accessor honors a user-supplied values
      client.alterSystem(ExecConstants.ADMIN_USER_GROUPS_KEY, testAdminUserGroups);
      String adminUserGroups2 = ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(optionManager);
      assertEquals(adminUserGroups2, testAdminUserGroups);

      // Test if we can handle a user-supplied admin user groups list that is not well formatted
      String crummyTestAdminUserGroupsList = " g1, g 2, g4 ,, g5 ";
      client.alterSystem(ExecConstants.ADMIN_USER_GROUPS_KEY, crummyTestAdminUserGroupsList);
      String systemAdminUserGroupsList2 = ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(optionManager);
      // test if all the group tokens are well-formed
      // Note: A hasAdminPrivilege() test cannot be done here, like in the tests for handling a crummy admin user list.
      // This is because ImpersonationUtil currently does not implement an API that takes a group as an input to check
      // for admin privileges
      for (String group : systemAdminUserGroupsList2.split(",")) {
        assertTrue(group.length() != 0);
        assertTrue(group.trim().equals(group));
      }
    }
  }
}
