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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.qlangtech.tis.manage.common.TISBaseCriteria;
import com.qlangtech.tis.manage.common.ibatis.BooleanYorNConvertCallback;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UsrDptRelationCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public UsrDptRelationCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected UsrDptRelationCriteria(UsrDptRelationCriteria example) {
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

        public Criteria andExtraDptRelationEqualToYES() {
            addCriterion("extra_dpt_relation =", BooleanYorNConvertCallback.YES, "extraDptRelation");
            return this;
        }

        public Criteria andAppIdEqual(Integer appid) {
            addCriterion("app.app_id =", appid, "usr_id");
            return this;
        }

        public Criteria andUsrIdIsNull() {
            addCriterion("usr_id is null");
            return this;
        }

        public Criteria andIsAutoDeploy() {
            addCriterion("app.is_auto_deploy=", BooleanYorNConvertCallback.YES, "usrId");
            return this;
        }

        public Criteria andUsrIdIsNotNull() {
            addCriterion("usr_id is not null");
            return this;
        }

        public Criteria andUsrIdEqualTo(String value) {
            addCriterion("usr_id =", value, "usrId");
            return this;
        }

        public Criteria andUsrIdNotEqualTo(String value) {
            addCriterion("usr_id <>", value, "usrId");
            return this;
        }

        public Criteria andUsrIdGreaterThan(String value) {
            addCriterion("usr_id >", value, "usrId");
            return this;
        }

        public Criteria andUsrIdGreaterThanOrEqualTo(String value) {
            addCriterion("usr_id >=", value, "usrId");
            return this;
        }

        public Criteria andUsrIdLessThan(String value) {
            addCriterion("usr_id <", value, "usrId");
            return this;
        }

        public Criteria andUsrIdLessThanOrEqualTo(String value) {
            addCriterion("usr_id <=", value, "usrId");
            return this;
        }

        public Criteria andUsrIdLike(String value) {
            addCriterion("usr_id like", value, "usrId");
            return this;
        }

        public Criteria andUsrIdNotLike(String value) {
            addCriterion("usr_id not like", value, "usrId");
            return this;
        }

        public Criteria andUsrIdIn(List<String> values) {
            addCriterion("usr_id in", values, "usrId");
            return this;
        }

        public Criteria andUsrIdNotIn(List<String> values) {
            addCriterion("usr_id not in", values, "usrId");
            return this;
        }

        public Criteria andUsrIdBetween(String value1, String value2) {
            addCriterion("usr_id between", value1, value2, "usrId");
            return this;
        }

        public Criteria andUsrIdNotBetween(String value1, String value2) {
            addCriterion("usr_id not between", value1, value2, "usrId");
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

        public Criteria andUserNameIsNull() {
            addCriterion("user_name is null");
            return this;
        }

        public Criteria andUserNameIsNotNull() {
            addCriterion("user_name is not null");
            return this;
        }

        public Criteria andUserNameEqualTo(String value) {
            addCriterion("user_name =", value, "userName");
            return this;
        }

        public Criteria andPasswordEqualTo(String value) {
            addCriterion("pass_word =", value, "pass_word");
            return this;
        }

        public Criteria andUserNameNotEqualTo(String value) {
            addCriterion("user_name <>", value, "userName");
            return this;
        }

        public Criteria andUserNameGreaterThan(String value) {
            addCriterion("user_name >", value, "userName");
            return this;
        }

        public Criteria andUserNameGreaterThanOrEqualTo(String value) {
            addCriterion("user_name >=", value, "userName");
            return this;
        }

        public Criteria andUserNameLessThan(String value) {
            addCriterion("user_name <", value, "userName");
            return this;
        }

        public Criteria andUserNameLessThanOrEqualTo(String value) {
            addCriterion("user_name <=", value, "userName");
            return this;
        }

        public Criteria andUserNameLike(String value) {
            addCriterion("user_name like", value, "userName");
            return this;
        }

        public Criteria andUserNameNotLike(String value) {
            addCriterion("user_name not like", value, "userName");
            return this;
        }

        public Criteria andUserNameIn(List<String> values) {
            addCriterion("user_name in", values, "userName");
            return this;
        }

        public Criteria andUserNameNotIn(List<String> values) {
            addCriterion("user_name not in", values, "userName");
            return this;
        }

        public Criteria andUserNameBetween(String value1, String value2) {
            addCriterion("user_name between", value1, value2, "userName");
            return this;
        }

        public Criteria andUserNameNotBetween(String value1, String value2) {
            addCriterion("user_name not between", value1, value2, "userName");
            return this;
        }

        public Criteria andRIdIsNull() {
            addCriterion("r_id is null");
            return this;
        }

        public Criteria andRIdIsNotNull() {
            addCriterion("r_id is not null");
            return this;
        }

        public Criteria andRIdEqualTo(Integer value) {
            addCriterion("r_id =", value, "rId");
            return this;
        }

        public Criteria andRIdNotEqualTo(Integer value) {
            addCriterion("r_id <>", value, "rId");
            return this;
        }

        public Criteria andRIdGreaterThan(Integer value) {
            addCriterion("r_id >", value, "rId");
            return this;
        }

        public Criteria andRIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("r_id >=", value, "rId");
            return this;
        }

        public Criteria andRIdLessThan(Integer value) {
            addCriterion("r_id <", value, "rId");
            return this;
        }

        public Criteria andRIdLessThanOrEqualTo(Integer value) {
            addCriterion("r_id <=", value, "rId");
            return this;
        }

        public Criteria andRIdIn(List<Integer> values) {
            addCriterion("r_id in", values, "rId");
            return this;
        }

        public Criteria andRIdNotIn(List<Integer> values) {
            addCriterion("r_id not in", values, "rId");
            return this;
        }

        public Criteria andRIdBetween(Integer value1, Integer value2) {
            addCriterion("r_id between", value1, value2, "rId");
            return this;
        }

        public Criteria andRIdNotBetween(Integer value1, Integer value2) {
            addCriterion("r_id not between", value1, value2, "rId");
            return this;
        }

        public Criteria andRoleNameIsNull() {
            addCriterion("role_name is null");
            return this;
        }

        public Criteria andRoleNameIsNotNull() {
            addCriterion("role_name is not null");
            return this;
        }

        public Criteria andRoleNameEqualTo(String value) {
            addCriterion("role_name =", value, "roleName");
            return this;
        }

        public Criteria andRoleNameNotEqualTo(String value) {
            addCriterion("role_name <>", value, "roleName");
            return this;
        }

        public Criteria andRoleNameGreaterThan(String value) {
            addCriterion("role_name >", value, "roleName");
            return this;
        }

        public Criteria andRoleNameGreaterThanOrEqualTo(String value) {
            addCriterion("role_name >=", value, "roleName");
            return this;
        }

        public Criteria andRoleNameLessThan(String value) {
            addCriterion("role_name <", value, "roleName");
            return this;
        }

        public Criteria andRoleNameLessThanOrEqualTo(String value) {
            addCriterion("role_name <=", value, "roleName");
            return this;
        }

        public Criteria andRoleNameLike(String value) {
            addCriterion("role_name like", value, "roleName");
            return this;
        }

        public Criteria andRoleNameNotLike(String value) {
            addCriterion("role_name not like", value, "roleName");
            return this;
        }

        public Criteria andRoleNameIn(List<String> values) {
            addCriterion("role_name in", values, "roleName");
            return this;
        }

        public Criteria andRoleNameNotIn(List<String> values) {
            addCriterion("role_name not in", values, "roleName");
            return this;
        }

        public Criteria andRoleNameBetween(String value1, String value2) {
            addCriterion("role_name between", value1, value2, "roleName");
            return this;
        }

        public Criteria andRoleNameNotBetween(String value1, String value2) {
            addCriterion("role_name not between", value1, value2, "roleName");
            return this;
        }
    }
}
