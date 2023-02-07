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

package org.apache.inlong.manager.pojo.group.pulsar;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = MQType.PULSAR)
@ApiModel("Inlong pulsar group topic info")
public class InlongPulsarTopicInfo extends InlongGroupTopicInfo {

    @ApiModelProperty(value = "Pulsar tenant")
    private String tenant;

    @ApiModelProperty(value = "Pulsar namespace")
    private String namespace;

    @ApiModelProperty(value = "Pulsar topics")
    private List<String> topics;

    public InlongPulsarTopicInfo() {
        this.setMqType(MQType.PULSAR);
    }

}
