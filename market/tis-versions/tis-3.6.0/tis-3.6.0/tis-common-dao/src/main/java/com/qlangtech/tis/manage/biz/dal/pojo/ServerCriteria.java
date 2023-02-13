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
public class ServerCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public ServerCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected ServerCriteria(ServerCriteria example) {
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

    public Criteria createCriteria(boolean delete) {
        Criteria criteria = createCriteriaInternal(delete);
        if (oredCriteria.size() == 0) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    public Criteria createCriteria() {
        return this.createCriteria(false);
    }

    protected Criteria createCriteriaInternal(boolean delete) {
        return new Criteria(delete);
    }

    public void clear() {
        oredCriteria.clear();
    }

    public static class Criteria {

        protected List<String> criteriaWithoutValue;

        protected List<Map<String, Object>> criteriaWithSingleValue;

        protected List<Map<String, Object>> criteriaWithListValue;

        protected List<Map<String, Object>> criteriaWithBetweenValue;

        protected Criteria(boolean delete) {
            super();
            criteriaWithoutValue = new ArrayList<String>();
            criteriaWithSingleValue = new ArrayList<Map<String, Object>>();
            criteriaWithListValue = new ArrayList<Map<String, Object>>();
            criteriaWithBetweenValue = new ArrayList<Map<String, Object>>();
            addCriterion("server.is_deleted =", delete ? "Y" : "N", "server.is_deleted");
        }

        protected Criteria() {
            this(false);
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

        public Criteria andSidIsNull() {
            addCriterion("sid is null");
            return this;
        }

        public Criteria andSidIsNotNull() {
            addCriterion("sid is not null");
            return this;
        }

        public Criteria andSidEqualTo(Integer value) {
            addCriterion("sid =", value, "sid");
            return this;
        }

        public Criteria andSidNotEqualTo(Integer value) {
            addCriterion("sid <>", value, "sid");
            return this;
        }

        public Criteria andSidGreaterThan(Integer value) {
            addCriterion("sid >", value, "sid");
            return this;
        }

        public Criteria andSidGreaterThanOrEqualTo(Integer value) {
            addCriterion("sid >=", value, "sid");
            return this;
        }

        public Criteria andSidLessThan(Integer value) {
            addCriterion("sid <", value, "sid");
            return this;
        }

        public Criteria andSidLessThanOrEqualTo(Integer value) {
            addCriterion("sid <=", value, "sid");
            return this;
        }

        public Criteria andSidIn(List<Integer> values) {
            addCriterion("sid in", values, "sid");
            return this;
        }

        public Criteria andSidNotIn(List<Integer> values) {
            addCriterion("sid not in", values, "sid");
            return this;
        }

        public Criteria andSidBetween(Integer value1, Integer value2) {
            addCriterion("sid between", value1, value2, "sid");
            return this;
        }

        public Criteria andSidNotBetween(Integer value1, Integer value2) {
            addCriterion("sid not between", value1, value2, "sid");
            return this;
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

        public Criteria andServerNameIsNull() {
            addCriterion("server_name is null");
            return this;
        }

        public Criteria andServerNameIsNotNull() {
            addCriterion("server_name is not null");
            return this;
        }

        public Criteria andServerNameEqualTo(String value) {
            addCriterion("server_name =", value, "serverName");
            return this;
        }

        public Criteria andServerNameNotEqualTo(String value) {
            addCriterion("server_name <>", value, "serverName");
            return this;
        }

        public Criteria andServerNameGreaterThan(String value) {
            addCriterion("server_name >", value, "serverName");
            return this;
        }

        public Criteria andServerNameGreaterThanOrEqualTo(String value) {
            addCriterion("server_name >=", value, "serverName");
            return this;
        }

        public Criteria andServerNameLessThan(String value) {
            addCriterion("server_name <", value, "serverName");
            return this;
        }

        public Criteria andServerNameLessThanOrEqualTo(String value) {
            addCriterion("server_name <=", value, "serverName");
            return this;
        }

        public Criteria andServerNameLike(String value) {
            addCriterion("server_name like", value, "serverName");
            return this;
        }

        public Criteria andServerNameNotLike(String value) {
            addCriterion("server_name not like", value, "serverName");
            return this;
        }

        public Criteria andServerNameIn(List<String> values) {
            addCriterion("server_name in", values, "serverName");
            return this;
        }

        public Criteria andServerNameNotIn(List<String> values) {
            addCriterion("server_name not in", values, "serverName");
            return this;
        }

        public Criteria andServerNameBetween(String value1, String value2) {
            addCriterion("server_name between", value1, value2, "serverName");
            return this;
        }

        public Criteria andServerNameNotBetween(String value1, String value2) {
            addCriterion("server_name not between", value1, value2, "serverName");
            return this;
        }

        public Criteria andIpAddressIsNull() {
            addCriterion("ip_address is null");
            return this;
        }

        public Criteria andIpAddressIsNotNull() {
            addCriterion("ip_address is not null");
            return this;
        }

        public Criteria andIpAddressEqualTo(String value) {
            addCriterion("ip_address =", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressNotEqualTo(String value) {
            addCriterion("ip_address <>", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressGreaterThan(String value) {
            addCriterion("ip_address >", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressGreaterThanOrEqualTo(String value) {
            addCriterion("ip_address >=", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressLessThan(String value) {
            addCriterion("ip_address <", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressLessThanOrEqualTo(String value) {
            addCriterion("ip_address <=", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressLike(String value) {
            addCriterion("ip_address like", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressNotLike(String value) {
            addCriterion("ip_address not like", value, "ipAddress");
            return this;
        }

        public Criteria andIpAddressIn(List<String> values) {
            addCriterion("ip_address in", values, "ipAddress");
            return this;
        }

        public Criteria andIpAddressNotIn(List<String> values) {
            addCriterion("ip_address not in", values, "ipAddress");
            return this;
        }

        public Criteria andIpAddressBetween(String value1, String value2) {
            addCriterion("ip_address between", value1, value2, "ipAddress");
            return this;
        }

        public Criteria andIpAddressNotBetween(String value1, String value2) {
            addCriterion("ip_address not between", value1, value2, "ipAddress");
            return this;
        }

        public Criteria andRoleIsNull() {
            addCriterion("role is null");
            return this;
        }

        public Criteria andRoleIsNotNull() {
            addCriterion("role is not null");
            return this;
        }

        public Criteria andRoleEqualTo(String value) {
            addCriterion("role =", value, "role");
            return this;
        }

        public Criteria andRoleNotEqualTo(String value) {
            addCriterion("role <>", value, "role");
            return this;
        }

        public Criteria andRoleGreaterThan(String value) {
            addCriterion("role >", value, "role");
            return this;
        }

        public Criteria andRoleGreaterThanOrEqualTo(String value) {
            addCriterion("role >=", value, "role");
            return this;
        }

        public Criteria andRoleLessThan(String value) {
            addCriterion("role <", value, "role");
            return this;
        }

        public Criteria andRoleLessThanOrEqualTo(String value) {
            addCriterion("role <=", value, "role");
            return this;
        }

        public Criteria andRoleLike(String value) {
            addCriterion("role like", value, "role");
            return this;
        }

        public Criteria andRoleNotLike(String value) {
            addCriterion("role not like", value, "role");
            return this;
        }

        public Criteria andRoleIn(List<String> values) {
            addCriterion("role in", values, "role");
            return this;
        }

        public Criteria andRoleNotIn(List<String> values) {
            addCriterion("role not in", values, "role");
            return this;
        }

        public Criteria andRoleBetween(String value1, String value2) {
            addCriterion("role between", value1, value2, "role");
            return this;
        }

        public Criteria andRoleNotBetween(String value1, String value2) {
            addCriterion("role not between", value1, value2, "role");
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

        public Criteria andSpIdIsNull() {
            addCriterion("sp_id is null");
            return this;
        }

        public Criteria andSpIdIsNotNull() {
            addCriterion("sp_id is not null");
            return this;
        }

        public Criteria andSpIdEqualTo(Integer value) {
            addCriterion("sp_id =", value, "spId");
            return this;
        }

        public Criteria andSpIdNotEqualTo(Integer value) {
            addCriterion("sp_id <>", value, "spId");
            return this;
        }

        public Criteria andSpIdGreaterThan(Integer value) {
            addCriterion("sp_id >", value, "spId");
            return this;
        }

        public Criteria andSpIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("sp_id >=", value, "spId");
            return this;
        }

        public Criteria andSpIdLessThan(Integer value) {
            addCriterion("sp_id <", value, "spId");
            return this;
        }

        public Criteria andSpIdLessThanOrEqualTo(Integer value) {
            addCriterion("sp_id <=", value, "spId");
            return this;
        }

        public Criteria andSpIdIn(List<Integer> values) {
            addCriterion("sp_id in", values, "spId");
            return this;
        }

        public Criteria andSpIdNotIn(List<Integer> values) {
            addCriterion("sp_id not in", values, "spId");
            return this;
        }

        public Criteria andSpIdBetween(Integer value1, Integer value2) {
            addCriterion("sp_id between", value1, value2, "spId");
            return this;
        }

        public Criteria andSpIdNotBetween(Integer value1, Integer value2) {
            addCriterion("sp_id not between", value1, value2, "spId");
            return this;
        }

        // baisui add 20120116
        public Criteria andRunEnvironmnetEqualTo(Integer value) {
            addCriterion("server_group.runt_environment = ", value, "server_group.runt_environment");
            return this;
        }

        public Criteria andApplicationNameEqualTo(String appName) {
            addCriterion("application.project_name = ", appName, "application.project_name");
            return this;
        }

        public Criteria andGroupIndexEqualTo(Integer groupIndex) {
            addCriterion("server_group.group_index = ", groupIndex, "server_group.group_index");
            return this;
        }
        // baisui add 20120116 end
    }
}
