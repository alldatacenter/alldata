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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.ranger.audit.model.AuditEventBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiDestAuditProvider extends BaseAuditHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(MultiDestAuditProvider.class);

	protected List<AuditHandler> mProviders = new ArrayList<AuditHandler>();
	static final String DEFAULT_NAME = "multi_dest";

	public MultiDestAuditProvider() {
		LOG.info("MultiDestAuditProvider: creating..");
		setName(DEFAULT_NAME);
	}

	public MultiDestAuditProvider(AuditHandler provider) {
		LOG.info("MultiDestAuditProvider(): provider="
				+ (provider == null ? null : provider.getName()));
		setName(DEFAULT_NAME);
		addAuditProvider(provider);
	}

	@Override
	public void init(Properties props) {
		LOG.info("MultiDestAuditProvider.init()");

		super.init(props);

		for (AuditHandler provider : mProviders) {
			try {
				provider.init(props);
			} catch (Throwable excp) {
				LOG.info("MultiDestAuditProvider.init(): failed "
						+ provider.getClass().getCanonicalName() + ")", excp);
			}
		}
	}

	@Override
	public void setParentPath(String parentPath) {
		super.setParentPath(parentPath);
		for (AuditHandler provider : mProviders) {
			if (provider instanceof BaseAuditHandler) {
				BaseAuditHandler baseAuditHander = (BaseAuditHandler) provider;
				baseAuditHander.setParentPath(getName());
			}
		}
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		for (AuditHandler provider : mProviders) {
			if (provider instanceof BaseAuditHandler) {
				BaseAuditHandler baseAuditHander = (BaseAuditHandler) provider;
				baseAuditHander.setParentPath(getName());
			}
		}
	}

	public void addAuditProvider(AuditHandler provider) {
		if (provider != null) {
			LOG.info("MultiDestAuditProvider.addAuditProvider(providerType="
					+ provider.getClass().getCanonicalName() + ")");

			mProviders.add(provider);
			if (provider instanceof BaseAuditHandler) {
				BaseAuditHandler baseAuditHander = (BaseAuditHandler) provider;
				baseAuditHander.setParentPath(getName());
			}
		}
	}

	public void addAuditProviders(List<AuditHandler> providers) {
		if (providers != null) {
			for (AuditHandler provider : providers) {
				LOG.info("Adding " + provider.getName()
						+ " as consumer to MultiDestination " + getName());
				addAuditProvider(provider);
			}
		}
	}

	@Override
	public boolean log(AuditEventBase event) {
		for (AuditHandler provider : mProviders) {
			try {
				provider.log(event);
			} catch (Throwable excp) {
				logFailedEvent(event, excp);
			}
		}
		return true;
	}

	@Override
	public boolean log(Collection<AuditEventBase> events) {
		for (AuditHandler provider : mProviders) {
			try {
				provider.log(events);
			} catch (Throwable excp) {
				logFailedEvent(events, excp);
			}
		}
		return true;
	}

	@Override
	public boolean logJSON(String event) {
		for (AuditHandler provider : mProviders) {
			try {
				provider.logJSON(event);
			} catch (Throwable excp) {
				logFailedEventJSON(event, excp);
			}
		}
		return true;
	}

	@Override
	public boolean logJSON(Collection<String> events) {
		for (AuditHandler provider : mProviders) {
			try {
				provider.logJSON(events);
			} catch (Throwable excp) {
				logFailedEventJSON(events, excp);
			}
		}
		return true;
	}


	@Override
	public boolean logFile(File file) {
		for (AuditHandler provider : mProviders) {
			try {
				provider.logFile(file);
			} catch (Throwable excp) {
			   logFailedEventJSON(file.getAbsolutePath(), excp);
			}
		}
		return true;
	}

	@Override
	public void start() {
		for (AuditHandler provider : mProviders) {
			try {
				provider.start();
			} catch (Throwable excp) {
				LOG.error("MultiDestAuditProvider.start(): failed for provider { "
						+ provider.getClass().getName() + " }", excp);
			}
		}
	}

	@Override
	public void stop() {
		for (AuditHandler provider : mProviders) {
			try {
				provider.stop();
			} catch (Throwable excp) {
				LOG.error("MultiDestAuditProvider.stop(): failed for provider { "
						+ provider.getClass().getName() + " }", excp);
			}
		}
	}

	@Override
	public void waitToComplete() {
		for (AuditHandler provider : mProviders) {
			try {
				provider.waitToComplete();
			} catch (Throwable excp) {
				LOG.error(
						"MultiDestAuditProvider.waitToComplete(): failed for provider { "
								+ provider.getClass().getName() + " }", excp);
			}
		}
	}

	@Override
	public void waitToComplete(long timeout) {
		for (AuditHandler provider : mProviders) {
			try {
				provider.waitToComplete(timeout);
			} catch (Throwable excp) {
				LOG.error(
						"MultiDestAuditProvider.waitToComplete(): failed for provider { "
								+ provider.getClass().getName() + " }", excp);
			}
		}
	}

	@Override
	public void flush() {
		for (AuditHandler provider : mProviders) {
			try {
				provider.flush();
			} catch (Throwable excp) {
				LOG.error("MultiDestAuditProvider.flush(): failed for provider { "
						+ provider.getClass().getName() + " }", excp);
			}
		}
	}
}
