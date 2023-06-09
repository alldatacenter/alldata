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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import com.qlangtech.tis.manage.biz.dal.dao.IUsrDptRelationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.IUser;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.TriggerCrontab;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-28
 */
public class NormalUserApplicationFetcher extends AppsFetcher {

    private final AbandonRepeatList belongDpt = new AbandonRepeatList();

    private List<Integer> dptids;

    public NormalUserApplicationFetcher(IUser user, Department department, RunContext context) {
        super(user, department, context);
        processDepartment(department, context);
    }

    protected void processDepartment(Department department, RunContext context) {
        processBelongDepartment(context.getDepartmentDAO().loadFromWriteDB(department.getDptId()), context);
        postCreateBelongDpt(belongDpt);
        this.dptids = new ArrayList<Integer>(belongDpt.addIds);
    }

    private static class AbandonRepeatList extends ArrayList<Department> {

        private static final long serialVersionUID = 1L;

        private final Set<Integer> addIds = new HashSet<Integer>();

        @Override
        public boolean add(Department e) {
            if (addIds.contains(e.getDptId())) {
                return false;
            }
            addIds.add(e.getDptId());
            return super.add(e);
        }

        @Override
        public boolean addAll(Collection<? extends Department> c) {
            for (Department dpt : c) {
                this.add(dpt);
            }
            return true;
        }
    }

    protected void postCreateBelongDpt(List<Department> belongDpt) {
    }




    private void processBelongDepartment(Department department, RunContext context) {
        if (true) {
            return;
        }
        Assert.assertNotNull("department can not be null", department);
        Assert.assertNotNull("context can not be null", context);
        // 部门是叶子节点吗？
        if (department.getLeaf()) {
            belongDpt.add(department);
        } else {
            DepartmentCriteria query = new DepartmentCriteria();
            query.createCriteria().andParentIdEqualTo(department.getDptId());
            for (Department dpt : context.getDepartmentDAO().selectByExample(query)) {
                processBelongDepartment(dpt, context);
            }
        }
    }

    // NormalUserApplicationFetcher(TUser user, RunContext context) {
    // super(user, context);
    // }
    @Override
    public final List<Application> getApps(CriteriaSetter setter) {
        ApplicationCriteria criteria = createCriteria(setter);
        criteria.setOrderByClause("app_id desc");
        return this.getApplicationDAO().selectByExample(criteria, 1, 500);
    }

    @Override
    public final int count(CriteriaSetter setter) {
        ApplicationCriteria criteria = createCriteria(setter);
        return this.getApplicationDAO().countByExample(criteria);
    }

    private ApplicationCriteria createCriteria(CriteriaSetter setter) {
        ApplicationCriteria criteria = new ApplicationCriteria();
        setter.set(process(criteria.createCriteria()));
        return criteria;
    }

    @Override
    public int update(Application app, CriteriaSetter setter) {
        ApplicationCriteria criteria = createCriteria(setter);
        return this.getApplicationDAO().updateByExampleSelective(app, criteria);
    }

    protected ApplicationCriteria.Criteria process(ApplicationCriteria.Criteria criteria) {
        // } else {
        return criteria.andDptIdIn(dptids);
    // }
    }

    @Override
    public List<Department> getDepartmentBelongs(RunContext runcontext) {
        return this.belongDpt;
    }

    @Override
    public List<TriggerCrontab> getTriggerTabs(IUsrDptRelationDAO usrDptRelationDAO) {
        UsrDptRelationCriteria ucriteria = new UsrDptRelationCriteria();
        // if (this.dpt.getLeaf()) {
        ucriteria.createCriteria().andDptIdIn(dptids).andIsAutoDeploy();
        // 应用触发器一览
        return usrDptRelationDAO.selectAppDumpJob(ucriteria);
    }

    @SuppressWarnings("all")
    @Override
    protected List<String> initAuthorityFuncList() {
        // .selectFuncListByUsrid(user.getId()));
        return new ArrayList<String>() {

            @Override
            public boolean contains(Object o) {
                return true;
            }
        };
    // }
    // try {
    // // 线上环境
    // List<String> authorityFuncList = new ArrayList<String>();
    // SimpleSSOUser ssoUser;
    //
    // ssoUser = SimpleUserUtil
    // .findUser(ServletActionContext.getRequest());
    // String userId = ssoUser.getId().toString();
    // UserPermissionCondition userPermissionCondition = new
    // UserPermissionCondition();
    // userPermissionCondition.setUserId(userId);// 接入SSO登录后可获得,
    // // simpleSSOUser.getId()
    // List<String> appNames = new ArrayList<String>();
    // appNames.add("terminatorconsole");// 在ACL后台配置的应用英文名
    // userPermissionCondition.setAppNames(appNames);
    // List<Permission> results;
    //
    // results = AclServiceProvider.getUserPermissionService()
    // .findPermissionByUser(userPermissionCondition);
    // for (Permission permission : results) {
    // authorityFuncList.add(permission.getName());
    // }
    // return authorityFuncList;
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    }
}
