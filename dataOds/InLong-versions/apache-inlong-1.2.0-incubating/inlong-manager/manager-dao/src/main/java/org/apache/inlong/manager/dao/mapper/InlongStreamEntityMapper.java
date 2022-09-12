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
import org.apache.inlong.manager.common.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InlongStreamEntityMapper {

    int insert(InlongStreamEntity record);

    int insertSelective(InlongStreamEntity record);

    InlongStreamEntity selectByIdentifier(@Param("groupId") String groupId, @Param("streamId") String streamId);

    Integer selectExistByIdentifier(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Query all inlong stream according to conditions (do not specify groupId, query all inlong streams)
     *
     * @param request query request
     * @return inlong stream list
     */
    List<InlongStreamEntity> selectByCondition(@Param("request") InlongStreamPageRequest request);

    List<InlongStreamBriefInfo> selectBriefList(@Param("groupId") String groupId);

    List<InlongStreamEntity> selectByGroupId(@Param("groupId") String groupId);

    int selectCountByGroupId(@Param("groupId") String groupId);

    int updateByPrimaryKey(InlongStreamEntity record);

    int updateByIdentifierSelective(InlongStreamEntity streamEntity);

    int updateStatusByIdentifier(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("status") Integer status, @Param("modifier") String modifier);

    /**
     * Logic delete dlq or rlq topic by bid
     */
    void logicDeleteDlqOrRlq(String groupId, String streamId, String operator);

    int deleteByPrimaryKey(Integer id);

    /**
     * Physically delete all inlong streams of the specified inlong group id
     *
     * @return rows deleted
     */
    int deleteAllByGroupId(@Param("groupId") String groupId);

}