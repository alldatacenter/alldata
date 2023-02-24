/**
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

package org.apache.atlas.authorize;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.utils.AtlasPerfMetrics.MetricRecorder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Arrays;

public class AtlasAuthorizationUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasAuthorizationUtils.class);

    public static void verifyAccess(AtlasAdminAccessRequest request, Object... errorMsgParams) throws AtlasBaseException {
        if (! isAccessAllowed(request)) {
            String message = (errorMsgParams != null && errorMsgParams.length > 0) ? StringUtils.join(errorMsgParams) : "";

            throw new AtlasBaseException(AtlasErrorCode.UNAUTHORIZED_ACCESS, request.getUser(), message);
        }
    }

    public static void verifyAccess(AtlasTypeAccessRequest request, Object... errorMsgParams) throws AtlasBaseException {
        if (! isAccessAllowed(request)) {
            String message = (errorMsgParams != null && errorMsgParams.length > 0) ? StringUtils.join(errorMsgParams) : "";

            throw new AtlasBaseException(AtlasErrorCode.UNAUTHORIZED_ACCESS, request.getUser(), message);
        }
    }

    public static void verifyAccess(AtlasEntityAccessRequest request, Object... errorMsgParams) throws AtlasBaseException {
        if (! isAccessAllowed(request)) {
            String message = (errorMsgParams != null && errorMsgParams.length > 0) ? StringUtils.join(errorMsgParams) : "";

            throw new AtlasBaseException(AtlasErrorCode.UNAUTHORIZED_ACCESS, request.getUser(), message);
        }
    }

    public static void verifyAccess(AtlasRelationshipAccessRequest request, Object... errorMsgParams) throws AtlasBaseException {
        if (!isAccessAllowed(request)) {
            String message = (errorMsgParams != null && errorMsgParams.length > 0) ? StringUtils.join(errorMsgParams) : "";
            throw new AtlasBaseException(AtlasErrorCode.UNAUTHORIZED_ACCESS, request.getUser(), message);
        }
    }

    public static void scrubSearchResults(AtlasSearchResultScrubRequest request) throws AtlasBaseException {
        String userName = getCurrentUserName();

        if (StringUtils.isNotEmpty(userName)) {
            try {
                AtlasAuthorizer authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();

                request.setUser(userName, getCurrentUserGroups());
                request.setClientIPAddress(RequestContext.get().getClientIPAddress());
                request.setForwardedAddresses(RequestContext.get().getForwardedAddresses());
                request.setRemoteIPAddress(RequestContext.get().getClientIPAddress());

                authorizer.scrubSearchResults(request);
            } catch (AtlasAuthorizationException e) {
                LOG.error("Unable to obtain AtlasAuthorizer", e);
            }
        }
    }

    public static boolean isAccessAllowed(AtlasAdminAccessRequest request) {
        MetricRecorder metric = RequestContext.get().startMetricRecord("isAccessAllowed");

        boolean ret      = false;
        String  userName = getCurrentUserName();

        if (StringUtils.isNotEmpty(userName)) {
            try {
                AtlasAuthorizer authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();

                request.setUser(userName, getCurrentUserGroups());
                request.setClientIPAddress(RequestContext.get().getClientIPAddress());
                request.setForwardedAddresses(RequestContext.get().getForwardedAddresses());
                request.setRemoteIPAddress(RequestContext.get().getClientIPAddress());
                ret = authorizer.isAccessAllowed(request);
            } catch (AtlasAuthorizationException e) {
                LOG.error("Unable to obtain AtlasAuthorizer", e);
            }
        } else {
            ret = true;
        }

        RequestContext.get().endMetricRecord(metric);

        return ret;
    }

    public static boolean isAccessAllowed(AtlasEntityAccessRequest request) {
        MetricRecorder metric = RequestContext.get().startMetricRecord("isAccessAllowed");

        boolean ret      = false;
        String  userName = getCurrentUserName();

        if (StringUtils.isNotEmpty(userName) && !RequestContext.get().isImportInProgress()) {
            try {
                AtlasAuthorizer authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();

                request.setUser(getCurrentUserName(), getCurrentUserGroups());
                request.setClientIPAddress(RequestContext.get().getClientIPAddress());
                request.setForwardedAddresses(RequestContext.get().getForwardedAddresses());
                request.setRemoteIPAddress(RequestContext.get().getClientIPAddress());
                ret = authorizer.isAccessAllowed(request);
            } catch (AtlasAuthorizationException e) {
                LOG.error("Unable to obtain AtlasAuthorizer", e);
            }
        } else {
            ret = true;
        }

        RequestContext.get().endMetricRecord(metric);

        return ret;
    }

    public static boolean isAccessAllowed(AtlasTypeAccessRequest request) {
        MetricRecorder metric = RequestContext.get().startMetricRecord("isAccessAllowed");

        boolean ret      = false;
        String  userName = getCurrentUserName();

        if (StringUtils.isNotEmpty(userName) && !RequestContext.get().isImportInProgress()) {
            try {
                AtlasAuthorizer authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();

                request.setUser(getCurrentUserName(), getCurrentUserGroups());
                request.setClientIPAddress(RequestContext.get().getClientIPAddress());
                request.setForwardedAddresses(RequestContext.get().getForwardedAddresses());
                request.setRemoteIPAddress(RequestContext.get().getClientIPAddress());
                ret = authorizer.isAccessAllowed(request);
            } catch (AtlasAuthorizationException e) {
                LOG.error("Unable to obtain AtlasAuthorizer", e);
            }
        } else {
            ret = true;
        }

        RequestContext.get().endMetricRecord(metric);

        return ret;
    }

    public static boolean isAccessAllowed(AtlasRelationshipAccessRequest request) {
        MetricRecorder metric = RequestContext.get().startMetricRecord("isAccessAllowed");

        boolean ret      = false;
        String  userName = getCurrentUserName();

        if (StringUtils.isNotEmpty(userName) && !RequestContext.get().isImportInProgress()) {
            try {
                AtlasAuthorizer authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();

                request.setUser(getCurrentUserName(), getCurrentUserGroups());
                request.setClientIPAddress(RequestContext.get().getClientIPAddress());
                request.setForwardedAddresses(RequestContext.get().getForwardedAddresses());
                request.setRemoteIPAddress(RequestContext.get().getClientIPAddress());
                ret = authorizer.isAccessAllowed(request);
            } catch (AtlasAuthorizationException e) {
                LOG.error("Unable to obtain AtlasAuthorizer", e);
            }
        } else {
            ret = true;
        }

        RequestContext.get().endMetricRecord(metric);

        return ret;
    }

    public static void filterTypesDef(AtlasTypesDefFilterRequest request) {
        MetricRecorder metric  = RequestContext.get().startMetricRecord("filterTypesDef");
        String        userName = getCurrentUserName();

        if (StringUtils.isNotEmpty(userName) && !RequestContext.get().isImportInProgress()) {
            try {
                AtlasAuthorizer authorizer = AtlasAuthorizerFactory.getAtlasAuthorizer();

                request.setUser(getCurrentUserName(), getCurrentUserGroups());
                request.setClientIPAddress(RequestContext.get().getClientIPAddress());
                request.setForwardedAddresses(RequestContext.get().getForwardedAddresses());
                request.setRemoteIPAddress(RequestContext.get().getClientIPAddress());

                authorizer.filterTypesDef(request);
            } catch (AtlasAuthorizationException e) {
                LOG.error("Unable to obtain AtlasAuthorizer", e);
            }
        }

        RequestContext.get().endMetricRecord(metric);
    }

    public static List<String> getForwardedAddressesFromRequest(HttpServletRequest httpServletRequest){
        String ipAddress = httpServletRequest.getHeader("X-FORWARDED-FOR");
        String[] forwardedAddresses = null ;

        if(!StringUtils.isEmpty(ipAddress)){
            forwardedAddresses = ipAddress.split(",");
        }
        return forwardedAddresses != null ? Arrays.asList(forwardedAddresses) : null;
    }

    public static String getRequestIpAddress(HttpServletRequest httpServletRequest) {
        String ret = "";

        try {
            InetAddress inetAddr = InetAddress.getByName(httpServletRequest.getRemoteAddr());

            ret = inetAddr.getHostAddress();
        } catch (UnknownHostException ex) {
            LOG.error("Failed to retrieve client IP address", ex);
        }

        return ret;
    }



    public static String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null ? auth.getName() : "";
    }

    public static Set<String> getCurrentUserGroups() {
        Set<String> ret = new HashSet<>();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            for (GrantedAuthority c : auth.getAuthorities()) {
                ret.add(c.getAuthority());
            }
        }

        return ret;
    }
}
