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

package org.apache.inlong.manager.service.source;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.source.SourcePageRequest;
import org.apache.inlong.manager.pojo.source.SourceRequest;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;
import java.util.Map;

/**
 * Service layer interface for stream source
 */
public interface StreamSourceService {

    /**
     * Save the source information
     *
     * @param request Source request.
     * @param operator Operator's name.
     * @return source id after saving.
     */
    Integer save(SourceRequest request, String operator);

    /**
     * Save the source information
     *
     * @param request Source request.
     * @param opInfo userinfo of operator
     * @return source id after saving.
     */
    Integer save(SourceRequest request, UserInfo opInfo);

    /**
     * Query source information based on id
     *
     * @param id source id.
     * @return Source info
     */
    StreamSource get(Integer id);

    /**
     * Query source information based on id
     *
     * @param id source id.
     * @param opInfo userinfo of operator
     * @return Source info
     */
    StreamSource get(Integer id, UserInfo opInfo);

    /**
     * Query source information based on inlong group id and inlong stream id.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id, can be null.
     * @return Source info list.
     */
    List<StreamSource> listSource(String groupId, String streamId);

    /**
     * Get the StreamSource Map by the inlong group info and inlong stream info list.
     * <p/>
     * If the group mode is LIGHTWEIGHT, means not using any MQ as a cached source, then just get all related sources.
     * Otherwise, if the group mode is STANDARD, need get the cached MQ sources.
     *
     * @param groupInfo inlong group info
     * @param streamInfos inlong stream info list
     * @return map of StreamSource list, key-inlongStreamId, value-StreamSourceList
     */
    Map<String, List<StreamSource>> getSourcesMap(InlongGroupInfo groupInfo, List<InlongStreamInfo> streamInfos);

    /**
     * Query the number of undeleted source info based on inlong group and inlong stream id.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id.
     * @return Number of source info.
     */
    Integer getCount(String groupId, String streamId);

    /**
     * Paging query source information based on conditions.
     *
     * @param request paging request.
     * @return source list
     */
    PageResult<? extends StreamSource> listByCondition(SourcePageRequest request);

    /**
     * Paging query source information based on conditions.
     *
     * @param request paging request.
     * @param opInfo userinfo of operator
     * @return source list
     */
    PageResult<? extends StreamSource> listByCondition(SourcePageRequest request, UserInfo opInfo);

    /**
     * Modify data source information
     *
     * @param sourceRequest Information that needs to be modified
     * @param operator Operator's name
     * @return whether succeed
     */
    Boolean update(SourceRequest sourceRequest, String operator);

    /**
     * Modify data source information
     *
     * @param sourceRequest Information that needs to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean update(SourceRequest sourceRequest, UserInfo opInfo);

    /**
     * Update source status by the given groupId and streamId
     *
     * @param groupId The belongs group id.
     * @param streamId The belongs stream id.
     * @param targetStatus The target status.
     * @param operator The operator name.
     * @return whether succeed
     */
    Boolean updateStatus(String groupId, String streamId, Integer targetStatus, String operator);

    /**
     * Delete the stream source by the given id and source type.
     *
     * @param id The primary key of the source.
     * @param operator Operator's name
     * @return Whether succeed
     */
    Boolean delete(Integer id, String operator);

    /**
     * Delete the stream source by the given id and source type.
     *
     * @param id The primary key of the source.
     * @param opInfo userinfo of operator
     * @return Whether succeed
     */
    Boolean delete(Integer id, UserInfo opInfo);

    /**
     * Force deletes the stream source by groupId and streamId
     *
     * @param groupId The belongs group id.
     * @param streamId The belongs stream id.
     * @param operator Operator's name
     * @return Whether succeed
     */
    Boolean forceDelete(String groupId, String streamId, String operator);

    /**
     * Delete the stream source by the given id and source type.
     *
     * @param id The primary key of the source.
     * @param operator Operator's name
     * @return Whether succeed
     */
    Boolean restart(Integer id, String operator);

    /**
     * Delete the stream source by the given id and source type.
     *
     * @param id The primary key of the source.
     * @param operator Operator's name
     * @return Whether succeed
     */
    Boolean stop(Integer id, String operator);

    /**
     * Logically delete stream source with the given conditions.
     *
     * @param groupId Inlong group id to which the data source belongs.
     * @param streamId Inlong stream id to which the data source belongs.
     * @param operator Operator's name
     * @return Whether succeed.
     */
    Boolean logicDeleteAll(String groupId, String streamId, String operator);

    /**
     * Physically delete stream source with the given conditions.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id.
     * @param operator Operator's name
     * @return Whether succeed.
     */
    Boolean deleteAll(String groupId, String streamId, String operator);

    /**
     * According to the inlong stream id, query the list of source types owned by it.
     *
     * @param groupId Inlong group id.
     * @param streamId Inlong stream id.
     * @return List of source types.
     */
    List<String> getSourceTypeList(String groupId, String streamId);

    /**
     * Save the information modified when the approval is passed.
     *
     * @param operator Operator's name
     * @return Whether succeed.
     */
    default Boolean updateAfterApprove(String operator) {
        return true;
    }

}
