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
package com.qlangtech.tis.manage.biz.dal.dao.impl;

import com.qlangtech.tis.manage.biz.dal.dao.IDepartmentDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DepartmentDAOImpl extends BasicDAO<Department, DepartmentCriteria> implements IDepartmentDAO {

  @Override
  public String getEntityName() {
    return "department";
  }

  @Override
  public List<Department> selectByInnerJoinWithExtraDptUsrRelation(String userid) {
    return this.listAnonymity("department.ibatorgenerated_join_with_extra_dpt_usr_relation", userid);
  }

  public DepartmentDAOImpl() {
    super();
  }

  public int countByExample(DepartmentCriteria example) {
    Integer count = this.count("department.ibatorgenerated_countByExample", example);
    return count;
  }

  public int countFromWriteDB(DepartmentCriteria example) {
    Integer count = this.countFromWriterDB("department.ibatorgenerated_countByExample", example);
    return count;
  }

  public int deleteByExample(DepartmentCriteria criteria) {
    return this.deleteRecords("department.ibatorgenerated_deleteByExample", criteria);
  }

  public int deleteByPrimaryKey(Integer dptId) {
    Department key = new Department();
    key.setDptId(dptId);
    return this.deleteRecords("department.ibatorgenerated_deleteByPrimaryKey", key);
  }

  public Integer insert(Department record) {
    return this.insert("department.ibatorgenerated_insert", record);
    // return getLastInsertPkVal();
  }

  public Integer insertSelective(Department record) {
    return this.insert("department.ibatorgenerated_insertSelective", record);
    // get_last_insert_id

  }


  public List<Department> selectByExample(DepartmentCriteria criteria) {
    return this.selectByExample(criteria, 1, 100);
  }

  @SuppressWarnings("unchecked")
  public List<Department> selectByExample(DepartmentCriteria example, int page, int pageSize) {
    example.setPage(page);
    example.setPageSize(pageSize);
    List<Department> list = this.list("department.ibatorgenerated_selectByExample", example);
    return list;
  }

  public Department selectByPrimaryKey(Integer dptId) {
    Department key = new Department();
    key.setDptId(dptId);
    Department record = this.load("department.ibatorgenerated_selectByPrimaryKey", key);
    return record;
  }

  public int updateByExampleSelective(Department record, DepartmentCriteria example) {
    UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
    return this.updateRecords("department.ibatorgenerated_updateByExampleSelective", parms);
  }

  public int updateByExample(Department record, DepartmentCriteria example) {
    UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
    return this.updateRecords("department.ibatorgenerated_updateByExample", parms);
  }

  public Department loadFromWriteDB(Integer dptId) {
    Department key = new Department();
    key.setDptId(dptId);
    Department record = this.loadFromWriterDB("department.ibatorgenerated_selectByPrimaryKey", key);
    return record;
  }

  private static class UpdateByExampleParms extends DepartmentCriteria {

    private Object record;

    public UpdateByExampleParms(Object record, DepartmentCriteria example) {
      super(example);
      this.record = record;
    }

    public Object getRecord() {
      return record;
    }
  }
}
