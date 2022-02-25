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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.ClientConfigFileDefinition;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

public class DatabaseConsistencyCheckHelper {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseConsistencyCheckHelper.class);

  @Inject
  private static Injector injector;

  private static MetainfoDAO metainfoDAO;
  private static AlertDefinitionDAO alertDefinitionDAO;
  private static Connection connection;
  private static AmbariMetaInfo ambariMetaInfo;
  private static DBAccessor dbAccessor;
  private static HostRoleCommandDAO hostRoleCommandDAO;
  private static ExecutionCommandDAO executionCommandDAO;
  private static StageDAO stageDAO;

  private static DatabaseConsistencyCheckResult checkResult = DatabaseConsistencyCheckResult.DB_CHECK_SUCCESS;
  public static final String GET_CONFIGS_SELECTED_MORE_THAN_ONCE_QUERY = "select c.cluster_name, cc.type_name from clusterconfig cc " +
      "join clusters c on cc.cluster_id=c.cluster_id " +
      "group by c.cluster_name, cc.type_name " +
      "having sum(cc.selected) > 1";

  /**
   * @return The result of the DB cheks run so far.
   */
  public static DatabaseConsistencyCheckResult getLastCheckResult() {
    return checkResult;
  }

  /**
   * Reset check result to {@link DatabaseConsistencyCheckResult#DB_CHECK_SUCCESS}.
   */
  public static void resetCheckResult() {
    checkResult = DatabaseConsistencyCheckResult.DB_CHECK_SUCCESS;
  }

  /**
   * Called internally to set the result of the DB checks. The new result is only recorded if it is more severe than
   * the existing result.
   *
    * @param newResult the new result
   */
  private static void setCheckResult(DatabaseConsistencyCheckResult newResult) {
    if (newResult.ordinal() > checkResult.ordinal()) {
      checkResult = newResult;
    }
  }

  /**
   * Called to indicate a warning during checks
   *
   * @param messageTemplate Message template (log4j format)
   * @param messageParams Message params
   */
  private static void warning(String messageTemplate, Object... messageParams) {
    LOG.warn(messageTemplate, messageParams);
    setCheckResult(DatabaseConsistencyCheckResult.DB_CHECK_WARNING);
  }

  /**
   * Called to indicate an error during checks
   *
   * @param messageTemplate Message template (log4j format)
   * @param messageParams Message params
   */
  private static void error(String messageTemplate, Object... messageParams) {
    LOG.error(messageTemplate, messageParams);
    setCheckResult(DatabaseConsistencyCheckResult.DB_CHECK_ERROR);
  }

  public static void setInjector(Injector injector) {
    DatabaseConsistencyCheckHelper.injector = injector;
    // Clean up: new injector means static fields should be reinitalized, though in real life it only occurs during testing
    closeConnection();
    connection = null;
    metainfoDAO = null;
    alertDefinitionDAO = null;
    ambariMetaInfo = null;
    dbAccessor = null;
  }

  public static void setConnection(Connection connection) {
    DatabaseConsistencyCheckHelper.connection = connection;
  }

  /*
    * method to close connection
    * */
  public static void closeConnection() {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception occurred during connection close procedure: ", e);
    }
  }

  public static DatabaseConsistencyCheckResult runAllDBChecks(boolean fixIssues) throws Throwable {
    LOG.info("******************************* Check database started *******************************");
    try {
      if (fixIssues) {
        fixHostComponentStatesCountEqualsHostComponentsDesiredStates();
        fixClusterConfigsNotMappedToAnyService();
        fixConfigGroupServiceNames();
        fixConfigGroupHostMappings();
        fixConfigGroupsForDeletedServices();
        fixConfigsSelectedMoreThanOnce();
        fixAlertsForDeletedServices();
      }
      checkSchemaName();
      checkMySQLEngine();
      checkForConfigsNotMappedToService();
      checkForConfigsSelectedMoreThanOnce();
      checkForHostsWithoutState();
      checkHostComponentStates();
      checkServiceConfigs();
      checkForLargeTables();
      checkConfigGroupsHasServiceName();
      checkConfigGroupHostMapping(true);
      checkConfigGroupsForDeletedServices(true);
      checkForStalealertdefs();
      LOG.info("******************************* Check database completed *******************************");
      return checkResult;
    }
    catch (Throwable ex) {
      LOG.error("An error occurred during database consistency check.", ex);
      throw ex;
    }
  }

  public static void checkDBVersionCompatible() throws AmbariException {
    LOG.info("Checking DB store version");

    if (metainfoDAO == null) {
      metainfoDAO = injector.getInstance(MetainfoDAO.class);
    }
    if (alertDefinitionDAO == null) {
      alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    }
    MetainfoEntity schemaVersionEntity = metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY);
    String schemaVersion = null;

    if (schemaVersionEntity != null) {
      schemaVersion = schemaVersionEntity.getMetainfoValue();
    }

    Configuration conf = injector.getInstance(Configuration.class);
    File versionFile = new File(conf.getServerVersionFilePath());
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    String serverVersion = null;
    try (Scanner scanner = new Scanner(versionFile)) {
      serverVersion = scanner.useDelimiter("\\Z").next();

    } catch (IOException ioe) {
      throw new AmbariException("Unable to read server version file.");
    }

    if (schemaVersionEntity==null || VersionUtils.compareVersions(schemaVersion, serverVersion, 3) != 0) {
      String error = "Current database store version is not compatible with " +
              "current server version"
              + ", serverVersion=" + serverVersion
              + ", schemaVersion=" + schemaVersion;
      LOG.error(error);
      throw new AmbariException(error);
    }

    LOG.info("DB store version is compatible");
  }

  /**
   * This method checks if ambari database has tables with too big size (according to limit).
   * First of all we are trying to get table size from schema information, but if it's not possible,
   * we will get tables rows count and compare it with row count limit.
   */
  static void checkForLargeTables() {
    LOG.info("Checking for tables with large physical size");

    ensureConnection();

    DBAccessor.DbType dbType = dbAccessor.getDbType();
    String schemaName = dbAccessor.getDbSchema();

    String GET_TABLE_SIZE_IN_BYTES_POSTGRESQL = "SELECT pg_total_relation_size('%s') \"Table Size\"";
    String GET_TABLE_SIZE_IN_BYTES_MYSQL = "SELECT (data_length + index_length) \"Table Size\" FROM information_schema.TABLES WHERE table_schema = \"" + schemaName + "\" AND table_name =\"%s\"";
    String GET_TABLE_SIZE_IN_BYTES_ORACLE = "SELECT bytes \"Table Size\" FROM user_segments WHERE segment_type='TABLE' AND segment_name='%s'";
    String GET_ROW_COUNT_QUERY = "SELECT COUNT(*) FROM %s";

    Map<DBAccessor.DbType, String> tableSizeQueryMap = new HashMap<>();
    tableSizeQueryMap.put(DBAccessor.DbType.POSTGRES, GET_TABLE_SIZE_IN_BYTES_POSTGRESQL);
    tableSizeQueryMap.put(DBAccessor.DbType.MYSQL, GET_TABLE_SIZE_IN_BYTES_MYSQL);
    tableSizeQueryMap.put(DBAccessor.DbType.ORACLE, GET_TABLE_SIZE_IN_BYTES_ORACLE);

    List<String> tablesToCheck = Arrays.asList("host_role_command", "execution_command", "stage", "request", "alert_history");

    final double TABLE_SIZE_LIMIT_MB = 3000.0;
    final int TABLE_ROW_COUNT_LIMIT = 3000000;

    String findTableSizeQuery = tableSizeQueryMap.get(dbType);

    if (dbType == DBAccessor.DbType.ORACLE) {
      for (int i = 0;i < tablesToCheck.size(); i++) {
        tablesToCheck.set(i, tablesToCheck.get(i).toUpperCase());
      }
    }

    for (String tableName : tablesToCheck) {

      ResultSet rs = null;
      Statement statement = null;
      Double tableSizeInMB = null;
      Long tableSizeInBytes = null;
      int tableRowCount = -1;

      try {
        statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        rs = statement.executeQuery(String.format(findTableSizeQuery, tableName));
        if (rs != null) {
          while (rs.next()) {
            tableSizeInBytes = rs.getLong(1);
            if (tableSizeInBytes != null) {
              tableSizeInMB = tableSizeInBytes / 1024.0 / 1024.0;
            }
          }
        }

        if (tableSizeInMB != null && tableSizeInMB > TABLE_SIZE_LIMIT_MB) {
          warning("The database table {} is currently {} MB (limit is {}) and may impact performance. It is recommended " +
                  "that you reduce its size by executing \"ambari-server db-purge-history\".",
                  tableName, tableSizeInMB, TABLE_SIZE_LIMIT_MB);
        } else if (tableSizeInMB != null && tableSizeInMB < TABLE_SIZE_LIMIT_MB) {
          LOG.info(String.format("The database table %s is currently %.3f MB and is within normal limits (%.3f)",
                  tableName, tableSizeInMB, TABLE_SIZE_LIMIT_MB));
        } else {
          throw new Exception();
        }
      } catch (Exception e) {
        LOG.error(String.format("Failed to get %s table size from database, will check row count: ", tableName), e);
        try {
          rs = statement.executeQuery(String.format(GET_ROW_COUNT_QUERY, tableName));
          if (rs != null) {
            while (rs.next()) {
              tableRowCount = rs.getInt(1);
            }
          }

          if (tableRowCount > TABLE_ROW_COUNT_LIMIT) {
            warning("The database table {} currently has {} rows (limit is {}) and may impact performance. It is " +
                    "recommended that you reduce its size by executing \"ambari-server db-purge-history\".",
                    tableName, tableRowCount, TABLE_ROW_COUNT_LIMIT);
          } else if (tableRowCount != -1 && tableRowCount < TABLE_ROW_COUNT_LIMIT) {
            LOG.info(String.format("The database table %s currently has %d rows and is within normal limits (%d)", tableName, tableRowCount, TABLE_ROW_COUNT_LIMIT));
          } else {
            warning("Unable to get size for table {}!", tableName);
          }
        } catch (SQLException ex) {
          warning(String.format("Failed to get %s row count: ", tableName), e);
        }
      } finally {
        if (rs != null) {
          try {
            rs.close();
          } catch (SQLException e) {
            LOG.error("Exception occurred during result set closing procedure: ", e);
          }
        }

        if (statement != null) {
          try {
            statement.close();
          } catch (SQLException e) {
            LOG.error("Exception occurred during statement closing procedure: ", e);
          }
        }
      }
    }

  }

  /**
   * This method checks if any config type in clusterconfig table, has more than
   * one versions selected. If config version is selected(in selected column =
   * 1), it means that this version of config is actual. So, if any config type
   * has more than one selected version it's a bug and we are showing error
   * message for user.
   */
  static void checkForConfigsSelectedMoreThanOnce() {
    LOG.info("Checking for more than 1 configuration of the same type being enabled.");

    String GET_CONFIGS_SELECTED_MORE_THAN_ONCE_QUERY = "select c.cluster_name, cc.type_name from clusterconfig cc "
        + "join clusters c on cc.cluster_id=c.cluster_id "
        + "group by c.cluster_name, cc.type_name " +
            "having sum(selected) > 1";

    Multimap<String, String> clusterConfigTypeMap = HashMultimap.create();
    ResultSet rs = null;
    Statement statement = null;

    ensureConnection();

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_CONFIGS_SELECTED_MORE_THAN_ONCE_QUERY);
      if (rs != null) {
        while (rs.next()) {
          clusterConfigTypeMap.put(rs.getString("cluster_name"), rs.getString("type_name"));
        }

        for (String clusterName : clusterConfigTypeMap.keySet()) {
          error("You have config(s), in cluster {}, that is(are) selected more than once in clusterconfig table: {}",
                  clusterName ,StringUtils.join(clusterConfigTypeMap.get(clusterName), ","));
        }
      }

    } catch (SQLException e) {
      warning("Exception occurred during check for config selected more than once procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }
  }

  /**
  * This method checks if all hosts from hosts table
  * has related host state info in hoststate table.
  * If not then we are showing error.
  * */
  static void checkForHostsWithoutState() {
    LOG.info("Checking for hosts without state");

    String GET_HOSTS_WITHOUT_STATUS_QUERY = "select host_name from hosts where host_id not in (select host_id from hoststate)";
    Set<String> hostsWithoutStatus = new HashSet<>();
    ResultSet rs = null;
    Statement statement = null;

    ensureConnection();

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_HOSTS_WITHOUT_STATUS_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostsWithoutStatus.add(rs.getString("host_name"));
        }

        if (!hostsWithoutStatus.isEmpty()) {
          warning("You have host(s) without state (in hoststate table): " + StringUtils.join(hostsWithoutStatus, ","));
        }
      }

    } catch (SQLException e) {
      warning("Exception occurred during check for host without state procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }
  }

  private static int runQuery(Statement statement, String query) {
    ResultSet rs = null;
    int result = 0;
    try {
      rs = statement.executeQuery(query);

      if (rs != null) {
        while (rs.next()) {
          result = rs.getInt(1);
        }
      }

    } catch (SQLException e) {
      warning("Exception occurred during topology request tables check: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }
    }
    return result;
  }


  /**
  * This method checks if count of host component states equals count
  * of desired host component states. According to ambari logic these
  * two tables should have the same count of rows. If not then we are
  * showing error for user.
  *
  * One more, we are checking if all components has only one host
  * component state. If some component has more, it can cause issues
  * */
  static void checkHostComponentStates() {
    LOG.info("Checking host component states count equals host component desired states count");

    String GET_HOST_COMPONENT_STATE_COUNT_QUERY = "select count(*) from hostcomponentstate";
    String GET_HOST_COMPONENT_DESIRED_STATE_COUNT_QUERY = "select count(*) from hostcomponentdesiredstate";
    String GET_MERGED_TABLE_ROW_COUNT_QUERY = "select count(*) FROM hostcomponentstate hcs " +
            "JOIN hostcomponentdesiredstate hcds ON hcs.service_name=hcds.service_name AND hcs.component_name=hcds.component_name AND hcs.host_id=hcds.host_id";
    String GET_HOST_COMPONENT_STATE_DUPLICATES_QUERY = "select component_name, host_id from hostcomponentstate group by component_name, host_id having count(component_name) > 1";
    int hostComponentStateCount = 0;
    int hostComponentDesiredStateCount = 0;
    Map<String, String> hostComponentStateDuplicates = new HashMap<>();
    int mergedCount = 0;
    ResultSet rs = null;
    Statement statement = null;

    ensureConnection();

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

      rs = statement.executeQuery(GET_HOST_COMPONENT_STATE_COUNT_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostComponentStateCount = rs.getInt(1);
        }
      }

      rs = statement.executeQuery(GET_HOST_COMPONENT_DESIRED_STATE_COUNT_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostComponentDesiredStateCount = rs.getInt(1);
        }
      }

      rs = statement.executeQuery(GET_MERGED_TABLE_ROW_COUNT_QUERY);
      if (rs != null) {
        while (rs.next()) {
          mergedCount = rs.getInt(1);
        }
      }

      if (hostComponentStateCount != hostComponentDesiredStateCount || hostComponentStateCount != mergedCount) {
        warning("Your host component states (hostcomponentstate table) count not equals host component desired states (hostcomponentdesiredstate table) count!");
      }


      rs = statement.executeQuery(GET_HOST_COMPONENT_STATE_DUPLICATES_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostComponentStateDuplicates.put(rs.getString("component_name"), rs.getString("host_id"));
        }
      }

      for (Map.Entry<String, String> component : hostComponentStateDuplicates.entrySet()) {
        warning("Component {} on host with id {}, has more than one host component state (hostcomponentstate table)!", component.getKey(), component.getValue());
      }

    } catch (SQLException e) {
      warning("Exception occurred during check for same count of host component states and host component desired states: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }

  }

  /**
  * Remove configs that are not mapped to any service.
  */
  @Transactional
  static void fixClusterConfigsNotMappedToAnyService() {
    LOG.info("Checking for configs not mapped to any Service");
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<ClusterConfigEntity> notMappedClusterConfigs = getNotMappedClusterConfigsToService();

    for (ClusterConfigEntity clusterConfigEntity : notMappedClusterConfigs){
      if (clusterConfigEntity.isUnmapped()){
        continue; // skip clusterConfigs that did not leave after service deletion
      }
      List<String> types = new ArrayList<>();
      String type = clusterConfigEntity.getType();
      types.add(type);
      LOG.error("Removing config that is not mapped to any service", clusterConfigEntity);
      clusterDAO.removeConfig(clusterConfigEntity);
    }
  }


  /**
   * Find ClusterConfigs that are not mapped to Service
   * @return ClusterConfigs that are not mapped to Service
   */
  private static List<ClusterConfigEntity> getNotMappedClusterConfigsToService() {
    Provider<EntityManager> entityManagerProvider = injector.getProvider(EntityManager.class);
    EntityManager entityManager = entityManagerProvider.get();

    String queryName = "ClusterConfigEntity.findNotMappedClusterConfigsToService";
    TypedQuery<ClusterConfigEntity> query = entityManager.createNamedQuery(queryName, ClusterConfigEntity.class);

    return query.getResultList();
  }

  /**
   * Look for configs that are not mapped to any service.
   */
  static void checkForConfigsNotMappedToService() {
    LOG.info("Checking for configs that are not mapped to any service");
    List<ClusterConfigEntity> notMappedClasterConfigs = getNotMappedClusterConfigsToService();

    Set<String> nonMappedConfigs = new HashSet<>();
    for (ClusterConfigEntity clusterConfigEntity : notMappedClasterConfigs) {
      if (!clusterConfigEntity.isUnmapped()){ //this check does not report warning for configs from deleted services
        nonMappedConfigs.add(clusterConfigEntity.getType() + '-' + clusterConfigEntity.getTag());
      }
    }
    if (!nonMappedConfigs.isEmpty()){
      warning("You have config(s): {} that is(are) not mapped (in serviceconfigmapping table) to any service!", StringUtils.join(nonMappedConfigs, ","));
    }
  }

  /**
  * This method checks if count of host component states equals count
  * of desired host component states. According to ambari logic these
  * two tables should have the same count of rows. If not then we are
  * adding missed host components.
  * hard to predict root couse of inconsistency, so it can be dangerous
  */
  @Transactional
  static void fixHostComponentStatesCountEqualsHostComponentsDesiredStates() {
    LOG.info("Checking that there are the same number of actual and desired host components");

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);

    List<HostComponentDesiredStateEntity> hostComponentDesiredStates = hostComponentDesiredStateDAO.findAll();
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findAll();

    Set<HostComponentDesiredStateEntity> missedHostComponentDesiredStates = new HashSet<>();
    missedHostComponentDesiredStates.addAll(hostComponentDesiredStates);
    Set<HostComponentStateEntity> missedHostComponentStates = new HashSet<>();
    missedHostComponentStates.addAll(hostComponentStates);

    for (Iterator<HostComponentStateEntity> stateIterator = missedHostComponentStates.iterator(); stateIterator.hasNext();){
      HostComponentStateEntity hostComponentStateEntity = stateIterator.next();
      for (Iterator<HostComponentDesiredStateEntity> desiredStateIterator = missedHostComponentDesiredStates.iterator(); desiredStateIterator.hasNext();) {
        HostComponentDesiredStateEntity hostComponentDesiredStateEntity = desiredStateIterator.next();
        if (hostComponentStateEntity.getComponentName().equals(hostComponentDesiredStateEntity.getComponentName()) &&
            hostComponentStateEntity.getServiceName().equals(hostComponentDesiredStateEntity.getServiceName()) &&
            hostComponentStateEntity.getHostId().equals(hostComponentDesiredStateEntity.getHostId())){
          desiredStateIterator.remove();
          stateIterator.remove();
          break;
        }
      }
    }

    for (HostComponentDesiredStateEntity hostComponentDesiredStateEntity : missedHostComponentDesiredStates) {
      HostComponentStateEntity stateEntity = new HostComponentStateEntity();
      stateEntity.setClusterId(hostComponentDesiredStateEntity.getClusterId());
      stateEntity.setComponentName(hostComponentDesiredStateEntity.getComponentName());
      stateEntity.setServiceName(hostComponentDesiredStateEntity.getServiceName());
      stateEntity.setVersion(State.UNKNOWN.toString());
      stateEntity.setHostEntity(hostComponentDesiredStateEntity.getHostEntity());
      stateEntity.setCurrentState(State.UNKNOWN);
      stateEntity.setUpgradeState(UpgradeState.NONE);
      stateEntity.setServiceComponentDesiredStateEntity(hostComponentDesiredStateEntity.getServiceComponentDesiredStateEntity());

      LOG.error("Trying to add missing record in hostcomponentstate: {}", stateEntity);
      hostComponentStateDAO.create(stateEntity);
    }

    for (HostComponentStateEntity missedHostComponentState : missedHostComponentStates) {

      HostComponentDesiredStateEntity stateEntity = new HostComponentDesiredStateEntity();
      stateEntity.setClusterId(missedHostComponentState.getClusterId());
      stateEntity.setComponentName(missedHostComponentState.getComponentName());
      stateEntity.setServiceName(missedHostComponentState.getServiceName());
      stateEntity.setHostEntity(missedHostComponentState.getHostEntity());
      stateEntity.setDesiredState(State.UNKNOWN);
      stateEntity.setServiceComponentDesiredStateEntity(missedHostComponentState.getServiceComponentDesiredStateEntity());

      LOG.error("Trying to add missing record in hostcomponentdesiredstate: {}", stateEntity);
      hostComponentDesiredStateDAO.create(stateEntity);
    }
  }

  /**
   * This makes the following checks for Postgres:
   * <ol>
   *   <li>Check if the connection's schema (first item on search path) is the one set in ambari.properties</li>
   *   <li>Check if the connection's schema is present in the DB</li>
   *   <li>Check if the ambari tables exist in the schema configured in ambari.properties</li>
   *   <li>Check if ambari tables don't exist in other shemas</li>
   * </ol>
   * The purpose of these checks is to avoid that tables and constraints in ambari's schema get confused with tables
   * and constraints in other schemas on the DB user's search path. This can happen after an improperly made DB restore
   * operation and can cause issues during upgrade.
   * we may have no permissions (e.g. external DB) to auto fix this problem
  **/
  static void checkSchemaName () {
    Configuration conf = injector.getInstance(Configuration.class);
    if(conf.getDatabaseType() == Configuration.DatabaseType.POSTGRES) {
      LOG.info("Ensuring that the schema set for Postgres is correct");

      ensureConnection();

      try (ResultSet schemaRs = connection.getMetaData().getSchemas();
           ResultSet searchPathRs = connection.createStatement().executeQuery("show search_path");
           ResultSet ambariTablesRs = connection.createStatement().executeQuery(
               "select table_schema from information_schema.tables where table_name = 'hostcomponentdesiredstate'")) {
        // Check if ambari's schema exists
        final boolean ambariSchemaExists = getResultSetColumn(schemaRs, "TABLE_SCHEM").contains(conf.getDatabaseSchema());
        if ( !ambariSchemaExists ) {
          warning("The schema [{}] defined for Ambari from ambari.properties has not been found in the database. " +
              "Storing Ambari tables under a different schema can lead to problems.", conf.getDatabaseSchema());
        }
        // Check if the right schema is first on the search path
        List<Object> searchPathResultColumn = getResultSetColumn(searchPathRs, "search_path");
        List<String> searchPath = searchPathResultColumn.isEmpty() ? ImmutableList.of() :
            ImmutableList.copyOf(Splitter.on(",").trimResults().split(String.valueOf(searchPathResultColumn.get(0))));
        String firstSearchPathItem = searchPath.isEmpty() ? null : searchPath.get(0);
        if (!Objects.equals(firstSearchPathItem, conf.getDatabaseSchema())) {
          warning("The schema [{}] defined for Ambari in ambari.properties is not first on the search path:" +
              " {}. This can lead to problems.", conf.getDatabaseSchema(), searchPath);
        }
        // Check schemas with Ambari tables.
        ArrayList<Object> schemasWithAmbariTables = getResultSetColumn(ambariTablesRs, "table_schema");
        if ( ambariSchemaExists && !schemasWithAmbariTables.contains(conf.getDatabaseSchema()) ) {
          warning("The schema [{}] defined for Ambari from ambari.properties does not contain the Ambari tables. " +
              "Storing Ambari tables under a different schema can lead to problems.", conf.getDatabaseSchema());
        }
        if ( schemasWithAmbariTables.size() > 1 ) {
          warning("Multiple schemas contain the Ambari tables: {}. This can lead to problems.", schemasWithAmbariTables);
        }
      }
      catch (SQLException e) {
        warning("Exception occurred during checking db schema name: ", e);
      }
    }
  }

  private static ArrayList<Object> getResultSetColumn(@Nullable ResultSet rs, String columnName) throws SQLException {
    ArrayList<Object> values = new ArrayList<>();
    if (null != rs) {
      while (rs.next()) {
        values.add(rs.getObject(columnName));
      }
    }
    return values;
  }

  /**
  * This method checks tables engine type to be innodb for MySQL.
  * it's too risky to autofix by migrating DB to innodb
  * */
  static void checkMySQLEngine () {
    Configuration conf = injector.getInstance(Configuration.class);
    if(conf.getDatabaseType()!=Configuration.DatabaseType.MYSQL) {
      return;
    }
    LOG.info("Checking to ensure that the MySQL DB engine type is set to InnoDB");

    ensureConnection();

    String GET_INNODB_ENGINE_SUPPORT = "select TABLE_NAME, ENGINE from information_schema.tables where TABLE_SCHEMA = '%s' and LOWER(ENGINE) != 'innodb';";

    ResultSet rs = null;
    Statement statement;

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(String.format(GET_INNODB_ENGINE_SUPPORT, conf.getDatabaseSchema()));
      if (rs != null) {
        List<String> tablesInfo = new ArrayList<>();
        while (rs.next()) {
          tablesInfo.add(rs.getString("TABLE_NAME"));
        }
        if (!tablesInfo.isEmpty()){
          warning("Found tables with engine type that is not InnoDB : {}", tablesInfo);
        }
      }
    } catch (SQLException e) {
      warning("Exception occurred during checking MySQL engine to be innodb: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }
    }
  }

  /**
   * This method checks for stale alert definitions..
   * */
  static Map<String, String>  checkForStalealertdefs () {
    Configuration conf = injector.getInstance(Configuration.class);
    Map<String, String> alertInfo = new HashMap<>();
    LOG.info("Checking to ensure there is no stale alert definitions");

    ensureConnection();

    String STALE_ALERT_DEFINITIONS = "select definition_name, service_name from alert_definition where service_name not in " +
            "(select service_name from clusterservices) and service_name not in ('AMBARI')";

    ResultSet rs = null;
    Statement statement;

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(STALE_ALERT_DEFINITIONS);
      if (rs != null) {
        while (rs.next()) {
          alertInfo.put(rs.getString("definition_name"),rs.getString("service_name"));
        }
        if (!alertInfo.isEmpty()){
          String alertInfoStr = "";
          for (Map.Entry<String, String> entry : alertInfo.entrySet()) {
            alertInfoStr = entry.getKey() + "(" + entry.getValue() + ")" ;
          }
          warning("You have Alerts that are not mapped with any services : {}.Run --auto-fix-database to fix "  +
                  "this automatically. Please backup Ambari Server database before running --auto-fix-database.", alertInfoStr);
        }
      }
    } catch (SQLException e) {
      warning("Exception occurred during checking for stale alert definitions: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during  checking for stale alert definitions: ", e);
        }
      }
    }
    return alertInfo;
  }

  /**
   * Fix inconsistencies found by {@code checkForConfigsSelectedMoreThanOnce}
   * selecting latest one by selectedTimestamp
   */
  @Transactional
  static void fixConfigsSelectedMoreThanOnce() {
    LOG.info("Fix configs selected more than once");
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);

    Clusters clusters = injector.getInstance(Clusters.class);

    Multimap<String, String> clusterConfigTypeMap = HashMultimap.create();
    ResultSet rs = null;
    Statement statement = null;

    ensureConnection();

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_CONFIGS_SELECTED_MORE_THAN_ONCE_QUERY);
      if (rs != null) {
        while (rs.next()) {
          clusterConfigTypeMap.put(rs.getString("cluster_name"), rs.getString("type_name"));
        }
      }

    } catch (SQLException e) {
      warning("Exception occurred during check for config selected more than once procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }

    for (String clusterName : clusterConfigTypeMap.keySet()) {
      Cluster cluster = null;
      try {
        cluster = clusters.getCluster(clusterName);

        Collection<String> typesWithMultipleSelectedConfigs = clusterConfigTypeMap.get(clusterName);

        for (String type: typesWithMultipleSelectedConfigs) {
          List<ClusterConfigEntity> enabledConfigsByType = getEnabledConfigsByType(cluster.getClusterId(), type);
          ClusterConfigEntity latestConfig = enabledConfigsByType.get(0);
          for (ClusterConfigEntity entity : enabledConfigsByType){
            entity.setSelected(false);
            if (latestConfig.getSelectedTimestamp() < entity.getSelectedTimestamp()){
              latestConfig = entity;
            }
            clusterDAO.merge(entity, true);
          }
          latestConfig.setSelected(true);
          clusterDAO.merge(latestConfig, true);
        }
      } catch (AmbariException e) {
        warning("Exception occurred during fix for config selected more than once procedure: ", e);
      }
    }
  }

  /**
   * Find ClusterConfigs with selected = 1
   * @return ClusterConfigs that are not mapped to Service by type
   */
  private static List<ClusterConfigEntity> getEnabledConfigsByType(long clusterId, String type) {

    Provider<EntityManager> entityManagerProvider = injector.getProvider(EntityManager.class);
    EntityManager entityManager = entityManagerProvider.get();

    Query query = entityManager.createNamedQuery("ClusterConfigEntity.findEnabledConfigByType",ClusterConfigEntity.class);
    query.setParameter("clusterId", clusterId);
    query.setParameter("type", type);

    return (List<ClusterConfigEntity>) query.getResultList();
  }

  /**
  * This method checks several potential problems for services:
  * 1) Check if we have services in cluster which doesn't have service config id(not available in serviceconfig table).
  * 2) Check if service has no mapped configs to it's service config id.
  * 3) Check if service has all required configs mapped to it.
  * 4) Check if service has config which is not selected(has no actual config version)
  * If any issue was discovered, we are showing warning message for user.
  * */
  static void checkServiceConfigs()  {
    LOG.info("Checking services and their configs");

    String GET_SERVICES_WITHOUT_CONFIGS_QUERY = "select c.cluster_name, service_name from clusterservices cs " +
            "join clusters c on cs.cluster_id=c.cluster_id " +
            "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)";
    String GET_SERVICE_CONFIG_WITHOUT_MAPPING_QUERY = "select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
            "join clusters c on sc.cluster_id=c.cluster_id " +
            "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null";
    String GET_STACK_NAME_VERSION_QUERY = "select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
            "join stack s on c.desired_stack_id = s.stack_id";
    String GET_SERVICES_WITH_CONFIGS_QUERY = "select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
            "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
            "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name, sc.version";
    String GET_NOT_SELECTED_SERVICE_CONFIGS_QUERY = "select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
            "join clusters c on cc.cluster_id=c.cluster_id " +
            "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name " +
            "having sum(cc.selected) < 1";
    Multimap<String, String> clusterServiceMap = HashMultimap.create();
    Map<String, Map<String, String>>  clusterStackInfo = new HashMap<>();
    Map<String, Multimap<String, String>> clusterServiceVersionMap = new HashMap<>();
    Map<String, Multimap<String, String>> clusterServiceConfigType = new HashMap<>();
    ResultSet rs = null;
    Statement statement = null;

    ensureConnection();

    LOG.info("Getting ambari metainfo instance");
    if (ambariMetaInfo == null) {
      ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    }

    try {
      LOG.info("Executing query 'GET_SERVICES_WITHOUT_CONFIGS'");
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

      rs = statement.executeQuery(GET_SERVICES_WITHOUT_CONFIGS_QUERY);
      if (rs != null) {
        while (rs.next()) {
          clusterServiceMap.put(rs.getString("cluster_name"), rs.getString("service_name"));
        }

        for (String clusterName : clusterServiceMap.keySet()) {
          warning("Service(s): {}, from cluster {} has no config(s) in serviceconfig table!", StringUtils.join(clusterServiceMap.get(clusterName), ","), clusterName);
        }

      }
      LOG.info("Executing query 'GET_SERVICE_CONFIG_WITHOUT_MAPPING'");
      rs = statement.executeQuery(GET_SERVICE_CONFIG_WITHOUT_MAPPING_QUERY);
      if (rs != null) {
        String serviceName = null, version = null, clusterName = null;
        while (rs.next()) {
          serviceName = rs.getString("service_name");
          clusterName = rs.getString("cluster_name");
          version = rs.getString("version");

          if (clusterServiceVersionMap.get(clusterName) != null) {
            Multimap<String, String> serviceVersion = clusterServiceVersionMap.get(clusterName);
            serviceVersion.put(serviceName, version);
          } else {
            Multimap<String, String> serviceVersion = HashMultimap.create();;
            serviceVersion.put(serviceName, version);
            clusterServiceVersionMap.put(clusterName, serviceVersion);
          }
        }

        for (String clName : clusterServiceVersionMap.keySet()) {
          Multimap<String, String> serviceVersion = clusterServiceVersionMap.get(clName);
          for (String servName : serviceVersion.keySet()) {
            warning("In cluster {}, service config mapping is unavailable (in table serviceconfigmapping) for service {} with version(s) {}! ", clName, servName, StringUtils.join(serviceVersion.get(servName), ","));
          }
        }

      }

      //get stack info from db
      LOG.info("Getting stack info from database");
      rs = statement.executeQuery(GET_STACK_NAME_VERSION_QUERY);
      if (rs != null) {
        while (rs.next()) {
          Map<String, String> stackInfoMap = new HashMap<>();
          stackInfoMap.put(rs.getString("stack_name"), rs.getString("stack_version"));
          clusterStackInfo.put(rs.getString("cluster_name"), stackInfoMap);
        }
      }


      Set<String> serviceNames = new HashSet<>();
      Map<String, Map<Integer, Multimap<String, String>>> dbClusterServiceVersionConfigs = new HashMap<>();
      Multimap<String, String> stackServiceConfigs = HashMultimap.create();

      LOG.info("Executing query 'GET_SERVICES_WITH_CONFIGS'");
      rs = statement.executeQuery(GET_SERVICES_WITH_CONFIGS_QUERY);
      if (rs != null) {
        String serviceName = null, configType = null, clusterName = null;
        Integer serviceVersion = null;
        while (rs.next()) {
          clusterName = rs.getString("cluster_name");
          serviceName = rs.getString("service_name");
          configType = rs.getString("type_name");
          serviceVersion = rs.getInt("version");

          serviceNames.add(serviceName);

          //collect data about mapped configs to services from db
          if (dbClusterServiceVersionConfigs.get(clusterName) != null) {
            Map<Integer, Multimap<String, String>> dbServiceVersionConfigs = dbClusterServiceVersionConfigs.get(clusterName);

            if (dbServiceVersionConfigs.get(serviceVersion) != null) {
              dbServiceVersionConfigs.get(serviceVersion).put(serviceName, configType);
            } else {
              Multimap<String, String> dbServiceConfigs = HashMultimap.create();
              dbServiceConfigs.put(serviceName, configType);
              dbServiceVersionConfigs.put(serviceVersion, dbServiceConfigs);
            }
          } else {
            Map<Integer, Multimap<String, String>> dbServiceVersionConfigs = new HashMap<>();
            Multimap<String, String> dbServiceConfigs = HashMultimap.create();
            dbServiceConfigs.put(serviceName, configType);
            dbServiceVersionConfigs.put(serviceVersion, dbServiceConfigs);
            dbClusterServiceVersionConfigs.put(clusterName, dbServiceVersionConfigs);
          }
        }
      }

      //compare service configs from stack with configs that we got from db
      LOG.info("Comparing service configs from stack with configs that we got from db");
      for (Map.Entry<String, Map<String, String>> clusterStackInfoEntry : clusterStackInfo.entrySet()) {
        //collect required configs for all services from stack
        String clusterName = clusterStackInfoEntry.getKey();
        Map<String, String> stackInfo = clusterStackInfoEntry.getValue();
        String stackName = stackInfo.keySet().iterator().next();
        String stackVersion = stackInfo.get(stackName);
        LOG.info("Getting services from metainfo");
        Map<String, ServiceInfo> serviceInfoMap = ambariMetaInfo.getServices(stackName, stackVersion);
        for (String serviceName : serviceNames) {
          LOG.info("Processing {}-{} / {}", stackName, stackVersion, serviceName);
          ServiceInfo serviceInfo = serviceInfoMap.get(serviceName);
          if (serviceInfo != null) {
            Set<String> configTypes = serviceInfo.getConfigTypeAttributes().keySet();
            for (String configType : configTypes) {
              stackServiceConfigs.put(serviceName, configType);
            }
          } else {
            warning("Service {} is not available for stack {} in cluster {}",
                    serviceName, stackName + "-" + stackVersion, clusterName);
          }
        }

        //compare required service configs from stack with mapped service configs from db
        LOG.info("Comparing required service configs from stack with mapped service configs from db");
        Map<Integer, Multimap<String, String>> dbServiceVersionConfigs = dbClusterServiceVersionConfigs.get(clusterName);
        if (dbServiceVersionConfigs != null) {
          for (Integer serviceVersion : dbServiceVersionConfigs.keySet()) {
            Multimap<String, String> dbServiceConfigs = dbServiceVersionConfigs.get(serviceVersion);
            if (dbServiceConfigs != null) {
              for (String serviceName : dbServiceConfigs.keySet()) {
                ServiceInfo serviceInfo = serviceInfoMap.get(serviceName);
                Collection<String> serviceConfigsFromStack = stackServiceConfigs.get(serviceName);
                Collection<String> serviceConfigsFromDB = dbServiceConfigs.get(serviceName);
                if (serviceConfigsFromDB != null && serviceConfigsFromStack != null) {
                  serviceConfigsFromStack.removeAll(serviceConfigsFromDB);
                  if (serviceInfo != null && serviceInfo.getComponents() != null) {
                    for (ComponentInfo componentInfo : serviceInfo.getComponents()) {
                      if (componentInfo.getClientConfigFiles() != null) {
                        for (ClientConfigFileDefinition clientConfigFileDefinition : componentInfo.getClientConfigFiles()) {
                          if (clientConfigFileDefinition.isOptional()) {
                            serviceConfigsFromStack.remove(clientConfigFileDefinition.getDictionaryName());
                          }
                        }
                      }
                    }
                  }

                  // skip ranger-{service_name}-* from being checked, unless ranger is installed
                  if(!dbServiceConfigs.containsKey("RANGER")) {
                    removeStringsByRegexp(serviceConfigsFromStack, "^ranger-"+ serviceName.toLowerCase() + "-" + "*");
                  }

                  if (!serviceConfigsFromStack.isEmpty()) {
                    warning("Required config(s): {} is(are) not available for service {} with service config version {} in cluster {}",
                            StringUtils.join(serviceConfigsFromStack, ","), serviceName, Integer.toString(serviceVersion), clusterName);
                  }
                }
              }
            }
          }
        }
      }

      // getting services which has mapped configs which are not selected in clusterconfig
      LOG.info("Getting services which has mapped configs which are not selected in clusterconfig");
      rs = statement.executeQuery(GET_NOT_SELECTED_SERVICE_CONFIGS_QUERY);
      if (rs != null) {
        String serviceName = null, configType = null, clusterName = null;
        while (rs.next()) {
          clusterName = rs.getString("cluster_name");
          serviceName = rs.getString("service_name");
          configType = rs.getString("type_name");


          if (clusterServiceConfigType.get(clusterName) != null) {
            Multimap<String, String> serviceConfigs = clusterServiceConfigType.get(clusterName);
            serviceConfigs.put(serviceName, configType);
          } else {

            Multimap<String, String> serviceConfigs = HashMultimap.create();
            serviceConfigs.put(serviceName, configType);
            clusterServiceConfigType.put(clusterName, serviceConfigs);

          }

        }
      }

      for (String clusterName : clusterServiceConfigType.keySet()) {
        Multimap<String, String> serviceConfig = clusterServiceConfigType.get(clusterName);
        for (String serviceName : serviceConfig.keySet()) {
          warning("You have non selected configs: {} for service {} from cluster {}!", StringUtils.join(serviceConfig.get(serviceName), ","), serviceName, clusterName);
        }
      }
    } catch (SQLException | AmbariException e) {
      warning("Exception occurred during complex service check procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }

  }

  /**
   * This method collects the ConfigGroups with empty or null service name field
   */
  static Map<Long, ConfigGroup> collectConfigGroupsWithoutServiceName() {
    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (MapUtils.isEmpty(clusterMap))
      return configGroupMap;

    for (Cluster cluster : clusterMap.values()) {
      Map<Long, ConfigGroup> configGroups = cluster.getConfigGroups();

      if (MapUtils.isEmpty(configGroups)) {
        continue;
      }

      for (ConfigGroup configGroup : configGroups.values()) {
        if (StringUtils.isEmpty(configGroup.getServiceName())) {
          configGroupMap.put(configGroup.getId(), configGroup);
        }
      }
    }

    return configGroupMap;
  }

  /**
   * This method checks if there are any ConfigGroup with empty or null service name field
   */
  static void checkConfigGroupsHasServiceName() {
    Map<Long, ConfigGroup> configGroupMap = collectConfigGroupsWithoutServiceName();
    if (MapUtils.isEmpty(configGroupMap))
      return;

    String message = String.join(" ), ( ",
            configGroupMap.values().stream().map(ConfigGroup::getName).collect(Collectors.toList()));

    warning("You have config groups present in the database with no " +
            "service name, [(ConfigGroup) => ( {} )]. Run --auto-fix-database to fix " +
            "this automatically. Please backup Ambari Server database before running --auto-fix-database.", message);
  }

  /**
   * Fix inconsistencies found by @collectConfigGroupsWithoutServiceName
   */
  @Transactional
  static void fixConfigGroupServiceNames() {
    Map<Long, ConfigGroup> configGroupMap = collectConfigGroupsWithoutServiceName();
    if (MapUtils.isEmpty(configGroupMap))
      return;

    Clusters clusters = injector.getInstance(Clusters.class);

    for (Map.Entry<Long, ConfigGroup> configGroupEntry : configGroupMap.entrySet()) {
      ConfigGroup configGroup = configGroupEntry.getValue();
      try {
        Cluster cluster = clusters.getCluster(configGroup.getClusterName());
        Map<String, Service> serviceMap = cluster.getServices();
        if (serviceMap.containsKey(configGroup.getTag())) {
          LOG.info("Setting service name of config group {} with id {} to {}",
                  configGroup.getName(), configGroupEntry.getKey(), configGroup.getTag());
          configGroup.setServiceName(configGroup.getTag());
        }
        else {
          LOG.info("Config group {} with id {} contains a tag {} which is not a service name in the cluster {}",
                  configGroup.getName(), configGroupEntry.getKey(), configGroup.getTag(), cluster.getClusterName());
        }
      } catch (AmbariException e) {
        // Ignore if cluster not found
      }
    }
  }

  /**
   * This method checks if there are any ConfigGroup host mappings with hosts
   * that are not longer a part of the cluster.
   */
  static Map<Long, Set<Long>> checkConfigGroupHostMapping(boolean warnIfFound) {
    LOG.info("Checking config group host mappings");
    Map<Long, Set<Long>> nonMappedHostIds = new HashMap<>();
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = clusters.getClusters();
    StringBuilder output = new StringBuilder("[(ConfigGroup, Service, HostCount) => ");

    if (!MapUtils.isEmpty(clusterMap)) {
      for (Cluster cluster : clusterMap.values()) {
        Map<Long, ConfigGroup> configGroups = cluster.getConfigGroups();
        Map<String, Host> clusterHosts = clusters.getHostsForCluster(cluster.getClusterName());

        if (!MapUtils.isEmpty(configGroups) && !MapUtils.isEmpty(clusterHosts)) {
          for (ConfigGroup configGroup : configGroups.values()) {
            // Based on current implementation of ConfigGroupImpl the
            // host mapping would be loaded only if the host actually exists
            // in the host table
            Map<Long, Host> hosts = configGroup.getHosts();
            boolean addToOutput = false;
            Set<String> hostnames = new HashSet<>();
            if (!MapUtils.isEmpty(hosts)) {
              for (Host host : hosts.values()) {
                // Lookup by hostname - It does have a unique constraint
                if (!clusterHosts.containsKey(host.getHostName())) {
                  Set<Long> hostIds = nonMappedHostIds.computeIfAbsent(configGroup.getId(), configGroupId -> new HashSet<>());
                  hostIds.add(host.getHostId());
                  hostnames.add(host.getHostName());
                  addToOutput = true;
                }
              }
            }
            if (addToOutput) {
              output.append("( ");
              output.append(configGroup.getName());
              output.append(", ");
              output.append(configGroup.getTag());
              output.append(", ");
              output.append(hostnames);
              output.append(" ), ");
            }
          }
        }
      }
    }
    if (!MapUtils.isEmpty(nonMappedHostIds) && warnIfFound) {
      output.replace(output.lastIndexOf(","), output.length(), "]");
      warning("You have config group host mappings with hosts that are no " +
        "longer associated with the cluster, {}. Run --auto-fix-database to " +
        "fix this automatically. Alternatively, you can remove this mapping " +
        "from the UI. Please backup Ambari Server database before running --auto-fix-database.", output.toString());
    }

    return nonMappedHostIds;
  }

  static Map<Long, ConfigGroup> checkConfigGroupsForDeletedServices(boolean warnIfFound) {
    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = clusters.getClusters();
    StringBuilder output = new StringBuilder("[(ConfigGroup, Service) => ");

    if (!MapUtils.isEmpty(clusterMap)) {
      for (Cluster cluster : clusterMap.values()) {
        Map<Long, ConfigGroup> configGroups = cluster.getConfigGroups();
        Map<String, Service> services = cluster.getServices();

        if (!MapUtils.isEmpty(configGroups)) {
          for (ConfigGroup configGroup : configGroups.values()) {
            if (!services.containsKey(configGroup.getServiceName())) {
              configGroupMap.put(configGroup.getId(), configGroup);
              output.append("( ");
              output.append(configGroup.getName());
              output.append(", ");
              output.append(configGroup.getServiceName());
              output.append(" ), ");
            }
          }
        }
      }
    }

    if (warnIfFound && !configGroupMap.isEmpty()) {
      output.replace(output.lastIndexOf(","), output.length(), "]");
      warning("You have config groups present in the database with no " +
        "corresponding service found, {}. Run --auto-fix-database to fix " +
          "this automatically. Please backup Ambari Server database before running --auto-fix-database.", output.toString());
    }

    return configGroupMap;
  }

  @Transactional
  static void fixConfigGroupsForDeletedServices() {
    Map<Long, ConfigGroup> configGroupMap = checkConfigGroupsForDeletedServices(false);
    Clusters clusters = injector.getInstance(Clusters.class);

    if (!MapUtils.isEmpty(configGroupMap)) {
      for (Map.Entry<Long, ConfigGroup> configGroupEntry : configGroupMap.entrySet()) {
        Long id = configGroupEntry.getKey();
        ConfigGroup configGroup = configGroupEntry.getValue();
        if (!StringUtils.isEmpty(configGroup.getServiceName())) {
          LOG.info("Deleting config group {} with id {} for deleted service {}",
                  configGroup.getName(), id, configGroup.getServiceName());
          try {
            Cluster cluster = clusters.getCluster(configGroup.getClusterName());
            cluster.deleteConfigGroup(id);
          } catch (AmbariException e) {
            // Ignore if cluster not found
          }
        }
        else {
          warning("The config group {} with id {} can not be fixed automatically because service name is missing.",
                  configGroup.getName(), id);
        }
      }
    }
  }

  @Transactional
  static void fixAlertsForDeletedServices() {

    Configuration conf = injector.getInstance(Configuration.class);

    LOG.info("fixAlertsForDeletedServices stale alert definitions for deleted services");

    ensureConnection();

    String SELECT_STALE_ALERT_DEFINITIONS = "select definition_id from alert_definition where service_name not in " +
            "(select service_name from clusterservices) and service_name not in ('AMBARI')";

    int recordsCount = 0;
    Statement statement = null;
    ResultSet rs = null;
    List<Integer> alertIds = new ArrayList<Integer>();
    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(SELECT_STALE_ALERT_DEFINITIONS);
      while (rs.next()) {
        alertIds.add(rs.getInt("definition_id"));
      }

    } catch (SQLException e) {
      warning("Exception occurred during fixing stale alert definitions: ", e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during fixing stale alert definitions: ", e);
        }
      }
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during fixing stale alert definitions: ", e);
        }
      }
    }

    for(Integer alertId : alertIds) {
      final AlertDefinitionEntity entity = alertDefinitionDAO.findById(alertId.intValue());
      alertDefinitionDAO.remove(entity);
    }
    warning("fixAlertsForDeletedServices - {}  Stale alerts were deleted", alertIds.size());

  }
  /**
   * Fix inconsistencies found by @checkConfigGroupHostMapping
   */
  @Transactional
  static void fixConfigGroupHostMappings() {
    Map<Long, Set<Long>> nonMappedHostIds = checkConfigGroupHostMapping(false);
    Clusters clusters = injector.getInstance(Clusters.class);

    if (!MapUtils.isEmpty(nonMappedHostIds)) {
      LOG.info("Fixing {} config groups with inconsistent host mappings", nonMappedHostIds.size());

      for (Map.Entry<Long, Set<Long>> nonMappedHostEntry : nonMappedHostIds.entrySet()) {
        if (!MapUtils.isEmpty(clusters.getClusters())) {
          for (Cluster cluster : clusters.getClusters().values()) {
            Map<Long, ConfigGroup> configGroups = cluster.getConfigGroups();
            if (!MapUtils.isEmpty(configGroups)) {
              ConfigGroup configGroup = configGroups.get(nonMappedHostEntry.getKey());
              if (configGroup != null) {
                for (Long hostId : nonMappedHostEntry.getValue()) {
                  try {
                    configGroup.removeHost(hostId);
                  } catch (AmbariException e) {
                    LOG.warn("Unable to fix inconsistency by removing host " +
                      "mapping for config group: {}, service: {}, hostId = {}",
                      configGroup.getName(), configGroup.getTag(), hostId);
                  }
                }
              } else {
                LOG.warn("Unable to find config group with id = {}", nonMappedHostEntry.getKey());
              }
            }
          }
        }
      }
    }
  }

  private static void ensureConnection() {
    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }
  }

  private static void removeStringsByRegexp(Collection<String> stringItems, String regexp) {
      Pattern pattern = Pattern.compile(regexp);

      for (Iterator<String> iterator = stringItems.iterator(); iterator.hasNext();) {
        String stringItem = iterator.next();
        Matcher matcher = pattern.matcher(stringItem);
        if (matcher.find()) {
          iterator.remove();
        }
      }
  }
}
