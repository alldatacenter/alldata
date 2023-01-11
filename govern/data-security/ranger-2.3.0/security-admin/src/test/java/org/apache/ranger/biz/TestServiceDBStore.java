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

package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ListUtils;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerFactory;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.apache.ranger.db.*;
import org.apache.ranger.entity.*;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.store.ServicePredicateUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.service.RangerAuditFields;
import org.apache.ranger.service.RangerDataHistService;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.RangerServiceDefService;
import org.apache.ranger.service.RangerServiceService;
import org.apache.ranger.service.RangerServiceWithAssignedIdService;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.view.RangerPolicyList;
import org.apache.ranger.view.RangerServiceDefList;
import org.apache.ranger.view.RangerServiceList;
import org.apache.ranger.view.VXGroupList;
import org.apache.ranger.view.VXString;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserList;
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
public class TestServiceDBStore {
	private static Long Id = 8L;

	@InjectMocks
	ServiceDBStore serviceDBStore = new ServiceDBStore();

	@Mock
	RangerDaoManager daoManager;

	@Mock
	RangerServiceService svcService;

	@Mock
	RangerDataHistService dataHistService;

	@Mock
	RangerServiceDefService serviceDefService;

	@Mock
	RangerPolicyService policyService;

	@Mock
	StringUtil stringUtil;

	@Mock
	XUserService xUserService;

	@Mock
	XUserMgr xUserMgr;

	@Mock
	RangerAuditFields rangerAuditFields;

	@Mock
	ContextUtil contextUtil;

	@Mock
	RangerBizUtil bizUtil;

	@Mock
	RangerServiceWithAssignedIdService svcServiceWithAssignedId;

	@Mock
	RangerFactory factory;

	@Mock
	ServicePredicateUtil predicateUtil;

    @Mock
    PolicyRefUpdater policyRefUpdater;

	@Mock
	XGroupService xGroupService;
	
	
	@Mock
	RESTErrorUtil restErrorUtil;

	@Mock
	AssetMgr assetMgr;

	@Mock
	RangerTransactionSynchronizationAdapter transactionSynchronizationAdapter;

	@Mock
	JSONUtil jsonUtil;

