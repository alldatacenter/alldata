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
public class ResourceParametersCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public ResourceParametersCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected ResourceParametersCriteria(ResourceParametersCriteria example) {
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

        public Criteria andRpIdIsNull() {
            addCriterion("rp_id is null");
            return this;
        }

        public Criteria andRpIdIsNotNull() {
            addCriterion("rp_id is not null");
            return this;
        }

        public Criteria andRpIdEqualTo(Long value) {
            addCriterion("rp_id =", value, "rpId");
            return this;
        }

        public Criteria andRpIdNotEqualTo(Long value) {
            addCriterion("rp_id <>", value, "rpId");
            return this;
        }

        public Criteria andRpIdGreaterThan(Long value) {
            addCriterion("rp_id >", value, "rpId");
            return this;
        }

        public Criteria andRpIdGreaterThanOrEqualTo(Long value) {
            addCriterion("rp_id >=", value, "rpId");
            return this;
        }

        public Criteria andRpIdLessThan(Long value) {
            addCriterion("rp_id <", value, "rpId");
            return this;
        }

        public Criteria andRpIdLessThanOrEqualTo(Long value) {
            addCriterion("rp_id <=", value, "rpId");
            return this;
        }

        public Criteria andRpIdIn(List<Long> values) {
            addCriterion("rp_id in", values, "rpId");
            return this;
        }

        public Criteria andRpIdNotIn(List<Long> values) {
            addCriterion("rp_id not in", values, "rpId");
            return this;
        }

        public Criteria andRpIdBetween(Long value1, Long value2) {
            addCriterion("rp_id between", value1, value2, "rpId");
            return this;
        }

        public Criteria andRpIdNotBetween(Long value1, Long value2) {
            addCriterion("rp_id not between", value1, value2, "rpId");
            return this;
        }

        public Criteria andKeyNameIsNull() {
            addCriterion("key_name is null");
            return this;
        }

        public Criteria andKeyNameIsNotNull() {
            addCriterion("key_name is not null");
            return this;
        }

        public Criteria andKeyNameEqualTo(String value) {
            addCriterion("key_name =", value, "keyName");
            return this;
        }

        public Criteria andKeyNameNotEqualTo(String value) {
            addCriterion("key_name <>", value, "keyName");
            return this;
        }

        public Criteria andKeyNameGreaterThan(String value) {
            addCriterion("key_name >", value, "keyName");
            return this;
        }

        public Criteria andKeyNameGreaterThanOrEqualTo(String value) {
            addCriterion("key_name >=", value, "keyName");
            return this;
        }

        public Criteria andKeyNameLessThan(String value) {
            addCriterion("key_name <", value, "keyName");
            return this;
        }

        public Criteria andKeyNameLessThanOrEqualTo(String value) {
            addCriterion("key_name <=", value, "keyName");
            return this;
        }

        public Criteria andKeyNameLike(String value) {
            addCriterion("key_name like", value, "keyName");
            return this;
        }

        public Criteria andKeyNameNotLike(String value) {
            addCriterion("key_name not like", value, "keyName");
            return this;
        }

        public Criteria andKeyNameIn(List<String> values) {
            addCriterion("key_name in", values, "keyName");
            return this;
        }

        public Criteria andKeyNameNotIn(List<String> values) {
            addCriterion("key_name not in", values, "keyName");
            return this;
        }

        public Criteria andKeyNameBetween(String value1, String value2) {
            addCriterion("key_name between", value1, value2, "keyName");
            return this;
        }

        public Criteria andKeyNameNotBetween(String value1, String value2) {
            addCriterion("key_name not between", value1, value2, "keyName");
            return this;
        }

        public Criteria andDailyValueIsNull() {
            addCriterion("daily_value is null");
            return this;
        }

        public Criteria andDailyValueIsNotNull() {
            addCriterion("daily_value is not null");
            return this;
        }

