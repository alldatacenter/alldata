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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequestException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorResponse;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommand.StackAdvisorData;
import org.apache.ambari.server.controller.internal.AmbariServerConfigurationHandler;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

/**
 * StackAdvisorCommand unit tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class StackAdvisorCommandTest {
  private static final String SINGLE_HOST_RESPONSE = "{\"href\":\"/api/v1/hosts?fields=Hosts/*&Hosts/host_name.in(%1$s)\",\"items\":[{\"href\":\"/api/v1/hosts/%1$s\",\"Hosts\":{\"host_name\":\"%1$s\"}}]}";
  private static final String TWO_HOST_RESPONSE = "{\"href\":\"/api/v1/hosts?fields=Hosts/*&Hosts/host_name.in(%1$s,%2$s)\",\"items\":[{\"href\":\"/api/v1/hosts/%1$s\",\"Hosts\":{\"host_name\":\"%1$s\"}},{\"href\":\"/api/v1/hosts/%2$s\",\"Hosts\":{\"host_name\":\"%2$s\"}}]}";

  private TemporaryFolder temp = new TemporaryFolder();
  @Mock
  AmbariServerConfigurationHandler ambariServerConfigurationHandler;

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test(expected = StackAdvisorException.class)
  public void testInvoke_invalidRequest_throwsException() throws StackAdvisorException {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo, null));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    doThrow(new StackAdvisorException("message")).when(command).validate(request);
    command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON);

    assertTrue(false);
  }

  @Test(expected = StackAdvisorException.class)
  public void testInvoke_saRunnerNotSucceed_throwsException() throws StackAdvisorException {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo, null));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    String hostsJSON = "{\"hosts\" : \"localhost\"";
    String servicesJSON = "{\"services\" : \"HDFS\"";
    StackAdvisorData data = new StackAdvisorData(hostsJSON, servicesJSON);
    doReturn(hostsJSON).when(command).getHostsInformation(request);
    doReturn(servicesJSON).when(command).getServicesInformation(request);
    doReturn(data).when(command)
        .adjust(any(StackAdvisorData.class), any(StackAdvisorRequest.class));

    doThrow(new StackAdvisorRequestException("error")).when(saRunner)
        .runScript(any(ServiceInfo.ServiceAdvisorType.class), any(StackAdvisorCommandType.class), any(File.class));
    command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON);
    assertTrue(false);
  }

  @Test(expected = WebApplicationException.class)
  public void testInvoke_adjustThrowsException_throwsException() throws StackAdvisorException {
    File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    int requestId = 0;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(recommendationsDir, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, requestId, saRunner, metaInfo, null));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    doReturn("{\"hosts\" : \"localhost\"").when(command).getHostsInformation(request);
    doReturn("{\"services\" : \"HDFS\"").when(command).getServicesInformation(request);
    doThrow(new WebApplicationException()).when(command).adjust(any(StackAdvisorData.class),
        any(StackAdvisorRequest.class));

    doThrow(new StackAdvisorException("error")).when(saRunner)
        .runScript(any(ServiceInfo.ServiceAdvisorType.class), any(StackAdvisorCommandType.class), any(File.class));
    command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON);


    assertTrue(false);
  }

  @Test
  public void testInvoke_success() throws StackAdvisorException {
    String expected = "success";
    final String testResourceString = String.format("{\"type\": \"%s\"}", expected);
    final File recommendationsDir = temp.newFolder("recommendationDir");
    String recommendationsArtifactsLifetime = "1w";
    final int requestId = 2;
    StackAdvisorRunner saRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);
    doReturn(Collections.emptyList()).when(metaInfo).getStackParentVersions(anyString(), anyString());
    final StackAdvisorCommand<TestResource> command = spy(new TestStackAdvisorCommand(
        recommendationsDir, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, requestId,
        saRunner, metaInfo, null));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.forStack("stackName", "stackVersion")
        .build();

    String hostsJSON = "{\"hosts\" : \"localhost\"";
    String servicesJSON = "{\"services\" : \"HDFS\"";
    StackAdvisorData data = new StackAdvisorData(hostsJSON, servicesJSON);
    doReturn(hostsJSON).when(command).getHostsInformation(request);
    doReturn(servicesJSON).when(command).getServicesInformation(request);
    doReturn(data).when(command)
        .adjust(any(StackAdvisorData.class), any(StackAdvisorRequest.class));

    doAnswer(invocation -> {
      String resultFilePath = String.format("%s/%s", requestId, command.getResultFileName());
      File resultFile = new File(recommendationsDir, resultFilePath);
      resultFile.getParentFile().mkdirs();
      FileUtils.writeStringToFile(resultFile, testResourceString, Charset.defaultCharset());
      return null;
    }).when(saRunner).runScript(any(ServiceInfo.ServiceAdvisorType.class),
            any(StackAdvisorCommandType.class), any(File.class));

    TestResource result = command.invoke(request, ServiceInfo.ServiceAdvisorType.PYTHON);

    assertEquals(expected, result.getType());
    assertEquals(requestId, result.getId());
  }

  @Test
  public void testPopulateStackHierarchy() throws Exception {
    File file = mock(File.class);
    String recommendationsArtifactsLifetime = "1w";
    StackAdvisorRunner stackAdvisorRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorCommand<TestResource> cmd = new TestStackAdvisorCommand(file, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, 1, stackAdvisorRunner, ambariMetaInfo, null);
    ObjectNode objectNode = (ObjectNode) cmd.mapper.readTree("{\"Versions\": " +
        "{\"stack_name\": \"stack\", \"stack_version\":\"1.0.0\"}}");

    doReturn(Arrays.asList("0.9", "0.8")).when(ambariMetaInfo).getStackParentVersions("stack", "1.0.0");

    cmd.populateStackHierarchy(objectNode);

    JsonNode stackHierarchy = objectNode.get("Versions").get("stack_hierarchy");
    assertNotNull(stackHierarchy);
    JsonNode stackName = stackHierarchy.get("stack_name");
    assertNotNull(stackName);
    assertEquals("stack", stackName.asText());
    ArrayNode stackVersions = (ArrayNode) stackHierarchy.get("stack_versions");
    assertNotNull(stackVersions);
    assertEquals(2, stackVersions.size());
    Iterator<JsonNode> stackVersionsElements = stackVersions.getElements();
    assertEquals("0.9", stackVersionsElements.next().asText());
    assertEquals("0.8", stackVersionsElements.next().asText());
  }

  @Test
  public void testPopulateAmbariServerProperties() throws Exception {
    File file = mock(File.class);
    String recommendationsArtifactsLifetime = "1w";
    StackAdvisorRunner stackAdvisorRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorCommand<TestResource> cmd = new TestStackAdvisorCommand(file, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, 1,
      stackAdvisorRunner, ambariMetaInfo, null);
    ObjectNode objectNode = (ObjectNode) cmd.mapper.readTree("{\"Versions\": " +
      "{\"stack_name\": \"stack\", \"stack_version\":\"1.0.0\"}}");

    Map<String, String> props = Collections.singletonMap("a", "b");

    doReturn(props).when(ambariMetaInfo).getAmbariServerProperties();

    cmd.populateAmbariServerInfo(objectNode);

    JsonNode serverProperties = objectNode.get("ambari-server-properties");
    assertNotNull(serverProperties);
    assertEquals("b", serverProperties.iterator().next().getTextValue());
  }

  @Test
  public void testPopulateStackHierarchy_noParents() throws Exception {
    File file = mock(File.class);
    String recommendationsArtifactsLifetime = "1w";
    StackAdvisorRunner stackAdvisorRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
    StackAdvisorCommand<TestResource> cmd = new TestStackAdvisorCommand(file, recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType.PYTHON, 1,
        stackAdvisorRunner, ambariMetaInfo, null);
    ObjectNode objectNode = (ObjectNode) cmd.mapper.readTree("{\"Versions\": " +
        "{\"stack_name\": \"stack\", \"stack_version\":\"1.0.0\"}}");

    doReturn(Collections.emptyList()).when(ambariMetaInfo).getStackParentVersions("stack", "1.0.0");

    cmd.populateStackHierarchy(objectNode);

    JsonNode stackHierarchy = objectNode.get("Versions").get("stack_hierarchy");
    assertNotNull(stackHierarchy);
    JsonNode stackName = stackHierarchy.get("stack_name");
    assertNotNull(stackName);
    assertEquals("stack", stackName.asText());
    ArrayNode stackVersions = (ArrayNode) stackHierarchy.get("stack_versions");
    assertNotNull(stackVersions);
    assertEquals(0, stackVersions.size());
  }

  @Test
  public void testPopulateLdapConfig() throws Exception {
    Map<String, Map<String, String>> storedConfig = Collections.singletonMap("ldap-configuration",
        Collections.singletonMap("authentication.ldap.secondaryUrl", "localhost:333"));
    TestStackAdvisorCommand command = new TestStackAdvisorCommand(
      temp.newFolder("recommendationDir"),
      "1w",
      ServiceInfo.ServiceAdvisorType.PYTHON,
      0,
      mock(StackAdvisorRunner.class),
      mock(AmbariMetaInfo.class), null);
    when(ambariServerConfigurationHandler.getConfigurations()).thenReturn(storedConfig);
    JsonNode servicesRootNode = json("{}");
    command.populateAmbariConfiguration((ObjectNode)servicesRootNode);
    JsonNode expectedLdapConfig = json("{\"ambari-server-configuration\":{\"ldap-configuration\":{\"authentication.ldap.secondaryUrl\":\"localhost:333\"}}}");
    assertEquals(expectedLdapConfig, servicesRootNode);
  }

  @Test
  public void testPopulateLdapConfig_NoConfigs() throws Exception {
    TestStackAdvisorCommand command = new TestStackAdvisorCommand(
      temp.newFolder("recommendationDir"),
      "1w",
      ServiceInfo.ServiceAdvisorType.PYTHON,
      0,
      mock(StackAdvisorRunner.class),
      mock(AmbariMetaInfo.class), null);
    when(ambariServerConfigurationHandler.getConfigurations()).thenReturn(emptyMap());
    JsonNode servicesRootNode = json("{}");
    command.populateAmbariConfiguration((ObjectNode)servicesRootNode);
    JsonNode expectedLdapConfig = json("{\"ambari-server-configuration\":{}}");
    assertEquals(expectedLdapConfig, servicesRootNode);
  }

  /**
   * Try to retrieve host info twice. The inner cache should be populated with first usage (with handleRequest method calling).
   * And for next info retrieving for the same host the saved value should be used.
   */
  @Test
  public void testHostInfoCachingSingleHost() throws StackAdvisorException {
    File file = mock(File.class);
    String recommendationsArtifactsLifetime = "1w";
    StackAdvisorRunner stackAdvisorRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
    Map<String, JsonNode> hostInfoCache = new HashMap<>();
    TestStackAdvisorCommand command = spy(new TestStackAdvisorCommand(file, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, 1,
        stackAdvisorRunner, ambariMetaInfo, hostInfoCache));

    // in second handling case NPE will be fired during result processing
    doReturn(Response.status(200).entity(String.format(SINGLE_HOST_RESPONSE, "hostName1")).build())
        .doReturn(null)
        .when(command).handleRequest(any(HttpHeaders.class), any(String.class), any(UriInfo.class), any(Request.Type.class),
        any(MediaType.class), any(ResourceInstance.class));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.
        forStack(null, null).ofType(StackAdvisorRequest.StackAdvisorRequestType.CONFIGURATIONS).
        forHosts(Arrays.asList(new String[]{"hostName1"})).
        build();
    String firstResponse = command.getHostsInformation(request);
    assertEquals(String.format(SINGLE_HOST_RESPONSE, "hostName1"), firstResponse);

    String secondResponse = command.getHostsInformation(request);
    assertEquals(String.format(SINGLE_HOST_RESPONSE, "hostName1"), secondResponse);
  }

  /**
   * Try to retrieve multiple hosts info twice. The inner cache should be populated with first usage for first host (hostName1).
   * For the next usage with the both hosts handleRequest should be used for second host only.
   */
  @Test
  public void testHostInfoCachingTwoHost() throws StackAdvisorException {
    File file = mock(File.class);
    String recommendationsArtifactsLifetime = "1w";
    StackAdvisorRunner stackAdvisorRunner = mock(StackAdvisorRunner.class);
    AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
    Map<String, JsonNode> hostInfoCache = new HashMap<>();
    TestStackAdvisorCommand command = spy(new TestStackAdvisorCommand(file, recommendationsArtifactsLifetime,
        ServiceInfo.ServiceAdvisorType.PYTHON, 1,
        stackAdvisorRunner, ambariMetaInfo, hostInfoCache));

    doReturn(Response.status(200).entity(String.format(SINGLE_HOST_RESPONSE, "hostName1")).build())
        .doReturn(Response.status(200).entity(String.format(SINGLE_HOST_RESPONSE, "hostName2")).build())
        .doReturn(null)
        .when(command).handleRequest(any(HttpHeaders.class), any(String.class), any(UriInfo.class), any(Request.Type.class),
        any(MediaType.class), any(ResourceInstance.class));

    StackAdvisorRequest request = StackAdvisorRequestBuilder.
        forStack(null, null).ofType(StackAdvisorRequest.StackAdvisorRequestType.CONFIGURATIONS).
        forHosts(Arrays.asList(new String[]{"hostName1"})).
        build();
    String firstResponse = command.getHostsInformation(request);
    assertEquals(String.format(SINGLE_HOST_RESPONSE, "hostName1"), firstResponse);

    request = StackAdvisorRequestBuilder.
        forStack(null, null).ofType(StackAdvisorRequest.StackAdvisorRequestType.CONFIGURATIONS).
        forHosts(Arrays.asList(new String[]{"hostName1", "hostName2"})).
        build();
    String secondResponse = command.getHostsInformation(request);
    assertEquals(String.format(TWO_HOST_RESPONSE, "hostName1", "hostName2"), secondResponse);
  }

  private static String jsonString(Object obj) throws IOException {
    return new ObjectMapper().writeValueAsString(obj);
  }

  private static JsonNode json(Object obj) throws IOException {
    return new ObjectMapper().convertValue(obj, JsonNode.class);
  }

  private static JsonNode json(String jsonString) throws IOException {
    return new ObjectMapper().readTree(jsonString);
  }

  private static List<Object> list(Object... items) {
    return Lists.newArrayList(items);
  }

  private static Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> map = new HashMap<>();
    Iterator<Object> iterator = Arrays.asList(keysAndValues).iterator();
    while (iterator.hasNext()) {
      map.put(iterator.next().toString(), iterator.next());
    }
    return map;
  }

  class TestStackAdvisorCommand extends StackAdvisorCommand<TestResource> {
    public TestStackAdvisorCommand(File recommendationsDir, String recommendationsArtifactsLifetime, ServiceInfo.ServiceAdvisorType serviceAdvisorType,
                                   int requestId, StackAdvisorRunner saRunner, AmbariMetaInfo metaInfo, Map<String, JsonNode> hostInfoCache) {
      super(recommendationsDir, recommendationsArtifactsLifetime, serviceAdvisorType, requestId, saRunner, metaInfo,
          ambariServerConfigurationHandler, hostInfoCache);
    }

    @Override
    protected void validate(StackAdvisorRequest request) throws StackAdvisorException {
      // do nothing
    }

    @Override
    protected String getResultFileName() {
      return "result.json";
    }

    @Override
    protected StackAdvisorCommandType getCommandType() {
      return StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    }

    @Override
    protected TestResource updateResponse(StackAdvisorRequest request, TestResource response) {
      return response;
    }

    // Overridden to ensure visiblity in tests
    @Override
    public javax.ws.rs.core.Response handleRequest(HttpHeaders headers, String body,
                                                                  UriInfo uriInfo, Request.Type requestType,
                                                                  MediaType mediaType, ResourceInstance resource) {
      return super.handleRequest(headers, body, uriInfo, requestType, mediaType, resource);
    }
  }

  public static class TestResource extends StackAdvisorResponse {
    @JsonProperty
    private String type;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

}
