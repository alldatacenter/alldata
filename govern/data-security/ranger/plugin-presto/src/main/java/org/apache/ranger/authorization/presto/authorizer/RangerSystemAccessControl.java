/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.authorization.presto.authorizer;

import io.prestosql.spi.connector.CatalogSchemaName;
import io.prestosql.spi.connector.CatalogSchemaRoutineName;
import io.prestosql.spi.connector.CatalogSchemaTableName;
import io.prestosql.spi.connector.ColumnMetadata;
import io.prestosql.spi.connector.SchemaTableName;
import io.prestosql.spi.security.AccessDeniedException;
import io.prestosql.spi.security.PrestoPrincipal;
import io.prestosql.spi.security.Privilege;
import io.prestosql.spi.security.SystemAccessControl;
import io.prestosql.spi.security.SystemSecurityContext;
import io.prestosql.spi.security.ViewExpression;
import io.prestosql.spi.type.Type;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Locale.ENGLISH;

public class RangerSystemAccessControl
  implements SystemAccessControl {
  private static Logger LOG = LoggerFactory.getLogger(RangerSystemAccessControl.class);

  final public static String RANGER_CONFIG_KEYTAB = "ranger.keytab";
  final public static String RANGER_CONFIG_PRINCIPAL = "ranger.principal";
  final public static String RANGER_CONFIG_USE_UGI = "ranger.use_ugi";
  final public static String RANGER_CONFIG_HADOOP_CONFIG = "ranger.hadoop_config";
  final public static String RANGER_PRESTO_DEFAULT_HADOOP_CONF = "presto-ranger-site.xml";
  final public static String RANGER_PRESTO_SERVICETYPE = "presto";
  final public static String RANGER_PRESTO_APPID = "presto";

  final private RangerBasePlugin rangerPlugin;

  private boolean useUgi = false;

  public RangerSystemAccessControl(Map<String, String> config) {
    super();

    Configuration hadoopConf = new Configuration();
    if (config.get(RANGER_CONFIG_HADOOP_CONFIG) != null) {
      URL url =  hadoopConf.getResource(config.get(RANGER_CONFIG_HADOOP_CONFIG));
      if (url == null) {
        LOG.warn("Hadoop config " + config.get(RANGER_CONFIG_HADOOP_CONFIG) + " not found");
      } else {
        hadoopConf.addResource(url);
      }
    } else {
      URL url = hadoopConf.getResource(RANGER_PRESTO_DEFAULT_HADOOP_CONF);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Trying to load Hadoop config from " + url + " (can be null)");
      }
      if (url != null) {
        hadoopConf.addResource(url);
      }
    }
    UserGroupInformation.setConfiguration(hadoopConf);

    if (config.get(RANGER_CONFIG_KEYTAB) != null && config.get(RANGER_CONFIG_PRINCIPAL) != null) {
      String keytab = config.get(RANGER_CONFIG_KEYTAB);
      String principal = config.get(RANGER_CONFIG_PRINCIPAL);

      LOG.info("Performing kerberos login with principal " + principal + " and keytab " + keytab);

      try {
        UserGroupInformation.loginUserFromKeytab(principal, keytab);
      } catch (IOException ioe) {
        LOG.error("Kerberos login failed", ioe);
        throw new RuntimeException(ioe);
      }
    }

    if (config.getOrDefault(RANGER_CONFIG_USE_UGI, "false").equalsIgnoreCase("true")) {
      useUgi = true;
    }

    rangerPlugin = new RangerBasePlugin(RANGER_PRESTO_SERVICETYPE, RANGER_PRESTO_APPID);
    rangerPlugin.init();
    rangerPlugin.setResultProcessor(new RangerDefaultAuditHandler());
  }


  /** FILTERING AND DATA MASKING **/

  private RangerAccessResult getDataMaskResult(RangerPrestoAccessRequest request) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("==> getDataMaskResult(request=" + request + ")");
    }

    RangerAccessResult ret = rangerPlugin.evalDataMaskPolicies(request, null);

    if(LOG.isDebugEnabled()) {
      LOG.debug("<== getDataMaskResult(request=" + request + "): ret=" + ret);
    }

    return ret;
  }

  private RangerAccessResult getRowFilterResult(RangerPrestoAccessRequest request) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("==> getRowFilterResult(request=" + request + ")");
    }

    RangerAccessResult ret = rangerPlugin.evalRowFilterPolicies(request, null);

    if(LOG.isDebugEnabled()) {
      LOG.debug("<== getRowFilterResult(request=" + request + "): ret=" + ret);
    }

    return ret;
  }

  private boolean isDataMaskEnabled(RangerAccessResult result) {
    return result != null && result.isMaskEnabled();
  }

  private boolean isRowFilterEnabled(RangerAccessResult result) {
    return result != null && result.isRowFilterEnabled();
  }

  @Override
  public Optional<ViewExpression> getRowFilter(SystemSecurityContext context, CatalogSchemaTableName tableName) {
    RangerPrestoAccessRequest request = createAccessRequest(createResource(tableName), context, PrestoAccessType.SELECT);
    RangerAccessResult result = getRowFilterResult(request);

    ViewExpression viewExpression = null;
    if (isRowFilterEnabled(result)) {
      String filter = result.getFilterExpr();
      viewExpression = new ViewExpression(
        context.getIdentity().getUser(),
        Optional.of(tableName.getCatalogName()),
        Optional.of(tableName.getSchemaTableName().getSchemaName()),
        filter
      );
    }
    return Optional.ofNullable(viewExpression);
  }

  @Override
  public Optional<ViewExpression> getColumnMask(SystemSecurityContext context, CatalogSchemaTableName tableName, String columnName, Type type) {
    RangerPrestoAccessRequest request = createAccessRequest(
      createResource(tableName.getCatalogName(), tableName.getSchemaTableName().getSchemaName(),
        tableName.getSchemaTableName().getTableName(), Optional.of(columnName)),
      context, PrestoAccessType.SELECT);
    RangerAccessResult result = getDataMaskResult(request);

    ViewExpression viewExpression = null;
    if (isDataMaskEnabled(result)) {
      String                maskType    = result.getMaskType();
      RangerServiceDef.RangerDataMaskTypeDef maskTypeDef = result.getMaskTypeDef();
      String transformer	= null;

      if (maskTypeDef != null) {
        transformer = maskTypeDef.getTransformer();
      }

      if(StringUtils.equalsIgnoreCase(maskType, RangerPolicy.MASK_TYPE_NULL)) {
        transformer = "NULL";
      } else if(StringUtils.equalsIgnoreCase(maskType, RangerPolicy.MASK_TYPE_CUSTOM)) {
        String maskedValue = result.getMaskedValue();

        if(maskedValue == null) {
          transformer = "NULL";
        } else {
          transformer = maskedValue;
        }
      }

      if(StringUtils.isNotEmpty(transformer)) {
        transformer = transformer.replace("{col}", columnName).replace("{type}", type.getDisplayName());
      }

      viewExpression = new ViewExpression(
        context.getIdentity().getUser(),
        Optional.of(tableName.getCatalogName()),
        Optional.of(tableName.getSchemaTableName().getSchemaName()),
        transformer
      );
      if (LOG.isDebugEnabled()) {
        LOG.debug("getColumnMask: user: %s, catalog: %s, schema: %s, transformer: %s");
      }

    }

    return Optional.ofNullable(viewExpression);
  }

  @Override
  public Set<String> filterCatalogs(SystemSecurityContext context, Set<String> catalogs) {
    LOG.debug("==> RangerSystemAccessControl.filterCatalogs("+ catalogs + ")");
    Set<String> filteredCatalogs = new HashSet<>(catalogs.size());
    for (String catalog: catalogs) {
      if (hasPermission(createResource(catalog), context, PrestoAccessType.SELECT)) {
        filteredCatalogs.add(catalog);
      }
    }
    return filteredCatalogs;
  }

  @Override
  public Set<String> filterSchemas(SystemSecurityContext context, String catalogName, Set<String> schemaNames) {
    LOG.debug("==> RangerSystemAccessControl.filterSchemas(" + catalogName + ")");
    Set<String> filteredSchemaNames = new HashSet<>(schemaNames.size());
    for (String schemaName: schemaNames) {
      if (hasPermission(createResource(catalogName, schemaName), context, PrestoAccessType.SELECT)) {
        filteredSchemaNames.add(schemaName);
      }
    }
    return filteredSchemaNames;
  }

  @Override
  public Set<SchemaTableName> filterTables(SystemSecurityContext context, String catalogName, Set<SchemaTableName> tableNames) {
    LOG.debug("==> RangerSystemAccessControl.filterTables(" + catalogName + ")");
    Set<SchemaTableName> filteredTableNames = new HashSet<>(tableNames.size());
    for (SchemaTableName tableName : tableNames) {
      RangerPrestoResource res = createResource(catalogName, tableName.getSchemaName(), tableName.getTableName());
      if (hasPermission(res, context, PrestoAccessType.SELECT)) {
        filteredTableNames.add(tableName);
      }
    }
    return filteredTableNames;
  }

  /** PERMISSION CHECKS ORDERED BY SYSTEM, CATALOG, SCHEMA, TABLE, VIEW, COLUMN, QUERY, FUNCTIONS, PROCEDURES **/

  /** SYSTEM **/

  @Override
  public void checkCanSetSystemSessionProperty(SystemSecurityContext context, String propertyName) {
    if (!hasPermission(createSystemPropertyResource(propertyName), context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanSetSystemSessionProperty denied");
      AccessDeniedException.denySetSystemSessionProperty(propertyName);
    }
  }

  @Override
  public void checkCanImpersonateUser(SystemSecurityContext context, String userName) {
    if (!hasPermission(createUserResource(userName), context, PrestoAccessType.IMPERSONATE)) {
      LOG.debug("RangerSystemAccessControl.checkCanImpersonateUser(" + userName + ") denied");
      AccessDeniedException.denyImpersonateUser(context.getIdentity().getUser(), userName);
    }
  }

  @Override
  public void checkCanSetUser(Optional<Principal> principal, String userName) {
    // pass as it is deprecated
  }

  /** CATALOG **/
  @Override
  public void checkCanSetCatalogSessionProperty(SystemSecurityContext context, String catalogName, String propertyName) {
    if (!hasPermission(createCatalogSessionResource(catalogName, propertyName), context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanSetCatalogSessionProperty(" + catalogName + ") denied");
      AccessDeniedException.denySetCatalogSessionProperty(catalogName, propertyName);
    }
  }

  @Override
  public void checkCanShowRoles(SystemSecurityContext context, String catalogName) {
    if (!hasPermission(createResource(catalogName), context, PrestoAccessType.SHOW)) {
      LOG.debug("RangerSystemAccessControl.checkCanShowRoles(" + catalogName + ") denied");
      AccessDeniedException.denyShowRoles(catalogName);
    }
  }


  @Override
  public void checkCanAccessCatalog(SystemSecurityContext context, String catalogName) {
    if (!hasPermission(createResource(catalogName), context, PrestoAccessType.USE)) {
      LOG.debug("RangerSystemAccessControl.checkCanAccessCatalog(" + catalogName + ") denied");
      AccessDeniedException.denyCatalogAccess(catalogName);
    }
  }

  @Override
  public void checkCanShowSchemas(SystemSecurityContext context, String catalogName) {
    if (!hasPermission(createResource(catalogName), context, PrestoAccessType.SHOW)) {
      LOG.debug("RangerSystemAccessControl.checkCanShowSchemas(" + catalogName + ") denied");
      AccessDeniedException.denyShowSchemas(catalogName);
    }
  }

  /** SCHEMA **/

  @Override
  public void checkCanSetSchemaAuthorization(SystemSecurityContext context, CatalogSchemaName schema, PrestoPrincipal principal) {
    if (!hasPermission(createResource(schema.getCatalogName(), schema.getSchemaName()), context, PrestoAccessType.GRANT)) {
      LOG.debug("RangerSystemAccessControl.checkCanSetSchemaAuthorization(" + schema.getSchemaName() + ") denied");
      AccessDeniedException.denySetSchemaAuthorization(schema.getSchemaName(), principal);
    }
  }

  @Override
  public void checkCanShowCreateSchema(SystemSecurityContext context, CatalogSchemaName schema) {
    if (!hasPermission(createResource(schema.getCatalogName(), schema.getSchemaName()), context, PrestoAccessType.SHOW)) {
      LOG.debug("RangerSystemAccessControl.checkCanShowCreateSchema(" + schema.getSchemaName() + ") denied");
      AccessDeniedException.denyShowCreateSchema(schema.getSchemaName());
    }
  }

  /**
   * Create schema is evaluated on the level of the Catalog. This means that it is assumed you have permission
   * to create a schema when you have create rights on the catalog level
   */
  @Override
  public void checkCanCreateSchema(SystemSecurityContext context, CatalogSchemaName schema) {
    if (!hasPermission(createResource(schema.getCatalogName()), context, PrestoAccessType.CREATE)) {
      LOG.debug("RangerSystemAccessControl.checkCanCreateSchema(" + schema.getSchemaName() + ") denied");
      AccessDeniedException.denyCreateSchema(schema.getSchemaName());
    }
  }

  /**
   * This is evaluated against the schema name as ownership information is not available
   */
  @Override
  public void checkCanDropSchema(SystemSecurityContext context, CatalogSchemaName schema) {
    if (!hasPermission(createResource(schema.getCatalogName(), schema.getSchemaName()), context, PrestoAccessType.DROP)) {
      LOG.debug("RangerSystemAccessControl.checkCanDropSchema(" + schema.getSchemaName() + ") denied");
      AccessDeniedException.denyDropSchema(schema.getSchemaName());
    }
  }

  /**
   * This is evaluated against the schema name as ownership information is not available
   */
  @Override
  public void checkCanRenameSchema(SystemSecurityContext context, CatalogSchemaName schema, String newSchemaName) {
    RangerPrestoResource res = createResource(schema.getCatalogName(), schema.getSchemaName());
    if (!hasPermission(res, context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanRenameSchema(" + schema.getSchemaName() + ") denied");
      AccessDeniedException.denyRenameSchema(schema.getSchemaName(), newSchemaName);
    }
  }

  /** TABLE **/

  @Override
  public void checkCanShowTables(SystemSecurityContext context, CatalogSchemaName schema) {
    if (!hasPermission(createResource(schema), context, PrestoAccessType.SHOW)) {
      LOG.debug("RangerSystemAccessControl.checkCanShowTables(" + schema.toString() + ") denied");
      AccessDeniedException.denyShowTables(schema.toString());
    }
  }


  @Override
  public void checkCanShowCreateTable(SystemSecurityContext context, CatalogSchemaTableName table) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.SHOW)) {
      LOG.debug("RangerSystemAccessControl.checkCanShowTables(" + table.toString() + ") denied");
      AccessDeniedException.denyShowCreateTable(table.toString());
    }
  }

  /**
   * Create table is verified on schema level
   */
  @Override
  public void checkCanCreateTable(SystemSecurityContext context, CatalogSchemaTableName table) {
    if (!hasPermission(createResource(table.getCatalogName(), table.getSchemaTableName().getSchemaName()), context, PrestoAccessType.CREATE)) {
      LOG.debug("RangerSystemAccessControl.checkCanCreateTable(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyCreateTable(table.getSchemaTableName().getTableName());
    }
  }

  /**
   * This is evaluated against the table name as ownership information is not available
   */
  @Override
  public void checkCanDropTable(SystemSecurityContext context, CatalogSchemaTableName table) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.DROP)) {
      LOG.debug("RangerSystemAccessControl.checkCanDropTable(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyDropTable(table.getSchemaTableName().getTableName());
    }
  }

  /**
   * This is evaluated against the table name as ownership information is not available
   */
  @Override
  public void checkCanRenameTable(SystemSecurityContext context, CatalogSchemaTableName table, CatalogSchemaTableName newTable) {
    RangerPrestoResource res = createResource(table);
    if (!hasPermission(res, context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanRenameTable(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyRenameTable(table.getSchemaTableName().getTableName(), newTable.getSchemaTableName().getTableName());
    }
  }

  @Override
  public void checkCanInsertIntoTable(SystemSecurityContext context, CatalogSchemaTableName table) {
    RangerPrestoResource res = createResource(table);
    if (!hasPermission(res, context, PrestoAccessType.INSERT)) {
      LOG.debug("RangerSystemAccessControl.checkCanInsertIntoTable(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyInsertTable(table.getSchemaTableName().getTableName());
    }
  }

  @Override
  public void checkCanDeleteFromTable(SystemSecurityContext context, CatalogSchemaTableName table) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.DELETE)) {
      LOG.debug("RangerSystemAccessControl.checkCanDeleteFromTable(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyDeleteTable(table.getSchemaTableName().getTableName());
    }
  }

  @Override
  public void checkCanGrantTablePrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaTableName table, PrestoPrincipal grantee, boolean withGrantOption) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.GRANT)) {
      LOG.debug("RangerSystemAccessControl.checkCanGrantTablePrivilege(" + table + ") denied");
      AccessDeniedException.denyGrantTablePrivilege(privilege.toString(), table.toString());
    }
  }

  @Override
  public void checkCanRevokeTablePrivilege(SystemSecurityContext context, Privilege privilege, CatalogSchemaTableName table, PrestoPrincipal revokee, boolean grantOptionFor) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.REVOKE)) {
      LOG.debug("RangerSystemAccessControl.checkCanRevokeTablePrivilege(" + table + ") denied");
      AccessDeniedException.denyRevokeTablePrivilege(privilege.toString(), table.toString());
    }
  }

  @Override
  public void checkCanSetTableComment(SystemSecurityContext context, CatalogSchemaTableName table) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanSetTableComment(" + table.toString() + ") denied");
      AccessDeniedException.denyCommentTable(table.toString());
    }
  }

  /**
   * Create view is verified on schema level
   */
  @Override
  public void checkCanCreateView(SystemSecurityContext context, CatalogSchemaTableName view) {
    if (!hasPermission(createResource(view.getCatalogName(), view.getSchemaTableName().getSchemaName()), context, PrestoAccessType.CREATE)) {
      LOG.debug("RangerSystemAccessControl.checkCanCreateView(" + view.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyCreateView(view.getSchemaTableName().getTableName());
    }
  }

  /**
   * This is evaluated against the table name as ownership information is not available
   */
  @Override
  public void checkCanDropView(SystemSecurityContext context, CatalogSchemaTableName view) {
    if (!hasPermission(createResource(view), context, PrestoAccessType.DROP)) {
      LOG.debug("RangerSystemAccessControl.checkCanDropView(" + view.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyDropView(view.getSchemaTableName().getTableName());
    }
  }

  /**
   * This check equals the check for checkCanCreateView
   */
  @Override
  public void checkCanCreateViewWithSelectFromColumns(SystemSecurityContext context, CatalogSchemaTableName table, Set<String> columns) {
    try {
      checkCanCreateView(context, table);
    } catch (AccessDeniedException ade) {
      LOG.debug("RangerSystemAccessControl.checkCanCreateViewWithSelectFromColumns(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyCreateViewWithSelect(table.getSchemaTableName().getTableName(), context.getIdentity());
    }
  }

  /**
   * This is evaluated against the table name as ownership information is not available
   */
  @Override
  public void checkCanRenameView(SystemSecurityContext context, CatalogSchemaTableName view, CatalogSchemaTableName newView) {
    if (!hasPermission(createResource(view), context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanRenameView(" + view.toString() + ") denied");
      AccessDeniedException.denyRenameView(view.toString(), newView.toString());
    }
  }

  /** COLUMN **/

  /**
   * This is evaluated on table level
   */
  @Override
  public void checkCanAddColumn(SystemSecurityContext context, CatalogSchemaTableName table) {
    RangerPrestoResource res = createResource(table);
    if (!hasPermission(res, context, PrestoAccessType.ALTER)) {
      AccessDeniedException.denyAddColumn(table.getSchemaTableName().getTableName());
    }
  }

  /**
   * This is evaluated on table level
   */
  @Override
  public void checkCanDropColumn(SystemSecurityContext context, CatalogSchemaTableName table) {
    RangerPrestoResource res = createResource(table);
    if (!hasPermission(res, context, PrestoAccessType.DROP)) {
      LOG.debug("RangerSystemAccessControl.checkCanDropColumn(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyDropColumn(table.getSchemaTableName().getTableName());
    }
  }

  /**
   * This is evaluated on table level
   */
  @Override
  public void checkCanRenameColumn(SystemSecurityContext context, CatalogSchemaTableName table) {
    RangerPrestoResource res = createResource(table);
    if (!hasPermission(res, context, PrestoAccessType.ALTER)) {
      LOG.debug("RangerSystemAccessControl.checkCanRenameColumn(" + table.getSchemaTableName().getTableName() + ") denied");
      AccessDeniedException.denyRenameColumn(table.getSchemaTableName().getTableName());
    }
  }

  /**
   * This is evaluated on table level
   */
  @Override
  public void checkCanShowColumns(SystemSecurityContext context, CatalogSchemaTableName table) {
    if (!hasPermission(createResource(table), context, PrestoAccessType.SHOW)) {
      LOG.debug("RangerSystemAccessControl.checkCanShowTables(" + table.toString() + ") denied");
      AccessDeniedException.denyShowColumns(table.toString());
    }
  }

  @Override
  public void checkCanSelectFromColumns(SystemSecurityContext context, CatalogSchemaTableName table, Set<String> columns) {
    for (RangerPrestoResource res : createResource(table, columns)) {
      if (!hasPermission(res, context, PrestoAccessType.SELECT)) {
        LOG.debug("RangerSystemAccessControl.checkCanSelectFromColumns(" + table.getSchemaTableName().getTableName() + ") denied");
        AccessDeniedException.denySelectColumns(table.getSchemaTableName().getTableName(), columns);
      }
    }
  }

  /**
   * This is a NOOP, no filtering is applied
   */
  @Override
  public List<ColumnMetadata> filterColumns(SystemSecurityContext context, CatalogSchemaTableName table, List<ColumnMetadata> columns) {
    return columns;
  }

  /** QUERY **/

  /**
   * This is a NOOP. Everyone can execute a query
   * @param context
   */
  @Override
  public void checkCanExecuteQuery(SystemSecurityContext context) {
  }

  @Override
  public void checkCanViewQueryOwnedBy(SystemSecurityContext context, String queryOwner) {
    if (!hasPermission(createUserResource(queryOwner), context, PrestoAccessType.IMPERSONATE)) {
      LOG.debug("RangerSystemAccessControl.checkCanViewQueryOwnedBy(" + queryOwner + ") denied");
      AccessDeniedException.denyImpersonateUser(context.getIdentity().getUser(), queryOwner);
    }
  }

  /**
   * This is a NOOP, no filtering is applied
   */
  @Override
  public Set<String> filterViewQueryOwnedBy(SystemSecurityContext context, Set<String> queryOwners) {
    return queryOwners;
  }

  @Override
  public void checkCanKillQueryOwnedBy(SystemSecurityContext context, String queryOwner) {
    if (!hasPermission(createUserResource(queryOwner), context, PrestoAccessType.IMPERSONATE)) {
      LOG.debug("RangerSystemAccessControl.checkCanKillQueryOwnedBy(" + queryOwner + ") denied");
      AccessDeniedException.denyImpersonateUser(context.getIdentity().getUser(), queryOwner);
    }
  }

  /** FUNCTIONS **/
  @Override
  public void checkCanGrantExecuteFunctionPrivilege(SystemSecurityContext context, String function, PrestoPrincipal grantee, boolean grantOption) {
    if (!hasPermission(createFunctionResource(function), context, PrestoAccessType.GRANT)) {
      LOG.debug("RangerSystemAccessControl.checkCanGrantExecuteFunctionPrivilege(" + function + ") denied");
      AccessDeniedException.denyGrantExecuteFunctionPrivilege(function, context.getIdentity(), grantee.getName());
    }
  }

  @Override
  public void checkCanExecuteFunction(SystemSecurityContext context, String function) {
    if (!hasPermission(createFunctionResource(function), context, PrestoAccessType.EXECUTE)) {
      LOG.debug("RangerSystemAccessControl.checkCanExecuteFunction(" + function + ") denied");
      AccessDeniedException.denyExecuteFunction(function);
    }
  }

  /** PROCEDURES **/
  @Override
  public void checkCanExecuteProcedure(SystemSecurityContext context, CatalogSchemaRoutineName procedure) {
    if (!hasPermission(createProcedureResource(procedure), context, PrestoAccessType.EXECUTE)) {
      LOG.debug("RangerSystemAccessControl.checkCanExecuteFunction(" + procedure.getSchemaRoutineName().getRoutineName() + ") denied");
      AccessDeniedException.denyExecuteProcedure(procedure.getSchemaRoutineName().getRoutineName());
    }
  }

  /** HELPER FUNCTIONS **/

  private RangerPrestoAccessRequest createAccessRequest(RangerPrestoResource resource, SystemSecurityContext context, PrestoAccessType accessType) {
	String userName = null;
	Set<String> userGroups = null;

    if (useUgi) {
      UserGroupInformation ugi = UserGroupInformation.createRemoteUser(context.getIdentity().getUser());

      userName = ugi.getShortUserName();
      String[] groups = ugi != null ? ugi.getGroupNames() : null;

      if (groups != null && groups.length > 0) {
        userGroups = new HashSet<>(Arrays.asList(groups));
      }
    } else {
      userName = context.getIdentity().getUser();
      userGroups = context.getIdentity().getGroups();
    }

    RangerPrestoAccessRequest request = new RangerPrestoAccessRequest(
      resource,
      userName,
      userGroups,
      accessType
    );

    return request;
  }

  private boolean hasPermission(RangerPrestoResource resource, SystemSecurityContext context, PrestoAccessType accessType) {
    boolean ret = false;

    RangerPrestoAccessRequest request = createAccessRequest(resource, context, accessType);

    RangerAccessResult result = rangerPlugin.isAccessAllowed(request);
    if (result != null && result.getIsAllowed()) {
      ret = true;
    }

    return ret;
  }

  private static RangerPrestoResource createUserResource(String userName) {
    RangerPrestoResource res = new RangerPrestoResource();
    res.setValue(RangerPrestoResource.KEY_USER, userName);

    return res;
  }

  private static RangerPrestoResource createFunctionResource(String function) {
    RangerPrestoResource res = new RangerPrestoResource();
    res.setValue(RangerPrestoResource.KEY_FUNCTION, function);

    return res;
  }

  private static RangerPrestoResource createProcedureResource(CatalogSchemaRoutineName procedure) {
    RangerPrestoResource res = new RangerPrestoResource();
    res.setValue(RangerPrestoResource.KEY_CATALOG, procedure.getCatalogName());
    res.setValue(RangerPrestoResource.KEY_SCHEMA, procedure.getSchemaRoutineName().getSchemaName());
    res.setValue(RangerPrestoResource.KEY_PROCEDURE, procedure.getSchemaRoutineName().getRoutineName());

    return res;
  }

  private static RangerPrestoResource createCatalogSessionResource(String catalogName, String propertyName) {
    RangerPrestoResource res = new RangerPrestoResource();
    res.setValue(RangerPrestoResource.KEY_CATALOG, catalogName);
    res.setValue(RangerPrestoResource.KEY_SESSION_PROPERTY, propertyName);

    return res;
  }

  private static RangerPrestoResource createSystemPropertyResource(String property) {
    RangerPrestoResource res = new RangerPrestoResource();
    res.setValue(RangerPrestoResource.KEY_SYSTEM_PROPERTY, property);

    return res;
  }

  private static RangerPrestoResource createResource(CatalogSchemaName catalogSchemaName) {
    return createResource(catalogSchemaName.getCatalogName(), catalogSchemaName.getSchemaName());
  }

  private static RangerPrestoResource createResource(CatalogSchemaTableName catalogSchemaTableName) {
    return createResource(catalogSchemaTableName.getCatalogName(),
      catalogSchemaTableName.getSchemaTableName().getSchemaName(),
      catalogSchemaTableName.getSchemaTableName().getTableName());
  }

  private static RangerPrestoResource createResource(String catalogName) {
    return new RangerPrestoResource(catalogName, Optional.empty(), Optional.empty());
  }

  private static RangerPrestoResource createResource(String catalogName, String schemaName) {
    return new RangerPrestoResource(catalogName, Optional.of(schemaName), Optional.empty());
  }

  private static RangerPrestoResource createResource(String catalogName, String schemaName, final String tableName) {
    return new RangerPrestoResource(catalogName, Optional.of(schemaName), Optional.of(tableName));
  }

  private static RangerPrestoResource createResource(String catalogName, String schemaName, final String tableName, final Optional<String> column) {
    return new RangerPrestoResource(catalogName, Optional.of(schemaName), Optional.of(tableName), column);
  }

  private static List<RangerPrestoResource> createResource(CatalogSchemaTableName table, Set<String> columns) {
    List<RangerPrestoResource> colRequests = new ArrayList<>();

    if (columns.size() > 0) {
      for (String column : columns) {
        RangerPrestoResource rangerPrestoResource = createResource(table.getCatalogName(),
          table.getSchemaTableName().getSchemaName(),
          table.getSchemaTableName().getTableName(), Optional.of(column));
        colRequests.add(rangerPrestoResource);
      }
    } else {
      colRequests.add(createResource(table.getCatalogName(),
        table.getSchemaTableName().getSchemaName(),
        table.getSchemaTableName().getTableName(), Optional.empty()));
    }
    return colRequests;
  }
}

class RangerPrestoResource
  extends RangerAccessResourceImpl {


  public static final String KEY_CATALOG = "catalog";
  public static final String KEY_SCHEMA = "schema";
  public static final String KEY_TABLE = "table";
  public static final String KEY_COLUMN = "column";
  public static final String KEY_USER = "prestouser";
  public static final String KEY_FUNCTION = "function";
  public static final String KEY_PROCEDURE = "procedure";
  public static final String KEY_SYSTEM_PROPERTY = "systemproperty";
  public static final String KEY_SESSION_PROPERTY = "sessionproperty";

  public RangerPrestoResource() {
  }

  public RangerPrestoResource(String catalogName, Optional<String> schema, Optional<String> table) {
    setValue(KEY_CATALOG, catalogName);
    if (schema.isPresent()) {
      setValue(KEY_SCHEMA, schema.get());
    }
    if (table.isPresent()) {
      setValue(KEY_TABLE, table.get());
    }
  }

  public RangerPrestoResource(String catalogName, Optional<String> schema, Optional<String> table, Optional<String> column) {
    setValue(KEY_CATALOG, catalogName);
    if (schema.isPresent()) {
      setValue(KEY_SCHEMA, schema.get());
    }
    if (table.isPresent()) {
      setValue(KEY_TABLE, table.get());
    }
    if (column.isPresent()) {
      setValue(KEY_COLUMN, column.get());
    }
  }

  public String getCatalogName() {
    return (String) getValue(KEY_CATALOG);
  }

  public String getTable() {
    return (String) getValue(KEY_TABLE);
  }

  public String getCatalog() {
    return (String) getValue(KEY_CATALOG);
  }

  public String getSchema() {
    return (String) getValue(KEY_SCHEMA);
  }

  public Optional<SchemaTableName> getSchemaTable() {
    final String schema = getSchema();
    if (StringUtils.isNotEmpty(schema)) {
      return Optional.of(new SchemaTableName(schema, Optional.ofNullable(getTable()).orElse("*")));
    }
    return Optional.empty();
  }
}

class RangerPrestoAccessRequest
  extends RangerAccessRequestImpl {
  public RangerPrestoAccessRequest(RangerPrestoResource resource,
                                   String user,
                                   Set<String> userGroups,
                                   PrestoAccessType prestoAccessType) {
    super(resource, prestoAccessType.name().toLowerCase(ENGLISH), user, userGroups, null);
    setAccessTime(new Date());
  }
}

enum PrestoAccessType {
  CREATE, DROP, SELECT, INSERT, DELETE, USE, ALTER, ALL, GRANT, REVOKE, SHOW, IMPERSONATE, EXECUTE;
}