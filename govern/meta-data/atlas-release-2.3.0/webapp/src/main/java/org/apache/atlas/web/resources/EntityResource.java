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

package org.apache.atlas.web.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.CreateUpdateEntitiesResult;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.GuidMapping;
import org.apache.atlas.model.legacy.EntityResult;
import org.apache.atlas.repository.audit.EntityAuditRepository;
import org.apache.atlas.repository.converters.AtlasInstanceConverter;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.atlas.utils.ParamChecker;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.web.rest.EntityREST;
import org.apache.atlas.web.util.Servlets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Entity management operations as REST API.
 *
 * An entity is an "instance" of a Type.  Entities conform to the definition
 * of the Type they correspond with.
 */
@Singleton
@Path("entities")
@Service
@Deprecated
public class EntityResource {

    private static final Logger LOG = LoggerFactory.getLogger(EntityResource.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("rest.EntityResource");

    private static final String TRAIT_NAME = "traitName";

    private final AtlasInstanceConverter restAdapters;
    private final AtlasEntityStore       entitiesStore;
    private final AtlasTypeRegistry      typeRegistry;
    private final EntityREST             entityREST;
    private final EntityAuditRepository  entityAuditRepository;
    private final AtlasInstanceConverter instanceConverter;

    @Context
    UriInfo uriInfo;

    @Inject
    public EntityResource(final AtlasInstanceConverter restAdapters,
                          final AtlasEntityStore entitiesStore,
                          final AtlasTypeRegistry typeRegistry,
                          final EntityREST entityREST,
                          final EntityAuditRepository entityAuditRepository,
                          final AtlasInstanceConverter instanceConverter) {
        this.restAdapters  = restAdapters;
        this.entitiesStore = entitiesStore;
        this.typeRegistry  = typeRegistry;
        this.entityREST    = entityREST;
        this.entityAuditRepository = entityAuditRepository;
        this.instanceConverter = instanceConverter;
    }

    /**
     * Submits the entity definitions (instances).
     * The body contains the JSONArray of entity json. The service takes care of de-duping the entities based on any
     * unique attribute for the give type.
     */
    @POST
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response submit(@Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.submit()");
        }

        String entityJson = null;
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.submit()");
            }

            entityJson = Servlets.getRequestPayload(request);

            //Handle backward compatibility - if entities is not JSONArray, convert to JSONArray
            String[] jsonStrings;

            try {
                ArrayNode jsonEntities = AtlasJson.parseToV1ArrayNode(entityJson);

                jsonStrings = new String[jsonEntities.size()];

                for (int i = 0; i < jsonEntities.size(); i++) {
                    jsonStrings[i] = AtlasJson.toV1Json(jsonEntities.get(i));
                }
            } catch (IOException e) {
                jsonStrings = new String[1];

                jsonStrings[0] = entityJson;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("submitting entities: count={}; entities-json={}", jsonStrings.length, entityJson);
            }

            AtlasEntitiesWithExtInfo entitiesInfo     = restAdapters.toAtlasEntities(jsonStrings);
            EntityMutationResponse   mutationResponse = entityREST.createOrUpdate(entitiesInfo);

