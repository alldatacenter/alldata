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

package org.apache.ambari.server.orm.entities;

import static org.easymock.EasyMock.createNiceMock;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.authorization.AmbariAuthorizationFilter;
import org.apache.ambari.server.view.ViewDataMigrationContextImpl;
import org.apache.ambari.server.view.ViewRegistryTest;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.server.view.validation.InstanceValidationResultImpl;
import org.apache.ambari.server.view.validation.ValidationException;
import org.apache.ambari.server.view.validation.ValidationResultImpl;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * ViewInstanceEntity tests.
 */
public class ViewInstanceEntityTest {

  private static String xml_with_instance_label = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <icon>/this/is/the/icon/url/icon.png</icon>\n" +
      "    <icon64>/this/is/the/icon/url/icon64.png</icon64>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "        <description>This is an instance description.</description>\n" +
      "        <visible>true</visible>\n" +
      "        <icon>/this/is/the/icon/url/instance_1_icon.png</icon>\n" +
      "        <icon64>/this/is/the/icon/url/instance_1_icon64.png</icon64>\n" +
      "    </instance>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE2</name>\n" +
      "        <label>My Instance 2!</label>\n" +
      "        <visible>false</visible>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_without_instance_label = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_valid_instance = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <parameter>\n" +
      "        <name>p1</name>\n" +
      "        <description>Parameter 1.</description>\n" +
      "        <required>true</required>\n" +
      "    </parameter>\n" +
      "    <parameter>\n" +
      "        <name>p2</name>\n" +
      "        <description>Parameter 2.</description>\n" +
      "        <required>false</required>\n" +
      "    </parameter>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "        <property>\n" +
      "            <key>p1</key>\n" +
      "            <value>v1-1</value>\n" +
      "        </property>\n" +
      "        <property>\n" +
      "            <key>p2</key>\n" +
      "            <value>v2-1</value>\n" +
      "        </property>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_invalid_instance = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <parameter>\n" +
      "        <name>p1</name>\n" +
      "        <description>Parameter 1.</description>\n" +
      "        <required>true</required>\n" +
      "    </parameter>\n" +
      "    <parameter>\n" +
      "        <name>p2</name>\n" +
      "        <description>Parameter 2.</description>\n" +
      "        <required>false</required>\n" +
      "    </parameter>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_view_with_migrator_v2 = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>2.0.0</version>\n" +
      "    <data-version>1</data-version>\n" +
      "    <data-migrator-class>org.apache.ambari.server.orm.entities.ViewInstanceEntityTest$MyDataMigrator</data-migrator-class>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  private static String xml_view_with_migrator_v1 = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  private static String XML_CONFIG_INSTANCE = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <parameter>\n" +
      "        <name>p1</name>\n" +
      "        <cluster-config>hadoop-env/hdfs_user</cluster-config>\n" +
      "        <required>true</required>\n" +
      "    </parameter>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "    </instance>\n" +
      "</view>";

