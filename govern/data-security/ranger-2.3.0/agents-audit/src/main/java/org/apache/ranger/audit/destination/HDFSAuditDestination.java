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

import java.io.File;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.AuditWriterFactory;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.audit.utils.RangerAuditWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class write the logs to local file
 */
public class HDFSAuditDestination extends AuditDestination {
	private static final Logger logger = LoggerFactory
			.getLogger(HDFSAuditDestination.class);

	private Map<String, String> auditConfigs            = null;
	private String              auditProviderName       = null;
	private RangerAuditWriter   auditWriter             = null;
	private boolean             initDone                = false;
	private boolean             isStopped               = false;

	@Override
	public void init(Properties prop, String propPrefix) {
		super.init(prop, propPrefix);
		this.auditProviderName = getName();
		this.auditConfigs	   = configProps;

		try {
			this.auditWriter = getWriter();
			this.initDone = true;
		} catch (Exception e) {
			logger.error("Error while getting Audit writer", e);
		}
	}

	@Override
	synchronized public boolean logJSON(final Collection<String> events) {
		logStatusIfRequired();
		addTotalCount(events.size());

		if (!initDone) {
			addDeferredCount(events.size());
			return false;
		}
		if (isStopped) {
			addDeferredCount(events.size());
			logError("log() called after stop was requested. name=" + getName());
			return false;
		}
		try {
			boolean ret = auditWriter.log(events);
			if (!ret) {
				addDeferredCount(events.size());
				return false;
			}
		} catch (Throwable t) {
			addDeferredCount(events.size());
			logError("Error writing to log file.", t);
			return false;
		} finally {
			logger.info("Flushing HDFS audit. Event Size:" + events.size());
			if (auditWriter != null) {
				flush();
			}
		}
		addSuccessCount(events.size());
		return true;
	}

	@Override
	synchronized public boolean logFile(final File file)  {
		logStatusIfRequired();
		if (!initDone) {
			return false;
		}
		if (isStopped) {
			logError("log() called after stop was requested. name=" + getName());
			return false;
		}

		try {
			boolean ret = auditWriter.logFile(file);
			if (!ret) {
				return false;
			}
		} catch (Throwable t) {
			logError("Error writing to log file.", t);
			return false;
		} finally {
			logger.info("Flushing HDFS audit. File:" + file.getAbsolutePath() + file.getName());
			if (auditWriter != null) {
				flush();
			}
		}
		return true;
	}

	@Override
	public void flush() {
		logger.info("Flush called. name=" + getName());
		MiscUtil.executePrivilegedAction(new PrivilegedAction<Void>() {
			@Override
			public Void run() {
				auditWriter.flush();
				return null;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#log(java.util.Collection)
	 */
	@Override
	public boolean log(Collection<AuditEventBase> events) {
		if (isStopped) {
			logStatusIfRequired();
			addTotalCount(events.size());
			addDeferredCount(events.size());
			logError("log() called after stop was requested. name=" + getName());
			return false;
		}
		List<String> jsonList = new ArrayList<String>();
		for (AuditEventBase event : events) {
			try {
				jsonList.add(MiscUtil.stringify(event));
			} catch (Throwable t) {
				logger.error("Error converting to JSON. event=" + event);
				addTotalCount(1);
				addFailedCount(1);
				logFailedEvent(event);
			}
		}
		return logJSON(jsonList);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#start()
	 */
	@Override
	public void start() {
		// Nothing to do here. We will open the file when the first log request
		// comes
	}

	@Override
	synchronized public void stop() {
		auditWriter.stop();
		logStatus();
		isStopped = true;
	}

	public RangerAuditWriter getWriter() throws Exception {
		AuditWriterFactory auditWriterFactory = AuditWriterFactory.getInstance();
		auditWriterFactory.init(props, propPrefix, auditProviderName, auditConfigs);
		return auditWriterFactory.getAuditWriter();
	}
}
