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
package org.apache.drill.exec.store.openTSDB;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.openTSDB.client.services.ServiceImpl;
import org.apache.drill.exec.store.openTSDB.schema.OpenTSDBSchemaFactory;

import java.io.IOException;

public class OpenTSDBStoragePlugin extends AbstractStoragePlugin {

  private final OpenTSDBStoragePluginConfig engineConfig;
  private final OpenTSDBSchemaFactory schemaFactory;

  private final ServiceImpl db;

  public OpenTSDBStoragePlugin(OpenTSDBStoragePluginConfig configuration, DrillbitContext context,
      String name) throws IOException {
    super(context, name);
    this.schemaFactory = new OpenTSDBSchemaFactory(this, getName());
    this.engineConfig = configuration;
    this.db = new ServiceImpl(configuration.getConnection());
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public OpenTSDBStoragePluginConfig getConfig() {
    return engineConfig;
  }

  @Override
  public OpenTSDBGroupScan getPhysicalScan(String userName, JSONOptions selection) throws IOException {
    OpenTSDBScanSpec scanSpec = selection.getListWith(new ObjectMapper(), new TypeReference<OpenTSDBScanSpec>() {
    });
    return new OpenTSDBGroupScan(this, scanSpec, null);
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  public ServiceImpl getClient() {
    return db;
  }
}
