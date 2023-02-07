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
package org.apache.drill.exec.store.hive.schema;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.drill.exec.store.hive.HiveStoragePlugin;
import org.apache.drill.exec.store.hive.HiveStoragePluginConfig;
import org.apache.drill.exec.store.hive.client.DrillHiveMetaStoreClient;
import org.apache.drill.exec.store.hive.client.DrillHiveMetaStoreClientFactory;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.cache.RemovalListener;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.drill.exec.util.ImpersonationUtil.getProcessUserName;

public class HiveSchemaFactory extends AbstractSchemaFactory {
  private static final Logger logger = LoggerFactory.getLogger(HiveSchemaFactory.class);

  // MetaStoreClient created using process user credentials
  private final DrillHiveMetaStoreClient processUserMetastoreClient;
  // MetasStoreClient created using SchemaConfig credentials
  private final LoadingCache<String, DrillHiveMetaStoreClient> metaStoreClientLoadingCache;

  private final HiveStoragePlugin plugin;
  private final boolean isDrillImpersonationEnabled;
  private final boolean isHS2DoAsSet;

  public HiveSchemaFactory(final HiveStoragePlugin plugin, final String name, final HiveConf hiveConf) throws ExecutionSetupException {
    super(name);
    this.plugin = plugin;

    isHS2DoAsSet = hiveConf.getBoolVar(ConfVars.HIVE_SERVER2_ENABLE_DOAS);
    isDrillImpersonationEnabled = plugin.getContext().getConfig().getBoolean(ExecConstants.IMPERSONATION_ENABLED);

    try {
      processUserMetastoreClient =
          DrillHiveMetaStoreClientFactory.createCloseableClientWithCaching(hiveConf);
    } catch (MetaException e) {
      throw new ExecutionSetupException("Failure setting up Hive metastore client.", e);
    }

    metaStoreClientLoadingCache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(5) // Up to 5 clients for impersonation-enabled.
        .removalListener((RemovalListener<String, DrillHiveMetaStoreClient>) notification -> {
          DrillHiveMetaStoreClient client = notification.getValue();
          client.close();
        })
        .build(new CacheLoader<String, DrillHiveMetaStoreClient>() {
          @Override
          public DrillHiveMetaStoreClient load(String userName) {
            return DrillHiveMetaStoreClientFactory.createClientWithAuthz(processUserMetastoreClient, hiveConf, userName);
          }
        });
  }

  /**
   * Close this schema factory in preparation for retrying. Attempt to close
   * connections, but just ignore any errors.
   */
  public void close() {
    AutoCloseables.closeSilently(processUserMetastoreClient, metaStoreClientLoadingCache::invalidateAll);
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    DrillHiveMetaStoreClient mClientForSchemaTree = processUserMetastoreClient;
    if (isDrillImpersonationEnabled) {
      try {
        mClientForSchemaTree = metaStoreClientLoadingCache.get(schemaConfig.getUserName());
      } catch (final ExecutionException e) {
        throw new IOException("Failure setting up Hive metastore client.", e);
      }
    }
    HiveSchema schema = new HiveSchema(schemaConfig, mClientForSchemaTree, getName());
    SchemaPlus hPlus = parent.add(getName(), schema);
    schema.setHolder(hPlus);
  }

  class HiveSchema extends AbstractSchema {

    private final SchemaConfig schemaConfig;
    private final DrillHiveMetaStoreClient mClient;
    private HiveDatabaseSchema defaultSchema;

    HiveSchema(final SchemaConfig schemaConfig, final DrillHiveMetaStoreClient mClient, final String name) {
      super(Collections.emptyList(), name);
      this.schemaConfig = schemaConfig;
      this.mClient = mClient;
      getSubSchema(DEFAULT_WS_NAME);
    }

    @Override
    public AbstractSchema getSubSchema(String name) {
      try {
        List<String> dbs = mClient.getDatabases(schemaConfig.getIgnoreAuthErrors());
        if (!dbs.contains(name)) {
          logger.debug("Database '{}' doesn't exists in Hive storage '{}'", name, getName());
          return null;
        }
        HiveDatabaseSchema schema = getSubSchemaKnownExists(name);
        if (DEFAULT_WS_NAME.equals(name)) {
          this.defaultSchema = schema;
        }
        return schema;
      } catch (TException e) {
        throw new DrillRuntimeException(e);
      }
    }

    /**
     * Helper method to get subschema when we know it exists (already checked the existence)
     */
    private HiveDatabaseSchema getSubSchemaKnownExists(String name) {
      return new HiveDatabaseSchema(this, name, mClient, schemaConfig);
    }

    void setHolder(SchemaPlus plusOfThis) {
      for (String s : getSubSchemaNames()) {
        plusOfThis.add(s, getSubSchemaKnownExists(s));
      }
    }

    @Override
    public boolean showInInformationSchema() {
      return false;
    }

    @Override
    public Set<String> getSubSchemaNames() {
      try {
        List<String> dbs = mClient.getDatabases(schemaConfig.getIgnoreAuthErrors());
        return new HashSet<>(dbs);
      } catch (TException e) {
        logger.warn("Failure while getting Hive database list.", e);
      }
      return super.getSubSchemaNames();
    }

    @Override
    public org.apache.calcite.schema.Table getTable(String name) {
      if (defaultSchema == null) {
        return super.getTable(name);
      }
      return defaultSchema.getTable(name);
    }

    @Override
    public Set<String> getTableNames() {
      if (defaultSchema == null) {
        return super.getTableNames();
      }
      return defaultSchema.getTableNames();
    }

    @Override
    public boolean areTableNamesCaseSensitive() {
      return false;
    }

    Table getDrillTable(String dbName, String t) {
      HiveReadEntry entry = getSelectionBaseOnName(dbName, t);
      if (entry == null) {
        return null;
      }
      final String schemaUser = schemaConfig.getUserName();
      return TableType.VIEW == entry.getJdbcTableType()
          ? new DrillHiveViewTable(entry, schemaPath, schemaConfig, getUser(schemaUser, entry.getTable().getOwner()))
          : new DrillHiveTable(getName(), plugin, getUser(schemaUser, getProcessUserName()), entry);
    }

    @Override
    public String getUser(String impersonated, String notImpersonated) {
      return needToImpersonateReadingData() ? impersonated : notImpersonated;
    }

    HiveReadEntry getSelectionBaseOnName(String dbName, String t) {
      if (dbName == null) {
        dbName = DEFAULT_WS_NAME;
      }
      try {
        return mClient.getHiveReadEntry(dbName, t, schemaConfig.getIgnoreAuthErrors());
      } catch (TException e) {
        logger.warn("Exception occurred while trying to read table. {}.{}", dbName, t, e.getCause());
        return null;
      }
    }

    @Override
    public AbstractSchema getDefaultSchema() {
      return defaultSchema;
    }

    @Override
    public String getTypeName() {
      return HiveStoragePluginConfig.NAME;
    }

    @Override
    public boolean needToImpersonateReadingData() {
      return isDrillImpersonationEnabled && isHS2DoAsSet;
    }
  }

}
