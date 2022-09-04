package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NamespaceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public NamespaceDOExample() {
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

        public Criteria andNamespaceNameIsNull() {
            addCriterion("namespace_name is null");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameIsNotNull() {
            addCriterion("namespace_name is not null");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameEqualTo(String value) {
            addCriterion("namespace_name =", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameNotEqualTo(String value) {
            addCriterion("namespace_name <>", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameGreaterThan(String value) {
            addCriterion("namespace_name >", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameGreaterThanOrEqualTo(String value) {
            addCriterion("namespace_name >=", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameLessThan(String value) {
            addCriterion("namespace_name <", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameLessThanOrEqualTo(String value) {
            addCriterion("namespace_name <=", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameLike(String value) {
            addCriterion("namespace_name like", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameNotLike(String value) {
            addCriterion("namespace_name not like", value, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameIn(List<String> values) {
            addCriterion("namespace_name in", values, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameNotIn(List<String> values) {
            addCriterion("namespace_name not in", values, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameBetween(String value1, String value2) {
            addCriterion("namespace_name between", value1, value2, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceNameNotBetween(String value1, String value2) {
            addCriterion("namespace_name not between", value1, value2, "namespaceName");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorIsNull() {
            addCriterion("namespace_creator is null");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorIsNotNull() {
            addCriterion("namespace_creator is not null");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorEqualTo(String value) {
            addCriterion("namespace_creator =", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorNotEqualTo(String value) {
            addCriterion("namespace_creator <>", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorGreaterThan(String value) {
            addCriterion("namespace_creator >", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorGreaterThanOrEqualTo(String value) {
            addCriterion("namespace_creator >=", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorLessThan(String value) {
            addCriterion("namespace_creator <", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorLessThanOrEqualTo(String value) {
            addCriterion("namespace_creator <=", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorLike(String value) {
            addCriterion("namespace_creator like", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorNotLike(String value) {
            addCriterion("namespace_creator not like", value, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorIn(List<String> values) {
            addCriterion("namespace_creator in", values, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorNotIn(List<String> values) {
            addCriterion("namespace_creator not in", values, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorBetween(String value1, String value2) {
            addCriterion("namespace_creator between", value1, value2, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceCreatorNotBetween(String value1, String value2) {
            addCriterion("namespace_creator not between", value1, value2, "namespaceCreator");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierIsNull() {
            addCriterion("namespace_modifier is null");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierIsNotNull() {
            addCriterion("namespace_modifier is not null");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierEqualTo(String value) {
            addCriterion("namespace_modifier =", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierNotEqualTo(String value) {
            addCriterion("namespace_modifier <>", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierGreaterThan(String value) {
            addCriterion("namespace_modifier >", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierGreaterThanOrEqualTo(String value) {
            addCriterion("namespace_modifier >=", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierLessThan(String value) {
            addCriterion("namespace_modifier <", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierLessThanOrEqualTo(String value) {
            addCriterion("namespace_modifier <=", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierLike(String value) {
            addCriterion("namespace_modifier like", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierNotLike(String value) {
            addCriterion("namespace_modifier not like", value, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierIn(List<String> values) {
            addCriterion("namespace_modifier in", values, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierNotIn(List<String> values) {
            addCriterion("namespace_modifier not in", values, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierBetween(String value1, String value2) {
            addCriterion("namespace_modifier between", value1, value2, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceModifierNotBetween(String value1, String value2) {
            addCriterion("namespace_modifier not between", value1, value2, "namespaceModifier");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtIsNull() {
            addCriterion("namespace_ext is null");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtIsNotNull() {
            addCriterion("namespace_ext is not null");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtEqualTo(String value) {
            addCriterion("namespace_ext =", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtNotEqualTo(String value) {
            addCriterion("namespace_ext <>", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtGreaterThan(String value) {
            addCriterion("namespace_ext >", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtGreaterThanOrEqualTo(String value) {
            addCriterion("namespace_ext >=", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtLessThan(String value) {
            addCriterion("namespace_ext <", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtLessThanOrEqualTo(String value) {
            addCriterion("namespace_ext <=", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtLike(String value) {
            addCriterion("namespace_ext like", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtNotLike(String value) {
            addCriterion("namespace_ext not like", value, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtIn(List<String> values) {
            addCriterion("namespace_ext in", values, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtNotIn(List<String> values) {
            addCriterion("namespace_ext not in", values, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtBetween(String value1, String value2) {
            addCriterion("namespace_ext between", value1, value2, "namespaceExt");
            return (Criteria) this;
        }

        public Criteria andNamespaceExtNotBetween(String value1, String value2) {
            addCriterion("namespace_ext not between", value1, value2, "namespaceExt");
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