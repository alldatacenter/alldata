/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.terminal;

import com.netease.arctic.server.catalog.CatalogType;

import java.util.List;
import java.util.Map;

public interface TerminalSession {

  /**
   * ResultSet of single statement execute result.
   */
  interface ResultSet {
    /**
     * @return - list of column name for result-set.
     */
    List<String> columns();

    /**
     * fetch row from result-set
     * @return - return false if there is no more data.
     */
    boolean next();

    /**
     * @return current row
     */
    Object[] rowData();

    /**
     * @return - return true if current statement shouldn't return result-set.
     */
    default boolean empty() {
      List<String> columns = columns();
      return columns == null || columns.isEmpty();
    }

    /**
     * close current statement and ignore data not fetched.
     */
    void close();
  }

  /**
   * getRuntime current session configs for logs
   */
  Map<String, String> configs();

  /**
   * execute a statement and return result set.
   * @param statement single statement.
   * @return result set
   */
  ResultSet executeStatement(String catalog, String statement);

  /**
   * @return - return logs during execution and clean logs
   */
  List<String> logs();

  /**
   * to check current session is alive. DO-NOT-THROW-ANYTHING of this method.
   * @return - false if session is not able to execute statement.
   */
  boolean active();

  /**
   * close session and release resources.
   */
  void release();

  static boolean canUseSparkSessionCatalog(Map<String, String> sessionConf, String catalog) {
    String usingSessionCatalogForHiveKey =
        TerminalSessionFactory.SessionConfigOptions.USING_SESSION_CATALOG_FOR_HIVE.key();
    String usingSessionCatalogForHive =
        sessionConf.getOrDefault(usingSessionCatalogForHiveKey, "false");
    String type =
        sessionConf.get(TerminalSessionFactory.SessionConfigOptions.catalogProperty(catalog, "type"));
    return usingSessionCatalogForHive.equals("true") && CatalogType.HIVE.name().equalsIgnoreCase(type);
  }
}
