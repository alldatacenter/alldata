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
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.ranger.plugin.contextenricher.RangerContextEnricher;
import org.apache.ranger.plugin.contextenricher.RangerTagEnricher;
import org.apache.ranger.plugin.service.RangerAuthContext;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestRangerAuthContext {
	private static Gson gsonBuilder;
	private static RangerBasePlugin plugin;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z")
				.setPrettyPrinting()
				.create();

		plugin = new RangerBasePlugin("hive", "TestRangerAuthContext");
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
	public void testRangerAuthContext() throws Exception {
		String[] tests = {"/policyengine/plugin/test_auth_context.json"};

		runTestsFromResourceFiles(tests);
	}

	private void runTestsFromResourceFiles(String[] resourceNames) throws Exception {
		for(String resourceName : resourceNames) {
			InputStream       inStream = this.getClass().getResourceAsStream(resourceName);
			InputStreamReader reader   = new InputStreamReader(inStream);

			runTests(reader, resourceName);
		}
	}

	private void runTests(InputStreamReader reader, String fileName) throws Exception {
		RangerAuthContextTests testCases = gsonBuilder.fromJson(reader, RangerAuthContextTests.class);

		for(RangerAuthContextTests.TestCase testCase : testCases.testCases) {
			String testName = testCase.name;

			plugin.setPolicies(testCase.servicePolicies);

			RangerAuthContext                  ctx              = plugin.getCurrentRangerAuthContext();
			Map<RangerContextEnricher, Object> contextEnrichers = ctx.getRequestContextEnrichers();

			assertTrue(fileName + "-" + testName + " - Empty contextEnrichers", MapUtils.isNotEmpty(contextEnrichers) && contextEnrichers.size() == 2);

			for (Map.Entry<RangerContextEnricher, Object> entry : contextEnrichers.entrySet()) {
				RangerContextEnricher enricher     = entry.getKey();
				String                enricherName = enricher.getName();
				Object                enricherData = entry.getValue();

				if (enricherName.equals("ProjectProvider")) {
					assertTrue(fileName + "-" + testName + " - Invalid contextEnricher", enricherData instanceof RangerContextEnricher);
				} else if (enricherName.equals("TagEnricher")) {
					assertTrue("- Invalid contextEnricher", (enricherData instanceof RangerTagEnricher || enricherData instanceof RangerTagEnricher.EnrichedServiceTags));
				} else {
					assertTrue(fileName + "-" + testName + " - Unexpected type of contextEnricher", false);
				}
			}
		}
	}

	static class RangerAuthContextTests {
		List<TestCase> testCases;

		class TestCase {
			String               name;
			ServicePolicies      servicePolicies;
		}
	}

}

