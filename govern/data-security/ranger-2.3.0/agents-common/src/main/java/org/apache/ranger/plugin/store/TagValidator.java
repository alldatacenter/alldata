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

package org.apache.ranger.plugin.store;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.*;

import java.util.*;

public class TagValidator {
	private TagStore tagStore;

	public TagValidator() {}

	public void setTagStore(TagStore tagStore) {
		this.tagStore = tagStore;
	}

	public RangerTagDef preCreateTagDef(final RangerTagDef tagDef, boolean updateIfExists) throws Exception {
		String name = tagDef.getName();

		if (StringUtils.isBlank(name)) {
			throw new Exception("TagDef has no name");
		}

		RangerTagDef existing = tagStore.getTagDefByName(name);

		return existing;
	}

	public RangerTag preCreateTag(final RangerTag tag) throws Exception {
		if(StringUtils.isBlank(tag.getType()) ) {
			throw new Exception("Tag has no type");
		}

		RangerTag ret = null;

		String guid = tag.getGuid();
		if (! StringUtils.isBlank(guid)) {
			ret = tagStore.getTagByGuid(guid);
		}

		return ret;
	}

	public void preUpdateTag(final Long id, final RangerTag tag) throws Exception {
		if (StringUtils.isBlank(tag.getType())) {
			throw new Exception("Tag has no type");
		}

		if (id == null) {
			throw new Exception("Invalid/null id");
		}

		RangerTag existing = tagStore.getTag(id);

		if (existing == null) {
			throw new Exception("Attempt to update nonexistant tag, id=" + id);
		}

		if (!StringUtils.equals(tag.getType(), existing.getType())) {
			throw new Exception("Attempt to change the tag-type");
		}

		tag.setId(existing.getId());
		tag.setGuid(existing.getGuid());
	}

	public void preUpdateTagByGuid(String guid, final RangerTag tag) throws Exception {
		if (StringUtils.isBlank(tag.getType())) {
			throw new Exception("Tag has no type");
		}

		RangerTag existing = tagStore.getTagByGuid(guid);
		if (existing == null) {
			throw new Exception("Attempt to update nonexistent tag, guid=" + guid);
		}

		if (!StringUtils.equals(tag.getType(), existing.getType())) {
			throw new Exception("Attempt to change the tag-type");
		}

		tag.setId(existing.getId());
		tag.setGuid(existing.getGuid());
	}

	public RangerTag preDeleteTag(Long id) throws Exception {
		if (id == null) {
			throw new Exception("Invalid/null id");
		}

		RangerTag existing = tagStore.getTag(id);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent tag, id=" + id);
		}

