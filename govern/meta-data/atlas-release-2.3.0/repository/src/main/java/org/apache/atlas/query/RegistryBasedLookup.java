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

package org.apache.atlas.query;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.type.*;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute.AtlasRelationshipEdgeDirection;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static org.apache.atlas.discovery.SearchContext.MATCH_ALL_CLASSIFIED;
import static org.apache.atlas.discovery.SearchContext.MATCH_ALL_NOT_CLASSIFIED;
import static org.apache.atlas.discovery.SearchContext.MATCH_ALL_ENTITY_TYPES;
import static org.apache.atlas.model.discovery.SearchParameters.ALL_CLASSIFICATIONS;
import static org.apache.atlas.model.discovery.SearchParameters.NO_CLASSIFICATIONS;
import static org.apache.atlas.model.discovery.SearchParameters.ALL_ENTITY_TYPES;

class RegistryBasedLookup implements Lookup {
    private static final Map<String, String> NUMERIC_ATTRIBUTES = new HashMap<String, String>() {{
            put(AtlasBaseTypeDef.ATLAS_TYPE_BYTE, "");
            put(AtlasBaseTypeDef.ATLAS_TYPE_SHORT, "");
            put(AtlasBaseTypeDef.ATLAS_TYPE_INT, "");
            put(AtlasBaseTypeDef.ATLAS_TYPE_LONG, "L");
            put(AtlasBaseTypeDef.ATLAS_TYPE_FLOAT, "f");
            put(AtlasBaseTypeDef.ATLAS_TYPE_DOUBLE, "d");
            put(AtlasBaseTypeDef.ATLAS_TYPE_BIGINTEGER, "");
            put(AtlasBaseTypeDef.ATLAS_TYPE_BIGDECIMAL, "");
        }};

    private final AtlasTypeRegistry typeRegistry;

    public RegistryBasedLookup(AtlasTypeRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;
    }

    @Override
    public AtlasType getType(String typeName) throws AtlasBaseException {
        AtlasType ret;

        if (typeName.equals(ALL_CLASSIFICATIONS)) {
            ret = MATCH_ALL_CLASSIFIED;
        } else if (typeName.equals(NO_CLASSIFICATIONS)) {
            ret = MATCH_ALL_NOT_CLASSIFIED;
        } else if (typeName.equals(ALL_ENTITY_TYPES)) {
            ret = MATCH_ALL_ENTITY_TYPES;
        } else {
            ret = typeRegistry.getType(typeName);
        }

        return ret;
    }

    @Override
    public String getQualifiedName(GremlinQueryComposer.Context context, String name) throws AtlasBaseException {
        AtlasStructType et = context.getActiveEntityType();
        if (et == null && isClassificationType(context)) {
            et = (AtlasClassificationType) context.getActiveType();
        }

        if (et == null) {
            return "";
        }

        return et.getVertexPropertyName(name);
    }

    @Override
    public boolean isPrimitive(GremlinQueryComposer.Context context, String attributeName) {
        AtlasStructType et = context.getActiveEntityType();
        if (et == null && isClassificationType(context)) {
           et = (AtlasClassificationType) context.getActiveType();
        }

        if (et == null) {
            return false;
        }

        AtlasType at = getAttributeType(et, attributeName);
        if(at == null) {
            return false;
        }

        TypeCategory tc = at.getTypeCategory();
        if (isPrimitiveUsingTypeCategory(tc)) return true;

        if ((tc != null) && (tc == TypeCategory.ARRAY)) {
            AtlasArrayType ct = ((AtlasArrayType)at);
            return isPrimitiveUsingTypeCategory(ct.getElementType().getTypeCategory());
        }

        if ((tc != null) && (tc == TypeCategory.MAP)) {
            AtlasMapType ct = ((AtlasMapType)at);
            return isPrimitiveUsingTypeCategory(ct.getValueType().getTypeCategory());
        }

        return false;
    }

    private boolean isPrimitiveUsingTypeCategory(TypeCategory tc) {
        return ((tc != null) && (tc == TypeCategory.PRIMITIVE || tc == TypeCategory.ENUM));
    }

    @Override
    public String getRelationshipEdgeLabel(GremlinQueryComposer.Context context, String attributeName) {
        AtlasEntityType et = context.getActiveEntityType();
        if(et == null) {
            return "";
        }

        AtlasStructType.AtlasAttribute attr = getAttribute(et, attributeName);

        return (attr != null) ? attr.getRelationshipEdgeLabel() : "";
    }

    @Override
    public AtlasRelationshipEdgeDirection getRelationshipEdgeDirection(GremlinQueryComposer.Context context, String attributeName) {
        AtlasEntityType entityType  = context.getActiveEntityType();
        AtlasStructType.AtlasAttribute attribute = null;
        AtlasRelationshipEdgeDirection ret = null;

        if (entityType != null) {
            attribute = entityType.getRelationshipAttribute(attributeName, null);
            if (attribute != null) {
                ret = attribute.getRelationshipEdgeDirection();
            }
        }
        return ret;
    }

    @Override
    public boolean hasAttribute(GremlinQueryComposer.Context context, String typeName) {
        AtlasStructType type = context.getActiveEntityType();

        if (type == null && isClassificationType(context)) {
            type = (AtlasClassificationType) context.getActiveType();
        }
        return getAttribute(type, typeName) != null;
    }

