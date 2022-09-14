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

package org.apache.inlong.manager.pojo.group;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;

import java.util.List;

/**
 * Inlong group and topic info
 */
@Data
@ApiModel("Inlong group and topic info")
public class InlongGroupTopicInfo {

    @ApiModelProperty(value = "Inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "MQ type, high throughput: TUBEMQ, high consistency: PULSAR")
    private String mqType;

    @ApiModelProperty(value = "MQ resource, TubeMQ topic name, or Pulsar namespace name")
    private String mqResource;

    @ApiModelProperty(value = "Topic list, TubeMQ corresponds to inlong group, there is only 1 topic, "
            + "Pulsar corresponds to inlong stream, there are multiple topics")
    private List<InlongStreamBriefInfo> streamTopics;

    @ApiModelProperty(value = "TubeMQ master URL")
    private String tubeMasterUrl;

    @ApiModelProperty(value = "Pulsar service URL")
    private String pulsarServiceUrl;

    @ApiModelProperty(value = "Pulsar admin URL")
    private String pulsarAdminUrl;

}
