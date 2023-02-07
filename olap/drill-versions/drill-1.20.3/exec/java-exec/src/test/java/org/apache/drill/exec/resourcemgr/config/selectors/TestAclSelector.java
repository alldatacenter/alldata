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
package org.apache.drill.exec.resourcemgr.config.selectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.ResourceManagerTest;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(ResourceManagerTest.class)
public final class TestAclSelector extends BaseTest {

  private static final List<String> groupsValue = new ArrayList<>();

  private static final List<String> usersValue = new ArrayList<>();

  private static final List<String> emptyList = new ArrayList<>();

  private static final Map<String, List<String>> aclConfigValue = new HashMap<>();

  private boolean checkIfSame(Object[] expected, Object[] actual) {
    Arrays.sort(expected);
    Arrays.sort(actual);
    return Arrays.equals(expected, actual);
  }

  @After
  public void cleanupAfterTest() {
    groupsValue.clear();
    usersValue.clear();
    aclConfigValue.clear();
  }

  private ResourcePoolSelector testNegativeHelper() throws RMConfigException {
    final Config testConfig = ConfigFactory.empty()
      .withValue("acl", ConfigValueFactory.fromMap(aclConfigValue));
    return ResourcePoolSelectorFactory.createSelector(testConfig);
  }

  private ResourcePoolSelector testCommonHelper(List<String> expectedPositiveUsers, List<String> expectedPositiveGroups,
                                                List<String> expectedNegativeUsers, List<String> expectedNegativeGroups)
    throws RMConfigException {
    ResourcePoolSelector testSelector = testNegativeHelper();
    assertTrue("TestSelector is not a ACL selector", testSelector instanceof AclSelector);
    assertTrue("Expected +ve users config mismatched with actual config",
      checkIfSame(expectedPositiveUsers.toArray(), ((AclSelector) testSelector).getAllowedUsers().toArray()));
    assertTrue("Expected +ve groups config mismatched with actual config",
      checkIfSame(expectedPositiveGroups.toArray(), ((AclSelector) testSelector).getAllowedGroups().toArray()));
    assertTrue("Expected -ve users config mismatched with actual config",
      checkIfSame(expectedNegativeUsers.toArray(), ((AclSelector) testSelector).getDeniedUsers().toArray()));
    assertTrue("Expected -ve groups config mismatched with actual config",
      checkIfSame(expectedNegativeGroups.toArray(), ((AclSelector) testSelector).getDeniedGroups().toArray()));
    return testSelector;
  }

  @Test
  public void testValidACLSelector_shortSyntax() throws Exception {
    groupsValue.add("sales");
    groupsValue.add("marketing");

    usersValue.add("user1");
    usersValue.add("user2");

    aclConfigValue.put("groups", groupsValue);
    aclConfigValue.put("users", usersValue);
    AclSelector testSelector = (AclSelector) testCommonHelper(usersValue, groupsValue, emptyList, emptyList);

    // check based on valid/invalid user
    Set<String> groups = new HashSet<>();
    assertFalse(testSelector.checkQueryUserGroups("user3", groups));
    assertTrue(testSelector.checkQueryUserGroups("user1", groups));

    // check based on correct group
    groups.add("sales");
    assertTrue(testSelector.checkQueryUserGroups("user3", groups));
  }

  @Test
  public void testACLSelector_onlyUsers() throws Exception {
    usersValue.add("user1");
    aclConfigValue.put("users", usersValue);
    testCommonHelper(usersValue, groupsValue, emptyList, emptyList);
  }

  @Test
  public void testACLSelector_onlyGroups() throws Exception {
    groupsValue.add("group1");
    aclConfigValue.put("groups", groupsValue);
    testCommonHelper(usersValue, groupsValue, emptyList, emptyList);
  }

  @Test(expected = RMConfigException.class)
  public void testInValidACLSelector_shortSyntax() throws Exception {
    aclConfigValue.put("groups", new ArrayList<>());
    aclConfigValue.put("users", new ArrayList<>());
    testNegativeHelper();
  }

