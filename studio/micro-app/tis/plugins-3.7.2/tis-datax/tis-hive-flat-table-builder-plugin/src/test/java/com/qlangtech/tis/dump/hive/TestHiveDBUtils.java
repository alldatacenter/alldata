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

package com.qlangtech.tis.dump.hive;

import com.qlangtech.tis.plugin.ds.DataSourceMeta;
import junit.framework.TestCase;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author: baisui 百岁
 * @create: 2020-05-29 14:19
 **/
public class TestHiveDBUtils extends TestCase {

    public void testTetConnection() throws Exception {
        HiveDBUtils dbUtils = HiveDBUtils.getInstance("192.168.28.200:10000", "tis");

        try (DataSourceMeta.JDBCConnection con = dbUtils.createConnection()) {

            // // Connection con = DriverManager.getConnection(
            // // "jdbc:hive://10.1.6.211:10000/tis", "", "");
            // System.out.println("start create connection");
            // // Connection con = DriverManager.getConnection(
            // // "jdbc:hive2://hadoop6:10001/tis", "", "");
            // System.out.println("create conn");
            System.out.println("start execute");
            Statement stmt = con.createStatement();
            //
            ResultSet result = stmt.executeQuery("select 1");
            System.out.println("wait receive");
            if (result.next()) {
                System.out.println(result.getInt(1));
            }
        }
    }
}
