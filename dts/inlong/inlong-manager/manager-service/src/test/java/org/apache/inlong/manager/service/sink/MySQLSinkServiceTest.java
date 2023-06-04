/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.sink;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLColumnInfo;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkRequest;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLTableInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.resource.sink.mysql.MySQLJdbcUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;

/**
 * MySQL sink service test.
 */
public class MySQLSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group1";
    private static final String globalStreamId = "stream1";
    private static final String globalOperator = "admin";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    private Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        MySQLSinkRequest sinkInfo = new MySQLSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.MYSQL);

        sinkInfo.setJdbcUrl("jdbc:mysql://localhost:3306/test01");
        sinkInfo.setUsername("inlong");
        sinkInfo.setPassword("mysql");
        sinkInfo.setTableName("user");
        sinkInfo.setPrimaryKey("name,age");

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        return sinkService.save(sinkInfo, globalOperator);
    }

    /**
     * Delete sink info by sink id.
     */
    public void deleteSink(Integer sinkId) {
        boolean result = sinkService.delete(sinkId, false, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer sinkId = this.saveSink("mysql_default1");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("mysql_default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        MySQLSink sink = (MySQLSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

    /**
     * Just using in local test.
     */
    @Disabled
    public void testDbResource() {
        final String url = "jdbc:mysql://localhost:3306/test01";
        final String user = "inlong";
        final String password = "mysql";
        final String dbName = "test01";
        final String tableName = "test_table02";

        try (Connection connection = MySQLJdbcUtils.getConnection(url, user, password)) {
            MySQLJdbcUtils.createDb(connection, dbName);
            MySQLTableInfo tableInfo = bulidTestMySQLTableInfo(dbName, tableName);
            MySQLJdbcUtils.createTable(connection, tableInfo);
            List<MySQLColumnInfo> addColumns = buildAddColumns();
            MySQLJdbcUtils.addColumns(connection, dbName, tableName, addColumns);
            List<MySQLColumnInfo> columns = MySQLJdbcUtils.getColumns(connection, dbName, tableName);
            Assertions.assertEquals(columns.size(), tableInfo.getColumns().size() + addColumns.size());
        } catch (Exception e) {
            // print to local consoleS
            e.printStackTrace();
        }
    }

    /**
     * Build add MySQL column info.
     *
     * @return {@link List}
     */
    private List<MySQLColumnInfo> buildAddColumns() {
        List<MySQLColumnInfo> list = Lists.newArrayList(
                new MySQLColumnInfo("add_column1", "int(12)", ""),
                new MySQLColumnInfo("add_column2", "varchar(22)", ""));
        return list;
    }

    /**
     * Build test mysql table info.
     *
     * @param dbName MySQL database name
     * @param tableName MySQL table name
     * @return {@link MySQLTableInfo}
     */
    private MySQLTableInfo bulidTestMySQLTableInfo(final String dbName, final String tableName) {
        List<MySQLColumnInfo> columnInfoList = Lists.newArrayList(
                new MySQLColumnInfo("id", "int(12)", "id"),
                new MySQLColumnInfo("age", "int(12)", "age"),
                new MySQLColumnInfo("cell", "varchar(20)", "cell"),
                new MySQLColumnInfo("name", "varchar(40)", "name"),
                new MySQLColumnInfo("create_time", "datetime", "create time"));

        MySQLTableInfo tableInfo = new MySQLTableInfo();
        tableInfo.setColumns(columnInfoList);
        tableInfo.setDbName(dbName);
        tableInfo.setTableName(tableName);
        tableInfo.setPrimaryKey("id");
        return tableInfo;
    }

}
