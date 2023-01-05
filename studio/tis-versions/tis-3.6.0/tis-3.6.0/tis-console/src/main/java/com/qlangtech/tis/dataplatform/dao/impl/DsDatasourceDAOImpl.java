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

import java.util.List;
import com.qlangtech.tis.dataplatform.dao.IDsDatasourceDAO;
import com.qlangtech.tis.dataplatform.pojo.DsDatasource;
import com.qlangtech.tis.dataplatform.pojo.DsDatasourceCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DsDatasourceDAOImpl extends BasicDAO<DsDatasource, DsDatasourceCriteria> implements IDsDatasourceDAO {

    public DsDatasourceDAOImpl() {
        super();
    }

    public int countByExample(DsDatasourceCriteria example) {
        Integer count = (Integer) this.count("ds_datasource.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(DsDatasourceCriteria example) {
        Integer count = (Integer) this.countFromWriterDB("ds_datasource.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(DsDatasourceCriteria criteria) {
        return this.deleteRecords("ds_datasource.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer dsId) {
        DsDatasource key = new DsDatasource();
        key.setDsId(dsId);
        return this.deleteRecords("ds_datasource.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(DsDatasource record) {
        Object newKey = this.insert("ds_datasource.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(DsDatasource record) {
        Object newKey = this.insert("ds_datasource.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<DsDatasource> selectByExample(DsDatasourceCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<DsDatasource> selectByExample(DsDatasourceCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<DsDatasource> list = this.list("ds_datasource.ibatorgenerated_selectByExample", example);
        return list;
    }

    public DsDatasource selectByPrimaryKey(Integer dsId) {
        DsDatasource key = new DsDatasource();
        key.setDsId(dsId);
        DsDatasource record = (DsDatasource) this.load("ds_datasource.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(DsDatasource record, DsDatasourceCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("ds_datasource.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(DsDatasource record, DsDatasourceCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("ds_datasource.ibatorgenerated_updateByExample", parms);
    }

    public DsDatasource loadFromWriteDB(Integer dsId) {
        DsDatasource key = new DsDatasource();
        key.setDsId(dsId);
        DsDatasource record = (DsDatasource) this.loadFromWriterDB("ds_datasource.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends DsDatasourceCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, DsDatasourceCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }

    @Override
    public String getEntityName() {
        return "DsDatasource";
    }
}
