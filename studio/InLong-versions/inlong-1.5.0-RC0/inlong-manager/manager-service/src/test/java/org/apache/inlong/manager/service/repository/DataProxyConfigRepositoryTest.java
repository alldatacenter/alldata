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

package org.apache.inlong.manager.service.repository;

import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSinkEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * DataProxyConfigRepositoryTest
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({InlongGroupEntityMapper.class, StreamSinkEntityMapper.class, InlongClusterEntityMapper.class})
public class DataProxyConfigRepositoryTest {

    private static final String INLONG_GROUP_ID = "03a00000026";
    private static final String INLONG_STREAM_ID = "1";
    private static final String CLUSTER_TAG_NEW = "ct_new";
    private static final String CLUSTER_TAG_OLD = "ct_old";
    private static final String TOPIC_NEW = "t_03a00000026";
    private static final String TOPIC_OLD = "t_03a00000026";
    private static final String CLS_CLUSTER_NEW = "cls_new";
    private static final String CLS_CLUSTER_OLD = "cls_old";
    private static final String CLS_DATA_NODE_NEW = "sid_cls_new";
    private static final String CLS_DATA_NODE_OLD = "sid_cls_old";

    @Test
    public void testChangeClusterTag() {
        DataProxyConfigRepository repository = new DataProxyConfigRepository();
        repository.setInlongGroupMapper(this.mockGroupMapper());
        repository.setClusterMapper(this.mockClusterMapper());
        repository.setStreamSinkMapper(this.mockStreamSinkMapper());
        String inlongGroupId = repository.changeClusterTag(INLONG_GROUP_ID, CLUSTER_TAG_NEW, TOPIC_NEW);
        assertEquals(inlongGroupId, INLONG_GROUP_ID);
    }

    private InlongGroupEntityMapper mockGroupMapper() {
        InlongGroupEntity entity = new InlongGroupEntity();
        entity.setInlongGroupId(INLONG_GROUP_ID);
        entity.setInlongClusterTag(CLUSTER_TAG_OLD);
        entity.setMqResource(TOPIC_OLD);
        InlongGroupEntityMapper mapper = PowerMockito.mock(InlongGroupEntityMapper.class);
        PowerMockito.when(mapper.selectByGroupId(anyString())).thenReturn(entity);
        PowerMockito.when(mapper.updateByIdentifierSelective(any())).thenReturn(1);
        return mapper;
    }

