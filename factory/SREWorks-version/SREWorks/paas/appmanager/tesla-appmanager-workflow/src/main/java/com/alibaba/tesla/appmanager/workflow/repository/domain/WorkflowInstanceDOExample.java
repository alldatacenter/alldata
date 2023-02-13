package com.alibaba.tesla.appmanager.workflow.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkflowInstanceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public WorkflowInstanceDOExample() {
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

        public Criteria andWorkflowStatusIsNull() {
            addCriterion("workflow_status is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusIsNotNull() {
            addCriterion("workflow_status is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusEqualTo(String value) {
            addCriterion("workflow_status =", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusNotEqualTo(String value) {
            addCriterion("workflow_status <>", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusGreaterThan(String value) {
            addCriterion("workflow_status >", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusGreaterThanOrEqualTo(String value) {
            addCriterion("workflow_status >=", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusLessThan(String value) {
            addCriterion("workflow_status <", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusLessThanOrEqualTo(String value) {
            addCriterion("workflow_status <=", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusLike(String value) {
            addCriterion("workflow_status like", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusNotLike(String value) {
            addCriterion("workflow_status not like", value, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusIn(List<String> values) {
            addCriterion("workflow_status in", values, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusNotIn(List<String> values) {
            addCriterion("workflow_status not in", values, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusBetween(String value1, String value2) {
            addCriterion("workflow_status between", value1, value2, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowStatusNotBetween(String value1, String value2) {
            addCriterion("workflow_status not between", value1, value2, "workflowStatus");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256IsNull() {
            addCriterion("workflow_sha256 is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256IsNotNull() {
            addCriterion("workflow_sha256 is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256EqualTo(String value) {
            addCriterion("workflow_sha256 =", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256NotEqualTo(String value) {
            addCriterion("workflow_sha256 <>", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256GreaterThan(String value) {
            addCriterion("workflow_sha256 >", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256GreaterThanOrEqualTo(String value) {
            addCriterion("workflow_sha256 >=", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256LessThan(String value) {
            addCriterion("workflow_sha256 <", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256LessThanOrEqualTo(String value) {
            addCriterion("workflow_sha256 <=", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256Like(String value) {
            addCriterion("workflow_sha256 like", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256NotLike(String value) {
            addCriterion("workflow_sha256 not like", value, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256In(List<String> values) {
            addCriterion("workflow_sha256 in", values, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256NotIn(List<String> values) {
            addCriterion("workflow_sha256 not in", values, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256Between(String value1, String value2) {
            addCriterion("workflow_sha256 between", value1, value2, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowSha256NotBetween(String value1, String value2) {
            addCriterion("workflow_sha256 not between", value1, value2, "workflowSha256");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorIsNull() {
            addCriterion("workflow_creator is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorIsNotNull() {
            addCriterion("workflow_creator is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorEqualTo(String value) {
            addCriterion("workflow_creator =", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorNotEqualTo(String value) {
            addCriterion("workflow_creator <>", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorGreaterThan(String value) {
            addCriterion("workflow_creator >", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorGreaterThanOrEqualTo(String value) {
            addCriterion("workflow_creator >=", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorLessThan(String value) {
            addCriterion("workflow_creator <", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorLessThanOrEqualTo(String value) {
            addCriterion("workflow_creator <=", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorLike(String value) {
            addCriterion("workflow_creator like", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorNotLike(String value) {
            addCriterion("workflow_creator not like", value, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorIn(List<String> values) {
            addCriterion("workflow_creator in", values, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorNotIn(List<String> values) {
            addCriterion("workflow_creator not in", values, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorBetween(String value1, String value2) {
            addCriterion("workflow_creator between", value1, value2, "workflowCreator");
            return (Criteria) this;
        }

        public Criteria andWorkflowCreatorNotBetween(String value1, String value2) {
            addCriterion("workflow_creator not between", value1, value2, "workflowCreator");
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

        public Criteria andClientHostIsNull() {
            addCriterion("client_host is null");
            return (Criteria) this;
        }

        public Criteria andClientHostIsNotNull() {
            addCriterion("client_host is not null");
            return (Criteria) this;
        }

        public Criteria andClientHostEqualTo(String value) {
            addCriterion("client_host =", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostNotEqualTo(String value) {
            addCriterion("client_host <>", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostGreaterThan(String value) {
            addCriterion("client_host >", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostGreaterThanOrEqualTo(String value) {
            addCriterion("client_host >=", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostLessThan(String value) {
            addCriterion("client_host <", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostLessThanOrEqualTo(String value) {
            addCriterion("client_host <=", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostLike(String value) {
            addCriterion("client_host like", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostNotLike(String value) {
            addCriterion("client_host not like", value, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostIn(List<String> values) {
            addCriterion("client_host in", values, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostNotIn(List<String> values) {
            addCriterion("client_host not in", values, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostBetween(String value1, String value2) {
            addCriterion("client_host between", value1, value2, "clientHost");
            return (Criteria) this;
        }

        public Criteria andClientHostNotBetween(String value1, String value2) {
            addCriterion("client_host not between", value1, value2, "clientHost");
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