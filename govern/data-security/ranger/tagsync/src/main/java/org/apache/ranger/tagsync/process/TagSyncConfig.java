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

package org.apache.ranger.tagsync.process;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.SecureClientLogin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.ranger.credentialapi.CredentialReader;
import org.apache.ranger.plugin.util.RangerCommonConstants;

public class TagSyncConfig extends Configuration {
	private static final Logger LOG = LoggerFactory.getLogger(TagSyncConfig.class);

	private static final String CONFIG_FILE = "ranger-tagsync-site.xml";

	private static final String DEFAULT_CONFIG_FILE = "ranger-tagsync-default.xml";

	private static final String CORE_SITE_FILE = "core-site.xml";

	public static final String TAGSYNC_ENABLED_PROP = "ranger.tagsync.enabled";

	public static final String TAGSYNC_LOGDIR_PROP = "ranger.tagsync.logdir";

	private static final String TAGSYNC_TAGADMIN_REST_URL_PROP = "ranger.tagsync.dest.ranger.endpoint";

	private static final String TAGSYNC_TAGADMIN_REST_SSL_CONFIG_FILE_PROP = "ranger.tagsync.dest.ranger.ssl.config.filename";

	private static final String TAGSYNC_SINK_CLASS_PROP = "ranger.tagsync.dest.ranger.impl.class";

	private static final String TAGSYNC_DEST_RANGER_PASSWORD_ALIAS = "tagadmin.user.password";
	private static final String TAGSYNC_SOURCE_ATLASREST_PASSWORD_ALIAS = "atlas.user.password";

	private static final String TAGSYNC_TAGADMIN_USERNAME_PROP = "ranger.tagsync.dest.ranger.username";
	private static final String TAGSYNC_ATLASREST_USERNAME_PROP = "ranger.tagsync.source.atlasrest.username";

	private static final String TAGSYNC_TAGADMIN_PASSWORD_PROP = "ranger.tagsync.dest.ranger.password";
	private static final String TAGSYNC_ATLASREST_PASSWORD_PROP = "ranger.tagsync.source.atlasrest.password";

	private static final String TAGSYNC_TAGADMIN_CONNECTION_CHECK_INTERVAL_PROP = "ranger.tagsync.dest.ranger.connection.check.interval";

	private static final String TAGSYNC_SOURCE_ATLAS_CUSTOM_RESOURCE_MAPPERS_PROP = "ranger.tagsync.atlas.custom.resource.mappers";

	private static final String TAGSYNC_ATLASSOURCE_ENDPOINT_PROP = "ranger.tagsync.source.atlasrest.endpoint";

	private static final String TAGSYNC_ATLAS_REST_SOURCE_DOWNLOAD_INTERVAL_PROP = "ranger.tagsync.source.atlasrest.download.interval.millis";

	private static final String TAGSYNC_ATLAS_REST_SSL_CONFIG_FILE_PROP = "ranger.tagsync.source.atlasrest.ssl.config.filename";

	public static final String TAGSYNC_FILESOURCE_FILENAME_PROP = "ranger.tagsync.source.file.filename";

	private static final String TAGSYNC_FILESOURCE_MOD_TIME_CHECK_INTERVAL_PROP = "ranger.tagsync.source.file.check.interval.millis";

	private static final String TAGSYNC_KEYSTORE_TYPE_PROP = "ranger.keystore.file.type";
	private static final String TAGSYNC_TAGADMIN_KEYSTORE_PROP = "ranger.tagsync.keystore.filename";
	private static final String TAGSYNC_ATLASREST_KEYSTORE_PROP = "ranger.tagsync.source.atlasrest.keystore.filename";

	private static final String TAGSYNC_SOURCE_RETRY_INITIALIZATION_INTERVAL_PROP = "ranger.tagsync.source.retry.initialization.interval.millis";

	public static final String TAGSYNC_RANGER_COOKIE_ENABLED_PROP = "ranger.tagsync.cookie.enabled";
	public static final String TAGSYNC_TAGADMIN_COOKIE_NAME_PROP = "ranger.tagsync.dest.ranger.session.cookie.name";

	private static final String DEFAULT_TAGADMIN_USERNAME = "rangertagsync";
	private static final String DEFAULT_ATLASREST_USERNAME = "admin";
	private static final String DEFAULT_ATLASREST_PASSWORD = "admin";

