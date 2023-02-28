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

import com.qlangtech.tis.dataplatform.pojo.DsDatasource;
import com.qlangtech.tis.dataplatform.pojo.DsDatasourceCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IDsDatasourceDAO {

    int countByExample(DsDatasourceCriteria example);

    int countFromWriteDB(DsDatasourceCriteria example);

    int deleteByExample(DsDatasourceCriteria criteria);

    int deleteByPrimaryKey(Integer dsId);

    Integer insert(DsDatasource record);

    Integer insertSelective(DsDatasource record);

    List<DsDatasource> selectByExample(DsDatasourceCriteria criteria);

    List<DsDatasource> selectByExample(DsDatasourceCriteria example, int page, int pageSize);

    DsDatasource selectByPrimaryKey(Integer dsId);

    int updateByExampleSelective(DsDatasource record, DsDatasourceCriteria example);

    int updateByExample(DsDatasource record, DsDatasourceCriteria example);

    DsDatasource loadFromWriteDB(Integer dsId);
}
