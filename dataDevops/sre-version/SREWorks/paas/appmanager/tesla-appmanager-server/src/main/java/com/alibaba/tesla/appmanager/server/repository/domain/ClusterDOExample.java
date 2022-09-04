package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClusterDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public ClusterDOExample() {
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

        public Criteria andClusterNameIsNull() {
            addCriterion("cluster_name is null");
            return (Criteria) this;
        }

        public Criteria andClusterNameIsNotNull() {
            addCriterion("cluster_name is not null");
            return (Criteria) this;
        }

        public Criteria andClusterNameEqualTo(String value) {
            addCriterion("cluster_name =", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameNotEqualTo(String value) {
            addCriterion("cluster_name <>", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameGreaterThan(String value) {
            addCriterion("cluster_name >", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameGreaterThanOrEqualTo(String value) {
            addCriterion("cluster_name >=", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameLessThan(String value) {
            addCriterion("cluster_name <", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameLessThanOrEqualTo(String value) {
            addCriterion("cluster_name <=", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameLike(String value) {
            addCriterion("cluster_name like", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameNotLike(String value) {
            addCriterion("cluster_name not like", value, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameIn(List<String> values) {
            addCriterion("cluster_name in", values, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameNotIn(List<String> values) {
            addCriterion("cluster_name not in", values, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameBetween(String value1, String value2) {
            addCriterion("cluster_name between", value1, value2, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterNameNotBetween(String value1, String value2) {
            addCriterion("cluster_name not between", value1, value2, "clusterName");
            return (Criteria) this;
        }

        public Criteria andClusterTypeIsNull() {
            addCriterion("cluster_type is null");
            return (Criteria) this;
        }

        public Criteria andClusterTypeIsNotNull() {
            addCriterion("cluster_type is not null");
            return (Criteria) this;
        }

        public Criteria andClusterTypeEqualTo(String value) {
            addCriterion("cluster_type =", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeNotEqualTo(String value) {
            addCriterion("cluster_type <>", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeGreaterThan(String value) {
            addCriterion("cluster_type >", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeGreaterThanOrEqualTo(String value) {
            addCriterion("cluster_type >=", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeLessThan(String value) {
            addCriterion("cluster_type <", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeLessThanOrEqualTo(String value) {
            addCriterion("cluster_type <=", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeLike(String value) {
            addCriterion("cluster_type like", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeNotLike(String value) {
            addCriterion("cluster_type not like", value, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeIn(List<String> values) {
            addCriterion("cluster_type in", values, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeNotIn(List<String> values) {
            addCriterion("cluster_type not in", values, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeBetween(String value1, String value2) {
            addCriterion("cluster_type between", value1, value2, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterTypeNotBetween(String value1, String value2) {
            addCriterion("cluster_type not between", value1, value2, "clusterType");
            return (Criteria) this;
        }

        public Criteria andClusterConfigIsNull() {
            addCriterion("cluster_config is null");
            return (Criteria) this;
        }

        public Criteria andClusterConfigIsNotNull() {
            addCriterion("cluster_config is not null");
            return (Criteria) this;
        }

        public Criteria andClusterConfigEqualTo(String value) {
            addCriterion("cluster_config =", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigNotEqualTo(String value) {
            addCriterion("cluster_config <>", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigGreaterThan(String value) {
            addCriterion("cluster_config >", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigGreaterThanOrEqualTo(String value) {
            addCriterion("cluster_config >=", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigLessThan(String value) {
            addCriterion("cluster_config <", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigLessThanOrEqualTo(String value) {
            addCriterion("cluster_config <=", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigLike(String value) {
            addCriterion("cluster_config like", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigNotLike(String value) {
            addCriterion("cluster_config not like", value, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigIn(List<String> values) {
            addCriterion("cluster_config in", values, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigNotIn(List<String> values) {
            addCriterion("cluster_config not in", values, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigBetween(String value1, String value2) {
            addCriterion("cluster_config between", value1, value2, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andClusterConfigNotBetween(String value1, String value2) {
            addCriterion("cluster_config not between", value1, value2, "clusterConfig");
            return (Criteria) this;
        }

        public Criteria andMasterFlagIsNull() {
            addCriterion("master_flag is null");
            return (Criteria) this;
        }

        public Criteria andMasterFlagIsNotNull() {
            addCriterion("master_flag is not null");
            return (Criteria) this;
        }

        public Criteria andMasterFlagEqualTo(Boolean value) {
            addCriterion("master_flag =", value, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagNotEqualTo(Boolean value) {
            addCriterion("master_flag <>", value, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagGreaterThan(Boolean value) {
            addCriterion("master_flag >", value, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagGreaterThanOrEqualTo(Boolean value) {
            addCriterion("master_flag >=", value, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagLessThan(Boolean value) {
            addCriterion("master_flag <", value, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagLessThanOrEqualTo(Boolean value) {
            addCriterion("master_flag <=", value, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagIn(List<Boolean> values) {
            addCriterion("master_flag in", values, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagNotIn(List<Boolean> values) {
            addCriterion("master_flag not in", values, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagBetween(Boolean value1, Boolean value2) {
            addCriterion("master_flag between", value1, value2, "masterFlag");
            return (Criteria) this;
        }

        public Criteria andMasterFlagNotBetween(Boolean value1, Boolean value2) {
            addCriterion("master_flag not between", value1, value2, "masterFlag");
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