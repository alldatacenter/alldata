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
package org.apache.drill.exec.physical.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.exec.metastore.analyze.MetadataAggregateContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.physical.AggPrelBase.OperatorPhase;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.Collections;

@JsonTypeName("metadataHashAggregate")
public class MetadataHashAggPOP extends HashAggregate {
  private final MetadataAggregateContext context;
  private final OperatorPhase phase;

  @JsonCreator
  public MetadataHashAggPOP(@JsonProperty("child") PhysicalOperator child,
      @JsonProperty("context") MetadataAggregateContext context,
      @JsonProperty("phase") OperatorPhase phase) {
    super(child, phase, context.groupByExpressions(), Collections.emptyList(), 1.0F);
    Preconditions.checkArgument(context.createNewAggregations(),
        "Hash aggregate for metadata collecting should be used only for creating new aggregations.");
    this.context = context;
    this.phase = phase;
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new MetadataHashAggPOP(child, context, phase);
  }

  @JsonProperty
  public MetadataAggregateContext getContext() {
    return context;
  }

  @JsonProperty
  public OperatorPhase getPhase() {
    return phase;
  }
}
