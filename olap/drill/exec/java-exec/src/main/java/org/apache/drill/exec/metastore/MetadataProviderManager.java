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
package org.apache.drill.exec.metastore;

import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.metastore.metadata.TableMetadataProviderBuilder;

/**
 * Base interface for passing and obtaining {@link SchemaProvider}, {@link DrillStatsTable} and
 * {@link TableMetadataProvider}, responsible for creating required
 * {@link TableMetadataProviderBuilder} which constructs required {@link TableMetadataProvider}
 * based on specified providers
 */
public interface MetadataProviderManager {

  void setSchemaProvider(SchemaProvider schemaProvider);

  SchemaProvider getSchemaProvider();

  void setStatsProvider(DrillStatsTable statsProvider);

  DrillStatsTable getStatsProvider();

  void setTableMetadataProvider(TableMetadataProvider tableMetadataProvider);

  TableMetadataProvider getTableMetadataProvider();

  /**
   * Returns {@code true} if current {@link MetadataProviderManager} instance uses Drill Metastore.
   *
   * @return {@code true} if current {@link MetadataProviderManager} instance uses Drill Metastore,
   * {@code false} otherwise.
   */
  boolean usesMetastore();
}
