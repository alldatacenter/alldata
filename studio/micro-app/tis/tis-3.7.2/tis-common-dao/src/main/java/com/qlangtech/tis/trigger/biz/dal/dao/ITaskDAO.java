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


import com.qlangtech.tis.trigger.biz.dal.pojo.ErrorJob;
import com.qlangtech.tis.trigger.biz.dal.pojo.Task;
import com.qlangtech.tis.trigger.biz.dal.pojo.TaskCriteria;

import java.util.List;

public interface ITaskDAO {
    int countByExample(TaskCriteria example);

    int countFromWriteDB(TaskCriteria example);

    int deleteByExample(TaskCriteria criteria);

    int deleteByPrimaryKey(Long taskId);

    Long insert(Task record);

    Long insertSelective(Task record);

    List<Task> selectByExample(TaskCriteria criteria);

    List<Task> selectByExample(TaskCriteria example, int page, int pageSize);

    Task selectByPrimaryKey(Long taskId);

    int updateByExampleSelective(Task record, TaskCriteria example);

    int updateByExample(Task record, TaskCriteria example);

    Task loadFromWriteDB(Long taskId);

    List<ErrorJob> getRecentExecuteJobs(String environment);
}
