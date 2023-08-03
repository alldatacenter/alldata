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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.SecureClientLogin;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.ranger.tagsync.model.TagSink;
import org.apache.ranger.tagsync.model.TagSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagSynchronizer {

	private static final Logger LOG = LoggerFactory.getLogger(TagSynchronizer.class);

	private static final String AUTH_TYPE_KERBEROS = "kerberos";

	private static final String TAGSYNC_SOURCE_BASE = "ranger.tagsync.source.";
	private static final String PROP_CLASS_NAME = "class";

	private TagSink tagSink = null;
	private List<TagSource> tagSources = new ArrayList<TagSource>();
	private List<TagSource> failedTagSources = new ArrayList<TagSource>();
	private Properties properties = null;

	private final Object shutdownNotifier = new Object();
	private volatile boolean isShutdownInProgress = false;

	public static void main(String[] args) {
		TagSynchronizer tagSynchronizer = new TagSynchronizer();

		TagSyncConfig config = TagSyncConfig.getInstance();

		Properties props = config.getProperties();

		tagSynchronizer.setProperties(props);

		boolean tagSynchronizerInitialized = tagSynchronizer.initialize();

		if (tagSynchronizerInitialized) {
			try {
				tagSynchronizer.run();
			} catch (Throwable t) {
				LOG.error("main thread caught exception..:", t);
				System.exit(1);
			}
		} else {
			LOG.error("TagSynchronizer failed to initialize correctly, exiting..");
			System.exit(1);
		}

	}

	TagSynchronizer() {
		this(null);
	}

	TagSynchronizer(Properties properties) {
		setProperties(properties);
	}

	void setProperties(Properties properties) {
		if (properties == null || MapUtils.isEmpty(properties)) {
			this.properties = new Properties();
		} else {
			this.properties = properties;
		}
	}

	public boolean initialize() {

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagSynchronizer.initialize()");
		}

		printConfigurationProperties(properties);

		boolean ret = initializeKerberosIdentity(properties);

		if (ret) {
			LOG.info("Initializing TAG source and sink");
			ret = false;
			tagSink = initializeTagSink(properties);

			if (tagSink != null) {
				initializeTagSources();
				ret = true;
			}
		} else {
			LOG.error("Error initializing kerberos identity");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagSynchronizer.initialize() : " + ret);
		}

		return ret;
	}

	public void run() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagSynchronizer.run()");
		}

		isShutdownInProgress = false;

		try {
			boolean threadsStarted = tagSink.start();

			if (LOG.isDebugEnabled()) {
				LOG.debug("==> starting TagSyncMetricsProducer with default metrics location : "+System.getProperty("logdir"));
			}
			//Start the tag sync metrics
			boolean isTagSyncMetricsEnabled = TagSyncConfig.isTagSyncMetricsEnabled(properties);
			if (isTagSyncMetricsEnabled) {
				TagSyncMetricsProducer tagSyncMetricsProducer = new TagSyncMetricsProducer();
				Thread tagSyncMetricsProducerThread = new Thread(tagSyncMetricsProducer);
				tagSyncMetricsProducerThread.setName("TagSyncMetricsProducerThread");
				tagSyncMetricsProducerThread.setDaemon(true);
				tagSyncMetricsProducerThread.start();
			} else {
				LOG.info(" Ranger tagsync metrics is not enabled");
			}
			for (TagSource tagSource : tagSources) {
				threadsStarted = threadsStarted && tagSource.start();
			}

			if (threadsStarted) {
				long tagSourceRetryInitializationInterval = TagSyncConfig.getTagSourceRetryInitializationInterval(properties);

				synchronized(shutdownNotifier) {
					while(! isShutdownInProgress) {
						shutdownNotifier.wait(tagSourceRetryInitializationInterval);
						if (CollectionUtils.isNotEmpty(failedTagSources)) {
							reInitializeFailedTagSources();
						}
					}
				}
			}
		} finally {
			LOG.info("Stopping all tagSources");

			for (TagSource tagSource : tagSources) {
				tagSource.stop();
			}

			LOG.info("Stopping tagSink");
			tagSink.stop();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagSynchronizer.run()");
		}
	}

	public void shutdown(String reason) {
		LOG.info("Received shutdown(), reason=" + reason);

		synchronized(shutdownNotifier) {
			isShutdownInProgress = true;
			shutdownNotifier.notifyAll();
		}
	}

	static public void printConfigurationProperties(Properties properties) {
		LOG.info("--------------------------------");
		LOG.info("");
		LOG.info("Ranger-TagSync Configuration: {\n");
		if (MapUtils.isNotEmpty(properties)) {
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				LOG.info("\tProperty-Name:" + entry.getKey());
				LOG.info("\tProperty-Value:" + entry.getValue());
				LOG.info("\n");
			}
		}
		LOG.info("\n}");
		LOG.info("");
		LOG.info("--------------------------------");
	}

	static public TagSink initializeTagSink(Properties properties) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagSynchronizer.initializeTagSink()");
		}
		TagSink ret = null;

		try {
			String tagSinkClassName = TagSyncConfig.getTagSinkClassName(properties);

			if (LOG.isDebugEnabled()) {
				LOG.debug("tagSinkClassName=" + tagSinkClassName);
			}
			@SuppressWarnings("unchecked")
			Class<TagSink> tagSinkClass = (Class<TagSink>) Class.forName(tagSinkClassName);

			ret = tagSinkClass.newInstance();

			if (!ret.initialize(properties)) {
				LOG.error("Failed to initialize TAG sink " + tagSinkClassName);
				ret = null;
			}
		} catch (Throwable t) {
			LOG.error("Failed to initialize TAG sink. Error details: ", t);
			ret = null;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagSynchronizer.initializeTagSink(), result:" + (ret == null ? "false" : "true"));
		}
		return ret;
	}

	private void initializeTagSources() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagSynchronizer.initializeTagSources()");
		}

		List<String> tagSourceNameList = new ArrayList<String>();

		for (Object propNameObj : properties.keySet()) {
			String propName = propNameObj.toString();
			if (!propName.startsWith(TAGSYNC_SOURCE_BASE)) {
				continue;
			}
			String tagSourceName = propName.substring(TAGSYNC_SOURCE_BASE.length());
			List<String> splits = toArray(tagSourceName, ".");
			if (splits.size() > 1) {
				continue;
			}
			String value = properties.getProperty(propName);
			if (value.equalsIgnoreCase("enable")
					|| value.equalsIgnoreCase("enabled")
					|| value.equalsIgnoreCase("true")) {
				tagSourceNameList.add(tagSourceName);
				LOG.info("Tag source " + propName + " is set to "
						+ value);
			}
		}

		List<String> initializedTagSourceNameList = new ArrayList<String>();

		for (String tagSourceName : tagSourceNameList) {
			String tagSourcePropPrefix = TAGSYNC_SOURCE_BASE + tagSourceName;
			TagSource tagSource = getTagSourceFromConfig(properties,
					tagSourcePropPrefix, tagSourceName);

			if (tagSource != null) {
				try {
					if (!tagSource.initialize(properties)) {
						LOG.error("Failed to initialize TAG source " + tagSourceName);
						failedTagSources.add(tagSource);
					} else {
						tagSource.setTagSink(tagSink);
						tagSources.add(tagSource);
						initializedTagSourceNameList.add(tagSourceName);
					}
				} catch(Exception exception) {
					LOG.error("tag-source:" + tagSourceName + " initialization failed with ", exception);
					failedTagSources.add(tagSource);
				}
			}
		}

		if (CollectionUtils.isEmpty(tagSources)) {
			LOG.warn("TagSync is not configured for any tag-sources. No tags will be received by TagSync.");
			LOG.warn("Please recheck configuration properties and tagsync environment to ensure that this is correct.");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagSynchronizer.initializeTagSources(initializedTagSources=" + initializedTagSourceNameList
					+ ", failedTagSources=" + failedTagSources + ")");
		}
	}

	private void reInitializeFailedTagSources() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagSynchronizer.reInitializeFailedTagSources()");
		}

		for (int index = 0; index < failedTagSources.size(); index++) {
			TagSource tagSource = failedTagSources.get(index);
			try {
				if (tagSource.initialize(properties)) {
					failedTagSources.remove(index);
					--index;
					tagSources.add(tagSource);
					tagSource.setTagSink(tagSink);
					if (tagSource.start()) {
						tagSources.add(tagSource);
					} else {
						LOG.error("Failed to start tagSource: " + tagSource);
					}
				} else {
					LOG.error("Failed to initialize TAG source " + tagSource);
				}
			} catch (Exception exception) {
				LOG.error("tag-source:" + tagSource + " initialization failed with ", exception);
			}

		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagSynchronizer.reInitializeFailedTagSources()");
		}
	}

	static private TagSource getTagSourceFromConfig(Properties props,
													String propPrefix, String tagSourceName) {
		TagSource tagSource = null;
		String className = getStringProperty(props, propPrefix + "."
				+ PROP_CLASS_NAME);
		if (StringUtils.isBlank(className)) {
			if (tagSourceName.equals("file")) {
				className = "org.apache.ranger.tagsync.source.file.FileTagSource";
			} else if (tagSourceName.equalsIgnoreCase("atlas")) {
				className = "org.apache.ranger.tagsync.source.atlas.AtlasTagSource";
			} else if (tagSourceName.equals("atlasrest")) {
				className = "org.apache.ranger.tagsync.source.atlasrest.AtlasRESTTagSource";
			} else {
				LOG.error("tagSource name doesn't have any class associated with it. tagSourceName="
						+ tagSourceName + ", propertyPrefix=" + propPrefix);
			}
		}
		if (StringUtils.isNotBlank(className)) {
			try {
				@SuppressWarnings("unchecked")
				Class<TagSource> tagSourceClass = (Class<TagSource>) Class.forName(className);

				tagSource = tagSourceClass.newInstance();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Created instance of " + className);
				}
				tagSource.setName(tagSourceName);
			} catch (Exception e) {
				LOG.error("Can't instantiate tagSource class for tagSourceName="
						+ tagSourceName + ", className=" + className
						+ ", propertyPrefix=" + propPrefix, e);
			}
		}
		return tagSource;
	}

	public static boolean initializeKerberosIdentity(Properties props) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> TagSynchronizer.initializeKerberosIdentity()");
		}

		boolean ret = false;

		String authenticationType = TagSyncConfig.getAuthenticationType(props);
		String principal = TagSyncConfig.getKerberosPrincipal(props);
		String keytab = TagSyncConfig.getKerberosKeytab(props);
		String nameRules = TagSyncConfig.getNameRules(props);

		if (LOG.isDebugEnabled()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("authenticationType=" + authenticationType);
				LOG.debug("principal=" + principal);
				LOG.debug("keytab" + keytab);
				LOG.debug("nameRules=" + nameRules);
			}
		}
		final boolean isKerberized = !StringUtils.isEmpty(authenticationType) && authenticationType.trim().equalsIgnoreCase(AUTH_TYPE_KERBEROS);

		if (isKerberized) {
			LOG.info("Configured for Kerberos Authentication");

			if (SecureClientLogin.isKerberosCredentialExists(principal, keytab)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Trying to get kerberos identity");
				}

				try {
					UserGroupInformation.loginUserFromKeytab(principal, keytab);
					UserGroupInformation kerberosIdentity = UserGroupInformation.getLoginUser();
					if (kerberosIdentity != null) {
						props.put(TagSyncConfig.TAGSYNC_KERBEROS_IDENTITY, kerberosIdentity.getUserName());
						if (LOG.isDebugEnabled()) {
							LOG.debug("Got UGI, user:[" + kerberosIdentity.getUserName() + "]");
						}
						ret = true;
					} else {
						LOG.error("KerberosIdentity is null!");
					}
				} catch (IOException exception) {
					LOG.error("Failed to get UGI from principal:[" + principal + "], and keytab:[" + keytab + "]", exception);
				}
			} else {
				LOG.error("Invalid Kerberos principal and/or keytab specified. Failed to initialize Kerberos identity");
			}
		} else {
			LOG.info("Not configured for Kerberos Authentication");

			props.remove(TagSyncConfig.TAGSYNC_KERBEROS_IDENTITY);

			ret = true;
		}

		if (!ret) {
			props.remove(TagSyncConfig.TAGSYNC_KERBEROS_IDENTITY);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== TagSynchronizer.initializeKerberosIdentity() : " + ret);
		}

		return ret;
	}

	private static String getStringProperty(Properties props, String propName) {
		String ret = null;

		if (props != null && propName != null) {
			String val = props.getProperty(propName);
			if (val != null) {
				ret = val;
			}
		}

		return ret;
	}

	private static List<String> toArray(String destListStr, String delim) {
		List<String> list = new ArrayList<String>();
		if (destListStr != null && !destListStr.isEmpty()) {
			StringTokenizer tokenizer = new StringTokenizer(destListStr,
					delim.trim());
			while (tokenizer.hasMoreTokens()) {
				list.add(tokenizer.nextToken());
			}
		}
		return list;
	}

}
