package com.netease.arctic.hive.io;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.HiveTableTestBase;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.netease.arctic.hive.io.TestIOUtils.readHiveKeyedTable;
import static com.netease.arctic.hive.io.TestIOUtils.testWriteChange;

public class KeyedTimeStampTest extends HiveTableTestBase {

  @Test
  public void testUnPartitioned() throws IOException {
    testWriteChange(testUnPartitionTSKeyedHiveTable.asKeyedTable(), 1L,
        ImmutableList.of(HiveTestRecords.testRecords().get(0)),
        ChangeAction.INSERT);
    testWriteChange(testUnPartitionTSKeyedHiveTable.asKeyedTable(), 2L,
        ImmutableList.of(HiveTestRecords.testRecords().get(1)),
        ChangeAction.INSERT);
    testWriteChange(testUnPartitionTSKeyedHiveTable.asKeyedTable(), 3L,
        ImmutableList.of(HiveTestRecords.testRecords().get(2)),
        ChangeAction.INSERT);
    testWriteChange(testUnPartitionTSKeyedHiveTable.asKeyedTable(), 4L,
        ImmutableList.of(HiveTestRecords.testRecords().get(3)),
        ChangeAction.INSERT);
    testWriteChange(testUnPartitionTSKeyedHiveTable.asKeyedTable(), 5L,
        ImmutableList.of(HiveTestRecords.testRecords().get(0)),
        ChangeAction.DELETE);
    List<Record> records1 = readHiveKeyedTable(testUnPartitionTSKeyedHiveTable, Expressions.alwaysTrue());
    Assert.assertEquals(3, records1.size());
    Assert.assertTrue(recordToIdList(records1).containsAll(Arrays.asList(2, 3, 4)));
  }

  @Test
  public void testPartitioned() throws IOException {
    testWriteChange(testPartitionTSKeyedHiveTable.asKeyedTable(), 1L,
        ImmutableList.of(HiveTestRecords.testRecords().get(0)),
        ChangeAction.INSERT);
    testWriteChange(testPartitionTSKeyedHiveTable.asKeyedTable(), 2L,
        ImmutableList.of(HiveTestRecords.testRecords().get(1)),
        ChangeAction.INSERT);
    testWriteChange(testPartitionTSKeyedHiveTable.asKeyedTable(), 3L,
        ImmutableList.of(HiveTestRecords.testRecords().get(2)),
        ChangeAction.INSERT);
    testWriteChange(testPartitionTSKeyedHiveTable.asKeyedTable(), 4L,
        ImmutableList.of(HiveTestRecords.testRecords().get(3)),
        ChangeAction.INSERT);
    testWriteChange(testPartitionTSKeyedHiveTable.asKeyedTable(), 5L,
        ImmutableList.of(HiveTestRecords.testRecords().get(3)),
        ChangeAction.DELETE);
    Expression partitionFilter = Expressions.and(
        Expressions.notNull("name"),
        Expressions.equal("name", "mack")
    );
    List<Record> records = readHiveKeyedTable(testPartitionTSKeyedHiveTable, partitionFilter);
    Assert.assertEquals(1, records.size());
    Assert.assertTrue(recordToIdList(records).contains(3));
  }

  private List<Integer> recordToIdList(List<Record> list) {
    return list.stream().map(r -> Integer.parseInt(r.getField("id").toString())).collect(Collectors.toList());
  }
}
