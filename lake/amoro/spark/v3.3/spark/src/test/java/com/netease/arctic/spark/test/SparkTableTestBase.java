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

package com.netease.arctic.spark.test;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.spark.test.helper.TestTableHelper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.metastore.TableType;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SparkTableTestBase extends SparkTestBase {
  protected static final TableFormat MIXED_HIVE = TableFormat.MIXED_HIVE;
  protected static final TableFormat MIXED_ICEBERG = TableFormat.MIXED_ICEBERG;
  protected static final PartitionSpec unpartitioned = PartitionSpec.unpartitioned();
  protected static final PrimaryKeySpec noPrimaryKey = PrimaryKeySpec.noPrimaryKey();

  private String database = "spark_test_database";
  private String table = "test_table";
  private String sourceTable = "test_source_table";

  private Identifier source;

  public String database() {
    return this.database;
  }

  public String table() {
    return this.table;
  }

  public Identifier target() {
    return new Identifier(catalog().name(), database, table, null);
  }

  public Identifier source() {
    Preconditions.checkNotNull(source);
    return source;
  }

  public ArcticTable loadTable() {
    return catalog().loadTable(target().toArcticIdentifier());
  }

  public Table loadHiveTable() {
    Identifier identifier = target();
    return context.loadHiveTable(identifier.database, identifier.table);
  }

  public String provider(TableFormat format) {
    switch (format) {
      case MIXED_HIVE:
      case MIXED_ICEBERG:
        return "arctic";
      case ICEBERG:
        return "iceberg";
      default:
        throw new IllegalArgumentException("un-supported type of format");
    }
  }

  @BeforeEach
  public void before() {
    try {
      LOG.debug("prepare database for table test: " + database);
      if (!catalog().listDatabases().contains(database)) {
        catalog().createDatabase(database);
      }
    } catch (AlreadyExistsException e) {
      // pass
    }
    source = null;
  }

  @AfterEach
  public void after() {
    LOG.debug("clean up table after test: " + catalog().name() + "." + database + "." + table);
    catalog().dropTable(TableIdentifier.of(catalog().name(), database, table), true);
    if (SESSION_CATALOG.equals(currentCatalog)) {
      try {
        context.getHiveClient().dropTable(database, table);
      } catch (Exception e) {
        //pass
      }
    }
  }

  protected void createHiveSource(List<FieldSchema> cols, List<FieldSchema> partitions) {
    this.createHiveSource(cols, partitions, ImmutableMap.of());
  }

  protected void createHiveSource(
      List<FieldSchema> cols, List<FieldSchema> partitions, Map<String, String> properties) {
    long currentTimeMillis = System.currentTimeMillis();
    Table source = new Table(
        sourceTable,
        database,
        null,
        (int) currentTimeMillis / 1000,
        (int) currentTimeMillis / 1000,
        Integer.MAX_VALUE,
        null,
        partitions,
        new HashMap<>(),
        null,
        null,
        TableType.EXTERNAL_TABLE.toString());
    StorageDescriptor storageDescriptor = new StorageDescriptor();
    storageDescriptor.setInputFormat(HiveTableProperties.PARQUET_INPUT_FORMAT);
    storageDescriptor.setOutputFormat(HiveTableProperties.PARQUET_OUTPUT_FORMAT);
    storageDescriptor.setCols(cols);
    SerDeInfo serDeInfo = new SerDeInfo();
    serDeInfo.setSerializationLib(HiveTableProperties.PARQUET_ROW_FORMAT_SERDE);
    storageDescriptor.setSerdeInfo(serDeInfo);
    source.setSd(storageDescriptor);
    source.setParameters(properties);
    try {
      context.getHiveClient().createTable(source);
      this.source = new Identifier(null, database, sourceTable, Identifier.SOURCE_TYPE_HIVE);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ArcticTable createArcticSource(Schema schema, Consumer<TableBuilder> consumer) {
    Identifier identifier = new Identifier(catalog().name(), database, sourceTable, Identifier.SOURCE_TYPE_ARCTIC);
    TableBuilder builder = catalog().newTableBuilder(identifier.toArcticIdentifier(), schema);
    consumer.accept(builder);
    ArcticTable source = builder.create();
    this.source = identifier;
    return source;
  }

  public void createViewSource(Schema schema, List<Record> data) {
    Dataset<Row> ds = spark().createDataFrame(
        data.stream().map(TestTableHelper::recordToRow).collect(Collectors.toList()),
        SparkSchemaUtil.convert(schema));

    ds.createOrReplaceTempView(sourceTable);
    this.source = new Identifier(null, null, sourceTable, Identifier.SOURCE_TYPE_VIEW);
  }

  public ArcticTable createTarget(Schema schema, Consumer<TableBuilder> consumer) {
    Identifier identifier = target();
    TableBuilder builder = catalog().newTableBuilder(identifier.toArcticIdentifier(), schema);
    consumer.accept(builder);
    return builder.create();
  }

  protected boolean tableExists() {
    return catalog().tableExists(target().toArcticIdentifier());
  }

  @AfterEach
  public void cleanUpSource() {
    if (source == null) {
      return;
    }
    if (Identifier.SOURCE_TYPE_ARCTIC.equalsIgnoreCase(source.sourceType)) {
      catalog().dropTable(source.toArcticIdentifier(), true);
    } else if (Identifier.SOURCE_TYPE_HIVE.equalsIgnoreCase(source.sourceType)) {
      context.dropHiveTable(source.database, source.table);
    } else if (Identifier.SOURCE_TYPE_VIEW.equalsIgnoreCase(source.sourceType)) {
      spark().sessionState().catalog().dropTempView(source.table);
    }
  }

  public static class Identifier {
    public static final String SOURCE_TYPE_HIVE = "hive";
    public static final String SOURCE_TYPE_ARCTIC = "arctic";
    public static final String SOURCE_TYPE_VIEW = "view";

    public final String database;
    public final String table;
    public final String catalog;
    public final String sourceType;

    public Identifier(String catalog, String database, String table, String sourceType) {
      this.database = database;
      this.table = table;
      this.catalog = catalog;
      this.sourceType = sourceType;
    }

    public TableIdentifier toArcticIdentifier() {
      return TableIdentifier.of(catalog, database, table);
    }

    @Override
    public String toString() {
      if (SOURCE_TYPE_VIEW.equalsIgnoreCase(sourceType)) {
        return table;
      }
      return database + "." + table;
    }
  }

  public void assertTableDesc(List<Row> rows, List<String> primaryKeys, List<String> partitionKey) {
    boolean partitionBlock = false;
    boolean primaryKeysBlock = false;
    List<String> descPartitionKey = Lists.newArrayList();
    List<String> descPrimaryKeys = Lists.newArrayList();
    List<Object[]> rs = rows.stream()
        .map(row -> IntStream.range(0, row.size())
            .mapToObj(pos -> row.isNullAt(pos) ? null : row.get(pos))
            .toArray(Object[]::new)
        ).collect(Collectors.toList());
    for (Object[] row : rs) {
      if (StringUtils.equalsIgnoreCase("# Partitioning", row[0].toString())) {
        partitionBlock = true;
      } else if (StringUtils.startsWith(row[0].toString(), "Part ") && partitionBlock) {
        descPartitionKey.add(row[1].toString());
      }
      if (StringUtils.equalsIgnoreCase("# Primary keys", row[0].toString())) {
        primaryKeysBlock = true;
      } else if (StringUtils.startsWith(row[0].toString(), "# ") && primaryKeysBlock) {
        primaryKeysBlock = false;
      } else if (primaryKeysBlock) {
        descPrimaryKeys.add(row[0].toString());
      }
    }
    Assertions.assertArrayEquals(
        partitionKey.stream().sorted().distinct().toArray(),
        descPartitionKey.stream().sorted().distinct().toArray());

    Assertions.assertEquals(primaryKeys.size(), descPrimaryKeys.size());
    Assertions.assertArrayEquals(
        primaryKeys.stream().sorted().distinct().toArray(),
        descPrimaryKeys.stream().sorted().distinct().toArray());
  }
}
