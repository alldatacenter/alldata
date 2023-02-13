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

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.inlong.manager.dao.entity.InlongGroupExtEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InlongGroupExtEntityMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(InlongGroupExtEntity record);

    int insertSelective(InlongGroupExtEntity record);

    InlongGroupExtEntity selectByPrimaryKey(Integer id);

    List<InlongGroupExtEntity> selectByGroupId(String groupId);

    int updateByPrimaryKey(InlongGroupExtEntity record);

    InlongGroupExtEntity selectByUniqueKey(
            @Param("inlongGroupId") String inlongGroupId,
            @Param("keyName") String keyName);

    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    Cursor<InlongGroupExtEntity> selectByKeyName(@Param("keyName") String keyName);

    /**
     * Insert data in batches
     *
     * @param extEntityList need to insert data
     */
    int insertAll(@Param("extList") List<InlongGroupExtEntity> extEntityList);

    /**
     * Insert data in batches, update if it exists, create new if it does not exist
     */
    int insertOnDuplicateKeyUpdate(@Param("extList") List<InlongGroupExtEntity> extEntityList);

    /**
     * Physically delete all extension fields based on the inlong group id
     *
     * @return rows deleted
     */
    int deleteAllByGroupId(String groupId);

    /**
     * Logically delete all extended fields based on inlong group id
     *
     * @return rows updated
     */
    int logicDeleteAllByGroupId(String groupId);

    /**
     * Physically delete all extended fields based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
