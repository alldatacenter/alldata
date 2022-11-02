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

package org.apache.ambari.server.stack;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CredentialStoreInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.ServicePropertyInfo;
import org.apache.ambari.server.state.SingleSignOnInfo;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * ServiceModule unit tests.
 */
public class ServiceModuleTest {

  @Test
  public void testResolve_Comment() throws Exception {
    String comment = "test comment";

    // comment specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setComment(comment);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(comment, service.getModuleInfo().getComment());

    // comment specified in parent only
    info.setComment(null);
    parentInfo.setComment(comment);

    service = resolveService(info, parentInfo);
    assertEquals(comment, service.getModuleInfo().getComment());

    // set in both
    info.setComment(comment);
    parentInfo.setComment("other comment");

    service = resolveService(info, parentInfo);
    assertEquals(comment, service.getModuleInfo().getComment());
  }

  @Test
  public void testResolve_DisplayName() throws Exception {
    String displayName = "test_display_name";

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setDisplayName(displayName);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(displayName, service.getModuleInfo().getDisplayName());

    // specified in parent only
    info.setDisplayName(null);
    parentInfo.setDisplayName(displayName);

    service = resolveService(info, parentInfo);
    assertEquals(displayName, service.getModuleInfo().getDisplayName());

    // specified in both
    info.setDisplayName(displayName);
    parentInfo.setDisplayName("other display name");

    service = resolveService(info, parentInfo);
    assertEquals(displayName, service.getModuleInfo().getDisplayName());
  }

  @Test
  public void testResolve_Version() throws Exception {
    String version = "1.1";

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setVersion(version);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(version, service.getModuleInfo().getVersion());

    // specified in parent only
    info.setVersion(null);
    parentInfo.setVersion(version);

    service = resolveService(info, parentInfo);
    assertEquals(version, service.getModuleInfo().getVersion());

    // specified in both
    info.setVersion(version);
    parentInfo.setVersion("1.0");

    service = resolveService(info, parentInfo);
    assertEquals(version, service.getModuleInfo().getVersion());
  }

  @Test
  public void testResolve_RequiredServices() throws Exception {
    List<String> requiredServices = new ArrayList<>();
    requiredServices.add("foo");
    requiredServices.add("bar");

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setRequiredServices(requiredServices);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(requiredServices, service.getModuleInfo().getRequiredServices());

    // specified in parent only
    info.setRequiredServices(null);
    parentInfo.setRequiredServices(requiredServices);

    service = resolveService(info, parentInfo);
    assertEquals(requiredServices, service.getModuleInfo().getRequiredServices());

    // specified in both
    info.setRequiredServices(requiredServices);
    parentInfo.setRequiredServices(Collections.singletonList("other"));

    service = resolveService(info, parentInfo);
    assertEquals(requiredServices, service.getModuleInfo().getRequiredServices());

    // not set in either
    info.setRequiredServices(null);
    parentInfo.setRequiredServices(null);

    service = resolveService(info, parentInfo);
    assertTrue(service.getModuleInfo().getRequiredServices().isEmpty());
  }

  @Test
  public void testResolve_RestartRequiredAfterChange() throws Exception {
    Boolean isRestartRequired = true;

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setRestartRequiredAfterChange(isRestartRequired);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(isRestartRequired, service.getModuleInfo().isRestartRequiredAfterChange());

    // specified in parent only
    info.setRestartRequiredAfterChange(null);
    parentInfo.setRestartRequiredAfterChange(isRestartRequired);

    service = resolveService(info, parentInfo);
    assertEquals(isRestartRequired, service.getModuleInfo().isRestartRequiredAfterChange());

    // specified in both
    info.setRestartRequiredAfterChange(isRestartRequired);
    parentInfo.setRestartRequiredAfterChange(false);

    service = resolveService(info, parentInfo);
    assertEquals(isRestartRequired, service.getModuleInfo().isRestartRequiredAfterChange());
  }

  @Test
  public void testResolve_MonitoringService() throws Exception {
    Boolean isMonitoringService = true;

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setMonitoringService(isMonitoringService);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(isMonitoringService, service.getModuleInfo().isMonitoringService());

    // specified in parent only
    info.setMonitoringService(null);
    parentInfo.setMonitoringService(isMonitoringService);

    service = resolveService(info, parentInfo);
    assertEquals(isMonitoringService, service.getModuleInfo().isMonitoringService());

    // specified in both
    info.setMonitoringService(isMonitoringService);
    parentInfo.setMonitoringService(false);

    service = resolveService(info, parentInfo);
    assertEquals(isMonitoringService, service.getModuleInfo().isMonitoringService());
  }

  @Test
  public void testResolve_OsSpecifics() throws Exception {
    Map<String, ServiceOsSpecific> osSpecifics = new HashMap<>();
    osSpecifics.put("foo", new ServiceOsSpecific());

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setOsSpecifics(osSpecifics);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(osSpecifics, service.getModuleInfo().getOsSpecifics());

    // specified in parent only
    info.setOsSpecifics(null);
    parentInfo.setOsSpecifics(osSpecifics);

    service = resolveService(info, parentInfo);
    assertEquals(osSpecifics, service.getModuleInfo().getOsSpecifics());

    // specified in both
    Map<String, ServiceOsSpecific> osSpecifics2 = new HashMap<>();
    osSpecifics.put("bar", new ServiceOsSpecific());

    info.setOsSpecifics(osSpecifics);
    parentInfo.setOsSpecifics(osSpecifics2);

    service = resolveService(info, parentInfo);
    assertEquals(osSpecifics, service.getModuleInfo().getOsSpecifics());
  }

  @Test
  public void testResolve_CommandScript() throws Exception {
    CommandScriptDefinition commandScript = new CommandScriptDefinition();

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setCommandScript(commandScript);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(commandScript, service.getModuleInfo().getCommandScript());

    // specified in parent only
    info.setCommandScript(null);
    parentInfo.setCommandScript(commandScript);

    service = resolveService(info, parentInfo);
    assertEquals(commandScript, service.getModuleInfo().getCommandScript());

    // specified in both
    CommandScriptDefinition commandScript2 = new CommandScriptDefinition();

    info.setCommandScript(commandScript);
    parentInfo.setCommandScript(commandScript2);

    service = resolveService(info, parentInfo);
    assertEquals(commandScript, service.getModuleInfo().getCommandScript());
  }

  @Test
  public void testResolve_ServicePackageFolder() throws Exception {
    String servicePackageFolder = "packageDir";

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);

    // set in the module constructor from a value obtained from service directory
    assertEquals("packageDir", child.getModuleInfo().getServicePackageFolder());
    parent.getModuleInfo().setServicePackageFolder(null);

