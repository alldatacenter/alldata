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

package org.apache.ambari.server.api.services.stackadvisor;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.partialMockBuilder;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ComponentLayoutValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationDependenciesRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationRecommendationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.ConfigurationValidationCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;

/**
 * StackAdvisorHelper unit tests.
 */
public class StackAdvisorHelperTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testValidate_returnsCommandResult() throws StackAdvisorException, IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<ValidationResponse> command = mock(StackAdvisorCommand.class);
    ValidationResponse expected = mock(ValidationResponse.class);

    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenReturn(expected);
    doReturn(command).when(helper).createValidationCommand("ZOOKEEPER", request);
    ValidationResponse response = helper.validate(request);
    assertEquals(expected, response);
  }

  @Test(expected = StackAdvisorException.class)
  @SuppressWarnings("unchecked")
  public void testValidate_commandThrowsException_throwsException() throws StackAdvisorException,
      IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<ValidationResponse> command = mock(StackAdvisorCommand.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenThrow(new StackAdvisorException("message"));
    doReturn(command).when(helper).createValidationCommand("ZOOKEEPER", request);
    helper.validate(request);

    fail();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRecommend_returnsCommandResult() throws StackAdvisorException, IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<RecommendationResponse> command = mock(StackAdvisorCommand.class);
    RecommendationResponse expected = mock(RecommendationResponse.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenReturn(expected);
    doReturn(command).when(helper).createRecommendationCommand("ZOOKEEPER", request);
    RecommendationResponse response = helper.recommend(request);

    assertEquals(expected, response);
  }

  @Test(expected = StackAdvisorException.class)
  @SuppressWarnings("unchecked")
  public void testRecommend_commandThrowsException_throwsException() throws StackAdvisorException,
      IOException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = stackAdvisorHelperSpy(configuration, saRunner, metaInfo);

    StackAdvisorCommand<RecommendationResponse> command = mock(StackAdvisorCommand.class);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    when(command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON)).thenThrow(new StackAdvisorException("message"));
    doReturn(command).when(helper).createRecommendationCommand("ZOOKEEPER", request);
    helper.recommend(request);

    fail("Expected StackAdvisorException to be thrown");
  }

  @Test
  public void testCreateRecommendationCommand_returnsComponentLayoutRecommendationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<RecommendationResponse> command = helper
        .createRecommendationCommand("ZOOKEEPER", request);

    assertEquals(ComponentLayoutRecommendationCommand.class, command.getClass());
  }

  @Test
  public void testCreateRecommendationCommand_returnsConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS);
  }

  @Test
  public void testCreateRecommendationCommand_returnsSingleSignOnConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.SSO_CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_SSO);
  }

  @Test
  public void testCreateRecommendationCommand_returnsLDAPConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.LDAP_CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_LDAP);
  }

  @Test
  public void testCreateRecommendationCommand_returnsKerberosConfigurationRecommendationCommand() throws IOException, StackAdvisorException {
    testCreateConfigurationRecommendationCommand(StackAdvisorRequestType.KERBEROS_CONFIGURATIONS, StackAdvisorCommandType.RECOMMEND_CONFIGURATIONS_FOR_KERBEROS);
  }

  @Test
  public void testCreateValidationCommand_returnsComponentLayoutValidationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.HOST_GROUPS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<ValidationResponse> command = helper.createValidationCommand("ZOOKEEPER", request);

    assertEquals(ComponentLayoutValidationCommand.class, command.getClass());
  }

  @Test
  public void testCreateValidationCommand_returnsConfigurationValidationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.CONFIGURATIONS;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<ValidationResponse> command = helper.createValidationCommand("ZOOKEEPER", request);

    assertEquals(ConfigurationValidationCommand.class, command.getClass());
  }

  @Test
  public void testCreateRecommendationDependencyCommand_returnsConfigurationDependencyRecommendationCommand()
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null, null);
    StackAdvisorRequestType requestType = StackAdvisorRequestType.CONFIGURATION_DEPENDENCIES;
    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<RecommendationResponse> command = helper.createRecommendationCommand("ZOOKEEPER", request);

    assertEquals(ConfigurationDependenciesRecommendationCommand.class, command.getClass());
  }

  @Test
  public void testClearCacheAndHost() throws IOException, NoSuchFieldException, IllegalAccessException {
    Field hostInfoCacheField = StackAdvisorHelper.class.getDeclaredField("hostInfoCache");
    Field configsRecommendationResponseField = StackAdvisorHelper.class.getDeclaredField("configsRecommendationResponse");
    StackAdvisorHelper helper = testClearCachesSetup(hostInfoCacheField, configsRecommendationResponseField);

    helper.clearCaches("hostName1");

    Map<String, JsonNode> hostInfoCache = (Map<String, JsonNode>) hostInfoCacheField.get(helper);
    Map<String, RecommendationResponse> configsRecommendationResponse =
        (Map<String, RecommendationResponse>) configsRecommendationResponseField.get(helper);

    assertEquals(2, hostInfoCache.size());
    assertTrue(hostInfoCache.containsKey("hostName2"));
    assertTrue(hostInfoCache.containsKey("hostName3"));
    assertTrue(configsRecommendationResponse.isEmpty());
  }

  @Test
  public void testClearCacheAndHosts() throws IOException, NoSuchFieldException, IllegalAccessException {

    Field hostInfoCacheField = StackAdvisorHelper.class.getDeclaredField("hostInfoCache");
    Field configsRecommendationResponseField = StackAdvisorHelper.class.getDeclaredField("configsRecommendationResponse");
    StackAdvisorHelper helper = testClearCachesSetup(hostInfoCacheField, configsRecommendationResponseField);

    helper.clearCaches(new HashSet<>(Arrays.asList(new String[]{"hostName1", "hostName2"})));

    Map<String, JsonNode> hostInfoCache = (Map<String, JsonNode>) hostInfoCacheField.get(helper);
    Map<String, RecommendationResponse> configsRecommendationResponse =
        (Map<String, RecommendationResponse>) configsRecommendationResponseField.get(helper);

    assertEquals(1, hostInfoCache.size());
    assertTrue(hostInfoCache.containsKey("hostName3"));
    assertTrue(configsRecommendationResponse.isEmpty());
  }

  @Test
  public void testCacheRecommendations() throws IOException, StackAdvisorException {
    Configuration configuration = createNiceMock(Configuration.class);
    StackAdvisorRunner stackAdvisorRunner = createNiceMock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    AmbariServerConfigurationHandler ambariServerConfigurationHandler = createNiceMock(AmbariServerConfigurationHandler.class);

    expect(configuration.getRecommendationsArtifactsRolloverMax()).andReturn(1);

    replay(configuration, stackAdvisorRunner, ambariMetaInfo, ambariServerConfigurationHandler);

    StackAdvisorHelper helper = partialMockBuilder(StackAdvisorHelper.class).withConstructor(Configuration.class,
        StackAdvisorRunner.class, AmbariMetaInfo.class, AmbariServerConfigurationHandler.class, Gson.class).
        withArgs(configuration, stackAdvisorRunner, ambariMetaInfo, ambariServerConfigurationHandler, new Gson()).
        addMockedMethod("createRecommendationCommand").
        createMock();

    verify(configuration, stackAdvisorRunner, ambariMetaInfo, ambariServerConfigurationHandler);
    reset(ambariMetaInfo);

    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.setServiceAdvisorType(ServiceInfo.ServiceAdvisorType.PYTHON);
    expect(ambariMetaInfo.getService(anyString(), anyString(), anyString())).andReturn(serviceInfo).atLeastOnce();

    ConfigurationRecommendationCommand command = createMock(ConfigurationRecommendationCommand.class);

    StackAdvisorRequest request = StackAdvisorRequestBuilder.
        forStack(null, null).ofType(StackAdvisorRequestType.CONFIGURATIONS).
        build();

    expect(helper.createRecommendationCommand(eq("ZOOKEEPER"), eq(request))).andReturn(command).times(2);

    // populate response with dummy info to check equivalence
    RecommendationResponse response = new RecommendationResponse();
    response.setServices(new HashSet<String>(){{add("service1"); add("service2"); add("service3");}});

    // invoke() should be fired for first recommend() execution only
    expect(command.invoke(eq(request), eq(ServiceInfo.ServiceAdvisorType.PYTHON))).andReturn(response).once();

    replay(ambariMetaInfo, helper, command);

    RecommendationResponse calculatedResponse = helper.recommend(request);
    RecommendationResponse cachedResponse = helper.recommend(request);

    verify(ambariMetaInfo, helper, command);

    assertEquals(response.getServices(), calculatedResponse.getServices());
    assertEquals(response.getServices(), cachedResponse.getServices());
  }

  private StackAdvisorHelper testClearCachesSetup(Field hostInfoCacheField,
                                                  Field configsRecommendationResponseField) throws IOException,
      NoSuchFieldException, IllegalAccessException {
    Configuration configuration = createNiceMock(Configuration.class);
    StackAdvisorRunner stackAdvisorRunner = createNiceMock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    AmbariServerConfigurationHandler ambariServerConfigurationHandler = createNiceMock(AmbariServerConfigurationHandler.class);

    replay(configuration, stackAdvisorRunner, ambariMetaInfo, ambariServerConfigurationHandler);

    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, stackAdvisorRunner,
        ambariMetaInfo, ambariServerConfigurationHandler, null);

    verify(configuration, stackAdvisorRunner, ambariMetaInfo, ambariServerConfigurationHandler);

    hostInfoCacheField.setAccessible(true);
    configsRecommendationResponseField.setAccessible(true);

    Map<String, JsonNode> hostInfoCache = new ConcurrentHashMap<>();
    JsonNodeFactory jnf = JsonNodeFactory.instance;
    hostInfoCache.put("hostName1", jnf.nullNode());
    hostInfoCache.put("hostName2", jnf.nullNode());
    hostInfoCache.put("hostName3", jnf.nullNode());
    Map<String, RecommendationResponse> configsRecommendationResponse = new ConcurrentHashMap<>();
    configsRecommendationResponse.put("111", new RecommendationResponse());
    configsRecommendationResponse.put("222", new RecommendationResponse());

    hostInfoCacheField.set(helper, hostInfoCache);
    configsRecommendationResponseField.set(helper, configsRecommendationResponse);
    return helper;
  }

  private void testCreateConfigurationRecommendationCommand(StackAdvisorRequestType requestType, StackAdvisorCommandType expectedCommandType)
      throws IOException, StackAdvisorException {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRecommendationsArtifactsRolloverMax()).thenReturn(100);
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    ServiceInfo service = mock(ServiceInfo.class);
    when(metaInfo.getService(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(service);
    when(service.getServiceAdvisorType()).thenReturn(ServiceInfo.ServiceAdvisorType.PYTHON);
    StackAdvisorHelper helper = new StackAdvisorHelper(configuration, saRunner, metaInfo, null, null);

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .ofType(requestType).build();

    StackAdvisorCommand<RecommendationResponse> command = helper.createRecommendationCommand("ZOOKEEPER", request);

    assertTrue(command instanceof ConfigurationRecommendationCommand);
    assertEquals(expectedCommandType, ((ConfigurationRecommendationCommand) command).getCommandType());

  }

  private static StackAdvisorHelper stackAdvisorHelperSpy(Configuration configuration, StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo) throws IOException {
    return spy(new StackAdvisorHelper(configuration, saRunner, metaInfo, null, null));
  }
}
