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

package org.apache.atlas.repository.store.graph.v2;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.AtlasEntityUtil;
import org.apache.commons.collections.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.atlas.repository.graph.GraphHelper.getCustomAttributes;

public class AtlasEntityComparator {
    private final AtlasTypeRegistry    typeRegistry;
    private final EntityGraphRetriever entityRetriever;
    private final Map<String, String>  guidRefMap;
    private final boolean              skipClassificationCompare;
    private final boolean              skipBusinessAttributeCompare;

    public AtlasEntityComparator(AtlasTypeRegistry typeRegistry, EntityGraphRetriever entityRetriever, Map<String, String> guidRefMap,
                                 boolean skipClassificationCompare, boolean skipBusinessAttributeCompare) {
        this.typeRegistry                 = typeRegistry;
        this.entityRetriever              = entityRetriever;
        this.guidRefMap                   = guidRefMap;
        this.skipClassificationCompare    = skipClassificationCompare;
        this.skipBusinessAttributeCompare = skipBusinessAttributeCompare;
    }

    public AtlasEntityDiffResult getDiffResult(AtlasEntity updatedEntity, AtlasEntity storedEntity, boolean findOnlyFirstDiff) throws AtlasBaseException {
        return getDiffResult(updatedEntity, storedEntity, null, findOnlyFirstDiff);
    }

    public AtlasEntityDiffResult getDiffResult(AtlasEntity updatedEntity, AtlasVertex storedVertex, boolean findOnlyFirstDiff) throws AtlasBaseException {
        return getDiffResult(updatedEntity, null, storedVertex, findOnlyFirstDiff);
    }

