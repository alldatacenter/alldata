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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.apache.ranger.biz.AssetMgr;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.biz.TagDBStore;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceDao;
import org.apache.ranger.db.XXServiceDefDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.store.TagValidator;
import org.apache.ranger.plugin.util.RangerPluginCapability;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServiceTags;
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
public class TestTagREST {
	private static Long id = 1L;
	private static String gId = "1427365526516_835_0";
	private static String name = "test";
	private static String serviceName = "HDFS";
	private static String resourceSignature = "testResourceSign";
	private static String tagGuid = "8787878787_09_1";
	private static String resourceGuid = "9898989898_09_1";
	private static Long lastKnownVersion = 10L;
	private static String pluginId = "1";
	private static String Allowed_User_List_For_Tag_Download = "tag.download.auth.users";

	@InjectMocks
	TagREST tagREST = new TagREST();

	@Mock
	TagValidator validator;

	@Mock
	TagDBStore tagStore;

	@Mock
	RESTErrorUtil restErrorUtil;
	
	@Mock
	RangerBizUtil bizUtil;
	
	@Mock
	RangerDaoManager daoManager;
	
	@Mock
	ServiceDBStore svcStore;

	@Mock
	AssetMgr assetMgr;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static String capabilityVector;

	static {
		capabilityVector = Long.toHexString(new RangerPluginCapability().getPluginCapabilities());
	}

	@Test
	public void test1createTagDef() {
		RangerTagDef oldTagDef = null;
		RangerTagDef newTagDef = new RangerTagDef();
		newTagDef.setId(id);
		newTagDef.setName(name);

		try {
			Mockito.when(validator.preCreateTagDef(oldTagDef, false)).thenReturn(null);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.createTagDef(oldTagDef)).thenReturn(newTagDef);
		} catch (Exception e) {
		}
		RangerTagDef rangerTagDef = tagREST.createTagDef(oldTagDef, false);

		Assert.assertEquals(rangerTagDef.getId(), newTagDef.getId());
		Assert.assertNotNull(rangerTagDef);
		Assert.assertEquals(rangerTagDef.getName(), newTagDef.getName());

		try {
			Mockito.verify(validator).preCreateTagDef(oldTagDef, false);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).createTagDef(oldTagDef);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test2createTagDef() {
		RangerTagDef oldTagDef = new RangerTagDef();
		RangerTagDef newTagDef = new RangerTagDef();
		oldTagDef.setId(id);
		newTagDef.setId(id);
		newTagDef.setName(name);

		try {
			Mockito.when(validator.preCreateTagDef(oldTagDef, true)).thenReturn(
					oldTagDef);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.updateTagDef(oldTagDef)).thenReturn(newTagDef);
		} catch (Exception e) {
		}

		RangerTagDef rangerTagDef = tagREST.createTagDef(oldTagDef, true);
		Assert.assertEquals(rangerTagDef.getName(), newTagDef.getName());
		Assert.assertEquals(rangerTagDef.getId(), newTagDef.getId());
		Assert.assertNotEquals(oldTagDef.getName(), rangerTagDef.getName());

