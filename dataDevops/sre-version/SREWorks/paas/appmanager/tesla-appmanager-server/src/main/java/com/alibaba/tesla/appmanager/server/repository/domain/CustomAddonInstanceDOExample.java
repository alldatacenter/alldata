package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class CustomAddonInstanceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public CustomAddonInstanceDOExample() {
        oredCriteria = new ArrayList<Criteria>();
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
            criteria = new ArrayList<Criterion>();
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

        public Criteria andAddonInstanceIdIsNull() {
            addCriterion("addon_instance_id is null");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdIsNotNull() {
            addCriterion("addon_instance_id is not null");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdEqualTo(String value) {
            addCriterion("addon_instance_id =", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdNotEqualTo(String value) {
            addCriterion("addon_instance_id <>", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdGreaterThan(String value) {
            addCriterion("addon_instance_id >", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdGreaterThanOrEqualTo(String value) {
            addCriterion("addon_instance_id >=", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdLessThan(String value) {
            addCriterion("addon_instance_id <", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdLessThanOrEqualTo(String value) {
            addCriterion("addon_instance_id <=", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdLike(String value) {
            addCriterion("addon_instance_id like", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdNotLike(String value) {
            addCriterion("addon_instance_id not like", value, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdIn(List<String> values) {
            addCriterion("addon_instance_id in", values, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdNotIn(List<String> values) {
            addCriterion("addon_instance_id not in", values, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdBetween(String value1, String value2) {
            addCriterion("addon_instance_id between", value1, value2, "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdNotBetween(String value1, String value2) {
            addCriterion("addon_instance_id not between", value1, value2, "addonInstanceId");
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

        public Criteria andAddonIdIsNull() {
            addCriterion("addon_id is null");
            return (Criteria) this;
        }

        public Criteria andAddonIdIsNotNull() {
            addCriterion("addon_id is not null");
            return (Criteria) this;
        }

        public Criteria andAddonIdEqualTo(String value) {
            addCriterion("addon_id =", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotEqualTo(String value) {
            addCriterion("addon_id <>", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdGreaterThan(String value) {
            addCriterion("addon_id >", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdGreaterThanOrEqualTo(String value) {
            addCriterion("addon_id >=", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLessThan(String value) {
            addCriterion("addon_id <", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLessThanOrEqualTo(String value) {
            addCriterion("addon_id <=", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLike(String value) {
            addCriterion("addon_id like", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotLike(String value) {
            addCriterion("addon_id not like", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdIn(List<String> values) {
            addCriterion("addon_id in", values, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotIn(List<String> values) {
            addCriterion("addon_id not in", values, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdBetween(String value1, String value2) {
            addCriterion("addon_id between", value1, value2, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotBetween(String value1, String value2) {
            addCriterion("addon_id not between", value1, value2, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonNameIsNull() {
            addCriterion("addon_name is null");
            return (Criteria) this;
        }

        public Criteria andAddonNameIsNotNull() {
            addCriterion("addon_name is not null");
            return (Criteria) this;
        }

        public Criteria andAddonNameEqualTo(String value) {
            addCriterion("addon_name =", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameNotEqualTo(String value) {
            addCriterion("addon_name <>", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameGreaterThan(String value) {
            addCriterion("addon_name >", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameGreaterThanOrEqualTo(String value) {
            addCriterion("addon_name >=", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameLessThan(String value) {
            addCriterion("addon_name <", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameLessThanOrEqualTo(String value) {
            addCriterion("addon_name <=", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameLike(String value) {
            addCriterion("addon_name like", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameNotLike(String value) {
            addCriterion("addon_name not like", value, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameIn(List<String> values) {
            addCriterion("addon_name in", values, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameNotIn(List<String> values) {
            addCriterion("addon_name not in", values, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameBetween(String value1, String value2) {
            addCriterion("addon_name between", value1, value2, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonNameNotBetween(String value1, String value2) {
            addCriterion("addon_name not between", value1, value2, "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonVersionIsNull() {
            addCriterion("addon_version is null");
            return (Criteria) this;
        }

        public Criteria andAddonVersionIsNotNull() {
            addCriterion("addon_version is not null");
            return (Criteria) this;
        }

        public Criteria andAddonVersionEqualTo(String value) {
            addCriterion("addon_version =", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotEqualTo(String value) {
            addCriterion("addon_version <>", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionGreaterThan(String value) {
            addCriterion("addon_version >", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionGreaterThanOrEqualTo(String value) {
            addCriterion("addon_version >=", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLessThan(String value) {
            addCriterion("addon_version <", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLessThanOrEqualTo(String value) {
            addCriterion("addon_version <=", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLike(String value) {
            addCriterion("addon_version like", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotLike(String value) {
            addCriterion("addon_version not like", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionIn(List<String> values) {
            addCriterion("addon_version in", values, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotIn(List<String> values) {
            addCriterion("addon_version not in", values, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionBetween(String value1, String value2) {
            addCriterion("addon_version between", value1, value2, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotBetween(String value1, String value2) {
            addCriterion("addon_version not between", value1, value2, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsIsNull() {
            addCriterion("addon_attrs is null");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsIsNotNull() {
            addCriterion("addon_attrs is not null");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsEqualTo(String value) {
            addCriterion("addon_attrs =", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsNotEqualTo(String value) {
            addCriterion("addon_attrs <>", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsGreaterThan(String value) {
            addCriterion("addon_attrs >", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsGreaterThanOrEqualTo(String value) {
            addCriterion("addon_attrs >=", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsLessThan(String value) {
            addCriterion("addon_attrs <", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsLessThanOrEqualTo(String value) {
            addCriterion("addon_attrs <=", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsLike(String value) {
            addCriterion("addon_attrs like", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsNotLike(String value) {
            addCriterion("addon_attrs not like", value, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsIn(List<String> values) {
            addCriterion("addon_attrs in", values, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsNotIn(List<String> values) {
            addCriterion("addon_attrs not in", values, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsBetween(String value1, String value2) {
            addCriterion("addon_attrs between", value1, value2, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsNotBetween(String value1, String value2) {
            addCriterion("addon_attrs not between", value1, value2, "addonAttrs");
            return (Criteria) this;
        }

        public Criteria andAddonInstanceIdLikeInsensitive(String value) {
            addCriterion("upper(addon_instance_id) like", value.toUpperCase(), "addonInstanceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLikeInsensitive(String value) {
            addCriterion("upper(namespace_id) like", value.toUpperCase(), "namespaceId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLikeInsensitive(String value) {
            addCriterion("upper(addon_id) like", value.toUpperCase(), "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonNameLikeInsensitive(String value) {
            addCriterion("upper(addon_name) like", value.toUpperCase(), "addonName");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLikeInsensitive(String value) {
            addCriterion("upper(addon_version) like", value.toUpperCase(), "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonAttrsLikeInsensitive(String value) {
            addCriterion("upper(addon_attrs) like", value.toUpperCase(), "addonAttrs");
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