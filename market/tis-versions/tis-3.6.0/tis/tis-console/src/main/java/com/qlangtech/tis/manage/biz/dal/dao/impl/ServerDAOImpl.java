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
import com.qlangtech.tis.manage.biz.dal.dao.IServerDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.Server;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ServerDAOImpl extends BasicDAO<Server, ServerCriteria> implements IServerDAO {

    @Override
    public String getEntityName() {
        return "server";
    }

    public ServerDAOImpl() {
        super();
    }

    public int countByExample(ServerCriteria example) {
        Integer count = this.count("server.ibatorgenerated_countByExample", example);
        return count;
    }

    public int countFromWriteDB(ServerCriteria example) {
        Integer count = this.countFromWriterDB("server.ibatorgenerated_countByExample", example);
        return count;
    }

    public int deleteByExample(ServerCriteria criteria) {
        return this.deleteRecords("server.ibatorgenerated_deleteByExample", criteria);
    }

    public int deleteByPrimaryKey(Integer sid) {
        Server key = new Server();
        key.setSid(sid);
        return this.deleteRecords("server.ibatorgenerated_deleteByPrimaryKey", key);
    }

    public Integer insert(Server record) {
        return (Integer) this.insert("server.ibatorgenerated_insert", record);
    }

    public Integer insertSelective(Server record) {
        return (Integer) this.insert("server.ibatorgenerated_insertSelective", record);
    }

    public List<Server> selectByExample(ServerCriteria criteria) {
        return this.selectByExample(criteria, 1, 100);
    }

    @SuppressWarnings("unchecked")
    public List<Server> selectByExample(ServerCriteria example, int page, int pageSize) {
        example.setPage(page);
        example.setPageSize(pageSize);
        List<Server> list = this.list("server.ibatorgenerated_selectByExample", example);
        return list;
    }

    public Server selectByPrimaryKey(Integer sid) {
        Server key = new Server();
        key.setSid(sid);
        Server record = this.load("server.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    public int updateByExampleSelective(Server record, ServerCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("server.ibatorgenerated_updateByExampleSelective", parms);
    }

    public int updateByExample(Server record, ServerCriteria example) {
        UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
        return this.updateRecords("server.ibatorgenerated_updateByExample", parms);
    }

    public Server loadFromWriteDB(Integer sid) {
        Server key = new Server();
        key.setSid(sid);
        Server record = this.loadFromWriterDB("server.ibatorgenerated_selectByPrimaryKey", key);
        return record;
    }

    private static class UpdateByExampleParms extends ServerCriteria {

        private Object record;

        public UpdateByExampleParms(Object record, ServerCriteria example) {
            super(example);
            this.record = record;
        }

        public Object getRecord() {
            return record;
        }
    }
}
