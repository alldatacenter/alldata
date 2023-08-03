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

package org.apache.ranger.audit.destination;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.audit.utils.InMemoryJAASConfiguration;
import org.apache.ranger.audit.utils.KerberosAction;
import org.apache.ranger.audit.utils.KerberosUser;
import org.apache.ranger.audit.utils.KerberosJAASConfigUser;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.login.LoginException;

import java.util.Arrays;
import java.util.Optional;


public class SolrAuditDestination extends AuditDestination {
	private static final Logger LOG = LoggerFactory
			.getLogger(SolrAuditDestination.class);

	public static final String PROP_SOLR_URLS = "urls";
	public static final String PROP_SOLR_ZK = "zookeepers";
	public static final String PROP_SOLR_COLLECTION = "collection";
	public static final String PROP_SOLR_FORCE_USE_INMEMORY_JAAS_CONFIG = "force.use.inmemory.jaas.config";

	public static final String DEFAULT_COLLECTION_NAME = "ranger_audits";
	public static final String PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

	private volatile SolrClient solrClient = null;
	private volatile KerberosUser kerberosUser = null;

	public SolrAuditDestination() {
	}

	@Override
	public void init(Properties props, String propPrefix) {
		LOG.info("init() called");
		super.init(props, propPrefix);
		init();
		connect();
	}

	@Override
	public void stop() {
		LOG.info("SolrAuditDestination.stop() called..");
		logStatus();

		if (solrClient != null) {
			try {
				solrClient.close();
			} catch (IOException ioe) {
				LOG.error("Error while stopping slor!", ioe);
			} finally {
				solrClient = null;
			}
		}

		if (kerberosUser != null) {
			try {
				kerberosUser.logout();
			} catch (LoginException excp) {
				LOG.error("Error logging out keytab user", excp);
			} finally {
				kerberosUser = null;
			}
		}
	}

