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
package org.apache.ranger.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.store.RangerServiceResourceSignature;
import org.apache.ranger.plugin.store.TagStore;
import org.apache.ranger.plugin.util.ServiceTags;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class TestServiceTagProcessor {

	@InjectMocks
	ServiceTagsProcessor sTagProcessor = new ServiceTagsProcessor(null);

	@Mock
	ServiceTags serviceTags;

	@Mock
	RangerValidatorFactory validatorFactory;

	@Mock
	TestServiceREST testServiceRest;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void test1processError() throws Exception {
		ServiceTagsProcessor sTagProcessor = new ServiceTagsProcessor(null);
		sTagProcessor.process(serviceTags);
		Assert.assertNull(serviceTags);
	}

	@Test
	public void test2processAddOrUpdate() throws Exception {
		serviceTags = new ServiceTags();
		Map<Long, RangerTagDef> fd = new HashMap<>();
		List<RangerTag> associatedTags = new ArrayList<>();
		RangerTagDef rTagDef = Mockito.mock(RangerTagDef.class);
		rTagDef.setGuid("guid");
		rTagDef.setName("rTagDefname");
		fd.put(1l, rTagDef);
		serviceTags.setTagDefinitions(fd);
		List<RangerServiceResource> serviceResources = new ArrayList<RangerServiceResource>();
		RangerServiceResource rserRes = new RangerServiceResource();
		testServiceRest = new TestServiceREST();
		RangerPolicy rp = testServiceRest.rangerPolicy();
		rserRes.setResourceElements(rp.getResources());
		rserRes.setGuid("guId");
		rserRes.setId(1L);
		serviceResources.add(rserRes);
		serviceTags.setServiceResources(serviceResources);
		Map<Long, List<Long>> resourceToTagIds = new HashMap<>();
		resourceToTagIds.put(1L, new ArrayList<Long>(Arrays.asList(11L, 1L, 13L)));
		serviceTags.setResourceToTagIds(resourceToTagIds);

		RangerTag rTag = new RangerTag();
		rTag.setId(22L);
		rTag.setOwner((short) 1);
		Map<Long, RangerTag> tags = new HashMap<>();
		tags.put(1L, rTag);
		serviceTags.setTags(tags);

		RangerTag rTag2 = new RangerTag();
		rTag2.setId(22L);
		rTag2.setOwner((short) 1);
		Map<Long, RangerTag> tags2 = new HashMap<>();
		tags2.put(1L, rTag2);
		associatedTags.add(rTag2);

		TagStore tagStore = Mockito.mock(TagStore.class);
		sTagProcessor = new ServiceTagsProcessor(tagStore);
		Mockito.when(tagStore.createTagDef(rTagDef)).thenReturn(rTagDef);
		Mockito.when(tagStore.getServiceResourceByGuid(rserRes.getGuid())).thenReturn(rserRes);
		Mockito.when(tagStore.createTag(rTag2)).thenReturn(rTag);

		Mockito.when(tagStore.getTagsForResourceId(rserRes.getId())).thenReturn(associatedTags);
		sTagProcessor.process(serviceTags);
		Mockito.verify(tagStore).createTagDef(rTagDef);
		Mockito.verify(tagStore).getServiceResourceByGuid(rserRes.getGuid());
		Mockito.verify(tagStore).createTag(rTag2);
	}

	@Test
	public void test3process() throws Exception {
		serviceTags = new ServiceTags();
		Map<Long, RangerTagDef> fd = new HashMap<>();
		List<RangerTag> associatedTags = new ArrayList<>();
		RangerTagDef rTagDef = Mockito.mock(RangerTagDef.class);
		rTagDef.setGuid("guid");
		rTagDef.setName("rTagDefname");
		fd.put(1l, rTagDef);
		serviceTags.setTagDefinitions(fd);
		List<RangerServiceResource> serviceResources = new ArrayList<RangerServiceResource>();
		RangerServiceResource rserRes = new RangerServiceResource();
		testServiceRest = new TestServiceREST();
		RangerPolicy rp = testServiceRest.rangerPolicy();
		rserRes.setResourceElements(rp.getResources());
		rserRes.setGuid("guId");
		rserRes.setId(1L);
		serviceResources.add(rserRes);
		serviceTags.setServiceResources(serviceResources);

		Map<Long, List<Long>> resourceToTagIds = new HashMap<>();
		resourceToTagIds.put(1L, new ArrayList<Long>(Arrays.asList(22L, 1L, 0L)));
		serviceTags.setResourceToTagIds(resourceToTagIds);

		RangerTag rTag = new RangerTag();
		rTag.setId(22L);
		rTag.setType("type1");
		Map<Long, RangerTag> tags = new HashMap<>();
		rTag.setOwner((short) 0);
		tags.put(1L, rTag);
		serviceTags.setTags(tags);

		associatedTags.add(rTag);

		TagStore tagStore = Mockito.mock(TagStore.class);
		sTagProcessor = new ServiceTagsProcessor(tagStore);
		Mockito.when(tagStore.createTagDef(rTagDef)).thenReturn(rTagDef);
		Mockito.when(tagStore.getServiceResourceByGuid(rserRes.getGuid())).thenReturn(rserRes);
		Mockito.when(tagStore.getTagsForResourceId(rserRes.getId())).thenReturn(associatedTags);

		sTagProcessor.process(serviceTags);

		Mockito.verify(tagStore).createTagDef(rTagDef);
		Mockito.verify(tagStore).getServiceResourceByGuid(rserRes.getGuid());
		Mockito.verify(tagStore).getTagsForResourceId(rserRes.getId());
	}

	@Test
	public void test4processDelete() throws Exception {
		serviceTags = new ServiceTags();
		serviceTags.setOp(ServiceTags.OP_DELETE);
		Map<Long, RangerTagDef> fd = new HashMap<>();
		List<RangerTag> associatedTags = new ArrayList<>();
		RangerTagDef rTagDef = Mockito.mock(RangerTagDef.class);
		rTagDef.setGuid("guid");
		rTagDef.setName("rTagDefName");
		fd.put(1l, rTagDef);
		serviceTags.setTagDefinitions(fd);
		List<RangerServiceResource> serviceResources = new ArrayList<RangerServiceResource>();
		RangerServiceResource rserRes = new RangerServiceResource();
		testServiceRest = new TestServiceREST();
		RangerPolicy rp = testServiceRest.rangerPolicy();
		rserRes.setResourceElements(rp.getResources());
		rserRes.setGuid("guId");
		rserRes.setId(1L);
		rserRes.setServiceName("serviceName1");
		serviceResources.add(rserRes);
		serviceTags.setServiceResources(serviceResources);
		List<RangerTagResourceMap> tagResourceMaps = new ArrayList<RangerTagResourceMap>();
		tagResourceMaps.add(new RangerTagResourceMap());
		Map<Long, List<Long>> resourceToTagIds = new HashMap<>();
		resourceToTagIds.put(1L, new ArrayList<Long>(Arrays.asList(22L, 1L, 0L)));
		serviceTags.setResourceToTagIds(resourceToTagIds);

		RangerTag rTag = new RangerTag();
		rTag.setId(22L);
		rTag.setType("type1");
		rTag.setGuid("tagGuID");
		Map<Long, RangerTag> tags = new HashMap<>();
		rTag.setOwner((short) 0);
		tags.put(1L, rTag);
		serviceTags.setTags(tags);

		RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(rserRes);
		String serviceResourceSignature = serializer.getSignature();
		associatedTags.add(rTag);
		TagStore tagStore = Mockito.mock(TagStore.class);
		sTagProcessor = new ServiceTagsProcessor(tagStore);

		Mockito.when(tagStore.getServiceResourceByGuid(rserRes.getGuid())).thenReturn(null);
		Mockito.when(tagStore.getServiceResourceByServiceAndResourceSignature(rserRes.getServiceName(),
				serviceResourceSignature)).thenReturn(rserRes);
		Mockito.when(tagStore.getTagResourceMapsForResourceGuid(rserRes.getGuid())).thenReturn(tagResourceMaps);
		Mockito.doNothing().when(tagStore).deleteServiceResource(rserRes.getId());
		Mockito.when(tagStore.getTagByGuid(rTag.getGuid())).thenReturn(rTag);
		Mockito.doNothing().when(tagStore).deleteTag(rTag.getId());
		Mockito.when(tagStore.getTagDefByGuid(rTagDef.getGuid())).thenReturn(rTagDef);

		sTagProcessor.process(serviceTags);

		Mockito.verify(tagStore).getServiceResourceByGuid(rserRes.getGuid());
		Mockito.verify(tagStore).getServiceResourceByServiceAndResourceSignature(rserRes.getServiceName(),
				serviceResourceSignature);
		Mockito.verify(tagStore).getTagResourceMapsForResourceGuid(rserRes.getGuid());
		Mockito.verify(tagStore).deleteServiceResource(rserRes.getId());
		Mockito.verify(tagStore).getTagByGuid(rTag.getGuid());
		Mockito.verify(tagStore).deleteTag(rTag.getId());
		Mockito.verify(tagStore).getTagDefByGuid(rTagDef.getGuid());
	}

	@Test
	public void test5processReplace() throws Exception {
		serviceTags = new ServiceTags();
		serviceTags.setOp(ServiceTags.OP_REPLACE);
		Map<Long, RangerTagDef> fd = new HashMap<>();
		List<RangerTag> associatedTags = new ArrayList<>();
		RangerTagDef rTagDef = Mockito.mock(RangerTagDef.class);
		rTagDef.setGuid("guid");
		rTagDef.setName("rTagDefName");
		fd.put(1l, rTagDef);
		serviceTags.setTagDefinitions(fd);
		List<RangerServiceResource> serviceResources = new ArrayList<RangerServiceResource>();
		RangerServiceResource rserRes = new RangerServiceResource();
		testServiceRest = new TestServiceREST();
		RangerPolicy rp = testServiceRest.rangerPolicy();
		rserRes.setResourceElements(rp.getResources());
		rserRes.setGuid("guId");
		rserRes.setId(1L);
		rserRes.setServiceName("serviceName1");
		serviceResources.add(rserRes);
		serviceTags.setServiceResources(serviceResources);
		List<RangerTagResourceMap> tagResourceMaps = new ArrayList<RangerTagResourceMap>();
		serviceTags.setServiceName("tagServiceName");
		RangerTagResourceMap rangerTagRmp = new RangerTagResourceMap();
		rangerTagRmp.setId(2L);
		tagResourceMaps.add(rangerTagRmp);
		Map<Long, List<Long>> resourceToTagIds = new HashMap<>();
		resourceToTagIds.put(1L, new ArrayList<Long>(Arrays.asList(22L, 1L, 0L)));
		serviceTags.setResourceToTagIds(resourceToTagIds);

		RangerTag rTag = new RangerTag();
		rTag.setId(22L);
		rTag.setType("type1");
		rTag.setGuid("tagGuID");
		Map<Long, RangerTag> tags = new HashMap<>();
		rTag.setOwner((short) 0);
		tags.put(1L, rTag);
		serviceTags.setTags(tags);
		associatedTags.add(rTag);
		TagStore tagStore = Mockito.mock(TagStore.class);
		sTagProcessor = new ServiceTagsProcessor(tagStore);

		List<String> serviceResourcesInDb = new ArrayList<>(Arrays.asList("guid"));
		Mockito.when(tagStore.getServiceResourceGuidsByService(serviceTags.getServiceName()))
				.thenReturn(serviceResourcesInDb);
		Mockito.when(tagStore.getTagResourceMapsForResourceGuid(Mockito.anyString())).thenReturn(tagResourceMaps);
		Mockito.doNothing().when(tagStore).deleteTagResourceMap(rangerTagRmp.getId());
		Mockito.doNothing().when(tagStore).deleteServiceResourceByGuid(Mockito.anyString());

		sTagProcessor.process(serviceTags);

		Mockito.verify(tagStore).getServiceResourceGuidsByService(serviceTags.getServiceName());
		Mockito.verify(tagStore).getTagResourceMapsForResourceGuid(Mockito.anyString());
		Mockito.verify(tagStore).deleteTagResourceMap(rangerTagRmp.getId());
		Mockito.verify(tagStore).deleteServiceResourceByGuid(Mockito.anyString());
	}
}
