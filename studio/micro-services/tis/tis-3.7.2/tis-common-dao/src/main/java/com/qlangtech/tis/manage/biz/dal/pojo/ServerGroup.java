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
import com.qlangtech.tis.manage.common.BasicOperationDomainLogger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public final class ServerGroup extends BasicOperationDomainLogger {

    private Integer gid;

    private Integer deleteFlag;

    private String appName;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    private Integer appId;

    private Short runtEnvironment;

    private Short groupIndex;

    private Integer publishSnapshotId;

    private Date createTime;

    private Date updateTime;

    public Integer getGid() {
        return gid;
    }

    public void setGid(Integer gid) {
        this.gid = gid;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Short getRuntEnvironment() {
        return runtEnvironment;
    }

    public void setRuntEnvironment(Short runtEnvironment) {
        this.runtEnvironment = runtEnvironment;
    }

    public Short getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(Short groupIndex) {
        this.groupIndex = groupIndex;
    }

    public Integer getPublishSnapshotId() {
        return publishSnapshotId;
    }

    public void setPublishSnapshotId(Integer publishSnapshotId) {
        this.publishSnapshotId = publishSnapshotId;
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

    private String yuntiPath;

    public String getYuntiPath() {
        return yuntiPath;
    }

    public void setYuntiPath(String yuntiPath) {
        this.yuntiPath = yuntiPath;
    }
}
