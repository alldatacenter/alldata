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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.AbstractWriter;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.ScanStats.GroupScanProperty;
import org.apache.drill.exec.physical.impl.ScanBatch;
import org.apache.drill.exec.physical.impl.StatisticsWriterRecordBatch;
import org.apache.drill.exec.physical.impl.WriterRecordBatch;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileReaderFactory;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileScanBuilder;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.planner.common.DrillStatsTable.TableStatistics;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.RecordReader;
import org.apache.drill.exec.store.RecordWriter;
import org.apache.drill.exec.store.StatisticsRecordWriter;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.dfs.BasicFormatMatcher;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatMatcher;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.drill.exec.store.schedule.CompleteFileWork;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.shaded.guava.com.google.common.base.Functions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for file readers.
 * <p>
 * Provides a bridge between the legacy {@link RecordReader}-style
 * readers and the newer {@link ManagedReader} style. Over time, split the
 * class, or provide a cleaner way to handle the differences.
 *
 * @param <T> the format plugin config for this reader
 */
public abstract class EasyFormatPlugin<T extends FormatPluginConfig> implements FormatPlugin {
  private static final Logger logger = LoggerFactory.getLogger(EasyFormatPlugin.class);

  /**
   * Defines the static, programmer-defined options for this plugin. These
   * options are attributes of how the plugin works. The plugin config,
   * defined in the class definition, provides user-defined options that can
   * vary across uses of the plugin.
   */
  public static class EasyFormatConfig {
    private BasicFormatMatcher matcher;
    private final boolean readable;
    private final boolean writable;
    private final boolean blockSplittable;
    private final boolean compressible;
    private final Configuration fsConf;
    private final List<String> extensions;
    private final String defaultName;

    // Config options that, prior to Drill 1.15, required the plugin to
    // override methods. Moving forward, plugins should be migrated to
    // use this simpler form. New plugins should use these options
    // instead of overriding methods.

    private final boolean supportsLimitPushdown;
    private final boolean supportsProjectPushdown;
    private final boolean supportsFileImplicitColumns;
    private final boolean supportsAutoPartitioning;
    private final boolean supportsStatistics;
    private final String readerOperatorType;
    private final String writerOperatorType;

    /**
     *  Choose whether to use the "traditional" or "enhanced" reader
     *  structure. Can also be selected at runtime by overriding
     *  {@link #useEnhancedScan()}.
     */
    private final boolean useEnhancedScan;

    public EasyFormatConfig(EasyFormatConfigBuilder builder) {
      this.matcher = builder.matcher;
      this.readable = builder.readable;
      this.writable = builder.writable;
      this.blockSplittable = builder.blockSplittable;
      this.compressible = builder.compressible;
      this.fsConf = builder.fsConf;
      this.extensions = builder.extensions;
      this.defaultName = builder.defaultName;
      this.supportsLimitPushdown = builder.supportsLimitPushdown;
      this.supportsProjectPushdown = builder.supportsProjectPushdown;
      this.supportsFileImplicitColumns = builder.supportsFileImplicitColumns;
      this.supportsAutoPartitioning = builder.supportsAutoPartitioning;
      this.supportsStatistics = builder.supportsStatistics;
      this.readerOperatorType = builder.readerOperatorType;
      this.writerOperatorType = builder.writerOperatorType;
      this.useEnhancedScan = builder.useEnhancedScan;
    }

    public BasicFormatMatcher getMatcher() {
      return matcher;
    }

    public boolean isReadable() {
      return readable;
    }

    public boolean isWritable() {
      return writable;
    }

    public boolean isBlockSplittable() {
      return blockSplittable;
    }

    public boolean isCompressible() {
      return compressible;
    }

    public Configuration getFsConf() {
      return fsConf;
    }

    public List<String> getExtensions() {
      return extensions;
    }

    public String getDefaultName() {
      return defaultName;
    }

    public boolean supportsLimitPushdown() {
      return supportsLimitPushdown;
    }

    public boolean supportsProjectPushdown() {
      return supportsProjectPushdown;
    }

    public boolean supportsFileImplicitColumns() {
      return supportsFileImplicitColumns;
    }

