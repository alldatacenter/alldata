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
package org.apache.ambari.server.upgrade;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.PropertyUpgradeBehavior;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

public class AbstractUpgradeCatalogTest {
  private static final String CONFIG_TYPE = "hdfs-site.xml";
  private final String CLUSTER_NAME = "c1";
  private final String SERVICE_NAME = "HDFS";
  private AmbariManagementController amc;
  private ConfigHelper configHelper;
  private Injector injector;
  private Cluster cluster;
  private Clusters clusters;
  private ServiceInfo serviceInfo;
  private Config oldConfig;
  private AbstractUpgradeCatalog upgradeCatalog;
  private HashMap<String, String> oldProperties;

  @Before
  public void init() throws AmbariException {
    injector = createNiceMock(Injector.class);
    configHelper = createNiceMock(ConfigHelper.class);
    amc = createNiceMock(AmbariManagementController.class);
    cluster = createNiceMock(Cluster.class);
    clusters = createStrictMock(Clusters.class);
    serviceInfo = createNiceMock(ServiceInfo.class);
    oldConfig = createNiceMock(Config.class);

    expect(injector.getInstance(ConfigHelper.class)).andReturn(configHelper).anyTimes();
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(amc).anyTimes();
    expect(injector.getInstance(Clusters.class)).andReturn(clusters).anyTimes();

    HashMap<String, Cluster> clusterMap = new HashMap<>();
    clusterMap.put(CLUSTER_NAME, cluster);
    expect(clusters.getClusters()).andReturn(clusterMap).anyTimes();

    HashSet<PropertyInfo> stackProperties = new HashSet<>();
    expect(configHelper.getStackProperties(cluster)).andReturn(stackProperties).anyTimes();

    HashMap<String, Service> serviceMap = new HashMap<>();
    serviceMap.put(SERVICE_NAME, null);
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();

    HashSet<PropertyInfo> serviceProperties = new HashSet<>();
    serviceProperties.add(createProperty(CONFIG_TYPE, "prop1", true, false, false));
    serviceProperties.add(createProperty(CONFIG_TYPE, "prop2", false, true, false));
    serviceProperties.add(createProperty(CONFIG_TYPE, "prop3", false, false, true));
    serviceProperties.add(createProperty(CONFIG_TYPE, "prop4", true, false, false));

    expect(configHelper.getServiceProperties(cluster, SERVICE_NAME)).andReturn(serviceProperties).anyTimes();

    expect(configHelper.getPropertyValueFromStackDefinitions(cluster, "hdfs-site", "prop1")).andReturn("v1").anyTimes();
    expect(configHelper.getPropertyValueFromStackDefinitions(cluster, "hdfs-site", "prop2")).andReturn("v2").anyTimes();
    expect(configHelper.getPropertyValueFromStackDefinitions(cluster, "hdfs-site", "prop3")).andReturn("v3").anyTimes();
    expect(configHelper.getPropertyValueFromStackDefinitions(cluster, "hdfs-site", "prop4")).andReturn("v4").anyTimes();
    expect(configHelper.getPropertyOwnerService(eq(cluster), eq("hdfs-site"), anyString())).andReturn(serviceInfo).anyTimes();
    expect(serviceInfo.getName()).andReturn(SERVICE_NAME).anyTimes();

    expect(cluster.getDesiredConfigByType("hdfs-site")).andReturn(oldConfig).anyTimes();
    oldProperties = new HashMap<>();
    expect(oldConfig.getProperties()).andReturn(oldProperties).anyTimes();

    upgradeCatalog = new AbstractUpgradeCatalog(injector) {
      @Override
      protected void executeDDLUpdates() throws AmbariException, SQLException { }

      @Override
      protected void executePreDMLUpdates() throws AmbariException, SQLException { }

      @Override
      protected void executeDMLUpdates() throws AmbariException, SQLException { }

      @Override
      public String getTargetVersion() { return null; }
    };
  }

  @Test
  public void shouldAddConfigurationFromXml() throws AmbariException {

    oldProperties.put("prop1", "v1-old");

    Map<String, Map<String, String>> tags = Collections.emptyMap();
    Map<String, String> mergedProperties = new HashMap<>();
    mergedProperties.put("prop1", "v1-old");
    mergedProperties.put("prop4", "v4");

    expect(amc.createConfig(eq(cluster), anyObject(StackId.class), eq("hdfs-site"), eq(mergedProperties), anyString(), eq(tags))).andReturn(null);

    replay(injector, configHelper, amc, cluster, clusters, serviceInfo, oldConfig);

    upgradeCatalog.addNewConfigurationsFromXml();

    verify(configHelper, amc, cluster, clusters, serviceInfo, oldConfig);
  }

  @Test
  public void shouldUpdateConfigurationFromXml() throws AmbariException {

    oldProperties.put("prop1", "v1-old");
    oldProperties.put("prop2", "v2-old");
    oldProperties.put("prop3", "v3-old");

    Map<String, Map<String, String>> tags = Collections.emptyMap();
    Map<String, String> mergedProperties = new HashMap<>();
    mergedProperties.put("prop1", "v1-old");
    mergedProperties.put("prop2", "v2");
    mergedProperties.put("prop3", "v3-old");

    expect(amc.createConfig(eq(cluster), anyObject(StackId.class), eq("hdfs-site"), eq(mergedProperties), anyString(), eq(tags))).andReturn(null);

    replay(injector, configHelper, amc, cluster, clusters, serviceInfo, oldConfig);

    upgradeCatalog.addNewConfigurationsFromXml();

    verify(configHelper, amc, cluster, clusters, serviceInfo, oldConfig);
  }

  @Test
  public void shouldDeleteConfigurationFromXml() throws AmbariException {

    oldProperties.put("prop1", "v1-old");
    oldProperties.put("prop3", "v3-old");

    Map<String, Map<String, String>> tags = Collections.emptyMap();
    Map<String, String> mergedProperties = new HashMap<>();
    mergedProperties.put("prop1", "v1-old");

    expect(amc.createConfig(eq(cluster), anyObject(StackId.class), eq("hdfs-site"), eq(mergedProperties), anyString(), eq(tags))).andReturn(null);

    replay(injector, configHelper, amc, cluster, clusters, serviceInfo, oldConfig);

    upgradeCatalog.addNewConfigurationsFromXml();

    verify(configHelper, amc, cluster, clusters, serviceInfo, oldConfig);
  }

  private static PropertyInfo createProperty(String filename, String name, boolean add, boolean update, boolean delete) {
    PropertyInfo propertyInfo = new PropertyInfo();
    propertyInfo.setFilename(filename);
    propertyInfo.setName(name);
    propertyInfo.setPropertyAmbariUpgradeBehavior(new PropertyUpgradeBehavior(add, delete, update));
    return propertyInfo;
  }
}