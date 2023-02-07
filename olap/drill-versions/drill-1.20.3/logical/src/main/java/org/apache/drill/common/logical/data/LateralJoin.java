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
package org.apache.drill.common.logical.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import java.util.Iterator;

@JsonTypeName("lateral-join")
public class LateralJoin extends LogicalOperatorBase {
  private final LogicalOperator left;
  private final LogicalOperator right;

  @JsonCreator
  public LateralJoin(@JsonProperty("left") LogicalOperator left, @JsonProperty("right") LogicalOperator right) {
    super();
    this.left = left;
    this.right = right;
    left.registerAsSubscriber(this);
    right.registerAsSubscriber(this);
  }

  public LogicalOperator getLeft() {
    return left;
  }

  public LogicalOperator getRight() {
    return right;
  }

  @Override
  public Iterator<LogicalOperator> iterator() {
    return Iterators.forArray(getLeft(), getRight());
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitLateralJoin(this, value);
  }
}
