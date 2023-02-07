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
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.oracle.OracleColumnInfo;
import org.apache.inlong.manager.pojo.sink.oracle.OracleSink;
import org.apache.inlong.manager.pojo.sink.oracle.OracleSinkRequest;
import org.apache.inlong.manager.pojo.sink.oracle.OracleTableInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.resource.sink.oracle.OracleJdbcUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle sink service test
 */
public class OracleSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group1_oracle";
    private static final String globalStreamId = "stream_oracle";
    private static final String globalOperator = "admin";
    private static final String fieldName = "oracle_field";
    private static final String fieldType = "oracle_type";
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
        OracleSinkRequest sinkInfo = new OracleSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.ORACLE);

        sinkInfo.setJdbcUrl("jdbc:oracle:thin@host:port/database");
        sinkInfo.setUsername("oracle");
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
        Integer oracleSinkId = this.saveSink("oracle_default1");
        StreamSink sink = sinkService.get(oracleSinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(oracleSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("oracle_default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        OracleSink sink = (OracleSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        OracleSinkRequest request = CommonBeanUtils.copyProperties(sink, OracleSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

    /**
     * Just using in local test.
     */
    @Disabled
    public void testDbResource() {
        final String url = "jdbc:oracle:thin:@localhost:1521/ORCLCDB";
        final String username = "c###inlong_test";
        final String password = "123456";
        final String tableName = "test02";

        try (Connection connection = OracleJdbcUtils.getConnection(url, username, password)) {
            OracleTableInfo tableInfo = bulidTestOracleTableInfo(username, tableName);
            OracleJdbcUtils.createTable(connection, tableInfo);
            List<OracleColumnInfo> addColumns = buildAddColumns();
            OracleJdbcUtils.addColumns(connection, tableName, addColumns);
            List<OracleColumnInfo> columns = OracleJdbcUtils.getColumns(connection, tableName);
            Assertions.assertEquals(columns.size(), tableInfo.getColumns().size() + addColumns.size());
        } catch (Exception e) {
            // print to local console
            e.printStackTrace();
        }
    }

    /**
     * Build add Oracle column info.
     *
     * @return {@link List}
     */
    private List<OracleColumnInfo> buildAddColumns() {
        List<OracleColumnInfo> list = Lists.newArrayList(
                new OracleColumnInfo("test1", "NUMBER(16)", "test1"),
                new OracleColumnInfo("test2", "VARCHAR2(10)", "test2"));
        return list;
    }

    /**
     * Build test Oracle table info.
     *
     * @param userName Oracle database name
     * @param tableName Oracle table name
     * @return {@link OracleTableInfo}
     */
    private OracleTableInfo bulidTestOracleTableInfo(final String userName, final String tableName) {
        OracleTableInfo oracleTableInfo = new OracleTableInfo();
        oracleTableInfo.setTableName(tableName);
        oracleTableInfo.setUserName(userName);

        List<OracleColumnInfo> columnInfos = Lists.newArrayList(
                new OracleColumnInfo("id", "NUMBER(6)", "id"),
                new OracleColumnInfo("cell", "VARCHAR2(10)", "cell"),
                new OracleColumnInfo("name", "VARCHAR2(20)", "name"));
        oracleTableInfo.setColumns(columnInfos);

        return oracleTableInfo;
    }

}
