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

import com.qlangtech.tis.manage.biz.dal.dao.IServerGroupDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroupCriteria;
import com.qlangtech.tis.manage.common.BasicDAO;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ServerGroupDAOImpl extends BasicDAO<ServerGroup, ServerGroupCriteria> implements IServerGroupDAO {

  @Override
  public String getEntityName() {
    return "server_group";
  }

  public ServerGroupDAOImpl() {
    super();
  }

  public int countByExample(ServerGroupCriteria example) {
    Integer count = this.count("server_group.ibatorgenerated_countByExample", example);
    return count;
  }

  public List<Long> getServergroupWithoutAppReference() {
    List<Long> list = this.getSqlMapClientTemplate()
      .queryForList("server_group.ibatorgenerated_get_servergroup_without_app_reference");
    return list;
  }


  public int countFromWriteDB(ServerGroupCriteria example) {
    Integer count = this.countFromWriterDB("server_group.ibatorgenerated_countByExample", example);
    return count;
  }

  public int deleteByExample(ServerGroupCriteria criteria) {
    return this.deleteRecords("server_group.ibatorgenerated_deleteByExample", criteria);
  }

  public int deleteByPrimaryKey(Integer gid) {
    ServerGroup key = new ServerGroup();
    key.setGid(gid);
    return this.deleteRecords("server_group.ibatorgenerated_deleteByPrimaryKey", key);
  }

  public int insert(ServerGroup record) {
    return (Integer) this.insert("server_group.ibatorgenerated_insert", record);
  }

  public Integer insertSelective(ServerGroup record) {
    return (Integer) this.insert("server_group.ibatorgenerated_insertSelective", record);
  }

  public List<ServerGroup> selectByExample(ServerGroupCriteria criteria) {
    return this.selectByExample(criteria, 1, 100);
  }

  @SuppressWarnings("unchecked")
  public List<ServerGroup> selectByExample(ServerGroupCriteria example, int page, int pageSize) {
    example.setPage(page);
    example.setPageSize(pageSize);
    List<ServerGroup> list = this.list("server_group.ibatorgenerated_selectByExample", example);
    return list;
  }

  public ServerGroup selectByPrimaryKey(Integer gid) {
    ServerGroup key = new ServerGroup();
    key.setGid(gid);
    ServerGroup record = this.load("server_group.ibatorgenerated_selectByPrimaryKey", key);
    return record;
  }

  public int updateByExampleSelective(ServerGroup record, ServerGroupCriteria example) {
    UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
    return this.updateRecords("server_group.ibatorgenerated_updateByExampleSelective", parms);
  }

  public int updateByExample(ServerGroup record, ServerGroupCriteria example) {
    UpdateByExampleParms parms = new UpdateByExampleParms(record, example);
    return this.updateRecords("server_group.ibatorgenerated_updateByExample", parms);
  }

  public ServerGroup loadFromWriteDB(Integer gid) {
    ServerGroup key = new ServerGroup();
    key.setGid(gid);
    ServerGroup record = this.loadFromWriterDB("server_group.ibatorgenerated_selectByPrimaryKey", key);
    return record;
  }

  /**
   * 百岁添加20120502
   */
  public ServerGroup load(String appName, Short groupIndex, Short runtime) {
    ServerGroup key = new ServerGroup();
    key.setAppName(appName);
    key.setGroupIndex(groupIndex);
    key.setRuntEnvironment(runtime);
    return this.loadFromWriterDB("server_group.ibatorgenerated_getBy_appName_groupIndex_runtime", key);
  }

  private static class UpdateByExampleParms extends ServerGroupCriteria {

    private Object record;

    public UpdateByExampleParms(Object record, ServerGroupCriteria example) {
      super(example);
      this.record = record;
    }

    public Object getRecord() {
      return record;
    }
  }
}
