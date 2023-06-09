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
package org.apache.drill.exec.physical.impl.scan.file;

import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/**
 * Specify the file name and optional selection root. If the selection root
 * is provided, then partitions are defined as the portion of the file name
 * that is not also part of the selection root. That is, if selection root is
 * /a/b and the file path is /a/b/c/d.csv, then dir0 is c.
 */

public class FileMetadata {

  private final Path filePath;
  private final String[] dirPath;
  private final FileSystem fs;

  public FileMetadata(Path filePath, Path selectionRoot) {
    this(filePath, selectionRoot, null);
  }

  public FileMetadata(Path filePath, Path selectionRoot, FileSystem fs) {
    this.filePath = filePath;
    this.fs = fs;

    // If the data source is not a file, no file metadata is available.

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

  public Path filePath() { return filePath; }

  public String partition(int index) {
    if (dirPath == null ||  dirPath.length <= index) {
      return null;
    }
    return dirPath[index];
  }

  public int dirPathLength() {
    return dirPath == null ? 0 : dirPath.length;
  }

  public boolean isSet() { return filePath != null; }

  public String getImplicitColumnValue(ColumnExplorer.ImplicitFileColumn column) {
    return ColumnExplorer.getImplicitColumnValue(column, filePath, fs);
  }
}
