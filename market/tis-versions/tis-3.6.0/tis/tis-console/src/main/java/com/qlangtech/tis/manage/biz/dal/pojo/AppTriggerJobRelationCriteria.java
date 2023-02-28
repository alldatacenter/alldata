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
public class AppTriggerJobRelationCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public AppTriggerJobRelationCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected AppTriggerJobRelationCriteria(AppTriggerJobRelationCriteria example) {
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

        public Criteria andAtIdIsNull() {
            addCriterion("at_id is null");
            return this;
        }

        public Criteria andAtIdIsNotNull() {
            addCriterion("at_id is not null");
            return this;
        }

        public Criteria andAtIdEqualTo(Long value) {
            addCriterion("at_id =", value, "atId");
            return this;
        }

        public Criteria andAtIdNotEqualTo(Long value) {
            addCriterion("at_id <>", value, "atId");
            return this;
        }

        public Criteria andAtIdGreaterThan(Long value) {
            addCriterion("at_id >", value, "atId");
            return this;
        }

        public Criteria andAtIdGreaterThanOrEqualTo(Long value) {
            addCriterion("at_id >=", value, "atId");
            return this;
        }

        public Criteria andAtIdLessThan(Long value) {
            addCriterion("at_id <", value, "atId");
            return this;
        }

        public Criteria andAtIdLessThanOrEqualTo(Long value) {
            addCriterion("at_id <=", value, "atId");
            return this;
        }

        public Criteria andAtIdIn(List<Long> values) {
            addCriterion("at_id in", values, "atId");
            return this;
        }

        public Criteria andAtIdNotIn(List<Long> values) {
            addCriterion("at_id not in", values, "atId");
            return this;
        }

        public Criteria andAtIdBetween(Long value1, Long value2) {
            addCriterion("at_id between", value1, value2, "atId");
            return this;
        }