        public Criteria andDailyValueEqualTo(String value) {
            addCriterion("daily_value =", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueNotEqualTo(String value) {
            addCriterion("daily_value <>", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueGreaterThan(String value) {
            addCriterion("daily_value >", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueGreaterThanOrEqualTo(String value) {
            addCriterion("daily_value >=", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueLessThan(String value) {
            addCriterion("daily_value <", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueLessThanOrEqualTo(String value) {
            addCriterion("daily_value <=", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueLike(String value) {
            addCriterion("daily_value like", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueNotLike(String value) {
            addCriterion("daily_value not like", value, "dailyValue");
            return this;
        }

        public Criteria andDailyValueIn(List<String> values) {
            addCriterion("daily_value in", values, "dailyValue");
            return this;
        }

        public Criteria andDailyValueNotIn(List<String> values) {
            addCriterion("daily_value not in", values, "dailyValue");
            return this;
        }

        public Criteria andDailyValueBetween(String value1, String value2) {
            addCriterion("daily_value between", value1, value2, "dailyValue");
            return this;
        }

        public Criteria andDailyValueNotBetween(String value1, String value2) {
            addCriterion("daily_value not between", value1, value2, "dailyValue");
            return this;
        }

        public Criteria andReadyValueIsNull() {
            addCriterion("ready_value is null");
            return this;
        }

        public Criteria andReadyValueIsNotNull() {
            addCriterion("ready_value is not null");
            return this;
        }

        public Criteria andReadyValueEqualTo(String value) {
            addCriterion("ready_value =", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueNotEqualTo(String value) {
            addCriterion("ready_value <>", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueGreaterThan(String value) {
            addCriterion("ready_value >", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueGreaterThanOrEqualTo(String value) {
            addCriterion("ready_value >=", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueLessThan(String value) {
            addCriterion("ready_value <", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueLessThanOrEqualTo(String value) {
            addCriterion("ready_value <=", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueLike(String value) {
            addCriterion("ready_value like", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueNotLike(String value) {
            addCriterion("ready_value not like", value, "readyValue");
            return this;
        }

        public Criteria andReadyValueIn(List<String> values) {
            addCriterion("ready_value in", values, "readyValue");
            return this;
        }

        public Criteria andReadyValueNotIn(List<String> values) {
            addCriterion("ready_value not in", values, "readyValue");
            return this;
        }

        public Criteria andReadyValueBetween(String value1, String value2) {
            addCriterion("ready_value between", value1, value2, "readyValue");
            return this;
        }

        public Criteria andReadyValueNotBetween(String value1, String value2) {
            addCriterion("ready_value not between", value1, value2, "readyValue");
            return this;
        }

        public Criteria andOnlineValueIsNull() {
            addCriterion("online_value is null");
            return this;
        }

        public Criteria andOnlineValueIsNotNull() {
            addCriterion("online_value is not null");
            return this;
        }

        public Criteria andOnlineValueEqualTo(String value) {
            addCriterion("online_value =", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueNotEqualTo(String value) {
            addCriterion("online_value <>", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueGreaterThan(String value) {
            addCriterion("online_value >", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueGreaterThanOrEqualTo(String value) {
            addCriterion("online_value >=", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueLessThan(String value) {
            addCriterion("online_value <", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueLessThanOrEqualTo(String value) {
            addCriterion("online_value <=", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueLike(String value) {
            addCriterion("online_value like", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueNotLike(String value) {
            addCriterion("online_value not like", value, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueIn(List<String> values) {
            addCriterion("online_value in", values, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueNotIn(List<String> values) {
            addCriterion("online_value not in", values, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueBetween(String value1, String value2) {
            addCriterion("online_value between", value1, value2, "onlineValue");
            return this;
        }

        public Criteria andOnlineValueNotBetween(String value1, String value2) {
            addCriterion("online_value not between", value1, value2, "onlineValue");
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

        public Criteria andGmtUpdateIsNull() {
            addCriterion("gmt_update is null");
            return this;
        }

        public Criteria andGmtUpdateIsNotNull() {
            addCriterion("gmt_update is not null");
            return this;
        }

        public Criteria andGmtUpdateEqualTo(Date value) {
            addCriterion("gmt_update =", value, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateNotEqualTo(Date value) {
            addCriterion("gmt_update <>", value, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateGreaterThan(Date value) {
            addCriterion("gmt_update >", value, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_update >=", value, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateLessThan(Date value) {
            addCriterion("gmt_update <", value, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateLessThanOrEqualTo(Date value) {
            addCriterion("gmt_update <=", value, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateIn(List<Date> values) {
            addCriterion("gmt_update in", values, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateNotIn(List<Date> values) {
            addCriterion("gmt_update not in", values, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateBetween(Date value1, Date value2) {
            addCriterion("gmt_update between", value1, value2, "gmtUpdate");
            return this;
        }

        public Criteria andGmtUpdateNotBetween(Date value1, Date value2) {
            addCriterion("gmt_update not between", value1, value2, "gmtUpdate");
            return this;
        }
    }
}
