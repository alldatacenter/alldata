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

import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("writer")
public class Writer extends SingleInputOperator {

  // TODO: Hack - Make the type generic until "common" and "java-exec" modules are merged (DRILL-507).
  private final Object createTableEntry;

  @JsonCreator
  public Writer(@JsonProperty("createTableEntry") Object createTableEntry) {
    this.createTableEntry = createTableEntry;
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitWriter(this, value);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends AbstractSingleBuilder<Writer, Builder> {

    // TODO: Hack - Make the type generic until "common" and "java-exec" modules are merged (DRILL-507).
    private Object createTableEntry;

    public Builder setCreateTableEntry(Object createTableEntry) {
      this.createTableEntry = createTableEntry;
      return this;
    }

    @Override
    public Writer internalBuild() {
      return new Writer(createTableEntry);
    }
  }
}
