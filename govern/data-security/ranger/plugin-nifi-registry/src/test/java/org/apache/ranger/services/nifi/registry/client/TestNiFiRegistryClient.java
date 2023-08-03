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
package org.apache.ranger.services.nifi.registry.client;

import com.google.common.io.Resources;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.when;

public class TestNiFiRegistryClient {

    private NiFiRegistryClient registryClient;

    @Before
    public void setup() throws IOException {
        final URL responseFile = TestNiFiRegistryClient.class.getResource("/resources-response.json");
        final String resourcesResponse = Resources.toString(responseFile, StandardCharsets.UTF_8);
        registryClient = new MockNiFiRegistryClient(resourcesResponse, 200);
    }

    @Test
    public void testGetResourcesNoUserInput() throws Exception {
        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("");

        final List<String> expectedResources = new ArrayList<>();
        expectedResources.add("/policies");
        expectedResources.add("/tenants");
        expectedResources.add("/proxy");
        expectedResources.add("/actuator");
        expectedResources.add("/swagger");
        expectedResources.add("/buckets");
        expectedResources.add("/buckets/fc0625e4-a9ae-4277-bab7-a2bc984f6c4f");
        expectedResources.add("/buckets/0b5edba5-da83-4839-b64a-adf5f21abaf4");

        List<String> resources = registryClient.getResources(resourceLookupContext);
        Assert.assertNotNull(resources);
        Assert.assertEquals(expectedResources.size(), resources.size());

        Assert.assertTrue(resources.containsAll(expectedResources));
    }

    @Test
    public void testGetResourcesWithUserInputBeginning() throws Exception {
        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("/p");

        final List<String> expectedResources = new ArrayList<>();
        expectedResources.add("/policies");
        expectedResources.add("/proxy");

        List<String> resources = registryClient.getResources(resourceLookupContext);
        Assert.assertNotNull(resources);
        Assert.assertEquals(expectedResources.size(), resources.size());

        Assert.assertTrue(resources.containsAll(expectedResources));
    }

    @Test
    public void testGetResourcesWithUserInputAnywhere() throws Exception {
        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("ant");

        final List<String> expectedResources = new ArrayList<>();
        expectedResources.add("/tenants");

        List<String> resources = registryClient.getResources(resourceLookupContext);
        Assert.assertNotNull(resources);
        Assert.assertEquals(expectedResources.size(), resources.size());

        Assert.assertTrue(resources.containsAll(expectedResources));
    }

    @Test
    public void testGetResourcesErrorResponse() {
        final String errorMsg = "unknown error";
        registryClient = new MockNiFiRegistryClient(errorMsg, Response.Status.BAD_REQUEST.getStatusCode());

        ResourceLookupContext resourceLookupContext = Mockito.mock(ResourceLookupContext.class);
        when(resourceLookupContext.getUserInput()).thenReturn("");

        try {
            registryClient.getResources(resourceLookupContext);
            Assert.fail("should have thrown exception");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(errorMsg));
        }
    }

    @Test
    public void testConnectionTestSuccess() {
        HashMap<String, Object> ret = registryClient.connectionTest();
        Assert.assertNotNull(ret);
        Assert.assertEquals(NiFiRegistryClient.SUCCESS_MSG, ret.get("message"));
    }

    @Test
    public void testConnectionTestFailure() {
        final String errorMsg = "unknown error";
        registryClient = new MockNiFiRegistryClient(errorMsg, Response.Status.BAD_REQUEST.getStatusCode());

        HashMap<String, Object> ret = registryClient.connectionTest();
        Assert.assertNotNull(ret);
        Assert.assertEquals(NiFiRegistryClient.FAILURE_MSG, ret.get("message"));
    }


    /**
     * Extend NiFiRegistryClient to return mock responses.
     */
    private static final class MockNiFiRegistryClient extends NiFiRegistryClient {

        private int statusCode;
        private String responseEntity;

        private MockNiFiRegistryClient(String responseEntity, int statusCode) {
            super("http://localhost:18080/nifi-registry-api/policiesresources", null);
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
