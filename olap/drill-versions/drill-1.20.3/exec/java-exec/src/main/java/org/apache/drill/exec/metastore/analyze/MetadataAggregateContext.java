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
package org.apache.drill.exec.metastore.analyze;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.metastore.metadata.MetadataType;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Class which provides information required for producing metadata aggregation when performing analyze.
 */
@JsonDeserialize(builder = MetadataAggregateContext.MetadataAggregateContextBuilder.class)
public class MetadataAggregateContext {
  private final List<NamedExpression> groupByExpressions;
  private final List<SchemaPath> interestingColumns;

  /**
   * List of columns which do not belong to table schema, but used to pass some metadata information like file location, row group index, etc.
   */
  private final List<SchemaPath> metadataColumns;
  private final boolean createNewAggregations;
  private final MetadataType metadataLevel;

  public MetadataAggregateContext(MetadataAggregateContextBuilder builder) {
    this.groupByExpressions = builder.groupByExpressions;
    this.interestingColumns = builder.interestingColumns;
    this.createNewAggregations = builder.createNewAggregations;
    this.metadataColumns = builder.metadataColumns;
    this.metadataLevel = builder.metadataLevel;
  }

  @JsonProperty
  public List<NamedExpression> groupByExpressions() {
    return groupByExpressions;
  }

  @JsonProperty
  public List<SchemaPath> interestingColumns() {
    return interestingColumns;
  }

  @JsonProperty
  public boolean createNewAggregations() {
    return createNewAggregations;
  }

  @JsonProperty
  public List<SchemaPath> metadataColumns() {
    return metadataColumns;
  }

  @JsonProperty
  public MetadataType metadataLevel() {
    return metadataLevel;
  }

  @Override
  public String toString() {
    return new StringJoiner(",\n", MetadataAggregateContext.class.getSimpleName() + "[", "]")
        .add("groupByExpressions=" + groupByExpressions)
        .add("interestingColumns=" + interestingColumns)
        .add("createNewAggregations=" + createNewAggregations)
        .add("excludedColumns=" + metadataColumns)
        .toString();
  }

  public static MetadataAggregateContextBuilder builder() {
    return new MetadataAggregateContextBuilder();
  }

  public MetadataAggregateContextBuilder toBuilder() {
    return new MetadataAggregateContextBuilder()
        .groupByExpressions(groupByExpressions)
        .interestingColumns(interestingColumns)
        .createNewAggregations(createNewAggregations)
        .metadataColumns(metadataColumns)
        .metadataLevel(metadataLevel);
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class MetadataAggregateContextBuilder {
    private List<NamedExpression> groupByExpressions;
    private List<SchemaPath> interestingColumns;
    private Boolean createNewAggregations;
    private MetadataType metadataLevel;
    private List<SchemaPath> metadataColumns;

    public MetadataAggregateContextBuilder groupByExpressions(List<NamedExpression> groupByExpressions) {
      this.groupByExpressions = groupByExpressions;
      return this;
    }

    public MetadataAggregateContextBuilder metadataLevel(MetadataType metadataLevel) {
      this.metadataLevel = metadataLevel;
      return this;
    }

    public MetadataAggregateContextBuilder interestingColumns(List<SchemaPath> interestingColumns) {
      this.interestingColumns = interestingColumns;
      return this;
    }

    public MetadataAggregateContextBuilder createNewAggregations(boolean createNewAggregations) {
      this.createNewAggregations = createNewAggregations;
      return this;
    }

    public MetadataAggregateContextBuilder metadataColumns(List<SchemaPath> metadataColumns) {
      this.metadataColumns = metadataColumns;
      return this;
    }

    public MetadataAggregateContext build() {
      Objects.requireNonNull(groupByExpressions, "groupByExpressions were not set");
      Objects.requireNonNull(createNewAggregations, "createNewAggregations was not set");
      Objects.requireNonNull(metadataColumns, "metadataColumns were not set");
      Objects.requireNonNull(metadataLevel, "metadataLevel was not set");
      return new MetadataAggregateContext(this);
    }
  }
}