  @Test
  public void testGetViewEntity() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertEquals(viewDefinition, viewInstanceDefinition.getViewEntity());
  }

  @Test
  public void testGetConfiguration() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertEquals(instanceConfig, viewInstanceDefinition.getConfiguration());
  }

  @Test
  public void testGetId() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Assert.assertNull(viewInstanceDefinition.getViewInstanceId());

    viewInstanceDefinition.setViewInstanceId(99L);
    Assert.assertEquals(99L, (long) viewInstanceDefinition.getViewInstanceId());
  }

  @Test
  public void testGetName() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Assert.assertEquals("INSTANCE1", viewInstanceDefinition.getName());
  }

  @Test
  public void testGetLabel() throws Exception {
    // with an instance label
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertEquals("My Instance 1!", viewInstanceDefinition.getLabel());

    // without an instance label
    instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_without_instance_label).get(0);
    viewDefinition = ViewEntityTest.getViewEntity();
    viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    // should default to view label
    Assert.assertEquals("My View!", viewInstanceDefinition.getLabel());
  }

  @Test
  public void testGetDescription() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertEquals("This is an instance description.", viewInstanceDefinition.getDescription());

    instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_without_instance_label).get(0);
    viewDefinition = ViewEntityTest.getViewEntity();
    viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertNull(viewInstanceDefinition.getDescription());
  }

  @Test
  public void testIsVisible() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertTrue(viewInstanceDefinition.isVisible());

    instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(1);
    viewDefinition = ViewEntityTest.getViewEntity();
    viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertFalse(viewInstanceDefinition.isVisible());

    instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_without_instance_label).get(0);
    viewDefinition = ViewEntityTest.getViewEntity();
    viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);

    Assert.assertTrue(viewInstanceDefinition.isVisible());
  }

  @Test
  public void testGetIcon() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    Assert.assertEquals("/this/is/the/icon/url/instance_1_icon.png", viewInstanceDefinition.getIcon());

    viewInstanceDefinition.setIcon("/a/different/icon.png");
    Assert.assertEquals("/a/different/icon.png", viewInstanceDefinition.getIcon());

    instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(1);
    viewDefinition = ViewEntityTest.getViewEntity();
    viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    Assert.assertEquals("/this/is/the/icon/url/icon.png", viewInstanceDefinition.getIcon());
  }

  @Test
  public void testAlterNames() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    Assert.assertTrue(viewInstanceDefinition.alterNames());

    viewInstanceDefinition.setAlterNames(false);
    Assert.assertFalse(viewInstanceDefinition.alterNames());

    viewInstanceDefinition.setAlterNames(true);
    Assert.assertTrue(viewInstanceDefinition.alterNames());
  }

  @Test
  public void testGetIcon64() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    Assert.assertEquals("/this/is/the/icon/url/instance_1_icon64.png", viewInstanceDefinition.getIcon64());

    viewInstanceDefinition.setIcon64("/a/different/icon.png");
    Assert.assertEquals("/a/different/icon.png", viewInstanceDefinition.getIcon64());

    instanceConfig = InstanceConfigTest.getInstanceConfigs(xml_with_instance_label).get(1);
    viewDefinition = ViewEntityTest.getViewEntity();
    viewInstanceDefinition = new ViewInstanceEntity(viewDefinition, instanceConfig);
    Assert.assertEquals("/this/is/the/icon/url/icon64.png", viewInstanceDefinition.getIcon64());
  }

  @Test
  public void testAddGetProperty() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    viewInstanceDefinition.putProperty("p1", "v1");
    viewInstanceDefinition.putProperty("p2", "v2");
    viewInstanceDefinition.putProperty("p3", "v3");

    Map<String, String> properties = viewInstanceDefinition.getPropertyMap();

    Assert.assertEquals(3, properties.size());

    Assert.assertEquals("v1", properties.get("p1"));
    Assert.assertEquals("v2", properties.get("p2"));
    Assert.assertEquals("v3", properties.get("p3"));
  }

  @Test
  public void testAddGetService() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Object service = new Object();

    viewInstanceDefinition.addService("resources", service);

    Object service2 = new Object();

    viewInstanceDefinition.addService("subresources", service2);

    Assert.assertEquals(service, viewInstanceDefinition.getService("resources"));
    Assert.assertEquals(service2, viewInstanceDefinition.getService("subresources"));
  }

  @Test
  public void testAddGetResourceProvider() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    ResourceProvider provider = createNiceMock(ResourceProvider.class);
    Resource.Type type = new Resource.Type("MY_VIEW{1.0.0}/myType");

    viewInstanceDefinition.addResourceProvider(type, provider);

    Assert.assertEquals(provider, viewInstanceDefinition.getResourceProvider(type));
    Assert.assertEquals(provider, viewInstanceDefinition.getResourceProvider("myType"));
  }

  @Test
  public void testContextPath() throws Exception {
    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity();

    Assert.assertEquals(AmbariAuthorizationFilter.VIEWS_CONTEXT_PATH_PREFIX + "MY_VIEW/1.0.0/INSTANCE1",
        viewInstanceDefinition.getContextPath());
  }

  @Test
  public void testInstanceData() throws Exception {
    TestSecurityHelper securityHelper = new TestSecurityHelper("user1");

    ViewInstanceEntity viewInstanceDefinition = getViewInstanceEntity(securityHelper);

    viewInstanceDefinition.putInstanceData("key1", "foo");

    ViewInstanceDataEntity dataEntity = viewInstanceDefinition.getInstanceData("key1");

    Assert.assertNotNull(dataEntity);

    Assert.assertEquals("foo", dataEntity.getValue());
    Assert.assertEquals("user1", dataEntity.getUser());

    viewInstanceDefinition.putInstanceData("key2", "bar");
    viewInstanceDefinition.putInstanceData("key3", "baz");
    viewInstanceDefinition.putInstanceData("key4", "monkey");
    viewInstanceDefinition.putInstanceData("key5", "runner");

    Map<String, String> dataMap = viewInstanceDefinition.getInstanceDataMap();

    Assert.assertEquals(5, dataMap.size());

    Assert.assertEquals("foo", dataMap.get("key1"));
    Assert.assertEquals("bar", dataMap.get("key2"));
    Assert.assertEquals("baz", dataMap.get("key3"));
    Assert.assertEquals("monkey", dataMap.get("key4"));
    Assert.assertEquals("runner", dataMap.get("key5"));

    viewInstanceDefinition.removeInstanceData("key3");
    dataMap = viewInstanceDefinition.getInstanceDataMap();
    Assert.assertEquals(4, dataMap.size());
    Assert.assertFalse(dataMap.containsKey("key3"));

    securityHelper.setUser("user2");

    dataMap = viewInstanceDefinition.getInstanceDataMap();
    Assert.assertTrue(dataMap.isEmpty());

    viewInstanceDefinition.putInstanceData("key1", "aaa");
    viewInstanceDefinition.putInstanceData("key2", "bbb");
    viewInstanceDefinition.putInstanceData("key3", "ccc");

    dataMap = viewInstanceDefinition.getInstanceDataMap();

    Assert.assertEquals(3, dataMap.size());

    Assert.assertEquals("aaa", dataMap.get("key1"));
    Assert.assertEquals("bbb", dataMap.get("key2"));
    Assert.assertEquals("ccc", dataMap.get("key3"));

    securityHelper.setUser("user1");

    dataMap = viewInstanceDefinition.getInstanceDataMap();
    Assert.assertEquals(4, dataMap.size());

    Assert.assertEquals("foo", dataMap.get("key1"));
    Assert.assertEquals("bar", dataMap.get("key2"));
    Assert.assertNull(dataMap.get("key3"));
    Assert.assertEquals("monkey", dataMap.get("key4"));
    Assert.assertEquals("runner", dataMap.get("key5"));
  }

  public static ViewInstanceEntity getViewInstanceEntity() throws Exception {
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity(viewDefinition, instanceConfig);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName(viewDefinition.getName());

    viewDefinition.setResourceType(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(20L);
    resourceEntity.setResourceType(resourceTypeEntity);
    viewInstanceEntity.setResource(resourceEntity);

    return viewInstanceEntity;
  }

  public static Set<ViewInstanceEntity> getViewInstanceEntities(ViewEntity viewDefinition) throws Exception {
    Set<ViewInstanceEntity> entities = new HashSet<>();

    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);
    entities.add(new ViewInstanceEntity(viewDefinition, instanceConfig));
    instanceConfig = InstanceConfigTest.getInstanceConfigs().get(1);
    entities.add(new ViewInstanceEntity(viewDefinition, instanceConfig));

    return entities;
  }

  @Test
  public void testValidate() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    viewInstanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_CREATE);
  }

  @Test
  public void testDataMigrator() throws Exception {
    Configuration ambariConfig = new Configuration();

    ViewConfig config2 = ViewConfigTest.getConfig(xml_view_with_migrator_v2);
    ViewEntity viewEntity2 = ViewRegistryTest.getViewEntity(config2, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity2 = ViewRegistryTest.getViewInstanceEntity(viewEntity2, config2.getInstances().get(0));

    ViewConfig config1 = ViewConfigTest.getConfig(xml_view_with_migrator_v1);
    ViewEntity viewEntity1 = ViewRegistryTest.getViewEntity(config1, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity1 = ViewRegistryTest.getViewInstanceEntity(viewEntity1, config1.getInstances().get(0));

    ViewDataMigrationContext context = new ViewDataMigrationContextImpl(viewInstanceEntity1, viewInstanceEntity2);
    ViewDataMigrator migrator2 = viewInstanceEntity2.getDataMigrator(context);
    Assert.assertTrue(migrator2 instanceof MyDataMigrator);

    //in the view xml version 1 migrator is not defined
    ViewDataMigrator migrator1 = viewInstanceEntity1.getDataMigrator(context);
    Assert.assertNull(migrator1);
  }

  @Test
  public void testValidateWithClusterConfig() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_CONFIG_INSTANCE);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    viewInstanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_CREATE);
  }

  @Test
  public void testValidateWithValidator() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    ViewEntityTest.TestValidator validator = new ViewEntityTest.TestValidator();
    validator.result = new ValidationResultImpl(true, "detail");
    viewEntity.setValidator(validator);

    viewInstanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_CREATE);
  }

  @Test
  public void testValidate_fail() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    try {
      viewInstanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_CREATE);
      Assert.fail("Expected an IllegalStateException");
    } catch (ValidationException e) {
      // expected
    }
  }

  @Test
  public void testValidateWithValidator_fail() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_invalid_instance);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    ViewEntityTest.TestValidator validator = new ViewEntityTest.TestValidator();
    validator.result = new ValidationResultImpl(false, "detail");
    viewEntity.setValidator(validator);

    try {
      viewInstanceEntity.validate(viewEntity, Validator.ValidationContext.PRE_CREATE);
      Assert.fail("Expected an IllegalStateException");
    } catch (ValidationException e) {
      // expected
    }
  }

  @Test
  public void testGetValidationResult() throws Exception {

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(xml_valid_instance);
    ViewEntity viewEntity = ViewRegistryTest.getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = ViewRegistryTest.getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    ViewEntityTest.TestValidator validator = new ViewEntityTest.TestValidator();
    validator.result = new ValidationResultImpl(true, "detail");
    viewEntity.setValidator(validator);

    InstanceValidationResultImpl result = viewInstanceEntity.getValidationResult(viewEntity, Validator.ValidationContext.PRE_CREATE);

    Map<String, ValidationResult> propertyResults = result.getPropertyResults();

    junit.framework.Assert.assertEquals(2, propertyResults.size());
    junit.framework.Assert.assertTrue(propertyResults.containsKey("p1"));
    junit.framework.Assert.assertTrue(propertyResults.containsKey("p2"));

    junit.framework.Assert.assertTrue(propertyResults.get("p1").isValid());
    junit.framework.Assert.assertTrue(propertyResults.get("p2").isValid());
  }

  public static ViewInstanceEntity getViewInstanceEntity(SecurityHelper securityHelper)
      throws Exception {
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity();
    viewInstanceEntity.setSecurityHelper(securityHelper);
    return viewInstanceEntity;
  }

  public static class MyDataMigrator implements ViewDataMigrator {
    @Override
    public boolean beforeMigration() throws ViewDataMigrationException {
      return true;
    }

    @Override
    public void afterMigration() throws ViewDataMigrationException {
    }

    @Override
    public void migrateEntity(Class originEntityClass, Class currentEntityClass) throws ViewDataMigrationException {
    }

    @Override
    public void migrateInstanceData() throws ViewDataMigrationException {
    }
  }

  protected static class TestSecurityHelper implements SecurityHelper {

    private String user;

    public TestSecurityHelper(String user) {
      this.user = user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    @Override
    public String getCurrentUserName() {
      return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
      return Collections.emptyList();
    }
  }
}
