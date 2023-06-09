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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(ResourceManagerTest.class)
public final class TestTagSelector extends BaseTest {

  private ResourcePoolSelector testCommonHelper(Object tagValue) throws RMConfigException {
    Config testConfig = ConfigFactory.empty()
      .withValue("tag", ConfigValueFactory.fromAnyRef(tagValue));
    final ResourcePoolSelector testSelector = ResourcePoolSelectorFactory.createSelector(testConfig);
    assertTrue("TestSelector is not a tag selector", testSelector instanceof TagSelector);
    assertEquals("Unexpected value for tag selector", tagValue, ((TagSelector) testSelector).getTagValue());
    return testSelector;
  }

  // Tests for TagSelector
  @Test
  public void testValidTagSelector() throws Exception {
    testCommonHelper("marketing");
  }

  @Test(expected = RMConfigException.class)
  public void testInValidTagSelector() throws Exception {
    testCommonHelper("");
    testCommonHelper(null);
  }

  @Test
  public void testTagSelectorWithQueryTags() throws Exception {
    QueryContext mockContext = mock(QueryContext.class);
    TagSelector testSelector = (TagSelector) testCommonHelper("small");

    // Test successful selection
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "small,large", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertTrue(testSelector.isQuerySelected(mockContext));

    // Test empty query tags
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));

    // Test different query tags
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "medium", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assertFalse(testSelector.isQuerySelected(mockContext));
  }
}
