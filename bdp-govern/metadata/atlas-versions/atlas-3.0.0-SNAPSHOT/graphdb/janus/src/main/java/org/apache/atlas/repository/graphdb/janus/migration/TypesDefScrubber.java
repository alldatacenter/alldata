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

import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_ARRAY_PREFIX;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_ARRAY_SUFFIX;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_MAP_KEY_VAL_SEP;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_MAP_PREFIX;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_MAP_SUFFIX;

public class TypesDefScrubber {
    private static final Logger LOG = LoggerFactory.getLogger(TypesDefScrubber.class);

    public  static final String LEGACY_TYPE_NAME_PREFIX = "legacy";

    private final Map<String, ClassificationToStructDefName> edgeLabelToClassificationToStructDefMap = new HashMap<>();
    private final Map<String, Integer>                       classificationIndexMap                  = new HashMap<>();
    private       AtlasTypesDef                              typesDef;

    public TypesDefScrubber() {
    }

    public AtlasTypesDef scrub(AtlasTypesDef typesDef) {
        this.typesDef = typesDef;

        display("incoming: ", typesDef);

        createClassificationNameIndexMap(typesDef.getClassificationDefs());

        for (AtlasStructDef structDef : new ArrayList<>(typesDef.getStructDefs())) { // work on copy of typesDef.getStructDefs(), as the list is modified by checkAndUpdate()
            checkAndUpdate(structDef);
        }

        for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
            checkAndUpdate(entityDef);
        }

        display("scrubbed: ", typesDef);

        return typesDef;
    }

    public Map<String, ClassificationToStructDefName> getTraitToTypeMap() {
        return edgeLabelToClassificationToStructDefMap;
    }

    public static String getEdgeLabel(String typeName, String attributeName) {
        return String.format("%s%s.%s", Constants.INTERNAL_PROPERTY_KEY_PREFIX, typeName, attributeName);
    }

    public static String getLegacyTypeNameForStructDef(String name) {
        return String.format("%s_%s", LEGACY_TYPE_NAME_PREFIX, name);
    }

    private void display(String s, AtlasTypesDef typesDef) {
        if(LOG.isDebugEnabled()) {
            LOG.debug(s + "{}", typesDef.toString());
        }
    }

    private void checkAndUpdate(AtlasStructDef structDef) {
        for (AtlasAttributeDef attrDef : structDef.getAttributeDefs()) {
            String attrTypeName = getAttributeTypeName(attrDef.getTypeName());

            if (classificationIndexMap.containsKey(attrTypeName)) {
                ClassificationToStructDefName pair = createLegacyStructDefFromClassification(attrTypeName);

                if (pair != null) {
                    updateAttributeWithNewType(pair.getTypeName(), pair.getLegacyTypeName(), attrDef);

                    addStructDefToTypesDef(structDef.getName(), attrDef.getName(), pair);

                    LOG.info("scrubbed: {}:{} -> {}", structDef.getName(), attrDef.getName(), attrDef.getTypeName());
                }
            }
        }
    }

    private String getAttributeTypeName(String typeName) {
        if (AtlasTypeUtil.isArrayType(typeName)) {
            int    startIdx        = ATLAS_TYPE_ARRAY_PREFIX.length();
            int    endIdx          = typeName.length() - ATLAS_TYPE_ARRAY_SUFFIX.length();
            String elementTypeName = typeName.substring(startIdx, endIdx).trim();

            return elementTypeName;
        } else if (AtlasTypeUtil.isMapType(typeName)) {
            int      startIdx      = ATLAS_TYPE_MAP_PREFIX.length();
            int      endIdx        = typeName.length() - ATLAS_TYPE_MAP_SUFFIX.length();
            String[] keyValueTypes = typeName.substring(startIdx, endIdx).split(ATLAS_TYPE_MAP_KEY_VAL_SEP, 2);
            String   valueTypeName = keyValueTypes.length > 1 ? keyValueTypes[1].trim() : null;

            return valueTypeName;
        }

        return typeName;
    }

    private void updateAttributeWithNewType(String oldTypeName, String newTypeName, AtlasAttributeDef ad) {
        if(StringUtils.isEmpty(newTypeName)) {
            return;
        }

        String str = ad.getTypeName().replace(oldTypeName, newTypeName);

        ad.setTypeName(str);
    }

    private ClassificationToStructDefName createLegacyStructDefFromClassification(String typeName) {
        AtlasClassificationDef classificationDef = getClassificationDefByName(typeName);

        if (classificationDef == null) {
            return null;
        }

        AtlasStructDef structDef = getStructDefFromClassificationDef(classificationDef);

        addStructDefToTypesDef(structDef);

        return new ClassificationToStructDefName(classificationDef.getName(), structDef.getName());
    }

    private void addStructDefToTypesDef(AtlasStructDef structDef) {
        for (AtlasStructDef sDef : typesDef.getStructDefs()) {
            if (StringUtils.equals(sDef.getName(), structDef.getName())) {
                return;
            }
        }

        typesDef.getStructDefs().add(structDef);
    }

    private void addStructDefToTypesDef(String typeName, String attributeName, ClassificationToStructDefName pair) {
        String key = getEdgeLabel(typeName, attributeName);

        edgeLabelToClassificationToStructDefMap.put(key, pair);
    }

    private AtlasClassificationDef getClassificationDefByName(String name) {
        if (classificationIndexMap.containsKey(name)) {
            return typesDef.getClassificationDefs().get(classificationIndexMap.get(name));
        }

        return null;
    }

    private AtlasStructDef getStructDefFromClassificationDef(AtlasClassificationDef classificationDef) {
        String legacyTypeName = getLegacyTypeNameForStructDef(classificationDef.getName());

        return new AtlasStructDef(legacyTypeName, classificationDef.getDescription(), classificationDef.getTypeVersion(),
                                  getDefaultAttributeDefsIfNecessary(classificationDef.getAttributeDefs()));
    }

    private List<AtlasAttributeDef> getDefaultAttributeDefsIfNecessary(List<AtlasAttributeDef> attributeDefs) {
        return attributeDefs.isEmpty() ? Collections.singletonList(new AtlasAttributeDef("name", "string")) : attributeDefs;
    }

    private void createClassificationNameIndexMap(List<AtlasClassificationDef> classificationDefs) {
        for (int i = 0; i < classificationDefs.size(); i++) {
            AtlasClassificationDef classificationDef = classificationDefs.get(i);

            classificationIndexMap.put(classificationDef.getName(), i);
        }
    }

    public static class ClassificationToStructDefName extends TypesUtil.Pair<String, String> {
        public ClassificationToStructDefName(String typeName, String legacyTypeName) {
            super(typeName, legacyTypeName);
        }

        public String getTypeName() {
            return left;
        }

        public String getLegacyTypeName() {
            return right;
        }
    }
}
