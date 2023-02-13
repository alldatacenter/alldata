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
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQL sink service test
 */
public class MySQLStreamSinkServiceTest extends ServiceBaseTest {

    private static final String globalGroupId = "b_group1_mysql";
    private static final String globalStreamId = "stream1_mysql";
    private static final String globalOperator = "admin";
    private static final String fieldName = "mysql_field";
    private static final String fieldType = "mysql_type";
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
        MySQLSinkRequest sinkInfo = new MySQLSinkRequest();
        sinkInfo.setInlongGroupId(globalGroupId);
        sinkInfo.setInlongStreamId(globalStreamId);
        sinkInfo.setSinkType(SinkType.MYSQL);

        sinkInfo.setJdbcUrl("jdbc:mysql://localhost:5432/test_db");
        sinkInfo.setUsername("mysql");
        sinkInfo.setPassword("inlong");
        sinkInfo.setTableName("user");
        sinkInfo.setPrimaryKey("name,age");

        sinkInfo.setSinkName(sinkName);
        sinkInfo.setEnableCreateResource(InlongConstants.DISABLE_CREATE_RESOURCE);
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
     * Delete sink info by sink id.
     */
    public void deleteSink(Integer sinkId) {
        boolean result = sinkService.delete(sinkId, false, globalOperator);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer mysqlSinkId = this.saveSink("mysql_default1");
        StreamSink sink = sinkService.get(mysqlSinkId);
        Assertions.assertEquals(globalGroupId, sink.getInlongGroupId());
        deleteSink(mysqlSinkId);
    }

    @Test
    public void testGetAndUpdate() {
        Integer sinkId = this.saveSink("mysql_default2");
        StreamSink streamSink = sinkService.get(sinkId);
        Assertions.assertEquals(globalGroupId, streamSink.getInlongGroupId());

        MySQLSink sink = (MySQLSink) streamSink;
        sink.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        MySQLSinkRequest request = CommonBeanUtils.copyProperties(sink, MySQLSinkRequest::new);
        boolean result = sinkService.update(request, globalOperator);
        Assertions.assertTrue(result);

        deleteSink(sinkId);
    }

}
