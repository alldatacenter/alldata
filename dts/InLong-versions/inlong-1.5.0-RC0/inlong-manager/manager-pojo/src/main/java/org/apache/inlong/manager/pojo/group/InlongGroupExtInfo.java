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

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inlong group extension information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group extension information")
public class InlongGroupExtInfo {

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "inlong group id", required = true)
    @Length(min = 4, max = 100, message = "length must be between 4 and 100")
    @Pattern(regexp = "^[a-z0-9_-]{4,100}$", message = "only supports lowercase letters, numbers, '-', or '_'")
    private String inlongGroupId;

    @ApiModelProperty(value = "property name")
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String keyName;

    @ApiModelProperty(value = "property value")
    @Length(min = 1, max = 163840, message = "length must be between 1 and 163840")
    private String keyValue;

}
