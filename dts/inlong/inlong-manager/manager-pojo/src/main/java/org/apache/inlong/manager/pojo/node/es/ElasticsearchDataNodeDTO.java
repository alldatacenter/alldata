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

package org.apache.inlong.manager.pojo.node.es;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

/**
 * Elasticsearch data node info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Elasticsearch data node info")
public class ElasticsearchDataNodeDTO {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchDataNodeDTO.class);

    @ApiModelProperty("Bulk action, default is 4000")
    private Integer bulkAction;

    @ApiModelProperty("Bulk size in MB, default is 4000")
    private Integer bulkSizeMb;

    @ApiModelProperty("Flush interval, default is 60")
    private Integer flushInterval;

    @ApiModelProperty("Concurrent requests, default is 60")
    private Integer concurrentRequests;

    @ApiModelProperty("Max connection, default is 10")
    private Integer maxConnect;

    @ApiModelProperty("Max keyword length, default is 32767")
    private Integer keywordMaxLength;

    @ApiModelProperty("Is use index id, default is false")
    private Boolean isUseIndexId;

    @ApiModelProperty("Max threads, default is 2")
    private Integer maxThreads;

    @ApiModelProperty("audit set name")
    private String auditSetName;

    /**
     * Get the dto instance from the request
     */
    public static ElasticsearchDataNodeDTO getFromRequest(ElasticsearchDataNodeRequest request) {
        return CommonBeanUtils.copyProperties(request, ElasticsearchDataNodeDTO::new, true);
    }

    /**
     * Get the dto instance from the JSON string.
     */
    public static ElasticsearchDataNodeDTO getFromJson(@NotNull String extParams) {
        try {
            return JsonUtils.parseObject(extParams, ElasticsearchDataNodeDTO.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.GROUP_INFO_INCORRECT,
                    String.format("Failed to parse extParams for Elasticsearch node: %s", e.getMessage()));
        }
    }

}
