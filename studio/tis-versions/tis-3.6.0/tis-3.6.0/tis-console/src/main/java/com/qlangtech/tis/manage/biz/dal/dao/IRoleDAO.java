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

import com.qlangtech.tis.manage.biz.dal.pojo.Role;
import com.qlangtech.tis.manage.biz.dal.pojo.RoleCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IRoleDAO {

    int countByExample(RoleCriteria example);

    int countFromWriteDB(RoleCriteria example);

    int deleteByExample(RoleCriteria criteria);

    int deleteByPrimaryKey(Integer rId);

    Integer insert(Role record);

    Integer insertSelective(Role record);

    List<Role> selectByExample(RoleCriteria criteria);

    List<Role> selectByExample(RoleCriteria example, int page, int pageSize);

    Role selectByPrimaryKey(Integer rId);

    int updateByExampleSelective(Role record, RoleCriteria example);

    int updateByExample(Role record, RoleCriteria example);

    Role loadFromWriteDB(Integer rId);
}
