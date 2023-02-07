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

package org.apache.inlong.manager.pojo.source.mqtt;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttSourceDTO {

    @ApiModelProperty("ServerURI of the Mqtt server")
    private String serverURI;

    @ApiModelProperty("Username of the Mqtt server")
    private String username;

    @ApiModelProperty("Password of the Mqtt server")
    private String password;

    @ApiModelProperty("Topic of the Mqtt server")
    private String topic;

    @ApiModelProperty("Mqtt qos")
    private int qos = 1;

    @ApiModelProperty("Client Id")
    private String clientId;

    @ApiModelProperty("Mqtt version")
    private String mqttVersion;

    public static MqttSourceDTO getFromRequest(MqttSourceRequest request) {
        return MqttSourceDTO.builder()
                .serverURI(request.getServerURI())
                .username(request.getUsername())
                .password(request.getPassword())
                .topic(request.getTopic())
                .qos(request.getQos())
                .clientId(request.getClientId())
                .mqttVersion(request.getMqttVersion())
                .build();
    }

    public static MqttSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, MqttSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of MqttSource failure: %s", e.getMessage()));
        }
    }

}
