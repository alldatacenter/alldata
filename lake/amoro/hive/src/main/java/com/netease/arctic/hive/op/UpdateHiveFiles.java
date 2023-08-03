package com.netease.arctic.hive.op;

import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.exceptions.CannotAlterHiveLocationException;
import com.netease.arctic.hive.table.UnkeyedHiveTable;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.io.ArcticHadoopFileIO;
import com.netease.arctic.op.UpdatePartitionProperties;
import com.netease.arctic.utils.TableFileUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.PartitionDropOptions;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotUpdate;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.StructLikeMap;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.netease.arctic.op.OverwriteBaseFiles.PROPERTIES_TRANSACTION_ID;

public abstract class UpdateHiveFiles<T extends SnapshotUpdate<T>> implements SnapshotUpdate<T> {

  private static final Logger LOG = LoggerFactory.getLogger(UpdateHiveFiles.class);

  public static final String PROPERTIES_VALIDATE_LOCATION = "validate-location";
  public static final String DELETE_UNTRACKED_HIVE_FILE = "delete-untracked-hive-file";

  protected final Transaction transaction;
  protected final boolean insideTransaction;
  protected final UnkeyedHiveTable table;
  protected final HMSClientPool hmsClient;
  protected final HMSClientPool transactionClient;
  protected final T delegate;
  protected final String db;
  protected final String tableName;

  protected final Table hiveTable;

  protected Expression expr;
  protected final List<DataFile> addFiles = Lists.newArrayList();
  protected final List<DataFile> deleteFiles = Lists.newArrayList();

  protected StructLikeMap<Partition> partitionToDelete;
  protected StructLikeMap<Partition> partitionToCreate;
  protected final StructLikeMap<Partition> partitionToAlter;
  protected final StructLikeMap<Partition> partitionToAlterLocation;
  protected String unpartitionTableLocation;
  protected Long txId = null;
  protected boolean validateLocation = true;
  protected boolean checkOrphanFiles = false;
  protected int commitTimestamp; // in seconds

  public UpdateHiveFiles(
      Transaction transaction, boolean insideTransaction,
      UnkeyedHiveTable table, T delegate,
      HMSClientPool hmsClient, HMSClientPool transactionClient) {
    this.transaction = transaction;
    this.insideTransaction = insideTransaction;
    this.table = table;
    this.delegate = delegate;
    this.hmsClient = hmsClient;
    this.transactionClient = transactionClient;

    this.db = table.id().getDatabase();
    this.tableName = table.id().getTableName();
    try {
      this.hiveTable = hmsClient.run(c -> c.getTable(db, tableName));
    } catch (TException | InterruptedException e) {
      throw new RuntimeException(e);
    }
    this.partitionToAlter = StructLikeMap.create(table.spec().partitionType());
    this.partitionToCreate = StructLikeMap.create(table.spec().partitionType());
    this.partitionToDelete = StructLikeMap.create(table.spec().partitionType());
    this.partitionToAlterLocation = StructLikeMap.create(table.spec().partitionType());
  }

  @Override
  public void commit() {
    commitTimestamp = (int) (System.currentTimeMillis() / 1000);
    if (table.spec().isUnpartitioned()) {
      generateUnpartitionTableLocation();
    } else {
      this.partitionToDelete = getDeletePartition();
      this.partitionToCreate = getCreatePartition(this.partitionToDelete);
    }

    if (checkOrphanFiles) {
      checkPartitionedOrphanFilesAndDelete(table.spec().isUnpartitioned());
    }
    // if no DataFiles to add or delete in Hive location, only commit to iceberg
    boolean noHiveDataFilesChanged = CollectionUtils.isEmpty(addFiles) && CollectionUtils.isEmpty(deleteFiles) &&
        expr != Expressions.alwaysTrue();

    delegate.commit();
    if (!noHiveDataFilesChanged) {
      commitPartitionProperties();
    }
    if (!insideTransaction) {
      transaction.commitTransaction();
    }
    if (noHiveDataFilesChanged) {
      return;
    }

    try {
      if (table.spec().isUnpartitioned()) {
        commitNonPartitionedTable();
      } else {
        commitPartitionedTable();
      }
    } catch (Exception e) {
      LOG.warn("Commit operation to HMS failed.", e);
    }
  }

