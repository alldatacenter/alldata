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

package org.apache.inlong.manager.pojo.node;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.pojo.common.PageRequest;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;

/**
 * Data node paging query conditions
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel("Data node paging query request")
public class DataNodePageRequest extends PageRequest {

    @ApiModelProperty(value = "Data node type, including MYSQL, HIVE, KAFKA, ES, etc.")
    @Length(max = 20, message = "length must be less than or equal to 20")
    private String type;

    @ApiModelProperty(value = "Data node name")
    @Pattern(regexp = "^[A-Za-z0-9_-]{1,128}$", message = "only supports letters, numbers, '-', or '_'")
    private String name;

    @ApiModelProperty(value = "Data node display name, just for display")
    private String displayName;

    @ApiModelProperty(value = "Keywords, name, url, etc.")
    private String keyword;

    @ApiModelProperty(value = "Status")
    private Integer status;

    @ApiModelProperty(value = "Current user", hidden = true)
    private String currentUser;

    @ApiModelProperty(value = "Whether the current user is in the administrator role", hidden = true)
    private Boolean isAdminRole;

}
