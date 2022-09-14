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

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * Created by v_wbyynie on 2021/9/16.
 */
public class MetaJsonInfo {

    private String workspaceName;

    /**
     * 项目名
     */
    @NotBlank(message = "projectName is null")
    private String projectName;

    /**
     * 作业名
     */
    @NotBlank(message = "jobName is null")
    private String jobName;

    /**
     * 目前只支持flink.sql、flink.jar
     */
    @NotBlank(message = "jobType is null")
    private String jobType;

    private String comment;

    /**
     * 应用标签
     */
    private String tags;

    /**
     * 作业描述
     */
    private String description;


    private Map<String,Object> jobContent;

    private String metaInfo;

    public String getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getJobContent() {
        return jobContent;
    }

    public void setJobContent(Map<String, Object> jobContent) {
        this.jobContent = jobContent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
