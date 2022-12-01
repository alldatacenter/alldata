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

package org.apache.atlas.web.filters;

import org.apache.atlas.web.service.ActiveInstanceState;
import org.apache.atlas.web.service.ServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * A servlet {@link Filter} that redirects web requests from a passive Atlas server instance to an active one.
 *
 * All requests to an active instance pass through. Requests received by a passive instance are redirected
 * by identifying the currently active server. Requests to servers which are in transition are returned with
 * an error SERVICE_UNAVAILABLE. Identification of this state is carried out using
 * {@link ServiceState} and {@link ActiveInstanceState}.
 */
@Component
public class ActiveServerFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ActiveServerFilter.class);
    private static final String MIGRATION_STATUS_STATIC_PAGE = "migration-status.html";

    private final ActiveInstanceState activeInstanceState;
    private ServiceState serviceState;

    @Inject
    public ActiveServerFilter(ActiveInstanceState activeInstanceState, ServiceState serviceState) {
        this.activeInstanceState = activeInstanceState;
        this.serviceState = serviceState;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("ActiveServerFilter initialized");
    }

    /**
     * Determines if this Atlas server instance is passive and redirects to active if so.
     *
     * @param servletRequest Request object from which the URL and other parameters are determined.
     * @param servletResponse Response object to handle the redirect.
     * @param filterChain Chain to pass through requests if the instance is Active.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {
        if (isFilteredURI(servletRequest)) {
            LOG.debug("Is a filtered URI: {}. Passing request downstream.",
                    ((HttpServletRequest)servletRequest).getRequestURI());
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (isInstanceActive()) {
            LOG.debug("Active. Passing request downstream");
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (serviceState.isInstanceInTransition()) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            LOG.error("Instance in transition. Service may not be ready to return a result");
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else if (serviceState.isInstanceInMigration()) {
            if (isRootURI(servletRequest)) {
                handleMigrationRedirect(servletRequest, servletResponse);
            }
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            LOG.error("Instance in migration. Service may not be ready to return a result");
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            String activeServerAddress = activeInstanceState.getActiveServerAddress();
            if (activeServerAddress == null) {
                LOG.error("Could not retrieve active server address as it is null. Cannot redirect request {}",
                        ((HttpServletRequest)servletRequest).getRequestURI());
                httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } else {
                handleRedirect((HttpServletRequest) servletRequest, httpServletResponse, activeServerAddress);
            }
        }
    }

    final String adminUriNotFiltered[] = { "/admin/export", "/admin/import", "/admin/importfile", "/admin/audits",
            "/admin/purge", "/admin/expimp/audit", "/admin/metrics", "/admin/server", "/admin/audit/", "admin/tasks"};
    private boolean isFilteredURI(ServletRequest servletRequest) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();

        if(requestURI.contains("/admin/")) {
            for (String s : adminUriNotFiltered) {
                if (requestURI.contains(s)) {
                    LOG.error("URL not supported in HA mode: {}", requestURI);
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isRootURI(ServletRequest servletRequest) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        return requestURI.equals("/");
    }

    boolean isInstanceActive() {
        return serviceState.getState() == ServiceState.ServiceStateValue.ACTIVE;
    }

    private void handleMigrationRedirect(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException {

        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        String redirectLocation = httpServletRequest.getRequestURL() + MIGRATION_STATUS_STATIC_PAGE;
        if (isUnsafeHttpMethod(httpServletRequest)) {
            httpServletResponse.setHeader(HttpHeaders.LOCATION, redirectLocation);
            httpServletResponse.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        } else {
            httpServletResponse.sendRedirect(redirectLocation);
        }
    }

    private void handleRedirect(HttpServletRequest servletRequest, HttpServletResponse httpServletResponse,
                                String activeServerAddress) throws IOException {
        String requestURI = servletRequest.getRequestURI();
        String queryString = servletRequest.getQueryString();

        if (queryString != null && (!queryString.isEmpty())) {
            queryString = UriUtils.encodeQuery(queryString, "UTF-8");
        }

        if ((queryString != null) && (!queryString.isEmpty())) {
            requestURI += "?" + queryString;
        }

        if (requestURI == null) {
            requestURI = "/";
        }
        String redirectLocation = activeServerAddress + requestURI;
        LOG.info("Not active. Redirecting to {}", redirectLocation);
        // A POST/PUT/DELETE require special handling by sending HTTP 307 instead of the regular 301/302.
        // Reference: http://stackoverflow.com/questions/2068418/whats-the-difference-between-a-302-and-a-307-redirect
        if (isUnsafeHttpMethod(servletRequest)) {
            httpServletResponse.setHeader(HttpHeaders.LOCATION, redirectLocation);
            httpServletResponse.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        } else {
            httpServletResponse.sendRedirect(redirectLocation);
        }
    }

    private boolean isUnsafeHttpMethod(HttpServletRequest httpServletRequest) {
        String method = httpServletRequest.getMethod();
        return (method.equals(HttpMethod.POST)) ||
                (method.equals(HttpMethod.PUT)) ||
                (method.equals(HttpMethod.DELETE));
    }

    @Override
    public void destroy() {

    }
}
