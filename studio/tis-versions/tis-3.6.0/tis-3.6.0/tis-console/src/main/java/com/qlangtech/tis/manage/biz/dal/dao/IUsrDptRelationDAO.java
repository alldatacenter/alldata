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
package com.qlangtech.tis.manage.biz.dal.dao;

import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelation;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.TriggerCrontab;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IUsrDptRelationDAO {

  void addAdminUser();

  int countByExample(UsrDptRelationCriteria example);

  int countFromWriteDB(UsrDptRelationCriteria example);

  int deleteByExample(UsrDptRelationCriteria criteria);

  int deleteByPrimaryKey(String usrId);

  void insert(UsrDptRelation record);

  void insertSelective(UsrDptRelation record);

  List<UsrDptRelation> selectByExample(UsrDptRelationCriteria criteria);

  List<UsrDptRelation> selectByExample(UsrDptRelationCriteria example, int page, int pageSize);

  UsrDptRelation selectByPrimaryKey(String usrId);

  int updateByExampleSelective(UsrDptRelation record, UsrDptRelationCriteria example);

  int updateByExample(UsrDptRelation record, UsrDptRelationCriteria example);

  UsrDptRelation loadFromWriteDB(String usrId);

  List<TriggerCrontab> selectAppDumpJob(UsrDptRelationCriteria criteria);
}
