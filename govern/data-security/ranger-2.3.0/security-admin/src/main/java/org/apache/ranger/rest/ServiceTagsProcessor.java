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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.store.RangerServiceResourceSignature;
import org.apache.ranger.plugin.store.TagStore;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.ServiceTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceTagsProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(ServiceTagsProcessor.class);
	private static final Logger PERF_LOG_ADD_OR_UPDATE = RangerPerfTracer.getPerfLogger("tags.addOrUpdate");

	private final TagStore tagStore;

	public ServiceTagsProcessor(TagStore tagStore) {
		this.tagStore = tagStore;
	}

	public void process(ServiceTags serviceTags) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceTagsProcessor.process()");
		}

		if (tagStore != null && serviceTags != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("serviceTags:	op=" + serviceTags.getOp());
			}
			String op = serviceTags.getOp();

			if (StringUtils.equalsIgnoreCase(op, ServiceTags.OP_ADD_OR_UPDATE)) {
				addOrUpdate(serviceTags);
			} else if (StringUtils.equalsIgnoreCase(op, ServiceTags.OP_DELETE)) {
				delete(serviceTags);
			} else if (StringUtils.equalsIgnoreCase(op, ServiceTags.OP_REPLACE)) {
				replace(serviceTags);
			} else {
				LOG.error("Unknown op, op=" + op);
			}
		} else {
			if(tagStore == null) {
				LOG.error("tagStore is null!!");
			}

			if (serviceTags == null) {
				LOG.error("No ServiceTags to import!!");
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceTagsProcessor.process()");
		}
	}

	// Map tagdef, tag, serviceResource ids to created ids and use them in tag-resource-mapping
	private void addOrUpdate(ServiceTags serviceTags) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceTagsProcessor.createOrUpdate()");
		}

		RangerPerfTracer perfTotal = null;
		RangerPerfTracer perf      = null;

		Map<Long, RangerTagDef>          tagDefsInStore   = new HashMap<Long, RangerTagDef>();
		Map<Long, RangerServiceResource> resourcesInStore = new HashMap<Long, RangerServiceResource>();

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
			perfTotal = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.addOrUpdate()");
		}

		if (MapUtils.isNotEmpty(serviceTags.getTagDefinitions())) {
			RangerTagDef tagDef = null;

			try {
				for (Map.Entry<Long, RangerTagDef> entry : serviceTags.getTagDefinitions().entrySet()) {
					tagDef = entry.getValue();

					RangerTagDef existing = null;

					if(StringUtils.isNotEmpty(tagDef.getGuid())) {
						existing = tagStore.getTagDefByGuid(tagDef.getGuid());
					}

					if(existing == null && StringUtils.isNotEmpty(tagDef.getName())) {
						existing = tagStore.getTagDefByName(tagDef.getName());
					}

					RangerTagDef tagDefInStore = null;

					if(existing == null) {
						tagDefInStore = tagStore.createTagDef(tagDef);
					} else {
						if (LOG.isDebugEnabled()) {
							LOG.debug("tagDef for name:" + tagDef.getName() + " exists, will not update it");
						}
						tagDefInStore = existing;
					}

					tagDefsInStore.put(entry.getKey(), tagDefInStore);
				}
			} catch (Exception exception) {
				LOG.error("createTagDef failed, tagDef=" + tagDef, exception);
				throw exception;
			}
		}

		List<RangerServiceResource> resources = serviceTags.getServiceResources();
		if (CollectionUtils.isNotEmpty(resources)) {
			RangerServiceResource resource = null;

			try {
				for (int i = 0; i < resources.size(); i++) {
					resource = resources.get(i);

					RangerServiceResource existing          = null;
					String                resourceSignature = null;
					Long                  resourceId        = resource.getId();

					if(StringUtils.isNotEmpty(resource.getGuid())) {
						if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
							perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.search_service_resource_by_guid(" + resourceId + ")");
						}
						existing = tagStore.getServiceResourceByGuid(resource.getGuid());
						RangerPerfTracer.logAlways(perf);
					} else {
						if(MapUtils.isNotEmpty(resource.getResourceElements())) {
							if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
								perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.search_service_resource_by_signature(" + resourceId + ")");
							}
							RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(resource);

							resourceSignature = serializer.getSignature();
							resource.setResourceSignature(resourceSignature);

							existing = tagStore.getServiceResourceByServiceAndResourceSignature(resource.getServiceName(), resourceSignature);

							RangerPerfTracer.logAlways(perf);
						}
					}

					RangerServiceResource resourceInStore = null;

					if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
						perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.createOrUpdate_service_resource(" + resourceId + ")");
					}
					if (existing == null) {
						if (StringUtils.isBlank(resource.getServiceName())) {
							resource.setServiceName(serviceTags.getServiceName());
						}
						resourceInStore = tagStore.createServiceResource(resource);

					} else if (StringUtils.isEmpty(resource.getServiceName()) || MapUtils.isEmpty(resource.getResourceElements())) {
						resourceInStore = existing;
					} else {
						resource.setId(existing.getId());
						resource.setGuid(existing.getGuid());

						resourceInStore = tagStore.updateServiceResource(resource);
					}

					resourcesInStore.put(resourceId, resourceInStore);
					RangerPerfTracer.logAlways(perf);
				}
			} catch (Exception exception) {
				LOG.error("createServiceResource failed, resource=" + resource, exception);
				throw exception;
			}
		}

		if (MapUtils.isNotEmpty(serviceTags.getResourceToTagIds())) {
			for (Map.Entry<Long, List<Long>> entry : serviceTags.getResourceToTagIds().entrySet()) {
				Long resourceId = entry.getKey();

				RangerServiceResource resourceInStore = resourcesInStore.get(resourceId);

				if (resourceInStore == null) {
					LOG.error("Resource (id=" + resourceId + ") not found. Skipping tags update");
					continue;
				}

				if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
					perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.get_tags_for_service_resource(" + resourceInStore.getId() + ")");
				}

				// Get all tags associated with this resourceId
				List<RangerTag> associatedTags = null;

				try {
					associatedTags = tagStore.getTagsForResourceId(resourceInStore.getId());
				} catch (Exception exception) {
					LOG.error("RangerTags cannot be retrieved for resource with guid=" + resourceInStore.getGuid());
					throw exception;
				} finally {
					RangerPerfTracer.logAlways(perf);
				}

				List<RangerTag> tagsToRetain = new ArrayList<RangerTag>();
				boolean         isAnyTagUpdated = false;

				List<Long> tagIds = entry.getValue();
				try {
					for (Long tagId : tagIds) {
						RangerTag incomingTag = MapUtils.isNotEmpty(serviceTags.getTags()) ? serviceTags.getTags().get(tagId) : null;

						if (incomingTag == null) {
							LOG.error("Tag (id=" + tagId + ") not found. Skipping addition of this tag for resource (id=" + resourceId + ")");
							continue;
						}

						RangerTag matchingTag = findMatchingTag(incomingTag, associatedTags);
						if (matchingTag == null) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("Did not find matching tag for tagId=" + tagId);
							}
							// create new tag from incoming tag and associate it with service-resource
							if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
								perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.create_tag(" + tagId + ")");
							}
							RangerTag newTag = tagStore.createTag(incomingTag);
							RangerPerfTracer.logAlways(perf);

							RangerTagResourceMap tagResourceMap = new RangerTagResourceMap();

							tagResourceMap.setTagId(newTag.getId());
							tagResourceMap.setResourceId(resourceInStore.getId());
							if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
								perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.create_tagResourceMap(" + tagId + ")");
							}
							tagResourceMap = tagStore.createTagResourceMap(tagResourceMap);
							RangerPerfTracer.logAlways(perf);

							associatedTags.add(newTag);
							tagsToRetain.add(newTag);

						} else {

							if (LOG.isDebugEnabled()) {
								LOG.debug("Found matching tag for tagId=" + tagId + ", matchingTag=" + matchingTag);
							}

							if (isResourcePrivateTag(incomingTag)) {
								if (!isResourcePrivateTag(matchingTag)) {
									// create new tag from incoming tag and associate it with service-resource
									RangerTag newTag = tagStore.createTag(incomingTag);

									RangerTagResourceMap tagResourceMap = new RangerTagResourceMap();

									tagResourceMap.setTagId(newTag.getId());
									tagResourceMap.setResourceId(resourceInStore.getId());

									tagResourceMap = tagStore.createTagResourceMap(tagResourceMap);

									associatedTags.add(newTag);
									tagsToRetain.add(newTag);

								} else {
									tagsToRetain.add(matchingTag);

									boolean isTagUpdateNeeded = false;

									// Note that as there is no easy way to check validityPeriods for equality, an easy way to rule out the possibility of validityPeriods
									// not matching is to check if both old and new tags have empty validityPeriods
									if (matchingTag.getGuid() != null && matchingTag.getGuid().equals(incomingTag.getGuid())) {
										if (isMatch(incomingTag, matchingTag) && CollectionUtils.isEmpty(incomingTag.getValidityPeriods()) && CollectionUtils.isEmpty(matchingTag.getValidityPeriods())) {
											if (LOG.isDebugEnabled()) {
												LOG.debug("No need to update existing-tag:[" + matchingTag + "] with incoming-tag:[" + incomingTag + "]");
											}
										} else {
											isTagUpdateNeeded = true;
										}
									} else {
										if (CollectionUtils.isEmpty(incomingTag.getValidityPeriods()) && CollectionUtils.isEmpty(matchingTag.getValidityPeriods())) {
											// Completely matched tags. No need to update
											if (LOG.isDebugEnabled()) {
												LOG.debug("No need to update existing-tag:[" + matchingTag + "] with incoming-tag:[" + incomingTag + "]");
											}
										} else {
											isTagUpdateNeeded = true;
										}
									}
									if (isTagUpdateNeeded) {
											// Keep this tag, and update it with attribute-values and validity schedules from incoming tag
											if (LOG.isDebugEnabled()) {
												LOG.debug("Updating existing private tag with id=" + matchingTag.getId());
											}
											incomingTag.setId(matchingTag.getId());
											tagStore.updateTag(incomingTag);
											isAnyTagUpdated = true;
									}
								}
							} else { // shared model
								if (isResourcePrivateTag(matchingTag)) {
									// create new tag from incoming tag and associate it with service-resource
									RangerTag newTag = tagStore.createTag(incomingTag);

									RangerTagResourceMap tagResourceMap = new RangerTagResourceMap();

									tagResourceMap.setTagId(newTag.getId());
									tagResourceMap.setResourceId(resourceInStore.getId());

									tagResourceMap = tagStore.createTagResourceMap(tagResourceMap);

									associatedTags.add(newTag);
									tagsToRetain.add(newTag);

								} else {
									// Keep this tag, but update it with attribute-values from incoming tag
									tagsToRetain.add(matchingTag);

									// Update shared tag with new values
									incomingTag.setId(matchingTag.getId());
									tagStore.updateTag(incomingTag);

									// associate with service-resource if not already associated
									if (findTagInList(matchingTag, associatedTags) == null) {
										RangerTagResourceMap tagResourceMap = new RangerTagResourceMap();

										tagResourceMap.setTagId(matchingTag.getId());
										tagResourceMap.setResourceId(resourceInStore.getId());

										tagResourceMap = tagStore.createTagResourceMap(tagResourceMap);
									} else {
										isAnyTagUpdated = true;
									}

								}
							}

						}
					}

				} catch (Exception exception) {
					LOG.error("createRangerTagResourceMap failed", exception);
					throw exception;
				}

				if (CollectionUtils.isNotEmpty(associatedTags)) {
					Long tagId = null;

					try {
						for (RangerTag associatedTag : associatedTags) {
							if (findTagInList(associatedTag, tagsToRetain) == null) {

								tagId = associatedTag.getId();

								RangerTagResourceMap tagResourceMap = tagStore.getTagResourceMapForTagAndResourceId(tagId, resourceInStore.getId());

								if (tagResourceMap != null) {
									tagStore.deleteTagResourceMap(tagResourceMap.getId());
								}

								if (LOG.isDebugEnabled()) {
									LOG.debug("Deleted tagResourceMap(tagId=" + tagId + ", resourceId=" + resourceInStore.getId());
								}
							}
						}
					} catch(Exception exception) {
						LOG.error("deleteTagResourceMap failed, tagId=" + tagId + ", resourceId=" + resourceInStore.getId());
						throw exception;
					}
				}
				if (isAnyTagUpdated) {
					if(RangerPerfTracer.isPerfTraceEnabled(PERF_LOG_ADD_OR_UPDATE)) {
						perf = RangerPerfTracer.getPerfTracer(PERF_LOG_ADD_OR_UPDATE, "tags.refreshServiceResource(" + resourceInStore.getId() + ")");
					}
					tagStore.refreshServiceResource(resourceInStore.getId());
					RangerPerfTracer.logAlways(perf);
				}
			}
		}

		RangerPerfTracer.logAlways(perfTotal);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceTagsProcessor.createOrUpdate()");
		}
	}

	private RangerTag findTagInList(RangerTag object, List<RangerTag> list) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceTagsProcessor.findTagInList(): object=" + (object == null ? null : object.getId()));
		}
		RangerTag ret = null;
		if (object != null) {
			for (RangerTag tag : list) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("==> ServiceTagsProcessor.findTagInList(): tag=" + tag.getId());
				}
				if (tag.getId().equals(object.getId())) {
					ret = tag;
					if (LOG.isDebugEnabled()) {
						LOG.debug("==> ServiceTagsProcessor.findTagInList(): found tag=" + tag.getId());
					}
					break;
				}
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceTagsProcessor.findTagInList(): ret=" + (ret == null ? null : ret.getId()));
		}
		return ret;
	}
	private boolean isResourcePrivateTag(RangerTag tag) {
		return tag.getOwner() == RangerTag.OWNER_SERVICERESOURCE;
	}

	private RangerTag findMatchingTag(RangerTag incomingTag, List<RangerTag> existingTags) throws Exception {

		RangerTag ret = null;

		if(StringUtils.isNotEmpty(incomingTag.getGuid())) {
			ret = tagStore.getTagByGuid(incomingTag.getGuid());
		}

		if (ret == null) {

			if (isResourcePrivateTag(incomingTag)) {

				for (RangerTag existingTag : existingTags) {
					if (isMatch(incomingTag, existingTag)) {
						ret = existingTag;
						break;
					}
				}
			}

		}

		return ret;
	}

	private boolean isMatch(final RangerTag incomingTag, final RangerTag existingTag) {
		boolean ret = false;

		if (incomingTag != null && existingTag != null) {

			if (StringUtils.equals(incomingTag.getType(), existingTag.getType())) {

				// Check attribute values
				Map<String, String> incomingTagAttributes = incomingTag.getAttributes();
				Map<String, String> existingTagAttributes = existingTag.getAttributes();

				if (CollectionUtils.isEqualCollection(incomingTagAttributes.keySet(), existingTagAttributes.keySet())) {

					boolean matched = true;

					for (Map.Entry<String, String> entry : incomingTagAttributes.entrySet()) {

						String key = entry.getKey();
						String value = entry.getValue();

						if (!StringUtils.equals(value, existingTagAttributes.get(key))) {
							matched = false;
							break;
						}

					}
					if (matched) {
						ret = true;
					}
				}

			}
		}
		return ret;
	}

	private void delete(ServiceTags serviceTags) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceTagsProcessor.delete()");
		}

		// We dont expect any resourceId->tagId mappings in delete operation, so ignoring them if specified

		List<RangerServiceResource> serviceResources = serviceTags.getServiceResources();
		if (CollectionUtils.isNotEmpty(serviceResources)) {

			for (RangerServiceResource serviceResource : serviceResources) {

				RangerServiceResource objToDelete = null;

				try {

					if (StringUtils.isNotBlank(serviceResource.getGuid())) {
						objToDelete = tagStore.getServiceResourceByGuid(serviceResource.getGuid());
					}

					if (objToDelete == null) {
						if (MapUtils.isNotEmpty(serviceResource.getResourceElements())) {
							RangerServiceResourceSignature serializer = new RangerServiceResourceSignature(serviceResource);

							String serviceResourceSignature = serializer.getSignature();

							objToDelete = tagStore.getServiceResourceByServiceAndResourceSignature(serviceResource.getServiceName(), serviceResourceSignature);
						}
					}

					if (objToDelete != null) {

						List<RangerTagResourceMap> tagResourceMaps = tagStore.getTagResourceMapsForResourceGuid(objToDelete.getGuid());

						if (CollectionUtils.isNotEmpty(tagResourceMaps)) {
							for (RangerTagResourceMap tagResourceMap : tagResourceMaps) {
								tagStore.deleteTagResourceMap(tagResourceMap.getId());
							}
						}

						tagStore.deleteServiceResource(objToDelete.getId());
					}
				} catch (Exception exception) {
					LOG.error("deleteServiceResourceByGuid failed, guid=" + serviceResource.getGuid(), exception);
					throw exception;
				}
			}
		}

		Map<Long, RangerTag> tagsMap = serviceTags.getTags();
		if (MapUtils.isNotEmpty(tagsMap)) {
			for (Map.Entry<Long, RangerTag> entry : tagsMap.entrySet()) {
				RangerTag tag = entry.getValue();
				try {
					RangerTag objToDelete = tagStore.getTagByGuid(tag.getGuid());

					if (objToDelete != null) {
						tagStore.deleteTag(objToDelete.getId());
					}
				} catch (Exception exception) {
					LOG.error("deleteTag failed, guid=" + tag.getGuid(), exception);
					throw exception;
				}
			}
		}

		Map<Long, RangerTagDef> tagDefsMap = serviceTags.getTagDefinitions();
		if (MapUtils.isNotEmpty(tagDefsMap)) {
			for (Map.Entry<Long, RangerTagDef> entry : tagDefsMap.entrySet()) {
				RangerTagDef tagDef = entry.getValue();
				try {
					RangerTagDef objToDelete = tagStore.getTagDefByGuid(tagDef.getGuid());

					if(objToDelete != null) {
						tagStore.deleteTagDef(objToDelete.getId());
					}
				} catch (Exception exception) {
					LOG.error("deleteTagDef failed, guid=" + tagDef.getGuid(), exception);
					throw exception;
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceTagsProcessor.delete()");
		}
	}

	private void replace(ServiceTags serviceTags) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> ServiceTagsProcessor.replace()");
		}

		// Delete those service-resources which are in ranger database but not in provided service-tags

		Map<String, RangerServiceResource> serviceResourcesInServiceTagsMap = new HashMap<String, RangerServiceResource>();

		List<RangerServiceResource> serviceResourcesInServiceTags = serviceTags.getServiceResources();

		for (RangerServiceResource rangerServiceResource : serviceResourcesInServiceTags) {
			String guid = rangerServiceResource.getGuid();

			if(serviceResourcesInServiceTagsMap.containsKey(guid)) {
				LOG.warn("duplicate service-resource found: guid=" + guid);
			}

			serviceResourcesInServiceTagsMap.put(guid, rangerServiceResource);
		}

		List<String> serviceResourcesInDb = tagStore.getServiceResourceGuidsByService(serviceTags.getServiceName());

		if (CollectionUtils.isNotEmpty(serviceResourcesInDb)) {
			for (String dbServiceResourceGuid : serviceResourcesInDb) {

				if (!serviceResourcesInServiceTagsMap.containsKey(dbServiceResourceGuid)) {

					if (LOG.isDebugEnabled()) {
						LOG.debug("Deleting serviceResource(guid=" + dbServiceResourceGuid + ") and its tag-associations...");
					}

					List<RangerTagResourceMap> tagResourceMaps = tagStore.getTagResourceMapsForResourceGuid(dbServiceResourceGuid);

					if (CollectionUtils.isNotEmpty(tagResourceMaps)) {
						for (RangerTagResourceMap tagResourceMap : tagResourceMaps) {
							tagStore.deleteTagResourceMap(tagResourceMap.getId());
						}
					}

					tagStore.deleteServiceResourceByGuid(dbServiceResourceGuid);
				}

			}
		}

		// Add/update resources and other tag-model objects provided in service-tags

		addOrUpdate(serviceTags);

		// All private tags at this point are associated with some service-resource and shared
		// tags cannot be deleted as they belong to some other service. In any case, any tags that
		// are not associated with service-resource will not be downloaded to plugin.

		// Tag-defs cannot be deleted as there may be a shared tag that it refers to it.

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== ServiceTagsProcessor.replace()");
		}
	}
}
