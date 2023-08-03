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

package com.netease.arctic.server.optimizing.scan;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.data.IcebergContentFile;
import com.netease.arctic.data.IcebergDataFile;
import com.netease.arctic.data.IcebergDeleteFile;
import com.netease.arctic.scan.ChangeTableIncrementalScan;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.table.KeyedTableSnapshot;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotSummary;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeyedTableFileScanHelper implements TableFileScanHelper {
  private static final Logger LOG = LoggerFactory.getLogger(KeyedTableFileScanHelper.class);
  private final KeyedTable arcticTable;
  private PartitionFilter partitionFilter;

  private final long changeSnapshotId;
  private final long baseSnapshotId;
  private final StructLikeMap<Long> partitionOptimizedSequence;
  private final StructLikeMap<Long> legacyPartitionMaxTransactionId;

  public KeyedTableFileScanHelper(KeyedTable arcticTable, KeyedTableSnapshot snapshot) {
    this.arcticTable = arcticTable;
    this.baseSnapshotId = snapshot.baseSnapshotId();
    this.changeSnapshotId = snapshot.changeSnapshotId();
    this.partitionOptimizedSequence = snapshot.partitionOptimizedSequence();
    this.legacyPartitionMaxTransactionId = snapshot.legacyPartitionMaxTransactionId();
  }

  @Override
  public List<FileScanResult> scan() {
    LOG.info("{} start scan files with changeSnapshotId = {}, baseSnapshotId = {}", arcticTable.id(), changeSnapshotId,
        baseSnapshotId);
    long startTime = System.currentTimeMillis();

    List<FileScanResult> results = Lists.newArrayList();
    ChangeFiles changeFiles = new ChangeFiles(arcticTable);
    UnkeyedTable baseTable;
    baseTable = arcticTable.baseTable();
    ChangeTable changeTable = arcticTable.changeTable();
    if (changeSnapshotId != ArcticServiceConstants.INVALID_SNAPSHOT_ID) {
      long maxSequence = getMaxSequenceLimit(arcticTable, changeSnapshotId, partitionOptimizedSequence,
          legacyPartitionMaxTransactionId);
      if (maxSequence != Long.MIN_VALUE) {
        ChangeTableIncrementalScan changeTableIncrementalScan = changeTable.newScan()
            .fromSequence(partitionOptimizedSequence)
            .fromLegacyTransaction(legacyPartitionMaxTransactionId)
            .toSequence(maxSequence)
            .useSnapshot(changeSnapshotId);
        for (IcebergContentFile<?> icebergContentFile : changeTableIncrementalScan.planFilesWithSequence()) {
          changeFiles.addFile(wrapChangeFile(((IcebergDataFile) icebergContentFile)));
        }
        PartitionSpec partitionSpec = changeTable.spec();
        changeFiles.allInsertFiles().forEach(insertFile -> {
          if (partitionFilter != null) {
            StructLike partition = insertFile.partition();
            String partitionPath = partitionSpec.partitionToPath(partition);
            if (!partitionFilter.test(partitionPath)) {
              return;
            }
          }
          List<IcebergContentFile<?>> relatedDeleteFiles = changeFiles.getRelatedDeleteFiles(insertFile);
          results.add(new FileScanResult(insertFile, relatedDeleteFiles));
        });
      }
    }

    LOG.info("{} finish scan change files, cost {} ms, get {} insert files", arcticTable.id(),
        System.currentTimeMillis() - startTime, results.size());

    if (baseSnapshotId != ArcticServiceConstants.INVALID_SNAPSHOT_ID) {
      PartitionSpec partitionSpec = baseTable.spec();
      try (CloseableIterable<FileScanTask> filesIterable =
               baseTable.newScan().useSnapshot(baseSnapshotId).planFiles()) {
        for (FileScanTask task : filesIterable) {
          if (partitionFilter != null) {
            StructLike partition = task.file().partition();
            String partitionPath = partitionSpec.partitionToPath(partition);
            if (!partitionFilter.test(partitionPath)) {
              continue;
            }
          }
          IcebergDataFile dataFile = wrapBaseFile(task.file());
          List<IcebergContentFile<?>> deleteFiles =
              task.deletes().stream().map(this::wrapDeleteFile).collect(Collectors.toList());
          List<IcebergContentFile<?>> relatedChangeDeleteFiles = changeFiles.getRelatedDeleteFiles(dataFile);
          deleteFiles.addAll(relatedChangeDeleteFiles);
          results.add(new FileScanResult(dataFile, deleteFiles));
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to close table scan of " + arcticTable.id(), e);
      }
    }
    LOG.info("{} finish scan files, cost {} ms, get {} files", arcticTable.id(), System.currentTimeMillis() - startTime,
        results.size());
    return results;
  }

  @Override
  public KeyedTableFileScanHelper withPartitionFilter(PartitionFilter partitionFilter) {
    this.partitionFilter = partitionFilter;
    return this;
  }

  private IcebergDataFile wrapChangeFile(IcebergDataFile icebergDataFile) {
    DefaultKeyedFile defaultKeyedFile = DefaultKeyedFile.parseChange(icebergDataFile.internalFile(),
        icebergDataFile.getSequenceNumber());
    return new IcebergDataFile(defaultKeyedFile, icebergDataFile.getSequenceNumber());
  }

  private IcebergDataFile wrapBaseFile(DataFile dataFile) {
    DefaultKeyedFile defaultKeyedFile = DefaultKeyedFile.parseBase(dataFile);
    long transactionId = FileNameRules.parseTransactionId(dataFile.path().toString());
    return new IcebergDataFile(defaultKeyedFile, transactionId);
  }

  private IcebergContentFile<?> wrapDeleteFile(DeleteFile deleteFile) {
    long transactionId = FileNameRules.parseTransactionId(deleteFile.path().toString());
    return new IcebergDeleteFile(deleteFile, transactionId);
  }

  private long getMaxSequenceLimit(KeyedTable arcticTable,
                                   long changeSnapshotId,
                                   StructLikeMap<Long> partitionOptimizedSequence,
                                   StructLikeMap<Long> legacyPartitionMaxTransactionId) {
    ChangeTable changeTable = arcticTable.changeTable();
    Snapshot changeSnapshot = changeTable.snapshot(changeSnapshotId);
    int totalFilesInSummary = PropertyUtil
        .propertyAsInt(changeSnapshot.summary(), SnapshotSummary.TOTAL_DATA_FILES_PROP, 0);
    int maxFileCntLimit = CompatiblePropertyUtil.propertyAsInt(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT, TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT_DEFAULT);
    // not scan files to improve performance
    if (totalFilesInSummary <= maxFileCntLimit) {
      return Long.MAX_VALUE;
    }
    // scan and get all change files grouped by sequence(snapshot)
    ChangeTableIncrementalScan changeTableIncrementalScan =
        changeTable.newScan()
            .fromSequence(partitionOptimizedSequence)
            .fromLegacyTransaction(legacyPartitionMaxTransactionId)
            .useSnapshot(changeSnapshot.snapshotId());
    Map<Long, SnapshotFileGroup> changeFilesGroupBySequence = new HashMap<>();
    try (CloseableIterable<IcebergContentFile<?>> files = changeTableIncrementalScan.planFilesWithSequence()) {
      for (IcebergContentFile<?> file : files) {
        SnapshotFileGroup fileGroup =
            changeFilesGroupBySequence.computeIfAbsent(file.getSequenceNumber(), key -> {
              long txId = FileNameRules.parseChangeTransactionId(file.path().toString(), file.getSequenceNumber());
              return new SnapshotFileGroup(file.getSequenceNumber(), txId);
            });
        fileGroup.addFile();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + arcticTable.name(), e);
    }

    if (changeFilesGroupBySequence.isEmpty()) {
      LOG.debug("{} get no change files to optimize with partitionOptimizedSequence {}", arcticTable.name(),
          partitionOptimizedSequence);
      return Long.MIN_VALUE;
    }

    long maxSequence =
        getMaxSequenceKeepingTxIdInOrder(new ArrayList<>(changeFilesGroupBySequence.values()), maxFileCntLimit);
    if (maxSequence == Long.MIN_VALUE) {
      LOG.warn("{} get no change files with self-optimizing.max-file-count={}, change it to a bigger value",
          arcticTable.name(), maxFileCntLimit);
    } else if (maxSequence != Long.MAX_VALUE) {
      LOG.warn("{} not all change files optimized with self-optimizing.max-file-count={}, maxSequence={}",
          arcticTable.name(), maxFileCntLimit, maxSequence);
    }
    return maxSequence;
  }


  private static class ChangeFiles {
    private final KeyedTable arcticTable;
    private final Map<String, Map<DataTreeNode, List<IcebergContentFile<?>>>> cachedRelatedDeleteFiles =
        Maps.newHashMap();

    private final Map<String, Map<DataTreeNode, Set<IcebergContentFile<?>>>> equalityDeleteFiles = Maps.newHashMap();
    private final Map<String, Map<DataTreeNode, Set<IcebergDataFile>>> insertFiles = Maps.newHashMap();

    public ChangeFiles(KeyedTable arcticTable) {
      this.arcticTable = arcticTable;
    }

    public void addFile(IcebergDataFile file) {
      String partition = arcticTable.spec().partitionToPath(file.partition());
      DataTreeNode node = FileNameRules.parseFileNodeFromFileName(file.path().toString());
      DataFileType type = FileNameRules.parseFileTypeForChange(file.path().toString());
      switch (type) {
        case EQ_DELETE_FILE:
          equalityDeleteFiles.computeIfAbsent(partition, key -> Maps.newHashMap())
              .computeIfAbsent(node, key -> Sets.newHashSet())
              .add(file);
          break;
        case INSERT_FILE:
          insertFiles.computeIfAbsent(partition, key -> Maps.newHashMap())
              .computeIfAbsent(node, key -> Sets.newHashSet())
              .add(file);
          break;
        default:
          throw new IllegalStateException("illegal file type in change store " + type);
      }
    }

    public Stream<IcebergDataFile> allInsertFiles() {
      return insertFiles.values().stream()
          .flatMap(dataTreeNodeSetMap -> dataTreeNodeSetMap.values().stream())
          .flatMap(Collection::stream);
    }

    public List<IcebergContentFile<?>> getRelatedDeleteFiles(DataFile file) {
      String partition = arcticTable.spec().partitionToPath(file.partition());
      if (!equalityDeleteFiles.containsKey(partition)) {
        return Collections.emptyList();
      }
      DataTreeNode node = FileNameRules.parseFileNodeFromFileName(file.path().toString());
      Map<DataTreeNode, List<IcebergContentFile<?>>> partitionDeleteFiles =
          cachedRelatedDeleteFiles.computeIfAbsent(partition, key -> Maps.newHashMap());
      if (partitionDeleteFiles.containsKey(node)) {
        return partitionDeleteFiles.get(node);
      } else {
        List<IcebergContentFile<?>> result = Lists.newArrayList();
        for (Map.Entry<DataTreeNode, Set<IcebergContentFile<?>>> entry : equalityDeleteFiles.get(partition)
            .entrySet()) {
          DataTreeNode deleteNode = entry.getKey();
          if (node.isSonOf(deleteNode) || deleteNode.isSonOf(node)) {
            result.addAll(entry.getValue());
          }
        }
        partitionDeleteFiles.put(node, result);
        return result;
      }
    }
  }

  /**
   * Files grouped by snapshot, but only with the file cnt.
   */
  static class SnapshotFileGroup implements Comparable<SnapshotFileGroup> {
    private final long sequence;
    private final long transactionId;
    private int fileCnt = 0;

    public SnapshotFileGroup(long sequence, long transactionId) {
      this.sequence = sequence;
      this.transactionId = transactionId;
    }

    public SnapshotFileGroup(long sequence, long transactionId, int fileCnt) {
      this.sequence = sequence;
      this.transactionId = transactionId;
      this.fileCnt = fileCnt;
    }

    public void addFile() {
      fileCnt++;
    }

    public long getTransactionId() {
      return transactionId;
    }

    public int getFileCnt() {
      return fileCnt;
    }

    public long getSequence() {
      return sequence;
    }

    @Override
    public int compareTo(SnapshotFileGroup o) {
      return Long.compare(this.sequence, o.sequence);
    }
  }


  /**
   * Select all the files whose sequence <= maxSequence as Selected-Files, seek the maxSequence to find as many
   * Selected-Files as possible, and also
   * - the cnt of these Selected-Files must <= maxFileCntLimit
   * - the max TransactionId of the Selected-Files must > the min TransactionId of all the left files
   *
   * @param snapshotFileGroups snapshotFileGroups
   * @param maxFileCntLimit    maxFileCntLimit
   * @return the max sequence of selected file, return Long.MAX_VALUE if all files should be selected,
   * Long.MIN_VALUE means no files should be selected
   */
  static long getMaxSequenceKeepingTxIdInOrder(List<SnapshotFileGroup> snapshotFileGroups, long maxFileCntLimit) {
    if (maxFileCntLimit <= 0 || snapshotFileGroups == null || snapshotFileGroups.isEmpty()) {
      return Long.MIN_VALUE;
    }
    // 1.sort sequence
    Collections.sort(snapshotFileGroups);
    // 2.find the max index where all file cnt <= maxFileCntLimit
    int index = -1;
    int fileCnt = 0;
    for (int i = 0; i < snapshotFileGroups.size(); i++) {
      fileCnt += snapshotFileGroups.get(i).getFileCnt();
      if (fileCnt <= maxFileCntLimit) {
        index = i;
      } else {
        break;
      }
    }
    // all files cnt <= maxFileCntLimit, return all files
    if (fileCnt <= maxFileCntLimit) {
      return Long.MAX_VALUE;
    }
    // if only check the first file groups, then the file cnt > maxFileCntLimit, no files should be selected
    if (index == -1) {
      return Long.MIN_VALUE;
    }

    // 3.wrap file group with the max TransactionId before and min TransactionId after
    List<SnapshotFileGroupWrapper> snapshotFileGroupWrappers = wrapMinMaxTransactionId(snapshotFileGroups);
    // 4.find the valid snapshotFileGroup
    while (true) {
      SnapshotFileGroupWrapper current = snapshotFileGroupWrappers.get(index);
      // check transaction id inorder: max transaction id before(inclusive) < min transaction id after
      if (Math.max(current.getFileGroup().getTransactionId(), current.getMaxTransactionIdBefore()) <
          current.getMinTransactionIdAfter()) {
        return current.getFileGroup().getSequence();
      }
      index--;
      if (index == -1) {
        return Long.MIN_VALUE;
      }
    }
  }

  /**
   * Wrap SnapshotFileGroup with max and min Transaction Id
   */
  private static class SnapshotFileGroupWrapper {
    private final SnapshotFileGroup fileGroup;
    // in the ordered file group list, the max transaction before this file group, Long.MIN_VALUE for the first
    private long maxTransactionIdBefore;
    // in the ordered file group list, the min transaction after this file group, Long.MAX_VALUE for the last
    private long minTransactionIdAfter;

    public SnapshotFileGroupWrapper(SnapshotFileGroup fileGroup) {
      this.fileGroup = fileGroup;
    }

    public SnapshotFileGroup getFileGroup() {
      return fileGroup;
    }

    public long getMaxTransactionIdBefore() {
      return maxTransactionIdBefore;
    }

    public void setMaxTransactionIdBefore(long maxTransactionIdBefore) {
      this.maxTransactionIdBefore = maxTransactionIdBefore;
    }

    public long getMinTransactionIdAfter() {
      return minTransactionIdAfter;
    }

    public void setMinTransactionIdAfter(long minTransactionIdAfter) {
      this.minTransactionIdAfter = minTransactionIdAfter;
    }
  }

  private static List<SnapshotFileGroupWrapper> wrapMinMaxTransactionId(List<SnapshotFileGroup> snapshotFileGroups) {
    List<SnapshotFileGroupWrapper> wrappedList = new ArrayList<>();
    for (SnapshotFileGroup snapshotFileGroup : snapshotFileGroups) {
      wrappedList.add(new SnapshotFileGroupWrapper(snapshotFileGroup));
    }
    long maxValue = Long.MIN_VALUE;
    for (int i = 0; i < wrappedList.size(); i++) {
      SnapshotFileGroupWrapper wrapper = wrappedList.get(i);
      wrapper.setMaxTransactionIdBefore(maxValue);
      if (wrapper.getFileGroup().getTransactionId() > maxValue) {
        maxValue = wrapper.getFileGroup().getTransactionId();
      }
    }
    long minValue = Long.MAX_VALUE;
    for (int i = wrappedList.size() - 1; i >= 0; i--) {
      SnapshotFileGroupWrapper wrapper = wrappedList.get(i);
      wrapper.setMinTransactionIdAfter(minValue);
      if (wrapper.getFileGroup().getTransactionId() < minValue) {
        minValue = wrapper.getFileGroup().getTransactionId();
      }
    }
    return wrappedList;
  }
}
