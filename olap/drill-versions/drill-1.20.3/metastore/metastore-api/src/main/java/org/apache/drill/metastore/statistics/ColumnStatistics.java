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
package org.apache.drill.metastore.statistics;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.util.TableMetadataUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents collection of statistics values for specific column.
 * For example, text representation of {@link ColumnStatistics} for varchar column
 * is the following:
 * <pre>
 * {
 *     "statistics":[
 *         {"statisticsValue":2.1,"statisticsKind":{"name":"approx_count_distinct"}},
 *         {"statisticsValue":"aaa","statisticsKind":{"exact":true,"name":"minValue"}},
 *         {"statisticsValue":3,"statisticsKind":{"exact":true,"name":"nullsCount"}},
 *         {"statisticsValue":"zzz","statisticsKind":{"exact":true,"name":"maxValue"}}],
 *     "type":"VARCHAR"
 * }
 * </pre>
 *
 * @param <T> type of column values
 */
@JsonAutoDetect(
  fieldVisibility = JsonAutoDetect.Visibility.NONE,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  isGetterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({"statistics", "comparator"})
public class ColumnStatistics<T> {

  private static final ObjectWriter OBJECT_WRITER = new ObjectMapper()
      .registerModule(new JodaModule())
      .writerFor(ColumnStatistics.class);

  private static final ObjectReader OBJECT_READER = new ObjectMapper()
      .registerModule(new JodaModule())
      .readerFor(ColumnStatistics.class);

  private final Map<String, StatisticsHolder<?>> statistics;
  private final Comparator<T> valueComparator;
  private final TypeProtos.MinorType type;

  @JsonCreator
  @SuppressWarnings("unchecked")
  public ColumnStatistics(@JsonProperty("statistics") Collection<StatisticsHolder<?>> statistics,
                          @JsonProperty("type") TypeProtos.MinorType type) {
    this.type = type;
    this.valueComparator = type != null
        ? TableMetadataUtils.getComparator(type)
        : (Comparator<T>) TableMetadataUtils.getNaturalNullsFirstComparator();
    this.statistics = statistics.stream()
        .collect(Collectors.toMap(
            statistic -> statistic.getStatisticsKind().getName(),
            Function.identity(),
            (a, b) -> a.getStatisticsKind().isExact() ? a : b));
  }

  public ColumnStatistics(Collection<StatisticsHolder<?>> statistics) {
    this(statistics, TypeProtos.MinorType.INT);
  }

  /**
   * Returns statistics value which corresponds to specified {@link StatisticsKind}.
   *
   * @param statisticsKind kind of statistics which value should be returned
   * @return statistics value
   */
  @SuppressWarnings("unchecked")
  public <V> V get(StatisticsKind<V> statisticsKind) {
    StatisticsHolder<V> statisticsHolder = (StatisticsHolder<V>)
        statistics.get(statisticsKind.getName());
    if (statisticsHolder != null) {
      return statisticsHolder.getStatisticsValue();
    }
    return null;
  }

  /**
   * Checks whether specified statistics kind is set in this column statistics.
   *
   * @param statisticsKind statistics kind to check
   * @return true if specified statistics kind is set
   */
  public boolean contains(StatisticsKind<?> statisticsKind) {
    return statistics.containsKey(statisticsKind.getName());
  }

  /**
   * Checks whether specified statistics kind is set in this column statistics
   * and it corresponds to the exact statistics value.
   *
   * @param statisticsKind statistics kind to check
   * @return true if value which corresponds to the specified statistics kind is exact
   */
  public boolean containsExact(StatisticsKind<?> statisticsKind) {
    StatisticsHolder<?> statisticsHolder = statistics.get(statisticsKind.getName());
    if (statisticsHolder != null) {
      return statisticsHolder.getStatisticsKind().isExact();
    }
    return false;
  }

  /**
   * Returns {@link Comparator} for comparing values with the same type as column values.
   *
   * @return {@link Comparator}
   */
  public Comparator<T> getValueComparator() {
    return valueComparator;
  }

  /**
   * Returns new {@link ColumnStatistics} instance with overridden statistics taken from specified {@link ColumnStatistics}.
   *
   * @param sourceStatistics source of statistics to override
   * @return new {@link ColumnStatistics} instance with overridden statistics
   */
  public ColumnStatistics<T> cloneWith(ColumnStatistics<T> sourceStatistics) {
    Map<String, StatisticsHolder<?>> newStats = new HashMap<>(this.statistics);
    sourceStatistics.statistics.values().forEach(statisticsHolder -> {
      StatisticsKind<?> statisticsKindToMerge = statisticsHolder.getStatisticsKind();
      StatisticsHolder<?> oldStatistics = statistics.get(statisticsKindToMerge.getName());
      if (oldStatistics == null
          || !oldStatistics.getStatisticsKind().isExact()
          || statisticsKindToMerge.isExact()) {
        newStats.put(statisticsKindToMerge.getName(), statisticsHolder);
      }
    });

    return new ColumnStatistics<>(newStats.values(), type);
  }

  @SuppressWarnings("unchecked")
  public ColumnStatistics<T> genericClone(ColumnStatistics<?> sourceStatistics) {
    return cloneWith((ColumnStatistics<T>) sourceStatistics);
  }

  @JsonProperty("statistics")
  @SuppressWarnings("unused") // used for serialization
  private Collection<StatisticsHolder<?>> getAll() {
    return statistics.values();
  }

  @JsonProperty("type")
  public TypeProtos.MinorType getComparatorType() {
    return type;
  }

  public String jsonString() {
    try {
      return OBJECT_WRITER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to convert column statistics to json string", e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ColumnStatistics<?> that = (ColumnStatistics<?>) o;
    return Objects.equals(statistics, that.statistics)
        && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(statistics, type);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ColumnStatistics.class.getSimpleName() + "[", "]")
        .add("statistics=" + statistics)
        .add("type=" + type)
        .toString();
  }

  public static ColumnStatistics<?> of(String columnStatistics) {
    try {
      return OBJECT_READER.readValue(columnStatistics);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to convert column statistics from json string: " + columnStatistics, e);
    }
  }
}
