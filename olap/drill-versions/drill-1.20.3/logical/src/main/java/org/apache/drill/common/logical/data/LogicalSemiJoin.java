package org.apache.drill.common.logical.data;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import java.util.Iterator;
import java.util.List;

@JsonTypeName("semi-join")
public class LogicalSemiJoin extends Join {

  @JsonCreator
  public LogicalSemiJoin(@JsonProperty("left") LogicalOperator left,
                         @JsonProperty("right") LogicalOperator right,
                         @JsonProperty("conditions") List<JoinCondition> conditions,
                         @JsonProperty("type") JoinRelType type) {
    super(left, right, conditions, type);
  }


  @Override
  public Iterator<LogicalOperator> iterator() {
    return Iterators.forArray(getLeft(), getRight());
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitJoin(this, value);
  }
}
