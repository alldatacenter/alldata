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

package org.apache.inlong.manager.service.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.kafka.KafkaClusterDTO;
import org.apache.inlong.manager.pojo.cluster.kafka.KafkaClusterInfo;
import org.apache.inlong.manager.pojo.cluster.kafka.KafkaClusterRequest;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Kafka cluster operator.
 */
@Service
public class KafkaClusterOperator extends AbstractClusterOperator {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaClusterOperator.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean accept(String clusterType) {
        return getClusterType().equals(clusterType);
    }

    @Override
    public String getClusterType() {
        return ClusterType.KAFKA;
    }

    @Override
    public ClusterInfo getFromEntity(InlongClusterEntity entity) {
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }

        KafkaClusterInfo kafkaClusterInfo = new KafkaClusterInfo();
        CommonBeanUtils.copyProperties(entity, kafkaClusterInfo);
        if (StringUtils.isNotBlank(entity.getExtParams())) {
            KafkaClusterDTO dto = KafkaClusterDTO.getFromJson(entity.getExtParams());
            CommonBeanUtils.copyProperties(dto, kafkaClusterInfo);
        }

        return kafkaClusterInfo;
    }

    @Override
    protected void setTargetEntity(ClusterRequest request, InlongClusterEntity targetEntity) {
        KafkaClusterRequest kafkaRequest = (KafkaClusterRequest) request;
        CommonBeanUtils.copyProperties(kafkaRequest, targetEntity, true);
        try {
            KafkaClusterDTO dto = KafkaClusterDTO.getFromRequest(kafkaRequest);
            targetEntity.setExtParams(objectMapper.writeValueAsString(dto));
            LOGGER.debug("success to set entity for kafka cluster");
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT,
                    String.format("serialize extParams of Kafka Cluster failure: %s", e.getMessage()));
        }
    }

    @Override
    public Boolean testConnection(ClusterRequest request) {
        String bootstrapServers = request.getUrl();
        Preconditions.expectNotBlank(bootstrapServers, ErrorCodeEnum.INVALID_PARAMETER,
                "connection url cannot be empty");
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (Admin ignored = Admin.create(props)) {
            ListTopicsResult topics = ignored.listTopics(new ListTopicsOptions().timeoutMs(30000));
            topics.names().get();
            LOGGER.debug("kafka connection not null - connection success for bootstrapServers={}", topics);
            return true;
        } catch (Exception e) {
            String errMsg = String.format("kafka connection failed for bootstrapServers=%s", bootstrapServers);
            LOGGER.error(errMsg, e);
            throw new BusinessException(errMsg);
        }
    }

}
