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

package org.apache.ambari.server.view.configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.ambari.view.NoSuchResourceException;
import org.apache.ambari.view.ReadRequest;
import org.apache.ambari.view.ResourceAlreadyExistsException;
import org.apache.ambari.view.ResourceProvider;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.UnsupportedPropertyException;
import org.apache.ambari.view.ViewInstanceDefinition;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;
import org.junit.Assert;
import org.junit.Test;

/**
 * ViewConfig tests.
 */
public class ViewConfigTest {

  private static String xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <description>Description</description>" +
      "    <version>1.0.0</version>\n" +
      "    <build>99</build>\n" +
      "    <data-version>42</data-version>\n" +
      "    <system>true</system>\n" +
      "    <icon64>/this/is/the/icon/url/icon64.png</icon64>\n" +
      "    <icon>/this/is/the/icon/url/icon.png</icon>\n" +
      "    <cluster-config-options>AMBARI-ONLY</cluster-config-options>\n" +
      "    <data-migrator-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyDataMigrator</data-migrator-class>" +
      "    <validator-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyValidator</validator-class>" +
      "    <masker-class>org.apache.ambari.server.view.DefaultMasker</masker-class>" +
      "    <parameter>\n" +
      "        <name>p1</name>\n" +
      "        <description>Parameter 1.</description>\n" +
      "        <label>Label 1.</label>\n" +
      "        <placeholder>Placeholder 1.</placeholder>\n" +
      "        <required>true</required>\n" +
      "    </parameter>\n" +
      "    <parameter>\n" +
      "        <name>p2</name>\n" +
      "        <description>Parameter 2.</description>\n" +
      "        <default-value>Default value 1.</default-value>\n" +
      "        <cluster-config>hdfs-site/dfs.namenode.http-address</cluster-config>\n" +
      "        <required>false</required>\n" +
      "        <masked>true</masked>" +
      "    </parameter>\n" +
      "    <resource>\n" +
      "        <name>resource</name>\n" +
      "        <plural-name>resources</plural-name>\n" +
      "        <id-property>id</id-property>\n" +
      "        <resource-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResource</resource-class>\n" +
      "        <provider-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResourceProvider</provider-class>\n" +
      "        <service-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResourceService</service-class>\n" +
      "        <sub-resource-name>subresource</sub-resource-name>\n" +
      "    </resource>\n" +
      "    <resource>\n" +
      "        <name>subresource</name>\n" +
      "        <plural-name>subresources</plural-name>\n" +
      "        <id-property>id</id-property>\n" +
      "        <resource-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResource</resource-class>\n" +
      "        <provider-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResourceProvider</provider-class>\n" +
      "        <service-class>org.apache.ambari.server.view.configuration.ViewConfigTest$MyResourceService</service-class>\n" +
      "    </resource>\n" +
      "    <auto-instance>\n" +
      "        <name>AUTO-INSTANCE</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "        <description>This is a description.</description>\n" +
      "        <icon64>/this/is/the/icon/url/instance_1_icon64.png</icon64>\n" +
      "        <icon>/this/is/the/icon/url/instance_1_icon.png</icon>\n" +
      "        <property>\n" +
      "            <key>p1</key>\n" +
      "            <value>v1-1</value>\n" +
      "        </property>\n" +
      "        <property>\n" +
      "            <key>p2</key>\n" +
      "            <value>v2-1</value>\n" +
      "        </property>\n" +
      "        <stack-id>HDP-2.0</stack-id>\n" +
      "        <services><service>HIVE</service><service>HDFS</service></services>\n" +
      "    </auto-instance>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE1</name>\n" +
      "        <label>My Instance 1!</label>\n" +
      "        <description>This is a description.</description>\n" +
      "        <icon64>/this/is/the/icon/url/instance_1_icon64.png</icon64>\n" +
      "        <icon>/this/is/the/icon/url/instance_1_icon.png</icon>\n" +
      "        <property>\n" +
      "            <key>p1</key>\n" +
      "            <value>v1-1</value>\n" +
      "        </property>\n" +
      "        <property>\n" +
      "            <key>p2</key>\n" +
      "            <value>v2-1</value>\n" +
      "        </property>\n" +
      "    </instance>\n" +
      "    <instance>\n" +
      "        <name>INSTANCE2</name>\n" +
      "        <label>My Instance 2!</label>\n" +
      "        <property>\n" +
      "            <key>p1</key>\n" +
      "            <value>v1-2</value>\n" +
      "        </property>\n" +
      "    </instance>\n" +
      "</view>";


  private static String minimal_xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "</view>";

  private static String view_class_xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <view-class>ViewImpl</view-class>\n" +
      "</view>";

  private static String system_xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <system>true</system>\n" +
      "</view>";

  private static String non_system_xml = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <system>false</system>\n" +
      "</view>";

  private static String with_ambari_versions = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <min-ambari-version>1.6.1</min-ambari-version>\n" +
      "    <max-ambari-version>2.0.0</max-ambari-version>\n" +
      "</view>";

  private static String EXTRA_CLASSPATH_XML = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <classpath>" +
      "      <path>/var/lib/</path>" +
      "      <path>/tmp/foo.jar</path>" +
      "    </classpath>\n" +
      "</view>";

