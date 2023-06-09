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
public class BizFuncAuthorityCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public BizFuncAuthorityCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected BizFuncAuthorityCriteria(BizFuncAuthorityCriteria example) {
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
            criteria.andNotDelete();
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

        // baisui add has not been delete start
        public Criteria andNotDelete() {
            addCriterion("biz_func_authority.is_deleted =", "N", "biz_func_authority.is_deleted");
            return this;
        }

        // baisui add has not been delete end
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

        public Criteria andBfIdIsNull() {
            addCriterion("bf_id is null");
            return this;
        }

        public Criteria andBfIdIsNotNull() {
            addCriterion("bf_id is not null");
            return this;
        }

        public Criteria andBfIdEqualTo(Integer value) {
            addCriterion("bf_id =", value, "bfId");
            return this;
        }

        public Criteria andBfIdNotEqualTo(Integer value) {
            addCriterion("bf_id <>", value, "bfId");
            return this;
        }

        public Criteria andBfIdGreaterThan(Integer value) {
            addCriterion("bf_id >", value, "bfId");
            return this;
        }

        public Criteria andBfIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("bf_id >=", value, "bfId");
            return this;
        }

        public Criteria andBfIdLessThan(Integer value) {
            addCriterion("bf_id <", value, "bfId");
            return this;
        }

        public Criteria andBfIdLessThanOrEqualTo(Integer value) {
            addCriterion("bf_id <=", value, "bfId");
            return this;
        }

        public Criteria andBfIdIn(List<Integer> values) {
            addCriterion("bf_id in", values, "bfId");
            return this;
        }

        public Criteria andBfIdNotIn(List<Integer> values) {
            addCriterion("bf_id not in", values, "bfId");
            return this;
        }

        public Criteria andBfIdBetween(Integer value1, Integer value2) {
            addCriterion("bf_id between", value1, value2, "bfId");
            return this;
        }

        public Criteria andBfIdNotBetween(Integer value1, Integer value2) {
            addCriterion("bf_id not between", value1, value2, "bfId");
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

        public Criteria andFuncIdIsNull() {
            addCriterion("func_id is null");
            return this;
        }

        public Criteria andFuncIdIsNotNull() {
            addCriterion("func_id is not null");
            return this;
        }

        public Criteria andFuncIdEqualTo(String value) {
            addCriterion("func_id =", value, "funcId");
            return this;
        }

        public Criteria andFuncIdNotEqualTo(String value) {
            addCriterion("func_id <>", value, "funcId");
            return this;
        }

        public Criteria andFuncIdGreaterThan(String value) {
            addCriterion("func_id >", value, "funcId");
            return this;
        }

        public Criteria andFuncIdGreaterThanOrEqualTo(String value) {
            addCriterion("func_id >=", value, "funcId");
            return this;
        }

        public Criteria andFuncIdLessThan(String value) {
            addCriterion("func_id <", value, "funcId");
            return this;
        }

        public Criteria andFuncIdLessThanOrEqualTo(String value) {
            addCriterion("func_id <=", value, "funcId");
            return this;
        }

        public Criteria andFuncIdLike(String value) {
            addCriterion("func_id like", value, "funcId");
            return this;
        }

        public Criteria andFuncIdNotLike(String value) {
            addCriterion("func_id not like", value, "funcId");
            return this;
        }

        public Criteria andFuncIdIn(List<String> values) {
            addCriterion("func_id in", values, "funcId");
            return this;
        }

        public Criteria andFuncIdNotIn(List<String> values) {
            addCriterion("func_id not in", values, "funcId");
            return this;
        }

        public Criteria andFuncIdBetween(String value1, String value2) {
            addCriterion("func_id between", value1, value2, "funcId");
            return this;
        }

        public Criteria andFuncIdNotBetween(String value1, String value2) {
            addCriterion("func_id not between", value1, value2, "funcId");
            return this;
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

        public Criteria andFuncDescIsNull() {
            addCriterion("func_desc is null");
            return this;
        }

        public Criteria andFuncDescIsNotNull() {
            addCriterion("func_desc is not null");
            return this;
        }

        public Criteria andFuncDescEqualTo(String value) {
            addCriterion("func_desc =", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescNotEqualTo(String value) {
            addCriterion("func_desc <>", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescGreaterThan(String value) {
            addCriterion("func_desc >", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescGreaterThanOrEqualTo(String value) {
            addCriterion("func_desc >=", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescLessThan(String value) {
            addCriterion("func_desc <", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescLessThanOrEqualTo(String value) {
            addCriterion("func_desc <=", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescLike(String value) {
            addCriterion("func_desc like", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescNotLike(String value) {
            addCriterion("func_desc not like", value, "funcDesc");
            return this;
        }

        public Criteria andFuncDescIn(List<String> values) {
            addCriterion("func_desc in", values, "funcDesc");
            return this;
        }

        public Criteria andFuncDescNotIn(List<String> values) {
            addCriterion("func_desc not in", values, "funcDesc");
            return this;
        }

        public Criteria andFuncDescBetween(String value1, String value2) {
            addCriterion("func_desc between", value1, value2, "funcDesc");
            return this;
        }

        public Criteria andFuncDescNotBetween(String value1, String value2) {
            addCriterion("func_desc not between", value1, value2, "funcDesc");
            return this;
        }

        public Criteria andAppNameIsNull() {
            addCriterion("app_name is null");
            return this;
        }

        public Criteria andAppNameIsNotNull() {
            addCriterion("app_name is not null");
            return this;
        }

        public Criteria andAppNameEqualTo(String value) {
            addCriterion("app_name =", value, "appName");
            return this;
        }

        public Criteria andAppNameNotEqualTo(String value) {
            addCriterion("app_name <>", value, "appName");
            return this;
        }

        public Criteria andAppNameGreaterThan(String value) {
            addCriterion("app_name >", value, "appName");
            return this;
        }

        public Criteria andAppNameGreaterThanOrEqualTo(String value) {
            addCriterion("app_name >=", value, "appName");
            return this;
        }

        public Criteria andAppNameLessThan(String value) {
            addCriterion("app_name <", value, "appName");
            return this;
        }

        public Criteria andAppNameLessThanOrEqualTo(String value) {
            addCriterion("app_name <=", value, "appName");
            return this;
        }

        public Criteria andAppNameLike(String value) {
            addCriterion("app_name like", value, "appName");
            return this;
        }

        public Criteria andAppNameNotLike(String value) {
            addCriterion("app_name not like", value, "appName");
            return this;
        }

        public Criteria andAppNameIn(List<String> values) {
            addCriterion("app_name in", values, "appName");
            return this;
        }

        public Criteria andAppNameNotIn(List<String> values) {
            addCriterion("app_name not in", values, "appName");
            return this;
        }

        public Criteria andAppNameBetween(String value1, String value2) {
            addCriterion("app_name between", value1, value2, "appName");
            return this;
        }

        public Criteria andAppNameNotBetween(String value1, String value2) {
            addCriterion("app_name not between", value1, value2, "appName");
            return this;
        }
    }
}
