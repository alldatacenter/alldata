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
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(ResourceManagerTest.class)
public final class TestNotEqualSelector extends BaseTest {

  private ResourcePoolSelector testCommonHelper(Map<String, ? extends Object> selectorValue) throws RMConfigException {
    Config testConfig = ConfigFactory.empty()
      .withValue("not_equal", ConfigValueFactory.fromMap(selectorValue));
    final ResourcePoolSelector testSelector = ResourcePoolSelectorFactory.createSelector(testConfig);
    assertTrue("TestSelector is not a not_equal selector", testSelector instanceof NotEqualSelector);
    return testSelector;
  }

  @Test
  public void testValidNotEqualTagSelector() throws Exception {
    Map<String, String> tagSelectorConfig = new HashMap<>();
    tagSelectorConfig.put("tag", "marketing");
    NotEqualSelector testSelector = (NotEqualSelector)testCommonHelper(tagSelectorConfig);
    assertTrue("Expected child selector type to be TagSelector",
      testSelector.getPoolSelector() instanceof TagSelector);

    QueryContext mockContext = mock(QueryContext.class);

    // Test successful selection
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "small,large", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test unsuccessful selection
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "marketing", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }

  @Test
  public void testValidNotEqualAclSelector() throws Exception {
    Map<String, List<String>> aclSelectorValue = new HashMap<>();
    List<String> usersValue = new ArrayList<>();
    usersValue.add("user1");
    usersValue.add("user2");

    List<String> groupsValue = new ArrayList<>();
    groupsValue.add("group1");
    groupsValue.add("group2");

    aclSelectorValue.put("users", usersValue);
    aclSelectorValue.put("groups", groupsValue);

    Map<String, Map<String, List<String>>> aclSelectorConfig = new HashMap<>();
    aclSelectorConfig.put("acl", aclSelectorValue);
    NotEqualSelector testSelector = (NotEqualSelector)testCommonHelper(aclSelectorConfig);
    assertTrue("Expected child selector type to be TagSelector",
      testSelector.getPoolSelector() instanceof AclSelector);
  }

  @Test(expected = RMConfigException.class)
  public void testInValidNotEqualAclSelector() throws Exception {
    Map<String, List<String>> aclSelectorValue = new HashMap<>();

    aclSelectorValue.put("users", new ArrayList<>());
    aclSelectorValue.put("groups", new ArrayList<>());

    Map<String, Map<String, List<String>>> aclSelectorConfig = new HashMap<>();
    aclSelectorConfig.put("acl", aclSelectorValue);
    testCommonHelper(aclSelectorConfig);
  }

  @Test(expected = RMConfigException.class)
  public void testNotEqualSelectorEmptyValue() throws Exception {
    Map<String, String> notEqualSelectorValue = new HashMap<>();
    testCommonHelper(notEqualSelectorValue);
  }

  @Test(expected = RMConfigException.class)
  public void testNotEqualSelectorStringValue() throws Exception {
    Config testConfig = ConfigFactory.empty()
      .withValue("not_equal", ConfigValueFactory.fromAnyRef("null"));
    ResourcePoolSelectorFactory.createSelector(testConfig);
  }
}
