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

import com.netease.arctic.table.TableMetaStore;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Implementation of {@link ArcticFileIO} with deleted files recovery support.
 */
public class RecoverableHadoopFileIO extends ArcticHadoopFileIO implements SupportFileRecycleOperations {
  private static final Logger LOG = LoggerFactory.getLogger(RecoverableHadoopFileIO.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final TableTrashManager trashManager;
  private final String trashFilePattern;
  private final Pattern pattern;

  RecoverableHadoopFileIO(
      TableMetaStore tableMetaStore,
      TableTrashManager trashManager,
      String trashFilePattern) {
    super(tableMetaStore);
    this.trashManager = trashManager;
    this.trashFilePattern = trashFilePattern;
    this.pattern = Strings.isNullOrEmpty(this.trashFilePattern) ? null : Pattern.compile(this.trashFilePattern);
  }

  @Override
  public void deleteFile(String path) {
    if (matchTrashFilePattern(path)) {
      moveToTrash(path);
    } else {
      super.deleteFile(path);
    }
  }

  @Override
  public void deleteFile(InputFile file) {
    if (matchTrashFilePattern(file.location())) {
      moveToTrash(file.location());
    } else {
      super.deleteFile(file);
    }
  }

  @Override
  public void deleteFile(OutputFile file) {
    if (matchTrashFilePattern(file.location())) {
      moveToTrash(file.location());
    } else {
      super.deleteFile(file);
    }
  }

  @VisibleForTesting
  protected boolean matchTrashFilePattern(String path) {
    return pattern.matcher(path).matches();
  }

  @VisibleForTesting
  public TableTrashManager getTrashManager() {
    return trashManager;
  }

  public String getTrashFilePattern() {
    return trashFilePattern;
  }

  private void moveToTrash(String filePath) {
    trashManager.moveFileToTrash(filePath);
    LOG.debug("Move file:{} to table trash", filePath);
  }

  @Override
  public boolean fileRecoverable(String path) {
    return this.trashManager.fileExistInTrash(path);
  }

  @Override
  public boolean recover(String path) {
    return this.trashManager.restoreFileFromTrash(path);
  }

  @Override
  public void expireRecycle(LocalDate expirationDate) {
    this.trashManager.cleanFiles(expirationDate);
  }
}
