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

package org.apache.ranger.policyengine;

import static com.google.common.collect.Iterables.get;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerPluginContext;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineImpl;
import org.apache.ranger.plugin.util.PerfDataRecorder;
import org.apache.ranger.plugin.util.PerfDataRecorder.PerfStatistic;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.policyengine.perftest.v2.RangerPolicyFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.io.Files;

import be.ceau.chart.LineChart;
import be.ceau.chart.color.Color;
import be.ceau.chart.data.LineData;
import be.ceau.chart.dataset.LineDataset;

/**
 * A parameterized JUnit test that tests the performance of RangerPolicyEngine, under increasing load of number of policies and concurrent calls.
 * A cross product of the input parameters are generated and fed into the test method.
 * This microbenchmark includes a warm-up phase so that any of the JIT performance optimizations happen before the measurement of the policy engine's performance.
 */
@RunWith(Parameterized.class)
public class RangerPolicyEnginePerformanceTest {

	private static final String STATISTICS_KEY__ACCESS_ALLOWED = "RangerPolicyEngine.isAccessAllowed";

	/* pre-warming unit-under-test's method with this many call iterations, so all possible JIT optimization happen before measuring performance */
	private static final int WARM_UP__ITERATIONS = 30_000;

	private static LoadingCache<Integer, List<RangerAccessRequest>> requestsCache = CacheBuilder.newBuilder().build(createAccessRequestsCacheLoader());

	private static LoadingCache<Integer, ServicePolicies> servicePoliciesCache = CacheBuilder.newBuilder().build(createServicePoliciesCacheLoader());

	@Parameter(0)
	public Integer numberOfPolicies;

	@Parameter(1)
	public Integer concurrency;

	/**
	 * Generates a cross product of number-of-policies X concurrency parameter sets.
	 * @returns a collection of "tuples" (Object[]) of numberOfPolicies and concurrency for the given test run
	 */
	@Parameters(name = "{index}: isAccessAllowed(policies: {0}, concurrent calls: {1})")
	public static Iterable<Object[]> data() {
		// tree set for maintaining natural ordering
		Set<Integer> policies = Sets.newTreeSet(Lists.newArrayList(5, 50, 100, 250, 500, 1_000, 2_000, 3_000, 4_000, 5_000));
		Set<Integer> concurrency = Sets.newTreeSet(Lists.newArrayList(1, 5, 10, 20, 30, 40, 50, 100));

		return Iterables.transform(Sets.cartesianProduct(policies, concurrency), new Function<List<Integer>, Object[]>() {
			@Override
			public Object[] apply(List<Integer> input) {
				return input.toArray();
			}
		});
	}

	@BeforeClass
	public static void init() throws IOException {
		PerfDataRecorder.initialize(Arrays.asList("")); // dummy value initializes PerfDataRecorder
		Files.write("policies;concurrency;average;min;max;total-time-spent;\n", outputFile(), Charsets.UTF_8);
	}

	@AfterClass
	public static void chartResults() throws IOException {
		// row: policies
		// column: concurrency
		// value: average
		LineChart chart = buildChart(parsePerformanceTable());
		String chartMarkup = StrSubstitutor.replace(RangerPolicyFactory.readResourceFile("/testdata/performance-chart.template"), ImmutableMap.of("data", chart.toJson()));
		Files.write(chartMarkup, new File("target", "performance-chart.html"), Charsets.UTF_8);
	}

	@Before
	public void before() throws Exception {
		PerfDataRecorder.clearStatistics();
	}

	@After
	public void after() throws IOException {
		Map<String, PerfStatistic> exposeStatistics = PerfDataRecorder.exposeStatistics();
		PerfStatistic stat = exposeStatistics.get(STATISTICS_KEY__ACCESS_ALLOWED);
		long average = stat.getNumberOfInvocations() > 0 ? (stat.getMicroSecondsSpent() / stat.getNumberOfInvocations()) : 0;
		Files.append(String.format("%s;%s;%s;%s;%s;%s;\n", numberOfPolicies, concurrency, average, stat.getMinTimeSpent(), stat.getMaxTimeSpent(), stat.getMicroSecondsSpent()), outputFile(), Charsets.UTF_8);
		PerfDataRecorder.printStatistics();
		PerfDataRecorder.clearStatistics();
	}

