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

package org.apache.ranger.authorization.nestedstructure.authorizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.plugin.util.ServiceTags;

import java.io.*;
import java.util.List;
import java.util.Set;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class TestNestedStructureAuthorizer {
    static Gson gsonBuilder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSSZ")
                                       .setPrettyPrinting()
                                       .create();
    }

    @Test
    public void test_customer_records() {
        runTestsFromResourceFile("/test_customer_records.json");
    }

    private void runTestsFromResourceFile(String resourceName) {
        try(InputStream       inStream = this.getClass().getResourceAsStream(resourceName);
            InputStreamReader reader   = new InputStreamReader(inStream)) {
            runTests(reader, resourceName);
        } catch (IOException excp) {
            // ignore
        }
    }

    private void runTests(InputStreamReader reader, String testName) {
        NestedStructureTestCase testCase = gsonBuilder.fromJson(reader, NestedStructureTestCase.class);

        assertTrue("invalid input: " + testName, testCase != null && testCase.policies != null && testCase.tests != null);

        if (testCase.policies.getServiceDef() == null && StringUtils.isNotBlank(testCase.serviceDefFilename)) {
            try (InputStream       inStream   = this.getClass().getResourceAsStream(testCase.serviceDefFilename);
                 InputStreamReader sdefReader = new InputStreamReader(inStream)) {
                testCase.policies.setServiceDef(gsonBuilder.fromJson(sdefReader, RangerServiceDef.class));
            } catch (IOException excp) {
                // ignore
            }
        }

        NestedStructureAuthorizer authorizer = new NestedStructureAuthorizer(testCase.policies, testCase.tags, testCase.roles);

        for (NestedStructureTestCase.TestData test : testCase.tests) {
            AccessResult expected = test.result;
            AccessResult result   = authorizer.authorize(test.schema, test.user, test.userGroups, test.json, NestedStructureAccessType.getAccessType(test.accessType));

            assertEquals(test.name + ": hasAccess doesn't match: expected=" + expected.hasAccess() + ", actual=" + result.hasAccess(), expected.hasAccess(), result.hasAccess());
            assertEquals(test.name + ": json doesn't match: expected=" + expected.getJson() + ", actual=" + result.getJson(), expected.getJson(), result.getJson());
        }
    }

    static class NestedStructureTestCase {
        public ServicePolicies policies;
        public ServiceTags     tags;
        public RangerRoles     roles;
        public List<TestData>  tests;
        public String          serviceDefFilename;

        class TestData {
            public String       name;
            public String       schema;
            public String       json;
            public String       user;
            public Set<String>  userGroups;
            public String       accessType;
            public AccessResult result;
        }
    }
}
