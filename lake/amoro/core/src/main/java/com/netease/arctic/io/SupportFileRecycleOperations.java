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

import java.time.LocalDate;

/**
 * A mixed-in interface for {@link ArcticFileIO} which indicate the storage system
 * support file recycle operations.
 */
public interface SupportFileRecycleOperations extends ArcticFileIO {

  @Override
  default boolean supportsFileRecycle() {
    return true;
  }

  /**
   * show the given path could be deleted as recoverable.
   * @param path - a give path to check
   * @return true if the path could be deleted as recoverable.
   */
  boolean fileRecoverable(String path);

  /**
   * recover a path which had been deleted as recoverable.
   * @param path a path had been deleted.
   * @return if the target of path has been recovered.
   */
  boolean recover(String path);

  /**
   * expire the recycle space by an expiration data
   * @param expirationDate the date for expire
   */
  void expireRecycle(LocalDate expirationDate);
}
