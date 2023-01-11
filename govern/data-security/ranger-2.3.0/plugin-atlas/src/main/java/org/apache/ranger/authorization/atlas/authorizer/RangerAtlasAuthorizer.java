/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.authorization.atlas.authorizer;


import org.apache.atlas.authorize.AtlasAdminAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizationException;
import org.apache.atlas.authorize.AtlasEntityAccessRequest;
import org.apache.atlas.authorize.AtlasSearchResultScrubRequest;
import org.apache.atlas.authorize.AtlasRelationshipAccessRequest;
import org.apache.atlas.authorize.AtlasTypesDefFilterRequest;
import org.apache.atlas.authorize.AtlasTypeAccessRequest;
import org.apache.atlas.authorize.AtlasAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizer;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.ranger.services.atlas.RangerServiceAtlas.*;


public class RangerAtlasAuthorizer implements AtlasAuthorizer {
    private static final Logger LOG      = LoggerFactory.getLogger(RangerAtlasAuthorizer.class);
    private static final Logger PERF_LOG = RangerPerfTracer.getPerfLogger("atlasauth.request");

    private static final  Set<AtlasPrivilege> CLASSIFICATION_PRIVILEGES = new HashSet<AtlasPrivilege>() {{
        add(AtlasPrivilege.ENTITY_ADD_CLASSIFICATION);
        add(AtlasPrivilege.ENTITY_REMOVE_CLASSIFICATION);
        add(AtlasPrivilege.ENTITY_UPDATE_CLASSIFICATION);
    }};

    private static volatile RangerBasePlugin atlasPlugin = null;

