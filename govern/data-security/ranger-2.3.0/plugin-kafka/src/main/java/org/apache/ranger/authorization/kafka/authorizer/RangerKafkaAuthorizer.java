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

package org.apache.ranger.authorization.kafka.authorizer;

import java.util.Date;
import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import scala.collection.immutable.HashSet;
import scala.collection.immutable.Set;
import kafka.security.auth.*;
import kafka.network.RequestChannel.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.ranger.audit.provider.MiscUtil;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResourceImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerKafkaAuthorizer implements Authorizer {
	private static final Logger logger = LoggerFactory
			.getLogger(RangerKafkaAuthorizer.class);
	private static final Logger PERF_KAFKAAUTH_REQUEST_LOG = RangerPerfTracer.getPerfLogger("kafkaauth.request");

	public static final String KEY_TOPIC = "topic";
	public static final String KEY_CLUSTER = "cluster";
	public static final String KEY_CONSUMER_GROUP = "consumergroup";
	public static final String KEY_TRANSACTIONALID = "transactionalid";
	public static final String KEY_DELEGATIONTOKEN = "delegationtoken";

	public static final String ACCESS_TYPE_READ = "consume";
	public static final String ACCESS_TYPE_WRITE = "publish";
	public static final String ACCESS_TYPE_CREATE = "create";
	public static final String ACCESS_TYPE_DELETE = "delete";
	public static final String ACCESS_TYPE_CONFIGURE = "configure";
	public static final String ACCESS_TYPE_DESCRIBE = "describe";
	public static final String ACCESS_TYPE_DESCRIBE_CONFIGS = "describe_configs";
	public static final String ACCESS_TYPE_ALTER_CONFIGS    = "alter_configs";
	public static final String ACCESS_TYPE_IDEMPOTENT_WRITE = "idempotent_write";
	public static final String ACCESS_TYPE_CLUSTER_ACTION   = "cluster_action";

	private static volatile RangerBasePlugin rangerPlugin = null;
	RangerKafkaAuditHandler auditHandler = null;

	public RangerKafkaAuthorizer() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see kafka.security.auth.Authorizer#configure(Map<String, Object>)
	 */
	@Override
	public void configure(Map<String, ?> configs) {
		RangerBasePlugin me = rangerPlugin;
		if (me == null) {
			synchronized(RangerKafkaAuthorizer.class) {
				me = rangerPlugin;
				if (me == null) {
					try {
						// Possible to override JAAS configuration which is used by Ranger, otherwise
						// SASL_PLAINTEXT is used, which force Kafka to use 'sasl_plaintext.KafkaServer',
						// if it's not defined, then it reverts to 'KafkaServer' configuration.
						final Object jaasContext = configs.get("ranger.jaas.context");
						final String listenerName = (jaasContext instanceof String
								&& StringUtils.isNotEmpty((String) jaasContext)) ? (String) jaasContext
										: SecurityProtocol.SASL_PLAINTEXT.name();
						final String saslMechanism = SaslConfigs.GSSAPI_MECHANISM;
						JaasContext context = JaasContext.loadServerContext(new ListenerName(listenerName), saslMechanism, configs);
						MiscUtil.setUGIFromJAASConfig(context.name());
						logger.info("LoginUser=" + MiscUtil.getUGILoginUser());
					} catch (Throwable t) {
						logger.error("Error getting principal.", t);
					}
					me = rangerPlugin = new RangerBasePlugin("kafka", "kafka");
				}
			}
		}
		logger.info("Calling plugin.init()");
		rangerPlugin.init();
		auditHandler = new RangerKafkaAuditHandler();
		rangerPlugin.setResultProcessor(auditHandler);
	}

	@Override
	public void close() {
		logger.info("close() called on authorizer.");
		try {
			if (rangerPlugin != null) {
				rangerPlugin.cleanup();
			}
		} catch (Throwable t) {
			logger.error("Error closing RangerPlugin.", t);
		}
	}

	@Override
	public boolean authorize(Session session, Operation operation,
			Resource resource) {

		if (rangerPlugin == null) {
			MiscUtil.logErrorMessageByInterval(logger,
					"Authorizer is still not initialized");
			return false;
		}

		RangerPerfTracer perf = null;

		if(RangerPerfTracer.isPerfTraceEnabled(PERF_KAFKAAUTH_REQUEST_LOG)) {
			perf = RangerPerfTracer.getPerfTracer(PERF_KAFKAAUTH_REQUEST_LOG, "RangerKafkaAuthorizer.authorize(resource=" + resource + ")");
		}
		String userName = null;
		if (session.principal() != null) {
			userName = session.principal().getName();
		}
		java.util.Set<String> userGroups = MiscUtil
				.getGroupsForRequestUser(userName);
		String ip = session.clientAddress().getHostAddress();

		// skip leading slash
		if (StringUtils.isNotEmpty(ip) && ip.charAt(0) == '/') {
			ip = ip.substring(1);
		}

		Date eventTime = new Date();
		String accessType = mapToRangerAccessType(operation);
		boolean validationFailed = false;
		String validationStr = "";

		if (accessType == null) {
			if (MiscUtil.logErrorMessageByInterval(logger,
					"Unsupported access type. operation=" + operation)) {
				logger.error("Unsupported access type. session=" + session
						+ ", operation=" + operation + ", resource=" + resource);
			}
			validationFailed = true;
			validationStr += "Unsupported access type. operation=" + operation;
		}
		String action = accessType;

		RangerAccessRequestImpl rangerRequest = new RangerAccessRequestImpl();
		rangerRequest.setUser(userName);
		rangerRequest.setUserGroups(userGroups);
		rangerRequest.setClientIPAddress(ip);
		rangerRequest.setAccessTime(eventTime);

		RangerAccessResourceImpl rangerResource = new RangerAccessResourceImpl();
		rangerRequest.setResource(rangerResource);
		rangerRequest.setAccessType(accessType);
		rangerRequest.setAction(action);
		rangerRequest.setRequestData(resource.name());

		if (resource.resourceType().equals(Topic$.MODULE$)) {
			rangerResource.setValue(KEY_TOPIC, resource.name());
		} else if (resource.resourceType().equals(Cluster$.MODULE$)) {
			rangerResource.setValue(KEY_CLUSTER, resource.name());
		} else if (resource.resourceType().equals(Group$.MODULE$)) {
			rangerResource.setValue(KEY_CONSUMER_GROUP, resource.name());
		} else if (resource.resourceType().equals(TransactionalId$.MODULE$)) {
			rangerResource.setValue(KEY_TRANSACTIONALID, resource.name());
		} else if (resource.resourceType().equals(DelegationToken$.MODULE$)) {
			rangerResource.setValue(KEY_DELEGATIONTOKEN, resource.name());
		} else {
			logger.error("Unsupported resourceType=" + resource.resourceType());
			validationFailed = true;
		}

		boolean returnValue = false;
		if (validationFailed) {
			MiscUtil.logErrorMessageByInterval(logger, validationStr
					+ ", request=" + rangerRequest);
		} else {

			try {
				RangerAccessResult result = rangerPlugin
						.isAccessAllowed(rangerRequest);
				if (result == null) {
					logger.error("Ranger Plugin returned null. Returning false");
				} else {
					returnValue = result.getIsAllowed();
				}
			} catch (Throwable t) {
				logger.error("Error while calling isAccessAllowed(). request="
						+ rangerRequest, t);
			} finally {
				auditHandler.flushAudit();
			}
		}
		RangerPerfTracer.log(perf);

		if (logger.isDebugEnabled()) {
			logger.debug("rangerRequest=" + rangerRequest + ", return="
					+ returnValue);
		}
		return returnValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * kafka.security.auth.Authorizer#addAcls(scala.collection.immutable.Set,
	 * kafka.security.auth.Resource)
	 */
	@Override
	public void addAcls(Set<Acl> acls, Resource resource) {
		logger.error("addAcls(Set<Acl>, Resource) is not supported by Ranger for Kafka");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * kafka.security.auth.Authorizer#removeAcls(scala.collection.immutable.Set,
	 * kafka.security.auth.Resource)
	 */
	@Override
	public boolean removeAcls(Set<Acl> acls, Resource resource) {
		logger.error("removeAcls(Set<Acl>, Resource) is not supported by Ranger for Kafka");
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * kafka.security.auth.Authorizer#removeAcls(kafka.security.auth.Resource)
	 */
	@Override
	public boolean removeAcls(Resource resource) {
		logger.error("removeAcls(Resource) is not supported by Ranger for Kafka");
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see kafka.security.auth.Authorizer#getAcls(kafka.security.auth.Resource)
	 */
	@Override
	public Set<Acl> getAcls(Resource resource) {
		Set<Acl> aclList = new HashSet<Acl>();
		logger.error("getAcls(Resource) is not supported by Ranger for Kafka");

		return aclList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * kafka.security.auth.Authorizer#getAcls(kafka.security.auth.KafkaPrincipal
	 * )
	 */
	@Override
	public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls(
			KafkaPrincipal principal) {
		scala.collection.immutable.Map<Resource, Set<Acl>> aclList = new scala.collection.immutable.HashMap<Resource, Set<Acl>>();
		logger.error("getAcls(KafkaPrincipal) is not supported by Ranger for Kafka");
		return aclList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see kafka.security.auth.Authorizer#getAcls()
	 */
	@Override
	public scala.collection.immutable.Map<Resource, Set<Acl>> getAcls() {
		scala.collection.immutable.Map<Resource, Set<Acl>> aclList = new scala.collection.immutable.HashMap<Resource, Set<Acl>>();
		logger.error("getAcls() is not supported by Ranger for Kafka");
		return aclList;
	}

	/**
	 * @param operation
	 * @return
	 */
	private String mapToRangerAccessType(Operation operation) {
		if (operation.equals(Read$.MODULE$)) {
			return ACCESS_TYPE_READ;
		} else if (operation.equals(Write$.MODULE$)) {
			return ACCESS_TYPE_WRITE;
		} else if (operation.equals(Alter$.MODULE$)) {
			return ACCESS_TYPE_CONFIGURE;
		} else if (operation.equals(Describe$.MODULE$)) {
			return ACCESS_TYPE_DESCRIBE;
		} else if (operation.equals(ClusterAction$.MODULE$)) {
			return ACCESS_TYPE_CLUSTER_ACTION;
		} else if (operation.equals(Create$.MODULE$)) {
			return ACCESS_TYPE_CREATE;
		} else if (operation.equals(Delete$.MODULE$)) {
			return ACCESS_TYPE_DELETE;
		} else if (operation.equals(DescribeConfigs$.MODULE$)) {
			return ACCESS_TYPE_DESCRIBE_CONFIGS;
		} else if (operation.equals(AlterConfigs$.MODULE$)) {
			return ACCESS_TYPE_ALTER_CONFIGS;
		} else if (operation.equals(IdempotentWrite$.MODULE$)) {
			return ACCESS_TYPE_IDEMPOTENT_WRITE;
		}
		return null;
	}
}
