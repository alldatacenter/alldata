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

package org.apache.inlong.manager.pojo.workflow;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.pojo.workflow.form.process.BaseProcessForm;

/**
 * Workflow operation request
 */
@Data
@ApiModel("Workflow operation request")
public class WorkflowOperationRequest {

    @ApiModelProperty(value = "Process name", notes = "Specify the process name when initiating the process")
    public ProcessName name;

    @ApiModelProperty(value = "Applicant name", notes = "Nominate applicants when initiating the process")
    public String applicant;

    @ApiModelProperty(value = "Remarks information", notes = "Submit remarks when operating a flow sheet or task sheet")
    public String remark;

    @ApiModelProperty(value = "Form information", notes = "When initiating a process or approving task, "
            + "submit the form information that needs to be submitted")
    public BaseProcessForm form;

}
