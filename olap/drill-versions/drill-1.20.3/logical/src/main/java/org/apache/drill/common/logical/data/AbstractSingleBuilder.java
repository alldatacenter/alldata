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

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public abstract class AbstractSingleBuilder<T extends SingleInputOperator, X extends AbstractSingleBuilder<T, X>> extends AbstractBuilder<T> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractSingleBuilder.class);

  private LogicalOperator input;

  @Override
  public final T build(){
    Preconditions.checkNotNull(input);
    T out = internalBuild();
    out.setInput(input);
    return out;
  }

  public X setInput(LogicalOperator input){
    this.input = input;
    return (X) this;
  }

  public abstract T internalBuild();

}
