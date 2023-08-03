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

import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.SupportsPrefixOperations;
import java.util.concurrent.Callable;

/**
 * Arctic extension from {@link FileIO}, adding more operations.
 */
public interface ArcticFileIO extends FileIO {

  /**
   * Run the given action with login user.
   *
   * @param callable the method to execute
   * @param <T>      the return type of the run method
   * @return the value from the run method
   */
  <T> T doAs(Callable<T> callable);

  /**
   * Check if a path exists.
   *
   * @param path source pathmkdir
   * @return true if the path exists;
   */
  default boolean exists(String path) {
    InputFile inputFile = newInputFile(path);
    return inputFile.exists();
  }

  /**
   * Returns true if this tableIo is an {@link SupportsPrefixOperations}
   */
  default boolean supportPrefixOperations() {
    return false;
  }

  /**
   * Return this cast to {@link SupportsPrefixOperations} if it is.
   */
  default SupportsPrefixOperations asPrefixFileIO() {
    if (supportPrefixOperations()) {
      return (SupportsPrefixOperations) this;
    } else {
      throw new IllegalStateException("Doesn't support prefix operations");
    }
  }

  /**
   * Returns true if this tableIo is an {@link SupportsFileSystemOperations}
   */
  default boolean supportFileSystemOperations() {
    return false;
  }

  /**
   * Return this cast to {@link SupportsFileSystemOperations} if it is.
   */
  default SupportsFileSystemOperations asFileSystemIO() {
    if (supportFileSystemOperations()) {
      return (SupportsFileSystemOperations) this;
    }
    throw new IllegalStateException("Doesn't support directory operations");
  }

  /**
   * Return true if this tableIo support file trash and could recover file be deleted.
   */
  default boolean supportsFileRecycle() {
    return false;
  }

  /**
   * Return this cast to {@link SupportFileRecycleOperations} if it is.
   */
  default SupportFileRecycleOperations asFileRecycleIO() {
    if (supportsFileRecycle()) {
      return (SupportFileRecycleOperations) this;
    }
    throw new IllegalStateException("Doesn't support file recycle");
  }
}
