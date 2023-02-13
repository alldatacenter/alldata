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

package org.apache.inlong.manager.pojo.stream;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Inlong stream extension information
 */
@Data
@ApiModel("Inlong stream extension information")
public class InlongStreamExtInfo {

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "inlong stream id", required = true)
    private String inlongStreamId;

    @ApiModelProperty(value = "property name")
    private String keyName;

    @ApiModelProperty(value = "property value")
    private String keyValue;
}
