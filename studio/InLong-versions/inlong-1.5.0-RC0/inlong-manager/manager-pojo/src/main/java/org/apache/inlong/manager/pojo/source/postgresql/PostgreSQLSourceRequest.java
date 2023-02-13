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

package org.apache.inlong.manager.pojo.source.postgresql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;

import java.util.List;

/**
 * PostgreSQL source request
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "PostgreSQL source request")
@JsonTypeDefine(value = SourceType.POSTGRESQL)
public class PostgreSQLSourceRequest extends SourceRequest {

    @ApiModelProperty("Username of the PostgreSQL server")
    private String username;

    @ApiModelProperty("Password of the PostgreSQL server")
    private String password;

    @ApiModelProperty("Hostname of the PostgreSQL server")
    private String hostname;

    @ApiModelProperty("Port of the PostgreSQL server")
    private Integer port = 5432;

    @ApiModelProperty("Database name")
    private String database;

    @ApiModelProperty("Schema name")
    private String schema;

    @ApiModelProperty("Decoding plugin name")
    private String decodingPluginName;

    @ApiModelProperty("List of table name")
    private List<String> tableNameList;

    @ApiModelProperty("Server time zone")
    private String serverTimeZone;

    @ApiModelProperty("Scan startup mode,  either initial or never")
    private String scanStartupMode;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    public PostgreSQLSourceRequest() {
        this.setSourceType(SourceType.POSTGRESQL);
    }

}
