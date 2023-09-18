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

import com.qlangtech.tis.workflow.pojo.DatasourceTable;
import com.qlangtech.tis.workflow.pojo.DatasourceTableCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDatasourceTableDAO {

    int countByExample(DatasourceTableCriteria example);

    int countFromWriteDB(DatasourceTableCriteria example);

    int deleteByExample(DatasourceTableCriteria criteria);

    int deleteByPrimaryKey(Integer id);

    Integer insert(DatasourceTable record);

    Integer insertSelective(DatasourceTable record);

    List<DatasourceTable> selectByExample(DatasourceTableCriteria criteria);

    List<DatasourceTable> selectByExample(DatasourceTableCriteria example, int page, int pageSize);

    List<DatasourceTable> minSelectByExample(DatasourceTableCriteria criteria);

    List<DatasourceTable> minSelectByExample(DatasourceTableCriteria example, int page, int pageSize);

    DatasourceTable selectByPrimaryKey(Integer id);

    int updateByExampleSelective(DatasourceTable record, DatasourceTableCriteria example);

    int updateByExample(DatasourceTable record, DatasourceTableCriteria example);

    DatasourceTable loadFromWriteDB(Integer id);
}
