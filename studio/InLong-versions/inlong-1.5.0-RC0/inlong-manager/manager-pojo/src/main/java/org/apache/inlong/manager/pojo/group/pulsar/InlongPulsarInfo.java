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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;

/**
 * Inlong group info for Pulsar
 */
@Data
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = MQType.PULSAR)
@ApiModel("Inlong group info for Pulsar")
public class InlongPulsarInfo extends InlongGroupInfo {

    @ApiModelProperty(value = "Pulsar tenant")
    private String tenant;

    @ApiModelProperty(value = "Queue model, parallel: multiple partitions, high throughput, out-of-order messages;"
            + "serial: single partition, low throughput, and orderly messages")
    private String queueModule = "PARALLEL";

    @ApiModelProperty("The number of partitions of Topic, 1-20")
    private int partitionNum = 3;

    @ApiModelProperty(value = "Ledger's number of writable nodes")
    private Integer ensemble = 3;

    @ApiModelProperty(value = "Ledger's number of copies")
    private Integer writeQuorum = 3;

    @ApiModelProperty(value = "Number of responses requested")
    private Integer ackQuorum = 2;

    @ApiModelProperty(value = "Message time-to-live duration")
    private Integer ttl = 24;

    @ApiModelProperty(value = "The unit of message's time-to-live duration")
    private String ttlUnit = "hours";

    @ApiModelProperty(value = "Message storage time")
    private Integer retentionTime = 72;

    @ApiModelProperty(value = "The unit of the message storage time")
    private String retentionTimeUnit = "hours";

    @ApiModelProperty(value = "Message size")
    private Integer retentionSize = -1;

    @ApiModelProperty(value = "The unit of message size")
    private String retentionSizeUnit = "MB";

    public InlongPulsarInfo() {
        this.setMqType(MQType.PULSAR);
    }

    @Override
    public InlongPulsarRequest genRequest() {
        return CommonBeanUtils.copyProperties(this, InlongPulsarRequest::new);
    }

}
