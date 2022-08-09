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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.view.ViewSubResourceDefinition;
import org.apache.ambari.server.view.configuration.ResourceConfig;
import org.apache.ambari.server.view.configuration.ResourceConfigTest;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;
import org.junit.Assert;
import org.junit.Test;


/**
 * ViewEntity tests.
 */
public class ViewEntityTest {

  private static String with_ambari_versions = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <min-ambari-version>1.6.1</min-ambari-version>\n" +
      "    <max-ambari-version>2.0.0</max-ambari-version>\n" +
      "</view>";

  public static ViewEntity getViewEntity() throws Exception {
    return getViewEntity(ViewConfigTest.getConfig());
  }

  public static ViewEntity getViewEntity(ViewConfig viewConfig) throws Exception {
    Properties properties = new Properties();
    properties.put("p1", "v1");
    properties.put("p2", "v2");
    properties.put("p3", "v3");

    Configuration ambariConfig = new Configuration(properties);
    ViewEntity viewEntity = new ViewEntity(viewConfig, ambariConfig, "view.jar");

    viewEntity.setClassLoader(ViewEntityTest.class.getClassLoader());

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName(viewEntity.getName());

    viewEntity.setResourceType(resourceTypeEntity);

    long id = 20L;
    for (ViewInstanceEntity viewInstanceEntity : viewEntity.getInstances()) {
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setId(id++);
      resourceEntity.setResourceType(resourceTypeEntity);
      viewInstanceEntity.setResource(resourceEntity);
    }
    return viewEntity;
  }

