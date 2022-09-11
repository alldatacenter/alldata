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
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.common.pojo.sink.es.ElasticsearchSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Stream sink service test
 */
public class ElasticsearchStreamSinkServiceTest extends ServiceBaseTest {

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
        ElasticsearchSinkRequest sinkInfo = new ElasticsearchSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.SINK_ELASTICSEARCH);

        sinkInfo.setHost("127.0.0.1");
        sinkInfo.setUsername("elasticsearch");
        sinkInfo.setPassword("inlong");
        sinkInfo.setDocumentType("public");
        sinkInfo.setIndexName("index");
        sinkInfo.setPrimaryKey("name,age");
        sinkInfo.setVersion(7);

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(GlobalConstants.DISABLE_CREATE_RESOURCE);
        return sinkService.save(sinkInfo, globalOperator);
    }

    /**
     * Delete Elasticsearch sink info by sink id.
     */
    public void deleteElasticsearchSink(Integer elasticsearchSinkId) {
        boolean result = sinkService.delete(elasticsearchSinkId, globalOperator);
        Assert.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer elasticsearchSinkId = this.saveSink("Elasticsearch_default1");
        StreamSink sink = sinkService.get(elasticsearchSinkId);
        Assert.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteElasticsearchSink(elasticsearchSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer elasticsearchSinkId = this.saveSink("Elasticsearch_default2");
        StreamSink response = sinkService.get(elasticsearchSinkId);
        Assert.assertEquals(globalGroupId, response.getInlongGroupId());

        ElasticsearchSink elasticsearchSink = (ElasticsearchSink) response;
        elasticsearchSink.setEnableCreateResource(GlobalConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = elasticsearchSink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assert.assertTrue(result);
        deleteElasticsearchSink(elasticsearchSinkId);
    }

}
