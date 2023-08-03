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

package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerAdminTagEnricher;
import org.apache.ranger.common.RangerServiceTagsCache;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXTagDao;
import org.apache.ranger.db.XXTagDefDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceResource;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.entity.XXTag;
import org.apache.ranger.entity.XXTagChangeLog;
import org.apache.ranger.entity.XXTagDef;
import org.apache.ranger.entity.XXTagResourceMap;
import org.apache.ranger.plugin.model.*;
import org.apache.ranger.plugin.model.validation.RangerValidityScheduleValidator;
import org.apache.ranger.plugin.model.validation.ValidationFailureDetails;
import org.apache.ranger.plugin.store.AbstractTagStore;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.store.RangerServiceResourceSignature;
import org.apache.ranger.plugin.util.RangerCommonConstants;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.RangerServiceNotFoundException;
import org.apache.ranger.plugin.util.RangerServiceTagsDeltaUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.service.RangerTagDefService;
import org.apache.ranger.service.RangerTagResourceMapService;
import org.apache.ranger.service.RangerTagService;
import org.apache.ranger.service.RangerServiceResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

@Component
public class TagDBStore extends AbstractTagStore {
	private static final Logger LOG      = LoggerFactory.getLogger(TagDBStore.class);
	private static final Logger PERF_LOG = RangerPerfTracer.getPerfLogger("db.TagDBStore");


	private static boolean SUPPORTS_TAG_DELTAS = false;
	private static boolean IS_SUPPORTS_TAG_DELTAS_INITIALIZED = false;
	public static boolean SUPPORTS_IN_PLACE_TAG_UPDATES = false;

	@Autowired
	RangerTagDefService rangerTagDefService;

	@Autowired
	RangerTagService rangerTagService;

	@Autowired
	RangerServiceResourceService rangerServiceResourceService;

	@Autowired
	RangerTagResourceMapService rangerTagResourceMapService;

	@Autowired
	RangerDaoManager daoManager;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	@Autowired
	RESTErrorUtil errorUtil;

	@Autowired
	RESTErrorUtil restErrorUtil;

	RangerAdminConfig config;

	@PostConstruct
	public void initStore() {
		config = RangerAdminConfig.getInstance();

		RangerAdminTagEnricher.setTagStore(this);
		RangerAdminTagEnricher.setDaoManager(daoManager);
	}

