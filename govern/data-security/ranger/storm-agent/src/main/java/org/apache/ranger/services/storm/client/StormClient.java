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

package org.apache.ranger.services.storm.client;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.KrbPasswordSaverLoginModule;
import org.apache.hadoop.security.SecureClientLogin;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.apache.ranger.services.storm.client.json.model.Topology;
import org.apache.ranger.services.storm.client.json.model.TopologyListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class StormClient {

	private static final Logger LOG = LoggerFactory.getLogger(StormClient.class);

	private static final String EXPECTED_MIME_TYPE = "application/json";

	private static final String TOPOLOGY_LIST_API_ENDPOINT = "/api/v1/topology/summary";

	private static final String errMessage =  " You can still save the repository and start creating "
											  + "policies, but you would not be able to use autocomplete for "
											  + "resource names. Check ranger_admin.log for more info.";

	String stormUIUrl;
	String userName;
	String password;
	String lookupPrincipal;
	String lookupKeytab;
	String nameRules;

	public StormClient(String aStormUIUrl, String aUserName, String aPassword, String lookupPrincipal, String lookupKeytab, String nameRules) {

		this.stormUIUrl = aStormUIUrl;
		this.userName = aUserName;
		this.password = aPassword;
		this.lookupPrincipal = lookupPrincipal;
		this.lookupKeytab = lookupKeytab;
		this.nameRules = nameRules;

		if (LOG.isDebugEnabled()) {
			LOG.debug("Storm Client is build with url [" + aStormUIUrl + "] user: [" + aUserName + "], password: [" + "" + "]");
		}

	}

	public List<String> getTopologyList(final String topologyNameMatching, final List<String> stormTopologyList) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Getting Storm topology list for topologyNameMatching : " + topologyNameMatching);
		}

		PrivilegedAction<ArrayList<String>> topologyListGetter = new PrivilegedAction<ArrayList<String>>() {
			@Override
			public ArrayList<String> run() {
				if (stormUIUrl == null || stormUIUrl.trim().isEmpty()) {
					return null;
				}

				String[] stormUIUrls = stormUIUrl.trim().split("[,;]");
				if (stormUIUrls == null || stormUIUrls.length == 0) {
					return null;
				}

				Client client = Client.create();
				ClientResponse response = null;
				for (String currentUrl : stormUIUrls) {
					if (currentUrl == null || currentUrl.trim().isEmpty()) {
						continue;
					}

					String url = currentUrl.trim() + TOPOLOGY_LIST_API_ENDPOINT;
					try {
						response = getTopologyResponse(url, client);

						if (response != null) {
							if (response.getStatus() == 200) {
								break;
							} else {
								response.close();
							}
						}
					} catch (Throwable t) {
						String msgDesc = "Exception while getting topology list." + " URL : " + url;
						LOG.error(msgDesc, t);
					}
				}

				ArrayList<String> lret = new ArrayList<String>();
				try {
					if (response != null) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("getTopologyList():response.getStatus()= " + response.getStatus());
						}
						if (response.getStatus() == 200) {
							String jsonString = response.getEntity(String.class);
							Gson gson = new GsonBuilder().setPrettyPrinting().create();
							TopologyListResponse topologyListResponse = gson.fromJson(jsonString, TopologyListResponse.class);
							if (topologyListResponse != null) {
								if (topologyListResponse.getTopologyList() != null) {
									for (Topology topology : topologyListResponse.getTopologyList()) {
										String topologyName = topology.getName();
										if (stormTopologyList != null && stormTopologyList.contains(topologyName)) {
											continue;
										}
										if (LOG.isDebugEnabled()) {
											LOG.debug("getTopologyList():Found topology " + topologyName);
											LOG.debug("getTopologyList():topology Name=[" + topology.getName()
													+ "], topologyNameMatching=[" + topologyNameMatching
													+ "], existingStormTopologyList=[" + stormTopologyList + "]");
										}
										if (topologyName != null) {
											if (topologyNameMatching == null || topologyNameMatching.isEmpty() || FilenameUtils.wildcardMatch(topology.getName(), topologyNameMatching + "*")) {
												if (LOG.isDebugEnabled()) {
													LOG.debug("getTopologyList():Adding topology " + topologyName);
												}
												lret.add(topologyName);
											}
										}
									}
								}
							}
						}
					} else {
						String msgDesc = "Unable to get a valid response for " + "expected mime type : ["
								+ EXPECTED_MIME_TYPE + "] URL : " + stormUIUrl + " - got null response.";
						LOG.error(msgDesc);
						HadoopException hdpException = new HadoopException(msgDesc);
						hdpException.generateResponseDataMap(false, msgDesc, msgDesc + errMessage, null, null);
						throw hdpException;
					}
				} catch (HadoopException he) {
					throw he;
				} catch (Throwable t) {
					String msgDesc = "Exception while getting Storm TopologyList." + " URL : " + stormUIUrl;
					HadoopException hdpException = new HadoopException(msgDesc, t);
					LOG.error(msgDesc, t);

					hdpException.generateResponseDataMap(false, BaseClient.getMessage(t), msgDesc + errMessage, null, null);
					throw hdpException;
				} finally {
					if (response != null) {
						response.close();
					}

					if (client != null) {
						client.destroy();
					}
				}
				return lret;
			}

			private ClientResponse getTopologyResponse(String url, Client client) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("getTopologyResponse():calling " + url);
				}

				WebResource webResource = client.resource(url);

				ClientResponse response = webResource.accept(EXPECTED_MIME_TYPE).get(ClientResponse.class);

				if (response != null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("getTopologyResponse():response.getStatus()= " + response.getStatus());
					}
					if (response.getStatus() != 200) {
						LOG.info("getTopologyResponse():response.getStatus()= " + response.getStatus() + " for URL "
								+ url + ", failed to get topology list");
						String jsonString = response.getEntity(String.class);
						LOG.info(jsonString);
					}
				}
				return response;
			}
		};

		List<String> ret = null;
		try {
			ret = executeUnderKerberos(this.userName, this.password, this.lookupPrincipal, this.lookupKeytab, this.nameRules, topologyListGetter);
		} catch (IOException e) {
			LOG.error("Unable to get Topology list from [" + stormUIUrl + "]", e);
		}

		return ret;
	}

	public static <T> T executeUnderKerberos(String userName, String password, String lookupPrincipal, String lookupKeytab, String nameRules,
			PrivilegedAction<T> action) throws IOException {

		T ret = null;

		Subject subject = null;
		LoginContext loginContext = null;

		try {
			Subject loginSubj = null;
			if(!StringUtils.isEmpty(lookupPrincipal) && !StringUtils.isEmpty(lookupKeytab)){
				LOG.info("Init Lookup Login: security enabled, using lookupPrincipal/lookupKeytab");
				if(StringUtils.isEmpty(nameRules)){
					nameRules = "DEFAULT";
				}
				loginSubj = SecureClientLogin.loginUserFromKeytab(lookupPrincipal, lookupKeytab, nameRules);
			}else{
			    subject = new Subject();
				LOG.debug("executeUnderKerberos():user=" + userName + ",pass=");
				LOG.debug("executeUnderKerberos():Creating config..");
				MySecureClientLoginConfiguration loginConf = new MySecureClientLoginConfiguration(
						userName, password);
				LOG.debug("executeUnderKerberos():Creating Context..");
				loginContext = new LoginContext("hadoop-keytab-kerberos", subject,
						null, loginConf);

				LOG.debug("executeUnderKerberos():Logging in..");
				loginContext.login();
				LOG.info("Init Login: using username/password");
				loginSubj = loginContext.getSubject();
			}
			if (loginSubj != null) {
				ret = Subject.doAs(loginSubj, action);
			}
		} catch (LoginException le) {
			String msgDesc = "executeUnderKerberos: Login failure using given"
					+ " configuration parameters, username : `" + userName + "`.";
			HadoopException hdpException = new HadoopException(msgDesc, le);
			LOG.error(msgDesc, le);

			hdpException.generateResponseDataMap(false,
					BaseClient.getMessage(le), msgDesc + errMessage, null, null);
			throw hdpException;
		} catch (SecurityException se) {
			String msgDesc = "executeUnderKerberos: Exception while getting Storm TopologyList.";
			HadoopException hdpException = new HadoopException(msgDesc, se);
			LOG.error(msgDesc, se);

			hdpException.generateResponseDataMap(false,
					BaseClient.getMessage(se), msgDesc + errMessage, null, null);
			throw hdpException;

		} finally {
			if (loginContext != null) {
				if (subject != null) {
					try {
						loginContext.logout();
					} catch (LoginException e) {
						throw new IOException("logout failure", e);
					}
				}
			}
		}

		return ret;
	}

	public static Map<String, Object> connectionTest(String serviceName,
			Map<String, String> configs) {

		String errMsg = errMessage;
		boolean connectivityStatus = false;
		Map<String, Object> responseData = new HashMap<String, Object>();

		StormClient stormClient = getStormClient(serviceName,
				configs);
		List<String> strList = getStormResources(stormClient, "",null);

		if (strList != null) {
			connectivityStatus = true;
		}

		if (connectivityStatus) {
			String successMsg = "ConnectionTest Successful";
			BaseClient.generateResponseDataMap(connectivityStatus, successMsg,
					successMsg, null, null, responseData);
		} else {
			String failureMsg = "Unable to retrieve any topologies using given parameters.";
			BaseClient.generateResponseDataMap(connectivityStatus, failureMsg,
					failureMsg + errMsg, null, null, responseData);
		}

		return responseData;
	}

	public static StormClient getStormClient(String serviceName,
			Map<String, String> configs) {
		StormClient stormClient = null;
		if(LOG.isDebugEnabled()){
			LOG.debug("Getting StormClient for datasource: " + serviceName);
			LOG.debug("configMap: " + configs);
		}
		String errMsg = errMessage;
		if (configs == null || configs.isEmpty()) {
			String msgDesc = "Could not connect as Connection ConfigMap is empty.";
			LOG.error(msgDesc);
			HadoopException hdpException = new HadoopException(msgDesc);
			hdpException.generateResponseDataMap(false, msgDesc, msgDesc
					+ errMsg, null, null);
			throw hdpException;
		} else {
			String stormUrl = configs.get("nimbus.url");
			String stormAdminUser = configs.get("username");
			String stormAdminPassword = configs.get("password");
			String lookupPrincipal = configs.get("lookupprincipal");
			String lookupKeytab = configs.get("lookupkeytab");
			String nameRules = configs.get("namerules");
			stormClient = new StormClient(stormUrl, stormAdminUser,
					stormAdminPassword, lookupPrincipal, lookupKeytab, nameRules);
		}
		return stormClient;
	}

	public static List<String> getStormResources(final StormClient stormClient,
			String topologyName, List<String> stormTopologyList) {

		List<String> resultList = new ArrayList<String>();
		String errMsg = errMessage;

		try {
			if (stormClient == null) {
				String msgDesc = "Unable to get Storm resources: StormClient is null.";
				LOG.error(msgDesc);
				HadoopException hdpException = new HadoopException(msgDesc);
				hdpException.generateResponseDataMap(false, msgDesc, msgDesc
						+ errMsg, null, null);
				throw hdpException;
			}

			if (topologyName != null) {
				String finalTopologyNameMatching = topologyName.trim();
				resultList = stormClient
						.getTopologyList(finalTopologyNameMatching,stormTopologyList);
				if (resultList != null) {
					LOG.debug("Returning list of " + resultList.size() + " topologies");
				}
			}
		} catch (HadoopException he) {
			throw he;
		} catch (Exception e) {
			String msgDesc = "getStormResources: Unable to get Storm resources.";
			LOG.error(msgDesc, e);
			HadoopException hdpException = new HadoopException(msgDesc);

			hdpException.generateResponseDataMap(false,
					BaseClient.getMessage(e), msgDesc + errMsg, null, null);
			throw hdpException;
		}
		return resultList;
	}

	private static class MySecureClientLoginConfiguration extends javax.security.auth.login.Configuration {

	    private String userName;
	    private String password;

	    MySecureClientLoginConfiguration(String aUserName, String password) {
	        this.userName = aUserName;
	        String decryptedPwd = null;
	        try {
	            decryptedPwd = PasswordUtils.decryptPassword(password);
	        } catch(Exception ex) {
	            LOG.info("Password decryption failed; trying Storm connection with received password string");
	            decryptedPwd = null;
	        } finally {
	            if (decryptedPwd == null) {
	                decryptedPwd = password;
	            }
	        }
	        this.password = decryptedPwd;
	    }

	    @Override
	    public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {

	        Map<String, String> kerberosOptions = new HashMap<String, String>();
	        kerberosOptions.put("principal", this.userName);
	        kerberosOptions.put("debug", "true");
	        kerberosOptions.put("useKeyTab", "false");
	        kerberosOptions.put(KrbPasswordSaverLoginModule.USERNAME_PARAM, this.userName);
	        kerberosOptions.put(KrbPasswordSaverLoginModule.PASSWORD_PARAM, this.password);
	        kerberosOptions.put("doNotPrompt", "false");
	        kerberosOptions.put("useFirstPass", "true");
	        kerberosOptions.put("tryFirstPass", "false");
	        kerberosOptions.put("storeKey", "true");
	        kerberosOptions.put("refreshKrb5Config", "true");

	        AppConfigurationEntry KEYTAB_KERBEROS_LOGIN = null;
	        AppConfigurationEntry KERBEROS_PWD_SAVER = null;
	        try {
	            KEYTAB_KERBEROS_LOGIN = new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
	                                                              AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
	                                                              kerberosOptions);
	            KERBEROS_PWD_SAVER = new AppConfigurationEntry(KrbPasswordSaverLoginModule.class.getName(),
	                                                           LoginModuleControlFlag.REQUIRED, kerberosOptions);

	        } catch (IllegalArgumentException e) {
	            String msgDesc = "executeUnderKerberos: Exception while getting Storm TopologyList.";
	            HadoopException hdpException = new HadoopException(msgDesc, e);
	            LOG.error(msgDesc, e);

	            hdpException.generateResponseDataMap(false,
	                                                 BaseClient.getMessage(e), msgDesc + errMessage, null,
	                                                 null);
	            throw hdpException;
	        }

	        LOG.debug("getAppConfigurationEntry():" + kerberosOptions.get("principal"));

	        return new AppConfigurationEntry[] { KERBEROS_PWD_SAVER, KEYTAB_KERBEROS_LOGIN };
	    }

	};
}

