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
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collection;

@Component
public class AtlasGlossaryCategoryDTO extends AbstractGlossaryDTO<AtlasGlossaryCategory> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasGlossaryCategoryDTO.class);

    @Inject
    protected AtlasGlossaryCategoryDTO(final AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, AtlasGlossaryCategory.class);
    }

    @Override
    public AtlasGlossaryCategory from(final AtlasEntity entity) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryCategoryDTO.from()", entity);
        }
        Objects.requireNonNull(entity, "entity");

        AtlasGlossaryCategory ret = new AtlasGlossaryCategory();

        ret.setGuid(entity.getGuid());
        ret.setQualifiedName((String) entity.getAttribute("qualifiedName"));
        ret.setName((String) entity.getAttribute("name"));
        ret.setShortDescription((String) entity.getAttribute("shortDescription"));
        ret.setLongDescription((String) entity.getAttribute("longDescription"));
        ret.setAdditionalAttributes((Map) entity.getAttribute("additionalAttributes"));

        Object anchor = entity.getRelationshipAttribute("anchor");
        if (anchor instanceof AtlasRelatedObjectId) {
            LOG.debug("Processing anchor");
            ret.setAnchor(constructGlossaryId((AtlasRelatedObjectId) anchor));
        }

        Object parentCategory = entity.getRelationshipAttribute("parentCategory");
        if (parentCategory instanceof AtlasRelatedObjectId) {
            LOG.debug("Processing parentCategory");
            ret.setParentCategory(constructRelatedCategoryId((AtlasRelatedObjectId) parentCategory));
        }

        Object childrenCategories = entity.getRelationshipAttribute("childrenCategories");
        if (childrenCategories instanceof Collection) {
            LOG.debug("Processing childrenCategories");
            for (Object child : (Collection) childrenCategories) {
                if (child instanceof AtlasRelatedObjectId) {
                    if (((AtlasRelatedObjectId) child).getRelationshipStatus() == AtlasRelationship.Status.ACTIVE) {
                        ret.addChild(constructRelatedCategoryId((AtlasRelatedObjectId) child));
                    }
                }
            }
        }

        Object terms = entity.getRelationshipAttribute("terms");
        if (terms instanceof Collection) {
            LOG.debug("Processing terms");
            for (Object term : (Collection) terms) {
                if (term instanceof AtlasRelatedObjectId) {
                    ret.addTerm(constructRelatedTermId((AtlasRelatedObjectId) term));
                }
            }
        }

        return ret;
    }

    @Override
    public AtlasGlossaryCategory from(final AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryCategoryDTO.from()", entityWithExtInfo);
        }
        Objects.requireNonNull(entityWithExtInfo, "entity");
        AtlasGlossaryCategory ret = from(entityWithExtInfo.getEntity());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryCategoryDTO.from() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity toEntity(final AtlasGlossaryCategory obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryCategoryDTO.toEntity()", obj);
        }
        Objects.requireNonNull(obj, "atlasGlossaryCategory");
        Objects.requireNonNull(obj.getQualifiedName(), "atlasGlossaryCategory qualifiedName must be specified");
        Objects.requireNonNull(obj.getAnchor(), "atlasGlossaryCategory anchor must be specified");

        AtlasEntity ret = getDefaultAtlasEntity(obj);

        ret.setAttribute("qualifiedName", obj.getQualifiedName());
        ret.setAttribute("name", obj.getName());
        ret.setAttribute("shortDescription", obj.getShortDescription());
        ret.setAttribute("longDescription", obj.getLongDescription());
        ret.setAttribute("anchor", new AtlasObjectId(obj.getAnchor().getGlossaryGuid()));
        ret.setAttribute("additionalAttributes", obj.getAdditionalAttributes());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryCategoryDTO.toEntity() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo toEntityWithExtInfo(final AtlasGlossaryCategory obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryCategoryDTO.toEntityWithExtInfo()", obj);
        }
        Objects.requireNonNull(obj, "atlasGlossaryCategory");
        AtlasEntity.AtlasEntityWithExtInfo ret = new AtlasEntity.AtlasEntityWithExtInfo(toEntity(obj));

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryCategoryDTO.toEntityWithExtInfo() : {}", ret);
        }
        return ret;
    }

    @Override
    public Map<String, Object> getUniqueAttributes(final AtlasGlossaryCategory obj) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("qualifiedName", obj.getQualifiedName());
        return ret;
    }
}
