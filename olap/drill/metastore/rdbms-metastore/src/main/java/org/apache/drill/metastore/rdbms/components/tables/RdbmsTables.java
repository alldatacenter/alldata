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
package org.apache.drill.metastore.rdbms.components.tables;

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.components.tables.TablesMetadataTypeValidator;
import org.apache.drill.metastore.operate.Metadata;
import org.apache.drill.metastore.operate.Modify;
import org.apache.drill.metastore.operate.Read;
import org.apache.drill.metastore.rdbms.QueryExecutorProvider;
import org.apache.drill.metastore.rdbms.RdbmsMetastoreContext;
import org.apache.drill.metastore.rdbms.operate.RdbmsMetadata;
import org.apache.drill.metastore.rdbms.operate.RdbmsModify;
import org.apache.drill.metastore.rdbms.operate.RdbmsRead;
import org.apache.drill.metastore.rdbms.transform.Transformer;

/**
 * Metastore Tables component which stores tables metadata in the corresponding RDBMS tables:
 * TABLES, SEGMENTS, FILES, ROW_GROUPS, PARTITIONS.
 * Provides methods to read and modify tables metadata.
 */
public class RdbmsTables implements Tables, RdbmsMetastoreContext<TableMetadataUnit> {

  private final QueryExecutorProvider executorProvider;

  public RdbmsTables(QueryExecutorProvider executorProvider) {
    this.executorProvider = executorProvider;
  }

  public RdbmsMetastoreContext<TableMetadataUnit> context() {
    return this;
  }

  @Override
  public Metadata metadata() {
    return new RdbmsMetadata();
  }

  @Override
  public Read<TableMetadataUnit> read() {
    return new RdbmsRead<>(TablesMetadataTypeValidator.INSTANCE, context());
  }

  @Override
  public Modify<TableMetadataUnit> modify() {
    return new RdbmsModify<>(TablesMetadataTypeValidator.INSTANCE, context());
  }

  @Override
  public QueryExecutorProvider executorProvider() {
    return executorProvider;
  }

  @Override
  public Transformer<TableMetadataUnit> transformer() {
    return TablesTransformer.get();
  }
}
