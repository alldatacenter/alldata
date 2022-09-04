package com.alibaba.tdata.aisp.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TaskDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public TaskDOExample() {
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

        public Criteria andTaskUuidIsNull() {
            addCriterion("task_uuid is null");
            return (Criteria) this;
        }

        public Criteria andTaskUuidIsNotNull() {
            addCriterion("task_uuid is not null");
            return (Criteria) this;
        }

        public Criteria andTaskUuidEqualTo(String value) {
            addCriterion("task_uuid =", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidNotEqualTo(String value) {
            addCriterion("task_uuid <>", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidGreaterThan(String value) {
            addCriterion("task_uuid >", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidGreaterThanOrEqualTo(String value) {
            addCriterion("task_uuid >=", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidLessThan(String value) {
            addCriterion("task_uuid <", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidLessThanOrEqualTo(String value) {
            addCriterion("task_uuid <=", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidLike(String value) {
            addCriterion("task_uuid like", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidNotLike(String value) {
            addCriterion("task_uuid not like", value, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidIn(List<String> values) {
            addCriterion("task_uuid in", values, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidNotIn(List<String> values) {
            addCriterion("task_uuid not in", values, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidBetween(String value1, String value2) {
            addCriterion("task_uuid between", value1, value2, "taskUuid");
            return (Criteria) this;
        }

        public Criteria andTaskUuidNotBetween(String value1, String value2) {
            addCriterion("task_uuid not between", value1, value2, "taskUuid");
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

        public Criteria andCostTimeIsNull() {
            addCriterion("cost_time is null");
            return (Criteria) this;
        }

        public Criteria andCostTimeIsNotNull() {
            addCriterion("cost_time is not null");
            return (Criteria) this;
        }

        public Criteria andCostTimeEqualTo(Long value) {
            addCriterion("cost_time =", value, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeNotEqualTo(Long value) {
            addCriterion("cost_time <>", value, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeGreaterThan(Long value) {
            addCriterion("cost_time >", value, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeGreaterThanOrEqualTo(Long value) {
            addCriterion("cost_time >=", value, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeLessThan(Long value) {
            addCriterion("cost_time <", value, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeLessThanOrEqualTo(Long value) {
            addCriterion("cost_time <=", value, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeIn(List<Long> values) {
            addCriterion("cost_time in", values, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeNotIn(List<Long> values) {
            addCriterion("cost_time not in", values, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeBetween(Long value1, Long value2) {
            addCriterion("cost_time between", value1, value2, "costTime");
            return (Criteria) this;
        }

        public Criteria andCostTimeNotBetween(Long value1, Long value2) {
            addCriterion("cost_time not between", value1, value2, "costTime");
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

        public Criteria andTaskTypeIsNull() {
            addCriterion("task_type is null");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIsNotNull() {
            addCriterion("task_type is not null");
            return (Criteria) this;
        }

        public Criteria andTaskTypeEqualTo(String value) {
            addCriterion("task_type =", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotEqualTo(String value) {
            addCriterion("task_type <>", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeGreaterThan(String value) {
            addCriterion("task_type >", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeGreaterThanOrEqualTo(String value) {
            addCriterion("task_type >=", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLessThan(String value) {
            addCriterion("task_type <", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLessThanOrEqualTo(String value) {
            addCriterion("task_type <=", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLike(String value) {
            addCriterion("task_type like", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotLike(String value) {
            addCriterion("task_type not like", value, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeIn(List<String> values) {
            addCriterion("task_type in", values, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotIn(List<String> values) {
            addCriterion("task_type not in", values, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeBetween(String value1, String value2) {
            addCriterion("task_type between", value1, value2, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskTypeNotBetween(String value1, String value2) {
            addCriterion("task_type not between", value1, value2, "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskStatusIsNull() {
            addCriterion("task_status is null");
            return (Criteria) this;
        }

        public Criteria andTaskStatusIsNotNull() {
            addCriterion("task_status is not null");
            return (Criteria) this;
        }

        public Criteria andTaskStatusEqualTo(String value) {
            addCriterion("task_status =", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotEqualTo(String value) {
            addCriterion("task_status <>", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusGreaterThan(String value) {
            addCriterion("task_status >", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusGreaterThanOrEqualTo(String value) {
            addCriterion("task_status >=", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLessThan(String value) {
            addCriterion("task_status <", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLessThanOrEqualTo(String value) {
            addCriterion("task_status <=", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLike(String value) {
            addCriterion("task_status like", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotLike(String value) {
            addCriterion("task_status not like", value, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusIn(List<String> values) {
            addCriterion("task_status in", values, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotIn(List<String> values) {
            addCriterion("task_status not in", values, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusBetween(String value1, String value2) {
            addCriterion("task_status between", value1, value2, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskStatusNotBetween(String value1, String value2) {
            addCriterion("task_status not between", value1, value2, "taskStatus");
            return (Criteria) this;
        }

        public Criteria andTaskUuidLikeInsensitive(String value) {
            addCriterion("upper(task_uuid) like", value.toUpperCase(), "taskUuid");
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

        public Criteria andInstanceCodeLikeInsensitive(String value) {
            addCriterion("upper(instance_code) like", value.toUpperCase(), "instanceCode");
            return (Criteria) this;
        }

        public Criteria andTaskTypeLikeInsensitive(String value) {
            addCriterion("upper(task_type) like", value.toUpperCase(), "taskType");
            return (Criteria) this;
        }

        public Criteria andTaskStatusLikeInsensitive(String value) {
            addCriterion("upper(task_status) like", value.toUpperCase(), "taskStatus");
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