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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.utils.Assertions.assertThrows;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.StackFactory;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class RequestValidatorTest extends EasyMockSupport {

  private static final Map<String, ?> FAKE_DESCRIPTOR = ImmutableMap.of("kerberos", "descriptor");
  private static final String FAKE_DESCRIPTOR_REFERENCE = "ref";

  private final AddServiceRequest request = createNiceMock(AddServiceRequest.class);
  private final Cluster cluster = createMock(Cluster.class);
  private final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
  private final ConfigHelper configHelper = createMock(ConfigHelper.class);
  private final StackFactory stackFactory = createNiceMock(StackFactory.class);
  private final KerberosDescriptorFactory kerberosDescriptorFactory = createNiceMock(KerberosDescriptorFactory.class);
  private final SecurityConfigurationFactory securityConfigurationFactory = createStrictMock(SecurityConfigurationFactory.class);
  private final RequestValidator validator = new RequestValidator(request, cluster, controller, configHelper, stackFactory, kerberosDescriptorFactory, securityConfigurationFactory);

  @Before
  public void setUp() {
    try {
      validator.setState(RequestValidator.State.INITIAL);
      expect(cluster.getClusterName()).andReturn("TEST").anyTimes();
      expect(cluster.getServices()).andStubReturn(ImmutableMap.of());
      expect(cluster.getSecurityType()).andStubReturn(SecurityType.NONE);
      expect(configHelper.calculateExistingConfigs(cluster)).andStubReturn(Configuration.newEmpty().asPair());
      expect(request.getServices()).andStubReturn(ImmutableSet.of());
      expect(request.getComponents()).andStubReturn(ImmutableSet.of());
      expect(request.getSecurity()).andStubReturn(Optional.empty());
      expect(request.getValidationType()).andStubReturn(AddServiceRequest.ValidationType.DEFAULT);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @After
  public void tearDown() {
    resetAll();
  }

  @Test
  public void cannotConstructInvalidRequestInfo() {
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));

    Stack stack = simpleMockStack();
    Map<String, Map<String, Set<String>>> newServices = someNewServices();
    Configuration config = Configuration.newEmpty();

    validator.setState(RequestValidator.State.INITIAL.with(stack));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
    validator.setState(validator.getState().with(config));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));

    validator.setState(RequestValidator.State.INITIAL.withNewServices(newServices));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
    validator.setState(validator.getState().with(stack));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));

    validator.setState(RequestValidator.State.INITIAL.with(config));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
    validator.setState(validator.getState().withNewServices(newServices));
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(null, null));
  }

  @Test
  public void canConstructValidRequestInfo() {
    validator.setState(
      RequestValidator.State.INITIAL
        .withNewServices(someNewServices())
        .with(simpleMockStack())
        .with(Configuration.newEmpty())
    );
    ActionManager actionManager = createNiceMock(ActionManager.class);
    RequestFactory requestFactory = createNiceMock(RequestFactory.class);
    replayAll();

    AddServiceInfo addServiceInfo = validator.createValidServiceInfo(actionManager, requestFactory);
    assertNotNull(addServiceInfo);
    assertSame(request, addServiceInfo.getRequest());
    assertEquals(cluster.getClusterName(), addServiceInfo.clusterName());
    assertSame(validator.getState().getConfig(), addServiceInfo.getConfig());
    assertSame(validator.getState().getStack(), addServiceInfo.getStack());
    assertEquals(validator.getState().getNewServices(), addServiceInfo.newServices());
  }

  @Test
  public void cannotConstructTwice() {
    ActionManager actionManager = createNiceMock(ActionManager.class);
    RequestFactory requestFactory = createNiceMock(RequestFactory.class);
    replayAll();

    validator.setState(
      RequestValidator.State.INITIAL
        .withNewServices(someNewServices())
        .with(simpleMockStack())
        .with(Configuration.newEmpty())
    );
    validator.createValidServiceInfo(actionManager, requestFactory);
    assertThrows(IllegalStateException.class, () -> validator.createValidServiceInfo(actionManager, requestFactory));
  }

  @Test
  public void reportsUnknownStackFromRequest() throws Exception {
    StackId requestStackId = new StackId("HDP", "123");
    expect(request.getStackId()).andReturn(Optional.of(requestStackId)).anyTimes();
    expect(stackFactory.createStack(requestStackId.getStackName(), requestStackId.getStackVersion(), controller)).andThrow(new AmbariException("Stack not found"));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateStack);
    assertTrue(e.getMessage().contains(requestStackId.toString()));
    assertNull(validator.getState().getStack());
  }

  @Test
  public void reportsUnknownStackFromCluster() throws Exception {
    StackId clusterStackId = new StackId("CLUSTER", "555");
    expect(request.getStackId()).andReturn(Optional.empty()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(clusterStackId);
    expect(stackFactory.createStack(clusterStackId.getStackName(), clusterStackId.getStackVersion(), controller)).andThrow(new AmbariException("Stack not found"));
    replayAll();

    IllegalStateException e = assertThrows(IllegalStateException.class, validator::validateStack);
    assertTrue(e.getMessage().contains(clusterStackId.toString()));
    assertNull(validator.getState().getStack());
  }

  @Test
  public void useClusterStackIfAbsentInRequest() throws Exception {
    StackId clusterStackId = new StackId("CLUSTER", "123");
    Stack expectedStack = createNiceMock(Stack.class);
    expect(request.getStackId()).andReturn(Optional.empty()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(clusterStackId);
    expect(stackFactory.createStack(clusterStackId.getStackName(), clusterStackId.getStackVersion(), controller)).andReturn(expectedStack);
    replayAll();

    validator.validateStack();

    assertSame(expectedStack, validator.getState().getStack());
  }

  @Test
  public void acceptsKnownServices() {
    String newService = "KAFKA";
    requestServices(false, newService);
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    validator.validateServicesAndComponents();

    Map<String, Map<String, Set<String>>> expectedNewServices = ImmutableMap.of(
      newService, ImmutableMap.of()
    );
    assertEquals(expectedNewServices, validator.getState().getNewServices());
  }

  @Test
  public void acceptsKnownComponents() {
    requestComponents("KAFKA_BROKER");
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    validator.validateServicesAndComponents();

    Map<String, Map<String, Set<String>>> expectedNewServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7401.ambari.apache.org"))
    );
    assertEquals(expectedNewServices, validator.getState().getNewServices());
  }

  @Test
  public void handlesMultipleComponentInstances() {
    expect(request.getComponents()).andReturn(
      Stream.of("c7401", "c7402")
        .map(hostname -> Component.of("KAFKA_BROKER", hostname))
        .collect(toSet()));
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    validator.validateServicesAndComponents();

    Map<String, Map<String, Set<String>>> expectedNewServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7401", "c7402"))
    );
    assertEquals(expectedNewServices, validator.getState().getNewServices());
  }

  @Test
  public void rejectsMultipleOccurrencesOfSameHostForSameComponent() {
    Set<String> duplicateHosts = ImmutableSet.of("c7402", "c7403");
    Set<String> uniqueHosts = ImmutableSet.of("c7401", "c7404");
    expect(request.getComponents()).andReturn(
      ImmutableSet.of(
        Component.of("KAFKA_BROKER", ProvisionAction.INSTALL_AND_START, "c7401", "c7402", "c7403"),
        Component.of("KAFKA_BROKER", ProvisionAction.INSTALL_ONLY, "c7402", "c7403", "c7404")
      )
    );
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains("hosts appear multiple"));
    duplicateHosts.forEach(host -> assertTrue(e.getMessage().contains(host)));
    uniqueHosts.forEach(host -> assertFalse(e.getMessage().contains(host)));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsUnknownService() {
    String serviceName = "UNKNOWN_SERVICE";
    requestServices(false, serviceName);
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(serviceName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsUnknownComponent() {
    String componentName = "UNKNOWN_COMPONENT";
    requestComponents(componentName);
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(componentName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsExistingServiceForService() {
    String serviceName = "KAFKA";
    requestServices(false, serviceName);
    clusterAlreadyHasServices(serviceName);
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(serviceName));
    assertNull(validator.getState().getNewServices());
  }

  private void clusterAlreadyHasServices(String serviceName) {
    expect(cluster.getServices()).andReturn(ImmutableMap.of(serviceName, createNiceMock(Service.class))).anyTimes();
  }

  @Test
  public void rejectsExistingServiceForComponent() {
    String serviceName = "KAFKA";
    String componentName = "KAFKA_BROKER";
    clusterAlreadyHasServices(serviceName);
    requestComponents(componentName);
    validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertTrue(e.getMessage().contains(serviceName));
    assertTrue(e.getMessage().contains(componentName));
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void rejectsEmptyServiceAndComponentList() {
    replayAll();

    assertThrows(IllegalArgumentException.class, validator::validateServicesAndComponents);
    assertNull(validator.getState().getNewServices());
  }

  @Test
  public void acceptsKnownHosts() {
    Set<String> requestHosts = ImmutableSet.of("c7401.ambari.apache.org", "c7402.ambari.apache.org");
    Set<String> otherHosts = ImmutableSet.of("c7403.ambari.apache.org", "c7404.ambari.apache.org");
    Set<String> clusterHosts = Sets.union(requestHosts, otherHosts);
    expect(cluster.getHostNames()).andReturn(clusterHosts).anyTimes();
    validator.setState(RequestValidator.State.INITIAL.withNewServices(ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", requestHosts)
    )));
    replayAll();

    validator.validateHosts();
  }

  @Test
  public void rejectsUnknownHosts() {
    Set<String> clusterHosts = ImmutableSet.of("c7401.ambari.apache.org", "c7402.ambari.apache.org");
    Set<String> otherHosts = ImmutableSet.of("c7403.ambari.apache.org", "c7404.ambari.apache.org");
    Set<String> requestHosts = ImmutableSet.copyOf(Sets.union(clusterHosts, otherHosts));
    expect(cluster.getHostNames()).andReturn(clusterHosts).anyTimes();
    validator.setState(RequestValidator.State.INITIAL.withNewServices(ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", requestHosts)
    )));
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateHosts);
    assertTrue(e.getMessage(), e.getMessage().contains("host"));
  }

  @Test
  public void acceptsAbsentSecurityWhenClusterHasKerberos() {
    secureCluster();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void acceptsAbsentSecurityWhenClusterHasNone() {
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void acceptsMatchingKerberosSecurity() {
    secureCluster();
    requestSpecifiesSecurity();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void acceptsMatchingNoneSecurity() {
    expect(request.getSecurity()).andReturn(Optional.of(SecurityConfiguration.NONE)).anyTimes();
    replayAll();

    validator.validateSecurity();
  }

  @Test
  public void rejectsNoneSecurityWhenClusterHasKerberos() {
    testBothValidationTypes(validator::validateSecurity, "KERBEROS", () -> {
      secureCluster();
      expect(request.getSecurity()).andReturn(Optional.of(SecurityConfiguration.NONE)).anyTimes();
    });
  }

  @Test
  public void rejectsKerberosSecurityWhenClusterHasNone() {
    testBothValidationTypes(validator::validateSecurity, "KERBEROS", this::requestSpecifiesSecurity);
  }

  @Test
  public void rejectsKerberosDescriptorForNoSecurity() {
    SecurityConfiguration requestSecurity = SecurityConfiguration.forTest(SecurityType.NONE, null, ImmutableMap.of("kerberos", "descriptor"));
    expect(request.getSecurity()).andReturn(Optional.of(requestSecurity)).anyTimes();
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    assertTrue(e.getMessage().contains("Kerberos descriptor"));
  }

  @Test
  public void rejectsKerberosDescriptorReferenceForNoSecurity() {
    SecurityConfiguration requestSecurity = SecurityConfiguration.forTest(SecurityType.NONE, "ref", null);
    expect(request.getSecurity()).andReturn(Optional.of(requestSecurity)).anyTimes();
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    assertTrue(e.getMessage().contains("Kerberos descriptor reference"));
  }

  @Test
  public void rejectsRequestWithBothKerberosDescriptorAndReference() {
    secureCluster();
    SecurityConfiguration invalidConfig = SecurityConfiguration.forTest(SecurityType.KERBEROS, "ref", ImmutableMap.of());
    expect(request.getSecurity()).andReturn(Optional.of(invalidConfig)).anyTimes();
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    assertTrue(e.getMessage().contains("Kerberos descriptor and reference"));
  }

  @Test
  public void loadsKerberosDescriptorByReference() {
    String newService = "KAFKA";
    secureCluster();
    requestServices(true, newService);
    KerberosDescriptor kerberosDescriptor = requestHasKerberosDescriptorFor(true, newService);
    replayAll();

    validator.validateSecurity();

    assertEquals(kerberosDescriptor, validator.getState().getKerberosDescriptor());
    verifyAll();
  }

  @Test
  public void reportsDanglingKerberosDescriptorReference() {
    String newService = "KAFKA";
    secureCluster();
    requestServices(true, newService);
    SecurityConfiguration requestSecurity = SecurityConfiguration.withReference(FAKE_DESCRIPTOR_REFERENCE);
    expect(request.getSecurity()).andReturn(Optional.of(requestSecurity)).anyTimes();
    expect(securityConfigurationFactory.loadSecurityConfigurationByReference(FAKE_DESCRIPTOR_REFERENCE))
      .andThrow(new IllegalArgumentException("No security configuration found for the reference: " + FAKE_DESCRIPTOR_REFERENCE));
    replayAll();

    assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    verifyAll();
  }

  @Test
  public void acceptsDescriptorWithOnlyNewServices() {
    String newService = "KAFKA";
    secureCluster();
    requestServices(true, newService);
    KerberosDescriptor kerberosDescriptor = requestHasKerberosDescriptorFor(false, newService);
    replayAll();

    validator.validateSecurity();

    assertEquals(kerberosDescriptor, validator.getState().getKerberosDescriptor());
  }

  @Test
  public void acceptsDescriptorWithAdditionalServices() {
    String newService = "KAFKA", otherService = "ZOOKEEPER";
    secureCluster();
    requestServices(true, newService);
    requestHasKerberosDescriptorFor(false, newService, otherService);
    replayAll();

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, validator::validateSecurity);
    assertTrue(e.getMessage().contains("only for new services"));
  }

  @Test
  public void acceptsDescriptorWithoutServices() {
    secureCluster();
    requestServices(true, "KAFKA");
    KerberosDescriptor kerberosDescriptor = requestHasKerberosDescriptorFor(false);
    replayAll();

    validator.validateSecurity();

    assertEquals(kerberosDescriptor, validator.getState().getKerberosDescriptor());
  }

  @Test
  public void combinesRequestConfigWithClusterAndStack() throws AmbariException {
    Configuration requestConfig = Configuration.newEmpty();
    requestConfig.setProperty("kafka-broker", "zookeeper.connect", "zookeeper.connect:request");
    requestConfig.setProperty("kafka-env", "custom_property", "custom_property:request");
    expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
    expect(request.getRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.ALWAYS_APPLY).anyTimes();

    Configuration clusterConfig = Configuration.newEmpty();
    clusterConfig.setProperty("zookeeper-env", "zk_user", "zk_user:cluster_level");
    expect(configHelper.calculateExistingConfigs(cluster)).andReturn(clusterConfig.asPair()).anyTimes();

    Stack stack = simpleMockStack();
    Configuration stackConfig = Configuration.newEmpty();
    stackConfig.setProperty("zookeeper-env", "zk_user", "zk_user:stack_default");
    stackConfig.setProperty("zookeeper-env", "zk_log_dir", "zk_log_dir:stack_default");
    stackConfig.setProperty("kafka-broker", "zookeeper.connect", "zookeeper.connect:stack_default");
    expect(stack.getDefaultConfig()).andReturn(stackConfig).anyTimes();

    replayAll();

    validator.setState(RequestValidator.State.INITIAL.with(stack));
    validator.validateConfiguration();

    Configuration config = validator.getState().getConfig();
    verifyConfigOverrides(requestConfig, clusterConfig, stackConfig, config);
  }

  @Test
  public void rejectsKerberosEnvChange() {
    testBothValidationTypes(validator::validateConfiguration, () -> {
      Configuration requestConfig = Configuration.newEmpty();
      requestConfig.setProperty("kerberos-env", "some-property", "some-value");
      expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
      validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    });
  }

  @Test
  public void rejectsKrb5ConfChange() {
    testBothValidationTypes(validator::validateConfiguration, () -> {
      Configuration requestConfig = Configuration.newEmpty();
      requestConfig.setProperty("krb5-conf", "some-property", "some-value");
      expect(request.getConfiguration()).andReturn(requestConfig.copy()).anyTimes();
      validator.setState(RequestValidator.State.INITIAL.with(simpleMockStack()));
    });
  }

  /**
   * Tests that the state created by {@code testCaseSetup} is rejected by strict validation, but accepted by permissive one.
   * @param validation the method to call on validator
   * @param testCaseSetup code to setup the state ("when"), call to replayAll() should be omitted
   */
  private void testBothValidationTypes(Runnable validation, Runnable testCaseSetup) {
    testBothValidationTypes(validation, null, testCaseSetup);
  }

  private void testBothValidationTypes(Runnable validation, String expectedMessage, Runnable testCaseSetup) {
    testCaseSetup.run();
    replayAll();
    Exception e = assertThrows(IllegalArgumentException.class, validation);
    if (expectedMessage != null) {
      assertTrue(e.getMessage().contains(expectedMessage));
    }

    resetAll();
    setUp();
    permissiveValidation();
    testCaseSetup.run();
    replayAll();
    validation.run();
  }

  private static void verifyConfigOverrides(Configuration requestConfig, Configuration clusterConfig, Configuration stackConfig, Configuration actualConfig) {
    requestConfig.getProperties().forEach(
      (type, properties) -> properties.forEach(
        (propertyName, propertyValue) -> assertEquals(type + "/" + propertyName, propertyValue, actualConfig.getPropertyValue(type, propertyName))
      )
    );
    clusterConfig.getProperties().forEach(
      (type, properties) -> properties.forEach(
        (propertyName, propertyValue) -> {
          if (!requestConfig.isPropertySet(type, propertyName)) {
            assertEquals(type + "/" + propertyName, propertyValue, actualConfig.getPropertyValue(type, propertyName));
          }
        }
      )
    );
    stackConfig.getProperties().forEach(
      (type, properties) -> properties.forEach(
        (propertyName, propertyValue) -> {
          if (!requestConfig.isPropertySet(type, propertyName) && !clusterConfig.isPropertySet(type, propertyName)) {
            assertEquals(type + "/" + propertyName, propertyValue, actualConfig.getPropertyValue(type, propertyName));
          }
        }
      )
    );
  }

  private Stack simpleMockStack() {
    Stack stack = createNiceMock(Stack.class);
    Set<String> stackServices = ImmutableSet.of("KAFKA", "ZOOKEEPER");
    expect(stack.getServices()).andStubReturn(stackServices);
    expect(stack.getServiceForComponent("KAFKA_BROKER")).andStubReturn("KAFKA");
    expect(stack.getServiceForComponent("ZOOKEEPER_SERVER")).andStubReturn("ZOOKEEPER");
    expect(stack.getServiceForComponent("ZOOKEEPER_CLIENT")).andStubReturn("ZOOKEEPER");
    expect(stack.getValidDefaultConfig()).andStubReturn(Configuration.newEmpty());
    return stack;
  }

  private static Map<String, Map<String, Set<String>>> someNewServices() {
    return ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7401.ambari.apache.org"))
    );
  }

  private void permissiveValidation() {
    expect(request.getValidationType()).andReturn(AddServiceRequest.ValidationType.PERMISSIVE).anyTimes();
  }

  private void requestServices(boolean validated, String... services) {
    expect(request.getServices()).andReturn(
      Arrays.stream(services)
        .map(org.apache.ambari.server.topology.addservice.Service::of)
        .collect(toSet())
    ).anyTimes();
    if (validated) {
      validatedServices(services);
    }
  }

  private void validatedServices(String... services) {
    validator.setState(
      RequestValidator.State.INITIAL
        .with(simpleMockStack())
        .withNewServices(
          Arrays.stream(services)
            .collect(toMap(Function.identity(), __ -> ImmutableMap.of()))
        )
    );
  }

  private void requestComponents(String... components) {
    expect(request.getComponents()).andReturn(
      Arrays.stream(components)
        .map(componentName -> Component.of(componentName, "c7401.ambari.apache.org"))
        .collect(toSet())
    );
  }

  private void secureCluster() {
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
  }

  private void requestSpecifiesSecurity() {
    expect(request.getSecurity()).andReturn(Optional.of(SecurityConfiguration.KERBEROS)).anyTimes();
  }

  private KerberosDescriptor requestHasKerberosDescriptorFor(boolean byReference, String... services) {
    SecurityConfiguration requestSecurity = byReference
      ? SecurityConfiguration.withReference(FAKE_DESCRIPTOR_REFERENCE)
      : SecurityConfiguration.withDescriptor(FAKE_DESCRIPTOR);
    expect(request.getSecurity()).andReturn(Optional.of(requestSecurity)).anyTimes();

    if (byReference) {
      expect(securityConfigurationFactory.loadSecurityConfigurationByReference(FAKE_DESCRIPTOR_REFERENCE))
        .andReturn(SecurityConfiguration.forTest(SecurityType.KERBEROS, FAKE_DESCRIPTOR_REFERENCE, FAKE_DESCRIPTOR));
    }

    KerberosDescriptor kerberosDescriptor = kerberosDescriptorForServices(services);
    expect(kerberosDescriptorFactory.createInstance(FAKE_DESCRIPTOR)).andReturn(kerberosDescriptor).anyTimes();

    return kerberosDescriptor;
  }

  private static KerberosDescriptor kerberosDescriptorForServices(String... newServices) {
    return new KerberosDescriptorFactory().createInstance(ImmutableMap.of(
      KerberosDescriptor.KEY_SERVICES, Arrays.stream(newServices)
        .map(each -> ImmutableMap.of(KerberosServiceDescriptor.KEY_NAME, each))
        .collect(toList())
    ));
  }

}
