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
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AtlasGlossaryTermDTO extends AbstractGlossaryDTO<AtlasGlossaryTerm> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasGlossaryTermDTO.class);

    @Inject
    protected AtlasGlossaryTermDTO(final AtlasTypeRegistry typeRegistry) {
        super(typeRegistry, AtlasGlossaryTerm.class);
    }

    @Override
    public AtlasGlossaryTerm from(final AtlasEntity entity) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryTermDTO.from()", entity);
        }

        Objects.requireNonNull(entity, "entity");

        AtlasGlossaryTerm ret = new AtlasGlossaryTerm();

        ret.setGuid(entity.getGuid());
        ret.setQualifiedName((String) entity.getAttribute("qualifiedName"));
        ret.setName((String) entity.getAttribute("name"));
        ret.setShortDescription((String) entity.getAttribute("shortDescription"));
        ret.setLongDescription((String) entity.getAttribute("longDescription"));
        ret.setExamples((List<String>) entity.getAttribute("examples"));
        ret.setAbbreviation((String) entity.getAttribute("abbreviation"));
        ret.setUsage((String) entity.getAttribute("usage"));
        ret.setAdditionalAttributes((Map) entity.getAttribute("additionalAttributes"));

        Object anchor = entity.getRelationshipAttribute("anchor");
        if (anchor instanceof AtlasRelatedObjectId) {
            LOG.debug("Processing anchor");
            ret.setAnchor(constructGlossaryId((AtlasRelatedObjectId) anchor));
        }

        Object categories = entity.getRelationshipAttribute("categories");
        if (categories instanceof Collection) {
            LOG.debug("Processing categories");
            for (Object category : (Collection) categories) {
                if (category instanceof AtlasRelatedObjectId) {
                    ret.addCategory(constructTermCategorizationId((AtlasRelatedObjectId) category));
                }
            }
        }