  private void commitPartitionProperties() {
    UpdatePartitionProperties updatePartitionProperties = table.updatePartitionProperties(transaction);
    if (table.spec().isUnpartitioned()) {
      updatePartitionProperties.set(TablePropertyUtil.EMPTY_STRUCT,
          HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION, unpartitionTableLocation);
      updatePartitionProperties.set(TablePropertyUtil.EMPTY_STRUCT,
          HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME, commitTimestamp + "");
    } else {
      partitionToDelete.forEach((partitionData, partition) -> {
        if (!partitionToCreate.containsKey(partitionData)) {
          updatePartitionProperties.remove(partitionData, HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION);
          updatePartitionProperties.remove(partitionData, HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME);
        }
      });
      partitionToCreate.forEach((partitionData, partition) -> {
        updatePartitionProperties.set(partitionData, HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION,
            partition.getSd().getLocation());
        updatePartitionProperties.set(partitionData, HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME,
            commitTimestamp + "");
      });
      partitionToAlter.forEach((partitionData, partition) -> {
        updatePartitionProperties.set(partitionData, HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION,
            partition.getSd().getLocation());
        updatePartitionProperties.set(partitionData, HiveTableProperties.PARTITION_PROPERTIES_KEY_TRANSIENT_TIME,
            commitTimestamp + "");
      });
    }
    updatePartitionProperties.commit();
  }

  protected StructLikeMap<Partition> getCreatePartition(StructLikeMap<Partition> partitionToDelete) {
    if (this.addFiles.isEmpty()) {
      return StructLikeMap.create(table.spec().partitionType());
    }

    Map<String, String> partitionLocationMap = Maps.newHashMap();
    Map<String, List<DataFile>> partitionDataFileMap = Maps.newHashMap();
    Map<String, List<String>> partitionValueMap = Maps.newHashMap();

    Types.StructType partitionSchema = table.spec().partitionType();
    for (DataFile d : addFiles) {
      List<String> partitionValues = HivePartitionUtil.partitionValuesAsList(d.partition(), partitionSchema);
      String value = Joiner.on("/").join(partitionValues);
      String location = TableFileUtil.getFileDir(d.path().toString());
      partitionLocationMap.put(value, location);
      if (!partitionDataFileMap.containsKey(value)) {
        partitionDataFileMap.put(value, Lists.newArrayList());
      }
      partitionDataFileMap.get(value).add(d);
      partitionValueMap.put(value, partitionValues);
    }

    StructLikeMap<Partition> createPartitions = StructLikeMap.create(table.spec().partitionType());
    for (String val : partitionValueMap.keySet()) {
      List<String> values = partitionValueMap.get(val);
      String location = partitionLocationMap.get(val);
      List<DataFile> dataFiles = partitionDataFileMap.get(val);

      checkCreatePartitionDataFiles(dataFiles, location);
      Partition p = HivePartitionUtil.newPartition(hiveTable, values, location, dataFiles, commitTimestamp);
      createPartitions.put(dataFiles.get(0).partition(), p);
    }

    createPartitions = filterNewPartitionNonExists(createPartitions, partitionToDelete);
    return createPartitions;
  }

  protected StructLikeMap<Partition> getDeletePartition() {
    if (expr != null) {
      List<DataFile> deleteFilesByExpr = applyDeleteExpr();
      this.deleteFiles.addAll(deleteFilesByExpr);
    }

    StructLikeMap<Partition> deletePartitions = StructLikeMap.create(table.spec().partitionType());
    if (deleteFiles.isEmpty()) {
      return deletePartitions;
    }

    Types.StructType partitionSchema = table.spec().partitionType();

    Set<String> checkedPartitionValues = Sets.newHashSet();
    Set<String> deleteFileLocations = Sets.newHashSet();

    for (DataFile dataFile : deleteFiles) {
      List<String> values = HivePartitionUtil.partitionValuesAsList(dataFile.partition(), partitionSchema);
      String pathValue = Joiner.on("/").join(values);
      deleteFileLocations.add(dataFile.path().toString());
      if (checkedPartitionValues.contains(pathValue)) {
        continue;
      }
      try {
        Partition partition = hmsClient.run(c -> c.getPartition(db, tableName, values));
        deletePartitions.put(dataFile.partition(), partition);
      } catch (NoSuchObjectException e) {
        // pass do nothing
      } catch (TException | InterruptedException e) {
        throw new RuntimeException(e);
      }
      checkedPartitionValues.add(pathValue);
    }

    if (validateLocation) {
      deletePartitions.values().forEach(p -> checkPartitionDelete(deleteFileLocations, p));
    }
    return deletePartitions;
  }

