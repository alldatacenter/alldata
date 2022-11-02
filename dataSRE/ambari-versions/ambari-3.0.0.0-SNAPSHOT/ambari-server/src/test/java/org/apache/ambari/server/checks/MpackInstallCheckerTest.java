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
package org.apache.ambari.server.checks;

import static org.easymock.EasyMock.expect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;

import org.apache.ambari.server.orm.DBAccessor;
import org.easymock.EasyMockSupport;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

/**
 * Unit tests for {@link MpackInstallChecker}
 */
public class MpackInstallCheckerTest {

  @Test
  public void testCheckValidClusters() throws Exception {

    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final ResultSet stackResultSet = easyMockSupport.createNiceMock(ResultSet.class);

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final PersistService mockPersistService = easyMockSupport.createNiceMock(PersistService.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(PersistService.class).toInstance(mockPersistService);
      }
    });

    MpackInstallChecker mpackInstallChecker = mockInjector.getInstance(MpackInstallChecker.class);

    HashSet<String> stacksInMpack = new HashSet<>();
    stacksInMpack.add("HDF");

    expect(mpackInstallChecker.getConnection()).andReturn(mockConnection);
    expect(mockConnection.createStatement(
        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
        "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDF");
    expect(stackResultSet.getString("stack_version")).andReturn("2.0");
    expect(stackResultSet.getString("cluster_name")).andReturn("cl1");

    easyMockSupport.replayAll();
    mpackInstallChecker.checkClusters(stacksInMpack);
    easyMockSupport.verifyAll();

    Assert.assertFalse("No errors should have been triggered.", mpackInstallChecker.isErrorsFound());
  }

  @Test
  public void testCheckInvalidClusters() throws Exception {

    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final ResultSet stackResultSet = easyMockSupport.createNiceMock(ResultSet.class);

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final PersistService mockPersistService = easyMockSupport.createNiceMock(PersistService.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(PersistService.class).toInstance(mockPersistService);
      }
    });
    MpackInstallChecker mpackInstallChecker = mockInjector.getInstance(MpackInstallChecker.class);

    HashSet<String> stacksInMpack = new HashSet<>();
    stacksInMpack.add("HDF");

    expect(mpackInstallChecker.getConnection()).andReturn(mockConnection);
    expect(mockConnection.createStatement(
        ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
        "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDP");
    expect(stackResultSet.getString("stack_version")).andReturn("2.5");
    expect(stackResultSet.getString("cluster_name")).andReturn("cl1");

    easyMockSupport.replayAll();
    mpackInstallChecker.checkClusters(stacksInMpack);
    easyMockSupport.verifyAll();

    Assert.assertTrue("Installing HDF mpack on HDP cluster with purge option should have triggered an error.",
        mpackInstallChecker.isErrorsFound());
  }

}
