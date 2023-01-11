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
package org.apache.ranger;

import com.sun.jersey.api.client.GenericType;
import org.apache.ranger.authorization.hadoop.config.RangerPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.plugin.model.*;
import org.apache.ranger.admin.client.datatype.RESTResponse;
import org.apache.ranger.plugin.util.GrantRevokeRoleRequest;
import org.apache.ranger.plugin.util.RangerRESTClient;
import org.apache.hadoop.security.SecureClientLogin;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;


public class RangerClient {
    private static final Logger LOG = LoggerFactory.getLogger(RangerClient.class);
    private static final String AUTH_KERBEROS    = "kerberos";

    // QueryParams
    private static final String PARAM_DAYS                          = "days";
    private static final String PARAM_EXEC_USER                     = "execUser";
    private static final String PARAM_POLICY_NAME                   = "policyname";
    private static final String PARAM_SERVICE_NAME                  = "serviceName";
    private static final String PARAM_RELOAD_SERVICE_POLICIES_CACHE = "reloadServicePoliciesCache";

    // URIs
    private static final String URI_BASE                  = "/service/public/v2/api";

    private static final String URI_SERVICEDEF            = URI_BASE + "/servicedef";
    private static final String URI_SERVICEDEF_BY_ID      = URI_SERVICEDEF + "/%d";
    private static final String URI_SERVICEDEF_BY_NAME    = URI_SERVICEDEF + "/name/%s";

    private static final String URI_SERVICE               = URI_BASE + "/service";
    private static final String URI_SERVICE_BY_ID         = URI_SERVICE + "/%d";
    private static final String URI_SERVICE_BY_NAME       = URI_SERVICE + "/name/%s";
    private static final String URI_POLICIES_IN_SERVICE   = URI_SERVICE + "/%s/policy";

    private static final String URI_POLICY                = URI_BASE + "/policy";
    private static final String URI_APPLY_POLICY          = URI_POLICY + "/apply";
    private static final String URI_POLICY_BY_ID          = URI_POLICY + "/%d";
    private static final String URI_POLICY_BY_NAME        = URI_SERVICE + "/%s/policy/%s";

    private static final String URI_ROLE                  = URI_BASE + "/roles";
    private static final String URI_ROLE_NAMES            = URI_ROLE + "/names";
    private static final String URI_ROLE_BY_ID            = URI_ROLE + "/%d";
    private static final String URI_ROLE_BY_NAME          = URI_ROLE + "/name/%s";
    private static final String URI_USER_ROLES            = URI_ROLE + "/user/%s";
    private static final String URI_GRANT_ROLE            = URI_ROLE + "/grant/%s";
    private static final String URI_REVOKE_ROLE           = URI_ROLE + "/revoke/%s";

    private static final String URI_ZONE                  = URI_BASE + "/zones";
    private static final String URI_ZONE_BY_ID            = URI_ZONE + "/%d";
    private static final String URI_ZONE_BY_NAME          = URI_ZONE + "/name/%s";

    private static final String URI_SERVICE_TAGS          = URI_SERVICE + "/%s/tags";
    private static final String URI_PLUGIN_INFO           = URI_BASE + "/plugins/info";
    private static final String URI_POLICY_DELTAS         = URI_BASE + "/server/policydeltas";