	private static final int DEFAULT_TAGSYNC_TAGADMIN_CONNECTION_CHECK_INTERVAL = 15000;
	private static final long DEFAULT_TAGSYNC_ATLASREST_SOURCE_DOWNLOAD_INTERVAL = 900000;
	public  static final int  DEFAULT_TAGSYNC_ATLASREST_SOURCE_ENTITIES_BATCH_SIZE = 10000;
	private static final long DEFAULT_TAGSYNC_FILESOURCE_MOD_TIME_CHECK_INTERVAL = 60000;
	private static final long DEFAULT_TAGSYNC_SOURCE_RETRY_INITIALIZATION_INTERVAL = 10000;

	private static final String AUTH_TYPE = "hadoop.security.authentication";
	private static final String NAME_RULES = "hadoop.security.auth_to_local";
	private static final String TAGSYNC_KERBEROS_PRICIPAL = "ranger.tagsync.kerberos.principal";
	private static final String TAGSYNC_KERBEROS_KEYTAB = "ranger.tagsync.kerberos.keytab";

	public static final String TAGSYNC_KERBEROS_IDENTITY = "tagsync.kerberos.identity";

	private static String LOCAL_HOSTNAME = "unknown";

    private static final String  TAGSYNC_METRICS_FILEPATH =   "ranger.tagsync.metrics.filepath";
    private static final String  DEFAULT_TAGSYNC_METRICS_FILEPATH =   "/tmp/";
    private static final String  TAGSYNC_METRICS_FILENAME =   "ranger.tagsync.metrics.filename";
    private static final String  DEFAULT_TAGSYNC_METRICS_FILENAME =   "ranger_tagsync_metric.json";
    private static final String  TAGSYNC_METRICS_FREQUENCY_TIME_IN_MILLIS_PARAM = "ranger.tagsync.metrics.frequencytimeinmillis";
    private static final long    DEFAULT_TAGSYNC_METRICS_FREQUENCY__TIME_IN_MILLIS = 10000L;
    private static final String  TAGSYNC_METRICS_ENABLED_PROP = "ranger.tagsync.metrics.enabled";

	private static final int     DEFAULT_TAGSYNC_SINK_MAX_BATCH_SIZE = 1;
	private static final String  TAGSYNC_SINK_MAX_BATCH_SIZE_PROP    = "ranger.tagsync.dest.ranger.max.batch.size";

	private static final String TAGSYNC_ATLASREST_SOURCE_ENTITIES_BATCH_SIZE = "ranger.tagsync.source.atlasrest.entities.batch.size";


	private Properties props;

	static {
		try {
			LOCAL_HOSTNAME = java.net.InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			LOCAL_HOSTNAME = "unknown";
		}
	}
	
	public static TagSyncConfig getInstance() {
		return new TagSyncConfig();
	}

	public Properties getProperties() {
		return props;
	}

	public static InputStream getFileInputStream(String path) throws FileNotFoundException {

		InputStream ret = null;

		File f = new File(path);

		if (f.exists() && f.isFile() && f.canRead()) {
			ret = new FileInputStream(f);
		} else {
			ret = TagSyncConfig.class.getResourceAsStream(path);

			if (ret == null) {
				if (! path.startsWith("/")) {
					ret = TagSyncConfig.class.getResourceAsStream("/" + path);
				}
			}

			if (ret == null) {
				ret = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
				if (ret == null) {
					if (! path.startsWith("/")) {
						ret = ClassLoader.getSystemResourceAsStream("/" + path);
					}
				}
			}
		}

		return ret;
	}

	public static String getResourceFileName(String path) {

		String ret = null;

		if (StringUtils.isNotBlank(path)) {

			File f = new File(path);

			if (f.exists() && f.isFile() && f.canRead()) {
				ret = path;
			} else {

				URL fileURL = TagSyncConfig.class.getResource(path);
				if (fileURL == null) {
					if (!path.startsWith("/")) {
						fileURL = TagSyncConfig.class.getResource("/" + path);
					}
				}

				if (fileURL == null) {
					fileURL = ClassLoader.getSystemClassLoader().getResource(path);
					if (fileURL == null) {
						if (!path.startsWith("/")) {
							fileURL = ClassLoader.getSystemClassLoader().getResource("/" + path);
						}
					}
				}

				if (fileURL != null) {
					try {
						ret = fileURL.getFile();
					} catch (Exception exception) {
						LOG.error(path + " is not a file", exception);
					}
				} else {
					LOG.warn("URL not found for " + path + " or no privilege for reading file " + path);
				}
			}
		}

		return ret;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("DEFAULT_CONFIG_FILE=").append(DEFAULT_CONFIG_FILE).append(", ")
				.append("CONFIG_FILE=").append(CONFIG_FILE).append("\n\n");

		return sb.toString() + super.toString();
	}

