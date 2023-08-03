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

import static org.junit.Assert.fail;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.admin.client.datatype.RESTResponse;
import org.apache.ranger.biz.AssetMgr;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.ServiceUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceDefDao;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.service.XAccessAuditService;
import org.apache.ranger.service.XAssetService;
import org.apache.ranger.service.XCredentialStoreService;
import org.apache.ranger.service.XPolicyExportAuditService;
import org.apache.ranger.service.XResourceService;
import org.apache.ranger.service.XTrxLogService;
import org.apache.ranger.view.VXAccessAudit;
import org.apache.ranger.view.VXAccessAuditList;
import org.apache.ranger.view.VXAsset;
import org.apache.ranger.view.VXAssetList;
import org.apache.ranger.view.VXCredentialStore;
import org.apache.ranger.view.VXCredentialStoreList;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXPolicy;
import org.apache.ranger.view.VXPolicyExportAudit;
import org.apache.ranger.view.VXPolicyExportAuditList;
import org.apache.ranger.view.VXResource;
import org.apache.ranger.view.VXResourceList;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXTrxLog;
import org.apache.ranger.view.VXTrxLogList;
import org.junit.Assert;
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
public class TestAssetREST {
	private static Long Id = 8L;
	@Mock
	ServiceREST serviceREST;

	@Mock
	ServiceUtil serviceUtil;

	@Mock
	RangerSearchUtil searchUtil;

	@Mock
	RangerBizUtil xaBizUtil;

	@Mock
	XAssetService xAssetService;

	@Mock
	XResourceService xResourceService;

	@Mock
	XCredentialStoreService xCredentialStoreService;

	@Mock
	AssetMgr assetMgr;

	@Mock
	HttpServletRequest request;

	@Mock
	XTrxLogService xTrxLogService;
	@Mock
	XPolicyExportAuditService xPolicyExportAudits;

	@Mock
	XAccessAuditService xAccessAuditService;

	@Mock
	XXServiceDefDao xxServiceDefDao;

	@Mock
	RangerBizUtil msBizUtil;

	@Mock
	RangerDaoManager daoManager;

	@InjectMocks
	AssetREST assetREST = new AssetREST();

	@Rule public ExpectedException thrown = ExpectedException.none();
	@Mock RESTErrorUtil restErrorUtil;
	@Mock WebApplicationException webApplicationException;


	public void TestAssetRest() {

	}

	private VXCredentialStore vXCredentialStore() {
		VXCredentialStore vXCredentialStore = new VXCredentialStore();
		vXCredentialStore.setId(Id);
		vXCredentialStore.setName("TestAssetRest");
		vXCredentialStore.setDescription("TestAssetRest");
		vXCredentialStore.setOwner("owner");
		return vXCredentialStore;
	}

