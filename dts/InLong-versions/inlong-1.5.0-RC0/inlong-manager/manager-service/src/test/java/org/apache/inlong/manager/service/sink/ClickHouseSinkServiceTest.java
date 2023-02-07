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

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSink;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ClickHouse sink service test
 */
public class ClickHouseSinkServiceTest extends ServiceBaseTest {

    // Partial test data
    private static final String globalGroupId = "b_group1";
    private static final String globalStreamId = "stream1_clickhouse";
    private static final String globalOperator = "admin";
    private static final String ckJdbcUrl = "jdbc:clickhouse://127.0.0.1:8123/default";
    private static final String ckUsername = "ck_user";
    private static final String ckDatabaseName = "ck_db";
    private static final String ckTableName = "ck_tbl";
    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        ClickHouseSinkRequest sinkInfo = new ClickHouseSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkName(sinkName);
        sinkInfo.setSinkType(SinkType.CLICKHOUSE);
        sinkInfo.setJdbcUrl(ckJdbcUrl);
        sinkInfo.setUsername(ckUsername);
        sinkInfo.setDbName(ckDatabaseName);
        sinkInfo.setTableName(ckTableName);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        sinkInfo.setId((int) (Math.random() * 100000 + 1));
        return sinkService.save(sinkInfo, globalOperator);
    }

    /**
     * Delete sink by sink id.
     */
    public void deleteSink(Integer sinkId) {
        boolean result = sinkService.delete(sinkId, false, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer sinkId = this.saveSink("default1");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        ClickHouseSink sink = (ClickHouseSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

}
