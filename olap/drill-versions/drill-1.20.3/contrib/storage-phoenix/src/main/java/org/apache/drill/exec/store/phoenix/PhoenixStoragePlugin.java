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
package org.apache.drill.exec.store.phoenix;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.PhoenixSqlDialect;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.phoenix.rules.PhoenixConvention;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.hadoop.security.UserGroupInformation;

public class PhoenixStoragePlugin extends AbstractStoragePlugin {

  private final PhoenixStoragePluginConfig config;
  private final SqlDialect dialect;
  private final PhoenixConvention convention;
  private final PhoenixSchemaFactory schemaFactory;
  private final boolean impersonationEnabled;
  private final LoadingCache<String, PhoenixDataSource> CACHE = CacheBuilder.newBuilder()
    .maximumSize(5) // Up to 5 clients for impersonation-enabled.
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build(new CacheLoader<String, PhoenixDataSource>() {
      @Override
      public PhoenixDataSource load(String userName) {
        UserGroupInformation ugi = ImpersonationUtil.getProcessUserUGI();
        return impersonationEnabled
          ? ugi.doAs((PrivilegedAction<PhoenixDataSource>) () -> createDataSource(userName))
          : createDataSource(userName);
      }
    });

  public PhoenixStoragePlugin(PhoenixStoragePluginConfig config, DrillbitContext context, String name) {
    super(context, name);
    this.config = config;
    this.dialect = PhoenixSqlDialect.DEFAULT;
    this.convention = new PhoenixConvention(dialect, name, this);
    this.schemaFactory = new PhoenixSchemaFactory(this);
    this.impersonationEnabled = context.getConfig().getBoolean(ExecConstants.IMPERSONATION_ENABLED);
  }

  @Override
  public StoragePluginConfig getConfig() {
    return config;
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public Set<? extends RelOptRule> getPhysicalOptimizerRules(OptimizerRulesContext optimizerRulesContext) {
    return convention.getRules();
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection) throws IOException {
    PhoenixScanSpec scanSpec =
      selection.getListWith(context.getLpPersistence().getMapper(), new TypeReference<PhoenixScanSpec>() {});
    return new PhoenixGroupScan(userName, scanSpec, this);
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(CACHE::invalidateAll);
  }

  public SqlDialect getDialect() {
    return dialect;
  }

  public PhoenixConvention getConvention() {
    return convention;
  }

  public PhoenixDataSource getDataSource(String userName) throws SQLException {
    try {
      return CACHE.get(userName);
    } catch (final ExecutionException e) {
      throw new SQLException("Failure setting up Phoenix DataSource (PQS client)", e);
    }
  }

  private PhoenixDataSource createDataSource(String userName) {
    // Don't use the pool with the connection
    Map<String, Object> props = config.getProps();
    if (config.getUsername() != null && config.getPassword() != null) {
      props.put("user", config.getUsername());
      props.put("password", config.getPassword());
    }
    boolean impersonationEnabled = context.getConfig().getBoolean(ExecConstants.IMPERSONATION_ENABLED);
    return StringUtils.isNotBlank(config.getJdbcURL())
      ? new PhoenixDataSource(config.getJdbcURL(), userName, props, impersonationEnabled) // the props is initiated.
      : new PhoenixDataSource(config.getHost(), config.getPort(), userName, props, impersonationEnabled);
  }
}
