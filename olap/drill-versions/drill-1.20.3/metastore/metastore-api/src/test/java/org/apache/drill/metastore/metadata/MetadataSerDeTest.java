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
package org.apache.drill.metastore.metadata;

import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.statistics.BaseStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(MetastoreTest.class)
public class MetadataSerDeTest extends BaseTest {

  @Test
  public void testStatisticsHolderSerialization() {
    checkStatisticsHolderSerialization(1, TableStatisticsKind.ROW_COUNT,
        "{\"statisticsValue\":1,\"statisticsKind\":{\"exact\":true,\"name\":\"rowCount\"}}");
    checkStatisticsHolderSerialization(1.234, TableStatisticsKind.EST_ROW_COUNT,
        "{\"statisticsValue\":1.234,\"statisticsKind\":{\"name\":\"rowcount\"}}");
    checkStatisticsHolderSerialization(true, TableStatisticsKind.HAS_DESCRIPTIVE_STATISTICS,
        "{\"statisticsValue\":true,\"statisticsKind\":{\"name\":\"hasDescriptiveStatistics\"}}");
    checkStatisticsHolderSerialization(null, TableStatisticsKind.EST_ROW_COUNT,
        "{\"statisticsKind\":{\"name\":\"rowcount\"}}");
    checkStatisticsHolderSerialization("aAaAaAa", ColumnStatisticsKind.MIN_VALUE,
        "{\"statisticsValue\":\"aAaAaAa\",\"statisticsKind\":{\"exact\":true,\"name\":\"minValue\"}}");
    checkStatisticsHolderSerialization(new BigDecimal("123.321"), ColumnStatisticsKind.MIN_VALUE,
        "{\"statisticsValue\":{\"java.math.BigDecimal\":123.321},\"statisticsKind\":{\"exact\":true,\"name\":\"minValue\"}}");
    checkStatisticsHolderSerialization(new byte[]{1, 1, 2, 3, 5, 8}, ColumnStatisticsKind.MAX_VALUE,
        "{\"statisticsValue\":{\"[B\":\"AQECAwUI\"},\"statisticsKind\":{\"exact\":true,\"name\":\"maxValue\"}}");
  }

  @Test
  public void testStatisticsHolderDeserialization() {
    checkStatisticsHolderDeserialization(1, TableStatisticsKind.ROW_COUNT);
    checkStatisticsHolderDeserialization(1.234, TableStatisticsKind.EST_ROW_COUNT);
    checkStatisticsHolderDeserialization(true, TableStatisticsKind.HAS_DESCRIPTIVE_STATISTICS);
    checkStatisticsHolderDeserialization(null, TableStatisticsKind.EST_ROW_COUNT);
    checkStatisticsHolderDeserialization("aAaAaAa", ColumnStatisticsKind.MIN_VALUE);
    checkStatisticsHolderDeserialization(new BigDecimal("123.321"), ColumnStatisticsKind.MIN_VALUE);
    checkStatisticsHolderDeserialization(new byte[]{1, 1, 2, 3, 5, 8}, ColumnStatisticsKind.MAX_VALUE);
  }

  @Test
  public void testColumnStatisticsSerialization() {
    List<StatisticsHolder<?>> statistics = Arrays.asList(
        new StatisticsHolder<>("aaa", ColumnStatisticsKind.MIN_VALUE),
        new StatisticsHolder<>("zzz", ColumnStatisticsKind.MAX_VALUE),
        new StatisticsHolder<>(3, ColumnStatisticsKind.NULLS_COUNT),
        new StatisticsHolder<>(2.1, ColumnStatisticsKind.NDV));
    ColumnStatistics<String> columnStatistics = new ColumnStatistics<>(statistics, TypeProtos.MinorType.VARCHAR);
    String serializedColumnStatistics = columnStatistics.jsonString();

    String expected =
        "{" +
            "\"statistics\":[" +
                "{\"statisticsValue\":2.1,\"statisticsKind\":{\"name\":\"approx_count_distinct\"}}," +
                "{\"statisticsValue\":\"aaa\",\"statisticsKind\":{\"exact\":true,\"name\":\"minValue\"}}," +
                "{\"statisticsValue\":3,\"statisticsKind\":{\"exact\":true,\"name\":\"nullsCount\"}}," +
                "{\"statisticsValue\":\"zzz\",\"statisticsKind\":{\"exact\":true,\"name\":\"maxValue\"}}]," +
            "\"type\":\"VARCHAR\"" +
        "}";

    assertEquals("StatisticsHolder was incorrectly serialized",
        expected,
        serializedColumnStatistics);
  }

  @Test
  public void testColumnStatisticsDeserialization() {
    List<StatisticsHolder<?>> statistics = Arrays.asList(
        new StatisticsHolder<>("aaa", ColumnStatisticsKind.MIN_VALUE),
        new StatisticsHolder<>("zzz", ColumnStatisticsKind.MAX_VALUE),
        new StatisticsHolder<>(3, ColumnStatisticsKind.NULLS_COUNT),
        new StatisticsHolder<>(2.1, ColumnStatisticsKind.NDV));
    ColumnStatistics<String> columnStatistics = new ColumnStatistics<>(statistics, TypeProtos.MinorType.VARCHAR);
    String serializedColumnStatistics = columnStatistics.jsonString();

    ColumnStatistics<?> deserialized = ColumnStatistics.of(serializedColumnStatistics);

    assertEquals("Type was incorrectly deserialized",
        columnStatistics.getComparatorType(),
        deserialized.getComparatorType());

    for (StatisticsHolder<?> statistic : statistics) {
      assertEquals("Statistics kind was incorrectly deserialized",
          statistic.getStatisticsKind().isExact(),
          deserialized.containsExact(statistic.getStatisticsKind()));
      assertEquals("Statistics value was incorrectly deserialized",
          statistic.getStatisticsValue(),
          deserialized.get(statistic.getStatisticsKind()));
    }
  }

  private <T> void checkStatisticsHolderSerialization(T statisticsValue,
      BaseStatisticsKind<?> statisticsKind, String expectedString) {
    StatisticsHolder<T> statisticsHolder =
        new StatisticsHolder<>(statisticsValue, statisticsKind);
    String serializedStatisticsHolder = statisticsHolder.jsonString();

    assertEquals("StatisticsHolder was incorrectly serialized",
        expectedString,
        serializedStatisticsHolder);
  }

  private <T> void checkStatisticsHolderDeserialization(T statisticsValue,
      BaseStatisticsKind<?> statisticsKind) {
    StatisticsHolder<T> rowCount =
        new StatisticsHolder<>(statisticsValue, statisticsKind);
    StatisticsHolder<?> deserializedRowCount = StatisticsHolder.of(rowCount.jsonString());

    assertTrue("Statistics value was incorrectly deserialized",
        Objects.deepEquals(rowCount.getStatisticsValue(), deserializedRowCount.getStatisticsValue()));

    assertStatisticsKindsEquals(rowCount, deserializedRowCount);
  }

  private <T> void assertStatisticsKindsEquals(StatisticsHolder<T> expected, StatisticsHolder<?> actual) {
    assertEquals("isExact statistics kind was incorrectly deserialized",
        expected.getStatisticsKind().isExact(),
        actual.getStatisticsKind().isExact());

    assertEquals("getName statistics kind was incorrectly deserialized",
        expected.getStatisticsKind().getName(),
        actual.getStatisticsKind().getName());
  }
}
