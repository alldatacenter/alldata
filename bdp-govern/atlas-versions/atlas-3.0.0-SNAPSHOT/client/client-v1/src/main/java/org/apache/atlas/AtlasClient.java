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

package org.apache.atlas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.WebResource;
import org.apache.atlas.model.legacy.EntityResult;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.typedef.AttributeDefinition;
import org.apache.atlas.v1.model.typedef.TraitTypeDefinition;
import org.apache.atlas.v1.model.typedef.TypesDef;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * Client for metadata.
 */
@Deprecated
public class AtlasClient extends AtlasBaseClient {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasClient.class);

    public static final String TYPE             = "type";
    public static final String TYPENAME         = "typeName";
    public static final String GUID             = "GUID";
    public static final String ENTITIES         = "entities";
    public static final String GUID_ASSIGNMENTS = "guidAssignments";

    public static final String DEFINITION = "definition";
    public static final String ERROR      = "error";
    public static final String STACKTRACE = "stackTrace";
    public static final String REQUEST_ID = "requestId";
    public static final String RESULTS    = "results";
    public static final String COUNT      = "count";
    public static final String ROWS       = "rows";
    public static final String DATATYPE   = "dataType";
    public static final String STATUS     = "Status";

    public static final String EVENTS      = "events";
    public static final String START_KEY   = "startKey";
    public static final String NUM_RESULTS = "count";

    public static final String URI_ENTITY        = "entities";
    public static final String URI_ENTITY_AUDIT  = "audit";
    public static final String URI_SEARCH        = "discovery/search";
    public static final String URI_NAME_LINEAGE  = "lineage/hive/table";
    public static final String URI_LINEAGE       = "lineage/";
    public static final String URI_TRAITS        = "traits";
    public static final String TRAITS            = "traits";
    public static final String TRAIT_DEFINITIONS = "traitDefinitions";


    public static final String QUERY_TYPE      = "queryType";
    public static final String ATTRIBUTE_NAME  = "property";
    public static final String ATTRIBUTE_VALUE = "value";

    public static final String SUPERTYPE     = "supertype";
    public static final String NOT_SUPERTYPE = "notsupertype";

    public static final String ASSET_TYPE  = "Asset";
    public static final String NAME        = "name";
    public static final String DESCRIPTION = "description";
    public static final String OWNER       = "owner";
    public static final String CREATE_TIME = "createTime";

    public static final String INFRASTRUCTURE_SUPER_TYPE = "Infrastructure";
    public static final String DATA_SET_SUPER_TYPE       = "DataSet";
    public static final String PROCESS_SUPER_TYPE        = "Process";
    public static final String PROCESS_ATTRIBUTE_INPUTS  = "inputs";
    public static final String PROCESS_ATTRIBUTE_OUTPUTS = "outputs";

    public static final String REFERENCEABLE_SUPER_TYPE     = "Referenceable";
    public static final String QUALIFIED_NAME               = "qualifiedName";
    public static final String REFERENCEABLE_ATTRIBUTE_NAME = QUALIFIED_NAME;

    public static final String UNKNOWN_STATUS = "Unknown status";

    /**
     * Constructor for AtlasClient with cookie params as header
     * @param baseUrl
     * @param cookieName
     * @param value
     * @param path
     * @param domain
     */

    public AtlasClient(String[] baseUrl, String cookieName, String value, String path, String domain) {
        super(baseUrl, new Cookie(cookieName, value, path, domain));
    }

    /**
     * Constructor for AtlasClient with cookie as header
     * @param baseUrl
     * @param cookie
     */

    public AtlasClient(String[] baseUrl, Cookie cookie) {
        super(baseUrl, cookie);
    }


    // New constructor for Basic auth
    public AtlasClient(String[] baseUrl, String[] basicAuthUserNamePassword) {
        super(baseUrl, basicAuthUserNamePassword);
    }

    /**
     * Create a new Atlas client.
     * @param baseUrls A list of URLs that point to an ensemble of Atlas servers working in
     *                 High Availability mode. The client will automatically determine the
     *                 active instance on startup and also when there is a scenario of
     *                 failover.
     */
    public AtlasClient(String... baseUrls) throws AtlasException {
        this(getCurrentUGI(), baseUrls);
    }

    /**
     * Create a new Atlas client.
     * @param ugi UserGroupInformation
     * @param doAsUser
     * @param baseUrls A list of URLs that point to an ensemble of Atlas servers working in
     *                 High Availability mode. The client will automatically determine the
     *                 active instance on startup and also when there is a scenario of
     *                 failover.
     */
    public AtlasClient(UserGroupInformation ugi, String doAsUser, String... baseUrls) {
        initializeState(baseUrls, ugi, doAsUser);
    }

    private AtlasClient(UserGroupInformation ugi, String[] baseUrls) {
        this(ugi, ugi.getShortUserName(), baseUrls);
    }

    //Used by LocalAtlasClient
    protected AtlasClient() {
        //Do nothing
    }

    @VisibleForTesting
    public AtlasClient(Configuration configuration, String[] baseUrl, String[] basicAuthUserNamePassword) {
        super(configuration, baseUrl, basicAuthUserNamePassword);
    }

    @Override
    protected API formatPathParameters(final API api, final String... params) {
        return new API(String.format(api.getPath(), params), api.getMethod(), api.getExpectedStatus());
    }

    @VisibleForTesting
    public AtlasClient(Configuration configuration, String... baseUrls) throws AtlasException {
        initializeState(configuration, baseUrls, getCurrentUGI(), getCurrentUGI().getShortUserName());
    }

    @VisibleForTesting
    AtlasClient(WebResource service, Configuration configuration) {
        super(service, configuration);
    }

    public WebResource getResource() {
        return service;
    }

    public static class API_V1 extends API {
        //Admin operations
        public static final API_V1 VERSION = new API_V1(BASE_URI + ADMIN_VERSION, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 STATUS  = new API_V1(BASE_URI + ADMIN_STATUS, HttpMethod.GET, Response.Status.OK);

        //Type operations
        public static final API_V1 CREATE_TYPE      = new API_V1(BASE_URI + TYPES, HttpMethod.POST, Response.Status.CREATED);
        public static final API_V1 UPDATE_TYPE      = new API_V1(BASE_URI + TYPES, HttpMethod.PUT, Response.Status.OK);
        public static final API_V1 GET_TYPE         = new API_V1(BASE_URI + TYPES, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 LIST_TYPES       = new API_V1(BASE_URI + TYPES, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 LIST_TRAIT_TYPES = new API_V1(BASE_URI + TYPES + "?type=trait", HttpMethod.GET, Response.Status.OK);

        //Entity operations
        public static final API_V1 CREATE_ENTITY         = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.POST, Response.Status.CREATED);
        public static final API_V1 GET_ENTITY            = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 UPDATE_ENTITY         = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.PUT, Response.Status.OK);
        public static final API_V1 UPDATE_ENTITY_PARTIAL = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.POST, Response.Status.OK);
        public static final API_V1 LIST_ENTITIES         = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 DELETE_ENTITIES       = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.DELETE, Response.Status.OK);
        public static final API_V1 DELETE_ENTITY         = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.DELETE, Response.Status.OK);

        //audit operation
        public static final API_V1 LIST_ENTITY_AUDIT = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.GET, Response.Status.OK);

        //Trait operations
        public static final API_V1 ADD_TRAITS                = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.POST, Response.Status.CREATED);
        public static final API_V1 DELETE_TRAITS             = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.DELETE, Response.Status.OK);
        public static final API_V1 LIST_TRAITS               = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 GET_ALL_TRAIT_DEFINITIONS = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 GET_TRAIT_DEFINITION      = new API_V1(BASE_URI + URI_ENTITY, HttpMethod.GET, Response.Status.OK);

        //Search operations
        public static final API_V1 SEARCH           = new API_V1(BASE_URI + URI_SEARCH, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 SEARCH_DSL       = new API_V1(BASE_URI + URI_SEARCH + "/dsl", HttpMethod.GET, Response.Status.OK);
        public static final API_V1 SEARCH_FULL_TEXT = new API_V1(BASE_URI + URI_SEARCH + "/fulltext", HttpMethod.GET, Response.Status.OK);
        public static final API_V1 GREMLIN_SEARCH   = new API_V1(BASE_URI + URI_SEARCH + "/gremlin", HttpMethod.GET, Response.Status.OK);

        //Lineage operations based on dataset name
        public static final API_V1 NAME_LINEAGE_INPUTS_GRAPH  = new API_V1(BASE_URI + URI_NAME_LINEAGE, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 NAME_LINEAGE_OUTPUTS_GRAPH = new API_V1(BASE_URI + URI_NAME_LINEAGE, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 NAME_LINEAGE_SCHEMA        = new API_V1(BASE_URI + URI_NAME_LINEAGE, HttpMethod.GET, Response.Status.OK);

        //Lineage operations based on entity id of the dataset
        public static final API_V1 LINEAGE_INPUTS_GRAPH  = new API_V1(BASE_URI + URI_LINEAGE, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 LINEAGE_OUTPUTS_GRAPH = new API_V1(BASE_URI + URI_LINEAGE, HttpMethod.GET, Response.Status.OK);
        public static final API_V1 LINEAGE_SCHEMA        = new API_V1(BASE_URI + URI_LINEAGE, HttpMethod.GET, Response.Status.OK);

        private API_V1(String path, String method, Response.Status status) {
            super(path, method, status);
        }
    }

    /**
     * Register the given type(meta model)
     * @param typeAsJson type definition a jaon
     * @return result json object
     * @throws AtlasServiceException
     */
    public List<String> createType(String typeAsJson) throws AtlasServiceException {
        LOG.debug("Creating type definition: {}", typeAsJson);
        ObjectNode response = callAPIWithBody(API_V1.CREATE_TYPE, typeAsJson);
        List<String> results = extractResults(response, AtlasClient.TYPES, new ExtractOperation<String, ObjectNode>() {
            @Override
            String extractElement(ObjectNode element) {
                return element.get(AtlasClient.NAME).asText();
            }
        });
        LOG.debug("Create type definition returned results: {}", results);
        return results;
    }

    /**
     * Register the given type(meta model)
     * @param typeDef type definition
     * @return result json object
     * @throws AtlasServiceException
     */
    public List<String> createType(TypesDef typeDef) throws AtlasServiceException {
        return createType(AtlasType.toV1Json(typeDef));
    }

    /**
     * Creates trait type with specifiedName, superTraits and attributes
     * @param traitName the name of the trait type
     * @param superTraits the list of super traits from which this trait type inherits attributes
     * @param attributeDefinitions the list of attributes of the trait type
     * @return the list of types created
     * @throws AtlasServiceException
     */
    public List<String> createTraitType(String traitName, Set<String> superTraits, AttributeDefinition... attributeDefinitions) throws AtlasServiceException {
        TraitTypeDefinition piiTrait = TypesUtil.createTraitTypeDef(traitName, null, superTraits, Arrays.asList(attributeDefinitions));
        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait),
                Collections.emptyList());

        LOG.debug("Creating trait type {} {}", traitName, AtlasType.toV1Json(piiTrait));

        return createType(AtlasType.toV1Json(typesDef));
    }

    /**
     * Creates simple trait type with specifiedName with no superTraits or attributes
     * @param traitName the name of the trait type
     * @return the list of types created
     * @throws AtlasServiceException
     */
    public List<String> createTraitType(String traitName) throws AtlasServiceException {
        return createTraitType(traitName, null);
    }

    /**
     * Register the given type(meta model)
     * @param typeAsJson type definition a jaon
     * @return result json object
     * @throws AtlasServiceException
     */
    public List<String> updateType(String typeAsJson) throws AtlasServiceException {
        LOG.debug("Updating type definition: {}", typeAsJson);
        ObjectNode response = callAPIWithBody(API_V1.UPDATE_TYPE, typeAsJson);
        List<String> results = extractResults(response, AtlasClient.TYPES, new ExtractOperation<String, ObjectNode>() {
            @Override
            String extractElement(ObjectNode element) {
                return element.get(AtlasClient.NAME).asText();
            }
        });
        LOG.debug("Update type definition returned results: {}", results);
        return results;
    }

    /**
     * Register the given type(meta model)
     * @param typeDef type definition
     * @return result json object
     * @throws AtlasServiceException
     */
    public List<String> updateType(TypesDef typeDef) throws AtlasServiceException {
        return updateType(AtlasType.toV1Json(typeDef));
    }

    /**
     * Returns all type names in the system
     * @return list of type names
     * @throws AtlasServiceException
     */
    public List<String> listTypes() throws AtlasServiceException {
        final ObjectNode jsonResponse = callAPIWithQueryParams(API_V1.LIST_TYPES, null);
        return extractStringList(jsonResponse);
    }

    /**
     * Returns all type names with the given category
     * @param category
     * @return list of type names
     * @throws AtlasServiceException
     */
    public List<String> listTypes(final DataTypes.TypeCategory category) throws AtlasServiceException {
        final API api = API_V1.LIST_TYPES;
        ObjectNode response = callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api.getNormalizedPath());
                resource = resource.queryParam(TYPE, category.name());
                return resource;
            }
        });
        return extractResults(response, AtlasClient.RESULTS, new ExtractOperation<String, String>());
    }

    /**
     * Return the list of type names in the type system which match the specified filter.
     *
     * @param category returns types whose category is the given typeCategory
     * @param superType returns types which contain the given supertype
     * @param notSupertype returns types which do not contain the given supertype
     *
     * Its possible to specify combination of these filters in one request and the conditions are combined with AND
     * For example, typeCategory = TRAIT && supertype contains 'X' && supertype !contains 'Y'
     * If there is no filter, all the types are returned
     * @return list of type names
     */
    public List<String> listTypes(final DataTypes.TypeCategory category, final String superType,
                                  final String notSupertype) throws AtlasServiceException {
        final API api = API_V1.LIST_TYPES;
        ObjectNode response = callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api);
                resource = resource.queryParam(TYPE, category.name());
                resource = resource.queryParam(SUPERTYPE, superType);
                resource = resource.queryParam(NOT_SUPERTYPE, notSupertype);
                return resource;
            }
        });
        return extractStringList(response);
    }

    public TypesDef getType(String typeName) throws AtlasServiceException {
        ObjectNode response = callAPIWithBodyAndParams(API_V1.GET_TYPE, null, typeName);
        String     typeJson = AtlasType.toJson(response.get(DEFINITION));
        return AtlasType.fromV1Json(typeJson, TypesDef.class);
    }

    /**
     * Create the given entity
     * @param entities entity(type instance) as json
     * @return json array of guids
     * @throws AtlasServiceException
     */
    protected List<String> createEntity(ArrayNode entities) throws AtlasServiceException {
        LOG.debug("Creating entities: {}", entities);
        ObjectNode   response = callAPIWithBody(API_V1.CREATE_ENTITY, entities.toString());
        List<String> results  = extractEntityResult(response).getCreatedEntities();
        LOG.debug("Create entities returned results: {}", results);
        return results;
    }

    protected EntityResult extractEntityResult(ObjectNode response) throws AtlasServiceException {
        return EntityResult.fromString(response.toString());
    }

    /**
     * Create the given entity
     * @param entitiesAsJson entity(type instance) as json
     * @return json array of guids
     * @throws AtlasServiceException
     */
    public List<String> createEntity(String... entitiesAsJson) throws AtlasServiceException {
        try {
            return createEntity(AtlasJson.parseToV1ArrayNode(Arrays.asList(entitiesAsJson)));
        } catch (IOException excp) {
            throw new AtlasServiceException(excp);
        }
    }

    public List<String> createEntity(Referenceable... entities) throws AtlasServiceException {
        return createEntity(Arrays.asList(entities));
    }

    public List<String> createEntity(Collection<Referenceable> entities) throws AtlasServiceException {
        ArrayNode entityArray = getEntitiesArray(entities);
        return createEntity(entityArray);
    }

    private ArrayNode getEntitiesArray(Collection<Referenceable> entities) {
        ArrayNode entityArray = AtlasJson.createV1ArrayNode();
        for (Referenceable entity : entities) {
            entityArray.add(AtlasType.toV1Json(entity));
        }
        return entityArray;
    }

    /**
     * Replaces entity definitions identified by their guid or unique attribute
     * Updates properties set in the definition for the entity corresponding to guid
     * @param entities entities to be updated
     * @return json array of guids which were updated/created
     * @throws AtlasServiceException
     */
    public EntityResult updateEntities(Referenceable... entities) throws AtlasServiceException {
        return updateEntities(Arrays.asList(entities));
    }

    protected EntityResult updateEntities(ArrayNode entities) throws AtlasServiceException {
        LOG.debug("Updating entities: {}", entities);
        ObjectNode   response = callAPIWithBody(API_V1.UPDATE_ENTITY, entities.toString());
        EntityResult results  = extractEntityResult(response);
        LOG.debug("Update entities returned results: {}", results);
        return results;
    }

    public EntityResult updateEntities(Collection<Referenceable> entities) throws AtlasServiceException {
        ArrayNode entitiesArray = getEntitiesArray(entities);
        return updateEntities(entitiesArray);
    }

    /**
     * Supports Partial updates
     * Updates property for the entity corresponding to guid
     * @param guid      guid
     * @param attribute  property key
     * @param value     property value
     */
    public EntityResult updateEntityAttribute(final String guid, final String attribute, String value)
            throws AtlasServiceException {
        LOG.debug("Updating entity id: {}, attribute name: {}, attribute value: {}", guid, attribute, value);
        final API api = API_V1.UPDATE_ENTITY_PARTIAL;
        ObjectNode response = callAPIWithRetries(api, value, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api, guid);
                resource = resource.queryParam(ATTRIBUTE_NAME, attribute);
                return resource;
            }
        });
        return extractEntityResult(response);
    }

    /**
     * Supports Partial updates
     * Updates properties set in the definition for the entity corresponding to guid
     * @param guid      guid
     * @param entity entity definition
     */
    public EntityResult updateEntity(String guid, Referenceable entity) throws AtlasServiceException {
        String entityJson = AtlasType.toV1Json(entity);
        LOG.debug("Updating entity id {} with {}", guid, entityJson);
        ObjectNode response = callAPIWithBodyAndParams(API_V1.UPDATE_ENTITY_PARTIAL, entityJson, guid);
        return extractEntityResult(response);
    }

    /**
     * Associate trait to an entity
     *
     * @param guid      guid
     * @param traitDefinition trait definition
     */
    public void addTrait(String guid, Struct traitDefinition) throws AtlasServiceException {
        String traitJson = AtlasType.toV1Json(traitDefinition);
        LOG.debug("Adding trait to entity with id {} {}", guid, traitJson);
        callAPIWithBodyAndParams(API_V1.ADD_TRAITS, traitJson, guid, URI_TRAITS);
    }

    /**
     * Delete a trait from the given entity
     * @param guid guid of the entity
     * @param traitName trait to be deleted
     * @throws AtlasServiceException
     */
    public void deleteTrait(String guid, String traitName) throws AtlasServiceException {
        callAPIWithBodyAndParams(API_V1.DELETE_TRAITS, null, guid, TRAITS, traitName);
    }

    /**
     * Supports Partial updates
     * Updates properties set in the definition for the entity corresponding to guid
     * @param entityType Type of the entity being updated
     * @param uniqueAttributeName Attribute Name that uniquely identifies the entity
     * @param uniqueAttributeValue Attribute Value that uniquely identifies the entity
     * @param entity entity definition
     */
    public EntityResult updateEntity(final String entityType, final String uniqueAttributeName,
                                     final String uniqueAttributeValue,
                                     Referenceable entity) throws AtlasServiceException {
        final API api        = API_V1.UPDATE_ENTITY_PARTIAL;
        String    entityJson = AtlasType.toV1Json(entity);
        LOG.debug("Updating entity type: {}, attributeName: {}, attributeValue: {}, entity: {}", entityType,
                  uniqueAttributeName, uniqueAttributeValue, entityJson);
        ObjectNode response = callAPIWithRetries(api, entityJson, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api, QUALIFIED_NAME);
                resource = resource.queryParam(TYPE, entityType);
                resource = resource.queryParam(ATTRIBUTE_NAME, uniqueAttributeName);
                resource = resource.queryParam(ATTRIBUTE_VALUE, uniqueAttributeValue);
                return resource;
            }
        });
        EntityResult result = extractEntityResult(response);
        LOG.debug("Update entity returned result: {}", result);
        return result;
    }

    protected String getString(ObjectNode jsonObject, String parameter) throws AtlasServiceException {
        return jsonObject.get(parameter).asText();
    }

    /**
     * Delete the specified entities from the repository
     *
     * @param guids guids of entities to delete
     * @return List of entity ids updated/deleted
     * @throws AtlasServiceException
     */
    public EntityResult deleteEntities(final String... guids) throws AtlasServiceException {
        LOG.debug("Deleting entities: {}", guids);
        final API api = API_V1.DELETE_ENTITIES;
        ObjectNode jsonResponse = callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api);
                for (String guid : guids) {
                    resource = resource.queryParam(GUID.toLowerCase(), guid);
                }
                return resource;
            }
        });
        EntityResult results = extractEntityResult(jsonResponse);
        LOG.debug("Delete entities returned results: {}", results);
        return results;
    }

    /**
     * Supports Deletion of an entity identified by its unique attribute value
     * @param entityType Type of the entity being deleted
     * @param uniqueAttributeName Attribute Name that uniquely identifies the entity
     * @param uniqueAttributeValue Attribute Value that uniquely identifies the entity
     * @return List of entity ids updated/deleted(including composite references from that entity)
     */
    public EntityResult deleteEntity(String entityType, String uniqueAttributeName, String uniqueAttributeValue)
            throws AtlasServiceException {
        LOG.debug("Deleting entity type: {}, attributeName: {}, attributeValue: {}", entityType, uniqueAttributeName,
                  uniqueAttributeValue);
        API         api      = API_V1.DELETE_ENTITIES;
        WebResource resource = getResource(api);
        resource = resource.queryParam(TYPE, entityType);
        resource = resource.queryParam(ATTRIBUTE_NAME, uniqueAttributeName);
        resource = resource.queryParam(ATTRIBUTE_VALUE, uniqueAttributeValue);
        ObjectNode   jsonResponse = callAPIWithResource(api, resource);
        EntityResult results      = extractEntityResult(jsonResponse);
        LOG.debug("Delete entities returned results: {}", results);
        return results;
    }

    /**
     * Get an entity given the entity id
     * @param guid entity id
     * @return result object
     * @throws AtlasServiceException
     */
    public Referenceable getEntity(String guid) throws AtlasServiceException {
        ObjectNode jsonResponse = callAPIWithBodyAndParams(API_V1.GET_ENTITY, null, guid);
        String entityInstanceDefinition = AtlasType.toJson(jsonResponse.get(AtlasClient.DEFINITION));
        return AtlasType.fromV1Json(entityInstanceDefinition, Referenceable.class);
    }

    public static String toString(ArrayNode jsonArray) {
        ArrayList<String> resultsList = new ArrayList<>();
        for (int index = 0; index < jsonArray.size(); index++) {
            resultsList.add(jsonArray.get(index).asText());
        }
        return StringUtils.join(resultsList, ",");
    }

    /**
     * Get an entity given the entity id
     * @param entityType entity type name
     * @param attribute qualified name of the entity
     * @param value
     * @return result object
     * @throws AtlasServiceException
     */
    public Referenceable getEntity(final String entityType, final String attribute, final String value)
            throws AtlasServiceException {
        final API api = API_V1.GET_ENTITY;
        ObjectNode jsonResponse = callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api);
                resource = resource.queryParam(TYPE, entityType);
                resource = resource.queryParam(ATTRIBUTE_NAME, attribute);
                resource = resource.queryParam(ATTRIBUTE_VALUE, value);
                return resource;
            }
        });
        String entityInstanceDefinition =  AtlasType.toJson(jsonResponse.get(AtlasClient.DEFINITION));
        return AtlasType.fromV1Json(entityInstanceDefinition, Referenceable.class);
    }

    /**
     * List entities for a given entity type
     * @param entityType
     * @return
     * @throws AtlasServiceException
     */
    public List<String> listEntities(final String entityType) throws AtlasServiceException {
        ObjectNode jsonResponse = callAPIWithRetries(API_V1.LIST_ENTITIES, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(API_V1.LIST_ENTITIES);
                resource = resource.queryParam(TYPE, entityType);
                return resource;
            }
        });
        return extractStringList(jsonResponse);
    }

    /**
     * List traits for a given entity identified by its GUID
     * @param guid GUID of the entity
     * @return List<String> - traitnames associated with entity
     * @throws AtlasServiceException
     */
    public List<String> listTraits(final String guid) throws AtlasServiceException {
        ObjectNode jsonResponse = callAPIWithBodyAndParams(API_V1.LIST_TRAITS, null, guid, URI_TRAITS);
        return extractStringList(jsonResponse);
    }

    /**
     * Get all trait definitions for an entity
     * @param guid GUID of the entity
     * @return List<String> trait definitions of the traits associated to the entity
     * @throws AtlasServiceException
     */
    public List<Struct> listTraitDefinitions(final String guid) throws AtlasServiceException {
        ObjectNode        jsonResponse    = callAPIWithBodyAndParams(API_V1.GET_ALL_TRAIT_DEFINITIONS, null, guid, TRAIT_DEFINITIONS);
        List<ObjectNode>  traitDefList    = extractResults(jsonResponse, AtlasClient.RESULTS, new ExtractOperation<ObjectNode, ObjectNode>());
        ArrayList<Struct> traitStructList = new ArrayList<>();
        for (ObjectNode traitDef : traitDefList) {
            Struct traitStruct = AtlasType.fromV1Json(traitDef.toString(), Struct.class);
            traitStructList.add(traitStruct);
        }
        return traitStructList;
    }

    /**
     * Get trait definition for a given entity and traitname
     * @param guid GUID of the entity
     * @param traitName
     * @return trait definition
     * @throws AtlasServiceException
     */
    public Struct getTraitDefinition(final String guid, final String traitName) throws AtlasServiceException {
        ObjectNode jsonResponse = callAPIWithBodyAndParams(API_V1.GET_TRAIT_DEFINITION, null, guid, TRAIT_DEFINITIONS, traitName);

        return AtlasType.fromV1Json(AtlasType.toJson(jsonResponse.get(AtlasClient.RESULTS)), Struct.class);
    }

    protected class ExtractOperation<T, U> {
        T extractElement(U element) {
            return (T) element;
        }
    }

    protected <T, U> List<T> extractResults(ObjectNode jsonResponse, String key, ExtractOperation<T, U> extractInterafce)
            throws AtlasServiceException {
        ArrayNode    results     = (ArrayNode)jsonResponse.get(key);
        ArrayList<T> resultsList = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            Object element = results.get(index);
            resultsList.add(extractInterafce.extractElement((U) element));
        }
        return resultsList;
    }

    /**
     * Get the latest numResults entity audit events in decreasing order of timestamp for the given entity id
     * @param entityId entity id
     * @param numResults number of results to be returned
     * @return list of audit events for the entity id
     * @throws AtlasServiceException
     */
    public List<EntityAuditEvent> getEntityAuditEvents(String entityId, short numResults)
            throws AtlasServiceException {
        return getEntityAuditEvents(entityId, null, numResults);
    }

    /**
     * Get the entity audit events in decreasing order of timestamp for the given entity id
     * @param entityId entity id
     * @param startKey key for the first event to be returned, used for pagination
     * @param numResults number of results to be returned
     * @return list of audit events for the entity id
     * @throws AtlasServiceException
     */
    public List<EntityAuditEvent> getEntityAuditEvents(String entityId, String startKey, short numResults)
            throws AtlasServiceException {
        WebResource resource = getResource(API_V1.LIST_ENTITY_AUDIT, entityId, URI_ENTITY_AUDIT);
        if (StringUtils.isNotEmpty(startKey)) {
            resource = resource.queryParam(START_KEY, startKey);
        }
        resource = resource.queryParam(NUM_RESULTS, String.valueOf(numResults));

        ObjectNode jsonResponse = callAPIWithResource(API_V1.LIST_ENTITY_AUDIT, resource);
        return extractResults(jsonResponse, AtlasClient.EVENTS, new ExtractOperation<EntityAuditEvent, ObjectNode>() {
            @Override
            EntityAuditEvent extractElement(ObjectNode element) {
                return AtlasType.fromV1Json(element.toString(), EntityAuditEvent.class);
            }
        });

    }

    /**
     * Search using dsl/full text
     * @param searchQuery
     * @param limit number of rows to be returned in the result, used for pagination. maxlimit > limit > 0. -1 maps to atlas.search.defaultlimit property value
     * @param offset offset to the results returned, used for pagination. offset >= 0. -1 maps to offset 0
     * @return Query results
     * @throws AtlasServiceException
     */
    public JsonNode search(final String searchQuery, final int limit, final int offset) throws AtlasServiceException {
        final API api = API_V1.SEARCH;
        ObjectNode result = callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api);
                resource = resource.queryParam(QUERY, searchQuery);
                resource = resource.queryParam(LIMIT, String.valueOf(limit));
                resource = resource.queryParam(OFFSET, String.valueOf(offset));
                return resource;
            }
        });
        return result.get(RESULTS);
    }

    /**
     * Search given query DSL
     * @param query DSL query
     * @param limit number of rows to be returned in the result, used for pagination. maxlimit > limit > 0. -1 maps to atlas.search.defaultlimit property value
     * @param offset offset to the results returned, used for pagination. offset >= 0. -1 maps to offset 0
     * @return result json object
     * @throws AtlasServiceException
     */
    public ArrayNode searchByDSL(final String query, final int limit, final int offset) throws AtlasServiceException {
        LOG.debug("DSL query: {}", query);
        final API api = API_V1.SEARCH_DSL;
        ObjectNode response = callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api);
                resource = resource.queryParam(QUERY, query);
                resource = resource.queryParam(LIMIT, String.valueOf(limit));
                resource = resource.queryParam(OFFSET, String.valueOf(offset));
                return resource;
            }
        });

        JsonNode results = response.get(RESULTS);

        return (results.isNull()) ? AtlasJson.createV1ArrayNode(): (ArrayNode) response.get(RESULTS);
    }

    /**
     * Search given full text search
     * @param query Query
     * @param limit number of rows to be returned in the result, used for pagination. maxlimit > limit > 0. -1 maps to atlas.search.defaultlimit property value
     * @param offset offset to the results returned, used for pagination. offset >= 0. -1 maps to offset 0
     * @return result json object
     * @throws AtlasServiceException
     */
    public ObjectNode searchByFullText(final String query, final int limit, final int offset) throws AtlasServiceException {
        final API api = API_V1.SEARCH_FULL_TEXT;
        return callAPIWithRetries(api, null, new ResourceCreator() {
            @Override
            public WebResource createResource() {
                WebResource resource = getResource(api);
                resource = resource.queryParam(QUERY, query);
                resource = resource.queryParam(LIMIT, String.valueOf(limit));
                resource = resource.queryParam(OFFSET, String.valueOf(offset));
                return resource;
            }
        });
    }

    public ObjectNode getInputGraph(String datasetName) throws AtlasServiceException {
        ObjectNode response = callAPIWithBodyAndParams(API_V1.NAME_LINEAGE_INPUTS_GRAPH, null, datasetName, "/inputs/graph");
        return (ObjectNode)response.get(AtlasClient.RESULTS);
    }

    public ObjectNode getOutputGraph(String datasetName) throws AtlasServiceException {
        ObjectNode response = callAPIWithBodyAndParams(API_V1.NAME_LINEAGE_OUTPUTS_GRAPH, null, datasetName, "/outputs/graph");
        return (ObjectNode)response.get(AtlasClient.RESULTS);
    }

    public ObjectNode getInputGraphForEntity(String entityId) throws AtlasServiceException {
        ObjectNode response = callAPIWithBodyAndParams(API_V1.LINEAGE_INPUTS_GRAPH, null, entityId, "/inputs/graph");
        return (ObjectNode)response.get(AtlasClient.RESULTS);
    }

    public ObjectNode getOutputGraphForEntity(String datasetId) throws AtlasServiceException {
        ObjectNode response = callAPIWithBodyAndParams(API_V1.LINEAGE_OUTPUTS_GRAPH, null, datasetId, "/outputs/graph");
        return (ObjectNode) response.get(AtlasClient.RESULTS);
    }

    public ObjectNode getSchemaForEntity(String datasetId) throws AtlasServiceException {
        ObjectNode response = callAPIWithBodyAndParams(API_V1.LINEAGE_OUTPUTS_GRAPH, null, datasetId, "/schema");
        return (ObjectNode) response.get(AtlasClient.RESULTS);
    }

    private List<String> extractStringList(ObjectNode response) {
        List<String> ret     = new ArrayList<>();
        JsonNode     results = (response != null) ? response.get(AtlasClient.RESULTS) : null;

        if (results != null && results instanceof ArrayNode) {
            for (JsonNode node : results) {
                ret.add(node.asText());
            }
        }

        return ret;
    }

    // Wrapper methods for compatibility
    @VisibleForTesting
    public ObjectNode callAPIWithResource(API api, WebResource resource) throws AtlasServiceException {
        return callAPIWithResource(api, resource, null, ObjectNode.class);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithResource(API_V1 apiV1, WebResource resource) throws AtlasServiceException {
        return callAPIWithResource(apiV1, resource, null, ObjectNode.class);
    }

    @VisibleForTesting
    public WebResource getResource(API api, String... params) {
        return getResource(api.getNormalizedPath(), params);
    }

    @VisibleForTesting
    public WebResource getResource(API_V1 apiV1, String... params) {
        return getResource(apiV1.getNormalizedPath(), params);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithBody(API api, Object requestObject) throws AtlasServiceException {
        return callAPI(api, ObjectNode.class, requestObject, (String[]) null);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithBody(API_V1 apiV1, Object requestObject) throws AtlasServiceException {
        return callAPI(apiV1, ObjectNode.class, requestObject, (String[]) null);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithBodyAndParams(API api, Object requestObject, String... params) throws AtlasServiceException {
        return callAPI(api, ObjectNode.class, requestObject, params);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithBodyAndParams(API_V1 apiV1, Object requestObject, String... params) throws AtlasServiceException {
        return callAPI(apiV1, ObjectNode.class, requestObject, params);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithQueryParams(API api, MultivaluedMap<String, String> queryParams) throws AtlasServiceException {
        return callAPI(api, ObjectNode.class, queryParams);
    }

    @VisibleForTesting
    public ObjectNode callAPIWithQueryParams(API_V1 apiV1, MultivaluedMap<String, String> queryParams) throws AtlasServiceException {
        return callAPI(apiV1, ObjectNode.class, queryParams);
    }

    @VisibleForTesting
    ObjectNode callAPIWithRetries(API api, Object requestObject, ResourceCreator resourceCreator) throws AtlasServiceException {
        return super.callAPIWithRetries(api, requestObject, resourceCreator);
    }

    @VisibleForTesting
    ObjectNode callAPIWithRetries(API_V1 apiV1, Object requestObject, ResourceCreator resourceCreator) throws AtlasServiceException {
        return super.callAPIWithRetries(apiV1, requestObject, resourceCreator);
    }
}
