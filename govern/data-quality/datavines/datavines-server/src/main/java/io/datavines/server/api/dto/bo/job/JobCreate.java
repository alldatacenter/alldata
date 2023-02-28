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
package io.datavines.server.api.dto.bo.job;

import io.datavines.common.enums.TimeoutStrategy;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@NotNull(message = "JobCreate cannot be null")
public class JobCreate {

    @NotNull(message = "Job type cannot be empty")
    private String type;

    @NotNull(message = "Datasource cannot be empty")
    private long dataSourceId;

    private long dataSourceId2;

    private String executePlatformType = "client";

    private String executePlatformParameter;

    private String engineType = "local";

    private String engineParameter;

    /**
     * Job Parameters 根据 jobType 来进行参数转换
     */
    private String parameter;

    private int timeout = 60000;

    private TimeoutStrategy timeoutStrategy = TimeoutStrategy.WARN;

    private Integer retryTimes = 0;

    private Integer retryInterval = 1000;

    private Long tenantCode;

    private Long env;

    private Long errorDataStorageId;

    /**
     * 1:running now, 0:don't run
     */
    private int runningNow;

}
