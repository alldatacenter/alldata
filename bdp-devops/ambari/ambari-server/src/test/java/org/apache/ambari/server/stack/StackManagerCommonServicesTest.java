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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.CommandScriptDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;


/**
 * StackManager unit tests.
 */
public class StackManagerCommonServicesTest {

  private static StackManager stackManager;
  private static MetainfoDAO metaInfoDao;
  private static StackDAO stackDao;
  private static ExtensionDAO extensionDao;
  private static ExtensionLinkDAO linkDao;
  private static ActionMetadata actionMetadata;
  private static OsFamily osFamily;

  @BeforeClass
  public static void initStack() throws Exception {
    stackManager = createTestStackManager();
  }

  public static StackManager createTestStackManager() throws Exception {
    String stack = ClassLoader.getSystemClassLoader().getResource(
        "stacks_with_common_services").getPath();

    String commonServices = ClassLoader.getSystemClassLoader().getResource(
        "common-services").getPath();
    String extensions = ClassLoader.getSystemClassLoader().getResource(
            "extensions").getPath();
    return createTestStackManager(stack, commonServices, extensions);
  }

  public static StackManager createTestStackManager(String stackRoot,
      String commonServicesRoot, String extensionRoot) throws Exception {
    // todo: dao , actionMetaData expectations
    metaInfoDao = createNiceMock(MetainfoDAO.class);
    stackDao = createNiceMock(StackDAO.class);
    extensionDao = createNiceMock(ExtensionDAO.class);
    linkDao = createNiceMock(ExtensionLinkDAO.class);
    actionMetadata = createNiceMock(ActionMetadata.class);
    Configuration config = createNiceMock(Configuration.class);
    StackEntity stackEntity = createNiceMock(StackEntity.class);
    ExtensionEntity extensionEntity = createNiceMock(ExtensionEntity.class);

    expect(config.getSharedResourcesDirPath()).andReturn(
        ClassLoader.getSystemClassLoader().getResource("").getPath()).anyTimes();

    expect(
        stackDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(stackEntity).atLeastOnce();


    expect(
        extensionDao.find(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(extensionEntity).atLeastOnce();

    List<ExtensionLinkEntity> list = Collections.emptyList();
    expect(
        linkDao.findByStack(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(list).atLeastOnce();

    replay(config, stackDao, extensionDao, linkDao);

    osFamily = new OsFamily(config);

    replay(metaInfoDao, actionMetadata);
    AmbariManagementHelper helper = new AmbariManagementHelper(stackDao, extensionDao, linkDao);

    StackManager stackManager = new StackManager(new File(stackRoot), new File(
        commonServicesRoot), new File(extensionRoot), osFamily, true, metaInfoDao,
        actionMetadata, stackDao, extensionDao, linkDao, helper);

    EasyMock.verify( config, stackDao );

    return stackManager;
  }

  @Test
  public void testGetStacksCount() throws Exception {
    Collection<StackInfo> stacks = stackManager.getStacks();
    assertEquals(2, stacks.size());
  }

  @Test
  public void testGetStacksByName() {
    Collection<StackInfo> stacks = stackManager.getStacks("HDP");
    assertEquals(2, stacks.size());
  }

  @Test
  public void testAddOnServiceRepoIsLoaded() {
    Collection<StackInfo> stacks = stackManager.getStacks("HDP");
    StackInfo stack = null;
    for(StackInfo stackInfo: stackManager.getStacks()) {
      if ("0.2".equals(stackInfo.getVersion())) {
        stack = stackInfo;
        break;
      }
    }
    List<RepositoryInfo> repos = stack.getRepositoriesByOs().get("redhat6");
    ImmutableSet<String> repoIds = ImmutableSet.copyOf(Lists.transform(repos, RepositoryInfo.GET_REPO_ID_FUNCTION));
    assertTrue("Repos are expected to contain MSFT_R-8.1", repoIds.contains("ADDON_REPO-1.0"));
  }

  @Test
  public void testGetStack() {
    StackInfo stack = stackManager.getStack("HDP", "0.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.1", stack.getVersion());

    Collection<ServiceInfo> services = stack.getServices();
    assertEquals(3, services.size());

    Map<String, ServiceInfo> serviceMap = new HashMap<>();
    for (ServiceInfo service : services) {
      serviceMap.put(service.getName(), service);
    }
    ServiceInfo hdfsService = serviceMap.get("HDFS");
    assertNotNull(hdfsService);
    List<ComponentInfo> components = hdfsService.getComponents();
    assertEquals(6, components.size());
    List<PropertyInfo> properties = hdfsService.getProperties();
    assertEquals(62, properties.size());

    // test a couple of the properties for filename
    boolean hdfsPropFound = false;
    boolean hbasePropFound = false;
    for (PropertyInfo p : properties) {
      if (p.getName().equals("hbase.regionserver.msginterval")) {
        assertEquals("hbase-site.xml", p.getFilename());
        hbasePropFound = true;
      } else if (p.getName().equals("dfs.name.dir")) {
        assertEquals("hdfs-site.xml", p.getFilename());
        hdfsPropFound = true;
      }
    }
    assertTrue(hbasePropFound);
    assertTrue(hdfsPropFound);

    ServiceInfo mrService = serviceMap.get("MAPREDUCE");
    assertNotNull(mrService);
    components = mrService.getComponents();
    assertEquals(3, components.size());

    ServiceInfo pigService = serviceMap.get("PIG");
    assertNotNull(pigService);
    assertEquals("PIG", pigService.getName());
    assertEquals("1.0", pigService.getVersion());
    assertEquals("This is comment for PIG service", pigService.getComment());
    components = pigService.getComponents();
    assertEquals(1, components.size());
    CommandScriptDefinition commandScript = pigService.getCommandScript();
    assertEquals("scripts/service_check.py", commandScript.getScript());
    assertEquals(CommandScriptDefinition.Type.PYTHON,
        commandScript.getScriptType());
    assertEquals(300, commandScript.getTimeout());
    List<String> configDependencies = pigService.getConfigDependencies();
    assertEquals(1, configDependencies.size());
    assertEquals("global", configDependencies.get(0));
    assertEquals("global",
            pigService.getConfigDependenciesWithComponents().get(0));
    ComponentInfo client = pigService.getClientComponent();
    assertNotNull(client);
    assertEquals("PIG", client.getName());
    assertEquals("0+", client.getCardinality());
    assertEquals("CLIENT", client.getCategory());
    assertEquals("configuration", pigService.getConfigDir());
    assertEquals("2.0", pigService.getSchemaVersion());
    Map<String, ServiceOsSpecific> osInfoMap = pigService.getOsSpecifics();
    assertEquals(1, osInfoMap.size());
    ServiceOsSpecific osSpecific = osInfoMap.get("centos6");
    assertNotNull(osSpecific);
    assertEquals("centos6", osSpecific.getOsFamily());
    assertNull(osSpecific.getRepo());
    List<ServiceOsSpecific.Package> packages = osSpecific.getPackages();
    assertEquals(2, packages.size());
    ServiceOsSpecific.Package pkg = packages.get(0);
    assertEquals("pig", pkg.getName());
    assertFalse(pkg.getSkipUpgrade());

    ServiceOsSpecific.Package lzoPackage = packages.get(1);
    assertEquals("lzo", lzoPackage.getName());
    assertTrue(lzoPackage.getSkipUpgrade());

    assertEquals(pigService.getParent(), "common-services/PIG/1.0");
  }

  @Test
  public void testGetServicePackageFolder() {
    StackInfo stack = stackManager.getStack("HDP", "0.1");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.1", stack.getVersion());
    ServiceInfo hdfsService1 = stack.getService("HDFS");
    assertNotNull(hdfsService1);

    stack = stackManager.getStack("HDP", "0.2");
    assertNotNull(stack);
    assertEquals("HDP", stack.getName());
    assertEquals("0.2", stack.getVersion());
    ServiceInfo hdfsService2 = stack.getService("HDFS");
    assertNotNull(hdfsService2);

    String packageDir1 = StringUtils.join(new String[] { "common-services",
        "HDFS", "1.0", "package" }, File.separator);
    String packageDir2 = StringUtils.join(new String[] {
        "stacks_with_common_services", "HDP", "0.2", "services", "HDFS",
        "package" }, File.separator);

    assertEquals(packageDir1, hdfsService1.getServicePackageFolder());
    assertEquals(packageDir2, hdfsService2.getServicePackageFolder());
  }
}
