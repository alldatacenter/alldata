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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.iceberg;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.iceberg.format.IcebergFormatPlugin;
import org.apache.drill.exec.store.iceberg.plan.DrillExprToIcebergTranslator;
import org.apache.drill.exec.store.iceberg.snapshot.Snapshot;
import org.apache.drill.exec.store.schedule.AffinityCreator;
import org.apache.drill.exec.store.schedule.AssignmentCreator;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.hadoop.HadoopTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("iceberg-scan")
@SuppressWarnings("unused")
public class IcebergGroupScan extends AbstractGroupScan {

  private static final Logger logger = LoggerFactory.getLogger(IcebergGroupScan.class);

  private final IcebergFormatPlugin formatPlugin;

  private final String path;

  private final TupleMetadata schema;

  private final LogicalExpression condition;

  private final DrillFileSystem fs;

  private final List<SchemaPath> columns;

  private int maxRecords;

  private List<IcebergCompleteWork> chunks;

  private TableScan tableScan;

  private List<EndpointAffinity> endpointAffinities;

  private ListMultimap<Integer, IcebergCompleteWork> mappings;

  @JsonCreator
  public IcebergGroupScan(
      @JsonProperty("userName") String userName,
      @JsonProperty("storage") StoragePluginConfig storageConfig,
      @JsonProperty("format") FormatPluginConfig formatConfig,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("schema") TupleMetadata schema,
      @JsonProperty("path") String path,
      @JsonProperty("condition") LogicalExpression condition,
      @JsonProperty("maxRecords") Integer maxRecords,
      @JacksonInject StoragePluginRegistry pluginRegistry) throws IOException {
    this(builder()
      .userName(userName)
      .formatPlugin(pluginRegistry.resolveFormat(storageConfig, formatConfig, IcebergFormatPlugin.class))
      .schema(schema)
      .path(path)
      .condition(condition)
      .columns(columns)
      .maxRecords(maxRecords));
  }

  private IcebergGroupScan(IcebergGroupScanBuilder builder) throws IOException {
    super(builder.userName);
    this.formatPlugin = builder.formatPlugin;
    this.columns = builder.columns;
    this.path = builder.path;
    this.schema = builder.schema;
    this.condition = builder.condition;
    this.maxRecords = builder.maxRecords;
    this.fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.resolveUserName(userName), formatPlugin.getFsConf());
    this.tableScan = initTableScan(formatPlugin, path, condition);

