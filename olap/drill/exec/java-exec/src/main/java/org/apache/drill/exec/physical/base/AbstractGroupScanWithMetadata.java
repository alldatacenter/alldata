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
package org.apache.drill.exec.physical.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.ExpressionStringBuilder;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.compile.sig.ConstantExpressionIdentifier;
import org.apache.drill.exec.exception.MetadataException;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.FilterBuilder;
import org.apache.drill.exec.expr.FilterPredicate;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.expr.fn.FunctionLookupContext;
import org.apache.drill.exec.expr.stat.RowsMatch;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.metastore.analyze.FileMetadataInfoCollector;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.parquet.FilterEvaluatorUtils;
import org.apache.drill.exec.store.parquet.ParquetTableMetadataUtils;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.metastore.metadata.Metadata;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.NonInterestingColumnsMetadata;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.drill.metastore.metadata.TableMetadataProvider;
import org.apache.drill.metastore.metadata.TableMetadataProviderBuilder;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static org.apache.drill.exec.ExecConstants.SKIP_RUNTIME_ROWGROUP_PRUNING_KEY;

/**
 * Represents table group scan with metadata usage.
 */
public abstract class AbstractGroupScanWithMetadata<P extends TableMetadataProvider> extends AbstractFileGroupScan {
  static final Logger logger = LoggerFactory.getLogger(AbstractGroupScanWithMetadata.class);

  protected P metadataProvider;

  // table metadata info
  protected TableMetadata tableMetadata;

  // partition metadata info: mixed partition values for all partition keys in the same list
  protected List<PartitionMetadata> partitions;

  protected Map<Path, SegmentMetadata> segments;

  protected NonInterestingColumnsMetadata nonInterestingColumnsMetadata;
  protected List<SchemaPath> partitionColumns;
  protected LogicalExpression filter;
  protected List<SchemaPath> columns;

  protected Map<Path, FileMetadata> files;

  // set of the files to be handled
  protected Set<Path> fileSet;

  // whether all files, partitions or row groups of this group scan fully match the filter
  protected boolean matchAllMetadata;

  protected boolean usedMetastore; // false by default

  // The pushed-down limit. LIMIT 0 is valid, it means to return only schema.
  // The default is a -1, which means unlimited.
  // Note that, in "Big Data", a table might have more then 2B rows, so we
  // cannot use Integer.MAX_VALUE as the limit.
  protected int limit = -1;

  protected AbstractGroupScanWithMetadata(String userName, List<SchemaPath> columns, LogicalExpression filter) {
    super(userName);
    this.columns = columns;
    this.filter = filter;
  }

  protected AbstractGroupScanWithMetadata(AbstractGroupScanWithMetadata<P> that) {
    super(that.getUserName());
    this.columns = that.columns;
    this.filter = that.filter;
    this.matchAllMetadata = that.matchAllMetadata;

    this.metadataProvider = that.metadataProvider;
    this.tableMetadata = that.tableMetadata;
    this.partitionColumns = that.partitionColumns;
    this.partitions = that.partitions;
    this.segments = that.segments;
    this.files = that.files;
    this.usedMetastore = that.usedMetastore;
    this.nonInterestingColumnsMetadata = that.nonInterestingColumnsMetadata;
    this.fileSet = that.fileSet == null ? null : new HashSet<>(that.fileSet);
    this.limit = that.limit;
  }

  @JsonProperty("columns")
  @Override
  public List<SchemaPath> getColumns() {
    return columns;
  }

  @Override
  public Collection<Path> getFiles() {
    return fileSet;
  }

  @Override
  public boolean hasFiles() {
    return true;
  }

  @JsonProperty("limit")
  public int getLimit() { return limit; }

  @JsonIgnore
  public boolean isMatchAllMetadata() {
    return matchAllMetadata;
  }

  /**
   * Return column value count for the specified column.
   * If does not contain such column, return 0.
   * Is used when applying convert to direct scan rule.
   *
   * @param column column schema path
   * @return column value count
   */
  @Override
  public long getColumnValueCount(SchemaPath column) {
    ColumnStatistics<?> columnStats = getTableMetadata().getColumnStatistics(column);
    ColumnStatistics<?> nonInterestingColStats = columnStats == null
        ? getNonInterestingColumnsMetadata().getColumnStatistics(column) : null;

    long tableRowCount;
    if (columnStats != null) {
      tableRowCount = TableStatisticsKind.ROW_COUNT.getValue(getTableMetadata());
    } else if (nonInterestingColStats != null) {
      tableRowCount = TableStatisticsKind.ROW_COUNT.getValue(getNonInterestingColumnsMetadata());
      columnStats = nonInterestingColStats;
    } else if (hasNestedStatsForColumn(column, getTableMetadata())
        || hasNestedStatsForColumn(column, getNonInterestingColumnsMetadata())) {
      // When statistics for nested field exists, this is complex column which is present in table.
      // But its nested fields statistics can't be used to extract tableRowCount for this column.
      // So NO_COLUMN_STATS returned here to avoid problems described in DRILL-7491.
      return Statistic.NO_COLUMN_STATS;
    } else {
      return 0; // returns 0 if the column doesn't exist in the table.
    }

    Long nulls = ColumnStatisticsKind.NULLS_COUNT.getFrom(columnStats);
    if (nulls == null || Statistic.NO_COLUMN_STATS == nulls || Statistic.NO_COLUMN_STATS == tableRowCount) {
      return Statistic.NO_COLUMN_STATS;
    } else {
      return tableRowCount - nulls;
    }
  }

