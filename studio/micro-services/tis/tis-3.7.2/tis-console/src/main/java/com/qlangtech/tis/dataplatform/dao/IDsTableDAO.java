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
package com.qlangtech.tis.dataplatform.dao;

import com.qlangtech.tis.dataplatform.pojo.DsTable;
import com.qlangtech.tis.dataplatform.pojo.DsTableCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDsTableDAO {

    int countByExample(DsTableCriteria example);

    int countFromWriteDB(DsTableCriteria example);

    int deleteByExample(DsTableCriteria criteria);

    int deleteByPrimaryKey(Long tabId);

    Long insert(DsTable record);

    Long insertSelective(DsTable record);

    List<DsTable> selectByExampleWithBLOBs(DsTableCriteria example);

    List<DsTable> selectByExampleWithoutBLOBs(DsTableCriteria criteria);

    List<DsTable> selectByExampleWithoutBLOBs(DsTableCriteria example, int page, int pageSize);

    DsTable selectByPrimaryKey(Long tabId);

    int updateByExampleSelective(DsTable record, DsTableCriteria example);

    int updateByExampleWithBLOBs(DsTable record, DsTableCriteria example);

    int updateByExampleWithoutBLOBs(DsTable record, DsTableCriteria example);

    DsTable loadFromWriteDB(Long tabId);
}
