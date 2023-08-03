/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.http.client;

import io.datavines.http.client.base.DataVinesApiEnum;
import io.datavines.http.client.base.DatavinesApiException;
import io.datavines.http.client.base.DatavinesBaseClient;
import io.datavines.http.client.request.*;
import io.datavines.http.client.response.DataVinesResponse;
import io.datavines.http.client.response.UserBaseInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.Serializable;
import java.util.Objects;
import java.util.Properties;

public class DataVinesClient extends DatavinesBaseClient {

    private Log log = LogFactory.getLog(DataVinesClient.class);

    public DataVinesClient(String baseUrl){
        super(baseUrl, new Properties(), "test", null);
    }

    /**
     * submit task
     * @param params
     * @return
     */
    public DataVinesResponse submitTask(String params) throws DatavinesApiException {
        DataVinesResponse res = callAPI(DataVinesApiEnum.TASK_SUBMIT_API.getDataVinesApi(), params);
        return res;
    }

    /**
     * submit task
     * @param request
     * @return
     */
    public DataVinesResponse submitTask(SubmitTaskRequest request) throws DatavinesApiException {
        DataVinesResponse res = callAPI(DataVinesApiEnum.TASK_SUBMIT_API.getDataVinesApi(), request);
        return res;
    }

    /**
     * kill task
     * @param taskId
     * @return
     * @throws DatavinesApiException
     */
    public DataVinesResponse killTask(Long taskId) throws DatavinesApiException {
       return callApiWithPathParam(DataVinesApiEnum.TASK_KILL_API, taskId);
    }

    /**
     * get task status
     * @param taskId
     * @return task status
     * @throws DatavinesApiException
     */
    public DataVinesResponse taskStatus(Long taskId) throws DatavinesApiException{
        return callApiWithPathParam(DataVinesApiEnum.TASK_STATUS_API, taskId);
    }

    /**
     * get task result info
     * @param taskId
     * @return task result
     * @throws DatavinesApiException
     */
    public DataVinesResponse taskResultInfo(Long taskId) throws DatavinesApiException{
        return callApiWithPathParam(DataVinesApiEnum.TASK_RESULT_API, taskId);
    }

    /**
     * get metric list
     * @return metric list
     * @throws DatavinesApiException
     */
    public DataVinesResponse metricList() throws DatavinesApiException {
        return callAPI(DataVinesApiEnum.METRIC_LIST_API);
    }

    /**
     * get metric info by metric name
     * @param name metric name
     * @return metric info
     * @throws DatavinesApiException
     */
    public DataVinesResponse metricInfo(String name) throws DatavinesApiException {
        return callApiWithPathParam(DataVinesApiEnum.METRIC_INFO_API, name);
    }

    public DataVinesResponse createWorkSpace(String spaceName) throws DatavinesApiException{
        DataVinesResponse result = callAPI(DataVinesApiEnum.CREATE_WORKSPACE.getDataVinesApi(), new WorkSpaceCreateRequest(spaceName));
        return result;
    }

    public DataVinesResponse createWorkSpace(WorkSpaceCreateRequest spaceName) throws DatavinesApiException{
        DataVinesResponse result = callAPI(DataVinesApiEnum.CREATE_WORKSPACE.getDataVinesApi(), spaceName);
        return result;
    }

    public DataVinesResponse updateWorkSpace(WorkSpaceUpdateRequest update) throws DatavinesApiException{
        DataVinesResponse result = callAPI(DataVinesApiEnum.UPDATE_WORKSPACE.getDataVinesApi(), update);
        return result;
    }

    public DataVinesResponse deleteWorkSpace(Long workspaceId) throws DatavinesApiException{
        DataVinesResponse result = callApiWithPathParam(DataVinesApiEnum.DELETE_WORKSPACE, workspaceId);
        return result;
    }

    public DataVinesResponse listWorkSpace() throws DatavinesApiException{
        DataVinesResponse result = callAPI(DataVinesApiEnum.LIST_WORKSPACE);
        return result;
    }

    public DataVinesResponse register(UserRegisterRequest registerRequest) throws DatavinesApiException{
        DataVinesResponse result = callAPI(DataVinesApiEnum.REGISTER.getDataVinesApi(), registerRequest);
        return result;
    }

    public DataVinesResponse login(UserLoginRequest loginRequest) throws DatavinesApiException{
        DataVinesResponse result = callAPI(DataVinesApiEnum.LOGIN.getDataVinesApi(), loginRequest);
        return result;
    }

    public DataVinesResponse<UserBaseInfo> login(String name, String password) throws DatavinesApiException{
        DataVinesResponse<UserBaseInfo> result = callAPI(DataVinesApiEnum.LOGIN.getDataVinesApi(), new UserLoginRequest(name, password));
        return result;
    }

    private <T> DataVinesResponse<T> callApiWithPathParam(DataVinesApiEnum dataVinesAPI, Serializable id) throws DatavinesApiException{
        if (Objects.isNull(id)){
            log.error("task id must not null!");
            throw new DatavinesApiException("task id must not null!");
        }
        DataVinesResponse result = callAPI(dataVinesAPI.getDataVinesApi(String.valueOf(id)), null);
        return result;
    }

    private <T> DataVinesResponse<T> callAPI(DataVinesApiEnum dataVinesAPI) throws DatavinesApiException{
        DataVinesResponse result = callAPI(dataVinesAPI.getDataVinesApi(), null);
        return result;
    }
}
