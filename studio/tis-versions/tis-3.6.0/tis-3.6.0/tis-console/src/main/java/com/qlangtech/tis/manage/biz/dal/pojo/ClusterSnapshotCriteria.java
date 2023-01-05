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
public class ClusterSnapshotCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public ClusterSnapshotCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected ClusterSnapshotCriteria(ClusterSnapshotCriteria example) {
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

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
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

        public Criteria andServiceNameIsNull() {
            addCriterion("service_name is null");
            return this;
        }

        public Criteria andServiceNameIsNotNull() {
            addCriterion("service_name is not null");
            return this;
        }

        // public Criteria andServiceNameEqualTo(String value) {
        // addCriterion("service_name =", value, "serviceName");
        // return this;
        // }
        //
        // public Criteria andServiceNameNotEqualTo(String value) {
        // addCriterion("service_name <>", value, "serviceName");
        // return this;
        // }
        public Criteria andServiceNameGreaterThan(String value) {
            addCriterion("service_name >", value, "serviceName");
            return this;
        }

        public Criteria andServiceNameGreaterThanOrEqualTo(String value) {
            addCriterion("service_name >=", value, "serviceName");
            return this;
        }

        public Criteria andServiceNameLessThan(String value) {
            addCriterion("service_name <", value, "serviceName");
            return this;
        }

        public Criteria andServiceNameLessThanOrEqualTo(String value) {
            addCriterion("service_name <=", value, "serviceName");
            return this;
        }

        public Criteria andServiceNameLike(String value) {
            addCriterion("service_name like", value, "serviceName");
            return this;
        }

        public Criteria andServiceNameNotLike(String value) {
            addCriterion("service_name not like", value, "serviceName");
            return this;
        }

        public Criteria andServiceNameIn(List<String> values) {
            addCriterion("service_name in", values, "serviceName");
            return this;
        }

        public Criteria andServiceNameNotIn(List<String> values) {
            addCriterion("service_name not in", values, "serviceName");
            return this;
        }

        public Criteria andServiceNameBetween(String value1, String value2) {
            addCriterion("service_name between", value1, value2, "serviceName");
            return this;
        }

        public Criteria andServiceNameNotBetween(String value1, String value2) {
            addCriterion("service_name not between", value1, value2, "serviceName");
            return this;
        }

        public Criteria andQpsIsNull() {
            addCriterion("qps is null");
            return this;
        }

        public Criteria andQpsIsNotNull() {
            addCriterion("qps is not null");
            return this;
        }

        public Criteria andQpsEqualTo(Integer value) {
            addCriterion("qps =", value, "qps");
            return this;
        }

        public Criteria andQpsNotEqualTo(Integer value) {
            addCriterion("qps <>", value, "qps");
            return this;
        }

        public Criteria andQpsGreaterThan(Integer value) {
            addCriterion("qps >", value, "qps");
            return this;
        }

        public Criteria andQpsGreaterThanOrEqualTo(Integer value) {
            addCriterion("qps >=", value, "qps");
            return this;
        }

        public Criteria andQpsLessThan(Integer value) {
            addCriterion("qps <", value, "qps");
            return this;
        }

        public Criteria andQpsLessThanOrEqualTo(Integer value) {
            addCriterion("qps <=", value, "qps");
            return this;
        }

        public Criteria andQpsIn(List<Integer> values) {
            addCriterion("qps in", values, "qps");
            return this;
        }

        public Criteria andQpsNotIn(List<Integer> values) {
            addCriterion("qps not in", values, "qps");
            return this;
        }

        public Criteria andQpsBetween(Integer value1, Integer value2) {
            addCriterion("qps between", value1, value2, "qps");
            return this;
        }

