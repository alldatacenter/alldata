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

package org.apache.inlong.manager.service.sink.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkDTO;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkRequest;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.service.sink.AbstractSinkOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MySQL sink operator
 */
@Service
public class MySQLSinkOperator extends AbstractSinkOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLSinkOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.MYSQL.equals(sinkType);
    }

    @Override
    protected String getSinkType() {
        return SinkType.MYSQL;
    }

    @Override
    protected void setTargetEntity(SinkRequest request, StreamSinkEntity targetEntity) {
        Preconditions.checkTrue(this.getSinkType().equals(request.getSinkType()),
                ErrorCodeEnum.SINK_TYPE_NOT_SUPPORT.getMessage() + ": " + getSinkType());
        MySQLSinkRequest sinkRequest = (MySQLSinkRequest) request;
        try {
            MySQLSinkDTO dto = MySQLSinkDTO.getFromRequest(sinkRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            LOGGER.error("parsing json string to sink info failed", e);
            throw new BusinessException(ErrorCodeEnum.SINK_SAVE_FAILED.getMessage());
        }
    }

    @Override
    public StreamSink getFromEntity(StreamSinkEntity entity) {
        MySQLSink sink = new MySQLSink();
        if (entity == null) {
            return sink;
        }

        MySQLSinkDTO dto = MySQLSinkDTO.getFromJson(entity.getExtParams());
        if (StringUtils.isBlank(dto.getJdbcUrl())) {
            String dataNodeName = entity.getDataNodeName();
            Preconditions.checkNotEmpty(dataNodeName, "mysql jdbc url not specified and data node is empty");
            DataNodeInfo dataNodeInfo = dataNodeHelper.getDataNodeInfo(dataNodeName, entity.getSinkType());
            CommonBeanUtils.copyProperties(dataNodeInfo, dto, true);
            dto.setJdbcUrl(dataNodeInfo.getUrl());
            dto.setPassword(dataNodeInfo.getToken());
        }
        CommonBeanUtils.copyProperties(entity, sink, true);
        CommonBeanUtils.copyProperties(dto, sink, true);
        List<SinkField> sinkFields = super.getSinkFields(entity.getId());
        sink.setSinkFieldList(sinkFields);
        return sink;
    }

}
