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
package com.qlangtech.tis.dataplatform.pojo;

import com.qlangtech.tis.manage.common.TISBaseCriteria;
import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DsTableCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public DsTableCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected DsTableCriteria(DsTableCriteria example) {
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

        public Criteria andTabIdIsNull() {
            addCriterion("tab_id is null");
            return this;
        }

        public Criteria andTabIdIsNotNull() {
            addCriterion("tab_id is not null");
            return this;
        }

        public Criteria andTabIdEqualTo(Long value) {
            addCriterion("tab_id =", value, "tabId");
            return this;
        }

        public Criteria andTabIdNotEqualTo(Long value) {
            addCriterion("tab_id <>", value, "tabId");
            return this;
        }

        public Criteria andTabIdGreaterThan(Long value) {
            addCriterion("tab_id >", value, "tabId");
            return this;
        }

        public Criteria andTabIdGreaterThanOrEqualTo(Long value) {
            addCriterion("tab_id >=", value, "tabId");
            return this;
        }

        public Criteria andTabIdLessThan(Long value) {
            addCriterion("tab_id <", value, "tabId");
            return this;
        }

        public Criteria andTabIdLessThanOrEqualTo(Long value) {
            addCriterion("tab_id <=", value, "tabId");
            return this;
        }

        public Criteria andTabIdIn(List<Long> values) {
            addCriterion("tab_id in", values, "tabId");
            return this;
        }

        public Criteria andTabIdNotIn(List<Long> values) {
            addCriterion("tab_id not in", values, "tabId");
            return this;
        }

        public Criteria andTabIdBetween(Long value1, Long value2) {
            addCriterion("tab_id between", value1, value2, "tabId");
            return this;
        }

        public Criteria andTabIdNotBetween(Long value1, Long value2) {
            addCriterion("tab_id not between", value1, value2, "tabId");
            return this;
        }

        public Criteria andDsIdIsNull() {
            addCriterion("ds_id is null");
            return this;
        }

        public Criteria andDsIdIsNotNull() {
            addCriterion("ds_id is not null");
            return this;
        }

        public Criteria andDsIdEqualTo(Long value) {
            addCriterion("ds_id =", value, "dsId");
            return this;
        }

        public Criteria andDsIdNotEqualTo(Long value) {
            addCriterion("ds_id <>", value, "dsId");
            return this;
        }

        public Criteria andDsIdGreaterThan(Long value) {
            addCriterion("ds_id >", value, "dsId");
            return this;
        }

        public Criteria andDsIdGreaterThanOrEqualTo(Long value) {
            addCriterion("ds_id >=", value, "dsId");
            return this;
        }

        public Criteria andDsIdLessThan(Long value) {
            addCriterion("ds_id <", value, "dsId");
            return this;
        }

        public Criteria andDsIdLessThanOrEqualTo(Long value) {
            addCriterion("ds_id <=", value, "dsId");
            return this;
        }

        public Criteria andDsIdIn(List<Long> values) {
            addCriterion("ds_id in", values, "dsId");
            return this;
        }

        public Criteria andDsIdNotIn(List<Long> values) {
            addCriterion("ds_id not in", values, "dsId");
            return this;
        }

        public Criteria andDsIdBetween(Long value1, Long value2) {
            addCriterion("ds_id between", value1, value2, "dsId");
            return this;
        }

        public Criteria andDsIdNotBetween(Long value1, Long value2) {
            addCriterion("ds_id not between", value1, value2, "dsId");
            return this;
        }

        public Criteria andDsNameIsNull() {
            addCriterion("ds_name is null");
            return this;
        }

        public Criteria andDsNameIsNotNull() {
            addCriterion("ds_name is not null");
            return this;
        }

        public Criteria andDsNameEqualTo(String value) {
            addCriterion("ds_name =", value, "dsName");
            return this;
        }

        public Criteria andDsNameNotEqualTo(String value) {
            addCriterion("ds_name <>", value, "dsName");
            return this;
        }

        public Criteria andDsNameGreaterThan(String value) {
            addCriterion("ds_name >", value, "dsName");
            return this;
        }

        public Criteria andDsNameGreaterThanOrEqualTo(String value) {
            addCriterion("ds_name >=", value, "dsName");
            return this;
        }

        public Criteria andDsNameLessThan(String value) {
            addCriterion("ds_name <", value, "dsName");
            return this;
        }

        public Criteria andDsNameLessThanOrEqualTo(String value) {
            addCriterion("ds_name <=", value, "dsName");
            return this;
        }

        public Criteria andDsNameLike(String value) {
            addCriterion("ds_name like", value, "dsName");
            return this;
        }

        public Criteria andDsNameNotLike(String value) {
            addCriterion("ds_name not like", value, "dsName");
            return this;
        }

        public Criteria andDsNameIn(List<String> values) {
            addCriterion("ds_name in", values, "dsName");
            return this;
        }

        public Criteria andDsNameNotIn(List<String> values) {
            addCriterion("ds_name not in", values, "dsName");
            return this;
        }

        public Criteria andDsNameBetween(String value1, String value2) {
            addCriterion("ds_name between", value1, value2, "dsName");
            return this;
        }

        public Criteria andDsNameNotBetween(String value1, String value2) {
            addCriterion("ds_name not between", value1, value2, "dsName");
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

        public Criteria andTabAliasIsNull() {
            addCriterion("tab_alias is null");
            return this;
        }

        public Criteria andTabAliasIsNotNull() {
            addCriterion("tab_alias is not null");
            return this;
        }

        public Criteria andTabAliasEqualTo(String value) {
            addCriterion("tab_alias =", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasNotEqualTo(String value) {
            addCriterion("tab_alias <>", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasGreaterThan(String value) {
            addCriterion("tab_alias >", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasGreaterThanOrEqualTo(String value) {
            addCriterion("tab_alias >=", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasLessThan(String value) {
            addCriterion("tab_alias <", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasLessThanOrEqualTo(String value) {
            addCriterion("tab_alias <=", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasLike(String value) {
            addCriterion("tab_alias like", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasNotLike(String value) {
            addCriterion("tab_alias not like", value, "tabAlias");
            return this;
        }

        public Criteria andTabAliasIn(List<String> values) {
            addCriterion("tab_alias in", values, "tabAlias");
            return this;
        }

        public Criteria andTabAliasNotIn(List<String> values) {
            addCriterion("tab_alias not in", values, "tabAlias");
            return this;
        }

        public Criteria andTabAliasBetween(String value1, String value2) {
            addCriterion("tab_alias between", value1, value2, "tabAlias");
            return this;
        }

        public Criteria andTabAliasNotBetween(String value1, String value2) {
            addCriterion("tab_alias not between", value1, value2, "tabAlias");
            return this;
        }

        public Criteria andModifyTimeIsNull() {
            addCriterion("modify_time is null");
            return this;
        }

        public Criteria andModifyTimeIsNotNull() {
            addCriterion("modify_time is not null");
            return this;
        }

        public Criteria andModifyTimeEqualTo(Date value) {
            addCriterion("modify_time =", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeNotEqualTo(Date value) {
            addCriterion("modify_time <>", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeGreaterThan(Date value) {
            addCriterion("modify_time >", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("modify_time >=", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeLessThan(Date value) {
            addCriterion("modify_time <", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeLessThanOrEqualTo(Date value) {
            addCriterion("modify_time <=", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeIn(List<Date> values) {
            addCriterion("modify_time in", values, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeNotIn(List<Date> values) {
            addCriterion("modify_time not in", values, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeBetween(Date value1, Date value2) {
            addCriterion("modify_time between", value1, value2, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeNotBetween(Date value1, Date value2) {
            addCriterion("modify_time not between", value1, value2, "modifyTime");
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
    }
}
