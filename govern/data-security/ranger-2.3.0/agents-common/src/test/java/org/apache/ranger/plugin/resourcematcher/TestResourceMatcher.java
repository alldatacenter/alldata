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
import java.util.List;
import java.util.Map;

import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.resourcematcher.TestResourceMatcher.ResourceMatcherTestCases.TestCase;
import org.apache.ranger.plugin.resourcematcher.TestResourceMatcher.ResourceMatcherTestCases.TestCase.OneTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestResourceMatcher {
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

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testResourceMatcher_default() throws Exception {
		String[] tests = { "/resourcematcher/test_resourcematcher_default.json" };

		runTestsFromResourceFiles(tests);
	}

	@Test
	public void testResourceMatcher_path() throws Exception {
		String[] tests = { "/resourcematcher/test_resourcematcher_path.json" };

		runTestsFromResourceFiles(tests);
	}

	@Test
    public void testResourceMatcher_dynamic() throws Exception {
        String[] tests = { "/resourcematcher/test_resourcematcher_dynamic.json"};

        runTestsFromResourceFiles(tests);
    }

	@Test
	public void testResourceMatcher_wildcards_as_delimiters() throws Exception {
		String[] tests = { "/resourcematcher/test_resourcematcher_wildcards_as_delimiters.json"};

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
		ResourceMatcherTestCases testCases = gsonBuilder.fromJson(reader, ResourceMatcherTestCases.class);

		assertTrue("invalid input: " + testName, testCases != null && testCases.testCases != null);

		for(TestCase testCase : testCases.testCases) {
			RangerResourceMatcher matcher = createResourceMatcher(testCase.resourceDef, testCase.policyResource);
			
			for(OneTest oneTest : testCase.tests) {
				if(oneTest == null) {
					continue;
				}

				boolean expected = oneTest.result;
				boolean result   = matcher.isMatch(oneTest.input, oneTest.evalContext);

				assertEquals("isMatch() failed! " + testCase.name + ":" + oneTest.name + ": input=" + oneTest.input, expected, result);
			}
		}
	}

	private RangerResourceMatcher createResourceMatcher(RangerResourceDef resourceDef, RangerPolicyResource policyResource) throws Exception {
		RangerResourceMatcher ret = null;

		@SuppressWarnings("unchecked")
		Class<RangerResourceMatcher> matcherClass = (Class<RangerResourceMatcher>) Class.forName(resourceDef.getMatcher());

		ret = matcherClass.newInstance();
		ret.setResourceDef(resourceDef);
		ret.setPolicyResource(policyResource);
		ret.init();

		return ret;
	}

	static class ResourceMatcherTestCases {
		public List<TestCase> testCases;

		class TestCase {
			public String               name;
			public RangerResourceDef    resourceDef;
			public RangerPolicyResource policyResource;
			public List<OneTest>        tests;

			class OneTest {
				String  name;
				String  input;
				Map<String, Object> evalContext;
				boolean result;
			}
		}
	}
}
