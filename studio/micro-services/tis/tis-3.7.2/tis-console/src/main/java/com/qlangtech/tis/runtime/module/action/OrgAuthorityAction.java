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
package com.qlangtech.tis.runtime.module.action;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.biz.dal.dao.IDepartmentDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelation;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.IUser;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.UserUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import junit.framework.Assert;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-5-10
 */
public abstract class OrgAuthorityAction extends BasicModule {

    /**
     */
    private static final long serialVersionUID = 1L;

    private static final XStream xstream = new XStream(new JsonHierarchicalStreamDriver());

    static {
        xstream.alias("result", JsonResult.class);
    // xstream.addImplicitCollection(JsonResult.class, "result");
    }

    @Override
    public boolean isEnableDomainView() {
        return false;
    }

    /**
     * 设置部门信息
     *
     * @param context
     */
    public void doSetDepartment(Context context) throws Exception {
        // 查看用户是否已经设置部门
        getResponse().setContentType("application/json");
        // 部门id
        Integer departmentId = this.getInt("orgadd");
        if (departmentId == null) {
            throw new IllegalArgumentException("departmentId can not be null");
        }
        UsrDptRelationCriteria query = new UsrDptRelationCriteria();
        query.createCriteria().andUsrIdEqualTo(this.getUserId());
        if (this.getUsrDptRelationDAO().countByExample(query) > 0) {
            // 已经设置过部门信息了
            getResponse().getWriter().print("{result:'您已经设置过部门信息了,如果要更改部门设置请通知管理员'}");
            return;
        }
        bindUser2Dpt(this, departmentId, UserUtils.getUser(this.getRequest(), this));
        getResponse().getWriter().print("{result:'您已成功设置部门信息'}");
    }

    public static void bindUser2Dpt(RunContext runContext, Integer departmentId, IUser user) {
        Assert.assertNotNull(user);
        UsrDptRelationCriteria query;
        UsrDptRelation relation = new UsrDptRelation();
        relation.setCreateTime(new Date());
        relation.setDptId(departmentId);
        relation.setDptName(getDepartmentName(runContext.getDepartmentDAO(), departmentId));
        relation.setUsrId(user.getId());
        relation.setUserName(user.getName());
        // relation.setIsDeleted("N");
        query = new UsrDptRelationCriteria();
        query.createCriteria().andUsrIdEqualTo(user.getId());
        Assert.assertNotNull(user.getId());
        runContext.getUsrDptRelationDAO().deleteByExample(query);
        runContext.getUsrDptRelationDAO().insertSelective(relation);
    }

    public static String getDepartmentName(IDepartmentDAO departmentDAO, Integer dptId) {
        List<Department> dptlist = new ArrayList<Department>();
        processDepartment(departmentDAO, dptlist, dptId);
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < dptlist.size(); i++) {
            result.append(dptlist.get(i).getName());
            if (i != (dptlist.size() - 1)) {
                result.append("-");
            }
        }
        return result.toString();
    }

    /**
     * @param
     * @param
     */
    public static void processDepartment(IDepartmentDAO departmentDAO, List<Department> dptlist, Integer dptId) {
        Department department = departmentDAO.loadFromWriteDB(dptId);
        if (department == null) {
            return;
        }
        if (department.getParentId() == null) {
            dptlist.add(department);
            return;
        }
        processDepartment(departmentDAO, dptlist, department.getParentId());
        dptlist.add(department);
    }

    public static class JsonResult {

        private final List<String> error = new ArrayList<String>();

        private final List<String> msg = new ArrayList<String>();

        private final List<String> deleteids = new ArrayList<String>();

        public void addDeleteId(String deleteid) {
            this.deleteids.add(deleteid);
        }

        public void addError(String error) {
            this.error.add(error);
        }

        public void addMsg(String msg) {
            this.msg.add(msg);
        }

        public List<String> getErrors() {
            return error;
        }

        public List<String> getMsg() {
            return msg;
        }
    }

    public static class Entry {

        private final Integer id;

        private final String name;

        Entry(Integer id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            return this.hashCode() == obj.hashCode();
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public static void main(String[] arg) {
        JsonResult result = new JsonResult();
        System.out.println(xstream.toXML(result));
    }
}
