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

package org.apache.ranger.plugin.resourcematcher;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyresourcematcher.RangerDefaultPolicyResourceMatcher;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestDefaultPolicyResourceMatcherForPolicy {
	static Gson gsonBuilder;

	static RangerServiceDef hdfsServiceDef;
	static RangerServiceDef hiveServiceDef;
	static RangerServiceDef hbaseServiceDef;
	static RangerServiceDef tagServiceDef;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
				.setPrettyPrinting()
				.registerTypeAdapter(RangerAccessResource.class, new TestDefaultPolicyResourceMatcherForPolicy.RangerResourceDeserializer())
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
		InputStream inStream = TestDefaultPolicyResourceMatcherForPolicy.class.getResourceAsStream("/admin/service-defs/test-" + name + "-servicedef.json");
		InputStreamReader reader = new InputStreamReader(inStream);
		return gsonBuilder.fromJson(reader, RangerServiceDef.class);
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
	public void testDefaultPolicyResourceMatcherForHdfs() throws Exception {
		String[] tests = { "/resourcematcher/test_defaultpolicyresourcematcher_for_hdfs_policy.json" };

		runTestsFromResourceFiles(tests, null);
	}

	@Test
	public void testDefaultPolicyResourceMatcherForHive() throws Exception {
		String[] tests = {"/resourcematcher/test_defaultpolicyresourcematcher_for_hive_policy.json"};

		runTestsFromResourceFiles(tests, null);
	}

	@Test
	public void testDefaultPolicyResourceMatcherForHive_ResourceSpecific() throws Exception {
		String[] tests = {"/resourcematcher/test_defaultpolicyresourcematcher_for_hive_policy.json"};

		runTestsFromResourceFiles(tests, hiveServiceDef);
	}

	private void runTestsFromResourceFiles(String[] resourceNames, RangerServiceDef serviceDef) throws Exception {
		for(String resourceName : resourceNames) {
			InputStream inStream = this.getClass().getResourceAsStream(resourceName);
			InputStreamReader reader = new InputStreamReader(inStream, Charset.defaultCharset());

			runTests(reader, serviceDef);
		}
	}

	private void runTests(InputStreamReader reader, RangerServiceDef serviceDef) throws Exception {
		DefaultPolicyResourceMatcherTestCases testCases = gsonBuilder.fromJson(reader, DefaultPolicyResourceMatcherTestCases.class);

		for (DefaultPolicyResourceMatcherTestCases.TestCase testCase : testCases.testCases) {
			runTest(testCase, serviceDef == null ? testCases.serviceDef : serviceDef);
		}
	}
	private void runTest(DefaultPolicyResourceMatcherTestCases.TestCase testCase, RangerServiceDef serviceDef) throws Exception {

		assertTrue("invalid input: " , testCase != null && testCase.tests != null);

		RangerDefaultPolicyResourceMatcher matcher = new RangerDefaultPolicyResourceMatcher();
		matcher.setServiceDef(serviceDef);
		matcher.setPolicyResources(testCase.policyResources);
		matcher.init();

		for(DefaultPolicyResourceMatcherTestCases.TestCase.OneTest oneTest : testCase.tests) {
			if(oneTest == null) {
				continue;
			}

			boolean expected = oneTest.result;
			RangerPolicyResourceMatcher.MatchScope scope;
			if (StringUtils.equalsIgnoreCase(oneTest.type, "selfOrDescendantMatch")) {
				scope = RangerPolicyResourceMatcher.MatchScope.SELF_OR_DESCENDANT;
			} else if (StringUtils.equalsIgnoreCase(oneTest.type, "descendantMatch")) {
				scope = RangerPolicyResourceMatcher.MatchScope.DESCENDANT;
			} else if (StringUtils.equalsIgnoreCase(oneTest.type, "exactMatch")) {
				scope = RangerPolicyResourceMatcher.MatchScope.SELF;
			} else if (StringUtils.equalsIgnoreCase(oneTest.type, "selfOrAncestorMatch")) {
				scope = RangerPolicyResourceMatcher.MatchScope.SELF_OR_ANCESTOR;
			} else if (StringUtils.equalsIgnoreCase(oneTest.type, "ancestorMatch")) {
				scope = RangerPolicyResourceMatcher.MatchScope.ANCESTOR;
			} else if (StringUtils.equalsIgnoreCase(oneTest.type, "anyMatch")) {
				scope = RangerPolicyResourceMatcher.MatchScope.ANY;
			} else {
				continue;
			}
			boolean result = matcher.isMatch(oneTest.policy, scope, oneTest.evalContext);

			assertEquals("match failed! " + ":" + testCase.name + ":" + oneTest.name + ":" + oneTest.type + ": policy=" + oneTest.policy, expected, result);
		}
	}

	private static class DefaultPolicyResourceMatcherTestCases {
		RangerServiceDef serviceDef;

		List<TestCase> testCases;

		static class TestCase {
			String name;
			Map<String, RangerPolicyResource> policyResources;
			List<OneTest> tests;

			static class OneTest {
				String name;
				String type;
				RangerPolicy policy;
				Map<String, Object> evalContext;
				boolean result;
			}
		}
	}

	private static class RangerResourceDeserializer implements JsonDeserializer<RangerAccessResource> {
		@Override
		public RangerAccessResource deserialize(JsonElement jsonObj, Type type,
												JsonDeserializationContext context) throws JsonParseException {
			return gsonBuilder.fromJson(jsonObj, RangerAccessResourceImpl.class);
		}
	}
}
