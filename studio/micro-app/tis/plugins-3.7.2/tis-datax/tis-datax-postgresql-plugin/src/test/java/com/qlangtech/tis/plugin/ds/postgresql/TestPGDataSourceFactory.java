/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.tis.plugin.ds.postgresql;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-22 14:15
 **/
public class TestPGDataSourceFactory extends TestCase {
    public void testShowTables() {

        // String createSQL = IOUtils.loadResourceFromClasspath(TestPGDataSourceFactory.class, "create-sql-instancedetail.sql");

        // 之后数据库如果没有了可以用下面的这个SQL 再把数据库给跑起来的
        // System.out.println(createSQL);

        PGDataSourceFactory dsFactory = new PGDataSourceFactory();
        dsFactory.userName = "postgres";
        dsFactory.password = "123456";
        dsFactory.dbName = "tis";
        dsFactory.port = 5432;
        dsFactory.nodeDesc = "192.168.28.201";

//        pg_toast
//        pg_temp_1
//        pg_toast_temp_1
//        pg_catalog
//        public
//        information_schema

        dsFactory.tabSchema = "tis";

       // System.out.println("tables:" + dsFactory.getTablesInDB().stream().collect(Collectors.joining(" ,")));

        dsFactory.visitAllConnection((c) -> {
            Connection conn = c.getConnection();

            try (Statement statement = conn.createStatement()) {
                statement.execute("    CREATE TABLE public.\"instancedetail\"\n" +
                        "(\n" +
                        "    \"instance_id\"            VARCHAR(32) PRIMARY KEY,\n" +
                        "    \"order_id\"               VARCHAR(32))");
            }
//
//            SELECT schema_name
//            ;

//            try (Statement statement = conn.createStatement()) {
//                try (ResultSet resultSet = statement.executeQuery("SELECT schema_name FROM information_schema.schemata")) {
//                  while(resultSet.next()){
//                      System.out.println(resultSet.getString(1));
//                  }
//
//                }
//            }

            Time t = Time.valueOf("18:22:00");
//            try (PreparedStatement statement = conn.prepareStatement("insert into time_test(id,time_c) values(?,?)")) {
//
//                statement.setInt(1, 1);
//                statement.setTime(2, t);
//
//                statement.executeUpdate();
//            }

//            try (Statement statement = conn.createStatement()) {
//                try (ResultSet resultSet = statement.executeQuery("select id,time_c from time_test")) {
//                    while (resultSet.next()) {
//                        System.out.print(resultSet.getObject(1));
//                        System.out.print("=");
//                        System.out.print(resultSet.getObject(2));
//                        System.out.println();
//                    }
//                }
//            }

        });

//        String instancedetail = "instancedetail";
//        List<String> tables = dsFactory.getTablesInDB();
//        assertEquals(1, tables.size());
//        tables.contains(instancedetail);
//
//
//        List<ColumnMetaData> tableMetadata = dsFactory.getTableMetadata(instancedetail);
//        assertTrue(tableMetadata.size() > 0);
//
//        for (ColumnMetaData col : tableMetadata) {
//            System.out.println(col.getKey() + "  " + col.getType());
//        }
    }
}