    @Override
    public boolean doesTypeHaveSubTypes(GremlinQueryComposer.Context context) {
        return (context.getActiveEntityType() != null && context.getActiveEntityType().getAllSubTypes().size() > 0);
    }

    @Override
    public String getTypeAndSubTypes(GremlinQueryComposer.Context context) {
        String[] str = context.getActiveEntityType() != null ?
                        context.getActiveEntityType().getTypeAndAllSubTypes().toArray(new String[]{}) :
                        new String[]{};
        if(str.length == 0) {
            return null;
        }

        String[] quoted = new String[str.length];
        for (int i = 0; i < str.length; i++) {
            quoted[i] = IdentifierHelper.getQuoted(str[i]);
        }

        return StringUtils.join(quoted, ",");
    }

    @Override
    public boolean isTraitType(String typeName) {
        AtlasType t = null;
        try {
            if (typeName.equalsIgnoreCase(ALL_CLASSIFICATIONS)) {
                t = MATCH_ALL_CLASSIFIED;
            } else if (typeName.equalsIgnoreCase(NO_CLASSIFICATIONS)) {
                t = MATCH_ALL_NOT_CLASSIFIED;
            } else if (typeName.equalsIgnoreCase(ALL_ENTITY_TYPES)) {
                t = MATCH_ALL_ENTITY_TYPES;
            } else {
                t = typeRegistry.getType(typeName);
            }

        } catch (AtlasBaseException e) {
            return false;
        }

        return isTraitType(t);
    }

    private boolean isTraitType(AtlasType t) {
        return (t != null && t.getTypeCategory() == TypeCategory.CLASSIFICATION);
    }

    @Override
    public String getTypeFromEdge(GremlinQueryComposer.Context context, String item) {
        AtlasEntityType et = context.getActiveEntityType();
        if(et == null) {
            return "";
        }

        AtlasStructType.AtlasAttribute attr = getAttribute(et, item);
        if(attr == null) {
            return null;
        }

        AtlasType at = attr.getAttributeType();
        switch (at.getTypeCategory()) {
            case ARRAY:
                AtlasArrayType arrType = ((AtlasArrayType)at);
                return getCollectionElementType(arrType.getElementType());

            case MAP:
                AtlasMapType mapType = ((AtlasMapType)at);
                return getCollectionElementType(mapType.getValueType());
        }

        return getAttribute(context.getActiveEntityType(), item).getTypeName();
    }

    private String getCollectionElementType(AtlasType elemType) {
        if(elemType.getTypeCategory() == TypeCategory.OBJECT_ID_TYPE) {
            return ((AtlasBuiltInTypes.AtlasObjectIdType)elemType).getObjectType();
        } else {
            return elemType.getTypeName();
        }
    }

    @Override
    public boolean isDate(GremlinQueryComposer.Context context, String attributeName) {
        AtlasEntityType et = context.getActiveEntityType();
        if (et == null) {
            return false;
        }

        AtlasType attr = getAttributeType(et, attributeName);
        return attr != null && attr.getTypeName().equals(AtlasBaseTypeDef.ATLAS_TYPE_DATE);
    }

    @Override
    public boolean isNumeric(GremlinQueryComposer.Context context, String attrName) {
        AtlasEntityType et = context.getActiveEntityType();
        if (et == null) {
            return false;
        }

        AtlasType attr = getAttributeType(et, attrName);
        boolean ret = attr != null && NUMERIC_ATTRIBUTES.containsKey(attr.getTypeName());
        if(ret) {
            context.setNumericTypeFormatter(NUMERIC_ATTRIBUTES.get(attr.getTypeName()));
        }

        return ret;
    }

    @Override
    public String getVertexPropertyName(String typeName, String attrName) {
        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

        if (entityType == null && StringUtils.equals(typeName, ALL_ENTITY_TYPES)) {
            entityType = MATCH_ALL_ENTITY_TYPES;
        }

        AtlasStructType.AtlasAttribute attribute = getAttribute(entityType, attrName);
        if (attribute == null) {
            return null;
        }

        return attribute.getVertexPropertyName();
    }

    private AtlasStructType.AtlasAttribute getAttribute(AtlasStructType type, String attrName) {
        AtlasStructType.AtlasAttribute ret = null;

        if (type == null) {
            return ret;
        }

        if (type instanceof AtlasEntityType) {
            AtlasEntityType entityType = (AtlasEntityType) type;
            ret = entityType.getAttribute(attrName);

            if (ret == null) {
                ret = entityType.getRelationshipAttribute(attrName, null);
            }

            return ret;
        }

        if (type instanceof AtlasClassificationType) {
            AtlasClassificationType classificationType = (AtlasClassificationType) type;
            return classificationType.getAttribute(attrName);

        }

        return ret;
    }

    private AtlasType getAttributeType(AtlasStructType entityType, String attrName) {
        AtlasStructType.AtlasAttribute attribute = getAttribute(entityType, attrName);

        return attribute != null ? attribute.getAttributeType() : null;
    }

    private boolean isClassificationType(GremlinQueryComposer.Context context) {
        return context.getActiveType() instanceof AtlasClassificationType;
    }

}
