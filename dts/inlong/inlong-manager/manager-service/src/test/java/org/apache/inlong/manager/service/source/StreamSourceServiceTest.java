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

package org.apache.inlong.manager.service.source;

import com.google.common.collect.Maps;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSource;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSourceRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Stream source service test
 */
public class StreamSourceServiceTest extends ServiceBaseTest {

    @Autowired
    private StreamSourceService sourceService;
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
        String sourceName = "stream_source_service_test";
        sourceInfo.setSourceName(sourceName);
        sourceInfo.setSourceType(SourceType.MYSQL_BINLOG);
        Map<String, Object> properties = Maps.newLinkedHashMap();
        properties.put("append-mode", "true");
        sourceInfo.setProperties(properties);
        return sourceService.save(sourceInfo, GLOBAL_OPERATOR);
    }

    @Test
    public void testSaveAndDelete() {
        Integer id = this.saveSource();
        Assertions.assertNotNull(id);

        boolean result = sourceService.delete(id, GLOBAL_OPERATOR);
        Assertions.assertTrue(result);
    }

    @Test
    public void testListByIdentifier() {
        Integer id = this.saveSource();

        StreamSource source = sourceService.get(id);
        Assertions.assertEquals(GLOBAL_GROUP_ID, source.getInlongGroupId());

        sourceService.delete(id, GLOBAL_OPERATOR);
    }

    @Test
    public void testGetAndUpdate() {
        Integer id = this.saveSource();
        StreamSource response = sourceService.get(id);
        Assertions.assertEquals(GLOBAL_GROUP_ID, response.getInlongGroupId());

        MySQLBinlogSource binlogResponse = (MySQLBinlogSource) response;
        MySQLBinlogSourceRequest request = CommonBeanUtils.copyProperties(binlogResponse,
                MySQLBinlogSourceRequest::new);
        boolean result = sourceService.update(request, GLOBAL_OPERATOR);
        Assertions.assertTrue("true".equals(binlogResponse.getProperties().get("append-mode")) && result);

        sourceService.delete(id, GLOBAL_OPERATOR);
    }

}
