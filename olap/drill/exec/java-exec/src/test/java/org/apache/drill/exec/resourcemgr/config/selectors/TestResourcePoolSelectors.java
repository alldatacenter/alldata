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
import org.apache.drill.categories.ResourceManagerTest;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

@Category(ResourceManagerTest.class)
public class TestResourcePoolSelectors extends BaseTest {

  @Test
  public void testNullSelectorConfig() throws Exception {
    final ResourcePoolSelector testSelector = ResourcePoolSelectorFactory.createSelector(null);
    assertTrue("TestSelector with null config is not of type Default Selector",
      testSelector instanceof DefaultSelector);
    assertTrue("DefaultSelector type is not default",
      testSelector.getSelectorType() == ResourcePoolSelector.SelectorType.DEFAULT);
  }

  @Test(expected = RMConfigException.class)
  public void testEmptySelectorConfig() throws Exception {
    Config testConfig = ConfigFactory.empty();
    ResourcePoolSelectorFactory.createSelector(testConfig);
  }
}
