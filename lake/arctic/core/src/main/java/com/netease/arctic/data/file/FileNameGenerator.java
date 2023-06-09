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

package com.netease.arctic.data.file;

import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.io.writer.TaskWriterKey;
import com.netease.arctic.utils.IdGenerator;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.iceberg.FileFormat;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File name pattern:${tree_node_id}-${file_type}-${transaction_id}-${partition_id}-${task_id}-{operation_id}-{count}
 * <ul>
 *   <li>tree_node_id: id of {@link com.netease.arctic.data.DataTreeNode} the file belong</li>
 *   <li>file_type: short name of file's {@link com.netease.arctic.data.DataFileType} </li>
 *   <li>transaction_id: id of transaction the file added</li>
 *   <li>partition_id: id of partitioned data in parallel engine like spark & flink </li>
 *   <li>task_id: id of write task within partition</li>
 *   <li>operation_id: a random id to avoid duplicated file name</li>
 *   <li>count: auto increment count within writer </li>
 * </ul>
 * like:
 * <ul>
 *   <li>1-B-100-0-0-4217271085623029157-0.parquet</li>
 *   <li>4-ED-101-0-0-9009257362994691056-1.parquet</li>
 *   <li>4-I-101-0-0-9009257362994691056-2.parquet</li>
 * </ul>
 * 
 */
public class FileNameGenerator {

  private static final String KEYED_FILE_NAME_PATTERN_STRING = "(\\d+)-(\\w+)-(\\d+)-(\\d+)-(\\d+)-.*";
  private static final Pattern KEYED_FILE_NAME_PATTERN = Pattern.compile(KEYED_FILE_NAME_PATTERN_STRING);

  private static final String FORMAT = "%d-%s-%d-%05d-%d-%s-%05d";

  public static final DefaultKeyedFile.FileMeta
      DEFAULT_BASE_FILE_META = new DefaultKeyedFile.FileMeta(0, DataFileType.BASE_FILE, DataTreeNode.ROOT);

  private final FileFormat fileFormat;
  private final int partitionId;
  private final long taskId;
  private final long transactionId;

  // uuid avoid duplicated file name
  private final String operationId = IdGenerator.randomId() + "";
  private final AtomicLong fileCount = new AtomicLong(0);

  public FileNameGenerator(
      FileFormat fileFormat,
      int partitionId,
      Long taskId,
      Long transactionId) {
    this.fileFormat = fileFormat;
    this.partitionId = partitionId;
    this.taskId = taskId;
    this.transactionId = transactionId == null ? 0 : transactionId;
  }

  public String fileName(TaskWriterKey key) {
    return fileFormat.addExtension(
        String.format(FORMAT, key.getTreeNode().getId(), key.getFileType().shortName(),
            transactionId, partitionId, taskId, operationId, fileCount.incrementAndGet()));
  }

  /**
   * Parse FileMeta for ChangeStore.
   * Flink write transactionId as 0.
   * If it is not arctic file format or transactionId from path is 0, we set transactionId as iceberg sequenceNumber.
   *
   * @param path           file path
   * @param sequenceNumber iceberg sequenceNumber
   * @return fileMeta
   */
  public static DefaultKeyedFile.FileMeta parseChange(String path, long sequenceNumber) {
    String fileName = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(fileName);
    if (matchArcticFileFormat(matcher)) {
      DataFileType type;
      long transactionId;
      long nodeId = Long.parseLong(matcher.group(1));
      type = DataFileType.ofShortName(matcher.group(2));
      transactionId = Long.parseLong(matcher.group(3));
      transactionId = transactionId == 0 ? sequenceNumber : transactionId;
      DataTreeNode node = DataTreeNode.ofId(nodeId);
      return new DefaultKeyedFile.FileMeta(transactionId, type, node);
    } else {
      return new DefaultKeyedFile.FileMeta(sequenceNumber, DataFileType.INSERT_FILE, DataTreeNode.ROOT);
    }
  }

