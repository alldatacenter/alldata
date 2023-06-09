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
package org.apache.drill.exec.store;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Helper class that can be used to obtain rules required for pushing down operators
 * that specific plugin supports configured using {@link StoragePluginRulesSupplierBuilder}.
 */
public class StoragePluginRulesSupplier {

  private final StoragePluginRulesSupplierBuilder storagePluginRulesSupplierBuilder;

  private StoragePluginRulesSupplier(StoragePluginRulesSupplierBuilder storagePluginRulesSupplierBuilder) {
    this.storagePluginRulesSupplierBuilder = storagePluginRulesSupplierBuilder;
  }

  public Set<? extends RelOptRule> getOptimizerRules() {
    ImmutableSet.Builder<RelOptRule> builder = ImmutableSet.builder();
    PluginRulesProvider rulesProvider = storagePluginRulesSupplierBuilder.rulesProvider();
    if (storagePluginRulesSupplierBuilder.supportsProjectPushdown()) {
      builder.addAll(rulesProvider.projectRules());
    }
    if (storagePluginRulesSupplierBuilder.supportsFilterPushdown()) {
      builder.addAll(rulesProvider.filterRules());
    }
    if (storagePluginRulesSupplierBuilder.supportsSortPushdown()) {
      builder.addAll(rulesProvider.sortRules());
    }
    if (storagePluginRulesSupplierBuilder.supportsUnionPushdown()) {
      builder.addAll(rulesProvider.unionRules());
    }
    if (storagePluginRulesSupplierBuilder.supportsJoinPushdown()) {
      builder.addAll(rulesProvider.joinRules());
    }
    if (storagePluginRulesSupplierBuilder.supportsAggregatePushdown()) {
      builder.addAll(rulesProvider.aggregateRules());
    }
    if (storagePluginRulesSupplierBuilder.supportsLimitPushdown()) {
      builder.addAll(rulesProvider.limitRules());
    }
    builder.add(rulesProvider.vertexRule());
    builder.add(rulesProvider.prelConverterRule());
    return builder.build();
  }

  public Convention convention() {
    return storagePluginRulesSupplierBuilder.convention();
  }

  public static StoragePluginRulesSupplierBuilder builder() {
    return new StoragePluginRulesSupplierBuilder();
  }

  public static class StoragePluginRulesSupplierBuilder {

    private boolean supportsProjectPushdown;

    private boolean supportsFilterPushdown;

    private boolean supportsAggregatePushdown;

    private boolean supportsSortPushdown;

    private boolean supportsUnionPushdown;

    private boolean supportsJoinPushdown;

    private boolean supportsLimitPushdown;

    private PluginRulesProvider rulesProvider;

    private Convention convention;

    public boolean supportsProjectPushdown() {
      return supportsProjectPushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsProjectPushdown(boolean supportsProjectPushdown) {
      this.supportsProjectPushdown = supportsProjectPushdown;
      return this;
    }

    public boolean supportsFilterPushdown() {
      return supportsFilterPushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsFilterPushdown(boolean supportsFilterPushdown) {
      this.supportsFilterPushdown = supportsFilterPushdown;
      return this;
    }

    public boolean supportsAggregatePushdown() {
      return supportsAggregatePushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsAggregatePushdown(boolean supportsAggregatePushdown) {
      this.supportsAggregatePushdown = supportsAggregatePushdown;
      return this;
    }

    public boolean supportsSortPushdown() {
      return supportsSortPushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsSortPushdown(boolean supportsSortPushdown) {
      this.supportsSortPushdown = supportsSortPushdown;
      return this;
    }

    public boolean supportsUnionPushdown() {
      return supportsUnionPushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsUnionPushdown(boolean supportsUnionPushdown) {
      this.supportsUnionPushdown = supportsUnionPushdown;
      return this;
    }

    public boolean supportsJoinPushdown() {
      return supportsJoinPushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsJoinPushdown(boolean supportsJoinPushdown) {
      this.supportsJoinPushdown = supportsJoinPushdown;
      return this;
    }

    public boolean supportsLimitPushdown() {
      return supportsLimitPushdown;
    }

    public StoragePluginRulesSupplierBuilder supportsLimitPushdown(boolean supportsLimitPushdown) {
      this.supportsLimitPushdown = supportsLimitPushdown;
      return this;
    }

    public PluginRulesProvider rulesProvider() {
      return rulesProvider;
    }

    public StoragePluginRulesSupplierBuilder rulesProvider(PluginRulesProvider rulesProvider) {
      this.rulesProvider = rulesProvider;
      return this;
    }

    public Convention convention() {
      return convention;
    }

    public StoragePluginRulesSupplierBuilder convention(Convention convention) {
      this.convention = convention;
      return this;
    }

    public StoragePluginRulesSupplier build() {
      return new StoragePluginRulesSupplier(this);
    }
  }
}
