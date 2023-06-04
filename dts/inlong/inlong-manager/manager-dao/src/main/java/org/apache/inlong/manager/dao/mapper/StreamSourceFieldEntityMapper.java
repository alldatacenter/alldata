/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.inlong.manager.dao.entity.StreamSourceFieldEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamSourceFieldEntityMapper {

    int insert(StreamSourceFieldEntity record);

    int insertSelective(StreamSourceFieldEntity record);

    /**
     * Select undeleted source field by source id.
     *
     * @param sourceId source id
     * @return stream source field list
     */
    List<StreamSourceFieldEntity> selectBySourceId(@Param("sourceId") Integer sourceId);

    int updateByPrimaryKeySelective(StreamSourceFieldEntity record);

    int updateByPrimaryKey(StreamSourceFieldEntity record);

    /**
     * Logically delete all stream source fields based on inlong group id and inlong stream id
     *
     * @return rows deleted
     */
    int updateByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Insert all field list
     *
     * @param fieldList stream source field list
     */
    void insertAll(@Param("list") List<StreamSourceFieldEntity> fieldList);

    /**
     * Delete all field list by sourceId
     *
     * @param sourceId source id
     * @return number of records deleted
     */
    int deleteAll(@Param("sourceId") Integer sourceId);

    /**
     * Physically delete all stream source fields based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
