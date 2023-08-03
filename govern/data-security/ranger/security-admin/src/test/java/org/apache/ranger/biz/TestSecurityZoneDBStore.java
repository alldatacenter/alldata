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
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXGlobalStateDao;
import org.apache.ranger.db.XXSecurityZoneDao;
import org.apache.ranger.entity.XXSecurityZone;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.store.ServicePredicateUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerSecurityZoneServiceService;
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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSecurityZoneDBStore {
	private static final String RANGER_GLOBAL_STATE_NAME = "RangerSecurityZone";

	@InjectMocks
	SecurityZoneDBStore securityZoneDBStore = new SecurityZoneDBStore();

	@Mock
	RangerSecurityZoneServiceService securityZoneService;

	@Mock
	SecurityZoneRefUpdater securityZoneRefUpdater;

	@Mock
	RangerDaoManager daoManager;
	
	@Mock
	RangerBizUtil bizUtil;

	@Mock
	ServicePredicateUtil predicateUtil;
	@Mock
	RESTErrorUtil restErrorUtil;

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	@Test
	public void test1createSecurityZone() throws Exception {
		XXSecurityZone xxSecurityZone = null;
		RangerSecurityZone securityZone = new RangerSecurityZone();
		RangerSecurityZone createdSecurityZone = new RangerSecurityZone();
		createdSecurityZone.setId(2L);

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		XXGlobalStateDao xXGlobalStateDao = Mockito.mock(XXGlobalStateDao.class);

		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneName(securityZone.getName())).thenReturn(xxSecurityZone);

		Mockito.when(daoManager.getXXGlobalState()).thenReturn(xXGlobalStateDao);
		Mockito.doNothing().when(xXGlobalStateDao).onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);

		Mockito.when(securityZoneService.create(securityZone)).thenReturn(createdSecurityZone);
		Mockito.doNothing().when(securityZoneRefUpdater).createNewZoneMappingForRefTable(createdSecurityZone);
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Mockito.doNothing().when(bizUtil).createTrxLog(trxLogList);
		
		RangerSecurityZone expectedSecurityZone = securityZoneDBStore.createSecurityZone(securityZone);

		Assert.assertNull(xxSecurityZone);
		Assert.assertEquals(createdSecurityZone.getId(), expectedSecurityZone.getId());
		Mockito.verify(daoManager).getXXSecurityZoneDao();
		Mockito.verify(daoManager).getXXGlobalState();
		Mockito.verify(securityZoneService).create(securityZone);
	}

	@Test
	public void test2updateSecurityZoneById() throws Exception {
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		RangerSecurityZone updateSecurityZone = new RangerSecurityZone();
		updateSecurityZone.setId(2L);

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		XXGlobalStateDao xXGlobalStateDao = Mockito.mock(XXGlobalStateDao.class);

		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneId(securityZone.getId())).thenReturn(xxSecurityZone);

		Mockito.when(daoManager.getXXGlobalState()).thenReturn(xXGlobalStateDao);
		Mockito.doNothing().when(xXGlobalStateDao).onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);

		Mockito.when(securityZoneService.update(securityZone)).thenReturn(updateSecurityZone);
		Mockito.doNothing().when(securityZoneRefUpdater).createNewZoneMappingForRefTable(updateSecurityZone);
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Mockito.doNothing().when(bizUtil).createTrxLog(trxLogList);

		RangerSecurityZone expectedSecurityZone = securityZoneDBStore.updateSecurityZoneById(securityZone);

		Assert.assertNotNull(xxSecurityZone);
		Assert.assertEquals(updateSecurityZone.getId(), expectedSecurityZone.getId());
		Mockito.verify(daoManager).getXXSecurityZoneDao();
		Mockito.verify(daoManager).getXXGlobalState();
		Mockito.verify(securityZoneService).update(securityZone);
	}

	@Test
	public void test3deleteSecurityZoneByName() throws Exception {
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		XXGlobalStateDao xXGlobalStateDao = Mockito.mock(XXGlobalStateDao.class);

		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneName(securityZone.getName())).thenReturn(xxSecurityZone);
		Mockito.when(securityZoneService.read(securityZone.getId())).thenReturn(securityZone);
		Mockito.when(daoManager.getXXGlobalState()).thenReturn(xXGlobalStateDao);
		Mockito.doNothing().when(xXGlobalStateDao).onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);
		Mockito.when(securityZoneRefUpdater.cleanupRefTables(securityZone)).thenReturn(true);
		Mockito.when(securityZoneService.delete(securityZone)).thenReturn(true);
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Mockito.doNothing().when(bizUtil).createTrxLog(trxLogList);

		securityZoneDBStore.deleteSecurityZoneByName(securityZone.getName());

		Assert.assertNotNull(xxSecurityZone);
	}

	@Test
	public void test4deleteSecurityZoneById() throws Exception {
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");

		XXGlobalStateDao xXGlobalStateDao = Mockito.mock(XXGlobalStateDao.class);
		Mockito.when(securityZoneService.read(securityZone.getId())).thenReturn(securityZone);
		Mockito.when(daoManager.getXXGlobalState()).thenReturn(xXGlobalStateDao);
		Mockito.doNothing().when(xXGlobalStateDao).onGlobalStateChange(RANGER_GLOBAL_STATE_NAME);
		Mockito.when(securityZoneRefUpdater.cleanupRefTables(securityZone)).thenReturn(true);
		Mockito.when(securityZoneService.delete(securityZone)).thenReturn(true);
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Mockito.doNothing().when(bizUtil).createTrxLog(trxLogList);

		securityZoneDBStore.deleteSecurityZoneById(securityZone.getId());
	}

	@Test
	public void test5getSecurityZoneByName() throws Exception {
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		xxSecurityZone.setName("sz1");
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");
		RangerSecurityZone createdSecurityZone = new RangerSecurityZone();
		createdSecurityZone.setId(2L);
		createdSecurityZone.setName("sz1");
		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);

		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneName(securityZone.getName())).thenReturn(xxSecurityZone);
		Mockito.when(securityZoneService.read(securityZone.getId())).thenReturn(createdSecurityZone);

		RangerSecurityZone expectedSecurityZone = securityZoneDBStore.getSecurityZoneByName(securityZone.getName());

		Assert.assertNotNull(xxSecurityZone);
		Assert.assertEquals(createdSecurityZone.getName(), expectedSecurityZone.getName());
		Mockito.verify(securityZoneService).read(securityZone.getId());
	}

	@Test
	public void test6getSecurityZones() throws Exception {
		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.ZONE_NAME, "sz1");

		List<RangerSecurityZone> ret = new ArrayList<>();
		List<XXSecurityZone> xxSecurityZones = new ArrayList<XXSecurityZone>();
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		xxSecurityZone.setName("sz1");
		xxSecurityZones.add(xxSecurityZone);

		RangerSecurityZone rangerSecurityZone = new RangerSecurityZone();
		rangerSecurityZone.setId(3L);
		ret.add(rangerSecurityZone);
		List<RangerSecurityZone> copy = new ArrayList<>(ret);

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.getAll()).thenReturn(xxSecurityZones);
		Mockito.when(securityZoneService.read(xxSecurityZone.getId())).thenReturn(rangerSecurityZone);
		Mockito.doNothing().when(predicateUtil).applyFilter(copy, filter);

		securityZoneDBStore.getSecurityZones(filter);

		Assert.assertNotNull(xxSecurityZone);
		Assert.assertNotNull(xxSecurityZones);
		Mockito.verify(daoManager).getXXSecurityZoneDao();
		Mockito.verify(securityZoneService).read(xxSecurityZone.getId());
		Mockito.verify(predicateUtil).applyFilter(copy, filter);
	}

	@Test
	public void test7getSecurityZonesForService() throws Exception {
		String serviceName = "hdfs_service";
		Map<String, RangerSecurityZone.RangerSecurityZoneService> retMap = null;

		SearchFilter filter = new SearchFilter();
		filter.setParam(SearchFilter.ZONE_NAME, "sz1");
		filter.setParam(SearchFilter.SERVICE_NAME, serviceName);
		List<RangerSecurityZone> ret = new ArrayList<>();
		List<XXSecurityZone> xxSecurityZones = new ArrayList<XXSecurityZone>();

		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		xxSecurityZone.setName("sz1");
		xxSecurityZones.add(xxSecurityZone);

		RangerSecurityZone rangerSecurityZone = new RangerSecurityZone();
		rangerSecurityZone.setId(3L);
		ret.add(rangerSecurityZone);
//		List<RangerSecurityZone> copy = new ArrayList<>(ret);

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.getAll()).thenReturn(xxSecurityZones);
		Mockito.when(securityZoneService.read(xxSecurityZone.getId())).thenReturn(rangerSecurityZone);