	@Override
	public RangerTagDef createTagDef(RangerTagDef tagDef) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.createTagDef(" + tagDef + ")");
		}

		RangerTagDef ret = rangerTagDefService.create(tagDef);

		ret = rangerTagDefService.read(ret.getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.createTagDef(" + tagDef + "): id=" + (ret == null ? null : ret.getId()));
		}

		return ret;
	}

	@Override
	public RangerTagDef updateTagDef(RangerTagDef tagDef) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.updateTagDef(" + tagDef + ")");
		}

		RangerTagDef existing = rangerTagDefService.read(tagDef.getId());

		if (existing == null) {
			throw errorUtil.createRESTException("failed to update tag-def [" + tagDef.getName() + "], Reason: No TagDef found with id: [" + tagDef.getId() + "]", MessageEnums.DATA_NOT_UPDATABLE);
		} else if (!existing.getName().equals(tagDef.getName())) {
			throw errorUtil.createRESTException("Cannot change tag-def name; existing-name:[" + existing.getName() + "], new-name:[" + tagDef.getName() + "]", MessageEnums.DATA_NOT_UPDATABLE);
		}

		tagDef.setCreatedBy(existing.getCreatedBy());
		tagDef.setCreateTime(existing.getCreateTime());
		tagDef.setGuid(existing.getGuid());
		tagDef.setVersion(existing.getVersion());

		RangerTagDef ret = rangerTagDefService.update(tagDef);

		ret = rangerTagDefService.read(ret.getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.updateTagDef(" + tagDef + "): " + ret);
		}

		return ret;
	}

	@Override
	public void deleteTagDefByName(String name) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteTagDef(" + name + ")");
		}

		if (StringUtils.isNotBlank(name)) {
			RangerTagDef tagDef = getTagDefByName(name);

			if(tagDef != null) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("Deleting tag-def [name=" + name + "; id=" + tagDef.getId() + "]");
				}

				rangerTagDefService.delete(tagDef);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteTagDef(" + name + ")");
		}
	}

	@Override
	public void deleteTagDef(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteTagDef(" + id + ")");
		}

		if(id != null) {
			RangerTagDef tagDef = rangerTagDefService.read(id);

			if(tagDef != null) {
				rangerTagDefService.delete(tagDef);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteTagDef(" + id + ")");
		}
	}

	@Override
	public RangerTagDef getTagDef(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagDef(" + id + ")");
		}

		RangerTagDef ret = rangerTagDefService.read(id);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagDef(" + id + "): " + ret);
		}

		return ret;
	}

	@Override
	public RangerTagDef getTagDefByGuid(String guid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagDefByGuid(" + guid + ")");
		}

		RangerTagDef ret = rangerTagDefService.getTagDefByGuid(guid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagDefByGuid(" + guid + "): " + ret);
		}

		return ret;
	}

	@Override
	public RangerTagDef getTagDefByName(String name) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagDefByName(" + name + ")");
		}

		RangerTagDef ret = null;

		if (StringUtils.isNotBlank(name)) {
			ret = rangerTagDefService.getTagDefByName(name);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagDefByName(" + name + "): " + ret);
		}

		return ret;
	}

	@Override
	public List<RangerTagDef> getTagDefs(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagDefs(" + filter + ")");
		}

		List<RangerTagDef> ret = getPaginatedTagDefs(filter).getList();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagDefs(" + filter + "): " + ret);
		}

		return ret;
	}

	@Override
	public PList<RangerTagDef> getPaginatedTagDefs(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getPaginatedTagDefs(" + filter + ")");
		}

		PList<RangerTagDef> ret = rangerTagDefService.searchRangerTagDefs(filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getPaginatedTagDefs(" + filter + "): " + ret);
		}

		return ret;
	}

	@Override
	public List<String> getTagTypes() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagTypes()");
		}

		List<String> ret = daoManager.getXXTagDef().getAllNames();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagTypes(): count=" + (ret != null ? ret.size() : 0));
		}

		return ret;
	}


	@Override
	public RangerTag createTag(RangerTag tag) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.createTag(" + tag + ")");
		}

		tag = validateTag(tag);

		RangerTag ret = rangerTagService.create(tag);

		ret = rangerTagService.read(ret.getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.createTag(" + tag + "): " + ret);
		}

		return ret;
	}
	
	@Override
	public RangerTag updateTag(RangerTag tag) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.updateTag(" + tag + ")");
		}

		tag = validateTag(tag);

		RangerTag existing = rangerTagService.read(tag.getId());

		if (existing == null) {
			throw errorUtil.createRESTException("failed to update tag [" + tag.getType() + "], Reason: No Tag found with id: [" + tag.getId() + "]", MessageEnums.DATA_NOT_UPDATABLE);
		}

		tag.setCreatedBy(existing.getCreatedBy());
		tag.setCreateTime(existing.getCreateTime());
		tag.setGuid(existing.getGuid());
		tag.setVersion(existing.getVersion());

		RangerTag ret = rangerTagService.update(tag);

		ret = rangerTagService.read(ret.getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.updateTag(" + tag + ") : " + ret);
		}

		return ret;
	}

	@Override
	public void deleteTag(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteTag(" + id + ")");
		}

		RangerTag tag = rangerTagService.read(id);

		rangerTagService.delete(tag);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteTag(" + id + ")");
		}
	}

	@Override
	public RangerTag getTag(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTag(" + id + ")");
		}

		RangerTag ret = rangerTagService.read(id);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTag(" + id + "): " + ret);
		}

		return ret;
	}

	@Override
	public RangerTag getTagByGuid(String guid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagByGuid(" + guid + ")");
		}

		RangerTag ret = rangerTagService.getTagByGuid(guid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagByGuid(" + guid + "): " + ret);
		}

		return ret;
	}

	@Override
	public List<RangerTag> getTagsByType(String type) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagsByType(" + type + ")");
		}

		List<RangerTag> ret = null;

		if (StringUtils.isNotBlank(type)) {
			ret = rangerTagService.getTagsByType(type);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagsByType(" + type + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<RangerTag> getTagsForResourceId(Long resourceId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagsForResourceId(" + resourceId + ")");
		}

		List<RangerTag> ret = null;

		if (resourceId != null) {
			ret = rangerTagService.getTagsForResourceId(resourceId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagsForResourceId(" + resourceId + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<RangerTag> getTagsForResourceGuid(String resourceGuid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagsForResourceGuid(" + resourceGuid + ")");
		}

		List<RangerTag> ret = null;

		if (resourceGuid != null) {
			ret = rangerTagService.getTagsForResourceGuid(resourceGuid);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagsForResourceGuid(" + resourceGuid + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<RangerTag> getTags(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTags(" + filter + ")");
		}

		List<RangerTag> ret = rangerTagService.searchRangerTags(filter).getList();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTags(" + filter + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public PList<RangerTag> getPaginatedTags(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getPaginatedTags(" + filter + ")");
		}

		PList<RangerTag> ret = rangerTagService.searchRangerTags(filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getPaginatedTags(" + filter + "): count=" + (ret == null ? 0 : ret.getPageSize()));
		}

		return ret;
	}

    public boolean resetTagCache(final String serviceName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TagDBStore.resetTagCache({})", serviceName);
        }

        boolean ret = RangerServiceTagsCache.getInstance().resetCache(serviceName);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== TagDBStore.resetTagCache(): ret={}", ret);
        }

        return ret;
    }

	@Override
	public RangerServiceResource createServiceResource(RangerServiceResource resource) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.createServiceResource(" + resource + ")");
		}

		if (StringUtils.isEmpty(resource.getResourceSignature())) {
			RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(resource);

			resource.setResourceSignature(serializer.getSignature());
		}

		RangerServiceResource ret = rangerServiceResourceService.create(resource);

		ret = rangerServiceResourceService.read(ret.getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.createServiceResource(" + resource + ")");
		}

		return ret;
	}

	@Override
	public RangerServiceResource updateServiceResource(RangerServiceResource resource) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.updateResource(" + resource + ")");
		}

		RangerServiceResource existing = rangerServiceResourceService.read(resource.getId());

		if (existing == null) {
			throw errorUtil.createRESTException("failed to update tag [" + resource.getId() + "], Reason: No resource found with id: [" + resource.getId() + "]", MessageEnums.DATA_NOT_UPDATABLE);
		}

		if (StringUtils.isEmpty(resource.getResourceSignature())) {
			RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(resource);

			resource.setResourceSignature(serializer.getSignature());
		}

		resource.setCreatedBy(existing.getCreatedBy());
		resource.setCreateTime(existing.getCreateTime());
		resource.setGuid(existing.getGuid());
		resource.setVersion(existing.getVersion());

		rangerServiceResourceService.update(resource);

		RangerServiceResource ret = rangerServiceResourceService.read(existing.getId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.updateResource(" + resource + ") : " + ret);
		}

		return ret;
	}


	@Override
	public void refreshServiceResource(Long resourceId) throws Exception {
		XXServiceResource serviceResourceEntity = daoManager.getXXServiceResource().getById(resourceId);
		String tagsText = null;

		List<RangerTagResourceMap> tagResourceMaps = getTagResourceMapsForResourceId(resourceId);
		if (tagResourceMaps != null) {
			List<RangerTag> associatedTags = new ArrayList<>();
			for (RangerTagResourceMap element : tagResourceMaps) {
				associatedTags.add(getTag(element.getTagId()));
			}
			tagsText = JsonUtils.listToJson(associatedTags);
		}
		serviceResourceEntity.setTags(tagsText);
		daoManager.getXXServiceResource().update(serviceResourceEntity);
	}

	@Override
	public void deleteServiceResource(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteServiceResource(" + id + ")");
		}

		RangerServiceResource resource = getServiceResource(id);

		if(resource != null) {
			rangerServiceResourceService.delete(resource);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteServiceResource(" + id + ")");
		}
	}

	@Override
	public void deleteServiceResourceByGuid(String guid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteServiceResourceByGuid(" + guid + ")");
		}

		RangerServiceResource resource = getServiceResourceByGuid(guid);

		if(resource != null) {
			rangerServiceResourceService.delete(resource);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteServiceResourceByGuid(" + guid + ")");
		}
	}

	@Override
	public RangerServiceResource getServiceResource(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceResource(" + id + ")");
		}

		RangerServiceResource ret = rangerServiceResourceService.read(id);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceResource(" + id + "): " + ret);
		}

		return ret;
	}

	@Override
	public RangerServiceResource getServiceResourceByGuid(String guid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceResourceByGuid(" + guid + ")");
		}

		RangerServiceResource ret = rangerServiceResourceService.getServiceResourceByGuid(guid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceResourceByGuid(" + guid + "): " + ret);
		}

		return ret;
	}

	@Override
	public List<RangerServiceResource> getServiceResourcesByService(String serviceName) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceResourcesByService(" + serviceName + ")");
		}

		List<RangerServiceResource> ret = null;

		Long serviceId = daoManager.getXXService().findIdByName(serviceName);

		if (serviceId != null) {
			ret = rangerServiceResourceService.getByServiceId(serviceId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceResourcesByService(" + serviceName + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<String> getServiceResourceGuidsByService(String serviceName) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceResourceGuidsByService(" + serviceName + ")");
		}

		List<String> ret = null;

		Long serviceId = daoManager.getXXService().findIdByName(serviceName);

		if (serviceId != null) {
			ret = daoManager.getXXServiceResource().findServiceResourceGuidsInServiceId(serviceId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceResourceGuidsByService(" + serviceName + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public RangerServiceResource getServiceResourceByServiceAndResourceSignature(String serviceName, String resourceSignature) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceResourceByServiceAndResourceSignature(" + serviceName + ", " + resourceSignature + ")");
		}

		RangerServiceResource ret = null;

		Long serviceId = daoManager.getXXService().findIdByName(serviceName);

		if (serviceId != null) {
			ret = rangerServiceResourceService.getByServiceAndResourceSignature(serviceId, resourceSignature);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceResourceByServiceAndResourceSignature(" + serviceName + ", " + resourceSignature + "): " + ret);
		}

		return ret;
	}

	@Override
	public List<RangerServiceResource> getServiceResources(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceResources(" + filter + ")");
		}

		List<RangerServiceResource> ret = rangerServiceResourceService.searchServiceResources(filter).getList();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceResources(" + filter + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public PList<RangerServiceResource> getPaginatedServiceResources(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getPaginatedServiceResources(" + filter + ")");
		}

		PList<RangerServiceResource> ret = rangerServiceResourceService.searchServiceResources(filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getPaginatedServiceResources(" + filter + "): count=" + (ret == null ? 0 : ret.getPageSize()));
		}

		return ret;
	}


	@Override
	public RangerTagResourceMap createTagResourceMap(RangerTagResourceMap tagResourceMap) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.createTagResourceMap(" + tagResourceMap + ")");
		}

		RangerTagResourceMap ret = rangerTagResourceMapService.create(tagResourceMap);

		// We also need to update tags stored with the resource
		refreshServiceResource(tagResourceMap.getResourceId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.createTagResourceMap(" + tagResourceMap + "): " + ret);
		}

		return ret;
	}

	@Override
	public void deleteTagResourceMap(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteTagResourceMap(" + id + ")");
		}

		RangerTagResourceMap tagResourceMap = rangerTagResourceMapService.read(id);
		Long tagId = tagResourceMap.getTagId();
		RangerTag tag = getTag(tagId);

		rangerTagResourceMapService.delete(tagResourceMap);

		if (tag.getOwner() == null || tag.getOwner() == RangerTag.OWNER_SERVICERESOURCE) {
			deleteTag(tagId);
		}
		// We also need to update tags stored with the resource
		refreshServiceResource(tagResourceMap.getResourceId());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteTagResourceMap(" + id + ")");
		}
	}

	@Override
	public RangerTagResourceMap getTagResourceMap(Long id) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMap(" + id + ")");
		}

		RangerTagResourceMap ret = rangerTagResourceMapService.read(id);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMap(" + id + ")");
		}

		return ret;
	}

	@Override
	public RangerTagResourceMap getTagResourceMapByGuid(String guid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapByGuid(" + guid + ")");
		}

		RangerTagResourceMap ret = rangerTagResourceMapService.getByGuid(guid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapByGuid(" + guid + ")");
		}

		return ret;
	}

	@Override
	public List<RangerTagResourceMap> getTagResourceMapsForTagId(Long tagId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapsForTagId(" + tagId + ")");
		}

		List<RangerTagResourceMap> ret = rangerTagResourceMapService.getByTagId(tagId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapsForTagId(" + tagId + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<RangerTagResourceMap> getTagResourceMapsForTagGuid(String tagGuid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapsForTagGuid(" + tagGuid + ")");
		}

		List<RangerTagResourceMap> ret = rangerTagResourceMapService.getByTagGuid(tagGuid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapsForTagGuid(" + tagGuid + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<Long> getTagIdsForResourceId(Long resourceId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagIdsForResourceId(" + resourceId + ")");
		}

		List<Long> ret = rangerTagResourceMapService.getTagIdsForResourceId(resourceId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagIdsForResourceId(" + resourceId + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<RangerTagResourceMap> getTagResourceMapsForResourceId(Long resourceId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapsForResourceId(" + resourceId + ")");
		}

		List<RangerTagResourceMap> ret = rangerTagResourceMapService.getByResourceId(resourceId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapsForResourceId(" + resourceId + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public List<RangerTagResourceMap> getTagResourceMapsForResourceGuid(String resourceGuid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapsForResourceGuid(" + resourceGuid + ")");
		}

		List<RangerTagResourceMap> ret = rangerTagResourceMapService.getByResourceGuid(resourceGuid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapsForResourceGuid(" + resourceGuid + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public RangerTagResourceMap getTagResourceMapForTagAndResourceId(Long tagId, Long resourceId) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapsForTagAndResourceId(" + tagId + ", " + resourceId + ")");
		}

		RangerTagResourceMap ret = rangerTagResourceMapService.getByTagAndResourceId(tagId, resourceId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapsForTagAndResourceId(" + tagId + ", " + resourceId + "): " + ret);
		}

		return ret;
	}

	@Override
	public RangerTagResourceMap getTagResourceMapForTagAndResourceGuid(String tagGuid, String resourceGuid) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMapForTagAndResourceGuid(" + tagGuid + ", " + resourceGuid + ")");
		}

		RangerTagResourceMap ret = rangerTagResourceMapService.getByTagAndResourceGuid(tagGuid, resourceGuid);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMapForTagAndResourceGuid(" + tagGuid + ", " + resourceGuid + "): " + ret);
		}

		return ret;
	}


	@Override
	public List<RangerTagResourceMap> getTagResourceMaps(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getTagResourceMaps(" + filter+ ")");
		}

		List<RangerTagResourceMap> ret = rangerTagResourceMapService.searchRangerTaggedResources(filter).getList();

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getTagResourceMaps(" + filter + "): count=" + (ret == null ? 0 : ret.size()));
		}

		return ret;
	}

	@Override
	public PList<RangerTagResourceMap> getPaginatedTagResourceMaps(SearchFilter filter) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getPaginatedTagResourceMaps(" + filter+ ")");
		}

		PList<RangerTagResourceMap> ret = rangerTagResourceMapService.searchRangerTaggedResources(filter);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getPaginatedTagResourceMaps(" + filter + "): count=" + (ret == null ? 0 : ret.getPageSize()));
		}

		return ret;
	}


	@Override
	public ServiceTags getServiceTagsIfUpdated(String serviceName, Long lastKnownVersion, boolean needsBackwardCompatibility) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + ")");
		}

		ServiceTags ret = null;

		Long serviceId = daoManager.getXXService().findIdByName(serviceName);

		if (serviceId == null) {
			LOG.error("Requested Service not found. serviceName=" + serviceName);
			throw restErrorUtil.createRESTException(HttpServletResponse.SC_NOT_FOUND, RangerServiceNotFoundException.buildExceptionMsg(serviceName),
					false);
		}

		XXServiceVersionInfo serviceVersionInfoDbObj = daoManager.getXXServiceVersionInfo().findByServiceName(serviceName);

		if (serviceVersionInfoDbObj == null) {
			LOG.warn("serviceVersionInfo does not exist. name=" + serviceName);
		}

		if (lastKnownVersion == null || serviceVersionInfoDbObj == null || serviceVersionInfoDbObj.getTagVersion() == null || !lastKnownVersion.equals(serviceVersionInfoDbObj.getTagVersion())) {
			ret = RangerServiceTagsCache.getInstance().getServiceTags(serviceName, serviceId, lastKnownVersion, needsBackwardCompatibility, this);
		}

		if (ret != null && lastKnownVersion != null && lastKnownVersion.equals(ret.getTagVersion())) {
			// ServiceTags are not changed
			ret = null;
		}

		if (LOG.isDebugEnabled()) {
			RangerServiceTagsCache.getInstance().dump();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceTagsIfUpdated(" + serviceName + ", " + lastKnownVersion + ", " + needsBackwardCompatibility + "): count=" + ((ret == null || ret.getTags() == null) ? 0 : ret.getTags().size()));
		}

		return ret;
	}

	@Override
	public Long getTagVersion(String serviceName) {

		XXServiceVersionInfo serviceVersionInfoDbObj = daoManager.getXXServiceVersionInfo().findByServiceName(serviceName);

		return serviceVersionInfoDbObj != null ? serviceVersionInfoDbObj.getTagVersion() : null;
	}

	@Override
	public ServiceTags getServiceTags(String serviceName, Long lastKnownVersion) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceTags(" + serviceName + ", " + lastKnownVersion + ")");
		}

		final ServiceTags ret;

		XXService xxService = daoManager.getXXService().findByName(serviceName);

		if (xxService == null) {
			throw new Exception("service does not exist. name=" + serviceName);
		}

		XXServiceVersionInfo serviceVersionInfoDbObj = daoManager.getXXServiceVersionInfo().findByServiceName(serviceName);

		if (serviceVersionInfoDbObj == null) {
			LOG.warn("serviceVersionInfo does not exist for service [" + serviceName + "]");
		}

		RangerServiceDef serviceDef = svcStore.getServiceDef(xxService.getType());

		if (serviceDef == null) {
			throw new Exception("service-def does not exist. id=" + xxService.getType());
		}

		ServiceTags delta = getServiceTagsDelta(xxService.getId(), serviceName, lastKnownVersion);

		if (delta != null) {
			ret = delta;
		} else {
			RangerTagDBRetriever tagDBRetriever = new RangerTagDBRetriever(daoManager, txManager, xxService);

			Map<Long, RangerTagDef> tagDefMap = tagDBRetriever.getTagDefs();
			Map<Long, RangerTag> tagMap = tagDBRetriever.getTags();
			List<RangerServiceResource> resources = tagDBRetriever.getServiceResources();
			Map<Long, List<Long>> resourceToTagIds = tagDBRetriever.getResourceToTagIds();

			ret = new ServiceTags();

			ret.setServiceName(xxService.getName());
			ret.setTagVersion(serviceVersionInfoDbObj == null ? null : serviceVersionInfoDbObj.getTagVersion());
			ret.setTagUpdateTime(serviceVersionInfoDbObj == null ? null : serviceVersionInfoDbObj.getTagUpdateTime());
			ret.setTagDefinitions(tagDefMap);
			ret.setTags(tagMap);
			ret.setServiceResources(resources);
			ret.setResourceToTagIds(resourceToTagIds);

			if (RangerServiceTagsDeltaUtil.isSupportsTagsDedup()) {
				final int countOfDuplicateTags = ret.dedupTags();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Number of duplicate tags removed from the received serviceTags:[" + countOfDuplicateTags + "]. Number of tags in the de-duplicated serviceTags :[" + ret.getTags().size() + "].");
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceTags(" + serviceName + ", " + lastKnownVersion + ")");
		}

		return ret;
	}

	@Override
	public ServiceTags getServiceTagsDelta(String serviceName, Long lastKnownVersion) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceTagsDelta(" + serviceName + ", " + lastKnownVersion + ")");
		}

		final ServiceTags ret;

		if (lastKnownVersion == -1L || !isSupportsTagDeltas()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Returning without computing tags-deltas.., SUPPORTS_TAG_DELTAS:[" + SUPPORTS_TAG_DELTAS + "], lastKnownVersion:[" + lastKnownVersion + "]");
			}
			ret = null;
		} else {
			Long serviceId = daoManager.getXXService().findIdByName(serviceName);

			if (serviceId == null) {
				throw new Exception("service does not exist. name=" + serviceName);
			}

			ret = getServiceTagsDelta(serviceId, serviceName, lastKnownVersion);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceTagsDelta(" + serviceName + ", " + lastKnownVersion + ")");
		}
		return ret;
	}

	@Override
	public void deleteAllTagObjectsForService(String serviceName) throws Exception {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.deleteAllTagObjectsForService(" + serviceName + ")");
		}

		XXService service = daoManager.getXXService().findByName(serviceName);

		if (service != null) {
			Long serviceId = service.getId();

			List<XXTag> xxTags = daoManager.getXXTag().findByServiceIdAndOwner(serviceId, RangerTag.OWNER_SERVICERESOURCE);

			List<XXTagResourceMap> xxTagResourceMaps = daoManager.getXXTagResourceMap().findByServiceId(serviceId);

			if (CollectionUtils.isNotEmpty(xxTagResourceMaps)) {
				for (XXTagResourceMap xxTagResourceMap : xxTagResourceMaps) {
					try {
						daoManager.getXXTagResourceMap().remove(xxTagResourceMap);
					} catch (Exception e) {
						LOG.error("Error deleting RangerTagResourceMap with id=" + xxTagResourceMap.getId(), e);
						throw e;
					}
				}
			}

			if (CollectionUtils.isNotEmpty(xxTags)) {
				for (XXTag xxTag : xxTags) {
					try {
						daoManager.getXXTag().remove(xxTag);
					} catch (Exception e) {
						LOG.error("Error deleting RangerTag with id=" + xxTag.getId(), e);
						throw e;
					}
				}
			}

			List<XXServiceResource> xxServiceResources = daoManager.getXXServiceResource().findByServiceId(serviceId);

			if (CollectionUtils.isNotEmpty(xxServiceResources)) {
				for (XXServiceResource xxServiceResource : xxServiceResources) {
					try {
						daoManager.getXXServiceResource().remove(xxServiceResource);
					} catch (Exception e) {
						LOG.error("Error deleting RangerServiceResource with id=" + xxServiceResource.getId(), e);
						throw e;
					}
				}
			}

		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.deleteAllTagObjectsForService(" + serviceName + ")");
		}
	}

	private RangerTag validateTag(RangerTag tag) throws Exception {
		List<RangerValiditySchedule> validityPeriods = tag.getValidityPeriods();

		if (CollectionUtils.isNotEmpty(validityPeriods)) {
			List<RangerValiditySchedule>   normalizedValidityPeriods = new ArrayList<>();
			List<ValidationFailureDetails> failures                  = new ArrayList<>();

			for (RangerValiditySchedule validityPeriod : validityPeriods) {
				RangerValidityScheduleValidator validator                = new RangerValidityScheduleValidator(validityPeriod);
				RangerValiditySchedule          normalizedValidityPeriod = validator.validate(failures);

				if (normalizedValidityPeriod != null && CollectionUtils.isEmpty(failures)) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Normalized ValidityPeriod:[" + normalizedValidityPeriod + "]");
					}

					normalizedValidityPeriods.add(normalizedValidityPeriod);
				} else {
					String error = "Incorrect time-specification:[" + Arrays.asList(failures) + "]";

					LOG.error(error);

					throw new Exception(error);
				}
			}

			tag.setValidityPeriods(normalizedValidityPeriods);
		}

		return tag;
	}

	private ServiceTags getServiceTagsDelta(Long serviceId, String serviceName, Long lastKnownVersion) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.getServiceTagsDelta(lastKnownVersion=" + lastKnownVersion + ")");
		}
		ServiceTags ret = null;

		if (lastKnownVersion == -1L || !isSupportsTagDeltas()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Returning without computing tags-deltas.., SUPPORTS_TAG_DELTAS:[" + SUPPORTS_TAG_DELTAS + "], lastKnownVersion:[" + lastKnownVersion + "]");
			}
		} else {
			RangerPerfTracer perf = null;

			if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "TagDBStore.getServiceTagsDelta(serviceName=" + serviceName + ", lastKnownVersion=" + lastKnownVersion + ")");
			}

			List<XXTagChangeLog> changeLogRecords = daoManager.getXXTagChangeLog().findLaterThan(lastKnownVersion, serviceId);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Number of tag-change-log records found since " + lastKnownVersion + " :[" + (changeLogRecords == null ? 0 : changeLogRecords.size()) + "] for serviceId:[" + serviceId + "]");
			}

			try {
				ret = createServiceTagsDelta(changeLogRecords);
				if (ret != null) {
					ret.setServiceName(serviceName);
				}
			} catch (Exception e) {
				LOG.error("Perhaps some tag or service-resource could not be found", e);
			}

			RangerPerfTracer.logAlways(perf);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.getServiceTagsDelta(lastKnownVersion=" + lastKnownVersion + ")");
		}

		return ret;
	}

	private ServiceTags createServiceTagsDelta(List<XXTagChangeLog> changeLogs) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagDBStore.createServiceTagsDelta()");
		}

		ServiceTags ret = null;

		if (CollectionUtils.isNotEmpty(changeLogs)) {
			Set<String> tagTypes           = new HashSet<>();
			Set<Long>   tagIds             = new HashSet<>();
			Set<Long>   serviceResourceIds = new HashSet<>();

			for (XXTagChangeLog record : changeLogs) {
				if (record.getChangeType().equals(ServiceTags.TagsChangeType.TAG_UPDATE.ordinal())) {
					tagIds.add(record.getTagId());
				} else if (record.getChangeType().equals(ServiceTags.TagsChangeType.SERVICE_RESOURCE_UPDATE.ordinal())) {
					serviceResourceIds.add(record.getServiceResourceId());
				} else if (record.getChangeType().equals(ServiceTags.TagsChangeType.TAG_RESOURCE_MAP_UPDATE.ordinal())) {
					tagIds.add(record.getTagId());
					serviceResourceIds.add(record.getServiceResourceId());
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Unknown changeType in tag-change-log record: [" + record + "]");
						LOG.debug("Returning without further processing");
						tagIds.clear();
						serviceResourceIds.clear();
						break;
					}
				}
			}

			if (CollectionUtils.isNotEmpty(serviceResourceIds) || CollectionUtils.isNotEmpty(tagIds)) {
				ret = new ServiceTags();
				ret.setIsDelta(true);

				ServiceTags.TagsChangeExtent tagsChangeExtent = ServiceTags.TagsChangeExtent.TAGS;

				ret.setTagVersion(changeLogs.get(changeLogs.size() - 1).getServiceTagsVersion());

				XXTagDao tagDao = daoManager.getXXTag();

				for (Long tagId : tagIds) {
					RangerTag tag = null;

					try {
						XXTag xTag = tagDao.getById(tagId);

						if (xTag != null) {
							tag = rangerTagService.getPopulatedViewObject(xTag);

							tagTypes.add(tag.getType());
						}
					} catch (Throwable t) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("TagDBStore.createServiceTagsDelta(): failed to read tag id={}", tagId, t);
						}
					} finally {
						if (tag == null) {
							tag = new RangerTag();
							tag.setId(tagId);
						}
					}

					RangerServiceTagsDeltaUtil.pruneUnusedAttributes(tag);

					ret.getTags().put(tag.getId(), tag);
				}

				XXTagDefDao tagDefDao = daoManager.getXXTagDef();

				for (String tagType : tagTypes) {
					try {
						XXTagDef     xTagDef = tagDefDao.findByName(tagType);
						RangerTagDef tagDef  = xTagDef != null ? rangerTagDefService.getPopulatedViewObject(xTagDef) : null;

						if (tagDef != null) {
							RangerServiceTagsDeltaUtil.pruneUnusedAttributes(tagDef);

							ret.getTagDefinitions().put(tagDef.getId(), tagDef);
						} else {
							if (LOG.isDebugEnabled()) {
								LOG.debug("TagDBStore.createServiceTagsDelta(): failed to load tagDef type={}", tagType);
							}
						}
					} catch (Throwable t) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("TagDBStore.createServiceTagsDelta(): failed to load tagDef type={}", tagType, t);
						}
					}
				}

				for (Long serviceResourceId : serviceResourceIds) {
					// Check if serviceResourceId is part of any resource->id mapping
					XXServiceResource xServiceResource = null;
					try {
						xServiceResource = daoManager.getXXServiceResource().getById(serviceResourceId);
					} catch (Throwable t) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("TagDBStore.createServiceTagsDelta(): failed to read serviceResource id={}", serviceResourceId, t);
						}
					}

					final RangerServiceResource serviceResource;

					if (xServiceResource == null) {
						serviceResource = new RangerServiceResource();
						serviceResource.setId(serviceResourceId);
					} else {
						serviceResource = rangerServiceResourceService.getPopulatedViewObject(xServiceResource);

						if (StringUtils.isNotEmpty(xServiceResource.getTags())) {
							List<RangerTag> tags = RangerTagDBRetriever.gsonBuilder.fromJson(xServiceResource.getTags(), RangerServiceResourceService.duplicatedDataType);

							if (CollectionUtils.isNotEmpty(tags)) {
								List<Long> resourceTagIds = new ArrayList<>(tags.size());

								for (RangerTag tag : tags) {
									RangerServiceTagsDeltaUtil.pruneUnusedAttributes(tag);

									if (!ret.getTags().containsKey(tag.getId())) {
										ret.getTags().put(tag.getId(), tag);
									}

									resourceTagIds.add(tag.getId());
								}

								ret.getResourceToTagIds().put(serviceResourceId, resourceTagIds);
							}
						}
					}

					RangerServiceTagsDeltaUtil.pruneUnusedAttributes(serviceResource);

					ret.getServiceResources().add(serviceResource);
					tagsChangeExtent = ServiceTags.TagsChangeExtent.SERVICE_RESOURCE;
				}

				ret.setTagsChangeExtent(tagsChangeExtent);
			}
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("No tag-change-log records provided to createServiceTagsDelta()");
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagDBStore.createServiceTagsDelta() : serviceTagsDelta={" + ret + "}");
		}

		return ret;
	}

	private static void initStatics() {
		if (!IS_SUPPORTS_TAG_DELTAS_INITIALIZED) {
			RangerAdminConfig config = RangerAdminConfig.getInstance();

			SUPPORTS_TAG_DELTAS = config.getBoolean("ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUFFIX_TAG_DELTA, RangerCommonConstants.RANGER_ADMIN_SUFFIX_TAG_DELTA_DEFAULT);
			SUPPORTS_IN_PLACE_TAG_UPDATES    = SUPPORTS_TAG_DELTAS && config.getBoolean("ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUFFIX_IN_PLACE_TAG_UPDATES, RangerCommonConstants.RANGER_ADMIN_SUFFIX_IN_PLACE_TAG_UPDATES_DEFAULT);
			IS_SUPPORTS_TAG_DELTAS_INITIALIZED = true;
		}
	}

	public static boolean isSupportsTagDeltas() {
		initStatics();
        return SUPPORTS_TAG_DELTAS;
    }

	public boolean isInPlaceTagUpdateSupported() {
		initStatics();
		return SUPPORTS_IN_PLACE_TAG_UPDATES;
	}
}
