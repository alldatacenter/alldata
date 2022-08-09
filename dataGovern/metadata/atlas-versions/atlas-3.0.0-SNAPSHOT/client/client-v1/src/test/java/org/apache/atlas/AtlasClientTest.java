/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.atlas.model.legacy.EntityResult;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AtlasClientTest {

    @Mock
    private WebResource service;
    @Mock
    private WebResource.Builder resourceBuilderMock;

    @Mock
    private Configuration configuration;

    @Mock
    private Client client;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldVerifyServerIsReady() throws AtlasServiceException {
        setupRetryParams();

        AtlasClient atlasClient = new AtlasClient(service, configuration);

        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.VERSION, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.getEntity(String.class)).thenReturn("{\"Version\":\"version-rrelease\",\"Name\":\"apache-atlas\"," +
                "\"Description\":\"Metadata Management and Data Governance Platform over Hadoop\"}");
        when(builder.method(AtlasClient.API_V1.VERSION.getMethod(), ClientResponse.class, null)).thenReturn(response);

        assertTrue(atlasClient.isServerReady());
    }

    @Test
    public void testCreateEntity() throws Exception {
        setupRetryParams();
        AtlasClient atlasClient = new AtlasClient(service, configuration);

        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.CREATE_ENTITY, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());

        String jsonResponse = AtlasType.toV1Json(new EntityResult(Arrays.asList("id"), null, null));
        when(response.getEntity(String.class)).thenReturn(jsonResponse.toString());
        when(response.getLength()).thenReturn(jsonResponse.length());

        String entityJson = AtlasType.toV1Json(new Referenceable("type"));
        when(builder.method(anyString(), Matchers.<Class>any(), anyString())).thenReturn(response);

        List<String> ids = atlasClient.createEntity(entityJson);
        assertEquals(ids.size(), 1);
        assertEquals(ids.get(0), "id");
    }

    private WebResource.Builder setupBuilder(AtlasClient.API_V1 api, WebResource webResource) {
        when(webResource.path(api.getPath())).thenReturn(service);
        when(webResource.path(api.getNormalizedPath())).thenReturn(service);
        return getBuilder(service);
    }

    @Test
    public void shouldReturnFalseIfServerIsNotReady() throws AtlasServiceException {
        setupRetryParams();
        AtlasClient atlasClient = new AtlasClient(service, configuration);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.VERSION, service);
        when(builder.method(AtlasClient.API_V1.VERSION.getMethod(), ClientResponse.class, null)).thenThrow(
                new ClientHandlerException());
        assertFalse(atlasClient.isServerReady());
    }

    @Test
    public void shouldReturnFalseIfServiceIsUnavailable() throws AtlasServiceException {
        setupRetryParams();
        AtlasClient atlasClient = new AtlasClient(service, configuration);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.VERSION, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        when(response.getClientResponseStatus()).thenReturn(ClientResponse.Status.SERVICE_UNAVAILABLE);

        when(builder.method(AtlasClient.API_V1.VERSION.getMethod(), ClientResponse.class, null)).thenReturn(response);

        assertFalse(atlasClient.isServerReady());
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void shouldThrowErrorIfAnyResponseOtherThanServiceUnavailable() throws AtlasServiceException {
        setupRetryParams();

        AtlasClient atlasClient = new AtlasClient(service, configuration);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.VERSION, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        when(response.getClientResponseStatus()).thenReturn(ClientResponse.Status.INTERNAL_SERVER_ERROR);

        when(builder.method(AtlasClient.API_V1.VERSION.getMethod(), ClientResponse.class, null)).thenReturn(response);

        atlasClient.isServerReady();
        fail("Should throw exception");
    }
    
    @Test
    public void shouldGetAdminStatus() throws AtlasServiceException {
        setupRetryParams();

        AtlasClient atlasClient = new AtlasClient(service, configuration);

        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"Active\"}";
        when(response.getEntity(String.class)).thenReturn(activeStatus);
        when(response.getLength()).thenReturn(activeStatus.length());
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).thenReturn(response);

//         Fix after AtlasBaseClient
//        atlasClient.setService();


        String status = atlasClient.getAdminStatus();
        assertEquals(status, "Active");
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void shouldReturnStatusAsUnknownOnException() throws AtlasServiceException {
        setupRetryParams();

        AtlasClient atlasClient = new AtlasClient(service, configuration);

        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        when(response.getClientResponseStatus()).thenReturn(ClientResponse.Status.INTERNAL_SERVER_ERROR);
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).thenReturn(response);

        String status = atlasClient.getAdminStatus();
        fail("Should fail with AtlasServiceException");
    }

    @Test
    public void shouldReturnStatusAsUnknownIfJSONIsInvalid() throws AtlasServiceException {
        setupRetryParams();
        AtlasClient atlasClient = new AtlasClient(service, configuration);

        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.getEntity(String.class)).thenReturn("{\"status\":\"Active\"}");
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).thenReturn(response);

        String status = atlasClient.getAdminStatus();
        assertEquals(status, AtlasClient.UNKNOWN_STATUS);
    }
    
    @Test
    public void shouldReturnBaseURLAsPassedInURL() {
        AtlasClient atlasClient = new AtlasClient(service, configuration);

        String serviceURL = atlasClient.determineActiveServiceURL(new String[]{"http://localhost:21000"}, client);
        assertEquals(serviceURL, "http://localhost:21000");
    }

    @Test
    public void shouldSelectActiveAmongMultipleServersIfHAIsEnabled() {
        setupRetryParams();

        when(client.resource(UriBuilder.fromUri("http://localhost:31000").build())).thenReturn(service);
        when(client.resource(UriBuilder.fromUri("http://localhost:41000").build())).thenReturn(service);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse firstResponse = mock(ClientResponse.class);
        when(firstResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String passiveStatus = "{\"Status\":\"PASSIVE\"}";
        when(firstResponse.getEntity(String.class)).thenReturn(passiveStatus);
        when(firstResponse.getLength()).thenReturn(passiveStatus.length());
        ClientResponse secondResponse = mock(ClientResponse.class);
        when(secondResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"ACTIVE\"}";
        when(secondResponse.getEntity(String.class)).thenReturn(activeStatus);
        when(secondResponse.getLength()).thenReturn(activeStatus.length());
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).
                thenReturn(firstResponse).thenReturn(firstResponse).thenReturn(firstResponse).
                thenReturn(secondResponse);

        AtlasClient atlasClient = new AtlasClient(service, configuration);

        String serviceURL = atlasClient.determineActiveServiceURL(
                new String[]{"http://localhost:31000", "http://localhost:41000"},
                client);
        assertEquals(serviceURL, "http://localhost:41000");
    }

    @Test
    public void shouldRetryUntilServiceBecomesActive() {
        setupRetryParams();

        when(client.resource(UriBuilder.fromUri("http://localhost:31000").build())).thenReturn(service);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.getEntity(String.class)).thenReturn("{\"Status\":\"BECOMING_ACTIVE\"}");
        ClientResponse nextResponse = mock(ClientResponse.class);
        when(nextResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"ACTIVE\"}";
        when(response.getEntity(String.class)).thenReturn(activeStatus);
        when(response.getLength()).thenReturn(activeStatus.length());
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).
                thenReturn(response).thenReturn(response).thenReturn(nextResponse);

        AtlasClient atlasClient = new AtlasClient(service, configuration);

        String serviceURL = atlasClient.determineActiveServiceURL(
                new String[] {"http://localhost:31000","http://localhost:41000"},
                client);
        assertEquals(serviceURL, "http://localhost:31000");
    }

    @Test
    public void shouldRetryIfCannotConnectToServiceInitially() {
        setupRetryParams();

        when(client.resource(UriBuilder.fromUri("http://localhost:31000").build())).thenReturn(service);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.getEntity(String.class)).thenReturn("{\"Status\":\"BECOMING_ACTIVE\"}");
        ClientResponse nextResponse = mock(ClientResponse.class);
        when(nextResponse.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"ACTIVE\"}";
        when(response.getEntity(String.class)).thenReturn(activeStatus);
        when(response.getLength()).thenReturn(activeStatus.length());
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).
                thenThrow(new ClientHandlerException("Simulating connection exception")).
                thenReturn(response).
                thenReturn(nextResponse);

        AtlasClient atlasClient = new AtlasClient(service, configuration);
        atlasClient.setService(service);
        atlasClient.setConfiguration(configuration);

        String serviceURL = atlasClient.determineActiveServiceURL(
                new String[] {"http://localhost:31000","http://localhost:41000"},
                client);
        assertEquals(serviceURL, "http://localhost:31000");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowExceptionIfActiveServerIsNotFound() {
        setupRetryParams();

        when(client.resource(UriBuilder.fromUri("http://localhost:31000").build())).thenReturn(service);
        WebResource.Builder builder = setupBuilder(AtlasClient.API_V1.STATUS, service);
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.getEntity(String.class)).thenReturn("{\"Status\":\"BECOMING_ACTIVE\"}");
        when(builder.method(AtlasClient.API_V1.STATUS.getMethod(), ClientResponse.class, null)).
                thenThrow(new ClientHandlerException("Simulating connection exception")).
                thenReturn(response).
                thenReturn(response);

        AtlasClient atlasClient = new AtlasClient(service, configuration);

        String serviceURL = atlasClient.determineActiveServiceURL(
                new String[] {"http://localhost:31000","http://localhost:41000"},
                client);
        assertNull(serviceURL);
    }

    @Test
    public void shouldRetryAPICallsOnClientHandlerException() throws AtlasServiceException, URISyntaxException {
        setupRetryParams();

        ResourceCreator resourceCreator = mock(ResourceCreator.class);
        WebResource resourceObject = mock(WebResource.class);
        when(resourceObject.getURI()).
                thenReturn(new URI("http://localhost:31000/api/atlas/types")).
                thenReturn(new URI("http://localhost:41000/api/atlas/types")).
                thenReturn(new URI("http://localhost:41000/api/atlas/types"));

        WebResource.Builder builder = getBuilder(resourceObject);

        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"ACTIVE\"}";
        when(response.getEntity(String.class)).thenReturn(activeStatus);
        when(response.getLength()).thenReturn(activeStatus.length());

        when(builder.method(AtlasClient.API_V1.LIST_TYPES.getMethod(), ClientResponse.class, null)).
                thenThrow(new ClientHandlerException("simulating exception in calling API", new ConnectException())).
                thenReturn(response);

        when(resourceCreator.createResource()).thenReturn(resourceObject);

        AtlasClient atlasClient = getClientForTest("http://localhost:31000","http://localhost:41000");

        atlasClient.setService(service);
        atlasClient.setConfiguration(configuration);

        atlasClient.callAPIWithRetries(AtlasClient.API_V1.LIST_TYPES, null, resourceCreator);

        verify(client).destroy();
        verify(client).resource(UriBuilder.fromUri("http://localhost:31000").build());
        verify(client).resource(UriBuilder.fromUri("http://localhost:41000").build());
    }

    @Test
    public void shouldRetryWithSameClientIfSingleAddressIsUsed() throws URISyntaxException, AtlasServiceException {
        setupRetryParams();

        ResourceCreator resourceCreator = mock(ResourceCreator.class);
        WebResource resourceObject = mock(WebResource.class);
        when(resourceObject.getURI()).
                thenReturn(new URI("http://localhost:31000/api/atlas/types"));

        WebResource.Builder builder = getBuilder(resourceObject);

        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"ACTIVE\"}";
        when(response.getEntity(String.class)).thenReturn(activeStatus);
        when(response.getLength()).thenReturn(activeStatus.length());

        when(builder.method(AtlasClient.API_V1.LIST_TYPES.getMethod(), ClientResponse.class, null)).
                thenThrow(new ClientHandlerException("simulating exception in calling API", new ConnectException())).
                thenReturn(response);

        when(resourceCreator.createResource()).thenReturn(resourceObject);
        when(configuration.getString("atlas.http.authentication.type", "simple")).thenReturn("simple");

        AtlasClient atlasClient = getClientForTest("http://localhost:31000");

        atlasClient.setService(resourceObject);
        atlasClient.setConfiguration(configuration);

        atlasClient.callAPIWithRetries(AtlasClient.API_V1.LIST_TYPES, null, resourceCreator);

        verify(client).destroy();
        verify(client, times(2)).resource(UriBuilder.fromUri("http://localhost:31000").build());
    }

    @Test
    public void shouldRetryAPICallsOnServiceUnavailable() throws AtlasServiceException, URISyntaxException {
        setupRetryParams();

        ResourceCreator resourceCreator = mock(ResourceCreator.class);
        WebResource resourceObject = mock(WebResource.class);
        when(resourceObject.getURI()).
                thenReturn(new URI("http://localhost:31000/api/atlas/types")).
                thenReturn(new URI("http://localhost:41000/api/atlas/types")).
                thenReturn(new URI("http://localhost:41000/api/atlas/types"));

        WebResource.Builder builder = getBuilder(resourceObject);

        ClientResponse firstResponse = mock(ClientResponse.class);
        when(firstResponse.getStatus()).thenReturn(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        when(firstResponse.getClientResponseStatus()).thenReturn(ClientResponse.Status.SERVICE_UNAVAILABLE);

        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        String activeStatus = "{\"Status\":\"ACTIVE\"}";
        when(response.getEntity(String.class)).thenReturn(activeStatus);
        when(response.getLength()).thenReturn(activeStatus.length());

        when(builder.method(AtlasClient.API_V1.LIST_TYPES.getMethod(), ClientResponse.class, null)).
                thenThrow(new ClientHandlerException("simulating exception in calling API", new ConnectException())).
                thenReturn(firstResponse).
                thenReturn(response);

        when(resourceCreator.createResource()).thenReturn(resourceObject);

        AtlasClient atlasClient = getClientForTest("http://localhost:31000","http://localhost:41000");
        atlasClient.setService(resourceObject);
        atlasClient.setConfiguration(configuration);

        atlasClient.callAPIWithRetries(AtlasClient.API_V1.LIST_TYPES, null, resourceCreator);


        verify(client).destroy();
        verify(client).resource(UriBuilder.fromUri("http://localhost:31000").build());
        verify(client).resource(UriBuilder.fromUri("http://localhost:41000").build());
    }

    private WebResource.Builder getBuilder(WebResource resourceObject) {
        when(resourceObject.getRequestBuilder()).thenReturn(resourceBuilderMock);
        when(resourceObject.path(anyString())).thenReturn(resourceObject);
        when(resourceBuilderMock.accept(AtlasBaseClient.JSON_MEDIA_TYPE)).thenReturn(resourceBuilderMock);
        when(resourceBuilderMock.accept(MediaType.APPLICATION_JSON)).thenReturn(resourceBuilderMock);
        when(resourceBuilderMock.type(AtlasBaseClient.JSON_MEDIA_TYPE)).thenReturn(resourceBuilderMock);
        when(resourceBuilderMock.type(MediaType.MULTIPART_FORM_DATA)).thenReturn(resourceBuilderMock);
        return resourceBuilderMock;
    }

    private void setupRetryParams() {
        when(configuration.getInt(AtlasClient.ATLAS_CLIENT_HA_RETRIES_KEY, AtlasClient.DEFAULT_NUM_RETRIES)).
                thenReturn(3);
        when(configuration.getInt(AtlasClient.ATLAS_CLIENT_HA_SLEEP_INTERVAL_MS_KEY,
                AtlasClient.DEFAULT_SLEEP_BETWEEN_RETRIES_MS)).
                thenReturn(1);
    }

    private AtlasClient getClientForTest(final String... baseUrls) {
        return new AtlasClient((UserGroupInformation)null, (String)null, baseUrls) {
            boolean firstCall = true;
            @Override
            protected String determineActiveServiceURL(String[] baseUrls, Client client) {
                String returnUrl = baseUrls[0];
                if (baseUrls.length > 1 && !firstCall) {
                    returnUrl = baseUrls[1];
                }
                firstCall = false;
                return returnUrl;
            }

            @Override
            protected Configuration getClientProperties() {
                return configuration;
            }

            @Override
            protected Client getClient(Configuration configuration, UserGroupInformation ugi, String doAsUser) {
                return client;
            }
        };
    }
}
