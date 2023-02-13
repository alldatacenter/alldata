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

package org.apache.inlong.manager.service.group;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupApproveRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Inlong group service layer interface
 */
public interface InlongGroupService {

    /**
     * Save inlong group info.
     *
     * @param groupInfo group request need to save
     * @param operator name of operator
     * @return inlong group id after saving
     */
    String save(@Valid @NotNull(message = "inlong group request cannot be null") InlongGroupRequest groupInfo,
            String operator);

    /**
     * Save inlong group info.
     *
     * @param groupInfo group request need to save
     * @param opInfo    userinfo of operator
     * @return detail of inlong group
     */
    String save(@Valid @NotNull(message = "inlong group request cannot be null") InlongGroupRequest groupInfo,
            UserInfo opInfo);

    /**
     * Query whether the specified group id exists
     *
     * @param groupId the group id to be queried
     * @return does it exist
     */
    Boolean exist(String groupId);

    /**
     * Get inlong group info based on inlong group id
     *
     * @param groupId inlong group id
     * @return detail of inlong group
     */
    InlongGroupInfo get(String groupId);

    /**
     * Get inlong group info based on inlong group id
     *
     * @param groupId inlong group id
     * @param opInfo userinfo of operator
     * @return detail of inlong group
     */
    InlongGroupInfo get(String groupId, UserInfo opInfo);

    /**
     * Query the group information of each status of the current user
     *
     * @param operator name of operator
     * @return inlong group status statistics
     */
    InlongGroupCountResponse countGroupByUser(String operator);

    /**
     * According to the group id, query the topic to which it belongs
     *
     * @param groupId Inlong group id
     * @return Topic information
     * @apiNote TubeMQ corresponds to the group, only 1 topic
     */
    InlongGroupTopicInfo getTopic(String groupId);

    /**
     * According to the group id, query the backup topic to which it belongs
     *
     * @param groupId inlong group id
     * @return backup topic info
     */
    InlongGroupTopicInfo getBackupTopic(String groupId);

    /**
     * Paging query inlong group brief info list
     *
     * @param request pagination query request
     * @return group list
     */
    PageResult<InlongGroupBriefInfo> listBrief(InlongGroupPageRequest request);

    /**
     * Query inlong group brief info list
     *
     * @param request pagination query request
     * @param opInfo  userinfo of operator
     * @return group list
     */
    List<InlongGroupBriefInfo> listBrief(InlongGroupPageRequest request, UserInfo opInfo);

    /**
     * Modify group information
     *
     * @param request inlong group request that needs to be modified
     * @param operator name of operator
     * @return inlong group id
     */
    String update(@Valid @NotNull(message = "inlong group request cannot be null") InlongGroupRequest request,
            String operator);

    /**
     * Modify group information
     *
     * @param request inlong group request that needs to be modified
     * @param opInfo userinfo of operator
     * @return inlong group id
     */
    String update(@Valid @NotNull(message = "inlong group request cannot be null") InlongGroupRequest request,
            UserInfo opInfo);

    /**
     * Modify the status of the specified group
     *
     * @param groupId inlong group id
     * @param status modified status
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean updateStatus(String groupId, Integer status, String operator);

    /**
     * Check whether deletion is supported for the specified group.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return inlong group info
     */
    InlongGroupInfo doDeleteCheck(String groupId, String operator);

    /**
     * Delete the group information of the specified group id
     *
     * @param groupId The group id that needs to be deleted
     * @param operator name of operator
     * @return whether succeed
     * @apiNote Before invoking this delete method, you must
     */
    Boolean delete(String groupId, String operator);

    /**
     * Save the group modified when the approval is passed
     *
     * @param approveRequest approval information
     * @param operator name of operator
     */
    void updateAfterApprove(
            @Valid @NotNull(message = "approve request cannot be null") InlongGroupApproveRequest approveRequest,
            String operator);

    /**
     * Save or update extended information
     * <p/>First physically delete the existing extended information, and then add this batch of extended information
     *
     * @param groupId inlong group id
     * @param infoList inlong group ext info list
     */
    void saveOrUpdateExt(String groupId, List<InlongGroupExtInfo> infoList);

    /**
     * List topic infos
     * @return List of InlongGroupTopicInfo
     */
    List<InlongGroupTopicInfo> listTopics(InlongGroupTopicRequest clusterTag);

}
