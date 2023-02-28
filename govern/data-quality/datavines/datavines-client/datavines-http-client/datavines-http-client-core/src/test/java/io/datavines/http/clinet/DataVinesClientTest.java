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
package io.datavines.http.clinet;


import io.datavines.http.client.DataVinesClient;
import io.datavines.http.client.base.DatavinesApiException;
import io.datavines.http.client.request.UserRegisterRequest;
import io.datavines.http.client.request.WorkSpaceCreateRequest;
import io.datavines.http.client.response.DataVinesResponse;
import io.datavines.http.client.response.UserBaseInfo;
import org.junit.Before;
import org.junit.Test;

public class DataVinesClientTest {

    private DataVinesClient client;

    {
        client = new DataVinesClient("http://localhost:5600");
    }

    @Test
    public void register() throws DatavinesApiException{
        DataVinesResponse test = client.register(new UserRegisterRequest("test1", "13435@163.com", "123456", "12436456"));
        System.out.println(test);
    }

    @Before
    public void login() throws DatavinesApiException {
        DataVinesResponse<UserBaseInfo> test = client.login("test1", "123456");
        UserBaseInfo data = test.getData();
        System.out.println(test);
        client.setToken(test.getToken());
    }

    @Test
    public void submitTask() throws DatavinesApiException {
        String json = "{\"name\":\"test\",\"parameter\":{\"metricType\":\"column_length\",\"metricParameter\":{\"table\":\"task\",\"column\":\"parameter\",\"comparator\":\">\",\"length\":1},\"srcConnectorParameter\":{\"type\":\"postgresql\",\"parameters\":{\"database\":\"datavines\",\"password\":\"lwslws\",\"port\":\"5432\",\"host\":\"localhost\",\"user\":\"postgres\",\"properties\":\"useUnicode=true&characterEncoding=UTF-8\"}}}}";
        DataVinesResponse task = client.submitTask(json);
        System.out.println(task);
    }

    @Test
    public void taskStatus() throws DatavinesApiException{
        DataVinesResponse DataVinesResponse = client.taskStatus(1516045488414031873L);
        System.out.println(DataVinesResponse);
    }

    @Test
    public void taskResultInfo() throws DatavinesApiException{
        DataVinesResponse DataVinesResponse = client.taskResultInfo(1516045488414031873L);
        System.out.println(DataVinesResponse);
    }

    @Test
    public void killTask() throws DatavinesApiException{
        DataVinesResponse DataVinesResponse = client.killTask(1516045488414031873L);
        System.out.println(DataVinesResponse);
    }

    @Test
    public void metricInfo() throws DatavinesApiException{
        DataVinesResponse DataVinesResponse = client.metricInfo("column_length");
        System.out.println(DataVinesResponse);
    }

    @Test
    public void metricList() throws DatavinesApiException{
        DataVinesResponse DataVinesResponse = client.metricList();
        System.out.println(DataVinesResponse);
    }

    @Test
    public void createWorkSpaceByName() throws DatavinesApiException{
        DataVinesResponse test = client.createWorkSpace("test");
        System.out.println(test);
    }

    @Test
    public void createWorkSpaceByCreateRequest() throws DatavinesApiException{
        DataVinesResponse test = client.createWorkSpace(new WorkSpaceCreateRequest("test2"));
        System.out.println(test);
    }

    @Test
    public void listWorkSpace() throws DatavinesApiException {
        DataVinesResponse response = client.listWorkSpace();
        System.out.println(response);
    }
}
