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

package org.apache.inlong.manager.pojo.tenant;

import org.apache.inlong.manager.common.validation.UpdateByIdValidation;
import org.apache.inlong.manager.common.validation.UpdateByKeyValidation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Tenant request")
public class InlongTenantRequest {

    @ApiModelProperty(value = "Primary key")
    private Integer id;

    @ApiModelProperty(value = "Tenant name")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,256}$", message = "only supports letters, numbers, '-', or '_'")
    @NotBlank
    private String name;

    @ApiModelProperty(value = "Description of the tenant")
    @Length(max = 256, message = "length must be less than or equal to 256")
    private String description;

    @ApiModelProperty(value = "Version number")
    @NotNull(groups = {UpdateByIdValidation.class, UpdateByKeyValidation.class}, message = "version cannot be null")
    private Integer version;
}
