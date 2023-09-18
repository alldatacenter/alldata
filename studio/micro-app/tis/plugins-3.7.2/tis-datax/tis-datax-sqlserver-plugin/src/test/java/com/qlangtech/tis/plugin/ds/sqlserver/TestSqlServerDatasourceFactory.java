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

package com.qlangtech.tis.plugin.ds.sqlserver;

import com.qlangtech.tis.plugin.common.PluginDesc;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.DescriptorsJSON;
import junit.framework.TestCase;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-23 17:46
 **/
public class TestSqlServerDatasourceFactory extends TestCase {

    public void testDescriptorDescGenerate() throws Exception {

        PluginDesc.testDescGenerate(SqlServerDatasourceFactory.class, "sqlserver-datax-ds-factory-descriptor.json");

//        SqlServerDatasourceFactory dsFactory = new SqlServerDatasourceFactory();
//        DescriptorsJSON descJson = new DescriptorsJSON(dsFactory.getDescriptor());
//
//        JsonUtil.assertJSONEqual(SqlServerDatasourceFactory.class, "sqlserver-datax-ds-factory-descriptor.json"
//                , descJson.getDescriptorsJSON(), (m, e, a) -> {
//                    assertEquals(m, e, a);
//                });
    }


//    public void testGetTablesInDB() throws Exception {
//        String tableName = "instancedetail";
//        SqlServerDatasourceFactory dbFactory = new SqlServerDatasourceFactory();
//        dbFactory.dbName = "tis";
//        dbFactory.password = "Hello1234!";
//        dbFactory.userName = "sa";
//        dbFactory.port = 1433;
//        dbFactory.nodeDesc = "192.168.28.201";
//
////        Connection conn = dbFactory.getConnection(dbFactory.buidJdbcUrl(null, dbFactory.nodeDesc, dbFactory.dbName));
////        assertNotNull(conn);
////        Statement statement = conn.createStatement();
////
////       // ResultSet resultSet = statement.executeQuery("select \"name\" from sys.tables where is_ms_shipped = 1");
////        ResultSet resultSet = statement.executeQuery("select count(1) from instancedetail");
////        while (resultSet.next()) {
////            System.out.println(resultSet.getString(1));
////        }
////
////        resultSet.close();
////        statement.close();
////        conn.close();
//        List<String> tablesInDB = dbFactory.getTablesInDB();
//        assertEquals(1, tablesInDB.size());
//
//        List<ColumnMetaData> cols = dbFactory.getTableMetadata(tableName);
//        assertEquals(59, cols.size());
//        for (ColumnMetaData col : cols) {
//            System.out.println(col.getKey() + "," + col.getType() + ",ispk:" + col.isPk());
//        }
//    }
}
