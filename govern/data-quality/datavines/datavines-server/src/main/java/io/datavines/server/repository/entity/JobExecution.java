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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.datavines.common.entity.JobExecutionParameter;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.enums.TimeoutStrategy;
import io.datavines.common.enums.JobType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dv_job_execution")
public class JobExecution implements Serializable {

    private static final long serialVersionUID = -1L;

    @TableId(type= IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    private String name;

    @TableField(value = "job_id")
    private Long jobId;

    @TableField(value = "job_type")
    private JobType jobType;

    @TableField(value = "datasource_id")
    private Long dataSourceId;

    @TableField(value = "execute_platform_type")
    private String executePlatformType;

    @TableField(value = "execute_platform_parameter")
    private String executePlatformParameter;

    @TableField(value = "engine_type")
    private String engineType;

    @TableField(value = "engine_parameter")
    private String engineParameter;

    @TableField(value = "error_data_storage_type")
    private String errorDataStorageType;

    @TableField(value = "error_data_storage_parameter")
    private String errorDataStorageParameter;

    @TableField(value = "error_data_file_name")
    private String errorDataFileName;

    /**
     * {@link JobExecutionParameter}
     */
    @TableField(value = "parameter")
    private String parameter;

    @TableField(value = "status")
    private ExecutionStatus status;

    @TableField(value = "retry_times")
    private Integer retryTimes;

    @TableField(value = "retry_interval")
    private Integer retryInterval;

    @TableField(value = "timeout")
    private Integer timeout;

    @TableField(value = "timeout_strategy")
    private TimeoutStrategy timeoutStrategy = TimeoutStrategy.WARN;

    @TableField(value = "tenant_code")
    private String tenantCode;

    @TableField(value = "execute_host")
    private String executeHost;

    @TableField(value = "application_id")
    private String applicationId;

    @TableField(value = "application_tag")
    private String applicationIdTag;

    @TableField(value = "process_id")
    private int processId;

    @TableField(value = "execute_file_path")
    private String executeFilePath;

    @TableField(value = "log_path")
    private String logPath;

    @TableField(value = "env")
    private String env;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(value = "submit_time")
    private LocalDateTime submitTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(value = "schedule_time")
    private LocalDateTime scheduleTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(value = "start_time")
    private LocalDateTime startTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(value = "end_time")
    private LocalDateTime endTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
