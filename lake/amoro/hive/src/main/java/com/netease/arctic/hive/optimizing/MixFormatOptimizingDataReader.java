package com.netease.arctic.hive.optimizing;

import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.hive.io.reader.AdaptHiveGenericArcticDataReader;
import com.netease.arctic.optimizing.OptimizingDataReader;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.BasicArcticFileScanTask;
import com.netease.arctic.scan.NodeFileScanTask;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is a temporary implementation，A delete multiplexed reader will be implemented in the future
 */
public class MixFormatOptimizingDataReader implements OptimizingDataReader {

  private ArcticTable table;

  private StructLikeCollections structLikeCollections;

  private RewriteFilesInput input;

  public MixFormatOptimizingDataReader(
      ArcticTable table,
      StructLikeCollections structLikeCollections,
      RewriteFilesInput input) {
    this.table = table;
    this.structLikeCollections = structLikeCollections;
    this.input = input;
  }

  @Override
  public CloseableIterable<Record> readData() {
    AdaptHiveGenericArcticDataReader reader = arcticDataReader(table.schema());

    // Change returned value by readData  from Iterator to Iterable in future
    CloseableIterator<Record> closeableIterator = reader.readData(nodeFileScanTask(input.rewrittenDataFilesForMixed()));
    return wrapIterator2Iterable(closeableIterator);
  }

  @Override
  public CloseableIterable<Record> readDeletedData() {
    Schema schema = new Schema(
        MetadataColumns.FILE_PATH,
        MetadataColumns.ROW_POSITION,
        com.netease.arctic.table.MetadataColumns.TREE_NODE_FIELD
    );
    AdaptHiveGenericArcticDataReader reader = arcticDataReader(schema);
    return wrapIterator2Iterable(reader.readDeletedData(nodeFileScanTask(input.rePosDeletedDataFilesForMixed())));
  }

  @Override
  public void close() {

  }

  private AdaptHiveGenericArcticDataReader arcticDataReader(Schema requiredSchema) {

    PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.noPrimaryKey();
    if (table.isKeyedTable()) {
      KeyedTable keyedTable = table.asKeyedTable();
      primaryKeySpec = keyedTable.primaryKeySpec();
    }

    return new AdaptHiveGenericArcticDataReader(table.io(), table.schema(), requiredSchema,
        primaryKeySpec, table.properties().get(TableProperties.DEFAULT_NAME_MAPPING),
        false, IdentityPartitionConverters::convertConstant, null,
        false, structLikeCollections);
  }

  private NodeFileScanTask nodeFileScanTask(List<PrimaryKeyedFile> dataFiles) {
    List<DeleteFile> posDeleteList = input.positionDeleteForMixed();

    List<PrimaryKeyedFile> equlityDeleteList = input.equalityDeleteForMixed();

    List<PrimaryKeyedFile> allTaskFiles = new ArrayList<>();
    allTaskFiles.addAll(equlityDeleteList);
    allTaskFiles.addAll(dataFiles);

    List<ArcticFileScanTask> fileScanTasks = allTaskFiles.stream()
        .map(file -> new BasicArcticFileScanTask(file, posDeleteList, table.spec()))
        .collect(Collectors.toList());
    return new NodeFileScanTask(fileScanTasks);
  }

  private CloseableIterable<Record> wrapIterator2Iterable(CloseableIterator<Record> iterator) {
    return new CloseableIterable<Record>() {
      @Override
      public CloseableIterator<Record> iterator() {
        return iterator;
      }

      @Override
      public void close() throws IOException {
        iterator.close();
      }
    };
  }
}
