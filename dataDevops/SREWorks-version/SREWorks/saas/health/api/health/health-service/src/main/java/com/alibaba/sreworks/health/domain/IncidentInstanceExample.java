package com.alibaba.sreworks.health.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class IncidentInstanceExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public IncidentInstanceExample() {
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

        public Criteria andGmtLastOccurIsNull() {
            addCriterion("gmt_last_occur is null");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurIsNotNull() {
            addCriterion("gmt_last_occur is not null");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurEqualTo(Date value) {
            addCriterion("gmt_last_occur =", value, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurNotEqualTo(Date value) {
            addCriterion("gmt_last_occur <>", value, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurGreaterThan(Date value) {
            addCriterion("gmt_last_occur >", value, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_last_occur >=", value, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurLessThan(Date value) {
            addCriterion("gmt_last_occur <", value, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurLessThanOrEqualTo(Date value) {
            addCriterion("gmt_last_occur <=", value, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurIn(List<Date> values) {
            addCriterion("gmt_last_occur in", values, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurNotIn(List<Date> values) {
            addCriterion("gmt_last_occur not in", values, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurBetween(Date value1, Date value2) {
            addCriterion("gmt_last_occur between", value1, value2, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andGmtLastOccurNotBetween(Date value1, Date value2) {
            addCriterion("gmt_last_occur not between", value1, value2, "gmtLastOccur");
            return (Criteria) this;
        }

        public Criteria andOccurTimesIsNull() {
            addCriterion("occur_times is null");
            return (Criteria) this;
        }

        public Criteria andOccurTimesIsNotNull() {
            addCriterion("occur_times is not null");
            return (Criteria) this;
        }

        public Criteria andOccurTimesEqualTo(Integer value) {
            addCriterion("occur_times =", value, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesNotEqualTo(Integer value) {
            addCriterion("occur_times <>", value, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesGreaterThan(Integer value) {
            addCriterion("occur_times >", value, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesGreaterThanOrEqualTo(Integer value) {
            addCriterion("occur_times >=", value, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesLessThan(Integer value) {
            addCriterion("occur_times <", value, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesLessThanOrEqualTo(Integer value) {
            addCriterion("occur_times <=", value, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesIn(List<Integer> values) {
            addCriterion("occur_times in", values, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesNotIn(List<Integer> values) {
            addCriterion("occur_times not in", values, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesBetween(Integer value1, Integer value2) {
            addCriterion("occur_times between", value1, value2, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andOccurTimesNotBetween(Integer value1, Integer value2) {
            addCriterion("occur_times not between", value1, value2, "occurTimes");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryIsNull() {
            addCriterion("gmt_recovery is null");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryIsNotNull() {
            addCriterion("gmt_recovery is not null");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryEqualTo(Date value) {
            addCriterion("gmt_recovery =", value, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryNotEqualTo(Date value) {
            addCriterion("gmt_recovery <>", value, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryGreaterThan(Date value) {
            addCriterion("gmt_recovery >", value, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_recovery >=", value, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryLessThan(Date value) {
            addCriterion("gmt_recovery <", value, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryLessThanOrEqualTo(Date value) {
            addCriterion("gmt_recovery <=", value, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryIn(List<Date> values) {
            addCriterion("gmt_recovery in", values, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryNotIn(List<Date> values) {
            addCriterion("gmt_recovery not in", values, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryBetween(Date value1, Date value2) {
            addCriterion("gmt_recovery between", value1, value2, "gmtRecovery");
            return (Criteria) this;
        }

        public Criteria andGmtRecoveryNotBetween(Date value1, Date value2) {
            addCriterion("gmt_recovery not between", value1, value2, "gmtRecovery");
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

        public Criteria andTraceIdIsNull() {
            addCriterion("trace_id is null");
            return (Criteria) this;
        }

        public Criteria andTraceIdIsNotNull() {
            addCriterion("trace_id is not null");
            return (Criteria) this;
        }

        public Criteria andTraceIdEqualTo(String value) {
            addCriterion("trace_id =", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdNotEqualTo(String value) {
            addCriterion("trace_id <>", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdGreaterThan(String value) {
            addCriterion("trace_id >", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdGreaterThanOrEqualTo(String value) {
            addCriterion("trace_id >=", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdLessThan(String value) {
            addCriterion("trace_id <", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdLessThanOrEqualTo(String value) {
            addCriterion("trace_id <=", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdLike(String value) {
            addCriterion("trace_id like", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdNotLike(String value) {
            addCriterion("trace_id not like", value, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdIn(List<String> values) {
            addCriterion("trace_id in", values, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdNotIn(List<String> values) {
            addCriterion("trace_id not in", values, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdBetween(String value1, String value2) {
            addCriterion("trace_id between", value1, value2, "traceId");
            return (Criteria) this;
        }

        public Criteria andTraceIdNotBetween(String value1, String value2) {
            addCriterion("trace_id not between", value1, value2, "traceId");
            return (Criteria) this;
        }

        public Criteria andSpanIdIsNull() {
            addCriterion("span_id is null");
            return (Criteria) this;
        }

        public Criteria andSpanIdIsNotNull() {
            addCriterion("span_id is not null");
            return (Criteria) this;
        }

        public Criteria andSpanIdEqualTo(String value) {
            addCriterion("span_id =", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdNotEqualTo(String value) {
            addCriterion("span_id <>", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdGreaterThan(String value) {
            addCriterion("span_id >", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdGreaterThanOrEqualTo(String value) {
            addCriterion("span_id >=", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdLessThan(String value) {
            addCriterion("span_id <", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdLessThanOrEqualTo(String value) {
            addCriterion("span_id <=", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdLike(String value) {
            addCriterion("span_id like", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdNotLike(String value) {
            addCriterion("span_id not like", value, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdIn(List<String> values) {
            addCriterion("span_id in", values, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdNotIn(List<String> values) {
            addCriterion("span_id not in", values, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdBetween(String value1, String value2) {
            addCriterion("span_id between", value1, value2, "spanId");
            return (Criteria) this;
        }

        public Criteria andSpanIdNotBetween(String value1, String value2) {
            addCriterion("span_id not between", value1, value2, "spanId");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartIsNull() {
            addCriterion("gmt_self_healing_start is null");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartIsNotNull() {
            addCriterion("gmt_self_healing_start is not null");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartEqualTo(Date value) {
            addCriterion("gmt_self_healing_start =", value, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartNotEqualTo(Date value) {
            addCriterion("gmt_self_healing_start <>", value, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartGreaterThan(Date value) {
            addCriterion("gmt_self_healing_start >", value, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_self_healing_start >=", value, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartLessThan(Date value) {
            addCriterion("gmt_self_healing_start <", value, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartLessThanOrEqualTo(Date value) {
            addCriterion("gmt_self_healing_start <=", value, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartIn(List<Date> values) {
            addCriterion("gmt_self_healing_start in", values, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartNotIn(List<Date> values) {
            addCriterion("gmt_self_healing_start not in", values, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartBetween(Date value1, Date value2) {
            addCriterion("gmt_self_healing_start between", value1, value2, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingStartNotBetween(Date value1, Date value2) {
            addCriterion("gmt_self_healing_start not between", value1, value2, "gmtSelfHealingStart");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndIsNull() {
            addCriterion("gmt_self_healing_end is null");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndIsNotNull() {
            addCriterion("gmt_self_healing_end is not null");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndEqualTo(Date value) {
            addCriterion("gmt_self_healing_end =", value, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndNotEqualTo(Date value) {
            addCriterion("gmt_self_healing_end <>", value, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndGreaterThan(Date value) {
            addCriterion("gmt_self_healing_end >", value, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_self_healing_end >=", value, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndLessThan(Date value) {
            addCriterion("gmt_self_healing_end <", value, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndLessThanOrEqualTo(Date value) {
            addCriterion("gmt_self_healing_end <=", value, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndIn(List<Date> values) {
            addCriterion("gmt_self_healing_end in", values, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndNotIn(List<Date> values) {
            addCriterion("gmt_self_healing_end not in", values, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndBetween(Date value1, Date value2) {
            addCriterion("gmt_self_healing_end between", value1, value2, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andGmtSelfHealingEndNotBetween(Date value1, Date value2) {
            addCriterion("gmt_self_healing_end not between", value1, value2, "gmtSelfHealingEnd");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusIsNull() {
            addCriterion("self_healing_status is null");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusIsNotNull() {
            addCriterion("self_healing_status is not null");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusEqualTo(String value) {
            addCriterion("self_healing_status =", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusNotEqualTo(String value) {
            addCriterion("self_healing_status <>", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusGreaterThan(String value) {
            addCriterion("self_healing_status >", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusGreaterThanOrEqualTo(String value) {
            addCriterion("self_healing_status >=", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusLessThan(String value) {
            addCriterion("self_healing_status <", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusLessThanOrEqualTo(String value) {
            addCriterion("self_healing_status <=", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusLike(String value) {
            addCriterion("self_healing_status like", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusNotLike(String value) {
            addCriterion("self_healing_status not like", value, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusIn(List<String> values) {
            addCriterion("self_healing_status in", values, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusNotIn(List<String> values) {
            addCriterion("self_healing_status not in", values, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusBetween(String value1, String value2) {
            addCriterion("self_healing_status between", value1, value2, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusNotBetween(String value1, String value2) {
            addCriterion("self_healing_status not between", value1, value2, "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andOptionsIsNull() {
            addCriterion("options is null");
            return (Criteria) this;
        }

        public Criteria andOptionsIsNotNull() {
            addCriterion("options is not null");
            return (Criteria) this;
        }

        public Criteria andOptionsEqualTo(String value) {
            addCriterion("options =", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsNotEqualTo(String value) {
            addCriterion("options <>", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsGreaterThan(String value) {
            addCriterion("options >", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsGreaterThanOrEqualTo(String value) {
            addCriterion("options >=", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsLessThan(String value) {
            addCriterion("options <", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsLessThanOrEqualTo(String value) {
            addCriterion("options <=", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsLike(String value) {
            addCriterion("options like", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsNotLike(String value) {
            addCriterion("options not like", value, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsIn(List<String> values) {
            addCriterion("options in", values, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsNotIn(List<String> values) {
            addCriterion("options not in", values, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsBetween(String value1, String value2) {
            addCriterion("options between", value1, value2, "options");
            return (Criteria) this;
        }

        public Criteria andOptionsNotBetween(String value1, String value2) {
            addCriterion("options not between", value1, value2, "options");
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

        public Criteria andTraceIdLikeInsensitive(String value) {
            addCriterion("upper(trace_id) like", value.toUpperCase(), "traceId");
            return (Criteria) this;
        }

        public Criteria andSpanIdLikeInsensitive(String value) {
            addCriterion("upper(span_id) like", value.toUpperCase(), "spanId");
            return (Criteria) this;
        }

        public Criteria andSelfHealingStatusLikeInsensitive(String value) {
            addCriterion("upper(self_healing_status) like", value.toUpperCase(), "selfHealingStatus");
            return (Criteria) this;
        }

        public Criteria andOptionsLikeInsensitive(String value) {
            addCriterion("upper(options) like", value.toUpperCase(), "options");
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