	private RangerService rangerService(Long id) {
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setConfigs(getSampleConfig());
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

	private VXAsset vXAsset(Long id) {
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

		VXAsset vXAsset = new VXAsset();
		vXAsset.setId(id);
		vXAsset.setActiveStatus(1);
		vXAsset.setAssetType(1);
		vXAsset.setDescription("service policy");
		vXAsset.setSupportNative(false);
		vXAsset.setName("HDFS_1");
		vXAsset.setUpdatedBy("Admin");
		vXAsset.setConfig(getSampleConfig().toString());
		return vXAsset;
	}

	private VXResource vxResource(Long id) {
		VXResource vXResource = new VXResource();
		vXResource.setName("HDFS_1-1-20150316062453");
		vXResource.setId(id);
		vXResource.setAssetId(id);
		return vXResource;
	}

	private RangerPolicy rangerPolicy(Long id) {
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
		policy.setId(id);
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

	private RangerServiceDef rangerServiceDef() {
		List<RangerServiceConfigDef> configs = new ArrayList<RangerServiceConfigDef>();
		List<RangerResourceDef> resources = new ArrayList<RangerResourceDef>();
		List<RangerAccessTypeDef> accessTypes = new ArrayList<RangerAccessTypeDef>();
		List<RangerPolicyConditionDef> policyConditions = new ArrayList<RangerPolicyConditionDef>();
		List<RangerContextEnricherDef> contextEnrichers = new ArrayList<RangerContextEnricherDef>();
		List<RangerEnumDef> enums = new ArrayList<RangerEnumDef>();

		RangerServiceDef rangerServiceDef = new RangerServiceDef();
		rangerServiceDef.setId(Id);
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

	private ServicePolicies servicePolicies() {
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		ServicePolicies servicePolicies = new ServicePolicies();
		List<RangerPolicy> policies = new ArrayList<RangerPolicy>();
		policies.add(rangerPolicy);
		servicePolicies.setServiceId(Id);
		servicePolicies.setServiceName("Hdfs_1");
		servicePolicies.setPolicyVersion(1L);
		servicePolicies.setPolicyUpdateTime(new Date());
		servicePolicies.setServiceDef(rangerServiceDef);
		servicePolicies.setPolicies(policies);
		return servicePolicies;
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

	@Test
	public void testGetXAsset() {
		RangerService rangerService = rangerService(Id);
		VXAsset vXAsset = vXAsset(Id);
		Mockito.when(serviceREST.getService(rangerService.getId())).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXAsset(rangerService)).thenReturn(vXAsset);
		VXAsset asset = assetREST.getXAsset(Id);
		Assert.assertNotNull(asset);
		Assert.assertEquals(vXAsset, asset);
		Mockito.verify(serviceREST).getService(rangerService.getId());
		Mockito.verify(serviceUtil).toVXAsset(rangerService);
	}

	@Test
	public void testCreateXAsset() {
		RangerService rangerService = rangerService(Id);
		VXAsset vXAsset = vXAsset(Id);
		Mockito.when(serviceREST.createService(rangerService)).thenReturn(rangerService);
		Mockito.when(serviceUtil.toRangerService(vXAsset)).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXAsset(rangerService)).thenReturn(vXAsset);
		VXAsset asset = assetREST.createXAsset(vXAsset);
		Assert.assertNotNull(asset);
		Assert.assertEquals(vXAsset, asset);
		Mockito.verify(serviceREST).createService(rangerService);
		Mockito.verify(serviceUtil).toRangerService(vXAsset);
		Mockito.verify(serviceUtil).toVXAsset(rangerService);
	}

	@Test
	public void testUpdateXAsset() {
		RangerService rangerService = rangerService(Id);
		HttpServletRequest request = null;
		VXAsset vXAsset = vXAsset(Id);
		Mockito.when(serviceUtil.toRangerService(vXAsset)).thenReturn(rangerService);
		Mockito.when(serviceREST.updateService(rangerService, request)).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXAsset(rangerService)).thenReturn(vXAsset);
		VXAsset asset = assetREST.updateXAsset(vXAsset);
		Assert.assertNotNull(asset);
		Assert.assertEquals(vXAsset, asset);
		Mockito.verify(serviceREST).updateService(rangerService, request);
		Mockito.verify(serviceUtil).toRangerService(vXAsset);
		Mockito.verify(serviceUtil).toVXAsset(rangerService);
	}

	@Test
	public void testDeleteXAsset() {
		RangerService rangerService = rangerService(Id);

		Mockito.doNothing().when(serviceREST).deleteService(Id);
		assetREST.deleteXAsset(rangerService.getId(), request);
		Mockito.verify(serviceREST).deleteService(rangerService.getId());
	}

	@Test
	public void testConfigTest() {
		RangerService rangerService = rangerService(Id);
		VXResponse expectedVxResponse = new VXResponse();
		expectedVxResponse.setStatusCode(VXResponse.STATUS_SUCCESS);
		expectedVxResponse.setMsgDesc("test connection successful");
		VXAsset vXAsset = vXAsset(Id);
		Mockito.when(serviceUtil.toRangerService(vXAsset)).thenReturn(rangerService);
		Mockito.when(serviceREST.validateConfig(rangerService)).thenReturn(expectedVxResponse);
		VXResponse actualVxResponse = assetREST.configTest(vXAsset);
		Assert.assertNotNull(actualVxResponse);
		Assert.assertEquals(expectedVxResponse, actualVxResponse);
		Mockito.verify(serviceUtil).toRangerService(vXAsset);
		Mockito.verify(serviceREST).validateConfig(rangerService);
	}

	@Test
	public void testSearchXAssets() {
		RangerService rangerService1 = rangerService(Id);
		RangerService rangerService2 = rangerService(9L);
		VXAsset vXAsset1 = vXAsset(Id);
		VXAsset vXAsset2 = vXAsset(9L);
		VXAssetList expectedVXAsset = new VXAssetList();
		List<VXAsset> vXAsset = Arrays.asList(vXAsset1, vXAsset2);
		expectedVXAsset.setVXAssets(vXAsset);
		List<RangerService> services = Arrays.asList(rangerService1, rangerService2);

		SearchFilter searchFilter = new SearchFilter();
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequestForRepositorySearch(request, null))
				.thenReturn(searchFilter);
		Mockito.when(serviceREST.getServices(searchFilter)).thenReturn(services);
		Mockito.when(serviceUtil.toVXAsset(rangerService1)).thenReturn(vXAsset1);
		Mockito.when(serviceUtil.toVXAsset(rangerService2)).thenReturn(vXAsset2);
		VXAssetList vXAssetList = assetREST.searchXAssets(request);
		Assert.assertNotNull(vXAssetList);
		Assert.assertEquals(expectedVXAsset.getVXAssets(), vXAssetList.getVXAssets());
		Mockito.verify(searchUtil).getSearchFilterFromLegacyRequestForRepositorySearch(request, null);
		Mockito.verify(serviceREST).getServices(searchFilter);
		Mockito.verify(serviceUtil, Mockito.times(1)).toVXAsset(rangerService1);
		Mockito.verify(serviceUtil, Mockito.times(1)).toVXAsset(rangerService2);
	}

