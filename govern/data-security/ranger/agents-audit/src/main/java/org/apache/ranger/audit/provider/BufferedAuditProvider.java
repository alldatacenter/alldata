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

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;

public abstract class BufferedAuditProvider extends BaseAuditHandler {
	private LogBuffer<AuditEventBase> mBuffer = null;
	private LogDestination<AuditEventBase> mDestination = null;

	@Override
	public boolean log(AuditEventBase event) {
		if (event instanceof AuthzAuditEvent) {
			AuthzAuditEvent authzEvent = (AuthzAuditEvent) event;

			if (authzEvent.getAgentHostname() == null) {
				authzEvent.setAgentHostname(MiscUtil.getHostname());
			}

			if (authzEvent.getLogType() == null) {
				authzEvent.setLogType("RangerAudit");
			}

			if (authzEvent.getEventId() == null) {
				authzEvent.setEventId(MiscUtil.generateUniqueId());
			}
		}

		if (!mBuffer.add(event)) {
			logFailedEvent(event);
			return false;
		}
		return true;
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		boolean ret = true;
		for (AuditEventBase event : events) {
			ret = log(event);
			if (!ret) {
				break;
			}
		}
		return ret;
	}

	@Override
	public boolean logJSON(String event) {
		AuditEventBase eventObj = MiscUtil.fromJson(event,
				AuthzAuditEvent.class);
		return log(eventObj);
	}

	@Override
	public boolean logJSON(Collection<String> events) {
		boolean ret = true;
		for (String event : events) {
			ret = logJSON(event);
			if (!ret) {
				break;
			}
		}
		return ret;
	}

	@Override
	public void start() {
		mBuffer.start(mDestination);
	}

	@Override
	public void stop() {
		mBuffer.stop();
	}

	@Override
	public void waitToComplete() {
	}

	@Override
	public void waitToComplete(long timeout) {
	}

	@Override
	public void flush() {
	}

	protected LogBuffer<AuditEventBase> getBuffer() {
		return mBuffer;
	}

	protected LogDestination<AuditEventBase> getDestination() {
		return mDestination;
	}

	protected void setBufferAndDestination(LogBuffer<AuditEventBase> buffer,
			LogDestination<AuditEventBase> destination) {
		mBuffer = buffer;
		mDestination = destination;
	}
}
