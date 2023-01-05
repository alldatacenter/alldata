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
public class FuncCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public FuncCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected FuncCriteria(FuncCriteria example) {
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

        public Criteria andFunIdIsNull() {
            addCriterion("fun_id is null");
            return this;
        }

        public Criteria andFunIdIsNotNull() {
            addCriterion("fun_id is not null");
            return this;
        }

        public Criteria andFunIdEqualTo(Integer value) {
            addCriterion("fun_id =", value, "funId");
            return this;
        }

        public Criteria andFunIdNotEqualTo(Integer value) {
            addCriterion("fun_id <>", value, "funId");
            return this;
        }

        public Criteria andFunIdGreaterThan(Integer value) {
            addCriterion("fun_id >", value, "funId");
            return this;
        }

        public Criteria andFunIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("fun_id >=", value, "funId");
            return this;
        }

        public Criteria andFunIdLessThan(Integer value) {
            addCriterion("fun_id <", value, "funId");
            return this;
        }

        public Criteria andFunIdLessThanOrEqualTo(Integer value) {
            addCriterion("fun_id <=", value, "funId");
            return this;
        }

        public Criteria andFunIdIn(List<Integer> values) {
            addCriterion("fun_id in", values, "funId");
            return this;
        }

        public Criteria andFunIdNotIn(List<Integer> values) {
            addCriterion("fun_id not in", values, "funId");
            return this;
        }

        public Criteria andFunIdBetween(Integer value1, Integer value2) {
            addCriterion("fun_id between", value1, value2, "funId");
            return this;
        }

        public Criteria andFunIdNotBetween(Integer value1, Integer value2) {
            addCriterion("fun_id not between", value1, value2, "funId");
            return this;
        }

        public Criteria andFunKeyIsNull() {
            addCriterion("fun_key is null");
            return this;
        }

        public Criteria andFunKeyIsNotNull() {
            addCriterion("fun_key is not null");
            return this;
        }

        public Criteria andFunKeyEqualTo(String value) {
            addCriterion("fun_key =", value, "funKey");
            return this;
        }

        public Criteria andFunKeyNotEqualTo(String value) {
            addCriterion("fun_key <>", value, "funKey");
            return this;
        }

        public Criteria andFunKeyGreaterThan(String value) {
            addCriterion("fun_key >", value, "funKey");
            return this;
        }

        public Criteria andFunKeyGreaterThanOrEqualTo(String value) {
            addCriterion("fun_key >=", value, "funKey");
            return this;
        }

        public Criteria andFunKeyLessThan(String value) {
            addCriterion("fun_key <", value, "funKey");
            return this;
        }

        public Criteria andFunKeyLessThanOrEqualTo(String value) {
            addCriterion("fun_key <=", value, "funKey");
            return this;
        }

        public Criteria andFunKeyLike(String value) {
            addCriterion("fun_key like", value, "funKey");
            return this;
        }

        public Criteria andFunKeyNotLike(String value) {
            addCriterion("fun_key not like", value, "funKey");
            return this;
        }

        public Criteria andFunKeyIn(List<String> values) {
            addCriterion("fun_key in", values, "funKey");
            return this;
        }

        public Criteria andFunKeyNotIn(List<String> values) {
            addCriterion("fun_key not in", values, "funKey");
            return this;
        }

        public Criteria andFunKeyBetween(String value1, String value2) {
            addCriterion("fun_key between", value1, value2, "funKey");
            return this;
        }

        public Criteria andFunKeyNotBetween(String value1, String value2) {
            addCriterion("fun_key not between", value1, value2, "funKey");
            return this;
        }

        public Criteria andFuncNameIsNull() {
            addCriterion("func_name is null");
            return this;
        }

        public Criteria andFuncNameIsNotNull() {
            addCriterion("func_name is not null");
            return this;
        }

        public Criteria andFuncNameEqualTo(String value) {
            addCriterion("func_name =", value, "funcName");
            return this;
        }

        public Criteria andFuncNameNotEqualTo(String value) {
            addCriterion("func_name <>", value, "funcName");
            return this;
        }

        public Criteria andFuncNameGreaterThan(String value) {
            addCriterion("func_name >", value, "funcName");
            return this;
        }

        public Criteria andFuncNameGreaterThanOrEqualTo(String value) {
            addCriterion("func_name >=", value, "funcName");
            return this;
        }

        public Criteria andFuncNameLessThan(String value) {
            addCriterion("func_name <", value, "funcName");
            return this;
        }

        public Criteria andFuncNameLessThanOrEqualTo(String value) {
            addCriterion("func_name <=", value, "funcName");
            return this;
        }

        public Criteria andFuncNameLike(String value) {
            addCriterion("func_name like", value, "funcName");
            return this;
        }

        public Criteria andFuncNameNotLike(String value) {
            addCriterion("func_name not like", value, "funcName");
            return this;
        }

        public Criteria andFuncNameIn(List<String> values) {
            addCriterion("func_name in", values, "funcName");
            return this;
        }

        public Criteria andFuncNameNotIn(List<String> values) {
            addCriterion("func_name not in", values, "funcName");
            return this;
        }

        public Criteria andFuncNameBetween(String value1, String value2) {
            addCriterion("func_name between", value1, value2, "funcName");
            return this;
        }

        public Criteria andFuncNameNotBetween(String value1, String value2) {
            addCriterion("func_name not between", value1, value2, "funcName");
            return this;
        }

        public Criteria andGmtCreateIsNull() {
            addCriterion("gmt_create is null");
            return this;
        }

        public Criteria andGmtCreateIsNotNull() {
            addCriterion("gmt_create is not null");
            return this;
        }

        public Criteria andGmtCreateEqualTo(Date value) {
            addCriterion("gmt_create =", value, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateNotEqualTo(Date value) {
            addCriterion("gmt_create <>", value, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateGreaterThan(Date value) {
            addCriterion("gmt_create >", value, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_create >=", value, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateLessThan(Date value) {
            addCriterion("gmt_create <", value, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateLessThanOrEqualTo(Date value) {
            addCriterion("gmt_create <=", value, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateIn(List<Date> values) {
            addCriterion("gmt_create in", values, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateNotIn(List<Date> values) {
            addCriterion("gmt_create not in", values, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateBetween(Date value1, Date value2) {
            addCriterion("gmt_create between", value1, value2, "gmtCreate");
            return this;
        }

        public Criteria andGmtCreateNotBetween(Date value1, Date value2) {
            addCriterion("gmt_create not between", value1, value2, "gmtCreate");
            return this;
        }

        public Criteria andGmtModifiedIsNull() {
            addCriterion("gmt_modified is null");
            return this;
        }

        public Criteria andGmtModifiedIsNotNull() {
            addCriterion("gmt_modified is not null");
            return this;
        }

        public Criteria andGmtModifiedEqualTo(Date value) {
            addCriterion("gmt_modified =", value, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedNotEqualTo(Date value) {
            addCriterion("gmt_modified <>", value, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedGreaterThan(Date value) {
            addCriterion("gmt_modified >", value, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_modified >=", value, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedLessThan(Date value) {
            addCriterion("gmt_modified <", value, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) {
            addCriterion("gmt_modified <=", value, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedIn(List<Date> values) {
            addCriterion("gmt_modified in", values, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedNotIn(List<Date> values) {
            addCriterion("gmt_modified not in", values, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedBetween(Date value1, Date value2) {
            addCriterion("gmt_modified between", value1, value2, "gmtModified");
            return this;
        }

        public Criteria andGmtModifiedNotBetween(Date value1, Date value2) {
            addCriterion("gmt_modified not between", value1, value2, "gmtModified");
            return this;
        }

        public Criteria andFuncGroupKeyIsNull() {
            addCriterion("func_group_key is null");
            return this;
        }

        public Criteria andFuncGroupKeyIsNotNull() {
            addCriterion("func_group_key is not null");
            return this;
        }

        public Criteria andFuncGroupKeyEqualTo(Integer value) {
            addCriterion("func_group_key =", value, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyNotEqualTo(Integer value) {
            addCriterion("func_group_key <>", value, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyGreaterThan(Integer value) {
            addCriterion("func_group_key >", value, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyGreaterThanOrEqualTo(Integer value) {
            addCriterion("func_group_key >=", value, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyLessThan(Integer value) {
            addCriterion("func_group_key <", value, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyLessThanOrEqualTo(Integer value) {
            addCriterion("func_group_key <=", value, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyIn(List<Integer> values) {
            addCriterion("func_group_key in", values, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyNotIn(List<Integer> values) {
            addCriterion("func_group_key not in", values, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyBetween(Integer value1, Integer value2) {
            addCriterion("func_group_key between", value1, value2, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupKeyNotBetween(Integer value1, Integer value2) {
            addCriterion("func_group_key not between", value1, value2, "funcGroupKey");
            return this;
        }

        public Criteria andFuncGroupNameIsNull() {
            addCriterion("func_group_name is null");
            return this;
        }

        public Criteria andFuncGroupNameIsNotNull() {
            addCriterion("func_group_name is not null");
            return this;
        }

        public Criteria andFuncGroupNameEqualTo(String value) {
            addCriterion("func_group_name =", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameNotEqualTo(String value) {
            addCriterion("func_group_name <>", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameGreaterThan(String value) {
            addCriterion("func_group_name >", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameGreaterThanOrEqualTo(String value) {
            addCriterion("func_group_name >=", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameLessThan(String value) {
            addCriterion("func_group_name <", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameLessThanOrEqualTo(String value) {
            addCriterion("func_group_name <=", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameLike(String value) {
            addCriterion("func_group_name like", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameNotLike(String value) {
            addCriterion("func_group_name not like", value, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameIn(List<String> values) {
            addCriterion("func_group_name in", values, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameNotIn(List<String> values) {
            addCriterion("func_group_name not in", values, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameBetween(String value1, String value2) {
            addCriterion("func_group_name between", value1, value2, "funcGroupName");
            return this;
        }

        public Criteria andFuncGroupNameNotBetween(String value1, String value2) {
            addCriterion("func_group_name not between", value1, value2, "funcGroupName");
            return this;
        }
    }
}
