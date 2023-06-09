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
package com.qlangtech.tis.workflow.dao.impl;

import com.qlangtech.tis.workflow.dao.IWorkFlowDAO;
import com.qlangtech.tis.workflow.pojo.WorkFlow;
import com.qlangtech.tis.workflow.pojo.WorkFlowCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class WorkFlowDAOImpl extends BasicDAO<WorkFlow, WorkFlowCriteria> implements IWorkFlowDAO {

    public WorkFlowDAOImpl() {
        super();
    }

    public final String getEntityName() {
        return "work_flow";
    }

    public int countByExample(WorkFlowCriteria example) {
        Integer count = (Integer) this.count("work_flow.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(WorkFlowCriteria example) {
        Integer count = (Integer) this.countFromWriterDB("work_flow.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(WorkFlowCriteria criteria) {
        return this.deleteRecords("work_flow.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer id) {
        WorkFlow key = new WorkFlow();
        key.setId(id);
        return this.deleteRecords("work_flow.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(WorkFlow record) {
        Object newKey = this.insert("work_flow.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(WorkFlow record) {
        Object newKey = this.insert("work_flow.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<WorkFlow> selectByExample(WorkFlowCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<WorkFlow> selectByExample(WorkFlowCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<WorkFlow> list = this.list("work_flow.ibatorgenerated_selectByExample", example);
        return list;
    }

    public List<WorkFlow> minSelectByExample(WorkFlowCriteria criteria) {
        return this.minSelectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<WorkFlow> minSelectByExample(WorkFlowCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<WorkFlow> list = this.list("work_flow.ibatorgenerated_minSelectByExample", example);
        return list;
    }

    public WorkFlow selectByPrimaryKey(Integer id) {
        WorkFlow key = new WorkFlow();
        key.setId(id);
        WorkFlow record = (WorkFlow) this.load("work_flow.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(WorkFlow record, WorkFlowCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("work_flow.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(WorkFlow record, WorkFlowCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("work_flow.ibatorgenerated_updateByExample", parms);
    }

    public WorkFlow loadFromWriteDB(Integer id) {
        WorkFlow key = new WorkFlow();
        key.setId(id);
        WorkFlow record = (WorkFlow) this.loadFromWriterDB("work_flow.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends WorkFlowCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, WorkFlowCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
