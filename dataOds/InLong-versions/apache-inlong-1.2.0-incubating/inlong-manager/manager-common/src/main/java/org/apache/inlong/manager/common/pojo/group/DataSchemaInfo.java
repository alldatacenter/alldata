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

package org.apache.inlong.manager.common.pojo.group;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Data schema info
 */
@Data
@ApiModel("Data schema info")
public class DataSchemaInfo {

    private Integer id;

    @ApiModelProperty(value = "schema name")
    private String name;

    @ApiModelProperty(value = "Agent type, support: file, db_incr, db_full")
    private String agentType;

    @ApiModelProperty(value = "data generate rule, support: day, hour")
    private String dataGenerateRule;

    @ApiModelProperty(value = "sort type, support: 0, 5, 9, 10, 13, 15")
    private Integer sortType;

    @ApiModelProperty(value = "time offset")
    private String timeOffset;

}
