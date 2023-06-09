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

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Jackson serializable description of a file selection.
 */
public class FileSelection implements DrillTableSelection {

  private static final Logger logger = LoggerFactory.getLogger(FileSelection.class);
  private static final String WILD_CARD = "*";

  private List<FileStatus> statuses;

  public List<Path> files;
  /**
   * root path for the selections
   */
  public final Path selectionRoot;
  /**
   * root path for the metadata cache file (if any)
   */
  public final Path cacheFileRoot;

  /**
   * metadata context useful for metadata operations (if any)
   */
  private MetadataContext metaContext = null;

  /**
   * Indicates whether this selectionRoot is an empty directory
   */
  private boolean emptyDirectory;

  private enum StatusType {
    NOT_CHECKED,         // initial state
    NO_DIRS,             // no directories in this selection
    HAS_DIRS,            // directories were found in the selection
    EXPANDED_FULLY,      // whether selection fully expanded to files
    EXPANDED_PARTIAL     // whether selection partially expanded to only directories (not files)
  }

  private StatusType dirStatus;
  // whether this selection previously had a wildcard, false by default
  private boolean hadWildcard;
  // whether all partitions were previously pruned for this selection, false by default
  private boolean wasAllPartitionsPruned;

  /**
   * Creates a {@link FileSelection selection} out of given file statuses/files and selection root.
   *
   * @param statuses  list of file statuses
   * @param files  list of files
   * @param selectionRoot  root path for selections
   */
  public FileSelection(List<FileStatus> statuses, List<Path> files, Path selectionRoot) {
    this(statuses, files, selectionRoot, null, false, StatusType.NOT_CHECKED);
  }

  public FileSelection(List<FileStatus> statuses, List<Path> files, Path selectionRoot, Path cacheFileRoot,
      boolean wasAllPartitionsPruned) {
    this(statuses, files, selectionRoot, cacheFileRoot, wasAllPartitionsPruned, StatusType.NOT_CHECKED);
  }

  public FileSelection(List<FileStatus> statuses, List<Path> files, Path selectionRoot, Path cacheFileRoot,
      boolean wasAllPartitionsPruned, StatusType dirStatus) {
    this.statuses = statuses;
    this.files = files;
    this.selectionRoot = selectionRoot;
    this.dirStatus = dirStatus;
    this.cacheFileRoot = cacheFileRoot;
    this.wasAllPartitionsPruned = wasAllPartitionsPruned;
  }

  /**
   * Copy constructor for convenience.
   */
  protected FileSelection(FileSelection selection) {
    Preconditions.checkNotNull(selection, "File selection cannot be null");
    this.statuses = selection.statuses;
    this.files = selection.files;
    this.selectionRoot = selection.selectionRoot;
    this.dirStatus = selection.dirStatus;
    this.cacheFileRoot = selection.cacheFileRoot;
    this.metaContext = selection.metaContext;
    this.hadWildcard = selection.hadWildcard;
    this.wasAllPartitionsPruned = selection.wasAllPartitionsPruned;
    this.emptyDirectory = selection.emptyDirectory;
  }

  public Path getSelectionRoot() {
    return selectionRoot;
  }

  public List<FileStatus> getStatuses(DrillFileSystem fs) throws IOException {
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;

    if (statuses == null)  {
      List<FileStatus> newStatuses = new ArrayList<>();
      for (Path pathStr : Objects.requireNonNull(files, "Files can not be null if statuses are null")) {
        newStatuses.add(fs.getFileStatus(pathStr));
      }
      statuses = newStatuses;
    }
    if (timer != null) {
      logger.debug("FileSelection.getStatuses() took {} ms, numFiles: {}",
          timer.elapsed(TimeUnit.MILLISECONDS), statuses == null ? 0 : statuses.size());
      timer.stop();
    }

    return statuses;
  }

  public List<Path> getFiles() {
    if (files == null) {
      files = Objects.requireNonNull(statuses, "Statuses can not be null if files are null").stream()
        .map(FileStatus::getPath)
        .collect(Collectors.toList());
    }
    return files;
  }

  public boolean containsDirectories(DrillFileSystem fs) throws IOException {
    if (dirStatus == StatusType.NOT_CHECKED) {
      dirStatus = StatusType.NO_DIRS;
      for (FileStatus status : getStatuses(fs)) {
        if (status.isDirectory()) {
          dirStatus = StatusType.HAS_DIRS;
          break;
        }
      }
    }
    return dirStatus == StatusType.HAS_DIRS;
  }

