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
package org.apache.drill.exec.store.parquet.metadata;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.collections.Collectors;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.drill.exec.serialization.PathSerDe;
import org.apache.drill.exec.store.TimedCallable;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetTableMetadataBase;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.RowGroupMetadata;
import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.SUPPORTED_VERSIONS;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ColumnMetadata_v4;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ColumnTypeMetadata_v4;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.FileMetadata;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.MetadataSummary;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ParquetFileAndRowCountMetadata;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ParquetFileMetadata_v4;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ParquetTableMetadata_v4;

/**
 * This is an utility class, holder for Parquet Table Metadata and {@link ParquetReaderConfig}. All the creation of
 * parquet metadata cache using create api's are forced to happen using the process user since only that user will have
 * write permission for the cache file
 */
public class Metadata {
  private static final Logger logger = LoggerFactory.getLogger(Metadata.class);

  public static final String[] OLD_METADATA_FILENAMES = {".drill.parquet_metadata", ".drill.parquet_metadata.v2"};
  public static final String OLD_METADATA_FILENAME = ".drill.parquet_metadata";
  public static final String METADATA_DIRECTORIES_FILENAME = ".drill.parquet_metadata_directories";
  public static final String METADATA_FILENAME = ".drill.parquet_file_metadata.v4";
  public static final String METADATA_SUMMARY_FILENAME = ".drill.parquet_summary_metadata.v4";
  public static final String[] CURRENT_METADATA_FILENAMES = {METADATA_SUMMARY_FILENAME, METADATA_FILENAME};
  public static final Long DEFAULT_NULL_COUNT = 0L;
  public static final Long NULL_COUNT_NOT_EXISTS = -1L;

  private final ParquetReaderConfig readerConfig;

  private ParquetTableMetadataBase parquetTableMetadata;
  private ParquetTableMetadataDirs parquetTableMetadataDirs;

  private Metadata(ParquetReaderConfig readerConfig) {
    this.readerConfig = readerConfig;
  }

  /**
   * Create the parquet metadata file for the directory at the given path, and for any subdirectories.
   * @param fs file system
   * @param path path
   * @param readerConfig parquet reader configuration
   * @param allColumnsInteresting if set, store column metadata for all the columns
   * @param columnSet Set of columns for which column metadata has to be stored
   */
  public static void createMeta(FileSystem fs, Path path, ParquetReaderConfig readerConfig,
      boolean allColumnsInteresting, Set<SchemaPath> columnSet) throws IOException {
    Metadata metadata = new Metadata(readerConfig);
    metadata.createMetaFilesRecursivelyAsProcessUser(path, fs, allColumnsInteresting, columnSet, false );
  }

  /**
   * Get the parquet metadata for the parquet files in the given directory, including those in subdirectories.
   *
   * @param fs file system
   * @param path path
   * @param readerConfig parquet reader configuration
   *
   * @return parquet table metadata
   */
  public static ParquetTableMetadata_v4 getParquetTableMetadata(FileSystem fs, Path path, ParquetReaderConfig readerConfig) throws IOException {
    Metadata metadata = new Metadata(readerConfig);
    return metadata.getParquetTableMetadata(path, fs);
  }

  /**
   * Get the parquet metadata for a list of parquet files.
   *
   * @param fileStatusMap file statuses and corresponding file systems
   * @param readerConfig parquet reader configuration
   * @return parquet table metadata
   */
  public static ParquetTableMetadata_v4 getParquetTableMetadata(Map<FileStatus, FileSystem> fileStatusMap,
                                                                ParquetReaderConfig readerConfig) throws IOException {
    Metadata metadata = new Metadata(readerConfig);
    return metadata.getParquetTableMetadata(fileStatusMap);
  }

  /**
   * Get the parquet metadata for the table by reading the metadata file
   *
   * @param fs current file system
   * @param paths The path to the metadata file, located in the directory that contains the parquet files
   * @param metaContext metadata context
   * @param readerConfig parquet reader configuration
   * @return parquet table metadata. Null if metadata cache is missing, unsupported or corrupted
   */
  public static ParquetTableMetadataBase readBlockMeta(FileSystem fs,
                                                                 List<Path> paths,
                                                                 MetadataContext metaContext,
                                                                 ParquetReaderConfig readerConfig) {
    Metadata metadata = new Metadata(readerConfig);
    if (paths.isEmpty()) {
      metaContext.setMetadataCacheCorrupted(true);
    }
    for (Path path: paths) {
      // readBlockMeta updates the metadataCacheCorrupted flag. So while reading the cache files
      // metadataCacheCorrupted should be checked if any of the previous cache files are corrupted.
      if (ignoreReadingMetadata(metaContext, path)) {
        return null;
      }
      metadata.readBlockMeta(path, false, metaContext, fs);
    }
    return metadata.parquetTableMetadata;
  }

