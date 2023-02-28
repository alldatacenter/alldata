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

import java.util.List;
import com.qlangtech.tis.manage.biz.dal.dao.IOperationLogDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.OperationLog;
import com.qlangtech.tis.manage.biz.dal.pojo.OperationLogCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class OperationLogDAOImpl extends BasicDAO<OperationLog, OperationLogCriteria> implements IOperationLogDAO {

    @Override
    public String getEntityName() {
        return "operation_log";
    }

    public OperationLogDAOImpl() {
        super();
    }

    public int countByExample(OperationLogCriteria example) {
        Integer count = this.count("operation_log.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(OperationLogCriteria example) {
        Integer count = this.countFromWriterDB("operation_log.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(OperationLogCriteria criteria) {
        return this.deleteRecords("operation_log.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer opId) {
        OperationLog key = new OperationLog();
        key.setOpId(opId);
        return this.deleteRecords("operation_log.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(OperationLog record) {
        Object newKey = this.insert("operation_log.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(OperationLog record) {
        Object newKey = this.insert("operation_log.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    @SuppressWarnings("unchecked")
    public List<OperationLog> selectByExampleWithBLOBs(OperationLogCriteria example) {
        List<OperationLog> list = this.list("operation_log.ibatorgenerated_selectByExampleWithBLOBs", example);
        return list;
    }

    public List<OperationLog> selectByExampleWithoutBLOBs(OperationLogCriteria criteria) {
        return this.selectByExampleWithoutBLOBs(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<OperationLog> selectByExampleWithoutBLOBs(OperationLogCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<OperationLog> list = this.list("operation_log.ibatorgenerated_selectByExample", example);
        return list;
    }

    public OperationLog selectByPrimaryKey(Integer opId) {
        OperationLog key = new OperationLog();
        key.setOpId(opId);
        OperationLog record = this.load("operation_log.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(OperationLog record, OperationLogCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("operation_log.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExampleWithBLOBs(OperationLog record, OperationLogCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        int rows = this.updateRecords("operation_log.ibatorgenerated_updateByExampleWithBLOBs", parms);
        return rows;
    }

    public int updateByExampleWithoutBLOBs(OperationLog record, OperationLogCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("operation_log.ibatorgenerated_updateByExample", parms);
    }

    public OperationLog loadFromWriteDB(Integer opId) {
        OperationLog key = new OperationLog();
        key.setOpId(opId);
        OperationLog record = this.loadFromWriterDB("operation_log.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends OperationLogCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, OperationLogCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
