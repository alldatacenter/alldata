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
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AtlasObjectIdConverter extends  AtlasAbstractFormatConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasObjectIdConverter.class);


    public AtlasObjectIdConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry) {
        this(registry, typeRegistry, TypeCategory.OBJECT_ID_TYPE);
    }

    protected AtlasObjectIdConverter(AtlasFormatConverters registry, AtlasTypeRegistry typeRegistry, TypeCategory typeCategory) {
        super(registry, typeRegistry, typeCategory);
    }

    @Override
    public boolean isValidValueV1(Object v1Obj, AtlasType type) {
        boolean ret = (v1Obj == null) || v1Obj instanceof Id || v1Obj instanceof Referenceable;

        if (LOG.isDebugEnabled()) {
            LOG.debug("AtlasObjectIdConverter.isValidValueV1(type={}, value={}): {}", (v1Obj != null ? v1Obj.getClass().getCanonicalName() : null), v1Obj, ret);
        }

        return ret;
    }

    @Override
    public Object fromV1ToV2(Object v1Obj, AtlasType type, AtlasFormatConverter.ConverterContext converterContext) throws AtlasBaseException {
        Object ret = null;

        if (v1Obj != null) {
            if (v1Obj instanceof Id) {
                Id id = (Id) v1Obj;

                ret = new AtlasObjectId(id.getId(), id.getTypeName());
            } else if (v1Obj instanceof Referenceable) {
                Referenceable refInst = (Referenceable) v1Obj;
                String        guid    = refInst.getId().getId();

                ret = new AtlasObjectId(guid, refInst.getTypeName());

                if (!converterContext.entityExists(guid) && hasAnyAssignedAttribute(refInst)) {
                    AtlasEntityType            entityType = typeRegistry.getEntityTypeByName(refInst.getTypeName());
                    AtlasEntityFormatConverter converter  = (AtlasEntityFormatConverter) converterRegistry.getConverter(TypeCategory.ENTITY);
                    AtlasEntity                entity     = converter.fromV1ToV2(v1Obj, entityType, converterContext);

                    converterContext.addReferredEntity(entity);
                }
            }
        }

        return ret;
    }

    @Override
    public Object fromV2ToV1(Object v2Obj, AtlasType type, ConverterContext converterContext) throws AtlasBaseException {
        Id ret = null;

        if (v2Obj != null) {
            if (v2Obj instanceof Map) {
                Map    v2Map    = (Map) v2Obj;
                String idStr    = (String)v2Map.get(AtlasObjectId.KEY_GUID);
                String typeName = (String)v2Map.get(AtlasObjectId.KEY_TYPENAME);

                if (StringUtils.isEmpty(idStr)) {
                    throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
                }

                ret = new Id(idStr, 0, typeName);
            } else if (v2Obj instanceof AtlasObjectId) { // transient-id
                AtlasObjectId objId = (AtlasObjectId) v2Obj;

                ret = new Id(objId.getGuid(), 0, objId.getTypeName());
            } else if (v2Obj instanceof AtlasEntity) {
                AtlasEntity entity = (AtlasEntity) v2Obj;

                ret = new Id(entity.getGuid(), entity.getVersion() == null ? 0 : entity.getVersion().intValue(), entity.getTypeName());
            } else {
                throw new AtlasBaseException(AtlasErrorCode.TYPE_CATEGORY_INVALID, type.getTypeCategory().name());
            }
        }

        return ret;
    }

    private boolean hasAnyAssignedAttribute(org.apache.atlas.v1.model.instance.Referenceable rInstance) {
        boolean ret = false;

        Map<String, Object> attributes = rInstance.getValues();

        if (MapUtils.isNotEmpty(attributes)) {
            for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                if (attribute.getValue() != null) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }
}
