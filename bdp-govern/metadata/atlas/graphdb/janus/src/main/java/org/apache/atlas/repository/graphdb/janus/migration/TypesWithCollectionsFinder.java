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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasMapType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.TypeCategory.*;

public class TypesWithCollectionsFinder {
    private static final Logger LOG = LoggerFactory.getLogger(TypesWithCollectionsFinder.class);

    static final EnumSet<TypeCategory> nonPrimitives = EnumSet.of(ENTITY, STRUCT, OBJECT_ID_TYPE);

    public static Map<String, Map<String, List<String>>> getVertexPropertiesForCollectionAttributes(AtlasTypeRegistry typeRegistry) {
        Map<String, Map<String, List<String>>> ret = new HashMap<>();

        addVertexPropertiesForCollectionAttributes(typeRegistry.getAllEntityTypes(), ret);
        addVertexPropertiesForCollectionAttributes(typeRegistry.getAllStructTypes(), ret);
        addVertexPropertiesForCollectionAttributes(typeRegistry.getAllClassificationTypes(), ret);

        displayInfo("types with properties: ", ret);

        return ret;
    }

    private static void addVertexPropertiesForCollectionAttributes(Collection<? extends AtlasStructType> types, Map<String, Map<String, List<String>>> typeAttrMap) {
        for (AtlasStructType type : types) {
            Map<String, List<String>> collectionProperties = getVertexPropertiesForCollectionAttributes(type);

            if(collectionProperties != null && collectionProperties.size() > 0) {
                typeAttrMap.put(type.getTypeName(), collectionProperties);
            }
        }
    }

    static Map<String, List<String>> getVertexPropertiesForCollectionAttributes(AtlasStructType type) {
        try {
            Map<String, List<String>> collectionProperties = new HashMap<>();

            for (AtlasAttribute attr : type.getAllAttributes().values()) {
                addIfCollectionAttribute(attr, collectionProperties);
            }

            if (type instanceof AtlasEntityType) {
                AtlasEntityType entityType = (AtlasEntityType) type;

                for (Map<String, AtlasAttribute> attrs : entityType.getRelationshipAttributes().values()) {
                    for (AtlasAttribute attr : attrs.values()) {
                        addIfCollectionAttribute(attr, collectionProperties);
                    }
                }
            }
            return collectionProperties;
        } catch (Exception e) {
            LOG.error("addVertexPropertiesForCollectionAttributes", e);
        }

        return null;
    }

    private static void addIfCollectionAttribute(AtlasAttribute attr, Map<String, List<String>> collectionProperties) {
        AtlasType    attrType         = attr.getAttributeType();
        TypeCategory attrTypeCategory = attrType.getTypeCategory();

        switch (attrTypeCategory) {
            case ARRAY: {
                TypeCategory arrayElementType = ((AtlasArrayType) attrType).getElementType().getTypeCategory();

                if (nonPrimitives.contains(arrayElementType)) {
                    addVertexProperty(attrTypeCategory.toString(), attr.getVertexPropertyName(), collectionProperties);
                }
            }
            break;

            case MAP: {
                TypeCategory mapValueType = ((AtlasMapType) attrType).getValueType().getTypeCategory();

                if (nonPrimitives.contains(mapValueType)) {
                    addVertexProperty(attrTypeCategory.toString(), attr.getVertexPropertyName(), collectionProperties);
                } else {
                    addVertexProperty(attrTypeCategory.toString() + "_PRIMITIVE", attr.getVertexPropertyName(), collectionProperties);
                }
            }
            break;
        }
    }

    private static void addVertexProperty(String collectionType, String propertyName, Map<String, List<String>> collectionProperties) {
        if(!collectionProperties.containsKey(collectionType)) {
            collectionProperties.put(collectionType, new ArrayList<>());
        }

        collectionProperties.get(collectionType).add(propertyName);
    }

    static void displayInfo(String message, Map<String, Map<String, List<String>>> map) {
        LOG.info(message);
        for (Map.Entry<String, Map<String, List<String>>> e : map.entrySet()) {
            LOG.info("  type: {} : {}", e.getKey(), e.getValue());
        }
    }
}
