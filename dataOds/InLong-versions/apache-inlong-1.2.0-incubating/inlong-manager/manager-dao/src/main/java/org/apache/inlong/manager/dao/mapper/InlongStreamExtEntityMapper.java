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
import org.apache.inlong.manager.dao.entity.InlongStreamExtEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InlongStreamExtEntityMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(InlongStreamExtEntity record);

    int insertSelective(InlongStreamExtEntity record);

    InlongStreamExtEntity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(InlongStreamExtEntity record);

    int updateByPrimaryKeyWithBLOBs(InlongStreamExtEntity record);

    int updateByPrimaryKey(InlongStreamExtEntity record);

    List<InlongStreamExtEntity> selectByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Insert data in batches
     *
     * @param extList need to insert data
     */
    int insertAll(@Param("extList") List<InlongStreamExtEntity> extList);

    /**
     * Insert data in batches, update if it exists, create new if it does not exist
     *
     * @param extList need to insertOnDuplicateUpdate data
     */
    int insertOnDuplicateKeyUpdate(@Param("extList") List<InlongStreamExtEntity> extList);

    /**
     * Physically delete all extension fields based on group id and stream id
     *
     * @return rows deleted
     */
    int deleteAllByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Logically delete all extended fields based on group id and stream id
     *
     * @return rows updated
     */
    int logicDeleteAllByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);
}