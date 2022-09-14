package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppAddonDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public AppAddonDOExample() {
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

        public Criteria andAddonTypeIsNull() {
            addCriterion("addon_type is null");
            return (Criteria) this;
        }

        public Criteria andAddonTypeIsNotNull() {
            addCriterion("addon_type is not null");
            return (Criteria) this;
        }

        public Criteria andAddonTypeEqualTo(String value) {
            addCriterion("addon_type =", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotEqualTo(String value) {
            addCriterion("addon_type <>", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeGreaterThan(String value) {
            addCriterion("addon_type >", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeGreaterThanOrEqualTo(String value) {
            addCriterion("addon_type >=", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeLessThan(String value) {
            addCriterion("addon_type <", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeLessThanOrEqualTo(String value) {
            addCriterion("addon_type <=", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeLike(String value) {
            addCriterion("addon_type like", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotLike(String value) {
            addCriterion("addon_type not like", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeIn(List<String> values) {
            addCriterion("addon_type in", values, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotIn(List<String> values) {
            addCriterion("addon_type not in", values, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeBetween(String value1, String value2) {
            addCriterion("addon_type between", value1, value2, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotBetween(String value1, String value2) {
            addCriterion("addon_type not between", value1, value2, "addonType");
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

        public Criteria andAddonConfigIsNull() {
            addCriterion("addon_config is null");
            return (Criteria) this;
        }

        public Criteria andAddonConfigIsNotNull() {
            addCriterion("addon_config is not null");
            return (Criteria) this;
        }

        public Criteria andAddonConfigEqualTo(String value) {
            addCriterion("addon_config =", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigNotEqualTo(String value) {
            addCriterion("addon_config <>", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigGreaterThan(String value) {
            addCriterion("addon_config >", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigGreaterThanOrEqualTo(String value) {
            addCriterion("addon_config >=", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigLessThan(String value) {
            addCriterion("addon_config <", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigLessThanOrEqualTo(String value) {
            addCriterion("addon_config <=", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigLike(String value) {
            addCriterion("addon_config like", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigNotLike(String value) {
            addCriterion("addon_config not like", value, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigIn(List<String> values) {
            addCriterion("addon_config in", values, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigNotIn(List<String> values) {
            addCriterion("addon_config not in", values, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigBetween(String value1, String value2) {
            addCriterion("addon_config between", value1, value2, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andAddonConfigNotBetween(String value1, String value2) {
            addCriterion("addon_config not between", value1, value2, "addonConfig");
            return (Criteria) this;
        }

        public Criteria andNameIsNull() {
            addCriterion("`name` is null");
            return (Criteria) this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("`name` is not null");
            return (Criteria) this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("`name` =", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("`name` <>", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("`name` >", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("`name` >=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("`name` <", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("`name` <=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("`name` like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("`name` not like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("`name` in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("`name` not in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("`name` between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("`name` not between", value1, value2, "name");
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

        public Criteria andStageIdIsNull() {
            addCriterion("stage_id is null");
            return (Criteria) this;
        }

        public Criteria andStageIdIsNotNull() {
            addCriterion("stage_id is not null");
            return (Criteria) this;
        }

        public Criteria andStageIdEqualTo(String value) {
            addCriterion("stage_id =", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotEqualTo(String value) {
            addCriterion("stage_id <>", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdGreaterThan(String value) {
            addCriterion("stage_id >", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdGreaterThanOrEqualTo(String value) {
            addCriterion("stage_id >=", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdLessThan(String value) {
            addCriterion("stage_id <", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdLessThanOrEqualTo(String value) {
            addCriterion("stage_id <=", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdLike(String value) {
            addCriterion("stage_id like", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotLike(String value) {
            addCriterion("stage_id not like", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdIn(List<String> values) {
            addCriterion("stage_id in", values, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotIn(List<String> values) {
            addCriterion("stage_id not in", values, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdBetween(String value1, String value2) {
            addCriterion("stage_id between", value1, value2, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotBetween(String value1, String value2) {
            addCriterion("stage_id not between", value1, value2, "stageId");
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