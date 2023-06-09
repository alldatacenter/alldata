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
public class RdsDb implements Serializable {

    /**
     * prop:主键
     */
    private Long id;

    /**
     * prop:创建时间
     */
    private Date gmtCreate;

    /**
     * prop:修改时间
     */
    private Date gmtModified;

    /**
     * prop:host地址
     */
    private String host;

    /**
     * prop:数据库所属rds实例名
     */
    private String rdsName;

    /**
     * prop:用户名
     */
    private String userName;

    /**
     * prop:密码
     */
    private String password;

    /**
     * prop:isv id
     */
    private Long iId;

    /**
     * prop:数据库名
     */
    private String dbName;

    private static final long serialVersionUID = 1L;

    /**
     * get:主键
     */
    public Long getId() {
        return id;
    }

    /**
     * set:主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * get:创建时间
     */
    public Date getGmtCreate() {
        return gmtCreate;
    }

    /**
     * set:创建时间
     */
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    /**
     * get:修改时间
     */
    public Date getGmtModified() {
        return gmtModified;
    }

    /**
     * set:修改时间
     */
    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    /**
     * get:host地址
     */
    public String getHost() {
        return host;
    }

    /**
     * set:host地址
     */
    public void setHost(String host) {
        this.host = host == null ? null : host.trim();
    }

    /**
     * get:数据库所属rds实例名
     */
    public String getRdsName() {
        return rdsName;
    }

    /**
     * set:数据库所属rds实例名
     */
    public void setRdsName(String rdsName) {
        this.rdsName = rdsName == null ? null : rdsName.trim();
    }

    /**
     * get:用户名
     */
    public String getUserName() {
        return userName;
    }

    /**
     * set:用户名
     */
    public void setUserName(String userName) {
        this.userName = userName == null ? null : userName.trim();
    }

    /**
     * get:密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * set:密码
     */
    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    /**
     * get:isv id
     */
    public Long getiId() {
        return iId;
    }

    /**
     * set:isv id
     */
    public void setiId(Long iId) {
        this.iId = iId;
    }

    /**
     * get:数据库名
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * set:数据库名
     */
    public void setDbName(String dbName) {
        this.dbName = dbName == null ? null : dbName.trim();
    }
}