//        ret.setContextRelevantTerms(toRelatedObjectIdsSet(entity.getRelationshipAttribute("contextRelevantTerms")));
//        ret.setUsedInContexts(toRelatedObjectIdsSet(entity.getRelationshipAttribute("usedInContexts")));

        Object assignedEntities = entity.getRelationshipAttribute("assignedEntities");
        if (assignedEntities instanceof Collection) {
            LOG.debug("Processing assigned entities");
            for (Object assignedEntity : (Collection) assignedEntities) {
                if (assignedEntity instanceof AtlasRelatedObjectId) {
                    AtlasRelatedObjectId id = (AtlasRelatedObjectId) assignedEntity;
                    // Since the edges are not a hard delete we need to filter the DELETED ones
                    if (id.getRelationshipStatus() == AtlasRelationship.Status.ACTIVE) {
                        ret.addAssignedEntity(id);
                    }
                }
            }
        }

        Object seeAlso = entity.getRelationshipAttribute("seeAlso");
        if (seeAlso instanceof Collection && CollectionUtils.isNotEmpty((Collection) seeAlso)) {
            LOG.debug("Processing RelatedTerm(seeAlso)");
            ret.setSeeAlso(toRelatedTermIdsSet(seeAlso));
        }

        Object synonyms = entity.getRelationshipAttribute("synonyms");
        if (synonyms instanceof Collection && CollectionUtils.isNotEmpty((Collection) synonyms)) {
            LOG.debug("Processing Synonym(synonyms)");
            ret.setSynonyms(toRelatedTermIdsSet(synonyms));
        }

        Object antonyms = entity.getRelationshipAttribute("antonyms");
        if (antonyms instanceof Collection && CollectionUtils.isNotEmpty((Collection) antonyms)) {
            LOG.debug("Processing Antonym(antonyms)");
            ret.setAntonyms(toRelatedTermIdsSet(antonyms));
        }

        Object preferredTerms = entity.getRelationshipAttribute("preferredTerms");
        if (preferredTerms instanceof Collection && CollectionUtils.isNotEmpty((Collection) preferredTerms)) {
            LOG.debug("Processing preferredTerm(preferredTerms)");
            ret.setPreferredTerms(toRelatedTermIdsSet(preferredTerms));
        }

        Object preferredToTerms = entity.getRelationshipAttribute("preferredToTerms");
        if (preferredToTerms instanceof Collection && CollectionUtils.isNotEmpty((Collection) preferredToTerms)) {
            LOG.debug("Processing preferredTerm(preferredToTerms)");
            ret.setPreferredToTerms(toRelatedTermIdsSet(preferredToTerms));
        }

        Object replacementTerms = entity.getRelationshipAttribute("replacementTerms");
        if (replacementTerms instanceof Collection && CollectionUtils.isNotEmpty((Collection) replacementTerms)) {
            LOG.debug("Processing ReplacementTerm(replacementTerms)");
            ret.setReplacementTerms(toRelatedTermIdsSet(replacementTerms));
        }

        Object replacedBy = entity.getRelationshipAttribute("replacedBy");
        if (replacedBy instanceof Collection && CollectionUtils.isNotEmpty((Collection) replacedBy)) {
            LOG.debug("Processing ReplacementTerm(replacedBy)");
            ret.setReplacedBy(toRelatedTermIdsSet(replacedBy));
        }

        Object translationTerms = entity.getRelationshipAttribute("translationTerms");
        if (translationTerms instanceof Collection && CollectionUtils.isNotEmpty((Collection) translationTerms)) {
            LOG.debug("Processing Translation(translationTerms)");
            ret.setTranslationTerms(toRelatedTermIdsSet(translationTerms));
        }

        Object translatedTerms = entity.getRelationshipAttribute("translatedTerms");
        if (translatedTerms instanceof Collection && CollectionUtils.isNotEmpty((Collection) translatedTerms)) {
            LOG.debug("Processing Translation(translatedTerms)");
            ret.setTranslatedTerms(toRelatedTermIdsSet(translatedTerms));
        }

        Object isA = entity.getRelationshipAttribute("isA");
        if (isA instanceof Collection && CollectionUtils.isNotEmpty((Collection) isA)) {
            LOG.debug("Processing Classifies(isA)");
            ret.setIsA(toRelatedTermIdsSet(isA));
        }

        Object classifies = entity.getRelationshipAttribute("classifies");
        if (classifies instanceof Collection && CollectionUtils.isNotEmpty((Collection) classifies)) {
            LOG.debug("Processing Classifies(classifies)");
            ret.setClassifies(toRelatedTermIdsSet(classifies));
        }

        Object validValues = entity.getRelationshipAttribute("validValues");
        if (validValues instanceof Collection && CollectionUtils.isNotEmpty((Collection) validValues)) {
            LOG.debug("Processing validValue(validValues)");
            ret.setValidValues(toRelatedTermIdsSet(validValues));
        }

        Object validValuesFor = entity.getRelationshipAttribute("validValuesFor");
        if (validValuesFor instanceof Collection && CollectionUtils.isNotEmpty((Collection) validValuesFor)) {
            LOG.debug("Processing validValue(validValuesFor)");
            ret.setValidValuesFor(toRelatedTermIdsSet(validValuesFor));
        }

        if (CollectionUtils.isNotEmpty(entity.getClassifications())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing term classifications");
            }
            ret.setClassifications(entity.getClassifications());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryTermDTO.from() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasGlossaryTerm from(final AtlasEntity.AtlasEntityWithExtInfo entityWithExtInfo) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryTermDTO.from()", entityWithExtInfo);
        }
        Objects.requireNonNull(entityWithExtInfo, "entityWithExtInfo");
        AtlasGlossaryTerm ret = from(entityWithExtInfo.getEntity());

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryTermDTO.from() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity toEntity(final AtlasGlossaryTerm obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryTermDTO.toEntity()", obj);
        }
        Objects.requireNonNull(obj, "atlasGlossaryTerm");
        Objects.requireNonNull(obj.getQualifiedName(), "atlasGlossaryTerm qualifiedName must be specified");
        Objects.requireNonNull(obj.getAnchor(), "atlasGlossaryTerm anchor must be specified");

        AtlasEntity ret = getDefaultAtlasEntity(obj);

        ret.setAttribute("qualifiedName", obj.getQualifiedName());
        ret.setAttribute("name", obj.getName());
        ret.setAttribute("shortDescription", obj.getShortDescription());
        ret.setAttribute("longDescription", obj.getLongDescription());
        ret.setAttribute("examples", obj.getExamples());
        ret.setAttribute("abbreviation", obj.getAbbreviation());
        ret.setAttribute("usage", obj.getUsage());
        ret.setAttribute("anchor", new AtlasObjectId(obj.getAnchor().getGlossaryGuid()));
        ret.setAttribute("additionalAttributes", obj.getAdditionalAttributes());

        if (CollectionUtils.isNotEmpty(obj.getClassifications())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Processing term classifications");
            }
            ret.setClassifications(obj.getClassifications());
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryTermDTO.toEntity() : {}", ret);
        }
        return ret;
    }

    @Override
    public AtlasEntity.AtlasEntityWithExtInfo toEntityWithExtInfo(final AtlasGlossaryTerm obj) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasGlossaryTermDTO.toEntityWithExtInfo()", obj);
        }

        Objects.requireNonNull(obj, "atlasGlossaryTerm");
        AtlasEntity entity = toEntity(obj);
        AtlasEntity.AtlasEntityWithExtInfo ret = new AtlasEntity.AtlasEntityWithExtInfo(entity);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasGlossaryTermDTO.toEntityWithExtInfo() : {}", ret);
        }
        return ret;
    }

    @Override
    public Map<String, Object> getUniqueAttributes(final AtlasGlossaryTerm obj) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("qualifiedName", obj.getQualifiedName());
        return ret;
    }
}
