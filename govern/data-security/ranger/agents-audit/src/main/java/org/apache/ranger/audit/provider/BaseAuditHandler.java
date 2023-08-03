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
package org.apache.ranger.audit.provider;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

//import java.io.File;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

public abstract class BaseAuditHandler implements AuditHandler {
	private static final Logger LOG = LoggerFactory.getLogger(BaseAuditHandler.class);

	static final String AUDIT_LOG_FAILURE_REPORT_MIN_INTERVAL_PROP = "xasecure.audit.log.failure.report.min.interval.ms";

	public static final String RANGER_POLICYMGR_CLIENT_KEY_FILE                  = "xasecure.policymgr.clientssl.keystore";
	public static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE             = "xasecure.policymgr.clientssl.keystore.type";
	public static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL       = "xasecure.policymgr.clientssl.keystore.credential.file";
	public static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_CREDENTIAL_ALIAS = "sslKeyStore";
	public static final String RANGER_POLICYMGR_CLIENT_KEY_FILE_TYPE_DEFAULT     = "jks";

	public static final String RANGER_POLICYMGR_TRUSTSTORE_FILE                  = "xasecure.policymgr.clientssl.truststore";
	public static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE             = "xasecure.policymgr.clientssl.truststore.type";
	public static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL       = "xasecure.policymgr.clientssl.truststore.credential.file";
	public static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_CREDENTIAL_ALIAS = "sslTrustStore";
	public static final String RANGER_POLICYMGR_TRUSTSTORE_FILE_TYPE_DEFAULT     = "jks";

	public static final String RANGER_SSL_KEYMANAGER_ALGO_TYPE					 = KeyManagerFactory.getDefaultAlgorithm();
	public static final String RANGER_SSL_TRUSTMANAGER_ALGO_TYPE				 = TrustManagerFactory.getDefaultAlgorithm();
	public static final String RANGER_SSL_CONTEXT_ALGO_TYPE					     = "TLSv1.2";

	public static final String PROP_CONFIG = "config";

	private int mLogFailureReportMinIntervalInMs = 60 * 1000;

	private AtomicLong mFailedLogLastReportTime = new AtomicLong(0);
	private AtomicLong mFailedLogCountSinceLastReport = new AtomicLong(0);
	private AtomicLong mFailedLogCountLifeTime = new AtomicLong(0);

	public static final String PROP_NAME = "name";
	public static final String PROP_CLASS_NAME = "classname";

	public static final String PROP_DEFAULT_PREFIX = "xasecure.audit.provider";

	protected String propPrefix = PROP_DEFAULT_PREFIX;

	protected String providerName = null;
	protected String parentPath = null;

	protected int failedRetryTimes = 3;
	protected int failedRetrySleep = 3 * 1000;

	int errorLogIntervalMS = 30 * 1000; // Every 30 seconds
	long lastErrorLogMS = 0;

	long totalCount = 0;
	long totalSuccessCount = 0;
	long totalFailedCount = 0;
	long totalStashedCount = 0;
	long totalDeferredCount = 0;

	long lastIntervalCount = 0;
	long lastIntervalSuccessCount = 0;
	long lastIntervalFailedCount = 0;
	long lastStashedCount = 0;
	long lastDeferredCount = 0;

	long lastStatusLogTime = System.currentTimeMillis();
	long statusLogIntervalMS = 1 * 60 * 1000;

	protected Properties props = null;
	protected Map<String, String> configProps = new HashMap<String, String>();

	@Override
	public void init(Properties props) {
		init(props, null);
	}

