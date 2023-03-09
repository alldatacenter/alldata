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

package org.apache.griffin.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.griffin.core.job.entity.JobHealth;
import org.junit.Test;

public class JsonUtilTest {

    public static final String JOB_HEALTH_JSON = "{\"healthyJobCount\":5,\"jobCount\":10}";

    @Test
    public void testToJson() throws JsonProcessingException {
        JobHealth jobHealth = new JobHealth(5, 10);
        String jobHealthStr = JsonUtil.toJson(jobHealth);
        assertEquals(jobHealthStr, JOB_HEALTH_JSON);
    }

    @Test
    public void testToJsonWithFormat() throws JsonProcessingException {
        JobHealth jobHealth = new JobHealth(5, 10);
        String jobHealthStr = JsonUtil.toJsonWithFormat(jobHealth);
        assertNotEquals(jobHealthStr, JOB_HEALTH_JSON);
    }

    @Test
    public void testToEntityWithParamClass() throws IOException {
        JobHealth jobHealth = JsonUtil.toEntity(JOB_HEALTH_JSON,
                JobHealth.class);
        assertEquals(jobHealth.getJobCount(), 10);
        assertEquals(jobHealth.getHealthyJobCount(), 5);
    }

    @Test
    public void testToEntityWithNullParamClass() throws IOException {
        String str = null;
        JobHealth jobHealth = JsonUtil.toEntity(str, JobHealth.class);
        assertNull(jobHealth);
    }

    @Test
    public void testToEntityWithParamTypeReference() throws IOException {
        TypeReference<HashMap<String, Integer>> type =
                new TypeReference<HashMap<String, Integer>>() {
                };
        Map map = JsonUtil.toEntity(JOB_HEALTH_JSON, type);
        assertEquals(map.get("jobCount"), 10);
    }

    @Test
    public void testToEntityWithNullParamTypeReference() throws IOException {
        String str = null;
        TypeReference<HashMap<String, Integer>> type =
                new TypeReference<HashMap<String, Integer>>() {
                };
        Map map = JsonUtil.toEntity(str, type);
        assertNull(map);
    }
}
