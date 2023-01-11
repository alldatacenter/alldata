/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.audit.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.hadoop.util.ShutdownHookManager;
import org.apache.ranger.audit.destination.*;
import org.apache.ranger.audit.provider.hdfs.HdfsAuditProvider;
import org.apache.ranger.audit.provider.kafka.KafkaAuditProvider;
import org.apache.ranger.audit.provider.solr.SolrAuditProvider;
import org.apache.ranger.audit.queue.AuditAsyncQueue;
import org.apache.ranger.audit.queue.AuditBatchQueue;
import org.apache.ranger.audit.queue.AuditFileQueue;
import org.apache.ranger.audit.queue.AuditQueue;
import org.apache.ranger.audit.queue.AuditSummaryQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * TODO:
 * 1) Flag to enable/disable audit logging
 * 2) Failed path to be recorded
 * 3) Repo name, repo type from configuration
 */

public class AuditProviderFactory {
	private static final Logger LOG = LoggerFactory
			.getLogger(AuditProviderFactory.class);

	public static final String AUDIT_IS_ENABLED_PROP = "xasecure.audit.is.enabled";
	public static final String AUDIT_HDFS_IS_ENABLED_PROP = "xasecure.audit.hdfs.is.enabled";
	public static final String AUDIT_LOG4J_IS_ENABLED_PROP = "xasecure.audit.log4j.is.enabled";
	public static final String AUDIT_KAFKA_IS_ENABLED_PROP = "xasecure.audit.kafka.is.enabled";
	public static final String AUDIT_SOLR_IS_ENABLED_PROP = "xasecure.audit.solr.is.enabled";

	public static final String AUDIT_DEST_BASE = "xasecure.audit.destination";
	public static final String AUDIT_SHUTDOWN_HOOK_MAX_WAIT_SEC = "xasecure.audit.shutdown.hook.max.wait.seconds";
	public static final String AUDIT_IS_FILE_CACHE_PROVIDER_ENABLE_PROP = "xasecure.audit.provider.filecache.is.enabled";
	public static final String FILE_QUEUE_TYPE	  = "filequeue";
	public static final String DEFAULT_QUEUE_TYPE = "memoryqueue";
	public static final int AUDIT_SHUTDOWN_HOOK_MAX_WAIT_SEC_DEFAULT = 30;

	public static final int AUDIT_ASYNC_MAX_QUEUE_SIZE_DEFAULT = 10 * 1024;
	public static final int AUDIT_ASYNC_MAX_FLUSH_INTERVAL_DEFAULT = 5 * 1000;

	private static final int RANGER_AUDIT_SHUTDOWN_HOOK_PRIORITY = 30;

	private volatile static AuditProviderFactory sFactory = null;

	private AuditHandler mProvider = null;
	private String componentAppType = "";
	private boolean mInitDone = false;
	private JVMShutdownHook jvmShutdownHook = null;
	private ArrayList<String> hbaseAppTypes = new ArrayList<>(Arrays.asList("hbaseMaster","hbaseRegional"));

	public AuditProviderFactory() {
		LOG.info("AuditProviderFactory: creating..");

		mProvider = getDefaultProvider();
	}

	public static AuditProviderFactory getInstance() {
		AuditProviderFactory ret = sFactory;
		if(ret == null) {
			synchronized(AuditProviderFactory.class) {
				ret = sFactory;
				if(ret == null) {
					ret = sFactory = new AuditProviderFactory();
				}
			}
		}

		return ret;
	}

	public AuditHandler getAuditProvider() {
		return mProvider;
	}

	public boolean isInitDone() {
		return mInitDone;
	}

	/**
	 * call shutdown hook to provide a way to
	 * shutdown gracefully in addition to the ShutdownHook mechanism
	 */
	public void shutdown() {
		if (isInitDone() && jvmShutdownHook != null) {
			jvmShutdownHook.run();
		}
	}

