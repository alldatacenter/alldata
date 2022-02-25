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

package org.apache.ambari.server.view;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.view.configuration.InstanceConfig;
import org.apache.ambari.server.view.configuration.InstanceConfigTest;
import org.apache.ambari.server.view.configuration.PropertyConfig;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.server.view.configuration.ViewConfigTest;
import org.apache.ambari.server.view.events.EventImpl;
import org.apache.ambari.server.view.events.EventImplTest;
import org.apache.ambari.server.view.validation.ValidationException;
import org.apache.ambari.view.ViewDefinition;
import org.apache.ambari.view.events.Event;
import org.apache.ambari.view.events.Listener;
import org.apache.ambari.view.validation.ValidationResult;
import org.apache.ambari.view.validation.Validator;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Provider;

/**
 * ViewRegistry tests.
 */
public class ViewRegistryTest {

  private static final String VIEW_XML_1 = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "</view>";

  private static final String VIEW_XML_2 = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>2.0.0</version>\n" +
      "</view>";

  private static final String XML_VALID_INSTANCE = "<view>\n" +
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
      "        <masked>true</masked>" +
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

  private static final String XML_INVALID_INSTANCE = "<view>\n" +
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

  private static final String AUTO_VIEW_XML = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <auto-instance>\n" +
      "        <name>AUTO-INSTANCE</name>\n" +
      "        <stack-id>HDP-2.0</stack-id>\n" +
      "        <services><service>HIVE</service><service>HDFS</service></services>\n" +
      "    </auto-instance>\n" +
      "</view>";

  private static final String AUTO_VIEW_WILD_STACK_XML = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <auto-instance>\n" +
      "        <name>AUTO-INSTANCE</name>\n" +
      "        <stack-id>HDP-2.*</stack-id>\n" +
      "        <services><service>HIVE</service><service>HDFS</service></services>\n" +
      "    </auto-instance>\n" +
      "</view>";

  private static final String AUTO_VIEW_WILD_ALL_STACKS_XML = "<view>\n" +
    "    <name>MY_VIEW</name>\n" +
    "    <label>My View!</label>\n" +
    "    <version>1.0.0</version>\n" +
    "    <auto-instance>\n" +
    "        <name>AUTO-INSTANCE</name>\n" +
    "        <stack-id>*</stack-id>\n" +
    "        <services><service>HIVE</service><service>HDFS</service></services>\n" +
    "    </auto-instance>\n" +
    "</view>";

  private static final String AUTO_VIEW_BAD_STACK_XML = "<view>\n" +
      "    <name>MY_VIEW</name>\n" +
      "    <label>My View!</label>\n" +
      "    <version>1.0.0</version>\n" +
      "    <auto-instance>\n" +
      "        <name>AUTO-INSTANCE</name>\n" +
      "        <stack-id>HDP-2.5</stack-id>\n" +
      "        <services><service>HIVE</service><service>HDFS</service></services>\n" +
      "    </auto-instance>\n" +
      "</view>";

  private static final String EXPECTED_HDP_2_0_STACK_NAME = "HDP-2.0";

  // registry mocks
  private static final ViewDAO viewDAO = createMock(ViewDAO.class);
  private static final ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
  private static final ViewInstanceOperationHandler viewInstanceOperationHandler = createNiceMock(ViewInstanceOperationHandler.class);
  private static final UserDAO userDAO = createNiceMock(UserDAO.class);
  private static final MemberDAO memberDAO = createNiceMock(MemberDAO.class);
  private static final PrivilegeDAO privilegeDAO = createNiceMock(PrivilegeDAO.class);
  private static final PermissionDAO permissionDAO = createNiceMock(PermissionDAO.class);
  private static final ResourceDAO resourceDAO = createNiceMock(ResourceDAO.class);
  private static final ResourceTypeDAO resourceTypeDAO = createNiceMock(ResourceTypeDAO.class);
  private static final SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
  private static final Configuration configuration = createNiceMock(Configuration.class);
  private static final ViewInstanceHandlerList handlerList = createNiceMock(ViewInstanceHandlerList.class);
  private static final AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
  private static final Clusters clusters = createNiceMock(Clusters.class);



