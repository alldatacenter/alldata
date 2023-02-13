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

package org.apache.inlong.manager.service.consume;

import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterRequest;
import org.apache.inlong.manager.pojo.common.OrderFieldEnum;
import org.apache.inlong.manager.pojo.common.OrderTypeEnum;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.pulsar.ConsumePulsarRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.core.impl.InlongStreamServiceTest;
import org.apache.inlong.manager.service.group.InlongGroupServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for {@link InlongConsumeServiceImpl}
 */
public class InlongConsumeServiceTest extends ServiceBaseTest {

    private final String groupId = "consume_group_id";
    private final String streamId = "consume_stream_id";
    private final String consumerGroup = "test_consumer_group";
    private final String deadLetterTopic = "test_dlp";
    private Integer clusterId;

    @Autowired
    private InlongConsumeService consumeService;
    @Autowired
    private InlongGroupServiceTest groupServiceTest;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;
    @Autowired
    private InlongClusterService clusterService;

    @BeforeEach
    public void before() {
        groupServiceTest.saveGroup(groupId, GLOBAL_OPERATOR);
        streamServiceTest.saveInlongStream(groupId, streamId, GLOBAL_OPERATOR);
        // before saving inlong consume, the related MQ cluster must exist
        this.saveCluster();
    }

    @Test
    public void testAll() {
        // test save operation
        Integer consumeId = this.testSave();
        Assertions.assertNotNull(consumeId);

        // test get operation
        InlongConsumeInfo consumeInfo = this.testGet(consumeId);
        Assertions.assertEquals(consumeInfo.getId(), consumeId);

        // test list operation
        Assertions.assertTrue(this.testList().getPageSize() > 0);

        // test count status operation
        InlongConsumeCountInfo countInfo = testCountStatus();
        Assertions.assertNotNull(countInfo);

        // test update operation
        Assertions.assertNotNull(this.testUpdate(consumeInfo));

        // test delete operation
        Assertions.assertTrue(this.testDelete(consumeId));
    }

    @AfterEach
    public void after() {
        streamServiceTest.deleteStream(groupId, streamId, GLOBAL_OPERATOR);
        // Current status=to_be_submit was not allowed to delete
        // groupServiceTest.deleteGroup(groupId, GLOBAL_OPERATOR);
        // before saving inlong consume, the related MQ cluster must exist
        clusterService.delete(clusterId, GLOBAL_OPERATOR);
    }

    private Integer testSave() {
        ConsumePulsarRequest request = new ConsumePulsarRequest();
        request.setInlongGroupId(groupId);
        request.setInlongStreamId(streamId);
        request.setMqType(MQType.PULSAR);
        request.setTopic(streamId);
        request.setConsumerGroup(consumerGroup);
        request.setInCharges(GLOBAL_OPERATOR);
        request.setIsDlq(1);
        request.setDeadLetterTopic(deadLetterTopic);
        request.setIsRlq(0);
        return consumeService.save(request, GLOBAL_OPERATOR);
    }

    private InlongConsumeInfo testGet(Integer id) {
        return consumeService.get(id);
    }

    private PageResult<InlongConsumeBriefInfo> testList() {
        InlongConsumePageRequest request = new InlongConsumePageRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setOrderField(OrderFieldEnum.CREATE_TIME.name());
        request.setOrderType(OrderTypeEnum.DESC.name());
        request.setConsumerGroup(consumerGroup);
        return consumeService.list(request);
    }

    private Integer testUpdate(InlongConsumeInfo consumeInfo) {
        ConsumePulsarRequest request = new ConsumePulsarRequest();
        request.setId(consumeInfo.getId());
        request.setMqType(MQType.PULSAR);
        request.setInlongGroupId(groupId);
        request.setIsDlq(1);
        request.setDeadLetterTopic(deadLetterTopic);
        request.setIsRlq(0);
        request.setVersion(consumeInfo.getVersion());
        return consumeService.update(request, GLOBAL_OPERATOR);
    }

    private InlongConsumeCountInfo testCountStatus() {
        return consumeService.countStatus(GLOBAL_OPERATOR);
    }

    private Boolean testDelete(Integer id) {
        return consumeService.delete(id, GLOBAL_OPERATOR);
    }

    private void saveCluster() {
        PulsarClusterRequest request = new PulsarClusterRequest();
        String clusterTag = "consume_cluster_tag";
        request.setClusterTags(clusterTag);
        String clusterName = "consume_pulsar_cluster";
        request.setName(clusterName);
        request.setType(ClusterType.PULSAR);
        String adminUrl = "http://127.0.0.1:8080";
        request.setAdminUrl(adminUrl);
        request.setInCharges(GLOBAL_OPERATOR);
        clusterId = clusterService.save(request, GLOBAL_OPERATOR);
    }

}
