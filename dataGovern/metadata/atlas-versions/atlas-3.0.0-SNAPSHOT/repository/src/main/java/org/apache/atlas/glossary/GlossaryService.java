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
import org.apache.atlas.SortOrder;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.bulkimport.BulkImportResponse.ImportInfo;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.glossary.relations.AtlasTermCategorizationHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.ogm.DataAccess;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityChangeNotifier;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.FileUtils;
import org.apache.atlas.utils.AtlasJson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.atlas.bulkimport.BulkImportResponse.ImportStatus.FAILED;
import static org.apache.atlas.bulkimport.BulkImportResponse.ImportStatus.SUCCESS;
import static org.apache.atlas.glossary.GlossaryUtils.getAtlasGlossaryCategorySkeleton;
import static org.apache.atlas.glossary.GlossaryUtils.getAtlasGlossaryTermSkeleton;
import static org.apache.atlas.glossary.GlossaryUtils.getGlossarySkeleton;

@Service
public class GlossaryService {
    private static final Logger  LOG                 = LoggerFactory.getLogger(GlossaryService.class);
    private static final boolean DEBUG_ENABLED       = LOG.isDebugEnabled();
    private static final String  QUALIFIED_NAME_ATTR = "qualifiedName";

    private final DataAccess                dataAccess;
    private final GlossaryTermUtils         glossaryTermUtils;
    private final GlossaryCategoryUtils     glossaryCategoryUtils;
    private final AtlasTypeRegistry         atlasTypeRegistry;
    private final AtlasEntityChangeNotifier entityChangeNotifier;

    private static final char[] invalidNameChars = { '@', '.' };

    @Inject
    public GlossaryService(DataAccess dataAccess, final AtlasRelationshipStore relationshipStore,
                           final AtlasTypeRegistry typeRegistry, AtlasEntityChangeNotifier entityChangeNotifier) {
        this.dataAccess           = dataAccess;
        atlasTypeRegistry         = typeRegistry;
        glossaryTermUtils         = new GlossaryTermUtils(relationshipStore, typeRegistry, dataAccess);
        glossaryCategoryUtils     = new GlossaryCategoryUtils(relationshipStore, typeRegistry, dataAccess);
        this.entityChangeNotifier = entityChangeNotifier;
    }

