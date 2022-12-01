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
package org.apache.atlas.entitytransform;

import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.lang.StringUtils;

import static org.apache.atlas.entitytransform.TransformationConstants.TYPE_NAME_ATTRIBUTE_NAME_SEP;

public class EntityAttribute {
    private final String          attributeKey;
    private final AtlasEntityType entityType;
    private final String          attributeName;

    public EntityAttribute(String attributeKey, TransformerContext context) {
        this.attributeKey = attributeKey;

        int idx = attributeKey != null ? attributeKey.indexOf(TYPE_NAME_ATTRIBUTE_NAME_SEP) : -1;

        if (idx != -1) {
            this.attributeName = StringUtils.trim(attributeKey.substring(idx + 1));

            AtlasTypeRegistry typeRegistry = context != null ? context.getTypeRegistry() : null;

            if (typeRegistry != null) {
                String typeName = StringUtils.trim(attributeKey.substring(0, idx));

                this.entityType = typeRegistry.getEntityTypeByName(typeName);
            } else {
                this.entityType = null;
            }
        } else {
            this.entityType    = null;
            this.attributeName = attributeKey;
        }
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public AtlasEntityType getEntityType() {
        return entityType;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public boolean appliesToEntityType(String typeName) {
        return entityType == null || StringUtils.isEmpty(typeName) || entityType.getTypeAndAllSubTypes().contains(typeName);
    }
}
