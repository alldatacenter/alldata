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
import lombok.Data;

/**
 * Workflow brief info
 */
@Data
@ApiModel("Workflow brief info")
public class WorkflowBriefDTO {

    @ApiModelProperty(value = "Process name")
    private String name;

    @ApiModelProperty(value = "Process display name")
    private String displayName;

    @ApiModelProperty(value = "Process type")
    private String type;

    @ApiModelProperty(value = "approval process-start node")
    private ElementDTO startEvent;

}