	public synchronized void init(Properties props, String appType) {
		LOG.info("AuditProviderFactory: initializing..");

		if (mInitDone) {
			LOG.warn("AuditProviderFactory.init(): already initialized! Will try to re-initialize");
		}
		mInitDone = true;
		componentAppType = appType;
		MiscUtil.setApplicationType(appType);

		boolean isEnabled = MiscUtil.getBooleanProperty(props,
				AUDIT_IS_ENABLED_PROP, true);
        if (!isEnabled) {
            LOG.info("AuditProviderFactory: Audit not enabled..");
            mProvider = getDefaultProvider();
            return;
        }

		boolean isAuditToHdfsEnabled = MiscUtil.getBooleanProperty(props,
				AUDIT_HDFS_IS_ENABLED_PROP, false);
		boolean isAuditToLog4jEnabled = MiscUtil.getBooleanProperty(props,
				AUDIT_LOG4J_IS_ENABLED_PROP, false);
		boolean isAuditToKafkaEnabled = MiscUtil.getBooleanProperty(props,
				AUDIT_KAFKA_IS_ENABLED_PROP, false);
		boolean isAuditToSolrEnabled = MiscUtil.getBooleanProperty(props,
				AUDIT_SOLR_IS_ENABLED_PROP, false);

		boolean isAuditFileCacheProviderEnabled = MiscUtil.getBooleanProperty(props,
				AUDIT_IS_FILE_CACHE_PROVIDER_ENABLE_PROP, false);

		List<AuditHandler> providers = new ArrayList<AuditHandler>();

		for (Object propNameObj : props.keySet()) {
			LOG.info("AUDIT PROPERTY: " + propNameObj.toString() + "="
					+ props.getProperty(propNameObj.toString()));
		}

		// Process new audit configurations
		List<String> destNameList = new ArrayList<String>();

		for (Object propNameObj : props.keySet()) {
			String propName = propNameObj.toString();
			if (!propName.startsWith(AUDIT_DEST_BASE)) {
				continue;
			}
			String destName = propName.substring(AUDIT_DEST_BASE.length() + 1);
			List<String> splits = MiscUtil.toArray(destName, ".");
			if (splits.size() > 1) {
				continue;
			}
			String value = props.getProperty(propName);
			if (value.equalsIgnoreCase("enable")
					|| value.equalsIgnoreCase("enabled")
					|| value.equalsIgnoreCase("true")) {
				destNameList.add(destName);
				LOG.info("Audit destination " + propName + " is set to "
						+ value);
			}
		}

		for (String destName : destNameList) {
			String destPropPrefix = AUDIT_DEST_BASE + "." + destName;
			AuditHandler destProvider = getProviderFromConfig(props,
					destPropPrefix, destName, null);

			if (destProvider != null) {
				destProvider.init(props, destPropPrefix);

				String queueName = MiscUtil.getStringProperty(props,
						destPropPrefix + "." + AuditQueue.PROP_QUEUE);
				if (queueName == null || queueName.isEmpty()) {
					LOG.info(destPropPrefix + "." + AuditQueue.PROP_QUEUE
							+ " is not set. Setting queue to batch for "
							+ destName);
					queueName = "batch";
				}
				LOG.info("queue for " + destName + " is " + queueName);
				if (queueName != null && !queueName.isEmpty()
						&& !queueName.equalsIgnoreCase("none")) {
					String queuePropPrefix = destPropPrefix + "." + queueName;
					AuditHandler queueProvider = getProviderFromConfig(props,
							queuePropPrefix, queueName, destProvider);
					if (queueProvider != null) {
						if (queueProvider instanceof AuditQueue) {
							AuditQueue qProvider = (AuditQueue) queueProvider;
							qProvider.init(props, queuePropPrefix);
							providers.add(queueProvider);
						} else {
							LOG.error("Provider queue doesn't extend AuditQueue. Destination="
									+ destName
									+ " can't be created. queueName="
									+ queueName);
						}
					} else {
						LOG.error("Queue provider for destination " + destName
								+ " can't be created. queueName=" + queueName);
					}
				} else {
					LOG.info("Audit destination " + destProvider.getName()
							+ " added to provider list");
					providers.add(destProvider);
				}
			}
		}
		if (providers.size() > 0) {
			LOG.info("Using v3 audit configuration");
			AuditHandler consumer = providers.get(0);

			// Possible pipeline is:
			// async_queue -> summary_queue -> multidestination -> batch_queue
			// -> hdfs_destination
			// -> batch_queue -> solr_destination
			// -> batch_queue -> kafka_destination
			// Above, up to multidestination, the providers are same, then it
			// branches out in parallel.

			// Set the providers in the reverse order e.g.

			if (providers.size() > 1) {
				// If there are more than one destination, then we need multi
				// destination to process it in parallel
				LOG.info("MultiDestAuditProvider is used. Destination count="
						+ providers.size());
				MultiDestAuditProvider multiDestProvider = new MultiDestAuditProvider();
				multiDestProvider.init(props);
				multiDestProvider.addAuditProviders(providers);
				consumer = multiDestProvider;
			}

			// Let's see if Summary is enabled, then summarize before sending it
			// downstream
			String propPrefix = BaseAuditHandler.PROP_DEFAULT_PREFIX;
			boolean summaryEnabled = MiscUtil.getBooleanProperty(props,
					propPrefix + "." + "summary" + "." + "enabled", false);
			AuditSummaryQueue summaryQueue = null;
			if (summaryEnabled) {
				LOG.info("AuditSummaryQueue is enabled");
				summaryQueue = new AuditSummaryQueue(consumer);
				summaryQueue.init(props, propPrefix);
				consumer = summaryQueue;
			} else {
				LOG.info("AuditSummaryQueue is disabled");
			}

			if (!isAuditFileCacheProviderEnabled) {
				// Create the AsysnQueue
				AuditAsyncQueue asyncQueue = new AuditAsyncQueue(consumer);
				propPrefix = BaseAuditHandler.PROP_DEFAULT_PREFIX + "." + "async";
				asyncQueue.init(props, propPrefix);
				asyncQueue.setParentPath(componentAppType);
				mProvider = asyncQueue;
				LOG.info("Starting audit queue " + mProvider.getName());
				mProvider.start();
			} else {
				// Assign AsyncQueue to AuditFileCacheProvider
				AuditFileCacheProvider auditFileCacheProvider = new AuditFileCacheProvider(consumer);
				propPrefix = BaseAuditHandler.PROP_DEFAULT_PREFIX + "." + "filecache";
				auditFileCacheProvider.init(props, propPrefix);
				auditFileCacheProvider.setParentPath(componentAppType);
				mProvider = auditFileCacheProvider;
				LOG.info("Starting Audit File Cache Provider " + mProvider.getName());
				mProvider.start();
			}
		} else {
			LOG.info("No v3 audit configuration found. Trying v2 audit configurations");
			if (!isEnabled
					|| !(isAuditToHdfsEnabled
					|| isAuditToKafkaEnabled || isAuditToLog4jEnabled
					|| isAuditToSolrEnabled || providers.size() == 0)) {
				LOG.info("AuditProviderFactory: Audit not enabled..");

				mProvider = getDefaultProvider();

				return;
			}

			if (isAuditToHdfsEnabled) {
				LOG.info("HdfsAuditProvider is enabled");

				HdfsAuditProvider hdfsProvider = new HdfsAuditProvider();

				boolean isAuditToHdfsAsync = MiscUtil.getBooleanProperty(props,
						HdfsAuditProvider.AUDIT_HDFS_IS_ASYNC_PROP, false);

				if (isAuditToHdfsAsync) {
					int maxQueueSize = MiscUtil.getIntProperty(props,
							HdfsAuditProvider.AUDIT_HDFS_MAX_QUEUE_SIZE_PROP,
							AUDIT_ASYNC_MAX_QUEUE_SIZE_DEFAULT);
					int maxFlushInterval = MiscUtil
							.getIntProperty(
									props,
									HdfsAuditProvider.AUDIT_HDFS_MAX_FLUSH_INTERVAL_PROP,
									AUDIT_ASYNC_MAX_FLUSH_INTERVAL_DEFAULT);

					AsyncAuditProvider asyncProvider = new AsyncAuditProvider(
							"HdfsAuditProvider", maxQueueSize,
							maxFlushInterval, hdfsProvider);

					providers.add(asyncProvider);
				} else {
					providers.add(hdfsProvider);
				}
			}

			if (isAuditToKafkaEnabled) {
				LOG.info("KafkaAuditProvider is enabled");
				KafkaAuditProvider kafkaProvider = new KafkaAuditProvider();
				kafkaProvider.init(props);

				if (kafkaProvider.isAsync()) {
					AsyncAuditProvider asyncProvider = new AsyncAuditProvider(
							"MyKafkaAuditProvider", 1000, 1000, kafkaProvider);
					providers.add(asyncProvider);
				} else {
					providers.add(kafkaProvider);
				}
			}

			if (isAuditToSolrEnabled) {
				LOG.info("SolrAuditProvider is enabled");
				SolrAuditProvider solrProvider = new SolrAuditProvider();
				solrProvider.init(props);

				if (solrProvider.isAsync()) {
					AsyncAuditProvider asyncProvider = new AsyncAuditProvider(
							"MySolrAuditProvider", 1000, 1000, solrProvider);
					providers.add(asyncProvider);
				} else {
					providers.add(solrProvider);
				}
			}

			if (isAuditToLog4jEnabled) {
				Log4jAuditProvider log4jProvider = new Log4jAuditProvider();

				boolean isAuditToLog4jAsync = MiscUtil.getBooleanProperty(
						props, Log4jAuditProvider.AUDIT_LOG4J_IS_ASYNC_PROP,
						false);

				if (isAuditToLog4jAsync) {
					int maxQueueSize = MiscUtil.getIntProperty(props,
							Log4jAuditProvider.AUDIT_LOG4J_MAX_QUEUE_SIZE_PROP,
							AUDIT_ASYNC_MAX_QUEUE_SIZE_DEFAULT);
					int maxFlushInterval = MiscUtil
							.getIntProperty(
									props,
									Log4jAuditProvider.AUDIT_LOG4J_MAX_FLUSH_INTERVAL_PROP,
									AUDIT_ASYNC_MAX_FLUSH_INTERVAL_DEFAULT);

					AsyncAuditProvider asyncProvider = new AsyncAuditProvider(
							"Log4jAuditProvider", maxQueueSize,
							maxFlushInterval, log4jProvider);

					providers.add(asyncProvider);
				} else {
					providers.add(log4jProvider);
				}
			}
			if (providers.size() == 0) {
				mProvider = getDefaultProvider();
			} else if (providers.size() == 1) {
				mProvider = providers.get(0);
			} else {
				MultiDestAuditProvider multiDestProvider = new MultiDestAuditProvider();

				multiDestProvider.addAuditProviders(providers);

				mProvider = multiDestProvider;
			}

			mProvider.init(props);
			mProvider.start();
		}

		installJvmSutdownHook(props);
	}

