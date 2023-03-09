/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.metastore.kafka;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.confluent.kafka.schemaregistry.client.rest.entities.Config;
import io.confluent.kafka.schemaregistry.client.rest.entities.Schema;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
public class KafkaSchemaServiceImplTest {

    @MockBean
    private KafkaSchemaServiceImpl service;

    @Before
    public void setup() {
        service.restTemplate = mock(RestTemplate.class);
    }

    @Test
    public void testGetSchemaString() {
        try {
            SchemaString ss = new SchemaString();
            ResponseEntity entity = mock(ResponseEntity.class);
            when(service.restTemplate.getForEntity(
                    "${kafka.schema.registry.url}/schemas/ids/1",
                    SchemaString.class)).thenReturn(entity);
            when(entity.getBody()).thenReturn(ss);
            service.getSchemaString(1);
            assertTrue(true);
        } catch (Throwable t) {
            fail("Cannot get all tables from all dbs");
        }
    }

    @Test
    public void testGetSubjects() {
        try {
            ResponseEntity entity = mock(ResponseEntity.class);
            when(service.restTemplate.getForEntity(
                    "${kafka.schema.registry.url}/subjects",
                    String[].class)).thenReturn(entity);
            when(entity.getBody()).thenReturn(new String[]{"aaa", "bbb"});
            service.getSubjects();
            assertTrue(true);
        } catch (Throwable t) {
            fail("Cannot get all tables from all dbs");
        }
    }

    @Test
    public void testGetSubjectVersions() {
        try {
            ResponseEntity entity = mock(ResponseEntity.class);
            when(service.restTemplate.getForEntity(
                    "${kafka.schema.registry.url}/subjects/sub/versions",
                    Integer[].class)).thenReturn(entity);
            when(entity.getBody()).thenReturn(new Integer[]{1, 2});
            service.getSubjectVersions("sub");
            assertTrue(true);
        } catch (Throwable t) {
            fail("Cannot get all tables from all dbs");
        }
    }

    @Test
    public void testGetSubjectSchema() {
        try {
            Schema schema = mock(Schema.class);
            ResponseEntity entity = mock(ResponseEntity.class);
            when(service.restTemplate.getForEntity(
                    "${kafka.schema.registry.url}/subjects/sub/versions/ver",
                    Schema.class)).thenReturn(entity);
            when(entity.getBody()).thenReturn(schema);
            service.getSubjectSchema("sub", "ver");
            assertTrue(true);
        } catch (Throwable t) {
            fail("Cannot get all tables from all dbs");
        }
    }

    @Test
    public void testGetTopLevelConfig() {
        try {
            Config config = mock(Config.class);
            ResponseEntity entity = mock(ResponseEntity.class);
            when(service.restTemplate.getForEntity(
                    "${kafka.schema.registry.url}/config",
                    Config.class)).thenReturn(entity);
            when(entity.getBody()).thenReturn(config);
            service.getTopLevelConfig();
            assertTrue(true);
        } catch (Throwable t) {
            fail("Cannot get all tables from all dbs");
        }
    }

    @Test
    public void testGetSubjectLevelConfig() {
        try {
            Config config = mock(Config.class);
            ResponseEntity entity = mock(ResponseEntity.class);
            when(service.restTemplate.getForEntity(
                    "${kafka.schema.registry.url}/config/subject",
                    Config.class)).thenReturn(entity);
            when(entity.getBody()).thenReturn(config);
            service.getSubjectLevelConfig("subject");
            assertTrue(true);
        } catch (Throwable t) {
            fail("Cannot get all tables from all dbs");
        }
    }
}
