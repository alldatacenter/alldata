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

package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.Collections;

import org.apache.ambari.server.events.AmbariEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTask;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit test for the ConfigureClusterTask class.
 * As business methods of this class don't return values, the assertions are made by verifying method calls on mocks.
 * Thus having strict mocks is essential!
 */
public class ConfigureClusterTaskTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private ClusterConfigurationRequest clusterConfigurationRequest;

  @Mock(type = MockType.STRICT)
  private ClusterTopology clusterTopology;

  @Mock(type = MockType.STRICT)
  private AmbariContext ambariContext;

  @Mock(type = MockType.NICE)
  private AmbariEventPublisher ambariEventPublisher;

  private ConfigureClusterTask testSubject;

  @Before
  public void before() {
    resetAll();
    testSubject = new ConfigureClusterTask(clusterTopology, clusterConfigurationRequest, ambariEventPublisher);
  }

  @Test
  public void taskShouldBeExecutedIfRequiredHostgroupsAreResolved() throws Exception {
    // GIVEN
    expect(clusterConfigurationRequest.getRequiredHostGroups()).andReturn(Collections.emptyList());
    expect(clusterTopology.getHostGroupInfo()).andReturn(Collections.emptyMap());
    expect(clusterTopology.getClusterId()).andReturn(1L).anyTimes();
    expect(clusterTopology.getAmbariContext()).andReturn(ambariContext);
    expect(ambariContext.getClusterName(1L)).andReturn("testCluster");
    clusterConfigurationRequest.process();
    ambariEventPublisher.publish(anyObject(AmbariEvent.class));
    replayAll();

    // WHEN
    Boolean result = testSubject.call();

    // THEN
    verifyAll();
    Assert.assertTrue(result);
  }

  @Test
  public void testsShouldConfigureClusterTaskExecuteWhenCalledFromAsyncCallableService() throws Exception {
    // GIVEN
    expect(clusterConfigurationRequest.getRequiredHostGroups()).andReturn(Collections.emptyList());
    expect(clusterTopology.getHostGroupInfo()).andReturn(Collections.emptyMap());
    clusterConfigurationRequest.process();
    replayAll();

    AsyncCallableService<Boolean> asyncService = new AsyncCallableService<>(testSubject, 5000, 500, "test", t -> {});

    // WHEN
    asyncService.call();

    // THEN
    verifyAll();
  }

}