package com.alibaba.sreworks.dataset.domain.primary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataModelConfigExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public DataModelConfigExample() {
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

        public Criteria andLabelIsNull() {
            addCriterion("label is null");
            return (Criteria) this;
        }

        public Criteria andLabelIsNotNull() {
            addCriterion("label is not null");
            return (Criteria) this;
        }

        public Criteria andLabelEqualTo(String value) {
            addCriterion("label =", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelNotEqualTo(String value) {
            addCriterion("label <>", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelGreaterThan(String value) {
            addCriterion("label >", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelGreaterThanOrEqualTo(String value) {
            addCriterion("label >=", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelLessThan(String value) {
            addCriterion("label <", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelLessThanOrEqualTo(String value) {
            addCriterion("label <=", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelLike(String value) {
            addCriterion("label like", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelNotLike(String value) {
            addCriterion("label not like", value, "label");
            return (Criteria) this;
        }

        public Criteria andLabelIn(List<String> values) {
            addCriterion("label in", values, "label");
            return (Criteria) this;
        }

        public Criteria andLabelNotIn(List<String> values) {
            addCriterion("label not in", values, "label");
            return (Criteria) this;
        }

        public Criteria andLabelBetween(String value1, String value2) {
            addCriterion("label between", value1, value2, "label");
            return (Criteria) this;
        }

        public Criteria andLabelNotBetween(String value1, String value2) {
            addCriterion("label not between", value1, value2, "label");
            return (Criteria) this;
        }

        public Criteria andBuildInIsNull() {
            addCriterion("build_in is null");
            return (Criteria) this;
        }

        public Criteria andBuildInIsNotNull() {
            addCriterion("build_in is not null");
            return (Criteria) this;
        }

        public Criteria andBuildInEqualTo(Boolean value) {
            addCriterion("build_in =", value, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInNotEqualTo(Boolean value) {
            addCriterion("build_in <>", value, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInGreaterThan(Boolean value) {
            addCriterion("build_in >", value, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInGreaterThanOrEqualTo(Boolean value) {
            addCriterion("build_in >=", value, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInLessThan(Boolean value) {
            addCriterion("build_in <", value, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInLessThanOrEqualTo(Boolean value) {
            addCriterion("build_in <=", value, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInIn(List<Boolean> values) {
            addCriterion("build_in in", values, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInNotIn(List<Boolean> values) {
            addCriterion("build_in not in", values, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInBetween(Boolean value1, Boolean value2) {
            addCriterion("build_in between", value1, value2, "buildIn");
            return (Criteria) this;
        }

        public Criteria andBuildInNotBetween(Boolean value1, Boolean value2) {
            addCriterion("build_in not between", value1, value2, "buildIn");
            return (Criteria) this;
        }

        public Criteria andDomainIdIsNull() {
            addCriterion("domain_id is null");
            return (Criteria) this;
        }

        public Criteria andDomainIdIsNotNull() {
            addCriterion("domain_id is not null");
            return (Criteria) this;
        }

        public Criteria andDomainIdEqualTo(Integer value) {
            addCriterion("domain_id =", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdNotEqualTo(Integer value) {
            addCriterion("domain_id <>", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdGreaterThan(Integer value) {
            addCriterion("domain_id >", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("domain_id >=", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdLessThan(Integer value) {
            addCriterion("domain_id <", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdLessThanOrEqualTo(Integer value) {
            addCriterion("domain_id <=", value, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdIn(List<Integer> values) {
            addCriterion("domain_id in", values, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdNotIn(List<Integer> values) {
            addCriterion("domain_id not in", values, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdBetween(Integer value1, Integer value2) {
            addCriterion("domain_id between", value1, value2, "domainId");
            return (Criteria) this;
        }

        public Criteria andDomainIdNotBetween(Integer value1, Integer value2) {
            addCriterion("domain_id not between", value1, value2, "domainId");
            return (Criteria) this;
        }

        public Criteria andTeamIdIsNull() {
            addCriterion("team_id is null");
            return (Criteria) this;
        }

        public Criteria andTeamIdIsNotNull() {
            addCriterion("team_id is not null");
            return (Criteria) this;
        }

        public Criteria andTeamIdEqualTo(Integer value) {
            addCriterion("team_id =", value, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdNotEqualTo(Integer value) {
            addCriterion("team_id <>", value, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdGreaterThan(Integer value) {
            addCriterion("team_id >", value, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("team_id >=", value, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdLessThan(Integer value) {
            addCriterion("team_id <", value, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdLessThanOrEqualTo(Integer value) {
            addCriterion("team_id <=", value, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdIn(List<Integer> values) {
            addCriterion("team_id in", values, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdNotIn(List<Integer> values) {
            addCriterion("team_id not in", values, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdBetween(Integer value1, Integer value2) {
            addCriterion("team_id between", value1, value2, "teamId");
            return (Criteria) this;
        }

        public Criteria andTeamIdNotBetween(Integer value1, Integer value2) {
            addCriterion("team_id not between", value1, value2, "teamId");
            return (Criteria) this;
        }

        public Criteria andSourceTypeIsNull() {
            addCriterion("source_type is null");
            return (Criteria) this;
        }

        public Criteria andSourceTypeIsNotNull() {
            addCriterion("source_type is not null");
            return (Criteria) this;
        }

        public Criteria andSourceTypeEqualTo(String value) {
            addCriterion("source_type =", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeNotEqualTo(String value) {
            addCriterion("source_type <>", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeGreaterThan(String value) {
            addCriterion("source_type >", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeGreaterThanOrEqualTo(String value) {
            addCriterion("source_type >=", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeLessThan(String value) {
            addCriterion("source_type <", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeLessThanOrEqualTo(String value) {
            addCriterion("source_type <=", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeLike(String value) {
            addCriterion("source_type like", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeNotLike(String value) {
            addCriterion("source_type not like", value, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeIn(List<String> values) {
            addCriterion("source_type in", values, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeNotIn(List<String> values) {
            addCriterion("source_type not in", values, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeBetween(String value1, String value2) {
            addCriterion("source_type between", value1, value2, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceTypeNotBetween(String value1, String value2) {
            addCriterion("source_type not between", value1, value2, "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceIdIsNull() {
            addCriterion("source_id is null");
            return (Criteria) this;
        }

        public Criteria andSourceIdIsNotNull() {
            addCriterion("source_id is not null");
            return (Criteria) this;
        }

        public Criteria andSourceIdEqualTo(String value) {
            addCriterion("source_id =", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdNotEqualTo(String value) {
            addCriterion("source_id <>", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdGreaterThan(String value) {
            addCriterion("source_id >", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdGreaterThanOrEqualTo(String value) {
            addCriterion("source_id >=", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdLessThan(String value) {
            addCriterion("source_id <", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdLessThanOrEqualTo(String value) {
            addCriterion("source_id <=", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdLike(String value) {
            addCriterion("source_id like", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdNotLike(String value) {
            addCriterion("source_id not like", value, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdIn(List<String> values) {
            addCriterion("source_id in", values, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdNotIn(List<String> values) {
            addCriterion("source_id not in", values, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdBetween(String value1, String value2) {
            addCriterion("source_id between", value1, value2, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceIdNotBetween(String value1, String value2) {
            addCriterion("source_id not between", value1, value2, "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceTableIsNull() {
            addCriterion("source_table is null");
            return (Criteria) this;
        }

        public Criteria andSourceTableIsNotNull() {
            addCriterion("source_table is not null");
            return (Criteria) this;
        }

        public Criteria andSourceTableEqualTo(String value) {
            addCriterion("source_table =", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableNotEqualTo(String value) {
            addCriterion("source_table <>", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableGreaterThan(String value) {
            addCriterion("source_table >", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableGreaterThanOrEqualTo(String value) {
            addCriterion("source_table >=", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableLessThan(String value) {
            addCriterion("source_table <", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableLessThanOrEqualTo(String value) {
            addCriterion("source_table <=", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableLike(String value) {
            addCriterion("source_table like", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableNotLike(String value) {
            addCriterion("source_table not like", value, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableIn(List<String> values) {
            addCriterion("source_table in", values, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableNotIn(List<String> values) {
            addCriterion("source_table not in", values, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableBetween(String value1, String value2) {
            addCriterion("source_table between", value1, value2, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andSourceTableNotBetween(String value1, String value2) {
            addCriterion("source_table not between", value1, value2, "sourceTable");
            return (Criteria) this;
        }

        public Criteria andGranularityIsNull() {
            addCriterion("granularity is null");
            return (Criteria) this;
        }

        public Criteria andGranularityIsNotNull() {
            addCriterion("granularity is not null");
            return (Criteria) this;
        }

        public Criteria andGranularityEqualTo(String value) {
            addCriterion("granularity =", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityNotEqualTo(String value) {
            addCriterion("granularity <>", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityGreaterThan(String value) {
            addCriterion("granularity >", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityGreaterThanOrEqualTo(String value) {
            addCriterion("granularity >=", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityLessThan(String value) {
            addCriterion("granularity <", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityLessThanOrEqualTo(String value) {
            addCriterion("granularity <=", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityLike(String value) {
            addCriterion("granularity like", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityNotLike(String value) {
            addCriterion("granularity not like", value, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityIn(List<String> values) {
            addCriterion("granularity in", values, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityNotIn(List<String> values) {
            addCriterion("granularity not in", values, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityBetween(String value1, String value2) {
            addCriterion("granularity between", value1, value2, "granularity");
            return (Criteria) this;
        }

        public Criteria andGranularityNotBetween(String value1, String value2) {
            addCriterion("granularity not between", value1, value2, "granularity");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsIsNull() {
            addCriterion("query_fields is null");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsIsNotNull() {
            addCriterion("query_fields is not null");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsEqualTo(String value) {
            addCriterion("query_fields =", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsNotEqualTo(String value) {
            addCriterion("query_fields <>", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsGreaterThan(String value) {
            addCriterion("query_fields >", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsGreaterThanOrEqualTo(String value) {
            addCriterion("query_fields >=", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsLessThan(String value) {
            addCriterion("query_fields <", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsLessThanOrEqualTo(String value) {
            addCriterion("query_fields <=", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsLike(String value) {
            addCriterion("query_fields like", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsNotLike(String value) {
            addCriterion("query_fields not like", value, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsIn(List<String> values) {
            addCriterion("query_fields in", values, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsNotIn(List<String> values) {
            addCriterion("query_fields not in", values, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsBetween(String value1, String value2) {
            addCriterion("query_fields between", value1, value2, "queryFields");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsNotBetween(String value1, String value2) {
            addCriterion("query_fields not between", value1, value2, "queryFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsIsNull() {
            addCriterion("value_fields is null");
            return (Criteria) this;
        }

        public Criteria andValueFieldsIsNotNull() {
            addCriterion("value_fields is not null");
            return (Criteria) this;
        }

        public Criteria andValueFieldsEqualTo(String value) {
            addCriterion("value_fields =", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsNotEqualTo(String value) {
            addCriterion("value_fields <>", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsGreaterThan(String value) {
            addCriterion("value_fields >", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsGreaterThanOrEqualTo(String value) {
            addCriterion("value_fields >=", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsLessThan(String value) {
            addCriterion("value_fields <", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsLessThanOrEqualTo(String value) {
            addCriterion("value_fields <=", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsLike(String value) {
            addCriterion("value_fields like", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsNotLike(String value) {
            addCriterion("value_fields not like", value, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsIn(List<String> values) {
            addCriterion("value_fields in", values, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsNotIn(List<String> values) {
            addCriterion("value_fields not in", values, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsBetween(String value1, String value2) {
            addCriterion("value_fields between", value1, value2, "valueFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsNotBetween(String value1, String value2) {
            addCriterion("value_fields not between", value1, value2, "valueFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsIsNull() {
            addCriterion("group_fields is null");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsIsNotNull() {
            addCriterion("group_fields is not null");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsEqualTo(String value) {
            addCriterion("group_fields =", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsNotEqualTo(String value) {
            addCriterion("group_fields <>", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsGreaterThan(String value) {
            addCriterion("group_fields >", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsGreaterThanOrEqualTo(String value) {
            addCriterion("group_fields >=", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsLessThan(String value) {
            addCriterion("group_fields <", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsLessThanOrEqualTo(String value) {
            addCriterion("group_fields <=", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsLike(String value) {
            addCriterion("group_fields like", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsNotLike(String value) {
            addCriterion("group_fields not like", value, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsIn(List<String> values) {
            addCriterion("group_fields in", values, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsNotIn(List<String> values) {
            addCriterion("group_fields not in", values, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsBetween(String value1, String value2) {
            addCriterion("group_fields between", value1, value2, "groupFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsNotBetween(String value1, String value2) {
            addCriterion("group_fields not between", value1, value2, "groupFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsIsNull() {
            addCriterion("model_fields is null");
            return (Criteria) this;
        }

        public Criteria andModelFieldsIsNotNull() {
            addCriterion("model_fields is not null");
            return (Criteria) this;
        }

        public Criteria andModelFieldsEqualTo(String value) {
            addCriterion("model_fields =", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsNotEqualTo(String value) {
            addCriterion("model_fields <>", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsGreaterThan(String value) {
            addCriterion("model_fields >", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsGreaterThanOrEqualTo(String value) {
            addCriterion("model_fields >=", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsLessThan(String value) {
            addCriterion("model_fields <", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsLessThanOrEqualTo(String value) {
            addCriterion("model_fields <=", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsLike(String value) {
            addCriterion("model_fields like", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsNotLike(String value) {
            addCriterion("model_fields not like", value, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsIn(List<String> values) {
            addCriterion("model_fields in", values, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsNotIn(List<String> values) {
            addCriterion("model_fields not in", values, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsBetween(String value1, String value2) {
            addCriterion("model_fields between", value1, value2, "modelFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsNotBetween(String value1, String value2) {
            addCriterion("model_fields not between", value1, value2, "modelFields");
            return (Criteria) this;
        }

        public Criteria andNameLikeInsensitive(String value) {
            addCriterion("upper(name) like", value.toUpperCase(), "name");
            return (Criteria) this;
        }

        public Criteria andLabelLikeInsensitive(String value) {
            addCriterion("upper(label) like", value.toUpperCase(), "label");
            return (Criteria) this;
        }

        public Criteria andSourceTypeLikeInsensitive(String value) {
            addCriterion("upper(source_type) like", value.toUpperCase(), "sourceType");
            return (Criteria) this;
        }

        public Criteria andSourceIdLikeInsensitive(String value) {
            addCriterion("upper(source_id) like", value.toUpperCase(), "sourceId");
            return (Criteria) this;
        }

        public Criteria andSourceTableLikeInsensitive(String value) {
            addCriterion("upper(source_table) like", value.toUpperCase(), "sourceTable");
            return (Criteria) this;
        }

        public Criteria andGranularityLikeInsensitive(String value) {
            addCriterion("upper(granularity) like", value.toUpperCase(), "granularity");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsLikeInsensitive(String value) {
            addCriterion("upper(query_fields) like", value.toUpperCase(), "queryFields");
            return (Criteria) this;
        }

        public Criteria andValueFieldsLikeInsensitive(String value) {
            addCriterion("upper(value_fields) like", value.toUpperCase(), "valueFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsLikeInsensitive(String value) {
            addCriterion("upper(group_fields) like", value.toUpperCase(), "groupFields");
            return (Criteria) this;
        }

        public Criteria andModelFieldsLikeInsensitive(String value) {
            addCriterion("upper(model_fields) like", value.toUpperCase(), "modelFields");
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