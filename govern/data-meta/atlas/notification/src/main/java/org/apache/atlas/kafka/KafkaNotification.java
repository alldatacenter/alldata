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
package org.apache.atlas.kafka;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasException;
import org.apache.atlas.notification.AbstractNotification;
import org.apache.atlas.notification.NotificationConsumer;
import org.apache.atlas.notification.NotificationException;
import org.apache.atlas.service.Service;
import org.apache.atlas.utils.KafkaUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.Future;

import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_PASSWORD_KEY;
import static org.apache.atlas.security.SecurityProperties.TLS_ENABLED;
import static org.apache.atlas.security.SecurityUtil.getPassword;

/**
 * Kafka specific access point to the Atlas notification framework.
 */
@Component
@Order(4)
public class KafkaNotification extends AbstractNotification implements Service {
    public static final Logger LOG = LoggerFactory.getLogger(KafkaNotification.class);

    public    static final String PROPERTY_PREFIX            = "atlas.kafka";
    public    static final String ATLAS_HOOK_TOPIC           = AtlasConfiguration.NOTIFICATION_HOOK_TOPIC_NAME.getString();
    public    static final String ATLAS_ENTITIES_TOPIC       = AtlasConfiguration.NOTIFICATION_ENTITIES_TOPIC_NAME.getString();
    protected static final String CONSUMER_GROUP_ID_PROPERTY = "group.id";

    private   static final String[] ATLAS_HOOK_CONSUMER_TOPICS     = AtlasConfiguration.NOTIFICATION_HOOK_CONSUMER_TOPIC_NAMES.getStringArray(ATLAS_HOOK_TOPIC);
    private   static final String[] ATLAS_ENTITIES_CONSUMER_TOPICS = AtlasConfiguration.NOTIFICATION_ENTITIES_CONSUMER_TOPIC_NAMES.getStringArray(ATLAS_ENTITIES_TOPIC);

    private static final String DEFAULT_CONSUMER_CLOSED_ERROR_MESSAGE = "This consumer has already been closed.";

    private static final Map<NotificationType, String> PRODUCER_TOPIC_MAP = new HashMap<NotificationType, String>() {
        {
            put(NotificationType.HOOK, ATLAS_HOOK_TOPIC);
            put(NotificationType.ENTITIES, ATLAS_ENTITIES_TOPIC);
        }
    };

    private static final Map<NotificationType, String[]> CONSUMER_TOPICS_MAP = new HashMap<NotificationType, String[]>() {
        {
            put(NotificationType.HOOK, trimAndPurge(ATLAS_HOOK_CONSUMER_TOPICS));
            put(NotificationType.ENTITIES, trimAndPurge(ATLAS_ENTITIES_CONSUMER_TOPICS));
        }
    };

    private final Properties                                 properties;
    private final Long                                       pollTimeOutMs;
    private final Map<NotificationType, List<KafkaConsumer>> consumers = new HashMap<>();
    private final Map<NotificationType, KafkaProducer>       producers = new HashMap<>();
    private       String                                     consumerClosedErrorMsg;

    // ----- Constructors ----------------------------------------------------

    /**
     * Construct a KafkaNotification.
     *
     * @param applicationProperties  the application properties used to configure Kafka
     *
     * @throws AtlasException if the notification interface can not be created
     */
    @Inject
    public KafkaNotification(Configuration applicationProperties) throws AtlasException {
        super(applicationProperties);

        LOG.info("==> KafkaNotification()");

        Configuration kafkaConf = ApplicationProperties.getSubsetConfiguration(applicationProperties, PROPERTY_PREFIX);

        properties             = ConfigurationConverter.getProperties(kafkaConf);
        pollTimeOutMs          = kafkaConf.getLong("poll.timeout.ms", 1000);
        consumerClosedErrorMsg = kafkaConf.getString("error.message.consumer_closed", DEFAULT_CONSUMER_CLOSED_ERROR_MESSAGE);

        //Override default configs
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        boolean oldApiCommitEnableFlag = kafkaConf.getBoolean("auto.commit.enable", false);

        //set old autocommit value if new autoCommit property is not set.
        properties.put("enable.auto.commit", kafkaConf.getBoolean("enable.auto.commit", oldApiCommitEnableFlag));
        properties.put("session.timeout.ms", kafkaConf.getString("session.timeout.ms", "30000"));

        if(applicationProperties.getBoolean(TLS_ENABLED, false)) {
            try {
                properties.put("ssl.truststore.password", getPassword(applicationProperties, TRUSTSTORE_PASSWORD_KEY));
            } catch (Exception e) {
                LOG.error("Exception while getpassword truststore.password ", e);
            }
        }

        // if no value is specified for max.poll.records, set to 1
        properties.put("max.poll.records", kafkaConf.getInt("max.poll.records", 1));

        KafkaUtils.setKafkaJAASProperties(applicationProperties, properties);

        LOG.info("<== KafkaNotification()");
    }