  public FileSelection minusDirectories(DrillFileSystem fs) throws IOException {
    if (isExpandedFully()) {
      return this;
    }
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    List<FileStatus> statuses = getStatuses(fs);

    List<FileStatus> nonDirectories = Lists.newArrayList();
    for (FileStatus status : statuses) {
      nonDirectories.addAll(DrillFileSystemUtil.listFiles(fs, status.getPath(), true));
    }

    FileSelection fileSel = create(nonDirectories, null, selectionRoot);
    if (timer != null) {
      logger.debug("FileSelection.minusDirectories() took {} ms, numFiles: {}", timer.elapsed(TimeUnit.MILLISECONDS), statuses.size());
      timer.stop();
    }

    // fileSel will be null if we query an empty folder
    if (fileSel != null) {
      fileSel.setExpandedFully();
    }

    return fileSel;
  }

  public FileStatus getFirstPath(DrillFileSystem fs) throws IOException {
    return getStatuses(fs).get(0);
  }

  public void setExpandedFully() {
    this.dirStatus = StatusType.EXPANDED_FULLY;
  }

  public boolean isExpandedFully() {
    return dirStatus == StatusType.EXPANDED_FULLY;
  }

  public void setExpandedPartial() {
    this.dirStatus = StatusType.EXPANDED_PARTIAL;
  }

  public boolean isExpandedPartial() {
    return dirStatus == StatusType.EXPANDED_PARTIAL;
  }

  public StatusType getDirStatus() {
    return dirStatus;
  }

  public boolean wasAllPartitionsPruned() {
    return this.wasAllPartitionsPruned;
  }

  /**
   * Returns longest common path for the given list of files.
   *
   * @param files  list of files.
   * @return  longest common path
   */
  private static Path commonPathForFiles(List<Path> files) {
    if (files == null || files.isEmpty()) {
      return new Path("/");
    }

    int total = files.size();
    String[][] folders = new String[total][];
    int shortest = Integer.MAX_VALUE;
    for (int i = 0; i < total; i++) {
      folders[i] = files.get(i).toUri().getPath().split(Path.SEPARATOR);
      shortest = Math.min(shortest, folders[i].length);
    }

    int latest;
    out:
    for (latest = 0; latest < shortest; latest++) {
      String current = folders[0][latest];
      for (int i = 1; i < folders.length; i++) {
        if (!current.equals(folders[i][latest])) {
          break out;
        }
      }
    }
    URI uri = files.get(0).toUri();
    String pathString = buildPath(folders[0], latest);
    return new Path(uri.getScheme(), uri.getAuthority(), pathString);
  }

  private static String buildPath(String[] path, int folderIndex) {
    StringBuilder builder = new StringBuilder();
    for (int i=0; i<folderIndex; i++) {
      builder.append(path[i]).append(Path.SEPARATOR);
    }
    builder.deleteCharAt(builder.length()-1);
    return builder.toString();
  }

  public static FileSelection create(DrillFileSystem fs, String parent, String path,
      boolean allowAccessOutsideWorkspace) throws IOException {
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    boolean hasWildcard = path.contains(WILD_CARD);

    String child = DrillStringUtils.removeLeadingSlash(path);
    Path combined = new Path(parent, child);
    // Unescape chars escaped with '\' for our root path to be consistent with what
    // fs.globStatus(...) below will do with them, c.f. DRILL-8064
    Path root = new Path(parent, DrillStringUtils.unescapeJava(child));

    if (!allowAccessOutsideWorkspace) {
      checkBackPaths(new Path(parent).toUri().getPath(), combined.toUri().getPath(), path);
    }
    FileStatus[] statuses = fs.globStatus(combined); // note: this will expand wildcards
    if (statuses == null) {
      return null;
    }
    FileSelection fileSel = create(Arrays.asList(statuses), null, root);
    if (timer != null) {
      logger.debug("FileSelection.create() took {} ms ", timer.elapsed(TimeUnit.MILLISECONDS));
      timer.stop();
    }
    if (fileSel == null) {
      return null;
    }
    fileSel.setHadWildcard(hasWildcard);
    return fileSel;

  }

  /**
   * Creates a {@link FileSelection selection} with the given file statuses/files and selection root.
   *
   * @param statuses  list of file statuses
   * @param files  list of files
   * @param root  root path for selections
   * @param cacheFileRoot root path for metadata cache (null for no metadata cache)
   * @return  null if creation of {@link FileSelection} fails with an {@link IllegalArgumentException}
   *          otherwise a new selection.
   *
   * @see FileSelection#FileSelection(List, List, Path)
   */
  public static FileSelection create(List<FileStatus> statuses, List<Path> files, Path root,
      Path cacheFileRoot, boolean wasAllPartitionsPruned) {
    boolean bothNonEmptySelection = (statuses != null && statuses.size() > 0) && (files != null && files.size() > 0);
    boolean bothEmptySelection = (statuses == null || statuses.size() == 0) && (files == null || files.size() == 0);

    if (bothNonEmptySelection || bothEmptySelection) {
      return null;
    }

    Path selectionRoot;
    if (statuses == null || statuses.isEmpty()) {
      selectionRoot = commonPathForFiles(files);
    } else {
      Objects.requireNonNull(root, "Selection root is null");
      Path rootPath = handleWildCard(root);
      URI uri = statuses.get(0).getPath().toUri();
      selectionRoot = new Path(uri.getScheme(), uri.getAuthority(), rootPath.toUri().getPath());
    }
    return new FileSelection(statuses, files, selectionRoot, cacheFileRoot, wasAllPartitionsPruned);
  }

