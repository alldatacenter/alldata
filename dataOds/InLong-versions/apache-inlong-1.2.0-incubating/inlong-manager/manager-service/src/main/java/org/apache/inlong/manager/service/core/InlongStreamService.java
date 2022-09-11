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

package org.apache.inlong.manager.service.core;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.stream.FullStreamRequest;
import org.apache.inlong.manager.common.pojo.stream.FullStreamResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamApproveRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamListResponse;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamPageRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamRequest;
import org.apache.inlong.manager.common.pojo.stream.StreamBriefResponse;

import java.util.List;

/**
 * Inlong stream service layer interface
 *
 * @apiNote It is associated with various sources, the upstream is StreamSource, and the downstream is
 *         StreamSink
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
     * Query the details of the specified inlong stream
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @return inlong stream details
     */
    InlongStreamInfo get(String groupId, String streamId);

    /**
     * List streams contained in one group
     *
     * @param groupId inlong group id.
     * @return Inlong stream info list
     */
    List<InlongStreamInfo> list(String groupId);

    /**
     * Query whether the inlong stream ID exists
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return true: exists, false: does not exist
     */
    Boolean exist(String groupId, String streamId);

    /**
     * Query inlong stream list based on conditions
     *
     * @param request Inlong stream paging query request
     * @return Inlong stream paging list
     */
    PageInfo<InlongStreamListResponse> listByCondition(InlongStreamPageRequest request);

    /**
     * InlongStream info that needs to be modified
     *
     * @param request inlong stream info that needs to be modified
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean update(InlongStreamRequest request, String operator);

    /**
     * Delete the specified inlong stream
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean delete(String groupId, String streamId, String operator);

    /**
     * Logically delete all inlong streams under the specified groupId
     *
     * @param groupId Inlong group id
     * @param operator Edit person's name
     * @return whether succeed
     */
    Boolean logicDeleteAll(String groupId, String operator);

    /**
     * Obtain the flow of inlong stream according to groupId
     *
     * @param groupId Inlong group id
     * @return Summary list of inlong stream
     */
    List<StreamBriefResponse> getBriefList(String groupId);

    /**
     * Save all information related to the inlong stream, its data source, and stream sink
     *
     * @param fullStreamRequest All information on the page
     * @param operator Edit person's name
     * @return Whether the save was successful
     */
    boolean saveAll(FullStreamRequest fullStreamRequest, String operator);

    /**
     * Save inlong streams, their data sources, and all information related to stream sink in batches
     *
     * @param fullStreamRequestList List of inlong stream page information
     * @param operator Edit person's name
     * @return Whether the save was successful
     * @apiNote This interface is only used when creating a new inlong group. To ensure data consistency,
     *         all associated data needs to be physically deleted, and then added
     */
    boolean batchSaveAll(List<FullStreamRequest> fullStreamRequestList, String operator);

    /**
     * Paging query all data of the inlong stream page under the specified groupId
     *
     * @param request Query
     * @return Paging list of all data on the inlong stream page
     */
    PageInfo<FullStreamResponse> listAllWithGroupId(InlongStreamPageRequest request);

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
