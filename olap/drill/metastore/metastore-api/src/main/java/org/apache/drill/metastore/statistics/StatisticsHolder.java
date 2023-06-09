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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class-holder for statistics kind and its value.
 *
 * @param <T> Type of statistics value
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StatisticsHolder<T> {

  private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerFor(StatisticsHolder.class);
  private static final ObjectReader OBJECT_READER = new ObjectMapper().readerFor(StatisticsHolder.class);

  private final T statisticsValue;
  private final BaseStatisticsKind<?> statisticsKind;

  @JsonCreator
  public StatisticsHolder(@JsonProperty("statisticsValue") T statisticsValue,
                          @JsonProperty("statisticsKind") BaseStatisticsKind<?> statisticsKind) {
    this.statisticsValue = statisticsValue;
    this.statisticsKind = statisticsKind;
  }

  public StatisticsHolder(T statisticsValue,
                          StatisticsKind<?> statisticsKind) {
    this.statisticsValue = statisticsValue;
    this.statisticsKind = (BaseStatisticsKind<?>) statisticsKind;
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
                include = JsonTypeInfo.As.WRAPPER_OBJECT)
  public T getStatisticsValue() {
    return statisticsValue;
  }

  public StatisticsKind<?> getStatisticsKind() {
    return statisticsKind;
  }

  public String jsonString() {
    try {
      return OBJECT_WRITER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to convert statistics holder to json string", e);
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
    StatisticsHolder<?> that = (StatisticsHolder<?>) o;
    return Objects.equals(statisticsValue, that.statisticsValue)
        && Objects.equals(statisticsKind, that.statisticsKind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statisticsValue, statisticsKind);
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", StatisticsHolder.class.getSimpleName() + "[\n", "]")
        .add("statisticsValue=" + statisticsValue)
        .add("statisticsKind=" + statisticsKind)
        .toString();
  }

  public static StatisticsHolder<?> of(String serialized) {
    try {
      return OBJECT_READER.readValue(serialized);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to convert statistics holder from json string: " + serialized, e);
    }
  }
}
