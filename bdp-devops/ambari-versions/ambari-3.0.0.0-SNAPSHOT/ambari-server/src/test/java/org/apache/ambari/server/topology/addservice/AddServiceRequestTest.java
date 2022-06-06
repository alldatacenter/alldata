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

package org.apache.ambari.server.topology.addservice;

import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;
import static org.apache.ambari.server.controller.internal.ProvisionAction.START_ONLY;
import static org.apache.ambari.server.topology.ConfigRecommendationStrategy.ALWAYS_APPLY;
import static org.apache.ambari.server.topology.addservice.AddServiceRequest.OperationType.ADD_SERVICE;
import static org.apache.ambari.server.topology.addservice.AddServiceRequest.ValidationType.PERMISSIVE;
import static org.apache.ambari.server.topology.addservice.AddServiceRequest.ValidationType.STRICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.Credential;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

/**
 * Tests for {@link AddServiceRequest} serialization / deserialization / syntax validation
 */
public class AddServiceRequestTest {

  private static String REQUEST_ALL_FIELDS_SET;
  private static String REQUEST_MINIMAL_SERVICES_AND_COMPONENTS;
  private static String REQUEST_MINIMAL_SERVICES_ONLY;
  private static String REQUEST_MINIMAL_COMPONENTS_ONLY;
  private static String REQUEST_INVALID_NO_SERVICES_AND_COMPONENTS;
  private static String REQUEST_INVALID_INVALID_FIELD;
  private static String REQUEST_INVALID_INVALID_CONFIG;
  private static final Map<String, List<Map<String, String>>> KERBEROS_DESCRIPTOR1 =
    ImmutableMap.of("services", ImmutableList.of(ImmutableMap.of("name", "ZOOKEEPER")));

  private ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void setUpClass() {
    REQUEST_ALL_FIELDS_SET = read("add_service_api/request1.json");
    REQUEST_MINIMAL_SERVICES_AND_COMPONENTS = read("add_service_api/request2.json");
    REQUEST_MINIMAL_SERVICES_ONLY = read("add_service_api/request3.json");
    REQUEST_MINIMAL_COMPONENTS_ONLY = read("add_service_api/request4.json");
    REQUEST_INVALID_NO_SERVICES_AND_COMPONENTS = read("add_service_api/request_invalid_1.json");
    REQUEST_INVALID_INVALID_FIELD = read("add_service_api/request_invalid_2.json");
    REQUEST_INVALID_INVALID_CONFIG = read("add_service_api/request_invalid_3.json");
  }

