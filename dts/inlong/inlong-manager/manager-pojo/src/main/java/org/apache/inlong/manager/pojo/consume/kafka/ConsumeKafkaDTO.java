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

package org.apache.inlong.manager.pojo.consume.kafka;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.consume.BaseInlongConsume;

import javax.validation.constraints.NotNull;

/**
 * Inlong group info of Kafka
 */
@Data
@Builder
@AllArgsConstructor
@ApiModel("Inlong group info of Kafka")
public class ConsumeKafkaDTO extends BaseInlongConsume {

    /**
     * Get the dto instance from the request
     */
    public static ConsumeKafkaDTO getFromRequest(ConsumeKafkaRequest request) {
        return CommonBeanUtils.copyProperties(request, ConsumeKafkaDTO::new, true);
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static ConsumeKafkaDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, ConsumeKafkaDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.CONSUMER_INFO_INCORRECT.getMessage() + ": " + e.getMessage());
        }
    }

}
