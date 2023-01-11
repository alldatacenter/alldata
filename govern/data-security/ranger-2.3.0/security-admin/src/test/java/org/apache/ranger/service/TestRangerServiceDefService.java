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
package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.*;

import org.apache.ranger.entity.*;

import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
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
public class TestRangerServiceDefService {

	private static Long Id = 8L;

	@InjectMocks
	RangerServiceDefService serviceDefService = new RangerServiceDefService();

	@Mock
	RangerDaoManager daoManager;

	@Mock
	JSONUtil jsonUtil;

	@Mock
	RangerPolicyService policyService;

	@Mock
	StringUtil stringUtil;

	@Mock
	XUserService xUserService;

	@Mock
	XXServiceDefDao xServiceDefDao;

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

	@Test
	public void test1ValidateForCreate() {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		serviceDefService.validateForCreate(rangerServiceDef);
		Assert.assertNotNull(rangerServiceDef);
	}

	@Test
	public void test2ValidateForUpdate() {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		XXServiceDef serviceDef = serviceDef();
		serviceDefService.validateForUpdate(rangerServiceDef, serviceDef);

		Assert.assertNotNull(rangerServiceDef);
	}

	@Test
	public void test3PopulateViewBean() {

		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXAccessTypeDefDao xAccessTypeDefDao = Mockito
				.mock(XXAccessTypeDefDao.class);
		XXAccessTypeDefGrantsDao xxAccessTypeDefGrantsDao = Mockito.mock(XXAccessTypeDefGrantsDao.class);
		XXPolicyConditionDefDao xPolicyConditionDefDao = Mockito
				.mock(XXPolicyConditionDefDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXContextEnricherDefDao xContextEnricherDefDao = Mockito
				.mock(XXContextEnricherDefDao.class);
		XXEnumDefDao xEnumDefDao = Mockito.mock(XXEnumDefDao.class);
		XXEnumElementDefDao xEnumElementDefDao = Mockito
				.mock(XXEnumElementDefDao.class);

		XXServiceDef serviceDef = serviceDef();

		String name = "fdfdfds";
		XXPortalUser tUser = new XXPortalUser();
		tUser.setAddedByUserId(Id);
		tUser.setCreateTime(new Date());
		tUser.setEmailAddress("test@gmail.com");
		tUser.setFirstName(name);
		tUser.setId(Id);
		tUser.setLastName(name);

		List<XXResourceDef> resDefList = new ArrayList<XXResourceDef>();
		XXResourceDef resourceDef = new XXResourceDef();
		resourceDef.setAddedByUserId(Id);
		resourceDef.setCreateTime(new Date());
		resourceDef.setDefid(Id);
		resourceDef.setDescription("test");
		resourceDef.setId(Id);
		resDefList.add(resourceDef);

		List<XXPolicyItem> xPolicyItemList = new ArrayList<XXPolicyItem>();
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
		xPolicyItemList.add(xPolicyItem);

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

		List<XXContextEnricherDef> contextEnrichersList = new ArrayList<XXContextEnricherDef>();
		XXContextEnricherDef contextEnricherDefObj = new XXContextEnricherDef();
		contextEnricherDefObj.setAddedByUserId(Id);
		contextEnricherDefObj.setCreateTime(new Date());
		contextEnricherDefObj.setDefid(Id);
		contextEnricherDefObj.setEnricher("RangerCountryProvider");
		contextEnricherDefObj.setName("country-provider");
		contextEnricherDefObj.setId(Id);
		contextEnricherDefObj.setOrder(0);
		contextEnricherDefObj.setUpdatedByUserId(Id);
		contextEnricherDefObj.setUpdateTime(new Date());
		contextEnrichersList.add(contextEnricherDefObj);

		List<XXEnumDef> xEnumList = new ArrayList<XXEnumDef>();
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(null);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName(name);
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());
		xEnumList.add(enumDefObj);

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

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(Id)).thenReturn(tUser);

		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);
		Mockito.when(
				xServiceConfigDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(serviceConfigDefList);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);

		Mockito.when(daoManager.getXXAccessTypeDef()).thenReturn(
				xAccessTypeDefDao);
		Mockito.when(xxAccessTypeDefGrantsDao.findImpliedGrantsByServiceDefId(Mockito.anyLong())).thenReturn(Collections.emptyMap());
		Mockito.when(daoManager.getXXAccessTypeDefGrants()).thenReturn(xxAccessTypeDefGrantsDao);

		Mockito.when(daoManager.getXXPolicyConditionDef()).thenReturn(
				xPolicyConditionDefDao);

		Mockito.when(daoManager.getXXContextEnricherDef()).thenReturn(
				xContextEnricherDefDao);
		Mockito.when(
				xContextEnricherDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(contextEnrichersList);

		Mockito.when(daoManager.getXXEnumDef()).thenReturn(xEnumDefDao);
		Mockito.when(xEnumDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(xEnumList);

		Mockito.when(daoManager.getXXEnumElementDef()).thenReturn(
				xEnumElementDefDao);
		Mockito.when(
				xEnumElementDefDao.findByEnumDefId(enumElementDefObj.getId()))
				.thenReturn(xElementsList);

		XXDataMaskTypeDefDao    xDataMaskTypeDao  = Mockito.mock(XXDataMaskTypeDefDao.class);
		List<XXDataMaskTypeDef> xDataMaskTypeDefs = new ArrayList<XXDataMaskTypeDef>();
		Mockito.when(daoManager.getXXDataMaskTypeDef()).thenReturn(xDataMaskTypeDao);
		Mockito.when(xDataMaskTypeDao.findByServiceDefId(serviceDef.getId())).thenReturn(xDataMaskTypeDefs);

		RangerServiceDef dbRangerServiceDef = serviceDefService
				.populateViewBean(serviceDef);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getId(), serviceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getName(), serviceDef.getName());
		Assert.assertEquals(dbRangerServiceDef.getDescription(),
				serviceDef.getDescription());
		Assert.assertEquals(dbRangerServiceDef.getGuid(), serviceDef.getGuid());
		Assert.assertEquals(dbRangerServiceDef.getVersion(),
				serviceDef.getVersion());
		Mockito.verify(daoManager).getXXServiceConfigDef();
		Mockito.verify(daoManager).getXXResourceDef();
		Mockito.verify(daoManager).getXXAccessTypeDef();
		Mockito.verify(daoManager).getXXPolicyConditionDef();
		Mockito.verify(daoManager).getXXContextEnricherDef();
		Mockito.verify(daoManager).getXXEnumDef();
		Mockito.verify(daoManager).getXXEnumElementDef();
	}

	@Test
	public void test4getAllServiceDefs() {
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXAccessTypeDefDao xAccessTypeDefDao = Mockito
				.mock(XXAccessTypeDefDao.class);
		XXAccessTypeDefGrantsDao xxAccessTypeDefGrantsDao = Mockito.mock(XXAccessTypeDefGrantsDao.class);
		XXPolicyConditionDefDao xPolicyConditionDefDao = Mockito
				.mock(XXPolicyConditionDefDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);
		XXContextEnricherDefDao xContextEnricherDefDao = Mockito
				.mock(XXContextEnricherDefDao.class);
		XXEnumDefDao xEnumDefDao = Mockito.mock(XXEnumDefDao.class);
		XXEnumElementDefDao xEnumElementDefDao = Mockito
				.mock(XXEnumElementDefDao.class);

		List<XXServiceDef> xServiceDefList = new ArrayList<XXServiceDef>();
		XXServiceDef serviceDef = new XXServiceDef();
		serviceDef.setAddedByUserId(Id);
		serviceDef.setCreateTime(new Date());
		serviceDef.setDescription("HDFS Repository");
		serviceDef.setGuid("1427365526516_835_0");
		serviceDef.setId(Id);
		serviceDef.setUpdateTime(new Date());
		serviceDef.setUpdatedByUserId(Id);
		serviceDef.setImplclassname("RangerServiceHdfs");
		serviceDef.setLabel("HDFS Repository");
		serviceDef.setRbkeylabel(null);
		serviceDef.setRbkeydescription(null);
		serviceDef.setIsEnabled(true);
		xServiceDefList.add(serviceDef);

		String name = "fdfdfds";
		XXPortalUser tUser = new XXPortalUser();
		tUser.setAddedByUserId(Id);
		tUser.setCreateTime(new Date());
		tUser.setEmailAddress("test@gmail.com");
		tUser.setFirstName(name);
		tUser.setId(Id);
		tUser.setLastName(name);

		List<XXResourceDef> resDefList = new ArrayList<XXResourceDef>();
		XXResourceDef resourceDef = new XXResourceDef();
		resourceDef.setAddedByUserId(Id);
		resourceDef.setCreateTime(new Date());
		resourceDef.setDefid(Id);
		resourceDef.setDescription("test");
		resourceDef.setId(Id);
		resDefList.add(resourceDef);

		List<XXPolicyItem> xPolicyItemList = new ArrayList<XXPolicyItem>();
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
		xPolicyItemList.add(xPolicyItem);

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

		List<XXContextEnricherDef> contextEnrichersList = new ArrayList<XXContextEnricherDef>();
		XXContextEnricherDef contextEnricherDefObj = new XXContextEnricherDef();
		contextEnricherDefObj.setAddedByUserId(Id);
		contextEnricherDefObj.setCreateTime(new Date());
		contextEnricherDefObj.setDefid(Id);
		contextEnricherDefObj.setEnricher("RangerCountryProvider");
		contextEnricherDefObj.setName("country-provider");
		contextEnricherDefObj.setId(Id);
		contextEnricherDefObj.setOrder(0);
		contextEnricherDefObj.setUpdatedByUserId(Id);
		contextEnricherDefObj.setUpdateTime(new Date());
		contextEnrichersList.add(contextEnricherDefObj);

		List<XXEnumDef> xEnumList = new ArrayList<XXEnumDef>();
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(null);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName(name);
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());
		xEnumList.add(enumDefObj);

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

		Mockito.when(xServiceDefDao.getAll()).thenReturn(xServiceDefList);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(Id)).thenReturn(tUser);

		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);
		Mockito.when(
				xServiceConfigDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(serviceConfigDefList);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);

		Mockito.when(daoManager.getXXAccessTypeDef()).thenReturn(
				xAccessTypeDefDao);
		Mockito.when(xxAccessTypeDefGrantsDao.findImpliedGrantsByServiceDefId(Mockito.anyLong())).thenReturn(Collections.emptyMap());
		Mockito.when(daoManager.getXXAccessTypeDefGrants()).thenReturn(xxAccessTypeDefGrantsDao);

		Mockito.when(daoManager.getXXPolicyConditionDef()).thenReturn(
				xPolicyConditionDefDao);

		Mockito.when(daoManager.getXXContextEnricherDef()).thenReturn(
				xContextEnricherDefDao);
		Mockito.when(
				xContextEnricherDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(contextEnrichersList);

		Mockito.when(daoManager.getXXEnumDef()).thenReturn(xEnumDefDao);
		Mockito.when(xEnumDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(xEnumList);

		Mockito.when(daoManager.getXXEnumElementDef()).thenReturn(
				xEnumElementDefDao);
		Mockito.when(
				xEnumElementDefDao.findByEnumDefId(enumElementDefObj.getId()))
				.thenReturn(xElementsList);

		XXDataMaskTypeDefDao    xDataMaskTypeDao  = Mockito.mock(XXDataMaskTypeDefDao.class);
		List<XXDataMaskTypeDef> xDataMaskTypeDefs = new ArrayList<XXDataMaskTypeDef>();
		Mockito.when(daoManager.getXXDataMaskTypeDef()).thenReturn(xDataMaskTypeDao);
		Mockito.when(xDataMaskTypeDao.findByServiceDefId(serviceDef.getId())).thenReturn(xDataMaskTypeDefs);

		List<RangerServiceDef> dbRangerServiceDef = serviceDefService
				.getAllServiceDefs();
		Assert.assertNotNull(dbRangerServiceDef);
		Mockito.verify(daoManager).getXXResourceDef();
		Mockito.verify(daoManager).getXXAccessTypeDef();
		Mockito.verify(daoManager).getXXPolicyConditionDef();
		Mockito.verify(daoManager).getXXContextEnricherDef();
		Mockito.verify(daoManager).getXXEnumDef();
		Mockito.verify(daoManager).getXXEnumElementDef();
	}

	@Test
	public void test5getPopulatedViewObject() {
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXServiceConfigDefDao xServiceConfigDefDao = Mockito
				.mock(XXServiceConfigDefDao.class);

		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXAccessTypeDefDao xAccessTypeDefDao = Mockito
				.mock(XXAccessTypeDefDao.class);
		XXAccessTypeDefGrantsDao xxAccessTypeDefGrantsDao = Mockito.mock(XXAccessTypeDefGrantsDao.class);
		XXPolicyConditionDefDao xPolicyConditionDefDao = Mockito
				.mock(XXPolicyConditionDefDao.class);
		XXContextEnricherDefDao xContextEnricherDefDao = Mockito
				.mock(XXContextEnricherDefDao.class);
		XXEnumDefDao xEnumDefDao = Mockito.mock(XXEnumDefDao.class);
		XXEnumElementDefDao xEnumElementDefDao = Mockito
				.mock(XXEnumElementDefDao.class);

		XXServiceDef serviceDef = serviceDef();
		String name = "fdfdfds";
		XXPortalUser tUser = new XXPortalUser();
		tUser.setAddedByUserId(Id);
		tUser.setCreateTime(new Date());
		tUser.setEmailAddress("test@gmail.com");
		tUser.setFirstName(name);
		tUser.setId(Id);
		tUser.setLastName(name);

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

		List<XXResourceDef> resDefList = new ArrayList<XXResourceDef>();
		XXResourceDef resourceDef = new XXResourceDef();
		resourceDef.setAddedByUserId(Id);
		resourceDef.setCreateTime(new Date());
		resourceDef.setDefid(Id);
		resourceDef.setDescription("test");
		resourceDef.setId(Id);
		resDefList.add(resourceDef);

		List<XXPolicyItem> xPolicyItemList = new ArrayList<XXPolicyItem>();
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
		xPolicyItemList.add(xPolicyItem);

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

		List<XXContextEnricherDef> contextEnrichersList = new ArrayList<XXContextEnricherDef>();
		XXContextEnricherDef contextEnricherDefObj = new XXContextEnricherDef();
		contextEnricherDefObj.setAddedByUserId(Id);
		contextEnricherDefObj.setCreateTime(new Date());
		contextEnricherDefObj.setDefid(Id);
		contextEnricherDefObj.setEnricher("RangerCountryProvider");
		contextEnricherDefObj.setName("country-provider");
		contextEnricherDefObj.setId(Id);
		contextEnricherDefObj.setOrder(0);
		contextEnricherDefObj.setUpdatedByUserId(Id);
		contextEnricherDefObj.setUpdateTime(new Date());
		contextEnrichersList.add(contextEnricherDefObj);

		List<XXEnumDef> xEnumList = new ArrayList<XXEnumDef>();
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(null);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName(name);
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());
		xEnumList.add(enumDefObj);

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

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(Id)).thenReturn(tUser);

		Mockito.when(daoManager.getXXServiceConfigDef()).thenReturn(
				xServiceConfigDefDao);
		Mockito.when(
				xServiceConfigDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(serviceConfigDefList);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);

		Mockito.when(daoManager.getXXAccessTypeDef()).thenReturn(
				xAccessTypeDefDao);
		Mockito.when(xxAccessTypeDefGrantsDao.findImpliedGrantsByServiceDefId(Mockito.anyLong())).thenReturn(Collections.emptyMap());
		Mockito.when(daoManager.getXXAccessTypeDefGrants()).thenReturn(xxAccessTypeDefGrantsDao);

		Mockito.when(daoManager.getXXPolicyConditionDef()).thenReturn(
				xPolicyConditionDefDao);

		Mockito.when(daoManager.getXXContextEnricherDef()).thenReturn(
				xContextEnricherDefDao);
		Mockito.when(
				xContextEnricherDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(contextEnrichersList);

		Mockito.when(daoManager.getXXEnumDef()).thenReturn(xEnumDefDao);
		Mockito.when(xEnumDefDao.findByServiceDefId(serviceDef.getId()))
				.thenReturn(xEnumList);

		Mockito.when(daoManager.getXXEnumElementDef()).thenReturn(
				xEnumElementDefDao);
		Mockito.when(
				xEnumElementDefDao.findByEnumDefId(enumElementDefObj.getId()))
				.thenReturn(xElementsList);

		XXDataMaskTypeDefDao    xDataMaskTypeDao  = Mockito.mock(XXDataMaskTypeDefDao.class);
		List<XXDataMaskTypeDef> xDataMaskTypeDefs = new ArrayList<XXDataMaskTypeDef>();
		Mockito.when(daoManager.getXXDataMaskTypeDef()).thenReturn(xDataMaskTypeDao);
		Mockito.when(xDataMaskTypeDao.findByServiceDefId(serviceDef.getId())).thenReturn(xDataMaskTypeDefs);

		RangerServiceDef dbRangerServiceDef = serviceDefService
				.getPopulatedViewObject(serviceDef);
		Assert.assertNotNull(dbRangerServiceDef);
		Mockito.verify(daoManager).getXXServiceConfigDef();
		Mockito.verify(daoManager).getXXResourceDef();
		Mockito.verify(daoManager).getXXAccessTypeDef();
		Mockito.verify(daoManager).getXXPolicyConditionDef();
		Mockito.verify(daoManager).getXXContextEnricherDef();
		Mockito.verify(daoManager).getXXEnumDef();
	}

}
