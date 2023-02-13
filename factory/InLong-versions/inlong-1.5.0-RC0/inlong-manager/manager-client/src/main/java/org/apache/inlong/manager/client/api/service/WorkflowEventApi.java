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

import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.EventLogResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface WorkflowEventApi {

    /**
     * Get event list by paginating
     */
    @GET("workflow/event/list")
    Call<Response<PageResult<EventLogResponse>>> list(@Query("map") Map<String, Object> map);

    /**
     * Execute the listener based on the event log ID
     */
    @POST("workflow/event/executeEventListener/{id}")
    Call<Response<Object>> executeEventListener(@Path("id") Integer id);

    /**
     * Re-execute the specified listener based on the process ID
     */
    @POST("workflow/event/≈")
    Call<Response<Object>> executeProcessEventListener(
            @Query("processId") Integer processId, @Query("listenerName") String listenerName);

    /**
     * Re-execute the specified listener based on the task ID
     */
    @POST("workflow/event/executeTaskEventListener")
    Call<Response<Object>> executeTaskEventListener(
            @Query("taskId") Integer taskId, @Query("listenerName") String listenerName);

    /**
     * Re-trigger the process event based on the process ID
     */
    @POST("workflow/event/triggerProcessEvent")
    Call<Response<Object>> triggerProcessEvent(
            @Query("processId") Integer processId, @Query("processEvent") ProcessEvent processEvent);

    /**
     * Re-trigger the process event based on the task ID
     */
    @POST("workflow/event/triggerProcessEvent")
    Call<Response<Object>> triggerTaskEvent(
            @Query("taskId") Integer taskId, @Query("taskEvent") TaskEvent taskEvent);

}