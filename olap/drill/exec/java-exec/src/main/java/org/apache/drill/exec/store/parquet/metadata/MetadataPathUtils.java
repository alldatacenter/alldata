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

import org.apache.drill.common.util.DrillVersionInfo;
import org.apache.hadoop.fs.Path;

import java.util.List;
import java.util.ArrayList;

import static org.apache.drill.exec.store.parquet.metadata.MetadataVersion.Constants.SUPPORTED_VERSIONS;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ParquetFileMetadata_v4;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V4.ParquetTableMetadata_v4;
import static org.apache.drill.exec.store.parquet.metadata.Metadata_V3.ParquetFileMetadata_v3;
import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ParquetFileMetadata;

/**
 * Util class that contains helper methods for converting paths in the table and directory metadata structures
 */
public class MetadataPathUtils {

  /**
   * Helper method that converts a list of relative paths to absolute ones
   *
   * @param paths list of relative paths
   * @param baseDir base parent directory
   * @return list of absolute paths
   */
  public static List<Path> convertToAbsolutePaths(List<Path> paths, String baseDir) {
    if (!paths.isEmpty()) {
      List<Path> absolutePaths = new ArrayList<>();
      for (Path relativePath : paths) {
        Path absolutePath = (relativePath.isAbsolute()) ? relativePath : new Path(baseDir, relativePath);
        absolutePaths.add(absolutePath);
      }
      return absolutePaths;
    }
    return paths;
  }

  /**
   * Convert a list of files with relative paths to files with absolute ones
   *
   * @param files list of files with relative paths
   * @param baseDir base parent directory
   * @return list of files with absolute paths
   */
  public static List<? extends ParquetFileMetadata> convertToFilesWithAbsolutePaths(
      List<? extends ParquetFileMetadata> files, String baseDir) {
    if (!files.isEmpty()) {
      List<ParquetFileMetadata> filesWithAbsolutePaths = new ArrayList<>();
      for (ParquetFileMetadata file : files) {
        Path relativePath = file.getPath();
        ParquetFileMetadata fileWithAbsolutePath = null;
        // create a new file if old one contains a relative path, otherwise use an old file
        if (file instanceof ParquetFileMetadata_v4) {
          fileWithAbsolutePath = (relativePath.isAbsolute()) ? file
              : new ParquetFileMetadata_v4(new Path(baseDir, relativePath), file.getLength(), (List<Metadata_V4.RowGroupMetadata_v4>) file.getRowGroups());
        } else if (file instanceof ParquetFileMetadata_v3) {
          fileWithAbsolutePath = (relativePath.isAbsolute()) ? file
              : new ParquetFileMetadata_v3(new Path(baseDir, relativePath), file.getLength(), (List<Metadata_V3.RowGroupMetadata_v3>) file.getRowGroups());
        }
        filesWithAbsolutePaths.add(fileWithAbsolutePath);
      }
      return filesWithAbsolutePaths;
    }
    return files;
  }

  /**
   * Creates a new parquet table metadata from the {@code tableMetadataWithAbsolutePaths} parquet table.
   * A new parquet table will contain relative paths for the files and directories.
   *
   * @param tableMetadataWithAbsolutePaths parquet table metadata with absolute paths for the files and directories
   * @param baseDir base parent directory
   * @return parquet table metadata with relative paths for the files and directories
   */
  public static ParquetTableMetadata_v4 createMetadataWithRelativePaths(
      ParquetTableMetadata_v4 tableMetadataWithAbsolutePaths, Path baseDir) {
    List<Path> directoriesWithRelativePaths = new ArrayList<>();
    for (Path directory : tableMetadataWithAbsolutePaths.getDirectories()) {
      directoriesWithRelativePaths.add(relativize(baseDir, directory));
    }
    List<ParquetFileMetadata_v4> filesWithRelativePaths = new ArrayList<>();
    for (ParquetFileMetadata_v4 file : (List<ParquetFileMetadata_v4>) tableMetadataWithAbsolutePaths.getFiles()) {
      filesWithRelativePaths.add(new ParquetFileMetadata_v4(
          relativize(baseDir, file.getPath()), file.length, file.rowGroups));
    }
    return new ParquetTableMetadata_v4(SUPPORTED_VERSIONS.last().toString(), tableMetadataWithAbsolutePaths,
        filesWithRelativePaths, directoriesWithRelativePaths, DrillVersionInfo.getVersion(), tableMetadataWithAbsolutePaths.getTotalRowCount(), tableMetadataWithAbsolutePaths.isAllColumnsInteresting());
  }

  /**
   * Constructs relative path from child full path and base path. Or return child path if the last one is already relative
   *
   * @param childPath full absolute path
   * @param baseDir base path (the part of the Path, which should be cut off from child path)
   * @return relative path
   */
  public static Path relativize(Path baseDir, Path childPath) {
    Path fullPathWithoutSchemeAndAuthority = Path.getPathWithoutSchemeAndAuthority(childPath);
    Path basePathWithoutSchemeAndAuthority = Path.getPathWithoutSchemeAndAuthority(baseDir);

    // Since hadoop Path hasn't relativize() we use uri.relativize() to get relative path
    Path relativeFilePath = new Path(basePathWithoutSchemeAndAuthority.toUri()
        .relativize(fullPathWithoutSchemeAndAuthority.toUri()));
    if (relativeFilePath.isAbsolute()) {
      throw new IllegalStateException(String.format("Path %s is not a subpath of %s.",
          basePathWithoutSchemeAndAuthority.toUri().getPath(), fullPathWithoutSchemeAndAuthority.toUri().getPath()));
    }
    return relativeFilePath;
  }

}
