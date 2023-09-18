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

import java.util.List;
import com.qlangtech.tis.manage.biz.dal.pojo.Server;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerCriteria;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IServerDAO {

    int countByExample(ServerCriteria example);

    int countFromWriteDB(ServerCriteria example);

    int deleteByExample(ServerCriteria criteria);

    int deleteByPrimaryKey(Integer sid);

    Integer insert(Server record);

    Integer insertSelective(Server record);

    List<Server> selectByExample(ServerCriteria criteria);

    List<Server> selectByExample(ServerCriteria example, int page, int pageSize);

    Server selectByPrimaryKey(Integer sid);

    int updateByExampleSelective(Server record, ServerCriteria example);

    int updateByExample(Server record, ServerCriteria example);
}
