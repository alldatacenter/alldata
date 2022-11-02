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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.audit.AuditLoggerModule;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.ldap.LdapModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.JdbcUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.persist.PersistService;

public class SchemaUpgradeHelper {
  private static final Logger LOG = LoggerFactory.getLogger
    (SchemaUpgradeHelper.class);

  private Set<UpgradeCatalog> allUpgradeCatalogs;
  private PersistService persistService;
  private DBAccessor dbAccessor;
  private Configuration configuration;
  private static final String[] rcaTableNames = {"workflow", "job", "task", "taskAttempt", "hdfsEvent", "mapreduceEvent", "clusterEvent"};
  static final Gson gson = new GsonBuilder().create();

  @Inject
  public SchemaUpgradeHelper(Set<UpgradeCatalog> allUpgradeCatalogs,
                             PersistService persistService,
                             DBAccessor dbAccessor,
                             Configuration configuration) {
    this.allUpgradeCatalogs = allUpgradeCatalogs;
    this.persistService = persistService;
    this.dbAccessor = dbAccessor;
    this.configuration = configuration;
  }

  public void startPersistenceService() {
    persistService.start();
  }

  public void stopPersistenceService() {
    persistService.stop();
  }

  public Set<UpgradeCatalog> getAllUpgradeCatalogs() {
    return allUpgradeCatalogs;
  }

