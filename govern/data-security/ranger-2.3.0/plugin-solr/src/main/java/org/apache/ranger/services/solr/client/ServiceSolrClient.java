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

package org.apache.ranger.services.solr.client;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecureClientLogin;
import org.apache.ranger.plugin.client.BaseClient;
import org.apache.ranger.plugin.client.HadoopConfigHolder;
import org.apache.ranger.plugin.client.HadoopException;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.util.PasswordUtils;
import org.apache.ranger.plugin.util.TimedEventUtil;
import org.apache.ranger.services.solr.RangerSolrConstants;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ConfigSetAdminRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceSolrClient {
	private static final Logger LOG = LoggerFactory.getLogger(ServiceSolrClient.class);

	private String username;
	private String password;
	private String serviceName;
	private SolrClient solrClient = null;
	private boolean isSolrCloud = true;
	private boolean isKerberosAuth;
	private String url;
	private Subject loginSubject;
	private String authType;

	public ServiceSolrClient(String serviceName, Map<String, String> configs, String url, boolean isSolrCloud) {
		this.username = configs.get("username");
		this.password = configs.get("password");
		this.authType = configs.get(HadoopConfigHolder.RANGER_AUTH_TYPE);
		this.isKerberosAuth = HadoopConfigHolder.HADOOP_SECURITY_AUTHENTICATION_METHOD.equalsIgnoreCase(this.authType);
		this.url = url;
		this.serviceName = serviceName;
		this.isSolrCloud = isSolrCloud;
		this.createSolrClientInstance();
		this.login(configs);
	}

	public Map<String, Object> connectionTest() throws Exception {
		Map<String, Object> responseData = new HashMap<String, Object>();

		if(this.isKerberosAuth) {
			Subject.doAs(this.loginSubject, new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					testConnection(responseData);
					return null;
				}
			});
		} else {
			testConnection(responseData);
		}
		return responseData;
	}

	private void testConnection(Map<String, Object> responseData) {
		String successMsg = "ConnectionTest Successful";
		try {
			getCollectionList(null);
			// If it doesn't throw exception, then assume the instance is
			// reachable
			BaseClient.generateResponseDataMap(true, successMsg, successMsg, null, null, responseData);
		} catch (Exception e) {
			LOG.error("Error connecting to Solr. solrClient=" + solrClient, e);
			String failureMsg = "Unable to connect to Solr instance." + e.getMessage();
			BaseClient.generateResponseDataMap(false, failureMsg, failureMsg + RangerSolrConstants.errMessage, null, null, responseData);
		}
	}

	private List<String> getSchemaList(List<String> ignoreSchemaList) throws Exception {
		return getCollectionList(ignoreSchemaList);
	}

	private List<String> getCollectionList(List<String> ignoreCollectionList)
			throws Exception {
		if (!isSolrCloud) {
			return getCoresList(ignoreCollectionList);
		}

		CollectionAdminRequest<?> request = new CollectionAdminRequest.List();
		String decPassword = getDecryptedPassword();
        if (!this.isKerberosAuth && username != null && decPassword != null) {
		    request.setBasicAuthCredentials(username, decPassword);
		}
		SolrResponse response = request.process(solrClient);
		List<String> list = new ArrayList<String>();
		List<String> responseCollectionList = (ArrayList<String>)response.getResponse().get("collections");
		if(CollectionUtils.isEmpty(responseCollectionList)) {
			return list;
		}
		for (String responseCollection : responseCollectionList) {
			if (ignoreCollectionList == null
					|| !ignoreCollectionList.contains(responseCollection)) {
				list.add(responseCollection);
			}
		}
		return list;
	}

	private List<String> getCoresList(List<String> ignoreCollectionList)
			throws Exception {
		CoreAdminRequest request = new CoreAdminRequest();
		request.setAction(CoreAdminAction.STATUS);
		String decPassword = getDecryptedPassword();
        if (!this.isKerberosAuth && username != null && decPassword != null) {
		    request.setBasicAuthCredentials(username, decPassword);
		}
		CoreAdminResponse cores = request.process(solrClient);
		// List of the cores
		List<String> coreList = new ArrayList<String>();
		for (int i = 0; i < cores.getCoreStatus().size(); i++) {
			if (ignoreCollectionList == null
					|| !ignoreCollectionList.contains(cores.getCoreStatus()
							.getName(i))) {
				coreList.add(cores.getCoreStatus().getName(i));
			}
		}
		return coreList;
	}

	private List<String> getFieldList(String collection,
			List<String> ignoreFieldList) throws Exception {
		// TODO: Best is to get the collections based on the collection value
		// which could contain wild cards
		String queryStr = "";
		if (collection != null && !collection.isEmpty()) {
			queryStr += "/" + collection;
		}
		queryStr += "/schema/fields";
		SolrQuery query = new SolrQuery();
		query.setRequestHandler(queryStr);
		QueryRequest req = new QueryRequest(query);
		String decPassword = getDecryptedPassword();
		if (!this.isKerberosAuth && username != null && decPassword != null) {
		    req.setBasicAuthCredentials(username, decPassword);
		}
		QueryResponse response = req.process(solrClient);

		List<String> fieldList = new ArrayList<String>();
		if (response != null && response.getStatus() == 0) {
			@SuppressWarnings("unchecked")
			List<SimpleOrderedMap<String>> fields = (ArrayList<SimpleOrderedMap<String>>) response
					.getResponse().get("fields");
			for (SimpleOrderedMap<String> fmap : fields) {
				String fieldName = fmap.get("name");
				if (ignoreFieldList == null
						|| !ignoreFieldList.contains(fieldName)) {
					fieldList.add(fieldName);
				}
			}
		} else {
			LOG.error("Error getting fields for collection=" + collection
					+ ", response=" + response);
		}
		return fieldList;
	}

	private List<String> getFieldList(List<String> collectionList,
			List<String> ignoreFieldList) throws Exception {

		Set<String> fieldSet = new LinkedHashSet<String>();
		if (collectionList == null || collectionList.size() == 0) {
			return getFieldList((String) null, ignoreFieldList);
		}
		for (String collection : collectionList) {
			try {
				fieldSet.addAll(getFieldList(collection, ignoreFieldList));
			} catch (Exception ex) {
				LOG.error("Error getting fields.", ex);
			}
		}
		return new ArrayList<String>(fieldSet);
	}


	private List<String> getConfigList(List<String> ignoreConfigList)
			throws Exception {

		ConfigSetAdminRequest request = new ConfigSetAdminRequest.List();

		String decPassword = getDecryptedPassword();
		if (!this.isKerberosAuth && username != null && decPassword != null) {
			request.setBasicAuthCredentials(username, decPassword);
		}
		SolrResponse response = request.process(solrClient);
		List<String> list = new ArrayList<String>();
		List<String> responseConfigSetList = (ArrayList<String>)response.getResponse().get("configSets");
		if(CollectionUtils.isEmpty(responseConfigSetList)) {
			return list;
		}
		for (String responseConfigSet : responseConfigSetList) {
			if (ignoreConfigList == null
					|| !ignoreConfigList.contains(responseConfigSet)) {
				list.add(responseConfigSet);
			}
		}
		return list;
	}

	public List<String> getResources(ResourceLookupContext context) {

		String userInput = context.getUserInput();
		String resource = context.getResourceName();
		Map<String, List<String>> resourceMap = context.getResources();
		List<String> resultList = null;
		List<String> collectionList = null;
		List<String> fieldList = null;
		List<String> configList = null;
		List<String> schemaList = null;

		RangerSolrConstants.RESOURCE_TYPE lookupResource = RangerSolrConstants.RESOURCE_TYPE.COLLECTION;

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== getResources() UserInput: \""
					+ userInput + "\" resource : " + resource
					+ " resourceMap: " + resourceMap);
		}

		if (userInput != null && resource != null) {
			if (resourceMap != null && !resourceMap.isEmpty()) {
				collectionList = resourceMap.get(RangerSolrConstants.COLLECTION_KEY);
				fieldList = resourceMap.get(RangerSolrConstants.FIELD_KEY);
				configList = resourceMap.get(RangerSolrConstants.CONFIG_KEY);
				schemaList = resourceMap.get(RangerSolrConstants.SCHEMA_KEY);
			}
			switch (resource.trim().toLowerCase()) {
			case RangerSolrConstants.COLLECTION_KEY:
				lookupResource = RangerSolrConstants.RESOURCE_TYPE.COLLECTION;
				break;
			case RangerSolrConstants.FIELD_KEY:
				lookupResource = RangerSolrConstants.RESOURCE_TYPE.FIELD;
				break;
			case RangerSolrConstants.CONFIG_KEY:
				lookupResource = RangerSolrConstants.RESOURCE_TYPE.CONFIG;
				break;
			case RangerSolrConstants.ADMIN_KEY:
				lookupResource = RangerSolrConstants.RESOURCE_TYPE.ADMIN;
				break;
			case RangerSolrConstants.SCHEMA_KEY:
				lookupResource = RangerSolrConstants.RESOURCE_TYPE.SCHEMA;
			default:
				break;
			}
		}
		if (userInput != null) {
			try {
				Callable<List<String>> callableObj = null;
				final String userInputFinal = userInput;

				final List<String> finalCollectionList = collectionList;
				final List<String> finalFieldList = fieldList;
				final List<String> finalConfigList = configList;
				final List<String> finalSchemaList = schemaList;

				if (lookupResource == RangerSolrConstants.RESOURCE_TYPE.COLLECTION) {
					// get the collection list for given Input
					callableObj = new Callable<List<String>>() {
						@Override
						public List<String> call() {
							List<String> retList = new ArrayList<String>();
							try {
								List<String> list = null;
								if (isKerberosAuth) {
									list = Subject.doAs(loginSubject, new PrivilegedAction<List<String>>() {
										@Override
										public List<String> run() {
											List<String> ret = null;
											try {
												ret = getCollectionList(finalCollectionList);
											} catch (Exception e) {
												LOG.error("Unable to get collections, Error : " + e.getMessage(),
														new Throwable(e));
											}
											return ret;
										}
									});
								} else {
									list = getCollectionList(finalCollectionList);
								}
								if (userInputFinal != null
										&& !userInputFinal.isEmpty()) {
									for (String value : list) {
										if (value.startsWith(userInputFinal)) {
											retList.add(value);
										}
									}
								} else {
									retList.addAll(list);
								}
							} catch (Exception ex) {
								LOG.error("Error getting collections.", ex);
							}
							return retList;
						};
					};
				} else if (lookupResource == RangerSolrConstants.RESOURCE_TYPE.FIELD) {
					callableObj = new Callable<List<String>>() {
						@Override
						public List<String> call() {
							List<String> retList = new ArrayList<String>();
							try {
								List<String> list = null;
								if (isKerberosAuth) {
									list = Subject.doAs(loginSubject, new PrivilegedAction<List<String>>() {
										@Override
										public List<String> run() {
											List<String> ret = new ArrayList<String>();
											try {
												ret = getFieldList(finalCollectionList, finalFieldList);
											} catch (Exception e) {
												LOG.error("Unable to get field list, Error : " + e.getMessage(),
														new Throwable(e));
											}
											return ret;
										}
									});
								} else {
									list = getFieldList(finalCollectionList, finalFieldList);
								}
								if (userInputFinal != null
										&& !userInputFinal.isEmpty()) {
									for (String value : list) {
										if (value.startsWith(userInputFinal)) {
											retList.add(value);
										}
									}
								} else {
									retList.addAll(list);
								}
							} catch (Exception ex) {
								LOG.error("Error getting collections.", ex);
							}
							return retList;
						};
					};
				} else if (lookupResource == RangerSolrConstants.RESOURCE_TYPE.CONFIG) {
					// get the config list for given Input
					callableObj = new Callable<List<String>>() {
						@Override
						public List<String> call() {
							List<String> retList = new ArrayList<String>();
							try {
								List<String> list = null;
								if (isKerberosAuth) {
									list = Subject.doAs(loginSubject, new PrivilegedAction<List<String>>() {
										@Override
										public List<String> run() {
											List<String> ret = null;
											try {
												ret = getConfigList(finalConfigList);
											} catch (Exception e) {
												LOG.error("Unable to get Solr configs, Error : " + e.getMessage(),
														new Throwable(e));
											}
											return ret;
										}
									});
								} else {
									list = getConfigList(finalConfigList);
								}
								if (userInputFinal != null
										&& !userInputFinal.isEmpty()) {
									for (String value : list) {
										if (value.startsWith(userInputFinal)) {
											retList.add(value);
										}
									}
								} else {
									retList.addAll(list);
								}
							} catch (Exception ex) {
								LOG.error("Error getting Solr configs.", ex);
							}
							return retList;
						}

						;
					} ;
				} else if (lookupResource == RangerSolrConstants.RESOURCE_TYPE.ADMIN) {
					List<String> retList = new ArrayList<String>();
					try {
						List<String> list = RangerSolrConstants.ADMIN_TYPE.VALUE_LIST;

						if (userInputFinal != null
								&& !userInputFinal.isEmpty()) {
							for (String value : list) {
								if (value.startsWith(userInputFinal)) {
									retList.add(value);
								}
							}
						} else {
							retList.addAll(list);
						}
					} catch (Exception ex) {
						LOG.error("Error getting Solr admin resources.", ex);
					}
					resultList = retList;

				} else if (lookupResource == RangerSolrConstants.RESOURCE_TYPE.SCHEMA) {
					// get the collection list for given Input, since there is no way of getting a list of the available
					// schemas
					callableObj = new Callable<List<String>>() {
						@Override
						public List<String> call() {
							List<String> retList = new ArrayList<String>();
							try {
								List<String> list = null;
								if (isKerberosAuth) {
									list = Subject.doAs(loginSubject, new PrivilegedAction<List<String>>() {
										@Override
										public List<String> run() {
											List<String> ret = null;
											try {
												ret = getSchemaList(finalSchemaList);
											} catch (Exception e) {
												LOG.error("Unable to get collections for schema listing, Error : "
														+ e.getMessage(), new Throwable(e));
											}
											return ret;
										}
									});
								} else {
									list = getSchemaList(finalSchemaList);
								}
								if (userInputFinal != null
										&& !userInputFinal.isEmpty()) {
									for (String value : list) {
										if (value.startsWith(userInputFinal)) {
											retList.add(value);
										}
									}
								} else {
									retList.addAll(list);
								}
							} catch (Exception ex) {
								LOG.error("Error getting collections for schema listing.", ex);
							}
							return retList;
						};
					};
				}
				// If we need to do lookup
				if (callableObj != null) {
					synchronized (this) {
						resultList = TimedEventUtil.timedTask(callableObj,
								RangerSolrConstants.LOOKUP_TIMEOUT_SEC, TimeUnit.SECONDS);
					}
				}
			} catch (Exception e) {
				LOG.error("Unable to get Solr resources.", e);
			}

		} // end if userinput != null

		return resultList;
	}

	private String getDecryptedPassword() {
	    String decryptedPwd = null;
        try {
            decryptedPwd = PasswordUtils.decryptPassword(password);
        } catch (Exception ex) {
            LOG.info("Password decryption failed; trying Solr connection with received password string");
            decryptedPwd = null;
        } finally {
            if (decryptedPwd == null) {
                decryptedPwd = password;
            }
        }

        return decryptedPwd;
	}

	private void createSolrClientInstance() {
		this.setHttpClientBuilderForKrb();
		if (this.isSolrCloud) {
			List<String> zookeeperHosts = Arrays.asList(this.url);
			this.solrClient = new CloudSolrClient.Builder(zookeeperHosts, Optional.empty()).build();
		} else {
			HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
			builder.withBaseSolrUrl(this.url);
			this.solrClient = builder.build();
		}
	}

	private void setHttpClientBuilderForKrb() {
		if (this.isKerberosAuth) {
			Krb5HttpClientBuilder krbBuild = new Krb5HttpClientBuilder();
			SolrHttpClientBuilder kb = krbBuild.getBuilder();
			HttpClientUtil.setHttpClientBuilder(kb);
		}
	}

	private void login(Map<String, String> configs) {
		try {
			String adminPrincipal = configs.get(HadoopConfigHolder.RANGER_PRINCIPAL);
			String adminKeytab = configs.get(HadoopConfigHolder.RANGER_KEYTAB);
			String nameRules = configs.get(HadoopConfigHolder.RANGER_NAME_RULES);
			if (StringUtils.isEmpty(nameRules)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Name Rule is empty. Setting Name Rule as 'DEFAULT'");
				}
				nameRules = "DEFAULT";
			}
			String userName = this.username;
			if (StringUtils.isEmpty(adminPrincipal) || StringUtils.isEmpty(adminKeytab)) {
				if (userName == null) {
					throw createException("Unable to find login username for hadoop environment, [" + serviceName + "]", null);
				}
				String keyTabFile = configs.get(HadoopConfigHolder.RANGER_KEYTAB);
				if (keyTabFile != null) {
					if (this.isKerberosAuth) {
						LOG.info("Init Login: security enabled, using username/keytab");
						this.loginSubject = SecureClientLogin.loginUserFromKeytab(userName, keyTabFile, nameRules);
					} else {
						LOG.info("Init Login: using username");
						this.loginSubject = SecureClientLogin.login(userName);
					}
				} else {
					String encryptedPwd = this.password;
					String password = null;
					if (encryptedPwd != null) {
						try {
							password = PasswordUtils.decryptPassword(encryptedPwd);
						} catch (Exception ex) {
							LOG.info("Password decryption failed; trying connection with received password string");
							password = null;
						} finally {
							if (password == null) {
								password = encryptedPwd;
							}
						}
					} else {
						LOG.info("Password decryption failed: no password was configured");
					}
					if (this.isKerberosAuth) {
						LOG.info("Init Login: using username/password");
						this.loginSubject = SecureClientLogin.loginUserWithPassword(userName, password);
					} else {
						LOG.info("Init Login: security not enabled, using username");
						this.loginSubject = SecureClientLogin.login(userName);
					}
				}
			} else {
				if (this.isKerberosAuth) {
					LOG.info("Init Lookup Login: security enabled, using lookupPrincipal/lookupKeytab");
					this.loginSubject = SecureClientLogin.loginUserFromKeytab(adminPrincipal, adminKeytab, nameRules);
				} else {
					LOG.info("Init Login: security not enabled, using username");
					this.loginSubject = SecureClientLogin.login(userName);
				}
			}
		} catch (IOException ioe) {
			throw createException("Unable to login to Hadoop environment [" + serviceName + "]", ioe);
		} catch (SecurityException se) {
			throw createException("Unable to login to Hadoop environment [" + serviceName + "]", se);
		}
	}

	private HadoopException createException(String msgDesc, Exception exp) {
		HadoopException hdpException = new HadoopException(msgDesc, exp);
		final String fullDescription = exp != null ? BaseClient.getMessage(exp) : msgDesc;
		hdpException.generateResponseDataMap(false, fullDescription + RangerSolrConstants.errMessage,
			msgDesc + RangerSolrConstants.errMessage, null, null);
		return hdpException;
	}

}
