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

package org.apache.inlong.manager.client.api.service;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * API for HeartbeatController in manager-web
 */
public interface HeartbeatApi {

    @POST(value = "heartbeat/component/get")
    Call<Response<ComponentHeartbeatResponse>> getComponentHeartbeat(@Body HeartbeatQueryRequest request);

    @POST(value = "heartbeat/group/get")
    Call<Response<GroupHeartbeatResponse>> getGroupHeartbeat(@Body HeartbeatQueryRequest request);

    @POST(value = "heartbeat/stream/get")
    Call<Response<StreamHeartbeatResponse>> getStreamHeartbeat(@Body HeartbeatQueryRequest request);

    @POST(value = "heartbeat/component/list")
    Call<Response<PageResult<ComponentHeartbeatResponse>>> listComponentHeartbeat(@Body HeartbeatPageRequest request);

    @POST(value = "heartbeat/group/list")
    Call<Response<PageResult<GroupHeartbeatResponse>>> listGroupHeartbeat(@Body HeartbeatPageRequest request);

    @POST(value = "heartbeat/stream/list")
    Call<Response<PageResult<StreamHeartbeatResponse>>> listStreamHeartbeat(@Body HeartbeatPageRequest request);
}
