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
import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class OperationLogCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public OperationLogCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected OperationLogCriteria(OperationLogCriteria example) {
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

        public Criteria andOpIdIsNull() {
            addCriterion("op_id is null");
            return this;
        }

        public Criteria andOpIdIsNotNull() {
            addCriterion("op_id is not null");
            return this;
        }

        public Criteria andOpIdEqualTo(Integer value) {
            addCriterion("op_id =", value, "opId");
            return this;
        }

        public Criteria andOpIdNotEqualTo(Integer value) {
            addCriterion("op_id <>", value, "opId");
            return this;
        }

        public Criteria andOpIdGreaterThan(Integer value) {
            addCriterion("op_id >", value, "opId");
            return this;
        }

        public Criteria andOpIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("op_id >=", value, "opId");
            return this;
        }

        public Criteria andOpIdLessThan(Integer value) {
            addCriterion("op_id <", value, "opId");
            return this;
        }

        public Criteria andOpIdLessThanOrEqualTo(Integer value) {
            addCriterion("op_id <=", value, "opId");
            return this;
        }

        public Criteria andOpIdIn(List<Integer> values) {
            addCriterion("op_id in", values, "opId");
            return this;
        }

        public Criteria andOpIdNotIn(List<Integer> values) {
            addCriterion("op_id not in", values, "opId");
            return this;
        }

        public Criteria andOpIdBetween(Integer value1, Integer value2) {
            addCriterion("op_id between", value1, value2, "opId");
            return this;
        }

        public Criteria andOpIdNotBetween(Integer value1, Integer value2) {
            addCriterion("op_id not between", value1, value2, "opId");
            return this;
        }

        public Criteria andUsrIdIsNull() {
            addCriterion("usr_id is null");
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

        public Criteria andUsrNameIsNull() {
            addCriterion("usr_name is null");
            return this;
        }

        public Criteria andUsrNameIsNotNull() {
            addCriterion("usr_name is not null");
            return this;
        }

        public Criteria andUsrNameEqualTo(String value) {
            addCriterion("usr_name =", value, "usrName");
            return this;
        }

        public Criteria andUsrNameNotEqualTo(String value) {
            addCriterion("usr_name <>", value, "usrName");
            return this;
        }

        public Criteria andUsrNameGreaterThan(String value) {
            addCriterion("usr_name >", value, "usrName");
            return this;
        }

        public Criteria andUsrNameGreaterThanOrEqualTo(String value) {
            addCriterion("usr_name >=", value, "usrName");
            return this;
        }

        public Criteria andUsrNameLessThan(String value) {
            addCriterion("usr_name <", value, "usrName");
            return this;
        }

        public Criteria andUsrNameLessThanOrEqualTo(String value) {
            addCriterion("usr_name <=", value, "usrName");
            return this;
        }

        public Criteria andUsrNameLike(String value) {
            addCriterion("usr_name like", value, "usrName");
            return this;
        }

        public Criteria andUsrNameNotLike(String value) {
            addCriterion("usr_name not like", value, "usrName");
            return this;
        }

        public Criteria andUsrNameIn(List<String> values) {
            addCriterion("usr_name in", values, "usrName");
            return this;
        }

        public Criteria andUsrNameNotIn(List<String> values) {
            addCriterion("usr_name not in", values, "usrName");
            return this;
        }

        public Criteria andUsrNameBetween(String value1, String value2) {
            addCriterion("usr_name between", value1, value2, "usrName");
            return this;
        }

        public Criteria andUsrNameNotBetween(String value1, String value2) {
            addCriterion("usr_name not between", value1, value2, "usrName");
            return this;
        }

        public Criteria andOpTypeIsNull() {
            addCriterion("op_type is null");
            return this;
        }

        public Criteria andOpTypeIsNotNull() {
            addCriterion("op_type is not null");
            return this;
        }

        public Criteria andOpTypeEqualTo(String value) {
            addCriterion("op_type =", value, "opType");
            return this;
        }

        public Criteria andOpTypeNotEqualTo(String value) {
            addCriterion("op_type <>", value, "opType");
            return this;
        }

        public Criteria andOpTypeGreaterThan(String value) {
            addCriterion("op_type >", value, "opType");
            return this;
        }

        public Criteria andOpTypeGreaterThanOrEqualTo(String value) {
            addCriterion("op_type >=", value, "opType");
            return this;
        }

        public Criteria andOpTypeLessThan(String value) {
            addCriterion("op_type <", value, "opType");
            return this;
        }

        public Criteria andOpTypeLessThanOrEqualTo(String value) {
            addCriterion("op_type <=", value, "opType");
            return this;
        }

        public Criteria andOpTypeLike(String value) {
            addCriterion("op_type like", value, "opType");
            return this;
        }

        public Criteria andOpTypeNotLike(String value) {
            addCriterion("op_type not like", value, "opType");
            return this;
        }

        public Criteria andOpTypeIn(List<String> values) {
            addCriterion("op_type in", values, "opType");
            return this;
        }

        public Criteria andOpTypeNotIn(List<String> values) {
            addCriterion("op_type not in", values, "opType");
            return this;
        }

        public Criteria andOpTypeBetween(String value1, String value2) {
            addCriterion("op_type between", value1, value2, "opType");
            return this;
        }

        public Criteria andOpTypeNotBetween(String value1, String value2) {
            addCriterion("op_type not between", value1, value2, "opType");
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

        public Criteria andTabNameIsNull() {
            addCriterion("tab_name is null");
            return this;
        }

        public Criteria andTabNameIsNotNull() {
            addCriterion("tab_name is not null");
            return this;
        }

        public Criteria andTabNameEqualTo(String value) {
            addCriterion("tab_name =", value, "tabName");
            return this;
        }

        public Criteria andTabNameNotEqualTo(String value) {
            addCriterion("tab_name <>", value, "tabName");
            return this;
        }

        public Criteria andTabNameGreaterThan(String value) {
            addCriterion("tab_name >", value, "tabName");
            return this;
        }

        public Criteria andTabNameGreaterThanOrEqualTo(String value) {
            addCriterion("tab_name >=", value, "tabName");
            return this;
        }

        public Criteria andTabNameLessThan(String value) {
            addCriterion("tab_name <", value, "tabName");
            return this;
        }

        public Criteria andTabNameLessThanOrEqualTo(String value) {
            addCriterion("tab_name <=", value, "tabName");
            return this;
        }

        public Criteria andTabNameLike(String value) {
            addCriterion("tab_name like", value, "tabName");
            return this;
        }

        public Criteria andTabNameNotLike(String value) {
            addCriterion("tab_name not like", value, "tabName");
            return this;
        }

        public Criteria andTabNameIn(List<String> values) {
            addCriterion("tab_name in", values, "tabName");
            return this;
        }

        public Criteria andTabNameNotIn(List<String> values) {
            addCriterion("tab_name not in", values, "tabName");
            return this;
        }

        public Criteria andTabNameBetween(String value1, String value2) {
            addCriterion("tab_name between", value1, value2, "tabName");
            return this;
        }

        public Criteria andTabNameNotBetween(String value1, String value2) {
            addCriterion("tab_name not between", value1, value2, "tabName");
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

        public Criteria andRuntimeIsNull() {
            addCriterion("runtime is null");
            return this;
        }

        public Criteria andRuntimeIsNotNull() {
            addCriterion("runtime is not null");
            return this;
        }

        public Criteria andRuntimeEqualTo(Short value) {
            addCriterion("runtime =", value, "runtime");
            return this;
        }

        public Criteria andRuntimeNotEqualTo(Short value) {
            addCriterion("runtime <>", value, "runtime");
            return this;
        }

        public Criteria andRuntimeGreaterThan(Short value) {
            addCriterion("runtime >", value, "runtime");
            return this;
        }

        public Criteria andRuntimeGreaterThanOrEqualTo(Short value) {
            addCriterion("runtime >=", value, "runtime");
            return this;
        }

        public Criteria andRuntimeLessThan(Short value) {
            addCriterion("runtime <", value, "runtime");
            return this;
        }

        public Criteria andRuntimeLessThanOrEqualTo(Short value) {
            addCriterion("runtime <=", value, "runtime");
            return this;
        }

        public Criteria andRuntimeIn(List<Short> values) {
            addCriterion("runtime in", values, "runtime");
            return this;
        }

        public Criteria andRuntimeNotIn(List<Short> values) {
            addCriterion("runtime not in", values, "runtime");
            return this;
        }

        public Criteria andRuntimeBetween(Short value1, Short value2) {
            addCriterion("runtime between", value1, value2, "runtime");
            return this;
        }

        public Criteria andRuntimeNotBetween(Short value1, Short value2) {
            addCriterion("runtime not between", value1, value2, "runtime");
            return this;
        }

        public Criteria andMemoIsNull() {
            addCriterion("memo is null");
            return this;
        }

        public Criteria andMemoIsNotNull() {
            addCriterion("memo is not null");
            return this;
        }

        public Criteria andMemoEqualTo(String value) {
            addCriterion("memo =", value, "memo");
            return this;
        }

        public Criteria andMemoNotEqualTo(String value) {
            addCriterion("memo <>", value, "memo");
            return this;
        }

        public Criteria andMemoGreaterThan(String value) {
            addCriterion("memo >", value, "memo");
            return this;
        }

        public Criteria andMemoGreaterThanOrEqualTo(String value) {
            addCriterion("memo >=", value, "memo");
            return this;
        }

        public Criteria andMemoLessThan(String value) {
            addCriterion("memo <", value, "memo");
            return this;
        }

        public Criteria andMemoLessThanOrEqualTo(String value) {
            addCriterion("memo <=", value, "memo");
            return this;
        }

        public Criteria andMemoLike(String value) {
            addCriterion("memo like", value, "memo");
            return this;
        }

        public Criteria andMemoNotLike(String value) {
            addCriterion("memo not like", value, "memo");
            return this;
        }

        public Criteria andMemoIn(List<String> values) {
            addCriterion("memo in", values, "memo");
            return this;
        }

        public Criteria andMemoNotIn(List<String> values) {
            addCriterion("memo not in", values, "memo");
            return this;
        }

        public Criteria andMemoBetween(String value1, String value2) {
            addCriterion("memo between", value1, value2, "memo");
            return this;
        }

        public Criteria andMemoNotBetween(String value1, String value2) {
            addCriterion("memo not between", value1, value2, "memo");
            return this;
        }
    }
}
