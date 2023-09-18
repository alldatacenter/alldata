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

import com.qlangtech.tis.workflow.dao.IDatasourceTableDAO;
import com.qlangtech.tis.workflow.pojo.DatasourceTable;
import com.qlangtech.tis.workflow.pojo.DatasourceTableCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DatasourceTableDAOImpl extends BasicDAO<DatasourceTable, DatasourceTableCriteria> implements IDatasourceTableDAO {

    public DatasourceTableDAOImpl() {
        super();
    }

    public final String getEntityName() {
        return "datasource_table";
    }

    public int countByExample(DatasourceTableCriteria example) {
        Integer count = (Integer) this.count("datasource_table.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(DatasourceTableCriteria example) {
        Integer count = (Integer) this.countFromWriterDB("datasource_table.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(DatasourceTableCriteria criteria) {
        return this.deleteRecords("datasource_table.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer id) {
        DatasourceTable key = new DatasourceTable();
        key.setId(id);
        return this.deleteRecords("datasource_table.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(DatasourceTable record) {
        Object newKey = this.insert("datasource_table.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(DatasourceTable record) {
        Object newKey = this.insert("datasource_table.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<DatasourceTable> selectByExample(DatasourceTableCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<DatasourceTable> selectByExample(DatasourceTableCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<DatasourceTable> list = this.list("datasource_table.ibatorgenerated_selectByExample", example);
        return list;
    }

    public List<DatasourceTable> minSelectByExample(DatasourceTableCriteria criteria) {
        return this.minSelectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<DatasourceTable> minSelectByExample(DatasourceTableCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<DatasourceTable> list = this.list("datasource_table.ibatorgenerated_minSelectByExample", example);
        return list;
    }

    public DatasourceTable selectByPrimaryKey(Integer id) {
        DatasourceTable key = new DatasourceTable();
        key.setId(id);
        DatasourceTable record = (DatasourceTable) this.load("datasource_table.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(DatasourceTable record, DatasourceTableCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("datasource_table.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(DatasourceTable record, DatasourceTableCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("datasource_table.ibatorgenerated_updateByExample", parms);
    }

    public DatasourceTable loadFromWriteDB(Integer id) {
        DatasourceTable key = new DatasourceTable();
        key.setId(id);
        DatasourceTable record = (DatasourceTable) this.loadFromWriterDB("datasource_table.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends DatasourceTableCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, DatasourceTableCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