  /**
   * For complex columns, stats may be present only for nested fields. For example, a column path is `a`,
   * but stats present for `a`.`b`. So before making a decision that column is absent, the case needs
   * to be tested.
   *
   * @param column   parent column path
   * @param metadata metadata with column statistics
   * @return whether stats exists for nested fields
   */
  private boolean hasNestedStatsForColumn(SchemaPath column, Metadata metadata) {
    return metadata.getColumnsStatistics().keySet().stream()
        .anyMatch(path -> path.contains(column));
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public ScanStats getScanStats() {
    int columnCount = columns == null ? 20 : columns.size();
    double rowCount = TableStatisticsKind.ROW_COUNT.getValue(getTableMetadata());

    ScanStats scanStats = new ScanStats(ScanStats.GroupScanProperty.EXACT_ROW_COUNT, rowCount, 1, rowCount * columnCount);
    logger.trace("Drill parquet scan statistics: {}", scanStats);
    return scanStats;
  }

  // filter push down methods block start
  @JsonProperty("filter")
  @Override
  public LogicalExpression getFilter() {
    return filter;
  }

  @Override
  public P getMetadataProvider() {
    return metadataProvider;
  }

  public void setFilter(LogicalExpression filter) {
    this.filter = filter;
  }

  /**
   *  Set the filter - thus enabling runtime rowgroup pruning
   *  The runtime pruning can be disabled with an option.
   * @param filterExpr The filter to be used at runtime to match with rowgroups' footers
   * @param optimizerContext The context for the options
   */
  public void setFilterForRuntime(LogicalExpression filterExpr, OptimizerRulesContext optimizerContext) {
    OptionManager options = optimizerContext.getPlannerSettings().getOptions();
    boolean skipRuntimePruning = options.getBoolean(SKIP_RUNTIME_ROWGROUP_PRUNING_KEY); // if option is set to disable runtime pruning
    if ( ! skipRuntimePruning ) { setFilter(filterExpr); }
  }

  /**
   * Applies specified filter {@code filterExpr} to current group scan and produces filtering at:
   * <ul>
   * <li>table level:
   * <ul><li>if filter matches all the the data or prunes all the data, sets corresponding value to
   * {@link AbstractGroupScanWithMetadata#isMatchAllMetadata()} and returns null</li></ul></li>
   * <li>segment level:
   * <ul><li>if filter matches all the the data or prunes all the data, sets corresponding value to
   * {@link AbstractGroupScanWithMetadata#isMatchAllMetadata()} and returns null</li>
   * <li>if segment metadata was pruned, prunes underlying metadata</li></ul></li>
   * <li>partition level:
   * <ul><li>if filter matches all the the data or prunes all the data, sets corresponding value to
   * {@link AbstractGroupScanWithMetadata#isMatchAllMetadata()} and returns null</li>
   * <li>if partition metadata was pruned, prunes underlying metadata</li></ul></li>
   * <li>file level:
   * <ul><li>if filter matches all the the data or prunes all the data, sets corresponding value to
   * {@link AbstractGroupScanWithMetadata#isMatchAllMetadata()} and returns null</li></ul></li>
   * </ul>
   *
   * @param filterExpr                     filter expression to build
   * @param udfUtilities                   udf utilities
   * @param functionImplementationRegistry context to find drill function holder
   * @param optionManager                  option manager
   * @return group scan with applied filter expression
   */
  @Override
  public AbstractGroupScanWithMetadata<?> applyFilter(LogicalExpression filterExpr, UdfUtilities udfUtilities,
      FunctionImplementationRegistry functionImplementationRegistry, OptionManager optionManager) {

    // Builds filter for pruning. If filter cannot be built, null should be returned.
    FilterPredicate<?> filterPredicate =
            getFilterPredicate(filterExpr, udfUtilities, functionImplementationRegistry, optionManager, true);
    if (filterPredicate == null) {
      logger.debug("FilterPredicate cannot be built.");
      return null;
    }

    GroupScanWithMetadataFilterer<?> filteredMetadata = getFilterer()
        .filterExpression(filterExpr)
        .schema(tableMetadata.getSchema())
        .context(functionImplementationRegistry)
        .udfUtilities(udfUtilities)
        .getFiltered(optionManager, filterPredicate);

    if (isGroupScanFullyMatchesFilter(filteredMetadata)) {
      logger.debug("applyFilter() does not have any pruning since GroupScan fully matches filter");
      matchAllMetadata = filteredMetadata.isMatchAllMetadata();
      return null;
    }

    if (isAllDataPruned(filteredMetadata)) {
      if (getFilesMetadata().size() == 1) {
        // For the case when group scan has single file and it was filtered,
        // no need to create new group scan with the same file.
        return null;
      }
      logger.debug("All files have been filtered out. Add back one to get schema from scanner");
      Map<Path, FileMetadata> filesMap = getNextOrEmpty(getFilesMetadata().values()).stream()
          .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));

      Map<Path, SegmentMetadata> segmentsMap = getNextOrEmpty(getSegmentsMetadata().values()).stream()
          .collect(Collectors.toMap(SegmentMetadata::getPath, Function.identity()));

      filteredMetadata.table(getTableMetadata())
          .segments(segmentsMap)
          .partitions(getNextOrEmpty(getPartitionsMetadata()))
          .files(filesMap)
          .nonInterestingColumns(getNonInterestingColumnsMetadata())
          .matching(false);
    }

