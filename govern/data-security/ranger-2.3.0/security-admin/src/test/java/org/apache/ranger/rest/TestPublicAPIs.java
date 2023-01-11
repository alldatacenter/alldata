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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.ServiceUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXPolicyDao;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XAssetService;
import org.apache.ranger.view.VXAsset;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXPolicy;
import org.apache.ranger.view.VXPolicyList;
import org.apache.ranger.view.VXRepository;
import org.apache.ranger.view.VXRepositoryList;
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
public class TestPublicAPIs {

	private static Long Id = 8L;
	
	@InjectMocks
	PublicAPIs publicAPIs = new PublicAPIs();
	
	@Mock
	ServiceREST serviceREST;
	
	@Mock
	ServiceUtil serviceUtil;

	@Mock
	RESTErrorUtil restErrorUtil;
	
	@Mock
	JSONUtil jsonUtil;
	
	@Mock
	RangerDaoManager daoMgr;
	
	@Mock
	RangerSearchUtil searchUtil;
	
	@Mock
	XAssetService xAssetService;
	
	@Mock
	RangerPolicyService policyService;
	
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
	
	private XXService xService() {
		XXService xService = new XXService();
		xService.setAddedByUserId(Id);
		xService.setCreateTime(new Date());
		xService.setDescription("Hdfs service");
		xService.setGuid("serviceguid");
		xService.setId(Id);
		xService.setIsEnabled(true);
		xService.setName("Hdfs");
		xService.setPolicyUpdateTime(new Date());
		xService.setPolicyVersion(1L);
		xService.setType(1L);
		xService.setUpdatedByUserId(Id);
		xService.setUpdateTime(new Date());

		return xService;
	}
	
	private VXRepository vXRepository(RangerService service) {
		VXRepository ret = new VXRepository();
		ret.setRepositoryType(service.getType());
		ret.setName(service.getName());
		ret.setDescription(service.getDescription());
		ret.setIsActive(service.getIsEnabled());
		ret.setConfig(jsonUtil.readMapToString(service.getConfigs()));
		//ret.setVersion(Long.toString(service.getVersion()));
		ret.setId(service.getId());
		ret.setCreateDate(service.getCreateTime());
		ret.setUpdateDate(service.getUpdateTime());
		ret.setOwner(service.getCreatedBy());
		ret.setUpdatedBy(service.getUpdatedBy());
		
		return ret;
	}
	
	private VXPolicy vXPolicy(RangerPolicy policy, RangerService service) {
		VXPolicy ret = new VXPolicy();
		ret.setPolicyName(StringUtils.trim(policy.getName()));
		ret.setDescription(policy.getDescription());
		ret.setRepositoryName(policy.getService());
		ret.setIsEnabled(policy.getIsEnabled() ? true : false);
		ret.setRepositoryType(service.getType());
		ret.setIsAuditEnabled(policy.getIsAuditEnabled());
		return ret;
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
	
	private XXPolicy policy() {
		XXPolicy xxPolicy = new XXPolicy();
		xxPolicy.setId(Id);
		xxPolicy.setName("HDFS_1-1-20150316062453");
		xxPolicy.setAddedByUserId(Id);
		xxPolicy.setCreateTime(new Date());
		xxPolicy.setDescription("test");
		xxPolicy.setIsAuditEnabled(false);
		xxPolicy.setIsEnabled(false);
		xxPolicy.setService(1L);
		xxPolicy.setUpdatedByUserId(Id);
		xxPolicy.setUpdateTime(new Date());
		xxPolicy.setVersion(Id);
		return xxPolicy;
	}

	@Test
	public void test1getRepository() throws Exception {
		RangerService rangerService = rangerService();
		VXRepository vXRepository = vXRepository(rangerService);
		Mockito.when(serviceREST.getService(rangerService.getId())).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXRepository(rangerService)).thenReturn(vXRepository);
		VXRepository dbVXRepository = publicAPIs.getRepository(Id);
		
		Assert.assertNotNull(dbVXRepository);
		Assert.assertEquals(dbVXRepository, vXRepository);
		Assert.assertEquals(dbVXRepository.getId(),
				vXRepository.getId());
		Assert.assertEquals(dbVXRepository.getName(),
				vXRepository.getName());
		Mockito.verify(serviceREST).getService(Id);
		Mockito.verify(serviceUtil).toVXRepository(rangerService);
		
	}
	