            final List<String> guids = restAdapters.getGuids(mutationResponse.getCreatedEntities());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Created entities {}", guids);
            }

            final CreateUpdateEntitiesResult result = restAdapters.toCreateUpdateEntitiesResult(mutationResponse);

            String response    = getResponse(result);
            URI    locationURI = getLocationURI(guids);

            return Response.created(locationURI).entity(response).build();

        } catch (AtlasBaseException e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw toWebApplicationException(e);
        } catch (AtlasException | IllegalArgumentException e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.submit()");
            }

        }
    }

    @VisibleForTesting
    public URI getLocationURI(List<String> guids) {
        URI locationURI = null;
        if (uriInfo != null) {
            UriBuilder ub = uriInfo.getAbsolutePathBuilder();
            locationURI = CollectionUtils.isEmpty(guids) ? null : ub.path(guids.get(0)).build();
        } else {
            String uriPath = AtlasClient.API_V1.GET_ENTITY.getNormalizedPath();
            locationURI = guids.isEmpty() ? null : UriBuilder
                .fromPath(AtlasConstants.DEFAULT_ATLAS_REST_ADDRESS)
                .path(uriPath).path(guids.get(0)).build();

        }
        return locationURI;
    }

    private String getResponse(EntityResult entityResult) throws AtlasBaseException, AtlasException {
        CreateUpdateEntitiesResult result = new CreateUpdateEntitiesResult();
        result.setEntityResult(entityResult);
        return getResponse(result);

    }
    private String getResponse(CreateUpdateEntitiesResult result) throws AtlasBaseException, AtlasException {
        Map<String, Object> response     = new HashMap<>();
        EntityResult        entityResult = result.getEntityResult();
        GuidMapping         mapping      = result.getGuidMapping();

        response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());

        if(entityResult != null) {
            response.put(AtlasClient.ENTITIES, entityResult.getEntities());

            String sampleEntityId = getSample(result.getEntityResult());
            if (sampleEntityId != null) {
                response.put(AtlasClient.DEFINITION, getEntity(sampleEntityId));
            }
        }

        if(mapping != null) {
            response.put(AtlasClient.GUID_ASSIGNMENTS, mapping);
        }
        return AtlasJson.toV1Json(response);
    }

    /**
     * Complete update of a set of entities - the values not specified will be replaced with null/removed
     * Adds/Updates given entities identified by its GUID or unique attribute
     * @return response payload as json
     */
    @PUT
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response updateEntities(@Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.updateEntities()");
        }

        String entityJson = null;
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.updateEntities()");
            }

            entityJson = Servlets.getRequestPayload(request);

            //Handle backward compatibility - if entities is not JSONArray, convert to JSONArray
            String[] jsonStrings;

            try {
                ArrayNode jsonEntities = AtlasJson.parseToV1ArrayNode(entityJson);

                jsonStrings = new String[jsonEntities.size()];

                for (int i = 0; i < jsonEntities.size(); i++) {
                    jsonStrings[i] = AtlasJson.toV1Json(jsonEntities.get(i));
                }
            } catch (IOException e) {
                jsonStrings = new String[1];

                jsonStrings[0] = entityJson;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating entities: count={}; entities-json={}", jsonStrings.length, entityJson);
            }

            AtlasEntitiesWithExtInfo   entitiesInfo     = restAdapters.toAtlasEntities(jsonStrings);
            EntityMutationResponse     mutationResponse = entityREST.createOrUpdate(entitiesInfo);
            CreateUpdateEntitiesResult result           = restAdapters.toCreateUpdateEntitiesResult(mutationResponse);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updated entities: {}", result.getEntityResult());
            }

            String response = getResponse(result);
            return Response.ok(response).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw toWebApplicationException(e);
        } catch (AtlasException | IllegalArgumentException e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to persist entity instance entityDef={}", entityJson, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.updateEntities()");
            }
        }
    }

    private String getSample(EntityResult entityResult) {
        String sample = getSample(entityResult.getCreatedEntities());
        if (sample == null) {
            sample = getSample(entityResult.getUpdateEntities());
        }
        return sample;
    }


    private String getSample(List<String> list) {
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Adds/Updates given entity identified by its unique attribute( entityType, attributeName and value)
     * Updates support only partial update of an entity - Adds/updates any new values specified
     * Updates do not support removal of attribute values
     *
     * @param entityType the entity type
     * @param attribute the unique attribute used to identify the entity
     * @param value the unique attributes value
     * @param request The updated entity json
     * @return response payload as json
     * The body contains the JSONArray of entity json. The service takes care of de-duping the entities based on any
     * unique attribute for the give type.
     */
    @POST
    @Path("qualifiedName")
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response updateByUniqueAttribute(@QueryParam("type") String entityType,
                                            @QueryParam("property") String attribute,
                                            @QueryParam("value") String value, @Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.updateByUniqueAttribute({}, {}, {})", entityType, attribute, value);
        }

        AtlasPerfTracer perf = null;
        String entityJson = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.updateByUniqueAttribute(" + entityType + ", " + attribute + ", " + value + ")");
            }

            entityJson = Servlets.getRequestPayload(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Partially updating entity by unique attribute {} {} {} {} ", entityType, attribute, value, entityJson);
            }

            Referenceable updatedEntity = AtlasType.fromV1Json(entityJson, Referenceable.class);

            entityType = ParamChecker.notEmpty(entityType, "Entity type cannot be null");
            attribute  = ParamChecker.notEmpty(attribute, "attribute name cannot be null");
            value      = ParamChecker.notEmpty(value, "attribute value cannot be null");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put(attribute, value);

            // update referenceable with Id if not specified in payload
            Id updateId = updatedEntity.getId();

            if (updateId != null && !AtlasTypeUtil.isAssignedGuid(updateId.getId())) {
                String guid = AtlasGraphUtilsV2.getGuidByUniqueAttributes(getEntityType(entityType), attributes);

                updatedEntity.setId(new Id(guid, 0, updatedEntity.getTypeName()));
            }

            AtlasEntitiesWithExtInfo   entitiesInfo     = restAdapters.toAtlasEntity(updatedEntity);
            EntityMutationResponse     mutationResponse = entitiesStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), true);
            CreateUpdateEntitiesResult result           = restAdapters.toCreateUpdateEntitiesResult(mutationResponse);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updated entities: {}", result.getEntityResult());
            }

            String response = getResponse(result);
            return Response.ok(response).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to partially update entity {} {}:{}.{}", entityJson, entityType, attribute, value, e);
            throw toWebApplicationException(e);
        } catch (AtlasException | IllegalArgumentException e) {
            LOG.error("Unable to partially update entity {} {}:{}.{}", entityJson, entityType, attribute, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to partially update entity {} {}:{}.{}", entityJson, entityType, attribute, value, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to partially update entity {} {}:{}.{}", entityJson, entityType, attribute, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.updateByUniqueAttribute({}, {}, {})", entityType, attribute, value);
            }
        }
    }

    /**
     * Updates entity identified by its GUID
     * Support Partial update of an entity - Adds/updates any new values specified
     * Does not support removal of attribute values
     *
     * @param guid
     * @param request The updated entity json
     * @return
     */
    @POST
    @Path("{guid}")
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response updateEntityByGuid(@PathParam("guid") String guid, @QueryParam("property") String attribute,
                                       @Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.updateEntityByGuid({}, {})", guid, attribute);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.updateEntityByGuid(" + guid + ", " + attribute + ")");
            }

            if (StringUtils.isEmpty(attribute)) {
                return partialUpdateEntityByGuid(guid, request);
            } else {
                return partialUpdateEntityAttrByGuid(guid, attribute, request);
            }
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.updateEntityByGuid({}, {})", guid, attribute);
            }
        }
    }

    private Response partialUpdateEntityByGuid(String guid, HttpServletRequest request) {
        String entityJson = null;
        try {
            guid = ParamChecker.notEmpty(guid, "Guid property cannot be null");
            entityJson = Servlets.getRequestPayload(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("partially updating entity for guid {} : {} ", guid, entityJson);
            }

            Referenceable updatedEntity = AtlasType.fromV1Json(entityJson, Referenceable.class);

            // update referenceable with Id if not specified in payload
            Id updateId = updatedEntity.getId();

            if (updateId != null && !AtlasTypeUtil.isAssignedGuid(updateId.getId())) {
                updatedEntity.setId(new Id(guid, 0, updatedEntity.getTypeName()));
            }

            AtlasEntitiesWithExtInfo   entitiesInfo     = restAdapters.toAtlasEntity(updatedEntity);
            EntityMutationResponse     mutationResponse = entitiesStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), true);
            CreateUpdateEntitiesResult result           = restAdapters.toCreateUpdateEntitiesResult(mutationResponse);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updated entities: {}", result.getEntityResult());
            }

            String response = getResponse(result);
            return Response.ok(response).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to update entity by GUID {} {} ", guid, entityJson, e);
            throw toWebApplicationException(e);
        } catch (AtlasException | IllegalArgumentException e) {
            LOG.error("Unable to update entity by GUID {} {}", guid, entityJson, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to update entity by GUID {} {} ", guid, entityJson, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to update entity by GUID {} {} ", guid, entityJson, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Supports Partial updates
     * Adds/Updates given entity specified by its GUID
     * Supports updation of only simple primitive attributes like strings, ints, floats, enums, class references and
     * does not support updation of complex types like arrays, maps
     * @param guid entity id
     * @param property property to add
     * @postbody property's value
     * @return response payload as json
     */
    private Response partialUpdateEntityAttrByGuid(String guid, String property, HttpServletRequest request) {
        String value = null;
        try {
            Preconditions.checkNotNull(property, "Entity property cannot be null");
            value = Servlets.getRequestPayload(request);
            Preconditions.checkNotNull(value, "Entity value cannot be null");

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updating entity {} for property {} = {}", guid, property, value);
            }

            EntityMutationResponse     mutationResponse = entitiesStore.updateEntityAttributeByGuid(guid, property, value);
            CreateUpdateEntitiesResult result           = restAdapters.toCreateUpdateEntitiesResult(mutationResponse);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Updated entities: {}", result.getEntityResult());
            }

            String response = getResponse(result);
            return Response.ok(response).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to add property {} to entity id {} {} ", property, guid, value, e);
            throw toWebApplicationException(e);
        } catch (AtlasException | IllegalArgumentException e) {
            LOG.error("Unable to add property {} to entity id {} {} ", property, guid, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to add property {} to entity id {} {} ", property, guid, value, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to add property {} to entity id {} {} ", property, guid, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * Delete entities from the repository identified by their guids (including their composite references)
     * or
     * Deletes a single entity identified by its type and unique attribute value from the repository (including their composite references)
     *
     * @param guids list of deletion candidate guids
     *              or
     * @param entityType the entity type
     * @param attribute the unique attribute used to identify the entity
     * @param value the unique attribute value used to identify the entity
     * @return response payload as json - including guids of entities(including composite references from that entity) that were deleted
     */
    @DELETE
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response deleteEntities(@QueryParam("guid") List<String> guids,
                                   @QueryParam("type") String entityType,
                                   @QueryParam("property") final String attribute,
                                   @QueryParam("value") final String value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.deleteEntities({}, {}, {}, {})", guids, entityType, attribute, value);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.deleteEntities(" + guids + ", " + entityType + ", " + attribute + ", " + value + ")");
            }

            EntityResult entityResult;

            if (guids != null && !guids.isEmpty()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleting entities {}", guids);
                }

                EntityMutationResponse mutationResponse = entityREST.deleteByGuids(guids);
                entityResult = restAdapters.toCreateUpdateEntitiesResult(mutationResponse).getEntityResult();
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleting entity type={} with property {}={}", entityType, attribute, value);
                }

                Map<String, Object> attributes = new HashMap<>();
                attributes.put(attribute, value);

                EntityMutationResponse mutationResponse = entitiesStore.deleteByUniqueAttributes(getEntityType(entityType), attributes);
                entityResult = restAdapters.toCreateUpdateEntitiesResult(mutationResponse).getEntityResult();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleted entity result: {}", entityResult);
            }

            String response = getResponse(entityResult);
            return Response.ok(response).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to delete entities {} {} {} {} ", guids, entityType, attribute, value, e);
            throw toWebApplicationException(e);
        }  catch (AtlasException | IllegalArgumentException e) {
            LOG.error("Unable to delete entities {} {} {} {} ", guids, entityType, attribute, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to delete entities {} {} {} {} ", guids, entityType, attribute, value, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to delete entities {} {} {} {} ", guids, entityType, attribute, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.deleteEntities({}, {}, {}, {})", guids, entityType, attribute, value);
            }
        }
    }

    /**
     * Fetch the complete definition of an entity given its GUID.
     *
     * @param guid GUID for the entity
     */
    @GET
    @Path("{guid}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getEntityDefinition(@PathParam("guid") String guid) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.getEntityDefinition({})", guid);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.getEntityDefinition(" + guid + ")");
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching entity definition for guid={} ", guid);
            }

            guid = ParamChecker.notEmpty(guid, "guid cannot be null");

            Referenceable entity = getEntity(guid);

            Map<String, Object> response = new HashMap<>();

            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());

            Response.Status status = Response.Status.NOT_FOUND;
            if (entity != null) {
                response.put(AtlasClient.DEFINITION, entity);
                status = Response.Status.OK;
            } else {
                response.put(AtlasClient.ERROR,
                        Servlets.escapeJsonString(String.format("An entity with GUID={%s} does not exist", guid)));
            }

            return Response.status(status).entity(AtlasJson.toV1Json(response)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Bad GUID={} ", guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (AtlasBaseException e) {
            LOG.error("Unable to get instance definition for GUID {}", guid, e);
            throw toWebApplicationException(e);
        } catch (WebApplicationException e) {
            LOG.error("Unable to get instance definition for GUID {}", guid, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get instance definition for GUID {}", guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.getEntityDefinition({})", guid);
            }
        }
    }

    /**
     * Gets the list of entities for a given entity type.
     *
     * @param entityType name of a type which is unique
     */
    public Response getEntityListByType(String entityType) {
        try {
            Preconditions.checkNotNull(entityType, "Entity type cannot be null");

            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching entity list for type={} ", entityType);
            }

            List<String> entityGUIDS = entitiesStore.getEntityGUIDS(entityType);

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.TYPENAME, entityType);
            response.put(AtlasClient.RESULTS, entityGUIDS);
            response.put(AtlasClient.COUNT, entityGUIDS.size());

            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (NullPointerException e) {
            LOG.error("Entity type cannot be null", e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get entity list for type {}", entityType, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get entity list for type {}", entityType, e);
            throw e;
        } catch (AtlasBaseException e) {
            LOG.error("Unable to get entity list for type {}", entityType, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (Throwable e) {
            LOG.error("Unable to get entity list for type {}", entityType, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @GET
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getEntity(@QueryParam("type") String entityType,
                              @QueryParam("property") String attribute,
                              @QueryParam("value") final String value) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.getEntity({}, {}, {})", entityType, attribute, value);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.getEntity(" + entityType + ", " + attribute + ", " + value + ")");
            }

            if (StringUtils.isEmpty(attribute)) {
                //List API
                return getEntityListByType(entityType);
            } else {
                //Get entity by unique attribute
                return getEntityDefinitionByAttribute(entityType, attribute, value);
            }
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.getEntity({}, {}, {})", entityType, attribute, value);
            }
        }
    }

    /**
     * Fetch the complete definition of an entity given its qualified name.
     *
     * @param entityType
     * @param attribute
     * @param value
     */
    public Response getEntityDefinitionByAttribute(String entityType, String attribute, String value) {
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching entity definition for type={}, qualified name={}", entityType, value);
            }

            entityType = ParamChecker.notEmpty(entityType, "Entity type cannot be null");
            attribute  = ParamChecker.notEmpty(attribute, "attribute name cannot be null");
            value      = ParamChecker.notEmpty(value, "attribute value cannot be null");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put(attribute, value);

            AtlasEntityWithExtInfo entityInfo;

            try {
                entityInfo = entitiesStore.getByUniqueAttributes(getEntityType(entityType), attributes);
            } catch (AtlasBaseException e) {
                LOG.error("Cannot find entity with type: {}, attribute: {} and value: {}", entityType, attribute, value);
                throw toWebApplicationException(e);
            }

            Referenceable entity = null;

            if (entityInfo != null) {
                entity = restAdapters.getReferenceable(entityInfo);
            }

            Map<String, Object> response = new HashMap<>();

            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());

            Response.Status status = Response.Status.NOT_FOUND;
            if (entity != null) {
                response.put(AtlasClient.DEFINITION, entity);
                status = Response.Status.OK;
            } else {
                response.put(AtlasClient.ERROR, Servlets.escapeJsonString(String.format("An entity with type={%s}, " +
                        "qualifiedName={%s} does not exist", entityType, value)));
            }

            return Response.status(status).entity(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to get instance definition for type={}, qualifiedName={}", entityType, value, e);
            throw toWebApplicationException(e);
        } catch (IllegalArgumentException e) {
            LOG.error("Bad type={}, qualifiedName={}", entityType, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get instance definition for type={}, qualifiedName={}", entityType, value, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get instance definition for type={}, qualifiedName={}", entityType, value, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        }
    }


    // Trait management functions

    /**
     * Gets the list of trait names for a given entity represented by a guid.
     *
     * @param guid globally unique identifier for the entity
     * @return a list of trait names for the given entity guid
     */
    @GET
    @Path("{guid}/traits")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getTraitNames(@PathParam("guid") String guid) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.getTraitNames({})", guid);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.getTraitNames(" + guid + ")");
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching trait names for entity={}", guid);
            }

            final List<AtlasClassification> classifications = entitiesStore.getClassifications(guid);

            List<String> traitNames = new ArrayList<>();
            for (AtlasClassification classification : classifications) {
                traitNames.add(classification.getTypeName());
            }

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.RESULTS, traitNames);
            response.put(AtlasClient.COUNT, traitNames.size());

            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to get trait definition for entity {}", guid, e);
            throw toWebApplicationException(e);
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get trait definition for entity {}", guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get trait names for entity {}", guid, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get trait names for entity {}", guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.getTraitNames({})", guid);
            }
        }
    }

    /**
     * Fetches the trait definitions of all the traits associated to the given entity
     * @param guid globally unique identifier for the entity
     */
    @GET
    @Path("{guid}/traitDefinitions")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getTraitDefinitionsForEntity(@PathParam("guid") String guid){
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.getTraitDefinitionsForEntity({})", guid);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.getTraitDefinitionsForEntity(" + guid + ")");
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching all trait definitions for entity={}", guid);
            }

            final List<AtlasClassification> classifications = entitiesStore.getClassifications(guid);

            List<Struct> traits = new ArrayList<>(classifications.size());
            for (AtlasClassification classification : classifications) {
                Struct trait = restAdapters.getTrait(classification);
                traits.add(trait);
            }

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.RESULTS, traits);
            response.put(AtlasClient.COUNT, traits.size());

            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to get trait definition for entity {}", guid, e);
            throw toWebApplicationException(e);
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get trait definition for entity {}", guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get trait definitions for entity {}", guid, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get trait definitions for entity {}", guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.getTraitDefinitionsForEntity({})", guid);
            }
        }
    }

    /**
     * Fetches the trait definition for an entity given its guid and trait name
     *
     * @param guid globally unique identifier for the entity
     * @param traitName name of the trait
     */
    @GET
    @Path("{guid}/traitDefinitions/{traitName}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getTraitDefinitionForEntity(@PathParam("guid") String guid, @PathParam("traitName") String traitName){
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.getTraitDefinitionForEntity({}, {})", guid, traitName);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.getTraitDefinitionForEntity(" + guid + ", " + traitName + ")");
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Fetching trait definition for entity {} and trait name {}", guid, traitName);
            }


            final AtlasClassification classification = entitiesStore.getClassification(guid, traitName);

            Struct traitDefinition = restAdapters.getTrait(classification);

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.RESULTS, traitDefinition);

            return Response.ok(AtlasJson.toV1Json(response)).build();

        } catch (AtlasBaseException e) {
            LOG.error("Unable to get trait definition for entity {} and trait {}", guid, traitName, e);
            throw toWebApplicationException(e);
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get trait definition for entity {} and trait {}", guid, traitName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get trait definition for entity {} and trait {}", guid, traitName, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get trait definition for entity {} and trait {}", guid, traitName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.getTraitDefinitionForEntity({}, {})", guid, traitName);
            }
        }
    }

    /**
     * Adds a new trait to an existing entity represented by a guid.
     *
     * @param guid globally unique identifier for the entity
     */
    @POST
    @Path("{guid}/traits")
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response addTrait(@Context HttpServletRequest request, @PathParam("guid") final String guid) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.addTrait({})", guid);
        }

        String traitDefinition = null;
        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.addTrait(" + guid + ")");
            }

            traitDefinition = Servlets.getRequestPayload(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding trait={} for entity={} ", traitDefinition, guid);
            }

            List<String> guids = new ArrayList<String>() {{
                add(guid);
            }};

            entitiesStore.addClassification(guids, restAdapters.toAtlasClassification(AtlasType.fromV1Json(traitDefinition, Struct.class)));

            URI locationURI = getLocationURI(new ArrayList<String>() {{
                add(guid);
            }});

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());

            return Response.created(locationURI).entity(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to add trait for entity={} traitDef={}", guid, traitDefinition, e);
            throw toWebApplicationException(e);
        } catch  (IllegalArgumentException e) {
            LOG.error("Unable to add trait for entity={} traitDef={}", guid, traitDefinition, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to add trait for entity={} traitDef={}", guid, traitDefinition, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to add trait for entity={} traitDef={}", guid, traitDefinition, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.addTrait({})", guid);
            }
        }
    }

    /**
     * Deletes a given trait from an existing entity represented by a guid.
     *
     * @param guid      globally unique identifier for the entity
     * @param traitName name of the trait
     */
    @DELETE
    @Path("{guid}/traits/{traitName}")
    @Consumes({Servlets.JSON_MEDIA_TYPE, MediaType.APPLICATION_JSON})
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response deleteTrait(@Context HttpServletRequest request, @PathParam("guid") String guid,
            @PathParam(TRAIT_NAME) final String traitName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.deleteTrait({}, {})", guid, traitName);
        }

        AtlasPerfTracer perf = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting trait={} from entity={} ", traitName, guid);
        }

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.deleteTrait(" + guid + ", " + traitName + ")");
            }

            entitiesStore.deleteClassification(guid, traitName);

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(TRAIT_NAME, traitName);

            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (AtlasBaseException e) {
            LOG.error("Unable to delete trait name={} for entity={}", traitName, guid, e);
            throw toWebApplicationException(e);
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to delete trait name={} for entity={}", traitName, guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to delete trait name={} for entity={}", traitName, guid, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to delete trait name={} for entity={}", traitName, guid, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.deleteTrait({}, {})", guid, traitName);
            }
        }
    }

    /**
     * Returns the entity audit events for a given entity id. The events are returned in the decreasing order of timestamp.
     * @param guid entity id
     * @param startKey used for pagination. Startkey is inclusive, the returned results contain the event with the given startkey.
     *                  First time getAuditEvents() is called for an entity, startKey should be null,
     *                  with count = (number of events required + 1). Next time getAuditEvents() is called for the same entity,
     *                  startKey should be equal to the entityKey of the last event returned in the previous call.
     * @param count number of events required
     * @return
     */
    @GET
    @Path("{guid}/audit")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getAuditEvents(@PathParam("guid") String guid, @QueryParam("startKey") String startKey,
                                   @QueryParam("count") @DefaultValue("100") short count) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> EntityResource.getAuditEvents({}, {}, {})", guid, startKey, count);
        }

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "EntityResource.getAuditEvents(" + guid + ", " + startKey + ", " + count + ")");
            }

            List                   events   = entityAuditRepository.listEvents(guid, startKey, count);
            List<EntityAuditEvent> v1Events = new ArrayList<>();

            for (Object event : events) {
                if (event instanceof EntityAuditEvent) {
                    v1Events.add((EntityAuditEvent) event);
                } else if (event instanceof EntityAuditEventV2) {
                    v1Events.add(instanceConverter.toV1AuditEvent((EntityAuditEventV2) event));
                } else {
                    LOG.warn("unknown entity-audit event type {}. Ignored", event != null ? event.getClass().getCanonicalName() : "null");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put(AtlasClient.REQUEST_ID, Servlets.getRequestId());
            response.put(AtlasClient.EVENTS, v1Events);
            return Response.ok(AtlasJson.toV1Json(response)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get audit events for entity guid={} startKey={}", guid, startKey, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get audit events for entity guid={} startKey={}", guid, startKey, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get audit events for entity guid={} startKey={}", guid, startKey, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== EntityResource.getAuditEvents({}, {}, {})", guid, startKey, count);
            }
        }
    }

    private AtlasEntityType getEntityType(String typeName) throws AtlasBaseException {
        AtlasEntityType ret = typeRegistry.getEntityTypeByName(typeName);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, typeName);
        }

        return ret;
    }

    public static WebApplicationException toWebApplicationException(AtlasBaseException e) {
        if (e.getAtlasErrorCode() == AtlasErrorCode.CLASSIFICATION_NOT_FOUND
            || e.getAtlasErrorCode() == AtlasErrorCode.INSTANCE_GUID_NOT_FOUND
            || e.getAtlasErrorCode() == AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND) {
            return new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.NOT_FOUND));
        }

        if (e.getAtlasErrorCode() == AtlasErrorCode.INVALID_PARAMETERS
            || e.getAtlasErrorCode() == AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS) {
            return new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        }

        return new WebApplicationException(Servlets.getErrorResponse(e, e.getAtlasErrorCode().getHttpCode()));
    }

    private Referenceable getEntity(String guid) throws AtlasBaseException {
        AtlasEntityWithExtInfo entity        = entitiesStore.getById(guid);
        Referenceable          referenceable = restAdapters.getReferenceable(entity);

        return referenceable;
    }
}
