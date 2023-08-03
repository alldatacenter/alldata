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
import com.netease.arctic.utils.TableFileUtil;
import org.apache.iceberg.io.FileInfo;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Basic implementation of {@link TableTrashManager}.
 */
class BasicTableTrashManager implements TableTrashManager {
  private static final Logger LOG = LoggerFactory.getLogger(BasicTableTrashManager.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
  private final TableIdentifier tableIdentifier;
  private final ArcticHadoopFileIO arcticFileIO;
  private final String tableRootLocation;
  private final String trashLocation;

  BasicTableTrashManager(
      TableIdentifier tableIdentifier, ArcticHadoopFileIO arcticFileIO,
      String tableRootLocation, String trashLocation) {
    this.tableIdentifier = tableIdentifier;
    this.arcticFileIO = arcticFileIO;
    this.tableRootLocation = tableRootLocation;
    this.trashLocation = trashLocation;
  }

  /**
   * Generate file location in trash
   *
   * @param relativeFileLocation - relative location of file
   * @param trashLocation        - trash location
   * @param deleteTime           - time the file deleted
   * @return file location in trash
   */
  @VisibleForTesting
  static String generateFileLocationInTrash(
      String relativeFileLocation, String trashLocation,
      long deleteTime) {
    return trashLocation + "/" + formatDate(deleteTime) + "/" + relativeFileLocation;
  }

  @VisibleForTesting
  static String getRelativeFileLocation(String tableRootLocation, String fileLocation) {
    tableRootLocation = TableFileUtil.getUriPath(tableRootLocation);
    fileLocation = TableFileUtil.getUriPath(fileLocation);
    if (!tableRootLocation.endsWith("/")) {
      tableRootLocation = tableRootLocation + "/";
    }
    Preconditions.checkArgument(
        fileLocation.startsWith(tableRootLocation),
        String.format("file %s is not in table location %s", fileLocation, tableRootLocation));
    return fileLocation.replaceFirst(tableRootLocation, "");
  }

  private static String formatDate(long time) {
    LocalDate localDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()).toLocalDate();
    return localDate.format(DATE_FORMATTER);
  }

  private static LocalDate parseDate(String formattedDate) {
    return LocalDate.parse(formattedDate, DATE_FORMATTER);
  }

  @Override
  public TableIdentifier tableId() {
    return tableIdentifier;
  }

  @Override
  public void moveFileToTrash(String path) {
    try {
      Preconditions.checkArgument(
          !arcticFileIO.supportFileSystemOperations() || !arcticFileIO.asFileSystemIO().isDirectory(path),
          "should not move a directory to trash " + path);
      String targetFileLocation = generateFileLocationInTrash(
          getRelativeFileLocation(this.tableRootLocation, path),
          this.trashLocation,
          System.currentTimeMillis());
      String targetFileDir = TableFileUtil.getFileDir(targetFileLocation);
      if (!arcticFileIO.exists(targetFileDir)) {
        arcticFileIO.makeDirectories(targetFileDir);
      }
      if (arcticFileIO.exists(targetFileLocation)) {
        arcticFileIO.deleteFile(targetFileLocation);
      }
      arcticFileIO.rename(path, targetFileLocation);
    } catch (Exception e) {
      LOG.error("{} failed to move file to trash, {}", tableIdentifier, path, e);
      throw e;
    }
  }

  @Override
  public boolean fileExistInTrash(String path) {
    return findFileFromTrash(path).isPresent();
  }

  @Override
  public boolean restoreFileFromTrash(String path) {
    Optional<String> fileFromTrash = findFileFromTrash(path);
    if (!fileFromTrash.isPresent()) {
      return false;
    }
    try {
      arcticFileIO.rename(fileFromTrash.get(), path);
      return true;
    } catch (Exception e) {
      LOG.info("{} failed to restore file, {}", tableIdentifier, path, e);
      return false;
    }
  }

  @Override
  public void cleanFiles(LocalDate expirationDate) {
    LOG.info("{} start clean files with expiration date {}", tableIdentifier, expirationDate);
    if (!arcticFileIO.exists(this.trashLocation)) {
      return;
    }
    Iterable<PathInfo> datePaths = arcticFileIO.listDirectory(this.trashLocation);

    for (FileInfo datePath : datePaths) {
      String dateName = TableFileUtil.getFileName(datePath.location());
      LocalDate localDate;
      try {
        localDate = parseDate(dateName);
      } catch (Exception e) {
        LOG.warn("{} failed to parse path to date {}", tableIdentifier, datePath.location(), e);
        continue;
      }
      if (localDate.isBefore(expirationDate)) {
        arcticFileIO.deletePrefix(datePath.location());
        LOG.info("{} delete files in trash for date {} success, {}", tableIdentifier, localDate,
            datePath.location());
      } else {
        LOG.info("{} should not delete files in trash for date {},  {}", tableIdentifier, localDate,
            datePath.location());
      }
    }
  }

  private Optional<String> findFileFromTrash(String path) {
    if (!arcticFileIO.exists(this.trashLocation)) {
      return Optional.empty();
    }
    String targetRelationLocationInTable = getRelativeFileLocation(this.tableRootLocation, path);

    Iterable<PathInfo> paths = arcticFileIO.listDirectory(this.trashLocation);

    List<String> targetLocationsInTrash = Lists.newArrayList();
    for (PathInfo p : paths) {
      String fullLocation = p.location() + "/" + targetRelationLocationInTable;
      if (arcticFileIO.exists(fullLocation)) {
        if (!arcticFileIO.isDirectory(fullLocation)) {
          targetLocationsInTrash.add(fullLocation);
        }
      }
    }

    return targetLocationsInTrash.stream()
        .max(Comparator.naturalOrder());
  }

  @Override
  public String getTrashLocation() {
    return trashLocation;
  }
}
