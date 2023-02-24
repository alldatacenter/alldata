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
package org.apache.atlas.repository.impexp;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportTransforms {
    private static final Logger LOG = LoggerFactory.getLogger(ImportTransforms.class);

    private static final String ALL_ATTRIBUTES = "*";

    private Map<String, Map<String, List<ImportTransformer>>> transforms;

    public static ImportTransforms fromJson(String jsonString) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        return new ImportTransforms(jsonString);
    }

    public Map<String, Map<String, List<ImportTransformer>>> getTransforms() {
        return transforms;
    }

    public Map<String, List<ImportTransformer>> getTransforms(String typeName) { return transforms.get(typeName); }

    public Set<String> getTypes() {
        return getTransforms().keySet();
    }

    public void addParentTransformsToSubTypes(String parentType, Set<String> subTypes) {
        Map<String, List<ImportTransformer>> attribtueTransformMap = getTransforms().get(parentType);
        for (String subType : subTypes) {
            if(!getTransforms().containsKey(subType)) {
                getTransforms().put(subType, attribtueTransformMap);
            } else {
                for (Map.Entry<String, List<ImportTransformer>> entry : attribtueTransformMap.entrySet()) {
                    if((getTransforms().get(subType).containsKey(entry.getKey()))){
                        getTransforms().get(subType).get(entry.getKey()).addAll(entry.getValue());
                    } else {
                        LOG.warn("Attribute {} does not exist for Type : {}", entry.getKey(), parentType);
                    }
                }
            }
        }
    }

    public AtlasEntity.AtlasEntityWithExtInfo apply(AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) throws AtlasBaseException {
        if (entityWithExtInfo == null) {
            return entityWithExtInfo;
        }

        apply(entityWithExtInfo.getEntity());

        if(MapUtils.isNotEmpty(entityWithExtInfo.getReferredEntities())) {
            for (AtlasEntity e : entityWithExtInfo.getReferredEntities().values()) {
                apply(e);
            }
        }

        return entityWithExtInfo;
    }

    public  AtlasEntity apply(AtlasEntity entity) throws AtlasBaseException {
        if (entity == null) {
            return entity;
        }

        Map<String, List<ImportTransformer>> entityTransforms = getTransforms(entity.getTypeName());
        if (MapUtils.isEmpty(entityTransforms)) {
            return entity;
        }

        applyEntitySpecific(entity, entityTransforms);

        applyAttributeSpecific(entity, entityTransforms);

        return entity;
    }

    private void applyAttributeSpecific(AtlasEntity entity, Map<String, List<ImportTransformer>> entityTransforms) throws AtlasBaseException {
        for (Map.Entry<String, List<ImportTransformer>> entry : entityTransforms.entrySet()) {
            String                   attributeName  = entry.getKey();
            List<ImportTransformer> attrTransforms = entry.getValue();

            if (!entity.hasAttribute(attributeName)) {
                continue;
            }

            Object attributeValue = entity.getAttribute(attributeName);
            for (ImportTransformer attrTransform : attrTransforms) {
                attributeValue = attrTransform.apply(attributeValue);
            }

            entity.setAttribute(attributeName, attributeValue);
        }
    }

    private void applyEntitySpecific(AtlasEntity entity, Map<String, List<ImportTransformer>> entityTransforms) throws AtlasBaseException {
        if(entityTransforms.containsKey(ALL_ATTRIBUTES)) {
            for (ImportTransformer attrTransform : entityTransforms.get(ALL_ATTRIBUTES)) {
                attrTransform.apply(entity);
            }
        }
    }

    private ImportTransforms() {
        transforms = new HashMap<>();
    }

    private ImportTransforms(String jsonString) {
        this();

        if (StringUtils.isEmpty(jsonString)) {
            return;
        }

        Map typeTransforms = AtlasType.fromJson(jsonString, Map.class);
        if (MapUtils.isEmpty(typeTransforms)) {
            return;
        }

        addOuterMap(typeTransforms);
    }

    private void addOuterMap(Map typeTransforms) {
        for (Object key : typeTransforms.keySet()) {
            Object              value               = typeTransforms.get(key);
            String              entityType          = (String) key;
            Map<String, Object> attributeTransforms = (Map<String, Object>)value;

            if (MapUtils.isEmpty(attributeTransforms)) {
                continue;
            }

            addInnerMap(entityType, attributeTransforms);
        }
    }

    private void addInnerMap(String entityType, Map<String, Object> attributeTransforms) {
        for (Map.Entry<String, Object> e : attributeTransforms.entrySet()) {
            String       attributeName = e.getKey();
            List<String> transforms    = (List<String>)e.getValue();

            if (CollectionUtils.isEmpty(transforms)) {
                continue;
            }

            addTransforms(entityType, attributeName, transforms);
        }
    }

    private void addTransforms(String entityType, String attributeName, List<String> transforms) {
        for (String transform : transforms) {
            ImportTransformer transformers = null;

            try {
                transformers = ImportTransformer.getTransformer(transform);
                if (transformers == null) {
                    continue;
                }

                add(entityType, attributeName, transformers);
            } catch (AtlasBaseException ex) {
                LOG.error("Error converting string to ImportTransformer: {}", transform, ex);
            }
        }
    }

    private void add(String typeName, String attributeName, ImportTransformer transformer) {
        Map<String, List<ImportTransformer>> attrMap;

        if(!transforms.containsKey(typeName)) {
            attrMap = new HashMap<>();
            transforms.put(typeName, attrMap);
        }

        attrMap = transforms.get(typeName);
        if(!attrMap.containsKey(attributeName)) {
            attrMap.put(attributeName, new ArrayList<ImportTransformer>());
        }

        attrMap.get(attributeName).add(transformer);
    }
}
