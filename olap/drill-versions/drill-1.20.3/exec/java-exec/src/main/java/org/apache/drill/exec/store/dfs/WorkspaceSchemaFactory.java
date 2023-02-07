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
package org.apache.drill.exec.store.dfs;

import static java.util.Collections.unmodifiableList;
import static org.apache.drill.exec.dotdrill.DotDrillType.STATS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Table;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.common.util.function.CheckedFunction;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.dotdrill.DotDrillFile;
import org.apache.drill.exec.dotdrill.DotDrillType;
import org.apache.drill.exec.dotdrill.DotDrillUtil;
import org.apache.drill.exec.dotdrill.View;
import org.apache.drill.exec.metastore.store.FileSystemMetadataProviderManager;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.metastore.MetastoreMetadataProviderManager;
import org.apache.drill.exec.metastore.MetastoreMetadataProviderManager.MetastoreMetadataProviderConfig;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.planner.logical.CreateTableEntry;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillViewTable;
import org.apache.drill.exec.planner.logical.DynamicDrillTable;
import org.apache.drill.exec.planner.logical.FileSystemCreateTableEntry;
import org.apache.drill.exec.planner.sql.ExpandingConcurrentMap;
import org.apache.drill.exec.record.metadata.schema.FsMetastoreSchemaProvider;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.PartitionNotFoundException;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.table.function.TableParamDef;
import org.apache.drill.exec.store.table.function.TableSignature;
import org.apache.drill.exec.store.table.function.WithOptionsTableMacro;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.store.easy.json.JSONFormatPlugin;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.exceptions.MetastoreException;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.AccessControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

