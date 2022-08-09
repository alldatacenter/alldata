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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import junit.framework.Assert;

/**
 * AutoInstanceConfig tests.
 */
public class AutoInstanceConfigTest {


  private static String VIEW_XML = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <description>Description</description>" +
      "    <version>1.0.0</version>\n" +
      "    <system>true</system>\n" +
      "    <icon64>/this/is/the/icon/url/icon64.png</icon64>\n" +
      "    <icon>/this/is/the/icon/url/icon.png</icon>\n" +
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
      "    <auto-instance>\n" +
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
      "        <stack-id>HDP-2.0</stack-id>\n" +
      "        <services><service>HIVE</service><service>HDFS</service></services>\n" +
      "        <roles><role>CLUSTER.OPERATOR </role><role> CLUSTER.USER</role></roles>\n" +
      "    </auto-instance>\n" +
      "</view>";

  @Test
  public void testGetName() throws Exception {
    AutoInstanceConfig config = getAutoInstanceConfigs(VIEW_XML);

    Assert.assertEquals("INSTANCE1", config.getName());
  }

  @Test
  public void testDescription() throws Exception {
    AutoInstanceConfig config = getAutoInstanceConfigs(VIEW_XML);

    assertEquals("This is a description.", config.getDescription());
  }

  @Test
  public void testGetStackId() throws Exception {
    AutoInstanceConfig config = getAutoInstanceConfigs(VIEW_XML);

    assertEquals("HDP-2.0", config.getStackId());
  }

  @Test
  public void testGetServices() throws Exception {
    AutoInstanceConfig config = getAutoInstanceConfigs(VIEW_XML);
    List<String> serviceNames = config.getServices();

    assertEquals(2, serviceNames.size());
    assertTrue(serviceNames.contains("HIVE"));
    assertTrue(serviceNames.contains("HDFS"));
  }

  @Test
  public void shouldParseClusterInheritedPermissions() throws Exception {
    AutoInstanceConfig config = getAutoInstanceConfigs(VIEW_XML);
    Collection<String> roles = config.getRoles();
    assertEquals(2, roles.size());
    assertTrue(roles.contains("CLUSTER.OPERATOR"));
    assertTrue(roles.contains("CLUSTER.USER"));
  }

  private static AutoInstanceConfig getAutoInstanceConfigs(String xml) throws JAXBException {
    ViewConfig config = ViewConfigTest.getConfig(xml);
    return config.getAutoInstance();
  }
}