//		Mockito.doNothing().when(predicateUtil).applyFilter(copy, filter);
		retMap = new HashMap<>();
		retMap.put(rangerSecurityZone.getName(), rangerSecurityZone.getServices().get(serviceName));

		securityZoneDBStore.getSecurityZonesForService(serviceName);

		Assert.assertNotNull(xxSecurityZone);
		Assert.assertNotNull(xxSecurityZones);
		Mockito.verify(daoManager).getXXSecurityZoneDao();
		Mockito.verify(securityZoneService).read(xxSecurityZone.getId());
	}
		
	@Test
	public void test8createSecurityZoneWithExistingName() throws Exception {
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		RangerSecurityZone createdSecurityZone = new RangerSecurityZone();
		createdSecurityZone.setId(2L);

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);

		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneName(securityZone.getName())).thenReturn(xxSecurityZone);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), Mockito.any(MessageEnums.class)))
				.thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		securityZoneDBStore.createSecurityZone(securityZone);

		Mockito.verify(daoManager, times(1)).getXXSecurityZoneDao();
		Mockito.verify(xXSecurityZoneDao, times(1)).findByZoneName(securityZone.getName());
	}

	@Test
	public void test9updateSecurityZoneByUnknownId() throws Exception {
		RangerSecurityZone securityZoneToUpdate = new RangerSecurityZone();
		securityZoneToUpdate.setId(2L);

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneId(securityZoneToUpdate.getId())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		securityZoneDBStore.updateSecurityZoneById(securityZoneToUpdate);
		Mockito.verify(daoManager, times(1)).getXXSecurityZoneDao();
		Mockito.verify(xXSecurityZoneDao, times(1)).findByZoneId(securityZoneToUpdate.getId());
	}

	@Test
	public void test10deleteSecurityZoneByWrongName() throws Exception {
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneName(securityZone.getName())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		securityZoneDBStore.deleteSecurityZoneByName(securityZone.getName());
		Mockito.verify(xXSecurityZoneDao, times(1)).findByZoneName(xxSecurityZone.getName());

	}

	@Test
	public void test11getSecurityZoneByWrongName() throws Exception {
		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.findByZoneName(securityZone.getName())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		securityZoneDBStore.getSecurityZoneByName(securityZone.getName());
		Mockito.verify(xXSecurityZoneDao, times(1)).findByZoneName(securityZone.getName());

	}
}
