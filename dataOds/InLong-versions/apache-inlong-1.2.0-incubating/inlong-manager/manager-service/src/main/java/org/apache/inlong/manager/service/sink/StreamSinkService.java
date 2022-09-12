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

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.sink.SinkApproveDTO;
import org.apache.inlong.manager.common.pojo.sink.SinkBriefResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkListResponse;
import org.apache.inlong.manager.common.pojo.sink.SinkPageRequest;
import org.apache.inlong.manager.common.pojo.sink.SinkRequest;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;

import java.util.List;

/**
 * Service layer interface for stream sink
 */
public interface StreamSinkService {

    /**
     * Save the sink information.
     *
     * @param request Sink request.
     * @param operator Operator's name.
     * @return Sink id after saving.
     */
    Integer save(SinkRequest request, String operator);

    /**
     * Query sink information based on id.
     *
     * @param id Sink id.
     * @return Sink info.
     */
    StreamSink get(Integer id);

    /**
     * Query sink information based on inlong group id and inlong stream id.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id, can be null.
     * @return Sink info list.
     */
    List<StreamSink> listSink(String groupId, String streamId);

    /**
     * Query sink summary based on inlong group id and inlong stream id, including sink cluster.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id.
     * @return Sink info list.
     */
    List<SinkBriefResponse> listBrief(String groupId, String streamId);

    /**
     * Query the number of undeleted sink info based on inlong group and inlong stream id
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id.
     * @return Number of sink info.
     */
    Integer getCount(String groupId, String streamId);

    /**
     * Paging query sink information based on conditions.
     *
     * @param request Paging request.
     * @return Sink info list.
     */
    PageInfo<? extends SinkListResponse> listByCondition(SinkPageRequest request);

    /**
     * Modify data sink information.
     *
     * @param sinkRequest Information that needs to be modified.
     * @param operator Operator's name.
     * @return Whether succeed.
     */
    Boolean update(SinkRequest sinkRequest, String operator);

    /**
     * Modify sink data status.
     *
     * @param id Sink id.
     * @param status Target status.
     * @param log Modify the log.
     */
    void updateStatus(int id, int status, String log);

    /**
     * Delete the stream sink by the given id and sink type.
     *
     * @param id The primary key of the sink.
     * @param operator Operator's name.
     * @return Whether succeed
     */
    Boolean delete(Integer id, String operator);

    /**
     * Logically delete stream sink with the given conditions.
     *
     * @param groupId InLong group id to which the data source belongs.
     * @param streamId InLong stream id to which the data source belongs.
     * @param operator Operator's name.
     * @return Whether succeed.
     */
    Boolean logicDeleteAll(String groupId, String streamId, String operator);

    /**
     * Physically delete stream sink with the given conditions.
     *
     * @param groupId InLong group id.
     * @param streamId InLong stream id.
     * @param operator Operator's name.
     * @return Whether succeed.
     */
    Boolean deleteAll(String groupId, String streamId, String operator);

    /**
     * According to the existing inlong stream ID list, filter out the inlong stream id list
     * containing the specified sink type.
     *
     * @param groupId Inlong group id.
     * @param sinkType Sink type.
     * @param streamIdList Inlong stream id list.
     * @return List of filtered inlong stream ids.
     */
    List<String> getExistsStreamIdList(String groupId, String sinkType, List<String> streamIdList);

    /**
     * According to the inlong stream id, query the list of sink types owned by it
     *
     * @param groupId Inlong group id
     * @param streamId Inlong stream id
     * @return List of sink types
     */
    List<String> getSinkTypeList(String groupId, String streamId);

    /**
     * Save the information modified when the approval is passed
     *
     * @param sinkApproveList Stream sink approval information
     * @param operator Operator's name
     * @return whether succeed
     */
    Boolean updateAfterApprove(List<SinkApproveDTO> sinkApproveList, String operator);

}
