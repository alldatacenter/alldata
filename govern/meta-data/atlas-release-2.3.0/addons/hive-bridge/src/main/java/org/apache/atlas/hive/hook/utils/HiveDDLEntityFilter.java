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
package org.apache.atlas.hive.hook.utils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.hive.hook.events.BaseHiveEvent;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.utils.AtlasPathExtractorUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HiveDDLEntityFilter implements EntityFilter {
    private static final Logger LOG = LoggerFactory.getLogger(HiveDDLEntityFilter.class);

    private static final Set<String> defaultPathTypesToRetain = new HashSet<String>() {{
        add(AtlasPathExtractorUtil.HDFS_TYPE_PATH);
        add(AtlasPathExtractorUtil.ADLS_GEN2_ACCOUNT);
        add(AtlasPathExtractorUtil.ADLS_GEN2_CONTAINER);
        add(AtlasPathExtractorUtil.ADLS_GEN2_DIRECTORY);
        add(AtlasPathExtractorUtil.GCS_BUCKET);
        add(AtlasPathExtractorUtil.GCS_VIRTUAL_DIR);
        add(AtlasPathExtractorUtil.AWS_S3_BUCKET);
        add(AtlasPathExtractorUtil.AWS_S3_V2_BUCKET);
        add(AtlasPathExtractorUtil.AWS_S3_V2_PSEUDO_DIR);
        add(AtlasPathExtractorUtil.AWS_S3_PSEUDO_DIR);
        add(AtlasPathExtractorUtil.OZONE_KEY);
        add(AtlasPathExtractorUtil.OZONE_BUCKET);
        add(AtlasPathExtractorUtil.OZONE_VOLUME);
    }};

    private static final Set<String> typesToRetain = new HashSet<String>() {{
        add(BaseHiveEvent.HIVE_TYPE_PROCESS);
        add(BaseHiveEvent.HIVE_TYPE_PROCESS_EXECUTION);
        add(BaseHiveEvent.HIVE_TYPE_COLUMN_LINEAGE);
        add(BaseHiveEvent.HIVE_DB_DDL);
        add(BaseHiveEvent.HIVE_TABLE_DDL);
        addAll(defaultPathTypesToRetain);
    }};

    public HiveDDLEntityFilter(List<String> additionalTypesToRetain) {
        if (CollectionUtils.isEmpty(additionalTypesToRetain)) {
            return;
        }

        typesToRetain.addAll(additionalTypesToRetain);
        LOG.info("Types retained: {}", typesToRetain.toArray());
    }

    public List<HookNotification> apply(List<HookNotification> incoming) {
        if (CollectionUtils.isEmpty(incoming)) {
            return incoming;
        }

        List<HookNotification> ret = new ArrayList<>();
        for (HookNotification notification : incoming) {
            HookNotification filteredNotification = apply(notification);
            if (filteredNotification == null) {
                continue;
            }

            ret.add(filteredNotification);
        }

        return ret;
    }

    @VisibleForTesting
    AtlasEntity.AtlasEntityWithExtInfo apply(AtlasEntity.AtlasEntityWithExtInfo incoming) {
        AtlasEntity.AtlasEntityWithExtInfo ret = new AtlasEntity.AtlasEntityWithExtInfo();

        AtlasEntity entity = filter(incoming.getEntity());
        if (entity == null) {
            return null;
        }

        ret.setEntity(entity);

        Map<String, AtlasEntity> refEntities = filter(incoming.getReferredEntities());
        if (!MapUtils.isEmpty(refEntities)) {
            ret.setReferredEntities(refEntities);
        }

        return ret;
    }

    @VisibleForTesting
    public AtlasEntity.AtlasEntitiesWithExtInfo apply(AtlasEntity.AtlasEntitiesWithExtInfo incoming) {
        if (incoming == null) {
            return incoming;
        }

        AtlasEntity.AtlasEntitiesWithExtInfo ret = new AtlasEntity.AtlasEntitiesWithExtInfo();

        filterEntities(incoming, ret);
        filterReferences(incoming, ret);

        return ret;
    }

    @VisibleForTesting
    List<AtlasObjectId> applyForObjectIds(List<AtlasObjectId> incoming) {
        if (incoming == null || CollectionUtils.isEmpty(incoming)) {
            return null;
        }

        List<AtlasObjectId> ret = new ArrayList<>();
        for (AtlasObjectId o : incoming) {
            if (filterObjectId(o) != null) {
                ret.add(o);
            }
        }

        return ret;
    }

    private AtlasObjectId filterObjectId(AtlasObjectId o) {
        if (o != null && typesToRetain.contains(o.getTypeName())) {
            return o;
        }

        return null;
    }

    private static void filterEntities(AtlasEntity.AtlasEntitiesWithExtInfo incoming, AtlasEntity.AtlasEntitiesWithExtInfo ret) {
        ret.setEntities(filter(incoming.getEntities()));
    }

    private static void filterReferences(AtlasEntity.AtlasEntitiesWithExtInfo incoming, AtlasEntity.AtlasEntitiesWithExtInfo ret) {
        ret.setReferredEntities(filter(incoming.getReferredEntities()));
    }

    private static Map<String, AtlasEntity> filter(Map<String, AtlasEntity> incoming) {
        if (incoming == null || MapUtils.isEmpty(incoming)) {
            return null;
        }

        return incoming.values()
                .stream()
                .filter(x -> typesToRetain.contains(x.getTypeName()))
                .collect(Collectors.toMap(AtlasEntity::getGuid, Function.identity()));
    }

    private static List<AtlasEntity> filter(List<AtlasEntity> incoming) {
        if (incoming == null) {
            return null;
        }

        List<AtlasEntity> ret = incoming.stream()
                .filter(x -> typesToRetain.contains(x.getTypeName()))
                .collect(Collectors.toList());

        for (AtlasEntity e : ret) {
            for (Object o : e.getRelationshipAttributes().values()) {
                if (o instanceof List) {
                    List list = (List) o;
                    for (Object ox : list) {
                        inferObjectTypeResetGuid(ox);
                    }
                } else {
                    inferObjectTypeResetGuid(o);
                }
            }
        }

        return ret;
    }

    private static void inferObjectTypeResetGuid(Object o) {
        if (o instanceof AtlasObjectId) {
            AtlasObjectId oid      = (AtlasObjectId) o;
            String        typeName = oid.getTypeName();

            if (oid.getUniqueAttributes() != null && !typesToRetain.contains(typeName)) {
                oid.setGuid(null);
            }
        } else {
            LinkedHashMap hm = (LinkedHashMap) o;
            if (!hm.containsKey(BaseHiveEvent.ATTRIBUTE_GUID)) {
                return;
            }

            String typeName = hm.containsKey(AtlasObjectId.KEY_TYPENAME) ? (String) hm.get(AtlasObjectId.KEY_TYPENAME) : null;

            if (hm.containsKey(BaseHiveEvent.ATTRIBUTE_UNIQUE_ATTRIBUTES) && !typesToRetain.contains(typeName)) {
                hm.remove(BaseHiveEvent.ATTRIBUTE_GUID);
            }
        }
    }

    private static AtlasEntity filter(AtlasEntity incoming) {
        if (incoming == null) {
            return null;
        }

        return typesToRetain.contains(incoming.getTypeName()) ? incoming : null;
    }

    private HookNotification apply(HookNotification notification) {
        if (notification instanceof HookNotification.EntityCreateRequestV2) {
            return apply((HookNotification.EntityCreateRequestV2) notification);
        }

        if (notification instanceof HookNotification.EntityUpdateRequestV2) {
            return apply((HookNotification.EntityUpdateRequestV2) notification);
        }

        if (notification instanceof HookNotification.EntityPartialUpdateRequestV2) {
            return apply((HookNotification.EntityPartialUpdateRequestV2) notification);
        }

        if (notification instanceof HookNotification.EntityDeleteRequestV2) {
            return apply((HookNotification.EntityDeleteRequestV2) notification);
        }

        return null;
    }

    private HookNotification.EntityCreateRequestV2 apply(HookNotification.EntityCreateRequestV2 notification) {
        AtlasEntity.AtlasEntitiesWithExtInfo entities = apply(notification.getEntities());
        if (entities == null || CollectionUtils.isEmpty(entities.getEntities())) {
            return null;
        }

        return new HookNotification.EntityCreateRequestV2(notification.getUser(), entities);
    }

    private HookNotification.EntityUpdateRequestV2 apply(HookNotification.EntityUpdateRequestV2 notification) {
        AtlasEntity.AtlasEntitiesWithExtInfo entities = apply(notification.getEntities());
        if (entities == null || CollectionUtils.isEmpty(entities.getEntities())) {
            return null;
        }

        return new HookNotification.EntityUpdateRequestV2(notification.getUser(), entities);
    }

    private HookNotification.EntityPartialUpdateRequestV2 apply(HookNotification.EntityPartialUpdateRequestV2 notification) {
        AtlasObjectId objectId = filterObjectId(notification.getEntityId());
        if (objectId == null) {
            return null;
        }

        AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo = apply(notification.getEntity());
        if (entityWithExtInfo == null) {
            return null;
        }

        return new HookNotification.EntityPartialUpdateRequestV2(notification.getUser(), objectId, entityWithExtInfo);
    }

    private HookNotification.EntityDeleteRequestV2 apply(HookNotification.EntityDeleteRequestV2 notification) {
        List<AtlasObjectId> objectIds = applyForObjectIds(notification.getEntities());
        if (CollectionUtils.isEmpty(objectIds)) {
            return null;
        }

        return new HookNotification.EntityDeleteRequestV2(notification.getUser(), objectIds);
    }
}
