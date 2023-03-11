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

import org.apache.hadoop.fs.FileStatus;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;

import java.util.List;
import java.util.concurrent.Callable;

public class ArcticFileIoDummy implements ArcticFileIO {

  private FileIO fileIO;

  public ArcticFileIoDummy(FileIO fileIO) {
    this.fileIO = fileIO;
  }

  @Override
  public <T> T doAs(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean exists(String path) {
    return false;
  }

  @Override
  public void mkdirs(String path) {

  }

  @Override
  public void rename(String oldpath, String newPath) {

  }

  @Override
  public boolean deleteFileWithResult(String path, boolean recursive) {
    return false;
  }

  @Override
  public List<FileStatus> list(String location) {
    return null;
  }

  @Override
  public boolean isDirectory(String location) {
    return false;
  }

  @Override
  public boolean isEmptyDirectory(String location) {
    return false;
  }

  @Override
  public InputFile newInputFile(String path) {
    return fileIO.newInputFile(path);
  }

  @Override
  public OutputFile newOutputFile(String path) {
    return fileIO.newOutputFile(path);
  }

  @Override
  public void deleteFile(String path) {

  }
}
