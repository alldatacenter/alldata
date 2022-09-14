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


package com.webank.wedatasphere.streamis.jobmanager.launcher.entity;

/**
 * Job conf value
 */
public class JobConfValue {

    /**
     * Job id
     */
    private Long jobId;

    /**
     * Job name
     */
    private String jobName;

    /**
     * Keyword refer to 'JobConfDefinition'
     */
    private String key;

    /**
     * Actual value
     */
    private String value;

    /**
     * Id refer to 'JobConfDefinition'
     */
    private Long referDefId;

    public JobConfValue(){

    }

    public JobConfValue(String key, String value, Long referDefId){
        this.key = key;
        this.value = value;
        this.referDefId = referDefId;
    }
    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getReferDefId() {
        return referDefId;
    }

    public void setReferDefId(Long referDefId) {
        this.referDefId = referDefId;
    }
}