	@Override
	public void init(Properties props, String basePropertyName) {
		LOG.info("BaseAuditProvider.init()");
		this.props = props;
		if (basePropertyName != null) {
			propPrefix = basePropertyName;
		}
		LOG.info("propPrefix=" + propPrefix);

		String name = MiscUtil.getStringProperty(props, basePropertyName + "."
				+ PROP_NAME);
		if (name != null && !name.isEmpty()) {
			setName(name);
		}
		// Get final token
		if (providerName == null) {
			List<String> tokens = MiscUtil.toArray(propPrefix, ".");
			if (!tokens.isEmpty()) {
				String finalToken = tokens.get(tokens.size() - 1);
				setName(finalToken);
				LOG.info("Using providerName from property prefix. providerName="
						+ getName());
			}
		}
		LOG.info("providerName=" + getName());

		try {
			new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").create();
		} catch (Throwable excp) {
			LOG.warn(
					"Log4jAuditProvider.init(): failed to create GsonBuilder object. events will be formated using toString(), instead of Json",
					excp);
		}

		mLogFailureReportMinIntervalInMs = MiscUtil.getIntProperty(props,
				AUDIT_LOG_FAILURE_REPORT_MIN_INTERVAL_PROP, 60 * 1000);

		String configPropsNamePrefix = propPrefix + "." + PROP_CONFIG + ".";
		for (Object propNameObj : props.keySet()) {
			String propName = propNameObj.toString();

			if (!propName.startsWith(configPropsNamePrefix)) {
				continue;
			}
			String configName = propName.substring(configPropsNamePrefix.length());
			String configValue = props.getProperty(propName);
			configProps.put(configName, configValue);
			LOG.info("Found Config property: " + configName + " => " + configValue);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#log(org.apache.ranger.
	 * audit.model.AuditEventBase)
	 */
	@Override
	public boolean log(AuditEventBase event) {
		return log(Collections.singletonList(event));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#logJSON(java.lang.String)
	 */
	@Override
	public boolean logJSON(String event) {
		AuditEventBase eventObj = MiscUtil.fromJson(event,
				AuthzAuditEvent.class);
		return log(eventObj);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#logJSON(java.util.Collection
	 * )
	 */
	@Override
	public boolean logJSON(Collection<String> events) {
		List<AuditEventBase> eventList = new ArrayList<AuditEventBase>(events.size());
		for (String event : events) {
			eventList.add(MiscUtil.fromJson(event, AuthzAuditEvent.class));
		}
		return log(eventList);
	}

   @Override
	public boolean logFile(File file) {
		return logFile(file);
     }

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public String getFinalPath() {
		return getName();
	}

	public void setName(String name) {
		providerName = name;
	}

	@Override
	public String getName() {
		if (parentPath != null) {
			return parentPath + "." + providerName;
		}
		return providerName;
	}

	public long addTotalCount(int count) {
		totalCount += count;
		return totalCount;
	}

	public long addSuccessCount(int count) {
		totalSuccessCount += count;
		return totalSuccessCount;
	}

	public long addFailedCount(int count) {
		totalFailedCount += count;
		return totalFailedCount;
	}

	public long addStashedCount(int count) {
		totalStashedCount += count;
		return totalStashedCount;
	}

	public long addDeferredCount(int count) {
		totalDeferredCount += count;
		return totalDeferredCount;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public long getTotalSuccessCount() {
		return totalSuccessCount;
	}

	public long getTotalFailedCount() {
		return totalFailedCount;
	}

	public long getTotalStashedCount() {
		return totalStashedCount;
	}

	public long getLastStashedCount() {
		return lastStashedCount;
	}

	public long getTotalDeferredCount() {
		return totalDeferredCount;
	}

	public long getLastDeferredCount() {
		return lastDeferredCount;
	}

	public void logStatusIfRequired() {
		long currTime = System.currentTimeMillis();
		if ((currTime - lastStatusLogTime) > statusLogIntervalMS) {
			logStatus();
		}
	}

	public void logStatus() {
		try {
			long currTime = System.currentTimeMillis();

			long diffTime = currTime - lastStatusLogTime;
			lastStatusLogTime = currTime;

			long diffCount = totalCount - lastIntervalCount;
			long diffSuccess = totalSuccessCount - lastIntervalSuccessCount;
			long diffFailed = totalFailedCount - lastIntervalFailedCount;
			long diffStashed = totalStashedCount - lastStashedCount;
			long diffDeferred = totalDeferredCount - lastDeferredCount;

			if (diffCount == 0 && diffSuccess == 0 && diffFailed == 0
					&& diffStashed == 0 && diffDeferred == 0) {
				return;
			}

			lastIntervalCount = totalCount;
			lastIntervalSuccessCount = totalSuccessCount;
			lastIntervalFailedCount = totalFailedCount;
			lastStashedCount = totalStashedCount;
			lastDeferredCount = totalDeferredCount;

			String finalPath = "";
			String tFinalPath = getFinalPath();
			if (!getName().equals(tFinalPath)) {
				finalPath = ", finalDestination=" + tFinalPath;
			}

			String msg = "Audit Status Log: name="
					+ getName()
					+ finalPath
					+ ", interval="
					+ formatIntervalForLog(diffTime)
					+ ", events="
					+ diffCount
					+ (diffSuccess > 0 ? (", succcessCount=" + diffSuccess)
							: "")
					+ (diffFailed > 0 ? (", failedCount=" + diffFailed) : "")
					+ (diffStashed > 0 ? (", stashedCount=" + diffStashed) : "")
					+ (diffDeferred > 0 ? (", deferredCount=" + diffDeferred)
							: "")
					+ ", totalEvents="
					+ totalCount
					+ (totalSuccessCount > 0 ? (", totalSuccessCount=" + totalSuccessCount)
							: "")
					+ (totalFailedCount > 0 ? (", totalFailedCount=" + totalFailedCount)
							: "")
					+ (totalStashedCount > 0 ? (", totalStashedCount=" + totalStashedCount)
							: "")
					+ (totalDeferredCount > 0 ? (", totalDeferredCount=" + totalDeferredCount)
							: "");
			LOG.info(msg);
		} catch (Throwable t) {
			LOG.error("Error while printing stats. auditProvider=" + getName());
		}
	}

	public void logError(String msg) {
		long currTimeMS = System.currentTimeMillis();
		if (currTimeMS - lastErrorLogMS > errorLogIntervalMS) {
			LOG.error(msg);
			lastErrorLogMS = currTimeMS;
		}
	}

	public void logError(String msg, Throwable ex) {
		long currTimeMS = System.currentTimeMillis();
		if (currTimeMS - lastErrorLogMS > errorLogIntervalMS) {
			LOG.error(msg, ex);
			lastErrorLogMS = currTimeMS;
		}
	}

	public String getTimeDiffStr(long time1, long time2) {
		long timeInMs = Math.abs(time1 - time2);
		return formatIntervalForLog(timeInMs);
	}

	public String formatIntervalForLog(long timeInMs) {
		long hours = timeInMs / (60 * 60 * 1000);
		long minutes = (timeInMs / (60 * 1000)) % 60;
		long seconds = (timeInMs % (60 * 1000)) / 1000;
		long mSeconds = (timeInMs % (1000));

		if (hours > 0)
			return String.format("%02d:%02d:%02d.%03d hours", hours, minutes,
					seconds, mSeconds);
		else if (minutes > 0)
			return String.format("%02d:%02d.%03d minutes", minutes, seconds,
					mSeconds);
		else if (seconds > 0)
			return String.format("%02d.%03d seconds", seconds, mSeconds);
		else
			return String.format("%03d milli-seconds", mSeconds);
	}

	public void logFailedEvent(AuditEventBase event) {
		logFailedEvent(event, "");
	}

	public void logFailedEvent(AuditEventBase event, Throwable excp) {
		long now = System.currentTimeMillis();

		long timeSinceLastReport = now - mFailedLogLastReportTime.get();
		long countSinceLastReport = mFailedLogCountSinceLastReport
				.incrementAndGet();
		long countLifeTime = mFailedLogCountLifeTime.incrementAndGet();

		if (timeSinceLastReport >= mLogFailureReportMinIntervalInMs) {
			mFailedLogLastReportTime.set(now);
			mFailedLogCountSinceLastReport.set(0);

			if (excp != null) {
				LOG.warn(
						"failed to log audit event: "
								+ MiscUtil.stringify(event), excp);
			} else {
				LOG.warn("failed to log audit event: "
						+ MiscUtil.stringify(event));
			}

			if (countLifeTime > 1) { // no stats to print for the 1st failure
				LOG.warn("Log failure count: " + countSinceLastReport
						+ " in past "
						+ formatIntervalForLog(timeSinceLastReport) + "; "
						+ countLifeTime + " during process lifetime");
			}
		}
	}

	public void logFailedEvent(Collection<AuditEventBase> events) {
		logFailedEvent(events, "");
	}

	public void logFailedEvent(Collection<AuditEventBase> events, Throwable excp) {
		for (AuditEventBase event : events) {
			logFailedEvent(event, excp);
		}
	}

	public void logFailedEvent(AuditEventBase event, String message) {
		long now = System.currentTimeMillis();

		long timeSinceLastReport = now - mFailedLogLastReportTime.get();
		long countSinceLastReport = mFailedLogCountSinceLastReport
				.incrementAndGet();
		long countLifeTime = mFailedLogCountLifeTime.incrementAndGet();

		if (timeSinceLastReport >= mLogFailureReportMinIntervalInMs) {
			mFailedLogLastReportTime.set(now);
			mFailedLogCountSinceLastReport.set(0);

			LOG.warn("failed to log audit event: " + MiscUtil.stringify(event)
					+ ", errorMessage=" + message);

			if (countLifeTime > 1) { // no stats to print for the 1st failure
				LOG.warn("Log failure count: " + countSinceLastReport
						+ " in past "
						+ formatIntervalForLog(timeSinceLastReport) + "; "
						+ countLifeTime + " during process lifetime");
			}
		}
	}

	public void logFailedEvent(Collection<AuditEventBase> events,
			String errorMessage) {
		for (AuditEventBase event : events) {
			logFailedEvent(event, errorMessage);
		}
	}

	public void logFailedEventJSON(String event, Throwable excp) {
		long now = System.currentTimeMillis();

		long timeSinceLastReport = now - mFailedLogLastReportTime.get();
		long countSinceLastReport = mFailedLogCountSinceLastReport
				.incrementAndGet();
		long countLifeTime = mFailedLogCountLifeTime.incrementAndGet();

		if (timeSinceLastReport >= mLogFailureReportMinIntervalInMs) {
			mFailedLogLastReportTime.set(now);
			mFailedLogCountSinceLastReport.set(0);

			if (excp != null) {
				LOG.warn("failed to log audit event: " + event, excp);
			} else {
				LOG.warn("failed to log audit event: " + event);
			}

			if (countLifeTime > 1) { // no stats to print for the 1st failure
				LOG.warn("Log failure count: " + countSinceLastReport
						+ " in past "
						+ formatIntervalForLog(timeSinceLastReport) + "; "
						+ countLifeTime + " during process lifetime");
			}
		}
	}

	public void logFailedEventJSON(Collection<String> events, Throwable excp) {
		for (String event : events) {
			logFailedEventJSON(event, excp);
		}
	}

}
