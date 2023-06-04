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

package org.apache.inlong.audit.service;

import org.apache.inlong.audit.db.entities.ESDataPo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles(value = {"test"})
@SpringBootTest(classes = ElasticsearchServiceTest.class)
public class ElasticsearchServiceTest {

    private static ElasticsearchService elasticsearchService;

    private final String index = "20220112_1";

    @BeforeClass
    public static void setUp() throws IOException {
        elasticsearchService = mock(ElasticsearchService.class);
        when(elasticsearchService.existsIndex(Mockito.anyString())).thenReturn(true);
        when(elasticsearchService.createIndex(Mockito.anyString())).thenReturn(true);
        when(elasticsearchService.deleteSingleIndex(Mockito.anyString())).thenReturn(true);
    }

    @Test
    public void testExistsIndex() throws IOException {
        boolean res = elasticsearchService.createIndex(index);
        Assert.assertTrue(res);

        res = elasticsearchService.existsIndex(index);
        Assert.assertTrue(res);
    }

    @Test
    public void testInsertData() {
        for (int i = 0; i < 5; i++) {
            ESDataPo po = new ESDataPo();
            po.setIp("0.0.0.0");
            po.setThreadId(String.valueOf(i));
            po.setDockerId(String.valueOf(i));
            po.setSdkTs(new Date().getTime());
            po.setLogTs(new Date());
            po.setAuditId("1");
            po.setCount(i);
            po.setDelay(i);
            po.setInlongGroupId(String.valueOf(i));
            po.setInlongStreamId(String.valueOf(i));
            po.setSize(i);
            po.setPacketId(i);
            elasticsearchService.insertData(po);
        }
    }

    @Test
    public void testDeleteSingleIndex() throws IOException {
        boolean res = elasticsearchService.createIndex(index);
        Assert.assertTrue(res);
        res = elasticsearchService.deleteSingleIndex(index);
        Assert.assertTrue(res);
    }

    @Test
    public void testDeleteTimeoutIndices() throws IOException {
        elasticsearchService.deleteTimeoutIndices();
    }

}
