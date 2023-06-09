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

package com.qlangtech.tis.trigger.biz.dal.pojo;

import com.qlangtech.tis.ibatis.BasicCriteria;

import java.util.*;

public class TaskCriteria extends BasicCriteria {
	protected String orderByClause;

	protected List<Criteria> oredCriteria;

	public TaskCriteria() {
		oredCriteria = new ArrayList<Criteria>();
	}

	protected TaskCriteria(TaskCriteria example) {
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
			return criteriaWithoutValue.size() > 0
					|| criteriaWithSingleValue.size() > 0
					|| criteriaWithListValue.size() > 0
					|| criteriaWithBetweenValue.size() > 0;
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

		protected void addCriterion(String condition, Object value,
				String property) {
			if (value == null) {
				throw new RuntimeException("Value for " + property
						+ " cannot be null");
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("condition", condition);
			map.put("value", value);
			criteriaWithSingleValue.add(map);
		}

		protected void addCriterion(String condition,
				List<? extends Object> values, String property) {
			if (values == null || values.size() == 0) {
				throw new RuntimeException("Value list for " + property
						+ " cannot be null or empty");
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("condition", condition);
			map.put("values", values);
			criteriaWithListValue.add(map);
		}

		protected void addCriterion(String condition, Object value1,
				Object value2, String property) {
			if (value1 == null || value2 == null) {
				throw new RuntimeException("Between values for " + property
						+ " cannot be null");
			}
			List<Object> list = new ArrayList<Object>();
			list.add(value1);
			list.add(value2);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("condition", condition);
			map.put("values", list);
			criteriaWithBetweenValue.add(map);
		}

		public Criteria andTaskIdIsNull() {
			addCriterion("task_id is null");
			return this;
		}

		public Criteria andTaskIdIsNotNull() {
			addCriterion("task_id is not null");
			return this;
		}

		public Criteria andTaskIdEqualTo(Long value) {
			addCriterion("task_id =", value, "taskId");
			return this;
		}

		public Criteria andTaskIdNotEqualTo(Long value) {
			addCriterion("task_id <>", value, "taskId");
			return this;
		}

		public Criteria andTaskIdGreaterThan(Long value) {
			addCriterion("task_id >", value, "taskId");
			return this;
		}

		public Criteria andTaskIdGreaterThanOrEqualTo(Long value) {
			addCriterion("task_id >=", value, "taskId");
			return this;
		}

		public Criteria andTaskIdLessThan(Long value) {
			addCriterion("task_id <", value, "taskId");
			return this;
		}

		public Criteria andTaskIdLessThanOrEqualTo(Long value) {
			addCriterion("task_id <=", value, "taskId");
			return this;
		}

		public Criteria andTaskIdIn(List<Long> values) {
			addCriterion("task_id in", values, "taskId");
			return this;
		}

		public Criteria andTaskIdNotIn(List<Long> values) {
			addCriterion("task_id not in", values, "taskId");
			return this;
		}

		public Criteria andTaskIdBetween(Long value1, Long value2) {
			addCriterion("task_id between", value1, value2, "taskId");
			return this;
		}

		public Criteria andTaskIdNotBetween(Long value1, Long value2) {
			addCriterion("task_id not between", value1, value2, "taskId");
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

		public Criteria andTriggerFromIsNull() {
			addCriterion("trigger_from is null");
			return this;
		}

		public Criteria andTriggerFromIsNotNull() {
			addCriterion("trigger_from is not null");
			return this;
		}

		public Criteria andTriggerFromEqualTo(String value) {
			addCriterion("trigger_from =", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromNotEqualTo(String value) {
			addCriterion("trigger_from <>", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromGreaterThan(String value) {
			addCriterion("trigger_from >", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromGreaterThanOrEqualTo(String value) {
			addCriterion("trigger_from >=", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromLessThan(String value) {
			addCriterion("trigger_from <", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromLessThanOrEqualTo(String value) {
			addCriterion("trigger_from <=", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromLike(String value) {
			addCriterion("trigger_from like", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromNotLike(String value) {
			addCriterion("trigger_from not like", value, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromIn(List<String> values) {
			addCriterion("trigger_from in", values, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromNotIn(List<String> values) {
			addCriterion("trigger_from not in", values, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromBetween(String value1, String value2) {
			addCriterion("trigger_from between", value1, value2, "triggerFrom");
			return this;
		}

		public Criteria andTriggerFromNotBetween(String value1, String value2) {
			addCriterion("trigger_from not between", value1, value2,
					"triggerFrom");
			return this;
		}

		public Criteria andExecStateIsNull() {
			addCriterion("exec_state is null");
			return this;
		}

		public Criteria andExecStateIsNotNull() {
			addCriterion("exec_state is not null");
			return this;
		}

		public Criteria andExecStateEqualTo(String value) {
			addCriterion("exec_state =", value, "execState");
			return this;
		}

		public Criteria andExecStateNotEqualTo(String value) {
			addCriterion("exec_state <>", value, "execState");
			return this;
		}

		public Criteria andExecStateGreaterThan(String value) {
			addCriterion("exec_state >", value, "execState");
			return this;
		}

		public Criteria andExecStateGreaterThanOrEqualTo(String value) {
			addCriterion("exec_state >=", value, "execState");
			return this;
		}

		public Criteria andExecStateLessThan(String value) {
			addCriterion("exec_state <", value, "execState");
			return this;
		}

		public Criteria andExecStateLessThanOrEqualTo(String value) {
			addCriterion("exec_state <=", value, "execState");
			return this;
		}

		public Criteria andExecStateLike(String value) {
			addCriterion("exec_state like", value, "execState");
			return this;
		}

		public Criteria andExecStateNotLike(String value) {
			addCriterion("exec_state not like", value, "execState");
			return this;
		}

		public Criteria andExecStateIn(List<String> values) {
			addCriterion("exec_state in", values, "execState");
			return this;
		}

		public Criteria andExecStateNotIn(List<String> values) {
			addCriterion("exec_state not in", values, "execState");
			return this;
		}

		public Criteria andExecStateBetween(String value1, String value2) {
			addCriterion("exec_state between", value1, value2, "execState");
			return this;
		}

		public Criteria andExecStateNotBetween(String value1, String value2) {
			addCriterion("exec_state not between", value1, value2, "execState");
			return this;
		}

		public Criteria andDomainIsNull() {
			addCriterion("domain is null");
			return this;
		}

		public Criteria andDomainIsNotNull() {
			addCriterion("domain is not null");
			return this;
		}

		public Criteria andDomainEqualTo(String value) {
			addCriterion("domain =", value, "domain");
			return this;
		}

		public Criteria andDomainNotEqualTo(String value) {
			addCriterion("domain <>", value, "domain");
			return this;
		}

		public Criteria andDomainGreaterThan(String value) {
			addCriterion("domain >", value, "domain");
			return this;
		}

		public Criteria andDomainGreaterThanOrEqualTo(String value) {
			addCriterion("domain >=", value, "domain");
			return this;
		}

		public Criteria andDomainLessThan(String value) {
			addCriterion("domain <", value, "domain");
			return this;
		}

		public Criteria andDomainLessThanOrEqualTo(String value) {
			addCriterion("domain <=", value, "domain");
			return this;
		}

		public Criteria andDomainLike(String value) {
			addCriterion("domain like", value, "domain");
			return this;
		}

		public Criteria andDomainNotLike(String value) {
			addCriterion("domain not like", value, "domain");
			return this;
		}

		public Criteria andDomainIn(List<String> values) {
			addCriterion("domain in", values, "domain");
			return this;
		}

		public Criteria andDomainNotIn(List<String> values) {
			addCriterion("domain not in", values, "domain");
			return this;
		}

		public Criteria andDomainBetween(String value1, String value2) {
			addCriterion("domain between", value1, value2, "domain");
			return this;
		}

		public Criteria andDomainNotBetween(String value1, String value2) {
			addCriterion("domain not between", value1, value2, "domain");
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

	private Date createTime;
//
		public void andGmtCreateInSameDay(Date createTime) {
			// addCriterion("datediff(gmt_create, now())", value, "gmtCreate");
			// return this;

			// addCriterion("gmt_create in", values, "gmtCreate");
			this.createTime = createTime;
		}
//		               gmtCreateInSameDay
		public Date getGmtCreateInSameDay(){
			return this.createTime;
		}
//
		public boolean isGmtCreateInSameDaySet() {

//			System.out.println("===================================");

			return createTime != null;
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
			addCriterion("gmt_modified not between", value1, value2,
					"gmtModified");
			return this;
		}

		 public Criteria andRuntimeEqualTo(String value) {
	            addCriterion("runtime =", value, "runtime");
	            return this;
	        }
	}
}
