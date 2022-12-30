package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EnvDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public EnvDOExample() {
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

        public Criteria andNamespaceIdIsNull() {
            addCriterion("namespace_id is null");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdIsNotNull() {
            addCriterion("namespace_id is not null");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdEqualTo(String value) {
            addCriterion("namespace_id =", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotEqualTo(String value) {
            addCriterion("namespace_id <>", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdGreaterThan(String value) {
            addCriterion("namespace_id >", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdGreaterThanOrEqualTo(String value) {
            addCriterion("namespace_id >=", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLessThan(String value) {
            addCriterion("namespace_id <", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLessThanOrEqualTo(String value) {
            addCriterion("namespace_id <=", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLike(String value) {
            addCriterion("namespace_id like", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotLike(String value) {
            addCriterion("namespace_id not like", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdIn(List<String> values) {
            addCriterion("namespace_id in", values, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotIn(List<String> values) {
            addCriterion("namespace_id not in", values, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdBetween(String value1, String value2) {
            addCriterion("namespace_id between", value1, value2, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotBetween(String value1, String value2) {
            addCriterion("namespace_id not between", value1, value2, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andEnvIdIsNull() {
            addCriterion("env_id is null");
            return (Criteria) this;
        }

        public Criteria andEnvIdIsNotNull() {
            addCriterion("env_id is not null");
            return (Criteria) this;
        }

        public Criteria andEnvIdEqualTo(String value) {
            addCriterion("env_id =", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdNotEqualTo(String value) {
            addCriterion("env_id <>", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdGreaterThan(String value) {
            addCriterion("env_id >", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdGreaterThanOrEqualTo(String value) {
            addCriterion("env_id >=", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdLessThan(String value) {
            addCriterion("env_id <", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdLessThanOrEqualTo(String value) {
            addCriterion("env_id <=", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdLike(String value) {
            addCriterion("env_id like", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdNotLike(String value) {
            addCriterion("env_id not like", value, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdIn(List<String> values) {
            addCriterion("env_id in", values, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdNotIn(List<String> values) {
            addCriterion("env_id not in", values, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdBetween(String value1, String value2) {
            addCriterion("env_id between", value1, value2, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvIdNotBetween(String value1, String value2) {
            addCriterion("env_id not between", value1, value2, "envId");
            return (Criteria) this;
        }

        public Criteria andEnvNameIsNull() {
            addCriterion("env_name is null");
            return (Criteria) this;
        }

        public Criteria andEnvNameIsNotNull() {
            addCriterion("env_name is not null");
            return (Criteria) this;
        }

        public Criteria andEnvNameEqualTo(String value) {
            addCriterion("env_name =", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameNotEqualTo(String value) {
            addCriterion("env_name <>", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameGreaterThan(String value) {
            addCriterion("env_name >", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameGreaterThanOrEqualTo(String value) {
            addCriterion("env_name >=", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameLessThan(String value) {
            addCriterion("env_name <", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameLessThanOrEqualTo(String value) {
            addCriterion("env_name <=", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameLike(String value) {
            addCriterion("env_name like", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameNotLike(String value) {
            addCriterion("env_name not like", value, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameIn(List<String> values) {
            addCriterion("env_name in", values, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameNotIn(List<String> values) {
            addCriterion("env_name not in", values, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameBetween(String value1, String value2) {
            addCriterion("env_name between", value1, value2, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvNameNotBetween(String value1, String value2) {
            addCriterion("env_name not between", value1, value2, "envName");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorIsNull() {
            addCriterion("env_creator is null");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorIsNotNull() {
            addCriterion("env_creator is not null");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorEqualTo(String value) {
            addCriterion("env_creator =", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorNotEqualTo(String value) {
            addCriterion("env_creator <>", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorGreaterThan(String value) {
            addCriterion("env_creator >", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorGreaterThanOrEqualTo(String value) {
            addCriterion("env_creator >=", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorLessThan(String value) {
            addCriterion("env_creator <", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorLessThanOrEqualTo(String value) {
            addCriterion("env_creator <=", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorLike(String value) {
            addCriterion("env_creator like", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorNotLike(String value) {
            addCriterion("env_creator not like", value, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorIn(List<String> values) {
            addCriterion("env_creator in", values, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorNotIn(List<String> values) {
            addCriterion("env_creator not in", values, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorBetween(String value1, String value2) {
            addCriterion("env_creator between", value1, value2, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvCreatorNotBetween(String value1, String value2) {
            addCriterion("env_creator not between", value1, value2, "envCreator");
            return (Criteria) this;
        }

        public Criteria andEnvModifierIsNull() {
            addCriterion("env_modifier is null");
            return (Criteria) this;
        }

        public Criteria andEnvModifierIsNotNull() {
            addCriterion("env_modifier is not null");
            return (Criteria) this;
        }

        public Criteria andEnvModifierEqualTo(String value) {
            addCriterion("env_modifier =", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierNotEqualTo(String value) {
            addCriterion("env_modifier <>", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierGreaterThan(String value) {
            addCriterion("env_modifier >", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierGreaterThanOrEqualTo(String value) {
            addCriterion("env_modifier >=", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierLessThan(String value) {
            addCriterion("env_modifier <", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierLessThanOrEqualTo(String value) {
            addCriterion("env_modifier <=", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierLike(String value) {
            addCriterion("env_modifier like", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierNotLike(String value) {
            addCriterion("env_modifier not like", value, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierIn(List<String> values) {
            addCriterion("env_modifier in", values, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierNotIn(List<String> values) {
            addCriterion("env_modifier not in", values, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierBetween(String value1, String value2) {
            addCriterion("env_modifier between", value1, value2, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvModifierNotBetween(String value1, String value2) {
            addCriterion("env_modifier not between", value1, value2, "envModifier");
            return (Criteria) this;
        }

        public Criteria andEnvExtIsNull() {
            addCriterion("env_ext is null");
            return (Criteria) this;
        }

        public Criteria andEnvExtIsNotNull() {
            addCriterion("env_ext is not null");
            return (Criteria) this;
        }

        public Criteria andEnvExtEqualTo(String value) {
            addCriterion("env_ext =", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtNotEqualTo(String value) {
            addCriterion("env_ext <>", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtGreaterThan(String value) {
            addCriterion("env_ext >", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtGreaterThanOrEqualTo(String value) {
            addCriterion("env_ext >=", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtLessThan(String value) {
            addCriterion("env_ext <", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtLessThanOrEqualTo(String value) {
            addCriterion("env_ext <=", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtLike(String value) {
            addCriterion("env_ext like", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtNotLike(String value) {
            addCriterion("env_ext not like", value, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtIn(List<String> values) {
            addCriterion("env_ext in", values, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtNotIn(List<String> values) {
            addCriterion("env_ext not in", values, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtBetween(String value1, String value2) {
            addCriterion("env_ext between", value1, value2, "envExt");
            return (Criteria) this;
        }

        public Criteria andEnvExtNotBetween(String value1, String value2) {
            addCriterion("env_ext not between", value1, value2, "envExt");
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