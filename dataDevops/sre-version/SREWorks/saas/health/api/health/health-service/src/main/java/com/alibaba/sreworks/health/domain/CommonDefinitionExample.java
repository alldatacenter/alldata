package com.alibaba.sreworks.health.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommonDefinitionExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public CommonDefinitionExample() {
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

        public Criteria andIdEqualTo(Integer value) {
            addCriterion("id =", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotEqualTo(Integer value) {
            addCriterion("id <>", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThan(Integer value) {
            addCriterion("id >", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("id >=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThan(Integer value) {
            addCriterion("id <", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdLessThanOrEqualTo(Integer value) {
            addCriterion("id <=", value, "id");
            return (Criteria) this;
        }

        public Criteria andIdIn(List<Integer> values) {
            addCriterion("id in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotIn(List<Integer> values) {
            addCriterion("id not in", values, "id");
            return (Criteria) this;
        }

        public Criteria andIdBetween(Integer value1, Integer value2) {
            addCriterion("id between", value1, value2, "id");
            return (Criteria) this;
        }

        public Criteria andIdNotBetween(Integer value1, Integer value2) {
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

        public Criteria andNameIsNull() {
            addCriterion("name is null");
            return (Criteria) this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("name is not null");
            return (Criteria) this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("name =", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("name <>", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("name >", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("name >=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("name <", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("name <=", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("name like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("name not like", value, "name");
            return (Criteria) this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("name in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("name not in", values, "name");
            return (Criteria) this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("name between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("name not between", value1, value2, "name");
            return (Criteria) this;
        }

        public Criteria andCategoryIsNull() {
            addCriterion("category is null");
            return (Criteria) this;
        }

        public Criteria andCategoryIsNotNull() {
            addCriterion("category is not null");
            return (Criteria) this;
        }

        public Criteria andCategoryEqualTo(String value) {
            addCriterion("category =", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotEqualTo(String value) {
            addCriterion("category <>", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryGreaterThan(String value) {
            addCriterion("category >", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryGreaterThanOrEqualTo(String value) {
            addCriterion("category >=", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryLessThan(String value) {
            addCriterion("category <", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryLessThanOrEqualTo(String value) {
            addCriterion("category <=", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryLike(String value) {
            addCriterion("category like", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotLike(String value) {
            addCriterion("category not like", value, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryIn(List<String> values) {
            addCriterion("category in", values, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotIn(List<String> values) {
            addCriterion("category not in", values, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryBetween(String value1, String value2) {
            addCriterion("category between", value1, value2, "category");
            return (Criteria) this;
        }

        public Criteria andCategoryNotBetween(String value1, String value2) {
            addCriterion("category not between", value1, value2, "category");
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

        public Criteria andAppNameIsNull() {
            addCriterion("app_name is null");
            return (Criteria) this;
        }

        public Criteria andAppNameIsNotNull() {
            addCriterion("app_name is not null");
            return (Criteria) this;
        }

        public Criteria andAppNameEqualTo(String value) {
            addCriterion("app_name =", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameNotEqualTo(String value) {
            addCriterion("app_name <>", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameGreaterThan(String value) {
            addCriterion("app_name >", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameGreaterThanOrEqualTo(String value) {
            addCriterion("app_name >=", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameLessThan(String value) {
            addCriterion("app_name <", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameLessThanOrEqualTo(String value) {
            addCriterion("app_name <=", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameLike(String value) {
            addCriterion("app_name like", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameNotLike(String value) {
            addCriterion("app_name not like", value, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameIn(List<String> values) {
            addCriterion("app_name in", values, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameNotIn(List<String> values) {
            addCriterion("app_name not in", values, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameBetween(String value1, String value2) {
            addCriterion("app_name between", value1, value2, "appName");
            return (Criteria) this;
        }

        public Criteria andAppNameNotBetween(String value1, String value2) {
            addCriterion("app_name not between", value1, value2, "appName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameIsNull() {
            addCriterion("app_component_name is null");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameIsNotNull() {
            addCriterion("app_component_name is not null");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameEqualTo(String value) {
            addCriterion("app_component_name =", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameNotEqualTo(String value) {
            addCriterion("app_component_name <>", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameGreaterThan(String value) {
            addCriterion("app_component_name >", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameGreaterThanOrEqualTo(String value) {
            addCriterion("app_component_name >=", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameLessThan(String value) {
            addCriterion("app_component_name <", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameLessThanOrEqualTo(String value) {
            addCriterion("app_component_name <=", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameLike(String value) {
            addCriterion("app_component_name like", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameNotLike(String value) {
            addCriterion("app_component_name not like", value, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameIn(List<String> values) {
            addCriterion("app_component_name in", values, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameNotIn(List<String> values) {
            addCriterion("app_component_name not in", values, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameBetween(String value1, String value2) {
            addCriterion("app_component_name between", value1, value2, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameNotBetween(String value1, String value2) {
            addCriterion("app_component_name not between", value1, value2, "appComponentName");
            return (Criteria) this;
        }

        public Criteria andMetricIdIsNull() {
            addCriterion("metric_id is null");
            return (Criteria) this;
        }

        public Criteria andMetricIdIsNotNull() {
            addCriterion("metric_id is not null");
            return (Criteria) this;
        }

        public Criteria andMetricIdEqualTo(Integer value) {
            addCriterion("metric_id =", value, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdNotEqualTo(Integer value) {
            addCriterion("metric_id <>", value, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdGreaterThan(Integer value) {
            addCriterion("metric_id >", value, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("metric_id >=", value, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdLessThan(Integer value) {
            addCriterion("metric_id <", value, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdLessThanOrEqualTo(Integer value) {
            addCriterion("metric_id <=", value, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdIn(List<Integer> values) {
            addCriterion("metric_id in", values, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdNotIn(List<Integer> values) {
            addCriterion("metric_id not in", values, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdBetween(Integer value1, Integer value2) {
            addCriterion("metric_id between", value1, value2, "metricId");
            return (Criteria) this;
        }

        public Criteria andMetricIdNotBetween(Integer value1, Integer value2) {
            addCriterion("metric_id not between", value1, value2, "metricId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdIsNull() {
            addCriterion("failure_ref_incident_id is null");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdIsNotNull() {
            addCriterion("failure_ref_incident_id is not null");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdEqualTo(Integer value) {
            addCriterion("failure_ref_incident_id =", value, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdNotEqualTo(Integer value) {
            addCriterion("failure_ref_incident_id <>", value, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdGreaterThan(Integer value) {
            addCriterion("failure_ref_incident_id >", value, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("failure_ref_incident_id >=", value, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdLessThan(Integer value) {
            addCriterion("failure_ref_incident_id <", value, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdLessThanOrEqualTo(Integer value) {
            addCriterion("failure_ref_incident_id <=", value, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdIn(List<Integer> values) {
            addCriterion("failure_ref_incident_id in", values, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdNotIn(List<Integer> values) {
            addCriterion("failure_ref_incident_id not in", values, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdBetween(Integer value1, Integer value2) {
            addCriterion("failure_ref_incident_id between", value1, value2, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andFailureRefIncidentIdNotBetween(Integer value1, Integer value2) {
            addCriterion("failure_ref_incident_id not between", value1, value2, "failureRefIncidentId");
            return (Criteria) this;
        }

        public Criteria andCreatorIsNull() {
            addCriterion("creator is null");
            return (Criteria) this;
        }

        public Criteria andCreatorIsNotNull() {
            addCriterion("creator is not null");
            return (Criteria) this;
        }

        public Criteria andCreatorEqualTo(String value) {
            addCriterion("creator =", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorNotEqualTo(String value) {
            addCriterion("creator <>", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorGreaterThan(String value) {
            addCriterion("creator >", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorGreaterThanOrEqualTo(String value) {
            addCriterion("creator >=", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorLessThan(String value) {
            addCriterion("creator <", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorLessThanOrEqualTo(String value) {
            addCriterion("creator <=", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorLike(String value) {
            addCriterion("creator like", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorNotLike(String value) {
            addCriterion("creator not like", value, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorIn(List<String> values) {
            addCriterion("creator in", values, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorNotIn(List<String> values) {
            addCriterion("creator not in", values, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorBetween(String value1, String value2) {
            addCriterion("creator between", value1, value2, "creator");
            return (Criteria) this;
        }

        public Criteria andCreatorNotBetween(String value1, String value2) {
            addCriterion("creator not between", value1, value2, "creator");
            return (Criteria) this;
        }

        public Criteria andReceiversIsNull() {
            addCriterion("receivers is null");
            return (Criteria) this;
        }

        public Criteria andReceiversIsNotNull() {
            addCriterion("receivers is not null");
            return (Criteria) this;
        }

        public Criteria andReceiversEqualTo(String value) {
            addCriterion("receivers =", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversNotEqualTo(String value) {
            addCriterion("receivers <>", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversGreaterThan(String value) {
            addCriterion("receivers >", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversGreaterThanOrEqualTo(String value) {
            addCriterion("receivers >=", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversLessThan(String value) {
            addCriterion("receivers <", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversLessThanOrEqualTo(String value) {
            addCriterion("receivers <=", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversLike(String value) {
            addCriterion("receivers like", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversNotLike(String value) {
            addCriterion("receivers not like", value, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversIn(List<String> values) {
            addCriterion("receivers in", values, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversNotIn(List<String> values) {
            addCriterion("receivers not in", values, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversBetween(String value1, String value2) {
            addCriterion("receivers between", value1, value2, "receivers");
            return (Criteria) this;
        }

        public Criteria andReceiversNotBetween(String value1, String value2) {
            addCriterion("receivers not between", value1, value2, "receivers");
            return (Criteria) this;
        }

        public Criteria andLastModifierIsNull() {
            addCriterion("last_modifier is null");
            return (Criteria) this;
        }

        public Criteria andLastModifierIsNotNull() {
            addCriterion("last_modifier is not null");
            return (Criteria) this;
        }

        public Criteria andLastModifierEqualTo(String value) {
            addCriterion("last_modifier =", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierNotEqualTo(String value) {
            addCriterion("last_modifier <>", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierGreaterThan(String value) {
            addCriterion("last_modifier >", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierGreaterThanOrEqualTo(String value) {
            addCriterion("last_modifier >=", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierLessThan(String value) {
            addCriterion("last_modifier <", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierLessThanOrEqualTo(String value) {
            addCriterion("last_modifier <=", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierLike(String value) {
            addCriterion("last_modifier like", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierNotLike(String value) {
            addCriterion("last_modifier not like", value, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierIn(List<String> values) {
            addCriterion("last_modifier in", values, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierNotIn(List<String> values) {
            addCriterion("last_modifier not in", values, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierBetween(String value1, String value2) {
            addCriterion("last_modifier between", value1, value2, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andLastModifierNotBetween(String value1, String value2) {
            addCriterion("last_modifier not between", value1, value2, "lastModifier");
            return (Criteria) this;
        }

        public Criteria andExConfigIsNull() {
            addCriterion("ex_config is null");
            return (Criteria) this;
        }

        public Criteria andExConfigIsNotNull() {
            addCriterion("ex_config is not null");
            return (Criteria) this;
        }

        public Criteria andExConfigEqualTo(String value) {
            addCriterion("ex_config =", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigNotEqualTo(String value) {
            addCriterion("ex_config <>", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigGreaterThan(String value) {
            addCriterion("ex_config >", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigGreaterThanOrEqualTo(String value) {
            addCriterion("ex_config >=", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigLessThan(String value) {
            addCriterion("ex_config <", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigLessThanOrEqualTo(String value) {
            addCriterion("ex_config <=", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigLike(String value) {
            addCriterion("ex_config like", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigNotLike(String value) {
            addCriterion("ex_config not like", value, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigIn(List<String> values) {
            addCriterion("ex_config in", values, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigNotIn(List<String> values) {
            addCriterion("ex_config not in", values, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigBetween(String value1, String value2) {
            addCriterion("ex_config between", value1, value2, "exConfig");
            return (Criteria) this;
        }

        public Criteria andExConfigNotBetween(String value1, String value2) {
            addCriterion("ex_config not between", value1, value2, "exConfig");
            return (Criteria) this;
        }

        public Criteria andDescriptionIsNull() {
            addCriterion("description is null");
            return (Criteria) this;
        }

        public Criteria andDescriptionIsNotNull() {
            addCriterion("description is not null");
            return (Criteria) this;
        }

        public Criteria andDescriptionEqualTo(String value) {
            addCriterion("description =", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionNotEqualTo(String value) {
            addCriterion("description <>", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionGreaterThan(String value) {
            addCriterion("description >", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionGreaterThanOrEqualTo(String value) {
            addCriterion("description >=", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionLessThan(String value) {
            addCriterion("description <", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionLessThanOrEqualTo(String value) {
            addCriterion("description <=", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionLike(String value) {
            addCriterion("description like", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionNotLike(String value) {
            addCriterion("description not like", value, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionIn(List<String> values) {
            addCriterion("description in", values, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionNotIn(List<String> values) {
            addCriterion("description not in", values, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionBetween(String value1, String value2) {
            addCriterion("description between", value1, value2, "description");
            return (Criteria) this;
        }

        public Criteria andDescriptionNotBetween(String value1, String value2) {
            addCriterion("description not between", value1, value2, "description");
            return (Criteria) this;
        }

        public Criteria andNameLikeInsensitive(String value) {
            addCriterion("upper(name) like", value.toUpperCase(), "name");
            return (Criteria) this;
        }

        public Criteria andCategoryLikeInsensitive(String value) {
            addCriterion("upper(category) like", value.toUpperCase(), "category");
            return (Criteria) this;
        }

        public Criteria andAppIdLikeInsensitive(String value) {
            addCriterion("upper(app_id) like", value.toUpperCase(), "appId");
            return (Criteria) this;
        }

        public Criteria andAppNameLikeInsensitive(String value) {
            addCriterion("upper(app_name) like", value.toUpperCase(), "appName");
            return (Criteria) this;
        }

        public Criteria andAppComponentNameLikeInsensitive(String value) {
            addCriterion("upper(app_component_name) like", value.toUpperCase(), "appComponentName");
            return (Criteria) this;
        }

        public Criteria andCreatorLikeInsensitive(String value) {
            addCriterion("upper(creator) like", value.toUpperCase(), "creator");
            return (Criteria) this;
        }

        public Criteria andReceiversLikeInsensitive(String value) {
            addCriterion("upper(receivers) like", value.toUpperCase(), "receivers");
            return (Criteria) this;
        }

        public Criteria andLastModifierLikeInsensitive(String value) {
            addCriterion("upper(last_modifier) like", value.toUpperCase(), "lastModifier");
            return (Criteria) this;
        }

        public Criteria andExConfigLikeInsensitive(String value) {
            addCriterion("upper(ex_config) like", value.toUpperCase(), "exConfig");
            return (Criteria) this;
        }

        public Criteria andDescriptionLikeInsensitive(String value) {
            addCriterion("upper(description) like", value.toUpperCase(), "description");
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