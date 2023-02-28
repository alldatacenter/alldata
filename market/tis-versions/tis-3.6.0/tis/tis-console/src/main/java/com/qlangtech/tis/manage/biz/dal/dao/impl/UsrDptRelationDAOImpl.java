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

import com.qlangtech.tis.manage.biz.dal.dao.IUsrDptRelationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelation;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;
import com.qlangtech.tis.manage.common.TriggerCrontab;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class UsrDptRelationDAOImpl extends BasicDAO<UsrDptRelation, UsrDptRelationCriteria> implements IUsrDptRelationDAO {

  public UsrDptRelationDAOImpl() {
    super();
  }

  /**
   * 添加一个新的系统管理员用户
   */
  @Override
  public void addAdminUser() {
    this.insert("usr_dpt_relation.ibatorgenerated_add_admin_user", new UsrDptRelation());
  }

  @Override
  public List<TriggerCrontab> selectAppDumpJob(UsrDptRelationCriteria criteria) {
    return this.listAnonymity("usr_dpt_relation.ibatorgenerated_select_out_join_app_trigger_job_relation_ByExample", criteria);
  }

  @Override
  public String getEntityName() {
    return "usr_dpt_relation";
  }

  public int countByExample(UsrDptRelationCriteria example) {
    Integer count = this.count("usr_dpt_relation.ibatorgenerated_countByExample", example);
    return count;
  }

  public int countFromWriteDB(UsrDptRelationCriteria example) {
    Integer count = this.countFromWriterDB("usr_dpt_relation.ibatorgenerated_countByExample", example);
    return count;
  }

  public int deleteByExample(UsrDptRelationCriteria criteria) {
    return this.deleteRecords("usr_dpt_relation.ibatorgenerated_deleteByExample", criteria);
  }

  public int deleteByPrimaryKey(String usrId) {
    UsrDptRelation key = new UsrDptRelation();
    key.setUsrId(usrId);
    return this.deleteRecords("usr_dpt_relation.ibatorgenerated_deleteByPrimaryKey", key);
  }

  public void insert(UsrDptRelation record) {
    this.insert("usr_dpt_relation.ibatorgenerated_insert", record);
  }

  public void insertSelective(UsrDptRelation record) {
    this.insert("usr_dpt_relation.ibatorgenerated_insertSelective", record);
  }

  public List<UsrDptRelation> selectByExample(UsrDptRelationCriteria criteria) {
    return this.selectByExample(criteria, 1, 100);
  }

  @SuppressWarnings("all")
  public List<UsrDptRelation> selectByExample(UsrDptRelationCriteria example, int page, int pageSize) {
    example.setPage(page);
    example.setPageSize(pageSize);
    List<UsrDptRelation> list = this.list("usr_dpt_relation.ibatorgenerated_selectByExample", example);
    return list;
  }

  public UsrDptRelation selectByPrimaryKey(String usrId) {
    UsrDptRelation key = new UsrDptRelation();
    key.setUsrId(usrId);
    UsrDptRelation record = this.load("usr_dpt_relation.ibatorgenerated_selectByPrimaryKey", key);
    return record;
  }

  public int updateByExampleSelective(UsrDptRelation record, UsrDptRelationCriteria example) {
    UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
    return this.updateRecords("usr_dpt_relation.ibatorgenerated_updateByExampleSelective", parms);
  }

  public int updateByExample(UsrDptRelation record, UsrDptRelationCriteria example) {
    UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
    return this.updateRecords("usr_dpt_relation.ibatorgenerated_updateByExample", parms);
  }

  public UsrDptRelation loadFromWriteDB(String usrId) {
    UsrDptRelation key = new UsrDptRelation();
    key.setUsrId(usrId);
    UsrDptRelation record = this.loadFromWriterDB("usr_dpt_relation.ibatorgenerated_selectByPrimaryKey", key);
    return record;
  }

  private static class UpdateByExampleParms extends UsrDptRelationCriteria {

    private Object record;

    public UpdateByExampleParms(Object record, UsrDptRelationCriteria example) {
      super(example);
      this.record = record;
    }

    public Object getRecord() {
      return record;
    }
  }
}
