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

package com.qlangtech.tis.trigger.biz.dal.dao;

import com.qlangtech.tis.trigger.biz.dal.pojo.TaskExecLog;
import com.qlangtech.tis.trigger.biz.dal.pojo.TaskExecLogCriteria;

import java.util.List;

public interface ITaskExecLogDAO {
    int countByExample(TaskExecLogCriteria example);

    int countFromWriteDB(TaskExecLogCriteria example);

    int deleteByExample(TaskExecLogCriteria criteria);

    int deleteByPrimaryKey(Long execLogId);

    Long insert(TaskExecLog record);

    Long insertSelective(TaskExecLog record);

    List<TaskExecLog> selectByExampleWithBLOBs(TaskExecLogCriteria example);

    List<TaskExecLog> selectByExampleWithoutBLOBs(TaskExecLogCriteria criteria);

    List<TaskExecLog> selectByExampleWithoutBLOBs(TaskExecLogCriteria example, int page, int pageSize);

    TaskExecLog selectByPrimaryKey(Long execLogId);

    int updateByExampleSelective(TaskExecLog record, TaskExecLogCriteria example);

    int updateByExampleWithBLOBs(TaskExecLog record, TaskExecLogCriteria example);

    int updateByExampleWithoutBLOBs(TaskExecLog record, TaskExecLogCriteria example);

    TaskExecLog loadFromWriteDB(Long execLogId);
}
