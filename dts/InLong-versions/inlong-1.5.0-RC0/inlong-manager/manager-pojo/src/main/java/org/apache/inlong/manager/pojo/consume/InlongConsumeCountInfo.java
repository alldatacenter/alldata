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

package org.apache.inlong.manager.pojo.consume;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Count info of inlong consume status.
 */
@Data
@ApiModel("Count info of inlong consume status")
public class InlongConsumeCountInfo {

    @ApiModelProperty(value = "Total consume number")
    private Integer totalCount = 0;

    @ApiModelProperty(value = "Total number of to be allocated (the number of configuring consumes)")
    private Integer waitAssignCount = 0;

    @ApiModelProperty(value = "Total number of to be approved")
    private Integer waitApproveCount = 0;

    @ApiModelProperty(value = "Total number of rejections")
    private Integer rejectCount = 0;

}