  @Test
  public void testDeserialize_basic() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_ALL_FIELDS_SET, AddServiceRequest.class);

    assertEquals(ADD_SERVICE, request.getOperationType());
    assertEquals(ALWAYS_APPLY, request.getRecommendationStrategy());
    assertEquals(INSTALL_ONLY, request.getProvisionAction());
    assertEquals(PERMISSIVE, request.getValidationType());
    assertEquals("HDP", request.getStackName());
    assertEquals("3.0", request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertEquals(
      ImmutableMap.of("storm-site", ImmutableMap.of("final", ImmutableMap.of("fs.defaultFS", "true"))),
      configuration.getAttributes());
    assertEquals(
      ImmutableMap.of("storm-site", ImmutableMap.of("ipc.client.connect.max.retries", "50")),
      configuration.getProperties());

    assertEquals(
      ImmutableSet.of(
        Component.of("NIMBUS", START_ONLY, "c7401.ambari.apache.org", "c7402.ambari.apache.org"),
        Component.of("BEACON_SERVER", "c7402.ambari.apache.org", "c7403.ambari.apache.org")
      ),
      request.getComponents());

    assertEquals(
      ImmutableSet.of(Service.of("STORM", INSTALL_AND_START), Service.of("BEACON")),
      request.getServices());

    assertEquals(
      Optional.of(SecurityConfiguration.forTest(SecurityType.KERBEROS, "ref_to_kerb_desc", KERBEROS_DESCRIPTOR1)),
      request.getSecurity());

    assertEquals(
      ImmutableMap.of(
        "kdc.admin.credential", new Credential( "kdc.admin.credential", "admin/admin@EXAMPLE.COM", "k", CredentialStoreType.TEMPORARY)
      ),
      request.getCredentials()
    );
  }

  @Test
  public void testDeserialize_defaultAndEmptyValues() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_SERVICES_AND_COMPONENTS, AddServiceRequest.class);

    // filled-out values
    assertEquals(
      ImmutableSet.of(Component.of("NIMBUS", "c7401.ambari.apache.org", "c7402.ambari.apache.org"), Component.of("BEACON_SERVER", "c7402.ambari.apache.org", "c7403.ambari.apache.org")),
      request.getComponents());

    assertEquals(
      ImmutableSet.of(Service.of("STORM"), Service.of("BEACON")),
      request.getServices());

    // default / empty values
    assertEquals(ADD_SERVICE, request.getOperationType());
    assertEquals(ConfigRecommendationStrategy.defaultForAddService(), request.getRecommendationStrategy());
    assertEquals(INSTALL_AND_START, request.getProvisionAction());
    assertEquals(STRICT, request.getValidationType());
    assertNull(request.getStackName());
    assertNull(request.getStackVersion());
    assertEquals(Optional.empty(), request.getSecurity());
    assertEquals(ImmutableMap.of(), request.getCredentials());

    Configuration configuration = request.getConfiguration();
    assertTrue(configuration.getFullAttributes().isEmpty());
    assertTrue(configuration.getFullProperties().isEmpty());
  }

  @Test
  public void testDeserialize_onlyServices() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_SERVICES_ONLY, AddServiceRequest.class);

    // filled-out values
    assertEquals(
      ImmutableSet.of(Service.of("STORM"), Service.of("BEACON")),
      request.getServices());

    // default / empty values
    assertEquals(ADD_SERVICE, request.getOperationType());
    assertEquals(ConfigRecommendationStrategy.defaultForAddService(), request.getRecommendationStrategy());
    assertEquals(INSTALL_AND_START, request.getProvisionAction());
    assertNull(request.getStackName());
    assertNull(request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertTrue(configuration.getFullAttributes().isEmpty());
    assertTrue(configuration.getFullProperties().isEmpty());

    assertTrue(request.getComponents().isEmpty());
  }

  @Test
  public void testDeserialize_onlyComponents() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_COMPONENTS_ONLY, AddServiceRequest.class);

    // filled-out values
    assertEquals(
      ImmutableSet.of(Component.of("NIMBUS", "c7401.ambari.apache.org", "c7402.ambari.apache.org"), Component.of("BEACON_SERVER", "c7402.ambari.apache.org", "c7403.ambari.apache.org")),
      request.getComponents());

    // default / empty values
    assertEquals(ADD_SERVICE, request.getOperationType());
    assertEquals(ConfigRecommendationStrategy.defaultForAddService(), request.getRecommendationStrategy());
    assertEquals(INSTALL_AND_START, request.getProvisionAction());
    assertNull(request.getStackName());
    assertNull(request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertTrue(configuration.getFullAttributes().isEmpty());
    assertTrue(configuration.getFullProperties().isEmpty());

    assertTrue(request.getServices().isEmpty());
  }

  @Test
  public void testDeserialize_invalid_noServicesAndComponents() throws Exception {
    // empty service/component list should be accepted at the JSON level, will be rejected by the request handler
    mapper.readValue(REQUEST_INVALID_NO_SERVICES_AND_COMPONENTS, AddServiceRequest.class);
  }

  @Test(expected = JsonProcessingException.class)
  public void testDeserialize_invalid_invalidField() throws Exception {
    mapper.readValue(REQUEST_INVALID_INVALID_FIELD, AddServiceRequest.class);
  }

  @Test(expected = JsonProcessingException.class)
  public void testDeserialize_invalid_invalidConfiguration() throws Exception {
    mapper.readValue(REQUEST_INVALID_INVALID_CONFIG, AddServiceRequest.class);
  }

  @Test
  public void testSerialize_basic() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_ALL_FIELDS_SET, AddServiceRequest.class);
    AddServiceRequest serialized = serialize(request);
    assertEquals(request, serialized);
    assertEquals(ImmutableMap.of(), serialized.getCredentials());
  }

  @Test
  public void testSerialize_EmptyOmitted() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_SERVICES_ONLY, AddServiceRequest.class);
    AddServiceRequest serialized = serialize(request);
    assertEquals(request, serialized);
  }

  private AddServiceRequest serialize(AddServiceRequest request) throws IOException {
    String serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    return mapper.readValue(serialized, AddServiceRequest.class);
  }

  private static String read(String resourceName) {
    try {
      return Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}