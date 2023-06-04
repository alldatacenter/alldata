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

package org.apache.inlong.manager.pojo.source;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.pojo.common.PageRequest;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Paging query request for Source
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel("Paging query request for Source")
public class SourcePageRequest extends PageRequest {

    @NotBlank(message = "inlongGroupId cannot be blank")
    @ApiModelProperty(value = "Inlong group id", required = true)
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong stream id")
    private String inlongStreamId;

    @ApiModelProperty(value = "Source type, such as FILE")
    private String sourceType;

    @ApiModelProperty(value = "Data node name")
    private String dataNodeName;

    @ApiModelProperty(value = "Inlong cluster name")
    private String inlongClusterName;

    @ApiModelProperty(value = "Keyword, can be group id, stream id or source name")
    private String keyword;

    @ApiModelProperty(value = "Status")
    private Integer status;

    @ApiModelProperty(value = "Source status list")
    private List<Integer> statusList;

}
