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
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.io.SupportsPrefixOperations;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * An adapter class to make a {@link FileIO} object adapt to {@link ArcticFileIO} interface.
 */
public class ArcticFileIOAdapter implements ArcticFileIO {

  private final FileIO io;

  public ArcticFileIOAdapter(FileIO io) {
    this.io = io;
  }

  @Override
  public <T> T doAs(Callable<T> callable) {
    if (io instanceof ArcticFileIO) {
      return ((ArcticFileIO) io).doAs(callable);
    }
    try {
      return callable.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean exists(String path) {
    if (io instanceof ArcticFileIO) {
      return ((ArcticFileIO) io).exists(path);
    }
    return ArcticFileIO.super.exists(path);
  }

  @Override
  public boolean supportPrefixOperations() {
    return io instanceof SupportsPrefixOperations;
  }

  @Override
  public SupportsPrefixOperations asPrefixFileIO() {
    Preconditions.checkArgument(supportPrefixOperations());
    return (SupportsPrefixOperations) io;
  }

  @Override
  public boolean supportFileSystemOperations() {
    return io instanceof ArcticFileIO && ((ArcticFileIO) io).supportFileSystemOperations();
  }

  @Override
  public SupportsFileSystemOperations asFileSystemIO() {
    Preconditions.checkArgument(this.supportFileSystemOperations());
    return ((ArcticFileIO) io).asFileSystemIO();
  }

  @Override
  public boolean supportsFileRecycle() {
    return io instanceof ArcticFileIO && ((ArcticFileIO) io).supportsFileRecycle();
  }

  @Override
  public SupportFileRecycleOperations asFileRecycleIO() {
    Preconditions.checkArgument(this.supportsFileRecycle());
    return ((ArcticFileIO) io).asFileRecycleIO();
  }

  @Override
  public InputFile newInputFile(String path) {
    return io.newInputFile(path);
  }

  @Override
  public InputFile newInputFile(String path, long length) {
    return io.newInputFile(path, length);
  }

  @Override
  public OutputFile newOutputFile(String path) {
    return io.newOutputFile(path);
  }

  @Override
  public void deleteFile(String path) {
    io.deleteFile(path);
  }

  @Override
  public void deleteFile(InputFile file) {
    io.deleteFile(file);
  }

  @Override
  public void deleteFile(OutputFile file) {
    io.deleteFile(file);
  }

  @Override
  public Map<String, String> properties() {
    return io.properties();
  }

  @Override
  public void initialize(Map<String, String> properties) {
    io.initialize(properties);
  }

  @Override
  public void close() {
    io.close();
  }
}
