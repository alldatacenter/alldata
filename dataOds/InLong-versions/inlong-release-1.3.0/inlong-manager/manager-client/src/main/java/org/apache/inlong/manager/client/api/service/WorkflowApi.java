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
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowOperationRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface WorkflowApi {

    @Headers("Content-Type: application/json")
    @POST("workflow/approve/{taskId}")
    Call<Response<WorkflowResult>> startInlongGroup(@Path("taskId") Integer taskId, @Body Map<String, Object> request);

    @POST("workflow/start")
    Call<Response<WorkflowResult>> start(@Body WorkflowOperationRequest request);

    @POST("workflow/cancel/{processId}")
    Call<Response<WorkflowResult>> cancel(@Path("processId") Integer processId, @Body WorkflowOperationRequest request);

    @POST("workflow/continue/{processId}")
    Call<Response<WorkflowResult>> continueProcess(@Path("processId") Integer processId,
            @Body WorkflowOperationRequest request);

    @POST("workflow/reject/{taskId}")
    Call<Response<WorkflowResult>> reject(@Path("taskId") Integer taskId, @Body WorkflowOperationRequest request);

    @POST("workflow/complete/{taskId}")
    Call<Response<WorkflowResult>> complete(@Path("taskId") Integer taskId, @Body WorkflowOperationRequest request);

    @GET("workflow/detail/{processId}")
    Call<Response<ProcessDetailResponse>> detail(@Path("processId") Integer processId, @Query("taskId") Integer taskId);

    @GET("workflow/listProcess")
    Call<Response<PageResult<ProcessResponse>>> listProcess(@Query("query") Map<String, Object> query);

    @GET("workflow/listTask")
    Call<Response<PageResult<TaskResponse>>> listTask(@Query("query") Map<String, Object> query);

}