  /**
   * Get the parquet metadata for all subdirectories by reading the metadata file
   *
   * @param fs current file system
   * @param path The path to the metadata file, located in the directory that contains the parquet files
   * @param metaContext metadata context
   * @param readerConfig parquet reader configuration
   * @return parquet metadata for a directory. Null if metadata cache is missing, unsupported or corrupted
   */
  public static ParquetTableMetadataDirs readMetadataDirs(FileSystem fs,
                                                                    Path path,
                                                                    MetadataContext metaContext,
                                                                    ParquetReaderConfig readerConfig) {
    if (ignoreReadingMetadata(metaContext, path)) {
      return null;
    }
    Metadata metadata = new Metadata(readerConfig);
    metadata.readBlockMeta(path, true, metaContext, fs);
    return metadata.parquetTableMetadataDirs;
  }

  /**
   * Ignore reading metadata files, if metadata is missing, unsupported or corrupted
   *
   * @param metaContext Metadata context
   * @param path The path to the metadata file, located in the directory that contains the parquet files
   * @return true if parquet metadata is missing or corrupted, false otherwise
   */
  private static boolean ignoreReadingMetadata(MetadataContext metaContext, Path path) {
    if (metaContext.isMetadataCacheCorrupted()) {
      logger.warn("Ignoring of reading '{}' metadata file. Parquet metadata cache files are unsupported or corrupted. " +
          "Query performance may be slow. Make sure the cache files are up-to-date by running the 'REFRESH TABLE " +
          "METADATA' command", path);
      return true;
    }
    return false;
  }

  /**
   * Wrapper which makes sure that in all cases metadata file is created as a process user no matter what the caller
   * is passing.
   * @param path to the directory of the parquet table
   * @param fs file system
   * @param allColumnsInteresting if set, store column metadata for all the columns
   * @param columnSet Set of columns for which column metadata has to be stored
   * @return Pair of parquet metadata. The left one is a parquet metadata for the table. The right one of the Pair is
   *         a metadata for all subdirectories (if they are present and there are no any parquet files in the
   *         {@code path} directory).
   * @throws IOException if parquet metadata can't be serialized and written to the json file
   */
  private Pair<ParquetTableMetadata_v4, ParquetTableMetadataDirs> createMetaFilesRecursivelyAsProcessUser(
      Path path, FileSystem fs, boolean allColumnsInteresting, Set<SchemaPath> columnSet, boolean autoRefreshTriggered)
    throws IOException {

    final FileSystem processUserFileSystem = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(),
      fs.getConf());

    // Get the allColumnsInteresting flag and columnSet from the existing summary if the auto refresh is triggered.
    if (autoRefreshTriggered) {
      allColumnsInteresting = isAllColumnsInteresting(fs, path, true);
      columnSet = null;
      if (!allColumnsInteresting) {
        columnSet = getInterestingColumns(fs, path, true);
      }
    }