    @VisibleForTesting
    protected KafkaNotification(Properties properties) {
        super();

        LOG.info("==> KafkaNotification()");

        this.properties    = properties;
        this.pollTimeOutMs = 1000L;

        LOG.info("<== KafkaNotification()");
    }

    @VisibleForTesting
    String getProducerTopicName(NotificationType notificationType) {
        return PRODUCER_TOPIC_MAP.get(notificationType);
    }

    // ----- Service ---------------------------------------------------------

    @Override
    public void start() throws AtlasException {
        LOG.info("==> KafkaNotification.start()");

        LOG.info("<== KafkaNotification.start()");
    }

    @Override
    public void stop() {
        LOG.info("==> KafkaNotification.stop()");

        LOG.info("<== KafkaNotification.stop()");
    }


    // ----- NotificationInterface -------------------------------------------
    public boolean isReady(NotificationType notificationType) {
        try {
            KafkaProducer producer = getOrCreateProducer(notificationType);
            producer.metrics();
            return true;
        }
        catch (Exception exception) {
            LOG.error("Error: Connecting... {}", exception.getMessage());
            return false;
        }
    }

    @Override
    public <T> List<NotificationConsumer<T>> createConsumers(NotificationType notificationType, int numConsumers) {
        return createConsumers(notificationType, numConsumers, Boolean.valueOf(properties.getProperty("enable.auto.commit", properties.getProperty("auto.commit.enable","false"))));
    }

    @VisibleForTesting
    public <T> List<NotificationConsumer<T>> createConsumers(NotificationType notificationType, int numConsumers, boolean autoCommitEnabled) {
        LOG.info("==> KafkaNotification.createConsumers(notificationType={}, numConsumers={}, autoCommitEnabled={})", notificationType, numConsumers, autoCommitEnabled);

        String[] topics = CONSUMER_TOPICS_MAP.get(notificationType);

        if (numConsumers < topics.length) {
            LOG.warn("consumers count {} is fewer than number of topics {}. Creating {} consumers, so that consumer count is equal to number of topics.", numConsumers, topics.length, topics.length);

            numConsumers = topics.length;
        } else if (numConsumers > topics.length) {
            LOG.warn("consumers count {} is higher than number of topics {}. Creating {} consumers, so that consumer count is equal to number of topics", numConsumers, topics.length, topics.length);

            numConsumers = topics.length;
        }

        List<KafkaConsumer> notificationConsumers = this.consumers.get(notificationType);

        if (notificationConsumers == null) {
            notificationConsumers = new ArrayList<>(numConsumers);

            this.consumers.put(notificationType, notificationConsumers);
        }

        List<NotificationConsumer<T>> consumers          = new ArrayList<>();
        Properties                    consumerProperties = getConsumerProperties(notificationType);

        consumerProperties.put("enable.auto.commit", autoCommitEnabled);

        for (int i = 0; i < numConsumers; i++) {
            KafkaConsumer existingConsumer = notificationConsumers.size() > i ? notificationConsumers.get(i) : null;
            KafkaConsumer kafkaConsumer    = getOrCreateKafkaConsumer(existingConsumer, consumerProperties, notificationType, i);

            if (notificationConsumers.size() > i) {
                notificationConsumers.set(i, kafkaConsumer);
            } else {
                notificationConsumers.add(kafkaConsumer);
            }

            consumers.add(new AtlasKafkaConsumer(notificationType, kafkaConsumer, autoCommitEnabled, pollTimeOutMs));
        }

        LOG.info("<== KafkaNotification.createConsumers(notificationType={}, numConsumers={}, autoCommitEnabled={})", notificationType, numConsumers, autoCommitEnabled);

        return consumers;
    }

    @Override
    public void close() {
        LOG.info("==> KafkaNotification.close()");

        for (KafkaProducer producer : producers.values()) {
            if (producer != null) {
                try {
                    producer.close();
                } catch (Throwable t) {
                    LOG.error("failed to close Kafka producer. Ignoring", t);
                }
            }
        }

        producers.clear();

        LOG.info("<== KafkaNotification.close()");
    }


