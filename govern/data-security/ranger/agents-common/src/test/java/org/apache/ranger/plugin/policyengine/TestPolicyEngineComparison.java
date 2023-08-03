package org.apache.ranger.plugin.policyengine;
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.contextenricher.RangerTagEnricher;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.plugin.util.ServiceTags;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestPolicyEngineComparison {
    private static Gson gsonBuilder;

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

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testComparePolicyEngines() throws Exception {
        String[] tests = {"/policyengine/test_compare_policyengines.json"};

        runTestsFromResourceFiles(tests);
    }

    private void runTestsFromResourceFiles(String[] resourceNames) throws Exception {
        for (String resourceName : resourceNames) {
            InputStream inStream = this.getClass().getResourceAsStream(resourceName);
            InputStreamReader reader = new InputStreamReader(inStream);

            runTests(reader, resourceName);
        }
    }
    private void runTests(InputStreamReader reader, String testName) throws Exception {

        ComparisonTests testCases = gsonBuilder.fromJson(reader, ComparisonTests.class);

        assertTrue("invalid input: " + testName, testCases != null && testCases.testCases != null);

        RangerPolicyEngineOptions options = new RangerPolicyEngineOptions();
        options.optimizeTrieForRetrieval = true;


        for (ComparisonTests.TestCase testCase : testCases.testCases) {

            assertTrue("invalid input: " + testCase.name ,testCase.me != null && testCase.other != null);

            ComparisonTests.TestCase.PolicyEngineData myData = testCase.me;
            ComparisonTests.TestCase.PolicyEngineData otherData = testCase.other;

            assertFalse("invalid input: " + testCase.name, myData.servicePoliciesFile == null || otherData.servicePoliciesFile == null);
            assertTrue("invalid input: " + testCase.name, myData.serviceTagsFile == null || otherData.serviceTagsFile != null);

            // Read servicePoliciesFile
            ServicePolicies myServicePolicies = readServicePolicies(myData.servicePoliciesFile);
            ServicePolicies otherServicePolicies = readServicePolicies(otherData.servicePoliciesFile);

            assertFalse("invalid input: " + testCase.name, myServicePolicies == null || otherServicePolicies == null);

            ServiceTags myServiceTags = null;
            ServiceTags otherServiceTags = null;

            if (myData.serviceTagsFile != null) {
                myServiceTags = readServiceTags(myData.serviceTagsFile);
                otherServiceTags = readServiceTags(otherData.serviceTagsFile);

                assertFalse("invalid input: " + testCase.name, myServiceTags == null || otherServiceTags == null);
            }

            boolean isPolicyEnginesEqual = true;
            boolean isTagsEqual = true;

            if (myServicePolicies != null) {
                RangerPluginContext myPluginContext = new RangerPluginContext(new RangerPluginConfig(myServicePolicies.getServiceDef().getName(), null, "test-compare-my-tags", null, null, options));
                RangerPluginContext otherPluginContext = new RangerPluginContext(new RangerPluginConfig(myServicePolicies.getServiceDef().getName(), null, "test-compare-other-tags", null, null, options));
                RangerPolicyEngineImpl myPolicyEngine = new RangerPolicyEngineImpl(myServicePolicies, myPluginContext, null);
                RangerPolicyEngineImpl otherPolicyEngine = new RangerPolicyEngineImpl(otherServicePolicies, otherPluginContext, null);

                isPolicyEnginesEqual = TestPolicyEngine.compare(myPolicyEngine.getPolicyEngine(), otherPolicyEngine.getPolicyEngine()) && TestPolicyEngine.compare(otherPolicyEngine.getPolicyEngine(), myPolicyEngine.getPolicyEngine());


                if (myServiceTags != null) {
                    RangerTagEnricher myTagEnricher = new RangerTagEnricher();
                    RangerTagEnricher otherTagEnricher = new RangerTagEnricher();

                    myTagEnricher.setAppId("test-compare-my-tags");
                    myTagEnricher.setServiceDef(myServicePolicies.getServiceDef());
                    myTagEnricher.setServiceName(myServiceTags.getServiceName());
                    myTagEnricher.init();

                    myTagEnricher.setServiceTags(myServiceTags);

                    otherTagEnricher.setAppId("test-compare-other-tags");
                    otherTagEnricher.setServiceDef(myServicePolicies.getServiceDef());
                    otherTagEnricher.setServiceName(otherServiceTags.getServiceName());
                    otherTagEnricher.init();

                    otherTagEnricher.setServiceTags(otherServiceTags);

                    isTagsEqual = TestPolicyEngine.compare(myTagEnricher, otherTagEnricher) && TestPolicyEngine.compare(otherTagEnricher, myTagEnricher);

                }
            }
            assertEquals("PolicyEngines are not equal " + testCase.name, isPolicyEnginesEqual, testCase.isPolicyEnginesEqual);
            assertEquals("Tags are not equal " + testCase.name,isTagsEqual, testCase.isTagsEqual);
        }

    }

    private ServicePolicies readServicePolicies(String fileName) {
        InputStream inStream = this.getClass().getResourceAsStream(fileName);
        InputStreamReader reader = new InputStreamReader(inStream);
        return gsonBuilder.fromJson(reader, ServicePolicies.class);
    }

    private ServiceTags readServiceTags(String fileName) {
        InputStream inStream = this.getClass().getResourceAsStream(fileName);
        InputStreamReader reader = new InputStreamReader(inStream);
        return gsonBuilder.fromJson(reader, ServiceTags.class);
    }

    static class ComparisonTests {
        List<TestCase> testCases;

        class TestCase {
            String          name;
            PolicyEngineData me;
            PolicyEngineData other;
            boolean          isPolicyEnginesEqual;
            boolean          isTagsEqual;

            class PolicyEngineData {
                String servicePoliciesFile;
                String serviceTagsFile;
            }
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
