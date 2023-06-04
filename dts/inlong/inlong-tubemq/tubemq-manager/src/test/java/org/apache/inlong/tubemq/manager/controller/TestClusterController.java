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

package org.apache.inlong.tubemq.manager.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.tubemq.manager.controller.cluster.request.AddClusterReq;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.repository.ClusterRepository;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TestClusterController {

    private final Gson gson = new Gson();

    @MockBean
    private MasterRepository masterRepository;

    @MockBean
    private ClusterRepository clusterRepository;

    @MockBean
    private NodeService nodeService;

    @MockBean
    private MasterService masterService;

    @Autowired
    private MockMvc mockMvc;

    private MasterEntry getNodeEntry() {
        MasterEntry masterEntry = new MasterEntry();
        masterEntry.setIp("127.0.0.1");
        masterEntry.setWebPort(8014);
        return masterEntry;
    }

    @Test
    public void testTopicQuery() throws Exception {
        MasterEntry masterEntry = getNodeEntry();
        when(masterRepository.findMasterEntryByClusterIdEquals(any(Integer.class)))
                .thenReturn(masterEntry);
        RequestBuilder request = get(
                "/v1/cluster/query?method=admin_query_topic_info&type=op_query&clusterId=1");
        MvcResult result = mockMvc.perform(request).andReturn();
        String resultStr = result.getResponse().getContentAsString();
        log.info("result json string is {}, response type is {}", resultStr,
                result.getResponse().getContentType());
    }

    @Test
    public void testBrokerQuery() throws Exception {
        MasterEntry masterEntry = getNodeEntry();
        when(masterRepository.findMasterEntryByClusterIdEquals(any(Integer.class)))
                .thenReturn(masterEntry);
        RequestBuilder request = get(
                "/v1/cluster/query?method=admin_query_broker_run_status&type=op_query&clusterId=1&brokerIp=");
        MvcResult result = mockMvc.perform(request).andReturn();
        String resultStr = result.getResponse().getContentAsString();
        log.info("result json string is {}, response type is {}", resultStr,
                result.getResponse().getContentType());
    }

    @Test
    public void testTopicAndGroupQuery() throws Exception {
        MasterEntry masterEntry = getNodeEntry();
        when(masterRepository.findMasterEntryByClusterIdEquals(any(Integer.class)))
                .thenReturn(masterEntry);
        RequestBuilder request = get(
                "/v1/cluster/query?method=admin_query_sub_info"
                        + "&type=op_query&clusterId=1&topicName=test&groupName=test");
        MvcResult result = mockMvc.perform(request).andReturn();
        String resultStr = result.getResponse().getContentAsString();
        log.info("result json string is {}, response type is {}", resultStr,
                result.getResponse().getContentType());
    }

    @Test
    public void testTopicAdd() throws Exception {
        String jsonStr = "{\n"
                + "  \"type\": \"op_modify\",\n"
                + "  \"method\": \"admin_add_new_topic_record\",\n"
                + "  \"confModAuthToken\": \"test\",\n"
                + "  \"clusterId\": 1,\n"
                + "  \"createUser\": \"webapi\",\n"
                + "  \"topicName\": \"test\",\n"
                + "  \"deleteWhen\": \"0 0 0 0 0\",\n"
                + "  \"unflushThreshold\": 1000,\n"
                + "  \"acceptPublish\": true,\n"
                + "  \"numPartitions\": 3,\n"
                + "  \"deletePolicy\": \"\",\n"
                + "  \"unflushInterval\": 1000,\n"
                + "  \"acceptSubscribe\": true,\n"
                + "  \"brokerId\": 12323\n"
                + "}\n";
        MasterEntry masterEntry = getNodeEntry();
        when(masterRepository.findMasterEntryByClusterIdEquals(any(Integer.class)))
                .thenReturn(masterEntry);
        RequestBuilder request = post("/v1/cluster/modify")
                .contentType(MediaType.APPLICATION_JSON).content(jsonStr);
        MvcResult result = mockMvc.perform(request).andReturn();
        String resultStr = result.getResponse().getContentAsString();
        log.info("result json string is {}, response type is {}", resultStr,
                result.getResponse().getContentType());
    }

    private ClusterEntry getOneClusterEntry() {
        ClusterEntry clusterEntry = new ClusterEntry();
        clusterEntry.setClusterId(1);
        clusterEntry.setClusterName("test");
        return clusterEntry;
    }

    @Test
    public void testAddCluster() throws Exception {

        AddClusterReq req = new AddClusterReq();
        req.setId(4);
        req.setClusterName("test");
        MasterEntry masterEntry = new MasterEntry();
        masterEntry.setIp("127.0.0.1");
        masterEntry.setPort(8089);
        masterEntry.setWebPort(8080);
        List<MasterEntry> masterEntries = new ArrayList<>();
        masterEntries.add(masterEntry);
        req.setMasterEntries(masterEntries);
        req.setToken("abc");
        req.setReloadBrokerSize(2);

        ClusterEntry entry = getOneClusterEntry();
        TubeMQResult successResult = new TubeMQResult();
        when(clusterRepository.saveAndFlush(any(ClusterEntry.class))).thenReturn(entry);
        // when(nodeService.addNode(any(MasterEntry.class))).thenReturn(Boolean.TRUE);
        when(masterService.checkMasterNodeStatus(anyString(), anyInt())).thenReturn(successResult);

        RequestBuilder request = post("/v1/cluster?method=add")
                .contentType(MediaType.APPLICATION_JSON).content(gson.toJson(req));
        MvcResult result = mockMvc.perform(request).andReturn();
        String resultStr = result.getResponse().getContentAsString();
        String expectRes = "{\"errMsg\":\"\",\"errCode\":0,\"result\":true,\"data\":null,\"error\":false}";
        Assert.assertEquals(resultStr, expectRes);
    }
}
