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
package org.apache.atlas.web.rest;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.SortOrder;
import org.apache.atlas.annotation.Timed;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.glossary.GlossaryService;
import org.apache.atlas.glossary.GlossaryTermUtils;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.atlas.web.util.Servlets;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Path("v2/glossary")
@Service
@Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
@Produces({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
public class GlossaryREST {
    private static final Logger LOG = LoggerFactory.getLogger(GlossaryREST.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("rest.GlossaryREST");

    private final GlossaryService glossaryService;

    @Inject
    public GlossaryREST(final GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
    }

    /**
     * Retrieve all glossaries registered with Atlas
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort Sort order, ASC (default) or DESC
     * @return List of glossary entities fitting the above criteria
     * @HTTP 200 List of existing glossaries fitting the search criteria or empty list if nothing matches
     * @throws AtlasBaseException
     */
    @GET
    @Timed
    public List<AtlasGlossary> getGlossaries(@DefaultValue("-1") @QueryParam("limit") final String limit,
                                             @DefaultValue("0") @QueryParam("offset") final String offset,
                                             @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaries()");
            }


            return glossaryService.getGlossaries(Integer.parseInt(limit), Integer.parseInt(offset), toSortOrder(sort));
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get a specific Glossary
     * @param glossaryGuid unique glossary identifier
     * @return Glossary
     * @throws AtlasBaseException
     * @HTTP 200 If glossary with given guid exists
     * @HTTP 404 If glossary GUID is invalid
     */
    @GET
    @Path("/{glossaryGuid}")
    @Timed
    public AtlasGlossary getGlossary(@PathParam("glossaryGuid") String glossaryGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossary(" + glossaryGuid + ")");
            }
            AtlasGlossary ret = glossaryService.getGlossary(glossaryGuid);

            if (ret == null) {
                throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }

            return ret;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get a specific Glossary
     * @param glossaryGuid unique glossary identifier
     * @return Glossary
     * @throws AtlasBaseException
     * @HTTP 200 If glossary exists for given GUID
     * @HTTP 404 If glossary GUID is invalid
     */
    @GET
    @Path("/{glossaryGuid}/detailed")
    @Timed
    public AtlasGlossary.AtlasGlossaryExtInfo getDetailedGlossary(@PathParam("glossaryGuid") String glossaryGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getDetailedGlossary(" + glossaryGuid + ")");
            }
            AtlasGlossary.AtlasGlossaryExtInfo ret = glossaryService.getDetailedGlossary(glossaryGuid);

            if (ret == null) {
                throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }

            return ret;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get specific glossary term
     * @param termGuid unique identifier for glossary term
     * @return Glossary term
     * @throws AtlasBaseException
     * @HTTP 200 If glossary term exists for given GUID
     * @HTTP 404 If glossary term GUID is invalid
     */
    @GET
    @Path("/term/{termGuid}")
    @Timed
    public AtlasGlossaryTerm getGlossaryTerm(@PathParam("termGuid") String termGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaryTerm(" + termGuid + ")");
            }
            AtlasGlossaryTerm ret = glossaryService.getTerm(termGuid);
            if (ret == null) {
                throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }

            return ret;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get specific glossary category
     * @param categoryGuid unique identifier for glossary category
     * @return Glossary category
     * @throws AtlasBaseException
     * @HTTP 200 If glossary category exists for given GUID
     * @HTTP 404 If glossary category GUID is invalid
     */
    @GET
    @Path("/category/{categoryGuid}")
    @Timed
    public AtlasGlossaryCategory getGlossaryCategory(@PathParam("categoryGuid") String categoryGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("categoryGuid", categoryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaryCategory(" + categoryGuid + ")");
            }
            AtlasGlossaryCategory ret = glossaryService.getCategory(categoryGuid);

            if (ret == null) {
                throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }

            return ret;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Create a glossary
     * @param atlasGlossary Glossary definition, terms & categories can be anchored to a glossary
     *                      using the anchor attribute when creating the Term/Category
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 If glossary creation was successful
     * @HTTP 400 If Glossary definition has invalid or missing information
     * @HTTP 409 If Glossary definition already exists (duplicate qualifiedName)
     */
    @POST
    @Timed
    public AtlasGlossary createGlossary(AtlasGlossary atlasGlossary) throws AtlasBaseException {
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.createGlossary()");
            }
            return glossaryService.createGlossary(atlasGlossary);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Create a glossary term
     * @param glossaryTerm Glossary term definition, a term must be anchored to a Glossary at the time of creation
     *                     optionally it can be categorized as well
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 If glossary term creation was successful
     * @HTTP 400 If Glossary term definition has invalid or missing information
     * @HTTP 409 If Glossary term already exists (duplicate qualifiedName)
     */
    @POST
    @Path("/term")
    @Timed
    public AtlasGlossaryTerm createGlossaryTerm(AtlasGlossaryTerm glossaryTerm) throws AtlasBaseException {
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.createGlossaryTerm()");
            }
            if (Objects.isNull(glossaryTerm.getAnchor())) {
                throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
            }
            return glossaryService.createTerm(glossaryTerm);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Create glossary terms in bulk
     * @param glossaryTerm glossary term definitions
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 If Bulk glossary terms creation was successful
     * @HTTP 400 If any glossary term definition has invalid or missing information
     */
    @POST
    @Path("/terms")
    @Timed
    public List<AtlasGlossaryTerm> createGlossaryTerms(List<AtlasGlossaryTerm> glossaryTerm) throws AtlasBaseException {
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.createGlossaryTerms()");
            }
            for (AtlasGlossaryTerm term : glossaryTerm) {
                if (Objects.isNull(term.getAnchor())) {
                    throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
                }
            }
            return glossaryService.createTerms(glossaryTerm);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Create glossary category
     * @param glossaryCategory glossary category definition, a category must be anchored to a Glossary when creating
     *                         Optionally, terms belonging to the category and the hierarchy can also be defined during creation
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 If glossary category creation was successful
     * @HTTP 400 If Glossary category definition has invalid or missing information
     * @HTTP 409 If Glossary category already exists (duplicate qualifiedName)
     */
    @POST
    @Path("/category")
    @Timed
    public AtlasGlossaryCategory createGlossaryCategory(AtlasGlossaryCategory glossaryCategory) throws AtlasBaseException {
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.createGlossaryCategory()");
            }
            if (Objects.isNull(glossaryCategory.getAnchor())) {
                throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
            }
            return glossaryService.createCategory(glossaryCategory);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Create glossary category in bulk
     * @param glossaryCategory glossary category definitions
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 If BULK glossary category creation was successful
     * @HTTP 400 If ANY Glossary category definition has invalid or missing information
     */
    @POST
    @Path("/categories")
    @Timed
    public List<AtlasGlossaryCategory> createGlossaryCategories(List<AtlasGlossaryCategory> glossaryCategory) throws AtlasBaseException {
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.createGlossaryCategories()");
            }
            for (AtlasGlossaryCategory category : glossaryCategory) {
                if (Objects.isNull(category.getAnchor())) {
                    throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ANCHOR);
                }

            }
            return glossaryService.createCategories(glossaryCategory);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Update the given glossary
     * @param glossaryGuid unique identifier for glossary
     * @param updatedGlossary Updated glossary definition
     * @return Glossary
     * @throws AtlasBaseException
     * @HTTP 200 If glossary update was successful
     * @HTTP 404 If glossary guid in invalid
     * @HTTP 400 If Glossary definition has invalid or missing information
     */
    @PUT
    @Path("/{glossaryGuid}")
    @Timed
    public AtlasGlossary updateGlossary(@PathParam("glossaryGuid") String glossaryGuid, AtlasGlossary updatedGlossary) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.updateGlossary(" + glossaryGuid + ")");
            }
            updatedGlossary.setGuid(glossaryGuid);
            return glossaryService.updateGlossary(updatedGlossary);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Partially update the glossary
     * @param glossaryGuid unique identifier for glossary term
     * @param partialUpdates Map containing keys as attribute names and values as corresponding attribute values
     * @return Updated glossary
     * @throws AtlasBaseException
     * @HTTP 200 If glossary partial update was successful
     * @HTTP 404 If glossary guid in invalid
     * @HTTP 400 If partial update parameters are invalid
     */
    @PUT
    @Path("/{glossaryGuid}/partial")
    @Timed
    public AtlasGlossary partialUpdateGlossary(@PathParam("glossaryGuid") String glossaryGuid, Map<String, String> partialUpdates) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.partialUpdateGlossary()");
            }

            if (MapUtils.isEmpty(partialUpdates)) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "PartialUpdates missing or empty");
            }

            AtlasGlossary glossary = glossaryService.getGlossary(glossaryGuid);
            for (Map.Entry<String, String> entry : partialUpdates.entrySet()) {
                try {
                    glossary.setAttribute(entry.getKey(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_PARTIAL_UPDATE_ATTR, entry.getKey(), "Glossary");
                }
            }
            return glossaryService.updateGlossary(glossary);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Update the given glossary term
     * @param termGuid unique identifier for glossary term
     * @param glossaryTerm updated glossary term
     * @return Updated glossary term
     * @throws AtlasBaseException
     * @HTTP 200 If glossary term update was successful
     * @HTTP 404 If glossary term guid in invalid
     * @HTTP 400 If Glossary temr definition has invalid or missing information
     */
    @PUT
    @Path("/term/{termGuid}")
    @Timed
    public AtlasGlossaryTerm updateGlossaryTerm(@PathParam("termGuid") String termGuid, AtlasGlossaryTerm glossaryTerm) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.updateGlossaryTerm()");
            }
            glossaryTerm.setGuid(termGuid);
            return glossaryService.updateTerm(glossaryTerm);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Partially update the glossary term
     * @param termGuid unique identifier for glossary term
     * @param partialUpdates Map containing keys as attribute names and values as corresponding attribute values
     * @return Updated glossary term
     * @throws AtlasBaseException
     * @HTTP 200 If glossary partial update was successful
     * @HTTP 404 If glossary term guid in invalid
     * @HTTP 400 If partial attributes are invalid
     */
    @PUT
    @Path("/term/{termGuid}/partial")
    @Timed
    public AtlasGlossaryTerm partialUpdateGlossaryTerm(@PathParam("termGuid") String termGuid, Map<String, String> partialUpdates) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.partialUpdateGlossaryTerm()");
            }

            if (MapUtils.isEmpty(partialUpdates)) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "PartialUpdates missing or empty");
            }

            AtlasGlossaryTerm glossaryTerm = glossaryService.getTerm(termGuid);
            for (Map.Entry<String, String> entry : partialUpdates.entrySet()) {
                try {
                    glossaryTerm.setAttribute(entry.getKey(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_PARTIAL_UPDATE_ATTR, "Glossary Term", entry.getKey());
                }
            }
            return glossaryService.updateTerm(glossaryTerm);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Update the given glossary category
     * @param categoryGuid unique identifier for glossary category
     * @param glossaryCategory updated glossary category
     * @return glossary category
     * @throws AtlasBaseException
     * @HTTP 200 If glossary category partial update was successful
     * @HTTP 404 If glossary category guid in invalid
     * @HTTP 400 If Glossary category definition has invalid or missing information
     */
    @PUT
    @Path("/category/{categoryGuid}")
    @Timed
    public AtlasGlossaryCategory updateGlossaryCategory(@PathParam("categoryGuid") String categoryGuid, AtlasGlossaryCategory glossaryCategory) throws AtlasBaseException {
        Servlets.validateQueryParamLength("categoryGuid", categoryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.updateGlossaryCategory()");
            }
            glossaryCategory.setGuid(categoryGuid);
            return glossaryService.updateCategory(glossaryCategory);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Partially update the glossary category
     * @param categoryGuid unique identifier for glossary term
     * @param partialUpdates Map containing keys as attribute names and values as corresponding attribute values
     * @return Updated glossary category
     * @throws AtlasBaseException
     * @HTTP 200 If glossary category partial update was successful
     * @HTTP 404 If glossary category guid in invalid
     * @HTTP 400 If category attributes are invalid
     */
    @PUT
    @Path("/category/{categoryGuid}/partial")
    @Timed
    public AtlasGlossaryCategory partialUpdateGlossaryCategory(@PathParam("categoryGuid") String categoryGuid, Map<String, String> partialUpdates) throws AtlasBaseException {
        Servlets.validateQueryParamLength("categoryGuid", categoryGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.partialUpdateGlossaryCategory()");
            }

            if (MapUtils.isEmpty(partialUpdates)) {
                throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "PartialUpdates missing or empty");
            }

            AtlasGlossaryCategory glossaryCategory = glossaryService.getCategory(categoryGuid);
            for (Map.Entry<String, String> entry : partialUpdates.entrySet()) {
                try {
                    glossaryCategory.setAttribute(entry.getKey(), entry.getValue());
                } catch (IllegalArgumentException e) {
                    throw new AtlasBaseException(AtlasErrorCode.INVALID_PARTIAL_UPDATE_ATTR, "Glossary Category", entry.getKey());
                }
            }
            return glossaryService.updateCategory(glossaryCategory);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Delete a glossary
     * @param glossaryGuid unique identifier for glossary
     * @throws AtlasBaseException
     * @HTTP 204 If glossary delete was successful
     * @HTTP 404 If glossary guid in invalid
     */
    @DELETE
    @Path("/{glossaryGuid}")
    @Timed
    public void deleteGlossary(@PathParam("glossaryGuid") String glossaryGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.deleteGlossary(" + glossaryGuid + ")");
            }
            glossaryService.deleteGlossary(glossaryGuid);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Delete a glossary term
     * @param termGuid unique identifier for glossary term
     * @throws AtlasBaseException
     * @HTTP 204 If glossary term delete was successful
     * @HTTP 404 If glossary term guid in invalid
     */
    @DELETE
    @Path("/term/{termGuid}")
    @Timed
    public void deleteGlossaryTerm(@PathParam("termGuid") String termGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.deleteGlossaryTerm(" + termGuid + ")");
            }
            glossaryService.deleteTerm(termGuid);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Delete a glossary category
     * @param categoryGuid unique identifier for glossary category
     * @throws AtlasBaseException
     * @HTTP 204 If glossary category delete was successful
     * @HTTP 404 If glossary category guid in invalid
     */
    @DELETE
    @Path("/category/{categoryGuid}")
    @Timed
    public void deleteGlossaryCategory(@PathParam("categoryGuid") String categoryGuid) throws AtlasBaseException {
        Servlets.validateQueryParamLength("categoryGuid", categoryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.deleteGlossaryCategory(" + categoryGuid + ")");
            }
            glossaryService.deleteCategory(categoryGuid);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get terms belonging to a specific glossary
     * @param glossaryGuid unique identifier for glossary
     * @param limit page size - by default there is no paging
     * @param offset starting offset for loading terms
     * @param sort ASC(default) or DESC
     * @return List of terms associated with the glossary
     * @throws AtlasBaseException
     * @HTTP 200 List of glossary terms for the given glossary or an empty list
     * @HTTP 404 If glossary guid in invalid
     */
    @GET
    @Path("/{glossaryGuid}/terms")
    @Timed
    public List<AtlasGlossaryTerm> getGlossaryTerms(@PathParam("glossaryGuid") String glossaryGuid,
                                                         @DefaultValue("-1") @QueryParam("limit") String limit,
                                                         @DefaultValue("0") @QueryParam("offset") String offset,
                                                         @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaryTerms(" + glossaryGuid + ")");
            }

            return glossaryService.getGlossaryTerms(glossaryGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get term headers belonging to a specific glossary
     * @param glossaryGuid unique identifier for glossary
     * @param limit page size - by default there is no paging
     * @param offset starting offset for loading terms
     * @param sort ASC(default) or DESC
     * @return List of terms associated with the glossary
     * @throws AtlasBaseException
     * @HTTP 200 List of glossary terms for the given glossary or an empty list
     * @HTTP 404 If glossary guid in invalid
     */
    @GET
    @Path("/{glossaryGuid}/terms/headers")
    @Timed
    public List<AtlasRelatedTermHeader> getGlossaryTermHeaders(@PathParam("glossaryGuid") String glossaryGuid,
                                                         @DefaultValue("-1") @QueryParam("limit") String limit,
                                                         @DefaultValue("0") @QueryParam("offset") String offset,
                                                         @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaryTermHeaders(" + glossaryGuid + ")");
            }

            return glossaryService.getGlossaryTermsHeaders(glossaryGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get the categories belonging to a specific glossary
     * @param glossaryGuid unique identifier for glossary term
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort ASC (default) or DESC
     * @return List of associated categories
     * @throws AtlasBaseException
     * @HTTP 200 List of glossary categories for the given glossary or an empty list
     * @HTTP 404 If glossary guid in invalid
     */
    @GET
    @Path("/{glossaryGuid}/categories")
    @Timed
    public List<AtlasGlossaryCategory> getGlossaryCategories(@PathParam("glossaryGuid") String glossaryGuid,
                                                                  @DefaultValue("-1") @QueryParam("limit") String limit,
                                                                  @DefaultValue("0") @QueryParam("offset") String offset,
                                                                  @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaryCategories(" + glossaryGuid + ")");
            }

            return glossaryService.getGlossaryCategories(glossaryGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get the categories belonging to a specific glossary
     * @param glossaryGuid unique identifier for glossary term
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort ASC (default) or DESC
     * @return List of associated categories
     * @throws AtlasBaseException
     * @HTTP 200 List of glossary categories for the given glossary or an empty list
     * @HTTP 404 If glossary guid in invalid
     */
    @GET
    @Path("/{glossaryGuid}/categories/headers")
    @Timed
    public List<AtlasRelatedCategoryHeader> getGlossaryCategoriesHeaders(@PathParam("glossaryGuid") String glossaryGuid,
                                                                  @DefaultValue("-1") @QueryParam("limit") String limit,
                                                                  @DefaultValue("0") @QueryParam("offset") String offset,
                                                                  @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("glossaryGuid", glossaryGuid);
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getGlossaryCategoriesHeaders(" + glossaryGuid + ")");
            }

            return glossaryService.getGlossaryCategoriesHeaders(glossaryGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get all terms associated with the specific category
     * @param categoryGuid unique identifier for glossary category
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort ASC (default) or DESC
     * @return List of associated terms
     * @throws AtlasBaseException
     * @HTTP 200 List of terms for the given category or an empty list
     * @HTTP 404 If glossary category guid in invalid
     */
    @GET
    @Path("/category/{categoryGuid}/terms")
    @Timed
    public List<AtlasRelatedTermHeader> getCategoryTerms(@PathParam("categoryGuid") String categoryGuid,
                                                         @DefaultValue("-1") @QueryParam("limit") String limit,
                                                         @DefaultValue("0") @QueryParam("offset") String offset,
                                                         @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("categoryGuid", categoryGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getCategoryTerms(" + categoryGuid + ")");
            }

            return glossaryService.getCategoryTerms(categoryGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get all related terms for a specific term
     * @param termGuid unique identifier for glossary term
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort ASC (default) or DESC
     * @return List of all related terms
     * @throws AtlasBaseException
     * @HTTP 200 List of related glossary terms for the given glossary or an empty list
     * @HTTP 404 If glossary term guid in invalid
     */
    @GET
    @Path("/terms/{termGuid}/related")
    @Timed
    public Map<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> getRelatedTerms(@PathParam("termGuid") String termGuid,
                                                                                        @DefaultValue("-1") @QueryParam("limit") String limit,
                                                                                        @DefaultValue("0") @QueryParam("offset") String offset,
                                                                                        @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getRelatedTermsInfo(" + termGuid + ")");
            }

            return glossaryService.getRelatedTerms(termGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Get all entity headers assigned with the specified term
     * @param termGuid GUID of the term
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort ASC (default) or DESC
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 List of entity headers (if any) for the given glossary or an empty list
     * @HTTP 404 If glossary term guid in invalid
     */
    @GET
    @Path("/terms/{termGuid}/assignedEntities")
    @Timed
    public List<AtlasRelatedObjectId> getEntitiesAssignedWithTerm(@PathParam("termGuid") String termGuid,
                                                                  @DefaultValue("-1") @QueryParam("limit") String limit,
                                                                  @DefaultValue("0") @QueryParam("offset") String offset,
                                                                  @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getEntitiesAssignedWithTerm(" + termGuid + ")");
            }

            return glossaryService.getAssignedEntities(termGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }


    /**
     * Assign the given term to the provided list of entity headers
     * @param termGuid Glossary term GUID
     * @param relatedObjectIds Related Entity IDs to which the term has to be associated
     * @throws AtlasBaseException
     * @HTTP 204 If the term assignment was successful
     * @HTTP 400 If ANY of the entity header is invalid
     * @HTTP 404 If glossary guid in invalid
     */
    @POST
    @Path("/terms/{termGuid}/assignedEntities")
    @Timed
    public void assignTermToEntities(@PathParam("termGuid") String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasBaseException {
        Servlets.validateQueryParamLength("termGuid", termGuid);

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.assignTermToEntities(" + termGuid + ")");
            }

            glossaryService.assignTermToEntities(termGuid, relatedObjectIds);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Remove the term assignment for the given list of entity headers
     * @param termGuid Glossary term GUID
     * @param relatedObjectIds List of related entity IDs from which the term has to be dissociated
     * @throws AtlasBaseException
     * @HTTP 204 If glossary term dissociation was successful
     * @HTTP 400 If ANY of the entity header is invalid
     * @HTTP 404 If glossary term guid in invalid
     */
    @DELETE
    @Path("/terms/{termGuid}/assignedEntities")
    @Timed
    public void removeTermAssignmentFromEntities(@PathParam("termGuid") String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasBaseException {
        removeTermFromGlossary(termGuid, relatedObjectIds);
    }


    /**
     * Remove the term assignment for the given list of entity headers
     * @param termGuid Glossary term GUID
     * @param relatedObjectIds List of related entity IDs from which the term has to be dissociated
     * @throws AtlasBaseException
     * @HTTP 204 If glossary term dissociation was successful
     * @HTTP 400 If ANY of the entity header is invalid
     * @HTTP 404 If glossary term guid in invalid
     */
    @PUT
    @Path("/terms/{termGuid}/assignedEntities")
    @Timed
    public void disassociateTermAssignmentFromEntities(@PathParam("termGuid") String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasBaseException {
        removeTermFromGlossary(termGuid, relatedObjectIds);
    }



    private void removeTermFromGlossary(String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasBaseException{

        Servlets.validateQueryParamLength("termGuid", termGuid) ;

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.removeTermFromGlossary(" + termGuid + ")");
            }

            glossaryService.removeTermFromEntities(termGuid, relatedObjectIds);
        } finally {
            AtlasPerfTracer.log(perf);
        }

    }

    /**
     * Get all related categories (parent and children)
     * @param categoryGuid unique identifier for glossary category
     * @param limit page size - by default there is no paging
     * @param offset offset for pagination purpose
     * @param sort ASC (default) or DESC
     * @return List of related categories
     * @throws AtlasBaseException
     */
    @GET
    @Path("/category/{categoryGuid}/related")
    @Timed
    public Map<String, List<AtlasRelatedCategoryHeader>> getRelatedCategories(@PathParam("categoryGuid") String categoryGuid,
                                                                         @DefaultValue("-1") @QueryParam("limit") String limit,
                                                                         @DefaultValue("0") @QueryParam("offset") String offset,
                                                                         @DefaultValue("ASC") @QueryParam("sort") final String sort) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "GlossaryREST.getRelatedCategories()");
            }

            return glossaryService.getRelatedCategories(categoryGuid, Integer.parseInt(offset), Integer.parseInt(limit), toSortOrder(sort));

        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    private SortOrder toSortOrder(final String sort) {
        SortOrder ret = SortOrder.ASCENDING;
        if (!"ASC".equals(sort)) {
            if ("DESC".equals(sort)) {
                ret = SortOrder.DESCENDING;
            }
        }
        return ret;
    }

    /**
     * Get sample template for uploading/creating bulk AtlasGlossaryTerm
     *
     * @return Template File
     * @HTTP 400 If the provided fileType is not supported
     */
    @GET
    @Path("/import/template")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput produceTemplate() {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                outputStream.write(GlossaryTermUtils.getGlossaryTermHeaders().getBytes());
            }
        };
    }

    /**
     * Upload glossary file for creating AtlasGlossaryTerms in bulk
     *
     * @param inputStream InputStream of file
     * @param fileDetail  FormDataContentDisposition metadata of file
     * @return
     * @throws AtlasBaseException
     * @HTTP 200 If glossary term creation was successful
     * @HTTP 400 If Glossary term definition has invalid or missing information
     * @HTTP 409 If Glossary term already exists (duplicate qualifiedName)
     */
    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Timed
    public BulkImportResponse importGlossaryData(@FormDataParam("file") InputStream inputStream,
                                                 @FormDataParam("file") FormDataContentDisposition fileDetail) throws AtlasBaseException {
        return glossaryService.importGlossaryData(inputStream, fileDetail.getFileName());
    }
}