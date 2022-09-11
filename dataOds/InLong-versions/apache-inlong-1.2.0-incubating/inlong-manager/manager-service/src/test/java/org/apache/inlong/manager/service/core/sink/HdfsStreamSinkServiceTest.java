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
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.hdfs.HdfsSink;
import org.apache.inlong.manager.common.pojo.sink.hdfs.HdfsSinkRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class HdfsStreamSinkServiceTest extends ServiceBaseTest {

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

        HdfsSinkRequest hdfsSinkRequest = new HdfsSinkRequest();
        hdfsSinkRequest.setInlongGroupId(globalGroupId);
        hdfsSinkRequest.setInlongStreamId(globalStreamId);
        hdfsSinkRequest.setSinkType(SinkType.SINK_HDFS);
        hdfsSinkRequest.setEnableCreateResource(GlobalConstants.DISABLE_CREATE_RESOURCE);
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
     * Delete Hdfs sink info by sink id.
     */
    public void deleteHdfsSink(Integer hdfsSinkId) {
        boolean result = sinkService.delete(hdfsSinkId, globalOperator);
        // Verify that the deletion was successful
        Assert.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer hdfsSinkId = this.saveSink("default_hdfs");
        StreamSink sink = sinkService.get(hdfsSinkId);
        // verify globalGroupId
        Assert.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteHdfsSink(hdfsSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer hdfsSinkId = this.saveSink("default_hdfs");
        StreamSink response = sinkService.get(hdfsSinkId);
        Assert.assertEquals(globalGroupId, response.getInlongGroupId());

        HdfsSink hdfsSink = (HdfsSink) response;
        hdfsSink.setEnableCreateResource(GlobalConstants.ENABLE_CREATE_RESOURCE);

        HdfsSinkRequest request = CommonBeanUtils.copyProperties(hdfsSink, HdfsSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assert.assertTrue(result);
        deleteHdfsSink(hdfsSinkId);
    }

}
