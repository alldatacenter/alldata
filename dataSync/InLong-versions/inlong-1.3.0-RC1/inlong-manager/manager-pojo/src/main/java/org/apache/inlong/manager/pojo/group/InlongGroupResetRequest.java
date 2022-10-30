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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;

/**
 * Inlong group reset request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group reset request")
public class InlongGroupResetRequest {

    @ApiModelProperty(value = "Inlong group id", required = true)
    @Length(min = 4, max = 200)
    @Pattern(regexp = "^(?![0-9]+$)[a-z][a-z0-9_-]{1,200}$",
            message = "inlongGroupId must starts with a lowercase letter "
                    + "and contains only lowercase letters, digits, `-` or `_`")
    private String inlongGroupId;

    @ApiModelProperty(value = "If rerun process when group is in operating, 0: false 1: true")
    @Builder.Default
    private Integer rerunProcess = 0;

    @ApiModelProperty(value = "This params will work when rerunProcess = 0, 0: reset to fail, 1: reset to success")
    @Builder.Default
    private Integer resetFinalStatus = 1;
}