	@Mock
	GUIDUtil guidUtil;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public void setup() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil
				.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
	}
	
	private XXAccessTypeDef rangerKmsAccessTypes(String accessTypeName, int itemId) {
		XXAccessTypeDef accessTypeDefObj = new XXAccessTypeDef();
		accessTypeDefObj.setAddedByUserId(Id);
		accessTypeDefObj.setCreateTime(new Date());
		accessTypeDefObj.setDefid(Long.valueOf(itemId));
		accessTypeDefObj.setId(Long.valueOf(itemId));
		accessTypeDefObj.setItemId(Long.valueOf(itemId));
		accessTypeDefObj.setLabel(accessTypeName);
		accessTypeDefObj.setName(accessTypeName);
		accessTypeDefObj.setOrder(null);
		accessTypeDefObj.setRbkeylabel(null);
		accessTypeDefObj.setUpdatedByUserId(Id);
		accessTypeDefObj.setUpdateTime(new Date());
		return accessTypeDefObj;
	}

	private RangerServiceDef rangerServiceDef() {
		List<RangerServiceConfigDef> configs = new ArrayList<RangerServiceConfigDef>();
		RangerServiceConfigDef serviceConfigDefObj = new RangerServiceConfigDef();
		serviceConfigDefObj.setDefaultValue("xyz");
		serviceConfigDefObj.setDescription("ServiceDef");
		serviceConfigDefObj.setItemId(Id);
		serviceConfigDefObj.setLabel("Username");
		serviceConfigDefObj.setMandatory(true);
		serviceConfigDefObj.setName("username");
		serviceConfigDefObj.setRbKeyDescription(null);
		serviceConfigDefObj.setRbKeyLabel(null);
		serviceConfigDefObj.setRbKeyValidationMessage(null);
		serviceConfigDefObj.setSubType(null);
		configs.add(serviceConfigDefObj);
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
		configs.put("service.admin.users", "testServiceAdminUser1,testServiceAdminUser2");

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
	
	private RangerService rangerKMSService() {
		Map<String, String> configs = new HashMap<String, String>();
		configs.put("username", "servicemgr");
		configs.put("password", "servicemgr");
		configs.put("provider", "kmsurl");
		
		RangerService rangerService = new RangerService();
		rangerService.setId(Id);
		rangerService.setConfigs(configs);
		rangerService.setCreateTime(new Date());
		rangerService.setDescription("service kms policy");
		rangerService.setGuid("1427365526516_835_1");
		rangerService.setIsEnabled(true);
		rangerService.setName("KMS_1");
		rangerService.setPolicyUpdateTime(new Date());
		rangerService.setType("7");
		rangerService.setUpdatedBy("Admin");
		rangerService.setUpdateTime(new Date());
		
		return rangerService;
	}

	private RangerPolicy rangerPolicy() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
		List<String> users = new ArrayList<String>();
		List<String> groups = new ArrayList<String>();
                List<String> policyLabels = new ArrayList<String>();
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
                policy.setPolicyLabels(policyLabels);

		return policy;
	}

	private XXServiceDef serviceDef() {
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setAddedByUserId(Id);
		xServiceDef.setCreateTime(new Date());
		xServiceDef.setDescription("HDFS Repository");
		xServiceDef.setGuid("1427365526516_835_0");
		xServiceDef.setId(Id);
		xServiceDef.setUpdateTime(new Date());
		xServiceDef.setUpdatedByUserId(Id);
		xServiceDef.setImplclassname("RangerServiceHdfs");
		xServiceDef.setLabel("HDFS Repository");
		xServiceDef.setRbkeylabel(null);
		xServiceDef.setRbkeydescription(null);
		xServiceDef.setIsEnabled(true);

		return xServiceDef;
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

	@Test
	public void test11createServiceDef() throws Exception {

		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXAccessTypeDefDao xAccessTypeDefDao = Mockito
				.mock(XXAccessTypeDefDao.class);
		XXPolicyConditionDefDao xPolicyConditionDefDao = Mockito
				.mock(XXPolicyConditionDefDao.class);
		XXContextEnricherDefDao xContextEnricherDefDao = Mockito
				.mock(XXContextEnricherDefDao.class);
		XXEnumDefDao xEnumDefDao = Mockito.mock(XXEnumDefDao.class);

		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);
		XXResourceDef xResourceDef = Mockito.mock(XXResourceDef.class);
		XXAccessTypeDef xAccessTypeDef = Mockito.mock(XXAccessTypeDef.class);
		List<XXAccessTypeDef> xAccessTypeDefs = new ArrayList<XXAccessTypeDef>();
		xAccessTypeDefs.add(xAccessTypeDef);
		List<XXResourceDef> xResourceDefs = new ArrayList<XXResourceDef>();
		xResourceDefs.add(xResourceDef);

		RangerServiceDef serviceDef = new RangerServiceDef();
		Mockito.when(serviceDefService.create(serviceDef)).thenReturn(
				serviceDef);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.getById(null)).thenReturn(xServiceDef);

		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);
		Mockito.when(xResourceDefDao.findByServiceDefId(xServiceDef.getId())).thenReturn(xResourceDefs);

		Mockito.when(daoManager.getXXAccessTypeDef()).thenReturn(
				xAccessTypeDefDao);
		Mockito.when(xAccessTypeDefDao.findByServiceDefId(xServiceDef.getId())).thenReturn(xAccessTypeDefs);

		Mockito.when(daoManager.getXXPolicyConditionDef()).thenReturn(
				xPolicyConditionDefDao);
		Mockito.when(daoManager.getXXContextEnricherDef()).thenReturn(
				xContextEnricherDefDao);

		Mockito.when(daoManager.getXXEnumDef()).thenReturn(xEnumDefDao);

		Mockito.when(serviceDefService.getPopulatedViewObject(xServiceDef))
				.thenReturn(serviceDef);

		RangerServiceDef dbServiceDef = serviceDBStore
				.createServiceDef(serviceDef);
		Assert.assertNotNull(dbServiceDef);
		Assert.assertEquals(dbServiceDef, serviceDef);
		Assert.assertEquals(dbServiceDef.getId(), serviceDef.getId());
		Assert.assertEquals(dbServiceDef.getCreatedBy(),
				serviceDef.getCreatedBy());
		Assert.assertEquals(dbServiceDef.getDescription(),
				serviceDef.getDescription());
		Assert.assertEquals(dbServiceDef.getGuid(), serviceDef.getGuid());
		Assert.assertEquals(dbServiceDef.getImplClass(),
				serviceDef.getImplClass());
		Assert.assertEquals(dbServiceDef.getLabel(), serviceDef.getLabel());
		Assert.assertEquals(dbServiceDef.getName(), serviceDef.getName());
		Assert.assertEquals(dbServiceDef.getRbKeyDescription(),
				serviceDef.getRbKeyDescription());
		Assert.assertEquals(dbServiceDef.getRbKeyLabel(), serviceDef.getLabel());
		Assert.assertEquals(dbServiceDef.getConfigs(), serviceDef.getConfigs());
		Assert.assertEquals(dbServiceDef.getVersion(), serviceDef.getVersion());
		Assert.assertEquals(dbServiceDef.getResources(),
				serviceDef.getResources());
		Mockito.verify(serviceDefService).getPopulatedViewObject(xServiceDef);
		Mockito.verify(serviceDefService).create(serviceDef);
		Mockito.verify(daoManager).getXXServiceConfigDef();
		Mockito.verify(daoManager).getXXEnumDef();
		Mockito.verify(daoManager).getXXAccessTypeDef();
	}

	@Test
	public void test12updateServiceDef() throws Exception {
		setup();
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXAccessTypeDefDao xAccessTypeDefDao = Mockito
				.mock(XXAccessTypeDefDao.class);
		XXPolicyConditionDefDao xPolicyConditionDefDao = Mockito
				.mock(XXPolicyConditionDefDao.class);
		XXContextEnricherDefDao xContextEnricherDefDao = Mockito
				.mock(XXContextEnricherDefDao.class);
		XXEnumDefDao xEnumDefDao = Mockito.mock(XXEnumDefDao.class);
		XXDataMaskTypeDefDao xDataMaskDefDao = Mockito.mock(XXDataMaskTypeDefDao.class);
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);

		RangerServiceDef rangerServiceDef = rangerServiceDef();
		Long serviceDefId = rangerServiceDef.getId();

		List<XXServiceConfigDef> svcConfDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		serviceConfigDefObj.setType("1");
		svcConfDefList.add(serviceConfigDefObj);

		Mockito.when(
				serviceDefService.populateRangerServiceConfigDefToXX(
						Mockito.any(RangerServiceConfigDef.class), Mockito.any(XXServiceConfigDef.class), Mockito.any(XXServiceDef.class),
						Mockito.eq(RangerServiceDefService.OPERATION_CREATE_CONTEXT))).thenReturn(serviceConfigDefObj);
		Mockito.when(xServiceConfigDefDao.create(serviceConfigDefObj))
				.thenReturn(serviceConfigDefObj);

		List<XXResourceDef> resDefList = new ArrayList<XXResourceDef>();
		XXResourceDef resourceDef = new XXResourceDef();
		resourceDef.setAddedByUserId(Id);
		resourceDef.setCreateTime(new Date());
		resourceDef.setDefid(Id);
		resourceDef.setDescription("test");
		resourceDef.setId(Id);
		resDefList.add(resourceDef);

		List<XXAccessTypeDef> accessTypeDefList = new ArrayList<XXAccessTypeDef>();
		XXAccessTypeDef accessTypeDefObj = new XXAccessTypeDef();
		accessTypeDefObj.setAddedByUserId(Id);
		accessTypeDefObj.setCreateTime(new Date());
		accessTypeDefObj.setDefid(Id);
		accessTypeDefObj.setId(Id);
		accessTypeDefObj.setLabel("Read");
		accessTypeDefObj.setName("read");
		accessTypeDefObj.setOrder(null);
		accessTypeDefObj.setRbkeylabel(null);
		accessTypeDefObj.setUpdatedByUserId(Id);
		accessTypeDefObj.setUpdateTime(new Date());
		accessTypeDefList.add(accessTypeDefObj);

		List<XXPolicyConditionDef> policyConditionDefList = new ArrayList<XXPolicyConditionDef>();
		XXPolicyConditionDef policyConditionDefObj = new XXPolicyConditionDef();
		policyConditionDefObj.setAddedByUserId(Id);
		policyConditionDefObj.setCreateTime(new Date());
		policyConditionDefObj.setDefid(Id);
		policyConditionDefObj.setDescription("policy");
		policyConditionDefObj.setId(Id);
		policyConditionDefObj.setName("country");
		policyConditionDefObj.setOrder(0);
		policyConditionDefObj.setUpdatedByUserId(Id);
		policyConditionDefObj.setUpdateTime(new Date());
		policyConditionDefList.add(policyConditionDefObj);

		List<XXContextEnricherDef> contextEnricherDefList = new ArrayList<XXContextEnricherDef>();
		XXContextEnricherDef contextEnricherDefObj = new XXContextEnricherDef();
		contextEnricherDefObj.setAddedByUserId(Id);
		contextEnricherDefObj.setCreateTime(new Date());
		contextEnricherDefObj.setDefid(Id);
		contextEnricherDefObj.setId(Id);
		contextEnricherDefObj.setName("country-provider");
		contextEnricherDefObj
				.setEnricherOptions("contextName=COUNTRY;dataFile=/etc/ranger/data/userCountry.properties");
		contextEnricherDefObj.setEnricher("RangerCountryProvider");
		contextEnricherDefObj.setOrder(null);
		contextEnricherDefObj.setUpdatedByUserId(Id);
		contextEnricherDefObj.setUpdateTime(new Date());
		contextEnricherDefList.add(contextEnricherDefObj);

		List<XXEnumDef> enumDefList = new ArrayList<XXEnumDef>();
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(0);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName("authnType");
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());
		enumDefList.add(enumDefObj);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.getById(serviceDefId)).thenReturn(
				xServiceDef);

		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);

		Mockito.when(daoManager.getXXAccessTypeDef()).thenReturn(
				xAccessTypeDefDao);

		Mockito.when(daoManager.getXXPolicyConditionDef()).thenReturn(
				xPolicyConditionDefDao);

		Mockito.when(daoManager.getXXContextEnricherDef()).thenReturn(
				xContextEnricherDefDao);

		Mockito.when(daoManager.getXXEnumDef()).thenReturn(xEnumDefDao);

		Mockito.when(daoManager.getXXDataMaskTypeDef()).thenReturn(xDataMaskDefDao);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByServiceDefId(serviceDefId)).thenReturn(null);

		Mockito.when(serviceDefService.read(Id)).thenReturn(rangerServiceDef);
		RangerServiceDef dbServiceDef = serviceDBStore
				.updateServiceDef(rangerServiceDef);
		Assert.assertNotNull(dbServiceDef);
		Assert.assertEquals(dbServiceDef, rangerServiceDef);
		Assert.assertEquals(dbServiceDef.getId(), rangerServiceDef.getId());
		Assert.assertEquals(dbServiceDef.getCreatedBy(),
				rangerServiceDef.getCreatedBy());
		Assert.assertEquals(dbServiceDef.getDescription(),
				rangerServiceDef.getDescription());
		Assert.assertEquals(dbServiceDef.getGuid(), rangerServiceDef.getGuid());
		Assert.assertEquals(dbServiceDef.getImplClass(),
				rangerServiceDef.getImplClass());
		Assert.assertEquals(dbServiceDef.getLabel(),
				rangerServiceDef.getLabel());
		Assert.assertEquals(dbServiceDef.getName(), rangerServiceDef.getName());
		Assert.assertEquals(dbServiceDef.getRbKeyDescription(),
				rangerServiceDef.getRbKeyDescription());
		Assert.assertEquals(dbServiceDef.getConfigs(),
				rangerServiceDef.getConfigs());
		Assert.assertEquals(dbServiceDef.getVersion(),
				rangerServiceDef.getVersion());
		Assert.assertEquals(dbServiceDef.getResources(),
				rangerServiceDef.getResources());

	}

	@Test
	public void test13deleteServiceDef() throws Exception {
		setup();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXDataMaskTypeDefDao xDataMaskDefDao = Mockito.mock(XXDataMaskTypeDefDao.class);
		XXAccessTypeDefDao xAccessTypeDefDao = Mockito
				.mock(XXAccessTypeDefDao.class);
		XXAccessTypeDefGrantsDao xAccessTypeDefGrantsDao = Mockito
				.mock(XXAccessTypeDefGrantsDao.class);
		XXPolicyRefAccessTypeDao xPolicyRefAccessTypeDao = Mockito
				.mock(XXPolicyRefAccessTypeDao.class);
		XXPolicyRefConditionDao xPolicyRefConditionDao  = Mockito
				.mock(XXPolicyRefConditionDao.class);
		XXPolicyRefResourceDao xPolicyRefResourceDao = Mockito
				.mock(XXPolicyRefResourceDao.class);
		XXContextEnricherDefDao xContextEnricherDefDao = Mockito
				.mock(XXContextEnricherDefDao.class);
		XXEnumDefDao xEnumDefDao = Mockito.mock(XXEnumDefDao.class);
		XXEnumElementDefDao xEnumElementDefDao = Mockito
				.mock(XXEnumElementDefDao.class);
		XXPolicyConditionDefDao xPolicyConditionDefDao = Mockito
				.mock(XXPolicyConditionDefDao.class);
		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);

		RangerServiceDef rangerServiceDef = rangerServiceDef();
		RangerService rangerService = rangerService();
		String name = "fdfdfds";
		Long serviceDefId = rangerServiceDef.getId();

		List<XXService> xServiceList = new ArrayList<XXService>();
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
		xServiceList.add(xService);

		List<XXAccessTypeDef> accessTypeDefList = new ArrayList<XXAccessTypeDef>();
		XXAccessTypeDef accessTypeDefObj = new XXAccessTypeDef();
		accessTypeDefObj.setAddedByUserId(Id);
		accessTypeDefObj.setCreateTime(new Date());
		accessTypeDefObj.setDefid(Id);
		accessTypeDefObj.setId(Id);
		accessTypeDefObj.setLabel("Read");
		accessTypeDefObj.setName("read");
		accessTypeDefObj.setOrder(null);
		accessTypeDefObj.setRbkeylabel(null);
		accessTypeDefObj.setUpdatedByUserId(Id);
		accessTypeDefObj.setUpdateTime(new Date());
		accessTypeDefList.add(accessTypeDefObj);

		List<XXAccessTypeDefGrants> accessTypeDefGrantslist = new ArrayList<XXAccessTypeDefGrants>();
		XXAccessTypeDefGrants accessTypeDefGrantsObj = new XXAccessTypeDefGrants();
		accessTypeDefGrantsObj.setAddedByUserId(Id);
		accessTypeDefGrantsObj.setAtdId(Id);
		accessTypeDefGrantsObj.setCreateTime(new Date());
		accessTypeDefGrantsObj.setId(Id);
		accessTypeDefGrantsObj.setUpdatedByUserId(Id);
		accessTypeDefGrantsObj.setUpdateTime(new Date());
		accessTypeDefGrantsObj.setImpliedGrant("read");
		accessTypeDefGrantslist.add(accessTypeDefGrantsObj);

		List<XXPolicyItemAccess> policyItemAccessList = new ArrayList<XXPolicyItemAccess>();
		XXPolicyItemAccess policyItemAccess = new XXPolicyItemAccess();
		policyItemAccess.setAddedByUserId(Id);
		policyItemAccess.setCreateTime(new Date());
		policyItemAccess.setPolicyitemid(Id);
		policyItemAccess.setId(Id);
		policyItemAccess.setOrder(1);
		policyItemAccess.setUpdatedByUserId(Id);
		policyItemAccess.setUpdateTime(new Date());
		policyItemAccessList.add(policyItemAccess);

		List<XXContextEnricherDef> contextEnricherDefList = new ArrayList<XXContextEnricherDef>();
		XXContextEnricherDef contextEnricherDefObj = new XXContextEnricherDef();
		contextEnricherDefObj.setAddedByUserId(Id);
		contextEnricherDefObj.setCreateTime(new Date());
		contextEnricherDefObj.setDefid(Id);
		contextEnricherDefObj.setId(Id);
		contextEnricherDefObj.setName("country-provider");
		contextEnricherDefObj
				.setEnricherOptions("contextName=COUNTRY;dataFile=/etc/ranger/data/userCountry.properties");
		contextEnricherDefObj.setEnricher("RangerCountryProvider");
		contextEnricherDefObj.setOrder(null);
		contextEnricherDefObj.setUpdatedByUserId(Id);
		contextEnricherDefObj.setUpdateTime(new Date());
		contextEnricherDefList.add(contextEnricherDefObj);

		List<XXEnumDef> enumDefList = new ArrayList<XXEnumDef>();
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(0);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName("authnType");
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());
		enumDefList.add(enumDefObj);

		List<XXEnumElementDef> xElementsList = new ArrayList<XXEnumElementDef>();
		XXEnumElementDef enumElementDefObj = new XXEnumElementDef();
		enumElementDefObj.setAddedByUserId(Id);
		enumElementDefObj.setCreateTime(new Date());
		enumElementDefObj.setEnumdefid(Id);
		enumElementDefObj.setId(Id);
		enumElementDefObj.setLabel("Authentication");
		enumElementDefObj.setName("authentication");
		enumElementDefObj.setUpdateTime(new Date());
		enumElementDefObj.setUpdatedByUserId(Id);
		enumElementDefObj.setRbkeylabel(null);
		enumElementDefObj.setOrder(0);
		xElementsList.add(enumElementDefObj);

		List<XXPolicyConditionDef> xConditionDefList = new ArrayList<XXPolicyConditionDef>();
		XXPolicyConditionDef policyConditionDefObj = new XXPolicyConditionDef();
		policyConditionDefObj.setAddedByUserId(Id);
		policyConditionDefObj.setCreateTime(new Date());
		policyConditionDefObj.setDefid(Id);
		policyConditionDefObj.setDescription("policy conditio");
		policyConditionDefObj.setId(Id);
		policyConditionDefObj.setName(name);
		policyConditionDefObj.setOrder(1);
		policyConditionDefObj.setLabel("label");
		xConditionDefList.add(policyConditionDefObj);

		List<XXPolicyItemCondition> policyItemConditionList = new ArrayList<XXPolicyItemCondition>();
		XXPolicyItemCondition policyItemCondition = new XXPolicyItemCondition();
		policyItemCondition.setAddedByUserId(Id);
		policyItemCondition.setCreateTime(new Date());
		policyItemCondition.setType(1L);
		policyItemCondition.setId(Id);
		policyItemCondition.setOrder(1);
		policyItemCondition.setPolicyItemId(Id);
		policyItemCondition.setUpdatedByUserId(Id);
		policyItemCondition.setUpdateTime(new Date());
		policyItemConditionList.add(policyItemCondition);

		List<XXResourceDef> resDefList = new ArrayList<XXResourceDef>();
		XXResourceDef resourceDef = new XXResourceDef();
		resourceDef.setAddedByUserId(Id);
		resourceDef.setCreateTime(new Date());
		resourceDef.setDefid(Id);
		resourceDef.setDescription("test");
		resourceDef.setId(Id);
		resDefList.add(resourceDef);

		List<XXPolicyResource> policyResourceList = new ArrayList<XXPolicyResource>();
		XXPolicyResource policyResource = new XXPolicyResource();
		policyResource.setId(Id);
		policyResource.setCreateTime(new Date());
		policyResource.setAddedByUserId(Id);
		policyResource.setIsExcludes(false);
		policyResource.setIsRecursive(false);
		policyResource.setPolicyId(Id);
		policyResource.setResDefId(Id);
		policyResource.setUpdatedByUserId(Id);
		policyResource.setUpdateTime(new Date());
		policyResourceList.add(policyResource);

		List<XXPolicyResourceMap> policyResourceMapList = new ArrayList<XXPolicyResourceMap>();
		XXPolicyResourceMap policyResourceMap = new XXPolicyResourceMap();
		policyResourceMap.setAddedByUserId(Id);
		policyResourceMap.setCreateTime(new Date());
		policyResourceMap.setId(Id);
		policyResourceMap.setOrder(1);
		policyResourceMap.setResourceId(Id);
		policyResourceMap.setUpdatedByUserId(Id);
		policyResourceMap.setUpdateTime(new Date());
		policyResourceMap.setValue("1L");
		policyResourceMapList.add(policyResourceMap);

		List<XXServiceConfigDef> serviceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setAddedByUserId(Id);
		serviceConfigDefObj.setCreateTime(new Date());
		serviceConfigDefObj.setDefaultvalue("simple");
		serviceConfigDefObj.setDescription("service config");
		serviceConfigDefObj.setId(Id);
		serviceConfigDefObj.setIsMandatory(true);
		serviceConfigDefObj.setName(name);
		serviceConfigDefObj.setLabel("username");
		serviceConfigDefObj.setRbkeydescription(null);
		serviceConfigDefObj.setRbkeylabel(null);
		serviceConfigDefObj.setRbKeyValidationMessage(null);
		serviceConfigDefObj.setType("password");
		serviceConfigDefList.add(serviceConfigDefObj);

		List<XXPolicy> policiesList = new ArrayList<XXPolicy>();
		XXPolicy policy = new XXPolicy();
		policy.setAddedByUserId(Id);
		policy.setCreateTime(new Date());
		policy.setDescription("polcy test");
		policy.setGuid("");
		policy.setId(rangerService.getId());
		policy.setIsAuditEnabled(true);
		policy.setName("HDFS_1-1-20150316062453");
		policy.setService(rangerService.getId());
		policiesList.add(policy);

		List<XXPolicyItem> policyItemList = new ArrayList<XXPolicyItem>();
		XXPolicyItem policyItem = new XXPolicyItem();
		policyItem.setAddedByUserId(Id);
		policyItem.setCreateTime(new Date());
		policyItem.setDelegateAdmin(false);
		policyItem.setId(Id);
		policyItem.setOrder(1);
		policyItem.setPolicyId(Id);
		policyItem.setUpdatedByUserId(Id);
		policyItem.setUpdateTime(new Date());
		policyItemList.add(policyItem);

		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(Id);

		List<XXPolicyItemGroupPerm> policyItemGroupPermlist = new ArrayList<XXPolicyItemGroupPerm>();
		XXPolicyItemGroupPerm policyItemGroupPermObj = new XXPolicyItemGroupPerm();
		policyItemGroupPermObj.setAddedByUserId(Id);
		policyItemGroupPermObj.setCreateTime(new Date());
		policyItemGroupPermObj.setGroupId(Id);
		policyItemGroupPermObj.setId(Id);
		policyItemGroupPermObj.setOrder(1);
		policyItemGroupPermObj.setPolicyItemId(Id);
		policyItemGroupPermObj.setUpdatedByUserId(Id);
		policyItemGroupPermObj.setUpdateTime(new Date());
		policyItemGroupPermlist.add(policyItemGroupPermObj);

		List<XXPolicyItemUserPerm> policyItemUserPermList = new ArrayList<XXPolicyItemUserPerm>();
		XXPolicyItemUserPerm policyItemUserPermObj = new XXPolicyItemUserPerm();
		policyItemUserPermObj.setAddedByUserId(Id);
		policyItemUserPermObj.setCreateTime(new Date());
		policyItemUserPermObj.setId(Id);
		policyItemUserPermObj.setOrder(1);
		policyItemUserPermObj.setPolicyItemId(Id);
		policyItemUserPermObj.setUpdatedByUserId(serviceDefId);
		policyItemUserPermObj.setUpdateTime(new Date());
		policyItemUserPermObj.setUserId(Id);
		policyItemUserPermList.add(policyItemUserPermObj);

		List<XXPolicyRefAccessType> policyRefAccessTypeList = new ArrayList<XXPolicyRefAccessType>();
		XXPolicyRefAccessType policyRefAccessType = new XXPolicyRefAccessType();
		policyRefAccessType.setId(Id);
		policyRefAccessType.setAccessTypeName("myAccessType");
		policyRefAccessType.setPolicyId(Id);
		policyRefAccessType.setCreateTime(new Date());
		policyRefAccessType.setUpdateTime(new Date());
		policyRefAccessType.setAddedByUserId(Id);
		policyRefAccessType.setUpdatedByUserId(Id);
		policyRefAccessTypeList.add(policyRefAccessType);

		List<XXPolicyRefCondition> policyRefConditionsList = new ArrayList<XXPolicyRefCondition>();
		XXPolicyRefCondition policyRefCondition = new XXPolicyRefCondition();
		policyRefCondition.setId(Id);
		policyRefCondition.setAddedByUserId(Id);
		policyRefCondition.setConditionDefId(Id);
		policyRefCondition.setConditionName("myConditionName");
		policyRefCondition.setPolicyId(Id);
		policyRefCondition.setUpdatedByUserId(Id);
		policyRefCondition.setCreateTime(new Date());
		policyRefCondition.setUpdateTime(new Date());
		policyRefConditionsList.add(policyRefCondition);

		List<XXPolicyRefResource> policyRefResourcesList = new ArrayList<XXPolicyRefResource>();
		XXPolicyRefResource policyRefResource = new XXPolicyRefResource();
		policyRefResource.setAddedByUserId(Id);
		policyRefResource.setCreateTime(new Date());
		policyRefResource.setId(Id);
		policyRefResource.setPolicyId(Id);
		policyRefResource.setResourceDefId(Id);
		policyRefResource.setUpdateTime(new Date());
		policyRefResource.setResourceName("myresourceName");
		policyRefResourcesList.add(policyRefResource);

		XXUser xUser = new XXUser();
		xUser.setAddedByUserId(Id);
		xUser.setCreateTime(new Date());
		xUser.setCredStoreId(Id);
		xUser.setDescription("user test");
		xUser.setId(Id);
		xUser.setIsVisible(null);
		xUser.setName(name);
		xUser.setStatus(0);
		xUser.setUpdatedByUserId(Id);
		xUser.setUpdateTime(new Date());

		Mockito.when(daoManager.getXXPolicyRefAccessType()).thenReturn(xPolicyRefAccessTypeDao);
		Mockito.when(xPolicyRefAccessTypeDao.findByAccessTypeDefId(Id)).thenReturn(policyRefAccessTypeList);
		Mockito.when(xPolicyRefAccessTypeDao.remove(policyRefAccessType)).thenReturn(true);

		Mockito.when(daoManager.getXXPolicyRefCondition()).thenReturn(xPolicyRefConditionDao);
		Mockito.when(xPolicyRefConditionDao.findByConditionDefId(Id)).thenReturn(policyRefConditionsList);
		Mockito.when(xPolicyRefConditionDao.remove(policyRefCondition)).thenReturn(true);

		Mockito.when(daoManager.getXXPolicyRefResource()).thenReturn(xPolicyRefResourceDao);
		Mockito.when(xPolicyRefResourceDao.findByResourceDefID(Id)).thenReturn(policyRefResourcesList);
		Mockito.when(xPolicyRefResourceDao.remove(policyRefResource)).thenReturn(true);

		Mockito.when(serviceDefService.read(Id)).thenReturn(rangerServiceDef);
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByServiceDefId(serviceDefId)).thenReturn(null);
//				xServiceList);

		Mockito.when(daoManager.getXXAccessTypeDef()).thenReturn(
				xAccessTypeDefDao);
		Mockito.when(xAccessTypeDefDao.findByServiceDefId(serviceDefId))
				.thenReturn(accessTypeDefList);

		Mockito.when(daoManager.getXXAccessTypeDefGrants()).thenReturn(
				xAccessTypeDefGrantsDao);
		Mockito.when(
				xAccessTypeDefGrantsDao.findByATDId(accessTypeDefObj.getId()))
				.thenReturn(accessTypeDefGrantslist);

		Mockito.when(daoManager.getXXContextEnricherDef()).thenReturn(
				xContextEnricherDefDao);
		Mockito.when(xContextEnricherDefDao.findByServiceDefId(serviceDefId))
				.thenReturn(contextEnricherDefList);

		Mockito.when(daoManager.getXXEnumDef()).thenReturn(xEnumDefDao);
		Mockito.when(xEnumDefDao.findByServiceDefId(serviceDefId)).thenReturn(
				enumDefList);

		Mockito.when(daoManager.getXXEnumElementDef()).thenReturn(
				xEnumElementDefDao);
		Mockito.when(xEnumElementDefDao.findByEnumDefId(enumDefObj.getId()))
				.thenReturn(xElementsList);

		Mockito.when(daoManager.getXXPolicyConditionDef()).thenReturn(
				xPolicyConditionDefDao);
		Mockito.when(xPolicyConditionDefDao.findByServiceDefId(serviceDefId))
				.thenReturn(xConditionDefList);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);
		Mockito.when(xResourceDefDao.findByServiceDefId(serviceDefId))
				.thenReturn(resDefList);

		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);
		Mockito.when(xServiceConfigDefDao.findByServiceDefId(serviceDefId))
				.thenReturn(serviceConfigDefList);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);

		svcServiceWithAssignedId.setPopulateExistingBaseFields(true);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByServiceDefId(serviceDefId)).thenReturn(null);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);

		Mockito.when(daoManager.getXXDataMaskTypeDef()).thenReturn(xDataMaskDefDao);
		Mockito.when(xDataMaskDefDao.findByServiceDefId(serviceDefId)).thenReturn(new ArrayList<XXDataMaskTypeDef>());

		serviceDBStore.deleteServiceDef(Id, true);
		Mockito.verify(daoManager).getXXContextEnricherDef();
		Mockito.verify(daoManager).getXXEnumDef();
	}

	@Test
	public void test14getServiceDef() throws Exception {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		Mockito.when(serviceDefService.read(Id)).thenReturn(rangerServiceDef);
		RangerServiceDef dbRangerServiceDef = serviceDBStore.getServiceDef(Id);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getCreatedBy(),
				rangerServiceDef.getCreatedBy());
		Assert.assertEquals(dbRangerServiceDef.getDescription(),
				rangerServiceDef.getDescription());
		Assert.assertEquals(dbRangerServiceDef.getGuid(),
				rangerServiceDef.getGuid());
		Assert.assertEquals(dbRangerServiceDef.getImplClass(),
				rangerServiceDef.getImplClass());
		Assert.assertEquals(dbRangerServiceDef.getLabel(),
				rangerServiceDef.getLabel());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Assert.assertEquals(dbRangerServiceDef.getRbKeyDescription(),
				rangerServiceDef.getRbKeyDescription());
		Assert.assertEquals(dbRangerServiceDef.getConfigs(),
				rangerServiceDef.getConfigs());
		Assert.assertEquals(dbRangerServiceDef.getVersion(),
				rangerServiceDef.getVersion());
		Assert.assertEquals(dbRangerServiceDef.getResources(),
				rangerServiceDef.getResources());
		Mockito.verify(serviceDefService).read(Id);
	}

	@Test
	public void test15getServiceDefByName() throws Exception {
		String name = "fdfdfds";

		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.findByName(name)).thenReturn(xServiceDef);

		RangerServiceDef dbServiceDef = serviceDBStore
				.getServiceDefByName(name);
		Assert.assertNull(dbServiceDef);
		Mockito.verify(daoManager).getXXServiceDef();
	}

	@Test
	public void test16getServiceDefByNameNotNull() throws Exception {
		String name = "fdfdfds";

		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);

		RangerServiceDef serviceDef = new RangerServiceDef();
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.findByName(name)).thenReturn(xServiceDef);
		Mockito.when(serviceDefService.getPopulatedViewObject(xServiceDef))
				.thenReturn(serviceDef);

		RangerServiceDef dbServiceDef = serviceDBStore
				.getServiceDefByName(name);
		Assert.assertNotNull(dbServiceDef);
		Mockito.verify(daoManager).getXXServiceDef();
	}

	@Test
	public void test17getServiceDefs() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");
		List<RangerServiceDef> serviceDefsList = new ArrayList<RangerServiceDef>();
		RangerServiceDef serviceDef = rangerServiceDef();
		serviceDefsList.add(serviceDef);
		RangerServiceDefList serviceDefList = new RangerServiceDefList();
		serviceDefList.setPageSize(0);
		serviceDefList.setResultSize(1);
		serviceDefList.setSortBy("asc");
		serviceDefList.setSortType("1");
		serviceDefList.setStartIndex(0);
		serviceDefList.setTotalCount(10);
		serviceDefList.setServiceDefs(serviceDefsList);
		Mockito.when(serviceDefService.searchRangerServiceDefs(filter))
				.thenReturn(serviceDefList);

		List<RangerServiceDef> dbServiceDef = serviceDBStore
				.getServiceDefs(filter);
		Assert.assertNotNull(dbServiceDef);
		Assert.assertEquals(dbServiceDef, serviceDefsList);
		Assert.assertEquals(dbServiceDef.get(0), serviceDefsList.get(0));
		Mockito.verify(serviceDefService).searchRangerServiceDefs(filter);
	}

	@Test
	public void test18getPaginatedServiceDefs() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		List<RangerServiceDef> serviceDefsList = new ArrayList<RangerServiceDef>();
		RangerServiceDef serviceDef = rangerServiceDef();
		serviceDefsList.add(serviceDef);
		RangerServiceDefList serviceDefList = new RangerServiceDefList();
		serviceDefList.setPageSize(0);
		serviceDefList.setResultSize(1);
		serviceDefList.setSortBy("asc");
		serviceDefList.setSortType("1");
		serviceDefList.setStartIndex(0);
		serviceDefList.setTotalCount(10);
		serviceDefList.setServiceDefs(serviceDefsList);
		Mockito.when(serviceDefService.searchRangerServiceDefs(filter))
				.thenReturn(serviceDefList);

		PList<RangerServiceDef> dbServiceDefList = serviceDBStore
				.getPaginatedServiceDefs(filter);
		Assert.assertNotNull(dbServiceDefList);
		Assert.assertEquals(dbServiceDefList.getList(),
				serviceDefList.getServiceDefs());
		Mockito.verify(serviceDefService).searchRangerServiceDefs(filter);
	}

	@Test
	public void test19createService() throws Exception {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXUserDao xUserDao = Mockito.mock(XXUserDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXService xService = Mockito.mock(XXService.class);

		RangerService rangerService = rangerService();

		List<XXServiceConfigDef> svcConfDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		serviceConfigDefObj.setType("1");
		svcConfDefList.add(serviceConfigDefObj);
		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);

		Mockito.when(svcService.create(rangerService)).thenReturn(rangerService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(rangerService.getId())).thenReturn(
				xService);
		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);

		Mockito.when(daoManager.getXXUser()).thenReturn(xUserDao);

		XXServiceConfigMap xConfMap = new XXServiceConfigMap();

		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		Mockito.when(
				rangerAuditFields.populateAuditFields(
						Mockito.isA(XXServiceConfigMap.class),
						Mockito.isA(XXService.class))).thenReturn(xConfMap);

		RangerServiceDef ran = new RangerServiceDef();
		ran.setName("Test");

		ServiceDBStore spy = Mockito.spy(serviceDBStore);

		Mockito.doNothing().when(spy).createDefaultPolicies(rangerService);

		spy.createService(rangerService);

		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXService();
		Mockito.verify(daoManager).getXXServiceConfigMap();
	}

	@Test
	public void test20updateService() throws Exception {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXUserDao xUserDao = Mockito.mock(XXUserDao.class);
        	XXServiceResourceDao xServiceResourceDao = Mockito.mock(XXServiceResourceDao.class);

		RangerService rangerService = rangerService();
		Map<String, Object> options = null;
		String name = "fdfdfds";

		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		XXTrxLog xTrxLogObj = new XXTrxLog();
		xTrxLogObj.setAction("create");
		xTrxLogObj.setAddedByUserId(Id);
		xTrxLogObj.setAttributeName("User Role");
		xTrxLogObj.setCreateTime(new Date());
		xTrxLogObj.setId(Id);
		xTrxLogObj.setNewValue("admin");
		xTrxLogObj.setObjectClassType(0);
		xTrxLogObj.setObjectId(1L);
		xTrxLogObj.setParentObjectClassType(0);
		xTrxLogObj.setParentObjectId(Id);
		trxLogList.add(xTrxLogObj);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);

		List<XXServiceConfigDef> xServiceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		xServiceConfigDefList.add(serviceConfigDefObj);
		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);
		Mockito.when(daoManager.getXXServiceResource()).thenReturn(xServiceResourceDao);

		Mockito.when(xServiceResourceDao.countTaggedResourcesInServiceId(xService.getId())).thenReturn(0L);
		Mockito.when(svcService.update(rangerService))
				.thenReturn(rangerService);
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);

		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		xConfMapList.add(xConfMap);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(xServiceConfigMapDao.findByServiceId(Id)).thenReturn(
				xConfMapList);
		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(xServiceConfigMapDao.remove(xConfMap)).thenReturn(true);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(daoManager.getXXUser()).thenReturn(xUserDao);

		Mockito.when(
				rangerAuditFields.populateAuditFields(
						Mockito.isA(XXServiceConfigMap.class),
						Mockito.isA(XXService.class))).thenReturn(xConfMap);

		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		RangerService dbRangerService = serviceDBStore
				.updateService(rangerService, options);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getId(), rangerService.getId());
		Assert.assertEquals(dbRangerService.getName(), rangerService.getName());
		Assert.assertEquals(dbRangerService.getCreatedBy(),
				rangerService.getCreatedBy());
		Assert.assertEquals(dbRangerService.getDescription(),
				rangerService.getDescription());
		Assert.assertEquals(dbRangerService.getType(), rangerService.getType());
		Assert.assertEquals(dbRangerService.getVersion(),
				rangerService.getVersion());
		Mockito.verify(daoManager).getXXUser();
	}

	@Test
	public void test21deleteService() throws Exception {
		setup();
		XXPolicyDao xPolicyDao = Mockito.mock(XXPolicyDao.class);
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXPolicyLabelMapDao xPolicyLabelMapDao = Mockito.mock(XXPolicyLabelMapDao.class);
		XXSecurityZoneDao xSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		XXRMSServiceResourceDao xRMSServiceResourceDao = Mockito.mock(XXRMSServiceResourceDao.class);

        RangerService rangerService = rangerService();
		RangerPolicy rangerPolicy = rangerPolicy();
		String name = "HDFS_1-1-20150316062453";

		List<XXPolicy> policiesList = new ArrayList<XXPolicy>();
		XXPolicy policy = new XXPolicy();
		policy.setAddedByUserId(Id);
		policy.setCreateTime(new Date());
		policy.setDescription("polcy test");
		policy.setGuid("");
		policy.setId(rangerService.getId());
		policy.setIsAuditEnabled(true);
		policy.setName("HDFS_1-1-20150316062453");
		policy.setService(rangerService.getId());
		policiesList.add(policy);

		List<Long> policiesIds = new ArrayList<Long>();
		policiesIds.add(Id);

		List<String> zonesNameList =new ArrayList<String>();

		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		XXTrxLog xTrxLogObj = new XXTrxLog();
		xTrxLogObj.setAction("delete");
		xTrxLogObj.setAddedByUserId(Id);
		xTrxLogObj.setAttributeName("User Role");
		xTrxLogObj.setCreateTime(new Date());
		xTrxLogObj.setId(Id);
		xTrxLogObj.setNewValue("admin");
		xTrxLogObj.setObjectClassType(0);
		xTrxLogObj.setObjectId(1L);
		xTrxLogObj.setParentObjectClassType(0);
		xTrxLogObj.setParentObjectId(Id);
		trxLogList.add(xTrxLogObj);

		List<XXPolicyItem> policyItemList = new ArrayList<XXPolicyItem>();
		XXPolicyItem policyItem = new XXPolicyItem();
		policyItem.setAddedByUserId(Id);
		policyItem.setCreateTime(new Date());
		policyItem.setDelegateAdmin(false);
		policyItem.setId(Id);
		policyItem.setOrder(1);
		policyItem.setPolicyId(Id);
		policyItem.setUpdatedByUserId(Id);
		policyItem.setUpdateTime(new Date());
		policyItemList.add(policyItem);

		List<XXPolicyItemCondition> policyItemConditionList = new ArrayList<XXPolicyItemCondition>();
		XXPolicyItemCondition policyItemCondition = new XXPolicyItemCondition();
		policyItemCondition.setAddedByUserId(Id);
		policyItemCondition.setCreateTime(new Date());
		policyItemCondition.setType(1L);
		policyItemCondition.setId(Id);
		policyItemCondition.setOrder(1);
		policyItemCondition.setPolicyItemId(Id);
		policyItemCondition.setUpdatedByUserId(Id);
		policyItemCondition.setUpdateTime(new Date());
		policyItemConditionList.add(policyItemCondition);

		List<XXPolicyItemGroupPerm> policyItemGroupPermList = new ArrayList<XXPolicyItemGroupPerm>();
		XXPolicyItemGroupPerm policyItemGroupPerm = new XXPolicyItemGroupPerm();
		policyItemGroupPerm.setAddedByUserId(Id);
		policyItemGroupPerm.setCreateTime(new Date());
		policyItemGroupPerm.setGroupId(Id);

		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setId(Id);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		xConfMapList.add(xConfMap);
		policyItemGroupPerm.setId(Id);
		policyItemGroupPerm.setOrder(1);
		policyItemGroupPerm.setPolicyItemId(Id);
		policyItemGroupPerm.setUpdatedByUserId(Id);
		policyItemGroupPerm.setUpdateTime(new Date());
		policyItemGroupPermList.add(policyItemGroupPerm);

		List<XXPolicyItemUserPerm> policyItemUserPermList = new ArrayList<XXPolicyItemUserPerm>();
		XXPolicyItemUserPerm policyItemUserPerm = new XXPolicyItemUserPerm();
		policyItemUserPerm.setAddedByUserId(Id);
		policyItemUserPerm.setCreateTime(new Date());
		policyItemUserPerm.setPolicyItemId(Id);
		policyItemUserPerm.setId(Id);
		policyItemUserPerm.setOrder(1);
		policyItemUserPerm.setUpdatedByUserId(Id);
		policyItemUserPerm.setUpdateTime(new Date());
		policyItemUserPermList.add(policyItemUserPerm);

		List<XXPolicyItemAccess> policyItemAccessList = new ArrayList<XXPolicyItemAccess>();
		XXPolicyItemAccess policyItemAccess = new XXPolicyItemAccess();
		policyItemAccess.setAddedByUserId(Id);
		policyItemAccess.setCreateTime(new Date());
		policyItemAccess.setPolicyitemid(Id);
		policyItemAccess.setId(Id);
		policyItemAccess.setOrder(1);
		policyItemAccess.setUpdatedByUserId(Id);
		policyItemAccess.setUpdateTime(new Date());
		policyItemAccessList.add(policyItemAccess);

		List<XXPolicyResource> policyResourceList = new ArrayList<XXPolicyResource>();
		XXPolicyResource policyResource = new XXPolicyResource();
		policyResource.setId(Id);
		policyResource.setCreateTime(new Date());
		policyResource.setAddedByUserId(Id);
		policyResource.setIsExcludes(false);
		policyResource.setIsRecursive(false);
		policyResource.setPolicyId(Id);
		policyResource.setResDefId(Id);
		policyResource.setUpdatedByUserId(Id);
		policyResource.setUpdateTime(new Date());
		policyResourceList.add(policyResource);

		List<XXPolicyResourceMap> policyResourceMapList = new ArrayList<XXPolicyResourceMap>();
		XXPolicyResourceMap policyResourceMap = new XXPolicyResourceMap();
		policyResourceMap.setAddedByUserId(Id);
		policyResourceMap.setCreateTime(new Date());
		policyResourceMap.setId(Id);
		policyResourceMap.setOrder(1);
		policyResourceMap.setResourceId(Id);
		policyResourceMap.setUpdatedByUserId(Id);
		policyResourceMap.setUpdateTime(new Date());
		policyResourceMap.setValue("1L");
		policyResourceMapList.add(policyResourceMap);

		List<XXServiceConfigDef> xServiceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		xServiceConfigDefList.add(serviceConfigDefObj);

		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xSecurityZoneDao);
		Mockito.when(xSecurityZoneDao.findZonesByServiceName(rangerService.getName())).thenReturn(zonesNameList);

		Mockito.when(daoManager.getXXPolicy()).thenReturn(xPolicyDao);
		Mockito.when(xPolicyDao.findPolicyIdsByServiceId(rangerService.getId()))
				.thenReturn(policiesIds);
		Mockito.when(svcService.delete(rangerService)).thenReturn(true);

		Mockito.when(svcService.getTransactionLog(rangerService, 3))
				.thenReturn(trxLogList);

		Mockito.when(policyService.read(Id)).thenReturn(rangerPolicy);
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(rangerService.getId())).thenReturn(
				xService);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(
				xServiceConfigMapDao.findByServiceId(rangerService.getId()))
				.thenReturn(xConfMapList);
		Mockito.when(daoManager.getXXPolicyLabelMap()).thenReturn(xPolicyLabelMapDao);
		Mockito.when(xPolicyLabelMapDao.findByPolicyId(rangerPolicy.getId())).thenReturn(ListUtils.EMPTY_LIST);

		Mockito.when(daoManager.getXXRMSServiceResource()).thenReturn(xRMSServiceResourceDao);

		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);
        serviceDBStore.deleteService(Id);
		Mockito.verify(svcService).delete(rangerService);
	}

	@Test
	public void test22getService() throws Exception {
		RangerService rangerService = rangerService();
		XXService xService = xService();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);
		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);

		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);
		RangerService dbRangerService = serviceDBStore.getService(Id);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getCreatedBy(),
				rangerService.getCreatedBy());
		Assert.assertEquals(dbRangerService.getDescription(),
				rangerService.getDescription());
		Assert.assertEquals(dbRangerService.getGuid(), rangerService.getGuid());
		Assert.assertEquals(dbRangerService.getName(), rangerService.getName());
		Assert.assertEquals(dbRangerService.getType(), rangerService.getType());
		Assert.assertEquals(dbRangerService.getUpdatedBy(),
				rangerService.getUpdatedBy());
		Assert.assertEquals(dbRangerService.getConfigs(),
				rangerService.getConfigs());
		Assert.assertEquals(dbRangerService.getCreateTime(),
				rangerService.getCreateTime());
		Assert.assertEquals(dbRangerService.getId(), rangerService.getId());
		Assert.assertEquals(dbRangerService.getPolicyVersion(),
				rangerService.getPolicyVersion());
		Assert.assertEquals(dbRangerService.getVersion(),
				rangerService.getVersion());
		Assert.assertEquals(dbRangerService.getPolicyUpdateTime(),
				rangerService.getPolicyUpdateTime());
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(bizUtil).hasAccess(xService, null);
		Mockito.verify(svcService).getPopulatedViewObject(xService);
	}

	@Test
	public void test23getServiceByName() throws Exception {

		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		RangerService rangerService = rangerService();
		XXService xService = xService();
		String name = rangerService.getName();

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByName(name)).thenReturn(xService);
		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);
		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		RangerService dbRangerService = serviceDBStore.getServiceByName(name);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, rangerService);
		Assert.assertEquals(dbRangerService.getName(), rangerService.getName());
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(bizUtil).hasAccess(xService, null);
		Mockito.verify(svcService).getPopulatedViewObject(xService);
	}

	@Test
	public void test24getServices() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		List<RangerService> serviceList = new ArrayList<RangerService>();
		RangerService rangerService = rangerService();
		serviceList.add(rangerService);

		RangerServiceList serviceListObj = new RangerServiceList();
		serviceListObj.setPageSize(0);
		serviceListObj.setResultSize(1);
		serviceListObj.setSortBy("asc");
		serviceListObj.setSortType("1");
		serviceListObj.setStartIndex(0);
		serviceListObj.setTotalCount(10);
		serviceListObj.setServices(serviceList);

		Mockito.when(svcService.searchRangerServices(filter)).thenReturn(
				serviceListObj);
		List<RangerService> dbRangerService = serviceDBStore
				.getServices(filter);
		Assert.assertNotNull(dbRangerService);
		Assert.assertEquals(dbRangerService, serviceList);
		Mockito.verify(svcService).searchRangerServices(filter);
	}

	@Test
	public void test25getPaginatedServiceDefs() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		List<RangerService> serviceList = new ArrayList<RangerService>();
		RangerService rangerService = rangerService();
		serviceList.add(rangerService);

		RangerServiceList serviceListObj = new RangerServiceList();
		serviceListObj.setPageSize(0);
		serviceListObj.setResultSize(1);
		serviceListObj.setSortBy("asc");
		serviceListObj.setSortType("1");
		serviceListObj.setStartIndex(0);
		serviceListObj.setTotalCount(10);
		serviceListObj.setServices(serviceList);

		Mockito.when(svcService.searchRangerServices(filter)).thenReturn(
				serviceListObj);

		PList<RangerService> dbServiceList = serviceDBStore
				.getPaginatedServices(filter);
		Assert.assertNotNull(dbServiceList);
		Assert.assertEquals(dbServiceList.getList(),
				serviceListObj.getServices());

		Mockito.verify(svcService).searchRangerServices(filter);
	}

	@Test
	public void test26createPolicy() throws Exception {
		setup();
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXPolicy xPolicy = Mockito.mock(XXPolicy.class);
		XXPolicyDao xPolicyDao = Mockito.mock(XXPolicyDao.class);
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);

		XXServiceDef xServiceDef = serviceDef();
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

		String policyName = "HDFS_1-1-20150316062345";
		String name = "HDFS_1-1-20150316062453";

		List<RangerPolicyItemAccess> accessesList = new ArrayList<RangerPolicyItemAccess>();
		RangerPolicyItemAccess policyItemAccess = new RangerPolicyItemAccess();
		policyItemAccess.setIsAllowed(true);
		policyItemAccess.setType("1");
		List<String> usersList = new ArrayList<String>();
		List<String> groupsList = new ArrayList<String>();
		List<String> rolesList = new ArrayList<String>();
                List<String> policyLabels = new ArrayList<String>();
		List<RangerPolicyItemCondition> conditionsList = new ArrayList<RangerPolicyItemCondition>();
		RangerPolicyItemCondition policyItemCondition = new RangerPolicyItemCondition();
		policyItemCondition.setType("1");
		policyItemCondition.setValues(usersList);
		conditionsList.add(policyItemCondition);

		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicy.RangerPolicyItem>();
		RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
		rangerPolicyItem.setDelegateAdmin(false);
		rangerPolicyItem.setAccesses(accessesList);
		rangerPolicyItem.setConditions(conditionsList);
		rangerPolicyItem.setGroups(groupsList);
		rangerPolicyItem.setUsers(usersList);
		policyItems.add(rangerPolicyItem);

		List<RangerPolicyItem> policyItemsSet = new ArrayList<RangerPolicy.RangerPolicyItem>();
		RangerPolicyItem paramPolicyItem = new RangerPolicyItem(accessesList,
				usersList, groupsList, rolesList, conditionsList, false);
		paramPolicyItem.setDelegateAdmin(false);
		paramPolicyItem.setAccesses(accessesList);
		paramPolicyItem.setConditions(conditionsList);
		paramPolicyItem.setGroups(groupsList);
		rangerPolicyItem.setUsers(usersList);
		policyItemsSet.add(paramPolicyItem);

		XXPolicyItem xPolicyItem = new XXPolicyItem();
		xPolicyItem.setDelegateAdmin(false);
		xPolicyItem.setAddedByUserId(null);
		xPolicyItem.setCreateTime(new Date());
		xPolicyItem.setGUID(null);
		xPolicyItem.setId(Id);
		xPolicyItem.setOrder(null);
		xPolicyItem.setPolicyId(Id);
		xPolicyItem.setUpdatedByUserId(null);
		xPolicyItem.setUpdateTime(new Date());

		XXPolicy xxPolicy = new XXPolicy();
		xxPolicy.setId(Id);
		xxPolicy.setName(name);
		xxPolicy.setAddedByUserId(Id);
		xxPolicy.setCreateTime(new Date());
		xxPolicy.setDescription("test");
		xxPolicy.setIsAuditEnabled(true);
		xxPolicy.setIsEnabled(true);
		xxPolicy.setService(1L);
		xxPolicy.setUpdatedByUserId(Id);
		xxPolicy.setUpdateTime(new Date());

		List<XXServiceConfigDef> xServiceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		xServiceConfigDefList.add(serviceConfigDefObj);

		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setId(Id);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		xConfMapList.add(xConfMap);

		List<String> users = new ArrayList<String>();

		RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
		rangerPolicyResource.setIsExcludes(true);
		rangerPolicyResource.setIsRecursive(true);
		rangerPolicyResource.setValue("1");
		rangerPolicyResource.setValues(users);

		Map<String, RangerPolicyResource> policyResource = new HashMap<String, RangerPolicyResource>();
		policyResource.put(name, rangerPolicyResource);
		policyResource.put(policyName, rangerPolicyResource);
		RangerPolicy rangerPolicy = new RangerPolicy();
		rangerPolicy.setId(Id);
		rangerPolicy.setCreateTime(new Date());
		rangerPolicy.setDescription("policy");
		rangerPolicy.setGuid("policyguid");
		rangerPolicy.setIsEnabled(true);
		rangerPolicy.setName("HDFS_1-1-20150316062453");
		rangerPolicy.setUpdatedBy("Admin");
		rangerPolicy.setUpdateTime(new Date());
		rangerPolicy.setService("HDFS_1-1-20150316062453");
		rangerPolicy.setIsAuditEnabled(true);
		rangerPolicy.setPolicyItems(policyItems);
		rangerPolicy.setResources(policyResource);
                rangerPolicy.setPolicyLabels(policyLabels);

		XXPolicyResource xPolicyResource = new XXPolicyResource();
		xPolicyResource.setAddedByUserId(Id);
		xPolicyResource.setCreateTime(new Date());
		xPolicyResource.setId(Id);
		xPolicyResource.setIsExcludes(true);
		xPolicyResource.setIsRecursive(true);
		xPolicyResource.setPolicyId(Id);
		xPolicyResource.setResDefId(Id);
		xPolicyResource.setUpdatedByUserId(Id);
		xPolicyResource.setUpdateTime(new Date());

		List<XXPolicyConditionDef> policyConditionDefList = new ArrayList<XXPolicyConditionDef>();
		XXPolicyConditionDef policyConditionDefObj = new XXPolicyConditionDef();
		policyConditionDefObj.setAddedByUserId(Id);
		policyConditionDefObj.setCreateTime(new Date());
		policyConditionDefObj.setDefid(Id);
		policyConditionDefObj.setDescription("policy");
		policyConditionDefObj.setId(Id);
		policyConditionDefObj.setName("country");
		policyConditionDefObj.setOrder(0);
		policyConditionDefObj.setUpdatedByUserId(Id);
		policyConditionDefObj.setUpdateTime(new Date());
		policyConditionDefList.add(policyConditionDefObj);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByName(name)).thenReturn(xService);

		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.findByName(rangerService.getType()))
				.thenReturn(xServiceDef);

		Mockito.when(daoManager.getXXPolicy()).thenReturn(xPolicyDao);

		Mockito.when(policyService.create(rangerPolicy, true)).thenReturn(
				rangerPolicy);

		Mockito.when(daoManager.getXXPolicy()).thenReturn(xPolicyDao);
		Mockito.when(xPolicyDao.getById(Id)).thenReturn(xPolicy);
		Mockito.doNothing().when(policyRefUpdater).createNewPolMappingForRefTable(rangerPolicy, xPolicy, xServiceDef);
		Mockito.when(policyService.getPopulatedViewObject(xPolicy)).thenReturn(rangerPolicy);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		RangerPolicyResourceSignature signature = Mockito
				.mock(RangerPolicyResourceSignature.class);
		Mockito.when(factory.createPolicyResourceSignature(rangerPolicy))
				.thenReturn(signature);
		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);

		RangerPolicy dbRangerPolicy = serviceDBStore.createPolicy(rangerPolicy);

		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(Id, dbRangerPolicy.getId());
	}

	@Test
	public void tess27getPolicy() throws Exception {
		RangerPolicy rangerPolicy = rangerPolicy();
		Mockito.when(policyService.read(Id)).thenReturn(rangerPolicy);
		RangerPolicy dbRangerPolicy = serviceDBStore.getPolicy(Id);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(), rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getName(), rangerPolicy.getName());
		Assert.assertEquals(dbRangerPolicy.getCreatedBy(),
				rangerPolicy.getCreatedBy());
		Assert.assertEquals(dbRangerPolicy.getDescription(),
				rangerPolicy.getDescription());
		Assert.assertEquals(dbRangerPolicy.getGuid(), rangerPolicy.getGuid());
		Assert.assertEquals(dbRangerPolicy.getService(),
				rangerPolicy.getService());
		Assert.assertEquals(dbRangerPolicy.getUpdatedBy(),
				rangerPolicy.getUpdatedBy());
		Assert.assertEquals(dbRangerPolicy.getCreateTime(),
				rangerPolicy.getCreateTime());
		Assert.assertEquals(dbRangerPolicy.getIsAuditEnabled(),
				rangerPolicy.getIsAuditEnabled());
		Assert.assertEquals(dbRangerPolicy.getIsEnabled(),
				rangerPolicy.getIsEnabled());
		Assert.assertEquals(dbRangerPolicy.getPolicyItems(),
				rangerPolicy.getPolicyItems());
		Assert.assertEquals(dbRangerPolicy.getVersion(),
				rangerPolicy.getVersion());
		Mockito.verify(policyService).read(Id);

	}

	@Test
	public void tess28updatePolicy() throws Exception {
		setup();
		XXPolicyDao xPolicyDao = Mockito.mock(XXPolicyDao.class);
		XXPolicy xPolicy = Mockito.mock(XXPolicy.class);
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);
		XXPolicyLabelMapDao xPolicyLabelMapDao = Mockito.mock(XXPolicyLabelMapDao.class);

		RangerService rangerService = rangerService();

		RangerPolicy rangerPolicy = rangerPolicy();
		String name = "HDFS_1-1-20150316062453";

		List<XXPolicyResource> policyResourceList = new ArrayList<XXPolicyResource>();
		XXPolicyResource policyResource = new XXPolicyResource();
		policyResource.setId(Id);
		policyResource.setCreateTime(new Date());
		policyResource.setAddedByUserId(Id);
		policyResource.setIsExcludes(false);
		policyResource.setIsRecursive(false);
		policyResource.setPolicyId(Id);
		policyResource.setResDefId(Id);
		policyResource.setUpdatedByUserId(Id);
		policyResource.setUpdateTime(new Date());
		policyResourceList.add(policyResource);

		List<XXPolicyResourceMap> policyResourceMapList = new ArrayList<XXPolicyResourceMap>();
		XXPolicyResourceMap policyResourceMap = new XXPolicyResourceMap();
		policyResourceMap.setAddedByUserId(Id);
		policyResourceMap.setCreateTime(new Date());
		policyResourceMap.setId(Id);
		policyResourceMap.setOrder(1);
		policyResourceMap.setResourceId(Id);
		policyResourceMap.setUpdatedByUserId(Id);
		policyResourceMap.setUpdateTime(new Date());
		policyResourceMap.setValue("1L");
		policyResourceMapList.add(policyResourceMap);

		List<XXServiceConfigDef> xServiceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		xServiceConfigDefList.add(serviceConfigDefObj);

		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setId(Id);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		xConfMapList.add(xConfMap);

		Mockito.when(daoManager.getXXPolicy()).thenReturn(xPolicyDao);
		Mockito.when(xPolicyDao.getById(Id)).thenReturn(xPolicy);
		Mockito.when(policyService.getPopulatedViewObject(xPolicy)).thenReturn(
				rangerPolicy);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByName(name)).thenReturn(xService);
		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.findByName(rangerService.getType()))
				.thenReturn(xServiceDef);

		Mockito.when(policyService.update(rangerPolicy)).thenReturn(
				rangerPolicy);
		Mockito.when(daoManager.getXXPolicy()).thenReturn(xPolicyDao);
		Mockito.when(xPolicyDao.getById(rangerPolicy.getId())).thenReturn(
				xPolicy);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(rangerService.getId())).thenReturn(
				xService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(rangerService.getId())).thenReturn(
				xService);
		Mockito.when(daoManager.getXXPolicyLabelMap()).thenReturn(xPolicyLabelMapDao);
		Mockito.when(xPolicyLabelMapDao.findByPolicyId(rangerPolicy.getId())).thenReturn(ListUtils.EMPTY_LIST);


		RangerPolicyResourceSignature signature = Mockito
				.mock(RangerPolicyResourceSignature.class);
		Mockito.when(factory.createPolicyResourceSignature(rangerPolicy))
				.thenReturn(signature);
		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);
        Mockito.when(policyRefUpdater.cleanupRefTables(rangerPolicy)).thenReturn(true);


        RangerPolicy dbRangerPolicy = serviceDBStore.updatePolicy(rangerPolicy);
		Assert.assertNotNull(dbRangerPolicy);
		Assert.assertEquals(dbRangerPolicy, rangerPolicy);
		Assert.assertEquals(dbRangerPolicy.getId(), rangerPolicy.getId());
		Assert.assertEquals(dbRangerPolicy.getCreatedBy(),
				rangerPolicy.getCreatedBy());
		Assert.assertEquals(dbRangerPolicy.getDescription(),
				rangerPolicy.getDescription());
		Assert.assertEquals(dbRangerPolicy.getName(), rangerPolicy.getName());
		Assert.assertEquals(dbRangerPolicy.getGuid(), rangerPolicy.getGuid());
		Assert.assertEquals(dbRangerPolicy.getService(),
				rangerPolicy.getService());
		Assert.assertEquals(dbRangerPolicy.getIsEnabled(),
				rangerPolicy.getIsEnabled());
		Assert.assertEquals(dbRangerPolicy.getVersion(),
				rangerPolicy.getVersion());
	}

	@Test
	public void tess29deletePolicy() throws Exception {
		setup();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXPolicyLabelMapDao xPolicyLabelMapDao = Mockito.mock(XXPolicyLabelMapDao.class);

		RangerService rangerService = rangerService();
		RangerPolicy rangerPolicy = rangerPolicy();
		String name = "HDFS_1-1-20150316062453";

		List<XXPolicyItem> policyItemList = new ArrayList<XXPolicyItem>();
		XXPolicyItem policyItem = new XXPolicyItem();
		policyItem.setAddedByUserId(Id);
		policyItem.setCreateTime(new Date());
		policyItem.setDelegateAdmin(false);
		policyItem.setId(Id);
		policyItem.setOrder(1);
		policyItem.setPolicyId(Id);
		policyItem.setUpdatedByUserId(Id);
		policyItem.setUpdateTime(new Date());
		policyItemList.add(policyItem);

		List<XXPolicyItemCondition> policyItemConditionList = new ArrayList<XXPolicyItemCondition>();
		XXPolicyItemCondition policyItemCondition = new XXPolicyItemCondition();
		policyItemCondition.setAddedByUserId(Id);
		policyItemCondition.setCreateTime(new Date());
		policyItemCondition.setType(1L);
		policyItemCondition.setId(Id);
		policyItemCondition.setOrder(1);
		policyItemCondition.setPolicyItemId(Id);
		policyItemCondition.setUpdatedByUserId(Id);
		policyItemCondition.setUpdateTime(new Date());
		policyItemConditionList.add(policyItemCondition);

		List<XXPolicyItemGroupPerm> policyItemGroupPermList = new ArrayList<XXPolicyItemGroupPerm>();
		XXPolicyItemGroupPerm policyItemGroupPerm = new XXPolicyItemGroupPerm();
		policyItemGroupPerm.setAddedByUserId(Id);
		policyItemGroupPerm.setCreateTime(new Date());
		policyItemGroupPerm.setGroupId(Id);

		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setId(Id);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		xConfMapList.add(xConfMap);
		policyItemGroupPerm.setId(Id);
		policyItemGroupPerm.setOrder(1);
		policyItemGroupPerm.setPolicyItemId(Id);
		policyItemGroupPerm.setUpdatedByUserId(Id);
		policyItemGroupPerm.setUpdateTime(new Date());
		policyItemGroupPermList.add(policyItemGroupPerm);

		List<XXPolicyItemUserPerm> policyItemUserPermList = new ArrayList<XXPolicyItemUserPerm>();
		XXPolicyItemUserPerm policyItemUserPerm = new XXPolicyItemUserPerm();
		policyItemUserPerm.setAddedByUserId(Id);
		policyItemUserPerm.setCreateTime(new Date());
		policyItemUserPerm.setPolicyItemId(Id);
		policyItemUserPerm.setId(Id);
		policyItemUserPerm.setOrder(1);
		policyItemUserPerm.setUpdatedByUserId(Id);
		policyItemUserPerm.setUpdateTime(new Date());
		policyItemUserPermList.add(policyItemUserPerm);

		List<XXPolicyItemAccess> policyItemAccessList = new ArrayList<XXPolicyItemAccess>();
		XXPolicyItemAccess policyItemAccess = new XXPolicyItemAccess();
		policyItemAccess.setAddedByUserId(Id);
		policyItemAccess.setCreateTime(new Date());
		policyItemAccess.setPolicyitemid(Id);
		policyItemAccess.setId(Id);
		policyItemAccess.setOrder(1);
		policyItemAccess.setUpdatedByUserId(Id);
		policyItemAccess.setUpdateTime(new Date());
		policyItemAccessList.add(policyItemAccess);

		List<XXPolicyResource> policyResourceList = new ArrayList<XXPolicyResource>();
		XXPolicyResource policyResource = new XXPolicyResource();
		policyResource.setId(Id);
		policyResource.setCreateTime(new Date());
		policyResource.setAddedByUserId(Id);
		policyResource.setIsExcludes(false);
		policyResource.setIsRecursive(false);
		policyResource.setPolicyId(Id);
		policyResource.setResDefId(Id);
		policyResource.setUpdatedByUserId(Id);
		policyResource.setUpdateTime(new Date());
		policyResourceList.add(policyResource);

		XXPolicyResourceMap policyResourceMap = new XXPolicyResourceMap();
		policyResourceMap.setAddedByUserId(Id);
		policyResourceMap.setCreateTime(new Date());
		policyResourceMap.setId(Id);
		policyResourceMap.setOrder(1);
		policyResourceMap.setResourceId(Id);
		policyResourceMap.setUpdatedByUserId(Id);
		policyResourceMap.setUpdateTime(new Date());
		policyResourceMap.setValue("1L");
		List<XXServiceConfigDef> xServiceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		xServiceConfigDefList.add(serviceConfigDefObj);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.findByName(name)).thenReturn(xService);
		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(rangerService.getId())).thenReturn(
				xService);
		Mockito.when(daoManager.getXXPolicyLabelMap()).thenReturn(xPolicyLabelMapDao);
		Mockito.when(xPolicyLabelMapDao.findByPolicyId(rangerPolicy.getId())).thenReturn(ListUtils.EMPTY_LIST);

		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);
        Mockito.when(policyRefUpdater.cleanupRefTables(rangerPolicy)).thenReturn(true);

		serviceDBStore.deletePolicy(rangerPolicy);
	}

	@Test
	public void test30getPolicies() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		List<RangerPolicy> rangerPolicyLists = new ArrayList<RangerPolicy>();
		RangerPolicy rangerPolicy = rangerPolicy();
		rangerPolicyLists.add(rangerPolicy);

		RangerPolicyList policyListObj = new RangerPolicyList();
		policyListObj.setPageSize(0);
		policyListObj.setResultSize(1);
		policyListObj.setSortBy("asc");
		policyListObj.setSortType("1");
		policyListObj.setStartIndex(0);
		policyListObj.setTotalCount(10);

		List<RangerPolicy> dbRangerPolicy = serviceDBStore.getPolicies(filter);
		Assert.assertNotNull(dbRangerPolicy);
	}

	@Test
	public void test31getPaginatedPolicies() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		RangerPolicyList policyListObj = new RangerPolicyList();
		policyListObj.setPageSize(0);
		policyListObj.setResultSize(1);
		policyListObj.setSortBy("asc");
		policyListObj.setSortType("1");
		policyListObj.setStartIndex(0);
		policyListObj.setTotalCount(10);

		PList<RangerPolicy> dbRangerPolicyList = serviceDBStore
				.getPaginatedPolicies(filter);
		Assert.assertNotNull(dbRangerPolicyList);
	}

	@Test
	public void test32getServicePolicies() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		XXService xService = xService();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		thrown.expect(Exception.class);
		List<RangerPolicy> dbRangerPolicy = serviceDBStore.getServicePolicies(
				Id, filter);
        Assert.assertFalse(dbRangerPolicy.isEmpty());
		Mockito.verify(daoManager).getXXService();
	}

	@Test
	public void test33getServicePoliciesIfUpdated() throws Exception {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceVersionInfoDao xServiceVersionInfoDao = Mockito.mock(XXServiceVersionInfoDao.class);

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

		XXServiceVersionInfo xServiceVersionInfo = new XXServiceVersionInfo();

		xServiceVersionInfo.setServiceId(Id);
		xServiceVersionInfo.setPolicyVersion(1L);
		xServiceVersionInfo.setPolicyUpdateTime(new Date());
		xServiceVersionInfo.setTagVersion(1L);
		xServiceVersionInfo.setTagUpdateTime(new Date());

		String serviceName = "HDFS_1";
		Long lastKnownVersion = 1l;
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(daoManager.getXXServiceVersionInfo()).thenReturn(xServiceVersionInfoDao);
		Mockito.when(xServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(xServiceVersionInfoDao.findByServiceName(serviceName)).thenReturn(xServiceVersionInfo);

		ServicePolicies dbServicePolicies = serviceDBStore
				.getServicePoliciesIfUpdated(serviceName, lastKnownVersion, true);
		Assert.assertNull(dbServicePolicies);
	}

	@Test
	public void test34getPolicyFromEventTime() {
		XXDataHistDao xDataHistDao = Mockito.mock(XXDataHistDao.class);
		XXDataHist xDataHist = Mockito.mock(XXDataHist.class);

		String eventTime = "2015-03-16 06:24:54";
		Mockito.when(daoManager.getXXDataHist()).thenReturn(xDataHistDao);
		Mockito.when(
				xDataHistDao.findObjByEventTimeClassTypeAndId(eventTime, 1020,
						Id)).thenReturn(xDataHist);

		RangerPolicy dbRangerPolicy = serviceDBStore.getPolicyFromEventTime(
				eventTime, Id);
		Assert.assertNull(dbRangerPolicy);
		Mockito.verify(daoManager).getXXDataHist();
	}

	@Test
	public void test35getPopulateExistingBaseFields() {
		Boolean isFound = serviceDBStore.getPopulateExistingBaseFields();
		Assert.assertFalse(isFound);
	}

	@Test
	public void test36getPaginatedServicePolicies() throws Exception {
		String serviceName = "HDFS_1";
		RangerPolicyList policyList = new RangerPolicyList();
		policyList.setPageSize(0);
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		PList<RangerPolicy> dbRangerPolicyList = serviceDBStore
				.getPaginatedServicePolicies(serviceName, filter);
		Assert.assertNotNull(dbRangerPolicyList);
	}

	@Test
	public void test37getPaginatedServicePolicies() throws Exception {

		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.POLICY_NAME, "policyName");
		filter.setParam(SearchFilter.SERVICE_NAME, "serviceName");
		RangerService rangerService = rangerService();

		XXService xService = xService();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		//PList<RangerPolicy> dbRangerPolicyList =
        serviceDBStore.getPaginatedServicePolicies(rangerService.getId(), filter);
	}

	@Test
	public void test38getPolicyVersionList() throws Exception {
		XXDataHistDao xDataHistDao = Mockito.mock(XXDataHistDao.class);
		List<Integer> versionList = new ArrayList<Integer>();
		versionList.add(1);
		versionList.add(2);
		Mockito.when(daoManager.getXXDataHist()).thenReturn(xDataHistDao);
		Mockito.when(xDataHistDao.getVersionListOfObject(Id, 1020)).thenReturn(
				versionList);

		VXString dbVXString = serviceDBStore.getPolicyVersionList(Id);
		Assert.assertNotNull(dbVXString);
		Mockito.verify(daoManager).getXXDataHist();
	}

	@Test
	public void test39getPolicyForVersionNumber() throws Exception {
		XXDataHistDao xDataHistDao = Mockito.mock(XXDataHistDao.class);
		XXDataHist xDataHist = Mockito.mock(XXDataHist.class);
		Mockito.when(daoManager.getXXDataHist()).thenReturn(xDataHistDao);
		Mockito.when(xDataHistDao.findObjectByVersionNumber(Id, 1020, 1))
				.thenReturn(xDataHist);
		RangerPolicy dbRangerPolicy = serviceDBStore.getPolicyForVersionNumber(
				Id, 1);
		Assert.assertNull(dbRangerPolicy);
		Mockito.verify(daoManager).getXXDataHist();
	}

	@Test
	public void test40getPoliciesByResourceSignature() throws Exception {
		List<RangerPolicy> rangerPolicyLists = new ArrayList<RangerPolicy>();
		RangerPolicy rangerPolicy = rangerPolicy();
		rangerPolicyLists.add(rangerPolicy);

		String serviceName = "HDFS_1";
		String policySignature = "Repo";
		Boolean isPolicyEnabled = true;

		RangerService rangerService = rangerService();
		List<XXPolicy> policiesList = new ArrayList<XXPolicy>();
		XXPolicy policy = new XXPolicy();
		policy.setAddedByUserId(Id);
		policy.setCreateTime(new Date());
		policy.setDescription("polcy test");
		policy.setGuid("");
		policy.setId(rangerService.getId());
		policy.setIsAuditEnabled(true);
		policy.setName("HDFS_1-1-20150316062453");
		policy.setService(rangerService.getId());
		policiesList.add(policy);

		XXPolicyDao xPolicyDao = Mockito.mock(XXPolicyDao.class);
		Mockito.when(daoManager.getXXPolicy()).thenReturn(xPolicyDao);
		Mockito.when(
				xPolicyDao.findByResourceSignatureByPolicyStatus(serviceName,
						policySignature, isPolicyEnabled)).thenReturn(
				policiesList);
		List<RangerPolicy> policyList = serviceDBStore
				.getPoliciesByResourceSignature(serviceName, policySignature,
						isPolicyEnabled);
		Assert.assertNotNull(policyList);
		Mockito.verify(daoManager).getXXPolicy();
	}

	@Test
	public void test41updateServiceCryptAlgo() throws Exception {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXUserDao xUserDao = Mockito.mock(XXUserDao.class);

		RangerService rangerService = rangerService();
		rangerService.getConfigs().put(ServiceDBStore.CONFIG_KEY_PASSWORD, "*****");

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);

		List<XXServiceConfigDef> xServiceConfigDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		xServiceConfigDefList.add(serviceConfigDefObj);
		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);

		Mockito.when(svcService.update(rangerService))
				.thenReturn(rangerService);
		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(Id)).thenReturn(xService);

		// the old pass
		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(ServiceDBStore.CONFIG_KEY_PASSWORD);
		//old outdated
		xConfMap.setConfigvalue("PBEWithSHA1AndDESede,ENCRYPT_KEY,SALTSALT,4,lXintlvY73rdk3jXvD7CqB5mcSKl0AMhouBbI5m3whrhLdbKddnzxA==");
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		xConfMapList.add(xConfMap);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
			xServiceConfigMapDao);
	Mockito.when(xServiceConfigMapDao.findByServiceId(Id)).thenReturn(
			xConfMapList);
	Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
			xServiceConfigMapDao);
	Mockito.when(xServiceConfigMapDao.remove(xConfMap)).thenReturn(true);

	Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
			xServiceConfigMapDao);
	Mockito.when(daoManager.getXXUser()).thenReturn(xUserDao);

	Mockito.when(
			rangerAuditFields.populateAuditFields(
					Mockito.isA(XXServiceConfigMap.class),
					Mockito.isA(XXService.class))).thenReturn(xConfMap);

	Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
			rangerService);

	XXServiceResourceDao xServiceResourceDao = Mockito.mock(XXServiceResourceDao.class);
	Mockito.when(daoManager.getXXServiceResource()).thenReturn(xServiceResourceDao);
	Mockito.when(xServiceResourceDao.countTaggedResourcesInServiceId(xService.getId())).thenReturn(0L);
	Map<String, Object> options = null;
	RangerService dbRangerService = serviceDBStore
			.updateService(rangerService, options);

	Assert.assertNotNull(dbRangerService);
	Assert.assertEquals(dbRangerService, rangerService);
	Assert.assertEquals(dbRangerService.getId(), rangerService.getId());
	Assert.assertEquals(dbRangerService.getName(), rangerService.getName());
	Assert.assertEquals(dbRangerService.getCreatedBy(),
			rangerService.getCreatedBy());
	Assert.assertEquals(dbRangerService.getDescription(),
			rangerService.getDescription());
	Assert.assertEquals(dbRangerService.getType(), rangerService.getType());
	Assert.assertEquals(dbRangerService.getVersion(),
			rangerService.getVersion());
	Mockito.verify(daoManager).getXXUser();
}

