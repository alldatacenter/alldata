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
package org.apache.drill.exec.store.dfs.easy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.metastore.store.MetastoreFileTableMetadataProvider;
import org.apache.drill.exec.metastore.MetastoreMetadataProviderManager;
import org.apache.drill.exec.metastore.store.SimpleFileTableMetadataProvider;
import org.apache.drill.exec.metastore.analyze.AnalyzeFileInfoProviderImpl;
import org.apache.drill.exec.metastore.analyze.AnalyzeInfoProvider;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.physical.base.AbstractGroupScanWithMetadata;
import org.apache.drill.exec.physical.base.FileGroupScan;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.metastore.store.FileTableMetadataProviderBuilder;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.schedule.AffinityCreator;
import org.apache.drill.exec.store.schedule.AssignmentCreator;
import org.apache.drill.exec.store.schedule.BlockMapBuilder;
import org.apache.drill.exec.store.schedule.CompleteFileWork;
import org.apache.drill.exec.store.schedule.CompleteFileWork.FileWorkImpl;
import org.apache.drill.exec.util.ImpersonationUtil;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.metastore.util.TableMetadataUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@JsonTypeName("fs-scan")
public class EasyGroupScan extends AbstractGroupScanWithMetadata<TableMetadataProvider> {
  private static final Logger logger = LoggerFactory.getLogger(EasyGroupScan.class);

  private final EasyFormatPlugin<?> formatPlugin;
  private FileSelection selection;
  private int partitionDepth;
  private int maxWidth;
  private int minWidth = 1;

  private ListMultimap<Integer, CompleteFileWork> mappings;
  private List<CompleteFileWork> chunks;
  private List<EndpointAffinity> endpointAffinities;
  private final Path selectionRoot;

  @JsonCreator
  public EasyGroupScan(
      @JsonProperty("userName") String userName,
      @JsonProperty("files") List<Path> files,
      @JsonProperty("storage") StoragePluginConfig storageConfig,
      @JsonProperty("format") FormatPluginConfig formatConfig,
      @JacksonInject StoragePluginRegistry engineRegistry,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("selectionRoot") Path selectionRoot,
      @JsonProperty("schema") TupleMetadata schema,
      @JsonProperty("limit") int limit
      ) throws IOException {
    super(ImpersonationUtil.resolveUserName(userName), columns, ValueExpressions.BooleanExpression.TRUE);
    this.selection = FileSelection.create(null, files, selectionRoot);
    this.formatPlugin = engineRegistry.resolveFormat(storageConfig, formatConfig, EasyFormatPlugin.class);
    this.columns = columns == null ? ALL_COLUMNS : columns;
    this.selectionRoot = selectionRoot;
    this.limit = limit;
    this.metadataProvider = defaultTableMetadataProviderBuilder(new FileSystemMetadataProviderManager())
        .withSelection(selection)
        .withSchema(schema)
        .build();
    initFromSelection(selection, formatPlugin);
  }

  public EasyGroupScan(
      String userName,
      FileSelection selection,
      EasyFormatPlugin<?> formatPlugin,
      List<SchemaPath> columns,
      Path selectionRoot,
      MetadataProviderManager metadataProviderManager
      ) throws IOException {
    super(userName, columns, ValueExpressions.BooleanExpression.TRUE);
    this.selection = Preconditions.checkNotNull(selection);
    this.formatPlugin = Preconditions.checkNotNull(formatPlugin,
        "Unable to load format plugin for provided format config.");
    this.columns = columns == null ? ALL_COLUMNS : columns;
    this.selectionRoot = selectionRoot;
    if (metadataProviderManager == null) {
      // use file system metadata provider without specified schema and statistics
      metadataProviderManager = new FileSystemMetadataProviderManager();
    }
    DrillFileSystem fs =
        ImpersonationUtil.createFileSystem(ImpersonationUtil.resolveUserName(userName), formatPlugin.getFsConf());

    this.metadataProvider = tableMetadataProviderBuilder(metadataProviderManager)
        .withSelection(selection)
        .withFileSystem(fs)
        .build();
    this.usedMetastore = metadataProviderManager.usesMetastore();
    initFromSelection(selection, formatPlugin);
    checkMetadataConsistency(selection, formatPlugin.getFsConf());
  }

