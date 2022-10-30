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
 *
 */

package org.apache.inlong.manager.pojo.source.tubemq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.TreeSet;

/**
 * TubeMQ source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TubeMQSourceDTO {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ApiModelProperty("Master RPC of the TubeMQ,127.0.0.1:8715")
    private String masterRpc;

    @ApiModelProperty("Topic of the TubeMQ")
    private String topic;

    @ApiModelProperty("Format of the TubeMQ")
    private String format;

    @ApiModelProperty("Group of the TubeMQ")
    private String groupId;

    @ApiModelProperty("Session key of the TubeMQ")
    private String sessionKey;

    /**
     * The tubemq consumers use this tid set to filter records reading from server.
     */
    @ApiModelProperty("Tid of the TubeMQ")
    private TreeSet<String> tid;

    @ApiModelProperty("Properties for TubeMQ")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static TubeMQSourceDTO getFromRequest(TubeMQSourceRequest request) {
        return TubeMQSourceDTO.builder()
                .masterRpc(request.getMasterRpc())
                .topic(request.getTopic())
                .format(request.getSerializationType())
                .groupId(request.getGroupId())
                .sessionKey(request.getSessionKey())
                .tid(request.getTid())
                .properties(request.getProperties())
                .build();
    }

    /**
     * Get the dto instance from the JSON string
     */
    public static TubeMQSourceDTO getFromJson(@NotNull String extParams) {
        try {
            OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return OBJECT_MAPPER.readValue(extParams, TubeMQSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }
}
