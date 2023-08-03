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

package org.apache.ranger.audit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.ranger.audit.destination.AuditDestination;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConsumer extends AuditDestination {
	private static final Logger logger = LoggerFactory.getLogger(TestConsumer.class);

	int countTotal = 0;
	int sumTotal = 0;
	int batchCount = 0;
	String providerName = getClass().getName();
	boolean isDown = false;

	List<AuthzAuditEvent> eventList = new ArrayList<AuthzAuditEvent>();

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#log(org.apache.ranger
	 * .audit.model.AuditEventBase)
	 */
	@Override
	public boolean log(AuditEventBase event) {
		if (isDown) {
			return false;
		}
		countTotal++;
		if (event instanceof AuthzAuditEvent) {
			AuthzAuditEvent azEvent = (AuthzAuditEvent) event;
			sumTotal += azEvent.getEventCount();
			logger.info("EVENT:" + event);
			eventList.add(azEvent);
		}
		return true;
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		if (isDown) {
			return false;
		}
		batchCount++;
		for (AuditEventBase event : events) {
			log(event);
		}
		return true;
	}

	@Override
	public boolean logJSON(String jsonStr) {
		if (isDown) {
			return false;
		}
		countTotal++;
		AuthzAuditEvent event = MiscUtil.fromJson(jsonStr,
				AuthzAuditEvent.class);
		sumTotal += event.getEventCount();
		logger.info("JSON:" + jsonStr);
		eventList.add(event);
		return true;
	}

	@Override
	public boolean logJSON(Collection<String> events) {
		if (isDown) {
			return false;
		}
		batchCount++;
		for (String event : events) {
			logJSON(event);
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#init(java.util.Properties
	 * )
	 */
	@Override
	public void init(Properties prop) {
		// Nothing to do here
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#start()
	 */
	@Override
	public void start() {
		// Nothing to do here
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#stop()
	 */
	@Override
	public void stop() {
		// Nothing to do here
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#waitToComplete()
	 */
	@Override
	public void waitToComplete() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#flush()
	 */
	@Override
	public void flush() {
		// Nothing to do here
	}

	public int getCountTotal() {
		return countTotal;
	}

	public int getSumTotal() {
		return sumTotal;
	}

	public int getBatchCount() {
		return batchCount;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.ranger.audit.provider.AuditProvider#init(java.util.Properties
	 * , java.lang.String)
	 */
	@Override
	public void init(Properties prop, String basePropertyName) {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#waitToComplete(long)
	 */
	@Override
	public void waitToComplete(long timeout) {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#getName()
	 */
	@Override
	public String getName() {
		return providerName;
	}

	// Local methods
	public AuthzAuditEvent isInSequence() {
		long lastSeq = -1;
		for (AuthzAuditEvent event : eventList) {
			if (event.getSeqNum() <= lastSeq) {
				return event;
			}
			lastSeq = event.getSeqNum();
		}
		return null;
	}
}
