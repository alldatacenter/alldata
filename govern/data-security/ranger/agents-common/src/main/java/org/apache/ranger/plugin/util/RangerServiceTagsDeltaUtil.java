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

package org.apache.ranger.plugin.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RangerServiceTagsDeltaUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RangerServiceTagsDeltaUtil.class);

    private static final Logger PERF_TAGS_DELTA_LOG = RangerPerfTracer.getPerfLogger("tags.delta");

    private static boolean SUPPORTS_TAGS_DEDUP_INITIALIZED = false;
    private static boolean SUPPORTS_TAGS_DEDUP             = false;

    /*
    It should be possible to call applyDelta() multiple times with serviceTags and delta resulting from previous call to applyDelta()
    The end result should be same if called once or multiple times.
     */
    static public ServiceTags applyDelta(ServiceTags serviceTags, ServiceTags delta) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerServiceTagsDeltaUtil.applyDelta()");
        }

        ServiceTags      ret  = serviceTags;
        RangerPerfTracer perf = null;

        if(RangerPerfTracer.isPerfTraceEnabled(PERF_TAGS_DELTA_LOG)) {
            perf = RangerPerfTracer.getPerfTracer(PERF_TAGS_DELTA_LOG, "RangerServiceTagsDeltaUtil.applyDelta()");
        }

        if (serviceTags != null && !serviceTags.getIsDelta() && delta != null && delta.getIsDelta()) {
            ret = new ServiceTags(serviceTags);

            ret.setServiceName(delta.getServiceName());
            ret.setTagVersion(delta.getTagVersion());

            int tagDefsAdded = 0, tagDefsUpdated = 0, tagDefsRemoved = 0;
            int tagsAdded    = 0, tagsUpdated    = 0, tagsRemoved    = 0;

            Map<Long, RangerTagDef> tagDefs = ret.getTagDefinitions();

            for (Iterator<Map.Entry<Long, RangerTagDef>> deltaTagDefIter = delta.getTagDefinitions().entrySet().iterator(); deltaTagDefIter.hasNext(); ) {
                Map.Entry<Long, RangerTagDef> entry         = deltaTagDefIter.next();
                Long                          deltaTagDefId = entry.getKey();
                RangerTagDef                  deltaTagDef   = entry.getValue();

                if (StringUtils.isEmpty(deltaTagDef.getName())) { // tagdef has been removed
                    RangerTagDef removedTagDef = tagDefs.remove(deltaTagDefId);

                    if (removedTagDef != null) {
                        tagDefsRemoved++;
                    }
                }

                RangerTagDef existing = tagDefs.put(deltaTagDefId, deltaTagDef);

                if (existing == null) {
                    tagDefsAdded++;
                } else if (!existing.equals(deltaTagDef)) {
                    tagDefsUpdated++;
                }
            }

            Map<Long, RangerTag> tags           = ret.getTags();
            Map<Long, Long>      replacedTagIds = new HashMap<>();

            for (Iterator<Map.Entry<Long, RangerTag>> deltaTagIter = delta.getTags().entrySet().iterator(); deltaTagIter.hasNext(); ) {
                Map.Entry<Long, RangerTag> entry      = deltaTagIter.next();
                Long                       deltaTagId = entry.getKey();
                RangerTag                  deltaTag   = entry.getValue();

                if (StringUtils.isEmpty(deltaTag.getType())) { // tag has been removed
                    RangerTag removedTag = tags.remove(deltaTagId);

                    if (removedTag != null) {
                        tagsRemoved++;

                        if (isSupportsTagsDedup()) {
                            ret.cachedTags.remove(removedTag);
                        }
                    }
                } else {
                    if (isSupportsTagsDedup()) {
                        Long cachedTagId = ret.cachedTags.get(deltaTag);

                        if (cachedTagId == null) {
                            ret.cachedTags.put(deltaTag, deltaTagId);
                            tags.put(deltaTagId, deltaTag);
                        } else {
                            replacedTagIds.put(deltaTagId, cachedTagId);
                            deltaTagIter.remove();
                        }
                    } else {
                        RangerTag existing = tags.put(deltaTagId, deltaTag);

                        if (existing == null) {
                            tagsAdded++;
                        } else if (!existing.equals(deltaTag)) {
                            tagsUpdated++;
                        }
                    }
                }
            }

            List<RangerServiceResource>      serviceResources  = ret.getServiceResources();
            Map<Long, List<Long>>            resourceToTagIds  = ret.getResourceToTagIds();
            Map<Long, RangerServiceResource> idResourceMap     = serviceResources.stream().collect(Collectors.toMap(RangerServiceResource::getId, Function.identity()));
            Map<Long, RangerServiceResource> resourcesToRemove = new HashMap<>();
            Map<Long, RangerServiceResource> resourcesToAdd    = new HashMap<>();

            for (RangerServiceResource resource : delta.getServiceResources()) {
                RangerServiceResource existingResource = idResourceMap.get(resource.getId());

                if (existingResource != null) {
                    if (StringUtils.isNotEmpty(resource.getResourceSignature())) {
                        if (!StringUtils.equals(resource.getResourceSignature(), existingResource.getResourceSignature())) {  // ServiceResource changed; replace existing instance
                            existingResource.setResourceSignature(null);

                            boolean isAddedResource = resourcesToAdd.remove(existingResource.getId()) == existingResource;

                            // if a resource with this ID was already removed, don't replace the entry in resourcesToRemove
                            if (!isAddedResource && !resourcesToRemove.containsKey(existingResource.getId())) {
                                resourcesToRemove.put(existingResource.getId(), existingResource);
                            }

                            resourcesToAdd.put(resource.getId(), resource);
                            idResourceMap.put(resource.getId(), resource);
                        }
                    } else { // resource deleted
                        boolean isAddedResource = resourcesToAdd.remove(existingResource.getId()) == existingResource;

                        if (!isAddedResource) {
                            resourcesToRemove.put(existingResource.getId(), existingResource);
                        }

                        idResourceMap.remove(existingResource.getId());
                        resourceToTagIds.remove(existingResource.getId());
                    }
                } else { // resource added
                    if (StringUtils.isNotEmpty(resource.getResourceSignature())) {
                        resourcesToAdd.put(resource.getId(), resource);

                        idResourceMap.put(resource.getId(), resource);
                    }
                }
            }

            if (!resourcesToRemove.isEmpty()) {
                for (ListIterator<RangerServiceResource> iter = serviceResources.listIterator(); iter.hasNext(); ) {
                    RangerServiceResource resource         = iter.next();
                    RangerServiceResource replacedResource = resourcesToRemove.get(resource.getId());

                    if (replacedResource == resource) {
                        iter.remove();
                    }
                }
            }

            serviceResources.addAll(resourcesToAdd.values());

            if (!replacedTagIds.isEmpty()) {
                for (Map.Entry<Long, List<Long>> resourceEntry : delta.getResourceToTagIds().entrySet()) {
                    ListIterator<Long> listIter = resourceEntry.getValue().listIterator();

                    while (listIter.hasNext()) {
                        Long tagId = listIter.next();
                        Long replacerTagId = replacedTagIds.get(tagId);

                        if (replacerTagId != null) {
                            listIter.set(replacerTagId);
                        }
                    }
                }
            }

            resourceToTagIds.putAll(delta.getResourceToTagIds());

            // Ensure that any modified service-resources are at head of list of service-resources in delta
            // So that in setServiceTags(), they get cleaned out first, and service-resource with new spec gets added
            if (!resourcesToAdd.isEmpty()) {
                List<RangerServiceResource> deltaServiceResources = new ArrayList<>(resourcesToAdd.values());

                deltaServiceResources.addAll(delta.getServiceResources());

                delta.setServiceResources(deltaServiceResources);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("RangerServiceTagsDeltaUtil.applyDelta(): delta(tagDefs={}, tags={}, resources={}), " +
                          "resources(total={}, added={}, removed={}), " +
                          "tags(total={}, added={}, updated={}, removed={}), " +
                          "tagDefs(total={}, added={}, updated={}, removed={})",
                        delta.getTagDefinitions().size(), delta.getTags().size(), delta.getServiceResources().size(),
                        serviceResources.size(), resourcesToAdd.size(), resourcesToRemove.size(),
                        tags.size(), tagsAdded, tagsUpdated, tagsRemoved,
                        tagDefs.size(), tagDefsAdded, tagDefsUpdated, tagDefsRemoved);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot apply deltas to service-tags as one of preconditions is violated. Returning received serviceTags without applying delta!!");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerServiceTagsDeltaUtil.applyDelta()");
        }

        RangerPerfTracer.log(perf);

        return ret;
    }

    public static void pruneUnusedAttributes(ServiceTags serviceTags) {
        if (serviceTags != null) {
            serviceTags.setTagUpdateTime(null);

            for (Map.Entry<Long, RangerTagDef> entry : serviceTags.getTagDefinitions().entrySet()) {
                pruneUnusedAttributes(entry.getValue());
            }

            for (Map.Entry<Long, RangerTag> entry : serviceTags.getTags().entrySet()) {
                pruneUnusedAttributes(entry.getValue());
            }

            for (RangerServiceResource serviceResource : serviceTags.getServiceResources()) {
                pruneUnusedAttributes(serviceResource);
            }
        }
    }

    public static void pruneUnusedAttributes(RangerTagDef tagDef) {
        tagDef.setCreatedBy(null);
        tagDef.setCreateTime(null);
        tagDef.setUpdatedBy(null);
        tagDef.setUpdateTime(null);
        tagDef.setGuid(null);
        tagDef.setVersion(null);

        if (tagDef.getAttributeDefs() != null && tagDef.getAttributeDefs().isEmpty()) {
            tagDef.setAttributeDefs(null);
        }
    }

    public static void pruneUnusedAttributes(RangerTag tag) {
        tag.setCreatedBy(null);
        tag.setCreateTime(null);
        tag.setUpdatedBy(null);
        tag.setUpdateTime(null);
        tag.setGuid(null);
        tag.setVersion(null);

        if (tag.getOwner() != null && tag.getOwner().shortValue() == RangerTag.OWNER_SERVICERESOURCE) {
            tag.setOwner(null);
        }

        if (tag.getAttributes() != null && tag.getAttributes().isEmpty()) {
            tag.setAttributes(null);
        }

        if (tag.getOptions() != null && tag.getOptions().isEmpty()) {
            tag.setOptions(null);
        }

        if (tag.getValidityPeriods() != null && tag.getValidityPeriods().isEmpty()) {
            tag.setValidityPeriods(null);
        }
    }

    public static void pruneUnusedAttributes(RangerServiceResource serviceResource) {
        serviceResource.setCreatedBy(null);
        serviceResource.setCreateTime(null);
        serviceResource.setUpdatedBy(null);
        serviceResource.setUpdateTime(null);
        serviceResource.setGuid(null);
        serviceResource.setVersion(null);

        if (serviceResource.getAdditionalInfo() != null && serviceResource.getAdditionalInfo().isEmpty()) {
            serviceResource.setAdditionalInfo(null);
        }
    }

    public static boolean isSupportsTagsDedup() {
        if (!SUPPORTS_TAGS_DEDUP_INITIALIZED) {
            RangerAdminConfig config = RangerAdminConfig.getInstance();

            SUPPORTS_TAGS_DEDUP = config.getBoolean("ranger.admin" + RangerCommonConstants.RANGER_ADMIN_SUPPORTS_TAGS_DEDUP, RangerCommonConstants.RANGER_ADMIN_SUPPORTS_TAGS_DEDUP_DEFAULT);
            SUPPORTS_TAGS_DEDUP_INITIALIZED = true;
        }
        return SUPPORTS_TAGS_DEDUP;
    }
}
