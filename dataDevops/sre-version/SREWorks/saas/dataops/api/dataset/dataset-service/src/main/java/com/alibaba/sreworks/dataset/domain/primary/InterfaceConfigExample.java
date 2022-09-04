package com.alibaba.sreworks.dataset.domain.primary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InterfaceConfigExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public InterfaceConfigExample() {
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

        public Criteria andAliasIsNull() {
            addCriterion("alias is null");
            return (Criteria) this;
        }

        public Criteria andAliasIsNotNull() {
            addCriterion("alias is not null");
            return (Criteria) this;
        }

        public Criteria andAliasEqualTo(String value) {
            addCriterion("alias =", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasNotEqualTo(String value) {
            addCriterion("alias <>", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasGreaterThan(String value) {
            addCriterion("alias >", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasGreaterThanOrEqualTo(String value) {
            addCriterion("alias >=", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasLessThan(String value) {
            addCriterion("alias <", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasLessThanOrEqualTo(String value) {
            addCriterion("alias <=", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasLike(String value) {
            addCriterion("alias like", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasNotLike(String value) {
            addCriterion("alias not like", value, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasIn(List<String> values) {
            addCriterion("alias in", values, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasNotIn(List<String> values) {
            addCriterion("alias not in", values, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasBetween(String value1, String value2) {
            addCriterion("alias between", value1, value2, "alias");
            return (Criteria) this;
        }

        public Criteria andAliasNotBetween(String value1, String value2) {
            addCriterion("alias not between", value1, value2, "alias");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeIsNull() {
            addCriterion("data_source_type is null");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeIsNotNull() {
            addCriterion("data_source_type is not null");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeEqualTo(String value) {
            addCriterion("data_source_type =", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeNotEqualTo(String value) {
            addCriterion("data_source_type <>", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeGreaterThan(String value) {
            addCriterion("data_source_type >", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeGreaterThanOrEqualTo(String value) {
            addCriterion("data_source_type >=", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeLessThan(String value) {
            addCriterion("data_source_type <", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeLessThanOrEqualTo(String value) {
            addCriterion("data_source_type <=", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeLike(String value) {
            addCriterion("data_source_type like", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeNotLike(String value) {
            addCriterion("data_source_type not like", value, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeIn(List<String> values) {
            addCriterion("data_source_type in", values, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeNotIn(List<String> values) {
            addCriterion("data_source_type not in", values, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeBetween(String value1, String value2) {
            addCriterion("data_source_type between", value1, value2, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeNotBetween(String value1, String value2) {
            addCriterion("data_source_type not between", value1, value2, "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdIsNull() {
            addCriterion("data_source_id is null");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdIsNotNull() {
            addCriterion("data_source_id is not null");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdEqualTo(String value) {
            addCriterion("data_source_id =", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdNotEqualTo(String value) {
            addCriterion("data_source_id <>", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdGreaterThan(String value) {
            addCriterion("data_source_id >", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdGreaterThanOrEqualTo(String value) {
            addCriterion("data_source_id >=", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdLessThan(String value) {
            addCriterion("data_source_id <", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdLessThanOrEqualTo(String value) {
            addCriterion("data_source_id <=", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdLike(String value) {
            addCriterion("data_source_id like", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdNotLike(String value) {
            addCriterion("data_source_id not like", value, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdIn(List<String> values) {
            addCriterion("data_source_id in", values, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdNotIn(List<String> values) {
            addCriterion("data_source_id not in", values, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdBetween(String value1, String value2) {
            addCriterion("data_source_id between", value1, value2, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdNotBetween(String value1, String value2) {
            addCriterion("data_source_id not between", value1, value2, "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableIsNull() {
            addCriterion("data_source_table is null");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableIsNotNull() {
            addCriterion("data_source_table is not null");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableEqualTo(String value) {
            addCriterion("data_source_table =", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableNotEqualTo(String value) {
            addCriterion("data_source_table <>", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableGreaterThan(String value) {
            addCriterion("data_source_table >", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableGreaterThanOrEqualTo(String value) {
            addCriterion("data_source_table >=", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableLessThan(String value) {
            addCriterion("data_source_table <", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableLessThanOrEqualTo(String value) {
            addCriterion("data_source_table <=", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableLike(String value) {
            addCriterion("data_source_table like", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableNotLike(String value) {
            addCriterion("data_source_table not like", value, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableIn(List<String> values) {
            addCriterion("data_source_table in", values, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableNotIn(List<String> values) {
            addCriterion("data_source_table not in", values, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableBetween(String value1, String value2) {
            addCriterion("data_source_table between", value1, value2, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableNotBetween(String value1, String value2) {
            addCriterion("data_source_table not between", value1, value2, "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andModeIsNull() {
            addCriterion("mode is null");
            return (Criteria) this;
        }

        public Criteria andModeIsNotNull() {
            addCriterion("mode is not null");
            return (Criteria) this;
        }

        public Criteria andModeEqualTo(String value) {
            addCriterion("mode =", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeNotEqualTo(String value) {
            addCriterion("mode <>", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeGreaterThan(String value) {
            addCriterion("mode >", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeGreaterThanOrEqualTo(String value) {
            addCriterion("mode >=", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeLessThan(String value) {
            addCriterion("mode <", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeLessThanOrEqualTo(String value) {
            addCriterion("mode <=", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeLike(String value) {
            addCriterion("mode like", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeNotLike(String value) {
            addCriterion("mode not like", value, "mode");
            return (Criteria) this;
        }

        public Criteria andModeIn(List<String> values) {
            addCriterion("mode in", values, "mode");
            return (Criteria) this;
        }

        public Criteria andModeNotIn(List<String> values) {
            addCriterion("mode not in", values, "mode");
            return (Criteria) this;
        }

        public Criteria andModeBetween(String value1, String value2) {
            addCriterion("mode between", value1, value2, "mode");
            return (Criteria) this;
        }

        public Criteria andModeNotBetween(String value1, String value2) {
            addCriterion("mode not between", value1, value2, "mode");
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

        public Criteria andSortFieldsIsNull() {
            addCriterion("sort_fields is null");
            return (Criteria) this;
        }

        public Criteria andSortFieldsIsNotNull() {
            addCriterion("sort_fields is not null");
            return (Criteria) this;
        }

        public Criteria andSortFieldsEqualTo(String value) {
            addCriterion("sort_fields =", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsNotEqualTo(String value) {
            addCriterion("sort_fields <>", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsGreaterThan(String value) {
            addCriterion("sort_fields >", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsGreaterThanOrEqualTo(String value) {
            addCriterion("sort_fields >=", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsLessThan(String value) {
            addCriterion("sort_fields <", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsLessThanOrEqualTo(String value) {
            addCriterion("sort_fields <=", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsLike(String value) {
            addCriterion("sort_fields like", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsNotLike(String value) {
            addCriterion("sort_fields not like", value, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsIn(List<String> values) {
            addCriterion("sort_fields in", values, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsNotIn(List<String> values) {
            addCriterion("sort_fields not in", values, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsBetween(String value1, String value2) {
            addCriterion("sort_fields between", value1, value2, "sortFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsNotBetween(String value1, String value2) {
            addCriterion("sort_fields not between", value1, value2, "sortFields");
            return (Criteria) this;
        }

        public Criteria andRequestParamsIsNull() {
            addCriterion("request_params is null");
            return (Criteria) this;
        }

        public Criteria andRequestParamsIsNotNull() {
            addCriterion("request_params is not null");
            return (Criteria) this;
        }

        public Criteria andRequestParamsEqualTo(String value) {
            addCriterion("request_params =", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsNotEqualTo(String value) {
            addCriterion("request_params <>", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsGreaterThan(String value) {
            addCriterion("request_params >", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsGreaterThanOrEqualTo(String value) {
            addCriterion("request_params >=", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsLessThan(String value) {
            addCriterion("request_params <", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsLessThanOrEqualTo(String value) {
            addCriterion("request_params <=", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsLike(String value) {
            addCriterion("request_params like", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsNotLike(String value) {
            addCriterion("request_params not like", value, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsIn(List<String> values) {
            addCriterion("request_params in", values, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsNotIn(List<String> values) {
            addCriterion("request_params not in", values, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsBetween(String value1, String value2) {
            addCriterion("request_params between", value1, value2, "requestParams");
            return (Criteria) this;
        }

        public Criteria andRequestParamsNotBetween(String value1, String value2) {
            addCriterion("request_params not between", value1, value2, "requestParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsIsNull() {
            addCriterion("response_params is null");
            return (Criteria) this;
        }

        public Criteria andResponseParamsIsNotNull() {
            addCriterion("response_params is not null");
            return (Criteria) this;
        }

        public Criteria andResponseParamsEqualTo(String value) {
            addCriterion("response_params =", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsNotEqualTo(String value) {
            addCriterion("response_params <>", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsGreaterThan(String value) {
            addCriterion("response_params >", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsGreaterThanOrEqualTo(String value) {
            addCriterion("response_params >=", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsLessThan(String value) {
            addCriterion("response_params <", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsLessThanOrEqualTo(String value) {
            addCriterion("response_params <=", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsLike(String value) {
            addCriterion("response_params like", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsNotLike(String value) {
            addCriterion("response_params not like", value, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsIn(List<String> values) {
            addCriterion("response_params in", values, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsNotIn(List<String> values) {
            addCriterion("response_params not in", values, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsBetween(String value1, String value2) {
            addCriterion("response_params between", value1, value2, "responseParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsNotBetween(String value1, String value2) {
            addCriterion("response_params not between", value1, value2, "responseParams");
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

        public Criteria andRequestMethodIsNull() {
            addCriterion("request_method is null");
            return (Criteria) this;
        }

        public Criteria andRequestMethodIsNotNull() {
            addCriterion("request_method is not null");
            return (Criteria) this;
        }

        public Criteria andRequestMethodEqualTo(String value) {
            addCriterion("request_method =", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodNotEqualTo(String value) {
            addCriterion("request_method <>", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodGreaterThan(String value) {
            addCriterion("request_method >", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodGreaterThanOrEqualTo(String value) {
            addCriterion("request_method >=", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodLessThan(String value) {
            addCriterion("request_method <", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodLessThanOrEqualTo(String value) {
            addCriterion("request_method <=", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodLike(String value) {
            addCriterion("request_method like", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodNotLike(String value) {
            addCriterion("request_method not like", value, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodIn(List<String> values) {
            addCriterion("request_method in", values, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodNotIn(List<String> values) {
            addCriterion("request_method not in", values, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodBetween(String value1, String value2) {
            addCriterion("request_method between", value1, value2, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andRequestMethodNotBetween(String value1, String value2) {
            addCriterion("request_method not between", value1, value2, "requestMethod");
            return (Criteria) this;
        }

        public Criteria andContentTypeIsNull() {
            addCriterion("content_type is null");
            return (Criteria) this;
        }

        public Criteria andContentTypeIsNotNull() {
            addCriterion("content_type is not null");
            return (Criteria) this;
        }

        public Criteria andContentTypeEqualTo(String value) {
            addCriterion("content_type =", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeNotEqualTo(String value) {
            addCriterion("content_type <>", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeGreaterThan(String value) {
            addCriterion("content_type >", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeGreaterThanOrEqualTo(String value) {
            addCriterion("content_type >=", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeLessThan(String value) {
            addCriterion("content_type <", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeLessThanOrEqualTo(String value) {
            addCriterion("content_type <=", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeLike(String value) {
            addCriterion("content_type like", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeNotLike(String value) {
            addCriterion("content_type not like", value, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeIn(List<String> values) {
            addCriterion("content_type in", values, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeNotIn(List<String> values) {
            addCriterion("content_type not in", values, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeBetween(String value1, String value2) {
            addCriterion("content_type between", value1, value2, "contentType");
            return (Criteria) this;
        }

        public Criteria andContentTypeNotBetween(String value1, String value2) {
            addCriterion("content_type not between", value1, value2, "contentType");
            return (Criteria) this;
        }

        public Criteria andPagingIsNull() {
            addCriterion("paging is null");
            return (Criteria) this;
        }

        public Criteria andPagingIsNotNull() {
            addCriterion("paging is not null");
            return (Criteria) this;
        }

        public Criteria andPagingEqualTo(Boolean value) {
            addCriterion("paging =", value, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingNotEqualTo(Boolean value) {
            addCriterion("paging <>", value, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingGreaterThan(Boolean value) {
            addCriterion("paging >", value, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingGreaterThanOrEqualTo(Boolean value) {
            addCriterion("paging >=", value, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingLessThan(Boolean value) {
            addCriterion("paging <", value, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingLessThanOrEqualTo(Boolean value) {
            addCriterion("paging <=", value, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingIn(List<Boolean> values) {
            addCriterion("paging in", values, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingNotIn(List<Boolean> values) {
            addCriterion("paging not in", values, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingBetween(Boolean value1, Boolean value2) {
            addCriterion("paging between", value1, value2, "paging");
            return (Criteria) this;
        }

        public Criteria andPagingNotBetween(Boolean value1, Boolean value2) {
            addCriterion("paging not between", value1, value2, "paging");
            return (Criteria) this;
        }

        public Criteria andNameLikeInsensitive(String value) {
            addCriterion("upper(name) like", value.toUpperCase(), "name");
            return (Criteria) this;
        }

        public Criteria andAliasLikeInsensitive(String value) {
            addCriterion("upper(alias) like", value.toUpperCase(), "alias");
            return (Criteria) this;
        }

        public Criteria andDataSourceTypeLikeInsensitive(String value) {
            addCriterion("upper(data_source_type) like", value.toUpperCase(), "dataSourceType");
            return (Criteria) this;
        }

        public Criteria andDataSourceIdLikeInsensitive(String value) {
            addCriterion("upper(data_source_id) like", value.toUpperCase(), "dataSourceId");
            return (Criteria) this;
        }

        public Criteria andDataSourceTableLikeInsensitive(String value) {
            addCriterion("upper(data_source_table) like", value.toUpperCase(), "dataSourceTable");
            return (Criteria) this;
        }

        public Criteria andModeLikeInsensitive(String value) {
            addCriterion("upper(mode) like", value.toUpperCase(), "mode");
            return (Criteria) this;
        }

        public Criteria andQueryFieldsLikeInsensitive(String value) {
            addCriterion("upper(query_fields) like", value.toUpperCase(), "queryFields");
            return (Criteria) this;
        }

        public Criteria andGroupFieldsLikeInsensitive(String value) {
            addCriterion("upper(group_fields) like", value.toUpperCase(), "groupFields");
            return (Criteria) this;
        }

        public Criteria andSortFieldsLikeInsensitive(String value) {
            addCriterion("upper(sort_fields) like", value.toUpperCase(), "sortFields");
            return (Criteria) this;
        }

        public Criteria andRequestParamsLikeInsensitive(String value) {
            addCriterion("upper(request_params) like", value.toUpperCase(), "requestParams");
            return (Criteria) this;
        }

        public Criteria andResponseParamsLikeInsensitive(String value) {
            addCriterion("upper(response_params) like", value.toUpperCase(), "responseParams");
            return (Criteria) this;
        }

        public Criteria andCreatorLikeInsensitive(String value) {
            addCriterion("upper(creator) like", value.toUpperCase(), "creator");
            return (Criteria) this;
        }

        public Criteria andLastModifierLikeInsensitive(String value) {
            addCriterion("upper(last_modifier) like", value.toUpperCase(), "lastModifier");
            return (Criteria) this;
        }

        public Criteria andRequestMethodLikeInsensitive(String value) {
            addCriterion("upper(request_method) like", value.toUpperCase(), "requestMethod");
            return (Criteria) this;
        }

        public Criteria andContentTypeLikeInsensitive(String value) {
            addCriterion("upper(content_type) like", value.toUpperCase(), "contentType");
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