  public static FileSelection create(List<FileStatus> statuses, List<Path> files, Path root) {
    return FileSelection.create(statuses, files, root, null, false);
  }

  public static FileSelection createFromDirectories(List<Path> dirPaths, FileSelection selection,
      Path cacheFileRoot) {
    Stopwatch timer = logger.isDebugEnabled() ? Stopwatch.createStarted() : null;
    Path root = selection.getSelectionRoot();
    Objects.requireNonNull(root, "Selection root is null");
    if (dirPaths == null || dirPaths.isEmpty()) {
      throw new DrillRuntimeException("List of directories is null or empty");
    }

    // for wildcard the directory list should have already been expanded
    List<Path> dirs = selection.hadWildcard() ? selection.getFileStatuses().stream()
        .map(FileStatus::getPath)
        .collect(Collectors.toList()) : new ArrayList<>(dirPaths);

    Path rootPath = handleWildCard(root);
    URI uri = selection.getFileStatuses().get(0).getPath().toUri();
    Path path = new Path(uri.getScheme(), uri.getAuthority(), rootPath.toUri().getPath());
    FileSelection fileSel = new FileSelection(null, dirs, path, cacheFileRoot, false);
    fileSel.setHadWildcard(selection.hadWildcard());
    if (timer != null) {
      logger.debug("FileSelection.createFromDirectories() took {} ms ", timer.elapsed(TimeUnit.MILLISECONDS));
      timer.stop();
    }
    return fileSel;
  }

  private static Path handleWildCard(Path root) {
    String stringRoot = root.toUri().getPath();
    if (stringRoot.contains(WILD_CARD)) {
      int idx = stringRoot.indexOf(WILD_CARD); // first wild card in the path
      idx = stringRoot.lastIndexOf('/', idx); // file separator right before the first wild card
      String newRoot = stringRoot.substring(0, idx);
      return DrillFileSystemUtil.createPathSafe(newRoot);
    } else {
      return new Path(stringRoot);
    }
  }

  /**
   * Check if the path is a valid sub path under the parent after removing backpaths. Throw an exception if
   * it is not. We pass subpath in as a parameter only for the error message
   *
   * @param parent The parent path (the workspace directory).
   * @param combinedPath The workspace directory and (relative) subpath path combined.
   * @param subpath For error message only, the subpath
   */
  public static void checkBackPaths(String parent, String combinedPath, String subpath) {
    Preconditions.checkArgument(!parent.isEmpty(), "Invalid root (" + parent + ") in file selection path.");
    Preconditions.checkArgument(!combinedPath.isEmpty(), "Empty path (" + combinedPath + "( in file selection path.");

    if (!combinedPath.startsWith(parent)) {
      throw new IllegalArgumentException(
        String.format("Invalid path [%s] takes you outside the workspace.", subpath));
    }
  }

  public List<FileStatus> getFileStatuses() {
    return statuses;
  }

  public boolean supportsDirPruning() {
    if (isExpandedFully() || isExpandedPartial()) {
      if (!wasAllPartitionsPruned) {
        return true;
      }
    }
    return false;
  }

  public void setHadWildcard(boolean wc) {
    this.hadWildcard = wc;
  }

  public boolean hadWildcard() {
    return this.hadWildcard;
  }

  public Path getCacheFileRoot() {
    return cacheFileRoot;
  }

  public void setMetaContext(MetadataContext context) {
    metaContext = context;
  }

  public MetadataContext getMetaContext() {
    return metaContext;
  }

  /**
   * @return true if this {@link FileSelection#selectionRoot} points to an empty directory, false otherwise
   */
  public boolean isEmptyDirectory() {
    return emptyDirectory;
  }

  /**
   * Setting {@link FileSelection#emptyDirectory} as true allows to identify this {@link FileSelection#selectionRoot}
   * as an empty directory
   */
  public void setEmptyDirectoryStatus() {
    this.emptyDirectory = true;
  }

  @Override
  public String digest() {
    return toString();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("root=").append(selectionRoot);
    sb.append("files=[");
    sb.append(getFiles().stream()
      .map(Path::toString)
      .collect(Collectors.joining(", ")));
    sb.append("]");
    return sb.toString();
  }
}
