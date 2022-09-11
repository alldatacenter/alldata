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

package org.apache.inlong.manager.service.core.sink;

import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresColumnInfo;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSink;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSinkRequest;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresTableInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.resource.postgres.PostgresJdbcUtils;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stream sink service test
 */
public class PostgresStreamSinkServiceTest extends ServiceBaseTest {

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
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId,
                globalOperator);
        PostgresSinkRequest sinkInfo = new PostgresSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.SINK_POSTGRES);

        sinkInfo.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        sinkInfo.setUsername("postgres");
        sinkInfo.setPassword("inlong");
        sinkInfo.setDbName("public");
        sinkInfo.setTableName("user");
        sinkInfo.setPrimaryKey("name,age");

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(GlobalConstants.DISABLE_CREATE_RESOURCE);
        return sinkService.save(sinkInfo, globalOperator);
    }

    /**
     * Delete postgres sink info by sink id.
     */
    public void deletePostgresSink(Integer postgresSinkId) {
        boolean result = sinkService.delete(postgresSinkId, globalOperator);
        Assert.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer postgresSinkId = this.saveSink("postgres_default1");
        StreamSink sink = sinkService.get(postgresSinkId);
        Assert.assertEquals(globalGroupId, sink.getInlongGroupId());
        deletePostgresSink(postgresSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer postgresSinkId = this.saveSink("postgres_default2");
        StreamSink response = sinkService.get(postgresSinkId);
        Assert.assertEquals(globalGroupId, response.getInlongGroupId());

        PostgresSink postgresSink = (PostgresSink) response;
        postgresSink.setEnableCreateResource(GlobalConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = postgresSink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assert.assertTrue(result);
        deletePostgresSink(postgresSinkId);
    }

    /**
     * Just using in local test
     */
    @Ignore
    public void testDbResource() {
        String url = "jdbc:postgresql://localhost:5432/test";
        String user = "postgres";
        String password = "123456";
        String dbName = "test";
        String tableName = "test_123";

        try {
            PostgresJdbcUtils.createDb(url, user, password, dbName);
            List<PostgresColumnInfo> columnInfoList = new ArrayList<>();
            PostgresColumnInfo info = new PostgresColumnInfo();
            info.setType("integer");
            info.setName("id");
            columnInfoList.add(info);
            PostgresColumnInfo info2 = new PostgresColumnInfo();
            info2.setType("integer");
            info2.setName("age");
            columnInfoList.add(info2);

            PostgresColumnInfo info3 = new PostgresColumnInfo();
            info3.setType("integer");
            info3.setName("high");
            columnInfoList.add(info3);

            PostgresTableInfo tableInfo = new PostgresTableInfo();
            tableInfo.setDbName(dbName);
            tableInfo.setColumns(columnInfoList);
            tableInfo.setTableName(tableName);

            boolean tableExists = PostgresJdbcUtils.checkTablesExist(url, user, password, dbName, tableName);
            if (!tableExists) {
                PostgresJdbcUtils.createTable(url, user, password, tableInfo);
            } else {
                List<PostgresColumnInfo> existColumns = PostgresJdbcUtils.getColumns(url, user, password, tableName);
                List<String> columnNameList = new ArrayList<>();
                existColumns.forEach(columnInfo -> columnNameList.add(columnInfo.getName()));

                List<PostgresColumnInfo> needAddColumns = tableInfo.getColumns().stream()
                        .filter((columnInfo) -> !columnNameList.contains(columnInfo.getName()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(needAddColumns)) {
                    PostgresJdbcUtils.addColumns(url, user, password, tableName, needAddColumns);
                }
            }
        } catch (Exception e) {
            // print to local console
            e.printStackTrace();
        }
    }

}
