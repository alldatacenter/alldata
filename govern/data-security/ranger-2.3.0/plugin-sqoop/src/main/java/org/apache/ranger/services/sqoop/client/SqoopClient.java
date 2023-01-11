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

package org.apache.ranger.services.sqoop.client;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.authentication.client.PseudoAuthenticator;
import org.apache.http.HttpStatus;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.services.sqoop.client.json.model.SqoopConnectorResponse;
import org.apache.ranger.services.sqoop.client.json.model.SqoopConnectorsResponse;
import org.apache.ranger.services.sqoop.client.json.model.SqoopJobResponse;
import org.apache.ranger.services.sqoop.client.json.model.SqoopJobsResponse;
import org.apache.ranger.services.sqoop.client.json.model.SqoopLinkResponse;
import org.apache.ranger.services.sqoop.client.json.model.SqoopLinksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class SqoopClient extends BaseClient {

	private static final Logger LOG = LoggerFactory.getLogger(SqoopClient.class);

	private static final String EXPECTED_MIME_TYPE = "application/json";

	private static final String SQOOP_CONNECTOR_API_ENDPOINT = "/sqoop/v1/connector/all";

	private static final String SQOOP_LINK_API_ENDPOINT = "/sqoop/v1/link/all";

	private static final String SQOOP_JOB_API_ENDPOINT = "/sqoop/v1/job/all";

	private static final String ERROR_MESSAGE = " You can still save the repository and start creating "
			+ "policies, but you would not be able to use autocomplete for "
			+ "resource names. Check ranger_admin.log for more info.";

	private String sqoopUrl;
	private String userName;

	public SqoopClient(String serviceName, Map<String, String> configs) {

		super(serviceName, configs, "sqoop-client");
		this.sqoopUrl = configs.get("sqoop.url");
		this.userName = configs.get("username");

		if (StringUtils.isEmpty(this.sqoopUrl)) {
			LOG.error("No value found for configuration 'sqoop.url'. Sqoop resource lookup will fail.");
		}
		if (StringUtils.isEmpty(this.userName)) {
			LOG.error("No value found for configuration 'username'. Sqoop resource lookup will fail.");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Sqoop Client is build with url [" + this.sqoopUrl + "], user: [" + this.userName + "].");
		}
	}

	public List<String> getConnectorList(final String connectorMatching, final List<String> existingConnectors) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Get sqoop connector list for connectorMatching: " + connectorMatching + ", existingConnectors: "
					+ existingConnectors);
		}
		Subject subj = getLoginSubject();
		if (subj == null) {
			return Collections.emptyList();
		}

		List<String> ret = Subject.doAs(subj, new PrivilegedAction<List<String>>() {

			@Override
			public List<String> run() {

				ClientResponse response = getClientResponse(sqoopUrl, SQOOP_CONNECTOR_API_ENDPOINT, userName);

				SqoopConnectorsResponse sqoopConnectorsResponse = getSqoopResourceResponse(response,
						SqoopConnectorsResponse.class);
				if (sqoopConnectorsResponse == null || CollectionUtils.isEmpty(sqoopConnectorsResponse.getConnectors())) {
					return Collections.emptyList();
				}
				List<String> connectorResponses = new ArrayList<>();
				for (SqoopConnectorResponse sqoopConnectorResponse : sqoopConnectorsResponse.getConnectors()) {
					connectorResponses.add(sqoopConnectorResponse.getName());
				}

				List<String> connectors = null;
				if (CollectionUtils.isNotEmpty(connectorResponses)) {
					connectors = filterResourceFromResponse(connectorMatching, existingConnectors, connectorResponses);
				}
				return connectors;
			}
		});

		if (LOG.isDebugEnabled()) {
			LOG.debug("Get sqoop connector list result: " + ret);
		}
		return ret;
	}

	public List<String> getLinkList(final String linkMatching, final List<String> existingLinks) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Get sqoop link list for linkMatching: " + linkMatching + ", existingLinks: " + existingLinks);
		}
		Subject subj = getLoginSubject();
		if (subj == null) {
			return Collections.emptyList();
		}

		List<String> ret = Subject.doAs(subj, new PrivilegedAction<List<String>>() {

			@Override
			public List<String> run() {

				ClientResponse response = getClientResponse(sqoopUrl, SQOOP_LINK_API_ENDPOINT, userName);

				SqoopLinksResponse sqoopLinksResponse = getSqoopResourceResponse(response, SqoopLinksResponse.class);
				if (sqoopLinksResponse == null || CollectionUtils.isEmpty(sqoopLinksResponse.getLinks())) {
					return Collections.emptyList();
				}
				List<String> linkResponses = new ArrayList<>();
				for (SqoopLinkResponse sqoopLinkResponse : sqoopLinksResponse.getLinks()) {
					linkResponses.add(sqoopLinkResponse.getName());
				}

				List<String> links = null;
				if (CollectionUtils.isNotEmpty(linkResponses)) {
					links = filterResourceFromResponse(linkMatching, existingLinks, linkResponses);
				}
				return links;
			}
		});

		if (LOG.isDebugEnabled()) {
			LOG.debug("Get sqoop link list result: " + ret);
		}
		return ret;
	}

	public List<String> getJobList(final String jobMatching, final List<String> existingJobs) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Get sqoop job list for jobMatching: " + jobMatching + ", existingJobs: " + existingJobs);
		}
		Subject subj = getLoginSubject();
		if (subj == null) {
			return Collections.emptyList();
		}

		List<String> ret = Subject.doAs(subj, new PrivilegedAction<List<String>>() {

			@Override
			public List<String> run() {

				ClientResponse response = getClientResponse(sqoopUrl, SQOOP_JOB_API_ENDPOINT, userName);

				SqoopJobsResponse sqoopJobsResponse = getSqoopResourceResponse(response, SqoopJobsResponse.class);
				if (sqoopJobsResponse == null || CollectionUtils.isEmpty(sqoopJobsResponse.getJobs())) {
					return Collections.emptyList();
				}
				List<String> jobResponses = new ArrayList<>();
				for (SqoopJobResponse sqoopJobResponse : sqoopJobsResponse.getJobs()) {
					jobResponses.add(sqoopJobResponse.getName());
				}

				List<String> jobs = null;
				if (CollectionUtils.isNotEmpty(jobResponses)) {
					jobs = filterResourceFromResponse(jobMatching, existingJobs, jobResponses);
				}
				return jobs;
			}
		});

		if (LOG.isDebugEnabled()) {
			LOG.debug("Get sqoop job list result: " + ret);
		}
		return ret;
	}

	private static ClientResponse getClientResponse(String sqoopUrl, String sqoopApi, String userName) {
		ClientResponse response = null;
		String[] sqoopUrls = sqoopUrl.trim().split("[,;]");
		if (ArrayUtils.isEmpty(sqoopUrls)) {
			return null;
		}

		Client client = Client.create();

		for (String currentUrl : sqoopUrls) {
			if (StringUtils.isBlank(currentUrl)) {
				continue;
			}

			String url = currentUrl.trim() + sqoopApi + "?" + PseudoAuthenticator.USER_NAME + "=" + userName;
			try {
				response = getClientResponse(url, client);

				if (response != null) {
					if (response.getStatus() == HttpStatus.SC_OK) {
						break;
					} else {
						response.close();
					}
				}
			} catch (Throwable t) {
				String msgDesc = "Exception while getting sqoop response, sqoopUrl: " + url;
				LOG.error(msgDesc, t);
			}
		}
		client.destroy();

		return response;
	}

	private static ClientResponse getClientResponse(String url, Client client) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getClientResponse():calling " + url);
		}

		WebResource webResource = client.resource(url);

		ClientResponse response = webResource.accept(EXPECTED_MIME_TYPE).get(ClientResponse.class);

		if (response != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getClientResponse():response.getStatus()= " + response.getStatus());
			}
			if (response.getStatus() != HttpStatus.SC_OK) {
				LOG.warn("getClientResponse():response.getStatus()= " + response.getStatus() + " for URL " + url
						+ ", failed to get sqoop resource list.");
				String jsonString = response.getEntity(String.class);
				LOG.warn(jsonString);
			}
		}
		return response;
	}

	private <T> T getSqoopResourceResponse(ClientResponse response, Class<T> classOfT) {
		T resource = null;
		try {
            if (response != null) {
                if (response.getStatus() == HttpStatus.SC_OK) {
                    String jsonString = response.getEntity(String.class);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();

                    resource = gson.fromJson(jsonString, classOfT);
                } else {
                    String msgDesc = "Unable to get a valid response for " + "expected mime type : ["
                            + EXPECTED_MIME_TYPE + "], sqoopUrl: " + sqoopUrl + " - got http response code "
                            + response.getStatus();
                    LOG.error(msgDesc);
                    HadoopException hdpException = new HadoopException(msgDesc);
                    hdpException.generateResponseDataMap(false, msgDesc, msgDesc + ERROR_MESSAGE, null, null);
                    throw hdpException;
                }

            } else {
                String msgDesc = "Unable to get a valid response for " + "expected mime type : [" + EXPECTED_MIME_TYPE
                        + "], sqoopUrl: " + sqoopUrl + " - got null response.";
                LOG.error(msgDesc);
                HadoopException hdpException = new HadoopException(msgDesc);
                hdpException.generateResponseDataMap(false, msgDesc, msgDesc + ERROR_MESSAGE, null, null);
                throw hdpException;
            }
		} catch (HadoopException he) {
			throw he;
		} catch (Throwable t) {
			String msgDesc = "Exception while getting sqoop resource response, sqoopUrl: " + sqoopUrl;
			HadoopException hdpException = new HadoopException(msgDesc, t);

			LOG.error(msgDesc, t);

			hdpException.generateResponseDataMap(false, BaseClient.getMessage(t), msgDesc + ERROR_MESSAGE, null, null);
			throw hdpException;

		} finally {
			if (response != null) {
				response.close();
			}
		}
		return resource;
	}

	private static List<String> filterResourceFromResponse(String resourceMatching, List<String> existingResources,
			List<String> resourceResponses) {
		List<String> resources = new ArrayList<String>();
		for (String resourceResponse : resourceResponses) {
			if (CollectionUtils.isNotEmpty(existingResources) && existingResources.contains(resourceResponse)) {
				continue;
			}
			if (StringUtils.isEmpty(resourceMatching) || resourceMatching.startsWith("*")
					|| resourceResponse.toLowerCase().startsWith(resourceMatching.toLowerCase())) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("filterResourceFromResponse(): Adding sqoop resource " + resourceResponse);
				}
				resources.add(resourceResponse);
			}
		}
		return resources;
	}

	public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) {
		SqoopClient sqoopClient = getSqoopClient(serviceName, configs);
		List<String> strList = sqoopClient.getConnectorList(null, null);

		boolean connectivityStatus = false;
		if (CollectionUtils.isNotEmpty(strList)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("ConnectionTest list size " + strList.size() + " sqoop connectors.");
			}
			connectivityStatus = true;
		}

		Map<String, Object> responseData = new HashMap<String, Object>();
		if (connectivityStatus) {
			String successMsg = "ConnectionTest Successful.";
			BaseClient.generateResponseDataMap(connectivityStatus, successMsg, successMsg, null, null, responseData);
		} else {
			String failureMsg = "Unable to retrieve any sqoop connectors using given parameters.";
			BaseClient.generateResponseDataMap(connectivityStatus, failureMsg, failureMsg + ERROR_MESSAGE, null, null,
					responseData);
		}

		return responseData;
	}

	public static SqoopClient getSqoopClient(String serviceName, Map<String, String> configs) {
		SqoopClient sqoopClient = null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting SqoopClient for datasource: " + serviceName);
		}
		if (MapUtils.isEmpty(configs)) {
			String msgDesc = "Could not connect sqoop as Connection ConfigMap is empty.";
			LOG.error(msgDesc);
			HadoopException hdpException = new HadoopException(msgDesc);
			hdpException.generateResponseDataMap(false, msgDesc, msgDesc + ERROR_MESSAGE, null, null);
			throw hdpException;
		} else {
			sqoopClient = new SqoopClient(serviceName, configs);
		}
		return sqoopClient;
	}
}
