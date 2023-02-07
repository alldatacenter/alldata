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

import org.apache.drill.common.exceptions.ErrorHelper;
import org.apache.drill.common.exceptions.UserException;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class that provides methods to list directories or file or both statuses.
 * Can list statuses recursive and apply custom filters.
 */
public class FileSystemUtil {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileSystemUtil.class);
  public static final String RECURSIVE_FILE_LISTING_MAX_SIZE = "drill.exec.recursive_file_listing_max_size";

  /**
   * Filter that will accept all files and directories.
   */
  public static final PathFilter DUMMY_FILTER = path -> true;

  /**
   * Indicates which file system objects should be returned during listing.
   */
  private enum Scope {
    DIRECTORIES,
    FILES,
    ALL
  }

  /**
   * Returns statuses of all directories present in given path applying custom filters if present.
   * Will also include nested directories if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to directory
   * @param recursive true if nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory statuses
   */
  public static List<FileStatus> listDirectories(FileSystem fs, Path path, boolean recursive, PathFilter... filters) throws IOException {
    return list(fs, path, Scope.DIRECTORIES, recursive, false, filters);
  }

  /**
   * Returns statuses of all directories present in given path applying custom filters if present.
   * Will also include nested directories if recursive flag is set to true.
   * Will ignore all exceptions during listing if any.
   *
   * @param fs current file system
   * @param path path to directory
   * @param recursive true if nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory statuses
   */
  public static List<FileStatus> listDirectoriesSafe(FileSystem fs, Path path, boolean recursive, PathFilter... filters) {
    try {
      return list(fs, path, Scope.DIRECTORIES, recursive, true, filters);
    } catch (Exception e) {
      // all exceptions are ignored
      return Collections.emptyList();
    }
  }

  /**
   * Returns statuses of all files present in given path applying custom filters if present.
   * Will also include files from nested directories if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if files in nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching file statuses
   */
  public static List<FileStatus> listFiles(FileSystem fs, Path path, boolean recursive, PathFilter... filters) throws IOException {
    return list(fs, path, Scope.FILES, recursive, false, filters);
  }

  /**
   * Returns statuses of all files present in given path applying custom filters if present.
   * Will also include files from nested directories if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if files in nested directories should be included
   * @param filters list of custom filters (optional)
   * @return list of matching file statuses
   */
  public static List<FileStatus> listFilesSafe(FileSystem fs, Path path, boolean recursive, PathFilter... filters) {
    try {
      return list(fs, path, Scope.FILES, recursive, true, filters);
    } catch (Exception e) {
      // all exceptions are ignored
      return Collections.emptyList();
    }
  }

  /**
   * Returns statuses of all directories and files present in given path applying custom filters if present.
   * Will also include nested directories and their files if recursive flag is set to true.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if nested directories and their files should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory and file statuses
   */
  public static List<FileStatus> listAll(FileSystem fs, Path path, boolean recursive, PathFilter... filters) throws IOException {
    return list(fs, path, Scope.ALL, recursive, false, filters);
  }

  /**
   * Returns statuses of all directories and files present in given path applying custom filters if present.
   * Will also include nested directories and their files if recursive flag is set to true.
   * Will ignore all exceptions during listing if any.
   *
   * @param fs current file system
   * @param path path to file or directory
   * @param recursive true if nested directories and their files should be included
   * @param filters list of custom filters (optional)
   * @return list of matching directory and file statuses
   */
  public static List<FileStatus> listAllSafe(FileSystem fs, Path path, boolean recursive, PathFilter... filters) {
    try {
      return list(fs, path, Scope.ALL, recursive, true, filters);
    } catch (Exception e) {
      // all exceptions are ignored
      return Collections.emptyList();
    }
  }

  /**
   * Merges given filter with array of filters.
   * If array of filters is null or empty, will return given filter.
   *
   * @param filter given filter
   * @param filters array of filters
   * @return one filter that combines all given filters
   */
  public static PathFilter mergeFilters(PathFilter filter, PathFilter[] filters) {
    if (filters == null || filters.length == 0) {
      return filter;
    }

    int length = filters.length;
    PathFilter[] newFilters = Arrays.copyOf(filters, length + 1);
    newFilters[length] = filter;
    return mergeFilters(newFilters);
  }

  /**
   * Will merge given array of filters into one.
   * If given array of filters is empty, will return {@link #DUMMY_FILTER}.
   *
   * @param filters array of filters
   * @return one filter that combines all given filters
   */
  public static PathFilter mergeFilters(PathFilter... filters) {
    if (filters.length == 0) {
      return DUMMY_FILTER;
    }

    return path -> Stream.of(filters).allMatch(filter -> filter.accept(path));
  }

  /**
   * Helper method that merges given filters into one and
   * determines which listing method should be called based on recursive flag value.
   *
   * @param fs file system
   * @param path path to file or directory
   * @param scope file system objects scope
   * @param recursive indicates if listing should be done recursively
   * @param suppressExceptions indicates if exceptions should be ignored
   * @param filters filters to be applied
   * @return list of file statuses
   */
  private static List<FileStatus> list(FileSystem fs, Path path, Scope scope, boolean recursive, boolean suppressExceptions, PathFilter... filters) throws IOException {
    PathFilter filter = mergeFilters(filters);
    return recursive ? listRecursive(fs, path, scope, suppressExceptions, filter)
      : listNonRecursive(fs, path, scope, suppressExceptions, filter);
  }

  /**
   * Lists file statuses non-recursively based on given file system objects {@link Scope}.
   *
   * @param fs file system
   * @param path path to file or directory
   * @param scope file system objects scope
   * @param suppressExceptions indicates if exceptions should be ignored
   * @param filter filter to be applied
   * @return list of file statuses
   */
  private static List<FileStatus> listNonRecursive(FileSystem fs, Path path, Scope scope, boolean suppressExceptions, PathFilter filter) throws IOException {
    try {
      return Stream.of(fs.listStatus(path, filter))
        .filter(status -> isStatusApplicable(status, scope))
        .collect(Collectors.toList());
    } catch (Exception e) {
      if (suppressExceptions) {
        logger.debug("Exception during listing file statuses", e);
        return Collections.emptyList();
      } else {
        throw e;
      }
    }
  }

  /**
   * Lists file statuses recursively based on given file system objects {@link Scope}.
   * Uses {@link ForkJoinPool} executor service and {@link RecursiveListing} task
   * to parallel and speed up listing.
   *
   * @param fs file system
   * @param path path to file or directory
   * @param scope file system objects scope
   * @param suppressExceptions indicates if exceptions should be ignored
   * @param filter filter to be applied
   * @return list of file statuses
   */
  private static List<FileStatus> listRecursive(FileSystem fs, Path path, Scope scope, boolean suppressExceptions, PathFilter filter) {
    ForkJoinPool pool = new ForkJoinPool();
    AtomicInteger fileCounter = new AtomicInteger(0);
    int recursiveListingMaxSize = fs.getConf().getInt(RECURSIVE_FILE_LISTING_MAX_SIZE, 0);

    try {
      RecursiveListing task = new RecursiveListing(
        fs,
        path,
        scope,
        suppressExceptions,
        filter,
        fileCounter,
        recursiveListingMaxSize,
        pool
      );
      return pool.invoke(task);
    } catch (CancellationException ex) {
      logger.debug("RecursiveListing task to list {} was cancelled.", path);
      return Collections.<FileStatus>emptyList();
    } finally {
      pool.shutdown();
    }
  }

  /**
   * Checks if file status is applicable based on file system object {@link Scope}.
   *
   * @param status file status
   * @param scope file system objects scope
   * @return true if status is applicable, false otherwise
   */
  private static boolean isStatusApplicable(FileStatus status, Scope scope) {
    switch (scope) {
      case DIRECTORIES:
        return status.isDirectory();
      case FILES:
        return status.isFile();
      case ALL:
        return true;
      default:
        return false;
    }
  }

  /**
   * Task that parallels file status listing for each nested directory,
   * gathers and returns common list of file statuses.
   */
  private static class RecursiveListing extends RecursiveTask<List<FileStatus>> {

    private final FileSystem fs;
    private final Path path;
    private final Scope scope;
    private final boolean suppressExceptions;
    private final PathFilter filter;
    // Running count of files for comparison with RECURSIVE_FILE_LISTING_MAX_SIZE
    private final AtomicInteger fileCounter;
    private final int recursiveListingMaxSize;
    private final ForkJoinPool pool;

    RecursiveListing(
      FileSystem fs,
      Path path,
      Scope scope,
      boolean suppressExceptions,
      PathFilter filter,
      AtomicInteger fileCounter,
      int recursiveListingMaxSize,
      ForkJoinPool pool
    ) {
      this.fs = fs;
      this.path = path;
      this.scope = scope;
      this.suppressExceptions = suppressExceptions;
      this.filter = filter;
      this.fileCounter = fileCounter;
      this.recursiveListingMaxSize = recursiveListingMaxSize;
      this.pool = pool;
    }

    @Override
    protected List<FileStatus> compute() {
      List<FileStatus> statuses = new ArrayList<>();
      List<RecursiveListing> tasks = new ArrayList<>();

      try {
        FileStatus[] dirFs = fs.listStatus(path, filter);
        if (fileCounter.addAndGet(dirFs.length) > recursiveListingMaxSize && recursiveListingMaxSize > 0 ) {
          try {
            throw UserException
              .resourceError()
              .message(
                "File listing size limit of %d exceeded recursing through path %s, see JVM system property %s",
                recursiveListingMaxSize,
                path,
                RECURSIVE_FILE_LISTING_MAX_SIZE
              )
              .build(logger);
          } finally {
            // Attempt to abort all tasks
            this.pool.shutdownNow();
          }
        }

        for (FileStatus status : dirFs) {
          if (isStatusApplicable(status, scope)) {
            statuses.add(status);
          }
          if (status.isDirectory()) {
            RecursiveListing task = new RecursiveListing(
              fs,
              status.getPath(),
              scope,
              suppressExceptions,
              filter,
              fileCounter,
              recursiveListingMaxSize,
              pool
            );
            task.fork();
            tasks.add(task);
          }
        }
      } catch (Exception e) {
        if (suppressExceptions) {
          logger.debug("Exception during listing file statuses", e);
        } else {
          // is used to re-throw checked exception
          ErrorHelper.sneakyThrow(e);
        }
      }

      tasks.stream()
        .map(ForkJoinTask::join)
        .forEach(statuses::addAll);

      return statuses;
    }
  }
}