        public Criteria andAtIdNotBetween(Long value1, Long value2) {
            addCriterion("at_id not between", value1, value2, "atId");
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

        public Criteria andJobTypeIsNull() {
            addCriterion("job_type is null");
            return this;
        }

        public Criteria andJobTypeIsNotNull() {
            addCriterion("job_type is not null");
            return this;
        }

        public Criteria andJobTypeEqualTo(Byte value) {
            addCriterion("job_type =", value, "jobType");
            return this;
        }

        public Criteria andJobTypeNotEqualTo(Byte value) {
            addCriterion("job_type <>", value, "jobType");
            return this;
        }

        public Criteria andJobTypeGreaterThan(Byte value) {
            addCriterion("job_type >", value, "jobType");
            return this;
        }

        public Criteria andJobTypeGreaterThanOrEqualTo(Byte value) {
            addCriterion("job_type >=", value, "jobType");
            return this;
        }

        public Criteria andJobTypeLessThan(Byte value) {
            addCriterion("job_type <", value, "jobType");
            return this;
        }

        public Criteria andJobTypeLessThanOrEqualTo(Byte value) {
            addCriterion("job_type <=", value, "jobType");
            return this;
        }

        public Criteria andJobTypeIn(List<Byte> values) {
            addCriterion("job_type in", values, "jobType");
            return this;
        }

        public Criteria andJobTypeNotIn(List<Byte> values) {
            addCriterion("job_type not in", values, "jobType");
            return this;
        }

        public Criteria andJobTypeBetween(Byte value1, Byte value2) {
            addCriterion("job_type between", value1, value2, "jobType");
            return this;
        }

        public Criteria andJobTypeNotBetween(Byte value1, Byte value2) {
            addCriterion("job_type not between", value1, value2, "jobType");
            return this;
        }

        public Criteria andJobIdIsNull() {
            addCriterion("job_id is null");
            return this;
        }

        public Criteria andJobIdIsNotNull() {
            addCriterion("job_id is not null");
            return this;
        }

        public Criteria andJobIdEqualTo(Long value) {
            addCriterion("job_id =", value, "jobId");
            return this;
        }

        public Criteria andJobIdNotEqualTo(Long value) {
            addCriterion("job_id <>", value, "jobId");
            return this;
        }

        public Criteria andJobIdGreaterThan(Long value) {
            addCriterion("job_id >", value, "jobId");
            return this;
        }

        public Criteria andJobIdGreaterThanOrEqualTo(Long value) {
            addCriterion("job_id >=", value, "jobId");
            return this;
        }

        public Criteria andJobIdLessThan(Long value) {
            addCriterion("job_id <", value, "jobId");
            return this;
        }

        public Criteria andJobIdLessThanOrEqualTo(Long value) {
            addCriterion("job_id <=", value, "jobId");
            return this;
        }

        public Criteria andJobIdIn(List<Long> values) {
            addCriterion("job_id in", values, "jobId");
            return this;
        }

        public Criteria andJobIdNotIn(List<Long> values) {
            addCriterion("job_id not in", values, "jobId");
            return this;
        }

        public Criteria andJobIdBetween(Long value1, Long value2) {
            addCriterion("job_id between", value1, value2, "jobId");
            return this;
        }

        public Criteria andJobIdNotBetween(Long value1, Long value2) {
            addCriterion("job_id not between", value1, value2, "jobId");
            return this;
        }

        public Criteria andCrontabIsNull() {
            addCriterion("crontab is null");
            return this;
        }

        public Criteria andCrontabIsNotNull() {
            addCriterion("crontab is not null");
            return this;
        }

        public Criteria andCrontabEqualTo(String value) {
            addCriterion("crontab =", value, "crontab");
            return this;
        }

        public Criteria andCrontabNotEqualTo(String value) {
            addCriterion("crontab <>", value, "crontab");
            return this;
        }

        public Criteria andCrontabGreaterThan(String value) {
            addCriterion("crontab >", value, "crontab");
            return this;
        }

        public Criteria andCrontabGreaterThanOrEqualTo(String value) {
            addCriterion("crontab >=", value, "crontab");
            return this;
        }

        public Criteria andCrontabLessThan(String value) {
            addCriterion("crontab <", value, "crontab");
            return this;
        }

        public Criteria andCrontabLessThanOrEqualTo(String value) {
            addCriterion("crontab <=", value, "crontab");
            return this;
        }

        public Criteria andCrontabLike(String value) {
            addCriterion("crontab like", value, "crontab");
            return this;
        }

        public Criteria andCrontabNotLike(String value) {
            addCriterion("crontab not like", value, "crontab");
            return this;
        }

        public Criteria andCrontabIn(List<String> values) {
            addCriterion("crontab in", values, "crontab");
            return this;
        }

        public Criteria andCrontabNotIn(List<String> values) {
            addCriterion("crontab not in", values, "crontab");
            return this;
        }

        public Criteria andCrontabBetween(String value1, String value2) {
            addCriterion("crontab between", value1, value2, "crontab");
            return this;
        }

        public Criteria andCrontabNotBetween(String value1, String value2) {
            addCriterion("crontab not between", value1, value2, "crontab");
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

        public Criteria andIsStopIsNull() {
            addCriterion("is_stop is null");
            return this;
        }

        public Criteria andIsStopIsNotNull() {
            addCriterion("is_stop is not null");
            return this;
        }

        public Criteria andIsStopEqualTo(String value) {
            addCriterion("is_stop =", value, "isStop");
            return this;
        }

        public Criteria andIsStopNotEqualTo(String value) {
            addCriterion("is_stop <>", value, "isStop");
            return this;
        }

        public Criteria andIsStopGreaterThan(String value) {
            addCriterion("is_stop >", value, "isStop");
            return this;
        }

        public Criteria andIsStopGreaterThanOrEqualTo(String value) {
            addCriterion("is_stop >=", value, "isStop");
            return this;
        }

        public Criteria andIsStopLessThan(String value) {
            addCriterion("is_stop <", value, "isStop");
            return this;
        }

        public Criteria andIsStopLessThanOrEqualTo(String value) {
            addCriterion("is_stop <=", value, "isStop");
            return this;
        }

        public Criteria andIsStopLike(String value) {
            addCriterion("is_stop like", value, "isStop");
            return this;
        }

        public Criteria andIsStopNotLike(String value) {
            addCriterion("is_stop not like", value, "isStop");
            return this;
        }

        public Criteria andIsStopIn(List<String> values) {
            addCriterion("is_stop in", values, "isStop");
            return this;
        }

        public Criteria andIsStopNotIn(List<String> values) {
            addCriterion("is_stop not in", values, "isStop");
            return this;
        }

        public Criteria andIsStopBetween(String value1, String value2) {
            addCriterion("is_stop between", value1, value2, "isStop");
            return this;
        }

        public Criteria andIsStopNotBetween(String value1, String value2) {
            addCriterion("is_stop not between", value1, value2, "isStop");
            return this;
        }
    }
}
