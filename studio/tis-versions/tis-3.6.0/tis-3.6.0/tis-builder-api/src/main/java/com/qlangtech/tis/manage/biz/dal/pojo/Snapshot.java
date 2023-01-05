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

import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Snapshot {

    private Integer snId;

    private Integer appId;

    // private Integer pid;
    private Long resSchemaId;

    private Long resSolrId;

    private Long resJarId;

    private Long resCorePropId;

    private Long resDsId;

    private Long resApplicationId;

    private Date createTime;

    private Long createUserId;

    private String createUserName;

    private Date updateTime;

    private Integer preSnId;

    private String memo;

    private Integer bizId;

    public Integer getBizId() {
        return bizId;
    }

    public void setBizId(Integer bizId) {
        this.bizId = bizId;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo == null ? null : memo.trim();
    }

    public Integer getSnId() {
        return snId;
    }

    public void setSnId(Integer snId) {
        this.snId = snId;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    // public Integer getPid() {
    // return pid;
    // }
    //
    // public void setPid(Integer pid) {
    // this.pid = pid;
    // }
    public Long getResSchemaId() {
        return resSchemaId;
    }

    public void setResSchemaId(Long resSchemaId) {
        this.resSchemaId = resSchemaId;
    }

    public Long getResSolrId() {
        return resSolrId;
    }

    public void setResSolrId(Long resSolrId) {
        this.resSolrId = resSolrId;
    }

    public Long getResJarId() {
        return resJarId;
    }

    public void setResJarId(Long resJarId) {
        this.resJarId = resJarId;
    }

    public Long getResCorePropId() {
        return resCorePropId;
    }

    public void setResCorePropId(Long resCorePropId) {
        this.resCorePropId = resCorePropId;
    }

    public Long getResDsId() {
        return resDsId;
    }

    public void setResDsId(Long resDsId) {
        this.resDsId = resDsId;
    }

    public Long getResApplicationId() {
        return resApplicationId;
    }

    public void setResApplicationId(Long resApplicationId) {
        this.resApplicationId = resApplicationId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName == null ? null : createUserName.trim();
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getPreSnId() {
        return preSnId;
    }

    public void setPreSnId(Integer preSnId) {
        this.preSnId = preSnId;
    }
}