@Test
public void test41getMetricByTypeusergroup() throws Exception {
    VXGroupList vxGroupList = new VXGroupList();
    vxGroupList.setTotalCount(4l);
    vxGroupList.setPageSize(1);
    String         type       = "usergroup";
    VXUserList     vXUserList = new VXUserList();
    vXUserList.setTotalCount(4l);
    Mockito.when(xUserMgr.searchXGroups(Mockito.any(SearchCriteria.class))).thenReturn(vxGroupList);
    Mockito.when(xUserMgr.searchXUsers(Mockito.any(SearchCriteria.class))).thenReturn(vXUserList);
    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));

}

@Test
public void test42getMetricByTypeaudits() throws Exception {
    String type = "audits";

    Date date = new Date();
    date.setYear(2018);

    Mockito.when(restErrorUtil.parseDate(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(date);
    RangerServiceDefList svcDefList = new RangerServiceDefList();
    svcDefList.setTotalCount(10l);
    Mockito.when(serviceDefService.searchRangerServiceDefs(Mockito.any(SearchFilter.class))).thenReturn(svcDefList);

    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));

}

@Test
public void test43getMetricByTypeServices() throws Exception {
    String                type    = "services";
    RangerServiceList     svcList = new RangerServiceList();
    svcList.setTotalCount(10l);
    Mockito.when(svcService.searchRangerServices(Mockito.any(SearchFilter.class))).thenReturn(svcList);
    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));
}

