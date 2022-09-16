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
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.pojo.source.SourcePageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamSourceEntityMapper {

    int insert(StreamSourceEntity record);

    StreamSourceEntity selectById(Integer id);

    StreamSourceEntity selectByIdForUpdate(Integer id);

    /**
     * Only used for agent collector, which will select all tasks related include deleted tasks.
     *
     * @param id stream source id
     * @return stream source info
     */
    StreamSourceEntity selectForAgentTask(Integer id);

    /**
     * Query un-deleted sources by the given agentIp.
     */
    List<StreamSourceEntity> selectByAgentIp(@Param("agentIp") String agentIp);

    /**
     * According to the inlong group id and inlong stream id, query the number of valid source
     */
    int selectCount(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Paging query source list based on conditions
     */
    List<StreamSourceEntity> selectByCondition(@Param("request") SourcePageRequest request);

    /**
     * Query valid source list by the given group id, stream id and source name.
     */
    List<StreamSourceEntity> selectByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("sourceName") String sourceName);

    /**
     * Query the tasks by the given status list.
     */
    List<StreamSourceEntity> selectByStatus(@Param("statusList") List<Integer> list, @Param("limit") int limit);

    /**
     * Query the tasks by the given status list and type List.
     */
    List<StreamSourceEntity> selectByStatusAndType(@Param("statusList") List<Integer> statusList,
            @Param("sourceTypeList") List<String> sourceTypeList, @Param("limit") int limit);

    /**
     * Query the tasks by the given status list and type List.
     */
    List<StreamSourceEntity> selectByAgentIpOrCluster(@Param("statusList") List<Integer> statusList,
            @Param("sourceTypeList") List<String> sourceTypeList, @Param("agentIp") String agentIp,
            @Param("clusterName") String clusterName);

    /**
     * Query the sources with status 20x by the given agent IP and agent UUID.
     *
     * @apiNote Sources with is_deleted > 0 should also be returned to agents to clear their local tasks.
     */
    List<StreamSourceEntity> selectByStatusAndIp(@Param("statusList") List<Integer> statusList,
            @Param("agentIp") String agentIp, @Param("uuid") String uuid);

    /**
     * Select all sources by groupIds
     */
    List<StreamSourceEntity> selectByGroupIds(@Param("groupIdList") List<String> groupIdList);

    /**
     * Select all sub sources by template id
     */
    List<StreamSourceEntity> selectByTemplateId(@Param("templateId") Integer templateId);

    /**
     * Get the distinct source type from the given groupId and streamId
     */
    List<String> selectSourceType(@Param("groupId") String groupId, @Param("streamId") String streamId);

    int updateByPrimaryKeySelective(StreamSourceEntity record);

    int updateByPrimaryKey(StreamSourceEntity record);

    /**
     * Update the status to `nextStatus` by the given id.
     */
    int updateStatus(@Param("id") Integer id, @Param("nextStatus") Integer nextStatus,
            @Param("changeTime") Boolean changeModifyTime);

    /**
     * Update the status to `nextStatus` by the given group id and stream id.
     *
     * @apiNote Should not change the modify_time
     */
    int updateStatusByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("nextStatus") Integer nextStatus);

    /**
     * Update the agentIp and uuid.
     */
    int updateIpAndUuid(@Param("id") Integer id, @Param("agentIp") String agentIp, @Param("uuid") String uuid,
            @Param("changeTime") Boolean changeModifyTime);

    int updateSnapshot(StreamSourceEntity entity);

    /**
     * Physical delete stream sources.
     */
    int deleteByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);

}