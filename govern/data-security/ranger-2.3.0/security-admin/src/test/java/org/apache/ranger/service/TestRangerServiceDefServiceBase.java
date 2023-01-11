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

import javax.ws.rs.WebApplicationException;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXEnumElementDefDao;
import org.apache.ranger.db.XXResourceDefDao;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXContextEnricherDef;
import org.apache.ranger.entity.XXEnumDef;
import org.apache.ranger.entity.XXEnumElementDef;
import org.apache.ranger.entity.XXPolicyConditionDef;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXServiceConfigDef;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumElementDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.view.RangerServiceDefList;
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
public class TestRangerServiceDefServiceBase {

	private static Long Id = 8L;

	@InjectMocks
	RangerServiceDefService rangerServiceDefService = new RangerServiceDefService();

	@Mock
	RangerDaoManager daoManager;

	@Mock
	RESTErrorUtil restErrorUtil;

	@Mock
	ContextUtil contextUtil;

	@Mock
	RangerAuditFields rangerAuditFields;

	@Mock
	RangerBizUtil rangerBizUtil;

	@Mock
	RangerSearchUtil searchUtil;
	
	@Mock
	GUIDUtil guidUtil;
	
	@Mock
	JSONUtil jsonUtil;

	@Mock
	BaseDao<XXServiceDef> baseDao;

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
		xServiceDef.setGuid("0d047247-bafe-4cf8-8e9b-d5d377284b2d");
		xServiceDef.setId(Id);
		xServiceDef.setImplclassname("RangerServiceHdfs");
		xServiceDef.setIsEnabled(true);
		xServiceDef.setLabel("HDFS Repository");
		xServiceDef.setName("hdfs");
		xServiceDef.setRbkeydescription(null);
		xServiceDef.setRbkeylabel(null);
		xServiceDef.setUpdatedByUserId(Id);
		xServiceDef.setUpdateTime(new Date());

