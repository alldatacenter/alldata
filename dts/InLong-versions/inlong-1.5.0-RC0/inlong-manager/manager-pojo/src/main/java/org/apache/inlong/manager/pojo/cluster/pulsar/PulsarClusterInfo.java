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

package org.apache.inlong.manager.pojo.cluster.pulsar;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;

/**
 * Inlong cluster info for Pulsar
 */
@Data
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = ClusterType.PULSAR)
@ApiModel("Inlong cluster info for Pulsar")
public class PulsarClusterInfo extends ClusterInfo {

    @ApiModelProperty(value = "Pulsar admin URL, such as: http://127.0.0.1:8080", notes = "Pulsar service URL is the 'url' field of the cluster")
    private String adminUrl;

    @ApiModelProperty(value = "Pulsar tenant, default is 'public'")
    private String tenant;

    public PulsarClusterInfo() {
        this.setType(ClusterType.PULSAR);
    }

    @Override
    public PulsarClusterRequest genRequest() {
        return CommonBeanUtils.copyProperties(this, PulsarClusterRequest::new);
    }

}
