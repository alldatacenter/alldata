/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.audit.provider.kafka;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.ranger.audit.destination.AuditDestination;
import org.apache.ranger.audit.model.AuditEventBase;
import org.apache.ranger.audit.model.AuthzAuditEvent;
import org.apache.ranger.audit.provider.MiscUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KafkaAuditProvider extends AuditDestination {
	private static final Logger LOG = LoggerFactory.getLogger(KafkaAuditProvider.class);

	public static final String AUDIT_MAX_QUEUE_SIZE_PROP = "xasecure.audit.kafka.async.max.queue.size";
	public static final String AUDIT_MAX_FLUSH_INTERVAL_PROP = "xasecure.audit.kafka.async.max.flush.interval.ms";
	public static final String AUDIT_KAFKA_BROKER_LIST = "xasecure.audit.kafka.broker_list";
	public static final String AUDIT_KAFKA_TOPIC_NAME = "xasecure.audit.kafka.topic_name";
	boolean initDone = false;

	Producer<String, String> producer = null;
	String topic = null;

	@Override
	public void init(Properties props) {
		LOG.info("init() called");
		super.init(props);

		topic = MiscUtil.getStringProperty(props,
				AUDIT_KAFKA_TOPIC_NAME);
		if (topic == null || topic.isEmpty()) {
			topic = "ranger_audits";
		}

		try {
			if (!initDone) {
				String brokerList = MiscUtil.getStringProperty(props,
						AUDIT_KAFKA_BROKER_LIST);
				if (brokerList == null || brokerList.isEmpty()) {
					brokerList = "localhost:9092";
				}

				final Map<String, Object> kakfaProps = new HashMap<String,Object>();
				kakfaProps.put("metadata.broker.list", brokerList);
				kakfaProps.put("serializer.class",
						"kafka.serializer.StringEncoder");
				// kakfaProps.put("partitioner.class",
				// "example.producer.SimplePartitioner");
				kakfaProps.put("request.required.acks", "1");

				LOG.info("Connecting to Kafka producer using properties:"
						+ kakfaProps.toString());

				producer  = MiscUtil.executePrivilegedAction(new PrivilegedAction<Producer<String, String>>() {
					@Override
					public Producer<String, String> run(){
						Producer<String, String> producer = new KafkaProducer<String, String>(kakfaProps);
						return producer;
					};
				});

				initDone = true;
			}
		} catch (Throwable t) {
			LOG.error("Error initializing kafka:", t);
		}
	}

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

		String message = MiscUtil.stringify(event);
		try {

			if (producer != null) {
				// TODO: Add partition key
				final ProducerRecord<String, String> keyedMessage = new ProducerRecord<String, String>(
						topic, message);

				MiscUtil.executePrivilegedAction(new PrivilegedAction<Void>() {
					@Override
					public Void run(){
						producer.send(keyedMessage);
						return null;
					};
				});

			} else {
				LOG.info("AUDIT LOG (Kafka Down):" + message);
			}
		} catch (Throwable t) {
			LOG.error("Error sending message to Kafka topic. topic=" + topic
					+ ", message=" + message, t);
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

	@Override
	public void start() {
		LOG.info("start() called");
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		LOG.info("stop() called");
		if (producer != null) {
			try {
				MiscUtil.executePrivilegedAction(new PrivilegedAction<Void>() {
					@Override
					public Void run() {
						producer.close();
						return null;
					};
				});
			} catch (Throwable t) {
				LOG.error("Error closing Kafka producer");
			}
		}
	}

	@Override
	public void waitToComplete() {
		LOG.info("waitToComplete() called");
	}
	
	@Override
	public void waitToComplete(long timeout) {
	}

	@Override
	public void flush() {
		LOG.info("flush() called");

	}

	public boolean isAsync() {
		return true;
	}

}
