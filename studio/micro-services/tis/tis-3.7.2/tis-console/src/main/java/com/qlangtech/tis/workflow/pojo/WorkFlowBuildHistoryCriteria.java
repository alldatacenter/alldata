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
package com.qlangtech.tis.workflow.pojo;

import com.qlangtech.tis.manage.common.TISBaseCriteria;

import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class WorkFlowBuildHistoryCriteria extends TISBaseCriteria {

  protected String orderByClause;

  protected List<Criteria> oredCriteria;

  public WorkFlowBuildHistoryCriteria() {
    oredCriteria = new ArrayList<Criteria>();
  }

  protected WorkFlowBuildHistoryCriteria(WorkFlowBuildHistoryCriteria example) {
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

    public Criteria andIdEqualTo(Integer value) {
      addCriterion("id =", value, "id");
      return this;
    }

    public Criteria andLastVerEqualTo(Integer value) {
      addCriterion("last_ver =", value, "last_ver");
      return this;
    }

    public Criteria andIdNotEqualTo(Integer value) {
      addCriterion("id <>", value, "id");
      return this;
    }

    public Criteria andIdGreaterThan(Integer value) {
      addCriterion("id >", value, "id");
      return this;
    }

    public Criteria andIdGreaterThanOrEqualTo(Integer value) {
      addCriterion("id >=", value, "id");
      return this;
    }

    public Criteria andIdLessThan(Integer value) {
      addCriterion("id <", value, "id");
      return this;
    }

    public Criteria andIdLessThanOrEqualTo(Integer value) {
      addCriterion("id <=", value, "id");
      return this;
    }

    public Criteria andIdIn(List<Integer> values) {
      addCriterion("id in", values, "id");
      return this;
    }

    public Criteria andIdNotIn(List<Integer> values) {
      addCriterion("id not in", values, "id");
      return this;
    }

    public Criteria andIdBetween(Integer value1, Integer value2) {
      addCriterion("id between", value1, value2, "id");
      return this;
    }

    public Criteria andIdNotBetween(Integer value1, Integer value2) {
      addCriterion("id not between", value1, value2, "id");
      return this;
    }

    public Criteria andStartTimeIsNull() {
      addCriterion("start_time is null");
      return this;
    }

    public Criteria andStartTimeIsNotNull() {
      addCriterion("start_time is not null");
      return this;
    }

    public Criteria andStartTimeEqualTo(Date value) {
      addCriterion("start_time =", value, "startTime");
      return this;
    }

    public Criteria andStartTimeNotEqualTo(Date value) {
      addCriterion("start_time <>", value, "startTime");
      return this;
    }

    public Criteria andStartTimeGreaterThan(Date value) {
      addCriterion("start_time >", value, "startTime");
      return this;
    }

    public Criteria andStartTimeGreaterThanOrEqualTo(Date value) {
      addCriterion("start_time >=", value, "startTime");
      return this;
    }

    public Criteria andStartTimeLessThan(Date value) {
      addCriterion("start_time <", value, "startTime");
      return this;
    }

    public Criteria andStartTimeLessThanOrEqualTo(Date value) {
      addCriterion("start_time <=", value, "startTime");
      return this;
    }

    public Criteria andStartTimeIn(List<Date> values) {
      addCriterion("start_time in", values, "startTime");
      return this;
    }

    public Criteria andStartTimeNotIn(List<Date> values) {
      addCriterion("start_time not in", values, "startTime");
      return this;
    }

    public Criteria andStartTimeBetween(Date value1, Date value2) {
      addCriterion("start_time between", value1, value2, "startTime");
      return this;
    }

    public Criteria andStartTimeNotBetween(Date value1, Date value2) {
      addCriterion("start_time not between", value1, value2, "startTime");
      return this;
    }

    public Criteria andEndTimeIsNull() {
      addCriterion("end_time is null");
      return this;
    }

    public Criteria andEndTimeIsNotNull() {
      addCriterion("end_time is not null");
      return this;
    }

    public Criteria andEndTimeEqualTo(Date value) {
      addCriterion("end_time =", value, "endTime");
      return this;
    }

    public Criteria andEndTimeNotEqualTo(Date value) {
      addCriterion("end_time <>", value, "endTime");
      return this;
    }

    public Criteria andEndTimeGreaterThan(Date value) {
      addCriterion("end_time >", value, "endTime");
      return this;
    }

    public Criteria andEndTimeGreaterThanOrEqualTo(Date value) {
      addCriterion("end_time >=", value, "endTime");
      return this;
    }

    public Criteria andEndTimeLessThan(Date value) {
      addCriterion("end_time <", value, "endTime");
      return this;
    }

    public Criteria andEndTimeLessThanOrEqualTo(Date value) {
      addCriterion("end_time <=", value, "endTime");
      return this;
    }

    public Criteria andEndTimeIn(List<Date> values) {
      addCriterion("end_time in", values, "endTime");
      return this;
    }

    public Criteria andEndTimeNotIn(List<Date> values) {
      addCriterion("end_time not in", values, "endTime");
      return this;
    }

    public Criteria andEndTimeBetween(Date value1, Date value2) {
      addCriterion("end_time between", value1, value2, "endTime");
      return this;
    }

    public Criteria andEndTimeNotBetween(Date value1, Date value2) {
      addCriterion("end_time not between", value1, value2, "endTime");
      return this;
    }

    public Criteria andStateIsNull() {
      addCriterion("state is null");
      return this;
    }

    public Criteria andStateIsNotNull() {
      addCriterion("state is not null");
      return this;
    }

    public Criteria andStateEqualTo(Byte value) {
      addCriterion("state =", value, "state");
      return this;
    }

    public Criteria andStateNotEqualTo(Byte value) {
      addCriterion("state <>", value, "state");
      return this;
    }

    public Criteria andStateGreaterThan(Byte value) {
      addCriterion("state >", value, "state");
      return this;
    }

    public Criteria andStateGreaterThanOrEqualTo(Byte value) {
      addCriterion("state >=", value, "state");
      return this;
    }

    public Criteria andStateLessThan(Byte value) {
      addCriterion("state <", value, "state");
      return this;
    }

    public Criteria andStateLessThanOrEqualTo(Byte value) {
      addCriterion("state <=", value, "state");
      return this;
    }

    public Criteria andStateIn(List<Byte> values) {
      addCriterion("state in", values, "state");
      return this;
    }

    public Criteria andStateNotIn(List<Byte> values) {
      addCriterion("state not in", values, "state");
      return this;
    }

    public Criteria andStateBetween(Byte value1, Byte value2) {
      addCriterion("state between", value1, value2, "state");
      return this;
    }

    public Criteria andStateNotBetween(Byte value1, Byte value2) {
      addCriterion("state not between", value1, value2, "state");
      return this;
    }

    public Criteria andTriggerTypeIsNull() {
      addCriterion("trigger_type is null");
      return this;
    }

    public Criteria andTriggerTypeIsNotNull() {
      addCriterion("trigger_type is not null");
      return this;
    }

    public Criteria andTriggerTypeEqualTo(Boolean value) {
      addCriterion("trigger_type =", value, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeNotEqualTo(Boolean value) {
      addCriterion("trigger_type <>", value, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeGreaterThan(Boolean value) {
      addCriterion("trigger_type >", value, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeGreaterThanOrEqualTo(Boolean value) {
      addCriterion("trigger_type >=", value, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeLessThan(Boolean value) {
      addCriterion("trigger_type <", value, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeLessThanOrEqualTo(Boolean value) {
      addCriterion("trigger_type <=", value, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeIn(List<Boolean> values) {
      addCriterion("trigger_type in", values, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeNotIn(List<Boolean> values) {
      addCriterion("trigger_type not in", values, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeBetween(Boolean value1, Boolean value2) {
      addCriterion("trigger_type between", value1, value2, "triggerType");
      return this;
    }

    public Criteria andTriggerTypeNotBetween(Boolean value1, Boolean value2) {
      addCriterion("trigger_type not between", value1, value2, "triggerType");
      return this;
    }

    public Criteria andOpUserIdIsNull() {
      addCriterion("op_user_id is null");
      return this;
    }

    public Criteria andOpUserIdIsNotNull() {
      addCriterion("op_user_id is not null");
      return this;
    }

    public Criteria andOpUserIdEqualTo(Integer value) {
      addCriterion("op_user_id =", value, "opUserId");
      return this;
    }

    public Criteria andOpUserIdNotEqualTo(Integer value) {
      addCriterion("op_user_id <>", value, "opUserId");
      return this;
    }

    public Criteria andOpUserIdGreaterThan(Integer value) {
      addCriterion("op_user_id >", value, "opUserId");
      return this;
    }

    public Criteria andOpUserIdGreaterThanOrEqualTo(Integer value) {
      addCriterion("op_user_id >=", value, "opUserId");
      return this;
    }

    public Criteria andOpUserIdLessThan(Integer value) {
      addCriterion("op_user_id <", value, "opUserId");
      return this;
    }

    public Criteria andOpUserIdLessThanOrEqualTo(Integer value) {
      addCriterion("op_user_id <=", value, "opUserId");
      return this;
    }

    public Criteria andOpUserIdIn(List<Integer> values) {
      addCriterion("op_user_id in", values, "opUserId");
      return this;
    }

    public Criteria andOpUserIdNotIn(List<Integer> values) {
      addCriterion("op_user_id not in", values, "opUserId");
      return this;
    }

    public Criteria andOpUserIdBetween(Integer value1, Integer value2) {
      addCriterion("op_user_id between", value1, value2, "opUserId");
      return this;
    }

    public Criteria andOpUserIdNotBetween(Integer value1, Integer value2) {
      addCriterion("op_user_id not between", value1, value2, "opUserId");
      return this;
    }

    public Criteria andOpUserNameIsNull() {
      addCriterion("op_user_name is null");
      return this;
    }

    public Criteria andOpUserNameIsNotNull() {
      addCriterion("op_user_name is not null");
      return this;
    }

    public Criteria andOpUserNameEqualTo(String value) {
      addCriterion("op_user_name =", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameNotEqualTo(String value) {
      addCriterion("op_user_name <>", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameGreaterThan(String value) {
      addCriterion("op_user_name >", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameGreaterThanOrEqualTo(String value) {
      addCriterion("op_user_name >=", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameLessThan(String value) {
      addCriterion("op_user_name <", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameLessThanOrEqualTo(String value) {
      addCriterion("op_user_name <=", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameLike(String value) {
      addCriterion("op_user_name like", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameNotLike(String value) {
      addCriterion("op_user_name not like", value, "opUserName");
      return this;
    }

    public Criteria andOpUserNameIn(List<String> values) {
      addCriterion("op_user_name in", values, "opUserName");
      return this;
    }

    public Criteria andOpUserNameNotIn(List<String> values) {
      addCriterion("op_user_name not in", values, "opUserName");
      return this;
    }

    public Criteria andOpUserNameBetween(String value1, String value2) {
      addCriterion("op_user_name between", value1, value2, "opUserName");
      return this;
    }

    public Criteria andOpUserNameNotBetween(String value1, String value2) {
      addCriterion("op_user_name not between", value1, value2, "opUserName");
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

    public Criteria andStartPhaseIsNull() {
      addCriterion("start_phase is null");
      return this;
    }

    public Criteria andStartPhaseIsNotNull() {
      addCriterion("start_phase is not null");
      return this;
    }

    public Criteria andStartPhaseEqualTo(Byte value) {
      addCriterion("start_phase =", value, "startPhase");
      return this;
    }

    public Criteria andStartPhaseNotEqualTo(Byte value) {
      addCriterion("start_phase <>", value, "startPhase");
      return this;
    }

    public Criteria andStartPhaseGreaterThan(Byte value) {
      addCriterion("start_phase >", value, "startPhase");
      return this;
    }

    public Criteria andStartPhaseGreaterThanOrEqualTo(Byte value) {
      addCriterion("start_phase >=", value, "startPhase");
      return this;
    }

    public Criteria andStartPhaseLessThan(Byte value) {
      addCriterion("start_phase <", value, "startPhase");
      return this;
    }

    public Criteria andStartPhaseLessThanOrEqualTo(Byte value) {
      addCriterion("start_phase <=", value, "startPhase");
      return this;
    }

    public Criteria andStartPhaseIn(List<Byte> values) {
      addCriterion("start_phase in", values, "startPhase");
      return this;
    }

    public Criteria andStartPhaseNotIn(List<Byte> values) {
      addCriterion("start_phase not in", values, "startPhase");
      return this;
    }

    public Criteria andStartPhaseBetween(Byte value1, Byte value2) {
      addCriterion("start_phase between", value1, value2, "startPhase");
      return this;
    }

    public Criteria andStartPhaseNotBetween(Byte value1, Byte value2) {
      addCriterion("start_phase not between", value1, value2, "startPhase");
      return this;
    }

    public Criteria andHistoryIdIsNull() {
      addCriterion("history_id is null");
      return this;
    }

    public Criteria andHistoryIdIsNotNull() {
      addCriterion("history_id is not null");
      return this;
    }

    public Criteria andHistoryIdEqualTo(Integer value) {
      addCriterion("history_id =", value, "historyId");
      return this;
    }

    public Criteria andHistoryIdNotEqualTo(Integer value) {
      addCriterion("history_id <>", value, "historyId");
      return this;
    }

    public Criteria andHistoryIdGreaterThan(Integer value) {
      addCriterion("history_id >", value, "historyId");
      return this;
    }

    public Criteria andHistoryIdGreaterThanOrEqualTo(Integer value) {
      addCriterion("history_id >=", value, "historyId");
      return this;
    }

    public Criteria andHistoryIdLessThan(Integer value) {
      addCriterion("history_id <", value, "historyId");
      return this;
    }

    public Criteria andHistoryIdLessThanOrEqualTo(Integer value) {
      addCriterion("history_id <=", value, "historyId");
      return this;
    }

    public Criteria andHistoryIdIn(List<Integer> values) {
      addCriterion("history_id in", values, "historyId");
      return this;
    }

    public Criteria andHistoryIdNotIn(List<Integer> values) {
      addCriterion("history_id not in", values, "historyId");
      return this;
    }

    public Criteria andHistoryIdBetween(Integer value1, Integer value2) {
      addCriterion("history_id between", value1, value2, "historyId");
      return this;
    }

    public Criteria andHistoryIdNotBetween(Integer value1, Integer value2) {
      addCriterion("history_id not between", value1, value2, "historyId");
      return this;
    }

    public Criteria andWorkFlowIdIsNull() {
      addCriterion("work_flow_id is null");
      return this;
    }

    public Criteria andWorkFlowIdIsNotNull() {
      addCriterion("work_flow_id is not null");
      return this;
    }

    public Criteria andWorkFlowIdEqualTo(Integer value) {
      addCriterion("work_flow_id =", value, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdNotEqualTo(Integer value) {
      addCriterion("work_flow_id <>", value, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdGreaterThan(Integer value) {
      addCriterion("work_flow_id >", value, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdGreaterThanOrEqualTo(Integer value) {
      addCriterion("work_flow_id >=", value, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdLessThan(Integer value) {
      addCriterion("work_flow_id <", value, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdLessThanOrEqualTo(Integer value) {
      addCriterion("work_flow_id <=", value, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdIn(List<Integer> values) {
      addCriterion("work_flow_id in", values, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdNotIn(List<Integer> values) {
      addCriterion("work_flow_id not in", values, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdBetween(Integer value1, Integer value2) {
      addCriterion("work_flow_id between", value1, value2, "workFlowId");
      return this;
    }

    public Criteria andWorkFlowIdNotBetween(Integer value1, Integer value2) {
      addCriterion("work_flow_id not between", value1, value2, "workFlowId");
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

    public Criteria andOpTimeIsNull() {
      addCriterion("op_time is null");
      return this;
    }

    public Criteria andOpTimeIsNotNull() {
      addCriterion("op_time is not null");
      return this;
    }

    public Criteria andOpTimeEqualTo(Date value) {
      addCriterion("op_time =", value, "opTime");
      return this;
    }

    public Criteria andOpTimeNotEqualTo(Date value) {
      addCriterion("op_time <>", value, "opTime");
      return this;
    }

    public Criteria andOpTimeGreaterThan(Date value) {
      addCriterion("op_time >", value, "opTime");
      return this;
    }

    public Criteria andOpTimeGreaterThanOrEqualTo(Date value) {
      addCriterion("op_time >=", value, "opTime");
      return this;
    }

    public Criteria andOpTimeLessThan(Date value) {
      addCriterion("op_time <", value, "opTime");
      return this;
    }

    public Criteria andOpTimeLessThanOrEqualTo(Date value) {
      addCriterion("op_time <=", value, "opTime");
      return this;
    }

    public Criteria andOpTimeIn(List<Date> values) {
      addCriterion("op_time in", values, "opTime");
      return this;
    }

    public Criteria andOpTimeNotIn(List<Date> values) {
      addCriterion("op_time not in", values, "opTime");
      return this;
    }

    public Criteria andOpTimeBetween(Date value1, Date value2) {
      addCriterion("op_time between", value1, value2, "opTime");
      return this;
    }

    public Criteria andOpTimeNotBetween(Date value1, Date value2) {
      addCriterion("op_time not between", value1, value2, "opTime");
      return this;
    }

    public Criteria andEndPhaseIsNull() {
      addCriterion("end_phase is null");
      return this;
    }

    public Criteria andEndPhaseIsNotNull() {
      addCriterion("end_phase is not null");
      return this;
    }

    public Criteria andEndPhaseEqualTo(Byte value) {
      addCriterion("end_phase =", value, "endPhase");
      return this;
    }

    public Criteria andEndPhaseNotEqualTo(Byte value) {
      addCriterion("end_phase <>", value, "endPhase");
      return this;
    }

    public Criteria andEndPhaseGreaterThan(Byte value) {
      addCriterion("end_phase >", value, "endPhase");
      return this;
    }

    public Criteria andEndPhaseGreaterThanOrEqualTo(Byte value) {
      addCriterion("end_phase >=", value, "endPhase");
      return this;
    }

    public Criteria andEndPhaseLessThan(Byte value) {
      addCriterion("end_phase <", value, "endPhase");
      return this;
    }

    public Criteria andEndPhaseLessThanOrEqualTo(Byte value) {
      addCriterion("end_phase <=", value, "endPhase");
      return this;
    }

    public Criteria andEndPhaseIn(List<Byte> values) {
      addCriterion("end_phase in", values, "endPhase");
      return this;
    }

    public Criteria andEndPhaseNotIn(List<Byte> values) {
      addCriterion("end_phase not in", values, "endPhase");
      return this;
    }

    public Criteria andEndPhaseBetween(Byte value1, Byte value2) {
      addCriterion("end_phase between", value1, value2, "endPhase");
      return this;
    }

    public Criteria andEndPhaseNotBetween(Byte value1, Byte value2) {
      addCriterion("end_phase not between", value1, value2, "endPhase");
      return this;
    }
  }
}
