package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AddonMetaDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public AddonMetaDOExample() {
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
            addCriterion("`id` is null");
            return (Criteria) this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("`id` is not null");
            return (Criteria) this;
        }

        public Criteria andIdEqualTo(Long value) {
            addCriterion("`id` =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Long value) {
            addCriterion("`id` <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Long value) {
            addCriterion("`id` >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Long value) {
            addCriterion("`id` >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Long value) {
            addCriterion("`id` <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Long value) {
            addCriterion("`id` <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Long> values) {
            addCriterion("`id` in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Long> values) {
            addCriterion("`id` not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Long value1, Long value2) {
            addCriterion("`id` between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Long value1, Long value2) {
            addCriterion("`id` not between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNull() {
            addCriterion("`gmt_create` is null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIsNotNull() {
            addCriterion("`gmt_create` is not null");
            return (Criteria) this;
        }

        public Criteria andGmtCreateEqualTo(Date value) {
            addCriterion("`gmt_create` =", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotEqualTo(Date value) {
            addCriterion("`gmt_create` <>", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThan(Date value) {
            addCriterion("`gmt_create` >", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) {
            addCriterion("`gmt_create` >=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThan(Date value) {
            addCriterion("`gmt_create` <", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateLessThanOrEqualTo(Date value) {
            addCriterion("`gmt_create` <=", value, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateIn(List<Date> values) {
            addCriterion("`gmt_create` in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotIn(List<Date> values) {
            addCriterion("`gmt_create` not in", values, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateBetween(Date value1, Date value2) {
            addCriterion("`gmt_create` between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtCreateNotBetween(Date value1, Date value2) {
            addCriterion("`gmt_create` not between", value1, value2, "gmtCreate");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNull() {
            addCriterion("`gmt_modified` is null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIsNotNull() {
            addCriterion("`gmt_modified` is not null");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedEqualTo(Date value) {
            addCriterion("`gmt_modified` =", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotEqualTo(Date value) {
            addCriterion("`gmt_modified` <>", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThan(Date value) {
            addCriterion("`gmt_modified` >", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) {
            addCriterion("`gmt_modified` >=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThan(Date value) {
            addCriterion("`gmt_modified` <", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedLessThanOrEqualTo(Date value) {
            addCriterion("`gmt_modified` <=", value, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedIn(List<Date> values) {
            addCriterion("`gmt_modified` in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotIn(List<Date> values) {
            addCriterion("`gmt_modified` not in", values, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedBetween(Date value1, Date value2) {
            addCriterion("`gmt_modified` between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andGmtModifiedNotBetween(Date value1, Date value2) {
            addCriterion("`gmt_modified` not between", value1, value2, "gmtModified");
            return (Criteria) this;
        }

        public Criteria andAddonTypeIsNull() {
            addCriterion("`addon_type` is null");
            return (Criteria) this;
        }

        public Criteria andAddonTypeIsNotNull() {
            addCriterion("`addon_type` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonTypeEqualTo(String value) {
            addCriterion("`addon_type` =", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotEqualTo(String value) {
            addCriterion("`addon_type` <>", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeGreaterThan(String value) {
            addCriterion("`addon_type` >", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_type` >=", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeLessThan(String value) {
            addCriterion("`addon_type` <", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeLessThanOrEqualTo(String value) {
            addCriterion("`addon_type` <=", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeLike(String value) {
            addCriterion("`addon_type` like", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotLike(String value) {
            addCriterion("`addon_type` not like", value, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeIn(List<String> values) {
            addCriterion("`addon_type` in", values, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotIn(List<String> values) {
            addCriterion("`addon_type` not in", values, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeBetween(String value1, String value2) {
            addCriterion("`addon_type` between", value1, value2, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonTypeNotBetween(String value1, String value2) {
            addCriterion("`addon_type` not between", value1, value2, "addonType");
            return (Criteria) this;
        }

        public Criteria andAddonIdIsNull() {
            addCriterion("`addon_id` is null");
            return (Criteria) this;
        }

        public Criteria andAddonIdIsNotNull() {
            addCriterion("`addon_id` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonIdEqualTo(String value) {
            addCriterion("`addon_id` =", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotEqualTo(String value) {
            addCriterion("`addon_id` <>", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdGreaterThan(String value) {
            addCriterion("`addon_id` >", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_id` >=", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLessThan(String value) {
            addCriterion("`addon_id` <", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLessThanOrEqualTo(String value) {
            addCriterion("`addon_id` <=", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdLike(String value) {
            addCriterion("`addon_id` like", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotLike(String value) {
            addCriterion("`addon_id` not like", value, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdIn(List<String> values) {
            addCriterion("`addon_id` in", values, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotIn(List<String> values) {
            addCriterion("`addon_id` not in", values, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdBetween(String value1, String value2) {
            addCriterion("`addon_id` between", value1, value2, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonIdNotBetween(String value1, String value2) {
            addCriterion("`addon_id` not between", value1, value2, "addonId");
            return (Criteria) this;
        }

        public Criteria andAddonVersionIsNull() {
            addCriterion("`addon_version` is null");
            return (Criteria) this;
        }

        public Criteria andAddonVersionIsNotNull() {
            addCriterion("`addon_version` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonVersionEqualTo(String value) {
            addCriterion("`addon_version` =", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotEqualTo(String value) {
            addCriterion("`addon_version` <>", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionGreaterThan(String value) {
            addCriterion("`addon_version` >", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_version` >=", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLessThan(String value) {
            addCriterion("`addon_version` <", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLessThanOrEqualTo(String value) {
            addCriterion("`addon_version` <=", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionLike(String value) {
            addCriterion("`addon_version` like", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotLike(String value) {
            addCriterion("`addon_version` not like", value, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionIn(List<String> values) {
            addCriterion("`addon_version` in", values, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotIn(List<String> values) {
            addCriterion("`addon_version` not in", values, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionBetween(String value1, String value2) {
            addCriterion("`addon_version` between", value1, value2, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonVersionNotBetween(String value1, String value2) {
            addCriterion("`addon_version` not between", value1, value2, "addonVersion");
            return (Criteria) this;
        }

        public Criteria andAddonLabelIsNull() {
            addCriterion("`addon_label` is null");
            return (Criteria) this;
        }

        public Criteria andAddonLabelIsNotNull() {
            addCriterion("`addon_label` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonLabelEqualTo(String value) {
            addCriterion("`addon_label` =", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelNotEqualTo(String value) {
            addCriterion("`addon_label` <>", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelGreaterThan(String value) {
            addCriterion("`addon_label` >", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_label` >=", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelLessThan(String value) {
            addCriterion("`addon_label` <", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelLessThanOrEqualTo(String value) {
            addCriterion("`addon_label` <=", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelLike(String value) {
            addCriterion("`addon_label` like", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelNotLike(String value) {
            addCriterion("`addon_label` not like", value, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelIn(List<String> values) {
            addCriterion("`addon_label` in", values, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelNotIn(List<String> values) {
            addCriterion("`addon_label` not in", values, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelBetween(String value1, String value2) {
            addCriterion("`addon_label` between", value1, value2, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonLabelNotBetween(String value1, String value2) {
            addCriterion("`addon_label` not between", value1, value2, "addonLabel");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionIsNull() {
            addCriterion("`addon_description` is null");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionIsNotNull() {
            addCriterion("`addon_description` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionEqualTo(String value) {
            addCriterion("`addon_description` =", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionNotEqualTo(String value) {
            addCriterion("`addon_description` <>", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionGreaterThan(String value) {
            addCriterion("`addon_description` >", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_description` >=", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionLessThan(String value) {
            addCriterion("`addon_description` <", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionLessThanOrEqualTo(String value) {
            addCriterion("`addon_description` <=", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionLike(String value) {
            addCriterion("`addon_description` like", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionNotLike(String value) {
            addCriterion("`addon_description` not like", value, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionIn(List<String> values) {
            addCriterion("`addon_description` in", values, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionNotIn(List<String> values) {
            addCriterion("`addon_description` not in", values, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionBetween(String value1, String value2) {
            addCriterion("`addon_description` between", value1, value2, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonDescriptionNotBetween(String value1, String value2) {
            addCriterion("`addon_description` not between", value1, value2, "addonDescription");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaIsNull() {
            addCriterion("`addon_schema` is null");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaIsNotNull() {
            addCriterion("`addon_schema` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaEqualTo(String value) {
            addCriterion("`addon_schema` =", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaNotEqualTo(String value) {
            addCriterion("`addon_schema` <>", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaGreaterThan(String value) {
            addCriterion("`addon_schema` >", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_schema` >=", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaLessThan(String value) {
            addCriterion("`addon_schema` <", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaLessThanOrEqualTo(String value) {
            addCriterion("`addon_schema` <=", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaLike(String value) {
            addCriterion("`addon_schema` like", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaNotLike(String value) {
            addCriterion("`addon_schema` not like", value, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaIn(List<String> values) {
            addCriterion("`addon_schema` in", values, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaNotIn(List<String> values) {
            addCriterion("`addon_schema` not in", values, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaBetween(String value1, String value2) {
            addCriterion("`addon_schema` between", value1, value2, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonSchemaNotBetween(String value1, String value2) {
            addCriterion("`addon_schema` not between", value1, value2, "addonSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaIsNull() {
            addCriterion("`addon_config_schema` is null");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaIsNotNull() {
            addCriterion("`addon_config_schema` is not null");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaEqualTo(String value) {
            addCriterion("`addon_config_schema` =", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaNotEqualTo(String value) {
            addCriterion("`addon_config_schema` <>", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaGreaterThan(String value) {
            addCriterion("`addon_config_schema` >", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaGreaterThanOrEqualTo(String value) {
            addCriterion("`addon_config_schema` >=", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaLessThan(String value) {
            addCriterion("`addon_config_schema` <", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaLessThanOrEqualTo(String value) {
            addCriterion("`addon_config_schema` <=", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaLike(String value) {
            addCriterion("`addon_config_schema` like", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaNotLike(String value) {
            addCriterion("`addon_config_schema` not like", value, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaIn(List<String> values) {
            addCriterion("`addon_config_schema` in", values, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaNotIn(List<String> values) {
            addCriterion("`addon_config_schema` not in", values, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaBetween(String value1, String value2) {
            addCriterion("`addon_config_schema` between", value1, value2, "addonConfigSchema");
            return (Criteria) this;
        }

        public Criteria andAddonConfigSchemaNotBetween(String value1, String value2) {
            addCriterion("`addon_config_schema` not between", value1, value2, "addonConfigSchema");
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