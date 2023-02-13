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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;

public interface InlongConsume {

    /**
     * Save inlong consume info.
     *
     * @param request consume request need to save
     * @return inlong consume id after saving
     */
    Integer save(InlongConsumeRequest request);

    /**
     * Get inlong consume info based on ID
     *
     * @param id inlong consume id
     * @return detail of inlong group
     */
    InlongConsumeInfo get(Integer id);

    /**
     * Query the inlong consume statistics info via the username
     *
     * @return inlong consume status statistics
     */
    InlongConsumeCountInfo countStatusByUser();

    /**
     * Paging query inlong consume info list
     *
     * @param request pagination query request
     * @return inlong consume list
     */
    PageResult<InlongConsumeBriefInfo> list(InlongConsumePageRequest request);

    /**
     * Update the inlong consume
     *
     * @param request inlong consume request that needs to be updated
     * @return inlong consume id after saving
     */
    Integer update(InlongConsumeRequest request);

    /**
     * Delete the inlong consume by the id
     *
     * @param id inlong consume id that needs to be deleted
     * @return whether succeed
     */
    Boolean delete(Integer id);

    /**
     * Start the process for the specified ID.
     *
     * @param id inlong consume id
     * @return workflow result
     */
    WorkflowResult startProcess(Integer id);
}
