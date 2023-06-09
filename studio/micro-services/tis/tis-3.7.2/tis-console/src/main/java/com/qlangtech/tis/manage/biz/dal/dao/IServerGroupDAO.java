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
package com.qlangtech.tis.manage.biz.dal.dao;

import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroupCriteria;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IServerGroupDAO {

  int countByExample(ServerGroupCriteria example);

  int countFromWriteDB(ServerGroupCriteria example);

  int deleteByExample(ServerGroupCriteria criteria);

  int deleteByPrimaryKey(Integer gid);

  int insert(ServerGroup record);

  Integer insertSelective(ServerGroup record);

  List<ServerGroup> selectByExample(ServerGroupCriteria criteria);

  List<ServerGroup> selectByExample(ServerGroupCriteria example, int page, int pageSize);

  ServerGroup selectByPrimaryKey(Integer gid);

  int updateByExampleSelective(ServerGroup record, ServerGroupCriteria example);

  int updateByExample(ServerGroup record, ServerGroupCriteria example);

  ServerGroup loadFromWriteDB(Integer gid);

  // 百岁添加

  List<Long> getServergroupWithoutAppReference();

  ServerGroup load(String appName, Short groupIndex, Short runtime);
  // end
}
