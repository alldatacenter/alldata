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

import static org.apache.ambari.server.upgrade.UpgradeCatalog271.CLUSTERS_BLUEPRINT_PROVISIONING_STATE_COLUMN;
import static org.apache.ambari.server.upgrade.UpgradeCatalog271.CLUSTERS_TABLE;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.startsWith;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.state.BlueprintProvisioningState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Injector;

public class UpgradeCatalog271Test {

  @Test
  public void testExecuteDDLUpdates() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    DBAccessor dbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    Capture<DBAccessor.DBColumnInfo> blueprintProvisioningStateColumnCapture = newCapture(CaptureType.ALL);
    dbAccessor.addColumn(eq(CLUSTERS_TABLE), capture(blueprintProvisioningStateColumnCapture));
    expectLastCall().once();

    replay(dbAccessor, injector);

    UpgradeCatalog271 upgradeCatalog271 = new UpgradeCatalog271(injector);
    upgradeCatalog271.dbAccessor = dbAccessor;
    upgradeCatalog271.executeDDLUpdates();

    DBAccessor.DBColumnInfo capturedBlueprintProvisioningStateColumn =
        blueprintProvisioningStateColumnCapture.getValue();
    Assert.assertEquals(CLUSTERS_BLUEPRINT_PROVISIONING_STATE_COLUMN,
        capturedBlueprintProvisioningStateColumn.getName());
    Assert.assertEquals(BlueprintProvisioningState.NONE, capturedBlueprintProvisioningStateColumn.getDefaultValue());
    Assert.assertEquals(String.class, capturedBlueprintProvisioningStateColumn.getType());

    easyMockSupport.verifyAll();

  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateRangerLogDirConfigs = UpgradeCatalog271.class.getDeclaredMethod("updateRangerLogDirConfigs");
    Method updateRangerKmsDbUrl = UpgradeCatalog271.class.getDeclaredMethod("updateRangerKmsDbUrl");
    Method renameAmbariInfraInConfigGroups = UpgradeCatalog271.class.getDeclaredMethod("renameAmbariInfraService");
    Method removeLogSearchPatternConfigs = UpgradeCatalog271.class.getDeclaredMethod("removeLogSearchPatternConfigs");
    Method updateSolrConfigurations = UpgradeCatalog271.class.getDeclaredMethod("updateSolrConfigurations");
    Method updateTimelineReaderAddress = UpgradeCatalog271.class.getDeclaredMethod("updateTimelineReaderAddress");

    UpgradeCatalog271 upgradeCatalog271 = createMockBuilder(UpgradeCatalog271.class)
      .addMockedMethod(updateRangerKmsDbUrl)
      .addMockedMethod(updateRangerLogDirConfigs)
      .addMockedMethod(addNewConfigurationsFromXml)
      .addMockedMethod(renameAmbariInfraInConfigGroups)
      .addMockedMethod(removeLogSearchPatternConfigs)
      .addMockedMethod(updateSolrConfigurations)
      .addMockedMethod(updateTimelineReaderAddress)
      .createMock();

    upgradeCatalog271.addNewConfigurationsFromXml();
    expectLastCall().once();

    upgradeCatalog271.updateRangerLogDirConfigs();
    expectLastCall().once();

    upgradeCatalog271.updateRangerKmsDbUrl();
    expectLastCall().once();

    upgradeCatalog271.renameAmbariInfraService();
    expectLastCall().once();

    upgradeCatalog271.removeLogSearchPatternConfigs();
    expectLastCall().once();

    upgradeCatalog271.updateSolrConfigurations();
    expectLastCall().once();

    upgradeCatalog271.updateTimelineReaderAddress();
    expectLastCall().once();

