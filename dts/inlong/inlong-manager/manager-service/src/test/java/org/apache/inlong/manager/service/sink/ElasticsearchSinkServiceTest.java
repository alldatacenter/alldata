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

import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.node.es.ElasticsearchDataNodeRequest;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.node.DataNodeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Elasticsearch sink service test
 */
public class ElasticsearchSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group1";
    private static final String globalStreamId = "stream1";
    private static final String globalOperator = "admin";
    private static final String TEST_CREATOR = "testUser";
    private static final String TEST_TOKEN = "12345";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;
    @Autowired
    DataNodeService dataNodeService;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName, String dataNodeName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        ElasticsearchSinkRequest sinkInfo = new ElasticsearchSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.ELASTICSEARCH);

        sinkInfo.setHosts("http://127.0.0.1:9200");
        sinkInfo.setUsername("elasticsearch");
        sinkInfo.setPassword("inlong");
        sinkInfo.setDocumentType("public");
        sinkInfo.setIndexName("index");
        sinkInfo.setPrimaryKey("name,age");
        sinkInfo.setEsVersion(7);
        sinkInfo.setDataNodeName(dataNodeName);

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
        String dataNodeName = "test_data_node_01";
        prepareEsDataNode(dataNodeName);
        Integer sinkId = this.saveSink("Elasticsearch_default1", dataNodeName);
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        String dataNodeName = "test_data_node_02";
        prepareEsDataNode(dataNodeName);
        Integer sinkId = this.saveSink("Elasticsearch_default2", dataNodeName);
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        ElasticsearchSink sink = (ElasticsearchSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

    private void prepareEsDataNode(String dataNodeName) {
        ElasticsearchDataNodeRequest request = new ElasticsearchDataNodeRequest();
        request.setName(dataNodeName);
        request.setType(DataNodeType.ELASTICSEARCH);
        request.setUrl("http://127.0.0.1:9200;http://127.0.0.1:9300");
        request.setExtParams(
                "{\"bulkAction\":4000,\"bulkSizeMb\":10,\"flushInterval\":60,\"concurrentRequests\":5,\"maxConnect\":10,\"keywordMaxLength\":32767,\"isUseIndexId\":false,\"maxThreads\":2,\"auditSetName\":null}");
        request.setInCharges(TEST_CREATOR);
        request.setUsername(TEST_CREATOR);
        request.setToken(TEST_TOKEN);
        dataNodeService.save(request, TEST_CREATOR);
    }
}
