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

package org.apache.inlong.manager.common.pojo.group;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.pojo.group.pulsar.InlongPulsarDTO;

/**
 * Extended inlong group info of different MQs
 */
@Data
@ApiModel("Extended inlong group info of different MQs")
@JsonTypeInfo(use = Id.NAME, visible = true, property = "mqType", defaultImpl = InlongGroupMqExtBase.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = InlongPulsarDTO.class, name = "PULSAR"),
        @JsonSubTypes.Type(value = InlongPulsarDTO.class, name = "TDMQ_PULSAR")
})
public class InlongGroupMqExtBase {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "is deleted? 0: deleted, 1: not deleted")
    private Integer isDeleted = 0;

    @ApiModelProperty(value = "MQ type, high throughput: TUBE, high consistency : PULSAR")
    private String mqType;

    @ApiModelProperty(value = "Whether to create mq resource, 0: not create, 1: create; default is 1")
    private Integer enableCreateResource = 1;

}
