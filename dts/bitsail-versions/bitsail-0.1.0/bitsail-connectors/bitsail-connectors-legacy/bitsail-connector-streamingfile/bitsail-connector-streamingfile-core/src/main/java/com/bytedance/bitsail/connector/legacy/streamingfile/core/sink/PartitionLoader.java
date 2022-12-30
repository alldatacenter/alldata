/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs.HdfsMetaStoreFactory;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.util.Preconditions;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils.generatePartitionPath;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils.listStatusWithoutHidden;

/**
 * Loader to temporary files to final output path and meta store. According to overwrite,
 * the loader will delete the previous data.
 *
 * <p>This provide two interface to load:
 * 1.{@link #commitPartitionFiles}: commit temporary partitioned files
 * 2.{@link #commitNonPartitionFile}: just rename all files to final output path.
 */
@Internal
@Builder
@Slf4j
public class PartitionLoader implements Closeable {

  private final boolean overwrite;
  private final FileSystem fs;
  private final TableMetaStoreFactory factory;
  private Path locationPath;

  public PartitionLoader(
      boolean overwrite,
      FileSystem fs,
      TableMetaStoreFactory factory,
      Path locationPath) {
    this.overwrite = overwrite;
    this.fs = fs;
    this.factory = factory;

    this.locationPath = locationPath;
  }