    public boolean supportsAutoPartitioning() {
      return supportsAutoPartitioning;
    }

    public boolean supportsStatistics() {
      return supportsStatistics;
    }

    public String getReaderOperatorType() {
      return readerOperatorType;
    }

    public String getWriterOperatorType() {
      return writerOperatorType;
    }

    public boolean useEnhancedScan() {
      return useEnhancedScan;
    }

    public static EasyFormatConfigBuilder builder() {
      return new EasyFormatConfigBuilder();
    }

    public EasyFormatConfigBuilder toBuilder() {
      return builder()
          .matcher(matcher)
          .readable(readable)
          .writable(writable)
          .blockSplittable(blockSplittable)
          .compressible(compressible)
          .fsConf(fsConf)
          .extensions(extensions)
          .defaultName(defaultName)
          .supportsLimitPushdown(supportsLimitPushdown)
          .supportsProjectPushdown(supportsProjectPushdown)
          .supportsFileImplicitColumns(supportsFileImplicitColumns)
          .supportsAutoPartitioning(supportsAutoPartitioning)
          .supportsStatistics(supportsStatistics)
          .readerOperatorType(readerOperatorType)
          .writerOperatorType(writerOperatorType)
          .useEnhancedScan(useEnhancedScan);
    }
  }

  public static class EasyFormatConfigBuilder {
    private BasicFormatMatcher matcher;
    private boolean readable = true;
    private boolean writable;
    private boolean blockSplittable;
    private boolean compressible;
    private Configuration fsConf;
    private List<String> extensions;
    private String defaultName;
    private boolean supportsLimitPushdown;
    private boolean supportsProjectPushdown;
    private boolean supportsFileImplicitColumns = true;
    private boolean supportsAutoPartitioning;
    private boolean supportsStatistics;
    private String readerOperatorType;
    private String writerOperatorType = "";
    private boolean useEnhancedScan;

    public EasyFormatConfigBuilder matcher(BasicFormatMatcher matcher) {
      this.matcher = matcher;
      return this;
    }

    public EasyFormatConfigBuilder readable(boolean readable) {
      this.readable = readable;
      return this;
    }

    public EasyFormatConfigBuilder writable(boolean writable) {
      this.writable = writable;
      return this;
    }

    public EasyFormatConfigBuilder blockSplittable(boolean blockSplittable) {
      this.blockSplittable = blockSplittable;
      return this;
    }

    public EasyFormatConfigBuilder compressible(boolean compressible) {
      this.compressible = compressible;
      return this;
    }

    public EasyFormatConfigBuilder fsConf(Configuration fsConf) {
      this.fsConf = fsConf;
      return this;
    }

    public EasyFormatConfigBuilder extensions(List<String> extensions) {
      this.extensions = extensions;
      return this;
    }

    public EasyFormatConfigBuilder extensions(String... extensions) {
      this.extensions = Arrays.asList(extensions);
      return this;
    }

    public EasyFormatConfigBuilder defaultName(String defaultName) {
      this.defaultName = defaultName;
      return this;
    }

    public EasyFormatConfigBuilder supportsLimitPushdown(boolean supportsLimitPushdown) {
      this.supportsLimitPushdown = supportsLimitPushdown;
      return this;
    }

    public EasyFormatConfigBuilder supportsProjectPushdown(boolean supportsProjectPushdown) {
      this.supportsProjectPushdown = supportsProjectPushdown;
      return this;
    }

    public EasyFormatConfigBuilder supportsFileImplicitColumns(boolean supportsFileImplicitColumns) {
      this.supportsFileImplicitColumns = supportsFileImplicitColumns;
      return this;
    }

    public EasyFormatConfigBuilder supportsAutoPartitioning(boolean supportsAutoPartitioning) {
      this.supportsAutoPartitioning = supportsAutoPartitioning;
      return this;
    }

    public EasyFormatConfigBuilder supportsStatistics(boolean supportsStatistics) {
      this.supportsStatistics = supportsStatistics;
      return this;
    }

    public EasyFormatConfigBuilder readerOperatorType(String readerOperatorType) {
      this.readerOperatorType = readerOperatorType;
      return this;
    }