	private AuditHandler getProviderFromConfig(Properties props,
			String propPrefix, String providerName, AuditHandler consumer) {
		AuditHandler provider = null;
		String className = MiscUtil.getStringProperty(props, propPrefix + "."
				+ BaseAuditHandler.PROP_CLASS_NAME);
		if (className != null && !className.isEmpty()) {
			try {
				Class<?> handlerClass = Class.forName(className);
				if (handlerClass.isAssignableFrom(AuditQueue.class)) {
					// Queue class needs consumer
					handlerClass.getDeclaredConstructor(AuditHandler.class)
							.newInstance(consumer);
				} else {
					provider = (AuditHandler) Class.forName(className)
							.newInstance();
				}
			} catch (Exception e) {
				LOG.error("Can't instantiate audit class for providerName="
						+ providerName + ", className=" + className
						+ ", propertyPrefix=" + propPrefix, e);
			}
		} else {
			if (providerName.equalsIgnoreCase("file")) {
				provider = new FileAuditDestination();
			} else if (providerName.equalsIgnoreCase("hdfs")) {
				provider = new HDFSAuditDestination();
			} else if (providerName.equalsIgnoreCase("solr")) {
				provider = new SolrAuditDestination();
			} else if (providerName.equalsIgnoreCase("elasticsearch")) {
				provider = new ElasticSearchAuditDestination();
			} else if (providerName.equalsIgnoreCase("amazon_cloudwatch")) {
				provider = new AmazonCloudWatchAuditDestination();
			} else if (providerName.equalsIgnoreCase("kafka")) {
				provider = new KafkaAuditProvider();
			} else if (providerName.equalsIgnoreCase("log4j")) {
				provider = new Log4JAuditDestination();
			} else if (providerName.equalsIgnoreCase("batch")) {
				provider = getAuditProvider(props, propPrefix, consumer);
			} else if (providerName.equalsIgnoreCase("async")) {
				provider = new AuditAsyncQueue(consumer);
			} else {
				LOG.error("Provider name doesn't have any class associated with it. providerName="
						+ providerName + ", propertyPrefix=" + propPrefix);
			}
		}
		if (provider != null && provider instanceof AuditQueue) {
			if (consumer == null) {
				LOG.error("consumer can't be null for AuditQueue. queue="
						+ provider.getName() + ", propertyPrefix=" + propPrefix);
				provider = null;
			}
		}
		return provider;
	}

