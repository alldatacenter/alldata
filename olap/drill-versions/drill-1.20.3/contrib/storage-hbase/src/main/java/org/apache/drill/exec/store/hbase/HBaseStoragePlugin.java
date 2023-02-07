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
package org.apache.drill.exec.store.hbase;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Connection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

public class HBaseStoragePlugin extends AbstractStoragePlugin {
  private static final HBaseConnectionManager hbaseConnectionManager = HBaseConnectionManager.INSTANCE;

  private final HBaseStoragePluginConfig storeConfig;
  private final HBaseSchemaFactory schemaFactory;
  private final HBaseConnectionKey connectionKey;

  private final String name;

  public HBaseStoragePlugin(HBaseStoragePluginConfig storeConfig, DrillbitContext context, String name)
      throws IOException {
    super(context, name);
    this.schemaFactory = new HBaseSchemaFactory(this, name);
    this.storeConfig = storeConfig;
    this.name = name;
    this.connectionKey = new HBaseConnectionKey();
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public HBaseGroupScan getPhysicalScan(String userName, JSONOptions selection) throws IOException {
    HBaseScanSpec scanSpec = selection.getListWith(new ObjectMapper(), new TypeReference<HBaseScanSpec>() {});
    return new HBaseGroupScan(userName, this, scanSpec, null);
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  @Override
  public HBaseStoragePluginConfig getConfig() {
    return storeConfig;
  }

  @Override
  public Set<StoragePluginOptimizerRule> getPhysicalOptimizerRules(OptimizerRulesContext optimizerRulesContext) {
    return ImmutableSet.of(HBasePushFilterIntoScan.FILTER_ON_SCAN, HBasePushFilterIntoScan.FILTER_ON_PROJECT);
  }

  @Override
  public void close() throws Exception {
    hbaseConnectionManager.closeConnection(connectionKey);
  }

  public Connection getConnection() {
    return hbaseConnectionManager.getConnection(connectionKey);
  }

  /**
   * An internal class which serves the key in a map of {@link HBaseStoragePlugin} => {@link Connection}.
   */
  class HBaseConnectionKey {

    private final ReentrantLock lock = new ReentrantLock();

    private HBaseConnectionKey() {}

    public void lock() {
      lock.lock();
    }

    public void unlock() {
      lock.unlock();
    }

    public Configuration getHBaseConf() {
      return storeConfig.getHBaseConf();
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((storeConfig == null) ? 0 : storeConfig.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if (obj == null) {
        return false;
      } else if (getClass() != obj.getClass()) {
        return false;
      }

      HBaseStoragePlugin other = ((HBaseConnectionKey) obj).getHBaseStoragePlugin();
      if (name == null) {
        if (other.name != null) {
          return false;
        }
      } else if (!name.equals(other.name)) {
        return false;
      }
      if (storeConfig == null) {
        if (other.storeConfig != null) {
          return false;
        }
      } else if (!storeConfig.equals(other.storeConfig)) {
        return false;
      }
      return true;
    }

    private HBaseStoragePlugin getHBaseStoragePlugin() {
      return HBaseStoragePlugin.this;
    }
  }
}
