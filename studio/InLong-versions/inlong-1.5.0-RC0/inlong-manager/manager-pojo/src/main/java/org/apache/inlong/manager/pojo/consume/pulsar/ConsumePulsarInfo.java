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

package org.apache.inlong.manager.pojo.consume.pulsar;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;

/**
 * Inlong consume info of Pulsar
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = MQType.PULSAR)
@ApiModel("Inlong consume info of Pulsar")
public class ConsumePulsarInfo extends InlongConsumeInfo {

    @ApiModelProperty("Whether to configure the dead letter queue, 0: not configure, 1: configure")
    private Integer isDlq;

    @ApiModelProperty("The name of the dead letter queue Topic")
    private String deadLetterTopic;

    @ApiModelProperty("Whether to configure the retry letter queue, 0: not configure, 1: configure")
    private Integer isRlq;

    @ApiModelProperty("The name of the retry letter queue topic")
    private String retryLetterTopic;

    @Override
    public ConsumePulsarRequest genRequest() {
        return CommonBeanUtils.copyProperties(this, ConsumePulsarRequest::new);
    }

}
