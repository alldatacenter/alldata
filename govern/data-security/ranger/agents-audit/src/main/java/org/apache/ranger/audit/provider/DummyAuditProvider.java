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

import java.io.File;
import java.util.Collection;
import java.util.Properties;

import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;


public class DummyAuditProvider implements AuditHandler {
	@Override
	public void init(Properties prop) {
		// intentionally left empty
	}

	@Override
	public boolean log(AuditEventBase event) {
		// intentionally left empty
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
		return false;
	}

	@Override
	public void start() {
		// intentionally left empty
	}

	@Override
	public void stop() {
		// intentionally left empty
	}

	@Override
	public void waitToComplete() {
		// intentionally left empty
	}

	@Override
	public void flush() {
		// intentionally left empty
	}

	/* (non-Javadoc)
	 * @see org.apache.ranger.audit.provider.AuditProvider#init(java.util.Properties, java.lang.String)
	 */
	@Override
	public void init(Properties prop, String basePropertyName) {
		// intentionally left empty		
	}

	/* (non-Javadoc)
	 * @see org.apache.ranger.audit.provider.AuditProvider#waitToComplete(long)
	 */
	@Override
	public void waitToComplete(long timeout) {
		// intentionally left empty		
	}

	/* (non-Javadoc)
	 * @see org.apache.ranger.audit.provider.AuditProvider#getName()
	 */
	@Override
	public String getName() {
		return this.getClass().getName();
	}

	/* (non-Javadoc)
	* @see org.apache.ranger.audit.provider.AuditProvider#getAuditFileType()
	*/
	@Override
	public boolean logFile(File file) {
		return logFile(file);
	}

}
