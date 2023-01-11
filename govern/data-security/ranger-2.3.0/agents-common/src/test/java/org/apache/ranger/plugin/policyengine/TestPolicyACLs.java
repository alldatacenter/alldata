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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.policyengine.RangerResourceACLs.DataMaskResult;
import org.apache.ranger.plugin.policyengine.RangerResourceACLs.RowFilterResult;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class TestPolicyACLs {
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
	public void testResourceMatcher_default() throws Exception {
		String[] tests = { "/policyengine/test_aclprovider_default.json" };

		runTestsFromResourceFiles(tests);
	}

	@Test
	public void testResourceACLs_dataMask() throws Exception {
		String[] tests = {"/policyengine/test_aclprovider_mask_filter.json"};

		runTestsFromResourceFiles(tests);
	}

	@Test
	public void testResourceACLs_hdfs() throws Exception {
		String[] tests = {"/policyengine/test_aclprovider_hdfs.json"};

		runTestsFromResourceFiles(tests);
	}

	private void runTestsFromResourceFiles(String[] resourceNames) throws Exception {
		for(String resourceName : resourceNames) {
			InputStream       inStream = this.getClass().getResourceAsStream(resourceName);
			InputStreamReader reader   = new InputStreamReader(inStream);

			runTests(reader, resourceName);
		}
	}

	private void runTests(InputStreamReader reader, String testName) throws Exception {
		PolicyACLsTests testCases = gsonBuilder.fromJson(reader, PolicyACLsTests.class);

		assertTrue("invalid input: " + testName, testCases != null && testCases.testCases != null);

		for(PolicyACLsTests.TestCase testCase : testCases.testCases) {
			String                    serviceType         = testCase.servicePolicies.getServiceDef().getName();
			RangerPolicyEngineOptions policyEngineOptions = new RangerPolicyEngineOptions();
			RangerPluginContext       pluginContext       = new RangerPluginContext(new RangerPluginConfig(serviceType, null, "test-policy-acls", "cl1", "on-prem", policyEngineOptions));
			RangerPolicyEngine        policyEngine        = new RangerPolicyEngineImpl(testCase.servicePolicies, pluginContext, null);

			for(PolicyACLsTests.TestCase.OneTest oneTest : testCase.tests) {
				if(oneTest == null) {
					continue;
				}
				RangerAccessRequestImpl request = new RangerAccessRequestImpl(oneTest.resource, RangerPolicyEngine.ANY_ACCESS, null, null, null);
				RangerResourceACLs acls = policyEngine.getResourceACLs(request);

				boolean userACLsMatched = true, groupACLsMatched = true, roleACLsMatched = true, rowFiltersMatched = true, dataMaskingMatched = true;

				if (MapUtils.isNotEmpty(acls.getUserACLs()) && MapUtils.isNotEmpty(oneTest.userPermissions)) {

					for (Map.Entry<String, Map<String, RangerResourceACLs.AccessResult>> entry :
							acls.getUserACLs().entrySet()) {
						String userName = entry.getKey();
						Map<String, RangerResourceACLs.AccessResult> expected = oneTest.userPermissions.get(userName);
						if (MapUtils.isNotEmpty(entry.getValue()) && MapUtils.isNotEmpty(expected)) {
							// Compare
							for (Map.Entry<String, RangerResourceACLs.AccessResult> privilege : entry.getValue().entrySet()) {
								if (StringUtils.equals(RangerPolicyEngine.ADMIN_ACCESS, privilege.getKey())) {
									continue;
								}
								RangerResourceACLs.AccessResult expectedResult = expected.get(privilege.getKey());
								if (expectedResult == null) {
									userACLsMatched = false;
									break;
								} else if (!expectedResult.equals(privilege.getValue())) {
									userACLsMatched = false;
									break;
								}
							}
						} else if (!(MapUtils.isEmpty(entry.getValue()) && MapUtils.isEmpty(expected))){
							Set<String> privileges = entry.getValue().keySet();
							if (privileges.size() == 1 && privileges.contains(RangerPolicyEngine.ADMIN_ACCESS)) {
								userACLsMatched = true;
							} else {
								userACLsMatched = false;
							}
							break;
						}
						if (!userACLsMatched) {
							break;
						}
					}
				} else if (!(MapUtils.isEmpty(acls.getUserACLs()) && MapUtils.isEmpty(oneTest.userPermissions))) {
					userACLsMatched = false;
				}

				if (acls.getDataMasks().isEmpty()) {
					dataMaskingMatched = (oneTest.dataMasks == null || oneTest.dataMasks.isEmpty());
				} else if (acls.getDataMasks().size() != (oneTest.dataMasks == null ? 0 : oneTest.dataMasks.size())) {
					dataMaskingMatched = false;
				} else {
					for (int i = 0; i < acls.getDataMasks().size(); i++) {
						DataMaskResult found    = acls.getDataMasks().get(i);
						DataMaskResult expected = oneTest.dataMasks.get(i);

						dataMaskingMatched = found.equals(expected);

						if (!dataMaskingMatched) {
							break;
						}
					}
				}

				if (acls.getRowFilters().isEmpty()) {
					rowFiltersMatched = (oneTest.rowFilters == null || oneTest.rowFilters.isEmpty());
				} else if (acls.getRowFilters().size() != (oneTest.rowFilters == null ? 0 : oneTest.rowFilters.size())) {
					rowFiltersMatched = false;
				} else {
					for (int i = 0; i < acls.getRowFilters().size(); i++) {
						RowFilterResult found    = acls.getRowFilters().get(i);
						RowFilterResult expected = oneTest.rowFilters.get(i);

						rowFiltersMatched = found.equals(expected);

						if (!rowFiltersMatched) {
							break;
						}
					}
				}

				if (MapUtils.isNotEmpty(acls.getGroupACLs()) && MapUtils.isNotEmpty(oneTest.groupPermissions)) {
					for (Map.Entry<String, Map<String, RangerResourceACLs.AccessResult>> entry :
							acls.getGroupACLs().entrySet()) {
						String groupName = entry.getKey();
						Map<String, RangerResourceACLs.AccessResult> expected = oneTest.groupPermissions.get(groupName);
						if (MapUtils.isNotEmpty(entry.getValue()) && MapUtils.isNotEmpty(expected)) {
							// Compare
							for (Map.Entry<String, RangerResourceACLs.AccessResult> privilege : entry.getValue().entrySet()) {
								if (StringUtils.equals(RangerPolicyEngine.ADMIN_ACCESS, privilege.getKey())) {
									continue;
								}
								RangerResourceACLs.AccessResult expectedResult = expected.get(privilege.getKey());
								if (expectedResult == null) {
									groupACLsMatched = false;
									break;
								} else if (!expectedResult.equals(privilege.getValue())) {
									groupACLsMatched = false;
									break;
								}
							}
						} else if (!(MapUtils.isEmpty(entry.getValue()) && MapUtils.isEmpty(expected))){
							Set<String> privileges = entry.getValue().keySet();
							if (privileges.size() == 1 && privileges.contains(RangerPolicyEngine.ADMIN_ACCESS)) {
								groupACLsMatched = true;
							} else {
								groupACLsMatched = false;
							}
							break;
						}
						if (!groupACLsMatched) {
							break;
						}
					}
				} else if (!(MapUtils.isEmpty(acls.getGroupACLs()) && MapUtils.isEmpty(oneTest.groupPermissions))) {
					groupACLsMatched = false;
				}

				if (MapUtils.isNotEmpty(acls.getRoleACLs()) && MapUtils.isNotEmpty(oneTest.rolePermissions)) {
					for (Map.Entry<String, Map<String, RangerResourceACLs.AccessResult>> entry :
							acls.getRoleACLs().entrySet()) {
						String roleName = entry.getKey();
						Map<String, RangerResourceACLs.AccessResult> expected = oneTest.rolePermissions.get(roleName);
						if (MapUtils.isNotEmpty(entry.getValue()) && MapUtils.isNotEmpty(expected)) {
							// Compare
							for (Map.Entry<String, RangerResourceACLs.AccessResult> privilege : entry.getValue().entrySet()) {
								if (StringUtils.equals(RangerPolicyEngine.ADMIN_ACCESS, privilege.getKey())) {
									continue;
								}
								RangerResourceACLs.AccessResult expectedResult = expected.get(privilege.getKey());
								if (expectedResult == null) {
									roleACLsMatched = false;
									break;
								} else if (!expectedResult.equals(privilege.getValue())) {
									roleACLsMatched = false;
									break;
								}
							}
						} else if (!(MapUtils.isEmpty(entry.getValue()) && MapUtils.isEmpty(expected))){
							Set<String> privileges = entry.getValue().keySet();
							if (privileges.size() == 1 && privileges.contains(RangerPolicyEngine.ADMIN_ACCESS)) {
								roleACLsMatched = true;
							} else {
								roleACLsMatched = false;
							}
							break;
						}
						if (!roleACLsMatched) {
							break;
						}
					}
				} else if (!(MapUtils.isEmpty(acls.getRoleACLs()) && MapUtils.isEmpty(oneTest.rolePermissions))) {
					roleACLsMatched = false;
				}
				assertTrue("getResourceACLs() failed! " + testCase.name + ":" + oneTest.name, userACLsMatched && groupACLsMatched && roleACLsMatched && rowFiltersMatched && dataMaskingMatched);
			}
		}
	}

	static class PolicyACLsTests {
		List<TestCase> testCases;

		class TestCase {
			String               name;
			ServicePolicies      servicePolicies;
			List<OneTest>        tests;

			class OneTest {
				String               name;
				RangerAccessResource resource;
				Map<String, Map<String, RangerResourceACLs.AccessResult>> userPermissions;
				Map<String, Map<String, RangerResourceACLs.AccessResult>> groupPermissions;
				Map<String, Map<String, RangerResourceACLs.AccessResult>> rolePermissions;
				List<RowFilterResult>                                     rowFilters;
				List<DataMaskResult>                                      dataMasks;
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

