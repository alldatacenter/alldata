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

package org.apache.inlong.manager.web.controller.openapi;

import org.apache.inlong.manager.dao.entity.SortClusterConfigEntity;
import org.apache.inlong.manager.dao.entity.SortTaskIdParamEntity;
import org.apache.inlong.manager.dao.entity.SortTaskSinkParamEntity;
import org.apache.inlong.manager.dao.mapper.SortClusterConfgiEntityMapper;
import org.apache.inlong.manager.dao.mapper.SortTaskIdParamEntityMapper;
import org.apache.inlong.manager.dao.mapper.SortTaskSinkParamEntityMapper;
import org.apache.inlong.manager.web.WebBaseTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for sort controller.
 */
public class SortControllerTest extends WebBaseTest {

    private static final Logger logger = LoggerFactory.getLogger(SortControllerTest.class);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    // todo Service do not support insert method now, use mappers to insert data.
    @Autowired
    private SortTaskIdParamEntityMapper taskIdParamEntityMapper;

    @Autowired
    private SortTaskSinkParamEntityMapper taskSinkParamEntityMapper;

    @Autowired
    private SortClusterConfgiEntityMapper sortClusterConfgiEntityMapper;

    // @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        taskIdParamEntityMapper.insert(this.prepareIdParamsEntity("testTask1", 1));
        taskIdParamEntityMapper.insert(this.prepareIdParamsEntity("testTask1", 2));
        taskIdParamEntityMapper.insert(this.prepareIdParamsEntity("testTask2", 1));
        taskSinkParamEntityMapper
                .insert(this.prepareSinkParamsEntity("testTask1", "kafka", 1));
        taskSinkParamEntityMapper
                .insert(this.prepareSinkParamsEntity("testTask2", "pulsar", 1));
        sortClusterConfgiEntityMapper.insert(this.prepareClusterConfigEntity("testTask1", "kafka"));
        sortClusterConfgiEntityMapper.insert(this.prepareClusterConfigEntity("testTask2", "pulsar"));
    }

    /**
     * Test if the interface works.
     *
     * @throws Exception Exceptions to request generating.
     */
    // @Test
    // @Transactional
    public void testGetSortClusterConfig() throws Exception {
        RequestBuilder request =
                get("/openapi/sort/getClusterConfig")
                        .param("clusterName", "testCluster")
                        .param("md5", "testMd5");
        mockMvc.perform(request).andExpect(status().isOk()).andDo(print());
    }

    // @Test
    // @Transactional
    public void testErrorSinkType() throws Exception {
        sortClusterConfgiEntityMapper.insert(
                this.prepareClusterConfigEntity("testTask1", "error type"));
        RequestBuilder request =
                get("/openapi/sort/getClusterConfig")
                        .param("clusterName", "testCluster")
                        .param("md5", "testMd5");
        mockMvc.perform(request).andExpect(status().isOk()).andDo(print());
    }

    // @Test
    // @Transactional
    public void testEmptyClusterNameWhenGet() throws Exception {
        RequestBuilder request =
                get("/openapi/sort/getClusterConfig")
                        .param("clusterName", "  ")
                        .param("md5", "testMd5");
        mockMvc.perform(request).andExpect(status().isOk()).andDo(print());
    }

    private SortTaskIdParamEntity prepareIdParamsEntity(String task, int idx) {
        return SortTaskIdParamEntity.builder()
                .groupId(String.valueOf(idx))
                .streamId(String.valueOf(idx))
                .taskName(task)
                .paramKey("idParamKey " + idx)
                .paramValue("idParamValue " + idx)
                .build();
    }

    private SortClusterConfigEntity prepareClusterConfigEntity(String taskName, String sinkType) {
        return SortClusterConfigEntity.builder()
                .clusterName("testCluster")
                .taskName(taskName)
                .sinkType(sinkType)
                .build();
    }

    private SortTaskSinkParamEntity prepareSinkParamsEntity(String task, String sinkType, int idx) {
        return SortTaskSinkParamEntity.builder()
                .sinkType(sinkType)
                .paramKey("sinkParamKey " + idx)
                .paramValue("sinkParamValue " + idx)
                .taskName(task)
                .build();
    }

    @Test
    public void defaultTest() {
        logger.info("Online exception druid connection timeout cannot create transaction, "
                + "add default test method.");
    }
}
