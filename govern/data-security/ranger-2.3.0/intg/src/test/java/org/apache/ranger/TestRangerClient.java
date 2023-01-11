/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.RangerRESTClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.testng.annotations.BeforeMethod;

import java.util.*;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestRangerClient {
    private static final RangerClient.API GET_TEST_API  = new RangerClient.API("/relative/path/test", HttpMethod.GET, Response.Status.OK);


    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void apiGet_Success() throws Exception {
        try {
            RangerRESTClient restClient = mock(RangerRESTClient.class);
            ClientResponse   response   = mock(ClientResponse.class);
            RangerClient     client     = new RangerClient(restClient);
            RangerService    service    = new RangerService("testType", "testService", "MockedService", "testTag", new HashMap<>());

            when(restClient.get(anyString(), any())).thenReturn(response);
            when(response.getStatus()).thenReturn(GET_TEST_API.getExpectedStatus().getStatusCode());
            when(response.getEntity(RangerService.class)).thenReturn(service);

            RangerService ret = client.getService(service.getName());

            Assert.assertNotNull(ret);
            Assert.assertEquals(ret.getName(), service.getName());
        } catch(RangerServiceException excp){
            Assert.fail("Not expected to fail! Found exception: " + excp);
        }
    }

    @Test
    public void apiGet_ServiceUnavailable() throws Exception {
        try {
            RangerRESTClient restClient = mock(RangerRESTClient.class);
            ClientResponse   response   = mock(ClientResponse.class);
            RangerClient     client     = new RangerClient(restClient);

            when(restClient.get(anyString(), any())).thenReturn(response);
            when(response.getStatus()).thenReturn(ClientResponse.Status.SERVICE_UNAVAILABLE.getStatusCode());

            RangerService ret = client.getService(1L);

            Assert.assertNull(ret);
        } catch(RangerServiceException excp){
            Assert.fail("Not expected to fail! Found exception: " + excp);
        }
    }

    @Test
    public void apiGet_FailWithUnexpectedStatusCode() throws Exception {
        try {
            RangerRESTClient restClient = mock(RangerRESTClient.class);
            ClientResponse   response   = mock(ClientResponse.class);
            RangerClient     client     = new RangerClient(restClient);

            when(restClient.get(anyString(), any())).thenReturn(response);
            when(response.getStatus()).thenReturn(ClientResponse.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            client.getService(1L);

            Assert.fail("supposed to fail with RangerServiceException");
        } catch(RangerServiceException excp) {
            Assert.assertTrue(excp.getMessage().contains("statusCode=" + ClientResponse.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
            Assert.assertTrue(excp.getMessage().contains("status=" + ClientResponse.Status.INTERNAL_SERVER_ERROR.getReasonPhrase()));
        }
    }

    @Test
    public void apiGet_FailWithNullResponse() throws Exception {
        try {
            RangerRESTClient restClient = mock(RangerRESTClient.class);
            RangerClient     client     = new RangerClient(restClient);

            when(restClient.get(anyString(), any())).thenReturn(null);

            client.getService(1L);

            Assert.fail("supposed to fail with RangerServiceException");
        } catch(RangerServiceException excp) {
            Assert.assertTrue(excp.getMessage().contains("statusCode=null"));
            Assert.assertTrue(excp.getMessage().contains("status=null"));
        }
    }

    @Test
    public void api_UrlMissingFormat() {
        try {
            new RangerClient.API("%dtest%dpath%d", HttpMethod.GET, Response.Status.OK).applyUrlFormat(1,1);
            Assert.fail("supposed to fail with RangerServiceException");
        } catch(RangerServiceException exp){
            Assert.assertTrue(exp.getMessage().contains("MissingFormatArgumentException"));
        }
    }

    @Test
    public void api_UrlIllegalFormatConversion() {
        try {
            new RangerClient.API("testpath%d", HttpMethod.GET, Response.Status.OK).applyUrlFormat("1");
            Assert.fail("supposed to fail with RangerServiceException");
        } catch(RangerServiceException exp){
            Assert.assertTrue(exp.getMessage().contains("IllegalFormatConversionException"));
        }

        try {
            new RangerClient.API("testpath%f", HttpMethod.GET, Response.Status.OK).applyUrlFormat(1);
            Assert.fail("supposed to fail with RangerServiceException");
        } catch(RangerServiceException exp){
            Assert.assertTrue(exp.getMessage().contains("IllegalFormatConversionException"));
        }
    }
}