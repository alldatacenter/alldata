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

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class BizDomainCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public BizDomainCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected BizDomainCriteria(BizDomainCriteria example) {
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

        public Criteria andBizIdIsNull() {
            addCriterion("biz_id is null");
            return this;
        }

        public Criteria andBizIdIsNotNull() {
            addCriterion("biz_id is not null");
            return this;
        }

        public Criteria andBizIdEqualTo(Integer value) {
            addCriterion("biz_id =", value, "bizId");
            return this;
        }

        public Criteria andBizIdNotEqualTo(Integer value) {
            addCriterion("biz_id <>", value, "bizId");
            return this;
        }

        public Criteria andBizIdGreaterThan(Integer value) {
            addCriterion("biz_id >", value, "bizId");
            return this;
        }

        // baisui add has not been delete start
        public Criteria andNotDelete() {
            addCriterion("biz_domain.is_deleted =", "N", "biz_domain.is_deleted");
            return this;
        }

        // baisui add has not been delete end
        public Criteria andBizIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("biz_id >=", value, "bizId");
            return this;
        }

        public Criteria andBizIdLessThan(Integer value) {
            addCriterion("biz_id <", value, "bizId");
            return this;
        }

        public Criteria andBizIdLessThanOrEqualTo(Integer value) {
            addCriterion("biz_id <=", value, "bizId");
            return this;
        }

        public Criteria andBizIdIn(List<Integer> values) {
            addCriterion("biz_id in", values, "bizId");
            return this;
        }

        public Criteria andBizIdNotIn(List<Integer> values) {
            addCriterion("biz_id not in", values, "bizId");
            return this;
        }

        public Criteria andBizIdBetween(Integer value1, Integer value2) {
            addCriterion("biz_id between", value1, value2, "bizId");
            return this;
        }

        public Criteria andBizIdNotBetween(Integer value1, Integer value2) {
            addCriterion("biz_id not between", value1, value2, "bizId");
            return this;
        }

        public Criteria andNameIsNull() {
            addCriterion("name is null");
            return this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("name is not null");
            return this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("name =", value, "name");
            return this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("name <>", value, "name");
            return this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("name >", value, "name");
            return this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("name >=", value, "name");
            return this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("name <", value, "name");
            return this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("name <=", value, "name");
            return this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("name like", value, "name");
            return this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("name not like", value, "name");
            return this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("name in", values, "name");
            return this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("name not in", values, "name");
            return this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("name between", value1, value2, "name");
            return this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("name not between", value1, value2, "name");
            return this;
        }

        public Criteria andDumpTypeIsNull() {
            addCriterion("dump_type is null");
            return this;
        }

        public Criteria andDumpTypeIsNotNull() {
            addCriterion("dump_type is not null");
            return this;
        }

        public Criteria andDumpTypeEqualTo(Short value) {
            addCriterion("dump_type =", value, "dumpType");
            return this;
        }

        public Criteria andDumpTypeNotEqualTo(Short value) {
            addCriterion("dump_type <>", value, "dumpType");
            return this;
        }

        public Criteria andDumpTypeGreaterThan(Short value) {
            addCriterion("dump_type >", value, "dumpType");
            return this;
        }

        public Criteria andDumpTypeGreaterThanOrEqualTo(Short value) {
            addCriterion("dump_type >=", value, "dumpType");
            return this;
        }

        public Criteria andDumpTypeLessThan(Short value) {
            addCriterion("dump_type <", value, "dumpType");
            return this;
        }

        public Criteria andDumpTypeLessThanOrEqualTo(Short value) {
            addCriterion("dump_type <=", value, "dumpType");
            return this;
        }

        public Criteria andDumpTypeIn(List<Short> values) {
            addCriterion("dump_type in", values, "dumpType");
            return this;
        }

        public Criteria andDumpTypeNotIn(List<Short> values) {
            addCriterion("dump_type not in", values, "dumpType");
            return this;
        }

        public Criteria andDumpTypeBetween(Short value1, Short value2) {
            addCriterion("dump_type between", value1, value2, "dumpType");
            return this;
        }

        public Criteria andDumpTypeNotBetween(Short value1, Short value2) {
            addCriterion("dump_type not between", value1, value2, "dumpType");
            return this;
        }

        public Criteria andBizLineIsNull() {
            addCriterion("biz_line is null");
            return this;
        }

        public Criteria andBizLineIsNotNull() {
            addCriterion("biz_line is not null");
            return this;
        }

        public Criteria andBizLineEqualTo(String value) {
            addCriterion("biz_line =", value, "bizLine");
            return this;
        }

        public Criteria andBizLineNotEqualTo(String value) {
            addCriterion("biz_line <>", value, "bizLine");
            return this;
        }

        public Criteria andBizLineGreaterThan(String value) {
            addCriterion("biz_line >", value, "bizLine");
            return this;
        }

        public Criteria andBizLineGreaterThanOrEqualTo(String value) {
            addCriterion("biz_line >=", value, "bizLine");
            return this;
        }

        public Criteria andBizLineLessThan(String value) {
            addCriterion("biz_line <", value, "bizLine");
            return this;
        }

        public Criteria andBizLineLessThanOrEqualTo(String value) {
            addCriterion("biz_line <=", value, "bizLine");
            return this;
        }

        public Criteria andBizLineLike(String value) {
            addCriterion("biz_line like", value, "bizLine");
            return this;
        }

        public Criteria andBizLineNotLike(String value) {
            addCriterion("biz_line not like", value, "bizLine");
            return this;
        }

        public Criteria andBizLineIn(List<String> values) {
            addCriterion("biz_line in", values, "bizLine");
            return this;
        }

        public Criteria andBizLineNotIn(List<String> values) {
            addCriterion("biz_line not in", values, "bizLine");
            return this;
        }

        public Criteria andBizLineBetween(String value1, String value2) {
            addCriterion("biz_line between", value1, value2, "bizLine");
            return this;
        }

        public Criteria andBizLineNotBetween(String value1, String value2) {
            addCriterion("biz_line not between", value1, value2, "bizLine");
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
    }
}
