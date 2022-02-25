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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptor;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * The {@link org.apache.ambari.server.upgrade.UpgradeCatalog260} upgrades Ambari from 2.5.2 to 2.6.0.
 */
public class UpgradeCatalog260 extends AbstractUpgradeCatalog {

  public static final String CLUSTER_CONFIG_MAPPING_TABLE = "clusterconfigmapping";
  public static final String CLUSTER_VERSION_TABLE = "cluster_version";
  public static final String CLUSTER_ID_COLUMN = "cluster_id";
  public static final String STATE_COLUMN = "state";
  public static final String CREATE_TIMESTAMP_COLUMN = "create_timestamp";
  public static final String VERSION_TAG_COLUMN = "version_tag";
  public static final String TYPE_NAME_COLUMN = "type_name";

  public static final String CLUSTER_CONFIG_TABLE = "clusterconfig";
  public static final String SELECTED_COLUMN = "selected";
  public static final String SERVICE_DELETED_COLUMN = "service_deleted";
  public static final String UNMAPPED_COLUMN = "unmapped";
  public static final String SELECTED_TIMESTAMP_COLUMN = "selected_timestamp";

  public static final String SERVICE_COMPONENT_DESIRED_STATE_TABLE = "servicecomponentdesiredstate";
  public static final String DESIRED_STACK_ID_COLUMN = "desired_stack_id";
  public static final String DESIRED_VERSION_COLUMN = "desired_version";
  public static final String DESIRED_REPO_VERSION_ID_COLUMN = "desired_repo_version_id";
  public static final String REPO_STATE_COLUMN = "repo_state";
  public static final String FK_SCDS_DESIRED_STACK_ID = "FK_scds_desired_stack_id";
  public static final String FK_SERVICECOMPONENTDESIREDSTATE_DESIRED_STACK_ID = "FK_servicecomponentdesiredstate_desired_stack_id";
  public static final String FK_SCDS_DESIRED_REPO_ID = "FK_scds_desired_repo_id";

  public static final String REPO_VERSION_TABLE = "repo_version";
  public static final String REPO_VERSION_ID_COLUMN = "repo_version_id";
  public static final String REPO_VERSION_RESOLVED_COLUMN = "resolved";
  public static final String REPO_VERSION_HIDDEN_COLUMN = "hidden";
  public static final String REPO_VERSION_LEGACY_COLUMN = "legacy";

  public static final String HOST_COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  public static final String FK_HCDS_DESIRED_STACK_ID = "FK_hcds_desired_stack_id";

  public static final String HOST_COMPONENT_STATE_TABLE = "hostcomponentstate";
  public static final String CURRENT_STACK_ID_COLUMN = "current_stack_id";
  public static final String FK_HCS_CURRENT_STACK_ID = "FK_hcs_current_stack_id";

  public static final String HOST_VERSION_TABLE = "host_version";
  public static final String UQ_HOST_REPO = "UQ_host_repo";
  public static final String HOST_ID_COLUMN = "host_id";

  public static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
  public static final String FK_SDS_DESIRED_STACK_ID = "FK_sds_desired_stack_id";
  public static final String FK_REPO_VERSION_ID = "FK_repo_version_id";

  public static final String CLUSTERS_TABLE = "clusters";

  public static final String UPGRADE_TABLE = "upgrade";
  public static final String UPGRADE_GROUP_TABLE = "upgrade_group";
  public static final String UPGRADE_ITEM_TABLE = "upgrade_item";
  public static final String FROM_REPO_VERSION_ID_COLUMN = "from_repo_version_id";
  public static final String TO_REPO_VERSION_ID_COLUMN = "to_repo_version_id";
  public static final String ORCHESTRATION_COLUMN = "orchestration";
  public static final String ALLOW_REVERT_COLUMN = "revert_allowed";
  public static final String FK_UPGRADE_FROM_REPO_ID = "FK_upgrade_from_repo_id";
  public static final String FK_UPGRADE_TO_REPO_ID = "FK_upgrade_to_repo_id";
  public static final String FK_UPGRADE_REPO_VERSION_ID = "FK_upgrade_repo_version_id";
  public static final String UPGRADE_ITEM_ITEM_TEXT = "item_text";

  public static final String SERVICE_COMPONENT_HISTORY_TABLE = "servicecomponent_history";
  public static final String UPGRADE_HISTORY_TABLE = "upgrade_history";
  public static final String ID_COLUMN = "id";
  public static final String UPGRADE_ID_COLUMN = "upgrade_id";
  public static final String SERVICE_NAME_COLUMN = "service_name";
  public static final String COMPONENT_NAME_COLUMN = "component_name";
  public static final String TARGET_REPO_VERSION_ID_COLUMN = "target_repo_version_id";
  public static final String PK_UPGRADE_HIST = "PK_upgrade_hist";
  public static final String FK_UPGRADE_HIST_UPGRADE_ID = "FK_upgrade_hist_upgrade_id";
  public static final String FK_UPGRADE_HIST_FROM_REPO = "FK_upgrade_hist_from_repo";
  public static final String FK_UPGRADE_HIST_TARGET_REPO = "FK_upgrade_hist_target_repo";
  public static final String UQ_UPGRADE_HIST = "UQ_upgrade_hist";
  public static final String SERVICE_CONFIG_MAPPING_TABLE = "serviceconfigmapping";
  public static final String SERVICE_COMPONENT_DESIRED_STATE = "servicecomponentdesiredstate";
  public static final String HOST_COMPONENT_DESIRED_STATE = "hostcomponentdesiredstate";
  public static final String HOST_COMPONENT_STATE = "hostcomponentstate";

  private static final String CORE_SITE = "core-site";
  public static final String AMS_SSL_CLIENT = "ams-ssl-client";
  public static final String METRIC_TRUSTSTORE_ALIAS = "ssl.client.truststore.alias";

