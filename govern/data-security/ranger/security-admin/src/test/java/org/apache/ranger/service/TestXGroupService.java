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
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.ranger.entity.XXDBBase;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.util.RangerEnumUtil;
import org.apache.ranger.view.VXGroup;

import java.util.Date;

import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.db.BaseDao;

@RunWith(MockitoJUnitRunner.Silent.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestXGroupService {

	@InjectMocks
	XGroupService XGroupService;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	XXGroupDao xXGroupDao;

	@Mock
	XXGroup resource;

	@Mock
	VXGroup vxGroup;

	@Mock
	XXPortalUserDao xXPortalUserDao;

	@Mock
	BaseDao entityDao;

	@Mock
	XXDBBase xXDBBase;

	@Mock
	XXPortalUser tUser;

	@Mock
	RangerEnumUtil xaEnumUtil;

	@Mock
	StringUtil stringUtil;

	@Mock
	AbstractBaseResourceService abstractBaseResourceService;

	@Test
	public void test1GetGroupByGroupName() {
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup xxGroup = createXXGroup();
		Mockito.when(xXGroupDao.findByGroupName(xxGroup.getName())).thenReturn(xxGroup);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.getById(xxGroup.getAddedByUserId())).thenReturn(tUser);
		XGroupService.getGroupByGroupName(xxGroup.getName());
	}

	@Test
	public void test2CreateXGroupWithOutLogin() {
		VXGroup vxGroup = createvXGroup();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		XXGroup resource = createXXGroup();
		Mockito.when(xXGroupDao.findByGroupName(vxGroup.getName())).thenReturn(resource);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.getById(1l)).thenReturn(tUser);
		Mockito.when(entityDao.update(resource)).thenReturn(resource);
		XGroupService.createXGroupWithOutLogin(vxGroup);
	}

	@Test
	public void test3ReadResourceWithOutLogin() {
		XXGroup resource = createXXGroup();
		Mockito.when(entityDao.getById(resource.getId())).thenReturn(resource);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.getById(resource.getAddedByUserId())).thenReturn(tUser);
		XGroupService.readResourceWithOutLogin(resource.getId());
	}

	@Test
	public void test4GetTransactionLog() {
		VXGroup vObj = createvXGroup();
		XGroupService.getTransactionLog(vObj, "update");

	}

	@Test
	public void test5GetTransactionLog() {
		VXGroup vObj = createvXGroup();
		XXGroup mObj = createXXGroup();
		XGroupService.getTransactionLog(vObj, mObj, "create");
	}

	public VXGroup createvXGroup() {
		VXGroup vXGroup = new VXGroup();
		Date date = new Date();
		vXGroup.setCreateDate(date);
		vXGroup.setCredStoreId(1L);
		vXGroup.setDescription("this is test description");
		vXGroup.setGroupSource(0);
		vXGroup.setGroupType(1);
		vXGroup.setId(1L);
		vXGroup.setIsVisible(1);
		vXGroup.setName("testGroup");
		vXGroup.setMObj(xXDBBase);
		vXGroup.setOwner("admin");
		vXGroup.setUpdateDate(date);
		vXGroup.setUpdatedBy("admin");
		return vXGroup;
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
		xXGroup.setName("testGroup");
		xXGroup.setUpdateTime(date);
		return xXGroup;
	}

}
