/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.state;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class ServiceComponentSupportTest extends EasyMockSupport {
  private static final String STACK_NAME = "HDP";
  private static final String VERSION = "3.0";
  @Mock
  private AmbariMetaInfo ambariMetaInfo;
  private ServiceComponentSupport componentSupport;

  @Before
  public void setUp() throws Exception {
    componentSupport = new ServiceComponentSupport(() -> ambariMetaInfo);
  }

  @Test
  public void testNoUnsupportedIfAllExistsInTargetStack() throws Exception {
    targetStackWith("SERVICE1", "SERVICE2");
    Set<String> unsupported = unsupportedServices(clusterWith("SERVICE1", "SERVICE2"));
    assertThat(unsupported, hasSize(0));
    verifyAll();
  }

  @Test
  public void testUnsupportedIfDoesntExistInTargetStack() throws Exception {
    targetStackWith("SERVICE1");
    Set<String> unsupported = unsupportedServices(clusterWith("SERVICE1", "SERVICE2"));
    assertThat(unsupported, hasOnlyItems(is("SERVICE2")));
    verifyAll();
  }

  @Test
  public void testUnsupportedIfDeletedFromTargetStack() throws Exception {
    targetStackWith("SERVICE1", "SERVICE2");
    markAsDeleted("SERVICE2");
    Set<String> unsupported = unsupportedServices(clusterWith("SERVICE1", "SERVICE2"));
    assertThat(unsupported, hasOnlyItems(is("SERVICE2")));
    verifyAll();
  }

  private void targetStackWith(String... serviceNames) throws AmbariException {
    expect(ambariMetaInfo.getServices(STACK_NAME, VERSION)).andReturn(serviceInfoMap(serviceNames)).anyTimes();
    replay(ambariMetaInfo);
  }

  private Map<String, ServiceInfo> serviceInfoMap(String... serviceNames) {
    Map<String, ServiceInfo> serviceInfoMap = new HashMap<>();
    for (String serviceName : serviceNames) {
      serviceInfoMap.put(serviceName, new ServiceInfo());
    }
    return serviceInfoMap;
  }

  private Set<String> unsupportedServices(Cluster cluster) {
    return componentSupport.unsupportedServices(cluster, STACK_NAME, VERSION);
  }

  private Cluster clusterWith(String... installedServiceNames) {
    Cluster cluster = mock(Cluster.class);
    Map<String, Service> serviceMap = new HashMap<>();
    for (String serviceName : installedServiceNames) {
      serviceMap.put(serviceName, null);
    }
    expect(cluster.getServices()).andReturn(serviceMap);
    replay(cluster);
    return cluster;
  }

  private void markAsDeleted(String serviceName) throws AmbariException {
    ambariMetaInfo.getServices(STACK_NAME, VERSION).get(serviceName).setDeleted(true);
  }

  private static Matcher hasOnlyItems(Matcher... matchers) {
    return allOf(hasSize(matchers.length), hasItems(matchers));
  }
}