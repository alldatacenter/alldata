package com.netease.arctic.server.dashboard;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.server.dashboard.model.AMSDataFileInfo;
import com.netease.arctic.server.dashboard.model.DDLInfo;
import com.netease.arctic.server.dashboard.model.PartitionBaseInfo;
import com.netease.arctic.server.dashboard.model.PartitionFileBaseInfo;
import com.netease.arctic.server.dashboard.model.TransactionsOfTable;
import com.netease.arctic.server.optimizing.OptimizingProcessMeta;
import com.netease.arctic.server.optimizing.OptimizingTaskMeta;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.OptimizingMapper;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableService;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.trace.SnapshotSummary;
import com.netease.arctic.utils.ManifestEntryFields;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataOperations;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.HistoryEntry;
import org.apache.iceberg.MetadataTableType;
import org.apache.iceberg.MetadataTableUtils;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableMetadataParser;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.InternalRecordWrapper;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerTableDescriptor extends PersistentBase {

  private static final Logger LOG = LoggerFactory.getLogger(ServerTableDescriptor.class);
  private static final Map<Integer, DataFileType> ICEBERG_FILE_TYPE_MAP = new HashMap<>();

  private final TableService tableService;

  static {
    ICEBERG_FILE_TYPE_MAP.put(FileContent.DATA.id(), DataFileType.BASE_FILE);
    ICEBERG_FILE_TYPE_MAP.put(FileContent.POSITION_DELETES.id(), DataFileType.POS_DELETE_FILE);
    ICEBERG_FILE_TYPE_MAP.put(FileContent.EQUALITY_DELETES.id(), DataFileType.EQ_DELETE_FILE);
  }

  public ServerTableDescriptor(TableService tableService) {
    this.tableService = tableService;
  }

  private ServerTableIdentifier getTable(String catalog, String db, String table) {
    return getAs(TableMetaMapper.class, mapper -> mapper.selectTableIdentifier(catalog, db, table));
  }

  public List<TransactionsOfTable> getTransactions(ServerTableIdentifier tableIdentifier) {
    List<TransactionsOfTable> transactionsOfTables = new ArrayList<>();
    ArcticTable arcticTable = tableService.loadTable(tableIdentifier);
    List<Table> tables = new ArrayList<>();
    if (arcticTable.isKeyedTable()) {
      tables.add(arcticTable.asKeyedTable().changeTable());
      tables.add(arcticTable.asKeyedTable().baseTable());
    } else {
      tables.add(arcticTable.asUnkeyedTable());
    }
    tables.forEach(table -> table.snapshots().forEach(snapshot -> {
      if (snapshot.operation().equals(DataOperations.REPLACE)) {
        return;
      }
      if (snapshot.summary().containsKey(SnapshotSummary.TRANSACTION_BEGIN_SIGNATURE)) {
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
      transactionsOfTables.add(transactionsOfTable);
    }));
    transactionsOfTables.sort((o1, o2) -> Long.compare(o2.commitTime, o1.commitTime));
    return transactionsOfTables;
  }

  public List<AMSDataFileInfo> getTransactionDetail(ServerTableIdentifier tableIdentifier, long transactionId) {
    List<AMSDataFileInfo> result = new ArrayList<>();
    ArcticTable arcticTable = tableService.loadTable(tableIdentifier);
    Snapshot snapshot;
    if (arcticTable.isKeyedTable()) {
      snapshot = arcticTable.asKeyedTable().changeTable().snapshot(transactionId);
      if (snapshot == null) {
        snapshot = arcticTable.asKeyedTable().baseTable().snapshot(transactionId);
      }
    } else {
      snapshot = arcticTable.asUnkeyedTable().snapshot(transactionId);
    }
    if (snapshot == null) {
      throw new IllegalArgumentException("unknown snapshot " + transactionId + " of " + tableIdentifier);
    }
    final long snapshotTime = snapshot.timestampMillis();
    snapshot.addedDataFiles(arcticTable.io()).forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          arcticTable.spec(), f.partition(),
          f.content(),
          f.fileSizeInBytes(),
          snapshotTime,
          "add"));
    });
    snapshot.removedDataFiles(arcticTable.io()).forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          arcticTable.spec(), f.partition(),
          f.content(),
          f.fileSizeInBytes(),
          snapshotTime,
          "remove"));
    });
    snapshot.addedDeleteFiles(arcticTable.io()).forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          arcticTable.spec(), f.partition(),
          f.content(),
          f.fileSizeInBytes(),
          snapshotTime,
          "add"));
    });
    snapshot.removedDeleteFiles(arcticTable.io()).forEach(f -> {
      result.add(new AMSDataFileInfo(
          f.path().toString(),
          arcticTable.spec(), f.partition(),
          f.content(),
          f.fileSizeInBytes(),
          snapshotTime,
          "remove"));
    });
    return result;
  }

  public List<DDLInfo> getTableOperations(ServerTableIdentifier tableIdentifier) {
    List<DDLInfo> result = new ArrayList<>();
    ArcticTable arcticTable = tableService.loadTable(tableIdentifier);
    Table table;
    if (arcticTable.isKeyedTable()) {
      table = arcticTable.asKeyedTable().baseTable();
    } else {
      table = arcticTable.asUnkeyedTable();
    }
    List<HistoryEntry> snapshotLog = ((HasTableOperations) table).operations().current().snapshotLog();
    List<org.apache.iceberg.TableMetadata.MetadataLogEntry> metadataLogEntries =
        ((HasTableOperations) table).operations().current().previousFiles();
    Set<Long> time = new HashSet<>();
    snapshotLog.forEach(e -> time.add(e.timestampMillis()));
    String lastMetadataLogEntryFile = null;
    org.apache.iceberg.TableMetadata lastTableMetadata = null;
    for (int i = 1; i < metadataLogEntries.size(); i++) {
      org.apache.iceberg.TableMetadata.MetadataLogEntry currentEntry = metadataLogEntries.get(i);
      if (!time.contains(currentEntry.timestampMillis())) {
        org.apache.iceberg.TableMetadata.MetadataLogEntry previousEntry = metadataLogEntries.get(i - 1);
        org.apache.iceberg.TableMetadata oldTableMetadata;
        if (lastMetadataLogEntryFile == null || !lastMetadataLogEntryFile.equals(previousEntry.file())) {
          oldTableMetadata = TableMetadataParser.read(table.io(), previousEntry.file());
        } else {
          oldTableMetadata = lastTableMetadata;
        }

        org.apache.iceberg.TableMetadata
            newTableMetadata = TableMetadataParser.read(table.io(), currentEntry.file());
        lastMetadataLogEntryFile = currentEntry.file();
        lastTableMetadata = newTableMetadata;

        DDLInfo.Generator generator = new DDLInfo.Generator();
        result.addAll(generator.tableIdentify(arcticTable.id())
            .oldMeta(oldTableMetadata)
            .newMeta(newTableMetadata)
            .generate());
      }
    }
    if (metadataLogEntries.size() > 0) {
      org.apache.iceberg.TableMetadata.MetadataLogEntry previousEntry = metadataLogEntries
          .get(metadataLogEntries.size() - 1);
      org.apache.iceberg.TableMetadata oldTableMetadata;

      if (lastMetadataLogEntryFile == null || !lastMetadataLogEntryFile.equals(previousEntry.file())) {
        oldTableMetadata = TableMetadataParser.read(table.io(), previousEntry.file());
      } else {
        oldTableMetadata = lastTableMetadata;
      }

      org.apache.iceberg.TableMetadata newTableMetadata = ((HasTableOperations) table).operations().current();
      DDLInfo.Generator generator = new DDLInfo.Generator();
      result.addAll(generator.tableIdentify(arcticTable.id())
          .oldMeta(oldTableMetadata)
          .newMeta(newTableMetadata)
          .generate());
    }
    return result;
  }

  public List<OptimizingProcessMeta> getOptimizingProcesses(String catalog, String db, String table) {
    return getAs(
        OptimizingMapper.class,
        mapper -> mapper.selectOptimizingProcesses(catalog, db, table));
  }

  public List<OptimizingTaskMeta> getOptimizingTasks(long processId) {
    return getAs(OptimizingMapper.class,
        mapper -> mapper.selectOptimizeTaskMetas(Collections.singletonList(processId)));
  }

  public List<OptimizingTaskMeta> getOptimizingTasks(List<OptimizingProcessMeta> processMetaList) {
    if (CollectionUtils.isEmpty(processMetaList)) {
      return Collections.emptyList();
    }
    List<Long> processIds = processMetaList.stream()
        .map(OptimizingProcessMeta::getProcessId).collect(Collectors.toList());
    return getAs(OptimizingMapper.class,
        mapper -> mapper.selectOptimizeTaskMetas(processIds));
  }

  public List<PartitionBaseInfo> getTablePartition(ArcticTable arcticTable) {
    if (arcticTable.spec().isUnpartitioned()) {
      return new ArrayList<>();
    }
    Map<String, PartitionBaseInfo> partitionBaseInfoHashMap = new HashMap<>();
    getTableFile(arcticTable, null).forEach(fileInfo -> {
      if (!partitionBaseInfoHashMap.containsKey(fileInfo.getPartitionName())) {
        partitionBaseInfoHashMap.put(fileInfo.getPartitionName(), new PartitionBaseInfo());
        partitionBaseInfoHashMap.get(fileInfo.getPartitionName()).setPartition(fileInfo.getPartitionName());
      }
      PartitionBaseInfo partitionInfo = partitionBaseInfoHashMap.get(fileInfo.getPartitionName());
      partitionInfo.setFileCount(partitionInfo.getFileCount() + 1);
      partitionInfo.setFileSize(partitionInfo.getFileSize() + fileInfo.getFileSize());
      partitionInfo.setLastCommitTime(partitionInfo.getLastCommitTime() > fileInfo.getCommitTime() ?
          partitionInfo.getLastCommitTime() :
          fileInfo.getCommitTime());
    });

    return new ArrayList<>(partitionBaseInfoHashMap.values());
  }

  public List<PartitionFileBaseInfo> getTableFile(ArcticTable arcticTable, String partition) {
    List<PartitionFileBaseInfo> result = new ArrayList<>();
    if (arcticTable.isKeyedTable()) {
      result.addAll(collectFileInfo(arcticTable.asKeyedTable().changeTable(), true, partition));
      result.addAll(collectFileInfo(arcticTable.asKeyedTable().baseTable(), false, partition));
    } else {
      result.addAll(collectFileInfo(arcticTable.asUnkeyedTable(), false, partition));
    }
    return result;
  }

  private List<PartitionFileBaseInfo> collectFileInfo(Table table, boolean isChangeTable, String partition) {
    PartitionSpec spec = table.spec();
    List<PartitionFileBaseInfo> result = new ArrayList<>();
    Table entriesTable = MetadataTableUtils.createMetadataTableInstance(((HasTableOperations) table).operations(),
        table.name(), table.name() + "#ENTRIES",
        MetadataTableType.ENTRIES);
    try (CloseableIterable<Record> manifests = IcebergGenerics.read(entriesTable)
        .where(Expressions.notEqual(ManifestEntryFields.STATUS.name(), ManifestEntryFields.Status.DELETED.id()))
        .build()) {
      for (Record record : manifests) {
        long snapshotId = (long) record.getField(ManifestEntryFields.SNAPSHOT_ID.name());
        GenericRecord dataFile = (GenericRecord) record.getField(ManifestEntryFields.DATA_FILE_FIELD_NAME);
        Integer contentId = (Integer) dataFile.getField(DataFile.CONTENT.name());
        String filePath = (String) dataFile.getField(DataFile.FILE_PATH.name());
        String partitionPath = null;
        GenericRecord parRecord = (GenericRecord) dataFile.getField(DataFile.PARTITION_NAME);
        if (parRecord != null) {
          InternalRecordWrapper wrapper = new InternalRecordWrapper(parRecord.struct());
          partitionPath = spec.partitionToPath(wrapper.wrap(parRecord));
        }
        if (partition != null && spec.isPartitioned() && !partition.equals(partitionPath)) {
          continue;
        }
        Long fileSize = (Long) dataFile.getField(DataFile.FILE_SIZE.name());
        DataFileType dataFileType =
            isChangeTable ? FileNameRules.parseFileTypeForChange(filePath) : ICEBERG_FILE_TYPE_MAP.get(contentId);
        long commitTime = -1;
        if (table.snapshot(snapshotId) != null) {
          commitTime = table.snapshot(snapshotId).timestampMillis();
        }
        result.add(new PartitionFileBaseInfo(String.valueOf(snapshotId), dataFileType, commitTime,
            partitionPath, filePath, fileSize));
      }
    } catch (IOException exception) {
      LOG.error("close manifest file error", exception);
    }
    return result;
  }
}
