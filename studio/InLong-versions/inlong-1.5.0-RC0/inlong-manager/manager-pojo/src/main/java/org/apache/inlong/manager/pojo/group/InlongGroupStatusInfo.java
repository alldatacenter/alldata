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
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;
import org.apache.inlong.manager.common.enums.SortStatus;
import org.apache.inlong.manager.pojo.source.StreamSource;

import java.util.List;

/**
 * Inlong group status info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Inlong group status info")
public class InlongGroupStatusInfo {

    @ApiModelProperty(value = "Inlong group id")
    private String inlongGroupId;

    @ApiModelProperty(value = "Inlong group original status")
    private Integer originalStatus;

    @ApiModelProperty(value = "Inlong group simple status")
    private SimpleGroupStatus simpleGroupStatus;

    @ApiModelProperty(value = "Stream sources in the inlong group")
    private List<StreamSource> streamSources;

    @ApiModelProperty(value = "Sort job status of the group")
    private SortStatus sortStatus = SortStatus.UNKNOWN;

}
