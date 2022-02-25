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

package org.apache.ambari.server.collections.functors;


import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import junit.framework.Assert;

public class ContextTransformerTest extends EasyMockSupport {

  @Test
  public void testGetKey() {
    ContextTransformer transformer = new ContextTransformer("key");
    Assert.assertEquals("key", transformer.getKey());
  }

  @Test
  public void testTransformSimple() {
    Map<String, Object> context = new HashMap<>();
    context.put("key", "value");
    context.put("key1", "value1");
    context.put("key2", "value2");

    ContextTransformer transformer = new ContextTransformer("key");
    Assert.assertEquals("value", transformer.transform(context));
  }

  @Test
  public void testTransformTree() {
    Map<String, Object> serviceSite = new HashMap<>();
    serviceSite.put("property", "service-site-property");

    Map<String, Object> configurations = new HashMap<>();
    configurations.put("service-site", serviceSite);
    configurations.put("property", "configuration-property");

    Map<String, Object> context = new HashMap<>();
    context.put("configurations", configurations);
    context.put("property", "context-property");

    ContextTransformer transformer;

    // Without leading "/"
    transformer = new ContextTransformer("configurations/service-site/property");
    Assert.assertEquals("service-site-property", transformer.transform(context));

    // With leading "/"
    transformer = new ContextTransformer("/configurations/service-site/property");
    Assert.assertEquals("service-site-property", transformer.transform(context));

    // Get map of properties
    transformer = new ContextTransformer("/configurations/service-site");
    Assert.assertEquals(serviceSite, transformer.transform(context));
  }
}