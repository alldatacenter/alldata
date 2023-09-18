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
public class FuncRoleRelation implements Serializable {

    /**
     * prop:id
     */
    private Integer id;

    /**
     * prop:r_id
     */
    private Integer rId;

    /**
     * prop:role_name
     */
    private String roleName;

    /**
     * prop:func_id
     */
    private Integer funcId;

    /**
     * prop:func_key
     */
    private String funcKey;

    /**
     * prop:gmt_create
     */
    private Date gmtCreate;

    /**
     * prop:gmt_modified
     */
    private Date gmtModified;

    /**
     * prop:func_name
     */
    private String funcName;

    private static final long serialVersionUID = 1L;

    /**
     * get:id
     */
    public Integer getId() {
        return id;
    }

    /**
     * set:id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * get:r_id
     */
    public Integer getrId() {
        return rId;
    }

    /**
     * set:r_id
     */
    public void setrId(Integer rId) {
        this.rId = rId;
    }

    /**
     * get:role_name
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * set:role_name
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName == null ? null : roleName.trim();
    }

    /**
     * get:func_id
     */
    public Integer getFuncId() {
        return funcId;
    }

    /**
     * set:func_id
     */
    public void setFuncId(Integer funcId) {
        this.funcId = funcId;
    }

    /**
     * get:func_key
     */
    public String getFuncKey() {
        return funcKey;
    }

    /**
     * set:func_key
     */
    public void setFuncKey(String funcKey) {
        this.funcKey = funcKey == null ? null : funcKey.trim();
    }

    /**
     * get:gmt_create
     */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /**
     * set:gmt_create
     */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /**
     * get:gmt_modified
     */
    public Date getGmtModified() {
        return gmtModified;
    }

    /**
     * set:gmt_modified
     */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /**
     * get:func_name
     */
    public String getFuncName() {
        return funcName;
    }

    /**
     * set:func_name
     */
    public void setFuncName(String funcName) {
        this.funcName = funcName == null ? null : funcName.trim();
    }
}
