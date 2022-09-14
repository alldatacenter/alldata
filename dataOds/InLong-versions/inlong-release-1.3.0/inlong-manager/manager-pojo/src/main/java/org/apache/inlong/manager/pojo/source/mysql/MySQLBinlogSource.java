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

package org.apache.inlong.manager.pojo.source.mysql;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
 * MySQL binlog source info
 */
@Data
@SuperBuilder
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "MySQL binlog source info")
@JsonTypeDefine(value = SourceType.MYSQL_BINLOG)
public class MySQLBinlogSource extends StreamSource {

    @ApiModelProperty("Username of the MySQL server")
    private String user;

    @ApiModelProperty("Password of the MySQL server")
    private String password;

    @ApiModelProperty("Hostname of the MySQL server")
    private String hostname;

    @ApiModelProperty("Port of the MySQL server")
    private Integer port;

    @ApiModelProperty("Id of physical node of MySQL Cluster, 0 if single node")
    @Builder.Default
    private Integer serverId = 0;

    @ApiModelProperty("Whether include schema, default is 'false'")
    private String includeSchema;

    @ApiModelProperty(value = "List of DBs to be collected, seperated by ',', supporting regular expressions")
    private String databaseWhiteList;

    @ApiModelProperty(value = "List of tables to be collected, seperated by ',',supporting regular expressions")
    private String tableWhiteList;

    @ApiModelProperty("Database time zone, Default is UTC")
    private String serverTimezone;

    @ApiModelProperty("The interval for recording an offset")
    private String intervalMs;

    @ApiModelProperty("Snapshot mode, supports: initial, when_needed, never, schema_only, schema_only_recovery")
    private String snapshotMode;

    @ApiModelProperty("The file path to store offset info")
    private String offsetFilename;

    @ApiModelProperty("The file path to store history info")
    private String historyFilename;

    @ApiModelProperty("Whether to monitor the DDL, default is 'false'")
    private String monitoredDdl;

    @ApiModelProperty("Timestamp standard for binlog: SQL, ISO_8601")
    @Builder.Default
    private String timestampFormatStandard = "SQL";

    @ApiModelProperty("Need transfer total database")
    private boolean allMigration;

    @ApiModelProperty("Primary key must be shared by all tables")
    private String primaryKey;

    @ApiModelProperty("Directly read binlog from the specified offset filename")
    private String specificOffsetFile;

    @ApiModelProperty("Directly read binlog from the specified offset position")
    private Integer specificOffsetPos;

    public MySQLBinlogSource() {
        this.setSourceType(SourceType.MYSQL_BINLOG);
    }

    @Override
    public SourceRequest genSourceRequest() {
        return CommonBeanUtils.copyProperties(this, MySQLBinlogSourceRequest::new);
    }
}
