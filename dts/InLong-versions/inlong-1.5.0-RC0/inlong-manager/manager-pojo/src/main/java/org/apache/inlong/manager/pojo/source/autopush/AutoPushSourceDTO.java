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

package org.apache.inlong.manager.pojo.source.autopush;

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
 * SDK source info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoPushSourceDTO {

    @ApiModelProperty(value = "DataProxy group name, used when the user enables local configuration")
    private String dataProxyGroup;

    @ApiModelProperty(value = "Data encoding format: UTF-8, GBK")
    private String dataEncoding;

    @ApiModelProperty(value = "Data separator")
    private String dataSeparator;

    @ApiModelProperty(value = "Data field escape symbol")
    private String dataEscapeChar;

    public static AutoPushSourceDTO getFromRequest(AutoPushSourceRequest request) {
        return AutoPushSourceDTO.builder()
                .dataProxyGroup(request.getDataProxyGroup())
                .build();
    }

    public static AutoPushSourceDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, AutoPushSourceDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.SOURCE_INFO_INCORRECT,
                    String.format("parse extParams of AutoPushSource failure: %s", e.getMessage()));
        }
    }
}
