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
import java.util.List;

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXTagDefDao;
import org.apache.ranger.entity.XXTagAttributeDef;
import org.apache.ranger.entity.XXTagDef;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRangerTagDefService {
	Long id = 1L;
	String guid = "989898_01_1";
	String name = "test";
	Long serviceId = 5L;

	@InjectMocks
	RangerTagDefService rangerTagDefService = new RangerTagDefService();
	
	@Mock
	RangerDaoManager daoMgr;
	
	@Test
	public void test1ValidateForCreate() {
		RangerTagDef rangerServiceDef = new RangerTagDef();
		
		rangerTagDefService.validateForCreate(rangerServiceDef);
		Assert.assertNotNull(rangerServiceDef);
	}
	
	@Test
	public void test2validateForUpdate() {
		RangerTagDef rangerServiceDef = new RangerTagDef();
		XXTagDef xXTagDef = new XXTagDef();
		
		rangerTagDefService.validateForUpdate(rangerServiceDef, xXTagDef);
		Assert.assertNotNull(rangerServiceDef);
	}
	
	@Test
	public void test3postUpdate(){
		XXTagDef tagDef = new XXTagDef();
		tagDef.setId(id);
		tagDef.setName(name);
		tagDef.setUpdateTime(new Date());
		
		List<XXTagAttributeDef> tagAttrDefList = new ArrayList<XXTagAttributeDef>();
		XXTagAttributeDef xxTagAttributeDef = new XXTagAttributeDef();
		xxTagAttributeDef.setId(id);
		xxTagAttributeDef.setName(name);
		tagAttrDefList.add(xxTagAttributeDef);

		RangerTagDef result = rangerTagDefService.postUpdate(tagDef);
		Assert.assertEquals(result.getId(), tagAttrDefList.get(0).getId());
		Assert.assertEquals(result.getName(), tagAttrDefList.get(0).getName());

	}
		
	@Test
	public void test4getTagDefByGuid(){
		XXTagDef xxTagDef = new XXTagDef();
		xxTagDef.setId(id);
		xxTagDef.setName(name);
		xxTagDef.setUpdateTime(new Date());
		
		XXTagDefDao xXTagDefDao = Mockito.mock(XXTagDefDao.class);
		Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
		Mockito.when(xXTagDefDao.findByGuid(guid)).thenReturn(xxTagDef);
		
		List<XXTagAttributeDef> tagAttrDefList = new ArrayList<XXTagAttributeDef>();
		XXTagAttributeDef xxTagAttributeDef = new XXTagAttributeDef();
		xxTagAttributeDef.setId(id);
		xxTagAttributeDef.setName(name);
		tagAttrDefList.add(xxTagAttributeDef);
		
		RangerTagDef result = rangerTagDefService.getTagDefByGuid(guid);
		Assert.assertEquals(result.getId(), tagAttrDefList.get(0).getId());
		Assert.assertEquals(result.getName(), tagAttrDefList.get(0).getName());
		
		Mockito.verify(daoMgr).getXXTagDef();
		Mockito.verify(xXTagDefDao).findByGuid(guid);
	}
	
	@Test
	public void test5getTagDefByGuid(){
		XXTagDef xxTagDef = null;
		
		XXTagDefDao xXTagDefDao = Mockito.mock(XXTagDefDao.class);
		Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
		Mockito.when(xXTagDefDao.findByGuid(guid)).thenReturn(xxTagDef);
		
		RangerTagDef result = rangerTagDefService.getTagDefByGuid(guid);
		Assert.assertNull(result);
		
		Mockito.verify(daoMgr).getXXTagDef();
		Mockito.verify(xXTagDefDao).findByGuid(guid);
	}
	
	@Test
	public void test6getTagDefByName(){
		RangerTagDef oldTagDef = new RangerTagDef();
		oldTagDef.setId(id);
		oldTagDef.setName(name);
		XXTagDef xxTagDef = new XXTagDef();
		xxTagDef.setId(id);
		xxTagDef.setName(name);
		xxTagDef.setUpdateTime(new Date());
		
		XXTagDefDao xXTagDefDao = Mockito.mock(XXTagDefDao.class);
		Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
		Mockito.when(xXTagDefDao.findByName(name)).thenReturn(xxTagDef);
		
		List<XXTagAttributeDef> tagAttrDefList = new ArrayList<XXTagAttributeDef>();
		XXTagAttributeDef xxTagAttributeDef = new XXTagAttributeDef();
		xxTagAttributeDef.setId(id);
		xxTagAttributeDef.setName(name);
		tagAttrDefList.add(xxTagAttributeDef);
		
		RangerTagDef result = rangerTagDefService.getTagDefByName(name);
		Assert.assertEquals(result.getId(), tagAttrDefList.get(0).getId());
		Assert.assertEquals(result.getName(), tagAttrDefList.get(0).getName());
		
		Mockito.verify(daoMgr).getXXTagDef();
		Mockito.verify(xXTagDefDao).findByName(name);
	}
	
	@Test
	public void test7getTagDefByName(){
		XXTagDef xxTagDef = null;
		
		XXTagDefDao xXTagDefDao = Mockito.mock(XXTagDefDao.class);
		Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
		Mockito.when(xXTagDefDao.findByName(name)).thenReturn(xxTagDef);
		
		RangerTagDef result = rangerTagDefService.getTagDefByName(name);
		Assert.assertNull(result);
		
		Mockito.verify(daoMgr).getXXTagDef();
		Mockito.verify(xXTagDefDao).findByName(name);
	}
	
	@Test
	public void test8getTagDefsByServiceId(){
		List<XXTagDef> xxTagDefs = new ArrayList<XXTagDef>();
		XXTagDef xxTagDef = new XXTagDef();
		xxTagDef.setId(id);
		xxTagDef.setName(name);
		xxTagDefs.add(xxTagDef);
		
		XXTagDefDao xXTagDefDao = Mockito.mock(XXTagDefDao.class);
		Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
		Mockito.when(xXTagDefDao.findByServiceId(serviceId)).thenReturn(xxTagDefs);
		
		List<XXTagAttributeDef> tagAttrDefList = new ArrayList<XXTagAttributeDef>();
		XXTagAttributeDef xxTagAttributeDef = new XXTagAttributeDef();
		xxTagAttributeDef.setId(id);
		xxTagAttributeDef.setName(name);
		tagAttrDefList.add(xxTagAttributeDef);
		
		List<RangerTagDef> result = rangerTagDefService.getTagDefsByServiceId(serviceId);
		Assert.assertEquals(result.get(0).getId(), tagAttrDefList.get(0).getId());
		Assert.assertEquals(result.get(0).getName(), tagAttrDefList.get(0).getName());
		
		Mockito.verify(daoMgr).getXXTagDef();
		Mockito.verify(xXTagDefDao).findByServiceId(serviceId);
	}
	
	
	@Test
	public void test9getTagDefsByServiceId(){
		List<XXTagDef> xxTagDefs = new ArrayList<XXTagDef>();
	
		XXTagDefDao xXTagDefDao = Mockito.mock(XXTagDefDao.class);
		Mockito.when(daoMgr.getXXTagDef()).thenReturn(xXTagDefDao);
		Mockito.when(xXTagDefDao.findByServiceId(serviceId)).thenReturn(xxTagDefs);
		
		List<RangerTagDef> result = rangerTagDefService.getTagDefsByServiceId(serviceId);
		Assert.assertNotNull(result);
		
		Mockito.verify(daoMgr).getXXTagDef();
		Mockito.verify(xXTagDefDao).findByServiceId(serviceId);
	}
	
	@Test
	public void test10getPopulatedViewObject(){
		XXTagDef xxTagDef = new XXTagDef();
		xxTagDef.setId(id);
		xxTagDef.setName(name);
		xxTagDef.setUpdateTime(new Date());
		
		List<XXTagAttributeDef> tagAttrDefList = new ArrayList<XXTagAttributeDef>();
		XXTagAttributeDef xxTagAttributeDef = new XXTagAttributeDef();
		xxTagAttributeDef.setId(id);
		xxTagAttributeDef.setName(name);
		tagAttrDefList.add(xxTagAttributeDef);
		
		RangerTagDef result = rangerTagDefService.getPopulatedViewObject(xxTagDef);
		Assert.assertEquals(result.getId(), tagAttrDefList.get(0).getId());
		Assert.assertEquals(result.getName(), tagAttrDefList.get(0).getName());
	}
}