	@Test
	public void test2createRepository() throws Exception {
		VXAsset vXAsset = new VXAsset();
		RangerService rangerService = rangerService();
		VXRepository vXRepository = vXRepository(rangerService);
		Mockito.when(serviceUtil.publicObjecttoVXAsset(vXRepository)).thenReturn(vXAsset);
		Mockito.when(serviceUtil.toRangerService(vXAsset)).thenReturn(rangerService);
		Mockito.when(serviceREST.createService(rangerService)).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXAsset(rangerService)).thenReturn(vXAsset);
		Mockito.when(serviceUtil.vXAssetToPublicObject(vXAsset)).thenReturn(vXRepository);
		VXRepository dbVXRepository = publicAPIs.createRepository(vXRepository);
		
		Assert.assertNotNull(dbVXRepository);
		Assert.assertEquals(dbVXRepository, vXRepository);
		Assert.assertEquals(dbVXRepository.getId(),
				vXRepository.getId());
		Assert.assertEquals(dbVXRepository.getName(),
				vXRepository.getName());
		Mockito.verify(serviceREST).createService(rangerService);
		Mockito.verify(serviceUtil).publicObjecttoVXAsset(vXRepository);
		Mockito.verify(serviceUtil).toRangerService(vXAsset);
		Mockito.verify(serviceUtil).toVXAsset(rangerService);
		Mockito.verify(serviceUtil).vXAssetToPublicObject(vXAsset);
	}
	
	@Test
	public void test3updateRepository() throws Exception {
		VXAsset vXAsset = new VXAsset();
		RangerService rangerService = rangerService();
        HttpServletRequest request = null;
		VXRepository vXRepository = vXRepository(rangerService);
		XXService xService = xService();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		Mockito.when(daoMgr.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);
		Mockito.when(serviceUtil.publicObjecttoVXAsset(vXRepository)).thenReturn(vXAsset);
		Mockito.when(serviceUtil.toRangerService(vXAsset)).thenReturn(rangerService);
		Mockito.when(serviceREST.updateService(rangerService, request)).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXAsset(rangerService)).thenReturn(vXAsset);
		Mockito.when(serviceUtil.vXAssetToPublicObject(vXAsset)).thenReturn(vXRepository);
		VXRepository dbVXRepository = publicAPIs.updateRepository(vXRepository, Id);
		
		Assert.assertNotNull(dbVXRepository);
		Assert.assertEquals(dbVXRepository, vXRepository);
		Assert.assertEquals(dbVXRepository.getId(),
				vXRepository.getId());
		Assert.assertEquals(dbVXRepository.getName(),
				vXRepository.getName());
		Mockito.verify(serviceREST).updateService(rangerService, request);
		Mockito.verify(serviceUtil).publicObjecttoVXAsset(vXRepository);
		Mockito.verify(serviceUtil).toRangerService(vXAsset);
		Mockito.verify(serviceUtil).toVXAsset(rangerService);
		Mockito.verify(serviceUtil).vXAssetToPublicObject(vXAsset);
		Mockito.verify(daoMgr).getXXService();
	}
	
	@Test
	public void test4deleteRepository() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.doNothing().when(serviceREST).deleteService(Id);
		publicAPIs.deleteRepository(Id, request);
		Mockito.verify(serviceREST).deleteService(Id);
	}

	@Test
	public void test5searchRepositories() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		List<RangerService> ret = new ArrayList<RangerService>();
		RangerService rangerService = rangerService();
		VXRepository vXRepository = vXRepository(rangerService);
		List<VXRepository> repoList = new ArrayList<VXRepository>();
		repoList.add(vXRepository);
		VXRepositoryList vXRepositoryList = new VXRepositoryList(repoList);
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequestForRepositorySearch(request, xAssetService.sortFields)).thenReturn(filter);
		Mockito.when(serviceREST.getServices(filter)).thenReturn(ret);
		Mockito.when(serviceUtil.rangerServiceListToPublicObjectList(ret)).thenReturn(vXRepositoryList);
		VXRepositoryList dbVXRepositoryList = publicAPIs.searchRepositories(request);
		Assert.assertNotNull(dbVXRepositoryList);
		Assert.assertEquals(dbVXRepositoryList.getResultSize(), vXRepositoryList.getResultSize());
	}
	
	@Test
	public void test6countRepositories() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		VXLong vXLong = new VXLong();
		List<RangerService> ret = new ArrayList<RangerService>();
		RangerService rangerService = rangerService();
		VXRepository vXRepository = vXRepository(rangerService);
		List<VXRepository> repoList = new ArrayList<VXRepository>();
		repoList.add(vXRepository);
		VXRepositoryList vXRepositoryList = new VXRepositoryList(repoList);
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequestForRepositorySearch(request, xAssetService.sortFields)).thenReturn(filter);
		Mockito.when(serviceREST.getServices(filter)).thenReturn(ret);
		Mockito.when(serviceUtil.rangerServiceListToPublicObjectList(ret)).thenReturn(vXRepositoryList);
		VXRepositoryList dbVXRepositoryList = publicAPIs.searchRepositories(request);
		vXLong.setValue(dbVXRepositoryList.getResultSize());
		Assert.assertNotNull(vXLong);
		Assert.assertEquals(vXLong.getValue(), 1);		
		Mockito.verify(searchUtil).getSearchFilterFromLegacyRequestForRepositorySearch(request, xAssetService.sortFields);
		Mockito.verify(serviceREST).getServices(filter);
		Mockito.verify(serviceUtil).rangerServiceListToPublicObjectList(ret);
	}
	
	@Test
	public void test7getPolicy() throws Exception {
		RangerPolicy policy = rangerPolicy();
		RangerService service = rangerService();
		VXPolicy vXPolicy = vXPolicy(policy, service);
		Mockito.when(serviceREST.getPolicy(policy.getId())).thenReturn(policy);
		Mockito.when(serviceREST.getServiceByName(policy.getService())).thenReturn(service);
		Mockito.when(serviceUtil.toVXPolicy(policy, service)).thenReturn(vXPolicy);
		VXPolicy dbVXPolicy = publicAPIs.getPolicy(Id);
		Assert.assertNotNull(dbVXPolicy);
		Assert.assertEquals(dbVXPolicy, vXPolicy);
		Assert.assertEquals(dbVXPolicy.getPolicyName(), vXPolicy.getPolicyName());
		Assert.assertEquals(dbVXPolicy.getRepositoryType(), vXPolicy.getRepositoryType());
		Mockito.verify(serviceREST).getPolicy(Id);
		Mockito.verify(serviceREST).getServiceByName(policy.getService());
		Mockito.verify(serviceUtil).toVXPolicy(policy, service);
	}
	
	@Test
	public void test8createPolicy() throws Exception {
		RangerPolicy policy = rangerPolicy();
		RangerService service = rangerService();
		VXPolicy vXPolicy = vXPolicy(policy, service);
		Mockito.when(serviceREST.getServiceByName(vXPolicy.getRepositoryName())).thenReturn(service);
		Mockito.when(serviceUtil.toRangerPolicy(vXPolicy,service)).thenReturn(policy);
		Mockito.when(serviceREST.createPolicy(policy, null)).thenReturn(policy);
		Mockito.when(serviceUtil.toVXPolicy(policy, service)).thenReturn(vXPolicy);
		VXPolicy dbVXPolicy = publicAPIs.createPolicy(vXPolicy);
		Assert.assertNotNull(dbVXPolicy);
		Assert.assertEquals(dbVXPolicy, vXPolicy);
		Assert.assertEquals(dbVXPolicy.getId(),
				vXPolicy.getId());
		Assert.assertEquals(dbVXPolicy.getRepositoryName(),
				vXPolicy.getRepositoryName());
		Mockito.verify(serviceREST).createPolicy(policy, null);
		Mockito.verify(serviceREST).getServiceByName(vXPolicy.getRepositoryName());
		Mockito.verify(serviceUtil).toVXPolicy(policy, service);
		Mockito.verify(serviceUtil).toRangerPolicy(vXPolicy,service);
		
	}
	
	@Test
	public void test9updatePolicy() throws Exception {
		RangerPolicy policy = rangerPolicy();
		RangerService service = rangerService();
		VXPolicy vXPolicy = vXPolicy(policy, service);
		XXPolicyDao xXPolicyDao = Mockito.mock(XXPolicyDao.class);
		XXPolicy xXPolicy = policy();
		Mockito.when(daoMgr.getXXPolicy()).thenReturn(xXPolicyDao);
		Mockito.when(xXPolicyDao.getById(Id)).thenReturn(xXPolicy);
		Mockito.when(serviceREST.getServiceByName(vXPolicy.getRepositoryName())).thenReturn(service);
		Mockito.when(serviceUtil.toRangerPolicy(vXPolicy,service)).thenReturn(policy);
		Mockito.when(serviceREST.updatePolicy(policy)).thenReturn(policy);
		Mockito.when(serviceUtil.toVXPolicy(policy, service)).thenReturn(vXPolicy);
		VXPolicy dbVXPolicy = publicAPIs.updatePolicy(vXPolicy, Id);
		
		Assert.assertNotNull(dbVXPolicy);
		Assert.assertEquals(dbVXPolicy, vXPolicy);
		Assert.assertEquals(dbVXPolicy.getId(),
				vXPolicy.getId());
		Assert.assertEquals(dbVXPolicy.getRepositoryName(),
				vXPolicy.getRepositoryName());
		Mockito.verify(serviceREST).updatePolicy(policy);
		Mockito.verify(serviceREST).getServiceByName(vXPolicy.getRepositoryName());
		Mockito.verify(serviceUtil).toVXPolicy(policy, service);
		Mockito.verify(serviceUtil).toRangerPolicy(vXPolicy,service);
		Mockito.verify(daoMgr).getXXPolicy();
		Mockito.verify(xXPolicyDao).getById(Id);
	}
	
	@Test
	public void test10deletePolicy() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.doNothing().when(serviceREST).deletePolicy(Id);
		publicAPIs.deletePolicy(Id, request);
		Mockito.verify(serviceREST).deletePolicy(Id);
	}
	
	@Test
	public void test11searchPolicies() throws Exception {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerService service = rangerService();
		RangerPolicy policy = rangerPolicy();
		List<RangerPolicy> policyList = new ArrayList<RangerPolicy>();
		policyList.add(policy);
		VXPolicy vXPolicy = vXPolicy(policy, service);
		List<VXPolicy> vXPolicies = new ArrayList<VXPolicy>();
		vXPolicies.add(vXPolicy);
		VXPolicyList vXPolicyList = new VXPolicyList(vXPolicies);
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");
		filter.setStartIndex(0);
		filter.setMaxRows(10);
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequest(request, policyService.sortFields)).thenReturn(filter);
		Mockito.when(serviceREST.getPolicies(filter)).thenReturn(policyList);
		Mockito.when(serviceUtil.rangerPolicyListToPublic(policyList,filter)).thenReturn(vXPolicyList);
		VXPolicyList dbVXPolicyList = publicAPIs.searchPolicies(request);
		Assert.assertNotNull(dbVXPolicyList);
		Assert.assertEquals(dbVXPolicyList.getResultSize(), vXPolicyList.getResultSize());
		Mockito.verify(searchUtil).getSearchFilterFromLegacyRequest(request, policyService.sortFields);
		Mockito.verify(serviceREST).getPolicies(filter);
		Mockito.verify(serviceUtil).rangerPolicyListToPublic(policyList,filter);
		
	}
	
	@Test
	public void test12countPolicies() throws Exception {
		VXLong vXLong = new VXLong();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		RangerService service = rangerService();
		RangerPolicy policy = rangerPolicy();
		List<RangerPolicy> policyList = new ArrayList<RangerPolicy>();
		policyList.add(policy);
		VXPolicy vXPolicy = vXPolicy(policy, service);
		List<VXPolicy> vXPolicies = new ArrayList<VXPolicy>();
		vXPolicies.add(vXPolicy);
		VXPolicyList vXPolicyList = new VXPolicyList(vXPolicies);
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");
		filter.setStartIndex(0);
		filter.setMaxRows(10);
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequest(request, policyService.sortFields)).thenReturn(filter);
		Mockito.when(serviceREST.getPolicies(filter)).thenReturn(policyList);
		Mockito.when(serviceUtil.rangerPolicyListToPublic(policyList,filter)).thenReturn(vXPolicyList);
		VXPolicyList dbVXPolicyList = publicAPIs.searchPolicies(request);
		vXLong.setValue(dbVXPolicyList.getResultSize());
		Assert.assertNotNull(vXLong);
		Assert.assertEquals(vXLong.getValue(), 1);
		Mockito.verify(searchUtil).getSearchFilterFromLegacyRequest(request, policyService.sortFields);
		Mockito.verify(serviceREST).getPolicies(filter);
		Mockito.verify(serviceUtil).rangerPolicyListToPublic(policyList,filter);
		
	}
	
	
}
