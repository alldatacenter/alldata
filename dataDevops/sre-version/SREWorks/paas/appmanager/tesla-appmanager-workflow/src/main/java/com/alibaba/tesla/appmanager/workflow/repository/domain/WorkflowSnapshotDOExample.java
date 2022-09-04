package com.alibaba.tesla.appmanager.workflow.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkflowSnapshotDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public WorkflowSnapshotDOExample() {
        oredCriteria = new ArrayList<>();
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
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
        orderByClause = null;
        distinct = false;
    }

    protected abstract static class GeneratedCriteria {
        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("id not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNull() {
            addCriterion("gmt_create is null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNotNull() {
            addCriterion("gmt_create is not null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateEqualTo(Date value) {
            addCriterion("gmt_create =", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotEqualTo(Date value) {
            addCriterion("gmt_create <>", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThan(Date value) {
            addCriterion("gmt_create >", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_create >=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThan(Date value) {
            addCriterion("gmt_create <", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThanOrEqualTo(Date value) {
            addCriterion("gmt_create <=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIn(List<Date> values) {
            addCriterion("gmt_create in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotIn(List<Date> values) {
            addCriterion("gmt_create not in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateBetween(Date value1, Date value2) {
            addCriterion("gmt_create between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotBetween(Date value1, Date value2) {
            addCriterion("gmt_create not between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNull() {
            addCriterion("gmt_modified is null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNotNull() {
            addCriterion("gmt_modified is not null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedEqualTo(Date value) {
            addCriterion("gmt_modified =", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotEqualTo(Date value) {
            addCriterion("gmt_modified <>", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThan(Date value) {
            addCriterion("gmt_modified >", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_modified >=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThan(Date value) {
            addCriterion("gmt_modified <", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) {
            addCriterion("gmt_modified <=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIn(List<Date> values) {
            addCriterion("gmt_modified in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotIn(List<Date> values) {
            addCriterion("gmt_modified not in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedBetween(Date value1, Date value2) {
            addCriterion("gmt_modified between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotBetween(Date value1, Date value2) {
            addCriterion("gmt_modified not between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdIsNull() {
            addCriterion("workflow_instance_id is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdIsNotNull() {
            addCriterion("workflow_instance_id is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdEqualTo(Long value) {
            addCriterion("workflow_instance_id =", value, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdNotEqualTo(Long value) {
            addCriterion("workflow_instance_id <>", value, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdGreaterThan(Long value) {
            addCriterion("workflow_instance_id >", value, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdGreaterThanOrEqualTo(Long value) {
            addCriterion("workflow_instance_id >=", value, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdLessThan(Long value) {
            addCriterion("workflow_instance_id <", value, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdLessThanOrEqualTo(Long value) {
            addCriterion("workflow_instance_id <=", value, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdIn(List<Long> values) {
            addCriterion("workflow_instance_id in", values, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdNotIn(List<Long> values) {
            addCriterion("workflow_instance_id not in", values, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdBetween(Long value1, Long value2) {
            addCriterion("workflow_instance_id between", value1, value2, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowInstanceIdNotBetween(Long value1, Long value2) {
            addCriterion("workflow_instance_id not between", value1, value2, "workflowInstanceId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdIsNull() {
            addCriterion("workflow_task_id is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdIsNotNull() {
            addCriterion("workflow_task_id is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdEqualTo(Long value) {
            addCriterion("workflow_task_id =", value, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdNotEqualTo(Long value) {
            addCriterion("workflow_task_id <>", value, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdGreaterThan(Long value) {
            addCriterion("workflow_task_id >", value, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdGreaterThanOrEqualTo(Long value) {
            addCriterion("workflow_task_id >=", value, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdLessThan(Long value) {
            addCriterion("workflow_task_id <", value, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdLessThanOrEqualTo(Long value) {
            addCriterion("workflow_task_id <=", value, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdIn(List<Long> values) {
            addCriterion("workflow_task_id in", values, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdNotIn(List<Long> values) {
            addCriterion("workflow_task_id not in", values, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdBetween(Long value1, Long value2) {
            addCriterion("workflow_task_id between", value1, value2, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTaskIdNotBetween(Long value1, Long value2) {
            addCriterion("workflow_task_id not between", value1, value2, "workflowTaskId");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextIsNull() {
            addCriterion("snapshot_context is null");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextIsNotNull() {
            addCriterion("snapshot_context is not null");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextEqualTo(String value) {
            addCriterion("snapshot_context =", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextNotEqualTo(String value) {
            addCriterion("snapshot_context <>", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextGreaterThan(String value) {
            addCriterion("snapshot_context >", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextGreaterThanOrEqualTo(String value) {
            addCriterion("snapshot_context >=", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextLessThan(String value) {
            addCriterion("snapshot_context <", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextLessThanOrEqualTo(String value) {
            addCriterion("snapshot_context <=", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextLike(String value) {
            addCriterion("snapshot_context like", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextNotLike(String value) {
            addCriterion("snapshot_context not like", value, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextIn(List<String> values) {
            addCriterion("snapshot_context in", values, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextNotIn(List<String> values) {
            addCriterion("snapshot_context not in", values, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextBetween(String value1, String value2) {
            addCriterion("snapshot_context between", value1, value2, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotContextNotBetween(String value1, String value2) {
            addCriterion("snapshot_context not between", value1, value2, "snapshotContext");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskIsNull() {
            addCriterion("snapshot_task is null");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskIsNotNull() {
            addCriterion("snapshot_task is not null");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskEqualTo(String value) {
            addCriterion("snapshot_task =", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskNotEqualTo(String value) {
            addCriterion("snapshot_task <>", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskGreaterThan(String value) {
            addCriterion("snapshot_task >", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskGreaterThanOrEqualTo(String value) {
            addCriterion("snapshot_task >=", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskLessThan(String value) {
            addCriterion("snapshot_task <", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskLessThanOrEqualTo(String value) {
            addCriterion("snapshot_task <=", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskLike(String value) {
            addCriterion("snapshot_task like", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskNotLike(String value) {
            addCriterion("snapshot_task not like", value, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskIn(List<String> values) {
            addCriterion("snapshot_task in", values, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskNotIn(List<String> values) {
            addCriterion("snapshot_task not in", values, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskBetween(String value1, String value2) {
            addCriterion("snapshot_task between", value1, value2, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotTaskNotBetween(String value1, String value2) {
            addCriterion("snapshot_task not between", value1, value2, "snapshotTask");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowIsNull() {
            addCriterion("snapshot_workflow is null");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowIsNotNull() {
            addCriterion("snapshot_workflow is not null");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowEqualTo(String value) {
            addCriterion("snapshot_workflow =", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowNotEqualTo(String value) {
            addCriterion("snapshot_workflow <>", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowGreaterThan(String value) {
            addCriterion("snapshot_workflow >", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowGreaterThanOrEqualTo(String value) {
            addCriterion("snapshot_workflow >=", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowLessThan(String value) {
            addCriterion("snapshot_workflow <", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowLessThanOrEqualTo(String value) {
            addCriterion("snapshot_workflow <=", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowLike(String value) {
            addCriterion("snapshot_workflow like", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowNotLike(String value) {
            addCriterion("snapshot_workflow not like", value, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowIn(List<String> values) {
            addCriterion("snapshot_workflow in", values, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowNotIn(List<String> values) {
            addCriterion("snapshot_workflow not in", values, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowBetween(String value1, String value2) {
            addCriterion("snapshot_workflow between", value1, value2, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andSnapshotWorkflowNotBetween(String value1, String value2) {
            addCriterion("snapshot_workflow not between", value1, value2, "snapshotWorkflow");
            return (Criteria) this;
        }

        public Criteria andLockVersionIsNull() {
            addCriterion("lock_version is null");
            return (Criteria) this;
        }

        public Criteria andLockVersionIsNotNull() {
            addCriterion("lock_version is not null");
            return (Criteria) this;
        }

        public Criteria andLockVersionEqualTo(Integer value) {
            addCriterion("lock_version =", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionNotEqualTo(Integer value) {
            addCriterion("lock_version <>", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionGreaterThan(Integer value) {
            addCriterion("lock_version >", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionGreaterThanOrEqualTo(Integer value) {
            addCriterion("lock_version >=", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionLessThan(Integer value) {
            addCriterion("lock_version <", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionLessThanOrEqualTo(Integer value) {
            addCriterion("lock_version <=", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionIn(List<Integer> values) {
            addCriterion("lock_version in", values, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionNotIn(List<Integer> values) {
            addCriterion("lock_version not in", values, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionBetween(Integer value1, Integer value2) {
            addCriterion("lock_version between", value1, value2, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionNotBetween(Integer value1, Integer value2) {
            addCriterion("lock_version not between", value1, value2, "lockVersion");
            return (Criteria) this;
        }
    }

    public static class Criteria extends GeneratedCriteria {

        protected Criteria() {
            super();
        }
    }

    public static class Criterion {
        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }
}