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

package com.netease.arctic.flink.read.source.log.pulsar;

import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.connector.pulsar.source.PulsarSourceBuilder;
import org.apache.flink.connector.pulsar.source.config.SourceConfiguration;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.connector.pulsar.source.enumerator.topic.range.FullRangeGenerator;
import org.apache.flink.connector.pulsar.source.enumerator.topic.range.SplitRangeGenerator;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE_EARLIEST;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE_LATEST;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE_TIMESTAMP;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_TIMESTAMP_MILLIS;
import static com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil.fetchLogstorePrefixProperties;
import static com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil.getLogTopic;
import static java.lang.Boolean.FALSE;
import static org.apache.flink.connector.pulsar.common.config.PulsarOptions.PULSAR_ENABLE_TRANSACTION;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_CONSUMER_NAME;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_ENABLE_AUTO_ACKNOWLEDGE_MESSAGE;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_PARTITION_DISCOVERY_INTERVAL_MS;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_READ_TRANSACTION_TIMEOUT;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_SUBSCRIPTION_NAME;
import static org.apache.flink.connector.pulsar.source.PulsarSourceOptions.PULSAR_SUBSCRIPTION_TYPE;
import static org.apache.flink.connector.pulsar.source.config.PulsarSourceConfigUtils.SOURCE_CONFIG_VALIDATOR;
import static org.apache.flink.util.InstantiationUtil.isSerializable;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.flink.util.Preconditions.checkState;

/**
 * The builder class for {@link LogPulsarSource} to make it easier for the users to construct a {@link
 * LogPulsarSource}.
 *
 * <p>The following example shows the minimum setup to create a LogPulsarSource that reads the String
 * values from a Pulsar topic.
 *
 * <p>The service url, admin url, subscription name, topics to consume are required fields that must be set.
 *
 * <p>To specify the starting position of LogPulsarSource, one can call {@link
 * #setStartCursor(StartCursor)}.
 *
 * <p>By default, the LogPulsarSource runs in an {@link Boundedness#CONTINUOUS_UNBOUNDED} mode and
 * never stop until the Flink job is canceled or fails.
 *
 * <pre>{@code
 * LogPulsarSource<String> source = LogPulsarSource
 *     .builder(schema, arcticTable.properties())
 *     .setTopics(TOPIC1)
 *     .setServiceUrl(getServiceUrl())
 *     .setAdminUrl(getAdminUrl())
 *     .setSubscriptionName("test")
 *     .build();
 * }</pre>
 */
@PublicEvolving
public class LogPulsarSourceBuilder extends PulsarSourceBuilder<RowData> {
  private static final Logger LOG = LoggerFactory.getLogger(LogPulsarSourceBuilder.class);

  private Schema schema;
  private Map<String, String> tableProperties;

  /**
   * @param schema          read schema, only contains the selected fields
   * @param tableProperties Arctic table properties
   */
  LogPulsarSourceBuilder(Schema schema, Map<String, String> tableProperties) {
    super();
    this.schema = schema;
    this.tableProperties = tableProperties;
    this.setProperties(fetchLogstorePrefixProperties(tableProperties));
    setupPulsarProperties();
  }

  public LogPulsarSourceBuilder setProperties(Properties properties) {
    configBuilder.set(properties);
    return this;
  }

  private void setupPulsarProperties() {
    if (tableProperties.containsKey(TableProperties.LOG_STORE_ADDRESS)) {
      this.setServiceUrl(tableProperties.get(TableProperties.LOG_STORE_ADDRESS));
    }
    if (tableProperties.containsKey(TableProperties.LOG_STORE_MESSAGE_TOPIC)) {
      setTopics(getLogTopic(tableProperties));
    }

    setupStartupMode();
  }

  private void setupStartupMode() {
    String startupMode = CompatiblePropertyUtil.propertyAsString(tableProperties, SCAN_STARTUP_MODE.key(),
        SCAN_STARTUP_MODE.defaultValue()).toLowerCase();
    long startupTimestampMillis = 0L;
    if (Objects.equals(startupMode.toLowerCase(), SCAN_STARTUP_MODE_TIMESTAMP)) {
      startupTimestampMillis = Long.parseLong(Preconditions.checkNotNull(
          tableProperties.get(SCAN_STARTUP_TIMESTAMP_MILLIS.key()),
          String.format("'%s' should be set in '%s' mode",
              SCAN_STARTUP_TIMESTAMP_MILLIS.key(), SCAN_STARTUP_MODE_TIMESTAMP)));
    }
    switch (startupMode) {
      case SCAN_STARTUP_MODE_EARLIEST:
        this.setStartCursor(StartCursor.earliest());
        break;
      case SCAN_STARTUP_MODE_LATEST:
        this.setStartCursor(StartCursor.latest());
        break;
      case SCAN_STARTUP_MODE_TIMESTAMP:
        this.setStartCursor(StartCursor.fromPublishTime(startupTimestampMillis));
        break;
      default:
        throw new ValidationException(String.format(
            "%s only support '%s', '%s', '%s'. But input is '%s'", ArcticValidator.SCAN_STARTUP_MODE,
            SCAN_STARTUP_MODE_LATEST, SCAN_STARTUP_MODE_EARLIEST, SCAN_STARTUP_MODE_TIMESTAMP, startupMode));
    }
  }

