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

package org.apache.inlong.manager.common.pojo.workflow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Process count response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Process count response")
public class ProcessCountResponse {

    @ApiModelProperty(value = "Count of applications")
    private Integer totalApplyCount;

    @ApiModelProperty(value = "Count of dismissed data")
    private Integer totalRejectCount;

    @ApiModelProperty(value = "Count of approved")
    private Integer totalApproveCount;

    @ApiModelProperty(value = "Count of awaiting approval")
    private Integer totalProcessingCount;

    @ApiModelProperty(value = "Count of cancelled")
    private Integer totalCancelCount;

}
