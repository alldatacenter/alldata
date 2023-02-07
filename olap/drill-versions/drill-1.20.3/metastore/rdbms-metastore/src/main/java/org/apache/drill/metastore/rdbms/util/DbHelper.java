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
package org.apache.drill.metastore.rdbms.util;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.config.MetastoreConfigConstants;
import org.apache.drill.metastore.rdbms.config.RdbmsConfigConstants;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides methods to configure database prior to data source initialization.
 */
public interface DbHelper {

  /**
   * Initializes {@link DbHelper} implementation based on {@link SQLDialect}.
   *
   * @param config Metastore config
   * @return DBHelper instance
   */
  static DbHelper init(DrillConfig config) {
    SQLDialect dialect = JDBCUtils.dialect(config.getString(RdbmsConfigConstants.DATA_SOURCE_URL));
    switch (dialect) {
      case SQLITE:
        return new SQLiteHelper(config);
      default:
        return NoOpHelper.get();
    }
  }

  /**
   * Prepares database prior to data source configuration.
   */
  void prepareDatabase();

  /**
   * No-op implementation of {@link DbHelper} for those databases that do not require
   * any preparation before data source creation.
   */
  class NoOpHelper implements DbHelper {

    private static final NoOpHelper INSTANCE = new NoOpHelper();

    public static NoOpHelper get() {
      return INSTANCE;
    }

    @Override
    public void prepareDatabase() {
      // do nothing
    }
  }

  /**
   * SQLite implementation of {@link DbHelper}, creates database path if needed.
   */
  class SQLiteHelper implements DbHelper {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteHelper.class);

    private final DrillConfig config;

    public SQLiteHelper(DrillConfig config) {
      this.config = config;
    }

    /**
     * SQLite database requires database path to exist prior to database initialization.
     * Checks if path creation flag set to true and path is set and attempts to create
     * database path recursively.
     */
    @Override
    public void prepareDatabase() {
      if (config.hasPath(RdbmsConfigConstants.SQLITE_PATH_CREATE)
        && config.hasPath(RdbmsConfigConstants.SQLITE_PATH_VALUE)
        && config.getBoolean(RdbmsConfigConstants.SQLITE_PATH_CREATE)) {

        String path = config.getString(RdbmsConfigConstants.SQLITE_PATH_VALUE);
        try {
          Path dbPath = Files.createDirectories(Paths.get(path));
          logger.info("Configured SQLite database path: {}", dbPath);
        } catch (IOException e) {
          throw new RdbmsMetastoreException(String.format("Unable to create SQLite database path [%s]: %s. " +
              "Optionally, path can be created manually and [%s] set to false in %s.", path, e.getMessage(),
            RdbmsConfigConstants.SQLITE_PATH_CREATE, MetastoreConfigConstants.OVERRIDE_RESOURCE_FILE_NAME), e);
        }
      }
    }
  }
}
