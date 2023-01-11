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

import java.util.Collection;
import java.util.Properties;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4JAuditDestination extends AuditDestination {
	private static final Logger logger = LoggerFactory
			.getLogger(Log4JAuditDestination.class);

	private static Logger auditLogger = null;

	public static final String PROP_LOG4J_LOGGER = "logger";
	public static final String DEFAULT_LOGGER_PREFIX = "ranger.audit";
	private String loggerName = null;

	public Log4JAuditDestination() {
		logger.info("Log4JAuditDestination() called.");

	}

	@Override
	public void init(Properties prop, String propPrefix) {
		super.init(prop, propPrefix);
		loggerName = MiscUtil.getStringProperty(props, propPrefix + "."
				+ PROP_LOG4J_LOGGER);
		if (loggerName == null || loggerName.isEmpty()) {
			loggerName = DEFAULT_LOGGER_PREFIX + "." + getName();
			logger.info("Logger property " + propPrefix + "."
					+ PROP_LOG4J_LOGGER + " was not set. Constructing default="
					+ loggerName);
		}
		logger.info("Logger name for " + getName() + " is " + loggerName);
		auditLogger = LoggerFactory.getLogger(loggerName);
		logger.info("Done initializing logger for audit. name=" + getName()
				+ ", loggerName=" + loggerName);
	}

	
	@Override
	public void stop() {
		super.stop();
		logStatus();
	}

	@Override
	public boolean log(AuditEventBase event) {
		if (!auditLogger.isInfoEnabled()) {
			logStatusIfRequired();
			addTotalCount(1);
			return true;
		}

		if (event != null) {
			String eventStr = MiscUtil.stringify(event);
			logJSON(eventStr);
		}
		return true;
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		if (!auditLogger.isInfoEnabled()) {
			logStatusIfRequired();
			addTotalCount(events.size());
			return true;
		}

		for (AuditEventBase event : events) {
			log(event);
		}
		return true;
	}

	@Override
	public boolean logJSON(String event) {
		logStatusIfRequired();
		addTotalCount(1);
		if (!auditLogger.isInfoEnabled()) {
			return true;
		}

		if (event != null) {
			auditLogger.info(event);
			addSuccessCount(1);
		}
		return true;
	}

	@Override
	public boolean logJSON(Collection<String> events) {
		if (!auditLogger.isInfoEnabled()) {
			logStatusIfRequired();
			addTotalCount(events.size());
			return true;
		}

		for (String event : events) {
			logJSON(event);
		}
		return false;
	}

}
