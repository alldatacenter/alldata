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
package org.apache.ambari.server.api.services.views;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import javax.ws.rs.WebApplicationException;

import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.ViewDataMigrationUtility;
import org.apache.ambari.server.view.ViewRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * ViewDataMigrationService tests.
 */
public class ViewDataMigrationServiceTest {

  private static String viewName = "MY_VIEW";
  private static String instanceName = "INSTANCE1";
  private static String version1 = "1.0.0";
  private static String version2 = "2.0.0";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testServiceMigrateCallAdmin() throws Exception {
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    expect(viewRegistry.checkAdmin()).andReturn(true).anyTimes();
    replay(viewRegistry);
    ViewRegistry.initInstance(viewRegistry);

    ViewDataMigrationService service = new ViewDataMigrationService();

    ViewDataMigrationUtility migrationUtility = createStrictMock(ViewDataMigrationUtility.class);
    migrationUtility.migrateData(anyObject(ViewInstanceEntity.class), anyObject(ViewInstanceEntity.class), eq(false));
    replay(migrationUtility);
    service.setViewDataMigrationUtility(migrationUtility);

    service.migrateData(viewName,version1,instanceName,version2, instanceName);

    verify(migrationUtility);
  }

  @Test
  public void testServiceMigrateCallNotAdmin() throws Exception {
    ViewRegistry viewRegistry = createNiceMock(ViewRegistry.class);
    expect(viewRegistry.checkAdmin()).andReturn(false).anyTimes();
    replay(viewRegistry);
    ViewRegistry.initInstance(viewRegistry);

    ViewDataMigrationService service = new ViewDataMigrationService();

    thrown.expect(WebApplicationException.class);
    service.migrateData(viewName,version1,instanceName,version2, instanceName);
  }
}
