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

import java.io.Serializable;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UsrDptRelation implements Serializable {

    private String usrId;

    private Integer dptId;

    private String dptName;

    private Date createTime;

    private Date updateTime;

    private String userName;

    private String password;

    private String realName;

    /**
     * prop:r_id
     */
    private Integer rId;

    /**
     * prop:r_id
     */
    private String roleName;

    private static final long serialVersionUID = 1L;

    private Boolean extraDptRelation;

    public Boolean isExtraDptRelation() {
        return extraDptRelation;
    }

    public void setExtraDptRelation(Boolean extraDptRelation) {
        this.extraDptRelation = extraDptRelation;
    }

    public String getUsrId() {
        return usrId;
    }

    public void setUsrId(String usrId) {
        this.usrId = usrId == null ? null : usrId.trim();
    }

    public Integer getDptId() {
        return dptId;
    }

    public void setDptId(Integer dptId) {
        this.dptId = dptId;
    }

    public String getDptName() {
        return dptName;
    }

    public void setDptName(String dptName) {
        this.dptName = dptName == null ? null : dptName.trim();
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName == null ? null : userName.trim();
    }

    /**
     * get:r_id
     */
    public Integer getrId() {
        return rId;
    }

    public Integer getRoleId() {
        return this.rId;
    }

    /**
     * set:r_id
     */
    public void setrId(Integer rId) {
        this.rId = rId;
    }

    /**
     * get:r_id
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * set:r_id
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName == null ? null : roleName.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return this.realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
