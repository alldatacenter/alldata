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

package org.apache.ranger.tagsync.source.atlas;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerTagDef;
import org.apache.ranger.plugin.model.RangerTagDef.RangerTagAttributeDef;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntityWithTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasNotificationMapper {
    private static final    int                 REPORTING_INTERVAL_FOR_UNHANDLED_ENTITYTYPE_IN_MILLIS = 5 * 60 * 1000; // 5 minutes

    private static final    Logger              LOG                 = LoggerFactory.getLogger(AtlasNotificationMapper.class);
    private static          Map<String, Long>   unhandledEventTypes = new HashMap<>();

    private static void logUnhandledEntityNotification(EntityNotificationWrapper entityNotification) {

        boolean skipLogging = entityNotification.getIsEntityCreateOp() && entityNotification.getIsEmptyClassifications();

        if (!skipLogging) {
            boolean loggingNeeded = false;

            String entityTypeName = entityNotification.getEntityTypeName();

            if (entityTypeName != null) {
                Long timeInMillis = unhandledEventTypes.get(entityTypeName);
                long currentTimeInMillis = System.currentTimeMillis();
                if (timeInMillis == null ||
                        (currentTimeInMillis - timeInMillis) >= REPORTING_INTERVAL_FOR_UNHANDLED_ENTITYTYPE_IN_MILLIS) {
                    unhandledEventTypes.put(entityTypeName, currentTimeInMillis);
                    loggingNeeded = true;
                }
            } else {
                LOG.error("EntityNotification contains NULL entity or NULL entity-type");
            }

            if (loggingNeeded) {
                if (!entityNotification.getIsEntityTypeHandled()) {
                    LOG.warn("Tag-sync is not enabled to handle notifications for Entity-type:[" + entityNotification.getEntityTypeName() + "]");
                }
                LOG.warn("Dropped process entity notification for Atlas-Entity [" + entityNotification.getRangerAtlasEntity() + "]");
            }

        }
    }

    public static ServiceTags processEntityNotification(EntityNotificationWrapper entityNotification) {

        ServiceTags ret = null;

        if (isNotificationHandled(entityNotification)) {
            try {
                RangerAtlasEntityWithTags entityWithTags = new RangerAtlasEntityWithTags(entityNotification);

                if (entityNotification.getIsEntityDeleteOp()) {
                    ret = buildServiceTagsForEntityDeleteNotification(entityWithTags);
                } else {
                    ret = buildServiceTags(entityWithTags, null);
                }

            } catch (Exception exception) {
                LOG.error("createServiceTags() failed!! ", exception);
            }
        } else {
            logUnhandledEntityNotification(entityNotification);
        }
        return ret;
    }

    public static Map<String, ServiceTags> processAtlasEntities(List<RangerAtlasEntityWithTags> atlasEntities) {
        Map<String, ServiceTags> ret = null;

        try {
            ret = buildServiceTags(atlasEntities);
        } catch (Exception exception) {
            LOG.error("Failed to build serviceTags", exception);
        }

        return ret;
    }

    static private boolean isNotificationHandled(EntityNotificationWrapper entityNotification) {
        boolean ret = false;

        EntityNotificationWrapper.NotificationOpType opType = entityNotification.getOpType();

        if (opType != null) {
            switch (opType) {
                case ENTITY_CREATE:
                    ret = entityNotification.getIsEntityActive() && !entityNotification.getIsEmptyClassifications();
                    if (!ret) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("ENTITY_CREATE notification is ignored, as there are no traits associated with the entity. Ranger will get necessary information from any subsequent TRAIT_ADDED notification");
                        }
                    }
                    break;
                case ENTITY_UPDATE:
                    ret = entityNotification.getIsEntityActive() && !entityNotification.getIsEmptyClassifications();
                    if (!ret) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("ENTITY_UPDATE notification is ignored, as there are no traits associated with the entity.");
                        }
                    }
                    break;
                case ENTITY_DELETE:
                    ret = true;
                    break;
                case CLASSIFICATION_ADD:
                case CLASSIFICATION_UPDATE:
                case CLASSIFICATION_DELETE: {
                    ret = entityNotification.getIsEntityActive();
                    break;
                }
                default:
                    LOG.error(opType + ": unknown notification received - not handled");
                    break;
            }
            if (ret) {
                ret = entityNotification.getIsEntityTypeHandled();
            }
            if (!ret) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Notification : [" + entityNotification + "] will NOT be processed.");
                }
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    static ServiceTags buildServiceTagsForEntityDeleteNotification(RangerAtlasEntityWithTags entityWithTags) {
        final ServiceTags ret;

        RangerAtlasEntity   entity = entityWithTags.getEntity();
        String              guid   = entity.getGuid();

        if (StringUtils.isNotBlank(guid)) {
            ret                                   = new ServiceTags();
            RangerServiceResource serviceResource = new RangerServiceResource();
            serviceResource.setGuid(guid);
            ret.getServiceResources().add(serviceResource);
        } else {
            ret = buildServiceTags(entityWithTags, null);
            if (ret != null) {
                // tag-definitions should NOT be deleted as part of service-resource delete
                ret.setTagDefinitions(MapUtils.EMPTY_MAP);
                // Ranger deletes tags associated with deleted service-resource
                ret.setTags(MapUtils.EMPTY_MAP);
            }
        }

        if (ret != null) {
            ret.setOp(ServiceTags.OP_DELETE);
        }

        return ret;
    }

    static private Map<String, ServiceTags> buildServiceTags(List<RangerAtlasEntityWithTags> entitiesWithTags) {

        Map<String, ServiceTags> ret = new HashMap<>();

        for (RangerAtlasEntityWithTags element : entitiesWithTags) {
            RangerAtlasEntity entity = element.getEntity();
            if (entity != null) {
                buildServiceTags(element, ret);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Ignoring entity because its State is not ACTIVE: " + element);
                }
            }
        }

        // Remove duplicate tag definitions
        if(CollectionUtils.isNotEmpty(ret.values())) {
            for (ServiceTags serviceTag : ret.values()) {
                if(MapUtils.isNotEmpty(serviceTag.getTagDefinitions())) {
                    Map<String, RangerTagDef> uniqueTagDefs = new HashMap<>();

                    for (RangerTagDef tagDef : serviceTag.getTagDefinitions().values()) {
                        RangerTagDef existingTagDef = uniqueTagDefs.get(tagDef.getName());

                        if (existingTagDef == null) {
                            uniqueTagDefs.put(tagDef.getName(), tagDef);
                        } else {
                            if(CollectionUtils.isNotEmpty(tagDef.getAttributeDefs())) {
                                for(RangerTagAttributeDef tagAttrDef : tagDef.getAttributeDefs()) {
                                    boolean attrDefExists = false;

                                    if(CollectionUtils.isNotEmpty(existingTagDef.getAttributeDefs())) {
                                        for(RangerTagAttributeDef existingTagAttrDef : existingTagDef.getAttributeDefs()) {
                                            if(StringUtils.equalsIgnoreCase(existingTagAttrDef.getName(), tagAttrDef.getName())) {
                                                attrDefExists = true;
                                                break;
                                            }
                                        }
                                    }

                                    if(! attrDefExists) {
                                        existingTagDef.getAttributeDefs().add(tagAttrDef);
                                    }
                                }
                            }
                        }
                    }

                    serviceTag.getTagDefinitions().clear();
                    for(RangerTagDef tagDef : uniqueTagDefs.values()) {
                        serviceTag.getTagDefinitions().put(tagDef.getId(), tagDef);
                    }
                }
            }
        }

        if (MapUtils.isNotEmpty(ret)) {
            for (Map.Entry<String, ServiceTags> entry : ret.entrySet()) {
                ServiceTags serviceTags = entry.getValue();
                serviceTags.setOp(ServiceTags.OP_REPLACE);
            }
        }
        return ret;
    }

    static private ServiceTags buildServiceTags(RangerAtlasEntityWithTags entityWithTags, Map<String, ServiceTags> serviceTagsMap) {
        ServiceTags             ret             = null;
        RangerAtlasEntity       entity          = entityWithTags.getEntity();
        RangerServiceResource  serviceResource  = AtlasResourceMapperUtil.getRangerServiceResource(entity);

        if (serviceResource != null) {

            List<RangerTag>    tags = getTags(entityWithTags);
            List<RangerTagDef> tagDefs = getTagDefs(entityWithTags);
            String             serviceName = serviceResource.getServiceName();

            ret = createOrGetServiceTags(serviceTagsMap, serviceName);

            serviceResource.setId((long) ret.getServiceResources().size());
            ret.getServiceResources().add(serviceResource);

            List<Long> tagIds = new ArrayList<>();

            if (CollectionUtils.isNotEmpty(tags)) {
                for (RangerTag tag : tags) {
                    tag.setId((long) ret.getTags().size());
                    ret.getTags().put(tag.getId(), tag);

                    tagIds.add(tag.getId());
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Entity " + entityWithTags + " does not have any tags associated with it");
                }
            }

            ret.getResourceToTagIds().put(serviceResource.getId(), tagIds);

            if (CollectionUtils.isNotEmpty(tagDefs)) {
                for (RangerTagDef tagDef : tagDefs) {
                    tagDef.setId((long) ret.getTagDefinitions().size());
                    ret.getTagDefinitions().put(tagDef.getId(), tagDef);
                }
            }
        } else {
            LOG.error("Failed to build serviceResource for entity:" + entity.getGuid());
        }

        return ret;
    }

    static private ServiceTags createOrGetServiceTags(Map<String, ServiceTags> serviceTagsMap, String serviceName) {
        ServiceTags ret = serviceTagsMap == null ? null : serviceTagsMap.get(serviceName);

        if (ret == null) {
            ret = new ServiceTags();

            if (serviceTagsMap != null) {
                serviceTagsMap.put(serviceName, ret);
            }

            ret.setOp(ServiceTags.OP_ADD_OR_UPDATE);
            ret.setServiceName(serviceName);
        }

        return ret;
    }

    static private List<RangerTag> getTags(RangerAtlasEntityWithTags entityWithTags) {
        List<RangerTag> ret = new ArrayList<>();

        if (entityWithTags != null && CollectionUtils.isNotEmpty(entityWithTags.getTags())) {
            List<EntityNotificationWrapper.RangerAtlasClassification> tags = entityWithTags.getTags();

            for (EntityNotificationWrapper.RangerAtlasClassification tag : tags) {
                RangerTag rangerTag = new RangerTag(null, tag.getName(), tag.getAttributes(), RangerTag.OWNER_SERVICERESOURCE);

                List<RangerValiditySchedule> validityPeriods = tag.getValidityPeriods();

                if (CollectionUtils.isNotEmpty(validityPeriods)) {
                    rangerTag.setValidityPeriods(validityPeriods);
                }
                ret.add(rangerTag);
            }
        }

        return ret;
    }

    static private List<RangerTagDef> getTagDefs(RangerAtlasEntityWithTags entityWithTags) {
        List<RangerTagDef> ret = new ArrayList<>();

        if (entityWithTags != null && CollectionUtils.isNotEmpty(entityWithTags.getTags())) {

            Map<String, String> tagNames = new HashMap<>();

            for (EntityNotificationWrapper.RangerAtlasClassification tag : entityWithTags.getTags()) {

                if (!tagNames.containsKey(tag.getName())) {
                    tagNames.put(tag.getName(), tag.getName());

                    RangerTagDef tagDef = new RangerTagDef(tag.getName(), "Atlas");
                    if (MapUtils.isNotEmpty(tag.getAttributes())) {
                        for (String attributeName : tag.getAttributes().keySet()) {
                            tagDef.getAttributeDefs().add(new RangerTagAttributeDef(attributeName, entityWithTags.getTagAttributeType(tag.getName(), attributeName)));
                        }
                    }
                    ret.add(tagDef);
                }
            }
        }

        return ret;
    }
}
