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
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicRequest;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceGroupInfo;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface InlongGroupEntityMapper {

    int insert(InlongGroupEntity record);

    InlongGroupEntity selectByPrimaryKey(Integer id);

    List<Map<String, Object>> countGroupByUser(@Param(value = "username") String username);

    InlongGroupEntity selectByGroupId(String groupId);

    InlongGroupEntity selectByGroupIdForUpdate(String groupId);

    List<InlongGroupEntity> selectByCondition(InlongGroupPageRequest request);

    List<InlongGroupBriefInfo> selectBriefList(InlongGroupPageRequest request);

    List<InlongGroupEntity> selectByClusterTag(@Param(value = "inlongClusterTag") String inlongClusterTag);

    List<InlongGroupEntity> selectByTopicRequest(InlongGroupTopicRequest request);

    /**
     * Select all group info for sort sdk.
     *
     * @return All inlong group info.
     */
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    Cursor<SortSourceGroupInfo> selectAllGroups();

    /**
     * Select all groups which are logical deleted before the specified last modify time
     * <p/>
     * Note, ensure that all the group ids found have been deleted,
     * and the group ids not deleted (is_deleted=0) should not be returned.
     *
     * @param timeBefore the latest modify time before which to select
     * @param limit max item count
     * @return all matched group ids
     */
    List<String> selectDeletedGroupIds(@Param("timeBefore") Date timeBefore, @Param("limit") Integer limit);

    int updateByPrimaryKey(InlongGroupEntity record);

    int updateByIdentifierSelective(InlongGroupEntity record);

    int updateStatus(@Param("groupId") String groupId, @Param("status") Integer status,
            @Param("modifier") String modifier);

    int deleteByPrimaryKey(Integer id);

    /**
     * Physically delete all inlong groups based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
