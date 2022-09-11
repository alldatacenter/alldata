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

package org.apache.inlong.manager.common.pojo.sink.hive;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.inlong.manager.common.enums.DataSeparator;
import org.apache.inlong.manager.common.enums.FileFormat;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.util.JsonTypeDefine;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Request of the Hive sink info
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "Request of the Hive sink info")
@JsonTypeDefine(value = SinkType.SINK_HIVE)
public class HiveSinkRequest extends SinkRequest {

    @NotNull(message = "jdbcUrl cannot be null")
    @ApiModelProperty("Hive JDBC URL, such as jdbc:hive2://${ip}:${port}")
    private String jdbcUrl;

    @ApiModelProperty("Username for JDBC URL")
    private String username;

    @ApiModelProperty("User password")
    private String password;

    @NotNull(message = "dbName cannot be null")
    @ApiModelProperty("Target database name")
    private String dbName;

    @NotNull(message = "tableName cannot be null")
    @ApiModelProperty("Target table name")
    private String tableName;

    @NotNull(message = "dataPath cannot be null")
    @ApiModelProperty("Data path, such as: hdfs://ip:port/user/hive/warehouse/test.db")
    private String dataPath;

    @ApiModelProperty("Partition interval, support: 1 H, 1 D, 30 I, 10 I")
    private Integer partitionInterval;

    @ApiModelProperty("Partition field list")
    private List<HivePartitionField> partitionFieldList;

    @ApiModelProperty("Partition creation strategy, partition start, partition close")
    private String partitionCreationStrategy;

    @ApiModelProperty("File format, support: TextFile, ORCFile, RCFile, SequenceFile, Avro, Parquet, etc")
    private String fileFormat = FileFormat.TextFile.name();

    @ApiModelProperty("Data encoding format: UTF-8, GBK")
    private String dataEncoding = StandardCharsets.UTF_8.toString();

    @ApiModelProperty("Data separator, stored as ASCII code")
    private String dataSeparator = DataSeparator.SOH.getSeparator();

    @ApiModelProperty("Version for Hive, such as: 3.2.1")
    private String hiveVersion;

    @ApiModelProperty("Config directory of Hive on HDFS, must include hive-site.xml")
    private String hiveConfDir;

}
