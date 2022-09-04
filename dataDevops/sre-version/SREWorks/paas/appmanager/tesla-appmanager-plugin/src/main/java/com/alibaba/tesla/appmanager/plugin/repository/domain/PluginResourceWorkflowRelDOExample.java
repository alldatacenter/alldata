package com.alibaba.tesla.appmanager.plugin.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PluginResourceWorkflowRelDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public PluginResourceWorkflowRelDOExample() {
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

        public Criteria andPluginResourceIdIsNull() {
            addCriterion("plugin_resource_id is null");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdIsNotNull() {
            addCriterion("plugin_resource_id is not null");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdEqualTo(Long value) {
            addCriterion("plugin_resource_id =", value, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdNotEqualTo(Long value) {
            addCriterion("plugin_resource_id <>", value, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdGreaterThan(Long value) {
            addCriterion("plugin_resource_id >", value, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdGreaterThanOrEqualTo(Long value) {
            addCriterion("plugin_resource_id >=", value, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdLessThan(Long value) {
            addCriterion("plugin_resource_id <", value, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdLessThanOrEqualTo(Long value) {
            addCriterion("plugin_resource_id <=", value, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdIn(List<Long> values) {
            addCriterion("plugin_resource_id in", values, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdNotIn(List<Long> values) {
            addCriterion("plugin_resource_id not in", values, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdBetween(Long value1, Long value2) {
            addCriterion("plugin_resource_id between", value1, value2, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andPluginResourceIdNotBetween(Long value1, Long value2) {
            addCriterion("plugin_resource_id not between", value1, value2, "pluginResourceId");
            return (Criteria) this;
        }

        public Criteria andClusterIdIsNull() {
            addCriterion("cluster_id is null");
            return (Criteria) this;
        }

        public Criteria andClusterIdIsNotNull() {
            addCriterion("cluster_id is not null");
            return (Criteria) this;
        }

        public Criteria andClusterIdEqualTo(String value) {
            addCriterion("cluster_id =", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotEqualTo(String value) {
            addCriterion("cluster_id <>", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdGreaterThan(String value) {
            addCriterion("cluster_id >", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdGreaterThanOrEqualTo(String value) {
            addCriterion("cluster_id >=", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdLessThan(String value) {
            addCriterion("cluster_id <", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdLessThanOrEqualTo(String value) {
            addCriterion("cluster_id <=", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdLike(String value) {
            addCriterion("cluster_id like", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotLike(String value) {
            addCriterion("cluster_id not like", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdIn(List<String> values) {
            addCriterion("cluster_id in", values, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotIn(List<String> values) {
            addCriterion("cluster_id not in", values, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdBetween(String value1, String value2) {
            addCriterion("cluster_id between", value1, value2, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotBetween(String value1, String value2) {
            addCriterion("cluster_id not between", value1, value2, "clusterId");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeIsNull() {
            addCriterion("workflow_type is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeIsNotNull() {
            addCriterion("workflow_type is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeEqualTo(String value) {
            addCriterion("workflow_type =", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeNotEqualTo(String value) {
            addCriterion("workflow_type <>", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeGreaterThan(String value) {
            addCriterion("workflow_type >", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeGreaterThanOrEqualTo(String value) {
            addCriterion("workflow_type >=", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeLessThan(String value) {
            addCriterion("workflow_type <", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeLessThanOrEqualTo(String value) {
            addCriterion("workflow_type <=", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeLike(String value) {
            addCriterion("workflow_type like", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeNotLike(String value) {
            addCriterion("workflow_type not like", value, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeIn(List<String> values) {
            addCriterion("workflow_type in", values, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeNotIn(List<String> values) {
            addCriterion("workflow_type not in", values, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeBetween(String value1, String value2) {
            addCriterion("workflow_type between", value1, value2, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowTypeNotBetween(String value1, String value2) {
            addCriterion("workflow_type not between", value1, value2, "workflowType");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdIsNull() {
            addCriterion("workflow_id is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdIsNotNull() {
            addCriterion("workflow_id is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdEqualTo(Long value) {
            addCriterion("workflow_id =", value, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdNotEqualTo(Long value) {
            addCriterion("workflow_id <>", value, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdGreaterThan(Long value) {
            addCriterion("workflow_id >", value, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdGreaterThanOrEqualTo(Long value) {
            addCriterion("workflow_id >=", value, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdLessThan(Long value) {
            addCriterion("workflow_id <", value, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdLessThanOrEqualTo(Long value) {
            addCriterion("workflow_id <=", value, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdIn(List<Long> values) {
            addCriterion("workflow_id in", values, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdNotIn(List<Long> values) {
            addCriterion("workflow_id not in", values, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdBetween(Long value1, Long value2) {
            addCriterion("workflow_id between", value1, value2, "workflowId");
            return (Criteria) this;
        }

        public Criteria andWorkflowIdNotBetween(Long value1, Long value2) {
            addCriterion("workflow_id not between", value1, value2, "workflowId");
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

        public Criteria andWorkflowErrorMessageIsNull() {
            addCriterion("workflow_error_message is null");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageIsNotNull() {
            addCriterion("workflow_error_message is not null");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageEqualTo(String value) {
            addCriterion("workflow_error_message =", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageNotEqualTo(String value) {
            addCriterion("workflow_error_message <>", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageGreaterThan(String value) {
            addCriterion("workflow_error_message >", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageGreaterThanOrEqualTo(String value) {
            addCriterion("workflow_error_message >=", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageLessThan(String value) {
            addCriterion("workflow_error_message <", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageLessThanOrEqualTo(String value) {
            addCriterion("workflow_error_message <=", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageLike(String value) {
            addCriterion("workflow_error_message like", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageNotLike(String value) {
            addCriterion("workflow_error_message not like", value, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageIn(List<String> values) {
            addCriterion("workflow_error_message in", values, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageNotIn(List<String> values) {
            addCriterion("workflow_error_message not in", values, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageBetween(String value1, String value2) {
            addCriterion("workflow_error_message between", value1, value2, "workflowErrorMessage");
            return (Criteria) this;
        }

        public Criteria andWorkflowErrorMessageNotBetween(String value1, String value2) {
            addCriterion("workflow_error_message not between", value1, value2, "workflowErrorMessage");
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