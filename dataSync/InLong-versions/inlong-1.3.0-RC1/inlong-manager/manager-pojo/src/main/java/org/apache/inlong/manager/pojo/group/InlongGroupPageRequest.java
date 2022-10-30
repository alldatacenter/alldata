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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.pojo.common.PageRequest;

import java.util.List;

/**
 * Inlong group query request
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group query request")
public class InlongGroupPageRequest extends PageRequest {

    @ApiModelProperty(value = "Keyword, can be group id or name")
    private String keyword;

    @ApiModelProperty(value = "Inlong group id list")
    private List<String> groupIdList;

    @ApiModelProperty(value = "MQ type")
    private String mqType;

    @ApiModelProperty(value = "Group status")
    private Integer status;

    @ApiModelProperty(value = "Group status list")
    private List<Integer> statusList;

    @ApiModelProperty(value = "The inlong cluster tag list")
    private List<String> clusterTagList;

    @ApiModelProperty(value = "Current user", hidden = true)
    private String currentUser;

    @ApiModelProperty(value = "Whether the current user is in the administrator role", hidden = true)
    private Boolean isAdminRole;

    @ApiModelProperty(value = "If list streamSource for group", hidden = true)
    @Builder.Default
    private boolean listSources = false;
}