    public EasyFormatConfigBuilder writerOperatorType(String writerOperatorType) {
      this.writerOperatorType = writerOperatorType;
      return this;
    }

    public EasyFormatConfigBuilder useEnhancedScan(boolean useEnhancedScan) {
      this.useEnhancedScan = useEnhancedScan;
      return this;
    }

    public EasyFormatConfig build() {
      Objects.requireNonNull(defaultName, "defaultName is not set");
      readerOperatorType = readerOperatorType == null
          ? defaultName.toUpperCase(Locale.ROOT) + "_SUB_SCAN"
          : readerOperatorType;
      return new EasyFormatConfig(this);
    }
  }

  /**
   * Builds the readers row-set based scan operator.
   */
  private static class EasyReaderFactory extends FileReaderFactory {

    private final EasyFormatPlugin<? extends FormatPluginConfig> plugin;
    private final EasySubScan scan;
    private final FragmentContext context;

    public EasyReaderFactory(EasyFormatPlugin<? extends FormatPluginConfig> plugin,
        EasySubScan scan, FragmentContext context) {
      this.plugin = plugin;
      this.scan = scan;
      this.context = context;
    }

    @Override
    public ManagedReader<? extends FileSchemaNegotiator> newReader() {
      try {
        return plugin.newBatchReader(scan, context.getOptions());
      } catch (ExecutionSetupException e) {
        throw UserException.validationError(e)
          .addContext("Reason", "Failed to create a batch reader")
          .addContext(errorContext())
          .build(logger);
      }
    }
  }

  private final String name;
  private final EasyFormatConfig easyConfig;
  private final DrillbitContext context;
  private final StoragePluginConfig storageConfig;
  protected final T formatConfig;

  /**
   * Legacy constructor.
   */
  protected EasyFormatPlugin(String name, DrillbitContext context, Configuration fsConf,
      StoragePluginConfig storageConfig, T formatConfig, boolean readable, boolean writable,
      boolean blockSplittable, boolean compressible, List<String> extensions, String defaultName) {
    this.name = name == null ? defaultName : name;
    easyConfig = EasyFormatConfig.builder()
        .matcher(new BasicFormatMatcher(this, fsConf, extensions, compressible))
        .readable(readable)
        .writable(writable)
        .blockSplittable(blockSplittable)
        .compressible(compressible)
        .fsConf(fsConf)
        .defaultName(defaultName)
        .build();
    this.context = context;
    this.storageConfig = storageConfig;
    this.formatConfig = formatConfig;
  }

  /**
   * Revised constructor in which settings are gathered into a configuration object.
   *
   * @param name name of the plugin
   * @param config configuration options for this plugin which determine
   * developer-defined runtime behavior
   * @param context the global server-wide Drillbit context
   * @param storageConfig the configuration for the storage plugin that owns this
   * format plugin
   * @param formatConfig the Jackson-serialized format configuration as created
   * by the user in the Drill web console. Holds user-defined options
   */

  protected EasyFormatPlugin(String name, EasyFormatConfig config, DrillbitContext context,
      StoragePluginConfig storageConfig, T formatConfig) {
    this.name = name;
    this.easyConfig = config;
    this.context = context;
    this.storageConfig = storageConfig;
    this.formatConfig = formatConfig;
    if (easyConfig.matcher == null) {
      easyConfig.matcher = new BasicFormatMatcher(this,
          easyConfig.fsConf, easyConfig.extensions,
          easyConfig.compressible);
    }
  }

  @Override
  public Configuration getFsConf() { return easyConfig.getFsConf(); }

  @Override
  public DrillbitContext getContext() { return context; }

  public EasyFormatConfig easyConfig() { return easyConfig; }

  @Override
  public String getName() { return name; }

  /**
   * Does this plugin support pushing the limit down to the batch reader?  If so, then
   * the reader itself should have logic to stop reading the file as soon as the limit has been
   * reached. It makes the most sense to do this with file formats that have consistent schemata
   * that are identified at the first row.  CSV for example.  If the user only wants 100 rows, it
   * does not make sense to read the entire file.
   */
  public boolean supportsLimitPushdown() { return easyConfig.supportsLimitPushdown(); }

