package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RtTraitInstanceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public RtTraitInstanceDOExample() {
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

        public Criteria andTraitInstanceIdIsNull() {
            addCriterion("trait_instance_id is null");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdIsNotNull() {
            addCriterion("trait_instance_id is not null");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdEqualTo(String value) {
            addCriterion("trait_instance_id =", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdNotEqualTo(String value) {
            addCriterion("trait_instance_id <>", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdGreaterThan(String value) {
            addCriterion("trait_instance_id >", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdGreaterThanOrEqualTo(String value) {
            addCriterion("trait_instance_id >=", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdLessThan(String value) {
            addCriterion("trait_instance_id <", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdLessThanOrEqualTo(String value) {
            addCriterion("trait_instance_id <=", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdLike(String value) {
            addCriterion("trait_instance_id like", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdNotLike(String value) {
            addCriterion("trait_instance_id not like", value, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdIn(List<String> values) {
            addCriterion("trait_instance_id in", values, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdNotIn(List<String> values) {
            addCriterion("trait_instance_id not in", values, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdBetween(String value1, String value2) {
            addCriterion("trait_instance_id between", value1, value2, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitInstanceIdNotBetween(String value1, String value2) {
            addCriterion("trait_instance_id not between", value1, value2, "traitInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdIsNull() {
            addCriterion("component_instance_id is null");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdIsNotNull() {
            addCriterion("component_instance_id is not null");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdEqualTo(String value) {
            addCriterion("component_instance_id =", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdNotEqualTo(String value) {
            addCriterion("component_instance_id <>", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdGreaterThan(String value) {
            addCriterion("component_instance_id >", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdGreaterThanOrEqualTo(String value) {
            addCriterion("component_instance_id >=", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdLessThan(String value) {
            addCriterion("component_instance_id <", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdLessThanOrEqualTo(String value) {
            addCriterion("component_instance_id <=", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdLike(String value) {
            addCriterion("component_instance_id like", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdNotLike(String value) {
            addCriterion("component_instance_id not like", value, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdIn(List<String> values) {
            addCriterion("component_instance_id in", values, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdNotIn(List<String> values) {
            addCriterion("component_instance_id not in", values, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdBetween(String value1, String value2) {
            addCriterion("component_instance_id between", value1, value2, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andComponentInstanceIdNotBetween(String value1, String value2) {
            addCriterion("component_instance_id not between", value1, value2, "componentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdIsNull() {
            addCriterion("app_instance_id is null");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdIsNotNull() {
            addCriterion("app_instance_id is not null");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdEqualTo(String value) {
            addCriterion("app_instance_id =", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdNotEqualTo(String value) {
            addCriterion("app_instance_id <>", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdGreaterThan(String value) {
            addCriterion("app_instance_id >", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdGreaterThanOrEqualTo(String value) {
            addCriterion("app_instance_id >=", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdLessThan(String value) {
            addCriterion("app_instance_id <", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdLessThanOrEqualTo(String value) {
            addCriterion("app_instance_id <=", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdLike(String value) {
            addCriterion("app_instance_id like", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdNotLike(String value) {
            addCriterion("app_instance_id not like", value, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdIn(List<String> values) {
            addCriterion("app_instance_id in", values, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdNotIn(List<String> values) {
            addCriterion("app_instance_id not in", values, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdBetween(String value1, String value2) {
            addCriterion("app_instance_id between", value1, value2, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdNotBetween(String value1, String value2) {
            addCriterion("app_instance_id not between", value1, value2, "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andTraitNameIsNull() {
            addCriterion("trait_name is null");
            return (Criteria) this;
        }

        public Criteria andTraitNameIsNotNull() {
            addCriterion("trait_name is not null");
            return (Criteria) this;
        }

        public Criteria andTraitNameEqualTo(String value) {
            addCriterion("trait_name =", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameNotEqualTo(String value) {
            addCriterion("trait_name <>", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameGreaterThan(String value) {
            addCriterion("trait_name >", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameGreaterThanOrEqualTo(String value) {
            addCriterion("trait_name >=", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameLessThan(String value) {
            addCriterion("trait_name <", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameLessThanOrEqualTo(String value) {
            addCriterion("trait_name <=", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameLike(String value) {
            addCriterion("trait_name like", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameNotLike(String value) {
            addCriterion("trait_name not like", value, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameIn(List<String> values) {
            addCriterion("trait_name in", values, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameNotIn(List<String> values) {
            addCriterion("trait_name not in", values, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameBetween(String value1, String value2) {
            addCriterion("trait_name between", value1, value2, "traitName");
            return (Criteria) this;
        }

        public Criteria andTraitNameNotBetween(String value1, String value2) {
            addCriterion("trait_name not between", value1, value2, "traitName");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("`status` is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("`status` is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(String value) {
            addCriterion("`status` =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(String value) {
            addCriterion("`status` <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(String value) {
            addCriterion("`status` >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(String value) {
            addCriterion("`status` >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(String value) {
            addCriterion("`status` <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(String value) {
            addCriterion("`status` <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLike(String value) {
            addCriterion("`status` like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotLike(String value) {
            addCriterion("`status` not like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<String> values) {
            addCriterion("`status` in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<String> values) {
            addCriterion("`status` not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(String value1, String value2) {
            addCriterion("`status` between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(String value1, String value2) {
            addCriterion("`status` not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andConditionsIsNull() {
            addCriterion("conditions is null");
            return (Criteria) this;
        }

        public Criteria andConditionsIsNotNull() {
            addCriterion("conditions is not null");
            return (Criteria) this;
        }

        public Criteria andConditionsEqualTo(String value) {
            addCriterion("conditions =", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsNotEqualTo(String value) {
            addCriterion("conditions <>", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsGreaterThan(String value) {
            addCriterion("conditions >", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsGreaterThanOrEqualTo(String value) {
            addCriterion("conditions >=", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsLessThan(String value) {
            addCriterion("conditions <", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsLessThanOrEqualTo(String value) {
            addCriterion("conditions <=", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsLike(String value) {
            addCriterion("conditions like", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsNotLike(String value) {
            addCriterion("conditions not like", value, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsIn(List<String> values) {
            addCriterion("conditions in", values, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsNotIn(List<String> values) {
            addCriterion("conditions not in", values, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsBetween(String value1, String value2) {
            addCriterion("conditions between", value1, value2, "conditions");
            return (Criteria) this;
        }

        public Criteria andConditionsNotBetween(String value1, String value2) {
            addCriterion("conditions not between", value1, value2, "conditions");
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