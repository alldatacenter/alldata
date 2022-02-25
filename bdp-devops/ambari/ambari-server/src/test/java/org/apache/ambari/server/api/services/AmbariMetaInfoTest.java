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

package org.apache.ambari.server.api.services;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.metadata.AmbariServiceAlertDefinitions;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.CustomCommandDefinition;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionFactory;
import org.apache.ambari.server.state.alert.MetricSource;
import org.apache.ambari.server.state.alert.PortSource;
import org.apache.ambari.server.state.alert.Reporting;
import org.apache.ambari.server.state.alert.Source;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorFactory;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.state.stack.MetricDefinition;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import junit.framework.Assert;

public class AmbariMetaInfoTest {

  private static final String STACK_NAME_HDP = "HDP";
  private static final String STACK_VERSION_HDP = "0.1";
  private static final String EXT_STACK_NAME = "2.0.6";
  private static final String STACK_VERSION_HDP_02 = "0.2";
  private static final String STACK_MINIMAL_VERSION_HDP = "0.0";
  private static final String SERVICE_NAME_HDFS = "HDFS";
  private static final String SERVICE_NAME_MAPRED2 = "MAPREDUCE2";
  private static final String SERVICE_COMPONENT_NAME = "NAMENODE";
  private static final String OS_TYPE = "centos5";
  private static final String REPO_ID = "HDP-UTILS-1.1.0.15";
  private static final String PROPERTY_NAME = "hbase.regionserver.msginterval";
  private static final String SHARED_PROPERTY_NAME = "content";

  private static final String NON_EXT_VALUE = "XXX";

  private static final int REPOS_CNT = 3;
  private static final int PROPERTIES_CNT = 64;
  private static final int OS_CNT = 4;

  private static TestAmbariMetaInfo metaInfo = null;
  private final static Logger LOG = LoggerFactory.getLogger(AmbariMetaInfoTest.class);
  private static final String FILE_NAME = "hbase-site.xml";
  private static final String HADOOP_ENV_FILE_NAME = "hadoop-env.xml";
  private static final String HDFS_LOG4J_FILE_NAME = "hdfs-log4j.xml";

  //private Injector injector;

