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

package org.apache.ranger.plugin.contextenricher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.ranger.plugin.contextenricher.TestTagEnricher.TagEnricherTestCase.TestData;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.policyengine.*;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.ServiceTags;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTagEnricher {
    static Gson gsonBuilder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
                .setPrettyPrinting()
                .registerTypeAdapter(RangerAccessResource.class, new RangerResourceDeserializer())
                .create();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testTagEnricher_hive() {
        String[] hiveTestResourceFiles = { "/contextenricher/test_tagenricher_hive.json" };

        runTestsFromResourceFiles(hiveTestResourceFiles);
    }

    private void runTestsFromResourceFiles(String[] resourceNames) {
        for(String resourceName : resourceNames) {
            InputStream       inStream = this.getClass().getResourceAsStream(resourceName);
            InputStreamReader reader   = new InputStreamReader(inStream);

            runTests(reader, resourceName);
        }
    }

    private void runTests(InputStreamReader reader, String testName) {
        TagEnricherTestCase testCase = gsonBuilder.fromJson(reader, TagEnricherTestCase.class);

        assertTrue("invalid input: " + testName, testCase != null && testCase.serviceDef != null && testCase.serviceResources != null && testCase.tests != null);

        ServiceTags serviceTags = new ServiceTags();
        serviceTags.setServiceName(testCase.serviceName);
        serviceTags.setTagDefinitions(testCase.tagDefinitions);
        serviceTags.setTags(testCase.tags);
        serviceTags.setServiceResources(testCase.serviceResources);
        serviceTags.setResourceToTagIds(testCase.resourceToTagIds);

        RangerTagEnricher tagEnricher = new RangerTagEnricher();

        tagEnricher.setServiceName(testCase.serviceName);
        tagEnricher.setServiceDef(testCase.serviceDef);
        tagEnricher.init();
        tagEnricher.setServiceTags(serviceTags);

        List<String> expectedTags = new ArrayList<>();
        List<String> resultTags   = new ArrayList<>();

        for (TestData test : testCase.tests) {
            RangerAccessRequestImpl request = new RangerAccessRequestImpl(test.resource, test.accessType, "testUser", null, null);

            tagEnricher.enrich(request);

            List<RangerTag> expected = test.result;

            Set<RangerTagForEval> result   = RangerAccessRequestUtil.getRequestTagsFromContext(request.getContext());

            expectedTags.clear();
            if(expected != null) {
                for (RangerTag tag : expected) {
                    expectedTags.add(tag.getType());
                }
                Collections.sort(expectedTags);
            }

            resultTags.clear();
            if(result != null) {
                for(RangerTagForEval tag : result) {
                    resultTags.add(tag.getType());
                }
                Collections.sort(resultTags);
            }

            assertEquals(test.name, expectedTags, resultTags);
        }
    }

    static class TagEnricherTestCase {
        public String                      serviceName;
        public RangerServiceDef            serviceDef;
        public Map<Long, RangerTagDef>     tagDefinitions;
        public Map<Long, RangerTag>        tags;
        public List<RangerServiceResource> serviceResources;
        public Map<Long, List<Long>>       resourceToTagIds;
        public List<TestData>              tests;


        class TestData {
            public String               name;
            public RangerAccessResource resource;
            public String               accessType;
            public List<RangerTag>      result;
        }
    }

    static class RangerResourceDeserializer implements JsonDeserializer<RangerAccessResource> {
        @Override
        public RangerAccessResource deserialize(JsonElement jsonObj, Type type,
                                                JsonDeserializationContext context) throws JsonParseException {
            return gsonBuilder.fromJson(jsonObj, RangerAccessResourceImpl.class);
        }
    }
}