  @Before
  public void resetGlobalMocks() {
    ViewRegistry.initInstance(getRegistry(viewInstanceOperationHandler, viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        permissionDAO, resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null, ambariMetaInfo, clusters));

    reset(viewInstanceOperationHandler, viewDAO, resourceDAO, viewInstanceDAO, userDAO, memberDAO,
        privilegeDAO, resourceTypeDAO, securityHelper, configuration, handlerList, ambariMetaInfo,
        clusters);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testReadViewArchives() throws Exception {
    testReadViewArchives(false, false, false);
  }

  @Test
  public void testReadViewArchives_removeUndeployed() throws Exception {
    testReadViewArchives(false, true, false);
  }

  @Test
  public void testReadViewArchives_badArchive() throws Exception {
    testReadViewArchives(true, false, false);
  }


  @Ignore("this will get refactored when divorced from the stack")
  public void testReadViewArchives_viewAutoInstanceCreation() throws Exception {
    testReadViewArchives(false, false, true);
  }

  private void testReadViewArchives(boolean badArchive, boolean removeUndeployed, boolean checkAutoInstanceCreation) throws Exception {

    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File metaInfDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    FileOutputStream fos = createMock(FileOutputStream.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    Set<ViewInstanceEntity> viewInstanceEntities = ViewInstanceEntityTest.getViewInstanceEntities(viewDefinition);

    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      viewInstanceEntity.putInstanceData("p1", "v1");

      Collection<ViewEntityEntity> entities = new HashSet<>();
      ViewEntityEntity viewEntityEntity = new ViewEntityEntity();
      viewEntityEntity.setId(99L);
      viewEntityEntity.setIdProperty("id");
      viewEntityEntity.setViewName("MY_VIEW{1.0.0}");
      viewEntityEntity.setClassName("class");
      viewEntityEntity.setViewInstanceName(viewInstanceEntity.getName());
      viewEntityEntity.setViewInstance(viewInstanceEntity);
      entities.add(viewEntityEntity);

      viewInstanceEntity.setEntities(entities);
    }
    viewDefinition.setInstances(viewInstanceEntities);

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

    long resourceId = 99L;
    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setId(resourceId);
      resourceEntity.setResourceType(resourceTypeEntity);
      viewInstanceEntity.setResource(resourceEntity);
    }

    Map<String, File> files = new HashMap<>();
    if (System.getProperty("os.name").contains("Windows")) {
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work", extractedArchiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}", archiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\view.xml", entryFile);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF/classes", classesDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF/lib", libDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\META-INF", metaInfDir);
    }
    else {
      files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);
    }

    Map<File, FileOutputStream> outputStreams = new HashMap<>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir).anyTimes();
    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views").anyTimes();
    }
    else {
      expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views").anyTimes();
    }

    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();
    expect(configuration.extractViewsAfterClusterConfig()).andReturn(Boolean.FALSE).anyTimes();

    expect(viewDir.listFiles()).andReturn(new File[]{viewArchive});

    expect(viewArchive.isDirectory()).andReturn(false);
    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewArchive.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }

    expect(archiveDir.exists()).andReturn(false).anyTimes();
    if (System.getProperty("os.name").contains("Windows")) {
      expect(archiveDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(archiveDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }
    expect(archiveDir.mkdir()).andReturn(true).anyTimes();
    expect(archiveDir.toURI()).andReturn(new URI("file:./")).anyTimes();

    expect(metaInfDir.mkdir()).andReturn(true).anyTimes();

    if (!badArchive) {
      expect(viewJarFile.getNextJarEntry()).andReturn(jarEntry);
      expect(viewJarFile.getNextJarEntry()).andReturn(null);

      expect(jarEntry.getName()).andReturn("view.xml");
      expect(jarEntry.isDirectory()).andReturn(false);

      expect(viewJarFile.read(anyObject(byte[].class))).andReturn(10);
      expect(viewJarFile.read(anyObject(byte[].class))).andReturn(-1);
      fos.write(anyObject(byte[].class), eq(0), eq(10));

      fos.flush();
      fos.close();
      viewJarFile.closeEntry();
      viewJarFile.close();

      expect(viewDAO.findByName("MY_VIEW{1.0.0}")).andReturn(viewDefinition);
    }

    expect(extractedArchiveDir.exists()).andReturn(false).anyTimes();
    expect(extractedArchiveDir.mkdir()).andReturn(true).anyTimes();

    expect(classesDir.exists()).andReturn(true).anyTimes();
    expect(classesDir.toURI()).andReturn(new URI("file:./")).anyTimes();

    expect(libDir.exists()).andReturn(true).anyTimes();

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry}).anyTimes();
    expect(fileEntry.toURI()).andReturn(new URI("file:./")).anyTimes();

    expect(configuration.isViewRemoveUndeployedEnabled()).andReturn(removeUndeployed).anyTimes();

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ViewInstanceEntity viewAutoInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    Capture<ViewInstanceEntity> viewAutoInstanceCapture = EasyMock.newCapture();

    ViewInstanceDataEntity autoInstanceDataEntity = createNiceMock(ViewInstanceDataEntity.class);
    expect(autoInstanceDataEntity.getName()).andReturn("p1").anyTimes();
    expect(autoInstanceDataEntity.getUser()).andReturn(" ").anyTimes();

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", service);
    serviceMap.put("HIVE", service);


    StackId stackId = new StackId("HDP-2.0");

    if(checkAutoInstanceCreation) {
      Map<String, Cluster> allClusters = new HashMap<>();
      expect(cluster.getClusterName()).andReturn("c1").anyTimes();
      expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
      expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
      allClusters.put("c1", cluster);
      expect(clusters.getClusters()).andReturn(allClusters);

      expect(viewInstanceDAO.merge(capture(viewAutoInstanceCapture))).andReturn(viewAutoInstanceEntity).anyTimes();
      expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", "AUTO-INSTANCE")).andReturn(viewAutoInstanceEntity).anyTimes();
      expect(viewAutoInstanceEntity.getInstanceData("p1")).andReturn(autoInstanceDataEntity).anyTimes();
    } else {
      expect(clusters.getClusters()).andReturn(new HashMap<>());
    }

    if (removeUndeployed) {
      expect(viewDAO.findAll()).andReturn(Collections.emptyList());
    }

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, resourceDAO, viewDAO, viewInstanceDAO, clusters,
      cluster, viewAutoInstanceEntity);

    TestViewArchiveUtility archiveUtility =
        new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles, badArchive);

    ViewRegistry registry = getRegistry(viewInstanceOperationHandler, viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, permissionDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, archiveUtility, ambariMetaInfo, clusters);

    registry.readViewArchives();

    ViewEntity view = null;

    // Wait for the view load to complete.
    long timeout = System.currentTimeMillis() + 10000L;
    while (!archiveUtility.isDeploymentFailed() && (view == null || !view.getStatus().equals(ViewDefinition.ViewStatus.DEPLOYED))&&
        System.currentTimeMillis() < timeout) {
      view = registry.getDefinition("MY_VIEW", "1.0.0");
    }

    int instanceDefinitionSize = checkAutoInstanceCreation ? 3: 2;

    if (badArchive) {
      Assert.assertNull(view);
      Assert.assertTrue(archiveUtility.isDeploymentFailed());
    } else {
      Assert.assertNotNull(view);
      Assert.assertEquals(ViewDefinition.ViewStatus.DEPLOYED, view.getStatus());

      Collection<ViewInstanceEntity> instanceDefinitions = registry.getInstanceDefinitions(view);
      ArrayList<ViewInstanceEntity> filteredInstanceDefinition = new ArrayList<>();
      Assert.assertEquals(instanceDefinitionSize, instanceDefinitions.size());

      if(checkAutoInstanceCreation) {
        Assert.assertEquals(viewAutoInstanceCapture.getValue(), registry.getInstanceDefinition("MY_VIEW", "1.0.0", "AUTO-INSTANCE"));
      }

      //Filter out AutoInstance view Entity
      for(ViewInstanceEntity entity : instanceDefinitions) {
        if(!entity.getName().equals("AUTO-INSTANCE")) {
          filteredInstanceDefinition.add(entity);
        }
      }

      for (ViewInstanceEntity viewInstanceEntity : filteredInstanceDefinition) {
        Assert.assertEquals("v1", viewInstanceEntity.getInstanceData("p1").getValue());

        Collection<ViewEntityEntity> entities = viewInstanceEntity.getEntities();
        Assert.assertEquals(1, entities.size());
        ViewEntityEntity viewEntityEntity = entities.iterator().next();
        Assert.assertEquals(99L, (long) viewEntityEntity.getId());
        Assert.assertEquals(viewInstanceEntity.getName(), viewEntityEntity.getViewInstanceName());
      }
    }



    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, resourceDAO, viewDAO, viewInstanceDAO);
  }

  @Test
  public void testReadViewArchives_exception() throws Exception {

    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);
    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File metaInfDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    FileOutputStream fos = createMock(FileOutputStream.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    Set<ViewInstanceEntity> viewInstanceEntities = ViewInstanceEntityTest.getViewInstanceEntities(viewDefinition);
    viewDefinition.setInstances(viewInstanceEntities);

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

    long resourceId = 99L;
    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setId(resourceId);
      resourceEntity.setResourceType(resourceTypeEntity);
      viewInstanceEntity.setResource(resourceEntity);
    }

    Map<String, File> files = new HashMap<>();

    if (System.getProperty("os.name").contains("Windows")) {
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work", extractedArchiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}", archiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\view.xml", entryFile);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF/classes", classesDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF/lib", libDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\META-INF", metaInfDir);
    }
    else {
      files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);
    }

    Map<File, FileOutputStream> outputStreams = new HashMap<>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir).anyTimes();
    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views").anyTimes();
    }
    else {
      expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views").anyTimes();
    }

    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();
    expect(configuration.extractViewsAfterClusterConfig()).andReturn(Boolean.FALSE).anyTimes();

    expect(viewDir.listFiles()).andReturn(new File[]{viewArchive}).anyTimes();

    expect(viewArchive.isDirectory()).andReturn(false);
    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewArchive.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }

    expect(archiveDir.exists()).andReturn(false);
    if (System.getProperty("os.name").contains("Windows")) {
      expect(archiveDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(archiveDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }
    expect(archiveDir.mkdir()).andReturn(true);
    expect(archiveDir.toURI()).andReturn(new URI("file:./"));

    expect(metaInfDir.mkdir()).andReturn(true);

    expect(viewJarFile.getNextJarEntry()).andReturn(jarEntry);
    expect(viewJarFile.getNextJarEntry()).andReturn(null);

    expect(jarEntry.getName()).andReturn("view.xml");
    expect(jarEntry.isDirectory()).andReturn(false);

    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(10);
    expect(viewJarFile.read(anyObject(byte[].class))).andReturn(-1);
    fos.write(anyObject(byte[].class), eq(0), eq(10));

    fos.flush();
    fos.close();
    viewJarFile.closeEntry();
    viewJarFile.close();

    expect(extractedArchiveDir.exists()).andReturn(false);
    expect(extractedArchiveDir.mkdir()).andReturn(true);

    expect(classesDir.exists()).andReturn(true);
    expect(classesDir.toURI()).andReturn(new URI("file:./"));

    expect(libDir.exists()).andReturn(true);

    expect(libDir.listFiles()).andReturn(new File[]{fileEntry});
    expect(fileEntry.toURI()).andReturn(new URI("file:./"));

    expect(viewDAO.findByName("MY_VIEW{1.0.0}")).andThrow(new IllegalArgumentException("Expected exception."));

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, viewDAO);

    TestViewArchiveUtility archiveUtility = new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles, false);

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, permissionDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, archiveUtility, ambariMetaInfo);

    registry.readViewArchives();

    ViewEntity view = null;

    // Wait for the view load to complete.
    long timeout = System.currentTimeMillis() + 10000L;
    while ((view == null || !view.getStatus().equals(ViewDefinition.ViewStatus.ERROR))&&
        System.currentTimeMillis() < timeout) {
      view = registry.getDefinition("MY_VIEW", "1.0.0");
    }

    Assert.assertNotNull(view);
    Assert.assertEquals(ViewDefinition.ViewStatus.ERROR, view.getStatus());

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, fos, viewDAO);
  }

  @Test
  public void testListener() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    TestListener listener = new TestListener();
    registry.registerListener(listener, "MY_VIEW", "1.0.0");

    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_1);

    registry.fireEvent(event);

    Assert.assertEquals(event, listener.getLastEvent());

    listener.clear();

    // fire an event for a different view
    event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_2);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());

    // un-register the listener
    registry.unregisterListener(listener, "MY_VIEW", "1.0.0");

    event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_1);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());
  }

  @Test
  public void testListener_allVersions() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    TestListener listener = new TestListener();
    registry.registerListener(listener, "MY_VIEW", null); // all versions of MY_VIEW

    EventImpl event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_1);

    registry.fireEvent(event);

    Assert.assertEquals(event, listener.getLastEvent());

    listener.clear();

    // fire an event for a different view
    event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_2);

    registry.fireEvent(event);

    Assert.assertEquals(event, listener.getLastEvent());

    listener.clear();

    // un-register the listener
    registry.unregisterListener(listener, "MY_VIEW", null); // all versions of MY_VIEW

    event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_1);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());

    event = EventImplTest.getEvent("MyEvent", Collections.emptyMap(), VIEW_XML_2);

    registry.fireEvent(event);

    Assert.assertNull(listener.getLastEvent());
  }

  @Test
  public void testGetResourceProviders() throws Exception {

    ViewConfig config = ViewConfigTest.getConfig();

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    viewDefinition.setConfiguration(config);

    registry.setupViewDefinition(viewDefinition, getClass().getClassLoader());

    Map<Resource.Type, ResourceProvider> providerMap = registry.getResourceProviders();

    Assert.assertEquals(3, providerMap.size());

    Assert.assertTrue(providerMap.containsKey(Resource.Type.valueOf("MY_VIEW{1.0.0}/resource")));
    Assert.assertTrue(providerMap.containsKey(Resource.Type.valueOf("MY_VIEW{1.0.0}/subresource")));
    Assert.assertTrue(providerMap.containsKey(Resource.Type.valueOf("MY_VIEW{1.0.0}/resources")));
  }

  @Test
  public void testAddGetDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    Assert.assertEquals(viewDefinition, registry.getDefinition("MY_VIEW", "1.0.0"));

    Collection<ViewEntity> viewDefinitions = registry.getDefinitions();

    Assert.assertEquals(1, viewDefinitions.size());

    Assert.assertEquals(viewDefinition, viewDefinitions.iterator().next());
  }

  @Test
  public void testGetDefinition() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName(viewDefinition.getName());

    viewDefinition.setResourceType(resourceTypeEntity);

    registry.addDefinition(viewDefinition);

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYING);

    Assert.assertNull(registry.getDefinition(resourceTypeEntity));

    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYED);

    Assert.assertEquals(viewDefinition, registry.getDefinition(resourceTypeEntity));
  }

  @Test
  public void testAddGetInstanceDefinitions() throws Exception {
    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = ViewInstanceEntityTest.getViewInstanceEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    registry.addInstanceDefinition(viewDefinition, viewInstanceDefinition);

    Assert.assertEquals(viewInstanceDefinition, registry.getInstanceDefinition("MY_VIEW", "1.0.0", "INSTANCE1"));

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewDefinition);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceDefinition, viewInstanceDefinitions.iterator().next());
  }

  @Test
  public void testGetSubResourceDefinitions() throws Exception {
    ViewConfig config = ViewConfigTest.getConfig();

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();

    ViewRegistry registry = ViewRegistry.getInstance();

    viewDefinition.setConfiguration(config);

    registry.setupViewDefinition(viewDefinition, getClass().getClassLoader());

    Set<SubResourceDefinition> subResourceDefinitions =
        registry.getSubResourceDefinitions(viewDefinition.getCommonName(), viewDefinition.getVersion());


    Assert.assertEquals(3, subResourceDefinitions.size());

    Set<String> names = new HashSet<>();
    for (SubResourceDefinition definition : subResourceDefinitions) {
      names.add(definition.getType().name());
    }

    Assert.assertTrue(names.contains("MY_VIEW{1.0.0}/resources"));
    Assert.assertTrue(names.contains("MY_VIEW{1.0.0}/resource"));
    Assert.assertTrue(names.contains("MY_VIEW{1.0.0}/subresource"));
  }

  @Test
  public void testAddInstanceDefinition() throws Exception {
    ViewRegistry registry = ViewRegistry.getInstance();

    ViewEntity viewEntity = ViewEntityTest.getViewEntity();
    InstanceConfig instanceConfig = InstanceConfigTest.getInstanceConfigs().get(0);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity(viewEntity, instanceConfig);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName(viewEntity.getName());

    viewEntity.setResourceType(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(20L);
    resourceEntity.setResourceType(resourceTypeEntity);
    viewInstanceEntity.setResource(resourceEntity);

    registry.addDefinition(viewEntity);
    registry.addInstanceDefinition(viewEntity, viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());
  }

  @Test
  public void testInstallViewInstance() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);

    handlerList.addViewInstance(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, resourceTypeDAO, securityHelper, handlerList);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());
    Assert.assertEquals("MY_VIEW{1.0.0}", viewInstanceEntity.getResource().getResourceType().getName());

    verify(viewDAO, viewInstanceDAO, resourceTypeDAO, securityHelper, handlerList);
  }

  @Test
  public void testInstallViewInstance_invalid() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_INVALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    replay(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);

    registry.addDefinition(viewEntity);
    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected an IllegalStateException");
    } catch (ValidationException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);
  }

  @Test
  public void testInstallViewInstance_validatorPass() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);
    Validator validator = createNiceMock(Validator.class);
    ValidationResult result = createNiceMock(ValidationResult.class);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    viewEntity.setValidator(validator);

    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);

    handlerList.addViewInstance(viewInstanceEntity);

    expect(validator.validateInstance(viewInstanceEntity, Validator.ValidationContext.PRE_CREATE)).andReturn(result).anyTimes();
    expect(result.isValid()).andReturn(true).anyTimes();

    replay(viewDAO, viewInstanceDAO, securityHelper, handlerList, validator, result);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper, handlerList, validator, result);
  }

  @Test
  public void testInstallViewInstance_validatorFail() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);
    Validator validator = createNiceMock(Validator.class);
    ValidationResult result = createNiceMock(ValidationResult.class);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    viewEntity.setValidator(validator);

    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(validator.validateInstance(viewInstanceEntity, Validator.ValidationContext.PRE_CREATE)).andReturn(result).anyTimes();
    expect(result.isValid()).andReturn(false).anyTimes();

    replay(viewDAO, viewInstanceDAO, securityHelper, handlerList, validator, result);

    registry.addDefinition(viewEntity);

    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected a ValidationException");
    } catch (ValidationException e) {
      // expected
    }

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertTrue(viewInstanceDefinitions.isEmpty());

    verify(viewDAO, viewInstanceDAO, securityHelper, handlerList, validator, result);
  }

  @Test
  public void testInstallViewInstance_unknownView() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    viewInstanceEntity.setViewName("BOGUS_VIEW");

    replay(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);

    registry.addDefinition(viewEntity);
    try {
      registry.installViewInstance(viewInstanceEntity);
      Assert.fail("expected an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper, resourceTypeDAO);
  }

  @Test
  public void testUpdateViewInstance() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity).anyTimes();

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    Assert.assertTrue(viewInstanceEntity.isVisible());

    updateInstance.setLabel("new label");
    updateInstance.setDescription("new description");
    updateInstance.setVisible(false);

    registry.updateViewInstance(updateInstance);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );
    Assert.assertEquals("new label", instanceEntity.getLabel() );
    Assert.assertEquals("new description", instanceEntity.getDescription() );
    Assert.assertFalse(instanceEntity.isVisible());

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testSetViewInstanceProperties() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));


    Map<String, String> instanceProperties = new HashMap<>();
    instanceProperties.put("p1", "newV1");
    instanceProperties.put("p2", "newV2");

    registry.setViewInstanceProperties(viewInstanceEntity, instanceProperties, viewEntity.getConfiguration(), viewEntity.getClassLoader());

    Assert.assertEquals("newV1", viewInstanceEntity.getProperty("p1").getValue());
    Assert.assertEquals("bmV3VjI=", viewInstanceEntity.getProperty("p2").getValue());
  }

  @Test
  public void testUninstallViewInstance() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Configuration ambariConfig = new Configuration(new Properties());

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    viewInstanceOperationHandler.uninstallViewInstance(viewInstanceEntity);
    handlerList.removeViewInstance(viewInstanceEntity);
    replay(viewInstanceOperationHandler,/* viewInstanceDAO, privilegeDAO,*/ handlerList/*, privilege1, privilege2, principalEntity*/);

    registry.addDefinition(viewEntity);
    registry.addInstanceDefinition(viewEntity, viewInstanceEntity);
    registry.uninstallViewInstance(viewInstanceEntity);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(0, viewInstanceDefinitions.size());

    verify(viewInstanceOperationHandler, /*viewInstanceDAO, privilegeDAO,*/ handlerList/*, privilege1, privilege2, principalEntity*/);
  }

  @Test
  public void testUpdateViewInstance_invalid() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewConfig invalidConfig = ViewConfigTest.getConfig(XML_INVALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, invalidConfig.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);

    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    try {
      registry.updateViewInstance(updateInstance);
      Assert.fail("expected an IllegalStateException");
    } catch (ValidationException e) {
      // expected
    }
    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testUpdateViewInstance_validatorPass() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);
    Validator validator = createNiceMock(Validator.class);
    ValidationResult result = createNiceMock(ValidationResult.class);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    viewEntity.setValidator(validator);
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity).anyTimes();

    expect(validator.validateInstance(viewInstanceEntity, Validator.ValidationContext.PRE_UPDATE)).andReturn(result).anyTimes();
    expect(result.isValid()).andReturn(true).anyTimes();

    replay(viewDAO, viewInstanceDAO, securityHelper, validator, result);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    registry.updateViewInstance(updateInstance);

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper, validator, result);
  }

  @Test
  public void testUpdateViewInstance_validatorFail() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    Properties properties = new Properties();
    properties.put("p1", "v1");

    Configuration ambariConfig = new Configuration(properties);
    Validator validator = createNiceMock(Validator.class);
    ValidationResult result = createNiceMock(ValidationResult.class);

    ViewConfig config = ViewConfigTest.getConfig(XML_VALID_INSTANCE);
    ViewEntity viewEntity = getViewEntity(config, ambariConfig, getClass().getClassLoader(), "");
    viewEntity.setValidator(validator);
    ViewInstanceEntity viewInstanceEntity = getViewInstanceEntity(viewEntity, config.getInstances().get(0));
    ViewInstanceEntity updateInstance = getViewInstanceEntity(viewEntity, config.getInstances().get(0));

    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", viewInstanceEntity.getInstanceName())).andReturn(viewInstanceEntity).anyTimes();

    expect(validator.validateInstance(viewInstanceEntity, Validator.ValidationContext.PRE_UPDATE)).andReturn(result).anyTimes();
    expect(result.isValid()).andReturn(false).anyTimes();

    replay(viewDAO, viewInstanceDAO, securityHelper, validator, result);

    registry.addDefinition(viewEntity);
    registry.installViewInstance(viewInstanceEntity);

    try {
      registry.updateViewInstance(updateInstance);
      Assert.fail("expected a ValidationException");
    } catch (ValidationException e) {
      // expected
    }

    Collection<ViewInstanceEntity> viewInstanceDefinitions = registry.getInstanceDefinitions(viewEntity);

    Assert.assertEquals(1, viewInstanceDefinitions.size());

    ViewInstanceEntity instanceEntity = viewInstanceDefinitions.iterator().next();
    Assert.assertEquals("v2-1", instanceEntity.getProperty("p2").getValue() );

    Assert.assertEquals(viewInstanceEntity, viewInstanceDefinitions.iterator().next());

    verify(viewDAO, viewInstanceDAO, securityHelper, validator, result);
  }

  @Test
  public void testRemoveInstanceData() throws Exception {

    ViewRegistry registry = ViewRegistry.getInstance();

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();

    viewInstanceEntity.putInstanceData("foo", "value");

    ViewInstanceDataEntity dataEntity = viewInstanceEntity.getInstanceData("foo");

    viewInstanceDAO.removeData(dataEntity);
    expect(viewInstanceDAO.merge(viewInstanceEntity)).andReturn(viewInstanceEntity);
    replay(viewDAO, viewInstanceDAO, securityHelper);

    registry.removeInstanceData(viewInstanceEntity, "foo");

    Assert.assertNull(viewInstanceEntity.getInstanceData("foo"));
    verify(viewDAO, viewInstanceDAO, securityHelper);
  }

  @Test
  public void testIncludeDefinitionForAdmin() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);

    replay(configuration);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    Assert.assertTrue(registry.includeDefinition(viewEntity));

    verify(configuration);
  }

  @Test
  public void testIncludeDefinitionForUserNoInstances() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);

    expect(viewEntity.getInstances()).andReturn(Collections.emptyList()).anyTimes();

    replay(viewEntity, configuration);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser(1L));

    Assert.assertFalse(registry.includeDefinition(viewEntity));

    verify(viewEntity, configuration);
  }

  @Test
  public void testIncludeDefinitionForUserHasAccess() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewInstanceEntity instanceEntity = createNiceMock(ViewInstanceEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    ResourceTypeEntity resourceTypeEntity = createNiceMock(ResourceTypeEntity.class);

    Collection<ViewInstanceEntity> instances = new ArrayList<>();
    instances.add(instanceEntity);

    expect(viewEntity.getInstances()).andReturn(instances);
    expect(instanceEntity.getResource()).andReturn(resourceEntity);
    expect(resourceEntity.getId()).andReturn(54L).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    expect(resourceTypeEntity.getId()).andReturn(ResourceType.VIEW.getId()).anyTimes();
    expect(resourceTypeEntity.getName()).andReturn(ResourceType.VIEW.name()).anyTimes();

    replay(viewEntity, instanceEntity, resourceEntity, resourceTypeEntity, configuration);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createViewUser(resourceEntity.getId()));

    Assert.assertTrue(registry.includeDefinition(viewEntity));

    verify(viewEntity, instanceEntity, resourceEntity, resourceTypeEntity, configuration);
  }

  @Test
  public void testOnAmbariEventServiceCreation() throws Exception {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("HDFS");
    serviceNames.add("HIVE");

    testOnAmbariEventServiceCreation(AUTO_VIEW_XML, serviceNames, EXPECTED_HDP_2_0_STACK_NAME,true);
  }

  @Test
  public void testOnAmbariEventServiceCreation_widcardStackVersion() throws Exception {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("HDFS");
    serviceNames.add("HIVE");

    testOnAmbariEventServiceCreation(AUTO_VIEW_WILD_STACK_XML, serviceNames, EXPECTED_HDP_2_0_STACK_NAME,true);
  }

  @Test
  public void testOnAmbariEventServiceCreation_widcardAllStacks() throws Exception {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("HDFS");
    serviceNames.add("HIVE");

    testOnAmbariEventServiceCreation(AUTO_VIEW_WILD_ALL_STACKS_XML, serviceNames, "HDF-3.1", true);
  }

  @Test
  public void testOnAmbariEventServiceCreation_mismatchStackVersion() throws Exception {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("HDFS");
    serviceNames.add("HIVE");

    testOnAmbariEventServiceCreation(AUTO_VIEW_BAD_STACK_XML, serviceNames, EXPECTED_HDP_2_0_STACK_NAME, false);
  }

  @Test
  public void testOnAmbariEventServiceCreation_missingClusterService() throws Exception {
    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("STORM");
    serviceNames.add("HIVE");

    testOnAmbariEventServiceCreation(AUTO_VIEW_XML, serviceNames, EXPECTED_HDP_2_0_STACK_NAME, false);
  }

  @Test
  public void testCheckViewVersions() {
    ViewRegistry registry = ViewRegistry.getInstance();
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);

    ViewConfig config = createNiceMock(ViewConfig.class);

    expect(viewEntity.getConfiguration()).andReturn(config).anyTimes();

    // null, null
    expect(config.getMinAmbariVersion()).andReturn(null);
    expect(config.getMaxAmbariVersion()).andReturn(null);

    // 1.0.0, 3.0.0
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn("3.0.0");

    // 1.0.0, 1.5
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn("1.5");

    // 2.5
    expect(config.getMinAmbariVersion()).andReturn("2.5");

    // null, 3.0.0
    expect(config.getMinAmbariVersion()).andReturn(null);
    expect(config.getMaxAmbariVersion()).andReturn("3.0.0");

    // 1.0.0, null
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn(null);

    // 1.0.0, *
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn("*");

    // *, 3.0.0
    expect(config.getMinAmbariVersion()).andReturn("*");
    expect(config.getMaxAmbariVersion()).andReturn("3.0.0");

    // 1.0.0, 2.*
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn("2.*");

    // 1.*, 3.0.0
    expect(config.getMinAmbariVersion()).andReturn("1.*");
    expect(config.getMaxAmbariVersion()).andReturn("3.0.0");

    // 1.0.0, 2.1.*
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn("2.1.*");

    // 1.5.*, 3.0.0
    expect(config.getMinAmbariVersion()).andReturn("1.5.*");
    expect(config.getMaxAmbariVersion()).andReturn("3.0.0");

    // 1.0.0, 1.9.9.*
    expect(config.getMinAmbariVersion()).andReturn("1.0.0");
    expect(config.getMaxAmbariVersion()).andReturn("1.9.9.*");

    // 2.0.0.1.*, 3.0.0
    expect(config.getMinAmbariVersion()).andReturn("2.0.0.1.*");

    replay(viewEntity, config);

    // null, null
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, 3.0.0
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, 1.5
    Assert.assertFalse(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 2.5, 3.0.0
    Assert.assertFalse(registry.checkViewVersions(viewEntity, "2.0.0"));

    // null, 3.0.0
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, null
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, *
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // *, 3.0.0
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, 2.*
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.*, 3.0.0
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, 2.1.*
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.5.*, 3.0.0
    Assert.assertTrue(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 1.0.0, 1.9.9.*
    Assert.assertFalse(registry.checkViewVersions(viewEntity, "2.0.0"));

    // 2.0.0.1.*, 3.0.0
    Assert.assertFalse(registry.checkViewVersions(viewEntity, "2.0.0"));

    verify(viewEntity, config);

  }

  @Test
  public void testExtractViewArchive() throws Exception {

    File viewDir = createNiceMock(File.class);
    File extractedArchiveDir = createNiceMock(File.class);
    File viewArchive = createNiceMock(File.class);

    File archiveDir = createNiceMock(File.class);
    File entryFile  = createNiceMock(File.class);
    File classesDir = createNiceMock(File.class);
    File libDir = createNiceMock(File.class);
    File metaInfDir = createNiceMock(File.class);
    File fileEntry = createNiceMock(File.class);

    JarInputStream viewJarFile = createNiceMock(JarInputStream.class);
    JarEntry jarEntry = createNiceMock(JarEntry.class);
    InputStream is = createMock(InputStream.class);
    FileOutputStream fos = createMock(FileOutputStream.class);
    ViewExtractor viewExtractor = createMock(ViewExtractor.class);

    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(10);
    resourceTypeEntity.setName("MY_VIEW{1.0.0}");

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    viewDefinition.setResourceType(resourceTypeEntity);

    Set<ViewInstanceEntity> viewInstanceEntities = ViewInstanceEntityTest.getViewInstanceEntities(viewDefinition);
    viewDefinition.setInstances(viewInstanceEntities);

    Map<File, ViewConfig> viewConfigs =
        Collections.singletonMap(viewArchive, viewDefinition.getConfiguration());

    long resourceId = 99L;
    for (ViewInstanceEntity viewInstanceEntity : viewInstanceEntities) {
      ResourceEntity resourceEntity = new ResourceEntity();
      resourceEntity.setId(resourceId);
      resourceEntity.setResourceType(resourceTypeEntity);
      viewInstanceEntity.setResource(resourceEntity);
    }

    Map<String, File> files = new HashMap<>();

    if (System.getProperty("os.name").contains("Windows")) {
      files.put("\\var\\lib\\ambari-server\\resources\\views\\my_view-1.0.0.jar", viewArchive);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work", extractedArchiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}", archiveDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\view.xml", entryFile);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF\\classes", classesDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\WEB-INF\\lib", libDir);
      files.put("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}\\META-INF", metaInfDir);
    }
    else {
      files.put("/var/lib/ambari-server/resources/views/my_view-1.0.0.jar", viewArchive);
      files.put("/var/lib/ambari-server/resources/views/work", extractedArchiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}", archiveDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/view.xml", entryFile);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/classes", classesDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/WEB-INF/lib", libDir);
      files.put("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}/META-INF", metaInfDir);
    }

    Map<File, FileOutputStream> outputStreams = new HashMap<>();
    outputStreams.put(entryFile, fos);

    Map<File, JarInputStream> jarFiles = new HashMap<>();
    jarFiles.put(viewArchive, viewJarFile);

    // set expectations
    expect(configuration.getViewsDir()).andReturn(viewDir);
    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views");
    }
    else {
      expect(viewDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views");
    }

    expect(configuration.getViewExtractionThreadPoolCoreSize()).andReturn(2).anyTimes();
    expect(configuration.getViewExtractionThreadPoolMaxSize()).andReturn(3).anyTimes();
    expect(configuration.getViewExtractionThreadPoolTimeout()).andReturn(10000L).anyTimes();

    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewArchive.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(viewArchive.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }

    expect(archiveDir.exists()).andReturn(false);
    if (System.getProperty("os.name").contains("Windows")) {
      expect(archiveDir.getAbsolutePath()).andReturn("\\var\\lib\\ambari-server\\resources\\views\\work\\MY_VIEW{1.0.0}").anyTimes();
    }
    else {
      expect(archiveDir.getAbsolutePath()).andReturn("/var/lib/ambari-server/resources/views/work/MY_VIEW{1.0.0}").anyTimes();
    }

    Capture<ViewEntity> viewEntityCapture = EasyMock.newCapture();
    if (System.getProperty("os.name").contains("Windows")) {
      expect(viewExtractor.ensureExtractedArchiveDirectory("\\var\\lib\\ambari-server\\resources\\views\\work")).andReturn(true);
    }
    else {
      expect(viewExtractor.ensureExtractedArchiveDirectory("/var/lib/ambari-server/resources/views/work")).andReturn(true);
    }
    expect(viewExtractor.extractViewArchive(capture(viewEntityCapture), eq(viewArchive), eq(archiveDir), anyObject(List.class))).andReturn(null);

    // replay mocks
    replay(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, is, fos, viewExtractor, resourceDAO, viewDAO, viewInstanceDAO);

    TestViewArchiveUtility archiveUtility = new TestViewArchiveUtility(viewConfigs, files, outputStreams, jarFiles, false);

    TestViewModule module = new TestViewModule(viewExtractor, archiveUtility, configuration);

    if (System.getProperty("os.name").contains("Windows")) {
      Assert.assertTrue(ViewRegistry.extractViewArchive("\\var\\lib\\ambari-server\\resources\\views\\my_view-1.0.0.jar",
          module, true));
    }
    else {
      Assert.assertTrue(ViewRegistry.extractViewArchive("/var/lib/ambari-server/resources/views/my_view-1.0.0.jar",
          module, true));
    }

    // verify mocks
    verify(configuration, viewDir, extractedArchiveDir, viewArchive, archiveDir, entryFile, classesDir,
        libDir, metaInfDir, fileEntry, viewJarFile, jarEntry, is, fos, viewExtractor, resourceDAO, viewDAO, viewInstanceDAO);
  }

  @Test
  public void testSetViewInstanceRoleAccess() throws Exception {

    final Map<String, PermissionEntity> permissions = new HashMap<>();
    permissions.put("CLUSTER.ADMINISTRATOR", TestAuthenticationFactory.createClusterAdministratorPermission());
    permissions.put("CLUSTER.OPERATOR", TestAuthenticationFactory.createClusterOperatorPermission());
    permissions.put("SERVICE.ADMINISTRATOR", TestAuthenticationFactory.createServiceAdministratorPermission());
    permissions.put("SERVICE.OPERATOR", TestAuthenticationFactory.createServiceOperatorPermission());
    permissions.put("CLUSTER.USER", TestAuthenticationFactory.createClusterUserPermission());

    PermissionEntity permissionViewUser = TestAuthenticationFactory.createViewUserPermission();

    ViewInstanceEntity viewInstanceEntity = ViewInstanceEntityTest.getViewInstanceEntity();
    ResourceEntity resourceEntity = viewInstanceEntity.getResource();

    // Expected PrivilegeEntity items to be created...
    Map<String, PrivilegeEntity> expectedPrivileges = new HashMap<>();
    for (Map.Entry<String, PermissionEntity> entry : permissions.entrySet()) {
      if(!entry.getKey().equals("CLUSTER.ADMINISTRATOR")) {
        expectedPrivileges.put(entry.getKey(), TestAuthenticationFactory.createPrivilegeEntity(resourceEntity, permissionViewUser, entry.getValue().getPrincipal()));
      }
    }

    Capture<PrivilegeEntity> captureCreatedPrivilegeEntity = Capture.newInstance(CaptureType.ALL);

    for (Map.Entry<String, PermissionEntity> entry : permissions.entrySet()) {
      expect(permissionDAO.findByName(entry.getKey())).andReturn(entry.getValue()).atLeastOnce();
    }
    expect(permissionDAO.findViewUsePermission()).andReturn(permissionViewUser).atLeastOnce();

    // The CLUSTER.ADMINISTRATOR privilege for this View instance already exists...
    expect(privilegeDAO.exists(EasyMock.anyObject(PrincipalEntity.class), eq(resourceEntity), eq(permissionViewUser)))
        .andAnswer(new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            return EasyMock.getCurrentArguments()[0] == permissions.get("CLUSTER.ADMINISTRATOR").getPrincipal();
          }
        })
        .anyTimes();

    privilegeDAO.create(capture(captureCreatedPrivilegeEntity));
    expectLastCall().times(expectedPrivileges.size());

    replay(privilegeDAO, permissionDAO);

    ViewRegistry viewRegistry = ViewRegistry.getInstance();

    viewRegistry.setViewInstanceRoleAccess(viewInstanceEntity, permissions.keySet());

    verify(privilegeDAO, permissionDAO);

    Assert.assertTrue(expectedPrivileges.size() != permissions.size());

    Assert.assertTrue(captureCreatedPrivilegeEntity.hasCaptured());

    List<PrivilegeEntity> capturedValues = captureCreatedPrivilegeEntity.getValues();
    Assert.assertNotNull( capturedValues);

    Set<PrivilegeEntity> uniqueCapturedValues = new HashSet<>(capturedValues);
    Assert.assertEquals(expectedPrivileges.size(), uniqueCapturedValues.size());

    for(PrivilegeEntity capturedValue: uniqueCapturedValues) {
      Assert.assertTrue(expectedPrivileges.containsValue(capturedValue));
    }
  }

  public static class TestViewModule extends ViewRegistry.ViewModule {

    private final ViewExtractor extractor;
    private final ViewArchiveUtility archiveUtility;
    private final Configuration configuration;

    public TestViewModule(ViewExtractor extractor, ViewArchiveUtility archiveUtility, Configuration configuration) {
      this.extractor = extractor;
      this.archiveUtility = archiveUtility;
      this.configuration = configuration;
    }

    @Override
    protected void configure() {
      bind(ViewExtractor.class).toInstance(extractor);
      bind(ViewArchiveUtility.class).toInstance(archiveUtility);
      bind(Configuration.class).toInstance(configuration);

      OsFamily osFamily = createNiceMock(OsFamily.class);
      replay(osFamily);
      bind(OsFamily.class).toInstance(osFamily);
    }
  }

  public static class TestViewArchiveUtility extends ViewArchiveUtility {
    private final Map<File, ViewConfig> viewConfigs;
    private final Map<String, File> files;
    private final Map<File, FileOutputStream> outputStreams;
    private final Map<File, JarInputStream> jarFiles;
    private final boolean badArchive;
    private boolean deploymentFailed = false;

    public TestViewArchiveUtility(Map<File, ViewConfig> viewConfigs, Map<String, File> files, Map<File,
        FileOutputStream> outputStreams, Map<File, JarInputStream> jarFiles, boolean badArchive) {
      this.viewConfigs = viewConfigs;
      this.files = files;
      this.outputStreams = outputStreams;
      this.jarFiles = jarFiles;
      this.badArchive = badArchive;
    }

    @Override
    public ViewConfig getViewConfigFromArchive(File archiveFile) throws MalformedURLException, JAXBException {
      if (badArchive) {
        deploymentFailed = true;
        throw new IllegalStateException("Bad archive");
      }
      return viewConfigs.get(archiveFile);
    }

    @Override
    public ViewConfig getViewConfigFromExtractedArchive(String archivePath, boolean validate)
        throws JAXBException, FileNotFoundException {
      if (badArchive) {
        deploymentFailed = true;
        throw new IllegalStateException("Bad archive");
      }

      for (File viewConfigKey: viewConfigs.keySet()) {
        if (viewConfigKey.getAbsolutePath().equals(archivePath)) {
          return viewConfigs.get(viewConfigKey);
        }
      }
      return null;
    }

    @Override
    public File getFile(String path) {
      return files.get(path);
    }

    @Override
    public FileOutputStream getFileOutputStream(File file) throws FileNotFoundException {
      return outputStreams.get(file);
    }

    @Override
    public JarInputStream getJarFileStream(File file) throws IOException {
      return jarFiles.get(file);
    }

    public boolean isDeploymentFailed() {
      return deploymentFailed;
    }
  }

  private static class TestListener implements Listener {
    private Event lastEvent = null;

    @Override
    public void notify(Event event) {
      lastEvent = event;
    }

    public Event getLastEvent() {
      return lastEvent;
    }

    public void clear() {
      lastEvent = null;
    }
  }

  public static ViewRegistry getRegistry(ViewDAO viewDAO, ViewInstanceDAO viewInstanceDAO,
                                         UserDAO userDAO, MemberDAO memberDAO,
                                         PrivilegeDAO privilegeDAO, PermissionDAO permissionDAO,
                                         ResourceDAO resourceDAO,
                                         ResourceTypeDAO resourceTypeDAO, SecurityHelper securityHelper,
                                         ViewInstanceHandlerList handlerList,
                                         ViewExtractor viewExtractor,
                                         ViewArchiveUtility archiveUtility,
                                         AmbariMetaInfo ambariMetaInfo) {
    return getRegistry(null, viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO, permissionDAO,
        resourceDAO, resourceTypeDAO, securityHelper, handlerList, viewExtractor, archiveUtility,
        ambariMetaInfo, null);
  }

  public static ViewRegistry getRegistry(ViewInstanceOperationHandler viewInstanceOperationHandler, ViewDAO viewDAO, ViewInstanceDAO viewInstanceDAO,
                                         UserDAO userDAO, MemberDAO memberDAO,
                                         PrivilegeDAO privilegeDAO, PermissionDAO permissionDAO,
                                         ResourceDAO resourceDAO, ResourceTypeDAO resourceTypeDAO,
                                         SecurityHelper securityHelper,
                                         ViewInstanceHandlerList handlerList,
                                         ViewExtractor viewExtractor,
                                         ViewArchiveUtility archiveUtility,
                                         AmbariMetaInfo ambariMetaInfo,
                                         Clusters clusters) {



    AmbariEventPublisher publisher = createNiceMock(AmbariEventPublisher.class);
    replay(publisher);


    ViewRegistry instance = new ViewRegistry(publisher);

    instance.viewInstanceOperationHandler = viewInstanceOperationHandler;
    instance.viewDAO = viewDAO;
    instance.resourceDAO = resourceDAO;
    instance.instanceDAO = viewInstanceDAO;
    instance.userDAO = userDAO;
    instance.memberDAO = memberDAO;
    instance.privilegeDAO = privilegeDAO;
    instance.resourceTypeDAO = resourceTypeDAO;
    instance.permissionDAO = permissionDAO;
    instance.securityHelper = securityHelper;
    instance.configuration = configuration;
    instance.handlerList = handlerList;
    instance.extractor = viewExtractor == null ? new ViewExtractor() : viewExtractor;
    instance.archiveUtility = archiveUtility == null ? new ViewArchiveUtility() : archiveUtility;
    instance.extractor.archiveUtility = instance.archiveUtility;

    final AmbariMetaInfo finalMetaInfo = ambariMetaInfo;
    instance.ambariMetaInfoProvider = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return finalMetaInfo;
      }
    };

    final Clusters finalClusters = clusters;
    instance.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return finalClusters;
      }
    };
    return instance;
  }

  public static ViewEntity getViewEntity(ViewConfig viewConfig, Configuration ambariConfig,
                                     ClassLoader cl, String archivePath) throws Exception{

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        permissionDAO, resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null, null);

    ViewEntity viewDefinition = new ViewEntity(viewConfig, ambariConfig, archivePath);

    registry.setupViewDefinition(viewDefinition, cl);

    return viewDefinition;
  }

  public static ViewInstanceEntity getViewInstanceEntity(ViewEntity viewDefinition, InstanceConfig instanceConfig) throws Exception {

    ViewRegistry registry = getRegistry(viewDAO, viewInstanceDAO, userDAO, memberDAO, privilegeDAO,
        permissionDAO, resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null, null);

    ViewInstanceEntity viewInstanceDefinition =
        new ViewInstanceEntity(viewDefinition, instanceConfig);

    for (PropertyConfig propertyConfig : instanceConfig.getProperties()) {
      viewInstanceDefinition.putProperty(propertyConfig.getKey(), propertyConfig.getValue());
    }

    registry.bindViewInstance(viewDefinition, viewInstanceDefinition);
    return viewInstanceDefinition;
  }

  private void testOnAmbariEventServiceCreation(String xml, Set<String> serviceNames, String stackName, boolean success) throws Exception {
    ViewConfig config = ViewConfigTest.getConfig(xml);

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity(config);

    ViewRegistry registry = ViewRegistry.getInstance();
    registry.addDefinition(viewDefinition);

    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    StackId stackId = new StackId(stackName);


    Map<String, Service> serviceMap = new HashMap<>();
    for (String serviceName : serviceNames) {
      serviceMap.put(serviceName, service);
      expect(cluster.getService(serviceName)).andReturn(service);
    }

    expect(clusters.getClusterById(99L)).andReturn(cluster);
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(service.getDesiredStackId()).andReturn(stackId).anyTimes();

    Capture<ViewInstanceEntity> viewInstanceCapture = EasyMock.newCapture();

    expect(viewInstanceDAO.merge(capture(viewInstanceCapture))).andReturn(viewInstanceEntity).anyTimes();
    expect(viewInstanceDAO.findByName("MY_VIEW{1.0.0}", "AUTO-INSTANCE")).andReturn(viewInstanceEntity).anyTimes();

    replay(securityHelper, configuration, viewInstanceDAO, clusters, cluster, service, viewInstanceEntity);


    ServiceInstalledEvent event = new ServiceInstalledEvent(99L, "HDP", "2.0", "HIVE");

    registry.onAmbariEvent(event);

    if (success) {
      Assert.assertEquals(viewInstanceCapture.getValue(), registry.getInstanceDefinition("MY_VIEW", "1.0.0", "AUTO-INSTANCE"));
    }

    verify(securityHelper, configuration, viewInstanceDAO, clusters, cluster, viewInstanceEntity);
  }
}