		List<RangerTagResourceMap> associations = tagStore.getTagResourceMapsForTagId(existing.getId());
		if (CollectionUtils.isNotEmpty(associations)) {
			throw new Exception("Attempt to delete tag which is associated with a service-resource, id=" + id);
		}
		return existing;
	}

	public RangerTag preDeleteTagByGuid(String guid) throws Exception {
		RangerTag exiting = tagStore.getTagByGuid(guid);

		if (exiting == null) {
			throw new Exception("Attempt to delete nonexistent tag, guid=" + guid);
		}

		List<RangerTagResourceMap> associations = tagStore.getTagResourceMapsForTagId(exiting.getId());
		if (CollectionUtils.isNotEmpty(associations)) {
			throw new Exception("Attempt to delete tag which is associated with a service-resource, guid=" + guid);
		}
		return exiting;
	}

	public RangerServiceResource preCreateServiceResource(RangerServiceResource resource) throws Exception {
		RangerServiceResource ret = null;

		if (StringUtils.isBlank(resource.getServiceName()) || MapUtils.isEmpty(resource.getResourceElements())) {
			throw new Exception("No serviceName or resource in RangerServiceResource");
		}

		String guid = resource.getGuid();
		if (! StringUtils.isBlank(guid)) {
			ret = tagStore.getServiceResourceByGuid(guid);
		}

		if (ret == null) {
			RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(resource);
			resource.setResourceSignature(serializer.getSignature());
		}

		return ret;
	}

	public void preUpdateServiceResource(Long id, RangerServiceResource resource) throws Exception {
		if (StringUtils.isBlank(resource.getServiceName()) || MapUtils.isEmpty(resource.getResourceElements())) {
			throw new Exception("No serviceName or resource in RangerServiceResource");
		}

		if (id == null) {
			throw new Exception("Invalid/null id");
		}

		RangerServiceResource existing = tagStore.getServiceResource(id);
		if (existing == null) {
			throw new Exception("Attempt to update nonexistent resource, id=" + id);
		}

		if (!StringUtils.equals(existing.getServiceName(), resource.getServiceName())) {
			throw new Exception("Attempt to change service-name for existing service-resource");
		}

		RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(resource);

		resource.setId(existing.getId());
		resource.setGuid(existing.getGuid());
		resource.setResourceSignature(serializer.getSignature());
	}

	public void preUpdateServiceResourceByGuid(String guid, RangerServiceResource resource) throws Exception {
		if (StringUtils.isBlank(resource.getServiceName()) || MapUtils.isEmpty(resource.getResourceElements())) {
			throw new Exception("No serviceName or resource in RangerServiceResource");
		}

		RangerServiceResource existing = tagStore.getServiceResourceByGuid(guid);
		if (existing == null) {
			throw new Exception("Attempt to update nonexistent resource, guid=" + guid);
		}

		if (!StringUtils.equals(existing.getServiceName(), resource.getServiceName())) {
			throw new Exception("Attempt to change service-name for existing service-resource");
		}

		RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(resource);

		resource.setId(existing.getId());
		resource.setGuid(guid);
		resource.setResourceSignature(serializer.getSignature());
	}

	public RangerServiceResource preDeleteServiceResource(Long id) throws Exception {
		RangerServiceResource existing = tagStore.getServiceResource(id);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent resource, id=" + id);
		}

		List<RangerTagResourceMap> associations = tagStore.getTagResourceMapsForResourceId(existing.getId());
		if (CollectionUtils.isNotEmpty(associations)) {
			throw new Exception("Attempt to delete serviceResource which is associated with a tag, id=" + id);
		}

		return existing;
	}

	public RangerServiceResource preDeleteServiceResourceByGuid(String guid, boolean deleteReferences) throws Exception {
		RangerServiceResource existing = tagStore.getServiceResourceByGuid(guid);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent resource, guid=" + guid);
		}

		List<RangerTagResourceMap> associations = tagStore.getTagResourceMapsForResourceId(existing.getId());
		if (CollectionUtils.isNotEmpty(associations) && !deleteReferences) {
			throw new Exception("Attempt to delete serviceResource which is associated with a tag, guid=" + guid);
		}

		return existing;
	}

	public RangerTagResourceMap preCreateTagResourceMap(String tagGuid, String resourceGuid) throws Exception {
		if (StringUtils.isBlank(resourceGuid) || StringUtils.isBlank(tagGuid)) {
			throw new Exception("Both resourceGuid and resourceId need to be non-empty");
		}

		RangerTagResourceMap existing = tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);

		if (existing != null) {
			throw new Exception("Attempt to create existing association between resourceId=" + resourceGuid + " and tagId=" + tagGuid);
		}

		RangerServiceResource existingServiceResource = tagStore.getServiceResourceByGuid(resourceGuid);

		if(existingServiceResource == null) {
			throw new Exception("No resource found for guid=" + resourceGuid);
		}

		RangerTag existingTag = tagStore.getTagByGuid(tagGuid);

		if(existingTag == null) {
			throw new Exception("No tag found for guid=" + tagGuid);
		}

		RangerTagResourceMap newTagResourceMap = new RangerTagResourceMap();
		newTagResourceMap.setResourceId(existingServiceResource.getId());
		newTagResourceMap.setTagId(existingTag.getId());

		return newTagResourceMap;
	}

	public RangerTagResourceMap preCreateTagResourceMapByIds(Long tagId, Long resourceId) throws Exception {
		RangerTagResourceMap existing = tagStore.getTagResourceMapForTagAndResourceId(tagId, resourceId);

		if (existing != null) {
			throw new Exception("Attempt to create existing association between resourceId=" + resourceId + " and tagId=" + tagId);
		}

		RangerServiceResource existingServiceResource = tagStore.getServiceResource(resourceId);

		if(existingServiceResource == null) {
			throw new Exception("No resource found for id=" + resourceId);
		}

		RangerTag existingTag = tagStore.getTag(tagId);

		if(existingTag == null) {
			throw new Exception("No tag found for id=" + tagId);
		}

		RangerTagResourceMap newTagResourceMap = new RangerTagResourceMap();
		newTagResourceMap.setResourceId(resourceId);
		newTagResourceMap.setTagId(tagId);

		return newTagResourceMap;
	}

	public RangerTagResourceMap preDeleteTagResourceMap(Long id) throws Exception {
		RangerTagResourceMap existing = tagStore.getTagResourceMap(id);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent tagResourceMap(id=" + id + ")");
		}

		return existing;
	}

	public RangerTagResourceMap preDeleteTagResourceMapByGuid(String guid) throws Exception {
		RangerTagResourceMap existing = tagStore.getTagResourceMapByGuid(guid);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent tagResourceMap(guid=" + guid + ")");
		}

		return existing;
	}

	public RangerTagResourceMap preDeleteTagResourceMap(String tagGuid, String resourceGuid) throws Exception {
		RangerTagResourceMap existing = tagStore.getTagResourceMapForTagAndResourceGuid(tagGuid, resourceGuid);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent association between resourceId=" + resourceGuid + " and tagId=" + tagGuid);
		}

		return existing;
	}

	public RangerTagResourceMap preDeleteTagResourceMapByIds(Long tagId, Long resourceId) throws Exception {
		RangerTagResourceMap existing = tagStore.getTagResourceMapForTagAndResourceId(tagId, resourceId);

		if (existing == null) {
			throw new Exception("Attempt to delete nonexistent association between resourceId=" + resourceId + " and tagId=" + tagId);
		}

		return existing;
	}
}
