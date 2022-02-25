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
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import junit.framework.Assert;

/**
 * ViewDataMigrationUtility Tests.
 */
public class ViewDataMigrationUtilityTest {

  private static String viewName = "MY_VIEW";
  private static String instanceName = "INSTANCE1";
  private static String version1 = "1.0.0";
  private static String version2 = "2.0.0";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ViewRegistry viewRegistry;

  @Before
  public void setUp() throws Exception {
    viewRegistry = createNiceMock(ViewRegistry.class);
    viewRegistry.copyPrivileges(anyObject(ViewInstanceEntity.class), anyObject(ViewInstanceEntity.class));
    expectLastCall().anyTimes();
    replay(viewRegistry);
    ViewRegistry.initInstance(viewRegistry);
  }

  @Test
  public void testMigrateDataSameVersions() throws Exception {
    TestViewDataMigrationUtility migrationUtility = new TestViewDataMigrationUtility(viewRegistry);

    ViewDataMigrationContextImpl context = getViewDataMigrationContext(42, 42);
    migrationUtility.setMigrationContext(context);

    ViewDataMigrator migrator =  migrationUtility.getViewDataMigrator(
        getInstanceDefinition(viewName, version2, instanceName), context);

    Assert.assertTrue(migrator instanceof ViewDataMigrationUtility.CopyAllDataMigrator);
  }

  @Test
  public void testMigrateDataDifferentVersions() throws Exception {
    TestViewDataMigrationUtility migrationUtility = new TestViewDataMigrationUtility(viewRegistry);

    ViewDataMigrationContextImpl context = getViewDataMigrationContext(2, 1);
    migrationUtility.setMigrationContext(context);

    ViewDataMigrator migrator = createStrictMock(ViewDataMigrator.class);
    expect(migrator.beforeMigration()).andReturn(true);
    migrator.migrateEntity(anyObject(Class.class), anyObject(Class.class)); expectLastCall();
    migrator.migrateInstanceData(); expectLastCall();
    migrator.afterMigration(); expectLastCall();

    replay(migrator);

    ViewInstanceEntity targetInstance = getInstanceDefinition(viewName, version2, instanceName, migrator);
    ViewInstanceEntity sourceInstance = getInstanceDefinition(viewName, version1, instanceName);
    migrationUtility.migrateData(targetInstance, sourceInstance, false);

    verify(migrator);
  }

  @Test
  public void testMigrateDataDifferentVersionsCancel() throws Exception {
    TestViewDataMigrationUtility migrationUtility = new TestViewDataMigrationUtility(viewRegistry);

    ViewDataMigrationContextImpl context = getViewDataMigrationContext(2, 1);
    migrationUtility.setMigrationContext(context);

    ViewDataMigrator migrator = createStrictMock(ViewDataMigrator.class);
    expect(migrator.beforeMigration()).andReturn(false);

    ViewInstanceEntity targetInstance = getInstanceDefinition(viewName, version2, instanceName, migrator);
    ViewInstanceEntity sourceInstance = getInstanceDefinition(viewName, version1, instanceName);

    thrown.expect(ViewDataMigrationException.class);
    migrationUtility.migrateData(targetInstance, sourceInstance, false);
  }

  private static ViewDataMigrationContextImpl getViewDataMigrationContext(int currentVersion, int originVersion) {
    Map<String, Class> entities = Collections.singletonMap("MyEntityClass", Object.class);
    ViewDataMigrationContextImpl context = createNiceMock(ViewDataMigrationContextImpl.class);
    expect(context.getOriginDataVersion()).andReturn(originVersion).anyTimes();
    expect(context.getCurrentDataVersion()).andReturn(currentVersion).anyTimes();
    expect(context.getOriginEntityClasses()).andReturn(entities).anyTimes();
    expect(context.getCurrentEntityClasses()).andReturn(entities).anyTimes();

    expect(context.getCurrentInstanceDataByUser()).andReturn(new HashMap<>());
    replay(context);
    return context;
  }

  private static class TestViewDataMigrationUtility extends ViewDataMigrationUtility {
    private ViewDataMigrationContextImpl migrationContext;

    public TestViewDataMigrationUtility(ViewRegistry viewRegistry) {
      super(viewRegistry);
    }

    @Override
    protected ViewDataMigrationContextImpl getViewDataMigrationContext(ViewInstanceEntity targetInstanceDefinition,
                                                                       ViewInstanceEntity sourceInstanceDefinition) {
      if (migrationContext == null) {
        return super.getViewDataMigrationContext(targetInstanceDefinition, sourceInstanceDefinition);
      }
      return migrationContext;
    }

    public ViewDataMigrationContextImpl getMigrationContext() {
      return migrationContext;
    }

    public void setMigrationContext(ViewDataMigrationContextImpl migrationContext) {
      this.migrationContext = migrationContext;
    }
  }

  private ViewInstanceEntity getInstanceDefinition(String viewName, String viewVersion, String instanceName) {
    ViewDataMigrator migrator = createNiceMock(ViewDataMigrator.class);
    replay(migrator);
    return getInstanceDefinition(viewName, viewVersion, instanceName, migrator);
  }

  private ViewInstanceEntity getInstanceDefinition(String viewName, String viewVersion, String instanceName,
                                                   ViewDataMigrator migrator) {
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    expect(viewEntity.getViewName()).andReturn(viewName);
    expect(viewEntity.getVersion()).andReturn(viewVersion);

    replay(viewEntity);

    ViewInstanceEntity instanceEntity = createNiceMock(ViewInstanceEntity.class);
    expect(instanceEntity.getViewEntity()).andReturn(viewEntity);
    expect(instanceEntity.getViewName()).andReturn(viewName);
    expect(instanceEntity.getInstanceName()).andReturn(instanceName);

    try {
      expect(instanceEntity.getDataMigrator(anyObject(ViewDataMigrationContext.class))).
          andReturn(migrator);
    } catch (Exception e) {
      e.printStackTrace();
    }

    replay(instanceEntity);
    return instanceEntity;
  }

}
