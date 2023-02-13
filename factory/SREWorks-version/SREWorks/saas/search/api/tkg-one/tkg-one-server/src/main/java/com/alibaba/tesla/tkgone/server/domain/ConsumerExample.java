package com.alibaba.tesla.tkgone.server.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsumerExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public ConsumerExample() {
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

        public Criteria andModifierIsNull() {
            addCriterion("modifier is null");
            return (Criteria) this;
        }

        public Criteria andModifierIsNotNull() {
            addCriterion("modifier is not null");
            return (Criteria) this;
        }

        public Criteria andModifierEqualTo(String value) {
            addCriterion("modifier =", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierNotEqualTo(String value) {
            addCriterion("modifier <>", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierGreaterThan(String value) {
            addCriterion("modifier >", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierGreaterThanOrEqualTo(String value) {
            addCriterion("modifier >=", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierLessThan(String value) {
            addCriterion("modifier <", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierLessThanOrEqualTo(String value) {
            addCriterion("modifier <=", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierLike(String value) {
            addCriterion("modifier like", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierNotLike(String value) {
            addCriterion("modifier not like", value, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierIn(List<String> values) {
            addCriterion("modifier in", values, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierNotIn(List<String> values) {
            addCriterion("modifier not in", values, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierBetween(String value1, String value2) {
            addCriterion("modifier between", value1, value2, "modifier");
            return (Criteria) this;
        }

        public Criteria andModifierNotBetween(String value1, String value2) {
            addCriterion("modifier not between", value1, value2, "modifier");
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

        public Criteria andImportConfigIsNull() {
            addCriterion("import_config is null");
            return (Criteria) this;
        }

        public Criteria andImportConfigIsNotNull() {
            addCriterion("import_config is not null");
            return (Criteria) this;
        }

        public Criteria andImportConfigEqualTo(String value) {
            addCriterion("import_config =", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigNotEqualTo(String value) {
            addCriterion("import_config <>", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigGreaterThan(String value) {
            addCriterion("import_config >", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigGreaterThanOrEqualTo(String value) {
            addCriterion("import_config >=", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigLessThan(String value) {
            addCriterion("import_config <", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigLessThanOrEqualTo(String value) {
            addCriterion("import_config <=", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigLike(String value) {
            addCriterion("import_config like", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigNotLike(String value) {
            addCriterion("import_config not like", value, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigIn(List<String> values) {
            addCriterion("import_config in", values, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigNotIn(List<String> values) {
            addCriterion("import_config not in", values, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigBetween(String value1, String value2) {
            addCriterion("import_config between", value1, value2, "importConfig");
            return (Criteria) this;
        }

        public Criteria andImportConfigNotBetween(String value1, String value2) {
            addCriterion("import_config not between", value1, value2, "importConfig");
            return (Criteria) this;
        }

        public Criteria andSourceInfoIsNull() {
            addCriterion("source_info is null");
            return (Criteria) this;
        }

        public Criteria andSourceInfoIsNotNull() {
            addCriterion("source_info is not null");
            return (Criteria) this;
        }

        public Criteria andSourceInfoEqualTo(String value) {
            addCriterion("source_info =", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoNotEqualTo(String value) {
            addCriterion("source_info <>", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoGreaterThan(String value) {
            addCriterion("source_info >", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoGreaterThanOrEqualTo(String value) {
            addCriterion("source_info >=", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoLessThan(String value) {
            addCriterion("source_info <", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoLessThanOrEqualTo(String value) {
            addCriterion("source_info <=", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoLike(String value) {
            addCriterion("source_info like", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoNotLike(String value) {
            addCriterion("source_info not like", value, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoIn(List<String> values) {
            addCriterion("source_info in", values, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoNotIn(List<String> values) {
            addCriterion("source_info not in", values, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoBetween(String value1, String value2) {
            addCriterion("source_info between", value1, value2, "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceInfoNotBetween(String value1, String value2) {
            addCriterion("source_info not between", value1, value2, "sourceInfo");
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

        public Criteria andClientIsNull() {
            addCriterion("client is null");
            return (Criteria) this;
        }

        public Criteria andClientIsNotNull() {
            addCriterion("client is not null");
            return (Criteria) this;
        }

        public Criteria andClientEqualTo(String value) {
            addCriterion("client =", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientNotEqualTo(String value) {
            addCriterion("client <>", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientGreaterThan(String value) {
            addCriterion("client >", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientGreaterThanOrEqualTo(String value) {
            addCriterion("client >=", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientLessThan(String value) {
            addCriterion("client <", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientLessThanOrEqualTo(String value) {
            addCriterion("client <=", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientLike(String value) {
            addCriterion("client like", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientNotLike(String value) {
            addCriterion("client not like", value, "client");
            return (Criteria) this;
        }

        public Criteria andClientIn(List<String> values) {
            addCriterion("client in", values, "client");
            return (Criteria) this;
        }

        public Criteria andClientNotIn(List<String> values) {
            addCriterion("client not in", values, "client");
            return (Criteria) this;
        }

        public Criteria andClientBetween(String value1, String value2) {
            addCriterion("client between", value1, value2, "client");
            return (Criteria) this;
        }

        public Criteria andClientNotBetween(String value1, String value2) {
            addCriterion("client not between", value1, value2, "client");
            return (Criteria) this;
        }

        public Criteria andOffsetIsNull() {
            addCriterion("offset is null");
            return (Criteria) this;
        }

        public Criteria andOffsetIsNotNull() {
            addCriterion("offset is not null");
            return (Criteria) this;
        }

        public Criteria andOffsetEqualTo(String value) {
            addCriterion("offset =", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetNotEqualTo(String value) {
            addCriterion("offset <>", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetGreaterThan(String value) {
            addCriterion("offset >", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetGreaterThanOrEqualTo(String value) {
            addCriterion("offset >=", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetLessThan(String value) {
            addCriterion("offset <", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetLessThanOrEqualTo(String value) {
            addCriterion("offset <=", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetLike(String value) {
            addCriterion("offset like", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetNotLike(String value) {
            addCriterion("offset not like", value, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetIn(List<String> values) {
            addCriterion("offset in", values, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetNotIn(List<String> values) {
            addCriterion("offset not in", values, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetBetween(String value1, String value2) {
            addCriterion("offset between", value1, value2, "offset");
            return (Criteria) this;
        }

        public Criteria andOffsetNotBetween(String value1, String value2) {
            addCriterion("offset not between", value1, value2, "offset");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("status is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("status is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(String value) {
            addCriterion("status =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(String value) {
            addCriterion("status <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(String value) {
            addCriterion("status >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(String value) {
            addCriterion("status >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(String value) {
            addCriterion("status <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(String value) {
            addCriterion("status <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLike(String value) {
            addCriterion("status like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotLike(String value) {
            addCriterion("status not like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<String> values) {
            addCriterion("status in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<String> values) {
            addCriterion("status not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(String value1, String value2) {
            addCriterion("status between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(String value1, String value2) {
            addCriterion("status not between", value1, value2, "status");
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

        public Criteria andEnableIsNull() {
            addCriterion("enable is null");
            return (Criteria) this;
        }

        public Criteria andEnableIsNotNull() {
            addCriterion("enable is not null");
            return (Criteria) this;
        }

        public Criteria andEnableEqualTo(String value) {
            addCriterion("enable =", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableNotEqualTo(String value) {
            addCriterion("enable <>", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableGreaterThan(String value) {
            addCriterion("enable >", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableGreaterThanOrEqualTo(String value) {
            addCriterion("enable >=", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableLessThan(String value) {
            addCriterion("enable <", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableLessThanOrEqualTo(String value) {
            addCriterion("enable <=", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableLike(String value) {
            addCriterion("enable like", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableNotLike(String value) {
            addCriterion("enable not like", value, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableIn(List<String> values) {
            addCriterion("enable in", values, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableNotIn(List<String> values) {
            addCriterion("enable not in", values, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableBetween(String value1, String value2) {
            addCriterion("enable between", value1, value2, "enable");
            return (Criteria) this;
        }

        public Criteria andEnableNotBetween(String value1, String value2) {
            addCriterion("enable not between", value1, value2, "enable");
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

        public Criteria andUserImportConfigIsNull() {
            addCriterion("user_import_config is null");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigIsNotNull() {
            addCriterion("user_import_config is not null");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigEqualTo(String value) {
            addCriterion("user_import_config =", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigNotEqualTo(String value) {
            addCriterion("user_import_config <>", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigGreaterThan(String value) {
            addCriterion("user_import_config >", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigGreaterThanOrEqualTo(String value) {
            addCriterion("user_import_config >=", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigLessThan(String value) {
            addCriterion("user_import_config <", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigLessThanOrEqualTo(String value) {
            addCriterion("user_import_config <=", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigLike(String value) {
            addCriterion("user_import_config like", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigNotLike(String value) {
            addCriterion("user_import_config not like", value, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigIn(List<String> values) {
            addCriterion("user_import_config in", values, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigNotIn(List<String> values) {
            addCriterion("user_import_config not in", values, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigBetween(String value1, String value2) {
            addCriterion("user_import_config between", value1, value2, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigNotBetween(String value1, String value2) {
            addCriterion("user_import_config not between", value1, value2, "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdIsNull() {
            addCriterion("effective_threshold is null");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdIsNotNull() {
            addCriterion("effective_threshold is not null");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdEqualTo(Integer value) {
            addCriterion("effective_threshold =", value, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdNotEqualTo(Integer value) {
            addCriterion("effective_threshold <>", value, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdGreaterThan(Integer value) {
            addCriterion("effective_threshold >", value, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdGreaterThanOrEqualTo(Integer value) {
            addCriterion("effective_threshold >=", value, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdLessThan(Integer value) {
            addCriterion("effective_threshold <", value, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdLessThanOrEqualTo(Integer value) {
            addCriterion("effective_threshold <=", value, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdIn(List<Integer> values) {
            addCriterion("effective_threshold in", values, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdNotIn(List<Integer> values) {
            addCriterion("effective_threshold not in", values, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdBetween(Integer value1, Integer value2) {
            addCriterion("effective_threshold between", value1, value2, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andEffectiveThresholdNotBetween(Integer value1, Integer value2) {
            addCriterion("effective_threshold not between", value1, value2, "effectiveThreshold");
            return (Criteria) this;
        }

        public Criteria andNotifiersIsNull() {
            addCriterion("notifiers is null");
            return (Criteria) this;
        }

        public Criteria andNotifiersIsNotNull() {
            addCriterion("notifiers is not null");
            return (Criteria) this;
        }

        public Criteria andNotifiersEqualTo(String value) {
            addCriterion("notifiers =", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersNotEqualTo(String value) {
            addCriterion("notifiers <>", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersGreaterThan(String value) {
            addCriterion("notifiers >", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersGreaterThanOrEqualTo(String value) {
            addCriterion("notifiers >=", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersLessThan(String value) {
            addCriterion("notifiers <", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersLessThanOrEqualTo(String value) {
            addCriterion("notifiers <=", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersLike(String value) {
            addCriterion("notifiers like", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersNotLike(String value) {
            addCriterion("notifiers not like", value, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersIn(List<String> values) {
            addCriterion("notifiers in", values, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersNotIn(List<String> values) {
            addCriterion("notifiers not in", values, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersBetween(String value1, String value2) {
            addCriterion("notifiers between", value1, value2, "notifiers");
            return (Criteria) this;
        }

        public Criteria andNotifiersNotBetween(String value1, String value2) {
            addCriterion("notifiers not between", value1, value2, "notifiers");
            return (Criteria) this;
        }

        public Criteria andModifierLikeInsensitive(String value) {
            addCriterion("upper(modifier) like", value.toUpperCase(), "modifier");
            return (Criteria) this;
        }

        public Criteria andCreatorLikeInsensitive(String value) {
            addCriterion("upper(creator) like", value.toUpperCase(), "creator");
            return (Criteria) this;
        }

        public Criteria andImportConfigLikeInsensitive(String value) {
            addCriterion("upper(import_config) like", value.toUpperCase(), "importConfig");
            return (Criteria) this;
        }

        public Criteria andSourceInfoLikeInsensitive(String value) {
            addCriterion("upper(source_info) like", value.toUpperCase(), "sourceInfo");
            return (Criteria) this;
        }

        public Criteria andSourceTypeLikeInsensitive(String value) {
            addCriterion("upper(source_type) like", value.toUpperCase(), "sourceType");
            return (Criteria) this;
        }

        public Criteria andClientLikeInsensitive(String value) {
            addCriterion("upper(client) like", value.toUpperCase(), "client");
            return (Criteria) this;
        }

        public Criteria andOffsetLikeInsensitive(String value) {
            addCriterion("upper(offset) like", value.toUpperCase(), "offset");
            return (Criteria) this;
        }

        public Criteria andStatusLikeInsensitive(String value) {
            addCriterion("upper(status) like", value.toUpperCase(), "status");
            return (Criteria) this;
        }

        public Criteria andNameLikeInsensitive(String value) {
            addCriterion("upper(name) like", value.toUpperCase(), "name");
            return (Criteria) this;
        }

        public Criteria andEnableLikeInsensitive(String value) {
            addCriterion("upper(enable) like", value.toUpperCase(), "enable");
            return (Criteria) this;
        }

        public Criteria andAppNameLikeInsensitive(String value) {
            addCriterion("upper(app_name) like", value.toUpperCase(), "appName");
            return (Criteria) this;
        }

        public Criteria andUserImportConfigLikeInsensitive(String value) {
            addCriterion("upper(user_import_config) like", value.toUpperCase(), "userImportConfig");
            return (Criteria) this;
        }

        public Criteria andNotifiersLikeInsensitive(String value) {
            addCriterion("upper(notifiers) like", value.toUpperCase(), "notifiers");
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