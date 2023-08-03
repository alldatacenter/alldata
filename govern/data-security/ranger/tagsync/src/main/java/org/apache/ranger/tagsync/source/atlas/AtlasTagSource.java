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

package org.apache.ranger.tagsync.source.atlas;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.atlas.kafka.NotificationProvider;
import org.apache.atlas.model.notification.EntityNotification;
import org.apache.atlas.notification.NotificationConsumer;
import org.apache.atlas.notification.NotificationInterface;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.tagsync.model.AbstractTagSource;
import org.apache.atlas.kafka.AtlasKafkaMessage;
import org.apache.kafka.common.TopicPartition;
import org.apache.ranger.tagsync.process.TagSyncConfig;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntityWithTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AtlasTagSource extends AbstractTagSource {
	private static final Logger LOG = LoggerFactory.getLogger(AtlasTagSource.class);

	public static final String TAGSYNC_ATLAS_PROPERTIES_FILE_NAME = "atlas-application.properties";

	public static final String TAGSYNC_ATLAS_KAFKA_ENDPOINTS = "atlas.kafka.bootstrap.servers";
	public static final String TAGSYNC_ATLAS_ZOOKEEPER_ENDPOINT = "atlas.kafka.zookeeper.connect";
	public static final String TAGSYNC_ATLAS_CONSUMER_GROUP = "atlas.kafka.entities.group.id";

	public static final int    MAX_WAIT_TIME_IN_MILLIS = 1000;

	private             int    maxBatchSize;

	private ConsumerRunnable consumerTask;
	private Thread myThread = null;

	@Override
	public boolean initialize(Properties properties) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AtlasTagSource.initialize()");
		}

		Properties atlasProperties = new Properties();

		boolean ret = AtlasResourceMapperUtil.initializeAtlasResourceMappers(properties);

		if (ret) {

			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TAGSYNC_ATLAS_PROPERTIES_FILE_NAME);

			if (inputStream != null) {
				try {
					atlasProperties.load(inputStream);
				} catch (Exception exception) {
					ret = false;
					LOG.error("Cannot load Atlas application properties file, file-name:" + TAGSYNC_ATLAS_PROPERTIES_FILE_NAME, exception);
				} finally {
					try {
						inputStream.close();
					} catch (IOException ioException) {
						LOG.error("Cannot close Atlas application properties file, file-name:\" + TAGSYNC_ATLAS_PROPERTIES_FILE_NAME", ioException);
					}
				}
			} else {
				ret = false;
				LOG.error("Cannot find Atlas application properties file");
			}
		}

		if (ret) {
			if (StringUtils.isBlank(atlasProperties.getProperty(TAGSYNC_ATLAS_KAFKA_ENDPOINTS))) {
				ret = false;
				LOG.error("Value of property '" + TAGSYNC_ATLAS_KAFKA_ENDPOINTS + "' is not specified!");
			}
			if (StringUtils.isBlank(atlasProperties.getProperty(TAGSYNC_ATLAS_ZOOKEEPER_ENDPOINT))) {
				ret = false;
				LOG.error("Value of property '" + TAGSYNC_ATLAS_ZOOKEEPER_ENDPOINT + "' is not specified!");
			}
			if (StringUtils.isBlank(atlasProperties.getProperty(TAGSYNC_ATLAS_CONSUMER_GROUP))) {
				ret = false;
				LOG.error("Value of property '" + TAGSYNC_ATLAS_CONSUMER_GROUP + "' is not specified!");
			}
		}

		if (ret) {
			NotificationInterface notification = NotificationProvider.get();
			List<NotificationConsumer<EntityNotification>> iterators = notification.createConsumers(NotificationInterface.NotificationType.ENTITIES, 1);

			consumerTask = new ConsumerRunnable(iterators.get(0));
		}

		maxBatchSize = TagSyncConfig.getSinkMaxBatchSize(properties);

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AtlasTagSource.initialize(), result=" + ret);
		}
		return ret;
	}

	@Override
	public boolean start() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> AtlasTagSource.start()");
		}
		if (consumerTask == null) {
			LOG.error("No consumerTask!!!");
		} else {
			myThread = new Thread(consumerTask);
			myThread.setDaemon(true);
			myThread.start();
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== AtlasTagSource.start()");
		}
		return myThread != null;
	}

	@Override
	public void stop() {
		if (myThread != null && myThread.isAlive()) {
			myThread.interrupt();
		}
	}

	private static String getPrintableEntityNotification(EntityNotificationWrapper notification) {
		StringBuilder sb = new StringBuilder();

		sb.append("{ Notification-Type: ").append(notification.getOpType()).append(", ");
        RangerAtlasEntityWithTags entityWithTags = new RangerAtlasEntityWithTags(notification);
        sb.append(entityWithTags.toString());

		sb.append("}");
		return sb.toString();
	}

	private class ConsumerRunnable implements Runnable {

		private final NotificationConsumer<EntityNotification> consumer;

		private final List<RangerAtlasEntityWithTags>             atlasEntitiesWithTags = new ArrayList<>();
		private final List<AtlasKafkaMessage<EntityNotification>> messages              = new ArrayList<>();

		private long    offsetOfLastMessageDeliveredToRanger = -1L;
		private long    offsetOfLastMessageCommittedToKafka  = -1L;

		private boolean isHandlingDeleteOps   = false;

		private ConsumerRunnable(NotificationConsumer<EntityNotification> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void run() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> ConsumerRunnable.run()");
			}

			while (true) {

				try {
					List<AtlasKafkaMessage<EntityNotification>> newMessages = consumer.receive(MAX_WAIT_TIME_IN_MILLIS);

					if (newMessages.size() == 0) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("AtlasTagSource.ConsumerRunnable.run: no message from NotificationConsumer within " + MAX_WAIT_TIME_IN_MILLIS + " milliseconds");
						}
						if (CollectionUtils.isNotEmpty(atlasEntitiesWithTags)) {
							buildAndUploadServiceTags();
						}
					} else {
						for (AtlasKafkaMessage<EntityNotification> message : newMessages) {
							EntityNotification notification = message != null ? message.getMessage() : null;

							if (notification != null) {
								EntityNotificationWrapper notificationWrapper = null;
								try {
									notificationWrapper = new EntityNotificationWrapper(notification);
								} catch (Throwable e) {
									LOG.error("notification:[" + notification + "] has some issues..perhaps null entity??", e);
								}
								if (notificationWrapper != null) {
									if (LOG.isDebugEnabled()) {
										LOG.debug("Message-offset=" + message.getOffset() + ", Notification=" + getPrintableEntityNotification(notificationWrapper));
									}

									RangerAtlasEntityWithTags entityWithTags = new RangerAtlasEntityWithTags(notificationWrapper);

									if ((notificationWrapper.getIsEntityDeleteOp() && !isHandlingDeleteOps) || (!notificationWrapper.getIsEntityDeleteOp() && isHandlingDeleteOps)) {
										buildAndUploadServiceTags();
										isHandlingDeleteOps = !isHandlingDeleteOps;
									}

									atlasEntitiesWithTags.add(entityWithTags);
									messages.add(message);
								}
							} else {
								LOG.error("Null entityNotification received from Kafka!! Ignoring..");
							}
						}
						if (CollectionUtils.isNotEmpty(atlasEntitiesWithTags) && atlasEntitiesWithTags.size() >= maxBatchSize) {
							buildAndUploadServiceTags();
						}
					}

				} catch (Exception exception) {
					LOG.error("Caught exception..: ", exception);
					// If transient error, retry after short interval
					try {
						Thread.sleep(100);
					} catch (InterruptedException interrupted) {
						LOG.error("Interrupted: ", interrupted);
						LOG.error("Returning from thread. May cause process to be up but not processing events!!");
						return;
					}
				}
			}
		}

		private void buildAndUploadServiceTags() throws Exception {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> buildAndUploadServiceTags()");
			}

			commitToKafka();

			Map<String, ServiceTags> serviceTagsMap = AtlasNotificationMapper.processAtlasEntities(atlasEntitiesWithTags);

			if (MapUtils.isNotEmpty(serviceTagsMap)) {
				if (serviceTagsMap.size() != 1) {
					LOG.warn("Unexpected!! Notifications for more than one service received by AtlasTagSource.. Service-Names:[" + serviceTagsMap.keySet() + "]");
				}
				for (Map.Entry<String, ServiceTags> entry : serviceTagsMap.entrySet()) {
					if (isHandlingDeleteOps) {
						entry.getValue().setOp(ServiceTags.OP_DELETE);
						entry.getValue().setTagDefinitions(Collections.EMPTY_MAP);
						entry.getValue().setTags(Collections.EMPTY_MAP);
					} else {
						entry.getValue().setOp(ServiceTags.OP_ADD_OR_UPDATE);
					}

					if (LOG.isDebugEnabled()) {
						Gson gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").setPrettyPrinting().create();
						String serviceTagsString = gsonBuilder.toJson(entry.getValue());

						LOG.debug("serviceTags=" + serviceTagsString);
					}
					updateSink(entry.getValue());
				}
				offsetOfLastMessageDeliveredToRanger = messages.get(messages.size()-1).getOffset();
				if (LOG.isDebugEnabled()) {
					LOG.debug("Completed processing batch of messages of size:[" + messages.size() + "] received from NotificationConsumer");
				}

				commitToKafka();
			}

			atlasEntitiesWithTags.clear();
			messages.clear();

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== buildAndUploadServiceTags()");
			}
		}

		private void commitToKafka() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> commitToKafka()");
			}

			for (AtlasKafkaMessage<EntityNotification> message : messages) {
				if (message.getOffset() > offsetOfLastMessageCommittedToKafka) {
					if (message.getOffset() <= offsetOfLastMessageDeliveredToRanger) {
						// Already delivered to Ranger
						TopicPartition partition = new TopicPartition("ATLAS_ENTITIES", message.getPartition());
						try {
							if (LOG.isDebugEnabled()) {
								LOG.debug("Committing message with offset:[" + message.getOffset() + "] to Kafka");
							}
							consumer.commit(partition, message.getOffset());
							offsetOfLastMessageCommittedToKafka = message.getOffset();
						} catch (Exception commitException) {
							LOG.warn("Ranger tagsync already processed message at offset " + message.getOffset() + ". Ignoring failure in committing this message and continuing to process next message", commitException);
							LOG.warn("This will cause Kafka to deliver this message:[" + message.getOffset() + "] repeatedly!! This may be unrecoverable error!!");
						}
					} else {
						break;
					}
				}
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== commitToKafka()");
			}
		}
	}
}

