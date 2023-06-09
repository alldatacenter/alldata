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

// import com.taobao.ibatis.extend.BasicCriteria;

import com.qlangtech.tis.manage.common.TISBaseCriteria;
import com.qlangtech.tis.manage.common.TISBaseCriteria;
import com.qlangtech.tis.manage.common.ibatis.BooleanYorNConvertCallback;

import java.util.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DepartmentCriteria extends TISBaseCriteria {

  protected String orderByClause;

  protected List<Criteria> oredCriteria;

  public DepartmentCriteria() {
    oredCriteria = new ArrayList<Criteria>();
  }

  protected DepartmentCriteria(DepartmentCriteria example) {
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

    public Criteria andDptIdIsNull() {
      addCriterion("dpt_id is null");
      return this;
    }

    public Criteria andDptIdIsNotNull() {
      addCriterion("dpt_id is not null");
      return this;
    }

    public Criteria andDptIdEqualTo(Integer value) {
      addCriterion("dpt_id =", value, "dptId");
      return this;
    }

    public Criteria andDptIdNotEqualTo(Integer value) {
      addCriterion("dpt_id <>", value, "dptId");
      return this;
    }

    public Criteria andDptIdGreaterThan(Integer value) {
      addCriterion("dpt_id >", value, "dptId");
      return this;
    }

    public Criteria andDptIdGreaterThanOrEqualTo(Integer value) {
      addCriterion("dpt_id >=", value, "dptId");
      return this;
    }

    public Criteria andDptIdLessThan(Integer value) {
      addCriterion("dpt_id <", value, "dptId");
      return this;
    }

    public Criteria andDptIdLessThanOrEqualTo(Integer value) {
      addCriterion("dpt_id <=", value, "dptId");
      return this;
    }

    public Criteria andDptIdIn(List<Integer> values) {
      addCriterion("dpt_id in", values, "dptId");
      return this;
    }

    public Criteria andDptIdNotIn(List<Integer> values) {
      addCriterion("dpt_id not in", values, "dptId");
      return this;
    }

    public Criteria andDptIdBetween(Integer value1, Integer value2) {
      addCriterion("dpt_id between", value1, value2, "dptId");
      return this;
    }

    public Criteria andDptIdNotBetween(Integer value1, Integer value2) {
      addCriterion("dpt_id not between", value1, value2, "dptId");
      return this;
    }

    public Criteria andParentIdIsNull() {
      addCriterion("parent_id = -1");
      return this;
    }

    public Criteria andParentIdIsNotNull() {
      addCriterion("parent_id is not null");
      return this;
    }

    public Criteria andParentIdEqualTo(Integer value) {
      addCriterion("parent_id =", value, "parentId");
      return this;
    }

    public Criteria andParentIdNotEqualTo(Integer value) {
      addCriterion("parent_id <>", value, "parentId");
      return this;
    }

    public Criteria andParentIdGreaterThan(Integer value) {
      addCriterion("parent_id >", value, "parentId");
      return this;
    }

    public Criteria andParentIdGreaterThanOrEqualTo(Integer value) {
      addCriterion("parent_id >=", value, "parentId");
      return this;
    }

    public Criteria andParentIdLessThan(Integer value) {
      addCriterion("parent_id <", value, "parentId");
      return this;
    }

    public Criteria andParentIdLessThanOrEqualTo(Integer value) {
      addCriterion("parent_id <=", value, "parentId");
      return this;
    }

    public Criteria andParentIdIn(List<Integer> values) {
      addCriterion("parent_id in", values, "parentId");
      return this;
    }

    public Criteria andParentIdNotIn(List<Integer> values) {
      addCriterion("parent_id not in", values, "parentId");
      return this;
    }

    public Criteria andParentIdBetween(Integer value1, Integer value2) {
      addCriterion("parent_id between", value1, value2, "parentId");
      return this;
    }

    public Criteria andParentIdNotBetween(Integer value1, Integer value2) {
      addCriterion("parent_id not between", value1, value2, "parentId");
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

    public Criteria andGmtCreateIsNull() {
      addCriterion("gmt_create is null");
      return this;
    }

    public Criteria andGmtCreateIsNotNull() {
      addCriterion("gmt_create is not null");
      return this;
    }

    public Criteria andGmtCreateEqualTo(Date value) {
      addCriterion("gmt_create =", value, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateNotEqualTo(Date value) {
      addCriterion("gmt_create <>", value, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateGreaterThan(Date value) {
      addCriterion("gmt_create >", value, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateGreaterThanOrEqualTo(Date value) {
      addCriterion("gmt_create >=", value, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateLessThan(Date value) {
      addCriterion("gmt_create <", value, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateLessThanOrEqualTo(Date value) {
      addCriterion("gmt_create <=", value, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateIn(List<Date> values) {
      addCriterion("gmt_create in", values, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateNotIn(List<Date> values) {
      addCriterion("gmt_create not in", values, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateBetween(Date value1, Date value2) {
      addCriterion("gmt_create between", value1, value2, "gmtCreate");
      return this;
    }

    public Criteria andGmtCreateNotBetween(Date value1, Date value2) {
      addCriterion("gmt_create not between", value1, value2, "gmtCreate");
      return this;
    }

    public Criteria andGmtModifiedIsNull() {
      addCriterion("gmt_modified is null");
      return this;
    }

    public Criteria andGmtModifiedIsNotNull() {
      addCriterion("gmt_modified is not null");
      return this;
    }

    public Criteria andGmtModifiedEqualTo(Date value) {
      addCriterion("gmt_modified =", value, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedNotEqualTo(Date value) {
      addCriterion("gmt_modified <>", value, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedGreaterThan(Date value) {
      addCriterion("gmt_modified >", value, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedGreaterThanOrEqualTo(Date value) {
      addCriterion("gmt_modified >=", value, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedLessThan(Date value) {
      addCriterion("gmt_modified <", value, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedLessThanOrEqualTo(Date value) {
      addCriterion("gmt_modified <=", value, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedIn(List<Date> values) {
      addCriterion("gmt_modified in", values, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedNotIn(List<Date> values) {
      addCriterion("gmt_modified not in", values, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedBetween(Date value1, Date value2) {
      addCriterion("gmt_modified between", value1, value2, "gmtModified");
      return this;
    }

    public Criteria andGmtModifiedNotBetween(Date value1, Date value2) {
      addCriterion("gmt_modified not between", value1, value2, "gmtModified");
      return this;
    }

    public Criteria andFullNameIsNull() {
      addCriterion("full_name is null");
      return this;
    }

    public Criteria andFullNameIsNotNull() {
      addCriterion("full_name is not null");
      return this;
    }

    public Criteria andFullNameEqualTo(String value) {
      addCriterion("full_name =", value, "fullName");
      return this;
    }

    public Criteria andFullNameNotEqualTo(String value) {
      addCriterion("full_name <>", value, "fullName");
      return this;
    }

    public Criteria andFullNameGreaterThan(String value) {
      addCriterion("full_name >", value, "fullName");
      return this;
    }

    public Criteria andFullNameGreaterThanOrEqualTo(String value) {
      addCriterion("full_name >=", value, "fullName");
      return this;
    }

    public Criteria andFullNameLessThan(String value) {
      addCriterion("full_name <", value, "fullName");
      return this;
    }

    public Criteria andFullNameLessThanOrEqualTo(String value) {
      addCriterion("full_name <=", value, "fullName");
      return this;
    }

    public Criteria andFullNameLike(String value) {
      addCriterion("full_name like", value, "fullName");
      return this;
    }

    public Criteria andFullNameNotLike(String value) {
      addCriterion("full_name not like", value, "fullName");
      return this;
    }

    public Criteria andFullNameIn(List<String> values) {
      addCriterion("full_name in", values, "fullName");
      return this;
    }

    public Criteria andFullNameNotIn(List<String> values) {
      addCriterion("full_name not in", values, "fullName");
      return this;
    }

    public Criteria andFullNameBetween(String value1, String value2) {
      addCriterion("full_name between", value1, value2, "fullName");
      return this;
    }

    public Criteria andFullNameNotBetween(String value1, String value2) {
      addCriterion("full_name not between", value1, value2, "fullName");
      return this;
    }

    public Criteria andLeafIsNull() {
      addCriterion("leaf is null");
      return this;
    }

    public Criteria andLeafIsNotNull() {
      addCriterion("leaf is not null");
      return this;
    }

    public Criteria andLeafEqualTo(String value) {
      addCriterion("leaf =", value, "leaf");
      return this;
    }

    public Criteria andIsLeaf(boolean leaf) {
      addCriterion("leaf =", (leaf ? BooleanYorNConvertCallback.YES : BooleanYorNConvertCallback.NO), "leaf");
      return this;
    }

    public Criteria andLeafNotEqualTo(String value) {
      addCriterion("leaf <>", value, "leaf");
      return this;
    }

    public Criteria andLeafGreaterThan(String value) {
      addCriterion("leaf >", value, "leaf");
      return this;
    }

    public Criteria andLeafGreaterThanOrEqualTo(String value) {
      addCriterion("leaf >=", value, "leaf");
      return this;
    }

    public Criteria andLeafLessThan(String value) {
      addCriterion("leaf <", value, "leaf");
      return this;
    }

    public Criteria andLeafLessThanOrEqualTo(String value) {
      addCriterion("leaf <=", value, "leaf");
      return this;
    }

    public Criteria andLeafLike(String value) {
      addCriterion("leaf like", value, "leaf");
      return this;
    }

    public Criteria andLeafNotLike(String value) {
      addCriterion("leaf not like", value, "leaf");
      return this;
    }

    public Criteria andLeafIn(List<String> values) {
      addCriterion("leaf in", values, "leaf");
      return this;
    }

    public Criteria andLeafNotIn(List<String> values) {
      addCriterion("leaf not in", values, "leaf");
      return this;
    }

    public Criteria andLeafBetween(String value1, String value2) {
      addCriterion("leaf between", value1, value2, "leaf");
      return this;
    }

    public Criteria andLeafNotBetween(String value1, String value2) {
      addCriterion("leaf not between", value1, value2, "leaf");
      return this;
    }
  }
}
