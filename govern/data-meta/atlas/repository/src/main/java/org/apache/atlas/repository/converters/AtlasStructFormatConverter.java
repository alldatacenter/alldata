/**
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
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.utils.AtlasEntityUtil;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.type.*;
import org.apache.atlas.type.AtlasBuiltInTypes.AtlasObjectIdType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasStructFormatConverter extends AtlasAbstractFormatConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasStructFormatConverter.class);

    public static final String ATTRIBUTES_PROPERTY_KEY              = "attributes";
    public static final String RELATIONSHIP_ATTRIBUTES_PROPERTY_KEY = "relationshipAttributes";

    public AtlasStructFormatConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry) {
        this(registry, typeRegistry, TypeCategory.STRUCT);
    }

    protected AtlasStructFormatConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry, TypeCategory typeCategory) {
        super(registry, typeRegistry, typeCategory);
    }

    @Override
    public boolean isValidValueV1(Object v1Obj, AtlasType type) {
        boolean ret = (v1Obj == null) || v1Obj instanceof Map || v1Obj instanceof Struct;

        if (LOG.isDebugEnabled()) {
            LOG.debug("AtlasStructFormatConverter.isValidValueV1(type={}, value={}): {}", (v1Obj != null ? v1Obj.getClass().getCanonicalName() : null), v1Obj, ret);
        }

        return ret;
    }

    @Override
    public Object fromV1ToV2(Object v1Obj, AtlasType type, ConverterContext converterContext) throws AtlasBaseException {
        AtlasStruct ret = null;

        if (v1Obj != null) {
            AtlasStructType structType = (AtlasStructType)type;

            if (v1Obj instanceof Map) {
                final Map v1Map     = (Map) v1Obj;
                final Map v1Attribs = (Map) v1Map.get(ATTRIBUTES_PROPERTY_KEY);

                if (MapUtils.isNotEmpty(v1Attribs)) {
                    ret = new AtlasStruct(type.getTypeName(), fromV1ToV2(structType, v1Attribs, converterContext));
                } else {
                    ret = new AtlasStruct(type.getTypeName());
                }
            } else if (v1Obj instanceof Struct) {
                Struct struct = (Struct) v1Obj;

                ret = new AtlasStruct(type.getTypeName(), fromV1ToV2(structType, struct.getValues(), converterContext));
            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "Map or Struct", v1Obj.getClass().getCanonicalName());
            }
        }

        return ret;
    }

    @Override
    public Object fromV2ToV1(Object v2Obj, AtlasType type, ConverterContext converterContext) throws AtlasBaseException {
        Struct ret = null;

        if (v2Obj != null) {
            AtlasStructType structType = (AtlasStructType)type;

            if (v2Obj instanceof Map) {
                final Map v2Map     = (Map) v2Obj;
                final Map v2Attribs;

                if (v2Map.containsKey(ATTRIBUTES_PROPERTY_KEY)) {
                    v2Attribs = (Map) v2Map.get(ATTRIBUTES_PROPERTY_KEY);
                } else {
                    v2Attribs = v2Map;
                }

                if (MapUtils.isNotEmpty(v2Attribs)) {
                    ret = new Struct(type.getTypeName(), fromV2ToV1(structType, v2Attribs, converterContext));
                } else {
                    ret = new Struct(type.getTypeName());
                }
            } else if (v2Obj instanceof AtlasStruct) {
                AtlasStruct struct = (AtlasStruct) v2Obj;

                ret = new Struct(type.getTypeName(), fromV2ToV1(structType, struct.getAttributes(), converterContext));
            } else {
                throw new AtlasBaseException(AtlasErrorCode.UNEXPECTED_TYPE, "Map or AtlasStruct", v2Obj.getClass().getCanonicalName());
            }
        }

        return ret;
    }

    protected Map<String, Object> fromV2ToV1(AtlasStructType structType, Map<String, Object> attributes, ConverterContext context) throws AtlasBaseException {
        Map<String, Object> ret          = null;
        boolean             isEntityType = structType instanceof AtlasEntityType;

        if (MapUtils.isNotEmpty(attributes)) {
            ret = new HashMap<>();

            // Only process the requested/set attributes
            for (String attrName : attributes.keySet()) {
                Object         v2Value = attributes.get(attrName);
                AtlasAttribute attr    = structType.getAttribute(attrName);

                if (attr == null) {
                    if (isEntityType) {
                        attr = ((AtlasEntityType) structType).getRelationshipAttribute(attrName, AtlasEntityUtil.getRelationshipType(v2Value));
                    }

                    if (attr == null) {
                        LOG.warn("ignored unknown attribute {}.{}", structType.getTypeName(), attrName);
                        continue;
                    }
                }

                AtlasType            attrType      = attr.getAttributeType();
                AtlasFormatConverter attrConverter = converterRegistry.getConverter(attrType.getTypeCategory());

                if (v2Value != null && isEntityType && attr.isOwnedRef()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("{}: is ownedRef, attrType={}", attr.getQualifiedName(), attrType.getTypeName());
                    }

                    if (attrType instanceof AtlasArrayType) {
                        AtlasArrayType  arrayType  = (AtlasArrayType) attrType;
                        AtlasType       elemType   = arrayType.getElementType();
                        String          elemTypeName;

                        if (elemType instanceof AtlasObjectIdType) {
                            elemTypeName = ((AtlasObjectIdType) elemType).getObjectType();
                        } else {
                            elemTypeName = elemType.getTypeName();
                        }

                        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(elemTypeName);;

                        if (entityType != null) {
                            Collection<?>     arrayValue = (Collection<?>) v2Value;
                            List<AtlasEntity> entities   = new ArrayList<>(arrayValue.size());

                            for (Object arrayElem : arrayValue) {
                                String      entityGuid = getGuid(arrayElem);
                                AtlasEntity entity = StringUtils.isNotEmpty(entityGuid) ? context.getById(entityGuid) : null;

                                if (entity != null) {
                                    entities.add(entity);
                                } else {
                                    LOG.warn("{}: not replacing objIdList with entityList - entity not found guid={}", attr.getQualifiedName(), entityGuid);

                                    entities = null;
                                    break;
                                }
                            }

                            if (entities != null) {
                                v2Value  = entities;
                                attrType = new AtlasArrayType(entityType, arrayType.getMinCount(), arrayType.getMaxCount(), arrayType.getCardinality());

                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("{}: replaced objIdList with entityList", attr.getQualifiedName());
                                }
                            }
                        } else {
                            LOG.warn("{}: not replacing objIdList with entityList - elementType {} is not an entityType", attr.getQualifiedName(), elemTypeName);
                        }
                    } else if (attrType instanceof AtlasObjectIdType) {
                        String          entityGuid = getGuid(v2Value);
                        AtlasEntity     entity     = StringUtils.isNotEmpty(entityGuid) ? context.getById(entityGuid) : null;
                        AtlasEntityType entityType = entity != null ? typeRegistry.getEntityTypeByName(entity.getTypeName()) : null;

                        if (entity != null && entityType != null) {
                            v2Value       = entity;
                            attrType      = entityType;
                            attrConverter = converterRegistry.getConverter(attrType.getTypeCategory());

                            if (LOG.isDebugEnabled()) {
                                LOG.debug("{}: replaced objId with entity guid={}", attr.getQualifiedName(), entityGuid);
                            }
                        } else {
                            LOG.warn("{}: not replacing objId with entity - entity not found guid={}", attr.getQualifiedName(), entityGuid);
                        }
                    } else {
                        LOG.warn("{}: not replacing objId with entity - unexpected attribute-type {}", attr.getQualifiedName(), attrType.getTypeName());
                    }
                }

                Object v1Value = attrConverter.fromV2ToV1(v2Value, attrType, context);

                ret.put(attr.getName(), v1Value);
            }
        }

        return ret;
    }

    private String getGuid(Object obj) {
        final String ret;

        if (obj instanceof AtlasObjectId) {
            AtlasObjectId objId = (AtlasObjectId) obj;

            ret = objId.getGuid();
        } else if (obj instanceof Map) {
            Map v2Map = (Map) obj;

            ret = (String)v2Map.get(AtlasObjectId.KEY_GUID);
        } else {
            ret = null;
        }

        return ret;
    }

    protected Map<String, Object> fromV1ToV2(AtlasStructType structType, Map attributes, ConverterContext context) throws AtlasBaseException {
        Map<String, Object> ret        = null;
        AtlasEntityType     entityType = (structType instanceof AtlasEntityType) ? ((AtlasEntityType) structType) : null;

        if (MapUtils.isNotEmpty(attributes)) {
            ret = new HashMap<>();

            // Only process the requested/set attributes
            for (Object attribKey : attributes.keySet()) {
                String         attrName = attribKey.toString();
                Object         v1Value  = attributes.get(attrName);
                AtlasAttribute attr     = structType.getAttribute(attrName);

                if (attr == null) {
                    if (entityType != null) {
                        attr = entityType.getRelationshipAttribute(attrName, null);
                    }

                    if (attr == null) {
                        LOG.warn("ignored unknown attribute {}.{}", structType.getTypeName(), attrName);
                        continue;
                    }
                }

                AtlasType            attrType      = attr.getAttributeType();
                AtlasFormatConverter attrConverter = converterRegistry.getConverter(attrType.getTypeCategory());

                if (attrConverter.isValidValueV1(v1Value, attrType)) {
                    Object v2Value = attrConverter.fromV1ToV2(v1Value, attrType, context);

                    ret.put(attrName, v2Value);
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS, attrName + "=" + v1Value);
                }
            }
        }

        return ret;
    }
}
