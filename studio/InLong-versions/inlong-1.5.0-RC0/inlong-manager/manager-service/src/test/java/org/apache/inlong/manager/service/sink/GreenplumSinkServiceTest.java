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
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumColumnInfo;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumSink;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumSinkRequest;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumTableInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.resource.sink.greenplum.GreenplumJdbcUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Greenplum sink service test
 */
public class GreenplumSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group1";
    private static final String globalStreamId = "stream1";
    private static final String globalOperator = "admin";
    private static final String fieldName = "greenplum_field";
    private static final String fieldType = "greenplum_type";
    private static final Integer fieldId = 1;

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        GreenplumSinkRequest sinkInfo = new GreenplumSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.GREENPLUM);

        sinkInfo.setJdbcUrl("jdbc:postgresql://localhost:5432/greenplum");
        sinkInfo.setUsername("greenplum");
        sinkInfo.setPassword("inlong");
        sinkInfo.setTableName("user");
        sinkInfo.setPrimaryKey("name,age");

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        SinkField sinkField = new SinkField();
        sinkField.setFieldName(fieldName);
        sinkField.setFieldType(fieldType);
        sinkField.setId(fieldId);
        List<SinkField> sinkFieldList = new ArrayList<>();
        sinkFieldList.add(sinkField);
        sinkInfo.setSinkFieldList(sinkFieldList);
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
        Integer sinkId = this.saveSink("greenplum_default1");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("greenplum_default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        GreenplumSink sink = (GreenplumSink) streamSink;
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
        final String url = "jdbc:postgresql://127.0.0.1:5432/testdb";
        final String username = "test";
        final String password = "123456";
        final String tableName = "test02";
        final String schemaName = "public";

        try (Connection connection = GreenplumJdbcUtils.getConnection(url, username, password)) {
            GreenplumTableInfo tableInfo = bulidTestGreenplumTableInfo(username, schemaName, tableName);
            GreenplumJdbcUtils.createTable(connection, tableInfo);
            List<GreenplumColumnInfo> addColumns = buildAddColumns();
            GreenplumJdbcUtils.addColumns(connection, schemaName, tableName, addColumns);
            List<GreenplumColumnInfo> columns = GreenplumJdbcUtils.getColumns(connection, schemaName, tableName);
            Assertions.assertEquals(columns.size(), tableInfo.getColumns().size() + addColumns.size());
        } catch (Exception e) {
            // print to local console
            e.printStackTrace();
        }
    }

    /**
     * Build add Greenplum column info.
     *
     * @return {@link List}
     */
    private List<GreenplumColumnInfo> buildAddColumns() {
        List<GreenplumColumnInfo> addColums = Lists.newArrayList(
                new GreenplumColumnInfo("test1", "int", "test1"),
                new GreenplumColumnInfo("test2", "varchar(30)", "test2"),
                new GreenplumColumnInfo("Test1", "varchar(50)", "Test1"));
        return addColums;
    }

    /**
     * Build test Greenplum table info.
     *
     * @param userName Greenplum database name
     * @param tableName Greenplum table name
     * @return {@link GreenplumTableInfo}
     */
    private GreenplumTableInfo bulidTestGreenplumTableInfo(final String userName, final String schemaName,
            final String tableName) {
        List<GreenplumColumnInfo> columns = Lists.newArrayList(
                new GreenplumColumnInfo("id", "int", "id"),
                new GreenplumColumnInfo("cell", "varchar(25)", "cell"),
                new GreenplumColumnInfo("name", "varchar(50)", "name"));
        final GreenplumTableInfo tableInfo = new GreenplumTableInfo();
        tableInfo.setColumns(columns);
        tableInfo.setTableName(tableName);
        tableInfo.setPrimaryKey("id");
        tableInfo.setSchemaName(schemaName);
        tableInfo.setComment(tableName);
        return tableInfo;
    }
}