    private AtlasEntityDiffResult getDiffResult(AtlasEntity updatedEntity, AtlasEntity storedEntity, AtlasVertex storedVertex, boolean findOnlyFirstDiff) throws AtlasBaseException {
        AtlasEntity                              diffEntity                       = new AtlasEntity(updatedEntity.getTypeName());
        AtlasEntityType                          entityType                       = typeRegistry.getEntityTypeByName(updatedEntity.getTypeName());
        Map<String, AtlasAttribute>              entityTypeAttributes             = entityType.getAllAttributes();
        Map<String, Map<String, AtlasAttribute>> entityTypeRelationshipAttributes = entityType.getRelationshipAttributes();

        int     sectionsWithDiff                = 0;
        boolean hasDiffInAttributes             = false;
        boolean hasDiffInRelationshipAttributes = false;
        boolean hasDiffInCustomAttributes       = false;
        boolean hasDiffInBusinessAttributes     = false;

        diffEntity.setGuid(updatedEntity.getGuid());

        if (MapUtils.isNotEmpty(updatedEntity.getAttributes())) { // check for attribute value change
            for (Map.Entry<String, Object> entry : updatedEntity.getAttributes().entrySet()) {
                String         attrName  = entry.getKey();
                AtlasAttribute attribute = entityTypeAttributes.get(attrName);

                if (attribute == null) { // no such attribute
                    continue;
                }

                Object newVal  = entry.getValue();
                Object currVal = (storedEntity != null) ? storedEntity.getAttribute(attrName) : entityRetriever.getEntityAttribute(storedVertex, attribute);

                if (!attribute.getAttributeType().areEqualValues(currVal, newVal, guidRefMap)) {
                    hasDiffInAttributes = true;

                    diffEntity.setAttribute(attrName, newVal);

                    if (findOnlyFirstDiff) {
                        return new AtlasEntityDiffResult(diffEntity, true, false, false);
                    }
                }
            }

            if (hasDiffInAttributes) {
                sectionsWithDiff++;
            }
        }

        if (MapUtils.isNotEmpty(updatedEntity.getRelationshipAttributes())) { // check for relationship-attribute value change
            for (Map.Entry<String, Object> entry : updatedEntity.getRelationshipAttributes().entrySet()) {
                String attrName = entry.getKey();

                if (!entityTypeRelationshipAttributes.containsKey(attrName)) {  // no such attribute
                    continue;
                }

                Object         newVal           = entry.getValue();
                String         relationshipType = AtlasEntityUtil.getRelationshipType(newVal);
                AtlasAttribute attribute        = entityType.getRelationshipAttribute(attrName, relationshipType);
                Object         currVal          = (storedEntity != null) ? storedEntity.getRelationshipAttribute(attrName) : entityRetriever.getEntityAttribute(storedVertex, attribute);

                if (!attribute.getAttributeType().areEqualValues(currVal, newVal, guidRefMap)) {
                    hasDiffInRelationshipAttributes = true;

                    diffEntity.setRelationshipAttribute(attrName, newVal);

                    if (findOnlyFirstDiff) {
                        return new AtlasEntityDiffResult(diffEntity, true, false, false);
                    }
                }
            }

            if (hasDiffInRelationshipAttributes) {
                sectionsWithDiff++;
            }
        }

        if (!skipClassificationCompare) {
            List<AtlasClassification> newVal  = updatedEntity.getClassifications();
            List<AtlasClassification> currVal = (storedEntity != null) ? storedEntity.getClassifications() : entityRetriever.getAllClassifications(storedVertex);

            if (!Objects.equals(currVal, newVal)) {
                diffEntity.setClassifications(newVal);

                sectionsWithDiff++;

                if (findOnlyFirstDiff) {
                    return new AtlasEntityDiffResult(diffEntity, true, false, false);
                }
            }
        }

        if (updatedEntity.getCustomAttributes() != null) {
            // event coming from hook does not have custom attributes, such events must not remove existing attributes
            // UI sends empty object in case of of intended removal.
            Map<String, String> newCustomAttributes  = updatedEntity.getCustomAttributes();
            Map<String, String> currCustomAttributes = (storedEntity != null) ? storedEntity.getCustomAttributes() : getCustomAttributes(storedVertex);

            if (!Objects.equals(currCustomAttributes, newCustomAttributes)) {
                diffEntity.setCustomAttributes(newCustomAttributes);

                hasDiffInCustomAttributes = true;
                sectionsWithDiff++;

                if (findOnlyFirstDiff && sectionsWithDiff > 1) {
                    return new AtlasEntityDiffResult(diffEntity, true, false, false);
                }
            }
        }

        if (!skipBusinessAttributeCompare) {
            Map<String, Map<String, Object>> newBusinessMetadata  = updatedEntity.getBusinessAttributes();
            Map<String, Map<String, Object>> currBusinessMetadata = (storedEntity != null) ? storedEntity.getBusinessAttributes() : entityRetriever.getBusinessMetadata(storedVertex);;

            if (!Objects.equals(currBusinessMetadata, newBusinessMetadata)) {
                diffEntity.setBusinessAttributes(newBusinessMetadata);

                hasDiffInBusinessAttributes = true;
                sectionsWithDiff++;

                if (findOnlyFirstDiff && sectionsWithDiff > 1) {
                    return new AtlasEntityDiffResult(diffEntity, true, false, false);
                }
            }
        }

        return new AtlasEntityDiffResult(diffEntity, sectionsWithDiff > 0, sectionsWithDiff == 1 && hasDiffInCustomAttributes, sectionsWithDiff == 1 && hasDiffInBusinessAttributes);
    }

    public static class AtlasEntityDiffResult {
        private final AtlasEntity diffEntity;
        private final boolean     hasDifference;
        private final boolean     hasDifferenceOnlyInCustomAttributes;
        private final boolean     hasDifferenceOnlyInBusinessAttributes;

        AtlasEntityDiffResult(AtlasEntity diffEntity, boolean hasDifference, boolean hasDifferenceOnlyInCustomAttributes, boolean hasDifferenceOnlyInBusinessAttributes) {
            this.diffEntity                            = diffEntity;
            this.hasDifference                         = hasDifference;
            this.hasDifferenceOnlyInCustomAttributes   = hasDifferenceOnlyInCustomAttributes;
            this.hasDifferenceOnlyInBusinessAttributes = hasDifferenceOnlyInBusinessAttributes;
        }

        public AtlasEntity getDiffEntity() {
            return diffEntity;
        }

        public boolean hasDifference() {
            return hasDifference;
        }

        public boolean hasDifferenceOnlyInCustomAttributes() {
            return hasDifferenceOnlyInCustomAttributes;
        }

        public boolean hasDifferenceOnlyInBusinessAttributes() {
            return hasDifferenceOnlyInBusinessAttributes;
        }
    }
}
