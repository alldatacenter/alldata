/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.pulsar.withoutadmin;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.RuntimeContextInitializationContextAdapters;
import org.apache.flink.api.common.state.CheckpointListener;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.OperatorStateStore;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.ClosureCleaner;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.runtime.TupleSerializer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.shaded.guava18.com.google.common.collect.Sets;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.connectors.pulsar.config.StartupMode;
import org.apache.flink.streaming.connectors.pulsar.internal.CachedPulsarClient;
import org.apache.flink.streaming.connectors.pulsar.internal.MessageIdSerializer;
import org.apache.flink.streaming.connectors.pulsar.internal.PulsarSourceStateSerializer;
import org.apache.flink.streaming.connectors.pulsar.internal.SerializableRange;
import org.apache.flink.streaming.connectors.pulsar.internal.SourceSinkUtils;
import org.apache.flink.streaming.connectors.pulsar.internal.TopicRange;
import org.apache.flink.streaming.connectors.pulsar.internal.TopicSubscription;
import org.apache.flink.streaming.connectors.pulsar.internal.TopicSubscriptionSerializer;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPeriodicWatermarksAdapter;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPunctuatedWatermarksAdapter;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.streaming.util.serialization.PulsarDeserializationSchema;
import org.apache.flink.util.ExceptionUtils;
import org.apache.flink.util.SerializedValue;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SourceMetricData;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.apache.inlong.sort.pulsar.table.DynamicPulsarDeserializationSchema;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.shade.com.google.common.collect.Maps;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.flink.streaming.connectors.pulsar.internal.metrics.PulsarSourceMetrics.COMMITS_FAILED_METRICS_COUNTER;
import static org.apache.flink.streaming.connectors.pulsar.internal.metrics.PulsarSourceMetrics.COMMITS_SUCCEEDED_METRICS_COUNTER;
import static org.apache.flink.streaming.connectors.pulsar.internal.metrics.PulsarSourceMetrics.PULSAR_SOURCE_METRICS_GROUP;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_IN;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_IN;

/**
 * Copy from io.streamnative.connectors:pulsar-flink-connector_2.11:1.13.6.1-rc9,
 * From {@link org.apache.flink.streaming.connectors.pulsar.FlinkPulsarSource}
 * Pulsar data source.
 *
 * @param <T> The type of records produced by this data source.
 */
