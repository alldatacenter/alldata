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

package org.apache.ranger.server.tomcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecureClientLogin;

import org.apache.ranger.credentialapi.CredentialReader;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.Subject;

public class EmbeddedServer {

	private static final Logger LOG = Logger.getLogger(EmbeddedServer.class
			.getName());
	private static final String DEFAULT_NAME_RULE = "DEFAULT";
	private static final String DEFAULT_WEBAPPS_ROOT_FOLDER = "webapps";
	private static String configFile = "ranger-admin-site.xml";
	private static final String AUTH_TYPE_KERBEROS = "kerberos";
	private static final String AUTHENTICATION_TYPE = "hadoop.security.authentication";
	private static final String ADMIN_USER_PRINCIPAL = "ranger.admin.kerberos.principal";
	private static final String AUDIT_SOURCE_TYPE = "ranger.audit.source.type";
	private static final String AUDIT_SOURCE_SOLR = "solr";
	private static final String AUDIT_SOURCE_ES = "elasticsearch";
	private static final String SOLR_BOOTSTRAP_ENABLED = "ranger.audit.solr.bootstrap.enabled";
	private static final String ES_BOOTSTRAP_ENABLED = "ranger.audit.elasticsearch.bootstrap.enabled";
	private static final String ADMIN_USER_KEYTAB = "ranger.admin.kerberos.keytab";

	private static final String ADMIN_NAME_RULES = "hadoop.security.auth_to_local";
	private static final String ADMIN_SERVER_NAME = "rangeradmin";
	private static final String KMS_SERVER_NAME   = "rangerkms";
	private static final String ACCESS_LOG_PREFIX = "ranger.accesslog.prefix";
	private static final String ACCESS_LOG_DATE_FORMAT = "ranger.accesslog.dateformat";
	private static final String ACCESS_LOG_PATTERN = "ranger.accesslog.pattern";
	private static final String ACCESS_LOG_ROTATE_ENABLED = "ranger.accesslog.rotate.enabled";
	private static final String ACCESS_LOG_ROTATE_MAX_DAYS = "ranger.accesslog.rotate.max_days";
	private static final String ACCESS_LOG_ROTATE_RENAME_ON_ROTATE = "ranger.accesslog.rotate.rename_on_rotate";
	public static final String RANGER_KEYSTORE_FILE_TYPE_DEFAULT = KeyStore.getDefaultType();
	public static final String RANGER_TRUSTSTORE_FILE_TYPE_DEFAULT = KeyStore.getDefaultType();
	public static final String RANGER_SSL_CONTEXT_ALGO_TYPE = "TLSv1.2";
	public static final String RANGER_SSL_KEYMANAGER_ALGO_TYPE = KeyManagerFactory.getDefaultAlgorithm();
	public static final String RANGER_SSL_TRUSTMANAGER_ALGO_TYPE = TrustManagerFactory.getDefaultAlgorithm();

	public static void main(String[] args) {
		new EmbeddedServer(args).start();
	}

	public EmbeddedServer(String[] args) {
		if (args.length > 0) {
			configFile = args[0];
		}

		EmbeddedServerUtil.loadRangerConfigProperties(configFile);
	}

	public static int DEFAULT_SHUTDOWN_PORT = 6185;
	public static String DEFAULT_SHUTDOWN_COMMAND = "SHUTDOWN";

