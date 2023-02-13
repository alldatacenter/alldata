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
import com.qlangtech.tis.manage.biz.dal.dao.ISnapshotDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.SnapshotCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class SnapshotDAOImpl extends BasicDAO<Snapshot, SnapshotCriteria> implements ISnapshotDAO {

    public SnapshotDAOImpl() {
        super();
    }

    @Override
    public String getEntityName() {
        return "snapshot";
    }

    @Override
    public List<Snapshot> findPassTestSnapshot(SnapshotCriteria example) {
        example.setPage(1);
        example.setPageSize(100);
        List<Snapshot> list = this.list("snapshot.ibatorgenerated_select_pass_test_ByExample", example);
        return list;
    }

    @Override
    public Integer getMaxSnapshotId(SnapshotCriteria criteria) {
        return this.count("snapshot.ibatorgenerated_selectMaxSnapshotId", criteria);
    }

    public int countByExample(SnapshotCriteria example) {
        Integer count = this.count("snapshot.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(SnapshotCriteria example) {
        Integer count = this.countFromWriterDB("snapshot.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(SnapshotCriteria criteria) {
        return this.deleteRecords("snapshot.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer snId) {
        Snapshot key = new Snapshot();
        key.setSnId(snId);
        return this.deleteRecords("snapshot.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(Snapshot record) {
        return (Integer) this.insert("snapshot.ibatorgenerated_insert", record);
    }

    public Integer insertSelective(Snapshot record) {
        return (Integer) this.insert("snapshot.ibatorgenerated_insertSelective", record);
    }

    public List<Snapshot> selectByExample(SnapshotCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<Snapshot> selectByExample(SnapshotCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<Snapshot> list = this.list("snapshot.ibatorgenerated_selectByExample", example);
        return list;
    }

    public Snapshot selectByPrimaryKey(Integer snId) {
        Snapshot key = new Snapshot();
        key.setSnId(snId);
        Snapshot record = this.load("snapshot.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(Snapshot record, SnapshotCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("snapshot.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(Snapshot record, SnapshotCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("snapshot.ibatorgenerated_updateByExample", parms);
    }

    public Snapshot loadFromWriteDB(Integer snId) {
        Snapshot key = new Snapshot();
        key.setSnId(snId);
        Snapshot record = this.loadFromWriterDB("snapshot.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends SnapshotCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, SnapshotCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
