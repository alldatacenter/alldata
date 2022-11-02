/*
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

package org.apache.ambari.server.upgrade;

import java.sql.SQLException;

import org.apache.ambari.server.orm.DBAccessor;

/**
 * Interface to encapsulate the logic that a DDL update requires for a particular feature or collection of tables.
 * Typically, each section initializes capture groups, then creates tables/columns, and then verifies expectations.
 */
public interface SectionDDL {

  /**
   * Execute any commands to create table/columns and store in the capture group.
   * @param dbAccessor
   * @throws SQLException
   */
  void execute(DBAccessor dbAccessor) throws SQLException;

  /**
   * Retrieve the capture groups and make assertions about tables/columns created.
   * @param dbAccessor
   * @throws SQLException
   */
  void verify(DBAccessor dbAccessor) throws SQLException;
}