	public void start() {
		SSLContext sslContext = getSSLContext();
		if (sslContext != null) {
			SSLContext.setDefault(sslContext);
		}
		final Tomcat server = new Tomcat();

		String logDir =  null;
		logDir = EmbeddedServerUtil.getConfig("logdir");
		if (logDir == null) {
			logDir = EmbeddedServerUtil.getConfig("kms.log.dir");
		}
		String servername = EmbeddedServerUtil.getConfig("servername");
		String hostName = EmbeddedServerUtil.getConfig("ranger.service.host");
		int serverPort = EmbeddedServerUtil.getIntConfig("ranger.service.http.port", 6181);
		int sslPort = EmbeddedServerUtil.getIntConfig("ranger.service.https.port", -1);
		int shutdownPort = EmbeddedServerUtil.getIntConfig("ranger.service.shutdown.port", DEFAULT_SHUTDOWN_PORT);
		String shutdownCommand = EmbeddedServerUtil.getConfig("ranger.service.shutdown.command", DEFAULT_SHUTDOWN_COMMAND);

		server.setHostname(hostName);
		server.setPort(serverPort);
		server.getServer().setPort(shutdownPort);
		server.getServer().setShutdown(shutdownCommand);

		boolean isHttpsEnabled = Boolean.valueOf(EmbeddedServerUtil.getConfig("ranger.service.https.attrib.ssl.enabled", "false"));
		boolean ajpEnabled = Boolean.valueOf(EmbeddedServerUtil.getConfig("ajp.enabled", "false"));

		if (ajpEnabled) {

			Connector ajpConnector = new Connector(
					"org.apache.coyote.ajp.AjpNioProtocol");
			ajpConnector.setPort(serverPort);
			ajpConnector.setProperty("protocol", "AJP/1.3");

			server.getService().addConnector(ajpConnector);

			// Making this as a default connector
			server.setConnector(ajpConnector);
			LOG.info("Created AJP Connector");
		} else if ((sslPort > 0) && isHttpsEnabled) {
			Connector ssl = new Connector();
			ssl.setPort(sslPort);
			ssl.setSecure(true);
			ssl.setScheme("https");
			ssl.setAttribute("SSLEnabled", "true");
			ssl.setAttribute("sslProtocol", EmbeddedServerUtil.getConfig("ranger.service.https.attrib.ssl.protocol", "TLSv1.2"));
			ssl.setAttribute("keystoreType", EmbeddedServerUtil.getConfig("ranger.keystore.file.type", RANGER_KEYSTORE_FILE_TYPE_DEFAULT));
			ssl.setAttribute("truststoreType", EmbeddedServerUtil.getConfig("ranger.truststore.file.type", RANGER_TRUSTSTORE_FILE_TYPE_DEFAULT));
			String clientAuth = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.clientAuth", "false");
			if("false".equalsIgnoreCase(clientAuth)){
				clientAuth = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.client.auth", "want");
			}
			ssl.setAttribute("clientAuth",clientAuth);
			String providerPath = EmbeddedServerUtil.getConfig("ranger.credential.provider.path");
			String keyAlias = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.keystore.credential.alias", "keyStoreCredentialAlias");
			String keystorePass=null;
			if(providerPath!=null && keyAlias!=null){
				keystorePass = CredentialReader.getDecryptedString(providerPath.trim(), keyAlias.trim(), EmbeddedServerUtil.getConfig("ranger.keystore.file.type", RANGER_KEYSTORE_FILE_TYPE_DEFAULT));
				if (StringUtils.isBlank(keystorePass) || "none".equalsIgnoreCase(keystorePass.trim())) {
					keystorePass = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.keystore.pass");
				}
			}
			ssl.setAttribute("keyAlias", EmbeddedServerUtil.getConfig("ranger.service.https.attrib.keystore.keyalias", "rangeradmin"));
			ssl.setAttribute("keystorePass", keystorePass);
			ssl.setAttribute("keystoreFile", getKeystoreFile());

			String defaultEnabledProtocols = "TLSv1.2";
			String enabledProtocols = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.ssl.enabled.protocols", defaultEnabledProtocols);
			ssl.setAttribute("sslEnabledProtocols", enabledProtocols);
			String ciphers = EmbeddedServerUtil.getConfig("ranger.tomcat.ciphers");
			if (StringUtils.isNotBlank(ciphers)) {
				ssl.setAttribute("ciphers", ciphers);
			}
			server.getService().addConnector(ssl);

			//
			// Making this as a default connector
			//
			server.setConnector(ssl);

		}
		updateHttpConnectorAttribConfig(server);

		File logDirectory = new File(logDir);
		if (!logDirectory.exists()) {
			logDirectory.mkdirs();
		}

		AccessLogValve valve = new AccessLogValve();
		valve.setRotatable(true);
		valve.setAsyncSupported(true);
		valve.setBuffered(false);
		valve.setEnabled(true);
		valve.setPrefix(EmbeddedServerUtil.getConfig(ACCESS_LOG_PREFIX,"access-" + hostName));
		valve.setFileDateFormat(EmbeddedServerUtil.getConfig(ACCESS_LOG_DATE_FORMAT, "-yyyy-MM-dd.HH"));
		valve.setDirectory(logDirectory.getAbsolutePath());
		valve.setSuffix(".log");
		valve.setRotatable(EmbeddedServerUtil.getBooleanConfig(ACCESS_LOG_ROTATE_ENABLED, true));
		valve.setMaxDays(EmbeddedServerUtil.getIntConfig(ACCESS_LOG_ROTATE_MAX_DAYS,15));
		valve.setRenameOnRotate(EmbeddedServerUtil.getBooleanConfig(ACCESS_LOG_ROTATE_RENAME_ON_ROTATE, false));

		String defaultAccessLogPattern = servername.equalsIgnoreCase(KMS_SERVER_NAME) ? "%h %l %u %t \"%m %U\" %s %b %D" : "%h %l %u %t \"%r\" %s %b %D";
		String logPattern = EmbeddedServerUtil.getConfig(ACCESS_LOG_PATTERN, defaultAccessLogPattern);
		valve.setPattern(logPattern);

		server.getHost().getPipeline().addValve(valve);

		ErrorReportValve errorReportValve = new ErrorReportValve();
		boolean showServerinfo = Boolean.valueOf(EmbeddedServerUtil.getConfig("ranger.valve.errorreportvalve.showserverinfo", "true"));
		boolean showReport = Boolean.valueOf(EmbeddedServerUtil.getConfig("ranger.valve.errorreportvalve.showreport", "true"));
		errorReportValve.setShowServerInfo(showServerinfo);
		errorReportValve.setShowReport(showReport);
		server.getHost().getPipeline().addValve(errorReportValve);

		try {
			String webapp_dir = EmbeddedServerUtil.getConfig("xa.webapp.dir");
			if (StringUtils.isBlank(webapp_dir)) {
				// If webapp location property is not set, then let's derive
				// from catalina_base
				String catalina_base = EmbeddedServerUtil.getConfig("catalina.base");
				if (StringUtils.isBlank(catalina_base)) {
					LOG.severe("Tomcat Server failed to start: catalina.base and/or xa.webapp.dir is not set");
					System.exit(1);
				}
				webapp_dir = catalina_base + File.separator + "webapp";
				LOG.info("Deriving webapp folder from catalina.base property. folder="
						+ webapp_dir);
			}

			//String webContextName = getConfig("xa.webapp.contextName", "/");
			String webContextName = EmbeddedServerUtil.getConfig("ranger.contextName", "/");
			if (webContextName == null) {
				webContextName = "/";
			} else if (!webContextName.startsWith("/")) {
				LOG.info("Context Name [" + webContextName
						+ "] is being loaded as [ /" + webContextName + "]");
				webContextName = "/" + webContextName;
			}

			File wad = new File(webapp_dir);
			if (wad.isDirectory()) {
				LOG.info("Webapp file =" + webapp_dir + ", webAppName = "
						+ webContextName);
			} else if (wad.isFile()) {
				File webAppDir = new File(DEFAULT_WEBAPPS_ROOT_FOLDER);
				if (!webAppDir.exists()) {
					webAppDir.mkdirs();
				}
				LOG.info("Webapp file =" + webapp_dir + ", webAppName = "
						+ webContextName);
			}
			LOG.info("Adding webapp [" + webContextName + "] = path ["
					+ webapp_dir + "] .....");
                       StandardContext webappCtx = (StandardContext) server.addWebapp(webContextName, new File(
                           webapp_dir).getAbsolutePath());
                       String workDirPath = EmbeddedServerUtil.getConfig("ranger.tomcat.work.dir", "");
                       if (!workDirPath.isEmpty() && new File(workDirPath).exists()) {
                           webappCtx.setWorkDir(workDirPath);
                       } else {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Skipping to set tomcat server work directory, '" + workDirPath +
                                    "', as it is blank or directory does not exist.");
                            }
                       }
			webappCtx.init();
			LOG.info("Finished init of webapp [" + webContextName
					+ "] = path [" + webapp_dir + "].");
		} catch (LifecycleException lce) {
			LOG.severe("Tomcat Server failed to start webapp:" + lce.toString());
			lce.printStackTrace();
		}

		if (servername.equalsIgnoreCase(ADMIN_SERVER_NAME)) {
			String keytab = EmbeddedServerUtil.getConfig(ADMIN_USER_KEYTAB);
			String principal = null;
			try {
				principal = SecureClientLogin.getPrincipal(EmbeddedServerUtil.getConfig(ADMIN_USER_PRINCIPAL), hostName);
			} catch (IOException ignored) {
				LOG.warning("Failed to get ranger.admin.kerberos.principal. Reason: " + ignored.toString());
			}
			String nameRules = EmbeddedServerUtil.getConfig(ADMIN_NAME_RULES);
			if (StringUtils.isBlank(nameRules)) {
				LOG.info("Name is empty. Setting Name Rule as 'DEFAULT'");
				nameRules = DEFAULT_NAME_RULE;
			}
			if (EmbeddedServerUtil.getConfig(AUTHENTICATION_TYPE) != null
					&& EmbeddedServerUtil.getConfig(AUTHENTICATION_TYPE).trim().equalsIgnoreCase(AUTH_TYPE_KERBEROS)
					&& SecureClientLogin.isKerberosCredentialExists(principal,keytab)) {
				try{
					LOG.info("Provided Kerberos Credential : Principal = "
							+ principal + " and Keytab = " + keytab);
					Subject sub = SecureClientLogin.loginUserFromKeytab(principal, keytab, nameRules);
					Subject.doAs(sub, new PrivilegedAction<Void>() {
						@Override
						public Void run() {
							LOG.info("Starting Server using kerberos credential");
							startServer(server);
							return null;
						}
					});
				} catch (Exception e) {
					LOG.severe("Tomcat Server failed to start:" + e.toString());
					e.printStackTrace();
				}
			} else {
				startServer(server);
			}
		} else {
			startServer(server);
		}
	}

	private void startServer(final Tomcat server) {
		try {
			String servername = EmbeddedServerUtil.getConfig("servername");
			LOG.info("Server Name : " + servername);
			if (servername.equalsIgnoreCase(ADMIN_SERVER_NAME)) {
				String auditSourceType = EmbeddedServerUtil.getConfig(AUDIT_SOURCE_TYPE, "db");
				if (AUDIT_SOURCE_SOLR.equalsIgnoreCase(auditSourceType)) {
					boolean solrBootstrapEnabled = Boolean.valueOf(EmbeddedServerUtil.getConfig(SOLR_BOOTSTRAP_ENABLED, "true"));
					if (solrBootstrapEnabled) {
						try {
							SolrCollectionBootstrapper solrSetup = new SolrCollectionBootstrapper();
							solrSetup.start();
						} catch (Exception e) {
							LOG.severe("Error while setting solr " + e);
						}
					}
				} else if (AUDIT_SOURCE_ES.equalsIgnoreCase(auditSourceType)) {
					boolean esBootstrapEnabled = Boolean.valueOf(EmbeddedServerUtil.getConfig(ES_BOOTSTRAP_ENABLED, "true"));
					if (esBootstrapEnabled) {
						try {
							ElasticSearchIndexBootStrapper esSchemaSetup = new ElasticSearchIndexBootStrapper();
							esSchemaSetup.start();
						} catch (Exception e) {
							LOG.severe("Error while setting elasticsearch " + e);
						}
					}
				}
			}
			server.start();
			server.getServer().await();
			shutdownServer();
		} catch (LifecycleException e) {
			LOG.severe("Tomcat Server failed to start:" + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			LOG.severe("Tomcat Server failed to start:" + e.toString());
			e.printStackTrace();
		}
	}

	private String getKeystoreFile() {
		String keystoreFile = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.keystore.file");
		if (StringUtils.isBlank(keystoreFile)) {
			// new property not configured, lets use the old property
			keystoreFile = EmbeddedServerUtil.getConfig("ranger.https.attrib.keystore.file");
		}
		return keystoreFile;
	}

	public void shutdownServer() {
		int timeWaitForShutdownInSeconds = EmbeddedServerUtil.getIntConfig(
				"service.waitTimeForForceShutdownInSeconds", 0);
		if (timeWaitForShutdownInSeconds > 0) {
			long endTime = System.currentTimeMillis()
					+ (timeWaitForShutdownInSeconds * 1000L);
			LOG.info("Will wait for all threads to shutdown gracefully. Final shutdown Time: "
					+ new Date(endTime));
			while (System.currentTimeMillis() < endTime) {
				int activeCount = Thread.activeCount();
				if (activeCount == 0) {
				    LOG.info("Number of active threads = " + activeCount + ".");
					break;
				} else {
					LOG.info("Number of active threads = " + activeCount
							+ ". Waiting for all threads to shutdown ...");
					try {
						Thread.sleep(5000L);
					} catch (InterruptedException e) {
						LOG.warning("shutdownServer process is interrupted with exception: "
								+ e);
						break;
					}
				}
			}
		}
		LOG.info("Shuting down the Server.");
		System.exit(0);
	}

	public void updateHttpConnectorAttribConfig(Tomcat server) {
		server.getConnector().setAllowTrace(Boolean.valueOf(EmbeddedServerUtil.getConfig("ranger.service.http.connector.attrib.allowTrace", "false")));
		server.getConnector().setAsyncTimeout(EmbeddedServerUtil.getLongConfig("ranger.service.http.connector.attrib.asyncTimeout", 10000L));
		server.getConnector().setEnableLookups(Boolean.valueOf(EmbeddedServerUtil.getConfig("ranger.service.http.connector.attrib.enableLookups", "false")));
		server.getConnector().setMaxParameterCount(EmbeddedServerUtil.getIntConfig("ranger.service.http.connector.attrib.maxParameterCount", 10000));
		server.getConnector().setMaxPostSize(EmbeddedServerUtil.getIntConfig("ranger.service.http.connector.attrib.maxPostSize", 2097152));
		server.getConnector().setMaxSavePostSize(EmbeddedServerUtil.getIntConfig("ranger.service.http.connector.attrib.maxSavePostSize", 4096));
		server.getConnector().setParseBodyMethods(EmbeddedServerUtil.getConfig("ranger.service.http.connector.attrib.methods", "POST"));
		server.getConnector().setURIEncoding(EmbeddedServerUtil.getConfig("ranger.service.http.connector.attrib.URIEncoding", "UTF-8"));
		server.getConnector().setXpoweredBy(false);
		server.getConnector().setAttribute("server", "Apache Ranger");
		server.getConnector().setProperty("sendReasonPhrase",EmbeddedServerUtil.getConfig("ranger.service.http.connector.property.sendReasonPhrase", "true"));
		Iterator<Object> iterator = EmbeddedServerUtil.getRangerConfigProperties().keySet().iterator();
		String key = null;
		String property = null;
		while (iterator.hasNext()){
			key = iterator.next().toString();
			if(key != null && key.startsWith("ranger.service.http.connector.property.")){
				property = key.replace("ranger.service.http.connector.property.","");
				server.getConnector().setProperty(property, EmbeddedServerUtil.getConfig(key));
				LOG.info(property + ":" + server.getConnector().getProperty(property));
			}
		}
	}

	private SSLContext getSSLContext() {
		KeyManager[] kmList = getKeyManagers();
		TrustManager[] tmList = getTrustManagers();
		SSLContext sslContext = null;
		if (tmList != null) {
			try {
				sslContext = SSLContext.getInstance(RANGER_SSL_CONTEXT_ALGO_TYPE);
				sslContext.init(kmList, tmList, new SecureRandom());
			} catch (NoSuchAlgorithmException e) {
				LOG.severe("SSL algorithm is not available in the environment. Reason: " + e.toString());
			} catch (KeyManagementException e) {
				LOG.severe("Unable to initials the SSLContext. Reason: " + e.toString());
			}
		}
		return sslContext;
	}

	private KeyManager[] getKeyManagers() {
		KeyManager[] kmList = null;
		String keyStoreFile = EmbeddedServerUtil.getConfig("ranger.keystore.file");
		String keyStoreAlias = EmbeddedServerUtil.getConfig("ranger.keystore.alias", "keyStoreCredentialAlias");
		if (StringUtils.isBlank(keyStoreFile)) {
			keyStoreFile = getKeystoreFile();
			keyStoreAlias = EmbeddedServerUtil.getConfig("ranger.service.https.attrib.keystore.credential.alias", "keyStoreCredentialAlias");
		}
		String keyStoreFileType = EmbeddedServerUtil.getConfig("ranger.keystore.file.type",RANGER_KEYSTORE_FILE_TYPE_DEFAULT);
		String credentialProviderPath = EmbeddedServerUtil.getConfig("ranger.credential.provider.path");
		String keyStoreFilepwd = CredentialReader.getDecryptedString(credentialProviderPath, keyStoreAlias, keyStoreFileType);

		if (StringUtils.isNotEmpty(keyStoreFile) && StringUtils.isNotEmpty(keyStoreFilepwd)) {
			InputStream in = null;

			try {
				in = getFileInputStream(keyStoreFile);

				if (in != null) {
					KeyStore keyStore = KeyStore.getInstance(keyStoreFileType);

					keyStore.load(in, keyStoreFilepwd.toCharArray());

					KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

					keyManagerFactory.init(keyStore, keyStoreFilepwd.toCharArray());

					kmList = keyManagerFactory.getKeyManagers();
				} else {
					LOG.severe("Unable to obtain keystore from file [" + keyStoreFile + "]");
				}
			} catch (KeyStoreException e) {
				LOG.log(Level.SEVERE, "Unable to obtain from KeyStore :" + e.getMessage(), e);
			} catch (NoSuchAlgorithmException e) {
				LOG.log(Level.SEVERE, "SSL algorithm is NOT available in the environment", e);
			} catch (CertificateException e) {
				LOG.log(Level.SEVERE, "Unable to obtain the requested certification ", e);
			} catch (FileNotFoundException e) {
				LOG.log(Level.SEVERE, "Unable to find the necessary SSL Keystore Files", e);
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Unable to read the necessary SSL Keystore Files", e);
			} catch (UnrecoverableKeyException e) {
				LOG.log(Level.SEVERE, "Unable to recover the key from keystore", e);
			} finally {
				close(in, keyStoreFile);
			}
		} else {
			if (StringUtils.isBlank(keyStoreFile)) {
				LOG.warning("Config 'ranger.keystore.file' or 'ranger.service.https.attrib.keystore.file' is not found or contains blank value");
			} else if (StringUtils.isBlank(keyStoreAlias)) {
				LOG.warning("Config 'ranger.keystore.alias' or 'ranger.service.https.attrib.keystore.credential.alias' is not found or contains blank value");
			} else if (StringUtils.isBlank(credentialProviderPath)) {
				LOG.warning("Config 'ranger.credential.provider.path' is not found or contains blank value");
			} else if (StringUtils.isBlank(keyStoreFilepwd)) {
				LOG.warning("Unable to read credential from credential store file ["+ credentialProviderPath + "] for given alias:"+keyStoreAlias);
			}
		}
		return kmList;
	}

	private TrustManager[] getTrustManagers() {
		TrustManager[] tmList = null;
		String truststoreFile = EmbeddedServerUtil.getConfig("ranger.truststore.file");
		String truststoreAlias = EmbeddedServerUtil.getConfig("ranger.truststore.alias");
		String credentialProviderPath = EmbeddedServerUtil.getConfig("ranger.credential.provider.path");
		String truststoreFileType = EmbeddedServerUtil.getConfig("ranger.truststore.file.type",RANGER_TRUSTSTORE_FILE_TYPE_DEFAULT);
		String trustStoreFilepwd = CredentialReader.getDecryptedString(credentialProviderPath, truststoreAlias, truststoreFileType);

		if (StringUtils.isNotEmpty(truststoreFile) && StringUtils.isNotEmpty(trustStoreFilepwd)) {
			InputStream in = null;

			try {
				in = getFileInputStream(truststoreFile);

				if (in != null) {
					KeyStore trustStore = KeyStore.getInstance(truststoreFileType);

					trustStore.load(in, trustStoreFilepwd.toCharArray());

					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(RANGER_SSL_TRUSTMANAGER_ALGO_TYPE);

					trustManagerFactory.init(trustStore);

					tmList = trustManagerFactory.getTrustManagers();
				} else {
					LOG.log(Level.SEVERE, "Unable to obtain truststore from file [" + truststoreFile + "]");
				}
			} catch (KeyStoreException e) {
				LOG.log(Level.SEVERE, "Unable to obtain from KeyStore", e);
			} catch (NoSuchAlgorithmException e) {
				LOG.log(Level.SEVERE, "SSL algorithm is NOT available in the environment :" + e.getMessage(), e);
			} catch (CertificateException e) {
				LOG.log(Level.SEVERE, "Unable to obtain the requested certification :" + e.getMessage(), e);
			} catch (FileNotFoundException e) {
				LOG.log(Level.SEVERE, "Unable to find the necessary SSL TrustStore File:" + truststoreFile, e);
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Unable to read the necessary SSL TrustStore Files :" + truststoreFile, e);
			} finally {
				close(in, truststoreFile);
			}
		} else {
			if (StringUtils.isBlank(truststoreFile)) {
				LOG.warning("Config 'ranger.truststore.file' is not found or contains blank value!");
			} else if (StringUtils.isBlank(truststoreAlias)) {
				LOG.warning("Config 'ranger.truststore.alias' is not found or contains blank value!");
			} else if (StringUtils.isBlank(credentialProviderPath)) {
				LOG.warning("Config 'ranger.credential.provider.path' is not found or contains blank value!");
			} else if (StringUtils.isBlank(trustStoreFilepwd)) {
				LOG.warning("Unable to read credential from credential store file ["+ credentialProviderPath + "] for given alias:"+truststoreAlias);
			}
		}

		return tmList;
	}

	private InputStream getFileInputStream(String fileName) throws IOException {
		InputStream in = null;
		if (StringUtils.isNotEmpty(fileName)) {
			File f = new File(fileName);
			if (f.exists()) {
				in = new FileInputStream(f);
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
				LOG.log(Level.SEVERE, "Error while closing file: [" + filename + "]", excp);
			}
		}
	}

}
