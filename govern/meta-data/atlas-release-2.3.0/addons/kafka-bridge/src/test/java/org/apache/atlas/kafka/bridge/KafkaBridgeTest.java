/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.kafka.bridge;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.kafka.model.KafkaDataTypes;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.utils.KafkaUtils;
import org.apache.avro.Schema;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class KafkaBridgeTest {

    private static final String TEST_TOPIC_NAME = "test_topic";
    private static final String CLUSTER_NAME = "primary";
    private static final String TOPIC_QUALIFIED_NAME = KafkaBridge.getTopicQualifiedName(CLUSTER_NAME, TEST_TOPIC_NAME);
    private static final String TEST_SCHEMA_NAME = "test_topic-value";
    private static final int TEST_SCHEMA_VERSION = 1;
    private static final String TEST_NAMESPACE = "test_namespace";
    private static final ArrayList<Integer> TEST_SCHEMA_VERSION_LIST = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
    private static final String TEST_SCHEMA = "{\"name\":\"test\",\"namespace\":\"testing\",\"type\":\"record\",\"fields\":[{\"name\":\"Field1\",\"type\":\"string\"},{\"name\":\"Field2\",\"type\":\"int\"}]}";
    private static final Schema.Field TEST_FIELD_NAME = new Schema.Parser().parse(TEST_SCHEMA).getField("Field1");
    private static final String TEST_FIELD_FULLNAME = "Field1";
    public static final AtlasEntity.AtlasEntityWithExtInfo TOPIC_WITH_EXT_INFO = new AtlasEntity.AtlasEntityWithExtInfo(
            getTopicEntityWithGuid("0dd466a4-3838-4537-8969-6abb8b9e9185"));
    public static final AtlasEntity.AtlasEntityWithExtInfo SCHEMA_WITH_EXT_INFO = new AtlasEntity.AtlasEntityWithExtInfo(
            getSchemaEntityWithGuid("2a9894bb-e535-4aa1-a00b-a7d21ac20738"));
    public static final AtlasEntity.AtlasEntityWithExtInfo FIELD_WITH_EXT_INFO = new AtlasEntity.AtlasEntityWithExtInfo(
            getFieldEntityWithGuid("41d1011f-d428-4f5a-9578-0a0a0439147f"));

    @BeforeMethod
    public void initializeMocks() {
        MockitoAnnotations.initMocks(this);
    }

    private static AtlasEntity getTopicEntityWithGuid(String guid) {
        AtlasEntity ret = new AtlasEntity(KafkaDataTypes.KAFKA_TOPIC.getName());
        ret.setGuid(guid);
        return ret;
    }

    private static AtlasEntity getSchemaEntityWithGuid(String guid) {
        AtlasEntity ret = new AtlasEntity(KafkaDataTypes.AVRO_SCHEMA.getName());
        ret.setGuid(guid);
        return ret;
    }

    private static AtlasEntity getFieldEntityWithGuid(String guid) {
        AtlasEntity ret = new AtlasEntity(KafkaDataTypes.AVRO_FIELD.getName());
        ret.setGuid(guid);
        return ret;
    }

    @Test
    public void testImportTopic() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockCreateResponse = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeader = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeader.getGuid()).thenReturn(TOPIC_WITH_EXT_INFO.getEntity().getGuid());
        when(mockCreateResponse.getCreatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeader));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.createEntity(any()))
                .thenReturn(mockCreateResponse);
        when(mockAtlasClientV2.getEntityByGuid(TOPIC_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(TOPIC_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        bridge.importTopic(TEST_TOPIC_NAME);

        ArgumentCaptor<AtlasEntity.AtlasEntityWithExtInfo> argumentCaptor = ArgumentCaptor.forClass(AtlasEntity.AtlasEntityWithExtInfo.class);
        verify(mockAtlasClientV2).createEntity(argumentCaptor.capture());
        AtlasEntity.AtlasEntityWithExtInfo entity = argumentCaptor.getValue();
        assertEquals(entity.getEntity().getAttribute("qualifiedName"), TOPIC_QUALIFIED_NAME);
    }

    @Test
    public void testCreateTopic() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockCreateResponse = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeader = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeader.getGuid()).thenReturn(TOPIC_WITH_EXT_INFO.getEntity().getGuid());
        when(mockCreateResponse.getCreatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeader));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.createEntity(any()))
                .thenReturn(mockCreateResponse);
        when(mockAtlasClientV2.getEntityByGuid(TOPIC_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(TOPIC_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        AtlasEntity.AtlasEntityWithExtInfo ret = bridge.createOrUpdateTopic(TEST_TOPIC_NAME);

        assertEquals(TOPIC_WITH_EXT_INFO, ret);
    }

    @Test
    public void testCreateSchema() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockCreateResponse = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeader = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeader.getGuid()).thenReturn(SCHEMA_WITH_EXT_INFO.getEntity().getGuid());
        when(mockCreateResponse.getCreatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeader));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.createEntity(any()))
                .thenReturn(mockCreateResponse);
        when(mockAtlasClientV2.getEntityByGuid(SCHEMA_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(SCHEMA_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        AtlasEntity.AtlasEntityWithExtInfo ret = bridge.createOrUpdateSchema(TEST_SCHEMA, TEST_SCHEMA_NAME, TEST_NAMESPACE, TEST_SCHEMA_VERSION);

        assertEquals(SCHEMA_WITH_EXT_INFO, ret);
    }

    @Test
    public void testCreateField() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockCreateResponse = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeader = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeader.getGuid()).thenReturn(FIELD_WITH_EXT_INFO.getEntity().getGuid());
        when(mockCreateResponse.getCreatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeader));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.createEntity(any()))
                .thenReturn(mockCreateResponse);
        when(mockAtlasClientV2.getEntityByGuid(FIELD_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(FIELD_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        AtlasEntity.AtlasEntityWithExtInfo ret = bridge.createOrUpdateField(TEST_FIELD_NAME, TEST_SCHEMA_NAME, TEST_NAMESPACE, TEST_SCHEMA_VERSION, TEST_FIELD_FULLNAME);

        assertEquals(FIELD_WITH_EXT_INFO, ret);
    }

    @Test
    public void testUpdateTopic() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockUpdateResponse = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeader = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeader.getGuid()).thenReturn(TOPIC_WITH_EXT_INFO.getEntity().getGuid());
        when(mockUpdateResponse.getUpdatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeader));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.getEntityByAttribute(eq(KafkaDataTypes.KAFKA_TOPIC.getName()), any()))
                .thenReturn(TOPIC_WITH_EXT_INFO);
        when(mockAtlasClientV2.updateEntity(any()))
                .thenReturn(mockUpdateResponse);
        when(mockAtlasClientV2.getEntityByGuid(TOPIC_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(TOPIC_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        AtlasEntity.AtlasEntityWithExtInfo ret = bridge.createOrUpdateTopic(TEST_TOPIC_NAME);

        assertEquals(TOPIC_WITH_EXT_INFO, ret);
    }

    @Test
    public void testUpdateSchema() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockUpdateResponseSchema = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeaderSchema = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeaderSchema.getGuid()).thenReturn(SCHEMA_WITH_EXT_INFO.getEntity().getGuid());
        when(mockUpdateResponseSchema.getUpdatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeaderSchema));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.getEntityByAttribute(eq(KafkaDataTypes.AVRO_SCHEMA.getName()), any()))
                .thenReturn(SCHEMA_WITH_EXT_INFO);
        when(mockAtlasClientV2.updateEntity(SCHEMA_WITH_EXT_INFO))
                .thenReturn(mockUpdateResponseSchema);
        when(mockAtlasClientV2.getEntityByGuid(SCHEMA_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(SCHEMA_WITH_EXT_INFO);

        EntityMutationResponse mockUpdateResponseField = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeaderField = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeaderField.getGuid()).thenReturn(FIELD_WITH_EXT_INFO.getEntity().getGuid());
        when(mockUpdateResponseField.getUpdatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeaderField));

        when(mockAtlasClientV2.getEntityByAttribute(eq(KafkaDataTypes.AVRO_FIELD.getName()), any()))
                .thenReturn(FIELD_WITH_EXT_INFO);
        when(mockAtlasClientV2.updateEntity(FIELD_WITH_EXT_INFO))
                .thenReturn(mockUpdateResponseField);
        when(mockAtlasClientV2.getEntityByGuid(FIELD_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(FIELD_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        AtlasEntity.AtlasEntityWithExtInfo ret = bridge.createOrUpdateSchema(TEST_SCHEMA, TEST_SCHEMA_NAME, TEST_NAMESPACE, TEST_SCHEMA_VERSION);

        System.out.println(SCHEMA_WITH_EXT_INFO);
        System.out.println(ret);
        assertEquals(SCHEMA_WITH_EXT_INFO, ret);
    }

    @Test
    public void testUpdateField() throws Exception {
        KafkaUtils mockKafkaUtils = mock(KafkaUtils.class);
        when(mockKafkaUtils.listAllTopics())
                .thenReturn(Collections.singletonList(TEST_TOPIC_NAME));
        when(mockKafkaUtils.getPartitionCount(TEST_TOPIC_NAME))
                .thenReturn(3);

        EntityMutationResponse mockUpdateResponse = mock(EntityMutationResponse.class);
        AtlasEntityHeader mockAtlasEntityHeader = mock(AtlasEntityHeader.class);
        when(mockAtlasEntityHeader.getGuid()).thenReturn(FIELD_WITH_EXT_INFO.getEntity().getGuid());
        when(mockUpdateResponse.getUpdatedEntities())
                .thenReturn(Collections.singletonList(mockAtlasEntityHeader));

        AtlasClientV2 mockAtlasClientV2 = mock(AtlasClientV2.class);
        when(mockAtlasClientV2.getEntityByAttribute(eq(KafkaDataTypes.AVRO_FIELD.getName()), any()))
                .thenReturn(FIELD_WITH_EXT_INFO);
        when(mockAtlasClientV2.updateEntity(any()))
                .thenReturn(mockUpdateResponse);
        when(mockAtlasClientV2.getEntityByGuid(FIELD_WITH_EXT_INFO.getEntity().getGuid()))
                .thenReturn(FIELD_WITH_EXT_INFO);

        KafkaBridge bridge = new KafkaBridge(ApplicationProperties.get(), mockAtlasClientV2, mockKafkaUtils);
        AtlasEntity.AtlasEntityWithExtInfo ret = bridge.createOrUpdateField(TEST_FIELD_NAME, TEST_SCHEMA_NAME, TEST_NAMESPACE, TEST_SCHEMA_VERSION, TEST_FIELD_FULLNAME);

        assertEquals(FIELD_WITH_EXT_INFO, ret);
    }

    @Test
    public void testGetSchemas() throws Exception {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        when(mockResponse.getStatusLine())
                .thenReturn(mock(StatusLine.class));
        when(mockResponse.getStatusLine().getStatusCode())
                .thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getEntity())
                .thenReturn(mock(HttpEntity.class));
        when(mockResponse.getEntity().getContent())
                .thenReturn(new ByteArrayInputStream(new String("{\"subject\":\"test-value\",\"version\":1,\"id\":1,\"schema\":"+ TEST_SCHEMA +"}").getBytes(StandardCharsets.UTF_8)));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        when(mockHttpClient.execute(any()))
                .thenReturn(mockResponse);
        when(mockHttpClient.getConnectionManager())
                .thenReturn(mock(ClientConnectionManager.class));

        String ret = SchemaRegistryConnector.getSchemaFromKafkaSchemaRegistry(mockHttpClient, TEST_SCHEMA_NAME,TEST_SCHEMA_VERSION);

        assertEquals(TEST_SCHEMA, ret);
    }

    @Test
    public void testGetSchemaVersions() throws Exception {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        when(mockResponse.getStatusLine())
                .thenReturn(mock(StatusLine.class));
        when(mockResponse.getStatusLine().getStatusCode())
                .thenReturn(HttpStatus.SC_OK);
        when(mockResponse.getEntity())
                .thenReturn(mock(HttpEntity.class));
        when(mockResponse.getEntity().getContent())
                .thenReturn(new ByteArrayInputStream(new String(TEST_SCHEMA_VERSION_LIST.toString()).getBytes(StandardCharsets.UTF_8)));

        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        when(mockHttpClient.execute(any()))
                .thenReturn(mockResponse);
        when(mockHttpClient.getConnectionManager())
                .thenReturn(mock(ClientConnectionManager.class));

        ArrayList<Integer> ret = SchemaRegistryConnector.getVersionsKafkaSchemaRegistry(mockHttpClient, TEST_SCHEMA_NAME);

        assertEquals(TEST_SCHEMA_VERSION_LIST, ret);
    }

}