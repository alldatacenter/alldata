/**
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

import java.util.Collection;
import java.util.Properties;

import org.apache.ranger.audit.destination.AuditDestination;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Log4jAuditProvider extends AuditDestination {

	private static final Logger LOG      = LoggerFactory.getLogger(Log4jAuditProvider.class);
	private static final Logger AUDITLOG = LoggerFactory.getLogger("xaaudit." + Log4jAuditProvider.class.getName());

	public static final String AUDIT_LOG4J_IS_ASYNC_PROP           = "xasecure.audit.log4j.is.async";
	public static final String AUDIT_LOG4J_MAX_QUEUE_SIZE_PROP     = "xasecure.audit.log4j.async.max.queue.size";
	public static final String AUDIT_LOG4J_MAX_FLUSH_INTERVAL_PROP = "xasecure.audit.log4j.async.max.flush.interval.ms";


	public Log4jAuditProvider() {
		LOG.info("Log4jAuditProvider: creating..");
	}

	@Override
	public void init(Properties props) {
		LOG.info("Log4jAuditProvider.init()");

		super.init(props);
	}

	@Override
	public boolean log(AuditEventBase event) {
		if(! AUDITLOG.isInfoEnabled())
			return true;
		
		if(event != null) {
			String eventStr = MiscUtil.stringify(event);
			AUDITLOG.info(eventStr);
		}
		return true;
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		for (AuditEventBase event : events) {
			log(event);
		}
		return true;
	}

	@Override
	public boolean logJSON(String event) {
		AuditEventBase eventObj = MiscUtil.fromJson(event,
				AuthzAuditEvent.class);
		return log(eventObj);
	}

	@Override
	public boolean logJSON(Collection<String> events) {
		for (String event : events) {
			logJSON(event);
		}
		return true;
	}

	@Override
	public void start() {
		// intentionally left empty
	}

	@Override
	public void stop() {
		// intentionally left empty
	}

	

	
}
