/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import com.netease.arctic.table.TableIdentifier;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Trash Manager for a table.
 */
public interface TableTrashManager extends Serializable {
  /**
   * Table identifier.
   * A TableTrashManager only handle files in this table's location.
   *
   * @return table identifier
   */
  TableIdentifier tableId();

  /**
   * Move a file to trash, not support directory.
   *
   * @param path the file path
   * @throws java.io.UncheckedIOException - if failed to move file to trash
   * @throws IllegalArgumentException     - if path is a directory
   */
  void moveFileToTrash(String path);

  /**
   * If a file exist in trash, not support directory.
   *
   * @param path the file path
   * @return true if exist
   * @throws IllegalArgumentException - if path is a directory in trash
   */
  boolean fileExistInTrash(String path);

  /**
   * Restore a file from trash, not support directory.
   *
   * @param path the file
   * @return true for success
   * @throws IllegalArgumentException - if path is a directory in trash
   */
  boolean restoreFileFromTrash(String path);

  /**
   * Clean files from trash before expiration date (exclusive).
   *
   * @param expirationDate -
   */
  void cleanFiles(LocalDate expirationDate);

  /**
   * Get the root location of table trash.
   *
   * @return trash root location
   */
  String getTrashLocation();
}
