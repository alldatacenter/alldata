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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.BlueprintDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.ClusterStateDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupConfigMappingDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostConfigMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostStateDAO;
import org.apache.ambari.server.orm.dao.KeyValueDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleBatchRequestDAO;
import org.apache.ambari.server.orm.dao.RequestScheduleDAO;
import org.apache.ambari.server.orm.dao.RoleSuccessCriteriaDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.EasyMock;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.PersistService;

@RunWith(Parameterized.class)
public class UpgradeTest {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeTest.class);

  private static String DDL_PATTERN = "ddl-scripts/Ambari-DDL-Derby-%s.sql";
  private static List<String> VERSIONS = Arrays.asList("1.4.4",
      "1.4.3", "1.4.2", "1.4.1", "1.4.0", "1.2.5", "1.2.4",
      "1.2.3"); //TODO add all
  private static String DROP_DERBY_URL = "jdbc:derby:memory:myDB/ambari;drop=true";

  private final String sourceVersion;
  private Properties properties = new Properties();

  private Injector injector;

  public UpgradeTest(String sourceVersion) {
    this.sourceVersion = sourceVersion;
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "remote");
    properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), Configuration.JDBC_IN_MEMORY_URL);
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), Configuration.JDBC_IN_MEMORY_DRIVER);
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), "src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), "src/test/resources/version");
    properties.setProperty(Configuration.OS_VERSION.getKey(), "centos5");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), "src/test/resources/");
  }

  @Test
  @Ignore
  public void testUpgrade() throws Exception {
    //not all tests close database properly, ensure it is empty
    try {
      dropDatabase();
    } catch (SQLException ignored) {
      // it is ok if database not found
    }

    String targetVersion = getLastVersion();

    injector = Guice.createInjector(new ControllerModule(properties));
    LOG.info("Testing upgrade from version {} to {}", sourceVersion, targetVersion);

    createSourceDatabase(sourceVersion);
    performUpgrade(targetVersion);
    testUpgradedSchema();

    dropDatabase();
  }

  private void dropDatabase() throws ClassNotFoundException, SQLException {
    Class.forName(Configuration.JDBC_IN_MEMORY_DRIVER);
    try {
      DriverManager.getConnection(DROP_DERBY_URL);
    } catch (SQLNonTransientConnectionException ignored) {
      LOG.info("Database dropped ", ignored); //error 08006 expected
    }
  }

  private void testUpgradedSchema() throws Exception {
    injector = Guice.createInjector(new ControllerModule(properties));
    injector.getInstance(PersistService.class).start();

    //TODO join() in AmbariServer.run() prevents proper start testing, figure out

    //check dao selects
    //TODO generify DAOs for basic methods? deal with caching config group daos in such case
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterDAO.findAll();
    BlueprintDAO blueprintDAO = injector.getInstance(BlueprintDAO.class);
    blueprintDAO.findAll();
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    clusterServiceDAO.findAll();
    injector.getInstance(ClusterStateDAO.class).findAll();
    injector.getInstance(ConfigGroupConfigMappingDAO.class).findAll();
    injector.getInstance(ConfigGroupDAO.class).findAll();
    injector.getInstance(ConfigGroupHostMappingDAO.class).findAll();
    injector.getInstance(ExecutionCommandDAO.class).findAll();
    injector.getInstance(HostComponentDesiredStateDAO.class).findAll();
    injector.getInstance(HostComponentStateDAO.class).findAll();
    injector.getInstance(HostConfigMappingDAO.class).findAll();
    injector.getInstance(HostDAO.class).findAll();
    injector.getInstance(HostRoleCommandDAO.class).findAll();
    injector.getInstance(HostStateDAO.class).findAll();
    injector.getInstance(KeyValueDAO.class).findAll();
    injector.getInstance(MetainfoDAO.class).findAll();
    RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
    requestDAO.findAll();
    requestDAO.findAllResourceFilters();
    injector.getInstance(RequestScheduleBatchRequestDAO.class).findAll();
    injector.getInstance(RequestScheduleDAO.class).findAll();
    injector.getInstance(RoleSuccessCriteriaDAO.class).findAll();
    injector.getInstance(ServiceComponentDesiredStateDAO.class).findAll();
    injector.getInstance(ServiceDesiredStateDAO.class).findAll();
    injector.getInstance(StageDAO.class).findAll();
    injector.getInstance(UserDAO.class).findAll();
    injector.getInstance(ViewDAO.class).findAll();
    injector.getInstance(ViewInstanceDAO.class).findAll();

    //TODO extend checks if needed
    injector.getInstance(PersistService.class).stop();
  }

  private void performUpgrade(String targetVersion) throws Exception {
    Injector injector = Guice.createInjector(new SchemaUpgradeHelper.UpgradeHelperModule(properties) {
      @Override
      protected void configure() {
        super.configure();
        ViewRegistry viewRegistryMock = EasyMock.createNiceMock(ViewRegistry.class);
        bind(ViewRegistry.class).toInstance(viewRegistryMock);
      }
    });
    SchemaUpgradeHelper schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);

    LOG.info("Upgrading schema to target version = " + targetVersion);

    UpgradeCatalog targetUpgradeCatalog = AbstractUpgradeCatalog
        .getUpgradeCatalog(targetVersion);

    LOG.debug("Target upgrade catalog. {}", targetUpgradeCatalog);

    // Read source version from DB
    String sourceVersion = schemaUpgradeHelper.readSourceVersion();
    LOG.info("Upgrading schema from source version = " + sourceVersion);

    List<UpgradeCatalog> upgradeCatalogs =
        schemaUpgradeHelper.getUpgradePath(sourceVersion, targetVersion);

    assertTrue("Final Upgrade Catalog should be run last",
      !upgradeCatalogs.isEmpty() && upgradeCatalogs.get(upgradeCatalogs.size() - 1).isFinal());

    try {
      schemaUpgradeHelper.executeUpgrade(upgradeCatalogs);
    } catch (Exception e) {
      // In UpgradeCatalog210, a lot of the classes had host_name removed, but the catalog makes raw SQL queries from Ambari 2.0.0
      // which still had that column, in order to populate the host_id. Therfore, ignore this exception type.
      if (e.getMessage().contains("Column 'T.HOST_NAME' is either not in any table in the FROM list") || e.getMessage().contains("Column 'T.HOSTNAME' is either not in any table in the FROM list")) {
        System.out.println("Ignoring on purpose, " + e.getMessage());
      } else {
        throw e;
      }
     }

    schemaUpgradeHelper.executePreDMLUpdates(upgradeCatalogs);

    schemaUpgradeHelper.executeDMLUpdates(upgradeCatalogs, "test");

    schemaUpgradeHelper.executeOnPostUpgrade(upgradeCatalogs);

    LOG.info("Upgrade successful.");
  }

  private String getLastVersion() throws Exception {
    Injector injector = Guice.createInjector(new SchemaUpgradeHelper.UpgradeHelperModule(properties));
    Set<UpgradeCatalog> upgradeCatalogs = injector.getInstance(Key.get(new TypeLiteral<Set<UpgradeCatalog>>() {
    }));
    String maxVersion = "1.2";
    for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
      String targetVersion = upgradeCatalog.getTargetVersion();
      if (VersionUtils.compareVersions(maxVersion, targetVersion) < 0) {
        maxVersion = targetVersion;
      }
    }
    return maxVersion;
  }

  private void createSourceDatabase(String version) throws IOException, SQLException {

    //create database
    String fileName = String.format(DDL_PATTERN, version);
    fileName = this.getClass().getClassLoader().getResource(fileName).getFile();
    DBAccessor dbAccessor = injector.getInstance(DBAccessor.class);
    dbAccessor.executeScript(fileName);
  }

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    Collection<Object[]> data = new ArrayList<>();
    for (String s : VERSIONS) {
      data.add(new Object[]{s});
    }
    return data;
  }
}
