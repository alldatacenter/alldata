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
package io.datavines.http.client.request;

import java.util.Map;



public class SubmitTaskRequest {

    private String name;

    private String executePlatformType = "client";

    private Map<String,Object> executePlatformParameter;

    private String engineType = "local";

    private Map<String,Object> engineParameter;

    private TaskParameter parameter;

    private Integer retryTimes = 0;

    private Integer retryInterval = 1000;

    private Integer timeout = 3600;

    private TimeoutStrategy timeoutStrategy = TimeoutStrategy.WARN;

    private String tenantCode;

    private String env;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExecutePlatformType() {
        return executePlatformType;
    }

    public void setExecutePlatformType(String executePlatformType) {
        this.executePlatformType = executePlatformType;
    }

    public Map<String,Object> getExecutePlatformParameter() {
        return executePlatformParameter;
    }

    public void setExecutePlatformParameter(Map<String,Object> executePlatformParameter) {
        this.executePlatformParameter = executePlatformParameter;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public Map<String,Object> getEngineParameter() {
        return engineParameter;
    }

    public void setEngineParameter(Map<String,Object> engineParameter) {
        this.engineParameter = engineParameter;
    }

    public TaskParameter getParameter() {
        return parameter;
    }

    public void setParameter(TaskParameter parameter) {
        this.parameter = parameter;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public TimeoutStrategy getTimeoutStrategy() {
        return timeoutStrategy;
    }

    public void setTimeoutStrategy(TimeoutStrategy timeoutStrategy) {
        this.timeoutStrategy = timeoutStrategy;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
