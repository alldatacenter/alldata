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
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-17 09:58
 **/
public class TestHiveDBUtils {

    @Test
    public void test1() throws Exception {
        System.out.println(  Class.forName("org.apache.http.conn.HttpClientConnectionManager") );
    }

    @Test
    public void testConn() throws Exception {
        HiveDBUtils dbUtils = HiveDBUtils.getInstance("192.168.28.200:10000", "default");

        try (DataSourceMeta.JDBCConnection conn = dbUtils.createConnection()) {
            Statement statement = conn.createStatement();
            try (ResultSet resultSet = statement.executeQuery("select count(instance_id) as countt,`type` FROM  instancedetail where 1=0 group by type")) {

                ResultSetMetaData metaData = (resultSet.getMetaData());
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    //metaData.
                    System.out.println("name:" + metaData.getColumnName(i) + ",type:" + metaData.getColumnType(i));
                }

            }
        }
    }


}
