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
public class GroupInfo {

    private Integer gid;

    private Integer appId;

    private Short groupCount;

    private Short runEnvironment;

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

    public Short getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(Short groupCount) {
        this.groupCount = groupCount;
    }

    /**
     * get:閽堝搴旂敤绫诲瀷锛�锛氭棩甯�1: daily 2锛氱嚎涓�
     */
    public Short getRunEnvironment() {
        return runEnvironment;
    }

    /**
     * set:閽堝搴旂敤绫诲瀷锛�锛氭棩甯�1: daily 2锛氱嚎涓�
     */
    public void setRunEnvironment(Short runEnvironment) {
        this.runEnvironment = runEnvironment;
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
}
