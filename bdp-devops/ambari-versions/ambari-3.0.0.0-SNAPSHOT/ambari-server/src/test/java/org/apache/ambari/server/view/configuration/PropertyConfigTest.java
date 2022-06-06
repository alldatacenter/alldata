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

package org.apache.ambari.server.view.configuration;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * PropertyConfig tests.
 */
public class PropertyConfigTest {
  @Test
  public void testGetKey() throws Exception {

    List<InstanceConfig> instanceConfigs = InstanceConfigTest.getInstanceConfigs();
    for (InstanceConfig instanceConfig : instanceConfigs) {
      List<PropertyConfig> propertyConfigs = instanceConfig.getProperties();

      Assert.assertTrue(propertyConfigs.size() <= 2);
      Assert.assertEquals("p1", propertyConfigs.get(0).getKey());

      if (propertyConfigs.size() == 2) {
        Assert.assertEquals("p2", propertyConfigs.get(1).getKey());
      }
    }
  }

  @Test
  public void testGetValue() throws Exception {
    List<InstanceConfig> instanceConfigs = InstanceConfigTest.getInstanceConfigs();
    for (InstanceConfig instanceConfig : instanceConfigs) {
      List<PropertyConfig> propertyConfigs = instanceConfig.getProperties();

      Assert.assertTrue(propertyConfigs.size() <= 2);
      Assert.assertTrue(propertyConfigs.get(0).getValue().startsWith("v1-"));

      if (propertyConfigs.size() == 2) {
        Assert.assertTrue(propertyConfigs.get(1).getValue().startsWith("v2-"));
      }
    }
  }
}
