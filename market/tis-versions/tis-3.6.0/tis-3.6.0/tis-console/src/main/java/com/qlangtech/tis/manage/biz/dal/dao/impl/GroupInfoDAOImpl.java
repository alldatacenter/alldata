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
import com.qlangtech.tis.manage.biz.dal.dao.IGroupInfoDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.GroupInfo;
import com.qlangtech.tis.manage.biz.dal.pojo.GroupInfoCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class GroupInfoDAOImpl extends BasicDAO<GroupInfo, GroupInfoCriteria> implements IGroupInfoDAO {

    @Override
    public String getEntityName() {
        return "group_info";
    }

    public GroupInfoDAOImpl() {
        super();
    }

    public int countByExample(GroupInfoCriteria example) {
        Integer count = this.count("group_info.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(GroupInfoCriteria example) {
        Integer count = this.countFromWriterDB("group_info.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(GroupInfoCriteria criteria) {
        return this.deleteRecords("group_info.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer gid) {
        GroupInfo key = new GroupInfo();
        key.setGid(gid);
        return this.deleteRecords("group_info.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public void insert(GroupInfo record) {
        this.insert("group_info.ibatorgenerated_insert", record);
    }

    public void insertSelective(GroupInfo record) {
        this.insert("group_info.ibatorgenerated_insertSelective", record);
    }

    public List<GroupInfo> selectByExample(GroupInfoCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<GroupInfo> selectByExample(GroupInfoCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<GroupInfo> list = this.list("group_info.ibatorgenerated_selectByExample", example);
        return list;
    }

    public GroupInfo selectByPrimaryKey(Integer gid) {
        GroupInfo key = new GroupInfo();
        key.setGid(gid);
        GroupInfo record = this.load("group_info.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(GroupInfo record, GroupInfoCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("group_info.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(GroupInfo record, GroupInfoCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("group_info.ibatorgenerated_updateByExample", parms);
    }

    public GroupInfo loadFromWriteDB(Integer gid) {
        GroupInfo key = new GroupInfo();
        key.setGid(gid);
        GroupInfo record = this.loadFromWriterDB("group_info.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends GroupInfoCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, GroupInfoCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
