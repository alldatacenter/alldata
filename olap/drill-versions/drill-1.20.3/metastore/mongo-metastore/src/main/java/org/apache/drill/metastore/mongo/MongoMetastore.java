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
package org.apache.drill.metastore.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.Metastore;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.components.views.Views;
import org.apache.drill.metastore.mongo.components.tables.MongoTables;
import org.apache.drill.metastore.mongo.config.MongoConfigConstants;

/**
 * Mongo Drill Metastore implementation.
 */
public class MongoMetastore implements Metastore {

  private final MongoClient client;
  private final String database;
  private final String tableCollection;

  public MongoMetastore(DrillConfig config) {
    this.client = MongoClients.create(
      new ConnectionString(config.getString(MongoConfigConstants.CONNECTION)));
    if (config.hasPath(MongoConfigConstants.DATABASE)) {
      this.database = config.getString(MongoConfigConstants.DATABASE);
    } else {
      this.database = MongoConfigConstants.DEFAULT_DATABASE;
    }
    if (config.hasPath(MongoConfigConstants.TABLE_COLLECTION)) {
      this.tableCollection = config.getString(MongoConfigConstants.TABLE_COLLECTION);
    } else {
      this.tableCollection = MongoConfigConstants.DEFAULT_TABLE_COLLECTION;
    }
  }

  @Override
  public Tables tables() {
    return new MongoTables(
      client.getDatabase(database).getCollection(tableCollection), client);
  }

  @Override
  public Views views() {
    throw new UnsupportedOperationException("Views metadata support is not implemented");
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.close();
    }
  }
}