    // ----- AbstractNotification --------------------------------------------
    @Override
    public void sendInternal(NotificationType notificationType, List<String> messages) throws NotificationException {
        KafkaProducer producer = getOrCreateProducer(notificationType);

        sendInternalToProducer(producer, notificationType, messages);
    }

    @VisibleForTesting
    void sendInternalToProducer(Producer p, NotificationType notificationType, List<String> messages) throws NotificationException {
        String               topic           = PRODUCER_TOPIC_MAP.get(notificationType);
        List<MessageContext> messageContexts = new ArrayList<>();

        for (String message : messages) {
            ProducerRecord record = new ProducerRecord(topic, message);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sending message for topic {}: {}", topic, message);
            }

            Future future = p.send(record);

            messageContexts.add(new MessageContext(future, message));
        }

        List<String> failedMessages       = new ArrayList<>();
        Exception    lastFailureException = null;

        for (MessageContext context : messageContexts) {
            try {
                RecordMetadata response = context.getFuture().get();

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sent message for topic - {}, partition - {}, offset - {}", response.topic(), response.partition(), response.offset());
                }
            } catch (Exception e) {
                lastFailureException = e;

                failedMessages.add(context.getMessage());
            }
        }

        if (lastFailureException != null) {
            throw new NotificationException(lastFailureException, failedMessages);
        }
    }

    // Get properties for consumer request
    @VisibleForTesting
    public Properties getConsumerProperties(NotificationType notificationType) {
        // find the configured group id for the given notification type
        String groupId = properties.getProperty(notificationType.toString().toLowerCase() + "." + CONSUMER_GROUP_ID_PROPERTY);

        if (StringUtils.isEmpty(groupId)) {
            throw new IllegalStateException("No configuration group id set for the notification type " + notificationType);
        }

        Properties consumerProperties = new Properties();

        consumerProperties.putAll(properties);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        return consumerProperties;
    }

    @VisibleForTesting
    public KafkaConsumer getOrCreateKafkaConsumer(KafkaConsumer existingConsumer, Properties consumerProperties, NotificationType notificationType, int idxConsumer) {
        KafkaConsumer ret = existingConsumer;

        try {
            if (ret == null || !isKafkaConsumerOpen(ret)) {
                String[] topics = CONSUMER_TOPICS_MAP.get(notificationType);
                String   topic  = topics[idxConsumer % topics.length];

                LOG.debug("Creating new KafkaConsumer for topic : {}, index : {}", topic, idxConsumer);

                ret = new KafkaConsumer(consumerProperties);

                ret.subscribe(Arrays.asList(topic));
            }
        } catch (Exception ee) {
            LOG.error("Exception in getKafkaConsumer ", ee);
        }

        return ret;
    }

    private KafkaProducer getOrCreateProducer(NotificationType notificationType) {
        LOG.debug("==> KafkaNotification.getOrCreateProducer()");

        KafkaProducer ret = producers.get(notificationType);

        if (ret == null) {
            synchronized (this) {
                ret = producers.get(notificationType);

                if (ret == null) {
                    ret = new KafkaProducer(properties);

                    producers.put(notificationType, ret);
                }
            }
        }

        LOG.debug("<== KafkaNotification.getOrCreateProducer()");

        return ret;
    }

    public static String[] trimAndPurge(String[] strings)  {
        List<String> ret = new ArrayList<>();

        if (strings != null) {
            for (int i = 0; i < strings.length; i++) {
                String str = StringUtils.trim(strings[i]);

                if (StringUtils.isNotEmpty(str)) {
                    ret.add(str);
                }
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    private class MessageContext {
        private final Future<RecordMetadata> future;
        private final String                 message;

        public MessageContext(Future<RecordMetadata> future, String message) {
            this.future  = future;
            this.message = message;
        }

        public Future<RecordMetadata> getFuture() {
            return future;
        }

        public String getMessage() {
            return message;
        }
    }

    // kafka-client doesn't have method to check if consumer is open, hence checking list topics and catching exception
    private boolean isKafkaConsumerOpen(KafkaConsumer consumer) {
        boolean ret = true;

        try {
            consumer.listTopics();
        } catch (IllegalStateException ex) {
            if (ex.getMessage().equalsIgnoreCase(consumerClosedErrorMsg)) {
                ret = false;
            }
        }

        return ret;
    }

}
