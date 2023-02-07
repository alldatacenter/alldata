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
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.hdfs.HDFSSink;
import org.apache.inlong.manager.pojo.sink.hdfs.HDFSSinkRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class HDFSStreamSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group_hdfs";
    private static final String globalStreamId = "stream1_hdfs";
    private static final String globalOperator = "admin";
    private static final String fileFormat = "TextFile";
    private static final String dataPath = "hdfs://ip:port/usr/hive/warehouse/test.db";
    private static final String serverTimeZone = "GMT%2b8";
    private static final String fieldName = "hdfs_field";
    private static final String fieldType = "hdfs_type";
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

        HDFSSinkRequest hdfsSinkRequest = new HDFSSinkRequest();
        hdfsSinkRequest.setInlongGroupId(globalGroupId);
        hdfsSinkRequest.setInlongStreamId(globalStreamId);
        hdfsSinkRequest.setSinkType(SinkType.HDFS);
        hdfsSinkRequest.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
        hdfsSinkRequest.setSinkName(sinkName);
        hdfsSinkRequest.setFileFormat(fileFormat);
        hdfsSinkRequest.setDataPath(dataPath);
        hdfsSinkRequest.setServerTimeZone(serverTimeZone);
        SinkField sinkField = new SinkField();
        sinkField.setFieldName(fieldName);
        sinkField.setFieldType(fieldType);
        sinkField.setId(fieldId);
        List<SinkField> sinkFieldList = new ArrayList<>();
        sinkFieldList.add(sinkField);
        hdfsSinkRequest.setSinkFieldList(sinkFieldList);
        return sinkService.save(hdfsSinkRequest, globalOperator);
    }

    /**
     * Delete sink info by sink id.
     */
    public void deleteSink(Integer sinkId) {
        boolean result = sinkService.delete(sinkId, false, globalOperator);
        // Verify that the deletion was successful
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer sinkId = this.saveSink("default_hdfs");
        StreamSink sink = sinkService.get(sinkId);
        // verify globalGroupId
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(sinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("default_hdfs");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        HDFSSink sink = (HDFSSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        HDFSSinkRequest request = CommonBeanUtils.copyProperties(sink, HDFSSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);
        deleteSink(sinkId);
    }

}
