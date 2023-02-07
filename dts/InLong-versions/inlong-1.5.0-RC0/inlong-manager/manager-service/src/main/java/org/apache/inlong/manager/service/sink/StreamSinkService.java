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

package org.apache.inlong.manager.service.sink;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.SinkApproveDTO;
import org.apache.inlong.manager.pojo.sink.SinkBriefInfo;
import org.apache.inlong.manager.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.pojo.sink.SinkRequest;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Service layer interface for stream sink
 */
public interface StreamSinkService {

    /**
     * Save the sink info.
     *
     * @param request sink request need to save
     * @param operator name of operator
     * @return sink id after saving
     */
    Integer save(SinkRequest request, String operator);

    /**
     * Save the sink info.
     *
     * @param request sink request need to save
     * @param opInfo userinfo of operator
     * @return sink id after saving
     */
    Integer save(SinkRequest request, UserInfo opInfo);

    /**
     * Get stream sink info based on id.
     *
     * @param id sink id
     * @return detail of stream sink info
     */
    StreamSink get(Integer id);

    /**
     * Get stream sink info based on id.
     *
     * @param id sink id
     * @param opInfo userinfo of operator
     * @return detail of stream sink info
     */
    StreamSink get(Integer id, UserInfo opInfo);

    /**
     * List the stream sinks based on inlong group id and inlong stream id.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id, can be null
     * @return sink info list
     */
    List<StreamSink> listSink(String groupId, @Nullable String streamId);

    /**
     * Query sink brief info based on inlong group id and inlong stream id.
     * <p/>
     * The result will include sink cluster info.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return stream sink brief info list
     */
    List<SinkBriefInfo> listBrief(String groupId, String streamId);

    /**
     * Get the StreamSink Map by the inlong group info and inlong stream info list.
     *
     * @param groupInfo inlong group info
     * @param streamInfos inlong stream info list
     * @return map of StreamSink list, key-inlongStreamId, value-StreamSinkList
     */
    Map<String, List<StreamSink>> getSinksMap(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfos);

    /**
     * Query the number of undeleted sink info based on inlong group and inlong stream id.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return count of sink info
     */
    Integer getCount(String groupId, String streamId);

    /**
     * Paging query stream sink info based on conditions.
     *
     * @param request paging request
     * @return sink page list
     */
    PageResult<? extends StreamSink> listByCondition(SinkPageRequest request);

    /**
     * Paging query stream sink info based on conditions.
     *
     * @param request paging request
     * @param opInfo  userinfo of operator
     * @return sink page list
     */
    List<? extends StreamSink> listByCondition(SinkPageRequest request, UserInfo opInfo);

    /**
     * Modify stream sink info by id.
     *
     * @param sinkRequest stream sink request that needs to be modified
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean update(SinkRequest sinkRequest, String operator);

    /**
     * Modify stream sink info by id.
     *
     * @param sinkRequest stream sink request that needs to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean update(SinkRequest sinkRequest, UserInfo opInfo);

    /**
     * Modify stream sink info by key.
     *
     * @param sinkRequest stream sink request that needs to be modified
     * @param operator name of operator
     * @return update result
     */
    UpdateResult updateByKey(SinkRequest sinkRequest, String operator);

    /**
     * Modify stream sink status.
     *
     * @param id stream sink id
     * @param status target status
     * @param log log info of this modification
     */
    void updateStatus(Integer id, int status, String log);

    /**
     * Delete the stream sink by the given id and sink type.
     *
     * @param id stream sink id
     * @param startProcess whether to start the process after saving or updating
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean delete(Integer id, Boolean startProcess, String operator);

    /**
     * Delete the stream sink by the given id and sink type.
     *
     * @param id stream sink id
     * @param startProcess whether to start the process after saving or updating
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean delete(Integer id, Boolean startProcess, UserInfo opInfo);

    /**
     * Delete the stream sink by given group id, stream id, and sink name.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @param name stream sink name
     * @param startProcess whether to start the process after saving or updating
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean deleteByKey(String groupId, String streamId, String name, Boolean startProcess, String operator);

    /**
     * Logically delete stream sink with the given conditions.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean logicDeleteAll(String groupId, String streamId, String operator);

    /**
     * Physically delete stream sink with the given conditions.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean deleteAll(String groupId, String streamId, String operator);

    /**
     * According to the existing inlong stream ID list, filter out the inlong stream id list
     * containing the specified sink type.
     *
     * @param groupId inlong group id
     * @param sinkType stream sink type
     * @param streamIdList inlong stream id list
     * @return list of filtered inlong stream ids
     */
    List<String> getExistsStreamIdList(String groupId, String sinkType, List<String> streamIdList);

    /**
     * According to the inlong stream id, query the list of sink types owned by it
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return list of sink types
     */
    List<String> getSinkTypeList(String groupId, String streamId);

    /**
     * Save the information modified when the approval is passed
     *
     * @param sinkApproveList stream sink approval information
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean updateAfterApprove(List<SinkApproveDTO> sinkApproveList, String operator);

}
