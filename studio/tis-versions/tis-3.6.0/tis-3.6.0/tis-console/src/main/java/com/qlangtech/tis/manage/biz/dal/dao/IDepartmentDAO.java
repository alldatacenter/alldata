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

import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDepartmentDAO {

    int countByExample(DepartmentCriteria example);

    int countFromWriteDB(DepartmentCriteria example);

    int deleteByExample(DepartmentCriteria criteria);

    int deleteByPrimaryKey(Integer dptId);

    Integer insert(Department record);

    Integer insertSelective(Department record);

    List<Department> selectByExample(DepartmentCriteria criteria);

    // baisui add 20130520
    List<Department> selectByInnerJoinWithExtraDptUsrRelation(String userid);

    List<Department> selectByExample(DepartmentCriteria example, int page, int pageSize);

    Department selectByPrimaryKey(Integer dptId);

    int updateByExampleSelective(Department record, DepartmentCriteria example);

    int updateByExample(Department record, DepartmentCriteria example);

    Department loadFromWriteDB(Integer dptId);
}
