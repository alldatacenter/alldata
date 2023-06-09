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
package org.apache.drill.exec.store.mapr;

import static com.mapr.fs.jni.MapRConstants.MAPRFS_PREFIX;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.base.AbstractWriter;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.hadoop.conf.Configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import com.mapr.fs.MapRFileSystem;

public abstract class TableFormatPlugin implements FormatPlugin {

  private final StoragePluginConfig storageConfig;
  private final TableFormatPluginConfig config;
  private final Configuration fsConf;
  private final DrillbitContext context;
  private final String name;

  private volatile AbstractStoragePlugin storagePlugin;
  private final MapRFileSystem maprfs;

  protected TableFormatPlugin(String name, DrillbitContext context, Configuration fsConf,
      StoragePluginConfig storageConfig, TableFormatPluginConfig formatConfig) {
    this.context = context;
    this.config = formatConfig;
    this.storageConfig = storageConfig;
    this.fsConf = fsConf;
    this.name = name == null ? "maprdb" : name;
    try {
      this.maprfs = new MapRFileSystem();
      getMaprFS().initialize(new URI(MAPRFS_PREFIX), fsConf);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public boolean supportsWrite() {
    return false;
  }

  @Override
  public boolean supportsAutoPartitioning() {
    return false;
  }

  @Override
  public Configuration getFsConf() {
    return fsConf;
  }

  @Override
  public Set<StoragePluginOptimizerRule> getOptimizerRules() {
    return ImmutableSet.of();
  }

  @Override
  public AbstractWriter getWriter(PhysicalOperator child, String location,
      List<String> partitionColumns) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FormatPluginConfig getConfig() {
    return config;
  }

  @Override
  public StoragePluginConfig getStorageConfig() {
    return storageConfig;
  }

  @Override
  public DrillbitContext getContext() {
    return context;
  }

  @Override
  public String getName() {
    return name;
  }

  public synchronized AbstractStoragePlugin getStoragePlugin() {
    if (this.storagePlugin == null) {
      this.storagePlugin = context.getStorage().resolve(storageConfig,
          AbstractStoragePlugin.class);
    }
    return storagePlugin;
  }

  @JsonIgnore
  public MapRFileSystem getMaprFS() {
    return maprfs;
  }
}
