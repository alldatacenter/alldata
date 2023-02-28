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
import com.qlangtech.tis.manage.biz.dal.dao.IRoleDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Role;
import com.qlangtech.tis.manage.biz.dal.pojo.RoleCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class RoleDAOImpl extends BasicDAO<Role, RoleCriteria> implements IRoleDAO {

    @Override
    public String getEntityName() {
        return "role";
    }

    public RoleDAOImpl() {
        super();
    }

    public int countByExample(RoleCriteria example) {
        Integer count = this.count("role.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(RoleCriteria example) {
        Integer count = this.countFromWriterDB("role.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(RoleCriteria criteria) {
        return this.deleteRecords("role.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer rId) {
        Role key = new Role();
        key.setrId(rId);
        return this.deleteRecords("role.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(Role record) {
        Object newKey = this.insert("role.ibatorgenerated_insert", record);
        return (Integer) newKey;
    }

    public Integer insertSelective(Role record) {
        Object newKey = this.insert("role.ibatorgenerated_insertSelective", record);
        return (Integer) newKey;
    }

    public List<Role> selectByExample(RoleCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<Role> selectByExample(RoleCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<Role> list = this.list("role.ibatorgenerated_selectByExample", example);
        return list;
    }

    public Role selectByPrimaryKey(Integer rId) {
        Role key = new Role();
        key.setrId(rId);
        Role record = this.load("role.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(Role record, RoleCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("role.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(Role record, RoleCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("role.ibatorgenerated_updateByExample", parms);
    }

    public Role loadFromWriteDB(Integer rId) {
        Role key = new Role();
        key.setrId(rId);
        Role record = this.loadFromWriterDB("role.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends RoleCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, RoleCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
