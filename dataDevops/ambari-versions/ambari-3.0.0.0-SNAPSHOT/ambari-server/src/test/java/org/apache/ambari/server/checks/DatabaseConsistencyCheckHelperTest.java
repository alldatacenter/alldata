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


import static com.google.common.collect.Lists.newArrayList;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.commons.collections.MapUtils;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class DatabaseConsistencyCheckHelperTest {

  private Injector injector;

  @After
  public void teardown() throws AmbariException, SQLException {
    if (injector != null) {
      H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    }
  }

  @Test
  public void testCheckForConfigsSelectedMoreThanOnce() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, cc.type_name from clusterconfig cc "
        + "join clusters c on cc.cluster_id=c.cluster_id "
        + "group by c.cluster_name, cc.type_name " +
            "having sum(selected) > 1")).andReturn(mockResultSet);



    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);


    easyMockSupport.replayAll();

    DatabaseConsistencyCheckHelper.checkForConfigsSelectedMoreThanOnce();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckForHostsWithoutState() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });



    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select host_name from hosts where host_id not in (select host_id from hoststate)")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();



    DatabaseConsistencyCheckHelper.checkForHostsWithoutState();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckHostComponentStatesCountEqualsHostComponentsDesiredStates() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {

        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });



    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select count(*) from hostcomponentstate")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select count(*) from hostcomponentdesiredstate")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select count(*) FROM hostcomponentstate hcs " +
            "JOIN hostcomponentdesiredstate hcds ON hcs.service_name=hcds.service_name AND " +
            "hcs.component_name=hcds.component_name AND hcs.host_id=hcds.host_id")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select component_name, host_id from hostcomponentstate group by component_name, host_id having count(component_name) > 1")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();


    DatabaseConsistencyCheckHelper.checkHostComponentStates();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckServiceConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariMetaInfo mockAmbariMetainfo = easyMockSupport.createNiceMock(AmbariMetaInfo.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet stackResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet serviceConfigResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final ServiceInfo mockHDFSServiceInfo = easyMockSupport.createNiceMock(ServiceInfo.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = createInjectorWithAmbariMetaInfo(mockAmbariMetainfo, mockDBDbAccessor);

    Map<String, ServiceInfo> services = new HashMap<>();
    services.put("HDFS", mockHDFSServiceInfo);

    Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();
    configAttributes.put("core-site", new HashMap<>());

    expect(mockHDFSServiceInfo.getConfigTypeAttributes()).andReturn(configAttributes);
    expect(mockAmbariMetainfo.getServices("HDP", "2.2")).andReturn(services);
    expect(serviceConfigResultSet.next()).andReturn(true).times(2);
    expect(serviceConfigResultSet.getString("service_name")).andReturn("HDFS").andReturn("HBASE");
    expect(serviceConfigResultSet.getString("type_name")).andReturn("core-site").andReturn("hbase-env");
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDP");
    expect(stackResultSet.getString("stack_version")).andReturn("2.2");
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, service_name from clusterservices cs " +
            "join clusters c on cs.cluster_id=c.cluster_id " +
            "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
            "join clusters c on sc.cluster_id=c.cluster_id " +
            "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
            "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
            "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
            "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name, sc.version")).andReturn(serviceConfigResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
            "join clusters c on cc.cluster_id=c.cluster_id " +
            "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name " +
            "having sum(cc.selected) < 1")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();

    mockAmbariMetainfo.init();

    DatabaseConsistencyCheckHelper.checkServiceConfigs();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testSchemaName_NoIssues() throws Exception {
    setupMocksForTestSchemaName("ambari", "ambari, public", newArrayList("ambari", "public"), newArrayList("ambari"));
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertFalse("No warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }

  @Test
  public void testSchemaName_WrongSearchPathOrder() throws Exception {
    setupMocksForTestSchemaName("ambari", "public, ambari", newArrayList("ambari", "public"), newArrayList("ambari"));
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertTrue("Warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }

  @Test
  public void testSchemaName_NoSearchPath() throws Exception {
    setupMocksForTestSchemaName("ambari", null, newArrayList("ambari", "public"), newArrayList("ambari"));
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertTrue("Warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }


  @Test
  public void testSchemaName_NoAmbariSchema() throws Exception {
    setupMocksForTestSchemaName("ambari", null, newArrayList("public"), Lists.newArrayList());
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertTrue("Warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }

  @Test
  public void testSchemaName_NoTablesInAmbariSchema() throws Exception {
    setupMocksForTestSchemaName("ambari", "ambari", newArrayList("ambari", "public"), newArrayList("public"));
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertTrue("Warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }

  @Test
  public void testSchemaName_AmbariTablesInMultipleSchemas() throws Exception {
    setupMocksForTestSchemaName("ambari", "ambari", newArrayList("ambari", "public"), newArrayList("ambari", "public"));
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertTrue("Warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }

  @Test
  public void testSchemaName_NullsAreTolerated() throws Exception {
    setupMocksForTestSchemaName(null, null, null, null);
    DatabaseConsistencyCheckHelper.checkSchemaName();
    assertTrue("Warnings were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult() ==
        DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    assertFalse("No errors were expected.", DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }

  private void setupMocksForTestSchemaName(String configuredSchema, String searchPath, List<String> schemas,
                                           List<String> schemasWithAmbariTables) throws Exception {
    final Configuration config = createNiceMock(Configuration.class);
    final OsFamily osFamily = createNiceMock(OsFamily.class);
    final Connection connection = createNiceMock(Connection.class);
    final DBAccessor dbAccessor = createStrictMock(DBAccessor.class);
    final Statement searchPathStatement = createStrictMock(Statement.class);
    final Statement getTablesStatement = createStrictMock(Statement.class);
    final DatabaseMetaData dbMetaData = createStrictMock(DatabaseMetaData.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DBAccessor.class).toInstance(dbAccessor);
        bind(OsFamily.class).toInstance(osFamily);
        bind(Configuration.class).toInstance(config);
      }
    });
    expect(config.getDatabaseSchema()).andReturn(configuredSchema).anyTimes();
    expect(config.getDatabaseType()).andReturn(Configuration.DatabaseType.POSTGRES);
    expect(dbAccessor.getConnection()).andReturn(connection);
    expect(connection.getMetaData()).andReturn(dbMetaData);
    expect(connection.createStatement()).andReturn(searchPathStatement);
    expect(connection.createStatement()).andReturn(getTablesStatement);
    expect(dbMetaData.getSchemas()).andReturn(resultSet("TABLE_SCHEM", schemas));
    expect(searchPathStatement.executeQuery(anyString())).andReturn(
        resultSet("search_path", newArrayList(searchPath)));
    expect(getTablesStatement.executeQuery(anyString())).andReturn(
        resultSet("table_schema", schemasWithAmbariTables));
    replay(config, connection, dbAccessor, dbMetaData, getTablesStatement, osFamily, searchPathStatement);
    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(null);
    DatabaseConsistencyCheckHelper.resetCheckResult();
  }

  private ResultSet resultSet(final String columnName, final List<? extends Object> columnData) throws SQLException {
    if (null == columnData) {
      return null;
    }
    else {
      ResultSet rs = createNiceMock(ResultSet.class);
      if ( !columnData.isEmpty() ) {
        expect(rs.next()).andReturn(true).times(columnData.size());
      }
      expect(rs.next()).andReturn(false);
      for(Object item: columnData) {
        expect(rs.getObject(columnName)).andReturn(item);
      }
      replay(rs);
      return rs;
    }
  }


  @Test
  public void testCheckServiceConfigs_missingServiceConfigGeneratesWarning() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariMetaInfo mockAmbariMetainfo = easyMockSupport.createNiceMock(AmbariMetaInfo.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet clusterServicesResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet stackResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet serviceConfigResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final ServiceInfo mockHDFSServiceInfo = easyMockSupport.createNiceMock(ServiceInfo.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = createInjectorWithAmbariMetaInfo(mockAmbariMetainfo, mockDBDbAccessor);

    Map<String, ServiceInfo> services = new HashMap<>();
    services.put("HDFS", mockHDFSServiceInfo);

    Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();
    configAttributes.put("core-site", new HashMap<>());

    expect(mockHDFSServiceInfo.getConfigTypeAttributes()).andReturn(configAttributes);
    expect(mockAmbariMetainfo.getServices("HDP", "2.2")).andReturn(services);
    expect(clusterServicesResultSet.next()).andReturn(true);
    expect(clusterServicesResultSet.getString("service_name")).andReturn("OPENSOFT R");
    expect(clusterServicesResultSet.getString("cluster_name")).andReturn("My Cluster");
    expect(serviceConfigResultSet.next()).andReturn(true);
    expect(serviceConfigResultSet.getString("service_name")).andReturn("HDFS");
    expect(serviceConfigResultSet.getString("type_name")).andReturn("core-site");
    expect(stackResultSet.next()).andReturn(true);
    expect(stackResultSet.getString("stack_name")).andReturn("HDP");
    expect(stackResultSet.getString("stack_version")).andReturn("2.2");
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, service_name from clusterservices cs " +
        "join clusters c on cs.cluster_id=c.cluster_id " +
        "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)")).andReturn(clusterServicesResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
        "join clusters c on sc.cluster_id=c.cluster_id " +
        "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null")).andReturn(mockResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
        "join stack s on c.desired_stack_id = s.stack_id")).andReturn(stackResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
        "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
        "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
        "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
        "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
        "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
        "group by c.cluster_name, cs.service_name, cc.type_name, sc.version")).andReturn(serviceConfigResultSet);
    expect(mockStatement.executeQuery("select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
        "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
        "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
        "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
        "join clusters c on cc.cluster_id=c.cluster_id " +
        "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
        "group by c.cluster_name, cs.service_name, cc.type_name " +
        "having sum(cc.selected) < 1")).andReturn(mockResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();

    mockAmbariMetainfo.init();

    DatabaseConsistencyCheckHelper.resetCheckResult();
    DatabaseConsistencyCheckHelper.checkServiceConfigs();

    easyMockSupport.verifyAll();

    Assert.assertTrue("Missing service config for OPENSOFT R should have triggered a warning.",
        DatabaseConsistencyCheckHelper.getLastCheckResult() == DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
    Assert.assertFalse("No errors should have been triggered.",
        DatabaseConsistencyCheckHelper.getLastCheckResult().isError());
  }


  @Test
  public void testCheckForLargeTables() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariMetaInfo mockAmbariMetainfo = easyMockSupport.createNiceMock(AmbariMetaInfo.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);

    final ResultSet hostRoleCommandResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet executionCommandResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet stageResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet requestResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final ResultSet alertHistoryResultSet = easyMockSupport.createNiceMock(ResultSet.class);

    final Injector mockInjector = createInjectorWithAmbariMetaInfo(mockAmbariMetainfo, mockDBDbAccessor);

    expect(hostRoleCommandResultSet.next()).andReturn(true).once();
    expect(executionCommandResultSet.next()).andReturn(true).once();
    expect(stageResultSet.next()).andReturn(true).once();
    expect(requestResultSet.next()).andReturn(true).once();
    expect(alertHistoryResultSet.next()).andReturn(true).once();
    expect(hostRoleCommandResultSet.getLong(1)).andReturn(2345L).atLeastOnce();
    expect(executionCommandResultSet.getLong(1)).andReturn(12345L).atLeastOnce();
    expect(stageResultSet.getLong(1)).andReturn(2321L).atLeastOnce();
    expect(requestResultSet.getLong(1)).andReturn(1111L).atLeastOnce();
    expect(alertHistoryResultSet.getLong(1)).andReturn(2223L).atLeastOnce();
    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockDBDbAccessor.getDbType()).andReturn(DBAccessor.DbType.MYSQL);
    expect(mockDBDbAccessor.getDbSchema()).andReturn("test_schema");
    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement).anyTimes();
    expect(mockStatement.executeQuery("SELECT (data_length + index_length) \"Table Size\" " +
            "FROM information_schema.TABLES WHERE table_schema = \"test_schema\" AND table_name =\"host_role_command\"")).andReturn(hostRoleCommandResultSet);
    expect(mockStatement.executeQuery("SELECT (data_length + index_length) \"Table Size\" " +
            "FROM information_schema.TABLES WHERE table_schema = \"test_schema\" AND table_name =\"execution_command\"")).andReturn(executionCommandResultSet);
    expect(mockStatement.executeQuery("SELECT (data_length + index_length) \"Table Size\" " +
            "FROM information_schema.TABLES WHERE table_schema = \"test_schema\" AND table_name =\"stage\"")).andReturn(stageResultSet);
    expect(mockStatement.executeQuery("SELECT (data_length + index_length) \"Table Size\" " +
            "FROM information_schema.TABLES WHERE table_schema = \"test_schema\" AND table_name =\"request\"")).andReturn(requestResultSet);
    expect(mockStatement.executeQuery("SELECT (data_length + index_length) \"Table Size\" " +
            "FROM information_schema.TABLES WHERE table_schema = \"test_schema\" AND table_name =\"alert_history\"")).andReturn(alertHistoryResultSet);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);

    easyMockSupport.replayAll();

    mockAmbariMetainfo.init();

    DatabaseConsistencyCheckHelper.resetCheckResult();
    DatabaseConsistencyCheckHelper.checkForLargeTables();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testConfigGroupHostMappings() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    Map<String, Cluster> clusters = new HashMap<>();
    Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    clusters.put("c1", cluster);
    expect(mockClusters.getClusters()).andReturn(clusters).anyTimes();

    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    ConfigGroup cg1 = easyMockSupport.createNiceMock(ConfigGroup.class);
    ConfigGroup cg2 = easyMockSupport.createNiceMock(ConfigGroup.class);
    configGroupMap.put(1L, cg1);
    configGroupMap.put(2L, cg2);

    expect(cluster.getConfigGroups()).andReturn(configGroupMap).anyTimes();

    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    Map<String, Host> hosts = new HashMap<>();
    Host h1 = easyMockSupport.createNiceMock(Host.class);
    Host h2 = easyMockSupport.createNiceMock(Host.class);
    hosts.put("h1", h1);
    expect(mockClusters.getHostsForCluster("c1")).andReturn(hosts);

    Map<Long, Host> cgHosts = new HashMap<>();
    cgHosts.put(1L, h1);
    cgHosts.put(2L, h2);

    expect(cg1.getHosts()).andReturn(cgHosts);

    expect(h1.getHostName()).andReturn("h1").anyTimes();
    expect(h2.getHostName()).andReturn("h2").anyTimes() ;
    expect(h1.getHostId()).andReturn(1L).anyTimes();
    expect(h2.getHostId()).andReturn(2L).anyTimes();

    expect(cg1.getId()).andReturn(1L).anyTimes();
    expect(cg2.getId()).andReturn(2L).anyTimes();
    expect(cg1.getName()).andReturn("cg1").anyTimes();
    expect(cg2.getName()).andReturn("cg2").anyTimes();

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);

    easyMockSupport.replayAll();

    Map<Long, Set<Long>> hostIds = DatabaseConsistencyCheckHelper.checkConfigGroupHostMapping(true);

    easyMockSupport.verifyAll();

    Assert.assertNotNull(hostIds);
    Assert.assertEquals(1, hostIds.size());
    Assert.assertEquals(1L, hostIds.keySet().iterator().next().longValue());
    Assert.assertEquals(2L, hostIds.get(1L).iterator().next().longValue());
  }

  @Test
  public void testConfigGroupForDeletedServices() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    Map<String, Cluster> clusters = new HashMap<>();
    Cluster cluster = easyMockSupport.createStrictMock(Cluster.class);
    clusters.put("c1", cluster);
    expect(mockClusters.getClusters()).andReturn(clusters).anyTimes();

    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    ConfigGroup cg1 = easyMockSupport.createNiceMock(ConfigGroup.class);
    ConfigGroup cg2 = easyMockSupport.createNiceMock(ConfigGroup.class);
    ConfigGroup cgWithoutServiceName = easyMockSupport.createNiceMock(ConfigGroup.class);
    configGroupMap.put(1L, cg1);
    configGroupMap.put(2L, cg2);
    configGroupMap.put(3L, cgWithoutServiceName);

    expect(cluster.getConfigGroups()).andStubReturn(configGroupMap);
    expect(cg1.getName()).andReturn("cg1").anyTimes();
    expect(cg1.getId()).andReturn(1L).anyTimes();
    expect(cg1.getServiceName()).andReturn("YARN").anyTimes();
    expect(cg2.getServiceName()).andReturn("HDFS").anyTimes();
    expect(cgWithoutServiceName.getName()).andReturn("cg3").anyTimes();
    expect(cgWithoutServiceName.getId()).andReturn(3L).anyTimes();
    expect(cgWithoutServiceName.getServiceName()).andReturn(null).anyTimes();

    Service service = easyMockSupport.createNiceMock(Service.class);
    Map<String, Service> services = new HashMap<>();
    services.put("HDFS", service);
    expect(cluster.getServices()).andReturn(services).anyTimes();

    expect(cg1.getClusterName()).andReturn("c1");
    expect(mockClusters.getCluster("c1")).andReturn(cluster).anyTimes();
    cluster.deleteConfigGroup(1L);
    expectLastCall();

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);

    easyMockSupport.replayAll();

    Map<Long, ConfigGroup> configGroups = DatabaseConsistencyCheckHelper.checkConfigGroupsForDeletedServices(true);
    DatabaseConsistencyCheckHelper.fixConfigGroupsForDeletedServices();

    easyMockSupport.verifyAll();

    Assert.assertFalse(MapUtils.isEmpty(configGroups));
    Assert.assertEquals(2, configGroups.size());
    Assert.assertTrue(configGroups.containsKey(1L));
    Assert.assertFalse(configGroups.containsKey(2L));
    Assert.assertTrue(configGroups.containsKey(3L));
  }

  @Test
  public void testCheckForStalealertdefs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AlertDefinition alertDefinition = easyMockSupport.createNiceMock(AlertDefinition.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);
    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AlertDefinition.class).toInstance(alertDefinition);
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });
    final ResultSet staleAlertResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    expect(staleAlertResultSet.next()).andReturn(true).once();
    expect(staleAlertResultSet.getString("definition_name")).andReturn("ALERT-NAME").atLeastOnce();
    expect(staleAlertResultSet.getString("service_name")).andReturn("SERVICE-DELETED").atLeastOnce();
    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    expect(mockDBDbAccessor.getConnection()).andReturn(mockConnection);
    expect(mockDBDbAccessor.getDbType()).andReturn(DBAccessor.DbType.MYSQL);
    expect(mockDBDbAccessor.getDbSchema()).andReturn("test_schema");

    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement).anyTimes();
    expect(mockStatement.executeQuery("select definition_name, service_name from alert_definition where service_name not in " +
            "(select service_name from clusterservices) and service_name not in ('AMBARI')")).andReturn(staleAlertResultSet);

    expect(alertDefinition.getDefinitionId()).andReturn(1L);
    expect(alertDefinition.getName()).andReturn("AlertTest");

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);

    easyMockSupport.replayAll();

    Map<String, String> stalealertdefs1 = DatabaseConsistencyCheckHelper.checkForStalealertdefs();

    Assert.assertEquals(1, stalealertdefs1.size());
  }

  @Test
  public void testCollectConfigGroupsWithoutServiceName() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    Map<String, Cluster> clusters = new HashMap<>();
    Cluster cluster1 = easyMockSupport.createNiceMock(Cluster.class);
    clusters.put("c1", cluster1);
    Cluster cluster2 = easyMockSupport.createNiceMock(Cluster.class);
    clusters.put("c2", cluster2);
    expect(cluster2.getConfigGroups()).andReturn(new HashMap<Long, ConfigGroup>(0)).anyTimes();
    expect(mockClusters.getClusters()).andReturn(clusters).anyTimes();
    expect(mockClusters.getCluster("c1")).andReturn(cluster1).anyTimes();
    expect(mockClusters.getCluster("c2")).andReturn(cluster2).anyTimes();

    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    ConfigGroup cgWithoutServiceName = easyMockSupport.createNiceMock(ConfigGroup.class);
    ConfigGroup cgWithServiceName = easyMockSupport.createNiceMock(ConfigGroup.class);
    ConfigGroup cgForNonExistentService = easyMockSupport.createNiceMock(ConfigGroup.class);
    configGroupMap.put(1L, cgWithoutServiceName);
    configGroupMap.put(2L, cgWithServiceName);
    configGroupMap.put(3L, cgForNonExistentService);

    expect(cluster1.getConfigGroups()).andReturn(configGroupMap).anyTimes();
    expect(cgWithoutServiceName.getId()).andReturn(1L).anyTimes();
    expect(cgWithoutServiceName.getClusterName()).andReturn("c1").anyTimes();
    expect(cgWithoutServiceName.getServiceName()).andReturn(null).anyTimes();
    expect(cgWithoutServiceName.getTag()).andReturn("YARN").anyTimes();
    cgWithoutServiceName.setServiceName("YARN"); expectLastCall();
    expect(cgWithServiceName.getId()).andReturn(2L).anyTimes();
    expect(cgWithServiceName.getClusterName()).andReturn("c1").anyTimes();
    expect(cgWithServiceName.getServiceName()).andReturn("HDFS").anyTimes();
    expect(cgForNonExistentService.getId()).andReturn(3L).anyTimes();
    expect(cgForNonExistentService.getClusterName()).andReturn("c1").anyTimes();
    expect(cgForNonExistentService.getServiceName()).andReturn(null).anyTimes();
    expect(cgForNonExistentService.getTag()).andReturn("NOT_EXISTS").anyTimes();

    Service hdfsService = easyMockSupport.createNiceMock(Service.class);
    Service yarnService = easyMockSupport.createNiceMock(Service.class);
    Map<String, Service> services = new HashMap<>();
    services.put("HDFS", hdfsService);
    services.put("YARN", yarnService);
    expect(cluster1.getServices()).andReturn(services).anyTimes();

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);

    easyMockSupport.replayAll();

    Map<Long, ConfigGroup> configGroups = DatabaseConsistencyCheckHelper.collectConfigGroupsWithoutServiceName();
    DatabaseConsistencyCheckHelper.fixConfigGroupServiceNames();

    easyMockSupport.verifyAll();

    Assert.assertFalse(MapUtils.isEmpty(configGroups));
    Assert.assertEquals(2, configGroups.size());
    Assert.assertTrue(configGroups.containsKey(1L));
    Assert.assertFalse(configGroups.containsKey(2L));
    Assert.assertTrue(configGroups.containsKey(3L));
  }

  @Test
  public void testCollectConfigGroupsWithoutServiceNameReturnsEmptyMapWhenNoClusters() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });

    Map<String, Cluster> clusters = new HashMap<>();
    expect(mockClusters.getClusters()).andReturn(clusters).anyTimes();

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);

    easyMockSupport.replayAll();

    Map<Long, ConfigGroup> configGroups = DatabaseConsistencyCheckHelper.collectConfigGroupsWithoutServiceName();

    easyMockSupport.verifyAll();

    Assert.assertTrue(MapUtils.isEmpty(configGroups));
  }

  @Test
  public void testFixConfigsSelectedMoreThanOnce() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final Connection mockConnection = easyMockSupport.createNiceMock(Connection.class);
    final ClusterDAO clusterDAO = easyMockSupport.createNiceMock(ClusterDAO.class);
    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final ResultSet mockResultSet = easyMockSupport.createNiceMock(ResultSet.class);
    final Statement mockStatement = easyMockSupport.createNiceMock(Statement.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);

    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(Clusters.class).toInstance(mockClusters);
        bind(ClusterDAO.class).toInstance(clusterDAO);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(OsFamily.class).toInstance(mockOSFamily);
      }
    });


    expect(mockConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)).andReturn(mockStatement);
    expect(mockStatement.executeQuery("select c.cluster_name, cc.type_name from clusterconfig cc " +
        "join clusters c on cc.cluster_id=c.cluster_id " +
        "group by c.cluster_name, cc.type_name " +
        "having sum(cc.selected) > 1")).andReturn(mockResultSet);
    expect(mockResultSet.next()).andReturn(true).once();
    expect(mockResultSet.getString("cluster_name")).andReturn("123").once();
    expect(mockResultSet.getString("type_name")).andReturn("type1").once();
    expect(mockResultSet.next()).andReturn(false).once();

    Cluster clusterMock = easyMockSupport.createNiceMock(Cluster.class);
    expect(mockClusters.getCluster("123")).andReturn(clusterMock);

    expect(clusterMock.getClusterId()).andReturn(123L).once();

    ClusterConfigEntity clusterConfigEntity1 = easyMockSupport.createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity clusterConfigEntity2 = easyMockSupport.createNiceMock(ClusterConfigEntity.class);
    expect(clusterConfigEntity1.getType()).andReturn("type1").anyTimes();
    expect(clusterConfigEntity1.getSelectedTimestamp()).andReturn(123L);
    clusterConfigEntity1.setSelected(false);
    expectLastCall().once();

    expect(clusterConfigEntity2.getType()).andReturn("type1").anyTimes();
    expect(clusterConfigEntity2.getSelectedTimestamp()).andReturn(321L);
    clusterConfigEntity2.setSelected(false);
    expectLastCall().once();
    clusterConfigEntity2.setSelected(true);
    expectLastCall().once();

    TypedQuery queryMock = easyMockSupport.createNiceMock(TypedQuery.class);
    expect(mockEntityManager.createNamedQuery(anyString(), anyObject(Class.class))).andReturn(queryMock).anyTimes();
    expect(queryMock.setParameter(anyString(), anyString())).andReturn(queryMock).once();
    expect(queryMock.setParameter(anyString(), anyLong())).andReturn(queryMock).once();
    expect(queryMock.getResultList()).andReturn(Arrays.asList(clusterConfigEntity1, clusterConfigEntity2)).once();
    expect(clusterDAO.merge(anyObject(ClusterConfigEntity.class), anyBoolean())).andReturn(null).times(3);

    DatabaseConsistencyCheckHelper.setInjector(mockInjector);
    DatabaseConsistencyCheckHelper.setConnection(mockConnection);

    easyMockSupport.replayAll();

    DatabaseConsistencyCheckHelper.fixConfigsSelectedMoreThanOnce();

    easyMockSupport.verifyAll();
  }

  @Test
  public void testCheckForConfigsNotMappedToService() throws SQLException, AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);
    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    DatabaseConsistencyCheckHelper.setInjector(injector);

    String STACK_VERSION = "0.1";
    String REPO_VERSION = "0.1-1234";
    StackId STACK_ID = new StackId("HDP", STACK_VERSION);
    OrmTestHelper ormTestHelper = injector.getInstance(OrmTestHelper.class);
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(STACK_ID, REPO_VERSION);

    String clusterName = "foo";
    clusters.addCluster(clusterName, STACK_ID);
    Cluster cluster = clusters.getCluster(clusterName);

    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    // add some cluster configs
    configFactory.createNew(cluster, "hdfs-site", "version1",
        new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<>());

    configFactory.createNew(cluster, "hdfs-site", "version2",
        new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<>());

    DatabaseConsistencyCheckHelper.checkForConfigsNotMappedToService();
    assertEquals(DatabaseConsistencyCheckResult.DB_CHECK_WARNING, DatabaseConsistencyCheckHelper.getLastCheckResult());

    DatabaseConsistencyCheckHelper.fixClusterConfigsNotMappedToAnyService();
    DatabaseConsistencyCheckHelper.resetCheckResult();
    DatabaseConsistencyCheckHelper.checkForConfigsNotMappedToService();
    assertEquals(DatabaseConsistencyCheckResult.DB_CHECK_SUCCESS, DatabaseConsistencyCheckHelper.getLastCheckResult());
  }

  private Injector createInjectorWithAmbariMetaInfo(AmbariMetaInfo mockAmbariMetainfo,
                                                    DBAccessor mockDBDbAccessor) {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder().addAmbariMetaInfoBinding()
            .addDBAccessorBinding(mockDBDbAccessor).addLdapBindings().build()
            .configure(binder());

        bind(AmbariMetaInfo.class).toInstance(mockAmbariMetainfo);
      }
    });
  }
}
