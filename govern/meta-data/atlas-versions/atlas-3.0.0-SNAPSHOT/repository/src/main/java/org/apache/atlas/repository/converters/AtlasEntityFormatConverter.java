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
package org.apache.atlas.repository.converters;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.Status;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.v1.model.instance.AtlasSystemAttributes;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasEntityFormatConverter extends AtlasStructFormatConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasEntityFormatConverter.class);

    public AtlasEntityFormatConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry) {
        super(registry, typeRegistry, TypeCategory.ENTITY);
    }

    @Override
    public boolean isValidValueV1(Object v1Obj, AtlasType type) {
        boolean ret = (v1Obj == null) || v1Obj instanceof Id || v1Obj instanceof Referenceable;

        if (LOG.isDebugEnabled()) {
            LOG.debug("AtlasEntityFormatConverter.isValidValueV1(type={}, value={}): {}", (v1Obj != null ? v1Obj.getClass().getCanonicalName() : null), v1Obj, ret);
        }

        return ret;
    }

    @Override
    public AtlasEntity fromV1ToV2(Object v1Obj, AtlasType type, ConverterContext context) throws AtlasBaseException {
        AtlasEntity entity = null;

        if (v1Obj != null) {
            AtlasEntityType entityType = (AtlasEntityType) type;

            if (v1Obj instanceof Referenceable) {
                Referenceable entRef = (Referenceable)v1Obj;

                String guid = entRef.getId().getId();

                if (!context.entityExists(guid)) {
                    entity = new AtlasEntity(entRef.getTypeName(), super.fromV1ToV2(entityType, entRef.getValues(), context));

                    entity.setGuid(entRef.getId().getId());
                    entity.setStatus(convertState(entRef.getId().getState()));

                    if (entRef.getSystemAttributes() != null) {
                        entity.setCreatedBy(entRef.getSystemAttributes().getCreatedBy());
                        entity.setCreateTime(entRef.getSystemAttributes().getCreatedTime());
                        entity.setUpdatedBy(entRef.getSystemAttributes().getModifiedBy());
                        entity.setUpdateTime(entRef.getSystemAttributes().getModifiedTime());
                    }

                    entity.setVersion((long) entRef.getId().getVersion());

                    if (CollectionUtils.isNotEmpty(entRef.getTraitNames())) {
                        List<AtlasClassification> classifications = new ArrayList<>();
                        AtlasFormatConverter      traitConverter  = converterRegistry.getConverter(TypeCategory.CLASSIFICATION);

                        for (String traitName : entRef.getTraitNames()) {
                            Struct              trait          = entRef.getTraits().get(traitName);
                            AtlasType           classifiType   = typeRegistry.getType(traitName);
                            AtlasClassification classification = (AtlasClassification) traitConverter.fromV1ToV2(trait, classifiType, context);

                            classifications.add(classification);
                        }

                        entity.setClassifications(classifications);
                    }
                }

            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "Referenceable",
                                             v1Obj.getClass().getCanonicalName());
            }
        }
        return entity;
    }

    @Override
    public Object fromV2ToV1(Object v2Obj, AtlasType type, ConverterContext context) throws AtlasBaseException {
        Object ret = null;

        if (v2Obj != null) {
            AtlasEntityType entityType = (AtlasEntityType) type;

            if (v2Obj instanceof Map) {
                Map    v2Map    = (Map) v2Obj;
                String idStr    = (String)v2Map.get(AtlasObjectId.KEY_GUID);
                String typeName = type.getTypeName();

                if (StringUtils.isEmpty(idStr)) {
                    throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
                }

                final Map v2Attribs = (Map) v2Map.get(ATTRIBUTES_PROPERTY_KEY);

                if (MapUtils.isEmpty(v2Attribs)) {
                    ret = new Id(idStr, 0, typeName);
                } else {
                    ret = new Referenceable(idStr, typeName, super.fromV2ToV1(entityType, v2Attribs, context));
                }
            } else if (v2Obj instanceof AtlasEntity) {
                AtlasEntity entity = (AtlasEntity) v2Obj;
                Status      status = entity.getStatus();

                if (status == null) {
                    status = Status.ACTIVE;
                }

                final Map<String, Object> v2Attribs = entity.getAttributes();

                Referenceable referenceable = new Referenceable(entity.getGuid(), entity.getTypeName(), status.name(),
                                                                fromV2ToV1(entityType, v2Attribs, context),
                                                                new AtlasSystemAttributes(entity.getCreatedBy(), entity.getUpdatedBy(), entity.getCreateTime(), entity.getUpdateTime()));

                if (CollectionUtils.isNotEmpty(entity.getClassifications())) {
                    for (AtlasClassification classification : entity.getClassifications()) {
                        String                  traitName          = classification.getTypeName();
                        AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(traitName);
                        AtlasFormatConverter    formatConverter    = classificationType != null ? converterRegistry.getConverter(classificationType.getTypeCategory()) : null;
                        Struct                  trait              = formatConverter != null ? (Struct)formatConverter.fromV2ToV1(classification, classificationType, context) : null;

                        if (trait != null) {
                            referenceable.getTraitNames().add(trait.getTypeName());
                            referenceable.getTraits().put(trait.getTypeName(), trait);
                        }
                    }
                }

                ret = referenceable;
            } else if (v2Obj instanceof AtlasObjectId) { // transient-id
                AtlasEntity entity = context.getById(((AtlasObjectId) v2Obj).getGuid());
                if ( entity == null) {
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Could not find entity ",
                        v2Obj.toString());
                }
                ret = this.fromV2ToV1(entity, typeRegistry.getType(((AtlasObjectId) v2Obj).getTypeName()), context);
            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "Map or AtlasEntity or String",
                                             v2Obj.getClass().getCanonicalName());
            }
        }
        return ret;
    }

    private Status convertState(Id.EntityState state){
        return (state != null && state.equals(Id.EntityState.DELETED)) ? Status.DELETED : Status.ACTIVE;
    }
}
