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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.audit.provider.AuditHandler;
import org.apache.ranger.audit.provider.AuditProviderFactory;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.TestPolicyEngineForDeltas.PolicyEngineTestCase.TestData;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerRequestedResources;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class TestPolicyEngineForDeltas {
	static RangerPluginContext pluginContext;
	static Gson gsonBuilder;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		pluginContext = new RangerPluginContext(new RangerPluginConfig("hive", null, "hive", "cl1", "on-prem", null));

		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSSZ")
				.setPrettyPrinting()
				.registerTypeAdapter(RangerAccessRequest.class, new RangerAccessRequestDeserializer())
				.registerTypeAdapter(RangerAccessResource.class, new RangerResourceDeserializer())
				.create();

		// For setting up auditProvider
		Properties auditProperties = new Properties();

		String AUDIT_PROPERTIES_FILE = "xasecure-audit.properties";

		File propFile = new File(AUDIT_PROPERTIES_FILE);

		if (propFile.exists()) {
			System.out.println("Loading Audit properties file" + AUDIT_PROPERTIES_FILE);

			auditProperties.load(new FileInputStream(propFile));
		} else {
			System.out.println("Audit properties file missing: " + AUDIT_PROPERTIES_FILE);

			auditProperties.setProperty("xasecure.audit.is.enabled", "false"); // Set this to true to enable audit logging
			auditProperties.setProperty("xasecure.audit.log4j.is.enabled", "false");
			auditProperties.setProperty("xasecure.audit.log4j.is.async", "false");
			auditProperties.setProperty("xasecure.audit.log4j.async.max.queue.size", "100000");
			auditProperties.setProperty("xasecure.audit.log4j.async.max.flush.interval.ms", "30000");
		}

		AuditProviderFactory factory = AuditProviderFactory.getInstance();
		factory.init(auditProperties, "hdfs"); // second parameter does not matter for v2

		AuditHandler provider = factory.getAuditProvider();

		System.out.println("provider=" + provider.toString());

		File file = File.createTempFile("ranger-admin-test-site", ".xml");
		file.deleteOnExit();

		FileOutputStream outStream = new FileOutputStream(file);
		OutputStreamWriter writer = new OutputStreamWriter(outStream);

		/*
		// For setting up TestTagProvider

		writer.write("<configuration>\n" +
				"        <property>\n" +
				"                <name>ranger.plugin.tag.policy.rest.url</name>\n" +
				"                <value>http://os-def:6080</value>\n" +
				"        </property>\n" +
				"        <property>\n" +
				"                <name>ranger.externalurl</name>\n" +
				"                <value>http://os-def:6080</value>\n" +
				"        </property>\n" +
				"</configuration>\n");
				*/

		writer.write("<configuration>\n" +
				/*
				// For setting up TestTagProvider
				"        <property>\n" +
				"                <name>ranger.plugin.tag.policy.rest.url</name>\n" +
				"                <value>http://os-def:6080</value>\n" +
				"        </property>\n" +
				"        <property>\n" +
				"                <name>ranger.externalurl</name>\n" +
				"                <value>http://os-def:6080</value>\n" +
				"        </property>\n" +
				*/
				// For setting up x-forwarded-for for Hive
				"        <property>\n" +
				"                <name>ranger.plugin.hive.use.x-forwarded-for.ipaddress</name>\n" +
				"                <value>true</value>\n" +
				"        </property>\n" +
				"        <property>\n" +
				"                <name>ranger.plugin.hive.trusted.proxy.ipaddresses</name>\n" +
				"                <value>255.255.255.255; 128.101.101.101;128.101.101.99</value>\n" +
				"        </property>\n" +
				"        <property>\n" +
				"                <name>ranger.plugin.tag.attr.additional.date.formats</name>\n" +
				"                <value>abcd||xyz||yyyy/MM/dd'T'HH:mm:ss.SSS'Z'</value>\n" +
				"        </property>\n" +
				"        <property>\n" +
				"                <name>ranger.policyengine.trie.builder.thread.count</name>\n" +
				"                <value>3</value>\n" +
				"        </property>\n" +
                "</configuration>\n");
		writer.close();

		pluginContext.getConfig().addResource(new org.apache.hadoop.fs.Path(file.toURI()));
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Test
	public void testPolicyEngine_hdfs_incremental_delete() {
		String[] hdfsTestResourceFiles = {"/policyengine/test_policyengine_hdfs_incremental_delete.json"};

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	private void runTestsFromResourceFiles(String[] resourceNames) {
		for(String resourceName : resourceNames) {
			InputStream inStream = this.getClass().getResourceAsStream(resourceName);
			InputStreamReader reader   = new InputStreamReader(inStream);

			runTests(reader, resourceName);
		}
	}

	private void runTests(InputStreamReader reader, String testName) {
		PolicyEngineTestCase testCase = gsonBuilder.fromJson(reader, PolicyEngineTestCase.class);

		assertTrue("invalid input: " + testName, testCase != null && testCase.serviceDef != null && testCase.policies != null && testCase.testsInfo != null && testCase.testsInfo.tests != null);

		ServicePolicies servicePolicies = new ServicePolicies();
		servicePolicies.setPolicyVersion(100L);
		servicePolicies.setServiceName(testCase.serviceName);
		servicePolicies.setServiceDef(testCase.serviceDef);
		servicePolicies.setPolicies(testCase.policies);
		servicePolicies.setSecurityZones(testCase.securityZones);
		servicePolicies.setServiceConfig(testCase.serviceConfig);

		if (StringUtils.isNotBlank(testCase.auditMode)) {
			servicePolicies.setAuditMode(testCase.auditMode);
		}

		if (null != testCase.tagPolicyInfo) {
			ServicePolicies.TagPolicies tagPolicies = new ServicePolicies.TagPolicies();
			tagPolicies.setServiceName(testCase.tagPolicyInfo.serviceName);
			tagPolicies.setServiceDef(testCase.tagPolicyInfo.serviceDef);
			tagPolicies.setPolicies(testCase.tagPolicyInfo.tagPolicies);
			tagPolicies.setServiceConfig(testCase.tagPolicyInfo.serviceConfig);

			if (StringUtils.isNotBlank(testCase.auditMode)) {
				tagPolicies.setAuditMode(testCase.auditMode);
			}
			servicePolicies.setTagPolicies(tagPolicies);
		}

		boolean useForwardedIPAddress = pluginContext.getConfig().getBoolean("ranger.plugin.hive.use.x-forwarded-for.ipaddress", false);
		String trustedProxyAddressString = pluginContext.getConfig().get("ranger.plugin.hive.trusted.proxy.ipaddresses");
		String[] trustedProxyAddresses = StringUtils.split(trustedProxyAddressString, ';');
		if (trustedProxyAddresses != null) {
			for (int i = 0; i < trustedProxyAddresses.length; i++) {
				trustedProxyAddresses[i] = trustedProxyAddresses[i].trim();
			}
		}

		RangerRoles roles = new RangerRoles();
		roles.setServiceName(testCase.serviceName);
		roles.setRoleVersion(-1L);
		Set<RangerRole> rolesSet = new HashSet<>();

		Map<String, Set<String>> userRoleMapping = testCase.userRoles;
		Map<String, Set<String>> groupRoleMapping = testCase.groupRoles;
		Map<String, Set<String>> roleRoleMapping = testCase.roleRoles;
		if (userRoleMapping != null) {
			for (Map.Entry<String, Set<String>> userRole : userRoleMapping.entrySet()) {
				String user = userRole.getKey();
				Set<String> userRoles = userRole.getValue();
				RangerRole.RoleMember userRoleMember = new RangerRole.RoleMember(user, true);
				List<RangerRole.RoleMember> userRoleMembers = Arrays.asList(userRoleMember);
				for (String usrRole : userRoles) {
					RangerRole rangerUserRole = new RangerRole(usrRole, usrRole, null, userRoleMembers, null);
					rolesSet.add(rangerUserRole);
				}
			}
		}

		if (groupRoleMapping != null) {
			for (Map.Entry<String, Set<String>> groupRole : groupRoleMapping.entrySet()) {
				String group = groupRole.getKey();
				Set<String> groupRoles = groupRole.getValue();
				RangerRole.RoleMember groupRoleMember = new RangerRole.RoleMember(group, true);
				List<RangerRole.RoleMember> groupRoleMembers = Arrays.asList(groupRoleMember);
				for (String grpRole : groupRoles) {
					RangerRole rangerGroupRole = new RangerRole(grpRole, grpRole, null, null, groupRoleMembers);
					rolesSet.add(rangerGroupRole);
				}
			}
		}

		if (roleRoleMapping != null) {
			for (Map.Entry<String, Set<String>> roleRole : roleRoleMapping.entrySet()) {
				String role = roleRole.getKey();
				Set<String> roleRoles = roleRole.getValue();
				RangerRole.RoleMember roleRoleMember = new RangerRole.RoleMember(role, true);
				List<RangerRole.RoleMember> roleRoleMembers = Arrays.asList(roleRoleMember);
				for (String rleRole : roleRoles) {
					RangerRole rangerRoleRole = new RangerRole(rleRole, rleRole, null, null, null, roleRoleMembers);
					rolesSet.add(rangerRoleRole);
				}
			}
		}

		roles.setRangerRoles(rolesSet);

        RangerPolicyEngineOptions policyEngineOptions = pluginContext.getConfig().getPolicyEngineOptions();

        policyEngineOptions.disableAccessEvaluationWithPolicyACLSummary = true;

        setPluginConfig(pluginContext.getConfig(), ".super.users", testCase.superUsers);
        setPluginConfig(pluginContext.getConfig(), ".super.groups", testCase.superGroups);
        setPluginConfig(pluginContext.getConfig(), ".audit.exclude.users", testCase.auditExcludedUsers);
        setPluginConfig(pluginContext.getConfig(), ".audit.exclude.groups", testCase.auditExcludedGroups);
        setPluginConfig(pluginContext.getConfig(), ".audit.exclude.roles", testCase.auditExcludedRoles);

        // so that setSuperUsersAndGroups(), setAuditExcludedUsersGroupsRoles() will be called on the pluginConfig
        new RangerBasePlugin(pluginContext.getConfig());

        RangerPolicyEngineImpl policyEngine = new RangerPolicyEngineImpl(servicePolicies, pluginContext, roles);

        policyEngine.setUseForwardedIPAddress(useForwardedIPAddress);
        policyEngine.setTrustedProxyAddresses(trustedProxyAddresses);

        policyEngineOptions.disableAccessEvaluationWithPolicyACLSummary = false;

		RangerPolicyEngineImpl policyEngineForEvaluatingWithACLs = new RangerPolicyEngineImpl(servicePolicies, pluginContext, roles);

		policyEngineForEvaluatingWithACLs.setUseForwardedIPAddress(useForwardedIPAddress);
		policyEngineForEvaluatingWithACLs.setTrustedProxyAddresses(trustedProxyAddresses);

		PolicyEngineTestCase.TestsInfo testsInfo = testCase.testsInfo;
		do {
			runTestCaseTests(policyEngine, policyEngineForEvaluatingWithACLs, testCase.serviceDef, testName, testsInfo.tests);
			if (testsInfo.updatedPolicies != null && CollectionUtils.isNotEmpty(testsInfo.updatedPolicies.policyDeltas)) {
				servicePolicies.setPolicyDeltas(testsInfo.updatedPolicies.policyDeltas);
				servicePolicies.setPolicies(null);
				if (MapUtils.isNotEmpty(testsInfo.updatedPolicies.securityZones)) {
					servicePolicies.setSecurityZones(testsInfo.updatedPolicies.securityZones);
				}
				policyEngine = (RangerPolicyEngineImpl) RangerPolicyEngineImpl.getPolicyEngine(policyEngine, servicePolicies);
				policyEngineForEvaluatingWithACLs = (RangerPolicyEngineImpl) RangerPolicyEngineImpl.getPolicyEngine(policyEngineForEvaluatingWithACLs, servicePolicies);
				if (policyEngine != null && policyEngineForEvaluatingWithACLs != null) {
					testsInfo = testsInfo.updatedTestsInfo;
				} else {
					testsInfo = null;
				}
			} else {
				testsInfo = null;
			}

		} while (testsInfo != null && testsInfo.tests != null);

	}

    private void runTestCaseTests(RangerPolicyEngine policyEngine, RangerPolicyEngine policyEngineForEvaluatingWithACLs, RangerServiceDef serviceDef, String testName, List<TestData> tests) {
        RangerAccessRequest request;

        for(TestData test : tests) {
			request = test.request;

			if (request.getContext().containsKey(RangerAccessRequestUtil.KEY_CONTEXT_TAGS) ||
					request.getContext().containsKey(RangerAccessRequestUtil.KEY_CONTEXT_REQUESTED_RESOURCES)) {
				// Create a new AccessRequest
				RangerAccessRequestImpl newRequest =
						new RangerAccessRequestImpl(request.getResource(), request.getAccessType(),
								request.getUser(), request.getUserGroups(), null);

				newRequest.setClientType(request.getClientType());
				newRequest.setAccessTime(request.getAccessTime());
				newRequest.setAction(request.getAction());
				newRequest.setRemoteIPAddress(request.getRemoteIPAddress());
				newRequest.setForwardedAddresses(request.getForwardedAddresses());
				newRequest.setRequestData(request.getRequestData());
				newRequest.setSessionId(request.getSessionId());

				Map<String, Object> context = request.getContext();
				String tagsJsonString = (String) context.get(RangerAccessRequestUtil.KEY_CONTEXT_TAGS);
				context.remove(RangerAccessRequestUtil.KEY_CONTEXT_TAGS);

				if(!StringUtils.isEmpty(tagsJsonString)) {
					try {
						Type setType = new TypeToken<Set<RangerTagForEval>>() {
						}.getType();
						Set<RangerTagForEval> tags = gsonBuilder.fromJson(tagsJsonString, setType);

						context.put(RangerAccessRequestUtil.KEY_CONTEXT_TAGS, tags);
					} catch (Exception e) {
						System.err.println("TestPolicyEngineForDeltas.runTests(): error parsing TAGS JSON string in file " + testName + ", tagsJsonString=" +
								tagsJsonString + ", exception=" + e);
					}
				} else if (request.getContext().containsKey(RangerAccessRequestUtil.KEY_CONTEXT_REQUESTED_RESOURCES)) {
					String resourcesJsonString = (String) context.get(RangerAccessRequestUtil.KEY_CONTEXT_REQUESTED_RESOURCES);
					context.remove(RangerAccessRequestUtil.KEY_CONTEXT_REQUESTED_RESOURCES);
					if (!StringUtils.isEmpty(resourcesJsonString)) {
						try {
							/*
							Reader stringReader = new StringReader(resourcesJsonString);
							RangerRequestedResources resources = gsonBuilder.fromJson(stringReader, RangerRequestedResources.class);
							*/

							Type myType = new TypeToken<RangerRequestedResources>() {
							}.getType();
							RangerRequestedResources resources = gsonBuilder.fromJson(resourcesJsonString, myType);

							context.put(RangerAccessRequestUtil.KEY_CONTEXT_REQUESTED_RESOURCES, resources);
						} catch (Exception e) {
							System.err.println("TestPolicyEngineForDeltas.runTests(): error parsing REQUESTED_RESOURCES string in file " + testName + ", resourcesJsonString=" +
									resourcesJsonString + ", exception=" + e);
						}
					}
				}
				newRequest.setContext(context);

				// accessResource.ServiceDef is set here, so that we can skip call to policyEngine.preProcess() which
				// sets the serviceDef in the resource AND calls enrichers. We dont want enrichers to be called when
				// context already contains tags -- This may change when we want enrichers to enrich request in the
				// presence of tags!!!

				// Safe cast
				RangerAccessResourceImpl accessResource = (RangerAccessResourceImpl) request.getResource();
				accessResource.setServiceDef(serviceDef);

				request = newRequest;

			}

			RangerAccessResultProcessor auditHandler = new RangerDefaultAuditHandler();

			if(test.result != null) {
                RangerAccessResult expected = test.result;
                RangerAccessResult result;

				result   = policyEngine.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_ACCESS, auditHandler);

				policyEngine.evaluateAuditPolicies(result);

				assertNotNull("result was null! - " + test.name, result);
				assertEquals("isAllowed mismatched! - " + test.name, expected.getIsAllowed(), result.getIsAllowed());
				assertEquals("policy-id mismatched! - " + test.name, expected.getPolicyId(), result.getPolicyId());
				assertEquals("isAudited mismatched! - " + test.name, expected.getIsAudited(), result.getIsAudited() && result.getIsAuditedDetermined());

				result   = policyEngineForEvaluatingWithACLs.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_ACCESS, auditHandler);

				policyEngine.evaluateAuditPolicies(result);

                assertNotNull("result was null! - " + test.name, result);
                assertEquals("isAllowed mismatched! - " + test.name, expected.getIsAllowed(), result.getIsAllowed());
                assertEquals("isAudited mismatched! - " + test.name, expected.getIsAudited(), result.getIsAudited());
			}

			if(test.dataMaskResult != null) {
				RangerAccessResult expected = test.dataMaskResult;
				RangerAccessResult result;

                result   = policyEngine.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_DATAMASK, auditHandler);

                policyEngine.evaluateAuditPolicies(result);

                assertNotNull("result was null! - " + test.name, result);
                assertEquals("maskType mismatched! - " + test.name, expected.getMaskType(), result.getMaskType());
                assertEquals("maskCondition mismatched! - " + test.name, expected.getMaskCondition(), result.getMaskCondition());
                assertEquals("maskedValue mismatched! - " + test.name, expected.getMaskedValue(), result.getMaskedValue());
                assertEquals("policyId mismatched! - " + test.name, expected.getPolicyId(), result.getPolicyId());

                result = policyEngineForEvaluatingWithACLs.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_DATAMASK, auditHandler);

                policyEngine.evaluateAuditPolicies(result);

				assertNotNull("result was null! - " + test.name, result);
				assertEquals("maskType mismatched! - " + test.name, expected.getMaskType(), result.getMaskType());
				assertEquals("maskCondition mismatched! - " + test.name, expected.getMaskCondition(), result.getMaskCondition());
				assertEquals("maskedValue mismatched! - " + test.name, expected.getMaskedValue(), result.getMaskedValue());
				assertEquals("policyId mismatched! - " + test.name, expected.getPolicyId(), result.getPolicyId());

			}

			if(test.rowFilterResult != null) {
				RangerAccessResult expected = test.rowFilterResult;
				RangerAccessResult result;

                result   = policyEngine.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_ROWFILTER, auditHandler);

                policyEngine.evaluateAuditPolicies(result);

                assertNotNull("result was null! - " + test.name, result);
                assertEquals("filterExpr mismatched! - " + test.name, expected.getFilterExpr(), result.getFilterExpr());
                assertEquals("policyId mismatched! - " + test.name, expected.getPolicyId(), result.getPolicyId());

				result = policyEngineForEvaluatingWithACLs.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_ROWFILTER, auditHandler);

				policyEngine.evaluateAuditPolicies(result);

				assertNotNull("result was null! - " + test.name, result);
				assertEquals("filterExpr mismatched! - " + test.name, expected.getFilterExpr(), result.getFilterExpr());
				assertEquals("policyId mismatched! - " + test.name, expected.getPolicyId(), result.getPolicyId());

			}

			if(test.resourceAccessInfo != null) {

				RangerResourceAccessInfo expected = new RangerResourceAccessInfo(test.resourceAccessInfo);
				RangerResourceAccessInfo result   = policyEngine.getResourceAccessInfo(test.request);

				assertNotNull("result was null! - " + test.name, result);
				assertEquals("allowedUsers mismatched! - " + test.name, expected.getAllowedUsers(), result.getAllowedUsers());
				assertEquals("allowedGroups mismatched! - " + test.name, expected.getAllowedGroups(), result.getAllowedGroups());
				assertEquals("deniedUsers mismatched! - " + test.name, expected.getDeniedUsers(), result.getDeniedUsers());
				assertEquals("deniedGroups mismatched! - " + test.name, expected.getDeniedGroups(), result.getDeniedGroups());
			}
		}

	}

	private void setPluginConfig(RangerPluginConfig conf, String suffix, Set<String> value) {
		conf.set(conf.getPropertyPrefix() + suffix, CollectionUtils.isNotEmpty(value) ? StringUtils.join(value, ',') : "");
	}

	static class PolicyEngineTestCase {
		public String             serviceName;
		public RangerServiceDef   serviceDef;
		public List<RangerPolicy> policies;
		public TagPolicyInfo	  tagPolicyInfo;
		public Map<String, ServicePolicies.SecurityZoneInfo> securityZones;
		public Map<String, Set<String>> userRoles;
		public Map<String, Set<String>> groupRoles;
		public Map<String, Set<String>> roleRoles;
		public String             auditMode;
		public TestsInfo          testsInfo;
		//public List<TestData>     tests;
		public Map<String, String> serviceConfig;
		//public UpdatedPolicies    updatedPolicies;
		//public List<TestData>     updatedTests;
		public Set<String>        superUsers;
		public Set<String>        superGroups;
		public Set<String>        auditExcludedUsers;
		public Set<String>        auditExcludedGroups;
		public Set<String>        auditExcludedRoles;

		static class TestsInfo {
			public List<TestData>     tests;
			public UpdatedPolicies    updatedPolicies;
			public TestsInfo          updatedTestsInfo;

		}
		static class TestData {
			public String              name;
			public RangerAccessRequest request;
			public RangerAccessResult  result;
			public RangerAccessResult  dataMaskResult;
			public RangerAccessResult rowFilterResult;
			public RangerResourceAccessInfo resourceAccessInfo;
		}

		static class TagPolicyInfo {
			public String	serviceName;
			public RangerServiceDef serviceDef;
			public Map<String, String> serviceConfig;
			public List<RangerPolicy> tagPolicies;
		}
	}

	static class UpdatedPolicies {
		public Map<String, ServicePolicies.SecurityZoneInfo> securityZones;
		public List<RangerPolicyDelta>                       policyDeltas;
	}

    static class RangerAccessRequestDeserializer implements JsonDeserializer<RangerAccessRequest> {
		@Override
		public RangerAccessRequest deserialize(JsonElement jsonObj, Type type,
				JsonDeserializationContext context) throws JsonParseException {
			RangerAccessRequestImpl ret = gsonBuilder.fromJson(jsonObj, RangerAccessRequestImpl.class);

			ret.setAccessType(ret.getAccessType()); // to force computation of isAccessTypeAny and isAccessTypeDelegatedAdmin
			if (ret.getAccessTime() == null) {
				ret.setAccessTime(new Date());
			}

			return ret;
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

