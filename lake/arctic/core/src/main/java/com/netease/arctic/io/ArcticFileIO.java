/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.fs.FileStatus;
import org.apache.iceberg.io.FileIO;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Arctic extension from {@link FileIO}, adding more operations.
 */
public interface ArcticFileIO extends FileIO, Configurable {

  /**
   * Run the given action with login user.
   *
   * @param callable the method to execute
   * @param <T> the return type of the run method
   * @return the value from the run method
   */
  <T> T doAs(Callable<T> callable);

  /**
   * Check if a path exists.
   *
   * @param path source pathmkdir
   * @return true if the path exists;
   */
  boolean exists(String path);

  /**
   * Create a new directory and all non-existent parents directories.
   *
   * @param path source path
   */
  void mkdirs(String path);

  /**
   * Rename file from old path to new path
   *
   * @param oldPath source path
   * @param newPath target path
   */
  void rename(String oldPath, String newPath);

  /** Delete a directory recursively
   *
   * @param path the path to delete.
   */
  void deleteDirectoryRecursively(String path);

  //TODO FileStatus is a hadoop object, need to be replaced
  List<FileStatus> list(String location);

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
}
