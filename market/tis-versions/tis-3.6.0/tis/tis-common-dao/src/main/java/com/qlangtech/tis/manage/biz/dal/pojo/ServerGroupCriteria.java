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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.qlangtech.tis.manage.common.TISBaseCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.BizDomainCriteria.Criteria;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ServerGroupCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public ServerGroupCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected ServerGroupCriteria(ServerGroupCriteria example) {
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
            criteria.andNotDelete();
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

        public Criteria andGidIsNull() {
            addCriterion("gid is null");
            return this;
        }

        public Criteria andGidIsNotNull() {
            addCriterion("gid is not null");
            return this;
        }

        public Criteria andGidEqualTo(Integer value) {
            addCriterion("gid =", value, "gid");
            return this;
        }

        public Criteria andGidNotEqualTo(Integer value) {
            addCriterion("gid <>", value, "gid");
            return this;
        }

        public Criteria andGidGreaterThan(Integer value) {
            addCriterion("gid >", value, "gid");
            return this;
        }

        public Criteria andGidGreaterThanOrEqualTo(Integer value) {
            addCriterion("gid >=", value, "gid");
            return this;
        }

        public Criteria andGidLessThan(Integer value) {
            addCriterion("gid <", value, "gid");
            return this;
        }

        public Criteria andGidLessThanOrEqualTo(Integer value) {
            addCriterion("gid <=", value, "gid");
            return this;
        }

        public Criteria andGidIn(List<Integer> values) {
            addCriterion("gid in", values, "gid");
            return this;
        }

        public Criteria andGidNotIn(List<Integer> values) {
            addCriterion("gid not in", values, "gid");
            return this;
        }

        public Criteria andGidBetween(Integer value1, Integer value2) {
            addCriterion("gid between", value1, value2, "gid");
            return this;
        }

        public Criteria andGidNotBetween(Integer value1, Integer value2) {
            addCriterion("gid not between", value1, value2, "gid");
            return this;
        }

        // baisui add has not been delete start
        public Criteria andNotDelete() {
            addCriterion("server_group.is_deleted =", "N", "server_group.is_deleted");
            return this;
        }

        // baisui add has not been delete end
        public Criteria andAppIdIsNull() {
            addCriterion("app_id is null");
            return this;
        }

        public Criteria andAppIdIsNotNull() {
            addCriterion("app_id is not null");
            return this;
        }

        public Criteria andAppIdEqualTo(Integer value) {
            addCriterion("app_id =", value, "appId");
            return this;
        }

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

        public Criteria andRuntEnvironmentIsNull() {
            addCriterion("runt_environment is null");
            return this;
        }

        public Criteria andRuntEnvironmentIsNotNull() {
            addCriterion("runt_environment is not null");
            return this;
        }

        public Criteria andRuntEnvironmentEqualTo(Short value) {
            addCriterion("runt_environment =", value, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentNotEqualTo(Short value) {
            addCriterion("runt_environment <>", value, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentGreaterThan(Short value) {
            addCriterion("runt_environment >", value, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentGreaterThanOrEqualTo(Short value) {
            addCriterion("runt_environment >=", value, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentLessThan(Short value) {
            addCriterion("runt_environment <", value, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentLessThanOrEqualTo(Short value) {
            addCriterion("runt_environment <=", value, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentIn(List<Short> values) {
            addCriterion("runt_environment in", values, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentNotIn(List<Short> values) {
            addCriterion("runt_environment not in", values, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentBetween(Short value1, Short value2) {
            addCriterion("runt_environment between", value1, value2, "runtEnvironment");
            return this;
        }

        public Criteria andRuntEnvironmentNotBetween(Short value1, Short value2) {
            addCriterion("runt_environment not between", value1, value2, "runtEnvironment");
            return this;
        }

        public Criteria andGroupIndexIsNull() {
            addCriterion("group_index is null");
            return this;
        }

        public Criteria andGroupIndexIsNotNull() {
            addCriterion("group_index is not null");
            return this;
        }

        public Criteria andGroupIndexEqualTo(Short value) {
            addCriterion("group_index =", value, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexNotEqualTo(Short value) {
            addCriterion("group_index <>", value, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexGreaterThan(Short value) {
            addCriterion("group_index >", value, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexGreaterThanOrEqualTo(Short value) {
            addCriterion("group_index >=", value, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexLessThan(Short value) {
            addCriterion("group_index <", value, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexLessThanOrEqualTo(Short value) {
            addCriterion("group_index <=", value, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexIn(List<Short> values) {
            addCriterion("group_index in", values, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexNotIn(List<Short> values) {
            addCriterion("group_index not in", values, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexBetween(Short value1, Short value2) {
            addCriterion("group_index between", value1, value2, "groupIndex");
            return this;
        }

        public Criteria andGroupIndexNotBetween(Short value1, Short value2) {
            addCriterion("group_index not between", value1, value2, "groupIndex");
            return this;
        }

        public Criteria andPublishSnapshotIdIsNull() {
            addCriterion("publish_snapshot_id is null");
            return this;
        }

        public Criteria andPublishSnapshotIdIsNotNull() {
            addCriterion("publish_snapshot_id is not null");
            return this;
        }

        public Criteria andPublishSnapshotIdEqualTo(Integer value) {
            addCriterion("publish_snapshot_id =", value, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdNotEqualTo(Integer value) {
            addCriterion("publish_snapshot_id <>", value, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdGreaterThan(Integer value) {
            addCriterion("publish_snapshot_id >", value, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("publish_snapshot_id >=", value, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdLessThan(Integer value) {
            addCriterion("publish_snapshot_id <", value, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdLessThanOrEqualTo(Integer value) {
            addCriterion("publish_snapshot_id <=", value, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdIn(List<Integer> values) {
            addCriterion("publish_snapshot_id in", values, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdNotIn(List<Integer> values) {
            addCriterion("publish_snapshot_id not in", values, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdBetween(Integer value1, Integer value2) {
            addCriterion("publish_snapshot_id between", value1, value2, "publishSnapshotId");
            return this;
        }

        public Criteria andPublishSnapshotIdNotBetween(Integer value1, Integer value2) {
            addCriterion("publish_snapshot_id not between", value1, value2, "publishSnapshotId");
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
        // public Criteria andDeleteFlagEqualTo(Integer value) {
        // addCriterion("is_deleted =", value, "deleteFlag");
        // return this;
        // }
        //
        // public Criteria andNotDelete() {
        // addCriterion("application.is_deleted =", "N",
        // "application.is_deleted");
        // return this;
        // }
    }
}