public class FlinkPulsarSource<T>
        extends
            RichParallelSourceFunction<T>
        implements
            ResultTypeQueryable<T>,
            CheckpointListener,
            CheckpointedFunction {

    private static final Logger log = LoggerFactory.getLogger(FlinkPulsarSource.class);

    /** The maximum number of pending non-committed checkpoints to track, to avoid memory leaks. */
    public static final int MAX_NUM_PENDING_CHECKPOINTS = 100;

    /** Boolean configuration key to disable metrics tracking. **/
    public static final String KEY_DISABLE_METRICS = "flink.disable-metrics";

    /** State name of the consumer's partition offset states. */
    private static final String OFFSETS_STATE_NAME = "topic-partition-offset-states";

    private static final String OFFSETS_STATE_NAME_V3 = "topic-offset-states";

    // ------------------------------------------------------------------------
    // configuration state, set on the client relevant for all subtasks
    // ------------------------------------------------------------------------

    protected String serverUrl;

    protected ClientConfigurationData clientConfigurationData;

    protected final Map<String, String> caseInsensitiveParams;

    protected final Map<String, Object> readerConf;

    protected volatile PulsarDeserializationSchema<T> deserializer;

    private Map<TopicRange, MessageId> ownedTopicStarts;

    /**
     * Optional watermark strategy that will be run per pulsar partition, to exploit per-partition
     * timestamp characteristics. The watermark strategy is kept in serialized form, to deserialize
     * it into multiple copies.
     */
    private SerializedValue<WatermarkStrategy<T>> watermarkStrategy;

    /** User configured value for discovery interval, in milliseconds. */
    private final long discoveryIntervalMillis;

    protected final int pollTimeoutMs;

    protected final int commitMaxRetries;

    /** The startup mode for the reader (default is {@link StartupMode#LATEST}). */
    private StartupMode startupMode = StartupMode.LATEST;

    /**
     * The subscription name to be used; only relevant when startup mode is {@link StartupMode#EXTERNAL_SUBSCRIPTION}
     * If the subscription exists for a partition, we would start reading this partition from the subscription cursor.
     * At the same time, checkpoint for the job would made progress on the subscription.
     */
    private String externalSubscriptionName;

    /**
     * The subscription position to use when subscription does not exist (default is {@link MessageId#latest});
     * Only relevant when startup mode is {@link StartupMode#EXTERNAL_SUBSCRIPTION}.
     */
    private MessageId subscriptionPosition = MessageId.latest;

    // TODO: remove this when MessageId is serializable itself.
    // see: https://github.com/apache/pulsar/pull/6064
    private Map<TopicRange, byte[]> specificStartupOffsetsAsBytes;

    protected final Properties properties;

    protected final UUID uuid = UUID.randomUUID();

    // ------------------------------------------------------------------------
    // runtime state (used individually by each parallel subtask)
    // ------------------------------------------------------------------------

    /** Data for pending but uncommitted offsets. */
    private final LinkedHashMap<Long, Map<TopicRange, MessageId>> pendingOffsetsToCommit = new LinkedHashMap<>();

    /** Fetcher implements Pulsar reads. */
    private transient volatile PulsarFetcher<T> pulsarFetcher;

    /** The partition discoverer, used to find new partitions. */
    protected transient volatile PulsarMetadataReader metadataReader;

    /**
     * The offsets to restore to, if the consumer restores state from a checkpoint.
     *
     * <p>This map will be populated by the {@link #initializeState(FunctionInitializationContext)} method.
     *
     * <p>Using a sorted map as the ordering is important when using restored state
     * to seed the partition discoverer.
     */
    private transient volatile TreeMap<TopicRange, MessageId> restoredState;
    private transient volatile Set<TopicRange> excludeStartMessageIds;

    /**
     * Accessor for state in the operator state backend.
     */
    private transient ListState<Tuple2<TopicSubscription, MessageId>> unionOffsetStates;

    private int oldStateVersion = 2;

    private volatile boolean stateSubEqualexternalSub = false;

    /** Discovery loop, executed in a separate thread. */
    private transient volatile Thread discoveryLoopThread;

    /** Flag indicating whether the consumer is still running. */
    private volatile boolean running = true;

    /**
     * Flag indicating whether or not metrics should be exposed.
     * If {@code true}, offset metrics (e.g. current offset, committed offset) and
     * other metrics will be registered.
     */
    private final boolean useMetrics;

    /** Counter for successful Pulsar offset commits. */
    private transient Counter successfulCommits;

    /** Counter for failed Pulsar offset commits. */
    private transient Counter failedCommits;

    private transient int taskIndex;

    private transient int numParallelTasks;

    private MetricState metricState;

    /**
     * Metric for InLong
     */
    private String inlongMetric;
    /**
     * audit host and ports
     */
    private String inlongAudit;

    private SourceMetricData sourceMetricData;

    private transient ListState<MetricState> metricStateListState;

    public FlinkPulsarSource(
            String serverUrl,
            ClientConfigurationData clientConf,
            PulsarDeserializationSchema<T> deserializer,
            Properties properties,
            String inlongMetric,
            String inlongAudit) {
        this.inlongAudit = inlongAudit;
        this.inlongMetric = inlongMetric;
        this.serverUrl = checkNotNull(serverUrl);
        this.clientConfigurationData = checkNotNull(clientConf);
        this.deserializer = deserializer;
        this.properties = properties;
        this.caseInsensitiveParams =
                SourceSinkUtils.validateStreamSourceOptions(Maps.fromProperties(properties));
        this.readerConf =
                SourceSinkUtils.getReaderParams(Maps.fromProperties(properties));
        this.discoveryIntervalMillis =
                SourceSinkUtils.getPartitionDiscoveryIntervalInMillis(caseInsensitiveParams);
        this.pollTimeoutMs =
                SourceSinkUtils.getPollTimeoutMs(caseInsensitiveParams);
        this.commitMaxRetries =
                SourceSinkUtils.getCommitMaxRetries(caseInsensitiveParams);
        this.useMetrics =
                SourceSinkUtils.getUseMetrics(caseInsensitiveParams);

        CachedPulsarClient.setCacheSize(SourceSinkUtils.getClientCacheSize(caseInsensitiveParams));

        if (this.clientConfigurationData.getServiceUrl() == null) {
            throw new IllegalArgumentException("ServiceUrl must be supplied in the client configuration");
        }
        this.oldStateVersion = SourceSinkUtils.getOldStateVersion(caseInsensitiveParams, oldStateVersion);
    }

    // ------------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------------

    /**
     * Specifies an {@link AssignerWithPunctuatedWatermarks} to emit watermarks
     * in a punctuated manner. The watermark extractor will run per Pulsar partition,
     * watermarks will be merged across partitions in the same way as in the Flink runtime,
     * when streams are merged.
     *
     * <p>When a subtask of a FlinkPulsarSource source reads multiple Pulsar partitions,
     * the streams from the partitions are unioned in a "first come first serve" fashion.
     * Per-partition characteristics are usually lost that way.
     * For example, if the timestamps are strictly ascending per Pulsar partition,
     * they will not be strictly ascending in the resulting Flink DataStream, if the
     * parallel source subtask reads more that one partition.
     *
     * <p>Running timestamp extractors / watermark generators directly inside the Pulsar source,
     * per Pulsar partition, allows users to let them exploit the per-partition characteristics.
     *
     * <p>Note: One can use either an {@link AssignerWithPunctuatedWatermarks} or an
     * {@link AssignerWithPeriodicWatermarks}, not both at the same time.
     *
     * @param assigner The timestamp assigner / watermark generator to use.
     * @return The reader object, to allow function chaining.
     */
    @Deprecated
    public FlinkPulsarSource<T> assignTimestampsAndWatermarks(AssignerWithPunctuatedWatermarks<T> assigner) {
        checkNotNull(assigner);

        if (this.watermarkStrategy != null) {
            throw new IllegalStateException("Some watermark strategy has already been set.");
        }

        try {
            ClosureCleaner.clean(assigner, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
            final WatermarkStrategy<T> wms = new AssignerWithPunctuatedWatermarksAdapter.Strategy<>(assigner);

            return assignTimestampsAndWatermarks(wms);
        } catch (Exception e) {
            throw new IllegalArgumentException("The given assigner is not serializable", e);
        }
    }

    /**
     * Specifies an {@link AssignerWithPunctuatedWatermarks} to emit watermarks
     * in a punctuated manner. The watermark extractor will run per Pulsar partition,
     * watermarks will be merged across partitions in the same way as in the Flink runtime,
     * when streams are merged.
     *
     * <p>When a subtask of a FlinkTDMQSource source reads multiple Pulsar partitions,
     * the streams from the partitions are unioned in a "first come first serve" fashion.
     * Per-partition characteristics are usually lost that way.
     * For example, if the timestamps are strictly ascending per Pulsar partition,
     * they will not be strictly ascending in the resulting Flink DataStream,
     * if the parallel source subtask reads more that one partition.
     *
     * <p>Running timestamp extractors / watermark generators directly inside the Pulsar source,
     * per Pulsar partition, allows users to let them exploit the per-partition characteristics.
     *
     * <p>Note: One can use either an {@link AssignerWithPunctuatedWatermarks} or an
     * {@link AssignerWithPeriodicWatermarks}, not both at the same time.
     *
     * @param assigner The timestamp assigner / watermark generator to use.
     * @return The reader object, to allow function chaining.
     */
    @Deprecated
    public FlinkPulsarSource<T> assignTimestampsAndWatermarks(AssignerWithPeriodicWatermarks<T> assigner) {
        checkNotNull(assigner);

        if (this.watermarkStrategy != null) {
            throw new IllegalStateException("Some watermark strategy has already been set.");
        }

        try {
            ClosureCleaner.clean(assigner, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
            final WatermarkStrategy<T> wms = new AssignerWithPeriodicWatermarksAdapter.Strategy<>(assigner);

            return assignTimestampsAndWatermarks(wms);
        } catch (Exception e) {
            throw new IllegalArgumentException("The given assigner is not serializable", e);
        }
    }

    /**
     * Sets the given {@link WatermarkStrategy} on this consumer. These will be used to assign
     * timestamps to records and generates watermarks to signal event time progress.
     *
     * <p>Running timestamp extractors / watermark generators directly inside the Pulsar source
     * (which you can do by using this method), per Pulsar partition, allows users to let them
     * exploit the per-partition characteristics.
     *
     * <p>When a subtask of a FlinkTDMQSource reads multiple pulsar partitions,
     * the streams from the partitions are unioned in a "first come first serve" fashion.
     * Per-partition characteristics are usually lost that way. For example, if the timestamps are
     * strictly ascending per Pulsar partition, they will not be strictly ascending in the resulting
     * Flink DataStream, if the parallel source subtask reads more than one partition.
     *
     * <p>Common watermark generation patterns can be found as static methods in the
     * {@link WatermarkStrategy} class.
     *
     * @return The consumer object, to allow function chaining.
     */
    public FlinkPulsarSource<T> assignTimestampsAndWatermarks(
            WatermarkStrategy<T> watermarkStrategy) {
        checkNotNull(watermarkStrategy);

        try {
            ClosureCleaner.clean(watermarkStrategy, ExecutionConfig.ClosureCleanerLevel.RECURSIVE, true);
            this.watermarkStrategy = new SerializedValue<>(watermarkStrategy);
        } catch (Exception e) {
            throw new IllegalArgumentException("The given WatermarkStrategy is not serializable", e);
        }

        return this;
    }

    public FlinkPulsarSource<T> setStartFromEarliest() {
        this.startupMode = StartupMode.EARLIEST;
        return this;
    }

    public FlinkPulsarSource<T> setStartFromLatest() {
        this.startupMode = StartupMode.LATEST;
        return this;
    }

    public FlinkPulsarSource<T> setStartFromSubscription(String externalSubscriptionName) {
        this.startupMode = StartupMode.EXTERNAL_SUBSCRIPTION;
        this.externalSubscriptionName = checkNotNull(externalSubscriptionName);
        return this;
    }

    public FlinkPulsarSource<T> setStartFromSubscription(String externalSubscriptionName,
            MessageId subscriptionPosition) {
        this.startupMode = StartupMode.EXTERNAL_SUBSCRIPTION;
        this.externalSubscriptionName = checkNotNull(externalSubscriptionName);
        this.subscriptionPosition = checkNotNull(subscriptionPosition);
        return this;
    }

    // ------------------------------------------------------------------------
    // Work methods
    // ------------------------------------------------------------------------

    @Override
    public void open(Configuration parameters) throws Exception {

        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(inlongMetric)
                .withInlongAudit(inlongAudit)
                .withRegisterMetric(RegisteredMetric.ALL)
                .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_IN) : 0L)
                .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_IN) : 0L)
                .build();

        if (metricOption != null) {
            sourceMetricData = new SourceMetricData(metricOption, getRuntimeContext().getMetricGroup());
        }

        if (this.deserializer != null) {

            DynamicPulsarDeserializationSchema dynamicKafkaDeserializationSchema =
                    (DynamicPulsarDeserializationSchema) deserializer;
            dynamicKafkaDeserializationSchema.setMetricData(sourceMetricData);

            this.deserializer.open(
                    RuntimeContextInitializationContextAdapters.deserializationAdapter(
                            getRuntimeContext(),
                            metricGroup -> metricGroup.addGroup("user")));

        }

        this.taskIndex = getRuntimeContext().getIndexOfThisSubtask();
        this.numParallelTasks = getRuntimeContext().getNumberOfParallelSubtasks();

        this.metadataReader = createMetadataReader();

        ownedTopicStarts = new HashMap<>();
        excludeStartMessageIds = new HashSet<>();
        Set<TopicRange> allTopics = metadataReader.discoverTopicChanges();

        Map<TopicRange, MessageId> allTopicOffsets = offsetForEachTopic(allTopics, startupMode);

        boolean usingRestoredState = (startupMode != StartupMode.EXTERNAL_SUBSCRIPTION) || stateSubEqualexternalSub;

        if (restoredState != null && usingRestoredState) {
            allTopicOffsets.entrySet().stream()
                    .filter(e -> !restoredState.containsKey(e.getKey()))
                    .forEach(e -> restoredState.put(e.getKey(), e.getValue()));

            SerializableRange subTaskRange = metadataReader.getRange();
            restoredState.entrySet().stream()
                    .filter(
                            e -> SourceSinkUtils.belongsTo(
                                    e.getKey().getTopic(),
                                    subTaskRange,
                                    numParallelTasks,
                                    taskIndex))
                    .forEach(
                            e -> {
                                TopicRange tr =
                                        new TopicRange(
                                                e.getKey().getTopic(),
                                                subTaskRange.getPulsarRange());
                                ownedTopicStarts.put(tr, e.getValue());
                                excludeStartMessageIds.add(e.getKey());
                            });

            Set<TopicRange> goneTopics =
                    Sets.difference(restoredState.keySet(), allTopics).stream()
                            .filter(
                                    k -> SourceSinkUtils.belongsTo(
                                            k.getTopic(),
                                            subTaskRange,
                                            numParallelTasks,
                                            taskIndex))
                            .map(k -> new TopicRange(k.getTopic(), subTaskRange.getPulsarRange()))
                            .collect(Collectors.toSet());

            for (TopicRange goneTopic : goneTopics) {
                log.warn(goneTopic + " is removed from subscription since "
                        + "it no longer matches with topics settings.");
                ownedTopicStarts.remove(goneTopic);
            }

            log.info("Source {} will start reading {} topics in restored state {}",
                    taskIndex, ownedTopicStarts.size(), StringUtils.join(ownedTopicStarts.entrySet()));
        } else {
            ownedTopicStarts.putAll(
                    allTopicOffsets.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            if (ownedTopicStarts.isEmpty()) {
                log.info("Source {} initially has no topics to read from.", taskIndex);
            } else {
                log.info("Source {} will start reading {} topics from initialized positions: {}",
                        taskIndex, ownedTopicStarts.size(), ownedTopicStarts);
            }
        }
    }

    protected String getSubscriptionName() {
        if (startupMode == StartupMode.EXTERNAL_SUBSCRIPTION) {
            checkNotNull(externalSubscriptionName);
            return externalSubscriptionName;
        } else {
            return "flink-pulsar-" + uuid.toString();
        }
    }

    protected PulsarMetadataReader createMetadataReader() throws PulsarClientException {
        return new PulsarMetadataReader(
                serverUrl,
                clientConfigurationData,
                getSubscriptionName(),
                caseInsensitiveParams,
                taskIndex,
                numParallelTasks,
                startupMode == StartupMode.EXTERNAL_SUBSCRIPTION);
    }

    @Override
    public void run(SourceContext<T> ctx) throws Exception {
        if (ownedTopicStarts == null) {
            throw new Exception("The partitions were not set for the source");
        }

        this.successfulCommits =
                this.getRuntimeContext().getMetricGroup().counter(COMMITS_SUCCEEDED_METRICS_COUNTER);
        this.failedCommits =
                this.getRuntimeContext().getMetricGroup().counter(COMMITS_FAILED_METRICS_COUNTER);

        if (ownedTopicStarts.isEmpty()) {
            ctx.markAsTemporarilyIdle();
        }

        log.info("Source {} creating fetcher with offsets {}",
                taskIndex,
                StringUtils.join(ownedTopicStarts.entrySet()));

        // from this point forward:
        // - 'snapshotState' will draw offsets from the fetcher,
        // instead of being built from `subscribedPartitionsToStartOffsets`
        // - 'notifyCheckpointComplete' will start to do work (i.e. commit offsets to
        // Pulsar through the fetcher, if configured to do so)

        StreamingRuntimeContext streamingRuntime = (StreamingRuntimeContext) getRuntimeContext();

        this.pulsarFetcher = createFetcher(
                ctx,
                ownedTopicStarts,
                watermarkStrategy,
                streamingRuntime.getProcessingTimeService(),
                streamingRuntime.getExecutionConfig().getAutoWatermarkInterval(),
                getRuntimeContext().getUserCodeClassLoader(),
                streamingRuntime,
                useMetrics,
                excludeStartMessageIds);

        if (!running) {
            return;
        }

        if (discoveryIntervalMillis < 0) {
            pulsarFetcher.runFetchLoop();
        } else {
            runWithTopicsDiscovery();
        }
    }

    protected PulsarFetcher<T> createFetcher(
            SourceContext<T> sourceContext,
            Map<TopicRange, MessageId> seedTopicsWithInitialOffsets,
            SerializedValue<WatermarkStrategy<T>> watermarkStrategy,
            ProcessingTimeService processingTimeProvider,
            long autoWatermarkInterval,
            ClassLoader userCodeClassLoader,
            StreamingRuntimeContext streamingRuntime,
            boolean useMetrics,
            Set<TopicRange> excludeStartMessageIds) throws Exception {

        // readerConf.putIfAbsent(PulsarOptions.SUBSCRIPTION_ROLE_OPTION_KEY, getSubscriptionName());

        return new PulsarFetcher<>(
                sourceContext,
                seedTopicsWithInitialOffsets,
                excludeStartMessageIds,
                watermarkStrategy,
                processingTimeProvider,
                autoWatermarkInterval,
                userCodeClassLoader,
                streamingRuntime,
                clientConfigurationData,
                readerConf,
                pollTimeoutMs,
                commitMaxRetries,
                deserializer,
                metadataReader,
                streamingRuntime.getMetricGroup().addGroup(PULSAR_SOURCE_METRICS_GROUP),
                useMetrics);
    }

    public void joinDiscoveryLoopThread() throws InterruptedException {
        if (discoveryLoopThread != null) {
            discoveryLoopThread.join();
        }
    }

    public void runWithTopicsDiscovery() throws Exception {
        AtomicReference<Exception> discoveryLoopErrorRef = new AtomicReference<>();
        createAndStartDiscoveryLoop(discoveryLoopErrorRef);

        pulsarFetcher.runFetchLoop();

        joinDiscoveryLoopThread();

        Exception discoveryLoopError = discoveryLoopErrorRef.get();
        if (discoveryLoopError != null) {
            throw new RuntimeException(discoveryLoopError);
        }
    }

    private void createAndStartDiscoveryLoop(AtomicReference<Exception> discoveryLoopErrorRef) {
        discoveryLoopThread = new Thread(
                () -> {
                    try {
                        while (running) {
                            Set<TopicRange> added = metadataReader.discoverTopicChanges();

                            if (running && !added.isEmpty()) {
                                pulsarFetcher.addDiscoveredTopics(added);
                            }

                            if (running && discoveryIntervalMillis != -1) {
                                Thread.sleep(discoveryIntervalMillis);
                            }
                        }
                    } catch (PulsarMetadataReader.ClosedException e) {
                        // break out while and do nothing
                    } catch (InterruptedException e) {
                        // break out while and do nothing
                    } catch (Exception e) {
                        discoveryLoopErrorRef.set(e);
                    } finally {
                        if (running) {
                            // calling cancel will also let the fetcher loop escape
                            // (if not running, cancel() was already called)
                            cancel();
                        }
                    }
                }, "Pulsar topic discovery for source " + taskIndex);
        discoveryLoopThread.start();
    }

    @Override
    public void close() throws Exception {
        cancel();

        joinDiscoveryLoopThread();

        Exception exception = null;

        if (metadataReader != null) {
            try {
                metadataReader.close();
            } catch (Exception e) {
                exception = e;
            }
        }

        try {
            super.close();
        } catch (Exception e) {
            exception = ExceptionUtils.firstOrSuppressed(e, exception);
        }

        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void cancel() {
        running = false;

        if (discoveryLoopThread != null) {
            discoveryLoopThread.interrupt();
        }

        if (pulsarFetcher != null) {
            try {
                pulsarFetcher.cancel();
            } catch (Exception e) {
                log.error("Failed to cancel the Pulsar Fetcher {}", ExceptionUtils.stringifyException(e));
                throw new RuntimeException(e);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ResultTypeQueryable methods
    // ------------------------------------------------------------------------

    @Override
    public TypeInformation<T> getProducedType() {
        return deserializer.getProducedType();
    }

    // ------------------------------------------------------------------------
    // Checkpoint and restore
    // ------------------------------------------------------------------------

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        OperatorStateStore stateStore = context.getOperatorStateStore();

        unionOffsetStates = stateStore.getUnionListState(
                new ListStateDescriptor<>(
                        OFFSETS_STATE_NAME_V3,
                        createStateSerializer()));

        if (this.inlongMetric != null) {
            this.metricStateListState =
                    stateStore.getUnionListState(
                            new ListStateDescriptor<>(
                                    INLONG_METRIC_STATE_NAME, TypeInformation.of(new TypeHint<MetricState>() {
                                    })));
        }

        if (context.isRestored()) {
            restoredState = new TreeMap<>();
            Iterator<Tuple2<TopicSubscription, MessageId>> iterator = unionOffsetStates.get().iterator();

            metricState = MetricStateUtils.restoreMetricState(metricStateListState,
                    getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());

            if (!iterator.hasNext()) {
                iterator = tryMigrateState(stateStore);
            }
            while (iterator.hasNext()) {
                final Tuple2<TopicSubscription, MessageId> tuple2 = iterator.next();
                final SerializableRange range =
                        tuple2.f0.getRange() != null ? tuple2.f0.getRange() : SerializableRange.ofFullRange();
                final TopicRange topicRange =
                        new TopicRange(tuple2.f0.getTopic(), range.getPulsarRange());
                restoredState.put(topicRange, tuple2.f1);
                String subscriptionName = tuple2.f0.getSubscriptionName();
                if (!stateSubEqualexternalSub && StringUtils.equals(subscriptionName, externalSubscriptionName)) {
                    stateSubEqualexternalSub = true;
                    log.info("Source restored state with subscriptionName {}", subscriptionName);
                }
            }
            log.info("Source subtask {} restored state {}",
                    taskIndex,
                    StringUtils.join(restoredState.entrySet()));
        } else {
            log.info("Source subtask {} has no restore state", taskIndex);
        }
    }

    @VisibleForTesting
    static TupleSerializer<Tuple2<TopicSubscription, MessageId>> createStateSerializer() {
        // explicit serializer will keep the compatibility with GenericTypeInformation and allow to
        // disableGenericTypes for users
        TypeSerializer<?>[] fieldSerializers =
                new TypeSerializer<?>[]{
                        TopicSubscriptionSerializer.INSTANCE,
                        MessageIdSerializer.INSTANCE
                };
        @SuppressWarnings("unchecked")
        Class<Tuple2<TopicSubscription, MessageId>> tupleClass =
                (Class<Tuple2<TopicSubscription, MessageId>>) (Class<?>) Tuple2.class;
        return new TupleSerializer<>(tupleClass, fieldSerializers);
    }

    /**
     * Try to restore the old save point.
     *
     * @param stateStore state store
     * @return state data
     * @throws Exception Type incompatibility, serialization failure
     */
    private Iterator<Tuple2<TopicSubscription, MessageId>> tryMigrateState(OperatorStateStore stateStore)
            throws Exception {
        log.info("restore old state version {}", oldStateVersion);
        PulsarSourceStateSerializer stateSerializer =
                new PulsarSourceStateSerializer(getRuntimeContext().getExecutionConfig());
        // Since stateStore.getUnionListState gets the data of a state point,
        // it can only be registered once and will fail to register again,
        // so it only allows the user to set a version number.
        ListState<?> rawStates = stateStore.getUnionListState(new ListStateDescriptor<>(
                OFFSETS_STATE_NAME,
                stateSerializer.getSerializer(oldStateVersion)));

        ListState<String> oldUnionSubscriptionNameStates =
                stateStore.getUnionListState(
                        new ListStateDescriptor<>(
                                OFFSETS_STATE_NAME + "_subName",
                                TypeInformation.of(new TypeHint<String>() {
                                })));
        final Iterator<String> subNameIterator = oldUnionSubscriptionNameStates.get().iterator();

        Iterator<?> tuple2s = rawStates.get().iterator();
        log.info("restore old state has data {}", tuple2s.hasNext());
        final List<Tuple2<TopicSubscription, MessageId>> records = new ArrayList<>();
        while (tuple2s.hasNext()) {
            final Object next = tuple2s.next();
            Tuple2<TopicSubscription, MessageId> tuple2 = stateSerializer.deserialize(oldStateVersion, next);

            String subName = tuple2.f0.getSubscriptionName();
            if (subNameIterator.hasNext()) {
                subName = subNameIterator.next();
            }
            final TopicSubscription topicSubscription = TopicSubscription.builder()
                    .topic(tuple2.f0.getTopic())
                    .range(tuple2.f0.getRange())
                    .subscriptionName(subName)
                    .build();
            final Tuple2<TopicSubscription, MessageId> record = Tuple2.of(topicSubscription, tuple2.f1);
            log.info("migrationState {}", record);
            records.add(record);
        }
        rawStates.clear();
        oldUnionSubscriptionNameStates.clear();
        return records.listIterator();
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        if (!running) {
            log.debug("snapshotState() called on closed source");
        } else {

            if (sourceMetricData != null && metricStateListState != null) {
                MetricStateUtils.snapshotMetricStateForSourceMetricData(metricStateListState, sourceMetricData,
                        getRuntimeContext().getIndexOfThisSubtask());
            }

            unionOffsetStates.clear();

            PulsarFetcher<T> fetcher = this.pulsarFetcher;

            if (fetcher == null) {
                // the fetcher has not yet been initialized, which means we need to return the
                // originally restored offsets or the assigned partitions
                for (Map.Entry<TopicRange, MessageId> entry : ownedTopicStarts.entrySet()) {
                    final TopicSubscription topicSubscription = TopicSubscription.builder()
                            .topic(entry.getKey().getTopic())
                            .range(entry.getKey().getRange())
                            .subscriptionName(getSubscriptionName())
                            .build();
                    unionOffsetStates.add(Tuple2.of(topicSubscription, entry.getValue()));
                }
                pendingOffsetsToCommit.put(context.getCheckpointId(), restoredState);
            } else {
                Map<TopicRange, MessageId> currentOffsets = fetcher.snapshotCurrentState();
                pendingOffsetsToCommit.put(context.getCheckpointId(), currentOffsets);
                for (Map.Entry<TopicRange, MessageId> entry : currentOffsets.entrySet()) {
                    final TopicSubscription topicSubscription = TopicSubscription.builder()
                            .topic(entry.getKey().getTopic())
                            .range(entry.getKey().getRange())
                            .subscriptionName(getSubscriptionName())
                            .build();
                    unionOffsetStates.add(Tuple2.of(topicSubscription, entry.getValue()));
                }

                int exceed = pendingOffsetsToCommit.size() - MAX_NUM_PENDING_CHECKPOINTS;
                Iterator<Long> iterator = pendingOffsetsToCommit.keySet().iterator();

                while (iterator.hasNext() && exceed > 0) {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {
        if (!running) {
            log.info("notifyCheckpointComplete() called on closed source");
            return;
        }

        PulsarFetcher<T> fetcher = this.pulsarFetcher;

        if (fetcher == null) {
            log.info("notifyCheckpointComplete() called on uninitialized source");
            return;
        }

        log.debug("Source {} received confirmation for unknown checkpoint id {}",
                taskIndex, checkpointId);

        try {
            if (!pendingOffsetsToCommit.containsKey(checkpointId)) {
                log.warn("Source {} received confirmation for unknown checkpoint id {}",
                        taskIndex, checkpointId);
                return;
            }

            Map<TopicRange, MessageId> offset = pendingOffsetsToCommit.get(checkpointId);

            // remove older checkpoints in map
            Iterator<Long> iterator = pendingOffsetsToCommit.keySet().iterator();
            while (iterator.hasNext()) {
                Long key = iterator.next();
                iterator.remove();
                if (Objects.equals(key, checkpointId)) {
                    break;
                }
            }

            if (offset == null || offset.size() == 0) {
                log.debug("Source {} has empty checkpoint state", taskIndex);
                return;
            }
            fetcher.commitOffsetToState(offset);
        } catch (Exception e) {
            if (running) {
                throw e;
            }
        }
    }

    @Override
    public void notifyCheckpointAborted(long checkpointId) throws Exception {
        log.error("checkpoint aborted, checkpointId: {}", checkpointId);
    }

    public Map<TopicRange, MessageId> offsetForEachTopic(
            Set<TopicRange> topics,
            StartupMode mode) {

        switch (mode) {
            case LATEST:
                return topics.stream()
                        .collect(Collectors.toMap(k -> k, k -> MessageId.latest));
            case EARLIEST:
                return topics.stream()
                        .collect(Collectors.toMap(k -> k, k -> MessageId.earliest));
            default:
                throw new IllegalArgumentException(
                        "Unknown startup mode option: " + mode);
        }
    }

    public Map<Long, Map<TopicRange, MessageId>> getPendingOffsetsToCommit() {
        return pendingOffsetsToCommit;
    }

    public Map<TopicRange, MessageId> getOwnedTopicStarts() {
        return ownedTopicStarts;
    }
}
