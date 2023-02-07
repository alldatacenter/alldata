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

package org.apache.inlong.manager.pojo.group.tubemq;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.group.BaseInlongGroup;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;

import javax.validation.constraints.NotNull;

/**
 * Inlong group info for TubeMQ
 */
@Data
@NoArgsConstructor
@ApiModel("Inlong group info for TubeMQ")
public class InlongTubeMQDTO extends BaseInlongGroup {

    /**
     * Get the dto instance from the request
     */
    public static InlongTubeMQDTO getFromRequest(InlongGroupRequest request) {
        return CommonBeanUtils.copyProperties(request, InlongTubeMQDTO::new, true);
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static InlongTubeMQDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, InlongTubeMQDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("parse extParams of TubeMQ failure: %s", e.getMessage()));
        }
    }

}
