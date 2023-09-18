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
package com.qlangtech.tis.manage.common.apps;

import com.qlangtech.tis.manage.biz.dal.dao.IApplicationDAO;
import com.qlangtech.tis.manage.biz.dal.dao.IUsrDptRelationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.*;
import com.qlangtech.tis.manage.common.IUser;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.TriggerCrontab;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-28
 */
public abstract class AppsFetcher implements IAppsFetcher {

    protected final RunContext context;

    protected final IUser user;

    protected final Department dpt;

    private final List<String> authorityFuncList;

    private static final Logger log = LoggerFactory.getLogger(AppsFetcher.class);

    public static IAppsFetcher create(IUser user, RunContext context, boolean maxMach) {
        if (true || maxMach) {
            UsrDptRelation usr = user.getUsr();
            // context.getUsrDptRelationDAO()
            // .loadFromWriteDB(user.getId());
            Department dpt = new Department();
            // dpt.setDptId( usr.getDptId());
            // dpt.setFullName(usr.getDptName());
            dpt.setDptId(123);
            dpt.setFullName("dpt");
            return new TerminatorAdminAppsFetcher(user, dpt, context);
        }
        // TUser user = UserUtils.getUser(DefaultFilter.getReqeust());
        Assert.assertNotNull("user can not be null", user);
        Assert.assertNotNull("context can not be null", context);
        // context.getUsrDptRelationDAO().loadFromWriteDB(user.getId());
        UsrDptRelation usr = user.getUsr();
        Department dpt = new Department();
        dpt.setDptId(usr.getDptId());
        dpt.setFullName(usr.getDptName());
        Assert.assertNotNull("dpt.getDptId() can not be null", dpt.getDptId());
        // if (user.getAppsFetcher() == null) {
        if (// dpt.getDptId().equals(Config.getDptTerminatorId())
        true) {
            return // user.setAppsFetcher(
            new // );
            TerminatorAdminAppsFetcher(// );
            user, // );
            dpt, context);
        } else if (usr.isExtraDptRelation()) {
            return new NormalUserWithExtraDptsApplicationFetcher(user, dpt, context);
        } else {
            // 部门普通使用者
            return // user.setAppsFetcher(
            new // );
            NormalUserApplicationFetcher(// );
            user, // );
            dpt, context);
        }
    }

    public static IAppsFetcher create(IUser user, RunContext context) {
        return create(user, context, false);
    }

    @Override
    public boolean hasGrantAuthority(String permissionCode) {
        return this.authorityFuncList.contains(permissionCode);
    }

    protected final IApplicationDAO getApplicationDAO() {
        return context.getApplicationDAO();
    }

    protected AppsFetcher(IUser user, Department department, RunContext context) {
        super();
        this.context = context;
        this.user = user;
        this.dpt = department;
        this.authorityFuncList = this.initAuthorityFuncList();
        if (user != null && department != null && context != null) {
            log.warn("userid:" + user.getId() + ",name:" + user.getName() + ",class:" + this.getClass().getSimpleName() + ",this.authorityFuncList.class:" + this.authorityFuncList.getClass().getSimpleName() + ",this.authorityFuncList.size:" + this.authorityFuncList.size());
        }
    }

    protected abstract List<String> initAuthorityFuncList();


    @Override
    public abstract List<Application> getApps(CriteriaSetter setter);


    @Override
    public abstract int count(CriteriaSetter setter);

    @Override
    public abstract int update(Application app, CriteriaSetter setter);


    @Override
    public abstract List<TriggerCrontab> getTriggerTabs(IUsrDptRelationDAO usrDptRelationDAO);



    public interface CriteriaSetter {

        void set(ApplicationCriteria.Criteria criteria);
    }
}
