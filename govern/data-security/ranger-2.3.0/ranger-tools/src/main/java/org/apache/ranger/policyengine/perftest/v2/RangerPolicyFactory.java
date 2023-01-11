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

package org.apache.ranger.policyengine.perftest.v2;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.policyengine.RangerAccessRequestDeserializer;
import org.apache.ranger.policyengine.RangerResourceDeserializer;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Factory for creating and wiring the object graph of {@link ServicePolicies} and {@link RangerAccessRequest}.
 */
public class RangerPolicyFactory {

	private static final double SUCCESSFUL_ACCESS_RATE = 0.7d;

	private static final Random RANDOM = new Random();

	private static final List<String> KNOWN_DATABASES = createList("database", 10);

	private static final List<String> KNOWN_TABLES = createList("tables", 100);

	private static final List<String> KNOWN_COLUMNS = createList("column", 1000);

	private static final List<String> KNOWN_USERS = createList("user", 1000);

	private static final List<String> RANDOM_VALUES = createList("random", 100);

	private static final List<String> ALWAYS_ALLOWED_ACCESS_TYPES = Arrays.asList("create", "select", "drop");

	/**
	 * Returns a {@link ServicePolicies service policy} instance with containing the specified number of generated policies.
	 * @param numberOfPolicies
	 * @return
	 */
	public static ServicePolicies createServicePolicy(int numberOfPolicies) {
		ServicePolicies servicePolicies = loadTemplate("/testdata/test_servicepolicies_hive.json", new TypeToken<ServicePolicies>(){}.getType());
		mutate(servicePolicies, numberOfPolicies);
		return servicePolicies;
	}

	private static void mutate(ServicePolicies servicePolicies, int numberOfPolicies) {
		servicePolicies.getPolicies().clear(); // reset
		servicePolicies.setPolicies(createPolicies(numberOfPolicies));
	}

	private static List<RangerPolicy> createPolicies(int numberOfPolicies) {
		List<RangerPolicy> policies = Lists.newArrayList();
		String template = readResourceFile("/testdata/single-policy-template.json");
		for (int i = 0; i < numberOfPolicies; i++) {
			policies.add(createPolicyFromTemplate(template, i, isAllowed()));
		}
		return policies;
	}

	private static RangerPolicy createPolicyFromTemplate(String template, long policyId, boolean isAllowed) {
		RangerPolicy rangerPolicy = buildGson().fromJson(template, RangerPolicy.class);
		rangerPolicy.setId(policyId);
		rangerPolicy.setName(String.format("generated policyname #%s", policyId));
		rangerPolicy.setResources(createRangerPolicyResourceMap(isAllowed));
		rangerPolicy.setPolicyItems(createPolicyItems(isAllowed));
		return rangerPolicy;
	}

	private static Map<String, RangerPolicyResource> createRangerPolicyResourceMap(boolean isAllowed) {
		RangerPolicyResource db = new RangerPolicyResource(isAllowed ? pickFewRandomly(KNOWN_DATABASES): RANDOM_VALUES, false, false);
		RangerPolicyResource table = new RangerPolicyResource(isAllowed ? pickFewRandomly(KNOWN_TABLES) : RANDOM_VALUES, false, false);
		RangerPolicyResource column = new RangerPolicyResource(isAllowed ? pickFewRandomly(KNOWN_COLUMNS) : RANDOM_VALUES, false, false);
		return ImmutableMap.of("database", db, "table", table, "column", column);
	}


	private static List<RangerPolicyItem> createPolicyItems(boolean isAllowed) {
		List<RangerPolicyItem> policyItems = Lists.newArrayList();
		for (int i = 0; i < 15; i++) {
			policyItems.add(createPolicyItem(isAllowed));
		}
		return policyItems;
	}

	private static RangerPolicyItem createPolicyItem(boolean isAllowed) {
		RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
		rangerPolicyItem.setDelegateAdmin(false);
		rangerPolicyItem.setUsers(isAllowed ? KNOWN_USERS : RANDOM_VALUES);
		rangerPolicyItem.setAccesses(createAccesses(isAllowed));
		return rangerPolicyItem;
	}