  /**
   * Does this plugin support projection push down? That is, can the reader
   * itself handle the tasks of projecting table columns, creating null
   * columns for missing table columns, and so on?
   *
   * @return {@code true} if the plugin supports projection push-down,
   * {@code false} if Drill should do the task by adding a project operator
   */
  public boolean supportsPushDown() { return easyConfig.supportsProjectPushdown(); }

  /**
   * Whether this format plugin supports implicit file columns.
   *
   * @return {@code true} if the plugin supports implicit file columns,
   * {@code false} otherwise
   */
  public boolean supportsFileImplicitColumns() {
    return easyConfig.supportsFileImplicitColumns();
  }

  /**
   * Whether or not you can split the format based on blocks within file
   * boundaries. If not, the simple format engine will only split on file
   * boundaries.
   *
   * @return {@code true} if splitable.
   */
  public boolean isBlockSplittable() { return easyConfig.isBlockSplittable(); }

  /**
   * Indicates whether or not this format could also be in a compression
   * container (for example: csv.gz versus csv). If this format uses its own
   * internal compression scheme, such as Parquet does, then this should return
   * false.
   *
   * @return {@code true} if it is compressible
   */
  public boolean isCompressible() { return easyConfig.isCompressible(); }

  /**
   * Return a record reader for the specific file format, when using the original
   * {@link ScanBatch} scanner.
   * @param context fragment context
   * @param dfs Drill file system
   * @param fileWork metadata about the file to be scanned
   * @param columns list of projected columns (or may just contain the wildcard)
   * @param userName the name of the user running the query
   * @return a record reader for this format
   * @throws ExecutionSetupException for many reasons
   */
  public RecordReader getRecordReader(FragmentContext context, DrillFileSystem dfs, FileWork fileWork,
      List<SchemaPath> columns, String userName) throws ExecutionSetupException {
    throw new ExecutionSetupException("Must implement getRecordReader() if using the legacy scanner.");
  }

  protected CloseableRecordBatch getReaderBatch(FragmentContext context,
      EasySubScan scan) throws ExecutionSetupException {
    if (useEnhancedScan()) {
      return buildScan(context, scan);
    } else {
      return buildScanBatch(context, scan);
    }
  }

  /**
   * Choose whether to use the enhanced scan based on the row set and scan
   * framework, or the "traditional" ad-hoc structure based on ScanBatch.
   * Normally set as a config option. Override this method if you want to
   * make the choice based on a system/session option.
   *
   * @return true to use the enhanced scan framework, false for the
   * traditional scan-batch framework
   */
  protected boolean useEnhancedScan() {
    return easyConfig.useEnhancedScan();
  }

  /**
   * Use the original scanner based on the {@link RecordReader} interface.
   * Requires that the storage plugin roll its own solutions for null columns.
   * Is not able to limit vector or batch sizes. Retained or backward
   * compatibility with "classic" format plugins which have not yet been
   * upgraded to use the new framework.
   */
  private CloseableRecordBatch buildScanBatch(FragmentContext context,
      EasySubScan scan) throws ExecutionSetupException {
    final ColumnExplorer columnExplorer =
        new ColumnExplorer(context.getOptions(), scan.getColumns());

    if (! columnExplorer.isStarQuery()) {
      scan = new EasySubScan(scan.getUserName(), scan.getWorkUnits(), scan.getFormatPlugin(),
          columnExplorer.getTableColumns(), scan.getSelectionRoot(),
          scan.getPartitionDepth(), scan.getSchema(), scan.getMaxRecords());
      scan.setOperatorId(scan.getOperatorId());
    }

    final OperatorContext oContext = context.newOperatorContext(scan);
    final DrillFileSystem dfs;
    try {
      dfs = oContext.newFileSystem(easyConfig().fsConf);
    } catch (final IOException e) {
      throw new ExecutionSetupException(String.format("Failed to create FileSystem: %s", e.getMessage()), e);
    }

    final List<RecordReader> readers = new LinkedList<>();
    final List<Map<String, String>> implicitColumns = Lists.newArrayList();
    Map<String, String> mapWithMaxColumns = Maps.newLinkedHashMap();
    final boolean supportsFileImplicitColumns = scan.getSelectionRoot() != null;
    for (final FileWork work : scan.getWorkUnits()) {
      final RecordReader recordReader = getRecordReader(
          context, dfs, work, scan.getColumns(), scan.getUserName());
      readers.add(recordReader);
      final List<String> partitionValues = ColumnExplorer.listPartitionValues(
          work.getPath(), scan.getSelectionRoot(), false);
      final Map<String, String> implicitValues = columnExplorer.populateColumns(
          work.getPath(), partitionValues, supportsFileImplicitColumns, dfs);
      implicitColumns.add(implicitValues);
      if (implicitValues.size() > mapWithMaxColumns.size()) {
        mapWithMaxColumns = implicitValues;
      }
    }

    // all readers should have the same number of implicit columns, add missing ones with value null
    final Map<String, String> diff = Maps.transformValues(mapWithMaxColumns, Functions.constant(null));
    for (final Map<String, String> map : implicitColumns) {
      map.putAll(Maps.difference(map, diff).entriesOnlyOnRight());
    }

    return new ScanBatch(context, oContext, readers, implicitColumns);
  }

