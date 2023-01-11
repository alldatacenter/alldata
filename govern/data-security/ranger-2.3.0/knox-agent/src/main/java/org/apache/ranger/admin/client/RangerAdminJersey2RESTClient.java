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

package org.apache.ranger.admin.client;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.plugin.util.*;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.authorization.utils.StringUtil;
import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class RangerAdminJersey2RESTClient extends AbstractRangerAdminClient {

	// none of the members are public -- this is only for testability.  None of these is meant to be accessible
	private static final Logger LOG = LoggerFactory.getLogger(RangerAdminJersey2RESTClient.class);

	boolean _isSSL = false;
	volatile Client _client = null;
	SSLContext _sslContext = null;
	HostnameVerifier _hv;
	String _sslConfigFileName = null;
	String _serviceName = null;
	String _serviceNameUrlParam = null;
	String _clusterName = null;
	boolean _supportsPolicyDeltas = false;
	boolean _supportsTagDeltas = false;
	String _pluginId = null;
	int	   _restClientConnTimeOutMs;
	int	   _restClientReadTimeOutMs;
	int	   _restClientMaxRetryAttempts;
	int	   _restClientRetryIntervalMs;
	private int lastKnownActiveUrlIndex;
	private List<String> configURLs;
	private boolean			 isRangerCookieEnabled;
	private String			 rangerAdminCookieName;
	private Cookie           policyDownloadSessionId            = null;
	private boolean	         isValidPolicyDownloadSessionCookie = false;
	private Cookie			 tagDownloadSessionId               = null;
	private boolean			 isValidTagDownloadSessionCookie    = false;
	private Cookie			 roleDownloadSessionId              = null;
	private boolean			 isValidRoleDownloadSessionCookie   = false;
	//private Map<String, NewCookie>	 cookieMap                  = new HashMap<>();
	private final String     pluginCapabilities                 = Long.toHexString(new RangerPluginCapability().getPluginCapabilities());
	private static final int MAX_PLUGIN_ID_LEN                  = 255;

	@Override
	public void init(String serviceName, String appId, String configPropertyPrefix, Configuration config) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.init(" + configPropertyPrefix + ")");
		}

		super.init(serviceName, appId, configPropertyPrefix, config);

		_serviceName             = serviceName;
		_pluginId 		         = getPluginId(serviceName, appId);
		String tmpUrl 		     = config.get(configPropertyPrefix + ".policy.rest.url");
		_sslConfigFileName 		 = config.get(configPropertyPrefix + ".policy.rest.ssl.config.file");
		_restClientConnTimeOutMs = config.getInt(configPropertyPrefix + ".policy.rest.client.connection.timeoutMs", 120 * 1000);
		_restClientReadTimeOutMs = config.getInt(configPropertyPrefix + ".policy.rest.client.read.timeoutMs", 30 * 1000);
		_restClientMaxRetryAttempts	= config.getInt(configPropertyPrefix + ".policy.rest.client.max.retry.attempts", 3);
		_restClientRetryIntervalMs	= config.getInt(configPropertyPrefix + ".policy.rest.client.retry.interval.ms", 1 * 1000);

		_clusterName             = config.get(configPropertyPrefix + ".access.cluster.name", "");
		if(StringUtil.isEmpty(_clusterName)){
			_clusterName =config.get(configPropertyPrefix + ".ambari.cluster.name", "");
		}
		_supportsPolicyDeltas = config.getBoolean(configPropertyPrefix + RangerCommonConstants.PLUGIN_CONFIG_SUFFIX_POLICY_DELTA, RangerCommonConstants.PLUGIN_CONFIG_SUFFIX_POLICY_DELTA_DEFAULT);
		_supportsTagDeltas = config.getBoolean(configPropertyPrefix + RangerCommonConstants.PLUGIN_CONFIG_SUFFIX_TAG_DELTA, RangerCommonConstants.PLUGIN_CONFIG_SUFFIX_TAG_DELTA_DEFAULT);
		isRangerCookieEnabled = config.getBoolean(configPropertyPrefix + ".policy.rest.client.cookie.enabled", RangerCommonConstants.POLICY_REST_CLIENT_SESSION_COOKIE_ENABLED);
		rangerAdminCookieName = config.get(configPropertyPrefix + ".policy.rest.client.session.cookie.name", RangerCommonConstants.DEFAULT_COOKIE_NAME);

		configURLs = StringUtil.getURLs(tmpUrl);
		this.lastKnownActiveUrlIndex = new Random().nextInt(configURLs.size());
		String url = configURLs.get(this.lastKnownActiveUrlIndex);
		_isSSL = isSsl(url);
		LOG.info("Init params: " + String.format("Base URL[%s], SSL Config filename[%s], ServiceName=[%s], SupportsPolicyDeltas=[%s], ConfigURLs=[%s]", url, _sslConfigFileName, _serviceName, _supportsPolicyDeltas, _supportsTagDeltas, configURLs));
		
		_client = getClient();
		_client.property(ClientProperties.CONNECT_TIMEOUT, _restClientConnTimeOutMs);
		_client.property(ClientProperties.READ_TIMEOUT, _restClientReadTimeOutMs);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.init(" + configPropertyPrefix + "): " + _client.toString());
		}

		try {
			this._serviceNameUrlParam = URLEncoderUtil.encodeURIParam(serviceName);
		} catch (UnsupportedEncodingException e) {
			LOG.warn("Unsupported encoding, serviceName=" + serviceName);
			this._serviceNameUrlParam = serviceName;
		}
	}

	@Override
	public ServicePolicies getServicePoliciesIfUpdated(final long lastKnownVersion, final long lastActivationTimeInMillis) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getServicePoliciesIfUpdated(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final ServicePolicies servicePolicies;

		if (isRangerCookieEnabled && policyDownloadSessionId != null && isValidPolicyDownloadSessionCookie) {
			servicePolicies = getServicePoliciesIfUpdatedWithCookie(lastKnownVersion, lastActivationTimeInMillis);
		} else {
			servicePolicies = getServicePoliciesIfUpdatedWithCred(lastKnownVersion, lastActivationTimeInMillis);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getServicePoliciesIfUpdated(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + servicePolicies);
		}
		return servicePolicies;
	}

	@Override
	public RangerRoles getRolesIfUpdated(final long lastKnowRoleVersion, final long lastActivationTimeInMillis) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getRolesIfUpdated(" + lastKnowRoleVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final RangerRoles rangerRoles;

		if (isRangerCookieEnabled && roleDownloadSessionId != null && isValidRoleDownloadSessionCookie) {
			rangerRoles = getRangerRolesIfUpdatedWithCookie(lastKnowRoleVersion, lastActivationTimeInMillis);
		} else {
			rangerRoles = getRangerRolesIfUpdatedWithCred(lastKnowRoleVersion, lastActivationTimeInMillis);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getRolesIfUpdated(" + lastKnowRoleVersion + ", " + lastActivationTimeInMillis + "): " + rangerRoles);
		}
		return rangerRoles;
	}

	@Override
	public void grantAccess(GrantRevokeRequest request) throws Exception {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminRESTClient.grantAccess(" + request + ")");
		}

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put(RangerRESTUtils.REST_PARAM_PLUGIN_ID, _pluginId);

		String relativeURL = RangerRESTUtils.REST_URL_SERVICE_GRANT_ACCESS + _serviceName;
		Response response = get(queryParams, relativeURL);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		
		switch(httpResponseCode) {
		case -1:
			LOG.warn("Unexpected: Null response from policy server while granting access! Returning null!");
			throw new Exception("unknown error!");
		case 200:
			LOG.debug("grantAccess() suceeded: HTTP status=" + httpResponseCode);
			break;
		case 401:
			throw new AccessControlException();
		default:
			String body = response.readEntity(String.class);
			String message = String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, relativeURL);
			LOG.warn(message);
			throw new Exception("HTTP status: " + httpResponseCode);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminRESTClient.grantAccess(" + request + ")");
		}
	}

	@Override
	public void revokeAccess(GrantRevokeRequest request) throws Exception {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminRESTClient.grantAccess(" + request + ")");
		}

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put(RangerRESTUtils.REST_PARAM_PLUGIN_ID, _pluginId);

		String relativeURL = RangerRESTUtils.REST_URL_SERVICE_REVOKE_ACCESS + _serviceName;
		Response response = get(queryParams, relativeURL);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		
		switch(httpResponseCode) {
		case -1:
			LOG.warn("Unexpected: Null response from policy server while granting access! Returning null!");
			throw new Exception("unknown error!");
		case 200:
			LOG.debug("grantAccess() suceeded: HTTP status=" + httpResponseCode);
			break;
		case 401:
			throw new AccessControlException();
		default:
			String body = response.readEntity(String.class);
			String message = String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, relativeURL);
			LOG.warn(message);
			throw new Exception("HTTP status: " + httpResponseCode);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminRESTClient.grantAccess(" + request + ")");
		}
	}

	@Override
	public ServiceTags getServiceTagsIfUpdated(final long lastKnownVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getServiceTagsIfUpdated(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final ServiceTags serviceTags;

		if (isRangerCookieEnabled && tagDownloadSessionId != null && isValidTagDownloadSessionCookie) {
			serviceTags = getServiceTagsIfUpdatedWithCookie(lastKnownVersion, lastActivationTimeInMillis);
		} else {
			serviceTags = getServiceTagsIfUpdatedWithCred(lastKnownVersion, lastActivationTimeInMillis);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getServiceTagsIfUpdated(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + serviceTags);
		}
		return serviceTags;
	}

	@Override
	public List<String> getTagTypes(String pattern) throws Exception {
		throw new Exception("RangerAdminjersey2RESTClient.getTagTypes() -- *** NOT IMPLEMENTED *** ");
	}

	@Override
	public RangerUserStore getUserStoreIfUpdated(long lastKnownUserStoreVersion, long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminjersey2RESTClient.getUserStoreIfUpdated(lastKnownUserStoreVersion={}, lastActivationTimeInMillis={})", lastKnownUserStoreVersion, lastActivationTimeInMillis);
		}

		final RangerUserStore      ret;
		final UserGroupInformation user         = MiscUtil.getUGILoginUser();
		final boolean              isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response             response;

		Map<String, String> queryParams = new HashMap<String, String>();

		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_KNOWN_USERSTORE_VERSION, Long.toString(lastKnownUserStoreVersion));
		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_ACTIVATION_TIME, Long.toString(lastActivationTimeInMillis));
		queryParams.put(RangerRESTUtils.REST_PARAM_PLUGIN_ID, _pluginId);
		queryParams.put(RangerRESTUtils.REST_PARAM_CLUSTER_NAME, _clusterName);
		queryParams.put(RangerRESTUtils.REST_PARAM_CAPABILITIES, pluginCapabilities);

		if (isSecureMode) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking UserStore updated as user: {}", user);
			}

			PrivilegedAction<Response> action = () -> {
				Response resp        = null;
				String   relativeURL = RangerRESTUtils.REST_URL_SERVICE_SERCURE_GET_USERSTORE + _serviceNameUrlParam;

				try {
					resp = get(queryParams, relativeURL);
				} catch (Exception e) {
					LOG.error("Failed to get response", e);
				}

				return resp;
			};

			response = user.doAs(action);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking UserStore updated as user: {}", user);
			}

			String relativeURL = RangerRESTUtils.REST_URL_SERVICE_GET_USERSTORE + _serviceNameUrlParam;

			response = get(queryParams, relativeURL);
		}

		if (response == null || response.getStatus() == 304) { // NOT_MODIFIED
			if (response == null) {
				LOG.error("Error getting UserStore; Received NULL response!!. secureMode={}, user={}, serviceName={}", isSecureMode, user, _serviceName);
			} else {
				String resp = response.hasEntity() ? response.readEntity(String.class) : null;

				if (LOG.isDebugEnabled()) {
					LOG.debug("No change in UserStore. secureMode={}, user={}, response={}, serviceName={}, lastKnownUserStoreVersion={}, lastActivationTimeInMillis={}",
							  isSecureMode, user, resp, _serviceName, lastKnownUserStoreVersion, lastActivationTimeInMillis);
				}
			}

			ret = null;
		} else if (response.getStatus() == 200) { // OK
			ret = response.readEntity(RangerUserStore.class);
		} else if (response.getStatus() == 404) { // NOT_FOUND
			ret = null;

			LOG.error("Error getting UserStore; service not found. secureMode={}, user={}, response={}, serviceName={}, lastKnownUserStoreVersion={}, lastActivationTimeInMillis={}",
					  isSecureMode, user, response.getStatus(), _serviceName, lastKnownUserStoreVersion, lastActivationTimeInMillis);

			String exceptionMsg = response.hasEntity() ? response.readEntity(String.class) : null;

			RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, exceptionMsg);

			LOG.warn("Received 404 error code with body:[{}], Ignoring", exceptionMsg);
		} else {
			String resp = response.hasEntity() ? response.readEntity(String.class) : null;

			LOG.warn("Error getting UserStore. secureMode={}, user={}, response={}, serviceName={}, lastKnownUserStoreVersion={}, lastActivationTimeInMillis={}",
					 isSecureMode, user, resp, _serviceName, lastKnownUserStoreVersion, lastActivationTimeInMillis);

			ret = null;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminjersey2RESTClient.getUserStoreIfUpdated(lastKnownUserStoreVersion={}, lastActivationTimeInMillis={}): ret={}", lastKnownUserStoreVersion, lastActivationTimeInMillis, ret);
		}

		return ret;
	}

	// We get date from the policy manager as unix long!  This deserializer exists to deal with it.  Remove this class once we start send date/time per RFC 3339
	public static class GsonUnixDateDeserializer implements JsonDeserializer<Date> {

		@Override
		public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return new Date(json.getAsJsonPrimitive().getAsLong());
		}

	}

	// package level methods left so (and not private only for testability!)  Not intended for use outside this class!!
	Gson getGson() {
		return new GsonBuilder()
			.setPrettyPrinting()
			// We get date from the policy manager as unix long!  This deserializer exists to deal with it.  Remove this class once we start send date/time per RFC 3339
			.registerTypeAdapter(Date.class, new GsonUnixDateDeserializer())
			.create();
	}
	
	Client getClient() {
		Client result = _client;
		if(result == null) {
			synchronized(this) {
				result = _client;
				if(result == null) {
					_client = result = buildClient();
				}
			}
		}

		return result;
	}

	Client buildClient() {
		
		if (_isSSL) {
			if (_sslContext == null) {
				RangerSslHelper sslHelper = new RangerSslHelper(_sslConfigFileName);
				_sslContext = sslHelper.createContext();
			}
			if (_hv == null) {
				_hv = new HostnameVerifier() {
					public boolean verify(String urlHostName, SSLSession session) {
						return session.getPeerHost().equals(urlHostName);
					}
				};
			}				
			_client = ClientBuilder.newBuilder()
					.sslContext(_sslContext)
					.hostnameVerifier(_hv)
					.build();
		}

		if(_client == null) {
			_client = ClientBuilder.newClient();
		}
		
		return _client;
	}

	private Response get(Map<String, String> queyParams, String relativeURL) {
		Response response = null;
		int startIndex = this.lastKnownActiveUrlIndex;
        int currentIndex = 0;
        int retryAttempt = 0;

		for (int index = 0; index < configURLs.size(); index++) {
			try {
				currentIndex = (startIndex + index) % configURLs.size();

				WebTarget target = _client.target(configURLs.get(currentIndex) + relativeURL);
				response = setQueryParams(target, queyParams).request(MediaType.APPLICATION_JSON_TYPE).get();
				if (response != null) {
					setLastKnownActiveUrlIndex(currentIndex);
					break;
				}
			} catch (ProcessingException e) {
				if (shouldRetry(configURLs.get(currentIndex), index, retryAttempt, e)) {
					retryAttempt++;

					index = -1; // start from first url
				}
			}
		}
		return response;
	}

	private Response get(Map<String, String> queyParams, String relativeURL, Cookie sessionId) {
		Response response = null;
		int startIndex = this.lastKnownActiveUrlIndex;
		int currentIndex = 0;
		int retryAttempt = 0;

		for (int index = 0; index < configURLs.size(); index++) {
			try {
				currentIndex = (startIndex + index) % configURLs.size();

				WebTarget target = _client.target(configURLs.get(currentIndex)+relativeURL);
				target = setQueryParams(target, queyParams);
				Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE).cookie(sessionId);
				response = invocationBuilder.get();
				if (response != null) {
					setLastKnownActiveUrlIndex(currentIndex);
					break;
				}
			} catch (ProcessingException e) {
				if (shouldRetry(configURLs.get(currentIndex), index, retryAttempt, e)) {
					retryAttempt++;

					index = -1; // start from first url
				}
			}
		}
		return response;
	}

	private static WebTarget setQueryParams(WebTarget target, Map<String, String> params) {
		WebTarget ret = target;
		if (target != null && params != null) {
			Set<Map.Entry<String, String>> entrySet = params.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				ret = ret.queryParam(entry.getKey(), entry.getValue());
			}
		}
		return ret;
	}

	private void setLastKnownActiveUrlIndex(int lastKnownActiveUrlIndex) {
		this.lastKnownActiveUrlIndex = lastKnownActiveUrlIndex;
	}

	private boolean isSsl(String url) {
		return !StringUtils.isEmpty(url) && url.toLowerCase().startsWith("https");
	}

	private String getPluginId(String serviceName, String appId) {
		String hostName = null;

		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			LOG.error("ERROR: Unable to find hostname for the agent ", e);
			hostName = "unknownHost";
		}

		String ret  = hostName + "-" + serviceName;

		if(! StringUtils.isEmpty(appId)) {
			ret = appId + "@" + ret;
		}

		if (ret.length() > MAX_PLUGIN_ID_LEN ) {
			ret = ret.substring(0,MAX_PLUGIN_ID_LEN);
		}

		return ret ;
	}

	/* Policies Download from Ranger admin */
	private ServicePolicies getServicePoliciesIfUpdatedWithCred(final long lastKnownVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getServicePoliciesWithCred(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final ServicePolicies ret;

		final UserGroupInformation user         = MiscUtil.getUGILoginUser();
		final boolean              isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response             response     = getRangerAdminPolicyDownloadResponse(lastKnownVersion, lastActivationTimeInMillis, user, isSecureMode);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		String body = null;

		switch (httpResponseCode) {
			case 200:
				body = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Response from 200 server: " + body);
				}

				Gson gson = getGson();
				ret = gson.fromJson(body, ServicePolicies.class);
				setCookieReceivedFromCredSession(response);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Deserialized response to: " + ret);
				}
				break;
			case 304:
				ret = null;
				setCookieReceivedFromCredSession(response);
				LOG.debug("Got response: 304. Ok. Returning null");
				break;
			case -1:
				ret = null;
				policyDownloadSessionId = null;
				LOG.warn("Unexpected: Null response from policy server while trying to get policies! Returning null!");
				break;
			case 404:
				ret  = null;
				policyDownloadSessionId = null;
				if (response.hasEntity()) {
					body = response.readEntity(String.class);
					if (StringUtils.isNotBlank(body)) {
						RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, body);
					}
				}
				LOG.warn("Received 404 error code with body:[" + body + "], Ignoring");
				break;
			default:
				ret = null;
				policyDownloadSessionId = null;
				body = response.readEntity(String.class);
				LOG.warn(String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, getRelativeURL(isSecureMode)));
				break;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getServicePoliciesWithCred(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private ServicePolicies getServicePoliciesIfUpdatedWithCookie(final long lastKnownVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getServicePoliciesWithCookie(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final ServicePolicies ret;

		final UserGroupInformation user         = MiscUtil.getUGILoginUser();
		final boolean              isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response 	           response     = getRangerAdminPolicyDownloadResponse(lastKnownVersion, lastActivationTimeInMillis, user, isSecureMode);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		String body = null;

		switch (httpResponseCode) {
			case 200:
				body = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Response from 200 server: " + body);
				}

				Gson gson = getGson();
				ret = gson.fromJson(body, ServicePolicies.class);
				checkAndResetSessionCookie(response);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Deserialized response to: " + ret);
				}
				break;
			case 304:
				ret = null;
				checkAndResetSessionCookie(response);
				LOG.debug("Got response: 304. Ok. Returning null");
				break;
			case -1:
				ret = null;
				policyDownloadSessionId = null;
				isValidPolicyDownloadSessionCookie = false;
				LOG.warn("Unexpected: Null response from policy server while trying to get policies! Returning null!");
				break;
			case 404:
				ret  = null;
				policyDownloadSessionId = null;
				isValidPolicyDownloadSessionCookie = false;
				if (response.hasEntity()) {
					body = response.readEntity(String.class);
					if (StringUtils.isNotBlank(body)) {
						RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, body);
					}
				}
				LOG.warn("Received 404 error code with body:[" + body + "], Ignoring");
				break;
			default:
				ret = null;
				policyDownloadSessionId = null;
				isValidPolicyDownloadSessionCookie = false;
				body = response.readEntity(String.class);
				LOG.warn(String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, getRelativeURL(isSecureMode)));
				break;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getServicePoliciesWithCookie(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private Response getRangerAdminPolicyDownloadResponse(final long lastKnownVersion, final long lastActivationTimeInMillis, final UserGroupInformation user, final boolean isSecureMode) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getRangerAdminPolicyDownloadResponse(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final Response ret;

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_KNOWN_POLICY_VERSION, Long.toString(lastKnownVersion));
		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_ACTIVATION_TIME, Long.toString(lastActivationTimeInMillis));
		queryParams.put(RangerRESTUtils.REST_PARAM_PLUGIN_ID, _pluginId);
		queryParams.put(RangerRESTUtils.REST_PARAM_CLUSTER_NAME, _clusterName);
		queryParams.put(RangerRESTUtils.REST_PARAM_SUPPORTS_POLICY_DELTAS, Boolean.toString(_supportsPolicyDeltas));
		queryParams.put(RangerRESTUtils.REST_PARAM_CAPABILITIES, pluginCapabilities);

		final String relativeURL = getRelativeURL(isSecureMode);

		if (isSecureMode) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking Service policy if updated as user : " + user);
			}
			PrivilegedAction<Response> action = new PrivilegedAction<Response>() {
				public Response run() {
					return get(queryParams, relativeURL, policyDownloadSessionId);
				}
			};
			ret = user.doAs(action);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking Service policy if updated with old api call");
			}
			ret = get(queryParams, relativeURL, policyDownloadSessionId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getRangerAdminPolicyDownloadResponse(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private String getRelativeURL(final boolean isSecureMode) {
		final String ret;
		if (isSecureMode){
			ret = RangerRESTUtils.REST_URL_POLICY_GET_FOR_SECURE_SERVICE_IF_UPDATED + _serviceName;
		} else {
			ret = RangerRESTUtils.REST_URL_POLICY_GET_FOR_SERVICE_IF_UPDATED + _serviceName;
		}
		return ret;
	}

	private void checkAndResetSessionCookie(Response response) {
		Map<String, NewCookie> cookieMap = response.getCookies();
		Set<String> cookieNames = cookieMap.keySet();
		for (String cookieName : cookieNames) {
			if (cookieName.equalsIgnoreCase(rangerAdminCookieName)) {
				policyDownloadSessionId = cookieMap.get(cookieName);
				isValidPolicyDownloadSessionCookie = (policyDownloadSessionId != null);
				break;
			}
		}
	}

	private void setCookieReceivedFromCredSession(Response response) {
		if (isRangerCookieEnabled) {
			Cookie sessionCookie = null;
			Map<String, NewCookie> cookieMap = response.getCookies();
			// save cookie received from credentials session login
			Set<String> cookieNames = cookieMap.keySet();
			for (String cookieName : cookieNames) {
				if (cookieName.equalsIgnoreCase(rangerAdminCookieName)) {
					sessionCookie = cookieMap.get(cookieName);
					break;
				}
			}
			policyDownloadSessionId = sessionCookie;
			isValidPolicyDownloadSessionCookie = (policyDownloadSessionId != null);
		}
	}

	/* Tags Download from Ranger admin */
	private ServiceTags getServiceTagsIfUpdatedWithCred(final long lastKnownVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getServiceTagsIfUpdatedWithCred(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final ServiceTags ret;

		final UserGroupInformation user         = MiscUtil.getUGILoginUser();
		final boolean              isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response             response     = getTagsDownloadResponse(lastKnownVersion, lastActivationTimeInMillis, user, isSecureMode);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		String body = null;

		switch (httpResponseCode) {
			case 200:
				body = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Response from 200 server: " + body);
				}

				Gson gson = getGson();
				ret = gson.fromJson(body, ServiceTags.class);
				setCookieReceivedFromTagDownloadSession(response);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Deserialized response to: " + ret);
				}
				break;
			case 304:
				ret = null;
				setCookieReceivedFromTagDownloadSession(response);
				LOG.debug("Got response: 304. Ok. Returning null");
				break;
			case -1:
				ret = null;
				tagDownloadSessionId = null;
				LOG.warn("Unexpected: Null response from tag server while trying to get tags! Returning null!");
				break;
			case 404:
				ret = null;
				tagDownloadSessionId = null;
				if (response.hasEntity()) {
					body = response.readEntity(String.class);
					if (StringUtils.isNotBlank(body)) {
						RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, body);
					}
				}
				LOG.warn("Received 404 error code with body:[" + body + "], Ignoring");
				break;
			default:
				ret = null;
				tagDownloadSessionId = null;
				body = response.readEntity(String.class);
				LOG.warn(String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, getRelativeURLForTagDownload(isSecureMode)));
				break;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getServiceTagsIfUpdatedWithCred(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private ServiceTags getServiceTagsIfUpdatedWithCookie(final long lastKnownVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getServiceTagsIfUpdatedWithCookie(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final ServiceTags ret;

		final UserGroupInformation user = MiscUtil.getUGILoginUser();
		final boolean isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response response = getTagsDownloadResponse(lastKnownVersion, lastActivationTimeInMillis, user, isSecureMode);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		String body = null;

		switch (httpResponseCode) {
			case 200:
				body = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Response from 200 server: " + body);
				}

				Gson gson = getGson();
				ret = gson.fromJson(body, ServiceTags.class);
				checkAndResetTagDownloadSessionCookie(response);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Deserialized response to: " + ret);
				}
				break;
			case 304:
				ret = null;
				checkAndResetTagDownloadSessionCookie(response);
				LOG.debug("Got response: 304. Ok. Returning null");
				break;
			case -1:
				ret = null;
				tagDownloadSessionId = null;
				isValidTagDownloadSessionCookie = false;
				LOG.warn("Unexpected: Null response from tag server while trying to get tags! Returning null!");
				break;
			case 404:
				ret = null;
				tagDownloadSessionId = null;
				isValidTagDownloadSessionCookie = false;
				if (response.hasEntity()) {
					body = response.readEntity(String.class);
					if (StringUtils.isNotBlank(body)) {
						RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, body);
					}
				}
				LOG.warn("Received 404 error code with body:[" + body + "], Ignoring");
				break;
			default:
				ret = null;
				tagDownloadSessionId = null;
				isValidTagDownloadSessionCookie = false;
				body = response.readEntity(String.class);
				LOG.warn(String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, ret));
				break;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getServiceTagsIfUpdatedWithCookie(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private Response getTagsDownloadResponse(final long lastKnownVersion, final long lastActivationTimeInMillis, final UserGroupInformation user, final boolean isSecureMode) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getTagsDownloadResponse(" + lastKnownVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final Response ret;

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put(RangerRESTUtils.LAST_KNOWN_TAG_VERSION_PARAM, Long.toString(lastKnownVersion));
		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_ACTIVATION_TIME, Long.toString(lastActivationTimeInMillis));
		queryParams.put(RangerRESTUtils.REST_PARAM_PLUGIN_ID, _pluginId);
		queryParams.put(RangerRESTUtils.REST_PARAM_SUPPORTS_TAG_DELTAS, Boolean.toString(_supportsTagDeltas));
		queryParams.put(RangerRESTUtils.REST_PARAM_CAPABILITIES, pluginCapabilities);

		final String relativeURL = getRelativeURLForTagDownload(isSecureMode);

		if (isSecureMode) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking Service tags if updated as user : " + user);
			}
			PrivilegedAction<Response> action = new PrivilegedAction<Response>() {
				public Response run() {
					return get(queryParams, relativeURL, tagDownloadSessionId);
				}
			};
			ret = user.doAs(action);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking Service tags if updated with old api call");
			}
			ret = get(queryParams, relativeURL, tagDownloadSessionId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getTagsDownloadResponse(" + lastKnownVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private String getRelativeURLForTagDownload(final boolean isSecureMode) {
		final String ret;
		if (isSecureMode){
			ret = RangerRESTUtils.REST_URL_GET_SECURE_SERVICE_TAGS_IF_UPDATED + _serviceName;
		} else {
			ret = RangerRESTUtils.REST_URL_GET_SERVICE_TAGS_IF_UPDATED + _serviceName;
		}
		return ret;
	}

	private void checkAndResetTagDownloadSessionCookie(Response response) {
		Map<String,NewCookie> cookieMap   = response.getCookies();
		Set<String> 		  cookieNames = cookieMap.keySet();
		for (String cookieName : cookieNames) {
			if (cookieName.equalsIgnoreCase(rangerAdminCookieName)) {
				tagDownloadSessionId = cookieMap.get(cookieName);
				isValidTagDownloadSessionCookie = (tagDownloadSessionId != null);
				break;
			}
		}
	}

	private void setCookieReceivedFromTagDownloadSession(Response response) {
		if (isRangerCookieEnabled) {
			Cookie sessionCookie = null;
			Map<String, NewCookie> cookieMap = response.getCookies();
			// save cookie received from credentials session login
			Set<String> cookieNames = cookieMap.keySet();
			for (String cookieName : cookieNames) {
				if (cookieName.equalsIgnoreCase(rangerAdminCookieName)) {
					sessionCookie = cookieMap.get(cookieName);
				}
			}
			tagDownloadSessionId = sessionCookie;
			isValidTagDownloadSessionCookie = (tagDownloadSessionId != null);
		}
	}

	/* Role Download from Ranger Admin */
	private RangerRoles getRangerRolesIfUpdatedWithCred(final long lastKnownRoleVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getRangerRolesIfUpdatedWithCred(" + lastKnownRoleVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final RangerRoles ret;

		final UserGroupInformation user         = MiscUtil.getUGILoginUser();
		final boolean              isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response             response     = getRoleDownloadResponse(lastKnownRoleVersion, lastActivationTimeInMillis, user, isSecureMode);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		String body = null;

		switch (httpResponseCode) {
			case 200:
				body = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Response from 200 server: " + body);
				}

				Gson gson = getGson();
				ret = gson.fromJson(body, RangerRoles.class);
				setCookieReceivedFromRoleDownloadSession(response);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Deserialized response to: " + ret);
				}
				break;
			case 304:
				ret = null;
				setCookieReceivedFromRoleDownloadSession(response);
				LOG.debug("Got response: 304. Ok. Returning null");
				break;
			case -1:
				ret = null;
				roleDownloadSessionId = null;
				LOG.warn("Unexpected: Null response from policy server while trying to get policies! Returning null!");
				break;
			case 404:
				ret = null;
				roleDownloadSessionId = null;
				if (response.hasEntity()) {
					body = response.readEntity(String.class);
					if (StringUtils.isNotBlank(body)) {
						RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, body);
					}
				}
				LOG.warn("Received 404 error code with body:[" + body + "], Ignoring");
				break;
			default:
				ret = null;
				roleDownloadSessionId = null;
				body = response.readEntity(String.class);
				LOG.warn(String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, getRelativeURLForRoleDownload(isSecureMode)));
				break;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getRangerRolesIfUpdatedWithCred(" + lastKnownRoleVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private RangerRoles getRangerRolesIfUpdatedWithCookie(final long lastKnownRoleVersion, final long lastActivationTimeInMillis) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getRangerRolesIfUpdatedWithCookie(" + lastKnownRoleVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final RangerRoles ret;

		final UserGroupInformation user = MiscUtil.getUGILoginUser();
		final boolean isSecureMode = user != null && UserGroupInformation.isSecurityEnabled();
		final Response response = getRoleDownloadResponse(lastKnownRoleVersion, lastActivationTimeInMillis, user, isSecureMode);

		int httpResponseCode = response == null ? -1 : response.getStatus();
		String body = null;

		switch (httpResponseCode) {
			case 200:
				body = response.readEntity(String.class);

				if (LOG.isDebugEnabled()) {
					LOG.debug("Response from 200 server: " + body);
				}

				Gson gson = getGson();
				ret = gson.fromJson(body, RangerRoles.class);
				checkAndResetRoleDownloadSessionCookie(response);
				if (LOG.isDebugEnabled()) {
					LOG.debug("Deserialized response to: " + ret);
				}
				break;
			case 304:
				ret = null;
				checkAndResetRoleDownloadSessionCookie(response);
				LOG.debug("Got response: 304. Ok. Returning null");
				break;
			case -1:
				ret = null;
				roleDownloadSessionId = null;
				isValidRoleDownloadSessionCookie = false;
				LOG.warn("Unexpected: Null response from policy server while trying to get policies! Returning null!");
				break;
			case 404:
				ret = null;
				roleDownloadSessionId = null;
				isValidRoleDownloadSessionCookie = false;
				if (response.hasEntity()) {
					body = response.readEntity(String.class);
					if (StringUtils.isNotBlank(body)) {
						RangerServiceNotFoundException.throwExceptionIfServiceNotFound(_serviceName, body);
					}
				}
				LOG.warn("Received 404 error code with body:[" + body + "], Ignoring");
				break;
			default:
				ret = null;
				roleDownloadSessionId = null;
				isValidRoleDownloadSessionCookie = false;
				body = response.readEntity(String.class);
				LOG.warn(String.format("Unexpected: Received status[%d] with body[%s] form url[%s]", httpResponseCode, body, getRelativeURLForRoleDownload(isSecureMode)));
				break;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getRangerRolesIfUpdatedWithCookie(" + lastKnownRoleVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private Response getRoleDownloadResponse(final long lastKnownRoleVersion, final long lastActivationTimeInMillis, final UserGroupInformation user, final boolean isSecureMode) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAdminJersey2RESTClient.getRoleDownloadResponse(" + lastKnownRoleVersion + ", " + lastActivationTimeInMillis + ")");
		}

		final Response ret;

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_KNOWN_ROLE_VERSION, Long.toString(lastKnownRoleVersion));
		queryParams.put(RangerRESTUtils.REST_PARAM_LAST_ACTIVATION_TIME, Long.toString(lastActivationTimeInMillis));
		queryParams.put(RangerRESTUtils.REST_PARAM_PLUGIN_ID, _pluginId);
		queryParams.put(RangerRESTUtils.REST_PARAM_CLUSTER_NAME, _clusterName);

		final String relativeURL = getRelativeURLForRoleDownload(isSecureMode);

		if (isSecureMode) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking Roles if updated as user : " + user);
			}
			PrivilegedAction<Response> action = new PrivilegedAction<Response>() {
				public Response run() {
					return get(queryParams, relativeURL, roleDownloadSessionId);
				}
			};
			ret = user.doAs(action);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Checking Roles if updated with old api call");
			}
			ret = get(queryParams, relativeURL, roleDownloadSessionId);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAdminJersey2RESTClient.getRoleDownloadResponse(" + lastKnownRoleVersion + ", " + lastActivationTimeInMillis + "): " + ret);
		}

		return ret;
	}

	private String getRelativeURLForRoleDownload(final boolean isSecureMode) {
		final String ret;
		if (isSecureMode){
			ret = RangerRESTUtils.REST_URL_SERVICE_SERCURE_GET_USER_GROUP_ROLES + _serviceName;
		} else {
			ret = RangerRESTUtils.REST_URL_SERVICE_GET_USER_GROUP_ROLES + _serviceName;
		}
		return ret;
	}

	private void checkAndResetRoleDownloadSessionCookie(Response response) {
		Map<String,NewCookie> cookieMap   = response.getCookies();
		Set<String> 		  cookieNames = cookieMap.keySet();
		for (String cookieName : cookieNames) {
			if (cookieName.equalsIgnoreCase(rangerAdminCookieName)) {
				roleDownloadSessionId = cookieMap.get(cookieName);
				isValidRoleDownloadSessionCookie = (roleDownloadSessionId != null);
				break;
			}
		}
	}

	private void setCookieReceivedFromRoleDownloadSession(Response response) {
		if (isRangerCookieEnabled) {
			Cookie sessionCookie = null;
			Map<String, NewCookie> cookieMap = response.getCookies();
			// save cookie received from credentials session login
			Set<String> cookieNames = cookieMap.keySet();
			for (String cookieName : cookieNames) {
				if (cookieName.equalsIgnoreCase(rangerAdminCookieName)) {
					sessionCookie = cookieMap.get(cookieName);
					break;
				}
			}
			roleDownloadSessionId = sessionCookie;
			isValidRoleDownloadSessionCookie = (roleDownloadSessionId != null);
		}
	}

	protected boolean shouldRetry(String currentUrl, int index, int retryAttemptCount, ProcessingException ex) {
		LOG.warn("Failed to communicate with Ranger Admin. URL: " + currentUrl + ". Error: " + ex.getMessage());

		boolean isLastUrl = index == (configURLs.size() - 1);

		// attempt retry after failure on the last url
		boolean ret = isLastUrl && (retryAttemptCount < _restClientMaxRetryAttempts);

		if (ret) {
			LOG.warn("Waiting for " + _restClientRetryIntervalMs + "ms before retry attempt #" + (retryAttemptCount + 1));

			try {
				Thread.sleep(_restClientRetryIntervalMs);
			} catch (InterruptedException excp) {
				LOG.error("Failed while waiting to retry", excp);
			}
		} else if (isLastUrl) {
			LOG.error("Failed to communicate with all Ranger Admin's URL's : [ " + configURLs + " ]");

			throw new ProcessingException("Failed to communicate with all Ranger Admin's URL : [ "+ configURLs+" ]", ex);
		}

		return ret;
	}
}