	private static List<RangerPolicyItemAccess> createAccesses(boolean isAllowed) {
		List<RangerPolicyItemAccess> results = Lists.newArrayList();
		results.addAll(Lists.transform(isAllowed ? ALWAYS_ALLOWED_ACCESS_TYPES : RANDOM_VALUES, new Function<String, RangerPolicyItemAccess>() {
			@Override
			public RangerPolicyItemAccess apply(String input) {
				return new RangerPolicyItemAccess(input, true);
			}
		}));
		return results;
	}

	/**
	 * Generates and returns a list of {@link RangerAccessRequest requests}
	 * @param nubmerOfRequests the number of requests to generate.
	 * @return
	 */
	public static List<RangerAccessRequest> createAccessRequests(int nubmerOfRequests) {
		List<RangerAccessRequest> result = Lists.newArrayList();
		Gson gson = buildGson();
		String template = readResourceFile("/testdata/single-request-template.json");
		for (int i = 0; i < nubmerOfRequests; i++) {
			RangerAccessRequestImpl accessRequest = gson.fromJson(template, RangerAccessRequestImpl.class);
			result.add(mutate(accessRequest, isAllowed()));
		}
		return result;
	}

	/**
	 * @return 10% of the time returns <code>true</code>, in which case the generated request policy should evaluate to true.
	 */
	private static boolean isAllowed() {
		return RANDOM.nextDouble() < SUCCESSFUL_ACCESS_RATE;
	}

	private static RangerAccessRequest mutate(RangerAccessRequest template, boolean shouldEvaluateToTrue) {
		RangerAccessRequestImpl accessRequest = (RangerAccessRequestImpl) template;
		accessRequest.setResource(new RangerAccessResourceImpl(createResourceElements(shouldEvaluateToTrue)));
		accessRequest.setAccessType(pickOneRandomly(ALWAYS_ALLOWED_ACCESS_TYPES ));
		accessRequest.setRequestData(null);
		accessRequest.setUser(pickOneRandomly(KNOWN_USERS));
		return accessRequest;
	}

	private static ImmutableMap<String, Object> createResourceElements(boolean shouldEvaluateToTrue) {
		String database = String.format("db_%s", System.nanoTime());
		String table = String.format("table_%s", System.nanoTime());
		String column = String.format("column_%s", System.nanoTime());

		if (shouldEvaluateToTrue) {
			database = pickOneRandomly(KNOWN_DATABASES);
			table = pickOneRandomly(KNOWN_TABLES);
			column = pickOneRandomly(KNOWN_COLUMNS);
		}
		return ImmutableMap.of("database", database, "table", table, "column", column);
	}

	private static List<String> createList(String name, int n) {
		List<String> results = Lists.newArrayList();
		for (int i = 0; i < n; i++) {
			results.add(String.format("%s_%s",name, i));
		}
		return results;
	}

	private static String pickOneRandomly(Collection<String> list) {
		return Iterables.get(list, RANDOM.nextInt(list.size()));
	}
	
	private static List<String> pickFewRandomly(final List<String> list) {
		int resultSize = RANDOM.nextInt(list.size());
		
		Set<String> results = Sets.newHashSet();
		for (int i = 0; i < resultSize; i++) {
			results.add(pickOneRandomly(list));
		}
		return ImmutableList.copyOf(results);
	}

	private static <T> T loadTemplate(String file, Type targetType) {
		try {
			T model = buildGson().fromJson(readResourceFile(file), targetType);
			return model;
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	private static Gson buildGson() {
		GsonBuilder gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z");
		return gsonBuilder
				.registerTypeAdapter(RangerAccessRequest.class, new RangerAccessRequestDeserializer(gsonBuilder))
				.registerTypeAdapter(RangerAccessResource.class, new RangerResourceDeserializer(gsonBuilder))
				.setPrettyPrinting().create();
	}

	public static String readResourceFile(String fileName) {
		try {
			File f = new File(RangerPolicyFactory.class.getResource(fileName).toURI());
			checkState(f.exists() && f.isFile() && f.canRead());
			return Files.toString(f, Charsets.UTF_8);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public static RangerPolicyEngineOptions createPolicyEngineOption() {
		RangerPolicyEngineOptions policyEngineOptions = new RangerPolicyEngineOptions();
		policyEngineOptions.disableTagPolicyEvaluation = false;
		policyEngineOptions.evaluatorType = RangerPolicyEvaluator.EVALUATOR_TYPE_OPTIMIZED;
		policyEngineOptions.cacheAuditResults = false;
		policyEngineOptions.disableTrieLookupPrefilter = true;
		return policyEngineOptions;
	}
}
