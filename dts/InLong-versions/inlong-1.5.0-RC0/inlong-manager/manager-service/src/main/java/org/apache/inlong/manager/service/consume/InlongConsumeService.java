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

package org.apache.inlong.manager.service.consume;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Inlong consume service layer interface
 */
public interface InlongConsumeService {

    /**
     * Save inlong consume info.
     *
     * @param request consume request need to save
     * @param operator name of operator
     * @return inlong consume id after saving
     */
    Integer save(InlongConsumeRequest request, String operator);

    /**
     * Save the consumer group by InLong system, and not start the workflow process.
     *
     * @return inlong consume id after saving
     */
    Integer saveBySystem(InlongGroupInfo groupInfo, String topic, String consumerGroup);

    /**
     * Get inlong consume info based on ID
     *
     * @param id inlong consume id
     * @return detail of inlong group
     */
    InlongConsumeInfo get(Integer id);

    /**
     * Check whether the consumer group exists or not
     *
     * @param consumerGroup consumer group
     * @param excludeSelfId exclude the ID of this record
     * @return true if exists, false if not exists
     */
    boolean consumerGroupExists(String consumerGroup, Integer excludeSelfId);

    /**
     * Paging query inlong consume info list
     *
     * @param request pagination query request
     * @return inlong consume list
     */
    PageResult<InlongConsumeBriefInfo> list(InlongConsumePageRequest request);

    /**
     * Query the inlong consume statistics info via the username
     *
     * @param username username
     * @return inlong consume status statistics
     */
    InlongConsumeCountInfo countStatus(String username);

    /**
     * Update the inlong consume
     *
     * @param request inlong consume request that needs to be updated
     * @param operator name of operator
     * @return inlong consume id after saving
     */
    Integer update(@Valid @NotNull(message = "inlong consume request cannot be null") InlongConsumeRequest request,
            String operator);

    /**
     * Update the inlong consume status to the specified status
     *
     * @param id inlong consume id
     * @param status modified status
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean updateStatus(Integer id, Integer status, String operator);

    /**
     * Delete the inlong consume by the id
     *
     * @param id inlong consume id that needs to be deleted
     * @param operator name of operator
     * @return whether succeed
     */
    Boolean delete(Integer id, String operator);

}
