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

package org.apache.atlas.authorize.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.authorize.*;
import org.apache.atlas.authorize.simple.AtlasSimpleAuthzPolicy.*;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult.AtlasFullTextResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.utils.AtlasJson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.atlas.authorize.AtlasPrivilege.ENTITY_ADD_CLASSIFICATION;
import static org.apache.atlas.authorize.AtlasPrivilege.ENTITY_REMOVE_CLASSIFICATION;
import static org.apache.atlas.authorize.AtlasPrivilege.ENTITY_UPDATE_CLASSIFICATION;
import static org.apache.atlas.authorize.AtlasPrivilege.TYPE_CREATE;
import static org.apache.atlas.authorize.AtlasPrivilege.TYPE_DELETE;
import static org.apache.atlas.authorize.AtlasPrivilege.TYPE_READ;
import static org.apache.atlas.authorize.AtlasPrivilege.TYPE_UPDATE;


public final class AtlasSimpleAuthorizer implements AtlasAuthorizer {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasSimpleAuthorizer.class);

    private final static String WILDCARD_ASTERISK = "*";

    private final static Set<AtlasPrivilege> CLASSIFICATION_PRIVILEGES = new HashSet<AtlasPrivilege>() {{
                                                                                add(ENTITY_ADD_CLASSIFICATION);
                                                                                add(ENTITY_REMOVE_CLASSIFICATION);
                                                                                add(ENTITY_UPDATE_CLASSIFICATION);
                                                                            }};

    private AtlasSimpleAuthzPolicy authzPolicy;

    private final static String REGEX_MATCHALL = ".*";

    public AtlasSimpleAuthorizer() {
    }

    @Override
    public void init() {
        LOG.info("==> SimpleAtlasAuthorizer.init()");

        InputStream inputStream = null;

        try {
            inputStream = ApplicationProperties.getFileAsInputStream(ApplicationProperties.get(), "atlas.authorizer.simple.authz.policy.file", "atlas-simple-authz-policy.json");

            authzPolicy = AtlasJson.fromJson(inputStream, AtlasSimpleAuthzPolicy.class);

            addImpliedTypeReadPrivilege(authzPolicy);
        } catch (IOException | AtlasException e) {
            LOG.error("SimpleAtlasAuthorizer.init(): initialization failed", e);

            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException excp) {
                    // ignore
                }
            }
        }

        LOG.info("<== SimpleAtlasAuthorizer.init()");
    }

    @Override
    public void cleanUp() {
        LOG.info("==> SimpleAtlasAuthorizer.cleanUp()");

        authzPolicy = null;

        LOG.info("<== SimpleAtlasAuthorizer.cleanUp()");
    }

    @Override
    public boolean isAccessAllowed(AtlasAdminAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SimpleAtlasAuthorizer.isAccessAllowed({})", request);
        }

        boolean ret = false;

        Set<String> roles = getRoles(request.getUser(), request.getUserGroups());

        for (String role : roles) {
            List<AtlasAdminPermission> permissions = getAdminPermissionsForRole(role);

            if (permissions != null) {
                final String action = request.getAction() != null ? request.getAction().getType() : null;

                for (AtlasAdminPermission permission : permissions) {
                    if (isMatch(action, permission.getPrivileges())) {
                        ret = true;

                        break;
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== SimpleAtlasAuthorizer.isAccessAllowed({}): {}", request, ret);
        }

        return ret;
    }

    @Override
    public boolean isAccessAllowed(AtlasTypeAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SimpleAtlasAuthorizer.isAccessAllowed({})", request);
        }

        boolean ret = false;

        Set<String> roles = getRoles(request.getUser(), request.getUserGroups());

        for (String role : roles) {
            List<AtlasTypePermission> permissions = getTypePermissionsForRole(role);

            if (permissions != null) {
                final String action       = request.getAction() != null ? request.getAction().getType() : null;
                final String typeCategory = request.getTypeDef() != null ? request.getTypeDef().getCategory().name() : null;
                final String typeName     = request.getTypeDef() != null ? request.getTypeDef().getName() : null;

                for (AtlasTypePermission permission : permissions) {
                    if (isMatch(action, permission.getPrivileges()) &&
                        isMatch(typeCategory, permission.getTypeCategories()) &&
                        isMatch(typeName, permission.getTypeNames())) {
                        ret = true;

                        break;
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== SimpleAtlasAuthorizer.isAccessAllowed({}): {}", request, ret);
        }

        return ret;
    }

    @Override
    public boolean isAccessAllowed(AtlasRelationshipAccessRequest request) throws AtlasAuthorizationException {
        final Set<String> roles                       = getRoles(request.getUser(), request.getUserGroups());
        final String      relationShipType            = request.getRelationshipType();
        final Set<String> end1EntityTypeAndSuperTypes = request.getEnd1EntityTypeAndAllSuperTypes();
        final Set<String> end1Classifications         = new HashSet<>(request.getEnd1EntityClassifications());
        final String      end1EntityId                = request.getEnd1EntityId();
        final Set<String> end2EntityTypeAndSuperTypes = request.getEnd2EntityTypeAndAllSuperTypes();
        final Set<String> end2Classifications         = new HashSet<>(request.getEnd2EntityClassifications());
        final String      end2EntityId                = request.getEnd2EntityId();
        final String      action                      = request.getAction() != null ? request.getAction().getType() : null;

        boolean hasEnd1EntityAccess = false;
        boolean hasEnd2EntityAccess = false;

        for (String role : roles) {
            final List<AtlasRelationshipPermission> permissions = getRelationshipPermissionsForRole(role);

            if (permissions == null) {
                continue;
            }

            for (AtlasRelationshipPermission permission : permissions) {
                if (isMatch(relationShipType, permission.getRelationshipTypes()) && isMatch(action, permission.getPrivileges())) {
                    //End1 permission check
                    if (!hasEnd1EntityAccess) {
                         if (isMatchAny(end1EntityTypeAndSuperTypes, permission.getEnd1EntityType()) && isMatch(end1EntityId, permission.getEnd1EntityId())) {
                             for (Iterator<String> iter = end1Classifications.iterator(); iter.hasNext();) {
                                 String entityClassification = iter.next();

                                 if (isMatchAny(request.getClassificationTypeAndAllSuperTypes(entityClassification), permission.getEnd1EntityClassification())) {
                                     iter.remove();
                                 }
                             }

                             hasEnd1EntityAccess = CollectionUtils.isEmpty(end1Classifications);
                        }
                    }

                    //End2 permission chech
                    if (!hasEnd2EntityAccess) {
                        if (isMatchAny(end2EntityTypeAndSuperTypes, permission.getEnd2EntityType()) && isMatch(end2EntityId, permission.getEnd2EntityId())) {
                            for (Iterator<String> iter = end2Classifications.iterator(); iter.hasNext();) {
                                String entityClassification = iter.next();

                                if (isMatchAny(request.getClassificationTypeAndAllSuperTypes(entityClassification), permission.getEnd2EntityClassification())) {
                                    iter.remove();
                                }
                            }

                            hasEnd2EntityAccess = CollectionUtils.isEmpty(end2Classifications);
                        }
                    }
                }
            }
        }

        return hasEnd1EntityAccess && hasEnd2EntityAccess;
    }

    @Override
    public boolean isAccessAllowed(AtlasEntityAccessRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SimpleAtlasAuthorizer.isAccessAllowed({})", request);
        }

        boolean           ret            = false;
        final String      action         = request.getAction() != null ? request.getAction().getType() : null;
        final Set<String> entityTypes    = request.getEntityTypeAndAllSuperTypes();
        final String      entityId       = request.getEntityId();
        final String      attribute      = request.getAttributeName();
        final Set<String> entClsToAuthz  = new HashSet<>(request.getEntityClassifications());
        final Set<String> roles          = getRoles(request.getUser(), request.getUserGroups());

        for (String role : roles) {
            List<AtlasEntityPermission> permissions = getEntityPermissionsForRole(role);

            if (permissions != null) {
                for (AtlasEntityPermission permission : permissions) {
                    if (isMatch(action, permission.getPrivileges()) && isMatchAny(entityTypes, permission.getEntityTypes()) &&
                        isMatch(entityId, permission.getEntityIds()) && isMatch(attribute, permission.getAttributes()) &&
                        isLabelMatch(request, permission) && isBusinessMetadataMatch(request, permission) && isClassificationMatch(request, permission)) {

                        // 1. entity could have multiple classifications
                        // 2. access for these classifications could be granted by multiple AtlasEntityPermission entries
                        // 3. classifications allowed by the current permission will be removed from entClsToAuthz
                        // 4. request will be allowed once entClsToAuthz is empty i.e. user has permission for all classifications
                        for (Iterator<String> iter = entClsToAuthz.iterator(); iter.hasNext(); ) {
                            String entityClassification = iter.next();

                            if (isMatchAny(request.getClassificationTypeAndAllSuperTypes(entityClassification), permission.getEntityClassifications())) {
                                iter.remove();
                            }
                        }

                        ret = CollectionUtils.isEmpty(entClsToAuthz);

                        if (ret) {
                            break;
                        }
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            if (!ret) {
                LOG.debug("isAccessAllowed={}; classificationsWithNoAccess={}", ret, entClsToAuthz);
            }

            LOG.debug("<== SimpleAtlasAuthorizer.isAccessAllowed({}): {}", request, ret);
        }

        return ret;
    }

    @Override
    public void scrubSearchResults(AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> SimpleAtlasAuthorizer.scrubSearchResults({})", request);
        }

        final AtlasSearchResult result = request.getSearchResult();

        if (CollectionUtils.isNotEmpty(result.getEntities())) {
            for (AtlasEntityHeader entity : result.getEntities()) {
                checkAccessAndScrub(entity, request);
            }
        }

        if (CollectionUtils.isNotEmpty(result.getFullTextResult())) {
            for (AtlasFullTextResult fullTextResult : result.getFullTextResult()) {
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== SimpleAtlasAuthorizer.scrubSearchResults({}): {}", request, result);
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

    private Set<String> getRoles(String userName, Set<String> userGroups) {
        Set<String> ret = new HashSet<>();

        if (authzPolicy != null) {
            if (userName != null && authzPolicy.getUserRoles() != null) {
                List<String> userRoles = authzPolicy.getUserRoles().get(userName);

                if (userRoles != null) {
                    ret.addAll(userRoles);
                }
            }

            if (userGroups != null && authzPolicy.getGroupRoles() != null) {
                for (String groupName : userGroups) {
                    List<String> groupRoles = authzPolicy.getGroupRoles().get(groupName);

                    if (groupRoles != null) {
                        ret.addAll(groupRoles);
                    }
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getRoles({}, {}): {}", userName, userGroups, ret);
        }

        return ret;
    }

    private List<AtlasAdminPermission> getAdminPermissionsForRole(String roleName) {
        List<AtlasAdminPermission> ret = null;

        if (authzPolicy != null && roleName != null) {
            AtlasAuthzRole role = authzPolicy.getRoles().get(roleName);

            ret = role != null ? role.getAdminPermissions() : null;
        }

        return ret;
    }

    private List<AtlasTypePermission> getTypePermissionsForRole(String roleName) {
        List<AtlasTypePermission> ret = null;

        if (authzPolicy != null && roleName != null) {
            AtlasAuthzRole role = authzPolicy.getRoles().get(roleName);

            ret = role != null ? role.getTypePermissions() : null;
        }

        return ret;
    }

    private List<AtlasEntityPermission> getEntityPermissionsForRole(String roleName) {
        List<AtlasEntityPermission> ret = null;

        if (authzPolicy != null && roleName != null) {
            AtlasAuthzRole role = authzPolicy.getRoles().get(roleName);

            ret = role != null ? role.getEntityPermissions() : null;
        }

        return ret;
    }


    private List<AtlasRelationshipPermission> getRelationshipPermissionsForRole(String roleName) {
        List<AtlasRelationshipPermission> ret = null;

        if (authzPolicy != null && roleName != null) {
            AtlasAuthzRole role = authzPolicy.getRoles().get(roleName);

            ret = role != null ? role.getRelationshipPermissions() : null;
        }

        return ret;
    }

    private boolean isMatch(String value, List<String> patterns) {
        boolean ret = false;

        if (value == null) {
            ret = true;
        } if (CollectionUtils.isNotEmpty(patterns)) {
            for (String pattern : patterns) {
                if (isMatch(value, pattern)) {
                    ret = true;

                    break;
                }
            }
        }

        if (!ret && LOG.isDebugEnabled()) {
            LOG.debug("<== isMatch({}, {}): {}", value, patterns, ret);
        }

        return ret;
    }

    private boolean isMatchAny(Set<String> values, List<String> patterns) {
        boolean ret = false;

        if (CollectionUtils.isEmpty(values)) {
            ret = true;
        }if (CollectionUtils.isNotEmpty(patterns)) {
            for (String value : values) {
                if (isMatch(value, patterns)) {
                    ret = true;

                    break;
                }
            }
        }

        if (!ret && LOG.isDebugEnabled()) {
            LOG.debug("<== isMatchAny({}, {}): {}", values, patterns, ret);
        }

        return ret;
    }

    private boolean isMatch(String value, String pattern) {
        boolean ret;

        if (value == null) {
            ret = true;
        } else {
            ret = pattern.equals(REGEX_MATCHALL) || StringUtils.equalsIgnoreCase(value, pattern) || value.matches(pattern);
        }

        return ret;
    }

    private void checkAccessAndScrub(AtlasEntityHeader entity, AtlasSearchResultScrubRequest request) throws AtlasAuthorizationException {
        if (entity != null && request != null) {
            final AtlasEntityAccessRequest entityAccessRequest = new AtlasEntityAccessRequest(request.getTypeRegistry(), AtlasPrivilege.ENTITY_READ, entity, request.getUser(), request.getUserGroups());

            entityAccessRequest.setClientIPAddress(request.getClientIPAddress());

            if (!isAccessAllowed(entityAccessRequest)) {
                scrubEntityHeader(entity);
            }
        }
    }

    private boolean isLabelMatch(AtlasEntityAccessRequest request, AtlasEntityPermission permission) {
        return (AtlasPrivilege.ENTITY_ADD_LABEL.equals(request.getAction()) || AtlasPrivilege.ENTITY_REMOVE_LABEL.equals(request.getAction())) ? isMatch(request.getLabel(), permission.getLabels()) : true;
    }

    private boolean isBusinessMetadataMatch(AtlasEntityAccessRequest request, AtlasEntityPermission permission) {
        return AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA.equals(request.getAction()) ? isMatch(request.getBusinessMetadata(), permission.getBusinessMetadata()) : true;
    }

    private boolean isClassificationMatch(AtlasEntityAccessRequest request, AtlasEntityPermission permission) {
        return (CLASSIFICATION_PRIVILEGES.contains(request.getAction()) && request.getClassification() != null) ? isMatch(request.getClassification().getTypeName(), permission.getClassifications()) : true;
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

    // add TYPE_READ privilege, if at least one of the following is granted: TYPE_CREATE, TYPE_UPDATE, TYPE_DELETE
    private void addImpliedTypeReadPrivilege(AtlasSimpleAuthzPolicy policy) {
        if (policy != null && policy.getRoles() != null) {
            for (AtlasAuthzRole role : policy.getRoles().values()) {
                if (role.getTypePermissions() == null) {
                    continue;
                }

                for (AtlasTypePermission permission : role.getTypePermissions()) {
                    List<String> privileges = permission.getPrivileges();

                    if (CollectionUtils.isEmpty(privileges) || privileges.contains(TYPE_READ.name())) {
                        continue;
                    }

                    if (privileges.contains(TYPE_CREATE.name()) || privileges.contains(TYPE_UPDATE.name()) || privileges.contains(TYPE_DELETE.name())) {
                        privileges.add(TYPE_READ.name());
                    }
                }
            }
        }
    }
}


