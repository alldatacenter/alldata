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

package org.apache.ranger.authorization.knox;

import java.io.IOException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.knox.gateway.filter.AbstractGatewayFilter;
import org.apache.knox.gateway.security.GroupPrincipal;
import org.apache.knox.gateway.security.ImpersonatedPrincipal;
import org.apache.knox.gateway.security.PrimaryPrincipal;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.authorization.knox.KnoxRangerPlugin.RequestBuilder;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerPDPKnoxFilter implements Filter {

	private static final Logger LOG = LoggerFactory.getLogger(RangerPDPKnoxFilter.class);

	private static final Logger PERF_KNOXAUTH_REQUEST_LOG = RangerPerfTracer.getPerfLogger("knoxauth.request");

	private static final String KNOX_GATEWAY_JASS_CONFIG_SECTION = "com.sun.security.jgss.initiate";

	private String resourceRole = null;
	private static volatile KnoxRangerPlugin plugin = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		resourceRole = getInitParameter(filterConfig, "resource.role");

		KnoxRangerPlugin me = plugin;

		if(me == null) {
			synchronized (RangerPDPKnoxFilter.class) {
				me = plugin;

				if(me == null) {
					try {
						MiscUtil.setUGIFromJAASConfig(KNOX_GATEWAY_JASS_CONFIG_SECTION);
						LOG.info("LoginUser=" + MiscUtil.getUGILoginUser());
					} catch (Throwable t) {
						LOG.error("Error while setting UGI for Knox Plugin...", t);
					}

					LOG.info("Creating KnoxRangerPlugin");
					plugin = new KnoxRangerPlugin();
					plugin.init();
				}
			}
		}
	}

	private String getInitParameter(FilterConfig filterConfig, String paramName) {
		return filterConfig.getInitParameter(paramName.toLowerCase());
	}

	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		String sourceUrl = (String) request
				.getAttribute(AbstractGatewayFilter.SOURCE_REQUEST_CONTEXT_URL_ATTRIBUTE_NAME);
		String topologyName = getTopologyName(sourceUrl);
		String serviceName = getServiceName();

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_KNOXAUTH_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_KNOXAUTH_REQUEST_LOG, "RangerPDPKnoxFilter.doFilter(url=" + sourceUrl + ", topologyName=" + topologyName + ")");
		}

		Subject subject = Subject.getSubject(AccessController.getContext());

		Set<PrimaryPrincipal> primaryPrincipals = subject.getPrincipals(
				PrimaryPrincipal.class);
		String primaryUser = null;
		if (primaryPrincipals != null && primaryPrincipals.size() > 0) {
			primaryUser = primaryPrincipals.stream().findFirst().get().getName();
		}

		String impersonatedUser = null;
		Set<ImpersonatedPrincipal> impersonations = subject.getPrincipals(
				ImpersonatedPrincipal.class);
		if (impersonations != null && impersonations.size() > 0) {
			impersonatedUser = impersonations.stream().findFirst().get().getName();
		}

		String user = (impersonatedUser != null) ? impersonatedUser
				: primaryUser;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Checking access primaryUser: " + primaryUser + ", impersonatedUser: "
					+ impersonatedUser + ", effectiveUser: " + user);
		}

		Set<GroupPrincipal> groupObjects = subject.getPrincipals(GroupPrincipal.class);
		Set<String> groups = new HashSet<String>();
		for (GroupPrincipal obj : groupObjects) {
			groups.add(obj.getName());
		}

		String clientIp = request.getRemoteAddr();
		List<String> forwardedAddresses = getForwardedAddresses(request);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Checking access primaryUser: " + primaryUser
					+ ", impersonatedUser: " + impersonatedUser
					+ ", effectiveUser: " + user + ", groups: " + groups
					+ ", clientIp: " + clientIp + ", remoteIp: " + clientIp + ", forwardedAddresses: " + forwardedAddresses);
		}

		RangerAccessRequest accessRequest = new RequestBuilder()
			.service(serviceName)
			.topology(topologyName)
			.user(user)
			.groups(groups)
			.clientIp(clientIp)
			.remoteIp(clientIp)
			.forwardedAddresses(forwardedAddresses)
			.build();

		boolean accessAllowed = false;

		if (plugin != null) {
			RangerAccessResult result = plugin.isAccessAllowed(accessRequest);

			accessAllowed = result != null && result.getIsAllowed();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Access allowed: " + accessAllowed);
		}

		RangerPerfTracer.log(perf);

		if (accessAllowed) {
			chain.doFilter(request, response);
		} else {
			sendForbidden((HttpServletResponse) response);
		}
	}

	private List<String> getForwardedAddresses(ServletRequest request) {
		List<String> forwardedAddresses = null;
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
			if(xForwardedFor != null) {
				forwardedAddresses = Arrays.asList(xForwardedFor.split(","));
			}
		}
		return forwardedAddresses;
	}

	private void sendForbidden(HttpServletResponse res) {
		sendErrorCode(res, 403);
	}

	private void sendErrorCode(HttpServletResponse res, int code) {
		try {
			res.sendError(code);
		} catch (IOException e) {
			LOG.error("Error while redirecting:", e);
		}
	}

	private String getTopologyName(String requestUrl) {
		if (requestUrl == null) {
			return null;
		}
		String url = requestUrl.trim();
		String[] tokens = url.split("/");
		if (tokens.length > 2) {
			return tokens[2];
		} else {
			return null;
		}
	}

	private String getServiceName() {
		return resourceRole;
	}
}
