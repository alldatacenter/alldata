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

import com.qlangtech.tis.manage.biz.dal.pojo.Func;
import com.qlangtech.tis.manage.biz.dal.pojo.FuncCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IFuncDAO {

    int countByExample(FuncCriteria example);

    int countFromWriteDB(FuncCriteria example);

    int deleteByExample(FuncCriteria criteria);

    int deleteByPrimaryKey(Integer funId);

    Integer insert(Func record);

    Integer insertSelective(Func record);

    List<Func> selectByExample(FuncCriteria criteria);

    List<Func> selectByExample(FuncCriteria example, int page, int pageSize);

    Func selectByPrimaryKey(Integer funId);

    int updateByExampleSelective(Func record, FuncCriteria example);

    int updateByExample(Func record, FuncCriteria example);

    Func loadFromWriteDB(Integer funId);
}