    return filteredMetadata.build();
  }

  protected boolean isAllDataPruned(GroupScanWithMetadataFilterer<?> filteredMetadata) {
    return !filteredMetadata.isMatchAllMetadata()
        // filter returns empty result using table metadata
        && (filteredMetadata.getTableMetadata() == null && getTableMetadata() != null)
            // all partitions are pruned if segment metadata is available
            || filteredMetadata.getSegments().isEmpty() && !getSegmentsMetadata().isEmpty()
            // all segments are pruned if partition metadata is available
            || filteredMetadata.getPartitions().isEmpty() && !getPartitionsMetadata().isEmpty()
            // all files are pruned if file metadata is available
            || filteredMetadata.getFiles().isEmpty() && !getFilesMetadata().isEmpty();
  }

  protected boolean isGroupScanFullyMatchesFilter(GroupScanWithMetadataFilterer<?> filteredMetadata) {
    if (MapUtils.isNotEmpty(getFilesMetadata())) {
      return getFilesMetadata().size() == filteredMetadata.getFiles().size();
    } else if (CollectionUtils.isNotEmpty(getPartitionsMetadata())) {
      return getPartitionsMetadata().size() == filteredMetadata.getPartitions().size();
    } else if (MapUtils.isNotEmpty(getSegmentsMetadata())) {
      return getSegmentsMetadata().size() == filteredMetadata.getSegments().size();
    } else {
      return getTableMetadata() != null;
    }
  }

  /**
   * Returns list with the first element of input list or empty list if input one was empty.
   *
   * @param inputList the source of the first element
   * @param <T>       type of values in the list
   * @return list with the first element of input list
   */
  protected <T> List<T> getNextOrEmpty(Collection<T> inputList) {
    return CollectionUtils.isNotEmpty(inputList) ? Collections.singletonList(inputList.iterator().next()) : Collections.emptyList();
  }

  /**
   * Returns holder for metadata values which provides API to filter metadata
   * and build new group scan instance using filtered metadata.
   */
  protected abstract GroupScanWithMetadataFilterer<?> getFilterer();

  public FilterPredicate<?> getFilterPredicate(LogicalExpression filterExpr,
      UdfUtilities udfUtilities,
      FunctionLookupContext functionImplementationRegistry,
      OptionManager optionManager,
      boolean omitUnsupportedExprs) {
    return getFilterPredicate(filterExpr, udfUtilities, functionImplementationRegistry, optionManager,
            omitUnsupportedExprs, supportsFileImplicitColumns(), getTableMetadata().getSchema());
  }

  /**
   * Returns parquet filter predicate built from specified {@code filterExpr}.
   *
   * @param filterExpr                     filter expression to build
   * @param udfUtilities                   udf utilities
   * @param functionImplementationRegistry context to find drill function holder
   * @param optionManager                  option manager
   * @param omitUnsupportedExprs           whether expressions which cannot be converted
   *                                       may be omitted from the resulting expression
   * @param supportsFileImplicitColumns    whether implicit columns are supported
   * @param schema                         schema
   * @return parquet filter predicate
   */
  public static FilterPredicate<?> getFilterPredicate(LogicalExpression filterExpr,
      UdfUtilities udfUtilities,
      FunctionLookupContext functionImplementationRegistry,
      OptionManager optionManager,
      boolean omitUnsupportedExprs,
      boolean supportsFileImplicitColumns,
      TupleMetadata schema) {
    TupleMetadata types = schema.copy();

    Set<SchemaPath> schemaPathsInExpr = filterExpr.accept(FilterEvaluatorUtils.FieldReferenceFinder.INSTANCE, null);

    // adds implicit or partition columns if they weren't added before.
    if (supportsFileImplicitColumns) {
      for (SchemaPath schemaPath : schemaPathsInExpr) {
        if (isImplicitOrPartCol(schemaPath, optionManager) && SchemaPathUtils.getColumnMetadata(schemaPath, types) == null) {
          types.add(MaterializedField.create(schemaPath.getRootSegmentPath(), Types.required(TypeProtos.MinorType.VARCHAR)));
        }
      }
    }

    ErrorCollector errorCollector = new ErrorCollectorImpl();
    LogicalExpression materializedFilter = ExpressionTreeMaterializer.materializeFilterExpr(
        filterExpr, types, errorCollector, functionImplementationRegistry);

    if (errorCollector.hasErrors()) {
      logger.error("{} error(s) encountered when materialize filter expression : {}",
        errorCollector.getErrorCount(), errorCollector.toErrorString());
      return null;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("materializedFilter : {}", ExpressionStringBuilder.toString(materializedFilter));
    }

    Set<LogicalExpression> constantBoundaries = ConstantExpressionIdentifier.getConstantExpressionSet(materializedFilter);
    return FilterBuilder.buildFilterPredicate(materializedFilter, constantBoundaries, udfUtilities, omitUnsupportedExprs);
  }

  @JsonProperty
  public TupleMetadata getSchema() {
    // creates a copy of TupleMetadata from tableMetadata
    TupleMetadata tuple = new TupleSchema();
    for (ColumnMetadata md : getTableMetadata().getSchema()) {
      tuple.addColumn(md.copy());
    }
    return tuple;
  }

  // limit push down methods start
  @Override
  public boolean supportsLimitPushdown() {
    return true;
  }

  @Override
  public GroupScan applyLimit(int maxRecords) {
    // LIMIT 0 is supported by EVF to return schema only.
    maxRecords = Math.max(maxRecords, 0);
    GroupScanWithMetadataFilterer<?> prunedMetadata = getFilterer();

    if (getTableMetadata() != null) {
      long tableRowCount = TableStatisticsKind.ROW_COUNT.getValue(getTableMetadata());
      if (tableRowCount == Statistic.NO_COLUMN_STATS || tableRowCount <= maxRecords) {
        logger.debug("limit push down does not apply, since total number of rows [{}] is less or equal to the required [{}].",
            tableRowCount, maxRecords);
        // Return the group scan with the limit pushed down
        if (this.limit != maxRecords) {
          prunedMetadata.limit(maxRecords);
          return prunedMetadata.build();
        }
        return null;
      }
    }
    // Calculate number of files to read based on maxRecords and update
    // number of records to read for each of those files.
    List<FileMetadata> qualifiedFiles = limitMetadata(getFilesMetadata().values(), maxRecords);

    // some files does not have set row count, do not do files pruning
    if (qualifiedFiles == null || qualifiedFiles.size() == getFilesMetadata().size()) {
      logger.debug("limit push down does not apply, since number of files was not reduced.");

      // Return the group scan with the limit pushed down
      if (this.limit != maxRecords) {
        prunedMetadata.limit(maxRecords);
        return prunedMetadata.build();
      }
      return null;
    }

    Map<Path, FileMetadata> filesMap = qualifiedFiles.stream()
        .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));

    return prunedMetadata
        .table(getTableMetadata())
        .segments(getSegmentsMetadata())
        .partitions(getPartitionsMetadata())
        .files(filesMap)
        .limit(maxRecords)
        .nonInterestingColumns(getNonInterestingColumnsMetadata())
        .matching(matchAllMetadata)
        .build();
  }

  /**
   * Removes metadata which does not belong to any of partitions in metadata list.
   *
   * @param metadataToPrune           list of metadata which should be pruned
   * @param filteredPartitionMetadata list of partition metadata which was pruned
   * @param <T>                       type of metadata to filter
   * @return list with metadata which belongs to pruned partitions
   */
  protected static <T extends BaseMetadata & LocationProvider> Map<Path, T> pruneForPartitions(Map<Path, T> metadataToPrune, List<PartitionMetadata> filteredPartitionMetadata) {
    Map<Path, T> prunedFiles = new LinkedHashMap<>();
    if (metadataToPrune != null) {
      metadataToPrune.forEach((path, metadata) -> {
        for (PartitionMetadata filteredPartition : filteredPartitionMetadata) {
          if (filteredPartition.getLocations().contains(path)) {
            prunedFiles.put(path, metadata);
            break;
          }
        }
      });
    }

    return prunedFiles;
  }

  /**
   * Prunes specified metadata list and leaves minimum metadata instances count with general rows number
   * which is not less than specified {@code maxRecords}.
   *
   * @param metadataList list of metadata to prune
   * @param maxRecords   rows number to leave
   * @param <T>          type of metadata to prune
   * @return pruned metadata list
   */
  protected <T extends BaseMetadata> List<T> limitMetadata(Collection<T> metadataList, int maxRecords) {
    List<T> qualifiedMetadata = new ArrayList<>();
    long currentRowCount = 0;
    for (T metadata : metadataList) {
      long rowCount = TableStatisticsKind.ROW_COUNT.getValue(metadata);
      if (rowCount == Statistic.NO_COLUMN_STATS) {
        return null;
      } else if (currentRowCount + rowCount <= maxRecords) {
        currentRowCount += rowCount;
        qualifiedMetadata.add(metadata);
        continue;
      } else if (currentRowCount < maxRecords) {
        qualifiedMetadata.add(metadata);
      }
      break;
    }
    return qualifiedMetadata;
  }
  // limit push down methods end

  // partition pruning methods start
  @Override
  public List<SchemaPath> getPartitionColumns() {
    if (partitionColumns == null) {
      partitionColumns = metadataProvider.getPartitionColumns();
      if (partitionColumns == null) {
        partitionColumns = new ArrayList<>();
      }
    }
    return partitionColumns;
  }

  @JsonIgnore
  public TypeProtos.MajorType getTypeForColumn(SchemaPath schemaPath) {
    ColumnMetadata columnMetadata = SchemaPathUtils.getColumnMetadata(schemaPath, getTableMetadata().getSchema());
    return columnMetadata != null ? columnMetadata.majorType() : null;
  }

  @JsonIgnore
  public <T> T getPartitionValue(Path path, SchemaPath column, Class<T> clazz) {
    return getPartitionsMetadata().stream()
        .filter(partition -> partition.getColumn().equals(column) && partition.getLocations().contains(path))
        .findAny()
        .map(metadata -> clazz.cast(metadata.getColumnsStatistics().get(column).get(ColumnStatisticsKind.MAX_VALUE)))
        .orElse(null);
  }

  @JsonIgnore
  public Set<Path> getFileSet() {
    return fileSet;
  }
  // partition pruning methods end

  // helper method used for partition pruning and filter push down
  @Override
  public void modifyFileSelection(FileSelection selection) {
    fileSet = new HashSet<>(selection.getFiles());
  }

  // protected methods block
  protected void init() throws IOException {
    if (fileSet == null && getFilesMetadata() != null) {
      fileSet = getFilesMetadata().keySet();
    }
  }

  protected String getFilterString() {
    return filter == null || filter.equals(ValueExpressions.BooleanExpression.TRUE) ?
      "" : ExpressionStringBuilder.toString(this.filter);
  }

  protected abstract boolean supportsFileImplicitColumns();
  protected abstract List<String> getPartitionValues(LocationProvider locationProvider);

  public static boolean isImplicitOrPartCol(SchemaPath schemaPath, OptionManager optionManager) {
    Set<String> implicitColNames = ColumnExplorer.initImplicitFileColumns(optionManager).keySet();
    return ColumnExplorer.isPartitionColumn(optionManager, schemaPath) || implicitColNames.contains(schemaPath.getRootSegmentPath());
  }

  @JsonIgnore
  public Map<Path, FileMetadata> getFilesMetadata() {
    if (files == null) {
      files = metadataProvider.getFilesMetadataMap();
    }
    return files;
  }

  @Override
  public TableMetadata getTableMetadata() {
    if (tableMetadata == null) {
      tableMetadata = metadataProvider.getTableMetadata();
    }
    return tableMetadata;
  }

  @JsonIgnore
  public List<PartitionMetadata> getPartitionsMetadata() {
    if (partitions == null) {
      partitions = metadataProvider.getPartitionsMetadata();
    }
    return partitions;
  }

  @JsonIgnore
  public Map<Path, SegmentMetadata> getSegmentsMetadata() {
    if (segments == null) {
      segments = metadataProvider.getSegmentsMetadataMap();
    }
    return segments;
  }

  @Override
  public boolean usedMetastore() {
    return usedMetastore;
  }

  @JsonIgnore
  public NonInterestingColumnsMetadata getNonInterestingColumnsMetadata() {
    if (nonInterestingColumnsMetadata == null) {
      nonInterestingColumnsMetadata = metadataProvider.getNonInterestingColumnsMetadata();
    }
    return nonInterestingColumnsMetadata;
  }

  /**
   * Returns {@link TableMetadataProviderBuilder} instance based on specified
   * {@link MetadataProviderManager} source.
   *
   * @param source metadata provider manager
   * @return {@link TableMetadataProviderBuilder} instance
   */
  protected abstract TableMetadataProviderBuilder tableMetadataProviderBuilder(MetadataProviderManager source);

  /**
   * Returns {@link TableMetadataProviderBuilder} instance which may provide metadata
   * without using Drill Metastore.
   *
   * @param source metadata provider manager
   * @return {@link TableMetadataProviderBuilder} instance
   */
  protected abstract TableMetadataProviderBuilder defaultTableMetadataProviderBuilder(MetadataProviderManager source);

  /**
   * Compares the last modified time of files obtained from specified selection with
   * the Metastore last modified time to determine whether Metastore metadata
   * is up-to-date. If metadata is outdated, {@link MetadataException} will be thrown.
   *
   * @param selection the source of files to check
   * @throws MetadataException if metadata is outdated
   */
  protected void checkMetadataConsistency(FileSelection selection, Configuration fsConf) throws IOException {
    if (metadataProvider.checkMetadataVersion()) {
      DrillFileSystem fileSystem =
          ImpersonationUtil.createFileSystem(ImpersonationUtil.resolveUserName(getUserName()), fsConf);

      List<FileStatus> fileStatuses = FileMetadataInfoCollector.getFileStatuses(selection, fileSystem);

      long lastModifiedTime = metadataProvider.getTableMetadata().getLastModifiedTime();

      Set<Path> removedFiles = new HashSet<>(metadataProvider.getFilesMetadataMap().keySet());
      Set<Path> newFiles = new HashSet<>();

      boolean isChanged = false;

      for (FileStatus fileStatus : fileStatuses) {
        if (!removedFiles.remove(Path.getPathWithoutSchemeAndAuthority(fileStatus.getPath()))) {
          newFiles.add(fileStatus.getPath());
        }
        if (fileStatus.getModificationTime() > lastModifiedTime) {
          isChanged = true;
          break;
        }
      }

      if (isChanged || !removedFiles.isEmpty() || !newFiles.isEmpty()) {
        throw MetadataException.of(MetadataException.MetadataExceptionType.OUTDATED_METADATA);
      }
    }
  }

  /**
   * This class is responsible for filtering different metadata levels.
   */
  protected abstract static class GroupScanWithMetadataFilterer<B extends GroupScanWithMetadataFilterer<B>> {
    protected final AbstractGroupScanWithMetadata<? extends TableMetadataProvider> source;

    protected boolean matchAllMetadata;

    protected TableMetadata tableMetadata;
    protected List<PartitionMetadata> partitions = Collections.emptyList();
    protected Map<Path, SegmentMetadata> segments = Collections.emptyMap();
    protected Map<Path, FileMetadata> files = Collections.emptyMap();
    protected NonInterestingColumnsMetadata nonInterestingColumnsMetadata;
    // required for rebuilding filter expression for the case of schema change
    protected LogicalExpression filterExpression;
    protected TupleMetadata tableSchema;
    protected UdfUtilities udfUtilities;
    protected FunctionLookupContext context;
    protected int limit = -1;

    // for the case when filtering is possible for partitions, but files count exceeds
    // PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD, new group scan with at least filtered partitions
    // and files which belongs to that partitions may be returned
    protected MetadataType overflowLevel = MetadataType.NONE;

    public GroupScanWithMetadataFilterer(AbstractGroupScanWithMetadata<?> source) {
      this.source = source;
    }

    /**
     * Constructs required implementation of {@link AbstractGroupScanWithMetadata} with filtered metadata.
     *
     * @return implementation of {@link AbstractGroupScanWithMetadata} with filtered metadata
     */
    public abstract AbstractGroupScanWithMetadata<?> build();

    public B table(TableMetadata tableMetadata) {
      this.tableMetadata = tableMetadata;
      return self();
    }

    public B partitions(List<PartitionMetadata> partitions) {
      this.partitions = partitions;
      return self();
    }

    public B segments(Map<Path, SegmentMetadata> segments) {
      this.segments = segments;
      return self();
    }

    public B nonInterestingColumns(NonInterestingColumnsMetadata nonInterestingColumns) {
      this.nonInterestingColumnsMetadata = nonInterestingColumns;
      return self();
    }

    public B files(Map<Path, FileMetadata> files) {
      this.files = files;
      return self();
    }

    public B limit(int maxRecords) {
      this.limit = maxRecords;
      return self();
    }

    public B matching(boolean matchAllMetadata) {
      this.matchAllMetadata = matchAllMetadata;
      return self();
    }

    public B overflow(MetadataType overflowLevel) {
      this.overflowLevel = overflowLevel;
      return self();
    }

    public B filterExpression(LogicalExpression filterExpression) {
      this.filterExpression = filterExpression;
      return self();
    }

    public B schema(TupleMetadata tableSchema) {
      this.tableSchema = tableSchema;
      return self();
    }

    public B udfUtilities(UdfUtilities udfUtilities) {
      this.udfUtilities = udfUtilities;
      return self();
    }

    public B context(FunctionLookupContext context) {
      this.context = context;
      return self();
    }

    public boolean isMatchAllMetadata() {
      return matchAllMetadata;
    }

    public TableMetadata getTableMetadata() {
      return tableMetadata;
    }

    public List<PartitionMetadata> getPartitions() {
      return partitions;
    }

    public Map<Path, SegmentMetadata> getSegments() {
      return segments;
    }

    public Map<Path, FileMetadata> getFiles() {
      return files;
    }

    public MetadataType getOverflowLevel() {
      return overflowLevel;
    }

    /**
     * Produces filtering of metadata and returns {@link GroupScanWithMetadataFilterer}
     * to construct resulting group scan.
     *
     * @param optionManager     option manager
     * @param filterPredicate   filter expression
     * @return this instance with filtered metadata
     */
    protected B getFiltered(OptionManager optionManager,
        FilterPredicate<?> filterPredicate) {
      Objects.requireNonNull(filterExpression, "filterExpression was not set");
      Objects.requireNonNull(tableSchema, "tableSchema was not set");
      Objects.requireNonNull(udfUtilities, "udfUtilities were not set");
      Objects.requireNonNull(context, "context was not set");

      Set<SchemaPath> schemaPathsInExpr =
          filterExpression.accept(FilterEvaluatorUtils.FieldReferenceFinder.INSTANCE, null);

      if (source.getTableMetadata() != null) {
        filterTableMetadata(filterPredicate, schemaPathsInExpr);
      }

      if (source.getSegmentsMetadata() != null) {
        filterSegmentMetadata(optionManager, filterPredicate, schemaPathsInExpr);
      }

      if (source.getPartitionsMetadata() != null) {
        filterPartitionMetadata(optionManager, filterPredicate, schemaPathsInExpr);
      }

      if (source.getFilesMetadata() != null) {
        filterFileMetadata(optionManager, filterPredicate, schemaPathsInExpr);
      }
      return self();
    }

    /**
     * Produces filtering of metadata at table level.
     *
     * @param filterPredicate   filter expression
     * @param schemaPathsInExpr columns used in filter expression
     */
    protected void filterTableMetadata(FilterPredicate<?> filterPredicate, Set<SchemaPath> schemaPathsInExpr) {
      // Filters table metadata. If resulting list is empty, should be used single minimum entity of metadata.
      // If table matches fully, nothing is pruned and pruning of underlying metadata is stopped.
      matchAllMetadata = true;
      List<TableMetadata> filteredTableMetadata = filterAndGetMetadata(schemaPathsInExpr,
          Collections.singletonList(source.getTableMetadata()), filterPredicate, null);
      if (CollectionUtils.isNotEmpty(filteredTableMetadata)) {
        this.tableMetadata = filteredTableMetadata.get(0);
      }
    }

    /**
     * Produces filtering of metadata at segment level.
     *
     * @param optionManager     option manager
     * @param filterPredicate   filter expression
     * @param schemaPathsInExpr columns used in filter expression
     */
    protected void filterSegmentMetadata(OptionManager optionManager,
                                         FilterPredicate<?> filterPredicate,
                                         Set<SchemaPath> schemaPathsInExpr) {
      if (!matchAllMetadata) {
        if (!source.getSegmentsMetadata().isEmpty()) {
          if (source.getSegmentsMetadata().size() <= optionManager.getOption(
              PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD)) {
            matchAllMetadata = true;
            segments = filterAndGetMetadata(schemaPathsInExpr,
                source.getSegmentsMetadata().values(),
                filterPredicate,
                optionManager).stream()
                    .collect(Collectors.toMap(
                        SegmentMetadata::getPath,
                        Function.identity(),
                        (first, second) -> second));
          } else {
            overflowLevel = MetadataType.SEGMENT;
          }
        }
      } else {
        segments = source.getSegmentsMetadata();
      }
    }

    /**
     * Produces filtering of metadata at partition level.
     *
     * @param optionManager     option manager
     * @param filterPredicate   filter expression
     * @param schemaPathsInExpr columns used in filter expression
     */
    protected void filterPartitionMetadata(OptionManager optionManager,
                                           FilterPredicate<?> filterPredicate,
                                           Set<SchemaPath> schemaPathsInExpr) {
      List<PartitionMetadata> prunedPartitions;
      if (!source.getSegmentsMetadata().isEmpty()
          && source.getSegmentsMetadata().size() > getSegments().size()) {
        // prunes row groups to leave only row groups which are contained by pruned segments
        prunedPartitions = pruneForSegments(source.getPartitionsMetadata(), getSegments());
      } else {
        prunedPartitions = source.getPartitionsMetadata();
      }

      if (isMatchAllMetadata()) {
        partitions = prunedPartitions;
        return;
      }

      if (!source.getPartitionsMetadata().isEmpty()) {
        if (source.getPartitionsMetadata().size() <= optionManager.getOption(
          PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD)) {
          matchAllMetadata = true;
          partitions = filterAndGetMetadata(schemaPathsInExpr, prunedPartitions, filterPredicate, optionManager);
        } else {
          overflowLevel = MetadataType.PARTITION;
        }
      }
    }

    /**
     * Produces filtering of metadata at file level.
     *
     * @param optionManager     option manager
     * @param filterPredicate   filter expression
     * @param schemaPathsInExpr columns used in filter expression
     */
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

      // Stop files pruning for the case:
      //    -  # of files is beyond PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD.
      if (prunedFiles.size() <= optionManager.getOption(
          PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD)) {

        matchAllMetadata = true;
        files = filterAndGetMetadata(schemaPathsInExpr, prunedFiles.values(), filterPredicate, optionManager).stream()
            .collect(Collectors.toMap(FileMetadata::getPath, Function.identity()));

      } else {
        matchAllMetadata = false;
        files = prunedFiles;
        overflowLevel = MetadataType.FILE;
      }
    }

    /**
     * Removes metadata which does not belong to any of partitions in metadata list.
     *
     * @param metadataToPrune         list of metadata which should be pruned
     * @param filteredSegmentMetadata list of segment metadata which was pruned
     * @param <T>                     type of metadata to filter
     * @return map with metadata which belongs to pruned partitions
     */
    protected static <T extends BaseMetadata & LocationProvider> Map<Path, T> pruneForSegments(
        Map<Path, T> metadataToPrune, Map<Path, SegmentMetadata> filteredSegmentMetadata) {
      Map<Path, T> prunedFiles = new HashMap<>();
      if (metadataToPrune != null) {
        for (Map.Entry<Path, T> entry : metadataToPrune.entrySet()) {
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

    /**
     * Removes metadata which does not belong to any of partitions in metadata list.
     *
     * @param metadataToPrune         list of partition metadata which should be pruned
     * @param filteredSegmentMetadata list of segment metadata which was pruned
     * @return list with metadata which belongs to pruned partitions
     */
    protected List<PartitionMetadata> pruneForSegments(
        List<PartitionMetadata> metadataToPrune, Map<Path, SegmentMetadata> filteredSegmentMetadata) {
      List<PartitionMetadata> prunedPartitions = new ArrayList<>();
      if (metadataToPrune != null) {
        for (PartitionMetadata partition : metadataToPrune) {
          for (SegmentMetadata segment : filteredSegmentMetadata.values()) {
            if (!Collections.disjoint(segment.getLocations(), partition.getLocations())) {
              prunedPartitions.add(partition);
              break;
            }
          }
        }
      }

      return prunedPartitions;
    }

    /**
     * Produces filtering of specified metadata using specified filter expression and returns filtered metadata.
     *
     * @param schemaPathsInExpr columns used in filter expression
     * @param metadataList      metadata to filter
     * @param filterPredicate   filter expression
     * @param optionManager     option manager
     * @param <T>               type of metadata to filter
     * @return filtered metadata
     */
    public <T extends Metadata> List<T> filterAndGetMetadata(
        Set<SchemaPath> schemaPathsInExpr,
        Iterable<T> metadataList,
        FilterPredicate<?> filterPredicate,
        OptionManager optionManager) {
      List<T> qualifiedMetadata = new ArrayList<>();

      for (T metadata : metadataList) {
        TupleMetadata schema = metadata.getSchema();
        if (schema != null && !tableSchema.isEquivalent(schema)) {
          filterPredicate = getFilterPredicate(filterExpression, udfUtilities,
              context, optionManager, true, true, schema);
        }
        Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = metadata.getColumnsStatistics();

        // adds partition (dir) column statistics if it may be used during filter evaluation
        if (metadata instanceof LocationProvider && optionManager != null) {
          LocationProvider locationProvider = (LocationProvider) metadata;
          columnsStatistics = ParquetTableMetadataUtils.addImplicitColumnsStatistics(columnsStatistics,
              source.columns, source.getPartitionValues(locationProvider), optionManager,
              locationProvider.getPath(), source.supportsFileImplicitColumns());
        }

        if (source.getNonInterestingColumnsMetadata() != null) {
          columnsStatistics.putAll(source.getNonInterestingColumnsMetadata().getColumnsStatistics());
        }
        RowsMatch match = FilterEvaluatorUtils.matches(filterPredicate,
            columnsStatistics, TableStatisticsKind.ROW_COUNT.getValue(metadata),
            schema, schemaPathsInExpr);
        if (match == RowsMatch.NONE) {
          continue; // No file comply to the filter => drop the file
        }
        if (matchAllMetadata) {
          matchAllMetadata = match == RowsMatch.ALL;
        }
        qualifiedMetadata.add(metadata);
      }
      if (qualifiedMetadata.isEmpty()) {
        matchAllMetadata = false;
      }
      return qualifiedMetadata;
    }

    protected abstract B self();
  }

}
