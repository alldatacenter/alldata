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

package org.apache.inlong.manager.service.core.impl;

import org.apache.inlong.common.pojo.agent.TaskSnapshotMessage;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSourceRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.AgentService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Date;

/**
 * Agent service test
 */
class AgentServiceTest extends ServiceBaseTest {

    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save source info.
     */
    public Integer saveSource() {
        streamServiceTest.saveInlongStream(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, GLOBAL_OPERATOR);
        MySQLBinlogSourceRequest sourceInfo = new MySQLBinlogSourceRequest();
        sourceInfo.setInlongGroupId(GLOBAL_GROUP_ID);
        sourceInfo.setInlongStreamId(GLOBAL_STREAM_ID);
        sourceInfo.setSourceType(SourceType.MYSQL_BINLOG);
        sourceInfo.setSourceName("binlog_source_in_agent_service_test");
        return sourceService.save(sourceInfo, GLOBAL_OPERATOR);
    }

    /**
     * Test report snapshot.
     */
    @Test
    void testReportSnapshot() {
        Integer id = this.saveSource();

        TaskSnapshotRequest request = new TaskSnapshotRequest();
        request.setAgentIp("127.0.0.1");
        request.setReportTime(new Date());

        TaskSnapshotMessage message = new TaskSnapshotMessage();
        message.setJobId(id);
        message.setSnapshot("{\"offset\": 100}");
        request.setSnapshotList(Collections.singletonList(message));

        Boolean result = agentService.reportSnapshot(request);
        Assertions.assertTrue(result);

        sourceService.delete(id, GLOBAL_OPERATOR);
    }

}