	static public String getTagsyncKeyStoreType(Properties prop) {
		return prop.getProperty(TAGSYNC_KEYSTORE_TYPE_PROP);
	}

	static public boolean isTagSyncRangerCookieEnabled(Properties prop) {
		String val = prop.getProperty(TAGSYNC_RANGER_COOKIE_ENABLED_PROP);
		return val == null || Boolean.valueOf(val.trim());
	}

	static public String getRangerAdminCookieName(Properties prop) {
		String ret = RangerCommonConstants.DEFAULT_COOKIE_NAME;
		String val = prop.getProperty(TAGSYNC_TAGADMIN_COOKIE_NAME_PROP);
		if (StringUtils.isNotBlank(val)) {
			ret = val;
		}
		return ret;
	}

	static public String getTagSyncLogdir(Properties prop) {
		return prop.getProperty(TAGSYNC_LOGDIR_PROP);
	}

	static public long getTagSourceFileModTimeCheckIntervalInMillis(Properties prop) {
		String val = prop.getProperty(TAGSYNC_FILESOURCE_MOD_TIME_CHECK_INTERVAL_PROP);
		long ret = DEFAULT_TAGSYNC_FILESOURCE_MOD_TIME_CHECK_INTERVAL;
		if (StringUtils.isNotBlank(val)) {
			try {
				ret = Long.valueOf(val);
			} catch (NumberFormatException exception) {
				// Ignore
			}
		}
		return ret;
	}

	static public long getTagSourceAtlasDownloadIntervalInMillis(Properties prop) {
		String val = prop.getProperty(TAGSYNC_ATLAS_REST_SOURCE_DOWNLOAD_INTERVAL_PROP);
		long ret = DEFAULT_TAGSYNC_ATLASREST_SOURCE_DOWNLOAD_INTERVAL;
		if (StringUtils.isNotBlank(val)) {
			try {
				ret = Long.valueOf(val);
			} catch (NumberFormatException exception) {
				// Ignore
			}
		}
		return ret;
	}

	static public String getTagSinkClassName(Properties prop) {
		String val = prop.getProperty(TAGSYNC_SINK_CLASS_PROP);
		if (StringUtils.equalsIgnoreCase(val, "ranger")) {
			return "org.apache.ranger.tagsync.sink.tagadmin.TagAdminRESTSink";
		} else
			return val;
	}

	static public String getTagAdminRESTUrl(Properties prop) {
		return prop.getProperty(TAGSYNC_TAGADMIN_REST_URL_PROP);
	}

	static public boolean isTagSyncEnabled(Properties prop) {
		String val = prop.getProperty(TAGSYNC_ENABLED_PROP);
		return val == null || Boolean.valueOf(val.trim());
	}

	static public String getTagAdminPassword(Properties prop) {
		//update credential from keystore
		String password = null;
		if (prop != null && prop.containsKey(TAGSYNC_TAGADMIN_PASSWORD_PROP)) {
			password = prop.getProperty(TAGSYNC_TAGADMIN_PASSWORD_PROP);
			if (password != null && !password.isEmpty()) {
				return password;
			}
		}
		if (prop != null && prop.containsKey(TAGSYNC_TAGADMIN_KEYSTORE_PROP)) {
			String path = prop.getProperty(TAGSYNC_TAGADMIN_KEYSTORE_PROP);
			if (path != null) {
				if (!path.trim().isEmpty()) {
					try {
						password = CredentialReader.getDecryptedString(path.trim(), TAGSYNC_DEST_RANGER_PASSWORD_ALIAS, getTagsyncKeyStoreType(prop));
					} catch (Exception ex) {
						password = null;
					}
					if (password != null && !password.trim().isEmpty() && !password.trim().equalsIgnoreCase("none")) {
						return password;
					}
				}
			}
		}
		return null;
	}