  //todo: add fail() for cases where an exception is expected such as getService, getComponent ...


  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @BeforeClass
  public static void beforeClass() throws Exception {
    File stacks = new File("src/test/resources/stacks");
    File version = new File("src/test/resources/version");
    File resourcesRoot = new File("src/test/resources/");

    if (System.getProperty("os.name").contains("Windows")) {
      stacks = new File(ClassLoader.getSystemClassLoader().getResource("stacks").getPath());
      version = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "version");
    }
    metaInfo = createAmbariMetaInfo(stacks, version, resourcesRoot);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabase(metaInfo.injector.getProvider(EntityManager.class).get());
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ActionMetadata.class);
    }
  }


  @Test
  public void getRestartRequiredServicesNames() throws AmbariException {
    Set<String> res = metaInfo.getRestartRequiredServicesNames(STACK_NAME_HDP, "2.0.7");
    assertEquals(1, res.size());
  }

  @Test
  public void testGetRackSensitiveServicesNames() throws AmbariException {
    Set<String> res = metaInfo.getRackSensitiveServicesNames(STACK_NAME_HDP, "2.0.7");
    assertEquals(1, res.size());
    assertEquals("HDFS", res.iterator().next());
  }

  @Test
  public void getComponentsByService() throws AmbariException {
    List<ComponentInfo> components = metaInfo.getComponentsByService(
        STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    assertNotNull(components);
    assertTrue(components.size() > 0);
  }

  @Test
  public void getLogs() throws AmbariException {
    ComponentInfo component;

    component = metaInfo.getComponent(STACK_NAME_HDP, "2.1.1", SERVICE_NAME_HDFS, "NAMENODE");
    assertNotNull(component);
    assertNotNull(component.getLogs());
    assertTrue(component.getLogs().size() == 2);
    assertEquals(component.getLogs().get(0).getLogId(), "hdfs_namenode");
    assertEquals(component.getLogs().get(1).getLogId(), "hdfs_audit");
    assertTrue(component.getLogs().get(0).isPrimary());
    assertFalse(component.getLogs().get(1).isPrimary());

    component = metaInfo.getComponent(STACK_NAME_HDP, "2.1.1", SERVICE_NAME_HDFS, "DATANODE");
    assertNotNull(component);
    assertNotNull(component.getLogs());
    assertTrue(component.getLogs().size() == 1);
    assertEquals(component.getLogs().get(0).getLogId(), "hdfs_datanode");
    assertTrue(component.getLogs().get(0).isPrimary());

    component = metaInfo.getComponent(STACK_NAME_HDP, "2.1.1", SERVICE_NAME_HDFS, "HDFS_CLIENT");
    assertNotNull(component);
    assertNotNull(component.getLogs());
    assertTrue(component.getLogs().isEmpty());
  }

  @Test
  public void getRepository() throws AmbariException {
    Map<String, List<RepositoryInfo>> repository = metaInfo.getRepository(
        STACK_NAME_HDP, STACK_VERSION_HDP);
    assertNotNull(repository);
    assertFalse(repository.get("centos5").isEmpty());
    assertFalse(repository.get("centos6").isEmpty());
  }

  @Test
  @Ignore
  public void testGetRepositoryDefault() throws Exception {
    // Scenario: user has internet and does nothing to repos via api
    // use the latest
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    AmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfo(buildDir);
    // The current stack already has (HDP, 2.1.1, redhat6) with valid latest
    // url
    ambariMetaInfo.init();

    waitForAllReposToBeResolved(ambariMetaInfo);

    List<RepositoryInfo> redhat6Repo = ambariMetaInfo.getRepositories(
        STACK_NAME_HDP, "2.1.1", "redhat6");
    assertNotNull(redhat6Repo);
    for (RepositoryInfo ri : redhat6Repo) {
      if (STACK_NAME_HDP.equals(ri.getRepoName())) {
        assertTrue(ri.getBaseUrl().equals(ri.getDefaultBaseUrl()));
      }
    }
  }

  @Test
  public void testGetRepositoryNoInternetDefault() throws Exception {
    // Scenario: user has no internet and does nothing to repos via api
    // use the default
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    setupTempAmbariMetaInfoDirs(buildDir);
    // The current stack already has (HDP, 2.1.1, redhat6).

    // Deleting the json file referenced by the latestBaseUrl to simulate No
    // Internet.
    File latestUrlFile = new File(buildDir, "ambari-metaInfo/HDP/2.1.1/repos/hdp.json");
    if (System.getProperty("os.name").contains("Windows")) {
      latestUrlFile.deleteOnExit();
    }
    else {
      FileUtils.deleteQuietly(latestUrlFile);
      assertTrue(!latestUrlFile.exists());
    }
    AmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfoExistingDirs(buildDir);

    List<RepositoryInfo> redhat6Repo = ambariMetaInfo.getRepositories(
        STACK_NAME_HDP, "2.1.1", "redhat6");
    assertNotNull(redhat6Repo);
    for (RepositoryInfo ri : redhat6Repo) {
      if (STACK_NAME_HDP.equals(ri.getRepoName())) {
        // baseUrl should be same as defaultBaseUrl since No Internet to load the
        // latestBaseUrl from the json file.
        assertEquals(ri.getBaseUrl(), ri.getDefaultBaseUrl());
      }
    }
  }

  @Test
  public void isSupportedStack() throws AmbariException {
    boolean supportedStack = metaInfo.isSupportedStack(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    assertTrue(supportedStack);

    boolean notSupportedStack = metaInfo.isSupportedStack(NON_EXT_VALUE,
        NON_EXT_VALUE);
    assertFalse(notSupportedStack);
  }

  @Test
  public void isValidService() throws AmbariException {
    boolean valid = metaInfo.isValidService(STACK_NAME_HDP, STACK_VERSION_HDP,
        SERVICE_NAME_HDFS);
    assertTrue(valid);

    boolean invalid = metaInfo.isValidService(STACK_NAME_HDP, STACK_VERSION_HDP, NON_EXT_VALUE);
    assertFalse(invalid);
  }

  @Test
  public void isServiceWithNoConfigs() throws AmbariException {
    assertTrue(metaInfo.isServiceWithNoConfigs(STACK_NAME_HDP, "2.0.8", "SYSTEMML"));
    assertTrue(metaInfo.isServiceWithNoConfigs(STACK_NAME_HDP, "2.0.7", "SYSTEMML"));
    assertFalse(metaInfo.isServiceWithNoConfigs(STACK_NAME_HDP, "2.0.8", "HIVE"));
  }

  @Test
  public void testServiceNameUsingComponentName() throws AmbariException {
    String serviceName = metaInfo.getComponentToService(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_COMPONENT_NAME);
    assertEquals("HDFS", serviceName);
  }

  /**
   * Method: Map<String, ServiceInfo> getServices(String stackName, String
   * version, String serviceName)
   * @throws AmbariException
   */
  @Test
  public void getServices() throws AmbariException {
    Map<String, ServiceInfo> services = metaInfo.getServices(STACK_NAME_HDP,
        STACK_VERSION_HDP);
    LOG.info("Getting all the services ");
    for (Map.Entry<String, ServiceInfo> entry : services.entrySet()) {
      LOG.info("Service Name " + entry.getKey() + " values " + entry.getValue());
    }
    assertTrue(services.containsKey("HDFS"));
    assertTrue(services.containsKey("MAPREDUCE"));
    assertNotNull(services);
    assertFalse(services.keySet().size() == 0);
  }

  /**
   * Method: getServiceInfo(String stackName, String version, String
   * serviceName)
   */
  @Test
  public void getServiceInfo() throws Exception {
    ServiceInfo si = metaInfo.getService(STACK_NAME_HDP, STACK_VERSION_HDP,
        SERVICE_NAME_HDFS);
    assertNotNull(si);
  }

  @Test
  public void testConfigDependencies() throws Exception {
    ServiceInfo serviceInfo = metaInfo.getService(STACK_NAME_HDP, EXT_STACK_NAME,
        SERVICE_NAME_MAPRED2);
    assertNotNull(serviceInfo);
    assertTrue(!serviceInfo.getConfigDependencies().isEmpty());
  }

  @Test
  public void testGetRepos() throws Exception {
    Map<String, List<RepositoryInfo>> repos = metaInfo.getRepository(
        STACK_NAME_HDP, STACK_VERSION_HDP);
    Set<String> centos5Cnt = new HashSet<>();
    Set<String> centos6Cnt = new HashSet<>();
    Set<String> redhat6cnt = new HashSet<>();
    Set<String> redhat5cnt = new HashSet<>();

    for (List<RepositoryInfo> vals : repos.values()) {
      for (RepositoryInfo repo : vals) {
        LOG.debug("Dumping repo info : {}", repo);
        if (repo.getOsType().equals("centos5")) {
          centos5Cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("centos6")) {
          centos6Cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("redhat6")) {
          redhat6cnt.add(repo.getRepoId());
        } else if (repo.getOsType().equals("redhat5")) {
          redhat5cnt.add(repo.getRepoId());
        } else {
          fail("Found invalid os " + repo.getOsType());
        }

        if (repo.getRepoId().equals("epel")) {
          assertFalse(repo.getMirrorsList().isEmpty());
          assertNull(repo.getBaseUrl());
        } else {
          assertNull(repo.getMirrorsList());
          assertFalse(repo.getBaseUrl().isEmpty());
        }
      }
    }

    assertEquals(3, centos5Cnt.size());
    assertEquals(3, redhat6cnt.size());
    assertEquals(3, redhat5cnt.size());
    assertEquals(3, centos6Cnt.size());
  }


  /**
   * Make sure global mapping is avaliable when global.xml is
   * in the path.
   */
  @Test
  public void testGlobalMapping() throws Exception {
    ServiceInfo sinfo = metaInfo.getService("HDP",
        "0.2", "HDFS");
    List<PropertyInfo> pinfo = sinfo.getProperties();
    // check all the config knobs and make sure the global one is there
    boolean checkforglobal = false;

    for (PropertyInfo pinfol: pinfo) {
      if ("global.xml".equals(pinfol.getFilename())) {
        checkforglobal = true;
      }
    }
    Assert.assertTrue(checkforglobal);
    sinfo = metaInfo.getService("HDP",
        "0.2", "MAPREDUCE");
    boolean checkforhadoopheapsize = false;
    pinfo = sinfo.getProperties();
    for (PropertyInfo pinfol: pinfo) {
      if ("global.xml".equals(pinfol.getFilename())) {
        if ("hadoop_heapsize".equals(pinfol.getName())) {
          checkforhadoopheapsize = true;
        }
      }
    }
    Assert.assertTrue(checkforhadoopheapsize);
  }

  @Test
  public void testMetaInfoFileFilter() throws Exception {
    String buildDir = tmpFolder.getRoot().getAbsolutePath();
    File stackRoot = new File("src/test/resources/stacks");
    File version = new File("src/test/resources/version");
    if (System.getProperty("os.name").contains("Windows")) {
      stackRoot = new File(ClassLoader.getSystemClassLoader().getResource("stacks").getPath());
      version = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "version");
    }
    File stackRootTmp = getStackRootTmp(buildDir);
    stackRootTmp.mkdir();
    FileUtils.copyDirectory(stackRoot, stackRootTmp);
    //todo
    //ambariMetaInfo.injector = injector;
    File f1, f2, f3;
    f1 = new File(stackRootTmp.getAbsolutePath() + "/001.svn"); f1.createNewFile();
    f2 = new File(stackRootTmp.getAbsolutePath() + "/abcd.svn/001.svn"); f2.mkdirs(); f2.createNewFile();
    f3 = new File(stackRootTmp.getAbsolutePath() + "/.svn");
    if (!f3.exists()) {
      f3.createNewFile();
    }

    AmbariMetaInfo ambariMetaInfo = createAmbariMetaInfo(stackRootTmp, version, new File(""));

    // Tests the stack is loaded as expected
    getServices();
    getComponentsByService();
    // Check .svn is not part of the stack but abcd.svn is
    Assert.assertNotNull(ambariMetaInfo.getStack("abcd.svn", "001.svn"));

    Assert.assertFalse(ambariMetaInfo.isSupportedStack(".svn", ""));
    Assert.assertFalse(ambariMetaInfo.isSupportedStack(".svn", ""));
  }

  @Test
  public void testGetComponent() throws Exception {
    ComponentInfo component = metaInfo.getComponent(STACK_NAME_HDP,
        STACK_VERSION_HDP, SERVICE_NAME_HDFS, SERVICE_COMPONENT_NAME);
    Assert.assertEquals(component.getName(), SERVICE_COMPONENT_NAME);

    try {
      metaInfo.getComponent(STACK_NAME_HDP,
          STACK_VERSION_HDP, SERVICE_NAME_HDFS, NON_EXT_VALUE);
    } catch (StackAccessException e) {
    }

  }

  @Test
  public void testGetRepositories() throws Exception {
    List<RepositoryInfo> repositories = metaInfo.getRepositories(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE);
    Assert.assertEquals(repositories.size(), REPOS_CNT);
  }

  @Test
  public void testGetRepository() throws Exception {
    RepositoryInfo repository = metaInfo.getRepository(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE, REPO_ID);
    Assert.assertEquals(repository.getRepoId(), REPO_ID);

    try {
      metaInfo.getRepository(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE, NON_EXT_VALUE);
    } catch (StackAccessException e) {
    }
  }

  @Test
  public void testGetService() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    Assert.assertEquals(service.getName(), SERVICE_NAME_HDFS);
    try {
      metaInfo.getService(STACK_NAME_HDP, STACK_VERSION_HDP, NON_EXT_VALUE);
    } catch (StackAccessException e) {
    }

  }

  @Test
  public void testGetStacks() {
    //Collection<StackInfo> stacks = metaInfo.getStacks();
    //todo: complete test
  }

  @Test
  public void testGetStackInfo() throws Exception {
    StackInfo stackInfo = metaInfo.getStack(STACK_NAME_HDP, STACK_VERSION_HDP);
    Assert.assertEquals(stackInfo.getName(), STACK_NAME_HDP);
    Assert.assertEquals(stackInfo.getVersion(), STACK_VERSION_HDP);
    try {
      metaInfo.getStack(STACK_NAME_HDP, NON_EXT_VALUE);
    } catch (StackAccessException e) {
    }
  }

  @Test
  public void testGetStackParentVersions() throws Exception {
    List<String> parents = metaInfo.getStackParentVersions(STACK_NAME_HDP, "2.0.8");
    Assert.assertEquals(3, parents.size());
    Assert.assertEquals("2.0.7", parents.get(0));
    Assert.assertEquals("2.0.6", parents.get(1));
    Assert.assertEquals("2.0.5", parents.get(2));
  }

  @Test
  public void testGetProperties() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getServiceProperties(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS);
    Assert.assertEquals(properties.size(), PROPERTIES_CNT);
  }

  @Test
  public void testGetPropertiesNoName() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getPropertiesByName(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS, PROPERTY_NAME);
    Assert.assertEquals(1, properties.size());
    for (PropertyInfo propertyInfo : properties) {
      Assert.assertEquals(PROPERTY_NAME, propertyInfo.getName());
      Assert.assertEquals(FILE_NAME, propertyInfo.getFilename());
    }

    try {
      metaInfo.getPropertiesByName(STACK_NAME_HDP, STACK_VERSION_HDP, SERVICE_NAME_HDFS, NON_EXT_VALUE);
    } catch (StackAccessException e) {
    }

  }

  @Test
  public void testGetPropertiesSharedName() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getPropertiesByName(STACK_NAME_HDP, STACK_VERSION_HDP_02, SERVICE_NAME_HDFS, SHARED_PROPERTY_NAME);
    Assert.assertEquals(2, properties.size());
    for (PropertyInfo propertyInfo : properties) {
      Assert.assertEquals(SHARED_PROPERTY_NAME, propertyInfo.getName());
      Assert.assertTrue(propertyInfo.getFilename().equals(HADOOP_ENV_FILE_NAME)
        || propertyInfo.getFilename().equals(HDFS_LOG4J_FILE_NAME));
    }
  }

  @Test
  public void testGetOperatingSystems() throws Exception {
    Set<OperatingSystemInfo> operatingSystems = metaInfo.getOperatingSystems(STACK_NAME_HDP, STACK_VERSION_HDP);
    Assert.assertEquals(OS_CNT, operatingSystems.size());
  }

  @Test
  public void testGetOperatingSystem() throws Exception {
    OperatingSystemInfo operatingSystem = metaInfo.getOperatingSystem(STACK_NAME_HDP, STACK_VERSION_HDP, OS_TYPE);
    Assert.assertEquals(operatingSystem.getOsType(), OS_TYPE);


    try {
      metaInfo.getOperatingSystem(STACK_NAME_HDP, STACK_VERSION_HDP, NON_EXT_VALUE);
    } catch (StackAccessException e) {
    }
  }

  @Test
  public void isOsSupported() throws Exception {
    Assert.assertTrue(metaInfo.isOsSupported("redhat5"));
    Assert.assertTrue(metaInfo.isOsSupported("centos5"));
    Assert.assertTrue(metaInfo.isOsSupported("oraclelinux5"));
    Assert.assertTrue(metaInfo.isOsSupported("redhat6"));
    Assert.assertTrue(metaInfo.isOsSupported("centos6"));
    Assert.assertTrue(metaInfo.isOsSupported("oraclelinux6"));
    Assert.assertTrue(metaInfo.isOsSupported("suse11"));
    Assert.assertTrue(metaInfo.isOsSupported("sles11"));
    Assert.assertTrue(metaInfo.isOsSupported("ubuntu12"));
    Assert.assertTrue(metaInfo.isOsSupported("win2008server6"));
    Assert.assertTrue(metaInfo.isOsSupported("win2008serverr26"));
    Assert.assertTrue(metaInfo.isOsSupported("win2012server6"));
    Assert.assertTrue(metaInfo.isOsSupported("win2012serverr26"));
  }

  @Test
  public void testExtendedStackDefinition() throws Exception {
    StackInfo stackInfo = metaInfo.getStack(STACK_NAME_HDP, EXT_STACK_NAME);
    Assert.assertTrue(stackInfo != null);
    Collection<ServiceInfo> serviceInfos = stackInfo.getServices();
    Assert.assertFalse(serviceInfos.isEmpty());
    Assert.assertTrue(serviceInfos.size() > 1);
    ServiceInfo deletedService = null;
    ServiceInfo redefinedService = null;
    for (ServiceInfo serviceInfo : serviceInfos) {
      if (serviceInfo.getName().equals("SQOOP")) {
        deletedService = serviceInfo;
      }
      if (serviceInfo.getName().equals("YARN")) {
        redefinedService = serviceInfo;
      }
    }
    Assert.assertNull("SQOOP is a deleted service, should not be a part of " +
      "the extended stack.", deletedService);
    Assert.assertNotNull(redefinedService);
    // Components
    Assert.assertEquals("YARN service is expected to be defined with 4 active" +
      " components.", 4, redefinedService.getComponents().size());
    Assert.assertEquals("TEZ is expected to be a part of extended stack " +
      "definition", "TEZ", redefinedService.getClientComponent().getName());
    Assert.assertFalse("YARN CLIENT is a deleted component.",
      redefinedService.getClientComponent().getName().equals("YARN_CLIENT"));
    // Properties
    Assert.assertNotNull(redefinedService.getProperties());
    Assert.assertTrue(redefinedService.getProperties().size() > 4);
    PropertyInfo deleteProperty1 = null;
    PropertyInfo deleteProperty2 = null;
    PropertyInfo redefinedProperty1 = null;
    PropertyInfo redefinedProperty2 = null;
    PropertyInfo redefinedProperty3 = null;
    PropertyInfo inheritedProperty = null;
    PropertyInfo newProperty = null;
    PropertyInfo newEnhancedProperty = null;
    PropertyInfo propertyWithExtraValueAttributes = null;
    PropertyInfo originalProperty = null;

    PropertyDependencyInfo propertyDependencyInfo =
      new PropertyDependencyInfo("yarn-site", "new-enhanced-yarn-property");

    for (PropertyInfo propertyInfo : redefinedService.getProperties()) {
      if (propertyInfo.getName().equals("yarn.resourcemanager.resource-tracker.address")) {
        deleteProperty1 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.scheduler.address")) {
        deleteProperty2 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.address")) {
        redefinedProperty1 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.resourcemanager.admin.address")) {
        redefinedProperty2 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.nodemanager.health-checker.interval-ms")) {
        redefinedProperty3 = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.nodemanager.address")) {
        inheritedProperty = propertyInfo;
      } else if (propertyInfo.getName().equals("new-yarn-property")) {
        newProperty = propertyInfo;
      } else if (propertyInfo.getName().equals("new-enhanced-yarn-property")) {
        newEnhancedProperty = propertyInfo;
      } else if (propertyInfo.getName().equals("yarn.nodemanager.aux-services")) {
        originalProperty = propertyInfo;
      } else if (propertyInfo.getName().equals("property.with.extra.value.attributes")) {
        propertyWithExtraValueAttributes = propertyInfo;
      }
    }

    Assert.assertNull(deleteProperty1);
    Assert.assertNull(deleteProperty2);
    Assert.assertNotNull(redefinedProperty1);
    Assert.assertNotNull(redefinedProperty2);
    Assert.assertNotNull("yarn.nodemanager.address expected to be inherited " +
      "from parent", inheritedProperty);
    Assert.assertEquals("localhost:100009", redefinedProperty1.getValue());
    // Parent property value will result in property being present in the child stack
    Assert.assertNotNull(redefinedProperty3);
    Assert.assertEquals("135000", redefinedProperty3.getValue());
    // Child can override parent property to empty value
    Assert.assertEquals("", redefinedProperty2.getValue());
    // New property
    Assert.assertNotNull(newProperty);
    Assert.assertEquals("some-value", newProperty.getValue());
    Assert.assertEquals("some description.", newProperty.getDescription());
    Assert.assertEquals("yarn-site.xml", newProperty.getFilename());
    Assert.assertEquals(1, newProperty.getDependedByProperties().size());
    Assert.assertTrue(newProperty.getDependedByProperties().contains(propertyDependencyInfo));
    // New enhanced property
    Assert.assertNotNull(newEnhancedProperty);
    Assert.assertEquals("1024", newEnhancedProperty.getValue());
    Assert.assertEquals("some enhanced description.", newEnhancedProperty.getDescription());
    Assert.assertEquals("yarn-site.xml", newEnhancedProperty.getFilename());
    Assert.assertEquals(2, newEnhancedProperty.getDependsOnProperties().size());
    Assert.assertTrue(newEnhancedProperty.getDependsOnProperties().contains(new PropertyDependencyInfo("yarn-site", "new-yarn-property")));
    Assert.assertTrue(newEnhancedProperty.getDependsOnProperties().contains(new PropertyDependencyInfo("global", "yarn_heapsize")));
    Assert.assertEquals("MB", newEnhancedProperty.getPropertyValueAttributes().getUnit());
    Assert.assertEquals("int", newEnhancedProperty.getPropertyValueAttributes().getType());
    Assert.assertEquals("512", newEnhancedProperty.getPropertyValueAttributes().getMinimum());
    Assert.assertEquals("15360", newEnhancedProperty.getPropertyValueAttributes().getMaximum());
    Assert.assertEquals("256", newEnhancedProperty.getPropertyValueAttributes().getIncrementStep());
    Assert.assertNull(newEnhancedProperty.getPropertyValueAttributes().getEntries());
    Assert.assertNull(newEnhancedProperty.getPropertyValueAttributes().getEntriesEditable());

    // property with extra value attributes
    Assert.assertTrue(propertyWithExtraValueAttributes.getPropertyValueAttributes().getEmptyValueValid());
    Assert.assertTrue(propertyWithExtraValueAttributes.getPropertyValueAttributes().getVisible());
    Assert.assertTrue(propertyWithExtraValueAttributes.getPropertyValueAttributes().getReadOnly());
    Assert.assertEquals(Boolean.FALSE, propertyWithExtraValueAttributes.getPropertyValueAttributes().getEditableOnlyAtInstall());
    Assert.assertEquals(Boolean.FALSE, propertyWithExtraValueAttributes.getPropertyValueAttributes().getOverridable());
    Assert.assertEquals(Boolean.FALSE, propertyWithExtraValueAttributes.getPropertyValueAttributes().getShowPropertyName());

    // Original property
    Assert.assertNotNull(originalProperty);
    Assert.assertEquals("mapreduce.shuffle", originalProperty.getValue());
    Assert.assertEquals("Auxilliary services of NodeManager",
      originalProperty.getDescription());
    Assert.assertEquals(6, redefinedService.getConfigDependencies().size());
    Assert.assertEquals(7, redefinedService.getConfigDependenciesWithComponents().size());
  }

  @Test
  public void testPropertyCount() throws Exception {
    Set<PropertyInfo> properties = metaInfo.getServiceProperties(STACK_NAME_HDP, STACK_VERSION_HDP_02, SERVICE_NAME_HDFS);
    // 3 empty properties
    Assert.assertEquals(103, properties.size());
  }

  @Test
  public void testBadStack() throws Exception {
    File stackRoot = new File("src/test/resources/bad-stacks");
    File version = new File("src/test/resources/version");
    if (System.getProperty("os.name").contains("Windows")) {
      stackRoot = new File(ClassLoader.getSystemClassLoader().getResource("bad-stacks").getPath());
      version = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "version");
    }
    LOG.info("Stacks file " + stackRoot.getAbsolutePath());


    TestAmbariMetaInfo ambariMetaInfo = createAmbariMetaInfo(stackRoot, version, new File(""));
    Assert.assertEquals(1, ambariMetaInfo.getStackManager().getStacks().size());
    Assert.assertEquals(false, ambariMetaInfo.getStackManager().getStack("HDP", "0.1").isValid());
    Assert.assertEquals(2, ambariMetaInfo.getStackManager().getStack("HDP", "0.1").getErrors().size());

  }

  @Test
  public void testMetricsJson() throws Exception {
    ServiceInfo svc = metaInfo.getService(STACK_NAME_HDP, "2.0.5", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getMetricsFile());

    svc = metaInfo.getService(STACK_NAME_HDP, "2.0.6", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getMetricsFile());

    List<MetricDefinition> list = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.5", "HDFS", SERVICE_COMPONENT_NAME, Resource.Type.Component.name());
    Assert.assertNotNull(list);
    checkNoAggregatedFunctionsForJmx(list);

    list = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.5", "HDFS", "DATANODE", Resource.Type.Component.name());
    Assert.assertNull(list);

    List<MetricDefinition> list0 = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.5", "HDFS", "DATANODE", Resource.Type.Component.name());
    Assert.assertNull(list0);
    Assert.assertTrue("Expecting subsequent calls to use a cached value for the definition", list == list0);

    // not explicitly defined, uses 2.0.5
    list = metaInfo.getMetrics(STACK_NAME_HDP, "2.0.6", "HDFS", "DATANODE", Resource.Type.Component.name());
    Assert.assertNull(list);
  }

  @Test
  public void testKerberosJson() throws Exception {
    ServiceInfo svc;

    svc = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertNotNull(svc);

    File kerberosDescriptorFile1 = svc.getKerberosDescriptorFile();
    Assert.assertNotNull(kerberosDescriptorFile1);
    Assert.assertTrue(kerberosDescriptorFile1.exists());

    svc = metaInfo.getService(STACK_NAME_HDP, "2.1.1", "HDFS");
    Assert.assertNotNull(svc);

    File kerberosDescriptorFile2 = svc.getKerberosDescriptorFile();
    Assert.assertNotNull(kerberosDescriptorFile1);
    Assert.assertTrue(kerberosDescriptorFile1.exists());

    Assert.assertEquals(kerberosDescriptorFile1, kerberosDescriptorFile2);

    svc = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertNotNull(svc);

    File kerberosDescriptorFile3 = svc.getKerberosDescriptorFile();
    Assert.assertNull(kerberosDescriptorFile3);
  }

  @Test
  public void testGanglia134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "GANGLIA");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(2, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("GANGLIA_SERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("GANGLIA_MONITOR")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertTrue(component.getAutoDeploy().isEnabled());
        // cardinality
        Assert.assertEquals("ALL", component.getCardinality());
      }
    }
  }

  @Test
  public void testHBase134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HBASE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(3, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("HBASE_MASTER")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(2, dependencyList.size());
        for (DependencyInfo dependency : dependencyList) {
          if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else if (dependency.getName().equals("ZOOKEEPER/ZOOKEEPER_SERVER")) {
            Assert.assertEquals("cluster", dependency.getScope());
            AutoDeployInfo autoDeploy = dependency.getAutoDeploy();
            Assert.assertEquals(true, autoDeploy.isEnabled());
            Assert.assertEquals("HBASE/HBASE_MASTER", autoDeploy.getCoLocate());
          } else {
            Assert.fail("Unexpected dependency");
          }
        }
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HBASE_REGIONSERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1+", component.getCardinality());
      }
      if (name.equals("HBASE_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testHDFS134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HDFS");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(4, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("NAMENODE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("DATANODE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1+", component.getCardinality());
      }
      if (name.equals("SECONDARY_NAMENODE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HDFS_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testHive134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HIVE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(4, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("HIVE_METASTORE")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        AutoDeployInfo autoDeploy = component.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("HIVE/HIVE_SERVER", autoDeploy.getCoLocate());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HIVE_SERVER")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(1, dependencyList.size());
        DependencyInfo dependency = dependencyList.get(0);
        Assert.assertEquals("ZOOKEEPER/ZOOKEEPER_SERVER", dependency.getName());
        Assert.assertEquals("cluster", dependency.getScope());
        AutoDeployInfo autoDeploy = dependency.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("HIVE/HIVE_SERVER", autoDeploy.getCoLocate());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("MYSQL_SERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        AutoDeployInfo autoDeploy = component.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("HIVE/HIVE_SERVER", autoDeploy.getCoLocate());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("HIVE_CLIENT")) {
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testHue134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HUE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("HUE_SERVER", component.getName());
    // dependencies
    Assert.assertEquals(0, component.getDependencies().size());
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("1", component.getCardinality());
  }

  @Test
  public void testMapReduce134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "MAPREDUCE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(4, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("JOBTRACKER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("TASKTRACKER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1+", component.getCardinality());
      }
      if (name.equals("HISTORYSERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        AutoDeployInfo autoDeploy = component.getAutoDeploy();
        Assert.assertTrue(autoDeploy.isEnabled());
        Assert.assertEquals("MAPREDUCE/JOBTRACKER", autoDeploy.getCoLocate());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("MAPREDUCE_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testOozie134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "OOZIE");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(2, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("OOZIE_SERVER")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(2, dependencyList.size());
        for (DependencyInfo dependency : dependencyList) {
          if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else {
            Assert.fail("Unexpected dependency");
          }
        }
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("OOZIE_CLIENT")) {
        // dependencies
        List<DependencyInfo> dependencyList = component.getDependencies();
        Assert.assertEquals(2, dependencyList.size());
        for (DependencyInfo dependency : dependencyList) {
          if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
            Assert.assertEquals("host", dependency.getScope());
            Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
          } else {
            Assert.fail("Unexpected dependency");
          }
        }
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testPig134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "PIG");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("PIG", component.getName());
    // dependencies
    Assert.assertEquals(0, component.getDependencies().size());
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("0+", component.getCardinality());
  }

  @Test
  public void testSqoop134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "SQOOP");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("SQOOP", component.getName());
    // dependencies
    List<DependencyInfo> dependencyList = component.getDependencies();
    Assert.assertEquals(2, dependencyList.size());
    for (DependencyInfo dependency : dependencyList) {
      if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else {
        Assert.fail("Unexpected dependency");
      }
    }
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("0+", component.getCardinality());
  }

  @Test
  public void testWebHCat134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "WEBHCAT");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(1, componentList.size());
    ComponentInfo component = componentList.get(0);
    Assert.assertEquals("WEBHCAT_SERVER", component.getName());
    // dependencies
    List<DependencyInfo> dependencyList = component.getDependencies();
    Assert.assertEquals(4, dependencyList.size());
    for (DependencyInfo dependency : dependencyList) {
      if (dependency.getName().equals("HDFS/HDFS_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else if (dependency.getName().equals("MAPREDUCE/MAPREDUCE_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      } else if (dependency.getName().equals("ZOOKEEPER/ZOOKEEPER_SERVER")) {
        Assert.assertEquals("cluster", dependency.getScope());
        AutoDeployInfo autoDeploy = dependency.getAutoDeploy();
        Assert.assertEquals(true, autoDeploy.isEnabled());
        Assert.assertEquals("WEBHCAT/WEBHCAT_SERVER", autoDeploy.getCoLocate());
      }else if (dependency.getName().equals("ZOOKEEPER/ZOOKEEPER_CLIENT")) {
        Assert.assertEquals("host", dependency.getScope());
        Assert.assertEquals(true, dependency.getAutoDeploy().isEnabled());
      }else {
        Assert.fail("Unexpected dependency");
      }
    }
    // component auto deploy
    Assert.assertNull(component.getAutoDeploy());
    // cardinality
    Assert.assertEquals("1", component.getCardinality());
  }

  @Test
  public void testZooKeeper134Dependencies() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "ZOOKEEPER");
    List<ComponentInfo> componentList = service.getComponents();
    Assert.assertEquals(2, componentList.size());
    for (ComponentInfo component : componentList) {
      String name = component.getName();
      if (name.equals("ZOOKEEPER_SERVER")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("1", component.getCardinality());
      }
      if (name.equals("ZOOKEEPER_CLIENT")) {
        // dependencies
        Assert.assertEquals(0, component.getDependencies().size());
        // component auto deploy
        Assert.assertNull(component.getAutoDeploy());
        // cardinality
        Assert.assertEquals("0+", component.getCardinality());
      }
    }
  }

  @Test
  public void testServicePackageDirInheritance() throws Exception {
    String assertionTemplate07 = StringUtils.join(
        new String[]{"stacks", "HDP", "2.0.7", "services", "%s", "package"}, File.separator);
    String assertionTemplate08 = StringUtils.join(
        new String[]{"stacks", "HDP", "2.0.8", "services", "%s", "package"}, File.separator);
    // Test service package dir determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HBASE");
    Assert.assertEquals(String.format(assertionTemplate07, "HBASE"),
            service.getServicePackageFolder());

    service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertEquals(String.format(assertionTemplate07, "HDFS"),
            service.getServicePackageFolder());
    // Test service package dir inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HBASE");
    Assert.assertEquals(String.format(assertionTemplate07, "HBASE"),
            service.getServicePackageFolder());
    // Test service package dir override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals(String.format(assertionTemplate08, "HDFS"),
            service.getServicePackageFolder());
  }


  @Test
  public void testServiceCommandScriptInheritance() throws Exception {
    // Test command script determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertEquals("scripts/service_check_1.py",
            service.getCommandScript().getScript());
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HBASE");
    Assert.assertEquals("scripts/service_check.py",
            service.getCommandScript().getScript());
    // Test command script inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HBASE");
    Assert.assertEquals("scripts/service_check.py",
            service.getCommandScript().getScript());
    // Test command script override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals("scripts/service_check_2.py",
            service.getCommandScript().getScript());
  }

    @Test
  public void testComponentCommandScriptInheritance() throws Exception {
    // Test command script determination in parent
    ComponentInfo component = metaInfo.getComponent(STACK_NAME_HDP,
        "2.0.7", "HDFS", "HDFS_CLIENT");
    Assert.assertEquals("scripts/hdfs_client.py",
            component.getCommandScript().getScript());
    component = metaInfo.getComponent(STACK_NAME_HDP,
        "2.0.7", "HBASE", "HBASE_MASTER");
    Assert.assertEquals("scripts/hbase_master.py",
            component.getCommandScript().getScript());
    // Test command script inheritance
    component = metaInfo.getComponent(STACK_NAME_HDP,
        "2.0.8", "HBASE", "HBASE_MASTER");
    Assert.assertEquals("scripts/hbase_master.py",
            component.getCommandScript().getScript());
    // Test command script override
    component = metaInfo.getComponent(STACK_NAME_HDP,
        "2.0.8", "HDFS", "HDFS_CLIENT");
    Assert.assertEquals("scripts/hdfs_client_overridden.py",
            component.getCommandScript().getScript());
  }


  @Test
  public void testServiceCustomCommandScriptInheritance() throws Exception {
    // Test custom command script determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");

    CustomCommandDefinition ccd = findCustomCommand("RESTART", service);
    Assert.assertEquals("scripts/restart_parent.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_SRV_COMMAND", service);
    Assert.assertEquals("scripts/yet_another_parent_srv_command.py",
            ccd.getCommandScript().getScript());

    Assert.assertEquals(2, service.getCustomCommands().size());

    // Test custom command script inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals(3, service.getCustomCommands().size());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_SRV_COMMAND", service);
    Assert.assertEquals("scripts/yet_another_parent_srv_command.py",
            ccd.getCommandScript().getScript());

    // Test custom command script override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");

    ccd = findCustomCommand("RESTART", service);
    Assert.assertEquals("scripts/restart_child.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_CHILD_SRV_COMMAND", service);
    Assert.assertEquals("scripts/yet_another_child_srv_command.py",
            ccd.getCommandScript().getScript());
  }


  @Test
  public void testChildCustomCommandScriptInheritance() throws Exception {
    // Test custom command script determination in parent
    ComponentInfo component = metaInfo.getComponent(STACK_NAME_HDP, "2.0.7",
            "HDFS", "NAMENODE");

    CustomCommandDefinition ccd = findCustomCommand("DECOMMISSION", component);
    Assert.assertEquals("scripts/namenode_dec.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_COMMAND", component);
    Assert.assertEquals("scripts/yet_another_parent_command.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("REBALANCEHDFS", component);
    Assert.assertEquals("scripts/namenode.py",
        ccd.getCommandScript().getScript());
    Assert.assertTrue(ccd.isBackground());

    Assert.assertEquals(3, component.getCustomCommands().size());

    // Test custom command script inheritance
    component = metaInfo.getComponent(STACK_NAME_HDP, "2.0.8",
            "HDFS", "NAMENODE");
    Assert.assertEquals(4, component.getCustomCommands().size());

    ccd = findCustomCommand("YET_ANOTHER_PARENT_COMMAND", component);
    Assert.assertEquals("scripts/yet_another_parent_command.py",
            ccd.getCommandScript().getScript());

    // Test custom command script override
    ccd = findCustomCommand("DECOMMISSION", component);
    Assert.assertEquals("scripts/namenode_dec_overr.py",
            ccd.getCommandScript().getScript());

    ccd = findCustomCommand("YET_ANOTHER_CHILD_COMMAND", component);
    Assert.assertEquals("scripts/yet_another_child_command.py",
            ccd.getCommandScript().getScript());
  }


  @Test
  public void testServiceOsSpecificsInheritance() throws Exception {
    // Test command script determination in parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HDFS");
    Assert.assertEquals("parent-package-def",
            service.getOsSpecifics().get("any").getPackages().get(0).getName());
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "HBASE");
    Assert.assertEquals(2, service.getOsSpecifics().keySet().size());
    // Test command script inheritance
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HBASE");
    Assert.assertEquals(2, service.getOsSpecifics().keySet().size());
    // Test command script override
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertEquals("child-package-def",
            service.getOsSpecifics().get("any").getPackages().get(0).getName());
  }

  @Test
  public void testServiceSchemaVersionInheritance() throws Exception {
    // Check parent
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "SQOOP");
    Assert.assertEquals("2.0", service.getSchemaVersion());
    // Test child service metainfo after merge
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "SQOOP");
    Assert.assertEquals("2.0", service.getSchemaVersion());
  }


  private CustomCommandDefinition findCustomCommand(String ccName,
                                                    ServiceInfo service) {
    for (CustomCommandDefinition ccd: service.getCustomCommands()) {
      if (ccd.getName().equals(ccName)) {
        return ccd;
      }
    }
    return null;
  }

  private CustomCommandDefinition findCustomCommand(String ccName,
                                                    ComponentInfo component) {
    for (CustomCommandDefinition ccd: component.getCustomCommands()) {
      if (ccd.getName().equals(ccName)) {
        return ccd;
      }
    }
    return null;
  }

  @Test
  public void testCustomConfigDir() throws Exception {

    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.7", "MAPREDUCE2");

    // assert that the property was found in a differently-named directory
    // cannot check the dirname itself since extended stacks won't carry over
    // the name

    boolean found = false;
    for (PropertyInfo pi : service.getProperties()) {
      if (pi.getName().equals("mr2-prop")) {
        Assert.assertEquals("some-mr2-value", pi.getValue());
        found = true;
      }
    }

    Assert.assertTrue(found);
  }

  @Test
  public void testLatestRepo() throws Exception {
    // ensure that all of the latest repo retrieval tasks have completed
    StackManager sm = metaInfo.getStackManager();
    int maxWait = 45000;
    int waitTime = 0;
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }

    for (RepositoryInfo ri : metaInfo.getRepositories("HDP", "2.1.1", "centos6")) {
      Assert.assertEquals(
          "Expected the default URL to be the same as in the xml file",
          "http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0",
          ri.getDefaultBaseUrl());
    }
  }


  @Test
  public void testLatestVdf() throws Exception {
    // ensure that all of the latest repo retrieval tasks have completed
    StackManager sm = metaInfo.getStackManager();
    int maxWait = 45000;
    int waitTime = 0;
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }

    // !!! default stack version is from latest-vdf.  2.2.0 only has one entry
    VersionDefinitionXml vdf = metaInfo.getVersionDefinition("HDP-2.2.0");
    assertNotNull(vdf);
    assertEquals(1, vdf.repositoryInfo.getOses().size());

    // !!! this stack has no "manifests" and no "latest-vdf".  So the default VDF should contain
    // information from repoinfo.xml and the "latest" structure
    vdf = metaInfo.getVersionDefinition("HDP-2.2.1");
    assertNotNull(vdf);

    assertEquals(2, vdf.repositoryInfo.getOses().size());
  }

  @Test
  public void testGetComponentDependency() throws AmbariException {
    DependencyInfo dependency = metaInfo.getComponentDependency("HDP", "1.3.4", "HIVE", "HIVE_SERVER", "ZOOKEEPER_SERVER");
    assertEquals("ZOOKEEPER/ZOOKEEPER_SERVER", dependency.getName());
    assertEquals("ZOOKEEPER_SERVER", dependency.getComponentName());
    assertEquals("ZOOKEEPER", dependency.getServiceName());
    assertEquals("cluster", dependency.getScope());
  }

  @Test
  public void testGetComponentDependencies() throws AmbariException {
    List<DependencyInfo> dependencies = metaInfo.getComponentDependencies("HDP", "1.3.4", "HBASE", "HBASE_MASTER");
    assertEquals(2, dependencies.size());

    DependencyInfo dependency = dependencies.get(0);
    assertEquals("HDFS/HDFS_CLIENT", dependency.getName());
    assertEquals("HDFS_CLIENT", dependency.getComponentName());
    assertEquals("HDFS", dependency.getServiceName());
    assertEquals("host", dependency.getScope());

    dependency = dependencies.get(1);
    assertEquals("ZOOKEEPER/ZOOKEEPER_SERVER", dependency.getName());
    assertEquals("ZOOKEEPER_SERVER", dependency.getComponentName());
    assertEquals("ZOOKEEPER", dependency.getServiceName());
    assertEquals("cluster", dependency.getScope());
  }

  @Test
  public void testPasswordPropertyAttribute() throws Exception {
    ServiceInfo service = metaInfo.getService(STACK_NAME_HDP, "2.0.1", "HIVE");
    List<PropertyInfo> propertyInfoList = service.getProperties();
    Assert.assertNotNull(propertyInfoList);
    PropertyInfo passwordProperty = null;
    for (PropertyInfo propertyInfo : propertyInfoList) {
      if (propertyInfo.isRequireInput()
          && propertyInfo.getPropertyTypes().contains(PropertyInfo.PropertyType.PASSWORD)) {
        passwordProperty = propertyInfo;
      } else {
        Assert.assertTrue(propertyInfo.getPropertyTypes().isEmpty());
      }
    }
    Assert.assertNotNull(passwordProperty);
    Assert.assertEquals("javax.jdo.option.ConnectionPassword", passwordProperty.getName());
  }

  @Test
  public void testAlertsJson() throws Exception {
    ServiceInfo svc = metaInfo.getService(STACK_NAME_HDP, "2.0.5", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getAlertsFile());

    svc = metaInfo.getService(STACK_NAME_HDP, "2.0.6", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNotNull(svc.getAlertsFile());

    svc = metaInfo.getService(STACK_NAME_HDP, "1.3.4", "HDFS");
    Assert.assertNotNull(svc);
    Assert.assertNull(svc.getAlertsFile());

    Set<AlertDefinition> set = metaInfo.getAlertDefinitions(STACK_NAME_HDP,
        "2.0.5", "HDFS");
    Assert.assertNotNull(set);
    Assert.assertTrue(set.size() > 0);

    // find two different definitions and test each one
    AlertDefinition nameNodeProcess = null;
    AlertDefinition nameNodeCpu = null;
    AlertDefinition datanodeStorage = null;
    AlertDefinition ignoreHost = null;

    Iterator<AlertDefinition> iterator = set.iterator();
    while (iterator.hasNext()) {
      AlertDefinition definition = iterator.next();
      if (definition.getName().equals("namenode_process")) {
        nameNodeProcess = definition;
      }

      if (definition.getName().equals("namenode_cpu")) {
        nameNodeCpu = definition;
      }

      if (definition.getName().equals("datanode_storage")) {
        datanodeStorage = definition;
      }

      if (definition.getName().equals("hdfs_ignore_host_test")) {
        ignoreHost = definition;
      }
    }

    assertNotNull(nameNodeProcess);
    assertNotNull(nameNodeCpu);
    assertNotNull(ignoreHost);

    assertEquals("NameNode Host CPU Utilization", nameNodeCpu.getLabel());

    // test namenode_process
    assertFalse(nameNodeProcess.isHostIgnored());
    assertEquals("A description of namenode_process", nameNodeProcess.getDescription());
    Source source = nameNodeProcess.getSource();
    assertNotNull(source);
    assertNotNull(((PortSource) source).getPort());
    Reporting reporting = source.getReporting();
    assertNotNull(reporting);
    assertNotNull(reporting.getOk());
    assertNotNull(reporting.getOk().getText());
    assertNull(reporting.getOk().getValue());
    assertNotNull(reporting.getCritical());
    assertNotNull(reporting.getCritical().getText());
    assertNull(reporting.getCritical().getValue());
    assertNull(reporting.getWarning());

    // test namenode_cpu
    assertFalse(nameNodeCpu.isHostIgnored());
    assertEquals("A description of namenode_cpu", nameNodeCpu.getDescription());
    source = nameNodeCpu.getSource();
    assertNotNull(source);
    reporting = source.getReporting();
    assertNotNull(reporting);
    assertNotNull(reporting.getOk());
    assertNotNull(reporting.getOk().getText());
    assertNull(reporting.getOk().getValue());
    assertNotNull(reporting.getCritical());
    assertNotNull(reporting.getCritical().getText());
    assertNotNull(reporting.getCritical().getValue());
    assertNotNull(reporting.getWarning());
    assertNotNull(reporting.getWarning().getText());
    assertNotNull(reporting.getWarning().getValue());

    // test a metric alert
    assertNotNull(datanodeStorage);
    assertEquals("A description of datanode_storage", datanodeStorage.getDescription());
    assertFalse(datanodeStorage.isHostIgnored());
    MetricSource metricSource = (MetricSource) datanodeStorage.getSource();
    assertNotNull( metricSource.getUri() );
    assertNotNull( metricSource.getUri().getHttpsProperty() );
    assertNotNull( metricSource.getUri().getHttpsPropertyValue() );
    assertNotNull( metricSource.getUri().getHttpsUri() );
    assertNotNull( metricSource.getUri().getHttpUri() );
    assertEquals(12345, metricSource.getUri().getDefaultPort().intValue());
//
    // ignore host
    assertTrue(ignoreHost.isHostIgnored());
  }


  //todo: refactor test to use mocks instead of injector
  /**
   * Tests merging stack-based with existing definitions works
   *
   * @throws Exception
   */
  @Test
  public void testAlertDefinitionMerging() throws Exception {
    final String stackVersion = "2.0.6";
    final String repoVersion = "2.0.6-1234";

    Injector injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(EntityManager.class);

    OrmTestHelper ormHelper = injector.getInstance(OrmTestHelper.class);
    long clusterId = ormHelper.createCluster("cluster" + System.currentTimeMillis());

    Class<?> c = metaInfo.getClass().getSuperclass();

    Field f = c.getDeclaredField("alertDefinitionDao");
    f.setAccessible(true);
    f.set(metaInfo, injector.getInstance(AlertDefinitionDAO.class));

    f = c.getDeclaredField("ambariServiceAlertDefinitions");
    f.setAccessible(true);
    f.set(metaInfo, injector.getInstance(AmbariServiceAlertDefinitions.class));

    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getClusterById(clusterId);
    cluster.setDesiredStackVersion(
        new StackId(STACK_NAME_HDP, stackVersion));

    RepositoryVersionEntity repositoryVersion = ormHelper.getOrCreateRepositoryVersion(
        cluster.getCurrentStackVersion(), repoVersion);

    cluster.addService("HDFS", repositoryVersion);

    metaInfo.reconcileAlertDefinitions(clusters, false);

    AlertDefinitionDAO dao = injector.getInstance(AlertDefinitionDAO.class);
    List<AlertDefinitionEntity> definitions = dao.findAll(clusterId);
    assertEquals(13, definitions.size());

    // figure out how many of these alerts were merged into from the
    // non-stack alerts.json
    int hostAlertCount = 0;
    for (AlertDefinitionEntity definition : definitions) {
      if (definition.getServiceName().equals("AMBARI")
          && definition.getComponentName().equals("AMBARI_AGENT")) {
        hostAlertCount++;
      }
    }

    assertEquals(3, hostAlertCount);
    assertEquals(10, definitions.size() - hostAlertCount);

    for (AlertDefinitionEntity definition : definitions) {
      definition.setScheduleInterval(28);
      dao.merge(definition);
    }

    metaInfo.reconcileAlertDefinitions(clusters, false);

    definitions = dao.findAll();
    assertEquals(13, definitions.size());

    for (AlertDefinitionEntity definition : definitions) {
      assertEquals(28, definition.getScheduleInterval().intValue());
    }

    // find all enabled for the cluster should find 6 (the ones from HDFS;
    // it will not find the agent alert since it's not bound to the cluster)
    definitions = dao.findAllEnabled(cluster.getClusterId());
    assertEquals(12, definitions.size());

    // create new definition
    AlertDefinitionEntity entity = new AlertDefinitionEntity();
    entity.setClusterId(clusterId);
    entity.setDefinitionName("bad_hdfs_alert");
    entity.setLabel("Bad HDFS Alert");
    entity.setDescription("A way to fake a component being removed");
    entity.setEnabled(true);
    entity.setHash(UUID.randomUUID().toString());
    entity.setScheduleInterval(1);
    entity.setServiceName("HDFS");
    entity.setComponentName("BAD_COMPONENT");
    entity.setSourceType(SourceType.METRIC);
    entity.setSource("{\"type\" : \"METRIC\"}");
    dao.create(entity);

    // verify the new definition is found (6 HDFS + 1 new one)
    definitions = dao.findAllEnabled(cluster.getClusterId());
    assertEquals(13, definitions.size());

    // reconcile, which should disable our bad definition
    metaInfo.reconcileAlertDefinitions(clusters, false);

    // find all enabled for the cluster should find 6
    definitions = dao.findAllEnabled(cluster.getClusterId());
    assertEquals(12, definitions.size());

    // find all should find 6 HDFS + 1 disabled + 1 agent alert + 2 server
    // alerts
    definitions = dao.findAll();
    assertEquals(14, definitions.size());

    entity = dao.findById(entity.getDefinitionId());
    assertFalse(entity.getEnabled());
  }

  /**
   * Test scenario when service were removed and not mapped alerts need to be disabled
   *
   * @throws Exception
   */
  @Test
  public void testAlertDefinitionMergingRemoveScenario() throws Exception {
    final String repoVersion = "2.0.6-1234";
    final String stackVersion = "2.0.6";

    Injector injector = Guice.createInjector(Modules.override(
      new InMemoryDefaultTestModule()).with(new MockModule()));

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(EntityManager.class);

    OrmTestHelper ormHelper = injector.getInstance(OrmTestHelper.class);
    long clusterId = ormHelper.createCluster("cluster" + System.currentTimeMillis());

    Class<?> c = metaInfo.getClass().getSuperclass();

    Field f = c.getDeclaredField("alertDefinitionDao");
    f.setAccessible(true);
    f.set(metaInfo, injector.getInstance(AlertDefinitionDAO.class));

    f = c.getDeclaredField("ambariServiceAlertDefinitions");
    f.setAccessible(true);
    f.set(metaInfo, injector.getInstance(AmbariServiceAlertDefinitions.class));

    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = clusters.getClusterById(clusterId);
    cluster.setDesiredStackVersion(
      new StackId(STACK_NAME_HDP, stackVersion));

    RepositoryVersionEntity repositoryVersion = ormHelper.getOrCreateRepositoryVersion(
      cluster.getCurrentStackVersion(), repoVersion);

    cluster.addService("HDFS", repositoryVersion);

    metaInfo.reconcileAlertDefinitions(clusters, false);

    AlertDefinitionDAO dao = injector.getInstance(AlertDefinitionDAO.class);
    List<AlertDefinitionEntity> definitions = dao.findAll(clusterId);
    assertEquals(13, definitions.size());

    cluster.deleteService("HDFS", new DeleteHostComponentStatusMetaData());
    metaInfo.reconcileAlertDefinitions(clusters, false);
    List<AlertDefinitionEntity> updatedDefinitions = dao.findAll(clusterId);
    assertEquals(7, updatedDefinitions.size());
  }

  @Test
  public void testKerberosDescriptor() throws Exception {
    ServiceInfo service;

    // Test that kerberos descriptor file is not available when not supplied in service definition
    service = metaInfo.getService(STACK_NAME_HDP, "2.1.1", "PIG");
    Assert.assertNotNull(service);
    Assert.assertNull(service.getKerberosDescriptorFile());

    // Test that kerberos descriptor file is available when supplied in service definition
    service = metaInfo.getService(STACK_NAME_HDP, "2.0.8", "HDFS");
    Assert.assertNotNull(service);
    Assert.assertNotNull(service.getKerberosDescriptorFile());

    // Test that kerberos descriptor file is available from inherited stack version
    service = metaInfo.getService(STACK_NAME_HDP, "2.1.1", "HDFS");
    Assert.assertNotNull(service);
    Assert.assertNotNull(service.getKerberosDescriptorFile());

    // Test that kerberos.json file can be parsed into mapped data
    Map<?,?> kerberosDescriptorData = new Gson()
        .fromJson(new FileReader(service.getKerberosDescriptorFile()), Map.class);

    Assert.assertNotNull(kerberosDescriptorData);
    Assert.assertEquals(1, kerberosDescriptorData.size());
  }

  @Test
  public void testReadKerberosDescriptorFromFile() throws AmbariException {
    String path = metaInfo.getCommonKerberosDescriptorFileLocation();
    KerberosDescriptor descriptor = metaInfo.readKerberosDescriptorFromFile(path);

    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(descriptor.getProperties());
    Assert.assertEquals(3, descriptor.getProperties().size());

    Assert.assertNotNull(descriptor.getIdentities());
    Assert.assertEquals(1, descriptor.getIdentities().size());
    Assert.assertEquals("spnego", descriptor.getIdentities().get(0).getName());

    Assert.assertNotNull(descriptor.getConfigurations());
    Assert.assertEquals(1, descriptor.getConfigurations().size());
    Assert.assertNotNull(descriptor.getConfigurations().get("core-site"));
    Assert.assertNotNull(descriptor.getConfiguration("core-site"));

    Assert.assertNull(descriptor.getServices());
  }

  @Test
  public void testGetKerberosDescriptor() throws AmbariException {
    KerberosDescriptor descriptor = metaInfo.getKerberosDescriptor(STACK_NAME_HDP, "2.0.8", false);

    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(descriptor.getProperties());
    Assert.assertEquals(3, descriptor.getProperties().size());

    Assert.assertNotNull(descriptor.getIdentities());
    Assert.assertEquals(1, descriptor.getIdentities().size());
    Assert.assertEquals("spnego", descriptor.getIdentities().get(0).getName());

    Assert.assertNotNull(descriptor.getConfigurations());
    Assert.assertEquals(1, descriptor.getConfigurations().size());
    Assert.assertNotNull(descriptor.getConfigurations().get("core-site"));
    Assert.assertNotNull(descriptor.getConfiguration("core-site"));

    Assert.assertNotNull(descriptor.getServices());
    Assert.assertEquals(1, descriptor.getServices().size());
    Assert.assertNotNull(descriptor.getServices().get("HDFS"));
    Assert.assertNotNull(descriptor.getService("HDFS"));
    Assert.assertFalse(descriptor.getService("HDFS").shouldPreconfigure());
  }

  @Test
  public void testGetKerberosDescriptorWithPreconfigure() throws AmbariException {
    KerberosDescriptor descriptor = metaInfo.getKerberosDescriptor(STACK_NAME_HDP, "2.0.8", true);

    Assert.assertNotNull(descriptor);
    Assert.assertNotNull(descriptor.getProperties());
    Assert.assertEquals(3, descriptor.getProperties().size());

    Assert.assertNotNull(descriptor.getIdentities());
    Assert.assertEquals(1, descriptor.getIdentities().size());
    Assert.assertEquals("spnego", descriptor.getIdentities().get(0).getName());

    Assert.assertNotNull(descriptor.getConfigurations());
    Assert.assertEquals(1, descriptor.getConfigurations().size());
    Assert.assertNotNull(descriptor.getConfigurations().get("core-site"));
    Assert.assertNotNull(descriptor.getConfiguration("core-site"));

    Assert.assertNotNull(descriptor.getServices());
    Assert.assertEquals(2, descriptor.getServices().size());
    Assert.assertNotNull(descriptor.getServices().get("HDFS"));
    Assert.assertNotNull(descriptor.getService("HDFS"));
    Assert.assertTrue(descriptor.getService("HDFS").shouldPreconfigure());
    Assert.assertNotNull(descriptor.getServices().get("HDFS"));
    Assert.assertNotNull(descriptor.getService("HDFS"));
    Assert.assertTrue(descriptor.getService("HDFS").shouldPreconfigure());
    Assert.assertNotNull(descriptor.getServices().get("NEW_SERVICE"));
    Assert.assertNotNull(descriptor.getService("NEW_SERVICE"));
    Assert.assertTrue(descriptor.getService("NEW_SERVICE").shouldPreconfigure());
  }

  @Test
  public void testGetCommonWidgetsFile() throws AmbariException {
    File widgetsFile = metaInfo.getCommonWidgetsDescriptorFile();

    Assert.assertNotNull(widgetsFile);
    Assert.assertEquals("src/test/resources/widgets.json", widgetsFile.getPath());
  }

  @Test
  public void testGetVersionDefinitionsForDisabledStack() throws AmbariException {
    Map<String, VersionDefinitionXml> versionDefinitions = metaInfo.getVersionDefinitions();
    Assert.assertNotNull(versionDefinitions);
    // Check presence
    Map.Entry<String, VersionDefinitionXml> vdfEntry = null;
    for (Map.Entry<String, VersionDefinitionXml> entry : versionDefinitions.entrySet()) {
      if (entry.getKey().equals("HDP-2.2.1")) {
        vdfEntry = entry;
      }
    }
    Assert.assertNotNull("Candidate stack and vdf for test case.", vdfEntry);
    StackInfo stackInfo = metaInfo.getStack("HDP", "2.2.1");
    // Strange that this is not immutable but works for this test !
    stackInfo.setActive(false);

    // Hate to use reflection hence changed contract to be package private
    metaInfo.versionDefinitions = null;

    versionDefinitions = metaInfo.getVersionDefinitions();
    vdfEntry = null;
    for (Map.Entry<String, VersionDefinitionXml> entry : versionDefinitions.entrySet()) {
      if (entry.getKey().equals("HDP-2.2.1")) {
        vdfEntry = entry;
      }
    }
    Assert.assertNull("Disabled stack should not be returned by the API", vdfEntry);
  }

  private File getStackRootTmp(String buildDir) {
    return new File(buildDir + "/ambari-metaInfo");
  }

  private File getVersion() {
    File version = new File("src/test/resources/version");

    if (System.getProperty("os.name").contains("Windows")) {
      version = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "version");
    }

    return version;
  }

  private void setupTempAmbariMetaInfoDirs(String buildDir) throws Exception {
    File stackRootTmp = getStackRootTmp(buildDir);
    File stackRoot = new File("src/test/resources/stacks");

    if (System.getProperty("os.name").contains("Windows")) {
      stackRoot = new File(ClassLoader.getSystemClassLoader().getResource("stacks").getPath());
    }

    stackRootTmp.mkdir();
    FileUtils.copyDirectory(stackRoot, stackRootTmp);
  }

  private TestAmbariMetaInfo setupTempAmbariMetaInfo(String buildDir) throws Exception {
    setupTempAmbariMetaInfoDirs(buildDir);
    TestAmbariMetaInfo ambariMetaInfo = setupTempAmbariMetaInfoExistingDirs(buildDir);
    return ambariMetaInfo;
  }

  private TestAmbariMetaInfo setupTempAmbariMetaInfoExistingDirs(String buildDir) throws Exception {
    File version = getVersion();
    File stackRootTmp = getStackRootTmp(buildDir);
    TestAmbariMetaInfo ambariMetaInfo = createAmbariMetaInfo(stackRootTmp, version, new File(""));
    return ambariMetaInfo;
  }

  private static TestAmbariMetaInfo createAmbariMetaInfo(File stackRoot,
    File versionFile, File resourcesRoot) throws Exception {

    Properties properties = new Properties();
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), stackRoot.getPath());
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), versionFile.getPath());

    properties.setProperty(Configuration.RESOURCES_DIR.getKey(), resourcesRoot.getPath());
    properties.setProperty(Configuration.MPACKS_V2_STAGING_DIR_PATH.getKey(),"src/test/resources/mpacks-v2");

    Configuration configuration = new Configuration(properties);

    TestAmbariMetaInfo metaInfo = new TestAmbariMetaInfo(configuration);

    metaInfo.replayAllMocks();

    try {
      metaInfo.init();
    } catch(Exception e) {
      LOG.info("Error in initializing ", e);
      throw e;
    }
    waitForAllReposToBeResolved(metaInfo);

    return metaInfo;
  }

  private static void checkNoAggregatedFunctionsForJmx(List<MetricDefinition> metricDefinitions) {
    for (MetricDefinition metricDefinition: metricDefinitions) {
      if ("jmx".equals(metricDefinition.getType())) {
        for (String metric: metricDefinition.getMetrics().keySet()) {
          if (metric.endsWith("._sum")) {
            Assert.fail("Aggregated functions aren't supported for JMX metrics. " + metric);
          }
        }
      }
    }
  }

  private static void waitForAllReposToBeResolved(AmbariMetaInfo metaInfo) throws Exception {
    int maxWait = 45000;
    int waitTime = 0;
    StackManager sm = metaInfo.getStackManager();
    while (waitTime < maxWait && ! sm.haveAllRepoUrlsBeenResolved()) {
      Thread.sleep(5);
      waitTime += 5;
    }

    if (waitTime >= maxWait) {
      fail("Latest Repo tasks did not complete");
    }
  }

  private static class TestAmbariMetaInfo extends AmbariMetaInfo {

    AlertDefinitionDAO alertDefinitionDAO;
    AlertDefinitionFactory alertDefinitionFactory;
    OsFamily osFamily;
    Injector injector;

    public TestAmbariMetaInfo(Configuration configuration) throws Exception {
      super(configuration);

      injector = Guice.createInjector(Modules.override(
          new InMemoryDefaultTestModule()).with(new MockModule()));

      injector.getInstance(GuiceJpaInitializer.class);
      injector.getInstance(EntityManager.class);

      Class<?> c = getClass().getSuperclass();

      // StackManagerFactory
      StackManagerFactory stackManagerFactory = injector.getInstance(StackManagerFactory.class);
      Field f = c.getDeclaredField("stackManagerFactory");
      f.setAccessible(true);
      f.set(this, stackManagerFactory);

      // MpackManagerFactory
      MpackManagerFactory mpackManagerFactory = injector.getInstance(MpackManagerFactory.class);
      f = c.getDeclaredField("mpackManagerFactory");
      f.setAccessible(true);
      f.set(this, mpackManagerFactory);

      //AlertDefinitionDAO
      alertDefinitionDAO = createNiceMock(AlertDefinitionDAO.class);
      f = c.getDeclaredField("alertDefinitionDao");
      f.setAccessible(true);
      f.set(this, alertDefinitionDAO);

      //AlertDefinitionFactory
      //alertDefinitionFactory = createNiceMock(AlertDefinitionFactory.class);
      alertDefinitionFactory = new AlertDefinitionFactory();
      f = c.getDeclaredField("alertDefinitionFactory");
      f.setAccessible(true);
      f.set(this, alertDefinitionFactory);

      //AmbariEventPublisher
      AmbariEventPublisher ambariEventPublisher = new AmbariEventPublisher();
      f = c.getDeclaredField("eventPublisher");
      f.setAccessible(true);
      f.set(this, ambariEventPublisher);

      //KerberosDescriptorFactory
      KerberosDescriptorFactory kerberosDescriptorFactory = new KerberosDescriptorFactory();
      f = c.getDeclaredField("kerberosDescriptorFactory");
      f.setAccessible(true);
      f.set(this, kerberosDescriptorFactory);

      //KerberosServiceDescriptorFactory
      KerberosServiceDescriptorFactory kerberosServiceDescriptorFactory = new KerberosServiceDescriptorFactory();
      f = c.getDeclaredField("kerberosServiceDescriptorFactory");
      f.setAccessible(true);
      f.set(this, kerberosServiceDescriptorFactory);

      //OSFamily
      Configuration config = createNiceMock(Configuration.class);
      if (System.getProperty("os.name").contains("Windows")) {
        expect(config.getSharedResourcesDirPath()).andReturn(ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();
        expect(config.getResourceDirPath()).andReturn(ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();
      }
      else {
        expect(config.getSharedResourcesDirPath()).andReturn("./src/test/resources").anyTimes();
        expect(config.getResourceDirPath()).andReturn("./src/test/resources").anyTimes();
      }

      replay(config);

      osFamily = new OsFamily(config);

      f = c.getDeclaredField("osFamily");
      f.setAccessible(true);
      f.set(this, osFamily);
    }

    public void replayAllMocks() {
      replay(alertDefinitionDAO);
    }

    public class MockModule extends AbstractModule {
      @Override
      protected void configure() {
        bind(ActionMetadata.class);

        // create a mock metainfo DAO for the entire system so that injectables
        // can use the mock as well
        bind(MetainfoDAO.class).toInstance(createNiceMock(MetainfoDAO.class));
      }
    }
  }
}
