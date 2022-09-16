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

package org.apache.inlong.manager.pojo.source.sqlserver;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonTypeDefine;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;

/**
 * SQLServer source info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "SQLServer source info")
@JsonTypeDefine(value = SourceType.SQLSERVER)
public class SQLServerSource extends StreamSource {

    @ApiModelProperty("Username of the SQLServer server")
    private String username;

    @ApiModelProperty("Password of the SQLServer server")
    private String password;

    @ApiModelProperty("Hostname of the SQLServer server")
    private String hostname;

    @ApiModelProperty("Port of the SQLServer server")
    private Integer port;

    @ApiModelProperty("Database name")
    private String database;

    @ApiModelProperty("Schema name")
    private String schemaName;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("Database time zone, default is UTC")
    private String serverTimezone;

    @ApiModelProperty("Whether to migrate all databases")
    private boolean allMigration;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    public SQLServerSource() {
        this.setSourceType(SourceType.SQLSERVER);
    }

    @Override
    public SourceRequest genSourceRequest() {
        return CommonBeanUtils.copyProperties(this, SQLServerSourceRequest::new);
    }

}