		try {
			Mockito.verify(validator).preCreateTagDef(oldTagDef, true);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).updateTagDef(oldTagDef);
		} catch (Exception e) {
		}
	}

	@Test
	public void test3createTagDef() {
		RangerTagDef oldTagDef = new RangerTagDef();
		RangerTagDef newTagDef = new RangerTagDef();
		oldTagDef.setId(null);
		newTagDef.setId(id);
		newTagDef.setName(name);

		try {
			Mockito.when(validator.preCreateTagDef(oldTagDef, true)).thenReturn(oldTagDef);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.updateTagDef(oldTagDef)).thenReturn(newTagDef);
		} catch (Exception e) {
		}

		RangerTagDef rangerTagDef = tagREST.createTagDef(oldTagDef, true);
		
		Assert.assertNotNull(rangerTagDef);
		Assert.assertEquals(rangerTagDef.getId(), newTagDef.getId());
		Assert.assertEquals(rangerTagDef.getName(), newTagDef.getName());
		Assert.assertNotEquals(rangerTagDef.getName(), oldTagDef.getName());
		
		try {
			Mockito.verify(validator).preCreateTagDef(oldTagDef, true);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).updateTagDef(oldTagDef);
		} catch (Exception e) {
		}
	}
	 
	@Test
	public void test4createTagDef() {
		RangerTagDef oldtagDef = new RangerTagDef();
		oldtagDef.setId(id);
		
		try {
			Mockito.when(validator.preCreateTagDef(oldtagDef, false)).thenReturn(
					oldtagDef);
		} catch (Exception e) {
		}
		Mockito.when(
				restErrorUtil.createRESTException(Mockito.anyInt(),
						Mockito.anyString(), Mockito.anyBoolean())).thenThrow(
				new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.createTagDef(oldtagDef, false);

		try {
			Mockito.verify(validator).preCreateTagDef(oldtagDef, false);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),
				Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test5deleteTagDef(){
		try {
			Mockito.doNothing().when(tagStore).deleteTagDef(id);
		} catch (Exception e) {
		}
		tagREST.deleteTagDef(id);
		try {
			Mockito.verify(tagStore).deleteTagDef(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test6deleteTagDefByGuid() {
		RangerTagDef oldTagDef = new RangerTagDef();
		oldTagDef.setId(id);
		oldTagDef.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getTagDefByGuid(oldTagDef.getGuid())).thenReturn(oldTagDef);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteTagDef(oldTagDef.getId());
		} catch (Exception e) {
		}
		
		tagREST.deleteTagDefByGuid(oldTagDef.getGuid());
		Assert.assertNotNull(oldTagDef.getId());
		Assert.assertNotNull(oldTagDef.getGuid());
		
		try {
			Mockito.verify(tagStore).getTagDefByGuid(oldTagDef.getGuid());
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).deleteTagDef(oldTagDef.getId());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test7deleteTagDefByGuid() {
		try {
			Mockito.when(tagStore.getTagDefByGuid(gId)).thenReturn(null);
		} catch (Exception e) {
		}
		tagREST.deleteTagDefByGuid(gId);
		try {
			Mockito.verify(tagStore).getTagDefByGuid(gId);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test8getTagDef() {
		RangerTagDef oldTagDef = new RangerTagDef();
		oldTagDef.setId(id);
		oldTagDef.setName(name);
		
		try {
			Mockito.when(tagStore.getTagDef(id)).thenReturn(oldTagDef);
		} catch (Exception e) {
		}
		
		RangerTagDef rangerTagDef = tagREST.getTagDef(id);
		Assert.assertNotNull(rangerTagDef.getId());
		Assert.assertEquals(rangerTagDef.getId(), oldTagDef.getId());
		Assert.assertEquals(rangerTagDef.getName(), oldTagDef.getName());
		
		try {
			Mockito.verify(tagStore).getTagDef(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test9getTagDef() {
		try {
			Mockito.when(tagStore.getTagDef(id)).thenReturn(null);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.getTagDef(id);
		
		try {
			Mockito.verify(tagStore).getTagDef(id);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),
				Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test10getTagDefByGuid() {
		RangerTagDef oldTagDef = new RangerTagDef();
		oldTagDef.setId(id);
		oldTagDef.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getTagDefByGuid(gId)).thenReturn(oldTagDef);
		} catch (Exception e) {
		}
		
		RangerTagDef rangerTagDef = tagREST.getTagDefByGuid(gId);
		Assert.assertNotNull(oldTagDef.getGuid());
		Assert.assertEquals(rangerTagDef.getGuid(), oldTagDef.getGuid());
		Assert.assertEquals(rangerTagDef.getId(), oldTagDef.getId());
		
		try {
			Mockito.verify(tagStore).getTagDefByGuid(gId);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test11getTagDefByGuid() {
		try {
			Mockito.when(tagStore.getTagDefByGuid(gId)).thenReturn(null);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.getTagDefByGuid(gId);
		
		try {
			Mockito.verify(tagStore).getTagDefByGuid(gId);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),
				Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test12getTagDefByName() {
		RangerTagDef oldTagDef = new RangerTagDef();
		oldTagDef.setId(id);
		oldTagDef.setName(name);
		
		try {
			Mockito.when(tagStore.getTagDefByName(name)).thenReturn(oldTagDef);
		} catch (Exception e) {
		}
		
		RangerTagDef rangerTagDef = tagREST.getTagDefByName(name);
		Assert.assertNotNull(rangerTagDef.getName());
		Assert.assertEquals(rangerTagDef.getName(), oldTagDef.getName());
		Assert.assertEquals(rangerTagDef.getId(), oldTagDef.getId());
		
		try {
			Mockito.verify(tagStore).getTagDefByName(name);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test13getTagDefByName() {
		try {
			Mockito.when(tagStore.getTagDefByName(name)).thenReturn(null);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.getTagDefByName(name);
		
		try {
			Mockito.verify(tagStore).getTagDefByName(name);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),
				Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test14getAllTagDefs() {
		List<RangerTagDef> ret = new ArrayList<RangerTagDef>();
		RangerTagDef rangerTagDef = new RangerTagDef();
		rangerTagDef.setId(id);
		rangerTagDef.setVersion(5L);
		ret.add(rangerTagDef);
		
		try {
			Mockito.when(tagStore.getTagDefs((SearchFilter)Mockito.any())).thenReturn(ret);
		} catch (Exception e) {
		}
		List<RangerTagDef> result = tagREST.getAllTagDefs();
		
		Assert.assertNotNull(result);
		Assert.assertEquals(result.get(0).getId(), ret.get(0).getId());
		Assert.assertEquals(result.get(0).getVersion(), ret.get(0).getVersion());
		
		try {
			Mockito.verify(tagStore).getTagDefs((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test15getAllTagDefs() {
		try {
			Mockito.when(tagStore.getTagDefs((SearchFilter)Mockito.any())).thenReturn(null);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.getAllTagDefs();
		
		try {
			Mockito.verify(tagStore).getTagDefs((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),
				Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test16getTagTypes(){
		List<String> ret = new ArrayList<String>();
		ret.add(name);
		
		try {
			Mockito.when(tagStore.getTagTypes()).thenReturn(ret);
		} catch (Exception e) {
		}
		List<String> result = tagREST.getTagTypes();
		Assert.assertNotNull(result);
		
		try {
			Mockito.verify(tagStore).getTagTypes();
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test17createTag() {
		RangerTag oldTag = null;
		RangerTag newTag = new RangerTag();
		newTag.setId(id);
		newTag.setGuid(gId);
		
		try {
			Mockito.when(validator.preCreateTag(oldTag)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.createTag(oldTag)).thenReturn(newTag);
		} catch (Exception e) {
		}
		RangerTag rangerTag = tagREST.createTag(oldTag, false);
		
		Assert.assertEquals(rangerTag.getId(),newTag.getId());
		Assert.assertEquals(rangerTag.getGuid(), newTag.getGuid());
		
		try {
			Mockito.verify(validator).preCreateTag(oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).createTag(oldTag);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test18createTag(){
		RangerTag oldTag = new RangerTag();
		RangerTag newTag = new RangerTag();
		oldTag.setId(id);
		newTag.setId(id);
		newTag.setVersion(5L);

		try {
			Mockito.when(validator.preCreateTag(oldTag)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(validator).preUpdateTag(oldTag.getId(), oldTag);
		} catch (Exception e1) {
		}
		try {
			Mockito.when(tagStore.updateTag(oldTag)).thenReturn(newTag);
		} catch (Exception e) {
		}
		
		RangerTag rangerTag = tagREST.createTag(oldTag,true);
		Assert.assertEquals(rangerTag.getVersion(), newTag.getVersion());
		Assert.assertNotNull(newTag.getVersion());
		Assert.assertNotEquals(oldTag.getVersion(), newTag.getVersion());
		Assert.assertEquals(oldTag.getId(), newTag.getId());
		
		try {
			Mockito.verify(validator).preCreateTag(oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(validator).preUpdateTag(oldTag.getId(), oldTag);
		} catch (Exception e1) {
		}
		try {
			Mockito.verify(tagStore).updateTag(oldTag);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test19createTag(){
		RangerTag oldTag = new RangerTag();
		oldTag.setId(id);
		
		try {
			Mockito.when(validator.preCreateTag(oldTag)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.createTag(oldTag,false);
		
		try {
			Mockito.verify(validator).preCreateTag(oldTag);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test20updateTagByGuid() {
		RangerTag oldTag = new RangerTag();
		RangerTag newTag = new RangerTag();
		oldTag.setGuid(gId);
		newTag.setGuid(gId);
		newTag.setVersion(5L);
		
		try {
			Mockito.doNothing().when(validator).preUpdateTagByGuid(gId, oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.updateTag(oldTag)).thenReturn(newTag);
		} catch (Exception e) {
		}
				
		RangerTag rangerTag = tagREST.updateTagByGuid(gId, oldTag);
		Assert.assertEquals(oldTag.getGuid(), newTag.getGuid());
		Assert.assertNotEquals(rangerTag.getVersion(), oldTag.getVersion());
		Assert.assertEquals(rangerTag.getVersion(), newTag.getVersion());
		
		try {
			Mockito.verify(validator).preUpdateTagByGuid(gId, oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).updateTag(oldTag);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test21deleteTag() {
		RangerTag oldTag = new RangerTag();
		oldTag.setId(id);
		
		try {
			Mockito.when(validator.preDeleteTag(id)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteTag(id);
		} catch (Exception e) {
		}
		
		tagREST.deleteTag(id);
		Assert.assertNotNull(oldTag.getId());
		
		try {
			Mockito.verify(validator).preDeleteTag(id);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).deleteTag(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test22deleteTagByGuid() {
		RangerTag oldTag = new RangerTag();
		oldTag.setId(id);
		oldTag.setGuid(gId);
		
		try {
			Mockito.when(validator.preDeleteTagByGuid(gId)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteTag(oldTag.getId());
		} catch (Exception e) {
		}
		
		tagREST.deleteTagByGuid(gId);
		Assert.assertNotNull(oldTag.getId());
		Assert.assertNotNull(oldTag.getGuid());
		
		try {
			Mockito.verify(validator).preDeleteTagByGuid(gId);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).deleteTag(oldTag.getId());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test23getTag() {
		RangerTag oldTag = new RangerTag();
		oldTag.setId(id);
		oldTag.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getTag(id)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		RangerTag rangerTag = tagREST.getTag(id);
		Assert.assertNotNull(oldTag.getId());
		Assert.assertEquals(rangerTag.getId(), oldTag.getId());
		Assert.assertEquals(rangerTag.getGuid(), oldTag.getGuid());
		
		try {
			Mockito.verify(tagStore).getTag(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test24getTagByGuid() {
		RangerTag oldTag = new RangerTag();
		oldTag.setId(id);
		oldTag.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getTagByGuid(gId)).thenReturn(oldTag);
		} catch (Exception e) {
		}
		RangerTag rangerTag = tagREST.getTagByGuid(gId);
		Assert.assertNotNull(oldTag.getGuid());
		Assert.assertEquals(rangerTag.getGuid(), oldTag.getGuid());
		Assert.assertEquals(rangerTag.getId(), oldTag.getId());
		Assert.assertNotNull(rangerTag.getId());
		
		try {
			Mockito.verify(tagStore).getTagByGuid(gId);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test25getTagsByType() {
		String type = "file";
		List<RangerTag> tag = new ArrayList<RangerTag>();
		RangerTag rTag = new RangerTag();
		rTag.setType(type);
		tag.add(rTag);
		
		try {
			Mockito.when(tagStore.getTagsByType(type)).thenReturn(tag);
		} catch (Exception e) {
		}
		List<RangerTag> rangerTag = tagREST.getTagsByType(type);
		Assert.assertEquals(rangerTag.get(0).getType(), tag.get(0).getType());
		
		try {
			Mockito.verify(tagStore).getTagsByType(type);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test26getAllTags() {
		List<RangerTag> ret = new ArrayList<RangerTag>();
		RangerTag rangerTag = new RangerTag();
		rangerTag.setId(id);
		rangerTag.setGuid(gId);
		ret.add(rangerTag);
		
		try {
			Mockito.when(tagStore.getTags((SearchFilter)Mockito.any())).thenReturn(ret);
		} catch (Exception e) {
		}
		
		List<RangerTag> result = tagREST.getAllTags();
		Assert.assertEquals(result.get(0).getId(), ret.get(0).getId());
		Assert.assertEquals(result.get(0).getVersion(), ret.get(0).getVersion());
		Assert.assertNotNull(result.get(0).getId());
		
		try {
			Mockito.verify(tagStore).getTags((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test60getAllTags() {
		List<RangerTag> ret = new ArrayList<RangerTag>();
		
		try {
			Mockito.when(tagStore.getTags((SearchFilter)Mockito.any())).thenReturn(ret);
		} catch (Exception e) {
		}
		
		List<RangerTag> result = tagREST.getAllTags();
		Assert.assertNotNull(result);
		
		try {
			Mockito.verify(tagStore).getTags((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test27createServiceResource() {
		RangerServiceResource oldRSR = null;
		RangerServiceResource newRSR = new RangerServiceResource();
		newRSR.setId(id);
		newRSR.setGuid(gId);
		
		try {
			Mockito.when(validator.preCreateServiceResource(oldRSR)).thenReturn(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.createServiceResource(oldRSR)).thenReturn(newRSR);
		} catch (Exception e) {
		}
		
		RangerServiceResource rangerServiceResource = tagREST.createServiceResource(oldRSR, false);
		Assert.assertNotNull(rangerServiceResource.getId());
		Assert.assertEquals(rangerServiceResource.getId(), newRSR.getId());
		Assert.assertEquals(rangerServiceResource.getGuid(), newRSR.getGuid());
		
		try {
			Mockito.verify(validator).preCreateServiceResource(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).createServiceResource(oldRSR);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test28createServiceResource() {
		RangerServiceResource oldRSR = new RangerServiceResource();
		RangerServiceResource newRSR = new RangerServiceResource();
		oldRSR.setId(id);
		newRSR.setId(id);
		newRSR.setVersion(5L);
		
		try {
			Mockito.when(validator.preCreateServiceResource(oldRSR)).thenReturn(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(validator).preUpdateServiceResource(oldRSR.getId(), oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.updateServiceResource(oldRSR)).thenReturn(newRSR);
		} catch (Exception e) {
		}
		
		RangerServiceResource rangerServiceResource = tagREST.createServiceResource(oldRSR, true);
		Assert.assertNotEquals(oldRSR.getVersion(), newRSR.getVersion());
		Assert.assertEquals(rangerServiceResource.getId(), newRSR.getId());
		Assert.assertEquals(rangerServiceResource.getId(), oldRSR.getId());
		
		try {
			Mockito.verify(validator).preCreateServiceResource(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(validator).preUpdateServiceResource(oldRSR.getId(), oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).updateServiceResource(oldRSR);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test29createServiceResource(){
		RangerServiceResource oldRSR = new RangerServiceResource();
		
		try {
			Mockito.when(validator.preCreateServiceResource(oldRSR)).thenReturn(oldRSR);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.createServiceResource(oldRSR, false);
		
		try {
			Mockito.verify(validator).preCreateServiceResource(oldRSR);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test30updateServiceResourceByGuid() {
		RangerServiceResource oldSRS = new RangerServiceResource();
		RangerServiceResource newSRS = new RangerServiceResource();
		oldSRS.setId(id);
		oldSRS.setGuid(gId);
		newSRS.setId(id);
		newSRS.setGuid(gId);
		newSRS.setVersion(5L);
		
		try {
			Mockito.doNothing().when(validator).preUpdateServiceResourceByGuid(gId, oldSRS);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.updateServiceResource(oldSRS)).thenReturn(newSRS);
		} catch (Exception e) {
		}
		
		RangerServiceResource rangerServiceResource = tagREST.updateServiceResourceByGuid(gId, oldSRS);
		Assert.assertEquals(oldSRS.getId(), newSRS.getId());
		Assert.assertEquals(oldSRS.getGuid(), newSRS.getGuid());
		Assert.assertNotEquals(oldSRS.getVersion(), newSRS.getVersion());
		Assert.assertEquals(rangerServiceResource.getVersion(), newSRS.getVersion());
		
		try {
			Mockito.verify(validator).preUpdateServiceResourceByGuid(gId, oldSRS);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).updateServiceResource(oldSRS);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test31deleteServiceResource() {
		RangerServiceResource oldSRS = new RangerServiceResource();
		oldSRS.setId(id);
		
		try {
			Mockito.when(validator.preDeleteServiceResource(id)).thenReturn(oldSRS);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteServiceResource(id);
		} catch (Exception e) {
		}
		
		tagREST.deleteServiceResource(id);
		Assert.assertNotNull(oldSRS.getId());
		
		try {
			Mockito.verify(validator).preDeleteServiceResource(id);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).deleteServiceResource(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test32getServiceResource() {
		RangerServiceResource oldSRS = new RangerServiceResource();
		oldSRS.setId(id);
		oldSRS.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getServiceResource(id)).thenReturn(oldSRS);
		} catch (Exception e) {
		}
		RangerServiceResource rangerServiceResource = tagREST.getServiceResource(id);
		
		Assert.assertNotNull(rangerServiceResource);
		Assert.assertEquals(rangerServiceResource.getId(), oldSRS.getId());
		Assert.assertEquals(rangerServiceResource.getGuid(), oldSRS.getGuid());
		try {
			Mockito.verify(tagStore).getServiceResource(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test33getServiceResourceByGuid() {
		RangerServiceResource oldSRS = new RangerServiceResource();
		oldSRS.setId(id);
		oldSRS.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getServiceResourceByGuid(gId)).thenReturn(oldSRS);
		} catch (Exception e) {
		}
		RangerServiceResource rangerServiceResource = tagREST.getServiceResourceByGuid(gId);
		
		Assert.assertNotNull(rangerServiceResource);
		Assert.assertEquals(rangerServiceResource.getGuid(), oldSRS.getGuid());
		Assert.assertEquals(rangerServiceResource.getId(), oldSRS.getId());
		try {
			Mockito.verify(tagStore).getServiceResourceByGuid(gId);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test34getServiceResourcesByService() {
		List<RangerServiceResource> ret = new ArrayList<RangerServiceResource>();
		RangerServiceResource rangerServiceResource = new RangerServiceResource();
		rangerServiceResource.setId(id);
		rangerServiceResource.setServiceName(serviceName);
		ret.add(rangerServiceResource);
		
		try {
			Mockito.when(tagStore.getServiceResourcesByService(serviceName)).thenReturn(ret);
		} catch (Exception e) {
		}
		
		List<RangerServiceResource> reslut = tagREST.getServiceResourcesByService(serviceName);
		Assert.assertNotNull(reslut.get(0).getId());
		Assert.assertEquals(reslut.get(0).getId(), ret.get(0).getId());
		Assert.assertEquals(reslut.get(0).getServiceName(), ret.get(0).getServiceName());
	
		try {
			Mockito.verify(tagStore).getServiceResourcesByService(serviceName);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test35getServiceResourcesByService() {
		List<RangerServiceResource> oldSRS = new ArrayList<RangerServiceResource>();
		RangerServiceResource rangerServiceResource = new RangerServiceResource();
		rangerServiceResource.setId(id);
		rangerServiceResource.setServiceName(serviceName);
		oldSRS.add(rangerServiceResource);
		
		try {
			Mockito.when(tagStore.getServiceResourcesByService(serviceName)).thenReturn(oldSRS);
		} catch (Exception e) {
		}
		
		List<RangerServiceResource> result = tagREST.getServiceResourcesByService(serviceName);
		Assert.assertNotNull(result);
		Assert.assertEquals(result.size(), 1);
		Assert.assertEquals(result.get(0).getId(), id);
		Assert.assertEquals(result.get(0).getServiceName(), serviceName);
	
		try {
			Mockito.verify(tagStore).getServiceResourcesByService(serviceName);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test59getServiceResourcesByService() {
		List<RangerServiceResource> oldSRS = new ArrayList<RangerServiceResource>();
		
		try {
			Mockito.when(tagStore.getServiceResourcesByService(serviceName)).thenReturn(oldSRS);
		} catch (Exception e) {
		}
		
		List<RangerServiceResource> result = tagREST.getServiceResourcesByService(serviceName);
		Assert.assertNotNull(result);
	
		try {
			Mockito.verify(tagStore).getServiceResourcesByService(serviceName);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test36getServiceResourceByServiceAndResourceSignature() {
		RangerServiceResource oldSRS = new RangerServiceResource();
		oldSRS.setId(id);
		oldSRS.setResourceSignature(resourceSignature);
		oldSRS.setServiceName(serviceName);
		
		try {
			Mockito.when(tagStore.getServiceResourceByServiceAndResourceSignature(serviceName, resourceSignature)).thenReturn(oldSRS);
		} catch (Exception e) {
		}
		
		RangerServiceResource rangerServiceResource = tagREST.getServiceResourceByServiceAndResourceSignature(serviceName, resourceSignature);
		Assert.assertEquals(rangerServiceResource.getId(), oldSRS.getId());
		Assert.assertEquals(rangerServiceResource.getServiceName(), oldSRS.getServiceName());
		Assert.assertEquals(rangerServiceResource.getResourceSignature(), oldSRS.getResourceSignature());
		
		try {
			Mockito.verify(tagStore).getServiceResourceByServiceAndResourceSignature(serviceName, resourceSignature);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test37getAllServiceResources() {
		List<RangerServiceResource> ret = new ArrayList<RangerServiceResource>();
		RangerServiceResource rangerServiceResource =  new RangerServiceResource();
		rangerServiceResource.setId(id);
		rangerServiceResource.setServiceName(serviceName);
		ret.add(rangerServiceResource);
		
		try {
			Mockito.when(tagStore.getServiceResources((SearchFilter)Mockito.any())).thenReturn(ret);
		} catch (Exception e) {
		}
		List<RangerServiceResource> result = tagREST.getAllServiceResources();
		Assert.assertNotNull(result.get(0).getId());
		Assert.assertEquals(result.get(0).getId(), ret.get(0).getId());
		Assert.assertEquals(result.get(0).getServiceName(), ret.get(0).getServiceName());
		
		try {
			Mockito.verify(tagStore).getServiceResources((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test38createTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = null;
		RangerTagResourceMap newTagResourceMap = new RangerTagResourceMap();
		newTagResourceMap.setTagId(id);
		newTagResourceMap.setResourceId(id);
		
		try {
			Mockito.when(tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		try {
			Mockito.when(validator.preCreateTagResourceMap(tagGuid, resourceGuid)).thenReturn(newTagResourceMap);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.createTagResourceMap(newTagResourceMap)).thenReturn(newTagResourceMap);
		} catch (Exception e) {
		}
		
		RangerTagResourceMap rangerTagResourceMap = tagREST.createTagResourceMap(tagGuid, resourceGuid, false);
		Assert.assertEquals(rangerTagResourceMap.getTagId(), newTagResourceMap.getTagId());
		Assert.assertEquals(rangerTagResourceMap.getResourceId(), newTagResourceMap.getResourceId());
		
		try {
			Mockito.verify(tagStore).getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(validator).preCreateTagResourceMap(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).createTagResourceMap(newTagResourceMap);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test39createTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		
		try {
			Mockito.when(tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(),Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		tagREST.createTagResourceMap(tagGuid, resourceGuid, false);
		
		try {
			Mockito.verify(tagStore).getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test40createTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = null;
		RangerTagResourceMap newTagResourceMap = new RangerTagResourceMap();
		newTagResourceMap.setId(id);
		newTagResourceMap.setGuid(gId);
		RangerTagResourceMap finalTagResourceMap = new RangerTagResourceMap();
		finalTagResourceMap.setId(id);
		finalTagResourceMap.setGuid(gId);
		finalTagResourceMap.setVersion(5L);
		
		try {
			Mockito.when(tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		try {
			Mockito.when(validator.preCreateTagResourceMap(tagGuid, resourceGuid)).thenReturn(newTagResourceMap);
		} catch (Exception e1) {
		}
		try {
			Mockito.when(tagStore.createTagResourceMap(newTagResourceMap)).thenReturn(finalTagResourceMap);
		} catch (Exception e1) {
		}
		RangerTagResourceMap result = tagREST.createTagResourceMap(tagGuid, resourceGuid, true);
		Assert.assertNotNull(result.getId());
		Assert.assertEquals(result.getGuid(), finalTagResourceMap.getGuid());
		Assert.assertEquals(result.getId(), finalTagResourceMap.getId());
		Assert.assertEquals(result.getVersion(), finalTagResourceMap.getVersion());
		
		try {
			Mockito.verify(tagStore).getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(validator).preCreateTagResourceMap(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).createTagResourceMap(newTagResourceMap);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test41deleteTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		oldTagResourceMap.setId(id);
		try {
			Mockito.when(validator.preDeleteTagResourceMap(id)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteTagResourceMap(id);
		} catch (Exception e) {
		}
		
		tagREST.deleteTagResourceMap(id);
		Assert.assertNotNull(oldTagResourceMap.getId());
		try {
			Mockito.verify(validator).preDeleteTagResourceMap(id);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).deleteTagResourceMap(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test42deleteTagResourceMapByGuid() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		oldTagResourceMap.setId(id);
		oldTagResourceMap.setGuid(gId);
		try {
			Mockito.when(validator.preDeleteTagResourceMapByGuid(gId)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteServiceResource(oldTagResourceMap.getId());
		} catch (Exception e) {
		}
		
		tagREST.deleteTagResourceMapByGuid(gId);
		Assert.assertNotNull(oldTagResourceMap.getId());
		Assert.assertNotNull(oldTagResourceMap.getGuid());
		
		try {
			Mockito.verify(validator).preDeleteTagResourceMapByGuid(gId);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).deleteServiceResource(oldTagResourceMap.getId());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test43deleteTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		oldTagResourceMap.setId(id);
		
		try {
			Mockito.when(validator.preDeleteTagResourceMap(tagGuid, resourceGuid)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteTagResourceMap(oldTagResourceMap.getId());
		} catch (Exception e) {
		}
		
		tagREST.deleteTagResourceMap(tagGuid, resourceGuid);
		try {
			Mockito.verify(validator).preDeleteTagResourceMap(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test44getTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		oldTagResourceMap.setId(id);
		oldTagResourceMap.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getTagResourceMap(id)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		
		RangerTagResourceMap rangerTagResourceMap = tagREST.getTagResourceMap(id);
		Assert.assertNotNull(rangerTagResourceMap.getId());
		Assert.assertEquals(rangerTagResourceMap.getId(), oldTagResourceMap.getId());
		Assert.assertEquals(rangerTagResourceMap.getGuid(), oldTagResourceMap.getGuid());
		try {
			Mockito.verify(tagStore).getTagResourceMap(id);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test45getTagResourceMapByGuid() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		oldTagResourceMap.setId(id);
		oldTagResourceMap.setGuid(gId);
		
		try {
			Mockito.when(tagStore.getTagResourceMapByGuid(gId)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		
		RangerTagResourceMap rangerTagResourceMap = tagREST.getTagResourceMapByGuid(gId);
		Assert.assertNotNull(rangerTagResourceMap.getId());
		Assert.assertEquals(rangerTagResourceMap.getId(), oldTagResourceMap.getId());
		Assert.assertEquals(rangerTagResourceMap.getGuid(), oldTagResourceMap.getGuid());
		try {
			Mockito.verify(tagStore).getTagResourceMapByGuid(gId);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test46getTagResourceMap() {
		RangerTagResourceMap oldTagResourceMap = new RangerTagResourceMap();
		oldTagResourceMap.setId(id);
		oldTagResourceMap.setTagId(id);
		
		try {
			Mockito.when(tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid)).thenReturn(oldTagResourceMap);
		} catch (Exception e) {
		}
		RangerTagResourceMap rangerTagResourceMap = tagREST.getTagResourceMap(tagGuid, resourceGuid);
		Assert.assertNotNull(rangerTagResourceMap.getId());
		Assert.assertEquals(rangerTagResourceMap.getId(), oldTagResourceMap.getId());
		Assert.assertEquals(rangerTagResourceMap.getTagId(), oldTagResourceMap.getTagId());
		try {
			Mockito.verify(tagStore).getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test47getAllTagResourceMaps() {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();
		RangerTagResourceMap rangerTagResourceMap = new RangerTagResourceMap();
		rangerTagResourceMap.setId(id);
		rangerTagResourceMap.setTagId(id);
		ret.add(rangerTagResourceMap);
		
		try {
			Mockito.when(tagStore.getTagResourceMaps((SearchFilter)Mockito.any())).thenReturn(ret);
		} catch (Exception e) {
		}
		
		List<RangerTagResourceMap> result = tagREST.getAllTagResourceMaps();
		Assert.assertNotNull(result.get(0).getId());
		Assert.assertEquals(result.get(0).getId(), ret.get(0).getId());
		Assert.assertEquals(result.get(0).getTagId(), ret.get(0).getTagId());
		
		try {
			Mockito.verify(tagStore).getTagResourceMaps((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test58getAllTagResourceMaps() {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();
		
		try {
			Mockito.when(tagStore.getTagResourceMaps((SearchFilter)Mockito.any())).thenReturn(ret);
		} catch (Exception e) {
		}
		
		List<RangerTagResourceMap> result = tagREST.getAllTagResourceMaps();
		Assert.assertNotNull(result);
		
		try {
			Mockito.verify(tagStore).getTagResourceMaps((SearchFilter)Mockito.any());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test48deleteServiceResourceByGuid() {
		RangerServiceResource oldRSR = new RangerServiceResource();
		oldRSR.setId(id);
		oldRSR.setGuid(gId);
		List<RangerTagResourceMap> tagResourceMaps = new ArrayList<RangerTagResourceMap>();
		RangerTagResourceMap rangerTagResourceMap = new RangerTagResourceMap();
		rangerTagResourceMap.setId(id);
		rangerTagResourceMap.setTagId(id);
		tagResourceMaps.add(rangerTagResourceMap);
		
		try {
			Mockito.when(validator.preDeleteServiceResourceByGuid(gId, true)).thenReturn(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.getTagResourceMapsForResourceGuid(oldRSR.getGuid())).thenReturn(tagResourceMaps);
		} catch (Exception e) {
		}
		tagREST.deleteServiceResourceByGuid(gId, true);
		
		try {
			Mockito.verify(validator).preDeleteServiceResourceByGuid(gId, true);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).getTagResourceMapsForResourceGuid(oldRSR.getGuid());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test49deleteServiceResourceByGuid() {
		RangerServiceResource oldRSR = new RangerServiceResource();
		oldRSR.setId(id);
		oldRSR.setGuid(gId);
		
		try {
			Mockito.when(validator.preDeleteServiceResourceByGuid(gId, false)).thenReturn(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.doNothing().when(tagStore).deleteServiceResource(oldRSR.getId());
		} catch (Exception e) {
		}
		
		tagREST.deleteServiceResourceByGuid(gId, false);
		
		try {
			Mockito.verify(validator).preDeleteServiceResourceByGuid(gId, false);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test61deleteServiceResourceByGuid() {
		RangerServiceResource oldRSR = new RangerServiceResource();
		oldRSR.setId(id);
		oldRSR.setGuid(gId);
		List<RangerTagResourceMap> tagResourceMaps = new ArrayList<RangerTagResourceMap>();
		
		try {
			Mockito.when(validator.preDeleteServiceResourceByGuid(gId, true)).thenReturn(oldRSR);
		} catch (Exception e) {
		}
		try {
			Mockito.when(tagStore.getTagResourceMapsForResourceGuid(oldRSR.getGuid())).thenReturn(tagResourceMaps);
		} catch (Exception e) {
		}
		tagREST.deleteServiceResourceByGuid(gId, true);
		
		try {
			Mockito.verify(validator).preDeleteServiceResourceByGuid(gId, true);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).getTagResourceMapsForResourceGuid(oldRSR.getGuid());
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test50getServiceTagsIfUpdated() {
		ServiceTags oldServiceTag = null;
		
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(),Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		tagREST.getServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, true);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(),Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test51getServiceTagsIfUpdated() {
		ServiceTags oldServiceTag = new ServiceTags();
		oldServiceTag.setServiceName(serviceName);
		oldServiceTag.setTagVersion(5L);
		
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		ServiceTags serviceTags = tagREST.getServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		Assert.assertEquals(serviceTags.getServiceName(), oldServiceTag.getServiceName());
		Assert.assertEquals(serviceTags.getTagVersion(), oldServiceTag.getTagVersion());
		
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, true);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test52getSecureServiceTagsIfUpdatedIsKeyAdminTrue() {
		boolean isAdmin = false;
		boolean isKeyAdmin = true;
		ServiceTags oldServiceTag = new ServiceTags();
		oldServiceTag.setServiceName(serviceName);
		oldServiceTag.setTagVersion(5L);
		
		XXService xService = new XXService();
		xService.setId(id);
		xService.setName(serviceName);
		xService.setType(5L);
		
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(id);
		xServiceDef.setVersion(5L);
		xServiceDef.setImplclassname("org.apache.ranger.services.kms.RangerServiceKMS");
		
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setName(serviceName);
		
		XXServiceDao xXServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceDefDao xXServiceDefDao  = Mockito.mock(XXServiceDefDao.class);
		
		Mockito.when(bizUtil.isAdmin()).thenReturn(isAdmin);
		Mockito.when(bizUtil.isKeyAdmin()).thenReturn(isKeyAdmin);

		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		Mockito.when(xXServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xXServiceDefDao);
		Mockito.when(xXServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);
		try {
			Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
		} catch (Exception e) {
		}
		
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		
		ServiceTags result = tagREST.getSecureServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		Assert.assertNotNull(result.getServiceName());
		Assert.assertEquals(result.getServiceName(), oldServiceTag.getServiceName());
		Assert.assertEquals(result.getTagVersion(), oldServiceTag.getTagVersion());
		
		Mockito.verify(bizUtil).isAdmin();
		Mockito.verify(bizUtil).isKeyAdmin();
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(xXServiceDao).findByName(serviceName);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(xXServiceDefDao).getById(xService.getType());
		try {
			Mockito.verify(svcStore).getServiceByName(serviceName);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, true);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test53getSecureServiceTagsIfUpdatedIsAdminTrue() {
		boolean isAdmin = true;
		boolean isKeyAdmin = false;
		ServiceTags oldServiceTag = new ServiceTags();
		oldServiceTag.setServiceName(serviceName);
		oldServiceTag.setTagVersion(5L);
		
		XXService xService = new XXService();
		xService.setId(id);
		xService.setName(serviceName);
		xService.setType(5L);
		
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(id);
		xServiceDef.setVersion(5L);
		
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setName(serviceName);
		
		XXServiceDao xXServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceDefDao xXServiceDefDao  = Mockito.mock(XXServiceDefDao.class);
		
		Mockito.when(bizUtil.isAdmin()).thenReturn(isAdmin);
		Mockito.when(bizUtil.isKeyAdmin()).thenReturn(isKeyAdmin);

		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		Mockito.when(xXServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xXServiceDefDao);
		Mockito.when(xXServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);
		try {
			Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
		} catch (Exception e) {
		}
		
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		
		ServiceTags result = tagREST.getSecureServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		Assert.assertNotNull(result.getServiceName());
		Assert.assertEquals(result.getServiceName(), oldServiceTag.getServiceName());
		Assert.assertEquals(result.getTagVersion(), oldServiceTag.getTagVersion());
		
		Mockito.verify(bizUtil).isAdmin();
		Mockito.verify(bizUtil).isKeyAdmin();
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(xXServiceDao).findByName(serviceName);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(xXServiceDefDao).getById(xService.getType());
		try {
			Mockito.verify(svcStore).getServiceByName(serviceName);
		} catch (Exception e) {
		}
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, true);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test54getSecureServiceTagsIfUpdatedIsKeyAdminFalse() {
		boolean isAdmin = false;
		boolean isKeyAdmin = false;
		boolean isAllowed = true;
		ServiceTags oldServiceTag = new ServiceTags();
		oldServiceTag.setServiceName(serviceName);
		oldServiceTag.setTagVersion(5L);
		
		XXService xService = new XXService();
		xService.setId(id);
		xService.setName(serviceName);
		xService.setType(5L);
		
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(id);
		xServiceDef.setVersion(5L);
		xServiceDef.setImplclassname("org.apache.ranger.services.kms.RangerServiceKMS");
		
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setName(serviceName);
		
		XXServiceDao xXServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceDefDao xXServiceDefDao  = Mockito.mock(XXServiceDefDao.class);
		
		Mockito.when(bizUtil.isAdmin()).thenReturn(isAdmin);
		Mockito.when(bizUtil.isKeyAdmin()).thenReturn(isKeyAdmin);

		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		Mockito.when(xXServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xXServiceDefDao);
		Mockito.when(xXServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);
		try {
			Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
		} catch (Exception e) {
		}
		
		Mockito.when(bizUtil.isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download)).thenReturn(isAllowed);
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		
		ServiceTags result = tagREST.getSecureServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		Assert.assertNotNull(result.getServiceName());
		Assert.assertEquals(result.getServiceName(), oldServiceTag.getServiceName());
		Assert.assertEquals(result.getTagVersion(), oldServiceTag.getTagVersion());
		
		Mockito.verify(bizUtil).isAdmin();
		Mockito.verify(bizUtil).isKeyAdmin();
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(xXServiceDao).findByName(serviceName);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(xXServiceDefDao).getById(xService.getType());
		try {
			Mockito.verify(svcStore).getServiceByName(serviceName);
		} catch (Exception e) {
		}
		Mockito.verify(bizUtil).isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download);
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, true);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test55getSecureServiceTagsIfUpdatedIsAdminFalse() {
		boolean isAdmin = false;
		boolean isKeyAdmin = false;
		boolean isAllowed = true;
		ServiceTags oldServiceTag = new ServiceTags();
		oldServiceTag.setServiceName(serviceName);
		oldServiceTag.setTagVersion(5L);
		
		XXService xService = new XXService();
		xService.setId(id);
		xService.setName(serviceName);
		xService.setType(5L);
		
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(id);
		xServiceDef.setVersion(5L);
		
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setName(serviceName);
		
		XXServiceDao xXServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceDefDao xXServiceDefDao  = Mockito.mock(XXServiceDefDao.class);
		
		Mockito.when(bizUtil.isAdmin()).thenReturn(isAdmin);
		Mockito.when(bizUtil.isKeyAdmin()).thenReturn(isKeyAdmin);

		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		Mockito.when(xXServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xXServiceDefDao);
		Mockito.when(xXServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);
		try {
			Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
		} catch (Exception e) {
		}
		
		Mockito.when(bizUtil.isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download)).thenReturn(isAllowed);
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		
		ServiceTags result = tagREST.getSecureServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		Assert.assertNotNull(result.getServiceName());
		Assert.assertEquals(result.getServiceName(), oldServiceTag.getServiceName());
		Assert.assertEquals(result.getTagVersion(), oldServiceTag.getTagVersion());
		
		Mockito.verify(bizUtil).isAdmin();
		Mockito.verify(bizUtil).isKeyAdmin();
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(xXServiceDao).findByName(serviceName);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(xXServiceDefDao).getById(xService.getType());
		try {
			Mockito.verify(svcStore).getServiceByName(serviceName);
		} catch (Exception e) {
		}
		Mockito.verify(bizUtil).isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download);
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, true);
		} catch (Exception e) {
		}
	}
	
	@Test
	public void test56getSecureServiceTagsIfUpdatedIsAllowedFalse() {
		boolean isAdmin = false;
		boolean isKeyAdmin = false;
		boolean isAllowed = false;
		ServiceTags oldServiceTag = new ServiceTags();
		oldServiceTag.setServiceName(serviceName);
		oldServiceTag.setTagVersion(5L);
		
		XXService xService = new XXService();
		xService.setId(id);
		xService.setName(serviceName);
		xService.setType(5L);
		
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(id);
		xServiceDef.setVersion(5L);
		
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setName(serviceName);
		
		XXServiceDao xXServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceDefDao xXServiceDefDao  = Mockito.mock(XXServiceDefDao.class);
		
		Mockito.when(bizUtil.isAdmin()).thenReturn(isAdmin);
		Mockito.when(bizUtil.isKeyAdmin()).thenReturn(isKeyAdmin);

		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		Mockito.when(xXServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xXServiceDefDao);
		Mockito.when(xXServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);
		try {
			Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
		} catch (Exception e) {
		}
		
		Mockito.when(bizUtil.isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download)).thenReturn(isAllowed);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		tagREST.getSecureServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		
		Mockito.verify(bizUtil).isAdmin();
		Mockito.verify(bizUtil).isKeyAdmin();
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(xXServiceDao).findByName(serviceName);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(xXServiceDefDao).getById(xService.getType());
		try {
			Mockito.verify(svcStore).getServiceByName(serviceName);
		} catch (Exception e) {
		}
		Mockito.verify(bizUtil).isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download);
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean());
	}
	
	@Test
	public void test57getSecureServiceTagsIfUpdated() {
		boolean isAdmin = false;
		boolean isKeyAdmin = false;
		boolean isAllowed = true;
		ServiceTags oldServiceTag = null;
		
		XXService xService = new XXService();
		xService.setId(id);
		xService.setName(serviceName);
		xService.setType(5L);
		
		XXServiceDef xServiceDef = new XXServiceDef();
		xServiceDef.setId(id);
		xServiceDef.setVersion(5L);
		
		RangerService rangerService = new RangerService();
		rangerService.setId(id);
		rangerService.setName(serviceName);
		
		XXServiceDao xXServiceDao = Mockito.mock(XXServiceDao.class);
		XXServiceDefDao xXServiceDefDao  = Mockito.mock(XXServiceDefDao.class);
		
		Mockito.when(bizUtil.isAdmin()).thenReturn(isAdmin);
		Mockito.when(bizUtil.isKeyAdmin()).thenReturn(isKeyAdmin);

		Mockito.when(daoManager.getXXService()).thenReturn(xXServiceDao);
		Mockito.when(xXServiceDao.findByName(serviceName)).thenReturn(xService);
		Mockito.when(daoManager.getXXServiceDef()).thenReturn(xXServiceDefDao);
		Mockito.when(xXServiceDefDao.getById(xService.getType())).thenReturn(xServiceDef);
		try {
			Mockito.when(svcStore.getServiceByName(serviceName)).thenReturn(rangerService);
		} catch (Exception e) {
		}
		
		Mockito.when(bizUtil.isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download)).thenReturn(isAllowed);
		try {
			Mockito.when(tagStore.getServiceTagsIfUpdated(serviceName, lastKnownVersion, true)).thenReturn(oldServiceTag);
		} catch (Exception e) {
		}
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		tagREST.getSecureServiceTagsIfUpdated(serviceName, lastKnownVersion, 0L, pluginId, false, capabilityVector, null);
		
		Mockito.verify(bizUtil).isAdmin();
		Mockito.verify(bizUtil).isKeyAdmin();
		Mockito.verify(daoManager).getXXService();
		Mockito.verify(xXServiceDao).findByName(serviceName);
		Mockito.verify(daoManager).getXXServiceDef();
		Mockito.verify(xXServiceDefDao).getById(xService.getType());
		try {
			Mockito.verify(svcStore).getServiceByName(serviceName);
		} catch (Exception e) {
		}
		Mockito.verify(bizUtil).isUserAllowed(rangerService, Allowed_User_List_For_Tag_Download);
		try {
			Mockito.verify(tagStore).getServiceTagsIfUpdated(serviceName, lastKnownVersion, false);
		} catch (Exception e) {
		}
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyInt(), Mockito.anyString(), Mockito.anyBoolean());
	}
}