@Test
public void test44getMetricByTypePolicies() throws Exception {
    String                type    = "policies";
    RangerServiceList     svcList = new RangerServiceList();
    svcList.setTotalCount(10l);
    Mockito.when(svcService.searchRangerServices(Mockito.any(SearchFilter.class))).thenReturn(svcList);
    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));
}

@Test
public void test45getMetricByTypeDatabase() throws Exception {
    String type = "database";
    Mockito.when(bizUtil.getDBVersion()).thenReturn("MYSQL");
    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));
}

@Test
public void test46getMetricByTypeContextenrichers() throws Exception {
    String                   type       = "contextenrichers";
    RangerServiceDefList     svcDefList = new RangerServiceDefList();
    svcDefList.setTotalCount(10l);
    Mockito.when(serviceDefService.searchRangerServiceDefs(Mockito.any(SearchFilter.class))).thenReturn(svcDefList);
    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));
}

@Test
public void test47getMetricByTypeDenyconditions() throws Exception {
    String                   type       = "denyconditions";
    RangerServiceDefList     svcDefList = new RangerServiceDefList();
    svcDefList.setTotalCount(10l);
    Mockito.when(serviceDefService.searchRangerServiceDefs(Mockito.any(SearchFilter.class))).thenReturn(svcDefList);
    serviceDBStore.getMetricByType(ServiceDBStore.METRIC_TYPE.getMetricTypeByName(type));
}

    @Test
    public void test48IsServiceAdminUserTrue() throws Exception{
    	String configName = "service.admin.users";
    	boolean result=false;
    	RangerService rService= rangerService();
    	XXServiceConfigMapDao xxServiceConfigMapDao = Mockito.mock(XXServiceConfigMapDao.class);
    	XXServiceConfigMap xxServiceConfigMap = new XXServiceConfigMap();
    	xxServiceConfigMap.setConfigkey(configName);
    	xxServiceConfigMap.setConfigvalue(rService.getConfigs().get(configName));

    	Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(xxServiceConfigMapDao);
    	Mockito.when(xxServiceConfigMapDao.findByServiceNameAndConfigKey(rService.getName(), configName)).thenReturn(xxServiceConfigMap);

    	result = serviceDBStore.isServiceAdminUser(rService.getName(),"testServiceAdminUser2");

    	Assert.assertTrue(result);
    	Mockito.verify(daoManager).getXXServiceConfigMap();
    	Mockito.verify(xxServiceConfigMapDao).findByServiceNameAndConfigKey(rService.getName(), configName);
    }

    @Test
    public void test49IsServiceAdminUserFalse() throws Exception{
    	String configName = "service.admin.users";
    	boolean result=false;
    	RangerService rService= rangerService();
    	XXServiceConfigMapDao xxServiceConfigMapDao = Mockito.mock(XXServiceConfigMapDao.class);
    	XXServiceConfigMap xxServiceConfigMap = new XXServiceConfigMap();
    	xxServiceConfigMap.setConfigkey(configName);
    	xxServiceConfigMap.setConfigvalue(rService.getConfigs().get(configName));

    	Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(xxServiceConfigMapDao);
    	Mockito.when(xxServiceConfigMapDao.findByServiceNameAndConfigKey(rService.getName(), configName)).thenReturn(xxServiceConfigMap);

    	result = serviceDBStore.isServiceAdminUser(rService.getName(),"testServiceAdminUser3");

    	Assert.assertFalse(result);
    	Mockito.verify(daoManager).getXXServiceConfigMap();
    	Mockito.verify(xxServiceConfigMapDao).findByServiceNameAndConfigKey(rService.getName(), configName);
    }
	
	@Test
	public void test41createKMSService() throws Exception {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXUserDao xUserDao = Mockito.mock(XXUserDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXUser xUser = Mockito.mock(XXUser.class);

		Mockito.when(xServiceDao.findByName("KMS_1")).thenReturn(
				xService);
		Mockito.when(!bizUtil.hasAccess(xService, null)).thenReturn(true);

		RangerService rangerService = rangerKMSService();
		VXUser vXUser = null;
		String userName = "servicemgr";

		List<XXServiceConfigDef> svcConfDefList = new ArrayList<XXServiceConfigDef>();
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();
		serviceConfigDefObj.setId(Id);
		serviceConfigDefObj.setType("7");
		svcConfDefList.add(serviceConfigDefObj);
		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);

		Mockito.when(svcService.create(rangerService)).thenReturn(rangerService);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getById(rangerService.getId())).thenReturn(
				xService);
		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);

		Mockito.when(stringUtil.getValidUserName(userName))
		.thenReturn(userName);
		Mockito.when(daoManager.getXXUser()).thenReturn(xUserDao);
		Mockito.when(xUserDao.findByUserName(userName)).thenReturn(xUser);

		Mockito.when(xUserService.populateViewBean(xUser)).thenReturn(vXUser);
		VXUser vXUserHdfs = new VXUser();
		vXUserHdfs.setName("hdfs");
		vXUserHdfs.setPassword("hdfs");
		VXUser vXUserHive = new VXUser();
		vXUserHive.setName("hive");
		vXUserHive.setPassword("hive");

		XXServiceConfigMap xConfMap = new XXServiceConfigMap();

		Mockito.when(svcService.getPopulatedViewObject(xService)).thenReturn(
				rangerService);

		Mockito.when(
				rangerAuditFields.populateAuditFields(
						Mockito.isA(XXServiceConfigMap.class),
						Mockito.isA(XXService.class))).thenReturn(xConfMap);

		List<XXAccessTypeDef> accessTypeDefList = new ArrayList<XXAccessTypeDef>();
		accessTypeDefList.add(rangerKmsAccessTypes("getmetadata", 7));
		accessTypeDefList.add(rangerKmsAccessTypes("generateeek", 8));
		accessTypeDefList.add(rangerKmsAccessTypes("decrypteek", 9));

		RangerServiceDef ran = new RangerServiceDef();
		ran.setName("KMS Test");

		ServiceDBStore spy = Mockito.spy(serviceDBStore);

		Mockito.when(spy.getServiceByName("KMS_1")).thenReturn(
				rangerService);
		Mockito.doNothing().when(spy).createDefaultPolicies(rangerService);

		RangerResourceDef resourceDef = new RangerResourceDef();
		resourceDef.setItemId(Id);
		resourceDef.setName("keyname");
		resourceDef.setType("string");
		resourceDef.setType("string");
		resourceDef.setLabel("Key Name");
		resourceDef.setDescription("Key Name");

		List<RangerResourceDef> resourceHierarchy = new ArrayList<RangerResourceDef>();
		resourceHierarchy.addAll(resourceHierarchy);

		spy.createService(rangerService);
		vXUser = new VXUser();
		vXUser.setName(userName);
		vXUser.setPassword(userName);
		
		spy.createDefaultPolicies(rangerService);

		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXService();
		Mockito.verify(daoManager).getXXServiceConfigMap();
	}

	@Test
	public void test50hasServiceConfigForPluginChanged() throws Exception {
		String pluginConfigKey = "ranger.plugin.testconfig";
		String otherConfigKey = "ranger.other.testconfig";
		Map<String, String> serviceConfigs = rangerService().getConfigs();
		List<XXServiceConfigMap> xConfMapList = new ArrayList<XXServiceConfigMap>();
		for (String key : serviceConfigs.keySet()) {
			XXServiceConfigMap xConfMap = new XXServiceConfigMap();
			xConfMap.setConfigkey(key);
			xConfMap.setConfigvalue(serviceConfigs.get(key));
			xConfMap.setServiceId(Id);
			xConfMapList.add(xConfMap);
		}

		Map<String, String> validConfig = new HashMap<String, String>();
		validConfig.putAll(serviceConfigs);
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(null, null));
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		validConfig.put(pluginConfigKey, "test value added");
		Assert.assertTrue(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setConfigkey(pluginConfigKey);
		xConfMap.setConfigvalue("test value added");
		xConfMap.setServiceId(Id);
		xConfMapList.add(xConfMap);
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		validConfig.put(pluginConfigKey, "test value changed");
		Assert.assertTrue(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		validConfig.remove(pluginConfigKey);
		Assert.assertTrue(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));
		int index = xConfMapList.size();
		xConfMap = xConfMapList.remove(index - 1);
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		validConfig.put(otherConfigKey, "other test value added");
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		xConfMap = new XXServiceConfigMap();
		xConfMap.setConfigkey(otherConfigKey);
		xConfMap.setConfigvalue("other test value added");
		xConfMap.setServiceId(Id);
		xConfMapList.add(xConfMap);
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		validConfig.put(otherConfigKey, "other test value changed");
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		validConfig.remove(otherConfigKey);
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));

		index = xConfMapList.size();
		xConfMap = xConfMapList.remove(index - 1);
		Assert.assertFalse(serviceDBStore.hasServiceConfigForPluginChanged(xConfMapList, validConfig));
	}
}
