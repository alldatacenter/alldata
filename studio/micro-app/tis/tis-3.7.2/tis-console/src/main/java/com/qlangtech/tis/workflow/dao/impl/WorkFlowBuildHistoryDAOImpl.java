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

import com.qlangtech.tis.manage.common.BasicDAO;
import com.qlangtech.tis.workflow.dao.IWorkFlowBuildHistoryDAO;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistoryCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class WorkFlowBuildHistoryDAOImpl extends BasicDAO<WorkFlowBuildHistory, WorkFlowBuildHistoryCriteria> implements IWorkFlowBuildHistoryDAO {

    public WorkFlowBuildHistoryDAOImpl() {
        super();
    }

    public final String getEntityName() {
        return "work_flow_build_history";
    }

    public int countByExample(WorkFlowBuildHistoryCriteria example) {
        Integer count = (Integer) this.count("work_flow_build_history.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(WorkFlowBuildHistoryCriteria example) {
        Integer count = (Integer) this.countFromWriterDB("work_flow_build_history.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(WorkFlowBuildHistoryCriteria criteria) {
        return this.deleteRecords("work_flow_build_history.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer id) {
        WorkFlowBuildHistory key = new WorkFlowBuildHistory();
        key.setId(id);
        return this.deleteRecords("work_flow_build_history.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(WorkFlowBuildHistory record) {
        Object newKey = this.insert("work_flow_build_history.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(WorkFlowBuildHistory record) {
        Object newKey = this.insert("work_flow_build_history.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<WorkFlowBuildHistory> selectByExample(WorkFlowBuildHistoryCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<WorkFlowBuildHistory> selectByExample(WorkFlowBuildHistoryCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<WorkFlowBuildHistory> list = this.list("work_flow_build_history.ibatorgenerated_selectByExample", example);
        return list;
    }

    public WorkFlowBuildHistory selectByPrimaryKey(Integer id) {
        WorkFlowBuildHistory key = new WorkFlowBuildHistory();
        key.setId(id);
        WorkFlowBuildHistory record = (WorkFlowBuildHistory) this.load("work_flow_build_history.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(WorkFlowBuildHistory record, WorkFlowBuildHistoryCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("work_flow_build_history.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(WorkFlowBuildHistory record, WorkFlowBuildHistoryCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("work_flow_build_history.ibatorgenerated_updateByExample", parms);
    }

    public WorkFlowBuildHistory loadFromWriteDB(Integer id) {
        WorkFlowBuildHistory key = new WorkFlowBuildHistory();
        key.setId(id);
        WorkFlowBuildHistory record = (WorkFlowBuildHistory) this.loadFromWriterDB("work_flow_build_history.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends WorkFlowBuildHistoryCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, WorkFlowBuildHistoryCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
