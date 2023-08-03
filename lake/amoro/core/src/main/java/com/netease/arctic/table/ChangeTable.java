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

package com.netease.arctic.table;

import com.netease.arctic.scan.ChangeTableIncrementalScan;

/**
 * Change table store of an {@link KeyedTable}, storing change records in it.
 */
public interface ChangeTable extends UnkeyedTable {
  /**
   * Create a new {@link ChangeTableIncrementalScan scan} for this table.
   *
   * @return a table scan for this table
   */
  ChangeTableIncrementalScan newScan();
}
