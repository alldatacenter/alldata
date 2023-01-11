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

import org.apache.atlas.model.TimeBoundary;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.notification.EntityNotification;
import org.apache.atlas.model.notification.EntityNotification.EntityNotificationV2;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.notification.EntityNotificationV1;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityNotificationWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(EntityNotificationWrapper.class);

	public enum NotificationOpType { UNKNOWN, ENTITY_CREATE, ENTITY_UPDATE, ENTITY_DELETE, CLASSIFICATION_ADD, CLASSIFICATION_UPDATE, CLASSIFICATION_DELETE}

	public static class RangerAtlasClassification {
		private final String                       name;
		private final Map<String, String>          attributes;
		private final List<RangerValiditySchedule> validityPeriods;

		public RangerAtlasClassification(String name, Map<String, String> attributes, List<RangerValiditySchedule> validityPeriods) {
			this.name            = name;
			this.attributes      = attributes;
			this.validityPeriods = validityPeriods;
		}
		public String getName() {
			return name;
		}
		public Map<String, String> getAttributes() {
			return attributes;
		}
		public List<RangerValiditySchedule> getValidityPeriods() {
			return validityPeriods;
		}

	}
    private final RangerAtlasEntity                         rangerAtlasEntity;
    private final String                                    entityTypeName;
    private final boolean                                   isEntityActive;
    private final boolean                                   isEntityTypeHandled;
    private final boolean                                   isEntityDeleteOp;
    private final boolean                                   isEntityCreateOp;
    private final boolean                                   isEmptyClassifications;
    private final List<RangerAtlasClassification>           classifications;
    private final NotificationOpType                        opType;

    EntityNotificationWrapper(@Nonnull EntityNotification notification) {
        EntityNotification.EntityNotificationType notificationType = notification.getType();

        switch (notificationType) {
            case ENTITY_NOTIFICATION_V2: {
                EntityNotificationV2 v2Notification = (EntityNotificationV2) notification;
                AtlasEntityHeader    atlasEntity    = v2Notification.getEntity();
                String               guid           = atlasEntity.getGuid();
                String               typeName       = atlasEntity.getTypeName();

                rangerAtlasEntity      = new RangerAtlasEntity(typeName, guid, atlasEntity.getAttributes());
                entityTypeName         = atlasEntity.getTypeName();
                isEntityActive         = atlasEntity.getStatus() == AtlasEntity.Status.ACTIVE;
                isEntityTypeHandled    = AtlasResourceMapperUtil.isEntityTypeHandled(entityTypeName);
                isEntityDeleteOp       = EntityNotificationV2.OperationType.ENTITY_DELETE == v2Notification.getOperationType();
                isEntityCreateOp       = EntityNotificationV2.OperationType.ENTITY_CREATE == v2Notification.getOperationType();
                isEmptyClassifications = CollectionUtils.isEmpty(atlasEntity.getClassifications());

                List<AtlasClassification> allClassifications = atlasEntity.getClassifications();

                if (CollectionUtils.isNotEmpty(allClassifications)) {
                    classifications                = new ArrayList<>();

                    for (AtlasClassification classification : allClassifications) {
                        String classificationName = classification.getTypeName();

                        Map<String, Object> valuesMap  = classification.getAttributes();
                        Map<String, String> attributes = new HashMap<>();

                        if (valuesMap != null) {
                            for (Map.Entry<String, Object> value : valuesMap.entrySet()) {
                                if (value.getValue() != null) {
                                    attributes.put(value.getKey(), value.getValue().toString());
                                }
                            }
                        }

                        List<RangerValiditySchedule> validitySchedules = null;
                        List<TimeBoundary> validityPeriods = classification.getValidityPeriods();

                        if (CollectionUtils.isNotEmpty(validityPeriods)) {
                            validitySchedules = convertTimeSpecFromAtlasToRanger(validityPeriods);
                        }
                        classifications.add(new RangerAtlasClassification(classificationName, attributes, validitySchedules));
                    }
                } else {
                    classifications                = null;
                }

                EntityNotificationV2.OperationType operationType = v2Notification.getOperationType();
                switch (operationType) {
                    case ENTITY_CREATE:
                        opType = NotificationOpType.ENTITY_CREATE;
                        break;
                    case ENTITY_UPDATE:
                        opType = NotificationOpType.ENTITY_UPDATE;
                        break;
                    case ENTITY_DELETE:
                        opType = NotificationOpType.ENTITY_DELETE;
                        break;
                    case CLASSIFICATION_ADD:
                        opType = NotificationOpType.CLASSIFICATION_ADD;
                        break;
                    case CLASSIFICATION_UPDATE:
                        opType = NotificationOpType.CLASSIFICATION_UPDATE;
                        break;
                    case CLASSIFICATION_DELETE:
                        opType = NotificationOpType.CLASSIFICATION_DELETE;
                        break;
                    default:
                        LOG.error("Received OperationType [" + operationType + "], converting to UNKNOWN");
                        opType = NotificationOpType.UNKNOWN;
                        break;
                }
            }
            break;

            case ENTITY_NOTIFICATION_V1: {
                EntityNotificationV1 v1Notification = (EntityNotificationV1) notification;
                Referenceable        atlasEntity    = v1Notification.getEntity();
                String               guid           = atlasEntity.getId()._getId();
                String               typeName       = atlasEntity.getTypeName();

                rangerAtlasEntity      = new RangerAtlasEntity(typeName, guid, atlasEntity.getValues());
                entityTypeName         = atlasEntity.getTypeName();
                isEntityActive         = atlasEntity.getId().getState() == Id.EntityState.ACTIVE;
                isEntityTypeHandled    = AtlasResourceMapperUtil.isEntityTypeHandled(entityTypeName);
                isEntityDeleteOp       = EntityNotificationV1.OperationType.ENTITY_DELETE == v1Notification.getOperationType();
                isEntityCreateOp       = EntityNotificationV1.OperationType.ENTITY_CREATE == v1Notification.getOperationType();
                isEmptyClassifications = CollectionUtils.isEmpty(v1Notification.getAllTraits());

                List<Struct> allTraits = ((EntityNotificationV1) notification).getAllTraits();

                if (CollectionUtils.isNotEmpty(allTraits)) {
                    classifications = new ArrayList<>();

                    for (Struct trait : allTraits) {
                        String              traitName  = trait.getTypeName();
                        Map<String, Object> valuesMap  = trait.getValuesMap();
                        Map<String, String> attributes = new HashMap<>();

                        if (valuesMap != null) {
                            for (Map.Entry<String, Object> value : valuesMap.entrySet()) {
                                if (value.getValue() != null) {
                                    attributes.put(value.getKey(), value.getValue().toString());
                                }
                            }
                        }

                        classifications.add(new RangerAtlasClassification(traitName, attributes, null));
                    }
                } else {
                    classifications = null;
                }

                EntityNotificationV1.OperationType operationType = v1Notification.getOperationType();
                switch (operationType) {
                    case ENTITY_CREATE:
                        opType = NotificationOpType.ENTITY_CREATE;
                        break;
                    case ENTITY_UPDATE:
                        opType = NotificationOpType.ENTITY_UPDATE;
                        break;
                    case ENTITY_DELETE:
                        opType = NotificationOpType.ENTITY_DELETE;
                        break;
                    case TRAIT_ADD:
                        opType = NotificationOpType.CLASSIFICATION_ADD;
                        break;
                    case TRAIT_UPDATE:
                        opType = NotificationOpType.CLASSIFICATION_UPDATE;
                        break;
                    case TRAIT_DELETE:
                        opType = NotificationOpType.CLASSIFICATION_DELETE;
                        break;
                    default:
                        LOG.error("Received OperationType [" + operationType + "], converting to UNKNOWN");
                        opType = NotificationOpType.UNKNOWN;
                        break;
                }
            }
            break;

            default: {
                LOG.error("Unknown notification type - [" + notificationType + "]");

                rangerAtlasEntity              = null;
                entityTypeName                 = null;
                isEntityActive                 = false;
                isEntityTypeHandled            = false;
                isEntityDeleteOp               = false;
                isEntityCreateOp               = false;
                isEmptyClassifications         = true;
                classifications                = null;
                opType                         = NotificationOpType.UNKNOWN;
            }

            break;
        }
    }

	public RangerAtlasEntity getRangerAtlasEntity() {
		return rangerAtlasEntity;
	}

	public String getEntityTypeName() {
		return entityTypeName;
	}

	public boolean getIsEntityTypeHandled() {
		return isEntityTypeHandled;
	}

	public boolean getIsEntityDeleteOp() {
		return isEntityDeleteOp;
	}

	public boolean getIsEntityCreateOp() {
		return isEntityCreateOp;
	}

	public boolean getIsEmptyClassifications() {
		return isEmptyClassifications;
	}

	public List<RangerAtlasClassification> getClassifications() {
		return classifications;
	}

    public NotificationOpType getOpType() {
        return opType;
    }

    public boolean getIsEntityActive() { return isEntityActive; }

    public static List<RangerValiditySchedule> convertTimeSpecFromAtlasToRanger(List<TimeBoundary> atlasTimeSpec) {
        List<RangerValiditySchedule> rangerTimeSpec = null;

        if (CollectionUtils.isNotEmpty(atlasTimeSpec)) {
            rangerTimeSpec = new ArrayList<>();

            for (TimeBoundary validityPeriod : atlasTimeSpec) {
                RangerValiditySchedule validitySchedule = new RangerValiditySchedule();

                validitySchedule.setStartTime(validityPeriod.getStartTime());
                validitySchedule.setEndTime(validityPeriod.getEndTime());
                validitySchedule.setTimeZone(validityPeriod.getTimeZone());

                rangerTimeSpec.add(validitySchedule);
            }
        }

        return rangerTimeSpec;
    }
}
