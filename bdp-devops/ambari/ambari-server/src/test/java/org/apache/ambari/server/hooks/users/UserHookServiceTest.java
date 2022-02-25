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

package org.apache.ambari.server.hooks.users;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.hooks.AmbariEventFactory;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class UserHookServiceTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private AmbariEventFactory eventFactoryMock;

  @Mock
  private AmbariEventPublisher ambariEventPublisherMock;

  @Mock
  private ActionManager actionManagerMock;

  @Mock
  private RequestFactory requestFactoryMock;

  @Mock
  private StageFactory stageFactoryMock;

  @Mock
  private Configuration configurationMock;

  @Mock
  private Clusters clustersMock;

  @Mock
  private ObjectMapper objectMapperMock;

  @Mock
  private Map<String, Cluster> clustersMap;

  @Mock
  private Cluster clusterMock;

  @Mock
  private Stage stageMock;

  @Mock
  private Config configMock;

  @TestSubject
  private UserHookService hookService = new UserHookService();


  private HookContext hookContext;
  private Map<String, Set<String>> usersToGroups;
  private UserCreatedEvent userCreatedEvent;

  @Before
  public void before() throws Exception {
    usersToGroups = new HashMap<>();
    usersToGroups.put("testUser", new HashSet<>(Arrays.asList("hdfs", "yarn")));
    hookContext = new PostUserCreationHookContext(usersToGroups);

    userCreatedEvent = new UserCreatedEvent(hookContext);

    resetAll();

  }

  @Test
  public void shouldServiceQuitWhenFeatureIsDisabled() {
    // GIVEN
    EasyMock.expect(configurationMock.isUserHookEnabled()).andReturn(Boolean.FALSE);
    replayAll();

    // WHEN
    Boolean triggered = hookService.execute(hookContext);

    //THEN
    Assert.assertFalse("The hook must not be triggered if feature is disabled!", triggered);

  }


  @Test
  public void shouldServiceQuitWhenClusterDoesNotExist() {
    // GIVEN
    EasyMock.expect(configurationMock.isUserHookEnabled()).andReturn(Boolean.TRUE);
    EasyMock.expect(clustersMap.isEmpty()).andReturn(Boolean.TRUE);
    EasyMock.expect(clustersMock.getClusters()).andReturn(clustersMap);


    replayAll();

    // WHEN
    Boolean triggered = hookService.execute(hookContext);

    //THEN
    Assert.assertFalse("The hook must not be triggered if there's no cluster!", triggered);

  }


  @Test
  public void shouldServiceQuitWhenCalledWithEmptyContext() {
    // GIVEN
    EasyMock.expect(configurationMock.isUserHookEnabled()).andReturn(Boolean.TRUE);
    EasyMock.expect(clustersMap.isEmpty()).andReturn(Boolean.FALSE);
    EasyMock.expect(clustersMock.getClusters()).andReturn(clustersMap);

    replayAll();

    // WHEN
    Boolean triggered = hookService.execute(new PostUserCreationHookContext(Collections.emptyMap()));

    //THEN
    Assert.assertFalse("The hook should not be triggered if there is no users in the context!", triggered);

  }


  @Test
  public void shouldServiceTriggerHookWhenPrerequisitesAreSatisfied() {
    // GIVEN
    EasyMock.expect(configurationMock.isUserHookEnabled()).andReturn(Boolean.TRUE);
    EasyMock.expect(clustersMap.isEmpty()).andReturn(Boolean.FALSE);
    EasyMock.expect(clustersMock.getClusters()).andReturn(clustersMap);

    Capture<HookContext> contextCapture = EasyMock.newCapture();
    EasyMock.expect(eventFactoryMock.newUserCreatedEvent(EasyMock.capture(contextCapture))).andReturn(userCreatedEvent);

    Capture<UserCreatedEvent> userCreatedEventCapture = EasyMock.newCapture();
    ambariEventPublisherMock.publish(EasyMock.capture(userCreatedEventCapture));

    replayAll();

    // WHEN
    Boolean triggered = hookService.execute(hookContext);

    //THEN
    Assert.assertTrue("The hook must be triggered if prerequisites satisfied!", triggered);
    Assert.assertEquals("The hook context the event is generated from is not as expected ", hookContext, contextCapture.getValue());
    Assert.assertEquals("The user created event is not the expected ", userCreatedEvent, userCreatedEventCapture.getValue());

  }

  @Test
  public void shouldCommandParametersBeSet() throws Exception {
    // GIVEN
    Map<String, Cluster> clsMap = new HashMap<>();
    clsMap.put("test-cluster", clusterMock);

    Map<String, String> configMap = new HashMap<>();
    configMap.put("hdfs_user", "hdfs-test-user");

    EasyMock.expect(clusterMock.getClusterId()).andReturn(1l);
    EasyMock.expect(clusterMock.getClusterName()).andReturn("test-cluster");
    EasyMock.expect(clusterMock.getSecurityType()).andReturn(SecurityType.NONE).times(3);
    EasyMock.expect(clusterMock.getDesiredConfigByType("hadoop-env")).andReturn(configMock);
    EasyMock.expect(configMock.getProperties()).andReturn(configMap);


    EasyMock.expect(actionManagerMock.getNextRequestId()).andReturn(1l);
    EasyMock.expect(clustersMock.getClusters()).andReturn(clsMap);
    EasyMock.expect(configurationMock.getServerTempDir()).andReturn("/var/lib/ambari-server/tmp").times(2);
    EasyMock.expect(configurationMock.getProperty(Configuration.POST_USER_CREATION_HOOK)).andReturn("/var/lib/ambari-server/resources/scripts/post-user-creation-hook.sh").anyTimes();
    EasyMock.expect(objectMapperMock.writeValueAsString(((PostUserCreationHookContext) userCreatedEvent.getContext()).getUserGroups())).andReturn("{testUser=[hdfs, yarn]}");
    stageMock.setStageId(-1);

    // TBD refine expectations to validate the logic / eg capture arguments
    stageMock.addServerActionCommand(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject(Role.class), EasyMock.anyObject(RoleCommand.class), EasyMock.anyString(), EasyMock.anyObject(ServiceComponentHostServerActionEvent.class),
        EasyMock.anyObject(), EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyInt(), EasyMock.anyBoolean(), EasyMock.anyBoolean());
    EasyMock.expect(requestFactoryMock.createNewFromStages(Arrays.asList(stageMock), "{}")).andReturn(null);
    EasyMock.expect(stageFactoryMock.createNew(1, "/var/lib/ambari-server/tmp:1", "test-cluster", 1, "Post user creation hook for [ 1 ] users", "{}", "{}")).andReturn(stageMock);


    replayAll();

    //WHEN
    hookService.onUserCreatedEvent(userCreatedEvent);

    //THEN
    // TBD assertions on the captured arguments!

  }
}
