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

import com.qlangtech.tis.dataplatform.dao.IClusterSnapshotPreDayDAO;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotPreDay;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotPreDayCriteria;
import com.taobao.ibatis.extend.BasicDAO;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ClusterSnapshotPreDayDAOImpl extends BasicDAO<ClusterSnapshotPreDay, ClusterSnapshotPreDayCriteria> implements IClusterSnapshotPreDayDAO {

    public ClusterSnapshotPreDayDAOImpl() {
        super();
    }

    public int countByExample(ClusterSnapshotPreDayCriteria example) {
        Integer count = (Integer) this.count("cluster_snapshot_pre_day.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(ClusterSnapshotPreDayCriteria example) {
        Integer count = (Integer) this.countFromWriterDB("cluster_snapshot_pre_day.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(ClusterSnapshotPreDayCriteria criteria) {
        return this.deleteRecords("cluster_snapshot_pre_day.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer id) {
        ClusterSnapshotPreDay key = new ClusterSnapshotPreDay();
        key.setId(id);
        return this.deleteRecords("cluster_snapshot_pre_day.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(ClusterSnapshotPreDay record) {
        Object newKey = this.insert("cluster_snapshot_pre_day.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(ClusterSnapshotPreDay record) {
        Object newKey = this.insert("cluster_snapshot_pre_day.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<ClusterSnapshotPreDay> selectByExample(ClusterSnapshotPreDayCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<ClusterSnapshotPreDay> selectByExample(ClusterSnapshotPreDayCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<ClusterSnapshotPreDay> list = this.list("cluster_snapshot_pre_day.ibatorgenerated_selectByExample", example);
        return list;
    }

    public ClusterSnapshotPreDay selectByPrimaryKey(Integer id) {
        ClusterSnapshotPreDay key = new ClusterSnapshotPreDay();
        key.setId(id);
        ClusterSnapshotPreDay record = (ClusterSnapshotPreDay) this.load("cluster_snapshot_pre_day.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(ClusterSnapshotPreDay record, ClusterSnapshotPreDayCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("cluster_snapshot_pre_day.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(ClusterSnapshotPreDay record, ClusterSnapshotPreDayCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("cluster_snapshot_pre_day.ibatorgenerated_updateByExample", parms);
    }

    public ClusterSnapshotPreDay loadFromWriteDB(Integer id) {
        ClusterSnapshotPreDay key = new ClusterSnapshotPreDay();
        key.setId(id);
        ClusterSnapshotPreDay record = (ClusterSnapshotPreDay) this.loadFromWriterDB("cluster_snapshot_pre_day.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends ClusterSnapshotPreDayCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, ClusterSnapshotPreDayCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
