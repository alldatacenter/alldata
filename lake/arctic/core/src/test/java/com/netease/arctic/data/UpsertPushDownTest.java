package com.netease.arctic.data;

import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class UpsertPushDownTest extends TableTestBase {

  public UpsertPushDownTest(PartitionSpec partitionSpec) {
    super(TableFormat.MIXED_ICEBERG, TableTestHelpers.TABLE_SCHEMA, TableTestHelpers.PRIMARY_KEY_SPEC,
        partitionSpec, Collections.singletonMap(TableProperties.UPSERT_ENABLED, "true"));
  }

  @Parameterized.Parameters(name = "spec = {0}")
  public static Object[] parameters() {
    return new Object[] {PartitionSpec.unpartitioned(), TableTestHelpers.SPEC,
                         PartitionSpec.builderFor(TableTestHelpers.TABLE_SCHEMA)
                             .day("op_time").identity("ts").build()};
  }

  @Before
  public void initChangeStoreData() {
    DataTestHelpers.writeAndCommitChangeStore(getArcticTable().asKeyedTable(), 1L,
        ChangeAction.DELETE, writeRecords(1, "aaa", 0, 1));
    DataTestHelpers.writeAndCommitChangeStore(getArcticTable().asKeyedTable(), 2L,
        ChangeAction.UPDATE_AFTER, writeRecords(1, "aaa", 0, 1));
    DataTestHelpers.writeAndCommitChangeStore(getArcticTable().asKeyedTable(), 3L,
        ChangeAction.DELETE, writeRecords(2, "bbb", 0, 2));
    DataTestHelpers.writeAndCommitChangeStore(getArcticTable().asKeyedTable(), 3L,
        ChangeAction.UPDATE_AFTER, writeRecords(2, "bbb", 0, 2));
    DataTestHelpers.writeAndCommitChangeStore(getArcticTable().asKeyedTable(), 4L,
        ChangeAction.DELETE, writeRecords(2, "ccc", 0, 2));
    DataTestHelpers.writeAndCommitChangeStore(getArcticTable().asKeyedTable(), 5L,
        ChangeAction.UPDATE_AFTER, writeRecords(2, "ccc", 0, 2));
  }

  @Test
  public void testReadKeyedTableWithoutFilter() {
    List<Record> records = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), Expressions.alwaysTrue());
    Assert.assertEquals(records.size(), 2);
    Assert.assertTrue(recordToNameList(records).containsAll(Arrays.asList("aaa", "ccc")));
  }

  @Test
  public void testReadKeyedTableWithPartitionAndColumnFilter() {
    Assume.assumeTrue(isPartitionedTable());
    Expression partitionAndColumnFilter = Expressions.and(
        Expressions.and(
            Expressions.notNull("op_time"),
            Expressions.equal("op_time", "2022-01-02T12:00:00")
        ),
        Expressions.and(
            Expressions.notNull("name"),
            Expressions.equal("name", "bbb")
        )
    );
    List<Record> records = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), partitionAndColumnFilter);
    // Scan from change store only filter partition column expression, so record(name=ccc) is still returned.
    Assert.assertEquals(records.size(), 1);
    Assert.assertTrue(recordToNameList(records).contains("ccc"));
  }

  @Test
  public void testReadKeyedTableWithPartitionOrColumnFilter() {
    Assume.assumeTrue(isPartitionedTable());
    Expression partitionOrColumnFilter = Expressions.or(
        Expressions.and(
            Expressions.notNull("op_time"),
            Expressions.equal("op_time", "2022-01-02T12:00:00")
        ),
        Expressions.and(
            Expressions.notNull("name"),
            Expressions.equal("name", "bbb")
        )
    );
    List<Record> records = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), partitionOrColumnFilter);
    Assert.assertEquals(records.size(), 2);
    Assert.assertTrue(recordToNameList(records).containsAll(Arrays.asList("aaa", "ccc")));
  }

  @Test
  public void testReadKeyedTableWithPartitionFilter() {
    Assume.assumeTrue(isPartitionedTable());
    Expression partitionFilter = Expressions.and(
        Expressions.notNull("op_time"),
        Expressions.equal("op_time", "2022-01-02T12:00:00")
    );
    List<Record> records = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), partitionFilter);
    Assert.assertEquals(records.size(), 1);
    Assert.assertTrue(recordToNameList(records).contains("ccc"));
  }

  @Test
  public void testReadKeyedTableWithColumnFilter() {
    Expression columnFilter = Expressions.and(
        Expressions.notNull("name"),
        Expressions.equal("name", "bbb")
    );
    List<Record> records = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), columnFilter);
    Assert.assertEquals(records.size(), 2);
    Assert.assertTrue(recordToNameList(records).containsAll(Arrays.asList("aaa", "ccc")));
  }

  @Test
  public void testReadKeyedTableWithGreaterPartitionAndColumnFilter() {
    Assume.assumeTrue(isPartitionedTable());
    Expression greaterPartitionAndColumnFilter = Expressions.and(
        Expressions.and(
            Expressions.notNull("op_time"),
            Expressions.greaterThan("op_time", "2022-01-01T12:00:00")
        ),
        Expressions.and(
            Expressions.notNull("name"),
            Expressions.equal("name", "bbb")
        )
    );
    List<Record> records = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(),
        greaterPartitionAndColumnFilter);
    Assert.assertEquals(records.size(), 1);
    Assert.assertTrue(recordToNameList(records).contains("ccc"));
  }

  private List<Record> writeRecords(int id, String name, long ts, int day) {

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(id, name, ts, String.format("2022-01-%02dT12:00:00", day)));

    return builder.build();
  }

  private List<String> recordToNameList(List<Record> list) {
    return list.stream().map(r -> r.getField("name").toString()).collect(Collectors.toList());
  }
}
