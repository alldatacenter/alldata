/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ranger.biz.SecurityZoneDBStore;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerSecurityZoneHeaderInfo;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceHeaderInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.view.RangerPolicyList;
import org.apache.ranger.view.RangerServiceDefList;
import org.apache.ranger.view.RangerServiceList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPublicAPIsv2 {

	private static Long Id = 8L;
	
	private static Long Id2 =10L;

	@InjectMocks
	PublicAPIsv2 publicAPIsv2 = new PublicAPIsv2();

	@Mock
	ServiceREST serviceREST;

	@Mock
	RangerSearchUtil searchUtil;

	@Mock
	RangerPolicyService policyService;

	@Mock
	RESTErrorUtil restErrorUtil;

	@Mock
	SecurityZoneDBStore securityZoneStore;

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Before
	public void setup() throws Exception {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil
				.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
	}

	private RangerServiceDef rangerServiceDef() {
		List<RangerServiceConfigDef> configs = new ArrayList<RangerServiceConfigDef>();
		List<RangerResourceDef> resources = new ArrayList<RangerResourceDef>();
		List<RangerAccessTypeDef> accessTypes = new ArrayList<RangerAccessTypeDef>();
		List<RangerPolicyConditionDef> policyConditions = new ArrayList<RangerPolicyConditionDef>();
		List<RangerContextEnricherDef> contextEnrichers = new ArrayList<RangerContextEnricherDef>();
		List<RangerEnumDef> enums = new ArrayList<RangerEnumDef>();

		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setId(Id);
		rangerServiceDef.setName("RangerServiceHdfs");
		rangerServiceDef.setImplClass("RangerServiceHdfs");
		rangerServiceDef.setLabel("HDFS Repository");
		rangerServiceDef.setDescription("HDFS Repository");
		rangerServiceDef.setRbKeyDescription(null);
		rangerServiceDef.setUpdatedBy("Admin");
		rangerServiceDef.setUpdateTime(new Date());
		rangerServiceDef.setConfigs(configs);
		rangerServiceDef.setResources(resources);
		rangerServiceDef.setAccessTypes(accessTypes);
		rangerServiceDef.setPolicyConditions(policyConditions);
		rangerServiceDef.setContextEnrichers(contextEnrichers);
		rangerServiceDef.setEnums(enums);

		return rangerServiceDef;
	}
	
	private RangerService rangerService() {
		Map<String, String> configs = new HashMap<String, String>();
		configs.put("username", "servicemgr");
		configs.put("password", "servicemgr");
		configs.put("namenode", "servicemgr");
		configs.put("hadoop.security.authorization", "No");
		configs.put("hadoop.security.authentication", "Simple");
		configs.put("hadoop.security.auth_to_local", "");
		configs.put("dfs.datanode.kerberos.principal", "");
		configs.put("dfs.namenode.kerberos.principal", "");
		configs.put("dfs.secondary.namenode.kerberos.principal", "");
		configs.put("hadoop.rpc.protection", "Privacy");
		configs.put("commonNameForCertificate", "");

		RangerService rangerService = new RangerService();
		rangerService.setId(Id);
		rangerService.setConfigs(configs);
		rangerService.setCreateTime(new Date());
		rangerService.setDescription("service policy");
		rangerService.setGuid("1427365526516_835_0");
		rangerService.setIsEnabled(true);
		rangerService.setName("HDFS_1");
		rangerService.setPolicyUpdateTime(new Date());
		rangerService.setType("1");
		rangerService.setUpdatedBy("Admin");
		rangerService.setUpdateTime(new Date());

		return rangerService;
	}
	
	private RangerPolicy rangerPolicy() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
		List<String> users = new ArrayList<String>();
		List<String> groups = new ArrayList<String>();
		List<RangerPolicyItemCondition> conditions = new ArrayList<RangerPolicyItemCondition>();
		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicyItem>();
		RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
		rangerPolicyItem.setAccesses(accesses);
		rangerPolicyItem.setConditions(conditions);
		rangerPolicyItem.setGroups(groups);
		rangerPolicyItem.setUsers(users);
		rangerPolicyItem.setDelegateAdmin(false);

		policyItems.add(rangerPolicyItem);

		Map<String, RangerPolicyResource> policyResource = new HashMap<String, RangerPolicyResource>();
		RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
		rangerPolicyResource.setIsExcludes(true);
		rangerPolicyResource.setIsRecursive(true);
		rangerPolicyResource.setValue("1");
		rangerPolicyResource.setValues(users);
		policyResource.put("resource", rangerPolicyResource);
		RangerPolicy policy = new RangerPolicy();
		policy.setId(Id);
		policy.setCreateTime(new Date());
		policy.setDescription("policy");
		policy.setGuid("policyguid");
		policy.setIsEnabled(true);
		policy.setName("HDFS_1-1-20150316062453");
		policy.setUpdatedBy("Admin");
		policy.setUpdateTime(new Date());
		policy.setService("HDFS_1-1-20150316062453");
		policy.setIsAuditEnabled(true);
		policy.setPolicyItems(policyItems);
		policy.setResources(policyResource);
		policy.setService("HDFS_1");

		return policy;
	}
	
	private RangerPolicy rangerPolicy1() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
		List<String> users = new ArrayList<String>();
		List<String> groups = new ArrayList<String>();
		List<RangerPolicyItemCondition> conditions = new ArrayList<RangerPolicyItemCondition>();
		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicyItem>();
		RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
		rangerPolicyItem.setAccesses(accesses);
		rangerPolicyItem.setConditions(conditions);
		rangerPolicyItem.setGroups(groups);
		rangerPolicyItem.setUsers(users);
		rangerPolicyItem.setDelegateAdmin(false);

		policyItems.add(rangerPolicyItem);

		Map<String, RangerPolicyResource> policyResource = new HashMap<String, RangerPolicyResource>();
		RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
		rangerPolicyResource.setIsExcludes(true);
		rangerPolicyResource.setIsRecursive(true);
		rangerPolicyResource.setValue("2");
		rangerPolicyResource.setValues(users);
		policyResource.put("resource", rangerPolicyResource);
		RangerPolicy policy = new RangerPolicy();
		policy.setId(Id2);
		policy.setCreateTime(new Date());
		policy.setDescription("policy");
		policy.setGuid("policyguid");
		policy.setIsEnabled(true);
		policy.setName("HDFS_1-1-20150316062454");
		policy.setUpdatedBy("Admin");
		policy.setUpdateTime(new Date());
		policy.setService("HDFS_1-1-20150316062454");
		policy.setIsAuditEnabled(true);
		policy.setPolicyItems(policyItems);
		policy.setResources(policyResource);
		policy.setService("HDFS_2");

		return policy;
	}

	@Test
	public void test1getServiceDef() throws Exception {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		Mockito.when(serviceREST.getServiceDef(rangerServiceDef.getId())).thenReturn(rangerServiceDef);
		RangerServiceDef dbRangerServiceDef = publicAPIsv2.getServiceDef(Id);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Mockito.verify(serviceREST).getServiceDef(Id);
	}

	@Test
	public void test2getServiceDefByName() throws Exception {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		String name = rangerServiceDef.getName();
		Mockito.when(serviceREST.getServiceDefByName(name)).thenReturn(rangerServiceDef);
		RangerServiceDef dbRangerServiceDef = publicAPIsv2.getServiceDefByName(name);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Mockito.verify(serviceREST).getServiceDefByName(name);
	}
	
	@Test
	public void test3searchServiceDefs() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		List<RangerServiceDef> serviceDefsList = new ArrayList<RangerServiceDef>();
		RangerServiceDef serviceDef = rangerServiceDef();
		serviceDefsList.add(serviceDef);
		RangerServiceDefList serviceDefList = new RangerServiceDefList(serviceDefsList);
		Mockito.when(serviceREST.getServiceDefs(request)).thenReturn(serviceDefList);
		List<RangerServiceDef> dbRangerServiceDefList = publicAPIsv2.searchServiceDefs(request);
		Assert.assertNotNull(dbRangerServiceDefList);
		Assert.assertEquals(dbRangerServiceDefList.size(), serviceDefsList.size());
		Mockito.verify(serviceREST).getServiceDefs(request);
	}
	
	@Test
	public void test4createServiceDef() throws Exception {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		Mockito.when(serviceREST.createServiceDef(rangerServiceDef)).thenReturn(rangerServiceDef);
		RangerServiceDef dbRangerServiceDef = publicAPIsv2.createServiceDef(rangerServiceDef);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Mockito.verify(serviceREST).createServiceDef(rangerServiceDef);
	}
	
	@Test
	public void test5updateServiceDef() throws Exception {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		Mockito.when(serviceREST.updateServiceDef(rangerServiceDef, rangerServiceDef.getId())).thenReturn(rangerServiceDef);
		RangerServiceDef dbRangerServiceDef = publicAPIsv2.updateServiceDef(rangerServiceDef, Id);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Mockito.verify(serviceREST).updateServiceDef(rangerServiceDef, rangerServiceDef.getId());
	}
	
	@Test
	public void test6updateServiceDefByName() throws Exception {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		String name = rangerServiceDef.getName();
		Mockito.when(serviceREST.getServiceDefByName(name)).thenReturn(rangerServiceDef);
		Mockito.when(serviceREST.updateServiceDef(rangerServiceDef, rangerServiceDef.getId())).thenReturn(rangerServiceDef);
		RangerServiceDef dbRangerServiceDef = publicAPIsv2.updateServiceDefByName(rangerServiceDef, name);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Mockito.verify(serviceREST).updateServiceDef(rangerServiceDef, dbRangerServiceDef.getId());
		Mockito.verify(serviceREST).getServiceDefByName(name);
	}
	
	@Test
	public void test7deleteServiceDef() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.doNothing().when(serviceREST).deleteServiceDef(Id, request);
		publicAPIsv2.deleteServiceDef(Id, request);
		Mockito.verify(serviceREST).deleteServiceDef(Id, request);
	}

	@Test
	public void test8deleteServiceDefByName() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		String name = rangerServiceDef.getName();
		Mockito.when(serviceREST.getServiceDefByName(name)).thenReturn(rangerServiceDef);
		Mockito.doNothing().when(serviceREST).deleteServiceDef(rangerServiceDef.getId(), request);
		publicAPIsv2.deleteServiceDefByName(name, request);
		Mockito.verify(serviceREST).deleteServiceDef(rangerServiceDef.getId(), request);
		Mockito.verify(serviceREST).getServiceDefByName(name);
	}
	
	@Test
	public void test9getService() throws Exception {
		RangerService rangerService = rangerService();
		Mockito.when(serviceREST.getService(rangerService.getId())).thenReturn(rangerService);
		RangerService dbRangerService = publicAPIsv2.getService(Id);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getId(),
				rangerService.getId());
		Assert.assertEquals(dbRangerService.getName(),
				rangerService.getName());
		Mockito.verify(serviceREST).getService(Id);
	}

	@Test
	public void test10getServiceByName() throws Exception {
		RangerService rangerService = rangerService();
		String name = rangerService.getName();
		Mockito.when(serviceREST.getServiceByName(name)).thenReturn(rangerService);
		RangerService dbRangerService = publicAPIsv2.getServiceByName(name);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getId(),
				rangerService.getId());
		Assert.assertEquals(dbRangerService.getName(),
				rangerService.getName());
		Mockito.verify(serviceREST).getServiceByName(name);
	}
	
	@Test
	public void test11searchServices() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		List<RangerService> servicesList = new ArrayList<RangerService>();
		RangerService service = rangerService();
		servicesList.add(service);
		RangerServiceList serviceList = new RangerServiceList(servicesList);
		Mockito.when(serviceREST.getServices(request)).thenReturn(serviceList);
		List<RangerService> dbRangerServiceList = publicAPIsv2.searchServices(request);
		Assert.assertNotNull(dbRangerServiceList);
		Assert.assertEquals(dbRangerServiceList.size(), servicesList.size());
		Mockito.verify(serviceREST).getServices(request);
	}
	
	@Test
	public void test12createService() throws Exception {
		RangerService rangerService = rangerService();
		Mockito.when(serviceREST.createService(rangerService)).thenReturn(rangerService);
		RangerService dbRangerService = publicAPIsv2.createService(rangerService);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getId(),
				rangerService.getId());
		Assert.assertEquals(dbRangerService.getName(),
				rangerService.getName());
		Mockito.verify(serviceREST).createService(rangerService);
	}
	
	@Test
	public void test13updateService() throws Exception {
		RangerService rangerService = rangerService();
		HttpServletRequest request = null;
		Mockito.when(serviceREST.updateService(rangerService, request)).thenReturn(rangerService);
		RangerService dbRangerService = publicAPIsv2.updateService(rangerService, Id, request);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getId(),
				rangerService.getId());
		Assert.assertEquals(dbRangerService.getName(),
				rangerService.getName());
		Mockito.verify(serviceREST).updateService(rangerService, request);
	}
	
	@Test
	public void test14updateServiceByName() throws Exception {
		RangerService rangerService = rangerService();
        HttpServletRequest request = null;
		String name = rangerService.getName();
		Mockito.when(serviceREST.getServiceByName(name)).thenReturn(rangerService);
		Mockito.when(serviceREST.updateService(rangerService, request)).thenReturn(rangerService);
		RangerService dbRangerService = publicAPIsv2.updateServiceByName(rangerService, name, request);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getId(),
				rangerService.getId());
		Assert.assertEquals(dbRangerService.getName(),
				rangerService.getName());
		Mockito.verify(serviceREST).updateService(rangerService, request);
		Mockito.verify(serviceREST).getServiceByName(name);
	}
	
	@Test
	public void test15deleteService() throws Exception {
		Mockito.doNothing().when(serviceREST).deleteService(Id);
		publicAPIsv2.deleteService(Id);
		Mockito.verify(serviceREST).deleteService(Id);
	}

	@Test
	public void test16deleteServiceByName() throws Exception {
		RangerService rangerService = rangerService();
		String name = rangerService.getName();
		Mockito.when(serviceREST.getServiceByName(name)).thenReturn(rangerService);
		Mockito.doNothing().when(serviceREST).deleteService(rangerService.getId());
		publicAPIsv2.deleteServiceByName(name);
		Mockito.verify(serviceREST).deleteService(rangerService.getId());
		Mockito.verify(serviceREST).getServiceByName(name);
	}
	
	@Test
	public void test17getPolicy() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.when(serviceREST.getPolicy(rangerPolicy.getId())).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.getPolicy(Id);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(),
				rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(),
				rangerPolicy.getName());
		Mockito.verify(serviceREST).getPolicy(Id);
	}

	@Test
	public void test18getPolicyByName() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerPolicy rangerPolicy = rangerPolicy();
		RangerService rangerService = rangerService();
		String serviceName = rangerService.getName();
		String policyName = rangerPolicy.getName();
		String zoneName = "zone-1";
		Mockito.when(serviceREST.getPolicyByName(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.getPolicyByName(serviceName, policyName, zoneName, request);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(),
				rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(),
				rangerPolicy.getName());
		Mockito.verify(serviceREST).getPolicyByName(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
	}
	
	@Test
	public void test19searchPolicies() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		List<RangerPolicy> policiesList = new ArrayList<RangerPolicy>();
		RangerService service = rangerService();
		String serviceName = service.getName();
		RangerPolicy rangerPolicy = rangerPolicy();
		policiesList.add(rangerPolicy);
		RangerPolicyList policyList = new RangerPolicyList(policiesList);
		Mockito.when(serviceREST.getServicePoliciesByName(serviceName, request)).thenReturn(policyList);
		List<RangerPolicy> dbRangerPolicyList = publicAPIsv2.searchPolicies(serviceName, request);
		Assert.assertNotNull(dbRangerPolicyList);
		Assert.assertEquals(dbRangerPolicyList.size(), policiesList.size());
		Mockito.verify(serviceREST).getServicePoliciesByName(serviceName, request);
	}
	
	@Test
	public void test20createPolicy() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.when(serviceREST.createPolicy(rangerPolicy, request)).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.createPolicy(rangerPolicy, request);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(),
				rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(),
				rangerPolicy.getName());
		Mockito.verify(serviceREST).createPolicy(rangerPolicy, request);
	}
	
	@Test
	public void test21applyPolicy() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.when(serviceREST.applyPolicy(rangerPolicy, request)).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.applyPolicy(rangerPolicy, request);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(),
				rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(),
				rangerPolicy.getName());
		Mockito.verify(serviceREST).applyPolicy(rangerPolicy, request);
	}
	
	@Test
	public void test22updatePolicy() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.when(serviceREST.updatePolicy(rangerPolicy, Id)).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.updatePolicy(rangerPolicy, Id);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(),
				rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(),
				rangerPolicy.getName());
		Mockito.verify(serviceREST).updatePolicy(rangerPolicy, Id);
	}
	
	@Test
	public void test23updatePolicyByName() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerPolicy rangerPolicy = rangerPolicy();
		String policyName = rangerPolicy.getName();
		RangerService rangerService = rangerService();
		String serviceName = rangerService.getName();
		String zoneName = "zone-1";
		Mockito.when(serviceREST.getPolicyByName(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(rangerPolicy);
		Mockito.when(serviceREST.updatePolicy(rangerPolicy, rangerPolicy.getId())).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.updatePolicyByName(rangerPolicy, serviceName, policyName, zoneName, request);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(),
				rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(),
				rangerPolicy.getName());
		Mockito.verify(serviceREST).updatePolicy(rangerPolicy, rangerPolicy.getId());
		Mockito.verify(serviceREST).getPolicyByName(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
	}
	
	@Test
	public void test24deletePolicy() throws Exception {
		Mockito.doNothing().when(serviceREST).deletePolicy(Id);
		publicAPIsv2.deletePolicy(Id);
		Mockito.verify(serviceREST).deletePolicy(Id);
	}

	@Test
	public void test25deletePolicyByName() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerPolicy rangerPolicy = rangerPolicy();
		String policyName = rangerPolicy.getName();
		RangerService rangerService = rangerService();
		String serviceName = rangerService.getName();
		String zoneName = "zone-1";
		Mockito.when(serviceREST.getPolicyByName(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(rangerPolicy);
		Mockito.doNothing().when(serviceREST).deletePolicy(Id);
		publicAPIsv2.deletePolicyByName(serviceName, policyName, zoneName, request);
		Mockito.verify(serviceREST).getPolicyByName(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
		Mockito.verify(serviceREST).deletePolicy(Id);
	}

	@Test
	public void test26getPolicies() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerPolicyList policyList = Mockito.mock(RangerPolicyList.class);
		List<RangerPolicy> rangerPolicies = new ArrayList<RangerPolicy>();
		RangerPolicy rangerpolicy1 = rangerPolicy();
		RangerPolicy rangerpolicy2 = rangerPolicy1();
		rangerPolicies.add(rangerpolicy1);
		rangerPolicies.add(rangerpolicy2);
		Mockito.when(serviceREST.getPolicies(request)).thenReturn(policyList);
		Mockito.when(policyList.getPolicies()).thenReturn(rangerPolicies);
		List<RangerPolicy> dbRangerPolicies = publicAPIsv2.getPolicies(request);
		Assert.assertNotNull(dbRangerPolicies);
		Assert.assertEquals(dbRangerPolicies.size(), rangerPolicies.size());
		Mockito.verify(serviceREST).getPolicies(request);
	}

    @Test
    public void testGetAllZoneNames() throws Exception {
        List<RangerSecurityZoneHeaderInfo> zoneHeaderInfoList = new ArrayList<>();
        zoneHeaderInfoList.add(new RangerSecurityZoneHeaderInfo(2L, "zone-1"));
        zoneHeaderInfoList.add(new RangerSecurityZoneHeaderInfo(3L, "zone-2"));

        Mockito.when(securityZoneStore.getSecurityZoneHeaderInfoList()).thenReturn(zoneHeaderInfoList);

        List<RangerSecurityZoneHeaderInfo> returnedZoneHeaderInfoList = publicAPIsv2.getSecurityZoneHeaderInfoList();
        Assert.assertEquals(returnedZoneHeaderInfoList.size(), zoneHeaderInfoList.size());
        Mockito.verify(securityZoneStore, Mockito.times(1)).getSecurityZoneHeaderInfoList();
    }

    @Test
    public void testGetServiceNamesForZone() throws Exception {
        Long zoneId1           = 2L;
        Long zoneId2           = 3L;
        Long nonExistingZondId = 101L;

        List<RangerServiceHeaderInfo> rangerServiceList1     = new ArrayList<RangerServiceHeaderInfo>();
        List<RangerServiceHeaderInfo> rangerServiceList2     = new ArrayList<RangerServiceHeaderInfo>();

        rangerServiceList1.add(new RangerServiceHeaderInfo(1L, "hdfs_1", false));
        rangerServiceList1.add(new RangerServiceHeaderInfo(2L, "hive_1", false));
        rangerServiceList1.add(new RangerServiceHeaderInfo(3L, "hbase_1", false));
        rangerServiceList1.add(new RangerServiceHeaderInfo(4L, "tag_1", true));

        rangerServiceList2.add(new RangerServiceHeaderInfo(5L, "yarn_1", false));

        Mockito.when(securityZoneStore.getServiceHeaderInfoListByZoneId(null)).thenReturn(Collections.emptyList());
        Mockito.when(securityZoneStore.getServiceHeaderInfoListByZoneId(zoneId1)).thenReturn(rangerServiceList1);
        Mockito.when(securityZoneStore.getServiceHeaderInfoListByZoneId(zoneId2)).thenReturn(rangerServiceList2);
        Mockito.when(securityZoneStore.getServiceHeaderInfoListByZoneId(nonExistingZondId)).thenReturn(Collections.emptyList());

        // Null
        List<RangerServiceHeaderInfo> returnedServicesNull = publicAPIsv2.getServiceHeaderInfoListByZoneId(null);

        Mockito.verify(securityZoneStore, Mockito.times(1)).getServiceHeaderInfoListByZoneId(null);
        Assert.assertEquals(returnedServicesNull.size(), 0);

        // Non existing zoneId
        List<RangerServiceHeaderInfo> returnedServicesNonExisting = publicAPIsv2.getServiceHeaderInfoListByZoneId(nonExistingZondId);

        Mockito.verify(securityZoneStore, Mockito.times(1)).getServiceHeaderInfoListByZoneId(null);
        Assert.assertEquals(returnedServicesNonExisting.size(), 0);

        // zoneId1
        List<RangerServiceHeaderInfo> returnedServicesZone1 = publicAPIsv2.getServiceHeaderInfoListByZoneId(zoneId1);

        Mockito.verify(securityZoneStore, Mockito.times(1)).getServiceHeaderInfoListByZoneId(zoneId1);
        Assert.assertEquals(returnedServicesZone1.size(), rangerServiceList1.size());

        // zoneId2
        List<RangerServiceHeaderInfo> returnedServicesZone2 = publicAPIsv2.getServiceHeaderInfoListByZoneId(zoneId2);

        Mockito.verify(securityZoneStore, Mockito.times(1)).getServiceHeaderInfoListByZoneId(zoneId2);
        Assert.assertEquals(returnedServicesZone2.size(), rangerServiceList2.size());
    }

	@Test
	public void testGetPolicyByGUIDAndServiceNameAndZoneName() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		RangerService rangerService = rangerService();
		String serviceName = rangerService.getName();
		Mockito.when(serviceREST.getPolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), serviceName, "zone-1")).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.getPolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), serviceName, "zone-1");
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(), rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(), rangerPolicy.getName());
		Mockito.verify(serviceREST).getPolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), serviceName, "zone-1");
	}

	@Test
	public void testGetPolicyByGUID() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.when(serviceREST.getPolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), null, null)).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = publicAPIsv2.getPolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), null, null);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(), rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(), rangerPolicy.getName());
		Mockito.verify(serviceREST).getPolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), null, null);
	}

	@Test
	public void testDeletePolicyByGUIDAndServiceNameAndZoneName() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		RangerService rangerService = rangerService();
		String serviceName = rangerService.getName();
		String zoneName = "zone-1";
		Mockito.doNothing().when(serviceREST).deletePolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), serviceName, zoneName);
		publicAPIsv2.deletePolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), serviceName, zoneName);
		Mockito.verify(serviceREST).deletePolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), serviceName, zoneName);
	}

	@Test
	public void testDeletePolicyByGUID() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.doNothing().when(serviceREST).deletePolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), null, null);
		publicAPIsv2.deletePolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), null, null);
		Mockito.verify(serviceREST).deletePolicyByGUIDAndServiceNameAndZoneName(rangerPolicy.getGuid(), null, null);
	}
}
