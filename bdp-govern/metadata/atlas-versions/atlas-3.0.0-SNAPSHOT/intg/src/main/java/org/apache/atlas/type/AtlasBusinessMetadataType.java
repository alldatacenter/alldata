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
package org.apache.atlas.type;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;

import static org.apache.atlas.model.typedef.AtlasBusinessMetadataDef.*;


public class AtlasBusinessMetadataType extends AtlasStructType {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasBusinessMetadataType.class);

    private final AtlasBusinessMetadataDef businessMetadataDef;


    public AtlasBusinessMetadataType(AtlasBusinessMetadataDef businessMetadataDef) {
        super(businessMetadataDef);

        this.businessMetadataDef = businessMetadataDef;
    }

    @Override
    public boolean isValidValue(Object o) {
        return true; // there is no runtime instance for businessMetadataDef, so return true
    }

    @Override
    public AtlasStruct createDefaultValue() {
        return null;  // there is no runtime instance for businessMetadataDef, so return null
    }

    @Override
    public Object getNormalizedValue(Object a) {
        return null;  // there is no runtime instance for businessMetadataDef, so return null
    }

    public AtlasBusinessMetadataDef getBusinessMetadataDef() {
        return businessMetadataDef;
    }

    @Override
    void resolveReferences(AtlasTypeRegistry typeRegistry) throws AtlasBaseException {
        super.resolveReferences(typeRegistry);

        Map<String, AtlasBusinessAttribute> a = new HashMap<>();

        for (AtlasAttribute attribute : super.allAttributes.values()) {
            AtlasAttributeDef attributeDef = attribute.getAttributeDef();
            String            attrName     = attribute.getName();
            AtlasType         attrType     = attribute.getAttributeType();

            if (attrType instanceof AtlasArrayType) {
                attrType = ((AtlasArrayType) attrType).getElementType();
            } else if (attrType instanceof AtlasMapType) {
                attrType = ((AtlasMapType) attrType).getValueType();
            }

            // check if attribute type is not struct/classification/entity/business-metadata
            if (attrType instanceof AtlasStructType) {
                throw new AtlasBaseException(AtlasErrorCode.BUSINESS_METADATA_DEF_ATTRIBUTE_TYPE_INVALID, getTypeName(), attrName);
            }

            Set<String>          entityTypeNames = attribute.getOptionSet(ATTR_OPTION_APPLICABLE_ENTITY_TYPES);
            Set<AtlasEntityType> entityTypes     = new HashSet<>();

            if (CollectionUtils.isNotEmpty(entityTypeNames)) {
                for (String entityTypeName : entityTypeNames) {
                    AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entityTypeName);

                    if (entityType == null) {
                        throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, entityTypeName);
                    }

                    entityTypes.add(entityType);
                }
            }

            AtlasBusinessAttribute bmAttribute;
            if (attrType instanceof AtlasBuiltInTypes.AtlasStringType) {
                Integer maxStringLength = attribute.getOptionInt(ATTR_MAX_STRING_LENGTH);
                if (maxStringLength == null) {
                    throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ATTRIBUTE, attributeDef.getName(), "options." + ATTR_MAX_STRING_LENGTH);
                }

                String validPattern = attribute.getOptionString(ATTR_VALID_PATTERN);
                bmAttribute = new AtlasBusinessAttribute(attribute, entityTypes, maxStringLength, validPattern);
            } else {
                bmAttribute = new AtlasBusinessAttribute(attribute, entityTypes);
            }

            a.put(attrName, bmAttribute);
        }

        super.allAttributes = Collections.unmodifiableMap(a);
    }

    @Override
    void resolveReferencesPhase2(AtlasTypeRegistry typeRegistry) throws AtlasBaseException {
        super.resolveReferencesPhase2(typeRegistry);

        for (AtlasAttribute attribute : super.allAttributes.values()) {
            AtlasBusinessAttribute bmAttribute = (AtlasBusinessAttribute) attribute;
            Set<AtlasEntityType>   entityTypes = bmAttribute.getApplicableEntityTypes();

            if (CollectionUtils.isNotEmpty(entityTypes)) {
                for (AtlasEntityType entityType : entityTypes) {
                    entityType.addBusinessAttribute(bmAttribute);
                }
            }
        }
    }

    public static class AtlasBusinessAttribute extends AtlasAttribute {
        private final Set<AtlasEntityType> applicableEntityTypes;
        private final int                  maxStringLength;
        private final String               validPattern;

        public AtlasBusinessAttribute(AtlasAttribute attribute, Set<AtlasEntityType> applicableEntityTypes) {
            super(attribute);

            this.maxStringLength       = 0;
            this.validPattern          = null;
            this.applicableEntityTypes = applicableEntityTypes;
        }

        public AtlasBusinessAttribute(AtlasAttribute attribute, Set<AtlasEntityType> applicableEntityTypes, int maxStringLength, String validPattern) {
            super(attribute);

            this.maxStringLength       = maxStringLength;
            this.validPattern          = validPattern;
            this.applicableEntityTypes = applicableEntityTypes;
        }

        @Override
        public AtlasBusinessMetadataType getDefinedInType() {
            return (AtlasBusinessMetadataType) super.getDefinedInType();
        }

        public Set<AtlasEntityType> getApplicableEntityTypes() {
            return applicableEntityTypes;
        }

        public String getValidPattern() {
            return validPattern;
        }

        public int getMaxStringLength() {
            return maxStringLength;
        }

        public boolean isValidLength(Object value) {
            boolean ret = true;
            if (value != null) {
                AtlasType attrType = getAttributeType();

                if (attrType instanceof AtlasBuiltInTypes.AtlasStringType) {
                    ret = isValidStringValue(value);
                } else if (attrType instanceof AtlasArrayType) {
                    attrType = ((AtlasArrayType) attrType).getElementType();
                    if (attrType instanceof AtlasBuiltInTypes.AtlasStringType) {
                        ret = isValidArrayValue(value);
                    }
                }
            }
            return ret;
        }

        private boolean isValidStringValue(Object obj) {
            return obj == null || String.valueOf(obj).length() <= this.maxStringLength;
        }

        private boolean isValidArrayValue(Object obj) {
            if (obj instanceof List || obj instanceof Set) {
                Collection objList = (Collection) obj;

                for (Object element : objList) {
                    if (!isValidStringValue(element)) {
                        return false;
                    }
                }
            } else if (obj.getClass().isArray()) {
                int arrayLen = Array.getLength(obj);
                for (int i = 0; i < arrayLen; i++) {
                    if (!isValidStringValue(Array.get(obj, i))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
