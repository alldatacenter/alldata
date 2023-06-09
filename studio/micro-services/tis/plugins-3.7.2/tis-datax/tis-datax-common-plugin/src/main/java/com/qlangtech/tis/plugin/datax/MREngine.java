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

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-07-08 14:35
 **/
public enum MREngine {
    SPARK("spark", "SHOW DATABASES"), HIVE("hive", SPARK.showDBSQL), AliyunODPS("odps", "LIST PROJECTS;");

    // private final int columnIndexGetTableName;
    private final String token;
    public final String showDBSQL;

//    public List<String> getTabs(Connection connection, EntityName dumpTable) throws Exception {
//        Objects.requireNonNull(dumpTable);
//        Objects.requireNonNull(dumpTable.getDbName());
//        final List<String> tables = new ArrayList<>();
//        // SPARK:
//        //        +-----------+---------------+--------------+--+
//        //        | database  |   tableName   | isTemporary  |
//        //        +-----------+---------------+--------------+--+
//        //        | order     | totalpayinfo  | false        |
//        //        +-----------+---------------+--------------+--+
//        // Hive
//        HiveDBUtils.query(connection, "show tables in " + dumpTable.getDbName()
//                , result -> tables.add(result.getString(this.columnIndexGetTableName)));
//        return tables;
//    }

    private MREngine(String token, String showDBSQL) {
        //  this.columnIndexGetTableName = columnIndexGetTableName;
        this.token = token;
        this.showDBSQL = showDBSQL;
    }

    public String getToken() {
        return token;
    }
}
