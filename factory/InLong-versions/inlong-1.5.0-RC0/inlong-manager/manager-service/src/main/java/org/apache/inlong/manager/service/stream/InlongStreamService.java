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

package org.apache.inlong.manager.service.stream;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.stream.InlongStreamApproveRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;

/**
 * Inlong stream service layer interface
 *
 * @apiNote InlongStream was associated with various sources, the upstream is StreamSource,
 *         and the downstream is StreamSink
 */
public interface InlongStreamService {

    /**
     * Save inlong stream information.
     *
     * @param request Inlong stream information.
     * @param operator The name of operator.
     * @return Id after successful save.
     */
    Integer save(InlongStreamRequest request, String operator);

    /**
     * Save inlong stream information.
     *
     * @param request Inlong stream information.
     * @param opInfo userinfo of operator
     * @return Id after successful save.
     */
    Integer save(InlongStreamRequest request, UserInfo opInfo);

    /**
     * Query whether the inlong stream ID exists
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return true: exists, false: does not exist
     */
    Boolean exist(String groupId, String streamId);

    /**
     * Query the details of the specified inlong stream
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @return inlong stream details
     */
    InlongStreamInfo get(String groupId, String streamId);

    /**
     * Query the details of the specified inlong stream
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @param opInfo userinfo of operator
     * @return inlong stream details
     */
    InlongStreamInfo get(String groupId, String streamId, UserInfo opInfo);

    /**
     * List streams contained in one group
     *
     * @param groupId inlong group id.
     * @return Inlong stream info list
     */
    List<InlongStreamInfo> list(String groupId);

    /**
     * Paging query inlong stream brief info list
     *
     * @param request query request
     * @return inlong stream brief list
     */
    PageResult<InlongStreamBriefInfo> listBrief(InlongStreamPageRequest request);

    /**
     * Query inlong stream brief info list
     *
     * @param request query request
     * @param opInfo  userinfo of operator
     * @return inlong stream brief list
     */
    List<InlongStreamBriefInfo> listBrief(InlongStreamPageRequest request, UserInfo opInfo);

    /**
     * Paging query inlong stream full info list, and get all related sources and sinks
     *
     * @param request query request
     * @return inlong stream info list
     */
    PageResult<InlongStreamInfo> listAll(InlongStreamPageRequest request);

    /**
     * Get the inlong stream brief list and related sink brief list.
     *
     * @param groupId inlong group id
     * @return brief list of inlong stream
     */
    List<InlongStreamBriefInfo> listBriefWithSink(String groupId);

    /**
     * Update the InlongStream info
     *
     * @param request inlong stream info that needs to be modified
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean update(InlongStreamRequest request, String operator);

    /**
     * Update the InlongStream info
     *
     * @param request inlong stream info that needs to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean update(InlongStreamRequest request, UserInfo opInfo);

    /**
     * Update the InlongStream - not check the InlongGroup status to which the stream belongs.
     *
     * @param request inlong stream info that needs to be modified
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean updateWithoutCheck(InlongStreamRequest request, String operator);

    /**
     * Delete the specified inlong stream.
     * <p/>
     * When deleting an inlong stream, you need to check whether there are some related
     * stream_sources or stream_sinks
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean delete(String groupId, String streamId, String operator);

    /**
     * Delete the specified inlong stream.
     * <p/>
     * When deleting an inlong stream, you need to check whether there are some related
     * stream_sources or stream_sinks
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean delete(String groupId, String streamId, UserInfo opInfo);

    /**
     * Logically delete all inlong streams under the specified groupId
     *
     * @param groupId Inlong group id
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean logicDeleteAll(String groupId, String operator);

    /**
     * According to the group id, query the number of valid inlong streams belonging to this service
     *
     * @param groupId Inlong group id
     * @return Number of inlong streams
     */
    int selectCountByGroupId(String groupId);

    /**
     * According to the inlong group id, query the Topic list
     */
    List<InlongStreamBriefInfo> getTopicList(String groupId);

    /**
     * Save the information modified when the approval is passed
     *
     * @param streamApproveList inlong stream approval information
     * @param operator Edit person's name
     * @return whether succeed
     */
    boolean updateAfterApprove(List<InlongStreamApproveRequest> streamApproveList, String operator);

    /**
     * Update stream status
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @param status Modified status
     * @param operator Edit person's name
     * @return whether succeed
     * @apiNote If streamId is null, update all inlong stream associated with groupId
     */
    boolean updateStatus(String groupId, String streamId, Integer status, String operator);

    /**
     * According to the specified DLQ / RLQ name, create the corresponding Pulsar's Topic stream
     *
     * @param topicName Pulsar's Topic name, which is the inlong stream ID
     */
    void insertDlqOrRlq(String bid, String topicName, String operator);

    /**
     * Logic delete dlq or rlq topic by bid
     */
    void logicDeleteDlqOrRlq(String bid, String topicName, String operator);

}
