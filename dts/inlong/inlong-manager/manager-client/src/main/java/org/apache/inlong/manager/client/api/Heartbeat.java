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
import org.apache.inlong.manager.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;

/**
 * Heartbeat interface.
 */
public interface Heartbeat {

    /**
     * Get component heartbeat
     *
     * @param request query request of heartbeat
     * @return component heartbeat
     */
    ComponentHeartbeatResponse getComponentHeartbeat(HeartbeatQueryRequest request);

    /**
     * Get inlong group heartbeat
     *
     * @param request query request of heartbeat
     * @return group heartbeat
     */
    GroupHeartbeatResponse getGroupHeartbeat(HeartbeatQueryRequest request);

    /**
     * Get inlong stream heartbeat
     *
     * @param request query request of heartbeat
     * @return stream heartbeat
     */
    StreamHeartbeatResponse getStreamHeartbeat(HeartbeatQueryRequest request);

    /**
     * List component heartbeat by page
     *
     * @param request paging query request
     * @return list of component heartbeat
     */
    PageResult<ComponentHeartbeatResponse> listComponentHeartbeat(HeartbeatPageRequest request);

    /**
     * List group heartbeat by page
     *
     * @param request paging query request
     * @return list of group heartbeat
     */
    PageResult<GroupHeartbeatResponse> listGroupHeartbeat(HeartbeatPageRequest request);

    /**
     * List stream heartbeat by page
     *
     * @param request paging query request
     * @return list of stream heartbeat
     */
    PageResult<StreamHeartbeatResponse> listStreamHeartbeat(HeartbeatPageRequest request);
}