  @Test
  public void testValidACLSelector_longSyntax() throws Exception {

    groupsValue.add("sales:+");
    groupsValue.add("marketing:-");

    List<String> expectedAllowedGroups = new ArrayList<>();
    expectedAllowedGroups.add("sales");

    List<String> expectedDisAllowedGroups = new ArrayList<>();
    expectedDisAllowedGroups.add("marketing");

    usersValue.add("user1:+");
    usersValue.add("user2:-");

    List<String> expectedAllowedUsers = new ArrayList<>();
    expectedAllowedUsers.add("user1");
    List<String> expectedDisAllowedUsers = new ArrayList<>();
    expectedDisAllowedUsers.add("user2");

    aclConfigValue.put("groups", groupsValue);
    aclConfigValue.put("users", usersValue);
    AclSelector testSelector = (AclSelector)testCommonHelper(expectedAllowedUsers, expectedAllowedGroups,
      expectedDisAllowedUsers, expectedDisAllowedGroups);

    Set<String> queryGroups = new HashSet<>();
    // Negative user/group
    queryGroups.add("marketing");
    assertFalse(testSelector.checkQueryUserGroups("user2", queryGroups));

    // Invalid user -ve group
    assertFalse(testSelector.checkQueryUserGroups("user3", queryGroups));

    // -ve user +ve group
    queryGroups.clear();
    queryGroups.add("sales");
    assertFalse(testSelector.checkQueryUserGroups("user2", queryGroups));

    // Invalid user +ve group
    assertTrue(testSelector.checkQueryUserGroups("user3", queryGroups));
  }

  @Test(expected = RMConfigException.class)
  public void testInvalidLongSyntaxIdentifier() throws Exception {
    groupsValue.add("sales:|");
    aclConfigValue.put("groups", groupsValue);
    testNegativeHelper();
  }

  @Test
  public void testMixLongShortAclSyntax() throws Exception {
    groupsValue.add("groups1");
    groupsValue.add("groups2:+");
    groupsValue.add("groups3:-");

    List<String> expectedAllowedGroups = new ArrayList<>();
    expectedAllowedGroups.add("groups1");
    expectedAllowedGroups.add("groups2");

    List<String> expectedDisAllowedGroups = new ArrayList<>();
    expectedDisAllowedGroups.add("groups3");

    usersValue.add("user1");
    usersValue.add("user2:+");
    usersValue.add("user3:-");

    List<String> expectedAllowedUsers = new ArrayList<>();
    expectedAllowedUsers.add("user1");
    expectedAllowedUsers.add("user2");

    List<String> expectedDisAllowedUsers = new ArrayList<>();
    expectedDisAllowedUsers.add("user3");

    aclConfigValue.put("groups", groupsValue);
    aclConfigValue.put("users", usersValue);

    testCommonHelper(expectedAllowedUsers, expectedAllowedGroups, expectedDisAllowedUsers, expectedDisAllowedGroups);
  }

  @Test
  public void testSameUserBothInPositiveNegative() throws Exception {
    usersValue.add("user1:+");
    usersValue.add("user1:-");

    List<String> expectedDisAllowedUsers = new ArrayList<>();
    expectedDisAllowedUsers.add("user1");

    aclConfigValue.put("users", usersValue);
    testCommonHelper(emptyList, emptyList, expectedDisAllowedUsers, emptyList);
  }

  @Test
  public void testStarInPositiveUsers() throws Exception {
    usersValue.add("*:+");
    usersValue.add("user1:-");

    List<String> expectedAllowedUsers = new ArrayList<>();
    expectedAllowedUsers.add("*");

    List<String> expectedDisAllowedUsers = new ArrayList<>();
    expectedDisAllowedUsers.add("user1");

    aclConfigValue.put("users", usersValue);
    AclSelector testSelector = (AclSelector)testCommonHelper(expectedAllowedUsers, emptyList,
      expectedDisAllowedUsers, emptyList);

    Set<String> queryGroups = new HashSet<>();
    // -ve user with Invalid groups
    assertFalse(testSelector.checkQueryUserGroups("user1", queryGroups));

    // Other user with invalid groups
    assertTrue(testSelector.checkQueryUserGroups("user2", queryGroups));
  }

  @Test
  public void testStarInNegativeUsers() throws Exception {
    usersValue.add("*:-");
    usersValue.add("user1:+");

    List<String> expectedAllowedUsers = new ArrayList<>();
    expectedAllowedUsers.add("user1");

    List<String> expectedDisAllowedUsers = new ArrayList<>();
    expectedDisAllowedUsers.add("*");

    aclConfigValue.put("users", usersValue);
    AclSelector testSelector = (AclSelector)testCommonHelper(expectedAllowedUsers, emptyList,
      expectedDisAllowedUsers, emptyList);

    Set<String> queryGroups = new HashSet<>();
    // Other user with Invalid groups
    assertFalse(testSelector.checkQueryUserGroups("user2", queryGroups));

    // +ve user with invalid groups
    assertTrue(testSelector.checkQueryUserGroups("user1", queryGroups));
  }
}
