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

package org.apache.inlong.manager.common.pojo.stream;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Inlong stream config log request.
 */
@Data
@ApiModel("Inlong stream config log")
public class InlongStreamConfigLogRequest {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong stream id")
    private String inlongStreamId;

    @ApiModelProperty(value = "ip")
    private String ip;

    @ApiModelProperty(value = "client version")
    private String version;

    @ApiModelProperty(value = "component name")
    private String componentName;

    @ApiModelProperty(value = "config name")
    private String configName;

    @ApiModelProperty(value = "log type, 0 normal, 1 error")
    private Integer logType;

    @ApiModelProperty(value = "report time")
    private long reportTime;

    @ApiModelProperty(value = "long info")
    private String logInfo;

}