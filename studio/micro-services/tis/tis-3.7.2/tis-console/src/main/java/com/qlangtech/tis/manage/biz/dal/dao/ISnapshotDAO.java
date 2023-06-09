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

import com.qlangtech.tis.manage.biz.dal.pojo.Snapshot;
import com.qlangtech.tis.manage.biz.dal.pojo.SnapshotCriteria;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface ISnapshotDAO {

    int countByExample(SnapshotCriteria example);

    int countFromWriteDB(SnapshotCriteria example);

    int deleteByExample(SnapshotCriteria criteria);

    List<Snapshot> findPassTestSnapshot(SnapshotCriteria example);

    int deleteByPrimaryKey(Integer snId);

    Integer insert(Snapshot record);

    Integer insertSelective(Snapshot record);

    List<Snapshot> selectByExample(SnapshotCriteria criteria);

    List<Snapshot> selectByExample(SnapshotCriteria example, int page, int pageSize);

    Integer getMaxSnapshotId(SnapshotCriteria criteria);

    Snapshot selectByPrimaryKey(Integer snId);

    int updateByExampleSelective(Snapshot record, SnapshotCriteria example);

    int updateByExample(Snapshot record, SnapshotCriteria example);

    Snapshot loadFromWriteDB(Integer snId);
}
