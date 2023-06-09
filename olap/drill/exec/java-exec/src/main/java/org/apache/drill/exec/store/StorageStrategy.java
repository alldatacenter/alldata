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
package org.apache.drill.exec.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/** Contains list of parameters that will be used to store path / files on file system. */
public class StorageStrategy {
  private static final Logger logger = LoggerFactory.getLogger(StorageStrategy.class);

  /**
   * For directories: drwxrwxr-x (owner and group have full access, others can read and execute).
   * For files: -rw-rw--r-- (owner and group can read and write, others can read).
   * Folders and files are not deleted on file system close.
   */
  public static final StorageStrategy DEFAULT = new StorageStrategy("002", false);

  /**
   * Primary is used for temporary tables.
   * For directories: drwx------ (owner has full access, group and others have no access).
   * For files: -rw------- (owner can read and write, group and others have no access).
   * Folders and files are deleted on file system close.
   */
  public static final StorageStrategy TEMPORARY = new StorageStrategy("077", true);

  private final String umask;
  private final boolean deleteOnExit;

  @JsonCreator
  public StorageStrategy(@JsonProperty("umask") String umask,
                         @JsonProperty("deleteOnExit") boolean deleteOnExit) {
    this.umask = validateUmask(umask);
    this.deleteOnExit = deleteOnExit;
  }

  public String getUmask() {
    return umask;
  }

  public boolean isDeleteOnExit() {
    return deleteOnExit;
  }

  /**
   * @return folder permission after applying umask
   */
  @JsonIgnore
  public FsPermission getFolderPermission() {
    return FsPermission.getDirDefault().applyUMask(new FsPermission(umask));
  }

  /**
   * @return file permission after applying umask
   */
  @JsonIgnore
  public FsPermission getFilePermission() {
    return FsPermission.getFileDefault().applyUMask(new FsPermission(umask));
  }

  /**
   * Creates passed path on appropriate file system.
   * Before creation checks which parent directories do not exists.
   * Applies storage strategy rules to all newly created directories.
   * Will return first created path or null already existed.
   *
   * Case 1: /a/b -> already exists, attempt to create /a/b/c/d
   * Will create path and return /a/b/c.
   * Case 2: /a/b/c -> already exists, attempt to create /a/b/c/d
   * Will create path and return /a/b/c/d.
   * Case 3: /a/b/c/d -> already exists, will return null.
   *
   * @param fs file system where file should be located
   * @param path location path
   * @return first created parent path or file
   * @throws IOException is thrown in case of problems while creating path, setting permission
   *         or adding path to delete on exit list
   */
  public Path createPathAndApply(FileSystem fs, Path path) throws IOException {
    List<Path> locations = getNonExistentLocations(fs, path);
    if (locations.isEmpty()) {
      return null;
    }
    fs.mkdirs(path);
    for (Path location : locations) {
      applyStrategy(fs, location, getFolderPermission(), deleteOnExit);
    }
    return locations.get(locations.size() - 1);
  }

  /**
   * Creates passed file on appropriate file system.
   * Before creation checks which parent directories do not exists.
   * Applies storage strategy rules to all newly created directories and file.
   * Will return first created parent path or file if no new parent paths created.
   *
   * Case 1: /a/b -> already exists, attempt to create /a/b/c/some_file.txt
   * Will create file and return /a/b/c.
   * Case 2: /a/b/c -> already exists, attempt to create /a/b/c/some_file.txt
   * Will create file and return /a/b/c/some_file.txt.
   * Case 3: /a/b/c/some_file.txt -> already exists, will fail.
   *
   * @param fs file system where file should be located
   * @param file file path
   * @return first created parent path or file
   * @throws IOException is thrown in case of problems while creating path, setting permission
   *         or adding path to delete on exit list
   */
  public Path createFileAndApply(FileSystem fs, Path file) throws IOException {
    List<Path> locations = getNonExistentLocations(fs, file.getParent());
    if (!fs.createNewFile(file)) {
      throw new IOException(String.format("File [%s] already exists on file system [%s].",
          file.toUri().getPath(), fs.getUri()));
    }
    applyToFile(fs, file);

    if (locations.isEmpty()) {
      return file;
    }

    for (Path location : locations) {
      applyStrategy(fs, location, getFolderPermission(), deleteOnExit);
    }
    return locations.get(locations.size() - 1);
  }

  /**
   * Applies storage strategy to file:
   * sets permission and adds to file system delete on exit list if needed.
   *
   * @param fs file system
   * @param file path to file
   * @throws IOException is thrown in case of problems while setting permission
   *         or adding file to delete on exit list
   */
  public void applyToFile(FileSystem fs, Path file) throws IOException {
    applyStrategy(fs, file, getFilePermission(), deleteOnExit);
  }

  /**
   * Validates if passed umask is valid.
   * If umask is valid, returns given umask.
   * If umask is invalid, returns default umask and logs error.
   *
   * @param umask umask string representation
   * @return valid umask value
   */
  private String validateUmask(String umask) {
    try {
      new FsPermission(umask);
      return umask;
    } catch (IllegalArgumentException | NullPointerException e) {
      logger.error("Invalid umask value [{}]. Using default [{}].", umask, DEFAULT.getUmask(), e);
      return DEFAULT.getUmask();
    }
  }

  /**
   * Returns list of parent locations that do not exist, including initial location.
   * First in the list will be initial location,
   * last in the list will be last parent location that does not exist.
   * If all locations exist, empty list will be returned.
   *
   * Case 1: if /a/b exists and passed location is /a/b/c/d,
   * will return list with two elements: 0 -> /a/b/c/d, 1 -> /a/b/c
   * Case 2: if /a/b exists and passed location is /a/b, will return empty list.
   *
   * @param fs file system where locations should be located
   * @param path location path
   * @return list of locations that do not exist
   * @throws IOException in case of troubles accessing file system
   */
  private List<Path> getNonExistentLocations(FileSystem fs, Path path) throws IOException {
    List<Path> locations = Lists.newArrayList();
    Path starting = path;
    while (starting != null && !fs.exists(starting)) {
      locations.add(starting);
      starting = starting.getParent();
    }
    return locations;
  }

  /**
   * Applies storage strategy to passed path on passed file system.
   * Sets appropriate permission
   * and adds to file system delete on exit list if needed.
   *
   * @param fs file system where path is located
   * @param path path location
   * @param permission permission to be applied
   * @param deleteOnExit if to delete path on exit
   * @throws IOException is thrown in case of problems while setting permission
   *         or adding path to delete on exit list
   */
  private void applyStrategy(FileSystem fs, Path path, FsPermission permission, boolean deleteOnExit) throws IOException {
    fs.setPermission(path, permission);
    if (deleteOnExit) {
      fs.deleteOnExit(path);
    }
  }

  @Override
  public String toString() {
    return "StorageStrategy[umask=" + umask + ", deleteOnExist=" + deleteOnExit + "]";
  }
}
