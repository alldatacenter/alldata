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

package org.apache.ambari.server.controller.internal;


import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ivory.Instance;
import org.apache.ambari.server.controller.ivory.IvoryService;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for InstanceResourceProvider.
 */
public class InstanceResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<>();

    Map<String, Object> properties = new HashMap<>();

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.emptyMap());

    InstanceResourceProvider provider = new InstanceResourceProvider(service);

    try {
      provider.createResources(request);
      Assert.fail("Expected UnsupportedOperationException");

    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(service);
  }

  @Test
  public void testGetResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<>();

    Map<String, Object> properties = new HashMap<>();

    List<Instance> instances = new LinkedList<>();

    Instance instance1 = new Instance("Feed1", "Instance1", "s", "st", "et", "d", "l");
    Instance instance2 = new Instance("Feed1", "Instance2", "s", "st", "et", "d", "l");
    Instance instance3 = new Instance("Feed1", "Instance3", "s", "st", "et", "d", "l");

    instances.add(instance1);
    instances.add(instance2);
    instances.add(instance3);

    // set expectations
    expect(service.getFeedNames()).andReturn(Collections.singletonList("Feed1"));
    expect(service.getInstances("Feed1")).andReturn(instances);

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.emptyMap());

    InstanceResourceProvider provider = new InstanceResourceProvider(service);

    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(3, resources.size());

    // verify
    verify(service);
  }

  @Test
  public void testUpdateResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Set<Map<String, Object>> propertySet = new HashSet<>();

    Map<String, Object> properties = new HashMap<>();

    properties.put(InstanceResourceProvider.INSTANCE_FEED_NAME_PROPERTY_ID, "Feed1");
    properties.put(InstanceResourceProvider.INSTANCE_ID_PROPERTY_ID, "Instance1");
    properties.put(InstanceResourceProvider.INSTANCE_STATUS_PROPERTY_ID, "SUSPEND");
    properties.put(InstanceResourceProvider.INSTANCE_START_TIME_PROPERTY_ID, "ST");
    properties.put(InstanceResourceProvider.INSTANCE_END_TIME_PROPERTY_ID, "ET");
    properties.put(InstanceResourceProvider.INSTANCE_DETAILS_PROPERTY_ID, "DETAILS");
    properties.put(InstanceResourceProvider.INSTANCE_LOG_PROPERTY_ID, "log");

    List<Instance> instances = new LinkedList<>();

    // set expectations
    expect(service.getFeedNames()).andReturn(Collections.singletonList("Feed1"));
    expect(service.getInstances("Feed1")).andReturn(instances);

    // replay
    replay(service);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, Collections.emptyMap());

    InstanceResourceProvider provider = new InstanceResourceProvider(service);

    provider.updateResources(request, null);

    // verify
    verify(service);
  }

  @Test
  public void testDeleteResources() throws Exception {
    IvoryService service = createMock(IvoryService.class);

    Instance instance1 = new Instance("Feed1", "Instance1", "SUBMITTED", "start", "end", "details", "log");

    // set expectations
    expect(service.getFeedNames()).andReturn(Collections.singletonList("Feed1"));
    expect(service.getInstances("Feed1")).andReturn(Collections.singletonList(instance1));
    service.killInstance("Feed1", "Instance1");

    // replay
    replay(service);

    InstanceResourceProvider provider = new InstanceResourceProvider(service);

    Predicate predicate = new PredicateBuilder().property(InstanceResourceProvider.INSTANCE_ID_PROPERTY_ID).equals("Instance1").toPredicate();

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    // verify
    verify(service);
  }
}
