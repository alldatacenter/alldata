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

package org.apache.inlong.manager.pojo.sink.sqlserver;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

/**
 * SQLServer sink request.
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "SQLServer sink request")
@JsonTypeDefine(value = SinkType.SQLSERVER)
public class SQLServerSinkRequest extends SinkRequest {

    @ApiModelProperty("Username of the SQLServer")
    private String username;

    @ApiModelProperty("Password of the SQLServer")
    private String password;

    @ApiModelProperty("SQLServer meta db URL, etc jdbc:sqlserver://host:port;databaseName=database")
    private String jdbcUrl;

    @ApiModelProperty("Schema name of the SQLServer")
    private String schemaName;

    @ApiModelProperty("Table name of the SQLServer")
    private String tableName;

    @ApiModelProperty("Database time zone, Default is UTC")
    private String serverTimezone;

    @ApiModelProperty("Whether to migrate all databases")
    private boolean allMigration;

    @ApiModelProperty(value = "Primary key must be shared by all tables")
    private String primaryKey;

}