  /**
   * Load a single partition.
   */
  @Deprecated
  public void loadPartition(
      LinkedHashMap<String, String> partSpec, List<Path> srcDirs) throws Exception {
    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      Optional<Path> pathFromMeta = metaStore.getPartition(partSpec);
      Path path = pathFromMeta.orElseGet(() -> new Path(
          metaStore.getLocationPath(), generatePartitionPath(partSpec)));

      overwriteAndRenameFiles(srcDirs, path);
      if (!pathFromMeta.isPresent()) {
        metaStore.createPartition(partSpec, path);
      }
    }
  }

  /**
   * Load a non-partition files to output path.
   */
  @Deprecated
  public void loadNonPartition(List<Path> srcDirs) throws Exception {
    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      Path tableLocation = metaStore.getLocationPath();
      overwriteAndRenameFiles(srcDirs, tableLocation);
    }
  }

  /**
   * commit a single partition file.
   */
  public List<Path> commitPartitionFile(
      LinkedHashMap<String, String> partSpec, List<Path> srcDirs) throws Exception {
    Path path = getPartitionPath(partSpec);

    return overwriteAndRenameFiles(srcDirs, path);
  }

  private Path getPartitionPath(LinkedHashMap<String, String> partSpec) throws Exception {
    Path path;
    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      if (metaStore instanceof HdfsMetaStoreFactory.HdfsTableMetaStore) {
        path = metaStore.getPartition(partSpec).get();
      } else {
        path = new Path(locationPath, generatePartitionPath(partSpec));
      }
    }
    return path;
  }

  public void commitPartitionFiles(LinkedHashMap<String, String> partSpec,
                                   List<Path> filePaths,
                                   CommitFileStatus commitFileStatus) throws Exception {
    Path path = getPartitionPath(partSpec);
    overwriteAndRenameSrcFiles(filePaths, path, commitFileStatus);
  }

  /**
   * commit a non-partition files to output path.
   */
  public void commitNonPartitionFile(List<Path> srcDirs) throws Exception {
    overwriteAndRenameFiles(srcDirs, locationPath);
  }

  /**
   * commit a non-partition files to output path.
   */
  public void commitNonPartitionSrcFiles(List<Path> filePaths, CommitFileStatus commitFileStatus) throws Exception {
    overwriteAndRenameSrcFiles(filePaths, locationPath, commitFileStatus);
  }

  private List<Path> overwriteAndRenameFiles(List<Path> srcDirs, Path destDir) throws Exception {
    boolean dirSuccessExist = fs.exists(destDir) || fs.mkdirs(destDir);
    Preconditions.checkState(dirSuccessExist, "Failed to create dest path " + destDir);
    overwrite(destDir);
    return renameFiles(srcDirs, destDir);
  }

  private void overwriteAndRenameSrcFiles(List<Path> srcFilePaths, Path destDir, CommitFileStatus commitFileStatus) throws Exception {
    boolean dirSuccessExist = fs.exists(destDir) || fs.mkdirs(destDir);
    Preconditions.checkState(dirSuccessExist, "Failed to create dest path " + destDir);
    overwrite(destDir);
    renameSrcFiles(srcFilePaths, destDir, commitFileStatus);
  }

  private void overwrite(Path destDir) throws Exception {
    if (overwrite) {
      // delete existing files for overwrite
      FileStatus[] existingFiles = listStatusWithoutHidden(fs, destDir);
      if (existingFiles != null) {
        for (FileStatus existingFile : existingFiles) {
          // TODO: We need move to trash when auto-purge is false.
          fs.delete(existingFile.getPath(), true);
        }
      }
    }
  }

  /**
   * Moves files from srcDir to destDir. Delete files in destDir first when overwrite.
   */
  private List<Path> renameFiles(List<Path> srcDirs, Path destDir) throws Exception {
    List<Path> destPaths = Lists.newArrayListWithCapacity(CollectionUtils.size(srcDirs));
    for (Path srcDir : srcDirs) {
      if (!srcDir.equals(destDir)) {
        FileStatus[] srcFiles = listStatusWithoutHidden(fs, srcDir);
        if (srcFiles != null) {
          for (FileStatus srcFile : srcFiles) {
            Path srcPath = srcFile.getPath();
            Path destPath = new Path(destDir, srcPath.getName());
            int count = 1;
            while (!fs.rename(srcPath, destPath)) {
              String name = srcPath.getName() + "_copy_" + count;
              destPath = new Path(destDir, name);
              count++;
            }
            destPaths.add(destPath);
          }
        }
      }
    }
    return destPaths;
  }

  /**
   * @param srcFilePaths src file path list with file name
   * @param destDir      dest directory
   */
  private void renameSrcFiles(List<Path> srcFilePaths, Path destDir, CommitFileStatus commitFileStatus) throws Exception {
    for (Path srcPath : srcFilePaths) {
      Path destPath = new Path(destDir, srcPath.getName());
      if (fs.exists(srcPath)) {
        int count = 1;
        //  rename success
        try {
          if (fs.rename(srcPath, destPath)) {
            commitFileStatus.addSuccessCount();
            continue;
          }
        } catch (Exception e) {
          checkDestExists(srcPath, destPath, commitFileStatus, false, e);
          continue;
        }
        // rename fail and src path is empty
        if (fs.exists(destPath) && fs.getFileStatus(srcPath).getLen() == 0) {
          //  when renaming file fail and src path is empty, we will skip renaming it.
          log.warn("Dest path {} is exists. And src path {} is empty. We will not rename it!", destPath, srcPath);
          commitFileStatus.addSkipCount();
          continue;
        }
        while (!fs.rename(srcPath, destPath)) {
          String name = srcPath.getName() + "_copy_" + count;
          count++;
          destPath = new Path(destDir, name);
        }
        commitFileStatus.addSuccessCount();
        log.debug("Rename src path {} to dest path {}.", srcPath, destPath);
        continue;
      }

      // src path not exists
      checkDestExists(srcPath, destDir, commitFileStatus, true, null);
    }
  }

  @VisibleForTesting
  void checkDestExists(Path srcPath,
                       Path destPath,
                       CommitFileStatus commitFileStatus,
                       boolean markAsSkip,
                       Exception exception) throws Exception {
    if (!fs.exists(destPath)) {
      // to check again src exists.
      if (Objects.nonNull(exception) && !fs.exists(srcPath)) {
        log.error("Destination path: {} and source path: {} both disappeared.", destPath, srcPath,
            exception);
        throw exception;
      }
      if (fs.exists(srcPath)) {
        throw new IllegalStateException(String.format("Src path %s rename failed.", srcPath));
      }
      commitFileStatus.addFailCount();
    } else {
      if (markAsSkip) {
        commitFileStatus.addSkipCount();
      } else {
        commitFileStatus.addSuccessCount();
      }
    }
  }

  @Override
  public void close() throws IOException {
  }
}
