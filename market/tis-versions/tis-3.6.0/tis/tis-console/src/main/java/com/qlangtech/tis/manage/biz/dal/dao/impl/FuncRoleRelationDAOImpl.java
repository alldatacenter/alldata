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
import com.qlangtech.tis.manage.biz.dal.dao.IFuncRoleRelationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.FuncRoleRelation;
import com.qlangtech.tis.manage.biz.dal.pojo.FuncRoleRelationCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class FuncRoleRelationDAOImpl extends BasicDAO<FuncRoleRelation, FuncRoleRelationCriteria> implements IFuncRoleRelationDAO {

    public FuncRoleRelationDAOImpl() {
        super();
    }

    @Override
    public String getEntityName() {
        return "funcRoleRelation";
    }

    public int countByExample(FuncRoleRelationCriteria example) {
        Integer count = this.count("func_role_relation.ibatorgenerated_countByExample", example);
        return count;
    }

    @SuppressWarnings("unchecked")
    public List<String> selectFuncListByUsrid(String usrid) {
        return (List<String>) this.getSqlMapClientTemplate().queryForList("func_role_relation.ibatorgenerated_select_func_key_ByUsrid", usrid);
    }

    public int countFromWriteDB(FuncRoleRelationCriteria example) {
        Integer count = this.countFromWriterDB("func_role_relation.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(FuncRoleRelationCriteria criteria) {
        return this.deleteRecords("func_role_relation.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer id) {
        FuncRoleRelation key = new FuncRoleRelation();
        key.setId(id);
        return this.deleteRecords("func_role_relation.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(FuncRoleRelation record) {
        Object newKey = this.insert("func_role_relation.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(FuncRoleRelation record) {
        Object newKey = this.insert("func_role_relation.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<FuncRoleRelation> selectByExample(FuncRoleRelationCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<FuncRoleRelation> selectByExample(FuncRoleRelationCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<FuncRoleRelation> list = this.list("func_role_relation.ibatorgenerated_selectByExample", example);
        return list;
    }

    public FuncRoleRelation selectByPrimaryKey(Integer id) {
        FuncRoleRelation key = new FuncRoleRelation();
        key.setId(id);
        FuncRoleRelation record = this.load("func_role_relation.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(FuncRoleRelation record, FuncRoleRelationCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("func_role_relation.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(FuncRoleRelation record, FuncRoleRelationCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("func_role_relation.ibatorgenerated_updateByExample", parms);
    }

    public FuncRoleRelation loadFromWriteDB(Integer id) {
        FuncRoleRelation key = new FuncRoleRelation();
        key.setId(id);
        FuncRoleRelation record = this.loadFromWriterDB("func_role_relation.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends FuncRoleRelationCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, FuncRoleRelationCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