    @Override
    public void init() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> RangerAtlasPlugin.init()");
        }

        RangerBasePlugin plugin = atlasPlugin;

        if (plugin == null) {
            synchronized (RangerAtlasPlugin.class) {
                plugin = atlasPlugin;

                if (plugin == null) {
                    plugin = new RangerAtlasPlugin();

                    plugin.init();

                    plugin.setResultProcessor(new RangerDefaultAuditHandler(plugin.getConfig()));

                    atlasPlugin = plugin;
                }
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== RangerAtlasPlugin.init()");
        }
    }

    @Override
    public void cleanUp() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> cleanUp ");
        }
    }

    @Override
    public boolean isAccessAllowed(AtlasAdminAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isAccessAllowed(" + request + ")");
        }

        final boolean    ret;
        RangerPerfTracer perf = null;

        try {
            if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerAtlasAuthorizer.isAccessAllowed(" + request + ")");
            }

            String                   action         = request.getAction() != null ? request.getAction().getType() : null;
            RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl(Collections.singletonMap(RESOURCE_SERVICE, "*"));
            RangerAccessRequestImpl  rangerRequest  = new RangerAccessRequestImpl(rangerResource, action, request.getUser(), request.getUserGroups(), null);

            rangerRequest.setClientIPAddress(request.getClientIPAddress());
            rangerRequest.setAccessTime(request.getAccessTime());
            rangerRequest.setAction(action);
            rangerRequest.setForwardedAddresses(request.getForwardedAddresses());
            rangerRequest.setRemoteIPAddress(request.getRemoteIPAddress());


            ret = checkAccess(rangerRequest);
        } finally {
            RangerPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isAccessAllowed(" + request + "): " + ret);
        }

        return ret;
    }

    @Override
    public boolean isAccessAllowed(AtlasEntityAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isAccessAllowed(" + request + ")");
        }

        boolean                 ret          = true;
        RangerPerfTracer        perf         = null;
        RangerAtlasAuditHandler auditHandler = null;

        try {
            if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerAtlasAuthorizer.isAccessAllowed(" + request + ")");
            }

            // not initializing audit handler, so that audits are not logged when entity details are NULL or EMPTY STRING
            if (!(StringUtils.isEmpty(request.getEntityId()) && request.getClassification() == null && request.getEntity() == null)) {
                auditHandler = new RangerAtlasAuditHandler(request, getServiceDef());
            }

            ret = isAccessAllowed(request, auditHandler);
        } finally {
            RangerPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isAccessAllowed(" + request + "): " + ret);
        }

        return ret;
    }

    @Override
    public boolean isAccessAllowed(AtlasTypeAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isAccessAllowed(" + request + ")");
        }

        final boolean    ret;
        RangerPerfTracer perf = null;

        try {
            if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerAtlasAuthorizer.isAccessAllowed(" + request + ")");
            }

            final String typeName     = request.getTypeDef() != null ? request.getTypeDef().getName() : null;
            final String typeCategory = request.getTypeDef() != null && request.getTypeDef().getCategory() != null ? request.getTypeDef().getCategory().name() : null;
            final String action       = request.getAction() != null ? request.getAction().getType() : null;

            RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();

            rangerResource.setValue(RESOURCE_TYPE_NAME, typeName);
            rangerResource.setValue(RESOURCE_TYPE_CATEGORY, typeCategory);

            RangerAccessRequestImpl rangerRequest = new RangerAccessRequestImpl(rangerResource, action, request.getUser(), request.getUserGroups(), null);
            rangerRequest.setClientIPAddress(request.getClientIPAddress());
            rangerRequest.setAccessTime(request.getAccessTime());
            rangerRequest.setAction(action);
            rangerRequest.setForwardedAddresses(request.getForwardedAddresses());
            rangerRequest.setRemoteIPAddress(request.getRemoteIPAddress());

            boolean isAuditDisabled = ACCESS_TYPE_TYPE_READ.equalsIgnoreCase(action);

            if (isAuditDisabled) {
                ret = checkAccess(rangerRequest, null);
            } else {
                ret = checkAccess(rangerRequest);
            }

        } finally {
            RangerPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isAccessAllowed(" + request + "): " + ret);
        }

        return ret;
    }



    public boolean isAccessAllowed(AtlasRelationshipAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isAccessAllowed(" + request + ")");
        }

        boolean ret;
        RangerPerfTracer perf = null;

        try {
            if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerAtlasAuthorizer.isAccessAllowed(" + request + ")");
            }

            final String      action                      = request.getAction() != null ? request.getAction().getType() : null;
            final Set<String> end1EntityTypeAndSuperTypes = request.getEnd1EntityTypeAndAllSuperTypes();
            final Set<String> end1Classifications         = new HashSet<>(request.getEnd1EntityClassifications());
            final String      end1EntityId                = request.getEnd1EntityId();

            final Set<String> end2EntityTypeAndSuperTypes = request.getEnd2EntityTypeAndAllSuperTypes();
            final Set<String> end2Classifications         = new HashSet<>(request.getEnd2EntityClassifications());
            final String      end2EntityId                = request.getEnd2EntityId();


            String relationShipType = request.getRelationshipType();

            RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();

            RangerAccessRequestImpl rangerRequest = new RangerAccessRequestImpl(rangerResource, action, request.getUser(), request.getUserGroups(), null);
            rangerRequest.setClientIPAddress(request.getClientIPAddress());
            rangerRequest.setAccessTime(request.getAccessTime());
            rangerRequest.setAction(action);
            rangerRequest.setForwardedAddresses(request.getForwardedAddresses());
            rangerRequest.setRemoteIPAddress(request.getRemoteIPAddress());

            rangerResource.setValue(RESOURCE_RELATIONSHIP_TYPE, relationShipType);


            Set<String> classificationsWithSuperTypesEnd1 = new HashSet();

            for (String classificationToAuthorize : end1Classifications) {
                classificationsWithSuperTypesEnd1.addAll(request.getClassificationTypeAndAllSuperTypes(classificationToAuthorize));
            }

            rangerResource.setValue(RESOURCE_END_ONE_ENTITY_TYPE, end1EntityTypeAndSuperTypes);
            rangerResource.setValue(RESOURCE_END_ONE_ENTITY_CLASSIFICATION, classificationsWithSuperTypesEnd1);
            rangerResource.setValue(RESOURCE_END_ONE_ENTITY_ID, end1EntityId);


            Set<String> classificationsWithSuperTypesEnd2 = new HashSet();

            for (String classificationToAuthorize : end2Classifications) {
                classificationsWithSuperTypesEnd2.addAll(request.getClassificationTypeAndAllSuperTypes(classificationToAuthorize));
            }

            rangerResource.setValue(RESOURCE_END_TWO_ENTITY_TYPE, end2EntityTypeAndSuperTypes);
            rangerResource.setValue(RESOURCE_END_TWO_ENTITY_CLASSIFICATION, classificationsWithSuperTypesEnd2);
            rangerResource.setValue(RESOURCE_END_TWO_ENTITY_ID, end2EntityId);

            ret = checkAccess(rangerRequest);

        } finally {
            RangerPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isAccessAllowed(" + request + "): " + ret);
        }

        return ret;
    }


    @Override
    public void scrubSearchResults(AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> scrubSearchResults(" + request + ")");
        }

        RangerPerfTracer perf = null;

        try {
            if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerAtlasAuthorizer.scrubSearchResults(" + request + ")");
            }

            final AtlasSearchResult result = request.getSearchResult();

            if (CollectionUtils.isNotEmpty(result.getEntities())) {
                for (AtlasEntityHeader entity : result.getEntities()) {
                    checkAccessAndScrub(entity, request);
                }
            }

            if (CollectionUtils.isNotEmpty(result.getFullTextResult())) {
                for (AtlasSearchResult.AtlasFullTextResult fullTextResult : result.getFullTextResult()) {
                    if (fullTextResult != null) {
                        checkAccessAndScrub(fullTextResult.getEntity(), request);
                    }
                }
            }

            if (MapUtils.isNotEmpty(result.getReferredEntities())) {
                for (AtlasEntityHeader entity : result.getReferredEntities().values()) {
                    checkAccessAndScrub(entity, request);
                }
            }
        } finally {
            RangerPerfTracer.log(perf);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== scrubSearchResults(): " + request);
        }
    }

    @Override
    public void filterTypesDef(AtlasTypesDefFilterRequest request) throws AtlasAuthorizationException {

        AtlasTypesDef typesDef = request.getTypesDef();

        filterTypes(request, typesDef.getEnumDefs());
        filterTypes(request, typesDef.getStructDefs());
        filterTypes(request, typesDef.getEntityDefs());
        filterTypes(request, typesDef.getClassificationDefs());
        filterTypes(request, typesDef.getRelationshipDefs());
        filterTypes(request, typesDef.getBusinessMetadataDefs());

    }

    private void filterTypes(AtlasAccessRequest request, List<? extends AtlasBaseTypeDef> typeDefs)throws AtlasAuthorizationException {
        if (typeDefs != null) {
            for (ListIterator<? extends AtlasBaseTypeDef> iter = typeDefs.listIterator(); iter.hasNext();) {
                AtlasBaseTypeDef       typeDef     = iter.next();
                AtlasTypeAccessRequest typeRequest = new AtlasTypeAccessRequest(request.getAction(), typeDef, request.getUser(), request.getUserGroups());

                typeRequest.setClientIPAddress(request.getClientIPAddress());
                typeRequest.setForwardedAddresses(request.getForwardedAddresses());
                typeRequest.setRemoteIPAddress(request.getRemoteIPAddress());

                if (!isAccessAllowed(typeRequest)) {
                    iter.remove();
                }
            }
        }
    }


    private RangerServiceDef getServiceDef() {
        RangerBasePlugin plugin = atlasPlugin;

        return plugin != null ? plugin.getServiceDef() : null;
    }

    private boolean isAccessAllowed(AtlasEntityAccessRequest request, RangerAtlasAuditHandler auditHandler) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> isAccessAllowed(" + request + ")");
        }

        boolean ret = false;

        try {
            final String                   action         = request.getAction() != null ? request.getAction().getType() : null;
            final Set<String>              entityTypes    = request.getEntityTypeAndAllSuperTypes();
            final String                   entityId       = request.getEntityId();
            final String                   classification = request.getClassification() != null ? request.getClassification().getTypeName() : null;
            final RangerAccessRequestImpl  rangerRequest  = new RangerAccessRequestImpl();
            final RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();
            final String                   ownerUser      = request.getEntity() != null ? (String) request.getEntity().getAttribute(RESOURCE_ENTITY_OWNER) : null;

            rangerResource.setValue(RESOURCE_ENTITY_TYPE, entityTypes);
            rangerResource.setValue(RESOURCE_ENTITY_ID, entityId);
            rangerResource.setOwnerUser(ownerUser);
            rangerRequest.setAccessType(action);
            rangerRequest.setAction(action);
            rangerRequest.setUser(request.getUser());
            rangerRequest.setUserGroups(request.getUserGroups());
            rangerRequest.setClientIPAddress(request.getClientIPAddress());
            rangerRequest.setAccessTime(request.getAccessTime());
            rangerRequest.setResource(rangerResource);
            rangerRequest.setForwardedAddresses(request.getForwardedAddresses());
            rangerRequest.setRemoteIPAddress(request.getRemoteIPAddress());

            if (AtlasPrivilege.ENTITY_ADD_LABEL.equals(request.getAction()) || AtlasPrivilege.ENTITY_REMOVE_LABEL.equals(request.getAction())) {
                rangerResource.setValue(RESOURCE_ENTITY_LABEL, request.getLabel());
            } else if (AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA.equals(request.getAction())) {
                rangerResource.setValue(RESOURCE_ENTITY_BUSINESS_METADATA, request.getBusinessMetadata());
            } else if (StringUtils.isNotEmpty(classification) && CLASSIFICATION_PRIVILEGES.contains(request.getAction())) {
                rangerResource.setValue(RESOURCE_CLASSIFICATION, request.getClassificationTypeAndAllSuperTypes(classification));
            }

            if (CollectionUtils.isNotEmpty(request.getEntityClassifications())) {
                // check authorization for each classification
                for (String classificationToAuthorize : request.getEntityClassifications()) {
                    rangerResource.setValue(RESOURCE_ENTITY_CLASSIFICATION, request.getClassificationTypeAndAllSuperTypes(classificationToAuthorize));

                    ret = checkAccess(rangerRequest, auditHandler);

                    if (!ret) {
                        break;
                    }
                }
            } else {
                rangerResource.setValue(RESOURCE_ENTITY_CLASSIFICATION, ENTITY_NOT_CLASSIFIED );

                ret = checkAccess(rangerRequest, auditHandler);
            }

        } finally {
            if(auditHandler != null) {
                auditHandler.flushAudit();
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== isAccessAllowed(" + request + "): " + ret);
        }

        return ret;
    }

    private boolean checkAccess(RangerAccessRequestImpl request) {
        boolean          ret    = false;
        RangerBasePlugin plugin = atlasPlugin;

        if (plugin != null) {
            RangerAccessResult result = plugin.isAccessAllowed(request);

            ret = result != null && result.getIsAllowed();
        } else {
            LOG.warn("RangerAtlasPlugin not initialized. Access blocked!!!");
        }

        return ret;
    }

    private boolean checkAccess(RangerAccessRequestImpl request, RangerAtlasAuditHandler auditHandler) {
        boolean          ret    = false;
        RangerBasePlugin plugin = atlasPlugin;

        if (plugin != null) {
            RangerAccessResult result = plugin.isAccessAllowed(request, auditHandler);

            ret = result != null && result.getIsAllowed();
        } else {
            LOG.warn("RangerAtlasPlugin not initialized. Access blocked!!!");
        }

        return ret;
    }

    private void checkAccessAndScrub(AtlasEntityHeader entity, AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {
        if (entity != null && request != null) {
            final AtlasEntityAccessRequest entityAccessRequest = new AtlasEntityAccessRequest(request.getTypeRegistry(), AtlasPrivilege.ENTITY_READ, entity, request.getUser(), request.getUserGroups());

            entityAccessRequest.setClientIPAddress(request.getClientIPAddress());
            entityAccessRequest.setForwardedAddresses(request.getForwardedAddresses());
            entityAccessRequest.setRemoteIPAddress(request.getRemoteIPAddress());

            if (!isAccessAllowed(entityAccessRequest, null)) {
                scrubEntityHeader(entity);
            }
        }
    }

    class RangerAtlasPlugin extends RangerBasePlugin {
        RangerAtlasPlugin() {
            super("atlas", "atlas");
        }
    }

    class RangerAtlasAuditHandler extends RangerDefaultAuditHandler {
        private final Map<String, AuthzAuditEvent> auditEvents;
        private final String                       resourcePath;
        private       boolean                      denyExists = false;

        public RangerAtlasAuditHandler(AtlasEntityAccessRequest request, RangerServiceDef serviceDef) {
            Collection<String> classifications    = request.getEntityClassifications();
            String             strClassifications = classifications == null ? "[]" : classifications.toString();

            if (request.getClassification() != null) {
                strClassifications += ("," + request.getClassification().getTypeName());
            }

            RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();

            rangerResource.setServiceDef(serviceDef);
            rangerResource.setValue(RESOURCE_ENTITY_TYPE, request.getEntityType());
            rangerResource.setValue(RESOURCE_ENTITY_CLASSIFICATION, strClassifications);
            rangerResource.setValue(RESOURCE_ENTITY_ID, request.getEntityId());

            if (AtlasPrivilege.ENTITY_ADD_LABEL.equals(request.getAction()) || AtlasPrivilege.ENTITY_REMOVE_LABEL.equals(request.getAction())) {
                rangerResource.setValue(RESOURCE_ENTITY_LABEL, "label=" + request.getLabel());
            } else if (AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA.equals(request.getAction())) {
                rangerResource.setValue(RESOURCE_ENTITY_BUSINESS_METADATA, "business-metadata=" + request.getBusinessMetadata());
            }

            auditEvents  = new HashMap<>();
            resourcePath = rangerResource.getAsString();
        }

        @Override
        public void processResult(RangerAccessResult result) {
            if (denyExists) { // nothing more to do, if a deny already encountered
                return;
            }

            AuthzAuditEvent auditEvent = super.getAuthzEvents(result);

            if (auditEvent != null) {
                // audit event might have list of entity-types and classification-types; overwrite with the values in original request
                if (resourcePath != null) {
                    auditEvent.setResourcePath(resourcePath);
                }

                if (!result.getIsAllowed()) {
                    denyExists = true;

                    auditEvents.clear();
                }

                auditEvents.put(auditEvent.getPolicyId() + auditEvent.getAccessType(), auditEvent);
            }
        }


        public void flushAudit() {
            if (auditEvents != null) {
                for (AuthzAuditEvent auditEvent : auditEvents.values()) {
                    logAuthzAudit(auditEvent);
                }
            }
        }
    }
}
