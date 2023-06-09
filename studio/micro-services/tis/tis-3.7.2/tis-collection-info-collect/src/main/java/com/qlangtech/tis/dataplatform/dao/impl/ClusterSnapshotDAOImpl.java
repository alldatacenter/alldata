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
package com.qlangtech.tis.dataplatform.dao.impl;

import java.util.Date;
import java.util.List;
import com.qlangtech.tis.dataplatform.dao.IClusterSnapshotDAO;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshot;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotCriteria;
import com.taobao.ibatis.extend.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ClusterSnapshotDAOImpl extends BasicDAO<ClusterSnapshot, ClusterSnapshotCriteria> implements IClusterSnapshotDAO {

    public ClusterSnapshotDAOImpl() {
        super();
    }

    @SuppressWarnings("all")
    public void createTodaySummary(Date today) {
        this.getSqlMapClientTemplate().insert("cluster_snapshot.ibatorgenerated_pre_day_all_request_count", today);
    }

    @SuppressWarnings("all")
    public void insertList(List<ClusterSnapshot> records) {
        this.getSqlMapClientTemplate().insert("cluster_snapshot.batchInsert", records);
    }

    public int countByExample(ClusterSnapshotCriteria example) {
        Integer count = (Integer) this.count("cluster_snapshot.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(ClusterSnapshotCriteria example) {
        Integer count = (Integer) this.countFromWriterDB("cluster_snapshot.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(ClusterSnapshotCriteria criteria) {
        return this.deleteRecords("cluster_snapshot.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer id) {
        ClusterSnapshot key = new ClusterSnapshot();
        key.setId(id);
        return this.deleteRecords("cluster_snapshot.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(ClusterSnapshot record) {
        Object newKey = this.insert("cluster_snapshot.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(ClusterSnapshot record) {
        Object newKey = this.insert("cluster_snapshot.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<ClusterSnapshot> selectByExample(ClusterSnapshotCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<ClusterSnapshot> selectByExample(ClusterSnapshotCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<ClusterSnapshot> list = this.list("cluster_snapshot.ibatorgenerated_selectByExample", example);
        return list;
    }

    public ClusterSnapshot selectByPrimaryKey(Integer id) {
        ClusterSnapshot key = new ClusterSnapshot();
        key.setId(id);
        ClusterSnapshot record = (ClusterSnapshot) this.load("cluster_snapshot.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(ClusterSnapshot record, ClusterSnapshotCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("cluster_snapshot.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(ClusterSnapshot record, ClusterSnapshotCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("cluster_snapshot.ibatorgenerated_updateByExample", parms);
    }

    public ClusterSnapshot loadFromWriteDB(Integer id) {
        ClusterSnapshot key = new ClusterSnapshot();
        key.setId(id);
        ClusterSnapshot record = (ClusterSnapshot) this.loadFromWriterDB("cluster_snapshot.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends ClusterSnapshotCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, ClusterSnapshotCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
