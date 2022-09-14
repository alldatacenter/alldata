/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo;

import java.util.List;

/**
 * Config value set
 */
public class JobConfValueSet {

    /**
     * Job id
     */
    private Long jobId;

    /**
     * Resource config
     */
    private List<JobConfValueVo> resourceConfig;

    /**
     * Produce config
     */
    private List<JobConfValueVo> produceConfig;

    /**
     * Parameter config
     */
    private List<JobConfValueVo> parameterConfig;

    /**
     * Alarm config
     */
    private List<JobConfValueVo> alarmConfig;

    /**
     * Permission config
     */
    private List<JobConfValueVo> permissionConfig;


    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public List<JobConfValueVo> getResourceConfig() {
        return resourceConfig;
    }

    public void setResourceConfig(List<JobConfValueVo> resourceConfig) {
        this.resourceConfig = resourceConfig;
    }

    public List<JobConfValueVo> getProduceConfig() {
        return produceConfig;
    }

    public void setProduceConfig(List<JobConfValueVo> produceConfig) {
        this.produceConfig = produceConfig;
    }

    public List<JobConfValueVo> getParameterConfig() {
        return parameterConfig;
    }

    public void setParameterConfig(List<JobConfValueVo> parameterConfig) {
        this.parameterConfig = parameterConfig;
    }

    public List<JobConfValueVo> getAlarmConfig() {
        return alarmConfig;
    }

    public void setAlarmConfig(List<JobConfValueVo> alarmConfig) {
        this.alarmConfig = alarmConfig;
    }

    public List<JobConfValueVo> getPermissionConfig() {
        return permissionConfig;
    }

    public void setPermissionConfig(List<JobConfValueVo> permissionConfig) {
        this.permissionConfig = permissionConfig;
    }
}
