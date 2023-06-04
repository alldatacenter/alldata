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

package org.apache.inlong.manager.pojo.cluster.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;

/**
 * Kafka cluster info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Kafka cluster info")
public class KafkaClusterDTO {

    /**
     * Repeated save to ext_params field, it is convenient for DataProxy to obtain.
     */
    @JsonProperty("bootstrap.servers")
    @ApiModelProperty(value = "Kafka bootstrap servers' URL, is the 'url' field of the cluster")
    private String bootstrapServers;

    /**
     * Saved to ext_params field, it is convenient for DataProxy to obtain.
     */
    @Builder.Default
    private String messageQueueHandler = "org.apache.inlong.dataproxy.sink.mq.kafka.KafkaHandler";

    /**
     * Get the dto instance from the request
     */
    public static KafkaClusterDTO getFromRequest(KafkaClusterRequest request) {
        return KafkaClusterDTO.builder()
                .bootstrapServers(request.getUrl())
                .build();
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static KafkaClusterDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, KafkaClusterDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CLUSTER_INFO_INCORRECT,
                    String.format("parse extParams of Kafka Cluster failure: %s", e.getMessage()));
        }
    }

}
