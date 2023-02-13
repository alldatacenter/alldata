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
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;

/**
 * Inlong group info for TubeMQ
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel("Inlong group info for TubeMQ")
@JsonTypeDefine(value = MQType.TUBEMQ)
public class InlongTubeMQInfo extends InlongGroupInfo {

    @ApiModelProperty("TubeMQ manager URL")
    private String managerUrl;

    @ApiModelProperty("TubeMQ master URL")
    private String masterUrl;

    @ApiModelProperty("TubeMQ cluster ID")
    private int clusterId = 1;

    public InlongTubeMQInfo() {
        this.setMqType(MQType.TUBEMQ);
    }

    @Override
    public InlongTubeMQRequest genRequest() {
        return CommonBeanUtils.copyProperties(this, InlongTubeMQRequest::new);
    }

}
