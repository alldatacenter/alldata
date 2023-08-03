/*
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

package org.apache.flink.connector.pulsar.source.config;

import org.apache.flink.connector.pulsar.source.PulsarSourceOptions;
import org.apache.flink.connector.pulsar.common.config.PulsarConfigValidator;
import org.apache.flink.connector.pulsar.common.config.PulsarOptions;
import org.apache.flink.annotation.Internal;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/** Create source related {@link Consumer} and validate config. */
@Internal
public final class PulsarSourceConfigUtils {

    private static final BatchReceivePolicy DISABLED_BATCH_RECEIVE_POLICY =
            BatchReceivePolicy.builder()
                    .timeout(0, TimeUnit.MILLISECONDS)
                    .maxNumMessages(1)
                    .build();

    private PulsarSourceConfigUtils() {
        // No need to create an instance.
    }

    public static final PulsarConfigValidator SOURCE_CONFIG_VALIDATOR =
            PulsarConfigValidator.builder()
                    .requiredOption(PulsarOptions.PULSAR_SERVICE_URL)
                    .requiredOption(PulsarOptions.PULSAR_ADMIN_URL)
                    .requiredOption(PulsarSourceOptions.PULSAR_SUBSCRIPTION_NAME)
                    .conflictOptions(PulsarOptions.PULSAR_AUTH_PARAMS, PulsarOptions.PULSAR_AUTH_PARAM_MAP)
                    .build();

    /** Create a pulsar consumer builder by using the given Configuration. */
    public static <T> ConsumerBuilder<T> createConsumerBuilder(
            PulsarClient client, Schema<T> schema, SourceConfiguration configuration) {
        ConsumerBuilder<T> builder = new PulsarConsumerBuilder<>(client, schema);

        configuration.useOption(PulsarSourceOptions.PULSAR_SUBSCRIPTION_NAME, builder::subscriptionName);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_ACK_TIMEOUT_MILLIS, v -> builder.ackTimeout(v, MILLISECONDS));
        configuration.useOption(PulsarSourceOptions.PULSAR_ACK_RECEIPT_ENABLED, builder::isAckReceiptEnabled);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_TICK_DURATION_MILLIS, v -> builder.ackTimeoutTickTime(v, MILLISECONDS));
        configuration.useOption(
                PulsarSourceOptions.PULSAR_NEGATIVE_ACK_REDELIVERY_DELAY_MICROS,
                v -> builder.negativeAckRedeliveryDelay(v, MICROSECONDS));
        configuration.useOption(PulsarSourceOptions.PULSAR_SUBSCRIPTION_TYPE, builder::subscriptionType);
        configuration.useOption(PulsarSourceOptions.PULSAR_SUBSCRIPTION_MODE, builder::subscriptionMode);
        configuration.useOption(PulsarSourceOptions.PULSAR_CRYPTO_FAILURE_ACTION, builder::cryptoFailureAction);
        configuration.useOption(PulsarSourceOptions.PULSAR_RECEIVER_QUEUE_SIZE, builder::receiverQueueSize);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_ACKNOWLEDGEMENTS_GROUP_TIME_MICROS,
                v -> builder.acknowledgmentGroupTime(v, MICROSECONDS));
        configuration.useOption(
                PulsarSourceOptions.PULSAR_REPLICATE_SUBSCRIPTION_STATE, builder::replicateSubscriptionState);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_MAX_TOTAL_RECEIVER_QUEUE_SIZE_ACROSS_PARTITIONS,
                builder::maxTotalReceiverQueueSizeAcrossPartitions);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_CONSUMER_NAME,
                consumerName -> String.format(consumerName, UUID.randomUUID()),
                builder::consumerName);
        configuration.useOption(PulsarSourceOptions.PULSAR_READ_COMPACTED, builder::readCompacted);
        configuration.useOption(PulsarSourceOptions.PULSAR_PRIORITY_LEVEL, builder::priorityLevel);
        createDeadLetterPolicy(configuration).ifPresent(builder::deadLetterPolicy);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_AUTO_UPDATE_PARTITIONS_INTERVAL_SECONDS,
                v -> builder.autoUpdatePartitionsInterval(v, SECONDS));
        configuration.useOption(PulsarSourceOptions.PULSAR_RETRY_ENABLE, builder::enableRetry);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_MAX_PENDING_CHUNKED_MESSAGE, builder::maxPendingChunkedMessage);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_AUTO_ACK_OLDEST_CHUNKED_MESSAGE_ON_QUEUE_FULL,
                builder::autoAckOldestChunkedMessageOnQueueFull);
        configuration.useOption(
                PulsarSourceOptions.PULSAR_EXPIRE_TIME_OF_INCOMPLETE_CHUNKED_MESSAGE_MILLIS,
                v -> builder.expireTimeOfIncompleteChunkedMessage(v, MILLISECONDS));
        configuration.useOption(PulsarSourceOptions.PULSAR_POOL_MESSAGES, builder::poolMessages);

        Map<String, String> properties = configuration.getProperties(PulsarSourceOptions.PULSAR_CONSUMER_PROPERTIES);
        if (!properties.isEmpty()) {
            builder.properties(properties);
        }

        // Flink connector doesn't need any batch receiving behaviours.
        // Disable the batch-receive timer for the Consumer instance.
        builder.batchReceivePolicy(DISABLED_BATCH_RECEIVE_POLICY);

        return builder;
    }

    private static Optional<DeadLetterPolicy> createDeadLetterPolicy(
            SourceConfiguration configuration) {
        if (configuration.contains(PulsarSourceOptions.PULSAR_MAX_REDELIVER_COUNT)
                || configuration.contains(PulsarSourceOptions.PULSAR_RETRY_LETTER_TOPIC)
                || configuration.contains(PulsarSourceOptions.PULSAR_DEAD_LETTER_TOPIC)) {
            DeadLetterPolicy.DeadLetterPolicyBuilder builder = DeadLetterPolicy.builder();

            configuration.useOption(PulsarSourceOptions.PULSAR_MAX_REDELIVER_COUNT, builder::maxRedeliverCount);
            configuration.useOption(PulsarSourceOptions.PULSAR_RETRY_LETTER_TOPIC, builder::retryLetterTopic);
            configuration.useOption(PulsarSourceOptions.PULSAR_DEAD_LETTER_TOPIC, builder::deadLetterTopic);

            return Optional.of(builder.build());
        } else {
            return Optional.empty();
        }
    }
}
