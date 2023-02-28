/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.dump.hive.HiveDBUtils;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-08 14:35
 **/
public enum MREngine {
    SPARK(2), HIVE(1);

    private final int columnIndexGetTableName;

    public List<String> getTabs(Connection connection, EntityName dumpTable) throws Exception {
        final List<String> tables = new ArrayList<>();
        // SPARK:
        //        +-----------+---------------+--------------+--+
        //        | database  |   tableName   | isTemporary  |
        //        +-----------+---------------+--------------+--+
        //        | order     | totalpayinfo  | false        |
        //        +-----------+---------------+--------------+--+
        // Hive
        HiveDBUtils.query(connection, "show tables in " + dumpTable.getDbName()
                , result -> tables.add(result.getString(this.columnIndexGetTableName)));
        return tables;
    }

    private MREngine(int columnIndexGetTableName) {
        this.columnIndexGetTableName = columnIndexGetTableName;
    }
}
