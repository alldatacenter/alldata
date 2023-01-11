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
package org.apache.ranger.services.nifi.client;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.when;

public class TestNiFiClient {

    private static final String RESOURCES_RESPONSE = "{\n" +
            "  \"revision\": {\n" +
            "    \"clientId\": \"0daac173-025c-4aa7-b644-97f7b10435d2\"\n" +
            "  },\n" +
            "  \"resources\": [\n" +
            "    {\n" +
            "      \"identifier\": \"/system\",\n" +
            "      \"name\": \"System\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"identifier\": \"/controller\",\n" +
            "      \"name\": \"Controller\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"identifier\": \"/flow\",\n" +
            "      \"name\": \"NiFi Flow\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"identifier\": \"/provenance\",\n" +
            "      \"name\": \"Provenance\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"identifier\": \"/proxy\",\n" +
            "      \"name\": \"Proxy User Requests\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"identifier\": \"/resources\",\n" +
            "      \"name\": \"NiFi Resources\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private NiFiClient niFiClient;

    @Before
    public void setup() {
        niFiClient = new MockNiFiClient(RESOURCES_RESPONSE, 200);
    }

    @Test
    public void testGetResourcesNoUserInput() throws Exception {
        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("");

        final List<String> expectedResources = new ArrayList<>();
        expectedResources.add("/system");
        expectedResources.add("/controller");
        expectedResources.add("/flow");
        expectedResources.add("/provenance");
        expectedResources.add("/proxy");
        expectedResources.add("/resources");

        List<String> resources = niFiClient.getResources(resourceLookupContext);
        Assert.assertNotNull(resources);
        Assert.assertEquals(expectedResources.size(), resources.size());

        resources.removeAll(expectedResources);
        Assert.assertEquals(0, resources.size());
    }

    @Test
    public void testGetResourcesWithUserInputBeginning() throws Exception {
        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("/pr");

        final List<String> expectedResources = new ArrayList<>();
        expectedResources.add("/provenance");
        expectedResources.add("/proxy");

        List<String> resources = niFiClient.getResources(resourceLookupContext);
        Assert.assertNotNull(resources);
        Assert.assertEquals(expectedResources.size(), resources.size());

        resources.removeAll(expectedResources);
        Assert.assertEquals(0, resources.size());
    }

    @Test
    public void testGetResourcesWithUserInputAnywhere() throws Exception {
        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("trol");

        final List<String> expectedResources = new ArrayList<>();
        expectedResources.add("/controller");

        List<String> resources = niFiClient.getResources(resourceLookupContext);
        Assert.assertNotNull(resources);
        Assert.assertEquals(expectedResources.size(), resources.size());

        resources.removeAll(expectedResources);
        Assert.assertEquals(0, resources.size());
    }

    @Test
    public void testGetResourcesErrorResponse() throws Exception {
        final String errorMsg = "unknown error";
        niFiClient = new MockNiFiClient(errorMsg, Response.Status.BAD_REQUEST.getStatusCode());

        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("");

        try {
            niFiClient.getResources(resourceLookupContext);
            Assert.fail("should have thrown exception");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(errorMsg));
        }
    }

    @Test
    public void testConnectionTestSuccess() {
        HashMap<String, Object> ret = niFiClient.connectionTest();
        Assert.assertNotNull(ret);
        Assert.assertEquals(NiFiClient.SUCCESS_MSG, ret.get("message"));
    }

    @Test
    public void testConnectionTestFailure() {
        final String errorMsg = "unknown error";
        niFiClient = new MockNiFiClient(errorMsg, Response.Status.BAD_REQUEST.getStatusCode());

        HashMap<String, Object> ret = niFiClient.connectionTest();
        Assert.assertNotNull(ret);
        Assert.assertEquals(NiFiClient.FAILURE_MSG, ret.get("message"));
    }


    /**
     * Extend NiFiClient to return mock responses.
     */
    private static final class MockNiFiClient extends NiFiClient {

        private int statusCode;
        private String responseEntity;

        public MockNiFiClient(String responseEntity, int statusCode) {
            super("http://localhost:8080/nifi-api/resources", null);
            this.statusCode = statusCode;
            this.responseEntity = responseEntity;
        }

        @Override
        protected WebResource getWebResource() {
            return Mockito.mock(WebResource.class);
        }

        @Override
        protected ClientResponse getResponse(WebResource resource, String accept) {
            ClientResponse response = Mockito.mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(statusCode);
            when(response.getEntityInputStream()).thenReturn(new ByteArrayInputStream(
                    responseEntity.getBytes(StandardCharsets.UTF_8)
            ));
            return response;
        }
    }
}
