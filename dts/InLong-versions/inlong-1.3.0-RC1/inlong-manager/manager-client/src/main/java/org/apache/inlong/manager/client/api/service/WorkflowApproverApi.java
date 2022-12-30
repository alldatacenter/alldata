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
import org.apache.inlong.manager.pojo.workflow.ApproverRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface WorkflowApproverApi {

    @POST("workflow/approver/save")
    Call<Response<Integer>> save(@Body ApproverRequest request);

    @GET("workflow/approver/get/{id}")
    Call<Response<ApproverResponse>> get(@Path("id") Integer id);

    @GET("workflow/approver/list")
    Call<Response<PageResult<ApproverResponse>>> listByCondition(@Query("map") Map<String, Object> map);

    @POST("workflow/approver/update")
    Call<Response<Integer>> update(@Body ApproverRequest request);

    @POST("workflow/approver/delete/{id}")
    Call<Response<Boolean>> delete(@Path("id") Integer id);

}
