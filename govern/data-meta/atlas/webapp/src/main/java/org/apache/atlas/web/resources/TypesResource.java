/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.resources;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.core.ResourceContext;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.v1.model.typedef.TypesDef;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.repository.converters.TypeConverterUtil;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.atlas.web.rest.TypesREST;
import org.apache.atlas.web.util.Servlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides RESTful API for Types.
 *
 * A type is the description of any representable item;
 * e.g. a Hive table
 *
 * You could represent any meta model representing any domain using these types.
 */
@Path("types")
@Singleton
@Service
@Deprecated
public class TypesResource {
    private static final Logger LOG = LoggerFactory.getLogger(TypesResource.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("rest.TypesResource");
    private static AtlasTypeRegistry typeRegistry;
    private final TypesREST typesREST;
    private final AtlasTypeDefStore typeDefStore;

    @Inject
    public TypesResource(AtlasTypeRegistry typeRegistry, TypesREST typesREST, AtlasTypeDefStore typeDefStore) {
        this.typeRegistry = typeRegistry;
        this.typesREST    = typesREST;
        this.typeDefStore = typeDefStore;
    }

    @Context
    private ResourceContext resourceContext;

    /**
     * Submits a type definition corresponding to a given type representing a meta model of a
     * domain. Could represent things like Hive Database, Hive Table, etc.
     */
    @POST
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response submit(@Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TypesResource.submit()");
        }

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "TypesResource.submit()");
        }

        try {
            final String typeDefinition = Servlets.getRequestPayload(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating type with definition {} ", typeDefinition);
            }

            AtlasTypesDef createTypesDef  = TypeConverterUtil.toAtlasTypesDef(typeDefinition, typeRegistry);
            AtlasTypesDef createdTypesDef = typesREST.createAtlasTypeDefs(createTypesDef);
            List<String>  typeNames       = TypeConverterUtil.getTypeNames(createdTypesDef);
            List<Map<String, Object>> typesResponse = new ArrayList<>(typeNames.size());

            for (String typeName : typeNames) {
                typesResponse.add(Collections.singletonMap(AtlasClient.NAME, typeName));
            }

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.TYPES, typesResponse);
            return Response.status(ClientResponse.Status.CREATED).entity(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Type creation failed", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e));
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to persist types", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to persist types", e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to persist types", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== TypesResource.submit()");
            }
        }
    }

    /**
     * Update of existing types - if the given type doesn't exist, creates new type
     * Allowed updates are:
     * 1. Add optional attribute
     * 2. Change required to optional attribute
     * 3. Add super types - super types shouldn't contain any required attributes
     * @param request
     * @return
     */
    @PUT
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response update(@Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TypesResource.update()");
        }

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "TypesResource.update()");
        }

        try {
            final String typeDefinition = Servlets.getRequestPayload(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating type with definition {} ", typeDefinition);
            }

            AtlasTypesDef updateTypesDef  = TypeConverterUtil.toAtlasTypesDef(typeDefinition, typeRegistry);
            AtlasTypesDef updatedTypesDef = typeDefStore.createUpdateTypesDef(updateTypesDef);
            List<String>  typeNames       = TypeConverterUtil.getTypeNames(updatedTypesDef);
            List<Map<String, Object>> typesResponse = new ArrayList<>(typeNames.size());

            for (String typeName : typeNames) {
                typesResponse.add(Collections.singletonMap(AtlasClient.NAME, typeName));
            }

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.TYPES, typesResponse);
            return Response.ok().entity(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to persist types", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e));
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to persist types", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to persist types", e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to persist types", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== TypesResource.update()");
            }
        }
    }

    /**
     * Fetch the complete definition of a given type name which is unique.
     *
     * @param typeName name of a type which is unique.
     */
    @GET
    @Path("{typeName}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getDefinition(@Context HttpServletRequest request, @PathParam("typeName") String typeName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TypesResource.getDefinition({})", typeName);
        }

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "TypesResource.getDefinition(" + typeName + ")");
        }

        Map<String, Object> response = new HashMap<>();

        try {
            TypesDef typesDef = TypeConverterUtil.toTypesDef(typeRegistry.getType(typeName), typeRegistry);;

            response.put(AtlasClient.TYPENAME, typeName);
            response.put(AtlasClient.DEFINITION, typesDef);
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());

            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to get type definition for type {}", typeName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e));
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get type definition for type {}", typeName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get type definition for type {}", typeName, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get type definition for type {}", typeName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== TypesResource.getDefinition({})", typeName);
            }
        }
    }

    /**
     * Return the list of type names in the type system which match the specified filter.
     *
     * @return list of type names
     * @param typeCategory returns types whose relationshipCategory is the given typeCategory
     * @param supertype returns types which contain the given supertype
     * @param notsupertype returns types which do not contain the given supertype
     *
     * Its possible to specify combination of these filters in one request and the conditions are combined with AND
     * For example, typeCategory = TRAIT && supertype contains 'X' && supertype !contains 'Y'
     * If there is no filter, all the types are returned
     */
    @GET
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getTypesByFilter(@Context HttpServletRequest request, @QueryParam("type") String typeCategory,
                                     @QueryParam("supertype") String supertype,
                                     @QueryParam("notsupertype") String notsupertype) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> TypesResource.getTypesByFilter({}, {}, {})", typeCategory, supertype, notsupertype);
        }

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "TypesResource.getTypesByFilter(" + typeCategory + ", " + supertype + ", " + notsupertype + ")");
        }

        Map<String, Object> response = new HashMap<>();
        try {
            List<String> result = TypeConverterUtil.getTypeNames(typesREST.getTypeDefHeaders(request));

            response.put(AtlasClient.RESULTS, result);
            response.put(AtlasClient.COUNT, result.size());
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());

            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.warn("TypesREST exception: {} {}", e.getClass().getSimpleName(), e.getMessage());
            throw new WebApplicationException(Servlets.getErrorResponse(e));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get types list", e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get types list", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== TypesResource.getTypesByFilter({}, {}, {})", typeCategory, supertype, notsupertype);
            }
        }
    }
}
