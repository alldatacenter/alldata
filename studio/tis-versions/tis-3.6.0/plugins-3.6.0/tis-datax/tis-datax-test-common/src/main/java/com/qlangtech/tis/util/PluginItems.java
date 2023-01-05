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

package com.qlangtech.tis.util;

import com.google.common.collect.Sets;
import com.qlangtech.tis.manage.common.Option;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: baisui 百岁
 * @create: 2021-04-09 12:18
 **/
public class PluginItems {

    public static final String DB_NAME = "baisuiMySQL";
    public static final String DB_MONGODB_NAME = "teatMongoDB";
    public static final String DB_TIDB_NAME = "testTiKV";
    public static final String DB_CLICKHOUSE_NAME = "clickhouseDB";
    public static final String DB_CASSANDRA_NAME = "cassandraDB";

    public static final String DB_POSTGRE_SQL = "PostgreSQLDB";
    public static final String DB_SQL_Server = "SqlServerDB";
    public static final String DB_SQL_ORACLE = "OracleDB";

    public static final String DB_SQL_DORIS = "DorisDB1";
    public static final String DB_SQL_StarRocks = "StarRocksDB1";


    public static List<Option> getExistDbs(String... extendClass) {

        Set<String> extendClazzs = Sets.newHashSet("MySQL-V5", "MySQL-V8");

        for (String sourceType : extendClass) {
            if (extendClazzs.contains(sourceType)) {
                return Collections.singletonList(new Option(DB_NAME, DB_NAME));
                //Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        Set<String> mongoDBextendClazzs = Sets.newHashSet("MongoDB");
        for (String sourceType : extendClass) {
            if (mongoDBextendClazzs.contains(sourceType)) {
                return Collections.singletonList(new Option(DB_MONGODB_NAME, DB_MONGODB_NAME));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }


        for (String sourceType : extendClass) {
            if ("TiDB".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_TIDB_NAME, DB_TIDB_NAME));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }


        for (String sourceType : extendClass) {
            if ("ClickHouse".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_CLICKHOUSE_NAME, DB_CLICKHOUSE_NAME));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        for (String sourceType : extendClass) {
            if ("Cassandra".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_CASSANDRA_NAME, DB_CASSANDRA_NAME));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        for (String sourceType : extendClass) {
            if ("PostgreSQL".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_POSTGRE_SQL, DB_POSTGRE_SQL));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        for (String sourceType : extendClass) {
            if ("SqlServer".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_SQL_Server, DB_SQL_Server));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        for (String sourceType : extendClass) {
            if ("Oracle".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_SQL_ORACLE, DB_SQL_ORACLE));

                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        for (String sourceType : extendClass) {
            if ("Doris".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_SQL_DORIS, DB_SQL_DORIS));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }

        for (String sourceType : extendClass) {
            if ("StarRocks".equals(sourceType)) {
                return Collections.singletonList(new Option(DB_SQL_StarRocks, DB_SQL_StarRocks));
                //  Assert.fail("param:" + sourceType + " must contain in:" + extendClazzs.stream().collect(Collectors.joining(",")));
            }
        }


//        String expectExtendClass = com.qlangtech.tis.plugin.ds.mysql.MySQLDataSourceFactory.class.getName();
//        if (!expectExtendClass.equals(extendClass)) {
//            Assert.fail("param:" + extendClass + " must equal with:" + expectExtendClass);
//        }


        throw new IllegalStateException("param:" + Arrays.stream(extendClass).collect(Collectors.joining(",")) + " must contain is illegal");
    }


}
