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
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

public interface InlongConsumeApi {

    @POST("consume/save")
    Call<Response<Integer>> save(@Body InlongConsumeRequest request);

    @GET("consume/get/{id}")
    Call<Response<InlongConsumeInfo>> get(@Path("id") Integer id);

    @GET("consume/countStatus")
    Call<Response<InlongConsumeCountInfo>> countStatusByUser();

    @GET("consume/list")
    Call<Response<PageResult<InlongConsumeBriefInfo>>> list(@Query("request") Map<String, Object> request);

    @POST("consume/update")
    Call<Response<Integer>> update(@Body InlongConsumeRequest request);

    @DELETE("consume/delete/{id}")
    Call<Response<Boolean>> delete(@Path("id") Integer id);

    @POST("consume/startProcess/{id}")
    Call<Response<WorkflowResult>> startProcess(@Path("id") Integer id);
}
