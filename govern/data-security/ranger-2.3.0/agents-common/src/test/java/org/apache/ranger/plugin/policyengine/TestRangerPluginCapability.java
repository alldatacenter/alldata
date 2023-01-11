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

package org.apache.ranger.plugin.policyengine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.plugin.util.RangerPluginCapability;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestRangerPluginCapability {

    private static Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
				.setPrettyPrinting()
				.create();

    @Test
    public void testRangerPluginCapabilities() {
        String[] tests = {"/policyengine/plugin/test_plugin_capability.json"};

        runTestsFromResourceFiles(tests);
    }

    private void runTestsFromResourceFiles(String[] resourceNames) {
        for(String resourceName : resourceNames) {
            InputStream inStream = this.getClass().getResourceAsStream(resourceName);
            InputStreamReader reader   = new InputStreamReader(inStream);

            runTests(reader, resourceName);
        }
    }

    private void runTests(InputStreamReader reader, String fileName) {
        RangerPluginCapabilityTest testCases = gsonBuilder.fromJson(reader, RangerPluginCapabilityTest.class);

        for (RangerPluginCapabilityTest.TestCase testCase : testCases.testCases) {
            String testName = testCase.name;

            RangerPluginCapability me;
            if (CollectionUtils.isEmpty(testCase.myCapabilities)) {
                me = new RangerPluginCapability();
            } else {
                me = new RangerPluginCapability(testCase.myCapabilities);
            }
            RangerPluginCapability other = new RangerPluginCapability(testCase.otherCapabilities);

            List<String> difference = me.compare(other);

            assertTrue(fileName + "-" + testName + "-" + Arrays.toString(difference.toArray()), StringUtils.equals(JsonUtils.listToJson(difference), JsonUtils.listToJson(testCase.difference)));

        }
    }

    static class RangerPluginCapabilityTest {
        List<TestCase> testCases;

        class TestCase {
            String  name;
            List<String> myCapabilities;
            List<String> otherCapabilities;

            List<String> difference;
        }
    }
}
