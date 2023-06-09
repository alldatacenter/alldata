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
package org.apache.drill.exec.store.http;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.exec.oauth.OAuthTokenProvider;
import org.apache.drill.exec.oauth.PersistentTokenTable;
import org.apache.drill.exec.oauth.TokenRegistry;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.planner.PlannerPhase;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.base.filter.FilterPushDownUtils;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Set;

public class HttpStoragePlugin extends AbstractStoragePlugin {

  private final HttpStoragePluginConfig config;
  private final HttpSchemaFactory schemaFactory;
  private final StoragePluginRegistry registry;
  private final TokenRegistry tokenRegistry;

  public HttpStoragePlugin(HttpStoragePluginConfig configuration, DrillbitContext context, String name) {
    super(context, name);
    this.config = configuration;
    this.registry = context.getStorage();
    this.schemaFactory = new HttpSchemaFactory(this);

    // Get OAuth Token Provider if needed
    OAuthTokenProvider tokenProvider = context.getoAuthTokenProvider();
    tokenRegistry = tokenProvider.getOauthTokenRegistry();
    tokenRegistry.createTokenTable(getName());
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  @Override
  public HttpStoragePluginConfig getConfig() {
    return config;
  }

  public StoragePluginRegistry getRegistry() {
    return registry;
  }

  public TokenRegistry getTokenRegistry() {
    return tokenRegistry;
  }

  public PersistentTokenTable getTokenTable() { return tokenRegistry.getTokenTable(getName()); }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection) throws IOException {
    HttpScanSpec scanSpec = selection.getListWith(context.getLpPersistence().getMapper(), new TypeReference<HttpScanSpec>() {});
    return new HttpGroupScan(scanSpec);
  }

  @Override
  public Set<? extends RelOptRule> getOptimizerRules(OptimizerRulesContext optimizerContext, PlannerPhase phase) {

    // Push-down planning is done at the logical phase so it can
    // influence parallelization in the physical phase. Note that many
    // existing plugins perform filter push-down at the physical
    // phase, which also works fine if push-down is independent of
    // parallelization.
    if (FilterPushDownUtils.isFilterPushDownPhase(phase) || phase == PlannerPhase.LOGICAL) {
      return HttpPushDownListener.rulesFor(optimizerContext);
    } else {
      return ImmutableSet.of();
    }
  }}