  /**
   * Revised scanner based on the revised {@link org.apache.drill.exec.physical.resultSet.ResultSetLoader}
   * and {@link org.apache.drill.exec.physical.impl.scan.RowBatchReader} classes.
   * Handles most projection tasks automatically. Able to limit
   * vector and batch sizes. Use this for new format plugins.
   */
  private CloseableRecordBatch buildScan(FragmentContext context,
      EasySubScan scan) throws ExecutionSetupException {
    try {
      final FileScanBuilder builder = frameworkBuilder(context.getOptions(), scan);

      // Add batch reader, if none specified

      if (builder.readerFactory() == null) {
        builder.setReaderFactory(new EasyReaderFactory(this, scan, context));
      }
      return builder.buildScanOperator(context, scan);
    } catch (final UserException e) {
      // Rethrow user exceptions directly
      throw e;
    } catch (final Throwable e) {
      // Wrap all others
      throw new ExecutionSetupException(e);
    }
  }

  /**
   * Initialize the scan framework builder with standard options.
   * Call this from the plugin-specific
   * {@link #frameworkBuilder(OptionManager, EasySubScan)} method.
   * The plugin can then customize/revise options as needed.
   *
   * @param builder the scan framework builder you create in the
   * {@link #frameworkBuilder(OptionManager, EasySubScan)} method
   * @param scan the physical scan operator definition passed to
   * the {@link #frameworkBuilder(OptionManager, EasySubScan)} method
   */
  protected void initScanBuilder(FileScanBuilder builder, EasySubScan scan) {
    builder.projection(scan.getColumns());
    builder.setUserName(scan.getUserName());

    // Pass along the output schema, if any
    builder.providedSchema(scan.getSchema());

    // Pass along file path information
    builder.setFileSystemConfig(easyConfig().fsConf);
    builder.setFiles(scan.getWorkUnits());
    final Path selectionRoot = scan.getSelectionRoot();
    if (selectionRoot != null) {
      builder.implicitColumnOptions().setSelectionRoot(selectionRoot);
      builder.implicitColumnOptions().setPartitionDepth(scan.getPartitionDepth());
    }

    // Additional error context to identify this plugin
    builder.errorContext(
        currentBuilder -> currentBuilder
            .addContext("Format plugin", easyConfig.getDefaultName())
            .addContext("Format plugin", EasyFormatPlugin.this.getClass().getSimpleName())
            .addContext("Plugin config name", getName()));
  }

  public ManagedReader<? extends FileSchemaNegotiator> newBatchReader(
      EasySubScan scan, OptionManager options) throws ExecutionSetupException {
    throw new ExecutionSetupException("Must implement newBatchReader() if using the enhanced framework.");
  }

