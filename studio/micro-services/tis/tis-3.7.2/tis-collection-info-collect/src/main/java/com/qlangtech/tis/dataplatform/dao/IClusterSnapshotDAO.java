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

import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshot;
import com.qlangtech.tis.dataplatform.pojo.ClusterSnapshotCriteria;
import java.util.Date;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface IClusterSnapshotDAO {

    /**
     * 创建当天的历史统计
     * @param today
     */
    public void createTodaySummary(Date today);

    @SuppressWarnings("all")
    public void insertList(List<ClusterSnapshot> records);

    int countByExample(ClusterSnapshotCriteria example);

    int countFromWriteDB(ClusterSnapshotCriteria example);

    int deleteByExample(ClusterSnapshotCriteria criteria);

    int deleteByPrimaryKey(Integer id);

    Integer insert(ClusterSnapshot record);

    Integer insertSelective(ClusterSnapshot record);

    List<ClusterSnapshot> selectByExample(ClusterSnapshotCriteria criteria);

    List<ClusterSnapshot> selectByExample(ClusterSnapshotCriteria example, int page, int pageSize);

    ClusterSnapshot selectByPrimaryKey(Integer id);

    int updateByExampleSelective(ClusterSnapshot record, ClusterSnapshotCriteria example);

    int updateByExample(ClusterSnapshot record, ClusterSnapshotCriteria example);

    ClusterSnapshot loadFromWriteDB(Integer id);
}