	synchronized void connect() {
		SolrClient me = solrClient;
		if (me == null) {
			synchronized(SolrAuditDestination.class) {
				me = solrClient;
				if (solrClient == null) {
					KeyManager[]   kmList     = getKeyManagers();
					TrustManager[] tmList     = getTrustManagers();
					SSLContext     sslContext = getSSLContext(kmList, tmList);
					if(sslContext != null) {
						SSLContext.setDefault(sslContext);
					}
					String urls = MiscUtil.getStringProperty(props, propPrefix
							+ "." + PROP_SOLR_URLS);
					if (urls != null) {
						urls = urls.trim();
					}
					if (urls != null && urls.equalsIgnoreCase("NONE")) {
						urls = null;
					}
					List<String> solrURLs = new ArrayList<String>();
					String zkHosts = null;
					solrURLs = MiscUtil.toArray(urls, ",");
					zkHosts = MiscUtil.getStringProperty(props, propPrefix + "."
							+ PROP_SOLR_ZK);
					if (zkHosts != null && zkHosts.equalsIgnoreCase("NONE")) {
						zkHosts = null;
					}
					String collectionName = MiscUtil.getStringProperty(props,
							propPrefix + "." + PROP_SOLR_COLLECTION);
					if (collectionName == null
							|| collectionName.equalsIgnoreCase("none")) {
						collectionName = DEFAULT_COLLECTION_NAME;
					}

					LOG.info("Solr zkHosts=" + zkHosts + ", solrURLs=" + urls
							+ ", collectionName=" + collectionName);

					if (zkHosts != null && !zkHosts.isEmpty()) {
						LOG.info("Connecting to solr cloud using zkHosts="
								+ zkHosts);
						try {
							// Instantiate
							Krb5HttpClientBuilder krbBuild = new Krb5HttpClientBuilder();
							SolrHttpClientBuilder kb = krbBuild.getBuilder();
							HttpClientUtil.setHttpClientBuilder(kb);

							final List<String> zkhosts = new ArrayList<String>(Arrays.asList(zkHosts.split(",")));
							final CloudSolrClient solrCloudClient = MiscUtil.executePrivilegedAction(new PrivilegedExceptionAction<CloudSolrClient>() {
								@Override
								public CloudSolrClient run()  throws Exception {
									CloudSolrClient solrCloudClient = new CloudSolrClient.Builder(zkhosts, Optional.empty()).build();
									return solrCloudClient;
								};
							});

							solrCloudClient.setDefaultCollection(collectionName);
							me = solrClient = solrCloudClient;
						} catch (Throwable t) {
							LOG.error("Can't connect to Solr server. ZooKeepers="
									+ zkHosts, t);
						}
					} else if (solrURLs != null && !solrURLs.isEmpty()) {
						try {
							LOG.info("Connecting to Solr using URLs=" + solrURLs);
							Krb5HttpClientBuilder krbBuild = new Krb5HttpClientBuilder();
							SolrHttpClientBuilder kb = krbBuild.getBuilder();
							HttpClientUtil.setHttpClientBuilder(kb);
							final List<String> solrUrls = solrURLs;
							final LBHttpSolrClient lbSolrClient = MiscUtil.executePrivilegedAction(new PrivilegedExceptionAction<LBHttpSolrClient>() {
								@Override
								public LBHttpSolrClient run()  throws Exception {
									LBHttpSolrClient.Builder builder = new LBHttpSolrClient.Builder();
									builder.withBaseSolrUrl(solrUrls.get(0));
									builder.withConnectionTimeout(1000);
									LBHttpSolrClient lbSolrClient = builder.build();
									return lbSolrClient;
								};
							});

							for (int i = 1; i < solrURLs.size(); i++) {
								lbSolrClient.addSolrServer(solrURLs.get(i));
							}
							me = solrClient = lbSolrClient;
						} catch (Throwable t) {
							LOG.error("Can't connect to Solr server. URL="
									+ solrURLs, t);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		boolean ret = false;
		try {
			logStatusIfRequired();
			addTotalCount(events.size());

			if (solrClient == null) {
				connect();
				if (solrClient == null) {
					// Solr is still not initialized. So need return error
					addDeferredCount(events.size());
					return ret;
				}
			}

			final Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
			for (AuditEventBase event : events) {
				AuthzAuditEvent authzEvent = (AuthzAuditEvent) event;
				// Convert AuditEventBase to Solr document
				SolrInputDocument document = toSolrDoc(authzEvent);
				docs.add(document);
			}
			try {
				final UpdateResponse response = addDocsToSolr(solrClient, docs);

				if (response.getStatus() != 0) {
					addFailedCount(events.size());
					logFailedEvent(events, response.toString());
				} else {
					addSuccessCount(events.size());
					ret = true;
				}
			} catch (SolrException ex) {
				addFailedCount(events.size());
				logFailedEvent(events, ex);
			}
		} catch (Throwable t) {
			addDeferredCount(events.size());
			logError("Error sending message to Solr", t);
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#flush()
	 */
	@Override
	public void flush() {

	}

	SolrInputDocument toSolrDoc(AuthzAuditEvent auditEvent) {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", auditEvent.getEventId());
		doc.addField("access", auditEvent.getAccessType());
		doc.addField("enforcer", auditEvent.getAclEnforcer());
		doc.addField("agent", auditEvent.getAgentId());
		doc.addField("repo", auditEvent.getRepositoryName());
		doc.addField("sess", auditEvent.getSessionId());
		doc.addField("reqUser", auditEvent.getUser());
		doc.addField("reqData", auditEvent.getRequestData());
		doc.addField("resource", auditEvent.getResourcePath());
		doc.addField("cliIP", auditEvent.getClientIP());
		doc.addField("logType", auditEvent.getLogType());
		doc.addField("result", auditEvent.getAccessResult());
		doc.addField("policy", auditEvent.getPolicyId());
		doc.addField("repoType", auditEvent.getRepositoryType());
		doc.addField("resType", auditEvent.getResourceType());
		doc.addField("reason", auditEvent.getResultReason());
		doc.addField("action", auditEvent.getAction());
		doc.addField("evtTime", auditEvent.getEventTime());
		doc.addField("seq_num", auditEvent.getSeqNum());
		doc.setField("event_count", auditEvent.getEventCount());
		doc.setField("event_dur_ms", auditEvent.getEventDurationMS());
		doc.setField("tags", auditEvent.getTags());
		doc.setField("cluster", auditEvent.getClusterName());
		doc.setField("zoneName", auditEvent.getZoneName());
		doc.setField("agentHost", auditEvent.getAgentHostname());
		doc.setField("policyVersion", auditEvent.getPolicyVersion());

		return doc;
	}

	public boolean isAsync() {
		return true;
	}

	private void init() {
		LOG.info("==>SolrAuditDestination.init()" );
		try {
			 // SolrJ requires "java.security.auth.login.config"  property to be set to identify itself that it is kerberized. So using a dummy property for it
			 // Acutal solrclient JAAS configs are read from the ranger-<component>-audit.xml present in  components conf folder and set by InMemoryJAASConfiguration
			 // Refer InMemoryJAASConfiguration doc for JAAS Configuration
			 String confFileName = System.getProperty(PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG);
			 LOG.info("In solrAuditDestination.init() : JAAS Configuration set as [" + confFileName + "]");
			 if ( System.getProperty(PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG) == null ) {
				 if ( MiscUtil.getBooleanProperty(props, propPrefix + "." + PROP_SOLR_FORCE_USE_INMEMORY_JAAS_CONFIG,false) ) {
					 System.setProperty(PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG, "/dev/null");
				 } else {
					LOG.warn("No Client JAAS config present in solr audit config. Ranger Audit to Kerberized Solr will fail...");
				}
			 }

			 LOG.info("Loading SolrClient JAAS config from Ranger audit config if present...");

			 InMemoryJAASConfiguration conf = InMemoryJAASConfiguration.init(props);

			 KerberosUser kerberosUser = new KerberosJAASConfigUser("Client", conf);

			 if (kerberosUser.getPrincipal() != null) {
				this.kerberosUser = kerberosUser;
			 }
		} catch (Exception e) {
			LOG.error("ERROR: Unable to load SolrClient JAAS config from Audit config file. Audit to Kerberized Solr will fail...", e);
		} finally {
			 String confFileName = System.getProperty(PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG);
			 LOG.info("In solrAuditDestination.init() (finally) : JAAS Configuration set as [" + confFileName + "]");
		}
		LOG.info("<==SolrAuditDestination.init()" );
	}

	private KeyManager[] getKeyManagers() {
		KeyManager[] kmList = null;
		String credentialProviderPath = MiscUtil.getStringProperty(props, RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL);
		String keyStoreAlias = RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL_ALIAS;
		String keyStoreFile = MiscUtil.getStringProperty(props, RANGER_POLICYMGR_CLIENT_KEY_FILE);
		String keyStoreFilepwd = MiscUtil.getCredentialString(credentialProviderPath, keyStoreAlias);
		if (StringUtils.isNotEmpty(keyStoreFile) && StringUtils.isNotEmpty(keyStoreFilepwd)) {
			InputStream in = null;

			try {
				in = getFileInputStream(keyStoreFile);

				if (in != null) {
					String keyStoreType = MiscUtil.getStringProperty(props, RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE);
					keyStoreType = StringUtils.isNotEmpty(keyStoreType) ? keyStoreType : RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE_DEFAULT;
					KeyStore keyStore = KeyStore.getInstance(keyStoreType);

					keyStore.load(in, keyStoreFilepwd.toCharArray());

					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(RANGER_SSL_KEYMANAGER_ALGO_TYPE);

					keyManagerFactory.init(keyStore, keyStoreFilepwd.toCharArray());

					kmList = keyManagerFactory.getKeyManagers();
				} else {
					LOG.error("Unable to obtain keystore from file [" + keyStoreFile + "]");
				}
			} catch (KeyStoreException e) {
				LOG.error("Unable to obtain from KeyStore :" + e.getMessage(), e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("SSL algorithm is NOT available in the environment", e);
			} catch (CertificateException e) {
				LOG.error("Unable to obtain the requested certification ", e);
			} catch (FileNotFoundException e) {
				LOG.error("Unable to find the necessary SSL Keystore Files", e);
			} catch (IOException e) {
				LOG.error("Unable to read the necessary SSL Keystore Files", e);
			} catch (UnrecoverableKeyException e) {
				LOG.error("Unable to recover the key from keystore", e);
			} finally {
				close(in, keyStoreFile);
			}
		}

		return kmList;
	}

	private TrustManager[] getTrustManagers() {
		TrustManager[] tmList = null;
		String credentialProviderPath = MiscUtil.getStringProperty(props, RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL);
		String trustStoreAlias = RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL_ALIAS;
		String trustStoreFile = MiscUtil.getStringProperty(props, RANGER_POLICYMGR_TRUSTSTORE_FILE);
		String trustStoreFilepwd = MiscUtil.getCredentialString(credentialProviderPath, trustStoreAlias);
		if (StringUtils.isNotEmpty(trustStoreFile) && StringUtils.isNotEmpty(trustStoreFilepwd)) {
			InputStream in = null;

			try {
				in = getFileInputStream(trustStoreFile);

				if (in != null) {
					String trustStoreType = MiscUtil.getStringProperty(props, RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE);
					trustStoreType = StringUtils.isNotEmpty(trustStoreType) ? trustStoreType : RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE_DEFAULT;
					KeyStore trustStore = KeyStore.getInstance(trustStoreType);

					trustStore.load(in, trustStoreFilepwd.toCharArray());

					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(RANGER_SSL_TRUSTMANAGER_ALGO_TYPE);

					trustManagerFactory.init(trustStore);

					tmList = trustManagerFactory.getTrustManagers();
				} else {
					LOG.error("Unable to obtain truststore from file [" + trustStoreFile + "]");
				}
			} catch (KeyStoreException e) {
				LOG.error("Unable to obtain from KeyStore", e);
			} catch (NoSuchAlgorithmException e) {
				LOG.error("SSL algorithm is NOT available in the environment :" + e.getMessage(), e);
			} catch (CertificateException e) {
				LOG.error("Unable to obtain the requested certification :" + e.getMessage(), e);
			} catch (FileNotFoundException e) {
				LOG.error("Unable to find the necessary SSL TrustStore File:" + trustStoreFile, e);
			} catch (IOException e) {
				LOG.error("Unable to read the necessary SSL TrustStore Files :" + trustStoreFile, e);
			} finally {
				close(in, trustStoreFile);
			}
		}

		return tmList;
	}

	private SSLContext getSSLContext(KeyManager[] kmList, TrustManager[] tmList) {
		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance(RANGER_SSL_CONTEXT_ALGO_TYPE);
			if (sslContext != null) {
				sslContext.init(kmList, tmList, new SecureRandom());
			}
		} catch (NoSuchAlgorithmException e) {
			LOG.error("SSL algorithm is not available in the environment", e);
		} catch (KeyManagementException e) {
			LOG.error("Unable to initialise the SSLContext", e);
		}
		return sslContext;
	}

	private  UpdateResponse addDocsToSolr(final SolrClient solrClient, final Collection<SolrInputDocument> docs) throws Exception {
		final UpdateResponse ret;

		try {
			final PrivilegedExceptionAction<UpdateResponse> action = () -> solrClient.add(docs);

			if (kerberosUser != null) {
				// execute the privileged action as the given keytab user
				final KerberosAction kerberosAction = new KerberosAction<>(kerberosUser, action, LOG);

				ret = (UpdateResponse) kerberosAction.execute();
			} else {
				ret = action.run();
			}
		} catch (Exception e) {
			throw e;
		}

		return ret;
	}

	private InputStream getFileInputStream(String fileName) throws IOException {
		InputStream in = null;
		if (StringUtils.isNotEmpty(fileName)) {
			File file = new File(fileName);
			if (file != null && file.exists()) {
				in = new FileInputStream(file);
			} else {
				in = ClassLoader.getSystemResourceAsStream(fileName);
			}
		}
		return in;
	}

	private void close(InputStream str, String filename) {
		if (str != null) {
			try {
				str.close();
			} catch (IOException excp) {
				LOG.error("Error while closing file: [" + filename + "]", excp);
			}
		}
	}
}
