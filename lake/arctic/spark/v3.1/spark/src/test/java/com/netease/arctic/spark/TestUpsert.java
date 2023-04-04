/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.spark.util.RecordGenerator;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.glassfish.jersey.internal.guava.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class TestUpsert extends SparkTestBase {
  private final String database = "db";
  private final String table = "sink_table";
  private final String sourceView = "source_table";
  private final String initView = "init_view";
  private final TableIdentifier identifier = TableIdentifier.of(catalogNameArctic, database, table);

  private static final Schema schema = new Schema(
      Types.NestedField.of(1, false, "order_key", Types.IntegerType.get()),
      Types.NestedField.of(2, false, "line_number", Types.IntegerType.get()),
      Types.NestedField.of(3, false, "data", Types.StringType.get()),
      Types.NestedField.of(4, false, "pt", Types.StringType.get())
  );

  private static final PartitionSpec partitionSpec = PartitionSpec.builderFor(schema)
      .identity("pt").build();

  private static final int newRecordSize = 30;
  private static final int upsertRecordSize = 20;

  @Parameterized.Parameters
  public static List<Object[]> arguments() {
    // pk-include-pt, duplicateCheck
    List<Boolean[]> args = Lists.newArrayList(
        new Boolean[] {true, true},
        new Boolean[] {false, true},
        new Boolean[] {true, false},
        new Boolean[] {false, false}
    );

    return args.stream()
        .map(a -> {
          boolean pkIncludePt = a[0];
          boolean duplicateCheck = a[1];
          PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.builderFor(schema)
              .addColumn("order_key")
              .addColumn("line_number").build();
          if (pkIncludePt) {
            primaryKeySpec = PrimaryKeySpec.builderFor(schema)
                .addColumn("order_key")
                .addColumn("line_number")
                .addColumn("pt").build();
          }
          RecordGenerator generator = RecordGenerator.buildFor(schema)
              .withSequencePrimaryKey(primaryKeySpec)
              .withRandomDate("pt")
              .build();
          List<GenericRecord> target = generator.records(100);
          List<GenericRecord> source = upsertSource(
              target, generator, primaryKeySpec, partitionSpec, upsertRecordSize, newRecordSize);

          return new Object[] {target, source, primaryKeySpec, duplicateCheck};
        })
        .collect(Collectors.toList());
  }

  private static List<GenericRecord> upsertSource(
      List<GenericRecord> initializeData, RecordGenerator sourceGenerator,
      PrimaryKeySpec keySpec, PartitionSpec partitionSpec,
      int upsertCount, int insertCount) {
    RecordGenerator generator = RecordGenerator.buildFor(schema)
        .withSeed(System.currentTimeMillis())
        .build();

    List<GenericRecord> source = Lists.newArrayList();
    List<GenericRecord> insertSource = sourceGenerator.records(insertCount);
    source.addAll(insertSource);

    List<GenericRecord> upsertSource = sourceGenerator.records(upsertCount);
    Iterator<GenericRecord> it = initializeData.iterator();

    Set<String> sourceCols = Sets.newHashSet();
    sourceCols.addAll(keySpec.fieldNames());
    partitionSpec.fields().stream()
        .map(f -> schema.findField(f.sourceId()))
        .map(Types.NestedField::name)
        .forEach(sourceCols::add);

    for (GenericRecord r : upsertSource) {
      GenericRecord t = it.next();
      sourceCols.forEach(col -> r.setField(col, t.getField(col)));
    }
    source.addAll(upsertSource);
    return source;
  }

  private final List<GenericRecord> sources;
  private final List<GenericRecord> initialize;
  private final boolean checkSourceUniqueness;
  private final PrimaryKeySpec primaryKeySpec;

  public TestUpsert(
      List<GenericRecord> targetData, List<GenericRecord> sourceData,
      PrimaryKeySpec primaryKeySpec,
      boolean sourceUniquenessCheck
  ) {
    this.initialize = targetData;
    this.sources = sourceData;
    this.checkSourceUniqueness = sourceUniquenessCheck;
    this.primaryKeySpec = primaryKeySpec;
  }

  @Before
  public void before() throws AnalysisException {
    sql("use " + catalogNameArctic);
    sql("create database if not exists {0}", database);

    ArcticCatalog catalog = catalog(catalogNameArctic);

    catalog.newTableBuilder(identifier, schema)
        .withPrimaryKeySpec(primaryKeySpec)
        .withPartitionSpec(partitionSpec)
        .withProperties(properties(
            "write.upsert.enabled", "true",
            "base.file-index.hash-bucket", "4"
        )).create();

    Dataset<Row> dataset = spark.createDataFrame(
        initialize.stream().map(SparkTestContext::recordToRow).collect(Collectors.toList()),
        SparkSchemaUtil.convert(schema));
    dataset.createTempView(initView);

    dataset = spark.createDataFrame(
        sources.stream().map(SparkTestContext::recordToRow).collect(Collectors.toList()),
        SparkSchemaUtil.convert(schema));
    dataset.createTempView(sourceView);
  }

  @After
  public void after() {
    sql("USE spark_catalog");
    sql("DROP VIEW IF EXISTS " + initView);
    sql("DROP VIEW IF EXISTS " + sourceView);
    ArcticCatalog catalog = catalog(catalogNameArctic);
    catalog.dropTable(identifier, true);
  }

  @Test
  public void testKeyedTableUpsert() {
    sql("set `spark.sql.arctic.check-source-data-uniqueness.enabled`=" + this.checkSourceUniqueness);
    sql("set `spark.sql.arctic.optimize-write-enabled`=`true`");

    sql("INSERT OVERWRITE " + database + "." + table + " SELECT * FROM " + initView);
    sql("insert into table " + database + '.' + table + " SELECT * FROM " + sourceView);

    rows = sql("SELECT * FROM " + database + "." + table + " ORDER BY order_key ");
    Assert.assertEquals(initialize.size() + newRecordSize, rows.size());
  }
}