  public EasyGroupScan(
      String userName,
      FileSelection selection,
      EasyFormatPlugin<?> formatPlugin,
      List<SchemaPath> columns,
      Path selectionRoot,
      int minWidth,
      MetadataProviderManager metadataProvider
      ) throws IOException {
    this(userName, selection, formatPlugin, columns, selectionRoot, metadataProvider);

    // Set the minimum width of this reader. Primarily used for testing
    // to force parallelism even for small test files.
    // See ExecConstants.MIN_READER_WIDTH
    this.minWidth = Math.max(1, Math.min(minWidth, maxWidth));

    // Compute the maximum partition depth across all files.
    partitionDepth = ColumnExplorer.getPartitionDepth(selection);
  }

  private EasyGroupScan(final EasyGroupScan that) {
    super(that);
    selection = that.selection;
    formatPlugin = that.formatPlugin;
    columns = that.columns;
    selectionRoot = that.selectionRoot;
    chunks = that.chunks;
    endpointAffinities = that.endpointAffinities;
    maxWidth = that.maxWidth;
    minWidth = that.minWidth;
    mappings = that.mappings;
    partitionDepth = that.partitionDepth;
    metadataProvider = that.metadataProvider;
  }

  @JsonIgnore
  public Iterable<CompleteFileWork> getWorkIterable() {
    return () -> Iterators.unmodifiableIterator(chunks.iterator());
  }

  private void initFromSelection(FileSelection selection, EasyFormatPlugin<?> formatPlugin) throws IOException {
    final DrillFileSystem dfs = ImpersonationUtil.createFileSystem(getUserName(), formatPlugin.getFsConf());
    this.selection = selection;
    BlockMapBuilder b = new BlockMapBuilder(dfs, formatPlugin.getContext().getBits());
    chunks = b.generateFileWork(selection.getStatuses(dfs), formatPlugin.isBlockSplittable());
    maxWidth = chunks.size();
    endpointAffinities = AffinityCreator.getAffinityMap(chunks);
  }

  @Override
  public Path getSelectionRoot() {
    return selectionRoot;
  }

  @Override
  @JsonIgnore
  public int getMinParallelizationWidth() {
    return minWidth;
  }

  @Override
  public int getMaxParallelizationWidth() {
    return maxWidth;
  }

  @Override
  public ScanStats getScanStats(final PlannerSettings settings) {
    return formatPlugin.getScanStats(settings, this);
  }

  @Override
  public boolean hasFiles() {
    return true;
  }

  @JsonProperty("files")
  @Override
  public List<Path> getFiles() {
    return selection.getFiles();
  }

  @JsonIgnore
  public FileSelection getFileSelection() {
    return selection;
  }

  @Override
  public void modifyFileSelection(FileSelection selection) {
    this.selection = selection;
  }

  @Override
  protected boolean supportsFileImplicitColumns() {
    return formatPlugin.supportsFileImplicitColumns();
  }

  @Override
  public boolean supportsFilterPushDown() {
    return usedMetastore();
  }

  @Override
  protected List<String> getPartitionValues(LocationProvider locationProvider) {
    return ColumnExplorer.listPartitionValues(locationProvider.getPath(), selectionRoot, false);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    assert children == null || children.isEmpty();
    return new EasyGroupScan(this);
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
  public void applyAssignments(List<DrillbitEndpoint> incomingEndpoints) {
    mappings = AssignmentCreator.getMappings(incomingEndpoints, chunks);
  }

  private void createMappings(List<EndpointAffinity> affinities) {
    List<DrillbitEndpoint> endpoints = Lists.newArrayList();
    for (EndpointAffinity e : affinities) {
      endpoints.add(e.getEndpoint());
    }
    applyAssignments(endpoints);
  }

  @Override
  public EasySubScan getSpecificScan(int minorFragmentId) {
    if (mappings == null) {
      createMappings(endpointAffinities);
    }
    assert minorFragmentId < mappings.size() : String.format(
        "Mappings length [%d] should be longer than minor fragment id [%d] but it isn't.", mappings.size(),
        minorFragmentId);

    List<CompleteFileWork> filesForMinor = mappings.get(minorFragmentId);

    Preconditions.checkArgument(!filesForMinor.isEmpty(),
        String.format("MinorFragmentId %d has no read entries assigned", minorFragmentId));

    EasySubScan subScan = new EasySubScan(getUserName(), convert(filesForMinor), formatPlugin,
        columns, selectionRoot, partitionDepth, getSchema(), limit);
    subScan.setOperatorId(getOperatorId());
    return subScan;
  }

  private List<FileWorkImpl> convert(List<CompleteFileWork> list) {
    List<FileWorkImpl> newList = Lists.newArrayList();
    for (CompleteFileWork f : list) {
      newList.add(f.getAsFileWork());
    }
    return newList;
  }

  @JsonProperty("storage")
  public StoragePluginConfig getStorageConfig() {
    return formatPlugin.getStorageConfig();
  }

  @JsonProperty("format")
  public FormatPluginConfig getFormatConfig() {
    return formatPlugin.getConfig();
  }

  @Override
  public String toString() {
    // Note that the output of this method is incorporated in the digest of
    // the corresponding scan node in the query plan. This means that the
    // fields included here constitute what the planner will use to decide
    // whether two scans are identical or not. E.g. the format config must be
    // present here because format config can be overriden using table functions
    // Two scans that differ by format config alone may produce different data
    // and therefore should not be considered identical.
    return new PlanStringBuilder(this)
      .field("selectionRoot", selectionRoot)
      .field("numFiles", getFiles().size())
      .field("columns", columns)
      .field("files", getFiles())
      .field("schema", getSchema())
      .field("usedMetastore", usedMetastore())
      .field("limit", limit)
      .field("formatConfig", getFormatConfig())
      .toString();
  }

  @Override
  public GroupScan clone(List<SchemaPath> columns) {
    if (!formatPlugin.supportsPushDown()) {
      throw new IllegalStateException(String.format("%s doesn't support push down.", this.getClass().getSimpleName()));
    }
    EasyGroupScan newScan = new EasyGroupScan(this);
    newScan.columns = columns;
    return newScan;
  }

  @Override
  public FileGroupScan clone(FileSelection selection) throws IOException {
    EasyGroupScan newScan = new EasyGroupScan(this);
    newScan.initFromSelection(selection, formatPlugin);
    newScan.mappings = null; /* the mapping will be created later when we get specific scan
                                since the end-point affinities are not known at this time */
    return newScan;
  }

  @Override
  @JsonIgnore
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return formatPlugin.supportsPushDown();
  }

