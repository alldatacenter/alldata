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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.SecurityZoneDBStore;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.db.XXServiceDefDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerSecurityZone.RangerSecurityZoneService;
import org.apache.ranger.plugin.model.validation.RangerSecurityZoneValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerSecurityZoneServiceService;
import org.apache.ranger.view.RangerSecurityZoneList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TestSecurityZoneREST {
	@InjectMocks
	SecurityZoneREST securityZoneREST = new SecurityZoneREST();
	@Mock
	RangerValidatorFactory validatorFactory;
	@Mock
	RangerSecurityZoneValidator validator;
	@Mock
	SecurityZoneDBStore securityZoneStore;
	@Mock
	RangerBizUtil rangerBizUtil;
	@Mock
	ServiceDBStore svcStore;
	@Mock
	RangerSearchUtil searchUtil;
	@Mock
    RangerSecurityZoneServiceService securityZoneService;
	@Mock
	RESTErrorUtil restErrorUtil;
	@Mock
	RangerDaoManager daoManager;
	@Mock
	XXServiceDef xServiceDef;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private RangerSecurityZone createRangerSecurityZone() {
		String testZone1 = "testzone1";
		List<String> testZone1ResoursesList = new ArrayList(Arrays.asList("/path/to/resource1", "/path/to/resource2"));
		List<String> userGroupList = new ArrayList(Arrays.asList("testuser", "testgroup"));

		RangerSecurityZone zone = new RangerSecurityZone();
		zone.setName(testZone1);
		zone.setAdminUserGroups(userGroupList);
		zone.setAdminUsers(userGroupList);
		zone.setAuditUserGroups(userGroupList);
		zone.setAuditUsers(userGroupList);
		Map<String, RangerSecurityZoneService> services = new HashMap<>();

		List<HashMap<String, List<String>>> resources = new ArrayList<>();
		resources.add((HashMap<String, List<String>>) new HashMap<String, List<String>>().put("resource_path",
				testZone1ResoursesList));

		RangerSecurityZoneService zoneService = new RangerSecurityZoneService();

		zoneService.setResources(resources);
		services.put("test_service_1", zoneService);
		zone.setServices(services);
		return zone;
	}

	@Test
	public void testCreateSecurityZone() throws Exception {
		RangerSecurityZone rangerSecurityZone = createRangerSecurityZone();
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		when(rangerBizUtil.isAdmin()).thenReturn(true);
		when(daoManager.getXXService()).thenReturn(xServiceDao);
		when(xServiceDao.findByName("test_service_1")).thenReturn(xService);

		when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		when(xServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);

		when(validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore)).thenReturn(validator);
		doNothing().when(validator).validate(rangerSecurityZone, RangerValidator.Action.CREATE);
		when(securityZoneStore.createSecurityZone(rangerSecurityZone)).thenReturn(rangerSecurityZone);
		RangerSecurityZone createdRangerSecurityZone = securityZoneREST.createSecurityZone(rangerSecurityZone);
		assertEquals(createdRangerSecurityZone.getName(), rangerSecurityZone.getName());
		verify(validator, times(1)).validate(rangerSecurityZone, RangerValidator.Action.CREATE);
	}

	@Test
	public void testUpdateSecurityZone() throws Exception {
		RangerSecurityZone rangerSecurityZoneToUpdate = createRangerSecurityZone();
		Long securityZoneId = 2L;
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		rangerSecurityZoneToUpdate.setId(securityZoneId);
		when(rangerBizUtil.isAdmin()).thenReturn(true);
		when(validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore)).thenReturn(validator);

		when(daoManager.getXXService()).thenReturn(xServiceDao);
		when(xServiceDao.findByName("test_service_1")).thenReturn(xService);

		when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		when(xServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);

		doNothing().when(validator).validate(rangerSecurityZoneToUpdate, RangerValidator.Action.UPDATE);
		when(securityZoneStore.updateSecurityZoneById(rangerSecurityZoneToUpdate))
				.thenReturn(rangerSecurityZoneToUpdate);
		RangerSecurityZone updatedRangerSecurityZone = securityZoneREST.updateSecurityZone(securityZoneId,
				rangerSecurityZoneToUpdate);
		assertEquals(rangerSecurityZoneToUpdate.getId(), updatedRangerSecurityZone.getId());
		verify(validator, times(1)).validate(rangerSecurityZoneToUpdate, RangerValidator.Action.UPDATE);
	}

	@Test
	public void testUpdateSecurityZoneWithMisMatchId() throws Exception {
		RangerSecurityZone rangerSecurityZoneToUpdate = createRangerSecurityZone();
		Long securityZoneId = 2L;
		XXServiceDefDao xServiceDefDao = Mockito.mock(XXServiceDefDao.class);
		XXServiceDao xServiceDao = Mockito.mock(XXServiceDao.class);
		XXService xService = Mockito.mock(XXService.class);

		rangerSecurityZoneToUpdate.setId(securityZoneId);
		when(rangerBizUtil.isAdmin()).thenReturn(true);

		when(daoManager.getXXService()).thenReturn(xServiceDao);
		when(xServiceDao.findByName("test_service_1")).thenReturn(xService);

		when(daoManager.getXXServiceDef()).thenReturn(xServiceDefDao);
		when(xServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);

		when(validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore)).thenReturn(validator);
		doNothing().when(validator).validate(rangerSecurityZoneToUpdate, RangerValidator.Action.UPDATE);
		when(securityZoneStore.updateSecurityZoneById(rangerSecurityZoneToUpdate))
				.thenReturn(rangerSecurityZoneToUpdate);
		when(restErrorUtil.createRESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		RangerSecurityZone updatedRangerSecurityZone = securityZoneREST.updateSecurityZone(9L,
				rangerSecurityZoneToUpdate);
		assertEquals(rangerSecurityZoneToUpdate.getId(), updatedRangerSecurityZone.getId());
		verify(validator, times(1)).validate(rangerSecurityZoneToUpdate, RangerValidator.Action.UPDATE);
	}

	@Test(expected = WebApplicationException.class)
	public void testGetSecurityZoneById() throws Exception {
		RangerSecurityZone securityZone = createRangerSecurityZone();
		Long securityZoneId = 2L;
		securityZone.setId(securityZoneId);
		when(securityZoneStore.getSecurityZone(securityZoneId)).thenReturn(securityZone);
		when(rangerBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(true);
		RangerSecurityZone rangerSecurityZone = securityZoneREST.getSecurityZone(securityZoneId);
		assertEquals(securityZoneId, rangerSecurityZone.getId());
		verify(securityZoneStore, times(1)).getSecurityZone(securityZoneId);

		//No access
		when(rangerBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(false);
		when(restErrorUtil.createRESTException(Mockito.anyString(), Mockito.any())).thenReturn(new WebApplicationException());
		securityZoneREST.getSecurityZone(securityZoneId);
		verify(securityZoneStore, times(0)).getSecurityZone(securityZoneId);
	}

	@Test(expected = WebApplicationException.class)
	public void testGetSecurityZoneByName() throws Exception {
		RangerSecurityZone securityZone = createRangerSecurityZone();
		Long securityZoneId = 2L;
		String securityZoneName = securityZone.getName();
		securityZone.setId(securityZoneId);
		when(securityZoneStore.getSecurityZoneByName(securityZoneName)).thenReturn(securityZone);
		when(rangerBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(true);
		RangerSecurityZone rangerSecurityZone = securityZoneREST.getSecurityZone(securityZoneName);
		assertEquals(securityZoneName, rangerSecurityZone.getName());
		verify(securityZoneStore, times(1)).getSecurityZoneByName(securityZoneName);

		//No access
		when(rangerBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(false);
		when(restErrorUtil.createRESTException(Mockito.anyString(), Mockito.any())).thenReturn(new WebApplicationException());
		securityZoneREST.getSecurityZone(securityZoneName);
		verify(securityZoneStore, times(0)).getSecurityZoneByName(securityZoneName);
	}

	@Test(expected = WebApplicationException.class)
	public void testGetAllSecurityZone() throws Exception {
		RangerSecurityZone securityZone = createRangerSecurityZone();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchFilter filter = new SearchFilter();
		when(
				searchUtil.getSearchFilter(request, securityZoneService.sortFields))
				.thenReturn(filter);
		Long securityZoneId = 2L;
		securityZone.setId(securityZoneId);
		List<RangerSecurityZone> zonesList = new ArrayList<>();
		zonesList.add(securityZone);
		RangerSecurityZoneList rangerZoneList = new RangerSecurityZoneList();
		rangerZoneList.setSecurityZoneList(zonesList);

		when(securityZoneStore.getSecurityZones(filter)).thenReturn(zonesList);
		when(rangerBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(true);

		RangerSecurityZoneList returnedZonesList = securityZoneREST.getAllZones(request);
		assertEquals(returnedZonesList.getResultSize(), rangerZoneList.getList().size());
		verify(securityZoneStore, times(1)).getSecurityZones(filter);

		//No access
		when(rangerBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(false);
		when(restErrorUtil.createRESTException(Mockito.anyString(), Mockito.any())).thenReturn(new WebApplicationException());
		securityZoneREST.getAllZones(request);
		verify(securityZoneStore, times(0)).getSecurityZones(filter);
	}

	@Test
	public void testDeleteSecurityZoneById() throws Exception {
		RangerSecurityZone securityZone = createRangerSecurityZone();
		Long securityZoneId = 2L;
		securityZone.setId(securityZoneId);
		when(rangerBizUtil.isAdmin()).thenReturn(true);
		when(validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore)).thenReturn(validator);
		doNothing().when(validator).validate(securityZoneId, RangerValidator.Action.DELETE);
		securityZoneREST.deleteSecurityZone(securityZoneId);
		verify(securityZoneStore, times(1)).deleteSecurityZoneById(securityZoneId);

	}

	@Test
	public void testDeleteSecurityZoneByName() throws Exception {
		RangerSecurityZone securityZone = createRangerSecurityZone();
		Long securityZoneId = 2L;
		securityZone.setId(securityZoneId);
		String securityZoneName = securityZone.getName();
		when(rangerBizUtil.isAdmin()).thenReturn(true);
		when(validatorFactory.getSecurityZoneValidator(svcStore, securityZoneStore)).thenReturn(validator);
		doNothing().when(validator).validate(securityZoneName, RangerValidator.Action.DELETE);
		securityZoneREST.deleteSecurityZone(securityZoneName);
		verify(securityZoneStore, times(1)).deleteSecurityZoneByName(securityZoneName);

	}
}
