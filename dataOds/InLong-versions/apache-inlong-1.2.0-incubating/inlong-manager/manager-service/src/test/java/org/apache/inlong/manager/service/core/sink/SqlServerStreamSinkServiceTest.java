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

import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.sqlserver.SqlServerSink;
import org.apache.inlong.manager.common.pojo.sink.sqlserver.SqlServerSinkRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stream sink service test
 */
public class SqlServerStreamSinkServiceTest extends ServiceBaseTest {

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
        SqlServerSinkRequest sinkInfo = new SqlServerSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.SINK_SQLSERVER);

        sinkInfo.setJdbcUrl("jdbc:sqlserver://localhost:5432/sqlserver");
        sinkInfo.setUsername("sqlserver");
        sinkInfo.setPassword("inlong");
        sinkInfo.setTableName("user");
        sinkInfo.setSchemaName("test");
        sinkInfo.setPrimaryKey("name,age");

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(GlobalConstants.DISABLE_CREATE_RESOURCE);
        return sinkService.save(sinkInfo, globalOperator);
    }

    /**
     * Delete sqlserver sink info by sink id.
     */
    public void deleteSqlServerSink(Integer sqlserverSinkId) {
        boolean result = sinkService.delete(sqlserverSinkId, globalOperator);
        Assert.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer sqlserverSinkId = this.saveSink("sqlserver_default1");
        StreamSink sink = sinkService.get(sqlserverSinkId);
        Assert.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSqlServerSink(sqlserverSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sqlserverSinkId = this.saveSink("sqlserver_default2");
        StreamSink response = sinkService.get(sqlserverSinkId);
        Assert.assertEquals(globalGroupId, response.getInlongGroupId());

        SqlServerSink sqlServerSink = (SqlServerSink) response;
        sqlServerSink.setEnableCreateResource(GlobalConstants.ENABLE_CREATE_RESOURCE);

        SqlServerSinkRequest request = CommonBeanUtils.copyProperties(sqlServerSink,
                SqlServerSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assert.assertTrue(result);
        deleteSqlServerSink(sqlserverSinkId);
    }

}
