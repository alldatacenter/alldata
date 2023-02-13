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
import org.apache.inlong.manager.dao.entity.StreamSinkEntity;
import org.apache.inlong.manager.pojo.sink.SinkBriefInfo;
import org.apache.inlong.manager.pojo.sink.SinkInfo;
import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.pojo.sort.standalone.SortIdInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortSourceStreamSinkInfo;
import org.apache.inlong.manager.pojo.sort.standalone.SortTaskInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreamSinkEntityMapper {

    int insert(StreamSinkEntity record);

    int insertSelective(StreamSinkEntity record);

    StreamSinkEntity selectByPrimaryKey(Integer id);

    /**
     * According to the inlong group id and inlong stream id, query the number of valid sink
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return Sink entity size
     */
    int selectCount(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Paging query sink list based on conditions
     *
     * @param request Paging query conditions
     * @return Sink entity list
     */
    List<StreamSinkEntity> selectByCondition(@Param("request") SinkPageRequest request);

    /**
     * Query the sink summary from the given groupId and streamId
     */
    List<SinkBriefInfo> selectSummary(@Param("groupId") String groupId,
            @Param("streamId") String streamId);

    /**
     * Query valid sink list by the given group id and stream id.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return stream sink entity list
     */
    List<StreamSinkEntity> selectByRelatedId(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Query stream sink by the unique key.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @param sinkName stream sink name
     * @return stream sink entity
     */
    StreamSinkEntity selectByUniqueKey(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("sinkName") String sinkName);

    /**
     * According to the group id, stream id and sink type, query valid sink entity list.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id.
     * @param sinkType Sink type.
     * @return Sink entity list.
     */
    List<StreamSinkEntity> selectByIdAndType(@Param("groupId") String groupId, @Param("streamId") String streamId,
            @Param("sinkType") String sinkType);

    /**
     * Filter stream ids with the specified groupId and sinkType from the given stream id list.
     *
     * @param groupId Inlong group id.
     * @param sinkType Sink type.
     * @param streamIdList Inlong stream id list.
     * @return List of Inlong stream id with the given sink type
     */
    List<String> selectExistsStreamId(@Param("groupId") String groupId, @Param("sinkType") String sinkType,
            @Param("streamIdList") List<String> streamIdList);

    /**
     * Get the distinct sink type from the given groupId and streamId
     */
    List<String> selectSinkType(@Param("groupId") String groupId, @Param("streamId") String streamId);

    /**
     * Select all config for Sort under the group id and stream id
     *
     * @param groupId inlong group id
     * @param streamIdList list of the inlong stream id, if is null, get all infos under the group id
     * @return Sort config
     */
    List<SinkInfo> selectAllConfig(@Param("groupId") String groupId, @Param("idList") List<String> streamIdList);

    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    Cursor<StreamSinkEntity> selectAllStreamSinks();

    /**
     * Select all tasks for sort-standalone
     *
     * @return All tasks
     */
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    Cursor<SortTaskInfo> selectAllTasks();

    /**
     * Select all id params for sort-standalone
     *
     * @return All id params
     */
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    Cursor<SortIdInfo> selectAllIdParams();

    /**
     * Select all streams for sort sdk.
     *
     * @return All stream info
     */
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    Cursor<SortSourceStreamSinkInfo> selectAllStreams();

    int updateByIdSelective(StreamSinkEntity record);

    int updateStatus(StreamSinkEntity entity);

    int deleteById(Integer id);

    /**
     * Physically delete all stream sinks based on inlong group ids
     *
     * @return rows deleted
     */
    int deleteByInlongGroupIds(@Param("groupIdList") List<String> groupIdList);

}
