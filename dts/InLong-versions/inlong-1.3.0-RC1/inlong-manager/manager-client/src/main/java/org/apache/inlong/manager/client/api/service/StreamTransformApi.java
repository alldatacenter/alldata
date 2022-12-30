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

import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

public interface StreamTransformApi {

    @POST("transform/save")
    Call<Response<Integer>> createTransform(@Body TransformRequest request);

    @GET("transform/list")
    Call<Response<List<TransformResponse>>> listTransform(@Query("inlongGroupId") String groupId,
            @Query("inlongStreamId") String streamId);

    @POST("transform/update")
    Call<Response<Boolean>> updateTransform(@Body TransformRequest request);

    @DELETE("transform/delete")
    Call<Response<Boolean>> deleteTransform(@Query("inlongGroupId") String groupId,
            @Query("inlongStreamId") String streamId, @Query("transformName") String transformName);

}
