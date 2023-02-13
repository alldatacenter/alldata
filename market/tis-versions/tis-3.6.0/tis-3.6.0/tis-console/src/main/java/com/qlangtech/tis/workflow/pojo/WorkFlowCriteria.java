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
package com.qlangtech.tis.workflow.pojo;

import com.qlangtech.tis.manage.common.TISBaseCriteria;
import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class WorkFlowCriteria extends TISBaseCriteria {

    protected String orderByClause;

    protected List<Criteria> oredCriteria;

    public WorkFlowCriteria() {
        oredCriteria = new ArrayList<Criteria>();
    }

    protected WorkFlowCriteria(WorkFlowCriteria example) {
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

        public Criteria andIdIsNull() {
            addCriterion("id is null");
            return this;
        }

        public Criteria andIdIsNotNull() {
            addCriterion("id is not null");
            return this;
        }

        public Criteria andIdEqualTo(Integer value) {
            addCriterion("id =", value, "id");
            return this;
        }

        public Criteria andIdNotEqualTo(Integer value) {
            addCriterion("id <>", value, "id");
            return this;
        }

        public Criteria andIdGreaterThan(Integer value) {
            addCriterion("id >", value, "id");
            return this;
        }

        public Criteria andIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("id >=", value, "id");
            return this;
        }

        public Criteria andIdLessThan(Integer value) {
            addCriterion("id <", value, "id");
            return this;
        }

        public Criteria andIdLessThanOrEqualTo(Integer value) {
            addCriterion("id <=", value, "id");
            return this;
        }

        public Criteria andIdIn(List<Integer> values) {
            addCriterion("id in", values, "id");
            return this;
        }

        public Criteria andIdNotIn(List<Integer> values) {
            addCriterion("id not in", values, "id");
            return this;
        }

        public Criteria andIdBetween(Integer value1, Integer value2) {
            addCriterion("id between", value1, value2, "id");
            return this;
        }

        public Criteria andIdNotBetween(Integer value1, Integer value2) {
            addCriterion("id not between", value1, value2, "id");
            return this;
        }

        public Criteria andNameIsNull() {
            addCriterion("name is null");
            return this;
        }

        public Criteria andNameIsNotNull() {
            addCriterion("name is not null");
            return this;
        }

        public Criteria andNameEqualTo(String value) {
            addCriterion("name =", value, "name");
            return this;
        }

        public Criteria andNameNotEqualTo(String value) {
            addCriterion("name <>", value, "name");
            return this;
        }

        public Criteria andNameGreaterThan(String value) {
            addCriterion("name >", value, "name");
            return this;
        }

        public Criteria andNameGreaterThanOrEqualTo(String value) {
            addCriterion("name >=", value, "name");
            return this;
        }

        public Criteria andNameLessThan(String value) {
            addCriterion("name <", value, "name");
            return this;
        }

        public Criteria andNameLessThanOrEqualTo(String value) {
            addCriterion("name <=", value, "name");
            return this;
        }

        public Criteria andNameLike(String value) {
            addCriterion("name like", value, "name");
            return this;
        }

        public Criteria andNameNotLike(String value) {
            addCriterion("name not like", value, "name");
            return this;
        }

        public Criteria andNameIn(List<String> values) {
            addCriterion("name in", values, "name");
            return this;
        }

        public Criteria andNameNotIn(List<String> values) {
            addCriterion("name not in", values, "name");
            return this;
        }

        public Criteria andNameBetween(String value1, String value2) {
            addCriterion("name between", value1, value2, "name");
            return this;
        }

        public Criteria andNameNotBetween(String value1, String value2) {
            addCriterion("name not between", value1, value2, "name");
            return this;
        }

        public Criteria andOpUserIdIsNull() {
            addCriterion("op_user_id is null");
            return this;
        }

        public Criteria andOpUserIdIsNotNull() {
            addCriterion("op_user_id is not null");
            return this;
        }

        public Criteria andOpUserIdEqualTo(Integer value) {
            addCriterion("op_user_id =", value, "opUserId");
            return this;
        }

        public Criteria andOpUserIdNotEqualTo(Integer value) {
            addCriterion("op_user_id <>", value, "opUserId");
            return this;
        }

        public Criteria andOpUserIdGreaterThan(Integer value) {
            addCriterion("op_user_id >", value, "opUserId");
            return this;
        }

        public Criteria andOpUserIdGreaterThanOrEqualTo(Integer value) {
            addCriterion("op_user_id >=", value, "opUserId");
            return this;
        }

        public Criteria andOpUserIdLessThan(Integer value) {
            addCriterion("op_user_id <", value, "opUserId");
            return this;
        }

        public Criteria andOpUserIdLessThanOrEqualTo(Integer value) {
            addCriterion("op_user_id <=", value, "opUserId");
            return this;
        }

        public Criteria andOpUserIdIn(List<Integer> values) {
            addCriterion("op_user_id in", values, "opUserId");
            return this;
        }

        public Criteria andOpUserIdNotIn(List<Integer> values) {
            addCriterion("op_user_id not in", values, "opUserId");
            return this;
        }

        public Criteria andOpUserIdBetween(Integer value1, Integer value2) {
            addCriterion("op_user_id between", value1, value2, "opUserId");
            return this;
        }

        public Criteria andOpUserIdNotBetween(Integer value1, Integer value2) {
            addCriterion("op_user_id not between", value1, value2, "opUserId");
            return this;
        }

        public Criteria andOpUserNameIsNull() {
            addCriterion("op_user_name is null");
            return this;
        }

        public Criteria andOpUserNameIsNotNull() {
            addCriterion("op_user_name is not null");
            return this;
        }

        public Criteria andOpUserNameEqualTo(String value) {
            addCriterion("op_user_name =", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameNotEqualTo(String value) {
            addCriterion("op_user_name <>", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameGreaterThan(String value) {
            addCriterion("op_user_name >", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameGreaterThanOrEqualTo(String value) {
            addCriterion("op_user_name >=", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameLessThan(String value) {
            addCriterion("op_user_name <", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameLessThanOrEqualTo(String value) {
            addCriterion("op_user_name <=", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameLike(String value) {
            addCriterion("op_user_name like", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameNotLike(String value) {
            addCriterion("op_user_name not like", value, "opUserName");
            return this;
        }

        public Criteria andOpUserNameIn(List<String> values) {
            addCriterion("op_user_name in", values, "opUserName");
            return this;
        }

        public Criteria andOpUserNameNotIn(List<String> values) {
            addCriterion("op_user_name not in", values, "opUserName");
            return this;
        }

        public Criteria andOpUserNameBetween(String value1, String value2) {
            addCriterion("op_user_name between", value1, value2, "opUserName");
            return this;
        }

        public Criteria andOpUserNameNotBetween(String value1, String value2) {
            addCriterion("op_user_name not between", value1, value2, "opUserName");
            return this;
        }

        public Criteria andGitPathIsNull() {
            addCriterion("git_path is null");
            return this;
        }

        public Criteria andGitPathIsNotNull() {
            addCriterion("git_path is not null");
            return this;
        }

        public Criteria andGitPathEqualTo(String value) {
            addCriterion("git_path =", value, "gitPath");
            return this;
        }

        public Criteria andGitPathNotEqualTo(String value) {
            addCriterion("git_path <>", value, "gitPath");
            return this;
        }

        public Criteria andGitPathGreaterThan(String value) {
            addCriterion("git_path >", value, "gitPath");
            return this;
        }

        public Criteria andGitPathGreaterThanOrEqualTo(String value) {
            addCriterion("git_path >=", value, "gitPath");
            return this;
        }

        public Criteria andGitPathLessThan(String value) {
            addCriterion("git_path <", value, "gitPath");
            return this;
        }

        public Criteria andGitPathLessThanOrEqualTo(String value) {
            addCriterion("git_path <=", value, "gitPath");
            return this;
        }

        public Criteria andGitPathLike(String value) {
            addCriterion("git_path like", value, "gitPath");
            return this;
        }

        public Criteria andGitPathNotLike(String value) {
            addCriterion("git_path not like", value, "gitPath");
            return this;
        }

        public Criteria andGitPathIn(List<String> values) {
            addCriterion("git_path in", values, "gitPath");
            return this;
        }

        public Criteria andGitPathNotIn(List<String> values) {
            addCriterion("git_path not in", values, "gitPath");
            return this;
        }

        public Criteria andGitPathBetween(String value1, String value2) {
            addCriterion("git_path between", value1, value2, "gitPath");
            return this;
        }

        public Criteria andGitPathNotBetween(String value1, String value2) {
            addCriterion("git_path not between", value1, value2, "gitPath");
            return this;
        }

        public Criteria andInChangeIsNull() {
            addCriterion("in_change is null");
            return this;
        }

        public Criteria andInChangeIsNotNull() {
            addCriterion("in_change is not null");
            return this;
        }

        public Criteria andInChangeEqualTo(Byte value) {
            addCriterion("in_change =", value, "inChange");
            return this;
        }

        public Criteria andInChangeNotEqualTo(Byte value) {
            addCriterion("in_change <>", value, "inChange");
            return this;
        }

        public Criteria andInChangeGreaterThan(Byte value) {
            addCriterion("in_change >", value, "inChange");
            return this;
        }

        public Criteria andInChangeGreaterThanOrEqualTo(Byte value) {
            addCriterion("in_change >=", value, "inChange");
            return this;
        }

        public Criteria andInChangeLessThan(Byte value) {
            addCriterion("in_change <", value, "inChange");
            return this;
        }

        public Criteria andInChangeLessThanOrEqualTo(Byte value) {
            addCriterion("in_change <=", value, "inChange");
            return this;
        }

        public Criteria andInChangeIn(List<Byte> values) {
            addCriterion("in_change in", values, "inChange");
            return this;
        }

        public Criteria andInChangeNotIn(List<Byte> values) {
            addCriterion("in_change not in", values, "inChange");
            return this;
        }

        public Criteria andInChangeBetween(Byte value1, Byte value2) {
            addCriterion("in_change between", value1, value2, "inChange");
            return this;
        }

        public Criteria andInChangeNotBetween(Byte value1, Byte value2) {
            addCriterion("in_change not between", value1, value2, "inChange");
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

        public Criteria andOpTimeIsNull() {
            addCriterion("op_time is null");
            return this;
        }

        public Criteria andOpTimeIsNotNull() {
            addCriterion("op_time is not null");
            return this;
        }

        public Criteria andOpTimeEqualTo(Date value) {
            addCriterion("op_time =", value, "opTime");
            return this;
        }

        public Criteria andOpTimeNotEqualTo(Date value) {
            addCriterion("op_time <>", value, "opTime");
            return this;
        }

        public Criteria andOpTimeGreaterThan(Date value) {
            addCriterion("op_time >", value, "opTime");
            return this;
        }

        public Criteria andOpTimeGreaterThanOrEqualTo(Date value) {
            addCriterion("op_time >=", value, "opTime");
            return this;
        }

        public Criteria andOpTimeLessThan(Date value) {
            addCriterion("op_time <", value, "opTime");
            return this;
        }

        public Criteria andOpTimeLessThanOrEqualTo(Date value) {
            addCriterion("op_time <=", value, "opTime");
            return this;
        }

        public Criteria andOpTimeIn(List<Date> values) {
            addCriterion("op_time in", values, "opTime");
            return this;
        }

        public Criteria andOpTimeNotIn(List<Date> values) {
            addCriterion("op_time not in", values, "opTime");
            return this;
        }

        public Criteria andOpTimeBetween(Date value1, Date value2) {
            addCriterion("op_time between", value1, value2, "opTime");
            return this;
        }

        public Criteria andOpTimeNotBetween(Date value1, Date value2) {
            addCriterion("op_time not between", value1, value2, "opTime");
            return this;
        }
    }
}
