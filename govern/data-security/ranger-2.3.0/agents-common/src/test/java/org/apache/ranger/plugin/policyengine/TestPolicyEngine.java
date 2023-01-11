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
import org.apache.ranger.plugin.contextenricher.RangerServiceResourceMatcher;
import org.apache.ranger.plugin.contextenricher.RangerTagEnricher;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyDelta;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.model.validation.RangerValidityScheduleValidator;
import org.apache.ranger.plugin.model.validation.ValidationFailureDetails;
import org.apache.ranger.plugin.policyengine.TestPolicyEngine.PolicyEngineTestCase.TestData;
import org.apache.ranger.plugin.policyevaluator.RangerValidityScheduleEvaluator;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceEvaluator;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerRequestedResources;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.RangerUserStore;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.plugin.util.ServiceTags;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class TestPolicyEngine {
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
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testPolicyEngine_hdfs_resourcespec() {
		String[] hdfsTestResourceFiles = { "/policyengine/test_policyengine_hdfs_resourcespec.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hdfs() {
		String[] hdfsTestResourceFiles = { "/policyengine/test_policyengine_hdfs.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hdfs_allaudit() {
		String[] hdfsTestResourceFiles = { "/policyengine/test_policyengine_hdfs_allaudit.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hdfs_noaudit() {
		String[] hdfsTestResourceFiles = { "/policyengine/test_policyengine_hdfs_noaudit.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hdfsForTag() {
		String[] hdfsTestResourceFiles = { "/policyengine/test_policyengine_tag_hdfs.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}
	@Test
	public void testPolicyEngine_hdfsForZones() {
		String[] hdfsTestResourceFiles = { "/policyengine/test_policyengine_hdfs_zones.json" };

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hive_with_partial_resource_policies() {
		String[] hiveTestResourceFiles = { "/policyengine/test_policyengine_hive_with_partial_resource_policies.json" };

		runTestsFromResourceFiles(hiveTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hive() {
		String[] hiveTestResourceFiles = { "/policyengine/test_policyengine_hive.json" };

		runTestsFromResourceFiles(hiveTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hive_incremental_add() {
		String[] hiveTestResourceFiles = {"/policyengine/test_policyengine_hive_incremental_add.json"};

		runTestsFromResourceFiles(hiveTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hive_incremental_delete() {
		String[] hiveTestResourceFiles = {"/policyengine/test_policyengine_hive_incremental_delete.json"};

		runTestsFromResourceFiles(hiveTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hive_incremental_update() {
		String[] hiveTestResourceFiles = {"/policyengine/test_policyengine_hive_incremental_update.json"};

		runTestsFromResourceFiles(hiveTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hdfs_incremental_update() {
		String[] hdfsTestResourceFiles = {"/policyengine/test_policyengine_hdfs_incremental_update.json"};

		runTestsFromResourceFiles(hdfsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hiveForTag() {
		String[] hiveTestResourceFiles = { "/policyengine/test_policyengine_tag_hive.json" };

		runTestsFromResourceFiles(hiveTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hbase() {
		String[] hbaseTestResourceFiles = { "/policyengine/test_policyengine_hbase.json" };

		runTestsFromResourceFiles(hbaseTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hbase_with_multiple_matching_policies() {
		String[] hbaseTestResourceFiles = { "/policyengine/test_policyengine_hbase_multiple_matching_policies.json" };

		runTestsFromResourceFiles(hbaseTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hbase_namespace() {
		String[] hbaseTestResourceFiles = { "/policyengine/test_policyengine_hbase_namespace.json" };

		runTestsFromResourceFiles(hbaseTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_conditions() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_conditions.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hive_mutex_conditions() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_hive_mutex_conditions.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_resourceAccessInfo() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_resource_access_info.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_geo() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_geo.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_hiveForTag_filebased() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_tag_hive_filebased.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

    @Test
    public void testPolicyEngine_hiveForShowDatabases() {
        String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_tag_hive_for_show_databases.json" };

        runTestsFromResourceFiles(conditionsTestResourceFiles);
    }

	@Test
	public void testPolicyEngine_descendant_tags() {
		String[] resourceFiles = {"/policyengine/test_policyengine_descendant_tags.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_hiveMasking() {
		String[] resourceFiles = {"/policyengine/test_policyengine_hive_mask_filter.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_hiveTagMasking() {
		String[] resourceFiles = {"/policyengine/test_policyengine_tag_hive_mask.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_owner() {
		String[] resourceFiles = {"/policyengine/test_policyengine_owner.json"};

		runTestsFromResourceFiles(resourceFiles);
	}
	@Test
	public void testPolicyEngine_temporary() {
		String[] resourceFiles = {"/policyengine/test_policyengine_temporary.json"};

		TimeZone defaultTZ = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("PST"));

		runTestsFromResourceFiles(resourceFiles);

		TimeZone.setDefault(defaultTZ);
	}

	@Test
	public void testPolicyEngine_atlas() {
		String[] resourceFiles = { "/policyengine/test_policyengine_atlas.json" };

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_policylevel_conditions() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_policylevel_conditions.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_with_roles() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_with_roles.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_with_owner() {
		String[] conditionsTestResourceFiles = { "/policyengine/test_policyengine_hive_default_policies.json" };

		runTestsFromResourceFiles(conditionsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_superUserGroups() {
		String[] resourceFiles = {"/policyengine/test_policyengine_super_user_groups.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_auditExcludeUsersGroupsRoles() {
		String[] resourceFiles = {"/policyengine/test_policyengine_audit_exclude_users_groups_roles.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_PolicyPriority() {
		String[] resourceFiles = {"/policyengine/test_policyengine_priority.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_superUserAccess() {
		String[] resourceFiles = {"/policyengine/test_policyengine_super_user_access.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_auditFilterHdfs() {
		String[] resourceFiles = {"/policyengine/test_policyengine_audit_filter_hdfs.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_descendantTagsDeny() {
		String[] resourceFiles = {"/policyengine/test_policyengine_descendant_tags_deny.json"};

		runTestsFromResourceFiles(resourceFiles);
	}


	@Test
	public void testPolicyEngine_auditFilterHive() {
		String[] resourceFiles = {"/policyengine/test_policyengine_audit_filter_hive.json"};

		runTestsFromResourceFiles(resourceFiles);
	}

	@Test
	public void testPolicyEngine_aws() {
		String[] awsTestResourceFiles = {"/policyengine/test_policyengine_aws.json"};

		runTestsFromResourceFiles(awsTestResourceFiles);
	}

	@Test
	public void testPolicyEngine_resourceWithReqExpressions() {
		String[] resourceFiles = {"/policyengine/test_policyengine_resource_with_req_expressions.json"};

		runTestsFromResourceFiles(resourceFiles);
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

		assertTrue("invalid input: " + testName, testCase != null && testCase.serviceDef != null && testCase.policies != null && testCase.tests != null);

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

		runTestCaseTests(policyEngine, policyEngineForEvaluatingWithACLs, testCase.serviceDef, testName, testCase.tests);

		if (testCase.updatedPolicies != null) {
			servicePolicies.setPolicyDeltas(testCase.updatedPolicies.policyDeltas);
			servicePolicies.setSecurityZones(testCase.updatedPolicies.securityZones);
			RangerPolicyEngine updatedPolicyEngine = RangerPolicyEngineImpl.getPolicyEngine(policyEngine, servicePolicies);
            RangerPolicyEngine updatedPolicyEngineForEvaluatingWithACLs = RangerPolicyEngineImpl.getPolicyEngine(policyEngineForEvaluatingWithACLs, servicePolicies);
			runTestCaseTests(updatedPolicyEngine, updatedPolicyEngineForEvaluatingWithACLs, testCase.serviceDef, testName, testCase.updatedTests);
		}
	}

    private void runTestCaseTests(RangerPolicyEngine policyEngine, RangerPolicyEngine policyEngineForEvaluatingWithACLs, RangerServiceDef serviceDef, String testName, List<TestData> tests) {
        RangerAccessRequest request = null;

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
						System.err.println("TestPolicyEngine.runTests(): error parsing TAGS JSON string in file " + testName + ", tagsJsonString=" +
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
							System.err.println("TestPolicyEngine.runTests(): error parsing REQUESTED_RESOURCES string in file " + testName + ", resourcesJsonString=" +
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

                if (MapUtils.isNotEmpty(test.userAttributes) || MapUtils.isNotEmpty(test.groupAttributes)) {
                    RangerUserStore userStore = new RangerUserStore();

                    userStore.setUserAttrMapping(test.userAttributes);
                    userStore.setGroupAttrMapping(test.groupAttributes);

                    RangerAccessRequestUtil.setRequestUserStoreInContext(request.getContext(), userStore);
                }

				result   = policyEngine.evaluatePolicies(request, RangerPolicy.POLICY_TYPE_ACCESS, auditHandler);

				policyEngine.evaluateAuditPolicies(result);

				assertNotNull("result was null! - " + test.name, result);
				assertEquals("isAllowed mismatched! - " + test.name, expected.getIsAllowed(), result.getIsAllowed());
				assertEquals("isAudited mismatched! - " + test.name, expected.getIsAudited(), result.getIsAudited());

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
		public List<TestData>     tests;
		public Map<String, String> serviceConfig;
		public UpdatedPolicies    updatedPolicies;
		public List<TestData>     updatedTests;
		public Set<String>        superUsers;
		public Set<String>        superGroups;
		public Set<String>        auditExcludedUsers;
		public Set<String>        auditExcludedGroups;
		public Set<String>        auditExcludedRoles;

		class TestData {
			public String              name;
			public RangerAccessRequest request;
			public RangerAccessResult  result;
			public RangerAccessResult  dataMaskResult;
			public RangerAccessResult rowFilterResult;
			public RangerResourceAccessInfo resourceAccessInfo;
			public Map<String, Map<String, String>> userAttributes;
			public Map<String, Map<String, String>> groupAttributes;
		}

		class TagPolicyInfo {
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

    static class ValiditySchedulerTestResult {
        boolean isValid;
        int validationFailureCount;
        boolean isApplicable;
    }

    static class ValiditySchedulerTestCase {
        String name;
        List<RangerValiditySchedule> validitySchedules;
        Date accessTime;
        ValiditySchedulerTestResult result;
    }

    @Test
    public void testValiditySchedulerInvalid() {
        String resourceName = "/policyengine/validityscheduler/test-validity-schedules-invalid.json";

        runValiditySchedulerTests(resourceName);
    }

    @Test
    public void testValiditySchedulerValid() {
        String resourceName = "/policyengine/validityscheduler/test-validity-schedules-valid.json";

        runValiditySchedulerTests(resourceName);
    }

    @Test
    public void testValiditySchedulerApplicable() {
        String resourceName = "/policyengine/validityscheduler/test-validity-schedules-valid-and-applicable.json";

        runValiditySchedulerTests(resourceName);
    }

    private void runValiditySchedulerTests(String resourceName) {
        TimeZone defaultTZ = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));

        List<ValiditySchedulerTestCase> testCases = null;

        InputStream inStream = this.getClass().getResourceAsStream(resourceName);
        InputStreamReader reader   = new InputStreamReader(inStream);
        try {
            Type listType = new TypeToken<List<ValiditySchedulerTestCase>>() {}.getType();
            testCases = gsonBuilder.fromJson(reader, listType);
        } catch (Exception e) {
            assertFalse("Exception in reading validity-scheduler test cases.", true);
        }

        assertNotNull("TestCases are null!", testCases);


        if (CollectionUtils.isNotEmpty(testCases)) {
            for (ValiditySchedulerTestCase testCase : testCases) {
                boolean isValid = true;
                List<ValidationFailureDetails> validationFailures = new ArrayList<>();
                boolean isApplicable = false;

                List<RangerValiditySchedule> validatedSchedules = new ArrayList<>();

                for (RangerValiditySchedule validitySchedule : testCase.validitySchedules) {
                    RangerValidityScheduleValidator validator = new RangerValidityScheduleValidator(validitySchedule);
                    RangerValiditySchedule validatedSchedule = validator.validate(validationFailures);
                    isValid = isValid && validatedSchedule != null;
                    if (isValid) {
                        validatedSchedules.add(validatedSchedule);
                    }
                }
                if (isValid) {
                    for (RangerValiditySchedule validSchedule : validatedSchedules) {
                        isApplicable = new RangerValidityScheduleEvaluator(validSchedule).isApplicable(testCase.accessTime.getTime());
                        if (isApplicable) {
                            break;
                        }
                    }
                }

                assertTrue(testCase.name, isValid == testCase.result.isValid);
                assertTrue(testCase.name, isApplicable == testCase.result.isApplicable);
                assertTrue(testCase.name + ", [" + validationFailures +"]", validationFailures.size() == testCase.result.validationFailureCount);
            }
        }
        TimeZone.setDefault(defaultTZ);
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

	// Test utility functions
	public static boolean compare(PolicyEngine me, PolicyEngine other) {
		boolean ret;

		if (me.getPolicyRepository() != null && other.getPolicyRepository() != null) {
			ret = compare(me.getPolicyRepository(), other.getPolicyRepository());
		} else {
			ret = me.getPolicyRepository() == other.getPolicyRepository();
		}

		if (ret) {
			if (me.getTagPolicyRepository() != null && other.getTagPolicyRepository() != null) {
				ret = compare(me.getTagPolicyRepository(), other.getTagPolicyRepository());
			} else {
				ret = me.getTagPolicyRepository() == other.getTagPolicyRepository();
			}
		}

		if (ret) {
			ret = Objects.equals(me.getResourceZoneTrie().keySet(), other.getResourceZoneTrie().keySet());

			if (ret) {
				for (Map.Entry<String, RangerResourceTrie> entry : me.getResourceZoneTrie().entrySet()) {
					ret = compareSubtree(entry.getValue(), other.getResourceZoneTrie().get(entry.getKey()));

					if (!ret) {
						break;
					}
				}
			}
		}

		if (ret) {
			ret = Objects.equals(me.getZonePolicyRepositories().keySet(), other.getZonePolicyRepositories().keySet());

			if (ret) {
				for (Map.Entry<String, RangerPolicyRepository> entry : me.getZonePolicyRepositories().entrySet()) {
					ret = compare(entry.getValue(), other.getZonePolicyRepositories().get(entry.getKey()));

					if (!ret) {
						break;
					}
				}
			}
		}

		return ret;
	}

	public static boolean compare(RangerPolicyRepository me, RangerPolicyRepository other) {
		return compareTrie(RangerPolicy.POLICY_TYPE_ACCESS, me, other) &&
				compareTrie(RangerPolicy.POLICY_TYPE_DATAMASK, me, other) &&
				compareTrie(RangerPolicy.POLICY_TYPE_ROWFILTER, me, other);
	}

	public static boolean compareTrie(final int policyType, RangerPolicyRepository me, RangerPolicyRepository other) {
		boolean ret;

		Map<String, RangerResourceTrie> myTrie    = me.getTrie(policyType);
		Map<String, RangerResourceTrie> otherTrie = other.getTrie(policyType);

		ret = myTrie.size() == otherTrie.size();

		if (ret) {
			for (Map.Entry<String, RangerResourceTrie> entry : myTrie.entrySet()) {
				RangerResourceTrie myResourceTrie    = entry.getValue();
				RangerResourceTrie otherResourceTrie = otherTrie.get(entry.getKey());

				ret = otherResourceTrie != null && compareSubtree(myResourceTrie, otherResourceTrie);

				if (!ret) {
					break;
				}
			}
		}

		return ret;
	}

	public static boolean compare(RangerTagEnricher me, RangerTagEnricher other) {
		boolean ret;

		if (me.getEnrichedServiceTags() == null || other == null || other.getEnrichedServiceTags() == null) {
			return false;
		}

		if (me.getEnrichedServiceTags().getServiceResourceTrie() != null && other.getEnrichedServiceTags().getServiceResourceTrie() != null) {
			ret = me.getEnrichedServiceTags().getServiceResourceTrie().size() == other.getEnrichedServiceTags().getServiceResourceTrie().size();

			if (ret && me.getEnrichedServiceTags().getServiceResourceTrie().size() > 0) {
				for (Map.Entry<String, RangerResourceTrie<RangerServiceResourceMatcher>> entry : me.getEnrichedServiceTags().getServiceResourceTrie().entrySet()) {
					ret = compareSubtree(entry.getValue(), other.getEnrichedServiceTags().getServiceResourceTrie().get(entry.getKey()));
					if (!ret) {
						break;
					}
				}
			}
		} else {
			ret = me.getEnrichedServiceTags().getServiceResourceTrie() == other.getEnrichedServiceTags().getServiceResourceTrie();
		}

		if (ret) {
			// Compare mappings
			ServiceTags myServiceTags = me.getEnrichedServiceTags().getServiceTags();
			ServiceTags otherServiceTags = other.getEnrichedServiceTags().getServiceTags();

			ret = StringUtils.equals(myServiceTags.getServiceName(), otherServiceTags.getServiceName()) &&
					//myServiceTags.getTagVersion().equals(otherServiceTags.getTagVersion()) &&
					myServiceTags.getTags().size() == otherServiceTags.getTags().size() &&
					myServiceTags.getServiceResources().size() == otherServiceTags.getServiceResources().size() &&
					myServiceTags.getResourceToTagIds().size() == otherServiceTags.getResourceToTagIds().size();
			if (ret) {
				for (RangerServiceResource serviceResource : myServiceTags.getServiceResources()) {
					Long serviceResourceId = serviceResource.getId();

					List<Long> myTagsForResource = myServiceTags.getResourceToTagIds().get(serviceResourceId);
					List<Long> otherTagsForResource = otherServiceTags.getResourceToTagIds().get(serviceResourceId);

					ret = CollectionUtils.size(myTagsForResource) == CollectionUtils.size(otherTagsForResource);

					if (ret && CollectionUtils.size(myTagsForResource) > 0) {
						ret = myTagsForResource.size() == CollectionUtils.intersection(myTagsForResource, otherTagsForResource).size();
					}
				}
			}
		}

		return ret;
	}

	public static boolean compareSubtree(RangerResourceTrie me, RangerResourceTrie other) {

		final boolean ret;
		List<RangerResourceTrie.TrieNode> mismatchedNodes = new ArrayList<>();

		if (me.getRoot() == null || other.getRoot() == null) {
			ret = me.getRoot() == other.getRoot();
			if (!ret) {
				mismatchedNodes.add(me.getRoot());
			}
		} else {
			ret = compareSubtree(me.getRoot(), other.getRoot(), mismatchedNodes);
		}
		return ret;
	}

	private static boolean compareSubtree(RangerResourceTrie.TrieNode me, RangerResourceTrie.TrieNode other, List<RangerResourceTrie.TrieNode> misMatched) {
		boolean ret = StringUtils.equals(me.getStr(), other.getStr());

		if (ret) {
			Map<Character, RangerResourceTrie.TrieNode> myChildren = me.getChildren();
			Map<Character, RangerResourceTrie.TrieNode> otherChildren = other.getChildren();

			ret = myChildren.size() == otherChildren.size() &&
					compareLists(me.getEvaluators(), other.getEvaluators()) &&
					compareLists(me.getWildcardEvaluators(), other.getWildcardEvaluators()) &&
					myChildren.keySet().size() == otherChildren.keySet().size();
			if (ret) {
				// Check if subtrees match
				for (Map.Entry<Character, RangerResourceTrie.TrieNode> entry : myChildren.entrySet()) {
					Character c = entry.getKey();
					RangerResourceTrie.TrieNode myNode = entry.getValue();
					RangerResourceTrie.TrieNode otherNode = otherChildren.get(c);
					ret = otherNode != null && compareSubtree(myNode, otherNode, misMatched);
					if (!ret) {
						break;
					}
				}
			}
		}

		if (!ret) {
			misMatched.add(me);
		}

		return ret;
	}

	private static boolean compareLists(Set me, Set other) {
		boolean ret;

		if (me == null || other == null) {
			ret = me == other;
		} else {
			ret = me.size() == other.size();

			if (ret) {
				List<? extends RangerPolicyResourceEvaluator> meAsList = new ArrayList<>(me);
				List<? extends RangerPolicyResourceEvaluator> otherAsList = new ArrayList<>(other);

				List<Long> myIds = new ArrayList<>();
				List<Long> otherIds = new ArrayList<>();
				for (RangerPolicyResourceEvaluator evaluator : meAsList) {
					myIds.add(evaluator.getId());
				}
				for (RangerPolicyResourceEvaluator evaluator : otherAsList) {
					otherIds.add(evaluator.getId());
				}

				ret = compareLongLists(myIds, otherIds);
			}
		}
		return ret;
	}

	private static boolean compareLongLists(List<Long> me, List<Long> other) {
		return me.size() == CollectionUtils.intersection(me, other).size();
	}


}

