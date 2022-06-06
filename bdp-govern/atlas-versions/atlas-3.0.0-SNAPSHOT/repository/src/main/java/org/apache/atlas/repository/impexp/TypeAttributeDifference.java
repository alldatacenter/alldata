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
package org.apache.atlas.repository.impexp;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasRelationshipType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TypeAttributeDifference {
    private static final Logger LOG = LoggerFactory.getLogger(TypeAttributeDifference.class);

    private final AtlasTypeDefStore typeDefStore;
    private final AtlasTypeRegistry typeRegistry;

    public TypeAttributeDifference(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) {
        this.typeDefStore = typeDefStore;
        this.typeRegistry = typeRegistry;
    }

    public void updateTypes(AtlasTypesDef typeDefinitionMap, AtlasImportResult result) throws AtlasBaseException {
        updateEnumDef(typeDefinitionMap.getEnumDefs(), result);
        updateStructDef(typeDefinitionMap.getStructDefs(), result);
        updateClassificationDef(typeDefinitionMap.getClassificationDefs(), result);
        updateEntityDef(typeDefinitionMap.getEntityDefs(), result);
        updateRelationshipDefs(typeDefinitionMap.getRelationshipDefs(), result);
        updateBusinessMetadataDefs(typeDefinitionMap.getBusinessMetadataDefs(), result);
    }

    private void updateEntityDef(List<AtlasEntityDef> entityDefs, AtlasImportResult result) throws AtlasBaseException {
        for (AtlasEntityDef def : entityDefs) {
            AtlasEntityDef existing = typeRegistry.getEntityDefByName(def.getName());
            if (existing != null && addAttributes(existing, def)) {
                typeDefStore.updateEntityDefByName(existing.getName(), existing);
                result.incrementMeticsCounter("typedef:entitydef:update");
            }
        }
    }

    private void updateClassificationDef(List<AtlasClassificationDef> classificationDefs, AtlasImportResult result) throws AtlasBaseException {
        for (AtlasClassificationDef def : classificationDefs) {
            AtlasClassificationDef existing = typeRegistry.getClassificationDefByName(def.getName());
            if (existing != null && addAttributes(existing, def)) {
                typeDefStore.updateClassificationDefByName(existing.getName(), existing);
                result.incrementMeticsCounter("typedef:classification:update");
            }
        }
    }

    private void updateEnumDef(List<AtlasEnumDef> enumDefs, AtlasImportResult result) throws AtlasBaseException {
        for (AtlasEnumDef def : enumDefs) {
            AtlasEnumDef existing = typeRegistry.getEnumDefByName(def.getName());
            if (existing != null && addElements(existing, def)) {
                typeDefStore.updateEnumDefByName(existing.getName(), existing);
                result.incrementMeticsCounter("typedef:enum:update");
            } else {

            }
        }
    }

    private void updateStructDef(List<AtlasStructDef> structDefs, AtlasImportResult result) throws AtlasBaseException {
        for (AtlasStructDef def : structDefs) {
            AtlasStructDef existing = typeRegistry.getStructDefByName(def.getName());
            if (existing != null && addAttributes(existing, def)) {
                typeDefStore.updateStructDefByName(existing.getName(), existing);
                result.incrementMeticsCounter("typedef:struct:update");
            }
        }
    }

    private void updateRelationshipDefs(List<AtlasRelationshipDef> relationshipDefs, AtlasImportResult result) throws AtlasBaseException {
        for (AtlasRelationshipDef def : relationshipDefs) {
            AtlasRelationshipDef existing = typeRegistry.getRelationshipDefByName(def.getName());
            if (existing != null && addAttributes(existing, def)) {
                typeDefStore.updateRelationshipDefByName(existing.getName(), existing);
                result.incrementMeticsCounter("typedef:relationshipdef:update");
            }
        }
    }

    private void updateBusinessMetadataDefs(List<AtlasBusinessMetadataDef> businessMetadataDefs, AtlasImportResult result) throws AtlasBaseException {
        for (AtlasBusinessMetadataDef def : businessMetadataDefs) {
            AtlasBusinessMetadataDef existing = typeRegistry.getBusinessMetadataDefByName(def.getName());
            if (existing != null && addAttributes(existing, def)) {
                typeDefStore.updateStructDefByName(existing.getName(), existing);
                result.incrementMeticsCounter("typedef:businessmetadatadef:update");
            }
        }
    }

    @VisibleForTesting
    boolean addElements(AtlasEnumDef existing, AtlasEnumDef incoming) throws AtlasBaseException {
        return addElements(existing, getElementsAbsentInExisting(existing, incoming));
    }

    private boolean addAttributes(AtlasStructDef existing, AtlasStructDef incoming) throws AtlasBaseException {
        return addAttributes(existing, getElementsAbsentInExisting(existing, incoming));
    }

    @VisibleForTesting
    List<AtlasStructDef.AtlasAttributeDef> getElementsAbsentInExisting(AtlasStructDef existing, AtlasStructDef incoming) throws AtlasBaseException {
        List<AtlasStructDef.AtlasAttributeDef> difference = new ArrayList<>();
        for (AtlasStructDef.AtlasAttributeDef attr : incoming.getAttributeDefs()) {
            updateCollectionWithDifferingAttributes(difference, existing, attr);
        }

        return difference;
    }

    private void updateCollectionWithDifferingAttributes(List<AtlasStructDef.AtlasAttributeDef> difference,
                                                         AtlasStructDef existing,
                                                         AtlasStructDef.AtlasAttributeDef incoming) throws AtlasBaseException {
        AtlasStructDef.AtlasAttributeDef existingAttribute = existing.getAttribute(incoming.getName());
        if (existingAttribute == null) {
            AtlasRelationshipType relationshipType = AtlasTypeUtil.findRelationshipWithLegacyRelationshipEnd(existing.getName(), incoming.getName(), typeRegistry);

            if (relationshipType == null) {
                difference.add(incoming);
            }
        } else {
            if (!existingAttribute.getTypeName().equals(incoming.getTypeName())) {
                LOG.error("Attribute definition difference found: {}, {}", existingAttribute, incoming);
                throw new AtlasBaseException(AtlasErrorCode.INVALID_IMPORT_ATTRIBUTE_TYPE_CHANGED, existing.getName(), existingAttribute.getName(), existingAttribute.getTypeName(), incoming.getTypeName());
            }
        }
    }

    @VisibleForTesting
    List<AtlasEnumDef.AtlasEnumElementDef> getElementsAbsentInExisting(AtlasEnumDef existing, AtlasEnumDef incoming) throws AtlasBaseException {
        List<AtlasEnumDef.AtlasEnumElementDef> difference = new ArrayList<>();
        for (AtlasEnumDef.AtlasEnumElementDef ed : incoming.getElementDefs()) {
            updateCollectionWithDifferingAttributes(existing, difference, ed);
        }

        return difference;
    }

    private void updateCollectionWithDifferingAttributes(AtlasEnumDef existing, List<AtlasEnumDef.AtlasEnumElementDef> difference, AtlasEnumDef.AtlasEnumElementDef ed) throws AtlasBaseException {
        AtlasEnumDef.AtlasEnumElementDef existingElement = existing.getElement(ed.getValue());
        if (existingElement == null) {
            difference.add(ed);
        }
    }

    private boolean addAttributes(AtlasStructDef def, List<AtlasStructDef.AtlasAttributeDef> list) {
        for (AtlasStructDef.AtlasAttributeDef ad : list) {
            def.addAttribute(ad);
        }

        return list.size() > 0;
    }

    private boolean addElements(AtlasEnumDef def, List<AtlasEnumDef.AtlasEnumElementDef> list) {
        for (AtlasEnumDef.AtlasEnumElementDef ad : list) {
            def.addElement(ad);
        }

        return list.size() > 0;
    }
}
