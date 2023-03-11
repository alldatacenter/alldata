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

package com.netease.arctic.flink.read.source.log.kafka;

import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.KafkaSourceOptions;
import org.apache.flink.connector.kafka.source.enumerator.initializer.NoStoppingOffsetsInitializer;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.enumerator.subscriber.KafkaSubscriber;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.Schema;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE_EARLIEST;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE_LATEST;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_MODE_TIMESTAMP;
import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SCAN_STARTUP_TIMESTAMP_MILLIS;
import static com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil.fetchLogstorePrefixProperties;
import static com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil.getLogTopic;
import static com.netease.arctic.table.TableProperties.LOG_STORE_ADDRESS;
import static com.netease.arctic.table.TableProperties.LOG_STORE_MESSAGE_TOPIC;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;

/**
 * The @builder class for {@link LogKafkaSource} to make it easier for the users to construct a {@link
 * LogKafkaSource}.
 *
 * <pre>{@code
 * LogKafkaSource source = LogKafkaSource.builder(arcticSchema, configuration)
 *    .setTopics(Arrays.asList(TOPIC1))
 *    .setStartingOffsets(OffsetsInitializer.earliest())
 *    .setProperties(properties)
 *    .build();
 * }</pre>
 */
public class LogKafkaSourceBuilder {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaSourceBuilder.class);
  private static final String[] REQUIRED_CONFIGS = {
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ConsumerConfig.GROUP_ID_CONFIG
  };
  // The subscriber specifies the partitions to subscribe to.
  private KafkaSubscriber subscriber;
  // Users can specify the starting / stopping offset initializer.
  private OffsetsInitializer startingOffsetsInitializer;
  private OffsetsInitializer stoppingOffsetsInitializer;
  // Boundedness
  private Boundedness boundedness;
  private KafkaRecordDeserializationSchema<RowData> deserializationSchema;
  // The configurations.
  protected Properties kafkaProperties;

  private Schema schema;
  private Map<String, String> tableProperties;

  /**
   * @param schema          read schema, only contains the selected fields
   * @param tableProperties arctic table properties, maybe include Flink SQL hints.
   */
  LogKafkaSourceBuilder(Schema schema, Map<String, String> tableProperties) {
    this.subscriber = null;
    this.startingOffsetsInitializer = OffsetsInitializer.earliest();
    this.stoppingOffsetsInitializer = new NoStoppingOffsetsInitializer();
    this.boundedness = Boundedness.CONTINUOUS_UNBOUNDED;
    this.deserializationSchema = null;
    this.kafkaProperties = fetchLogstorePrefixProperties(tableProperties);
    this.schema = schema;
    this.tableProperties = tableProperties;
    setupKafkaProperties();
  }

  /**
   * Sets the bootstrap servers for the KafkaConsumer of the LogKafkaSource.
   *
   * @param bootstrapServers the bootstrap servers of the Kafka cluster.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setBootstrapServers(String bootstrapServers) {
    return setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
  }

  /**
   * Sets the consumer group id of the LogKafkaSource.
   *
   * @param groupId the group id of the LogKafkaSource.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setGroupId(String groupId) {
    return setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
  }

  /**
   * Set a list of topics the LogKafkaSource should consume from. All the topics in the list should
   * have existed in the Kafka cluster. Otherwise an exception will be thrown. To allow some of
   * the topics to be created lazily, please use {@link #setTopicPattern(Pattern)} instead.
   */
  public LogKafkaSourceBuilder setTopics(List<String> topics) {
    ensureSubscriberIsNull("topics");
    subscriber = KafkaSubscriber.getTopicListSubscriber(topics);
    return this;
  }

  /**
   * Set a list of topics the LogKafkaSource should consume from. All the topics in the list should
   * have existed in the Kafka cluster. Otherwise an exception will be thrown. To allow some of
   * the topics to be created lazily, please use {@link #setTopicPattern(Pattern)} instead.
   *
   * @param topics the list of topics to consume from.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setTopics(String... topics) {
    return setTopics(Arrays.asList(topics));
  }

  /**
   * Set a topic pattern to consume from use the java {@link Pattern}.
   *
   * @param topicPattern the pattern of the topic name to consume from.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setTopicPattern(Pattern topicPattern) {
    ensureSubscriberIsNull("topic pattern");
    subscriber = KafkaSubscriber.getTopicPatternSubscriber(topicPattern);
    return this;
  }

  /**
   * Set a set of partitions to consume from.
   *
   * @param partitions the set of partitions to consume from.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setPartitions(Set<TopicPartition> partitions) {
    ensureSubscriberIsNull("partitions");
    subscriber = KafkaSubscriber.getPartitionSetSubscriber(partitions);
    return this;
  }

  /**
   * Specify from which offsets the LogKafkaSource should start consume from by providing an {@link
   * OffsetsInitializer}.
   *
   * <p>The following {@link OffsetsInitializer}s are commonly used and provided out of the box.
   * Users can also implement their own {@link OffsetsInitializer} for custom behaviors.
   *
   * <ul>
   *   <li>{@link OffsetsInitializer#earliest()} - starting from the earliest offsets. This is
   *       also the default {@link OffsetsInitializer} of the KafkaSource for starting offsets.
   *   <li>{@link OffsetsInitializer#latest()} - starting from the latest offsets.
   *   <li>{@link OffsetsInitializer#committedOffsets()} - starting from the committed offsets of
   *       the consumer group.
   *   <li>{@link
   *       OffsetsInitializer#committedOffsets(org.apache.kafka.clients.consumer.OffsetResetStrategy)}
   *       - starting from the committed offsets of the consumer group. If there is no committed
   *       offsets, starting from the offsets specified by the {@link
   *       org.apache.kafka.clients.consumer.OffsetResetStrategy OffsetResetStrategy}.
   *   <li>{@link OffsetsInitializer#offsets(Map)} - starting from the specified offsets for each
   *       partition.
   *   <li>{@link OffsetsInitializer#timestamp(long)} - starting from the specified timestamp for
   *       each partition. Note that the guarantee here is that all the records in Kafka whose
   *       {@link org.apache.kafka.clients.consumer.ConsumerRecord#timestamp()} is greater than
   *       the given starting timestamp will be consumed. However, it is possible that some
   *       consumer records whose timestamp is smaller than the given starting timestamp are also
   *       consumed.
   * </ul>
   *
   * @param startingOffsetsInitializer the {@link OffsetsInitializer} setting the starting offsets
   *                                   for the Source.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setStartingOffsets(
      OffsetsInitializer startingOffsetsInitializer) {
    this.startingOffsetsInitializer = startingOffsetsInitializer;
    return this;
  }

  /**
   * By default the LogKafkaSource is set to run in {@link Boundedness#CONTINUOUS_UNBOUNDED} manner
   * and thus never stops until the Flink job fails or is canceled. To let the KafkaSource run as
   * a streaming source but still stops at some point, one can set an {@link OffsetsInitializer}
   * to specify the stopping offsets for each partition. When all the partitions have reached
   * their stopping offsets, the KafkaSource will then exit.
   *
   * <p>This method is different from {@link #setBounded(OffsetsInitializer)} that after setting
   * the stopping offsets with this method, {@link KafkaSource#getBoundedness()} will still return
   * {@link Boundedness#CONTINUOUS_UNBOUNDED} even though it will stop at the stopping offsets
   * specified by the stopping offsets {@link OffsetsInitializer}.
   *
   * <p>The following {@link OffsetsInitializer} are commonly used and provided out of the box.
   * Users can also implement their own {@link OffsetsInitializer} for custom behaviors.
   *
   * <ul>
   *   <li>{@link OffsetsInitializer#latest()} - stop at the latest offsets of the partitions when
   *       the KafkaSource starts to run.
   *   <li>{@link OffsetsInitializer#committedOffsets()} - stops at the committed offsets of the
   *       consumer group.
   *   <li>{@link OffsetsInitializer#offsets(Map)} - stops at the specified offsets for each
   *       partition.
   *   <li>{@link OffsetsInitializer#timestamp(long)} - stops at the specified timestamp for each
   *       partition. The guarantee of setting the stopping timestamp is that no Kafka records
   *       whose {@link org.apache.kafka.clients.consumer.ConsumerRecord#timestamp()} is greater
   *       than the given stopping timestamp will be consumed. However, it is possible that some
   *       records whose timestamp is smaller than the specified stopping timestamp are not
   *       consumed.
   * </ul>
   *
   * @param stoppingOffsetsInitializer The {@link OffsetsInitializer} to specify the stopping
   *                                   offset.
   * @return this LogKafkaSourceBuilder.
   * @see #setBounded(OffsetsInitializer)
   */
  public LogKafkaSourceBuilder setUnbounded(OffsetsInitializer stoppingOffsetsInitializer) {
    this.boundedness = Boundedness.CONTINUOUS_UNBOUNDED;
    this.stoppingOffsetsInitializer = stoppingOffsetsInitializer;
    return this;
  }

  /**
   * By default the LogKafkaSource is set to run in {@link Boundedness#CONTINUOUS_UNBOUNDED} manner
   * and thus never stops until the Flink job fails or is canceled. To let the KafkaSource run in
   * {@link Boundedness#BOUNDED} manner and stops at some point, one can set an {@link
   * OffsetsInitializer} to specify the stopping offsets for each partition. When all the
   * partitions have reached their stopping offsets, the KafkaSource will then exit.
   *
   * <p>This method is different from {@link #setUnbounded(OffsetsInitializer)} that after setting
   * the stopping offsets with this method, {@link KafkaSource#getBoundedness()} will return
   * {@link Boundedness#BOUNDED} instead of {@link Boundedness#CONTINUOUS_UNBOUNDED}.
   *
   * <p>The following {@link OffsetsInitializer} are commonly used and provided out of the box.
   * Users can also implement their own {@link OffsetsInitializer} for custom behaviors.
   *
   * <ul>
   *   <li>{@link OffsetsInitializer#latest()} - stop at the latest offsets of the partitions when
   *       the KafkaSource starts to run.
   *   <li>{@link OffsetsInitializer#committedOffsets()} - stops at the committed offsets of the
   *       consumer group.
   *   <li>{@link OffsetsInitializer#offsets(Map)} - stops at the specified offsets for each
   *       partition.
   *   <li>{@link OffsetsInitializer#timestamp(long)} - stops at the specified timestamp for each
   *       partition. The guarantee of setting the stopping timestamp is that no Kafka records
   *       whose {@link org.apache.kafka.clients.consumer.ConsumerRecord#timestamp()} is greater
   *       than the given stopping timestamp will be consumed. However, it is possible that some
   *       records whose timestamp is smaller than the specified stopping timestamp are not
   *       consumed.
   * </ul>
   *
   * @param stoppingOffsetsInitializer the {@link OffsetsInitializer} to specify the stopping
   *                                   offsets.
   * @return this LogKafkaSourceBuilder.
   * @see #setUnbounded(OffsetsInitializer)
   */
  public LogKafkaSourceBuilder setBounded(OffsetsInitializer stoppingOffsetsInitializer) {
    this.boundedness = Boundedness.BOUNDED;
    this.stoppingOffsetsInitializer = stoppingOffsetsInitializer;
    return this;
  }

  /**
   * Sets the {@link KafkaRecordDeserializationSchema deserializer} of the {@link
   * org.apache.kafka.clients.consumer.ConsumerRecord ConsumerRecord} for LogKafkaSource.
   *
   * @param recordDeserializer the deserializer for Kafka {@link
   *                           org.apache.kafka.clients.consumer.ConsumerRecord ConsumerRecord}.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setDeserializer(
      KafkaRecordDeserializationSchema<RowData> recordDeserializer) {
    this.deserializationSchema = recordDeserializer;
    return this;
  }

  /**
   * Sets the client id prefix of this LogKafkaSource.
   *
   * @param prefix the client id prefix to use for this LogKafkaSource.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setClientIdPrefix(String prefix) {
    return setProperty(KafkaSourceOptions.CLIENT_ID_PREFIX.key(), prefix);
  }

  /**
   * Set an arbitrary property for the LogKafkaSource and LogKafkaConsumer. The valid keys can be found
   * in {@link ConsumerConfig} and {@link KafkaSourceOptions}.
   *
   * <p>Note that the following keys will be overridden by the builder when the KafkaSource is
   * created.
   *
   * <ul>
   *   <li><code>key.deserializer</code> is always set to {@link ByteArrayDeserializer}.
   *   <li><code>value.deserializer</code> is always set to {@link ByteArrayDeserializer}.
   *   <li><code>auto.offset.reset.strategy</code> is overridden by {@link
   *       OffsetsInitializer#getAutoOffsetResetStrategy()} for the starting offsets, which is by
   *       default {@link OffsetsInitializer#earliest()}.
   *   <li><code>partition.discovery.interval.ms</code> is overridden to -1 when {@link
   *       #setBounded(OffsetsInitializer)} has been invoked.
   * </ul>
   *
   * @param key   the key of the property.
   * @param value the value of the property.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setProperty(String key, String value) {
    kafkaProperties.setProperty(key, value);
    return this;
  }

  /**
   * Set arbitrary properties for the LogKafkaSource and LogKafkaConsumer. The valid keys can be found
   * in {@link ConsumerConfig} and {@link KafkaSourceOptions}.
   *
   * <p>Note that the following keys will be overridden by the builder when the KafkaSource is
   * created.
   *
   * <ul>
   *   <li><code>key.deserializer</code> is always set to {@link ByteArrayDeserializer}.
   *   <li><code>value.deserializer</code> is always set to {@link ByteArrayDeserializer}.
   *   <li><code>auto.offset.reset.strategy</code> is overridden by {@link
   *       OffsetsInitializer#getAutoOffsetResetStrategy()} for the starting offsets, which is by
   *       default {@link OffsetsInitializer#earliest()}.
   *   <li><code>partition.discovery.interval.ms</code> is overridden to -1 when {@link
   *       #setBounded(OffsetsInitializer)} has been invoked.
   *   <li><code>client.id</code> is overridden to the "client.id.prefix-RANDOM_LONG", or
   *       "group.id-RANDOM_LONG" if the client id prefix is not set.
   * </ul>
   *
   * @param props the properties to set for the LogKafkaSource.
   * @return this LogKafkaSourceBuilder.
   */
  public LogKafkaSourceBuilder setProperties(Properties props) {
    this.kafkaProperties.putAll(props);
    return this;
  }

  /**
   * Build the {@link LogKafkaSource}.
   *
   * @return a LogKafkaSource with the settings made for this builder.
   */
  public LogKafkaSource build() {
    sanityCheck();
    parseAndSetRequiredProperties();
    return new LogKafkaSource(
        subscriber,
        startingOffsetsInitializer,
        stoppingOffsetsInitializer,
        boundedness,
        deserializationSchema,
        kafkaProperties,
        schema,
        tableProperties);
  }

  private void setupKafkaProperties() {
    if (tableProperties.containsKey(TableProperties.LOG_STORE_ADDRESS)) {
      kafkaProperties.put(BOOTSTRAP_SERVERS_CONFIG, tableProperties.get(
          TableProperties.LOG_STORE_ADDRESS));
    }
    if (tableProperties.containsKey(TableProperties.LOG_STORE_MESSAGE_TOPIC)) {
      setTopics(getLogTopic(tableProperties));
    }

    kafkaProperties.putIfAbsent("properties.key.serializer",
        "org.apache.kafka.common.serialization.ByteArraySerializer");
    kafkaProperties.putIfAbsent("properties.value.serializer",
        "org.apache.kafka.common.serialization.ByteArraySerializer");
    kafkaProperties.putIfAbsent("properties.key.deserializer",
        "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    kafkaProperties.putIfAbsent("properties.value.deserializer",
        "org.apache.kafka.common.serialization.ByteArrayDeserializer");

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
        setStartingOffsets(OffsetsInitializer.earliest());
        break;
      case SCAN_STARTUP_MODE_LATEST:
        setStartingOffsets(OffsetsInitializer.latest());
        break;
      case SCAN_STARTUP_MODE_TIMESTAMP:
        setStartingOffsets(OffsetsInitializer.timestamp(startupTimestampMillis));
        break;
      default:
        throw new ValidationException(String.format(
            "%s only support '%s', '%s', '%s'. But input is '%s'", ArcticValidator.SCAN_STARTUP_MODE,
            SCAN_STARTUP_MODE_LATEST, SCAN_STARTUP_MODE_EARLIEST, SCAN_STARTUP_MODE_TIMESTAMP, startupMode));
    }
  }

  // ------------- private helpers  --------------

  private void ensureSubscriberIsNull(String attemptingSubscribeMode) {
    if (subscriber != null) {
      throw new IllegalStateException(
          String.format(
              "Cannot use %s for consumption because a %s is already set for consumption.",
              attemptingSubscribeMode, subscriber.getClass().getSimpleName()));
    }
  }

  private void parseAndSetRequiredProperties() {
    maybeOverride(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        ByteArrayDeserializer.class.getName(),
        true);
    maybeOverride(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        ByteArrayDeserializer.class.getName(),
        true);
    maybeOverride(
        ConsumerConfig.GROUP_ID_CONFIG, "KafkaSource-" + new Random().nextLong(), false);
    maybeOverride(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false", false);
    maybeOverride(
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
        startingOffsetsInitializer.getAutoOffsetResetStrategy().name().toLowerCase(),
        true);

    // If the source is bounded, do not run periodic partition discovery.
    maybeOverride(
        KafkaSourceOptions.PARTITION_DISCOVERY_INTERVAL_MS.key(),
        "-1",
        boundedness == Boundedness.BOUNDED);

    // If the client id prefix is not set, reuse the consumer group id as the client id prefix.
    maybeOverride(
        KafkaSourceOptions.CLIENT_ID_PREFIX.key(),
        kafkaProperties.getProperty(ConsumerConfig.GROUP_ID_CONFIG),
        false);
  }

  private boolean maybeOverride(String key, String value, boolean override) {
    boolean overridden = false;
    String userValue = kafkaProperties.getProperty(key);
    if (userValue != null) {
      if (override) {
        LOG.warn(
            String.format(
                "Property %s is provided but will be overridden from %s to %s",
                key, userValue, value));
        kafkaProperties.setProperty(key, value);
        overridden = true;
      }
    } else {
      kafkaProperties.setProperty(key, value);
    }
    return overridden;
  }

  private void sanityCheck() {
    // Check required configs.
    checkNotNull(
        kafkaProperties.getProperty(BOOTSTRAP_SERVERS_CONFIG),
        String.format("Property %s is required but not provided", LOG_STORE_ADDRESS));
    // Check required settings.
    checkNotNull(
        subscriber,
        String.format("No topic is specified, '%s' should be set.", LOG_STORE_MESSAGE_TOPIC));
  }
}