	@Test
	public void policyEngineTest() throws InterruptedException {
		List<RangerAccessRequest> requests = requestsCache.getUnchecked(concurrency);
		ServicePolicies servicePolicies = servicePoliciesCache.getUnchecked(numberOfPolicies);
		RangerPluginContext pluginContext = new RangerPluginContext(new RangerPluginConfig("hive", null, "perf-test", "cl1", "on-prem", RangerPolicyFactory.createPolicyEngineOption()));
		final RangerPolicyEngineImpl rangerPolicyEngine = new RangerPolicyEngineImpl(servicePolicies, pluginContext, null);

		for (int iterations = 0; iterations < WARM_UP__ITERATIONS; iterations++) {
			// using return value of 'isAccessAllowed' with a cheap operation: System#identityHashCode so JIT wont remove it as dead code
			System.identityHashCode(rangerPolicyEngine.evaluatePolicies(requests.get(iterations % concurrency), RangerPolicy.POLICY_TYPE_ACCESS, null));
			PerfDataRecorder.clearStatistics();
		}

		final CountDownLatch latch = new CountDownLatch(concurrency);
		for (int i = 0; i < concurrency; i++) {
			final RangerAccessRequest rangerAccessRequest = requests.get(i);
			new Thread(new Runnable() {
				@Override
				public void run() {
					System.identityHashCode(rangerPolicyEngine.evaluatePolicies(rangerAccessRequest, RangerPolicy.POLICY_TYPE_ACCESS, null));
					latch.countDown();
				}
			}, String.format("Client #%s", i)).start();
		}
		latch.await();
	}

	private static File outputFile() {
		return new File("target", "ranger-policy-engine-performance.csv");
	}

	private static CacheLoader<Integer, List<RangerAccessRequest>> createAccessRequestsCacheLoader() {
		return new CacheLoader<Integer, List<RangerAccessRequest>> () {
			@Override
			public List<RangerAccessRequest> load(Integer numberOfRequests) throws Exception {
				return RangerPolicyFactory.createAccessRequests(numberOfRequests);
			}
		};
	}

	private static CacheLoader<Integer, ServicePolicies> createServicePoliciesCacheLoader() {
		return new CacheLoader<Integer, ServicePolicies>() {
			@Override
			public ServicePolicies load(Integer numberOfPolicies) throws Exception {
				return RangerPolicyFactory.createServicePolicy(numberOfPolicies);
			}
		};
	}
	
	private static LineChart buildChart(Table<Long, Long, BigDecimal> policyConcurrencyValueTable) {
		LineData lineData = new LineData();
		LineChart chart = new LineChart(lineData);
		for (Entry<Long, Map<Long, BigDecimal>> concurrencyKeyedEntry : policyConcurrencyValueTable.columnMap().entrySet()) {
			LineDataset dataset = new LineDataset()
					.setBackgroundColor(Color.TRANSPARENT)
					.setBorderColor(Color.random())
					.setLabel(String.format("%s client(s)", concurrencyKeyedEntry.getKey()))
					.setData(concurrencyKeyedEntry.getValue().values());
			lineData.addDataset(dataset);
		}
		
		for (Long policies : policyConcurrencyValueTable.rowKeySet()) {
			lineData.addLabels(String.format("Policies %s", policies));
		}
		return chart;
	}

	private static Table<Long, Long, BigDecimal> parsePerformanceTable() throws IOException {
		Table<Long, Long, BigDecimal> policyConcurrencyValueTable = TreeBasedTable.create();
		List<String> lines = Files.readLines(outputFile(), Charsets.UTF_8);
		Splitter splitter = Splitter.on(";");
		// the 0th. line contains the header, skipping that.
		for (int i = 1; i < lines.size(); i++) {
			Iterable<String> values = splitter.split(lines.get(i));
			Long policies = Long.valueOf(get(values, 0));
			Long concurrency = Long.valueOf(get(values, 1));
			BigDecimal averageValue = new BigDecimal(get(values, 2));
			policyConcurrencyValueTable.put(policies, concurrency, averageValue);
		}
		return policyConcurrencyValueTable;
	}
}
