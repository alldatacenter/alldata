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

package org.apache.ranger.authorization.solr.authorizer;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.plugin.contextenricher.RangerContextEnricher;
import org.apache.ranger.plugin.contextenricher.RangerUserStoreEnricher;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerAuthContext;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.services.solr.RangerSolrConstants;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.security.AuthorizationContext.RequestType;
import org.apache.solr.security.AuthorizationPlugin;
import org.apache.solr.security.AuthorizationResponse;
import org.apache.solr.security.AuthorizationContext;
import org.apache.solr.security.AuthorizationContext.CollectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import org.apache.solr.security.PermissionNameProvider;

public class RangerSolrAuthorizer extends SearchComponent implements AuthorizationPlugin {
	private static final Logger logger = LoggerFactory
			.getLogger(RangerSolrAuthorizer.class);

	private List<FieldToAttributeMapping> fieldAttributeMappings = new LinkedList<>();

	private String andQParserName;

	private static volatile RangerBasePlugin solrPlugin = null;

	boolean useProxyIP = false;
	String proxyIPHeader = "HTTP_X_FORWARDED_FOR";
	String solrAppName = "Client";

	private String authField;
	private String allRolesToken;
	private boolean enabled;
	private RangerSolrConstants.MatchType matchMode;
	private String tokenCountField;
	private boolean allowMissingValue;
	private String qParserName;
	private boolean attrsEnabled;

	public RangerSolrAuthorizer() {
		logger.info("RangerSolrAuthorizer()");
	}