  public String readSourceVersion() {

    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT " + dbAccessor.quoteObjectName("metainfo_value") +
          " from metainfo WHERE " + dbAccessor.quoteObjectName("metainfo_key") + "='version'");
        if (rs != null && rs.next()) {
          return rs.getString(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Unable to read database version", e);

    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          throw new RuntimeException("Cannot close result set");
        }
      }
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          throw new RuntimeException("Cannot close statement");
        }
      }
    }
    //not found, assume oldest version
    //doesn't matter as there single upgrade catalog for 1.2.0 - 1.5.0 and 1.4.4 - 1.5.0 upgrades
    return "1.2.0";
  }

  /**
   * Read server version file
   * @return
   */
  protected String getAmbariServerVersion() {
    return configuration.getServerVersion();
  }

  /**
   * Return a set Upgrade catalogs to be applied to upgrade from
   * @sourceVersion to @targetVersion
   *
   * @param sourceVersion
   * @param targetVersion
   * @return
   * @throws org.apache.ambari.server.AmbariException
   */
  protected List<UpgradeCatalog> getUpgradePath(String sourceVersion,
                                                       String targetVersion) throws AmbariException {
    List<UpgradeCatalog> upgradeCatalogs = new ArrayList<>();
    List<UpgradeCatalog> candidateCatalogs = new ArrayList<>(allUpgradeCatalogs);

    Collections.sort(candidateCatalogs, new AbstractUpgradeCatalog.VersionComparator());

    for (UpgradeCatalog upgradeCatalog : candidateCatalogs) {
      if (sourceVersion == null || VersionUtils.compareVersions(sourceVersion,
        upgradeCatalog.getTargetVersion(), 4) < 0) {
        // catalog version is newer than source
        if (VersionUtils.compareVersions(upgradeCatalog.getTargetVersion(),
          targetVersion, 4) <= 0) {
          // catalog version is older or equal to target
          upgradeCatalogs.add(upgradeCatalog);
        }
      }
    }

    LOG.info("Upgrade path: " + upgradeCatalogs);

    return upgradeCatalogs;
  }

  /**
   * Extension of main controller module
   */
  public static class UpgradeHelperModule extends ControllerModule {

    public UpgradeHelperModule() throws Exception {
    }

    public UpgradeHelperModule(Properties properties) throws Exception {
      super(properties);
    }

    @Override
    protected void configure() {
      super.configure();
      // Add binding to each newly created catalog
      Multibinder<UpgradeCatalog> catalogBinder =
        Multibinder.newSetBinder(binder(), UpgradeCatalog.class);
      catalogBinder.addBinding().to(UpgradeCatalog251.class);
      catalogBinder.addBinding().to(UpgradeCatalog252.class);
      catalogBinder.addBinding().to(UpgradeCatalog260.class);
      catalogBinder.addBinding().to(UpgradeCatalog261.class);
      catalogBinder.addBinding().to(UpgradeCatalog262.class);
      catalogBinder.addBinding().to(UpgradeCatalog270.class);
      catalogBinder.addBinding().to(UpgradeCatalog271.class);
      catalogBinder.addBinding().to(UpgradeCatalog272.class);
      catalogBinder.addBinding().to(UpgradeCatalog280.class);
      catalogBinder.addBinding().to(UpdateAlertScriptPaths.class);
      catalogBinder.addBinding().to(FinalUpgradeCatalog.class);

      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
    }
  }

  public void executeUpgrade(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Executing DDL upgrade...");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.upgradeSchema();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executePreDMLUpdates(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Executing Pre-DML changes.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.preUpgradeData();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executeDMLUpdates(List<UpgradeCatalog> upgradeCatalogs, String ambariUpgradeConfigUpdatesFileName) throws AmbariException {
    LOG.info("Executing DML changes.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.setConfigUpdatesFileName(ambariUpgradeConfigUpdatesFileName);
          upgradeCatalog.upgradeData();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executeOnPostUpgrade(List<UpgradeCatalog> upgradeCatalogs)
      throws AmbariException {
    LOG.info("Finalizing catalog upgrade.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.onPostUpgrade();
          upgradeCatalog.updateDatabaseSchemaVersion();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void outputUpgradeJsonOutput(List<UpgradeCatalog> upgradeCatalogs)
      throws AmbariException {
    LOG.info("Combining upgrade json output.");
    Map<String,String> combinedUpgradeJsonOutput = new HashMap<>();

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          combinedUpgradeJsonOutput.putAll(upgradeCatalog.getUpgradeJsonOutput());

        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
    String content = gson.toJson(combinedUpgradeJsonOutput);
    System.out.println(content);
  }

  public void resetUIState() throws AmbariException {
    LOG.info("Resetting UI state.");
    try {
      dbAccessor.updateTable("key_value_store", dbAccessor.quoteObjectName("value"),
          "{\"clusterState\":\"CLUSTER_STARTED_5\"}",
          "where " + dbAccessor.quoteObjectName("key") + "='CLUSTER_CURRENT_STATUS'");
    } catch (SQLException e) {
      throw new AmbariException("Unable to reset UI state", e);
    }
  }

  public void cleanUpRCATables() {
    LOG.info("Cleaning up RCA tables.");
    for (String tableName : rcaTableNames) {
      try {
        if (dbAccessor.tableExists(tableName)) {
          dbAccessor.truncateTable(tableName);
        }
      } catch (Exception e) {
        LOG.warn("Error cleaning rca table " + tableName, e);
      }
    }
    try {
      cleanUpTablesFromRCADatabase();
    } catch (Exception e) {
      LOG.warn("Error cleaning rca tables from ambarirca db", e);
    }
  }

  private void cleanUpTablesFromRCADatabase() throws ClassNotFoundException, SQLException {
    String driverName = configuration.getRcaDatabaseDriver();
    String connectionURL = configuration.getRcaDatabaseUrl();
    if (connectionURL.contains(Configuration.HOSTNAME_MACRO)) {
      connectionURL = connectionURL.replace(Configuration.HOSTNAME_MACRO, "localhost");
    }
    String username = configuration.getRcaDatabaseUser();
    String password = configuration.getRcaDatabasePassword();

    Class.forName(driverName);
    try (Connection connection = DriverManager.getConnection(connectionURL, username, password)) {
      connection.setAutoCommit(true);
      for (String tableName : rcaTableNames) {
        String query = "DELETE FROM " + tableName;
        try (Statement statement = connection.createStatement()) {
          statement.execute(query);
        } catch (Exception e) {
          LOG.warn("Error while executing query: " + query, e);
        }
      }
    }
  }

  /**
   * Returns minimal version of available {@link UpgradeCatalog}
   *
   * @return string representation of minimal version of {@link UpgradeCatalog}
   */
  private String getMinimalUpgradeCatalogVersion(){
    List<UpgradeCatalog> candidateCatalogs = new ArrayList<>(allUpgradeCatalogs);
    Collections.sort(candidateCatalogs, new AbstractUpgradeCatalog.VersionComparator());

    if (candidateCatalogs.isEmpty()) {
      return null;
    }

    return candidateCatalogs.iterator().next().getTargetVersion();
  }

  /**
   * Checks if source version meets minimal requirements for upgrade
   *
   * @param minUpgradeVersion min allowed version for the upgrade, could be obtained via {@link #getMinimalUpgradeCatalogVersion()}
   * @param sourceVersion current version of the Database, which need to be upgraded
   *
   * @return  true if upgrade is allowed or false if not
   */
  private boolean verifyUpgradePath(String minUpgradeVersion, String sourceVersion){
    if (null == minUpgradeVersion){
      return false;
    }

    return VersionUtils.compareVersions(sourceVersion, minUpgradeVersion) >= 0;
  }

  private List<String> getMyISAMTables() throws SQLException {
    if (!configuration.getDatabaseType().equals(Configuration.DatabaseType.MYSQL)) {
      return Collections.emptyList();
    }
    List<String> myISAMTables = new ArrayList<>();
    String query = String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' " +
      "AND engine = 'MyISAM' AND table_type = 'BASE TABLE'", configuration.getServerDBName());
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      rs = statement.executeQuery(query);
      if (rs != null) {
        while (rs.next()) {
          myISAMTables.add(rs.getString("table_name"));
        }
      }
    } finally {
      JdbcUtils.closeResultSet(rs);
      JdbcUtils.closeStatement(statement);
    }
    return myISAMTables;
  }

  /**
   * Upgrade Ambari DB schema to the target version passed in as the only
   * argument.
   * @param args args[0] = target version to upgrade to.
   */
  public static void main(String[] args) throws Exception {
    try {
      // check java version to be higher then 1.6
      String[] splittedJavaVersion = System.getProperty("java.version").split("\\.");
      float javaVersion = Float.parseFloat(splittedJavaVersion[0] + "." + splittedJavaVersion[1]);
      if (javaVersion < Configuration.JDK_MIN_VERSION) {
        LOG.error(String.format("Oracle JDK version is lower than %.1f It can cause problems during upgrade process. Please," +
                " use 'ambari-server setup' command to upgrade JDK!", Configuration.JDK_MIN_VERSION));
        System.exit(1);
      }

      Injector injector = Guice.createInjector(new UpgradeHelperModule(), new AuditLoggerModule(), new LdapModule());

      // Startup the JPA infrastructure, but do not indicate it is initialized since the underlying
      // database schema may not be updated to meet the expectations of the Entity instances.
      GuiceJpaInitializer jpaInitializer = injector.getInstance(GuiceJpaInitializer.class);

      SchemaUpgradeHelper schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);

      //Fail if MySQL database has tables with MyISAM engine
      List<String> myISAMTables = schemaUpgradeHelper.getMyISAMTables();
      if (!myISAMTables.isEmpty()) {
        String errorMessage = String.format("Unsupported MyISAM table %s detected. " +
            "For correct upgrade database should be migrated to InnoDB engine.", myISAMTables.get(0));
        LOG.error(errorMessage);
        throw new AmbariException(errorMessage);
      }

      String targetVersion = schemaUpgradeHelper.getAmbariServerVersion();
      LOG.info("Upgrading schema to target version = " + targetVersion);

      UpgradeCatalog targetUpgradeCatalog = AbstractUpgradeCatalog
        .getUpgradeCatalog(targetVersion);

      LOG.debug("Target upgrade catalog. {}", targetUpgradeCatalog);

      // Read source version from DB
      String sourceVersion = schemaUpgradeHelper.readSourceVersion();
      LOG.info("Upgrading schema from source version = " + sourceVersion);

      String minimalRequiredUpgradeVersion = schemaUpgradeHelper.getMinimalUpgradeCatalogVersion();

      if (!schemaUpgradeHelper.verifyUpgradePath(minimalRequiredUpgradeVersion, sourceVersion)){
        throw new AmbariException(String.format("Database version does not meet minimal upgrade requirements. Expected version should be not less than %s, current version is %s",
          minimalRequiredUpgradeVersion, sourceVersion));
      }

      List<UpgradeCatalog> upgradeCatalogs =
        schemaUpgradeHelper.getUpgradePath(sourceVersion, targetVersion);

      String date = new SimpleDateFormat("MM-dd-yyyy_HH:mm:ss").format(new Date());
      String ambariUpgradeConfigUpdatesFileName = "ambari_upgrade_config_changes_" + date + ".json";

      schemaUpgradeHelper.executeUpgrade(upgradeCatalogs);

      // The DDL is expected to be updated, now send the JPA initialized event so Entity
      // implementations can be created.
      jpaInitializer.setInitialized();

      schemaUpgradeHelper.executePreDMLUpdates(upgradeCatalogs);

      schemaUpgradeHelper.executeDMLUpdates(upgradeCatalogs, ambariUpgradeConfigUpdatesFileName);

      schemaUpgradeHelper.executeOnPostUpgrade(upgradeCatalogs);
      schemaUpgradeHelper.outputUpgradeJsonOutput(upgradeCatalogs);

      schemaUpgradeHelper.resetUIState();

      LOG.info("Upgrade successful.");

      schemaUpgradeHelper.cleanUpRCATables();

      schemaUpgradeHelper.stopPersistenceService();

      // Signal all threads that we are ready to exit...
      System.exit(0);
    } catch (Throwable e) {
      if (e instanceof AmbariException) {
        LOG.error("Exception occurred during upgrade, failed", e);
        throw (AmbariException)e;
      }else{
        LOG.error("Unexpected error, upgrade failed", e);
        throw new Exception("Unexpected error, upgrade failed", e);
      }
    }
  }
}
