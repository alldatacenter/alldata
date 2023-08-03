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

package org.apache.ranger.audit.provider.solr;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import org.apache.ranger.audit.destination.AuditDestination;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.audit.utils.SolrAppUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrAuditProvider extends AuditDestination {
	private static final Logger LOG = LoggerFactory.getLogger(SolrAuditProvider.class);

	public static final String AUDIT_MAX_QUEUE_SIZE_PROP = "xasecure.audit.solr.async.max.queue.size";
	public static final String AUDIT_MAX_FLUSH_INTERVAL_PROP = "xasecure.audit.solr.async.max.flush.interval.ms";
	public static final String AUDIT_RETRY_WAIT_PROP = "xasecure.audit.solr.retry.ms";

	static final Object lock = new Object();
	volatile SolrClient solrClient = null;
	Date lastConnectTime = null;
	long lastFailTime = 0;

	int retryWaitTime = 30000;

	public SolrAuditProvider() {
	}

	@Override
	public void init(Properties props) {
		LOG.info("init() called");
		super.init(props);

		retryWaitTime = MiscUtil.getIntProperty(props,
				AUDIT_RETRY_WAIT_PROP, retryWaitTime);
	}

	void connect() {
		SolrClient  me  = solrClient;
		if (me == null) {
			synchronized (lock) {
				me = solrClient;
				if (me == null) {
					final String solrURL = MiscUtil.getStringProperty(props,
							"xasecure.audit.solr.solr_url");

					if (lastConnectTime != null) {
						// Let's wait for enough time before retrying
						long diff = System.currentTimeMillis()
								- lastConnectTime.getTime();
						if (diff < retryWaitTime) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("Ignore connecting to solr url="
										+ solrURL + ", lastConnect=" + diff
										+ "ms");
							}
							return;
						}
					}
					lastConnectTime = new Date();

					if (solrURL == null || solrURL.isEmpty()) {
						LOG.error("Solr URL for Audit is empty");
						return;
					}

					try {
						// TODO: Need to support SolrCloud also
						solrClient = MiscUtil.executePrivilegedAction(new PrivilegedExceptionAction<SolrClient>() {
							@Override
							public SolrClient run()  throws Exception {
								HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
								builder.withBaseSolrUrl(solrURL);
								builder.allowCompression(true);
								builder.withConnectionTimeout(1000);
								HttpSolrClient httpSolrClient = builder.build();
								return httpSolrClient;
							};
						});

						me = solrClient;
					} catch (Throwable t) {
						LOG.error("Can't connect to Solr server. URL="
								+ solrURL, t);
					}
				}
			}
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
		if (!(event instanceof AuthzAuditEvent)) {
			LOG.error(event.getClass().getName()
					+ " audit event class type is not supported");
			return false;
		}
		AuthzAuditEvent authzEvent = (AuthzAuditEvent) event;
		// TODO: This should be done at a higher level

		if (authzEvent.getAgentHostname() == null) {
			authzEvent.setAgentHostname(MiscUtil.getHostname());
		}

		if (authzEvent.getLogType() == null) {
			authzEvent.setLogType("RangerAudit");
		}

		if (authzEvent.getEventId() == null) {
			authzEvent.setEventId(MiscUtil.generateUniqueId());
		}

		try {
			if (solrClient == null) {
				connect();
				if (solrClient == null) {
					// Solr is still not initialized. So need to throw error
					return false;
				}
			}

			if (lastFailTime > 0) {
				long diff = System.currentTimeMillis() - lastFailTime;
				if (diff < retryWaitTime) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Ignore sending audit. lastConnect=" + diff
								+ " ms");
					}
					return false;
				}
			}
			// Convert AuditEventBase to Solr document
			final SolrInputDocument document = toSolrDoc(authzEvent);
			final Collection<SolrInputDocument> docs = Collections.singletonList(document);
			final UpdateResponse response = SolrAppUtil.addDocsToSolr(solrClient, docs);

			if (response.getStatus() != 0) {
				lastFailTime = System.currentTimeMillis();

				// System.out.println("Response=" + response.toString()
				// + ", status= " + response.getStatus() + ", event="
				// + event);
				// throw new Exception("Aborting. event=" + event +
				// ", response="
				// + response.toString());
			} else {
				lastFailTime = 0;
			}

		} catch (Throwable t) {
			LOG.error("Error sending message to Solr", t);
			return false;
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
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#start()
	 */
	@Override
	public void start() {
		connect();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#stop()
	 */
	@Override
	public void stop() {
		LOG.info("SolrAuditProvider.stop() called..");
		try {
			if (solrClient != null) {
				solrClient.close();
			}
		} catch (IOException ioe) {
			LOG.error("Error while stopping slor!", ioe);
		} finally {
			solrClient = null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#waitToComplete()
	 */
	@Override
	public void waitToComplete() {

	}

	
	@Override
	public void waitToComplete(long timeout) {
		
	}
	
	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.ranger.audit.provider.AuditProvider#flush()
	 */
	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	SolrInputDocument toSolrDoc(AuthzAuditEvent auditEvent) {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("id", auditEvent.getEventId());
		doc.addField("access", auditEvent.getAccessType());
		doc.addField("enforcer", auditEvent.getAclEnforcer());
		doc.addField("agent", auditEvent.getAgentId());
		doc.addField("repo", auditEvent.getRepositoryName());
		doc.addField("sess", auditEvent.getSessionId());
		doc.addField("reqUser", auditEvent.getUser());
		doc.addField("reqData", auditEvent.getRequestData());
		doc.addField("resource", auditEvent.getResourcePath());
		doc.addField("cliIP", auditEvent.getClientIP());
		doc.addField("logType", auditEvent.getLogType());
		doc.addField("result", auditEvent.getAccessResult());
		doc.addField("policy", auditEvent.getPolicyId());
		doc.addField("repoType", auditEvent.getRepositoryType());
		doc.addField("resType", auditEvent.getResourceType());
		doc.addField("reason", auditEvent.getResultReason());
		doc.addField("action", auditEvent.getAction());
		doc.addField("evtTime", auditEvent.getEventTime());
		doc.addField("tags", auditEvent.getTags());
		doc.addField("cluster", auditEvent.getClusterName());
		doc.addField("zone", auditEvent.getZoneName());
		doc.addField("agentHost", auditEvent.getAgentHostname());
		return doc;
	}
	
	public boolean isAsync() {
		return true;
	}

}
