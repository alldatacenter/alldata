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
package com.qlangtech.tis.dataplatform.pojo;

import com.qlangtech.tis.manage.common.TISBaseCriteria;
import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DsDatasourceCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public DsDatasourceCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected DsDatasourceCriteria(DsDatasourceCriteria example) {
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

        public Criteria andDsIdIsNull() {
            addCriterion("ds_id is null");
            return this;
        }

        public Criteria andDsIdIsNotNull() {
            addCriterion("ds_id is not null");
            return this;
        }

        public Criteria andDsIdEqualTo(Integer value) {
            addCriterion("ds_id =", value, "dsId");
            return this;
        }

        public Criteria andDsIdNotEqualTo(Integer value) {
            addCriterion("ds_id <>", value, "dsId");
            return this;
        }

        public Criteria andDsIdGreaterThan(Integer value) {
            addCriterion("ds_id >", value, "dsId");
            return this;
        }

        public Criteria andDsIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("ds_id >=", value, "dsId");
            return this;
        }

        public Criteria andDsIdLessThan(Integer value) {
            addCriterion("ds_id <", value, "dsId");
            return this;
        }

        public Criteria andDsIdLessThanOrEqualTo(Integer value) {
            addCriterion("ds_id <=", value, "dsId");
            return this;
        }

        public Criteria andDsIdIn(List<Integer> values) {
            addCriterion("ds_id in", values, "dsId");
            return this;
        }

        public Criteria andDsIdNotIn(List<Integer> values) {
            addCriterion("ds_id not in", values, "dsId");
            return this;
        }

        public Criteria andDsIdBetween(Integer value1, Integer value2) {
            addCriterion("ds_id between", value1, value2, "dsId");
            return this;
        }

        public Criteria andDsIdNotBetween(Integer value1, Integer value2) {
            addCriterion("ds_id not between", value1, value2, "dsId");
            return this;
        }

        public Criteria andDbEnumIsNull() {
            addCriterion("db_enum is null");
            return this;
        }

        public Criteria andDbEnumIsNotNull() {
            addCriterion("db_enum is not null");
            return this;
        }

        public Criteria andDbEnumEqualTo(String value) {
            addCriterion("db_enum =", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumNotEqualTo(String value) {
            addCriterion("db_enum <>", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumGreaterThan(String value) {
            addCriterion("db_enum >", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumGreaterThanOrEqualTo(String value) {
            addCriterion("db_enum >=", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumLessThan(String value) {
            addCriterion("db_enum <", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumLessThanOrEqualTo(String value) {
            addCriterion("db_enum <=", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumLike(String value) {
            addCriterion("db_enum like", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumNotLike(String value) {
            addCriterion("db_enum not like", value, "dbEnum");
            return this;
        }

        public Criteria andDbEnumIn(List<String> values) {
            addCriterion("db_enum in", values, "dbEnum");
            return this;
        }

        public Criteria andDbEnumNotIn(List<String> values) {
            addCriterion("db_enum not in", values, "dbEnum");
            return this;
        }

        public Criteria andDbEnumBetween(String value1, String value2) {
            addCriterion("db_enum between", value1, value2, "dbEnum");
            return this;
        }

        public Criteria andDbEnumNotBetween(String value1, String value2) {
            addCriterion("db_enum not between", value1, value2, "dbEnum");
            return this;
        }

        public Criteria andUsernameIsNull() {
            addCriterion("username is null");
            return this;
        }

        public Criteria andUsernameIsNotNull() {
            addCriterion("username is not null");
            return this;
        }

        public Criteria andUsernameEqualTo(String value) {
            addCriterion("username =", value, "username");
            return this;
        }

        public Criteria andUsernameNotEqualTo(String value) {
            addCriterion("username <>", value, "username");
            return this;
        }

        public Criteria andUsernameGreaterThan(String value) {
            addCriterion("username >", value, "username");
            return this;
        }

        public Criteria andUsernameGreaterThanOrEqualTo(String value) {
            addCriterion("username >=", value, "username");
            return this;
        }

        public Criteria andUsernameLessThan(String value) {
            addCriterion("username <", value, "username");
            return this;
        }

        public Criteria andUsernameLessThanOrEqualTo(String value) {
            addCriterion("username <=", value, "username");
            return this;
        }

        public Criteria andUsernameLike(String value) {
            addCriterion("username like", value, "username");
            return this;
        }

        public Criteria andUsernameNotLike(String value) {
            addCriterion("username not like", value, "username");
            return this;
        }

        public Criteria andUsernameIn(List<String> values) {
            addCriterion("username in", values, "username");
            return this;
        }

        public Criteria andUsernameNotIn(List<String> values) {
            addCriterion("username not in", values, "username");
            return this;
        }

        public Criteria andUsernameBetween(String value1, String value2) {
            addCriterion("username between", value1, value2, "username");
            return this;
        }

        public Criteria andUsernameNotBetween(String value1, String value2) {
            addCriterion("username not between", value1, value2, "username");
            return this;
        }

        public Criteria andPasswordIsNull() {
            addCriterion("password is null");
            return this;
        }

        public Criteria andPasswordIsNotNull() {
            addCriterion("password is not null");
            return this;
        }

        public Criteria andPasswordEqualTo(String value) {
            addCriterion("password =", value, "password");
            return this;
        }

        public Criteria andPasswordNotEqualTo(String value) {
            addCriterion("password <>", value, "password");
            return this;
        }

        public Criteria andPasswordGreaterThan(String value) {
            addCriterion("password >", value, "password");
            return this;
        }

        public Criteria andPasswordGreaterThanOrEqualTo(String value) {
            addCriterion("password >=", value, "password");
            return this;
        }

        public Criteria andPasswordLessThan(String value) {
            addCriterion("password <", value, "password");
            return this;
        }

        public Criteria andPasswordLessThanOrEqualTo(String value) {
            addCriterion("password <=", value, "password");
            return this;
        }

        public Criteria andPasswordLike(String value) {
            addCriterion("password like", value, "password");
            return this;
        }

        public Criteria andPasswordNotLike(String value) {
            addCriterion("password not like", value, "password");
            return this;
        }

        public Criteria andPasswordIn(List<String> values) {
            addCriterion("password in", values, "password");
            return this;
        }

        public Criteria andPasswordNotIn(List<String> values) {
            addCriterion("password not in", values, "password");
            return this;
        }

        public Criteria andPasswordBetween(String value1, String value2) {
            addCriterion("password between", value1, value2, "password");
            return this;
        }

        public Criteria andPasswordNotBetween(String value1, String value2) {
            addCriterion("password not between", value1, value2, "password");
            return this;
        }

        public Criteria andDbTypeIsNull() {
            addCriterion("db_type is null");
            return this;
        }

        public Criteria andDbTypeIsNotNull() {
            addCriterion("db_type is not null");
            return this;
        }

        public Criteria andDbTypeEqualTo(String value) {
            addCriterion("db_type =", value, "dbType");
            return this;
        }

        public Criteria andDbTypeNotEqualTo(String value) {
            addCriterion("db_type <>", value, "dbType");
            return this;
        }

        public Criteria andDbTypeGreaterThan(String value) {
            addCriterion("db_type >", value, "dbType");
            return this;
        }

        public Criteria andDbTypeGreaterThanOrEqualTo(String value) {
            addCriterion("db_type >=", value, "dbType");
            return this;
        }

        public Criteria andDbTypeLessThan(String value) {
            addCriterion("db_type <", value, "dbType");
            return this;
        }

        public Criteria andDbTypeLessThanOrEqualTo(String value) {
            addCriterion("db_type <=", value, "dbType");
            return this;
        }

        public Criteria andDbTypeLike(String value) {
            addCriterion("db_type like", value, "dbType");
            return this;
        }

        public Criteria andDbTypeNotLike(String value) {
            addCriterion("db_type not like", value, "dbType");
            return this;
        }

        public Criteria andDbTypeIn(List<String> values) {
            addCriterion("db_type in", values, "dbType");
            return this;
        }

        public Criteria andDbTypeNotIn(List<String> values) {
            addCriterion("db_type not in", values, "dbType");
            return this;
        }

        public Criteria andDbTypeBetween(String value1, String value2) {
            addCriterion("db_type between", value1, value2, "dbType");
            return this;
        }

        public Criteria andDbTypeNotBetween(String value1, String value2) {
            addCriterion("db_type not between", value1, value2, "dbType");
            return this;
        }

        public Criteria andDsIdentityNameIsNull() {
            addCriterion("ds_identity_name is null");
            return this;
        }

        public Criteria andDsIdentityNameIsNotNull() {
            addCriterion("ds_identity_name is not null");
            return this;
        }

        public Criteria andDsIdentityNameEqualTo(String value) {
            addCriterion("ds_identity_name =", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameNotEqualTo(String value) {
            addCriterion("ds_identity_name <>", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameGreaterThan(String value) {
            addCriterion("ds_identity_name >", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameGreaterThanOrEqualTo(String value) {
            addCriterion("ds_identity_name >=", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameLessThan(String value) {
            addCriterion("ds_identity_name <", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameLessThanOrEqualTo(String value) {
            addCriterion("ds_identity_name <=", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameLike(String value) {
            addCriterion("ds_identity_name like", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameNotLike(String value) {
            addCriterion("ds_identity_name not like", value, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameIn(List<String> values) {
            addCriterion("ds_identity_name in", values, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameNotIn(List<String> values) {
            addCriterion("ds_identity_name not in", values, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameBetween(String value1, String value2) {
            addCriterion("ds_identity_name between", value1, value2, "dsIdentityName");
            return this;
        }

        public Criteria andDsIdentityNameNotBetween(String value1, String value2) {
            addCriterion("ds_identity_name not between", value1, value2, "dsIdentityName");
            return this;
        }

        public Criteria andDsNameIsNull() {
            addCriterion("ds_name is null");
            return this;
        }

        public Criteria andDsNameIsNotNull() {
            addCriterion("ds_name is not null");
            return this;
        }

        public Criteria andDsNameEqualTo(String value) {
            addCriterion("ds_name =", value, "dsName");
            return this;
        }

        public Criteria andDsNameNotEqualTo(String value) {
            addCriterion("ds_name <>", value, "dsName");
            return this;
        }

        public Criteria andDsNameGreaterThan(String value) {
            addCriterion("ds_name >", value, "dsName");
            return this;
        }

        public Criteria andDsNameGreaterThanOrEqualTo(String value) {
            addCriterion("ds_name >=", value, "dsName");
            return this;
        }

        public Criteria andDsNameLessThan(String value) {
            addCriterion("ds_name <", value, "dsName");
            return this;
        }

        public Criteria andDsNameLessThanOrEqualTo(String value) {
            addCriterion("ds_name <=", value, "dsName");
            return this;
        }

        public Criteria andDsNameLike(String value) {
            addCriterion("ds_name like", value, "dsName");
            return this;
        }

        public Criteria andDsNameNotLike(String value) {
            addCriterion("ds_name not like", value, "dsName");
            return this;
        }

        public Criteria andDsNameIn(List<String> values) {
            addCriterion("ds_name in", values, "dsName");
            return this;
        }

        public Criteria andDsNameNotIn(List<String> values) {
            addCriterion("ds_name not in", values, "dsName");
            return this;
        }

        public Criteria andDsNameBetween(String value1, String value2) {
            addCriterion("ds_name between", value1, value2, "dsName");
            return this;
        }

        public Criteria andDsNameNotBetween(String value1, String value2) {
            addCriterion("ds_name not between", value1, value2, "dsName");
            return this;
        }

        public Criteria andModifyTimeIsNull() {
            addCriterion("modify_time is null");
            return this;
        }

        public Criteria andModifyTimeIsNotNull() {
            addCriterion("modify_time is not null");
            return this;
        }

        public Criteria andModifyTimeEqualTo(Date value) {
            addCriterion("modify_time =", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeNotEqualTo(Date value) {
            addCriterion("modify_time <>", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeGreaterThan(Date value) {
            addCriterion("modify_time >", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("modify_time >=", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeLessThan(Date value) {
            addCriterion("modify_time <", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeLessThanOrEqualTo(Date value) {
            addCriterion("modify_time <=", value, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeIn(List<Date> values) {
            addCriterion("modify_time in", values, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeNotIn(List<Date> values) {
            addCriterion("modify_time not in", values, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeBetween(Date value1, Date value2) {
            addCriterion("modify_time between", value1, value2, "modifyTime");
            return this;
        }

        public Criteria andModifyTimeNotBetween(Date value1, Date value2) {
            addCriterion("modify_time not between", value1, value2, "modifyTime");
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
    }
}
