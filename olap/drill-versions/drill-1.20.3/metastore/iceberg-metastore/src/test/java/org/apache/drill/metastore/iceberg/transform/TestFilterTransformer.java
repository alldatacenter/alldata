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
package org.apache.drill.metastore.iceberg.transform;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.iceberg.IcebergBaseTest;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestFilterTransformer extends IcebergBaseTest {

  private static FilterTransformer transformer;

  @BeforeClass
  public static void init() {
    transformer = new FilterTransformer();
  }

  @Test
  public void testToFilterNull() {
    Expression expected = Expressions.alwaysTrue();
    Expression actual = transformer.transform((FilterExpression) null);

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterEqual() {
    Expression expected = Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1);
    Expression actual = transformer.transform(FilterExpression.equal(MetastoreColumn.ROW_GROUP_INDEX, 1));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterNotEqual() {
    Expression expected = Expressions.notEqual(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1);
    Expression actual = transformer.transform(FilterExpression.notEqual(MetastoreColumn.ROW_GROUP_INDEX, 1));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterLessThan() {
    Expression expected = Expressions.lessThan(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1);
    Expression actual = transformer.transform(FilterExpression.lessThan(MetastoreColumn.ROW_GROUP_INDEX, 1));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterLessThanOrEqual() {
    Expression expected = Expressions.lessThanOrEqual(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1);
    Expression actual = transformer.transform(FilterExpression.lessThanOrEqual(MetastoreColumn.ROW_GROUP_INDEX, 1));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterGreaterThan() {
    Expression expected = Expressions.greaterThan(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1);
    Expression actual = transformer.transform(FilterExpression.greaterThan(MetastoreColumn.ROW_GROUP_INDEX, 1));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterGreaterThanOrEqual() {
    Expression expected = Expressions.greaterThanOrEqual(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1);
    Expression actual = transformer.transform(FilterExpression.greaterThanOrEqual(MetastoreColumn.ROW_GROUP_INDEX, 1));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterIn() {
    Expression expected = Expressions.in(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1, 2);
    Expression actual = transformer.transform(FilterExpression.in(MetastoreColumn.ROW_GROUP_INDEX, 1, 2));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterNotIn() {
    Expression expected = Expressions.notIn(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1, 2);
    Expression actual = transformer.transform(FilterExpression.notIn(MetastoreColumn.ROW_GROUP_INDEX, 1, 2));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterIsNull() {
    Expression expected = Expressions.isNull(MetastoreColumn.ROW_GROUP_INDEX.columnName());
    Expression actual = transformer.transform(FilterExpression.isNull(MetastoreColumn.ROW_GROUP_INDEX));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterIsNotNull() {
    Expression expected = Expressions.notNull(MetastoreColumn.ROW_GROUP_INDEX.columnName());
    Expression actual = transformer.transform(FilterExpression.isNotNull(MetastoreColumn.ROW_GROUP_INDEX));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterNot() {
    Expression expected = Expressions.not(Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1));
    Expression actual = transformer.transform(FilterExpression.not(FilterExpression.equal(MetastoreColumn.ROW_GROUP_INDEX, 1)));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterAnd() {
    Expression expected = Expressions.and(
      Expressions.equal(MetastoreColumn.STORAGE_PLUGIN.columnName(), "dfs"),
      Expressions.equal(MetastoreColumn.WORKSPACE.columnName(), "tmp"),
      Expressions.equal(MetastoreColumn.TABLE_NAME.columnName(), "nation"),
      Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 4));

    Expression actual = transformer.transform(FilterExpression.and(
      FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"),
      FilterExpression.equal(MetastoreColumn.WORKSPACE, "tmp"),
      FilterExpression.equal(MetastoreColumn.TABLE_NAME, "nation"),
      FilterExpression.equal(MetastoreColumn.ROW_GROUP_INDEX, 4)));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterOr() {
    Expression expected = Expressions.or(
      Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1),
      Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 2));
    Expression actual = transformer.transform(
      FilterExpression.or(
        FilterExpression.equal(MetastoreColumn.ROW_GROUP_INDEX, 1),
        FilterExpression.equal(MetastoreColumn.ROW_GROUP_INDEX, 2)));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterUnsupported() {
    thrown.expect(UnsupportedOperationException.class);

    transformer.transform(new FilterExpression() {
      @Override
      public Operator operator() {
        return null;
      }

      @Override
      public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
      }
    });
  }

  @Test
  public void testToFilterConditionsNull() {
    assertEquals(Expressions.alwaysTrue().toString(), transformer.transform((Map<MetastoreColumn, Object>) null).toString());
  }

  @Test
  public void testToFilterConditionsEmpty() {
    assertEquals(Expressions.alwaysTrue().toString(), transformer.transform(Collections.emptyMap()).toString());
  }

  @Test
  public void testToFilterConditionsOne() {
    Map<MetastoreColumn, Object> conditions = new LinkedHashMap<>();
    conditions.put(MetastoreColumn.ROW_GROUP_INDEX, 1);

    assertEquals(Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 1).toString(), transformer.transform(conditions).toString());
  }

  @Test
  public void testToFilterConditionsTwo() {
    Map<MetastoreColumn, Object> conditions = new LinkedHashMap<>();
    conditions.put(MetastoreColumn.STORAGE_PLUGIN, "dfs");
    conditions.put(MetastoreColumn.WORKSPACE, "tmp");

    Expression expected = Expressions.and(
      Expressions.equal(MetastoreColumn.STORAGE_PLUGIN.columnName(), "dfs"),
      Expressions.equal(MetastoreColumn.WORKSPACE.columnName(), "tmp"));

    assertEquals(expected.toString(), transformer.transform(conditions).toString());
  }

  @Test
  public void testToFilterConditionsFour() {
    Map<MetastoreColumn, Object> conditions = new LinkedHashMap<>();
    conditions.put(MetastoreColumn.STORAGE_PLUGIN, "dfs");
    conditions.put(MetastoreColumn.WORKSPACE, "tmp");
    conditions.put(MetastoreColumn.TABLE_NAME, "nation");
    conditions.put(MetastoreColumn.ROW_GROUP_INDEX, 4);

    Expression expected = Expressions.and(
      Expressions.equal(MetastoreColumn.STORAGE_PLUGIN.columnName(), "dfs"),
      Expressions.equal(MetastoreColumn.WORKSPACE.columnName(), "tmp"),
      Expressions.equal(MetastoreColumn.TABLE_NAME.columnName(), "nation"),
      Expressions.equal(MetastoreColumn.ROW_GROUP_INDEX.columnName(), 4));

    assertEquals(expected.toString(), transformer.transform(conditions).toString());
  }

  @Test
  public void testToFilterMetadataTypesAll() {
    Expression expected = Expressions.alwaysTrue();

    Expression actual = transformer.transform(
      Sets.newHashSet(MetadataType.PARTITION, MetadataType.FILE, MetadataType.ALL));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterMetadataTypesOne() {
    Expression expected = Expressions.equal(MetastoreColumn.METADATA_TYPE.columnName(), MetadataType.PARTITION.name());

    Expression actual = transformer.transform(Collections.singleton(MetadataType.PARTITION));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testToFilterMetadataTypesSeveral() {
    Expression expected = Expressions.in(MetastoreColumn.METADATA_TYPE.columnName(),
      MetadataType.PARTITION.name(), MetadataType.FILE.name());

    Expression actual = transformer.transform(
      Sets.newHashSet(MetadataType.PARTITION, MetadataType.FILE));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testCombineNone() {
    Expression expected = Expressions.alwaysTrue();

    Expression actual = transformer.combine();

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testCombineOne() {
    Expression expected = Expressions.equal("a", 1);

    Expression actual = transformer.combine(expected);

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testCombineTwo() {
    Expression expected = Expressions.and(
      Expressions.equal("a", 1), Expressions.equal("a", 2));

    Expression actual = transformer.combine(
      Expressions.equal("a", 1), Expressions.equal("a", 2));

    assertEquals(expected.toString(), actual.toString());
  }

  @Test
  public void testCombineFour() {
    Expression expected = Expressions.and(
      Expressions.equal("a", 1), Expressions.equal("a", 2),
      Expressions.equal("a", 3), Expressions.equal("a", 4));

    Expression actual = transformer.combine(
      Expressions.equal("a", 1), Expressions.equal("a", 2),
      Expressions.equal("a", 3), Expressions.equal("a", 4));

    assertEquals(expected.toString(), actual.toString());
  }
}
