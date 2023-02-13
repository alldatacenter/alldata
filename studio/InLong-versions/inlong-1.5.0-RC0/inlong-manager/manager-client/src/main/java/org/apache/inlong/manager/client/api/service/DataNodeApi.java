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
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DataNodeApi {

    @POST("node/save")
    Call<Response<Integer>> save(@Body DataNodeRequest request);

    @GET("node/get/{id}")
    Call<Response<DataNodeInfo>> get(@Path("id") Integer id);

    @POST("node/list")
    Call<Response<PageResult<DataNodeInfo>>> list(@Body DataNodeRequest request);

    @POST("node/update")
    Call<Response<Boolean>> update(@Body DataNodeRequest request);

    @POST("node/updateByKey")
    Call<Response<UpdateResult>> updateByKey(@Body DataNodeRequest request);

    @DELETE("node/delete/{id}")
    Call<Response<Boolean>> delete(@Path("id") Integer id);

    @DELETE("node/deleteByKey")
    Call<Response<Boolean>> deleteByKey(@Query("name") String name, @Query("type") String type);

}
