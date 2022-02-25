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


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.ServiceDesiredStateDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertGroupEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.PrepareKerberosIdentitiesServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosConfigurationDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.topology.validators.HiveServiceValidator;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class UpgradeCatalog270 extends AbstractUpgradeCatalog {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog270.class);

  protected static final String STAGE_TABLE = "stage";
  protected static final String STAGE_STATUS_COLUMN = "status";
  protected static final String STAGE_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String REQUEST_TABLE = "request";
  protected static final String REQUEST_DISPLAY_STATUS_COLUMN = "display_status";
  protected static final String REQUEST_USER_NAME_COLUMN = "user_name";
  protected static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  protected static final String HRC_OPS_DISPLAY_NAME_COLUMN = "ops_display_name";
  protected static final String COMPONENT_DESIRED_STATE_TABLE = "hostcomponentdesiredstate";
  protected static final String COMPONENT_STATE_TABLE = "hostcomponentstate";
  protected static final String COMPONENT_LAST_STATE_COLUMN = "last_live_state";
  protected static final String SERVICE_DESIRED_STATE_TABLE = "servicedesiredstate";
  protected static final String SECURITY_STATE_COLUMN = "security_state";

  protected static final String AMBARI_SEQUENCES_TABLE = "ambari_sequences";
  protected static final String AMBARI_SEQUENCES_SEQUENCE_NAME_COLUMN = "sequence_name";
  protected static final String AMBARI_SEQUENCES_SEQUENCE_VALUE_COLUMN = "sequence_value";

  protected static final String AMBARI_CONFIGURATION_TABLE = "ambari_configuration";
  protected static final String AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN = "category_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN = "property_name";
  protected static final String AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN = "property_value";

  protected static final String USER_AUTHENTICATION_TABLE = "user_authentication";
  protected static final String USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN = "user_authentication_id";
  protected static final String USER_AUTHENTICATION_USER_ID_COLUMN = "user_id";
  protected static final String USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN = "authentication_type";
  protected static final String USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN = "authentication_key";
  protected static final String USER_AUTHENTICATION_CREATE_TIME_COLUMN = "create_time";
  protected static final String USER_AUTHENTICATION_UPDATE_TIME_COLUMN = "update_time";
  protected static final String USER_AUTHENTICATION_PRIMARY_KEY = "PK_user_authentication";
  protected static final String USER_AUTHENTICATION_USER_AUTHENTICATION_USER_ID_INDEX = "IDX_user_authentication_user_id";
  protected static final String USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY = "FK_user_authentication_users";

  protected static final String USERS_TABLE = "users";
  protected static final String USERS_USER_ID_COLUMN = "user_id";
  protected static final String USERS_PRINCIPAL_ID_COLUMN = "principal_id";
  protected static final String USERS_USER_TYPE_COLUMN = "user_type";
  protected static final String USERS_USER_PASSWORD_COLUMN = "user_password";
  protected static final String USERS_CREATE_TIME_COLUMN = "create_time";
  protected static final String USERS_LDAP_USER_COLUMN = "ldap_user";
  protected static final String USERS_CONSECUTIVE_FAILURES_COLUMN = "consecutive_failures";
  protected static final String USERS_USER_NAME_COLUMN = "user_name";
  protected static final String USERS_DISPLAY_NAME_COLUMN = "display_name";
  protected static final String USERS_LOCAL_USERNAME_COLUMN = "local_username";
  protected static final String USERS_VERSION_COLUMN = "version";
  protected static final String UNIQUE_USERS_0_INDEX = "UNQ_users_0";

  protected static final String MEMBERS_TABLE = "members";
  protected static final String MEMBERS_MEMBER_ID_COLUMN = "member_id";
  protected static final String MEMBERS_GROUP_ID_COLUMN = "group_id";
  protected static final String MEMBERS_USER_ID_COLUMN = "user_id";

  protected static final String ADMINPRIVILEGE_TABLE = "adminprivilege";
  protected static final String ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN = "privilege_id";
  protected static final String ADMINPRIVILEGE_PERMISSION_ID_COLUMN = "permission_id";
  protected static final String ADMINPRIVILEGE_RESOURCE_ID_COLUMN = "resource_id";
  protected static final String ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN = "principal_id";

  // kerberos tables constants
  protected static final String KERBEROS_KEYTAB_TABLE = "kerberos_keytab";
  protected static final String KERBEROS_KEYTAB_PRINCIPAL_TABLE = "kerberos_keytab_principal";
  protected static final String KKP_MAPPING_SERVICE_TABLE = "kkp_mapping_service";
  protected static final String KEYTAB_PATH_FIELD = "keytab_path";
  protected static final String OWNER_NAME_FIELD = "owner_name";
  protected static final String OWNER_ACCESS_FIELD = "owner_access";
  protected static final String GROUP_NAME_FIELD = "group_name";
  protected static final String GROUP_ACCESS_FIELD = "group_access";
  protected static final String IS_AMBARI_KEYTAB_FIELD = "is_ambari_keytab";
  protected static final String WRITE_AMBARI_JAAS_FIELD = "write_ambari_jaas";
  protected static final String PK_KERBEROS_KEYTAB = "PK_kerberos_keytab";
  protected static final String KKP_ID_COLUMN = "kkp_id";
  protected static final String PRINCIPAL_NAME_COLUMN = "principal_name";
  protected static final String IS_DISTRIBUTED_COLUMN = "is_distributed";
  protected static final String PK_KKP = "PK_kkp";
  protected static final String UNI_KKP = "UNI_kkp";
  protected static final String SERVICE_NAME_COLUMN = "service_name";
  protected static final String COMPONENT_NAME_COLUMN = "component_name";
  protected static final String PK_KKP_MAPPING_SERVICE = "PK_kkp_mapping_service";
  protected static final String FK_KKP_KEYTAB_PATH = "FK_kkp_keytab_path";
  protected static final String FK_KKP_HOST_ID = "FK_kkp_host_id";
  protected static final String FK_KKP_PRINCIPAL_NAME = "FK_kkp_principal_name";
  protected static final String HOSTS_TABLE = "hosts";
  protected static final String KERBEROS_PRINCIPAL_TABLE = "kerberos_principal";
  protected static final String FK_KKP_SERVICE_PRINCIPAL = "FK_kkp_service_principal";
  protected static final String KKP_ID_SEQ_NAME = "kkp_id_seq";
  protected static final String KERBEROS_PRINCIPAL_HOST_TABLE = "kerberos_principal_host";
  protected static final String HOST_ID_COLUMN = "host_id";

  protected static final String REPO_OS_TABLE = "repo_os";
  protected static final String REPO_OS_ID_COLUMN = "id";
  protected static final String REPO_OS_REPO_VERSION_ID_COLUMN = "repo_version_id";
  protected static final String REPO_OS_FAMILY_COLUMN = "family";
  protected static final String REPO_OS_AMBARI_MANAGED_COLUMN = "ambari_managed";
  protected static final String REPO_OS_PRIMARY_KEY = "PK_repo_os_id";
  protected static final String REPO_OS_FOREIGN_KEY = "FK_repo_os_id_repo_version_id";

  protected static final String REPO_DEFINITION_TABLE = "repo_definition";
  protected static final String REPO_DEFINITION_ID_COLUMN = "id";
  protected static final String REPO_DEFINITION_REPO_OS_ID_COLUMN = "repo_os_id";
  protected static final String REPO_DEFINITION_REPO_NAME_COLUMN = "repo_name";
  protected static final String REPO_DEFINITION_REPO_ID_COLUMN = "repo_id";
  protected static final String REPO_DEFINITION_BASE_URL_COLUMN = "base_url";
  protected static final String REPO_DEFINITION_DISTRIBUTION_COLUMN = "distribution";
  protected static final String REPO_DEFINITION_COMPONENTS_COLUMN = "components";
  protected static final String REPO_DEFINITION_UNIQUE_REPO_COLUMN = "unique_repo";
  protected static final String REPO_DEFINITION_MIRRORS_COLUMN = "mirrors";
  protected static final String REPO_DEFINITION_PRIMARY_KEY = "PK_repo_definition_id";
  protected static final String REPO_DEFINITION_FOREIGN_KEY = "FK_repo_definition_repo_os_id";

  protected static final String REPO_TAGS_TABLE = "repo_tags";
  protected static final String REPO_TAGS_REPO_DEFINITION_ID_COLUMN = "repo_definition_id";
  protected static final String REPO_TAGS_TAG_COLUMN = "tag";
  protected static final String REPO_TAGS_FOREIGN_KEY = "FK_repo_tag_definition_id";

  protected static final String REPO_APPLICABLE_SERVICES_TABLE = "repo_applicable_services";
  protected static final String REPO_APPLICABLE_SERVICES_REPO_DEFINITION_ID_COLUMN = "repo_definition_id";
  protected static final String REPO_APPLICABLE_SERVICES_SERVICE_NAME_COLUMN = "service_name";
  protected static final String REPO_APPLICABLE_SERVICES_FOREIGN_KEY = "FK_repo_app_service_def_id";

  protected static final String REPO_VERSION_TABLE = "repo_version";
  protected static final String REPO_VERSION_REPO_VERSION_ID_COLUMN = "repo_version_id";
  protected static final String REPO_VERSION_REPOSITORIES_COLUMN = "repositories";

  protected static final String WIDGET_TABLE = "widget";
  protected static final String WIDGET_TAG_COLUMN = "tag";

  protected static final String SERVICE_COMPONENT_DESIRED_STATE_TABLE = "servicecomponentdesiredstate";
  protected static final String HIVE_SERVICE_COMPONENT_WEBHCAT_SERVER = "WEBHCAT_SERVER";
  protected static final String CONFIGURATION_CORE_SITE = "core-site";
  protected static final String CONFIGURATION_WEBHCAT_SITE = "webhcat-site";
  protected static final String PROPERTY_HADOOP_PROXYUSER_HTTP_HOSTS = "hadoop.proxyuser.HTTP.hosts";
  protected static final String PROPERTY_TEMPLETON_HIVE_PROPERTIES = "templeton.hive.properties";
  public static final String AMBARI_INFRA_OLD_NAME = "AMBARI_INFRA";
  public static final String AMBARI_INFRA_NEW_NAME = "AMBARI_INFRA_SOLR";

  public static final String SERVICE_CONFIG_MAPPING_TABLE = "serviceconfigmapping";
  public static final String CLUSTER_CONFIG_TABLE = "clusterconfig";

  // Broken constraints added by Views
  public static final String FK_HOSTCOMPONENTDESIREDSTATE_COMPONENT_NAME = "fk_hostcomponentdesiredstate_component_name";
  public static final String FK_HOSTCOMPONENTSTATE_COMPONENT_NAME = "fk_hostcomponentstate_component_name";
  public static final String FK_SERVICECOMPONENTDESIREDSTATE_SERVICE_NAME = "fk_servicecomponentdesiredstate_service_name";

  static final String YARN_SERVICE = "YARN";

  @Inject
  DaoUtils daoUtils;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog270(Injector injector) {
    super(injector);

    daoUtils = injector.getInstance(DaoUtils.class);
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.7.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.6.2";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    dropBrokenFKs();
    updateStageTable();
    updateRequestTable();
    addOpsDisplayNameColumnToHostRoleCommand();
    removeSecurityState();
    addAmbariConfigurationTable();
    addHostComponentLastStateTable();
    upgradeUserTables();
    upgradeKerberosTables();
    upgradeRepoTables();
    upgradeWidgetTable();
  }

  /**
   * Upgrade the users table as well as supporting tables.
   * <p>
   * Affected table are
   * <ul>
   * <li>users</li>
   * <li>user_authentication (new)</li>
   * <li>members</li>
   * <li>adminprivilege</li>
   * </ul>
   *
   * @throws SQLException if an error occurs while executing SQL statements
   * @see #createUserAuthenticationTable()
   * @see #updateGroupMembershipRecords()
   * @see #updateAdminPrivilegeRecords()
   * @see #updateUsersTable()
   */
  protected void upgradeUserTables() throws SQLException {
    convertUserCreationTimeToLong();
    createUserAuthenticationTable();
    updateGroupMembershipRecords();
    updateAdminPrivilegeRecords();
    updateUsersTable();
  }

  protected void upgradeRepoTables() throws SQLException {
    createRepoOsTable();
    createRepoDefinitionTable();
    createRepoTagsTable();
    createRepoApplicableServicesTable();
    migrateRepoData();
    updateRepoVersionTable();
  }

  /**
   * Adds the repo_os table to the Ambari database.
   * <pre>
   * CREATE TABLE repo_os (
   *   id BIGINT NOT NULL,
   *   repo_version_id BIGINT NOT NULL,
   *   family VARCHAR(255) NOT NULL DEFAULT '',
   *   ambari_managed SMALLINT DEFAULT 1,
   *   CONSTRAINT PK_repo_os_id PRIMARY KEY (id),
   *   CONSTRAINT FK_repo_os_id_repo_version_id FOREIGN KEY (repo_version_id) REFERENCES repo_version (repo_version_id));
   * </pre>
   *
   * @throws SQLException
   */
  private void createRepoOsTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(REPO_OS_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_OS_REPO_VERSION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_OS_FAMILY_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_OS_AMBARI_MANAGED_COLUMN, Integer.class, null, 1, true));

    dbAccessor.createTable(REPO_OS_TABLE, columns);
    dbAccessor.addPKConstraint(REPO_OS_TABLE, REPO_OS_PRIMARY_KEY, REPO_OS_ID_COLUMN);
    dbAccessor.addFKConstraint(REPO_OS_TABLE, REPO_OS_FOREIGN_KEY, REPO_OS_REPO_VERSION_ID_COLUMN, REPO_VERSION_TABLE, REPO_VERSION_REPO_VERSION_ID_COLUMN, false);
  }

  /**
   * Adds the repo_definition table to the Ambari database.
   * <pre>
   *   CREATE TABLE repo_definition (
   *     id BIGINT NOT NULL,
   *     repo_os_id BIGINT,
   *     repo_name VARCHAR(255) NOT NULL,
   *     repo_id VARCHAR(255) NOT NULL,
   *     base_url VARCHAR(2048) NOT NULL,
   *     distribution VARCHAR(2048),
   *     components VARCHAR(2048),
   *     unique_repo SMALLINT DEFAULT 1,
   *     mirrors VARCHAR(2048),
   *     CONSTRAINT PK_repo_definition_id PRIMARY KEY (id),
   *     CONSTRAINT FK_repo_definition_repo_os_id FOREIGN KEY (repo_os_id) REFERENCES repo_os (id));
   * </pre>
   *
   * @throws SQLException
   */
  private void createRepoDefinitionTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_REPO_OS_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_REPO_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_REPO_ID_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_BASE_URL_COLUMN, String.class, 2048, null, true));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_DISTRIBUTION_COLUMN, String.class, 2048, null, true));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_COMPONENTS_COLUMN, String.class, 2048, null, true));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_UNIQUE_REPO_COLUMN, Integer.class, 1, 1, true));
    columns.add(new DBAccessor.DBColumnInfo(REPO_DEFINITION_MIRRORS_COLUMN, String.class, 2048, null, true));

    dbAccessor.createTable(REPO_DEFINITION_TABLE, columns);
    dbAccessor.addPKConstraint(REPO_DEFINITION_TABLE, REPO_DEFINITION_PRIMARY_KEY, REPO_DEFINITION_ID_COLUMN);
    dbAccessor.addFKConstraint(REPO_DEFINITION_TABLE, REPO_DEFINITION_FOREIGN_KEY, REPO_DEFINITION_REPO_OS_ID_COLUMN, REPO_OS_TABLE, REPO_OS_ID_COLUMN, false);
  }

  /**
   * Adds the repo_tags table to the Ambari database.
   * <pre>
   *   CREATE TABLE repo_tags (
   *     repo_definition_id BIGINT NOT NULL,
   *     tag VARCHAR(255) NOT NULL,
   *     CONSTRAINT FK_repo_tag_definition_id FOREIGN KEY (repo_definition_id) REFERENCES repo_definition (id));
   * </pre>
   *
   * @throws SQLException
   */
  private void createRepoTagsTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(REPO_TAGS_REPO_DEFINITION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_TAGS_TAG_COLUMN, String.class, 255, null, false));

    dbAccessor.createTable(REPO_TAGS_TABLE, columns);
    dbAccessor.addFKConstraint(REPO_TAGS_TABLE, REPO_TAGS_FOREIGN_KEY, REPO_TAGS_REPO_DEFINITION_ID_COLUMN, REPO_DEFINITION_TABLE, REPO_DEFINITION_ID_COLUMN, false);
  }

  /**
   * Adds the repo_applicable_services table to the Ambari database.
   * <pre>
   *   CREATE TABLE repo_applicable_services (
   *     repo_definition_id BIGINT NOT NULL,
   *     service_name VARCHAR(255) NOT NULL,
   *     CONSTRAINT FK_repo_applicable_service_definition_id FOREIGN KEY (repo_definition_id) REFERENCES repo_definition (id));
   * </pre>
   *
   * @throws SQLException
   */
  private void createRepoApplicableServicesTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(REPO_APPLICABLE_SERVICES_REPO_DEFINITION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(REPO_APPLICABLE_SERVICES_SERVICE_NAME_COLUMN, String.class, 255, null, false));

    dbAccessor.createTable(REPO_APPLICABLE_SERVICES_TABLE, columns);
    dbAccessor.addFKConstraint(REPO_APPLICABLE_SERVICES_TABLE, REPO_APPLICABLE_SERVICES_FOREIGN_KEY, REPO_APPLICABLE_SERVICES_REPO_DEFINITION_ID_COLUMN, REPO_DEFINITION_TABLE, REPO_DEFINITION_ID_COLUMN, false);
  }

  /**
   * Perform steps to move data from the old repo_version.repositories structure into new tables -
   * repo_os, repo_definition, repo_tags
   *
   * @throws SQLException
   */
  private void migrateRepoData() throws SQLException {
    if(dbAccessor.tableHasColumn(REPO_VERSION_TABLE, REPO_VERSION_REPOSITORIES_COLUMN)) {
      int repoOsId = 0;
      int repoDefinitionId = 0;

      // Get a map of repo_version.id to repo_version.repositories
      Map<Long, String> repoVersionData = dbAccessor.getKeyToStringColumnMap(REPO_VERSION_TABLE,
          REPO_VERSION_REPO_VERSION_ID_COLUMN, REPO_VERSION_REPOSITORIES_COLUMN, null, null, true);

      if (repoVersionData != null) {
        // For each entry in the map, parse the repo_version.repositories data and created records in the new
        // repo_os, repo_definition, and repo_tabs tables...
        for (Map.Entry<Long, String> entry : repoVersionData.entrySet()) {
          Long repoVersionId = entry.getKey();
          String repositoriesJson = entry.getValue();

          if (!StringUtils.isEmpty(repositoriesJson)) {
            JsonArray rootJson = new JsonParser().parse(repositoriesJson).getAsJsonArray();

            if (rootJson != null) {
              for (JsonElement rootElement : rootJson) {
                // process each OS element
                JsonObject rootObject = rootElement.getAsJsonObject();

                if (rootObject != null) {
                  JsonPrimitive osType = rootObject.getAsJsonPrimitive("OperatingSystems/os_type");
                  JsonPrimitive ambariManaged = rootObject.getAsJsonPrimitive("OperatingSystems/ambari_managed_repositories");
                  String isAmbariManaged = ambariManaged == null ? "1" :  (ambariManaged.getAsBoolean() ? "1" : "0"); //the SQL script which creates the DB schema defaults to 1

                  JsonArray repositories = rootObject.getAsJsonArray("repositories");

                  dbAccessor.insertRowIfMissing(REPO_OS_TABLE,
                      new String[]{REPO_OS_ID_COLUMN, REPO_OS_REPO_VERSION_ID_COLUMN, REPO_OS_AMBARI_MANAGED_COLUMN, REPO_OS_FAMILY_COLUMN},
                      new String[]{String.valueOf(++repoOsId), String.valueOf(repoVersionId), isAmbariManaged, getFormattedJSONPrimitiveString(osType)},
                      false);

                  if (repositories != null) {
                    for (JsonElement repositoryElement : repositories) {
                      JsonObject repositoryObject = repositoryElement.getAsJsonObject();

                      if (repositoryObject != null) {
                        JsonPrimitive repoId = repositoryObject.getAsJsonPrimitive("Repositories/repo_id");
                        JsonPrimitive repoName = repositoryObject.getAsJsonPrimitive("Repositories/repo_name");
                        JsonPrimitive baseUrl = repositoryObject.getAsJsonPrimitive("Repositories/base_url");
                        JsonArray tags = repositoryObject.getAsJsonArray("Repositories/tags");

                        dbAccessor.insertRowIfMissing(REPO_DEFINITION_TABLE,
                            new String[]{REPO_DEFINITION_ID_COLUMN, REPO_DEFINITION_REPO_OS_ID_COLUMN,
                                REPO_DEFINITION_REPO_NAME_COLUMN, REPO_DEFINITION_REPO_ID_COLUMN, REPO_DEFINITION_BASE_URL_COLUMN},
                            new String[]{String.valueOf(++repoDefinitionId), String.valueOf(repoOsId),
                                getFormattedJSONPrimitiveString(repoName), getFormattedJSONPrimitiveString(repoId),
                                getFormattedJSONPrimitiveString(baseUrl)},
                            false);

                        if (tags != null) {
                          for (JsonElement tagsElement : tags) {
                            JsonPrimitive tag = tagsElement.getAsJsonPrimitive();

                            if (tag != null) {
                              dbAccessor.insertRowIfMissing(REPO_TAGS_TABLE,
                                  new String[]{REPO_TAGS_REPO_DEFINITION_ID_COLUMN, REPO_TAGS_TAG_COLUMN},
                                  new String[]{String.valueOf(repoDefinitionId), getFormattedJSONPrimitiveString(tag)},
                                  false);
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Add the relevant records in the ambari_sequence table
      // - repo_os_id_seq
      // - repo_definition_id_seq
      dbAccessor.insertRowIfMissing(AMBARI_SEQUENCES_TABLE,
          new String[]{AMBARI_SEQUENCES_SEQUENCE_NAME_COLUMN, AMBARI_SEQUENCES_SEQUENCE_VALUE_COLUMN},
          new String[]{"'repo_os_id_seq'", String.valueOf(++repoOsId)},
          false);
      dbAccessor.insertRowIfMissing(AMBARI_SEQUENCES_TABLE,
          new String[]{AMBARI_SEQUENCES_SEQUENCE_NAME_COLUMN, AMBARI_SEQUENCES_SEQUENCE_VALUE_COLUMN},
          new String[]{"'repo_definition_id_seq'", String.valueOf(++repoDefinitionId)},
          false);
    }
  }

  private String getFormattedJSONPrimitiveString(JsonPrimitive jsonValue) {
    return jsonValue == null ? null : String.format("'%s'", jsonValue.getAsString());
  }

  /**
   * Updates the repo_version table by removing old columns
   *
   * @throws SQLException
   */
  private void updateRepoVersionTable() throws SQLException {
    dbAccessor.dropColumn(REPO_VERSION_TABLE, REPO_VERSION_REPOSITORIES_COLUMN);
  }

  /**
   * In order to save the epoch equivalent of users.create_time we need to convert data in this column as follows:
   * <ol>
   * <li>creating a temporary column where we store the numeric representation of
   * the timestamp
   * <li>populating data in the temporary column
   * <li>removing original column column
   * <li>renaming the temporary column to the original column
   * </ol>
   *
   * @throws SQLException
   *           if an error occurs while executing SQL statements
   *
   */
  private void convertUserCreationTimeToLong() throws SQLException {
    if (!isUserCreationTimeMigrated()) {
      LOG.info("Converting user creation times...");
      final String temporaryColumnName = USERS_CREATE_TIME_COLUMN + "_numeric";
      if (!dbAccessor.tableHasColumn(USERS_TABLE, temporaryColumnName)) {
        final DBAccessor.DBColumnInfo tempColumnInfo = new DBAccessor.DBColumnInfo(temporaryColumnName, Long.class);
        dbAccessor.addColumn(USERS_TABLE, tempColumnInfo);
      }

      if (dbAccessor.tableHasColumn(USERS_TABLE, USERS_CREATE_TIME_COLUMN)) {
        final Map<Integer, Timestamp> currentUserCreateTimes = fetchCurrentUserCreateTimesNotYetMigrated(temporaryColumnName);
        for (Map.Entry<Integer, Timestamp> currentUserCreateTime : currentUserCreateTimes.entrySet()) {
          dbAccessor.updateTable(USERS_TABLE, temporaryColumnName, currentUserCreateTime.getValue().getTime(),
              "WHERE " + USERS_USER_ID_COLUMN + "=" + currentUserCreateTime.getKey());
        }

        dbAccessor.dropColumn(USERS_TABLE, USERS_CREATE_TIME_COLUMN);
      }

      final DBAccessor.DBColumnInfo usersCreateTimeColumnInfo = new DBAccessor.DBColumnInfo(USERS_CREATE_TIME_COLUMN, Long.class, null, null, false);
      dbAccessor.renameColumn(USERS_TABLE, temporaryColumnName, usersCreateTimeColumnInfo);
      LOG.info("Converted user creation times");
    } else {
      LOG.info("Already converted user creation timestamps to EPOCH representation");
    }
  }

  private boolean isUserCreationTimeMigrated() throws SQLException {
    final int columnType = dbAccessor.getColumnType(USERS_TABLE, USERS_CREATE_TIME_COLUMN);
    LOG.info(USERS_TABLE + "." + USERS_CREATE_TIME_COLUMN + "'s type = " + columnType);
    return columnType != Types.DATE && columnType != Types.TIMESTAMP;
  }

  private Map<Integer, Timestamp> fetchCurrentUserCreateTimesNotYetMigrated(String temporaryColumnName) throws SQLException {
    final Map<Integer, Timestamp> currentUserCreateTimes = new HashMap<>();
    try (
        PreparedStatement pstmt = dbAccessor.getConnection().prepareStatement("SELECT " + USERS_USER_ID_COLUMN + ", " + USERS_CREATE_TIME_COLUMN + " FROM " + USERS_TABLE + " WHERE " + temporaryColumnName + " IS NULL ORDER BY " + USERS_USER_ID_COLUMN);
        ResultSet rs = pstmt.executeQuery()) {
      while (rs.next()) {
        currentUserCreateTimes.put(rs.getInt(1), rs.getTimestamp(2) == null ? new Timestamp(System.currentTimeMillis()) : rs.getTimestamp(2));
      }
    }
    return currentUserCreateTimes;
  }

  /**
   * If the <code>users</code> table has not yet been migrated, create the <code>user_authentication</code>
   * table and generate relevant records for that table based on data in the <code>users</code> table.
   * <p>
   * The records in the new <code>user_authentication</code> table represent all of the types associated
   * with a given (case-insensitive) username.  If <code>UserA:LOCAL</code>, <code>usera:LOCAL</code> and
   * <code>usera:LDAP</code> exist in the original <code>users</code> table, three records will be created
   * in the <code>user_authentication</code> table: one for each t
   * to <code>Role1</code>, the three <code>adminprivilege</code> records will be merged into a single
   * record for <code>usera</code>.
   *
   * @throws SQLException if an error occurs while executing SQL statements
   */
  private void createUserAuthenticationTable() throws SQLException {
    if (!usersTableUpgraded()) {
      final String temporaryTable = USER_AUTHENTICATION_TABLE + "_tmp";
      List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN, Long.class, null, null, false));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_USER_ID_COLUMN, Integer.class, null, null, false));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN, String.class, 50, null, false));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN, String.class, 2048, null, true));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_CREATE_TIME_COLUMN, Long.class, null, null, true));
      columns.add(new DBAccessor.DBColumnInfo(USER_AUTHENTICATION_UPDATE_TIME_COLUMN, Long.class, null, null, true));

      // Make sure the temporary table does not exist
      dbAccessor.dropTable(temporaryTable);

      // Create temporary table
      dbAccessor.createTable(temporaryTable, columns);

      dbAccessor.executeUpdate(
        "insert into " + temporaryTable +
          "(" + USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN + ", " + USER_AUTHENTICATION_USER_ID_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + ", " + USER_AUTHENTICATION_CREATE_TIME_COLUMN + ", " + USER_AUTHENTICATION_UPDATE_TIME_COLUMN + ")" +
          " select distinct" +
          "  u." + USERS_USER_ID_COLUMN + "," +
          "  t.min_user_id," +
          "  u." + USERS_USER_TYPE_COLUMN + "," +
          "  u." + USERS_USER_PASSWORD_COLUMN + "," +
          "  u." + USERS_CREATE_TIME_COLUMN + "," +
          "  u." + USERS_CREATE_TIME_COLUMN +
          " from " + USERS_TABLE + " u inner join" +
          "   (select" +
          "     lower(" + USERS_USER_NAME_COLUMN + ") as " + USERS_USER_NAME_COLUMN + "," +
          "     min(" + USERS_USER_ID_COLUMN + ") as min_user_id" +
          "    from " + USERS_TABLE +
          "    group by lower(" + USERS_USER_NAME_COLUMN + ")) t" +
          " on (lower(u." + USERS_USER_NAME_COLUMN + ") = lower(t." + USERS_USER_NAME_COLUMN + "))"
      );

      // Ensure only LOCAL users have keys set in the user_authentication table
      dbAccessor.executeUpdate("update " + temporaryTable +
        " set " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + "=null" +
        " where " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + "!='" + UserAuthenticationType.LOCAL.name() + "'");

      dbAccessor.createTable(USER_AUTHENTICATION_TABLE, columns);
      dbAccessor.addPKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_PRIMARY_KEY, USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN);
      dbAccessor.addFKConstraint(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_USER_AUTHENTICATION_USERS_FOREIGN_KEY, USER_AUTHENTICATION_USER_ID_COLUMN, USERS_TABLE, USERS_USER_ID_COLUMN, false);

      dbAccessor.executeUpdate(
        "insert into " + USER_AUTHENTICATION_TABLE +
          "(" + USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN + ", " + USER_AUTHENTICATION_USER_ID_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + ", " + USER_AUTHENTICATION_CREATE_TIME_COLUMN + ", " + USER_AUTHENTICATION_UPDATE_TIME_COLUMN + ")" +
          " select " +
          USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN + ", " + USER_AUTHENTICATION_USER_ID_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_TYPE_COLUMN + ", " + USER_AUTHENTICATION_AUTHENTICATION_KEY_COLUMN + ", " + USER_AUTHENTICATION_CREATE_TIME_COLUMN + ", " + USER_AUTHENTICATION_UPDATE_TIME_COLUMN +
          " from " + temporaryTable
      );

      // Delete the temporary table
      dbAccessor.dropTable(temporaryTable);
    }
  }

  private boolean usersTableUpgraded() {
    try {
      dbAccessor.getColumnType(USERS_TABLE, USERS_USER_TYPE_COLUMN);
      return false;
    } catch (SQLException e) {
      return true;
    }
  }

  /**
   * Update the <code>users</code> table by adjusting the relevant columns, contained data, and indicies.
   * <p>
   * This method should be executed after creating the <code>user_authentication</code> table and
   * adjusting the <code>members</code> and <code>adminprivilege</code> data by merging data while
   * combine user entries with the same username (but different type).
   * <p>
   * <ol>
   * <li>
   * Orphaned data is removed.  These will be the records where the usernamne is duplicated but
   * the user type is different.  Only a single record with a given username should be left.
   * </li>
   * <li>
   * Remove the unique record constraint so it may be added back later declaring new constraints
   * </li>
   * <li>
   * Obsolete columns are removed: <code>user_type</code>, <code>ldap_user</code>, <code>user_password</code>.
   * These columns are handled by the <codee>user_authentication</codee> table.
   * </li>
   * <li>
   * Add new columns: <code>consecutive_failures</code>, <code>display_name</code>,
   * <code>local_username</code>, <code>version</code>.
   * The non-null constraints are to be set after all the date is set properly.
   * </li>
   * <li>
   * Ensure the <code>display_name</code> and <code>local_username</code> columns have properly set data.
   * </li>
   * <li>
   * Add the non-null constraint back for the <code>display_name</code> and <code>local_username</code> columns.
   * </li>
   * <li>
   * Add a unique index on the <code>user_name</code> column
   * </li>
   * </ol>
   *
   * @throws SQLException if an error occurs while executing SQL statements
   * @see #createUserAuthenticationTable()
   * @see #updateGroupMembershipRecords()
   * @see #updateAdminPrivilegeRecords()
   */
  private void updateUsersTable() throws SQLException {
    // Remove orphaned user records...
    dbAccessor.executeUpdate("delete from " + USERS_TABLE +
      " where " + USERS_USER_ID_COLUMN + " not in (select " + USER_AUTHENTICATION_USER_ID_COLUMN + " from " + USER_AUTHENTICATION_TABLE + ")");

    // Update the users table
    dbAccessor.dropUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX);
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_TYPE_COLUMN);
    dbAccessor.dropColumn(USERS_TABLE, USERS_LDAP_USER_COLUMN);
    dbAccessor.dropColumn(USERS_TABLE, USERS_USER_PASSWORD_COLUMN);
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_CONSECUTIVE_FAILURES_COLUMN, Integer.class, null, 0, false));
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, true)); // Set to non-null later
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, true)); // Set to non-null later
    dbAccessor.addColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_VERSION_COLUMN, Long.class, null, 0, false));

    // Set the display name and local username values based on the username value
    dbAccessor.executeUpdate("update " + USERS_TABLE +
      " set " + USERS_DISPLAY_NAME_COLUMN + "=" + USERS_USER_NAME_COLUMN +
      ", " + USERS_LOCAL_USERNAME_COLUMN + "= lower(" + USERS_USER_NAME_COLUMN + ")" +
      ", " + USERS_USER_NAME_COLUMN + "= lower(" + USERS_USER_NAME_COLUMN + ")");

    // Change columns to non-null
    dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_DISPLAY_NAME_COLUMN, String.class, 255, null, false));
    dbAccessor.alterColumn(USERS_TABLE, new DBAccessor.DBColumnInfo(USERS_LOCAL_USERNAME_COLUMN, String.class, 255, null, false));

    // Add a unique constraint on the user_name column
    dbAccessor.addUniqueConstraint(USERS_TABLE, UNIQUE_USERS_0_INDEX, USERS_USER_NAME_COLUMN);
  }

  /**
   * Update the <code>members</code> table to ensure records for the same username but different user
   * records are referencing the main user record. Duplicate records will be be ignored when updating
   * the <code>members</code> table.
   * <p>
   * If <code>UserA:LOCAL</code>, <code>usera:LOCAL</code> and <code>usera:LDAP</code> all belong to
   * <code>Group1</code>, the three <code>members</code> records will be merged into a single record
   * for <code>usera</code>.
   * <p>
   * This method may be executed multiple times and will yield the same results each time.
   *
   * @throws SQLException if an error occurs while executing SQL statements
   */
  private void updateGroupMembershipRecords() throws SQLException {
    final String temporaryTable = MEMBERS_TABLE + "_tmp";

    // Make sure the temporary table does not exist
    dbAccessor.dropTable(temporaryTable);

    // Create temporary table
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(MEMBERS_MEMBER_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(MEMBERS_USER_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(MEMBERS_GROUP_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(temporaryTable, columns);

    // Insert updated data
    /* *******
     * Find the user id for the merged user records for the user that is related to each member record.
     * - Using the user_id from the original member record, find the user_name of that user.
     * - Using the found user_name, find the user_id for the _merged_ record.  This will be the value of the
     *   smallest user_id for all user_ids where the user_name matches that found user_name.
     * - The user_name value is case-insensitive.
     * ******* */
    dbAccessor.executeUpdate(
      "insert into " + temporaryTable + " (" + MEMBERS_MEMBER_ID_COLUMN + ", " + MEMBERS_USER_ID_COLUMN + ", " + MEMBERS_GROUP_ID_COLUMN + ")" +
        "  select" +
        "    m." + MEMBERS_MEMBER_ID_COLUMN + "," +
        "    u.min_user_id," +
        "    m." + MEMBERS_GROUP_ID_COLUMN +
        "  from " + MEMBERS_TABLE + " m inner join" +
        "    (" +
        "      select" +
        "        iu." + USERS_USER_NAME_COLUMN + "," +
        "        iu." + USERS_USER_ID_COLUMN + "," +
        "        t.min_user_id" +
        "      from " + USERS_TABLE + " iu inner join" +
        "        (" +
        "          select" +
        "           lower(" + USERS_USER_NAME_COLUMN + ") as " + USERS_USER_NAME_COLUMN + "," +
        "            min(" + USERS_USER_ID_COLUMN + ") as min_user_id" +
        "          from " + USERS_TABLE +
        "          group by lower(" + USERS_USER_NAME_COLUMN + ")" +
        "        ) t on (lower(t." + USERS_USER_NAME_COLUMN + ") = lower(iu." + USERS_USER_NAME_COLUMN + "))" +
        "    ) u on (m." + MEMBERS_USER_ID_COLUMN + " = u." + USERS_USER_ID_COLUMN + ")");

    // Truncate existing membership records
    dbAccessor.truncateTable(MEMBERS_TABLE);

    // Insert temporary records into members table
    /*
     * Copy the generated data to the original <code>members</code> table, effectively skipping
     * duplicate records.
     */
    dbAccessor.executeUpdate(
      "insert into " + MEMBERS_TABLE + " (" + MEMBERS_MEMBER_ID_COLUMN + ", " + MEMBERS_USER_ID_COLUMN + ", " + MEMBERS_GROUP_ID_COLUMN + ")" +
        "  select " +
        "    min(" + MEMBERS_MEMBER_ID_COLUMN + ")," +
        "    " + MEMBERS_USER_ID_COLUMN + "," +
        "    " + MEMBERS_GROUP_ID_COLUMN +
        "  from " + temporaryTable +
        "  group by " + MEMBERS_USER_ID_COLUMN + ", " + MEMBERS_GROUP_ID_COLUMN);

    // Delete the temporary table
    dbAccessor.dropTable(temporaryTable);
  }

  /**
   * Update the <code>adminprivilege</code> table to ensure records for the same username but different user
   * records are referencing the main user record. Duplicate records will be be ignored when updating
   * the <code>adminprivilege</code> table.
   * <p>
   * If <code>UserA:LOCAL</code>, <code>usera:LOCAL</code> and <code>usera:LDAP</code> are assigned
   * to <code>Role1</code>, the three <code>adminprivilege</code> records will be merged into a single
   * record for <code>usera</code>.
   * <p>
   * This method may be executed multiple times and will yield the same results each time.
   *
   * @throws SQLException if an error occurs while executing SQL statements
   */
  private void updateAdminPrivilegeRecords() throws SQLException {
    final String temporaryTable = ADMINPRIVILEGE_TABLE + "_tmp";

    // Make sure the temporary table does not exist
    dbAccessor.dropTable(temporaryTable);

    // Create temporary table
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PERMISSION_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_RESOURCE_ID_COLUMN, Long.class, null, null, false));
    columns.add(new DBAccessor.DBColumnInfo(ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN, Long.class, null, null, false));
    dbAccessor.createTable(temporaryTable, columns);

    // Insert updated data
    /* *******
     * Find the principal id for the merged user records for the user that is related to each relevant
     * adminprivilege record.
     * - Using the principal_id from the original adminprivilege record, find the user_name of that user.
     * - Using the found user_name, find the user_id for the _merged_ record.  This will be the value of the
     *   smallest user_id for all user_ids where the user_name matches that found user_name.
     * - Using the found user_id, obtain the relevant principal_id
     * - The user_name value is case-insensitive.
     * ******* */
    dbAccessor.executeUpdate(
      "insert into " + temporaryTable + " (" + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + ", " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + ", " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + ", " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + ")" +
        "  select" +
        "    ap." + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN +
        "  from " + ADMINPRIVILEGE_TABLE + " ap" +
        "  where ap." + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + " not in" +
        "        (" +
        "          select " + USERS_PRINCIPAL_ID_COLUMN +
        "          from " + USERS_TABLE +
        "        )" +
        "  union" +
        "  select" +
        "    ap." + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + "," +
        "    ap." + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + "," +
        "    t.new_principal_id" +
        "  from " + ADMINPRIVILEGE_TABLE + " ap inner join" +
        "    (" +
        "      select" +
        "        u." + USERS_USER_ID_COLUMN + "," +
        "        u." + USERS_USER_NAME_COLUMN + "," +
        "        u." + USERS_PRINCIPAL_ID_COLUMN + " as new_principal_id," +
        "        t1." + USERS_PRINCIPAL_ID_COLUMN + " as orig_principal_id" +
        "      from " + USERS_TABLE + " u inner join" +
        "        (" +
        "          select" +
        "            u1." + USERS_USER_NAME_COLUMN + "," +
        "            u1." + USERS_PRINCIPAL_ID_COLUMN + "," +
        "            t2.min_user_id" +
        "          from " + USERS_TABLE + " u1 inner join" +
        "            (" +
        "              select" +
        "                lower(" + USERS_USER_NAME_COLUMN + ") as " + USERS_USER_NAME_COLUMN + "," +
        "                min(" + USERS_USER_ID_COLUMN + ") as min_user_id" +
        "              from " + USERS_TABLE +
        "              group by lower(" + USERS_USER_NAME_COLUMN + ")" +
        "            ) t2 on (lower(u1." + USERS_USER_NAME_COLUMN + ") = lower(t2." + USERS_USER_NAME_COLUMN + "))" +
        "        ) t1 on (u." + USERS_USER_ID_COLUMN + " = t1.min_user_id)" +
        "    ) t on (ap." + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + " = t.orig_principal_id)");

    // Truncate existing adminprivilege records
    dbAccessor.truncateTable(ADMINPRIVILEGE_TABLE);

    // Insert temporary records into adminprivilege table
    /*
     * Copy the generated data to the original <code>adminprivilege</code> table, effectively skipping
     * duplicate records.
     */
    dbAccessor.executeUpdate(
      "insert into " + ADMINPRIVILEGE_TABLE + " (" + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + ", " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + ", " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + ", " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN + ")" +
        "  select " +
        "    min(" + ADMINPRIVILEGE_PRIVILEGE_ID_COLUMN + ")," +
        "    " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + "," +
        "    " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + "," +
        "    " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN +
        "  from " + temporaryTable +
        "  group by " + ADMINPRIVILEGE_PERMISSION_ID_COLUMN + ", " + ADMINPRIVILEGE_RESOURCE_ID_COLUMN + ", " + ADMINPRIVILEGE_PRINCIPAL_ID_COLUMN);

    // Delete the temporary table
    dbAccessor.dropTable(temporaryTable);
  }

  private void dropBrokenFKs() throws SQLException {
    dbAccessor.dropFKConstraint(COMPONENT_DESIRED_STATE_TABLE, FK_HOSTCOMPONENTDESIREDSTATE_COMPONENT_NAME);
    dbAccessor.dropFKConstraint(COMPONENT_STATE_TABLE, FK_HOSTCOMPONENTSTATE_COMPONENT_NAME);
    dbAccessor.dropFKConstraint(SERVICE_COMPONENT_DESIRED_STATE_TABLE, FK_SERVICECOMPONENTDESIREDSTATE_SERVICE_NAME);
  }

  protected void updateStageTable() throws SQLException {
    dbAccessor.addColumn(STAGE_TABLE,
      new DBAccessor.DBColumnInfo(STAGE_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(STAGE_TABLE,
      new DBAccessor.DBColumnInfo(STAGE_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
    dbAccessor.addColumn(REQUEST_TABLE,
      new DBAccessor.DBColumnInfo(REQUEST_DISPLAY_STATUS_COLUMN, String.class, 255, HostRoleStatus.PENDING, false));
  }

  protected void updateRequestTable() throws SQLException {
    dbAccessor.addColumn(REQUEST_TABLE, new DBAccessor.DBColumnInfo(REQUEST_USER_NAME_COLUMN, String.class, 255));
  }

  protected void upgradeWidgetTable() throws SQLException {
    dbAccessor.addColumn(WIDGET_TABLE, new DBAccessor.DBColumnInfo(WIDGET_TAG_COLUMN, String.class, 255));
  }

  protected void addAmbariConfigurationTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN, String.class, 100, null, false));
    columns.add(new DBAccessor.DBColumnInfo(AMBARI_CONFIGURATION_PROPERTY_VALUE_COLUMN, String.class, 2048, null, true));

    dbAccessor.createTable(AMBARI_CONFIGURATION_TABLE, columns);
    dbAccessor.addPKConstraint(AMBARI_CONFIGURATION_TABLE, "PK_ambari_configuration", AMBARI_CONFIGURATION_CATEGORY_NAME_COLUMN, AMBARI_CONFIGURATION_PROPERTY_NAME_COLUMN);
  }

  protected void addHostComponentLastStateTable() throws SQLException {
    dbAccessor.addColumn(COMPONENT_STATE_TABLE,
        new DBAccessor.DBColumnInfo(COMPONENT_LAST_STATE_COLUMN, String.class, 255, State.UNKNOWN, true));
  }

  /**
   * Creates new tables for changed kerberos data.
   *
   * @throws SQLException
   */
  protected void upgradeKerberosTables() throws SQLException {
    List<DBAccessor.DBColumnInfo> kerberosKeytabColumns = new ArrayList<>();
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(KEYTAB_PATH_FIELD, String.class, 255, null, false));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(OWNER_NAME_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(OWNER_ACCESS_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(GROUP_NAME_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(GROUP_ACCESS_FIELD, String.class, 255, null, true));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(IS_AMBARI_KEYTAB_FIELD, Integer.class, null, 0, false));
    kerberosKeytabColumns.add(new DBAccessor.DBColumnInfo(WRITE_AMBARI_JAAS_FIELD, Integer.class, null, 0, false));
    dbAccessor.createTable(KERBEROS_KEYTAB_TABLE, kerberosKeytabColumns);
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_TABLE, PK_KERBEROS_KEYTAB, KEYTAB_PATH_FIELD);

    List<DBAccessor.DBColumnInfo> kkpColumns = new ArrayList<>();
    kkpColumns.add(new DBAccessor.DBColumnInfo(KKP_ID_COLUMN, Long.class, null, 0L, false));
    kkpColumns.add(new DBAccessor.DBColumnInfo(KEYTAB_PATH_FIELD, String.class, 255, null, false));
    kkpColumns.add(new DBAccessor.DBColumnInfo(PRINCIPAL_NAME_COLUMN, String.class, 255, null, false));
    kkpColumns.add(new DBAccessor.DBColumnInfo(HOST_ID_COLUMN, Long.class, null, null, true));
    kkpColumns.add(new DBAccessor.DBColumnInfo(IS_DISTRIBUTED_COLUMN, Integer.class, null, 0, false));
    dbAccessor.createTable(KERBEROS_KEYTAB_PRINCIPAL_TABLE, kkpColumns);
    dbAccessor.addPKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, PK_KKP, KKP_ID_COLUMN);
    dbAccessor.addUniqueConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, UNI_KKP, KEYTAB_PATH_FIELD, PRINCIPAL_NAME_COLUMN, HOST_ID_COLUMN);

    List<DBAccessor.DBColumnInfo> kkpMappingColumns = new ArrayList<>();
    kkpMappingColumns.add(new DBAccessor.DBColumnInfo(KKP_ID_COLUMN, Long.class, null, 0L, false));
    kkpMappingColumns.add(new DBAccessor.DBColumnInfo(SERVICE_NAME_COLUMN, String.class, 255, null, false));
    kkpMappingColumns.add(new DBAccessor.DBColumnInfo(COMPONENT_NAME_COLUMN, String.class, 255, null, false));
    dbAccessor.createTable(KKP_MAPPING_SERVICE_TABLE, kkpMappingColumns);
    dbAccessor.addPKConstraint(KKP_MAPPING_SERVICE_TABLE, PK_KKP_MAPPING_SERVICE, KKP_ID_COLUMN, SERVICE_NAME_COLUMN, COMPONENT_NAME_COLUMN);


    //  cross tables constraints
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_KEYTAB_PATH, KEYTAB_PATH_FIELD, KERBEROS_KEYTAB_TABLE, KEYTAB_PATH_FIELD, false);
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_HOST_ID, HOST_ID_COLUMN, HOSTS_TABLE, HOST_ID_COLUMN, false);
    dbAccessor.addFKConstraint(KERBEROS_KEYTAB_PRINCIPAL_TABLE, FK_KKP_PRINCIPAL_NAME, PRINCIPAL_NAME_COLUMN, KERBEROS_PRINCIPAL_TABLE, PRINCIPAL_NAME_COLUMN, false);
    dbAccessor.addFKConstraint(KKP_MAPPING_SERVICE_TABLE, FK_KKP_SERVICE_PRINCIPAL, KKP_ID_COLUMN, KERBEROS_KEYTAB_PRINCIPAL_TABLE, KKP_ID_COLUMN, false);

    addSequence(KKP_ID_SEQ_NAME, 0L, false);
    dbAccessor.dropTable(KERBEROS_PRINCIPAL_HOST_TABLE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    renameAmbariInfra();
    updateKerberosDescriptorArtifacts();
    addNewConfigurationsFromXml();
    showHcatDeletedUserMessage();
    setStatusOfStagesAndRequests();
    updateLogSearchConfigs();
    updateKerberosConfigurations();
    moveAmbariPropertiesToAmbariConfiguration();
    createRoleAuthorizations();
    addUserAuthenticationSequence();
    updateSolrConfigurations();
    updateAmsConfigs();
    updateStormConfigs();
    clearHadoopMetrics2Content();
  }

  protected void renameAmbariInfra() {
    LOG.info("Renaming service AMBARI_INFRA to AMBARI_INFRA_SOLR");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters == null)
      return;

    Map<String, Cluster> clusterMap = clusters.getClusters();
    if (MapUtils.isEmpty(clusterMap))
      return;

    EntityManager entityManager = getEntityManagerProvider().get();
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    ServiceDesiredStateDAO serviceDesiredStateDAO = injector.getInstance(ServiceDesiredStateDAO.class);
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);

    for (final Cluster cluster : clusterMap.values()) {
      ClusterServiceEntityPK clusterServiceEntityPK = new ClusterServiceEntityPK();
      clusterServiceEntityPK.setClusterId(cluster.getClusterId());
      clusterServiceEntityPK.setServiceName(AMBARI_INFRA_OLD_NAME);
      ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByPK(clusterServiceEntityPK);
      if (clusterServiceEntity == null)
        continue;

      List<ServiceComponentDesiredStateEntity> serviceComponentDesiredStateEntities =
              new ArrayList<>(clusterServiceEntity.getServiceComponentDesiredStateEntities());
      ServiceDesiredStateEntity serviceDesiredStateEntity = clusterServiceEntity.getServiceDesiredStateEntity();
      List<HostComponentStateEntity> hostComponentStateEntities = hostComponentStateDAO.findByService(AMBARI_INFRA_OLD_NAME);
      List<HostComponentDesiredStateEntity> hostComponentDesiredStateEntities = new ArrayList<>();
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity : clusterServiceEntity.getServiceComponentDesiredStateEntities()) {
        hostComponentDesiredStateEntities.addAll(
                hostComponentDesiredStateDAO.findByIndex(cluster.getClusterId(), AMBARI_INFRA_OLD_NAME, serviceComponentDesiredStateEntity.getComponentName()));
      }

      for (HostComponentStateEntity hostComponentStateEntity : hostComponentStateEntities) {
        hostComponentStateDAO.remove(hostComponentStateEntity);
        entityManager.detach(hostComponentStateEntity);
        hostComponentStateEntity.setServiceName(AMBARI_INFRA_NEW_NAME);
      }

      for (HostComponentDesiredStateEntity hostComponentDesiredStateEntity : hostComponentDesiredStateEntities) {
        hostComponentDesiredStateDAO.remove(hostComponentDesiredStateEntity);
        entityManager.detach(hostComponentDesiredStateEntity);
        hostComponentDesiredStateEntity.setServiceName(AMBARI_INFRA_NEW_NAME);
        if ("INFRA_SOLR".equals(hostComponentDesiredStateEntity.getComponentName())) {
          hostComponentDesiredStateEntity.setRestartRequired(true);
        }
      }

      clusterServiceEntity.getServiceComponentDesiredStateEntities().clear();
      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity : serviceComponentDesiredStateEntities) {
        serviceComponentDesiredStateDAO.remove(serviceComponentDesiredStateEntity);
        entityManager.detach(serviceComponentDesiredStateEntity);
        serviceComponentDesiredStateEntity.setServiceName(AMBARI_INFRA_NEW_NAME);
      }

      if (serviceDesiredStateEntity != null) {
        clusterServiceEntity.setServiceDesiredStateEntity(null);
        serviceDesiredStateDAO.remove(serviceDesiredStateEntity);
        entityManager.detach(serviceDesiredStateEntity);
        serviceDesiredStateEntity.setServiceName(AMBARI_INFRA_NEW_NAME);
      }

      clusterServiceDAO.remove(clusterServiceEntity);
      entityManager.detach(clusterServiceEntity);

      clusterServiceEntity.setServiceName(AMBARI_INFRA_NEW_NAME);
      clusterServiceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);
      clusterServiceDAO.create(clusterServiceEntity);

      for (ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity : serviceComponentDesiredStateEntities)
        serviceComponentDesiredStateDAO.create(serviceComponentDesiredStateEntity);
      for (HostComponentStateEntity hostComponentStateEntity : hostComponentStateEntities)
        hostComponentStateDAO.create(hostComponentStateEntity);
      for (HostComponentDesiredStateEntity hostComponentDesiredStateEntity : hostComponentDesiredStateEntities)
        hostComponentDesiredStateDAO.create(hostComponentDesiredStateEntity);
    }

    executeInTransaction(() -> {
      TypedQuery<ServiceConfigEntity> serviceConfigUpdate = entityManager.createQuery(
              "UPDATE ServiceConfigEntity SET serviceName = :newServiceName WHERE serviceName = :oldServiceName", ServiceConfigEntity.class);
      serviceConfigUpdate.setParameter("newServiceName", AMBARI_INFRA_NEW_NAME);
      serviceConfigUpdate.setParameter("oldServiceName", AMBARI_INFRA_OLD_NAME);
      serviceConfigUpdate.executeUpdate();
    });

    executeInTransaction(() -> {
      for (final Cluster cluster : clusterMap.values()) {
        TypedQuery<AlertDefinitionEntity> alertDefinitionUpdate = entityManager.createQuery(
                "UPDATE AlertDefinitionEntity SET serviceName = :newServiceName WHERE serviceName = :oldServiceName AND clusterId = :clusterId", AlertDefinitionEntity.class);
        alertDefinitionUpdate.setParameter("clusterId", cluster.getClusterId());
        alertDefinitionUpdate.setParameter("newServiceName", AMBARI_INFRA_NEW_NAME);
        alertDefinitionUpdate.setParameter("oldServiceName", AMBARI_INFRA_OLD_NAME);
        alertDefinitionUpdate.executeUpdate();
      }
    });

    executeInTransaction(() -> {
      TypedQuery<AlertGroupEntity> alertGroupUpdate = entityManager.createQuery("UPDATE AlertGroupEntity SET serviceName = :newServiceName, groupName = :newServiceName WHERE serviceName = :oldServiceName", AlertGroupEntity.class);
      alertGroupUpdate.setParameter("newServiceName", AMBARI_INFRA_NEW_NAME);
      alertGroupUpdate.setParameter("oldServiceName", AMBARI_INFRA_OLD_NAME);
      alertGroupUpdate.executeUpdate();
    });

    executeInTransaction(() -> {
      TypedQuery<AlertHistoryEntity> alertHistoryUpdate = entityManager.createQuery("UPDATE AlertHistoryEntity SET serviceName = :newServiceName WHERE serviceName = :oldServiceName", AlertHistoryEntity.class);
      alertHistoryUpdate.setParameter("newServiceName", AMBARI_INFRA_NEW_NAME);
      alertHistoryUpdate.setParameter("oldServiceName", AMBARI_INFRA_OLD_NAME);
      alertHistoryUpdate.executeUpdate();
    });

    // Force the clusters object to reload to ensure the renamed service is accounted for
    entityManager.getEntityManagerFactory().getCache().evictAll();
    clusters.invalidateAllClusters();
  }

  @Override
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    if (artifactEntity == null) {
      return;
    }

    Map<String, Object> data = artifactEntity.getArtifactData();
    if (data == null) {
      return;
    }

    final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);
    if (kerberosDescriptor == null) {
      return;
    }

    final boolean updateInfraKerberosDescriptor = updateInfraKerberosDescriptor(kerberosDescriptor);
    final boolean updateWebHCatHostKerberosDescriptor = updateWebHCatHostKerberosDescriptor(kerberosDescriptor);
    final boolean updateYarnKerberosDescriptor = updateYarnKerberosDescriptor(kerberosDescriptor);

    if (updateInfraKerberosDescriptor || updateWebHCatHostKerberosDescriptor || updateYarnKerberosDescriptor) {
      artifactEntity.setArtifactData(kerberosDescriptor.toMap());
      artifactDAO.merge(artifactEntity);
    }
  }

  /**
   * Updates the Yarn Kerberos descriptor stored in the user-supplied Kerberos Descriptor.
   * <p>
   * Any updates will be performed on the supplied Kerberos Descriptor.
   * <p>
   * The following changes may be made:
   * <ul>
   * <li>Change the reference to rm_host to resourcemanager_hosts</li>
   * </ul>
   *
   * @param kerberosDescriptor the user-supplied Kerberos descriptor used to perform the in-place update
   * @return <code>true</code> if changes were made; otherwise <code>false</code>
   */
  private boolean updateYarnKerberosDescriptor(KerberosDescriptor kerberosDescriptor) {
    boolean updated = false;
    KerberosServiceDescriptor yarnServiceDescriptor = kerberosDescriptor.getServices().get(YARN_SERVICE);
    if (yarnServiceDescriptor != null) {
      KerberosConfigurationDescriptor coreSiteConfiguration = yarnServiceDescriptor.getConfiguration(CONFIGURATION_CORE_SITE);
      if (coreSiteConfiguration != null) {
        Map<String, String> coreSiteProperties = coreSiteConfiguration.getProperties();
        if (coreSiteProperties != null) {
          for (Map.Entry<String, String> entry : coreSiteProperties.entrySet()) {
            String value = entry.getValue();
            if (value.contains("rm_host")) {
              // changing rm_host to resourcemanager_hosts
              String newValue = value.replaceAll("rm_host", "resourcemanager_hosts");
              if (!newValue.equals(value)) {
                updated = true;
                entry.setValue(newValue);
              }
            }
          }

          if (updated) {
            // Ensure that the properties are being updated
            coreSiteConfiguration.setProperties(coreSiteProperties);
          }
        }
      }
    }

    return updated;
  }

  /**
   * Updates the Infra Kerberos descriptor stored in the user-supplied Kerberos Descriptor.
   * <p>
   * Any updates will be performed on the supplied Kerberos Descriptor.
   * <p>
   * The following changes may be made:
   * <ul>
   * <li>Rename the AMBARI_INFRA service to AMBARI_INFRA_SOLR</li>
   * </ul>
   *
   * @param kerberosDescriptor the user-supplied Kerberos descriptor used to perform the in-place update
   * @return <code>true</code> if changes were made; otherwise <code>false</code>
   */
  private boolean updateInfraKerberosDescriptor(KerberosDescriptor kerberosDescriptor) {
    boolean updated = false;

    Map<String, KerberosServiceDescriptor> services = kerberosDescriptor.getServices();
    KerberosServiceDescriptor ambariInfraService = services.get(AMBARI_INFRA_OLD_NAME);
    if (ambariInfraService != null) {
      ambariInfraService.setName(AMBARI_INFRA_NEW_NAME);
      services.remove(AMBARI_INFRA_OLD_NAME);
      services.put(AMBARI_INFRA_NEW_NAME, ambariInfraService);
      kerberosDescriptor.setServices(services);

      for (KerberosServiceDescriptor serviceDescriptor : kerberosDescriptor.getServices().values()) {
        updateKerberosIdentities(serviceDescriptor);
        if (MapUtils.isNotEmpty(serviceDescriptor.getComponents())) {
          for (KerberosComponentDescriptor componentDescriptor : serviceDescriptor.getComponents().values()) {
            updateKerberosIdentities(componentDescriptor);
          }
        }
      }

      updated = true;
    }

    return updated;
  }

  /**
   * Updates the Hive/WebHCat Kerberos descriptor stored in the user-supplied Kerberos Descriptor.
   * <p>
   * Any updates will be performed on the supplied Kerberos Descriptor.
   * <p>
   * The following changes may be made:
   * <ul>
   * <li>some command json elements were modified from ..._host to ..._hosts, kerberos related properties must be adjusted accordingly</li>
   * </ul>
   *
   * @param kerberosDescriptor the user-supplied Kerberos descriptor used to perform the in-place update
   * @return <code>true</code> if changes were made; otherwise <code>false</code>
   */
  private boolean updateWebHCatHostKerberosDescriptor(KerberosDescriptor kerberosDescriptor) {
    boolean updated = false;
    final KerberosServiceDescriptor hiveService = kerberosDescriptor.getServices().get(HiveServiceValidator.HIVE_SERVICE);
    if (hiveService != null) {
      final KerberosComponentDescriptor webhcatServer = hiveService.getComponent(HIVE_SERVICE_COMPONENT_WEBHCAT_SERVER);
      if (webhcatServer != null) {
        final KerberosConfigurationDescriptor coreSiteConfiguration = webhcatServer.getConfiguration(CONFIGURATION_CORE_SITE);
        if (coreSiteConfiguration != null) {
          final String currentHadoopProxyuserHttpHosts = coreSiteConfiguration.getProperty(PROPERTY_HADOOP_PROXYUSER_HTTP_HOSTS);
          if (StringUtils.isNotBlank(currentHadoopProxyuserHttpHosts)) {
            LOG.info("Updating hadoop.proxyuser.HTTP.hosts...");
            String newValue = currentHadoopProxyuserHttpHosts.replace("webhcat_server_host|", "webhcat_server_hosts|"); // replacing webhcat_server_host to webhcat_server_hosts
            newValue = newValue.replace("\\\\,", "\\,"); // Replacing the concatDelimiter in 'append' variable replacement function
            coreSiteConfiguration.putProperty(PROPERTY_HADOOP_PROXYUSER_HTTP_HOSTS, newValue);
            updated = true;
          }
        }
        final KerberosConfigurationDescriptor webhcatSiteConfiguration = webhcatServer.getConfiguration(CONFIGURATION_WEBHCAT_SITE);
        if (webhcatSiteConfiguration != null) {
          final String currentTempletonHiveProperties = webhcatSiteConfiguration.getProperty(PROPERTY_TEMPLETON_HIVE_PROPERTIES);
          if (StringUtils.isNotBlank(currentTempletonHiveProperties)) {
            LOG.info("Updating " + PROPERTY_TEMPLETON_HIVE_PROPERTIES + "...");
            String newValue = currentTempletonHiveProperties.replace("hive_metastore_host|", "hive_metastore_hosts|");
            newValue = newValue.replace("\\\\,", "\\,"); // Replacing the concatDelimiter in 'append' variable replacement function
            webhcatSiteConfiguration.putProperty(PROPERTY_TEMPLETON_HIVE_PROPERTIES, newValue);
            updated = true;
          }
        }
      }
    }
    return updated;
  }

  protected void addUserAuthenticationSequence() throws SQLException {
    final long maxUserAuthenticationId = fetchMaxId(USER_AUTHENTICATION_TABLE, USER_AUTHENTICATION_USER_AUTHENTICATION_ID_COLUMN);
    LOG.info("Maximum user authentication ID = " + maxUserAuthenticationId);
    addSequence("user_authentication_id_seq", maxUserAuthenticationId + 1, false);
  }

  protected void createRoleAuthorizations() throws SQLException {
    addRoleAuthorization("AMBARI.MANAGE_CONFIGURATION",
      "Manage ambari configuration",
      Collections.singleton("AMBARI.ADMINISTRATOR:AMBARI"));
  }

  protected void showHcatDeletedUserMessage() {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
      for (final Cluster cluster : clusterMap.values()) {
        Config hiveEnvConfig = cluster.getDesiredConfigByType("hive-env");
        if (hiveEnvConfig != null) {
          Map<String, String> hiveEnvProperties = hiveEnvConfig.getProperties();
          String webhcatUser = hiveEnvProperties.get("webhcat_user");
          String hcatUser = hiveEnvProperties.get("hcat_user");
          if (!StringUtils.equals(webhcatUser, hcatUser)) {
            System.out.print("WARNING: In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
            LOG.warn("In hive-env config, webhcat and hcat user are different. In current ambari release (3.0.0), hcat user was removed from stack, so potentially you could have some problems.");
          }
        }
      }
    }

  }

  protected void setStatusOfStagesAndRequests() {
    executeInTransaction(new Runnable() {
      @Override
      public void run() {
        try {
          RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
          StageFactory stageFactory = injector.getInstance(StageFactory.class);
          EntityManager em = getEntityManagerProvider().get();
          List<RequestEntity> requestEntities = requestDAO.findAll();
          for (RequestEntity requestEntity : requestEntities) {
            Collection<StageEntity> stageEntities = requestEntity.getStages();
            List<HostRoleStatus> stageDisplayStatuses = new ArrayList<>();
            List<HostRoleStatus> stageStatuses = new ArrayList<>();
            for (StageEntity stageEntity : stageEntities) {
              Stage stage = stageFactory.createExisting(stageEntity);
              List<HostRoleCommand> hostRoleCommands = stage.getOrderedHostRoleCommands();
              Map<HostRoleStatus, Integer> statusCount = CalculatedStatus.calculateStatusCountsForTasks(hostRoleCommands);
              HostRoleStatus stageDisplayStatus = CalculatedStatus.calculateSummaryDisplayStatus(statusCount, hostRoleCommands.size(), stage.isSkippable());
              HostRoleStatus stageStatus = CalculatedStatus.calculateStageStatus(hostRoleCommands, statusCount, stage.getSuccessFactors(), stage.isSkippable());
              stageEntity.setStatus(stageStatus);
              stageStatuses.add(stageStatus);
              stageEntity.setDisplayStatus(stageDisplayStatus);
              stageDisplayStatuses.add(stageDisplayStatus);
              em.merge(stageEntity);
            }
            HostRoleStatus requestStatus = CalculatedStatus.getOverallStatusForRequest(stageStatuses);
            requestEntity.setStatus(requestStatus);
            HostRoleStatus requestDisplayStatus = CalculatedStatus.getOverallDisplayStatusForRequest(stageDisplayStatuses);
            requestEntity.setDisplayStatus(requestDisplayStatus);
            em.merge(requestEntity);
          }
        } catch (Exception e) {
          LOG.warn("Setting status for stages and Requests threw exception. ", e);
        }
      }
    });
  }

  /**
   * Adds the {@value #HRC_OPS_DISPLAY_NAME_COLUMN} column to the
   * {@value #HOST_ROLE_COMMAND_TABLE} table.
   *
   * @throws SQLException
   */
  private void addOpsDisplayNameColumnToHostRoleCommand() throws SQLException {
    dbAccessor.addColumn(HOST_ROLE_COMMAND_TABLE,
      new DBAccessor.DBColumnInfo(HRC_OPS_DISPLAY_NAME_COLUMN, String.class, 255, null, true));
  }

  private void removeSecurityState() throws SQLException {
    dbAccessor.dropColumn(COMPONENT_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
    dbAccessor.dropColumn(COMPONENT_STATE_TABLE, SECURITY_STATE_COLUMN);
    dbAccessor.dropColumn(SERVICE_DESIRED_STATE_TABLE, SECURITY_STATE_COLUMN);
  }

  /**
   * Updates Log Search configs.
   *
   * @throws AmbariException
   */
  protected void updateLogSearchConfigs() throws AmbariException, SQLException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config logSearchEnv = cluster.getDesiredConfigByType("logsearch-env");

          String oldProtocolProperty = null;
          String oldPortProperty = null;
          if (logSearchEnv != null) {
            oldPortProperty = logSearchEnv.getProperties().get("logsearch_ui_port");
            oldProtocolProperty = logSearchEnv.getProperties().get("logsearch_ui_protocol");
          }

          Config logSearchProperties = cluster.getDesiredConfigByType("logsearch-properties");
          Config logFeederProperties = cluster.getDesiredConfigByType("logfeeder-properties");
          if (logSearchProperties != null && logFeederProperties != null) {
            configHelper.createConfigType(cluster, cluster.getDesiredStackVersion(), ambariManagementController,
              "logsearch-common-properties", Collections.emptyMap(), "ambari-upgrade",
              String.format("Updated logsearch-common-properties during Ambari Upgrade from %s to %s",
                getSourceVersion(), getTargetVersion()));

            String defaultLogLevels = logSearchProperties.getProperties().get("logsearch.logfeeder.include.default.level");

            Set<String> removeProperties = Sets.newHashSet("logsearch.logfeeder.include.default.level");
            removeConfigurationPropertiesFromCluster(cluster, "logsearch-properties", removeProperties);

            Map<String, String> newLogSearchProperties = new HashMap<>();
            if (oldProtocolProperty != null) {
              newLogSearchProperties.put("logsearch.protocol", oldProtocolProperty);
            }

            if (oldPortProperty != null) {
              newLogSearchProperties.put("logsearch.http.port", oldPortProperty);
              newLogSearchProperties.put("logsearch.https.port", oldPortProperty);
            }
            if (!newLogSearchProperties.isEmpty()) {
              updateConfigurationPropertiesForCluster(cluster, "logsearch-properties", newLogSearchProperties, true, true);
            }

            Map<String, String> newLogfeederProperties = new HashMap<>();
            newLogfeederProperties.put("logfeeder.include.default.level", defaultLogLevels);
            updateConfigurationPropertiesForCluster(cluster, "logfeeder-properties", newLogfeederProperties, true, true);
          }

          Config logFeederLog4jProperties = cluster.getDesiredConfigByType("logfeeder-log4j");
          if (logFeederLog4jProperties != null) {
            String content = logFeederLog4jProperties.getProperties().get("content");
            if (content.contains("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">")) {
              content = content.replace("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");
              updateConfigurationPropertiesForCluster(cluster, "logfeeder-log4j", Collections.singletonMap("content", content), true, true);
            }
          }

          Config logSearchLog4jProperties = cluster.getDesiredConfigByType("logsearch-log4j");
          if (logSearchLog4jProperties != null) {
            String content = logSearchLog4jProperties.getProperties().get("content");
            if (content.contains("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">")) {
              content = content.replace("<!DOCTYPE log4j:configuration SYSTEM \"log4j.dtd\">", "<!DOCTYPE log4j:configuration SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">");
              updateConfigurationPropertiesForCluster(cluster, "logsearch-log4j", Collections.singletonMap("content", content), true, true);
            }
          }

          removeAdminHandlersFrom(cluster, "logsearch-service_logs-solrconfig");
          removeAdminHandlersFrom(cluster, "logsearch-audit_logs-solrconfig");

          Config logFeederOutputConfig = cluster.getDesiredConfigByType("logfeeder-output-config");
          if (logFeederOutputConfig != null) {
            String content = logFeederOutputConfig.getProperties().get("content");
            content = content.replace(
              "      \"collection\":\"{{logsearch_solr_collection_service_logs}}\",\n" +
                "      \"number_of_shards\": \"{{logsearch_collection_service_logs_numshards}}\",\n" +
                "      \"splits_interval_mins\": \"{{logsearch_service_logs_split_interval_mins}}\",\n",
              "      \"type\": \"service\",\n");

            content = content.replace(
              "      \"collection\":\"{{logsearch_solr_collection_audit_logs}}\",\n" +
                "      \"number_of_shards\": \"{{logsearch_collection_audit_logs_numshards}}\",\n" +
                "      \"splits_interval_mins\": \"{{logsearch_audit_logs_split_interval_mins}}\",\n",
              "      \"type\": \"audit\",\n");

            updateConfigurationPropertiesForCluster(cluster, "logfeeder-output-config", Collections.singletonMap("content", content), true, true);
          }
          DBAccessor dba = dbAccessor != null ? dbAccessor : injector.getInstance(DBAccessor.class); // for testing
          removeLogSearchPatternConfigs(dba);
        }
      }
    }
  }

  private void removeLogSearchPatternConfigs(DBAccessor dbAccessor) throws SQLException {
    // remove config types with -logsearch-conf suffix
    String configSuffix = "-logsearch-conf";
    String serviceConfigMappingRemoveSQL = String.format(
      "DELETE FROM %s WHERE config_id IN (SELECT config_id from %s where type_name like '%%%s')",
      SERVICE_CONFIG_MAPPING_TABLE, CLUSTER_CONFIG_TABLE, configSuffix);

    String clusterConfigRemoveSQL = String.format(
      "DELETE FROM %s WHERE type_name like '%%%s'",
      CLUSTER_CONFIG_TABLE, configSuffix);

    dbAccessor.executeQuery(serviceConfigMappingRemoveSQL);
    dbAccessor.executeQuery(clusterConfigRemoveSQL);
  }

  private void removeAdminHandlersFrom(Cluster cluster, String configType) throws AmbariException {
    Config logSearchServiceLogsConfig = cluster.getDesiredConfigByType(configType);
    if (logSearchServiceLogsConfig != null) {
      String content = logSearchServiceLogsConfig.getProperties().get("content");
      if (content.contains("class=\"solr.admin.AdminHandlers\"")) {
        content = removeAdminHandlers(content);
        updateConfigurationPropertiesForCluster(cluster, configType, Collections.singletonMap("content", content), true, true);
      }
    }
  }

  protected String removeAdminHandlers(String content) {
    return content.replaceAll("(?s)<requestHandler\\s+name=\"/admin/\"\\s+class=\"solr.admin.AdminHandlers\"\\s*/>", "");
  }

  private void updateKerberosIdentities(AbstractKerberosDescriptorContainer descriptorContainer) {
    if (descriptorContainer.getIdentities() == null)
      return;
    descriptorContainer.getIdentities().stream()
            .filter(identityDescriptor -> identityDescriptor.getReference() != null && identityDescriptor.getReference().contains(AMBARI_INFRA_OLD_NAME))
            .forEach(identityDescriptor -> identityDescriptor.setReference(identityDescriptor.getReference().replace(AMBARI_INFRA_OLD_NAME, AMBARI_INFRA_NEW_NAME)));
    descriptorContainer.getIdentities().stream()
            .filter(identityDescriptor -> identityDescriptor.getWhen() != null).collect(Collectors.toList())
            .forEach(identityDescriptor -> {
              Map<String, Object> whenMap = identityDescriptor.getWhen().toMap();
              if (whenMap.containsKey("contains")) {
                List<String> serviceList = (List<String>) whenMap.get("contains");
                if (serviceList.contains(AMBARI_INFRA_OLD_NAME)) {
                  serviceList.remove(AMBARI_INFRA_OLD_NAME);
                  serviceList.add(AMBARI_INFRA_NEW_NAME);
                  identityDescriptor.setWhen(org.apache.ambari.server.collections.PredicateUtils.fromMap((Map<?, ?>) whenMap));
                }
              }
            });
  }

  protected PrepareKerberosIdentitiesServerAction getPrepareIdentityServerAction() {
    return new PrepareKerberosIdentitiesServerAction();
  }

  /**
   * Upgrades kerberos related data.
   * Also creates keytabs and principals database records. This happens via code in PrepareKerberosIdentitiesServerAction,
   * so code reused and all changes will be reflected in upgrade.
   *
   * @throws AmbariException
   */
  protected void updateKerberosConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (!MapUtils.isEmpty(clusterMap)) {
        for (Cluster cluster : clusterMap.values()) {
          Config config = cluster.getDesiredConfigByType("kerberos-env");
          if (config != null) {
            Map<String, String> properties = config.getProperties();
            if (properties.containsKey("group")) {
              // Covert kerberos-env/group to kerberos-env/ipa_user_group
              updateConfigurationPropertiesForCluster(cluster, "kerberos-env",
                Collections.singletonMap("ipa_user_group", properties.get("group")), Collections.singleton("group"),
                true, false);
            }
          }
          if (config != null) {
            PrepareKerberosIdentitiesServerAction prepareIdentities = getPrepareIdentityServerAction();
            ExecutionCommand executionCommand = new ExecutionCommand();
            executionCommand.setCommandParams(new HashMap<String, String>() {{
              put(KerberosServerAction.DEFAULT_REALM, config.getProperties().get("realm"));
            }});
            prepareIdentities.setExecutionCommand(executionCommand);

            // inject whatever we need for calling desired server action
            injector.injectMembers(prepareIdentities);
            KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

            injector.getInstance(AmbariServer.class).performStaticInjection();
            AmbariServer.setController(injector.getInstance(AmbariManagementController.class));

            KerberosDescriptor kerberosDescriptor = kerberosHelper.getKerberosDescriptor(cluster, false);
            Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();
            Map<String, Set<String>> propertiesToIgnore = new HashMap<>();
            List<ServiceComponentHost> schToProcess = kerberosHelper.getServiceComponentHostsToProcess(cluster, kerberosDescriptor, null, null);
            Map<String, Map<String, String>> configurations = kerberosHelper.calculateConfigurations(cluster, null, kerberosDescriptor, false, false);
            boolean includeAmbariIdentity = true;
            String dataDirectory = kerberosHelper.createTemporaryDirectory().getAbsolutePath();
            try {
              executeInTransaction(new Runnable() {
                @Override
                public void run() {
                  try {
                    prepareIdentities.processServiceComponentHosts(cluster, kerberosDescriptor, schToProcess, null, dataDirectory, configurations, kerberosConfigurations, includeAmbariIdentity, propertiesToIgnore);
                  } catch (AmbariException e) {
                    throw new RuntimeException(e);
                  }
                }
              });
            } catch (RuntimeException e) {
              throw new AmbariException("Failed to upgrade kerberos tables", e);
            }
          }
        }
      }
    }


  }

  /**
   * Moves SSO and LDAP related properties from ambari.properties to ambari_configuration DB table
   *
   * @throws AmbariException if there was any issue when clearing ambari.properties
   */
  protected void moveAmbariPropertiesToAmbariConfiguration() throws AmbariException {
    LOG.info("Moving LDAP and SSO related properties from ambari.properties to ambari_configuration DB table...");
    final AmbariConfigurationDAO ambariConfigurationDAO = injector.getInstance(AmbariConfigurationDAO.class);
    final Map<AmbariServerConfigurationCategory, Map<String, String>> propertiesToBeMoved = new HashMap<>();

    final Map<AmbariServerConfigurationKey, String> configurationMap = getAmbariConfigurationMap();
    configurationMap.forEach((key, oldPropertyName) -> {
      String propertyValue = configuration.getProperty(oldPropertyName);
      if (propertyValue != null) { // Empty strings are ok
        if (AmbariServerConfigurationKey.SERVER_HOST == key || AmbariServerConfigurationKey.SECONDARY_SERVER_HOST == key) {
          final HostAndPort hostAndPort = HostAndPort.fromString(propertyValue);
          AmbariServerConfigurationKey keyToBesaved = AmbariServerConfigurationKey.SERVER_HOST == key ? AmbariServerConfigurationKey.SERVER_HOST
              : AmbariServerConfigurationKey.SECONDARY_SERVER_HOST;
          populateConfigurationToBeMoved(propertiesToBeMoved, oldPropertyName, keyToBesaved, hostAndPort.getHost());

          keyToBesaved = AmbariServerConfigurationKey.SERVER_HOST == key ? AmbariServerConfigurationKey.SERVER_PORT : AmbariServerConfigurationKey.SECONDARY_SERVER_PORT;
          populateConfigurationToBeMoved(propertiesToBeMoved, oldPropertyName, keyToBesaved, String.valueOf(hostAndPort.getPort()));
        } else if (AmbariServerConfigurationKey.SSO_PROVIDER_CERTIFICATE == key) {
          // Read in the PEM file and store the PEM data rather than the file path...
          StringBuilder contentBuilder = new StringBuilder();
          try (Stream<String> stream = Files.lines(Paths.get(propertyValue), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
          } catch (IOException e) {
            LOG.error(String.format("Failed to read the SSO provider's certificate file, %s: %s", propertyValue, e.getMessage()), e);
          }
          populateConfigurationToBeMoved(propertiesToBeMoved, oldPropertyName, key, contentBuilder.toString());
        } else if (AmbariServerConfigurationKey.SSO_AUTHENTICATION_ENABLED == key) {
          populateConfigurationToBeMoved(propertiesToBeMoved, oldPropertyName, key, propertyValue);

          if("true".equalsIgnoreCase(propertyValue)) {
            // Add the new properties to tell Ambari that SSO is enabled:
            populateConfigurationToBeMoved(propertiesToBeMoved, null, AmbariServerConfigurationKey.SSO_MANAGE_SERVICES, "true");
            populateConfigurationToBeMoved(propertiesToBeMoved, null, AmbariServerConfigurationKey.SSO_ENABLED_SERVICES, "AMBARI");
          }
        } else if (AmbariServerConfigurationKey.LDAP_ENABLED == key) {
          populateConfigurationToBeMoved(propertiesToBeMoved, oldPropertyName, key, propertyValue);

          if ("true".equalsIgnoreCase(propertyValue)) {
            // Add the new properties to tell Ambari that LDAP is enabled:
            populateConfigurationToBeMoved(propertiesToBeMoved, null, AmbariServerConfigurationKey.AMBARI_MANAGES_LDAP_CONFIGURATION, "true");
            populateConfigurationToBeMoved(propertiesToBeMoved, null, AmbariServerConfigurationKey.LDAP_ENABLED_SERVICES, "AMBARI");
          }
        } else {
          populateConfigurationToBeMoved(propertiesToBeMoved, oldPropertyName, key, propertyValue);
        }
      }
    });

    if (propertiesToBeMoved.isEmpty()) {
      LOG.info("There are no properties to be moved from ambari.properties to the Ambari DB; moved 0 elements");
    } else {
      for (Map.Entry<AmbariServerConfigurationCategory, Map<String, String>> entry : propertiesToBeMoved.entrySet()) {
        Map<String, String> properties = entry.getValue();

        if (properties != null) {
          String categoryName = entry.getKey().getCategoryName();
          ambariConfigurationDAO.reconcileCategory(categoryName, entry.getValue(), false);
          LOG.info("Moved {} properties to the {} Ambari Configuration category", properties.size(), categoryName);
        }
      }

      configuration.removePropertiesFromAmbariProperties(configurationMap.values());
    }
  }

  private void populateConfigurationToBeMoved(Map<AmbariServerConfigurationCategory, Map<String, String>> propertiesToBeSaved, String oldPropertyName, AmbariServerConfigurationKey key, String value) {
    AmbariServerConfigurationCategory category = key.getConfigurationCategory();
    String newPropertyName = key.key();
    Map<String, String> categoryProperties = propertiesToBeSaved.computeIfAbsent(category, k->new HashMap<>());
    categoryProperties.put(newPropertyName, value);

    if(oldPropertyName != null) {
      LOG.info("Upgrading '{}' to '{}'", oldPropertyName, newPropertyName);
    }
  }

  /**
   * @return a map describing the new LDAP configuration key to the old ambari.properties property name
   */
  @SuppressWarnings("serial")
  private Map<AmbariServerConfigurationKey, String> getAmbariConfigurationMap() {
    Map<AmbariServerConfigurationKey, String> map = new HashMap<>();

    // LDAP-related properties
    map.put(AmbariServerConfigurationKey.LDAP_ENABLED, "ambari.ldap.isConfigured");
    map.put(AmbariServerConfigurationKey.SERVER_HOST, "authentication.ldap.primaryUrl");
    map.put(AmbariServerConfigurationKey.SECONDARY_SERVER_HOST, "authentication.ldap.secondaryUrl");
    map.put(AmbariServerConfigurationKey.USE_SSL, "authentication.ldap.useSSL");
    map.put(AmbariServerConfigurationKey.ANONYMOUS_BIND, "authentication.ldap.bindAnonymously");
    map.put(AmbariServerConfigurationKey.BIND_DN, "authentication.ldap.managerDn");
    map.put(AmbariServerConfigurationKey.BIND_PASSWORD, "authentication.ldap.managerPassword");
    map.put(AmbariServerConfigurationKey.DN_ATTRIBUTE, "authentication.ldap.dnAttribute");
    map.put(AmbariServerConfigurationKey.USER_OBJECT_CLASS, "authentication.ldap.userObjectClass");
    map.put(AmbariServerConfigurationKey.USER_NAME_ATTRIBUTE, "authentication.ldap.usernameAttribute");
    map.put(AmbariServerConfigurationKey.USER_SEARCH_BASE, "authentication.ldap.baseDn");
    map.put(AmbariServerConfigurationKey.USER_BASE, "authentication.ldap.userBase");
    map.put(AmbariServerConfigurationKey.GROUP_OBJECT_CLASS, "authentication.ldap.groupObjectClass");
    map.put(AmbariServerConfigurationKey.GROUP_NAME_ATTRIBUTE, "authentication.ldap.groupNamingAttr");
    map.put(AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE, "authentication.ldap.groupMembershipAttr");
    map.put(AmbariServerConfigurationKey.GROUP_SEARCH_BASE, "authentication.ldap.baseDn");
    map.put(AmbariServerConfigurationKey.GROUP_BASE, "authentication.ldap.groupBase");
    map.put(AmbariServerConfigurationKey.USER_SEARCH_FILTER, "authentication.ldap.userSearchFilter");
    map.put(AmbariServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN, "authentication.ldap.sync.userMemberReplacePattern");
    map.put(AmbariServerConfigurationKey.USER_MEMBER_FILTER, "authentication.ldap.sync.userMemberFilter");
    map.put(AmbariServerConfigurationKey.ALTERNATE_USER_SEARCH_ENABLED, "authentication.ldap.alternateUserSearchEnabled");
    map.put(AmbariServerConfigurationKey.ALTERNATE_USER_SEARCH_FILTER, "authentication.ldap.alternateUserSearchFilter");
    map.put(AmbariServerConfigurationKey.GROUP_SEARCH_FILTER, "authorization.ldap.groupSearchFilter");
    map.put(AmbariServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN, "authentication.ldap.sync.groupMemberReplacePattern");
    map.put(AmbariServerConfigurationKey.GROUP_MEMBER_FILTER, "authentication.ldap.sync.groupMemberFilter");
    map.put(AmbariServerConfigurationKey.GROUP_MAPPING_RULES, "authorization.ldap.adminGroupMappingRules");
    map.put(AmbariServerConfigurationKey.FORCE_LOWERCASE_USERNAMES, "authentication.ldap.username.forceLowercase");
    map.put(AmbariServerConfigurationKey.REFERRAL_HANDLING, "authentication.ldap.referral");
    map.put(AmbariServerConfigurationKey.PAGINATION_ENABLED, "authentication.ldap.pagination.enabled");
    map.put(AmbariServerConfigurationKey.COLLISION_BEHAVIOR, "ldap.sync.username.collision.behavior");

    // Added in the event a previous version of Ambari had AMBARI-24827 back-ported to it
    map.put(AmbariServerConfigurationKey.DISABLE_ENDPOINT_IDENTIFICATION, "ldap.sync.disable.endpoint.identification");

    // SSO-related properties
    map.put(AmbariServerConfigurationKey.SSO_PROVIDER_URL, "authentication.jwt.providerUrl");
    map.put(AmbariServerConfigurationKey.SSO_PROVIDER_CERTIFICATE, "authentication.jwt.publicKey");
    map.put(AmbariServerConfigurationKey.SSO_PROVIDER_ORIGINAL_URL_PARAM_NAME, "authentication.jwt.originalUrlParamName");
    map.put(AmbariServerConfigurationKey.SSO_AUTHENTICATION_ENABLED, "authentication.jwt.enabled");
    map.put(AmbariServerConfigurationKey.SSO_JWT_AUDIENCES, "authentication.jwt.audiences");
    map.put(AmbariServerConfigurationKey.SSO_JWT_COOKIE_NAME, "authentication.jwt.cookieName");

    return map;
  }

  protected void updateSolrConfigurations() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters == null)
      return;

    Map<String, Cluster> clusterMap = clusters.getClusters();
    if (clusterMap == null || clusterMap.isEmpty()) {
      return;
    }

    for (final Cluster cluster : clusterMap.values()) {
      updateConfig(cluster, "logsearch-service_logs-solrconfig", (content) -> {
        content = updateLuceneMatchVersion(content, "7.3.1");
        return updateMergeFactor(content, "logsearch_service_logs_merge_factor");
      });
      updateConfig(cluster, "logsearch-audit_logs-solrconfig", (content) -> {
        content = updateLuceneMatchVersion(content,"7.3.1");
        return updateMergeFactor(content, "logsearch_audit_logs_merge_factor");
      });
      updateConfig(cluster, "ranger-solr-configuration", (content) -> {
        content = updateLuceneMatchVersion(content,"6.6.0");
        return updateMergeFactor(content, "ranger_audit_logs_merge_factor");
      });

      updateConfig(cluster, "atlas-solrconfig",
              (content) -> updateLuceneMatchVersion(content,"6.6.0"));

      updateConfig(cluster, "infra-solr-env", this::updateInfraSolrEnv);

      updateConfig(cluster, "infra-solr-security-json", (content) ->
              content.replace("org.apache.ambari.infra.security.InfraRuleBasedAuthorizationPlugin",
                      "org.apache.solr.security.InfraRuleBasedAuthorizationPlugin"));
    }
  }

  private void updateConfig(Cluster cluster, String configType, Function<String, String> contentUpdater) throws AmbariException {
    Config config = cluster.getDesiredConfigByType(configType);
    if (config == null)
      return;
    if (config.getProperties() == null || !config.getProperties().containsKey("content"))
      return;

    String content = config.getProperties().get("content");
    content = contentUpdater.apply(content);
    updateConfigurationPropertiesForCluster(cluster, configType, Collections.singletonMap("content", content), true, true);
  }

  protected String updateLuceneMatchVersion(String content, String newLuceneMatchVersion) {
    return content.replaceAll("<luceneMatchVersion>.*</luceneMatchVersion>",
            "<luceneMatchVersion>" + newLuceneMatchVersion + "</luceneMatchVersion>");
  }

  protected String updateMergeFactor(String content, String variableName) {
    return content.replaceAll("<mergeFactor>\\{\\{" + variableName + "\\}\\}</mergeFactor>",
            "<mergePolicyFactory class=\"org.apache.solr.index.TieredMergePolicyFactory\">\n" +
                    "      <int name=\"maxMergeAtOnce\">{{" + variableName + "}}</int>\n" +
                    "      <int name=\"segmentsPerTier\">{{" + variableName + "}}</int>\n" +
                    "    </mergePolicyFactory>");
  }

  protected String updateInfraSolrEnv(String content) {
    return content.replaceAll("SOLR_KERB_NAME_RULES=\".*\"", "")
            .replaceAll("#*SOLR_HOST=\".*\"", "SOLR_HOST=`hostname -f`")
            .replaceAll("SOLR_AUTHENTICATION_CLIENT_CONFIGURER=\".*\"", "SOLR_AUTH_TYPE=\"kerberos\"");
  }

  protected void updateAmsConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Map<String, String> newProperties = new HashMap<>();
          LOG.info("Updating ams-site:timeline.metrics.service.default.result.limit to 5760");
          newProperties.put("timeline.metrics.service.default.result.limit", "5760");

          Config config = cluster.getDesiredConfigByType("ams-site");
          if (config != null) {
            Map<String, String> oldAmsSite = config.getProperties();
            if (MapUtils.isNotEmpty(oldAmsSite)) {
              if (oldAmsSite.containsKey("timeline.container-metrics.ttl")) {
                try {
                  int oldTtl = Integer.parseInt(oldAmsSite.get("timeline.container-metrics.ttl"));
                  if (oldTtl > 14 * 86400) {
                    LOG.info("Updating ams-site:timeline.container-metrics.ttl to 1209600");
                    newProperties.put("timeline.container-metrics.ttl", "1209600");
                  }
                } catch (Exception e) {
                  LOG.warn("Error updating Container metrics TTL for ams-site (AMBARI_METRICS)");
                }
              }
              String topnDownsamplerMetricPatternsKey = "timeline.metrics.downsampler.topn.metric.patterns";
              if (oldAmsSite.containsKey(topnDownsamplerMetricPatternsKey) &&
                StringUtils.isNotEmpty(oldAmsSite.get(topnDownsamplerMetricPatternsKey))) {
                LOG.info("Updating ams-site:timeline.metrics.downsampler.topn.metric.patterns to empty.");
                newProperties.put(topnDownsamplerMetricPatternsKey, "");
              }
            }
          }
          LOG.info("Removing ams-site host and aggregate cluster split points.");
          Set<String> removeProperties = Sets.newHashSet("timeline.metrics.host.aggregate.splitpoints",
            "timeline.metrics.cluster.aggregate.splitpoints");
          updateConfigurationPropertiesForCluster(cluster, "ams-site", newProperties, removeProperties, true, true);


          Map<String, String> newAmsHbaseSiteProperties = new HashMap<>();
          Config amsHBasiteSiteConfig = cluster.getDesiredConfigByType("ams-hbase-site");
          if (amsHBasiteSiteConfig != null) {
            Map<String, String> oldAmsHBaseSite = amsHBasiteSiteConfig.getProperties();
            if (MapUtils.isNotEmpty(oldAmsHBaseSite)) {
              if (oldAmsHBaseSite.containsKey("hbase.snapshot.enabled")) {
                try {
                  Boolean hbaseSnapshotEnabled = Boolean.valueOf(oldAmsHBaseSite.get("hbase.snapshot.enabled"));
                  if (!hbaseSnapshotEnabled) {
                    LOG.info("Updating ams-hbase-site:hbase.snapshot.enabled to true");
                    newAmsHbaseSiteProperties.put("hbase.snapshot.enabled", "true");
                  }
                } catch (Exception e) {
                  LOG.warn("Error updating ams-hbase-site:hbase.snapshot.enabled (AMBARI_METRICS)");
                }
              }
            }
            updateConfigurationPropertiesForCluster(cluster, "ams-hbase-site", newAmsHbaseSiteProperties, true, true);
          }

        }
      }
    }
  }

  /**
   * Removes storm-site configs that were present for ambari needs and
   * sets the actual property `storm.thrift.transport` to the correct value
   * @throws AmbariException
   */
  protected void updateStormConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        Set<String> removeProperties = Sets.newHashSet("_storm.thrift.nonsecure.transport",
          "_storm.thrift.secure.transport");
        String stormSecurityClassKey = "storm.thrift.transport";
        String stormSecurityClassValue = "org.apache.storm.security.auth.SimpleTransportPlugin";
        String stormSite = "storm-site";
        for (final Cluster cluster : clusterMap.values()) {
          Config config = cluster.getDesiredConfigByType(stormSite);
          if (config != null) {
            Map<String, String> stormSiteProperties = config.getProperties();
            if (stormSiteProperties.containsKey(stormSecurityClassKey)) {
              LOG.info("Updating " + stormSecurityClassKey);
              if (cluster.getSecurityType() == SecurityType.KERBEROS) {
                stormSecurityClassValue = "org.apache.storm.security.auth.kerberos.KerberosSaslTransportPlugin";
              }
              Map<String, String> updateProperty = Collections.singletonMap(stormSecurityClassKey, stormSecurityClassValue);
              updateConfigurationPropertiesForCluster(cluster, stormSite, updateProperty, removeProperties,
                true, false);
            }
          }
        }
      }
    }
  }

  protected void clearHadoopMetrics2Content() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        String hadoopMetrics2ContentProperty = "content";
        String hadoopMetrics2ContentValue = "";
        String hadoopMetrics2ConfigType = "hadoop-metrics2.properties";
        for (final Cluster cluster : clusterMap.values()) {
          Config config = cluster.getDesiredConfigByType(hadoopMetrics2ConfigType);
          if (config != null) {
            Map<String, String> hadoopMetrics2Configs = config.getProperties();
            if (hadoopMetrics2Configs.containsKey(hadoopMetrics2ContentProperty)) {
              LOG.info("Updating " + hadoopMetrics2ContentProperty);
              Map<String, String> updateProperty = Collections.singletonMap(hadoopMetrics2ContentProperty, hadoopMetrics2ContentValue);
              updateConfigurationPropertiesForCluster(cluster, hadoopMetrics2ConfigType, updateProperty, Collections.EMPTY_SET,
                  true, false);
            }
          }
        }
      }
    }
  }
}
