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

package com.webank.wedatasphere.streamis.jobmanager.manager.entity;

import java.util.Calendar;
import java.util.Date;


public class StreamTask {
    private Long id;
    private Long jobVersionId;
    private Long jobId;
    private String submitUser;
    private Date startTime;
    private Date lastUpdateTime;
    private String linkisJobId;
    private String linkisJobInfo;
    private String errDesc;
    private String version;
    private Integer status;

    public StreamTask(){
        Calendar calendar = Calendar.getInstance();
        this.lastUpdateTime = calendar.getTime();
        this.startTime = calendar.getTime();
    }

    public StreamTask(Long jobId, Long jobVersionId, String version, String submitUser){
        this();
        this.jobId = jobId;
        this.jobVersionId = jobVersionId;
        this.version = version;
        this.submitUser = submitUser;
    }
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getErrDesc() {
        return errDesc;
    }

    public void setErrDesc(String errDesc) {
        this.errDesc = errDesc;
    }

    public Long getJobVersionId() {
        return jobVersionId;
    }

    public void setJobVersionId(Long jobVersionId) {
        this.jobVersionId = jobVersionId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSubmitUser() {
        return submitUser;
    }

    public void setSubmitUser(String submitUser) {
        this.submitUser = submitUser;
    }

    public String getLinkisJobId() {
        return linkisJobId;
    }

    public void setLinkisJobId(String linkisJobId) {
        this.linkisJobId = linkisJobId;
    }

    public String getLinkisJobInfo() {
        return linkisJobInfo;
    }

    public void setLinkisJobInfo(String linkisJobInfo) {
        this.linkisJobInfo = linkisJobInfo;
    }
}