  @Override
  public TupleMetadata getSchema() {
    return getTableMetadata().getSchema();
  }

  @Override
  public AnalyzeInfoProvider getAnalyzeInfoProvider() {
    return new AnalyzeFileInfoProviderImpl(formatPlugin.getName());
  }

  @Override
  protected GroupScanWithMetadataFilterer<?> getFilterer() {
    return new EasyGroupScanFilterer(this);
  }

  @Override
  protected FileTableMetadataProviderBuilder<?> tableMetadataProviderBuilder(MetadataProviderManager source) {
    if (source.usesMetastore()) {
      return new MetastoreFileTableMetadataProvider.Builder<>((MetastoreMetadataProviderManager) source);
    } else {
      return defaultTableMetadataProviderBuilder(source);
    }
  }

  @Override
  protected FileTableMetadataProviderBuilder<?> defaultTableMetadataProviderBuilder(MetadataProviderManager source) {
    return new SimpleFileTableMetadataProvider.Builder(source);
  }

  /**
   * Implementation of GroupScanWithMetadataFilterer which uses {@link EasyGroupScan} as source and
   * builds {@link EasyGroupScan} instance with filtered metadata.
   */
  private static class EasyGroupScanFilterer extends GroupScanWithMetadataFilterer<EasyGroupScanFilterer> {

    EasyGroupScanFilterer(EasyGroupScan source) {
      super(source);
    }

    @Override
    public AbstractGroupScanWithMetadata<?> build() {
      EasyGroupScan newScan = new EasyGroupScan((EasyGroupScan) source);
      newScan.tableMetadata = tableMetadata;
      // updates common row count and nulls counts for every column
      if (newScan.getTableMetadata() != null && MapUtils.isNotEmpty(files) && newScan.getFilesMetadata().size() != files.size()) {
        newScan.tableMetadata = TableMetadataUtils.updateRowCount(newScan.getTableMetadata(), files.values());
      }
      newScan.partitions = partitions;
      newScan.segments = segments;
      newScan.files = files;
      newScan.matchAllMetadata = matchAllMetadata;
      newScan.nonInterestingColumnsMetadata = nonInterestingColumnsMetadata;
      newScan.limit = limit;

      Map<Path, FileMetadata> filesMetadata = newScan.getFilesMetadata();
      if (MapUtils.isNotEmpty(filesMetadata)) {
        newScan.fileSet = filesMetadata.keySet();
        newScan.selection = FileSelection.create(null, new ArrayList<>(newScan.fileSet), newScan.selectionRoot);
      }
      try {
        newScan.initFromSelection(newScan.selection, newScan.formatPlugin);
      } catch (IOException e) {
        throw new DrillRuntimeException("Failed to initialize scan from the selection.", e);
      }

      return newScan;
    }

    @Override
    protected EasyGroupScanFilterer self() {
      return this;
    }
  }
}
