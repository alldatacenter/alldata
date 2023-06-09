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

package com.netease.arctic.utils;

import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CompatiblePropertyUtilTest {

  @Test
  public void testGetNewProperty() {
    Map<String, String> properties = Maps.newHashMap();
    Assert.assertEquals(
        TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT,
        CompatiblePropertyUtil.propertyAsBoolean(properties, TableProperties.ENABLE_SELF_OPTIMIZING,
            TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));

    properties.put(TableProperties.ENABLE_SELF_OPTIMIZING, "false");
    Assert.assertEquals(
        false,
        CompatiblePropertyUtil.propertyAsBoolean(properties, TableProperties.ENABLE_SELF_OPTIMIZING,
            TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));

    properties.put(TableProperties.ENABLE_OPTIMIZE, "true");
    Assert.assertEquals(
        false,
        CompatiblePropertyUtil.propertyAsBoolean(properties, TableProperties.ENABLE_SELF_OPTIMIZING,
            TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }

  @Test
  public void testGetLegacyProperty() {
    Map<String, String> properties = Maps.newHashMap();
    properties.put(TableProperties.ENABLE_OPTIMIZE, "false");
    Assert.assertEquals(
        false,
        CompatiblePropertyUtil.propertyAsBoolean(properties, TableProperties.ENABLE_SELF_OPTIMIZING,
            TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }
}