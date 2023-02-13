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
import org.apache.inlong.manager.dao.entity.StreamTransformFieldEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamTransformFieldEntityMapper {

    int insert(StreamTransformFieldEntity record);

    int insertSelective(StreamTransformFieldEntity record);

    /**
     * Select undeleted transform field by transform id.
     */
    List<StreamTransformFieldEntity> selectByTransformId(@Param("transformId") Integer transformId);

    /**
     * Select undeleted transform field by transform ids.
     */
    List<StreamTransformFieldEntity> selectByTransformIds(@Param("transformIds") List<Integer> transformIds);

    StreamTransformFieldEntity selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(StreamTransformFieldEntity record);

    int updateByPrimaryKey(StreamTransformFieldEntity record);

    /**
     * Insert all field list
     */
    void insertAll(@Param("list") List<StreamTransformFieldEntity> fieldList);

    int deleteById(Integer id);

    /**
     * Delete all field list by transformId
     */
    int deleteAll(@Param("transformId") Integer transformId);

    /**
     * Physically delete all stream transform fields based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
