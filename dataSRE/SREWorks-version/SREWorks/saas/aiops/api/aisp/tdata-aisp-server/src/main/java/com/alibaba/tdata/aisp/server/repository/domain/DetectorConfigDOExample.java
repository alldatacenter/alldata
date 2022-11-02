package com.alibaba.tdata.aisp.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DetectorConfigDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public DetectorConfigDOExample() {
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

        public Criteria andDetectorUrlIsNull() {
            addCriterion("detector_url is null");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlIsNotNull() {
            addCriterion("detector_url is not null");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlEqualTo(String value) {
            addCriterion("detector_url =", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlNotEqualTo(String value) {
            addCriterion("detector_url <>", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlGreaterThan(String value) {
            addCriterion("detector_url >", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlGreaterThanOrEqualTo(String value) {
            addCriterion("detector_url >=", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlLessThan(String value) {
            addCriterion("detector_url <", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlLessThanOrEqualTo(String value) {
            addCriterion("detector_url <=", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlLike(String value) {
            addCriterion("detector_url like", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlNotLike(String value) {
            addCriterion("detector_url not like", value, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlIn(List<String> values) {
            addCriterion("detector_url in", values, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlNotIn(List<String> values) {
            addCriterion("detector_url not in", values, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlBetween(String value1, String value2) {
            addCriterion("detector_url between", value1, value2, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlNotBetween(String value1, String value2) {
            addCriterion("detector_url not between", value1, value2, "detectorUrl");
            return (Criteria) this;
        }

        public Criteria andDetectorCodeLikeInsensitive(String value) {
            addCriterion("upper(detector_code) like", value.toUpperCase(), "detectorCode");
            return (Criteria) this;
        }

        public Criteria andDetectorUrlLikeInsensitive(String value) {
            addCriterion("upper(detector_url) like", value.toUpperCase(), "detectorUrl");
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