        public Criteria andQpsNotBetween(Integer value1, Integer value2) {
            addCriterion("qps not between", value1, value2, "qps");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestIsNull() {
            addCriterion("avg_consume_time_per_request is null");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestIsNotNull() {
            addCriterion("avg_consume_time_per_request is not null");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestEqualTo(Float value) {
            addCriterion("avg_consume_time_per_request =", value, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestNotEqualTo(Float value) {
            addCriterion("avg_consume_time_per_request <>", value, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestGreaterThan(Float value) {
            addCriterion("avg_consume_time_per_request >", value, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestGreaterThanOrEqualTo(Float value) {
            addCriterion("avg_consume_time_per_request >=", value, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestLessThan(Float value) {
            addCriterion("avg_consume_time_per_request <", value, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestLessThanOrEqualTo(Float value) {
            addCriterion("avg_consume_time_per_request <=", value, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestIn(List<Float> values) {
            addCriterion("avg_consume_time_per_request in", values, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestNotIn(List<Float> values) {
            addCriterion("avg_consume_time_per_request not in", values, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestBetween(Float value1, Float value2) {
            addCriterion("avg_consume_time_per_request between", value1, value2, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andAvgConsumeTimePerRequestNotBetween(Float value1, Float value2) {
            addCriterion("avg_consume_time_per_request not between", value1, value2, "avgConsumeTimePerRequest");
            return this;
        }

        public Criteria andRequestCountIsNull() {
            addCriterion("request_count is null");
            return this;
        }

        public Criteria andRequestCountIsNotNull() {
            addCriterion("request_count is not null");
            return this;
        }

        public Criteria andRequestCountEqualTo(Long value) {
            addCriterion("request_count =", value, "requestCount");
            return this;
        }

        public Criteria andRequestCountNotEqualTo(Long value) {
            addCriterion("request_count <>", value, "requestCount");
            return this;
        }

        public Criteria andRequestCountGreaterThan(Long value) {
            addCriterion("request_count >", value, "requestCount");
            return this;
        }

        public Criteria andRequestCountGreaterThanOrEqualTo(Long value) {
            addCriterion("request_count >=", value, "requestCount");
            return this;
        }

        public Criteria andRequestCountLessThan(Long value) {
            addCriterion("request_count <", value, "requestCount");
            return this;
        }

        public Criteria andRequestCountLessThanOrEqualTo(Long value) {
            addCriterion("request_count <=", value, "requestCount");
            return this;
        }

        public Criteria andRequestCountIn(List<Long> values) {
            addCriterion("request_count in", values, "requestCount");
            return this;
        }

        public Criteria andRequestCountNotIn(List<Long> values) {
            addCriterion("request_count not in", values, "requestCount");
            return this;
        }

        public Criteria andRequestCountBetween(Long value1, Long value2) {
            addCriterion("request_count between", value1, value2, "requestCount");
            return this;
        }

        public Criteria andRequestCountNotBetween(Long value1, Long value2) {
            addCriterion("request_count not between", value1, value2, "requestCount");
            return this;
        }

        public Criteria andDocNumberIsNull() {
            addCriterion("doc_number is null");
            return this;
        }

        public Criteria andDocNumberIsNotNull() {
            addCriterion("doc_number is not null");
            return this;
        }

        public Criteria andDocNumberEqualTo(Long value) {
            addCriterion("doc_number =", value, "docNumber");
            return this;
        }

        public Criteria andDocNumberNotEqualTo(Long value) {
            addCriterion("doc_number <>", value, "docNumber");
            return this;
        }

        public Criteria andDocNumberGreaterThan(Long value) {
            addCriterion("doc_number >", value, "docNumber");
            return this;
        }

        public Criteria andDocNumberGreaterThanOrEqualTo(Long value) {
            addCriterion("doc_number >=", value, "docNumber");
            return this;
        }

        public Criteria andDocNumberLessThan(Long value) {
            addCriterion("doc_number <", value, "docNumber");
            return this;
        }

        public Criteria andDocNumberLessThanOrEqualTo(Long value) {
            addCriterion("doc_number <=", value, "docNumber");
            return this;
        }

        public Criteria andDocNumberIn(List<Long> values) {
            addCriterion("doc_number in", values, "docNumber");
            return this;
        }

        public Criteria andDocNumberNotIn(List<Long> values) {
            addCriterion("doc_number not in", values, "docNumber");
            return this;
        }

        public Criteria andDocNumberBetween(Long value1, Long value2) {
            addCriterion("doc_number between", value1, value2, "docNumber");
            return this;
        }

        public Criteria andDocNumberNotBetween(Long value1, Long value2) {
            addCriterion("doc_number not between", value1, value2, "docNumber");
            return this;
        }
    }
}
