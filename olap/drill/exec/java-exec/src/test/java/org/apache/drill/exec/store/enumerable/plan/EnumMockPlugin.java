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
package org.apache.drill.exec.store.enumerable.plan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.planner.PlannerPhase;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.SchemaFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Mock plugin implementation for testing "enumerable" rel nodes transformation.
 */
public class EnumMockPlugin extends AbstractStoragePlugin {

  private final SchemaFactory schemaFactory;

  private final EnumMockStoragePluginConfig config;

  private final Convention convention;

  public EnumMockPlugin(EnumMockStoragePluginConfig config, DrillbitContext inContext, String inName) {
    super(inContext, inName);
    this.config = config;
    EnumMockSchema schema = new EnumMockSchema(inName, this);
    this.schemaFactory = (SchemaConfig schemaConfig, SchemaPlus parent) -> parent.add(inName, schema);
    this.convention = new Convention.Impl(inName, EnumMockRel.class);
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public StoragePluginConfig getConfig() {
    return config;
  }

  @Override
  public Set<? extends RelOptRule> getOptimizerRules(OptimizerRulesContext optimizerContext, PlannerPhase phase) {
    switch (phase) {
      case PHYSICAL:
      case LOGICAL:
        return ImmutableSet.of(
            new EnumerableIntermediatePrelConverterRule(
                new EnumMockRel.MockEnumerablePrelContext(convention), convention),
            new VertexDrelConverterRule(convention));
      case LOGICAL_PRUNE_AND_JOIN:
      case LOGICAL_PRUNE:
      case PARTITION_PRUNING:
      case JOIN_PLANNING:
      default:
        return Collections.emptySet();
    }
  }

  public Convention getConvention() {
    return convention;
  }

  @JsonTypeName(EnumMockStoragePluginConfig.NAME)
  public static class EnumMockStoragePluginConfig extends StoragePluginConfig {
    public static final String NAME = "enum_mock";

    @JsonCreator
    public EnumMockStoragePluginConfig() {
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof EnumMockStoragePluginConfig;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }
}
