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
package org.apache.atlas.repository.ogm.glossary;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AtlasGlossaryDTO extends AbstractGlossaryDTO<AtlasGlossary> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasGlossaryDTO.class);

    @Inject
    public AtlasGlossaryDTO(AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, AtlasGlossary.class);
    }

    @Override
    public AtlasGlossary from(final AtlasEntity entity) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryDTO.from()", entity);
        }

        Objects.requireNonNull(entity, "entity");

        AtlasGlossary ret = new AtlasGlossary();

        ret.setGuid(entity.getGuid());
        ret.setQualifiedName((String) entity.getAttribute("qualifiedName"));
        ret.setName((String) entity.getAttribute("name"));
        ret.setShortDescription((String) entity.getAttribute("shortDescription"));
        ret.setLongDescription((String) entity.getAttribute("longDescription"));
        ret.setLanguage((String) entity.getAttribute("language"));
        ret.setUsage((String) entity.getAttribute("usage"));
        ret.setAdditionalAttributes((Map) entity.getAttribute("additionalAttributes"));

        Object categoriesAttr = entity.getRelationshipAttribute("categories");
        Object termsAttr = entity.getRelationshipAttribute("terms");

        // Populate categories
        if (Objects.nonNull(categoriesAttr)) {
            LOG.debug("Processing categories");
            if (categoriesAttr instanceof Collection) {
                for (Object o : (Collection) categoriesAttr) {
                    if (o instanceof AtlasRelatedObjectId) {
                        ret.addCategory(constructRelatedCategoryId((AtlasRelatedObjectId) o));
                    }
                }
            }
        }

        // Populate terms
        if (Objects.nonNull(termsAttr)) {
            LOG.debug("Processing terms");
            if (termsAttr instanceof Collection) {
                for (Object o : (Collection) termsAttr) {
                    if (o instanceof AtlasRelatedObjectId) {
                        ret.addTerm(constructRelatedTermId((AtlasRelatedObjectId) o));
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public AtlasGlossary from(final AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryDTO.from()", entityWithExtInfo);
        }
        Objects.requireNonNull(entityWithExtInfo, "entity");
        AtlasGlossary ret = from(entityWithExtInfo.getEntity());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryDTO.from() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity toEntity(final AtlasGlossary obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryDTO.toEntity()", obj);
        }
        Objects.requireNonNull(obj, "atlasGlossary");
        Objects.requireNonNull(obj.getQualifiedName(), "atlasGlossary qualifiedName must be specified");

        AtlasEntity ret = getDefaultAtlasEntity(obj);

        ret.setAttribute("qualifiedName", obj.getQualifiedName());
        ret.setAttribute("name", obj.getName());
        ret.setAttribute("shortDescription", obj.getShortDescription());
        ret.setAttribute("longDescription", obj.getLongDescription());
        ret.setAttribute("language", obj.getLanguage());
        ret.setAttribute("usage", obj.getUsage());
        ret.setAttribute("additionalAttributes", obj.getAdditionalAttributes());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryDTO.toEntity() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo toEntityWithExtInfo(final AtlasGlossary obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryDTO.toEntityWithExtInfo()", obj);
        }
        Objects.requireNonNull(obj, "atlasGlossary");
        AtlasEntity                        entity = toEntity(obj);
        AtlasEntity.AtlasEntityWithExtInfo ret = new AtlasEntity.AtlasEntityWithExtInfo(entity);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryDTO.toEntityWithExtInfo() : {}", ret);
        }
        return ret;
    }

    @Override
    public Map<String, Object> getUniqueAttributes(final AtlasGlossary obj) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("qualifiedName", obj.getQualifiedName());
        return ret;
    }
}
