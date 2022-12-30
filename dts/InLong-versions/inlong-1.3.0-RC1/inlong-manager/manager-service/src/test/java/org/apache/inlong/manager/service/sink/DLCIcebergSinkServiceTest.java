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
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.dlciceberg.DLCIcebergSink;
import org.apache.inlong.manager.pojo.sink.dlciceberg.DLCIcebergSinkRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * DLCIceberg sink service test..
 */
public class DLCIcebergSinkServiceTest extends ServiceBaseTest {

    private static final String fieldName = "dlc_field";
    private static final String fieldType = "dlc_type";
    private static final Integer fieldId = 1;
    private final String globalGroupId = "b_group1";
    private final String globalStreamId = "stream1_dlciceberg";
    private final String globalOperator = "admin";

    @Autowired
    private StreamSinkService sinkService;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;

    /**
     * Save sink info.
     */
    public Integer saveSink(String sinkName) {
        streamServiceTest.saveInlongStream(globalGroupId, globalStreamId, globalOperator);
        DLCIcebergSinkRequest sinkInfo = new DLCIcebergSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.DLCICEBERG);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        SinkField sinkField = new SinkField();
        sinkField.setFieldName(fieldName);
        sinkField.setFieldType(fieldType);
        sinkField.setId(fieldId);
        List<SinkField> sinkFieldList = new ArrayList<>();
        sinkFieldList.add(sinkField);
        sinkInfo.setSinkFieldList(sinkFieldList);

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setId((int) (Math.random() * 100000 + 1));
        return sinkService.save(sinkInfo, globalOperator);
    }

    @Test
    public void testSaveAndDelete() {
        Integer sinkId = this.saveSink("default1");
        Assertions.assertNotNull(sinkId);
        boolean result = sinkService.delete(sinkId, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer sinkId = this.saveSink("default2");
        StreamSink sink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        sinkService.delete(sinkId, globalOperator);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("default3");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        DLCIcebergSink sink = (DLCIcebergSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        SinkRequest request = sink.genSinkRequest();
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        sinkService.delete(sinkId, globalOperator);
    }

}