public class WorkspaceSchemaFactory {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceSchemaFactory.class);

  private final List<FormatMatcher> fileMatchers;
  private final List<FormatMatcher> dropFileMatchers;
  private final List<FormatMatcher> dirMatchers;
  private final List<FormatLocationTransformer> formatLocationTransformers;

  private final WorkspaceConfig config;
  private final Configuration fsConf;
  private final String storageEngineName;
  private final String schemaName;
  private final FileSystemPlugin plugin;
  private final ObjectMapper mapper;
  private final Path wsPath;

  private final FormatPluginOptionExtractor optionExtractor;

  public WorkspaceSchemaFactory(
      FileSystemPlugin plugin,
      String schemaName,
      String storageEngineName,
      WorkspaceConfig config,
      List<FormatMatcher> formatMatchers,
      ObjectMapper mapper,
      ScanResult scanResult) throws ExecutionSetupException {
    this.mapper = mapper;
    this.fsConf = plugin.getFsConf();
    this.plugin = plugin;
    this.config = config;
    this.fileMatchers = new ArrayList<>();
    this.dirMatchers = new ArrayList<>();
    this.formatLocationTransformers = new ArrayList<>();
    this.storageEngineName = storageEngineName;
    this.schemaName = schemaName;
    this.wsPath = new Path(config.getLocation());
    this.optionExtractor = new FormatPluginOptionExtractor(scanResult);

    for (FormatMatcher m : formatMatchers) {
      if (m.supportDirectoryReads()) {
        dirMatchers.add(m);
      }
      fileMatchers.add(m);
      FormatLocationTransformer transformer = m.getFormatLocationTransformer();
      if (transformer != null) {
        formatLocationTransformers.add(transformer);
      }
    }

    // NOTE: Add fallback format matcher if given in the configuration. Make sure fileMatchers is an order-preserving list.
    final String defaultInputFormat = config.getDefaultInputFormat();
    if (!Strings.isNullOrEmpty(defaultInputFormat)) {
      final FormatPlugin formatPlugin = plugin.getFormatPlugin(defaultInputFormat);
      if (formatPlugin == null) {
        final String message = String.format("Unable to find default input format[%s] for workspace[%s.%s]",
            defaultInputFormat, storageEngineName, schemaName);
        throw new ExecutionSetupException(message);
      }
      final FormatMatcher fallbackMatcher = new BasicFormatMatcher(formatPlugin,
          ImmutableList.of(Pattern.compile(".*")), ImmutableList.of());
      fileMatchers.add(fallbackMatcher);
      dropFileMatchers = fileMatchers.subList(0, fileMatchers.size() - 1);
    } else {
      dropFileMatchers = fileMatchers.subList(0, fileMatchers.size());
    }
  }

  /**
   * Checks whether the given user has permission to list files/directories under the workspace directory.
   *
   * @param userName User who is trying to access the workspace.
   * @return True if the user has access. False otherwise.
   */
  public boolean accessible(final String userName) throws IOException {
    final DrillFileSystem fs = ImpersonationUtil.createFileSystem(userName, fsConf);
    return accessible(fs);
  }

  /**
   * Checks whether a FileSystem object has the permission to list/read workspace directory
   * @param fs a DrillFileSystem object that was created with certain user privilege
   * @return True if the user has access. False otherwise.
   */
  public boolean accessible(DrillFileSystem fs) throws IOException {
    try {
      /*
       * For Windows local file system, fs.access ends up using DeprecatedRawLocalFileStatus which has
       * TrustedInstaller as owner, and a member of Administrators group could not satisfy the permission.
       * In this case, we will still use method listStatus.
       * In other cases, we use access method since it is cheaper.
       */
      if (SystemUtils.IS_OS_WINDOWS && fs.getUri().getScheme().equalsIgnoreCase(FileSystemSchemaFactory.LOCAL_FS_SCHEME)) {
        fs.listStatus(wsPath);
      }
      else {
        fs.access(wsPath, FsAction.READ);
      }
    } catch (final UnsupportedOperationException e) {
      logger.trace("The filesystem for this workspace does not support this operation.", e);
    } catch (final FileNotFoundException | AccessControlException e) {
      return false;
    }

    return true;
  }

  private Path getViewPath(String name) {
    return DotDrillType.VIEW.getPath(config.getLocation(), name);
  }

  public WorkspaceSchema createSchema(List<String> parentSchemaPath, SchemaConfig schemaConfig, DrillFileSystem fs) throws IOException {
    if (!accessible(fs)) {
      return null;
    }
    return new WorkspaceSchema(parentSchemaPath, schemaName, schemaConfig, fs);
  }

  public String getSchemaName() {
    return schemaName;
  }

  public FileSystemPlugin getPlugin() {
    return plugin;
  }

  // Ensure given tableName is not a stats table
  private static void ensureNotStatsTable(final String tableName) {
    if (tableName.toLowerCase().endsWith(STATS.getEnding())) {
      throw UserException
          .validationError()
          .message("Given table [%s] is already a stats table. " +
              "Cannot perform stats operations on a stats table.", tableName)
          .build(logger);
    }
  }

  private static Object[] array(Object... objects) {
    return objects;
  }

  public static final class TableInstance {
    final TableSignature sig;
    final List<Object> params;

    public TableInstance(TableSignature sig, List<Object> params) {
      if (params.size() != sig.getParams().size()) {
        throw UserException.parseError()
            .message(
                "should have as many params (%d) as signature (%d)",
                params.size(), sig.getParams().size())
            .addContext("table", sig.getName())
            .build(logger);
      }
      this.sig = sig;
      this.params = unmodifiableList(params);
    }

    String presentParams() {
      StringBuilder sb = new StringBuilder("(");
      boolean first = true;
      for (int i = 0; i < params.size(); i++) {
        Object param = params.get(i);
        if (param != null) {
          if (first) {
            first = false;
          } else {
            sb.append(", ");
          }
          TableParamDef paramDef = sig.getParams().get(i);
          sb.append(paramDef.getName()).append(": ").append(paramDef.getType().getSimpleName()).append(" => ").append(param);
        }
      }
      sb.append(")");
      return sb.toString();
    }

    private Object[] toArray() {
      return array(sig, params);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(toArray());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TableInstance) {
        return Arrays.equals(this.toArray(), ((TableInstance)obj).toArray());
      }
      return false;
    }

    @Override
    public String toString() {
      return sig.getName() + (params.size() == 0 ? "" : presentParams());
    }
  }

  public class WorkspaceSchema extends AbstractSchema implements ExpandingConcurrentMap.MapValueFactory<TableInstance, DrillTable> {
    private final ExpandingConcurrentMap<TableInstance, DrillTable> tables = new ExpandingConcurrentMap<>(this);
    private final SchemaConfig schemaConfig;
    private final DrillFileSystem fs;
    // Drill Process User file-system
    private final DrillFileSystem dpsFs;

    public WorkspaceSchema(List<String> parentSchemaPath, String wsName, SchemaConfig schemaConfig, DrillFileSystem fs) {
      super(parentSchemaPath, wsName);
      this.schemaConfig = schemaConfig;
      this.fs = fs;
      this.dpsFs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), fsConf);
    }

    DrillTable getDrillTable(TableInstance key) {
      return tables.get(key);
    }

    @Override
    public boolean createView(View view) throws IOException {
      Path viewPath = getViewPath(view.getName());
      boolean replaced = getFS().exists(viewPath);
      final FsPermission viewPerms =
          new FsPermission(schemaConfig.getOption(ExecConstants.NEW_VIEW_DEFAULT_PERMS_KEY).string_val);
      try (OutputStream stream = DrillFileSystem.create(getFS(), viewPath, viewPerms)) {
        mapper.writeValue(stream, view);
      }
      return replaced;
    }

    @Override
    public Iterable<String> getSubPartitions(String table,
                                             List<String> partitionColumns,
                                             List<String> partitionValues
    ) throws PartitionNotFoundException {

      List<FileStatus> fileStatuses;
      try {
        fileStatuses = DrillFileSystemUtil.listDirectories(getFS(), new Path(getDefaultLocation(), table), false);
      } catch (IOException e) {
        throw new PartitionNotFoundException("Error finding partitions for table " + table, e);
      }
      return new SubDirectoryList(fileStatuses);
    }

    @Override
    public void dropView(String viewName) throws IOException {
      getFS().delete(getViewPath(viewName), false);
    }

    private Set<String> getViews() {
      Set<String> viewSet = Sets.newHashSet();
      // Look for files with ".view.drill" extension.
      List<DotDrillFile> files;
      try {
        files = DotDrillUtil.getDotDrills(getFS(), new Path(config.getLocation()), DotDrillType.VIEW);
        for (DotDrillFile f : files) {
          viewSet.add(f.getBaseName());
        }
      } catch (UnsupportedOperationException e) {
        logger.debug("The filesystem for this workspace does not support this operation.", e);
      } catch (AccessControlException e) {
        if (!schemaConfig.getIgnoreAuthErrors()) {
          logger.debug(e.getMessage());
          throw UserException
              .permissionError(e)
              .message("Not authorized to list view tables in schema [%s]", getFullSchemaName())
              .build(logger);
        }
      } catch (Exception e) {
        logger.warn("Failure while trying to list .view.drill files in workspace [{}]", getFullSchemaName(), e);
      }

      return viewSet;
    }

    private Set<String> rawTableNames() {
      return tables.keySet().stream()
          .map(input -> input.sig.getName())
          .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getTableNames() {
      return Sets.union(rawTableNames(), getViews());
    }

    @Override
    public Set<String> getFunctionNames() {
      return rawTableNames();
    }

    @Override
    public List<Function> getFunctions(String name) {
      // add parent functions first
      List<Function> functions = new ArrayList<>(super.getFunctions(name));

      List<TableParamDef> tableParameters = getFunctionParameters();
      List<TableSignature> signatures = optionExtractor.getTableSignatures(name, tableParameters);
      signatures.stream()
        .map(signature -> new WithOptionsTableMacro(signature, params -> getDrillTable(new TableInstance(signature, params))))
        .forEach(functions::add);

      return functions;
    }

    private View getView(DotDrillFile f) throws IOException {
      assert f.getType() == DotDrillType.VIEW;
      return f.getView(mapper);
    }

    @Override
    public Table getTable(String tableName) {
      TableInstance tableKey = new TableInstance(TableSignature.of(tableName), ImmutableList.of());
      // first check existing tables.
      if (tables.alreadyContainsKey(tableKey)) {
        return tables.get(tableKey);
      }

      // then look for files that start with this name and end in .drill.
      List<DotDrillFile> files = Collections.emptyList();
      try {
        try {
          files = DotDrillUtil.getDotDrills(getFS(), new Path(config.getLocation()),
              DrillStringUtils.removeLeadingSlash(tableName), DotDrillType.VIEW);
        } catch (AccessControlException e) {
          if (!schemaConfig.getIgnoreAuthErrors()) {
            logger.debug(e.getMessage());
            throw UserException.permissionError(e)
              .message("Not authorized to list or query tables in schema [%s]", getFullSchemaName())
              .build(logger);
          }
        } catch (IOException e) {
          logger.warn("Failure while trying to list view tables in workspace [{}]", getFullSchemaName(), e);
        }

        for (DotDrillFile f : files) {
          switch (f.getType()) {
            case VIEW:
              try {
                return new DrillViewTable(getView(f), f.getOwner(), schemaConfig.getViewExpansionContext());
              } catch (AccessControlException e) {
                if (!schemaConfig.getIgnoreAuthErrors()) {
                  logger.debug(e.getMessage());
                  throw UserException.permissionError(e)
                    .message("Not authorized to read view [%s] in schema [%s]", tableName, getFullSchemaName())
                    .build(logger);
                }
              } catch (IOException e) {
                logger.warn("Failure while trying to load {}.view.drill file in workspace [{}]", tableName, getFullSchemaName(), e);
              }
            default:
          }
        }
      } catch (UnsupportedOperationException e) {
        logger.debug("The filesystem for this workspace does not support this operation.", e);
      }
      DrillTable table = tables.get(tableKey);
      setMetadataProviderManager(table, tableName);
      return table;
    }

    private void setSchema(MetadataProviderManager providerManager, String tableName) {
      if (schemaConfig.getOption(ExecConstants.STORE_TABLE_USE_SCHEMA_FILE).bool_val) {
        try {
          FsMetastoreSchemaProvider schemaProvider = new FsMetastoreSchemaProvider(this, tableName);
          providerManager.setSchemaProvider(schemaProvider);
        } catch (IOException e) {
          logger.debug("Unable to init schema provider for table [{}]", tableName, e);
        }
      }
    }

    private void setMetadataTable(MetadataProviderManager metadataProviderManager, DrillTable table, final String tableName) {

      // If this itself is the stats table, then skip it.
      if (tableName.toLowerCase().endsWith(STATS.getEnding())) {
        return;
      }

      try {
        String statsTableName = getStatsTableName(tableName);
        Path statsTableFilePath = getStatsTableFilePath(tableName);
        metadataProviderManager.setStatsProvider(new DrillStatsTable(table, getFullSchemaName(), statsTableName,
            statsTableFilePath, fs));
      } catch (Exception e) {
        logger.warn("Failed to find the stats table for table [{}] in schema [{}]",
            tableName, getFullSchemaName());
      }
    }

    // Get stats table name for a given table name.
    private String getStatsTableName(final String tableName) {
      // Access stats file as DRILL process user (not impersonated user)
      final Path tablePath = new Path(config.getLocation(), tableName);
      try {
        String name;
        if (dpsFs.isDirectory(tablePath)) {
          name = tableName + Path.SEPARATOR + STATS.getEnding();
          if (dpsFs.isDirectory(new Path(name))) {
            return name;
          }
        } else {
          //TODO: Not really useful. Remove?
          name = tableName + STATS.getEnding();
          if (dpsFs.isFile(new Path(name))) {
            return name;
          }
        }
        return name;
      } catch (final Exception e) {
        throw new DrillRuntimeException(
            String.format("Failed to find the stats for table [%s] in schema [%s]",
                tableName, getFullSchemaName()));
      }
    }

    // Get stats table file (JSON) path for the given table name.
    private Path getStatsTableFilePath(final String tableName) {
      // Access stats file as DRILL process user (not impersonated user)
      final Path tablePath = new Path(config.getLocation(), tableName);
      try {
        Path stFPath = null;
        if (dpsFs.isDirectory(tablePath)) {
          stFPath = new Path(tablePath, STATS.getEnding()+ Path.SEPARATOR + "0_0.json");
          if (dpsFs.isFile(stFPath)) {
            return stFPath;
          }
        }
        return stFPath;
      } catch (final Exception e) {
        throw new DrillRuntimeException(
            String.format("Failed to find the the stats for table [%s] in schema [%s]",
                tableName, getFullSchemaName()));
      }
    }

    @Override
    public boolean isMutable() {
      return config.isWritable();
    }

    public DrillFileSystem getFS() {
      return fs;
    }

    public String getDefaultLocation() {
      return config.getLocation();
    }

    @Override
    public CreateTableEntry createNewTable(String tableName, List<String> partitionColumns, StorageStrategy storageStrategy) {
      String storage = schemaConfig.getOption(ExecConstants.OUTPUT_FORMAT_OPTION).string_val;
      FormatPlugin formatPlugin = plugin.getFormatPlugin(storage);

      return createOrAppendToTable(tableName, formatPlugin, partitionColumns, storageStrategy);
    }

    @Override
    public CreateTableEntry createStatsTable(String tableName) {
      ensureNotStatsTable(tableName);
      final String statsTableName = getStatsTableName(tableName);
      FormatPlugin formatPlugin = plugin.getFormatPlugin(JSONFormatPlugin.DEFAULT_NAME);
      return createOrAppendToTable(statsTableName, formatPlugin, Collections.emptyList(),
          StorageStrategy.DEFAULT);
    }

    @Override
    public CreateTableEntry appendToStatsTable(String tableName) {
      ensureNotStatsTable(tableName);
      final String statsTableName = getStatsTableName(tableName);
      FormatPlugin formatPlugin = plugin.getFormatPlugin(JSONFormatPlugin.DEFAULT_NAME);
      return createOrAppendToTable(statsTableName, formatPlugin, Collections.emptyList(),
          StorageStrategy.DEFAULT);
    }

    @Override
    public Table getStatsTable(String tableName) {
      return getTable(getStatsTableName(tableName));
    }

    private CreateTableEntry createOrAppendToTable(String tableName, FormatPlugin formatPlugin,
        List<String> partitionColumns, StorageStrategy storageStrategy) {
      if (formatPlugin == null) {
        throw new UnsupportedOperationException(
          String.format("Unsupported format '%s' in workspace '%s'", config.getDefaultInputFormat(),
              Joiner.on(".").join(getSchemaPath())));
      }

      return new FileSystemCreateTableEntry(
          (FileSystemConfig) plugin.getConfig(),
          formatPlugin,
          config.getLocation() + Path.SEPARATOR + tableName,
          partitionColumns,
          storageStrategy);
    }

    @Override
    public String getTypeName() {
      return FileSystemConfig.NAME;
    }

    @Override
    public DrillTable create(TableInstance key) {
      try {
        FileSelectionInspector inspector = new FileSelectionInspector(key);
        if (inspector.fileSelection == null) {
          return null;
        }

        DrillTable table = inspector.matchFormat();

        if (key.sig.getParams().size() == 0) {
          return table;
        } else {
          return parseTableFunction(key, inspector, table);
        }
      } catch (AccessControlException e) {
        if (!schemaConfig.getIgnoreAuthErrors()) {
          logger.debug(e.getMessage());
          throw UserException.permissionError(e)
            .message("Not authorized to read table [%s] in schema [%s]", key, getFullSchemaName())
            .build(logger);
        }
      } catch (IOException e) {
        logger.debug("Failed to create DrillTable with root {} and name {}", config.getLocation(), key, e);
      }
      return null;
    }

    private DrillTable parseTableFunction(TableInstance key,
        FileSelectionInspector inspector, DrillTable table) {
      FileSelection newSelection = inspector.selection();

      if (newSelection.isEmptyDirectory()) {
        return new DynamicDrillTable(plugin, storageEngineName, schemaConfig.getUserName(),
            inspector.fileSelection);
      }

      FormatPluginConfig baseConfig = inspector.formatMatch == null
          ? null : inspector.formatMatch.getFormatPlugin().getConfig();
      FormatPluginConfig formatConfig = optionExtractor.createConfigForTable(key, mapper, baseConfig);
      FormatSelection selection = new FormatSelection(formatConfig, newSelection);
      DrillTable drillTable = new DynamicDrillTable(plugin, storageEngineName, schemaConfig.getUserName(), selection);
      setMetadataProviderManager(drillTable, key.sig.getName());

      List<TableParamDef> commonParams = key.sig.getCommonParams();
      if (commonParams.isEmpty()) {
        return drillTable;
      }
      // extract only common parameters related values
      List<Object> paramValues = key.params.subList(key.params.size() - commonParams.size(), key.params.size());
      return applyFunctionParameters(drillTable, commonParams, paramValues);
    }

    /**
     * Expands given file selection if it has directories.
     * If expanded file selection is null (i.e. directory is empty), sets empty directory status to true.
     *
     * @param fileSelection file selection
     * @param hasDirectories flag that indicates if given file selection has directories
     * @return revisited file selection
     */
    private FileSelection detectEmptySelection(FileSelection fileSelection, boolean hasDirectories) throws IOException {
      FileSelection newSelection = hasDirectories ? fileSelection.minusDirectories(getFS()) : fileSelection;
      if (newSelection == null) {
        // empty directory / selection means that this is the empty and schemaless table
        fileSelection.setEmptyDirectoryStatus();
        return fileSelection;
      }
      return newSelection;
    }

    private FormatMatcher findMatcher(FileStatus file) {
      try {
        for (FormatMatcher m : dropFileMatchers) {
          if (m.isFileReadable(getFS(), file)) {
            return m;
          }
        }
      } catch (IOException e) {
        logger.debug("Failed to find format matcher for file: {}", file, e);
      }
      return null;
    }

    private void setMetadataProviderManager(DrillTable table, String tableName) {
      if (table != null) {
        MetadataProviderManager providerManager = null;

        if (schemaConfig.getOption(ExecConstants.METASTORE_ENABLED).bool_val) {
          try {
            MetastoreRegistry metastoreRegistry = plugin.getContext().getMetastoreRegistry();
            TableInfo tableInfo = TableInfo.builder()
                .storagePlugin(plugin.getName())
                .workspace(schemaName)
                .name(tableName)
                .build();

            MetastoreTableInfo metastoreTableInfo = metastoreRegistry.get()
                .tables()
                .basicRequests()
                .metastoreTableInfo(tableInfo);
            if (metastoreTableInfo.isExists()) {
              providerManager = new MetastoreMetadataProviderManager(metastoreRegistry, tableInfo,
                  new MetastoreMetadataProviderConfig(schemaConfig.getOption(ExecConstants.METASTORE_USE_SCHEMA_METADATA).bool_val,
                      schemaConfig.getOption(ExecConstants.METASTORE_USE_STATISTICS_METADATA).bool_val,
                      schemaConfig.getOption(ExecConstants.METASTORE_FALLBACK_TO_FILE_METADATA).bool_val));
            }
          } catch (MetastoreException e) {
            logger.warn("Exception happened during obtaining Metastore instance. File system metadata provider will be used.", e);
          }
        }
        if (providerManager == null) {
          providerManager = FileSystemMetadataProviderManager.init();
        }
        setMetadataTable(providerManager, table, tableName);
        setSchema(providerManager, tableName);
        table.setTableMetadataProviderManager(providerManager);
      }
    }

    @Override
    public void destroy(DrillTable value) {
    }

    /**
     * Check if the table contains homogeneous files that can be read by Drill. Eg: parquet, json csv etc.
     * However if it contains more than one of these formats or a totally different file format that Drill cannot
     * understand then we will raise an exception.
     * @param tableName name of the table to be checked for homogeneous property
     * @return true if table contains homogeneous files, false otherwise
     * @throws IOException is case of problems accessing table files
     */
    private boolean isHomogeneous(String tableName) throws IOException {
      FileSelection fileSelection = FileSelection.create(getFS(), config.getLocation(), tableName, config.allowAccessOutsideWorkspace());

      if (fileSelection == null) {
        throw UserException
            .validationError()
            .message(String.format("Table [%s] not found", tableName))
            .build(logger);
      }

      FormatMatcher matcher = null;
      Queue<FileStatus> listOfFiles = new LinkedList<>(fileSelection.getStatuses(getFS()));

      while (!listOfFiles.isEmpty()) {
        FileStatus currentFile = listOfFiles.poll();
        if (currentFile.isDirectory()) {
          listOfFiles.addAll(DrillFileSystemUtil.listFiles(getFS(), currentFile.getPath(), true));
        } else {
          if (matcher != null) {
            if (!matcher.isFileReadable(getFS(), currentFile)) {
              return false;
            }
          } else {
            matcher = findMatcher(currentFile);
            // Did not match any of the file patterns, exit
            if (matcher == null) {
              return false;
            }
          }
        }
      }
      return true;
    }

    /**
     * We check if the table contains homogeneous file formats that Drill can read. Once the checks are performed
     * we rename the file to start with an "_". After the rename we issue a recursive delete of the directory.
     * @param table - Path of table to be dropped
     */
    @Override
    public void dropTable(String table) {
      DrillFileSystem fs = getFS();
      String defaultLocation = getDefaultLocation();
      try {
        if (!isHomogeneous(table)) {
          throw UserException
              .validationError()
              .message("Table contains different file formats. \n" +
                  "Drop Table is only supported for directories that contain homogeneous file formats consumable by Drill")
              .build(logger);
        }

        StringBuilder tableRenameBuilder = new StringBuilder();
        int lastSlashIndex = table.lastIndexOf(Path.SEPARATOR);
        if (lastSlashIndex != -1) {
          tableRenameBuilder.append(table, 0, lastSlashIndex + 1);
        }
        // Generate unique identifier which will be added as a suffix to the table name
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long time =  (System.currentTimeMillis()/1000);
        long p1 = ((Integer.MAX_VALUE - time) << 32) + r.nextInt();
        long p2 = r.nextLong();
        final String fileNameDelimiter = DrillFileSystem.UNDERSCORE_PREFIX;
        String[] pathSplit = table.split(Path.SEPARATOR);
        /*
         * Builds the string for the renamed table
         * Prefixes the table name with an underscore (intent for this to be treated as a hidden file)
         * and suffixes the table name with unique identifiers (similar to how we generate query id's)
         * separated by underscores
         */
        tableRenameBuilder
            .append(DrillFileSystem.UNDERSCORE_PREFIX)
            .append(pathSplit[pathSplit.length - 1])
            .append(fileNameDelimiter)
            .append(p1)
            .append(fileNameDelimiter)
            .append(p2);

        String tableRename = tableRenameBuilder.toString();
        fs.rename(new Path(defaultLocation, table), new Path(defaultLocation, tableRename));
        fs.delete(new Path(defaultLocation, tableRename), true);
      } catch (AccessControlException e) {
        throw UserException
            .permissionError(e)
            .message("Unauthorized to drop table")
            .build(logger);
      } catch (IOException e) {
        throw UserException
            .dataWriteError(e)
            .message("Failed to drop table: " + e.getMessage())
            .build(logger);
      }
    }

    @Override
    public List<Map.Entry<String, TableType>> getTableNamesAndTypes() {
      return Stream.concat(
          tables.entrySet().stream().map(kv -> Pair.of(kv.getKey().sig.getName(), kv.getValue().getJdbcTableType())),
          getViews().stream().map(viewName -> Pair.of(viewName, TableType.VIEW))
      ).collect(Collectors.toList());
    }

    /**
     * Compute and retain file selection and format match properties used
     * by multiple functions above.
     */
    private class FileSelectionInspector {
      private final TableInstance key;
      private final DrillFileSystem fs;
      public final FileSelection fileSelection;
      public final boolean hasDirectories;
      private FileSelection newSelection;
      public FormatMatcher formatMatch;

      public FileSelectionInspector(TableInstance key) throws IOException {
        this.key = key;
        this.fs = getFS();
        String path = key.sig.getName();
        FileSelection fileSelection = getFileSelection(path);
        if (fileSelection == null) {
          fileSelection = formatLocationTransformers.stream()
            .filter(t -> t.canTransform(path))
            .map(t -> t.transform(path, (CheckedFunction<String, FileSelection, IOException>) this::getFileSelection))
            .findFirst()
            .orElse(null);
        }
        this.fileSelection = fileSelection;
        this.hasDirectories = fileSelection != null && fileSelection.containsDirectories(fs);
      }

      private FileSelection getFileSelection(String path) throws IOException {
        return FileSelection.create(fs, config.getLocation(), path, config.allowAccessOutsideWorkspace());
      }

      protected DrillTable matchFormat() throws IOException {
         if (hasDirectories) {
          for (final FormatMatcher matcher : dirMatchers) {
            try {
              DrillTable table = matcher.isReadable(getFS(), fileSelection, plugin, storageEngineName, schemaConfig);
              if (table != null) {
                formatMatch = matcher;
                setMetadataProviderManager(table, key.sig.getName());
                return table;
              }
            } catch (IOException e) {
              logger.debug("File read failed.", e);
            }
          }
        }

        newSelection = detectEmptySelection(fileSelection, hasDirectories);
        if (newSelection.isEmptyDirectory()) {
          return new DynamicDrillTable(plugin, storageEngineName, schemaConfig.getUserName(), fileSelection);
        }

        for (final FormatMatcher matcher : fileMatchers) {
          DrillTable table = matcher.isReadable(getFS(), newSelection, plugin, storageEngineName, schemaConfig);
          if (table != null) {
            formatMatch = matcher;
            setMetadataProviderManager(table, key.sig.getName());
            return table;
          }
        }
        return null;
      }

      public FileSelection selection() {
        return newSelection != null ? newSelection : fileSelection;
      }
    }
  }
}
