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
package org.apache.drill.metastore.rdbms.config;

import org.apache.drill.metastore.config.MetastoreConfigConstants;

/**
 * Drill RDBMS Metastore configuration which is defined
 * in {@link MetastoreConfigConstants#MODULE_RESOURCE_FILE_NAME} file.
 */
public interface RdbmsConfigConstants {

  /**
   * Drill RDBMS Metastore configuration properties namespace.
   */
  String BASE = MetastoreConfigConstants.BASE + "rdbms.";

  /**
   * RDBMS Metastore data source namespace.
   */
  String DATA_SOURCE_NAMESPACE = BASE + "data_source.";

  /**
   * RDBMS Metastore data source driver property. Required.
   */
  String DATA_SOURCE_DRIVER = DATA_SOURCE_NAMESPACE + "driver";

  /**
   * RDBMS Metastore data source url property. Required.
   */
  String DATA_SOURCE_URL = DATA_SOURCE_NAMESPACE + "url";

  /**
   * RDBMS Metastore data source url property. Optional.
   */
  String DATA_SOURCE_USER_NAME = DATA_SOURCE_NAMESPACE + "username";

  /**
   * RDBMS Metastore data source url property. Optional.
   */
  String DATA_SOURCE_PASSWORD = DATA_SOURCE_NAMESPACE + "password";

  /**
   * RDBMS Metastore data source properties. Optional.
   * Can be set based on Hikari properties: <a href="https://github.com/brettwooldridge/HikariCP">.
   */
  String DATA_SOURCE_PROPERTIES = DATA_SOURCE_NAMESPACE + "properties";

  /**
   * RDBMS Metastore database namespace.
   */
  String DATABASE_NAMESPACE = BASE + "database.";

  /**
   * RDBMS Metastore SQLite database namespace.
   */
  String SQLITE_NAMESPACE = DATABASE_NAMESPACE + "sqlite.";

  /**
   * RDBMS Metastore SQLite database path namespace.
   */
  String SQLITE_PATH_NAMESPACE = SQLITE_NAMESPACE + "path.";

  /**
   * RDBMS Metastore SQLite database path value.
   */
  String SQLITE_PATH_VALUE = SQLITE_PATH_NAMESPACE + "value";

  /**
   * Flag which indicates if RDBMS Metastore SQLite database path value
   * should be created prior to data source initialization.
   * Flag can be set to {@code false} if path already exists.
   */
  String SQLITE_PATH_CREATE = SQLITE_PATH_NAMESPACE + "create";
}
