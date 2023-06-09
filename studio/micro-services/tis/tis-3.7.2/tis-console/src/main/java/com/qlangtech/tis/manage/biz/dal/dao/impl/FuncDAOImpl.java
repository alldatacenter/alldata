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
import com.qlangtech.tis.manage.biz.dal.dao.IFuncDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Func;
import com.qlangtech.tis.manage.biz.dal.pojo.FuncCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class FuncDAOImpl extends BasicDAO<Func, FuncCriteria> implements IFuncDAO {

    @Override
    public String getEntityName() {
        return "func";
    }

    public FuncDAOImpl() {
        super();
    }

    public int countByExample(FuncCriteria example) {
        Integer count = this.count("func.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(FuncCriteria example) {
        Integer count = this.countFromWriterDB("func.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(FuncCriteria criteria) {
        return this.deleteRecords("func.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer funId) {
        Func key = new Func();
        key.setFunId(funId);
        return this.deleteRecords("func.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(Func record) {
        Object newKey = this.insert("func.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(Func record) {
        Object newKey = this.insert("func.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<Func> selectByExample(FuncCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<Func> selectByExample(FuncCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<Func> list = this.list("func.ibatorgenerated_selectByExample", example);
        return list;
    }

    public Func selectByPrimaryKey(Integer funId) {
        Func key = new Func();
        key.setFunId(funId);
        Func record = this.load("func.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(Func record, FuncCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("func.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(Func record, FuncCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("func.ibatorgenerated_updateByExample", parms);
    }

    public Func loadFromWriteDB(Integer funId) {
        Func key = new Func();
        key.setFunId(funId);
        Func record = this.loadFromWriterDB("func.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends FuncCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, FuncCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
