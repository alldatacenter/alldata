/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import org.apache.iceberg.io.SupportsPrefixOperations;

/**
 * A mixed-in interface for {@link ArcticFileIO} indicate that the storage system
 * supports the file system operations such as directories operations.
 */
public interface SupportsFileSystemOperations extends ArcticFileIO, SupportsPrefixOperations {

  /**
   * Create a new directory and all non-existent parents directories.
   *
   * @param path source path
   */
  void makeDirectories(String path);

  /**
   * Check if a location is a directory.
   *
   * @param location source location
   * @return true if the location is a directory
   */
  boolean isDirectory(String location);

  /**
   * Check if a location is an empty directory.
   *
   * @param location source location
   * @return true if the location is an empty directory
   */
  boolean isEmptyDirectory(String location);

  /**
   * Rename file from old path to new path
   *
   * @param oldPath source path
   * @param newPath target path
   */
  void rename(String oldPath, String newPath);


  Iterable<PathInfo> listDirectory(String location);


  @Override
  default SupportsFileSystemOperations asFileSystemIO() {
    return this;
  }

  @Override
  default boolean supportFileSystemOperations() {
    return true;
  }

  @Override
  default SupportsPrefixOperations asPrefixFileIO() {
    return this;
  }

  @Override
  default boolean supportPrefixOperations() {
    return true;
  }
}
