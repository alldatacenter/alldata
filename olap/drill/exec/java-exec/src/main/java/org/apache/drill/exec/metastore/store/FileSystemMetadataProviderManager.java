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
package org.apache.drill.exec.metastore.store;

import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.metastore.metadata.TableMetadataProvider;

/**
 * Implementation of {@link MetadataProviderManager} which uses file system providers and returns
 * builders for file system based {@link TableMetadataProvider} instances.
 */
public class FileSystemMetadataProviderManager implements MetadataProviderManager {

  private SchemaProvider schemaProvider;
  private DrillStatsTable statsProvider;

  private TableMetadataProvider tableMetadataProvider;

  public static MetadataProviderManager init() {
    return new FileSystemMetadataProviderManager();
  }

  /**
   * Returns {@link TableMetadataProvider} which provides specified schema.
   *
   * @param schema table schema which should be provided
   * @return {@link TableMetadataProvider} which provides specified schema
   */
  public static TableMetadataProvider getMetadataProviderForSchema(TupleMetadata schema) {
    return new SimpleFileTableMetadataProvider.Builder(new FileSystemMetadataProviderManager())
        .withSchema(schema)
        .build();
  }

  /**
   * Checks whether specified {@link MetadataProviderManager} is not null and returns {@link TableMetadataProvider}
   * obtained from specified {@link MetadataProviderManager}.
   * Otherwise {@link FileSystemMetadataProviderManager} is used to construct {@link TableMetadataProvider}.
   *
   * @param providerManager metadata provider manager
   * @return {@link MetadataProviderManager} instance
   */
  public static TableMetadataProvider getMetadataProvider(MetadataProviderManager providerManager) {
    return providerManager == null
        ? new SimpleFileTableMetadataProvider.Builder(new FileSystemMetadataProviderManager()).build()
        : providerManager.getTableMetadataProvider();
  }

  @Override
  public void setSchemaProvider(SchemaProvider schemaProvider) {
    this.schemaProvider = schemaProvider;
  }

  @Override
  public SchemaProvider getSchemaProvider() {
    return schemaProvider;
  }

  @Override
  public void setStatsProvider(DrillStatsTable statsProvider) {
    this.statsProvider = statsProvider;
  }

  @Override
  public DrillStatsTable getStatsProvider() {
    return statsProvider;
  }

  @Override
  public void setTableMetadataProvider(TableMetadataProvider tableMetadataProvider) {
    this.tableMetadataProvider = tableMetadataProvider;
  }

  @Override
  public TableMetadataProvider getTableMetadataProvider() {
    return tableMetadataProvider;
  }

  public boolean usesMetastore() {
    return false;
  }
}