    replay(upgradeCatalog271);
    upgradeCatalog271.executeDMLUpdates();
    verify(upgradeCatalog271);
  }

  @Test
  public void testRemoveLogSearchPatternConfigs() throws Exception {
    // GIVEN
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    DBAccessor dbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    expect(injector.getInstance(DBAccessor.class)).andReturn(dbAccessor).anyTimes();
    String serviceConfigMapping = "serviceconfigmapping";
    String clusterConfig = "clusterconfig";
    dbAccessor.executeQuery(startsWith("DELETE FROM "+ serviceConfigMapping));
    expectLastCall().once();
    dbAccessor.executeQuery(startsWith("DELETE FROM "+ clusterConfig));
    expectLastCall().once();
    replay(dbAccessor, injector);
    // WHEN
    new UpgradeCatalog271(injector).removeLogSearchPatternConfigs();
    // THEN
    easyMockSupport.verifyAll();

  }

  @Test
  public void testUpdateRangerLogDirConfigs() throws Exception {

    Map<String, Service> installedServices = new HashMap<String, Service>() {
      {
        put("RANGER", null);
      }
    };

    Map<String, String> rangerEnvConfig = new HashMap<String, String>() {
      {
        put("ranger_admin_log_dir", "/var/log/ranger/admin");
        put("ranger_usersync_log_dir", "/var/log/ranger/usersync");
      }
    };

    Map<String, String> oldRangerUgsyncSiteConfig = new HashMap<String, String>() {
      {
        put("ranger.usersync.logdir", "{{usersync_log_dir}}");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .createNiceMock();

    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getClusterName()).andReturn("cl1").anyTimes();
    expect(cluster.getServices()).andReturn(installedServices).atLeastOnce();

    Config mockRangerEnvConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ranger-env")).andReturn(mockRangerEnvConfig).atLeastOnce();
    expect(mockRangerEnvConfig.getProperties()).andReturn(rangerEnvConfig).anyTimes();

    Config mockRangerAdminSiteConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ranger-admin-site")).andReturn(mockRangerAdminSiteConfig).atLeastOnce();
    expect(mockRangerAdminSiteConfig.getProperties()).andReturn(Collections.emptyMap()).anyTimes();

    Config mockRangerUgsyncSiteConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ranger-ugsync-site")).andReturn(mockRangerUgsyncSiteConfig).atLeastOnce();
    expect(mockRangerUgsyncSiteConfig.getProperties()).andReturn(oldRangerUgsyncSiteConfig).anyTimes();

    Capture<Map> rangerAdminpropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(rangerAdminpropertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    Capture<Map> rangerUgsyncPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(rangerUgsyncPropertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    Capture<Map> rangerEnvPropertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(rangerEnvPropertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector, clusters, mockRangerEnvConfig, mockRangerAdminSiteConfig, mockRangerUgsyncSiteConfig, cluster);
    new UpgradeCatalog271(injector).updateRangerLogDirConfigs();
    easyMockSupport.verifyAll();

    Map<String, String> updatedRangerAdminConfig = rangerAdminpropertiesCapture.getValue();
    Assert.assertEquals(updatedRangerAdminConfig.get("ranger.logs.base.dir"), "/var/log/ranger/admin");

    Map<String, String> updatedRangerUgsyncSiteConfig = rangerUgsyncPropertiesCapture.getValue();
    Assert.assertEquals(updatedRangerUgsyncSiteConfig.get("ranger.usersync.logdir"), "/var/log/ranger/usersync");

    Map<String, String> updatedRangerEnvConfig = rangerEnvPropertiesCapture.getValue();
    Assert.assertFalse(updatedRangerEnvConfig.containsKey("ranger_admin_log_dir"));
    Assert.assertFalse(updatedRangerEnvConfig.containsKey("ranger_usersync_log_dir"));
  }

  @Test
  public void testUpdateRangerKmsDbUrl() throws Exception {

    Map<String, Service> installedServices = new HashMap<String, Service>() {
      {
        put("RANGER_KMS", null);
      }
    };

    Map<String, String> rangerKmsPropertiesConfig = new HashMap<String, String>() {
      {
        put("DB_FLAVOR", "MYSQL");
        put("db_host", "c6401.ambari.apache.org");
      }
    };

    Map<String, String> rangerKmsDbksPropertiesConfig = new HashMap<String, String>() {
      {
        put("ranger.ks.jpa.jdbc.url", "jdbc:mysql://c6401.ambari.apache.org:3546");
      }
    };

    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .createNiceMock();

    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getClusterName()).andReturn("cl1").once();
    expect(cluster.getServices()).andReturn(installedServices).atLeastOnce();

    Config mockRangerKmsPropertiesConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("kms-properties")).andReturn(mockRangerKmsPropertiesConfig).atLeastOnce();

    Config mockRangerKmsEnvConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("kms-env")).andReturn(mockRangerKmsEnvConfig).atLeastOnce();

    Config mockRangerKmsDbksConfig = easyMockSupport.createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("dbks-site")).andReturn(mockRangerKmsDbksConfig).atLeastOnce();

    expect(mockRangerKmsPropertiesConfig.getProperties()).andReturn(rangerKmsPropertiesConfig).anyTimes();
    expect(mockRangerKmsEnvConfig.getProperties()).andReturn(Collections.emptyMap()).anyTimes();
    expect(mockRangerKmsDbksConfig.getProperties()).andReturn(rangerKmsDbksPropertiesConfig).anyTimes();

    Capture<Map> propertiesCapture = EasyMock.newCapture();
    expect(controller.createConfig(anyObject(Cluster.class), anyObject(StackId.class), anyString(), capture(propertiesCapture), anyString(),
      anyObject(Map.class))).andReturn(createNiceMock(Config.class)).once();

    replay(controller, injector, clusters, mockRangerKmsPropertiesConfig, mockRangerKmsEnvConfig, mockRangerKmsDbksConfig, cluster);
    new UpgradeCatalog271(injector).updateRangerKmsDbUrl();
    easyMockSupport.verifyAll();

    Map<String, String> updatedRangerKmsEnvConfig = propertiesCapture.getValue();
    Assert.assertEquals(updatedRangerKmsEnvConfig.get("ranger_kms_privelege_user_jdbc_url"), "jdbc:mysql://c6401.ambari.apache.org:3546");
  }

  @Test
  public void testUpdateSolrConfigurations() throws Exception {
    // GIVEN
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);

    Config mockedServiceLogSolrConfig = easyMockSupport.createNiceMock(Config.class);
    Config mockedAudiitLogSolrConfig = easyMockSupport.createNiceMock(Config.class);
    Config mockedSolrLog4JConfig = easyMockSupport.createNiceMock(Config.class);

    Map<String, Config> allDummy = new HashMap<>();

    Map<String, String> serviceLogProps = new HashMap<>();
    serviceLogProps.put("content", "<luceneMatchVersion>7.3.1</luceneMatchVersion>");
    Map<String, String> auditLogProps = new HashMap<>();
    auditLogProps.put("content", "<luceneMatchVersion>7.3.1</luceneMatchVersion>");
    Map<String, String> solrLog4jProps = new HashMap<>();
    solrLog4jProps.put("content", "log4jContent");

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .addMockedMethod("createConfig")
      .createNiceMock();

    DaoUtils daoUtilsMock = easyMockSupport.createNiceMock(DaoUtils.class);
    Map<String, Cluster> clusterMap = new HashMap<>();
    clusterMap.put("cl1", cluster);
    expect(injector.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(injector.getInstance(DaoUtils.class)).andReturn(daoUtilsMock).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getClusters()).andReturn(clusterMap).anyTimes();
    expect(cluster.getDesiredConfigByType("logsearch-service_logs-solrconfig")).andReturn(mockedServiceLogSolrConfig);
    expect(cluster.getDesiredConfigByType("logsearch-audit_logs-solrconfig")).andReturn(mockedAudiitLogSolrConfig);
    expect(cluster.getDesiredConfigByType("infra-solr-log4j")).andReturn(mockedSolrLog4JConfig);
    expect(mockedServiceLogSolrConfig.getProperties()).andReturn(serviceLogProps).anyTimes();
    expect(mockedAudiitLogSolrConfig.getProperties()).andReturn(auditLogProps).anyTimes();
    expect(mockedSolrLog4JConfig.getProperties()).andReturn(solrLog4jProps).anyTimes();
    // WHEN
    replay(daoUtilsMock, controller, injector, clusters, cluster, mockedServiceLogSolrConfig, mockedAudiitLogSolrConfig, mockedSolrLog4JConfig);
    UpgradeCatalog271 underTest = createMockBuilder(UpgradeCatalog271.class)
      .withConstructor(Injector.class)
      .withArgs(injector)
      .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class, Map.class, boolean.class, boolean.class)
      .createNiceMock();
    underTest.updateConfigurationPropertiesForCluster(anyObject(Cluster.class), anyString(), anyObject(), anyBoolean(), anyBoolean());
    expectLastCall().times(3);
    replay(underTest);
    underTest.updateSolrConfigurations();
    // THEN
    easyMockSupport.verifyAll();
  }


}
