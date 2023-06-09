/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.rdbms.components.tables;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.TestData;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.rdbms.RdbmsBaseTest;
import org.apache.drill.metastore.rdbms.exception.RdbmsMetastoreException;
import org.apache.drill.metastore.rdbms.operate.RdbmsOperation;
import org.apache.drill.metastore.rdbms.transform.MetadataMapper;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.jooq.Record;
import org.jooq.generated.Tables;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class TestTablesTransformer extends RdbmsBaseTest {

  private static final TablesTransformer TRANSFORMER = TablesTransformer.get();

  @Test
  public void testToMappersAll() {
    Set<MetadataMapper<TableMetadataUnit, ? extends Record>> metadataMappers =
      TRANSFORMER.toMappers(Sets.newHashSet(MetadataType.ALL, MetadataType.FILE));

    assertEquals(5, metadataMappers.size());
  }

  @Test
  public void testToMappersSome() {
    Set<MetadataMapper<TableMetadataUnit, ? extends Record>> metadataMappers =
      TRANSFORMER.toMappers(Sets.newHashSet(MetadataType.TABLE, MetadataType.FILE));

    assertEquals(
      Sets.newHashSet(TablesMetadataMapper.TableMapper.get(), TablesMetadataMapper.FileMapper.get()),
      metadataMappers);
  }

  @Test
  public void testToMappersAbsent() {
    try {
      TRANSFORMER.toMappers(Sets.newHashSet(MetadataType.TABLE, MetadataType.VIEW));
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Metadata mapper is absent for type"));
    }
  }

  @Test
  public void testToMapperExisting() {
    MetadataMapper<TableMetadataUnit, ? extends Record> mapper = TRANSFORMER.toMapper(MetadataType.TABLE);
    assertSame(TablesMetadataMapper.TableMapper.get(), mapper);
  }

  @Test
  public void testToMapperAbsent() {
    try {
      TRANSFORMER.toMapper(MetadataType.VIEW);
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Metadata mapper is absent for type"));
    }
  }

  @Test
  public void testToMapperAll() {
    try {
      TRANSFORMER.toMapper(MetadataType.ALL);
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Metadata mapper is absent for type"));
    }
  }

  @Test
  public void testToOverwriteOneUnit() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Collections.singletonList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .build());

    List<RdbmsOperation.Overwrite> overwrites = TRANSFORMER.toOverwrite(units);
    assertEquals(1, overwrites.size());

    RdbmsOperation.Overwrite overwrite = overwrites.get(0);
    assertEquals(Tables.TABLES, overwrite.table());

    assertEquals(1, overwrite.deleteConditions().size());
    assertEquals(
      DSL.and(Tables.TABLES.STORAGE_PLUGIN.eq("dfs"),
        Tables.TABLES.WORKSPACE.eq("tmp"),
        Tables.TABLES.TABLE_NAME.eq("nation")),
      overwrite.deleteConditions().get(0));
  }

  @Test
  public void testToOverwriteSeveralUnitsSameType() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("region")
        .metadataType(MetadataType.TABLE.name())
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .build()
    );

    List<RdbmsOperation.Overwrite> overwrites = TRANSFORMER.toOverwrite(units);
    assertEquals(1, overwrites.size());

    RdbmsOperation.Overwrite overwrite = overwrites.get(0);
    assertEquals(Tables.TABLES, overwrite.table());

    assertEquals(2, overwrite.deleteConditions().size());
  }

  @Test
  public void testToOverwriteSeveralUnitsDifferentTypes() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("region")
        .metadataType(MetadataType.TABLE.name())
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .build(),
      basicUnit.toBuilder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.SEGMENT.name())
        .build()
    );

    List<RdbmsOperation.Overwrite> overwrites = TRANSFORMER.toOverwrite(units);
    assertEquals(2, overwrites.size());
  }

  @Test
  public void testToOverwriteAbsentMetadataType() {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    List<TableMetadataUnit> units = Arrays.asList(
      basicUnit.toBuilder()
        .metadataType(MetadataType.TABLE.name())
        .build(),
      basicUnit.toBuilder()
        .metadataType(MetadataType.VIEW.name())
        .build()
    );

    try {
      TRANSFORMER.toOverwrite(units);
      fail();
    } catch (RdbmsMetastoreException e) {
      assertThat(e.getMessage(), startsWith("Metadata mapper is absent for type"));
    }
  }

  @Test
  public void testToDeleteOne() {
    org.apache.drill.metastore.operate.Delete metastoreDelete = org.apache.drill.metastore.operate.Delete.builder()
      .metadataType(MetadataType.TABLE)
      .filter(FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"))
      .build();

    List<RdbmsOperation.Delete> rdbmsDeletes = TRANSFORMER.toDelete(metastoreDelete);

    assertEquals(1, rdbmsDeletes.size());

    RdbmsOperation.Delete rdbmsDelete = rdbmsDeletes.get(0);
    assertEquals(Tables.TABLES, rdbmsDelete.table());
    assertEquals(Tables.TABLES.STORAGE_PLUGIN.eq("dfs").toString(), rdbmsDelete.condition().toString());
  }

  @Test
  public void testToDeleteSeveral() {
    org.apache.drill.metastore.operate.Delete metastoreDelete = org.apache.drill.metastore.operate.Delete.builder()
      .metadataType(MetadataType.ALL)
      .filter(FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"))
      .build();

    List<RdbmsOperation.Delete> rdbmsDeletes = TRANSFORMER.toDelete(metastoreDelete);

    assertEquals(5, rdbmsDeletes.size());
  }

  @Test
  public void testToDeleteAll() {
    List<RdbmsOperation.Delete> rdbmsDeletes = TRANSFORMER.toDeleteAll();

    assertEquals(5, rdbmsDeletes.size());
    rdbmsDeletes.stream()
      .map(RdbmsOperation.Delete::condition)
      .forEach(condition -> assertEquals(DSL.noCondition().toString(), condition.toString()));
  }
}
