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

package org.apache.inlong.manager.common.pojo.source.postgres;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.SourceListResponse;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

import java.util.List;

/**
 * Response info of postgres source list
 */
@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("Response of postgres source paging list")
@JsonTypeDefine(value = SourceType.SOURCE_POSTGRES)
public class PostgresSourceListResponse extends SourceListResponse {

    @ApiModelProperty("Primary key")
    private String primaryKey;

    @ApiModelProperty("Username of the DB server")
    private String username;

    @ApiModelProperty("Password of the DB server")
    private String password;

    @ApiModelProperty("Hostname of the DB server")
    private String hostname;

    @ApiModelProperty("Exposed port of the DB server")
    private int port;

    @ApiModelProperty("Exposed database of the DB")
    private String database;

    @ApiModelProperty("Schema info")
    private String schema;

    @ApiModelProperty("Decoding plugin name")
    private String decodingPluginName;

    @ApiModelProperty("List of tables name")
    private List<String> tableNameList;

    public PostgresSourceListResponse() {
        this.setSourceType(SourceType.POSTGRES.getType());
    }
}
