/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iceberg.flink.source;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.Schema;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.flink.FlinkConfigOptions;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.apache.iceberg.TableProperties.DEFAULT_NAME_MAPPING;

/**
 * Copy from Iceberg {@link org.apache.iceberg.flink.source.ScanContext}.
 * only change line 115 and expand the modifier.
 * Context object with optional arguments for a Flink Scan.
 */
public class ScanContext implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final ConfigOption<Long> SNAPSHOT_ID =
      ConfigOptions.key("snapshot-id").longType().defaultValue(null)
        .withDescription("Retrieve the full data of the specified snapshot by ID, used for batch scan mode");

  public static final ConfigOption<Boolean> CASE_SENSITIVE =
      ConfigOptions.key("case-sensitive").booleanType().defaultValue(false)
        .withDescription("Set if column names are case-sensitive");

  public static final ConfigOption<Long> AS_OF_TIMESTAMP =
      ConfigOptions.key("as-of-timestamp").longType().defaultValue(null)
        .withDescription("Retrieve the full data of the specified snapshot at the given timestamp, " +
          "used for batch scan mode");

  public static final ConfigOption<StreamingStartingStrategy> STARTING_STRATEGY =
      ConfigOptions.key("starting-strategy")
      .enumType(StreamingStartingStrategy.class)
      .defaultValue(StreamingStartingStrategy.INCREMENTAL_FROM_LATEST_SNAPSHOT)
        .withDescription("Specific the starting strategy for streaming execution");

  public static final ConfigOption<Long> START_SNAPSHOT_TIMESTAMP =
      ConfigOptions.key("start-snapshot-timestamp").longType().defaultValue(null)
        .withDescription("Specific the snapshot timestamp that streaming job starts from");

  public static final ConfigOption<Long> START_SNAPSHOT_ID =
      ConfigOptions.key("start-snapshot-id").longType().defaultValue(null)
        .withDescription("Specific the snapshot id that streaming job starts from");

  public static final ConfigOption<Long> END_SNAPSHOT_ID =
      ConfigOptions.key("end-snapshot-id").longType().defaultValue(null)
        .withDescription("Specific the snapshot id that streaming job to end");

  public static final ConfigOption<Long> SPLIT_SIZE =
      ConfigOptions.key("split-size").longType().defaultValue(null)
        .withDescription("Specific the target size when combining data input splits");

  public static final ConfigOption<Integer> SPLIT_LOOKBACK =
      ConfigOptions.key("split-lookback").intType().defaultValue(null)
        .withDescription("Specify the number of bins to consider when combining input splits");

  public static final ConfigOption<Long> SPLIT_FILE_OPEN_COST =
      ConfigOptions.key("split-file-open-cost").longType().defaultValue(null)
        .withDescription("The estimated cost to open a file, used as a minimum weight when combining splits");

  public static final ConfigOption<Boolean> STREAMING =
      ConfigOptions.key("streaming").booleanType().defaultValue(true)
        .withDescription("Set if job is bounded or unbounded");

  public static final ConfigOption<Duration> MONITOR_INTERVAL =
      ConfigOptions.key("monitor-interval").durationType().defaultValue(Duration.ofSeconds(10))
        .withDescription("Specify the time interval for consecutively monitoring newly committed data files");

  public static final ConfigOption<Boolean> INCLUDE_COLUMN_STATS =
      ConfigOptions.key("include-column-stats").booleanType().defaultValue(false)
        .withDescription("Set if loads the column stats with each file");

  public static final ConfigOption<Integer> MAX_PLANNING_SNAPSHOT_COUNT =
      ConfigOptions.key("max-planning-snapshot-count").intType().defaultValue(Integer.MAX_VALUE)
        .withDescription("Specify the max planning snapshot count");

  protected final boolean caseSensitive;
  protected final boolean exposeLocality;
  protected final Long snapshotId;
  protected final StreamingStartingStrategy startingStrategy;
  protected final Long startSnapshotId;
  protected final Long startSnapshotTimestamp;
  protected final Long endSnapshotId;
  protected final Long asOfTimestamp;
  protected final Long splitSize;
  protected final Integer splitLookback;
  protected final Long splitOpenFileCost;
  protected final boolean isStreaming;
  protected final Duration monitorInterval;

  protected final String nameMapping;
  protected final Schema schema;
  protected final List<Expression> filters;
  protected final long limit;
  protected final boolean includeColumnStats;
  protected final Integer planParallelism;
  protected final int maxPlanningSnapshotCount;

  protected ScanContext(
      boolean caseSensitive,
      Long snapshotId,
      StreamingStartingStrategy startingStrategy,
      Long startSnapshotTimestamp,
      Long startSnapshotId,
      Long endSnapshotId,
      Long asOfTimestamp,
      Long splitSize,
      Integer splitLookback,
      Long splitOpenFileCost,
      boolean isStreaming,
      Duration monitorInterval,
      String nameMapping,
      Schema schema,
      List<Expression> filters,
      long limit,
      boolean includeColumnStats,
      boolean exposeLocality,
      Integer planParallelism,
      int maxPlanningSnapshotCount) {
    this.caseSensitive = caseSensitive;
    this.snapshotId = snapshotId;
    this.startingStrategy = startingStrategy;
    this.startSnapshotTimestamp = startSnapshotTimestamp;
    this.startSnapshotId = startSnapshotId;
    this.endSnapshotId = endSnapshotId;
    this.asOfTimestamp = asOfTimestamp;
    this.splitSize = splitSize;
    this.splitLookback = splitLookback;
    this.splitOpenFileCost = splitOpenFileCost;
    this.isStreaming = isStreaming;
    this.monitorInterval = monitorInterval;

    this.nameMapping = nameMapping;
    this.schema = schema;
    this.filters = filters;
    this.limit = limit;
    this.includeColumnStats = includeColumnStats;
    this.exposeLocality = exposeLocality;
    this.planParallelism = planParallelism;
    this.maxPlanningSnapshotCount = maxPlanningSnapshotCount;

    validate();
  }

  private void validate() {
    if (isStreaming) {
      if (startingStrategy == StreamingStartingStrategy.INCREMENTAL_FROM_SNAPSHOT_ID) {
        Preconditions.checkArgument(
            startSnapshotId != null,
            "Invalid starting snapshot id for SPECIFIC_START_SNAPSHOT_ID strategy: null");
        Preconditions.checkArgument(
            startSnapshotTimestamp == null,
            "Invalid starting snapshot timestamp for SPECIFIC_START_SNAPSHOT_ID strategy: not null");
      }
      if (startingStrategy == StreamingStartingStrategy.INCREMENTAL_FROM_SNAPSHOT_TIMESTAMP) {
        Preconditions.checkArgument(
            startSnapshotTimestamp != null,
            "Invalid starting snapshot timestamp for SPECIFIC_START_SNAPSHOT_TIMESTAMP strategy: null");
        Preconditions.checkArgument(
            startSnapshotId == null,
            "Invalid starting snapshot id for SPECIFIC_START_SNAPSHOT_ID strategy: not null");
      }
    }
  }

  boolean caseSensitive() {
    return caseSensitive;
  }

  Long snapshotId() {
    return snapshotId;
  }

  StreamingStartingStrategy streamingStartingStrategy() {
    return startingStrategy;
  }

  Long startSnapshotTimestamp() {
    return startSnapshotTimestamp;
  }

  Long startSnapshotId() {
    return startSnapshotId;
  }

  Long endSnapshotId() {
    return endSnapshotId;
  }

  Long asOfTimestamp() {
    return asOfTimestamp;
  }

  Long splitSize() {
    return splitSize;
  }

  Integer splitLookback() {
    return splitLookback;
  }

  Long splitOpenFileCost() {
    return splitOpenFileCost;
  }

  boolean isStreaming() {
    return isStreaming;
  }

  Duration monitorInterval() {
    return monitorInterval;
  }

  String nameMapping() {
    return nameMapping;
  }

  Schema project() {
    return schema;
  }

  List<Expression> filters() {
    return filters;
  }

  long limit() {
    return limit;
  }

  boolean includeColumnStats() {
    return includeColumnStats;
  }

  boolean exposeLocality() {
    return exposeLocality;
  }

  Integer planParallelism() {
    return planParallelism;
  }

  int maxPlanningSnapshotCount() {
    return maxPlanningSnapshotCount;
  }

  ScanContext copyWithAppendsBetween(Long newStartSnapshotId, long newEndSnapshotId) {
    return ScanContext.builder()
        .caseSensitive(caseSensitive)
        .useSnapshotId(null)
        .startSnapshotId(newStartSnapshotId)
        .endSnapshotId(newEndSnapshotId)
        .asOfTimestamp(null)
        .splitSize(splitSize)
        .splitLookback(splitLookback)
        .splitOpenFileCost(splitOpenFileCost)
        .streaming(isStreaming)
        .monitorInterval(monitorInterval)
        .nameMapping(nameMapping)
        .project(schema)
        .filters(filters)
        .limit(limit)
        .includeColumnStats(includeColumnStats)
        .exposeLocality(exposeLocality)
        .planParallelism(planParallelism)
        .maxPlanningSnapshotCount(maxPlanningSnapshotCount)
        .build();
  }

  ScanContext copyWithSnapshotId(long newSnapshotId) {
    return ScanContext.builder()
        .caseSensitive(caseSensitive)
        .useSnapshotId(newSnapshotId)
        .startSnapshotId(null)
        .endSnapshotId(null)
        .asOfTimestamp(null)
        .splitSize(splitSize)
        .splitLookback(splitLookback)
        .splitOpenFileCost(splitOpenFileCost)
        .streaming(isStreaming)
        .monitorInterval(monitorInterval)
        .nameMapping(nameMapping)
        .project(schema)
        .filters(filters)
        .limit(limit)
        .includeColumnStats(includeColumnStats)
        .exposeLocality(exposeLocality)
        .planParallelism(planParallelism)
        .maxPlanningSnapshotCount(maxPlanningSnapshotCount)
        .build();
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private boolean caseSensitive = CASE_SENSITIVE.defaultValue();
    private Long snapshotId = SNAPSHOT_ID.defaultValue();
    private StreamingStartingStrategy startingStrategy = STARTING_STRATEGY.defaultValue();
    private Long startSnapshotTimestamp = START_SNAPSHOT_TIMESTAMP.defaultValue();
    private Long startSnapshotId = START_SNAPSHOT_ID.defaultValue();
    private Long endSnapshotId = END_SNAPSHOT_ID.defaultValue();
    private Long asOfTimestamp = AS_OF_TIMESTAMP.defaultValue();
    private Long splitSize = SPLIT_SIZE.defaultValue();
    private Integer splitLookback = SPLIT_LOOKBACK.defaultValue();
    private Long splitOpenFileCost = SPLIT_FILE_OPEN_COST.defaultValue();
    private boolean isStreaming = STREAMING.defaultValue();
    private Duration monitorInterval = MONITOR_INTERVAL.defaultValue();
    private String nameMapping;
    private Schema projectedSchema;
    private List<Expression> filters;
    private long limit = -1L;
    private boolean includeColumnStats = INCLUDE_COLUMN_STATS.defaultValue();
    private boolean exposeLocality;
    private Integer planParallelism =
        FlinkConfigOptions.TABLE_EXEC_ICEBERG_WORKER_POOL_SIZE.defaultValue();
    private int maxPlanningSnapshotCount = MAX_PLANNING_SNAPSHOT_COUNT.defaultValue();

    private Builder() {
    }

    Builder caseSensitive(boolean newCaseSensitive) {
      this.caseSensitive = newCaseSensitive;
      return this;
    }

    Builder useSnapshotId(Long newSnapshotId) {
      this.snapshotId = newSnapshotId;
      return this;
    }

    Builder startingStrategy(StreamingStartingStrategy newStartingStrategy) {
      this.startingStrategy = newStartingStrategy;
      return this;
    }

    Builder startSnapshotTimestamp(Long newStartSnapshotTimestamp) {
      this.startSnapshotTimestamp = newStartSnapshotTimestamp;
      return this;
    }

    Builder startSnapshotId(Long newStartSnapshotId) {
      this.startSnapshotId = newStartSnapshotId;
      return this;
    }

    Builder endSnapshotId(Long newEndSnapshotId) {
      this.endSnapshotId = newEndSnapshotId;
      return this;
    }

    Builder asOfTimestamp(Long newAsOfTimestamp) {
      this.asOfTimestamp = newAsOfTimestamp;
      return this;
    }

    Builder splitSize(Long newSplitSize) {
      this.splitSize = newSplitSize;
      return this;
    }

    Builder splitLookback(Integer newSplitLookback) {
      this.splitLookback = newSplitLookback;
      return this;
    }

    Builder splitOpenFileCost(Long newSplitOpenFileCost) {
      this.splitOpenFileCost = newSplitOpenFileCost;
      return this;
    }

    Builder streaming(boolean streaming) {
      this.isStreaming = streaming;
      return this;
    }

    Builder monitorInterval(Duration newMonitorInterval) {
      this.monitorInterval = newMonitorInterval;
      return this;
    }

    Builder nameMapping(String newNameMapping) {
      this.nameMapping = newNameMapping;
      return this;
    }

    Builder project(Schema newProjectedSchema) {
      this.projectedSchema = newProjectedSchema;
      return this;
    }

    Builder filters(List<Expression> newFilters) {
      this.filters = newFilters;
      return this;
    }

    Builder limit(long newLimit) {
      this.limit = newLimit;
      return this;
    }

    Builder includeColumnStats(boolean newIncludeColumnStats) {
      this.includeColumnStats = newIncludeColumnStats;
      return this;
    }

    Builder exposeLocality(boolean newExposeLocality) {
      this.exposeLocality = newExposeLocality;
      return this;
    }

    Builder planParallelism(Integer parallelism) {
      this.planParallelism = parallelism;
      return this;
    }

    Builder maxPlanningSnapshotCount(int newMaxPlanningSnapshotCount) {
      this.maxPlanningSnapshotCount = newMaxPlanningSnapshotCount;
      return this;
    }

    Builder fromProperties(Map<String, String> properties) {
      Configuration config = new Configuration();
      properties.forEach(config::setString);

      return this.useSnapshotId(config.get(SNAPSHOT_ID))
          .caseSensitive(config.get(CASE_SENSITIVE))
          .asOfTimestamp(config.get(AS_OF_TIMESTAMP))
          .startingStrategy(config.get(STARTING_STRATEGY))
          .startSnapshotTimestamp(config.get(START_SNAPSHOT_TIMESTAMP))
          .startSnapshotId(config.get(START_SNAPSHOT_ID))
          .endSnapshotId(config.get(END_SNAPSHOT_ID))
          .splitSize(config.get(SPLIT_SIZE))
          .splitLookback(config.get(SPLIT_LOOKBACK))
          .splitOpenFileCost(config.get(SPLIT_FILE_OPEN_COST))
          .streaming(config.get(STREAMING))
          .monitorInterval(config.get(MONITOR_INTERVAL))
          .nameMapping(properties.get(DEFAULT_NAME_MAPPING))
          .includeColumnStats(config.get(INCLUDE_COLUMN_STATS))
          .maxPlanningSnapshotCount(config.get(MAX_PLANNING_SNAPSHOT_COUNT));
    }

    public ScanContext build() {
      return new ScanContext(
          caseSensitive,
          snapshotId,
          startingStrategy,
          startSnapshotTimestamp,
          startSnapshotId,
          endSnapshotId,
          asOfTimestamp,
          splitSize,
          splitLookback,
          splitOpenFileCost,
          isStreaming,
          monitorInterval,
          nameMapping,
          projectedSchema,
          filters,
          limit,
          includeColumnStats,
          exposeLocality,
          planParallelism,
          maxPlanningSnapshotCount);
    }
  }
}