	@Override
	public void init(NamedList args) {
		SolrParams params = args.toSolrParams();
		this.authField = params.get(RangerSolrConstants.AUTH_FIELD_PROP, RangerSolrConstants.DEFAULT_AUTH_FIELD);
		this.allRolesToken = params.get(RangerSolrConstants.ALL_ROLES_TOKEN_PROP, "");
		this.enabled = params.getBool(RangerSolrConstants.ENABLED_PROP, false);
		this.matchMode = RangerSolrConstants.MatchType.valueOf(params.get(RangerSolrConstants.MODE_PROP, RangerSolrConstants.DEFAULT_MODE_PROP).toUpperCase());

		if (this.matchMode == RangerSolrConstants.MatchType.CONJUNCTIVE) {
			this.qParserName = params.get(RangerSolrConstants.QPARSER_PROP, "subset").trim();
			this.allowMissingValue = params.getBool(RangerSolrConstants.ALLOW_MISSING_VAL_PROP, false);
			this.tokenCountField = params.get(RangerSolrConstants.TOKEN_COUNT_PROP, RangerSolrConstants.DEFAULT_TOKEN_COUNT_FIELD_PROP);
		}

		this.attrsEnabled = params.getBool(RangerSolrConstants.ATTRS_ENABLED_PROP, false);

		logger.info("RangerSolrAuthorizer.init(): authField={" + authField + "}, allRolesToken={" + allRolesToken +
				"}, enabled={" + enabled + "}, matchType={" + matchMode + "}, qParserName={" + qParserName +
				"}, allowMissingValue={" + allowMissingValue + "}, tokenCountField={" + tokenCountField + "}, attrsEnabled={" + attrsEnabled + "}");

		if (attrsEnabled) {

			if (params.get(RangerSolrConstants.FIELD_ATTR_MAPPINGS) != null) {
				logger.info("Solr params = " + params.get(RangerSolrConstants.FIELD_ATTR_MAPPINGS));

				NamedList mappings = checkAndGet(args, RangerSolrConstants.FIELD_ATTR_MAPPINGS);

				Iterator<Map.Entry<String, NamedList>> iter = mappings.iterator();
				while (iter.hasNext()) {
					Map.Entry<String, NamedList> entry = iter.next();
					String solrFieldName = entry.getKey();
					String attributeNames = checkAndGet(entry.getValue(), RangerSolrConstants.ATTR_NAMES);
					String filterType = checkAndGet(entry.getValue(), RangerSolrConstants.FIELD_FILTER_TYPE);
					boolean acceptEmpty = false;
					if (entry.getValue().getBooleanArg(RangerSolrConstants.PERMIT_EMPTY_VALUES) != null) {
						acceptEmpty = entry.getValue().getBooleanArg(RangerSolrConstants.PERMIT_EMPTY_VALUES);
					}
					String allUsersValue = getWithDefault(entry.getValue(), RangerSolrConstants.ALL_USERS_VALUE, "");
					String regex = getWithDefault(entry.getValue(), RangerSolrConstants.ATTRIBUTE_FILTER_REGEX, "");
					String extraOpts = getWithDefault(entry.getValue(), RangerSolrConstants.EXTRA_OPTS, "");
					FieldToAttributeMapping mapping = new FieldToAttributeMapping(solrFieldName, attributeNames, filterType, acceptEmpty, allUsersValue, regex, extraOpts);
					fieldAttributeMappings.add(mapping);
				}
			}
			this.andQParserName = this.<String>checkAndGet(args, RangerSolrConstants.AND_OP_QPARSER).trim();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.solr.security.SolrAuthorizationPlugin#init(java.util.Map)
	 */
	@Override
	public void init(Map<String, Object> initInfo) {
		logger.info("init()");

		try {
			RangerBasePlugin me = solrPlugin;
			if (me == null) {
				synchronized(RangerSolrAuthorizer.class) {
					me = solrPlugin;
					logger.info("RangerSolrAuthorizer(): init called");
					if (me == null) {
						authToJAASFile();
						logger.info("Creating RangerSolrPlugin");
						me = solrPlugin = new RangerBasePlugin("solr", "solr");
					}
					logger.info("Calling solrPlugin.init()");
					solrPlugin.init();
					solrPlugin.setResultProcessor(new RangerSolrAuditHandler(solrPlugin.getConfig()));
				}
			}

			useProxyIP = solrPlugin.getConfig().getBoolean(
					RangerSolrConstants.PROP_USE_PROXY_IP, useProxyIP);
			proxyIPHeader = solrPlugin.getConfig().get(
					RangerSolrConstants.PROP_PROXY_IP_HEADER, proxyIPHeader);
			// First get from the -D property
			solrAppName = System.getProperty("solr.kerberos.jaas.appname",
					solrAppName);
			// Override if required from Ranger properties
			solrAppName = solrPlugin.getConfig().get(
					RangerSolrConstants.PROP_SOLR_APP_NAME, solrAppName);

			logger.info("init(): useProxyIP=" + useProxyIP);
			logger.info("init(): proxyIPHeader=" + proxyIPHeader);
			logger.info("init(): solrAppName=" + solrAppName);
			logger.info("init(): KerberosName.rules="
					+ MiscUtil.getKerberosNamesRules());
		} catch (Throwable t) {
			logger.error("Error creating and initializing RangerBasePlugin()", t);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		logger.info("close() called");
		try {
			solrPlugin.cleanup();
			/* Solr shutdown is not graceful so that JVM shutdown hooks
			 * are not always invoked and the audit store are not flushed. So
			 * we are forcing a cleanup here.
			 */
			if (solrPlugin.getAuditProviderFactory() != null) {
				solrPlugin.getAuditProviderFactory().shutdown();
			}
		} catch (Throwable t) {
			logger.error("Error cleaning up Ranger plugin. Ignoring error", t);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.solr.security.SolrAuthorizationPlugin#authorize(org.apache
	 * .solr.security.SolrRequestContext)
	 */
	@Override
	public AuthorizationResponse authorize(AuthorizationContext context) {
		boolean isDenied = false;

		try {
			if (logger.isDebugEnabled()) {
				logger.debug("==> RangerSolrAuthorizer.authorize()");
				logAuthorizationContext(context);
			}

			RangerSolrAuditHandler auditHandler = new RangerSolrAuditHandler(solrPlugin.getConfig());

			RangerPerfTracer perf = null;

			if(RangerPerfTracer.isPerfTraceEnabled(RangerSolrConstants.PERF_SOLRAUTH_REQUEST_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(RangerSolrConstants.PERF_SOLRAUTH_REQUEST_LOG, "RangerSolrAuthorizer.authorize()");
			}

			String userName = getUserName(context);
			Set<String> userGroups = getGroupsForUser(userName);
			String ip = null;
			Date eventTime = new Date();

			// // Set the IP
			if (useProxyIP) {
				ip = context.getHttpHeader(proxyIPHeader);
			}
			if (ip == null) {
				ip = context.getHttpHeader("REMOTE_ADDR");
			}
			if (ip == null) {
				ip = context.getRemoteAddr();
			}

			// Create the list of requests for access check.
			// We are going to build a list of ranger requests which represent the requested privileges.
			// At the end will we iterate this list and invoke Ranger to check for privileges.
			List<RangerAccessRequestImpl> rangerRequests = new ArrayList<RangerAccessRequestImpl>();

			// The following logic is taken from Sentry See in SentrySolrPluginImpl.java.

			if (context.getHandler() instanceof PermissionNameProvider) {
				PermissionNameProvider.Name perm = ((PermissionNameProvider) context.getHandler()).getPermissionName(context);
				switch (perm) {
					case READ_PERM:
					case UPDATE_PERM: {
						RangerSolrConstants.ACCESS_TYPE accessType = (perm == PermissionNameProvider.Name.READ_PERM) ?
								RangerSolrConstants.ACCESS_TYPE.QUERY : RangerSolrConstants.ACCESS_TYPE.UPDATE;
						for (CollectionRequest req : context.getCollectionRequests()) {
							rangerRequests.add(createRequest(userName, userGroups, ip, eventTime, context,
									RangerSolrConstants.RESOURCE_TYPE.COLLECTION, req.collectionName, accessType));
						}
						break;

					}
					case SECURITY_EDIT_PERM: {
						rangerRequests.add(createAdminRequest(userName, userGroups, ip, eventTime, context,
								RangerSolrConstants.ADMIN_TYPE.SECURITY, RangerSolrConstants.ACCESS_TYPE.UPDATE));
						break;
					}
					case SECURITY_READ_PERM: {
						rangerRequests.add(createAdminRequest(userName, userGroups, ip, eventTime, context,
								RangerSolrConstants.ADMIN_TYPE.SECURITY, RangerSolrConstants.ACCESS_TYPE.QUERY));
						break;
					}
					case CORE_READ_PERM:
					case CORE_EDIT_PERM:
					case COLL_READ_PERM:
					case COLL_EDIT_PERM: {
						RangerSolrConstants.ADMIN_TYPE adminType =  (perm == PermissionNameProvider.Name.COLL_READ_PERM
								|| perm == PermissionNameProvider.Name.COLL_EDIT_PERM)
								? RangerSolrConstants.ADMIN_TYPE.COLLECTIONS : RangerSolrConstants.ADMIN_TYPE.CORES;

						RangerSolrConstants.ACCESS_TYPE accessType = (perm == PermissionNameProvider.Name.COLL_READ_PERM
								|| perm == PermissionNameProvider.Name.CORE_READ_PERM)
								? RangerSolrConstants.ACCESS_TYPE.QUERY : RangerSolrConstants.ACCESS_TYPE.UPDATE;

						// add admin permissions to the ranger request list
						rangerRequests.add(createAdminRequest(userName, userGroups, ip, eventTime, context,
									adminType, accessType));

						// add collection level permissions to the ranger request list
						Map<String, RangerSolrConstants.ACCESS_TYPE> collectionsForAdminOpMap =
								SolrAuthzUtil.getCollectionsForAdminOp(context);

						String finalIp = ip;
						collectionsForAdminOpMap.forEach((k, v) -> rangerRequests.add(createRequest(userName, userGroups,
								finalIp, eventTime, context, RangerSolrConstants.RESOURCE_TYPE.COLLECTION, k, v)));
						break;
					}
					case CONFIG_EDIT_PERM: {
						for (String s: SolrAuthzUtil.getConfigAuthorizables(context)) {
							rangerRequests.add(createRequest(userName, userGroups, ip, eventTime, context,
									RangerSolrConstants.RESOURCE_TYPE.CONFIG, s, RangerSolrConstants.ACCESS_TYPE.UPDATE));
						}
						break;
					}
					case CONFIG_READ_PERM: {
						for (String s: SolrAuthzUtil.getConfigAuthorizables(context)) {
							rangerRequests.add(createRequest(userName, userGroups, ip, eventTime, context,
									RangerSolrConstants.RESOURCE_TYPE.CONFIG, s, RangerSolrConstants.ACCESS_TYPE.QUERY));
						}
						break;
					}
					case SCHEMA_EDIT_PERM: {
						for (String s: SolrAuthzUtil.getSchemaAuthorizables(context)) {
							rangerRequests.add(createRequest(userName, userGroups, ip, eventTime, context,
									RangerSolrConstants.RESOURCE_TYPE.SCHEMA, s, RangerSolrConstants.ACCESS_TYPE.UPDATE));
						}
						break;
					}
					case SCHEMA_READ_PERM: {
						for (String s: SolrAuthzUtil.getSchemaAuthorizables(context)) {
							rangerRequests.add(createRequest(userName, userGroups, ip, eventTime, context,
									RangerSolrConstants.RESOURCE_TYPE.SCHEMA, s, RangerSolrConstants.ACCESS_TYPE.QUERY));
						}
						break;
					}
					case METRICS_HISTORY_READ_PERM:
					case METRICS_READ_PERM: {
						rangerRequests.add(createAdminRequest(userName, userGroups, ip, eventTime, context,
								RangerSolrConstants.ADMIN_TYPE.METRICS, RangerSolrConstants.ACCESS_TYPE.QUERY));
						break;
					}
					case AUTOSCALING_READ_PERM:
					case AUTOSCALING_HISTORY_READ_PERM: {
						rangerRequests.add(createAdminRequest(userName, userGroups, ip, eventTime, context,
								RangerSolrConstants.ADMIN_TYPE.AUTOSCALING, RangerSolrConstants.ACCESS_TYPE.QUERY));
						break;
					}
					case AUTOSCALING_WRITE_PERM: {
						rangerRequests.add(createAdminRequest(userName, userGroups, ip, eventTime, context,
								RangerSolrConstants.ADMIN_TYPE.AUTOSCALING, RangerSolrConstants.ACCESS_TYPE.UPDATE));
						break;
					}
					case ALL: {
						logger.debug("Not adding anything to the requested privileges, since permission is ALL");
					}
				}
			} else {
				logger.warn("Request Handler: " + context.getHandler().getClass().getName() + " is not an instance of PermissionNameProvider and so we are not able to" +
						" authenticate the request. Check SOLR-11623 for more information.");
			}

			/*
			 * The switch-case statement above handles all possible permission types. Some of the request handlers
			 * in SOLR do not implement PermissionNameProvider interface and hence are incapable to providing the
			 * type of permission to be enforced for this request. This is a design limitation (or a bug) on the SOLR
			 * side. Until that issue is resolved, Solr/Sentry plugin needs to return OK for such requests.
			 * Ref: SOLR-11623
			 */

			if (logger.isDebugEnabled()) {
				logger.debug("rangerRequests.size()=" + rangerRequests.size());
			}

			try {
				// Let's check the access for each request/resource
				for (RangerAccessRequestImpl rangerRequest : rangerRequests) {
					RangerAccessResult result = solrPlugin.isAccessAllowed(
							rangerRequest, auditHandler);
					if (logger.isDebugEnabled()) {
						logger.debug("rangerRequest=" + result);
					}
					if (result == null || !result.getIsAllowed()) {
						isDenied = true;
						// rejecting on first failure
						break;
					}
				}
			} finally {
				auditHandler.flushAudit();
				RangerPerfTracer.log(perf);
			}
		} catch (Throwable t) {
			isDenied = true;
			MiscUtil.logErrorMessageByInterval(logger, t.getMessage(), t);
		}
		AuthorizationResponse response = null;
		if (isDenied) {
			response = new AuthorizationResponse(403);
		} else {
			response = new AuthorizationResponse(200);
		}
		if (logger.isDebugEnabled()) {
			logger.debug( "<== RangerSolrAuthorizer.authorize() result: " + isDenied + "Response : " + response.getMessage());
		}
		return response;
	}

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		if (!enabled) {
			if (logger.isDebugEnabled()) {
				logger.debug("Solr Document level Authorization is not enabled!");
			}
			return;
		}

		String userName = getUserName(rb.req);
		if (RangerSolrConstants.SUPERUSER.equals(userName)) {
			return;
		}
		RangerSolrAuditHandler auditHandler = new RangerSolrAuditHandler(solrPlugin.getConfig());
		boolean isDenied = false;

		if (attrsEnabled) {
			if (logger.isDebugEnabled()) {
				logger.debug("Checking Ldap attributes to be added to the query filter");
			}
			if (getUserStoreEnricher() == null || getUserStoreEnricher().getRangerUserStore() == null) {
				logger.error("No User store enricher to read the ldap attributes");
				isDenied = true;
			}
			// Ranger UserStore info for user/group attributes
			Map<String, Map<String, String>> userAttrMapping = getUserStoreEnricher().getRangerUserStore().getUserAttrMapping();
			if (MapUtils.isNotEmpty(userAttrMapping)) {
				ModifiableSolrParams newParams = new ModifiableSolrParams(rb.req.getParams());
				Map<String, String> userAttributes = userAttrMapping.get(userName);
				for (FieldToAttributeMapping mapping : fieldAttributeMappings) {
					String filterQuery = buildFilterQueryString(userName, userAttributes, mapping);
					if (logger.isDebugEnabled()) {
						logger.debug("Adding filter clause : {}" + filterQuery);
					}
					newParams.add("fq", filterQuery);
				}

				rb.req.setParams(newParams);
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Checking User roles to be added to the query filter");
			}

			Set<String> roles = getRolesForUser(userName);
			if (roles != null && !roles.isEmpty()) {
				String filterQuery;
				if (matchMode == RangerSolrConstants.MatchType.DISJUNCTIVE) {
					filterQuery = getDisjunctiveFilterQueryStr(roles);
				} else {
					filterQuery = getConjunctiveFilterQueryStr(roles);
				}
				ModifiableSolrParams newParams = new ModifiableSolrParams(rb.req.getParams());
				newParams.add("fq", filterQuery);
				rb.req.setParams(newParams);
				if (logger.isDebugEnabled()) {
					logger.debug("Adding filter query {" + filterQuery + "} for user {" + userName + "} with roles {" + roles + "}");
				}

			} else {
				isDenied = true;
			}
		}
		HttpServletRequest httpServletRequest = (HttpServletRequest) rb.req.getContext().get("httpRequest");
		if (httpServletRequest == null) {
			SolrCore solrCore = rb.req.getCore();
			StringBuilder builder = new StringBuilder("Unable to locate HttpServletRequest");
			if (solrCore != null && !solrCore.getSolrConfig().get("requestDispatcher/requestParsers/@addHttpRequestToContext").boolVal(true)) {
				builder.append(", ensure requestDispatcher/requestParsers/@addHttpRequestToContext is set to true in solrconfig.xml");
			}
			throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, builder.toString());
		}
		String ip = null;
		Date eventTime = new Date();
		// Set the IP
		if (useProxyIP) {
			ip = httpServletRequest.getHeader("X-Forwarded-For");
		}

		if (ip == null) {
			ip = httpServletRequest.getRemoteAddr();
		}

		try {

			RangerAccessRequestImpl rangerRequest = createQueryRequest(
					userName, getGroupsForUser(userName), ip, eventTime, rb.req);
			if (isDenied == true) {
				RangerAccessResult result = new RangerAccessResult(RangerPolicy.POLICY_TYPE_ACCESS, solrPlugin.getServiceName(), solrPlugin.getServiceDef(), rangerRequest);
				result.setIsAllowed(false);
				result.setPolicyId(-1);
				result.setIsAccessDetermined(true);
				result.setIsAudited(true);
				auditHandler.processResult(result);
			} else {
				RangerAccessResult result = solrPlugin.isAccessAllowed(
						rangerRequest, auditHandler);
				if (logger.isDebugEnabled()) {
					logger.debug("rangerRequest=" + result);
				}
				if (result == null) {
					isDenied = true;
				}
			}
		} finally {
			auditHandler.flushAudit();
		}
		if (isDenied == true) {
			throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED,
					"Permission denied for user: " + userName);
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
	}

	@Override
	public String getDescription() {
		return "Handle Query Document Authorization";
	}

	private void authToJAASFile() {
		try {
			MiscUtil.setUGIFromJAASConfig(solrAppName);
			logger.info("LoginUser=" + MiscUtil.getUGILoginUser());
		} catch (Throwable t) {
			logger.error("Error authenticating for appName=" + solrAppName, t);
		}
	}

	/**
	 * @param context
	 */
	private void logAuthorizationContext(AuthorizationContext context) {
		try {
			// Note: This method should be called with isDebugEnabled()
			String collections = "";
			int i = -1;
			for (CollectionRequest collectionRequest : context
					.getCollectionRequests()) {
				i++;
				if (i > 0) {
					collections += ",";
				}
				collections += collectionRequest.collectionName;
			}

			String headers = "";
			i = -1;
			@SuppressWarnings("unchecked")
			Enumeration<String> eList = context.getHeaderNames();
			while (eList.hasMoreElements()) {
				i++;
				if (i > 0) {
					headers += ",";
				}
				String header = eList.nextElement();
				String value = context.getHttpHeader(header);
				headers += header + "=" + value;
			}

			String ipAddress = context.getHttpHeader("HTTP_X_FORWARDED_FOR");

			if (ipAddress == null) {
				ipAddress = context.getHttpHeader("REMOTE_HOST");
			}
			if (ipAddress == null) {
				ipAddress = context.getHttpHeader("REMOTE_ADDR");
			}
			if (ipAddress == null) {
				ipAddress = context.getRemoteAddr();
			}

			String userName = getUserName(context);
			Set<String> groups = getGroupsForUser(userName);
			String resource    = context.getResource();
			String solrParams  = "";
			try {
				solrParams = context.getParams().toQueryString();
			} catch (Throwable t) {
				//Exception ignored
			}
			RequestType requestType  = context.getRequestType();
			Principal	principal	 = context.getUserPrincipal();

			String contextString = new String("AuthorizationContext: ");
			contextString  = contextString + "context.getResource()= " + ((resource != null ) ? resource : "");
			contextString  = contextString + ", solarParams= " + (( solrParams != null ) ? solrParams : "");
			contextString  = contextString + ", requestType= " + (( requestType != null ) ? requestType : "");
			contextString  = contextString + ", userPrincipal= " + ((principal != null ) ? principal : "");
			contextString  = contextString + ", userName= "  + userName;
			contextString  = contextString + ", groups= " + groups;
			contextString  = contextString + ", ipAddress= " + ipAddress;
			contextString  = contextString + ", collections= " + collections;
			contextString  = contextString + ", headers= " + headers;

			logger.debug(contextString);
		} catch (Throwable t) {
			logger.error("Error getting request context!!!", t);
		}

	}

	/**
	 * @param userName
	 * @param userGroups
	 * @param ip
	 * @param eventTime
	 * @param context
	 * @param resourceType - the type of resource we are requesting permission to access (collection, config, field, etc.)
	 * @param resourceName - the name of the resource (e.g. collection) we are requesting permission to access
	 * @param accessType - the access type (QUERY, UPDATE, ALL (*)) we are requesting
	 * @return
	 */
	private RangerAccessRequestImpl createRequest(String userName, Set<String> userGroups, String ip, Date eventTime, AuthorizationContext context, RangerSolrConstants.RESOURCE_TYPE resourceType,
	String resourceName, RangerSolrConstants.ACCESS_TYPE accessType) {
		String action = accessType.toString();
		RangerAccessRequestImpl rangerRequest = createBaseRequest(userName,
				userGroups, ip, eventTime);
		RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();

		rangerResource.setValue(resourceType.toString(), resourceName);

		rangerRequest.setResource(rangerResource);
		rangerRequest.setAccessType(accessType.toString());
		rangerRequest.setAction(action);

		return rangerRequest;
	}


	private RangerAccessRequestImpl createAdminRequest(String userName, Set<String> userGroups, String ip, Date eventTime, AuthorizationContext context, RangerSolrConstants.ADMIN_TYPE adminType,
	RangerSolrConstants.ACCESS_TYPE accessType) {
		return createRequest(userName, userGroups, ip, eventTime, context, RangerSolrConstants.RESOURCE_TYPE.ADMIN,
				adminType.toString(), accessType);
	}

	private RangerAccessRequestImpl createQueryRequest(String userName, Set<String> userGroups, String ip, Date eventTime, SolrQueryRequest queryRequest) {
		String accessType = RangerSolrConstants.ACCESS_TYPE.QUERY.toString();
		String action = RangerSolrConstants.ACCESS_TYPE.QUERY.toString();
		RangerAccessRequestImpl rangerRequest = createBaseRequest(userName,
				userGroups, ip, eventTime);
		RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();
		rangerResource.setServiceDef(solrPlugin.getServiceDef());
		rangerResource.setValue(RangerSolrConstants.RESOURCE_TYPE.COLLECTION.toString(), queryRequest.getCore().getCoreDescriptor().getCollectionName());
		rangerRequest.setResource(rangerResource);
		rangerRequest.setAccessType(accessType);
		rangerRequest.setAction(action);
		rangerRequest.setRequestData(queryRequest.getParams().toLocalParamsString());
		rangerRequest.setClusterName(solrPlugin.getClusterName());

		return rangerRequest;
	}

	private RangerAccessRequestImpl createBaseRequest(String userName,
			Set<String> userGroups, String ip, Date eventTime) {
		RangerAccessRequestImpl rangerRequest = new RangerAccessRequestImpl();
		if (userName != null && !userName.isEmpty()) {
			rangerRequest.setUser(userName);
		}
		if (userGroups != null && userGroups.size() > 0) {
			rangerRequest.setUserGroups(userGroups);
		}
		if (ip != null && !ip.isEmpty()) {
			rangerRequest.setClientIPAddress(ip);
		}
		rangerRequest.setAccessTime(eventTime);
		return rangerRequest;
	}

	private String getUserName(AuthorizationContext context) {
		Principal principal = context.getUserPrincipal();
		if (principal != null) {
			return MiscUtil.getShortNameFromPrincipalName(principal.getName());
		}
		return null;
	}

	/**
	 * @param name
	 * @return
	 */
	private Set<String> getGroupsForUser(String name) {
		return MiscUtil.getGroupsForRequestUser(name);
	}



	private void addDisjunctiveRawClause(StringBuilder builder, String value) {
		// requires a space before the first term, so the
		// default lucene query parser will be used
		builder.append(" {!raw f=").append(authField).append(" v=")
				.append(value).append("}");
	}

	private String getDisjunctiveFilterQueryStr(Set<String> roles) {
		if (roles != null && !roles.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (String role : roles) {
				addDisjunctiveRawClause(builder, role);
			}
			if (allRolesToken != null && !allRolesToken.isEmpty()) {
				addDisjunctiveRawClause(builder, allRolesToken);
			}
			return builder.toString();
		}
		return null;
	}

	private String getConjunctiveFilterQueryStr(Set<String> roles) {
		StringBuilder filterQuery = new StringBuilder();
		filterQuery
				.append(" {!").append(qParserName)
				.append(" set_field=\"").append(authField).append("\"")
				.append(" set_value=\"").append(Joiner.on(',').join(roles.iterator())).append("\"")
				.append(" count_field=\"").append(tokenCountField).append("\"");
		if (allRolesToken != null && !allRolesToken.equals("")) {
			filterQuery.append(" wildcard_token=\"").append(allRolesToken).append("\"");
		}
		filterQuery.append(" allow_missing_val=").append(allowMissingValue).append(" }");

		return filterQuery.toString();
	}

	private Set<String> getRolesForUser(String name) {
		if (solrPlugin.getCurrentRangerAuthContext() != null) {
			return solrPlugin.getRolesFromUserAndGroups(name, getGroupsForUser(name));
		}
		else {
			logger.info("Current Ranger Auth Context is null!!");
			return null;
		}
	}

	/**
	 * This method return the user name from the provided {@linkplain SolrQueryRequest}
	 */
	private final String getUserName(SolrQueryRequest req) {
		// If a local request, treat it like a super user request; i.e. it is equivalent to an
		// http request from the same process.
		if (req instanceof LocalSolrQueryRequest) {
			return RangerSolrConstants.SUPERUSER;
		}

		SolrCore solrCore = req.getCore();

		HttpServletRequest httpServletRequest = (HttpServletRequest) req.getContext().get("httpRequest");
		if (httpServletRequest == null) {
			StringBuilder builder = new StringBuilder("Unable to locate HttpServletRequest");
			if (solrCore != null && !solrCore.getSolrConfig().get("requestDispatcher/requestParsers/@addHttpRequestToContext").boolVal(true)) {
				builder.append(", ensure requestDispatcher/requestParsers/@addHttpRequestToContext is set to true in solrconfig.xml");
			}
			throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, builder.toString());
		}

		String userName = httpServletRequest.getRemoteUser();
		if (userName == null) {
			userName = MiscUtil.getShortNameFromPrincipalName(httpServletRequest.getUserPrincipal().getName());
		}
		if (userName == null) {
			throw new SolrException(SolrException.ErrorCode.UNAUTHORIZED, "This request is not authenticated.");
		}

		return userName;
	}

	private RangerUserStoreEnricher getUserStoreEnricher() {
		RangerUserStoreEnricher ret         = null;
		RangerAuthContext authContext = solrPlugin.getCurrentRangerAuthContext();

		if (authContext != null) {
			Map<RangerContextEnricher, Object> contextEnricherMap = authContext.getRequestContextEnrichers();

			if (MapUtils.isNotEmpty(contextEnricherMap)) {
				Set<RangerContextEnricher> contextEnrichers = contextEnricherMap.keySet();

				for (RangerContextEnricher enricher : contextEnrichers) {
					if (enricher instanceof RangerUserStoreEnricher) {
						ret = (RangerUserStoreEnricher) enricher;

						break;
					}
				}
			}
		}
		return ret;
	}

	private <T> T checkAndGet(NamedList args, String key) {
		logger.info("checkAndGet() " + key);
		return (T) Preconditions.checkNotNull(args.get(key));
	}

	private <T> T getWithDefault(NamedList args, String key, T defaultValue) {
		T value = (T) args.get(key);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}

	private String buildFilterQueryString(String userName, Map<String, String> userAttributes, FieldToAttributeMapping mapping) {
		String fieldName = mapping.getFieldName();
		Collection<String> attributeValues = getUserAttributesForField(userName, userAttributes, mapping);
		switch (mapping.getFilterType()) {
			case OR:
				return buildSimpleORFilterQuery(fieldName, attributeValues, mapping.getAcceptEmpty(), mapping.getAllUsersValue(), mapping.getExtraOpts());
			case AND:
				return buildSubsetFilterQuery(fieldName, attributeValues, mapping.getAcceptEmpty(), mapping.getAllUsersValue(), mapping.getExtraOpts());
			case GTE:
				return buildGreaterThanFilterQuery(fieldName, attributeValues, mapping.getAcceptEmpty(), mapping.getAllUsersValue(), mapping.getExtraOpts());
			case LTE:
				return buildLessThanFilterQuery(fieldName, attributeValues, mapping.getAcceptEmpty(), mapping.getAllUsersValue(), mapping.getExtraOpts());
			default:
				return null;
		}
	}

	private Collection<String> getUserAttributesForField(String userName, Map<String, String> userAttributes, FieldToAttributeMapping mapping) {
		Set<String> userAttributesSubset = new HashSet<>();
		if (CollectionUtils.isNotEmpty(mapping.getAttributes())) {
			if (mapping.getAttributes().contains("groups")) {
				userAttributesSubset.addAll(getGroupsForUser(userName));
			}
		}
		for (String attributeName : mapping.getAttributes()) {
			userAttributesSubset.add(userAttributes.get(attributeName));
		}
		return userAttributesSubset;
	}

	private String buildSimpleORFilterQuery(String fieldName, Collection<String> attributeValues, boolean allowEmptyField, String allUsersValue, String extraOpts) {
		StringBuilder s = new StringBuilder();
		for (String attributeValue : attributeValues) {
			s.append(fieldName).append(":\"").append(attributeValue).append("\" ");
		}
		if (allUsersValue != null && !allUsersValue.equals("")) {
			s.append(fieldName).append(":\"").append(allUsersValue).append("\" ");
		}
		if (allowEmptyField) {
			s.append("(*:* AND -").append(fieldName).append(":*) ");
		}
		if (extraOpts != null && !extraOpts.equals("")) {
			s.append(extraOpts + " ");
		}
		s.deleteCharAt(s.length() - 1);
		return s.toString();
	}

	private String buildSubsetFilterQuery(String fieldName, Collection<String> attributeValues, boolean allowEmptyField, String allUsersValue, String extraOpts) {
		StringBuilder s = new StringBuilder();
		s.append("{!").append(andQParserName)
				.append(" set_field=").append(fieldName)
				.append(" set_value=").append(Joiner.on(',').join(attributeValues));
		if (allUsersValue != null && !allUsersValue.equals("")) {
			s.append(" wildcard_token=").append(allUsersValue);
		}
		if (allowEmptyField) {
			s.append(" allow_missing_val=true");
		} else {
			s.append(" allow_missing_val=false");
		}
		if (extraOpts != null && !extraOpts.equals("")) {
			s.append(" " + extraOpts);
		}
		s.append("}");
		return s.toString();
	}

	private String buildGreaterThanFilterQuery(String fieldName, Collection<String> attributeValues, boolean allowEmptyField, String allUsersValue, String extraOpts) {
		String value;
		if (attributeValues.size() == 1) {
			value = attributeValues.iterator().next();
		} else if (allUsersValue != null && !allUsersValue.equals("")) {
			value = allUsersValue;
		} else {
			throw new IllegalArgumentException("Greater Than Filter Query cannot be built for field " + fieldName);
		}
		StringBuilder extraClause = new StringBuilder();
		if (allowEmptyField) {
			extraClause.append(" (*:* AND -").append(fieldName).append(":*)");
		}
		if (extraOpts != null && !extraOpts.equals("")) {
			extraClause.append(" ").append(extraOpts);
		}
		return fieldName + ":[" + value + " TO *]" + extraClause.toString();
	}

	private String buildLessThanFilterQuery(String fieldName, Collection<String> attributeValues, boolean allowEmptyField, String allUsersValue, String extraOpts) {
		String value;
		if (attributeValues.size() == 1) {
			value = attributeValues.iterator().next();
		} else if (allUsersValue != null && !allUsersValue.equals("")) {
			value = allUsersValue;
		} else {
			throw new IllegalArgumentException("Less Than Filter Query cannot be built for field " + fieldName);
		}
		StringBuilder extraClause = new StringBuilder();
		if (allowEmptyField) {
			extraClause.append(" (*:* AND -").append(fieldName).append(":*)");
		}
		if (extraOpts != null && !extraOpts.equals("")) {
			extraClause.append(" ").append(extraOpts);
		}
		return fieldName + ":[* TO " + value + "]" + extraClause.toString();
	}

}
