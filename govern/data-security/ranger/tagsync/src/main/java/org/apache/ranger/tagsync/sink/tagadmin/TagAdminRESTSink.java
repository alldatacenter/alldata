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

package org.apache.ranger.tagsync.sink.tagadmin;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.admin.client.datatype.RESTResponse;
import org.apache.ranger.plugin.util.RangerRESTClient;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.tagsync.model.TagSink;
import org.apache.ranger.tagsync.process.TagSyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;

public class TagAdminRESTSink implements TagSink, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(TagAdminRESTSink.class);

	private static final String REST_PREFIX = "/service";
	private static final String MODULE_PREFIX = "/tags";

	private static final String REST_URL_IMPORT_SERVICETAGS_RESOURCE = REST_PREFIX + MODULE_PREFIX + "/importservicetags/";

	private long rangerAdminConnectionCheckInterval;

	private Cookie sessionId=null;

	private boolean isValidRangerCookie=false;

	List<NewCookie> cookieList=new ArrayList<>();

	private boolean isRangerCookieEnabled;
	private String rangerAdminCookieName;

	private RangerRESTClient tagRESTClient = null;

	private boolean isKerberized;

	private BlockingQueue<UploadWorkItem> uploadWorkItems;

	private Thread myThread = null;

	@Override
	public boolean initialize(Properties properties) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagAdminRESTSink.initialize()");
		}

		boolean ret = false;

		String restUrl = TagSyncConfig.getTagAdminRESTUrl(properties);
		String sslConfigFile = TagSyncConfig.getTagAdminRESTSslConfigFile(properties);
		String userName = TagSyncConfig.getTagAdminUserName(properties);
		String password = TagSyncConfig.getTagAdminPassword(properties);
		rangerAdminConnectionCheckInterval = TagSyncConfig.getTagAdminConnectionCheckInterval(properties);
		isKerberized = TagSyncConfig.getTagsyncKerberosIdentity(properties) != null;
		isRangerCookieEnabled = TagSyncConfig.isTagSyncRangerCookieEnabled(properties);
		rangerAdminCookieName=TagSyncConfig.getRangerAdminCookieName(properties);
		sessionId=null;

		if (LOG.isDebugEnabled()) {
			LOG.debug("restUrl=" + restUrl);
			LOG.debug("sslConfigFile=" + sslConfigFile);
			LOG.debug("userName=" + userName);
			LOG.debug("rangerAdminConnectionCheckInterval=" + rangerAdminConnectionCheckInterval);
			LOG.debug("isKerberized=" + isKerberized);
		}

		if (StringUtils.isNotBlank(restUrl)) {
			tagRESTClient = new RangerRESTClient(restUrl, sslConfigFile, TagSyncConfig.getInstance());
			if (!isKerberized) {
				tagRESTClient.setBasicAuthInfo(userName, password);
			}
			// Build and cache REST client. This will catch any errors in building REST client up-front
			tagRESTClient.getClient();

			uploadWorkItems = new LinkedBlockingQueue<UploadWorkItem>();
			ret = true;
		} else {
			LOG.error("No value specified for property 'ranger.tagsync.tagadmin.rest.url'!");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagAdminRESTSink.initialize(), result=" + ret);
		}

		return ret;
	}

	@Override
	public ServiceTags upload(ServiceTags toUpload) throws Exception {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> upload() ");
		}

		UploadWorkItem uploadWorkItem = new UploadWorkItem(toUpload);

		uploadWorkItems.put(uploadWorkItem);

		// Wait until message is successfully delivered
		ServiceTags ret = uploadWorkItem.waitForUpload();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== upload()");
		}

		return ret;
	}

	private ServiceTags doUpload(ServiceTags serviceTags) throws Exception {
			if(isKerberized) {
				try{
					UserGroupInformation userGroupInformation = UserGroupInformation.getLoginUser();
					if (userGroupInformation != null) {
						try {
							userGroupInformation.checkTGTAndReloginFromKeytab();
						} catch (IOException ioe) {
							LOG.error("Error renewing TGT and relogin", ioe);
							userGroupInformation = null;
						}
					}
					if (userGroupInformation != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("Using Principal = " + userGroupInformation.getUserName());
						}
						final ServiceTags serviceTag = serviceTags;
						ServiceTags ret = userGroupInformation.doAs(new PrivilegedAction<ServiceTags>() {
							@Override
							public ServiceTags run() {
								try {
									return uploadServiceTags(serviceTag);
								} catch (Exception e) {
									LOG.error("Upload of service-tags failed with message ", e);
								}
								return null;
							}
						});
						return ret;
					} else {
						LOG.error("Failed to get UserGroupInformation.getLoginUser()");
						return null; // This will cause retries !!!
					}
				}catch(Exception e){
					LOG.error("Upload of service-tags failed with message ", e);
				}
				return null;
			}else{
				return uploadServiceTags(serviceTags);
			}
	}
	
	private ServiceTags uploadServiceTags(ServiceTags serviceTags) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> doUpload()");
		}
		ClientResponse response = null;
		if (isRangerCookieEnabled) {
			response = uploadServiceTagsUsingCookie(serviceTags);
		} else {
			response = tagRESTClient.put(REST_URL_IMPORT_SERVICETAGS_RESOURCE, null, serviceTags);
		}

		if(response == null || response.getStatus() != HttpServletResponse.SC_NO_CONTENT) {

			RESTResponse resp = RESTResponse.fromClientResponse(response);

			LOG.error("Upload of service-tags failed with message " + resp.getMessage());

			if (response == null || resp.getHttpStatusCode() != HttpServletResponse.SC_BAD_REQUEST) {
				// NOT an application error
				throw new Exception("Upload of service-tags failed with response: " + response);
			}

		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== doUpload()");
		}

		return serviceTags;
	}

	private ClientResponse uploadServiceTagsUsingCookie(ServiceTags serviceTags) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> uploadServiceTagCache()");
		}
		ClientResponse clientResponse = null;
		if (sessionId != null && isValidRangerCookie) {
			clientResponse = tryWithCookie(serviceTags);

		} else {
			clientResponse = tryWithCred(serviceTags);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== uploadServiceTagCache()");
		}
		return clientResponse;
	}

	private ClientResponse tryWithCred(ServiceTags serviceTags) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> tryWithCred");
		}
		ClientResponse clientResponsebyCred = uploadTagsWithCred(serviceTags);
		if (clientResponsebyCred != null && clientResponsebyCred.getStatus() != HttpServletResponse.SC_NO_CONTENT
				&& clientResponsebyCred.getStatus() != HttpServletResponse.SC_BAD_REQUEST
				&& clientResponsebyCred.getStatus() != HttpServletResponse.SC_OK) {
			sessionId = null;
			clientResponsebyCred = null;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== tryWithCred");
		}
		return clientResponsebyCred;
	}

	private ClientResponse tryWithCookie(ServiceTags serviceTags) {
		ClientResponse clientResponsebySessionId = uploadTagsWithCookie(serviceTags);
		if (clientResponsebySessionId != null
				&& clientResponsebySessionId.getStatus() != HttpServletResponse.SC_NO_CONTENT
				&& clientResponsebySessionId.getStatus() != HttpServletResponse.SC_BAD_REQUEST
				&& clientResponsebySessionId.getStatus() != HttpServletResponse.SC_OK) {
			sessionId = null;
			isValidRangerCookie = false;
			clientResponsebySessionId = null;
		}
		return clientResponsebySessionId;
	}

	private synchronized ClientResponse uploadTagsWithCred(ServiceTags serviceTags) {
			if (sessionId == null) {
				tagRESTClient.resetClient();

				ClientResponse response = null;
				try {
					response = tagRESTClient.put(REST_URL_IMPORT_SERVICETAGS_RESOURCE, null, serviceTags);
				} catch (Exception e) {
					LOG.error("Failed to get response, Error is : "+e.getMessage());
				}
				if (response != null) {
					if (!(response.toString().contains(REST_URL_IMPORT_SERVICETAGS_RESOURCE))) {
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
					} else if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
						LOG.warn("Credentials response from ranger is 401.");
					} else if (response.getStatus() == HttpServletResponse.SC_OK
							|| response.getStatus() == HttpServletResponse.SC_NO_CONTENT) {
						cookieList = response.getCookies();
						// save cookie received from credentials session login
						for (NewCookie cookie : cookieList) {
							if (cookie.getName().equalsIgnoreCase(rangerAdminCookieName)) {
								sessionId = cookie.toCookie();
								isValidRangerCookie = true;
								break;
							} else {
								isValidRangerCookie = false;
							}
						}
					}
				}
				return response;
			} else {
				ClientResponse clientResponsebySessionId = uploadTagsWithCookie(serviceTags);

				if (!(clientResponsebySessionId.toString().contains(REST_URL_IMPORT_SERVICETAGS_RESOURCE))) {
					clientResponsebySessionId.setStatus(HttpServletResponse.SC_NOT_FOUND);
				}
				return clientResponsebySessionId;
			}
	}

	private ClientResponse uploadTagsWithCookie(ServiceTags serviceTags) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> uploadTagsWithCookie");
		}

		ClientResponse response = null;
		try {
			response = tagRESTClient.put(REST_URL_IMPORT_SERVICETAGS_RESOURCE, serviceTags, sessionId);
		} catch (Exception e) {
			LOG.error("Failed to get response, Error is : "+e.getMessage());
		}
		if (response != null) {
			if (!(response.toString().contains(REST_URL_IMPORT_SERVICETAGS_RESOURCE))) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				sessionId = null;
				isValidRangerCookie = false;
			} else if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
				sessionId = null;
				isValidRangerCookie = false;
			} else if (response.getStatus() == HttpServletResponse.SC_NO_CONTENT
					|| response.getStatus() == HttpServletResponse.SC_OK) {
				List<NewCookie> respCookieList = response.getCookies();
				for (NewCookie respCookie : respCookieList) {
					if (respCookie.getName().equalsIgnoreCase(rangerAdminCookieName)) {
						if (!(sessionId.getValue().equalsIgnoreCase(respCookie.toCookie().getValue()))) {
							sessionId = respCookie.toCookie();
						}
						isValidRangerCookie = true;
						break;
					}
				}
			}

		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== uploadTagsWithCookie");
		}
		return response;
	}

	@Override
	public boolean start() {

		myThread = new Thread(this);
		myThread.setDaemon(true);
		myThread.start();

		return true;
	}

	@Override
	public void stop() {
		if (myThread != null && myThread.isAlive()) {
			myThread.interrupt();
		}
	}

	@Override
	public void run() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagAdminRESTSink.run()");
		}

		while (true) {
			UploadWorkItem uploadWorkItem;

			try {
				uploadWorkItem = uploadWorkItems.take();

				ServiceTags toUpload = uploadWorkItem.getServiceTags();

				boolean doRetry;

				do {
					doRetry = false;

					try {
						ServiceTags uploaded = doUpload(toUpload);
						if (uploaded == null) { // Treat this as if an Exception is thrown by doUpload
							doRetry = true;
							Thread.sleep(rangerAdminConnectionCheckInterval);
						} else {
							// ServiceTags uploaded successfully
							uploadWorkItem.uploadCompleted(uploaded);
						}
					} catch (InterruptedException interrupted) {
						LOG.error("Caught exception..: ", interrupted);
						return;
					} catch (Exception exception) {
						doRetry = true;
						Thread.sleep(rangerAdminConnectionCheckInterval);
					}
				} while (doRetry);

			}
			catch (InterruptedException exception) {
				LOG.error("Interrupted..: ", exception);
				return;
			}
		}

	}

	static class UploadWorkItem {
		private ServiceTags serviceTags;
		private BlockingQueue<ServiceTags> uploadedServiceTags;

		ServiceTags getServiceTags() {
			return serviceTags;
		}

		ServiceTags waitForUpload() throws InterruptedException {
			return uploadedServiceTags.take();
		}

		void uploadCompleted(ServiceTags uploaded) throws InterruptedException {
			// ServiceTags uploaded successfully
			uploadedServiceTags.put(uploaded);
		}

		UploadWorkItem(ServiceTags serviceTags) {
			setServiceTags(serviceTags);
			uploadedServiceTags = new ArrayBlockingQueue<ServiceTags>(1);
		}

		void setServiceTags(ServiceTags serviceTags) {
			this.serviceTags = serviceTags;
		}

	}

}
