package com.netease.arctic.hive;

import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.thrift.TException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MockDataFileBuilder {
  final TableIdentifier identifier;
  final Table hiveTable;
  final ArcticTable table;

  public MockDataFileBuilder(ArcticTable table, HiveMetaStoreClient client) throws TException {
    identifier = table.id();
    this.table = table;
    hiveTable = client.getTable(identifier.getDatabase(), identifier.getTableName());
  }

  public DataFile build(String valuePath, String path) {
    String hiveLocation = ((SupportHive) table).hiveLocation();

    String finalPath = hiveLocation + path;
    DataFiles.Builder builder =  DataFiles.builder(table.spec())
        .withPath(finalPath)
        .withFileSizeInBytes(0)
        .withRecordCount(2);

    if (!StringUtils.isEmpty(valuePath)){
      builder = builder.withPartitionPath(valuePath);
    }
    if (!table.io().exists(finalPath)){
      // create a temp file for test
      try {
        table.io().newOutputFile(finalPath).createOrOverwrite().close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return builder.build();
  }

  public List<DataFile> buildList(List<Map.Entry<String, String>> partValueFiles){
    return partValueFiles.stream().map(
        kv -> this.build(kv.getKey(), kv.getValue())
    ).collect(Collectors.toList());
  }
}
