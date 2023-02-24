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

import org.apache.atlas.model.impexp.AttributeTransform;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class BaseEntityHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BaseEntityHandler.class);

    protected final List<AtlasEntityTransformer> transformers;


    public BaseEntityHandler(List<AtlasEntityTransformer> transformers) {
        this.transformers = transformers;
    }

    public AtlasEntity transform(AtlasEntity entity) {
        if (CollectionUtils.isEmpty(transformers)) {
            return entity;
        }

        AtlasTransformableEntity transformableEntity = getTransformableEntity(entity);

        if (transformableEntity == null) {
            return entity;
        }

        for (AtlasEntityTransformer transformer : transformers) {
            transformer.transform(transformableEntity);
        }

        transformableEntity.transformComplete();

        return entity;
    }

    public AtlasTransformableEntity getTransformableEntity(AtlasEntity entity) {
        return new AtlasTransformableEntity(entity);
    }

    public static List<BaseEntityHandler> createEntityHandlers(List<AttributeTransform> transforms, TransformerContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> BaseEntityHandler.createEntityHandlers(transforms={})", transforms);
        }

        List<BaseEntityHandler> ret = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(transforms)) {
            List<AtlasEntityTransformer> transformers = new ArrayList<>();

            for (AttributeTransform transform : transforms) {
                transformers.add(new AtlasEntityTransformer(transform, context));
            }

            if (hasTransformerForAnyAttribute(transformers, HdfsPathEntityHandler.CUSTOM_TRANSFORM_ATTRIBUTES)) {
                ret.add(new HdfsPathEntityHandler(transformers));
            }

            if (hasTransformerForAnyAttribute(transformers, HiveDatabaseEntityHandler.CUSTOM_TRANSFORM_ATTRIBUTES)) {
                ret.add(new HiveDatabaseEntityHandler(transformers));
            }

            if (hasTransformerForAnyAttribute(transformers, HiveTableEntityHandler.CUSTOM_TRANSFORM_ATTRIBUTES)) {
                ret.add(new HiveTableEntityHandler(transformers));
            }

            if (hasTransformerForAnyAttribute(transformers, HiveColumnEntityHandler.CUSTOM_TRANSFORM_ATTRIBUTES)) {
                ret.add(new HiveColumnEntityHandler(transformers));
            }

            if (hasTransformerForAnyAttribute(transformers, HiveStorageDescriptorEntityHandler.CUSTOM_TRANSFORM_ATTRIBUTES)) {
                ret.add(new HiveStorageDescriptorEntityHandler(transformers));
            }

            if (CollectionUtils.isEmpty(ret)) {
                ret.add(new BaseEntityHandler(transformers));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== BaseEntityHandler.createEntityHandlers(transforms={}): ret.size={}", transforms, ret.size());
        }

        return ret;
    }

    private static boolean hasTransformerForAnyAttribute(List<AtlasEntityTransformer> transformers, List<String> attributes) {
        if (CollectionUtils.isNotEmpty(transformers) && CollectionUtils.isNotEmpty(attributes)) {
            for (AtlasEntityTransformer transformer : transformers) {
                for (Action action : transformer.getActions()) {
                    if (attributes.contains(action.getAttribute().getAttributeKey())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static class AtlasTransformableEntity {
        protected final AtlasEntity entity;

        protected AtlasTransformableEntity(AtlasEntity entity) {
            this.entity = entity;
        }

        public AtlasEntity getEntity() {
            return entity;
        }

        public Object getAttribute(EntityAttribute attribute) {
            final Object ret;

            if (attribute.appliesToEntityType(entity.getTypeName())) {
                ret = entity.getAttribute(attribute.getAttributeName());
            } else {
                ret = null;
            }

            return ret;
        }

        public void setAttribute(EntityAttribute attribute, String attributeValue) {
            if (attribute.appliesToEntityType(entity.getTypeName())) {
                entity.setAttribute(attribute.getAttributeName(), attributeValue);
            }
        }

        public boolean hasAttribute(EntityAttribute attribute) {
            return getAttribute(attribute) != null;
        }

        public void transformComplete() {
            // implementations can override to set value of computed-attributes
        }
    }

    public static List<BaseEntityHandler> fromJson(String transformersString, TransformerContext context) {
        if (StringUtils.isEmpty(transformersString)) {
            return null;
        }

        Object transformersObj = AtlasType.fromJson(transformersString, Object.class);
        List transformers = (transformersObj != null && transformersObj instanceof List) ? (List) transformersObj : null;

        List<AttributeTransform> attributeTransforms = new ArrayList<>();

        if (CollectionUtils.isEmpty(transformers)) {
            return null;
        }

        for (Object transformer : transformers) {
            String transformerStr = AtlasType.toJson(transformer);
            AttributeTransform attributeTransform = AtlasType.fromJson(transformerStr, AttributeTransform.class);

            if (attributeTransform == null) {
                continue;
            }

            attributeTransforms.add(attributeTransform);
        }

        if (CollectionUtils.isEmpty(attributeTransforms)) {
            return null;
        }

        List<BaseEntityHandler> entityHandlers = createEntityHandlers(attributeTransforms, context);
        if (CollectionUtils.isEmpty(entityHandlers)) {
            return null;
        }

        return entityHandlers;
    }
}