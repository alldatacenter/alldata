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

package org.apache.ranger.services.kylin.client;

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
import org.apache.http.HttpStatus;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.apache.ranger.services.kylin.client.json.model.KylinProjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class KylinClient extends BaseClient {

	private static final Logger LOG = LoggerFactory.getLogger(KylinClient.class);

	private static final String EXPECTED_MIME_TYPE = "application/json";

	private static final String KYLIN_LIST_API_ENDPOINT = "/kylin/api/projects";

	private static final String ERROR_MESSAGE = " You can still save the repository and start creating "
			+ "policies, but you would not be able to use autocomplete for "
			+ "resource names. Check ranger_admin.log for more info.";

	private String kylinUrl;
	private String userName;
	private String password;

	public KylinClient(String serviceName, Map<String, String> configs) {

		super(serviceName, configs, "kylin-client");

		this.kylinUrl = configs.get("kylin.url");
		this.userName = configs.get("username");
		this.password = configs.get("password");

		if (StringUtils.isEmpty(this.kylinUrl)) {
			LOG.error("No value found for configuration 'kylin.url'. Kylin resource lookup will fail.");
		}
		if (StringUtils.isEmpty(this.userName)) {
			LOG.error("No value found for configuration 'username'. Kylin resource lookup will fail.");
		}
		if (StringUtils.isEmpty(this.password)) {
			LOG.error("No value found for configuration 'password'. Kylin resource lookup will fail.");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Kylin client is build with url [" + this.kylinUrl + "], user: [" + this.userName
					+ "], password: [" + "*********" + "].");
		}
	}

	public List<String> getProjectList(final String projectMatching, final List<String> existingProjects) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting kylin project list for projectMatching: " + projectMatching + ", existingProjects: "
					+ existingProjects);
		}
		Subject subj = getLoginSubject();
		if (subj == null) {
			return Collections.emptyList();
		}

		List<String> ret = Subject.doAs(subj, new PrivilegedAction<List<String>>() {

			@Override
			public List<String> run() {

				ClientResponse response = getClientResponse(kylinUrl, userName, password);

				List<KylinProjectResponse> projectResponses = getKylinProjectResponse(response);

				if (CollectionUtils.isEmpty(projectResponses)) {
					return Collections.emptyList();
				}

				List<String> projects = getProjectFromResponse(projectMatching, existingProjects, projectResponses);

				return projects;
			}
		});

		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting kylin project list result: " + ret);
		}
		return ret;
	}

	private static ClientResponse getClientResponse(String kylinUrl, String userName, String password) {
		ClientResponse response = null;
		String[] kylinUrls = kylinUrl.trim().split("[,;]");
		if (ArrayUtils.isEmpty(kylinUrls)) {
			return null;
		}

		Client client = Client.create();
		String decryptedPwd = PasswordUtils.getDecryptPassword(password);
		client.addFilter(new HTTPBasicAuthFilter(userName, decryptedPwd));

		for (String currentUrl : kylinUrls) {
			if (StringUtils.isBlank(currentUrl)) {
				continue;
			}

			String url = currentUrl.trim() + KYLIN_LIST_API_ENDPOINT;
			try {
				response = getProjectResponse(url, client);

				if (response != null) {
					if (response.getStatus() == HttpStatus.SC_OK) {
						break;
					} else {
						response.close();
					}
				}
			} catch (Throwable t) {
				String msgDesc = "Exception while getting kylin response, kylinUrl: " + url;
				LOG.error(msgDesc, t);
			}
		}
		client.destroy();

		return response;
	}

	private List<KylinProjectResponse> getKylinProjectResponse(ClientResponse response) {
		List<KylinProjectResponse> projectResponses = null;
		try {
			if (response != null) {
				if (response.getStatus() == HttpStatus.SC_OK) {
					String jsonString = response.getEntity(String.class);
					Gson gson = new GsonBuilder().setPrettyPrinting().create();

					projectResponses = gson.fromJson(jsonString, new TypeToken<List<KylinProjectResponse>>() {
					}.getType());
				} else {
					String msgDesc = "Unable to get a valid response for " + "expected mime type : [" + EXPECTED_MIME_TYPE
							+ "], kylinUrl: " + kylinUrl + " - got http response code " + response.getStatus();
					LOG.error(msgDesc);
					HadoopException hdpException = new HadoopException(msgDesc);
					hdpException.generateResponseDataMap(false, msgDesc, msgDesc + ERROR_MESSAGE, null, null);
					throw hdpException;
				}
			} else {
				String msgDesc = "Unable to get a valid response for " + "expected mime type : [" + EXPECTED_MIME_TYPE
						+ "], kylinUrl: " + kylinUrl + " - got null response.";
				LOG.error(msgDesc);
				HadoopException hdpException = new HadoopException(msgDesc);
				hdpException.generateResponseDataMap(false, msgDesc, msgDesc + ERROR_MESSAGE, null, null);
				throw hdpException;
			}
		} catch (HadoopException he) {
			throw he;
		} catch (Throwable t) {
			String msgDesc = "Exception while getting kylin project response, kylinUrl: " + kylinUrl;
			HadoopException hdpException = new HadoopException(msgDesc, t);

			LOG.error(msgDesc, t);

			hdpException.generateResponseDataMap(false, BaseClient.getMessage(t), msgDesc + ERROR_MESSAGE, null, null);
			throw hdpException;

		} finally {
			if (response != null) {
				response.close();
			}
		}
		return projectResponses;
	}

	private static ClientResponse getProjectResponse(String url, Client client) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("getProjectResponse():calling " + url);
		}

		WebResource webResource = client.resource(url);

		ClientResponse response = webResource.accept(EXPECTED_MIME_TYPE).get(ClientResponse.class);

		if (response != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getProjectResponse():response.getStatus()= " + response.getStatus());
			}
			if (response.getStatus() != HttpStatus.SC_OK) {
				LOG.warn("getProjectResponse():response.getStatus()= " + response.getStatus() + " for URL " + url
						+ ", failed to get kylin project list.");
				String jsonString = response.getEntity(String.class);
				LOG.warn(jsonString);
			}
		}
		return response;
	}

	private static List<String> getProjectFromResponse(String projectMatching, List<String> existingProjects,
			List<KylinProjectResponse> projectResponses) {
		List<String> projcetNames = new ArrayList<String>();
		for (KylinProjectResponse project : projectResponses) {
			String projectName = project.getName();
			if (CollectionUtils.isNotEmpty(existingProjects) && existingProjects.contains(projectName)) {
				continue;
			}
			if (StringUtils.isEmpty(projectMatching) || projectMatching.startsWith("*")
					|| projectName.toLowerCase().startsWith(projectMatching.toLowerCase())) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("getProjectFromResponse(): Adding kylin project " + projectName);
				}
				projcetNames.add(projectName);
			}
		}
		return projcetNames;
	}

	public static Map<String, Object> connectionTest(String serviceName, Map<String, String> configs) {
		KylinClient kylinClient = getKylinClient(serviceName, configs);
		List<String> strList = kylinClient.getProjectList(null, null);

		boolean connectivityStatus = false;
		if (CollectionUtils.isNotEmpty(strList)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("ConnectionTest list size" + strList.size() + " kylin projects");
			}
			connectivityStatus = true;
		}

		Map<String, Object> responseData = new HashMap<String, Object>();
		if (connectivityStatus) {
			String successMsg = "ConnectionTest Successful";
			BaseClient.generateResponseDataMap(connectivityStatus, successMsg, successMsg, null, null, responseData);
		} else {
			String failureMsg = "Unable to retrieve any kylin projects using given parameters.";
			BaseClient.generateResponseDataMap(connectivityStatus, failureMsg, failureMsg + ERROR_MESSAGE, null, null,
					responseData);
		}

		return responseData;
	}

	public static KylinClient getKylinClient(String serviceName, Map<String, String> configs) {
		KylinClient kylinClient = null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting KylinClient for datasource: " + serviceName);
		}
		if (MapUtils.isEmpty(configs)) {
			String msgDesc = "Could not connect kylin as connection configMap is empty.";
			LOG.error(msgDesc);
			HadoopException hdpException = new HadoopException(msgDesc);
			hdpException.generateResponseDataMap(false, msgDesc, msgDesc + ERROR_MESSAGE, null, null);
			throw hdpException;
		} else {
			kylinClient = new KylinClient(serviceName, configs);
		}
		return kylinClient;
	}
}
