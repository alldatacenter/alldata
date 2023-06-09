/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.biz.dal.pojo;

import com.qlangtech.tis.manage.common.TISBaseCriteria;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class SnapshotCriteria extends TISBaseCriteria {

    public static final short TEST_STATE_BUILD_INDEX = 2;

    public static final short TEST_STATE_CONFIRM = 3;

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public SnapshotCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected SnapshotCriteria(SnapshotCriteria example) {
        this.orderByClause = example.orderByClause;
        this.oredCriteria = example.oredCriteria;
    }

    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    public String getOrderByClause() {
        return orderByClause;
    }

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public void or(Criteria criteria) {
        oredCriteria.add(criteria);
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
    }

    public static class Criteria {

        protected List<String> criteriaWithoutValue;

        protected List<Map<String, Object>> criteriaWithSingleValue;

        protected List<Map<String, Object>> criteriaWithListValue;

        protected List<Map<String, Object>> criteriaWithBetweenValue;

        protected Criteria() {
            super();
            criteriaWithoutValue = new ArrayList<String>();
            criteriaWithSingleValue = new ArrayList<Map<String, Object>>();
            criteriaWithListValue = new ArrayList<Map<String, Object>>();
            criteriaWithBetweenValue = new ArrayList<Map<String, Object>>();
        }

        public boolean isValid() {
            return criteriaWithoutValue.size() > 0 || criteriaWithSingleValue.size() > 0 || criteriaWithListValue.size() > 0 || criteriaWithBetweenValue.size() > 0;
        }

        public List<String> getCriteriaWithoutValue() {
            return criteriaWithoutValue;
        }

        public List<Map<String, Object>> getCriteriaWithSingleValue() {
            return criteriaWithSingleValue;
        }

        public List<Map<String, Object>> getCriteriaWithListValue() {
            return criteriaWithListValue;
        }

        public List<Map<String, Object>> getCriteriaWithBetweenValue() {
            return criteriaWithBetweenValue;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteriaWithoutValue.add(condition);
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("condition", condition);
            map.put("value", value);
            criteriaWithSingleValue.add(map);
        }

        protected void addCriterion(String condition, List<? extends Object> values, String property) {
            if (values == null || values.size() == 0) {
                throw new RuntimeException("Value list for " + property + " cannot be null or empty");
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("condition", condition);
            map.put("values", values);
            criteriaWithListValue.add(map);
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            List<Object> list = new ArrayList<Object>();
            list.add(value1);
            list.add(value2);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("condition", condition);
            map.put("values", list);
            criteriaWithBetweenValue.add(map);
        }

        public Criteria andSnIdIsNull() {
            addCriterion("sn_id is null");
            return this;
        }

        public Criteria andSnIdIsNotNull() {
            addCriterion("sn_id is not null");
            return this;
        }

        public Criteria andSnIdEqualTo(Integer value) {
            addCriterion("sn_id =", value, "snId");
            return this;
        }

        public Criteria andSnIdNotEqualTo(Integer value) {
            addCriterion("sn_id <>", value, "snId");
            return this;
        }

        public Criteria andSnIdGreaterThan(Integer value) {
            addCriterion("sn_id >", value, "snId");
            return this;
        }

        // 百岁添加 start
        public Criteria andAppidEqualTo(Integer appid) {
            addCriterion(" app_id =", appid, "app_id");
            return this;
        }

        // public Criteria andIndexHasConfirm() {
        // addCriterion("app_package.test_status =", TEST_STATE_CONFIRM,
        // "server_group.app_id");
        // return this;
        // }
        // public Criteria andRunEnvironmentEqualTo(Integer runid) {
        // addCriterion("server_group.runt_environment =", runid,
        // "server_group.runt_environment");
        // return this;
        // }
        // 百岁添加 end
        public Criteria andSnIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("sn_id >=", value, "snId");
            return this;
        }

        public Criteria andSnIdLessThan(Integer value) {
            addCriterion("sn_id <", value, "snId");
            return this;
        }

        public Criteria andSnIdLessThanOrEqualTo(Integer value) {
            addCriterion("sn_id <=", value, "snId");
            return this;
        }

        public Criteria andSnIdIn(List<Integer> values) {
            addCriterion("sn_id in", values, "snId");
            return this;
        }

        public Criteria andSnIdNotIn(List<Integer> values) {
            addCriterion("sn_id not in", values, "snId");
            return this;
        }

        public Criteria andSnIdBetween(Integer value1, Integer value2) {
            addCriterion("sn_id between", value1, value2, "snId");
            return this;
        }

        public Criteria andSnIdNotBetween(Integer value1, Integer value2) {
            addCriterion("sn_id not between", value1, value2, "snId");
            return this;
        }

        public Criteria andAppIdIsNull() {
            addCriterion("app_id is null");
            return this;
        }

        public Criteria andAppIdIsNotNull() {
            addCriterion("app_id is not null");
            return this;
        }

        // public Criteria andAppIdEqualTo(Integer value) {
        // addCriterion("snapshot.app_id =", value, "appId");
        // return this;
        // }
        public Criteria andAppIdNotEqualTo(Integer value) {
            addCriterion("app_id <>", value, "appId");
            return this;
        }

        public Criteria andAppIdGreaterThan(Integer value) {
            addCriterion("app_id >", value, "appId");
            return this;
        }

        public Criteria andAppIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("app_id >=", value, "appId");
            return this;
        }

        public Criteria andAppIdLessThan(Integer value) {
            addCriterion("app_id <", value, "appId");
            return this;
        }

        public Criteria andAppIdLessThanOrEqualTo(Integer value) {
            addCriterion("app_id <=", value, "appId");
            return this;
        }

        public Criteria andAppIdIn(List<Integer> values) {
            addCriterion("app_id in", values, "appId");
            return this;
        }

        public Criteria andAppIdNotIn(List<Integer> values) {
            addCriterion("app_id not in", values, "appId");
            return this;
        }

        public Criteria andAppIdBetween(Integer value1, Integer value2) {
            addCriterion("app_id between", value1, value2, "appId");
            return this;
        }

        public Criteria andAppIdNotBetween(Integer value1, Integer value2) {
            addCriterion("app_id not between", value1, value2, "appId");
            return this;
        }

        //
        // public Criteria andPidIsNull() {
        // addCriterion("pid is null");
        // return this;
        // }
        //
        // public Criteria andPidIsNotNull() {
        // addCriterion("pid is not null");
        // return this;
        // }
        // public Criteria andPidEqualTo(Integer value) {
        // addCriterion("snapshot.pid =", value, "pid");
        // return this;
        // }
        //
        // public Criteria andPidNotEqualTo(Integer value) {
        // addCriterion("pid <>", value, "pid");
        // return this;
        // }
        //
        // public Criteria andPidGreaterThan(Integer value) {
        // addCriterion("pid >", value, "pid");
        // return this;
        // }
        //
        // public Criteria andPidGreaterThanOrEqualTo(Integer value) {
        // addCriterion("pid >=", value, "pid");
        // return this;
        // }
        //
        // public Criteria andPidLessThan(Integer value) {
        // addCriterion("pid <", value, "pid");
        // return this;
        // }
        //
        // public Criteria andPidLessThanOrEqualTo(Integer value) {
        // addCriterion("pid <=", value, "pid");
        // return this;
        // }
        //
        // public Criteria andPidIn(List<Integer> values) {
        // addCriterion("pid in", values, "pid");
        // return this;
        // }
        //
        // public Criteria andPidNotIn(List<Integer> values) {
        // addCriterion("pid not in", values, "pid");
        // return this;
        // }
        //
        // public Criteria andPidBetween(Integer value1, Integer value2) {
        // addCriterion("pid between", value1, value2, "pid");
        // return this;
        // }
        //
        // public Criteria andPidNotBetween(Integer value1, Integer value2) {
        // addCriterion("pid not between", value1, value2, "pid");
        // return this;
        // }
        public Criteria andResSchemaIdIsNull() {
            addCriterion("res_schema_id is null");
            return this;
        }

        public Criteria andResSchemaIdIsNotNull() {
            addCriterion("res_schema_id is not null");
            return this;
        }

        public Criteria andResSchemaIdEqualTo(Long value) {
            addCriterion("res_schema_id =", value, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdNotEqualTo(Long value) {
            addCriterion("res_schema_id <>", value, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdGreaterThan(Long value) {
            addCriterion("res_schema_id >", value, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdGreaterThanOrEqualTo(Long value) {
            addCriterion("res_schema_id >=", value, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdLessThan(Long value) {
            addCriterion("res_schema_id <", value, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdLessThanOrEqualTo(Long value) {
            addCriterion("res_schema_id <=", value, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdIn(List<Long> values) {
            addCriterion("res_schema_id in", values, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdNotIn(List<Long> values) {
            addCriterion("res_schema_id not in", values, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdBetween(Long value1, Long value2) {
            addCriterion("res_schema_id between", value1, value2, "resSchemaId");
            return this;
        }

        public Criteria andResSchemaIdNotBetween(Long value1, Long value2) {
            addCriterion("res_schema_id not between", value1, value2, "resSchemaId");
            return this;
        }

        public Criteria andResSolrIdIsNull() {
            addCriterion("res_solr_id is null");
            return this;
        }

        public Criteria andResSolrIdIsNotNull() {
            addCriterion("res_solr_id is not null");
            return this;
        }

        public Criteria andResSolrIdEqualTo(Long value) {
            addCriterion("res_solr_id =", value, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdNotEqualTo(Long value) {
            addCriterion("res_solr_id <>", value, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdGreaterThan(Long value) {
            addCriterion("res_solr_id >", value, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdGreaterThanOrEqualTo(Long value) {
            addCriterion("res_solr_id >=", value, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdLessThan(Long value) {
            addCriterion("res_solr_id <", value, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdLessThanOrEqualTo(Long value) {
            addCriterion("res_solr_id <=", value, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdIn(List<Long> values) {
            addCriterion("res_solr_id in", values, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdNotIn(List<Long> values) {
            addCriterion("res_solr_id not in", values, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdBetween(Long value1, Long value2) {
            addCriterion("res_solr_id between", value1, value2, "resSolrId");
            return this;
        }

        public Criteria andResSolrIdNotBetween(Long value1, Long value2) {
            addCriterion("res_solr_id not between", value1, value2, "resSolrId");
            return this;
        }

        public Criteria andResJarIdIsNull() {
            addCriterion("res_jar_id is null");
            return this;
        }

        public Criteria andResJarIdIsNotNull() {
            addCriterion("res_jar_id is not null");
            return this;
        }

        public Criteria andResJarIdEqualTo(Long value) {
            addCriterion("res_jar_id =", value, "resJarId");
            return this;
        }

        public Criteria andResJarIdNotEqualTo(Long value) {
            addCriterion("res_jar_id <>", value, "resJarId");
            return this;
        }

        public Criteria andResJarIdGreaterThan(Long value) {
            addCriterion("res_jar_id >", value, "resJarId");
            return this;
        }

        public Criteria andResJarIdGreaterThanOrEqualTo(Long value) {
            addCriterion("res_jar_id >=", value, "resJarId");
            return this;
        }

        public Criteria andResJarIdLessThan(Long value) {
            addCriterion("res_jar_id <", value, "resJarId");
            return this;
        }

        public Criteria andResJarIdLessThanOrEqualTo(Long value) {
            addCriterion("res_jar_id <=", value, "resJarId");
            return this;
        }

        public Criteria andResJarIdIn(List<Long> values) {
            addCriterion("res_jar_id in", values, "resJarId");
            return this;
        }

        public Criteria andResJarIdNotIn(List<Long> values) {
            addCriterion("res_jar_id not in", values, "resJarId");
            return this;
        }

        public Criteria andResJarIdBetween(Long value1, Long value2) {
            addCriterion("res_jar_id between", value1, value2, "resJarId");
            return this;
        }

        public Criteria andResJarIdNotBetween(Long value1, Long value2) {
            addCriterion("res_jar_id not between", value1, value2, "resJarId");
            return this;
        }

        public Criteria andResCorePropIdIsNull() {
            addCriterion("res_core_prop_id is null");
            return this;
        }

        public Criteria andResCorePropIdIsNotNull() {
            addCriterion("res_core_prop_id is not null");
            return this;
        }

        public Criteria andResCorePropIdEqualTo(Long value) {
            addCriterion("res_core_prop_id =", value, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdNotEqualTo(Long value) {
            addCriterion("res_core_prop_id <>", value, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdGreaterThan(Long value) {
            addCriterion("res_core_prop_id >", value, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdGreaterThanOrEqualTo(Long value) {
            addCriterion("res_core_prop_id >=", value, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdLessThan(Long value) {
            addCriterion("res_core_prop_id <", value, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdLessThanOrEqualTo(Long value) {
            addCriterion("res_core_prop_id <=", value, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdIn(List<Long> values) {
            addCriterion("res_core_prop_id in", values, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdNotIn(List<Long> values) {
            addCriterion("res_core_prop_id not in", values, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdBetween(Long value1, Long value2) {
            addCriterion("res_core_prop_id between", value1, value2, "resCorePropId");
            return this;
        }

        public Criteria andResCorePropIdNotBetween(Long value1, Long value2) {
            addCriterion("res_core_prop_id not between", value1, value2, "resCorePropId");
            return this;
        }

        public Criteria andResDsIdIsNull() {
            addCriterion("res_ds_id is null");
            return this;
        }

        public Criteria andResDsIdIsNotNull() {
            addCriterion("res_ds_id is not null");
            return this;
        }

        public Criteria andResDsIdEqualTo(Long value) {
            addCriterion("res_ds_id =", value, "resDsId");
            return this;
        }

        public Criteria andResDsIdNotEqualTo(Long value) {
            addCriterion("res_ds_id <>", value, "resDsId");
            return this;
        }

        public Criteria andResDsIdGreaterThan(Long value) {
            addCriterion("res_ds_id >", value, "resDsId");
            return this;
        }

        public Criteria andResDsIdGreaterThanOrEqualTo(Long value) {
            addCriterion("res_ds_id >=", value, "resDsId");
            return this;
        }

        public Criteria andResDsIdLessThan(Long value) {
            addCriterion("res_ds_id <", value, "resDsId");
            return this;
        }

        public Criteria andResDsIdLessThanOrEqualTo(Long value) {
            addCriterion("res_ds_id <=", value, "resDsId");
            return this;
        }

        public Criteria andResDsIdIn(List<Long> values) {
            addCriterion("res_ds_id in", values, "resDsId");
            return this;
        }

        public Criteria andResDsIdNotIn(List<Long> values) {
            addCriterion("res_ds_id not in", values, "resDsId");
            return this;
        }

        public Criteria andResDsIdBetween(Long value1, Long value2) {
            addCriterion("res_ds_id between", value1, value2, "resDsId");
            return this;
        }

        public Criteria andResDsIdNotBetween(Long value1, Long value2) {
            addCriterion("res_ds_id not between", value1, value2, "resDsId");
            return this;
        }

        public Criteria andResApplicationIdIsNull() {
            addCriterion("res_application_id is null");
            return this;
        }

        public Criteria andResApplicationIdIsNotNull() {
            addCriterion("res_application_id is not null");
            return this;
        }

        public Criteria andResApplicationIdEqualTo(Long value) {
            addCriterion("res_application_id =", value, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdNotEqualTo(Long value) {
            addCriterion("res_application_id <>", value, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdGreaterThan(Long value) {
            addCriterion("res_application_id >", value, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdGreaterThanOrEqualTo(Long value) {
            addCriterion("res_application_id >=", value, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdLessThan(Long value) {
            addCriterion("res_application_id <", value, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdLessThanOrEqualTo(Long value) {
            addCriterion("res_application_id <=", value, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdIn(List<Long> values) {
            addCriterion("res_application_id in", values, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdNotIn(List<Long> values) {
            addCriterion("res_application_id not in", values, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdBetween(Long value1, Long value2) {
            addCriterion("res_application_id between", value1, value2, "resApplicationId");
            return this;
        }

        public Criteria andResApplicationIdNotBetween(Long value1, Long value2) {
            addCriterion("res_application_id not between", value1, value2, "resApplicationId");
            return this;
        }

        public Criteria andCreateTimeIsNull() {
            addCriterion("create_time is null");
            return this;
        }

        public Criteria andCreateTimeIsNotNull() {
            addCriterion("create_time is not null");
            return this;
        }

        public Criteria andCreateTimeEqualTo(Date value) {
            addCriterion("create_time =", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeNotEqualTo(Date value) {
            addCriterion("create_time <>", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeGreaterThan(Date value) {
            addCriterion("create_time >", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("create_time >=", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeLessThan(Date value) {
            addCriterion("create_time <", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeLessThanOrEqualTo(Date value) {
            addCriterion("create_time <=", value, "createTime");
            return this;
        }

        public Criteria andCreateTimeIn(List<Date> values) {
            addCriterion("create_time in", values, "createTime");
            return this;
        }

        public Criteria andCreateTimeNotIn(List<Date> values) {
            addCriterion("create_time not in", values, "createTime");
            return this;
        }

        public Criteria andCreateTimeBetween(Date value1, Date value2) {
            addCriterion("create_time between", value1, value2, "createTime");
            return this;
        }

        public Criteria andCreateTimeNotBetween(Date value1, Date value2) {
            addCriterion("create_time not between", value1, value2, "createTime");
            return this;
        }

        public Criteria andCreateUserIdIsNull() {
            addCriterion("create_user_id is null");
            return this;
        }

        public Criteria andCreateUserIdIsNotNull() {
            addCriterion("create_user_id is not null");
            return this;
        }

        public Criteria andCreateUserIdEqualTo(Long value) {
            addCriterion("create_user_id =", value, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdNotEqualTo(Long value) {
            addCriterion("create_user_id <>", value, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdGreaterThan(Long value) {
            addCriterion("create_user_id >", value, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdGreaterThanOrEqualTo(Long value) {
            addCriterion("create_user_id >=", value, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdLessThan(Long value) {
            addCriterion("create_user_id <", value, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdLessThanOrEqualTo(Long value) {
            addCriterion("create_user_id <=", value, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdIn(List<Long> values) {
            addCriterion("create_user_id in", values, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdNotIn(List<Long> values) {
            addCriterion("create_user_id not in", values, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdBetween(Long value1, Long value2) {
            addCriterion("create_user_id between", value1, value2, "createUserId");
            return this;
        }

        public Criteria andCreateUserIdNotBetween(Long value1, Long value2) {
            addCriterion("create_user_id not between", value1, value2, "createUserId");
            return this;
        }

        public Criteria andCreateUserNameIsNull() {
            addCriterion("create_user_name is null");
            return this;
        }

        public Criteria andCreateUserNameIsNotNull() {
            addCriterion("create_user_name is not null");
            return this;
        }

        public Criteria andCreateUserNameEqualTo(String value) {
            addCriterion("create_user_name =", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameNotEqualTo(String value) {
            addCriterion("create_user_name <>", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameGreaterThan(String value) {
            addCriterion("create_user_name >", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameGreaterThanOrEqualTo(String value) {
            addCriterion("create_user_name >=", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameLessThan(String value) {
            addCriterion("create_user_name <", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameLessThanOrEqualTo(String value) {
            addCriterion("create_user_name <=", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameLike(String value) {
            addCriterion("create_user_name like", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameNotLike(String value) {
            addCriterion("create_user_name not like", value, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameIn(List<String> values) {
            addCriterion("create_user_name in", values, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameNotIn(List<String> values) {
            addCriterion("create_user_name not in", values, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameBetween(String value1, String value2) {
            addCriterion("create_user_name between", value1, value2, "createUserName");
            return this;
        }

        public Criteria andCreateUserNameNotBetween(String value1, String value2) {
            addCriterion("create_user_name not between", value1, value2, "createUserName");
            return this;
        }

        public Criteria andUpdateTimeIsNull() {
            addCriterion("update_time is null");
            return this;
        }

        public Criteria andUpdateTimeIsNotNull() {
            addCriterion("update_time is not null");
            return this;
        }

        public Criteria andUpdateTimeEqualTo(Date value) {
            addCriterion("update_time =", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeNotEqualTo(Date value) {
            addCriterion("update_time <>", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeGreaterThan(Date value) {
            addCriterion("update_time >", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("update_time >=", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeLessThan(Date value) {
            addCriterion("update_time <", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeLessThanOrEqualTo(Date value) {
            addCriterion("update_time <=", value, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeIn(List<Date> values) {
            addCriterion("update_time in", values, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeNotIn(List<Date> values) {
            addCriterion("update_time not in", values, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeBetween(Date value1, Date value2) {
            addCriterion("update_time between", value1, value2, "updateTime");
            return this;
        }

        public Criteria andUpdateTimeNotBetween(Date value1, Date value2) {
            addCriterion("update_time not between", value1, value2, "updateTime");
            return this;
        }

        public Criteria andPreSnIdIsNull() {
            addCriterion("pre_sn_id is null");
            return this;
        }

        public Criteria andPreSnIdIsNotNull() {
            addCriterion("pre_sn_id is not null");
            return this;
        }

        public Criteria andPreSnIdEqualTo(Integer value) {
            addCriterion("pre_sn_id =", value, "preSnId");
            return this;
        }

        public Criteria andPreSnIdNotEqualTo(Integer value) {
            addCriterion("pre_sn_id <>", value, "preSnId");
            return this;
        }

        public Criteria andPreSnIdGreaterThan(Integer value) {
            addCriterion("pre_sn_id >", value, "preSnId");
            return this;
        }

        public Criteria andPreSnIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("pre_sn_id >=", value, "preSnId");
            return this;
        }

        public Criteria andPreSnIdLessThan(Integer value) {
            addCriterion("pre_sn_id <", value, "preSnId");
            return this;
        }

        public Criteria andPreSnIdLessThanOrEqualTo(Integer value) {
            addCriterion("pre_sn_id <=", value, "preSnId");
            return this;
        }

        public Criteria andPreSnIdIn(List<Integer> values) {
            addCriterion("pre_sn_id in", values, "preSnId");
            return this;
        }

        public Criteria andPreSnIdNotIn(List<Integer> values) {
            addCriterion("pre_sn_id not in", values, "preSnId");
            return this;
        }

        public Criteria andPreSnIdBetween(Integer value1, Integer value2) {
            addCriterion("pre_sn_id between", value1, value2, "preSnId");
            return this;
        }

        public Criteria andPreSnIdNotBetween(Integer value1, Integer value2) {
            addCriterion("pre_sn_id not between", value1, value2, "preSnId");
            return this;
        }
    }
}
