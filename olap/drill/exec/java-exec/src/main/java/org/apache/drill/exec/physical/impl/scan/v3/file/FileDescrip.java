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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import java.io.IOException;
import java.io.InputStream;

import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileSplit;

/**
 * Describes one file within a scan and is used to populate implicit columns.
 * Specify the file name and optional selection root. If the selection root
 * is provided, then partitions are defined as the portion of the file name
 * that is not also part of the selection root. That is, if selection root is
 * /a/b and the file path is /a/b/c/d.csv, then dir0 is c.
 */
public class FileDescrip {

  private final DrillFileSystem dfs;
  private final FileWork fileWork;
  private final FileSplit split;
  private final String[] dirPath;

  // Option to open the file as optionally compressed
  private boolean isCompressible;

  // Parquet-related attributes
  protected Integer rowGroupIndex;
  protected Long rowGroupStart;
  protected Long rowGroupLength;

  // Cached modification time. Cached as a string because
  // that's the odd way we return the value.
  private String modTime;

  // Flag to indicate that the file turned out to be empty.
  // Used to set one of the internal implicit columns.
  protected boolean isEmpty;

  public FileDescrip(DrillFileSystem dfs, FileWork fileWork, Path selectionRoot) {
    this.dfs = dfs;
    this.fileWork = fileWork;
    Path path = dfs.makeQualified(fileWork.getPath());
    this.split = new FileSplit(path, fileWork.getStart(), fileWork.getLength(), new String[]{""});

    // If the data source is not a file, no file metadata is available.
    Path filePath = fileWork.getPath();
    if (selectionRoot == null || filePath == null) {
      dirPath = null;
      return;
    }

    // If the query is against a single file, selection root and file path
    // will be identical, oddly.
    Path rootPath = Path.getPathWithoutSchemeAndAuthority(selectionRoot);
    Path bareFilePath = Path.getPathWithoutSchemeAndAuthority(filePath);
    if (rootPath.equals(bareFilePath)) {
      dirPath = null;
      return;
    }

    dirPath = ColumnExplorer.parsePartitions(filePath, rootPath, false);
    if (dirPath == null) {
      throw new IllegalArgumentException(
          String.format("Selection root of \"%s\" is not a leading path of \"%s\"",
          selectionRoot.toString(), filePath.toString()));
    }
  }

  /**
   * Gives the Drill file system for this operator.
   */
  public DrillFileSystem fileSystem() { return dfs; }

  /**
   * Returns Drill's version of the Hadoop file split.
   */
  public Path filePath() { return fileWork.getPath(); }

  /**
   * Describes the file split (path and block offset) for this scan.
   *
   * @return Hadoop file split object with the file path, block
   * offset, and length.
   */
  public FileSplit split() { return split; }

  public FileWork fileWork() { return fileWork; }

  public String partition(int index) {
    if (dirPath == null ||  dirPath.length <= index) {
      return null;
    }
    return dirPath[index];
  }

  public int dirPathLength() {
    return dirPath == null ? 0 : dirPath.length;
  }

  public void setRowGroupAttribs(int index, long start, long length) {
    this.rowGroupIndex = index;
    this.rowGroupStart = start;
    this.rowGroupLength = length;
  }

  public String getModTime() {
    if (modTime == null) {
      try {
        modTime = String.valueOf(dfs.getFileStatus(filePath()).getModificationTime());
      } catch (IOException e) {

        // This is an odd place to catch and report errors. Assume that, if the file
        // has problems, the call to open the file will fail and will return a better
        // error message than we can provide here.
      }
    }
    return modTime;
  }

  /**
   * Explicitly set the cached modification time. For testing only.
   */
  @VisibleForTesting
  public void setModTime(String modTime) {
    this.modTime = modTime;
  }

  public void setCompressible(boolean isCompressed) {
    this.isCompressible = isCompressed;
  }

  public boolean isCompressible() { return isCompressible; }

  public InputStream open() throws IOException {
    if (isCompressible) {
      return dfs.openPossiblyCompressedStream(filePath());
    } else {
      return dfs.open(filePath());
    }
  }

  public void markEmpty() {
    isEmpty = true;
  }
}