    init();
  }

  public static TableScan initTableScan(IcebergFormatPlugin formatPlugin, String path, LogicalExpression condition) {
    TableScan tableScan = new HadoopTables(formatPlugin.getFsConf()).load(path).newScan();
    Map<String, String> properties = formatPlugin.getConfig().getProperties();
    if (properties != null) {
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        tableScan = tableScan.option(entry.getKey(), entry.getValue());
      }
    }
    if (condition != null) {
      Expression expression = condition.accept(
        DrillExprToIcebergTranslator.INSTANCE, null);
      tableScan = tableScan.filter(expression);
    }
    Snapshot snapshot = formatPlugin.getConfig().getSnapshot();
    if (snapshot != null) {
      tableScan = snapshot.apply(tableScan);
    }
    Boolean caseSensitive = formatPlugin.getConfig().getCaseSensitive();
    if (caseSensitive != null) {
      tableScan = tableScan.caseSensitive(caseSensitive);
    }
    Boolean includeColumnStats = formatPlugin.getConfig().getIncludeColumnStats();
    if (includeColumnStats != null && includeColumnStats) {
      tableScan = tableScan.includeColumnStats();
    }
    Boolean ignoreResiduals = formatPlugin.getConfig().getIgnoreResiduals();
    if (ignoreResiduals != null && ignoreResiduals) {
      tableScan = tableScan.ignoreResiduals();
    }
    return tableScan;
  }

  /**
   * Private constructor, used for cloning.
   *
   * @param that The IcebergGroupScan to clone
   */
  private IcebergGroupScan(IcebergGroupScan that) {
    super(that);
    this.columns = that.columns;
    this.formatPlugin = that.formatPlugin;
    this.path = that.path;
    this.condition = that.condition;
    this.schema = that.schema;
    this.mappings = that.mappings;
    this.fs = that.fs;
    this.maxRecords = that.maxRecords;
    this.chunks = that.chunks;
    this.tableScan = that.tableScan;
    this.endpointAffinities = that.endpointAffinities;
  }

  public static IcebergGroupScanBuilder builder() {
    return new IcebergGroupScanBuilder();
  }

  @Override
  public IcebergGroupScan clone(List<SchemaPath> columns) {
    try {
      return toBuilder().columns(columns).build();
    } catch (IOException e) {
      throw new DrillRuntimeException(e);
    }
  }

  @Override
  public IcebergGroupScan applyLimit(int maxRecords) {
    IcebergGroupScan clone = new IcebergGroupScan(this);
    clone.maxRecords = maxRecords;
    return clone;
  }

  @Override
  public void applyAssignments(List<DrillbitEndpoint> endpoints) {
    mappings = AssignmentCreator.getMappings(endpoints, chunks);
  }

  private void createMappings(List<EndpointAffinity> affinities) {
    List<DrillbitEndpoint> endpoints = affinities.stream()
      .map(EndpointAffinity::getEndpoint)
      .collect(Collectors.toList());
    applyAssignments(endpoints);
  }

  @Override
  public IcebergSubScan getSpecificScan(int minorFragmentId) {
    if (mappings == null) {
      createMappings(endpointAffinities);
    }
    assert minorFragmentId < mappings.size() : String.format(
      "Mappings length [%d] should be longer than minor fragment id [%d] but it isn't.", mappings.size(),
      minorFragmentId);

    List<IcebergCompleteWork> workList = mappings.get(minorFragmentId);

    Preconditions.checkArgument(!workList.isEmpty(),
      String.format("MinorFragmentId %d has no read entries assigned", minorFragmentId));

    IcebergSubScan subScan = IcebergSubScan.builder()
      .userName(userName)
      .formatPlugin(formatPlugin)
      .columns(columns)
      .condition(condition)
      .schema(schema)
      .workList(convertWorkList(workList))
      .tableScan(tableScan)
      .path(path)
      .maxRecords(maxRecords)
      .build();

    subScan.setOperatorId(getOperatorId());
    return subScan;
  }

  private List<IcebergWork> convertWorkList(List<IcebergCompleteWork> workList) {
    return workList.stream()
      .map(IcebergCompleteWork::getScanTask)
      .map(IcebergWork::new)
      .collect(Collectors.toList());
  }

  @JsonIgnore
  public TableScan getTableScan() {
    return tableScan;
  }

  @JsonProperty("maxRecords")
  public int getMaxRecords() {
    return maxRecords;
  }

  @Override
  public int getMaxParallelizationWidth() {
    return chunks.size();
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public ScanStats getScanStats() {
    int expectedRecordsPerChunk = 1_000_000;
    if (maxRecords >= 0) {
      expectedRecordsPerChunk = Math.max(maxRecords, 1);
    }
    int estimatedRecords = chunks.size() * expectedRecordsPerChunk;
    return new ScanStats(ScanStats.GroupScanProperty.NO_EXACT_ROW_COUNT, estimatedRecords, 1, 0);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new IcebergGroupScan(this);
  }

  private void init() throws IOException {
    tableScan = projectColumns(tableScan, columns);
    chunks = new IcebergBlockMapBuilder(fs, formatPlugin.getContext().getBits())
      .generateFileWork(tableScan.planTasks());
    endpointAffinities = AffinityCreator.getAffinityMap(chunks);
  }

  public static TableScan projectColumns(TableScan tableScan, List<SchemaPath> columns) {
    boolean hasStar = columns.stream()
      .anyMatch(SchemaPath::isDynamicStar);
    if (!hasStar) {
      List<String> projectColumns = columns.stream()
        .map(IcebergGroupScan::getPath)
        .collect(Collectors.toList());
      return tableScan.select(projectColumns);
    }
    return tableScan;
  }

  public static String getPath(SchemaPath schemaPath) {
    StringBuilder sb = new StringBuilder();
    PathSegment segment = schemaPath.getRootSegment();
    sb.append(segment.getNameSegment().getPath());

    while ((segment = segment.getChild()) != null) {
      sb.append('.')
        .append(segment.isNamed()
          ? segment.getNameSegment().getPath()
          : "element");
    }
    return sb.toString();
  }

  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    if (endpointAffinities == null) {
      logger.debug("Chunks size: {}", chunks.size());
      endpointAffinities = AffinityCreator.getAffinityMap(chunks);
    }
    return endpointAffinities;
  }

  @Override
  public boolean supportsLimitPushdown() {
    return false;
  }

  @Override
  @JsonProperty("columns")
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @JsonProperty("schema")
  public TupleMetadata getSchema() {
    return schema;
  }

  @JsonProperty("storage")
  public StoragePluginConfig getStorageConfig() {
    return formatPlugin.getStorageConfig();
  }

  @JsonProperty("format")
  public FormatPluginConfig getFormatConfig() {
    return formatPlugin.getConfig();
  }

  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  @JsonProperty("condition")
  public LogicalExpression getCondition() {
    return condition;
  }

  @JsonIgnore
  public IcebergFormatPlugin getFormatPlugin() {
    return formatPlugin;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("path", path)
      .field("schema", schema)
      .field("columns", columns)
      .field("tableScan", tableScan)
      .field("maxRecords", maxRecords)
      .toString();
  }

  public IcebergGroupScanBuilder toBuilder() {
    return new IcebergGroupScanBuilder()
      .userName(this.userName)
      .formatPlugin(this.formatPlugin)
      .schema(this.schema)
      .path(this.path)
      .condition(this.condition)
      .columns(this.columns)
      .maxRecords(this.maxRecords);
  }

  public static class IcebergGroupScanBuilder {
    private String userName;

    private IcebergFormatPlugin formatPlugin;

    private TupleMetadata schema;

    private String path;

    private LogicalExpression condition;

    private List<SchemaPath> columns;

    private int maxRecords;

    public IcebergGroupScanBuilder userName(String userName) {
      this.userName = userName;
      return this;
    }

    public IcebergGroupScanBuilder formatPlugin(IcebergFormatPlugin formatPlugin) {
      this.formatPlugin = formatPlugin;
      return this;
    }

    public IcebergGroupScanBuilder schema(TupleMetadata schema) {
      this.schema = schema;
      return this;
    }

    public IcebergGroupScanBuilder path(String path) {
      this.path = path;
      return this;
    }

    public IcebergGroupScanBuilder condition(LogicalExpression condition) {
      this.condition = condition;
      return this;
    }

    public IcebergGroupScanBuilder columns(List<SchemaPath> columns) {
      this.columns = columns;
      return this;
    }

    public IcebergGroupScanBuilder maxRecords(int maxRecords) {
      this.maxRecords = maxRecords;
      return this;
    }

    public IcebergGroupScan build() throws IOException {
      return new IcebergGroupScan(this);
    }
  }
}
