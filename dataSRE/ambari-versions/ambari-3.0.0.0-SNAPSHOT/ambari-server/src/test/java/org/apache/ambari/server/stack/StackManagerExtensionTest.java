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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.controller.AmbariManagementHelper;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ThemeInfo;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMock;
import org.junit.Test;

/**
 * StackManager extension unit tests.
 */
public class StackManagerExtensionTest  {

  @Test
  public void testExtensions() throws Exception {
    MetainfoDAO metaInfoDao = createNiceMock(MetainfoDAO.class);
    StackDAO stackDao = createNiceMock(StackDAO.class);
    ExtensionDAO extensionDao = createNiceMock(ExtensionDAO.class);
    ExtensionLinkDAO linkDao = createNiceMock(ExtensionLinkDAO.class);
    ActionMetadata actionMetadata = createNiceMock(ActionMetadata.class);
    OsFamily osFamily = createNiceMock(OsFamily.class);
    StackEntity stack1 = new StackEntity();
    stack1.setStackName("HDP");
    stack1.setStackVersion("0.1");
    StackEntity stack2 = new StackEntity();
    stack2.setStackName("HDP");
    stack2.setStackVersion("0.2");
    StackEntity stack3 = new StackEntity();
    stack3.setStackName("HDP");
    stack3.setStackVersion("0.3");
    StackEntity stack4 = new StackEntity();
    stack4.setStackName("HDP");
    stack4.setStackVersion("0.4");
    ExtensionEntity extension1 = new ExtensionEntity();
    extension1.setExtensionName("EXT");
    extension1.setExtensionVersion("0.1");
    ExtensionEntity extension2 = new ExtensionEntity();
    extension2.setExtensionName("EXT");
    extension2.setExtensionVersion("0.2");
    ExtensionEntity extension3 = new ExtensionEntity();
    extension3.setExtensionName("EXT");
    extension3.setExtensionVersion("0.3");
    ExtensionEntity extension4 = new ExtensionEntity();
    extension4.setExtensionName("EXT");
    extension4.setExtensionVersion("0.4");
    ExtensionLinkEntity link1 = new ExtensionLinkEntity();
    link1.setLinkId(new Long(-1));
    link1.setStack(stack1);
    link1.setExtension(extension1);
    List<ExtensionLinkEntity> list = new ArrayList<>();
    List<ExtensionLinkEntity> linkList = new ArrayList<>();
    linkList.add(link1);

    expect(stackDao.find("HDP", "0.1")).andReturn(stack1).atLeastOnce();
    expect(stackDao.find("HDP", "0.2")).andReturn(stack2).atLeastOnce();
    expect(stackDao.find("HDP", "0.3")).andReturn(stack3).atLeastOnce();
    expect(stackDao.find("HDP", "0.4")).andReturn(stack3).atLeastOnce();
    expect(extensionDao.find("EXT", "0.1")).andReturn(extension1).atLeastOnce();
    expect(extensionDao.find("EXT", "0.2")).andReturn(extension2).atLeastOnce();
    expect(extensionDao.find("EXT", "0.3")).andReturn(extension3).atLeastOnce();
    expect(extensionDao.find("EXT", "0.4")).andReturn(extension4).atLeastOnce();

    expect(linkDao.findByStack("HDP", "0.1")).andReturn(linkList).atLeastOnce();
    expect(linkDao.findByStack(EasyMock.anyObject(String.class),
            EasyMock.anyObject(String.class))).andReturn(list).atLeastOnce();

    expect(linkDao.findByStackAndExtension("HDP", "0.2", "EXT", "0.2")).andReturn(null).atLeastOnce();
    expect(linkDao.findByStackAndExtension("HDP", "0.1", "EXT", "0.1")).andReturn(link1).atLeastOnce();

    expect(linkDao.merge(link1)).andReturn(link1).atLeastOnce();

    replay(actionMetadata, stackDao, metaInfoDao, osFamily, extensionDao, linkDao); //linkEntity

    String stacks = ClassLoader.getSystemClassLoader().getResource("stacks_with_extensions").getPath();
    String common = ClassLoader.getSystemClassLoader().getResource("common-services").getPath();
    String extensions = ClassLoader.getSystemClassLoader().getResource("extensions").getPath();

    AmbariManagementHelper helper = new AmbariManagementHelper(stackDao, extensionDao, linkDao);

    StackManager stackManager = null;
    try {
      stackManager = new StackManager(new File(stacks),
        new File(common), new File(extensions), osFamily, false,
        metaInfoDao, actionMetadata, stackDao, extensionDao, linkDao, helper);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    assertNotNull("Failed to create Stack Manager", stackManager);

    ExtensionInfo extension = stackManager.getExtension("EXT", "0.1");
    assertNull("EXT 0.1's parent: " + extension.getParentExtensionVersion(), extension.getParentExtensionVersion());
    assertNotNull(extension.getService("OOZIE2"));
    ServiceInfo oozie = extension.getService("OOZIE2");
    assertNotNull("Package dir is " + oozie.getServicePackageFolder(), oozie.getServicePackageFolder());
    assertTrue("Package dir is " + oozie.getServicePackageFolder(), oozie.getServicePackageFolder().contains("extensions/EXT/0.1/services/OOZIE2/package"));
    assertEquals(oozie.getVersion(), "3.2.0");
    File checks = oozie.getChecksFolder();
    assertNotNull(checks);
    assertTrue("Checks dir is " + checks.getPath(), checks.getPath().contains("extensions/EXT/0.1/services/OOZIE2/checks"));
    File serverActions = oozie.getServerActionsFolder();
    assertNotNull(serverActions);
    assertTrue("Server actions dir is " + serverActions.getPath(), serverActions.getPath().contains("extensions/EXT/0.1/services/OOZIE2/server_actions"));
    List<ThemeInfo> themes = oozie.getThemes();
    assertNotNull(themes);
    assertTrue("Number of themes is " + themes.size(), themes.size() == 1);
    ThemeInfo theme = themes.get(0);
    assertTrue("Theme: " + theme.getFileName(), theme.getFileName().contains("working_theme.json"));

    extension = stackManager.getExtension("EXT", "0.2");
    assertNotNull("EXT 0.2's parent: " + extension.getParentExtensionVersion(), extension.getParentExtensionVersion());
    assertEquals("EXT 0.2's parent: " + extension.getParentExtensionVersion(), "0.1", extension.getParentExtensionVersion());
    assertNotNull(extension.getService("OOZIE2"));
    assertTrue("Extension is set to auto link", !extension.isAutoLink());
    oozie = extension.getService("OOZIE2");
    assertNotNull("Package dir is " + oozie.getServicePackageFolder(), oozie.getServicePackageFolder());
    assertTrue("Package dir is " + oozie.getServicePackageFolder(), oozie.getServicePackageFolder().contains("extensions/EXT/0.1/services/OOZIE2/package"));
    assertEquals(oozie.getVersion(), "4.0.0");
    checks = oozie.getChecksFolder();
    assertNotNull(checks);
    assertTrue("Checks dir is " + checks.getPath(), checks.getPath().contains("extensions/EXT/0.1/services/OOZIE2/checks"));
    serverActions = oozie.getServerActionsFolder();
    assertNotNull(serverActions);
    assertTrue("Server actions dir is " + serverActions.getPath(), serverActions.getPath().contains("extensions/EXT/0.1/services/OOZIE2/server_actions"));
    themes = oozie.getThemes();
    assertNotNull(themes);
    assertTrue("Number of themes is " + themes.size(), themes.size() == 0);

    extension = stackManager.getExtension("EXT", "0.3");
    assertTrue("Extension is not set to auto link", extension.isAutoLink());

    StackInfo stack = stackManager.getStack("HDP", "0.1");
    assertNotNull(stack.getService("OOZIE2"));
    oozie = stack.getService("OOZIE2");
    assertNotNull("Package dir is " + oozie.getServicePackageFolder(), oozie.getServicePackageFolder());
    assertTrue("Package dir is " + oozie.getServicePackageFolder(), oozie.getServicePackageFolder().contains("extensions/EXT/0.1/services/OOZIE2/package"));
    assertEquals(oozie.getVersion(), "3.2.0");
    assertTrue("Extensions found: " + stack.getExtensions().size(), stack.getExtensions().size() == 1);
    extension = stack.getExtensions().iterator().next();
    assertEquals("Extension name: " + extension.getName(), extension.getName(), "EXT");
    assertEquals("Extension version: " + extension.getVersion(), extension.getVersion(), "0.1");

    ExtensionInfo extensionInfo2 = stackManager.getExtension("EXT", "0.2");
    helper.updateExtensionLink(stackManager, link1, stack, extension, extensionInfo2);
    assertEquals(link1.getExtension().getExtensionVersion(), link1.getExtension().getExtensionVersion(), "0.2");

    stack = stackManager.getStack("HDP", "0.2");
    assertTrue("Extensions found: " + stack.getExtensions().size(), stack.getExtensions().size() == 0);

    stack = stackManager.getStack("HDP", "0.3");
    assertTrue("Extensions found: " + stack.getExtensions().size(), stack.getExtensions().size() == 1);
    extension = stack.getExtensions().iterator().next();
    assertNotNull(extension.getService("OOZIE2"));
    oozie = extension.getService("OOZIE2");
    assertEquals(oozie.getVersion(), "4.0.0");
    assertEquals("Extension name: " + extension.getName(), extension.getName(), "EXT");
    assertEquals("Extension version: " + extension.getVersion(), extension.getVersion(), "0.4");

    stack = stackManager.getStack("HDP", "0.4");
    assertTrue("Extensions found: " + stack.getExtensions().size(), stack.getExtensions().size() == 1);
    extension = stack.getExtensions().iterator().next();
    assertEquals("Extension name: " + extension.getName(), extension.getName(), "EXT");
    assertEquals("Extension version: " + extension.getVersion(), extension.getVersion(), "0.4");
  }
}
