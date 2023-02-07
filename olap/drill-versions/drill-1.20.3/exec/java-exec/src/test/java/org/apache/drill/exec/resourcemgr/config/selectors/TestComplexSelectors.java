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
import org.junit.After;
import org.junit.BeforeClass;
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
public final class TestComplexSelectors extends BaseTest {

  private static final Map<String, String> tagSelectorConfig1 = new HashMap<>();

  private static final Map<String, String> tagSelectorConfig2 = new HashMap<>();

  private static final QueryContext mockContext = mock(QueryContext.class);

  private static final List<Object> complexSelectorValue = new ArrayList<>();

  @BeforeClass
  public static void classLevelSetup() {
    tagSelectorConfig1.put("tag", "small");
    tagSelectorConfig2.put("tag", "large");
  }

  @After
  public void testLevelSetup() {
    complexSelectorValue.clear();
  }

  private ResourcePoolSelector testOrCommonHelper() throws RMConfigException {
    Config testConfig = ConfigFactory.empty().withValue("or", ConfigValueFactory.fromIterable(complexSelectorValue));
    final ResourcePoolSelector testSelector = ResourcePoolSelectorFactory.createSelector(testConfig);
    assertTrue("TestSelector is not a OrSelector", testSelector instanceof OrSelector);
    return testSelector;
  }

  private ResourcePoolSelector testAndCommonHelper() throws RMConfigException {
    Config testConfig = ConfigFactory.empty().withValue("and", ConfigValueFactory.fromIterable(complexSelectorValue));
    final ResourcePoolSelector testSelector = ResourcePoolSelectorFactory.createSelector(testConfig);
    assertTrue("TestSelector is not a AndSelector", testSelector instanceof AndSelector);
    return testSelector;
  }

  @Test(expected = RMConfigException.class)
  public void testOrSelectorSingleValidSelector() throws Exception {
    // setup complexSelectorValue
    complexSelectorValue.add(tagSelectorConfig1);
    testOrCommonHelper();
  }

  @Test(expected = RMConfigException.class)
  public void testOrSelectorEmptyValue() throws Exception {
    testOrCommonHelper();
  }

  @Test(expected = RMConfigException.class)
  public void testAndSelectorEmptyValue() throws Exception {
    testAndCommonHelper();
  }

  @Test(expected = RMConfigException.class)
  public void testOrSelectorStringValue() throws Exception {
    Config testConfig = ConfigFactory.empty().withValue("or", ConfigValueFactory.fromAnyRef("dummy"));
    ResourcePoolSelectorFactory.createSelector(testConfig);
  }

  @Test(expected = RMConfigException.class)
  public void testAndSelectorStringValue() throws Exception {
    Config testConfig = ConfigFactory.empty().withValue("and", ConfigValueFactory.fromAnyRef("dummy"));
    ResourcePoolSelectorFactory.createSelector(testConfig);
  }

  @Test
  public void testOrSelectorWithTwoValidSelector() throws Exception {
    // setup complexSelectorValue
    complexSelectorValue.add(tagSelectorConfig1);
    complexSelectorValue.add(tagSelectorConfig2);

    // Test for OrSelector
    ResourcePoolSelector testSelector = testOrCommonHelper();

    // Test successful selection with OrSelector
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test unsuccessful selection with OrSelector
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "medium", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }

  @Test
  public void testAndSelectorWithTwoValidSelector() throws Exception {
    // setup complexSelectorValue
    complexSelectorValue.add(tagSelectorConfig1);
    complexSelectorValue.add(tagSelectorConfig2);

    // Test for AndSelector
    ResourcePoolSelector testSelector = testAndCommonHelper();

    // Test successful selection with AndSelector
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small,large", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test unsuccessful selection with AndSelector
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }

  @Test
  public void testAndSelectorWithNotEqualSelector() throws Exception {
    // setup NotEqualSelector config
    Map<String, Object> notEqualConfig = new HashMap<>();
    notEqualConfig.put("not_equal", tagSelectorConfig1);

    // setup complex selector value
    complexSelectorValue.add(notEqualConfig);
    complexSelectorValue.add(tagSelectorConfig2);

    // Test for AndSelector
    ResourcePoolSelector testSelector = testAndCommonHelper();

    // Test successful selection with AndSelector
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "large", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test unsuccessful selection with AndSelector
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }

  @Test
  public void testORSelectorWithNotEqualSelector() throws Exception {
    // setup NotEqualSelector config
    Map<String, Object> notEqualConfig = new HashMap<>();
    notEqualConfig.put("not_equal", tagSelectorConfig1);

    // setup complex selector value
    complexSelectorValue.add(notEqualConfig);
    complexSelectorValue.add(tagSelectorConfig2);

    // Test for AndSelector
    ResourcePoolSelector testSelector = testOrCommonHelper();

    // Test successful selection with AndSelector
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "medium", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "large", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test unsuccessful selection with AndSelector
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }

  @Test
  public void testAndSelectorWithOrSelector() throws Exception {
    // setup OrSelector config
    List<Object> orConfigValue = new ArrayList<>();
    orConfigValue.add(tagSelectorConfig1);
    orConfigValue.add(tagSelectorConfig2);

    Map<String, Object> orConfig = new HashMap<>();
    orConfig.put("or", orConfigValue);

    // get another TagSelector config
    Map<String, String> tagSelectorConfig3 = new HashMap<>();
    tagSelectorConfig3.put("tag", "medium");

    // setup complex selector value
    complexSelectorValue.add(orConfig);
    complexSelectorValue.add(tagSelectorConfig3);

    // Test for AndSelector
    ResourcePoolSelector testSelector = testAndCommonHelper();

    // Test successful selection with AndSelector
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small,medium", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "large,medium", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test unsuccessful selection with AndSelector
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY,
      ExecConstants.RM_QUERY_TAGS_KEY, "small,verylarge", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }
}
