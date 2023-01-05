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

import com.qlangtech.tis.manage.biz.dal.pojo.AppTriggerJobRelation;
import com.qlangtech.tis.manage.biz.dal.pojo.AppTriggerJobRelationCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IAppTriggerJobRelationDAO {

    int countByExample(AppTriggerJobRelationCriteria example);

    int countFromWriteDB(AppTriggerJobRelationCriteria example);

    int deleteByExample(AppTriggerJobRelationCriteria criteria);

    int deleteByPrimaryKey(Long atId);

    Long insert(AppTriggerJobRelation record);

    Long insertSelective(AppTriggerJobRelation record);

    List<AppTriggerJobRelation> selectByExample(AppTriggerJobRelationCriteria criteria);

    List<AppTriggerJobRelation> selectByExample(AppTriggerJobRelationCriteria example, int page, int pageSize);

    AppTriggerJobRelation selectByPrimaryKey(Long atId);

    int updateByExampleSelective(AppTriggerJobRelation record, AppTriggerJobRelationCriteria example);

    int updateByExample(AppTriggerJobRelation record, AppTriggerJobRelationCriteria example);

    AppTriggerJobRelation loadFromWriteDB(Long atId);
}
