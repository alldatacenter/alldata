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

package org.apache.ranger.biz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.biz.TestPolicyDb.PolicyDbTestCase.TestData;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestPolicyDb {
	static Gson gsonBuilder;
    static RangerServiceDef hdfsServiceDef;
    static RangerServiceDef hiveServiceDef;
    static RangerServiceDef hbaseServiceDef;
    static RangerServiceDef tagServiceDef;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
									   .setPrettyPrinting()
									   .create();
		initializeServiceDefs();
	}

	private static void initializeServiceDefs() {
        hdfsServiceDef = readServiceDef("hdfs");
        hiveServiceDef = readServiceDef("hive");
        hbaseServiceDef = readServiceDef("hbase");
        tagServiceDef = readServiceDef("tag");
    }

    private static RangerServiceDef readServiceDef(String name) {
        InputStream inStream = TestPolicyDb.class.getResourceAsStream("/admin/service-defs/test-" + name + "-servicedef.json");
        InputStreamReader reader = new InputStreamReader(inStream);
        return gsonBuilder.fromJson(reader, RangerServiceDef.class);

    }

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testPolicyDb_hdfs() {

		String[] hdfsTestResourceFiles = { "/biz/test_policydb_hdfs.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles, hdfsServiceDef);
	}

    @Test
    public void testPolicyDb_hive() {
        String[] hiveTestResourceFiles = { "/biz/test_policydb_hive.json" };

        runTestsFromResourceFiles(hiveTestResourceFiles, hiveServiceDef);
    }

	private void runTestsFromResourceFiles(String[] resourceNames, RangerServiceDef serviceDef) {
		for(String resourceName : resourceNames) {
			InputStream       inStream = this.getClass().getResourceAsStream(resourceName);
			InputStreamReader reader   = new InputStreamReader(inStream);

			runTests(reader, resourceName, serviceDef);
		}
	}

	private void runTests(InputStreamReader reader, String testName, RangerServiceDef serviceDef) {
		PolicyDbTestCase testCase = gsonBuilder.fromJson(reader, PolicyDbTestCase.class);
		if (serviceDef != null) {
			// Override serviceDef in the json test-file with a global service-def
			testCase.servicePolicies.setServiceDef(serviceDef);
		}

		assertTrue("invalid input: " + testName, testCase != null && testCase.servicePolicies != null && testCase.tests != null && testCase.servicePolicies.getPolicies() != null);


		RangerPolicyEngineOptions policyEngineOptions = new RangerPolicyEngineOptions();

		policyEngineOptions.evaluatorType           = RangerPolicyEvaluator.EVALUATOR_TYPE_OPTIMIZED;
		policyEngineOptions.cacheAuditResults       = false;
		policyEngineOptions.disableContextEnrichers = true;
		policyEngineOptions.disableCustomConditions = true;

		RangerPluginContext pluginContext = new RangerPluginContext(new RangerPluginConfig("hive", null, "test-policydb", "cl1", "on-prem", policyEngineOptions));
		RangerPolicyAdmin   policyAdmin   = new RangerPolicyAdminImpl(testCase.servicePolicies, pluginContext, null);

		for(TestData test : testCase.tests) {
			boolean expected = test.result;

			if(test.allowedPolicies != null) {
				List<RangerPolicy> allowedPolicies = policyAdmin.getAllowedUnzonedPolicies(test.user, test.userGroups, test.accessType);

				assertEquals("allowed-policy count mismatch!", test.allowedPolicies.size(), allowedPolicies.size());
				
				Set<Long> allowedPolicyIds = new HashSet<>();
				for(RangerPolicy allowedPolicy : allowedPolicies) {
					allowedPolicyIds.add(allowedPolicy.getId());
				}
				assertEquals("allowed-policy list mismatch!", test.allowedPolicies, allowedPolicyIds);
			} else {
				boolean result = policyAdmin.isAccessAllowedByUnzonedPolicies(test.resources, null, test.user, test.userGroups, test.accessType);

				assertEquals("isAccessAllowed mismatched! - " + test.name, expected, result);
			}
		}
	}

	static class PolicyDbTestCase {
		public ServicePolicies servicePolicies;
		public List<TestData>  tests;
		
		class TestData {
			public String                            name;
			public Map<String, RangerPolicyResource> resources;
			public String                            user;
			public Set<String>                       userGroups;
			public String                            accessType;
			public boolean                           result;
			public Set<Long>                         allowedPolicies;
		}
	}
}
