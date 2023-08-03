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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXServiceConfigMapDao;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.db.XXServiceDefDao;

import org.apache.ranger.db.XXServiceVersionInfoDao;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.plugin.model.RangerService;

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
public class TestRangerServiceService {

	private static Long userId = 8L;

	@InjectMocks
	RangerServiceService serviceService = new RangerServiceService();

	@Mock
	RangerDaoManager daoManager;

	@Mock
	RangerServiceService svcService;

	@Mock
	JSONUtil jsonUtil;

	@Mock
	RangerServiceDefService serviceDefService;

	@Mock
	RangerPolicyService policyService;

	@Mock
	StringUtil stringUtil;

	@Mock
	XUserService xUserService;

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
		rangerService.setId(userId);
		rangerService.setConfigs(configs);
		rangerService.setCreateTime(new Date());
		rangerService.setDescription("service");
		rangerService.setGuid("serviceguid");
		rangerService.setIsEnabled(true);
		rangerService.setName("Hdfs service");
		rangerService.setPolicyUpdateTime(new Date());
		rangerService.setPolicyVersion(1L);
		rangerService.setType(null);
		rangerService.setUpdatedBy("Admin");
		rangerService.setUpdateTime(new Date());
		rangerService.setVersion(userId);

		return rangerService;
	}

	private XXService xService() {
		XXService xService = new XXService();
		xService.setAddedByUserId(userId);
		xService.setCreateTime(new Date());
		xService.setDescription("Hdfs service");
		xService.setGuid("serviceguid");
		xService.setId(userId);
		xService.setIsEnabled(true);
		xService.setName("Hdfs");
		xService.setPolicyUpdateTime(new Date());
		xService.setPolicyVersion(1L);
		xService.setType(1L);
		xService.setUpdatedByUserId(userId);
		xService.setUpdateTime(new Date());

		return xService;
	}

	@Test
	public void test1ValidateForCreate() {
		RangerService service = rangerService();
		serviceService.validateForCreate(service);
		Assert.assertNotNull(service);
	}

	@Test
	public void test2ValidateForUpdate() {
		RangerService vService = rangerService();
		XXService xService = xService();

		serviceService.validateForUpdate(vService, xService);
		Assert.assertNotNull(vService);
	}

	@Test
	public void test3PopulateViewBean() {
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXService xService = xService();
		String name = "fdfdfds";

		List<XXServiceConfigMap> svcConfigMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);

		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		svcConfigMapList.add(xConfMap);

		XXPortalUser tUser = new XXPortalUser();
		tUser.setAddedByUserId(userId);
		tUser.setCreateTime(new Date());
		tUser.setEmailAddress("test@gmail.com");
		tUser.setFirstName(name);
		tUser.setId(userId);
		tUser.setLastName(name);

		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setAddedByUserId(userId);
		xServiceDef.setCreateTime(new Date());
		xServiceDef.setDescription("test");
		xServiceDef.setGuid("1427365526516_835_0");
		xServiceDef.setId(userId);

		XXServiceVersionInfoDao xServiceVersionInfoDao = Mockito.mock(XXServiceVersionInfoDao.class);

		XXServiceVersionInfo serviceVersionInfo = new XXServiceVersionInfo();
		serviceVersionInfo.setServiceId(xService.getId());
		serviceVersionInfo.setPolicyVersion(xService.getPolicyVersion());
		serviceVersionInfo.setPolicyUpdateTime(xService.getPolicyUpdateTime());
		serviceVersionInfo.setTagVersion(xService.getTagVersion());
		serviceVersionInfo.setTagUpdateTime(xService.getTagUpdateTime());

		Mockito.when(daoManager.getXXServiceVersionInfo()).thenReturn(xServiceVersionInfoDao);
		Mockito.when(xServiceVersionInfoDao.findByServiceId(xService.getId())).thenReturn(
				serviceVersionInfo);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(tUser);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.getById(xService.getType())).thenReturn(
				xServiceDef);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(xServiceConfigMapDao.findByServiceId(xService.getId()))
				.thenReturn(svcConfigMapList);

		RangerService dbService = serviceService.populateViewBean(xService);

		Assert.assertNotNull(dbService);
		Assert.assertEquals(userId, dbService.getId());
		Assert.assertEquals(xService.getAddedByUserId(), dbService.getId());
		Assert.assertEquals(xService.getId(), dbService.getId());
		Assert.assertEquals(xService.getDescription(),
				dbService.getDescription());
		Assert.assertEquals(xService.getGuid(), dbService.getGuid());
		Assert.assertEquals(xService.getName(), dbService.getName());
		Assert.assertEquals(xService.getPolicyUpdateTime(),
				dbService.getPolicyUpdateTime());
		Assert.assertEquals(xService.getPolicyVersion(),
				dbService.getPolicyVersion());
		Assert.assertEquals(xService.getVersion(), dbService.getVersion());

		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(daoManager).getXXServiceConfigMap();
	}

	@Test
	public void test4GetPopulatedViewObject() {
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXService xService = xService();
		String name = "fdfdfds";

		List<XXServiceConfigMap> svcConfigMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		svcConfigMapList.add(xConfMap);

		XXPortalUser tUser = new XXPortalUser();
		tUser.setAddedByUserId(userId);
		tUser.setCreateTime(new Date());
		tUser.setEmailAddress("test@gmail.com");
		tUser.setFirstName(name);
		tUser.setId(userId);
		tUser.setLastName(name);

		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setAddedByUserId(userId);
		xServiceDef.setCreateTime(new Date());
		xServiceDef.setDescription("test");
		xServiceDef.setGuid("1427365526516_835_0");
		xServiceDef.setId(userId);

		XXServiceVersionInfoDao xServiceVersionInfoDao = Mockito.mock(XXServiceVersionInfoDao.class);

		XXServiceVersionInfo serviceVersionInfo = new XXServiceVersionInfo();
		serviceVersionInfo.setServiceId(xService.getId());
		serviceVersionInfo.setPolicyVersion(xService.getPolicyVersion());
		serviceVersionInfo.setPolicyUpdateTime(xService.getPolicyUpdateTime());
		serviceVersionInfo.setTagVersion(xService.getTagVersion());
		serviceVersionInfo.setTagUpdateTime(xService.getTagUpdateTime());

		Mockito.when(daoManager.getXXServiceVersionInfo()).thenReturn(xServiceVersionInfoDao);
		Mockito.when(xServiceVersionInfoDao.findByServiceId(xService.getId())).thenReturn(
				serviceVersionInfo);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(tUser);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.getById(xService.getType())).thenReturn(
				xServiceDef);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(xServiceConfigMapDao.findByServiceId(xService.getId()))
				.thenReturn(svcConfigMapList);

		RangerService dbService = serviceService
				.getPopulatedViewObject(xService);

		Assert.assertNotNull(dbService);
		Assert.assertEquals(userId, dbService.getId());
		Assert.assertEquals(xService.getAddedByUserId(), dbService.getId());
		Assert.assertEquals(xService.getId(), dbService.getId());
		Assert.assertEquals(xService.getDescription(),
				dbService.getDescription());
		Assert.assertEquals(xService.getGuid(), dbService.getGuid());
		Assert.assertEquals(xService.getName(), dbService.getName());
		Assert.assertEquals(xService.getPolicyUpdateTime(),
				dbService.getPolicyUpdateTime());
		Assert.assertEquals(xService.getPolicyVersion(),
				dbService.getPolicyVersion());
		Assert.assertEquals(xService.getVersion(), dbService.getVersion());

		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(daoManager).getXXServiceConfigMap();
	}

	@Test
	public void test5GetAllServices() {
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXServiceConfigMapDao xServiceConfigMapDao = Mockito
				.mock(XXServiceConfigMapDao.class);
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);

		String name = "fdfdfds";

		List<XXServiceConfigMap> svcConfigMapList = new ArrayList<XXServiceConfigMap>();
		XXServiceConfigMap xConfMap = new XXServiceConfigMap();
		xConfMap.setAddedByUserId(null);
		xConfMap.setConfigkey(name);
		xConfMap.setConfigvalue(name);
		xConfMap.setCreateTime(new Date());
		xConfMap.setServiceId(null);
		xConfMap.setUpdatedByUserId(null);
		xConfMap.setUpdateTime(new Date());
		svcConfigMapList.add(xConfMap);

		List<XXService> xServiceList = new ArrayList<XXService>();
		XXService xService = xService();
		xServiceList.add(xService);

		XXPortalUser tUser = new XXPortalUser();
		tUser.setAddedByUserId(userId);
		tUser.setCreateTime(new Date());
		tUser.setEmailAddress("test@gmail.com");
		tUser.setFirstName(name);
		tUser.setId(userId);
		tUser.setLastName(name);

		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setAddedByUserId(userId);
		xServiceDef.setCreateTime(new Date());
		xServiceDef.setDescription("test");
		xServiceDef.setGuid("1427365526516_835_0");
		xServiceDef.setId(userId);

		XXServiceVersionInfoDao xServiceVersionInfoDao = Mockito.mock(XXServiceVersionInfoDao.class);

		XXServiceVersionInfo serviceVersionInfo = new XXServiceVersionInfo();
		serviceVersionInfo.setServiceId(xService.getId());
		serviceVersionInfo.setPolicyVersion(xService.getPolicyVersion());
		serviceVersionInfo.setPolicyUpdateTime(xService.getPolicyUpdateTime());
		serviceVersionInfo.setTagVersion(xService.getTagVersion());
		serviceVersionInfo.setTagUpdateTime(xService.getTagUpdateTime());

		Mockito.when(daoManager.getXXServiceVersionInfo()).thenReturn(xServiceVersionInfoDao);
		Mockito.when(xServiceVersionInfoDao.findByServiceId(xService.getId())).thenReturn(
				serviceVersionInfo);

		Mockito.when(daoManager.getXXService()).thenReturn(xServiceDao);
		Mockito.when(xServiceDao.getAll()).thenReturn(xServiceList);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(tUser);

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.getById(xService.getType())).thenReturn(
				xServiceDef);

		Mockito.when(daoManager.getXXServiceConfigMap()).thenReturn(
				xServiceConfigMapDao);
		Mockito.when(xServiceConfigMapDao.findByServiceId(xService.getId()))
				.thenReturn(svcConfigMapList);

		List<RangerService> dbServiceList = serviceService.getAllServices();
		Assert.assertNotNull(dbServiceList);

		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(daoManager).getXXServiceConfigMap();
	}

	@Test
	public void test6GetTransactionLogCreate() {
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);
		RangerService rangerService = rangerService();

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.findByName(rangerService.getType()))
				.thenReturn(xServiceDef);

		List<XXTrxLog> dbXXTrxLogList = serviceService.getTransactionLog(
				rangerService, 1);
		Assert.assertNotNull(dbXXTrxLogList);
	}

	@Test
	public void test7GetTransactionLogUpdate() {
		RangerService rangerService = rangerService();

		List<XXTrxLog> dbXXTrxLogList = serviceService.getTransactionLog(
				rangerService, 2);
		Assert.assertNull(dbXXTrxLogList);
	}

	@Test
	public void test8GetTransactionLogDelete() {
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDef xServiceDef = Mockito.mock(XXServiceDef.class);
		RangerService rangerService = rangerService();

		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		Mockito.when(xServiceDefDao.findByName(rangerService.getType()))
				.thenReturn(xServiceDef);

		List<XXTrxLog> dbXXTrxLogList = serviceService.getTransactionLog(
				rangerService, 3);
		Assert.assertNotNull(dbXXTrxLogList);
	}
}
