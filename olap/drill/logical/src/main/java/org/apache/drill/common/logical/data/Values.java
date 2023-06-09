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

import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

@JsonTypeName("values")
public class Values extends SourceOperator {

    private final JSONOptions content;

    @JsonCreator
    public Values(@JsonProperty("content") JSONOptions content){
        super();
        this.content = content;
        Preconditions.checkNotNull(content, "content attribute is required for source operator 'constant'.");
    }

    public JSONOptions getContent() {
        return content;
    }

    @Override
    public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
      return logicalVisitor.visitValues(this, value);
    }

    public static Builder builder(){
      return new Builder();
    }

    public static class Builder extends AbstractBuilder<Values>{
      private JSONOptions content;

      public Builder content(JsonNode n){
        content = new JSONOptions(n, JsonLocation.NA);
        return this;
      }

      @Override
      public Values build() {
        return new Values(content);
      }
    }

}
