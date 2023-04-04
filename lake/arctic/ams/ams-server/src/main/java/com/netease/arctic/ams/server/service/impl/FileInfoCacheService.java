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

package com.netease.arctic.ams.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.DataFile;
import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.api.PartitionFieldData;
import com.netease.arctic.ams.api.TableChange;
import com.netease.arctic.ams.api.TableCommitMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.mapper.FileInfoCacheMapper;
import com.netease.arctic.ams.server.mapper.SnapInfoCacheMapper;
import com.netease.arctic.ams.server.model.AMSDataFileInfo;
import com.netease.arctic.ams.server.model.CacheFileInfo;
import com.netease.arctic.ams.server.model.CacheSnapshotInfo;
import com.netease.arctic.ams.server.model.PartitionBaseInfo;
import com.netease.arctic.ams.server.model.PartitionFileBaseInfo;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.model.TransactionsOfTable;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.ams.server.service.IMetaService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.CatalogUtil;
import com.netease.arctic.ams.server.utils.TableMetadataUtil;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.trace.SnapshotSummary;
import com.netease.arctic.utils.ConvertStructUtil;
import com.netease.arctic.utils.SnapshotFileUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.DataOperations;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.hash.Hashing;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileInfoCacheService extends IJDBCService {

  private static final Logger LOG = LoggerFactory.getLogger(FileInfoCacheService.class);

  public void commitCacheFileInfo(TableCommitMeta tableCommitMeta) {
    if (needFixCacheFromTable(tableCommitMeta)) {
      LOG.warn("should not cache {}", tableCommitMeta);
      return;
    }

    List<CacheFileInfo> fileInfoList = genFileInfo(tableCommitMeta);
    List<CacheSnapshotInfo> cacheSnapInfoList = genSnapInfo(tableCommitMeta);

    try (SqlSession sqlSession = getSqlSession(false)) {
      try {
        FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
        fileInfoList.stream().filter(e -> e.getDeleteSnapshotId() == null)
            .forEach(fileInfoCacheMapper::insertCache);
        LOG.info("insert {} files into file cache", fileInfoList.stream().filter(e -> e.getDeleteSnapshotId() == null)
            .count());

        fileInfoList.stream().filter(e -> e.getDeleteSnapshotId() != null).forEach(fileInfoCacheMapper::updateCache);
        LOG.info("update {} files in file cache", fileInfoList.stream().filter(e -> e.getDeleteSnapshotId() != null)
            .count());

        SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
        cacheSnapInfoList.forEach(snapInfoCacheMapper::insertCache);
        LOG.info("insert {} snapshot into snapshot cache", cacheSnapInfoList.size());

        sqlSession.commit();
      } catch (Exception e) {
        sqlSession.rollback();
        LOG.error("insert file cache {} error", JSONObject.toJSONString(tableCommitMeta), e);
      }
    } catch (Exception e) {
      LOG.error("insert file cache {} error", JSONObject.toJSONString(tableCommitMeta), e);
    }
  }

  public List<DataFileInfo> getOptimizeDatafiles(TableIdentifier tableIdentifier, String tableType) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      return fileInfoCacheMapper.getOptimizeDatafiles(tableIdentifier, tableType);
    }
  }

  public List<DataFileInfo> getOptimizeDatafilesWithSnapshot(TableIdentifier tableIdentifier, String tableType,
                                                             Snapshot snapshot) {
    Preconditions.checkNotNull(snapshot, "snapshot should not be null");
    try (SqlSession sqlSession = getSqlSession(false)) {
      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      if (!snapInfoCacheMapper.snapshotIsCached(tableIdentifier, tableType, snapshot.snapshotId())) {
        throw new IllegalArgumentException("snapshot is not cached " + snapshot.snapshotId());
      }
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      return fileInfoCacheMapper.getOptimizeDatafilesWithSnapshot(tableIdentifier, tableType,
          snapshot.sequenceNumber());
    }
  }

  public List<DataFileInfo> getChangeTableTTLDataFiles(TableIdentifier tableIdentifier, long ttl) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      return fileInfoCacheMapper.getChangeTableTTLDataFiles(tableIdentifier, Constants.INNER_TABLE_CHANGE, ttl);
    }
  }

  public Long getCachedMaxTime(TableIdentifier identifier, String innerTable) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      Timestamp maxTime = snapInfoCacheMapper.getCachedMaxTime(identifier, innerTable);
      return maxTime == null ? 0 : maxTime.getTime();
    }
  }

  public Boolean snapshotIsCached(TableIdentifier identifier, String innerTable, Long snapshotId) {
    if (snapshotId == -1) {
      return true;
    }
    try (SqlSession sqlSession = getSqlSession(true)) {
      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      return snapInfoCacheMapper.snapshotIsCached(identifier, innerTable, snapshotId);
    }
  }

  /**
   * @param time delete all cache which commit time less than time and is deleted
   */
  public void expiredCache(long time) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      fileInfoCacheMapper.expireCache(time);
      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      List<TableMetadata> tableMetadata = ServiceContainer.getMetaService().listTables();
      tableMetadata.forEach(meta -> {
        snapInfoCacheMapper.expireCache(time, meta.getTableIdentifier().buildTableIdentifier(), "base");
        snapInfoCacheMapper.expireCache(time, meta.getTableIdentifier().buildTableIdentifier(), "change");
      });
    }
  }

  public void syncTableFileInfo(TableIdentifier identifier, String tableType) {
    LOG.info("start sync table {} file info", identifier);
    try {
      // load table
      Table table = null;
      try {
        AmsClient client = ServiceContainer.getTableMetastoreHandler();
        ArcticCatalog catalog = CatalogLoader.load(client, identifier.getCatalog());
        com.netease.arctic.table.TableIdentifier tmp = com.netease.arctic.table.TableIdentifier.of(
            identifier.getCatalog(),
            identifier.getDatabase(),
            identifier.getTableName());
        ArcticTable arcticTable = catalog.loadTable(tmp);
        if (arcticTable.isUnkeyedTable()) {
          table = arcticTable.asUnkeyedTable();
        } else {
          if (Constants.INNER_TABLE_CHANGE.equalsIgnoreCase(tableType)) {
            table = arcticTable.asKeyedTable().changeTable();
          } else {
            table = arcticTable.asKeyedTable().baseTable();
          }
        }
      } catch (Exception e) {
        LOG.warn(
            String.format("load table error when sync file info cache:%s.%s.%s",
                identifier.getCatalog(), identifier.getDatabase(), identifier.getTableName()),
            e);
      }

      // get snapshot info
      if (table == null) {
        return;
      }
      if (table.currentSnapshot() == null) {
        return;
      }

      boolean isCached = false;
      List<Snapshot> snapshots = new ArrayList<>();
      Snapshot curr = table.currentSnapshot();
      while (curr != null) {
        isCached = snapshotIsCached(identifier, tableType, curr.snapshotId());
        if (isCached) {
          break;
        }
        snapshots.add(curr);

        if (curr.parentId() == null || curr.parentId() == -1L) {
          break;
        }
        curr = table.snapshot(curr.parentId());
      }
      if (!isCached) {
        //there is snapshot expired and not in cache.need delete all cache,and cache current snapshot
        deleteInnerTableCache(new com.netease.arctic.table.TableIdentifier(identifier), tableType);
        syncCurrentSnapshotFile(table, identifier, tableType);
        return;
      }

      // generate cache info
      LOG.info("{} start sync file info", identifier);
      Table finalTable = table;
      if (snapshots.size() > 0) {
        ((ArcticTable) finalTable).io().doAs(() -> {
          syncFileInfo(finalTable, identifier, tableType, Lists.reverse(snapshots));
          return null;
        });
      }
    } catch (Exception e) {
      LOG.error("sync cache info error " + identifier, e);
    }
  }

  public void deleteTableCache(com.netease.arctic.table.TableIdentifier identifier) {
    TableIdentifier tableIdentifier = new TableIdentifier();
    tableIdentifier.catalog = identifier.getCatalog();
    tableIdentifier.database = identifier.getDatabase();
    tableIdentifier.tableName = identifier.getTableName();
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      fileInfoCacheMapper.deleteTableCache(tableIdentifier);

      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      snapInfoCacheMapper.deleteTableCache(tableIdentifier);
    } catch (Exception e) {
      LOG.error("delete table file cache error ", e);
      throw e;
    }
  }

  public void deleteInnerTableCache(com.netease.arctic.table.TableIdentifier identifier, String innerTable) {
    TableIdentifier tableIdentifier = new TableIdentifier();
    tableIdentifier.catalog = identifier.getCatalog();
    tableIdentifier.database = identifier.getDatabase();
    tableIdentifier.tableName = identifier.getTableName();
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      fileInfoCacheMapper.deleteInnerTableCache(tableIdentifier, innerTable);

      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      snapInfoCacheMapper.deleteInnerTableCache(tableIdentifier, innerTable);
    } catch (Exception e) {
      LOG.error("delete table file cache error ", e);
      throw e;
    }
  }

  private boolean needFixCacheFromTable(TableCommitMeta tableCommitMeta) {
    if (CollectionUtils.isNotEmpty(tableCommitMeta.getChanges())) {
      TableChange tableChange = tableCommitMeta.getChanges().get(0);
      if (tableChange.getParentSnapshotId() == -1) {
        return false;
      }
      return !(snapshotIsCached(tableCommitMeta.getTableIdentifier(), tableChange.getInnerTable(),
          tableChange.getParentSnapshotId()) &&
          !snapshotIsCached(tableCommitMeta.getTableIdentifier(), tableChange.getInnerTable(),
              tableChange.getSnapshotId()));
    }
    return true;
  }

  private void syncFileInfo(
      Table table,
      TableIdentifier identifier,
      String tableType,
      List<Snapshot> snapshots) {
    Iterator<Snapshot> iterator = snapshots.iterator();
    while (iterator.hasNext()) {
      Snapshot snapshot = iterator.next();
      List<CacheFileInfo> fileInfos = new ArrayList<>();
      List<DataFile> addFiles = new ArrayList<>();
      List<DataFile> deleteFiles = new ArrayList<>();
      SnapshotFileUtil.getSnapshotFiles((ArcticTable) table, tableType, snapshot, addFiles, deleteFiles);
      for (DataFile amsFile : addFiles) {
        CacheFileInfo cacheFileInfo = CacheFileInfo.convert(amsFile, identifier, tableType, snapshot);
        fileInfos.add(cacheFileInfo);
      }

      for (DataFile amsFile : deleteFiles) {
        CacheFileInfo cacheFileInfo = new CacheFileInfo();
        String partitionName = StringUtils.isEmpty(partitionToPath(amsFile.getPartition())) ?
            "" :
            partitionToPath(amsFile.getPartition());
        cacheFileInfo.setDeleteSnapshotId(snapshot.snapshotId());
        String primaryKey =
            TableMetadataUtil.getTableAllIdentifyName(identifier) + tableType + amsFile.getPath() + partitionName;
        String primaryKeyMd5 = Hashing.md5()
            .hashBytes(primaryKey.getBytes(StandardCharsets.UTF_8))
            .toString();
        cacheFileInfo.setPrimaryKeyMd5(primaryKeyMd5);
        fileInfos.add(cacheFileInfo);
      }

      long fileSize = 0L;
      int fileCount = 0;
      for (DataFile file : addFiles) {
        fileSize += file.getFileSize();
        fileCount++;
      }
      for (DataFile file : deleteFiles) {
        fileSize += file.getFileSize();
        fileCount++;
      }
      CacheSnapshotInfo snapshotInfo = syncSnapInfo(identifier, tableType, snapshot, fileSize, fileCount);
      //remove snapshot to release memory of snapshot, because there is too much cache in BaseSnapshot
      iterator.remove();

      try (SqlSession sqlSession = getSqlSession(false)) {
        try {
          FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
          fileInfos.stream().filter(e -> e.getDeleteSnapshotId() == null).forEach(fileInfoCacheMapper::insertCache);
          fileInfos.stream().filter(e -> e.getDeleteSnapshotId() != null).forEach(fileInfoCacheMapper::updateCache);

          SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
          snapInfoCacheMapper.insertCache(snapshotInfo);
          sqlSession.commit();
        } catch (Exception e) {
          sqlSession.rollback();
          LOG.error(
              "insert table {} file {} cache error",
              identifier,
              JSONObject.toJSONString(fileInfos),
              e);
        }
      } catch (Exception e) {
        LOG.error(
            "insert table {} file {} cache error",
            identifier,
            JSONObject.toJSONString(fileInfos),
            e);
      }
    }
  }

  private void syncCurrentSnapshotFile(Table table, TableIdentifier identifier, String tableType) {
    Set<org.apache.iceberg.DataFile> dataFiles = new HashSet<>();
    Set<String> addedDeleteFiles = new HashSet<>();
    Set<org.apache.iceberg.DeleteFile> deleteFiles = new HashSet<>();
    Snapshot curr = table.currentSnapshot();
    try (CloseableIterable<FileScanTask> fileScanTasks = table.newScan().planFiles()) {
      fileScanTasks.forEach(fileScanTask -> {
        dataFiles.add(fileScanTask.file());
        fileScanTask.deletes().forEach(deleteFile -> {
          if (!addedDeleteFiles.contains(deleteFile.path().toString())) {
            deleteFiles.add(deleteFile);
            addedDeleteFiles.add(deleteFile.path().toString());
          }
        });
      });
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + table.name(), e);
    }
    List<CacheFileInfo> cacheFileInfos = new ArrayList<>();
    dataFiles.forEach(dataFile -> {
      DataFile amsFile = ConvertStructUtil.convertToAmsDatafile(dataFile, (ArcticTable) table, tableType);
      cacheFileInfos.add(CacheFileInfo.convert(amsFile, identifier, tableType, curr));
    });
    deleteFiles.forEach(dataFile -> {
      DataFile amsFile = ConvertStructUtil.convertToAmsDatafile(dataFile, (ArcticTable) table, tableType);
      cacheFileInfos.add(CacheFileInfo.convert(amsFile, identifier, tableType, curr));
    });

    long fileSize = 0L;
    int fileCount = 0;
    for (CacheFileInfo file : cacheFileInfos) {
      fileSize += file.getFileSize();
      fileCount++;
    }
    CacheSnapshotInfo snapshotInfo = syncSnapInfo(identifier, tableType, curr, fileSize, fileCount);
    try (SqlSession sqlSession = getSqlSession(false)) {
      try {
        FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
        cacheFileInfos.forEach(fileInfoCacheMapper::insertCache);

        SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
        snapInfoCacheMapper.insertCache(snapshotInfo);
        sqlSession.commit();
      } catch (Exception e) {
        sqlSession.rollback();
      }
    } catch (Exception e) {
      LOG.error(
          "insert table {} file {} cache error",
          identifier,
          JSONObject.toJSONString(cacheFileInfos),
          e);
    }
  }

  private List<CacheFileInfo> genFileInfo(TableCommitMeta tableCommitMeta) {
    List<CacheFileInfo> rs = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(tableCommitMeta.getChanges())) {
      tableCommitMeta.getChanges().forEach(tableChange -> {
        if (CollectionUtils.isNotEmpty(tableChange.getAddFiles())) {
          tableChange.getAddFiles().forEach(datafile -> {
            CacheFileInfo cacheFileInfo = new CacheFileInfo();
            cacheFileInfo.setTableIdentifier(tableCommitMeta.getTableIdentifier());
            cacheFileInfo.setAddSnapshotId(tableChange.getSnapshotId());
            cacheFileInfo.setAddSnapshotSequence(tableChange.getSnapshotSequence());
            cacheFileInfo.setParentSnapshotId(tableChange.getParentSnapshotId());
            cacheFileInfo.setInnerTable(tableChange.getInnerTable());
            cacheFileInfo.setFilePath(datafile.getPath());
            cacheFileInfo.setFileType(datafile.getFileType());
            String partitionName = StringUtils.isEmpty(partitionToPath(datafile.getPartition())) ?
                "" :
                partitionToPath(datafile.getPartition());
            cacheFileInfo.setPartitionName(partitionName);
            String primaryKey = TableMetadataUtil.getTableAllIdentifyName(tableCommitMeta.getTableIdentifier()) +
                tableChange.getInnerTable() + datafile.getPath() + partitionName;
            String primaryKeyMd5 = Hashing.md5()
                .hashBytes(primaryKey.getBytes(StandardCharsets.UTF_8))
                .toString();
            cacheFileInfo.setPrimaryKeyMd5(primaryKeyMd5);
            cacheFileInfo.setFileSize(datafile.getFileSize());
            cacheFileInfo.setFileMask(datafile.getMask());
            cacheFileInfo.setFileIndex(datafile.getIndex());
            cacheFileInfo.setRecordCount(datafile.getRecordCount());
            cacheFileInfo.setSpecId(datafile.getSpecId());
            cacheFileInfo.setAction(tableCommitMeta.getAction());
            cacheFileInfo.setCommitTime(tableCommitMeta.getCommitTime());
            cacheFileInfo.setProducer(tableCommitMeta.getCommitMetaProducer().name());
            rs.add(cacheFileInfo);
          });
        }

        if (CollectionUtils.isNotEmpty(tableChange.getDeleteFiles())) {
          tableChange.getDeleteFiles().forEach(datafile -> {
            CacheFileInfo cacheFileInfo = new CacheFileInfo();
            String partitionName = StringUtils.isEmpty(partitionToPath(datafile.getPartition())) ?
                "" :
                partitionToPath(datafile.getPartition());
            String primaryKey = TableMetadataUtil.getTableAllIdentifyName(tableCommitMeta.getTableIdentifier()) +
                tableChange.getInnerTable() + datafile.getPath() + partitionName;
            String primaryKeyMd5 = Hashing.md5()
                .hashBytes(primaryKey.getBytes(StandardCharsets.UTF_8))
                .toString();
            cacheFileInfo.setPrimaryKeyMd5(primaryKeyMd5);
            cacheFileInfo.setDeleteSnapshotId(tableChange.getSnapshotId());
            rs.add(cacheFileInfo);
          });
        }
      });
    }
    return rs;
  }

  private CacheSnapshotInfo syncSnapInfo(
      TableIdentifier identifier, String tableType, Snapshot snapshot,
      long fileSize, int fileCount) {
    CacheSnapshotInfo cache = new CacheSnapshotInfo();
    cache.setTableIdentifier(identifier);
    cache.setSnapshotId(snapshot.snapshotId());
    cache.setSnapshotSequence(snapshot.sequenceNumber());
    cache.setParentSnapshotId(snapshot.parentId() == null ? -1 : snapshot.parentId());
    cache.setAction(snapshot.operation());
    cache.setInnerTable(tableType);
    cache.setCommitTime(snapshot.timestampMillis());
    cache.setProducer(snapshot.summary()
        .getOrDefault(SnapshotSummary.SNAPSHOT_PRODUCER, SnapshotSummary.SNAPSHOT_PRODUCER_DEFAULT));
    cache.setFileSize(fileSize);
    cache.setFileCount(fileCount);
    return cache;
  }

  private List<CacheSnapshotInfo> genSnapInfo(TableCommitMeta tableCommitMeta) {
    List<CacheSnapshotInfo> rs = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(tableCommitMeta.getChanges())) {
      tableCommitMeta.getChanges().forEach(tableChange -> {
        CacheSnapshotInfo cache = new CacheSnapshotInfo();
        cache.setTableIdentifier(tableCommitMeta.getTableIdentifier());
        cache.setSnapshotId(tableChange.getSnapshotId());
        cache.setSnapshotSequence(tableChange.getSnapshotSequence());
        cache.setParentSnapshotId(tableChange.getParentSnapshotId());
        cache.setAction(tableCommitMeta.getAction());
        cache.setInnerTable(tableChange.getInnerTable());
        cache.setCommitTime(tableCommitMeta.getCommitTime());
        cache.setProducer(tableCommitMeta.getCommitMetaProducer().name());
        long fileSize = 0L;
        int fileCount = 0;
        if (CollectionUtils.isNotEmpty(tableChange.getAddFiles())) {
          for (DataFile file : tableChange.getAddFiles()) {
            fileSize += file.getFileSize();
            fileCount++;
          }
        }
        if (CollectionUtils.isNotEmpty(tableChange.getDeleteFiles())) {
          for (DataFile file : tableChange.getDeleteFiles()) {
            fileSize += file.getFileSize();
            fileCount++;
          }
        }
        cache.setFileSize(fileSize);
        cache.setFileCount(fileCount);
        rs.add(cache);
      });
    }
    return rs;
  }

  public List<TransactionsOfTable> getTxExcludeOptimize(TableIdentifier identifier) {
    return getTxExcludeOptimize(identifier, true);
  }

  public List<TransactionsOfTable> getTxExcludeOptimize(TableIdentifier identifier, boolean ignoreEmptyTransaction) {
    if (CatalogUtil.isIcebergCatalog(identifier.getCatalog())) {
      List<TransactionsOfTable> result = new ArrayList<>();
      ArcticCatalog catalog = CatalogUtil.getArcticCatalog(identifier.getCatalog());
      ArcticTable arcticTable = catalog.loadTable(com.netease.arctic.table.TableIdentifier.of(identifier.getCatalog(),
          identifier.getDatabase(), identifier.getTableName()));
      arcticTable.asUnkeyedTable().snapshots().forEach(snapshot -> {
        if (snapshot.operation().equals(DataOperations.REPLACE)) {
          return;
        }
        TransactionsOfTable transactionsOfTable = new TransactionsOfTable();
        transactionsOfTable.setTransactionId(snapshot.snapshotId());
        int fileCount = PropertyUtil
            .propertyAsInt(snapshot.summary(), org.apache.iceberg.SnapshotSummary.ADDED_FILES_PROP, 0);
        fileCount += PropertyUtil
            .propertyAsInt(snapshot.summary(), org.apache.iceberg.SnapshotSummary.ADDED_DELETE_FILES_PROP, 0);
        fileCount += PropertyUtil
            .propertyAsInt(snapshot.summary(), org.apache.iceberg.SnapshotSummary.DELETED_FILES_PROP, 0);
        fileCount += PropertyUtil
            .propertyAsInt(snapshot.summary(), org.apache.iceberg.SnapshotSummary.REMOVED_DELETE_FILES_PROP, 0);
        transactionsOfTable.setFileCount(fileCount);
        transactionsOfTable.setFileSize(PropertyUtil
            .propertyAsLong(snapshot.summary(), org.apache.iceberg.SnapshotSummary.ADDED_FILE_SIZE_PROP, 0) +
            PropertyUtil
                .propertyAsLong(snapshot.summary(), org.apache.iceberg.SnapshotSummary.REMOVED_FILE_SIZE_PROP, 0));
        transactionsOfTable.setCommitTime(snapshot.timestampMillis());
        result.add(transactionsOfTable);
      });
      Collections.reverse(result);
      return result;
    }
    try (SqlSession sqlSession = getSqlSession(true)) {
      SnapInfoCacheMapper snapInfoCacheMapper = getMapper(sqlSession, SnapInfoCacheMapper.class);
      List<TransactionsOfTable> transactions = snapInfoCacheMapper.getTxExcludeOptimize(identifier);
      if (ignoreEmptyTransaction) {
        return transactions.stream().filter(t -> t.getFileCount() > 0 || t.getFileSize() > 0)
            .collect(Collectors.toList());
      } else {
        return transactions;
      }
    }
  }

  public List<AMSDataFileInfo> getDatafilesInfo(TableIdentifier identifier, Long transactionId) {
    if (CatalogUtil.isIcebergCatalog(identifier.getCatalog())) {
      return genIcebergFileInfo(identifier, transactionId);
    }
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      return fileInfoCacheMapper.getDatafilesInfo(identifier, transactionId);
    }
  }

  public List<AMSDataFileInfo> genIcebergFileInfo(TableIdentifier identifier, Long transactionId) {
    List<AMSDataFileInfo> result = new ArrayList<>();
    ArcticCatalog catalog = CatalogUtil.getArcticCatalog(identifier.getCatalog());
    ArcticTable arcticTable = catalog.loadTable(com.netease.arctic.table.TableIdentifier.of(identifier.getCatalog(),
        identifier.getDatabase(), identifier.getTableName()));
    List<DeleteFile> addFiles = new ArrayList<>();
    List<DeleteFile> deleteFiles = new ArrayList<>();
    Snapshot snapshot = arcticTable.asUnkeyedTable().snapshot(transactionId);
    SnapshotFileUtil.getDeleteFiles(arcticTable, snapshot, addFiles, deleteFiles);
    snapshot.addedFiles().forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          partitionToPath(ConvertStructUtil.partitionFields(arcticTable.spec(), f.partition())),
          getIcebergFileType(f.content()),
          f.fileSizeInBytes(),
          snapshot.timestampMillis(),
          "add"));
    });
    snapshot.deletedFiles().forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          partitionToPath(ConvertStructUtil.partitionFields(arcticTable.spec(), f.partition())),
          getIcebergFileType(f.content()),
          f.fileSizeInBytes(),
          snapshot.timestampMillis(),
          "remove"));
    });
    addFiles.forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          partitionToPath(ConvertStructUtil.partitionFields(arcticTable.spec(), f.partition())),
          getIcebergFileType(f.content()),
          f.fileSizeInBytes(),
          snapshot.timestampMillis(),
          "add"));
    });
    deleteFiles.forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          partitionToPath(ConvertStructUtil.partitionFields(arcticTable.spec(), f.partition())),
          getIcebergFileType(f.content()),
          f.fileSizeInBytes(),
          snapshot.timestampMillis(),
          "remove"));
    });
    return result;
  }

  private static String getIcebergFileType(FileContent fileContent) {
    switch (fileContent) {
      case DATA:
        return "data";
      case EQUALITY_DELETES:
        return "eq-deletes";
      case POSITION_DELETES:
        return "pos-deletes";
      default:
        throw new UnsupportedOperationException("unknown fileContent " + fileContent);
    }
  }

  public List<PartitionBaseInfo> getPartitionBaseInfoList(TableIdentifier tableIdentifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      return fileInfoCacheMapper.getPartitionBaseInfoList(tableIdentifier);
    }
  }

  public List<PartitionFileBaseInfo> getPartitionFileList(TableIdentifier tableIdentifier, String partition) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      FileInfoCacheMapper fileInfoCacheMapper = getMapper(sqlSession, FileInfoCacheMapper.class);
      return fileInfoCacheMapper.getPartitionFileList(tableIdentifier, partition);
    }
  }

  private String partitionToPath(List<PartitionFieldData> partitionFieldDataList) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < partitionFieldDataList.size(); i++) {
      if (i > 0) {
        sb.append("/");
      }
      sb.append(partitionFieldDataList.get(i).getName()).append("=")
          .append(partitionFieldDataList.get(i).getValue());
    }
    return sb.toString();
  }

  public static class SyncAndExpireFileCacheTask {

    public static final Logger LOG = LoggerFactory.getLogger(SyncAndExpireFileCacheTask.class);

    private final FileInfoCacheService fileInfoCacheService;
    private final IMetaService metaService;

    public SyncAndExpireFileCacheTask() {
      this.fileInfoCacheService = ServiceContainer.getFileInfoCacheService();
      this.metaService = ServiceContainer.getMetaService();
    }

    public void doTask() {
      LOG.info("start execute doTask");
      expiredCache();
      syncCache();
    }

    private void expiredCache() {
      LOG.info("start execute expiredCache");
      fileInfoCacheService.expiredCache(
          System.currentTimeMillis() - ArcticMetaStore.conf.getLong(ArcticMetaStoreConf.FILE_CACHE_EXPIRED_INTERVAL));
    }

    private void syncCache() {
      LOG.info("start execute syncCache");
      List<TableMetadata> tableMetadata = metaService.listTables();
      long lowTime = System.currentTimeMillis() -
          ArcticMetaStore.conf.getLong(ArcticMetaStoreConf.TABLE_FILE_INFO_CACHE_INTERVAL);
      tableMetadata.forEach(meta -> {
        if (meta.getTableIdentifier() == null) {
          return;
        }
        TableIdentifier tableIdentifier = new TableIdentifier();
        tableIdentifier.catalog = meta.getTableIdentifier().getCatalog();
        tableIdentifier.database = meta.getTableIdentifier().getDatabase();
        tableIdentifier.tableName = meta.getTableIdentifier().getTableName();
        try {
          ArcticCatalog catalog =
              CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), tableIdentifier.getCatalog());
          com.netease.arctic.table.TableIdentifier tmp = com.netease.arctic.table.TableIdentifier.of(
              tableIdentifier.getCatalog(),
              tableIdentifier.getDatabase(),
              tableIdentifier.getTableName());
          ArcticTable arcticTable = catalog.loadTable(tmp);
          doSync(tableIdentifier, Constants.INNER_TABLE_BASE, lowTime);
          if (arcticTable.isKeyedTable()) {
            doSync(tableIdentifier, Constants.INNER_TABLE_CHANGE, lowTime);
          }
        } catch (Exception e) {
          LOG.error(
              String.format("SyncAndExpireFileCacheTask sync cache error %s.%s.%s",
                  tableIdentifier.catalog, tableIdentifier.database, tableIdentifier.tableName),
              e);
        }
      });
    }

    private void doSync(TableIdentifier tableIdentifier, String innerTable, long lowTime) {
      try {
        Long maxCommitTime = fileInfoCacheService.getCachedMaxTime(tableIdentifier, innerTable);
        if (maxCommitTime == null || maxCommitTime < lowTime) {
          fileInfoCacheService.syncTableFileInfo(tableIdentifier, innerTable);
        }
      } catch (Exception e) {
        LOG.error("period sync file cache error", e);
      }
    }
  }
}
