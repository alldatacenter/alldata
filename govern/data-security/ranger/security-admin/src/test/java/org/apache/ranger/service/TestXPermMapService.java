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

import java.util.Date;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXAssetDao;
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXResourceDao;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.entity.XXDBBase;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPermMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXUser;
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
public class TestXPermMapService {

	@InjectMocks
	XPermMapService xPermMapService;

	@Mock
	XXPermMap xXPermMap;

	@Mock
	UserSessionBase currentUserSession;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	StringUtil stringUtil;

	@Mock
	XXPortalUserDao xXPortalUserDao;

	@Mock
	XXPortalUser tUser;

	@Mock
	XGroupService xGroupService;

	@Mock
	VXGroup vXGroup;

	@Mock
	XUserService xUserService;

	@Mock
	VXUser vXUser;

	@Mock
	XXDBBase gjObj;

	@Mock
	XXGroupDao xXGroupDao;

	@Mock
	XXResourceDao xXResourceDao;

	@Mock
	XXResource xXResource;

	@Mock
	XXAssetDao xXAssetDao;

	@Mock
	XXAsset xXAsset;

	@Mock
	RangerEnumUtil xaEnumUtil;

	@Mock
	AbstractBaseResourceService abstractBaseResourceService;

	@Test
	public void test1GetGroupName() {
		Mockito.when(xGroupService.readResource(1L)).thenReturn(vXGroup);
		xPermMapService.getGroupName(1L);

	}

	@Test
	public void test2GetUserName() {
		Mockito.when(xUserService.readResource(1L)).thenReturn(vXUser);
		xPermMapService.getUserName(1L);

	}

	@Test
	public void test3GetTransactionLog() {
		VXPermMap vXPermMap = createVXPermMap();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup xGroup = createXXGroup();
		Mockito.when(xXGroupDao.getById(1L)).thenReturn(xGroup);
		Mockito.when(daoManager.getXXResource()).thenReturn(xXResourceDao);
		Mockito.when(xXResourceDao.getById(vXPermMap.getId())).thenReturn(xXResource);
		Mockito.when(xXResource.getAssetId()).thenReturn(1L);
		Mockito.when(daoManager.getXXAsset()).thenReturn(xXAssetDao);
		Mockito.when(xXAssetDao.getById(1L)).thenReturn(xXAsset);
		xPermMapService.getTransactionLog(vXPermMap, "create");

	}

	@Test
	public void test4GetTransactionLog() {
		VXPermMap vObj = createVXPermMap();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup xGroup = createXXGroup();
		Mockito.when(xXGroupDao.getById(1L)).thenReturn(xGroup);
		Mockito.when(daoManager.getXXResource()).thenReturn(xXResourceDao);
		Mockito.when(xXResourceDao.getById(vObj.getId())).thenReturn(xXResource);
		Mockito.when(xXResource.getAssetId()).thenReturn(1L);
		Mockito.when(daoManager.getXXAsset()).thenReturn(xXAssetDao);
		Mockito.when(xXAssetDao.getById(1L)).thenReturn(xXAsset);
		xPermMapService.getTransactionLog(vObj, vObj, "update");
	}

	private VXPermMap createVXPermMap() {
		VXPermMap vXPermMap = new VXPermMap();
		Date date = new Date();
		vXPermMap.setCreateDate(date);
		vXPermMap.setGrantOrRevoke(false);
		vXPermMap.setGroupId(1L);
		vXPermMap.setGroupName("testGroupName");
		vXPermMap.setId(1L);
		vXPermMap.setIpAddress("123.45.678.90");
		vXPermMap.setIsRecursive(0);
		vXPermMap.setIsWildCard(false);
		vXPermMap.setMObj(gjObj);
		vXPermMap.setOwner("admin");
		vXPermMap.setPermFor(0);
		vXPermMap.setPermGroup("");
		vXPermMap.setPermType(0);
		vXPermMap.setResourceId(1L);
		vXPermMap.setUpdateDate(date);
		vXPermMap.setUpdatedBy("admin");
		vXPermMap.setUserId(1L);
		vXPermMap.setUserName("testUser");
		return vXPermMap;
	}

	public XXGroup createXXGroup() {
		XXGroup xGroup = new XXGroup();
		xGroup.setAddedByUserId(1L);
		Date date = new Date();
		xGroup.setCreateTime(date);
		xGroup.setCredStoreId(1L);
		xGroup.setDescription("this is test xGroup");
		xGroup.setGroupSource(1);
		xGroup.setGroupType(1);
		xGroup.setId(1L);
		xGroup.setIsVisible(1);
		xGroup.setName("testxGroup");
		xGroup.setStatus(1);
		xGroup.setUpdatedByUserId(1L);
		xGroup.setUpdateTime(date);
		return xGroup;
	}
}