  private static final String HIVE_INTERACTIVE_SITE = "hive-interactive-site";
  public static final String HIVE_LLAP_DAEMON_KEYTAB_FILE = "hive.llap.daemon.keytab.file";
  public static final String HIVE_LLAP_ZK_SM_KEYTAB_FILE = "hive.llap.zk.sm.keytab.file";
  public static final String HIVE_LLAP_TASK_KEYTAB_FILE = "hive.llap.task.keytab.file";
  public static final String HIVE_SERVER_KERBEROS_PREFIX = "/HIVE/HIVE_SERVER/";
  public static final String YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY = "llap_zk_hive";
  public static final String YARN_LLAP_TASK_HIVE_KERBEROS_IDENTITY = "llap_task_hive";
  public static final String HIVE_SERVER_HIVE_KERBEROS_IDENTITY = "hive_server_hive";

  // Used to track whether YARN -> NODEMANAGER -> 'llap_zk_hive' kerberos descriptor was updated or not.
  private List<String> yarnKerberosDescUpdatedList = new ArrayList<>();


  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog260.class);
  public static final String STANDARD = "STANDARD";
  public static final String NOT_REQUIRED = "NOT_REQUIRED";
  public static final String CURRENT = "CURRENT";
  public static final String SELECTED = "1";
  public static final String VIEWURL_TABLE = "viewurl";
  public static final String VIEWINSTANCE_TABLE = "viewinstance";
  public static final String PK_VIEWURL = "PK_viewurl";
  public static final String URL_ID_COLUMN = "url_id";
  public static final String STALE_POSTGRESS_VIEWURL_PKEY = "viewurl_pkey";
  public static final String USERS_TABLE = "users";
  public static final String STALE_POSTGRESS_USERS_LDAP_USER_KEY = "users_ldap_user_key";
  public static final String SHORT_URL_COLUMN = "short_url";
  public static final String FK_INSTANCE_URL_ID = "FK_instance_url_id";
  public static final String FK_SERVICEDESIREDSTATE_DESIRED_STACK_ID = "FK_servicedesiredstate_desired_stack_id";
  public static final String FK_HOSTCOMPONENTDESIREDSTATE_DESIRED_STACK_ID = "FK_hostcomponentdesiredstate_desired_stack_id";
  public static final String FK_HOSTCOMPONENTSTATE_CURRENT_STACK_ID = "FK_hostcomponentstate_current_stack_id";
  public static final String FK_UPGRADE_FROM_REPO_VERSION_ID = "FK_upgrade_from_repo_version_id";
  public static final String FK_UPGRADE_TO_REPO_VERSION_ID = "FK_upgrade_to_repo_version_id";


  /**
   * Constructor.
   *
   * @param injector
   */
  @Inject
  public UpgradeCatalog260(Injector injector) {
    super(injector);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion()
  {
    return "2.5.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.6.0";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    Integer currentVersionID = getCurrentVersionID();
    dropBrokenFK();
    updateServiceComponentDesiredStateTable(currentVersionID);
    updateServiceDesiredStateTable(currentVersionID);
    addSelectedCollumsToClusterconfigTable();
    updateHostComponentDesiredStateTable();
    updateHostComponentStateTable();
    dropStaleTables();
    updateUpgradeTable();
    createUpgradeHistoryTable();
    updateRepositoryVersionTable();
    renameServiceDeletedColumn();
    addLegacyColumn();
    expandUpgradeItemItemTextColumn();
    addViewUrlPKConstraint();
    removeStaleConstraints();
  }

  /**
   * Drop broken FK
   * {@value #FK_SERVICECOMPONENTDESIREDSTATE_DESIRED_STACK_ID}
   * {@value #FK_SERVICEDESIREDSTATE_DESIRED_STACK_ID}
   * {@value #FK_HOSTCOMPONENTDESIREDSTATE_DESIRED_STACK_ID}
   * {@value #FK_HOSTCOMPONENTSTATE_CURRENT_STACK_ID}
   * {@value #FK_UPGRADE_FROM_REPO_VERSION_ID}
   * {@value #FK_UPGRADE_TO_REPO_VERSION_ID}
   */
  private void dropBrokenFK() throws SQLException {
    dbAccessor.dropFKConstraint(UPGRADE_TABLE, FK_UPGRADE_FROM_REPO_VERSION_ID);
    dbAccessor.dropFKConstraint(UPGRADE_TABLE, FK_UPGRADE_TO_REPO_VERSION_ID);
    dbAccessor.dropFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SERVICECOMPONENTDESIREDSTATE_DESIRED_STACK_ID);
    dbAccessor.dropFKConstraint(SERVICE_DESIRED_STATE_TABLE, FK_SERVICEDESIREDSTATE_DESIRED_STACK_ID);
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, FK_HOSTCOMPONENTDESIREDSTATE_DESIRED_STACK_ID);
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, FK_HOSTCOMPONENTSTATE_CURRENT_STACK_ID);
  }


  /**
   * Updates {@value #VIEWURL_TABLE} table.
   * Adds the {@value #PK_VIEWURL} constraint.
   */
  private void addViewUrlPKConstraint() throws SQLException {
    dbAccessor.dropFKConstraint(VIEWINSTANCE_TABLE, FK_INSTANCE_URL_ID);
    dbAccessor.dropPKConstraint(VIEWURL_TABLE, STALE_POSTGRESS_VIEWURL_PKEY);
    dbAccessor.addPKConstraint(VIEWURL_TABLE, PK_VIEWURL, URL_ID_COLUMN);
    dbAccessor.addFKConstraint(VIEWINSTANCE_TABLE, FK_INSTANCE_URL_ID,
        SHORT_URL_COLUMN, VIEWURL_TABLE, URL_ID_COLUMN, false);
  }

  /**
   * remove stale unnamed constraints
   */
  private void removeStaleConstraints() throws SQLException {
    dbAccessor.dropUniqueConstraint(USERS_TABLE, STALE_POSTGRESS_USERS_LDAP_USER_KEY);
  }

  /**
   * Expand item_text column of upgrade_item
   */
  private void expandUpgradeItemItemTextColumn() throws SQLException {
    dbAccessor.changeColumnType(UPGRADE_ITEM_TABLE, UPGRADE_ITEM_ITEM_TEXT,
      String.class, char[].class);
  }

  private void addLegacyColumn() throws AmbariException, SQLException {
    Boolean isLegacyColumnExists = dbAccessor.tableHasColumn(REPO_VERSION_TABLE, REPO_VERSION_LEGACY_COLUMN);
    if (!isLegacyColumnExists) {
      DBAccessor.DBColumnInfo legacyColumn = new DBAccessor.DBColumnInfo(REPO_VERSION_LEGACY_COLUMN, Short.class, null, 1, false);
      dbAccessor.addColumn(REPO_VERSION_TABLE, legacyColumn);

      legacyColumn.setDefaultValue(0);
      dbAccessor.alterColumn(REPO_VERSION_TABLE, legacyColumn);
    }
  }

  private void renameServiceDeletedColumn() throws AmbariException, SQLException {
    if (dbAccessor.tableHasColumn(CLUSTER_CONFIG_TABLE, SERVICE_DELETED_COLUMN)) {
      dbAccessor.renameColumn(CLUSTER_CONFIG_TABLE, SERVICE_DELETED_COLUMN, new DBAccessor.DBColumnInfo(UNMAPPED_COLUMN, Short.class, null, 0, false));
    }
  }

  /*
  * This method, search for configs which are not linked with any service
  * and set "unmapped" to true. We need that because in case when
  * config is not mapped and "unmapped" flag = false, db consistency check
  * will show warning about not mapped config. (AMBARI-21795)
  * */
  private void setUnmappedForOrphanedConfigs() {
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        EntityManager entityManager = getEntityManagerProvider().get();
        Query query = entityManager.createNamedQuery("ClusterConfigEntity.findNotMappedClusterConfigsToService",ClusterConfigEntity.class);

        List<ClusterConfigEntity> notMappedConfigs =  query.getResultList();
        if (notMappedConfigs != null) {
          for (ClusterConfigEntity clusterConfigEntity : notMappedConfigs) {
            clusterConfigEntity.setUnmapped(true);
            entityManager.merge(clusterConfigEntity);
          }
        }
      }
    });
  }

  private void createUpgradeHistoryTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();

    columns.add(new DBAccessor.DBColumnInfo(ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(UPGRADE_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(SERVICE_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(COMPONENT_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(FROM_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(TARGET_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(UPGRADE_HISTORY_TABLE, columns);

    dbAccessor.addPKConstraint(UPGRADE_HISTORY_TABLE, PK_UPGRADE_HIST, ID_COLUMN);

    dbAccessor.addFKConstraint(UPGRADE_HISTORY_TABLE, FK_UPGRADE_HIST_UPGRADE_ID, UPGRADE_ID_COLUMN, UPGRADE_TABLE, UPGRADE_ID_COLUMN, false);
    dbAccessor.addFKConstraint(UPGRADE_HISTORY_TABLE, FK_UPGRADE_HIST_FROM_REPO, FROM_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
    dbAccessor.addFKConstraint(UPGRADE_HISTORY_TABLE, FK_UPGRADE_HIST_TARGET_REPO, TARGET_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
    dbAccessor.addUniqueConstraint(UPGRADE_HISTORY_TABLE, UQ_UPGRADE_HIST, UPGRADE_ID_COLUMN, COMPONENT_NAME_COLUMN, SERVICE_NAME_COLUMN);

    addSequence("upgrade_history_id_seq", 0L, false);
  }

  /**
   * Updates {@value #UPGRADE_TABLE} table.
   * clear {@value #UPGRADE_TABLE} table
   * Removes {@value #FROM_REPO_VERSION_ID_COLUMN} column.
   * Removes {@value #TO_REPO_VERSION_ID_COLUMN} column.
   * Adds the {@value #ORCHESTRATION_COLUMN} column.
   * Adds the {@value #REPO_VERSION_ID_COLUMN} column.
   * Removes {@value #FK_UPGRADE_FROM_REPO_ID} foreign key.
   * Removes {@value #FK_UPGRADE_TO_REPO_ID} foreign key.
   * adds {@value #FK_REPO_VERSION_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateUpgradeTable() throws SQLException {
    dbAccessor.clearTableColumn(CLUSTERS_TABLE, UPGRADE_ID_COLUMN, null);

    dbAccessor.clearTable(UPGRADE_ITEM_TABLE);
    dbAccessor.clearTable(UPGRADE_GROUP_TABLE);
    dbAccessor.clearTable(UPGRADE_TABLE);
    dbAccessor.dropFKConstraint(UPGRADE_TABLE, FK_UPGRADE_FROM_REPO_ID);
    dbAccessor.dropFKConstraint(UPGRADE_TABLE, FK_UPGRADE_TO_REPO_ID);
    dbAccessor.dropColumn(UPGRADE_TABLE, FROM_REPO_VERSION_ID_COLUMN);
    dbAccessor.dropColumn(UPGRADE_TABLE, TO_REPO_VERSION_ID_COLUMN);

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(REPO_VERSION_ID_COLUMN, Long.class, null, null, false));

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(ORCHESTRATION_COLUMN, String.class, 255, STANDARD, false));

    dbAccessor.addColumn(UPGRADE_TABLE,
        new DBAccessor.DBColumnInfo(ALLOW_REVERT_COLUMN, Short.class, null, 0, false));

    dbAccessor.addFKConstraint(UPGRADE_TABLE, FK_UPGRADE_REPO_VERSION_ID, REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
  }

  /**
   * Updates {@value #SERVICE_DESIRED_STATE_TABLE} table.
   * Removes {@value #DESIRED_STACK_ID_COLUMN} column.
   * Adds the {@value #DESIRED_REPO_VERSION_ID_COLUMN} column.
   * Removes {@value #FK_SDS_DESIRED_STACK_ID} foreign key.
   * adds {@value #FK_REPO_VERSION_ID} foreign key.
   *
   * @param currentRepoID id of current repo_version. Can be null if there are no cluster repo versions
   *                      (in this case {@value #SERVICE_DESIRED_STATE_TABLE} table must be empty)
   *
   * @throws java.sql.SQLException
   */
  private void updateServiceDesiredStateTable(Integer currentRepoID) throws SQLException {
    //in case if currentRepoID is null {@value #SERVICE_DESIRED_STATE_TABLE} table must be empty and null defaultValue is ok for non-nullable column
    dbAccessor.addColumn(SERVICE_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, currentRepoID, false));
    dbAccessor.alterColumn(SERVICE_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));

    dbAccessor.addFKConstraint(SERVICE_DESIRED_STATE_TABLE, FK_REPO_VERSION_ID, DESIRED_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);
    dbAccessor.dropFKConstraint(SERVICE_DESIRED_STATE_TABLE, FK_SDS_DESIRED_STACK_ID);
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
  }

  /**
   * drop {@value #CLUSTER_CONFIG_MAPPING_TABLE} and {@value #CLUSTER_VERSION_TABLE} tables.
   *
   * @throws java.sql.SQLException
   */
  private void dropStaleTables() throws SQLException {
    dbAccessor.dropTable(CLUSTER_CONFIG_MAPPING_TABLE);
    dbAccessor.dropTable(CLUSTER_VERSION_TABLE);
    dbAccessor.dropTable(SERVICE_COMPONENT_HISTORY_TABLE);
  }

  /**
   * Adds the {@value #SELECTED_COLUMN} and {@value #SELECTED_TIMESTAMP_COLUMN} columns to the
   * {@value #CLUSTER_CONFIG_TABLE} table.
   *
   * @throws java.sql.SQLException
   */
  private void addSelectedCollumsToClusterconfigTable() throws SQLException {
    DBAccessor.DBColumnInfo selectedColumnInfo = new DBAccessor.DBColumnInfo(SELECTED_COLUMN, Short.class, null, 0, false);
    DBAccessor.DBColumnInfo selectedmappingColumnInfo = new DBAccessor.DBColumnInfo(SELECTED_COLUMN, Integer.class, null, 0, false);
    DBAccessor.DBColumnInfo selectedTimestampColumnInfo = new DBAccessor.DBColumnInfo(SELECTED_TIMESTAMP_COLUMN, Long.class, null, 0, false);
    DBAccessor.DBColumnInfo createTimestampColumnInfo = new DBAccessor.DBColumnInfo(CREATE_TIMESTAMP_COLUMN, Long.class, null, null, false);
    dbAccessor.copyColumnToAnotherTable(CLUSTER_CONFIG_MAPPING_TABLE, selectedmappingColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, CLUSTER_CONFIG_TABLE, selectedColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, SELECTED_COLUMN, SELECTED, 0);

    dbAccessor.copyColumnToAnotherTable(CLUSTER_CONFIG_MAPPING_TABLE, createTimestampColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, CLUSTER_CONFIG_TABLE, selectedTimestampColumnInfo,
        CLUSTER_ID_COLUMN, TYPE_NAME_COLUMN, VERSION_TAG_COLUMN, SELECTED_COLUMN, SELECTED, 0);
  }


  /**
   * Updates {@value #SERVICE_COMPONENT_DESIRED_STATE_TABLE} table.
   * Removes {@value #DESIRED_VERSION_COLUMN},{@value #DESIRED_STACK_ID_COLUMN} columns.
   * Adds the {@value #DESIRED_REPO_VERSION_ID_COLUMN},{@value #REPO_STATE_COLUMN} columns.
   * Removes {@value #FK_SCDS_DESIRED_STACK_ID} foreign key.
   * adds {@value #FK_SCDS_DESIRED_REPO_ID} foreign key.
   *
   * @param currentRepoID id of current repo_version. Can be null if there are no cluster repo versions
   *                      (in this case {@value #SERVICE_DESIRED_STATE_TABLE} table must be empty)
   *
   * @throws java.sql.SQLException
   */
  private void updateServiceComponentDesiredStateTable(Integer currentRepoID) throws SQLException {
    //in case if currentRepoID is null {@value #SERVICE_DESIRED_STATE_TABLE} table must be empty and null defaultValue is ok for non-nullable column
    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, currentRepoID, false));
    dbAccessor.alterColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(DESIRED_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));

    dbAccessor.addColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(REPO_STATE_COLUMN, String.class, 255, CURRENT, false));
    dbAccessor.alterColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE,
        new DBAccessor.DBColumnInfo(REPO_STATE_COLUMN, String.class, 255, NOT_REQUIRED, false));

    dbAccessor.addFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SCDS_DESIRED_REPO_ID, DESIRED_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_ID_COLUMN, false);

    dbAccessor.dropFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SCDS_DESIRED_STACK_ID);
    dbAccessor.dropColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
    dbAccessor.dropColumn(SERVICE_COMPONENT_DESIRED_STATE_TABLE, DESIRED_VERSION_COLUMN);
  }

  /**
   * Updates {@value #HOST_COMPONENT_DESIRED_STATE_TABLE} table.
   * Removes {@value #DESIRED_STACK_ID_COLUMN} column.
   * Removes {@value #FK_HCDS_DESIRED_STACK_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateHostComponentDesiredStateTable() throws SQLException {
    dbAccessor.dropFKConstraint(HOST_COMPONENT_DESIRED_STATE_TABLE, FK_HCDS_DESIRED_STACK_ID);
    dbAccessor.dropColumn(HOST_COMPONENT_DESIRED_STATE_TABLE, DESIRED_STACK_ID_COLUMN);
  }

  /**
   * Updates {@value #HOST_COMPONENT_STATE_TABLE} table.
   * Removes {@value #CURRENT_STACK_ID_COLUMN} column.
   * Removes {@value #FK_HCS_CURRENT_STACK_ID} foreign key.
   *
   * @throws java.sql.SQLException
   */
  private void updateHostComponentStateTable() throws SQLException {
    dbAccessor.dropFKConstraint(HOST_COMPONENT_STATE_TABLE, FK_HCS_CURRENT_STACK_ID);
    dbAccessor.dropColumn(HOST_COMPONENT_STATE_TABLE, CURRENT_STACK_ID_COLUMN);
  }

  /**
   * Updates {@value #REPO_VERSION_TABLE} table. Adds the following columns:
   * <ul>
   * <li>{@value #REPO_VERSION_HIDDEN_COLUMN}
   * <li>{@value #REPO_VERSION_RESOLVED_COLUMN}
   * </ul>
   *
   * @throws java.sql.SQLException
   */
  private void updateRepositoryVersionTable() throws SQLException {
    dbAccessor.addColumn(REPO_VERSION_TABLE,
        new DBAccessor.DBColumnInfo(REPO_VERSION_HIDDEN_COLUMN, Short.class, null, 0, false));

    dbAccessor.addColumn(REPO_VERSION_TABLE,
        new DBAccessor.DBColumnInfo(REPO_VERSION_RESOLVED_COLUMN, Short.class, null, 0, false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    removeSupersetFromDruid();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    setUnmappedForOrphanedConfigs();
    ensureZeppelinProxyUserConfigs();
    updateKerberosDescriptorArtifacts();
    updateAmsConfigs();
    updateHiveConfigs();
    updateHDFSWidgetDefinition();
    updateExistingRepositoriesToBeResolved();
  }

  /**
   * get {@value #REPO_VERSION_ID_COLUMN} value from {@value #CLUSTER_VERSION_TABLE}
   * where {@value #STATE_COLUMN} = {@value #CURRENT}
   * and validate it
   *
   * @return current version ID or null if no cluster versions do exist
   * @throws AmbariException if cluster versions are present, but current is not selected
   * @throws SQLException
   */
  public Integer getCurrentVersionID() throws AmbariException, SQLException {
    if (!dbAccessor.tableExists(CLUSTER_VERSION_TABLE)) {
      return null;
    }
    List<Integer> currentVersionList = dbAccessor.getIntColumnValues(CLUSTER_VERSION_TABLE, REPO_VERSION_ID_COLUMN,
        new String[]{STATE_COLUMN}, new String[]{CURRENT}, false);
    if (currentVersionList.isEmpty()) {
      List<Integer> allVersionList = dbAccessor.getIntColumnValues(CLUSTER_VERSION_TABLE, REPO_VERSION_ID_COLUMN, null, null,false);
      if (allVersionList.isEmpty()){
        return null;
      } else {
        throw new AmbariException("Unable to find any CURRENT repositories.");
      }
    } else if (currentVersionList.size() != 1) {
      throw new AmbariException("The following repositories were found to be CURRENT: ".concat(StringUtils.join(currentVersionList, ",")));
    }
    return currentVersionList.get(0);
  }

  // Superset is moved as an independent service in Ambari-2.6.
  // This will remove superset component if installed under Druid Service
  protected void removeSupersetFromDruid() throws SQLException {
    removeComponent("DRUID_SUPERSET", "druid-superset");
  }

  private void removeComponent(String componentName, String configPrefix) throws SQLException {

    String serviceConfigMappingRemoveSQL = String.format(
        "DELETE FROM %s WHERE config_id IN (SELECT config_id from %s where type_name like '%s%%')",
        SERVICE_CONFIG_MAPPING_TABLE, CLUSTER_CONFIG_TABLE, configPrefix);

    String supersetConfigRemoveSQL = String.format(
        "DELETE FROM %s WHERE type_name like '%s%%'",
        CLUSTER_CONFIG_TABLE, configPrefix);

    String hostComponentDesiredStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        HOST_COMPONENT_DESIRED_STATE, componentName);

    String hostComponentStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        HOST_COMPONENT_STATE, componentName);

    String serviceComponentDesiredStateRemoveSQL = String.format(
        "DELETE FROM %s WHERE component_name = '%s'",
        SERVICE_COMPONENT_DESIRED_STATE, componentName);

    dbAccessor.executeQuery(serviceConfigMappingRemoveSQL);
    dbAccessor.executeQuery(supersetConfigRemoveSQL);
    dbAccessor.executeQuery(hostComponentDesiredStateRemoveSQL);
    dbAccessor.executeQuery(hostComponentStateRemoveSQL);
    dbAccessor.executeQuery(serviceComponentDesiredStateRemoveSQL);
  }

  /**
   * If Zeppelin is installed, ensure that the proxyuser configurations are set in <code>core-site</code>.
   * <p>
   * The following configurations will be added, if core-site exists and the properties are not in the
   * set of core-site properties:
   * <ul>
   * <li><code>"hadoop.proxyuser.{zeppelin-env/zeppelin_user}.groups": "*"</code></li>
   * <li><code>"hadoop.proxyuser.{zeppelin-env/zeppelin_user}.hosts": "*"</code></li>
   * </ul>
   */
  void ensureZeppelinProxyUserConfigs() throws AmbariException {
    Clusters clusters = injector.getInstance(Clusters.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);

    if ((clusterMap != null) && !clusterMap.isEmpty()) {
      for (final Cluster cluster : clusterMap.values()) {
        Config zeppelinEnvConfig = cluster.getDesiredConfigByType("zeppelin-env");

        if (zeppelinEnvConfig != null) {
          // If zeppelin-env exists, than it is assumed that Zeppelin is installed
          Map<String, String> zeppelinEnvProperties = zeppelinEnvConfig.getProperties();

          String zeppelinUser = null;
          if (zeppelinEnvProperties != null) {
            zeppelinUser = zeppelinEnvProperties.get("zeppelin_user");
          }

          if (!StringUtils.isEmpty(zeppelinUser)) {
            // If the zeppelin user is set, see if the proxyuser configs need to be set

            Config coreSiteConfig = cluster.getDesiredConfigByType(CORE_SITE);
            if (coreSiteConfig != null) {
              // If core-site exists, ensure the proxyuser configurations for Zeppelin are set.
              // If they are not already set, set them to their default value.
              String proxyUserHostsName = String.format("hadoop.proxyuser.%s.hosts", zeppelinUser);
              String proxyUserGroupsName = String.format("hadoop.proxyuser.%s.groups", zeppelinUser);

              Map<String, String> proxyUserProperties = new HashMap<>();
              proxyUserProperties.put(proxyUserHostsName, "*");
              proxyUserProperties.put(proxyUserGroupsName, "*");

              Map<String, String> coreSiteConfigProperties = coreSiteConfig.getProperties();

              if (coreSiteConfigProperties != null) {
                if (coreSiteConfigProperties.containsKey(proxyUserHostsName)) {
                  proxyUserProperties.remove(proxyUserHostsName);
                }

                if (coreSiteConfigProperties.containsKey(proxyUserGroupsName)) {
                  proxyUserProperties.remove(proxyUserGroupsName);
                }
              }

              if (!proxyUserProperties.isEmpty()) {
                updateConfigurationPropertiesForCluster(cluster, CORE_SITE, proxyUserProperties, true, false);
              }
            }
          }
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    if (artifactEntity != null) {
      Map<String, Object> data = artifactEntity.getArtifactData();
      if (data != null) {
        final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);
        if (kerberosDescriptor != null) {
          fixRangerKMSKerberosDescriptor(kerberosDescriptor);
          fixIdentityReferences(getCluster(artifactEntity), kerberosDescriptor);
          fixYarnHsiKerberosDescriptorAndSiteConfig(getCluster(artifactEntity), kerberosDescriptor);

          artifactEntity.setArtifactData(kerberosDescriptor.toMap());
          artifactDAO.merge(artifactEntity);
        }
      }
    }
  }

  protected void fixRangerKMSKerberosDescriptor(KerberosDescriptor kerberosDescriptor) {
    KerberosServiceDescriptor rangerKmsServiceDescriptor = kerberosDescriptor.getService("RANGER_KMS");
    if (rangerKmsServiceDescriptor != null) {

      KerberosIdentityDescriptor rangerKmsServiceIdentity = rangerKmsServiceDescriptor.getIdentity("/smokeuser");
      if (rangerKmsServiceIdentity != null) {
        rangerKmsServiceDescriptor.removeIdentity("/smokeuser");
      }
      KerberosComponentDescriptor rangerKmscomponentDescriptor = rangerKmsServiceDescriptor.getComponent("RANGER_KMS_SERVER");
      if (rangerKmscomponentDescriptor != null) {
        KerberosIdentityDescriptor rangerKmsComponentIdentity = rangerKmscomponentDescriptor.getIdentity("/smokeuser");
        if (rangerKmsComponentIdentity != null) {
          rangerKmscomponentDescriptor.removeIdentity("/smokeuser");
        }
      }
    }
  }

  /**
   * Updates YARN's NM 'llap_zk_hive' kerberos descriptor as reference and the associated config
   * hive-interactive-site/hive.llap.zk.sm.keytab.file
   */
  protected void fixYarnHsiKerberosDescriptorAndSiteConfig(Cluster cluster, KerberosDescriptor kerberosDescriptor) {
    LOG.info("Updating YARN's HSI Kerberos Descriptor ....");

    // Step 1. Get Hive -> HIVE_SERVER -> 'hive_server_hive' kerberos description for referencing later
    KerberosServiceDescriptor hiveServiceDescriptor = kerberosDescriptor.getService("HIVE");
    KerberosIdentityDescriptor hsh_identityDescriptor = null;
    KerberosPrincipalDescriptor hsh_principalDescriptor = null;
    KerberosKeytabDescriptor hsh_keytabDescriptor = null;
    if (hiveServiceDescriptor != null) {
      KerberosComponentDescriptor hiveServerKerberosDescriptor = hiveServiceDescriptor.getComponent("HIVE_SERVER");
      if (hiveServerKerberosDescriptor != null) {
        hsh_identityDescriptor = hiveServerKerberosDescriptor.getIdentity(HIVE_SERVER_HIVE_KERBEROS_IDENTITY);
        if (hsh_identityDescriptor != null) {
          LOG.info("  Retrieved HIVE->HIVE_SERVER kerberos descriptor. Name = " + hsh_identityDescriptor.getName());
          hsh_principalDescriptor = hsh_identityDescriptor.getPrincipalDescriptor();
          hsh_keytabDescriptor = hsh_identityDescriptor.getKeytabDescriptor();
        }
      }

      // Step 2. Update YARN -> NODEMANAGER's : (1). 'llap_zk_hive' and (2). 'llap_task_hive' kerberos descriptor as reference to
      // HIVE -> HIVE_SERVER -> 'hive_server_hive' (Same as YARN -> NODEMANAGER -> 'yarn_nodemanager_hive_server_hive')
      if (hsh_principalDescriptor != null && hsh_keytabDescriptor != null) {
        KerberosServiceDescriptor yarnServiceDescriptor = kerberosDescriptor.getService("YARN");
        if (yarnServiceDescriptor != null) {
          KerberosComponentDescriptor yarnNmKerberosDescriptor = yarnServiceDescriptor.getComponent("NODEMANAGER");
          if (yarnNmKerberosDescriptor != null) {
            String[] identities = {YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY, YARN_LLAP_TASK_HIVE_KERBEROS_IDENTITY};
            for (String identity : identities) {
              KerberosIdentityDescriptor identityDescriptor = yarnNmKerberosDescriptor.getIdentity(identity);

              KerberosPrincipalDescriptor principalDescriptor = null;
              KerberosKeytabDescriptor keytabDescriptor = null;
              if (identityDescriptor != null) {
                LOG.info("  Retrieved YARN->NODEMANAGER kerberos descriptor to be updated. Name = " + identityDescriptor.getName());
                principalDescriptor = identityDescriptor.getPrincipalDescriptor();
                keytabDescriptor = identityDescriptor.getKeytabDescriptor();

                identityDescriptor.setReference(HIVE_SERVER_KERBEROS_PREFIX + hsh_identityDescriptor.getName());
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' identity descriptor reference = '"
                        + identityDescriptor.getReference() + "'");
                principalDescriptor.setValue(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' principal descriptor value = '"
                        + principalDescriptor.getValue() + "'");

                // Updating keytabs now
                keytabDescriptor.setFile(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor file = '"
                        + keytabDescriptor.getFile() + "'");
                keytabDescriptor.setOwnerName(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor owner name = '" + keytabDescriptor.getOwnerName() + "'");
                keytabDescriptor.setOwnerAccess(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor owner access = '" + keytabDescriptor.getOwnerAccess() + "'");
                keytabDescriptor.setGroupName(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor group name = '" + keytabDescriptor.getGroupName() + "'");
                keytabDescriptor.setGroupAccess(null);
                LOG.info("    Updated '" + YARN_LLAP_ZK_HIVE_KERBEROS_IDENTITY + "' keytab descriptor group access = '" + keytabDescriptor.getGroupAccess() + "'");

                // Need this as trigger to update the HIVE_LLAP_ZK_SM_KEYTAB_FILE configs later.

                // Get the keytab file 'config name'.
                String[] splits = keytabDescriptor.getConfiguration().split("/");
                if (splits.length == 2) {
                  updateYarnKerberosDescUpdatedList(splits[1]);
                  LOG.info("    Updated 'yarnKerberosDescUpdatedList' = " + getYarnKerberosDescUpdatedList());
                }
              }
            }
          }
        }
      }
    }
  }

  public void updateYarnKerberosDescUpdatedList(String val) {
    yarnKerberosDescUpdatedList.add(val);
  }

  public List<String> getYarnKerberosDescUpdatedList() {
    return yarnKerberosDescUpdatedList;
  }

  protected void updateHiveConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          // Updating YARN->NodeManager kerebros descriptor : (1). 'llap_zk_hive' and (2). 'llap_task_hive''s associated configs
          // hive-interactive-site/hive.llap.zk.sm.keytab.file and hive-interactive-site/hive.llap.task.keytab.file respectively,
          // based on what hive-interactive-site/hive.llap.daemon.keytab.file has.
          Config hsiSiteConfig = cluster.getDesiredConfigByType(HIVE_INTERACTIVE_SITE);
          if (hsiSiteConfig != null) {
            Map<String, String> hsiSiteConfigProperties = hsiSiteConfig.getProperties();
            if (hsiSiteConfigProperties != null &&
                    hsiSiteConfigProperties.containsKey(HIVE_LLAP_DAEMON_KEYTAB_FILE)) {
              String[] identities = {HIVE_LLAP_ZK_SM_KEYTAB_FILE, HIVE_LLAP_TASK_KEYTAB_FILE};
              Map<String, String> newProperties = new HashMap<>();
              for (String identity : identities) {
                // Update only if we were able to modify the corresponding kerberos descriptor,
                // reflected in list 'getYarnKerberosDescUpdatedList'.
                if (getYarnKerberosDescUpdatedList().contains(identity) && hsiSiteConfigProperties.containsKey(identity)) {
                  newProperties.put(identity, hsiSiteConfigProperties.get(HIVE_LLAP_DAEMON_KEYTAB_FILE));
                }
              }

              // Update step.
              if (newProperties.size() > 0) {
                try {
                  updateConfigurationPropertiesForCluster(cluster, HIVE_INTERACTIVE_SITE, newProperties, true, false);
                  LOG.info("Updated HSI config(s) : " + newProperties.keySet() + " with value(s) = " + newProperties.values() + " respectively.");
                } catch (AmbariException e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    }
  }

  protected void updateAmsConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {


          Config amsSslClient = cluster.getDesiredConfigByType(AMS_SSL_CLIENT);
          if (amsSslClient != null) {
            Map<String, String> amsSslClientProperties = amsSslClient.getProperties();

            if (amsSslClientProperties.containsKey(METRIC_TRUSTSTORE_ALIAS)) {
              LOG.info("Removing " + METRIC_TRUSTSTORE_ALIAS + " from " + AMS_SSL_CLIENT);
              removeConfigurationPropertiesFromCluster(cluster, AMS_SSL_CLIENT, Collections.singleton(METRIC_TRUSTSTORE_ALIAS));
            }

          }
        }
      }
    }
  }

  protected void updateHDFSWidgetDefinition() throws AmbariException {
    LOG.info("Updating HDFS widget definition.");

    Map<String, List<String>> widgetMap = new HashMap<>();
    Map<String, String> sectionLayoutMap = new HashMap<>();

    List<String> hdfsHeatmapWidgets = new ArrayList<>(Arrays.asList("HDFS Bytes Read", "HDFS Bytes Written",
      "DataNode Process Disk I/O Utilization", "DataNode Process Network I/O Utilization"));
    widgetMap.put("HDFS_HEATMAPS", hdfsHeatmapWidgets);
    sectionLayoutMap.put("HDFS_HEATMAPS", "default_hdfs_heatmap");

    updateWidgetDefinitionsForService("HDFS", widgetMap, sectionLayoutMap);
  }

  /**
   * Retrieves the relevant {@link Cluster} given information from the suppliied {@link ArtifactEntity}.
   * <p>
   * The cluster id value is taken from the entity's foreign key value and then used to obtain the cluster object.
   *
   * @param artifactEntity an {@link ArtifactEntity}
   * @return a {@link Cluster}
   */
  private Cluster getCluster(ArtifactEntity artifactEntity) {
    if (artifactEntity != null) {
      Map<String, String> keys = artifactEntity.getForeignKeys();
      if (keys != null) {
        String clusterId = keys.get("cluster");
        if (StringUtils.isNumeric(clusterId)) {
          Clusters clusters = injector.getInstance(Clusters.class);
          try {
            return clusters.getCluster(Long.valueOf(clusterId));
          } catch (AmbariException e) {
            LOG.error(String.format("Failed to obtain cluster using cluster id %s -  %s", clusterId, e.getMessage()), e);
          }
        } else {
          LOG.error(String.format("Failed to obtain cluster id from artifact entity with foreign keys: %s", keys));
        }
      }
    }

    return null;
  }

  /**
   * Recursively traverses the Kerberos descriptor to find and fix the identity references.
   * <p>
   * Each found identity descriptor that indicates it is a reference by having a <code>name</code>
   * value that starts with a "/" or a "./" is fixed by clearing the <code>principal name</code>value,
   * setting the <code>reference</code> value to the <code>name</code> value and changing the
   * <code>name</code> value to a name with the following pattern:
   * <code>SERVICE_COMPONENT_IDENTITY</code>
   * <p>
   * For example, if the identity is for the "SERVICE1" service and is a reference to "HDFS/NAMENODE/hdfs";
   * then the name is set to "<code>service1_hdfs</code>"
   * <p>
   * For example, if the identity is for the "COMPONENT21" component of the "SERVICE2" service and is a reference to "HDFS/NAMENODE/hdfs";
   * then the name is set to "<code>service2_component21_hdfs</code>"
   * <p>
   * Once the identity descriptor properties of the identity are fixed, the relevant configuration
   * value is fixed to match the value if the referenced identity. This may lead to a new version
   * of the relevant configuration type.
   *
   * @param cluster   the cluster
   * @param container the current Kerberos descriptor container
   * @throws AmbariException if an error occurs
   */
  private void fixIdentityReferences(Cluster cluster, AbstractKerberosDescriptorContainer container)
      throws AmbariException {
    List<KerberosIdentityDescriptor> identities = container.getIdentities();
    if (identities != null) {
      for (KerberosIdentityDescriptor identity : identities) {
        String name = identity.getName();

        if (!StringUtils.isEmpty(name) && (name.startsWith("/") || name.startsWith("./"))) {
          String[] parts = name.split("/");
          String newName = buildName(identity.getParent(), parts[parts.length - 1]);

          identity.setName(newName);
          identity.setReference(name);
        }

        String identityReference = identity.getReference();
        if (!StringUtils.isEmpty(identityReference)) {
          // If this identity references another identity:
          //  * The principal name needs to be the same as the referenced identity
          //    - ensure that no principal name is being set for this identity
          //  * Any configuration set to contain the reference principal name needs to be fixed to
          //    be the correct principal name
          KerberosPrincipalDescriptor principal = identity.getPrincipalDescriptor();
          if (principal != null) {
            // Fix the value
            principal.setValue(null);

            // Fix the relative configuration
            if (!StringUtils.isEmpty(principal.getConfiguration())) {
              String referencedPrincipalName = getConfiguredPrincipalNameFromReference(cluster, container, identityReference);

              if(!StringUtils.isEmpty(referencedPrincipalName)) {
                String[] parts = principal.getConfiguration().split("/");
                if (parts.length == 2) {
                  String type = parts[0];
                  String property = parts[1];

                  updateConfigurationPropertiesForCluster(cluster,
                      type,
                      Collections.singletonMap(property, referencedPrincipalName),
                      true,
                      false);
                }
              }
            }
          }
        }
      }
    }

    if (container instanceof KerberosDescriptor) {
      Map<String, KerberosServiceDescriptor> services = ((KerberosDescriptor) container).getServices();
      if (services != null) {
        for (KerberosServiceDescriptor serviceDescriptor : services.values()) {
          fixIdentityReferences(cluster, serviceDescriptor);
        }
      }
    } else if (container instanceof KerberosServiceDescriptor) {
      Map<String, KerberosComponentDescriptor> components = ((KerberosServiceDescriptor) container).getComponents();
      if (components != null) {
        for (KerberosComponentDescriptor componentDescriptor : components.values()) {
          fixIdentityReferences(cluster, componentDescriptor);
        }
      }
    }
  }

  /**
   * Finds the value of the configuration found for the principal in the referenced identity
   * descriptor.
   *
   * @param cluster           the cluster
   * @param container         the current {@link KerberosIdentityDescriptor}, ideally the identity's parent descriptor
   * @param identityReference the path to the referenced identity
   * @return the value of the configuration specified in the referenced identity's principal descriptor
   * @throws AmbariException if an error occurs
   */
  private String getConfiguredPrincipalNameFromReference(Cluster cluster,
                                                         AbstractKerberosDescriptorContainer container,
                                                         String identityReference)
      throws AmbariException {
    KerberosIdentityDescriptor identityDescriptor = container.getReferencedIdentityDescriptor(identityReference);

    if (identityDescriptor != null) {
      KerberosPrincipalDescriptor principal = identityDescriptor.getPrincipalDescriptor();
      if ((principal != null) && (!StringUtils.isEmpty(principal.getConfiguration()))) {
        String[] parts = principal.getConfiguration().split("/");
        if (parts.length == 2) {
          String type = parts[0];
          String property = parts[1];

          Config config = cluster.getDesiredConfigByType(type);

          if (config != null) {
            return config.getProperties().get(property);
          }
        }
      }
    }

    return null;
  }

  /**
   * Builds the name of an identity based on the identity's container and the referenced identity's name.
   * <p>
   * The calculated name will be in the following format and converted to all lowercase characters:
   * <code>SERVICE_COMPONENT_IDENTITY</code>
   *
   * @param container    the current {@link KerberosIdentityDescriptor}, ideally the identity's parent descriptor
   * @param identityName the referenced identity's name
   * @return a name
   */
  private String buildName(AbstractKerberosDescriptor container, String identityName) {
    if (container instanceof KerberosServiceDescriptor) {
      return container.getName().toLowerCase() + "_" + identityName;
    } else if (container instanceof KerberosComponentDescriptor) {
      return container.getParent().getName().toLowerCase() + "_" + container.getName().toLowerCase() + "_" + identityName;
    } else {
      return identityName;
    }
  }

  /**
   * Sets all existing repository versions to be resolved (we have to assume
   * that they are good since they've been using them to run stuff).
   *
   * @throws AmbariException
   */
  protected void updateExistingRepositoriesToBeResolved() throws AmbariException {
    RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    List<RepositoryVersionEntity> repositoryVersions = repositoryVersionDAO.findAll();
    for (RepositoryVersionEntity repositoryVersion : repositoryVersions) {
      repositoryVersion.setResolved(true);
      repositoryVersionDAO.merge(repositoryVersion);
    }
  }
}
