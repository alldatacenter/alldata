package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeployAppDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public DeployAppDOExample() {
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

        public Criteria andAppPackageIdIsNull() {
            addCriterion("app_package_id is null");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdIsNotNull() {
            addCriterion("app_package_id is not null");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdEqualTo(Long value) {
            addCriterion("app_package_id =", value, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdNotEqualTo(Long value) {
            addCriterion("app_package_id <>", value, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdGreaterThan(Long value) {
            addCriterion("app_package_id >", value, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdGreaterThanOrEqualTo(Long value) {
            addCriterion("app_package_id >=", value, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdLessThan(Long value) {
            addCriterion("app_package_id <", value, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdLessThanOrEqualTo(Long value) {
            addCriterion("app_package_id <=", value, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdIn(List<Long> values) {
            addCriterion("app_package_id in", values, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdNotIn(List<Long> values) {
            addCriterion("app_package_id not in", values, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdBetween(Long value1, Long value2) {
            addCriterion("app_package_id between", value1, value2, "appPackageId");
            return (Criteria) this;
        }

        public Criteria andAppPackageIdNotBetween(Long value1, Long value2) {
            addCriterion("app_package_id not between", value1, value2, "appPackageId");
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

        public Criteria andGmtStartIsNull() {
            addCriterion("gmt_start is null");
            return (Criteria) this;
        }

        public Criteria andGmtStartIsNotNull() {
            addCriterion("gmt_start is not null");
            return (Criteria) this;
        }

        public Criteria andGmtStartEqualTo(Date value) {
            addCriterion("gmt_start =", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartNotEqualTo(Date value) {
            addCriterion("gmt_start <>", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartGreaterThan(Date value) {
            addCriterion("gmt_start >", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_start >=", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartLessThan(Date value) {
            addCriterion("gmt_start <", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartLessThanOrEqualTo(Date value) {
            addCriterion("gmt_start <=", value, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartIn(List<Date> values) {
            addCriterion("gmt_start in", values, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartNotIn(List<Date> values) {
            addCriterion("gmt_start not in", values, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartBetween(Date value1, Date value2) {
            addCriterion("gmt_start between", value1, value2, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtStartNotBetween(Date value1, Date value2) {
            addCriterion("gmt_start not between", value1, value2, "gmtStart");
            return (Criteria) this;
        }

        public Criteria andGmtEndIsNull() {
            addCriterion("gmt_end is null");
            return (Criteria) this;
        }

        public Criteria andGmtEndIsNotNull() {
            addCriterion("gmt_end is not null");
            return (Criteria) this;
        }

        public Criteria andGmtEndEqualTo(Date value) {
            addCriterion("gmt_end =", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndNotEqualTo(Date value) {
            addCriterion("gmt_end <>", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndGreaterThan(Date value) {
            addCriterion("gmt_end >", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndGreaterThanOrEqualTo(Date value) {
            addCriterion("gmt_end >=", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndLessThan(Date value) {
            addCriterion("gmt_end <", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndLessThanOrEqualTo(Date value) {
            addCriterion("gmt_end <=", value, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndIn(List<Date> values) {
            addCriterion("gmt_end in", values, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndNotIn(List<Date> values) {
            addCriterion("gmt_end not in", values, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndBetween(Date value1, Date value2) {
            addCriterion("gmt_end between", value1, value2, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andGmtEndNotBetween(Date value1, Date value2) {
            addCriterion("gmt_end not between", value1, value2, "gmtEnd");
            return (Criteria) this;
        }

        public Criteria andDeployStatusIsNull() {
            addCriterion("deploy_status is null");
            return (Criteria) this;
        }

        public Criteria andDeployStatusIsNotNull() {
            addCriterion("deploy_status is not null");
            return (Criteria) this;
        }

        public Criteria andDeployStatusEqualTo(String value) {
            addCriterion("deploy_status =", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusNotEqualTo(String value) {
            addCriterion("deploy_status <>", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusGreaterThan(String value) {
            addCriterion("deploy_status >", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusGreaterThanOrEqualTo(String value) {
            addCriterion("deploy_status >=", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusLessThan(String value) {
            addCriterion("deploy_status <", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusLessThanOrEqualTo(String value) {
            addCriterion("deploy_status <=", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusLike(String value) {
            addCriterion("deploy_status like", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusNotLike(String value) {
            addCriterion("deploy_status not like", value, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusIn(List<String> values) {
            addCriterion("deploy_status in", values, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusNotIn(List<String> values) {
            addCriterion("deploy_status not in", values, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusBetween(String value1, String value2) {
            addCriterion("deploy_status between", value1, value2, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployStatusNotBetween(String value1, String value2) {
            addCriterion("deploy_status not between", value1, value2, "deployStatus");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageIsNull() {
            addCriterion("deploy_error_message is null");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageIsNotNull() {
            addCriterion("deploy_error_message is not null");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageEqualTo(String value) {
            addCriterion("deploy_error_message =", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageNotEqualTo(String value) {
            addCriterion("deploy_error_message <>", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageGreaterThan(String value) {
            addCriterion("deploy_error_message >", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageGreaterThanOrEqualTo(String value) {
            addCriterion("deploy_error_message >=", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageLessThan(String value) {
            addCriterion("deploy_error_message <", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageLessThanOrEqualTo(String value) {
            addCriterion("deploy_error_message <=", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageLike(String value) {
            addCriterion("deploy_error_message like", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageNotLike(String value) {
            addCriterion("deploy_error_message not like", value, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageIn(List<String> values) {
            addCriterion("deploy_error_message in", values, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageNotIn(List<String> values) {
            addCriterion("deploy_error_message not in", values, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageBetween(String value1, String value2) {
            addCriterion("deploy_error_message between", value1, value2, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployErrorMessageNotBetween(String value1, String value2) {
            addCriterion("deploy_error_message not between", value1, value2, "deployErrorMessage");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorIsNull() {
            addCriterion("deploy_creator is null");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorIsNotNull() {
            addCriterion("deploy_creator is not null");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorEqualTo(String value) {
            addCriterion("deploy_creator =", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorNotEqualTo(String value) {
            addCriterion("deploy_creator <>", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorGreaterThan(String value) {
            addCriterion("deploy_creator >", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorGreaterThanOrEqualTo(String value) {
            addCriterion("deploy_creator >=", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorLessThan(String value) {
            addCriterion("deploy_creator <", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorLessThanOrEqualTo(String value) {
            addCriterion("deploy_creator <=", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorLike(String value) {
            addCriterion("deploy_creator like", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorNotLike(String value) {
            addCriterion("deploy_creator not like", value, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorIn(List<String> values) {
            addCriterion("deploy_creator in", values, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorNotIn(List<String> values) {
            addCriterion("deploy_creator not in", values, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorBetween(String value1, String value2) {
            addCriterion("deploy_creator between", value1, value2, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployCreatorNotBetween(String value1, String value2) {
            addCriterion("deploy_creator not between", value1, value2, "deployCreator");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdIsNull() {
            addCriterion("deploy_process_id is null");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdIsNotNull() {
            addCriterion("deploy_process_id is not null");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdEqualTo(Long value) {
            addCriterion("deploy_process_id =", value, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdNotEqualTo(Long value) {
            addCriterion("deploy_process_id <>", value, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdGreaterThan(Long value) {
            addCriterion("deploy_process_id >", value, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdGreaterThanOrEqualTo(Long value) {
            addCriterion("deploy_process_id >=", value, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdLessThan(Long value) {
            addCriterion("deploy_process_id <", value, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdLessThanOrEqualTo(Long value) {
            addCriterion("deploy_process_id <=", value, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdIn(List<Long> values) {
            addCriterion("deploy_process_id in", values, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdNotIn(List<Long> values) {
            addCriterion("deploy_process_id not in", values, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdBetween(Long value1, Long value2) {
            addCriterion("deploy_process_id between", value1, value2, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andDeployProcessIdNotBetween(Long value1, Long value2) {
            addCriterion("deploy_process_id not between", value1, value2, "deployProcessId");
            return (Criteria) this;
        }

        public Criteria andPackageVersionIsNull() {
            addCriterion("package_version is null");
            return (Criteria) this;
        }

        public Criteria andPackageVersionIsNotNull() {
            addCriterion("package_version is not null");
            return (Criteria) this;
        }

        public Criteria andPackageVersionEqualTo(String value) {
            addCriterion("package_version =", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionNotEqualTo(String value) {
            addCriterion("package_version <>", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionGreaterThan(String value) {
            addCriterion("package_version >", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionGreaterThanOrEqualTo(String value) {
            addCriterion("package_version >=", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionLessThan(String value) {
            addCriterion("package_version <", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionLessThanOrEqualTo(String value) {
            addCriterion("package_version <=", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionLike(String value) {
            addCriterion("package_version like", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionNotLike(String value) {
            addCriterion("package_version not like", value, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionIn(List<String> values) {
            addCriterion("package_version in", values, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionNotIn(List<String> values) {
            addCriterion("package_version not in", values, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionBetween(String value1, String value2) {
            addCriterion("package_version between", value1, value2, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andPackageVersionNotBetween(String value1, String value2) {
            addCriterion("package_version not between", value1, value2, "packageVersion");
            return (Criteria) this;
        }

        public Criteria andVersionIsNull() {
            addCriterion("version is null");
            return (Criteria) this;
        }

        public Criteria andVersionIsNotNull() {
            addCriterion("version is not null");
            return (Criteria) this;
        }

        public Criteria andVersionEqualTo(Integer value) {
            addCriterion("version =", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotEqualTo(Integer value) {
            addCriterion("version <>", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionGreaterThan(Integer value) {
            addCriterion("version >", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionGreaterThanOrEqualTo(Integer value) {
            addCriterion("version >=", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionLessThan(Integer value) {
            addCriterion("version <", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionLessThanOrEqualTo(Integer value) {
            addCriterion("version <=", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionIn(List<Integer> values) {
            addCriterion("version in", values, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotIn(List<Integer> values) {
            addCriterion("version not in", values, "version");
            return (Criteria) this;
        }

        public Criteria andVersionBetween(Integer value1, Integer value2) {
            addCriterion("version between", value1, value2, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotBetween(Integer value1, Integer value2) {
            addCriterion("version not between", value1, value2, "version");
            return (Criteria) this;
        }

        public Criteria andClusterIdIsNull() {
            addCriterion("cluster_id is null");
            return (Criteria) this;
        }

        public Criteria andClusterIdIsNotNull() {
            addCriterion("cluster_id is not null");
            return (Criteria) this;
        }

        public Criteria andClusterIdEqualTo(String value) {
            addCriterion("cluster_id =", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotEqualTo(String value) {
            addCriterion("cluster_id <>", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdGreaterThan(String value) {
            addCriterion("cluster_id >", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdGreaterThanOrEqualTo(String value) {
            addCriterion("cluster_id >=", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdLessThan(String value) {
            addCriterion("cluster_id <", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdLessThanOrEqualTo(String value) {
            addCriterion("cluster_id <=", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdLike(String value) {
            addCriterion("cluster_id like", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotLike(String value) {
            addCriterion("cluster_id not like", value, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdIn(List<String> values) {
            addCriterion("cluster_id in", values, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotIn(List<String> values) {
            addCriterion("cluster_id not in", values, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdBetween(String value1, String value2) {
            addCriterion("cluster_id between", value1, value2, "clusterId");
            return (Criteria) this;
        }

        public Criteria andClusterIdNotBetween(String value1, String value2) {
            addCriterion("cluster_id not between", value1, value2, "clusterId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdIsNull() {
            addCriterion("namespace_id is null");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdIsNotNull() {
            addCriterion("namespace_id is not null");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdEqualTo(String value) {
            addCriterion("namespace_id =", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotEqualTo(String value) {
            addCriterion("namespace_id <>", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdGreaterThan(String value) {
            addCriterion("namespace_id >", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdGreaterThanOrEqualTo(String value) {
            addCriterion("namespace_id >=", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLessThan(String value) {
            addCriterion("namespace_id <", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLessThanOrEqualTo(String value) {
            addCriterion("namespace_id <=", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdLike(String value) {
            addCriterion("namespace_id like", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotLike(String value) {
            addCriterion("namespace_id not like", value, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdIn(List<String> values) {
            addCriterion("namespace_id in", values, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotIn(List<String> values) {
            addCriterion("namespace_id not in", values, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdBetween(String value1, String value2) {
            addCriterion("namespace_id between", value1, value2, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andNamespaceIdNotBetween(String value1, String value2) {
            addCriterion("namespace_id not between", value1, value2, "namespaceId");
            return (Criteria) this;
        }

        public Criteria andStageIdIsNull() {
            addCriterion("stage_id is null");
            return (Criteria) this;
        }

        public Criteria andStageIdIsNotNull() {
            addCriterion("stage_id is not null");
            return (Criteria) this;
        }

        public Criteria andStageIdEqualTo(String value) {
            addCriterion("stage_id =", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotEqualTo(String value) {
            addCriterion("stage_id <>", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdGreaterThan(String value) {
            addCriterion("stage_id >", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdGreaterThanOrEqualTo(String value) {
            addCriterion("stage_id >=", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdLessThan(String value) {
            addCriterion("stage_id <", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdLessThanOrEqualTo(String value) {
            addCriterion("stage_id <=", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdLike(String value) {
            addCriterion("stage_id like", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotLike(String value) {
            addCriterion("stage_id not like", value, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdIn(List<String> values) {
            addCriterion("stage_id in", values, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotIn(List<String> values) {
            addCriterion("stage_id not in", values, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdBetween(String value1, String value2) {
            addCriterion("stage_id between", value1, value2, "stageId");
            return (Criteria) this;
        }

        public Criteria andStageIdNotBetween(String value1, String value2) {
            addCriterion("stage_id not between", value1, value2, "stageId");
            return (Criteria) this;
        }

        public Criteria andConfigSha256IsNull() {
            addCriterion("config_sha256 is null");
            return (Criteria) this;
        }

        public Criteria andConfigSha256IsNotNull() {
            addCriterion("config_sha256 is not null");
            return (Criteria) this;
        }

        public Criteria andConfigSha256EqualTo(String value) {
            addCriterion("config_sha256 =", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256NotEqualTo(String value) {
            addCriterion("config_sha256 <>", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256GreaterThan(String value) {
            addCriterion("config_sha256 >", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256GreaterThanOrEqualTo(String value) {
            addCriterion("config_sha256 >=", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256LessThan(String value) {
            addCriterion("config_sha256 <", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256LessThanOrEqualTo(String value) {
            addCriterion("config_sha256 <=", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256Like(String value) {
            addCriterion("config_sha256 like", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256NotLike(String value) {
            addCriterion("config_sha256 not like", value, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256In(List<String> values) {
            addCriterion("config_sha256 in", values, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256NotIn(List<String> values) {
            addCriterion("config_sha256 not in", values, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256Between(String value1, String value2) {
            addCriterion("config_sha256 between", value1, value2, "configSha256");
            return (Criteria) this;
        }

        public Criteria andConfigSha256NotBetween(String value1, String value2) {
            addCriterion("config_sha256 not between", value1, value2, "configSha256");
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