  private void checkPartitionDelete(Set<String> deleteFiles, Partition partition) {
    String partitionLocation = partition.getSd().getLocation();

    try (ArcticHadoopFileIO io = table.io()) {
      io.listDirectory(partitionLocation)
          .forEach(f -> {
            if (!deleteFiles.contains(f.location())) {
              throw new CannotAlterHiveLocationException(
                  "can't delete hive partition: " + partitionToString(partition) +
                      ", file under partition is not deleted: " +
                      f.location());
            }
          });
    }
  }

  /**
   * check all file with same partition key under same path
   */
  private void checkCreatePartitionDataFiles(List<DataFile> addFiles, String partitionLocation) {
    Path partitionPath = new Path(partitionLocation);
    for (DataFile df : addFiles) {
      String fileDir = TableFileUtil.getFileDir(df.path().toString());
      Path dirPath = new Path(fileDir);
      if (!partitionPath.equals(dirPath)) {
        throw new CannotAlterHiveLocationException(
            "can't create new hive location: " + partitionLocation + " for data file: " + df.path().toString() +
                " is not under partition location path"
        );
      }
    }
  }

  /**
   * check files in the partition, and delete orphan files
   */
  private void checkPartitionedOrphanFilesAndDelete(boolean isUnPartitioned) {
    List<String> partitionsToCheck = Lists.newArrayList();
    if (isUnPartitioned) {
      partitionsToCheck.add(this.unpartitionTableLocation);
    } else {
      partitionsToCheck.addAll(this.partitionToCreate.values()
          .stream().map(partition -> partition.getSd().getLocation()).collect(Collectors.toList()));
      partitionsToCheck.addAll(this.partitionToAlterLocation.values()
          .stream().map(partition -> partition.getSd().getLocation()).collect(Collectors.toList()));
    }
    Set<String> addFilesPathCollect = addFiles.stream()
        .map(dataFile -> dataFile.path().toString()).collect(Collectors.toSet());
    Set<String> deleteFilesPathCollect = deleteFiles.stream()
        .map(deleteFile -> deleteFile.path().toString()).collect(Collectors.toSet());

    for (String partitionLocation : partitionsToCheck) {
      try (ArcticHadoopFileIO io = table.io()) {
        io.listPrefix(partitionLocation).forEach(f -> {
          if (!addFilesPathCollect.contains(f.location()) &&
              !deleteFilesPathCollect.contains(f.location())) {
            io.deleteFile(f.location());
            LOG.warn("Delete orphan file path: {}", f.location());
          }
        });
      }
    }
  }

