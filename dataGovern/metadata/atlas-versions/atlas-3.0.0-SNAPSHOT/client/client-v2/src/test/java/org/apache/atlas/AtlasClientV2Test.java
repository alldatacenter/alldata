/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;


import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.commons.configuration.Configuration;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AtlasClientV2Test {
    @Mock
    private WebResource service;

    @Mock
    private WebResource.Builder resourceBuilderMock;

    @Mock
    private Configuration configuration;


    @BeforeMethod
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private WebResource.Builder setupBuilder(AtlasClientV2.API_V2 api, WebResource webResource) {
        when(webResource.path(api.getPath())).thenReturn(service);
        when(webResource.path(api.getNormalizedPath())).thenReturn(service);
        return getBuilder(service);
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


    @Test
    public void updateClassificationsShouldNotThrowExceptionIfResponseIs204() {

        AtlasClientV2 atlasClient = new AtlasClientV2(service, configuration);


        AtlasClassification atlasClassification = new AtlasClassification("Testdb");
        atlasClassification.setEntityGuid("abb672b1-e4bd-402d-a98f-73cd8f775e2a");
        WebResource.Builder builder = setupBuilder(AtlasClientV2.API_V2.UPDATE_CLASSIFICATIONS, service);


        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.NO_CONTENT.getStatusCode());


        when(builder.method(anyString(), Matchers.<Class>any(), anyString())).thenReturn(response);

        try {
            atlasClient.updateClassifications("abb672b1-e4bd-402d-a98f-73cd8f775e2a", Collections.singletonList(atlasClassification));

        } catch (AtlasServiceException e) {
            Assert.fail("Failed with Exception");
        }

    }


    @Test
    public void updateClassificationsShouldThrowExceptionIfResponseIsNot204() {

        AtlasClientV2 atlasClient = new AtlasClientV2(service, configuration);


        AtlasClassification atlasClassification = new AtlasClassification("Testdb");
        atlasClassification.setEntityGuid("abb672b1-e4bd-402d-a98f-73cd8f775e2a");
        WebResource.Builder builder = setupBuilder(AtlasClientV2.API_V2.UPDATE_CLASSIFICATIONS, service);


        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());


        when(builder.method(anyString(), Matchers.<Class>any(), anyString())).thenReturn(response);

        try {
            atlasClient.updateClassifications("abb672b1-e4bd-402d-a98f-73cd8f775e2a", Collections.singletonList(atlasClassification));
            Assert.fail("Failed with Exception");
        } catch (AtlasServiceException e) {
            Assert.assertTrue(e.getMessage().contains(" failed with status 200 "));
        }

    }

    @Test
    public void restRequestCheck() {
        AtlasClientV2 atlasClient = new AtlasClientV2(service, configuration);
        String pathForRelationshipTypeDef           = atlasClient.getPathForType(AtlasRelationshipDef.class);
        Assert.assertEquals("relationshipdef", pathForRelationshipTypeDef);
        String pathForStructTypeDef                 = atlasClient.getPathForType(AtlasStructDef.class);
        Assert.assertEquals("structdef", pathForStructTypeDef);
        String pathForBusinessMetadataTypeDef       = atlasClient.getPathForType(AtlasBusinessMetadataDef.class);
        Assert.assertEquals("businessmetadatadef", pathForBusinessMetadataTypeDef);
        String pathForEnumTypeDef                   = atlasClient.getPathForType(AtlasEnumDef.class);
        Assert.assertEquals("enumdef", pathForEnumTypeDef);
        String pathForClassificationTypeDef         = atlasClient.getPathForType(AtlasClassificationDef.class);
        Assert.assertEquals("classificationdef", pathForClassificationTypeDef);
        String pathForEntityTypeDef                 = atlasClient.getPathForType(AtlasEntityDef.class);
        Assert.assertEquals("entitydef", pathForEntityTypeDef);
    }
}