	private AuditHandler getAuditProvider(Properties props, String propPrefix, AuditHandler consumer) {
		AuditHandler ret       = null;
		String       queueType = MiscUtil.getStringProperty(props, propPrefix + "." + "queuetype", DEFAULT_QUEUE_TYPE);

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AuditProviderFactory.getAuditProvider()  propPerfix=  " + propPrefix + ", " + " queueType=  " + queueType);
		}

		if (FILE_QUEUE_TYPE.equalsIgnoreCase(queueType)) {
			AuditFileQueue auditFileQueue      = new AuditFileQueue(consumer);
			String         propPrefixFileQueue = propPrefix + "." + FILE_QUEUE_TYPE;
			auditFileQueue.init(props, propPrefixFileQueue);
			ret = new AuditBatchQueue(auditFileQueue);
		} else {
			ret = new AuditBatchQueue(consumer);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AuditProviderFactory.getAuditProvider()");
		}

		return ret;
	}

	private AuditHandler getDefaultProvider() {
		return new DummyAuditProvider();
	}

	private void installJvmSutdownHook(Properties props) {
		int shutdownHookMaxWaitSeconds = MiscUtil.getIntProperty(props, AUDIT_SHUTDOWN_HOOK_MAX_WAIT_SEC, AUDIT_SHUTDOWN_HOOK_MAX_WAIT_SEC_DEFAULT);
		jvmShutdownHook = new JVMShutdownHook(mProvider, shutdownHookMaxWaitSeconds);
		String appType = this.componentAppType;
		if (appType != null && !hbaseAppTypes.contains(appType)) {
			ShutdownHookManager.get().addShutdownHook(jvmShutdownHook, RANGER_AUDIT_SHUTDOWN_HOOK_PRIORITY);
		}
	}

	private static class RangerAsyncAuditCleanup implements Runnable {

		final Semaphore startCleanup;
		final Semaphore doneCleanup;
		final AuditHandler mProvider;

		RangerAsyncAuditCleanup(AuditHandler provider, Semaphore startCleanup, Semaphore doneCleanup) {
			this.startCleanup = startCleanup;
			this.doneCleanup = doneCleanup;
			this.mProvider = provider;
		}

		@Override
		public void run() {
			while (true) {
				LOG.info("RangerAsyncAuditCleanup: Waiting to audit cleanup start signal");
				try {
					startCleanup.acquire();
				} catch (InterruptedException e) {
					LOG.info("RangerAsyncAuditCleanup: Interrupted while waiting for audit startCleanup signal!  Exiting the thread...", e);
					break;
				}
				LOG.info("RangerAsyncAuditCleanup: Starting cleanup");
				mProvider.waitToComplete();
				mProvider.stop();
				doneCleanup.release();
				LOG.info("RangerAsyncAuditCleanup: Done cleanup");
			}
		}
	}

	private static class JVMShutdownHook extends Thread {
		final Semaphore startCleanup = new Semaphore(0);
		final Semaphore doneCleanup = new Semaphore(0);
		final Thread cleanupThread;
		final int maxWait;
		final AtomicBoolean done = new AtomicBoolean(false);

		public JVMShutdownHook(AuditHandler provider, int maxWait) {
			this.maxWait = maxWait;
			Runnable runnable = new RangerAsyncAuditCleanup(provider, startCleanup, doneCleanup);
			cleanupThread = new Thread(runnable, "Ranger async Audit cleanup");
			cleanupThread.setDaemon(true);
			cleanupThread.start();
		}

		public void run() {
			if (!done.compareAndSet(false, true)) {
				LOG.info("==> JVMShutdownHook.run() already done by another thread");
				return;
			}
			LOG.info("==> JVMShutdownHook.run()");
			LOG.info("JVMShutdownHook: Signalling async audit cleanup to start.");
			startCleanup.release();
			try {
				Long start = System.currentTimeMillis();
				LOG.info("JVMShutdownHook: Waiting up to " + maxWait + " seconds for audit cleanup to finish.");
				boolean cleanupFinishedInTime = doneCleanup.tryAcquire(maxWait, TimeUnit.SECONDS);
				if (cleanupFinishedInTime) {
					LOG.info("JVMShutdownHook: Audit cleanup finished after " + (System.currentTimeMillis() - start) + " milli seconds");
				} else {
					LOG.warn("JVMShutdownHook: could not detect finishing of audit cleanup even after waiting for " + maxWait + " seconds!");
				}
			} catch (InterruptedException e) {
				LOG.info("JVMShutdownHook: Interrupted while waiting for completion of Async executor!", e);
			}
			LOG.info("JVMShutdownHook: Interrupting ranger async audit cleanup thread");
			cleanupThread.interrupt();
			LOG.info("<== JVMShutdownHook.run()");
		}
	}
}
