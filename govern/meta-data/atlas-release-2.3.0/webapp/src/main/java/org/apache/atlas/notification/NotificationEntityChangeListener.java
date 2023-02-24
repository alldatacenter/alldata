/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasException;
import org.apache.atlas.RequestContext;
import org.apache.atlas.listener.EntityChangeListener;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.utils.AtlasPerfMetrics.MetricRecorder;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.notification.EntityNotificationV1;
import org.apache.atlas.v1.model.notification.EntityNotificationV1.OperationType;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;

/**
 * Listen to the repository for entity changes and produce entity change notifications.
 */
@Component
public class NotificationEntityChangeListener implements EntityChangeListener {
    protected static final String ATLAS_ENTITY_NOTIFICATION_PROPERTY = "atlas.notification.entity";

    private final AtlasTypeRegistry                              typeRegistry;
    private final Configuration                                  configuration;
    private final EntityNotificationSender<EntityNotificationV1> notificationSender;
    private final Map<String, List<String>>                      notificationAttributesCache = new HashMap<>();




    // ----- Constructors ------------------------------------------------------

    /**
     * Construct a NotificationEntityChangeListener.
     *
     * @param notificationInterface the notification framework interface
     * @param typeRegistry the Atlas type system
     */
    @Inject
    public NotificationEntityChangeListener(NotificationInterface notificationInterface, AtlasTypeRegistry typeRegistry, Configuration configuration) {
        this.typeRegistry       = typeRegistry;
        this.configuration      = configuration;
        this.notificationSender = new EntityNotificationSender<>(notificationInterface, configuration);

    }


    // ----- EntityChangeListener ----------------------------------------------

    @Override
    public void onEntitiesAdded(Collection<Referenceable> entities, boolean isImport) throws AtlasException {
        notifyOfEntityEvent(entities, OperationType.ENTITY_CREATE);
    }

    @Override
    public void onEntitiesUpdated(Collection<Referenceable> entities, boolean isImport) throws AtlasException {
        notifyOfEntityEvent(entities, OperationType.ENTITY_UPDATE);
    }

    @Override
    public void onTraitsAdded(Referenceable entity, Collection<? extends Struct> traits) throws AtlasException {
        notifyOfEntityEvent(Collections.singleton(entity), OperationType.TRAIT_ADD);
    }

    @Override
    public void onTraitsDeleted(Referenceable entity, Collection<? extends Struct> traits) throws AtlasException {
        notifyOfEntityEvent(Collections.singleton(entity), OperationType.TRAIT_DELETE);
    }

    @Override
    public void onTraitsUpdated(Referenceable entity, Collection<? extends Struct> traits) throws AtlasException {
        notifyOfEntityEvent(Collections.singleton(entity), OperationType.TRAIT_UPDATE);
    }

    @Override
    public void onEntitiesDeleted(Collection<Referenceable> entities, boolean isImport) throws AtlasException {
        notifyOfEntityEvent(entities, OperationType.ENTITY_DELETE);
    }

    @Override
    public void onTermAdded(Collection<Referenceable> entities, AtlasGlossaryTerm term) throws AtlasException {
        // do nothing
    }

    @Override
    public void onTermDeleted(Collection<Referenceable> entities, AtlasGlossaryTerm term) throws AtlasException {
        // do nothing
    }


    // ----- helper methods -------------------------------------------------


    // ----- helper methods ----------------------------------------------------
    @VisibleForTesting
    public static List<Struct> getAllTraits(Referenceable entityDefinition, AtlasTypeRegistry typeRegistry) throws AtlasException {
        List<Struct> ret = new ArrayList<>();

        for (String traitName : entityDefinition.getTraitNames()) {
            Struct                  trait          = entityDefinition.getTrait(traitName);
            AtlasClassificationType traitType      = typeRegistry.getClassificationTypeByName(traitName);
            Set<String>             superTypeNames = traitType != null ? traitType.getAllSuperTypes() : null;

            ret.add(trait);

            if (CollectionUtils.isNotEmpty(superTypeNames)) {
                for (String superTypeName : superTypeNames) {
                    Struct superTypeTrait = new Struct(superTypeName);

                    if (MapUtils.isNotEmpty(trait.getValues())) {
                        AtlasClassificationType superType = typeRegistry.getClassificationTypeByName(superTypeName);

                        if (superType != null && MapUtils.isNotEmpty(superType.getAllAttributes())) {
                            Map<String, Object> superTypeTraitAttributes = new HashMap<>();

                            for (Map.Entry<String, Object> attrEntry : trait.getValues().entrySet()) {
                                String attrName = attrEntry.getKey();

                                if (superType.getAllAttributes().containsKey(attrName)) {
                                    superTypeTraitAttributes.put(attrName, attrEntry.getValue());
                                }
                            }

                            superTypeTrait.setValues(superTypeTraitAttributes);
                        }
                    }

                    ret.add(superTypeTrait);
                }
            }
        }

        return ret;
    }

    // send notification of entity change
    private void notifyOfEntityEvent(Collection<Referenceable> entityDefinitions,
                                     OperationType             operationType) throws AtlasException {
        MetricRecorder metric = RequestContext.get().startMetricRecord("entityNotification");

        List<EntityNotificationV1> messages = new ArrayList<>();

        for (Referenceable entityDefinition : entityDefinitions) {
            if(GraphHelper.isInternalType(entityDefinition.getTypeName())) {
                continue;
            }

            Referenceable       entity                  = new Referenceable(entityDefinition);
            Map<String, Object> attributesMap           = entity.getValuesMap();
            List<String>        entityNotificationAttrs = getNotificationAttributes(entity.getTypeName());

            if (MapUtils.isNotEmpty(attributesMap) && CollectionUtils.isNotEmpty(entityNotificationAttrs)) {
                Collection<String> attributesToRemove = CollectionUtils.subtract(attributesMap.keySet(), entityNotificationAttrs);

                for (String attributeToRemove : attributesToRemove) {
                    attributesMap.remove(attributeToRemove);
                }
            }

            EntityNotificationV1 notification = new EntityNotificationV1(entity, operationType, getAllTraits(entity, typeRegistry));

            messages.add(notification);
        }

        if (!messages.isEmpty()) {
            notificationSender.send(messages);
        }

        RequestContext.get().endMetricRecord(metric);
    }

    private List<String> getNotificationAttributes(String entityType) {
        List<String> ret = null;

        if (notificationAttributesCache.containsKey(entityType)) {
            ret = notificationAttributesCache.get(entityType);
        } else if (configuration != null) {
            String[] notificationAttributes = configuration.getStringArray(ATLAS_ENTITY_NOTIFICATION_PROPERTY + "." + entityType + "." + "attributes.include");

            if (notificationAttributes != null) {
                ret = Arrays.asList(notificationAttributes);
            }

            notificationAttributesCache.put(entityType, ret);
        }

        return ret;
    }
}