  /**
   * filter partitionToCreate. make sure all partition non-exist in hive. or
   * 0. partition is able to delete.
   * 0.1 - not same location, allow to create
   * 0.2 - same location, can't create ( delete partition will not delete files )
   * 1. exists but location is same. skip
   * 2. exists but location is not same, throw {@link CannotAlterHiveLocationException}
   */
  private StructLikeMap<Partition> filterNewPartitionNonExists(
      StructLikeMap<Partition> partitionToCreate,
      StructLikeMap<Partition> partitionToDelete) {
    StructLikeMap<Partition> partitions = StructLikeMap.create(table.spec().partitionType());
    Map<String, Partition> deletePartitionValueMap = Maps.newHashMap();
    for (Partition p : partitionToDelete.values()) {
      String partValue = Joiner.on("/").join(p.getValues());
      deletePartitionValueMap.put(partValue, p);
    }

    for (Map.Entry<StructLike, Partition> entry : partitionToCreate.entrySet()) {
      String partValue = Joiner.on("/").join(entry.getValue().getValues());
      String location = entry.getValue().getSd().getLocation();
      Partition toDelete = deletePartitionValueMap.get(partValue);
      if (toDelete != null) {
        String deleteLocation = toDelete.getSd().getLocation();
        // if exists partition to delete with same value
        // make sure location is different
        if (isPathEquals(location, deleteLocation) && validateLocation) {
          throw new CannotAlterHiveLocationException("can't create new partition: " +
              partitionToString(entry.getValue()) + ", this " +
              "partition will be " +
              "delete and re-create with same location");
        } else {
          // this partition is need to alter, rather than delete
          partitionToAlter.put(entry.getKey(), entry.getValue());
          partitionToAlterLocation.put(entry.getKey(), entry.getValue());
          partitionToDelete.remove(entry.getKey());
          continue;
        }
      }

      try {
        Partition partitionInHive = hmsClient.run(c -> c.getPartition(db, tableName, entry.getValue().getValues()));
        String locationInHive = partitionInHive.getSd().getLocation();
        if (isPathEquals(location, locationInHive)) {
          partitionToAlter.put(entry.getKey(), entry.getValue());
          continue;
        }
        throw new CannotAlterHiveLocationException("can't create new partition: " +
            partitionToString(entry.getValue()) +
            ", this partition exists in hive with different location: " + locationInHive);
      } catch (NoSuchObjectException e) {
        partitions.put(entry.getKey(), entry.getValue());
      } catch (TException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return partitions;
  }

  private void commitPartitionedTable() {
    if (!partitionToDelete.isEmpty()) {
      for (Partition p : partitionToDelete.values()) {
        try {
          transactionClient.run(c -> {
            PartitionDropOptions options = PartitionDropOptions.instance()
                .deleteData(false)
                .ifExists(true)
                .purgeData(false)
                .returnResults(false);
            c.dropPartition(db, tableName, p.getValues(), options);
            return 0;
          });
        } catch (NoSuchObjectException e) {
          LOG.warn("try to delete hive partition {} but partition not exist.", p);
        } catch (TException | InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    if (!partitionToCreate.isEmpty()) {
      try {
        transactionClient.run(c -> c.addPartitions(Lists.newArrayList(partitionToCreate.values())));
      } catch (TException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    if (!partitionToAlter.isEmpty()) {
      try {
        transactionClient.run(c -> {
          try {
            c.alterPartitions(db, tableName, Lists.newArrayList(partitionToAlter.values()), null);
          } catch (InvocationTargetException | InstantiationException |
                   IllegalAccessException | NoSuchMethodException |
                   ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
          return null;
        });
      } catch (TException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void generateUnpartitionTableLocation() {
    if (this.addFiles.isEmpty()) {
      unpartitionTableLocation = createUnpartitionEmptyLocationForHive();
    } else {
      unpartitionTableLocation = TableFileUtil.getFileDir(this.addFiles.get(0).path().toString());
    }
  }

  private void commitNonPartitionedTable() {

    final String finalLocation = unpartitionTableLocation;
    try {
      transactionClient.run(c -> {
        Table hiveTable = c.getTable(db, tableName);
        hiveTable.getSd().setLocation(finalLocation);
        HiveTableUtil.generateTableProperties(commitTimestamp, addFiles)
            .forEach((key, value) -> hiveTable.getParameters().put(key, value));
        c.alterTable(db, tableName, hiveTable);
        return 0;
      });
    } catch (TException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private String createUnpartitionEmptyLocationForHive() {
    // create a new empty location for hive
    String newLocation;
    newLocation = HiveTableUtil.newHiveDataLocation(table.hiveLocation(), table.spec(), null,
        txId != null ? HiveTableUtil.newHiveSubdirectory(txId) : HiveTableUtil.newHiveSubdirectory());
    try (FileIO io = table.io()) {
      OutputFile file = io.newOutputFile(newLocation + "/.keep");
      try {
        file.createOrOverwrite().close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return newLocation;
  }

  protected List<DataFile> applyDeleteExpr() {
    try (CloseableIterable<FileScanTask> tasks = table.newScan().filter(expr).planFiles()) {
      return Lists.newArrayList(tasks).stream().map(FileScanTask::file).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isPathEquals(String pathA, String pathB) {
    Path path1 = new Path(pathA);
    Path path2 = new Path(pathB);
    return path1.equals(path2);
  }

  private String partitionToString(Partition p) {
    return "Partition(values: [" + Joiner.on("/").join(p.getValues()) +
        "], location: " + p.getSd().getLocation() + ")";
  }

  @Override
  public T scanManifestsWith(ExecutorService executorService) {
    delegate.scanManifestsWith(executorService);
    return self();
  }

  @Override
  public T set(String property, String value) {
    if (PROPERTIES_TRANSACTION_ID.equals(property)) {
      this.txId = Long.parseLong(value);
    }

    if (PROPERTIES_VALIDATE_LOCATION.equals(property)) {
      this.validateLocation = Boolean.parseBoolean(value);
    }

    if (DELETE_UNTRACKED_HIVE_FILE.equals(property)) {
      this.checkOrphanFiles = Boolean.parseBoolean(value);
    }

    delegate.set(property, value);
    return self();
  }

  @Override
  public T deleteWith(Consumer<String> deleteFunc) {
    delegate.deleteWith(deleteFunc);
    return self();
  }

  @Override
  public T stageOnly() {
    delegate.stageOnly();
    return self();
  }

  @Override
  public Snapshot apply() {
    return delegate.apply();
  }

  @Override
  public Object updateEvent() {
    return delegate.updateEvent();
  }

  protected abstract T self();
}
