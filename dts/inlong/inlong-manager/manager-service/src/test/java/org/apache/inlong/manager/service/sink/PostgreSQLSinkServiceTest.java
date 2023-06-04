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
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLColumnInfo;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLSink;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLSinkRequest;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLTableInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.resource.sink.postgresql.PostgreSQLJdbcUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.List;

/**
 * PostgreSQL sink service test
 */
public class PostgreSQLSinkServiceTest extends ServiceBaseTest {

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
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        PostgreSQLSinkRequest sinkInfo = new PostgreSQLSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.POSTGRESQL);

        sinkInfo.setJdbcUrl("jdbc:postgresql://localhost:5432/test_db");
        sinkInfo.setUsername("postgresql");
        sinkInfo.setPassword("inlong");
        sinkInfo.setDbName("public");
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
        Integer sinkId = this.saveSink("postgresql_default1");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("postgresql_default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        PostgreSQLSink sink = (PostgreSQLSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

    /**
     * Just using in local test
     */
    @Disabled
    public void testDbResource() {
        String url = "jdbc:postgresql://localhost:5432/testdb";
        String username = "pguser";
        String password = "123456";
        String tableName = "test01";
        String schemaName = "public";

        try (Connection connection = PostgreSQLJdbcUtils.getConnection(url, username, password)) {
            PostgreSQLTableInfo tableInfo = bulidTestPostgreSQLTableInfo(username, schemaName, tableName);
            PostgreSQLJdbcUtils.createTable(connection, tableInfo);
            List<PostgreSQLColumnInfo> addColumns = buildAddColumns();
            PostgreSQLJdbcUtils.addColumns(connection, schemaName, tableName, addColumns);
            List<PostgreSQLColumnInfo> columns = PostgreSQLJdbcUtils.getColumns(connection, schemaName, tableName);
            Assertions.assertEquals(columns.size(), tableInfo.getColumns().size() + addColumns.size());
        } catch (Exception e) {
            // print to local console
            e.printStackTrace();
        }
    }

    /**
     * Build add PostgreSQL column info.
     *
     * @return {@link List}
     */
    private List<PostgreSQLColumnInfo> buildAddColumns() {
        List<PostgreSQLColumnInfo> addColums = Lists.newArrayList(
                new PostgreSQLColumnInfo("test1", "int", "test1"),
                new PostgreSQLColumnInfo("test2", "varchar(30)", "test2"),
                new PostgreSQLColumnInfo("Test1", "varchar(50)", "Test1"));
        return addColums;
    }

    /**
     * Build test PostgreSQL table info.
     *
     * @param userName PostgreSQL database name
     * @param tableName PostgreSQL table name
     * @return {@link PostgreSQLTableInfo}
     */
    private PostgreSQLTableInfo bulidTestPostgreSQLTableInfo(final String userName, final String schemaName,
            final String tableName) {
        List<PostgreSQLColumnInfo> columns = Lists.newArrayList(
                new PostgreSQLColumnInfo("id", "int", "id"),
                new PostgreSQLColumnInfo("cell", "varchar(25)", "cell"),
                new PostgreSQLColumnInfo("name", "varchar(50)", "name"));
        final PostgreSQLTableInfo tableInfo = new PostgreSQLTableInfo();
        tableInfo.setColumns(columns);
        tableInfo.setTableName(tableName);
        tableInfo.setPrimaryKey("id");
        tableInfo.setSchemaName(schemaName);
        tableInfo.setComment(tableName);
        return tableInfo;
    }

}
