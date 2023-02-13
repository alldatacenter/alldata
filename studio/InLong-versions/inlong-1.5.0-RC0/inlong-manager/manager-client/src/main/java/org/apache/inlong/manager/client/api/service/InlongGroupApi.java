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
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupTopicRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;

public interface InlongGroupApi {

    @GET("group/exist/{id}")
    Call<Response<Boolean>> isGroupExists(@Path("id") String id);

    @GET("group/get/{id}")
    Call<Response<Object>> getGroupInfo(@Path("id") String id);

    @POST("group/list")
    Call<Response<PageResult<InlongGroupBriefInfo>>> listGroups(@Body InlongGroupPageRequest request);

    @POST("group/save")
    Call<Response<String>> createGroup(@Body InlongGroupRequest request);

    @POST("group/update")
    Call<Response<String>> updateGroup(@Body InlongGroupRequest request);

    @POST("group/startProcess/{id}")
    Call<Response<WorkflowResult>> initInlongGroup(@Path("id") String id);

    @POST("group/suspendProcessAsync/{id}")
    Call<Response<String>> suspendProcessAsync(@Path("id") String id);

    @POST("group/suspendProcess/{id}")
    Call<Response<String>> suspendProcess(@Path("id") String id);

    @POST("group/restartProcessAsync/{id}")
    Call<Response<String>> restartProcessAsync(@Path("id") String id);

    @POST("group/restartProcess/{id}")
    Call<Response<String>> restartProcess(@Path("id") String id);

    @DELETE("group/deleteAsync/{id}")
    Call<Response<String>> deleteGroupAsync(@Path("id") String id);

    @DELETE("group/delete/{id}")
    Call<Response<Boolean>> deleteGroup(@Path("id") String id);

    @POST("group/reset")
    Call<Response<Boolean>> resetGroup(@Body InlongGroupResetRequest request);

    @GET("group/countByStatus")
    Call<Response<Object>> countGroupByUser();

    @GET("group/getTopic/{id}")
    Call<Response<Object>> getTopic(@Path("id") String id);

    @GET("group/listTopics")
    Call<Response<List<InlongGroupTopicInfo>>> listTopics(@Body InlongGroupTopicRequest request);
}