    // APIs
    public static final API CREATE_SERVICEDEF         = new API(URI_SERVICEDEF, HttpMethod.POST, Response.Status.OK);
    public static final API UPDATE_SERVICEDEF_BY_ID   = new API(URI_SERVICEDEF_BY_ID, HttpMethod.PUT, Response.Status.OK);
    public static final API UPDATE_SERVICEDEF_BY_NAME = new API(URI_SERVICEDEF_BY_NAME, HttpMethod.PUT, Response.Status.OK);
    public static final API DELETE_SERVICEDEF_BY_ID   = new API(URI_SERVICEDEF_BY_ID, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API DELETE_SERVICEDEF_BY_NAME = new API(URI_SERVICEDEF_BY_NAME, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API GET_SERVICEDEF_BY_ID      = new API(URI_SERVICEDEF_BY_ID, HttpMethod.GET, Response.Status.OK);
    public static final API GET_SERVICEDEF_BY_NAME    = new API(URI_SERVICEDEF_BY_NAME, HttpMethod.GET, Response.Status.OK);
    public static final API FIND_SERVICEDEFS          = new API(URI_SERVICEDEF, HttpMethod.GET, Response.Status.OK);

    public static final API CREATE_SERVICE            = new API(URI_SERVICE, HttpMethod.POST, Response.Status.OK);
    public static final API UPDATE_SERVICE_BY_ID      = new API(URI_SERVICE_BY_ID, HttpMethod.PUT, Response.Status.OK);
    public static final API UPDATE_SERVICE_BY_NAME    = new API(URI_SERVICE_BY_NAME, HttpMethod.PUT, Response.Status.OK);
    public static final API DELETE_SERVICE_BY_ID      = new API(URI_SERVICE_BY_ID, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API DELETE_SERVICE_BY_NAME    = new API(URI_SERVICE_BY_NAME, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API GET_SERVICE_BY_ID         = new API(URI_SERVICE_BY_ID, HttpMethod.GET, Response.Status.OK);
    public static final API GET_SERVICE_BY_NAME       = new API(URI_SERVICE_BY_NAME, HttpMethod.GET, Response.Status.OK);
    public static final API FIND_SERVICES             = new API(URI_SERVICE, HttpMethod.GET, Response.Status.OK);

    public static final API CREATE_POLICY            = new API(URI_POLICY, HttpMethod.POST, Response.Status.OK);
    public static final API UPDATE_POLICY_BY_ID      = new API(URI_POLICY_BY_ID, HttpMethod.PUT, Response.Status.OK);
    public static final API UPDATE_POLICY_BY_NAME    = new API(URI_POLICY_BY_NAME, HttpMethod.PUT, Response.Status.OK);
    public static final API APPLY_POLICY             = new API(URI_APPLY_POLICY, HttpMethod.POST, Response.Status.OK);
    public static final API DELETE_POLICY_BY_ID      = new API(URI_POLICY_BY_ID, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API DELETE_POLICY_BY_NAME    = new API(URI_POLICY, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API GET_POLICY_BY_ID         = new API(URI_POLICY_BY_ID, HttpMethod.GET, Response.Status.OK);
    public static final API GET_POLICY_BY_NAME       = new API(URI_POLICY_BY_NAME, HttpMethod.GET, Response.Status.OK);
    public static final API GET_POLICIES_IN_SERVICE  = new API(URI_POLICIES_IN_SERVICE, HttpMethod.GET, Response.Status.OK);
    public static final API FIND_POLICIES            = new API(URI_POLICY, HttpMethod.GET, Response.Status.OK);

    public static final API CREATE_ZONE         = new API(URI_ZONE, HttpMethod.POST, Response.Status.OK);
    public static final API UPDATE_ZONE_BY_ID   = new API(URI_ZONE_BY_ID, HttpMethod.PUT, Response.Status.OK);
    public static final API UPDATE_ZONE_BY_NAME = new API(URI_ZONE_BY_NAME, HttpMethod.PUT, Response.Status.OK);
    public static final API DELETE_ZONE_BY_ID   = new API(URI_ZONE_BY_ID, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API DELETE_ZONE_BY_NAME = new API(URI_ZONE_BY_NAME, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API GET_ZONE_BY_ID      = new API(URI_ZONE_BY_ID, HttpMethod.GET, Response.Status.OK);
    public static final API GET_ZONE_BY_NAME    = new API(URI_ZONE_BY_NAME, HttpMethod.GET, Response.Status.OK);
    public static final API FIND_ZONES          = new API(URI_ZONE, HttpMethod.GET, Response.Status.OK);

    public static final API CREATE_ROLE         = new API(URI_ROLE, HttpMethod.POST, Response.Status.OK);
    public static final API UPDATE_ROLE_BY_ID   = new API(URI_ROLE_BY_ID, HttpMethod.PUT, Response.Status.OK);
    public static final API DELETE_ROLE_BY_ID   = new API(URI_ROLE_BY_ID, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API DELETE_ROLE_BY_NAME = new API(URI_ROLE_BY_NAME, HttpMethod.DELETE, Response.Status.NO_CONTENT);
    public static final API GET_ROLE_BY_ID      = new API(URI_ROLE_BY_ID, HttpMethod.GET, Response.Status.OK);
    public static final API GET_ROLE_BY_NAME    = new API(URI_ROLE_BY_NAME, HttpMethod.GET, Response.Status.OK);
    public static final API GET_ALL_ROLE_NAMES  = new API(URI_ROLE_NAMES, HttpMethod.GET, Response.Status.OK);
    public static final API GET_USER_ROLES      = new API(URI_USER_ROLES, HttpMethod.GET, Response.Status.OK);
    public static final API GRANT_ROLE          = new API(URI_GRANT_ROLE, HttpMethod.PUT, Response.Status.OK);
    public static final API REVOKE_ROLE         = new API(URI_REVOKE_ROLE, HttpMethod.PUT, Response.Status.OK);
    public static final API FIND_ROLES          = new API(URI_ROLE, HttpMethod.GET, Response.Status.OK);

    public static final API IMPORT_SERVICE_TAGS  = new API(URI_SERVICE_TAGS, HttpMethod.PUT, Response.Status.NO_CONTENT);
    public static final API GET_SERVICE_TAGS     = new API(URI_SERVICE_TAGS, HttpMethod.GET, Response.Status.OK);
    public static final API GET_PLUGIN_INFO      = new API(URI_PLUGIN_INFO, HttpMethod.GET, Response.Status.OK);
    public static final API DELETE_POLICY_DELTAS = new API(URI_POLICY_DELTAS, HttpMethod.DELETE, Response.Status.NO_CONTENT);


    private final RangerRESTClient restClient;
    private boolean isSecureMode = false;
    private Subject sub = null;

    private void authInit(String authType, String username, String password) {
        if (AUTH_KERBEROS.equalsIgnoreCase(authType)) {
            if (SecureClientLogin.isKerberosCredentialExists(username, password)) {
                isSecureMode = true;
                try {
                    sub = SecureClientLogin.loginUserFromKeytab(username, password);
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }
            } else LOG.error("Authentication credentials missing/invalid");
        } else {
            restClient.setBasicAuthInfo(username, password);
        }
    }

    public RangerClient(String hostName, String authType, String username, String password, String configFile) {
        restClient = new RangerRESTClient(hostName, configFile, new Configuration());
        authInit(authType, username, password);
    }

    public RangerClient(String hostname, String authType, String username, String password, String appId, String serviceType){
        this(hostname, authType, username, password,
                new RangerPluginConfig(serviceType, null,appId,null,null,null)
                        .get("ranger.plugin." + serviceType + ".policy.rest.ssl.config.file"));
    }


    public RangerClient(RangerRESTClient restClient) {
        this.restClient = restClient;
    }

    /*
     * ServiceDef APIs
     */
    public RangerServiceDef createServiceDef(RangerServiceDef serviceDef) throws RangerServiceException {
        return callAPI(CREATE_SERVICEDEF, null, serviceDef, RangerServiceDef.class);
    }

    public RangerServiceDef updateServiceDef(long serviceDefId, RangerServiceDef serviceDef) throws RangerServiceException {
        return callAPI(UPDATE_SERVICEDEF_BY_ID.applyUrlFormat(serviceDefId), null, serviceDef, RangerServiceDef.class);
    }

    public RangerServiceDef updateServiceDef(String serviceDefName, RangerServiceDef serviceDef) throws RangerServiceException {
        return callAPI(UPDATE_SERVICEDEF_BY_NAME.applyUrlFormat(serviceDefName), null, serviceDef, RangerServiceDef.class);
    }

    public void deleteServiceDef(long serviceDefId) throws RangerServiceException {
        callAPI(DELETE_SERVICEDEF_BY_ID.applyUrlFormat(serviceDefId), null);
    }

    public void deleteServiceDef(String serviceDefName) throws RangerServiceException {
        callAPI(DELETE_SERVICEDEF_BY_NAME.applyUrlFormat(serviceDefName), null);
    }

    public RangerServiceDef getServiceDef(long serviceDefId) throws RangerServiceException {
        return callAPI(GET_SERVICEDEF_BY_ID.applyUrlFormat(serviceDefId), null, null, RangerServiceDef.class);
    }

    public RangerServiceDef getServiceDef(String serviceDefName) throws RangerServiceException {
        return callAPI(GET_SERVICEDEF_BY_NAME.applyUrlFormat(serviceDefName), null, null, RangerServiceDef.class);
    }

    public List<RangerServiceDef> findServiceDefs(Map<String, String> filter) throws RangerServiceException {
        return callAPI(FIND_SERVICEDEFS, filter, null, new GenericType<List<RangerServiceDef>>(){});
    }


    /*
     * Service APIs
     */
    public RangerService createService(RangerService service) throws RangerServiceException {
        return callAPI(CREATE_SERVICE, null, service, RangerService.class);
    }

    public RangerService updateService(long serviceId, RangerService service) throws RangerServiceException {
        return callAPI(UPDATE_SERVICE_BY_ID.applyUrlFormat(serviceId), null, service, RangerService.class);
    }

    public RangerService updateService(String serviceName, RangerService service) throws RangerServiceException {
        return callAPI(UPDATE_SERVICE_BY_NAME.applyUrlFormat(serviceName), null, service, RangerService.class);
    }

    public void deleteService(long serviceId) throws RangerServiceException {
        callAPI(DELETE_SERVICE_BY_ID.applyUrlFormat(serviceId), null);
    }

    public void deleteService(String serviceName) throws RangerServiceException {
        callAPI(DELETE_SERVICE_BY_NAME.applyUrlFormat(serviceName), null);
    }

    public RangerService getService(long serviceId) throws RangerServiceException {
        return callAPI(GET_SERVICE_BY_ID.applyUrlFormat(serviceId), null, null, RangerService.class);
    }

    public RangerService getService(String serviceName) throws RangerServiceException {
        return callAPI(GET_SERVICE_BY_NAME.applyUrlFormat(serviceName), null, null, RangerService.class);
    }

    public List<RangerService> findServices(Map<String, String> filter) throws RangerServiceException {
        return callAPI(FIND_SERVICES, filter, null, new GenericType<List<RangerService>>(){});
    }


    /*
     * Policy APIs
     */
    public RangerPolicy createPolicy(RangerPolicy policy) throws RangerServiceException {
        return callAPI(CREATE_POLICY, null, policy, RangerPolicy.class);
    }

    public RangerPolicy updatePolicy(long policyId, RangerPolicy policy) throws RangerServiceException {
        return callAPI(UPDATE_POLICY_BY_ID.applyUrlFormat(policyId), null, policy, RangerPolicy.class);
    }

    public RangerPolicy updatePolicy(String serviceName, String policyName, RangerPolicy policy) throws RangerServiceException {
        return callAPI(UPDATE_POLICY_BY_NAME.applyUrlFormat(serviceName, policyName), null, policy, RangerPolicy.class);
    }

    public RangerPolicy applyPolicy(RangerPolicy policy) throws RangerServiceException {
        return callAPI(APPLY_POLICY, null, policy, RangerPolicy.class);
    }

    public void deletePolicy(long policyId) throws RangerServiceException {
        callAPI(DELETE_POLICY_BY_ID.applyUrlFormat(policyId), null);
    }

    public void deletePolicy(String serviceName, String policyName) throws RangerServiceException {
        Map<String,String> queryParams = new HashMap<>();

        queryParams.put(PARAM_POLICY_NAME, policyName);
        queryParams.put("servicename", serviceName);

        callAPI(DELETE_POLICY_BY_NAME, queryParams);
    }

    public RangerPolicy getPolicy(long policyId) throws RangerServiceException {
        return callAPI(GET_POLICY_BY_ID.applyUrlFormat(policyId), null, null, RangerPolicy.class);
    }

    public RangerPolicy getPolicy(String serviceName, String policyName) throws RangerServiceException {
        return callAPI(GET_POLICY_BY_NAME.applyUrlFormat(serviceName, policyName), null, null, RangerPolicy.class);
    }

    public List<RangerPolicy> getPoliciesInService(String serviceName) throws RangerServiceException {
        return callAPI(GET_POLICIES_IN_SERVICE.applyUrlFormat(serviceName), null, null, new GenericType<List<RangerPolicy>>(){});
    }

    public List<RangerPolicy> findPolicies(Map<String, String> filter) throws RangerServiceException {
        return callAPI(FIND_POLICIES, filter, null, new GenericType<List<RangerPolicy>>(){});
    }


    /*
     * SecurityZone APIs
     */
    public RangerSecurityZone createSecurityZone(RangerSecurityZone securityZone) throws RangerServiceException {
        return callAPI(CREATE_ZONE, null, securityZone, RangerSecurityZone.class);
    }

    public RangerSecurityZone updateSecurityZone(long zoneId, RangerSecurityZone securityZone) throws RangerServiceException {
        return callAPI(UPDATE_ZONE_BY_ID.applyUrlFormat(zoneId), null, securityZone, RangerSecurityZone.class);
    }

    public RangerSecurityZone updateSecurityZone(String zoneName, RangerSecurityZone securityZone) throws RangerServiceException {
        return callAPI(UPDATE_ZONE_BY_NAME.applyUrlFormat(zoneName), null, securityZone, RangerSecurityZone.class);
    }

    public void deleteSecurityZone(long zoneId) throws RangerServiceException {
        callAPI(DELETE_ZONE_BY_ID.applyUrlFormat(zoneId), null);
    }

    public void deleteSecurityZone(String zoneName) throws RangerServiceException {
        callAPI(DELETE_ZONE_BY_NAME.applyUrlFormat(zoneName), null);
    }

    public RangerSecurityZone getSecurityZone(long zoneId) throws RangerServiceException {
        return callAPI(GET_ZONE_BY_ID.applyUrlFormat(zoneId), null, null, RangerSecurityZone.class);
    }

    public RangerSecurityZone getSecurityZone(String zoneName) throws RangerServiceException {
        return callAPI(GET_ZONE_BY_NAME.applyUrlFormat(zoneName), null, null, RangerSecurityZone.class);
    }

    public List<RangerSecurityZone> findSecurityZones(Map<String, String> filter) throws RangerServiceException {
        return callAPI(FIND_ZONES, filter, null, new GenericType<List<RangerSecurityZone>>(){});
    }

    /*
     * Role APIs
     */
    public RangerRole createRole(String serviceName, RangerRole role) throws RangerServiceException {
        return callAPI(CREATE_ROLE, Collections.singletonMap(PARAM_SERVICE_NAME, serviceName), role, RangerRole.class);
    }

    public RangerRole updateRole(long roleId, RangerRole role) throws RangerServiceException {
        return callAPI(UPDATE_ROLE_BY_ID.applyUrlFormat(roleId), null, role, RangerRole.class);
    }

    public void deleteRole(long roleId) throws RangerServiceException {
        callAPI(DELETE_ROLE_BY_ID.applyUrlFormat(roleId), null);
    }

    public void deleteRole(String roleName, String execUser, String serviceName) throws RangerServiceException {
        Map<String,String> queryParams = new HashMap<>();

        queryParams.put(PARAM_EXEC_USER, execUser);
        queryParams.put(PARAM_SERVICE_NAME, serviceName);

        callAPI(DELETE_ROLE_BY_NAME.applyUrlFormat(roleName), queryParams);
    }

    public RangerRole getRole(long roleId) throws RangerServiceException {
        return callAPI(GET_ROLE_BY_ID.applyUrlFormat(roleId), null, null, RangerRole.class);
    }

    public RangerRole getRole(String roleName, String execUser, String serviceName) throws RangerServiceException {
        Map<String,String> queryParams = new HashMap<>();

        queryParams.put(PARAM_EXEC_USER, execUser);
        queryParams.put(PARAM_SERVICE_NAME, serviceName);

        return callAPI(GET_ROLE_BY_NAME.applyUrlFormat(roleName), queryParams, null, RangerRole.class);
    }

    public List<String> getAllRoleNames(String execUser, String serviceName) throws RangerServiceException {
        Map<String,String> queryParams = new HashMap<>();

        queryParams.put(PARAM_EXEC_USER, execUser);
        queryParams.put(PARAM_SERVICE_NAME, serviceName);

        return callAPI(GET_ALL_ROLE_NAMES.applyUrlFormat(serviceName), queryParams, null, new GenericType<List<String>>(){});
    }

    public List<String> getUserRoles(String user) throws RangerServiceException {
        return callAPI(GET_USER_ROLES.applyUrlFormat(user), null, null, new GenericType<List<String>>(){});
    }

    public List<RangerRole> findRoles(Map<String, String> filter) throws RangerServiceException {
        return callAPI(FIND_ROLES, filter, null, new GenericType<List<RangerRole>>(){});
    }

    public RESTResponse grantRole(String serviceName, GrantRevokeRoleRequest request) throws RangerServiceException {
        return callAPI(GRANT_ROLE.applyUrlFormat(serviceName), null, request, RESTResponse.class);
    }

    public RESTResponse revokeRole(String serviceName, GrantRevokeRoleRequest request) throws RangerServiceException {
        return callAPI(REVOKE_ROLE.applyUrlFormat(serviceName), null, request, RESTResponse.class);
    }


    /*
     * Admin APIs
     */
    public void importServiceTags(String serviceName, RangerServiceTags svcTags) throws RangerServiceException {
        callAPI(IMPORT_SERVICE_TAGS.applyUrlFormat(serviceName), null, svcTags, (GenericType<Void>) null);
    }

    public RangerServiceTags getServiceTags(String serviceName) throws RangerServiceException {
        return callAPI(GET_SERVICE_TAGS.applyUrlFormat(serviceName), null, null, RangerServiceTags.class);
    }

    public List<RangerPluginInfo> getPluginsInfo() throws RangerServiceException {
        return callAPI(GET_PLUGIN_INFO, null, null, new GenericType<List<RangerPluginInfo>>(){});
    }

    public void deletePolicyDeltas(int days, boolean reloadServicePoliciesCache) throws RangerServiceException {
        Map<String,String> queryParams = new HashMap<>();

        queryParams.put(PARAM_DAYS, String.valueOf(days));
        queryParams.put(PARAM_RELOAD_SERVICE_POLICIES_CACHE, String.valueOf(reloadServicePoliciesCache));

        callAPI(DELETE_POLICY_DELTAS, queryParams);
    }

    private ClientResponse invokeREST(API api, Map<String, String> params, Object request) throws RangerServiceException {
        final ClientResponse clientResponse;
        try {
            switch (api.getMethod()) {
                case HttpMethod.POST:
                    clientResponse = restClient.post(api.getPath(), params, request);
                    break;

                case HttpMethod.PUT:
                    clientResponse = restClient.put(api.getPath(), params, request);
                    break;

                case HttpMethod.GET:
                    clientResponse = restClient.get(api.getPath(), params);
                    break;

                case HttpMethod.DELETE:
                    clientResponse = restClient.delete(api.getPath(), params);
                    break;

                default:
                    LOG.error(api.getMethod() + ": unsupported HTTP method");

                    clientResponse = null;
            }
        } catch (Exception excp) {
            throw new RangerServiceException(excp);
        }
        return clientResponse;
    }

    private ClientResponse responseHandler(API api, Map<String, String> params, Object request) throws RangerServiceException {
        final ClientResponse clientResponse;

        if (LOG.isDebugEnabled()){
            LOG.debug("Call         : {} {}", api.getMethod(), api.getNormalizedPath());
            LOG.debug("Content-type : {} ", api.getConsumes());
            LOG.debug("Accept       : {} ", api.getProduces());
            if (request != null) {
                LOG.debug("Request      : {}", request);
            }
        }

        if (isSecureMode) {
            clientResponse = Subject.doAs(sub, (PrivilegedAction<ClientResponse>) () -> {
                try {
                    return invokeREST(api,params,request);
                } catch (RangerServiceException e) {
                    LOG.error(e.getMessage());
                }
                return null;
            });
        } else {
            clientResponse = invokeREST(api,params,request);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("method={}, path={}, contentType={}, accept={}, httpStatus={}", api.getMethod(), api.getNormalizedPath(), api.getConsumes(), api.getProduces(), (clientResponse != null ? clientResponse.getStatus() : "null"));
        }

        if (clientResponse == null) {
            throw new RangerServiceException(api, null);
        } else if (clientResponse.getStatus() == api.getExpectedStatus().getStatusCode()) {
            return clientResponse;
        } else if (clientResponse.getStatus() == ClientResponse.Status.SERVICE_UNAVAILABLE.getStatusCode()) {
            LOG.error("Ranger Admin unavailable. HTTP Status: {}", clientResponse.getStatus());
        } else {
            throw new RangerServiceException(api, clientResponse);
        }
        return clientResponse;
    }

    private void callAPI(API api, Map<String, String> params) throws RangerServiceException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> callAPI({},{})",api, params);
        }

        responseHandler(api, params, null);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== callAPI({},{})", api, params);
        }
    }

    private <T> T callAPI(API api, Map<String, String> params, Object request, GenericType<T> responseType) throws RangerServiceException {
        T ret = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> callAPI({},{},{})",api, params, request);
            LOG.debug("------------------------------------------------------");
        }
        final ClientResponse clientResponse = responseHandler(api, params, request);
        if (responseType != null) {
            ret = clientResponse.getEntity(responseType);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Response: {}", restClient.toJson(ret));
                LOG.debug("------------------------------------------------------");
                LOG.debug("<== callAPI({},{},{},{}), result = {}", api, params, request, responseType, ret);
            }
        }
        return ret;
    }

    private <T> T callAPI(API api, Map<String, String> params, Object request, Class<T> responseType) throws RangerServiceException {
        T ret = null;
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> callAPI({},{},{})",api, params, request);
            LOG.debug("------------------------------------------------------");
        }
        final ClientResponse clientResponse = responseHandler(api, params, request);
        if (responseType != null) {
            ret = clientResponse.getEntity(responseType);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Response: {}", restClient.toJson(ret));
                LOG.debug("------------------------------------------------------");
                LOG.debug("<== callAPI({},{},{},{}), result = {}", api, params, request, responseType, ret);
            }
        }
        return ret;
    }

    public static class API {
        private static final Logger LOG = LoggerFactory.getLogger(API.class);

        private final String          path;
        private final String          method;
        private final Response.Status expectedStatus;
        private final String          consumes;
        private final String          produces;


        public API(String path, String method, Response.Status expectedStatus) {
            this(path, method, expectedStatus, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
        }

        public API(String path, String method, Response.Status expectedStatus, String consumes, String produces) {
            this.path           = path;
            this.method         = method;
            this.expectedStatus = expectedStatus;
            this.consumes       = consumes;
            this.produces       = produces;
        }

        public String getPath() {
            return path;
        }

        public String getMethod() {
            return method;
        }

        public Response.Status getExpectedStatus() {
            return expectedStatus;
        }

        public String getConsumes() {
            return consumes;
        }

        public String getProduces() {
            return produces;
        }

        public String getNormalizedPath() {
            // This method used to return Paths.get(path).normalize().toString(), but
            // the use of Paths.get(path) on Windows produces a path with Windows
            // path separators (i.e. back-slashes) which is not valid for a URI
            // and will result in an HTTP 404 status code.
            String ret = null;

            try {
                URI uri = new URI(path);

                URI normalizedUri = uri.normalize();

                ret = normalizedUri.toString();
            } catch (Exception e) {
                LOG.error("getNormalizedPath() caught exception for path={}", path, e);

                ret = null;
            }

            return ret;
        }

        public API applyUrlFormat(Object... params) throws RangerServiceException {
            try{
                return new API(String.format(path, params), method, expectedStatus, consumes, produces);
            } catch(IllegalFormatException e) {
                LOG.error("Arguments not formatted properly");

                throw new RangerServiceException(e);
            }
        }
    }
}