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

package org.apache.inlong.manager.common.pojo.source.mongodb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * Response of MongoDB paging list
 */
@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("Response of MongoDB paging list")
@JsonTypeDefine(value = SourceType.SOURCE_MONGODB)
public class MongoDBSourceListResponse extends SourceListResponse {

    @ApiModelProperty("MongoDB primaryKey")
    private String primaryKey;

    @ApiModelProperty("MongoDB hosts")
    private String hosts;

    @ApiModelProperty("MongoDB username")
    private String username;

    @ApiModelProperty("MongoDB password")
    private String password;

    @ApiModelProperty("MongoDB database")
    private String database;

    @ApiModelProperty("MongoDB collection")
    private String collection;

    public MongoDBSourceListResponse() {
        this.setSourceType(SourceType.MONGODB.getType());
    }

}
