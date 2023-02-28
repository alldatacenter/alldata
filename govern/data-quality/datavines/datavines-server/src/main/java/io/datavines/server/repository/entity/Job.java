/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.repository.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.datavines.common.enums.TimeoutStrategy;
import io.datavines.common.enums.JobType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dv_job")
public class Job implements Serializable {

    private static final long serialVersionUID = -1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    private String name;

    @TableField(value = "type")
    private JobType type;

    @TableField(value = "datasource_id")
    private Long dataSourceId;

    @TableField(value = "datasource_id_2")
    private Long dataSourceId2;

    @TableField(value = "schema_name")
    private String schemaName;

    @TableField(value = "table_name")
    private String tableName;

    @TableField(value = "column_name")
    private String columnName;

    @TableField(value = "selected_column")
    private String selectedColumn;

    @TableField(value = "metric_type")
    private String metricType;

    @TableField(value = "execute_platform_type")
    private String executePlatformType;

    @TableField(value = "execute_platform_parameter",updateStrategy = FieldStrategy.IGNORED)
    private String executePlatformParameter;

    @TableField(value = "engine_type")
    private String engineType;

    @TableField(value = "engine_parameter",updateStrategy = FieldStrategy.IGNORED)
    private String engineParameter;

    @TableField(value = "error_data_storage_id",updateStrategy = FieldStrategy.IGNORED)
    private Long errorDataStorageId;

    @TableField(value = "parameter")
    private String parameter;

    @TableField(value = "retry_times")
    private Integer retryTimes;

    @TableField(value = "retry_interval")
    private Integer retryInterval;

    @TableField(value = "timeout")
    private Integer timeout;

    @TableField(value = "timeout_strategy")
    private TimeoutStrategy timeoutStrategy;

    @TableField(value = "tenant_code",updateStrategy = FieldStrategy.IGNORED)
    private Long tenantCode;

    @TableField(value = "env",updateStrategy = FieldStrategy.IGNORED)
    private Long env;

    @TableField(value = "create_by")
    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private Long updateBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
