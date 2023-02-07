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

package org.apache.inlong.manager.service.source.tubemq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.tubemq.TubeClusterInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.tubemq.TubeMQSource;
import org.apache.inlong.manager.pojo.source.tubemq.TubeMQSourceDTO;
import org.apache.inlong.manager.pojo.source.tubemq.TubeMQSourceRequest;
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
 * TubeMQ source operator
 */
@Service
public class TubeMQSourceOperator extends AbstractSourceOperator {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private InlongClusterService clusterService;

    @Override
    public Boolean accept(String sourceType) {
        return SourceType.TUBEMQ.equals(sourceType);
    }

    @Override
    protected String getSourceType() {
        return SourceType.TUBEMQ;
    }

    @Override
    protected void setTargetEntity(SourceRequest request, StreamSourceEntity targetEntity) {
        TubeMQSourceRequest sourceRequest = (TubeMQSourceRequest) request;
        CommonBeanUtils.copyProperties(sourceRequest, targetEntity, true);
        try {
            TubeMQSourceDTO dto = TubeMQSourceDTO.getFromRequest(sourceRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SINK_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

    @Override
    public StreamSource getFromEntity(StreamSourceEntity entity) {
        TubeMQSource source = new TubeMQSource();
        if (entity == null) {
            return source;
        }
        TubeMQSourceDTO dto = TubeMQSourceDTO.getFromJson(entity.getExtParams());
        CommonBeanUtils.copyProperties(entity, source, true);
        CommonBeanUtils.copyProperties(dto, source, true);
        List<StreamField> sourceFields = super.getSourceFields(entity.getId());
        source.setFieldList(sourceFields);
        return source;
    }

    @Override
    public Map<String, List<StreamSource>> getSourcesMap(InlongGroupInfo groupInfo,
            List<InlongStreamInfo> streamInfos, List<StreamSource> streamSources) {
        ClusterInfo clusterInfo = clusterService.getOne(groupInfo.getInlongClusterTag(), null, ClusterType.TUBEMQ);
        TubeClusterInfo tubeClusterInfo = (TubeClusterInfo) clusterInfo;
        String masterRpc = tubeClusterInfo.getUrl();

        Map<String, List<StreamSource>> sourceMap = Maps.newHashMap();
        streamInfos.forEach(streamInfo -> {
            TubeMQSource tubeMQSource = new TubeMQSource();
            String streamId = streamInfo.getInlongStreamId();
            tubeMQSource.setSourceName(streamId);
            tubeMQSource.setTopic(streamInfo.getMqResource());
            tubeMQSource.setGroupId(streamId);
            tubeMQSource.setMasterRpc(masterRpc);
            String serializationType = DataTypeEnum.forType(streamInfo.getDataType()).getType();
            tubeMQSource.setSerializationType(serializationType);
            tubeMQSource.setIgnoreParseError(streamInfo.getIgnoreParseError());

            for (StreamSource sourceInfo : streamSources) {
                if (!Objects.equals(streamId, sourceInfo.getInlongStreamId())) {
                    continue;
                }
                tubeMQSource.setSerializationType(sourceInfo.getSerializationType());
            }
            tubeMQSource.setFieldList(streamInfo.getFieldList());
            sourceMap.computeIfAbsent(streamId, key -> Lists.newArrayList()).add(tubeMQSource);
        });

        return sourceMap;
    }

}
