/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.audit.AuditSearchParameters;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.model.discovery.AtlasQuickSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.AtlasSuggestionsResult;
import org.apache.atlas.model.discovery.QuickSearchParameters;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasClassification.AtlasClassifications;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasEntityHeaders;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.model.instance.AtlasRelationship.AtlasRelationshipWithExtInfo;
import org.apache.atlas.model.instance.ClassificationAssociateRequest;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection;
import org.apache.atlas.model.profile.AtlasUserSavedSearch;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypeDefHeader;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AtlasClientV2 extends AtlasBaseClient {
    // Type APIs
    public static final  String TYPES_API            = BASE_URI + "v2/types/";
    // Entity APIs
    public static final  String ENTITY_API           = BASE_URI + "v2/entity/";
    private static final String PREFIX_ATTR          = "attr:";
    private static final String PREFIX_ATTR_         = "attr_";
    private static final String TYPEDEFS_API         = TYPES_API + "typedefs/";
    private static final String TYPEDEF_BY_NAME      = TYPES_API + "typedef/name/";
    private static final String TYPEDEF_BY_GUID      = TYPES_API + "typedef/guid/";
    private static final String GET_BY_NAME_TEMPLATE = TYPES_API + "%s/name/%s";
    private static final String GET_BY_GUID_TEMPLATE = TYPES_API + "%s/guid/%s";
    private static final String ENTITY_BULK_API      = ENTITY_API + "bulk/";

    //Admin Entity
    private static final String ADMIN_API            = BASE_URI + "admin/";
    private static final String ENTITY_PURGE_API     = ADMIN_API + "purge/";
    private static final String ATLAS_AUDIT_API      = ADMIN_API + "audits/";

    // Lineage APIs
    private static final String LINEAGE_URI          = BASE_URI + "v2/lineage/";

    // Discovery APIs
    private static final String DISCOVERY_URI        = BASE_URI + "v2/search";
    private static final String DSL_SEARCH_URI       = DISCOVERY_URI + "/dsl";
    private static final String FULL_TEXT_SEARCH_URI = DISCOVERY_URI + "/fulltext";
    private static final String BASIC_SEARCH_URI     = DISCOVERY_URI + "/basic";
    private static final String FACETED_SEARCH_URI   = BASIC_SEARCH_URI;
    private static final String SAVED_SEARCH_URI     = DISCOVERY_URI + "/saved";
    private static final String QUICK_SEARCH_URI     = DISCOVERY_URI + "/quick";

    // Relationships APIs
    private static final String RELATIONSHIPS_URI        = BASE_URI + "v2/relationship/";
    private static final String BULK_HEADERS             = "bulk/headers";
    private static final String BULK_SET_CLASSIFICATIONS = "bulk/setClassifications";
    private static final String RELATIONSHIP_URI         = DISCOVERY_URI + "/relationship";


    //Glossary APIs
    private static final String GLOSSARY_URI         = BASE_URI + "v2/glossary";
    private static final String GLOSSARY_TERM        = GLOSSARY_URI + "/term";
    private static final String GLOSSARY_TERMS       = GLOSSARY_URI + "/terms";
    private static final String GLOSSARY_CATEGORY    = GLOSSARY_URI + "/category";
    private static final String GLOSSARY_CATEGORIES  = GLOSSARY_URI + "/categories";


    public AtlasClientV2(String[] baseUrl, String[] basicAuthUserNamePassword) {
        super(baseUrl, basicAuthUserNamePassword);
    }

    public AtlasClientV2(String... baseUrls) throws AtlasException {
        super(baseUrls);
    }

    public AtlasClientV2(UserGroupInformation ugi, String doAsUser, String... baseUrls) {
        super(ugi, doAsUser, baseUrls);
    }

    /**
     * Constructor for AtlasClient with cookie params as header
     * @param baseUrl
     * @param cookieName
     * @param value
     * @param path
     * @param domain
     */
    public AtlasClientV2(String[] baseUrl, String cookieName, String value, String path, String domain) {
        super(baseUrl, new Cookie(cookieName, value, path, domain));
    }

    /**
     * Constructor for AtlasClient with cookie as header
     * @param baseUrl
     * @param cookie
     */
    public AtlasClientV2(String[] baseUrl, Cookie cookie) {
        super(baseUrl, cookie);
    }

    public AtlasClientV2(Configuration configuration, String[] baseUrl, String[] basicAuthUserNamePassword) {
        super(configuration, baseUrl, basicAuthUserNamePassword);
    }

    @VisibleForTesting
    AtlasClientV2(WebResource service, Configuration configuration) {
        super(service, configuration);
    }

    /**
     * Bulk retrieval API for retrieving all type definitions in Atlas
     *
     * @return A composite wrapper object with lists of all type definitions
     */
    public AtlasTypesDef getAllTypeDefs(SearchFilter searchFilter) throws AtlasServiceException {
        return callAPI(API_V2.GET_ALL_TYPE_DEFS, AtlasTypesDef.class, searchFilter.getParams());
    }

    public List<AtlasTypeDefHeader> getAllTypeDefHeaders(SearchFilter searchFilter) throws AtlasServiceException {
        return callAPI(API_V2.GET_ALL_TYPE_DEF_HEADERS, List.class, searchFilter.getParams());
    }

    public boolean typeWithGuidExists(String guid) {
        try {
            callAPI(API_V2.GET_TYPEDEF_BY_GUID, String.class, null, guid);
        } catch (AtlasServiceException e) {
            return false;
        }

        return true;
    }

    public boolean typeWithNameExists(String name) {
        try {
            callAPI(API_V2.GET_TYPEDEF_BY_NAME, String.class, null, name);
        } catch (AtlasServiceException e) {
            return false;
        }

        return true;
    }

    public AtlasEnumDef getEnumDefByName(String name) throws AtlasServiceException {
        return getTypeDefByName(name, AtlasEnumDef.class);
    }

    public AtlasEnumDef getEnumDefByGuid(String guid) throws AtlasServiceException {
        return getTypeDefByGuid(guid, AtlasEnumDef.class);
    }

    public AtlasStructDef getStructDefByName(String name) throws AtlasServiceException {
        return getTypeDefByName(name, AtlasStructDef.class);
    }

    public AtlasStructDef getStructDefByGuid(String guid) throws AtlasServiceException {
        return getTypeDefByGuid(guid, AtlasStructDef.class);
    }

    public AtlasClassificationDef getClassificationDefByName(String name) throws AtlasServiceException {
        return getTypeDefByName(name, AtlasClassificationDef.class);
    }

    public AtlasClassificationDef getClassificationDefByGuid(String guid) throws AtlasServiceException {
        return getTypeDefByGuid(guid, AtlasClassificationDef.class);
    }

    public AtlasEntityDef getEntityDefByName(String name) throws AtlasServiceException {
        return getTypeDefByName(name, AtlasEntityDef.class);
    }

    public AtlasEntityDef getEntityDefByGuid(String guid) throws AtlasServiceException {
        return getTypeDefByGuid(guid, AtlasEntityDef.class);
    }

    public AtlasRelationshipDef getRelationshipDefByName(String name) throws AtlasServiceException {
        return getTypeDefByName(name, AtlasRelationshipDef.class);
    }

    public AtlasRelationshipDef getRelationshipDefByGuid(String guid) throws AtlasServiceException {
        return getTypeDefByGuid(guid, AtlasRelationshipDef.class);
    }

    public AtlasBusinessMetadataDef getBusinessMetadataDefByName(String name) throws AtlasServiceException {
        return getTypeDefByName(name, AtlasBusinessMetadataDef.class);
    }

    public AtlasBusinessMetadataDef getBusinessMetadataDefGuid(String guid) throws AtlasServiceException {
        return getTypeDefByGuid(guid, AtlasBusinessMetadataDef.class);
    }

    @Deprecated
    public AtlasEnumDef createEnumDef(AtlasEnumDef enumDef) throws AtlasServiceException {
        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getEnumDefs().add(enumDef);

        AtlasTypesDef created = createAtlasTypeDefs(typesDef);

        assert created != null;
        assert created.getEnumDefs() != null;

        return created.getEnumDefs().get(0);
    }

    @Deprecated
    public AtlasStructDef createStructDef(AtlasStructDef structDef) throws AtlasServiceException {
        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getStructDefs().add(structDef);

        AtlasTypesDef created = createAtlasTypeDefs(typesDef);

        assert created != null;
        assert created.getStructDefs() != null;

        return created.getStructDefs().get(0);
    }

    @Deprecated
    public AtlasEntityDef createEntityDef(AtlasEntityDef entityDef) throws AtlasServiceException {
        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getEntityDefs().add(entityDef);

        AtlasTypesDef created = createAtlasTypeDefs(typesDef);

        assert created != null;
        assert created.getEntityDefs() != null;

        return created.getEntityDefs().get(0);
    }

    @Deprecated
    public AtlasClassificationDef createClassificationDef(AtlasClassificationDef classificationDef) throws AtlasServiceException {
        AtlasTypesDef typesDef = new AtlasTypesDef();

        typesDef.getClassificationDefs().add(classificationDef);

        AtlasTypesDef created = createAtlasTypeDefs(typesDef);

        assert created != null;
        assert created.getClassificationDefs() != null;

        return created.getClassificationDefs().get(0);
    }

    /**
     * Bulk create APIs for all atlas type definitions, only new definitions will be created.
     * Any changes to the existing definitions will be discarded
     *
     * @param typesDef A composite wrapper object with corresponding lists of the type definition
     * @return A composite wrapper object with lists of type definitions that were successfully
     * created
     */
    public AtlasTypesDef createAtlasTypeDefs(AtlasTypesDef typesDef) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_TYPE_DEFS, AtlasTypesDef.class, AtlasType.toJson(typesDef));
    }

    /**
     * Bulk update API for all types, changes detected in the type definitions would be persisted
     *
     * @param typesDef A composite object that captures all type definition changes
     * @return A composite object with lists of type definitions that were updated
     */
    public AtlasTypesDef updateAtlasTypeDefs(AtlasTypesDef typesDef) throws AtlasServiceException {
        return callAPI(API_V2.UPDATE_TYPE_DEFS, AtlasTypesDef.class, AtlasType.toJson(typesDef));
    }

    /**
     * Bulk delete API for all types
     *
     * @param typesDef A composite object that captures all types to be deleted
     */
    public void deleteAtlasTypeDefs(AtlasTypesDef typesDef) throws AtlasServiceException {
        callAPI(API_V2.DELETE_TYPE_DEFS, (Class<?>)null, AtlasType.toJson(typesDef));
    }

    public void deleteTypeByName(String typeName) throws AtlasServiceException{
        callAPI(API_V2.DELETE_TYPE_DEF_BY_NAME, (Class) null, null, typeName);
    }


    // Entity APIs
    public AtlasEntityWithExtInfo getEntityByGuid(String guid) throws AtlasServiceException {
        return getEntityByGuid(guid, false, false);
    }

    public AtlasEntityWithExtInfo getEntityByGuid(String guid, boolean minExtInfo, boolean ignoreRelationships) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("minExtInfo", String.valueOf(minExtInfo));
        queryParams.add("ignoreRelationships", String.valueOf(ignoreRelationships));

        return callAPI(API_V2.GET_ENTITY_BY_GUID, AtlasEntityWithExtInfo.class, queryParams, guid);
    }

    public AtlasEntityWithExtInfo getEntityByAttribute(String typeName, Map<String, String> uniqAttributes) throws AtlasServiceException {
        return getEntityByAttribute(typeName, uniqAttributes, false, false);
    }

    public AtlasEntityWithExtInfo getEntityByAttribute(String typeName, Map<String, String> uniqAttributes, boolean minExtInfo, boolean ignoreRelationship) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        queryParams.add("minExtInfo", String.valueOf(minExtInfo));
        queryParams.add("ignoreRelationships", String.valueOf(ignoreRelationship));

        return callAPI(API_V2.GET_ENTITY_BY_UNIQUE_ATTRIBUTE, AtlasEntityWithExtInfo.class, queryParams, typeName);
    }

    public AtlasEntitiesWithExtInfo getEntitiesByGuids(List<String> guids) throws AtlasServiceException {
        return getEntitiesByGuids(guids, false, false);
    }

    public AtlasEntitiesWithExtInfo getEntitiesByGuids(List<String> guids, boolean minExtInfo, boolean ignoreRelationships) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.put("guid", guids);
        queryParams.add("minExtInfo", String.valueOf(minExtInfo));
        queryParams.add("ignoreRelationships", String.valueOf(ignoreRelationships));

        return callAPI(API_V2.GET_ENTITIES_BY_GUIDS, AtlasEntitiesWithExtInfo.class, queryParams);
    }

    public AtlasEntitiesWithExtInfo getEntitiesByAttribute(String typeName, List<Map<String,String>> uniqAttributesList) throws AtlasServiceException {
        return getEntitiesByAttribute(typeName, uniqAttributesList, false, false);
    }

    public AtlasEntitiesWithExtInfo getEntitiesByAttribute(String typeName, List<Map<String, String>> uniqAttributesList, boolean minExtInfo, boolean ignoreRelationship) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributesList, null);

        queryParams.add("minExtInfo", String.valueOf(minExtInfo));
        queryParams.add("ignoreRelationships", String.valueOf(ignoreRelationship));

        return callAPI(API_V2.GET_ENTITIES_BY_UNIQUE_ATTRIBUTE, AtlasEntitiesWithExtInfo.class, queryParams, typeName);
    }

    public AtlasEntityHeader getEntityHeaderByGuid(String entityGuid) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.GET_ENTITY_HEADER_BY_GUID, entityGuid), AtlasEntityHeader.class, null);
    }

    public AtlasEntityHeader getEntityHeaderByAttribute(String typeName, Map<String, String> uniqAttributes) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        return callAPI(formatPathParameters(API_V2.GET_ENTITY_HEADER_BY_UNIQUE_ATTRIBUTE, typeName), AtlasEntityHeader.class, queryParams);
    }

    public List<EntityAuditEventV2> getAuditEvents(String guid, String startKey, EntityAuditEventV2.EntityAuditActionV2 auditAction, short count) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("startKey", startKey);

        if (auditAction != null) {
            queryParams.add("auditAction", auditAction.toString());
        }

        queryParams.add("count", String.valueOf(count));

        return callAPI(formatPathParameters(API_V2.GET_AUDIT_EVENTS, guid), List.class, queryParams);
    }

    public EntityMutationResponse createEntity(AtlasEntityWithExtInfo entity) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_ENTITY, EntityMutationResponse.class, entity);
    }

    public EntityMutationResponse createEntities(AtlasEntitiesWithExtInfo atlasEntities) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_ENTITIES, EntityMutationResponse.class, atlasEntities);
    }

    public EntityMutationResponse updateEntity(AtlasEntityWithExtInfo entity) throws AtlasServiceException {
        return callAPI(API_V2.UPDATE_ENTITY, EntityMutationResponse.class, entity);
    }

    public EntityMutationResponse updateEntities(AtlasEntitiesWithExtInfo atlasEntities) throws AtlasServiceException {
        return callAPI(API_V2.UPDATE_ENTITIES, EntityMutationResponse.class, atlasEntities);
    }

    public EntityMutationResponse updateEntityByAttribute(String typeName, Map<String, String> uniqAttributes, AtlasEntityWithExtInfo entityInfo) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        return callAPI(API_V2.UPDATE_ENTITY_BY_ATTRIBUTE, EntityMutationResponse.class, entityInfo, queryParams, typeName);
    }

    public EntityMutationResponse partialUpdateEntityByGuid(String entityGuid, Object attrValue, String attrName) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("name", attrName);

        return callAPI(formatPathParameters(API_V2.PARTIAL_UPDATE_ENTITY_BY_GUID, entityGuid), EntityMutationResponse.class, attrValue, queryParams);
    }

    public EntityMutationResponse deleteEntityByGuid(String guid) throws AtlasServiceException {
        return callAPI(API_V2.DELETE_ENTITY_BY_GUID, EntityMutationResponse.class, null, guid);
    }

    public EntityMutationResponse deleteEntityByAttribute(String typeName, Map<String, String> uniqAttributes) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        return callAPI(API_V2.DELETE_ENTITY_BY_ATTRIBUTE, EntityMutationResponse.class, queryParams, typeName);
    }

    public EntityMutationResponse deleteEntitiesByGuids(List<String> guids) throws AtlasServiceException {
        return callAPI(API_V2.DELETE_ENTITIES_BY_GUIDS, EntityMutationResponse.class, "guid", guids);
    }

    public EntityMutationResponse purgeEntitiesByGuids(Set<String> guids) throws AtlasServiceException {
        return callAPI(API_V2.PURGE_ENTITIES_BY_GUIDS, EntityMutationResponse.class, guids);
    }


    // Entity-classification APIs
    public AtlasClassifications getClassifications(String guid) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.GET_CLASSIFICATIONS, guid), AtlasClassifications.class, null);
    }

    public AtlasClassifications getEntityClassifications(String entityGuid, String classificationName) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.GET_FROM_CLASSIFICATION, entityGuid, classificationName), AtlasClassifications.class, null);
    }

    public void addClassification(ClassificationAssociateRequest request) throws AtlasServiceException {
        callAPI(API_V2.ADD_CLASSIFICATION, (Class<?>) null, request);
    }

    public void addClassifications(String guid, List<AtlasClassification> classifications) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.ADD_CLASSIFICATIONS, guid), (Class<?>)null, classifications, (String[]) null);
    }

    public void addClassifications(String typeName, Map<String, String> uniqAttributes, List<AtlasClassification> classifications) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        callAPI(formatPathParameters(API_V2.ADD_CLASSIFICATION_BY_TYPE_AND_ATTRIBUTE, typeName), (Class<?>) null, classifications, queryParams);
    }

    public void updateClassifications(String guid, List<AtlasClassification> classifications) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.UPDATE_CLASSIFICATIONS, guid), (Class<?>)null, classifications);
    }

    public void updateClassifications(String typeName, Map<String, String> uniqAttributes, List<AtlasClassification> classifications) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        callAPI(formatPathParameters(API_V2.UPDATE_CLASSIFICATION_BY_TYPE_AND_ATTRIBUTE, typeName), (Class<?>) null, classifications, queryParams);
    }

    public String setClassifications(AtlasEntityHeaders entityHeaders) throws AtlasServiceException {
        return callAPI(API_V2.UPDATE_BULK_SET_CLASSIFICATIONS, String.class, entityHeaders);
    }

    public void deleteClassification(String guid, String classificationName) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_CLASSIFICATION, guid, classificationName), null, null);
    }

    public void deleteClassifications(String guid, List<AtlasClassification> classifications) throws AtlasServiceException {
        for (AtlasClassification c : classifications) {
            callAPI(formatPathParameters(API_V2.DELETE_CLASSIFICATION, guid, c.getTypeName()), null, null);
        }
    }

    public void removeClassification(String entityGuid, String classificationName, String associatedEntityGuid) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_CLASSIFICATION, entityGuid, classificationName), (Class<?>) null, associatedEntityGuid);
    }

    public void removeClassification(String typeName, Map<String, String> uniqAttributes, String classificationName) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        callAPI(formatPathParameters(API_V2.DELETE_CLASSIFICATION_BY_TYPE_AND_ATTRIBUTE, typeName, classificationName), (Class<?>) null, queryParams);
    }

    public AtlasEntityHeaders getEntityHeaders(long tagUpdateStartTime) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("tagUpdateStartTime", Long.toString(tagUpdateStartTime));

        return callAPI(API_V2.GET_BULK_HEADERS, AtlasEntityHeaders.class, queryParams);
    }


    // Business attributes APIs
    public void addOrUpdateBusinessAttributes(String entityGuid, boolean isOverwrite, Map<String, Map<String, Object>> businessAttributes) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("isOverwrite", String.valueOf(isOverwrite));

        callAPI(formatPathParameters(API_V2.ADD_BUSINESS_ATTRIBUTE, entityGuid), (Class<?>) null, businessAttributes, queryParams);
    }

    public void addOrUpdateBusinessAttributes(String entityGuid, String bmName, Map<String, Map<String, Object>> businessAttributes) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.ADD_BUSINESS_ATTRIBUTE_BY_NAME, entityGuid, bmName), (Class<?>) null, businessAttributes);
    }

    public void removeBusinessAttributes(String entityGuid, Map<String, Map<String, Object>> businessAttributes) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_BUSINESS_ATTRIBUTE, entityGuid), (Class<?>) null, businessAttributes);
    }

    public void removeBusinessAttributes(String entityGuid, String bmName, Map<String, Map<String, Object>> businessAttributes) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_BUSINESS_ATTRIBUTE_BY_NAME, entityGuid, bmName), (Class<?>) null, businessAttributes);
    }

    public String getTemplateForBulkUpdateBusinessAttributes() throws AtlasServiceException {
        InputStream inputStream = (InputStream) callAPI(API_V2.GET_BUSINESS_METADATA_TEMPLATE, Object.class, null);

        return readStreamContents(inputStream);
    }

    public BulkImportResponse bulkUpdateBusinessAttributes(String fileName) throws AtlasServiceException {
        MultiPart multipartEntity = getMultiPartData(fileName);

        return callAPI(API_V2.IMPORT_BUSINESS_METADATA, BulkImportResponse.class, multipartEntity);
    }


    // Labels APIs
    public void addLabels(String entityGuid, Set<String> labels) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.ADD_LABELS, entityGuid), (Class<?>) null, labels, (String[]) null);
    }

    public void addLabels(String typeName, Map<String, String> uniqAttributes, Set<String> labels) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        callAPI(formatPathParameters(API_V2.SET_LABELS_BY_UNIQUE_ATTRIBUTE, typeName), (Class<?>) null, labels, queryParams);
    }

    public void removeLabels(String entityGuid, Set<String> labels) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_LABELS, entityGuid), (Class<?>) null, labels);
    }

    public void removeLabels(String typeName, Map<String, String> uniqAttributes, Set<String> labels) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        callAPI(formatPathParameters(API_V2.DELETE_LABELS_BY_UNIQUE_ATTRIBUTE, typeName), (Class<?>) null, labels, queryParams);
    }

    public void setLabels(String entityGuid, Set<String> labels) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.SET_LABELS, entityGuid), (Class<?>) null, labels, (String[]) null);
    }

    public void setLabels(String typeName, Map<String, String> uniqAttributes, Set<String> labels) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(uniqAttributes);

        callAPI(formatPathParameters(API_V2.ADD_LABELS_BY_UNIQUE_ATTRIBUTE, typeName), (Class<?>) null, labels, queryParams);
    }


    /* Lineage APIs  */
    public AtlasLineageInfo getLineageInfo(String guid, LineageDirection direction, int depth) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("direction", direction.toString());
        queryParams.add("depth", String.valueOf(depth));

        return callAPI(API_V2.LINEAGE_INFO, AtlasLineageInfo.class, queryParams, guid);
    }

    public AtlasLineageInfo getLineageInfo(String typeName, Map<String, String> attributes, LineageDirection direction, int depth) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = attributesToQueryParams(attributes);

        queryParams.add("direction", direction.toString());
        queryParams.add("depth", String.valueOf(depth));

        return callAPI(API_V2.GET_LINEAGE_BY_ATTRIBUTES, AtlasLineageInfo.class, queryParams, typeName);
    }


    /* Discovery APIs */
    public AtlasSearchResult dslSearch(String query) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add(QUERY, query);

        return callAPI(API_V2.DSL_SEARCH, AtlasSearchResult.class, queryParams);
    }

    public AtlasSearchResult dslSearchWithParams(String query, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add(QUERY, query);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));

        return callAPI(API_V2.DSL_SEARCH, AtlasSearchResult.class, queryParams);
    }

    public AtlasSearchResult fullTextSearch(String query) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add(QUERY, query);

        return callAPI(API_V2.FULL_TEXT_SEARCH, AtlasSearchResult.class, queryParams);
    }

    public AtlasSearchResult fullTextSearchWithParams(String query, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add(QUERY, query);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));

        return callAPI(API_V2.FULL_TEXT_SEARCH, AtlasSearchResult.class, queryParams);
    }

    public AtlasSearchResult basicSearch(String typeName, String classification, String query, boolean excludeDeletedEntities, int limit, int offset) throws AtlasServiceException {
        return this.basicSearch(typeName, null, classification, query, excludeDeletedEntities, limit, offset);
    }

    public AtlasSearchResult basicSearch(String typeName, SearchParameters.FilterCriteria entityFilters, String classification, String query, boolean excludeDeletedEntities, int limit, int offset) throws AtlasServiceException {
        SearchParameters parameters = new SearchParameters();
        parameters.setTypeName(typeName);
        parameters.setClassification(classification);
        parameters.setQuery(query);
        parameters.setExcludeDeletedEntities(excludeDeletedEntities);
        parameters.setLimit(limit);
        parameters.setOffset(offset);
        if (entityFilters != null){
            parameters.setEntityFilters(entityFilters);
        }

        return callAPI(API_V2.BASIC_SEARCH, AtlasSearchResult.class, parameters);
    }

    public AtlasSearchResult facetedSearch(SearchParameters searchParameters) throws AtlasServiceException {
        return callAPI(API_V2.FACETED_SEARCH, AtlasSearchResult.class, searchParameters);
    }

    public AtlasSearchResult attributeSearch(String typeName, String attrName, String attrValuePrefix, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("attrName", attrName);
        queryParams.add("attrValuePrefix", attrValuePrefix);
        queryParams.add("typeName", typeName);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));

        return callAPI(API_V2.ATTRIBUTE_SEARCH, AtlasSearchResult.class, queryParams);
    }

    public AtlasSearchResult relationshipSearch(String guid, String relation, String sortByAttribute, SortOrder sortOrder, boolean excludeDeletedEntities, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("guid", guid);
        queryParams.add("relation", relation);
        queryParams.add("sortBy", sortByAttribute);

        if (sortOrder != null) {
            queryParams.add("sortOrder", String.valueOf(sortOrder));
        }

        queryParams.add("excludeDeletedEntities", String.valueOf(excludeDeletedEntities));
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));

        return callAPI(API_V2.RELATIONSHIP_SEARCH, AtlasSearchResult.class, queryParams);
    }

    public AtlasQuickSearchResult quickSearch(String query, String typeName, boolean excludeDeletedEntities, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("query", query);
        queryParams.add("typeName", typeName);
        queryParams.add("excludeDeletedEntities", String.valueOf(excludeDeletedEntities));
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));

        return callAPI(API_V2.QUICK_SEARCH_WITH_GET, AtlasQuickSearchResult.class, queryParams);
    }

    public AtlasQuickSearchResult quickSearch(QuickSearchParameters quickSearchParameters) throws AtlasServiceException {
        return callAPI(API_V2.QUICK_SEARCH_WITH_POST, AtlasQuickSearchResult.class, quickSearchParameters);
    }

    // fieldName should be the parameter on which indexing is enabled such as "qualifiedName"
    public AtlasSuggestionsResult getSuggestions(String prefixString, String fieldName) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        if (StringUtils.isNotEmpty(prefixString)) {
            queryParams.add("prefixString", prefixString);
        }

        if (StringUtils.isNotEmpty(fieldName)) {
            queryParams.add("fieldName", fieldName);
        }

        return callAPI(API_V2.GET_SUGGESTIONS, AtlasSuggestionsResult.class, queryParams);
    }

    public List<AtlasUserSavedSearch> getSavedSearches(String userName) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("user", userName);

        return callAPI(API_V2.GET_SAVED_SEARCHES, List.class, queryParams);
    }

    public AtlasUserSavedSearch getSavedSearch(String userName, String searchName) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("user", userName);

        return callAPI(formatPathParameters(API_V2.GET_SAVED_SEARCH, searchName), AtlasUserSavedSearch.class, queryParams);
    }

    public AtlasUserSavedSearch addSavedSearch(AtlasUserSavedSearch savedSearch) throws AtlasServiceException {
        return callAPI(API_V2.ADD_SAVED_SEARCH, AtlasUserSavedSearch.class, savedSearch);
    }

    public AtlasUserSavedSearch updateSavedSearch(AtlasUserSavedSearch savedSearch) throws AtlasServiceException {
        return callAPI(API_V2.UPDATE_SAVED_SEARCH, AtlasUserSavedSearch.class, savedSearch);
    }

    public void deleteSavedSearch(String guid) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_SAVED_SEARCH, guid), (Class<?>) null, null);
    }

    public AtlasSearchResult executeSavedSearch(String userName, String searchName) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("user", userName);

        return callAPI(formatPathParameters(API_V2.EXECUTE_SAVED_SEARCH_BY_NAME, searchName), AtlasSearchResult.class, queryParams);
    }

    public AtlasSearchResult executeSavedSearch(String searchGuid) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.EXECUTE_SAVED_SEARCH_BY_GUID, searchGuid), AtlasSearchResult.class, null);
    }


    // Relationship APIs
    public AtlasRelationshipWithExtInfo getRelationshipByGuid(String guid) throws AtlasServiceException {
        return callAPI(API_V2.GET_RELATIONSHIP_BY_GUID, AtlasRelationshipWithExtInfo.class, null, guid);
    }

    public AtlasRelationshipWithExtInfo getRelationshipByGuid(String guid, boolean extendedInfo) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("extendedInfo", String.valueOf(extendedInfo));

        return callAPI(API_V2.GET_RELATIONSHIP_BY_GUID, AtlasRelationshipWithExtInfo.class, queryParams, guid);
    }

    public AtlasRelationship createRelationship(AtlasRelationship relationship) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_RELATIONSHIP, AtlasRelationship.class, relationship);
    }

    public AtlasRelationship updateRelationship(AtlasRelationship relationship) throws AtlasServiceException {
        return callAPI(API_V2.UPDATE_RELATIONSHIP, AtlasRelationship.class, relationship);
    }

    public void deleteRelationshipByGuid(String guid) throws AtlasServiceException {
        callAPI(API_V2.DELETE_RELATIONSHIP_BY_GUID, (Class) null, null, guid);
    }


    // Admin APIs
    public List<AtlasAuditEntry> getAtlasAuditByOperation(AuditSearchParameters auditSearchParameters) throws AtlasServiceException {
        ArrayNode response = callAPI(API_V2.GET_ATLAS_AUDITS, ArrayNode.class, auditSearchParameters);

        return extractResults(response, new ExtractOperation<AtlasAuditEntry, ObjectNode>() {
            @Override
            AtlasAuditEntry extractElement(ObjectNode element) {
                return AtlasType.fromV1Json(element.toString(), AtlasAuditEntry.class);
            }
        });
    }


    // Glossary APIs
    public List<AtlasGlossary> getAllGlossaries(String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("sort", sortByAttribute);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));

        return callAPI(API_V2.GET_ALL_GLOSSARIES, List.class, queryParams);
    }

    public AtlasGlossary getGlossaryByGuid(String glossaryGuid) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.GET_GLOSSARY_BY_GUID, glossaryGuid), AtlasGlossary.class, null);
    }

    public AtlasGlossary.AtlasGlossaryExtInfo getGlossaryExtInfo(String glossaryGuid) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.GET_DETAILED_GLOSSARY, glossaryGuid), AtlasGlossary.AtlasGlossaryExtInfo.class, null);
    }

    public AtlasGlossaryTerm getGlossaryTerm(String termGuid) throws AtlasServiceException {
        return callAPI(API_V2.GET_GLOSSARY_TERM, AtlasGlossaryTerm.class, null, termGuid);
    }

    public List<AtlasGlossaryTerm> getGlossaryTerms(String glossaryGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("glossaryGuid", glossaryGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_GLOSSARY_TERMS, glossaryGuid), List.class, queryParams);
    }

    public List<AtlasRelatedTermHeader> getGlossaryTermHeaders(String glossaryGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("glossaryGuid", glossaryGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_GLOSSARY_TERMS_HEADERS, glossaryGuid), List.class, queryParams);
    }

    public AtlasGlossaryCategory getGlossaryCategory(String categoryGuid) throws AtlasServiceException {
        return callAPI(API_V2.GET_GLOSSARY_CATEGORY, AtlasGlossaryCategory.class, null, categoryGuid);
    }

    public List<AtlasGlossaryCategory> getGlossaryCategories(String glossaryGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("glossaryGuid", glossaryGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_GLOSSARY_CATEGORIES, glossaryGuid), List.class, queryParams);
    }

    public List<AtlasRelatedCategoryHeader> getGlossaryCategoryHeaders(String glossaryGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("glossaryGuid", glossaryGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_GLOSSARY_CATEGORIES_HEADERS, glossaryGuid), List.class, queryParams);
    }

    public List<AtlasRelatedTermHeader> getCategoryTerms(String categoryGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("categoryGuid", categoryGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_CATEGORY_TERMS, categoryGuid), List.class, queryParams);
    }

    public Map<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> getRelatedTerms(String termGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("termGuid", termGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_RELATED_TERMS, termGuid), Map.class, queryParams);
    }

    public Map<String, List<AtlasRelatedCategoryHeader>> getRelatedCategories(String categoryGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_RELATED_CATEGORIES, categoryGuid), Map.class, queryParams);
    }

    public AtlasGlossary createGlossary(AtlasGlossary glossary) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_GLOSSARY, AtlasGlossary.class, glossary);
    }

    public AtlasGlossaryTerm createGlossaryTerm(AtlasGlossaryTerm glossaryTerm) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_GLOSSARY_TERM, AtlasGlossaryTerm.class, glossaryTerm);
    }

    public List<AtlasGlossaryTerm> createGlossaryTerms(List<AtlasGlossaryTerm> glossaryTerms) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_GLOSSARY_TERMS, List.class, glossaryTerms);
    }

    public AtlasGlossaryCategory createGlossaryCategory(AtlasGlossaryCategory glossaryCategory) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_GLOSSARY_CATEGORY, AtlasGlossaryCategory.class, glossaryCategory);
    }

    public List<AtlasGlossaryCategory> createGlossaryCategories(List<AtlasGlossaryCategory> glossaryCategories) throws AtlasServiceException {
        return callAPI(API_V2.CREATE_GLOSSARY_CATEGORIES, List.class, glossaryCategories);
    }

    public AtlasGlossary updateGlossaryByGuid(String glossaryGuid, AtlasGlossary updatedGlossary) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.UPDATE_GLOSSARY_BY_GUID, glossaryGuid), AtlasGlossary.class, updatedGlossary);
    }

    public AtlasGlossary partialUpdateGlossaryByGuid(String glossaryGuid, Map<String, String> attributes) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.UPDATE_PARTIAL_GLOSSARY, glossaryGuid), AtlasGlossary.class, attributes);
    }

    public AtlasGlossaryTerm updateGlossaryTermByGuid(String termGuid, AtlasGlossaryTerm glossaryTerm) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.UPDATE_GLOSSARY_TERM, termGuid), AtlasGlossaryTerm.class, glossaryTerm);
    }

    public AtlasGlossaryTerm partialUpdateTermByGuid(String termGuid, Map<String, String> attributes) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.UPDATE_PARTIAL_TERM, termGuid), AtlasGlossaryTerm.class, attributes);
    }

    public AtlasGlossaryCategory updateGlossaryCategoryByGuid(String categoryGuid, AtlasGlossaryCategory glossaryCategory) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.UPDATE_CATEGORY_BY_GUID, categoryGuid), AtlasGlossaryCategory.class, glossaryCategory);
    }

    public AtlasGlossaryCategory partialUpdateCategoryByGuid(String categoryGuid, Map<String, String> attributes) throws AtlasServiceException {
        return callAPI(formatPathParameters(API_V2.UPDATE_PARTIAL_CATEGORY, categoryGuid), AtlasGlossaryCategory.class, attributes);
    }

    public void deleteGlossaryByGuid(String glossaryGuid) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_GLOSSARY_BY_GUID, glossaryGuid), (Class<?>) null, null);
    }

    public void deleteGlossaryTermByGuid(String termGuid) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_TERM_BY_GUID, termGuid), (Class<?>) null, null);
    }

    public void deleteGlossaryCategoryByGuid(String categoryGuid) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DELETE_CATEGORY_BY_GUID, categoryGuid), (Class<?>) null, null);
    }

    public List<AtlasRelatedObjectId> getEntitiesAssignedWithTerm(String termGuid, String sortByAttribute, int limit, int offset) throws AtlasServiceException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();

        queryParams.add("termGuid", termGuid);
        queryParams.add(LIMIT, String.valueOf(limit));
        queryParams.add(OFFSET, String.valueOf(offset));
        queryParams.add("sort", sortByAttribute);

        return callAPI(formatPathParameters(API_V2.GET_ENTITIES_ASSIGNED_WITH_TERM, termGuid), List.class, queryParams);
    }

    public void assignTermToEntities(String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.ASSIGN_TERM_TO_ENTITIES, termGuid), (Class<?>) null, relatedObjectIds);

    }

    public void disassociateTermFromEntities(String termGuid, List<AtlasRelatedObjectId> relatedObjectIds) throws AtlasServiceException {
        callAPI(formatPathParameters(API_V2.DISASSOCIATE_TERM_FROM_ENTITIES, termGuid), (Class<?>) null, relatedObjectIds);
    }

    public String getGlossaryImportTemplate() throws AtlasServiceException {
        InputStream inputStream = (InputStream) callAPI(API_V2.GET_IMPORT_GLOSSARY_TEMPLATE, Object.class, null);

        return readStreamContents(inputStream);
    }

    public BulkImportResponse importGlossary(String fileName) throws AtlasServiceException {
        MultiPart multipartEntity = getMultiPartData(fileName);

        return callAPI(API_V2.IMPORT_GLOSSARY, BulkImportResponse.class, multipartEntity);
    }


    @Override
    protected API formatPathParameters(API api, String... params) {
        return new API(String.format(api.getPath(), params), api.getMethod(), api.getExpectedStatus());
    }


    private MultiPart getMultiPartData(String fileName) throws AtlasServiceException {
        try {
            File                             file        = new File(fileName);
            InputStream                      inputStream = new FileInputStream(file);
            final FormDataContentDisposition fd          = FormDataContentDisposition.name("file").fileName(file.getName()).build();

            return new FormDataMultiPart().bodyPart(new StreamDataBodyPart("file", inputStream))
                                          .bodyPart(new FormDataBodyPart(fd, "file"));
        } catch (FileNotFoundException e) {
            throw new AtlasServiceException(e);
        }
    }

    private class ExtractOperation<T, U> {
        T extractElement(U element) {
            return (T) element;
        }
    }

    private String readStreamContents(InputStream inputStream) throws AtlasServiceException {
        try {
            //Converting InputStream to String
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer   sb     = new StringBuffer();
            String template;

            while ((template = reader.readLine()) != null) {
                sb.append(template);
            }

            return sb.toString();
        } catch (Exception e) {
            throw new AtlasServiceException(e);
        }
    }

    private <T, U> List<T> extractResults(ArrayNode jsonResponse, ExtractOperation<T, U> extractInterafce) {
        ArrayList<T> ret = new ArrayList<>();

        for (int index = 0; index < jsonResponse.size(); index++) {
            Object element = jsonResponse.get(index);

            ret.add(extractInterafce.extractElement((U) element));
        }

        return ret;
    }

    private MultivaluedMap<String, String> attributesToQueryParams(Map<String, String> attributes) {
        return attributesToQueryParams(attributes, null);
    }

    private MultivaluedMap<String, String> attributesToQueryParams(Map<String, String>            attributes,
                                                                   MultivaluedMap<String, String> queryParams) {
        if (queryParams == null) {
            queryParams = new MultivaluedMapImpl();
        }

        if (MapUtils.isNotEmpty(attributes)) {
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                queryParams.putSingle(PREFIX_ATTR + e.getKey(), e.getValue());
            }
        }

        return queryParams;
    }

    private MultivaluedMap<String, String> attributesToQueryParams(List<Map<String, String>>      attributesList,
                                                                   MultivaluedMap<String, String> queryParams) {
        if (queryParams == null) {
            queryParams = new MultivaluedMapImpl();
        }

        for (int i = 0; i < attributesList.size(); i++) {
            Map<String, String> attributes = attributesList.get(i);

            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                queryParams.putSingle(PREFIX_ATTR_ + i + ":" + entry.getKey(), entry.getValue());
            }
        }

        return queryParams;
    }

    private <T> T getTypeDefByName(String name, Class<T> typeDefClass) throws AtlasServiceException {
        String pathForType = getPathForType(typeDefClass);
        API    api         = new API(String.format(GET_BY_NAME_TEMPLATE, pathForType, name), HttpMethod.GET, Response.Status.OK);

        return callAPI(api, typeDefClass, null);
    }

    private <T> T getTypeDefByGuid(String guid, Class<T> typeDefClass) throws AtlasServiceException {
        String pathForType = getPathForType(typeDefClass);
        API    api         = new API(String.format(GET_BY_GUID_TEMPLATE, pathForType, guid), HttpMethod.GET, Response.Status.OK);

        return callAPI(api, typeDefClass, null);
    }

    protected  <T> String getPathForType(Class<T> typeDefClass) {
        if (AtlasEnumDef.class.isAssignableFrom(typeDefClass)) {
            return "enumdef";
        } else if (AtlasEntityDef.class.isAssignableFrom(typeDefClass)) {
            return "entitydef";
        } else if (AtlasClassificationDef.class.isAssignableFrom(typeDefClass)) {
            return "classificationdef";
        } else if (AtlasRelationshipDef.class.isAssignableFrom(typeDefClass)) {
            return "relationshipdef";
        } else if (AtlasBusinessMetadataDef.class.isAssignableFrom(typeDefClass)) {
            return "businessmetadatadef";
        }else if (AtlasStructDef.class.isAssignableFrom(typeDefClass)) {
            return "structdef";
        }

        // Code should never reach this point
        return "";
    }

    public static class API_V2 extends API {
        // TypeDef APIs
        public static final API_V2 GET_TYPEDEF_BY_NAME      = new API_V2(TYPEDEF_BY_NAME, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_TYPEDEF_BY_GUID      = new API_V2(TYPEDEF_BY_GUID, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ALL_TYPE_DEFS        = new API_V2(TYPEDEFS_API, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ALL_TYPE_DEF_HEADERS = new API_V2(TYPEDEFS_API + "headers", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 CREATE_TYPE_DEFS         = new API_V2(TYPEDEFS_API, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 UPDATE_TYPE_DEFS         = new API_V2(TYPEDEFS_API, HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 DELETE_TYPE_DEFS         = new API_V2(TYPEDEFS_API, HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_TYPE_DEF_BY_NAME  = new API_V2(TYPEDEF_BY_NAME, HttpMethod.DELETE, Response.Status.NO_CONTENT);

        // Entity APIs
        public static final API_V2 GET_ENTITY_BY_GUID                    = new API_V2(ENTITY_API + "guid/", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ENTITY_BY_UNIQUE_ATTRIBUTE        = new API_V2(ENTITY_API + "uniqueAttribute/type/", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ENTITIES_BY_GUIDS                 = new API_V2(ENTITY_BULK_API, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ENTITIES_BY_UNIQUE_ATTRIBUTE      = new API_V2(ENTITY_BULK_API + "uniqueAttribute/type/", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ENTITY_HEADER_BY_GUID             = new API_V2(ENTITY_API + "guid/%s/header", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_ENTITY_HEADER_BY_UNIQUE_ATTRIBUTE = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/header", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_AUDIT_EVENTS                      = new API_V2(ENTITY_API + "%s/audit", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 CREATE_ENTITY                         = new API_V2(ENTITY_API, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 CREATE_ENTITIES                       = new API_V2(ENTITY_BULK_API, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 UPDATE_ENTITY                         = new API_V2(ENTITY_API, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 UPDATE_ENTITY_BY_ATTRIBUTE            = new API_V2(ENTITY_API + "uniqueAttribute/type/", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 UPDATE_ENTITIES                       = new API_V2(ENTITY_BULK_API, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 PARTIAL_UPDATE_ENTITY_BY_GUID         = new API_V2(ENTITY_API + "guid/%s", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 DELETE_ENTITY_BY_GUID                 = new API_V2(ENTITY_API + "guid/", HttpMethod.DELETE, Response.Status.OK);
        public static final API_V2 DELETE_ENTITY_BY_ATTRIBUTE            = new API_V2(ENTITY_API + "uniqueAttribute/type/", HttpMethod.DELETE, Response.Status.OK);
        public static final API_V2 DELETE_ENTITIES_BY_GUIDS              = new API_V2(ENTITY_BULK_API, HttpMethod.DELETE, Response.Status.OK);
        public static final API_V2 PURGE_ENTITIES_BY_GUIDS               = new API_V2(ENTITY_PURGE_API, HttpMethod.PUT, Response.Status.OK);

        // Entity-classification APIs
        public static final API_V2 GET_CLASSIFICATIONS                         = new API_V2(ENTITY_API + "guid/%s/classifications", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_FROM_CLASSIFICATION                     = new API_V2(ENTITY_API + "guid/%s/classification/%s", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 ADD_CLASSIFICATIONS                         = new API_V2(ENTITY_API + "guid/%s/classifications", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 ADD_CLASSIFICATION                          = new API_V2(ENTITY_BULK_API + "/classification", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 ADD_CLASSIFICATION_BY_TYPE_AND_ATTRIBUTE    = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/classifications", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 UPDATE_CLASSIFICATIONS                      = new API_V2(ENTITY_API + "guid/%s/classifications", HttpMethod.PUT, Response.Status.NO_CONTENT);
        public static final API_V2 UPDATE_CLASSIFICATION_BY_TYPE_AND_ATTRIBUTE = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/classifications", HttpMethod.PUT, Response.Status.NO_CONTENT);
        public static final API_V2 UPDATE_BULK_SET_CLASSIFICATIONS             = new API_V2(ENTITY_API + AtlasClientV2.BULK_SET_CLASSIFICATIONS, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 DELETE_CLASSIFICATION                       = new API_V2(ENTITY_API + "guid/%s/classification/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_CLASSIFICATION_BY_TYPE_AND_ATTRIBUTE = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/classification/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 GET_BULK_HEADERS                            = new API_V2(ENTITY_API + BULK_HEADERS, HttpMethod.GET, Response.Status.OK);

        // business-attributes APIs
        public static final API_V2 ADD_BUSINESS_ATTRIBUTE            = new API_V2(ENTITY_API + "guid/%s/businessmetadata", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 ADD_BUSINESS_ATTRIBUTE_BY_NAME    = new API_V2(ENTITY_API + "guid/%s/businessmetadata/%s", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_BUSINESS_ATTRIBUTE         = new API_V2(ENTITY_API + "guid/%s/businessmetadata", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_BUSINESS_ATTRIBUTE_BY_NAME = new API_V2(ENTITY_API + "guid/%s/businessmetadata/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 GET_BUSINESS_METADATA_TEMPLATE    = new API_V2(ENTITY_API + "businessmetadata/import/template", HttpMethod.GET, Response.Status.OK, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM);
        public static final API_V2 IMPORT_BUSINESS_METADATA          = new API_V2(ENTITY_API + "businessmetadata/import", HttpMethod.POST, Response.Status.OK, MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON);

        // labels APIs
        public static final API_V2 ADD_LABELS                        = new API_V2(ENTITY_API + "guid/%s/labels", HttpMethod.PUT, Response.Status.NO_CONTENT);
        public static final API_V2 ADD_LABELS_BY_UNIQUE_ATTRIBUTE    = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/labels", HttpMethod.PUT, Response.Status.NO_CONTENT);
        public static final API_V2 SET_LABELS                        = new API_V2(ENTITY_API + "guid/%s/labels", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 SET_LABELS_BY_UNIQUE_ATTRIBUTE    = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/labels", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_LABELS                     = new API_V2(ENTITY_API + "guid/%s/labels", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_LABELS_BY_UNIQUE_ATTRIBUTE = new API_V2(ENTITY_API + "uniqueAttribute/type/%s/labels", HttpMethod.DELETE, Response.Status.NO_CONTENT);

        // Lineage APIs
        public static final API_V2 LINEAGE_INFO                = new API_V2(LINEAGE_URI, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_LINEAGE_BY_ATTRIBUTES   = new API_V2(LINEAGE_URI + "uniqueAttribute/type/", HttpMethod.GET, Response.Status.OK);

        // Discovery APIs
        public static final API_V2 DSL_SEARCH                  = new API_V2(DSL_SEARCH_URI, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 FULL_TEXT_SEARCH            = new API_V2(FULL_TEXT_SEARCH_URI, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 BASIC_SEARCH                = new API_V2(BASIC_SEARCH_URI, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 FACETED_SEARCH              = new API_V2(FACETED_SEARCH_URI, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 ATTRIBUTE_SEARCH            = new API_V2(DISCOVERY_URI+ "/attribute", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 RELATIONSHIP_SEARCH         = new API_V2(DISCOVERY_URI+ "/relationship", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 QUICK_SEARCH_WITH_GET       = new API_V2(QUICK_SEARCH_URI, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 QUICK_SEARCH_WITH_POST      = new API_V2(QUICK_SEARCH_URI, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 GET_SUGGESTIONS             = new API_V2(DISCOVERY_URI+ "/suggestions", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_SAVED_SEARCHES          = new API_V2(SAVED_SEARCH_URI, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_SAVED_SEARCH            = new API_V2(SAVED_SEARCH_URI+ "/%s", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 ADD_SAVED_SEARCH            = new API_V2(SAVED_SEARCH_URI, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 UPDATE_SAVED_SEARCH         = new API_V2(SAVED_SEARCH_URI, HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 DELETE_SAVED_SEARCH         = new API_V2(SAVED_SEARCH_URI+ "/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 EXECUTE_SAVED_SEARCH_BY_NAME = new API_V2(SAVED_SEARCH_URI+"/execute/%s", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 EXECUTE_SAVED_SEARCH_BY_GUID = new API_V2(SAVED_SEARCH_URI+"/execute/guid/%s", HttpMethod.GET, Response.Status.OK);

        // Relationship APIs
        public static final API_V2 GET_RELATIONSHIP_BY_GUID    = new API_V2(RELATIONSHIPS_URI + "guid/", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 CREATE_RELATIONSHIP         = new API_V2(RELATIONSHIPS_URI , HttpMethod.POST, Response.Status.OK);
        public static final API_V2 UPDATE_RELATIONSHIP         = new API_V2(RELATIONSHIPS_URI , HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 DELETE_RELATIONSHIP_BY_GUID = new API_V2(RELATIONSHIPS_URI + "guid/", HttpMethod.DELETE, Response.Status.NO_CONTENT);

        // Admin APIs
        public static final API_V2 GET_ATLAS_AUDITS            = new API_V2(ATLAS_AUDIT_API, HttpMethod.POST, Response.Status.OK);

        // Glossary APIs
        public static final API_V2 GET_ALL_GLOSSARIES              = new API_V2(GLOSSARY_URI, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_BY_GUID            = new API_V2(GLOSSARY_URI + "/%s", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_DETAILED_GLOSSARY           = new API_V2(GLOSSARY_URI + "/%s/detailed", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_TERM               = new API_V2(GLOSSARY_TERM, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_TERMS              = new API_V2(GLOSSARY_URI + "/%s/terms", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_TERMS_HEADERS      = new API_V2(GLOSSARY_URI + "/%s/terms/headers", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_CATEGORY           = new API_V2(GLOSSARY_CATEGORY, HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_CATEGORIES         = new API_V2(GLOSSARY_URI + "/%s/categories", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_GLOSSARY_CATEGORIES_HEADERS = new API_V2(GLOSSARY_URI + "/%s/categories/headers", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_CATEGORY_TERMS              = new API_V2(GLOSSARY_CATEGORY + "/%s/terms", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_RELATED_TERMS               = new API_V2(GLOSSARY_TERMS + "/%s/related", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 GET_RELATED_CATEGORIES          = new API_V2(GLOSSARY_CATEGORY + "/%s/related", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 CREATE_GLOSSARY                 = new API_V2(GLOSSARY_URI, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 CREATE_GLOSSARY_TERM            = new API_V2(GLOSSARY_TERM, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 CREATE_GLOSSARY_TERMS           = new API_V2(GLOSSARY_TERMS, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 CREATE_GLOSSARY_CATEGORY        = new API_V2(GLOSSARY_CATEGORY, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 CREATE_GLOSSARY_CATEGORIES      = new API_V2(GLOSSARY_CATEGORIES, HttpMethod.POST, Response.Status.OK);
        public static final API_V2 UPDATE_GLOSSARY_BY_GUID         = new API_V2(GLOSSARY_URI + "/%s", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 UPDATE_PARTIAL_GLOSSARY         = new API_V2(GLOSSARY_URI + "/%s/partial", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 UPDATE_GLOSSARY_TERM            = new API_V2(GLOSSARY_TERM + "/%s", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 UPDATE_PARTIAL_TERM             = new API_V2(GLOSSARY_TERM + "/%s/partial", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 UPDATE_CATEGORY_BY_GUID         = new API_V2(GLOSSARY_CATEGORY + "/%s", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 UPDATE_PARTIAL_CATEGORY         = new API_V2(GLOSSARY_CATEGORY + "/%s/partial", HttpMethod.PUT, Response.Status.OK);
        public static final API_V2 DELETE_GLOSSARY_BY_GUID         = new API_V2(GLOSSARY_URI+ "/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_TERM_BY_GUID             = new API_V2(GLOSSARY_TERM+ "/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 DELETE_CATEGORY_BY_GUID         = new API_V2(GLOSSARY_CATEGORY+ "/%s", HttpMethod.DELETE, Response.Status.NO_CONTENT);
        public static final API_V2 GET_ENTITIES_ASSIGNED_WITH_TERM = new API_V2(GLOSSARY_TERMS + "/%s/assignedEntities", HttpMethod.GET, Response.Status.OK);
        public static final API_V2 ASSIGN_TERM_TO_ENTITIES         = new API_V2(GLOSSARY_TERMS + "/%s/assignedEntities", HttpMethod.POST, Response.Status.NO_CONTENT);
        public static final API_V2 DISASSOCIATE_TERM_FROM_ENTITIES = new API_V2(GLOSSARY_TERMS + "/%s/assignedEntities", HttpMethod.PUT, Response.Status.NO_CONTENT);
        public static final API_V2 GET_IMPORT_GLOSSARY_TEMPLATE    = new API_V2(GLOSSARY_URI + "/import/template", HttpMethod.GET, Response.Status.OK, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM);
        public static final API_V2 IMPORT_GLOSSARY                 = new API_V2(GLOSSARY_URI + "/import", HttpMethod.POST, Response.Status.OK, MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_JSON);

        private API_V2(String path, String method, Response.Status status) {
            super(path, method, status);
        }

        private API_V2(String path, String method, Response.Status status, String consumes, String produces) {
            super(path, method, status, consumes, produces);
        }
    }
}
