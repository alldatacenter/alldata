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
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApi {

    @POST("user/currentUser")
    Call<Response<UserInfo>> currentUser();

    @POST("user/register")
    Call<Response<Integer>> register(@Body UserRequest userInfo);

    @GET("user/get/{id}")
    Call<Response<UserInfo>> getById(@Path("id") Integer id);

    @GET("user/getByName/{name}")
    Call<Response<UserInfo>> getByName(@Path("name") String name);

    @POST("user/listAll")
    Call<Response<PageResult<UserInfo>>> list(@Body UserRequest request);

    @POST("user/update")
    Call<Response<Integer>> update(@Body UserRequest userInfo);

    @DELETE("user/delete")
    Call<Response<Boolean>> delete(@Query("id") Integer id);
}
