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
package org.apache.drill.exec.metastore.store.parquet;

import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.MetadataContext;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.parquet.BaseParquetMetadataProvider;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.store.parquet.metadata.Metadata;
import org.apache.drill.exec.store.parquet.metadata.MetadataBase;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParquetTableMetadataProviderImpl extends BaseParquetMetadataProvider
    implements ParquetTableMetadataProvider {
  private static final Logger logger =
      LoggerFactory.getLogger(ParquetTableMetadataProviderImpl.class);

  private final DrillFileSystem fs;
  private final MetadataContext metaContext;
  // may change when filter push down / partition pruning is applied
  private Path selectionRoot;
  private Path cacheFileRoot;
  private final boolean corruptDatesAutoCorrected;
  private boolean usedMetadataCache; // false by default

  private ParquetTableMetadataProviderImpl(Builder builder) throws IOException {
    super(builder);

    BaseParquetMetadataProvider source = (BaseParquetMetadataProvider) builder.metadataProviderManager().getTableMetadataProvider();

    this.fs = builder.fs;
    if (builder.entries() != null) {
      // reuse previously stored metadata
      this.selectionRoot = builder.selectionRoot();
      this.cacheFileRoot = builder.cacheFileRoot;
      this.metaContext = new MetadataContext();

      this.corruptDatesAutoCorrected = builder.autoCorrectCorruptedDates;

      init(source);
    } else {
      this.selectionRoot = builder.selection().getSelectionRoot();
      this.cacheFileRoot = builder.selection().getCacheFileRoot();

      MetadataContext metadataContext = builder.selection().getMetaContext();
      this.metaContext = metadataContext != null ? metadataContext : new MetadataContext();

      this.corruptDatesAutoCorrected = builder.autoCorrectCorruptedDates;

      FileSelection fileSelection = expandIfNecessary(builder.selection());
      if (fileSelection != null) {
        if (checkForInitializingEntriesWithSelectionRoot()) {
          // The fully expanded list is already stored as part of the fileSet
          entries.add(new ReadEntryWithPath(fileSelection.getSelectionRoot()));
        } else {
          for (Path fileName : fileSelection.getFiles()) {
            entries.add(new ReadEntryWithPath(fileName));
          }
        }
        init(source);
      }
    }
    // store results into MetadataProviderManager to be able to use them when creating new instances
    if (source == null || source.getRowGroupsMeta().size() < getRowGroupsMeta().size()) {
      builder.metadataProviderManager().setTableMetadataProvider(this);
    }
  }

  @Override
  public boolean isUsedMetadataCache() {
    return usedMetadataCache;
  }

  @Override
  public Path getSelectionRoot() {
    return selectionRoot;
  }

  /**
   * Returns list of metadata cache files
   * @param p directory path of the cache file
   * @param fs filesystem object
   * @return list of cache files found in the given directory path
   */
  public List<Path> populateMetaPaths(Path p, DrillFileSystem fs) throws IOException {
    if (fs.isDirectory(p)) {
      List<Path> metaFilepaths = Arrays.stream(Metadata.CURRENT_METADATA_FILENAMES)
          .map(filename -> new Path(p, filename))
          .collect(Collectors.toList());
      for (String filename : Metadata.OLD_METADATA_FILENAMES) {
        // Read the older version of metadata file if the current version of metadata cache files does not exist.
        if (fileExists(fs, metaFilepaths)) {
          return metaFilepaths;
        }
        metaFilepaths.clear();
        metaFilepaths.add(new Path(p, filename));
      }
      if (fileExists(fs, metaFilepaths)) {
        return metaFilepaths;
      }
    }
    return Collections.emptyList();
  }

  public boolean fileExists(DrillFileSystem fs, List<Path> paths) throws IOException {
    for (Path path : paths) {
      if (!fs.exists(path)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void initInternal() throws IOException {
    try (FileSystem processUserFileSystem = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), fs.getConf())) {
      // Depending on the version of metadata this may represent more than 1 metadata file paths.
      List<Path> metaPaths = new ArrayList<>();
      if (entries.size() == 1 && parquetTableMetadata == null) {
        Path p = Path.getPathWithoutSchemeAndAuthority(entries.get(0).getPath());
        if (fs.isDirectory(p)) {
          // Using the metadata file makes sense when querying a directory; otherwise
          // if querying a single file we can look up the metadata directly from the file
          metaPaths = populateMetaPaths(p, fs);
        }
        if (!metaContext.isMetadataCacheCorrupted() && !metaPaths.isEmpty()) {
          parquetTableMetadata = Metadata.readBlockMeta(processUserFileSystem, metaPaths, metaContext, readerConfig);
          if (parquetTableMetadata != null) {
            usedMetadataCache = true;
          }
        }
        if (!usedMetadataCache) {
          parquetTableMetadata = Metadata.getParquetTableMetadata(processUserFileSystem, p, readerConfig);
        }
      } else {
        Path p = Path.getPathWithoutSchemeAndAuthority(selectionRoot);
        metaPaths = populateMetaPaths(p, fs);
        if (!metaContext.isMetadataCacheCorrupted() && fs.isDirectory(selectionRoot) && !metaPaths.isEmpty()) {
          if (parquetTableMetadata == null) {
            parquetTableMetadata = Metadata.readBlockMeta(processUserFileSystem, metaPaths, metaContext, readerConfig);
          }
          if (parquetTableMetadata != null) {
            usedMetadataCache = true;
            if (fileSet != null) {
              parquetTableMetadata = removeUnneededRowGroups(parquetTableMetadata);
            }
          }
        }
        if (!usedMetadataCache) {
          final List<FileStatus> fileStatuses = new ArrayList<>();
          for (ReadEntryWithPath entry : entries) {
            fileStatuses.addAll(
                DrillFileSystemUtil.listFiles(fs, Path.getPathWithoutSchemeAndAuthority(entry.getPath()), true));
          }

          Map<FileStatus, FileSystem> statusMap = fileStatuses.stream()
              .collect(
                Collectors.toMap(
                  Function.identity(),
                  s -> processUserFileSystem,
                  (oldFs, newFs) -> newFs,
                  LinkedHashMap::new));

          parquetTableMetadata = Metadata.getParquetTableMetadata(statusMap, readerConfig);
        }
      }
    }
  }

  // private methods block start
  /**
   * Expands the selection's folders if metadata cache is found for the selection root.<br>
   * If the selection has already been expanded or no metadata cache was found, does nothing
   *
   * @param selection actual selection before expansion
   * @return new selection after expansion, if no expansion was done returns the input selection
   */
  private FileSelection expandIfNecessary(FileSelection selection) throws IOException {
    if (selection.isExpandedFully()) {
      return selection;
    }

    // use the cacheFileRoot if provided (e.g after partition pruning)
    Path path = cacheFileRoot != null ? cacheFileRoot : selectionRoot;
    // Depending on the version of metadata this may represent more than 1 metadata file paths.
    List<Path> metaPaths = populateMetaPaths(path, fs);
    if (metaPaths.isEmpty()) { // no metadata cache
      if (selection.isExpandedPartial()) {
        logger.error("'{}' metadata file/files does not exist, but metadata directories cache file is present", metaPaths.toString());
        metaContext.setMetadataCacheCorrupted(true);
      }

      return selection;
    }

    return expandSelectionFromMetadataCache(selection, metaPaths);
  }

  /**
   * For two cases the entries should be initialized with just the selection root instead of the fully expanded list:
   * <ul>
   *   <li> When metadata caching is corrupted (to use correct file selection)
   *   <li> Metadata caching is correct and used, but pruning was not applicable or was attempted and nothing was pruned
   *        (to reduce overhead in parquet group scan).
   * </ul>
   *
   * @return true if entries should be initialized with selection root, false otherwise
   */
  private boolean checkForInitializingEntriesWithSelectionRoot() {
    return metaContext.isMetadataCacheCorrupted() || (parquetTableMetadata != null &&
        (metaContext.getPruneStatus() == MetadataContext.PruneStatus.NOT_STARTED || metaContext.getPruneStatus() == MetadataContext.PruneStatus.NOT_PRUNED));
  }

  /**
   * Create and return a new file selection based on reading the metadata cache file.
   *
   * This function also initializes a few of ParquetGroupScan's fields as appropriate.
   *
   * @param selection initial file selection
   * @param metaFilePaths metadata cache file path
   * @return file selection read from cache
   *
   * @throws org.apache.drill.common.exceptions.UserException when the updated selection is empty, this happens if the user selects an empty folder.
   */
  private FileSelection expandSelectionFromMetadataCache(FileSelection selection, List<Path> metaFilePaths) throws IOException {
    // get the metadata for the root directory by reading the metadata file
    // parquetTableMetadata contains the metadata for all files in the selection root folder, but we need to make sure
    // we only select the files that are part of selection (by setting fileSet appropriately)

    // get (and set internal field) the metadata for the directory by reading the metadata file
    FileSystem processUserFileSystem = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), fs.getConf());
    parquetTableMetadata = Metadata.readBlockMeta(processUserFileSystem, metaFilePaths, metaContext, readerConfig);
    if (ignoreExpandingSelection(parquetTableMetadata)) {
      return selection;
    }
    if (corruptDatesAutoCorrected) {
      ParquetReaderUtility.correctDatesInMetadataCache(this.parquetTableMetadata);
    }
    ParquetReaderUtility.transformBinaryInMetadataCache(parquetTableMetadata, readerConfig);
    List<FileStatus> fileStatuses = selection.getStatuses(fs);

    if (fileSet == null) {
      fileSet = new HashSet<>();
    }

    final Path first = fileStatuses.get(0).getPath();
    if (fileStatuses.size() == 1 && selection.getSelectionRoot().equals(first)) {
      // we are selecting all files from selection root. Expand the file list from the cache
      for (MetadataBase.ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
        fileSet.add(file.getPath());
      }

    } else if (selection.isExpandedPartial() && !selection.hadWildcard() && cacheFileRoot != null) {
      if (selection.wasAllPartitionsPruned()) {
        // if all partitions were previously pruned, we only need to read 1 file (for the schema)
        fileSet.add(this.parquetTableMetadata.getFiles().get(0).getPath());
      } else {
        // we are here if the selection is in the expanded_partial state (i.e it has directories).  We get the
        // list of files from the metadata cache file that is present in the cacheFileRoot directory and populate
        // the fileSet. However, this is *not* the final list of files that will be scanned in execution since the
        // second phase of partition pruning will apply on the files and modify the file selection appropriately.
        for (MetadataBase.ParquetFileMetadata file : this.parquetTableMetadata.getFiles()) {
          fileSet.add(file.getPath());
        }
      }
    } else {
      // we need to expand the files from fileStatuses
      for (FileStatus status : fileStatuses) {
        Path currentCacheFileRoot = status.getPath();
        if (status.isDirectory()) {
          // TODO [DRILL-4496] read the metadata cache files in parallel
          // Depending on the version of metadata this may represent more than 1 metadata file paths.
          List<Path> metaPaths = populateMetaPaths(currentCacheFileRoot, fs);
          MetadataBase.ParquetTableMetadataBase metadata = Metadata.readBlockMeta(processUserFileSystem, metaPaths, metaContext, readerConfig);
          if (ignoreExpandingSelection(metadata)) {
            return selection;
          }
          for (MetadataBase.ParquetFileMetadata file : metadata.getFiles()) {
            fileSet.add(file.getPath());
          }
        } else {
          final Path path = Path.getPathWithoutSchemeAndAuthority(currentCacheFileRoot);
          fileSet.add(path);
        }
      }
    }

    if (fileSet.isEmpty()) {
      // no files were found, most likely we tried to query some empty sub folders
      logger.warn("The table is empty but with outdated invalid metadata cache files. Please, delete them.");
      return null;
    }

    List<Path> fileNames = new ArrayList<>(fileSet);

    // when creating the file selection, set the selection root without the URI prefix
    // The reason is that the file names above have been created in the form
    // /a/b/c.parquet and the format of the selection root must match that of the file names
    // otherwise downstream operations such as partition pruning can break.
    Path metaRootPath = Path.getPathWithoutSchemeAndAuthority(selection.getSelectionRoot());
    this.selectionRoot = metaRootPath;

    // Use the FileSelection constructor directly here instead of the FileSelection.create() method
    // because create() changes the root to include the scheme and authority; In future, if create()
    // is the preferred way to instantiate a file selection, we may need to do something different...
    // WARNING: file statuses and file names are inconsistent
    FileSelection newSelection = new FileSelection(selection.getStatuses(fs), fileNames, metaRootPath, cacheFileRoot,
        selection.wasAllPartitionsPruned());

    newSelection.setExpandedFully();
    newSelection.setMetaContext(metaContext);
    return newSelection;
  }

  private MetadataBase.ParquetTableMetadataBase removeUnneededRowGroups(MetadataBase.ParquetTableMetadataBase parquetTableMetadata) {
    List<MetadataBase.ParquetFileMetadata> newFileMetadataList = new ArrayList<>();
    for (MetadataBase.ParquetFileMetadata file : parquetTableMetadata.getFiles()) {
      if (fileSet.contains(file.getPath())) {
        newFileMetadataList.add(file);
      }
    }

    MetadataBase.ParquetTableMetadataBase metadata = parquetTableMetadata.clone();
    metadata.assignFiles(newFileMetadataList);
    return metadata;
  }

  /**
   * If metadata is corrupted, ignore expanding selection and reset parquetTableMetadata and fileSet fields
   *
   * @param metadata parquet table metadata
   * @return true if parquet metadata is corrupted, false otherwise
   */
  private boolean ignoreExpandingSelection(MetadataBase.ParquetTableMetadataBase metadata) {
    if (metadata == null || metaContext.isMetadataCacheCorrupted()) {
      logger.debug("Selection can't be expanded since metadata file is corrupted or metadata version is not supported");
      this.parquetTableMetadata = null;
      this.fileSet = null;
      return true;
    }
    return false;
  }

  public static class Builder extends BaseParquetMetadataProvider.Builder<Builder>
      implements ParquetTableMetadataProviderBuilder<Builder> {

    private Path cacheFileRoot;
    private DrillFileSystem fs;
    private boolean autoCorrectCorruptedDates;

    public Builder(MetadataProviderManager source) {
      super(source);
    }

    @Override
    public Builder withCacheFileRoot(Path cacheFileRoot) {
      this.cacheFileRoot = cacheFileRoot;
      return this;
    }

    @Override
    public Builder withFileSystem(DrillFileSystem fs) {
      this.fs = fs;
      return this;
    }

    @Override
    public Builder withCorrectCorruptedDates(boolean autoCorrectCorruptedDates) {
      this.autoCorrectCorruptedDates = autoCorrectCorruptedDates;
      return this;
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public ParquetTableMetadataProvider build() throws IOException {
      return new ParquetTableMetadataProviderImpl(this);
    }
  }
}
