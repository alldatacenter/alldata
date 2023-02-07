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

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("statistics-aggregate")
public class StatisticsAggregate extends StreamingAggregate {

  public static final String OPERATOR_TYPE = "STATISTICS_AGGREGATE";

  private final List<String> functions;

  @JsonCreator
  public StatisticsAggregate(
      @JsonProperty("child") PhysicalOperator child,
      @JsonProperty("functions") List<String> functions) {
    super(child, null, null);
    this.functions = ImmutableList.copyOf(functions);
  }

  public List<String> getFunctions() {
    return functions;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value)
      throws E {
    return physicalVisitor.visitStatisticsAggregate(this, value);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    return new StatisticsAggregate(child, functions);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

}
