package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RtAppInstanceDOExample {
    protected String orderByClause;

    protected boolean distinct;

    protected List<Criteria> oredCriteria;

    public RtAppInstanceDOExample() {
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

        public Criteria andVersionIsNull() {
            addCriterion("version is null");
            return (Criteria) this;
        }

        public Criteria andVersionIsNotNull() {
            addCriterion("version is not null");
            return (Criteria) this;
        }

        public Criteria andVersionEqualTo(String value) {
            addCriterion("version =", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotEqualTo(String value) {
            addCriterion("version <>", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionGreaterThan(String value) {
            addCriterion("version >", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionGreaterThanOrEqualTo(String value) {
            addCriterion("version >=", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionLessThan(String value) {
            addCriterion("version <", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionLessThanOrEqualTo(String value) {
            addCriterion("version <=", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionLike(String value) {
            addCriterion("version like", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotLike(String value) {
            addCriterion("version not like", value, "version");
            return (Criteria) this;
        }

        public Criteria andVersionIn(List<String> values) {
            addCriterion("version in", values, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotIn(List<String> values) {
            addCriterion("version not in", values, "version");
            return (Criteria) this;
        }

        public Criteria andVersionBetween(String value1, String value2) {
            addCriterion("version between", value1, value2, "version");
            return (Criteria) this;
        }

        public Criteria andVersionNotBetween(String value1, String value2) {
            addCriterion("version not between", value1, value2, "version");
            return (Criteria) this;
        }

        public Criteria andStatusIsNull() {
            addCriterion("`status` is null");
            return (Criteria) this;
        }

        public Criteria andStatusIsNotNull() {
            addCriterion("`status` is not null");
            return (Criteria) this;
        }

        public Criteria andStatusEqualTo(String value) {
            addCriterion("`status` =", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotEqualTo(String value) {
            addCriterion("`status` <>", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThan(String value) {
            addCriterion("`status` >", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusGreaterThanOrEqualTo(String value) {
            addCriterion("`status` >=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThan(String value) {
            addCriterion("`status` <", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLessThanOrEqualTo(String value) {
            addCriterion("`status` <=", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusLike(String value) {
            addCriterion("`status` like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotLike(String value) {
            addCriterion("`status` not like", value, "status");
            return (Criteria) this;
        }

        public Criteria andStatusIn(List<String> values) {
            addCriterion("`status` in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotIn(List<String> values) {
            addCriterion("`status` not in", values, "status");
            return (Criteria) this;
        }

        public Criteria andStatusBetween(String value1, String value2) {
            addCriterion("`status` between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andStatusNotBetween(String value1, String value2) {
            addCriterion("`status` not between", value1, value2, "status");
            return (Criteria) this;
        }

        public Criteria andLockVersionIsNull() {
            addCriterion("lock_version is null");
            return (Criteria) this;
        }

        public Criteria andLockVersionIsNotNull() {
            addCriterion("lock_version is not null");
            return (Criteria) this;
        }

        public Criteria andLockVersionEqualTo(Integer value) {
            addCriterion("lock_version =", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionNotEqualTo(Integer value) {
            addCriterion("lock_version <>", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionGreaterThan(Integer value) {
            addCriterion("lock_version >", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionGreaterThanOrEqualTo(Integer value) {
            addCriterion("lock_version >=", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionLessThan(Integer value) {
            addCriterion("lock_version <", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionLessThanOrEqualTo(Integer value) {
            addCriterion("lock_version <=", value, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionIn(List<Integer> values) {
            addCriterion("lock_version in", values, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionNotIn(List<Integer> values) {
            addCriterion("lock_version not in", values, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionBetween(Integer value1, Integer value2) {
            addCriterion("lock_version between", value1, value2, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andLockVersionNotBetween(Integer value1, Integer value2) {
            addCriterion("lock_version not between", value1, value2, "lockVersion");
            return (Criteria) this;
        }

        public Criteria andVisitIsNull() {
            addCriterion("visit is null");
            return (Criteria) this;
        }

        public Criteria andVisitIsNotNull() {
            addCriterion("visit is not null");
            return (Criteria) this;
        }

        public Criteria andVisitEqualTo(Boolean value) {
            addCriterion("visit =", value, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitNotEqualTo(Boolean value) {
            addCriterion("visit <>", value, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitGreaterThan(Boolean value) {
            addCriterion("visit >", value, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitGreaterThanOrEqualTo(Boolean value) {
            addCriterion("visit >=", value, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitLessThan(Boolean value) {
            addCriterion("visit <", value, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitLessThanOrEqualTo(Boolean value) {
            addCriterion("visit <=", value, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitIn(List<Boolean> values) {
            addCriterion("visit in", values, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitNotIn(List<Boolean> values) {
            addCriterion("visit not in", values, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitBetween(Boolean value1, Boolean value2) {
            addCriterion("visit between", value1, value2, "visit");
            return (Criteria) this;
        }

        public Criteria andVisitNotBetween(Boolean value1, Boolean value2) {
            addCriterion("visit not between", value1, value2, "visit");
            return (Criteria) this;
        }

        public Criteria andUpgradeIsNull() {
            addCriterion("upgrade is null");
            return (Criteria) this;
        }

        public Criteria andUpgradeIsNotNull() {
            addCriterion("upgrade is not null");
            return (Criteria) this;
        }

        public Criteria andUpgradeEqualTo(Boolean value) {
            addCriterion("upgrade =", value, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeNotEqualTo(Boolean value) {
            addCriterion("upgrade <>", value, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeGreaterThan(Boolean value) {
            addCriterion("upgrade >", value, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeGreaterThanOrEqualTo(Boolean value) {
            addCriterion("upgrade >=", value, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeLessThan(Boolean value) {
            addCriterion("upgrade <", value, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeLessThanOrEqualTo(Boolean value) {
            addCriterion("upgrade <=", value, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeIn(List<Boolean> values) {
            addCriterion("upgrade in", values, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeNotIn(List<Boolean> values) {
            addCriterion("upgrade not in", values, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeBetween(Boolean value1, Boolean value2) {
            addCriterion("upgrade between", value1, value2, "upgrade");
            return (Criteria) this;
        }

        public Criteria andUpgradeNotBetween(Boolean value1, Boolean value2) {
            addCriterion("upgrade not between", value1, value2, "upgrade");
            return (Criteria) this;
        }

        public Criteria andLatestVersionIsNull() {
            addCriterion("latest_version is null");
            return (Criteria) this;
        }

        public Criteria andLatestVersionIsNotNull() {
            addCriterion("latest_version is not null");
            return (Criteria) this;
        }

        public Criteria andLatestVersionEqualTo(String value) {
            addCriterion("latest_version =", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionNotEqualTo(String value) {
            addCriterion("latest_version <>", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionGreaterThan(String value) {
            addCriterion("latest_version >", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionGreaterThanOrEqualTo(String value) {
            addCriterion("latest_version >=", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionLessThan(String value) {
            addCriterion("latest_version <", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionLessThanOrEqualTo(String value) {
            addCriterion("latest_version <=", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionLike(String value) {
            addCriterion("latest_version like", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionNotLike(String value) {
            addCriterion("latest_version not like", value, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionIn(List<String> values) {
            addCriterion("latest_version in", values, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionNotIn(List<String> values) {
            addCriterion("latest_version not in", values, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionBetween(String value1, String value2) {
            addCriterion("latest_version between", value1, value2, "latestVersion");
            return (Criteria) this;
        }

        public Criteria andLatestVersionNotBetween(String value1, String value2) {
            addCriterion("latest_version not between", value1, value2, "latestVersion");
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

        public Criteria andAppInstanceNameIsNull() {
            addCriterion("app_instance_name is null");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameIsNotNull() {
            addCriterion("app_instance_name is not null");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameEqualTo(String value) {
            addCriterion("app_instance_name =", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameNotEqualTo(String value) {
            addCriterion("app_instance_name <>", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameGreaterThan(String value) {
            addCriterion("app_instance_name >", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameGreaterThanOrEqualTo(String value) {
            addCriterion("app_instance_name >=", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameLessThan(String value) {
            addCriterion("app_instance_name <", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameLessThanOrEqualTo(String value) {
            addCriterion("app_instance_name <=", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameLike(String value) {
            addCriterion("app_instance_name like", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameNotLike(String value) {
            addCriterion("app_instance_name not like", value, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameIn(List<String> values) {
            addCriterion("app_instance_name in", values, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameNotIn(List<String> values) {
            addCriterion("app_instance_name not in", values, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameBetween(String value1, String value2) {
            addCriterion("app_instance_name between", value1, value2, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andAppInstanceNameNotBetween(String value1, String value2) {
            addCriterion("app_instance_name not between", value1, value2, "appInstanceName");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceIsNull() {
            addCriterion("owner_reference is null");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceIsNotNull() {
            addCriterion("owner_reference is not null");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceEqualTo(String value) {
            addCriterion("owner_reference =", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceNotEqualTo(String value) {
            addCriterion("owner_reference <>", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceGreaterThan(String value) {
            addCriterion("owner_reference >", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceGreaterThanOrEqualTo(String value) {
            addCriterion("owner_reference >=", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceLessThan(String value) {
            addCriterion("owner_reference <", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceLessThanOrEqualTo(String value) {
            addCriterion("owner_reference <=", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceLike(String value) {
            addCriterion("owner_reference like", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceNotLike(String value) {
            addCriterion("owner_reference not like", value, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceIn(List<String> values) {
            addCriterion("owner_reference in", values, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceNotIn(List<String> values) {
            addCriterion("owner_reference not in", values, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceBetween(String value1, String value2) {
            addCriterion("owner_reference between", value1, value2, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andOwnerReferenceNotBetween(String value1, String value2) {
            addCriterion("owner_reference not between", value1, value2, "ownerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceIsNull() {
            addCriterion("parent_owner_reference is null");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceIsNotNull() {
            addCriterion("parent_owner_reference is not null");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceEqualTo(String value) {
            addCriterion("parent_owner_reference =", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceNotEqualTo(String value) {
            addCriterion("parent_owner_reference <>", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceGreaterThan(String value) {
            addCriterion("parent_owner_reference >", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceGreaterThanOrEqualTo(String value) {
            addCriterion("parent_owner_reference >=", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceLessThan(String value) {
            addCriterion("parent_owner_reference <", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceLessThanOrEqualTo(String value) {
            addCriterion("parent_owner_reference <=", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceLike(String value) {
            addCriterion("parent_owner_reference like", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceNotLike(String value) {
            addCriterion("parent_owner_reference not like", value, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceIn(List<String> values) {
            addCriterion("parent_owner_reference in", values, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceNotIn(List<String> values) {
            addCriterion("parent_owner_reference not in", values, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceBetween(String value1, String value2) {
            addCriterion("parent_owner_reference between", value1, value2, "parentOwnerReference");
            return (Criteria) this;
        }

        public Criteria andParentOwnerReferenceNotBetween(String value1, String value2) {
            addCriterion("parent_owner_reference not between", value1, value2, "parentOwnerReference");
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