  @Test
  public void testGetName() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("MY_VIEW", config.getName());
  }

  @Test
  public void testGetLabel() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("My View!", config.getLabel());
  }

  @Test
  public void testGetDescription() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("Description", config.getDescription());
  }

  @Test
  public void testGetVersion() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("1.0.0", config.getVersion());
  }

  @Test
  public void testGetBuild() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("99", config.getBuild());
  }

  @Test
  public void testGetDataVersion() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals(42, config.getDataVersion());

    config = getConfig(minimal_xml);
    Assert.assertEquals(0, config.getDataVersion());
  }

  @Test
  public void testGetIcon() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("/this/is/the/icon/url/icon.png", config.getIcon());
  }

  @Test
  public void testGetIcon64() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("/this/is/the/icon/url/icon64.png", config.getIcon64());
  }

  @Test
  public void testGetValidator() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("org.apache.ambari.server.view.configuration.ViewConfigTest$MyValidator", config.getValidator());
  }

  @Test
  public void testGetDataMigrator() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("org.apache.ambari.server.view.configuration.ViewConfigTest$MyDataMigrator", config.getDataMigrator());
  }

  @Test
  public void testGetDataMigratorClass() throws Exception {
    ViewConfig config = getConfig();
    Class migrator = config.getDataMigratorClass(getClass().getClassLoader());
    Assert.assertEquals(MyDataMigrator.class, migrator);
  }

  @Test
  public void testMasker() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("org.apache.ambari.server.view.DefaultMasker", config.getMasker());
  }

  @Test
  public void testGetView() throws Exception {
    ViewConfig config = getConfig(view_class_xml);
    Assert.assertEquals("ViewImpl", config.getView());
  }

  @Test
  public void testGetParameters() throws Exception {
    ViewConfig config = getConfig();
    List<ParameterConfig> parameters = config.getParameters();
    Assert.assertEquals(2, parameters.size());
    Assert.assertEquals("p1", parameters.get(0).getName());
    Assert.assertEquals("p2", parameters.get(1).getName());

    // check the case where no parameters are specified for the view...
    config = getConfig(minimal_xml);
    parameters = config.getParameters();
    Assert.assertNotNull(parameters);
    Assert.assertEquals(0, parameters.size());
  }

  @Test
  public void testGetResources() throws Exception {
    ViewConfig config = getConfig();
    List<ResourceConfig> resources = config.getResources();
    Assert.assertEquals(2, resources.size());
    Assert.assertEquals("resource", resources.get(0).getName());
    Assert.assertEquals("subresource", resources.get(1).getName());

    // check the case where no resources are specified for the view...
    config = getConfig(minimal_xml);
    resources = config.getResources();
    Assert.assertNotNull(resources);
    Assert.assertEquals(0, resources.size());
  }

  @Test
  public void testGetAutoInstance() throws Exception {
    ViewConfig config = getConfig(xml);
    AutoInstanceConfig instance = config.getAutoInstance();
    Assert.assertEquals("AUTO-INSTANCE", instance.getName());
  }

  @Test
  public void testGetInstances() throws Exception {
    ViewConfig config = getConfig(xml);
    List<InstanceConfig> instances = config.getInstances();
    Assert.assertEquals(2, instances.size());
    Assert.assertEquals("INSTANCE1", instances.get(0).getName());
    Assert.assertEquals("INSTANCE2", instances.get(1).getName());

    // check the case where no instances are specified for the view...
    config = getConfig(minimal_xml);
    instances = config.getInstances();
    Assert.assertNotNull(instances);
    Assert.assertEquals(0, instances.size());
  }

  @Test
  public void testIsSystem() throws Exception {
    ViewConfig config = getConfig(system_xml);
    Assert.assertTrue(config.isSystem());

    config = getConfig(non_system_xml);
    Assert.assertFalse(config.isSystem());
  }

  @Test
  public void testGetExtraClasspath() throws Exception {
    ViewConfig config = getConfig(system_xml);

    Assert.assertNull(config.getExtraClasspath());

    config = getConfig(EXTRA_CLASSPATH_XML);

    Assert.assertEquals("/var/lib/,/tmp/foo.jar", config.getExtraClasspath());
  }

  @Test
  public void testGetMinAmbariVersion() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertNull(config.getMinAmbariVersion());

    config = getConfig(with_ambari_versions);
    Assert.assertEquals("1.6.1", config.getMinAmbariVersion());
  }

  @Test
  public void testGetMaxAmbariVersion() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertNull(config.getMaxAmbariVersion());

    config = getConfig(with_ambari_versions);
    Assert.assertEquals("2.0.0", config.getMaxAmbariVersion());
  }

  @Test
  public void testGetClusterConfigOptions() throws Exception {
    ViewConfig config = getConfig();
    Assert.assertEquals("AMBARI-ONLY",config.getClusterConfigOptions());
  }

  public static  ViewConfig getConfig() throws JAXBException {
      return getConfig(xml);
  }

  public static  ViewConfig getConfig(String xml) throws JAXBException {
    InputStream configStream =  new ByteArrayInputStream(xml.getBytes());
    JAXBContext jaxbContext = JAXBContext.newInstance(ViewConfig.class);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    return (ViewConfig) unmarshaller.unmarshal(configStream);
  }

  public static class MyViewServlet extends HttpServlet {
    // nothing
  }

  public static class MyValidator implements Validator {
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

  public static class MyResource {
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  public static class MyResourceProvider implements ResourceProvider<MyResource> {

    @Override
    public MyResource getResource(String resourceId, Set<String> properties)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return null;
    }

    @Override
    public Set<MyResource> getResources(ReadRequest request)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return null;
    }

    @Override
    public void createResource(String resourceId, Map<String, Object> properties)
        throws SystemException, ResourceAlreadyExistsException, NoSuchResourceException, UnsupportedPropertyException {
    }

    @Override
    public boolean updateResource(String resourceId, Map<String, Object> properties)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return false;
    }

    @Override
    public boolean deleteResource(String resourceId)
        throws SystemException, NoSuchResourceException, UnsupportedPropertyException {
      return false;
    }
  }

  public static class MyResourceService {
    // nothing
  }
}
