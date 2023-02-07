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
package org.apache.drill.metastore.mongo.config;

import org.apache.drill.metastore.config.MetastoreConfigConstants;

/**
 * Drill Mongo Metastore configuration which is defined
 * in {@link MetastoreConfigConstants#MODULE_RESOURCE_FILE_NAME} file.
 */
public interface MongoConfigConstants {

  /**
   * Drill Mongo Metastore configuration properties namespace.
   */
  String BASE = MetastoreConfigConstants.BASE + "mongo.";

  /**
   * Mongo Metastore data source url property. Required.
   */
  String CONNECTION = BASE + "connection";

  /**
   * Database to store meta data. Optional, default is
   * {@link MongoConfigConstants#DEFAULT_DATABASE}
   */
  String DATABASE = BASE + "database";

  /**
   * Collection to store meta data for tables. Optional, default is
   * {@link MongoConfigConstants#DEFAULT_TABLE_COLLECTION}
   */
  String TABLE_COLLECTION = BASE + "table_collection";

  /**
   * Default database to store meta data.
   */
  String DEFAULT_DATABASE = "meta";

  /**
   * Default collection to store meta data for tables.
   */
  String DEFAULT_TABLE_COLLECTION = "tables";

  /**
   * Field name used to identify one document uniquely.
   */
  String ID = "_id";
}
