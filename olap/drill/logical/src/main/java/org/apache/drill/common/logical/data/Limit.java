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

import java.util.Iterator;

import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;

@JsonTypeName("limit")
public class Limit extends SingleInputOperator {

  private final Integer first;
  private final Integer last;

  @JsonCreator
  public Limit(@JsonProperty("first") Integer first, @JsonProperty("last") Integer last) {
    super();
    this.first = first;
    this.last = last;
  }

  public Integer getFirst() {
    return first;
  }

  public Integer getLast() {
    return last;
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitLimit(this, value);
  }

  @Override
  public NodeBuilder<Limit> nodeBuilder() {
    return new LimitNodeBuilder();  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Iterator<LogicalOperator> iterator() {
    return Iterators.singletonIterator(getInput());
  }

  public static class LimitNodeBuilder implements NodeBuilder<Limit> {

    @Override
    public ObjectNode convert(ObjectMapper mapper, Limit operator, Integer inputId) {
      ObjectNode limitNode = mapper.createObjectNode();
      limitNode.put("op", "limit");
      limitNode.put("input", inputId);
      limitNode.put("first", operator.first);
      limitNode.put("last", operator.last);
      return limitNode;
    }
  }
}