  /**
   * Build the {@link LogPulsarSource}.
   *
   * @return a LogPulsarSource with the settings made for this builder.
   */
  @SuppressWarnings("java:S3776")
  public LogPulsarSource build() {
    buildOfficial();

    // If subscription name is not set, set a random value.
    if (!configBuilder.contains(PULSAR_SUBSCRIPTION_NAME)) {
      String uuid = UUID.randomUUID().toString();
      LOG.warn("properties.{} has not set, using random subscription name: {}", PULSAR_SUBSCRIPTION_NAME.key(), uuid);
      configBuilder.set(PULSAR_SUBSCRIPTION_NAME, uuid);
    }
    // Check builder configuration.
    SourceConfiguration sourceConfiguration =
        configBuilder.build(SOURCE_CONFIG_VALIDATOR, SourceConfiguration::new);

    return new LogPulsarSource(
        sourceConfiguration,
        subscriber,
        rangeGenerator,
        startCursor,
        stopCursor,
        boundedness,
        deserializationSchema,
        schema,
        tableProperties);
  }

  /**
   * copy from org.apache.flink.connector.pulsar.source.PulsarSourceBuilder#build
   */
  private void buildOfficial() {
    // Ensure the topic subscriber for pulsar.
    checkNotNull(subscriber, "No topic names or topic pattern are provided.");

    SubscriptionType subscriptionType = configBuilder.get(PULSAR_SUBSCRIPTION_TYPE);
    if (subscriptionType == SubscriptionType.Key_Shared) {
      if (rangeGenerator == null) {
        LOG.warn(
            "No range generator provided for key_shared subscription," +
                " we would use the UniformRangeGenerator as the default range generator.");
        this.rangeGenerator = new SplitRangeGenerator();
      }
    } else {
      // Override the range generator.
      this.rangeGenerator = new FullRangeGenerator();
    }

    if (boundedness == null) {
      LOG.warn("No boundedness was set, mark it as a endless stream.");
      this.boundedness = Boundedness.CONTINUOUS_UNBOUNDED;
    }
    if (boundedness == Boundedness.BOUNDED && configBuilder.get(PULSAR_PARTITION_DISCOVERY_INTERVAL_MS) >= 0) {
      LOG.warn(
          "{} property is overridden to -1 because the source is bounded.",
          PULSAR_PARTITION_DISCOVERY_INTERVAL_MS);
      configBuilder.override(PULSAR_PARTITION_DISCOVERY_INTERVAL_MS, -1L);
    }

    // Enable transaction if the cursor auto commit is disabled for Key_Shared & Shared.
    if (FALSE.equals(configBuilder.get(PULSAR_ENABLE_AUTO_ACKNOWLEDGE_MESSAGE)) &&
        (subscriptionType == SubscriptionType.Key_Shared || subscriptionType == SubscriptionType.Shared)) {
      LOG.info(
          "Pulsar cursor auto commit is disabled, make sure checkpoint is enabled " +
              "and your pulsar cluster is support the transaction.");
      configBuilder.override(PULSAR_ENABLE_TRANSACTION, true);

      if (!configBuilder.contains(PULSAR_READ_TRANSACTION_TIMEOUT)) {
        LOG.warn(
            "The default pulsar transaction timeout is 3 hours, " +
                "make sure it was greater than your checkpoint interval.");
      } else {
        Long timeout = configBuilder.get(PULSAR_READ_TRANSACTION_TIMEOUT);
        LOG.warn(
            "The configured transaction timeout is {} mille seconds, " +
                "make sure it was greater than your checkpoint interval.",
            timeout);
      }
    }

    if (!configBuilder.contains(PULSAR_CONSUMER_NAME)) {
      LOG.warn(
          "We recommend set a readable consumer name through setConsumerName(String) in production mode.");
    } else {
      String consumerName = configBuilder.get(PULSAR_CONSUMER_NAME);
      if (!consumerName.contains("%s")) {
        configBuilder.override(PULSAR_CONSUMER_NAME, consumerName + " - %s");
      }
    }

    // Since these implementation could be a lambda, make sure they are serializable.
    checkState(isSerializable(startCursor), "StartCursor isn't serializable");
    checkState(isSerializable(stopCursor), "StopCursor isn't serializable");
    checkState(isSerializable(rangeGenerator), "RangeGenerator isn't serializable");
  }
}
