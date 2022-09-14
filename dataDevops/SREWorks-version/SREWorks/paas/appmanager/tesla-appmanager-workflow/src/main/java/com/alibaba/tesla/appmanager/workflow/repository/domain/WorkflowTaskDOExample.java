package com.alibaba.tesla.appmanager.workflow.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkflowTaskDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public WorkflowTaskDOExample() {
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

        public Criteria andAppIdIsNull() {
            addCriterion("app_id is null");
            return (Criteria) this;
        }

        public Criteria andAppIdIsNotNull() {
            addCriterion("app_id is not null");
            return (Criteria) this;
        }

        public Criteria andAppIdEqualTo(String value) {
            addCriterion("app_id =", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotEqualTo(String value) {
            addCriterion("app_id <>", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdGreaterThan(String value) {
            addCriterion("app_id >", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdGreaterThanOrEqualTo(String value) {
            addCriterion("app_id >=", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdLessThan(String value) {
            addCriterion("app_id <", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdLessThanOrEqualTo(String value) {
            addCriterion("app_id <=", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdLike(String value) {
            addCriterion("app_id like", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotLike(String value) {
            addCriterion("app_id not like", value, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdIn(List<String> values) {
            addCriterion("app_id in", values, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotIn(List<String> values) {
            addCriterion("app_id not in", values, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdBetween(String value1, String value2) {
            addCriterion("app_id between", value1, value2, "appId");
            return (Criteria) this;
        }

        public Criteria andAppIdNotBetween(String value1, String value2) {
            addCriterion("app_id not between", value1, value2, "appId");
            return (Criteria) this;
        }

        public Criteria andGmtStartIsNull() {
            addCriterion("gmt_start is null");
            return (Criteria) this;
        }

        public Criteria andGmtStartIsNotNull() {
            addCriterion("gmt_start is not null");
            return (Criteria) this;
        }

        public Criteria andGmtStartEqualTo(Date value) {
            addCriterion("gmt_start =", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartNotEqualTo(Date value) {
            addCriterion("gmt_start <>", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartGreaterThan(Date value) {
            addCriterion("gmt_start >", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_start >=", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartLessThan(Date value) {
            addCriterion("gmt_start <", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartLessThanOrEqualTo(Date value) {
            addCriterion("gmt_start <=", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartIn(List<Date> values) {
            addCriterion("gmt_start in", values, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartNotIn(List<Date> values) {
            addCriterion("gmt_start not in", values, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartBetween(Date value1, Date value2) {
            addCriterion("gmt_start between", value1, value2, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartNotBetween(Date value1, Date value2) {
            addCriterion("gmt_start not between", value1, value2, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtEndIsNull() {
            addCriterion("gmt_end is null");
            return (Criteria) this;
        }

        public Criteria andGmtEndIsNotNull() {
            addCriterion("gmt_end is not null");
            return (Criteria) this;
        }

        public Criteria andGmtEndEqualTo(Date value) {
            addCriterion("gmt_end =", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndNotEqualTo(Date value) {
            addCriterion("gmt_end <>", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndGreaterThan(Date value) {
            addCriterion("gmt_end >", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_end >=", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndLessThan(Date value) {
            addCriterion("gmt_end <", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndLessThanOrEqualTo(Date value) {
            addCriterion("gmt_end <=", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndIn(List<Date> values) {
            addCriterion("gmt_end in", values, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndNotIn(List<Date> values) {
            addCriterion("gmt_end not in", values, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndBetween(Date value1, Date value2) {
            addCriterion("gmt_end between", value1, value2, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndNotBetween(Date value1, Date value2) {
            addCriterion("gmt_end not between", value1, value2, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIsNull() {
            addCriterion("task_type is null");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIsNotNull() {
            addCriterion("task_type is not null");
            return (Criteria) this;
        }

        public Criteria andTaskTypeEqualTo(String value) {
            addCriterion("task_type =", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotEqualTo(String value) {
            addCriterion("task_type <>", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeGreaterThan(String value) {
            addCriterion("task_type >", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeGreaterThanOrEqualTo(String value) {
            addCriterion("task_type >=", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLessThan(String value) {
            addCriterion("task_type <", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLessThanOrEqualTo(String value) {
            addCriterion("task_type <=", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLike(String value) {
            addCriterion("task_type like", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotLike(String value) {
            addCriterion("task_type not like", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIn(List<String> values) {
            addCriterion("task_type in", values, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotIn(List<String> values) {
            addCriterion("task_type not in", values, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeBetween(String value1, String value2) {
            addCriterion("task_type between", value1, value2, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotBetween(String value1, String value2) {
            addCriterion("task_type not between", value1, value2, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskStageIsNull() {
            addCriterion("task_stage is null");
            return (Criteria) this;
        }

        public Criteria andTaskStageIsNotNull() {
            addCriterion("task_stage is not null");
            return (Criteria) this;
        }

        public Criteria andTaskStageEqualTo(String value) {
            addCriterion("task_stage =", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageNotEqualTo(String value) {
            addCriterion("task_stage <>", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageGreaterThan(String value) {
            addCriterion("task_stage >", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageGreaterThanOrEqualTo(String value) {
            addCriterion("task_stage >=", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageLessThan(String value) {
            addCriterion("task_stage <", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageLessThanOrEqualTo(String value) {
            addCriterion("task_stage <=", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageLike(String value) {
            addCriterion("task_stage like", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageNotLike(String value) {
            addCriterion("task_stage not like", value, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageIn(List<String> values) {
            addCriterion("task_stage in", values, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageNotIn(List<String> values) {
            addCriterion("task_stage not in", values, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageBetween(String value1, String value2) {
            addCriterion("task_stage between", value1, value2, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStageNotBetween(String value1, String value2) {
            addCriterion("task_stage not between", value1, value2, "taskStage");
            return (Criteria) this;
        }

        public Criteria andTaskStatusIsNull() {
            addCriterion("task_status is null");
            return (Criteria) this;
        }

        public Criteria andTaskStatusIsNotNull() {
            addCriterion("task_status is not null");
            return (Criteria) this;
        }

        public Criteria andTaskStatusEqualTo(String value) {
            addCriterion("task_status =", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotEqualTo(String value) {
            addCriterion("task_status <>", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusGreaterThan(String value) {
            addCriterion("task_status >", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusGreaterThanOrEqualTo(String value) {
            addCriterion("task_status >=", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLessThan(String value) {
            addCriterion("task_status <", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLessThanOrEqualTo(String value) {
            addCriterion("task_status <=", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLike(String value) {
            addCriterion("task_status like", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotLike(String value) {
            addCriterion("task_status not like", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusIn(List<String> values) {
            addCriterion("task_status in", values, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotIn(List<String> values) {
            addCriterion("task_status not in", values, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusBetween(String value1, String value2) {
            addCriterion("task_status between", value1, value2, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotBetween(String value1, String value2) {
            addCriterion("task_status not between", value1, value2, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdIsNull() {
            addCriterion("deploy_app_id is null");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdIsNotNull() {
            addCriterion("deploy_app_id is not null");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdEqualTo(Long value) {
            addCriterion("deploy_app_id =", value, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdNotEqualTo(Long value) {
            addCriterion("deploy_app_id <>", value, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdGreaterThan(Long value) {
            addCriterion("deploy_app_id >", value, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdGreaterThanOrEqualTo(Long value) {
            addCriterion("deploy_app_id >=", value, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdLessThan(Long value) {
            addCriterion("deploy_app_id <", value, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdLessThanOrEqualTo(Long value) {
            addCriterion("deploy_app_id <=", value, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdIn(List<Long> values) {
            addCriterion("deploy_app_id in", values, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdNotIn(List<Long> values) {
            addCriterion("deploy_app_id not in", values, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdBetween(Long value1, Long value2) {
            addCriterion("deploy_app_id between", value1, value2, "deployAppId");
            return (Criteria) this;
        }

        public Criteria andDeployAppIdNotBetween(Long value1, Long value2) {
            addCriterion("deploy_app_id not between", value1, value2, "deployAppId");
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

        public Criteria andDeployAppUnitIdIsNull() {
            addCriterion("deploy_app_unit_id is null");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdIsNotNull() {
            addCriterion("deploy_app_unit_id is not null");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdEqualTo(String value) {
            addCriterion("deploy_app_unit_id =", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdNotEqualTo(String value) {
            addCriterion("deploy_app_unit_id <>", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdGreaterThan(String value) {
            addCriterion("deploy_app_unit_id >", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdGreaterThanOrEqualTo(String value) {
            addCriterion("deploy_app_unit_id >=", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdLessThan(String value) {
            addCriterion("deploy_app_unit_id <", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdLessThanOrEqualTo(String value) {
            addCriterion("deploy_app_unit_id <=", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdLike(String value) {
            addCriterion("deploy_app_unit_id like", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdNotLike(String value) {
            addCriterion("deploy_app_unit_id not like", value, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdIn(List<String> values) {
            addCriterion("deploy_app_unit_id in", values, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdNotIn(List<String> values) {
            addCriterion("deploy_app_unit_id not in", values, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdBetween(String value1, String value2) {
            addCriterion("deploy_app_unit_id between", value1, value2, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppUnitIdNotBetween(String value1, String value2) {
            addCriterion("deploy_app_unit_id not between", value1, value2, "deployAppUnitId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdIsNull() {
            addCriterion("deploy_app_namespace_id is null");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdIsNotNull() {
            addCriterion("deploy_app_namespace_id is not null");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdEqualTo(String value) {
            addCriterion("deploy_app_namespace_id =", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdNotEqualTo(String value) {
            addCriterion("deploy_app_namespace_id <>", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdGreaterThan(String value) {
            addCriterion("deploy_app_namespace_id >", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdGreaterThanOrEqualTo(String value) {
            addCriterion("deploy_app_namespace_id >=", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdLessThan(String value) {
            addCriterion("deploy_app_namespace_id <", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdLessThanOrEqualTo(String value) {
            addCriterion("deploy_app_namespace_id <=", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdLike(String value) {
            addCriterion("deploy_app_namespace_id like", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdNotLike(String value) {
            addCriterion("deploy_app_namespace_id not like", value, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdIn(List<String> values) {
            addCriterion("deploy_app_namespace_id in", values, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdNotIn(List<String> values) {
            addCriterion("deploy_app_namespace_id not in", values, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdBetween(String value1, String value2) {
            addCriterion("deploy_app_namespace_id between", value1, value2, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppNamespaceIdNotBetween(String value1, String value2) {
            addCriterion("deploy_app_namespace_id not between", value1, value2, "deployAppNamespaceId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdIsNull() {
            addCriterion("deploy_app_stage_id is null");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdIsNotNull() {
            addCriterion("deploy_app_stage_id is not null");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdEqualTo(String value) {
            addCriterion("deploy_app_stage_id =", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdNotEqualTo(String value) {
            addCriterion("deploy_app_stage_id <>", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdGreaterThan(String value) {
            addCriterion("deploy_app_stage_id >", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdGreaterThanOrEqualTo(String value) {
            addCriterion("deploy_app_stage_id >=", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdLessThan(String value) {
            addCriterion("deploy_app_stage_id <", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdLessThanOrEqualTo(String value) {
            addCriterion("deploy_app_stage_id <=", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdLike(String value) {
            addCriterion("deploy_app_stage_id like", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdNotLike(String value) {
            addCriterion("deploy_app_stage_id not like", value, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdIn(List<String> values) {
            addCriterion("deploy_app_stage_id in", values, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdNotIn(List<String> values) {
            addCriterion("deploy_app_stage_id not in", values, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdBetween(String value1, String value2) {
            addCriterion("deploy_app_stage_id between", value1, value2, "deployAppStageId");
            return (Criteria) this;
        }

        public Criteria andDeployAppStageIdNotBetween(String value1, String value2) {
            addCriterion("deploy_app_stage_id not between", value1, value2, "deployAppStageId");
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