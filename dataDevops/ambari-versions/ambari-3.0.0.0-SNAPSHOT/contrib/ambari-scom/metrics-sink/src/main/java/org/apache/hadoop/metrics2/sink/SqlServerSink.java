/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.metrics2.sink;

import java.lang.Override;

/**
 * This class stores published metrics to the SQL Server database.
 */
public abstract class SqlServerSink extends SqlSink {
  public SqlServerSink(String NAMENODE_URL_KEY, String DFS_BLOCK_SIZE_KEY) {
    super(NAMENODE_URL_KEY, DFS_BLOCK_SIZE_KEY);
  }

  @Override
  protected String getInsertMetricsProcedureName() {
    return "dbo." + SqlSink.insertMetricProc;
  }

  @Override
  protected String getGetMetricsProcedureName() {
    return "dbo." + SqlSink.getMetricRecordProc;
  }

  @Override
  protected String getDatabaseDriverClassName() {
    return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  }
}