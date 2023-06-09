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
public class Func implements Serializable {

    /**
     * prop:涓婚敭
     */
    private Integer funId;

    /**
     * prop:fun_key
     */
    private String funKey;

    /**
     * prop:func_name
     */
    private String funcName;

    /**
     * prop:gmt_create
     */
    private Date gmtCreate;

    /**
     * prop:gmt_modified
     */
    private Date gmtModified;

    /**
     * prop:鍔熻兘缁刱ey
     */
    private Integer funcGroupKey;

    /**
     * prop:鍔熻兘缁勫悕绉�
     */
    private String funcGroupName;

    private static final long serialVersionUID = 1L;

    /**
     * get:涓婚敭
     */
    public Integer getFunId() {
        return funId;
    }

    /**
     * set:涓婚敭
     */
    public void setFunId(Integer funId) {
        this.funId = funId;
    }

    /**
     * get:fun_key
     */
    public String getFunKey() {
        return funKey;
    }

    /**
     * set:fun_key
     */
    public void setFunKey(String funKey) {
        this.funKey = funKey == null ? null : funKey.trim();
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
     * get:鍔熻兘缁刱ey
     */
    public Integer getFuncGroupKey() {
        return funcGroupKey;
    }

    /**
     * set:鍔熻兘缁刱ey
     */
    public void setFuncGroupKey(Integer funcGroupKey) {
        this.funcGroupKey = funcGroupKey;
    }

    /**
     * get:鍔熻兘缁勫悕绉�
     */
    public String getFuncGroupName() {
        return funcGroupName;
    }

    /**
     * set:鍔熻兘缁勫悕绉�
     */
    public void setFuncGroupName(String funcGroupName) {
        this.funcGroupName = funcGroupName == null ? null : funcGroupName.trim();
    }
}
