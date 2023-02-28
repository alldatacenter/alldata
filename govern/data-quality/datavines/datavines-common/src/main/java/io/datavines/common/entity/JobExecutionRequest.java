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
package io.datavines.common.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.datavines.common.enums.TimeoutStrategy;
import lombok.Data;

@Data
public class JobExecutionRequest implements Serializable {

    private long id;

    private long jobExecutionId;

    private String jobExecutionUniqueId;

    private String jobExecutionName;

    private String executePlatformType;

    private String executePlatformParameter;

    private String engineType;

    private String engineParameter;

    private String errorDataStorageType;

    private String errorDataStorageParameter;

    private String validateResultDataStorageType;

    private String validateResultDataStorageParameter;

    private String notificationParameters;

    private String applicationParameter;

    private boolean isEn;

    private String tenantCode;

    private Integer retryTimes;

    private Integer retryInterval;

    private Integer timeout;

    private TimeoutStrategy timeoutStrategy;

    private String executeHost;

    private Integer status;

    private String applicationId;

    private int processId;

    private String executeFilePath;

    private String logPath;

    private String env;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
