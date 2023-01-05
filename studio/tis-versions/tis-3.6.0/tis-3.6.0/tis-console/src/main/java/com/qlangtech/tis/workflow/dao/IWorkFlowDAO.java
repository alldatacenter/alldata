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
package com.qlangtech.tis.workflow.dao;

import com.qlangtech.tis.workflow.pojo.WorkFlow;
import com.qlangtech.tis.workflow.pojo.WorkFlowCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IWorkFlowDAO {

    int countByExample(WorkFlowCriteria example);

    int countFromWriteDB(WorkFlowCriteria example);

    int deleteByExample(WorkFlowCriteria criteria);

    int deleteByPrimaryKey(Integer id);

    Integer insert(WorkFlow record);

    Integer insertSelective(WorkFlow record);

    List<WorkFlow> selectByExample(WorkFlowCriteria criteria);

    List<WorkFlow> selectByExample(WorkFlowCriteria example, int page, int pageSize);

    List<WorkFlow> minSelectByExample(WorkFlowCriteria criteria);

    List<WorkFlow> minSelectByExample(WorkFlowCriteria example, int page, int pageSize);

    WorkFlow selectByPrimaryKey(Integer id);

    int updateByExampleSelective(WorkFlow record, WorkFlowCriteria example);

    int updateByExample(WorkFlow record, WorkFlowCriteria example);

    WorkFlow loadFromWriteDB(Integer id);
}
