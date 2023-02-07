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
package org.apache.drill.exec.store.parquet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.drill.common.expression.ExpressionStringBuilder;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.FilterPredicate;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.metastore.store.parquet.ParquetMetadataProvider;
import org.apache.drill.exec.metastore.store.parquet.ParquetMetadataProviderBuilder;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.drill.exec.physical.EndpointAffinity;
import org.apache.drill.exec.physical.base.AbstractGroupScanWithMetadata;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.schedule.AffinityCreator;
import org.apache.drill.exec.store.schedule.AssignmentCreator;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.drill.exec.store.schedule.EndpointByteMapImpl;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.statistics.ExactStatisticsConstants;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.util.TableMetadataUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.LinkedListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.ListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.hadoop.fs.Path;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractParquetGroupScan extends AbstractGroupScanWithMetadata<ParquetMetadataProvider> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractParquetGroupScan.class);

  protected List<ReadEntryWithPath> entries;
  protected Multimap<Path, RowGroupMetadata> rowGroups;

  protected ListMultimap<Integer, RowGroupInfo> mappings;
  protected ParquetReaderConfig readerConfig;

  private List<EndpointAffinity> endpointAffinities;
  // used for applying assignments for incoming endpoints
  private List<RowGroupInfo> rowGroupInfos;

  protected AbstractParquetGroupScan(String userName,
                                     List<SchemaPath> columns,
                                     List<ReadEntryWithPath> entries,
                                     ParquetReaderConfig readerConfig,
                                     LogicalExpression filter) {
    super(userName, columns, filter);
    this.entries = entries;
    this.readerConfig = readerConfig == null ? ParquetReaderConfig.getDefaultInstance() : readerConfig;
  }

  // immutable copy constructor
  protected AbstractParquetGroupScan(AbstractParquetGroupScan that) {
    super(that);

    this.rowGroups = that.rowGroups;

    this.endpointAffinities = that.endpointAffinities == null ? null : new ArrayList<>(that.endpointAffinities);
    this.mappings = that.mappings == null ? null : ArrayListMultimap.create(that.mappings);

    this.entries = that.entries == null ? null : new ArrayList<>(that.entries);
    this.readerConfig = that.readerConfig;

  }

  @JsonProperty
  public List<ReadEntryWithPath> getEntries() {
    return entries;
  }

  @JsonProperty("readerConfig")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  // do not serialize reader config if it contains all default values
  public ParquetReaderConfig getReaderConfigForSerialization() {
    return ParquetReaderConfig.getDefaultInstance().equals(readerConfig) ? null : readerConfig;
  }

  @JsonIgnore
  public ParquetReaderConfig getReaderConfig() {
    return readerConfig;
  }

  /**
   * This method is excluded from serialization in this group scan
   * since the actual files list to scan in this class is handled by {@link #entries} field.
   */
  @JsonIgnore
  @Override
  public Collection<Path> getFiles() {
    return super.getFiles();
  }

  @Override
  public boolean canPushdownProjects(List<SchemaPath> columns) {
    return true;
  }

  @Override
  public boolean supportsFilterPushDown() {
    return true;
  }

  /**
   * Calculates the affinity each endpoint has for this scan,
   * by adding up the affinity each endpoint has for each rowGroup.
   *
   * @return a list of EndpointAffinity objects
   */
  @Override
  public List<EndpointAffinity> getOperatorAffinity() {
    if (endpointAffinities == null) {
      this.endpointAffinities = AffinityCreator.getAffinityMap(getRowGroupInfos());
    }
    return endpointAffinities;
  }

  @Override
  public void applyAssignments(List<CoordinationProtos.DrillbitEndpoint> incomingEndpoints) {
    this.mappings = AssignmentCreator.getMappings(incomingEndpoints, getRowGroupInfos());
  }

  private List<RowGroupInfo> getRowGroupInfos() {
    if (rowGroupInfos == null) {
      Map<String, CoordinationProtos.DrillbitEndpoint> hostEndpointMap = new HashMap<>();

      for (CoordinationProtos.DrillbitEndpoint endpoint : getDrillbits()) {
        hostEndpointMap.put(endpoint.getAddress(), endpoint);
      }

      rowGroupInfos = new ArrayList<>();
      for (RowGroupMetadata rowGroupMetadata : getRowGroupsMetadata().values()) {
        RowGroupInfo rowGroupInfo = new RowGroupInfo(rowGroupMetadata.getPath(),
            rowGroupMetadata.getStatistic(() -> ExactStatisticsConstants.START),
            rowGroupMetadata.getStatistic(() -> ExactStatisticsConstants.LENGTH),
            rowGroupMetadata.getRowGroupIndex(),
            TableStatisticsKind.ROW_COUNT.getValue(rowGroupMetadata));
        rowGroupInfo.setNumRecordsToRead(rowGroupInfo.getRowCount());

        EndpointByteMap endpointByteMap = new EndpointByteMapImpl();
        for (String host : rowGroupMetadata.getHostAffinity().keySet()) {
          if (hostEndpointMap.containsKey(host)) {
            endpointByteMap.add(hostEndpointMap.get(host),
              (long) (rowGroupMetadata.getHostAffinity().get(host) * (long) rowGroupMetadata.getStatistic(() -> ExactStatisticsConstants.LENGTH)));
          }
        }

        rowGroupInfo.setEndpointByteMap(endpointByteMap);

        rowGroupInfos.add(rowGroupInfo);
      }
    }
    return rowGroupInfos;
  }

  @Override
  public int getMaxParallelizationWidth() {
    if (!getRowGroupsMetadata().isEmpty()) {
      return getRowGroupsMetadata().size();
    } else if (!getFilesMetadata().isEmpty()) {
      return getFilesMetadata().size();
    } else {
      return !getPartitionsMetadata().isEmpty() ? getPartitionsMetadata().size() : 1;
    }
  }

  protected List<RowGroupReadEntry> getReadEntries(int minorFragmentId) {
    assert minorFragmentId < mappings.size() : String
        .format("Mappings length [%d] should be longer than minor fragment id [%d] but it isn't.",
            mappings.size(), minorFragmentId);

    List<RowGroupInfo> rowGroupsForMinor = mappings.get(minorFragmentId);

    Preconditions.checkArgument(!rowGroupsForMinor.isEmpty(),
        String.format("MinorFragmentId %d has no read entries assigned", minorFragmentId));

    List<RowGroupReadEntry> readEntries = new ArrayList<>();
    for (RowGroupInfo rgi : rowGroupsForMinor) {
      RowGroupReadEntry entry = new RowGroupReadEntry(rgi.getPath(), rgi.getStart(),
          rgi.getLength(), rgi.getRowGroupIndex(),
          rgi.getNumRecordsToRead());
      readEntries.add(entry);
    }
    return readEntries;
  }

  /**
   * {@inheritDoc}
   * <ul>
   * <ul><li>file metadata was pruned, prunes underlying metadata</li></ul>
   * <li>row group level:
   * <ul><li>if filter matches all the the data or prunes all the data, sets corresponding value to
   * {@link AbstractParquetGroupScan#isMatchAllMetadata()} and returns null</li></ul></li>
   * </ul>
   *
   * @return group scan with applied filter expression
   */
  @Override
  public AbstractGroupScanWithMetadata<?> applyFilter(LogicalExpression filterExpr, UdfUtilities udfUtilities,
      FunctionImplementationRegistry functionImplementationRegistry, OptionManager optionManager) {
    // Builds filter for pruning. If filter cannot be built, null should be returned.
    FilterPredicate<?> filterPredicate = getFilterPredicate(filterExpr, udfUtilities, functionImplementationRegistry, optionManager, true);
    if (filterPredicate == null) {
      logger.debug("FilterPredicate cannot be built.");
      return null;
    }

    RowGroupScanFilterer<?> filteredMetadata = getFilterer()
        .filterExpression(filterExpr)
        .schema(tableMetadata.getSchema())
        .context(functionImplementationRegistry)
        .udfUtilities(udfUtilities)
        .getFiltered(optionManager, filterPredicate);

    // checks whether metadata for specific level was available and there was no reduction of metadata
    if (isGroupScanFullyMatchesFilter(filteredMetadata)) {
      logger.debug("applyFilter() does not have any pruning");
      matchAllMetadata = filteredMetadata.isMatchAllMetadata();
      return null;
    }

    if (isAllDataPruned(filteredMetadata)) {
      if (getRowGroupsMetadata().size() == 1) {
        // For the case when group scan has single row group and it was filtered,
        // no need to create new group scan with the same row group.
        return null;
      }

      // Stop files pruning for the case:
      //    -  # of row groups is beyond PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD.
      if (getRowGroupsMetadata().size() >= optionManager.getOption(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD)) {
        this.rowGroups = getRowGroupsMetadata();
        matchAllMetadata = false;
        logger.trace("Stopping plan time pruning. Metadata has {} rowgroups, but the threshold option is set to {} rowgroups", this.rowGroups.size(),
          optionManager.getOption(PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD));
        return null;
      }

      logger.debug("All row groups have been filtered out. Add back one to get schema from scanner");

      Map<Path, SegmentMetadata> segmentsMap = getNextOrEmpty(getSegmentsMetadata().values()).stream()
          .collect(Collectors.toMap(SegmentMetadata::getPath, Function.identity()));

      Map<Path, FileMetadata> filesMap = getNextOrEmpty(getFilesMetadata().values()).stream()
          .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));

      Multimap<Path, RowGroupMetadata> rowGroupsMap = LinkedListMultimap.create();
      getNextOrEmpty(getRowGroupsMetadata().values()).forEach(entry -> rowGroupsMap.put(entry.getPath(), entry));

      filteredMetadata.rowGroups(rowGroupsMap)
          .table(getTableMetadata())
          .segments(segmentsMap)
          .partitions(getNextOrEmpty(getPartitionsMetadata()))
          .nonInterestingColumns(getNonInterestingColumnsMetadata())
          .files(filesMap)
          .matching(false);
    }

    if (filteredMetadata.getOverflowLevel() != MetadataType.NONE) {
      if (logger.isWarnEnabled()) {
        logger.warn("applyFilter {} wasn't able to do pruning for  all metadata levels filter condition, since metadata count for " +
              "{} level exceeds `planner.store.parquet.rowgroup.filter.pushdown.threshold` value.\n" +
              "But underlying metadata was pruned without filter expression according to the metadata with above level.",
            ExpressionStringBuilder.toString(filterExpr), filteredMetadata.getOverflowLevel());
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("applyFilter {} reduce row groups # from {} to {}",
          ExpressionStringBuilder.toString(filterExpr), getRowGroupsMetadata().size(), filteredMetadata.getRowGroups().size());
    }

    return filteredMetadata.build();
  }

  private boolean isAllDataPruned(RowGroupScanFilterer<?> filteredMetadata) {
    return !filteredMetadata.isMatchAllMetadata()
        && (super.isAllDataPruned(filteredMetadata)
            // all row groups are pruned if row group metadata is available
            || filteredMetadata.getRowGroups().isEmpty() && !getRowGroupsMetadata().isEmpty());
  }

  private boolean isGroupScanFullyMatchesFilter(RowGroupScanFilterer<?> filteredMetadata) {
    if (!getRowGroupsMetadata().isEmpty()) {
      return getRowGroupsMetadata().size() == filteredMetadata.getRowGroups().size();
    } else {
      return super.isGroupScanFullyMatchesFilter(filteredMetadata);
    }
  }

  protected Multimap<Path, RowGroupMetadata> pruneRowGroupsForFiles(Map<Path, FileMetadata> filteredFileMetadata) {
    Multimap<Path, RowGroupMetadata> prunedRowGroups = LinkedListMultimap.create();
    for (Path filteredPartition : filteredFileMetadata.keySet()) {
      Multimap<Path, RowGroupMetadata> rowGroupsMetadata = getRowGroupsMetadata();
      Collection<RowGroupMetadata> filesRowGroupMetadata = rowGroupsMetadata.get(filteredPartition);
      if (CollectionUtils.isNotEmpty(filesRowGroupMetadata)) {
        prunedRowGroups.putAll(filteredPartition, filesRowGroupMetadata);
      }
    }

    return prunedRowGroups;
  }

  // filter push down methods block end

  // limit push down methods start
  @Override
  public GroupScan applyLimit(int maxRecords) {
    maxRecords = Math.max(maxRecords, 1); // Make sure it request at least 1 row -> 1 rowGroup.
    if (getTableMetadata() != null) {
      long tableRowCount = TableStatisticsKind.ROW_COUNT.getValue(getTableMetadata());
      if (tableRowCount == Statistic.NO_COLUMN_STATS || tableRowCount <= maxRecords) {
        logger.debug("limit push down does not apply, since total number of rows [{}] is less or equal to the required [{}].",
            tableRowCount, maxRecords);
        return null;
      }
    }

    List<RowGroupMetadata> qualifiedRowGroups = limitMetadata(getRowGroupsMetadata().values(), maxRecords);

    if (qualifiedRowGroups == null || getRowGroupsMetadata().size() == qualifiedRowGroups.size()) {
      logger.debug("limit push down does not apply, since number of row groups was not reduced.");
      return null;
    }

    Map<Path, FileMetadata> filesMetadata = getFilesMetadata();
    Map<Path, FileMetadata> qualifiedFiles = qualifiedRowGroups.stream()
        .map(rowGroup -> filesMetadata.get(rowGroup.getPath()))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));

    Multimap<Path, RowGroupMetadata> prunedRowGroups = LinkedListMultimap.create();

    for (RowGroupMetadata qualifiedRowGroup : qualifiedRowGroups) {
      prunedRowGroups.put(qualifiedRowGroup.getPath(), qualifiedRowGroup);
    }

    return getFilterer()
        .rowGroups(prunedRowGroups)
        .table(tableMetadata)
        .partitions(partitions)
        .segments(segments)
        .files(qualifiedFiles)
        .nonInterestingColumns(nonInterestingColumnsMetadata)
        .matching(matchAllMetadata)
        .build();
  }
  // limit push down methods end

  // helper method used for partition pruning and filter push down
  @Override
  public void modifyFileSelection(FileSelection selection) {
    super.modifyFileSelection(selection);

    List<Path> files = selection.getFiles();
    fileSet = new HashSet<>(files);
    entries = new ArrayList<>(files.size());

    entries.addAll(files.stream()
        .map(ReadEntryWithPath::new)
        .collect(Collectors.toList()));

    Multimap<Path, RowGroupMetadata> newRowGroups = LinkedListMultimap.create();
    if (!getRowGroupsMetadata().isEmpty()) {
      getRowGroupsMetadata().entries().stream()
          .filter(entry -> fileSet.contains(entry.getKey()))
          .forEachOrdered(entry -> newRowGroups.put(entry.getKey(), entry.getValue()));
    }
    this.rowGroups = newRowGroups;

    tableMetadata = TableMetadataUtils.updateRowCount(getTableMetadata(), getRowGroupsMetadata().values());

    if (!getFilesMetadata().isEmpty()) {
      this.files = getFilesMetadata().entrySet().stream()
          .filter(entry -> fileSet.contains(entry.getKey()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } else {
      this.files = Collections.emptyMap();
    }

    List<PartitionMetadata> newPartitions = new ArrayList<>();
    if (!getPartitionsMetadata().isEmpty()) {
      for (PartitionMetadata entry : getPartitionsMetadata()) {
        for (Path partLocation : entry.getLocations()) {
          if (fileSet.contains(partLocation)) {
            newPartitions.add(entry);
            break;
          }
        }
      }
    }
    partitions = newPartitions;

    if (!getSegmentsMetadata().isEmpty()) {
      this.segments = getSegmentsMetadata().entrySet().stream()
          .filter(entry -> fileSet.contains(entry.getKey()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    rowGroupInfos = null;
  }

  // protected methods block
  protected Multimap<Path, RowGroupMetadata> getRowGroupsMetadata() {
    if (rowGroups == null) {
      rowGroups = metadataProvider.getRowGroupsMetadataMap();
    }
    return rowGroups;
  }

  /**
   * Removes metadata which does not belong to any of partitions in metadata list.
   *
   * @param metadataToPrune           list of metadata which should be pruned
   * @param filteredPartitionMetadata list of partition metadata which was pruned
   * @param <T>                       type of metadata to filter
   * @return list with metadata which belongs to pruned partitions
   */
  protected static <T extends BaseMetadata & LocationProvider> Multimap<Path, T> pruneForPartitions(Multimap<Path, T> metadataToPrune,
      List<PartitionMetadata> filteredPartitionMetadata) {
    Multimap<Path, T> prunedFiles = LinkedListMultimap.create();
    if (metadataToPrune != null) {
      for (Map.Entry<Path, T> entry : metadataToPrune.entries()) {
        for (PartitionMetadata filteredPartition : filteredPartitionMetadata) {
          if (filteredPartition.getLocations().contains(entry.getKey())) {
            prunedFiles.put(entry.getKey(), entry.getValue());
            break;
          }
        }
      }
    }

    return prunedFiles;
  }

  // abstract methods block start
  protected abstract Collection<CoordinationProtos.DrillbitEndpoint> getDrillbits();
  protected abstract AbstractParquetGroupScan cloneWithFileSelection(Collection<Path> filePaths) throws IOException;

  // narrows the return type
  @Override
  protected abstract ParquetMetadataProviderBuilder<?> defaultTableMetadataProviderBuilder(MetadataProviderManager source);

  @Override
  protected abstract RowGroupScanFilterer<? extends RowGroupScanFilterer<?>> getFilterer();
  // abstract methods block end

  /**
   * This class is responsible for filtering different metadata levels including row group level.
   */
  protected abstract static class RowGroupScanFilterer<B extends RowGroupScanFilterer<B>> extends GroupScanWithMetadataFilterer<B> {
    protected Multimap<Path, RowGroupMetadata> rowGroups = LinkedListMultimap.create();

    public RowGroupScanFilterer(AbstractGroupScanWithMetadata<?> source) {
      super(source);
    }

    public B rowGroups(Multimap<Path, RowGroupMetadata> rowGroups) {
      this.rowGroups = rowGroups;
      return self();
    }

    /**
     * Returns new {@link AbstractParquetGroupScan} instance to be populated with filtered metadata
     * from this {@link RowGroupScanFilterer} instance.
     *
     * @return new {@link AbstractParquetGroupScan} instance
     */
    protected abstract AbstractParquetGroupScan getNewScan();

    public Multimap<Path, RowGroupMetadata> getRowGroups() {
      return rowGroups;
    }

    @Override
    public AbstractParquetGroupScan build() {
      AbstractParquetGroupScan newScan = getNewScan();
      newScan.tableMetadata = tableMetadata;
      // updates common row count and nulls counts for every column
      if (newScan.getTableMetadata() != null && rowGroups != null && newScan.getRowGroupsMetadata().size() != rowGroups.size()) {
        newScan.tableMetadata = TableMetadataUtils.updateRowCount(newScan.getTableMetadata(), rowGroups.values());
      }
      newScan.partitions = partitions;
      newScan.segments = segments;
      newScan.files = files;
      newScan.rowGroups = rowGroups;
      newScan.matchAllMetadata = matchAllMetadata;
      newScan.nonInterestingColumnsMetadata = nonInterestingColumnsMetadata;
      newScan.limit = limit;
      // since builder is used when pruning happens, entries and fileSet should be expanded
      if (!newScan.getFilesMetadata().isEmpty()) {
        newScan.entries = newScan.getFilesMetadata().keySet().stream()
            .map(ReadEntryWithPath::new)
            .collect(Collectors.toList());

        newScan.fileSet = new HashSet<>(newScan.getFilesMetadata().keySet());
      } else if (!newScan.getRowGroupsMetadata().isEmpty()) {
        newScan.entries = newScan.getRowGroupsMetadata().keySet().stream()
            .map(ReadEntryWithPath::new)
            .collect(Collectors.toList());

        newScan.fileSet = new HashSet<>(newScan.getRowGroupsMetadata().keySet());
      }

      return newScan;
    }

    @Override
    protected B getFiltered(OptionManager optionManager, FilterPredicate<?> filterPredicate) {
      super.getFiltered(optionManager, filterPredicate);

      if (!((AbstractParquetGroupScan) source).getRowGroupsMetadata().isEmpty()) {
        filterRowGroupMetadata(optionManager, filterPredicate);
      }
      return self();
    }

    /**
     * Produces filtering of metadata at row group level.
     *
     * @param optionManager     option manager
     * @param filterPredicate   filter expression
     */
    protected void filterRowGroupMetadata(OptionManager optionManager,
                                          FilterPredicate<?> filterPredicate) {
      Set<SchemaPath> schemaPathsInExpr =
          filterExpression.accept(FilterEvaluatorUtils.FieldReferenceFinder.INSTANCE, null);

      AbstractParquetGroupScan abstractParquetGroupScan = (AbstractParquetGroupScan) source;
      Multimap<Path, RowGroupMetadata> prunedRowGroups;
      if (!abstractParquetGroupScan.getFilesMetadata().isEmpty()
          && abstractParquetGroupScan.getFilesMetadata().size() > getFiles().size()) {
        // prunes row groups to leave only row groups which are contained by pruned files
        prunedRowGroups = abstractParquetGroupScan.pruneRowGroupsForFiles(getFiles());
      } else if (!abstractParquetGroupScan.getPartitionsMetadata().isEmpty()
          && abstractParquetGroupScan.getPartitionsMetadata().size() > getPartitions().size()) {
        // prunes row groups to leave only row groups which are contained by pruned partitions
        prunedRowGroups = pruneForPartitions(abstractParquetGroupScan.getRowGroupsMetadata(), getPartitions());
      } else if (!abstractParquetGroupScan.getSegmentsMetadata().isEmpty()
          && abstractParquetGroupScan.getSegmentsMetadata().size() > getSegments().size()) {
        // prunes row groups to leave only row groups which are contained by pruned segments
        prunedRowGroups = pruneForSegments(abstractParquetGroupScan.getRowGroupsMetadata(), getSegments());
      } else {
        // no partition or file pruning happened, no need to prune initial row groups list
        prunedRowGroups = abstractParquetGroupScan.getRowGroupsMetadata();
      }

      if (isMatchAllMetadata()) {
        this.rowGroups = prunedRowGroups;
        return;
      }

      // Stop files pruning for the case:
      //    -  # of row groups is beyond PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD.
      if (prunedRowGroups.size() <= optionManager.getOption(
        PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD)) {
        matchAllMetadata = true;
        List<RowGroupMetadata> filteredRowGroups = filterAndGetMetadata(schemaPathsInExpr, prunedRowGroups.values(), filterPredicate, optionManager);

        this.rowGroups = LinkedListMultimap.create();
        filteredRowGroups.forEach(entry -> this.rowGroups.put(entry.getPath(), entry));
        // updates files list to include only present row groups
        if (MapUtils.isNotEmpty(files)) {
          files = rowGroups.keySet().stream()
              .map(files::get)
              .collect(Collectors.toMap(
                  FileMetadata::getPath,
                  Function.identity(),
                  (o, n) -> n,
                  LinkedHashMap::new));
        }
      } else {
        this.rowGroups = prunedRowGroups;
        matchAllMetadata = false;
        overflowLevel = MetadataType.ROW_GROUP;
      }
    }

    /**
     * Produces filtering of metadata at file level.
     *
     * @param optionManager     option manager
     * @param filterPredicate   filter expression
     * @param schemaPathsInExpr columns used in filter expression
     */
    @Override
    protected void filterFileMetadata(OptionManager optionManager,
        FilterPredicate<?> filterPredicate,
        Set<SchemaPath> schemaPathsInExpr) {
      Map<Path, FileMetadata> prunedFiles;
      if (!source.getPartitionsMetadata().isEmpty()
          && source.getPartitionsMetadata().size() > getPartitions().size()) {
        // prunes files to leave only files which are contained by pruned partitions
        prunedFiles = pruneForPartitions(source.getFilesMetadata(), getPartitions());
      } else if (!source.getSegmentsMetadata().isEmpty()
          && source.getSegmentsMetadata().size() > getSegments().size()) {
        // prunes row groups to leave only row groups which are contained by pruned segments
        prunedFiles = pruneForSegments(source.getFilesMetadata(), getSegments());
      } else {
        prunedFiles = source.getFilesMetadata();
      }

      if (isMatchAllMetadata()) {
        files = prunedFiles;
        return;
      }

      // files which have only single row group may be pruned when pruning row groups
      Map<Path, FileMetadata> omittedFiles = new HashMap<>();

      AbstractParquetGroupScan abstractParquetGroupScan = (AbstractParquetGroupScan) source;

      Map<Path, FileMetadata> filesToFilter = new HashMap<>(prunedFiles);
      if (!abstractParquetGroupScan.rowGroups.isEmpty()) {
        prunedFiles.forEach((path, fileMetadata) -> {
          if (abstractParquetGroupScan.rowGroups.get(path).size() == 1) {
            omittedFiles.put(path, fileMetadata);
            filesToFilter.remove(path);
          }
        });
      }

      // Stop files pruning for the case:
      //    -  # of files is beyond PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD.
      if (filesToFilter.size() <= optionManager.getOption(
          PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD)) {

        matchAllMetadata = true;
        files = filterAndGetMetadata(schemaPathsInExpr, filesToFilter.values(), filterPredicate, optionManager).stream()
            .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));

        files.putAll(omittedFiles);
      } else {
        matchAllMetadata = false;
        files = prunedFiles;
        overflowLevel = MetadataType.FILE;
      }
    }

    /**
     * Removes metadata which does not belong to any of segments in metadata list.
     *
     * @param metadataToPrune         list of metadata which should be pruned
     * @param filteredSegmentMetadata list of segment metadata which was pruned
     * @param <T>                     type of metadata to filter
     * @return multimap with metadata which belongs to pruned segments
     */
    protected static <T extends BaseMetadata & LocationProvider> Multimap<Path, T> pruneForSegments(
        Multimap<Path, T> metadataToPrune, Map<Path, SegmentMetadata> filteredSegmentMetadata) {
        Multimap<Path, T> prunedFiles = LinkedListMultimap.create();
      if (metadataToPrune != null) {
        for (Map.Entry<Path, T> entry : metadataToPrune.entries()) {
          for (SegmentMetadata filteredPartition : filteredSegmentMetadata.values()) {
            if (filteredPartition.getLocations().contains(entry.getKey())) {
              prunedFiles.put(entry.getKey(), entry.getValue());
              break;
            }
          }
        }
      }
      return prunedFiles;
    }
  }
}
