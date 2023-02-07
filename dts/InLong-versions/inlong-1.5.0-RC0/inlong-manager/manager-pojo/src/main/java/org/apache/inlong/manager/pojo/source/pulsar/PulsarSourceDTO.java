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

package org.apache.inlong.manager.pojo.source.pulsar;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.JsonUtils;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Pulsar source information data transfer object
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PulsarSourceDTO {

    @ApiModelProperty("Pulsar tenant")
    private String tenant;

    @ApiModelProperty("Pulsar namespace")
    private String namespace;

    @ApiModelProperty("Pulsar topic")
    private String topic;

    @ApiModelProperty("Pulsar adminUrl")
    private String adminUrl;

    @ApiModelProperty("Pulsar serviceUrl")
    private String serviceUrl;

    @ApiModelProperty("Primary key, needed when serialization type is csv, json, avro")
    private String primaryKey;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator;

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    @ApiModelProperty("Configure the Source's startup mode. "
            + "Available options are earliest, latest, external-subscription, and specific-offsets.")
    @Builder.Default
    private String scanStartupMode = "earliest";

    @ApiModelProperty("Properties for Pulsar")
    private Map<String, Object> properties;

    /**
     * Get the dto instance from the request
     */
    public static PulsarSourceDTO getFromRequest(PulsarSourceRequest request) {
        return PulsarSourceDTO.builder()
                .adminUrl(request.getAdminUrl())
                .serviceUrl(request.getServiceUrl())
                .tenant(request.getTenant())
                .namespace(request.getNamespace())
                .topic(request.getTopic())
                .primaryKey(request.getPrimaryKey())
                .scanStartupMode(request.getScanStartupMode())
                .properties(request.getProperties())
                .build();
    }

    public static PulsarSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, PulsarSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of PulsarSource failure: %s", e.getMessage()));
        }
    }

}