	static public String getTagAdminUserName(Properties prop) {
		String userName=null;
		if(prop!=null && prop.containsKey(TAGSYNC_TAGADMIN_USERNAME_PROP)){
			userName=prop.getProperty(TAGSYNC_TAGADMIN_USERNAME_PROP);
		}
		if(StringUtils.isBlank(userName)){
			userName=DEFAULT_TAGADMIN_USERNAME;
		}
		return userName;
	}

	static public String getTagAdminRESTSslConfigFile(Properties prop) {
		return prop.getProperty(TAGSYNC_TAGADMIN_REST_SSL_CONFIG_FILE_PROP);
	}

	static public String getTagSourceFileName(Properties prop) {
		return prop.getProperty(TAGSYNC_FILESOURCE_FILENAME_PROP);
	}

	static public String getAtlasRESTEndpoint(Properties prop) {
		return prop.getProperty(TAGSYNC_ATLASSOURCE_ENDPOINT_PROP);
	}

	static public String getAtlasRESTPassword(Properties prop) {
		//update credential from keystore
		String password = null;
		if (prop != null && prop.containsKey(TAGSYNC_ATLASREST_PASSWORD_PROP)) {
			password = prop.getProperty(TAGSYNC_ATLASREST_PASSWORD_PROP);
			if (password != null && !password.isEmpty()) {
				return password;
			}
		}
		if (prop != null && prop.containsKey(TAGSYNC_ATLASREST_KEYSTORE_PROP)) {
			String path = prop.getProperty(TAGSYNC_ATLASREST_KEYSTORE_PROP);
			if (path != null) {
				if (!path.trim().isEmpty()) {
					try {
						password = CredentialReader.getDecryptedString(path.trim(), TAGSYNC_SOURCE_ATLASREST_PASSWORD_ALIAS, getTagsyncKeyStoreType(prop));
					} catch (Exception ex) {
						password = null;
					}
					if (password != null && !password.trim().isEmpty() && !password.trim().equalsIgnoreCase("none")) {
						return password;
					}
				}
			}
		}
		if(StringUtils.isBlank(password)){
			return DEFAULT_ATLASREST_PASSWORD;
		}
		return null;
	}

	static public String getAtlasRESTUserName(Properties prop) {
		String userName=null;
		if(prop!=null && prop.containsKey(TAGSYNC_ATLASREST_USERNAME_PROP)){
			userName=prop.getProperty(TAGSYNC_ATLASREST_USERNAME_PROP);
		}
		if(StringUtils.isBlank(userName)){
			userName=DEFAULT_ATLASREST_USERNAME;
		}
		return userName;
	}

	static public String getAtlasRESTSslConfigFile(Properties prop) {
		return prop.getProperty(TAGSYNC_ATLAS_REST_SSL_CONFIG_FILE_PROP);
	}

	static public String getCustomAtlasResourceMappers(Properties prop) {
		return prop.getProperty(TAGSYNC_SOURCE_ATLAS_CUSTOM_RESOURCE_MAPPERS_PROP);
	}
	
	static public String getAuthenticationType(Properties prop){
		return prop.getProperty(AUTH_TYPE, "simple");
	}
	
	static public String getNameRules(Properties prop){
		return prop.getProperty(NAME_RULES, "DEFAULT");
	}
	
	static public String getKerberosPrincipal(Properties prop){
//		return prop.getProperty(TAGSYNC_KERBEROS_PRICIPAL);
		String principal = null;
		try {
			principal = SecureClientLogin.getPrincipal(prop.getProperty(TAGSYNC_KERBEROS_PRICIPAL, ""), LOCAL_HOSTNAME);
		} catch (IOException ignored) {
			 // do nothing
		}
		return principal;
	}
	
	static public String getKerberosKeytab(Properties prop){
		return prop.getProperty(TAGSYNC_KERBEROS_KEYTAB, "");
	}

	static public long getTagAdminConnectionCheckInterval(Properties prop) {
		long ret = DEFAULT_TAGSYNC_TAGADMIN_CONNECTION_CHECK_INTERVAL;
		String val = prop.getProperty(TAGSYNC_TAGADMIN_CONNECTION_CHECK_INTERVAL_PROP);
		if (StringUtils.isNotBlank(val)) {
			try {
				ret = Long.valueOf(val);
			} catch (NumberFormatException exception) {
				// Ignore
			}
		}
		return ret;
	}

