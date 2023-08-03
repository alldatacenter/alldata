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

import org.apache.ranger.common.db.BaseDao;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.db.XXGroupUserDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.entity.XXDBBase;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXGroupUser;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.view.VXGroupUser;
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
public class TestXGroupUserService {

	@InjectMocks
	XGroupUserService xGroupUserService;

	@Mock
	VXGroupUser vxGroupUser;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	XXGroupUserDao xXGroupUserDao;

	@Mock
	XXGroupUser xXGroupUser;

	@Mock
	XXGroupDao xXGroupDao;

	@Mock
	XXGroup xXGroup;

	@Mock
	XXDBBase gjObj;

	@Mock
	XXPortalUserDao xXPortalUserDao;

	@Mock
	XXPortalUser tUser;

	@Mock
	BaseDao entityDao;

	@Mock
	XXUserDao xXUserDao;

	@Mock
	AbstractBaseResourceService abstractBaseResourceService;

	@Test
	public void test1CreateXGroupUserWithOutLogin() {
		XXGroupUser resource = createXXGroupUser();
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xXGroupUserDao);
		Mockito.when(xXGroupUserDao.findByGroupNameAndUserId(resource.getName(), resource.getId()))
				.thenReturn(resource);
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup xGroup = createXXGroup();
		VXGroupUser vxGroupUser = createVXGroupUser();
		Mockito.when(xXGroupDao.findByGroupName(vxGroupUser.getName())).thenReturn(xGroup);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.getById(vxGroupUser.getId())).thenReturn(tUser);
		Mockito.when(entityDao.update(resource)).thenReturn(resource);
		xGroupUserService.createXGroupUserWithOutLogin(vxGroupUser);

	}

	@Test
	public void test2GetTransactionLog() {
		VXGroupUser vXGroupUser = createVXGroupUser();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup xGroup = createXXGroup();
		Mockito.when(xXGroupDao.getById(1L)).thenReturn(xGroup);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		XXUser xUser = createXXUser();
		Mockito.when(xXUserDao.getById(1L)).thenReturn(xUser);
		xGroupUserService.getTransactionLog(vXGroupUser, "create");

	}

	@Test
	public void test3GetTransactionLog() {
		VXGroupUser vObj = createVXGroupUser();
		XXGroupUser mObj = createXXGroupUser();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup xGroup = createXXGroup();
		Mockito.when(xXGroupDao.getById(1L)).thenReturn(xGroup);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		XXUser xUser = createXXUser();
		Mockito.when(xXUserDao.getById(1L)).thenReturn(xUser);
		xGroupUserService.getTransactionLog(vObj, mObj, "create");

	}

	private XXGroup createXXGroup() {
		XXGroup xXGroup = new XXGroup();
		Date date = new Date();
		xXGroup.setAddedByUserId(1L);
		xXGroup.setCreateTime(date);
		xXGroup.setCredStoreId(1L);
		xXGroup.setDescription("this is test description");
		xXGroup.setGroupSource(0);
		xXGroup.setGroupType(1);
		xXGroup.setId(1L);
		xXGroup.setIsVisible(1);
		xXGroup.setName("testName");
		xXGroup.setUpdateTime(date);

		return xXGroup;
	}

	private VXGroupUser createVXGroupUser() {
		VXGroupUser vxGroupUser = new VXGroupUser();
		Date date = new Date();
		vxGroupUser.setCreateDate(date);
		vxGroupUser.setId(1l);
		vxGroupUser.setMObj(gjObj);
		vxGroupUser.setName("testGroupUser");
		vxGroupUser.setOwner("admin");
		vxGroupUser.setParentGroupId(1L);
		vxGroupUser.setUpdateDate(date);
		vxGroupUser.setUpdatedBy("admin");
		vxGroupUser.setUserId(1L);
		return vxGroupUser;
	}

	private XXGroupUser createXXGroupUser() {
		XXGroupUser xXGroupUser = new XXGroupUser();
		xXGroupUser.setAddedByUserId(1L);
		Date date = new Date();
		xXGroupUser.setCreateTime(date);
		xXGroupUser.setId(1L);
		xXGroupUser.setName("testGroupUser");
		xXGroupUser.setParentGroupId(1L);
		xXGroupUser.setUpdatedByUserId(1L);
		xXGroupUser.setUpdateTime(date);
		xXGroupUser.setUserId(1L);
		return xXGroupUser;

	}

	private XXUser createXXUser() {
		XXUser xUser = new XXUser();
		xUser.setAddedByUserId(1L);
		Date date = new Date();
		xUser.setCreateTime(date);
		xUser.setCredStoreId(1L);
		xUser.setDescription("this is test xUser");
		xUser.setId(1L);
		xUser.setIsVisible(1);
		xUser.setName("testUser");
		xUser.setStatus(1);
		xUser.setUpdatedByUserId(1L);
		xUser.setUpdateTime(date);
		return xUser;

	}
}
