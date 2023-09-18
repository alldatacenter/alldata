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
package com.qlangtech.tis.manage.common;

import com.alibaba.fastjson.annotation.JSONField;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelation;
import com.qlangtech.tis.manage.common.apps.AppsFetcher;
import com.qlangtech.tis.manage.common.apps.IAppsFetcher;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-23
 */
public class TUser implements IUser {

    private final String id;

    private final String name;

    private Integer departmentid;

    private String department;

    private final UsrDptRelation usr;

    private final IAppsFetcher appsFetcher;

    private final RunContext runContext;

    private String email;

    public TUser(UsrDptRelation usr, RunContext runContext) {
        // this(usr,runContext,);
        this.id = usr.getUsrId();
        this.name = usr.getUserName();
        this.runContext = runContext;
        this.usr = usr;
        this.appsFetcher = AppsFetcher.create(this, runContext);
    }

    @Override
    public boolean hasLogin() {
        return true;
    }

    public TUser(UsrDptRelation usr, RunContext runContext, IAppsFetcher appsFetcher) {
        super();
        this.usr = usr;
        this.id = usr.getUsrId();
        this.name = usr.getUserName();
        this.runContext = runContext;
        this.appsFetcher = appsFetcher;
    }

    @JSONField(serialize = false)
    public UsrDptRelation getUsr() {
        return usr;
    }

    @JSONField(serialize = false)
    public IAppsFetcher getAppsFetcher() {
        return appsFetcher;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    public boolean hasGrantAuthority(String permissionCode) {
        return this.appsFetcher.hasGrantAuthority(permissionCode);
    }

    /**
     * 部門id
     *
     * @return
     */
    public Integer getDepartmentid() {
        return departmentid;
    }

    public void setDepartmentid(Integer departmentid) {
        this.departmentid = departmentid;
    }

    /**
     * 部门描述
     *
     * @return
     */
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return usr.getUserName();
    }
}
