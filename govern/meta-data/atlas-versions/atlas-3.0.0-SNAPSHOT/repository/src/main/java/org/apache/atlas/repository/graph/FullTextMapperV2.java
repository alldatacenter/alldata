/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.graph;

import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasBuiltInTypes;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class FullTextMapperV2 implements IFullTextMapper {
    private static final Logger LOG = LoggerFactory.getLogger(FullTextMapperV2.class);

    private static final String FULL_TEXT_DELIMITER                  = " ";
    private static final String FULL_TEXT_FOLLOW_REFERENCES          = "atlas.search.fulltext.followReferences";
    private static final String FULL_TEXT_EXCLUDE_ATTRIBUTE_PROPERTY = "atlas.search.fulltext.type";

    private final AtlasTypeRegistry        typeRegistry;
    private final Configuration            configuration;
    private final EntityGraphRetriever     entityGraphRetriever;
    private final boolean                  followReferences;
    private final Map<String, Set<String>> excludeAttributesCache = new HashMap<>();


    @Inject
    public FullTextMapperV2(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry, Configuration configuration) {
        this.typeRegistry  = typeRegistry;
        this.configuration = configuration;

        followReferences = this.configuration != null && this.configuration.getBoolean(FULL_TEXT_FOLLOW_REFERENCES, false);
        // If followReferences = false then ignore relationship attr loading
        entityGraphRetriever = new EntityGraphRetriever(atlasGraph, typeRegistry, !followReferences);
    }

    /**
     * Map newly associated/defined classifications for the entity with given GUID
     * @param guid Entity guid
     * @param classifications new classifications added to the entity
     * @return Full text string ONLY for the added classifications
     * @throws AtlasBaseException
     */

    @Override
    public String getIndexTextForClassifications(String guid, List<AtlasClassification> classifications) throws AtlasBaseException {
        String                       ret     = null;
        final AtlasEntityWithExtInfo entityWithExtInfo;

        if (followReferences) {
            entityWithExtInfo = getAndCacheEntityWithExtInfo(guid);
        } else {
            AtlasEntity entity = getAndCacheEntity(guid);

            entityWithExtInfo = entity != null ? new AtlasEntityWithExtInfo(entity) : null;
        }

        if (entityWithExtInfo != null) {
            StringBuilder sb = new StringBuilder();

            if (CollectionUtils.isNotEmpty(classifications)) {
                for (AtlasClassification classification : classifications) {
                    final AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(classification.getTypeName());
                    final Set<String>             excludeAttributes  = getExcludeAttributesForIndexText(classification.getTypeName());

                    sb.append(classification.getTypeName()).append(FULL_TEXT_DELIMITER);

                    mapAttributes(classificationType, classification.getAttributes(), entityWithExtInfo, sb, new HashSet<String>(), excludeAttributes, false); //false because of full text context.
                }
            }

            ret = sb.toString();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("FullTextMapperV2.map({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public String getIndexTextForEntity(String guid) throws AtlasBaseException {
        String                   ret    = null;
        final AtlasEntity        entity;
        final AtlasEntityExtInfo entityExtInfo;

        if (followReferences) {
            AtlasEntityWithExtInfo entityWithExtInfo = getAndCacheEntityWithExtInfo(guid);

            entity        = entityWithExtInfo != null ? entityWithExtInfo.getEntity() : null;
            entityExtInfo = entityWithExtInfo;
        } else {
            entity        = getAndCacheEntity(guid, false);
            entityExtInfo = null;
        }

        if (entity != null) {
            StringBuilder sb = new StringBuilder();

            map(entity, entityExtInfo, sb, new HashSet<String>(), false);

            ret = sb.toString();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("FullTextMapperV2.map({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public String getClassificationTextForEntity(AtlasEntity entity) throws AtlasBaseException {
        String                   ret    = null;

        if (entity != null) {
            StringBuilder sb = new StringBuilder();
            map(entity, null, sb, new HashSet<String>(), true);
            ret = sb.toString();
        }

        if (LOG.isDebugEnabled()) {
            LOG.info("FullTextMapperV2.getClassificationTextForEntity({}): {}", entity.getGuid(), ret);
        }
        return ret;
    }

    private void map(AtlasEntity entity, AtlasEntityExtInfo entityExtInfo, StringBuilder sb, Set<String> processedGuids, boolean isClassificationOnly) throws AtlasBaseException {
        if (entity == null || processedGuids.contains(entity.getGuid())) {
            return;
        }

        final AtlasEntityType entityType        = typeRegistry.getEntityTypeByName(entity.getTypeName());
        final Set<String>     excludeAttributes = getExcludeAttributesForIndexText(entity.getTypeName());

        processedGuids.add(entity.getGuid());
        if(!isClassificationOnly) {
            sb.append(entity.getTypeName()).append(FULL_TEXT_DELIMITER);

            mapAttributes(entityType, entity.getAttributes(), entityExtInfo, sb, processedGuids, excludeAttributes, isClassificationOnly);
        }

        final List<AtlasClassification> classifications = entity.getClassifications();
        if (CollectionUtils.isNotEmpty(classifications)) {
            for (AtlasClassification classification : classifications) {
                final AtlasClassificationType classificationType              = typeRegistry.getClassificationTypeByName(classification.getTypeName());
                final Set<String>             excludeClassificationAttributes = getExcludeAttributesForIndexText(classification.getTypeName());


                sb.append(classification.getTypeName()).append(FULL_TEXT_DELIMITER);

                mapAttributes(classificationType, classification.getAttributes(), entityExtInfo, sb, processedGuids, excludeClassificationAttributes, isClassificationOnly);
            }
        }
    }

    private void mapAttributes(AtlasStructType structType, Map<String, Object> attributes, AtlasEntityExtInfo entityExtInfo, StringBuilder sb,
                               Set<String> processedGuids, Set<String> excludeAttributes, boolean isClassificationOnly) throws AtlasBaseException {
        if (MapUtils.isEmpty(attributes)) {
            return;
        }

        for (Map.Entry<String, Object> attributeEntry : attributes.entrySet()) {
            String attribKey = attributeEntry.getKey();
            Object attrValue = attributeEntry.getValue();

            if (attrValue == null || excludeAttributes.contains(attribKey)) {
                continue;
            }

            if (!followReferences) {
                AtlasAttribute attribute     = structType != null ? structType.getAttribute(attribKey) : null;
                AtlasType      attributeType = attribute != null ? attribute.getAttributeType() : null;

                if (attributeType == null) {
                    continue;
                }

                if (attributeType instanceof AtlasArrayType) {
                    attributeType = ((AtlasArrayType) attributeType).getElementType();
                }

                if (attributeType instanceof AtlasEntityType || attributeType instanceof AtlasBuiltInTypes.AtlasObjectIdType) {
                    continue;
                }
            }


            sb.append(attribKey).append(FULL_TEXT_DELIMITER);

            mapAttribute(attrValue, entityExtInfo, sb, processedGuids, isClassificationOnly);
        }
    }

    private void mapAttribute(Object value, AtlasEntityExtInfo entityExtInfo, StringBuilder sb, Set<String> processedGuids, boolean isClassificationOnly) throws AtlasBaseException {
        if (value instanceof AtlasObjectId) {
            if (followReferences && entityExtInfo != null) {
                AtlasObjectId objectId = (AtlasObjectId) value;
                AtlasEntity   entity   = entityExtInfo.getEntity(objectId.getGuid());

                if (entity != null) {
                    map(entity, entityExtInfo, sb, processedGuids, isClassificationOnly);
                }
            }
        } else if (value instanceof List) {
            List valueList = (List) value;

            for (Object listElement : valueList) {
                mapAttribute(listElement, entityExtInfo, sb, processedGuids, isClassificationOnly);
            }
        } else if (value instanceof Map) {
            Map valueMap = (Map) value;

            for (Object key : valueMap.keySet()) {
                mapAttribute(key, entityExtInfo, sb, processedGuids, isClassificationOnly);
                mapAttribute(valueMap.get(key), entityExtInfo, sb, processedGuids, isClassificationOnly);
            }
        } else if (value instanceof Enum) {
            Enum enumValue = (Enum) value;

            sb.append(enumValue.name()).append(FULL_TEXT_DELIMITER);
        } else if (value instanceof AtlasStruct) {
            AtlasStruct atlasStruct = (AtlasStruct) value;

            for (Map.Entry<String, Object> entry : atlasStruct.getAttributes().entrySet()) {
                sb.append(entry.getKey()).append(FULL_TEXT_DELIMITER);
                mapAttribute(entry.getValue(), entityExtInfo, sb, processedGuids, isClassificationOnly);
            }
        } else {
            sb.append(String.valueOf(value)).append(FULL_TEXT_DELIMITER);
        }
    }

    @Override
    public AtlasEntity getAndCacheEntity(String guid) throws AtlasBaseException {
        return getAndCacheEntity(guid, true);
    }

    @Override
    public AtlasEntity  getAndCacheEntity(String guid, boolean includeReferences) throws AtlasBaseException {
        RequestContext context = RequestContext.get();
        AtlasEntity    entity  = context.getEntity(guid);

        if (entity == null) {
            entity = entityGraphRetriever.toAtlasEntity(guid, includeReferences);

            if (entity != null) {
                context.cache(entity);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cache miss -> GUID = {}", guid);
                }
            }
        }

        return entity;
    }

    @Override
    public AtlasEntityWithExtInfo getAndCacheEntityWithExtInfo(String guid) throws AtlasBaseException {
        RequestContext         context           = RequestContext.get();
        AtlasEntityWithExtInfo entityWithExtInfo = context.getEntityWithExtInfo(guid);

        if (entityWithExtInfo == null) {
            // Only map ownedRef and relationship attr when follow references is set to true
            entityWithExtInfo = entityGraphRetriever.toAtlasEntityWithExtInfo(guid, !followReferences);

            if (entityWithExtInfo != null) {
                context.cache(entityWithExtInfo);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cache miss -> GUID = {}", guid);
                }
            }
        }
        return entityWithExtInfo;
    }

    private Set<String> getExcludeAttributesForIndexText(String typeName) {
        final Set<String> ret;

        if (excludeAttributesCache.containsKey(typeName)) {
            ret = excludeAttributesCache.get(typeName);

        } else if (configuration != null) {
            String[] excludeAttributes = configuration.getStringArray(FULL_TEXT_EXCLUDE_ATTRIBUTE_PROPERTY + "." +
                                                                              typeName + "." + "attributes.exclude");

            if (ArrayUtils.isNotEmpty(excludeAttributes)) {
                ret = new HashSet<>(Arrays.asList(excludeAttributes));
            } else {
                ret = Collections.emptySet();
            }

            excludeAttributesCache.put(typeName, ret);
        } else {
            ret = Collections.emptySet();
        }

        return ret;
    }
}
