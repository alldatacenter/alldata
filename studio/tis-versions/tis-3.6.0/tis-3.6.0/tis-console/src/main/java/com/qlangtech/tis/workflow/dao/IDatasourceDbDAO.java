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
package com.qlangtech.tis.workflow.dao;

import com.qlangtech.tis.workflow.pojo.DatasourceDb;
import com.qlangtech.tis.workflow.pojo.DatasourceDbCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDatasourceDbDAO {

    int countByExample(DatasourceDbCriteria example);

    int countFromWriteDB(DatasourceDbCriteria example);

    int deleteByExample(DatasourceDbCriteria criteria);

    int deleteByPrimaryKey(Integer id);

    Integer insert(DatasourceDb record);

    Integer insertSelective(DatasourceDb record);

    List<DatasourceDb> selectByExample(DatasourceDbCriteria criteria);

    List<DatasourceDb> selectByExample(DatasourceDbCriteria example, int page, int pageSize);

    List<DatasourceDb> minSelectByExample(DatasourceDbCriteria criteria);

    List<DatasourceDb> minSelectByExample(DatasourceDbCriteria example, int page, int pageSize);

    DatasourceDb selectByPrimaryKey(Integer id);

    int updateByExampleSelective(DatasourceDb record, DatasourceDbCriteria example);

    int updateByExample(DatasourceDb record, DatasourceDbCriteria example);

    DatasourceDb loadFromWriteDB(Integer id);
}