  /**
   * Parse FileMeta for BaseStore.
   * Path writen by hive can not be parsed by arctic file format, we set it to be DEFAULT_BASE_FILE_META.
   *
   * @param path - path
   * @return fileMeta
   */
  public static DefaultKeyedFile.FileMeta parseBase(String path) {
    String fileName = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(fileName);
    if (matchArcticFileFormat(matcher)) {
      long nodeId = Long.parseLong(matcher.group(1));
      DataFileType type = DataFileType.ofShortName(matcher.group(2));
      if (type == DataFileType.INSERT_FILE) {
        type = DataFileType.BASE_FILE;
      }
      long transactionId = Long.parseLong(matcher.group(3));
      DataTreeNode node = DataTreeNode.ofId(nodeId);
      return new DefaultKeyedFile.FileMeta(transactionId, type, node);
    } else {
      return DEFAULT_BASE_FILE_META;
    }
  }

  /**
   * Parse file type for ChangeStore.
   *
   * @param path - path
   * @return file type, return INSERT_FILE if is not arctic file format
   */
  public static DataFileType parseFileTypeForChange(String path) {
    String fileName = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(fileName);
    if (matchArcticFileFormat(matcher)) {
      return DataFileType.ofShortName(matcher.group(2));
    } else {
      return DataFileType.INSERT_FILE;
    }
  }

  /**
   * Parse file type for BaseStore.
   *
   * @param path - path
   * @return file type, return BASE_FILE if is not arctic file format
   */
  public static DataFileType parseFileTypeForBase(String path) {
    String fileName = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(fileName);
    if (matchArcticFileFormat(matcher)) {
      DataFileType type;
      type = DataFileType.ofShortName(matcher.group(2));
      if (type == DataFileType.INSERT_FILE) {
        type = DataFileType.BASE_FILE;
      }
      return type;
    } else {
      return DataFileType.BASE_FILE;
    }
  }

  /**
   * Parse file type.
   *
   * @param path      - path
   * @param tableType - table type, base/change
   * @return file type
   */
  public static DataFileType parseFileType(String path, String tableType) {
    if (Constants.INNER_TABLE_CHANGE.equals(tableType)) {
      return parseFileTypeForChange(path);
    } else if (Constants.INNER_TABLE_BASE.equals(tableType)) {
      return parseFileTypeForBase(path);
    } else {
      throw new IllegalArgumentException("unknown tableType " + tableType);
    }
  }

  /**
   * Parse keyed file node id from file name.
   *
   * @param path path
   * @return node, return node(0,0) if path is not arctic file format.
   */
  public static DataTreeNode parseFileNodeFromFileName(String path) {
    path = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(path);
    if (matchArcticFileFormat(matcher)) {
      long nodeId = Long.parseLong(matcher.group(1));
      return DataTreeNode.ofId(nodeId);
    } else {
      return DataTreeNode.ROOT;
    }
  }

  /**
   * Parse transaction id from file name.
   *
   * @param path path
   * @return transactionId, return 0 if path is not arctic file format.
   */
  public static long parseTransactionId(String path) {
    String fileName = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(fileName);
    if (matchArcticFileFormat(matcher)) {
      return Long.parseLong(matcher.group(3));
    } else {
      return 0L;
    }
  }

  /**
   * Parse transaction id of change file.
   *
   * @param path path
   * @return transactionId, return 0 if path is not arctic file format.
   */
  public static long parseChangeTransactionId(String path, long sequenceNumber) {
    long transactionId = parseTransactionId(path);
    return transactionId == 0 ? sequenceNumber : transactionId;
  }

  /**
   * Check if is arctic file format.
   *
   * @param path - path
   * @return true if is arctic file format
   */
  public static boolean isArcticFileFormat(String path) {
    String fileName = TableFileUtils.getFileName(path);
    Matcher matcher = KEYED_FILE_NAME_PATTERN.matcher(fileName);
    return matchArcticFileFormat(matcher);
  }


  private static boolean matchArcticFileFormat(Matcher fileNameMatcher) {
    return fileNameMatcher.matches() && validFileType(fileNameMatcher.group(2));
  }

  private static boolean validFileType(String typeName) {
    try {
      DataFileType.ofShortName(typeName);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