    resolveService(child, parent);
    assertEquals(servicePackageFolder, child.getModuleInfo().getServicePackageFolder());

    // specified in parent only
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setServicePackageFolder(servicePackageFolder);
    child.getModuleInfo().setServicePackageFolder(null);

    resolveService(child, parent);
    assertEquals(servicePackageFolder, child.getModuleInfo().getServicePackageFolder());

    // specified in both
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setServicePackageFolder("someOtherDir");
    child.getModuleInfo().setServicePackageFolder(servicePackageFolder);

    resolveService(child, parent);
    assertEquals(servicePackageFolder, child.getModuleInfo().getServicePackageFolder());
  }

  @Test
  public void testResolve_MetricsFile() throws Exception {
    File metricsFile = new File("testMetricsFile");

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);

    // set in the module constructor from a value obtained from service directory which is mocked
    assertEquals(metricsFile, child.getModuleInfo().getMetricsFile());
    parent.getModuleInfo().setMetricsFile(null);

    resolveService(child, parent);
    assertEquals(metricsFile, child.getModuleInfo().getMetricsFile());

    // specified in parent only
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setMetricsFile(metricsFile);
    child.getModuleInfo().setMetricsFile(null);

    resolveService(child, parent);
    assertEquals(metricsFile, child.getModuleInfo().getMetricsFile());

    // specified in both
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setMetricsFile(new File("someOtherDir"));
    child.getModuleInfo().setMetricsFile(metricsFile);

    resolveService(child, parent);
    assertEquals(metricsFile, child.getModuleInfo().getMetricsFile());
  }

  @Test
  public void testResolve_AlertsFile() throws Exception {
    File alertsFile = new File("testAlertsFile");

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);

    // set in the module constructor from a value obtained from service directory which is mocked
    assertEquals(alertsFile, child.getModuleInfo().getAlertsFile());
    parent.getModuleInfo().setAlertsFile(null);

    resolveService(child, parent);
    assertEquals(alertsFile, child.getModuleInfo().getAlertsFile());

    // specified in parent only
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setAlertsFile(alertsFile);
    child.getModuleInfo().setAlertsFile(null);

    resolveService(child, parent);
    assertEquals(alertsFile, child.getModuleInfo().getAlertsFile());

    // specified in both
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setAlertsFile(new File("someOtherDir"));
    child.getModuleInfo().setAlertsFile(alertsFile);

    resolveService(child, parent);
    assertEquals(alertsFile, child.getModuleInfo().getAlertsFile());
  }

  @Test
  public void testResolve_KerberosDescriptorFile() throws Exception {
    File kerberosDescriptorFile = new File("testKerberosDescriptorFile");

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);

    // set in the module constructor from a value obtained from service directory which is mocked
    assertEquals(kerberosDescriptorFile, child.getModuleInfo().getKerberosDescriptorFile());
    parent.getModuleInfo().setKerberosDescriptorFile(null);

    resolveService(child, parent);
    assertEquals(kerberosDescriptorFile, child.getModuleInfo().getKerberosDescriptorFile());

    // specified in parent only
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setKerberosDescriptorFile(kerberosDescriptorFile);
    child.getModuleInfo().setKerberosDescriptorFile(null);

    resolveService(child, parent);
    assertEquals(kerberosDescriptorFile, child.getModuleInfo().getKerberosDescriptorFile());

    // specified in both
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setKerberosDescriptorFile(new File("someOtherDir"));
    child.getModuleInfo().setKerberosDescriptorFile(kerberosDescriptorFile);

    resolveService(child, parent);
    assertEquals(kerberosDescriptorFile, child.getModuleInfo().getKerberosDescriptorFile());
  }

  @Test
  public void testResolveServiceAdvisor() throws Exception {
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);

    // Parent is NULL, Child is NULL => Child defaults to PYTHON
    parent.getModuleInfo().setServiceAdvisorType(null);
    child.getModuleInfo().setServiceAdvisorType(null);
    resolveService(child, parent);
    assertEquals(ServiceInfo.ServiceAdvisorType.PYTHON, child.getModuleInfo().getServiceAdvisorType());

    // Parent is NULL, Child is JAVA => Child is JAVA
    child.getModuleInfo().setServiceAdvisorType(ServiceInfo.ServiceAdvisorType.JAVA);
    resolveService(child, parent);
    assertEquals(ServiceInfo.ServiceAdvisorType.JAVA, child.getModuleInfo().getServiceAdvisorType());

    // Parent is JAVA, Child is NULL => Child inherits JAVA
    parent.getModuleInfo().setServiceAdvisorType(ServiceInfo.ServiceAdvisorType.JAVA);
    child.getModuleInfo().setServiceAdvisorType(null);
    resolveService(child, parent);
    assertEquals(ServiceInfo.ServiceAdvisorType.JAVA, child.getModuleInfo().getServiceAdvisorType());

    // Parent is JAVA, Child is PYTHON => Child overrides and keeps PYTHON
    parent.getModuleInfo().setServiceAdvisorType(null);
    child.getModuleInfo().setServiceAdvisorType(ServiceInfo.ServiceAdvisorType.PYTHON);
    resolveService(child, parent);
    assertEquals(ServiceInfo.ServiceAdvisorType.PYTHON, child.getModuleInfo().getServiceAdvisorType());
  }

  @Test
  public void testResolve_UpgradeCheckDirectory() throws Exception {
    File checks = new File("checks");

    // check directory specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);
    child.getModuleInfo().setChecksFolder(checks);
    resolveService(child, parent);
    assertEquals(checks.getPath(), child.getModuleInfo().getChecksFolder().getPath());

    // check directory specified in parent only
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setChecksFolder(checks);
    resolveService(child, parent);
    assertEquals(checks.getPath(), child.getModuleInfo().getChecksFolder().getPath());

    // check directory set in both
    info.setChecksFolder(checks);
    child = createServiceModule(info);
    child.getModuleInfo().setChecksFolder(checks);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setChecksFolder(new File("other"));
    resolveService(child, parent);
    assertEquals(checks.getPath(), child.getModuleInfo().getChecksFolder().getPath());
  }

  @Test
  public void testResolve_ServerActionDirectory() throws Exception {
    File serverActions = new File("server_actions");

    // check directory specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);
    child.getModuleInfo().setServerActionsFolder(serverActions);
    resolveService(child, parent);
    assertEquals(serverActions.getPath(), child.getModuleInfo().getServerActionsFolder().getPath());

    // check directory specified in parent only
    child = createServiceModule(info);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setServerActionsFolder(serverActions);
    resolveService(child, parent);
    assertEquals(serverActions.getPath(), child.getModuleInfo().getServerActionsFolder().getPath());

    // check directory set in both
    info.setServerActionsFolder(serverActions);
    child = createServiceModule(info);
    child.getModuleInfo().setServerActionsFolder(serverActions);
    parent = createServiceModule(parentInfo);
    parent.getModuleInfo().setServerActionsFolder(new File("other"));
    resolveService(child, parent);
    assertEquals(serverActions.getPath(), child.getModuleInfo().getServerActionsFolder().getPath());
  }

  @Test
  public void testResolve_CustomCommands() throws Exception {
    List<CustomCommandDefinition> customCommands = new ArrayList<>();
    CustomCommandDefinition cmd1 = new CustomCommandDefinition();
    setPrivateField(cmd1, "name", "cmd1");
    setPrivateField(cmd1, "background", false);
    CustomCommandDefinition cmd2 = new CustomCommandDefinition();
    setPrivateField(cmd2, "name", "cmd2");
    customCommands.add(cmd1);
    customCommands.add(cmd2);

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setCustomCommands(customCommands);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(customCommands, service.getModuleInfo().getCustomCommands());

    // specified in parent only
    info.setCustomCommands(null);
    parentInfo.setCustomCommands(customCommands);

    service = resolveService(info, parentInfo);
    assertEquals(customCommands, service.getModuleInfo().getCustomCommands());

    // specified in both
    List<CustomCommandDefinition> parentCustomCommands = new ArrayList<>();
    CustomCommandDefinition cmd3 = new CustomCommandDefinition();
    setPrivateField(cmd3, "name", "cmd1");
    setPrivateField(cmd3, "background", true);
    CustomCommandDefinition cmd4 = new CustomCommandDefinition();
    setPrivateField(cmd4, "name", "cmd4");
    parentCustomCommands.add(cmd3);
    parentCustomCommands.add(cmd4);

    info.setCustomCommands(customCommands);
    parentInfo.setCustomCommands(parentCustomCommands);

    service = resolveService(info, parentInfo);
    Collection<CustomCommandDefinition> mergedCommands =  service.getModuleInfo().getCustomCommands();
    assertEquals(3, mergedCommands.size());
    assertTrue(mergedCommands.contains(cmd2));
    assertTrue(mergedCommands.contains(cmd3));
    assertTrue(mergedCommands.contains(cmd4));

    // not set in either
    info.setCustomCommands(null);
    parentInfo.setCustomCommands(null);

    service = resolveService(info, parentInfo);
    assertTrue(service.getModuleInfo().getCustomCommands().isEmpty());
  }

  @Test
  public void testResolve_ConfigDependencies() throws Exception {
    List<String> configDependencies = new ArrayList<>();
    configDependencies.add("foo");
    configDependencies.add("bar");

    // specified in child only
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    info.setConfigDependencies(configDependencies);

    ServiceModule service = resolveService(info, parentInfo);
    assertEquals(configDependencies, service.getModuleInfo().getConfigDependencies());

    // specified in parent only
    info.setConfigDependencies(null);
    parentInfo.setConfigDependencies(configDependencies);

    service = resolveService(info, parentInfo);
    assertEquals(configDependencies, service.getModuleInfo().getConfigDependencies());

    // specified in both
    List<String> parentCustomCommands = new ArrayList<>();
    parentCustomCommands.add("bar");
    parentCustomCommands.add("other");

    info.setConfigDependencies(configDependencies);
    parentInfo.setConfigDependencies(parentCustomCommands);

    service = resolveService(info, parentInfo);
    Collection<String> mergedConfigDependencies =  service.getModuleInfo().getConfigDependencies();
    assertEquals(3, mergedConfigDependencies.size());
    assertTrue(mergedConfigDependencies.contains("foo"));
    assertTrue(mergedConfigDependencies.contains("bar"));
    assertTrue(mergedConfigDependencies.contains("other"));

    // not set in either
    info.setConfigDependencies(null);
    parentInfo.setConfigDependencies(null);

    service = resolveService(info, parentInfo);
    assertTrue(service.getModuleInfo().getConfigDependencies().isEmpty());
  }

  @Test
  public void testResolve_Components() throws Exception {
    // resolve should merge the child component collections
    // components 1, 2 and XX are set on the parent
    // components 1, 4 and XX are set on the child
    // component XX is marked for delete on the child and shouldn't be included
    // component 1 should be merged
    // both non-intersecting components 2 and 4 should be included
    ComponentInfo info1 = new ComponentInfo();
    info1.setName("1");
    ComponentInfo info2 = new ComponentInfo();
    info2.setName("2");
    ComponentInfo XX = new ComponentInfo();
    XX.setName("XX");

    ComponentInfo info3 = new ComponentInfo();
    // overlaps with info1
    info3.setName("1");
    info3.setCardinality("ALL");
    info3.setCategory("category");
    ComponentInfo info4 = new ComponentInfo();
    info4.setName("4");
    ComponentInfo info5 = new ComponentInfo();
    // overlaps with componentToBeDeleted
    info5.setName("XX");
    info5.setDeleted(true);

    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    //todo: getComponents() should return a protective copy, but for now there is no set/add method
    List<ComponentInfo> childComponents = info.getComponents();
    childComponents.add(info3);
    childComponents.add(info4);
    childComponents.add(info5);

    List<ComponentInfo> parentComponents = parentInfo.getComponents();
    parentComponents.add(info1);
    parentComponents.add(info2);

    ServiceModule child = createServiceModule(info);
    ServiceModule parent = createServiceModule(parentInfo);

    resolveService(child, parent);

    List<ComponentInfo> components = child.getModuleInfo().getComponents();
    assertEquals(3, components.size());

    Map<String, ComponentInfo> mergedComponents = new HashMap<>();
    for (ComponentInfo component : components) {
      mergedComponents.put(component.getName(), component);
    }
    assertTrue(mergedComponents.containsKey("1"));
    assertTrue(mergedComponents.containsKey("2"));
    assertTrue(mergedComponents.containsKey("4"));

    // ensure that overlapping components were merged.
    //don't test all properties, this is done in ComponentModuleTest
    assertEquals("ALL", mergedComponents.get("1").getCardinality());
    assertEquals("category", mergedComponents.get("1").getCategory());
  }

  @Test
  public void testResolve_Configuration__properties() throws Exception {
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    // child configurations
    //FOO
    Collection<PropertyInfo> childFooProperties = new ArrayList<>();
    PropertyInfo childProp1 = new PropertyInfo();
    childProp1.setName("childName1");
    childProp1.setValue("childVal1");
    childFooProperties.add(childProp1);

    //BAR : Doesn't inherit parents BAR due to attribute Supports.DO_NOT_EXTEND
    Collection<PropertyInfo> childBarProperties = new ArrayList<>();
    PropertyInfo childProp2 = new PropertyInfo();
    childProp2.setName("childName2");
    childProp2.setValue("childVal2");
    childBarProperties.add(childProp2);

    // add attributes for BAR
    Map<String, String> attributes = new HashMap<>();
    attributes.put(ConfigurationInfo.Supports.DO_NOT_EXTEND.getXmlAttributeName(), "true");

    // create child config modules
    ConfigurationModule childConfigModule1 = createConfigurationModule("FOO", childFooProperties);
    ConfigurationModule childConfigModule2 = createConfigurationModule("BAR", childBarProperties, attributes);
    Collection<ConfigurationModule> childModules = new ArrayList<>();
    childModules.add(childConfigModule1);
    childModules.add(childConfigModule2);

    // parent configurations
    //FOO
    Collection<PropertyInfo> parentFooProperties = new ArrayList<>();
    PropertyInfo parentProp1 = new PropertyInfo();
    parentProp1.setName("parentName1");
    parentProp1.setValue("parentVal1");
    parentFooProperties.add(parentProp1);
    PropertyInfo parentProp12 = new PropertyInfo();
    // overwritten by child
    parentProp12.setName("childName1");
    parentProp12.setValue("parentVal1");
    parentFooProperties.add(parentProp12);

    //BAR
    Collection<PropertyInfo> parentBarProperties = new ArrayList<>();
    PropertyInfo parentProp2 = new PropertyInfo();
    parentProp2.setName("parentName2");
    parentProp2.setValue("parentVal2");
    parentBarProperties.add(parentProp2);

    //OTHER
    Collection<PropertyInfo> parentOtherProperties = new ArrayList<>();
    PropertyInfo parentProp3 = new PropertyInfo();
    parentProp3.setName("parentName3");
    parentProp3.setValue("parentVal3");
    parentOtherProperties.add(parentProp3);

    // create parent config modules
    ConfigurationModule parentConfigModule1 = createConfigurationModule("FOO", parentFooProperties);
    ConfigurationModule parentConfigModule2 = createConfigurationModule("BAR", parentBarProperties);
    ConfigurationModule parentConfigModule3 = createConfigurationModule("OTHER", parentOtherProperties);
    Collection<ConfigurationModule> parentModules = new ArrayList<>();
    parentModules.add(parentConfigModule1);
    parentModules.add(parentConfigModule2);
    parentModules.add(parentConfigModule3);

    // create service modules
    ServiceModule child = createServiceModule(info, childModules);
    ServiceModule parent = createServiceModule(parentInfo, parentModules);

    // resolve child with parent
    resolveService(child, parent);

    // assertions
    List<PropertyInfo> mergedProperties = child.getModuleInfo().getProperties();
    assertEquals(4, mergedProperties.size());

    Map<String, PropertyInfo> mergedPropertyMap = new HashMap<>();
    for (PropertyInfo prop : mergedProperties) {
      mergedPropertyMap.put(prop.getName(), prop);
    }

    // filename is null for all props because that is set in ConfigurationDirectory which is mocked
    assertEquals("childVal1", mergedPropertyMap.get("childName1").getValue());
    assertEquals("childVal2", mergedPropertyMap.get("childName2").getValue());
    assertEquals("parentVal1", mergedPropertyMap.get("parentName1").getValue());
    assertEquals("parentVal3", mergedPropertyMap.get("parentName3").getValue());

    Map<String, Map<String, Map<String, String>>> childAttributes = child.getModuleInfo().getConfigTypeAttributes();
    Map<String, Map<String, Map<String, String>>> parentAttributes = parent.getModuleInfo().getConfigTypeAttributes();

    assertEquals(3, childAttributes.size());
    assertAttributes(childAttributes.get("FOO"), Collections.emptyMap());
    assertAttributes(childAttributes.get("BAR"), attributes);
    assertAttributes(childAttributes.get("OTHER"), Collections.emptyMap());

    assertEquals(3, parentAttributes.size());
    assertAttributes(parentAttributes.get("FOO"), Collections.emptyMap());
    assertAttributes(parentAttributes.get("BAR"), Collections.emptyMap());
    assertAttributes(parentAttributes.get("OTHER"), Collections.emptyMap());
  }

  @Test
  public void testResolve_Service__selection() throws Exception {
    ServiceInfo firstInfo = new ServiceInfo();
    ServiceInfo secondInfo = new ServiceInfo();
    ServiceInfo thirdInfo = new ServiceInfo();

    firstInfo.setSelection(ServiceInfo.Selection.MANDATORY);

    resolveService(secondInfo, firstInfo);

    assertEquals(secondInfo.getSelection(), ServiceInfo.Selection.MANDATORY);

    thirdInfo.setSelection(ServiceInfo.Selection.TECH_PREVIEW);

    resolveService(thirdInfo, secondInfo);

    assertEquals(thirdInfo.getSelection(), ServiceInfo.Selection.TECH_PREVIEW);
  }

  @Test
  public void testResolve_Configuration__attributes() throws Exception {
    ServiceInfo info = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();

    // child configurations
    //FOO
    Collection<PropertyInfo> childFooProperties = new ArrayList<>();
    PropertyInfo childProp1 = new PropertyInfo();
    childProp1.setName("childName1");
    childProp1.setValue("childVal1");
    childFooProperties.add(childProp1);

    // add attributes for parent FOO
    Map<String, String> childFooAttributes = new HashMap<>();
    // override parents value
    childFooAttributes.put(ConfigurationInfo.Supports.ADDING_FORBIDDEN.getXmlAttributeName(), "false");

    // create child config modules
    ConfigurationModule childConfigModule1 = createConfigurationModule("FOO", childFooProperties, childFooAttributes);
    Collection<ConfigurationModule> childModules = new ArrayList<>();
    childModules.add(childConfigModule1);

    // parent configurations
    //FOO
    Collection<PropertyInfo> parentFooProperties = new ArrayList<>();
    PropertyInfo parentProp1 = new PropertyInfo();
    parentProp1.setName("parentName1");
    parentProp1.setValue("parentVal1");
    parentFooProperties.add(parentProp1);

    // add attributes for parent FOO
    Map<String, String> parentFooAttributes = new HashMap<>();
    // child will inherit
    parentFooAttributes.put(ConfigurationInfo.Supports.FINAL.getXmlAttributeName(), "true");
    // child will override
    parentFooAttributes.put(ConfigurationInfo.Supports.ADDING_FORBIDDEN.getXmlAttributeName(), "true");

    //BAR
    Collection<PropertyInfo> parentBarProperties = new ArrayList<>();
    PropertyInfo parentProp2 = new PropertyInfo();
    parentProp2.setName("parentName2");
    parentProp2.setValue("parentVal2");
    parentBarProperties.add(parentProp2);


    // create parent config modules
    ConfigurationModule parentConfigModule1 = createConfigurationModule("FOO", parentFooProperties, parentFooAttributes);
    ConfigurationModule parentConfigModule2 = createConfigurationModule("BAR", parentBarProperties);
    Collection<ConfigurationModule> parentModules = new ArrayList<>();
    parentModules.add(parentConfigModule1);
    parentModules.add(parentConfigModule2);

    // create service modules
    ServiceModule child = createServiceModule(info, childModules);
    ServiceModule parent = createServiceModule(parentInfo, parentModules);

    // resolve child with parent
    resolveService(child, parent);

    // assertions
    Map<String, Map<String, Map<String, String>>> childTypeAttributes = child.getModuleInfo().getConfigTypeAttributes();
    Map<String, Map<String, Map<String, String>>> parentTypeAttributes = parent.getModuleInfo().getConfigTypeAttributes();
    assertTrue(childTypeAttributes.containsKey("FOO"));
    Map<String, Map<String, String>> mergedChildFooAttributes = childTypeAttributes.get("FOO");
    assertTrue(mergedChildFooAttributes.containsKey(ConfigurationInfo.Supports.KEYWORD));
    // inherited value
    assertEquals("true", mergedChildFooAttributes.get(ConfigurationInfo.Supports.KEYWORD).
        get(ConfigurationInfo.Supports.valueOf("FINAL").getPropertyName()));
    // overridden value
    assertEquals("false", mergedChildFooAttributes.get(ConfigurationInfo.Supports.KEYWORD).
        get(ConfigurationInfo.Supports.valueOf("ADDING_FORBIDDEN").getPropertyName()));

    assertEquals(2, childTypeAttributes.size());

    assertEquals(2, parentTypeAttributes.size());
    assertAttributes(parentTypeAttributes.get("FOO"), parentFooAttributes);
    assertAttributes(parentTypeAttributes.get("BAR"), Collections.emptyMap());
  }

  @Test
  public void testResolve_Configuration__ExcludedTypes() throws Exception {
    ServiceInfo info = new ServiceInfo();
    info.setExcludedConfigTypes(Collections.singleton("BAR"));

    //FOO
    Collection<PropertyInfo> fooProperties = new ArrayList<>();
    PropertyInfo prop1 = new PropertyInfo();
    prop1.setName("name1");
    prop1.setValue("val1");
    fooProperties.add(prop1);
    PropertyInfo prop2 = new PropertyInfo();
    prop2.setName("name2");
    prop2.setValue("val2");
    fooProperties.add(prop2);

    //BAR
    Collection<PropertyInfo> barProperties = new ArrayList<>();
    PropertyInfo prop3 = new PropertyInfo();
    prop3.setName("name1");
    prop3.setValue("val3");
    barProperties.add(prop3);

    //OTHER
    Collection<PropertyInfo> otherProperties = new ArrayList<>();
    PropertyInfo prop4 = new PropertyInfo();
    prop4.setName("name1");
    prop4.setValue("val4");
    otherProperties.add(prop4);

    ConfigurationModule configModule1 = createConfigurationModule("FOO", fooProperties);
    ConfigurationModule configModule2 = createConfigurationModule("BAR", barProperties);
    ConfigurationModule configModule3 = createConfigurationModule("OTHER", otherProperties);
    Collection<ConfigurationModule> configModules = new ArrayList<>();
    configModules.add(configModule1);
    configModules.add(configModule2);
    configModules.add(configModule3);

    ServiceModule service = createServiceModule(info, configModules);

    List<PropertyInfo> properties = service.getModuleInfo().getProperties();
    assertEquals(4, properties.size());

    Map<String, Map<String, Map<String, String>>> attributes = service.getModuleInfo().getConfigTypeAttributes();
    assertEquals(2, attributes.size());
    assertTrue(attributes.containsKey("FOO"));
    assertTrue(attributes.containsKey("OTHER"));
  }

  @Test
  public void testResolve_Configuration__ExcludedTypes__ParentType() throws Exception {
    // child
    ServiceInfo info = new ServiceInfo();
    info.setExcludedConfigTypes(Collections.singleton("BAR"));

    //FOO
    Collection<PropertyInfo> fooProperties = new ArrayList<>();
    PropertyInfo prop1 = new PropertyInfo();
    prop1.setName("name1");
    prop1.setValue("val1");
    fooProperties.add(prop1);
    PropertyInfo prop2 = new PropertyInfo();
    prop2.setName("name2");
    prop2.setValue("val2");
    fooProperties.add(prop2);

    ConfigurationModule childConfigModule = createConfigurationModule("FOO", fooProperties);
    Collection<ConfigurationModule> childConfigModules = new ArrayList<>();
    childConfigModules.add(childConfigModule);

    // parent
    ServiceInfo parentInfo = new ServiceInfo();

    //BAR
    Collection<PropertyInfo> barProperties = new ArrayList<>();
    PropertyInfo prop3 = new PropertyInfo();
    prop3.setName("name1");
    prop3.setValue("val3");
    barProperties.add(prop3);

    ConfigurationModule parentConfigModule = createConfigurationModule("BAR", barProperties);
    Collection<ConfigurationModule> parentConfigModules = new ArrayList<>();
    parentConfigModules.add(parentConfigModule);

    // create service modules
    ServiceModule service = createServiceModule(info, childConfigModules);
    ServiceModule parentService = createServiceModule(parentInfo, parentConfigModules);
    // resolve child with parent
    resolveService(service, parentService);
    // assertions
    List<PropertyInfo> properties = service.getModuleInfo().getProperties();
    assertEquals(2, properties.size());

    Map<String, Map<String, Map<String, String>>> attributes = service.getModuleInfo().getConfigTypeAttributes();
    assertEquals(1, attributes.size());
    assertTrue(attributes.containsKey("FOO"));

    Map<String, Map<String, Map<String, String>>> parentAttributes = parentService.getModuleInfo().getConfigTypeAttributes();
    assertEquals(1, parentAttributes.size());
    assertTrue(parentAttributes.containsKey("BAR"));
  }

  @Test
  public void testMerge_Configuration__ExcludedTypes() throws Exception {
    // child
    ServiceInfo info = new ServiceInfo();
    Set<String> childExcludedConfigTypes = new HashSet<>();
    childExcludedConfigTypes.add("FOO");
    info.setExcludedConfigTypes(childExcludedConfigTypes);

    //FOO
    Collection<PropertyInfo> fooProperties = new ArrayList<>();

    ConfigurationModule childConfigModule = createConfigurationModule("FOO", fooProperties);
    Collection<ConfigurationModule> childConfigModules = new ArrayList<>();
    childConfigModules.add(childConfigModule);

    // parent
    ServiceInfo parentInfo = new ServiceInfo();
    Set<String> parentExcludedConfigTypes = new HashSet<>();
    childExcludedConfigTypes.add("BAR");
    info.setExcludedConfigTypes(childExcludedConfigTypes);
    parentInfo.setExcludedConfigTypes(parentExcludedConfigTypes);
    //BAR
    Collection<PropertyInfo> barProperties = new ArrayList<>();


    ConfigurationModule parentConfigModule = createConfigurationModule("BAR", barProperties);
    Collection<ConfigurationModule> parentConfigModules = new ArrayList<>();
    parentConfigModules.add(parentConfigModule);

    // create service modules
    ServiceModule service = createServiceModule(info, childConfigModules);
    ServiceModule parentService = createServiceModule(parentInfo, parentConfigModules);
    // resolve child with parent

    resolveService(service, parentService);

    //resolveService(service, parentService);
    assertEquals(2, service.getModuleInfo().getExcludedConfigTypes().size());
  }

  /**
   * Verify stack resolution for credential-store
   *
   * @throws Exception
   */
  @Test
  public void testResolve_CredentialStoreInfo() throws Exception {
    CredentialStoreInfo credentialStoreInfoChild = new CredentialStoreInfo(true /* supported */, false /* enabled */, true /*required*/);
    CredentialStoreInfo credentialStoreInfoParent = new CredentialStoreInfo(true /* supported */, true /* enabled */, false /*required*/);
    ServiceInfo childInfo = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    ServiceModule service;

    // specified in child only, child wins
    childInfo.setCredentialStoreInfo(credentialStoreInfoChild);
    parentInfo.setCredentialStoreInfo(null);
    service = resolveService(childInfo, parentInfo);
    assertEquals(credentialStoreInfoChild.isSupported(), service.getModuleInfo().isCredentialStoreSupported());
    assertEquals(credentialStoreInfoChild.isEnabled(), service.getModuleInfo().isCredentialStoreEnabled());
    assertEquals(credentialStoreInfoChild.isRequired(), service.getModuleInfo().isCredentialStoreRequired());

    // specified in parent only, parent wins
    childInfo.setCredentialStoreInfo(null);
    parentInfo.setCredentialStoreInfo(credentialStoreInfoParent);
    service = resolveService(childInfo, parentInfo);
    assertEquals(credentialStoreInfoParent.isSupported(), service.getModuleInfo().isCredentialStoreSupported());
    assertEquals(credentialStoreInfoParent.isEnabled(), service.getModuleInfo().isCredentialStoreEnabled());
    assertEquals(credentialStoreInfoParent.isRequired(), service.getModuleInfo().isCredentialStoreRequired());

    // specified in both, child wins
    childInfo.setCredentialStoreInfo(credentialStoreInfoChild);
    parentInfo.setCredentialStoreInfo(credentialStoreInfoParent);
    service = resolveService(childInfo, parentInfo);
    assertEquals(credentialStoreInfoChild.isSupported(), service.getModuleInfo().isCredentialStoreSupported());
    assertEquals(credentialStoreInfoChild.isEnabled(), service.getModuleInfo().isCredentialStoreEnabled());
    assertEquals(credentialStoreInfoChild.isRequired(), service.getModuleInfo().isCredentialStoreRequired());
  }
  /**
   * Verify stack resolution for single sign-on details
   *
   * @throws Exception
   */
  @Test
  public void testResolve_SingleSignOnInfo() throws Exception {
    SingleSignOnInfo singleSignOnInfoChild = new SingleSignOnInfo(false, null, true);
    SingleSignOnInfo singleSignOnInfoParent = new SingleSignOnInfo(true, "config-type/property_name", false);
    ServiceInfo childInfo = new ServiceInfo();
    ServiceInfo parentInfo = new ServiceInfo();
    ServiceModule serviceModule;
    ServiceInfo serviceInfo;

    // specified in child only, child wins
    childInfo.setSingleSignOnInfo(singleSignOnInfoChild);
    parentInfo.setSingleSignOnInfo(null);
    serviceModule = resolveService(childInfo, parentInfo);
    serviceInfo = serviceModule.getModuleInfo();
    assertEquals(singleSignOnInfoChild.isSupported(), serviceInfo.isSingleSignOnSupported());
    assertEquals(singleSignOnInfoChild.isSupported(), serviceInfo.getSingleSignOnInfo().isSupported());
    assertEquals(singleSignOnInfoChild.getSupported(), serviceInfo.getSingleSignOnInfo().getSupported());
    assertEquals(singleSignOnInfoChild.getEnabledConfiguration(), serviceInfo.getSingleSignOnInfo().getEnabledConfiguration());
    assertEquals(singleSignOnInfoChild.getSsoEnabledTest(), serviceInfo.getSingleSignOnInfo().getSsoEnabledTest());
    assertEquals(singleSignOnInfoChild.isKerberosRequired(), serviceInfo.isKerberosRequiredForSingleSignOnIntegration());
    assertEquals(singleSignOnInfoChild.isKerberosRequired(), serviceInfo.getSingleSignOnInfo().isKerberosRequired());

    // specified in parent only, parent wins
    childInfo.setSingleSignOnInfo(null);
    parentInfo.setSingleSignOnInfo(singleSignOnInfoParent);
    serviceModule = resolveService(childInfo, parentInfo);
    serviceInfo = serviceModule.getModuleInfo();
    assertEquals(singleSignOnInfoParent.isSupported(), serviceInfo.isSingleSignOnSupported());
    assertEquals(singleSignOnInfoParent.isSupported(), serviceInfo.getSingleSignOnInfo().isSupported());
    assertEquals(singleSignOnInfoParent.getSupported(), serviceInfo.getSingleSignOnInfo().getSupported());
    assertEquals(singleSignOnInfoParent.getEnabledConfiguration(), serviceInfo.getSingleSignOnInfo().getEnabledConfiguration());
    assertEquals(singleSignOnInfoParent.getSsoEnabledTest(), serviceInfo.getSingleSignOnInfo().getSsoEnabledTest());
    assertEquals(singleSignOnInfoParent.isKerberosRequired(), serviceInfo.isKerberosRequiredForSingleSignOnIntegration());
    assertEquals(singleSignOnInfoParent.isKerberosRequired(), serviceInfo.getSingleSignOnInfo().isKerberosRequired());

    // specified in both, child wins
    childInfo.setSingleSignOnInfo(singleSignOnInfoChild);
    parentInfo.setSingleSignOnInfo(singleSignOnInfoParent);
    serviceModule = resolveService(childInfo, parentInfo);
    serviceInfo = serviceModule.getModuleInfo();
    assertEquals(singleSignOnInfoChild.isSupported(), serviceInfo.isSingleSignOnSupported());
    assertEquals(singleSignOnInfoChild.isSupported(), serviceInfo.getSingleSignOnInfo().isSupported());
    assertEquals(singleSignOnInfoChild.getSupported(), serviceInfo.getSingleSignOnInfo().getSupported());
    assertEquals(singleSignOnInfoChild.getEnabledConfiguration(), serviceInfo.getSingleSignOnInfo().getEnabledConfiguration());
    assertEquals(singleSignOnInfoChild.getSsoEnabledTest(), serviceInfo.getSingleSignOnInfo().getSsoEnabledTest());
    assertEquals(singleSignOnInfoChild.isKerberosRequired(), serviceInfo.isKerberosRequiredForSingleSignOnIntegration());
    assertEquals(singleSignOnInfoChild.isKerberosRequired(), serviceInfo.getSingleSignOnInfo().isKerberosRequired());
  }

  @Test
  public void testServiceCheckRegistered() throws Exception {
    ServiceInfo info = new ServiceInfo();
    info.setName("service1");
    info.setCommandScript(createNiceMock(CommandScriptDefinition.class));

    StackContext context = createStackContext(info.getName(), true);
    ServiceModule service = createServiceModule(info, Collections.emptySet(), context);
    service.finalizeModule();

    verify(context);
  }

  @Test
  public void testServiceCheckNotRegisteredForDeletedService() throws Exception {
    ServiceInfo info = new ServiceInfo();
    info.setName("service1");
    info.setCommandScript(createNiceMock(CommandScriptDefinition.class));
    info.setDeleted(true);

    StackContext context = createStackContext(info.getName(), false);
    ServiceModule service = createServiceModule(info, Collections.emptySet(), context);
    service.finalizeModule();

    verify(context);
  }

  @Test
  public void testInvalidServiceInfo() {
    // Given
    ServiceInfo serviceInfo = new ServiceInfo();
    serviceInfo.setName("TEST_SERVICE");
    serviceInfo.setVersion("1.0.0");
    serviceInfo.setValid(false);
    serviceInfo.addError("Test error message");


    // When
    ServiceModule serviceModule = createServiceModule(serviceInfo);

    // Then
    assertFalse("Service module should be invalid due to the service info being invalid !", serviceModule.isValid());

    assertTrue("Service module error collection should contain error message that caused service info being invalid !", serviceModule.getErrors().contains("Test error message"));
  }


  @Test
  public void testMergeServicePropertiesInheritFromParent() throws Exception {
    // Given
    ServiceInfo serviceInfo = new ServiceInfo();
    ServiceInfo parentServiceInfo = new ServiceInfo();

    ServicePropertyInfo p1 = new ServicePropertyInfo();
    p1.setName("P1");
    p1.setValue("V1");

    ServicePropertyInfo p2 = new ServicePropertyInfo();
    p2.setName("P2");
    p2.setValue("V2");


    List<ServicePropertyInfo> parentServicePropertyList = Lists.newArrayList(p1, p2);

    parentServiceInfo.setServicePropertyList(parentServicePropertyList);


    // When
    ServiceModule serviceModule = resolveService(serviceInfo, parentServiceInfo);

    // Then
    Map<String, String> parentServiceProperties =  ImmutableMap.<String, String>builder()
      .put("P1", "V1")
      .put("P2", "V2")
      .put(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY)
      .put(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY)
      .put(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY)
      .build();


    assertEquals(parentServicePropertyList, serviceModule.getModuleInfo().getServicePropertyList());
    assertEquals(parentServiceProperties, serviceModule.getModuleInfo().getServiceProperties());
  }

  @Test
  public void testMergeServicePropertiesInheritFromEmptyParent() throws Exception {
    // Parent has no properties defined thus no service properties inherited

    // Given
    ServiceInfo serviceInfo = new ServiceInfo();
    ServiceInfo parentServiceInfo = new ServiceInfo();

    ServicePropertyInfo p1 = new ServicePropertyInfo();
    p1.setName("P1");
    p1.setValue("V1");

    ServicePropertyInfo p2 = new ServicePropertyInfo();
    p2.setName("P2");
    p2.setValue("V2");


    List<ServicePropertyInfo> servicePropertyList = Lists.newArrayList(p1, p2);

    serviceInfo.setServicePropertyList(servicePropertyList);


    // When
    ServiceModule serviceModule = resolveService(serviceInfo, parentServiceInfo);

    // Then
    Map<String, String> serviceProperties = ImmutableMap.<String, String>builder()
      .put("P1", "V1")
      .put("P2", "V2")
      .put(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY)
      .put(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY)
      .put(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY)
      .build();

    assertEquals(servicePropertyList, serviceModule.getModuleInfo().getServicePropertyList());
    assertEquals(serviceProperties, serviceModule.getModuleInfo().getServiceProperties());
  }


  @Test
  public void testMergeServiceProperties() throws Exception {
    // Given
    ServiceInfo serviceInfo = new ServiceInfo();
    ServiceInfo parentServiceInfo = new ServiceInfo();

    ServicePropertyInfo p1 = new ServicePropertyInfo();
    p1.setName("P1");
    p1.setValue("V1");

    ServicePropertyInfo p2 = new ServicePropertyInfo();
    p2.setName("P2");
    p2.setValue("V2");

    ServicePropertyInfo p2Override = new ServicePropertyInfo();
    p2Override.setName("P2");
    p2Override.setValue("V2_OVERRIDE");

    ServicePropertyInfo p3 = new ServicePropertyInfo();
    p3.setName("P3");
    p3.setValue("V3");

    List<ServicePropertyInfo> parentServicePropertyList = Lists.newArrayList(p1, p2);
    parentServiceInfo.setServicePropertyList(parentServicePropertyList);

    List<ServicePropertyInfo> servicePropertyList = Lists.newArrayList(p2Override, p3);
    serviceInfo.setServicePropertyList(servicePropertyList);


    // When
    ServiceModule serviceModule = resolveService(serviceInfo, parentServiceInfo);

    // Then
    List<ServicePropertyInfo> expectedPropertyList = Lists.newArrayList(p1, p2Override, p3);
    Map<String, String> expectedServiceProperties = ImmutableMap.<String, String>builder()
      .put("P1", "V1")
      .put("P2", "V2_OVERRIDE")
      .put("P3", "V3")
      .put(ServiceInfo.DEFAULT_SERVICE_INSTALLABLE_PROPERTY)
      .put(ServiceInfo.DEFAULT_SERVICE_MANAGED_PROPERTY)
      .put(ServiceInfo.DEFAULT_SERVICE_MONITORED_PROPERTY)
      .build();

    List<ServicePropertyInfo> actualPropertyList = serviceModule.getModuleInfo().getServicePropertyList();


    assertTrue(actualPropertyList.containsAll(expectedPropertyList) && expectedPropertyList.containsAll(actualPropertyList));
    assertEquals(expectedServiceProperties, serviceModule.getModuleInfo().getServiceProperties());
  }


  private ServiceModule createServiceModule(ServiceInfo serviceInfo) {
    String configType = "type1";

    if (serviceInfo.getName() == null) {
      serviceInfo.setName("service1");
    }

    StackContext context = createStackContext(serviceInfo.getName(), true);
    // no config props
    ConfigurationInfo configInfo = createConfigurationInfo(Collections.emptyList(),
        Collections.emptyMap());

    ConfigurationModule module = createConfigurationModule(configType, configInfo);
    ConfigurationDirectory configDirectory = createConfigurationDirectory(Collections.singletonList(module));
    ServiceDirectory serviceDirectory = createServiceDirectory(serviceInfo.getConfigDir(), configDirectory);

    return createServiceModule(context, serviceInfo, serviceDirectory);
  }

  private ServiceModule createServiceModule(ServiceInfo serviceInfo,
                                            Collection<ConfigurationModule> configurations,
                                            StackContext context) {

    if (serviceInfo.getName() == null) {
      serviceInfo.setName("service1");
    }

    ConfigurationDirectory configDirectory = createConfigurationDirectory(configurations);
    ServiceDirectory serviceDirectory = createServiceDirectory(serviceInfo.getConfigDir(), configDirectory);

    return createServiceModule(context, serviceInfo, serviceDirectory);
  }

  private ServiceModule createServiceModule(ServiceInfo serviceInfo, Collection<ConfigurationModule> configurations) {
    String serviceName = serviceInfo.getName();

    if (serviceInfo.getName() == null) {
      serviceInfo.setName("service1");
    }

    return createServiceModule(serviceInfo, configurations, createStackContext(serviceName, true));
  }

  private ServiceModule createServiceModule(StackContext context, ServiceInfo serviceInfo,
                                            ServiceDirectory serviceDirectory) {

    return new ServiceModule(context, serviceInfo, serviceDirectory);
  }

  private ServiceDirectory createServiceDirectory(String dir, ConfigurationDirectory configDir) {

    ServiceDirectory serviceDirectory = createNiceMock(ServiceDirectory.class);

    expect(serviceDirectory.getConfigurationDirectory(dir, StackDirectory.SERVICE_PROPERTIES_FOLDER_NAME)).andReturn(configDir).anyTimes();
    expect(serviceDirectory.getMetricsFile(anyObject(String.class))).andReturn(new File("testMetricsFile")).anyTimes();
    expect(serviceDirectory.getWidgetsDescriptorFile(anyObject(String.class))).andReturn(new File("testWidgetsFile")).anyTimes();
    expect(serviceDirectory.getAlertsFile()).andReturn(new File("testAlertsFile")).anyTimes();
    expect(serviceDirectory.getKerberosDescriptorFile()).andReturn(new File("testKerberosDescriptorFile")).anyTimes();
    expect(serviceDirectory.getPackageDir()).andReturn("packageDir").anyTimes();
    replay(serviceDirectory);

    return serviceDirectory;
  }

  private ConfigurationDirectory createConfigurationDirectory(Collection<ConfigurationModule> modules) {
    ConfigurationDirectory configDir = createNiceMock(ConfigurationDirectory.class);

    expect(configDir.getConfigurationModules()).andReturn(modules).anyTimes();
    replay(configDir);

    return configDir;
  }

  private ConfigurationModule createConfigurationModule(String configType, ConfigurationInfo info) {
    return new ConfigurationModule(configType, info);
  }

  private ConfigurationModule createConfigurationModule(String configType, Collection<PropertyInfo> properties) {
    ConfigurationInfo info = new ConfigurationInfo(properties, Collections.emptyMap());
    return new ConfigurationModule(configType, info);
  }

  private ConfigurationModule createConfigurationModule(String configType,
                                                        Collection<PropertyInfo> properties,
                                                        Map<String, String> attributes) {

    ConfigurationInfo info = new ConfigurationInfo(properties, attributes);
    return new ConfigurationModule(configType, info);
  }

  private ConfigurationInfo createConfigurationInfo(Collection<PropertyInfo> properties,
                                                    Map<String, String> attributes) {

    return new ConfigurationInfo(properties, attributes);
  }

  private StackContext createStackContext(String serviceName, boolean expectServiceRegistration) {
    StackContext context = createStrictMock(StackContext.class);

    if (expectServiceRegistration) {
      context.registerServiceCheck(serviceName);
    }
    replay(context);

    return context;
  }

  private ServiceModule resolveService(ServiceInfo info, ServiceInfo parentInfo) throws AmbariException {
    ServiceModule service = createServiceModule(info);
    ServiceModule parentService = createServiceModule(parentInfo);

    resolveService(service, parentService);
    return service;
  }

  private void resolveService(ServiceModule service, ServiceModule parent) throws AmbariException {
    service.resolve(parent, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    // during runtime this would be called by the Stack module when it's resolve completed
    service.finalizeModule();
    parent.finalizeModule();
  }

  private void assertAttributes(Map<String, Map<String, String>> mergedAttributes, Map<String, String> specifiedAttributes) {
    assertEquals(1, mergedAttributes.size()); // only supports
    Map<String, String> supportsAttributes = mergedAttributes.get(ConfigurationInfo.Supports.KEYWORD);
    assertEquals(ConfigurationInfo.Supports.values().length, supportsAttributes.size());
    for (Map.Entry<String, String> attribute : supportsAttributes.entrySet()) {
      String attributeName = attribute.getKey();
      String attributeValue = attribute.getValue();

      //need to call toUpper() because propertyName is name().toLowerCase()
      ConfigurationInfo.Supports s = ConfigurationInfo.Supports.valueOf(attributeName.toUpperCase());
      String specifiedVal = specifiedAttributes.get(s.getXmlAttributeName());
      if (specifiedVal != null) {
        assertEquals(specifiedVal, attributeValue);
      } else {
        assertEquals(s.getDefaultValue(), attributeValue);
      }
    }
  }

  private void setPrivateField(Object o, String field, Object value) throws Exception{
    Class<?> c = o.getClass();
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    f.set(o, value);    }
}
