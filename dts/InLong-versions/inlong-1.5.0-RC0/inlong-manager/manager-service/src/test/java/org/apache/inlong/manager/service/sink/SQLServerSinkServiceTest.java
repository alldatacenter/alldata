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
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerColumnInfo;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerSink;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerSinkRequest;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerTableInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.resource.sink.sqlserver.SQLServerJdbcUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;

/**
 * SQLServer sink service test
 */
public class SQLServerSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group1_sqlserver";
    private static final String globalStreamId = "stream1_sqlserver";
    private static final String globalOperator = "admin";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId,
                globalOperator);
        SQLServerSinkRequest sinkInfo = new SQLServerSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.SQLSERVER);

        sinkInfo.setJdbcUrl("jdbc:sqlserver://localhost:5432/sqlserver");
        sinkInfo.setUsername("sqlserver");
        sinkInfo.setPassword("inlong");
        sinkInfo.setTableName("user");
        sinkInfo.setSchemaName("test");
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
        Integer sinkId = this.saveSink("sqlserver_default1");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("sqlserver_default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        SQLServerSink sink = (SQLServerSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        SQLServerSinkRequest request = CommonBeanUtils.copyProperties(sink, SQLServerSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

    /**
     * Just using in local test.
     */
    @Disabled
    public void testDbResource() {
        final String url = "jdbc:sqlserver://127.0.0.1:1434;databaseName=inlong;";
        final String username = "sa";
        final String password = "123456";
        final String tableName = "test01";
        final String schemaName = "dbo";

        try (Connection connection = SQLServerJdbcUtils.getConnection(url, username, password);) {
            SQLServerTableInfo tableInfo = bulidTableInfo(schemaName, tableName);
            SQLServerJdbcUtils.createSchema(connection, schemaName);
            SQLServerJdbcUtils.createTable(connection, tableInfo);
            List<SQLServerColumnInfo> addColumns = buildAddColumns();
            SQLServerJdbcUtils.addColumns(connection, schemaName, tableName, addColumns);
            List<SQLServerColumnInfo> columns = SQLServerJdbcUtils.getColumns(connection, schemaName, tableName);
            Assertions.assertEquals(columns.size(), tableInfo.getColumns().size() + addColumns.size());
        } catch (Exception e) {
            // print to local console
            e.printStackTrace();
        }
    }

    /**
     * Build add SQLServer column info.
     *
     * @return {@link List}
     */
    private final List<SQLServerColumnInfo> buildAddColumns() {
        List<SQLServerColumnInfo> addCloums = Lists.newArrayList(
                new SQLServerColumnInfo("test1", "varchar(40)", "test1"),
                new SQLServerColumnInfo("test2", "varchar(40)", "test2"));
        return addCloums;
    }

    /**
     * Build test SQLServer table info.
     *
     * @param schemaName SqlServer schema name
     * @param tableName SqlServer table name
     * @return {@link SQLServerTableInfo}
     */
    private final SQLServerTableInfo bulidTableInfo(final String schemaName, final String tableName) {
        SQLServerTableInfo tableInfo = new SQLServerTableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setComment("test01 ");
        tableInfo.setPrimaryKey("id");
        tableInfo.setSchemaName(schemaName);

        List<SQLServerColumnInfo> columnInfos = Lists.newArrayList(
                new SQLServerColumnInfo("id", "int", "id"),
                new SQLServerColumnInfo("cell", "varchar(20)", "cell"),
                new SQLServerColumnInfo("name", "varchar(40)", "name"));
        tableInfo.setColumns(columnInfos);
        return tableInfo;
    }

}
