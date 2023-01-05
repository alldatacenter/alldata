/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.biz.dal.pojo;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Application implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer appId;
    private Integer appType;

    private String projectName;

    public boolean hasAppendSearch4Prefix = false;

    private String recept;

    private String manager;

    private Date createTime;

    private Date updateTime;

    private String isDeleted;

    private Boolean isAutoDeploy;

    /**
     * prop:dpt_id
     */
    private Integer dptId;

    /**
     * prop:dpt_name
     */
    private String dptName;


    private String fullBuildCronTime;


    public Integer getAppId() {
        return this.appId;
    }

    public Integer getAppType() {
        return this.appType;
    }

    public AppType parseAppType() {
        return AppType.parse(this.appType);
    }

    public void setAppType(Integer appType) {
        this.appType = appType;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName == null ? null : projectName.trim();
    }

    public String getRecept() {
        return recept;
    }

    public void setRecept(String recept) {
        this.recept = recept == null ? null : recept.trim();
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager == null ? null : manager.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(String isDeleted) {
        this.isDeleted = isDeleted == null ? null : isDeleted.trim();
    }

    public Boolean getIsAutoDeploy() {
        return isAutoDeploy;
    }

    public void setIsAutoDeploy(Boolean isAutoDeploy) {
        this.isAutoDeploy = isAutoDeploy;
    }

    /**
     * get:dpt_id
     */
    public Integer getDptId() {
        return dptId;
    }

    /**
     * set:dpt_id
     */
    public void setDptId(Integer dptId) {
        this.dptId = dptId;
    }

    /**
     * get:dpt_name
     */
    public String getDptName() {
        return dptName;
    }

    /**
     * set:dpt_name
     */
    public void setDptName(String dptName) {
        this.dptName = dptName == null ? null : dptName.trim();
    }

    public Integer getWorkFlowId() {
        return 0;
    }

    public String getFullBuildCronTime() {
        return this.fullBuildCronTime;
    }

    public void setFullBuildCronTime(String fullBuildCronTime) {
        this.fullBuildCronTime = StringUtils.trimToNull(fullBuildCronTime);
    }
}
