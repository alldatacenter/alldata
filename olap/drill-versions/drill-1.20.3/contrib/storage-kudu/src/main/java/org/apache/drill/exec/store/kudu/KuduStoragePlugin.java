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
package org.apache.drill.exec.store.kudu;

import java.io.IOException;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.kudu.client.KuduClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KuduStoragePlugin extends AbstractStoragePlugin {

  private final KuduStoragePluginConfig engineConfig;
  private final KuduSchemaFactory schemaFactory;

  private final KuduClient client;

  public KuduStoragePlugin(KuduStoragePluginConfig configuration, DrillbitContext context, String name)
    throws IOException {
    super(context, name);
    this.schemaFactory = new KuduSchemaFactory(this, name);
    this.engineConfig = configuration;
    this.client = new KuduClient.KuduClientBuilder(configuration.getMasterAddresses()).build();
  }

  public KuduClient getClient() {
    return client;
  }

  @Override
  public void close() throws Exception {
    client.close();
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public KuduGroupScan getPhysicalScan(String userName, JSONOptions selection) throws IOException {
    KuduScanSpec scanSpec = selection.getListWith(new ObjectMapper(), new TypeReference<KuduScanSpec>() {});
    return new KuduGroupScan(this, scanSpec, null);
  }

  @Override
  public boolean supportsWrite() {
    return true;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  @Override
  public KuduStoragePluginConfig getConfig() {
    return engineConfig;
  }
}
