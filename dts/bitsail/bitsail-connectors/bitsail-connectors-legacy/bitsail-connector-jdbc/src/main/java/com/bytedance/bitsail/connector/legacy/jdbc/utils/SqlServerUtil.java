/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlServerUtil extends AbstractJdbcUtil {
  public static final String DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  public static final String DB_QUOTE = "\"";

  public static final String VALUE_QUOTE = "'";

  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String DISTINCT_KEYS = "CARDINALITY";
  private static final String COLUMN_POSITION = "ORDINAL_POSITION";
  private static final String INDEX_NAME = "INDEX_NAME";

  public SqlServerUtil() {
    super(DISTINCT_KEYS, COLUMN_NAME, INDEX_NAME, COLUMN_POSITION, DRIVER_NAME);
  }
}
