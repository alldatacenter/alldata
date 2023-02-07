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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.rpc.user;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.tools.ValidationException;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.handlers.SqlHandlerUtil;
import org.apache.drill.exec.proto.UserBitShared.UserCredentials;
import org.apache.drill.exec.proto.UserProtos.UserProperties;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.SessionOptionManager;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSession implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(UserSession.class);

  private boolean supportComplexTypes;
  private UserCredentials credentials;
  private DrillProperties properties;
  private SessionOptionManager sessionOptions;
  private final AtomicInteger queryCount;
  private final String sessionId;

  /** Stores list of temporary tables, key is original table name converted to lower case to achieve case-insensitivity,
   *  value is generated table name. **/
  private final ConcurrentMap<String, String> temporaryTables;
  /** Stores list of session temporary locations, key is path to location, value is file system associated with location. **/
  private final ConcurrentMap<Path, FileSystem> temporaryLocations;

  /** On session close deletes all session temporary locations recursively and clears temporary locations list. */
  @Override
  public void close() {
    for (Map.Entry<Path, FileSystem> entry : temporaryLocations.entrySet()) {
      Path path = entry.getKey();
      FileSystem fs = entry.getValue();
      try {
        fs.delete(path, true);
        logger.info("Deleted session temporary location [{}] from file system [{}]",
            path.toUri().getPath(), fs.getUri());
      } catch (Exception e) {
        logger.warn("Error during session temporary location [{}] deletion from file system [{}]: [{}]",
            path.toUri().getPath(), fs.getUri(), e.getMessage());
      }
    }
    temporaryLocations.clear();
  }

  /**
   * Implementations of this interface are allowed to increment queryCount.
   * {@link org.apache.drill.exec.work.user.UserWorker} should have a member that implements the interface.
   * No other core class should implement this interface. Test classes may implement (see ControlsInjectionUtil).
   */
  public interface QueryCountIncrementer {
    void increment(final UserSession session);
  }

  public static class Builder {
    private UserSession userSession;

    public static Builder newBuilder() {
      return new Builder();
    }

    public Builder withCredentials(UserCredentials credentials) {
      userSession.credentials = credentials;
      return this;
    }

    public Builder withOptionManager(OptionManager systemOptions) {
      userSession.sessionOptions = new SessionOptionManager(systemOptions, userSession);
      return this;
    }

    public Builder withUserProperties(UserProperties properties) {
      userSession.properties = DrillProperties.createFromProperties(properties, false);
      return this;
    }

    public Builder setSupportComplexTypes(boolean supportComplexTypes) {
      userSession.supportComplexTypes = supportComplexTypes;
      return this;
    }

    private boolean canApplyUserProperty() {
      final StringBuilder sb = new StringBuilder();
      if (userSession.properties.containsKey(DrillProperties.QUOTING_IDENTIFIERS)) {
        sb.append(DrillProperties.QUOTING_IDENTIFIERS).append(",");
      }

      if (userSession.properties.containsKey(DrillProperties.QUERY_TAGS)) {
        sb.append(DrillProperties.QUERY_TAGS);
      }

      if (userSession.sessionOptions == null && sb.length() > 0) {
        logger.warn("User property {} can't be installed as a server option without the session option manager",
          sb.toString());
        return false;
      }
      return true;
    }

    public UserSession build() {
      if (canApplyUserProperty()) {
        if (userSession.properties.containsKey(DrillProperties.QUOTING_IDENTIFIERS)) {
          userSession.setSessionOption(PlannerSettings.QUOTING_IDENTIFIERS_KEY,
            userSession.properties.getProperty(DrillProperties.QUOTING_IDENTIFIERS));
        }

        if (userSession.properties.containsKey(DrillProperties.QUERY_TAGS)) {
          userSession.setSessionOption(ExecConstants.RM_QUERY_TAGS_KEY,
            userSession.properties.getProperty(DrillProperties.QUERY_TAGS));
        }
      }
      UserSession session = userSession;
      userSession = null;
      return session;
    }

    Builder() {
      userSession = new UserSession();
    }
  }

  private UserSession() {
    queryCount = new AtomicInteger(0);
    sessionId = UUID.randomUUID().toString();
    temporaryTables = Maps.newConcurrentMap();
    temporaryLocations = Maps.newConcurrentMap();
    properties = DrillProperties.createEmpty();
  }

  public boolean isSupportComplexTypes() {
    return supportComplexTypes;
  }

  public SessionOptionManager getOptions() {
    return sessionOptions;
  }

  public UserCredentials getCredentials() {
    return credentials;
  }

  /**
   * Replace current user credentials with the given user's credentials. Meant to be called only by a
   * {@link InboundImpersonationManager impersonation manager}.
   *
   * @param impersonationManager impersonation manager making this call
   * @param newCredentials user credentials to change to
   */
  public void replaceUserCredentials(final InboundImpersonationManager impersonationManager,
                                     final UserCredentials newCredentials) {
    Preconditions.checkNotNull(impersonationManager, "User credentials can only be replaced by an" +
        " impersonation manager.");
    credentials = newCredentials;
  }

  public String getTargetUserName() {
    return properties.getProperty(DrillProperties.IMPERSONATION_TARGET);
  }

  public void incrementQueryCount(final QueryCountIncrementer incrementer) {
    assert incrementer != null;
    queryCount.incrementAndGet();
  }

  public int getQueryCount() {
    return queryCount.get();
  }

  /**
   * Update the schema path for the session.
   * @param newDefaultSchemaPath New default schema path to set. It could be relative to the current default schema or
   *                             absolute schema.
   * @param currentDefaultSchema Current default schema.
   * @throws ValidationException If the given default schema path is invalid in current schema tree.
   */
  public void setDefaultSchemaPath(String newDefaultSchemaPath, SchemaPlus currentDefaultSchema)
      throws ValidationException {
    final List<String> newDefaultPathAsList = SchemaUtilites.getSchemaPathAsList(newDefaultSchemaPath);
    SchemaPlus newDefault;

    // First try to find the given schema relative to the current default schema.
    newDefault = SchemaUtilites.findSchema(currentDefaultSchema, newDefaultPathAsList);

    if (newDefault == null) {
      // If we fail to find the schema relative to current default schema, consider the given new default schema path as
      // absolute schema path.
      newDefault = SchemaUtilites.findSchema(currentDefaultSchema, newDefaultPathAsList);
    }

    if (newDefault == null) {
      SchemaUtilites.throwSchemaNotFoundException(currentDefaultSchema, newDefaultSchemaPath);
    }

    properties.setProperty(DrillProperties.SCHEMA, SchemaUtilites.getSchemaPath(newDefault));
  }

  /**
   * @return Get current default schema path.
   */
  public String getDefaultSchemaPath() {
    return properties.getProperty(DrillProperties.SCHEMA, "");
  }

  /**
   * Get default schema from current default schema path and given schema tree.
   * @param rootSchema root schema
   * @return A {@link org.apache.calcite.schema.SchemaPlus} object.
   */
  public SchemaPlus getDefaultSchema(SchemaPlus rootSchema) {
    final String defaultSchemaPath = getDefaultSchemaPath();

    if (Strings.isNullOrEmpty(defaultSchemaPath)) {
      return null;
    }

    return SchemaUtilites.findSchema(rootSchema, defaultSchemaPath);
  }

  /**
   * Set the option of a session level.
   * Note: Option's kind is automatically detected if such option exists.
   *
   * @param name option name
   * @param value option value
   */
  public void setSessionOption(String name, String value) {
    sessionOptions.setLocalOption(name, value);
  }

  /**
   * @return unique session identifier
   */
  public String getSessionId() { return sessionId; }

  /**
   * Creates and adds session temporary location if absent using schema configuration.
   * Before any actions, checks if passed table schema is valid default temporary workspace.
   * Generates temporary table name and stores it's original name as key
   * and generated name as value in  session temporary tables cache.
   * Original temporary name is converted to lower case to achieve case-insensitivity.
   * If original table name already exists, new name is not regenerated and is reused.
   * This can happen if default temporary workspace was changed (file system or location) or
   * orphan temporary table name has remained (name was registered but table creation did not succeed).
   *
   * @param schema table schema
   * @param tableName original table name
   * @param config drill config
   * @return generated temporary table name
   * @throws IOException if error during session temporary location creation
   */
  public String registerTemporaryTable(AbstractSchema schema, String tableName, DrillConfig config) throws IOException {
    addTemporaryLocation(SchemaUtilites.resolveToValidTemporaryWorkspace(schema, config));
    String temporaryTableName = new Path(sessionId, UUID.randomUUID().toString()).toUri().getPath();
    String oldTemporaryTableName = temporaryTables.putIfAbsent(tableName.toLowerCase(), temporaryTableName);
    return oldTemporaryTableName == null ? temporaryTableName : oldTemporaryTableName;
  }

  /**
   * Returns generated temporary table name from the list of session temporary tables, null otherwise.
   * Original temporary name is converted to lower case to achieve case-insensitivity.
   *
   * @param tableName original table name
   * @return generated temporary table name
   */
  public String resolveTemporaryTableName(String tableName) {
    return temporaryTables.get(tableName.toLowerCase());
  }

  public String getOriginalTableNameFromTemporaryTable(String tableName) {
    for (String originalTableName : temporaryTables.keySet()) {
      if (temporaryTables.get(originalTableName).equals(tableName)) {
        return originalTableName;
      }
    }
    return null;
  }

  /**
   * Checks if passed table is temporary, table name is case-insensitive.
   * Before looking for table checks if passed schema is temporary and returns false if not
   * since temporary tables are allowed to be created in temporary workspace only.
   * If passed workspace is temporary, looks for temporary table.
   * First checks if table name is among temporary tables, if not returns false.
   * If temporary table named was resolved, checks that temporary table exists on disk,
   * to ensure that temporary table actually exists and resolved table name is not orphan
   * (for example, in result of unsuccessful temporary table creation).
   *
   * @param drillSchema table schema
   * @param config drill config
   * @param tableName original table name
   * @return true if temporary table exists in schema, false otherwise
   */
  public boolean isTemporaryTable(AbstractSchema drillSchema, DrillConfig config, String tableName) {
    if (drillSchema == null || !SchemaUtilites.isTemporaryWorkspace(drillSchema.getFullSchemaName(), config)) {
      return false;
    }
    String temporaryTableName = resolveTemporaryTableName(tableName);
    if (temporaryTableName != null) {
      Table temporaryTable = SqlHandlerUtil.getTableFromSchema(drillSchema, temporaryTableName);
      if (temporaryTable != null && temporaryTable.getJdbcTableType() == Schema.TableType.TABLE) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes temporary table name from the list of session temporary tables.
   * Original temporary name is converted to lower case to achieve case-insensitivity.
   * Before temporary table drop, checks if passed table schema is valid default temporary workspace.
   *
   * @param schema table schema
   * @param tableName original table name
   * @param config drill config
   */
  public void removeTemporaryTable(AbstractSchema schema, String tableName, DrillConfig config) {
    String temporaryTable = resolveTemporaryTableName(tableName);
    if (temporaryTable == null) {
      return;
    }
    SqlHandlerUtil.dropTableFromSchema(SchemaUtilites.resolveToValidTemporaryWorkspace(schema, config), temporaryTable);
    temporaryTables.remove(tableName.toLowerCase());
  }

  /**
   * Session temporary tables are stored under temporary workspace location in session folder
   * defined by unique session id. These session temporary locations are deleted on session close.
   * If default temporary workspace file system or location is changed at runtime,
   * new session temporary location will be added with corresponding file system
   * to the list of session temporary locations. If location does not exist it will be created and
   * {@link StorageStrategy#TEMPORARY} storage rules will be applied to it.
   *
   * @param temporaryWorkspace temporary workspace
   * @throws IOException in case of error during temporary location creation
   */
  private void addTemporaryLocation(WorkspaceSchemaFactory.WorkspaceSchema temporaryWorkspace) throws IOException {
    DrillFileSystem fs = temporaryWorkspace.getFS();
    Path temporaryLocation = new Path(fs.getUri().toString(),
        new Path(temporaryWorkspace.getDefaultLocation(), sessionId));

    FileSystem fileSystem = temporaryLocations.putIfAbsent(temporaryLocation, fs);

    if (fileSystem == null) {
      StorageStrategy.TEMPORARY.createPathAndApply(fs, temporaryLocation);
      Preconditions.checkArgument(fs.exists(temporaryLocation),
          String.format("Temporary location should exist [%s]", temporaryLocation.toUri().getPath()));
    }
  }
}