    return createMetaFilesRecursively(path, processUserFileSystem, allColumnsInteresting, columnSet);
  }

  /**
   * Create the parquet metadata files for the directory at the given path and for any subdirectories.
   * Metadata cache files written to the disk contain relative paths. Returned Pair of metadata contains absolute paths.
   *
   * @param path to the directory of the parquet table
   * @param fs file system
   * @param allColumnsInteresting if set, store column metadata for all the columns
   * @param columnSet Set of columns for which column metadata has to be stored
   * @return Pair of parquet metadata. The left one is a parquet metadata for the table. The right one of the Pair is
   *         a metadata for all subdirectories (if they are present and there are no any parquet files in the
   *         {@code path} directory).
   * @throws IOException if parquet metadata can't be serialized and written to the json file
   */
  private Pair<ParquetTableMetadata_v4, ParquetTableMetadataDirs> createMetaFilesRecursively(
      Path path, FileSystem fs, boolean allColumnsInteresting, Set<SchemaPath> columnSet) throws IOException {
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    List<ParquetFileMetadata_v4> metaDataList = Lists.newArrayList();
    List<Path> directoryList = Lists.newArrayList();
    ConcurrentHashMap<ColumnTypeMetadata_v4.Key, ColumnTypeMetadata_v4> columnTypeInfoSet =
        new ConcurrentHashMap<>();
    FileStatus fileStatus = fs.getFileStatus(path);
    long dirTotalRowCount = 0;
    assert fileStatus.isDirectory() : "Expected directory";

    final Map<FileStatus, FileSystem> childFiles = new LinkedHashMap<>();

    for (final FileStatus file : DrillFileSystemUtil.listAll(fs, path, false)) {
      if (file.isDirectory()) {
        ParquetTableMetadata_v4 subTableMetadata = (createMetaFilesRecursively(file.getPath(), fs, allColumnsInteresting, columnSet)).getLeft();
        ConcurrentHashMap<ColumnTypeMetadata_v4.Key, ColumnTypeMetadata_v4> subTableColumnTypeInfo = subTableMetadata.getColumnTypeInfoMap();
        metaDataList.addAll((List<ParquetFileMetadata_v4>) subTableMetadata.getFiles());
        directoryList.addAll(subTableMetadata.getDirectories());
        directoryList.add(file.getPath());
        //TODO: We need a merge method that merges two columns with the same name but different types
        if (columnTypeInfoSet.isEmpty()) {
          columnTypeInfoSet.putAll(subTableColumnTypeInfo);
        } else {
          for (ColumnTypeMetadata_v4.Key key : subTableColumnTypeInfo.keySet()) {
            ColumnTypeMetadata_v4 columnTypeMetadata_v4 = columnTypeInfoSet.get(key);
            if (columnTypeMetadata_v4 == null) {
              columnTypeMetadata_v4 = subTableColumnTypeInfo.get(key);
            } else {
              // If the existing total null count or the null count of the child file is unknown(-1), update the total null count
              // as unknown
              if (subTableColumnTypeInfo.get(key).totalNullCount < 0 || columnTypeMetadata_v4.totalNullCount < 0) {
                columnTypeMetadata_v4.totalNullCount = NULL_COUNT_NOT_EXISTS;
              } else {
                columnTypeMetadata_v4.totalNullCount = columnTypeMetadata_v4.totalNullCount + subTableColumnTypeInfo.get(key).totalNullCount;
              }
            }
            columnTypeInfoSet.put(key, columnTypeMetadata_v4);
          }
        }
        dirTotalRowCount = dirTotalRowCount + subTableMetadata.getTotalRowCount();
      } else {
        childFiles.put(file, fs);
      }
    }
    Metadata_V4.MetadataSummary metadataSummary = new Metadata_V4.MetadataSummary(SUPPORTED_VERSIONS.last().toString(),
      DrillVersionInfo.getVersion(), allColumnsInteresting || columnSet == null);
    ParquetTableMetadata_v4 parquetTableMetadata = new ParquetTableMetadata_v4(metadataSummary);
    if (childFiles.size() > 0) {
      List<ParquetFileAndRowCountMetadata> childFileAndRowCountMetadata = getParquetFileMetadata_v4(parquetTableMetadata, childFiles, allColumnsInteresting, columnSet);
      // If the columnTypeInfoSet is empty, add the columnTypeInfo from the parquetTableMetadata
      if (columnTypeInfoSet.isEmpty()) {
        columnTypeInfoSet.putAll(parquetTableMetadata.getColumnTypeInfoMap());
      }
      for (ParquetFileAndRowCountMetadata parquetFileAndRowCountMetadata : childFileAndRowCountMetadata) {
        metaDataList.add(parquetFileAndRowCountMetadata.getFileMetadata());
        dirTotalRowCount = dirTotalRowCount + parquetFileAndRowCountMetadata.getFileRowCount();
        Map<ColumnTypeMetadata_v4.Key, Long> totalNullCountMap = parquetFileAndRowCountMetadata.getTotalNullCountMap();
        for (ColumnTypeMetadata_v4.Key column: totalNullCountMap.keySet()) {
          ColumnTypeMetadata_v4 columnTypeMetadata_v4 = columnTypeInfoSet.get(column);
          // If the column is not present in columnTypeInfoSet, get it from parquetTableMetadata
          if (columnTypeMetadata_v4 == null) {
            columnTypeMetadata_v4 = parquetTableMetadata.getColumnTypeInfoMap().get(column);
          }
          // If the existing total null count or the null count of the child file is unknown(-1), update the total null count
          // as unknown
          if ( columnTypeMetadata_v4.totalNullCount < 0 || totalNullCountMap.get(column) < 0) {
            columnTypeMetadata_v4.totalNullCount = NULL_COUNT_NOT_EXISTS;
          } else {
            columnTypeMetadata_v4.totalNullCount += totalNullCountMap.get(column);
          }
          columnTypeInfoSet.put(column, columnTypeMetadata_v4);
        }
      }
    }

    metadataSummary.directories = directoryList;
    parquetTableMetadata.assignFiles(metaDataList);
    // TODO: We need a merge method that merges two columns with the same name but different types
    if (metadataSummary.columnTypeInfo == null) {
      metadataSummary.columnTypeInfo = new ConcurrentHashMap<>();
    }
    metadataSummary.columnTypeInfo.putAll(columnTypeInfoSet);
    metadataSummary.allColumnsInteresting = allColumnsInteresting;
    metadataSummary.totalRowCount = dirTotalRowCount;
    parquetTableMetadata.metadataSummary = metadataSummary;
    for (String oldName : OLD_METADATA_FILENAMES) {
      fs.delete(new Path(path, oldName), false);
    }
    //  relative paths in the metadata are only necessary for meta cache files.
    ParquetTableMetadata_v4 metadataTableWithRelativePaths =
        MetadataPathUtils.createMetadataWithRelativePaths(parquetTableMetadata, path);
    writeFile(metadataTableWithRelativePaths.fileMetadata, new Path(path, METADATA_FILENAME), fs);
    writeFile(metadataTableWithRelativePaths.getSummary(), new Path(path, METADATA_SUMMARY_FILENAME), fs);
    Metadata_V4.MetadataSummary metadataSummaryWithRelativePaths = metadataTableWithRelativePaths.getSummary();
    // Directories list will be empty at the leaf level directories. For sub-directories with both files and directories,
    // only the directories will be included in the list.
    writeFile(new ParquetTableMetadataDirs(metadataSummaryWithRelativePaths.directories),
        new Path(path, METADATA_DIRECTORIES_FILENAME), fs);
    if (timer != null) {
      logger.debug("Creating metadata files recursively took {} ms", timer.elapsed(TimeUnit.MILLISECONDS));
      timer.stop();
    }
    return Pair.of(parquetTableMetadata, new ParquetTableMetadataDirs(directoryList));
  }

  /**
   * Get the parquet metadata for the parquet files in a directory.
   *
   * @param path the path of the directory
   * @return metadata object for an entire parquet directory structure
   * @throws IOException in case of problems during accessing files
   */
  private ParquetTableMetadata_v4 getParquetTableMetadata(Path path, FileSystem fs) throws IOException {
    FileStatus fileStatus = fs.getFileStatus(path);
    Stopwatch watch = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    List<FileStatus> fileStatuses = new ArrayList<>();
    if (fileStatus.isFile()) {
      fileStatuses.add(fileStatus);
    } else {
      // the thing we need!?
      fileStatuses.addAll(DrillFileSystemUtil.listFiles(fs, path, true));
    }
    if (watch != null) {
      logger.debug("Took {} ms to get file statuses", watch.elapsed(TimeUnit.MILLISECONDS));
      watch.reset();
      watch.start();
    }

    Map<FileStatus, FileSystem> fileStatusMap = fileStatuses.stream()
        .collect(
            java.util.stream.Collectors.toMap(
                Function.identity(),
                s -> fs,
                (oldFs, newFs) -> newFs,
                LinkedHashMap::new));

    ParquetTableMetadata_v4 metadata_v4 = getParquetTableMetadata(fileStatusMap);
    if (watch != null) {
      logger.debug("Took {} ms to read file metadata", watch.elapsed(TimeUnit.MILLISECONDS));
      watch.stop();
    }
    return metadata_v4;
  }

  /**
   * Get the parquet metadata for a list of parquet files
   *
   * @param fileStatusMap file statuses and corresponding file systems
   * @return parquet table metadata object
   * @throws IOException if parquet file metadata can't be obtained
   */
  private ParquetTableMetadata_v4 getParquetTableMetadata(Map<FileStatus, FileSystem> fileStatusMap)
      throws IOException {
    Metadata_V4.MetadataSummary tableMetadataSummary = new Metadata_V4.MetadataSummary(SUPPORTED_VERSIONS.last().toString(), DrillVersionInfo.getVersion(), new ArrayList<>(), true);
    ParquetTableMetadata_v4 tableMetadata = new ParquetTableMetadata_v4(tableMetadataSummary);
    List<ParquetFileAndRowCountMetadata> parquetFileAndRowCountMetadata = getParquetFileMetadata_v4(tableMetadata, fileStatusMap, true, null);
    List<ParquetFileMetadata_v4> parquetFileMetadata = new ArrayList<>();
    for (ParquetFileAndRowCountMetadata fileAndGlobalMetadata : parquetFileAndRowCountMetadata) {
      parquetFileMetadata.add(fileAndGlobalMetadata.getFileMetadata());
    }
    tableMetadata.assignFiles(parquetFileMetadata);
    return tableMetadata;
  }

  /**
   * Get a list of file metadata for a list of parquet files
   *
   * @param parquetTableMetadata_v4 can store column schema info from all the files and row groups
   * @param fileStatusMap parquet files statuses and corresponding file systems
   *
   * @param allColumnsInteresting if set, store column metadata for all the columns
   * @param columnSet Set of columns for which column metadata has to be stored
   * @return list of the parquet file metadata with absolute paths
   * @throws IOException is thrown in case of issues while executing the list of runnables
   */
  private List<ParquetFileAndRowCountMetadata> getParquetFileMetadata_v4(ParquetTableMetadata_v4 parquetTableMetadata_v4,
      Map<FileStatus, FileSystem> fileStatusMap, boolean allColumnsInteresting, Set<SchemaPath> columnSet) throws IOException {
    return TimedCallable.run("Fetch parquet metadata", logger,
        Collectors.toList(fileStatusMap,
            (fileStatus, fileSystem) -> new MetadataGatherer(parquetTableMetadata_v4, fileStatus, fileSystem, allColumnsInteresting, columnSet)),
        16
    );
  }

  /**
   * TimedRunnable that reads the footer from parquet and collects file metadata
   */
  private class MetadataGatherer extends TimedCallable<ParquetFileAndRowCountMetadata> {

    private final ParquetTableMetadata_v4 parquetTableMetadata;
    private final FileStatus fileStatus;
    private final FileSystem fs;
    private final boolean allColumnsInteresting;
    private final Set<SchemaPath> columnSet;

    MetadataGatherer(ParquetTableMetadata_v4 parquetTableMetadata, FileStatus fileStatus, FileSystem fs,
        boolean allColumnsInteresting, Set<SchemaPath> columnSet) {
      this.parquetTableMetadata = parquetTableMetadata;
      this.fileStatus = fileStatus;
      this.fs = fs;
      this.allColumnsInteresting = allColumnsInteresting;
      this.columnSet = columnSet;
    }

    @Override
    protected ParquetFileAndRowCountMetadata runInner() throws Exception {
      return getParquetFileMetadata_v4(parquetTableMetadata, fileStatus, fs, allColumnsInteresting, columnSet, readerConfig);
    }

    public String toString() {
      return new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("path", fileStatus.getPath()).toString();
    }
  }

  // A private version of the following static method, with no footer given
  private ParquetFileAndRowCountMetadata getParquetFileMetadata_v4(ParquetTableMetadata_v4 parquetTableMetadata,
      FileStatus file, final FileSystem fs, boolean allColumnsInteresting, Set<SchemaPath> columnSet, ParquetReaderConfig readerConfig)
      throws IOException, InterruptedException {
    return getParquetFileMetadata_v4(parquetTableMetadata, null /* no footer */,
        file, fs, allColumnsInteresting, false, columnSet, readerConfig);
  }
  /**
   * Get the file metadata for a single file
   *
   * @param parquetTableMetadata The table metadata to be updated with all the columns' info
   * @param footer If non null, use this footer instead of reading it from the file
   * @param file The file
   * @param allColumnsInteresting If true, read the min/max metadata for all the columns
   * @param skipNonInteresting If true, collect info only for the interesting columns
   * @param columnSet Specifies specific columns for which min/max metadata is collected
   * @param readerConfig for the options
   * @return the file metadata
   */
  public static ParquetFileAndRowCountMetadata getParquetFileMetadata_v4(ParquetTableMetadata_v4 parquetTableMetadata,
                                                                         ParquetMetadata footer,
                                                                         FileStatus file,
                                                                         FileSystem fs,
                                                                         boolean allColumnsInteresting,
                                                                         boolean skipNonInteresting,
                                                                         Set<SchemaPath> columnSet,
                                                                         ParquetReaderConfig readerConfig)
    throws IOException, InterruptedException {
    ParquetMetadata metadata = footer; // if a non-null footer is given, no need to read it again from the file
    if (metadata == null) {
      UserGroupInformation processUserUgi = ImpersonationUtil.getProcessUserUGI();
      Configuration conf = new Configuration(fs.getConf());
      try {
        metadata = processUserUgi.doAs((PrivilegedExceptionAction<ParquetMetadata>) () -> {
          try (ParquetFileReader parquetFileReader = ParquetFileReader.open(HadoopInputFile.fromStatus(file, conf), readerConfig.toReadOptions())) {
            return parquetFileReader.getFooter();
          }
        });
      } catch (Exception e) {
        logger.error("Exception while reading footer of parquet file [Details - path: {}, owner: {}] as process user {}",
          file.getPath(), file.getOwner(), processUserUgi.getShortUserName(), e);
        throw e;
      }
    }

    FileMetadataCollector metadataCollector = new FileMetadataCollector(metadata, file, fs, allColumnsInteresting,
      skipNonInteresting, columnSet, readerConfig);

    parquetTableMetadata.metadataSummary.columnTypeInfo.putAll(metadataCollector.getColumnTypeInfo());
    return metadataCollector.getFileMetadata();
  }

  /**
   * Serialize parquet metadata to json and write to a file.
   *
   * @param parquetMetadata parquet table or directory metadata
   * @param p file path
   * @param fs Drill file system
   * @throws IOException if metadata can't be serialized
   */
  private void writeFile(Object parquetMetadata, Path p, FileSystem fs) throws IOException {
    JsonFactory jsonFactory = new JsonFactory();
    jsonFactory.configure(Feature.AUTO_CLOSE_TARGET, false);
    jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    ObjectMapper mapper = new ObjectMapper(jsonFactory);
    SimpleModule module = new SimpleModule();
    module.addSerializer(Path.class, new PathSerDe.Se());
    if (parquetMetadata instanceof Metadata_V4.FileMetadata) {
      module.addSerializer(ColumnMetadata_v4.class, new ColumnMetadata_v4.Serializer());
    }
    mapper.registerModule(module);
    OutputStream os = fs.create(p);
    mapper.writerWithDefaultPrettyPrinter().writeValue(os, parquetMetadata);
    os.flush();
    os.close();
  }

  /**
   * Read the parquet metadata from a file
   *
   * @param path to metadata file
   * @param dirsOnly true for {@link Metadata#METADATA_DIRECTORIES_FILENAME}
   *                 or false for {@link Metadata#OLD_METADATA_FILENAME} files reading
   * @param metaContext current metadata context
   */
  private void readBlockMeta(Path path, boolean dirsOnly, MetadataContext metaContext, FileSystem fs) {
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    Path metadataParentDir = Path.getPathWithoutSchemeAndAuthority(path.getParent());
    String metadataParentDirPath = metadataParentDir.toUri().getPath();
    ObjectMapper mapper = new ObjectMapper();

    final SimpleModule serialModule = new SimpleModule();
    serialModule.addDeserializer(SchemaPath.class, new SchemaPath.De());
    serialModule.addKeyDeserializer(Metadata_V2.ColumnTypeMetadata_v2.Key.class, new Metadata_V2.ColumnTypeMetadata_v2.Key.DeSerializer());
    serialModule.addKeyDeserializer(Metadata_V3.ColumnTypeMetadata_v3.Key.class, new Metadata_V3.ColumnTypeMetadata_v3.Key.DeSerializer());
    serialModule.addKeyDeserializer(ColumnTypeMetadata_v4.Key.class, new ColumnTypeMetadata_v4.Key.DeSerializer());

    AfterburnerModule module = new AfterburnerModule();
    module.setUseOptimizedBeanDeserializer(true);

    boolean isFileMetadata = path.toString().endsWith(METADATA_FILENAME);
    boolean isSummaryFile = path.toString().endsWith(METADATA_SUMMARY_FILENAME);
    mapper.registerModule(serialModule);
    mapper.registerModule(module);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try (InputStream is = fs.open(path)) {
      boolean alreadyCheckedModification;
      boolean newMetadata = false;
      alreadyCheckedModification = metaContext.getStatus(metadataParentDir);

      if (dirsOnly) {
        parquetTableMetadataDirs = mapper.readValue(is, ParquetTableMetadataDirs.class);
        if (timer != null) {
          logger.debug("Took {} ms to read directories from directory cache file", timer.elapsed(TimeUnit.MILLISECONDS));
          timer.stop();
        }
        parquetTableMetadataDirs.updateRelativePaths(metadataParentDirPath);
        if (!alreadyCheckedModification && tableModified(parquetTableMetadataDirs.getDirectories(), path, metadataParentDir, metaContext, fs)) {
          parquetTableMetadataDirs =
              (createMetaFilesRecursivelyAsProcessUser(Path.getPathWithoutSchemeAndAuthority(path.getParent()), fs, true, null, true)).getRight();
          newMetadata = true;
        }
      } else {
        if (isFileMetadata) {
          parquetTableMetadata.assignFiles((mapper.readValue(is, FileMetadata.class)).getFiles());
          if (new MetadataVersion(parquetTableMetadata.getMetadataVersion()).isAtLeast(4, 0)) {
            ((ParquetTableMetadata_v4) parquetTableMetadata).updateRelativePaths(metadataParentDirPath);
          }

          if (!alreadyCheckedModification && tableModified(parquetTableMetadata.getDirectories(), path, metadataParentDir, metaContext, fs)) {
            parquetTableMetadata =
                    (createMetaFilesRecursivelyAsProcessUser(Path.getPathWithoutSchemeAndAuthority(path.getParent()), fs, true, null, true)).getLeft();
            newMetadata = true;
          }
        } else if (isSummaryFile) {
          MetadataSummary metadataSummary = mapper.readValue(is, Metadata_V4.MetadataSummary.class);
          parquetTableMetadata = new ParquetTableMetadata_v4(metadataSummary);
        } else {
          parquetTableMetadata = mapper.readValue(is, ParquetTableMetadataBase.class);
          if (new MetadataVersion(parquetTableMetadata.getMetadataVersion()).isAtLeast(3, 0)) {
            ((Metadata_V3.ParquetTableMetadata_v3) parquetTableMetadata).updateRelativePaths(metadataParentDirPath);
          }
          if (!alreadyCheckedModification && tableModified((parquetTableMetadata.getDirectories()), path, metadataParentDir, metaContext, fs)) {
            parquetTableMetadata =
                    (createMetaFilesRecursivelyAsProcessUser(Path.getPathWithoutSchemeAndAuthority(path.getParent()), fs, true, null, true)).getLeft();
            newMetadata = true;
          }
        }
        if (timer != null) {
          logger.debug("Took {} ms to read metadata from cache file", timer.elapsed(TimeUnit.MILLISECONDS));
          timer.stop();
        }
        if (!isSummaryFile) {
          List<? extends ParquetFileMetadata> files = parquetTableMetadata.getFiles();
          if (files != null) {
            for (ParquetFileMetadata file : files) {
              // DRILL-5009: Remove empty row groups unless it is the only row group
              List<? extends RowGroupMetadata> rowGroups = file.getRowGroups();
              if (rowGroups.size() == 1) {
                continue;
              }
              rowGroups.removeIf(r -> r.getRowCount() == 0);
            }
          }
        }
        if (newMetadata) {
          // if new metadata files were created, invalidate the existing metadata context
          metaContext.clear();
        }
      }
    } catch (IOException e) {
      logger.error("Failed to read '{}' metadata file", path, e);
      metaContext.setMetadataCacheCorrupted(true);
    }
  }

  private Set<SchemaPath> getInterestingColumns(FileSystem fs, Path metadataParentDir, boolean autoRefreshTriggered) {
    Metadata_V4.MetadataSummary metadataSummary = getSummary(fs, metadataParentDir, autoRefreshTriggered, null);
    if (metadataSummary == null) {
      return null;
    } else {
      Set<SchemaPath> interestingColumns = new HashSet<>();
      for (ColumnTypeMetadata_v4 columnTypeMetadata_v4: metadataSummary.columnTypeInfo.values()) {
        if (columnTypeMetadata_v4.isInteresting) {
          interestingColumns.add(SchemaPath.getSimplePath(SchemaPath.getCompoundPath(columnTypeMetadata_v4.name).getRootSegmentPath()));
        }
      }
      return interestingColumns;
    }
  }

  private boolean isAllColumnsInteresting(FileSystem fs, Path metadataParentDir, boolean autoRefreshTriggered) {
    Metadata_V4.MetadataSummary metadataSummary = getSummary(fs, metadataParentDir, autoRefreshTriggered, null);
    if (metadataSummary == null) {
      return true;
    }
    return metadataSummary.isAllColumnsInteresting();
  }

  public static Path getSummaryFileName(Path metadataParentDir) {
    return new Path(metadataParentDir, METADATA_SUMMARY_FILENAME);
  }

  public static Path getDirFileName(Path metadataParentDir) {
    return new Path(metadataParentDir, METADATA_DIRECTORIES_FILENAME);
  }

  private static Path getFileMetadataFileName(Path metadataParentDir) {
    return new Path(metadataParentDir, METADATA_FILENAME);
  }

  /**
   * Returns if metadata exists or not in that directory
   * @param fs file system
   * @param metadataParentDir parent directory that holds metadata files
   * @return true if metadata exists in that directory
   * @throws IOException when unable to check metadata exists status
   */
  private static boolean metadataExists(FileSystem fs, Path metadataParentDir) throws IOException {
    Path summaryFile = new Path(metadataParentDir, METADATA_SUMMARY_FILENAME);
    Path metadataDirFile = new Path(metadataParentDir, METADATA_DIRECTORIES_FILENAME);
    Path fileMetadataFile = new Path(metadataParentDir, METADATA_FILENAME);
    if (!fs.exists(summaryFile) && !fs.exists(metadataDirFile) && !fs.exists(fileMetadataFile)) {
      return false;
    }
    return true;
  }

  /**
   * Reads the summary from the metadata cache file, if the cache file is stale recreates the metadata
   * @param fs file system
   * @param metadataParentDir parent directory that holds metadata files
   * @param autoRefreshTriggered true if the auto-refresh is already triggered
   * @param readerConfig Parquet reader config
   * @return returns metadata summary
   */
  public static Metadata_V4.MetadataSummary getSummary(FileSystem fs, Path metadataParentDir, boolean autoRefreshTriggered, ParquetReaderConfig readerConfig) {
    Path summaryFile = getSummaryFileName(metadataParentDir);
    Path metadataDirFile = getDirFileName(metadataParentDir);
    MetadataContext metaContext = new MetadataContext();
    try {
      // If autoRefresh is not triggered and none of the metadata files exist
      if (!autoRefreshTriggered && !metadataExists(fs, metadataParentDir)) {
        logger.debug("Metadata doesn't exist in {}", metadataParentDir);
        return null;
      } else if (autoRefreshTriggered && !fs.exists(summaryFile)) {
        logger.debug("Metadata Summary file {} does not exist", summaryFile);
        return null;
      } else {
        // If the autorefresh is not triggered, check if the cache file is stale and trigger auto-refresh
        if (!autoRefreshTriggered) {
          Metadata metadata = new Metadata(readerConfig);
          if (!fs.exists(metadataDirFile)) {
            return null;
          }
          ParquetTableMetadataDirs metadataDirs  = readMetadataDirs(fs, metadataDirFile, metaContext, readerConfig);
          if (metadata.tableModified(metadataDirs.getDirectories(), summaryFile, metadataParentDir, metaContext, fs) && true) {
            ParquetTableMetadata_v4 parquetTableMetadata = (metadata.createMetaFilesRecursivelyAsProcessUser(Path.getPathWithoutSchemeAndAuthority(summaryFile.getParent()), fs, true, null, true)).getLeft();
            return parquetTableMetadata.getSummary();
          }
        }
        // Read the existing metadataSummary cache file to get the metadataSummary
        ObjectMapper mapper = new ObjectMapper();
        final SimpleModule serialModule = new SimpleModule();
        serialModule.addDeserializer(SchemaPath.class, new SchemaPath.De());
        serialModule.addKeyDeserializer(ColumnTypeMetadata_v4.Key.class, new ColumnTypeMetadata_v4.Key.DeSerializer());
        AfterburnerModule module = new AfterburnerModule();
        module.setUseOptimizedBeanDeserializer(true);
        mapper.registerModule(serialModule);
        mapper.registerModule(module);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InputStream is = fs.open(summaryFile);
        return mapper.readValue(is, Metadata_V4.MetadataSummary.class);
        }
    } catch (IOException e) {
      logger.debug("Failed to read '{}' summary metadata file", summaryFile, e);
      return null;
    }
  }

  /**
   * Check if the parquet metadata needs to be updated by comparing the modification time of the directories with
   * the modification time of the metadata file
   *
   * @param directories List of directories
   * @param metaFilePath path of parquet metadata cache file
   * @return true if metadata needs to be updated, false otherwise
   * @throws IOException if some resources are not accessible
   */
  private boolean tableModified(List<Path> directories, Path metaFilePath, Path parentDir,
                                MetadataContext metaContext, FileSystem fs) throws IOException {
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    metaContext.setStatus(parentDir);
    long metaFileModifyTime = fs.getFileStatus(metaFilePath).getModificationTime();
    FileStatus directoryStatus = fs.getFileStatus(parentDir);
    int numDirs = 1;
    if (directoryStatus.getModificationTime() > metaFileModifyTime) {
      return logAndStopTimer(true, directoryStatus.getPath().toString(), timer, numDirs);
    }
    boolean isModified = false;
    for (Path directory : directories) {
      numDirs++;
      metaContext.setStatus(directory);
      directoryStatus = fs.getFileStatus(directory);
      if (directoryStatus.getModificationTime() > metaFileModifyTime) {
        isModified = true;
        break;
      }
    }
    return logAndStopTimer(isModified, directoryStatus.getPath().toString(), timer, numDirs);
  }

  private boolean logAndStopTimer(boolean isModified, String directoryName,
                                  Stopwatch timer, int numDirectories) {
    if (timer != null) {
      logger.debug("{} directory was modified. Took {} ms to check modification time of {} directories",
        isModified ? directoryName : "No", timer.elapsed(TimeUnit.MILLISECONDS), numDirectories);
      timer.stop();
    }
    return isModified;
  }
}