    private InlongClusterEntityMapper mockClusterMapper() {
        final List<InlongClusterEntity> clusters = new ArrayList<>();
        InlongClusterEntity dataProxyCluster = new InlongClusterEntity();
        dataProxyCluster.setName("dp_1");
        dataProxyCluster.setType("DATAPROXY");
        dataProxyCluster.setClusterTags(CLUSTER_TAG_OLD);
        dataProxyCluster.setExtTag("sz=true");
        dataProxyCluster.setExtParams("{}");
        clusters.add(dataProxyCluster);
        InlongClusterEntity pulsarCluster = new InlongClusterEntity();
        pulsarCluster.setName("pc_1");
        pulsarCluster.setType("PULSAR");
        pulsarCluster.setClusterTags(CLUSTER_TAG_OLD);
        pulsarCluster.setExtTag("sz=true&producer=true&consumer=true");
        pulsarCluster.setExtParams("{}");
        clusters.add(pulsarCluster);
        InlongClusterEntity clsCluster = new InlongClusterEntity();
        clsCluster.setName(CLS_CLUSTER_OLD);
        clsCluster.setType("cls");
        clsCluster.setClusterTags(CLUSTER_TAG_OLD);
        clsCluster.setExtParams(String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}",
                DataProxyConfigRepository.KEY_SINK_NAME, CLS_DATA_NODE_OLD,
                DataProxyConfigRepository.KEY_SORT_TASK_NAME, CLS_DATA_NODE_OLD,
                DataProxyConfigRepository.KEY_DATA_NODE_NAME, CLS_DATA_NODE_OLD,
                DataProxyConfigRepository.KEY_SORT_CONSUMER_GROUP, CLS_DATA_NODE_OLD));
        clusters.add(clsCluster);
        InlongClusterEntity kafkaCluster = new InlongClusterEntity();
        kafkaCluster.setName("kafka_1");
        kafkaCluster.setType("kafka");
        kafkaCluster.setExtParams("{}");
        clusters.add(kafkaCluster);
        InlongClusterEntity clsCluster2 = new InlongClusterEntity();
        clsCluster2.setName(CLS_CLUSTER_NEW);
        clsCluster2.setType("cls");
        clsCluster2.setClusterTags(CLUSTER_TAG_NEW);
        clsCluster2.setExtParams(String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}",
                DataProxyConfigRepository.KEY_SINK_NAME, CLS_DATA_NODE_NEW,
                DataProxyConfigRepository.KEY_SORT_TASK_NAME, CLS_DATA_NODE_NEW,
                DataProxyConfigRepository.KEY_DATA_NODE_NAME, CLS_DATA_NODE_NEW,
                DataProxyConfigRepository.KEY_SORT_CONSUMER_GROUP, CLS_DATA_NODE_NEW));
        clusters.add(clsCluster2);
        InlongClusterEntityMapper mapper = PowerMockito.mock(InlongClusterEntityMapper.class);
        PowerMockito.when(mapper.selectByCondition(any())).thenReturn(clusters);
        return mapper;
    }

    private StreamSinkEntityMapper mockStreamSinkMapper() {
        final List<StreamSinkEntity> streamSinks = new ArrayList<>();
        StreamSinkEntity clsSink = new StreamSinkEntity();
        clsSink.setInlongGroupId(INLONG_GROUP_ID);
        clsSink.setInlongStreamId(INLONG_STREAM_ID);
        clsSink.setInlongClusterName(CLS_CLUSTER_OLD);
        clsSink.setSinkName(CLS_DATA_NODE_OLD);
        clsSink.setSortTaskName(CLS_DATA_NODE_OLD);
        clsSink.setDataNodeName(CLS_DATA_NODE_OLD);
        clsSink.setSortConsumerGroup(CLS_DATA_NODE_OLD);
        clsSink.setSinkType("cls");
        streamSinks.add(clsSink);
        StreamSinkEntity kafkaSink = new StreamSinkEntity();
        kafkaSink.setInlongGroupId(INLONG_GROUP_ID);
        kafkaSink.setInlongStreamId(INLONG_STREAM_ID);
        kafkaSink.setInlongClusterName("kafka_1");
        kafkaSink.setSinkName("sid_" + kafkaSink.getInlongClusterName());
        kafkaSink.setSortTaskName("sid_" + kafkaSink.getInlongClusterName());
        kafkaSink.setDataNodeName("sid_" + kafkaSink.getInlongClusterName());
        kafkaSink.setSortConsumerGroup("sid_" + kafkaSink.getInlongClusterName());
        kafkaSink.setSinkType("kafka");
        streamSinks.add(kafkaSink);
        StreamSinkEntityMapper mapper = PowerMockito.mock(StreamSinkEntityMapper.class);
        PowerMockito.when(mapper.selectByCondition(any())).thenReturn(streamSinks);
        PowerMockito.when(mapper.insert(any())).thenReturn(1);
        PowerMockito.when(mapper.deleteById(anyInt())).thenReturn(1);
        return mapper;
    }

    @Test
    public void testRemoveBackupClusterTag() {
        final DataProxyConfigRepository repository = new DataProxyConfigRepository();
        // group
        InlongGroupEntityMapper groupMapper = this.mockGroupMapper();
        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(INLONG_GROUP_ID);
        groupEntity.setInlongClusterTag(CLUSTER_TAG_NEW);
        groupEntity.setMqResource(TOPIC_NEW);
        groupEntity.setExtParams(
                String.format("{\"%s\":\"%s\",\"%s\":\"%s\"}", DataProxyConfigRepository.KEY_BACKUP_CLUSTER_TAG,
                        CLUSTER_TAG_OLD, DataProxyConfigRepository.KEY_BACKUP_TOPIC, TOPIC_OLD));
        repository.setInlongGroupMapper(groupMapper);
        // cluster
        repository.setClusterMapper(this.mockClusterMapper());
        // stream sink
        StreamSinkEntityMapper streamSinkMapper = this.mockStreamSinkMapper();
        SinkPageRequest sinkPageRequest = new SinkPageRequest();
        final List<StreamSinkEntity> streamSinks = streamSinkMapper.selectByCondition(sinkPageRequest);
        StreamSinkEntity clsSink = new StreamSinkEntity();
        clsSink.setInlongGroupId(INLONG_GROUP_ID);
        clsSink.setInlongStreamId(INLONG_STREAM_ID);
        clsSink.setInlongClusterName(CLS_CLUSTER_NEW);
        clsSink.setSinkName(CLS_DATA_NODE_NEW);
        clsSink.setSortTaskName(CLS_DATA_NODE_NEW);
        clsSink.setDataNodeName(CLS_DATA_NODE_NEW);
        clsSink.setSortConsumerGroup(CLS_DATA_NODE_NEW);
        clsSink.setSinkType("cls");
        streamSinks.add(clsSink);
        repository.setStreamSinkMapper(streamSinkMapper);
        // test
        String inlongGroupId = repository.removeBackupClusterTag(INLONG_GROUP_ID);
        assertEquals(INLONG_GROUP_ID, inlongGroupId);
    }
}
