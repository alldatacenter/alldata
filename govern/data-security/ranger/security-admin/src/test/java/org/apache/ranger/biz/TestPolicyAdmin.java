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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.biz.TestPolicyAdmin.PolicyAdminTestCase.TestData;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPolicyAdmin {
	static Gson gsonBuilder;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
									   .setPrettyPrinting()
									   .create();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testPolicyAdmin_additionalResources() {
		String[] testFile = { "/biz/test_policyadmin_additional_resources.json" };

		runTestsFromResourceFiles(testFile);
	}

	private void runTestsFromResourceFiles(String[] resourceNames) {
		for(String resourceName : resourceNames) {
			InputStream       inStream = this.getClass().getResourceAsStream(resourceName);
			InputStreamReader reader   = new InputStreamReader(inStream);

			runTests(reader, resourceName);
		}
	}

	private void runTests(InputStreamReader reader, String testName) {
		PolicyAdminTestCase testCase = gsonBuilder.fromJson(reader, PolicyAdminTestCase.class);

		assertTrue("invalid input: " + testName, testCase != null && testCase.servicePolicies != null && testCase.tests != null && testCase.servicePolicies.getPolicies() != null);

		RangerPolicyEngineOptions policyEngineOptions = new RangerPolicyEngineOptions();

		policyEngineOptions.evaluatorType             = RangerPolicyEvaluator.EVALUATOR_TYPE_OPTIMIZED;
		policyEngineOptions.cacheAuditResults         = false;
		policyEngineOptions.disableContextEnrichers   = true;
		policyEngineOptions.disableCustomConditions   = true;
		policyEngineOptions.evaluateDelegateAdminOnly = true;

		RangerPluginContext pluginContext = new RangerPluginContext(new RangerPluginConfig("hive", null, "test-policydb", "cl1", "on-prem", policyEngineOptions));
		RangerPolicyAdmin   policyAdmin   = new RangerPolicyAdminImpl(testCase.servicePolicies, pluginContext, null);

		for(TestData test : testCase.tests) {
			if (test.userGroups == null) {
				test.userGroups = Collections.emptySet();
			}

			if (test.allowedPolicies != null) {
				Set<Long> allowedPolicies = new HashSet<>();

				for (RangerPolicy policy : testCase.servicePolicies.getPolicies()) {
					boolean isAllowed = test.isModifyAccess ? policyAdmin.isDelegatedAdminAccessAllowedForModify(policy, test.user, test.userGroups, null, null)
							                                : policyAdmin.isDelegatedAdminAccessAllowedForRead(policy, test.user, test.userGroups, null, null);

					if (isAllowed) {
						allowedPolicies.add(policy.getId());
					}
				}

				assertEquals("allowed-policy count mismatch! - " + test.name, test.allowedPolicies.size(), allowedPolicies.size());

				assertEquals("allowed-policy list mismatch! - " + test.name, test.allowedPolicies, allowedPolicies);
			} else {
				RangerPolicy     policy     = new RangerPolicy();
				RangerPolicyItem policyItem = new RangerPolicyItem();

				policyItem.getUsers().add(test.user);
				policyItem.getUsers().addAll(test.userGroups);

				for (String accessType : test.accessTypes) {
					policyItem.getAccesses().add(new RangerPolicy.RangerPolicyItemAccess(accessType));
				}

				policy.setResources(test.resources);
				policy.setAdditionalResources(test.additionalResources);
				policy.getPolicyItems().add(policyItem);

				final boolean expected = test.result;
				final boolean result;

				if (test.isModifyAccess) {
					result = policyAdmin.isDelegatedAdminAccessAllowedForModify(policy, test.user, test.userGroups, null, null);
				} else {
					result = policyAdmin.isDelegatedAdminAccessAllowedForRead(policy, test.user, test.userGroups, null, null);
				}

				assertEquals("isAccessAllowed mismatched! - " + test.name, expected, result);
			}
		}
	}

	static class PolicyAdminTestCase {
		public ServicePolicies servicePolicies;
		public List<TestData>  tests;

		class TestData {
			public String                                  name;
			public Map<String, RangerPolicyResource>       resources;
			public List<Map<String, RangerPolicyResource>> additionalResources;
			public String                                  user;
			public Set<String>                             userGroups;
			public Set<String>                             accessTypes;
			public boolean                                 isModifyAccess;
			public boolean                                 result;
			public Set<Long>                               allowedPolicies;
		}
	}
}
