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

package org.apache.inlong.manager.service.source.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.kafka.KafkaClusterInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.kafka.KafkaOffset;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSourceDTO;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSourceRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.service.cluster.InlongClusterService;
import org.apache.inlong.manager.service.source.AbstractSourceOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * kafka stream source operator
 */
@Service
public class KafkaSourceOperator extends AbstractSourceOperator {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InlongClusterService clusterService;

    @Override
    public Boolean accept(String sourceType) {
        return SourceType.KAFKA.equals(sourceType);
    }

    @Override
    protected String getSourceType() {
        return SourceType.KAFKA;
    }

    @Override
    protected void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity) {
        KafkaSourceRequest sourceRequest = (KafkaSourceRequest) request;
        CommonBeanUtils.copyProperties(sourceRequest, targetEntity, true);
        try {
            KafkaSourceDTO dto = KafkaSourceDTO.getFromRequest(sourceRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    @Override
    public StreamSource getFromEntity(StreamSourceEntity entity) {
        KafkaSource source = new KafkaSource();
        if (entity == null) {
            return source;
        }

        KafkaSourceDTO dto = KafkaSourceDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, source, true);
        CommonBeanUtils.copyProperties(dto, source, true);

        List<StreamField> sourceFields = super.getSourceFields(entity.getId());
        source.setFieldList(sourceFields);
        return source;
    }

    @Override
    public Map<String, List<StreamSource>> getSourcesMap(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfos,
            List<StreamSource> streamSources) {
        ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.KAFKA);
        KafkaClusterInfo kafkaClusterInfo = (KafkaClusterInfo) clusterInfo;
        String bootstrapServers = kafkaClusterInfo.getUrl();

        Map<String, List<StreamSource>> sourceMap = Maps.newHashMap();
        streamInfos.forEach(streamInfo -> {
            KafkaSource kafkaSource = new KafkaSource();
            String streamId = streamInfo.getInlongStreamId();
            kafkaSource.setSourceName(streamId);
            kafkaSource.setBootstrapServers(bootstrapServers);
            kafkaSource.setTopic(streamInfo.getMqResource());
            String serializationType = DataTypeEnum.forType(streamInfo.getDataType()).getType();
            kafkaSource.setSerializationType(serializationType);
            kafkaSource.setIgnoreParseError(streamInfo.getIgnoreParseError());

            for (StreamSource sourceInfo : streamSources) {
                if (!Objects.equals(streamId, sourceInfo.getInlongStreamId())) {
                    continue;
                }
                if (StringUtils.isEmpty(kafkaSource.getSerializationType()) && StringUtils.isNotEmpty(
                        sourceInfo.getSerializationType())) {
                    kafkaSource.setSerializationType(sourceInfo.getSerializationType());
                }
            }

            kafkaSource.setWrapWithInlongMsg(streamInfo.getWrapWithInlongMsg());

            kafkaSource.setAutoOffsetReset(KafkaOffset.EARLIEST.getName());
            kafkaSource.setFieldList(streamInfo.getFieldList());
            sourceMap.computeIfAbsent(streamId, key -> Lists.newArrayList()).add(kafkaSource);
        });
        return sourceMap;
    }
}
