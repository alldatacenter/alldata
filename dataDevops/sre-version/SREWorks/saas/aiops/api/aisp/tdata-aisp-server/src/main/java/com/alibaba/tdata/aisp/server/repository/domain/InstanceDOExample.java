package com.alibaba.tdata.aisp.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InstanceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public InstanceDOExample() {
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

        public Criteria andInstanceCodeIsNull() {
            addCriterion("instance_code is null");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeIsNotNull() {
            addCriterion("instance_code is not null");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeEqualTo(String value) {
            addCriterion("instance_code =", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeNotEqualTo(String value) {
            addCriterion("instance_code <>", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeGreaterThan(String value) {
            addCriterion("instance_code >", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeGreaterThanOrEqualTo(String value) {
            addCriterion("instance_code >=", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeLessThan(String value) {
            addCriterion("instance_code <", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeLessThanOrEqualTo(String value) {
            addCriterion("instance_code <=", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeLike(String value) {
            addCriterion("instance_code like", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeNotLike(String value) {
            addCriterion("instance_code not like", value, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeIn(List<String> values) {
            addCriterion("instance_code in", values, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeNotIn(List<String> values) {
            addCriterion("instance_code not in", values, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeBetween(String value1, String value2) {
            addCriterion("instance_code between", value1, value2, "instanceCode");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeNotBetween(String value1, String value2) {
            addCriterion("instance_code not between", value1, value2, "instanceCode");
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

        public Criteria andSceneCodeIsNull() {
            addCriterion("scene_code is null");
            return (Criteria) this;
        }

        public Criteria andSceneCodeIsNotNull() {
            addCriterion("scene_code is not null");
            return (Criteria) this;
        }

        public Criteria andSceneCodeEqualTo(String value) {
            addCriterion("scene_code =", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeNotEqualTo(String value) {
            addCriterion("scene_code <>", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeGreaterThan(String value) {
            addCriterion("scene_code >", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeGreaterThanOrEqualTo(String value) {
            addCriterion("scene_code >=", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeLessThan(String value) {
            addCriterion("scene_code <", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeLessThanOrEqualTo(String value) {
            addCriterion("scene_code <=", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeLike(String value) {
            addCriterion("scene_code like", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeNotLike(String value) {
            addCriterion("scene_code not like", value, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeIn(List<String> values) {
            addCriterion("scene_code in", values, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeNotIn(List<String> values) {
            addCriterion("scene_code not in", values, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeBetween(String value1, String value2) {
            addCriterion("scene_code between", value1, value2, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeNotBetween(String value1, String value2) {
            addCriterion("scene_code not between", value1, value2, "sceneCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeIsNull() {
            addCriterion("detector_code is null");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeIsNotNull() {
            addCriterion("detector_code is not null");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeEqualTo(String value) {
            addCriterion("detector_code =", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeNotEqualTo(String value) {
            addCriterion("detector_code <>", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeGreaterThan(String value) {
            addCriterion("detector_code >", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeGreaterThanOrEqualTo(String value) {
            addCriterion("detector_code >=", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeLessThan(String value) {
            addCriterion("detector_code <", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeLessThanOrEqualTo(String value) {
            addCriterion("detector_code <=", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeLike(String value) {
            addCriterion("detector_code like", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeNotLike(String value) {
            addCriterion("detector_code not like", value, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeIn(List<String> values) {
            addCriterion("detector_code in", values, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeNotIn(List<String> values) {
            addCriterion("detector_code not in", values, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeBetween(String value1, String value2) {
            addCriterion("detector_code between", value1, value2, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeNotBetween(String value1, String value2) {
            addCriterion("detector_code not between", value1, value2, "detectorCode");
            return (Criteria) this;
        }

        public Criteria andEntityIdIsNull() {
            addCriterion("entity_id is null");
            return (Criteria) this;
        }

        public Criteria andEntityIdIsNotNull() {
            addCriterion("entity_id is not null");
            return (Criteria) this;
        }

        public Criteria andEntityIdEqualTo(String value) {
            addCriterion("entity_id =", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdNotEqualTo(String value) {
            addCriterion("entity_id <>", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdGreaterThan(String value) {
            addCriterion("entity_id >", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdGreaterThanOrEqualTo(String value) {
            addCriterion("entity_id >=", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdLessThan(String value) {
            addCriterion("entity_id <", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdLessThanOrEqualTo(String value) {
            addCriterion("entity_id <=", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdLike(String value) {
            addCriterion("entity_id like", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdNotLike(String value) {
            addCriterion("entity_id not like", value, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdIn(List<String> values) {
            addCriterion("entity_id in", values, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdNotIn(List<String> values) {
            addCriterion("entity_id not in", values, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdBetween(String value1, String value2) {
            addCriterion("entity_id between", value1, value2, "entityId");
            return (Criteria) this;
        }

        public Criteria andEntityIdNotBetween(String value1, String value2) {
            addCriterion("entity_id not between", value1, value2, "entityId");
            return (Criteria) this;
        }

        public Criteria andInstanceCodeLikeInsensitive(String value) {
            addCriterion("upper(instance_code) like", value.toUpperCase(), "instanceCode");
            return (Criteria) this;
        }

        public Criteria andSceneCodeLikeInsensitive(String value) {
            addCriterion("upper(scene_code) like", value.toUpperCase(), "sceneCode");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeLikeInsensitive(String value) {
            addCriterion("upper(detector_code) like", value.toUpperCase(), "detectorCode");
            return (Criteria) this;
        }

        public Criteria andEntityIdLikeInsensitive(String value) {
            addCriterion("upper(entity_id) like", value.toUpperCase(), "entityId");
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