		return xServiceDef;
	}

	@Test
	public void test1MapViewToEntityBean() {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		XXServiceDef serviceDef = serviceDef();
		int operationContext = 1;

		XXServiceDef dbServiceDef = rangerServiceDefService
				.mapViewToEntityBean(rangerServiceDef, serviceDef,
						operationContext);
		Assert.assertNotNull(dbServiceDef);
		Assert.assertEquals(dbServiceDef, serviceDef);
		Assert.assertEquals(dbServiceDef.getDescription(),
				serviceDef.getDescription());
		Assert.assertEquals(dbServiceDef.getGuid(), serviceDef.getGuid());
		Assert.assertEquals(dbServiceDef.getName(), serviceDef.getName());
		Assert.assertEquals(dbServiceDef.getAddedByUserId(),
				serviceDef.getAddedByUserId());
		Assert.assertEquals(dbServiceDef.getId(), serviceDef.getId());
		Assert.assertEquals(dbServiceDef.getVersion(), serviceDef.getVersion());
		Assert.assertEquals(dbServiceDef.getImplclassname(),
				serviceDef.getImplclassname());
		Assert.assertEquals(dbServiceDef.getUpdatedByUserId(),
				serviceDef.getUpdatedByUserId());
	}

	@Test
	public void test2MapEntityToViewBean() {
		RangerServiceDef rangerServiceDef = rangerServiceDef();
		XXServiceDef serviceDef = serviceDef();

		RangerServiceDef dbRangerServiceDef = rangerServiceDefService
				.mapEntityToViewBean(rangerServiceDef, serviceDef);
		Assert.assertNotNull(dbRangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef, rangerServiceDef);
		Assert.assertEquals(dbRangerServiceDef.getDescription(),
				rangerServiceDef.getDescription());
		Assert.assertEquals(dbRangerServiceDef.getGuid(),
				rangerServiceDef.getGuid());
		Assert.assertEquals(dbRangerServiceDef.getName(),
				rangerServiceDef.getName());
		Assert.assertEquals(dbRangerServiceDef.getId(),
				rangerServiceDef.getId());
		Assert.assertEquals(dbRangerServiceDef.getVersion(),
				rangerServiceDef.getVersion());

	}

	@Test
	public void test3populateRangerServiceConfigDefToXX() {
		RangerServiceConfigDef serviceConfigDefObj = new RangerServiceConfigDef();
		XXServiceConfigDef configDefObj = new XXServiceConfigDef();
		XXServiceDef serviceDefObj = new XXServiceDef();

		Mockito.when(
				(XXServiceConfigDef) rangerAuditFields.populateAuditFields(
						configDefObj, serviceDefObj)).thenReturn(configDefObj);

		XXServiceConfigDef dbServiceConfigDef = rangerServiceDefService
				.populateRangerServiceConfigDefToXX(serviceConfigDefObj,
						configDefObj, serviceDefObj, 1);
		Assert.assertNotNull(dbServiceConfigDef);

	}

	@Test
	public void test4populateXXToRangerServiceConfigDef() {
		XXServiceConfigDef serviceConfigDefObj = new XXServiceConfigDef();

		RangerServiceConfigDef dbserviceConfigDefObj = rangerServiceDefService
				.populateXXToRangerServiceConfigDef(serviceConfigDefObj);
		Assert.assertNotNull(dbserviceConfigDefObj);
	}

	@Test
	public void test5populateRangerResourceDefToXX() {
		RangerResourceDef rangerResourceDefObj = new RangerResourceDef();
		rangerResourceDefObj.setDescription("HDFS Repository");
		rangerResourceDefObj.setExcludesSupported(false);
		rangerResourceDefObj.setLabel("HDFS Repository");
		rangerResourceDefObj.setName("HDFs");

		XXResourceDef resourceDefObj = new XXResourceDef();
		resourceDefObj.setAddedByUserId(Id);
		resourceDefObj.setCreateTime(new Date());
		resourceDefObj.setDefid(Id);
		resourceDefObj.setDescription("HDFS Repository");
		resourceDefObj.setId(Id);

		XXServiceDef serviceDefObj = new XXServiceDef();
		serviceDefObj.setAddedByUserId(Id);
		serviceDefObj.setCreateTime(new Date());
		serviceDefObj.setDescription("HDFS Repository");
		serviceDefObj.setGuid("1427365526516_835_0");
		serviceDefObj.setId(Id);

		Mockito.when(
				(XXResourceDef) rangerAuditFields.populateAuditFields(
						resourceDefObj, serviceDefObj)).thenReturn(
				resourceDefObj);

		XXResourceDef dbResourceDef = rangerServiceDefService
				.populateRangerResourceDefToXX(rangerResourceDefObj,
						resourceDefObj, serviceDefObj, 1);
		Assert.assertNotNull(dbResourceDef);
		Assert.assertEquals(dbResourceDef, resourceDefObj);
		Assert.assertEquals(dbResourceDef.getId(), resourceDefObj.getId());
		Assert.assertEquals(dbResourceDef.getLabel(), resourceDefObj.getLabel());
		Assert.assertEquals(dbResourceDef.getName(), resourceDefObj.getName());
		Assert.assertEquals(dbResourceDef.getDescription(),
				resourceDefObj.getDescription());

	}

	@Test
	public void test6populateXXToRangerResourceDef() {
		XXResourceDefDao xResourceDefDao = Mockito.mock(XXResourceDefDao.class);
		XXResourceDef resourceDefObj = new XXResourceDef();
		resourceDefObj.setAddedByUserId(Id);
		resourceDefObj.setCreateTime(new Date());
		resourceDefObj.setDefid(Id);
		resourceDefObj.setDescription("HDFS Repository");
		resourceDefObj.setId(Id);

		Mockito.when(daoManager.getXXResourceDef()).thenReturn(xResourceDefDao);

		RangerResourceDef dbRangerResourceDef = rangerServiceDefService
				.populateXXToRangerResourceDef(resourceDefObj);
		Assert.assertNotNull(dbRangerResourceDef);
		Assert.assertEquals(dbRangerResourceDef.getName(),
				resourceDefObj.getName());
		Assert.assertEquals(dbRangerResourceDef.getDescription(),
				resourceDefObj.getDescription());
		Assert.assertEquals(dbRangerResourceDef.getType(),
				resourceDefObj.getType());
		Assert.assertEquals(dbRangerResourceDef.getRbKeyDescription(),
				resourceDefObj.getRbkeydescription());
		Mockito.verify(daoManager).getXXResourceDef();
	}

	@Test
	public void test7populateRangerAccessTypeDefToXX() {
		RangerAccessTypeDef rangerAccessTypeDefObj = new RangerAccessTypeDef();
		rangerAccessTypeDefObj.setLabel("Read");
		rangerAccessTypeDefObj.setName("read");
		rangerAccessTypeDefObj.setRbKeyLabel(null);
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
		XXServiceDef serviceDefObj = new XXServiceDef();
		serviceDefObj.setAddedByUserId(Id);
		serviceDefObj.setCreateTime(new Date());
		serviceDefObj.setDescription("HDFS Repository");
		serviceDefObj.setGuid("1427365526516_835_0");
		serviceDefObj.setId(Id);

		Mockito.when(
				(XXAccessTypeDef) rangerAuditFields.populateAuditFields(
						accessTypeDefObj, serviceDefObj)).thenReturn(
				accessTypeDefObj);

		XXAccessTypeDef dbAccessTypeDef = rangerServiceDefService
				.populateRangerAccessTypeDefToXX(rangerAccessTypeDefObj,
						accessTypeDefObj, serviceDefObj, 1);
		Assert.assertNotNull(dbAccessTypeDef);
		Assert.assertEquals(dbAccessTypeDef, accessTypeDefObj);
		Assert.assertEquals(dbAccessTypeDef.getName(),
				accessTypeDefObj.getName());
		Assert.assertEquals(dbAccessTypeDef.getLabel(),
				accessTypeDefObj.getLabel());
		Assert.assertEquals(dbAccessTypeDef.getRbkeylabel(),
				accessTypeDefObj.getRbkeylabel());
		Assert.assertEquals(dbAccessTypeDef.getDefid(),
				accessTypeDefObj.getDefid());
		Assert.assertEquals(dbAccessTypeDef.getId(), accessTypeDefObj.getId());
		Assert.assertEquals(dbAccessTypeDef.getCreateTime(),
				accessTypeDefObj.getCreateTime());
		Assert.assertEquals(dbAccessTypeDef.getOrder(),
				accessTypeDefObj.getOrder());

	}

	@Test
	public void test8populateRangerAccessTypeDefToXXNullValue() {
		RangerAccessTypeDef rangerAccessTypeDefObj = null;
		XXAccessTypeDef accessTypeDefObj = null;
		XXServiceDef serviceDefObj = null;
		Mockito.when(
				restErrorUtil.createRESTException(
						"RangerServiceDef cannot be null.",
						MessageEnums.DATA_NOT_FOUND)).thenThrow(
				new WebApplicationException());

		thrown.expect(WebApplicationException.class);
		XXAccessTypeDef dbAccessTypeDef = rangerServiceDefService
				.populateRangerAccessTypeDefToXX(rangerAccessTypeDefObj,
						accessTypeDefObj, serviceDefObj, 1);
		Assert.assertNull(dbAccessTypeDef);
	}

	@Test
	public void test9populateXXToRangerAccessTypeDef() {

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

		RangerAccessTypeDef dbRangerAccessTypeDef = rangerServiceDefService
				.populateXXToRangerAccessTypeDef(accessTypeDefObj, Collections.emptyList());
		Assert.assertNotNull(dbRangerAccessTypeDef);
		Assert.assertEquals(dbRangerAccessTypeDef.getName(),
				accessTypeDefObj.getName());
		Assert.assertEquals(dbRangerAccessTypeDef.getLabel(),
				accessTypeDefObj.getLabel());
		Assert.assertEquals(dbRangerAccessTypeDef.getRbKeyLabel(),
				accessTypeDefObj.getRbkeylabel());
	}

	@Test
	public void test10populateRangerPolicyConditionDefToXX() {
		RangerPolicyConditionDef rangerConditionDefvObj = new RangerPolicyConditionDef();
		rangerConditionDefvObj.setDescription("Countries");
		rangerConditionDefvObj.setEvaluator("COUNTRY");
		rangerConditionDefvObj.setLabel("Countries");
		rangerConditionDefvObj.setName("country");
		rangerConditionDefvObj.setRbKeyDescription(null);
		rangerConditionDefvObj.setRbKeyLabel(null);
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
		XXServiceDef serviceDefObj = new XXServiceDef();
		serviceDefObj.setAddedByUserId(Id);
		serviceDefObj.setCreateTime(new Date());
		serviceDefObj.setDescription("HDFS Repository");
		serviceDefObj.setGuid("1427365526516_835_0");
		serviceDefObj.setId(Id);
		Mockito.when(
				(XXPolicyConditionDef) rangerAuditFields.populateAuditFields(
						policyConditionDefObj, serviceDefObj)).thenReturn(
				policyConditionDefObj);
		XXPolicyConditionDef dbPolicyConditionDef = rangerServiceDefService
				.populateRangerPolicyConditionDefToXX(rangerConditionDefvObj,
						policyConditionDefObj, serviceDefObj, 1);
		Assert.assertNotNull(dbPolicyConditionDef);
		Assert.assertEquals(dbPolicyConditionDef.getName(),
				policyConditionDefObj.getName());
		Assert.assertEquals(dbPolicyConditionDef.getDescription(),
				policyConditionDefObj.getDescription());
		Assert.assertEquals(dbPolicyConditionDef.getEvaluator(),
				policyConditionDefObj.getEvaluator());
		Assert.assertEquals(dbPolicyConditionDef.getLabel(),
				policyConditionDefObj.getLabel());
		Assert.assertEquals(dbPolicyConditionDef.getId(),
				policyConditionDefObj.getId());
		Assert.assertEquals(dbPolicyConditionDef.getRbkeydescription(),
				policyConditionDefObj.getRbkeydescription());
		Assert.assertEquals(dbPolicyConditionDef.getOrder(),
				policyConditionDefObj.getOrder());
		Assert.assertEquals(dbPolicyConditionDef.getUpdatedByUserId(),
				policyConditionDefObj.getUpdatedByUserId());
		Assert.assertEquals(dbPolicyConditionDef.getUpdateTime(),
				policyConditionDefObj.getUpdateTime());

	}

	@Test
	public void test11populateRangerPolicyConditionDefToXXnullValue() {
		RangerPolicyConditionDef rangerConditionDefvObj = null;
		XXPolicyConditionDef policyConditionDefObj = null;
		XXServiceDef serviceDefObj = null;

		Mockito.when(
				restErrorUtil.createRESTException(
						"RangerServiceDef cannot be null.",
						MessageEnums.DATA_NOT_FOUND)).thenThrow(
				new WebApplicationException());

		thrown.expect(WebApplicationException.class);

		XXPolicyConditionDef dbPolicyConditionDef = rangerServiceDefService
				.populateRangerPolicyConditionDefToXX(rangerConditionDefvObj,
						policyConditionDefObj, serviceDefObj, 1);
		Assert.assertNull(dbPolicyConditionDef);
	}

	@Test
	public void test12populateXXToRangerPolicyConditionDef() {
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

		RangerPolicyConditionDef dbRangerPolicyConditionDef = rangerServiceDefService
				.populateXXToRangerPolicyConditionDef(policyConditionDefObj);
		Assert.assertNotNull(dbRangerPolicyConditionDef);
		Assert.assertEquals(dbRangerPolicyConditionDef.getName(),
				policyConditionDefObj.getName());
		Assert.assertEquals(dbRangerPolicyConditionDef.getDescription(),
				policyConditionDefObj.getDescription());
		Assert.assertEquals(dbRangerPolicyConditionDef.getEvaluator(),
				policyConditionDefObj.getEvaluator());
		Assert.assertEquals(dbRangerPolicyConditionDef.getLabel(),
				policyConditionDefObj.getLabel());
	}

	@Test
	public void test13populateRangerContextEnricherDefToXX() {
		RangerContextEnricherDef rangerContextEnricherDefObj = new RangerContextEnricherDef();
		rangerContextEnricherDefObj.setName("country-provider");
		rangerContextEnricherDefObj.setEnricher("RangerCountryProvider");

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
		XXServiceDef serviceDefObj = new XXServiceDef();
		serviceDefObj.setAddedByUserId(Id);
		serviceDefObj.setCreateTime(new Date());
		serviceDefObj.setDescription("HDFS Repository");
		serviceDefObj.setGuid("1427365526516_835_0");
		serviceDefObj.setId(Id);

		Mockito.when(
				(XXContextEnricherDef) rangerAuditFields.populateAuditFields(
						contextEnricherDefObj, serviceDefObj)).thenReturn(
				contextEnricherDefObj);

		XXContextEnricherDef dbContextEnricherDef = rangerServiceDefService
				.populateRangerContextEnricherDefToXX(
						rangerContextEnricherDefObj, contextEnricherDefObj,
						serviceDefObj, 1);
		Assert.assertNotNull(dbContextEnricherDef);
		Assert.assertEquals(dbContextEnricherDef.getEnricher(),
				contextEnricherDefObj.getEnricher());
		Assert.assertEquals(dbContextEnricherDef.getEnricherOptions(),
				contextEnricherDefObj.getEnricherOptions());
		Assert.assertEquals(dbContextEnricherDef.getName(),
				contextEnricherDefObj.getName());
		Assert.assertEquals(dbContextEnricherDef.getCreateTime(),
				contextEnricherDefObj.getCreateTime());
		Assert.assertEquals(dbContextEnricherDef.getId(),
				contextEnricherDefObj.getId());
		Assert.assertEquals(dbContextEnricherDef.getOrder(),
				contextEnricherDefObj.getOrder());
		Assert.assertEquals(dbContextEnricherDef.getUpdatedByUserId(),
				contextEnricherDefObj.getUpdatedByUserId());
		Assert.assertEquals(dbContextEnricherDef.getUpdateTime(),
				contextEnricherDefObj.getUpdateTime());

	}

	@Test
	public void test14populateRangerContextEnricherDefToXXnullValue() {
		RangerContextEnricherDef rangerContextEnricherDefObj = null;
		XXContextEnricherDef contextEnricherDefObj = null;
		XXServiceDef serviceDefObj = null;

		Mockito.when(
				restErrorUtil.createRESTException(
						"RangerServiceDef cannot be null.",
						MessageEnums.DATA_NOT_FOUND)).thenThrow(
				new WebApplicationException());

		thrown.expect(WebApplicationException.class);

		XXContextEnricherDef dbContextEnricherDef = rangerServiceDefService
				.populateRangerContextEnricherDefToXX(
						rangerContextEnricherDefObj, contextEnricherDefObj,
						serviceDefObj, 1);
		Assert.assertNull(dbContextEnricherDef);

	}

	@Test
	public void test15populateXXToRangerContextEnricherDef() {
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

		RangerContextEnricherDef dbRangerContextEnricherDef = rangerServiceDefService
				.populateXXToRangerContextEnricherDef(contextEnricherDefObj);
		Assert.assertNotNull(dbRangerContextEnricherDef);
		Assert.assertEquals(dbRangerContextEnricherDef.getEnricher(),
				contextEnricherDefObj.getEnricher());
		Assert.assertEquals(dbRangerContextEnricherDef.getName(),
				contextEnricherDefObj.getName());

	}

	@Test
	public void test16populateRangerEnumDefToXX() {
		RangerEnumDef rangerEnumDefObj = new RangerEnumDef();
		rangerEnumDefObj.setName("authnType");
		rangerEnumDefObj.setDefaultIndex(0);
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(0);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName("authnType");
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());
		XXServiceDef serviceDefObj = new XXServiceDef();
		serviceDefObj.setAddedByUserId(Id);
		serviceDefObj.setCreateTime(new Date());
		serviceDefObj.setDescription("HDFS Repository");
		serviceDefObj.setGuid("1427365526516_835_0");
		serviceDefObj.setId(Id);

		Mockito.when(
				(XXEnumDef) rangerAuditFields.populateAuditFields(enumDefObj,
						serviceDefObj)).thenReturn(enumDefObj);

		XXEnumDef dbEnumDef = rangerServiceDefService
				.populateRangerEnumDefToXX(rangerEnumDefObj, enumDefObj,
						serviceDefObj,1);
		Assert.assertNotNull(dbEnumDef);
		Assert.assertEquals(dbEnumDef, enumDefObj);
		Assert.assertEquals(dbEnumDef.getName(), enumDefObj.getName());
		Assert.assertEquals(dbEnumDef.getDefid(), enumDefObj.getDefid());
		Assert.assertEquals(dbEnumDef.getId(), enumDefObj.getId());
		Assert.assertEquals(dbEnumDef.getCreateTime(),
				enumDefObj.getCreateTime());

	}

	@Test
	public void test17populateRangerEnumDefToXXnullValue() {
		RangerEnumDef rangerEnumDefObj = null;
		XXEnumDef enumDefObj = null;
		XXServiceDef serviceDefObj = null;

		Mockito.when(
				restErrorUtil.createRESTException(
						"RangerServiceDef cannot be null.",
						MessageEnums.DATA_NOT_FOUND)).thenThrow(
				new WebApplicationException());

		thrown.expect(WebApplicationException.class);

		XXEnumDef dbEnumDef = rangerServiceDefService
				.populateRangerEnumDefToXX(rangerEnumDefObj, enumDefObj,
						serviceDefObj, 1);
		Assert.assertNull(dbEnumDef);

	}

	@Test
	public void test18populateXXToRangerEnumDef() {
		XXEnumElementDefDao xEnumElementDefDao = Mockito
				.mock(XXEnumElementDefDao.class);

		List<XXEnumElementDef> enumElementDefList = new ArrayList<XXEnumElementDef>();
		XXEnumElementDef enumElementDefObj = new XXEnumElementDef();
		enumElementDefObj.setAddedByUserId(Id);
		enumElementDefObj.setCreateTime(new Date());
		enumElementDefObj.setEnumdefid(Id);
		enumElementDefObj.setId(Id);
		enumElementDefObj.setLabel("Simple");
		enumElementDefObj.setName("simple");
		enumElementDefObj.setOrder(0);
		enumElementDefObj.setUpdatedByUserId(Id);
		enumElementDefObj.setUpdateTime(new Date());
		enumElementDefList.add(enumElementDefObj);

		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(0);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName("authnType");
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());

		Mockito.when(daoManager.getXXEnumElementDef()).thenReturn(
				xEnumElementDefDao);
		Mockito.when(xEnumElementDefDao.findByEnumDefId(enumDefObj.getId()))
				.thenReturn(enumElementDefList);

		RangerEnumDef dbRangerEnumDef = rangerServiceDefService
				.populateXXToRangerEnumDef(enumDefObj);
		Assert.assertNotNull(dbRangerEnumDef);
		Assert.assertEquals(dbRangerEnumDef.getName(), enumDefObj.getName());

		Mockito.verify(daoManager).getXXEnumElementDef();
	}

	@Test
	public void test19populateRangerEnumElementDefToXX() {

		RangerEnumElementDef rangerEnumElementDefObj = new RangerEnumElementDef();
		rangerEnumElementDefObj.setLabel("Simple");
		rangerEnumElementDefObj.setName("simple");
		rangerEnumElementDefObj.setRbKeyLabel(null);
		XXEnumElementDef enumElementDefObj = new XXEnumElementDef();
		enumElementDefObj.setAddedByUserId(Id);
		enumElementDefObj.setCreateTime(new Date());
		enumElementDefObj.setEnumdefid(Id);
		enumElementDefObj.setId(Id);
		enumElementDefObj.setLabel("Simple");
		enumElementDefObj.setName("simple");
		enumElementDefObj.setOrder(0);
		enumElementDefObj.setUpdatedByUserId(Id);
		enumElementDefObj.setUpdateTime(new Date());
		XXEnumDef enumDefObj = new XXEnumDef();
		enumDefObj.setAddedByUserId(Id);
		enumDefObj.setCreateTime(new Date());
		enumDefObj.setDefaultindex(0);
		enumDefObj.setDefid(Id);
		enumDefObj.setId(Id);
		enumDefObj.setName("authnType");
		enumDefObj.setUpdatedByUserId(Id);
		enumDefObj.setUpdateTime(new Date());

		Mockito.when(
				(XXEnumElementDef) rangerAuditFields.populateAuditFields(
						enumElementDefObj, enumDefObj)).thenReturn(
				enumElementDefObj);
		XXEnumElementDef dbEnumElementDef = rangerServiceDefService
				.populateRangerEnumElementDefToXX(rangerEnumElementDefObj,
						enumElementDefObj, enumDefObj, 1);
		Assert.assertNotNull(dbEnumElementDef);
		Assert.assertEquals(dbEnumElementDef.getId(), enumElementDefObj.getId());
		Assert.assertEquals(dbEnumElementDef.getName(),
				enumElementDefObj.getName());
		Assert.assertEquals(dbEnumElementDef.getLabel(),
				enumElementDefObj.getLabel());
		Assert.assertEquals(dbEnumElementDef.getCreateTime(),
				enumElementDefObj.getCreateTime());
		Assert.assertEquals(dbEnumElementDef.getAddedByUserId(),
				enumElementDefObj.getAddedByUserId());
		Assert.assertEquals(dbEnumElementDef.getUpdateTime(),
				enumElementDefObj.getUpdateTime());
		Assert.assertEquals(dbEnumElementDef.getUpdatedByUserId(),
				enumElementDefObj.getUpdatedByUserId());
		Mockito.verify(rangerAuditFields).populateAuditFields(
				enumElementDefObj, enumDefObj);
	}

	@Test
	public void test20populateXXToRangerEnumElementDef() {
		XXEnumElementDef enumElementDefObj = new XXEnumElementDef();
		enumElementDefObj.setAddedByUserId(Id);
		enumElementDefObj.setCreateTime(new Date());
		enumElementDefObj.setEnumdefid(Id);
		enumElementDefObj.setId(Id);
		enumElementDefObj.setLabel("Simple");
		enumElementDefObj.setName("simple");
		enumElementDefObj.setOrder(0);
		enumElementDefObj.setUpdatedByUserId(Id);
		enumElementDefObj.setUpdateTime(new Date());

		RangerEnumElementDef dbRangerEnumElementDef = rangerServiceDefService
				.populateXXToRangerEnumElementDef(enumElementDefObj);
		Assert.assertNotNull(dbRangerEnumElementDef);
		Assert.assertEquals(dbRangerEnumElementDef.getLabel(),
				enumElementDefObj.getLabel());
		Assert.assertEquals(dbRangerEnumElementDef.getName(),
				enumElementDefObj.getName());

	}

	@Test
	public void test21searchRangerServiceDefs() {
		setup();
		SearchFilter searchFilter = new SearchFilter();
		searchFilter.setParam(SearchFilter.POLICY_NAME, "policyName");
		searchFilter.setParam(SearchFilter.SERVICE_NAME, "serviceName");

		RangerServiceDefList dbRangerServiceDefList = rangerServiceDefService
				.searchRangerServiceDefs(searchFilter);
		Assert.assertNotNull(dbRangerServiceDefList);
	}
}