	static public long getTagSourceRetryInitializationInterval(Properties prop) {
		long ret = DEFAULT_TAGSYNC_SOURCE_RETRY_INITIALIZATION_INTERVAL;
		String val = prop.getProperty(TAGSYNC_SOURCE_RETRY_INITIALIZATION_INTERVAL_PROP);
		if (StringUtils.isNotBlank(val)) {
			try {
				ret = Long.valueOf(val);
			} catch (NumberFormatException exception) {
				// Ignore
			}
		}
		return ret;
	}

	static public String getTagsyncKerberosIdentity(Properties prop) {
		return prop.getProperty(TAGSYNC_KERBEROS_IDENTITY);
	}

	public static int getSinkMaxBatchSize(Properties prop) {
		int ret = DEFAULT_TAGSYNC_SINK_MAX_BATCH_SIZE;

		String maxBatchSizeStr = prop.getProperty(TAGSYNC_SINK_MAX_BATCH_SIZE_PROP);

		if (StringUtils.isNotEmpty(maxBatchSizeStr)) {
			try {
				ret = Integer.valueOf(maxBatchSizeStr);
			} catch (Exception e) {
			}
		}
		return ret;
	}

	private TagSyncConfig() {
		super(false);
		init();
	}

	private void init() {

		readConfigFile(CORE_SITE_FILE);
		readConfigFile(DEFAULT_CONFIG_FILE);
		readConfigFile(CONFIG_FILE);

		props = getProps();

		@SuppressWarnings("unchecked")
		Enumeration<String> propertyNames = (Enumeration<String>)props.propertyNames();

		while (propertyNames.hasMoreElements()) {
			String propertyName = propertyNames.nextElement();
			String systemPropertyValue = System.getProperty(propertyName);
			if (systemPropertyValue != null) {
				props.setProperty(propertyName, systemPropertyValue);
			}
		}

	}

	private void readConfigFile(String fileName) {

		if (StringUtils.isNotBlank(fileName)) {
			String fName = getResourceFileName(fileName);
			if (StringUtils.isBlank(fName)) {
				LOG.warn("Cannot find configuration file " + fileName + " in the classpath");
			} else {
				LOG.info("Loading configuration from " + fName);
				addResource(fileName);
			}
		} else {
			LOG.error("Configuration fileName is null");
		}
	}

	public String getTagSyncMetricsFileName() {
		String val = getProperties().getProperty(TAGSYNC_METRICS_FILEPATH);
		if (StringUtils.isBlank(val)) {
			if (StringUtils.isBlank(System.getProperty("logdir"))) {
				val = DEFAULT_TAGSYNC_METRICS_FILEPATH;
			} else {
				val = System.getProperty("logdir");
			}
		}

		if (Files.notExists(Paths.get(val))) {
				return null;
		}

		StringBuilder pathAndFileName = new StringBuilder(val);
		if (!val.endsWith("/")) {
			pathAndFileName.append("/");
		}
		String fileName = getProperties().getProperty(TAGSYNC_METRICS_FILENAME);
		if (StringUtils.isBlank(fileName)) {
			fileName = DEFAULT_TAGSYNC_METRICS_FILENAME;
		}
		pathAndFileName.append(fileName);
		return pathAndFileName.toString();
	}

	public long getTagSyncMetricsFrequency() {
		long ret = DEFAULT_TAGSYNC_METRICS_FREQUENCY__TIME_IN_MILLIS;
		String val = getProperties().getProperty(TAGSYNC_METRICS_FREQUENCY_TIME_IN_MILLIS_PARAM);
		if (StringUtils.isNotBlank(val)) {
			try {
				ret = Long.valueOf(val);
			} catch (NumberFormatException exception) {
				// Ignore
			}
		}
		return ret;
	}

	public static boolean isTagSyncMetricsEnabled(Properties prop) {
		String val = prop.getProperty(TAGSYNC_METRICS_ENABLED_PROP);
		return "true".equalsIgnoreCase(StringUtils.trimToEmpty(val));
	}

	static public int getAtlasRestSourceEntitiesBatchSize(Properties prop) {
		String val = prop.getProperty(TAGSYNC_ATLASREST_SOURCE_ENTITIES_BATCH_SIZE);
		int    ret = DEFAULT_TAGSYNC_ATLASREST_SOURCE_ENTITIES_BATCH_SIZE;

		if (StringUtils.isNotBlank(val)) {
			try {
				ret = Integer.valueOf(val);
			} catch (NumberFormatException exception) {
				// Ignore
			}
		}

		return ret;
	}
}
