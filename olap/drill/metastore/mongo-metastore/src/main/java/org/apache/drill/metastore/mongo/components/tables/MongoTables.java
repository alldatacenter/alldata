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
package org.apache.drill.metastore.mongo.components.tables;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.components.tables.TablesMetadataTypeValidator;
import org.apache.drill.metastore.mongo.MongoMetastoreContext;
import org.apache.drill.metastore.mongo.operate.MongoMetadata;
import org.apache.drill.metastore.mongo.operate.MongoModify;
import org.apache.drill.metastore.mongo.operate.MongoRead;
import org.apache.drill.metastore.mongo.transform.Transformer;
import org.apache.drill.metastore.operate.Metadata;
import org.apache.drill.metastore.operate.Modify;
import org.apache.drill.metastore.operate.Read;
import org.bson.Document;


/**
 * Metastore Tables component which stores tables metadata in mongo collection
 * Provides methods to read and modify tables metadata.
 */
public class MongoTables implements Tables, MongoMetastoreContext<TableMetadataUnit> {

  private final MongoClient client;
  private final MongoCollection<Document> tableCollection;

  public MongoTables(MongoCollection<Document> tableCollection, MongoClient client) {
    this.tableCollection = tableCollection;
    this.client = client;
  }

  public MongoMetastoreContext<TableMetadataUnit> context() {
    return this;
  }

  @Override
  public Metadata metadata() {
    return new MongoMetadata();
  }

  @Override
  public Read<TableMetadataUnit> read() {
    return new MongoRead<>(TablesMetadataTypeValidator.INSTANCE, context());
  }

  @Override
  public Modify<TableMetadataUnit> modify() {
    return new MongoModify<>(TablesMetadataTypeValidator.INSTANCE, context());
  }

  @Override
  public MongoCollection<Document> table() {
    return tableCollection;
  }

  @Override
  public MongoClient client() {
    return client;
  }

  @Override
  public Transformer<TableMetadataUnit> transformer() {
    return new TablesTransformer(context());
  }
}