    /**
     * List all glossaries
     *
     * @param limit     page size - no paging by default
     * @param offset    offset for pagination
     * @param sortOrder ASC (default) or DESC
     * @return List of all glossaries
     * @throws AtlasBaseException
     */
    @GraphTransaction
    public List<AtlasGlossary> getGlossaries(int limit, int offset, SortOrder sortOrder) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossaries({}, {}, {})", limit, offset, sortOrder);
        }

        List<String>     glossaryGuids    = AtlasGraphUtilsV2.findEntityGUIDsByType(GlossaryUtils.ATLAS_GLOSSARY_TYPENAME, sortOrder);
        PaginationHelper paginationHelper = new PaginationHelper<>(glossaryGuids, offset, limit);

        List<AtlasGlossary> ret;
        List<String>        guidsToLoad = paginationHelper.getPaginatedList();
        if (CollectionUtils.isNotEmpty(guidsToLoad)) {
            ret = guidsToLoad.stream().map(GlossaryUtils::getGlossarySkeleton).collect(Collectors.toList());
            Iterable<AtlasGlossary> glossaries = dataAccess.load(ret);
            ret.clear();

            // Set the displayText for all relations
            for (AtlasGlossary glossary : glossaries) {
                setInfoForRelations(glossary);
                ret.add(glossary);
            }
        } else {
            ret = Collections.emptyList();
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossaries() : {}", ret);
        }
        return ret;
    }

    /**
     * Create a glossary
     *
     * @param atlasGlossary Glossary specification to be created
     * @return Glossary definition
     * @throws AtlasBaseException
     */
    @GraphTransaction
    public AtlasGlossary createGlossary(AtlasGlossary atlasGlossary) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.createGlossary({})", atlasGlossary);
        }

        if (Objects.isNull(atlasGlossary)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Glossary definition missing");
        }

        if (StringUtils.isEmpty(atlasGlossary.getQualifiedName())) {
            if (StringUtils.isEmpty(atlasGlossary.getName())) {
                throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_QUALIFIED_NAME_CANT_BE_DERIVED);
            }
            if (isNameInvalid(atlasGlossary.getName())){
                throw new AtlasBaseException(AtlasErrorCode.INVALID_DISPLAY_NAME);
            } else {
                atlasGlossary.setQualifiedName(atlasGlossary.getName());
            }
        }

        if (glossaryExists(atlasGlossary)) {
            throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_ALREADY_EXISTS, atlasGlossary.getName());
        }

        AtlasGlossary storeObject = dataAccess.save(atlasGlossary);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.createGlossary() : {}", storeObject);
        }

        return storeObject;
    }

    /**
     * Get specific glossary
     *
     * @param glossaryGuid unique identifier
     * @return Glossary corresponding to specified glossaryGuid
     * @throws AtlasBaseException
     */
    @GraphTransaction
    public AtlasGlossary getGlossary(String glossaryGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossary({})", glossaryGuid);
        }

        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        AtlasGlossary atlasGlossary = getGlossarySkeleton(glossaryGuid);
        AtlasGlossary ret           = dataAccess.load(atlasGlossary);

        setInfoForRelations(ret);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossary() : {}", ret);
        }
        return ret;
    }

    /**
     * Get specific glossary
     *
     * @param glossaryGuid unique identifier
     * @return Glossary corresponding to specified glossaryGuid
     * @throws AtlasBaseException
     */
    @GraphTransaction
    public AtlasGlossary.AtlasGlossaryExtInfo getDetailedGlossary(String glossaryGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossary({})", glossaryGuid);
        }

        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        AtlasGlossary atlasGlossary = getGlossarySkeleton(glossaryGuid);
        AtlasGlossary glossary      = dataAccess.load(atlasGlossary);

        AtlasGlossary.AtlasGlossaryExtInfo ret = new AtlasGlossary.AtlasGlossaryExtInfo(glossary);

        // Load all linked terms
        if (CollectionUtils.isNotEmpty(ret.getTerms())) {
            List<AtlasGlossaryTerm> termsToLoad = ret.getTerms()
                                                     .stream()
                                                     .map(id -> getAtlasGlossaryTermSkeleton(id.getTermGuid()))
                                                     .collect(Collectors.toList());
            Iterable<AtlasGlossaryTerm> glossaryTerms = dataAccess.load(termsToLoad);
            glossaryTerms.forEach(ret::addTermInfo);
        }

        // Load all linked categories
        if (CollectionUtils.isNotEmpty(ret.getCategories())) {
            List<AtlasGlossaryCategory> categoriesToLoad = ret.getCategories()
                                                              .stream()
                                                              .map(id -> getAtlasGlossaryCategorySkeleton(id.getCategoryGuid()))
                                                              .collect(Collectors.toList());
            Iterable<AtlasGlossaryCategory> glossaryCategories = dataAccess.load(categoriesToLoad);
            glossaryCategories.forEach(ret::addCategoryInfo);
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossary() : {}", ret);
        }
        return ret;
    }

    @GraphTransaction
    public AtlasGlossary updateGlossary(AtlasGlossary atlasGlossary) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.updateGlossary({})", atlasGlossary);
        }

        if (Objects.isNull(atlasGlossary)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Glossary is null/empty");
        }

        if (StringUtils.isEmpty(atlasGlossary.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "DisplayName can't be null/empty");
        }

        if (isNameInvalid(atlasGlossary.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_DISPLAY_NAME);
        }

        AtlasGlossary storeObject = dataAccess.load(atlasGlossary);

        if (!storeObject.equals(atlasGlossary)) {
            atlasGlossary.setGuid(storeObject.getGuid());
            atlasGlossary.setQualifiedName(storeObject.getQualifiedName());

            storeObject = dataAccess.save(atlasGlossary);
            setInfoForRelations(storeObject);
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.updateGlossary() : {}", storeObject);
        }
        return storeObject;
    }

    @GraphTransaction
    public void deleteGlossary(String glossaryGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.deleteGlossary({})", glossaryGuid);
        }
        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        AtlasGlossary storeObject = dataAccess.load(getGlossarySkeleton(glossaryGuid));

        Set<AtlasRelatedTermHeader> terms = storeObject.getTerms();
        deleteTerms(storeObject, terms);

        Set<AtlasRelatedCategoryHeader> categories = storeObject.getCategories();
        deleteCategories(storeObject, categories);

        // Once all relations are deleted, then delete the Glossary
        dataAccess.delete(glossaryGuid);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.deleteGlossary()");
        }
    }

    /*
     * GlossaryTerms related operations
     * */
    @GraphTransaction
    public AtlasGlossaryTerm getTerm(String termGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getTerm({})", termGuid);
        }
        if (Objects.isNull(termGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "termGuid is null/empty");
        }


        AtlasGlossaryTerm atlasGlossary = getAtlasGlossaryTermSkeleton(termGuid);
        AtlasGlossaryTerm ret           = dataAccess.load(atlasGlossary);

        setInfoForRelations(ret);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getTerm() : {}", ret);
        }
        return ret;
    }

    @GraphTransaction
    public AtlasGlossaryTerm createTerm(AtlasGlossaryTerm glossaryTerm) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.create({})", glossaryTerm);
        }
        if (Objects.isNull(glossaryTerm)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "GlossaryTerm definition missing");
        }
        if (Objects.isNull(glossaryTerm.getAnchor())) {
            throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
        }
        if (StringUtils.isEmpty(glossaryTerm.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_TERM_QUALIFIED_NAME_CANT_BE_DERIVED);
        }

        if (isNameInvalid(glossaryTerm.getName())){
            throw new AtlasBaseException(AtlasErrorCode.INVALID_DISPLAY_NAME);
        } else {
            // Derive the qualifiedName
            String        anchorGlossaryGuid = glossaryTerm.getAnchor().getGlossaryGuid();
            AtlasGlossary glossary           = dataAccess.load(getGlossarySkeleton(anchorGlossaryGuid));
            glossaryTerm.setQualifiedName(glossaryTerm.getName() + "@" + glossary.getQualifiedName());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Derived qualifiedName = {}", glossaryTerm.getQualifiedName());
            }
        }

        // This might fail for the case when the term's qualifiedName has been updated and the duplicate request comes in with old name
        if (termExists(glossaryTerm)) {
            throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_TERM_ALREADY_EXISTS, glossaryTerm.getQualifiedName());
        }

        AtlasGlossaryTerm storeObject = dataAccess.save(glossaryTerm);
        glossaryTermUtils.processTermRelations(storeObject, glossaryTerm, GlossaryUtils.RelationshipOperation.CREATE);

        // Re-load term after handling relations
        storeObject = dataAccess.load(glossaryTerm);
        setInfoForRelations(storeObject);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.create() : {}", storeObject);
        }
        return storeObject;
    }

    @GraphTransaction
    public List<AtlasGlossaryTerm> createTerms(List<AtlasGlossaryTerm> glossaryTerm) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.create({})", glossaryTerm);
        }

        if (Objects.isNull(glossaryTerm)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryTerm(s) is null/empty");
        }


        List<AtlasGlossaryTerm> ret = new ArrayList<>();
        for (AtlasGlossaryTerm atlasGlossaryTerm : glossaryTerm) {
            ret.add(createTerm(atlasGlossaryTerm));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== GlossaryService.createTerms() : {}", ret);
        }

        return ret;
    }

    public AtlasGlossaryTerm updateTerm(AtlasGlossaryTerm atlasGlossaryTerm) throws AtlasBaseException {
        return updateTerm(atlasGlossaryTerm, true);
    }

    @GraphTransaction
    public AtlasGlossaryTerm updateTerm(AtlasGlossaryTerm atlasGlossaryTerm, boolean ignoreUpdateIfTermExists) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.updateTerm({})", atlasGlossaryTerm);
        }

        if (Objects.isNull(atlasGlossaryTerm)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "atlasGlossaryTerm is null/empty");
        }

        if (StringUtils.isEmpty(atlasGlossaryTerm.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "DisplayName can't be null/empty");
        }

        if (isNameInvalid(atlasGlossaryTerm.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_DISPLAY_NAME);
        }

        if (ignoreUpdateIfTermExists) {
            String qualifiedName = getDuplicateGlossaryRelatedTerm(atlasGlossaryTerm);

            if (StringUtils.isNotEmpty(qualifiedName)) {
                throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_TERM_ALREADY_EXISTS, qualifiedName);
            }
        }

        AtlasGlossaryTerm storeObject = dataAccess.load(atlasGlossaryTerm);
        if (!storeObject.equals(atlasGlossaryTerm)) {
            atlasGlossaryTerm.setGuid(storeObject.getGuid());
            atlasGlossaryTerm.setQualifiedName(storeObject.getQualifiedName());

            storeObject = dataAccess.save(atlasGlossaryTerm);

            glossaryTermUtils.processTermRelations(storeObject, atlasGlossaryTerm, GlossaryUtils.RelationshipOperation.UPDATE);

            // If qualifiedName changes due to anchor change, we need to persist the term again with updated qualifiedName
            if (StringUtils.equals(storeObject.getQualifiedName(), atlasGlossaryTerm.getQualifiedName())) {
                storeObject = dataAccess.load(atlasGlossaryTerm);
            } else {
                atlasGlossaryTerm.setQualifiedName(storeObject.getQualifiedName());

                if (termExists(atlasGlossaryTerm)) {
                    throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_TERM_ALREADY_EXISTS, atlasGlossaryTerm.getQualifiedName());
                }

                storeObject = dataAccess.save(atlasGlossaryTerm);
            }
        }

        setInfoForRelations(storeObject);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.updateTerm() : {}", storeObject);
        }

        return storeObject;
    }

    @GraphTransaction
    public void deleteTerm(String termGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.deleteTerm({})", termGuid);
        }
        if (Objects.isNull(termGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "termGuid is null/empty");
        }

        AtlasGlossaryTerm storeObject = dataAccess.load(getAtlasGlossaryTermSkeleton(termGuid));

        // Term can't be deleted if it is assigned to any entity
        if (CollectionUtils.isNotEmpty(storeObject.getAssignedEntities())) {
            throw new AtlasBaseException(AtlasErrorCode.TERM_HAS_ENTITY_ASSOCIATION, storeObject.getGuid(), String.valueOf(storeObject.getAssignedEntities().size()));
        }

        // Remove term from Glossary
        glossaryTermUtils.processTermRelations(storeObject, storeObject, GlossaryUtils.RelationshipOperation.DELETE);


        // Now delete the term
        dataAccess.delete(termGuid);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.deleteTerm()");
        }
    }

    @GraphTransaction
    public void assignTermToEntities(String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.assignTermToEntities({}, {})", termGuid, relatedObjectIds);
        }

        AtlasGlossaryTerm glossaryTerm = dataAccess.load(getAtlasGlossaryTermSkeleton(termGuid));

        glossaryTermUtils.processTermAssignments(glossaryTerm, relatedObjectIds);

        entityChangeNotifier.onTermAddedToEntities(glossaryTerm, relatedObjectIds);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.assignTermToEntities()");
        }

    }

    @GraphTransaction
    public void removeTermFromEntities(String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.removeTermFromEntities({}, {})", termGuid, relatedObjectIds);
        }

        AtlasGlossaryTerm glossaryTerm = dataAccess.load(getAtlasGlossaryTermSkeleton(termGuid));

        glossaryTermUtils.processTermDissociation(glossaryTerm, relatedObjectIds);

        entityChangeNotifier.onTermDeletedFromEntities(glossaryTerm, relatedObjectIds);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.removeTermFromEntities()");
        }
    }

    /*
     * GlossaryCategory related operations
     * */
    @GraphTransaction
    public AtlasGlossaryCategory getCategory(String categoryGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getCategory({})", categoryGuid);
        }
        if (Objects.isNull(categoryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "categoryGuid is null/empty");
        }

        AtlasGlossaryCategory atlasGlossary = getAtlasGlossaryCategorySkeleton(categoryGuid);
        AtlasGlossaryCategory ret           = dataAccess.load(atlasGlossary);

        setInfoForRelations(ret);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getCategory() : {}", ret);
        }
        return ret;
    }

    @GraphTransaction
    public AtlasGlossaryCategory createCategory(AtlasGlossaryCategory glossaryCategory) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.createCategory({})", glossaryCategory);
        }

        if (Objects.isNull(glossaryCategory)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "GlossaryCategory definition missing");
        }
        if (Objects.isNull(glossaryCategory.getAnchor())) {
            throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
        }
        if (StringUtils.isEmpty(glossaryCategory.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_CATEGORY_QUALIFIED_NAME_CANT_BE_DERIVED);
        }
        if (isNameInvalid(glossaryCategory.getName())){
            throw new AtlasBaseException(AtlasErrorCode.INVALID_DISPLAY_NAME);
        } else {
            // Derive the qualifiedName
            String anchorGlossaryGuid = glossaryCategory.getAnchor().getGlossaryGuid();
            AtlasGlossary glossary = dataAccess.load(getGlossarySkeleton(anchorGlossaryGuid));
            glossaryCategory.setQualifiedName(glossaryCategory.getName()+ "@" + glossary.getQualifiedName());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Derived qualifiedName = {}", glossaryCategory.getQualifiedName());
            }


        }

        // This might fail for the case when the category's qualifiedName has been updated during a hierarchy change
        // and the duplicate request comes in with old name
        if (categoryExists(glossaryCategory)) {
            throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_CATEGORY_ALREADY_EXISTS, glossaryCategory.getQualifiedName());
        }

        AtlasGlossaryCategory storeObject = dataAccess.save(glossaryCategory);

        // Attempt relation creation and gather all impacted categories
        Map<String, AtlasGlossaryCategory> impactedCategories = glossaryCategoryUtils.processCategoryRelations(storeObject, glossaryCategory, GlossaryUtils.RelationshipOperation.CREATE);

        // Since the current category is also affected, we need to update qualifiedName and save again
        if (StringUtils.equals(glossaryCategory.getQualifiedName(), storeObject.getQualifiedName())) {
            storeObject = dataAccess.load(glossaryCategory);
        } else {
            glossaryCategory.setQualifiedName(storeObject.getQualifiedName());

            if (categoryExists(glossaryCategory)) {
                throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_CATEGORY_ALREADY_EXISTS, glossaryCategory.getQualifiedName());
            }

            storeObject = dataAccess.save(glossaryCategory);
        }

        // Re save the categories in case any qualifiedName change has occurred
        dataAccess.save(impactedCategories.values());

        setInfoForRelations(storeObject);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.createCategory() : {}", storeObject);
        }

        return storeObject;
    }

    @GraphTransaction
    public List<AtlasGlossaryCategory> createCategories(List<AtlasGlossaryCategory> glossaryCategory) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.createCategories({})", glossaryCategory);
        }
        if (Objects.isNull(glossaryCategory)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryCategory is null/empty");
        }

        List<AtlasGlossaryCategory> ret = new ArrayList<>();
        for (AtlasGlossaryCategory category : glossaryCategory) {
            ret.add(createCategory(category));
        }
        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.createCategories() : {}", ret);
        }
        return ret;
    }

    @GraphTransaction
    public AtlasGlossaryCategory updateCategory(AtlasGlossaryCategory glossaryCategory) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.updateCategory({})", glossaryCategory);
        }

        if (Objects.isNull(glossaryCategory)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "GlossaryCategory is null/empty");
        }

        if (StringUtils.isEmpty(glossaryCategory.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "DisplayName can't be null/empty");
        }

        if (isNameInvalid(glossaryCategory.getName())) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_DISPLAY_NAME);
        }

        AtlasGlossaryCategory storeObject = dataAccess.load(glossaryCategory);

        if (!storeObject.equals(glossaryCategory)) {
            try {
                glossaryCategory.setGuid(storeObject.getGuid());
                glossaryCategory.setQualifiedName(storeObject.getQualifiedName());

                storeObject = dataAccess.save(glossaryCategory);
            } catch (AtlasBaseException e) {
                LOG.debug("No immediate attribute update. Exception: {}", e.getMessage());
            }

            Map<String, AtlasGlossaryCategory> impactedCategories = glossaryCategoryUtils.processCategoryRelations(storeObject, glossaryCategory, GlossaryUtils.RelationshipOperation.UPDATE);

            // Since the current category is also affected, we need to update qualifiedName and save again
            if (StringUtils.equals(glossaryCategory.getQualifiedName(), storeObject.getQualifiedName())) {
                storeObject = dataAccess.load(glossaryCategory);
            } else {
                glossaryCategory.setQualifiedName(storeObject.getQualifiedName());

                if (categoryExists(glossaryCategory)) {
                    throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_CATEGORY_ALREADY_EXISTS, glossaryCategory.getQualifiedName());
                }

                storeObject = dataAccess.save(glossaryCategory);
            }

            dataAccess.save(impactedCategories.values());
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.updateCategory() : {}", storeObject);
        }

        setInfoForRelations(storeObject);

        return storeObject;
    }

    @GraphTransaction
    public void deleteCategory(String categoryGuid) throws AtlasBaseException {
        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.deleteCategory({})", categoryGuid);
        }
        if (Objects.isNull(categoryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Category guid is null/empty");
        }

        AtlasGlossaryCategory storeObject = dataAccess.load(getAtlasGlossaryCategorySkeleton(categoryGuid));

        // Delete all relations
        glossaryCategoryUtils.processCategoryRelations(storeObject, storeObject, GlossaryUtils.RelationshipOperation.DELETE);

        // Now delete the category
        dataAccess.delete(categoryGuid);

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.deleteCategory()");
        }
    }

    @GraphTransaction
    public List<AtlasRelatedTermHeader> getGlossaryTermsHeaders(String glossaryGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossaryTermsHeaders({}, {}, {}, {})", glossaryGuid, offset, limit, sortOrder);
        }

        AtlasGlossary glossary = getGlossary(glossaryGuid);

        List<AtlasRelatedTermHeader> ret;
        if (CollectionUtils.isNotEmpty(glossary.getTerms())) {
            List<AtlasRelatedTermHeader> terms = new ArrayList<>(glossary.getTerms());
            if (sortOrder != null) {
                terms.sort((o1, o2) -> sortOrder == SortOrder.ASCENDING ?
                                               o1.getDisplayText().compareTo(o2.getDisplayText()) :
                                               o2.getDisplayText().compareTo(o1.getDisplayText()));
            }
            ret = new PaginationHelper<>(terms, offset, limit).getPaginatedList();
        } else {
            ret = Collections.emptyList();
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossaryTermsHeaders() : {}", ret);
        }

        return ret;
    }

    @GraphTransaction
    public List<AtlasGlossaryTerm> getGlossaryTerms(String glossaryGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossaryTerms({}, {}, {}, {})", glossaryGuid, offset, limit, sortOrder);
        }

        List<AtlasGlossaryTerm> ret = new ArrayList<>();

        List<AtlasRelatedTermHeader> termHeaders = getGlossaryTermsHeaders(glossaryGuid, offset, limit, sortOrder);
        for (AtlasRelatedTermHeader header : termHeaders) {
            ret.add(dataAccess.load(getAtlasGlossaryTermSkeleton(header.getTermGuid())));
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossaryTerms() : {}", ret);
        }

        return ret;
    }

    @GraphTransaction
    public List<AtlasRelatedCategoryHeader> getGlossaryCategoriesHeaders(String glossaryGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossaryCategoriesHeaders({}, {}, {}, {})", glossaryGuid, offset, limit, sortOrder);
        }

        List<AtlasRelatedCategoryHeader> ret;

        AtlasGlossary glossary = getGlossary(glossaryGuid);

        if (CollectionUtils.isNotEmpty(glossary.getCategories())) {
            List<AtlasRelatedCategoryHeader> categories = new ArrayList<>(glossary.getCategories());
            if (sortOrder != null) {
                categories.sort((o1, o2) -> sortOrder == SortOrder.ASCENDING ?
                                                    o1.getDisplayText().compareTo(o2.getDisplayText()) :
                                                    o2.getDisplayText().compareTo(o1.getDisplayText()));
            }
            ret = new PaginationHelper<>(categories, offset, limit).getPaginatedList();
        } else {
            ret = Collections.emptyList();
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossaryCategoriesHeaders() : {}", ret);
        }

        return ret;
    }

    @GraphTransaction
    public List<AtlasGlossaryCategory> getGlossaryCategories(String glossaryGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(glossaryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "glossaryGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getGlossaryCategories({}, {}, {}, {})", glossaryGuid, offset, limit, sortOrder);
        }

        List<AtlasGlossaryCategory> ret = new ArrayList<>();

        List<AtlasRelatedCategoryHeader> categoryHeaders = getGlossaryCategoriesHeaders(glossaryGuid, offset, limit, sortOrder);
        for (AtlasRelatedCategoryHeader header : categoryHeaders) {
            ret.add(dataAccess.load(getAtlasGlossaryCategorySkeleton(header.getCategoryGuid())));
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getGlossaryCategories() : {}", ret);
        }

        return ret;
    }

    @GraphTransaction
    public List<AtlasRelatedTermHeader> getCategoryTerms(String categoryGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(categoryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "categoryGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getCategoryTerms({}, {}, {}, {})", categoryGuid, offset, limit, sortOrder);
        }

        List<AtlasRelatedTermHeader> ret;

        AtlasGlossaryCategory glossaryCategory = getCategory(categoryGuid);

        if (CollectionUtils.isNotEmpty(glossaryCategory.getTerms())) {
            List<AtlasRelatedTermHeader> terms = new ArrayList<>(glossaryCategory.getTerms());
            if (sortOrder != null) {
                terms.sort((o1, o2) -> sortOrder == SortOrder.ASCENDING ?
                                               o1.getDisplayText().compareTo(o2.getDisplayText()) :
                                               o2.getDisplayText().compareTo(o1.getDisplayText()));
            }
            ret = new PaginationHelper<>(terms, offset, limit).getPaginatedList();
        } else {
            ret = Collections.emptyList();
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getCategoryTerms() : {}", ret);
        }
        return ret;
    }

    @GraphTransaction
    public Map<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> getRelatedTerms(String termGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(termGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "termGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getRelatedTerms({}, {}, {}, {})", termGuid, offset, limit, sortOrder);
        }

        AtlasGlossaryTerm                                            glossaryTerm = getTerm(termGuid);
        Map<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> ret;
        if (glossaryTerm.hasTerms()) {
            ret = glossaryTerm.getRelatedTerms();
        } else {
            ret = Collections.emptyMap();
        }

        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getRelatedTerms() : {}", ret);
        }

        return ret;
    }

    @GraphTransaction
    public Map<String, List<AtlasRelatedCategoryHeader>> getRelatedCategories(String categoryGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(categoryGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "categoryGuid is null/empty");
        }

        if (DEBUG_ENABLED) {
            LOG.debug("==> GlossaryService.getRelatedCategories({}, {}, {}, {})", categoryGuid, offset, limit, sortOrder);
        }

        AtlasGlossaryCategory glossaryCategory = getCategory(categoryGuid);

        Map<String, List<AtlasRelatedCategoryHeader>> ret = new HashMap<>();
        if (glossaryCategory.getParentCategory() != null) {
            ret.put("parent", new ArrayList<AtlasRelatedCategoryHeader>() {{
                add(glossaryCategory.getParentCategory());
            }});
        }
        if (CollectionUtils.isNotEmpty(glossaryCategory.getChildrenCategories())) {
            ret.put("children", new ArrayList<>(glossaryCategory.getChildrenCategories()));
        }


        if (DEBUG_ENABLED) {
            LOG.debug("<== GlossaryService.getRelatedCategories() : {}", ret);
        }

        return ret;
    }

    @GraphTransaction
    public List<AtlasRelatedObjectId> getAssignedEntities(final String termGuid, int offset, int limit, SortOrder sortOrder) throws AtlasBaseException {
        if (Objects.isNull(termGuid)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "termGuid is null/empty");
        }

        AtlasGlossaryTerm         glossaryTerm     = dataAccess.load(getAtlasGlossaryTermSkeleton(termGuid));
        Set<AtlasRelatedObjectId> assignedEntities = glossaryTerm.getAssignedEntities();

        List<AtlasRelatedObjectId> ret;
        if (CollectionUtils.isNotEmpty(assignedEntities)) {
            ret = new ArrayList<>(assignedEntities);
            if (sortOrder != null) {
                ret.sort((o1, o2) -> sortOrder == SortOrder.ASCENDING ?
                                             o1.getDisplayText().compareTo(o2.getDisplayText()) :
                                             o2.getDisplayText().compareTo(o1.getDisplayText()));
            }
            ret = new PaginationHelper<>(assignedEntities, offset, limit).getPaginatedList();
        } else {
            ret = Collections.emptyList();
        }

        return ret;
    }

    private boolean glossaryExists(AtlasGlossary atlasGlossary) {
        AtlasVertex vertex = AtlasGraphUtilsV2.findByUniqueAttributes(atlasTypeRegistry.getEntityTypeByName(GlossaryUtils.ATLAS_GLOSSARY_TYPENAME), new HashMap<String, Object>() {{
            put(QUALIFIED_NAME_ATTR, atlasGlossary.getQualifiedName());
        }});
        return Objects.nonNull(vertex);
    }

    private boolean termExists(AtlasGlossaryTerm term) {
        AtlasVertex vertex = AtlasGraphUtilsV2.findByUniqueAttributes(atlasTypeRegistry.getEntityTypeByName(GlossaryUtils.ATLAS_GLOSSARY_TERM_TYPENAME), new HashMap<String, Object>() {{
            put(QUALIFIED_NAME_ATTR, term.getQualifiedName());
        }});
        return Objects.nonNull(vertex);
    }

    private boolean categoryExists(AtlasGlossaryCategory category) {
        AtlasVertex vertex = AtlasGraphUtilsV2.findByUniqueAttributes(atlasTypeRegistry.getEntityTypeByName(GlossaryUtils.ATLAS_GLOSSARY_CATEGORY_TYPENAME), new HashMap<String, Object>() {{
            put(QUALIFIED_NAME_ATTR, category.getQualifiedName());
        }});
        return Objects.nonNull(vertex);
    }

    private void deleteCategories(final AtlasGlossary storeObject, final Collection<AtlasRelatedCategoryHeader> categories) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(categories)) {
            if (DEBUG_ENABLED) {
                LOG.debug("Deleting categories within glossary guid = {}", storeObject.getGuid());
            }
            for (AtlasRelatedCategoryHeader category : categories) {
                // Delete category
                deleteCategory(category.getCategoryGuid());
            }
        }
    }

    private void deleteTerms(final AtlasGlossary storeObject, final Collection<AtlasRelatedTermHeader> terms) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(terms)) {
            if (DEBUG_ENABLED) {
                LOG.debug("Deleting terms within glossary guid = {}", storeObject.getGuid());
            }
            for (AtlasRelatedTermHeader term : terms) {
                // Delete the term
                deleteTerm(term.getTermGuid());
            }
        }
    }

    private void setInfoForRelations(final AtlasGlossary ret) throws AtlasBaseException {
        if (Objects.nonNull(ret.getTerms())) {
            setInfoForTerms(ret.getTerms());
        }

        if (Objects.nonNull(ret.getCategories())) {
            setInfoForRelatedCategories(ret.getCategories());
        }
    }

    private void setInfoForRelations(final AtlasGlossaryTerm ret) throws AtlasBaseException {
        if (Objects.nonNull(ret.getCategories())) {
            setDisplayNameForTermCategories(ret.getCategories());
        }
        if (Objects.nonNull(ret.getRelatedTerms())) {
            for (Map.Entry<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> entry : ret.getRelatedTerms().entrySet()) {
                setInfoForTerms(entry.getValue());
            }
        }
    }

    private void setInfoForRelations(final AtlasGlossaryCategory glossaryCategory) throws AtlasBaseException {
        if (Objects.nonNull(glossaryCategory.getChildrenCategories())) {
            setInfoForRelatedCategories(glossaryCategory.getChildrenCategories());
        }
        if (Objects.nonNull(glossaryCategory.getTerms())) {
            setInfoForTerms(glossaryCategory.getTerms());
        }
    }

    private void setDisplayNameForTermCategories(final Set<AtlasTermCategorizationHeader> categorizationHeaders) throws AtlasBaseException {
        List<AtlasGlossaryCategory> categories = categorizationHeaders
                                                         .stream()
                                                         .map(id -> getAtlasGlossaryCategorySkeleton(id.getCategoryGuid()))
                                                         .collect(Collectors.toList());
        Map<String, AtlasGlossaryCategory> categoryMap = new HashMap<>();
        dataAccess.load(categories).forEach(c -> categoryMap.put(c.getGuid(), c));
        categorizationHeaders.forEach(c -> c.setDisplayText(categoryMap.get(c.getCategoryGuid()).getName()));
    }

    private void setInfoForRelatedCategories(final Collection<AtlasRelatedCategoryHeader> categoryHeaders) throws AtlasBaseException {
        List<AtlasGlossaryCategory> categories = categoryHeaders
                                                         .stream()
                                                         .map(id -> getAtlasGlossaryCategorySkeleton(id.getCategoryGuid()))
                                                         .collect(Collectors.toList());
        Map<String, AtlasGlossaryCategory> categoryMap = new HashMap<>();
        dataAccess.load(categories).forEach(c -> categoryMap.put(c.getGuid(), c));
        for (AtlasRelatedCategoryHeader c : categoryHeaders) {
            AtlasGlossaryCategory category = categoryMap.get(c.getCategoryGuid());
            c.setDisplayText(category.getName());
            if (Objects.nonNull(category.getParentCategory())) {
                c.setParentCategoryGuid(category.getParentCategory().getCategoryGuid());
            }
        }
    }

    private void setInfoForTerms(final Collection<AtlasRelatedTermHeader> termHeaders) throws AtlasBaseException {
        List<AtlasGlossaryTerm> terms = termHeaders
                                                .stream()
                                                .map(id -> getAtlasGlossaryTermSkeleton(id.getTermGuid()))
                                                .collect(Collectors.toList());
        Map<String, AtlasGlossaryTerm> termMap = new HashMap<>();
        dataAccess.load(terms).iterator().forEachRemaining(t -> termMap.put(t.getGuid(), t));

        termHeaders.forEach(t -> t.setDisplayText(getDisplayText(termMap.get(t.getTermGuid()))));
    }

    public static boolean isNameInvalid(String name) {
        return StringUtils.containsAny(name, invalidNameChars);
    }

    private String getDuplicateGlossaryRelatedTerm(AtlasGlossaryTerm atlasGlossaryTerm) throws AtlasBaseException {
        Map<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> relatedTermsMap = atlasGlossaryTerm.getRelatedTerms();
        for (Map.Entry<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> relatedTermsMapEntry : relatedTermsMap.entrySet()) {
            Set<AtlasRelatedTermHeader> termHeaders = relatedTermsMapEntry.getValue();

            if (CollectionUtils.isNotEmpty(termHeaders)) {
                List<AtlasRelatedTermHeader> duplicateTermHeaders = termHeaders.stream()
                        .collect(Collectors.groupingBy(AtlasRelatedTermHeader::getTermGuid))
                        .values().stream()
                        .filter(duplicates -> duplicates.size() > 1)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(duplicateTermHeaders) && duplicateTermHeaders.size() > 0) {
                    String dupTermGuid = duplicateTermHeaders.get(0).getTermGuid();
                    AtlasGlossaryTerm glossaryTerm = getTerm(dupTermGuid);
                    return glossaryTerm.getQualifiedName();
                }
            }
        }
        return StringUtils.EMPTY;
    }

    private String getDisplayText(AtlasGlossaryTerm term) {
        return term != null ? term.getName() : null;
    }

    static class PaginationHelper<T> {
        private int     pageStart;
        private int     pageEnd;
        private int     maxSize;
        private List<T> items;

        PaginationHelper(Collection<T> items, int offset, int limit) {
            Objects.requireNonNull(items, "items can't be empty/null");
            this.items = new ArrayList<>(items);
            this.maxSize = items.size();

            // If limit is negative then limit is effectively the maxSize, else the smaller one out of limit and maxSize
            int adjustedLimit = limit < 0 ? maxSize : Integer.min(maxSize, limit);
            // Page starting can only be between zero and adjustedLimit
            pageStart = Integer.max(0, offset);
            // Page end can't exceed the maxSize
            pageEnd = Integer.min(adjustedLimit + pageStart, maxSize);
        }

        List<T> getPaginatedList() {
            List<T> ret;
            if (isValidOffset()) {
                if (isPagingNeeded()) {
                    ret = items.subList(pageStart, pageEnd);
                } else {
                    ret = items;
                }
            } else {
                ret = Collections.emptyList();
            }

            return ret;
        }

        private boolean isPagingNeeded() {
            return !(pageStart == 0 && pageEnd == maxSize) && pageStart <= pageEnd;
        }

        private boolean isValidOffset() {
            return pageStart <= maxSize;
        }
    }

    @GraphTransaction
    public BulkImportResponse importGlossaryData(InputStream inputStream, String fileName) throws AtlasBaseException {
        BulkImportResponse ret = new BulkImportResponse();

        if (StringUtils.isBlank(fileName)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_FILE_TYPE, fileName);
        }

        try {
            List<String[]> fileData = FileUtils.readFileData(fileName, inputStream);

            List<AtlasGlossaryTerm> glossaryTermsWithoutRelations = glossaryTermUtils.getGlossaryTermDataWithoutRelations(fileData, ret);
            createGlossaryTerms(glossaryTermsWithoutRelations, ret);

            List<AtlasGlossaryTerm> glossaryTermsWithRelations = glossaryTermUtils.getGlossaryTermDataWithRelations(fileData, ret);
            updateGlossaryTermsRelation(glossaryTermsWithRelations, ret);
        } finally {
            glossaryTermUtils.clearImportCache();
        }

        return ret;
    }

    private void createGlossaryTerms(List<AtlasGlossaryTerm> glossaryTerms, BulkImportResponse bulkImportResponse) throws AtlasBaseException {
        for (AtlasGlossaryTerm glossaryTerm : glossaryTerms) {
            String glossaryTermName = glossaryTerm.getName();
            String glossaryName     = getGlossaryName(glossaryTerm);

            try {
                AtlasGlossaryTerm createdTerm = createTerm(glossaryTerm);

                bulkImportResponse.addToSuccessImportInfoList(new ImportInfo(glossaryName, glossaryTermName, SUCCESS, AtlasJson.toJson(createdTerm.getGlossaryTermHeader())));
            } catch (AtlasBaseException e) {
                LOG.error(AtlasErrorCode.FAILED_TO_CREATE_GLOSSARY_TERM.toString(), glossaryTermName, e);

                bulkImportResponse.addToFailedImportInfoList(new ImportInfo(glossaryName, glossaryTermName, FAILED, e.getMessage()));
            }
        }

        checkForSuccessImports(bulkImportResponse);
    }

    private void updateGlossaryTermsRelation(List<AtlasGlossaryTerm> glossaryTerms, BulkImportResponse bulkImportResponse) {
        for (AtlasGlossaryTerm glossaryTerm : glossaryTerms) {
            glossaryTermUtils.updateGlossaryTermRelations(glossaryTerm);

            if (glossaryTerm.hasTerms()) {
                String glossaryTermName = glossaryTerm.getName();
                String glossaryName     = getGlossaryName(glossaryTerm);

                try {
                    updateTerm(glossaryTerm, false);
                } catch (AtlasBaseException e) {
                    LOG.error(AtlasErrorCode.FAILED_TO_UPDATE_GLOSSARY_TERM.toString(), glossaryTermName, e);

                    bulkImportResponse.addToFailedImportInfoList(new ImportInfo(glossaryName, glossaryTermName, FAILED, e.getMessage()));
                }
            }
        }
    }

    private String getGlossaryName(AtlasGlossaryTerm glossaryTerm) {
        String ret               = "";
        String glossaryTermQName = glossaryTerm.getQualifiedName();

        if (StringUtils.isNotBlank(glossaryTermQName)){
            String[] glossaryQnameSplit = glossaryTermQName.split("@");

            ret = (glossaryQnameSplit.length == 2) ? glossaryQnameSplit[1] : "";
        }

        return ret;
    }

    private void checkForSuccessImports(BulkImportResponse bulkImportResponse) throws AtlasBaseException {
        if (CollectionUtils.isEmpty(bulkImportResponse.getSuccessImportInfoList())) {
            throw new AtlasBaseException(AtlasErrorCode.GLOSSARY_IMPORT_FAILED);
        }
    }
}
