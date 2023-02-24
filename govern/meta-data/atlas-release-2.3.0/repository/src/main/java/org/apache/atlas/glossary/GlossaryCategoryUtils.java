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
package org.apache.atlas.glossary;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.type.AtlasRelationshipType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GlossaryCategoryUtils extends GlossaryUtils {
    private static final Logger  LOG           = LoggerFactory.getLogger(GlossaryCategoryUtils.class);
    private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();

    protected GlossaryCategoryUtils(AtlasRelationshipStore relationshipStore, AtlasTypeRegistry typeRegistry, DataAccess dataAccess) {
        super(relationshipStore, typeRegistry, dataAccess);
    }

    public Map<String, AtlasGlossaryCategory> processCategoryRelations(AtlasGlossaryCategory storeObject, AtlasGlossaryCategory updatedCategory, RelationshipOperation op) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryCategoryUtils.processCategoryRelations({}, {}, {})", storeObject, updatedCategory, op);
        }
        Map<String, AtlasGlossaryCategory> impactedCategories = new HashMap<>();

        processCategoryAnchor(storeObject, updatedCategory, op, impactedCategories);
        processParentCategory(storeObject, updatedCategory, op, impactedCategories);
        processCategoryChildren(storeObject, updatedCategory, op, impactedCategories);
        processAssociatedTerms(storeObject, updatedCategory, op);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryCategoryUtils.processCategoryRelations(): {}", impactedCategories);
        }

        return impactedCategories;
    }

    private void processCategoryAnchor(AtlasGlossaryCategory storeObject, AtlasGlossaryCategory updatedCategory,
                                       RelationshipOperation op, Map<String, AtlasGlossaryCategory> impactedCategories) throws AtlasBaseException {
        if (Objects.isNull(updatedCategory.getAnchor()) && op != RelationshipOperation.DELETE) {
            throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
        }

        AtlasGlossaryHeader existingAnchor        = storeObject.getAnchor();
        AtlasGlossaryHeader updatedCategoryAnchor = updatedCategory.getAnchor();

        switch (op) {
            case CREATE:
                if (StringUtils.isEmpty(updatedCategoryAnchor.getGlossaryGuid())) {
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_NEW_ANCHOR_GUID);
                } else {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Creating new category anchor, category = {}, glossary = {}", storeObject.getGuid(), updatedCategory.getAnchor().getGlossaryGuid());
                    }

                    String anchorGlossaryGuid = updatedCategory.getAnchor().getGlossaryGuid();
                    createRelationship(defineCategoryAnchorRelation(anchorGlossaryGuid, storeObject.getGuid()));
                }
                break;
            case UPDATE:
                if (!Objects.equals(existingAnchor, updatedCategoryAnchor)) {
                    if (Objects.isNull(updatedCategory.getAnchor().getGlossaryGuid())) {
                        throw new AtlasBaseException(AtlasErrorCode.INVALID_NEW_ANCHOR_GUID);
                    }

                    if (DEBUG_ENABLED) {
                        LOG.debug("Updating category anchor, currAnchor = {}, newAnchor = {} and category = {}",
                                  storeObject.getAnchor().getGlossaryGuid(),
                                  updatedCategory.getAnchor().getGlossaryGuid(),
                                  storeObject.getGuid());
                    }
                    relationshipStore.deleteById(storeObject.getAnchor().getRelationGuid(), true);

                    // Derive the qualifiedName when anchor changes
                    String        anchorGlossaryGuid = updatedCategory.getAnchor().getGlossaryGuid();
                    AtlasGlossary glossary           = dataAccess.load(getGlossarySkeleton(anchorGlossaryGuid));
                    storeObject.setQualifiedName(storeObject.getName()+ "@" + glossary.getQualifiedName());

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Derived qualifiedName = {}", storeObject.getQualifiedName());
                    }

                    createRelationship(defineCategoryAnchorRelation(updatedCategory.getAnchor().getGlossaryGuid(), storeObject.getGuid()));

                    // Anchor changed, qualifiedName of children needs an update
                    updateChildCategories(storeObject, storeObject.getChildrenCategories(), impactedCategories, false);
                }
                break;
            case DELETE:
                if (Objects.nonNull(storeObject.getAnchor())) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Deleting category anchor");
                    }
                    relationshipStore.deleteById(storeObject.getAnchor().getRelationGuid(), true);
                }
                break;
        }
    }

    private void processParentCategory(AtlasGlossaryCategory storeObject, AtlasGlossaryCategory updatedCategory,
                                       RelationshipOperation op, Map<String, AtlasGlossaryCategory> impactedCategories) throws AtlasBaseException {
        AtlasRelatedCategoryHeader newParent      = updatedCategory.getParentCategory();
        AtlasRelatedCategoryHeader existingParent = storeObject.getParentCategory();

        switch (op) {
            case CREATE:
                if (Objects.nonNull(newParent)) {
                    processNewParent(storeObject, newParent, impactedCategories);
                }
                break;
            case UPDATE:
                if (Objects.equals(newParent, existingParent)) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("No change to parent");
                    }
                    break;
                }

                if (Objects.isNull(existingParent)) {
                    processNewParent(storeObject, newParent, impactedCategories);
                } else if (Objects.isNull(newParent)) {
                    processParentRemoval(storeObject, updatedCategory, existingParent, impactedCategories);
                } else {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Updating category parent, category = {}, currParent = {}, newParent = {}", storeObject.getGuid(), existingParent.getDisplayText(), newParent.getDisplayText());
                    }
                    AtlasRelationship parentRelationship = relationshipStore.getById(existingParent.getRelationGuid());
                    if (existingParent.getCategoryGuid().equals(newParent.getCategoryGuid())) {
                        updateRelationshipAttributes(parentRelationship, newParent);
                        relationshipStore.update(parentRelationship);
                    } else {
                        // Delete link to existing parent and link to new parent
                        relationshipStore.deleteById(parentRelationship.getGuid(), true);
                        createRelationship(defineCategoryHierarchyLink(newParent, storeObject.getGuid()));
                    }
                }
                break;
            case DELETE:
                if (Objects.nonNull(existingParent)) {
                    processParentRemoval(storeObject, updatedCategory, existingParent, impactedCategories);
                }
                break;
        }
    }

    private void processNewParent(AtlasGlossaryCategory storeObject, AtlasRelatedCategoryHeader newParent, Map<String, AtlasGlossaryCategory> impactedCategories) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("Creating new parent, category = {}, parent = {}", storeObject.getGuid(), newParent.getDisplayText());
        }
        createRelationship(defineCategoryHierarchyLink(newParent, storeObject.getGuid()));

        // New parent added, qualifiedName needs recomputation
        // Derive the qualifiedName of the Glossary
        AtlasGlossaryCategory parentCategory = dataAccess.load(getAtlasGlossaryCategorySkeleton(newParent.getCategoryGuid()));
        storeObject.setQualifiedName(storeObject.getName() + "." + parentCategory.getQualifiedName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Derived qualifiedName = {}", storeObject.getQualifiedName());
        }

        updateChildCategories(storeObject, storeObject.getChildrenCategories(), impactedCategories, false);
    }

    private void processParentRemoval(AtlasGlossaryCategory storeObject, AtlasGlossaryCategory updatedCategory, AtlasRelatedCategoryHeader existingParent, Map<String, AtlasGlossaryCategory> impactedCategories) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("Removing category parent, category = {}, parent = {}", storeObject.getGuid(), existingParent.getDisplayText());
        }
        relationshipStore.deleteById(existingParent.getRelationGuid(), true);

        // Parent deleted, qualifiedName needs recomputation

        // Derive the qualifiedName of the Glossary
        String        anchorGlossaryGuid = updatedCategory.getAnchor().getGlossaryGuid();
        AtlasGlossary glossary           = dataAccess.load(getGlossarySkeleton(anchorGlossaryGuid));
        storeObject.setQualifiedName(storeObject.getName() + "@" + glossary.getQualifiedName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Derived qualifiedName = {}", storeObject.getQualifiedName());
        }

        updateChildCategories(storeObject, storeObject.getChildrenCategories(), impactedCategories, false);
    }

    private void processAssociatedTerms(AtlasGlossaryCategory storeObject, AtlasGlossaryCategory updatedCategory, RelationshipOperation op) throws AtlasBaseException {
        Map<String, AtlasRelatedTermHeader> newTerms      = getTerms(updatedCategory);
        Map<String, AtlasRelatedTermHeader> existingTerms = getTerms(storeObject);

        switch (op) {
            case CREATE:
                if (DEBUG_ENABLED) {
                    LOG.debug("Creating term relation with category = {}, terms = {}", storeObject.getName(),
                              Objects.nonNull(newTerms) ? newTerms.size() : "none");
                }
                createTermCategorizationRelationships(storeObject, newTerms.values());
                break;
            case UPDATE:
                if (MapUtils.isEmpty(existingTerms)) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Creating term relation with category = {}, terms = {}", storeObject.getName(),
                                  Objects.nonNull(newTerms) ? newTerms.size() : "none");
                    }
                    createTermCategorizationRelationships(storeObject, newTerms.values());
                    break;
                }

                if (MapUtils.isEmpty(newTerms)) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Deleting term relation with category = {}, terms = {}", storeObject.getName(), existingTerms.size());
                    }
                    deleteTermCategorizationRelationships(storeObject, existingTerms.values());
                    break;
                }

                Set<AtlasRelatedTermHeader> toCreate = newTerms
                                                               .values()
                                                               .stream()
                                                               .filter(t -> !existingTerms.containsKey(t.getTermGuid()))
                                                               .collect(Collectors.toSet());
                createTermCategorizationRelationships(storeObject, toCreate);

                Set<AtlasRelatedTermHeader> toUpdate = newTerms
                                                               .values()
                                                               .stream()
                                                               .filter(t -> updatedExistingTermRelation(existingTerms, t))
                                                               .collect(Collectors.toSet());
                updateTermCategorizationRelationships(storeObject, toUpdate);

                Set<AtlasRelatedTermHeader> toDelete = existingTerms
                                                               .values()
                                                               .stream()
                                                               .filter(t -> !newTerms.containsKey(t.getTermGuid()))
                                                               .collect(Collectors.toSet());
                deleteTermCategorizationRelationships(storeObject, toDelete);
                break;
            case DELETE:
                deleteTermCategorizationRelationships(storeObject, existingTerms.values());
                break;
        }
    }

    private boolean updatedExistingTermRelation(Map<String, AtlasRelatedTermHeader> existingTerms, AtlasRelatedTermHeader term) {
        return Objects.nonNull(term.getRelationGuid()) && !existingTerms.get(term.getTermGuid()).equals(term);
    }

    private Map<String, AtlasRelatedTermHeader> getTerms(AtlasGlossaryCategory category) {
        if (Objects.nonNull(category.getTerms())) {
            Map<String, AtlasRelatedTermHeader> map = new HashMap<>();
            for (AtlasRelatedTermHeader t : category.getTerms()) {
                AtlasRelatedTermHeader header = map.get(t.getTermGuid());
                if (header == null) {
                    map.put(t.getTermGuid(), t);
                } else if (StringUtils.isEmpty(header.getRelationGuid()) && StringUtils.isNotEmpty(t.getRelationGuid())) {
                    map.put(t.getTermGuid(), t);
                }
            }
            return map;
        }
        else return Collections.emptyMap();
    }

    private void createTermCategorizationRelationships(AtlasGlossaryCategory storeObject, Collection<AtlasRelatedTermHeader> terms) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(terms)) {
            Set<AtlasRelatedTermHeader> existingTerms = storeObject.getTerms();
            for (AtlasRelatedTermHeader term : terms) {
                if (Objects.isNull(term.getTermGuid())) {
                    throw new AtlasBaseException(AtlasErrorCode.MISSING_TERM_ID_FOR_CATEGORIZATION);
                } else {
                    if (Objects.nonNull(existingTerms) && existingTerms.contains(term)) {
                        if (DEBUG_ENABLED) {
                            LOG.debug("Skipping existing term guid={}", term.getTermGuid());
                        }
                        continue;
                    }
                    if (DEBUG_ENABLED) {
                        LOG.debug("Creating relation between category = {} and term = {}", storeObject.getGuid(), term.getDisplayText());
                    }
                    createRelationship(defineCategorizedTerm(storeObject.getGuid(), term));
                }
            }
        }
    }

    private void updateTermCategorizationRelationships(AtlasGlossaryCategory storeObject, Collection<AtlasRelatedTermHeader> terms) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(terms)) {
            for (AtlasRelatedTermHeader term : terms) {
                if (DEBUG_ENABLED) {
                    LOG.debug("Updating term relation with category = {}, term = {}", storeObject.getName(), term.getDisplayText());
                }
                AtlasRelationship relationship = relationshipStore.getById(term.getRelationGuid());
                updateRelationshipAttributes(relationship, term);
                relationshipStore.update(relationship);
            }
        }
    }

    private void deleteTermCategorizationRelationships(AtlasGlossaryCategory storeObject, Collection<AtlasRelatedTermHeader> terms) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(terms)) {
            for (AtlasRelatedTermHeader term : terms) {
                if (DEBUG_ENABLED) {
                    LOG.debug("Creating term relation with category = {}, terms = {}", storeObject.getName(), term.getDisplayText());
                }
                relationshipStore.deleteById(term.getRelationGuid(), true);
            }
        }
    }

    private void processCategoryChildren(AtlasGlossaryCategory storeObject, AtlasGlossaryCategory updatedCategory, RelationshipOperation op, Map<String, AtlasGlossaryCategory> impactedCategories) throws AtlasBaseException {
        Map<String, AtlasRelatedCategoryHeader> newChildren      = getChildren(updatedCategory);
        Map<String, AtlasRelatedCategoryHeader> existingChildren = getChildren(storeObject);
        switch (op) {
            case CREATE:
                if (DEBUG_ENABLED) {
                    LOG.debug("Creating new children, category = {}, children = {}", storeObject.getName(),
                              Objects.nonNull(newChildren) ? newChildren.size() : "none");
                }
                createCategoryRelationships(storeObject, newChildren.values());

                // New children added, qualifiedName needs recomputation
                updateChildCategories(storeObject, newChildren.values(), impactedCategories, false);
                break;
            case UPDATE:
                // Create new children
                if (MapUtils.isEmpty(existingChildren)) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Creating new children, category = {}, children = {}", storeObject.getName(),
                                  Objects.nonNull(newChildren) ? newChildren.size() : "none");
                    }
                    createCategoryRelationships(storeObject, newChildren.values());

                    // New children added, qualifiedName needs recomputation
                    updateChildCategories(storeObject, newChildren.values(), impactedCategories, false);
                    break;
                }
                // Delete current children
                if (MapUtils.isEmpty(newChildren)) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Deleting children, category = {}, children = {}", storeObject.getName(), existingChildren.size());
                    }
                    deleteCategoryRelationships(storeObject, existingChildren.values());

                    // Re-compute the qualifiedName for all children
                    updateChildCategories(storeObject, existingChildren.values(), impactedCategories, true);
                    break;
                }

                Set<AtlasRelatedCategoryHeader> toCreate = newChildren
                                                                   .values()
                                                                   .stream()
                                                                   .filter(c -> !existingChildren.containsKey(c.getCategoryGuid()))
                                                                   .collect(Collectors.toSet());
                createCategoryRelationships(storeObject, toCreate);
                // New children added, qualifiedName needs recomputation
                updateChildCategories(storeObject, toCreate, impactedCategories, false);

                Set<AtlasRelatedCategoryHeader> toUpdate = newChildren
                                                                   .values()
                                                                   .stream()
                                                                   .filter(c -> updatedExistingCategoryRelation(existingChildren, c))
                                                                   .collect(Collectors.toSet());
                updateCategoryRelationships(storeObject, toUpdate);

                Set<AtlasRelatedCategoryHeader> toDelete = existingChildren
                                                                   .values()
                                                                   .stream()
                                                                   .filter(c -> !newChildren.containsKey(c.getCategoryGuid()))
                                                                   .collect(Collectors.toSet());
                deleteCategoryRelationships(storeObject, toDelete);
                break;
            case DELETE:
                deleteCategoryRelationships(storeObject, existingChildren.values());
                break;
        }
    }

    private boolean updatedExistingCategoryRelation(Map<String, AtlasRelatedCategoryHeader> existingChildren, AtlasRelatedCategoryHeader header) {
        return Objects.nonNull(header.getRelationGuid()) && !header.equals(existingChildren.get(header.getCategoryGuid()));
    }

    private Map<String, AtlasRelatedCategoryHeader> getChildren(AtlasGlossaryCategory category) {
        if (Objects.nonNull(category.getChildrenCategories())) {
            Map<String, AtlasRelatedCategoryHeader> map = new HashMap<>();
            for (AtlasRelatedCategoryHeader c : category.getChildrenCategories()) {
                AtlasRelatedCategoryHeader header = map.get(c.getCategoryGuid());
                if (header == null || (StringUtils.isEmpty(header.getRelationGuid()) && StringUtils.isNotEmpty(c.getRelationGuid()))) {
                    map.put(c.getCategoryGuid(), c);
                }
            }
            return map;
        }
        else return Collections.emptyMap();
    }

    private void createCategoryRelationships(AtlasGlossaryCategory storeObject, Collection<AtlasRelatedCategoryHeader> newChildren) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(newChildren)) {
            Set<AtlasRelatedCategoryHeader> existingChildren = storeObject.getChildrenCategories();
            for (AtlasRelatedCategoryHeader child : newChildren) {
                if (Objects.nonNull(existingChildren) && existingChildren.contains(child)) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Skipping existing child relation for category guid = {}", child.getCategoryGuid());
                    }
                    continue;
                }

                if (DEBUG_ENABLED) {
                    LOG.debug("Loading the child category to perform glossary check");
                }

                AtlasGlossaryCategory childCategory = new AtlasGlossaryCategory();
                childCategory.setGuid(child.getCategoryGuid());
                childCategory = dataAccess.load(childCategory);

                if (StringUtils.equals(storeObject.getAnchor().getGlossaryGuid(), childCategory.getAnchor().getGlossaryGuid())) {
                    if (DEBUG_ENABLED) {
                        LOG.debug("Creating new child, category = {}, child = {}", storeObject.getName(), child.getDisplayText());
                    }
                    createRelationship(defineCategoryHierarchyLink(storeObject.getGuid(), child));
                } else {
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_CHILD_CATEGORY_DIFFERENT_GLOSSARY, child.getCategoryGuid());
                }

            }
        }
    }

    private void updateCategoryRelationships(AtlasGlossaryCategory storeObject, Collection<AtlasRelatedCategoryHeader> toUpdate) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            for (AtlasRelatedCategoryHeader categoryHeader : toUpdate) {
                if (DEBUG_ENABLED) {
                    LOG.debug("Updating child, category = {}, child = {}", storeObject.getName(), categoryHeader.getDisplayText());
                }
                AtlasRelationship childRelationship = relationshipStore.getById(categoryHeader.getRelationGuid());
                updateRelationshipAttributes(childRelationship, categoryHeader);
                relationshipStore.update(childRelationship);
            }
        }
    }

    private void deleteCategoryRelationships(AtlasGlossaryCategory storeObject, Collection<AtlasRelatedCategoryHeader> existingChildren) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(existingChildren)) {
            for (AtlasRelatedCategoryHeader child : existingChildren) {
                if (DEBUG_ENABLED) {
                    LOG.debug("Deleting child, category = {}, child = {}", storeObject.getName(), child.getDisplayText());
                }
                relationshipStore.deleteById(child.getRelationGuid(), true);
            }
        }
    }

    private AtlasRelationship defineCategoryAnchorRelation(String glossaryGuid, String categoryGuid) {
        AtlasRelationshipType relationshipType = typeRegistry.getRelationshipTypeByName(CATEGORY_ANCHOR);
        AtlasStruct           defaultAttrs     = relationshipType.createDefaultValue();

        return new AtlasRelationship(CATEGORY_ANCHOR, new AtlasObjectId(glossaryGuid), new AtlasObjectId(categoryGuid), defaultAttrs.getAttributes());
    }

    private AtlasRelationship defineCategoryHierarchyLink(String parentCategoryGuid, AtlasRelatedCategoryHeader childCategory) {
        AtlasRelationshipType relationshipType = typeRegistry.getRelationshipTypeByName(CATEGORY_HIERARCHY);
        AtlasStruct           defaultAttrs     = relationshipType.createDefaultValue();

        AtlasRelationship relationship = new AtlasRelationship(CATEGORY_HIERARCHY, new AtlasObjectId(parentCategoryGuid), new AtlasObjectId(childCategory.getCategoryGuid()), defaultAttrs.getAttributes());
        updateRelationshipAttributes(relationship, childCategory);
        return relationship;
    }

    private AtlasRelationship defineCategoryHierarchyLink(AtlasRelatedCategoryHeader parentCategory, String childGuid) {
        AtlasRelationshipType relationshipType = typeRegistry.getRelationshipTypeByName(CATEGORY_HIERARCHY);
        AtlasStruct           defaultAttrs     = relationshipType.createDefaultValue();

        AtlasRelationship relationship = new AtlasRelationship(CATEGORY_HIERARCHY, new AtlasObjectId(parentCategory.getCategoryGuid()), new AtlasObjectId(childGuid), defaultAttrs.getAttributes());
        updateRelationshipAttributes(relationship, parentCategory);
        return relationship;
    }

    private AtlasRelationship defineCategorizedTerm(String categoryGuid, AtlasRelatedTermHeader relatedTermId) {
        AtlasRelationshipType relationshipType = typeRegistry.getRelationshipTypeByName(TERM_CATEGORIZATION);
        AtlasStruct           defaultAttrs     = relationshipType.createDefaultValue();

        AtlasRelationship relationship = new AtlasRelationship(TERM_CATEGORIZATION, new AtlasObjectId(categoryGuid), new AtlasObjectId(relatedTermId.getTermGuid()), defaultAttrs.getAttributes());
        updateRelationshipAttributes(relationship, relatedTermId);
        return relationship;
    }

    private void updateRelationshipAttributes(AtlasRelationship relationship, AtlasRelatedCategoryHeader relatedCategoryHeader) {
        if (Objects.nonNull(relationship)) {
            relationship.setAttribute("description", relatedCategoryHeader.getDescription());
        }
    }

    private void updateChildCategories(AtlasGlossaryCategory parentCategory, Collection<AtlasRelatedCategoryHeader> childCategories, Map<String, AtlasGlossaryCategory> impactedCategories, boolean isParentRemoved) throws AtlasBaseException {
        if (CollectionUtils.isEmpty(childCategories)) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Recomputing qualifiedName for {} children of category(guid={}, qualifiedName={})", childCategories.size(), parentCategory.getGuid(), parentCategory.getQualifiedName());
        }

        String parentAnchorGuid = parentCategory.getAnchor().getGlossaryGuid();

        for (AtlasRelatedCategoryHeader childCategoryHeader : childCategories) {
            AtlasGlossaryCategory child           = dataAccess.load(getAtlasGlossaryCategorySkeleton(childCategoryHeader.getCategoryGuid()));
            String                qualifiedName   = child.getName() + ".";
            String                childAnchorGuid = child.getAnchor().getGlossaryGuid();
            if (isParentRemoved) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Parent removed, deriving qualifiedName using Glossary");
                }
                AtlasGlossary glossary = dataAccess.load(getGlossarySkeleton(childAnchorGuid));
                qualifiedName += glossary.getQualifiedName();
                child.setParentCategory(null);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Using parent to derive qualifiedName");
                }
                qualifiedName += parentCategory.getQualifiedName();
            }
            child.setQualifiedName(qualifiedName);

            if (!StringUtils.equals(childAnchorGuid, parentAnchorGuid)){
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Child anchor guid({}) doesn't match parent anchor guid({}). Updating child anchor", childAnchorGuid, parentAnchorGuid);
                }
                // Remove old glossary relation
                relationshipStore.deleteById(child.getAnchor().getRelationGuid(), true);

                // Link to new glossary
                createRelationship(defineCategoryAnchorRelation(parentAnchorGuid, child.getGuid()));

                // Update the child's anchor GUID
                child.getAnchor().setGlossaryGuid(parentAnchorGuid);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Derived qualifiedName = {}", qualifiedName);
            }

            // Collect all affected/impacted categories in this accumulator. All impacted categories will be saved as a part of post processing
            impactedCategories.put(child.getGuid(), child);
            updateChildCategories(child, child.getChildrenCategories(), impactedCategories, false);
        }
    }

}