	@Test
	public void testCountXAssets() {
		RangerService rangerService1 = rangerService(Id);
		RangerService rangerService2 = rangerService(9L);
		VXAsset vXAsset1 = vXAsset(Id);
		VXAsset vXAsset2 = vXAsset(9L);
		VXAssetList expectedVXAsset = new VXAssetList();
		List<VXAsset> vXAsset = Arrays.asList(vXAsset1, vXAsset2);
		expectedVXAsset.setVXAssets(vXAsset);
		VXLong expectedAsset = new VXLong();
		expectedAsset.setValue(2l);

		List<RangerService> services = Arrays.asList(rangerService1, rangerService2);
		SearchFilter searchFilter = new SearchFilter();
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequestForRepositorySearch(request, null))
				.thenReturn(searchFilter);
		Mockito.when(serviceREST.getServices(searchFilter)).thenReturn(services);
		Mockito.when(serviceUtil.toVXAsset(rangerService1)).thenReturn(vXAsset1);
		Mockito.when(serviceUtil.toVXAsset(rangerService2)).thenReturn(vXAsset2);
		VXLong actualAsset = assetREST.countXAssets(request);
		Assert.assertEquals(expectedAsset.getValue(), actualAsset.getValue());
	}

	@Test
	public void testGetXResource() {
		VXResource expectedvxResource = vxResource(Id);
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		RangerService rangerService = rangerService(Id);
		Mockito.when(serviceREST.getPolicy(Id)).thenReturn(rangerPolicy);
		Mockito.when(serviceREST.getServiceByName(rangerPolicy.getService())).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXResource(rangerPolicy, rangerService)).thenReturn(expectedvxResource);
		VXResource actualvxResource = assetREST.getXResource(Id);
		Assert.assertNotNull(actualvxResource);
		Assert.assertEquals(expectedvxResource, actualvxResource);
	}

	@Test
	public void testCreateXResource() {
		VXResource vxResource = vxResource(Id);
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		RangerService rangerService = rangerService(Id);
		Mockito.when(serviceREST.getService(vxResource.getAssetId())).thenReturn(rangerService);
		Mockito.when(serviceREST.createPolicy(rangerPolicy, null)).thenReturn(rangerPolicy);
		Mockito.when(serviceUtil.toRangerPolicy(vxResource, rangerService)).thenReturn(rangerPolicy);
		Mockito.when(serviceUtil.toVXResource(rangerPolicy, rangerService)).thenReturn(vxResource);
		VXResource actualvxResource = assetREST.createXResource(vxResource);
		Assert.assertNotNull(actualvxResource);
		Assert.assertEquals(vxResource, actualvxResource);
		Mockito.verify(serviceREST).getService(vxResource.getAssetId());
		Mockito.verify(serviceREST).createPolicy(rangerPolicy, null);
		Mockito.verify(serviceUtil).toRangerPolicy(vxResource, rangerService);
		Mockito.verify(serviceUtil).toVXResource(rangerPolicy, rangerService);
	}

	@Test
	public void testUpdateXResource() {
		VXResource vxResource = vxResource(Id);
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		RangerService rangerService = rangerService(Id);
		Mockito.when(serviceREST.getService(vxResource.getAssetId())).thenReturn(rangerService);
		Mockito.when(serviceREST.updatePolicy(rangerPolicy, Id)).thenReturn(rangerPolicy);
		Mockito.when(serviceUtil.toRangerPolicy(vxResource, rangerService)).thenReturn(rangerPolicy);
		Mockito.when(serviceUtil.toVXResource(rangerPolicy, rangerService)).thenReturn(vxResource);
		VXResource actualvxResource = assetREST.updateXResource(vxResource, Id);
		Assert.assertNotNull(actualvxResource);
		Assert.assertEquals(vxResource, actualvxResource);
		Mockito.verify(serviceREST).getService(vxResource.getAssetId());
		Mockito.verify(serviceREST).updatePolicy(rangerPolicy, Id);
		Mockito.verify(serviceUtil).toRangerPolicy(vxResource, rangerService);
		Mockito.verify(serviceUtil).toVXResource(rangerPolicy, rangerService);
	}

	@Test
	public void testUpdateXResourceForInvalidResourceId() {
		VXResource vxResource = vxResource(Id);
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		RangerService rangerService = rangerService(Id);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		VXResource actualvxResource = assetREST.updateXResource(vxResource, -11L);
		Assert.assertNotNull(actualvxResource);
		Assert.assertEquals(vxResource, actualvxResource);
		Mockito.verify(serviceREST).getService(vxResource.getAssetId());
		Mockito.verify(serviceREST).updatePolicy(rangerPolicy, Id);
		Mockito.verify(serviceUtil).toRangerPolicy(vxResource, rangerService);
		Mockito.verify(serviceUtil).toVXResource(rangerPolicy, rangerService);
	}

	@Test
	public void testUpdateXResourceWhenResourceIdIsNull() {
		VXResource vxResource = vxResource(Id);
		vxResource.setId(null);
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		RangerService rangerService = rangerService(Id);
		Mockito.when(serviceREST.getService(vxResource.getAssetId())).thenReturn(rangerService);
		Mockito.when(serviceREST.updatePolicy(rangerPolicy, Id)).thenReturn(rangerPolicy);
		Mockito.when(serviceUtil.toRangerPolicy(vxResource, rangerService)).thenReturn(rangerPolicy);
		Mockito.when(serviceUtil.toVXResource(rangerPolicy, rangerService)).thenReturn(vxResource);
		VXResource actualvxResource = assetREST.updateXResource(vxResource, Id);
		Assert.assertNotNull(actualvxResource);
		Assert.assertEquals(vxResource, actualvxResource);
		Mockito.verify(serviceREST).getService(vxResource.getAssetId());
		Mockito.verify(serviceREST).updatePolicy(rangerPolicy, Id);
		Mockito.verify(serviceUtil).toRangerPolicy(vxResource, rangerService);
		Mockito.verify(serviceUtil).toVXResource(rangerPolicy, rangerService);
	}

	@Test
	public void testDeleteXResource() {
		Mockito.doNothing().when(serviceREST).deletePolicy(Id);
		assetREST.deleteXResource(Id, request);
		Mockito.verify(serviceREST).deletePolicy(Id);
	}

	@Test
	public void testSearchXResource() {
		List<RangerPolicy> rangerPolicyList = new ArrayList<RangerPolicy>();
		List<VXResource> vXResourcesList = new ArrayList<VXResource>();
		RangerService rangerService = rangerService(Id);
		long i;
		for (i = 1; i <= 2; i++) {
			RangerPolicy rangerPolicy = rangerPolicy(i);
			VXResource vXresource = vxResource(i);
			rangerPolicyList.add(rangerPolicy);
			vXResourcesList.add(vXresource);
			Mockito.when(serviceUtil.toVXResource(rangerPolicy, rangerService)).thenReturn(vXresource);
		}
		Mockito.when(serviceREST.getServiceByName(rangerPolicyList.get(0).getService())).thenReturn(rangerService);
		VXResourceList expectedVXResourceList = new VXResourceList();
		expectedVXResourceList.setVXResources(vXResourcesList);

		SearchFilter searchFilter = new SearchFilter();
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequest(request, null)).thenReturn(searchFilter);
		Mockito.when(serviceREST.getPolicies(searchFilter)).thenReturn(rangerPolicyList);
		VXResourceList actualVXResourceList = assetREST.searchXResources(request);
		Assert.assertNotNull(actualVXResourceList);
		Assert.assertEquals(expectedVXResourceList.getVXResources(), actualVXResourceList.getVXResources());
		Mockito.verify(searchUtil).getSearchFilterFromLegacyRequest(request, null);
		Mockito.verify(serviceREST).getPolicies(searchFilter);
		for (i = 0; i < 2; i++) {
			Mockito.verify(serviceUtil, Mockito.times(1)).toVXResource(rangerPolicyList.get((int) i), rangerService);
		}
		Mockito.verify(serviceREST, Mockito.times(2)).getServiceByName(rangerPolicyList.get(0).getService());
	}

	@Test
	public void testCountXResource() {
		List<RangerPolicy> rangerPolicyList = new ArrayList<RangerPolicy>();
		List<VXResource> vXResourcesList = new ArrayList<VXResource>();
		RangerService rangerService = rangerService(Id);
		long i;
		for (i = 1; i <= 2; i++) {
			RangerPolicy rangerPolicy = rangerPolicy(i);
			VXResource vXresource = vxResource(i);
			rangerPolicyList.add(rangerPolicy);
			vXResourcesList.add(vXresource);
			Mockito.when(serviceUtil.toVXResource(rangerPolicy, rangerService)).thenReturn(vXresource);
		}
		VXLong expectedXResouce = new VXLong();
		expectedXResouce.setValue(2l);
		Mockito.when(serviceREST.getServiceByName(rangerPolicyList.get(0).getService())).thenReturn(rangerService);
		VXResourceList expectedVXResourceList = new VXResourceList();
		expectedVXResourceList.setVXResources(vXResourcesList);

		SearchFilter searchFilter = new SearchFilter();
		Mockito.when(searchUtil.getSearchFilterFromLegacyRequest(request, null)).thenReturn(searchFilter);
		Mockito.when(serviceREST.getPolicies(searchFilter)).thenReturn(rangerPolicyList);
		VXLong actualXResource = assetREST.countXResources(request);
		Assert.assertEquals(expectedXResouce.getValue(), actualXResource.getValue());
	}

	@Test
	public void testGetXCredentialStore() {
		VXCredentialStore vXCredentialStore = vXCredentialStore();
		Mockito.when(assetMgr.getXCredentialStore(Id)).thenReturn(vXCredentialStore);
		VXCredentialStore actualvXCredentialStore = assetREST.getXCredentialStore(Id);
		Assert.assertNotNull(actualvXCredentialStore);
		Assert.assertEquals(vXCredentialStore, actualvXCredentialStore);
		Mockito.verify(assetMgr).getXCredentialStore(Id);
	}

	@Test
	public void testCreateXCredentialStore() {
		VXCredentialStore vXCredentialStore = vXCredentialStore();
		Mockito.when(assetMgr.createXCredentialStore(vXCredentialStore)).thenReturn(vXCredentialStore);
		VXCredentialStore actualvXCredentialStore = assetREST.createXCredentialStore(vXCredentialStore);
		Assert.assertNotNull(actualvXCredentialStore);
		Assert.assertEquals(vXCredentialStore, actualvXCredentialStore);
		Mockito.verify(assetMgr).createXCredentialStore(vXCredentialStore);
	}

	@Test
	public void testUpdateXCredentialStoree() {
		VXCredentialStore vXCredentialStore = vXCredentialStore();
		Mockito.when(assetMgr.updateXCredentialStore(vXCredentialStore)).thenReturn(vXCredentialStore);
		VXCredentialStore actualvXCredentialStore = assetREST.updateXCredentialStore(vXCredentialStore);
		Assert.assertNotNull(actualvXCredentialStore);
		Assert.assertEquals(vXCredentialStore, actualvXCredentialStore);
		Mockito.verify(assetMgr).updateXCredentialStore(vXCredentialStore);
	}

	@Test
	public void testDeleteXCredentialStore() {
		Mockito.doNothing().when(assetMgr).deleteXCredentialStore(Id, false);
		assetREST.deleteXCredentialStore(Id, request);
		Mockito.verify(assetMgr).deleteXCredentialStore(Id, false);
	}

	@Test
	public void testSearchXCredentialStores() {
		VXCredentialStore vXCredentialStore = vXCredentialStore();
		List<VXCredentialStore> vXCredentialStores = Arrays.asList(vXCredentialStore);
		VXCredentialStoreList vXCredentialStoreList = new VXCredentialStoreList();
		vXCredentialStoreList.setVXCredentialStores(vXCredentialStores);
		SearchCriteria searchCriteria = new SearchCriteria();
		List<SortField> sortFields = null;
		Mockito.when(searchUtil.extractCommonCriterias(request, sortFields)).thenReturn(searchCriteria);
		Mockito.when(assetMgr.searchXCredentialStores(searchCriteria)).thenReturn(vXCredentialStoreList);
		VXCredentialStoreList actualvxCredentialStoreList = assetREST.searchXCredentialStores(request);
		Assert.assertEquals(vXCredentialStoreList.getVXCredentialStores(),
				actualvxCredentialStoreList.getVXCredentialStores());
		Mockito.verify(assetMgr).searchXCredentialStores(searchCriteria);
	}

	@Test
	public void testCountXCredentialStores() {
		VXLong expectedvXLong = new VXLong();
		SearchCriteria searchCriteria = new SearchCriteria();
		List<SortField> sortFields = null;
		Mockito.when(searchUtil.extractCommonCriterias(request, sortFields)).thenReturn(searchCriteria);
		Mockito.when(assetMgr.getXCredentialStoreSearchCount(searchCriteria)).thenReturn(expectedvXLong);
		VXLong actualvXLong = assetREST.countXCredentialStores(request);
		Assert.assertEquals(expectedvXLong, actualvXLong);
		Mockito.verify(assetMgr).getXCredentialStoreSearchCount(searchCriteria);
	}

	@Test
	public void testGetXResourceFile() {
		File file = new File("testGetXResource");
		Response expectedResponse = Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
				.header("Content-Disposition", "attachment;filename=" + file.getName()).build();
		VXResource vxResource = vxResource(Id);
		Mockito.when(
				searchUtil.extractString((HttpServletRequest) Mockito.any(), (SearchCriteria) Mockito.any(),
						(String) Mockito.any(), (String) Mockito.any(), (String) Mockito.any()))
				.thenReturn("json");
		Mockito.when(assetREST.getXResource(Id)).thenReturn(vxResource);
		Mockito.when(assetMgr.getXResourceFile(vxResource, "json")).thenReturn(file);
		Response reponse = assetREST.getXResourceFile(request, Id);
		Assert.assertEquals(expectedResponse.getStatus(), reponse.getStatus());
		Mockito.verify(assetMgr).getXResourceFile(vxResource, "json");
		Mockito.verify(searchUtil).extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), (String) Mockito.any(), (String) Mockito.any(),
				(String) Mockito.any());
	}

	@Test
	public void testGetResourceJSON() {
		RangerService rangerService = rangerService(Id);
		String file = "testGetResourceJSON";
		VXAsset vXAsset = vXAsset(Id);
		Date date = new Date();
		String strdt = date.toString();
		X509Certificate[] certchain = new X509Certificate[1];
		certchain[0] = Mockito.mock(X509Certificate.class);
		ServicePolicies servicePolicies = servicePolicies();
		RangerPolicy rangerPolicy = rangerPolicy(Id);
		List<RangerPolicy> policies = new ArrayList<RangerPolicy>();
		policies.add(rangerPolicy);
		Mockito.when(request.getParameter("epoch")).thenReturn(strdt);
		Mockito.when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(certchain);
		Mockito.when(request.getHeader("X-FORWARDED-FOR")).thenReturn("valid");
		Mockito.when(request.isSecure()).thenReturn(true);
		Mockito.when(request.getParameter("policyCount")).thenReturn("4");
		Mockito.when(request.getParameter("agentId")).thenReturn("12");
		// Mockito.when(PropertiesUtil.getBooleanProperty("ranger.service.http.enabled",true)).thenReturn(true);
		try {
			Mockito.when(serviceREST.getServicePoliciesIfUpdated(Mockito.anyString(), Mockito.anyLong(),
					Mockito.anyLong(), Mockito.anyString(), Mockito.anyString() , Mockito.anyString() , Mockito.anyBoolean(), Mockito.anyString(), (HttpServletRequest) Mockito.any()))
					.thenReturn(servicePolicies);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
		Mockito.when(serviceUtil.getServiceByName("hdfs_dev")).thenReturn(rangerService);
		Mockito.when(serviceUtil.toVXAsset(rangerService)).thenReturn(vXAsset);
		Mockito.when(assetMgr.getLatestRepoPolicy((VXAsset) Mockito.any(), Mockito.<VXResource>anyList(), Mockito.anyLong(),
				(X509Certificate[]) Mockito.any(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString(),
				Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString())).thenReturn(file);
		String actualFile = assetREST.getResourceJSON(request, "hdfs_dev");
		Assert.assertEquals(file, actualFile);
		Mockito.verify(serviceUtil).getServiceByName("hdfs_dev");
		Mockito.verify(serviceUtil).toVXAsset(rangerService);
		Mockito.verify(request).getParameter("epoch");
		Mockito.verify(request).getAttribute("javax.servlet.request.X509Certificate");
		Mockito.verify(request).getHeader("X-FORWARDED-FOR");
		Mockito.verify(request).isSecure();
		Mockito.verify(request).getParameter("policyCount");
		Mockito.verify(request).getParameter("agentId");
		Mockito.verify(assetMgr).getLatestRepoPolicy((VXAsset) Mockito.any(), Mockito.<VXResource>anyList(),
				Mockito.anyLong(), (X509Certificate[]) Mockito.any(), Mockito.anyBoolean(), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void testSearchXPolicyExportAudits() {
		SearchCriteria searchCriteria = new SearchCriteria();
		List<SortField> sortFields = null;
		List<VXPolicyExportAudit> vXPolicyExportAudits = new ArrayList<VXPolicyExportAudit>();
		VXPolicyExportAuditList vXPolicyExportAuditList = new VXPolicyExportAuditList();
		vXPolicyExportAuditList.setVXPolicyExportAudits(vXPolicyExportAudits);
		Mockito.when(searchUtil.extractCommonCriterias(request, sortFields)).thenReturn(searchCriteria);
		Mockito.when(searchUtil.extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn("test");
		Mockito.when(searchUtil.extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn((Integer) 8);
		Mockito.when(assetMgr.searchXPolicyExportAudits(searchCriteria)).thenReturn(vXPolicyExportAuditList);
		VXPolicyExportAuditList expectedVXPolicyExportAuditList = assetREST.searchXPolicyExportAudits(request);
		Assert.assertEquals(vXPolicyExportAuditList, expectedVXPolicyExportAuditList);
		Mockito.verify(searchUtil).extractCommonCriterias(request, sortFields);
		Mockito.verify(searchUtil, Mockito.times(5)).extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil).extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil, Mockito.times(2)).extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), (String)Mockito.isNull());
		Mockito.verify(searchUtil).extractCommonCriterias(request, sortFields);
		Mockito.verify(assetMgr).searchXPolicyExportAudits(searchCriteria);
	}

	@Test
	public void testGetReportLogs() {
		SearchCriteria searchCriteria = new SearchCriteria();
		List<SortField> sortFields = null;
		List<VXTrxLog> vXTrxLogs = new ArrayList<VXTrxLog>();
		VXTrxLogList vXTrxLogList = new VXTrxLogList();
		vXTrxLogList.setVXTrxLogs(vXTrxLogs);
		Mockito.when(searchUtil.extractCommonCriterias(request, sortFields)).thenReturn(searchCriteria);
		Mockito.when(searchUtil.extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn("test");
		Mockito.when(searchUtil.extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn((Integer) 8);
		Mockito.when(searchUtil.extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(new Date());
		Mockito.when(assetMgr.getReportLogs(searchCriteria)).thenReturn(vXTrxLogList);
		VXTrxLogList expectedVXTrxLogListt = assetREST.getReportLogs(request);
		Assert.assertEquals(vXTrxLogList, expectedVXTrxLogListt);
		Mockito.verify(searchUtil, Mockito.times(4)).extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil).extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil, Mockito.times(2)).extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(assetMgr).getReportLogs(searchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias(request, sortFields);
	}

	@Test
	public void testGetTransactionReport() {
		List<VXTrxLog> vXTrxLogs = new ArrayList<VXTrxLog>();
		VXTrxLogList vXTrxLogList = new VXTrxLogList();
		vXTrxLogList.setVXTrxLogs(vXTrxLogs);
		String transactionId = "123456";
		Mockito.when(assetMgr.getTransactionReport(transactionId)).thenReturn(vXTrxLogList);
		VXTrxLogList expectedVXTrxLogListt = assetREST.getTransactionReport(request, transactionId);
		Assert.assertEquals(vXTrxLogList, expectedVXTrxLogListt);
		Mockito.verify(assetMgr).getTransactionReport(transactionId);
	}

	@Test
	public void testGetAccessLogs() {
		SearchCriteria searchCriteria = new SearchCriteria();
		List<SortField> sortFields = null;
		List<VXAccessAudit> vXAccessAudits = new ArrayList<VXAccessAudit>();
		VXAccessAuditList vXAccessAuditList = new VXAccessAuditList();
		vXAccessAuditList.setVXAccessAudits(vXAccessAudits);
		Mockito.when(searchUtil.extractCommonCriterias(request, sortFields)).thenReturn(searchCriteria);
		Mockito.when(searchUtil.extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn("test");
		Mockito.when(searchUtil.extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn((Integer) 8);
		Mockito.when(searchUtil.extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(new Date());
		Mockito.when(searchUtil.extractLong((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn((Long) 8l);
		Mockito.when(msBizUtil.isKeyAdmin()).thenReturn(false);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xxServiceDefDao);
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(Id);
		Mockito.when(xxServiceDefDao.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KMS_NAME))
				.thenReturn(xServiceDef);
		Mockito.when(assetMgr.getAccessLogs(searchCriteria)).thenReturn(vXAccessAuditList);
		VXAccessAuditList expectedVXAccessAuditList = assetREST.getAccessLogs(request);
		Assert.assertEquals(vXAccessAuditList, expectedVXAccessAuditList);
		Mockito.verify(msBizUtil).isKeyAdmin();
		Mockito.verify(assetMgr).getAccessLogs(searchCriteria);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(searchUtil, Mockito.times(14)).extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.nullable(String.class));
		Mockito.verify(searchUtil, Mockito.times(4)).extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil, Mockito.times(2)).extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil).extractLong((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil).extractStringList(Mockito.any(HttpServletRequest.class),
			(SearchCriteria) Mockito.any(), Mockito.eq("requestUser"), Mockito.eq("Users"), Mockito.eq("requestUser"),
			Mockito.any(), Mockito.eq(StringUtil.VALIDATION_TEXT));
		Mockito.verify(searchUtil).extractStringList(Mockito.any(HttpServletRequest.class),
				(SearchCriteria) Mockito.any(), Mockito.eq("excludeUser"), Mockito.eq("Exclude Users"), Mockito.eq("-requestUser"),
				Mockito.any(), Mockito.eq(StringUtil.VALIDATION_TEXT));
		Mockito.verify(searchUtil).extractStringList(Mockito.any(HttpServletRequest.class),
				(SearchCriteria) Mockito.any(), Mockito.eq("zoneName"), Mockito.eq("Zone Name List"), Mockito.eq("zoneName"),
				Mockito.eq(null), Mockito.eq(null));
		Mockito.verify(searchUtil).extractCommonCriterias(Mockito.any(HttpServletRequest.class),
			(List<SortField>) Mockito.any());
		Mockito.verifyNoMoreInteractions(searchUtil, assetMgr, daoManager);

	}

	@Test
	public void testGetAccessLogsForKms() {
		SearchCriteria searchCriteria = new SearchCriteria();
		List<SortField> sortFields = null;
		List<VXAccessAudit> vXAccessAudits = new ArrayList<VXAccessAudit>();
		VXAccessAuditList vXAccessAuditList = new VXAccessAuditList();
		vXAccessAuditList.setVXAccessAudits(vXAccessAudits);
		Mockito.when(searchUtil.extractCommonCriterias(request, sortFields)).thenReturn(searchCriteria);
		Mockito.when(searchUtil.extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn("test");
		Mockito.when(searchUtil.extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn((Integer) 8);
		Mockito.when(searchUtil.extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(new Date());
		Mockito.when(searchUtil.extractLong((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn((Long) 8l);
		Mockito.when(searchUtil.extractLong((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn((Long) 8l);
		Mockito.when(msBizUtil.isKeyAdmin()).thenReturn(true);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xxServiceDefDao);
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(Id);
		Mockito.when(xxServiceDefDao.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KMS_NAME))
				.thenReturn(xServiceDef);
		Mockito.when(assetMgr.getAccessLogs(searchCriteria)).thenReturn(vXAccessAuditList);
		VXAccessAuditList expectedVXAccessAuditList = assetREST.getAccessLogs(request);
		Assert.assertEquals(vXAccessAuditList, expectedVXAccessAuditList);
		Mockito.verify(msBizUtil).isKeyAdmin();
		Mockito.verify(assetMgr).getAccessLogs(searchCriteria);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(searchUtil, Mockito.times(14)).extractString((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.nullable(String.class));
		Mockito.verify(searchUtil, Mockito.times(4)).extractInt((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil, Mockito.times(2)).extractDate((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil).extractLong((HttpServletRequest) Mockito.any(),
				(SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
		Mockito.verify(searchUtil).extractStringList(Mockito.any(HttpServletRequest.class),
			(SearchCriteria) Mockito.any(), Mockito.eq("requestUser"), Mockito.eq("Users"), Mockito.eq("requestUser"),
			Mockito.any(), Mockito.eq(StringUtil.VALIDATION_TEXT));
		Mockito.verify(searchUtil).extractStringList(Mockito.any(HttpServletRequest.class),
			(SearchCriteria) Mockito.any(), Mockito.eq("excludeUser"), Mockito.eq("Exclude Users"), Mockito.eq("-requestUser"),
			Mockito.any(), Mockito.eq(StringUtil.VALIDATION_TEXT));
		Mockito.verify(searchUtil).extractStringList(Mockito.any(HttpServletRequest.class),
				(SearchCriteria) Mockito.any(), Mockito.eq("zoneName"), Mockito.eq("Zone Name List"), Mockito.eq("zoneName"),
				Mockito.eq(null), Mockito.eq(null));
		Mockito.verify(searchUtil).extractCommonCriterias(Mockito.any(HttpServletRequest.class),
			(List<SortField>) Mockito.any());
		Mockito.verifyNoMoreInteractions(searchUtil, assetMgr, daoManager);
	}

	@Test
	public void testGrantPermission() {
		RangerPolicy policy = rangerPolicy(Id);
		RangerService service = rangerService(Id);
		VXPolicy vXPolicy = vXPolicy(policy, service);
		GrantRevokeRequest grantRequestObj = new GrantRevokeRequest();
		grantRequestObj.setAccessTypes(null);
		grantRequestObj.setDelegateAdmin(true);
		grantRequestObj.setEnableAudit(true);
		grantRequestObj.setGrantor("read");
		grantRequestObj.setIsRecursive(true);
		RESTResponse response = Mockito.mock(RESTResponse.class);
		Mockito.when(serviceUtil.toGrantRevokeRequest(vXPolicy)).thenReturn(grantRequestObj);
		try {
			Mockito.when(serviceREST.grantAccess(vXPolicy.getRepositoryName(), grantRequestObj, request))
					.thenReturn(response);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
		VXPolicy expectedVXPolicy = assetREST.grantPermission(request, vXPolicy);
		Assert.assertEquals(vXPolicy, expectedVXPolicy);
		Mockito.verify(serviceUtil).toGrantRevokeRequest(vXPolicy);
		try {
			Mockito.verify(serviceREST).grantAccess(vXPolicy.getRepositoryName(), grantRequestObj, request);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
	}

	@Test
	public void testGrantPermissionWebApplicationException() {
		RangerPolicy policy = rangerPolicy(Id);
		RangerService service = rangerService(Id);
		VXPolicy vXPolicy = vXPolicy(policy, service);
		GrantRevokeRequest grantRequestObj = new GrantRevokeRequest();
		grantRequestObj.setAccessTypes(null);
		grantRequestObj.setDelegateAdmin(true);
		grantRequestObj.setEnableAudit(true);
		grantRequestObj.setGrantor("read");
		grantRequestObj.setIsRecursive(true);
		WebApplicationException webApplicationException = new WebApplicationException();
		Mockito.when(serviceUtil.toGrantRevokeRequest(vXPolicy)).thenReturn(grantRequestObj);
		try {
			Mockito.when(serviceREST.grantAccess(vXPolicy.getRepositoryName(), grantRequestObj, request))
					.thenThrow(webApplicationException);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
		try {
			assetREST.grantPermission(request, vXPolicy);
			fail("Exception not thrown");
		} catch (WebApplicationException e) {
			Assert.assertTrue(true);
		}
		Mockito.verify(serviceUtil).toGrantRevokeRequest(vXPolicy);
		try {
			Mockito.verify(serviceREST).grantAccess(vXPolicy.getRepositoryName(), grantRequestObj, request);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}

	}

	@Test
	public void testRevokePermission() {
		RangerPolicy policy = rangerPolicy(Id);
		RangerService service = rangerService(Id);
		VXPolicy vXPolicy = vXPolicy(policy, service);
		GrantRevokeRequest grantRequestObj = new GrantRevokeRequest();
		grantRequestObj.setAccessTypes(null);
		grantRequestObj.setDelegateAdmin(true);
		grantRequestObj.setEnableAudit(true);
		grantRequestObj.setGrantor("read");
		grantRequestObj.setIsRecursive(true);
		RESTResponse response = Mockito.mock(RESTResponse.class);
		Mockito.when(serviceUtil.toGrantRevokeRequest(vXPolicy)).thenReturn(grantRequestObj);
		try {
			Mockito.when(serviceREST.revokeAccess(vXPolicy.getRepositoryName(), grantRequestObj, request))
					.thenReturn(response);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
		VXPolicy expectedVXPolicy = assetREST.revokePermission(request, vXPolicy);
		Assert.assertEquals(vXPolicy, expectedVXPolicy);
		Mockito.verify(serviceUtil).toGrantRevokeRequest(vXPolicy);
		try {
			Mockito.verify(serviceREST).revokeAccess(vXPolicy.getRepositoryName(), grantRequestObj, request);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
	}

	@Test
	public void testRevokePermissionWebApplicationException() {
		RangerPolicy policy = rangerPolicy(Id);
		RangerService service = rangerService(Id);
		VXPolicy vXPolicy = vXPolicy(policy, service);
		GrantRevokeRequest grantRequestObj = new GrantRevokeRequest();
		grantRequestObj.setAccessTypes(null);
		grantRequestObj.setDelegateAdmin(true);
		grantRequestObj.setEnableAudit(true);
		grantRequestObj.setGrantor("read");
		grantRequestObj.setIsRecursive(true);
		WebApplicationException webApplicationException = new WebApplicationException();
		Mockito.when(serviceUtil.toGrantRevokeRequest(vXPolicy)).thenReturn(grantRequestObj);
		try {
			Mockito.when(serviceREST.revokeAccess(vXPolicy.getRepositoryName(), grantRequestObj, request))
					.thenThrow(webApplicationException);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}
		try {
			assetREST.revokePermission(request, vXPolicy);
			fail("Exception not thrown");
		} catch (WebApplicationException e) {
			Assert.assertTrue(true);
		}
		Mockito.verify(serviceUtil).toGrantRevokeRequest(vXPolicy);
		try {
			Mockito.verify(serviceREST).revokeAccess(vXPolicy.getRepositoryName(), grantRequestObj, request);
		} catch (Exception e) {
			fail("test failed due to: " + e.getMessage());
		}

	}

        @Test
        public void testGetReportLogsForAuditAdmin() {
                SearchCriteria searchCriteria = new SearchCriteria();
                List<VXTrxLog> vXTrxLogs = new ArrayList<VXTrxLog>();
                VXTrxLogList vXTrxLogList = new VXTrxLogList();
                vXTrxLogList.setVXTrxLogs(vXTrxLogs);
                Mockito.when(searchUtil.extractCommonCriterias(request, xTrxLogService.sortFields)).thenReturn(searchCriteria);
                Mockito.when(searchUtil.extractString((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                                .thenReturn("test");
                Mockito.when(searchUtil.extractInt((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                                .thenReturn((Integer) 8);
                Mockito.when(searchUtil.extractDate((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                                .thenReturn(new Date());
                Mockito.when(assetMgr.getReportLogs(searchCriteria)).thenReturn(vXTrxLogList);
                VXTrxLogList expectedVXTrxLogListt = assetREST.getReportLogs(request);
                Assert.assertEquals(vXTrxLogList, expectedVXTrxLogListt);
                Mockito.verify(searchUtil, Mockito.times(4)).extractString((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
                Mockito.verify(searchUtil).extractInt((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
                Mockito.verify(searchUtil, Mockito.times(2)).extractDate((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
                Mockito.verify(assetMgr).getReportLogs(searchCriteria);
                Mockito.verify(searchUtil).extractCommonCriterias(request, xTrxLogService.sortFields);
        }


        @Test
        public void testGetReportLogsForAuditKeyAdmin() {
                SearchCriteria searchCriteria = new SearchCriteria();
                List<VXTrxLog> vXTrxLogs = new ArrayList<VXTrxLog>();
                VXTrxLogList vXTrxLogList = new VXTrxLogList();
                vXTrxLogList.setVXTrxLogs(vXTrxLogs);
                Mockito.when(searchUtil.extractCommonCriterias(request, xTrxLogService.sortFields)).thenReturn(searchCriteria);
                Mockito.when(searchUtil.extractString((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                                .thenReturn("test");
                Mockito.when(searchUtil.extractInt((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                                .thenReturn((Integer) 8);
                Mockito.when(searchUtil.extractDate((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                                .thenReturn(new Date());
                Mockito.when(assetMgr.getReportLogs(searchCriteria)).thenReturn(vXTrxLogList);
                VXTrxLogList expectedVXTrxLogListt = assetREST.getReportLogs(request);
                Assert.assertEquals(vXTrxLogList, expectedVXTrxLogListt);
                Mockito.verify(searchUtil, Mockito.times(4)).extractString((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
                Mockito.verify(searchUtil).extractInt((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString());
                Mockito.verify(searchUtil, Mockito.times(2)).extractDate((HttpServletRequest) Mockito.any(),
                                (SearchCriteria) Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
                Mockito.verify(assetMgr).getReportLogs(searchCriteria);
                Mockito.verify(searchUtil).extractCommonCriterias(request, xTrxLogService.sortFields);
        }

	public Map<String, String> getSampleConfig() {
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
		return configs;
	}

}
