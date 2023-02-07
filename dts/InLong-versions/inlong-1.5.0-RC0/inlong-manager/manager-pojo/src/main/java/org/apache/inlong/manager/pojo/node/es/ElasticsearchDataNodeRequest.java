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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;

/**
 * ES data node request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeDefine(value = DataNodeType.ELASTICSEARCH)
@ApiModel("ES data node request")
public class ElasticsearchDataNodeRequest extends DataNodeRequest {

    @ApiModelProperty("Bulk action, default is 4000")
    private Integer bulkAction = 4000;

    @ApiModelProperty("Bulk size in MB, default is 4000")
    private Integer bulkSizeMb = 10;

    @ApiModelProperty("Flush interval, default is 60")
    private Integer flushInterval = 60;

    @ApiModelProperty("Concurrent requests, default is 60")
    private Integer concurrentRequests = 5;

    @ApiModelProperty("Max connection, default is 10")
    private Integer maxConnect = 10;

    @ApiModelProperty("Max keyword length, default is 32767")
    private Integer keywordMaxLength = 32767;

    @ApiModelProperty("Is use index id, default is false")
    private Boolean isUseIndexId = false;

    @ApiModelProperty("Max threads, default is 2")
    private Integer maxThreads = 2;

    @ApiModelProperty("audit set name")
    private String auditSetName;

    public ElasticsearchDataNodeRequest() {
        setType(DataNodeType.ELASTICSEARCH);
    }
}