  @Test
  public void testGetName() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("MY_VIEW{1.0.0}", viewDefinition.getName());
  }

  @Test
  public void testGetCommonName() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("MY_VIEW", viewDefinition.getCommonName());
  }

  @Test
  public void testGetLabel() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("My View!", viewDefinition.getLabel());
  }

  @Test
  public void testGetVersion() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("1.0.0", viewDefinition.getVersion());
  }

  @Test
  public void testGetBuild() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("99", viewDefinition.getBuild());
  }

  @Test
  public void testGetIcon() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("/this/is/the/icon/url/icon.png", viewDefinition.getIcon());

    viewDefinition.setIcon("/a/different/icon.png");
    Assert.assertEquals("/a/different/icon.png", viewDefinition.getIcon());
  }

  @Test
  public void testGetIcon64() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("/this/is/the/icon/url/icon64.png", viewDefinition.getIcon64());

    viewDefinition.setIcon64("/a/different/icon.png");
    Assert.assertEquals("/a/different/icon.png", viewDefinition.getIcon64());
  }

  @Test
  public void testSetGetConfiguration() throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    ViewEntity viewDefinition = getViewEntity(viewConfig);
    Assert.assertEquals(viewConfig, viewDefinition.getConfiguration());

    ViewConfig newViewConfig = ViewConfigTest.getConfig(with_ambari_versions);
    viewDefinition.setConfiguration(newViewConfig);
    Assert.assertEquals(newViewConfig, viewDefinition.getConfiguration());
  }

  @Test
  public void testIsClusterConfigurable() throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    ViewEntity viewDefinition = getViewEntity(viewConfig);
    Assert.assertEquals(viewConfig, viewDefinition.getConfiguration());

    ViewConfig newViewConfig = ViewConfigTest.getConfig();
    viewDefinition.setConfiguration(newViewConfig);
    Assert.assertTrue(viewDefinition.isClusterConfigurable());

    newViewConfig = ViewConfigTest.getConfig(with_ambari_versions);
    viewDefinition.setConfiguration(newViewConfig);
    Assert.assertFalse(viewDefinition.isClusterConfigurable());
  }

  @Test
  public void testGetAmbariProperty() throws Exception {
    ViewConfig viewConfig = ViewConfigTest.getConfig();
    ViewEntity viewDefinition = getViewEntity(viewConfig);
    Assert.assertEquals("v1", viewDefinition.getAmbariProperty("p1"));
    Assert.assertEquals("v2", viewDefinition.getAmbariProperty("p2"));
    Assert.assertEquals("v3", viewDefinition.getAmbariProperty("p3"));
  }

  @Test
  public void testAddGetResourceProvider() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    ResourceProvider provider1 = createNiceMock(ResourceProvider.class);

    Resource.Type type1 = new Resource.Type("myType1");
    viewDefinition.addResourceProvider(type1, provider1);

    Assert.assertEquals(provider1, viewDefinition.getResourceProvider(type1));

    ResourceProvider provider2 = createNiceMock(ResourceProvider.class);

    Resource.Type type2 = new Resource.Type("myType2");
    viewDefinition.addResourceProvider(type2, provider2);

    Assert.assertEquals(provider2, viewDefinition.getResourceProvider(type2));

    Set<Resource.Type> types = viewDefinition.getViewResourceTypes();

    Assert.assertEquals(2, types.size());

    Assert.assertTrue(types.contains(type1));
    Assert.assertTrue(types.contains(type2));
  }

  @Test
  public void testAddGetResourceDefinition() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    ViewSubResourceDefinition definition = createNiceMock(ViewSubResourceDefinition.class);
    Resource.Type type = new Resource.Type("myType");

    expect(definition.getType()).andReturn(type);

    replay(definition);

    viewDefinition.addResourceDefinition(definition);

    Assert.assertEquals(definition, viewDefinition.getResourceDefinition(type));

    verify(definition);
  }

  @Test
  public void testAddGetResourceConfiguration() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    ResourceConfig config = ResourceConfigTest.getResourceConfigs().get(0);

    Resource.Type type1 = new Resource.Type("myType");

    viewDefinition.addResourceConfiguration(type1, config);

    Assert.assertEquals(config, viewDefinition.getResourceConfigurations().get(type1));

    Resource.Type type2 = new Resource.Type("myType2");

    viewDefinition.addResourceConfiguration(type2, config);

    Assert.assertEquals(config, viewDefinition.getResourceConfigurations().get(type2));
  }

  @Test
  public void testAddGetInstanceDefinition() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    ViewInstanceEntity definition = createNiceMock(ViewInstanceEntity.class);

    expect(definition.getName()).andReturn("instance1").anyTimes();

    replay(definition);

    viewDefinition.addInstanceDefinition(definition);

    Assert.assertEquals(definition, viewDefinition.getInstanceDefinition("instance1"));

    Collection<ViewInstanceEntity> definitions = viewDefinition.getInstances();

    Assert.assertEquals(1, definitions.size());

    Assert.assertTrue(definitions.contains(definition));

    verify(definition);
  }

  @Test
  public void testGetClassLoader() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals(ViewEntityTest.class.getClassLoader(), viewDefinition.getClassLoader());
  }

  @Test
  public void testGetArchive() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Assert.assertEquals("view.jar", viewDefinition.getArchive());
  }

  @Test
  public void testGetAmbariConfiguration() throws Exception {
    ViewEntity viewDefinition = getViewEntity();
    Configuration configuration = viewDefinition.getAmbariConfiguration();

    Assert.assertEquals("v1", configuration.getProperty("p1"));
    Assert.assertEquals("v2", configuration.getProperty("p2"));
    Assert.assertEquals("v3", configuration.getProperty("p3"));
  }

  @Test
  public void testGetSetStatus() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    viewDefinition.setStatus(ViewDefinition.ViewStatus.PENDING);
    Assert.assertEquals(ViewDefinition.ViewStatus.PENDING, viewDefinition.getStatus());

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    Assert.assertEquals(ViewDefinition.ViewStatus.DEPLOYING, viewDefinition.getStatus());

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYED);
    Assert.assertEquals(ViewDefinition.ViewStatus.DEPLOYED, viewDefinition.getStatus());

    viewDefinition.setStatus(ViewDefinition.ViewStatus.ERROR);
    Assert.assertEquals(ViewDefinition.ViewStatus.ERROR, viewDefinition.getStatus());
  }

  @Test
  public void testGetSetStatusDetail() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    viewDefinition.setStatusDetail("status detail");
    Assert.assertEquals("status detail", viewDefinition.getStatusDetail());
  }

  @Test
  public void testGetSetValidator() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    Validator validator = new TestValidator();

    viewDefinition.setValidator(validator);
    Assert.assertEquals(validator, viewDefinition.getValidator());
  }

  @Test
  public void testisDeployed() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    viewDefinition.setStatus(ViewDefinition.ViewStatus.PENDING);
    Assert.assertFalse(viewDefinition.isDeployed());

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    Assert.assertFalse(viewDefinition.isDeployed());

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYED);
    Assert.assertTrue(viewDefinition.isDeployed());

    viewDefinition.setStatus(ViewDefinition.ViewStatus.ERROR);
    Assert.assertFalse(viewDefinition.isDeployed());
  }

  @Test
  public void testSetIsSystem() throws Exception {
    ViewEntity viewDefinition = getViewEntity();

    viewDefinition.setSystem(false);
    Assert.assertFalse(viewDefinition.isSystem());

    viewDefinition.setSystem(true);
    Assert.assertTrue(viewDefinition.isSystem());
  }

  public static class TestValidator implements Validator {
    ValidationResult result;

    @Override
    public ValidationResult validateInstance(ViewInstanceDefinition definition, ValidationContext mode) {
      return result;
    }

    @Override
    public ValidationResult validateProperty(String property, ViewInstanceDefinition definition, ValidationContext mode) {
      return result;
    }
  }
}
