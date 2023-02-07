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
package org.apache.drill.exec.util;

import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import java.io.IOException;
import java.util.List;

/**
 * In Drill file system all directories and files that start with dot or underscore is ignored.
 * This helper class that delegates all work to list directory and file statuses to {@link org.apache.drill.exec.util.FileSystemUtil} class,
 * only adding Drill file system filter first.
 */
public class DrillFileSystemUtil {

  /**
   * Path filter that skips all files and folders that start with dot or underscore.
   */
  public static final PathFilter DRILL_SYSTEM_FILTER = path ->
    !path.getName().startsWith(DrillFileSystem.UNDERSCORE_PREFIX) && !path.getName().startsWith(DrillFileSystem.DOT_PREFIX);

  /**
   * Returns statuses of all directories present in given path applying custom filters if present.
   * Directories that start with dot or underscore are skipped.
   * Will also include nested directories if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to directory
   * @param recursive true if nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory statuses
   */
  public static List<FileStatus> listDirectories(final FileSystem fs, Path path, boolean recursive, PathFilter... filters) throws IOException {
    return FileSystemUtil.listDirectories(fs, path, recursive, FileSystemUtil.mergeFilters(DRILL_SYSTEM_FILTER, filters));
  }

  /**
   * Returns statuses of all directories present in given path applying custom filters if present.
   * Directories that start with dot or underscore are skipped.
   * Will include nested directories if recursive flag is set to true.
   * Will ignore all exceptions during listing if any.
   *
   * @param fs current file system
   * @param path path to directory
   * @param recursive true if nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory statuses
   */
  public static List<FileStatus> listDirectoriesSafe(final FileSystem fs, Path path, boolean recursive, PathFilter... filters) {
    return FileSystemUtil.listDirectoriesSafe(fs, path, recursive, FileSystemUtil.mergeFilters(DRILL_SYSTEM_FILTER, filters));
  }

  /**
   * Returns statuses of all files present in given path applying custom filters if present.
   * Files and nested directories that start with dot or underscore are skipped.
   * Will also include files from nested directories if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if files in nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching file statuses
   */
  public static List<FileStatus> listFiles(final FileSystem fs, Path path, boolean recursive, PathFilter... filters) throws IOException {
    return FileSystemUtil.listFiles(fs, path, recursive, FileSystemUtil.mergeFilters(DRILL_SYSTEM_FILTER, filters));
  }

  /**
   * Returns statuses of all files present in given path applying custom filters if present.
   * Files and nested directories that start with dot or underscore are skipped.
   * Will include files from nested directories if recursive flag is set to true.
   * Will ignore all exceptions during listing if any.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if files in nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching file statuses
   */
  public static List<FileStatus> listFilesSafe(final FileSystem fs, Path path, boolean recursive, PathFilter... filters) {
    return FileSystemUtil.listFilesSafe(fs, path, recursive, FileSystemUtil.mergeFilters(DRILL_SYSTEM_FILTER, filters));
  }


  /**
   * Returns statuses of all directories and files present in given path applying custom filters if present.
   * Directories and files that start with dot or underscore are skipped.
   * Will also include nested directories and their files if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if nested directories and their files should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory and file statuses
   */
  public static List<FileStatus> listAll(FileSystem fs, Path path, boolean recursive, PathFilter... filters) throws IOException {
    return FileSystemUtil.listAll(fs, path, recursive, FileSystemUtil.mergeFilters(DRILL_SYSTEM_FILTER, filters));
  }

  /**
   * Returns statuses of all directories and files present in given path applying custom filters if present.
   * Directories and files that start with dot or underscore are skipped.
   * Will include nested directories and their files if recursive flag is set to true.
   * Will ignore all exceptions during listing if any.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if nested directories and their files should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory and file statuses
   */
  public static List<FileStatus> listAllSafe(FileSystem fs, Path path, boolean recursive, PathFilter... filters) {
    return FileSystemUtil.listAllSafe(fs, path, recursive, FileSystemUtil.mergeFilters(DRILL_SYSTEM_FILTER, filters));
  }

  /**
   * Safely creates Hadoop Path for null and empty String paths
   *
   * @param path String path, which can be null or empty
   * @return Hadoop Path. Root - for empty or null path
   */
  public static Path createPathSafe(String path) {
    return Strings.isNullOrEmpty(path) ? new Path("/") : new Path(path);
  }

}
