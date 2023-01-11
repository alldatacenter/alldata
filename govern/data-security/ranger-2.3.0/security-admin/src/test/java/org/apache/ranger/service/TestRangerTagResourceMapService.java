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
package org.apache.ranger.service;

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceVersionInfoDao;
import org.apache.ranger.db.XXTagResourceMapDao;
import org.apache.ranger.entity.XXTagResourceMap;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerTagResourceMapService {
	@InjectMocks
	RangerTagResourceMapService rangerTagResourceMapService;

	@Mock
	XXTagResourceMap xXTagResourceMap;

	@Mock
	RangerDaoManager daoMgr;

	@Mock
	XXServiceVersionInfoDao xXServiceVersionInfoDao;

	@Mock
	org.apache.ranger.db.XXPortalUserDao XXPortalUserDao;

	@Mock
	XXTagResourceMapDao xXTagResourceMapDao;

	@Test
	public void test1PostCreate() {
		Mockito.when(daoMgr.getXXPortalUser()).thenReturn(XXPortalUserDao);
		Mockito.when(daoMgr.getXXServiceVersionInfo()).thenReturn(xXServiceVersionInfoDao);
		rangerTagResourceMapService.postCreate(xXTagResourceMap);

	}

	@Test
	public void test3GetPopulatedViewObject() {
		Mockito.when(daoMgr.getXXPortalUser()).thenReturn(XXPortalUserDao);
		rangerTagResourceMapService.getPopulatedViewObject(xXTagResourceMap);

	}

	@Test
	public void test4GetByTagId() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByTagId(1L);

	}

	@Test
	public void test5GetByTagGuid() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByTagGuid("1");

	}

	@Test
	public void test6GetByResourceId() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByResourceId(1L);

	}

	@Test
	public void test7GetTagIdsForResourceId() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getTagIdsForResourceId(1L);

	}

	@Test
	public void test8GetByResourceGuid() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByResourceGuid("1");

	}

	@Test
	public void test9GetByGuid() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByGuid("1");

	}

	@Test
	public void test10GetByTagAndResourceId() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByTagAndResourceId(1L, 1L);

	}

	@Test
	public void test11GetByTagAndResourceGuid() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getByTagAndResourceGuid("1", "1");

	}

	@Test
	public void test12GetTagResourceMapsByServiceId() {
		Mockito.when(daoMgr.getXXTagResourceMap()).thenReturn(xXTagResourceMapDao);
		rangerTagResourceMapService.getTagResourceMapsByServiceId(1L);

	}
}
