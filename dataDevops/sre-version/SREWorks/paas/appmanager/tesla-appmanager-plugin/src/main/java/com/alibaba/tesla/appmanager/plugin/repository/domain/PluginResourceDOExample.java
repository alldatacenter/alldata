package com.alibaba.tesla.appmanager.plugin.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PluginResourceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public PluginResourceDOExample() {
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

        public Criteria andPluginNameIsNull() {
            addCriterion("plugin_name is null");
            return (Criteria) this;
        }

        public Criteria andPluginNameIsNotNull() {
            addCriterion("plugin_name is not null");
            return (Criteria) this;
        }

        public Criteria andPluginNameEqualTo(Long value) {
            addCriterion("plugin_name =", value, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameNotEqualTo(Long value) {
            addCriterion("plugin_name <>", value, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameGreaterThan(Long value) {
            addCriterion("plugin_name >", value, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameGreaterThanOrEqualTo(Long value) {
            addCriterion("plugin_name >=", value, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameLessThan(Long value) {
            addCriterion("plugin_name <", value, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameLessThanOrEqualTo(Long value) {
            addCriterion("plugin_name <=", value, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameIn(List<Long> values) {
            addCriterion("plugin_name in", values, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameNotIn(List<Long> values) {
            addCriterion("plugin_name not in", values, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameBetween(Long value1, Long value2) {
            addCriterion("plugin_name between", value1, value2, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginNameNotBetween(Long value1, Long value2) {
            addCriterion("plugin_name not between", value1, value2, "pluginName");
            return (Criteria) this;
        }

        public Criteria andPluginVersionIsNull() {
            addCriterion("plugin_version is null");
            return (Criteria) this;
        }

        public Criteria andPluginVersionIsNotNull() {
            addCriterion("plugin_version is not null");
            return (Criteria) this;
        }

        public Criteria andPluginVersionEqualTo(String value) {
            addCriterion("plugin_version =", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionNotEqualTo(String value) {
            addCriterion("plugin_version <>", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionGreaterThan(String value) {
            addCriterion("plugin_version >", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionGreaterThanOrEqualTo(String value) {
            addCriterion("plugin_version >=", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionLessThan(String value) {
            addCriterion("plugin_version <", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionLessThanOrEqualTo(String value) {
            addCriterion("plugin_version <=", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionLike(String value) {
            addCriterion("plugin_version like", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionNotLike(String value) {
            addCriterion("plugin_version not like", value, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionIn(List<String> values) {
            addCriterion("plugin_version in", values, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionNotIn(List<String> values) {
            addCriterion("plugin_version not in", values, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionBetween(String value1, String value2) {
            addCriterion("plugin_version between", value1, value2, "pluginVersion");
            return (Criteria) this;
        }

        public Criteria andPluginVersionNotBetween(String value1, String value2) {
            addCriterion("plugin_version not between", value1, value2, "pluginVersion");
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

        public Criteria andInstanceStatusIsNull() {
            addCriterion("instance_status is null");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusIsNotNull() {
            addCriterion("instance_status is not null");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusEqualTo(String value) {
            addCriterion("instance_status =", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusNotEqualTo(String value) {
            addCriterion("instance_status <>", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusGreaterThan(String value) {
            addCriterion("instance_status >", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusGreaterThanOrEqualTo(String value) {
            addCriterion("instance_status >=", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusLessThan(String value) {
            addCriterion("instance_status <", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusLessThanOrEqualTo(String value) {
            addCriterion("instance_status <=", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusLike(String value) {
            addCriterion("instance_status like", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusNotLike(String value) {
            addCriterion("instance_status not like", value, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusIn(List<String> values) {
            addCriterion("instance_status in", values, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusNotIn(List<String> values) {
            addCriterion("instance_status not in", values, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusBetween(String value1, String value2) {
            addCriterion("instance_status between", value1, value2, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceStatusNotBetween(String value1, String value2) {
            addCriterion("instance_status not between", value1, value2, "instanceStatus");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageIsNull() {
            addCriterion("instance_error_message is null");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageIsNotNull() {
            addCriterion("instance_error_message is not null");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageEqualTo(String value) {
            addCriterion("instance_error_message =", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageNotEqualTo(String value) {
            addCriterion("instance_error_message <>", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageGreaterThan(String value) {
            addCriterion("instance_error_message >", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageGreaterThanOrEqualTo(String value) {
            addCriterion("instance_error_message >=", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageLessThan(String value) {
            addCriterion("instance_error_message <", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageLessThanOrEqualTo(String value) {
            addCriterion("instance_error_message <=", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageLike(String value) {
            addCriterion("instance_error_message like", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageNotLike(String value) {
            addCriterion("instance_error_message not like", value, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageIn(List<String> values) {
            addCriterion("instance_error_message in", values, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageNotIn(List<String> values) {
            addCriterion("instance_error_message not in", values, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageBetween(String value1, String value2) {
            addCriterion("instance_error_message between", value1, value2, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceErrorMessageNotBetween(String value1, String value2) {
            addCriterion("instance_error_message not between", value1, value2, "instanceErrorMessage");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredIsNull() {
            addCriterion("instance_registered is null");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredIsNotNull() {
            addCriterion("instance_registered is not null");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredEqualTo(Boolean value) {
            addCriterion("instance_registered =", value, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredNotEqualTo(Boolean value) {
            addCriterion("instance_registered <>", value, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredGreaterThan(Boolean value) {
            addCriterion("instance_registered >", value, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredGreaterThanOrEqualTo(Boolean value) {
            addCriterion("instance_registered >=", value, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredLessThan(Boolean value) {
            addCriterion("instance_registered <", value, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredLessThanOrEqualTo(Boolean value) {
            addCriterion("instance_registered <=", value, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredIn(List<Boolean> values) {
            addCriterion("instance_registered in", values, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredNotIn(List<Boolean> values) {
            addCriterion("instance_registered not in", values, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredBetween(Boolean value1, Boolean value2) {
            addCriterion("instance_registered between", value1, value2, "instanceRegistered");
            return (Criteria) this;
        }

        public Criteria andInstanceRegisteredNotBetween(Boolean value1, Boolean value2) {
            addCriterion("instance_registered not between", value1, value2, "instanceRegistered");
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