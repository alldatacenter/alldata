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
import org.apache.inlong.manager.common.pojo.sink.SinkField;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.greenplum.GreenplumSink;
import org.apache.inlong.manager.common.pojo.sink.greenplum.GreenplumSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream sink service test
 */
public class GreenplumStreamSinkServiceTest extends ServiceBaseTest {

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
        sinkInfo.setSinkType(SinkType.SINK_GREENPLUM);

        sinkInfo.setJdbcUrl("jdbc:postgresql://localhost:5432/greenplum");
        sinkInfo.setUsername("greenplum");
        sinkInfo.setPassword("inlong");
        sinkInfo.setTableName("user");
        sinkInfo.setPrimaryKey("name,age");

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(GlobalConstants.DISABLE_CREATE_RESOURCE);
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
     * Delete greenplum sink info by sink id.
     */
    public void deleteGreenplumSink(Integer greenplumSinkId) {
        boolean result = sinkService.delete(greenplumSinkId, globalOperator);
        Assert.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer greenplumSinkId = this.saveSink("greenplum_default1");
        StreamSink sink = sinkService.get(greenplumSinkId);
        Assert.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteGreenplumSink(greenplumSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer greenplumSinkId = this.saveSink("greenplum_default2");
        StreamSink response = sinkService.get(greenplumSinkId);
        Assert.assertEquals(globalGroupId, response.getInlongGroupId());

        GreenplumSink greenplumSink = (GreenplumSink) response;
        greenplumSink.setEnableCreateResource(GlobalConstants.ENABLE_CREATE_RESOURCE);
        SinkRequest request = greenplumSink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assert.assertTrue(result);
        deleteGreenplumSink(greenplumSinkId);
    }

}
