package com.alibaba.sreworks.health.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RiskInstanceExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public RiskInstanceExample() {
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

        public Criteria andDefIdIsNull() {
            addCriterion("def_id is null");
            return (Criteria) this;
        }

        public Criteria andDefIdIsNotNull() {
            addCriterion("def_id is not null");
            return (Criteria) this;
        }

        public Criteria andDefIdEqualTo(Integer value) {
            addCriterion("def_id =", value, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdNotEqualTo(Integer value) {
            addCriterion("def_id <>", value, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdGreaterThan(Integer value) {
            addCriterion("def_id >", value, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("def_id >=", value, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdLessThan(Integer value) {
            addCriterion("def_id <", value, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdLessThanOrEqualTo(Integer value) {
            addCriterion("def_id <=", value, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdIn(List<Integer> values) {
            addCriterion("def_id in", values, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdNotIn(List<Integer> values) {
            addCriterion("def_id not in", values, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdBetween(Integer value1, Integer value2) {
            addCriterion("def_id between", value1, value2, "defId");
            return (Criteria) this;
        }

        public Criteria andDefIdNotBetween(Integer value1, Integer value2) {
            addCriterion("def_id not between", value1, value2, "defId");
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

        public Criteria andAppComponentInstanceIdIsNull() {
            addCriterion("app_component_instance_id is null");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdIsNotNull() {
            addCriterion("app_component_instance_id is not null");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdEqualTo(String value) {
            addCriterion("app_component_instance_id =", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdNotEqualTo(String value) {
            addCriterion("app_component_instance_id <>", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdGreaterThan(String value) {
            addCriterion("app_component_instance_id >", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdGreaterThanOrEqualTo(String value) {
            addCriterion("app_component_instance_id >=", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdLessThan(String value) {
            addCriterion("app_component_instance_id <", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdLessThanOrEqualTo(String value) {
            addCriterion("app_component_instance_id <=", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdLike(String value) {
            addCriterion("app_component_instance_id like", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdNotLike(String value) {
            addCriterion("app_component_instance_id not like", value, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdIn(List<String> values) {
            addCriterion("app_component_instance_id in", values, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdNotIn(List<String> values) {
            addCriterion("app_component_instance_id not in", values, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdBetween(String value1, String value2) {
            addCriterion("app_component_instance_id between", value1, value2, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdNotBetween(String value1, String value2) {
            addCriterion("app_component_instance_id not between", value1, value2, "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andGmtOccurIsNull() {
            addCriterion("gmt_occur is null");
            return (Criteria) this;
        }

        public Criteria andGmtOccurIsNotNull() {
            addCriterion("gmt_occur is not null");
            return (Criteria) this;
        }

        public Criteria andGmtOccurEqualTo(Date value) {
            addCriterion("gmt_occur =", value, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurNotEqualTo(Date value) {
            addCriterion("gmt_occur <>", value, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurGreaterThan(Date value) {
            addCriterion("gmt_occur >", value, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_occur >=", value, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurLessThan(Date value) {
            addCriterion("gmt_occur <", value, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurLessThanOrEqualTo(Date value) {
            addCriterion("gmt_occur <=", value, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurIn(List<Date> values) {
            addCriterion("gmt_occur in", values, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurNotIn(List<Date> values) {
            addCriterion("gmt_occur not in", values, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurBetween(Date value1, Date value2) {
            addCriterion("gmt_occur between", value1, value2, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andGmtOccurNotBetween(Date value1, Date value2) {
            addCriterion("gmt_occur not between", value1, value2, "gmtOccur");
            return (Criteria) this;
        }

        public Criteria andSourceIsNull() {
            addCriterion("source is null");
            return (Criteria) this;
        }

        public Criteria andSourceIsNotNull() {
            addCriterion("source is not null");
            return (Criteria) this;
        }

        public Criteria andSourceEqualTo(String value) {
            addCriterion("source =", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceNotEqualTo(String value) {
            addCriterion("source <>", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceGreaterThan(String value) {
            addCriterion("source >", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceGreaterThanOrEqualTo(String value) {
            addCriterion("source >=", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceLessThan(String value) {
            addCriterion("source <", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceLessThanOrEqualTo(String value) {
            addCriterion("source <=", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceLike(String value) {
            addCriterion("source like", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceNotLike(String value) {
            addCriterion("source not like", value, "source");
            return (Criteria) this;
        }

        public Criteria andSourceIn(List<String> values) {
            addCriterion("source in", values, "source");
            return (Criteria) this;
        }

        public Criteria andSourceNotIn(List<String> values) {
            addCriterion("source not in", values, "source");
            return (Criteria) this;
        }

        public Criteria andSourceBetween(String value1, String value2) {
            addCriterion("source between", value1, value2, "source");
            return (Criteria) this;
        }

        public Criteria andSourceNotBetween(String value1, String value2) {
            addCriterion("source not between", value1, value2, "source");
            return (Criteria) this;
        }

        public Criteria andAppInstanceIdLikeInsensitive(String value) {
            addCriterion("upper(app_instance_id) like", value.toUpperCase(), "appInstanceId");
            return (Criteria) this;
        }

        public Criteria andAppComponentInstanceIdLikeInsensitive(String value) {
            addCriterion("upper(app_component_instance_id) like", value.toUpperCase(), "appComponentInstanceId");
            return (Criteria) this;
        }

        public Criteria andSourceLikeInsensitive(String value) {
            addCriterion("upper(source) like", value.toUpperCase(), "source");
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