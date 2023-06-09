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

import com.qlangtech.tis.manage.common.TISBaseCriteria;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ApplicationCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public ApplicationCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected ApplicationCriteria(ApplicationCriteria example) {
        this.orderByClause = example.orderByClause;
        this.oredCriteria = example.oredCriteria;
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        oredCriteria.clear();
    }

    public static class Criteria {

        protected List<String> criteriaWithoutValue;

        protected List<Map<String, Object>> criteriaWithSingleValue;

        protected List<Map<String, Object>> criteriaWithListValue;

        protected List<Map<String, Object>> criteriaWithBetweenValue;

        protected Criteria() {
            super();
            criteriaWithoutValue = new ArrayList<String>();
            criteriaWithSingleValue = new ArrayList<Map<String, Object>>();
            criteriaWithListValue = new ArrayList<Map<String, Object>>();
            criteriaWithBetweenValue = new ArrayList<Map<String, Object>>();
        }

        public boolean isValid() {
            return criteriaWithoutValue.size() > 0 || criteriaWithSingleValue.size() > 0 || criteriaWithListValue.size() > 0 || criteriaWithBetweenValue.size() > 0;
        }

        public List<String> getCriteriaWithoutValue() {
            return criteriaWithoutValue;
        }

        public List<Map<String, Object>> getCriteriaWithSingleValue() {
            return criteriaWithSingleValue;
        }

        public List<Map<String, Object>> getCriteriaWithListValue() {
            return criteriaWithListValue;
        }

        public List<Map<String, Object>> getCriteriaWithBetweenValue() {
            return criteriaWithBetweenValue;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteriaWithoutValue.add(condition);
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("condition", condition);
            map.put("value", value);
            criteriaWithSingleValue.add(map);
        }

        protected void addCriterion(String condition, List<? extends Object> values, String property) {
            if (values == null || values.size() == 0) {
                throw new RuntimeException("Value list for " + property + " cannot be null or empty");
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("condition", condition);
            map.put("values", values);
            criteriaWithListValue.add(map);
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            List<Object> list = new ArrayList<Object>();
            list.add(value1);
            list.add(value2);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("condition", condition);
            map.put("values", list);
            criteriaWithBetweenValue.add(map);
        }

        public Criteria andAppIdIsNull() {
            addCriterion("app_id is null");
            return this;
        }

        public Criteria andAppIdIsNotNull() {
            addCriterion("app_id is not null");
            return this;
        }

        public Criteria andAppIdEqualTo(Integer value) {
            addCriterion("app_id =", value, "appId");
            return this;
        }

        public Criteria andAppTypeEqualTo(Integer value) {
            addCriterion("app_type =", value, "appType");
            return this;
        }

        public Criteria andAppIdNotEqualTo(Integer value) {
            addCriterion("app_id <>", value, "appId");
            return this;
        }

        public Criteria andAppIdGreaterThan(Integer value) {
            addCriterion("app_id >", value, "appId");
            return this;
        }

        public Criteria andAppIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("app_id >=", value, "appId");
            return this;
        }

        public Criteria andAppIdLessThan(Integer value) {
            addCriterion("app_id <", value, "appId");
            return this;
        }

        public Criteria andAppIdLessThanOrEqualTo(Integer value) {
            addCriterion("app_id <=", value, "appId");
            return this;
        }

        public Criteria andAppIdIn(List<Integer> values) {
            addCriterion("app_id in", values, "appId");
            return this;
        }

        public Criteria andAppIdNotIn(List<Integer> values) {
            addCriterion("app_id not in", values, "appId");
            return this;
        }

        public Criteria andAppIdBetween(Integer value1, Integer value2) {
            addCriterion("app_id between", value1, value2, "appId");
            return this;
        }

        public Criteria andAppIdNotBetween(Integer value1, Integer value2) {
            addCriterion("app_id not between", value1, value2, "appId");
            return this;
        }

        public Criteria andProjectNameIsNull() {
            addCriterion("project_name is null");
            return this;
        }

        public Criteria andProjectNameIsNotNull() {
            addCriterion("project_name is not null");
            return this;
        }

        public Criteria andProjectNameEqualTo(String value) {
            addCriterion("project_name =", value, "projectName");
            return this;
        }

        public Criteria andProjectNameNotEqualTo(String value) {
            addCriterion("project_name <>", value, "projectName");
            return this;
        }

        public Criteria andProjectNameGreaterThan(String value) {
            addCriterion("project_name >", value, "projectName");
            return this;
        }

        public Criteria andProjectNameGreaterThanOrEqualTo(String value) {
            addCriterion("project_name >=", value, "projectName");
            return this;
        }

        public Criteria andProjectNameLessThan(String value) {
            addCriterion("project_name <", value, "projectName");
            return this;
        }

        public Criteria andProjectNameLessThanOrEqualTo(String value) {
            addCriterion("project_name <=", value, "projectName");
            return this;
        }

        public Criteria andProjectNameLike(String value) {
            addCriterion("project_name like", value, "projectName");
            return this;
        }

        public Criteria andProjectNameNotLike(String value) {
            addCriterion("project_name not like", value, "projectName");
            return this;
        }

        public Criteria andProjectNameIn(List<String> values) {
            addCriterion("project_name in", values, "projectName");
            return this;
        }

        public Criteria andProjectNameNotIn(List<String> values) {
            addCriterion("project_name not in", values, "projectName");
            return this;
        }

        public Criteria andProjectNameBetween(String value1, String value2) {
            addCriterion("project_name between", value1, value2, "projectName");
            return this;
        }

        public Criteria andProjectNameNotBetween(String value1, String value2) {
            addCriterion("project_name not between", value1, value2, "projectName");
            return this;
        }

        public Criteria andReceptIsNull() {
            addCriterion("recept is null");
            return this;
        }

        public Criteria andReceptIsNotNull() {
            addCriterion("recept is not null");
            return this;
        }

        public Criteria andReceptEqualTo(String value) {
            addCriterion("recept =", value, "recept");
            return this;
        }

        public Criteria andReceptNotEqualTo(String value) {
            addCriterion("recept <>", value, "recept");
            return this;
        }

        public Criteria andReceptGreaterThan(String value) {
            addCriterion("recept >", value, "recept");
            return this;
        }

        public Criteria andReceptGreaterThanOrEqualTo(String value) {
            addCriterion("recept >=", value, "recept");
            return this;
        }

        public Criteria andReceptLessThan(String value) {
            addCriterion("recept <", value, "recept");
            return this;
        }

        public Criteria andReceptLessThanOrEqualTo(String value) {
            addCriterion("recept <=", value, "recept");
            return this;
        }

        public Criteria andReceptLike(String value) {
            addCriterion("recept like", value, "recept");
            return this;
        }

        public Criteria andReceptNotLike(String value) {
            addCriterion("recept not like", value, "recept");
            return this;
        }

        public Criteria andReceptIn(List<String> values) {
            addCriterion("recept in", values, "recept");
            return this;
        }

        public Criteria andReceptNotIn(List<String> values) {
            addCriterion("recept not in", values, "recept");
            return this;
        }

        public Criteria andReceptBetween(String value1, String value2) {
            addCriterion("recept between", value1, value2, "recept");
            return this;
        }

        public Criteria andReceptNotBetween(String value1, String value2) {
            addCriterion("recept not between", value1, value2, "recept");
            return this;
        }

        public Criteria andManagerIsNull() {
            addCriterion("manager is null");
            return this;
        }

        public Criteria andManagerIsNotNull() {
            addCriterion("manager is not null");
            return this;
        }

        public Criteria andManagerEqualTo(String value) {
            addCriterion("manager =", value, "manager");
            return this;
        }

        public Criteria andManagerNotEqualTo(String value) {
            addCriterion("manager <>", value, "manager");
            return this;
        }

        public Criteria andManagerGreaterThan(String value) {
            addCriterion("manager >", value, "manager");
            return this;
        }

        public Criteria andManagerGreaterThanOrEqualTo(String value) {
            addCriterion("manager >=", value, "manager");
            return this;
        }

        public Criteria andManagerLessThan(String value) {
            addCriterion("manager <", value, "manager");
            return this;
        }

        public Criteria andManagerLessThanOrEqualTo(String value) {
            addCriterion("manager <=", value, "manager");
            return this;
        }

        public Criteria andManagerLike(String value) {
            addCriterion("manager like", value, "manager");
            return this;
        }

        public Criteria andManagerNotLike(String value) {
            addCriterion("manager not like", value, "manager");
            return this;
        }

        public Criteria andManagerIn(List<String> values) {
            addCriterion("manager in", values, "manager");
            return this;
        }

        public Criteria andManagerNotIn(List<String> values) {
            addCriterion("manager not in", values, "manager");
            return this;
        }

        public Criteria andManagerBetween(String value1, String value2) {
            addCriterion("manager between", value1, value2, "manager");
            return this;
        }

        public Criteria andManagerNotBetween(String value1, String value2) {
            addCriterion("manager not between", value1, value2, "manager");
            return this;
        }

        public Criteria andCreateTimeIsNull() {
            addCriterion("create_time is null");
            return this;
        }

        public Criteria andCreateTimeIsNotNull() {
            addCriterion("create_time is not null");
            return this;
        }

        public Criteria andCreateTimeEqualTo(Date value) {
            addCriterion("create_time =", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeNotEqualTo(Date value) {
            addCriterion("create_time <>", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeGreaterThan(Date value) {
            addCriterion("create_time >", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("create_time >=", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeLessThan(Date value) {
            addCriterion("create_time <", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(Date value) {
            addCriterion("create_time <=", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeIn(List<Date> values) {
            addCriterion("create_time in", values, "createTime");
            return this;
        }

        public Criteria andCreateTimeNotIn(List<Date> values) {
            addCriterion("create_time not in", values, "createTime");
            return this;
        }

        public Criteria andCreateTimeBetween(Date value1, Date value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return this;
        }

        public Criteria andCreateTimeNotBetween(Date value1, Date value2) {
            addCriterion("create_time not between", value1, value2, "createTime");
            return this;
        }

        public Criteria andUpdateTimeIsNull() {
            addCriterion("update_time is null");
            return this;
        }

        public Criteria andUpdateTimeIsNotNull() {
            addCriterion("update_time is not null");
            return this;
        }

        public Criteria andUpdateTimeEqualTo(Date value) {
            addCriterion("update_time =", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeNotEqualTo(Date value) {
            addCriterion("update_time <>", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeGreaterThan(Date value) {
            addCriterion("update_time >", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("update_time >=", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeLessThan(Date value) {
            addCriterion("update_time <", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeLessThanOrEqualTo(Date value) {
            addCriterion("update_time <=", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeIn(List<Date> values) {
            addCriterion("update_time in", values, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeNotIn(List<Date> values) {
            addCriterion("update_time not in", values, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeBetween(Date value1, Date value2) {
            addCriterion("update_time between", value1, value2, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeNotBetween(Date value1, Date value2) {
            addCriterion("update_time not between", value1, value2, "updateTime");
            return this;
        }

        public Criteria andIsDeletedIsNull() {
            addCriterion("is_deleted is null");
            return this;
        }

        public Criteria andIsDeletedIsNotNull() {
            addCriterion("is_deleted is not null");
            return this;
        }

        public Criteria andIsDeletedEqualTo(String value) {
            addCriterion("is_deleted =", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedNotEqualTo(String value) {
            addCriterion("is_deleted <>", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedGreaterThan(String value) {
            addCriterion("is_deleted >", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedGreaterThanOrEqualTo(String value) {
            addCriterion("is_deleted >=", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedLessThan(String value) {
            addCriterion("is_deleted <", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedLessThanOrEqualTo(String value) {
            addCriterion("is_deleted <=", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedLike(String value) {
            addCriterion("is_deleted like", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedNotLike(String value) {
            addCriterion("is_deleted not like", value, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedIn(List<String> values) {
            addCriterion("is_deleted in", values, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedNotIn(List<String> values) {
            addCriterion("is_deleted not in", values, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedBetween(String value1, String value2) {
            addCriterion("is_deleted between", value1, value2, "isDeleted");
            return this;
        }

        public Criteria andIsDeletedNotBetween(String value1, String value2) {
            addCriterion("is_deleted not between", value1, value2, "isDeleted");
            return this;
        }

        public Criteria andIsAutoDeployIsNull() {
            addCriterion("is_auto_deploy is null");
            return this;
        }

        public Criteria andIsAutoDeployIsNotNull() {
            addCriterion("is_auto_deploy is not null");
            return this;
        }

        public Criteria andIsAutoDeployEqualTo(String value) {
            addCriterion("is_auto_deploy =", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployNotEqualTo(String value) {
            addCriterion("is_auto_deploy <>", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployGreaterThan(String value) {
            addCriterion("is_auto_deploy >", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployGreaterThanOrEqualTo(String value) {
            addCriterion("is_auto_deploy >=", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployLessThan(String value) {
            addCriterion("is_auto_deploy <", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployLessThanOrEqualTo(String value) {
            addCriterion("is_auto_deploy <=", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployLike(String value) {
            addCriterion("is_auto_deploy like", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployNotLike(String value) {
            addCriterion("is_auto_deploy not like", value, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployIn(List<String> values) {
            addCriterion("is_auto_deploy in", values, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployNotIn(List<String> values) {
            addCriterion("is_auto_deploy not in", values, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployBetween(String value1, String value2) {
            addCriterion("is_auto_deploy between", value1, value2, "isAutoDeploy");
            return this;
        }

        public Criteria andIsAutoDeployNotBetween(String value1, String value2) {
            addCriterion("is_auto_deploy not between", value1, value2, "isAutoDeploy");
            return this;
        }

        public Criteria andDptIdIsNull() {
            addCriterion("dpt_id is null");
            return this;
        }

        public Criteria andDptIdIsNotNull() {
            addCriterion("dpt_id is not null");
            return this;
        }

        public Criteria andDptIdEqualTo(Integer value) {
            addCriterion("dpt_id =", value, "dptId");
            return this;
        }

        public Criteria andDptIdNotEqualTo(Integer value) {
            addCriterion("dpt_id <>", value, "dptId");
            return this;
        }

        public Criteria andDptIdGreaterThan(Integer value) {
            addCriterion("dpt_id >", value, "dptId");
            return this;
        }

        public Criteria andDptIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("dpt_id >=", value, "dptId");
            return this;
        }

        public Criteria andDptIdLessThan(Integer value) {
            addCriterion("dpt_id <", value, "dptId");
            return this;
        }

        public Criteria andDptIdLessThanOrEqualTo(Integer value) {
            addCriterion("dpt_id <=", value, "dptId");
            return this;
        }

        public Criteria andDptIdIn(List<Integer> values) {
            addCriterion("dpt_id in", values, "dptId");
            return this;
        }

        public Criteria andDptIdNotIn(List<Integer> values) {
            addCriterion("dpt_id not in", values, "dptId");
            return this;
        }

        public Criteria andDptIdBetween(Integer value1, Integer value2) {
            addCriterion("dpt_id between", value1, value2, "dptId");
            return this;
        }

        public Criteria andDptIdNotBetween(Integer value1, Integer value2) {
            addCriterion("dpt_id not between", value1, value2, "dptId");
            return this;
        }

        public Criteria andDptNameIsNull() {
            addCriterion("dpt_name is null");
            return this;
        }

        public Criteria andDptNameIsNotNull() {
            addCriterion("dpt_name is not null");
            return this;
        }

        public Criteria andDptNameEqualTo(String value) {
            addCriterion("dpt_name =", value, "dptName");
            return this;
        }

        public Criteria andDptNameNotEqualTo(String value) {
            addCriterion("dpt_name <>", value, "dptName");
            return this;
        }

        public Criteria andDptNameGreaterThan(String value) {
            addCriterion("dpt_name >", value, "dptName");
            return this;
        }

        public Criteria andDptNameGreaterThanOrEqualTo(String value) {
            addCriterion("dpt_name >=", value, "dptName");
            return this;
        }

        public Criteria andDptNameLessThan(String value) {
            addCriterion("dpt_name <", value, "dptName");
            return this;
        }

        public Criteria andDptNameLessThanOrEqualTo(String value) {
            addCriterion("dpt_name <=", value, "dptName");
            return this;
        }

        public Criteria andDptNameLike(String value) {
            addCriterion("dpt_name like", value, "dptName");
            return this;
        }

        public Criteria andDptNameNotLike(String value) {
            addCriterion("dpt_name not like", value, "dptName");
            return this;
        }

        public Criteria andDptNameIn(List<String> values) {
            addCriterion("dpt_name in", values, "dptName");
            return this;
        }

        public Criteria andDptNameNotIn(List<String> values) {
            addCriterion("dpt_name not in", values, "dptName");
            return this;
        }

        public Criteria andDptNameBetween(String value1, String value2) {
            addCriterion("dpt_name between", value1, value2, "dptName");
            return this;
        }

        public Criteria andDptNameNotBetween(String value1, String value2) {
            addCriterion("dpt_name not between", value1, value2, "dptName");
            return this;
        }

        public Criteria andWorkFlowIdIsNull() {
            addCriterion("work_flow_id is null");
            return this;
        }

        public Criteria andWorkFlowIdIsNotNull() {
            addCriterion("work_flow_id is not null");
            return this;
        }

        public Criteria andWorkFlowIdEqualTo(Integer value) {
            addCriterion("work_flow_id =", value, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdNotEqualTo(Integer value) {
            addCriterion("work_flow_id <>", value, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdGreaterThan(Integer value) {
            addCriterion("work_flow_id >", value, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("work_flow_id >=", value, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdLessThan(Integer value) {
            addCriterion("work_flow_id <", value, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdLessThanOrEqualTo(Integer value) {
            addCriterion("work_flow_id <=", value, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdIn(List<Integer> values) {
            addCriterion("work_flow_id in", values, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdNotIn(List<Integer> values) {
            addCriterion("work_flow_id not in", values, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdBetween(Integer value1, Integer value2) {
            addCriterion("work_flow_id between", value1, value2, "workFlowId");
            return this;
        }

        public Criteria andWorkFlowIdNotBetween(Integer value1, Integer value2) {
            addCriterion("work_flow_id not between", value1, value2, "workFlowId");
            return this;
        }

        public Criteria andfullBuildCronTimeIsNull() {
            addCriterion("full_build_cron_time is null");
            return this;
        }

        public Criteria andfullBuildCronTimeIsNotNull() {
            addCriterion("full_build_cron_time is not null");
            return this;
        }

        public Criteria andfullBuildCronTimeEqualTo(String value) {
            addCriterion("full_build_cron_time =", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeNotEqualTo(String value) {
            addCriterion("full_build_cron_time <>", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeGreaterThan(String value) {
            addCriterion("full_build_cron_time >", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeGreaterThanOrEqualTo(String value) {
            addCriterion("full_build_cron_time >=", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeLessThan(String value) {
            addCriterion("full_build_cron_time <", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeLessThanOrEqualTo(String value) {
            addCriterion("full_build_cron_time <=", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeLike(String value) {
            addCriterion("full_build_cron_time like", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeNotLike(String value) {
            addCriterion("full_build_cron_time not like", value, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeIn(List<String> values) {
            addCriterion("full_build_cron_time in", values, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeNotIn(List<String> values) {
            addCriterion("full_build_cron_time not in", values, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeBetween(String value1, String value2) {
            addCriterion("full_build_cron_time between", value1, value2, "fullBuildCronTime");
            return this;
        }

        public Criteria andfullBuildCronTimeNotBetween(String value1, String value2) {
            addCriterion("full_build_cron_time not between", value1, value2, "fullBuildCronTime");
            return this;
        }
    }
}