  /**
   * Create the plugin-specific framework that manages the scan. The framework
   * creates batch readers one by one for each file or block. It defines semantic
   * rules for projection. It handles "early" or "late" schema readers. A typical
   * framework builds on standardized frameworks for files in general or text
   * files in particular.
   *
   * @param scan the physical operation definition for the scan operation. Contains
   * one or more files to read. (The Easy format plugin works only for files.)
   * @return the scan framework which orchestrates the scan operation across
   * potentially many files
   * @throws ExecutionSetupException for all setup failures
   */
  protected FileScanBuilder frameworkBuilder(
      OptionManager options, EasySubScan scan) throws ExecutionSetupException {
    throw new ExecutionSetupException("Must implement frameworkBuilder() if using the enhanced framework.");
  }

  public boolean isStatisticsRecordWriter(FragmentContext context, EasyWriter writer) {
    return false;
  }

  public RecordWriter getRecordWriter(FragmentContext context,
                                      EasyWriter writer) throws IOException {
    throw new UnsupportedOperationException("unimplemented");
  }

  public StatisticsRecordWriter getStatisticsRecordWriter(FragmentContext context,
      EasyWriter writer) throws IOException {
    return null;
  }

  public CloseableRecordBatch getWriterBatch(FragmentContext context, RecordBatch incoming, EasyWriter writer)
      throws ExecutionSetupException {
    try {
      if (isStatisticsRecordWriter(context, writer)) {
        return new StatisticsWriterRecordBatch(writer, incoming, context, getStatisticsRecordWriter(context, writer));
      } else {
        return new WriterRecordBatch(writer, incoming, context, getRecordWriter(context, writer));
      }
    } catch(IOException e) {
      throw new ExecutionSetupException(String.format("Failed to create the WriterRecordBatch. %s", e.getMessage()), e);
    }
  }

  protected ScanStats getScanStats(PlannerSettings settings, EasyGroupScan scan) {
    long data = 0;
    for (CompleteFileWork work : scan.getWorkIterable()) {
      data += work.getTotalBytes();
    }

    final long estRowCount = data / 1024;
    return new ScanStats(GroupScanProperty.NO_EXACT_ROW_COUNT, estRowCount, 1, data);
  }

  @Override
  public AbstractWriter getWriter(PhysicalOperator child, String location, List<String> partitionColumns) {
    return new EasyWriter(child, location, partitionColumns, this);
  }

  @Override
  public AbstractGroupScan getGroupScan(String userName, FileSelection selection, List<SchemaPath> columns)
      throws IOException {
    return new EasyGroupScan(userName, selection, this, columns, selection.selectionRoot, null);
  }

  @Override
  public AbstractGroupScan getGroupScan(String userName, FileSelection selection,
      List<SchemaPath> columns, MetadataProviderManager metadataProviderManager) throws IOException {
    return new EasyGroupScan(userName, selection, this, columns, selection.selectionRoot, metadataProviderManager);
  }

  @Override
  public T getConfig() { return formatConfig; }

  @Override
  public StoragePluginConfig getStorageConfig() { return storageConfig; }

  @Override
  public boolean supportsRead() { return easyConfig.isReadable(); }

  @Override
  public boolean supportsWrite() { return easyConfig.isWritable(); }

  @Override
  public boolean supportsAutoPartitioning() { return easyConfig.supportsAutoPartitioning(); }

  @Override
  public FormatMatcher getMatcher() { return easyConfig.getMatcher(); }

  @Override
  public Set<StoragePluginOptimizerRule> getOptimizerRules() {
    return ImmutableSet.of();
  }

  public String getReaderOperatorType() {
    return easyConfig.getReaderOperatorType();
  }

  public String getWriterOperatorType() { return easyConfig.getWriterOperatorType(); }

  @Override
  public boolean supportsStatistics() { return easyConfig.supportsStatistics(); }

  @Override
  public TableStatistics readStatistics(FileSystem fs, Path statsTablePath) throws IOException {
    throw new UnsupportedOperationException("unimplemented");
  }

  @Override
  public void writeStatistics(TableStatistics statistics, FileSystem fs,
      Path statsTablePath) throws IOException {
    throw new UnsupportedOperationException("unimplemented");
  }
}
