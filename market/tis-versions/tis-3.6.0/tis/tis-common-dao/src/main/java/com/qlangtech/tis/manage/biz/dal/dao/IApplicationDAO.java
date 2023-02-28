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

import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IApplicationDAO {

    int countByExample(ApplicationCriteria example);

    int countFromWriteDB(ApplicationCriteria example);

    int deleteByExample(ApplicationCriteria criteria);

    int deleteByPrimaryKey(Integer appId);

    Integer insert(Application record);

    Integer insertSelective(Application record);

    List<Application> selectByExample(ApplicationCriteria criteria);

    List<Application> selectByExample(ApplicationCriteria example, int page, int pageSize);

    Application selectByPrimaryKey(Integer appId);

    Application selectByName(String name);

    int updateByExampleSelective(Application record, ApplicationCriteria example);

    int updateLastProcessTime(String appname);

    int updateByExample(Application record, ApplicationCriteria example);

    Application loadFromWriteDB(Integer appId);
}
